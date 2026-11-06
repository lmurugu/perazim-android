// Pure guard checks only: no app import, sockets, member accounts, or DB writes.
const assert = require('node:assert/strict');
const { assertDisposableTestDatabase: guard } = require('./test_database_safety');

assert.doesNotThrow(() => guard('postgres://test@localhost/perazim_test', 'test'));
for (const [url, mode] of [
  ['postgres://member@localhost/perazim_local', 'test'],
  ['postgres://member@localhost/perazim_prod', 'test'],
  ['postgres://member@localhost/perazim_test', 'production'],
  ['malformed', 'test']
]) {
  assert.throws(() => guard(url, mode), /SAFETY ABORT/);
}
process.env.PERAZIM_TEST_SAFE = 'true';
assert.throws(() => guard('postgres://member@localhost/perazim_local', 'test'), /SAFETY ABORT/);
console.log('Disposable test database preflight checks passed (no network or DB writes).');
