'use strict';

// Review fix F-02: 15-minute access tokens, single-use rotating refresh tokens,
// a short grace period for races, and revocation when a token is reused later.
// Runs on a fresh disposable PostgreSQL database.
const assert = require('node:assert/strict');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { assertDisposableTestDatabase } = require('./test_database_safety');

async function main() {
  assertDisposableTestDatabase(process.env.DATABASE_URL);
  const { app } = require('../src/index');
  const { initDb, query, closeDb } = require('../src/db');
  await initDb();
  const now = Date.now();
  await query(`INSERT INTO users (id, phone, pin_hash, name, campus_id, role, is_active, created_at, updated_at)
               VALUES ('refresh_member', '+254733000001', $1, 'Synthetic Refresh', NULL, 'MEMBER', TRUE, $2, $2)`,
    [await bcrypt.hash('123456', 12), now]);

  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const post = async (path, body, token) => {
    const res = await fetch(base + path, { method: 'POST', headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) }, body: JSON.stringify(body) });
    return { status: res.status, body: await res.json().catch(() => ({})) };
  };
  const meStatus = async token => (await fetch(base + '/api/auth/me', { headers: { authorization: `Bearer ${token}` } })).status;

  try {
    const login = await post('/api/auth/login', { phone: '+254733000001', pin: '123456' });
    assert.equal(login.status, 200);
    assert.ok(login.body.refresh_token, 'login returns a refresh token');
    const lifetime = jwt.decode(login.body.token).exp - jwt.decode(login.body.token).iat;
    assert.equal(lifetime, 15 * 60, 'access tokens last 15 minutes');
    process.stdout.write('PASS: access tokens last 15 minutes and sign-in returns a refresh token.\n');

    const first = await post('/api/auth/refresh', { refresh_token: login.body.refresh_token });
    assert.equal(first.status, 200, 'a refresh token exchanges for a new session');
    assert.equal(await meStatus(first.body.token), 200, 'the new access token works');
    process.stdout.write('PASS: refresh rotates the session.\n');

    // Reuse just after rotation (race between two requests): refused, but the new session survives.
    const race = await post('/api/auth/refresh', { refresh_token: login.body.refresh_token });
    assert.equal(race.status, 401, 'a reused refresh token is refused');
    assert.equal(await meStatus(first.body.token), 200, 'the race does not end the new session');
    process.stdout.write('PASS: a reused token inside the grace period is refused without ending the session.\n');

    // Reuse after the grace period: every session on the account is revoked.
    await query(`UPDATE refresh_tokens SET revoked_at = revoked_at - 3600000 WHERE user_id = 'refresh_member' AND revoked_at IS NOT NULL`);
    const stolen = await post('/api/auth/refresh', { refresh_token: login.body.refresh_token });
    assert.equal(stolen.status, 401);
    const after = await post('/api/auth/refresh', { refresh_token: first.body.refresh_token });
    assert.equal(after.status, 401, 'after reuse, the account\'s sessions are revoked');
    process.stdout.write('PASS: late reuse revokes all sessions on the account.\n');

    const again = await post('/api/auth/login', { phone: '+254733000001', pin: '123456' });
    assert.equal((await post('/api/auth/refresh', { refresh_token: again.body.refresh_token })).status, 200, 'signing in again works');
    process.stdout.write('PASS: signing in again starts a new session.\n');
  } finally {
    await new Promise(resolve => server.close(resolve));
    await closeDb();
  }
}

main().catch(error => {
  console.error('FAIL: refresh token test:', error.message);
  process.exit(1);
});
