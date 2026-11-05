const http = require('http');
const { app } = require('../src/index');
const { initDb, query, closeDb, isPostgresActive } = require('../src/db');
const { resetAll } = require('../src/middleware/rateLimiter');

const PORT = 3002;
process.env.PORT = PORT;

if (!process.env.DATABASE_URL) {
  process.env.DATABASE_URL = 'postgres://perazim_test:test_secret_pass_2026@127.0.0.1:5433/perazim_test';
}

if (!process.env.JWT_SECRET) {
  process.env.JWT_SECRET = 'perazim_test_jwt_secret_must_be_at_least_32_characters_long_for_security';
}

function makeRequest(method, path, body = null, token = null) {
  return new Promise((resolve, reject) => {
    const payload = body ? JSON.stringify(body) : null;
    const options = {
      hostname: '127.0.0.1',
      port: PORT,
      path: path,
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

function assert(condition, message) {
  if (!condition) {
    console.error(`❌ ASSERTION FAILED: ${message}`);
    process.exit(1);
  }
  console.log(`✅ ${message}`);
}

async function runPostgresTests() {
  console.log('\n======================================================');
  console.log('🐘 Starting Real PostgreSQL Server Verification Suite');
  console.log('======================================================\n');
  console.log(`Connecting to database: ${process.env.DATABASE_URL}`);

  resetAll();

  // 1. Initialize real PostgreSQL connection and schema
  await initDb();
  assert(isPostgresActive() === true, 'Database successfully initialized in REAL PostgreSQL mode (NOT memoryStore)');

  // Clean test tables to ensure repeatable test runs
  await query('TRUNCATE TABLE users, prayers, prayer_amens, messages, connections, sync_events CASCADE');

  // 2. Verify schema tables in PostgreSQL
  const tablesRes = await query(
    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name"
  );
  const tableNames = tablesRes.rows.map(r => r.table_name);
  console.log('Verified PostgreSQL tables:', tableNames.join(', '));
  assert(tableNames.includes('users'), 'Table "users" exists in PostgreSQL');
  assert(tableNames.includes('prayers'), 'Table "prayers" exists in PostgreSQL');
  assert(tableNames.includes('prayer_amens'), 'Table "prayer_amens" exists in PostgreSQL');
  assert(tableNames.includes('campuses'), 'Table "campuses" exists in PostgreSQL');
  assert(tableNames.includes('sync_events'), 'Table "sync_events" exists in PostgreSQL');

  const server = app.listen(PORT, '127.0.0.1');

  try {
    // 3. Health check
    const health = await makeRequest('GET', '/api/health');
    assert(health.status === 200, 'Health endpoint responds 200');

    // 4. Register Brother John in real PostgreSQL
    const regUser1 = await makeRequest('POST', '/api/auth/register', {
      phone: '0711000001',
      pin: '1234',
      name: 'Brother John (Embu)',
      campus_id: 'campus_embu'
    });
    assert(regUser1.status === 201, 'User 1 registered in real PostgreSQL table (201)');
    const token1 = regUser1.body.token;

    // 5. Test UNIQUE phone constraint in real PostgreSQL
    const duplicateReg = await makeRequest('POST', '/api/auth/register', {
      phone: '0711000001',
      pin: '9999',
      name: 'Duplicate Brother John',
      campus_id: 'campus_embu'
    });
    assert(duplicateReg.status === 409, 'PostgreSQL UNIQUE phone constraint enforced: duplicate registration rejected (409)');

    // 6. User 1 Login against bcrypt hash in PostgreSQL
    const loginUser1 = await makeRequest('POST', '/api/auth/login', {
      phone: '0711000001',
      pin: '1234'
    });
    assert(loginUser1.status === 200, 'User 1 authenticated against PostgreSQL hash (200)');
    assert(loginUser1.body.user.name === 'Brother John (Embu)', 'User profile matches PostgreSQL record');

    // 7. Register Sister Mary
    const regUser2 = await makeRequest('POST', '/api/auth/register', {
      phone: '0711000002',
      pin: '5678',
      name: 'Sister Mary (Nairobi)',
      campus_id: 'campus_nairobi'
    });
    assert(regUser2.status === 201, 'User 2 registered in PostgreSQL (201)');
    const token2 = regUser2.body.token;

    // 8. User 1 creates prayer in PostgreSQL
    const postPrayer = await makeRequest('POST', '/api/prayers', {
      title: 'Healing for Mama in Embu',
      content: 'Praying for swift recovery after surgery.',
      isAnonymous: false
    }, token1);
    assert(postPrayer.status === 201, 'Prayer inserted into PostgreSQL "prayers" table (201)');
    const prayerId = postPrayer.body.prayer.id;

    // 9. User 2 Amens prayer in PostgreSQL
    const amenRes = await makeRequest('POST', `/api/prayers/${prayerId}/amen`, {}, token2);
    assert(amenRes.status === 200, 'Amen recorded into PostgreSQL "prayer_amens" table (200)');

    // 10. Test duplicate amen idempotency in PostgreSQL composite PK
    const duplicateAmenRes = await makeRequest('POST', `/api/prayers/${prayerId}/amen`, {}, token2);
    assert(duplicateAmenRes.status === 200, 'Duplicate amen handled idempotently in PostgreSQL (200)');

    // 11. Test unauthorized testimony rejection (User 2 cannot claim User 1's prayer)
    const hackTestimony = await makeRequest('POST', `/api/prayers/${prayerId}/testimony`, {
      testimony: 'Hijacked testimony'
    }, token2);
    assert(hackTestimony.status === 403, 'PostgreSQL row ownership verified: User 2 testimony rejected with 403');

    // 12. User 1 posts testimony to own prayer
    const validTestimony = await makeRequest('POST', `/api/prayers/${prayerId}/testimony`, {
      testimony: 'Mama discharged today in perfect health!'
    }, token1);
    assert(validTestimony.status === 200, 'User 1 testimony accepted and updated in PostgreSQL (200)');

    // 13. Test Batch Sync with supported and unsupported operations
    const syncRes = await makeRequest('POST', '/api/sync', {
      mutations: [
        {
          id: 'sync_valid_pg_1',
          operationType: 'CREATE',
          entityType: 'PRAYER',
          entityId: 'prayer_pg_offline_1',
          payloadJson: JSON.stringify({
            title: 'Physical Harvest Thanksgiving',
            content: 'Thanking God for abundance',
            isAnonymous: false
          })
        },
        {
          id: 'sync_hack_testimony',
          operationType: 'UPDATE',
          entityType: 'PRAYER_TESTIMONY',
          entityId: prayerId,
          payloadJson: JSON.stringify({ testimony: 'Attacker update' })
        },
        {
          id: 'sync_unknown_op',
          operationType: 'DESTROY',
          entityType: 'UNKNOWN_ENTITY',
          entityId: 'bad_id',
          payloadJson: '{}'
        }
      ],
      lastSyncTimestamp: 0
    }, token2);
    assert(syncRes.status === 200, 'Batch sync responds 200 in PostgreSQL mode');
    assert(syncRes.body.syncedIds.includes('sync_valid_pg_1'), 'Valid offline prayer synced and saved in PostgreSQL');
    assert(!syncRes.body.syncedIds.includes('sync_hack_testimony'), 'Unauthorized sync testimony rejected and excluded from syncedIds');
    assert(!syncRes.body.syncedIds.includes('sync_unknown_op'), 'Unsupported operation rejected and excluded from syncedIds');
    assert(syncRes.body.rejectedMutations.some(r => r.id === 'sync_hack_testimony' && r.reason === 'FORBIDDEN'), 'Forbidden reason recorded in rejected list');
    assert(syncRes.body.rejectedMutations.some(r => r.id === 'sync_unknown_op' && r.reason === 'UNSUPPORTED_OPERATION'), 'Unsupported operation recorded in rejected list');

    // 14. Brute-Force Rate Limiting Test against PostgreSQL
    const bruteForcePhone = '0777000000';
    await makeRequest('POST', '/api/auth/register', {
      phone: bruteForcePhone,
      pin: '4321',
      name: 'PG Rate Limit Target',
      campus_id: 'campus_nairobi'
    });

    for (let i = 1; i <= 5; i++) {
      const fail = await makeRequest('POST', '/api/auth/login', {
        phone: bruteForcePhone,
        pin: '0000'
      });
      assert(fail.status === 401, `Failed attempt ${i} returns 401`);
    }

    const lockout = await makeRequest('POST', '/api/auth/login', {
      phone: bruteForcePhone,
      pin: '0000'
    });
    assert(lockout.status === 429, '6th failed attempt triggers HTTP 429 lockout');

    // 15. Token Revocation via POST /api/auth/logout
    const logoutRes = await makeRequest('POST', '/api/auth/logout', {}, token1);
    assert(logoutRes.status === 200, 'POST /api/auth/logout succeeds (200)');
    const revokedCheck = await makeRequest('GET', '/api/auth/me', null, token1);
    assert(revokedCheck.status === 401, 'Revoked token rejected with HTTP 401 on subsequent requests');

    // 16. Starter Metric Preservation (0 grace points never overwritten with 2)
    await query('UPDATE users SET grace_points = 0 WHERE phone = $1', ['+254711000002']);
    const loginUser2Zero = await makeRequest('POST', '/api/auth/login', {
      phone: '0711000002',
      pin: '5678'
    });
    assert(loginUser2Zero.status === 200, 'User 2 logged in (200)');
    assert(loginUser2Zero.body.user.grace_points === 0, 'Stored 0 grace points preserved on login (NOT reset to 2)');

    const meUser2Zero = await makeRequest('GET', '/api/auth/me', null, loginUser2Zero.body.token);
    assert(meUser2Zero.status === 200, 'User 2 GET /me succeeds (200)');
    assert(meUser2Zero.body.user.grace_points === 0, 'Stored 0 grace points preserved on GET /me (NOT reset to 2)');

    // 17. Server Process Restart Data Persistence
    // Shut down HTTP server process, leave PostgreSQL intact, restart HTTP server
    if (server.closeAllConnections) {
      server.closeAllConnections();
    }
    await new Promise((resolve, reject) => {
      server.close(err => (err ? reject(err) : resolve()));
    });

    const restartedServer = await new Promise((resolve, reject) => {
      const s = app.listen(PORT, '127.0.0.1', () => resolve(s));
      s.on('error', reject);
    });

    const verifyAfterRestart = await makeRequest('GET', '/api/prayers');
    assert(verifyAfterRestart.status === 200, 'Prayers endpoint responds 200 after server process restart');
    const persistedPrayer = verifyAfterRestart.body.prayers.find(p => p.id === prayerId);
    assert(persistedPrayer !== undefined, 'Prayer preserved in PostgreSQL after API process restart');
    assert(persistedPrayer.prayer_count >= 1, 'Prayer amen count preserved in PostgreSQL after API process restart');
    assert(persistedPrayer.testimony === 'Mama discharged today in perfect health!', 'Prayer testimony preserved after restart');

    await new Promise(r => restartedServer.close(r));

    console.log('\n======================================================');
    console.log('🎉 ALL 18 REAL POSTGRESQL VERIFICATION TESTS PASSED!');
    console.log('======================================================\n');
  } catch (err) {
    console.error('PostgreSQL test run failed with error:', err);
    process.exit(1);
  } finally {
    try { server.close(); } catch (e) {}
    await closeDb();
    process.exit(0);
  }
}

runPostgresTests();
