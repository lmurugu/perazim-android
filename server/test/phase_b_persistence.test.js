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
  'admin.audit.read'
];
const B2_TABLES = [
  'membership_imports',
  'membership_records',
  'membership_claims',
  'membership_verifications',
  'membership_verification_history',
  'permissions',
  'user_permissions',
  'admin_audit_log',
  'admin_security',
  'pilot_bootstrap',
  'privileged_sessions'
];

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
  const baselinePath = path.join(migrationsDir, '0001_current_schema_baseline.sql');
  const fullMigrationPath = path.join(migrationsDir, '0002_membership_permissions.sql');
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
    assert.strictEqual(firstRun.applied.length, 2, 'ordered baseline and membership migration are recorded');
    assert.strictEqual(firstRun.applied[1].name, '0002_membership_permissions.sql');
    const newTables = (await tableNames(sourcePool, [LEDGER])).filter(name => !existingTables.includes(name));
    assert.deepStrictEqual(newTables, [...B2_TABLES].sort(), 'migration adds exactly the approved membership and permission tables');
    const checksum = (await sourcePool.query(`SELECT checksum FROM public.${LEDGER} WHERE version='0002'`)).rows[0].checksum;
    assert.match(checksum, /^[a-f0-9]{64}$/);
    assert.strictEqual(await rowsSnapshot(sourcePool, existingTables), existingRowsBefore, 'B2 preserves every pre-existing public table row');
    assert.strictEqual(await catalogSnapshot(sourcePool, existingTables), existingCatalogBefore, 'B2 does not alter the pre-existing tables or their constraints/indexes');
    assert.strictEqual((await sourcePool.query('SELECT grace_points FROM users WHERE id=$1', [fixture.owner])).rows[0].grace_points, 0);
    assert.strictEqual((await sourcePool.query('SELECT grace_points FROM users WHERE id=$1', [fixture.member])).rows[0].grace_points, 9);

    const permissions = await sourcePool.query('SELECT code,active FROM permissions ORDER BY code');
    assert.deepStrictEqual(permissions.rows.map(row => row.code), [...PERMISSION_CODES].sort(), 'only the canonical permission definitions are seeded');
    assert(permissions.rows.every(row => row.active === true));
    assert.strictEqual((await sourcePool.query('SELECT count(*)::int AS count FROM user_permissions')).rows[0].count, 0, 'migration creates no permission grants');
    assert.strictEqual((await sourcePool.query("SELECT column_default FROM information_schema.columns WHERE table_schema='public' AND table_name='membership_verifications' AND column_name='status'")).rows[0].column_default, "'REGISTERED'::text");
    assert.strictEqual((await sourcePool.query("SELECT column_default FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_security' AND column_name='auth_not_before'")).rows[0].column_default, '0');
    const membershipFixture = await insertMembershipFixture(sourcePool, fixture, token);
    assert.strictEqual((await sourcePool.query("SELECT count(*)::int AS count FROM membership_verifications WHERE status='REGISTERED'")).rows[0].count, 1, 'membership verification defaults to REGISTERED');
    await assert.rejects(() => sourcePool.query("UPDATE membership_verifications SET status='VERIFIED_MEMBER',membership_record_id=NULL WHERE user_id=$1", [fixture.member]));
    await assert.rejects(() => sourcePool.query(`UPDATE admin_audit_log SET reason='changed' WHERE id=$1`, [membershipFixture.auditId]), /append-only/);
    await assert.rejects(() => sourcePool.query(`DELETE FROM admin_audit_log WHERE id=$1`, [membershipFixture.auditId]), /append-only/);
    await assert.rejects(() => sourcePool.query('TRUNCATE admin_audit_log'), /append-only/);
    assert.strictEqual((await sourcePool.query('SELECT count(*)::int AS count FROM admin_audit_log')).rows[0].count, 1, 'audit row survives rejected updates, deletes, and truncate');
    await assert.rejects(() => sourcePool.query("UPDATE membership_records SET record_status='UNKNOWN' WHERE id=$1", [membershipFixture.recordId]));
    await assert.rejects(() => sourcePool.query('UPDATE membership_records SET revision=0 WHERE id=$1', [membershipFixture.recordId]));
    await assert.rejects(() => sourcePool.query("UPDATE membership_verifications SET membership_record_id=$1,status='VERIFIED_MEMBER' WHERE user_id=$2", [membershipFixture.recordId, fixture.member]), /duplicate key/);
    await assert.rejects(() => sourcePool.query(`INSERT INTO user_permissions (id,user_id,permission_code,scope,granted_by,granted_at,effective_at,expires_at) VALUES ($1,$2,'membership.status.self.read','global',$3,$4,$4,$4)`, [newId(), fixture.member, fixture.owner, Date.now()]));
    const secondRun = await migrate(sourcePool);
    assert.strictEqual(secondRun.applied.length, 2, 'repeat migration leaves the ledger unchanged');
    assert.strictEqual((await sourcePool.query(`SELECT checksum FROM public.${LEDGER} WHERE version='0002'`)).rows[0].checksum, checksum);

    await createDatabase(adminPool, failureName);
    failurePool = new Pool({ connectionString: databaseUrl(admin, failureName) });
    await assertPhaseBTestDatabase(failurePool);
    await applyBaselineOnly(failurePool, path.join(tempDir, 'failure-baseline'), baselinePath);
    fs.mkdirSync(failureDir);
    fs.copyFileSync(baselinePath, path.join(failureDir, '0001_current_schema_baseline.sql'));
    fs.copyFileSync(fullMigrationPath, path.join(failureDir, '0002_membership_permissions.sql'));
    fs.writeFileSync(path.join(failureDir, '0003_synthetic_rollback.sql'), 'CREATE TABLE public.phase_b_rollback_probe (id integer); SELECT 1 / 0;\n');
    await assert.rejects(() => migrate(failurePool, failureDir), /division by zero/);
    assert.deepStrictEqual((await failurePool.query(`SELECT version FROM public.${LEDGER} ORDER BY version`)).rows.map(row => row.version), ['0001'], 'failed B2 transaction leaves only its prior baseline ledger row');
    assert.strictEqual((await failurePool.query("SELECT to_regclass('public.membership_imports') AS relation")).rows[0].relation, null, 'failed B2 DDL rolls back');
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

adminUrl();
run().catch(error => {
  const location = error.stack && error.stack.split('\n').find(line => line.includes('phase_b_persistence.test.js:'));
  process.stderr.write(`Phase B persistence test failed: ${error.message}${location ? `\n${location.trim()}` : ''}\n`);
  process.exitCode = 1;
});
