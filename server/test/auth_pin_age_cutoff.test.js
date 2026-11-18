'use strict';

// Review corrections 1-3: age confirmation at sign-up, one login answer for every
// failure, legacy 4-digit PINs still sign in, PIN change revokes older tokens.
// Runs on a fresh disposable PostgreSQL database.
const assert = require('node:assert/strict');
const bcrypt = require('bcryptjs');
const { assertDisposableTestDatabase } = require('./test_database_safety');

async function main() {
  assertDisposableTestDatabase(process.env.DATABASE_URL);
  const { app } = require('../src/index');
  const { initDb, query, closeDb } = require('../src/db');
  await initDb();
  const now = Date.now();
  await query(`INSERT INTO users (id, phone, pin_hash, name, campus_id, role, is_active, created_at, updated_at)
               VALUES ('legacy_user', '+254711000001', $1, 'Synthetic Legacy', NULL, 'MEMBER', TRUE, $2, $2)`,
    [await bcrypt.hash('1234', 12), now]);

  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const call = async (method, path, body, token) => {
    const res = await fetch(base + path, {
      method,
      headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined
    });
    return { status: res.status, body: await res.json().catch(() => ({})) };
  };

  try {
    // 1. Age confirmation is required by the server, not only by the app.
    const noAge = await call('POST', '/api/auth/register', { phone: '+254711000002', pin: '654321', name: 'Synthetic New', campus_id: 'campus_embu' });
    assert.equal(noAge.status, 400, 'registration without age confirmation is refused');
    assert.match(noAge.body.error, /18/, 'the refusal explains the age rule');
    process.stdout.write('PASS: registration without age confirmation is refused by the server.\n');

    // 2. A legacy 4-digit PIN still signs in.
    const ok = await call('POST', '/api/auth/login', { phone: '+254711000001', pin: '1234' });
    assert.equal(ok.status, 200, 'a legacy 4-digit PIN still signs in');
    const oldToken = ok.body.token;
    process.stdout.write('PASS: legacy 4-digit PIN still signs in.\n');

    // 3. One answer for unknown number and wrong PIN.
    const unknown = await call('POST', '/api/auth/login', { phone: '+254799999999', pin: '1234' });
    const wrong = await call('POST', '/api/auth/login', { phone: '+254711000001', pin: '0000' });
    assert.equal(unknown.status, 401);
    assert.equal(wrong.status, 401);
    assert.equal(unknown.body.error, wrong.body.error, 'unknown number and wrong PIN give the same message');
    process.stdout.write('PASS: unknown number and wrong PIN get the same answer.\n');

    // 4. PIN change: rules, current PIN check, and revocation of older tokens.
    assert.equal((await call('GET', '/api/auth/me', null, oldToken)).status, 200, 'token works before the change');
    await new Promise(resolve => setTimeout(resolve, 1100));
    assert.equal((await call('POST', '/api/auth/change-pin', { current_pin: '1234', new_pin: '12345' }, oldToken)).status, 400, 'new PIN must be 6 digits');
    assert.equal((await call('POST', '/api/auth/change-pin', { current_pin: '0000', new_pin: '123456' }, oldToken)).status, 401, 'wrong current PIN is refused');
    const changed = await call('POST', '/api/auth/change-pin', { current_pin: '1234', new_pin: '123456' }, oldToken);
    assert.equal(changed.status, 200, 'PIN change succeeds with the current PIN');
    const newToken = changed.body.token;
    assert.equal((await call('GET', '/api/auth/me', null, oldToken)).status, 401, 'the token from before the change is revoked');
    assert.equal((await call('GET', '/api/auth/me', null, newToken)).status, 200, 'the token issued by the change works');
    assert.equal((await call('POST', '/api/auth/login', { phone: '+254711000001', pin: '1234' })).status, 401, 'the old PIN no longer works');
    assert.equal((await call('POST', '/api/auth/login', { phone: '+254711000001', pin: '123456' })).status, 200, 'the new PIN works');
    process.stdout.write('PASS: PIN change needs the current PIN, revokes older tokens and switches the PIN.\n');
  } finally {
    await new Promise(resolve => server.close(resolve));
    await closeDb();
  }
}

main().catch(error => {
  console.error('FAIL: PIN, age and token cutoff test:', error.message);
  process.exit(1);
});
