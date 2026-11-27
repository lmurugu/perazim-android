// Scratch-only. Changed, renamed, missing or extra tracked migration files on disk must be refused
// before any write. For each case the database must be unchanged.
const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { CLONE, SCRATCH_PG, MIG_A, MIG_B } = require('./scratch-env');
const { Pool } = require(CLONE + '/node_modules/pg');
const M = require(CLONE + '/src/migrations.js');
const HERE = __dirname;
const sha = s => crypto.createHash('sha256').update(s).digest('hex');
const ADMIN = SCRATCH_PG + '/postgres';
const DUMP = process.env.PRE_CHANGE_DUMP_FOR_TESTS;
const SRC_MIG = MIG_B;   // byte-identical to git HEAD for 0001-0023 (verified earlier)
const FAKE = process.env.FAKE_SERVER || require('path').join(require('os').tmpdir(), 'pilot-recon-fake-server');

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
// Build a fake server tree whose migrations folder is a copy we may tamper with.
function freshFakeServer() {
  fs.rmSync(path.join(FAKE, 'migrations'), { recursive: true, force: true });
  fs.mkdirSync(path.join(FAKE, 'migrations'), { recursive: true });
  for (const f of fs.readdirSync(SRC_MIG)) fs.copyFileSync(path.join(SRC_MIG, f), path.join(FAKE, 'migrations', f));
  for (const link of ['node_modules', 'src']) {
    const p = path.join(FAKE, link);
    if (!fs.existsSync(p)) fs.symlinkSync(path.join(CLONE, link), p);
  }
}
function run(db) {
  return spawnSync('node', [path.join(HERE, 'reconcile-and-migrate.js'), `--confirm=${db}`], {
    env: { ...process.env, DATABASE_URL: `${SCRATCH_PG}/${db}`, MIGRATE_CONFIRM: db, PRE_CHANGE_DUMP: DUMP, CLONE_SERVER: FAKE },
    encoding: 'utf8'
  });
}
const M_DIR = path.join(FAKE, 'migrations');
const cases = [
  ['0022 content changed on disk', () => fs.appendFileSync(path.join(M_DIR, '0022_games_leaderboard.sql'), '\n-- tampered\n')],
  ['0023 content changed on disk', () => fs.appendFileSync(path.join(M_DIR, '0023_privacy_notice_v02.sql'), '\n-- tampered\n')],
  ['0023 renamed on disk', () => fs.renameSync(path.join(M_DIR, '0023_privacy_notice_v02.sql'), path.join(M_DIR, '0023_privacy_notice_v02_renamed.sql'))],
  ['0023 missing on disk', () => fs.unlinkSync(path.join(M_DIR, '0023_privacy_notice_v02.sql'))],
  ['0022 missing on disk (gap)', () => fs.unlinkSync(path.join(M_DIR, '0022_games_leaderboard.sql'))],
  ['extra 0024 file on disk', () => fs.writeFileSync(path.join(M_DIR, '0024_extra.sql'), 'SELECT 1;\n')]
];

(async () => {
  let failures = 0;
  for (let i = 0; i < cases.length; i++) {
    const [label, tamper] = cases[i];
    const db = `perazim_disk_${i}`;
    const pool = await freshPilotLike(db);
    freshFakeServer();
    tamper();
    const before = await snapshot(pool);
    const r = run(db);
    const after = await snapshot(pool);
    const refused = r.status !== 0 && /ERROR/.test(r.stderr || '');
    const unchanged = before === after;
    if (!refused || !unchanged) failures++;
    console.log(`${refused && unchanged ? 'PASS' : 'FAIL'} ${label}: exit ${r.status}; ${(r.stderr || '').split('\n').filter(Boolean).slice(-1)[0].slice(0, 120)}; unchanged=${unchanged}`);
    await pool.end();
  }
  // Control: untampered fake tree commits.
  const db = 'perazim_disk_control';
  const pool = await freshPilotLike(db);
  freshFakeServer();
  const r = run(db);
  const ok = r.status === 0;
  if (!ok) failures++;
  console.log(`${ok ? 'PASS' : 'FAIL'} control (untampered tree): exit ${r.status}; ${r.stdout.trim().split('\n')[0]}`);
  await pool.end();
  console.log(failures === 0 ? 'ALL PASS' : `${failures} FAILURE(S)`);
  process.exit(failures ? 1 : 0);
})().catch(e => { console.error('ERROR', e.stack || e.message); process.exit(1); });
