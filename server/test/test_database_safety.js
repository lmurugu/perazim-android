// Pure preflight: run before importing the app or initializing its schema.
function assertDisposableTestDatabase(connectionString, nodeEnv = process.env.NODE_ENV) {
  if (nodeEnv === 'production') {
    throw new Error('SAFETY ABORT: Test helpers cannot run in production mode.');
  }
  let database;
  try {
    database = decodeURIComponent(new URL(connectionString).pathname.slice(1));
  } catch (_) {
    throw new Error('SAFETY ABORT: Tests require a valid disposable PostgreSQL URL.');
  }
  if (!database.endsWith('_test')) {
    throw new Error('SAFETY ABORT: Tests require a disposable database ending in _test; overrides are not allowed.');
  }
}

module.exports = { assertDisposableTestDatabase };
