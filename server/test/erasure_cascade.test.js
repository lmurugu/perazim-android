'use strict';

// Plan C.2: an erasure request must remove the member's personal content
// and leave other members' content intact. Runs on a fresh disposable database.
const assert = require('node:assert/strict');
const { assertDisposableTestDatabase } = require('./test_database_safety');
const { initDb, query, withTransaction, closeDb } = require('../src/db');
const { eraseMemberData } = require('../src/privacy/erasure');

async function run() {
  assertDisposableTestDatabase(process.env.DATABASE_URL);
  await initDb();
  const now = Date.now();
  const insertUser = (id, phone, name) => query(
    `INSERT INTO users (id, phone, pin_hash, name, campus_id, role, is_active, created_at, updated_at)
     VALUES ($1, $2, 'synthetic-hash', $3, NULL, 'MEMBER', TRUE, $4, $4)`,
    [id, phone, name, now]);
  await insertUser('erase_a', '+254700000001', 'Synthetic Erase A');
  await insertUser('erase_b', '+254700000002', 'Synthetic Erase B');

  await query(`INSERT INTO prayers (id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at)
               VALUES ('pr_a', 'A', 'synthetic A prayer', 'erase_a', 'Synthetic Erase A', FALSE, 'PUBLIC', 0, FALSE, '', $1, $1),
                      ('pr_b', 'B', 'synthetic B prayer', 'erase_b', 'Synthetic Erase B', FALSE, 'PUBLIC', 0, FALSE, '', $1, $1)`, [now]);
  await query(`INSERT INTO prayer_amens (prayer_id, user_id, created_at) VALUES ('pr_b', 'erase_a', $1)`, [now]);
  await query(`INSERT INTO messages (id, conversation_id, sender_id, receiver_id, content, status, created_at, delivery_status) VALUES ('msg_1', 'conv_ab', 'erase_a', 'erase_b', 'synthetic message', 'SENT', $1, 'SENT')`, [now]);
  assert.equal((await query(`SELECT COUNT(*)::int AS n FROM messages WHERE id = 'msg_1'`)).rows[0].n, 1, 'fixture message is stored before erasure');
  await query(`INSERT INTO connections (id, requester_id, recipient_id, status, created_at) VALUES ('conn_1', 'erase_a', 'erase_b', 'ACCEPTED', $1)`, [now]);

  const result = await withTransaction(client => eraseMemberData(client, 'erase_a', now));
  assert.ok(Array.isArray(result.filenames));

  assert.equal((await query(`SELECT COUNT(*)::int AS n FROM prayers WHERE author_id = 'erase_a'`)).rows[0].n, 0, 'member prayers are removed');
  assert.equal((await query(`SELECT COUNT(*)::int AS n FROM prayer_amens WHERE user_id = 'erase_a'`)).rows[0].n, 0, 'member amens are removed');
  assert.equal((await query(`SELECT COUNT(*)::int AS n FROM connections WHERE requester_id = 'erase_a' OR recipient_id = 'erase_a'`)).rows[0].n, 0, 'member connections are removed');
  assert.equal((await query(`SELECT COUNT(*)::int AS n FROM messages WHERE sender_id = 'erase_a' OR receiver_id = 'erase_a'`)).rows[0].n, 0, 'member messages are removed');

  const account = (await query(`SELECT name, phone, is_active, pin_hash FROM users WHERE id = 'erase_a'`)).rows[0];
  assert.equal(account.name, 'Erased member', 'name is anonymised');
  assert.ok(account.phone.startsWith('erased-'), 'phone is replaced');
  assert.equal(account.is_active, false, 'login is disabled');
  assert.notEqual(account.pin_hash, 'synthetic-hash', 'PIN hash is replaced');

  assert.equal((await query(`SELECT COUNT(*)::int AS n FROM prayers WHERE id = 'pr_b'`)).rows[0].n, 1, 'other members prayers are untouched');
  assert.equal((await query(`SELECT COUNT(*)::int AS n FROM users WHERE id = 'erase_b' AND is_active = TRUE`)).rows[0].n, 1, 'other members accounts are untouched');

  await closeDb();
  process.stdout.write('PASS: erasure removes the member content, anonymises the account and leaves other members data intact.\n');
}

run().catch(async error => {
  console.error('FAIL: erasure cascade test:', error.message);
  try { await closeDb(); } catch (_) {}
  process.exit(1);
});
