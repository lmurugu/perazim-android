'use strict';

// Synthetic current-authority gate. Never accepts an application database URL.
const assert = require('assert/strict');
const crypto = require('crypto');
const { Pool } = require('pg');
const { migrate } = require('../src/migrations');
const { getCurrentNotice } = require('../src/phase-b/privacyNotice');

async function run() {
  const adminUrl = new URL(process.env.PHASE_B_TEST_ADMIN_URL || '');
  assert.equal(adminUrl.pathname, '/postgres');
  assert(['127.0.0.1', 'localhost', '[::1]'].includes(adminUrl.hostname));
  assert.notEqual(process.env.NODE_ENV, 'production');
  const name = `perazim_phaseb_${crypto.randomBytes(8).toString('hex')}_test`;
  const admin = new Pool({ connectionString: adminUrl.toString() });
  let pool, server;
  const db = require('../src/db');
  try {
    await admin.query(`CREATE DATABASE "${name}"`);
    adminUrl.pathname = '/' + name;
    process.env.DATABASE_URL = adminUrl.toString();
    process.env.NODE_ENV = 'test';
    process.env.PHASE_B_SYNTHETIC = 'true';
    process.env.JWT_SECRET = crypto.randomBytes(48).toString('hex');
    pool = new Pool({ connectionString: adminUrl.toString() });
    await migrate(pool);
    await db.initDb();
    const { app } = require('../src/index');
    const { requireAuth, signToken } = require('../src/middleware/auth');
    app.get('/api/authority-probe', requireAuth, (req, res) => res.json(req.user));
    server = await new Promise(resolve => {
      const listener = app.listen(0, '127.0.0.1', () => resolve(listener));
    });
    const base = `http://127.0.0.1:${server.address().port}`;
    const notice = await getCurrentNotice();
    async function request(path, token, body) {
      const response = await fetch(base + path, {
        method: body ? 'POST' : 'GET',
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
        ...(body ? { body: JSON.stringify(body) } : {})
      });
      return { status: response.status, body: await response.json() };
    }
    const registration = await request('/api/auth/register', null, {
      name: 'Synthetic Authority Test', phone: '+254700009881', pin: '853692',
      role: 'ADMIN', membership_status: 'VERIFIED_MEMBER', permissions: ['admin.permissions.grant'],
      privacy_notice_version: notice.version, privacy_notice_hash: notice.content_hash,
      privacy_notice_acknowledged: true
    });
    assert.equal(registration.status, 201);
    const { user, token } = registration.body;
    assert.equal(user.role, 'MEMBER');
    const stale = signToken({ ...user, role: 'ADMIN', name: 'Stale name' });
    const resolved = await request('/api/authority-probe', stale);
    assert.equal(resolved.status, 200);
    assert.equal(resolved.body.role, 'MEMBER');
    assert.equal(resolved.body.name, 'Synthetic Authority Test');
    assert.equal(resolved.body.membership_status, undefined);
    await pool.query("UPDATE users SET role='MINISTRY_WORKER' WHERE id=$1", [user.id]);
    assert.equal((await request('/api/authority-probe', token)).body.role, 'MINISTRY_WORKER');
    const invalid = token.slice(0, -8) + 'invalidx';
    assert.equal((await request('/api/authority-probe', invalid)).status, 401);
    await pool.query('UPDATE users SET is_active=FALSE WHERE id=$1', [user.id]);
    assert.equal((await request('/api/authority-probe', token)).status, 401);
    assert.equal((await request('/api/auth/login', null, { phone: user.phone, pin: '853692' })).status, 401);
    await pool.query('UPDATE users SET is_active=TRUE WHERE id=$1', [user.id]);
    assert.equal((await request('/api/auth/logout', token, {})).status, 200);
    assert.equal((await request('/api/authority-probe', token)).status, 401);
    assert.equal((await request('/api/prayers')).status, 200);
    assert.equal((await request('/api/prayers', null, { title: 'Synthetic guest', content: 'Synthetic public fixture' })).status, 201);
    assert.equal((await request('/api/community/posts')).status, 200);
    // The community router schedules synthetic seed work one second after load.
    // Let that bounded task finish before closing pools and dropping this DB.
    await new Promise(resolve => setTimeout(resolve, 1200));
    console.log('B3 current-authority gate passed: forged/stale claims, current roles, inactive login/request, signature, logout revocation, public and guest compatibility.');
  } finally {
    if (server) await new Promise(resolve => server.close(resolve));
    await db.closeDb();
    if (pool) await pool.end();
    await admin.query(`DROP DATABASE IF EXISTS "${name}" WITH (FORCE)`);
    await admin.end();
  }
}
run().catch(error => { console.error('B3 authority gate failed:', error.message); process.exitCode = 1; });
