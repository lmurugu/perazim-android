'use strict';

const assert = require('node:assert/strict');
const { loadMigrations, assertMigrationPlanAllowed, migrate } = require('../src/migrations');

const plan = loadMigrations();
assert.throws(
  () => assertMigrationPlanAllowed(plan),
  error => error.code === 'PROHIBITED_MIGRATION_IN_PLAN' && /0007_disable_contact_list_verification/.test(error.message),
  'a plan containing migration 0007 is rejected'
);

let connections = 0;
const pool = { async connect() { connections += 1; throw new Error('connect must not be called'); } };
migrate(pool).then(
  () => { throw new Error('migrate unexpectedly accepted a plan containing migration 0007'); },
  error => {
    assert.equal(error.code, 'PROHIBITED_MIGRATION_IN_PLAN');
    assert.equal(connections, 0, 'the migration is refused before pool.connect()');
    process.stdout.write('PASS: migration 0007 plan refused before database connection.\n');
  }
).catch(error => { console.error(error); process.exitCode = 1; });
