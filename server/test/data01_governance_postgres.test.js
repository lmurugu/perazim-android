'use strict';

const assert = require('node:assert/strict');
const crypto = require('crypto');
const { Pool } = require('pg');
const { migrate } = require('../src/migrations');
const db = require('../src/db');
const { hasCurrentConsent, recordConsentChoice } = require('../src/phase-b/data01Governance');
const privacyRoutes = require('../src/routes/privacy');

async function invokeLast(method, routePath, userId, body, params = {}) {
  const layer = privacyRoutes.stack.find(item => item.route && item.route.path === routePath && item.route.methods[method]);
  assert(layer, `route ${routePath} exists`);
  const handler = layer.route.stack[layer.route.stack.length - 1].handle;
  const res = { statusCode: 200, status(n) { this.statusCode = n; return this; }, json(value) { this.body = value; return this; } };
  await handler({ user: { id: userId }, body: body || {}, params }, res);
  return res;
}

async function run() {
  const adminUrl = new URL(process.env.PHASE_B_TEST_ADMIN_URL || '');
  assert.equal(adminUrl.pathname, '/postgres');
  assert(['127.0.0.1', 'localhost', '[::1]'].includes(adminUrl.hostname));
  const admin = new Pool({ connectionString: adminUrl.toString() });
  const database = `perazim_phaseb_governance_${crypto.randomBytes(7).toString('hex')}_test`;
  let pool;
  try {
    await admin.query(`CREATE DATABASE "${database}"`);
    adminUrl.pathname = `/${database}`;
    pool = new Pool({ connectionString: adminUrl.toString() });
    await migrate(pool);
    const expectedMigrationNames = [
      '0001_current_schema_baseline.sql','0002_membership_permissions.sql','0003_membership_claim_rate_limits.sql',
      '0004_membership_identity_and_revisions.sql','0005_member_contact_auto_verification.sql',
      '0006_privacy_notice_acknowledgements.sql','0007_disable_contact_list_verification.sql',
      '0008_membership_import_staging.sql','0009_legacy_identity_reconciliation.sql',
      '0010_data01_privacy_governance.sql','0011_identity_revision_pin.sql'
    ];
    const ledger = await pool.query('SELECT version,name FROM perazim_schema_migrations ORDER BY version');
    assert.deepEqual(ledger.rows.map(row => row.name), expectedMigrationNames, 'migration ledger contains the exact ordered filenames 0001 through 0011');
    process.env.NODE_ENV = 'test';
    process.env.DATABASE_URL = adminUrl.toString();
    await db.initDb();
    await db.query(`INSERT INTO users(id,phone,pin_hash,name,campus_id,role,is_active,created_at,updated_at)
      VALUES ('synthetic_governance_user','+254700000211','fixture','Synthetic Governance User','campus_embu','MEMBER',TRUE,1,1)`);
    await db.query(`INSERT INTO membership_verifications(user_id,status,verification_source,revision,updated_at)
      VALUES ('synthetic_governance_user','REGISTERED','MANUAL',1,1)`);
    const authorityBefore = await db.query(`SELECT u.role,v.status,
      (SELECT count(*)::int FROM privacy_notice_acknowledgements a WHERE a.user_id=u.id) AS acknowledgements,
      (SELECT count(*)::int FROM user_permissions p WHERE p.user_id=u.id) AS permissions
      FROM users u JOIN membership_verifications v ON v.user_id=u.id WHERE u.id='synthetic_governance_user'`);

    await db.query(`INSERT INTO privacy_policy_versions(version,document_kind,content_hash,document_path,publication_status,is_active,created_at)
      VALUES ('SYNTHETIC_POLICY_DRAFT','PUBLIC_POLICY',$1,'docs/data-protection/public/synthetic.md','DRAFT',FALSE,1)`, ['c'.repeat(64)]);
    await assert.rejects(() => db.query(`UPDATE privacy_policy_versions SET content_hash=$1 WHERE version='SYNTHETIC_POLICY_DRAFT'`, ['d'.repeat(64)]), /identity and content are immutable/);
    await assert.rejects(() => db.query(`DELETE FROM privacy_policy_versions WHERE version='SYNTHETIC_POLICY_DRAFT'`), /append-only/);
    await db.query(`INSERT INTO privacy_policy_versions(version,document_kind,content_hash,document_path,publication_status,effective_at,is_active,created_at)
      VALUES ('SYNTHETIC_POLICY_EFFECTIVE_A','PUBLIC_POLICY',$1,'docs/data-protection/public/a.md','PUBLISHED',1,TRUE,1)`, ['e'.repeat(64)]);
    await assert.rejects(() => db.query(`INSERT INTO privacy_policy_versions(version,document_kind,content_hash,document_path,publication_status,effective_at,is_active,created_at)
      VALUES ('SYNTHETIC_POLICY_EFFECTIVE_B','PUBLIC_POLICY',$1,'docs/data-protection/public/b.md','PUBLISHED',1,TRUE,1)`, ['f'.repeat(64)]), /privacy_policy_one_active_published_per_kind/);
    await db.query(`INSERT INTO privacy_publication_events(id,document_version,event,reason_code,created_at)
      VALUES ($1,'SYNTHETIC_POLICY_DRAFT','SUBMITTED','SYNTHETIC_TEST',2)`, [crypto.randomUUID()]);
    await assert.rejects(() => db.query(`UPDATE privacy_publication_events SET reason_code='EDITED'`), /append-only/);
    await db.query(`INSERT INTO privacy_presentation_receipts(receipt_hash,user_id,notice_version,content_hash,presented_at,expires_at)
      VALUES($1,'synthetic_governance_user','0.1',$2,10,20)`, ['9'.repeat(64), '4'.repeat(64)]);
    await assert.rejects(() => db.query(`DELETE FROM privacy_presentation_receipts WHERE receipt_hash=$1`, ['9'.repeat(64)]), /append-only/);

    const empty = await db.query('SELECT count(*)::int AS n FROM consent_purposes');
    assert.equal(empty.rows[0].n, 0, 'production consent catalogue starts empty');
    await assert.rejects(() => db.query(`INSERT INTO consent_events(id,user_id,purpose_key,wording_version,wording_hash,decision,channel,created_at)
      VALUES($1,'synthetic_governance_user','MISSING_PURPOSE','1',$2,'GRANTED','TEST',1)`, [crypto.randomUUID(), 'a'.repeat(64)]));

    await db.query(`INSERT INTO consent_purposes(purpose_key,wording_version,wording_hash,purpose_text,lawful_basis_reference,active,created_at)
      VALUES('SYNTHETIC_PURPOSE','1',$1,'Synthetic test only','SYNTHETIC_TEST_ONLY',TRUE,1)`, ['b'.repeat(64)]);
    assert.equal(await db.withTransaction(client => recordConsentChoice(client, { userId: 'synthetic_governance_user', purposeKey: 'SYNTHETIC_PURPOSE', decision: 'GRANTED', now: 2 })), true);
    assert.equal(await db.withTransaction(client => hasCurrentConsent(client, 'synthetic_governance_user', 'SYNTHETIC_PURPOSE')), true);
    await db.withTransaction(client => recordConsentChoice(client, { userId: 'synthetic_governance_user', purposeKey: 'SYNTHETIC_PURPOSE', decision: 'WITHDRAWN', now: 3 }));
    assert.equal(await db.withTransaction(client => hasCurrentConsent(client, 'synthetic_governance_user', 'SYNTHETIC_PURPOSE')), false, 'withdrawal stops processing authorization at service check');
    await assert.rejects(() => db.query("UPDATE consent_events SET decision='GRANTED'"), /append-only/);
    const membershipStatus = await db.query(`SELECT status FROM membership_verifications WHERE user_id='synthetic_governance_user'`);
    assert.equal(membershipStatus.rows[0].status, 'REGISTERED', 'consent choice does not change membership status');
    const authorityAfter = await db.query(`SELECT u.role,v.status,
      (SELECT count(*)::int FROM privacy_notice_acknowledgements a WHERE a.user_id=u.id) AS acknowledgements,
      (SELECT count(*)::int FROM user_permissions p WHERE p.user_id=u.id) AS permissions
      FROM users u JOIN membership_verifications v ON v.user_id=u.id WHERE u.id='synthetic_governance_user'`);
    assert.deepEqual(authorityAfter.rows[0], authorityBefore.rows[0], 'consent does not change role, notice acknowledgement or administrator authority');

    const submitted = await invokeLast('post', '/requests', 'synthetic_governance_user', {
      request_id: '32bb7163-2bec-4926-bdf7-542c967f4433', request_type: 'ACCESS', narrative: 'SYNTHETIC_NARRATIVE_NOT_STORED'
    });
    assert.equal(submitted.statusCode, 202);
    const exposed = await db.query('SELECT count(*)::int AS n FROM data_subject_requests WHERE requester_id<>$1', ['synthetic_governance_user']);
    assert.equal(exposed.rows[0].n, 0);
    const narrative = await db.query("SELECT to_jsonb(r)::text AS value FROM data_subject_requests r");
    assert.ok(!narrative.rows[0].value.includes('SYNTHETIC_NARRATIVE_NOT_STORED'));
    const otherRequester = await invokeLast('get', '/requests/:requestId', 'synthetic_other_user', {}, { requestId: '32bb7163-2bec-4926-bdf7-542c967f4433' });
    assert.equal(otherRequester.statusCode, 404, 'mismatched requester receives no request data');
    console.log('DATA-01 governance PostgreSQL integration passed: empty consent catalogue, purpose FK, grant/withdraw point-of-use, append-only evidence, neutral requester scope.');
  } finally {
    await db.closeDb();
    if (pool) await pool.end();
    await admin.query(`DROP DATABASE IF EXISTS "${database}" WITH (FORCE)`);
    await admin.end();
  }
}

run().catch(error => { console.error(error.stack || error); process.exitCode = 1; });
