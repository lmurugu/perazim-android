#!/usr/bin/env node
'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const os = require('os');
const readline = require('readline');
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');
const { validatePhaseBTestDatabaseUrl } = require('../src/db');
const { encryptSecret, generateSecret } = require('../src/phase-b/totp');
const { isLocalDatabaseAddress, ensureSelfServiceGrants } = require('../src/phase-b/security');
const { ACKNOWLEDGEMENT_TEXT } = require('../src/phase-b/privacyNotice');
const authRoutes = require('../src/routes/auth');

function args(argv) {
  const result = {};
  for (let i = 0; i < argv.length; i++) {
    const key = argv[i];
    if (!key.startsWith('--')) throw new Error('Unknown argument');
    if (key === '--new-pin') throw new Error('PIN values are not accepted as command-line arguments; use --pin-file');
    if (key === '--create-account') {
      if (result.createAccount) throw new Error('Duplicate create-account option');
      result.createAccount = true;
      continue;
    }
    if (['--bootstrap', '--recover'].includes(key)) {
      if (result.mode) throw new Error('Choose one mode');
      result.mode = key.slice(2);
    } else {
      if (!argv[i + 1] || argv[i + 1].startsWith('--')) throw new Error('Argument value missing');
      result[key.slice(2).replace(/-([a-z])/g, (_, letter) => letter.toUpperCase())] = argv[++i];
    }
  }
  return result;
}

function assertSafety(env, confirmation, dbName) {
  if (env.NODE_ENV !== 'test' || env.PHASE_B_SYNTHETIC !== 'true') throw new Error('Synthetic test mode is required');
  const target = validatePhaseBTestDatabaseUrl(env.DATABASE_URL || '');
  const actualFromUrl = target.database;
  if (!/^perazim_phaseb_[a-z0-9]{6,32}_test$/.test(actualFromUrl)) throw new Error('A uniquely named local Phase B test database is required');
  if (!dbName || dbName !== actualFromUrl || confirmation !== actualFromUrl) throw new Error('Exact database name and confirmation are required');
  return actualFromUrl;
}

function validPilotId(id) { return typeof id === 'string' && /^synthetic_pilot_[a-z0-9]{6,32}$/.test(id); }
function validApproval(value) { return typeof value === 'string' && /^[A-Z0-9][A-Z0-9_-]{3,63}$/.test(value); }

function writeEnrollmentFile(file, payload) {
  if (typeof file !== 'string' || !path.isAbsolute(file)) throw new Error('An absolute enrollment-file path is required');
  const fd = fs.openSync(file, 'wx', 0o600);
  try {
    fs.fchmodSync(fd, 0o600);
    fs.writeFileSync(fd, `${JSON.stringify(payload, null, 2)}\n`, { encoding: 'utf8' });
    fs.fsyncSync(fd);
  } finally { fs.closeSync(fd); }
}

function readPrivatePinFile(file) {
  if (typeof file !== 'string' || !path.isAbsolute(file)) throw new Error('An absolute private PIN file path is required');
  const stat = fs.statSync(file);
  if (!stat.isFile() || (stat.mode & 0o077) !== 0) throw new Error('PIN file must be a regular file accessible only to its owner');
  const pin = fs.readFileSync(file, 'utf8').trim();
  if (!/^\d{6,12}$/.test(pin)) throw new Error('PIN file must contain a 6–12 digit PIN');
  return pin;
}

function promptVisible(question) {
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  return new Promise(resolve => rl.question(question, answer => {
    rl.close();
    resolve(answer.trim());
  }));
}

function promptHidden(question) {
  const input = process.stdin;
  if (!input.isTTY || typeof input.setRawMode !== 'function') {
    return Promise.reject(new Error('Run the setup directly in an interactive terminal'));
  }
  return new Promise((resolve, reject) => {
    let value = '';
    const finish = (error = null) => {
      input.removeListener('data', onData);
      input.setRawMode(false);
      input.pause();
      process.stdout.write('\n');
      if (error) reject(error); else resolve(value);
    };
    const onData = chunk => {
      for (const char of String(chunk)) {
        if (char === '\u0003') return finish(new Error('Setup cancelled'));
        if (char === '\r' || char === '\n') return finish();
        if (char === '\u007f' || char === '\b') value = value.slice(0, -1);
        else if (char >= ' ' && char <= '~' && value.length < 64) value += char;
      }
    };
    process.stdout.write(question);
    input.setEncoding('utf8');
    input.setRawMode(true);
    input.resume();
    input.on('data', onData);
  });
}

async function readPublishedTestNotice(pool) {
  const result = await pool.query(
    `SELECT version,content_hash,document_path,publication_status,effective_at,withdrawn_at
       FROM privacy_notice_versions WHERE is_active=TRUE LIMIT 1`
  );
  const row = result.rows[0];
  if (!row || row.publication_status !== 'PUBLISHED' || !String(row.version).includes('TEST') ||
      row.withdrawn_at != null || Number(row.effective_at) > Date.now()) {
    throw new Error('An active, TEST-labeled published notice is required in the disposable database');
  }
  const serverDir = path.resolve(__dirname, '..');
  const documentPath = path.resolve(serverDir, row.document_path);
  if (path.dirname(documentPath) !== serverDir) throw new Error('Test notice path is outside the server document directory');
  const content = fs.readFileSync(documentPath, 'utf8');
  const hash = crypto.createHash('sha256').update(content, 'utf8').digest('hex');
  if (hash !== String(row.content_hash).trim() || !content.startsWith('# TEST ONLY')) {
    throw new Error('TEST notice content or hash is invalid');
  }
  return { ...row, presented_at: Date.now(), content };
}

async function verifyActualDatabase(client, expected, env = process.env) {
  const target = validatePhaseBTestDatabaseUrl(env.DATABASE_URL || '');
  const actual = await client.query(`SELECT current_database() AS database, inet_server_addr()::text AS address,
    inet_server_port() AS port, current_setting('data_directory') AS data_directory`);
  if (actual.rows[0].database !== expected) throw new Error('Connected database does not match the confirmed target');
  if (actual.rows[0].address && !isLocalDatabaseAddress(actual.rows[0].address)) throw new Error('PostgreSQL server address is not local');
  if (Number(actual.rows[0].port) !== target.port) throw new Error('Connected PostgreSQL port does not match the confirmed target');
  if (env.PHASE_B_TEST_EXPECTED_PORT && Number(actual.rows[0].port) !== Number(env.PHASE_B_TEST_EXPECTED_PORT)) throw new Error('Connected PostgreSQL port does not match the expected test port');
  if (env.PHASE_B_TEST_EXPECTED_DATA_DIR && path.resolve(actual.rows[0].data_directory) !== path.resolve(env.PHASE_B_TEST_EXPECTED_DATA_DIR)) throw new Error('Connected PostgreSQL cluster directory does not match the expected test cluster');
}

async function transact(pool, callback) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await client.query('SELECT pg_advisory_xact_lock(hashtext($1))', ['perazim_phaseb_synthetic_admin']);
    const result = await callback(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    try { await client.query('ROLLBACK'); } catch (_) {}
    throw error;
  } finally { client.release(); }
}

async function bootstrap(pool, options, env = process.env) {
  const dbName = assertSafety(env, options.confirm, options.db);
  if (!validPilotId(options.account) || !validApproval(options.approval)) throw new Error('Synthetic pilot ID and owner approval reference are required');
  if (options.createAccount && (!/^[A-Za-z][A-Za-z '\u2019-]{0,126}$/.test(options.name || '') ||
      typeof options.phone !== 'string' || !/^\+254[17]\d{8}$/.test(options.phone) ||
      typeof options.pinHash !== 'string' || !options.privacyNotice || options.privacyAcknowledged !== true)) {
    throw new Error('Interactive test-account details and notice acknowledgement are required');
  }
  const secret = generateSecret();
  const encrypted = encryptSecret(secret);
  const now = Date.now();
  const result = await transact(pool, async client => {
    await verifyActualDatabase(client, dbName, env);
    const existingPilot = await client.query('SELECT user_id FROM pilot_bootstrap WHERE singleton=TRUE FOR UPDATE');
    if (existingPilot.rows.length) throw new Error('The sole pilot administrator is already bootstrapped');
    if (options.createAccount) {
      const duplicate = await client.query('SELECT id FROM users WHERE phone=$1 FOR UPDATE', [options.phone]);
      if (duplicate.rows.length) throw new Error('An account with that phone already exists in the disposable database');
      await client.query(
        `INSERT INTO users (id,phone,pin_hash,name,campus_id,role,avatar_url,xp,streak,grace_points,is_active,created_at,updated_at)
         VALUES ($1,$2,$3,$4,'campus_embu','ADMIN','',0,0,0,TRUE,$5,$5)`,
        [options.account, options.phone, options.pinHash, options.name.trim(), now]
      );
      await client.query(
        `INSERT INTO membership_verifications (user_id,membership_record_id,status,verification_source,revision,updated_at)
         VALUES ($1,NULL,'REGISTERED','MANUAL',1,$2)`, [options.account, now]
      );
      await client.query(
        `INSERT INTO membership_verification_history (id,user_id,claim_id,previous_status,new_status,actor_id,reason,created_at)
         VALUES ($1,$2,NULL,NULL,'REGISTERED',$2,'SYNTHETIC_TEST_ACCOUNT_CREATED',$3)`,
        [crypto.randomUUID(), options.account, now]
      );
      await ensureSelfServiceGrants(client, options.account, now);
      await client.query(
        `INSERT INTO privacy_notice_acknowledgements
          (user_id,notice_version,content_hash,presented_at,acknowledged_at,acknowledgement_text,client_platform)
         VALUES ($1,$2,$3,$4,$5,$6,'terminal-test-setup')`,
        [options.account, options.privacyNotice.version, options.privacyNotice.content_hash,
          options.privacyNotice.presented_at, now, ACKNOWLEDGEMENT_TEXT]
      );
    } else {
      const account = await client.query('SELECT id,is_active FROM users WHERE id=$1 FOR UPDATE', [options.account]);
      if (!account.rows.length || !account.rows[0].is_active) throw new Error('The specified synthetic account must already exist and be active');
    }
    await client.query(
      'INSERT INTO admin_security (user_id,totp_secret_cipher,totp_last_counter,security_version,auth_not_before,created_at) VALUES ($1,$2,-1,0,0,$3)',
      [options.account, encrypted, now]
    );
    await client.query('INSERT INTO pilot_bootstrap (singleton,user_id,approval_ref,created_at) VALUES (TRUE,$1,$2,$3)', [options.account, options.approval, now]);
    for (const code of [
      'membership.record.read', 'membership.register.import', 'membership.claim.review',
      'membership.verification.confirm', 'membership.record.correct', 'membership.verification.correct',
      'membership.verification.revoke', 'admin.permissions.read', 'admin.permissions.grant',
      'admin.permissions.revoke', 'admin.audit.read'
    ]) {
      const active = await client.query('SELECT 1 FROM permissions WHERE code=$1 AND active=TRUE', [code]);
      if (!active.rows.length) throw new Error(`Required capability is unavailable: ${code}`);
      await client.query(
        `INSERT INTO user_permissions (id,user_id,permission_code,scope,granted_by,granted_at,effective_at,expires_at)
         VALUES ($1,$2,$3,'global',$2,$4,$4,NULL)`, [crypto.randomUUID(), options.account, code, now]
      );
    }
    const forbidden = await client.query(
      `SELECT 1 FROM user_permissions WHERE user_id=$1 AND permission_code='privacy.document.publish' AND revoked_at IS NULL LIMIT 1`,
      [options.account]
    );
    if (forbidden.rows.length) throw new Error('Forbidden privacy publication permission is already assigned');
    await client.query(
      `INSERT INTO admin_audit_log (id,actor_id,action,resource_type,resource_id,outcome,reason,request_id,created_at)
       VALUES ($1,$2,'SYNTHETIC_ADMIN_BOOTSTRAP','administrator',$3,'SUCCESS','OWNER_APPROVED',$4,$5)`,
      [crypto.randomUUID(), options.account, options.account, options.approval, now]
    );
    return { account_id: options.account, approval_ref: options.approval, totp_secret: secret };
  });
  return result;
}

async function recover(pool, options, env = process.env) {
  const dbName = assertSafety(env, options.confirm, options.db);
  if (!validPilotId(options.account) || !validApproval(options.approval) || typeof options.newPin !== 'string' || !/^\d{6,12}$/.test(options.newPin)) {
    throw new Error('Recovery requires the exact synthetic pilot, approval reference, and a new 6–12 digit PIN');
  }
  const secret = generateSecret();
  const encrypted = encryptSecret(secret);
  const pinHash = await bcrypt.hash(options.newPin, 10);
  const now = Date.now();
  const notBefore = (Math.floor(now / 1000) + 1) * 1000;
  const result = await transact(pool, async client => {
    await verifyActualDatabase(client, dbName);
    const pilot = await client.query('SELECT user_id,approval_ref FROM pilot_bootstrap WHERE singleton=TRUE FOR UPDATE');
    if (!pilot.rows.length || pilot.rows[0].user_id !== options.account) throw new Error('Recovery may target only the existing synthetic pilot identity');
    if (pilot.rows[0].approval_ref === options.approval) throw new Error('Recovery requires a fresh approval reference');
    const reused = await client.query(`SELECT 1 FROM admin_audit_log WHERE action='SYNTHETIC_ADMIN_RECOVERY' AND request_id=$1 LIMIT 1`, [options.approval]);
    if (reused.rows.length) throw new Error('Recovery approval reference has already been used');
    const security = await client.query('SELECT user_id FROM admin_security WHERE user_id=$1 FOR UPDATE', [options.account]);
    if (!security.rows.length) throw new Error('Synthetic pilot security record is unavailable');
    await client.query('UPDATE privileged_sessions SET revoked_at=$2 WHERE user_id=$1 AND revoked_at IS NULL', [options.account, now]);
    await client.query(
      `UPDATE admin_security SET totp_secret_cipher=$2,totp_last_counter=-1,security_version=security_version+1,auth_not_before=$3 WHERE user_id=$1`,
      [options.account, encrypted, notBefore]
    );
    await client.query('UPDATE users SET pin_hash=$2,is_active=TRUE,updated_at=$3 WHERE id=$1', [options.account, pinHash, now]);
    await client.query(
      `INSERT INTO admin_audit_log (id,actor_id,action,resource_type,resource_id,outcome,reason,request_id,created_at)
       VALUES ($1,$2,'SYNTHETIC_ADMIN_RECOVERY','administrator',$3,'SUCCESS','OWNER_APPROVED',$4,$5)`,
      [crypto.randomUUID(), options.account, options.account, options.approval, now]
    );
    return { account_id: options.account, approval_ref: options.approval, auth_not_before: notBefore, totp_secret: secret };
  });
  return result;
}

async function setupInteractiveAdmin(options, env = process.env) {
  const dbName = assertSafety(env, options.confirm, options.db);
  if (!validApproval(options.approval)) throw new Error('A synthetic test approval reference is required');
  const target = validatePhaseBTestDatabaseUrl(env.DATABASE_URL || '');
  if (Number(env.PHASE_B_TEST_EXPECTED_PORT) !== target.port || !env.PHASE_B_TEST_EXPECTED_DATA_DIR ||
      !fs.existsSync(env.PHASE_B_TEST_EXPECTED_DATA_DIR)) {
    throw new Error('The exact disposable test cluster port and data directory must be configured');
  }
  if (!env.PHASE_B_ADMIN_KEY) throw new Error('The local TOTP encryption key is not configured');

  const pool = new Pool(target);
  let tempDir = null;
  let enrollmentFile = null;
  try {
    const client = await pool.connect();
    try {
      await verifyActualDatabase(client, dbName, env);
      const pilot = await client.query('SELECT user_id FROM pilot_bootstrap WHERE singleton=TRUE');
      if (pilot.rows.length) throw new Error('The sole pilot administrator is already bootstrapped');
    } finally { client.release(); }

    const notice = await readPublishedTestNotice(pool);
    process.stdout.write(`\nTEST notice for disposable database ${dbName}:\n\n${notice.content}\n`);
    const acknowledgement = await promptVisible('After reviewing the TEST-only notice above, type ACKNOWLEDGE to continue: ');
    if (acknowledgement !== 'ACKNOWLEDGE') throw new Error('TEST privacy notice acknowledgement was not confirmed');

    const name = await promptVisible('Administrator full name: ');
    if (!/^[A-Za-z][A-Za-z '\u2019-]{0,126}$/.test(name)) throw new Error('Name format is invalid');
    const rawPhone = await promptHidden('Phone number (hidden): ');
    const phone = authRoutes.validateAndNormalizePhone(rawPhone);
    if (!phone) throw new Error('Phone number format is invalid');
    const pin = await promptHidden('Choose a 6–12 digit PIN (hidden): ');
    if (!/^\d{6,12}$/.test(pin)) throw new Error('PIN must contain 6–12 digits');
    const pinAgain = await promptHidden('Re-enter the PIN (hidden): ');
    if (pin !== pinAgain) throw new Error('PIN entries did not match');
    const pinHash = await bcrypt.hash(pin, 10);
    const account = `synthetic_pilot_${crypto.randomBytes(8).toString('hex')}`;

    tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'perazim-test-admin-'));
    fs.chmodSync(tempDir, 0o700);
    enrollmentFile = path.join(tempDir, 'enrollment.json');

    const result = await bootstrap(pool, {
      ...options,
      createAccount: true,
      account,
      name,
      phone,
      pinHash,
      privacyNotice: notice,
      privacyAcknowledged: true
    }, env);

    writeEnrollmentFile(enrollmentFile, {
      account_id: result.account_id,
      approval_ref: result.approval_ref,
      totp_secret: result.totp_secret,
      created_at: new Date().toISOString(),
      note: 'Synthetic test enrollment; delete after adding the key to the authenticator.'
    });
    const enrollmentStat = fs.statSync(enrollmentFile);
    if ((enrollmentStat.mode & 0o777) !== 0o600) throw new Error('Temporary TOTP enrollment file is not owner-only');
    const enrollment = JSON.parse(fs.readFileSync(enrollmentFile, 'utf8'));
    process.stdout.write(`\nTOTP key (displayed once; add it to your authenticator now):\n${enrollment.totp_secret}\n`);
    await promptVisible('After adding the key, press Enter to delete the temporary enrollment file: ');
    process.stdout.write('\x1b[2J\x1b[H\x1b[3J');
    return { success: true, account_id: result.account_id, role: 'ADMIN', membership_status: 'REGISTERED', privacy_document_publish_granted: false };
  } finally {
    if (enrollmentFile) {
      try { fs.unlinkSync(enrollmentFile); } catch (error) { if (error.code !== 'ENOENT') throw error; }
    }
    if (tempDir) {
      try { fs.rmdirSync(tempDir); } catch (error) { if (error.code !== 'ENOENT') throw error; }
    }
    await pool.end();
  }
}

async function main() {
  const options = args(process.argv.slice(2));
  if (!options.mode || !process.env.DATABASE_URL) throw new Error('Usage: --bootstrap|--recover --db NAME --confirm NAME --account synthetic_pilot_<suffix> --approval REF --enrollment-file /absolute/path [--pin-file /absolute/path]');
  assertSafety(process.env, options.confirm, options.db);
  if (options.createAccount) {
    if (options.mode !== 'bootstrap') throw new Error('--create-account is available only with --bootstrap');
    const result = await setupInteractiveAdmin(options);
    process.stdout.write(`${JSON.stringify(result)}\n`);
    return;
  }
  if (!options.enrollmentFile) throw new Error('A new, absolute --enrollment-file path is required; enrollment secrets are never printed');
  if (options.newPin) throw new Error('PIN values are not accepted as command-line arguments; use a private --pin-file');
  if (options.pinFile) options.newPin = readPrivatePinFile(options.pinFile);
  if (!path.isAbsolute(options.enrollmentFile)) throw new Error('An absolute enrollment-file path is required');
  const enrollmentFd = fs.openSync(options.enrollmentFile, 'wx', 0o600);
  fs.fchmodSync(enrollmentFd, 0o600);
  const pool = new Pool(validatePhaseBTestDatabaseUrl(process.env.DATABASE_URL));
  try {
    const result = options.mode === 'bootstrap' ? await bootstrap(pool, options) : await recover(pool, options);
    const { totp_secret: totpSecret, ...safeResult } = result;
    fs.writeFileSync(enrollmentFd, `${JSON.stringify({
      account_id: safeResult.account_id,
      approval_ref: safeResult.approval_ref,
      totp_secret: totpSecret,
      created_at: new Date().toISOString(),
      note: 'Synthetic-only enrollment material. Protect this file and import the TOTP secret into an approved authenticator.'
    }, null, 2)}\n`, { encoding: 'utf8' });
    fs.fsyncSync(enrollmentFd);
    process.stdout.write(`${JSON.stringify({ success: true, ...safeResult, enrollment_file_written: true })}\n`);
  } finally { fs.closeSync(enrollmentFd); await pool.end(); }
}

if (require.main === module) main().catch(() => {
  process.stderr.write('Synthetic administrator operation failed; detailed errors are withheld to protect connection and enrollment data.\n');
  process.exitCode = 1;
});

module.exports = { args, assertSafety, verifyActualDatabase, bootstrap, recover, writeEnrollmentFile, readPrivatePinFile };
