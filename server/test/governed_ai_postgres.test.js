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

      const structure = await client.query(`
        SELECT (SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN
          ('ai_knowledge_sources','ai_knowledge_chunks','ai_knowledge_events','ai_knowledge_source_reviews',
           'ai_knowledge_conflict_groups','ai_knowledge_conflict_sources','ai_scripture_corpora',
           'ai_scripture_corpus_events','ai_scripture_books','ai_scripture_verses'))::int AS tables,
          (SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname IN
           ('ai_knowledge_one_published_version','ai_scripture_one_published_translation'))::int AS indexes,
          (SELECT active FROM permissions WHERE code='knowledge.source.approve.institutional') AS approval_active`);
      assert.equal(structure.rows[0].tables, 10);
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
      app.use(express.json());
      app.use('/api/knowledge-sources', require('../src/routes/knowledgeGovernance'));
      httpServer = await new Promise((resolve, reject) => {
        const server = app.listen(0, '127.0.0.1', () => resolve(server));
        server.once('error', reject);
      });
      const base = `http://127.0.0.1:${httpServer.address().port}/api/knowledge-sources`;
      const headers = { authorization: `Bearer ${token}`, 'x-privileged-session': stepUp, 'content-type': 'application/json' };
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
