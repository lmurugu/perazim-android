'use strict';

// Church-wide games leaderboard: opt-in, first names only, duplicate uploads ignored, limits enforced.
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const { assertDisposableTestDatabase } = require('./test_database_safety');

if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL must target a disposable test database');
assertDisposableTestDatabase(process.env.DATABASE_URL);
if (!process.env.JWT_SECRET) process.env.JWT_SECRET = crypto.randomBytes(32).toString('hex');

const { app } = require('../src/index');
const { initDb, query, closeDb } = require('../src/db');
const { _internal } = require('../src/routes/games');

function assert(condition, message) {
  if (!condition) throw new Error(`FAIL: ${message}`);
  console.log(`PASS: ${message}`);
}

function auth(userId) {
  return { authorization: `Bearer ${jwt.sign({ id: userId }, process.env.JWT_SECRET, { expiresIn: '5m' })}` };
}

async function call(origin, method, path, body, userId) {
  const headers = { 'content-type': 'application/json', ...(userId ? auth(userId) : {}) };
  const response = await fetch(`${origin}${path}`, { method, headers, body: body ? JSON.stringify(body) : undefined });
  return { status: response.status, body: await response.json() };
}

async function main() {
  // Pure helpers.
  assert(_internal.weekStart(Date.UTC(2026, 10, 18, 12)) === '2026-11-16', 'week starts on Monday in Kenyan time');
  assert(_internal.weekStart(Date.UTC(2026, 10, 15, 22)) === '2026-11-16', 'Sunday 22:00 UTC is already Monday in Kenya');
  assert(_internal.firstName('grace  WANJIKU') === 'Grace', 'only the first name is shown, tidied');
  assert(_internal.firstName('') === 'Member', 'blank names show as Member');
  assert(_internal.isAdultConfirmed(1700000000000) === true, 'a confirmed 18+ account counts as adult');
  assert(_internal.isAdultConfirmed(null) === false && _internal.isAdultConfirmed(undefined) === false, 'no age confirmation means not adult');

  const suffix = crypto.randomUUID().replace(/-/g, '').slice(0, 12);
  const alice = `lb_a_${suffix}`, bob = `lb_b_${suffix}`, carol = `lb_c_${suffix}`;
  const users = [[alice, 'Synthetic Alice Tester'], [bob, 'Synthetic Bob'], [carol, 'Synthetic Carol']];
  let server;
  try {
    await initDb();
    const now = Date.now();
    // Alice and Bob confirmed 18+; Carol has no confirmation, so she is a non-adult in this test.
    const adultIds = new Set([alice, bob]);
    for (const [id, name] of users) {
      await query(
        `INSERT INTO users (id,phone,pin_hash,name,campus_id,role,avatar_url,xp,streak,grace_points,is_active,created_at,updated_at,age_confirmed_at)
         VALUES ($1,$2,'synthetic-test-hash',$3,'campus_embu','MEMBER','',0,0,0,TRUE,$4,$4,$5)`,
        [id, `lb-${id}`, name, now, adultIds.has(id) ? now : null]
      );
    }
    server = await new Promise((resolve, reject) => {
      const listener = app.listen(0, '127.0.0.1', () => resolve(listener));
      listener.once('error', reject);
    });
    const origin = `http://127.0.0.1:${server.address().port}`;

    assert((await call(origin, 'POST', '/api/games/scores', { runId: 'run_guest_000', game: 'ladder', points: 3 })).status === 401, 'guests cannot upload scores');
    assert((await call(origin, 'POST', '/api/games/scores', { runId: 'bad', game: 'ladder', points: 3 }, alice)).status === 400, 'short run ids are rejected');
    assert((await call(origin, 'POST', '/api/games/scores', { runId: 'run_alice_big', game: 'ladder', points: 999 }, alice)).status === 400, 'oversized uploads are rejected');

    const first = await call(origin, 'POST', '/api/games/scores', { runId: 'run_alice_001', game: 'ladder', points: 12 }, alice);
    assert(first.status === 200 && first.body.weekPoints === 12 && !first.body.duplicate, 'an upload adds to this week');
    const retry = await call(origin, 'POST', '/api/games/scores', { runId: 'run_alice_001', game: 'ladder', points: 12 }, alice);
    assert(retry.body.duplicate === true && retry.body.weekPoints === 12, 'a retried upload is not counted twice');
    await call(origin, 'POST', '/api/games/scores', { runId: 'run_alice_002', game: 'quiz', points: 5 }, alice);
    await call(origin, 'POST', '/api/games/scores', { runId: 'run_bob_0001', game: 'ladder', points: 20 }, bob);
    const childUpload = await call(origin, 'POST', '/api/games/scores', { runId: 'run_carol_001', game: 'ladder', points: 30 }, carol);
    assert(childUpload.status === 200 && childUpload.body.recorded === false, 'a non-adult upload is not stored');
    const childOptIn = await call(origin, 'PUT', '/api/games/leaderboard/me', { optIn: true }, carol);
    assert(childOptIn.status === 403 && childOptIn.body.code === 'ADULTS_ONLY', 'a non-adult cannot join the leaderboard');
    const childStatus = await call(origin, 'GET', '/api/games/leaderboard/me', null, carol);
    assert(childStatus.body.eligible === false, 'a non-adult is told the leaderboard is closed to them');

    let board = await call(origin, 'GET', '/api/games/leaderboard?period=week');
    const listed = board.body.entries.map(e => e.name);
    assert(!listed.includes('Synthetic'), 'nobody is listed before opting in');

    assert((await call(origin, 'PUT', '/api/games/leaderboard/me', { optIn: 'yes' }, alice)).status === 400, 'opt-in must be a boolean');
    const optIn = await call(origin, 'PUT', '/api/games/leaderboard/me', { optIn: true }, alice);
    assert(optIn.body.optIn === true && optIn.body.displayName === 'Synthetic', 'opting in shows the first name that will appear');
    await call(origin, 'PUT', '/api/games/leaderboard/me', { optIn: true }, bob);

    board = await call(origin, 'GET', '/api/games/leaderboard?period=week', null, alice);
    const ours = board.body.entries.filter(e => e.points === 17 || e.points === 20);
    assert(ours.length === 2, 'opted-in members appear with their weekly points');
    assert(!board.body.entries.some(e => e.points === 30), 'a member who did not opt in stays hidden');
    assert(board.body.me && board.body.me.points === 17, 'the caller sees their own points and rank');
    assert(board.body.entries.every(e => !('userId' in e) && !('phone' in e)), 'no ids or phone numbers are exposed');
    const aliceRow = board.body.entries.find(e => e.me);
    const bobRow = board.body.entries.find(e => e.points === 20);
    assert(bobRow.rank < aliceRow.rank, 'higher points rank higher');

    const all = await call(origin, 'GET', '/api/games/leaderboard?period=all', null, alice);
    assert(all.body.period === 'all' && all.body.me.points === 17, 'all-time totals are available');

    const settings = await call(origin, 'GET', '/api/games/settings');
    assert(settings.status === 200 && settings.body.dailyStarCap === 50 && settings.body.missedPercent === 30, 'game settings are public and carry the agreed defaults');

    await call(origin, 'PUT', '/api/games/leaderboard/me', { optIn: false }, alice);
    board = await call(origin, 'GET', '/api/games/leaderboard?period=week', null, alice);
    assert(!board.body.entries.some(e => e.points === 17) && board.body.me === null, 'opting out removes the member at once');
  } finally {
    if (server) await new Promise(resolve => server.close(resolve));
    try {
      const ids = users.map(u => u[0]);
      await query('DELETE FROM game_score_events WHERE user_id = ANY($1::text[])', [ids]);
      await query('DELETE FROM game_score_weeks WHERE user_id = ANY($1::text[])', [ids]);
      await query('DELETE FROM game_leaderboard_profiles WHERE user_id = ANY($1::text[])', [ids]);
      await query('DELETE FROM users WHERE id = ANY($1::text[])', [ids]);
    } finally {
      await closeDb();
    }
  }
}

main().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
