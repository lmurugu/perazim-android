'use strict';

// Review item 7: every person-linked column is covered by the erasure policy, the
// policy names only real columns, and an erased member has no rows left in the
// columns marked "delete". Runs on a fresh disposable database.
const assert = require('node:assert/strict');
const { assertDisposableTestDatabase } = require('./test_database_safety');
const { initDb, query, withTransaction, closeDb } = require('../src/db');
const { eraseMemberData, ERASURE_POLICY } = require('../src/privacy/erasure');

const PERSON_COLUMN_NAMES = ['user_id', 'author_id', 'owner_id', 'sender_id', 'receiver_id', 'requester_id', 'recipient_id', 'actor_id'];

async function run() {
  assertDisposableTestDatabase(process.env.DATABASE_URL);
  await initDb();

  const fkColumns = (await query(`
    SELECT tc.table_name, kcu.column_name
      FROM information_schema.table_constraints tc
      JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
      JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema
     WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public' AND ccu.table_name = 'users'`)).rows;
  const namedColumns = (await query(`
    SELECT table_name, column_name FROM information_schema.columns
     WHERE table_schema = 'public' AND column_name = ANY($1::text[])`, [PERSON_COLUMN_NAMES])).rows;
  const candidates = new Set([...fkColumns, ...namedColumns].map(row => `${row.table_name}.${row.column_name}`));

  const missing = [...candidates].filter(key => !(key in ERASURE_POLICY));
  assert.deepEqual(missing, [], 'every person-linked column has an erasure policy entry');

  const allColumns = new Set((await query(`SELECT table_name || '.' || column_name AS key FROM information_schema.columns WHERE table_schema = 'public'`)).rows.map(row => row.key));
  const stale = Object.keys(ERASURE_POLICY).filter(key => !allColumns.has(key));
  assert.deepEqual(stale, [], 'every policy entry names a real column');
  process.stdout.write(`PASS: ${candidates.size} person-linked columns are all covered by the erasure policy.\n`);

  const now = Date.now();
  await query(`INSERT INTO users (id, phone, pin_hash, name, campus_id, role, is_active, created_at, updated_at)
               VALUES ('complete_member', '+254722000001', 'synthetic-hash', 'Synthetic Completeness', NULL, 'MEMBER', TRUE, $1, $1)`, [now]);
  await withTransaction(client => eraseMemberData(client, 'complete_member', now));

  const deleteKeys = Object.keys(ERASURE_POLICY).filter(key => ERASURE_POLICY[key] === 'delete');
  for (const key of deleteKeys) {
    const [table, column] = key.split('.');
    const left = await query(`SELECT COUNT(*)::int AS n FROM ${table} WHERE ${column} = 'complete_member'`);
    assert.equal(left.rows[0].n, 0, `no rows remain in ${key}`);
  }
  process.stdout.write(`PASS: no rows remain for the erased member in ${deleteKeys.length} deleted columns.\n`);

  const account = (await query(`SELECT is_active, auth_valid_after_seconds FROM users WHERE id = 'complete_member'`)).rows[0];
  assert.equal(account.is_active, false, 'login is disabled');
  assert.ok(Number(account.auth_valid_after_seconds) > Math.floor(now / 1000), 'tokens issued before erasure are revoked');
  process.stdout.write('PASS: erased account is disabled and its tokens are revoked.\n');

  await closeDb();
}

run().catch(async error => {
  console.error('FAIL: erasure completeness test:', error.message);
  try { await closeDb(); } catch (_) {}
  process.exit(1);
});
