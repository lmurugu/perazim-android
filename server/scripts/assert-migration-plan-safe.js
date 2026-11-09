#!/usr/bin/env node
'use strict';

const { loadMigrations, assertMigrationPlanAllowed } = require('../src/migrations');

try {
  assertMigrationPlanAllowed(loadMigrations());
  process.stdout.write('Migration plan passed the no-0007 execution guard.\n');
} catch (error) {
  process.stderr.write(`${error.message}\n`);
  process.exitCode = 1;
}
