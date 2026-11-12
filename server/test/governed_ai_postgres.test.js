'use strict';

// Explicit PostgreSQL integration suite. It creates and drops only a uniquely
// named local *_test database; it never consults the application's configured DB.
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const { Pool } = require('pg');

function adminUrl() {
  if (process.env.NODE_ENV === 'production') throw new Error('PostgreSQL integration suite is disabled in production.');
  const raw = process.env.MIGRATIONS_TEST_ADMIN_URL;
  if (!raw) throw new Error('Set MIGRATIONS_TEST_ADMIN_URL to a local PostgreSQL URL targeting database postgres.');
  const url = new URL(raw);
  const host = (url.searchParams.get('host') || url.hostname).replace(/^\[|\]$/g, '').toLowerCase();
  if (!['postgres:', 'postgresql:'].includes(url.protocol) || url.pathname !== '/postgres' || url.search || url.hash ||
      !(host === 'localhost' || host === '127.0.0.1' || host === '::1')) {
    throw new Error('Integration suite requires an unmodified local loopback URL targeting postgres.');
  }
  return url;
}

function databaseUrl(admin, database) {
  const url = new URL(admin.toString());
  url.pathname = `/${database}`;
  return url.toString();
}

async function run() {
  const admin = adminUrl();
  const adminPool = new Pool({ connectionString: admin.toString() });
  const dbName = `perazim_phaseb_${crypto.randomBytes(6).toString('hex')}_test`;
  const roleName = `perazim_stage4_${crypto.randomBytes(4).toString('hex')}`;
  const pool = new Pool({ connectionString: databaseUrl(admin, dbName) });
  let appDb;
  let httpServer;
  const root = path.resolve(__dirname, '..');
  const files = ['0001_current_schema_baseline.sql', '0002_membership_permissions.sql',
    '0003_membership_claim_rate_limits.sql', '0004_membership_identity_and_revisions.sql',
    '0005_member_contact_auto_verification.sql', '0006_privacy_notice_acknowledgements.sql',
    '0008_membership_import_staging.sql', '0009_legacy_identity_reconciliation.sql',
    '0010_data01_privacy_governance.sql', '0011_identity_revision_pin.sql',
    '0012_membership_verification_history_append_only.sql',
    '0013_governed_ai_knowledge.sql', '0014_ai_source_workflow_and_scripture.sql'];
  const applied = [];
  const G = require('../src/phase-b/knowledgeGovernance');
  const requestId = () => crypto.randomUUID();

  async function apply(client, filename) {
    const sql = fs.readFileSync(path.join(root, 'migrations', filename), 'utf8');
    await client.query('BEGIN');
    try { await client.query(sql); await client.query('COMMIT'); applied.push(filename); }
    catch (error) { await client.query('ROLLBACK'); throw error; }
  }

  async function transact(work) {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const value = await work(client);
      await client.query('COMMIT');
      return value;
    } catch (error) {
      try { await client.query('ROLLBACK'); } catch (_) {}
      throw error;
    } finally { client.release(true); }
  }

  try {
    const target = await adminPool.query('SELECT current_database() AS db, inet_server_addr()::text AS address, inet_server_port() AS port, current_setting(\'server_version_num\')::int AS version');
    assert.equal(target.rows[0].db, 'postgres');
    assert.match(target.rows[0].address, /^(127\.|::1$)/);
    assert.ok(target.rows[0].version >= 130000);
    await adminPool.query(`CREATE DATABASE "${dbName}"`);

    const client = await pool.connect();
    try {
      // The official runner deliberately rejects any plan containing 0007.
      // Exercise the other real prerequisites in order and document the one
      // skipped guarded migration in the Stage 4 report.
      for (const filename of files.slice(0, -2)) await apply(client, filename);
      const governedFiles = files.slice(-2);
      await client.query('BEGIN');
      await client.query(fs.readFileSync(path.join(root, 'migrations', governedFiles[0]), 'utf8'));
      await client.query(fs.readFileSync(path.join(root, 'migrations', governedFiles[1]), 'utf8'));
      await assert.rejects(() => client.query('SELECT 1 / 0'), /division by zero/);
      await client.query('ROLLBACK');
      const rolledBack = await client.query("SELECT to_regclass('public.ai_knowledge_sources') AS source_table, to_regclass('public.ai_scripture_verses') AS verse_table");
      assert.deepEqual(rolledBack.rows[0], { source_table: null, verse_table: null });
      await apply(client, governedFiles[0]);
      await apply(client, governedFiles[1]);
      await apply(client, '0015_online_ai_daily_budget.sql');
      await apply(client, '0016_offline_ai_control_sequence.sql');
      await apply(client, '0017_ai_offline_rollback_authorizations.sql');

      const structure = await client.query(`
        SELECT (SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN
          ('ai_knowledge_sources','ai_knowledge_chunks','ai_knowledge_events','ai_knowledge_source_reviews',
           'ai_knowledge_conflict_groups','ai_knowledge_conflict_sources','ai_scripture_corpora',
           'ai_scripture_corpus_events','ai_scripture_books','ai_scripture_verses',
           'ai_offline_rollback_authorizations'))::int AS tables,
          (SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname IN
           ('ai_knowledge_one_published_version','ai_scripture_one_published_translation'))::int AS indexes,
          (SELECT active FROM permissions WHERE code='knowledge.source.approve.institutional') AS approval_active`);
      assert.equal(structure.rows[0].tables, 11);
      assert.equal(structure.rows[0].indexes, 2);
      assert.equal(structure.rows[0].approval_active, false);

      const users = [
        ['stage4_submitter', '+254700000101'],
        ['stage4_reviewer', '+254700000102'],
        ['stage4_approver', '+254700000103'],
        ['stage4_publisher', '+254700000104'],
        ['stage4_admin', '+254700000105']
      ];
      for (const [id, phone] of users) await client.query(
        `INSERT INTO users(id,phone,pin_hash,name,campus_id,role,created_at,updated_at)
         VALUES($1,$2,'synthetic','Stage 4 Fixture','campus_embu','MEMBER',1,1)`, [id, phone]);
      const sourceId = 'stage4-synthetic-source';
      await client.query(`INSERT INTO ai_knowledge_sources
        (source_id,version,title,document_type,authority_class,approval_status,confidentiality,
         confidentiality_reviewed_by,confidentiality_reviewed_at,permitted_audiences,permitted_uses,
         source_location,content_hash,language,ingested_at,created_by)
        VALUES($1,'v1','Synthetic source','POLICY','OFFICIAL_POLICY','UNVERIFIED','PUBLIC',
         'Stage 4 reviewer',1,'["TEST"]','["TEST"]','Fixture section',$2,'en',1,'stage4_submitter')`,
        [sourceId, 'a'.repeat(64)]);

      await assert.rejects(() => client.query(`INSERT INTO ai_knowledge_sources
        (source_id,version,title,document_type,authority_class,approval_status,confidentiality,
         confidentiality_reviewed_by,confidentiality_reviewed_at,permitted_audiences,permitted_uses,
         source_location,content_hash,language,ingested_at,created_by)
        VALUES($1,'v1','Synthetic source','POLICY','OFFICIAL_POLICY','UNVERIFIED','PUBLIC',
         'Stage 4 reviewer',1,'{}','["TEST"]','Fixture section',$2,'en',1,'stage4_submitter')`,
        ['bad-json-fixture', 'b'.repeat(64)]), /check constraint/i);
      await assert.rejects(() => client.query(`INSERT INTO ai_knowledge_source_reviews
        (review_id,source_id,source_version,reviewer_id,decision,reason_code,request_id,created_at)
        VALUES($1,$2,'missing-version','stage4_reviewer','RECOMMEND_APPROVAL','SOURCE_REVIEW',$3,2)`,
        [requestId(), sourceId, requestId()]), /foreign key/i);

      // Exercise the actual Express route and PostgreSQL-backed auth/permission
      // path. Only the synthetic pilot account is granted test privileges.
      process.env.NODE_ENV = 'test';
      process.env.PHASE_B_SYNTHETIC = 'true';
      process.env.DATABASE_URL = databaseUrl(admin, dbName);
      process.env.PHASE_B_TEST_EXPECTED_PORT = String(admin.port || 55439);
      if (!process.env.PHASE_B_TEST_EXPECTED_DATA_DIR) throw new Error('Set PHASE_B_TEST_EXPECTED_DATA_DIR to this disposable cluster path.');
      process.env.JWT_SECRET = crypto.randomBytes(32).toString('hex');
      appDb = require('../src/db');
      await appDb.initDb();
      await client.query(`INSERT INTO pilot_bootstrap(singleton,user_id,approval_ref,created_at)
        VALUES(TRUE,'stage4_admin','SYNTHETIC-TEST-ONLY',1)`);
      await client.query(`INSERT INTO admin_security(user_id,totp_secret_cipher,security_version,auth_not_before,created_at)
        VALUES('stage4_admin','synthetic-test-only',7,0,1)`);
      const capabilityCodes = ['knowledge.source.manage','knowledge.source.review','knowledge.source.publish','knowledge.source.approve.institutional'];
      for (const code of capabilityCodes) {
        const active = code !== 'knowledge.source.approve.institutional';
        await client.query('UPDATE permissions SET active=$2 WHERE code=$1', [code, active]);
        await client.query(`INSERT INTO user_permissions(id,user_id,permission_code,scope,granted_by,granted_at,effective_at)
          VALUES($1,'stage4_admin',$2,'global','stage4_admin',1,1)`, [requestId(), code]);
      }
      const { signToken } = require('../src/middleware/auth');
      const token = signToken({ id: 'stage4_admin', phone: '+254700000105', name: 'Stage 4 Fixture', role: 'MEMBER', campus_id: 'campus_embu' });
      const stepUp = 'synthetic-step-up-token';
      const hash = value => crypto.createHash('sha256').update(value).digest('hex');
      await client.query(`INSERT INTO privileged_sessions(token_hash,user_id,security_version,auth_token_hash,expires_at,created_at)
        VALUES($1,'stage4_admin',7,$2,$3,1)`, [hash(stepUp), hash(token), Date.now() + 60000]);
      const apiSourceId = 'stage4-api-synthetic-source';
      await client.query(`INSERT INTO ai_knowledge_sources
        (source_id,version,title,document_type,authority_class,approval_status,confidentiality,
         confidentiality_reviewed_by,confidentiality_reviewed_at,permitted_audiences,permitted_uses,
         source_location,content_hash,language,ingested_at,created_by)
        VALUES($1,'v1','Synthetic API source','POLICY','OFFICIAL_POLICY','UNVERIFIED','PUBLIC',
         'Stage 4 reviewer',1,'["TEST"]','["TEST"]','Fixture section',$2,'en',1,'stage4_submitter')`,
        [apiSourceId, 'd'.repeat(64)]);
      const express = require('express');
      const app = express();
      app.use('/api/ai', require('../src/routes/ai'));
      app.use(express.json());
      app.use('/api/knowledge-sources', require('../src/routes/knowledgeGovernance'));
      httpServer = await new Promise((resolve, reject) => {
        const server = app.listen(0, '127.0.0.1', () => resolve(server));
        server.once('error', reject);
      });
      const base = `http://127.0.0.1:${httpServer.address().port}/api/knowledge-sources`;
      const headers = { authorization: `Bearer ${token}`, 'x-privileged-session': stepUp, 'content-type': 'application/json' };
      const aiBase = `http://127.0.0.1:${httpServer.address().port}/api/ai`;
      const packageKeyPair = crypto.generateKeyPairSync('ed25519');
      process.env.PERAZIM_AI_OFFLINE_PACKAGE_EXPORT_ENABLED = 'true';
      process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID = 'synthetic-export-key';
      process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY = packageKeyPair.privateKey.export({ type: 'pkcs8', format: 'pem' });
      process.env.PERAZIM_AI_PACKAGE_MIN_APP_VERSION = '2.2.0';
      process.env.PERAZIM_AI_PACKAGE_TTL_MS = '86400000';
      process.env.PERAZIM_AI_OFFLINE_CONTROL_TTL_MS = '86400000';
      process.env.PERAZIM_AI_ROLLBACK_TTL_MS = '3600000';
      const packageText = 'Synthetic approved source text for export acceptance.';
      const packageSourceId = 'stage4-package-export-source';
      const packageHash = crypto.createHash('sha256').update(packageText).digest('hex');
      await client.query(`INSERT INTO ai_knowledge_sources
        (source_id,version,title,document_type,authority_class,approval_status,approval_authority,
         approval_reference,approved_at,effective_at,confidentiality,confidentiality_reviewed_by,
         confidentiality_reviewed_at,permitted_audiences,permitted_uses,source_location,content_hash,
         language,ingested_at,created_by,publication_status,reviewed_by,review_decision,reviewed_at,
         approval_actor_id,published_by,published_at)
        VALUES($1,'v1','Synthetic export source','TEACHING','APPROVED_TEACHING','APPROVED',
         'Synthetic authority','FIXTURE-EXPORT-1',$2,$2,'PUBLIC','synthetic-reviewer',$2,
         '["PUBLIC"]','["OFFLINE"]','Synthetic section',$3,'en',$2,'stage4_submitter',
         'PUBLISHED','stage4_reviewer','RECOMMEND_APPROVAL',$2,'stage4_approver','stage4_publisher',$2)`,
        [packageSourceId, Date.now() - 1000, packageHash]);
      await client.query(`INSERT INTO ai_knowledge_chunks
        (chunk_id,source_id,source_version,section,location,content,content_hash,created_at)
        VALUES($1,$2,'v1','Synthetic section','Synthetic location',$3,$4,$5)`,
        [packageHash, packageSourceId, packageText, packageHash, Date.now()]);
      const packageResponse = await fetch(`${aiBase}/offline-package/current`, { headers: { authorization: `Bearer ${token}` } });
      assert.equal(packageResponse.status, 200);
      assert.equal(packageResponse.headers.get('cache-control'), 'private, no-store');
      const packageEnvelope = await packageResponse.json();
      const { verifyOfflinePackage } = require('../src/phase-b/offlineKnowledgePackage');
      const verifiedPackage = verifyOfflinePackage(packageEnvelope, {
        trustedKeys: { 'synthetic-export-key': packageKeyPair.publicKey },
        appVersion: '2.2.0'
      });
      assert.equal(verifiedPackage.documents[0].text, packageText);
      assert.ok(verifiedPackage.expiresAt > Date.now(), 'export includes a future signed expiry');
      assert.equal(verifiedPackage.expiresAt - verifiedPackage.publishedAt, 86400000);
      const rollbackBody = { requestId: requestId(), targetPackageId: verifiedPackage.packageId,
        targetDigest: verifiedPackage.contentDigest, reasonCode: 'PACKAGE_ROLLBACK' };
      const rollbackUrl = `${aiBase}/offline-package/rollback/authorize`;
      const rollbackDisabled = await fetch(rollbackUrl, { method: 'POST', headers,
        body: JSON.stringify(rollbackBody) });
      assert.equal(rollbackDisabled.status, 503, 'rollback issuance fails closed until its dedicated gate is enabled');
      process.env.PERAZIM_AI_OFFLINE_ROLLBACK_AUTHORIZATION_ENABLED = 'true';
      const rollbackNoStepUp = await fetch(rollbackUrl, { method: 'POST',
        headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
        body: JSON.stringify(rollbackBody) });
      assert.equal(rollbackNoStepUp.status, 401, 'rollback issuance requires fresh administrator step-up');
      const rollbackDenied = await fetch(rollbackUrl, { method: 'POST', headers,
        body: JSON.stringify(rollbackBody) });
      assert.equal(rollbackDenied.status, 403, 'rollback issuance requires its dedicated server permission');
      await client.query("INSERT INTO user_permissions(id,user_id,permission_code,scope,granted_by,granted_at,effective_at) VALUES($1,'stage4_admin','knowledge.package.rollback.authorize','global','stage4_admin',$2,$2)", [requestId(), Date.now()]);
      const rollbackIssuedResponse = await fetch(rollbackUrl, { method: 'POST', headers,
        body: JSON.stringify(rollbackBody) });
      assert.equal(rollbackIssuedResponse.status, 201);
      assert.equal(rollbackIssuedResponse.headers.get('cache-control'), 'private, no-store');
      const rollbackEnvelope = await rollbackIssuedResponse.json();
      const { verifyRollbackAuthorization } = require('../src/phase-b/offlineKnowledgePackage');
      const verifiedRollback = verifyRollbackAuthorization(rollbackEnvelope, {
        trustedKeys: { 'synthetic-export-key': packageKeyPair.publicKey }
      });
      assert.equal(verifiedRollback.targetPackageId, verifiedPackage.packageId);
      assert.equal(verifiedRollback.targetDigest, verifiedPackage.contentDigest);
      assert.equal(verifiedRollback.reasonCode, 'PACKAGE_ROLLBACK');
      const rollbackReplay = await fetch(rollbackUrl, { method: 'POST', headers,
        body: JSON.stringify(rollbackBody) });
      assert.equal(rollbackReplay.status, 409, 'rollback request identifiers cannot be replayed');
      const rollbackDelivery = await fetch(`${aiBase}/offline-package/rollback/current?targetPackageId=${encodeURIComponent(verifiedPackage.packageId)}&targetDigest=${verifiedPackage.contentDigest}`,
        { headers: { authorization: `Bearer ${token}` } });
      assert.equal(rollbackDelivery.status, 200, 'the authenticated Android client can retrieve its exact authorization');
      assert.deepEqual(await rollbackDelivery.json(), rollbackEnvelope);
      const wrongRollbackTarget = await fetch(`${aiBase}/offline-package/rollback/current?targetPackageId=${encodeURIComponent(verifiedPackage.packageId)}&targetDigest=${'0'.repeat(64)}`,
        { headers: { authorization: `Bearer ${token}` } });
      assert.equal(wrongRollbackTarget.status, 404, 'authorization delivery is scoped to the exact target digest');
      const rollbackAudit = await client.query("SELECT action,reason FROM admin_audit_log WHERE actor_id='stage4_admin' AND action='AI_PACKAGE_ROLLBACK_AUTHORIZE'");
      assert.deepEqual(rollbackAudit.rows, [{ action: 'AI_PACKAGE_ROLLBACK_AUTHORIZE', reason: 'PACKAGE_ROLLBACK' }]);
      const controlResponse = await fetch(`${aiBase}/offline-package/control/current`, { headers: { authorization: `Bearer ${token}` } });
      assert.equal(controlResponse.status, 200);
      assert.equal(controlResponse.headers.get('cache-control'), 'private, no-store');
      const controlEnvelope = await controlResponse.json();
      const { verifyWithdrawalManifest } = require('../src/phase-b/offlineKnowledgePackage');
      const verifiedControl = verifyWithdrawalManifest(controlEnvelope, {
        trustedKeys: { 'synthetic-export-key': packageKeyPair.publicKey }
      });
      assert.deepEqual(verifiedControl.withdrawnSources, []);
      delete process.env.PERAZIM_AI_OFFLINE_CONTROL_TTL_MS;
      const missingControlTtl = await fetch(`${aiBase}/offline-package/control/current`, { headers: { authorization: `Bearer ${token}` } });
      assert.equal(missingControlTtl.status, 503, 'status export fails closed without a configured control lifetime');
      process.env.PERAZIM_AI_OFFLINE_CONTROL_TTL_MS = '86400000';
      delete process.env.PERAZIM_AI_PACKAGE_TTL_MS;
      const missingTtlPackage = await fetch(`${aiBase}/offline-package/current`, { headers: { authorization: `Bearer ${token}` } });
      assert.equal(missingTtlPackage.status, 503, 'export fails closed without a configured package lifetime');
      process.env.PERAZIM_AI_PACKAGE_TTL_MS = '86400000';
      await transact(db => G.withdrawSource(db, { sourceId: packageSourceId, version: 'v1',
        actorId: 'stage4_publisher', reasonCode: 'SOURCE_ERROR', requestId: requestId() }));
      const withdrawnPackage = await fetch(`${aiBase}/offline-package/current`, { headers: { authorization: `Bearer ${token}` } });
      assert.equal(withdrawnPackage.status, 200, 'a signed empty snapshot communicates that no eligible sources remain');
      const emptyEnvelope = await withdrawnPackage.json();
      const emptySnapshot = verifyOfflinePackage(emptyEnvelope, {
        trustedKeys: { 'synthetic-export-key': packageKeyPair.publicKey }, appVersion: '2.2.0'
      });
      assert.equal(emptySnapshot.snapshotKind, 'EMPTY');
      assert.equal(emptySnapshot.documents.length, 0, 'withdrawn content is removed from the next snapshot');
      assert.ok(Number(emptySnapshot.version) > Number(verifiedPackage.version),
        'the full snapshot version advances when a source is withdrawn');
      const withdrawnControlResponse = await fetch(`${aiBase}/offline-package/control/current`, { headers: { authorization: `Bearer ${token}` } });
      assert.equal(withdrawnControlResponse.status, 200);
      const withdrawnControl = verifyWithdrawalManifest(await withdrawnControlResponse.json(), {
        trustedKeys: { 'synthetic-export-key': packageKeyPair.publicKey }
      });
      assert.ok(withdrawnControl.controlSequence > verifiedControl.controlSequence,
        'withdrawal controls use a monotonic database sequence');
      assert.deepEqual(withdrawnControl.withdrawnSources, [{ sourceId: packageSourceId,
        version: 'v1', reasonCode: 'SOURCE_ERROR' }], 'signed control contains only withdrawn public/offline source metadata');
      delete process.env.PERAZIM_AI_OFFLINE_PACKAGE_EXPORT_ENABLED;
      const disabledPackage = await fetch(`${aiBase}/offline-package/current`, { headers: { authorization: `Bearer ${token}` } });
      assert.equal(disabledPackage.status, 503, 'export fails closed without explicit export gate');
      process.env.PERAZIM_AI_OFFLINE_PACKAGE_EXPORT_ENABLED = 'true';

      const aiUrl = `${aiBase}/online`;
      const aiRequest = (question, extraHeaders = {}) => fetch(aiUrl, { method: 'POST',
        headers: { 'content-type': 'application/json', ...extraHeaders }, body: JSON.stringify({ question }) });
      // Exercise one fully grounded online request end to end. The provider is
      // replaced only for the synthetic Worker URL; all auth, entitlement,
      // retrieval, Scripture lookup, quota, response validation, and routing
      // stay real and PostgreSQL-backed.
      const syntheticMembershipImport = requestId();
      const syntheticMembershipRecord = requestId();
      await client.query(`INSERT INTO membership_imports(id,source,checksum,actor_id,created_at)
        VALUES($1,'synthetic-test','${'a'.repeat(64)}','stage4_admin',$2)`, [syntheticMembershipImport, Date.now()]);
      await client.query(`INSERT INTO membership_records(id,import_id,reference,full_name,phone,record_status,created_at,updated_at)
        VALUES($1,$2,'SYNTHETIC-ONLINE-1','Synthetic Member','+254700000105','ACTIVE',$3,$3)`,
        [syntheticMembershipRecord, syntheticMembershipImport, Date.now()]);
      await client.query(`INSERT INTO membership_verifications(user_id,membership_record_id,status,updated_at)
        VALUES('stage4_admin',$1,'VERIFIED_MEMBER',$2)`, [syntheticMembershipRecord, Date.now()]);
      const onlinePermission = 'ai.online.use';
      process.env.PERAZIM_AI_ONLINE_PERMISSION_CODE = onlinePermission;
      await client.query(`INSERT INTO permissions(code,description,active) VALUES($1,'Synthetic online AI test permission',TRUE)`, [onlinePermission]);
      await client.query(`INSERT INTO user_permissions(id,user_id,permission_code,scope,granted_by,granted_at,effective_at)
        VALUES($1,'stage4_admin',$2,'global','stage4_admin',$3,$3)`, [requestId(), onlinePermission, Date.now()]);

      const institutionalText = 'Synthetic source passage about creation and the beginning of the world.';
      const institutionalHash = crypto.createHash('sha256').update(institutionalText).digest('hex');
      const onlineSourceId = 'stage4-online-grounded-source';
      const fixtureNow = Date.now();
      await client.query(`INSERT INTO ai_knowledge_sources
        (source_id,version,title,document_type,authority_class,approval_status,approval_authority,
         approval_reference,approved_at,effective_at,confidentiality,confidentiality_reviewed_by,
         confidentiality_reviewed_at,permitted_audiences,permitted_uses,source_location,content_hash,
         language,ingested_at,created_by,publication_status,reviewed_by,review_decision,reviewed_at,
         approval_actor_id,published_by,published_at)
        VALUES($1,'v1','Synthetic creation teaching','TEACHING','APPROVED_TEACHING','APPROVED',
         'Synthetic authority','FIXTURE-ONLINE-1',$2,$2,'PUBLIC','synthetic-reviewer',$2,
         '["PUBLIC"]','["INSTITUTIONAL"]','Synthetic creation section',$3,'en',$2,'stage4_submitter',
         'PUBLISHED','stage4_reviewer','RECOMMEND_APPROVAL',$2,'stage4_approver','stage4_publisher',$2)`,
        [onlineSourceId, fixtureNow, institutionalHash]);
      await client.query(`INSERT INTO ai_knowledge_chunks
        (chunk_id,source_id,source_version,section,location,content,content_hash,created_at)
        VALUES($1,$2,'v1','Creation','Synthetic creation section',$3,$4,$5)`,
        [crypto.createHash('sha256').update(`${onlineSourceId}\0v1\0Synthetic creation section\0${institutionalText}`).digest('hex'),
          onlineSourceId, institutionalText, institutionalHash, fixtureNow]);

      const scriptureText = 'In the beginning God created the heaven and the earth.';
      const scriptureHash = crypto.createHash('sha256').update(scriptureText).digest('hex');
      const corpusId = 'stage4-synthetic-scripture';
      const corpusHash = 'b'.repeat(64);
      await client.query(`INSERT INTO ai_scripture_corpora
        (corpus_id,translation_id,source_name,source_version,license_evidence_reference,license_status,
         corpus_hash,book_count,chapter_count,verse_count,integrity_validation_result,imported_at,imported_by,
         publication_status,manifest)
        VALUES($1,'KJV','Synthetic test fixture','fixture-v1','SYNTHETIC-TEST-ONLY','AUTHORIZED',
         $2,1,1,1,'VALIDATED',$3,'stage4_submitter','STAGED','{}')`, [corpusId, corpusHash, fixtureNow]);
      await client.query(`INSERT INTO ai_scripture_books(corpus_id,book_number,book_id,name,aliases,chapter_count,verse_count)
        VALUES($1,1,'GEN','Genesis','["Gen"]',1,1)`, [corpusId]);
      await client.query(`INSERT INTO ai_scripture_verses(corpus_id,book_number,chapter_number,verse_number,text,content_hash)
        VALUES($1,1,1,1,$2,$3)`, [corpusId, scriptureText, scriptureHash]);
      await client.query(`UPDATE ai_scripture_corpora SET publication_status='PUBLISHED',published_at=$2,published_by='stage4_publisher'
        WHERE corpus_id=$1`, [corpusId, fixtureNow]);
      await client.query(`INSERT INTO ai_scripture_corpus_events(event_id,corpus_id,event_type,actor_id,request_id,reason,created_at)
        VALUES($1,$2,'PUBLISHED','stage4_publisher',$3,'SYNTHETIC_TEST',$4)`,
        [requestId(), corpusId, requestId(), fixtureNow]);

      const onlineEnv = {
        PERAZIM_AI_ONLINE_ENABLED: 'true',
        PERAZIM_AI_ONLINE_PRIVACY_REVIEWED: 'true',
        PERAZIM_AI_ONLINE_QUOTA_ENFORCED: 'true',
        PERAZIM_AI_WORKER_CONTRACT: 'signed-v1',
        PERAZIM_AI_WORKER_URL: 'https://synthetic-worker.invalid/ai',
        PERAZIM_AI_WORKER_SHARED_SECRET: 'synthetic-worker-secret-'.padEnd(32, 's'),
        PERAZIM_AI_SUBJECT_PSEUDONYM_KEY: 'synthetic-subject-key-'.padEnd(32, 'k'),
        PERAZIM_AI_GLOBAL_DAILY_LIMIT: '10',
        PERAZIM_AI_SCRIPTURE_TRANSLATION: 'KJV'
      };
      for (const [key, value] of Object.entries(onlineEnv)) process.env[key] = value;
      const originalFetch = global.fetch;
      let workerCalls = 0;
      let mockInvalidCitation = false;
      global.fetch = async (input, options) => {
        if (String(input) !== onlineEnv.PERAZIM_AI_WORKER_URL) return originalFetch(input, options);
        workerCalls++;
        assert.equal(options.method, 'POST');
        const jwt = require('jsonwebtoken');
        const grant = jwt.verify(options.headers.Authorization.slice('Bearer '.length), onlineEnv.PERAZIM_AI_WORKER_SHARED_SECRET,
          { algorithms: ['HS256'], issuer: 'perazim-express', audience: 'perazim-ai-worker' });
        const workerBody = JSON.parse(options.body);
        assert.equal(grant.ctx, crypto.createHash('sha256').update(JSON.stringify(workerBody.context)).digest('hex'));
        assert.equal(workerBody.question, 'What does Genesis 1:1 say about creation?');
        const source = workerBody.context.passages[0];
        const scripture = workerBody.context.scriptures[0];
        return new Response(JSON.stringify({ remaining: 2, response: {
          category: 'PASSAGE_INTERPRETATION',
          answer: 'Genesis 1:1 describes the beginning of creation.',
          sourceReferences: [{ sourceId: mockInvalidCitation ? 'fabricated-source-id' : source.sourceId,
            version: source.version, location: source.location }],
          scriptureReferences: [{ reference: scripture.reference, translationId: scripture.translationId,
            corpusId: scripture.corpusId, corpusVersion: scripture.corpusVersion, corpusHash: scripture.corpusHash,
            quote: 'In the beginning' }],
          confidence: 'HIGH', escalationRequired: false
        } }), { status: 200, headers: { 'content-type': 'application/json' } });
      };
      try {
        const grounded = await aiRequest('What does Genesis 1:1 say about creation?', headers);
        const groundedBody = await grounded.json();
        assert.equal(grounded.status, 200, JSON.stringify(groundedBody));
        assert.equal(workerCalls, 1, 'the synthetic Worker receives the grounded request');
        assert.equal(groundedBody.sourceSufficiency, 'SUFFICIENT');
        assert.equal(groundedBody.answer, 'Genesis 1:1 describes the beginning of creation.');
        assert.equal(groundedBody.remaining, 2);
        assert.equal(groundedBody.sourceReferences[0].sourceId, onlineSourceId);
        assert.equal(groundedBody.scriptureReferences[0].text, scriptureText);
        assert.equal(groundedBody.scriptureReferences[0].corpusHash, corpusHash);
        assert.equal(groundedBody.confidence, 'LOW', 'the server discards model self-confidence');
        mockInvalidCitation = true;
        const fabricated = await aiRequest('What does Genesis 1:1 say about creation?', headers);
        const fabricatedBody = await fabricated.json();
        assert.equal(fabricated.status, 200);
        assert.equal(workerCalls, 2);
        assert.equal(fabricatedBody.sourceSufficiency, 'INVALID_MODEL_RESPONSE');
        assert.equal(fabricatedBody.limitationCode, 'UNVERIFIED_SOURCE_REFERENCE');
        assert.deepEqual(fabricatedBody.sourceReferences, [], 'the route suppresses fabricated citations');
        assert.equal(fabricatedBody.escalationRequired, true);
      } finally {
        global.fetch = originalFetch;
      }

      const oversizedAiRequest = await aiRequest('x'.repeat(5000));
      assert.equal(oversizedAiRequest.status, 413);
      assert.equal(oversizedAiRequest.headers.get('cache-control'), 'no-store');
      assert.equal((await aiRequest('What is a psalm?')).status, 401);
      assert.equal((await aiRequest('Please pray for me', headers)).status, 422);
      const crisis = await aiRequest('I can’t go on', headers);
      assert.equal(crisis.status, 200);
      assert.equal((await crisis.json()).escalationRequired, true);
      const unauthorized = await fetch(`${base}/${apiSourceId}/v1/review`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({}) });
      assert.equal(unauthorized.status, 401);
      assert.equal(unauthorized.headers.get('cache-control'), 'private, no-store');
      const prematurePublication = await fetch(`${base}/${apiSourceId}/v1/publish`, { method: 'POST', headers,
        body: JSON.stringify({ requestId: requestId() }) });
      assert.equal(prematurePublication.status, 409, 'publisher cannot publish an unapproved source');
      const reviewResponse = await fetch(`${base}/${apiSourceId}/v1/review`, { method: 'POST', headers,
        body: JSON.stringify({ decision: 'RECOMMEND_APPROVAL', reasonCode: 'SOURCE_REVIEW', requestId: requestId() }) });
      const reviewBody = await reviewResponse.json();
      assert.equal(reviewResponse.status, 200, JSON.stringify(reviewBody));
      assert.equal(reviewBody.approvalStatus, 'PENDING_APPROVAL');
      const approvalResponse = await fetch(`${base}/${apiSourceId}/v1/approve`, { method: 'POST', headers,
        body: JSON.stringify({ approvalAuthority: 'Synthetic council', approvalReference: 'TEST-RESOLUTION-4', requestId: requestId() }) });
      assert.equal(approvalResponse.status, 403, 'institutional approval remains inactive through the real API');
      await client.query(`UPDATE permissions SET active=FALSE WHERE code IN ('knowledge.source.manage','knowledge.source.review')`);
      const deniedInspection = await fetch(base, { headers });
      assert.equal(deniedInspection.status, 403, 'authenticated administrator without an active registry permission cannot inspect sources');
      const deniedInspectionBody = await deniedInspection.json();
      assert.equal(deniedInspectionBody.code, 'PERMISSION_DENIED');

      const reviews = await Promise.allSettled([
        transact(db => G.recordReview(db, { sourceId, version: 'v1', actorId: 'stage4_reviewer',
          decision: 'RECOMMEND_APPROVAL', reasonCode: 'SOURCE_REVIEW', requestId: requestId() })),
        transact(db => G.recordReview(db, { sourceId, version: 'v1', actorId: 'stage4_reviewer',
          decision: 'RECOMMEND_APPROVAL', reasonCode: 'SOURCE_REVIEW', requestId: requestId() }))
      ]);
      assert.equal(reviews.filter(result => result.status === 'fulfilled').length, 1, 'row locks serialize competing reviews');
      assert.equal(reviews.filter(result => result.status === 'rejected').length, 1);

      const approvals = await Promise.allSettled(['stage4_approver', 'stage4_publisher'].map(actorId =>
        transact(db => G.approveSource(db, { sourceId, version: 'v1', actorId,
          approvalAuthority: 'Synthetic council', approvalReference: 'TEST-RESOLUTION-4', requestId: requestId() }))));
      assert.equal(approvals.filter(result => result.status === 'fulfilled').length, 1, 'row locks serialize competing approvals');
      assert.equal(approvals.filter(result => result.status === 'rejected').length, 1);
      const approvalRow = await pool.query('SELECT approval_actor_id FROM ai_knowledge_sources WHERE source_id=$1 AND version=\'v1\'', [sourceId]);
      const approvalActor = approvalRow.rows[0].approval_actor_id;
      const publisherActor = approvalActor === 'stage4_approver' ? 'stage4_publisher' : 'stage4_approver';
      const publications = await Promise.allSettled([1, 2].map(() =>
        transact(db => G.publishSource(db, { sourceId, version: 'v1', actorId: publisherActor, requestId: requestId() }))));
      assert.equal(publications.filter(result => result.status === 'fulfilled').length, 1, 'only one concurrent publication succeeds');
      assert.equal(publications.filter(result => result.status === 'rejected').length, 1);
      const withdrawalIds = [requestId(), requestId()];
      const withdrawals = await Promise.allSettled(withdrawalIds.map(id =>
        transact(db => G.withdrawSource(db, { sourceId, version: 'v1', actorId: publisherActor,
          reasonCode: 'SOURCE_ERROR', requestId: id }))));
      assert.equal(withdrawals.filter(result => result.status === 'fulfilled').length, 1, 'only one concurrent withdrawal succeeds');
      assert.equal(withdrawals.filter(result => result.status === 'rejected').length, 1);
      const committedWithdrawal = withdrawals.findIndex(result => result.status === 'fulfilled');
      await assert.rejects(() => transact(db => G.withdrawSource(db, { sourceId, version: 'v1', actorId: publisherActor,
        reasonCode: 'SOURCE_ERROR', requestId: withdrawalIds[committedWithdrawal] })), error => error.code === 'REQUEST_ALREADY_PROCESSED');

      const persisted = await pool.query(`SELECT approval_status,publication_status,reviewed_by,approval_actor_id,published_by,withdrawn_by
        FROM ai_knowledge_sources WHERE source_id=$1 AND version='v1'`, [sourceId]);
      assert.deepEqual(persisted.rows[0], { approval_status: 'WITHDRAWN', publication_status: 'WITHDRAWN',
        reviewed_by: 'stage4_reviewer', approval_actor_id: approvalActor,
        published_by: publisherActor, withdrawn_by: publisherActor });
      const events = await pool.query('SELECT event_type FROM ai_knowledge_events WHERE source_id=$1 ORDER BY created_at,event_type', [sourceId]);
      assert.deepEqual(events.rows.map(row => row.event_type).sort(), ['APPROVED','PUBLISHED','REVIEWED','WITHDRAWN']);

      const replacementId = 'stage4-supersession-source';
      await client.query(`INSERT INTO ai_knowledge_sources
        (source_id,version,title,document_type,authority_class,approval_status,approval_authority,approval_reference,approved_at,effective_at,
         supersedes_source_id,supersedes_version,confidentiality,confidentiality_reviewed_by,confidentiality_reviewed_at,
         permitted_audiences,permitted_uses,source_location,content_hash,language,ingested_at,created_by,
         publication_status,reviewed_by,review_decision,reviewed_at,approval_actor_id,published_by,published_at)
        VALUES($1,'v1','Old synthetic source','POLICY','OFFICIAL_POLICY','APPROVED','Synthetic council','TEST-RESOLUTION-OLD',1,1,
         NULL,NULL,'PUBLIC','Stage 4 reviewer',1,'["TEST"]','["TEST"]','Fixture section',$2,'en',1,'stage4_submitter',
         'PUBLISHED','stage4_reviewer','RECOMMEND_APPROVAL',1,'stage4_approver','stage4_publisher',1)`,
        [replacementId, 'e'.repeat(64)]);
      await client.query(`INSERT INTO ai_knowledge_sources
        (source_id,version,title,document_type,authority_class,approval_status,approval_authority,approval_reference,approved_at,effective_at,
         supersedes_source_id,supersedes_version,confidentiality,confidentiality_reviewed_by,confidentiality_reviewed_at,
         permitted_audiences,permitted_uses,source_location,content_hash,language,ingested_at,created_by,
         publication_status,reviewed_by,review_decision,reviewed_at,approval_actor_id)
        VALUES($1,'v2','Replacement synthetic source','POLICY','OFFICIAL_POLICY','APPROVED','Synthetic council','TEST-RESOLUTION-NEW',2,2,
         $1,'v1','PUBLIC','Stage 4 reviewer',2,'["TEST"]','["TEST"]','Fixture section',$2,'en',2,'stage4_submitter',
         'UNPUBLISHED','stage4_reviewer','RECOMMEND_APPROVAL',2,'stage4_approver')`,
        [replacementId, 'f'.repeat(64)]);
      const supersession = await transact(db => G.publishSource(db, { sourceId: replacementId, version: 'v2',
        actorId: 'stage4_publisher', requestId: requestId() }));
      assert.equal(supersession.supersededVersion, 'v1');
      const versions = await pool.query(`SELECT version,approval_status,publication_status,superseded_by_version
        FROM ai_knowledge_sources WHERE source_id=$1 ORDER BY version`, [replacementId]);
      assert.deepEqual(versions.rows, [
        { version: 'v1', approval_status: 'SUPERSEDED', publication_status: 'SUPERSEDED', superseded_by_version: 'v2' },
        { version: 'v2', approval_status: 'APPROVED', publication_status: 'PUBLISHED', superseded_by_version: null }
      ]);

      const K = require('../src/phase-b/governedKnowledge');
      const conflictTexts = new Map([
        ['stage4-conflict-a', 'Church policy synthetic matching passage.'],
        ['stage4-conflict-b', 'A different synthetic passage with no matching terms.']
      ]);
      for (const [conflictSourceId, content] of conflictTexts) {
        await client.query(`INSERT INTO ai_knowledge_sources
          (source_id,version,title,document_type,authority_class,approval_status,approval_authority,approval_reference,approved_at,
           confidentiality,confidentiality_reviewed_by,confidentiality_reviewed_at,permitted_audiences,permitted_uses,
           source_location,content_hash,language,ingested_at,created_by,publication_status,reviewed_by,review_decision,
           reviewed_at,approval_actor_id,published_by,published_at)
          VALUES($1,'v1','Synthetic conflict source','POLICY','OFFICIAL_POLICY','APPROVED','Synthetic council','TEST-RESOLUTION-CONFLICT',1,
           'PUBLIC','Stage 4 reviewer',1,'["PUBLIC"]','["INSTITUTIONAL"]','Conflict section',$2,'en',1,'stage4_submitter',
           'PUBLISHED','stage4_reviewer','RECOMMEND_APPROVAL',1,'stage4_approver','stage4_publisher',1)`,
          [conflictSourceId, K.sha256(content)]);
        await client.query(`INSERT INTO ai_knowledge_chunks(chunk_id,source_id,source_version,section,location,content,content_hash,created_at)
          VALUES($1,$2,'v1','Policy','Conflict section',$3,$4,1)`,
          [K.sha256(`${conflictSourceId}\0v1\0Conflict section\0${content}`), conflictSourceId, content, K.sha256(content)]);
      }
      const conflictResult = await transact(db => G.classifySourceConflict(db, { sources: [
        { sourceId: 'stage4-conflict-a', version: 'v1' }, { sourceId: 'stage4-conflict-b', version: 'v1' }
      ], actorId: 'stage4_reviewer', classification: 'AUTHORITY_SCOPE',
      classificationReference: 'TEST-REVIEW-CONFLICT-1', requestId: requestId() }));
      await transact(db => G.withdrawSource(db, { sourceId: 'stage4-conflict-b', version: 'v1',
        actorId: 'stage4_publisher', reasonCode: 'SOURCE_ERROR', requestId: requestId() }));
      const retrievedConflict = await K.retrieveInstitutionalContext(appDb.query, 'church policy');
      assert.equal(retrievedConflict.passages.length, 1, 'withdrawn source content is not retrieved');
      assert.deepEqual(retrievedConflict.conflicts, [{ conflictGroup: retrievedConflict.conflicts[0].conflictGroup,
        sourceIds: ['stage4-conflict-a','stage4-conflict-b'] }], 'withdrawing one side cannot hide its unresolved conflict');

      await client.query(`CREATE ROLE ${roleName} NOSUPERUSER NOLOGIN`);
      await client.query(`GRANT SELECT,UPDATE,DELETE,TRUNCATE ON ai_knowledge_events TO ${roleName}`);
      await client.query(`GRANT SELECT,UPDATE,DELETE,TRUNCATE ON ai_knowledge_sources TO ${roleName}`);
      await client.query(`GRANT SELECT,UPDATE,DELETE,TRUNCATE ON ai_knowledge_source_reviews TO ${roleName}`);
      await client.query(`GRANT SELECT,UPDATE,DELETE,TRUNCATE ON ai_knowledge_conflict_groups TO ${roleName}`);
      await client.query(`GRANT SELECT,UPDATE,DELETE,TRUNCATE ON ai_knowledge_conflict_sources TO ${roleName}`);
      await client.query(`GRANT SELECT,UPDATE,DELETE,TRUNCATE ON ai_scripture_corpus_events TO ${roleName}`);
      await client.query(`SET ROLE ${roleName}`);
      await assert.rejects(() => client.query('UPDATE ai_knowledge_events SET reason=\'tampered\''), /append-only/);
      await assert.rejects(() => client.query('UPDATE ai_knowledge_sources SET title=\'tampered\' WHERE source_id=$1', [sourceId]), /identity and content are immutable/);
      await assert.rejects(() => client.query(`UPDATE ai_knowledge_sources SET approval_status='APPROVED',publication_status='PUBLISHED'
        WHERE source_id=$1 AND version='v1'`, [sourceId]), /invalid AI knowledge (approval|publication) transition/);
      await assert.rejects(() => client.query('DELETE FROM ai_knowledge_sources WHERE source_id=$1', [sourceId]), /retained for audit/);
      await assert.rejects(() => client.query('DELETE FROM ai_knowledge_source_reviews'), /append-only/);
      await assert.rejects(() => client.query('UPDATE ai_knowledge_conflict_groups SET classification=\'OTHER\' WHERE conflict_group=$1', [conflictResult.conflictGroup]), /immutable conflict evidence/);
      await assert.rejects(() => client.query('DELETE FROM ai_knowledge_conflict_sources WHERE conflict_group=$1', [conflictResult.conflictGroup]), /immutable conflict evidence/);
      await assert.rejects(() => client.query('TRUNCATE ai_knowledge_conflict_sources'), /immutable conflict evidence/);
      await assert.rejects(() => client.query('TRUNCATE ai_scripture_corpus_events'), /append-only/);
      await client.query('RESET ROLE');
      const newConnection = await pool.query(`SELECT publication_status FROM ai_knowledge_sources WHERE source_id=$1 AND version='v1'`, [sourceId]);
      assert.equal(newConnection.rows[0].publication_status, 'WITHDRAWN', 'committed state survives a separate connection');
    } finally { client.release(); }
    const { reserveOnlineAiBudget } = require('../src/phase-b/onlineAiBudget');
    const budget = {
      subjectKey: 'synthetic-budget-test-key-0000000000',
      globalLimit: '4',
      now: new Date('2026-11-09T21:00:00Z')
    };
    const attempts = await Promise.allSettled(Array.from({ length: 6 }, () =>
      reserveOnlineAiBudget(transact, { ...budget, userId: 'synthetic-account' })));
    assert.deepEqual(attempts.filter(result => result.status === 'fulfilled')
      .map(result => result.value).sort(), [0, 1, 2]);
    assert.ok(attempts.filter(result => result.status === 'rejected')
      .every(result => result.reason.code === 'AI_QUOTA_EXCEEDED'));
    assert.equal(await reserveOnlineAiBudget(transact, { ...budget, userId: 'other-account' }), 2);
    await assert.rejects(reserveOnlineAiBudget(transact, { ...budget, userId: 'third-account' }),
      { code: 'AI_SERVICE_LIMIT_REACHED' });
    const dates = await pool.query('SELECT DISTINCT budget_date::text AS day FROM online_ai_daily_budget');
    const budgetDays = dates.rows.map(row => row.day).sort();
    assert.ok(budgetDays.includes('2026-11-10'), 'UTC 21:00 rolls over to Nairobi date November 10');
    assert.ok(budgetDays.includes(new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Nairobi' }).format(new Date())),
      'the authenticated online route reserves its own current Nairobi-day quota');
    process.stdout.write('PASS: atomic daily account/global quotas, rollback, and Nairobi midnight.\n');

    process.stdout.write(`PASS: PostgreSQL ${target.rows[0].version}; applied ${applied.join(', ')}; rollback, constraints, concurrent review, lifecycle persistence, replay defense, ordinary-role event immutability, and authenticated API permission gates.\n`);
  } finally {
    if (httpServer && typeof httpServer.closeAllConnections === 'function') httpServer.closeAllConnections();
    if (httpServer) await new Promise(resolve => httpServer.close(resolve));
    if (appDb) await appDb.closeDb();
    await pool.end();
    const active = await adminPool.query('SELECT pid,application_name,state FROM pg_stat_activity WHERE datname=$1', [dbName]);
    if (active.rows.length) process.stderr.write(`Unexpected active test database sessions: ${JSON.stringify(active.rows)}\n`);
    await adminPool.query(`DROP DATABASE IF EXISTS "${dbName}" WITH (FORCE)`);
    await adminPool.query(`DROP ROLE IF EXISTS ${roleName}`);
    await adminPool.end();
  }
}

run().catch(error => { console.error(error.stack || error); process.exitCode = 1; });
