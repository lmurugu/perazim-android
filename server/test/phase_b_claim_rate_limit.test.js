'use strict';

const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { fork } = require('node:child_process');
const { once } = require('node:events');
const { Pool, Client } = require('pg');
const jwt = require('jsonwebtoken');
const { migrate } = require('../src/migrations');
const { hash } = require('../src/phase-b/security');

const message = 'Too many membership claim attempts. Please wait before trying again.';
const receipt = 'Your request has been received for review.';
const timeout = () => ({ signal: AbortSignal.timeout(10000) });

async function runWorker() {
  assert(process.send && process.env.NODE_ENV === 'test' && process.env.PHASE_B_SYNTHETIC === 'true');
  const url = new URL(process.env.DATABASE_URL || '');
  assert(['postgres:', 'postgresql:'].includes(url.protocol));
  assert.equal(url.hostname, '127.0.0.1');
  assert.equal(url.search, '');
  assert(url.port && url.port !== '5433');
  assert.match(url.pathname, /^\/perazim_phaseb_[a-f0-9]{16}_test$/);
  const db = require('../src/db');
  let claimQueries = 0;
  const query = Client.prototype.query;
  Client.prototype.query = function (config, ...args) {
    if (/\b(membership_claims|membership_verifications|membership_records|user_permissions)\b/.test(config?.text || config)) claimQueries++;
    const result = query.call(this, config, ...args);
    return result?.catch ? result.catch(error => {
      console.error('Synthetic worker query failed:', error.code, error.message);
      throw error;
    }) : result;
  };

  await db.initDb();
  const { app } = require('../src/index');
  const server = app.listen(0, '127.0.0.1');
  await once(server, 'listening', timeout());
  process.send({ port: server.address().port });
  process.on('message', async command => {
    if (command === 'inspect') process.send({ claimQueries });
    if (command === 'stop') {
      try {
        const closed = once(server, 'close', timeout());
        server.close();
        await closed;
        await db.closeDb();
        process.disconnect();
      } catch (error) {
        console.error(error);
        process.exit(1);
      }
    }
  });
}

function check429(response, maxSeconds) {
  assert.equal(response.status, 429, JSON.stringify(response.body));
  assert.equal(response.cache, 'no-store');
  assert.match(response.retry, /^[1-9]\d*$/);
  const seconds = Number(response.retry);
  assert(seconds <= maxSeconds, 'Retry-After must be within the applicable window');
  assert.deepEqual(response.body, { error: message, code: 'RATE_LIMITED', retry_after_seconds: seconds });
}

function check202(response) {
  assert.equal(response.status, 202, JSON.stringify(response.body));
  assert.deepEqual(response.body, { received: true, request_id: response.requestId, message: receipt });
}

async function run() {
  assert.notEqual(process.env.NODE_ENV, 'production');
  const adminUrl = new URL(process.env.PHASE_B_TEST_ADMIN_URL || '');
  assert(['postgres:', 'postgresql:'].includes(adminUrl.protocol));
  assert.equal(adminUrl.hostname, '127.0.0.1');
  assert.equal(adminUrl.pathname, '/postgres');
  assert.equal(adminUrl.search, '');
  assert(adminUrl.port && adminUrl.port !== '5433', 'Use the owned isolated runner, never the app database port');
  const suffix = crypto.randomBytes(8).toString('hex');
  const database = `perazim_phaseb_${suffix}_test`;
  const admin = new Pool({ connectionString: adminUrl.toString(), connectionTimeoutMillis: 5000, statement_timeout: 10000 });
  const workers = new Set();
  let created = false;
  let pool;
  const jwtSecret = crypto.randomBytes(48).toString('hex');
  const databaseUrl = new URL(adminUrl);
  databaseUrl.pathname = '/' + database;

  async function startWorker(trustProxy) {
    const child = fork(__filename, ['--worker'], {
      env: { ...process.env, NODE_ENV: 'test', PHASE_B_SYNTHETIC: 'true', DATABASE_URL: databaseUrl.toString(),
        TRUST_PROXY_HOPS: trustProxy ? '1' : '', JWT_SECRET: jwtSecret },
      stdio: ['ignore', 'ignore', 'inherit', 'ipc']
    });
    workers.add(child);
    const [ready] = await once(child, 'message', timeout());
    assert(Number.isInteger(ready.port));
    return { child, port: ready.port };
  }

  async function stopWorker(child) {
    try {
      assert(child.connected, 'Worker exited before orderly shutdown');
      const exited = once(child, 'exit', timeout());
      child.send('stop');
      const [code, signal] = await exited;
      assert.deepEqual([code, signal], [0, null]);
    } catch (error) {
      if (child.exitCode === null && child.signalCode === null) {
        const exited = once(child, 'exit', timeout());
        child.kill('SIGKILL');
        await exited;
      }
      throw error;
    } finally {
      workers.delete(child);
    }
  }

  async function attempt(worker, account, ip, variation = 0, requestId = crypto.randomUUID(), forgedPrefix) {
    const forwarded = forgedPrefix ? `${forgedPrefix}, ${ip}` : ip;
    const response = await fetch(`http://127.0.0.1:${worker.port}/api/membership/claims`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${account.token}`, 'X-Forwarded-For': forwarded },
      body: JSON.stringify({ request_id: requestId, name: `Synthetic Claim ${variation}`, phone: `+2547000${String(variation).padStart(5, '0')}`, user_id: 'caller-supplied-id' }),
      ...timeout()
    });
    return { status: response.status, retry: response.headers.get('retry-after'), cache: response.headers.get('cache-control'), body: await response.json(), requestId };
  }

  async function counts(scope, key) {
    const result = await pool.query('SELECT cardinality(attempts) AS count FROM membership_claim_rate_limits WHERE scope=$1 AND bucket_key=$2', [scope, hash(key)]);
    return result.rows[0]?.count || 0;
  }

  async function checkBoundary(scope, key, windowSeconds, send) {
    const { rows: [bucket] } = await pool.query(
      `SELECT (SELECT min(at) FROM unnest(attempts) AS t(at)) AS oldest,
              floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint AS before
       FROM membership_claim_rate_limits WHERE scope=$1 AND bucket_key=$2`, [scope, hash(key)]);
    const response = await send();
    check429(response, windowSeconds);
    const { rows: [{ now }] } = await pool.query('SELECT floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint AS now');
    const expiry = Number(bucket.oldest) + windowSeconds * 1000;
    const retry = Number(response.retry);
    assert(retry >= Math.max(1, Math.ceil((expiry - Number(now)) / 1000)));
    assert(retry <= Math.ceil((expiry - Number(bucket.before)) / 1000));
  }

  async function age(scope, key, milliseconds) {
    const result = await pool.query(
      `UPDATE membership_claim_rate_limits SET expires_at=expires_at-$3::bigint,
       attempts=ARRAY(SELECT at-$3::bigint FROM unnest(attempts) WITH ORDINALITY AS u(at,n) ORDER BY n)
       WHERE scope=$1 AND bucket_key=$2`, [scope, hash(key), milliseconds]);
    assert.equal(result.rowCount, 1);
  }

  async function inspect(worker) {
    const result = once(worker.child, 'message', timeout());
    worker.child.send('inspect');
    return (await result)[0].claimQueries;
  }

  const failures = [];
  try {
    const { rows: [identity] } = await admin.query('SELECT current_database() AS name, host(inet_server_addr()) AS address');
    assert.deepEqual(identity, { name: 'postgres', address: '127.0.0.1' });
    await admin.query(`CREATE DATABASE "${database}"`);
    created = true;
    pool = new Pool({ connectionString: databaseUrl.toString(), connectionTimeoutMillis: 5000, statement_timeout: 10000 });
    await migrate(pool);
    const accounts = [];
    for (let i = 0; i < 26; i++) {
      const id = `claimtest_${suffix}_${i}`;
      await pool.query(`INSERT INTO users (id,phone,pin_hash,name,role,is_active,created_at,updated_at)
        VALUES ($1,$2,'synthetic-only','Synthetic SEC-01','MEMBER',true,0,0)`,
      [id, `+2547100${String(i).padStart(5, '0')}`]);
      accounts.push({ id, token: jwt.sign({ id }, jwtSecret, { expiresIn: '1h' }) });
    }
    const a = await startWorker(true);
    const b = await startWorker(true);
    const firstIp = '192.0.2.1';
    const replay = crypto.randomUUID();
    for (let i = 0; i < 5; i++) check202(await attempt(a, accounts[0], firstIp, i, i < 2 ? replay : undefined));
    await checkBoundary('account', accounts[0].id, 3600, () => attempt(b, accounts[0], firstIp, 5));
    assert.equal(await counts('ip', firstIp), 5);
    const otherIp = '192.0.2.2';
    check429(await attempt(a, accounts[0], otherIp, 6), 3600);
    assert.equal(await counts('ip', otherIp), 0);
    check202(await attempt(b, accounts[1], firstIp, 7));
    assert.equal(await counts('ip', firstIp), 6);
    console.log('PASS account threshold, replay and payload variation, account-first IP isolation, exact neutral 429');

    const sharedIp = '192.0.2.3';
    for (let i = 2; i < 8; i++) {
      for (let j = 0; j < 5; j++) check202(await attempt(j % 2 ? a : b, accounts[i], sharedIp, i * 5 + j));
    }
    const c = await startWorker(true);
    assert.equal(await inspect(c), 0);
    check429(await attempt(c, accounts[0], otherIp, 99), 3600);
    assert.equal(await inspect(c), 0, 'account-rejected request ran no claim processing queries');
    await checkBoundary('ip', sharedIp, 900, () => attempt(c, accounts[8], sharedIp, 100, undefined, '198.51.100.99'));
    assert.equal(await inspect(c), 0, 'IP-rejected fresh account ran no claim, grant, verification or matching queries');
    assert.equal(await counts('account', accounts[8].id), 1);
    assert.equal(await counts('ip', sharedIp), 30);
    assert.equal(await counts('ip', '198.51.100.99'), 0, 'forged extra forwarding hop did not select a bucket');
    check202(await attempt(c, accounts[8], otherIp, 101));
    console.log('PASS shared-IP threshold across accounts/workers, separate IP allowances, trusted-hop spoof resistance and rejected side effects');

    const burst = await Promise.all(Array.from({ length: 11 }, (_, i) =>
      attempt(i % 2 ? a : b, accounts[9], '192.0.2.4', 200 + i)));
    for (const response of burst) response.status === 202 ? check202(response) : check429(response, 3600);
    assert.equal(burst.filter(r => r.status === 202).length, 5);
    assert.equal(await counts('ip', '192.0.2.4'), 5);
    const { rows: [claimCount] } = await pool.query('SELECT count(*)::int AS count FROM membership_claims WHERE user_id=$1', [accounts[9].id]);
    assert.equal(claimCount.count, 1);
    const ipBurst = await Promise.all(Array.from({ length: 35 }, (_, i) =>
      attempt(i % 2 ? a : b, accounts[10 + Math.floor(i / 5)], '192.0.2.5', 300 + i)));
    for (const response of ipBurst) response.status === 202 ? check202(response) : check429(response, 900);
    assert.equal(ipBurst.filter(r => r.status === 202).length, 30);
    assert.equal(await counts('ip', '192.0.2.5'), 30);
    console.log('PASS concurrent cross-process account and shared-IP bursts, one-open-claim deduplication');

    await Promise.all([stopWorker(a.child), stopWorker(b.child), stopWorker(c.child)]);
    const restarted = await startWorker(true);
    check429(await attempt(restarted, accounts[0], firstIp), 3600);
    check429(await attempt(restarted, accounts[17], sharedIp), 900);
    await age('account', accounts[0].id, 59 * 60000);
    await age('ip', sharedIp, 14 * 60000);
    check429(await attempt(restarted, accounts[0], firstIp), 60);
    check429(await attempt(restarted, accounts[17], sharedIp), 60);
    await age('account', accounts[0].id, 2 * 60000);
    await age('ip', sharedIp, 2 * 60000);
    const expiredKey = hash('expired-fixture');
    await pool.query("INSERT INTO membership_claim_rate_limits VALUES ('ip',$1,ARRAY[1::bigint],2)", [expiredKey]);
    check202(await attempt(restarted, accounts[0], firstIp));
    check202(await attempt(restarted, accounts[17], sharedIp));
    assert.equal(await counts('account', accounts[0].id), 1);
    assert.equal(await counts('ip', sharedIp), 1);
    assert.equal(await counts('ip', 'expired-fixture'), 0);
    console.log('PASS restart persistence, both window boundaries, expired bucket cleanup');

    const direct = await startWorker(false);
    for (let i = 0; i < 30; i++) {
      check202(await attempt(direct, accounts[18 + Math.floor(i / 5)], `198.51.100.${i + 1}`));
    }
    check429(await attempt(direct, accounts[24], '198.51.100.200'), 900);
    assert.equal(await counts('ip', '127.0.0.1'), 30);
    assert.equal(await counts('ip', '198.51.100.200'), 0);
    console.log('PASS direct-mode forwarding-header spoof resistance');

    const authorityIp = '192.0.2.6';
    const claimant = accounts[25];
    const forged = { token: jwt.sign({ id: claimant.id }, crypto.randomBytes(48).toString('hex')) };
    assert.equal((await attempt(restarted, forged, authorityIp)).status, 401);
    await pool.query('INSERT INTO revoked_tokens (token_hash,user_id,expires_at,revoked_at) VALUES ($1,$2,$3,$4)',
      [hash(claimant.token), claimant.id, Date.now() + 60000, Date.now()]);
    assert.equal((await attempt(restarted, claimant, authorityIp)).status, 401);
    assert.equal(await counts('account', claimant.id), 0);
    assert.equal(await counts('ip', authorityIp), 0);
    console.log('PASS forged and revoked bearers cannot spend account or IP quota');
  } catch (error) {
    failures.push(error);
  } finally {
    const results = await Promise.allSettled([...workers].map(stopWorker));
    for (const result of results) if (result.status === 'rejected') failures.push(result.reason);
    for (const cleanup of [
      async () => { if (pool) await pool.end(); },
      async () => { if (created) await admin.query(`DROP DATABASE "${database}"`); },
      () => admin.end()
    ]) {
      try { await cleanup(); } catch (error) { failures.push(error); }
    }
  }
  if (failures.length) throw new AggregateError(failures, 'SEC-01 test or teardown failed');
  console.log('PASS SEC-01 acceptance checks and owned worker/database teardown');
}

(process.argv[2] === '--worker' ? runWorker() : run()).catch(error => {
  console.error(error);
  process.exit(1);
});
