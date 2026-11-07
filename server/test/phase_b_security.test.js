'use strict';

// B3 authority gate: synthetic-only HTTP checks for current database identity,
// account activity, forged JWTs, individual revocation, and recovery cutoffs.
const assert = require('assert');
const crypto = require('crypto');
const http = require('http');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { assertPhaseBTestDatabase } = require('./phase_b_fixture');
const cryptoLib = require('net');

function localDatabaseAddress(value) {
  const address = String(value || '').split('/')[0].toLowerCase().replace(/^::ffff:/, '');
  if (address === '::1' || address.startsWith('fc') || address.startsWith('fd') || address.startsWith('fe80:')) return true;
  if (cryptoLib.isIP(address) !== 4) return false;
  const [a,b] = address.split('.').map(Number);
  return a === 10 || a === 127 || a === 169 && b === 254 || a === 192 && b === 168 || a === 172 && b >= 16 && b <= 31;
}

// Never inherit a runtime/member DATABASE_URL. The runner creates and migrates
// its own uniquely named local database before loading the Express app.
delete process.env.DATABASE_URL;
process.env.NODE_ENV = 'test';
process.env.PHASE_B_SYNTHETIC = 'true';
process.env.JWT_SECRET = crypto.randomBytes(32).toString('hex');
process.env.PHASE_B_ADMIN_KEY = crypto.randomBytes(32).toString('hex');

function request(port, method, path, token, body = null) {
  return new Promise((resolve, reject) => {
    const payload = body == null ? null : JSON.stringify(body);
    const req = http.request({
      hostname: '127.0.0.1', port, method, path,
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(payload ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) } : {}),
        Connection: 'close'
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try { resolve({ status: res.statusCode, headers: res.headers, body: data ? JSON.parse(data) : {} }); }
        catch (_) { resolve({ status: res.statusCode, headers: res.headers, body: { raw: data } }); }
      });
    });
    req.on('error', reject);
    if (payload) req.write(payload);
    req.end();
  });
}

function token(user, overrides = {}) {
  return jwt.sign({ id: user.id, phone: user.phone, name: user.name, role: user.role, campus_id: user.campus_id, ...overrides }, process.env.JWT_SECRET, { expiresIn: '30d' });
}

function assertLocalAdminUrl(value) {
  const url = new URL(value || '');
  const host = url.hostname.toLowerCase();
  const local = host === 'localhost' || host === '::1' || cryptoLib.isIP(host) === 4 && host.startsWith('127.');
  if (!local || decodeURIComponent(url.pathname.replace(/^\//, '')) !== 'postgres') throw new Error('PHASE_B_TEST_ADMIN_URL must point to the local postgres maintenance database');
  return url;
}

async function main() {
  const { Pool } = require('pg');
  const { migrate } = require('../src/migrations');
  const adminUrl = assertLocalAdminUrl(process.env.PHASE_B_TEST_ADMIN_URL);
  const adminPool = new Pool({ connectionString: adminUrl.toString() });
  const actualAdmin = await adminPool.query('SELECT current_database() AS database,inet_server_addr()::text AS address');
  if (actualAdmin.rows[0].database !== 'postgres' || (actualAdmin.rows[0].address && !localDatabaseAddress(actualAdmin.rows[0].address))) {
    await adminPool.end();
    throw new Error('Admin connection did not resolve to local postgres');
  }
  const dbName = `perazim_phaseb_${crypto.randomBytes(6).toString('hex')}_test`;
  await adminPool.query(`CREATE DATABASE "${dbName}"`);
  const databaseUrl = new URL(adminUrl.toString());
  databaseUrl.pathname = `/${dbName}`;
  process.env.DATABASE_URL = databaseUrl.toString();
  let pool;
  let appServer;
  let closeDb = async () => {};
  try {
    pool = new Pool({ connectionString: process.env.DATABASE_URL });
    await assertPhaseBTestDatabase(pool);
    const actualTarget = await pool.query('SELECT current_database() AS database,inet_server_addr()::text AS address');
    if (actualTarget.rows[0].database !== dbName || (actualTarget.rows[0].address && !localDatabaseAddress(actualTarget.rows[0].address))) throw new Error('Test pool did not resolve to the created local database');
    await migrate(pool);
    const { app } = require('../src/index');
    const db = require('../src/db');
    closeDb = db.closeDb;
    await db.initDb();
    const query = db.query;
    const { revokeToken } = require('../src/middleware/auth');
  const suffix = crypto.randomBytes(6).toString('hex');
  const phoneDigits = String(BigInt(`0x${suffix}`) % 10000000n).padStart(7, '0');
  const now = Date.now();
  const pinHash = await bcrypt.hash('7351', 4);
  const users = {
    target: { id: `b3_target_${suffix}`, phone: `+2547${phoneDigits}0`, name: 'Synthetic Target', role: 'MEMBER', campus_id: 'campus_embu' },
    outsider: { id: `b3_outsider_${suffix}`, phone: `+2547${phoneDigits}1`, name: 'Synthetic Outsider', role: 'MEMBER', campus_id: 'campus_nairobi' },
    inactive: { id: `b3_inactive_${suffix}`, phone: `+2547${phoneDigits}2`, name: 'Synthetic Inactive', role: 'MEMBER', campus_id: 'campus_embu' }
  };
    const server = app.listen(0, '127.0.0.1');
    appServer = server;
    await new Promise(resolve => server.once('listening', resolve));
    const port = server.address().port;
    for (const user of Object.values(users)) {
      await query(`INSERT INTO users (id,phone,pin_hash,name,campus_id,role,avatar_url,xp,streak,grace_points,is_active,created_at,updated_at)
        VALUES ($1,$2,$3,$4,$5,$6,'',0,0,0,TRUE,$7,$7)`, [user.id, user.phone, pinHash, user.name, user.campus_id, user.role, now]);
    }
    await query(`INSERT INTO community_groups (id,name,description,created_by,is_active,created_at)
      VALUES ($1,'Synthetic B3 Private Group','isolated test',$2,TRUE,$3)`, [`b3_private_${suffix}`, users.outsider.id, now]);

    // A validly signed but stale ADMIN role cannot reveal another user's private group.
    const staleAdmin = token({ ...users.target, role: 'ADMIN' });
    let response = await request(port, 'GET', '/api/community/groups', staleAdmin);
    assert.strictEqual(response.status, 200);
    assert(!response.body.groups.some(group => group.id === `b3_private_${suffix}`), 'stale privileged JWT role must not grant access');

    // A stale MEMBER role is upgraded from current DB authority, rather than blocking the DB role.
    await query('UPDATE users SET role=$1 WHERE id=$2', ['ADMIN', users.target.id]);
    const staleMember = token(users.target);
    response = await request(port, 'GET', '/api/community/groups', staleMember);
    assert.strictEqual(response.status, 200);
    assert(response.body.groups.some(group => group.id === `b3_private_${suffix}`), 'current database role must supersede stale JWT role');
    await query('UPDATE users SET role=$1 WHERE id=$2', ['MEMBER', users.target.id]);

    // Modifying a signed JWT claim without the signing key invalidates its signature.
    const parts = staleMember.split('.');
    const changedPayload = Buffer.from(JSON.stringify({ id: users.target.id, phone: users.target.phone, name: users.target.name, role: 'ADMIN', campus_id: 'campus_embu', iat: Math.floor(now / 1000), exp: Math.floor(now / 1000) + 86400 })).toString('base64url');
    response = await request(port, 'GET', '/api/community/groups', `${parts[0]}.${changedPayload}.${parts[2]}`);
    assert.strictEqual(response.status, 401, 'forged role claim must fail JWT signature verification');

    // Inactive users cannot log in or keep using an existing bearer token.
    const inactiveToken = token(users.inactive);
    await query('UPDATE users SET is_active=FALSE WHERE id=$1', [users.inactive.id]);
    response = await request(port, 'POST', '/api/auth/login', null, { phone: users.inactive.phone, pin: '7351' });
    assert.strictEqual(response.status, 401, 'inactive account cannot log in');
    response = await request(port, 'GET', '/api/community/groups', inactiveToken);
    assert.strictEqual(response.status, 401, 'inactive account cannot use an existing token');

    // Individual token revocation remains effective.
    const revoked = token(users.outsider);
    await revokeToken(revoked, users.outsider.id);
    response = await request(port, 'GET', '/api/community/groups', revoked);
    assert.strictEqual(response.status, 401, 'revoked token remains denied');

    // Recovery cutoff invalidates all tokens issued before auth_not_before.
    const oldRecoveryToken = token(users.target);
    await query(`INSERT INTO admin_security (user_id,totp_secret_cipher,auth_not_before,created_at)
      VALUES ($1,'synthetic-test', $2, $3) ON CONFLICT (user_id) DO UPDATE SET auth_not_before=EXCLUDED.auth_not_before`,
      [users.target.id, (Math.floor(now / 1000) + 2) * 1000, now]);
    response = await request(port, 'GET', '/api/community/groups', oldRecoveryToken);
    assert.strictEqual(response.status, 401, 'recovery cutoff invalidates existing bearer tokens');

    // Explicit registered-account self grants are persisted once and a revoked
    // grant is not silently recreated by the next self-service request.
    const selfId = `b3_self_${suffix}`;
    const selfPhone = `+2547${phoneDigits}3`;
    await query(`INSERT INTO users (id,phone,pin_hash,name,campus_id,role,avatar_url,xp,streak,grace_points,is_active,created_at,updated_at)
      VALUES ($1,$2,$3,'Synthetic Self Service','campus_embu','MEMBER','',0,0,0,TRUE,$4,$4)`, [selfId, selfPhone, pinHash, now]);
    const selfToken = token({ id: selfId, phone: selfPhone, name: 'Synthetic Self Service', role: 'MEMBER', campus_id: 'campus_embu' });
    response = await request(port, 'GET', '/api/membership/status', selfToken);
    assert.strictEqual(response.status, 200);
    assert.strictEqual(response.headers['cache-control'], 'private, no-store', 'protected Phase B responses are not cached');
    assert.strictEqual(response.body.status, 'REGISTERED');
    const claimGrant = await query(`SELECT id FROM user_permissions WHERE user_id=$1 AND permission_code='membership.claim.self.submit'`, [selfId]);
    assert.strictEqual(claimGrant.rows.length, 1, 'registered account gets one persisted self-service grant');
    await query(`UPDATE user_permissions SET revoked_at=$2,revoked_by=$1 WHERE id=$3`, [selfId, Date.now(), claimGrant.rows[0].id]);
    response = await request(port, 'POST', '/api/membership/claims', selfToken, { request_id: crypto.randomUUID(), name: 'Synthetic Self Service', phone: selfPhone });
    assert.strictEqual(response.status, 403, 'revoked self-service capability denies claim submission');
    const afterRevoke = await query(`SELECT COUNT(*)::int AS count FROM user_permissions WHERE user_id=$1 AND permission_code='membership.claim.self.submit'`, [selfId]);
    assert.strictEqual(afterRevoke.rows[0].count, 1, 'a revoked self-service grant is never recreated');

    // Synthetic-only route gating never changes the legacy public surface.
    process.env.PHASE_B_SYNTHETIC = 'false';
    response = await request(port, 'GET', '/api/membership/status', selfToken);
    assert.strictEqual(response.status, 503, 'new membership route fails closed outside synthetic mode');
    response = await request(port, 'GET', '/api/campuses');
    assert.strictEqual(response.status, 200, 'legacy public route remains available when Phase B gate is off');
    response = await request(port, 'GET', '/api/not-a-real-route');
    assert.strictEqual(response.status, 404, 'unmatched legacy API path is not captured by Phase B gate');
    process.env.PHASE_B_SYNTHETIC = 'true';

    process.stdout.write('PASS B3/B4 authority, account activity, revocation, self-grant persistence, and route compatibility\n');
  } finally {
    if (appServer) await new Promise(resolve => appServer.close(resolve));
    await closeDb();
    if (pool) await pool.end();
    try { await adminPool.query(`DROP DATABASE "${dbName}"`); } finally { await adminPool.end(); }
  }
}

main().catch(error => {
  process.stderr.write(`FAIL B3 authority checks: ${error.message}\n`);
  process.exitCode = 1;
});
