'use strict';

const assert = require('assert/strict');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');
const { migrate } = require('../src/migrations');

async function run() {
  assert.notEqual(process.env.NODE_ENV, 'production');
  const adminUrl = new URL(process.env.PHASE_B_TEST_ADMIN_URL || '');
  assert.equal(adminUrl.pathname, '/postgres');
  assert(['127.0.0.1', 'localhost', '[::1]'].includes(adminUrl.hostname));
  const suffix = crypto.randomBytes(8).toString('hex');
  const database = `perazim_phaseb_${suffix}_test`;
  const pilot = `synthetic_pilot_${suffix}`;
  const admin = new Pool({ connectionString: adminUrl.toString() });
  const db = require('../src/db');
  let pool, server;
  let checks = 0;
  function check(actual, expected, message) { assert.equal(actual, expected, message); checks++; }
  try {
    await admin.query(`CREATE DATABASE "${database}"`);
    adminUrl.pathname = '/' + database;
    Object.assign(process.env, {
      DATABASE_URL: adminUrl.toString(), NODE_ENV: 'test', PHASE_B_SYNTHETIC: 'true',
      JWT_SECRET: crypto.randomBytes(48).toString('hex'), PHASE_B_ADMIN_KEY: crypto.randomBytes(32).toString('hex')
    });
    pool = new Pool({ connectionString: adminUrl.toString() });
    await migrate(pool);
    const pin = '853692';
    const now = Date.now();
    await pool.query(`INSERT INTO users(id,phone,pin_hash,name,role,is_active,created_at,updated_at)
      VALUES($1,'+254700009882',$2,'Synthetic Pilot','MEMBER',TRUE,$3,$3)`, [pilot, await bcrypt.hash(pin, 10), now]);
    const { bootstrap, recover } = require('../scripts/bootstrap-synthetic-admin');
    const options = { db: database, confirm: database, account: pilot, approval: 'SYNTHETIC_BOOTSTRAP_APPROVAL' };
    const enrollment = await bootstrap(pool, options);
    await assert.rejects(() => bootstrap(pool, options), /already bootstrapped/); checks++;
    check((await pool.query('SELECT role FROM users WHERE id=$1', [pilot])).rows[0].role, 'MEMBER', 'bootstrap does not change organisational role');
    await db.initDb();
    const { app } = require('../src/index');
    server = await new Promise(resolve => { const value = app.listen(0, '127.0.0.1', () => resolve(value)); });
    const base = `http://127.0.0.1:${server.address().port}`;
    async function request(method, path, token, body, privileged) {
      const response = await fetch(base + path, { method, headers: {
        'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}),
        ...(privileged ? { 'X-Privileged-Session': privileged } : {})
      }, ...(body !== undefined ? { body: JSON.stringify(body) } : {}) });
      return { status: response.status, body: await response.json() };
    }
    const login = await request('POST', '/api/auth/login', null, { phone: '+254700009882', pin });
    check(login.status, 200);
    const token = login.body.token;
    const { totpAt, matchingCounter, encryptSecret, decryptSecret } = require('../src/phase-b/totp');
    check(totpAt('GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ', 1), '287082', 'RFC4226 vector');
    const encrypted = encryptSecret('SYNTHETIC_SECRET');
    check(decryptSecret(encrypted), 'SYNTHETIC_SECRET');
    await assert.rejects(async () => decryptSecret(encrypted.slice(0, -2) + (encrypted.endsWith('00') ? '01' : '00'))); checks++;
    const counter = Math.floor(Date.now() / 30000);
    const code = totpAt(enrollment.totp_secret, counter);
    check(matchingCounter(enrollment.totp_secret, code, Date.now(), counter), null, 'counter replay rejected');
    check((await request('GET', '/api/admin/audit', token)).status, 401, 'step-up required');
    const step = await request('POST', '/api/admin/security/step-up', token, { pin, code });
    check(step.status, 200, JSON.stringify(step.body));
    const privileged = step.body.token;
    check((await request('POST', '/api/admin/security/step-up', token, { pin, code })).status, 401, 'TOTP replay denied');
    async function privilegedRequest(method, path, body) { return request(method, '/api/admin' + path, token, body, privileged); }
    const users = [];
    for (let i = 0; i < 3; i++) {
      const registered = await request('POST', '/api/auth/register', null, {
        name: `Synthetic Claim ${i}`, phone: `+25470000989${i}`, pin, role: 'ADMIN', membership_status: 'VERIFIED_MEMBER'
      });
      check(registered.status, 201);
      users.push(registered.body);
      check((await request('GET', '/api/membership/status', registered.body.token)).body.status, 'REGISTERED');
    }
    check((await request('GET', '/api/membership/status')).status, 401);
    check((await request('GET', '/api/membership/member-resource', users[0].token)).status, 403);
    check((await request('GET', '/api/admin/membership/records', users[0].token, undefined, privileged)).status, 403, 'non-pilot cannot borrow privileged session');
    const records = [
      { reference: 'SYNTHETIC_A', full_name: 'Synthetic Claim 0', phone: '+254700009890' },
      { reference: 'SYNTHETIC_B', full_name: 'Synthetic Claim 1', phone: '+254700009891' },
      { reference: 'SYNTHETIC_C', full_name: 'Synthetic Claim 1', phone: '+254700009891' }
    ];
    const imported = await privilegedRequest('POST', '/membership/import', { source: 'SYNTHETIC_FIXTURE', records, request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST' });
    check(imported.status, 201, JSON.stringify(imported.body));
    check((await privilegedRequest('POST', '/membership/import', { source: 'SYNTHETIC_FIXTURE', records, request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST' })).status, 409, 'duplicates cannot overwrite');
    for (let i = 0; i < users.length; i++) {
      const body = { request_id: crypto.randomUUID(), name: users[i].user.name, phone: users[i].user.phone };
      const claim = await request('POST', '/api/membership/claims', users[i].token, body);
      check(claim.status, 202);
      check(Object.keys(claim.body).sort().join(','), 'message,received,request_id', 'neutral receipt contains no matching metadata');
      check((await request('GET', '/api/membership/status', users[i].token)).body.status, 'VERIFICATION_PENDING', 'exact, missing and duplicate matches have identical public state');
      if (i === 0) {
        const duplicate = await request('POST', '/api/membership/claims', users[i].token, body);
        check(duplicate.status, 202, 'request replay is safely acknowledged');
        check((await request('POST', '/api/membership/claims', users[i].token, { ...body, request_id: crypto.randomUUID() })).status, 202);
        check((await pool.query('SELECT count(*)::int AS count FROM membership_claims WHERE user_id=$1', [users[i].user.id])).rows[0].count, 1, 'one open claim per account prevents flooding the review queue');
      }
    }
    const listing = await privilegedRequest('GET', '/membership/claims');
    check(listing.status, 200, JSON.stringify(listing.body));
    const claim = listing.body.claims.find(value => value.user_id === users[0].user.id);
    const registry = await privilegedRequest('GET', '/membership/records');
    check(registry.status, 200);
    const record = registry.body.records.find(value => value.reference === 'SYNTHETIC_A');
    const decided = await privilegedRequest('POST', `/membership/claims/${claim.id}/decision`, { decision: 'APPROVE', record_id: record.id, request_id: crypto.randomUUID(), reason: 'IDENTITY_CONFIRMED' });
    check(decided.status, 200, JSON.stringify(decided.body));
    check((await request('GET', '/api/membership/status', users[0].token)).body.status, 'VERIFIED_MEMBER');
    const notice = (await request('GET', '/api/community/notifications', users[0].token)).body.notifications[0];
    check(notice.type, 'MEMBERSHIP_STATUS');
    check(notice.body, 'Open Membership in your profile to check your status.', 'notification does not expose register information');
    check((await request('GET', '/api/community/notifications', users[1].token)).body.notifications.length, 0, 'another account cannot see the notification');
    check((await request('GET', '/api/membership/member-resource', users[0].token)).status, 200);
    check((await request('GET', '/api/admin/audit', users[0].token, undefined, privileged)).status, 403, 'verified member is not administrator');
    await request('POST', '/api/membership/claims', users[0].token, { request_id: crypto.randomUUID(), name: 'Different synthetic claim', phone: users[0].user.phone });
    const newClaims = await privilegedRequest('GET', '/membership/claims');
    const additional = newClaims.body.claims.find(value => value.user_id === users[0].user.id);
    check((await privilegedRequest('POST', `/membership/claims/${additional.id}/decision`, { decision: 'REJECT', request_id: crypto.randomUUID(), reason: 'INSUFFICIENT_EVIDENCE' })).status, 200);
    check((await request('GET', '/api/membership/status', users[0].token)).body.status, 'VERIFIED_MEMBER', 'rejected additional attempt preserves verified link');
    check((await privilegedRequest('POST', `/membership/users/${users[0].user.id}/revoke`, { request_id: crypto.randomUUID(), reason: 'VERIFICATION_ERROR' })).status, 200);
    check((await request('GET', '/api/membership/member-resource', users[0].token)).status, 403);
    // Revocation must survive another first-use/self-service permission refresh.
    await request('GET', '/api/access/permissions', users[2].token);
    await pool.query("UPDATE user_permissions SET revoked_at=$2,revoked_by=$1 WHERE user_id=$1 AND permission_code='membership.claim.self.submit'", [users[2].user.id, Date.now()]);
    check((await request('POST', '/api/membership/claims', users[2].token, { request_id: crypto.randomUUID(), name: users[2].user.name, phone: users[2].user.phone })).status, 403, 'revoked baseline grant not recreated');
    const originalGrant = await pool.query("SELECT id FROM user_permissions WHERE user_id=$1 AND permission_code='membership.record.read'", [pilot]);
    check((await privilegedRequest('DELETE', `/permissions/${pilot}/${originalGrant.rows[0].id}`, { request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST' })).status, 200);
    check((await privilegedRequest('GET', '/membership/records')).status, 403, 'revoked admin permission effective immediately');
    const scoped = await privilegedRequest('POST', `/permissions/${pilot}`, {
      code: 'membership.record.read', scope: record.id, request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST'
    });
    check(scoped.status, 201);
    const scopedRecords = await privilegedRequest('GET', '/membership/records');
    check(scopedRecords.status, 200);
    check(scopedRecords.body.records.map(value => value.id).join(','), record.id, 'scope filters out all other register entries');
    await pool.query("UPDATE permissions SET active=FALSE WHERE code='membership.record.read'");
    check((await privilegedRequest('GET', '/membership/records')).status, 403, 'disabled permission denies even an otherwise valid assignment');
    await pool.query("UPDATE permissions SET active=TRUE WHERE code='membership.record.read'");
    await pool.query('UPDATE user_permissions SET effective_at=$2,expires_at=$3 WHERE id=$1', [scoped.body.grant.id, Date.now() - 2000, Date.now() - 1000]);
    check((await privilegedRequest('GET', '/membership/records')).status, 403, 'expired scoped assignment denied');
    check((await privilegedRequest('POST', `/permissions/${users[0].user.id}`, { code: 'admin.permissions.grant', request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST' })).status, 403, 'cannot create another administrator');
    check((await privilegedRequest('GET', '/audit')).status, 200);
    const audit = await pool.query('SELECT action FROM admin_audit_log');
    assert(audit.rows.some(row => row.action.includes('READ'))); checks++;
    await assert.rejects(() => pool.query("UPDATE admin_audit_log SET outcome='ALTERED'"), /append-only/); checks++;
    check((await privilegedRequest('POST', '/security/revoke-sessions', { compromised: true, reason: 'ACCOUNT_COMPROMISE', request_id: crypto.randomUUID() })).status, 200);
    check((await request('GET', '/api/auth/me', token)).status, 401, 'compromised bearer rejected');
    const recovery = await recover(pool, { ...options, approval: 'SYNTHETIC_RECOVERY_APPROVAL', newPin: '792631' });
    await new Promise(resolve => setTimeout(resolve, Math.max(0, recovery.auth_not_before - Date.now() + 30)));
    check((await request('GET', '/api/auth/me', token)).status, 401, 'recovered account rejects old bearer');
    check((await request('POST', '/api/auth/login', null, { phone: '+254700009882', pin: '792631' })).status, 200, 'same identity can authenticate after recovery');
  } finally {
    if (server) await new Promise(resolve => server.close(resolve));
    await db.closeDb();
    if (pool) await pool.end();
    try {
      for (let attempt = 0; ; attempt++) {
        try { await admin.query(`DROP DATABASE IF EXISTS "${database}"`); break; }
        catch (error) {
          if (error.code !== '55006' || attempt === 4) throw error;
          await new Promise(resolve => setTimeout(resolve, 150));
        }
      }
    } finally { await admin.end(); }
  }
    console.log(`Phase B workflow gate passed: ${checks} assertions; synthetic bootstrap, neutral claims, review, verification, revocation, permission isolation, audit, TOTP and recovery.`);
}
run().catch(error => { console.error('Phase B workflow gate failed:', error.message); process.exitCode = 1; });
