'use strict';

const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const { assertDisposableTestDatabase } = require('./test_database_safety');

if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL must target a disposable test database');
assertDisposableTestDatabase(process.env.DATABASE_URL);
if (!process.env.JWT_SECRET) process.env.JWT_SECRET = crypto.randomBytes(32).toString('hex');

const { app } = require('../src/index');
const { initDb, query, closeDb } = require('../src/db');

function assert(condition, message) {
  if (!condition) throw new Error(`FAIL: ${message}`);
  console.log(`PASS: ${message}`);
}

async function getJson(origin, path, userId = null) {
  const headers = userId ? { authorization: `Bearer ${jwt.sign({ id: userId }, process.env.JWT_SECRET, { expiresIn: '5m' })}` } : {};
  const response = await fetch(`${origin}${path}`, { headers });
  if (!response.ok) throw new Error(`HTTP ${response.status} for ${path}`);
  return response.json();
}

async function postJson(origin, path, body, userId = null) {
  const headers = { 'content-type': 'application/json' };
  if (userId) headers.authorization = `Bearer ${jwt.sign({ id: userId }, process.env.JWT_SECRET, { expiresIn: '5m' })}`;
  const response = await fetch(`${origin}${path}`, { method: 'POST', headers, body: JSON.stringify(body) });
  if (!response.ok) throw new Error(`HTTP ${response.status} for ${path}`);
  return response.json();
}

async function main() {
  const suffix = crypto.randomUUID().replace(/-/g, '');
  const userA = `b1_user_a_${suffix}`;
  const userB = `b1_user_b_${suffix}`;
  const admin = `b1_admin_${suffix}`;
  const privateId = `b1_private_${suffix}`;
  const publicId = `b1_public_${suffix}`;
  const laterPublicId = `b1_later_${suffix}`;
  const prayerIds = [privateId, publicId, laterPublicId];
  let server;

  try {
    await initDb();
    const now = Date.now() + 1000;
    for (const [id, role] of [[userA, 'MEMBER'], [userB, 'MEMBER'], [admin, 'ADMIN']]) {
      await query(
        `INSERT INTO users (id,phone,pin_hash,name,campus_id,role,avatar_url,xp,streak,grace_points,is_active,created_at,updated_at)
         VALUES ($1,$2,'synthetic-test-hash',$3,'campus_embu',$4,'',0,0,0,TRUE,$5,$5)`,
        [id, `b1-${id.slice(0, 28)}`, `Synthetic ${role}`, role, now]
      );
    }
    const rows = [
      [privateId, 'Private synthetic prayer', userA, 'PRIVATE', now],
      [publicId, 'Public synthetic prayer', userA, 'PUBLIC', now + 10],
      [laterPublicId, 'Later public synthetic prayer', userB, 'PUBLIC', now + 20]
    ];
    for (const [id, title, authorId, visibility, timestamp] of rows) {
      await query(
        `INSERT INTO prayers (id,title,content,author_id,author_name,is_anonymous,visibility,prayer_count,is_answered,testimony,created_at,updated_at)
         VALUES ($1,$2,'synthetic body',$3,'Synthetic test author',FALSE,$4,0,FALSE,'synthetic testimony',$5,$5)`,
        [id, title, authorId, visibility, timestamp]
      );
    }

    server = await new Promise((resolve, reject) => {
      const listener = app.listen(0, '127.0.0.1', () => resolve(listener));
      listener.once('error', reject);
    });
    const origin = `http://127.0.0.1:${server.address().port}`;

    const guestWall = await getJson(origin, '/api/prayers?since=0');
    assert(!guestWall.prayers.some(row => row.id === privateId), 'guest wall excludes another user’s PRIVATE prayer');
    assert(guestWall.prayers.some(row => row.id === publicId), 'guest wall still returns PUBLIC prayers');

    const userBWall = await getJson(origin, '/api/prayers?since=0', userB);
    assert(!userBWall.prayers.some(row => row.id === privateId), 'other member wall excludes the owner’s PRIVATE prayer');

    const userAWall = await getJson(origin, '/api/prayers?since=0', userA);
    assert(userAWall.prayers.some(row => row.id === privateId), 'owner can read their own PRIVATE prayer');

    const cursorPayload = { limit: 100, cursor: `${now - 1}:!` };
    const guestCursor = await postJson(origin, '/api/sync', cursorPayload);
    assert(!guestCursor.prayers.some(row => row.id === privateId), 'guest tuple-cursor sync excludes PRIVATE prayer');
    const userBCursor = await postJson(origin, '/api/sync', cursorPayload, userB);
    assert(!userBCursor.prayers.some(row => row.id === privateId), 'other member tuple-cursor sync excludes PRIVATE prayer');

    const adminWall = await getJson(origin, '/api/prayers?since=0', admin);
    assert(adminWall.prayers.some(row => row.id === privateId), 'existing ADMIN role check can read all prayers');
  } finally {
    if (server) await new Promise(resolve => server.close(resolve));
    try {
      await query('DELETE FROM prayers WHERE id = ANY($1::text[])', [prayerIds]);
      await query('DELETE FROM users WHERE id = ANY($1::text[])', [[userA, userB, admin]]);
    } finally {
      await closeDb();
    }
  }
}

main().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
