'use strict';

const assert = require('node:assert/strict');
const { fork } = require('node:child_process');
const { once } = require('node:events');
const { randomBytes, randomUUID } = require('node:crypto');
const { setTimeout: wait } = require('node:timers/promises');
const { Pool } = require('pg');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { migrate } = require('../src/migrations');
const { hash } = require('../src/phase-b/security');
const { totpAt } = require('../src/phase-b/totp');

const bounded = () => ({ signal: AbortSignal.timeout(15000) });
const pin = '853692';
const wrongPin = '000000';
const actions = ['ADMIN_STEP_UP_FAILURE', 'ADMIN_STEP_UP_SUCCESS'];

function localUrl(value, pathPattern) {
  const url = new URL(value || '');
  assert(['postgres:', 'postgresql:'].includes(url.protocol));
  assert.equal(url.hostname, '127.0.0.1');
  assert(url.port && url.port !== '5433' && !url.search);
  assert.match(url.pathname, pathPattern);
  return url;
}

async function worker() {
  assert(process.send && process.env.NODE_ENV === 'test' && process.env.PHASE_B_SYNTHETIC === 'true');
  localUrl(process.env.DATABASE_URL, /^\/perazim_phaseb_[a-f0-9]{16}_test$/);
  const db = require('../src/db');
  await db.initDb();
  const server = require('../src/index').app.listen(0, '127.0.0.1');
  await once(server, 'listening', bounded());
  process.send(server.address().port);
  const realNow = Date.now;
  process.on('message', async offset => {
    if (offset === 'stop') {
      const closed = once(server, 'close', bounded());
      server.close();
      await closed;
      await db.closeDb();
      process.disconnect();
    } else {
      Date.now = () => realNow() + offset;
      process.send('clock-ready');
    }
  });
}

async function run() {
  assert.equal(process.env.NODE_ENV, 'test');
  const url = localUrl(process.env.PHASE_B_TEST_ADMIN_URL, /^\/postgres$/);
  const dbName = `perazim_phaseb_${randomBytes(8).toString('hex')}_test`;
  const admin = new Pool({ connectionString: url.toString(), connectionTimeoutMillis: 5000, statement_timeout: 15000 });
  const workers = [];
  const failures = [];
  const pilot = `synthetic_pilot_${randomBytes(6).toString('hex')}`;
  let pool, created = false, offset = 0, secret;
  let assertions = 0, cases = 0;
  const check = (actual, expected, message) => {
    assert.deepEqual(actual, expected, message);
    assertions++;
  };
  const pass = label => { cases++; console.log('PASS ' + label); };
  const activeCounter = () => Math.floor((Date.now() + offset) / 30000);
  const currentCode = () => totpAt(secret, activeCounter());
  const pastCode = () => {
    const current = [-1, 0, 1].map(delta => totpAt(secret, activeCounter() + delta));
    for (let delta = 4; ; delta++) {
      const old = totpAt(secret, activeCounter() - delta);
      if (!current.includes(old)) return old;
    }
  };

  async function launch() {
    const child = fork(__filename, ['--worker'], { stdio: ['ignore', 'ignore', 'inherit', 'ipc'] });
    workers.push(child);
    const [port] = await once(child, 'message', bounded());
    assert(Number.isInteger(port));
    return port;
  }

  async function advanceWindow() {
    offset += 16 * 60000;
    await Promise.all(workers.map(async child => {
      const ack = once(child, 'message', bounded());
      child.send(offset);
      check((await ack)[0], 'clock-ready');
    }));
  }

  async function call(port, path, token, body, privileged) {
    const response = await fetch(`http://127.0.0.1:${port}/api/admin${path}`, {
      method: body ? 'POST' : 'GET',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json',
        ...(privileged ? { 'X-Privileged-Session': privileged } : {}) },
      ...(body ? { body: JSON.stringify(body) } : {}), ...bounded()
    });
    return { status: response.status, body: await response.json() };
  }

  function denied(response, locked = false) {
    check(response, locked
      ? { status: 429, body: { error: 'Too many administrator authentication attempts', code: 'STEP_UP_RATE_LIMITED' } }
      : { status: 401, body: { error: 'Administrator authentication failed', code: 'STEP_UP_FAILED' } });
  }

  async function events() {
    const { rows: [row] } = await pool.query(
      `SELECT count(*) FILTER (WHERE action=$2)::int AS failures,
              count(*) FILTER (WHERE action=$3)::int AS successes
       FROM admin_audit_log WHERE actor_id=$1`, [pilot, ...actions]);
    return row;
  }

  async function securityState() {
    const { rows: [row] } = await pool.query(
      `SELECT security_version, totp_last_counter, auth_not_before,
         (SELECT count(*)::int FROM privileged_sessions WHERE user_id=$1) AS sessions
       FROM admin_security WHERE user_id=$1`, [pilot]);
    return row;
  }

  // Hold the shared pilot lock until every request reaches it. This makes the
  // old check-before-lock race deterministic without changing production code.
  async function blockedBatch(groups) {
    const blocker = await pool.connect();
    const pending = [];
    const start = group => {
      for (const action of group) {
        const promise = action();
        pending.push(promise);
        promise.catch(() => {}); // Drained below, even if the lock barrier fails.
      }
    };
    let error;
    try {
      await blocker.query('BEGIN');
      await blocker.query('SELECT user_id FROM admin_security WHERE user_id=$1 FOR UPDATE', [pilot]);
      for (const group of groups) {
        start(group);
        const deadline = Date.now() + 8000;
        let waiting;
        do {
          const { rows: [row] } = await pool.query(
            `SELECT count(*)::int AS n FROM pg_stat_activity WHERE datname=current_database()
             AND wait_event_type='Lock' AND query LIKE '%FOR UPDATE OF pb,u,s%'`);
          waiting = row.n;
          if (waiting === pending.length) break;
          await wait(15);
        } while (Date.now() < deadline);
        check(waiting, pending.length, 'Each request must reach the same PostgreSQL row lock');
      }
    } catch (caught) {
      error = caught;
    } finally {
      try { await blocker.query('ROLLBACK'); } finally { blocker.release(); }
    }
    const results = await Promise.allSettled(pending);
    if (error) throw error;
    return results.map(result => {
      if (result.status === 'rejected') throw result.reason;
      return result.value;
    });
  }

  try {
    const { rows: [identity] } = await admin.query('SELECT current_database() AS name, host(inet_server_addr()) AS address');
    check(identity, { name: 'postgres', address: '127.0.0.1' });
    await admin.query(`CREATE DATABASE "${dbName}"`);
    created = true;
    url.pathname = '/' + dbName;
    Object.assign(process.env, { DATABASE_URL: url.toString(), PHASE_B_SYNTHETIC: 'true',
      JWT_SECRET: randomBytes(48).toString('hex'), PHASE_B_ADMIN_KEY: randomBytes(32).toString('hex') });
    pool = new Pool({ connectionString: url.toString(), connectionTimeoutMillis: 5000, statement_timeout: 15000 });
    await migrate(pool);
    const ordinary = 'ordinary_synthetic';
    const pinHash = await bcrypt.hash(pin, 4);
    for (const [i, id] of [pilot, ordinary].entries()) {
      await pool.query(`INSERT INTO users(id,phone,pin_hash,name,role,is_active,created_at,updated_at)
                        VALUES($1,$2,$3,'Synthetic SEC-02','MEMBER',true,0,0)`, [id, `+25470000888${i}`, pinHash]);
    }
    const enrollment = await require('../scripts/bootstrap-synthetic-admin').bootstrap(pool,
      { db: dbName, confirm: dbName, account: pilot, approval: 'SYNTHETIC_SEC02_TEST' });
    secret = enrollment.totp_secret;
    const token = jwt.sign({ id: pilot }, process.env.JWT_SECRET, { expiresIn: '4h' });
    const ordinaryToken = jwt.sign({ id: ordinary }, process.env.JWT_SECRET, { expiresIn: '4h' });
    const ports = await Promise.all([launch(), launch()]);
    check(new Set(ports).size, 2);
    check(new Set(workers.map(child => child.pid)).size, 2);
    const step = (port, candidatePin, code, bearer = token) =>
      call(port, '/security/step-up', bearer, { pin: candidatePin, code });
    const success = async port => {
      const response = await step(port, pin, currentCode());
      check(response.status, 200, JSON.stringify(response.body));
      check(response.body.expires_at - response.body.server_time, 600000);
      return response.body.token;
    };

    // Run the deterministic regression first so an unfixed route reproduces SEC-02.
    for (let round = 0; round < 2; round++) {
      const before = await events();
      const state = await securityState();
      const results = await blockedBatch([Array.from({ length: 12 }, (_, i) =>
        () => step(ports[i % 2], wrongPin, currentCode()))]);
      check(results.filter(r => r.status === 401).length, 5, 'Concurrent failures must spend exactly five attempts');
      for (const response of results) denied(response, response.status === 429);
      check(await events(), { failures: before.failures + 5, successes: before.successes });
      check(await securityState(), state);
      denied(await step(ports[1], pin, currentCode()), true);
      await advanceWindow();
      await success(ports[0]);
    }
    pass('two 12-request bursts across independent Node processes share five attempts each');

    await advanceWindow();
    const beforeReset = await events();
    for (let i = 0; i < 2; i++) denied(await step(ports[i], wrongPin, currentCode()));
    const priorSession = await success(ports[0]);
    for (let i = 0; i < 5; i++) denied(await step(ports[i % 2], pin, i === 0 ? 'invalid' : pastCode()));
    denied(await step(ports[1], pin, currentCode()), true);
    check(await events(), { failures: beforeReset.failures + 7, successes: beforeReset.successes + 1 });
    await advanceWindow();
    const validSession = await success(ports[1]);
    check((await call(ports[0], '/audit', token, null, priorSession)).status, 401);
    check((await call(ports[0], '/audit', token, null, validSession)).status, 200);
    pass('sequential wrong PIN, invalid and out-of-window TOTP, reset on success, expiry and session lifetime');

    const beforeMixed = await events();
    for (let i = 0; i < 4; i++) denied(await step(ports[i % 2], wrongPin, currentCode()));
    const mixed = await blockedBatch([
      [() => step(ports[0], wrongPin, currentCode())],
      [() => step(ports[1], pin, currentCode()), () => step(ports[1], wrongPin, currentCode())]
    ]);
    denied(mixed[0]);
    denied(mixed[1], true);
    denied(mixed[2], true);
    check(await events(), { failures: beforeMixed.failures + 5, successes: beforeMixed.successes });
    pass('valid credentials queued behind the threshold failure cannot clear lockout');

    await advanceWindow();
    const beforeSuccessFirst = await events();
    const oldState = await securityState();
    const successFirst = await blockedBatch([
      [() => step(ports[0], pin, currentCode())],
      [() => step(ports[1], wrongPin, currentCode()), () => step(ports[0], wrongPin, currentCode())]
    ]);
    check(successFirst[0].status, 200);
    denied(successFirst[1]);
    denied(successFirst[2]);
    check(await events(), { failures: beforeSuccessFirst.failures + 2, successes: beforeSuccessFirst.successes + 1 });
    const newState = await securityState();
    check(newState.security_version, oldState.security_version);
    check(newState.auth_not_before, oldState.auth_not_before);
    check(newState.sessions, oldState.sessions + 1);
    check(Number(newState.totp_last_counter) > Number(oldState.totp_last_counter), true);
    const session = successFirst[0].body.token;
    check((await call(ports[1], '/audit', token, null, session)).status, 200);
    denied(await step(ports[1], pin, currentCode(), ordinaryToken));
    check((await call(ports[1], '/audit', ordinaryToken, null, session)).status, 403);
    check((await call(ports[0], '/security/revoke-sessions', token,
      { request_id: randomUUID(), reason: 'SYNTHETIC_TEST', compromised: false }, session)).status, 200);
    check((await call(ports[1], '/audit', token, null, session)).status, 401);
    check((await securityState()).security_version, newState.security_version + 1);
    pass('success-first concurrency preserves newer security state, pilot authority and session revocation');
  } catch (error) {
    failures.push(error);
  } finally {
    const stopped = await Promise.allSettled(workers.map(async child => {
      if (child.exitCode !== null || child.signalCode !== null) throw new Error('Worker exited before cleanup');
      try {
        const done = once(child, 'exit', bounded());
        child.send('stop');
        check(await done, [0, null]);
      } catch (error) {
        if (child.exitCode === null && child.signalCode === null) {
          const done = once(child, 'exit', bounded());
          child.kill('SIGKILL');
          await done;
        }
        throw error;
      }
    }));
    for (const result of stopped) if (result.status === 'rejected') failures.push(result.reason);
    for (const close of [async () => pool?.end(), async () => {
      if (created) await admin.query(`DROP DATABASE "${dbName}"`);
    }, () => admin.end()]) {
      try { await close(); } catch (error) { failures.push(error); }
    }
  }
  if (failures.length) throw new AggregateError(failures, 'SEC-02 check or cleanup failed');
  console.log(`PASS SEC-02: ${cases} scenarios, ${assertions} assertions, two independent workers, shared database dropped`);
}

(process.argv[2] === '--worker' ? worker() : run()).catch(error => {
  console.error(error);
  process.exitCode = 1;
  if (process.connected) process.disconnect();
});
