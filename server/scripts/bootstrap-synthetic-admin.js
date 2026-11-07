#!/usr/bin/env node
'use strict';

const crypto = require('crypto');
const net = require('net');
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');
const { encryptSecret, generateSecret } = require('../src/phase-b/totp');
const { isLocalDatabaseAddress } = require('../src/phase-b/security');

function args(argv) {
  const result = {};
  for (let i = 0; i < argv.length; i++) {
    const key = argv[i];
    if (!key.startsWith('--')) throw new Error('Unknown argument');
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
  const url = new URL(env.DATABASE_URL || '');
  const host = url.hostname.toLowerCase();
  const local = host === 'localhost' || host === '::1' || net.isIP(host) === 4 && host.startsWith('127.');
  const actualFromUrl = decodeURIComponent(url.pathname.replace(/^\//, ''));
  if (!local || !/^perazim_phaseb_[a-z0-9]{6,32}_test$/.test(actualFromUrl)) throw new Error('A uniquely named local Phase B test database is required');
  if (!dbName || dbName !== actualFromUrl || confirmation !== actualFromUrl) throw new Error('Exact database name and confirmation are required');
  return actualFromUrl;
}

function validPilotId(id) { return typeof id === 'string' && /^synthetic_pilot_[a-z0-9]{6,32}$/.test(id); }
function validApproval(value) { return typeof value === 'string' && /^[A-Z0-9][A-Z0-9_-]{3,63}$/.test(value); }

async function verifyActualDatabase(client, expected) {
  const actual = await client.query('SELECT current_database() AS database, inet_server_addr()::text AS address');
  if (actual.rows[0].database !== expected) throw new Error('Connected database does not match the confirmed target');
  if (actual.rows[0].address && !isLocalDatabaseAddress(actual.rows[0].address)) throw new Error('PostgreSQL server address is not local');
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
  const secret = generateSecret();
  const encrypted = encryptSecret(secret);
  const now = Date.now();
  const result = await transact(pool, async client => {
    await verifyActualDatabase(client, dbName);
    const existingPilot = await client.query('SELECT user_id FROM pilot_bootstrap WHERE singleton=TRUE FOR UPDATE');
    if (existingPilot.rows.length) throw new Error('The sole pilot administrator is already bootstrapped');
    const account = await client.query('SELECT id,is_active FROM users WHERE id=$1 FOR UPDATE', [options.account]);
    if (!account.rows.length || !account.rows[0].is_active) throw new Error('The specified synthetic account must already exist and be active');
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

async function main() {
  const options = args(process.argv.slice(2));
  if (!options.mode || !process.env.DATABASE_URL) throw new Error('Usage: --bootstrap|--recover --db NAME --confirm NAME --account synthetic_pilot_<suffix> --approval REF [--new-pin 6–12 digits]');
  assertSafety(process.env, options.confirm, options.db);
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  try {
    const result = options.mode === 'bootstrap' ? await bootstrap(pool, options) : await recover(pool, options);
    process.stdout.write(`${JSON.stringify({ success: true, ...result })}\n`);
  } finally { await pool.end(); }
}

if (require.main === module) main().catch(error => {
  process.stderr.write(`Synthetic administrator operation failed: ${error.message}\n`);
  process.exitCode = 1;
});

module.exports = { args, assertSafety, verifyActualDatabase, bootstrap, recover };
