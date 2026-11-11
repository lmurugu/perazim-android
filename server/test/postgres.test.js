const { assertDisposableTestDatabase } = require('./test_database_safety');
if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL must target a disposable test database');
assertDisposableTestDatabase(process.env.DATABASE_URL);
const http = require('http');
const crypto = require('crypto');
const { app } = require('../src/index');
const { initDb, query, closeDb, isPostgresActive } = require('../src/db');
const { resetAll } = require('../src/middleware/rateLimiter');

const PORT = 3002;
process.env.PORT = PORT;


if (!process.env.JWT_SECRET) {
  process.env.JWT_SECRET = crypto.randomBytes(32).toString('hex');
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
    console.error(`❌ ASSERTION FAILED: ${redactCredentials(message)}`);
    throw new Error(`Assertion failed: ${redactCredentials(message)}`);
  }
  console.log(`✅ ${redactCredentials(message)}`);
}

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

const redactConnectionString = redactCredentials;

function getDatabaseName(connStr) {
  if (!connStr) return '';
  try {
    const parsed = new URL(connStr);
    return parsed.pathname.replace(/^\//, '');
  } catch (e) {
    const match = connStr.match(/\/([^/?#]+)(?:\?.*)?$/);
    return match ? match[1] : '';
  }
}

function isDatabaseSafeForTruncate(dbName, isExplicitlySafe = false) {
  if (!dbName || typeof dbName !== 'string') return false;
  return dbName.endsWith('_test');
}

function assertDatabaseSafe(dbName, isExplicitlySafe = false) {
  if (!isDatabaseSafeForTruncate(dbName, isExplicitlySafe)) {
    throw new Error(
      `SAFETY ABORT: Refusing to truncate tables on non-test database "${dbName}". ` +
      `Target database name must end with "_test"; overrides are not allowed.`
    );
  }
  return true;
}

async function verifySafeTestEnvironment() {
  const currentDbRes = await query('SELECT current_database() AS db_name');
  const activeDb = currentDbRes.rows[0]?.db_name || getDatabaseName(process.env.DATABASE_URL);
  const isExplicitlySafe = process.env.PERAZIM_TEST_SAFE === 'true';

  assertDatabaseSafe(activeDb, isExplicitlySafe);

  console.log(`🔒 Truncation safety check passed for database "${activeDb}" (is_test_db=${activeDb.endsWith('_test')}, PERAZIM_TEST_SAFE=${isExplicitlySafe})`);
  return activeDb;
}

async function safeTruncate(tablesSql = 'users, prayers, prayer_amens, messages, connections, sync_events CASCADE') {
  await verifySafeTestEnvironment();
  return query(`TRUNCATE TABLE ${tablesSql}`);
}

async function runPostgresTests() {
  console.log('\n======================================================');
  console.log('🐘 Starting PostgreSQL API Contract Integration Test Suite');
  console.log('   (Node.js HTTP & PostgreSQL integration; NOT Android on-device)');
  console.log('======================================================\n');
  console.log(`Connecting to database: ${redactCredentials(process.env.DATABASE_URL)}`);

  resetAll();

  // --- SAFE TEST ENVIRONMENT GUARDS VERIFICATION (REQ-13, REQ-17) ---
  const samplePassword = crypto.randomBytes(24).toString('hex');
  console.log('--- Verifying Safe Test Environment Guards ---');
  // 1. Password Redaction in URL with username:password
  const sampleUrl = ['postgres://', 'perazim_user', ':', encodeURIComponent(samplePassword), '@127.0.0.1:5433/perazim_test'].join('');
  const redactedSample = redactCredentials(sampleUrl);
  assert(!redactedSample.includes(samplePassword) && redactedSample.includes('***'), 'Database connection string password redacted cleanly');

  // 2. Password Redaction in query parameters
  const sampleUrlWithParams = new URL('postgres://perazim_user@127.0.0.1:5433/perazim_test');
  sampleUrlWithParams.searchParams.set('pass' + 'word', samplePassword);
  const redactedParams = redactCredentials(sampleUrlWithParams.toString());
  assert(!redactedParams.includes(samplePassword) && redactedParams.includes('***'), 'Query parameter password redacted cleanly');

  // 3. Password Redaction in Error message strings
  const errorWithUrl = 'Error: connect ECONNREFUSED ' + ['postgres://', 'admin', ':', encodeURIComponent(samplePassword), '@localhost:5432/perazim_test'].join('');
  assert(!redactCredentials(errorWithUrl).includes(samplePassword), 'Error message credentials redacted cleanly');

  // 4. Safe Database Name Truncation Checks
  assert(isDatabaseSafeForTruncate('perazim_test', false) === true, 'Database ending in _test is approved for truncation without override');
  assert(isDatabaseSafeForTruncate('perazim_church', false) === false, 'Production database name is rejected for truncation');
  assert(isDatabaseSafeForTruncate('perazim_prod', false) === false, 'Production database without _test suffix is rejected for truncation');
  assert(isDatabaseSafeForTruncate('perazim_church', true) === false, 'PERAZIM_TEST_SAFE cannot bypass member database protection');
  assert(isDatabaseSafeForTruncate('', false) === false, 'Empty database name is rejected for truncation');
  assert(isDatabaseSafeForTruncate(null, false) === false, 'Null database name is rejected for truncation');

  // 5. Destructive Abort Verification
  let caughtAbort = false;
  try {
    assertDatabaseSafe('perazim_prod', false);
  } catch (err) {
    caughtAbort = true;
    assert(err.message.includes('SAFETY ABORT'), 'Safety abort error raised on attempt to truncate production database');
  }
  assert(caughtAbort, 'Verification confirmed: TRUNCATE strictly aborts on non-test database without PERAZIM_TEST_SAFE');

  // 1. Initialize real PostgreSQL connection and schema
  await initDb();
  assert(isPostgresActive() === true, 'Database successfully initialized in REAL PostgreSQL mode (NOT memoryStore)');

  // The isolated runner supplies a brand-new database, so do not truncate
  // through append-only acknowledgement history using CASCADE.
  assert((await query('SELECT COUNT(*)::int AS count FROM users')).rows[0].count === 0,
    'Fresh isolated PostgreSQL fixture starts without user data');

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
    const { getCurrentNotice } = require('../src/phase-b/privacyNotice');
    const notice = await getCurrentNotice();
    const registrationNotice = {
      privacy_notice_version: notice.version,
      privacy_notice_hash: notice.content_hash,
      privacy_notice_acknowledged: true
    };
    // 3. Health check
    const health = await makeRequest('GET', '/api/health');
    assert(health.status === 200, 'Health endpoint responds 200');

    // 4. Register Brother John in real PostgreSQL
    const regUser1 = await makeRequest('POST', '/api/auth/register', {
      phone: '0711000001',
      pin: '1234',
      name: 'Brother John (Embu)',
      campus_id: 'campus_embu',
      grace_points: 999,
      ...registrationNotice
    });
    assert(regUser1.body.user.grace_points === 0, 'PostgreSQL registration ignores client-granted points');
    assert((await query('SELECT grace_points FROM users WHERE id = $1', [regUser1.body.user.id])).rows[0].grace_points === 0, 'PostgreSQL persists starting allowance 0');
    assert(regUser1.status === 201, 'User 1 registered in real PostgreSQL table (201)');
    const token1 = regUser1.body.token;

    // 5. Test UNIQUE phone constraint in real PostgreSQL
    const duplicateReg = await makeRequest('POST', '/api/auth/register', {
      phone: '0711000001',
      pin: '9999',
      name: 'Duplicate Brother John',
      campus_id: 'campus_embu',
      ...registrationNotice
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
      campus_id: 'campus_nairobi',
      ...registrationNotice
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

    // --- REQ-04: Batch-Sync Amen Safety in PostgreSQL ---
    const pgCountBefore = (await makeRequest('GET', '/api/prayers')).body.prayers.find(p => p.id === prayerId).prayer_count;
    const pgAmenSync1 = await makeRequest('POST', '/api/sync', {
      mutations: [{ id: 'pg_sync_amen_1', operationType: 'CREATE', entityType: 'PRAYER_AMEN', entityId: prayerId, payloadJson: '{}' }]
    }, token1);
    assert(pgAmenSync1.status === 200, 'PostgreSQL REQ-04: Batch amen responds 200');
    assert(pgAmenSync1.body.syncedIds.includes('pg_sync_amen_1'), 'PostgreSQL REQ-04: Amen mutation acknowledged in syncedIds');
    const pgCountAfter1 = (await makeRequest('GET', '/api/prayers')).body.prayers.find(p => p.id === prayerId).prayer_count;
    assert(pgCountAfter1 === pgCountBefore + 1, 'PostgreSQL REQ-04: Prayer count incremented by 1');

    // Duplicate amen in PostgreSQL mode
    const pgAmenSyncDup = await makeRequest('POST', '/api/sync', {
      mutations: [{ id: 'pg_sync_amen_dup', operationType: 'CREATE', entityType: 'PRAYER_AMEN', entityId: prayerId, payloadJson: '{}' }]
    }, token1);
    assert(pgAmenSyncDup.status === 200, 'PostgreSQL REQ-04: Duplicate amen responds 200');
    assert(pgAmenSyncDup.body.syncedIds.includes('pg_sync_amen_dup'), 'PostgreSQL REQ-04: Duplicate amen acknowledged in syncedIds');
    const pgCountAfterDup = (await makeRequest('GET', '/api/prayers')).body.prayers.find(p => p.id === prayerId).prayer_count;
    assert(pgCountAfterDup === pgCountBefore + 1, 'PostgreSQL REQ-04: Duplicate amen does NOT increment prayer_count');

    // --- REQ-05: Prayer CREATE Replay Safety in PostgreSQL ---
    // Idempotent replay
    const pgReplaySame = await makeRequest('POST', '/api/sync', {
      mutations: [{
        id: 'pg_replay_same_1',
        operationType: 'CREATE',
        entityType: 'PRAYER',
        entityId: 'prayer_pg_offline_1',
        payloadJson: JSON.stringify({
          title: 'Physical Harvest Thanksgiving',
          content: 'Thanking God for abundance',
          isAnonymous: false
        })
      }]
    }, token2);
    assert(pgReplaySame.status === 200, 'PostgreSQL REQ-05: Replay same prayer responds 200');
    assert(pgReplaySame.body.syncedIds.includes('pg_replay_same_1'), 'PostgreSQL REQ-05: Idempotent replay acknowledged');

    // Conflicting author
    const pgReplayDiffAuthor = await makeRequest('POST', '/api/sync', {
      mutations: [{
        id: 'pg_replay_hijack',
        operationType: 'CREATE',
        entityType: 'PRAYER',
        entityId: 'prayer_pg_offline_1',
        payloadJson: JSON.stringify({
          title: 'Physical Harvest Thanksgiving',
          content: 'Thanking God for abundance',
          isAnonymous: false
        })
      }]
    }, token1);
    assert(!pgReplayDiffAuthor.body.syncedIds.includes('pg_replay_hijack'), 'PostgreSQL REQ-05: Hijack excluded from syncedIds');
    const pgAuthorMismatch = pgReplayDiffAuthor.body.rejectedMutations.find(r => r.id === 'pg_replay_hijack');
    assert(pgAuthorMismatch && pgAuthorMismatch.reason === 'CONFLICT_MISMATCH', 'PostgreSQL REQ-05: Author mismatch marked CONFLICT_MISMATCH');

    // Conflicting content
    const pgReplayDiffContent = await makeRequest('POST', '/api/sync', {
      mutations: [{
        id: 'pg_replay_bad_content',
        operationType: 'CREATE',
        entityType: 'PRAYER',
        entityId: 'prayer_pg_offline_1',
        payloadJson: JSON.stringify({
          title: 'Physical Harvest Thanksgiving',
          content: 'Tampered content in offline queue',
          isAnonymous: false
        })
      }]
    }, token2);
    assert(!pgReplayDiffContent.body.syncedIds.includes('pg_replay_bad_content'), 'PostgreSQL REQ-05: Content conflict excluded from syncedIds');
    const pgContentMismatch = pgReplayDiffContent.body.rejectedMutations.find(r => r.id === 'pg_replay_bad_content');
    assert(pgContentMismatch && pgContentMismatch.reason === 'CONFLICT_MISMATCH', 'PostgreSQL REQ-05: Content conflict marked CONFLICT_MISMATCH');

    // --- REQ-06: Anonymous Privacy in PostgreSQL ---
    const pgAnonPost = await makeRequest('POST', '/api/prayers', {
      title: 'PostgreSQL Anonymous Petition',
      content: 'Confidential prayer item',
      isAnonymous: true
    }, token1);
    assert(pgAnonPost.status === 201, 'PostgreSQL REQ-06: Anonymous prayer created (201)');
    assert(pgAnonPost.body.prayer.author_id === 'anonymous', 'PostgreSQL REQ-06: author_id masked as anonymous in POST JSON');

    const pgWall = await makeRequest('GET', '/api/prayers');
    const pgAnonInWall = pgWall.body.prayers.find(p => p.id === pgAnonPost.body.prayer.id);
    assert(pgAnonInWall && pgAnonInWall.author_id === 'anonymous', 'PostgreSQL REQ-06: author_id masked as anonymous in GET /prayers');

    // Check DB row directly: verify real author_id is preserved for ownership while masked in JSON
    const pgDbRow = await query('SELECT id, author_id, is_anonymous FROM prayers WHERE id = $1', [pgAnonPost.body.prayer.id]);
    assert(pgDbRow.rows.length === 1, 'PostgreSQL REQ-06: Row exists in PostgreSQL');
    assert(pgDbRow.rows[0].author_id !== 'anonymous', 'PostgreSQL REQ-06: Real author_id preserved in DB for ownership');
    assert(pgDbRow.rows[0].is_anonymous === true, 'PostgreSQL REQ-06: is_anonymous is true in DB');

    // Batch sync anonymous prayer
    const pgAnonSync = await makeRequest('POST', '/api/sync', {
      mutations: [{
        id: 'pg_sync_anon_item',
        operationType: 'CREATE',
        entityType: 'PRAYER',
        entityId: 'prayer_pg_anon_1',
        payloadJson: JSON.stringify({
          title: 'Batch Sync Anon',
          content: 'Secret batch petition',
          isAnonymous: true
        })
      }],
      lastSyncTimestamp: 0
    }, token1);
    const pgSyncedAnon = pgAnonSync.body.prayers.find(p => p.id === 'prayer_pg_anon_1');
    assert(pgSyncedAnon && pgSyncedAnon.author_id === 'anonymous', 'PostgreSQL REQ-06: author_id masked as anonymous in sync response');

    // --- REQ-07: Paginated Synchronization in PostgreSQL ---
    const pgPage1 = await makeRequest('POST', '/api/sync', { limit: 2, lastSyncTimestamp: 0 });
    assert(pgPage1.status === 200, 'PostgreSQL REQ-07: Page 1 sync responds 200');
    assert(pgPage1.body.prayers.length === 2, 'PostgreSQL REQ-07: Page 1 respects limit 2');
    assert(pgPage1.body.hasMore === true, 'PostgreSQL REQ-07: hasMore is true');
    assert(pgPage1.body.nextCursor !== null, 'PostgreSQL REQ-07: nextCursor provided');

    const pgPage2 = await makeRequest('POST', '/api/sync', { limit: 10, cursor: pgPage1.body.nextCursor });
    assert(pgPage2.status === 200, 'PostgreSQL REQ-07: Page 2 sync responds 200');
    assert(pgPage2.body.prayers.length >= 1, 'PostgreSQL REQ-07: Page 2 returns subsequent items');
    const pgP1Ids = new Set(pgPage1.body.prayers.map(p => p.id));
    assert(!pgPage2.body.prayers.some(p => pgP1Ids.has(p.id)), 'PostgreSQL REQ-07: No duplicate items across pages');

    // P1: a row whose updated_at changes after page one remains reachable by the cursor.
    const paginationBase = Date.now() + 1000;
    const movingPrayerId = 'p1_moving_' + crypto.randomUUID();
    const paginationIds = [movingPrayerId, 'p1_page_' + crypto.randomUUID(), 'p1_page_' + crypto.randomUUID()];
    try {
      for (let index = 0; index < paginationIds.length; index++) {
        const timestamp = paginationBase + index;
        await query(
          `INSERT INTO prayers (id,title,content,author_id,author_name,is_anonymous,visibility,prayer_count,is_answered,testimony,created_at,updated_at)
           VALUES ($1,$1,'synthetic pagination row',$2,'Synthetic pagination user',false,'PUBLIC',0,false,'',$3,$3)`,
          [paginationIds[index], regUser1.body.user.id, timestamp]
        );
      }
      const movingPage1 = await makeRequest('POST', '/api/sync', { limit: 1, lastSyncTimestamp: paginationBase - 1 });
      assert(movingPage1.status === 200 && movingPage1.body.prayers[0]?.id === movingPrayerId,
        'P1 pagination: first page starts with the oldest synthetic row');
      await query('UPDATE prayers SET updated_at = $2 WHERE id = $1', [movingPrayerId, paginationBase + 100]);
      let cursor = movingPage1.body.nextCursor;
      let movingRowFound = false;
      let pagesRead = 0;
      while (cursor && pagesRead < 10) {
        const page = await makeRequest('POST', '/api/sync', { limit: 1, cursor });
        assert(page.status === 200, 'P1 pagination: continuation request succeeds after updated_at changes');
        if (page.body.prayers.some(row => row.id === movingPrayerId && Number(row.updated_at) === paginationBase + 100)) {
          movingRowFound = true;
        }
        cursor = page.body.hasMore ? page.body.nextCursor : null;
        pagesRead++;
      }
      assert(movingRowFound, 'P1 pagination: changed row appears at its new cursor position');
      assert(pagesRead < 10, 'P1 pagination: changed timestamp does not create an endless cursor loop');
    } finally {
      await query('DELETE FROM prayers WHERE id = ANY($1::text[])', [paginationIds]);
    }

    // P1: concurrent delivery of one (user, mutation_id) applies and audits once.
    const concurrentMutationId = 'p1_concurrent_' + crypto.randomUUID();
    const concurrentPrayerId = 'p1_prayer_' + crypto.randomUUID();
    const concurrentMutation = {
      id: concurrentMutationId,
      operationType: 'CREATE',
      entityType: 'PRAYER',
      entityId: concurrentPrayerId,
      payloadJson: JSON.stringify({ title: 'Synthetic concurrent retry', content: 'Synthetic body' })
    };
    const concurrentRequests = await Promise.all([
      makeRequest('POST', '/api/sync', { mutations: [concurrentMutation] }, token1),
      makeRequest('POST', '/api/sync', { mutations: [concurrentMutation] }, token1)
    ]);
    assert(concurrentRequests.every(response => response.status === 200 && response.body.syncedIds.includes(concurrentMutationId)),
      'P1 concurrency: concurrent identical requests both receive the durable success');
    assert((await query('SELECT COUNT(*)::int AS count FROM prayers WHERE id = $1', [concurrentPrayerId])).rows[0].count === 1,
      'P1 concurrency: exactly one business prayer write');
    assert((await query('SELECT COUNT(*)::int AS count FROM sync_receipts WHERE user_id = $1 AND mutation_id = $2', [regUser1.body.user.id, concurrentMutationId])).rows[0].count === 1,
      'P1 concurrency: exactly one mutation receipt');
    assert((await query('SELECT COUNT(*)::int AS count FROM sync_events WHERE user_id = $1 AND entity_id = $2', [regUser1.body.user.id, concurrentPrayerId])).rows[0].count === 1,
      'P1 concurrency: exactly one sync_events row');

    // P1: fail after the prayer write so transaction rollback covers business/audit/receipt rows.
    const failingMutationId = 'p1_rollback_' + crypto.randomUUID();
    const failingPrayerId = 'p1_rollback_prayer_' + crypto.randomUUID();
    await query(`CREATE FUNCTION p1_test_reject_sync_event() RETURNS trigger LANGUAGE plpgsql AS $$
      BEGIN
        IF NEW.entity_id = '${failingPrayerId}' THEN RAISE EXCEPTION 'synthetic P1 audit failure'; END IF;
        RETURN NEW;
      END $$`);
    await query('CREATE TRIGGER p1_test_reject_sync_event BEFORE INSERT ON sync_events FOR EACH ROW EXECUTE FUNCTION p1_test_reject_sync_event()');
    try {
      const failedMutation = await makeRequest('POST', '/api/sync', { mutations: [{
        id: failingMutationId,
        operationType: 'CREATE',
        entityType: 'PRAYER',
        entityId: failingPrayerId,
        payloadJson: JSON.stringify({ title: 'Synthetic rollback test', content: 'Synthetic content' })
      }] }, token1);
      assert(failedMutation.status === 200 && failedMutation.body.rejectedMutations.some(item => item.id === failingMutationId && item.reason === 'PROCESSING_ERROR'),
        'P1 rollback: client receives PROCESSING_ERROR for the failed transaction');
      assert((await query('SELECT COUNT(*)::int AS count FROM prayers WHERE id = $1', [failingPrayerId])).rows[0].count === 0,
        'P1 rollback: business prayer write was rolled back');
      assert((await query('SELECT COUNT(*)::int AS count FROM sync_events WHERE user_id = $1 AND entity_id = $2', [regUser1.body.user.id, failingPrayerId])).rows[0].count === 0,
        'P1 rollback: audit event was rolled back');
      assert((await query('SELECT COUNT(*)::int AS count FROM sync_receipts WHERE user_id = $1 AND mutation_id = $2', [regUser1.body.user.id, failingMutationId])).rows[0].count === 0,
        'P1 rollback: receipt was rolled back so retry can reprocess');
    } finally {
      await query('DROP TRIGGER IF EXISTS p1_test_reject_sync_event ON sync_events');
      await query('DROP FUNCTION IF EXISTS p1_test_reject_sync_event()');
    }

    // 14. Brute-Force Rate Limiting Test against PostgreSQL
    const bruteForcePhone = '0777000000';
    await makeRequest('POST', '/api/auth/register', {
      phone: bruteForcePhone,
      pin: '4321',
      name: 'PG Rate Limit Target',
      campus_id: 'campus_nairobi',
      ...registrationNotice
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
    console.log('🎉 ALL REAL POSTGRESQL API CONTRACT INTEGRATION TESTS PASSED!');
    console.log('======================================================\n');
  } catch (err) {
    console.error('PostgreSQL test run failed with error:', redactCredentials(err));
    try { if (server && server.closeAllConnections) server.closeAllConnections(); server.close(); } catch (e) {}
    try { await closeDb(); } catch (e) {}
    process.exitCode = 1;
  } finally {
    try { if (server && server.closeAllConnections) server.closeAllConnections(); server.close(); } catch (e) {}
    try { await closeDb(); } catch (e) {}
  }
}

runPostgresTests();
