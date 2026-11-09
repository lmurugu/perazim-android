'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const scripts = [
  '../scripts/test-phase-b-isolated.sh',
  '../scripts/test-sec-02-isolated.sh',
  '../scripts/test-adm-01-isolated.sh'
];
const creationCommand = /mktemp -d|\/initdb\b|\/createdb\b/;
for (const script of scripts) {
  const fullPath = path.resolve(__dirname, script);
  const lines = fs.readFileSync(fullPath, 'utf8').split(/\r?\n/);
  const guardLine = lines.findIndex(line => line.includes('assert-migration-plan-safe.js'));
  assert.notEqual(guardLine, -1, `${path.basename(script)} invokes the no-0007 plan guard`);
  const creationLine = lines.findIndex(line => !line.trim().startsWith('#') && creationCommand.test(line));
  assert.notEqual(creationLine, -1, `${path.basename(script)} contains an owned cluster/database creation point`);
  assert.ok(guardLine < creationLine, `${path.basename(script)} checks migration plan before cluster/database creation`);
}
process.stdout.write('PASS: Phase B, SEC-02 and ADM-01 runners invoke the migration 0007 guard before temporary-directory or database/cluster creation commands.\n');
