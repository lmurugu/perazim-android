'use strict';

const assert = require('assert/strict');
const { validateAdminTarget, assertConnectedOwnedCluster } = require('../src/phase-b/auditConnectionGuard');

const good = 'postgres://postgres@127.0.0.1:54321/postgres';
let assertions = 0;
function ok(fn, message) { assertions++; assert.doesNotThrow(fn, message); }
function bad(value, message, expectedPort = 54321) { assertions++; assert.throws(() => validateAdminTarget(value, expectedPort), /admin|target|connection|database|parameter|loopback/i, message); }

ok(() => validateAdminTarget(good, 54321), 'owned loopback maintenance database accepted');
bad('postgres://postgres@127.0.0.1:54321/postgres?host=example.com', 'host query override rejected');
bad('postgres://postgres@127.0.0.1:54321/postgres?hostaddr=203.0.113.5', 'hostaddr override rejected');
bad('postgres://postgres@127.0.0.1:54321/postgres?host=127.0.0.1&hostaddr=203.0.113.5', 'conflicting host and hostaddr rejected');
bad('postgres://postgres@example.com:54321/postgres', 'non-loopback target rejected');
bad('postgres://postgres@127.0.0.1:54321/production', 'unexpected database rejected');
bad('postgres://postgres@127.0.0.1:54321/postgres?options=-csearch_path%3Dpublic', 'startup options rejected');
bad('postgres://postgres@127.0.0.1:54321/postgres?service=prod', 'service override rejected');
bad('postgres://postgres@127.0.0.1:54321/postgres?target_session_attrs=any', 'target session override rejected');
bad('postgres://postgres@127.0.0.1:54321/postgres#fragment', 'fragment rejected');
bad('postgres://postgres@127.0.0.1:54321/postgres', 'port mismatch rejected', 54322);
assertions++;
assert.doesNotThrow(() => assertConnectedOwnedCluster({ db: 'postgres', user: 'postgres', addr: '127.0.0.1/32', port: 54321, data_dir: '/tmp/owned-pg' }, 54321, '/tmp/owned-pg'));
assertions++;
assert.throws(() => assertConnectedOwnedCluster({ db: 'production', user: 'postgres', addr: '127.0.0.1/32', port: 54321, data_dir: '/tmp/owned-pg' }, 54321, '/tmp/owned-pg'), /identity mismatch/);
assertions++;
assert.throws(() => assertConnectedOwnedCluster({ db: 'postgres', user: 'postgres', addr: '203.0.113.10/32', port: 54321, data_dir: '/tmp/owned-pg' }, 54321, '/tmp/owned-pg'), /identity mismatch/);
assertions++;
assert.throws(() => assertConnectedOwnedCluster({ db: 'postgres', user: 'postgres', addr: '127.0.0.1/32', port: 54322, data_dir: '/tmp/owned-pg' }, 54321, '/tmp/owned-pg'), /identity mismatch/);
assertions++;
assert.throws(() => assertConnectedOwnedCluster({ db: 'postgres', user: 'postgres', addr: '127.0.0.1/32', port: 54321, data_dir: '/tmp/not-owned' }, 54321, '/tmp/owned-pg'), /identity mismatch/);
console.log(`Connection guard tests: ${assertions} assertions passed, 0 skipped`);
