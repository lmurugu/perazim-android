const http = require('http');
const { app, startServer } = require('../src/index');

const PORT = 3001;
process.env.PORT = PORT;

function makeRequest(method, path, body = null, token = null) {
  return new Promise((resolve, reject) => {
    const payload = body ? JSON.stringify(body) : null;
    const options = {
      hostname: '127.0.0.1',
      port: PORT,
      path: path,
      method: method,
      headers: {
        'Content-Type': 'application/json'
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

async function runTests() {
  console.log('\n======================================================');
  console.log('🧪 Starting Perazim Server Automated Verification Suite');
  console.log('======================================================\n');

  const server = app.listen(PORT, '127.0.0.1');

  try {
    // 1. Health check
    const health = await makeRequest('GET', '/api/health');
    assert(health.status === 200, 'Health endpoint responds 200');
    assert(health.body.status === 'healthy', 'Server reports healthy status');

    // 2. Campuses
    const campusesRes = await makeRequest('GET', '/api/campuses');
    assert(campusesRes.status === 200, 'Campuses endpoint responds 200');
    assert(Array.isArray(campusesRes.body.campuses) && campusesRes.body.campuses.length >= 4, 'All 4 campuses present');

    // 3. User 1 Registration (Phone + PIN)
    const regUser1 = await makeRequest('POST', '/api/auth/register', {
      phone: '0711000001',
      pin: '1234',
      name: 'Brother John (Embu)',
      campus_id: 'campus_embu'
    });
    assert(regUser1.status === 201, 'User 1 registered with phone + PIN (201)');
    assert(!!regUser1.body.token, 'User 1 received valid JWT auth token');
    const token1 = regUser1.body.token;
    const user1Id = regUser1.body.user.id;

    // 4. User 1 Login
    const loginUser1 = await makeRequest('POST', '/api/auth/login', {
      phone: '0711000001',
      pin: '1234'
    });
    assert(loginUser1.status === 200, 'User 1 logged in successfully with PIN (200)');
    assert(loginUser1.body.user.name === 'Brother John (Embu)', 'User profile verified');

    // 5. User 2 Registration
    const regUser2 = await makeRequest('POST', '/api/auth/register', {
      phone: '0711000002',
      pin: '5678',
      name: 'Sister Mary (Nairobi)',
      campus_id: 'campus_nairobi'
    });
    assert(regUser2.status === 201, 'User 2 registered with phone + PIN (201)');
    const token2 = regUser2.body.token;

    // 6. User 1 posts prayer petition
    const postPrayerRes = await makeRequest('POST', '/api/prayers', {
      title: 'Prayer for healing in Embu',
      content: 'Please stand with our family in prayer for breakthrough and healing.',
      isAnonymous: false
    }, token1);
    assert(postPrayerRes.status === 201, 'User 1 submitted prayer petition (201)');
    const prayerId = postPrayerRes.body.prayer.id;
    assert(postPrayerRes.body.prayer.author_name === 'Brother John (Embu)', 'Prayer author correctly attributed');

    // 7. User 2 fetches public prayers — verifies multi-device visibility!
    const fetchPrayersRes = await makeRequest('GET', '/api/prayers');
    assert(fetchPrayersRes.status === 200, 'User 2 fetched prayer wall (200)');
    const foundPrayer = fetchPrayersRes.body.prayers.find(p => p.id === prayerId);
    assert(!!foundPrayer, 'User 2 sees User 1 prayer petition on wall across devices');
    assert(foundPrayer.prayer_count === 0, 'Initial prayer count is 0');

    // 8. User 2 amens User 1's prayer
    const amenRes = await makeRequest('POST', `/api/prayers/${prayerId}/amen`, {}, token2);
    assert(amenRes.status === 200, 'User 2 amen recorded (200)');

    const fetchAfterAmen = await makeRequest('GET', '/api/prayers');
    const amenedPrayer = fetchAfterAmen.body.prayers.find(p => p.id === prayerId);
    assert(amenedPrayer.prayer_count === 1, 'Prayer count incremented to 1');

    // 9. Unauthorized Testimony Check (User 2 attempts to claim User 1's prayer)
    const unauthorizedTestimonyRes = await makeRequest('POST', `/api/prayers/${prayerId}/testimony`, {
      testimony: 'Unauthorized attempt to alter prayer'
    }, token2);
    assert(unauthorizedTestimonyRes.status === 403, 'Unauthorized user testimony rejected with 403 Forbidden');

    // 10. Legitimate Author Testimony (User 1 posts testimony)
    const testimonyRes = await makeRequest('POST', `/api/prayers/${prayerId}/testimony`, {
      testimony: 'Praise God, full healing received!'
    }, token1);
    assert(testimonyRes.status === 200, 'Original prayer author testimony accepted (200)');

    // 11. Batch Offline Sync Dispatch with Legitimate and Unsupported Mutations
    const syncRes = await makeRequest('POST', '/api/sync', {
      mutations: [
        {
          id: 'sync_item_1',
          operationType: 'CREATE',
          entityType: 'PRAYER',
          entityId: 'prayer_offline_101',
          payloadJson: JSON.stringify({
            title: 'Offline Faith Petition',
            content: 'Written while offline in rural Meru',
            isAnonymous: false
          })
        },
        {
          id: 'sync_item_unsupported',
          operationType: 'DELETE',
          entityType: 'UNKNOWN_ENTITY',
          entityId: 'unknown_999',
          payloadJson: '{}'
        },
        {
          id: 'sync_item_unauthorized_testimony',
          operationType: 'UPDATE',
          entityType: 'PRAYER_TESTIMONY',
          entityId: prayerId,
          payloadJson: JSON.stringify({ testimony: 'Stolen testimony' })
        }
      ],
      lastSyncTimestamp: 0
    }, token2);
    assert(syncRes.status === 200, 'Batch sync endpoint responds 200');
    assert(syncRes.body.syncedIds.includes('sync_item_1'), 'Supported offline prayer acknowledged in syncedIds');
    assert(!syncRes.body.syncedIds.includes('sync_item_unsupported'), 'Unsupported mutation NOT acknowledged in syncedIds');
    assert(!syncRes.body.syncedIds.includes('sync_item_unauthorized_testimony'), 'Unauthorized sync testimony NOT acknowledged in syncedIds');
    assert(Array.isArray(syncRes.body.rejectedMutations) && syncRes.body.rejectedMutations.length >= 2, 'Rejected mutations list returned');
    assert(syncRes.body.prayers.some(p => p.id === 'prayer_offline_101'), 'Offline prayer replicated to server wall');

    // 12. Brute-Force Rate Limiting Test (5 failed attempts trigger 429 lockout)
    const bruteForcePhone = '0788000000';
    // Register account to test wrong PIN attempts
    await makeRequest('POST', '/api/auth/register', {
      phone: bruteForcePhone,
      pin: '1234',
      name: 'Rate Limit Target',
      campus_id: 'campus_embu'
    });

    for (let i = 1; i <= 5; i++) {
      const failRes = await makeRequest('POST', '/api/auth/login', {
        phone: bruteForcePhone,
        pin: '9999'
      });
      assert(failRes.status === 401, `Failed attempt ${i} returns 401 Incorrect PIN`);
    }

    const lockedRes = await makeRequest('POST', '/api/auth/login', {
      phone: bruteForcePhone,
      pin: '9999'
    });
    assert(lockedRes.status === 429, '6th failed attempt triggers HTTP 429 Too Many Requests rate limit');
    assert(lockedRes.body.error.includes('Too many failed login attempts'), 'Rate limit error message returned');

    console.log('\n======================================================');
    console.log('🎉 ALL 12 SERVER VERIFICATION TESTS PASSED SUCCESSFULLY!');
    console.log('======================================================\n');
  } catch (err) {
    console.error('Test run failed with error:', err);
    process.exit(1);
  } finally {
    server.close();
    process.exit(0);
  }
}

runTests();
