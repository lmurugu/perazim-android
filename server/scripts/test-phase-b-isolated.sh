#!/usr/bin/env bash
set -euo pipefail

ALLOW_MISSING_B3=0
case "${1:-}" in
  '') ;;
  --allow-missing-b3) ALLOW_MISSING_B3=1 ;;
  --help)
    cat <<'HELP'
Usage: scripts/test-phase-b-isolated.sh [--allow-missing-b3]

Runs Phase B persistence, migration, security, and legacy backend checks using
only a temporary local PostgreSQL cluster and generated *_test databases. By
default the B3 security suite must exist. The option --allow-missing-b3 is for
preparatory validation only.

Optional: set PHASE_B_TEST_ADMIN_URL to an explicit local PostgreSQL URL for an
isolated test cluster and PHASE_B_PG_BIN to matching pg_dump/pg_restore tools.
HELP
    exit 0 ;;
  *) echo "Unknown argument. Use --help." >&2; exit 2 ;;
esac

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVER_DIR="$APP_DIR/server"
PG_ROOT="$SERVER_DIR/.runtime/pgsql/usr"
PG_TOOLS="$PG_ROOT/bin"
B3_SECURITY_TEST="$SERVER_DIR/test/phase_b_security.test.js"
B3_AUTHORITY_TEST="$SERVER_DIR/test/phase_b_authority.test.js"
PG_DATA=''
PG_TEMP=''
CLUSTER_STARTED=0
EXTERNAL_CLUSTER=0
LEGACY_DB=""
RESTART_DB=""
LEGACY_CREATED=0
RESTART_CREATED=0

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM
  if [[ "$LEGACY_CREATED" == 1 ]]; then "$PG_TOOLS/dropdb" --if-exists --force --host "$PGHOST" --port "$PGPORT" --username "$PGUSER" "$LEGACY_DB" >/dev/null 2>&1 || true; fi
  if [[ "$RESTART_CREATED" == 1 ]]; then "$PG_TOOLS/dropdb" --if-exists --force --host "$PGHOST" --port "$PGPORT" --username "$PGUSER" "$RESTART_DB" >/dev/null 2>&1 || true; fi
  if [[ "$CLUSTER_STARTED" == 1 || ( -n "$PG_DATA" && -f "$PG_DATA/PG_VERSION" ) ]]; then
    "$PG_TOOLS/pg_ctl" -D "$PG_DATA" -m fast -w stop >/dev/null 2>&1 || true
  fi
  if [[ "$EXTERNAL_CLUSTER" == 0 && -n "$PG_TEMP" ]]; then rm -rf -- "$PG_TEMP"; fi
  exit "$exit_code"
}
trap cleanup EXIT INT TERM

if { [[ ! -f "$B3_SECURITY_TEST" ]] || [[ ! -f "$B3_AUTHORITY_TEST" ]]; } && [[ "$ALLOW_MISSING_B3" == 0 ]]; then
  echo 'B3 security and authority suites must both be present. --allow-missing-b3 is for preparatory validation only.' >&2
  exit 2
fi

if [[ ! -x "$PG_TOOLS/initdb" || ! -x "$PG_TOOLS/pg_ctl" || ! -x "$PG_TOOLS/psql" ]]; then
  echo "PostgreSQL runtime tools were not found under server/.runtime/pgsql/usr/bin." >&2
  exit 1
fi
if [[ ! -f "$PG_ROOT/share/pgsql/postgres.bki" ]]; then
  echo "PostgreSQL runtime share files were not found under server/.runtime/pgsql/usr/share/pgsql." >&2
  exit 1
fi

export LD_LIBRARY_PATH="$PG_ROOT/lib64${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export PATH="$PG_TOOLS:$PATH"
export TMPDIR=/tmp
unset DATABASE_URL PGDATABASE PGSERVICE PGSERVICEFILE PGOPTIONS PGPASSWORD PHASE_B_SYNTHETIC PHASE_B_ADMIN_KEY || true
export PGPASSFILE=/dev/null

if [[ -n "${PHASE_B_TEST_ADMIN_URL:-}" ]]; then
  EXTERNAL_CLUSTER=1
  ADMIN_URL="$PHASE_B_TEST_ADMIN_URL"
  # Parse only this explicit test URL. Never read DATABASE_URL or local backend
  # credentials, and never print the URL or its password.
  ADMIN_PARTS_TEXT="$(PHASE_B_ADMIN_URL="$ADMIN_URL" node - <<'NODE'
const u = new URL(process.env.PHASE_B_ADMIN_URL);
const database = decodeURIComponent(u.pathname.slice(1));
const host = (u.searchParams.get('host') || u.hostname).replace(/^\[|\]$/g, '').toLowerCase();
const port = u.searchParams.get('port') || u.port || '5432';
const user = decodeURIComponent(u.username);
const password = decodeURIComponent(u.password);
if (!['postgres:', 'postgresql:'].includes(u.protocol) || database !== 'postgres') process.exit(20);
const isLocal = host === 'localhost' || host === '127.0.0.1' || host === '::1' || host.startsWith('/tmp/');
if (!isLocal) process.exit(21);
if ([host, port, user, password].some(value => /[\r\n]/.test(value))) process.exit(22);
process.stdout.write(`${host}\n${port}\n${user}\n${password}\n`);
NODE
  )" || { echo "PHASE_B_TEST_ADMIN_URL must target a local postgres maintenance database." >&2; exit 1; }
  mapfile -t ADMIN_PARTS <<< "$ADMIN_PARTS_TEXT"
  PGHOST="${ADMIN_PARTS[0]}"
  PGPORT="${ADMIN_PARTS[1]}"
  PGUSER="${ADMIN_PARTS[2]}"
  if [[ -n "${ADMIN_PARTS[3]}" ]]; then PGPASSWORD="${ADMIN_PARTS[3]}"; export PGPASSWORD; else unset PGPASSWORD || true; fi
  export PGHOST PGPORT PGUSER
  unset DATABASE_URL || true
  PG_DUMP_TOOLS="${PHASE_B_PG_BIN:-$PG_TOOLS}"
else
  EXTERNAL_CLUSTER=0
  PG_TEMP="$(mktemp -d /tmp/perazim-phase-b-runner.XXXXXX)"
  chmod 700 "$PG_TEMP"
  PG_DATA="$PG_TEMP/data"
  PG_SOCKET="$PG_TEMP/socket"
  mkdir -m 700 "$PG_SOCKET"
  PGPORT="$(node - <<'NODE'
const net = require('net');
const server = net.createServer();
server.listen(0, '127.0.0.1', () => {
  const port = server.address().port;
  server.close(() => process.stdout.write(String(port)));
});
NODE
  )"
  PGHOST='127.0.0.1'
  PGUSER='postgres'
  unset PGPASSWORD DATABASE_URL || true
  export PGHOST PGPORT PGUSER
  "$PG_TOOLS/initdb" -D "$PG_DATA" -U postgres -L "$PG_ROOT/share/pgsql" \
    --auth-local=trust --auth-host=trust --locale=C.UTF-8 >/dev/null
  "$PG_TOOLS/pg_ctl" -D "$PG_DATA" -l "$PG_TEMP/postgres.log" \
    -o "-h 127.0.0.1 -p $PGPORT -k $PG_SOCKET -c dynamic_library_path=$PG_ROOT/lib64/pgsql" -w start >/dev/null
  CLUSTER_STARTED=1
  PG_DUMP_TOOLS="$PG_TOOLS"
  ADMIN_URL="postgres://postgres@127.0.0.1:$PGPORT/postgres"
fi

if [[ ! -x "$PG_DUMP_TOOLS/pg_dump" || ! -x "$PG_DUMP_TOOLS/pg_restore" ]]; then
  echo 'Matching pg_dump and pg_restore tools must be available in PHASE_B_PG_BIN.' >&2
  exit 1
fi
RUNTIME_PG_MAJOR="$("$PG_TOOLS/postgres" --version | sed -nE 's/^postgres \(PostgreSQL\) ([0-9]+).*/\1/p')"
if [[ "$EXTERNAL_CLUSTER" == 0 && "$RUNTIME_PG_MAJOR" != 18 ]]; then
  echo "The owned-cluster runner requires PostgreSQL 18 runtime binaries; found major ${RUNTIME_PG_MAJOR}." >&2
  exit 1
fi

RUN_TOKEN="$(node -p 'require("crypto").randomBytes(6).toString("hex")')"
LEGACY_DB="perazim_phaseb_legacy_${RUN_TOKEN}_test"
RESTART_DB="perazim_phaseb_restart_${RUN_TOKEN}_test"
if [[ ! "$LEGACY_DB" =~ ^perazim_phaseb_[a-z0-9_]+_test$ || ! "$RESTART_DB" =~ ^perazim_phaseb_[a-z0-9_]+_test$ ]]; then
  echo 'SAFETY ABORT: generated database names did not pass the disposable-name guard.' >&2
  exit 1
fi

make_database_url() {
  PHASE_B_ADMIN_URL="$ADMIN_URL" PHASE_B_DATABASE_NAME="$1" node - <<'NODE'
const u = new URL(process.env.PHASE_B_ADMIN_URL);
u.pathname = `/${process.env.PHASE_B_DATABASE_NAME}`;
process.stdout.write(u.toString());
NODE
}

run() {
  local label="$1"
  shift
  printf '\n[Phase B isolated runner] %s\n' "$label"
  "$@"
}

SERVER_VERSION="$("$PG_TOOLS/psql" --no-psqlrc --set ON_ERROR_STOP=1 --host "$PGHOST" --port "$PGPORT" --username "$PGUSER" --dbname postgres --tuples-only --no-align --command 'SHOW server_version')"
printf '[Phase B isolated runner] PostgreSQL %s on local port %s\n' "$SERVER_VERSION" "$PGPORT"
printf '[Phase B isolated runner] External test cluster: %s\n' "$([[ "$EXTERNAL_CLUSTER" == 1 ]] && echo yes || echo no)"

run 'B2 persistence, migration, failure rollback, and dump/restore' \
  env -u DATABASE_URL NODE_ENV=test PHASE_B_TEST_ADMIN_URL="$ADMIN_URL" PHASE_B_PG_BIN="$PG_DUMP_TOOLS" \
  node "$SERVER_DIR/test/phase_b_persistence.test.js"

run 'Foundation migration safety suite' \
  env -u DATABASE_URL NODE_ENV=test MIGRATIONS_TEST_ADMIN_URL="$ADMIN_URL" PHASE_B_PG_BIN="$PG_DUMP_TOOLS" \
  npm --prefix "$SERVER_DIR" run test:migrations

run 'B4/B5 membership, permissions and administrator workflow' \
  env -u DATABASE_URL NODE_ENV=test PHASE_B_TEST_ADMIN_URL="$ADMIN_URL" \
  node "$SERVER_DIR/test/phase_b_workflow.test.js"

if [[ -f "$B3_SECURITY_TEST" ]]; then
  run 'B3 security integration suite' \
    env -u DATABASE_URL NODE_ENV=test PHASE_B_TEST_ADMIN_URL="$ADMIN_URL" MIGRATIONS_TEST_ADMIN_URL="$ADMIN_URL" PHASE_B_PG_BIN="$PG_DUMP_TOOLS" \
    node "$B3_SECURITY_TEST"
else
  echo '[Phase B isolated runner] B3 security suite skipped by explicit --allow-missing-b3.'
fi
if [[ -f "$B3_AUTHORITY_TEST" ]]; then
  run 'B3 authority integration suite' \
    env -u DATABASE_URL NODE_ENV=test PHASE_B_TEST_ADMIN_URL="$ADMIN_URL" PHASE_B_PG_BIN="$PG_DUMP_TOOLS" \
    node "$B3_AUTHORITY_TEST"
else
  echo '[Phase B isolated runner] B3 authority suite skipped by explicit --allow-missing-b3.'
fi

"$PG_TOOLS/createdb" --host "$PGHOST" --port "$PGPORT" --username "$PGUSER" "$LEGACY_DB"
LEGACY_CREATED=1
LEGACY_URL="$(make_database_url "$LEGACY_DB")"
run 'Legacy unit, PostgreSQL, and community contracts on a fresh test database' \
  env -u DATABASE_URL NODE_ENV=test DATABASE_URL="$LEGACY_URL" npm --prefix "$SERVER_DIR" test

"$PG_TOOLS/createdb" --host "$PGHOST" --port "$PGPORT" --username "$PGUSER" "$RESTART_DB"
RESTART_CREATED=1
RESTART_URL="$(make_database_url "$RESTART_DB")"
run 'Prepare the separate restart test database with the existing schema' \
  env PGHOST="$PGHOST" PGPORT="$PGPORT" PGUSER="$PGUSER" \
  "$PG_TOOLS/psql" --no-psqlrc --set ON_ERROR_STOP=1 --dbname "$RESTART_DB" --file "$SERVER_DIR/schema.sql"
run 'API process restart persistence on its own initialized test database' \
  env -u DATABASE_URL NODE_ENV=test DATABASE_URL="$RESTART_URL" npm --prefix "$SERVER_DIR" run test:restart

printf '\n[Phase B isolated runner] All requested suites passed.\n'
