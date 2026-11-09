'use strict';

const assert = require('node:assert/strict');
const { fork } = require('node:child_process');
const { randomBytes, randomUUID } = require('node:crypto');
const { once } = require('node:events');
const { setTimeout: wait } = require('node:timers/promises');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { migrate } = require('../src/migrations');
const { hash } = require('../src/phase-b/security');
const { totpAt } = require('../src/phase-b/totp');
const { bootstrap, recover } = require('../scripts/bootstrap-synthetic-admin');

const deadline = () => ({ signal: AbortSignal.timeout(10000) });
const origin = Date.UTC(2030, 0, 1);
const stepMs = 30000;
const lockoutMs = 15 * 60000;
const pin = '853692';
const workerFlag = '--worker';

function localUrl(raw, pathPattern) {
  const url = new URL(raw);
  assert.equal(url.protocol, 'postgres:');
  assert.equal(url.hostname, '127.0.0.1');
  assert(url.port && url.port !== '5433' && !url.search);
  assert.match(url.pathname, pathPattern);
  return url;
}

async function serve() {
  assert(process.send && process.env.NODE_ENV === 'test' && process.env.PHASE_B_SYNTHETIC === 'true');
  localUrl(process.env.DATABASE_URL, /^\/perazim_phaseb_[a-f0-9]{16}_test$/);
  const db = require('../src/db');
  await db.initDb();
  assert(db.isPostgresActive());
  const server = require('../src/index').app.listen(0, '127.0.0.1');
  await once(server, 'listening', deadline());
  let now = origin;
  Date.now = () => now;
  process.on('disconnect', () => process.exit());
  process.on('message', async command => {
    if (command === 'stop') {
      await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
      await db.closeDb();
      process.disconnect();
    } else {
      now = command;
      process.send(now);
    }
  });
  process.send(server.address().port);
}

async function run() {
  assert.equal(process.env.NODE_ENV, 'test');
  assert.match(process.env.ADM01_CLUSTER || '', /^\/tmp\/perazim-adm01\.[a-zA-Z0-9]+\/data$/);
  const url = localUrl(process.env.PHASE_B_TEST_ADMIN_URL, /^\/postgres$/);
  const admin = new Pool({ connectionString: url.href, connectionTimeoutMillis: 5000, statement_timeout: 10000 });
  const fixtures = [];
  const workers = new Set();
  let stoppedWorkers = 0;
  const key = randomBytes(32).toString('hex');
  const jwtSecret = randomBytes(48).toString('hex');
  const pilot = 'synthetic_pilot_adm01main';
  const otherPilot = 'synthetic_pilot_adm01other';
  const approval = 'ADM01_BOOTSTRAP';
  let assertions = 0;
  let passed = 0;

  function check(actual, expected, detail) {
    assert.deepEqual(actual, expected, detail);
    assertions++;
  }

  async function rejects(action, pattern) {
    await assert.rejects(action, pattern);
    assertions++;
  }

  async function group(label, test) {
    const start = assertions;
    await test();
    passed++;
    console.log(`PASS ${label}: ${assertions - start} assertions`);
  }

  async function fixture() {
    const name = `perazim_phaseb_${randomBytes(8).toString('hex')}_test`;
    await admin.query(`CREATE DATABASE "${name}"`);
    const databaseUrl = new URL(name, url).href;
    const pool = new Pool({ connectionString: databaseUrl, connectionTimeoutMillis: 5000, statement_timeout: 10000 });
    const env = {
      NODE_ENV: 'test', PHASE_B_SYNTHETIC: 'true', DATABASE_URL: databaseUrl,
      PHASE_B_ADMIN_KEY: key, JWT_SECRET: jwtSecret
    };
    const options = { db: name, confirm: name, account: pilot, approval };
    const result = { name, pool, env, options };
    fixtures.push(result);
    await migrate(pool);
    const pinHash = await bcrypt.hash(pin, 4);
    for (const [index, id, active] of [[1, pilot, true], [2, otherPilot, true], [3, 'synthetic_pilot_inactive', false], [4, 'ordinary_synthetic', true]]) {
      await pool.query(
        `INSERT INTO users(id,phone,pin_hash,name,role,is_active,created_at,updated_at)
         VALUES ($1,$2,$3,'ADM-01 synthetic','MEMBER',$4,0,0)`,
        [id, `+25470000700${index}`, pinHash, active]
      );
    }
    return result;
  }

  async function launch(target) {
    const child = fork(__filename, [workerFlag], { env: target.env, stdio: ['ignore', 'ignore', 'inherit', 'ipc'] });
    workers.add(child);
    const [port] = await once(child, 'message', deadline());
    check(Number.isInteger(port), true, 'worker is listening on loopback');
    return { child, port };
  }

  async function stopWorker(child) {
    if (child.exitCode !== null || child.signalCode !== null) throw new Error(`Worker ${child.pid} exited before cleanup`);
    const exit = once(child, 'exit', deadline());
    try {
      child.send('stop');
      assert.deepEqual(await exit, [0, null]);
      workers.delete(child);
      stoppedWorkers++;
    } catch (error) {
      if (child.exitCode === null && child.signalCode === null) {
        const killed = once(child, 'exit', deadline());
        child.kill('SIGKILL');
        await killed;
      }
      throw error;
    }
  }

  async function setClock(time, ...targets) {
    await Promise.all(targets.map(async ({ child }) => {
      const reply = once(child, 'message', deadline());
      child.send(time);
      check((await reply)[0], time, 'worker clock acknowledged');
    }));
  }

  function bearer(id = pilot, issued = origin - 60000) {
    return jwt.sign({ id, role: 'ADMIN', iat: issued / 1000 }, jwtSecret, { expiresIn: '4h', jwtid: randomUUID() });
  }

  async function call(worker, path, token, body, session) {
    const response = await fetch(`http://127.0.0.1:${worker.port}/api/admin${path}`, {
      method: body ? 'POST' : 'GET',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json',
        ...(session ? { 'X-Privileged-Session': session } : {}) },
      ...(body ? { body: JSON.stringify(body) } : {}), ...deadline()
    });
    return { status: response.status, body: await response.json() };
  }

  const audit = async target => (await target.pool.query('SELECT * FROM admin_audit_log ORDER BY created_at, id')).rows;
  const event = (action, outcome, reason, requestId = null) => ({
    actor_id: pilot, resource_type: 'administrator', resource_id: pilot,
    action, outcome, reason, request_id: requestId
  });
  const stepFailure = event('ADMIN_STEP_UP_FAILURE', 'DENIED', 'AUTH_FAILURE');
  const stepSuccess = event('ADMIN_STEP_UP_SUCCESS', 'SUCCESS', 'AUTH_SUCCESS');
  function eventFields({ id, created_at, ...fields }) { return fields; }

  const savedAdminKey = process.env.PHASE_B_ADMIN_KEY;
  process.env.PHASE_B_ADMIN_KEY = key;
  try {
    const { rows: [instance] } = await admin.query(`
      SELECT current_setting('data_directory') AS directory, current_database() AS database,
             host(inet_server_addr()) AS address, current_setting('server_version') AS version,
             current_setting('default_transaction_isolation') AS isolation`);
    check([instance.directory, instance.database, instance.address], [process.env.ADM01_CLUSTER, 'postgres', '127.0.0.1'],
      'database must belong to the owned runner');
    console.log(`PostgreSQL ${instance.version}, ${instance.isolation}, owned loopback cluster`);

    const main = await fixture();
    const separate = await fixture();
    const { pool, options, env } = main;
    const beforeBootstrap = await audit(main);
    const seedAdmin = account => bootstrap(pool, { ...options, account }, env);
    let secret;
    await group('G3 bootstrap rejection and singleton', async () => {
      await rejects(() => seedAdmin('ordinary_synthetic'), /Synthetic pilot ID/);
      await rejects(() => seedAdmin('synthetic_pilot_inactive'), /must already exist and be active/);
      await rejects(() => seedAdmin('synthetic_pilot_nonexistent'), /must already exist and be active/);
      check(await audit(main), beforeBootstrap, 'rejected bootstrap must not record success');
      check((await pool.query('SELECT * FROM pilot_bootstrap')).rowCount, 0);
      secret = (await seedAdmin(pilot)).totp_secret;
      check((await audit(main)).map(eventFields), [event('SYNTHETIC_ADMIN_BOOTSTRAP', 'SUCCESS', 'OWNER_APPROVED', approval)]);
      const afterBootstrap = await audit(main);
      await rejects(() => bootstrap(pool, { ...options, account: otherPilot, approval: 'ADM01_OTHER' }, env), /already bootstrapped/);
      check(await audit(main), afterBootstrap);
      check((await pool.query('SELECT user_id FROM pilot_bootstrap')).rows, [{ user_id: pilot }]);
    });
    const separateSecret = (await bootstrap(separate.pool, { ...separate.options, account: otherPilot }, separate.env)).totp_secret;
    const first = await launch(main);
    const second = await launch(main);
    const third = await launch(separate);
    check(new Set([first, second, third].map(w => w.child.pid)).size, 3);
    check(new Set([first, second, third].map(w => w.port)).size, 3);
    const token = bearer();
    const step = (code, worker = first, candidatePin = pin) => call(worker, '/security/step-up', token, { pin: candidatePin, code });
    const read = (worker, session, candidateBearer = token) => call(worker, '/audit', candidateBearer, undefined, session);
    const counter = time => Math.floor(time / stepMs);
    const codeAt = time => totpAt(secret, counter(time));
    const result = ({ status, body }) => [status, body.code];
    const wrongCode = time => {
      const window = [-1, 0, 1].map(delta => codeAt(time + delta * stepMs));
      return ['000000', '000001', '000002', '000003'].find(code => !window.includes(code));
    };
    async function open(time, worker = first) {
      const response = await step(codeAt(time), worker);
      check(response.status, 200, 'step-up creates a session');
      check(response.body.server_time, time);
      check(response.body.expires_at, time + 600000);
      return response.body.token;
    }

    await group('G1 TOTP and exact rolling lockout boundary', async () => {
      let time = origin;
      check(result(await step(wrongCode(time))), [401, 'STEP_UP_FAILED'], 'wrong well-formed code');
      // Advance only by whole TOTP steps if a six-digit code happens to collide.
      while (new Set([-2, -1, 0, 1, 2].map(delta => codeAt(time + delta * stepMs))).size !== 5) time += stepMs;
      await setClock(time, first, second);
      check(result(await step(codeAt(time - 2 * stepMs))), [401, 'STEP_UP_FAILED'], 'one step below accepted window');
      check(result(await step(codeAt(time + 2 * stepMs))), [401, 'STEP_UP_FAILED'], 'one step above accepted window');
      check((await step(codeAt(time - stepMs))).status, 200, 'earliest accepted step');
      check(result(await step(codeAt(time - stepMs), second)), [401, 'STEP_UP_FAILED'], 'used accepted counter is rejected across workers');
      check((await step(codeAt(time + stepMs), second)).status, 200, 'latest accepted step');
      check((await audit(main)).slice(1).map(eventFields), [
        stepFailure, stepFailure, stepFailure, stepSuccess, stepFailure, stepSuccess
      ], 'all step-up results record the exact administrator audit fields');
      check(result(await call(second, '/security/step-up', bearer(otherPilot),
        { pin, code: codeAt(time) })), [401, 'STEP_UP_FAILED']);
      time += 4 * stepMs;
      const historical = await audit(main);
      await setClock(time, first, second);
      for (let attempt = 0; attempt < 5; attempt++) {
        check(result(await step(codeAt(time), attempt % 2 ? second : first, '000000')), [401, 'STEP_UP_FAILED'], 'first five failures are counted');
      }
      check(result(await step(codeAt(time), second)), [429, 'STEP_UP_RATE_LIMITED'], 'sixth attempt is locked out even with correct credentials');
      const lockedAudit = await audit(main);
      check(lockedAudit.slice(historical.length).map(eventFields), Array(5).fill(stepFailure), 'lockout consumes no extra attempt or success event');
      check(lockedAudit.slice(historical.length).map(row => Number(row.created_at)), [0, 1, 2, 3, 4].map(offset => time + offset));
      await setClock(time + lockoutMs - 1, first, second);
      check(result(await step(codeAt(time + lockoutMs - 1), first)), [429, 'STEP_UP_RATE_LIMITED'], 'locked one millisecond before release');
      check(await audit(main), lockedAudit);
      await setClock(time + lockoutMs, first, second);
      await open(time + lockoutMs, second);
      const unlockedAudit = await audit(main);
      check(unlockedAudit.slice(0, -1), lockedAudit, 'successful step-up preserves complete historical rows');
      check(eventFields(unlockedAudit.at(-1)), stepSuccess);
      await setClock(time + lockoutMs + stepMs, first, second);
      for (let attempt = 0; attempt < 5; attempt++) {
        check(result(await step(wrongCode(time + lockoutMs + stepMs))), [401, 'STEP_UP_FAILED'], 'new failures count after successful reset');
      }
      check(result(await step(codeAt(time + lockoutMs + stepMs))), [429, 'STEP_UP_RATE_LIMITED']);
    });

    // Move beyond the intentionally exhausted window before testing independent controls.
    const bindingTime = origin + 60 * 60000;
    await setClock(bindingTime, first, second, third);
    let compromiseSession;
    let ordinaryRevocationId;
    await group('G2 bearer, pilot context, expiry, version and current authority', async () => {
      const session = await open(bindingTime);
      check((await read(second, session)).status, 200, 'baseline accepts on another process');
      const alternate = bearer();
      check(result(await read(second, session, alternate)), [401, 'STEP_UP_REQUIRED'], 'different valid bearer for same pilot');
      const alternateSession = await call(second, '/security/step-up', alternate, { pin, code: codeAt(bindingTime + stepMs) });
      check(alternateSession.status, 200, 'alternate bearer can obtain its own session');
      check((await read(first, alternateSession.body.token, alternate)).status, 200);
      const otherBearer = bearer(otherPilot);
      const otherStep = await call(third, '/security/step-up', otherBearer,
        { pin, code: totpAt(separateSecret, counter(bindingTime)) });
      check(otherStep.status, 200, 'other database has its own functioning pilot context');
      check((await read(third, otherStep.body.token, otherBearer)).status, 200);
      check(result(await read(third, session, otherBearer)), [401, 'STEP_UP_REQUIRED'], 'different database and current pilot');
      check(result(await read(third, session)), [403, 'ADMIN_FORBIDDEN'], 'original pilot is not the current pilot in the other database');
      check(result(await read(second, session, bearer(otherPilot))), [403, 'ADMIN_FORBIDDEN'], 'other account with stale ADMIN claim cannot borrow session');

      await pool.query('UPDATE admin_security SET security_version=security_version+1 WHERE user_id=$1', [pilot]);
      const { rows: [state] } = await pool.query(
        `SELECT p.revoked_at, p.security_version AS session_version, s.security_version AS current_version
         FROM privileged_sessions p JOIN admin_security s USING (user_id) WHERE p.token_hash=$1`, [hash(session)]);
      check(state, { revoked_at: null, session_version: 0, current_version: 1 });
      check(result(await read(second, session)), [401, 'STEP_UP_REQUIRED'], 'version mismatch alone invalidates the unrevoked session');

      const next = bindingTime + 2 * stepMs;
      await setClock(next, first, second);
      const expiring = await open(next);
      await setClock(next + 600000 - 1, first, second);
      check((await read(second, expiring)).status, 200, 'valid one millisecond before expiration');
      await setClock(next + 600000, first, second);
      check(result(await read(second, expiring)), [401, 'STEP_UP_REQUIRED'], 'denied at exact expiration');
      const current = await open(next + 600000);
      await pool.query("UPDATE permissions SET active=FALSE WHERE code='admin.audit.read'");
      check(result(await read(second, current)), [403, 'PERMISSION_DENIED'], 'current permission overrides valid session and JWT claim');
      await pool.query("UPDATE permissions SET active=TRUE WHERE code='admin.audit.read'");
      await pool.query('UPDATE users SET is_active=FALSE WHERE id=$1', [pilot]);
      check((await read(second, current)).status, 401, 'current account activity is rechecked');
      await pool.query('UPDATE users SET is_active=TRUE WHERE id=$1', [pilot]);
      check((await read(second, current)).status, 200, 'restored permission and account make the same session valid');

      const ref = randomUUID();
      ordinaryRevocationId = ref;
      const prior = await audit(main);
      check((await call(first, '/security/revoke-sessions', token, { request_id: ref, reason: 'ADM01_REVOKE' }, current)).status, 200);
      check(result(await read(second, current)), [401, 'STEP_UP_REQUIRED'], 'revocation is enforced across processes');
      check((await pool.query('SELECT revoked_at IS NOT NULL AS revoked FROM privileged_sessions WHERE token_hash=$1',
        [hash(current)])).rows, [{ revoked: true }]);
      check((await audit(main)).filter(row => !prior.some(previous => previous.id === row.id)).map(eventFields),
        [event('ADMIN_SESSION_REVOKE', 'SUCCESS', 'ADM01_REVOKE', ref)]);
      const after = next + 600000 + stepMs;
      await setClock(after, first, second);
      compromiseSession = await open(after);
    });

    const containmentTime = bindingTime + 3 * stepMs + 600000 + 1;
    const recoveryRef = 'ADM01_RECOVERY';
    const recoveryPin = '927631';
    const recoveryOptions = { ...options, approval: recoveryRef, newPin: recoveryPin };
    const containmentRequest = { request_id: randomUUID(), reason: 'ADM01_CONTAINMENT', compromised: true };
    let recoveredBearer, recoveredSession, recoveredAtCutoff;
    await group('G4 compromise containment audit and append-only integrity', async () => {
      await setClock(containmentTime, first, second);
      const before = await audit(main);
      check((await call(first, '/security/revoke-sessions', token,
        containmentRequest, compromiseSession)).status, 200);
      check((await read(second, compromiseSession)).status, 401, 'contained account denied by the other worker');
      const after = await audit(main);
      check(after.slice(0, -1), before, 'containment leaves previous rows intact');
      check(eventFields(after.at(-1)), event('ADMIN_COMPROMISE_CONTAINMENT', 'SUCCESS',
        containmentRequest.reason, containmentRequest.request_id));
      check(Number(after.at(-1).created_at), containmentTime);
      check((await pool.query('SELECT is_active FROM users WHERE id=$1', [pilot])).rows, [{ is_active: false }]);
      check((await pool.query(
        'SELECT revoked_at::bigint AS revoked_at FROM privileged_sessions WHERE token_hash=$1', [hash(compromiseSession)]
      )).rows, [{ revoked_at: String(containmentTime) }]);
      check((await call(first, '/security/revoke-sessions', token,
        containmentRequest, compromiseSession)).status, 401);
      check(await audit(main), after, 'failed containment replay adds no success event');
      await rejects(() => pool.query('UPDATE admin_audit_log SET reason=$1 WHERE id=$2', ['ALTERED', after[0].id]), /append-only/);
      await rejects(() => pool.query('DELETE FROM admin_audit_log WHERE id=$1', [after[0].id]), /append-only/);
      await rejects(() => pool.query('TRUNCATE admin_audit_log'), /append-only/);
      check(await audit(main), after);
    });

    await group('G3/G4 recovery validation, audit and new credentials', async () => {
      const before = await audit(main);
      const recoverWith = override => recover(pool, { ...recoveryOptions, ...override }, env);
      await rejects(() => recoverWith({ account: otherPilot }), /only the existing synthetic pilot/);
      await rejects(() => recoverWith({ approval }), /fresh approval reference/);
      await rejects(() => recoverWith({ approval: 'invalid reference' }), /approval reference/);
      await rejects(() => recoverWith({ approval: '' }), /approval reference/);
      check(await audit(main), before, 'rejected recoveries record no success');
      check((await pool.query('SELECT is_active FROM users WHERE id=$1', [pilot])).rows, [{ is_active: false }]);
      const clock = Date.now;
      const recoveredAt = containmentTime + stepMs - 1;
      let recovery;
      try {
        Date.now = () => recoveredAt;
        recovery = await recoverWith({});
        await rejects(() => recoverWith({}), /reference has already been used/);
        await rejects(() => recoverWith({ account: otherPilot }), /only the existing synthetic pilot/);
        await rejects(() => recoverWith({ approval }), /fresh approval reference/);
      } finally {
        Date.now = clock;
      }
      const after = await audit(main);
      check(after.slice(0, -1), before);
      check(eventFields(after.at(-1)), event('SYNTHETIC_ADMIN_RECOVERY', 'SUCCESS', 'OWNER_APPROVED', recoveryRef));
      check(Number(after.at(-1).created_at), recoveredAt);
      check(recovery.auth_not_before, recoveredAt + 1000);
      await setClock(recovery.auth_not_before, first, second);
      check((await read(second, compromiseSession)).status, 401, 'old bearer remains denied after recovery');
      const fresh = bearer(pilot, recovery.auth_not_before);
      const freshCode = totpAt(recovery.totp_secret, counter(recovery.auth_not_before));
      check(result(await call(first, '/security/step-up', fresh, { pin, code: freshCode })), [401, 'STEP_UP_FAILED']);
      const response = await call(second, '/security/step-up', fresh, { pin: recoveryPin, code: freshCode });
      check(response.status, 200);
      const recoveredAudit = await audit(main);
      check(recoveredAudit.slice(0, -2), after);
      check(recoveredAudit.slice(-2).map(eventFields), [stepFailure, stepSuccess]);
      check(recoveredAudit.slice(-2).map(row => Number(row.created_at)), [recovery.auth_not_before, recovery.auth_not_before + 1]);
      check((await read(first, response.body.token, fresh)).status, 200);
      recoveredBearer = fresh;
      recoveredSession = response.body.token;
      recoveredAtCutoff = recovery.auth_not_before;
    });

    await group('G4 duplicate containment after recovery', async () => {
      const outcome = await call(second, '/security/revoke-sessions', recoveredBearer, containmentRequest, recoveredSession);
      const { rows: [state] } = await pool.query(
        `SELECT u.is_active, (SELECT count(*)::int FROM admin_audit_log
           WHERE action='ADMIN_COMPROMISE_CONTAINMENT' AND request_id=$2) AS containment_events
         FROM users u WHERE u.id=$1`, [pilot, containmentRequest.request_id]);
      check({ response: result(outcome), ...state },
        { response: [409, 'REQUEST_ALREADY_PROCESSED'], is_active: true, containment_events: 1 },
        'a consumed containment request must not deactivate the recovered pilot or create a duplicate audit event');
      check((await read(second, recoveredSession, recoveredBearer)).status, 200, 'replay preserves fresh bearer and session');
      check(result(await call(first, '/security/revoke-sessions', recoveredBearer,
        { ...containmentRequest, compromised: false }, recoveredSession)), [409, 'REQUEST_ALREADY_PROCESSED']);
      check(result(await call(first, '/security/revoke-sessions', recoveredBearer,
        { ...containmentRequest, request_id: ordinaryRevocationId }, recoveredSession)), [409, 'REQUEST_ALREADY_PROCESSED']);
    });

    let restarted;
    await group('G4 replay after restarting an Express worker', async () => {
      await stopWorker(second.child);
      restarted = await launch(main);
      check(restarted.child.pid !== second.child.pid, true);
      await setClock(recoveredAtCutoff, restarted);
      const before = await audit(main);
      check(result(await call(restarted, '/security/revoke-sessions', recoveredBearer,
        { ...containmentRequest, request_id: containmentRequest.request_id.toUpperCase() }, recoveredSession)),
      [409, 'REQUEST_ALREADY_PROCESSED'], 'restart and UUID casing cannot bypass persisted replay protection');
      check(await audit(main), before, 'restart replay did not add an audit event');
      check((await read(restarted, recoveredSession, recoveredBearer)).status, 200);
    });

    await group('G4 a new containment ID succeeds exactly once under concurrent requests', async () => {
      const request = { ...containmentRequest, request_id: randomUUID() };
      const blocker = await pool.connect();
      let pending;
      let waiting = 0;
      try {
        await blocker.query('BEGIN');
        await blocker.query('SELECT user_id FROM admin_security WHERE user_id=$1 FOR UPDATE', [pilot]);
        pending = Promise.allSettled([first, restarted].map(worker =>
          call(worker, '/security/revoke-sessions', recoveredBearer, request, recoveredSession)));
        const limit = Date.now() + 5000;
        do {
          const { rows: [locks] } = await pool.query(
            `SELECT count(*)::int AS waiting FROM pg_stat_activity
             WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE '%FOR UPDATE OF pb,s,u%'`);
          waiting = locks.waiting;
          if (waiting === 2) break;
          await wait(15);
        } while (Date.now() < limit);
      } finally {
        try { await blocker.query('ROLLBACK'); } finally { blocker.release(); }
      }
      const responses = (await pending).map(outcome => {
        if (outcome.status === 'rejected') throw outcome.reason;
        return outcome.value.status;
      });
      check(waiting, 2, 'both processes reached the shared PostgreSQL row lock');
      check(responses.sort((a, b) => a - b), [200, 403], 'losing request rechecks authority after the winner deactivates the pilot');
      check((await pool.query('SELECT is_active FROM users WHERE id=$1', [pilot])).rows, [{ is_active: false }]);
      check((await audit(main)).filter(row => row.request_id === request.request_id).map(eventFields),
        [event('ADMIN_COMPROMISE_CONTAINMENT', 'SUCCESS', request.reason, request.request_id)]);
    });
  } finally {
    if (savedAdminKey === undefined) delete process.env.PHASE_B_ADMIN_KEY;
    else process.env.PHASE_B_ADMIN_KEY = savedAdminKey;

    const cleanup = await Promise.allSettled([...workers].map(stopWorker));
    for (const { name, pool } of fixtures) {
      cleanup.push(...await Promise.allSettled([pool.end()]));
      cleanup.push(...await Promise.allSettled([admin.query(`DROP DATABASE "${name}"`)]));
    }
    cleanup.push(...await Promise.allSettled([admin.end()]));
    const errors = cleanup.filter(outcome => outcome.status === 'rejected').map(outcome => outcome.reason);
    if (errors.length) throw new AggregateError(errors, 'ADM-01 cleanup failed');
    console.log(`Cleaned up ${stoppedWorkers} workers and ${fixtures.length} disposable databases`);
    console.log(`ADM-01 progress: ${passed} groups passed, ${assertions} completed assertions`);
  }
}

(process.argv[2] === workerFlag ? serve() : run()).catch(error => {
  console.error(error);
  process.exitCode = 1;
  if (process.connected) process.disconnect();
});
