// Scratch-only tests for the atomic path, the failure rollback, and the recovery restore.
const { spawnSync, execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { CLONE, SCRATCH_PG, MIG_A, MIG_B } = require('./scratch-env');
const { Pool } = require(CLONE + '/node_modules/pg');
const M = require(CLONE + '/src/migrations.js');
const HERE = __dirname;
const variant = process.argv[2] || 'V1';
const db = 'perazim_atomic_' + variant.toLowerCase();
const url = SCRATCH_PG + '/' + db;
const sha = s => crypto.createHash('sha256').update(s).digest('hex');
const log = (...a) => console.log(`[${variant}]`, ...a);

function runScript() {
  return spawnSync('node', [path.join(HERE, 'reconcile-and-migrate.js'), `--confirm=${db}`], {
    env: { ...process.env, DATABASE_URL: url, MIGRATE_CONFIRM: db, PRE_CHANGE_DUMP: process.env.PRE_CHANGE_DUMP, CLONE_SERVER: CLONE },
    encoding: 'utf8'
  });
}
async function state(pool) {
  const notices = (await pool.query('SELECT version, publication_status, is_active FROM privacy_notice_versions ORDER BY created_at')).rows;
  const ledger = (await pool.query("SELECT version FROM perazim_schema_migrations ORDER BY version")).rows.map(r => r.version);
  const tables = (await pool.query("SELECT to_regclass('public.game_score_events') IS NOT NULL AS games")).rows[0].games;
  return { notices: notices.map(n => `${n.version}:${n.publication_status}:${n.is_active ? 'ACTIVE' : 'inactive'}`).join(', '), ledgerCount: ledger.length, has0022: ledger.includes('0022'), has0023: ledger.includes('0023'), gamesTables: tables };
}
(async () => {
  const admin = new Pool({ connectionString: SCRATCH_PG + '/postgres' });
  await admin.query(`DROP DATABASE IF EXISTS ${db}`); await admin.query(`CREATE DATABASE ${db}`); await admin.end();
  const pool = new Pool({ connectionString: url });
  await M.migrate(pool, MIG_A);
  if (variant === 'V1') await pool.query("DELETE FROM privacy_notice_versions WHERE version='0.1'");
  else await pool.query("UPDATE privacy_notice_versions SET is_active=FALSE WHERE version='0.1'");
  await pool.query(`INSERT INTO privacy_notice_versions (version,content_hash,document_path,publication_status,effective_at,is_active,created_at)
    VALUES ('0.1-TEST',$1,'test-privacy-notice.md','PUBLISHED',$2,TRUE,$3)`, [sha('x'), Date.now() - 86400000, Date.now() - 172800000]);
  log('start:', JSON.stringify(await state(pool)));

  // Dump before any change, verified, through the same backup script the operator will use.
  const dumpDir = path.join(HERE, 'dumps-' + variant); fs.mkdirSync(dumpDir, { recursive: true });
  const dump = execFileSync(path.join(HERE, 'backup-pilot.sh'), { env: { ...process.env, DATABASE_URL: url, BACKUP_DIR: dumpDir }, encoding: 'utf8' }).trim();
  log('pre-change dump:', path.basename(dump), 'sha256 file exists:', fs.existsSync(dump + '.sha256'));
  process.env.PRE_CHANGE_DUMP = dump;

  // Test 1: failure part-way (after the reconciliation and 0022 have run) must roll everything back.
  await pool.query(`CREATE FUNCTION scratch_fail_0_2() RETURNS trigger AS $$ BEGIN IF NEW.version = '0.2' THEN RAISE EXCEPTION 'SCRATCH INJECTED FAILURE'; END IF; RETURN NEW; END $$ LANGUAGE plpgsql`);
  await pool.query('CREATE TRIGGER scratch_fail BEFORE INSERT ON privacy_notice_versions FOR EACH ROW EXECUTE FUNCTION scratch_fail_0_2()');
  const fail = runScript();
  log('T1 injected failure: exit', fail.status, '|', (fail.stderr || '').split('\n').filter(Boolean).slice(-2).join(' / '));
  log('T1 state after rollback:', JSON.stringify(await state(pool)));
  await pool.query('DROP TRIGGER scratch_fail ON privacy_notice_versions; DROP FUNCTION scratch_fail_0_2()');

  // Test 2: clean run commits everything in one transaction.
  const ok = runScript();
  log('T2 clean run: exit', ok.status, '|', ok.stdout.trim().split('\n').join(' / '), '|', (ok.stderr || '').trim());
  log('T2 state after commit:', JSON.stringify(await state(pool)));
  const again = runScript();
  log('T2 rerun refused: exit', again.status, '|', (again.stderr || '').split('\n').filter(Boolean)[0]);

  // Test 3: recovery from the pre-change dump restores the pre-change state.
  await pool.end();
  const newdb = db + '_recovered';
  const admin2 = new Pool({ connectionString: SCRATCH_PG + '/postgres' });
  await admin2.query(`DROP DATABASE IF EXISTS ${newdb}`); await admin2.end();
  const rec = spawnSync(path.join(HERE, 'recover-from-backup.sh'), [dump, newdb], { env: { ...process.env, PILOT_HOST_URL: SCRATCH_PG }, encoding: 'utf8' });
  log('T3 recovery: exit', rec.status, (rec.stdout || '').trim(), (rec.stderr || '').trim().split('\n').slice(-2).join(' / '));
  const p2 = new Pool({ connectionString: SCRATCH_PG + '/' + newdb });
  log('T3 state after restore:', JSON.stringify(await state(p2)));
  const plan = await M.plan(p2, MIG_B);
  log('T3 pending after restore:', JSON.stringify(plan.pending.map(p => p.version)));
  await p2.end();
})().catch(e => { console.error('ERROR', e.stack || e.message); process.exit(1); });
