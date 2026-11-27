// Scratch-only. For each ledger tamper, the script must refuse BEFORE any write, and the database
// must be unchanged (notices + ledger + games tables) compared with before the run. Also: an injected
// mid-transaction failure must roll back fully, and a clean run must commit.
const { spawnSync } = require('child_process');
const path = require('path');
const crypto = require('crypto');
const { CLONE, SCRATCH_PG, MIG_A, MIG_B } = require('./scratch-env');
;
const { Pool } = require(CLONE + '/node_modules/pg');
const M = require(CLONE + '/src/migrations.js');
const HERE = __dirname;
const sha = s => crypto.createHash('sha256').update(s).digest('hex');
const ADMIN = SCRATCH_PG + '/postgres';
const DUMP = process.env.PRE_CHANGE_DUMP_FOR_TESTS;

async function freshPilotLike(db) {
  const admin = new Pool({ connectionString: ADMIN });
  await admin.query(`DROP DATABASE IF EXISTS ${db}`); await admin.query(`CREATE DATABASE ${db}`); await admin.end();
  const pool = new Pool({ connectionString: `${SCRATCH_PG}/${db}` });
  await M.migrate(pool, MIG_A);
  await pool.query("DELETE FROM privacy_notice_versions WHERE version='0.1'");
  await pool.query(`INSERT INTO privacy_notice_versions (version,content_hash,document_path,publication_status,effective_at,is_active,created_at)
    VALUES ('0.1-TEST',$1,'test-privacy-notice.md','PUBLISHED',$2,TRUE,$3)`, [sha('x'), Date.now() - 86400000, Date.now() - 172800000]);
  return pool;
}
async function snapshot(pool) {
  const n = (await pool.query('SELECT version, publication_status, is_active, content_hash FROM privacy_notice_versions ORDER BY version')).rows;
  const l = (await pool.query('SELECT version, name, checksum FROM perazim_schema_migrations ORDER BY version')).rows;
  const t = (await pool.query("SELECT to_regclass('public.game_score_events') IS NOT NULL AS g")).rows[0].g;
  return JSON.stringify({ n, l, t });
}
function run(db) {
  return spawnSync('node', [path.join(HERE, 'reconcile-and-migrate.js'), `--confirm=${db}`], {
    env: { ...process.env, DATABASE_URL: `${SCRATCH_PG}/${db}`, MIGRATE_CONFIRM: db, PRE_CHANGE_DUMP: DUMP, CLONE_SERVER: CLONE },
    encoding: 'utf8'
  });
}

const cases = [
  ['wrong checksum for 0010', "UPDATE perazim_schema_migrations SET checksum = repeat('f',64) WHERE version='0010'"],
  ['wrong name for 0005', "UPDATE perazim_schema_migrations SET name = '0005_wrong_name.sql' WHERE version='0005'"],
  ['wrong version: 0021 recorded as 0031', "UPDATE perazim_schema_migrations SET version = '0031' WHERE version='0021'"],
  ['unknown extra version 0024', "INSERT INTO perazim_schema_migrations (version,name,checksum) VALUES ('0024','0024_fake.sql',repeat('a',64))"],
  ['gap: 0003 missing', "DELETE FROM perazim_schema_migrations WHERE version='0003'"],
  ['0022 already recorded', "INSERT INTO perazim_schema_migrations (version,name,checksum) VALUES ('0022','0022_games_leaderboard.sql','c27c05e4ecab95668c65e3ba7ca996c5e5f7f7727fb237af1b9094cacfda43b0')"]
];

(async () => {
  let failures = 0;
  for (let i = 0; i < cases.length; i++) {
    const [label, sql] = cases[i];
    const db = `perazim_drift_${i}`;
    const pool = await freshPilotLike(db);
    await pool.query(sql);
    const before = await snapshot(pool);
    const r = run(db);
    const after = await snapshot(pool);
    const refused = r.status !== 0 && /ERROR:/.test(r.stderr || '');
    const unchanged = before === after;
    if (!refused || !unchanged) failures++;
    console.log(`${refused && unchanged ? 'PASS' : 'FAIL'} ${label}: exit ${r.status}; ${(r.stderr || '').split('\n').filter(Boolean).slice(-1)[0].slice(0, 110)}; unchanged=${unchanged}`);
    await pool.end();
  }

  const db = 'perazim_drift_inject';
  const pool = await freshPilotLike(db);
  const before = await snapshot(pool);
  await pool.query(`CREATE FUNCTION scratch_fail_0_2() RETURNS trigger AS $$ BEGIN IF NEW.version = '0.2' THEN RAISE EXCEPTION 'SCRATCH INJECTED FAILURE'; END IF; RETURN NEW; END $$ LANGUAGE plpgsql`);
  await pool.query('CREATE TRIGGER scratch_fail BEFORE INSERT ON privacy_notice_versions FOR EACH ROW EXECUTE FUNCTION scratch_fail_0_2()');
  const r1 = run(db);
  const rolledBack = r1.status !== 0 && (await snapshot(pool)) === before;
  if (!rolledBack) failures++;
  console.log(`${rolledBack ? 'PASS' : 'FAIL'} injected failure after 0022 ran: exit ${r1.status}; rolled back=${rolledBack}`);
  await pool.query('DROP TRIGGER scratch_fail ON privacy_notice_versions; DROP FUNCTION scratch_fail_0_2()');

  const r2 = run(db);
  const plan = await M.plan(pool, MIG_B);
  const committed = r2.status === 0 && plan.pending.length === 0;
  if (!committed) failures++;
  console.log(`${committed ? 'PASS' : 'FAIL'} clean run: exit ${r2.status}; ${r2.stdout.trim().split('\n').join(' / ')}`);
  await pool.end();
  console.log(failures === 0 ? 'ALL PASS' : `${failures} FAILURE(S)`);
  process.exit(failures ? 1 : 0);
})().catch(e => { console.error('ERROR', e.stack || e.message); process.exit(1); });
