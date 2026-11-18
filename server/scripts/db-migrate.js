#!/usr/bin/env node
const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { status, plan, migrate, baseline, loadMigrations, assertMigrationPlanAllowed } = require('../src/migrations');

async function main() {
  const command = process.argv[2] || 'status';
  const commands = { status, plan, migrate, baseline };
  if (!commands[command]) throw new Error('Usage: node scripts/db-migrate.js [status|plan|migrate|baseline]');
  if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL is required.');
  if (command === 'migrate' || command === 'baseline') {
    const confirmation = process.argv[3];
    const databaseUrl = new URL(process.env.DATABASE_URL);
    const host = (databaseUrl.searchParams.get('host') || databaseUrl.hostname).replace(/^\[|\]$/g, '').toLowerCase();
    if (!(host === 'localhost' || host === '127.0.0.1' || host === '::1' || host.startsWith('/tmp/'))) {
      throw new Error('Write commands are limited to local PostgreSQL connections in this foundation release.');
    }
    const target = decodeURIComponent(databaseUrl.pathname.slice(1));
    if (confirmation !== `--confirm=${target}`) {
      throw new Error(`Write command requires the exact target confirmation: --confirm=${target}`);
    }
    // Real local databases (not *_test) need a second confirmation through the
    // environment and a fresh pg_dump backup taken before any change (plan 3.1).
    if (!target.endsWith('_test')) {
      if (process.env.MIGRATE_CONFIRM !== target) {
        throw new Error(`Non-test database: also set MIGRATE_CONFIRM=${target}`);
      }
      const backupDir = path.join(__dirname, '../.runtime/backups');
      fs.mkdirSync(backupDir, { recursive: true });
      const backupFile = path.join(backupDir, `${target}_before_${command}_${new Date().toISOString().replace(/[:.]/g, '-')}.dump`);
      execFileSync('pg_dump', [process.env.DATABASE_URL, '-Fc', '-f', backupFile], { stdio: 'inherit' });
      console.error(`Backup written: ${backupFile}`);
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
