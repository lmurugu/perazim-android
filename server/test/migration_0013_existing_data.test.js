const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Pool } = require('pg');
const { assertDisposableTestDatabase } = require('./test_database_safety');

if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL must target a disposable test database');
assertDisposableTestDatabase(process.env.DATABASE_URL);

async function run() {
  const schema = `migration_0013_${crypto.randomBytes(8).toString('hex')}`;
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  const client = await pool.connect();
  try {
    await client.query(`CREATE SCHEMA "${schema}"`);
    await client.query(`SET search_path TO "${schema}"`);
    await client.query('CREATE TABLE users (id VARCHAR(64) PRIMARY KEY)');
    await client.query("INSERT INTO users (id) VALUES ('synthetic_existing_user')");

    const migration = fs.readFileSync(path.join(__dirname, '../migrations/0013_sync_mutation_receipts.sql'), 'utf8');
    await client.query(migration);
    const existingUser = await client.query('SELECT id FROM users WHERE id = $1', ['synthetic_existing_user']);
    if (existingUser.rowCount !== 1) throw new Error('Migration 0013 did not preserve pre-existing synthetic data');
    console.log('PASS: migration 0013 applies when the database already contains a synthetic user row');

    const columns = await client.query(
      `SELECT column_name FROM information_schema.columns WHERE table_schema = $1 AND table_name = 'sync_receipts'`,
      [schema]
    );
    const columnNames = new Set(columns.rows.map(row => row.column_name));
    for (const required of ['user_id', 'mutation_id', 'status', 'response_json', 'created_at']) {
      if (!columnNames.has(required)) throw new Error(`Migration 0013 missing expected column: ${required}`);
    }
    console.log('PASS: migration 0013 creates the complete sync_receipts table without dropping existing rows');
  } finally {
    try { await client.query('RESET search_path'); } catch (_) {}
    try { await client.query(`DROP SCHEMA IF EXISTS "${schema}" CASCADE`); } catch (_) {}
    client.release();
    await pool.end();
  }
}

run().catch(error => {
  console.error('FAIL: migration 0013 existing-data test:', error.message);
  process.exitCode = 1;
});
