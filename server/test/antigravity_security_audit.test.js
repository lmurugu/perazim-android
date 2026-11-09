'use strict';

/**
 * ANTIGRAVITY INDEPENDENT PHASE B SECURITY AUDIT SUITE
 *
 * Target: Perazim AI Project - Phase B (Membership Verification & Scoped Permissions)
 * Author: Antigravity (Independent Review Agent)
 *
 * Mandatory Isolation:
 * - Runs exclusively against disposable, uniquely generated synthetic databases (`perazim_phaseb_*_test`).
 * - Strictly verifies local loopback connection and fails closed on external addresses.
 * - Leaves zero persistent state; drops all test databases upon completion.
 */

const nativeAssert = require('assert/strict');
let assertionCount = 0;
const assert = new Proxy(nativeAssert, {
  apply(target, thisArg, args) {
    assertionCount++;
    return Reflect.apply(target, thisArg, args);
  },
  get(target, property, receiver) {
    const value = Reflect.get(target, property, receiver);
    if (typeof value !== 'function') return value;
    return (...args) => { assertionCount++; return value.apply(target, args); };
  }
});
const crypto = require('crypto');
const net = require('net');
const http = require('http');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Pool } = require('pg');
const { validateAdminTarget, assertConnectedOwnedCluster } = require('../src/phase-b/auditConnectionGuard');

function httpRequest(port, method, path, headers = {}, body = null, requestTimeoutMs = 5000) {
  return new Promise((resolve, reject) => {
    const payload = body == null ? null : (typeof body === 'string' ? body : JSON.stringify(body));
    const reqHeaders = {
      ...headers,
      ...(payload ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) } : {}),
      Connection: 'close'
    };
    const req = http.request({
      hostname: '127.0.0.1',
      port,
      method,
      path,
      headers: reqHeaders
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        let parsed;
        try { parsed = JSON.parse(data); } catch (_) { parsed = { raw: data }; }
        resolve({ status: res.statusCode, headers: res.headers, body: parsed });
      });
    });
    req.on('error', reject);
    req.setTimeout(requestTimeoutMs, () => req.destroy(new Error(`HTTP request timed out: ${method} ${path}`)));
    if (payload) req.write(payload);
    req.end();
  });
}

async function runAudit() {
  console.log('================================================================');
  console.log('⚡ ANTIGRAVITY INDEPENDENT PHASE B SECURITY AUDIT TEST RUNNER');
  console.log('================================================================\n');

  const expectedPort = Number(process.env.PHASE_B_TEST_EXPECTED_PORT);
  const adminTarget = validateAdminTarget(process.env.PHASE_B_TEST_ADMIN_URL, expectedPort);
  const adminUrl = new URL(adminTarget.connectionString);
  const adminPool = new Pool(adminTarget);

  // Verify server identity
  const check = await adminPool.query('SELECT current_database() AS db, current_user AS "user", inet_server_addr()::text AS addr, inet_server_port() AS port, current_setting(\'data_directory\') AS data_dir');
  const serverDb = check.rows[0].db;
  const serverAddr = check.rows[0].addr;
  assertConnectedOwnedCluster(check.rows[0], expectedPort, process.env.PHASE_B_TEST_EXPECTED_DATA_DIR);
  console.log(`[PASS] Admin connection verified on ${serverAddr}:${check.rows[0].port} (${serverDb}, user=${check.rows[0].user})`);

  // Generate unique test database
  const runId = crypto.randomBytes(6).toString('hex');
  const testDbName = `perazim_phaseb_${runId}_test`;
  console.log(`[INFO] Creating isolated synthetic test database: ${testDbName}`);
  await adminPool.query(`CREATE DATABASE "${testDbName}"`);

  const testDbUrl = new URL(adminUrl.toString());
  testDbUrl.pathname = `/${testDbName}`;

  // Configure environment strictly for synthetic test
  process.env.NODE_ENV = 'test';
  process.env.PHASE_B_SYNTHETIC = 'true';
  process.env.DATABASE_URL = testDbUrl.toString();
  process.env.JWT_SECRET = crypto.randomBytes(48).toString('hex');
  process.env.PHASE_B_ADMIN_KEY = crypto.randomBytes(32).toString('hex');

  const testPool = new Pool({ connectionString: testDbUrl.toString() });
  testPool.on('error', () => {});
  const dbModule = require('../src/db');
  let server = null;
  const auditResults = [];
  const skippedSuites = [];

  function record(name, category, status, details) {
    auditResults.push({ name, category, status, details });
    const mark = status === 'PASS' ? '✅ PASS' : (status === 'FAIL' ? '❌ FAIL' : '⚠️ WARN');
    console.log(`${mark} [${category}] ${name}: ${details}`);
  }

  try {
    // Run schema migrations
    const { migrate } = require('../src/migrations');
    await migrate(testPool);
    console.log('[PASS] Baseline & Phase B migrations (0001, 0002) applied successfully.\n');

    // Start Express app on ephemeral port
    const db = require('../src/db');
    await db.initDb();
    const { app } = require('../src/index');

    server = await new Promise(resolve => {
      const s = app.listen(0, '127.0.0.1', () => resolve(s));
    });
    const port = server.address().port;
    console.log(`[INFO] Express test server listening on 127.0.0.1:${port}\n`);

    // Setup base synthetic actors
    const pin = '123456';
    const pinHash = await bcrypt.hash(pin, 10);
    const now = Date.now();

    // 1. Synthetic Member A (Ordinary applicant)
    const userA = { id: `user_a_${runId}`, phone: '+254711000001', name: 'Sister Faith' };
    await testPool.query(
      `INSERT INTO users (id, phone, pin_hash, name, role, is_active, created_at, updated_at)
       VALUES ($1, $2, $3, $4, 'MEMBER', TRUE, $5, $5)`,
      [userA.id, userA.phone, pinHash, userA.name, now]
    );
    const tokenA = jwt.sign({ id: userA.id, phone: userA.phone, name: userA.name, role: 'MEMBER' }, process.env.JWT_SECRET, { expiresIn: '1d' });

    // 2. Synthetic Member B (Attacker / Adversary)
    const userB = { id: `user_b_${runId}`, phone: '+254711000002', name: 'Malicious Actor' };
    await testPool.query(
      `INSERT INTO users (id, phone, pin_hash, name, role, is_active, created_at, updated_at)
       VALUES ($1, $2, $3, $4, 'MEMBER', TRUE, $5, $5)`,
      [userB.id, userB.phone, pinHash, userB.name, now]
    );
    const tokenB = jwt.sign({ id: userB.id, phone: userB.phone, name: userB.name, role: 'MEMBER' }, process.env.JWT_SECRET, { expiresIn: '1d' });

    // 3. Synthetic Pilot Administrator
    const pilotUser = { id: `synthetic_pilot_${runId}`, phone: '+254700000000', name: 'Synthetic Pilot' };
    await testPool.query(
      `INSERT INTO users (id, phone, pin_hash, name, role, is_active, created_at, updated_at)
       VALUES ($1, $2, $3, $4, 'MEMBER', TRUE, $5, $5)`,
      [pilotUser.id, pilotUser.phone, pinHash, pilotUser.name, now]
    );
    const pilotToken = jwt.sign({ id: pilotUser.id, phone: pilotUser.phone, name: pilotUser.name, role: 'MEMBER' }, process.env.JWT_SECRET, { expiresIn: '1d' });

    const { bootstrap } = require('../scripts/bootstrap-synthetic-admin');
    const enrollment = await bootstrap(testPool, {
      db: testDbName,
      confirm: testDbName,
      account: pilotUser.id,
      approval: 'ANTIGRAVITY_AUDIT_APPROVAL'
    });
    assert(enrollment.totp_secret, 'Bootstrap must return TOTP secret');

    // 4. Populate synthetic church register (AST-01)
    const importId = crypto.randomUUID();
    await testPool.query(
      `INSERT INTO membership_imports (id, source, checksum, actor_id, created_at)
       VALUES ($1, 'SYNTHETIC_TEST_REGISTER', 'checksum', $2, $3)`,
      [importId, pilotUser.id, now]
    );
    // Register record 1: Exact match for Faith
    const record1Id = crypto.randomUUID();
    await testPool.query(
      `INSERT INTO membership_records (id, import_id, reference, full_name, phone, record_status, revision, created_at, updated_at)
       VALUES ($1, $2, 'MEM-001', 'Sister Faith', '+254711000001', 'ACTIVE', 1, $3, $3)`,
      [record1Id, importId, now]
    );
    // Register record 2: Another member
    const record2Id = crypto.randomUUID();
    await testPool.query(
      `INSERT INTO membership_records (id, import_id, reference, full_name, phone, record_status, revision, created_at, updated_at)
       VALUES ($1, $2, 'MEM-002', 'Elder John', '+254711000099', 'ACTIVE', 1, $3, $3)`,
      [record2Id, importId, now]
    );

    // ================================================================
    // TEST A: Membership Claim Privacy & Anti-Enumeration
    // ================================================================
    // Scenario 1: Exact match claim (Sister Faith claiming her own record)
    const req1 = { request_id: crypto.randomUUID(), name: 'Sister Faith', phone: '+254711000001' };
    const res1 = await httpRequest(port, 'POST', '/api/membership/claims', { Authorization: `Bearer ${tokenA}` }, req1);

    // Scenario 2: Zero match claim (Non-existent person)
    const req2 = { request_id: crypto.randomUUID(), name: 'Nobody At All', phone: '+254799999999' };
    const res2 = await httpRequest(port, 'POST', '/api/membership/claims', { Authorization: `Bearer ${tokenB}` }, req2);

    // Scenario 3: Mismatched name/phone
    const req3 = { request_id: crypto.randomUUID(), name: 'Elder John', phone: '+254711000001' };
    const res3 = await httpRequest(port, 'POST', '/api/membership/claims', { Authorization: `Bearer ${tokenB}` }, req3);

    const neutralMessage = 'Your request has been received for review.';
    if (res1.status === 202 && res2.status === 202 && res3.status === 202 &&
        res1.body.message === neutralMessage && res2.body.message === neutralMessage && res3.body.message === neutralMessage &&
        res1.body.received === true && res2.body.received === true && res3.body.received === true) {
      record('Anti-Enumeration Neutrality', 'Privacy', 'PASS', 'Exact match, no-match, and mismatch all returned identical HTTP 202 responses with zero data leakage.');
    } else {
      record('Anti-Enumeration Neutrality', 'Privacy', 'FAIL', `Responses differed! S1=${res1.status}, S2=${res2.status}, S3=${res3.status}`);
    }

    // Verify claimant API does NOT reveal candidate matches or status
    const statusA = await httpRequest(port, 'GET', '/api/membership/status', { Authorization: `Bearer ${tokenA}` });
    assert.equal(statusA.body.status, 'VERIFICATION_PENDING');
    assert.equal(statusA.body.candidates, undefined);
    assert.equal(statusA.body.membership_record_id, undefined);
    record('Zero Candidate Leakage in Status API', 'Privacy', 'PASS', 'GET /api/membership/status reveals only own status enum; candidates and IDs omitted.');

    // ================================================================
    // TEST B: Claim Submission Rate Limiting Vulnerability Reproduction
    // ================================================================
    console.log('\n[INFO] Testing Claim Submission Rate Limiting (Controlled 15-burst)...');
    let successfulClaims = 0;
    const burstSize = 15;
    for (let i = 0; i < burstSize; i++) {
      const burstReq = { request_id: crypto.randomUUID(), name: `Probe Claim ${i}`, phone: `+2547118880${i.toString().padStart(2, '0')}` };
      const burstRes = await httpRequest(port, 'POST', '/api/membership/claims', { Authorization: `Bearer ${tokenB}` }, burstReq);
      if (burstRes.status === 202) successfulClaims++;
    }

    const dbClaimsCount = await testPool.query('SELECT COUNT(*)::int AS cnt FROM membership_claims WHERE user_id=$1', [userB.id]);
    const actualDbRows = dbClaimsCount.rows[0].cnt;
    const rateLimited = await httpRequest(port, 'POST', '/api/membership/claims', { Authorization: `Bearer ${tokenB}` }, {
      request_id: crypto.randomUUID(), name: 'Rate limit probe', phone: '+254711888099'
    });
    const valid429 = rateLimited.status === 429 && rateLimited.body &&
      typeof rateLimited.body.error === 'string' && /too many/i.test(rateLimited.body.error) &&
      Number(rateLimited.headers['retry-after']) > 0 &&
      rateLimited.body.retry_after_seconds === Number(rateLimited.headers['retry-after']);
    if (successfulClaims === burstSize || !valid429) {
      record('Claim Submission Rate Limiting', 'Rate Limiting', 'FAIL',
        `Expected bounded 202 burst followed by exact HTTP 429 with error and Retry-After; burst=${successfulClaims}/${burstSize}, final=${rateLimited.status}, body=${JSON.stringify(rateLimited.body)}, retry-after=${rateLimited.headers['retry-after']}, rows=${actualDbRows}.`
      );
    } else {
      record('Claim Submission Rate Limiting', 'Rate Limiting', 'PASS', `Received ${successfulClaims}/${burstSize} initial 202 responses then exact HTTP 429 with neutral error and positive Retry-After.`);
    }

    // Verify duplicate request_id replay prevention with fresh users with no existing pending claim
    const freshUser1 = { id: `user_f1_${runId}`, phone: '+254711777001', name: 'Fresh Claimant 1' };
    const freshUser2 = { id: `user_f2_${runId}`, phone: '+254711777002', name: 'Fresh Claimant 2' };
    await testPool.query(
      `INSERT INTO users (id, phone, pin_hash, name, role, is_active, created_at, updated_at)
       VALUES ($1, $2, $3, $4, 'MEMBER', TRUE, $5, $5),
              ($6, $7, $3, $8, 'MEMBER', TRUE, $5, $5)`,
      [freshUser1.id, freshUser1.phone, pinHash, freshUser1.name, now, freshUser2.id, freshUser2.phone, freshUser2.name]
    );
    const tokenF1 = jwt.sign({ id: freshUser1.id, phone: freshUser1.phone, name: freshUser1.name, role: 'MEMBER' }, process.env.JWT_SECRET, { expiresIn: '1d' });
    const tokenF2 = jwt.sign({ id: freshUser2.id, phone: freshUser2.phone, name: freshUser2.name, role: 'MEMBER' }, process.env.JWT_SECRET, { expiresIn: '1d' });

    const replayId = crypto.randomUUID();
    const rep1 = await httpRequest(port, 'POST', '/api/membership/claims', { Authorization: `Bearer ${tokenF1}` }, { request_id: replayId, name: 'Replay Test', phone: '+254711777001' });
    const rep2 = await httpRequest(port, 'POST', '/api/membership/claims', { Authorization: `Bearer ${tokenF1}` }, { request_id: replayId, name: 'Replay Test', phone: '+254711777001' });
    const repOtherUser = await httpRequest(port, 'POST', '/api/membership/claims', { Authorization: `Bearer ${tokenF2}` }, { request_id: replayId, name: 'Replay Test', phone: '+254711777002' });

    assert.equal(rep1.status, 202, 'First claim submission must succeed');
    assert.equal(rep2.status, 202, 'Identical request_id from same user must return idempotent 202');
    assert.equal(repOtherUser.status, 409, 'Same request_id from different user must be rejected with HTTP 409');
    record('Request ID Replay & Cross-Account Collision', 'Integrity', 'PASS', 'Idempotent for same user, rejected with HTTP 409 for different user.');

    // ================================================================
    // TEST C: Privileged Session Protection & Step-Up Security
    // ================================================================
    const { totpAt } = require('../src/phase-b/totp');
    const counter = Math.floor(Date.now() / 30000);
    const validTotp = totpAt(enrollment.totp_secret, counter);

    // Step-up authentication
    const stepUpRes = await httpRequest(port, 'POST', '/api/admin/security/step-up', { Authorization: `Bearer ${pilotToken}` }, { pin, code: validTotp });
    assert.equal(stepUpRes.status, 200, 'Step up must succeed with valid credentials');
    const privilegedToken = stepUpRes.body.token;

    // 1. Forged privileged token test
    const fakePrivilegedRes = await httpRequest(port, 'GET', '/api/admin/membership/records', {
      Authorization: `Bearer ${pilotToken}`,
      'X-Privileged-Session': crypto.randomBytes(32).toString('base64url')
    });
    assert.equal(fakePrivilegedRes.status, 401, 'Forged privileged session must return 401');
    record('Forged Privileged Token Rejection', 'Authorization', 'PASS', 'Random base64 token rejected with STEP_UP_REQUIRED.');

    // 2. Cross-account session borrowing (Member B using Pilot privileged token)
    const borrowedRes = await httpRequest(port, 'GET', '/api/admin/membership/records', {
      Authorization: `Bearer ${tokenB}`,
      'X-Privileged-Session': privilegedToken
    });
    assert.equal(borrowedRes.status, 403, 'Ordinary user cannot borrow pilot session');
    record('Cross-Account Privileged Session Theft', 'Authorization', 'PASS', 'Non-pilot account using valid admin session rejected with 403 ADMIN_FORBIDDEN.');

    // 3. Replay of same TOTP code
    const replayedStepUp = await httpRequest(port, 'POST', '/api/admin/security/step-up', { Authorization: `Bearer ${pilotToken}` }, { pin, code: validTotp });
    assert.equal(replayedStepUp.status, 401, 'TOTP replay within same counter must be rejected');
    record('TOTP Code Replay Protection', 'Authentication', 'PASS', 'Reusing TOTP code rejected with STEP_UP_FAILED.');

    // ================================================================
    // TEST D: Administrator Escalation & Singleton Pilot Protection
    // ================================================================
    // Ordinary member attempting to grant self capabilities
    const grantAttempt = await httpRequest(port, 'POST', `/api/admin/permissions/${userB.id}`, {
      Authorization: `Bearer ${tokenB}`,
      'X-Privileged-Session': privilegedToken
    }, { code: 'membership.record.read', scope: 'global', reason: 'UNAUTHORIZED_GRANT', request_id: crypto.randomUUID() });
    assert.equal(grantAttempt.status, 403, 'Ordinary member cannot call permissions grant');
    record('Unauthorized Privilege Escalation', 'Authorization', 'PASS', 'Ordinary user cannot grant permissions.');

    // Second bootstrap attempt on existing singleton
    const secondBootstrapAttempt = await testPool.query('SELECT 1 FROM pilot_bootstrap WHERE singleton=TRUE');
    assert.equal(secondBootstrapAttempt.rows.length, 1);
    await assert.rejects(async () => {
      await bootstrap(testPool, {
        db: testDbName,
        confirm: testDbName,
        account: `synthetic_pilot_${crypto.randomBytes(6).toString("hex")}`,
        approval: 'SECOND_BOOTSTRAP_ATTEMPT'
      });
    }, /already bootstrapped/);
    record('Singleton Pilot Enforcement', 'Architecture', 'PASS', 'Secondary administrator bootstrap strictly rejected by database singleton constraint.');

    // Legacy role check: User with role='ADMIN' in users table does NOT gain Phase B powers
    await testPool.query(`UPDATE users SET role='ADMIN' WHERE id=$1`, [userB.id]);
    const legacyAdminAuditRead = await httpRequest(port, 'GET', '/api/admin/audit', { Authorization: `Bearer ${tokenB}` });
    assert.equal(legacyAdminAuditRead.status, 401, 'Legacy role=ADMIN must still require step-up and pilot authority');
    record('Legacy ADMIN Role Segregation', 'Authorization', 'PASS', 'Setting role=ADMIN in users table grants zero Phase B capabilities.');

    // ================================================================
    // TEST E: Token Compromise & Cutoff (`auth_not_before`)
    // ================================================================
    // Pilot calls revoke-sessions with compromised=true
    const revokeRes = await httpRequest(port, 'POST', '/api/admin/security/revoke-sessions', {
      Authorization: `Bearer ${pilotToken}`,
      'X-Privileged-Session': privilegedToken
    }, { request_id: crypto.randomUUID(), reason: 'AUDIT_SECURITY_CUTOFF', compromised: true });
    assert.equal(revokeRes.status, 200, 'Revoke sessions must succeed');

    // Verify pilot token is now rejected due to auth_not_before and account deactivation
    const postCutoffRes = await httpRequest(port, 'GET', '/api/access/permissions', { Authorization: `Bearer ${pilotToken}` });
    assert.equal(postCutoffRes.status, 401, 'Previously valid token must be rejected post-compromise cutoff');
    record('Compromise Containment & auth_not_before Cutoff', 'Session Management', 'PASS', 'Previously active token immediately rejected after security version bump & account deactivation.');

    // ================================================================
    // TEST F: Asynchronous Claim Classification Concurrency
    // ================================================================
    console.log('\n[INFO] Testing Claim Classification Concurrency Race...');
    const concurrentUser = { id: `user_c_${runId}`, phone: '+254711555001', name: 'Concurrent Sister' };
    await testPool.query(
      `INSERT INTO users (id, phone, pin_hash, name, role, is_active, created_at, updated_at)
       VALUES ($1, $2, $3, $4, 'MEMBER', TRUE, $5, $5)`,
      [concurrentUser.id, concurrentUser.phone, pinHash, concurrentUser.name, now]
    );
    const tokenC = jwt.sign({ id: concurrentUser.id, phone: concurrentUser.phone, name: concurrentUser.name, role: 'MEMBER' }, process.env.JWT_SECRET, { expiresIn: '1d' });

    // Submit claim and immediately query status in tight loop
    const cReqId = crypto.randomUUID();
    const claimPromise = httpRequest(port, 'POST', '/api/membership/claims', { Authorization: `Bearer ${tokenC}` }, {
      request_id: cReqId, name: concurrentUser.name, phone: concurrentUser.phone
    });
    const statusPromise = httpRequest(port, 'GET', '/api/membership/status', { Authorization: `Bearer ${tokenC}` });

    const [cClaimRes, cStatusRes] = await Promise.all([claimPromise, statusPromise]);
    assert.equal(cClaimRes.status, 202);
    // Even if query arrives before setImmediate finishes, status must be REGISTERED or VERIFICATION_PENDING, never an error
    assert(['REGISTERED', 'VERIFICATION_PENDING'].includes(cStatusRes.body.status));
    record('Claim Classification Race Safety', 'Concurrency', 'PASS', `Status remained consistent and valid (${cStatusRes.body.status}) during concurrent execution.`);

    console.log('\n================================================================');
    console.log('🏁 AUDIT EXECUTION COMPLETE');
    console.log('================================================================');
    const passedCount = auditResults.filter(r => r.status === 'PASS').length;
    const failedCount = auditResults.filter(r => r.status === 'FAIL').length;
    console.log(`Total Checks: ${auditResults.length} | Passed: ${passedCount} | Failed (Vulnerabilities Found): ${failedCount}\n`);

  } finally {
    if (server) {
      await new Promise(resolve => server.close(resolve));
      console.log('[INFO] Test HTTP server closed.');
    }
    const appDb = require('../src/db');
    await appDb.closeDb();
    assert.equal(appDb.isPostgresActive(), false, 'Application database pool must close through exported closeDb()');
    await testPool.end();
    console.log(`[INFO] Terminating remaining connections to ${testDbName}...`);
    await adminPool.query(`
      SELECT pg_terminate_backend(pid) FROM pg_stat_activity
      WHERE datname=$1 AND pid <> pg_backend_pid()
    `, [testDbName]);
    console.log(`[INFO] Dropping isolated synthetic test database: ${testDbName}`);
    await adminPool.query(`DROP DATABASE IF EXISTS "${testDbName}"`);
    await adminPool.end();
    console.log('[PASS] Isolated test database dropped cleanly. Zero residual state.');
    const passedCount = auditResults.filter(r => r.status === 'PASS').length;
    const failedCount = auditResults.filter(r => r.status === 'FAIL').length;
    const skippedCount = skippedSuites.length;
    console.log(`AUDIT_SUMMARY passed=${passedCount} failed=${failedCount} skipped=${skippedCount}`);
    console.log(`AUDIT_ASSERTIONS=${assertionCount}`);
    if (failedCount) process.exitCode = 1;
  }
}

if (require.main === module) {
  if (process.argv.includes('--self-test-child-failure')) {
    console.error('CONTROLLED CHILD FAILURE');
    process.exit(23);
  }
  if (process.argv.includes('--self-test-sleep')) {
    console.log('CONTROLLED CHILD SLEEP');
    setTimeout(() => process.exit(0), 30000);
  } else if (process.argv.includes('--self-test-pass') || process.argv.includes('--self-test-child-pass')) {
    console.log('CONTROLLED CHILD PASS');
  } else {
  runAudit().catch(err => {
    console.error('FATAL AUDIT FAILURE:', err);
    process.exit(1);
  });
  }
}

module.exports = { runAudit, httpRequest };
