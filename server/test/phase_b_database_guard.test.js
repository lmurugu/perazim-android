'use strict';

const assert = require('node:assert/strict');
const Module = require('node:module');
const db = require('../src/db');
const { validatePhaseBTestDatabaseUrl } = db;

const VALID_V4 = 'postgres://postgres@127.0.0.1:55431/perazim_phaseb_abcdef123456_test';
const VALID_V6 = 'postgres://postgres@[::1]:55432/perazim_phaseb_abcdef123456_test';

assert.deepEqual(validatePhaseBTestDatabaseUrl(VALID_V4), {
  host: '127.0.0.1', port: 55431, database: 'perazim_phaseb_abcdef123456_test',
  user: 'postgres', password: undefined, ssl: false
});
assert.equal(validatePhaseBTestDatabaseUrl(VALID_V6).host, '::1');

for (const unsafe of [
  'postgres://postgres@127.0.0.1:55431/perazim_local',
  'postgres://postgres@192.0.2.10:55431/perazim_phaseb_abcdef123456_test',
  'postgres://postgres@localhost:55431/perazim_phaseb_abcdef123456_test',
  'postgres://postgres@127.0.0.1/perazim_phaseb_abcdef123456_test',
  `${VALID_V4}?host=192.0.2.10`,
  `${VALID_V4}?port=5432`,
  `${VALID_V4}?hostaddr=192.0.2.10`,
  `${VALID_V4}?service=untrusted`,
  `${VALID_V4}?sslmode=require`,
  `${VALID_V4}?options=-csearch_path%3Dpublic`,
  `${VALID_V4}#fragment`
]) {
  assert.throws(() => validatePhaseBTestDatabaseUrl(unsafe), /SAFETY ABORT/);
}

const priorHost = process.env.PGHOST;
const priorPort = process.env.PGPORT;
process.env.PGHOST = '192.0.2.55';
process.env.PGPORT = '5432';
assert.deepEqual(validatePhaseBTestDatabaseUrl(VALID_V4), {
  host: '127.0.0.1', port: 55431, database: 'perazim_phaseb_abcdef123456_test',
  user: 'postgres', password: undefined, ssl: false
}, 'explicit validated URL fields take precedence over PGHOST and PGPORT defaults');
if (priorHost === undefined) delete process.env.PGHOST; else process.env.PGHOST = priorHost;
if (priorPort === undefined) delete process.env.PGPORT; else process.env.PGPORT = priorPort;

async function verifyStartupPreflight() {
  const envKeys = ['DATABASE_URL', 'NODE_ENV', 'PHASE_B_SYNTHETIC', 'ALLOW_MEMORY_STORE', 'PHASE_B_TEST_EXPECTED_PORT', 'PHASE_B_TEST_EXPECTED_DATA_DIR', 'PGHOST', 'PGHOSTADDR', 'PGPORT', 'PGDATABASE', 'PGSERVICE', 'PGOPTIONS'];
  const previous = Object.fromEntries(envKeys.map(key => [key, process.env[key]]));
  const originalLoad = Module._load;
  const originalConsoleError = console.error;
  let poolConstructions = 0;
  Module._load = function (request, parent, isMain) {
    if (request === 'pg') {
      return { Pool: class MockPool { constructor() { poolConstructions++; } } };
    }
    return originalLoad.call(this, request, parent, isMain);
  };
  Object.assign(process.env, {
    NODE_ENV: 'test', PHASE_B_SYNTHETIC: 'true', ALLOW_MEMORY_STORE: 'false',
    DATABASE_URL: 'postgres://postgres@127.0.0.1:55431/perazim_local'
  });
  console.error = () => {};
  try {
    await assert.rejects(db.initDb(), /SAFETY ABORT/);
    assert.equal(poolConstructions, 0, 'unsafe URL is rejected before Pool construction or a PostgreSQL connection');
    process.env.DATABASE_URL = VALID_V4;
    await assert.rejects(db.initDb(), /expected port and disposable cluster data directory/);
    assert.equal(poolConstructions, 0, 'synthetic connection is refused before Pool construction unless runner supplies the exact cluster identity');
  } finally {
    Module._load = originalLoad;
    console.error = originalConsoleError;
    for (const key of envKeys) {
      if (previous[key] === undefined) delete process.env[key]; else process.env[key] = previous[key];
    }
  }

  let capturedPoolOptions;
  Module._load = function (request, parent, isMain) {
    if (request === 'pg') {
      return { Pool: class MockPool {
        constructor(options) { capturedPoolOptions = options; }
        async query(sql) {
          if (sql === 'SELECT 1') return { rows: [{ '?column?': 1 }] };
          if (sql.includes('current_database()')) return { rows: [{
            database: 'perazim_phaseb_abcdef123456_test', address: '127.0.0.1/32', port: 55431,
            data_directory: '/tmp'
          }] };
          return { rows: [] };
        }
        async end() {}
      } };
    }
    return originalLoad.call(this, request, parent, isMain);
  };
  const previousConfig = Object.fromEntries(envKeys.map(key => [key, process.env[key]]));
  Object.assign(process.env, {
    NODE_ENV: 'test', PHASE_B_SYNTHETIC: 'true', ALLOW_MEMORY_STORE: 'false', DATABASE_URL: VALID_V4,
    PHASE_B_TEST_EXPECTED_PORT: '55431', PHASE_B_TEST_EXPECTED_DATA_DIR: '/tmp',
    PGHOST: '192.0.2.55', PGHOSTADDR: '192.0.2.56', PGPORT: '5432', PGDATABASE: 'untrusted',
    PGSERVICE: 'untrusted', PGOPTIONS: '-c search_path=untrusted'
  });
  const priorConsole = [console.log, console.warn, console.error];
  console.log = console.warn = console.error = () => {};
  try {
    await db.initDb();
    assert.deepEqual(capturedPoolOptions, {
      host: '127.0.0.1', port: 55431, database: 'perazim_phaseb_abcdef123456_test',
      user: 'postgres', password: undefined, ssl: false
    }, 'Pool receives only explicit validated destination options, ignoring PG* environment overrides');
    await db.closeDb();
  } finally {
    try { await db.closeDb(); } catch (_) {}
    Module._load = originalLoad;
    [console.log, console.warn, console.error] = priorConsole;
    for (const key of envKeys) {
      if (previousConfig[key] === undefined) delete process.env[key]; else process.env[key] = previousConfig[key];
    }
  }
}

verifyStartupPreflight().then(() => {
  console.log('Phase B database guard passed: strict parsed destinations, query/environment override rejection, and pre-Pool startup refusal.');
}).catch(error => {
  console.error(error);
  process.exitCode = 1;
});
