/**
 * Genuine Node.js API Process Restart & PostgreSQL Persistence Test Suite
 *
 * Requirements: REQ-14, REQ-13, REQ-17
 *
 * Verifies true durability across OS process lifetimes:
 * 1. Spawns Child Process 1 running `node src/index.js` on port 3099 against PostgreSQL.
 * 2. Writes member account, prayer petition, and amen intercession.
 * 3. Terminates Child Process 1 with SIGTERM, waits for process death, verifies port 3099 closed.
 * 4. Spawns Child Process 2 pointing to the SAME port 3099 and SAME PostgreSQL database.
 * 5. Verifies data survived process death intact in PostgreSQL (auth, prayers, amens).
 * 6. Cleanly terminates Child Process 2.
 */

const { assertDisposableTestDatabase } = require('./test_database_safety');
if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL must target a disposable test database');
assertDisposableTestDatabase(process.env.DATABASE_URL);
const crypto = require('crypto');
const http = require('http');
const path = require('path');
const { spawn } = require('child_process');
const { Pool } = require('pg');

const PORT = 3099;
const DATABASE_URL = process.env.DATABASE_URL;
const JWT_SECRET = process.env.JWT_SECRET || crypto.randomBytes(32).toString('hex');
const SERVER_DIR = path.resolve(__dirname, '..');
const TEST_PHONE = `+2547${crypto.randomInt(0, 100000000).toString().padStart(8, '0')}`;
const TEST_NAME = 'Synthetic Process Restart Account';
const TEST_PRAYER_TITLE = 'Synthetic process restart persistence check';

/**
 * Redacts credentials (passwords) from database connection strings, error messages,
 * and arbitrary text before logging or outputting.
 */
function redactCredentials(input) {
  if (!input) return input;
  let text = typeof input === 'string' ? input : (input.stack || input.message || String(input));

  // 1. Redact passwords from database connection URLs.
  text = text.replace(/([a-zA-Z][a-zA-Z0-9+.-]*:\/\/[^:]+:)([^@/]+)(@)/g, '$1***$3');

  // 2. Redact passwords in query parameters or connection strings
  text = text.replace(/([?&]password=)[^&]+/gi, '$1***');
  text = text.replace(/(password\s*=\s*['"]?)[^'"\s;]+(['"]?)/gi, '$1***$2');

  // 3. Redact active DATABASE_URL password if configured
  if (DATABASE_URL) {
    try {
      const parsed = new URL(DATABASE_URL);
      if (parsed.password && parsed.password.length > 0) {
        text = text.split(parsed.password).join('***');
      }
    } catch (_) {}
  }

  return text;
}

function assert(condition, message) {
  if (!condition) {
    console.error(`❌ ASSERTION FAILED: ${redactCredentials(message)}`);
    process.exit(1);
  }
  console.log(`✅ ${redactCredentials(message)}`);
}

function makeRequest(method, reqPath, body = null, token = null) {
  return new Promise((resolve, reject) => {
    const payload = body ? JSON.stringify(body) : null;
    const options = {
      hostname: '127.0.0.1',
      port: PORT,
      path: reqPath,
      method: method,
      agent: false,
      headers: {
        'Content-Type': 'application/json',
        'Connection': 'close'
      }
    };

    if (payload) {
      options.headers['Content-Length'] = Buffer.byteLength(payload);
    }
    if (token) {
      options.headers['Authorization'] = `Bearer ${token}`;
    }

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        let json = null;
        try {
          json = data ? JSON.parse(data) : {};
        } catch (e) {
          json = { raw: data };
        }
        resolve({ status: res.statusCode, body: json });
      });
    });

    req.on('error', reject);
    if (payload) {
      req.write(payload);
    }
    req.end();
  });
}

async function waitForServerReady(timeoutMs = 15000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    try {
      const res = await makeRequest('GET', '/api/health');
      if (res.status === 200) {
        return true;
      }
    } catch (e) {
      // Server not ready yet, continue polling
    }
    await new Promise(r => setTimeout(r, 100));
  }
  throw new Error(`Timeout waiting for API server to become ready on port ${PORT}`);
}

async function verifyPortClosed(timeoutMs = 5000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    try {
      await makeRequest('GET', '/api/health');
      // If request succeeds, port is still open; wait
      await new Promise(r => setTimeout(r, 100));
    } catch (e) {
      if (e.code === 'ECONNREFUSED' || e.code === 'ECONNRESET') {
        return true;
      }
    }
  }
  // Try one last immediate probe
  try {
    await makeRequest('GET', '/api/health');
    return false;
  } catch (e) {
    return true;
  }
}

function spawnServerProcess(processLabel = 'Process') {
  const child = spawn('node', ['src/index.js'], {
    cwd: SERVER_DIR,
    env: {
      ...process.env,
      PORT: String(PORT),
      DATABASE_URL: DATABASE_URL,
      JWT_SECRET: JWT_SECRET
    },
    stdio: ['ignore', 'pipe', 'pipe']
  });

  child.stdout.on('data', (d) => {
    // Optionally log child output if debugging needed
  });

  child.stderr.on('data', (d) => {
    const errText = redactCredentials(d.toString());
    if (errText.includes('FATAL') || errText.includes('Error')) {
      console.error(`[${processLabel} STDERR]`, errText.trim());
    }
  });

  return child;
}

function terminateChildProcess(child, timeoutMs = 5000) {
  return new Promise((resolve, reject) => {
    if (child.exitCode !== null || child.killed) {
      return resolve({ code: child.exitCode, signal: child.signalCode });
    }

    let forceKillTimer = null;

    const onExit = (code, signal) => {
      if (forceKillTimer) clearTimeout(forceKillTimer);
      resolve({ code, signal });
    };

    child.once('exit', onExit);

    forceKillTimer = setTimeout(() => {
      console.warn(`⚠️ Child process PID ${child.pid} did not exit cleanly within ${timeoutMs}ms; sending SIGKILL`);
      try { child.kill('SIGKILL'); } catch (e) {}
    }, timeoutMs);

    child.kill('SIGTERM');
  });
}

function isDatabaseSafeForTruncate(dbName, isExplicitlySafe = false) {
  if (!dbName || typeof dbName !== 'string') return false;
  return dbName.endsWith('_test');
}

async function prepareTestDatabase() {
  const pool = new Pool({ connectionString: DATABASE_URL });
  try {
    const dbRes = await pool.query('SELECT current_database() AS db_name');
    const activeDb = dbRes.rows[0]?.db_name;
    const isExplicitlySafe = process.env.PERAZIM_TEST_SAFE === 'true';

    if (!isDatabaseSafeForTruncate(activeDb, isExplicitlySafe)) {
      throw new Error(
        `SAFETY ABORT: Refusing to run tests on non-test database "${activeDb}". ` +
        `Target database name must end with "_test"; overrides are not allowed.`
      );
    }

    console.log(`🔒 Truncation safety check passed for database "${activeDb}" (is_test_db=${activeDb.endsWith('_test')})`);

    // Clean up specific restart test phone numbers to guarantee clean test slate
    await pool.query('DELETE FROM users WHERE phone = $1', [TEST_PHONE]);
    await pool.query('DELETE FROM prayers WHERE title = $1', [TEST_PRAYER_TITLE]);
    const notice = await pool.query(`SELECT version,content_hash FROM privacy_notice_versions
      WHERE is_active=TRUE ORDER BY created_at DESC LIMIT 1`);
    if (!notice.rows[0]) throw new Error('Synthetic restart database has no current privacy notice fixture.');
    return notice.rows[0];
  } finally {
    await pool.end();
  }
}

async function runProcessRestartTest() {
  console.log('\n========================================================================');
  console.log('🔄 STARTING GENUINE API PROCESS RESTART & POSTGRESQL PERSISTENCE TEST');
  console.log('   (Child process termination, port check & resurrection against PostgreSQL)');
  console.log('========================================================================\n');
  console.log(`Target Database: ${redactCredentials(DATABASE_URL)}`);
  console.log(`Port: ${PORT}`);

  let child1 = null;
  let child2 = null;

  try {
    // 1. Prepare clean test state with safety check
    const registrationNotice = await prepareTestDatabase();

    // ========================================================================
    // STAGE 1: Spawn Child Process 1
    // ========================================================================
    console.log('\n--- STAGE 1: Spawn Child Process 1 ---');
    child1 = spawnServerProcess('Process-1');
    console.log(`Spawned Child Process 1 (PID: ${child1.pid})`);

    await waitForServerReady();
    console.log(`Child Process 1 is healthy and responding on port ${PORT}`);

    // Register test user
    const testPhone = TEST_PHONE;
    const testPin = '1234';
    const regRes = await makeRequest('POST', '/api/auth/register', {
      phone: testPhone,
      pin: testPin,
      name: TEST_NAME,
      campus_id: 'campus_embu',
      privacy_notice_version: registrationNotice.version,
      privacy_notice_hash: registrationNotice.content_hash,
      privacy_notice_acknowledged: true
    });
    assert(regRes.status === 201, 'User registered in Process 1 (201)');
    const token = regRes.body.token;
    const userId = regRes.body.user.id;
    assert(!!token, 'Received valid JWT token from Process 1');

    // Submit prayer petition
    const prayerTitle = TEST_PRAYER_TITLE;
    const prayerContent = 'Synthetic prayer fixture used to verify process restart persistence.';
    const postPrayer = await makeRequest('POST', '/api/prayers', {
      title: prayerTitle,
      content: prayerContent,
      isAnonymous: false
    }, token);
    assert(postPrayer.status === 201, 'Prayer petition inserted into PostgreSQL via Process 1 (201)');
    const prayerId = postPrayer.body.prayer.id;
    assert(!!prayerId, 'Prayer created with valid ID');

    // Amen the prayer
    const amenRes = await makeRequest('POST', `/api/prayers/${prayerId}/amen`, {}, token);
    assert(amenRes.status === 200, 'Amen recorded via Process 1 (200)');

    // Verify initial prayer state on Process 1
    const verifyInitial = await makeRequest('GET', '/api/prayers');
    assert(verifyInitial.status === 200, 'GET /api/prayers responds 200 on Process 1');
    const initialPrayer = verifyInitial.body.prayers.find(p => p.id === prayerId);
    assert(initialPrayer !== undefined, 'Prayer found on prayer wall in Process 1');
    assert(initialPrayer.prayer_count === 1, 'Initial prayer amen count is 1 in Process 1');

    // ========================================================================
    // STAGE 2: Hard Process Termination (SIGTERM Child Process 1)
    // ========================================================================
    console.log('\n--- STAGE 2: Terminate Child Process 1 ---');
    console.log(`Sending SIGTERM to Child Process 1 (PID: ${child1.pid})...`);
    const terminationResult = await terminateChildProcess(child1);
    console.log(`Child Process 1 terminated (exit code: ${terminationResult.code}, signal: ${terminationResult.signal})`);

    const portClosed = await verifyPortClosed();
    assert(portClosed === true, `Port ${PORT} confirmed closed (ECONNREFUSED) after Process 1 termination`);

    // ========================================================================
    // STAGE 3: Spawn Child Process 2 (Resurrection)
    // ========================================================================
    console.log('\n--- STAGE 3: Spawn Child Process 2 (Resurrection) ---');
    child2 = spawnServerProcess('Process-2');
    console.log(`Spawned Child Process 2 (PID: ${child2.pid}) pointing to SAME port ${PORT} & PostgreSQL`);

    await waitForServerReady();
    console.log(`Child Process 2 is healthy and responding on port ${PORT}`);

    // ========================================================================
    // STAGE 4: Verify Data Persistence across Process Lifetimes
    // ========================================================================
    console.log('\n--- STAGE 4: Verify PostgreSQL Data Persistence in Process 2 ---');

    // 1. Authenticate user against newly resurrected Process 2
    const loginRes = await makeRequest('POST', '/api/auth/login', {
      phone: testPhone,
      pin: testPin
    });
    assert(loginRes.status === 200, 'User successfully authenticated against Process 2 (200)');
    assert(loginRes.body.user.id === userId, 'User ID matches record stored before process restart');
    assert(loginRes.body.user.name === TEST_NAME, 'User profile name matches record stored before restart');
    const newToken = loginRes.body.token;

    // 2. Fetch prayers from Process 2 and verify petition & amen survived restart
    const verifyRestart = await makeRequest('GET', '/api/prayers');
    assert(verifyRestart.status === 200, 'GET /api/prayers responds 200 on Process 2');
    const persistedPrayer = verifyRestart.body.prayers.find(p => p.id === prayerId);
    assert(persistedPrayer !== undefined, 'Prayer petition survived hard Node.js process restart intact in PostgreSQL');
    assert(persistedPrayer.title === prayerTitle, 'Prayer title preserved across process restart');
    assert(persistedPrayer.content === prayerContent, 'Prayer content preserved across process restart');
    assert(persistedPrayer.author_name === TEST_NAME, 'Prayer author attribution preserved across restart');
    assert(persistedPrayer.prayer_count >= 1, 'Prayer amen count (>= 1) preserved in PostgreSQL across process restart');

    // 3. Post author testimony via Process 2 to prove write capabilities on resurrected process
    const testimonyRes = await makeRequest('POST', `/api/prayers/${prayerId}/testimony`, {
      testimony: 'Verified: God preserved our petition through the fire of process restart!'
    }, newToken);
    assert(testimonyRes.status === 200, 'Process 2 accepts new writes to pre-existing PostgreSQL records (200)');

    const verifyAfterTestimony = await makeRequest('GET', '/api/prayers');
    const updatedPrayer = verifyAfterTestimony.body.prayers.find(p => p.id === prayerId);
    assert(updatedPrayer.testimony.includes('Verified: God preserved'), 'Testimony verified on prayer record in Process 2');

    // ========================================================================
    // STAGE 5: Clean Exit
    // ========================================================================
    console.log('\n--- STAGE 5: Clean Teardown ---');
    console.log(`Terminating Child Process 2 (PID: ${child2.pid})...`);
    await terminateChildProcess(child2);
    console.log('Child Process 2 cleanly terminated.');

    console.log('\n========================================================================');
    console.log('🎉 ALL PROCESS RESTART & POSTGRESQL PERSISTENCE CHECKS PASSED!');
    console.log('========================================================================\n');
    process.exit(0);

  } catch (err) {
    console.error('Process restart test failed with error:', redactCredentials(err));
    if (child1) { try { child1.kill('SIGKILL'); } catch (e) {} }
    if (child2) { try { child2.kill('SIGKILL'); } catch (e) {} }
    process.exit(1);
  }
}

runProcessRestartTest();
