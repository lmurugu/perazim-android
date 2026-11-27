// Scratch-only tests for reencrypt-totp.js. Uses a throwaway Postgres (SCRATCH_PG) and never the pilot.
// Keys are generated for each test run and never printed.
const { spawnSync, execFileSync } = require('child_process');
const path = require('path');
const crypto = require('crypto');
const { CLONE, SCRATCH_PG, MIG_B } = require('../pilot-reconciliation-v3/scratch-env');
const { Pool } = require(path.join(CLONE, 'node_modules/pg'));
const M = require(path.join(CLONE, 'src/migrations.js'));
const { encryptSecret, decryptSecret } = require(path.join(CLONE, 'src/phase-b/totp.js'));
const HERE = __dirname;
const SCRIPT = path.join(HERE, 'reencrypt-totp.js');
const OLD = crypto.randomBytes(32).toString('hex');
const NEW = crypto.randomBytes(32).toString('hex');
const OTHER = crypto.randomBytes(32).toString('hex');
let failures = 0;
const report = (ok, label, detail = '') => { if (!ok) failures++; console.log(`${ok ? 'PASS' : 'FAIL'} ${label}${detail ? ': ' + detail : ''}`); };
const withKey = (k, fn) => { const p = process.env.PHASE_B_ADMIN_KEY; process.env.PHASE_B_ADMIN_KEY = k; try { return fn(); } finally { if (p === undefined) delete process.env.PHASE_B_ADMIN_KEY; else process.env.PHASE_B_ADMIN_KEY = p; } };

async function freshDb(name, seed) {
  const admin = new Pool({ connectionString: SCRATCH_PG + '/postgres' });
  await admin.query(`DROP DATABASE IF EXISTS ${name}`); await admin.query(`CREATE DATABASE ${name}`); await admin.end();
  const pool = new Pool({ connectionString: `${SCRATCH_PG}/${name}` });
  await M.migrate(pool, MIG_B);
  const secrets = {};
  for (const id of ['u-alpha', 'u-bravo', 'u-charlie']) {
    await pool.query(`INSERT INTO users (id, name, phone, pin_hash, is_active, created_at, updated_at) VALUES ($1,'Test',$2,'x',TRUE,1,1)`, [id, '+2547' + String(Math.floor(Math.random() * 1e8)).padStart(8, '0')]);
    if (seed) {
      secrets[id] = 'JBSWY3DPEHPK3PXP' + id.length;
      await pool.query('INSERT INTO admin_security (user_id,totp_secret_cipher,totp_last_counter,security_version,auth_not_before,created_at) VALUES ($1,$2,7,3,99,1)',
        [id, withKey(OLD, () => encryptSecret(secrets[id]))]);
    }
  }
  return { pool, secrets };
}
async function snapshot(pool) {
  return JSON.stringify((await pool.query('SELECT user_id,totp_secret_cipher,totp_last_counter,security_version,auth_not_before FROM admin_security ORDER BY user_id')).rows);
}
function run(db, env, args = []) {
  return spawnSync('node', [SCRIPT, ...args], {
    env: { PATH: process.env.PATH, HOME: process.env.HOME, DATABASE_URL: `${SCRATCH_PG}/${db}`, OLD_PHASE_B_ADMIN_KEY: OLD, NEW_PHASE_B_ADMIN_KEY: NEW, CLONE_SERVER: CLONE, ...env },
    encoding: 'utf8'
  });
}
const dumpFor = db => execFileSync(path.join(HERE, '../pilot-reconciliation-v3/backup-pilot.sh'), { env: { ...process.env, DATABASE_URL: `${SCRATCH_PG}/${db}`, BACKUP_DIR: path.join(HERE, 'dumps') }, encoding: 'utf8' }).trim();

(async () => {
  // T1 dry run: decrypts every row, writes nothing.
  let { pool, secrets } = await freshDb('perazim_totp_t1', true);
  const s1 = await snapshot(pool);
  let r = run('perazim_totp_t1', {});
  report(r.status === 0 && /DRY RUN/.test(r.stdout) && /decrypted with OLD key: 3 of 3/.test(r.stdout), 'T1 dry run decrypts 3 of 3 and exits 0', r.stdout.trim().split('\n').slice(-2).join(' / '));
  report(s1 === await snapshot(pool), 'T1 dry run leaves the table unchanged');
  await pool.end();

  // T2 apply: rows re-encrypted, decrypt with NEW to the same secret, counters and security_version unchanged.
  r = run('perazim_totp_t1', { PRE_CHANGE_DUMP: dumpFor('perazim_totp_t1') }, ['--apply', '--confirm=perazim_totp_t1']);
  pool = new Pool({ connectionString: `${SCRATCH_PG}/perazim_totp_t1` });
  const rows = (await pool.query('SELECT user_id,totp_secret_cipher,totp_last_counter,security_version,auth_not_before FROM admin_security ORDER BY user_id')).rows;
  const allNew = rows.every(row => withKey(NEW, () => decryptSecret(row.totp_secret_cipher)) === secrets[row.user_id]);
  const allOldFails = rows.every(row => { try { withKey(OLD, () => decryptSecret(row.totp_secret_cipher)); return false; } catch (_) { return true; } });
  report(r.status === 0 && allNew && allOldFails && rows.every(x => Number(x.totp_last_counter) === 7 && x.security_version === 3 && Number(x.auth_not_before) === 99),
    'T2 apply re-encrypts all rows; secrets preserved; counters and security_version untouched', r.stdout.trim().split('\n').slice(-1)[0]);
  await pool.end();

  // T3 wrong OLD key: refused, nothing changed.
  let { pool: p3 } = await freshDb('perazim_totp_t3', true);
  const s3 = await snapshot(p3);
  r = run('perazim_totp_t3', { OLD_PHASE_B_ADMIN_KEY: OTHER, PRE_CHANGE_DUMP: dumpFor('perazim_totp_t3') }, ['--apply', '--confirm=perazim_totp_t3']);
  report(r.status !== 0 && /STOPPED/.test(r.stderr) && s3 === await snapshot(p3), 'T3 wrong OLD key refused, table unchanged', r.stderr.trim());
  await p3.end();

  // T4 one malformed row: whole run refused, the good rows are not changed either.
  let { pool: p4 } = await freshDb('perazim_totp_t4', true);
  await p4.query("UPDATE admin_security SET totp_secret_cipher='not-a-valid-secret' WHERE user_id='u-bravo'");
  const s4 = await snapshot(p4);
  r = run('perazim_totp_t4', { PRE_CHANGE_DUMP: dumpFor('perazim_totp_t4') }, ['--apply', '--confirm=perazim_totp_t4']);
  report(r.status !== 0 && s4 === await snapshot(p4), 'T4 malformed row refused, no row changed', r.stderr.trim());
  await p4.end();

  // T5 failure on the third update: trigger rejects it; the first two updates must roll back.
  let { pool: p5 } = await freshDb('perazim_totp_t5', true);
  await p5.query(`CREATE FUNCTION scratch_reject_charlie() RETURNS trigger AS $$ BEGIN IF NEW.user_id='u-charlie' THEN RAISE EXCEPTION 'SCRATCH REJECT'; END IF; RETURN NEW; END $$ LANGUAGE plpgsql`);
  await p5.query('CREATE TRIGGER scratch_reject BEFORE UPDATE ON admin_security FOR EACH ROW EXECUTE FUNCTION scratch_reject_charlie()');
  const s5 = await snapshot(p5);
  r = run('perazim_totp_t5', { PRE_CHANGE_DUMP: dumpFor('perazim_totp_t5') }, ['--apply', '--confirm=perazim_totp_t5']);
  report(r.status !== 0 && s5 === await snapshot(p5), 'T5 failure on the last row rolls back the earlier rows', r.stderr.trim());
  await p5.end();

  // T6 zero rows: bootstrap path validates the NEW key and reports nothing to re-encrypt.
  let { pool: p6 } = await freshDb('perazim_totp_t6', false);
  r = run('perazim_totp_t6', { PRE_CHANGE_DUMP: dumpFor('perazim_totp_t6') }, ['--apply', '--confirm=perazim_totp_t6']);
  report(r.status === 0 && /admin_security rows: 0/.test(r.stdout) && /bootstrap path/.test(r.stdout), 'T6 zero rows: bootstrap path, exit 0', r.stdout.trim().split('\n')[0]);
  await p6.end();

  // T7 guards.
  r = run('perazim_totp_t1', { NEW_PHASE_B_ADMIN_KEY: OLD });
  report(r.status !== 0 && /identical/.test(r.stderr), 'T7a identical keys refused');
  r = run('perazim_totp_t1', { NEW_PHASE_B_ADMIN_KEY: 'short' });
  report(r.status !== 0 && !/short/.test(r.stderr + r.stdout), 'T7b malformed NEW key refused without echoing it');
  r = run('perazim_totp_t1', {}, ['--apply']);
  report(r.status !== 0 && /--confirm/.test(r.stderr), 'T7c --apply without --confirm refused');

  // T8 rollback by key swap: re-running with OLD and NEW exchanged restores decryptability with OLD.
  let { pool: p8, secrets: secrets8 } = await freshDb('perazim_totp_t8', true);
  const dump8 = dumpFor('perazim_totp_t8');
  r = run('perazim_totp_t8', { PRE_CHANGE_DUMP: dump8 }, ['--apply', '--confirm=perazim_totp_t8']);
  r = run('perazim_totp_t8', { OLD_PHASE_B_ADMIN_KEY: NEW, NEW_PHASE_B_ADMIN_KEY: OLD, PRE_CHANGE_DUMP: dump8 }, ['--apply', '--confirm=perazim_totp_t8']);
  const back = (await p8.query('SELECT user_id,totp_secret_cipher FROM admin_security ORDER BY user_id')).rows;
  report(r.status === 0 && back.every(x => withKey(OLD, () => decryptSecret(x.totp_secret_cipher)) === secrets8[x.user_id]) && back.length === 3,
    'T8 rollback by key swap restores decryptability with OLD');
  await p8.end();

  // T9 rollback by dump: restoring the pre-change dump brings back the exact original ciphertext.
  let { pool: p9 } = await freshDb('perazim_totp_t9', true);
  const original9 = await snapshot(p9);
  const dump9 = dumpFor('perazim_totp_t9');
  r = run('perazim_totp_t9', { PRE_CHANGE_DUMP: dump9 }, ['--apply', '--confirm=perazim_totp_t9']);
  const restore = spawnSync('sh', ['-c', `pg_restore --clean --if-exists --no-owner --exit-on-error --dbname=${SCRATCH_PG}/perazim_totp_t9 "${dump9}"`], { encoding: 'utf8' });
  report(r.status === 0 && restore.status === 0 && (await snapshot(p9)) === original9, 'T9 restoring the pre-change dump gives the exact original rows', restore.stderr.trim().split('\n')[0] || '');
  await p9.end();

  console.log(failures === 0 ? 'ALL PASS' : `${failures} FAILURE(S)`);
  process.exit(failures ? 1 : 0);
})().catch(e => { console.error('ERROR', e.stack || e.message); process.exit(1); });
