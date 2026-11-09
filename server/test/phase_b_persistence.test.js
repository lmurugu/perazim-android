const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');
const { Pool } = require('pg');
const { migrate, LEDGER } = require('../src/migrations');
const { assertDisposableTestDatabase } = require('./test_database_safety');
const { newId, assertPhaseBTestDatabase, createSyntheticExistingFixture, tableNames, rowsSnapshot, catalogSnapshot } = require('./phase_b_fixture');

const PERMISSION_CODES = [
  'membership.status.self.read',
  'membership.claim.self.submit',
  'membership.record.read',
  'membership.register.import',
  'membership.claim.review',
  'membership.verification.confirm',
  'membership.record.correct',
  'membership.verification.correct',
  'membership.verification.revoke',
  'access.permissions.self.read',
  'admin.permissions.read',
  'admin.permissions.grant',
  'admin.permissions.revoke',
  'admin.audit.read',
  'privacy.document.publish'
];
const EXPECTED_MIGRATION_ADDED_TABLES = [
  'admin_audit_log',
  'admin_security',
  'consent_events',
  'consent_purposes',
  'data_subject_request_events',
  'data_subject_request_revisions',
  'data_subject_requests',
  'disposal_events',
  'legal_holds',
  'membership_account_identities',
  'membership_account_identity_history',
  'membership_claim_rate_limits',
  'membership_claims',
  'membership_identity_reconciliation_events',
  'membership_identity_reconciliation_requests',
  'membership_identity_reconciliation_revisions',
  'membership_import_staging_events',
  'membership_import_staging_rows',
  'membership_imports',
  'membership_record_revisions',
  'membership_records',
  'membership_verification_history',
  'membership_verifications',
  'permissions',
  'pilot_bootstrap',
  'privacy_notice_acknowledgements',
  'privacy_notice_versions',
  'privacy_policy_versions',
  'privacy_presentation_receipts',
  'privacy_publication_events',
  'privileged_sessions',
  'retention_reviews',
  'retention_rules',
  'user_permissions'
];
const EXPECTED_MIGRATIONS = [
  '0001_current_schema_baseline.sql',
  '0002_membership_permissions.sql',
  '0003_membership_claim_rate_limits.sql',
  '0004_membership_identity_and_revisions.sql',
  '0005_member_contact_auto_verification.sql',
  '0006_privacy_notice_acknowledgements.sql',
  '0007_disable_contact_list_verification.sql',
  '0008_membership_import_staging.sql',
  '0009_legacy_identity_reconciliation.sql',
  '0010_data01_privacy_governance.sql',
  '0011_identity_revision_pin.sql',
  '0012_membership_verification_history_append_only.sql'
];

const EXPECTED_ALL_TABLES = [
  'admin_audit_log', 'admin_security', 'campuses', 'community_classes', 'community_groups', 'community_media',
  'community_posts', 'connections', 'consent_events', 'consent_purposes', 'daily_challenges',
  'data_subject_request_events', 'data_subject_request_revisions', 'data_subject_requests', 'disposal_events',
  'event_rsvps', 'group_memberships', 'in_app_notifications', 'legal_holds', 'membership_account_identities',
  'membership_account_identity_history', 'membership_claim_rate_limits', 'membership_claims',
  'membership_identity_reconciliation_events', 'membership_identity_reconciliation_requests',
  'membership_identity_reconciliation_revisions', 'membership_import_staging_events', 'membership_import_staging_rows',
  'membership_imports', 'membership_record_revisions', 'membership_records', 'membership_verification_history',
  'membership_verifications', 'messages', 'permissions', 'pilot_bootstrap', 'post_amens', 'post_comments',
  'prayer_amens', 'prayers', 'privacy_notice_acknowledgements', 'privacy_notice_versions', 'privacy_policy_versions',
  'privacy_presentation_receipts', 'privacy_publication_events', 'privileged_sessions', 'retention_reviews',
  'retention_rules', 'revoked_tokens', 'sync_events', 'user_challenge_completions', 'user_permissions', 'users'
];
const EXPECTED_FUNCTIONS = [
  'capture_membership_registration_identity',
  'protect_privacy_policy_version',
  'protect_privacy_presentation_receipt',
  'reject_admin_audit_mutation',
  'reject_data01_evidence_mutation',
  'reject_data01_row_mutation',
  'reject_membership_account_identity_history_mutation',
  'reject_membership_identity_reconciliation_event_mutation',
  'reject_membership_identity_reconciliation_revision_mutation',
  'reject_membership_import_staging_event_mutation',
  'reject_membership_record_revision_mutation',
  'reject_membership_verification_history_mutation',
  'reject_privacy_acknowledgement_mutation'
];
const EXPECTED_TRIGGERS = [
  'admin_audit_log.admin_audit_log_no_row_mutation',
  'admin_audit_log.admin_audit_log_no_truncate',
  'consent_events.consent_events_no_row_mutation',
  'consent_events.consent_events_no_truncate',
  'data_subject_request_events.data_subject_request_events_no_row_mutation',
  'data_subject_request_events.data_subject_request_events_no_truncate',
  'data_subject_request_revisions.data_subject_request_revisions_no_row_mutation',
  'data_subject_request_revisions.data_subject_request_revisions_no_truncate',
  'disposal_events.disposal_events_no_row_mutation',
  'disposal_events.disposal_events_no_truncate',
  'membership_account_identity_history.membership_account_identity_history_no_row_mutation',
  'membership_account_identity_history.membership_account_identity_history_no_truncate',
  'membership_identity_reconciliation_events.membership_identity_reconciliation_events_no_row_mutation',
  'membership_identity_reconciliation_events.membership_identity_reconciliation_events_no_truncate',
  'membership_identity_reconciliation_revisions.membership_identity_reconciliation_revisions_no_row_mutation',
  'membership_identity_reconciliation_revisions.membership_identity_reconciliation_revisions_no_truncate',
  'membership_import_staging_events.membership_import_staging_events_no_row_mutation',
  'membership_import_staging_events.membership_import_staging_events_no_truncate',
  'membership_record_revisions.membership_record_revisions_no_row_mutation',
  'membership_record_revisions.membership_record_revisions_no_truncate',
  'membership_verification_history.membership_verification_history_no_row_mutation',
  'membership_verification_history.membership_verification_history_no_truncate',
  'privacy_notice_acknowledgements.privacy_ack_no_truncate',
  'privacy_notice_acknowledgements.privacy_ack_no_update_delete',
  'privacy_policy_versions.privacy_policy_version_identity_immutable',
  'privacy_policy_versions.privacy_policy_versions_no_delete',
  'privacy_policy_versions.privacy_policy_versions_no_truncate',
  'privacy_presentation_receipts.privacy_presentation_receipt_single_use',
  'privacy_presentation_receipts.privacy_presentation_receipts_no_delete',
  'privacy_presentation_receipts.privacy_presentation_receipts_no_truncate',
  'privacy_publication_events.privacy_publication_events_no_row_mutation',
  'privacy_publication_events.privacy_publication_events_no_truncate',
  'retention_reviews.retention_reviews_no_row_mutation',
  'retention_reviews.retention_reviews_no_truncate',
  'users.capture_membership_registration_identity'
];

function assertMigrationFileNames(directory) {
  assert.deepStrictEqual(
    fs.readdirSync(directory).filter(name => name.endsWith('.sql')).sort(),
    EXPECTED_MIGRATIONS,
    'migration directory contains exactly the approved ordered migration filenames'
  );
}

function verifyMigrationFileNameGuard() {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'perazim-migration-name-fixture-'));
  try {
    for (const name of EXPECTED_MIGRATIONS) fs.writeFileSync(path.join(directory, name), '');
    assertMigrationFileNames(directory);

    fs.unlinkSync(path.join(directory, EXPECTED_MIGRATIONS[4]));
    assert.throws(() => assertMigrationFileNames(directory), assert.AssertionError,
      'a removed migration filename must fail the exact-list check');
    fs.writeFileSync(path.join(directory, EXPECTED_MIGRATIONS[4]), '');

    fs.writeFileSync(path.join(directory, '0099_unexpected_migration.sql'), '');
    assert.throws(() => assertMigrationFileNames(directory), assert.AssertionError,
      'an extra migration filename must fail the exact-list check');
    console.log('PASS exact migration filename guard detects a missing file and an extra file in temporary filename fixtures.');
  } finally {
    fs.rmSync(directory, { recursive: true, force: true });
  }
}

function adminUrl() {
  if (process.env.NODE_ENV === 'production') throw new Error('SAFETY ABORT: Phase B persistence tests cannot run in production.');
  if (!process.env.PHASE_B_TEST_ADMIN_URL) throw new Error('Set PHASE_B_TEST_ADMIN_URL to a local PostgreSQL admin URL targeting the maintenance database named postgres.');
  const url = new URL(process.env.PHASE_B_TEST_ADMIN_URL);
  const database = decodeURIComponent(url.pathname.slice(1));
  const host = (url.searchParams.get('host') || url.hostname).replace(/^\[|\]$/g, '').toLowerCase();
  if (!['postgres:', 'postgresql:'].includes(url.protocol)) throw new Error('SAFETY ABORT: Phase B tests require a PostgreSQL URL.');
  if (database !== 'postgres') throw new Error('SAFETY ABORT: Phase B admin URL must target the postgres maintenance database.');
  if (!(host === 'localhost' || host === '127.0.0.1' || host === '::1' || host.startsWith('/tmp/'))) throw new Error('SAFETY ABORT: Phase B tests may only connect to a local PostgreSQL cluster.');
  return url;
}

function databaseUrl(admin, database) {
  const url = new URL(admin.toString());
  url.pathname = `/${database}`;
  return url.toString();
}

function assertGeneratedDatabase(name) {
  if (!/^perazim_phaseb_[a-z0-9_]+_test$/.test(name)) throw new Error('SAFETY ABORT: Phase B fixtures only create perazim_phaseb_*_test database names.');
  assertDisposableTestDatabase(`postgres://fixture@localhost/${name}`);
}

async function createDatabase(pool, name) {
  assertGeneratedDatabase(name);
  await pool.query(`CREATE DATABASE "${name}"`);
}

async function dropDatabase(pool, name) {
  assertGeneratedDatabase(name);
  await pool.query(`DROP DATABASE IF EXISTS "${name}" WITH (FORCE)`);
}

async function insertMembershipFixture(pool, fixture, suffix) {
  await assertPhaseBTestDatabase(pool);
  const now = Date.now();
  const importId = newId();
  const recordId = newId();
  const claimId = newId();
  const requestId = newId();
  const auditId = newId();
  await pool.query('INSERT INTO membership_imports (id,source,checksum,actor_id,created_at) VALUES ($1,$2,$3,$4,$5)', [importId, 'synthetic-fixture', crypto.createHash('sha256').update(suffix).digest('hex'), fixture.owner, now]);
  await pool.query(`INSERT INTO membership_records (id,import_id,reference,full_name,phone,record_status,created_at,updated_at) VALUES ($1,$2,$3,'Synthetic Owner','+254700000001','ACTIVE',$4,$4)`, [recordId, importId, `FIX-${suffix}`, now]);
  await pool.query('INSERT INTO membership_claims (id,user_id,request_id,user_name,user_phone,status,created_at,updated_at) VALUES ($1,$2,$3,$4,$5,$6,$7,$7)', [claimId, fixture.owner, requestId, 'Synthetic Owner', '+254700000001', 'APPROVED', now]);
  await pool.query(`INSERT INTO membership_verifications (user_id,membership_record_id,status,updated_at) VALUES ($1,$2,'VERIFIED_MEMBER',$3)`, [fixture.owner, recordId, now]);
  await pool.query('INSERT INTO membership_verifications (user_id,updated_at) VALUES ($1,$2)', [fixture.member, now]);
  await pool.query('INSERT INTO membership_verification_history (id,user_id,claim_id,previous_status,new_status,actor_id,reason,created_at) VALUES ($1,$2,$3,$4,$5,$6,$7,$8)', [newId(), fixture.owner, claimId, 'VERIFICATION_PENDING', 'VERIFIED_MEMBER', fixture.owner, 'Synthetic fixture approval', now]);
  await pool.query(`INSERT INTO user_permissions (id,user_id,permission_code,scope,granted_by,granted_at,effective_at) VALUES ($1,$2,'membership.status.self.read','global',$3,$4,$4)`, [newId(), fixture.member, fixture.owner, now]);
  await pool.query('INSERT INTO admin_security (user_id,totp_secret_cipher,totp_last_counter,security_version,auth_not_before,created_at) VALUES ($1,$2,8,1,$3,$3)', [fixture.owner, 'synthetic-encrypted-fixture-only', now - 1000]);
  await pool.query('INSERT INTO pilot_bootstrap (singleton,user_id,approval_ref,created_at) VALUES (TRUE,$1,$2,$3)', [fixture.owner, `synthetic-approval-${suffix}`, now]);
  await pool.query('INSERT INTO privileged_sessions (token_hash,user_id,security_version,auth_token_hash,expires_at,created_at) VALUES ($1,$2,1,$3,$4,$5)', ['1'.repeat(64), fixture.owner, '2'.repeat(64), now + 60000, now]);
  await pool.query('INSERT INTO admin_audit_log (id,actor_id,action,resource_type,resource_id,outcome,reason,request_id,created_at) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)', [auditId, fixture.owner, 'FIXTURE_CHECK', 'membership', recordId, 'SUCCESS', 'Synthetic fixture event', `req-${suffix}`, now]);
  return { importId, recordId, claimId, auditId, createdAt: now };
}

async function applyBaselineOnly(pool, directory, baselinePath) {
  fs.mkdirSync(directory, { recursive: true });
  fs.copyFileSync(baselinePath, path.join(directory, '0001_current_schema_baseline.sql'));
  await migrate(pool, directory);
}

async function run() {
  const admin = adminUrl();
  const adminPool = new Pool({ connectionString: admin.toString() });
  const token = `${process.pid}_${crypto.randomBytes(5).toString('hex')}`;
  const sourceName = `perazim_phaseb_source_${token}_test`;
  const restoreName = `perazim_phaseb_restore_${token}_test`;
  const failureName = `perazim_phaseb_failure_${token}_test`;
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'perazim-phase-b-fixture-'));
  const migrationsDir = path.resolve(__dirname, '../migrations');
  assertMigrationFileNames(migrationsDir);
  const baselinePath = path.join(migrationsDir, '0001_current_schema_baseline.sql');
  const fullMigrationPath = path.join(migrationsDir, '0002_membership_permissions.sql');
  const rateLimitPath = path.join(migrationsDir, '0003_membership_claim_rate_limits.sql');
  const identityMigration = '0004_membership_identity_and_revisions.sql';
  const identityPath = path.join(migrationsDir, identityMigration);
  const baselineOnlyDir = path.join(tempDir, 'baseline-only');
  const failureDir = path.join(tempDir, 'failure-migrations');
  let sourcePool;
  let restorePool;
  let failurePool;
  try {
    assertGeneratedDatabase(sourceName);
    assertGeneratedDatabase(restoreName);
    assertGeneratedDatabase(failureName);
    const adminInfo = await adminPool.query('SELECT current_database() AS name,current_setting(\'server_version_num\')::int AS version');
    assert.strictEqual(adminInfo.rows[0].name, 'postgres');
    assert(adminInfo.rows[0].version >= 130000, 'PostgreSQL 13+ supports isolated forced cleanup');

    await createDatabase(adminPool, sourceName);
    sourcePool = new Pool({ connectionString: databaseUrl(admin, sourceName) });
    await assertPhaseBTestDatabase(sourcePool);
    await applyBaselineOnly(sourcePool, baselineOnlyDir, baselinePath);
    const fixture = await createSyntheticExistingFixture(sourcePool, crypto.randomBytes(4).toString('hex'));
    const existingTables = await tableNames(sourcePool, [LEDGER]);
    const existingRowsBefore = await rowsSnapshot(sourcePool, existingTables);
    const existingCatalogBefore = await catalogSnapshot(sourcePool, existingTables);

    const firstRun = await migrate(sourcePool);
    assert.deepStrictEqual(firstRun.applied.map(migration => migration.name), EXPECTED_MIGRATIONS, 'ordered migration filenames are recorded exactly');
    assert.strictEqual(firstRun.applied[1].name, '0002_membership_permissions.sql');
    assert.strictEqual(firstRun.applied[2].name, '0003_membership_claim_rate_limits.sql');
    assert.strictEqual(firstRun.applied[3].name, identityMigration);
    const newTables = (await tableNames(sourcePool, [LEDGER])).filter(name => !existingTables.includes(name));
    assert.deepStrictEqual(newTables, EXPECTED_MIGRATION_ADDED_TABLES, 'migrations add exactly the approved tables');
    assert.deepStrictEqual(await tableNames(sourcePool, [LEDGER]), EXPECTED_ALL_TABLES, 'migrations produce exactly the approved combined table set');
    const checksum = (await sourcePool.query(`SELECT checksum FROM public.${LEDGER} WHERE version='0002'`)).rows[0].checksum;
    assert.match(checksum, /^[a-f0-9]{64}$/);
    assert.strictEqual(await rowsSnapshot(sourcePool, existingTables), existingRowsBefore, 'B2 preserves every pre-existing public table row');
    const migratedCatalog = JSON.parse(await catalogSnapshot(sourcePool));
    assert.deepStrictEqual(migratedCatalog.routines.map(routine => routine.name), EXPECTED_FUNCTIONS, 'migrations create exactly the approved functions');
    assert.deepStrictEqual(migratedCatalog.triggers.map(trigger => `${trigger.table_name}.${trigger.name}`), EXPECTED_TRIGGERS, 'migrations create exactly the approved triggers');
    const existingCatalogAfter = JSON.parse(await catalogSnapshot(sourcePool, existingTables));
    assert.strictEqual(existingCatalogAfter.triggers.filter(trigger =>
      trigger.table_name === 'users' && trigger.name === 'capture_membership_registration_identity').length, 1);
    existingCatalogAfter.triggers = existingCatalogAfter.triggers.filter(trigger =>
      trigger.table_name !== 'users' || trigger.name !== 'capture_membership_registration_identity');
    const newRoutines = EXPECTED_FUNCTIONS.filter(name => name !== 'reject_admin_audit_mutation');
    assert.strictEqual(existingCatalogAfter.routines.filter(routine => newRoutines.includes(routine.name)).length, newRoutines.length);
    existingCatalogAfter.routines = existingCatalogAfter.routines.filter(routine => !newRoutines.includes(routine.name));
    assert.deepStrictEqual(existingCatalogAfter, JSON.parse(existingCatalogBefore), 'only the approved capture trigger and new routines extend the pre-existing schema');
    assert.strictEqual((await sourcePool.query('SELECT count(*)::int AS count FROM membership_account_identities')).rows[0].count, 0, 'legacy display names are not backfilled as registration identities');
    assert.strictEqual((await sourcePool.query('SELECT grace_points FROM users WHERE id=$1', [fixture.owner])).rows[0].grace_points, 0);
    assert.strictEqual((await sourcePool.query('SELECT grace_points FROM users WHERE id=$1', [fixture.member])).rows[0].grace_points, 9);

    const permissions = await sourcePool.query('SELECT code,active FROM permissions ORDER BY code');
    assert.deepStrictEqual(permissions.rows.map(row => row.code), [...PERMISSION_CODES].sort(), 'only the canonical permission definitions are seeded');
    assert(permissions.rows.every(row => row.active === true));
    assert.strictEqual((await sourcePool.query('SELECT count(*)::int AS count FROM user_permissions')).rows[0].count, 0, 'migration creates no permission grants');
    assert.strictEqual((await sourcePool.query("SELECT count(*)::int AS count FROM user_permissions WHERE permission_code='privacy.document.publish'")).rows[0].count, 0, 'migration creates no privacy publication permission holders');
    assert.strictEqual((await sourcePool.query("SELECT column_default FROM information_schema.columns WHERE table_schema='public' AND table_name='membership_verifications' AND column_name='status'")).rows[0].column_default, "'REGISTERED'::text");
    assert.strictEqual((await sourcePool.query("SELECT column_default FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_security' AND column_name='auth_not_before'")).rows[0].column_default, '0');
    const membershipFixture = await insertMembershipFixture(sourcePool, fixture, token);
    await sourcePool.query(
      `INSERT INTO membership_record_revisions (record_id,revision,full_name,phone,updated_at)
       SELECT id,revision,full_name,phone,updated_at FROM membership_records WHERE id=$1`, [membershipFixture.recordId]
    );
    await sourcePool.query("UPDATE membership_records SET revision=2,full_name='Corrected Synthetic Owner' WHERE id=$1", [membershipFixture.recordId]);
    assert.strictEqual((await sourcePool.query("SELECT count(*)::int AS count FROM membership_verifications WHERE status='REGISTERED'")).rows[0].count, 1, 'membership verification defaults to REGISTERED');
    await assert.rejects(() => sourcePool.query("UPDATE membership_verifications SET status='VERIFIED_MEMBER',membership_record_id=NULL WHERE user_id=$1", [fixture.member]));
    await assert.rejects(() => sourcePool.query(`UPDATE admin_audit_log SET reason='changed' WHERE id=$1`, [membershipFixture.auditId]), /append-only/);
    await assert.rejects(() => sourcePool.query(`DELETE FROM admin_audit_log WHERE id=$1`, [membershipFixture.auditId]), /append-only/);
    await assert.rejects(() => sourcePool.query('TRUNCATE admin_audit_log'), /append-only/);
    assert.strictEqual((await sourcePool.query('SELECT count(*)::int AS count FROM admin_audit_log')).rows[0].count, 1, 'audit row survives rejected updates, deletes, and truncate');
    const verificationHistoryId = newId();
    const historyCountBefore = (await sourcePool.query('SELECT count(*)::int AS count FROM membership_verification_history')).rows[0].count;
    await sourcePool.query(`INSERT INTO membership_verification_history
      (id,user_id,claim_id,previous_status,new_status,actor_id,reason,created_at)
      VALUES ($1,$2,$3,'REGISTERED','VERIFIED_MEMBER',$2,'SYNTHETIC_TEST',$4)`,
    [verificationHistoryId, fixture.owner, membershipFixture.claimId, Date.now()]);
    await assert.rejects(() => sourcePool.query(`UPDATE membership_verification_history SET reason='changed' WHERE id=$1`, [verificationHistoryId]), /append-only/);
    await assert.rejects(() => sourcePool.query(`DELETE FROM membership_verification_history WHERE id=$1`, [verificationHistoryId]), /append-only/);
    await assert.rejects(() => sourcePool.query('TRUNCATE membership_verification_history'), /append-only/);
    assert.strictEqual((await sourcePool.query('SELECT count(*)::int AS count FROM membership_verification_history')).rows[0].count, historyCountBefore + 1,
      'verification history accepts inserts and survives rejected updates, deletes, and truncate');
    await assert.rejects(() => sourcePool.query("UPDATE membership_records SET record_status='UNKNOWN' WHERE id=$1", [membershipFixture.recordId]));
    await assert.rejects(() => sourcePool.query('UPDATE membership_records SET revision=0 WHERE id=$1', [membershipFixture.recordId]));
    await assert.rejects(() => sourcePool.query("UPDATE membership_verifications SET membership_record_id=$1,status='VERIFIED_MEMBER' WHERE user_id=$2", [membershipFixture.recordId, fixture.member]), /duplicate key/);
    await assert.rejects(() => sourcePool.query(`INSERT INTO user_permissions (id,user_id,permission_code,scope,granted_by,granted_at,effective_at,expires_at) VALUES ($1,$2,'membership.status.self.read','global',$3,$4,$4,$4)`, [newId(), fixture.member, fixture.owner, Date.now()]));
    const secondRun = await migrate(sourcePool);
    assert.deepStrictEqual(secondRun.applied.map(migration => migration.name), EXPECTED_MIGRATIONS, 'repeat migration leaves the exact ledger unchanged');
    assert.strictEqual((await sourcePool.query(`SELECT checksum FROM public.${LEDGER} WHERE version='0002'`)).rows[0].checksum, checksum);

    await createDatabase(adminPool, failureName);
    failurePool = new Pool({ connectionString: databaseUrl(admin, failureName) });
    await assertPhaseBTestDatabase(failurePool);
    await applyBaselineOnly(failurePool, path.join(tempDir, 'failure-baseline'), baselinePath);
    fs.mkdirSync(failureDir);
    fs.copyFileSync(baselinePath, path.join(failureDir, '0001_current_schema_baseline.sql'));
    fs.copyFileSync(fullMigrationPath, path.join(failureDir, '0002_membership_permissions.sql'));
    fs.copyFileSync(rateLimitPath, path.join(failureDir, '0003_membership_claim_rate_limits.sql'));
    fs.copyFileSync(identityPath, path.join(failureDir, identityMigration));
    fs.writeFileSync(path.join(failureDir, '0005_synthetic_rollback.sql'), 'CREATE TABLE public.phase_b_rollback_probe (id integer); SELECT 1 / 0;\n');
    await assert.rejects(() => migrate(failurePool, failureDir), /division by zero/);
    assert.deepStrictEqual((await failurePool.query(`SELECT version FROM public.${LEDGER} ORDER BY version`)).rows.map(row => row.version), ['0001'], 'failed B2 transaction leaves only its prior baseline ledger row');
    assert.strictEqual((await failurePool.query("SELECT to_regclass('public.membership_imports') AS relation")).rows[0].relation, null, 'failed B2 DDL rolls back');
    assert.strictEqual((await failurePool.query("SELECT to_regclass('public.membership_claim_rate_limits') AS relation")).rows[0].relation, null, 'failed later migration rolls back SEC-01 DDL');
    assert.strictEqual((await failurePool.query("SELECT to_regclass('public.membership_record_revisions') AS relation")).rows[0].relation, null, 'failed later migration rolls back revision DDL');
    assert.strictEqual((await failurePool.query("SELECT to_regclass('public.membership_account_identities') AS relation")).rows[0].relation, null, 'failed later migration rolls back identity DDL');
    assert.strictEqual((await failurePool.query("SELECT to_regclass('public.phase_b_rollback_probe') AS relation")).rows[0].relation, null, 'synthetic later failure also rolls back');
    assert.strictEqual((await failurePool.query('SELECT count(*)::int AS count FROM users')).rows[0].count, 0, 'rollback does not fabricate user data');

    await createDatabase(adminPool, restoreName);
    assertGeneratedDatabase(restoreName);
    const dumpPath = path.join(tempDir, 'phase-b-fixture.dump');
    const pgBin = process.env.PHASE_B_PG_BIN || path.resolve(__dirname, '../.runtime/pgsql/usr/bin');
    const pgEnv = { ...process.env, PATH: `${pgBin}:${process.env.PATH || ''}` };
    execFileSync(path.join(pgBin, 'pg_dump'), ['--format=custom', '--no-owner', '--file', dumpPath, '--dbname', databaseUrl(admin, sourceName)], { env: pgEnv, stdio: 'pipe' });
    execFileSync(path.join(pgBin, 'pg_restore'), ['--no-owner', '--dbname', databaseUrl(admin, restoreName), dumpPath], { env: pgEnv, stdio: 'pipe' });
    restorePool = new Pool({ connectionString: databaseUrl(admin, restoreName) });
    await assertPhaseBTestDatabase(restorePool);
    assert.strictEqual(await rowsSnapshot(restorePool, await tableNames(restorePool)), await rowsSnapshot(sourcePool, await tableNames(sourcePool)), 'dump/restore preserves existing, membership, permission, audit, security, and ledger rows');
    assert.strictEqual(await catalogSnapshot(restorePool), await catalogSnapshot(sourcePool), 'dump/restore preserves tables, constraints, indexes, triggers, routines, and policy metadata');
    assert.strictEqual((await restorePool.query('SELECT count(*)::int AS count FROM permissions')).rows[0].count, PERMISSION_CODES.length);
    console.log(`Phase B persistence passed on isolated databases ${sourceName}, ${restoreName}, ${failureName}.`);
  } finally {
    if (restorePool) await restorePool.end();
    if (failurePool) await failurePool.end();
    if (sourcePool) await sourcePool.end();
    await fs.promises.rm(tempDir, { recursive: true, force: true });
    await dropDatabase(adminPool, failureName);
    await dropDatabase(adminPool, restoreName);
    await dropDatabase(adminPool, sourceName);
    await adminPool.end();
  }
}

if (process.argv[2] === '--verify-migration-filename-guard') {
  verifyMigrationFileNameGuard();
} else {
  adminUrl();
  run().catch(error => {
    const location = error.stack && error.stack.split('\n').find(line => line.includes('phase_b_persistence.test.js:'));
    process.stderr.write(`Phase B persistence test failed: ${error.message}${location ? `\n${location.trim()}` : ''}\n`);
    process.exitCode = 1;
  });
}
