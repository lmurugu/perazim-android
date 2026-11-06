// This suite always uses its own transient store, never an inherited member DB.
delete process.env.DATABASE_URL;
process.env.NODE_ENV = 'test';
process.env.ALLOW_MEMORY_STORE = 'true';
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

/**
 * Redacts credentials (passwords) from database connection strings, error messages,
 * and arbitrary text before logging or outputting.
 */
function redactCredentials(input) {
  if (!input) return input;
  let text = typeof input === 'string' ? input : (input.stack || input.message || String(input));

  // 1. Redact passwords in connection URLs (e.g. postgres://user:password@host:port/db)
  text = text.replace(/([a-zA-Z][a-zA-Z0-9+.-]*:\/\/[^:]+:)([^@/]+)(@)/g, '$1***$3');

  // 2. Redact passwords in query parameters or connection strings
  text = text.replace(/([?&]password=)[^&]+/gi, '$1***');
  text = text.replace(/(password\s*=\s*['"]?)[^'"\s;]+(['"]?)/gi, '$1***$2');

  // 3. Redact active DATABASE_URL password if configured
  if (process.env.DATABASE_URL) {
    try {
      const parsed = new URL(process.env.DATABASE_URL);
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

async function runTests() {
  console.log('\n======================================================');
  console.log('🧪 Starting Perazim Server API Contract Integration Suite');
  console.log('   (Simulated Node.js HTTP clients; NOT an Android on-device test)');
  console.log('======================================================\n');

  // Verify credential redaction helper
  const testSecretUrl = 'postgres://perazim_user:secret12345@127.0.0.1:5433/perazim_test';
  assert(!redactCredentials(testSecretUrl).includes('secret12345') && redactCredentials(testSecretUrl).includes('***'), 'Database connection string password redacted cleanly in API test suite');

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

    // 7. User 2 fetches public prayers — verifies multi-client HTTP contract visibility
    const fetchPrayersRes = await makeRequest('GET', '/api/prayers');
    assert(fetchPrayersRes.status === 200, 'User 2 fetched prayer wall (200)');
    const foundPrayer = fetchPrayersRes.body.prayers.find(p => p.id === prayerId);
    assert(!!foundPrayer, 'User 2 sees User 1 prayer petition on public wall in separate client session (API contract test)');
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

    // 13. REQ-04: Batch-Sync Amen Safety (Transactional, Duplicate Amen Safe)
    const initialWall = await makeRequest('GET', '/api/prayers');
    const targetPrayer = initialWall.body.prayers.find(p => p.id === prayerId);
    const countBeforeAmen = targetPrayer.prayer_count;

    // Send first amen via batch sync
    const amenSync1 = await makeRequest('POST', '/api/sync', {
      mutations: [
        {
          id: 'sync_amen_item_1',
          operationType: 'CREATE',
          entityType: 'PRAYER_AMEN',
          entityId: prayerId,
          payloadJson: '{}'
        }
      ],
      lastSyncTimestamp: 0
    }, token1);
    assert(amenSync1.status === 200, 'REQ-04: Batch sync amen responds 200');
    assert(amenSync1.body.syncedIds.includes('sync_amen_item_1'), 'REQ-04: Amen mutation acknowledged in syncedIds');

    const wallAfterAmen1 = await makeRequest('GET', '/api/prayers');
    const prayerAfterAmen1 = wallAfterAmen1.body.prayers.find(p => p.id === prayerId);
    assert(prayerAfterAmen1.prayer_count === countBeforeAmen + 1, 'REQ-04: Prayer count incremented by 1 on first batch amen');

    // Replay duplicate amen via batch sync from same user
    const amenSyncDuplicate = await makeRequest('POST', '/api/sync', {
      mutations: [
        {
          id: 'sync_amen_item_dup',
          operationType: 'CREATE',
          entityType: 'PRAYER_AMEN',
          entityId: prayerId,
          payloadJson: '{}'
        }
      ],
      lastSyncTimestamp: 0
    }, token1);
    assert(amenSyncDuplicate.status === 200, 'REQ-04: Duplicate batch sync amen responds 200');
    assert(amenSyncDuplicate.body.syncedIds.includes('sync_amen_item_dup'), 'REQ-04: Duplicate amen mutation is acknowledged in syncedIds');

    const wallAfterAmenDup = await makeRequest('GET', '/api/prayers');
    const prayerAfterAmenDup = wallAfterAmenDup.body.prayers.find(p => p.id === prayerId);
    assert(prayerAfterAmenDup.prayer_count === countBeforeAmen + 1, 'REQ-04: Duplicate batch amen does NOT increment prayer_count');

    // 14. REQ-05: Prayer CREATE Replay Safety (Ownership-aware & Content Match)
    // 14a. Idempotent replay: same author, same content -> acknowledged in syncedIds
    const replaySameRes = await makeRequest('POST', '/api/sync', {
      mutations: [
        {
          id: 'replay_prayer_1',
          operationType: 'CREATE',
          entityType: 'PRAYER',
          entityId: 'prayer_offline_101',
          payloadJson: JSON.stringify({
            title: 'Offline Faith Petition',
            content: 'Written while offline in rural Meru',
            isAnonymous: false
          })
        }
      ]
    }, token2);
    assert(replaySameRes.status === 200, 'REQ-05: Replay same prayer responds 200');
    assert(replaySameRes.body.syncedIds.includes('replay_prayer_1'), 'REQ-05: Idempotent replay acknowledged in syncedIds');

    // 14b. Conflict: Different author claims existing prayer ID -> rejected
    const replayDiffAuthorRes = await makeRequest('POST', '/api/sync', {
      mutations: [
        {
          id: 'hijack_prayer_attempt',
          operationType: 'CREATE',
          entityType: 'PRAYER',
          entityId: 'prayer_offline_101',
          payloadJson: JSON.stringify({
            title: 'Offline Faith Petition',
            content: 'Written while offline in rural Meru',
            isAnonymous: false
          })
        }
      ]
    }, token1); // token1 (User 1) trying to recreate User 2's prayer
    assert(replayDiffAuthorRes.status === 200, 'REQ-05: Conflicting author sync responds 200');
    assert(!replayDiffAuthorRes.body.syncedIds.includes('hijack_prayer_attempt'), 'REQ-05: Hijack attempt NOT acknowledged in syncedIds');
    const authorMismatch = replayDiffAuthorRes.body.rejectedMutations.find(r => r.id === 'hijack_prayer_attempt');
    assert(!!authorMismatch, 'REQ-05: Hijack attempt in rejectedMutations');
    assert(authorMismatch.reason === 'CONFLICT_MISMATCH', 'REQ-05: Reason is CONFLICT_MISMATCH for wrong author');
    assert(authorMismatch.message === 'Prayer ID is registered to another author', 'REQ-05: Correct mismatch message');

    // 14c. Conflict: Same author, different content -> rejected
    const replayDiffContentRes = await makeRequest('POST', '/api/sync', {
      mutations: [
        {
          id: 'conflict_content_attempt',
          operationType: 'CREATE',
          entityType: 'PRAYER',
          entityId: 'prayer_offline_101',
          payloadJson: JSON.stringify({
            title: 'Offline Faith Petition',
            content: 'Conflicting content altered without authorization',
            isAnonymous: false
          })
        }
      ]
    }, token2); // token2 (same author), but different content
    assert(replayDiffContentRes.status === 200, 'REQ-05: Conflicting content sync responds 200');
    assert(!replayDiffContentRes.body.syncedIds.includes('conflict_content_attempt'), 'REQ-05: Conflicting content NOT acknowledged in syncedIds');
    const contentMismatch = replayDiffContentRes.body.rejectedMutations.find(r => r.id === 'conflict_content_attempt');
    assert(!!contentMismatch, 'REQ-05: Conflicting content in rejectedMutations');
    assert(contentMismatch.reason === 'CONFLICT_MISMATCH', 'REQ-05: Reason is CONFLICT_MISMATCH for changed content');
    assert(contentMismatch.message === 'Prayer ID already exists with conflicting content', 'REQ-05: Correct conflict message');

    // 15. REQ-06: Anonymous Privacy
    // 15a. Direct POST /api/prayers with isAnonymous: true
    const postAnonRes = await makeRequest('POST', '/api/prayers', {
      title: 'Secret Thanksgiving',
      content: 'Prayers offered anonymously for our family.',
      isAnonymous: true
    }, token1);
    assert(postAnonRes.status === 201, 'REQ-06: Anonymous prayer created (201)');
    assert(postAnonRes.body.prayer.is_anonymous === true, 'REQ-06: is_anonymous is true');
    assert(postAnonRes.body.prayer.author_id === 'anonymous', 'REQ-06: author_id masked as anonymous in POST /api/prayers response');
    const anonPrayerId = postAnonRes.body.prayer.id;

    // 15b. GET /api/prayers verification
    const getWallAnon = await makeRequest('GET', '/api/prayers');
    const fetchedAnon = getWallAnon.body.prayers.find(p => p.id === anonPrayerId);
    assert(!!fetchedAnon, 'REQ-06: Anonymous prayer visible on wall');
    assert(fetchedAnon.author_id === 'anonymous', 'REQ-06: author_id masked as anonymous in GET /api/prayers response');

    // 15c. Batch sync anonymous prayer verification
    const syncAnonRes = await makeRequest('POST', '/api/sync', {
      mutations: [
        {
          id: 'sync_anon_item',
          operationType: 'CREATE',
          entityType: 'PRAYER',
          entityId: 'prayer_anon_offline_99',
          payloadJson: JSON.stringify({
            title: 'Anonymous Offline Petition',
            content: 'Written anonymously offline',
            isAnonymous: true
          })
        }
      ],
      lastSyncTimestamp: 0
    }, token2);
    assert(syncAnonRes.status === 200, 'REQ-06: Batch sync anonymous prayer responds 200');
    assert(syncAnonRes.body.syncedIds.includes('sync_anon_item'), 'REQ-06: Anonymous offline prayer synced');
    const syncedAnonPrayer = syncAnonRes.body.prayers.find(p => p.id === 'prayer_anon_offline_99');
    assert(!!syncedAnonPrayer, 'REQ-06: Anonymous offline prayer returned in batch sync prayers');
    assert(syncedAnonPrayer.author_id === 'anonymous', 'REQ-06: author_id masked as anonymous in batch sync returned prayers');

    // 16. REQ-07: Paginated Synchronization with stable ordering (updated_at ASC, id ASC)
    // Request page 1 with limit = 2
    const page1Res = await makeRequest('POST', '/api/sync', {
      limit: 2,
      lastSyncTimestamp: 0
    });
    assert(page1Res.status === 200, 'REQ-07: Page 1 sync responds 200');
    assert(page1Res.body.prayers.length === 2, 'REQ-07: Page 1 respects batch limit of 2');
    assert(page1Res.body.hasMore === true, 'REQ-07: hasMore is true when additional items exist');
    assert(typeof page1Res.body.nextCursor === 'string' || typeof page1Res.body.nextCursor === 'number', 'REQ-07: nextCursor returned as string/number');
    assert(page1Res.body.nextCursor !== null, 'REQ-07: nextCursor is not null when hasMore is true');

    // Verify page 1 ordering (updated_at ASC, id ASC)
    const p1 = page1Res.body.prayers;
    const p1_time0 = p1[0].updated_at || p1[0].created_at;
    const p1_time1 = p1[1].updated_at || p1[1].created_at;
    assert(p1_time0 <= p1_time1, 'REQ-07: Items in page 1 ordered by updated_at ASC');

    // Request page 2 using nextCursor
    const page2Res = await makeRequest('POST', '/api/sync', {
      limit: 10,
      cursor: page1Res.body.nextCursor
    });
    assert(page2Res.status === 200, 'REQ-07: Page 2 sync responds 200');
    assert(page2Res.body.prayers.length >= 1, 'REQ-07: Page 2 returns subsequent items');
    // Ensure no duplicates between page 1 and page 2
    const page1Ids = new Set(page1Res.body.prayers.map(p => p.id));
    const overlap = page2Res.body.prayers.some(p => page1Ids.has(p.id));
    assert(!overlap, 'REQ-07: No duplicate items across paginated pages');

    // 17. REQ-01: Startup Validation & Database Fallback Policy
    const { execSync } = require('child_process');
    const path = require('path');
    const serverIndexPath = path.resolve(__dirname, '../src/index');

    try {
      execSync(`NODE_ENV=production JWT_SECRET="" node -e "require('${serverIndexPath}')"`, {
        stdio: 'pipe',
        cwd: process.cwd()
      });
      assert(false, 'Expected process to exit when booting in production without JWT_SECRET');
    } catch (e) {
      assert(e.status === 1, 'REQ-01: Production mode without JWT_SECRET immediately terminates process with exit code 1');
    }

    try {
      execSync(`DATABASE_URL="postgres://user:pass@127.0.0.1:59999/dummy" node -e "const { startServer } = require('${serverIndexPath}'); startServer();"`, {
        stdio: 'pipe',
        cwd: process.cwd()
      });
      assert(false, 'Expected startServer to terminate when DATABASE_URL fails to connect');
    } catch (e) {
      assert(e.status === 1, 'REQ-01: Failed PostgreSQL connection terminates with exit code 1 when ALLOW_MEMORY_STORE is not set');
    }

    const fallbackOutput = execSync(`PORT=3097 DATABASE_URL="postgres://user:pass@127.0.0.1:59999/dummy" ALLOW_MEMORY_STORE="true" node -e "const { startServer } = require('${serverIndexPath}'); startServer().then(s => { console.log('FALLBACK_SUCCESS'); s.close(); });"`, {
      stdio: 'pipe',
      cwd: process.cwd()
    }).toString();
    assert(fallbackOutput.includes('FALLBACK_SUCCESS'), 'REQ-01: Fallback to memory store is permitted only when ALLOW_MEMORY_STORE=true is set');

    // 18. REQ-02: Phone Validation & Token Revocation
    // Phone validation
    const invalidPhoneRes1 = await makeRequest('POST', '/api/auth/register', {
      phone: '0201234567',
      pin: '1234',
      name: 'Invalid Phone User'
    });
    assert(invalidPhoneRes1.status === 400, 'REQ-02: Invalid phone prefix (020) rejected with HTTP 400');

    const invalidPhoneRes2 = await makeRequest('POST', '/api/auth/register', {
      phone: '12345',
      pin: '1234',
      name: 'Short Phone User'
    });
    assert(invalidPhoneRes2.status === 400, 'REQ-02: Short phone number (12345) rejected with HTTP 400');

    const invalidPhoneLogin = await makeRequest('POST', '/api/auth/login', {
      phone: '+1234567890',
      pin: '1234'
    });
    assert(invalidPhoneLogin.status === 400, 'REQ-02: Non-Kenyan phone login rejected with HTTP 400');

    const valid01Reg = await makeRequest('POST', '/api/auth/register', {
      phone: '0111000003',
      pin: '1234',
      name: 'Kenyan 01 Prefix User',
      campus_id: 'campus_meru'
    });
    assert(valid01Reg.status === 201, 'REQ-02: Valid 10-digit 01... Kenyan phone accepted for registration (201)');

    // Token Revocation via POST /api/auth/logout
    const logoutRes = await makeRequest('POST', '/api/auth/logout', {}, token1);
    assert(logoutRes.status === 200, 'REQ-02: POST /api/auth/logout succeeds (200)');
    assert(!!logoutRes.body.revokedTokenHash, 'REQ-02: Logout returns SHA-256 revokedTokenHash');

    const meAfterRevocation = await makeRequest('GET', '/api/auth/me', null, token1);
    assert(meAfterRevocation.status === 401, 'REQ-02: Revoked token rejected with HTTP 401 on subsequent requests');
    assert(meAfterRevocation.body.error.includes('revoked'), 'REQ-02: Revoked token error message confirmed');

    // 19. REQ-03: Stored-Zero Metric Preservation & Defaults
    const zeroGraceUser = await makeRequest('POST', '/api/auth/register', {
      phone: '0711000004',
      pin: '4321',
      name: 'Zero Grace Point Brother',
      campus_id: 'campus_embu',
      grace_points: 999
    });
    assert(zeroGraceUser.status === 201, 'REQ-03: User registered with server-owned starting allowance (201)');
    assert(zeroGraceUser.body.user.grace_points === 0, 'REQ-03: New account starts at 0 even when client requests 999 points');

    const zeroLogin = await makeRequest('POST', '/api/auth/login', {
      phone: '0711000004',
      pin: '4321'
    });
    assert(zeroLogin.status === 200, 'REQ-03: Zero grace user logged in (200)');
    assert(zeroLogin.body.user.grace_points === 0, 'REQ-03: Stored 0 grace points preserved on login (NOT reset to 2)');

    const zeroMe = await makeRequest('GET', '/api/auth/me', null, zeroLogin.body.token);
    assert(zeroMe.status === 200, 'REQ-03: Zero grace user GET /me succeeds (200)');
    assert(zeroMe.body.user.grace_points === 0, 'REQ-03: Stored 0 grace points preserved on GET /me (NOT reset to 2)');

    console.log('\n======================================================');
    console.log('🎉 ALL 19 SERVER API CONTRACT INTEGRATION TESTS PASSED SUCCESSFULLY!');
    console.log('======================================================\n');
  } catch (err) {
    console.error('Test run failed with error:', redactCredentials(err));
    if (server) {
      if (server.closeAllConnections) {
        server.closeAllConnections();
      }
      server.close();
    }
    process.exit(1);
  } finally {
    if (server) {
      if (server.closeAllConnections) {
        server.closeAllConnections();
      }
      server.close();
    }
    process.exit(0);
  }
}

runTests();
