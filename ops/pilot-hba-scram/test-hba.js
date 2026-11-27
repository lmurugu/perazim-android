// Scratch-only tests for hba-scram-cutover.js, run together with rotate-db-password.js (in ../pilot-password-rotation).
// Needs the scratch cluster from scratch-up.sh (postgres OS user, 127.0.0.1:55556, local socket /tmp/pgscratch/hba-sock).
// Nothing here touches the pilot. Passwords are generated for each run and never printed.
const { spawnSync, execFileSync } = require('child_process');
const crypto = require('crypto');
const fs = require('fs');
const os = require('os');
const path = require('path');
const HERE = __dirname;
const CUTOVER = path.join(HERE, 'hba-scram-cutover.js');
const ROTATE = path.join(HERE, '../pilot-password-rotation/rotate-db-password.js');
const BACKUP_PG = path.join(HERE, '../pilot-reconciliation-v3/backup-pilot.sh');
const CLONE = process.env.CLONE_SERVER || path.resolve(HERE, '../../server');
const { Client } = require(path.join(CLONE, 'node_modules/pg'));

const PORT = process.env.SCRATCH_HBA_PORT || '55556';
const SOCK = process.env.SCRATCH_HBA_SOCK || '/tmp/pgscratch/hba-sock';
const ROLE = 'perazim_hba_app';
const DB = 'perazim_hba_test';
const ROLE_MD5 = 'perazim_hba_md5';
const PSQL = '/usr/lib/postgresql/16/bin/psql';
const ADMIN_CMD = `runuser -u postgres -- ${PSQL} -X -q -t -A -v ON_ERROR_STOP=1 -h ${SOCK} -p ${PORT} -U postgres -d postgres`;
const HBA_GET_CMD = 'runuser -u postgres -- cat "$HBA_FILE"';
const HBA_PUT_CMD = 'runuser -u postgres -- sh -c \'umask 077; cat > "$HBA_FILE.new" && mv "$HBA_FILE.new" "$HBA_FILE"\'';
const BASELINE = [
  '# baseline: trust, as the pilot starts (scratch only)',
  'local   all   all                 trust',
  'host    all   all   127.0.0.1/32  trust',
  'host    all   all   ::1/128       trust',
  'local   replication all           trust',
  'host    replication all 127.0.0.1/32 trust',
  'host    replication all ::1/128      trust',
  ''
].join('\n');

const OLD = crypto.randomBytes(24).toString('hex');
const WRONG = crypto.randomBytes(24).toString('hex');
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'hba-test-'));
const BACKUP_DIR = path.join(dir, 'hba-backups');
const ENV_DIR = path.join(dir, 'env');
fs.mkdirSync(ENV_DIR, { recursive: true, mode: 0o700 });
const envFile = path.join(ENV_DIR, 'api.env');
let failures = 0;
const report = (ok, label, detail = '') => { if (!ok) failures++; console.log(`${ok ? 'PASS' : 'FAIL'} ${label}${detail ? ': ' + detail : ''}`); };
const urlFor = pw => `postgresql://${ROLE}:${pw}@127.0.0.1:${PORT}/${DB}`;

function sh(cmd, input = '', extraEnv = {}) {
  return spawnSync('sh', ['-c', cmd], { input, encoding: 'utf8', env: { ...process.env, ...extraEnv } });
}
const admin = sql => sh(ADMIN_CMD, sql);
const adminOut = sql => { const r = admin(sql); if (r.status !== 0) throw new Error('admin failed: ' + r.stderr.split('\n')[0]); return r.stdout.trim(); };
const hbaPath = () => adminOut("SELECT current_setting('hba_file');");
const hbaText = () => { const r = sh(HBA_GET_CMD, '', { HBA_FILE: hbaPath() }); if (r.status !== 0 || !r.stdout) throw new Error('cannot read pg_hba.conf: ' + r.stderr); return r.stdout; };
const putHba = text => { const r = sh(HBA_PUT_CMD, text, { HBA_FILE: hbaPath() }); if (r.status !== 0) throw new Error('cannot write pg_hba.conf: ' + r.stderr); return r; };
const reload = () => adminOut('SELECT pg_reload_conf();');

function cutover(args, env = {}) {
  return spawnSync('node', [CUTOVER, ...args], {
    encoding: 'utf8',
    env: {
      PATH: process.env.PATH, HOME: process.env.HOME, CLONE_SERVER: CLONE,
      ADMIN_CMD, HBA_GET_CMD, HBA_PUT_CMD, ROLE, TEST_URL: urlFor(OLD), BACKUP_DIR,
      ...env
    }
  });
}
function rotate(args) {
  return spawnSync('node', [ROTATE, ...args], {
    encoding: 'utf8',
    env: { PATH: process.env.PATH, HOME: process.env.HOME, ENV_FILE: envFile, DB_ADMIN_CMD: ADMIN_CMD, CLONE_SERVER: CLONE }
  });
}
async function accepts(url) {
  const c = new Client({ connectionString: url, connectionTimeoutMillis: 4000 });
  try { await c.connect(); await c.query('SELECT 1'); return true; } catch (_) { return false; } finally { try { await c.end(); } catch (_) {} }
}
const readEnvUrl = () => fs.readFileSync(envFile, 'utf8').match(/^DATABASE_URL=(.*)$/m)[1];
const newestBackup = (d, prefix) => fs.readdirSync(d).filter(f => f.startsWith(prefix)).sort().pop();

(async () => {
  // Setup. The cluster starts from trust rules; this makes sure of it and creates a fresh role and database.
  putHba(BASELINE); reload();
  const setup = admin(`DROP DATABASE IF EXISTS ${DB};
DROP ROLE IF EXISTS ${ROLE_MD5};
DROP ROLE IF EXISTS ${ROLE};
CREATE ROLE ${ROLE} LOGIN PASSWORD '${OLD}';
CREATE DATABASE ${DB} OWNER ${ROLE};
`);
  if (setup.status !== 0) { console.error('setup failed: ' + setup.stderr.split('\n')[0]); process.exit(1); }
  fs.writeFileSync(envFile, `NODE_ENV=production\nPORT=3000\nDATABASE_URL=${urlFor(OLD)}\n`, { mode: 0o600 });
  const original = fs.readFileSync(envFile, 'utf8');

  // Fresh dump, taken while the cluster still trusts local connections (the pre-change dump the script requires).
  const dumpOut = execFileSync(BACKUP_PG, [], {
    env: { ...process.env, DATABASE_URL: `postgresql://postgres@127.0.0.1:${PORT}/${DB}`, BACKUP_DIR: path.join(dir, 'dumps') },
    encoding: 'utf8'
  }).trim().split('\n').pop();
  const dump = dumpOut;
  report(fs.existsSync(dump) && spawnSync('sha256sum', ['-c', dump + '.sha256']).status === 0, 'pre-change dump written with a verifying checksum');

  const baseHba = hbaText();
  report(/\btrust\b/.test(baseHba), 'baseline cluster starts with trust rules');

  // T1: dry run changes nothing.
  const t1 = cutover([], { TEST_URL: urlFor(OLD) });
  report(t1.status === 0 && hbaText() === baseHba, 'T1 dry run passes checks and changes nothing', t1.stdout.trim().split('\n').pop());

  // T2: a login role with a non-SCRAM (md5) password is refused, because it would be locked out.
  const md5 = admin(`SET password_encryption = 'md5';
CREATE ROLE ${ROLE_MD5} LOGIN PASSWORD 'md5-scratch-only';
`);
  const t2 = cutover(['--apply', `--confirm=${DB}`], { PRE_CHANGE_DUMP: dump });
  report(md5.status === 0 && t2.status !== 0 && /locked out/.test(t2.stderr) && hbaText() === baseHba,
    'T2 apply refuses when a login role has a non-SCRAM password, and changes nothing', (t2.stderr || '').trim().split('\n')[0]);
  admin(`DROP ROLE IF EXISTS ${ROLE_MD5};`);

  // T3: the cutover applies. Current password works, wrong and missing passwords are refused, admin socket still works.
  const t3 = cutover(['--apply', `--confirm=${DB}`], { PRE_CHANGE_DUMP: dump });
  const m = /Pre-change pg_hba\.conf: (\S+)/.exec(t3.stdout || '');
  const backupHba = m ? m[1] : '';
  report(t3.status === 0 && /COMMITTED/.test(t3.stdout) && backupHba !== '', 'T3 apply commits', (t3.stderr || t3.stdout).trim().split('\n')[0]);
  const scramNow = /^host\s+all\s+all\s+all\s+scram-sha-256$/m.test(hbaText());
  report(scramNow, 'T3 host rule is scram-sha-256 for all addresses');
  const goodOk = await accepts(urlFor(OLD));
  const wrongRefused = !(await accepts(urlFor(WRONG)));
  const noPwRefused = !(await accepts(`postgresql://${ROLE}@127.0.0.1:${PORT}/${DB}`));
  report(goodOk && wrongRefused && noPwRefused, 'T3 current password accepted; wrong and missing passwords refused over TCP');
  report(adminOut('SELECT 1;') === '1', 'T3 admin path over the local socket still works after the cutover');

  // T4: a bad rule in the new file is refused and the previous file is restored.
  const pre4 = hbaText();
  const t4 = cutover(['--apply', `--confirm=${DB}`], { PRE_CHANGE_DUMP: dump, TEST_ONLY_EXTRA_HBA_LINE: 'hostt all all all scram-sha-256' });
  report(t4.status !== 0 && /rejected|previous file restored/.test(t4.stderr) && hbaText() === pre4,
    'T4 bad pg_hba line is refused and the previous file is restored', (t4.stderr || '').trim().split('\n')[0]);

  // T5: with host auth now SCRAM, rotation proves the OLD password is refused and the NEW one works.
  const t5 = rotate(['--apply', `--confirm=${ROLE}`]);
  const newUrl = readEnvUrl();
  report(t5.status === 0 && /old password refused/.test(t5.stdout) && !(await accepts(urlFor(OLD))) && (await accepts(newUrl)),
    'T5 rotation under SCRAM: old password refused, new accepted, env updated', (t5.stdout || t5.stderr).trim().split('\n').pop());
  report(!(t5.stdout + t5.stderr).includes(OLD) && !(t5.stdout + t5.stderr).includes(readEnvUrl().split(':').pop()), 'T5 no password value printed');

  // T6: rollback in the coordinated order. Rotation first (restores the old password and the env file), then pg_hba.
  const envBackup = path.join(ENV_DIR, newestBackup(ENV_DIR, 'api.env.before-rotation-') || 'missing');
  const t6a = rotate(['--rollback', envBackup, `--confirm=${ROLE}`]);
  report(t6a.status === 0 && await accepts(urlFor(OLD)) && fs.readFileSync(envFile, 'utf8') === original,
    'T6a rotation rollback restores the old password and the env file', (t6a.stderr || t6a.stdout).trim().split('\n').pop());
  const t6b = cutover(['--rollback', backupHba, `--confirm=${DB}`]);
  report(t6b.status === 0 && hbaText() === fs.readFileSync(backupHba, 'utf8'),
    'T6b pg_hba rollback restores the pre-change file exactly', (t6b.stderr || t6b.stdout).trim().split('\n').pop());
  report(adminOut('SELECT 1;') === '1', 'T6b admin path still works after the rollback');
  report(!/scram-sha-256/.test(hbaText().split('\n').filter(l => /^host\s/.test(l)).join('\n')), 'T6b host rules are back to trust');

  console.log(failures ? `${failures} FAILURE(S)` : 'ALL PASS');
  process.exit(failures ? 1 : 0);
})().catch(e => { console.error('test harness error: ' + e.message); process.exit(1); });
