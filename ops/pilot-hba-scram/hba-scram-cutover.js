#!/usr/bin/env node
// Coordinated local-access cutover for a Postgres cluster.
// Every TCP connection must present a SCRAM password. The local socket keeps peer access for the
// postgres OS user, which is the admin recovery path (for example: podman exec -u postgres <container>).
// Dry run by default. Nothing is written unless --apply and --confirm=<database> are both given.
//
// Environment:
//   ADMIN_CMD        shell command that runs psql as the cluster superuser over the local socket and reads SQL on stdin.
//   HBA_GET_CMD      shell command that prints the pg_hba.conf named by $HBA_FILE to stdout.
//   HBA_PUT_CMD      shell command that replaces the file named by $HBA_FILE with stdin, atomically, mode 600.
//   CLONE_SERVER     server tree with node_modules (for the pg client). Default: this repo's server/.
//   ROLE             the login role the API connects as, for example perazim_app.
//   TEST_URL         that role's CURRENT connection URL. Used only to check it still connects.
//   BACKUP_DIR       where the pre-change pg_hba.conf is kept (mode 600).
//   PRE_CHANGE_DUMP  (apply only) a fresh dump taken just before, with its .sha256 file beside it.
//
// Usage:
//   node hba-scram-cutover.js                                    dry run
//   node hba-scram-cutover.js --apply --confirm=<database>       cutover (checks, then reloads)
//   node hba-scram-cutover.js --rollback <backup file> --confirm=<database>
//
// Order with the password rotation: run this first (the current password is still valid under SCRAM),
// then rotate-db-password.js, which can only prove the OLD password is refused once host auth is SCRAM.
'use strict';
const { spawnSync } = require('child_process');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const die = msg => { console.error('STOPPED: ' + msg); process.exit(1); };
const CLONE = process.env.CLONE_SERVER || path.resolve(__dirname, '../../server');
const { Client } = require(path.join(CLONE, 'node_modules/pg'));

const args = process.argv.slice(2);
const doApply = args.includes('--apply');
const rbIdx = args.indexOf('--rollback');
const rollbackFile = rbIdx >= 0 ? args[rbIdx + 1] : null;
const confirmArg = args.find(a => a.startsWith('--confirm=')) || '';

const ADMIN_CMD = process.env.ADMIN_CMD || die('ADMIN_CMD is required');
const ROLE = process.env.ROLE || die('ROLE is required');
const TEST_URL = process.env.TEST_URL || die('TEST_URL is required');
const BACKUP_DIR = process.env.BACKUP_DIR || die('BACKUP_DIR is required');
if (!/^[a-z_][a-z0-9_]*$/.test(ROLE)) die('ROLE must be a plain role name');
const parsed = new URL(TEST_URL);
if (decodeURIComponent(parsed.username) !== ROLE) die('TEST_URL user does not match ROLE');
const dbName = decodeURIComponent(parsed.pathname.slice(1));

function admin(sql) {
  const r = spawnSync('sh', ['-c', ADMIN_CMD], {
    input: '\\pset tuples_only on\n\\pset format unaligned\n' + sql, encoding: 'utf8'
  });
  if (r.status !== 0) throw new Error('admin SQL failed: ' + ((r.stderr || '').split('\n').find(l => l.trim()) || 'see psql'));
  return r.stdout.trim();
}
function hbaCmd(cmd, input, file) {
  const r = spawnSync('sh', ['-c', cmd], {
    input, encoding: 'utf8', env: { ...process.env, HBA_FILE: file }
  });
  if (r.status !== 0) throw new Error('HBA command failed: ' + ((r.stderr || '').split('\n').find(l => l.trim()) || 'see output'));
  return r.stdout;
}
const hbaFile = () => admin("SELECT current_setting('hba_file');");
const getHba = file => hbaCmd(process.env.HBA_GET_CMD || die('HBA_GET_CMD is required'), '', file);
const putHba = (file, text) => hbaCmd(process.env.HBA_PUT_CMD || die('HBA_PUT_CMD is required'), text, file);

// Reloads the server and reports "<rules with errors> <host rules not using scram-sha-256>". Expect "0 0".
function reloadAndCheck() {
  admin('SELECT pg_reload_conf();');
  return admin("SELECT count(*) FILTER (WHERE error IS NOT NULL) || ' ' || count(*) FILTER (WHERE type LIKE 'host%' AND auth_method IS DISTINCT FROM 'scram-sha-256') FROM pg_hba_file_rules;");
}

function newHba(backupName) {
  // TEST_ONLY_EXTRA_HBA_LINE exists so the test harness can prove a bad file is refused. Never set it in production.
  const extra = process.env.TEST_ONLY_EXTRA_HBA_LINE ? process.env.TEST_ONLY_EXTRA_HBA_LINE + '\n' : '';
  return [
    `# pg_hba.conf after the SCRAM cutover (hba-scram-cutover.js). Pre-change copy: ${backupName}`,
    '# The postgres OS user keeps peer access over the local socket, for admin recovery.',
    '# Every other connection needs a SCRAM-SHA-256 password.',
    'local   all             postgres                                peer',
    'local   all             all                                     scram-sha-256',
    'host    all             all             all                     scram-sha-256',
    'local   replication     all                                     peer',
    'host    replication     all             all                     scram-sha-256',
    extra
  ].join('\n');
}

function preflight() {
  if (admin(`SELECT count(*) FROM pg_roles WHERE rolname = '${ROLE}';`) !== '1') die(`role ${ROLE} does not exist`);
  // Under SCRAM-only host auth, a login role without a SCRAM password would be locked out.
  const bad = admin(`SELECT string_agg(rolname, ', ') FROM pg_authid
    WHERE rolcanlogin AND ((rolpassword IS NOT NULL AND rolpassword NOT LIKE 'SCRAM-SHA-256$%')
      OR (rolpassword IS NULL AND rolname = '${ROLE}'));`);
  if (bad) die(`login roles that would be locked out (no SCRAM password): ${bad}. Set their passwords first.`);
}

function withPassword(url, pw) {
  const u = new URL(url);
  u.password = pw;
  return u.toString();
}
async function canConnect(url) {
  const c = new Client({ connectionString: url, connectionTimeoutMillis: 4000 });
  try { await c.connect(); await c.query('SELECT 1'); return true; } catch (_) { return false; } finally { try { await c.end(); } catch (_) {} }
}

function restore(backupPath, file) {
  putHba(file, fs.readFileSync(backupPath, 'utf8'));
  reloadAndCheck();
  if (getHba(file) !== fs.readFileSync(backupPath, 'utf8')) die(`restore did not read back the same file; check ${file} by hand`);
}

async function dryRun() {
  preflight();
  const file = hbaFile();
  const rules = admin("SELECT string_agg(DISTINCT coalesce(type,'?') || ':' || coalesce(auth_method,'?'), ' ') FROM pg_hba_file_rules;");
  console.log(`hba_file: ${file}`);
  console.log(`current rule kinds: ${rules}`);
  console.log(`role ${ROLE} has a SCRAM password: yes`);
  console.log('DRY RUN: nothing written. Re-run with --apply --confirm=' + dbName + ' after a fresh dump.');
}

async function apply() {
  if (confirmArg !== `--confirm=${dbName}`) die(`--apply needs --confirm=${dbName}`);
  const dump = process.env.PRE_CHANGE_DUMP || die('PRE_CHANGE_DUMP must name a fresh dump taken before --apply');
  if (spawnSync('sha256sum', ['-c', dump + '.sha256'], { encoding: 'utf8' }).status !== 0) die('dump checksum is missing or does not verify');
  preflight();
  const file = hbaFile();
  const before = getHba(file);
  const backupName = `pg_hba.conf.before-scram-${new Date().toISOString().replace(/[:.]/g, '-')}`;
  const backupPath = path.join(BACKUP_DIR, backupName);
  fs.mkdirSync(BACKUP_DIR, { recursive: true, mode: 0o700 });
  fs.writeFileSync(backupPath, before, { mode: 0o600 });

  putHba(file, newHba(backupName));
  const check = reloadAndCheck();
  if (check !== '0 0') {
    restore(backupPath, file);
    die(`new pg_hba.conf rejected (rules with errors, host rules not SCRAM: "${check}"); previous file restored`);
  }
  const good = await canConnect(TEST_URL);
  const wrong = await canConnect(withPassword(TEST_URL, crypto.randomBytes(16).toString('hex')));
  const none = await canConnect(withPassword(TEST_URL, ''));
  if (!good || wrong || none) {
    restore(backupPath, file);
    die(`connection check failed (current password ok: ${good}, wrong password refused: ${!wrong}, no password refused: ${!none}); previous file restored`);
  }
  console.log(`COMMITTED: host connections now need SCRAM. Pre-change pg_hba.conf: ${backupPath}`);
  console.log(`Admin recovery: the ADMIN_CMD path (postgres OS user, local socket) still works.`);
  console.log('Next: rotate-db-password.js (the old password is now refused over TCP).');
}

async function rollback() {
  if (confirmArg !== `--confirm=${dbName}`) die(`--rollback needs --confirm=${dbName}`);
  const backup = path.resolve(rollbackFile || die('--rollback needs a backup file'));
  if (!path.basename(backup).startsWith('pg_hba.conf.before-scram-') || !fs.existsSync(backup)) die('rollback needs an existing pg_hba.conf.before-scram-* file');
  const file = hbaFile();
  restore(backup, file);
  console.log(`ROLLED BACK: ${file} restored from ${backup} and read back identical.`);
}

(async () => {
  try {
    if (rollbackFile) await rollback();
    else if (doApply) await apply();
    else await dryRun();
  } catch (e) {
    die(e.message);
  }
})();
