#!/usr/bin/env node
const { Pool } = require('pg');
const { status, plan, migrate, baseline, loadMigrations, assertMigrationPlanAllowed } = require('../src/migrations');

async function main() {
  const command = process.argv[2] || 'status';
  const commands = { status, plan, migrate, baseline };
  if (!commands[command]) throw new Error('Usage: node scripts/db-migrate.js [status|plan|migrate|baseline]');
  if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL is required.');
  if (command === 'migrate' || command === 'baseline') {
    if (process.env.NODE_ENV === 'production') throw new Error('Production writes are disabled in this foundation release.');
    const confirmation = process.argv[3];
    const databaseUrl = new URL(process.env.DATABASE_URL);
    const host = (databaseUrl.searchParams.get('host') || databaseUrl.hostname).replace(/^\[|\]$/g, '').toLowerCase();
    if (!(host === 'localhost' || host === '127.0.0.1' || host === '::1' || host.startsWith('/tmp/'))) {
      throw new Error('Write commands are limited to local PostgreSQL connections in this foundation release.');
    }
    const target = decodeURIComponent(databaseUrl.pathname.slice(1));
    if (!target.endsWith('_test')) throw new Error('Write commands are limited to disposable databases ending in _test in this foundation release.');
    if (confirmation !== `--confirm=${target}`) {
      throw new Error(`Write command requires the exact target confirmation: --confirm=${target}`);
    }
  }
  if (command === 'migrate') assertMigrationPlanAllowed(loadMigrations());
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  try { process.stdout.write(`${JSON.stringify(await commands[command](pool), null, 2)}\n`); }
  finally { await pool.end(); }
}

main().catch(error => {
  process.stderr.write(`Migration command failed: ${error.message}\n`);
  process.exitCode = 1;
});
