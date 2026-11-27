#!/usr/bin/env node
// Rotates the password of ONE PostgreSQL login role used by the pilot API, and updates DATABASE_URL in
// the API env file. Order: (1) check the current password works, (2) write a new password into the
// database with ALTER ROLE, (3) check the NEW password works and the OLD one is refused, (4) only then
// replace the DATABASE_URL line in the env file (atomic rename, mode 600), keeping a backup of the old
// env file for rollback. Passwords are never printed or put on a command line: the ALTER ROLE statement
// goes to the database admin command through stdin.
//
// Modes:
//   --dry-run (default): checks everything that can be checked without changing anything.
//   --apply --confirm=<role>: performs the rotation. Stop the services that use the URL first.
//   --rollback <backup env file> --confirm=<role>: restores the backup env file and sets the role
//     password back to the one recorded in it.
//
// Environment:
//   ENV_FILE            path to the API env file (must contain exactly one DATABASE_URL= line, unquoted)
//   DB_ADMIN_CMD        shell command that runs psql as a superuser and reads SQL on stdin, for example
//                       "podman exec -i <container> psql -v ON_ERROR_STOP=1 -U postgres -d postgres"
//   CLONE_SERVER        server tree with node_modules (for the pg driver)
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { spawnSync } = require('child_process');
const SERVER = process.env.CLONE_SERVER || path.resolve(__dirname, '../../server');
const { Client } = require(path.join(SERVER, 'node_modules/pg'));

const die = msg => { throw new Error(msg); };
const ROLE_RE = /^[a-z_][a-z0-9_]{0,62}$/;

function parseUrl(url) {
  const u = new URL(url);
  return { user: decodeURIComponent(u.username), password: decodeURIComponent(u.password), host: u.hostname, port: u.port, db: decodeURIComponent(u.pathname.slice(1)) };
}
function withPassword(url, password) {
  const u = new URL(url);
  u.password = password;
  return u.toString();
}
function readEnvLine(file) {
  const lines = fs.readFileSync(file, 'utf8').split('\n');
  const idx = lines.map((l, i) => (l.startsWith('DATABASE_URL=') ? i : -1)).filter(i => i >= 0);
  if (idx.length !== 1) die(`ENV_FILE must contain exactly one DATABASE_URL= line (found ${idx.length})`);
  const value = lines[idx[0]].slice('DATABASE_URL='.length);
  if (/^["']/.test(value)) die('DATABASE_URL is quoted in ENV_FILE; this script only handles unquoted values');
  return { lines, idx: idx[0], value };
}
async function canConnect(url) {
  const client = new Client({ connectionString: url, connectionTimeoutMillis: 5000 });
  try { await client.connect(); await client.query('SELECT 1'); return true; }
  catch (_) { return false; }
  finally { try { await client.end(); } catch (_) {} }
}
function adminSql(sql) {
  const cmd = process.env.DB_ADMIN_CMD || die('DB_ADMIN_CMD is required');
  const r = spawnSync(cmd, { shell: true, input: sql, encoding: 'utf8' });
  if (r.status !== 0) die('database admin command failed (no secret values shown)');
}
function writeEnvAtomically(file, lines) {
  const tmp = `${file}.new-${process.pid}`;
  fs.writeFileSync(tmp, lines.join('\n'), { mode: 0o600 });
  fs.renameSync(tmp, file);
}

async function apply({ role, dryRun, backupFile }) {
  const envFile = process.env.ENV_FILE || die('ENV_FILE is required');
  const { lines, idx, value: oldUrl } = readEnvLine(envFile);
  const info = parseUrl(oldUrl);
  if (info.user !== role) die(`DATABASE_URL user does not match role ${role}`);
  if (!ROLE_RE.test(role)) die('role name is not a plain identifier');
  if (!info.password) die('DATABASE_URL has no password; nothing to rotate');
  if (!(await canConnect(oldUrl))) die('the current DATABASE_URL does not connect; stop and investigate before rotating');
  console.log(`checks: role=${role} database=${info.db} current password accepted`);
  if (dryRun) { console.log('DRY RUN: nothing changed. Re-run with --apply --confirm=' + role + ' after a fresh verified dump and with the services stopped.'); return; }

  const newPassword = crypto.randomBytes(24).toString('hex');
  const newUrl = withPassword(oldUrl, newPassword);
  const backup = `${envFile}.before-rotation-${new Date().toISOString().replace(/[:.]/g, '-')}`;
  fs.copyFileSync(envFile, backup);
  fs.chmodSync(backup, 0o600);
  console.log(`backup of the env file written (mode 600): ${path.basename(backup)}`);

  adminSql(`ALTER ROLE "${role}" PASSWORD '${newPassword}';\n`);
  try {
    if (!(await canConnect(newUrl))) die('new password did not connect');
    if (await canConnect(oldUrl)) die('old password still accepted after rotation');
    console.log('new password accepted; old password refused');
    lines[idx] = 'DATABASE_URL=' + newUrl;
    writeEnvAtomically(envFile, lines);
  } catch (e) {
    // Any failure after ALTER ROLE puts the old password back, so the env file and the role still agree.
    adminSql(`ALTER ROLE "${role}" PASSWORD '${info.password}';\n`);
    die(e.message + '; the old password was restored in the database and the env file is unchanged');
  }
  console.log('env file updated; restart the services that read it. Rollback: --rollback ' + path.basename(backup) + ' --confirm=' + role);
}

async function rollback({ role, backupFile }) {
  const envFile = process.env.ENV_FILE || die('ENV_FILE is required');
  if (!backupFile) die('--rollback needs the backup env file path');
  const old = readEnvLine(backupFile);
  const oldInfo = parseUrl(old.value);
  if (oldInfo.user !== role) die('backup does not match role');
  const { lines, idx } = readEnvLine(envFile);
  const currentPassword = parseUrl(lines[idx].slice('DATABASE_URL='.length)).password;
  adminSql(`ALTER ROLE "${role}" PASSWORD '${oldInfo.password}';\n`);
  if (!(await canConnect(old.value))) { adminSql(`ALTER ROLE "${role}" PASSWORD '${currentPassword}';\n`); die('old password did not connect; the rotated password was restored'); }
  writeEnvAtomically(envFile, old.lines);
  console.log('rolled back: role password and env file restored from the backup; restart the services');
}

(async () => {
  const args = process.argv.slice(2);
  const roleArg = args.find(a => a.startsWith('--confirm='));
  const role = roleArg ? roleArg.slice('--confirm='.length) : null;
  const rbIdx = args.indexOf('--rollback');
  if (rbIdx >= 0) {
    if (!role || !ROLE_RE.test(role)) die('--rollback needs --confirm=<role>');
    return rollback({ role, backupFile: args[rbIdx + 1] });
  }
  const dryRun = !args.includes('--apply');
  if (!dryRun && !role) die('--apply needs --confirm=<role>');
  if (!role) die('pass --confirm=<role> (the login role whose password is rotated)');
  return apply({ role, dryRun });
})().catch(e => { console.error('STOPPED:', e.message.split('\n')[0]); process.exit(1); });
