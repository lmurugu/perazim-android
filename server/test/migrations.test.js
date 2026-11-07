const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync, spawnSync } = require('child_process');
const { Pool } = require('pg');
const { status, plan, migrate, baseline, LEDGER } = require('../src/migrations');
const { assertDisposableTestDatabase } = require('./test_database_safety');

function requireSafeAdminUrl() {
  if (process.env.NODE_ENV === 'production') throw new Error('SAFETY ABORT: Migration tests cannot run in production mode.');
  const raw = process.env.MIGRATIONS_TEST_ADMIN_URL;
  if (!raw) throw new Error('Set MIGRATIONS_TEST_ADMIN_URL to a PostgreSQL admin URL connected to database postgres; tests create uniquely named *_test databases.');
  const parsed = new URL(raw);
  if (decodeURIComponent(parsed.pathname.slice(1)) !== 'postgres') throw new Error('SAFETY ABORT: Migration test admin URL must target the postgres maintenance database.');
  const host = parsed.searchParams.get('host') || parsed.hostname;
  if (!(host === 'localhost' || host === '127.0.0.1' || host === '::1' || host.startsWith('/tmp/'))) {
    throw new Error('SAFETY ABORT: Migration tests only connect to a local PostgreSQL instance.');
  }
  return parsed;
}

function dbUrl(admin, database) {
  const url = new URL(admin.toString());
  url.pathname = `/${database}`;
  return url.toString();
}

async function createDb(adminPool, name) { await adminPool.query(`CREATE DATABASE "${name}"`); }
async function dropDb(adminPool, name) { await adminPool.query(`DROP DATABASE IF EXISTS "${name}" WITH (FORCE)`); }

async function rowSnapshot(pool, omitLedger = false) {
  const list = await pool.query(`SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename`);
  const result = {};
  for (const { tablename } of list.rows) {
    if (omitLedger && tablename === LEDGER) continue;
    const safeName = tablename.replace(/"/g, '""');
    const rows = await pool.query(`SELECT COALESCE(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text), '[]'::jsonb) AS data FROM public."${safeName}" t`);
    result[tablename] = rows.rows[0].data;
  }
  return JSON.stringify(result);
}

async function structuralSnapshot(pool, omitLedger = false) {
  const exclusion = omitLedger ? `AND c.relname <> '${LEDGER}'` : '';
  const [relations, columns, constraints, indexes, sequences, triggers, policies, views] = await Promise.all([
    pool.query(`SELECT c.relname AS name, c.relkind AS kind, c.relrowsecurity AS row_security, c.relforcerowsecurity AS force_row_security, c.relpersistence AS persistence, c.relispartition AS is_partition, pg_get_partkeydef(c.oid) AS partition_key FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind IN ('r','p','v','m','S','f') ${exclusion} ORDER BY c.relname`),
    pool.query(`SELECT table_name, ordinal_position, column_name, data_type, udt_name, udt_schema, is_nullable, column_default, is_generated, generation_expression, is_identity, identity_generation, character_maximum_length, numeric_precision, numeric_scale, collation_name FROM information_schema.columns WHERE table_schema='public' ${omitLedger ? `AND table_name <> '${LEDGER}'` : ''} ORDER BY table_name, ordinal_position`),
    pool.query(`SELECT rel.relname AS table_name, con.conname AS name, con.contype AS type, pg_get_constraintdef(con.oid,true) AS definition FROM pg_constraint con JOIN pg_class rel ON rel.oid=con.conrelid JOIN pg_namespace ns ON ns.oid=rel.relnamespace WHERE ns.nspname='public' ${omitLedger ? `AND rel.relname <> '${LEDGER}'` : ''} ORDER BY rel.relname, con.conname`),
    pool.query(`SELECT tablename AS table_name, indexname AS name, regexp_replace(indexdef, ' ON [^ ]+\\.', ' ON <schema>.') AS definition FROM pg_indexes WHERE schemaname='public' ${omitLedger ? `AND tablename <> '${LEDGER}'` : ''} ORDER BY tablename, indexname`),
    pool.query(`SELECT sequencename, data_type, start_value, min_value, max_value, increment_by, cycle FROM pg_sequences WHERE schemaname='public' ORDER BY sequencename`),
    pool.query(`SELECT rel.relname AS table_name, trigger.tgname AS name, trigger.tgenabled AS enabled, pg_get_triggerdef(trigger.oid,true) AS definition FROM pg_trigger trigger JOIN pg_class rel ON rel.oid=trigger.tgrelid JOIN pg_namespace ns ON ns.oid=rel.relnamespace WHERE ns.nspname='public' AND NOT trigger.tgisinternal ${omitLedger ? `AND rel.relname <> '${LEDGER}'` : ''} ORDER BY rel.relname, trigger.tgname`),
    pool.query(`SELECT tablename, policyname, permissive, roles, cmd, qual, with_check FROM pg_policies WHERE schemaname='public' ORDER BY tablename, policyname`),
    pool.query(`SELECT c.relname AS relation_name, c.relkind AS kind, pg_get_viewdef(c.oid,true) AS definition FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind IN ('v','m') ORDER BY c.relname`)
  ]);
  return JSON.stringify({ relations: relations.rows, columns: columns.rows, constraints: constraints.rows, indexes: indexes.rows, sequences: sequences.rows, triggers: triggers.rows, policies: policies.rows, views: views.rows });
}

async function run() {
  const cli = path.join(__dirname, '../scripts/db-migrate.js');
  const nonTestWrite = spawnSync(process.execPath, [cli, 'migrate', '--confirm=perazim_local'], {
    encoding: 'utf8', env: { ...process.env, NODE_ENV: 'test', DATABASE_URL: 'postgres://local@127.0.0.1/perazim_local' }
  });
  assert.strictEqual(nonTestWrite.status, 1);
  assert.match(nonTestWrite.stderr, /limited to disposable databases/);
  const remoteWrite = spawnSync(process.execPath, [cli, 'migrate', '--confirm=perazim_safe_test'], {
    encoding: 'utf8', env: { ...process.env, NODE_ENV: 'test', DATABASE_URL: 'postgres://remote.example/perazim_safe_test' }
  });
  assert.strictEqual(remoteWrite.status, 1);
  assert.match(remoteWrite.stderr, /limited to local PostgreSQL connections/);
  const productionWrite = spawnSync(process.execPath, [cli, 'baseline', '--confirm=perazim_safe_test'], {
    encoding: 'utf8', env: { ...process.env, NODE_ENV: 'production', DATABASE_URL: 'postgres://local@127.0.0.1/perazim_safe_test' }
  });
  assert.strictEqual(productionWrite.status, 1);
  assert.match(productionWrite.stderr, /Production writes are disabled/);

  const admin = requireSafeAdminUrl();
  const adminPool = new Pool({ connectionString: admin.toString() });
  const suffix = `${process.pid}_${crypto.randomBytes(5).toString('hex')}`;
  const sourceName = `perazim_migration_source_${suffix}_test`;
  const restoreName = `perazim_migration_restore_${suffix}_test`;
  const rollbackName = `perazim_migration_rollback_${suffix}_test`;
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'perazim-pg-restore-'));
  let sourcePool;
  // Keep the Phase A baseline-adoption regression pinned to its original schema.
  // Later feature migrations are exercised separately against ordered history.
  const foundationDir = path.join(tempDir, 'foundation');
  fs.mkdirSync(foundationDir);
  fs.copyFileSync(path.join(__dirname, '../migrations/0001_current_schema_baseline.sql'), path.join(foundationDir, '0001_current_schema_baseline.sql'));
  const testMigrationsDir = path.join(tempDir, 'migrations');
  fs.mkdirSync(testMigrationsDir);
  fs.copyFileSync(path.join(__dirname, '../migrations/0001_current_schema_baseline.sql'), path.join(testMigrationsDir, '0001_current_schema_baseline.sql'));
  const badMigrationPath = path.join(testMigrationsDir, '0002_test_rollback.sql');
  try {
    const adminInfo = await adminPool.query('SELECT current_database() AS db, current_setting(\'server_version_num\')::int AS version');
    assert.strictEqual(adminInfo.rows[0].db, 'postgres');
    assert(adminInfo.rows[0].version >= 130000, 'PostgreSQL 13+ supports forced cleanup for disposable test databases');
    for (const dbName of [sourceName, restoreName, rollbackName]) assertDisposableTestDatabase(dbUrl(admin, dbName));
    await createDb(adminPool, sourceName);
    sourcePool = new Pool({ connectionString: dbUrl(admin, sourceName) });

    await sourcePool.query('BEGIN READ ONLY');
    const before = await status(sourcePool, foundationDir);
    assert.strictEqual(before.state, 'empty');
    assert.strictEqual((await plan(sourcePool, foundationDir)).pending.length, 1);
    await sourcePool.query('ROLLBACK');
    const migrated = await migrate(sourcePool, foundationDir);
    assert.strictEqual(migrated.state, 'tracked');
    assert.strictEqual(migrated.applied.length, 1);
    assert.strictEqual((await migrate(sourcePool, foundationDir)).applied.length, 1, 'repeat migration is a no-op');

    await sourcePool.query(`
      INSERT INTO users (id,phone,pin_hash,name,campus_id,role,avatar_url,xp,streak,grace_points,is_active,created_at,updated_at,bio)
      VALUES ('u_owner','+254700000001','fixture-hash-a','Fixture Owner','campus_embu','MEMBER','',12,3,0,true,100,200,'owner bio'),
             ('u_member','+254700000002','fixture-hash-b','Fixture Member','campus_nairobi','MEMBER','avatar',4,1,7,true,101,201,'member bio');
      INSERT INTO prayers (id,title,content,author_id,author_name,is_anonymous,visibility,prayer_count,is_answered,testimony,created_at,updated_at)
      VALUES ('prayer_1','Fixture prayer','Keep this prayer','u_owner','Fixture Owner',false,'PUBLIC',1,true,'Fixture testimony',300,301);
      INSERT INTO prayer_amens VALUES ('prayer_1','u_member',302);
      INSERT INTO messages (id,conversation_id,sender_id,receiver_id,content,status,created_at,media_url,delivery_status,delivered_at)
      VALUES ('message_1','conversation_1','u_owner','u_member','Keep this message','READ',303,'/uploads/fixture','READ',304);
      INSERT INTO connections VALUES ('connection_1','u_owner','u_member','ACCEPTED',305);
      INSERT INTO sync_events VALUES ('sync_1','u_owner','prayer','prayer_1','UPSERT','{"fixture":true}',306);
      INSERT INTO revoked_tokens VALUES ('revoked_fixture','u_member',999,307);
      INSERT INTO community_groups VALUES ('group_1','Fixture Group','Group description','u_owner',true,308);
      INSERT INTO group_memberships VALUES ('group_1','u_member','MEMBER',309);
      INSERT INTO community_posts (id,author_id,author_name,type,title,content,media_url,group_id,is_pinned,is_answered,event_date,event_location,amen_count,comment_count,created_at,updated_at)
      VALUES ('post_1','u_owner','Fixture Owner','GENERAL','Fixture post','Keep post','/uploads/fixture','group_1',false,false,NULL,'',1,1,310,311);
      INSERT INTO post_amens VALUES ('post_1','u_member',312);
      INSERT INTO post_comments VALUES ('comment_1','post_1','u_member','Fixture Member','Keep comment',313);
      INSERT INTO event_rsvps VALUES ('post_1','u_member','ATTENDING',314);
      INSERT INTO in_app_notifications VALUES ('notification_1','u_member','u_owner','Fixture Owner','COMMENT','comment_1','Fixture notification','Keep notification',false,315);
      INSERT INTO daily_challenges VALUES ('2099-01-01','riddle_1','Fixture question','answer','hint',316);
      INSERT INTO user_challenge_completions VALUES ('u_member','2099-01-01',317);
      INSERT INTO community_classes VALUES ('class_1','group_1','Fixture class','Keep class',318,45,'https://example.invalid/meeting','u_owner',false,319);
      INSERT INTO community_media VALUES ('fixture-media.png','u_owner',320);
    `);

    const rowStateBeforeBaseline = await rowSnapshot(sourcePool, true);
    const structureBeforeBaseline = await structuralSnapshot(sourcePool, true);
    await sourcePool.query(`DROP TABLE public.${LEDGER}`);
    const schemaRowsBeforeBaseline = await sourcePool.query("SELECT table_name, column_name, data_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name, ordinal_position");
    await sourcePool.query('BEGIN READ ONLY');
    assert.strictEqual((await status(sourcePool, foundationDir)).state, 'untracked');
    assert.strictEqual((await plan(sourcePool, foundationDir)).action, 'baseline-check-required');
    await sourcePool.query('ROLLBACK');
    await sourcePool.query('CREATE TABLE public.baseline_drift_probe (id integer PRIMARY KEY)');
    await sourcePool.query('INSERT INTO public.baseline_drift_probe VALUES (42)');
    await assert.rejects(() => baseline(sourcePool, foundationDir), /does not exactly match/);
    assert.strictEqual((await sourcePool.query('SELECT id FROM public.baseline_drift_probe')).rows[0].id, 42, 'baseline mismatch leaves unexpected user objects and data untouched');
    await sourcePool.query('DROP TABLE public.baseline_drift_probe');
    const baselineState = await baseline(sourcePool, foundationDir);
    assert.strictEqual(baselineState.applied.length, 1);
    assert.strictEqual(await rowSnapshot(sourcePool, true), rowStateBeforeBaseline, 'verified baseline changes only the ledger');
    assert.strictEqual(await structuralSnapshot(sourcePool, true), structureBeforeBaseline, 'verified baseline preserves all schema metadata and sequence definitions');
    assert.strictEqual((await sourcePool.query(`SELECT count(*)::int AS n FROM information_schema.columns WHERE table_schema='public' AND table_name <> '${LEDGER}'`)).rows[0].n, schemaRowsBeforeBaseline.rowCount);
    await assert.rejects(() => baseline(sourcePool, foundationDir), /Baseline refused: database is tracked/);
    assert.strictEqual(await rowSnapshot(sourcePool, true), rowStateBeforeBaseline, 'a repeated baseline attempt leaves user rows unchanged');

    const originalChecksum = baselineState.applied[0];
    const goodChecksum = (await sourcePool.query(`SELECT checksum FROM public.${LEDGER} WHERE version=$1`, [originalChecksum.version])).rows[0].checksum;
    await sourcePool.query(`UPDATE public.${LEDGER} SET checksum=$1 WHERE version=$2`, ['0'.repeat(64), originalChecksum.version]);
    await assert.rejects(() => status(sourcePool, foundationDir), /checksum\/name drift/);
    await sourcePool.query(`UPDATE public.${LEDGER} SET checksum=$1 WHERE version=$2`, [goodChecksum, originalChecksum.version]);

    await fs.promises.writeFile(badMigrationPath, 'CREATE TABLE public.migration_rollback_probe (id integer); SELECT 1 / 0;\n');
    await createDb(adminPool, rollbackName);
    const rollbackPool = new Pool({ connectionString: dbUrl(admin, rollbackName) });
    try {
      await assert.rejects(() => migrate(rollbackPool, testMigrationsDir), /division by zero/);
      assert.strictEqual((await rollbackPool.query("SELECT to_regclass('public." + LEDGER + "') AS t")).rows[0].t, null, 'ledger creation rolls back after a partial migration failure');
      assert.strictEqual((await rollbackPool.query("SELECT to_regclass('public.migration_rollback_probe') AS t")).rows[0].t, null, 'partial DDL rolls back');
    } finally { await rollbackPool.end(); }
    await fs.promises.unlink(badMigrationPath);

    await createDb(adminPool, restoreName);
    const dumpFile = path.join(tempDir, 'fixture.dump');
    const pgBin = process.env.PHASE_B_PG_BIN || path.join(__dirname, '../.runtime/pgsql/usr/bin');
    const pgEnv = { ...process.env, PATH: `${pgBin}:${process.env.PATH || ''}` };
    execFileSync('pg_dump', ['--format=custom', '--no-owner', '--file', dumpFile, '--dbname', dbUrl(admin, sourceName)], { env: pgEnv, stdio: 'pipe' });
    execFileSync('pg_restore', ['--no-owner', '--dbname', dbUrl(admin, restoreName), dumpFile], { env: pgEnv, stdio: 'pipe' });
    const restoredPool = new Pool({ connectionString: dbUrl(admin, restoreName) });
    try {
      assert.strictEqual(await rowSnapshot(restoredPool), await rowSnapshot(sourcePool), 'pg_dump/pg_restore preserves fixture rows and migration ledger');
      assert.strictEqual(await structuralSnapshot(restoredPool), await structuralSnapshot(sourcePool), 'pg_dump/pg_restore preserves schema objects, constraints, indexes, and sequence definitions');
      assert.strictEqual((await status(restoredPool, foundationDir)).applied.length, 1);
    } finally { await restoredPool.end(); }
    console.log(`Migration safety integration passed on isolated databases ${sourceName}, ${restoreName}, ${rollbackName}.`);
  } finally {
    if (sourcePool) await sourcePool.end();
    await fs.promises.rm(tempDir, { recursive: true, force: true });
    await fs.promises.rm(badMigrationPath, { force: true });
    await dropDb(adminPool, rollbackName);
    await dropDb(adminPool, restoreName);
    await dropDb(adminPool, sourceName);
    await adminPool.end();
  }
}

requireSafeAdminUrl();
run().catch(error => {
  process.stderr.write(`Migration integration failed: ${error.message}\n`);
  process.exitCode = 1;
});
