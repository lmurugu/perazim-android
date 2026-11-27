#!/usr/bin/env node
// Pilot-only operator script. ONE transaction holding the migration runner's advisory lock:
// (1) the runner's own verifyHistory (ledger versions, names and checksums vs the tracked files,
//     gap check), (2) exact pending-set check (must be exactly 0022 and 0023),
// (3) the pilot reconciliation SQL, (4) the pending tracked migrations as tracked,
// (5) ledger rows, (6) post-checks. Any failure rolls back every step. COMMIT only if all pass.
//
// Usage (after backup-pilot.sh and pre-flight.sql have been reviewed):
//   DATABASE_URL=postgresql://... MIGRATE_CONFIRM=perazim_pilot PRE_CHANGE_DUMP=/path/to/dump \
//   CLONE_SERVER=/path/to/clean/server node reconcile-and-migrate.js --confirm=perazim_pilot
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const SERVER = process.env.CLONE_SERVER || path.resolve(__dirname, '../../server');
const { Pool } = require(path.join(SERVER, 'node_modules/pg'));
const M = require(path.join(SERVER, 'src/migrations.js'));
const MIGRATIONS = path.join(SERVER, 'migrations');
const RECON_SQL = fs.readFileSync(path.join(__dirname, 'reconcile-pilot-privacy-notice.sql'), 'utf8');
const LOCK_KEY = '723448319116047';   // must equal LOCK_KEY in src/migrations.js (checked below)
const EXPECTED_PENDING = {             // checksums of the tracked files at git HEAD
  '0022': { name: '0022_games_leaderboard.sql', checksum: 'c27c05e4ecab95668c65e3ba7ca996c5e5f7f7727fb237af1b9094cacfda43b0' },
  '0023': { name: '0023_privacy_notice_v02.sql', checksum: 'a336e29b8bf8a511b73b7c0ff565170fe3dcbab2dc4d645837be3d0776df81f8' }
};
const die = msg => { throw new Error(msg); };

async function main() {
  const databaseUrl = process.env.DATABASE_URL || die('DATABASE_URL is required');
  const target = decodeURIComponent(new URL(databaseUrl).pathname.slice(1));
  if (process.argv[2] !== `--confirm=${target}`) die(`pass --confirm=${target}`);
  if (!['localhost', '127.0.0.1', '::1'].includes(new URL(databaseUrl).hostname)) die('pilot writes are limited to a local database');
  if (!target.endsWith('_test') && process.env.MIGRATE_CONFIRM !== target) die(`also set MIGRATE_CONFIRM=${target}`);
  const dump = process.env.PRE_CHANGE_DUMP || die('PRE_CHANGE_DUMP must name the dump taken before this step');
  execFileSync('pg_restore', ['--list', dump], { stdio: 'ignore' });
  execFileSync('sha256sum', ['--check', `${dump}.sha256`], { stdio: 'ignore' });
  if (!/pg_advisory_xact_lock\(\$1::bigint\)/.test(fs.readFileSync(path.join(SERVER, 'src/migrations.js'), 'utf8')) ||
      !fs.readFileSync(path.join(SERVER, 'src/migrations.js'), 'utf8').includes(`LOCK_KEY = '${LOCK_KEY}'`)) {
    die('advisory lock key in the tree does not match this script');
  }

  // Tracked files on disk must be the reviewed ones before anything runs.
  const onDisk = M.loadMigrations(MIGRATIONS);
  for (const [v, exp] of Object.entries(EXPECTED_PENDING)) {
    const m = onDisk.find(x => x.version === v);
    if (!m || m.name !== exp.name || m.checksum !== exp.checksum) die(`tracked ${v} does not match git HEAD`);
  }

  const pool = new Pool({ connectionString: databaseUrl });
  const client = await pool.connect();
  let committed = false;
  try {
    await client.query('BEGIN');
    await client.query('SELECT pg_advisory_xact_lock($1::bigint)', [LOCK_KEY]);   // same lock as the runner
    await client.query('SET LOCAL search_path TO public');

    // The runner's own history check, inside the lock, before any write.
    const history = await M.verifyHistory(client, MIGRATIONS);                  // throws on name/checksum drift or gaps
    if (history.state.kind !== 'tracked') die(`ledger state is ${history.state.kind}, expected tracked`);
    const appliedVersions = history.applied.map(r => r.version);
    const expectedApplied = Array.from({ length: 21 }, (_, i) => String(i + 1).padStart(4, '0'));
    if (JSON.stringify(appliedVersions) !== JSON.stringify(expectedApplied)) {
      die(`ledger must hold exactly 0001-0021; found ${appliedVersions.join(',')}`);
    }
    const pending = history.migrations.filter(m => !appliedVersions.includes(m.version));
    const pendingVersions = pending.map(m => m.version);
    if (JSON.stringify(pendingVersions) !== JSON.stringify(Object.keys(EXPECTED_PENDING))) {
      die(`pending must be exactly 0022 and 0023; found ${pendingVersions.join(',') || 'none'}`);
    }
    for (const m of pending) {
      const exp = EXPECTED_PENDING[m.version];
      if (m.name !== exp.name || m.checksum !== exp.checksum) die(`pending ${m.version} differs from git HEAD`);
    }

    await client.query(RECON_SQL);                                             // reconciliation
    for (const m of pending) {                                                  // tracked migrations, unmodified
      await client.query(m.sql);
      await client.query(`INSERT INTO public.${M.LEDGER} (version, name, checksum) VALUES ($1,$2,$3)`, [m.version, m.name, m.checksum]);
    }

    // Post-checks inside the transaction.
    const notices = (await client.query('SELECT version, publication_status, is_active FROM privacy_notice_versions ORDER BY created_at')).rows;
    const active = notices.filter(n => n.is_active);
    if (active.length !== 1 || active[0].version !== '0.2' || active[0].publication_status !== 'DRAFT') die('post-check: active notice is not 0.2 DRAFT');
    if (!notices.some(n => n.version === '0.1-TEST' && !n.is_active && n.publication_status === 'PUBLISHED')) die('post-check: 0.1-TEST not preserved as inactive PUBLISHED');
    const after = await M.verifyHistory(client, MIGRATIONS);
    if (after.applied.length !== 23) die('post-check: ledger does not hold 23 verified rows');

    await client.query('COMMIT');
    committed = true;
    console.log('COMMITTED: reconciliation, 0022 and 0023 applied in one transaction.');
  } catch (e) {
    try { await client.query('ROLLBACK'); console.error('ROLLED BACK: no change was committed.'); } catch (_) {}
    console.error('ERROR:', e.message.split('\n')[0]);
    process.exitCode = 1;
  } finally {
    client.release();
  }
  if (committed) {
    const planAfter = await M.plan(pool, MIGRATIONS);
    console.log('pending after commit:', JSON.stringify(planAfter.pending.map(p => p.version)));
    M.assertNoPendingMigrations(planAfter);
    console.log('assertNoPendingMigrations: PASS');
  }
  await pool.end();
}
main().catch(e => { console.error('ERROR', e.message); process.exit(1); });
