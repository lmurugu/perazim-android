'use strict';

// Migration 0007 is applied by decision (docs/adr/0002-apply-migration-0007.md).
// This test confirms it is part of the ordered plan and is accepted by the guard.
const assert = require('node:assert/strict');
const { loadMigrations, assertMigrationPlanAllowed } = require('../src/migrations');

const migrations = loadMigrations();
const migration = migrations.find(item => item.version === '0007');
assert.ok(migration, 'migration 0007 is present in the ordered plan');
assert.equal(migration.name, '0007_disable_contact_list_verification.sql');
assert.equal(assertMigrationPlanAllowed(migrations), migrations, 'a plan containing 0007 is accepted');
process.stdout.write('PASS: migration 0007 is in the ordered plan and accepted by the guard.\n');
