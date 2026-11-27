// Scratch-only tests for rotate-db-password.js. Uses a throwaway Postgres (SCRATCH_PG), whose pg_hba
// must have a scram line for host connections as the test role (added by the operator before running).
// Passwords are generated for the run and never printed.
const { spawnSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const { CLONE, SCRATCH_PG } = require('../pilot-reconciliation-v3/scratch-env');
const { Client, Pool } = require(path.join(CLONE, 'node_modules/pg'));
const HERE = __dirname;
const SCRIPT = path.join(HERE, 'rotate-db-password.js');
const ROLE = 'perazim_rot_app';
const DB = 'perazim_rot_test';
const OLD = crypto.randomBytes(24).toString('hex');
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'rot-test-'));
const envFile = path.join(dir, 'api.env');
const ADMIN = `psql -X -q -v ON_ERROR_STOP=1 ${SCRATCH_PG}/postgres`;
let failures = 0;
const report = (ok, label, detail = '') => { if (!ok) failures++; console.log(`${ok ? 'PASS' : 'FAIL'} ${label}${detail ? ': ' + detail : ''}`); };
const urlFor = pw => `postgresql://${ROLE}:${pw}@127.0.0.1:55555/${DB}`;
async function accepts(pw) {
  const c = new Client({ connectionString: urlFor(pw), connectionTimeoutMillis: 4000 });
  try { await c.connect(); await c.query('SELECT 1'); return true; } catch (_) { return false; } finally { try { await c.end(); } catch (_) {} }
}
function rot(args, env = {}) {
  return spawnSync('node', [SCRIPT, ...args], {
    env: { PATH: process.env.PATH, HOME: process.env.HOME, ENV_FILE: envFile, DB_ADMIN_CMD: ADMIN, CLONE_SERVER: CLONE, ...env },
    encoding: 'utf8'
  });
}
const writeEnv = (extra = '') => fs.writeFileSync(envFile, `NODE_ENV=production\nPORT=3000\nJWT_SECRET=${crypto.randomBytes(24).toString('hex')}\nDATABASE_URL=${urlFor(OLD)}\n${extra}`, { mode: 0o600 });
const readEnv = () => fs.readFileSync(envFile, 'utf8');

(async () => {
  // Setup: role with password, database owned by it.
  const setup = spawnSync('sh', ['-c', `${ADMIN} -c "DROP DATABASE IF EXISTS ${DB}" -c "DROP ROLE IF EXISTS ${ROLE}" -c "CREATE ROLE ${ROLE} LOGIN PASSWORD '${OLD}'" -c "CREATE DATABASE ${DB} OWNER ${ROLE}"`], { encoding: 'utf8' });
  if (setup.status !== 0) { console.error('setup failed'); process.exit(1); }
  writeEnv();
  const original = readEnv();

  // T1 dry run: checks the current password, changes nothing.
  let r = rot(['--confirm=' + ROLE]);
  report(r.status === 0 && /DRY RUN/.test(r.stdout) && /current password accepted/.test(r.stdout) && readEnv() === original && await accepts(OLD), 'T1 dry run passes checks, changes nothing');

  // T2 apply: new password works, old refused, env updated with mode 600 and a backup of the old env.
  r = rot(['--apply', '--confirm=' + ROLE]);
  const backups = fs.readdirSync(dir).filter(f => f.includes('before-rotation'));
  const envNow = readEnv();
  const newPw = (envNow.match(/DATABASE_URL=postgresql:\/\/[^:]+:([0-9a-f]+)@/) || [])[1] || '';
  const mode = (fs.statSync(envFile).mode & 0o777).toString(8);
  report(r.status === 0 && newPw && newPw !== OLD && await accepts(newPw) && !(await accepts(OLD)) && backups.length === 1 && mode === '600',
    'T2 apply: new password accepted, old refused, env mode 600, backup kept', r.stdout.trim().split('\n').slice(-1)[0]);
  report(!/OLD_SECRET_MARK/.test(r.stdout + r.stderr) && !r.stdout.includes(OLD) && !r.stdout.includes(newPw) && !r.stderr.includes(OLD), 'T2 no password value printed');
  const backupPath = path.join(dir, backups[0]);

  // T3 rollback from the backup env file restores the old password and the old env file.
  r = rot(['--rollback', backupPath, '--confirm=' + ROLE]);
  report(r.status === 0 && await accepts(OLD) && !(await accepts(newPw)) && readEnv() === original, 'T3 rollback restores old password and env file', r.stdout.trim());

  // T4 failure after ALTER ROLE: a wrapper sets a wrong password on the first ALTER only.
  const counter = path.join(dir, 'count');
  const wrapper = path.join(dir, 'admin-wrong-first.sh');
  fs.writeFileSync(wrapper, `#!/bin/sh\nif [ ! -f "${counter}" ]; then touch "${counter}"; sed "s/PASSWORD '[0-9a-f]*'/PASSWORD 'wrong-value-x'/" | ${ADMIN}; else ${ADMIN}; fi\n`, { mode: 0o755 });
  const beforeFail = readEnv();
  r = rot(['--apply', '--confirm=' + ROLE], { DB_ADMIN_CMD: wrapper });
  report(r.status !== 0 && /STOPPED/.test(r.stderr) && readEnv() === beforeFail && await accepts(OLD), 'T4 new password fails: old password restored in DB, env unchanged', r.stderr.trim());
  fs.rmSync(counter, { force: true });

  // T5 guards: wrong confirm, two DATABASE_URL lines, quoted value, no password in URL.
  r = rot(['--apply', '--confirm=other_role']);
  report(r.status !== 0 && readEnv() === beforeFail, 'T5a wrong --confirm refused');
  fs.writeFileSync(envFile, beforeFail + `DATABASE_URL=${urlFor(OLD)}\n`, { mode: 0o600 });
  r = rot(['--dry-run', '--confirm=' + ROLE]);
  report(r.status !== 0 && /exactly one DATABASE_URL/.test(r.stderr), 'T5b two DATABASE_URL lines refused');
  fs.writeFileSync(envFile, beforeFail.replace(`DATABASE_URL=${urlFor(OLD)}`, `DATABASE_URL="${urlFor(OLD)}"`), { mode: 0o600 });
  r = rot(['--confirm=' + ROLE]);
  report(r.status !== 0 && /quoted/.test(r.stderr), 'T5c quoted DATABASE_URL refused');
  fs.writeFileSync(envFile, beforeFail, { mode: 0o600 });

  // Cleanup scratch objects.
  spawnSync('sh', ['-c', `${ADMIN} -c "DROP DATABASE IF EXISTS ${DB}" -c "DROP ROLE IF EXISTS ${ROLE}"`]);
  fs.rmSync(dir, { recursive: true, force: true });
  console.log(failures === 0 ? 'ALL PASS' : `${failures} FAILURE(S)`);
  process.exit(failures ? 1 : 0);
})().catch(e => { console.error('ERROR', e.stack || e.message); process.exit(1); });
