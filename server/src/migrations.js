const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const MIGRATIONS_DIR = path.join(__dirname, '../migrations');
const LEDGER = 'perazim_schema_migrations';
const LOCK_KEY = '723448319116047';

function loadMigrations(directory = MIGRATIONS_DIR) {
  const sqlFiles = fs.readdirSync(directory).filter(name => name.endsWith('.sql'));
  const invalid = sqlFiles.find(name => !/^\d{4}_[a-z0-9_-]+\.sql$/.test(name));
  if (invalid) throw new Error(`Migration filename is invalid: ${invalid}. Expected four-digit ordered prefix and lowercase name.`);
  const migrations = sqlFiles
    .sort()
    .map(name => {
      const sql = fs.readFileSync(path.join(directory, name), 'utf8');
      return { version: name.slice(0, 4), name, sql, checksum: crypto.createHash('sha256').update(sql).digest('hex') };
    });
  if (!migrations.length) throw new Error('No ordered migration files are available.');
  for (let index = 0; index < migrations.length; index++) {
    if (migrations[index].version !== String(index + 1).padStart(4, '0')) {
      throw new Error(`Migration files must begin at 0001 and remain gap-free; found ${migrations[index].name}.`);
    }
  }
  return migrations;
}

async function ledgerExists(client) {
  const result = await client.query("SELECT to_regclass('public." + LEDGER + "') AS ledger");
  return result.rows[0].ledger !== null;
}

async function publicRelations(client) {
  const result = await client.query(`
    SELECT c.relname AS name, c.relkind AS kind
      FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p', 'v', 'm', 'S', 'f')
       AND c.relname <> $1 ORDER BY c.relname
  `, [LEDGER]);
  return result.rows;
}

async function snapshot(client, schema) {
  const relations = await client.query(`SELECT c.relname AS name, c.relkind AS kind, c.relrowsecurity AS row_security, c.relforcerowsecurity AS force_row_security, c.relpersistence AS persistence, c.relispartition AS is_partition, pg_get_partkeydef(c.oid) AS partition_key FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=$1 AND c.relkind IN ('r','p','v','m','S','f') AND c.relname<>$2 ORDER BY c.relname`, [schema, LEDGER]);
  const columns = await client.query(`SELECT table_name, ordinal_position, column_name, data_type, udt_name, udt_schema, is_nullable, column_default, is_generated, generation_expression, is_identity, identity_generation, character_maximum_length, numeric_precision, numeric_scale, collation_name FROM information_schema.columns WHERE table_schema=$1 ORDER BY table_name, ordinal_position`, [schema]);
  const constraints = await client.query(`SELECT rel.relname AS table_name, con.conname AS name, con.contype AS type, pg_get_constraintdef(con.oid,true) AS definition FROM pg_constraint con JOIN pg_class rel ON rel.oid=con.conrelid JOIN pg_namespace ns ON ns.oid=rel.relnamespace WHERE ns.nspname=$1 ORDER BY rel.relname, con.conname`, [schema]);
  const indexes = await client.query(`SELECT tablename AS table_name, indexname AS name, regexp_replace(indexdef, ' ON [^ ]+\\.', ' ON <schema>.') AS definition FROM pg_indexes WHERE schemaname=$1 ORDER BY tablename, indexname`, [schema]);
  const triggers = await client.query(`SELECT rel.relname AS table_name, trigger.tgname AS name, trigger.tgenabled AS enabled, pg_get_triggerdef(trigger.oid,true) AS definition FROM pg_trigger trigger JOIN pg_class rel ON rel.oid=trigger.tgrelid JOIN pg_namespace ns ON ns.oid=rel.relnamespace WHERE ns.nspname=$1 AND NOT trigger.tgisinternal ORDER BY rel.relname, trigger.tgname`, [schema]);
  const policies = await client.query(`SELECT tablename, policyname, permissive, roles, cmd, qual, with_check FROM pg_policies WHERE schemaname=$1 ORDER BY tablename, policyname`, [schema]);
  const sequences = await client.query(`SELECT c.relname AS sequence_name, s.seqtypid::regtype::text AS data_type, s.seqstart AS start_value, s.seqmin AS min_value, s.seqmax AS max_value, s.seqincrement AS increment_by, s.seqcycle AS cycle FROM pg_sequence s JOIN pg_class c ON c.oid=s.seqrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=$1 ORDER BY c.relname`, [schema]);
  const views = await client.query(`SELECT c.relname AS relation_name, c.relkind AS kind, pg_get_viewdef(c.oid,true) AS definition FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=$1 AND c.relkind IN ('v','m') ORDER BY c.relname`, [schema]);
  return JSON.stringify({ relations: relations.rows, columns: columns.rows, constraints: constraints.rows, indexes: indexes.rows, triggers: triggers.rows, policies: policies.rows, sequences: sequences.rows, views: views.rows });
}

async function expectedBaselineSnapshot(client, migration) {
  const schema = `migration_expect_${process.pid}_${crypto.randomBytes(4).toString('hex')}`;
  await client.query(`CREATE SCHEMA "${schema}"`);
  try {
    await client.query(`SET LOCAL search_path TO "${schema}"`);
    await client.query(migration.sql);
    return await snapshot(client, schema);
  } finally {
    await client.query(`DROP SCHEMA IF EXISTS "${schema}" CASCADE`);
  }
}

async function inspectUntrackedSchema(client) {
  const relations = await publicRelations(client);
  if (relations.length === 0) return { kind: 'empty', relations };
  if (await ledgerExists(client)) return { kind: 'tracked', relations };
  // Read-only status and planning never create a schema, even temporarily.
  return { kind: 'untracked', relations };
}

async function readApplied(client) {
  if (!(await ledgerExists(client))) return null;
  const result = await client.query(`SELECT version, name, checksum FROM public.${LEDGER} ORDER BY version`);
  return result.rows;
}

async function verifyHistory(client, migrationsDirectory = MIGRATIONS_DIR) {
  const migrations = loadMigrations(migrationsDirectory);
  const applied = await readApplied(client);
  if (applied === null) return { migrations, applied: [], state: await inspectUntrackedSchema(client) };
  const known = new Map(migrations.map(migration => [migration.version, migration]));
  for (const row of applied) {
    const migration = known.get(row.version);
    if (!migration) throw new Error(`Migration ledger contains unknown version ${row.version}; refusing to continue.`);
    if (migration.name !== row.name || migration.checksum !== row.checksum) throw new Error(`Migration checksum/name drift detected for ${row.version}; refusing to continue.`);
  }
  const seen = new Set(applied.map(row => row.version));
  const firstMissing = migrations.findIndex(migration => !seen.has(migration.version));
  if (firstMissing >= 0 && migrations.slice(firstMissing).some(migration => seen.has(migration.version))) throw new Error('Migration ledger has a gap in its applied history; refusing to continue.');
  return { migrations, applied, state: { kind: 'tracked' } };
}

async function status(client, migrationsDirectory = MIGRATIONS_DIR) {
  const result = await verifyHistory(client, migrationsDirectory);
  return {
    state: result.state.kind,
    applied: result.applied.map(({ version, name }) => ({ version, name })),
    pending: result.migrations.filter(m => !result.applied.some(row => row.version === m.version)).map(({ version, name }) => ({ version, name }))
  };
}

async function plan(client, migrationsDirectory = MIGRATIONS_DIR) {
  const result = await verifyHistory(client, migrationsDirectory);
  if (result.state.kind === 'untracked') return { action: 'baseline-check-required', pending: [], message: 'The explicit baseline command performs a strict schema comparison before recording history.' };
  return { action: 'apply', pending: result.migrations.filter(m => !result.applied.some(row => row.version === m.version)).map(({ version, name, checksum }) => ({ version, name, checksum })) };
}

async function withLock(pool, callback) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await client.query('SELECT pg_advisory_xact_lock($1::bigint)', [LOCK_KEY]);
    const result = await callback(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    try { await client.query('ROLLBACK'); } catch (_) {}
    throw error;
  } finally { client.release(); }
}

async function createLedger(client) {
  await client.query(`CREATE TABLE public.${LEDGER} (version VARCHAR(4) PRIMARY KEY, name TEXT NOT NULL UNIQUE, checksum CHAR(64) NOT NULL, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())`);
}

async function migrate(pool, migrationsDirectory = MIGRATIONS_DIR) {
  return withLock(pool, async client => {
    const result = await verifyHistory(client, migrationsDirectory);
    if (result.state.kind === 'untracked') throw new Error('Existing schema needs explicit verified baselining; migrate will not adopt it automatically.');
    if (result.state.kind !== 'empty' && result.applied.length === 0) throw new Error('Database is not empty and has no verified migration history.');
    if (result.state.kind === 'empty') await createLedger(client);
    await client.query('SET LOCAL search_path TO public');
    const applied = new Set(result.applied.map(row => row.version));
    for (const migration of result.migrations) {
      if (applied.has(migration.version)) continue;
      await client.query(migration.sql);
      await client.query(`INSERT INTO public.${LEDGER} (version, name, checksum) VALUES ($1, $2, $3)`, [migration.version, migration.name, migration.checksum]);
    }
    return status(client, migrationsDirectory);
  });
}

async function baseline(pool, migrationsDirectory = MIGRATIONS_DIR) {
  return withLock(pool, async client => {
    const result = await verifyHistory(client, migrationsDirectory);
    if (result.state.kind !== 'untracked') throw new Error(`Baseline refused: database is ${result.state.kind}; only an exact untracked baseline can be adopted.`);
    const migration = result.migrations[0];
    if (!migration) throw new Error('No baseline migration is available.');
    const actual = await snapshot(client, 'public');
    const expected = await expectedBaselineSnapshot(client, migration);
    if (actual !== expected) throw new Error('Baseline refused: database schema does not exactly match the migration baseline. No user tables or rows were changed.');
    await createLedger(client);
    await client.query(`INSERT INTO public.${LEDGER} (version, name, checksum) VALUES ($1, $2, $3)`, [migration.version, migration.name, migration.checksum]);
    return status(client, migrationsDirectory);
  });
}

module.exports = { LEDGER, loadMigrations, status, plan, migrate, baseline, verifyHistory };
