#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
SERVER_DIR="$ROOT_DIR/server"
DEPS_DIR="${AUDIT_NODE_MODULES:-$SERVER_DIR/node_modules}"
# This checkout intentionally has no vendored runtime. These are tool binaries
# only; all application source and tests are resolved from ROOT_DIR above.
SHARED_TOOL_ROOT="${AUDIT_PG_RUNTIME:-/home/murugu/Android/app/server/.runtime/pgsql/usr}"
PG_TOOLS="$SHARED_TOOL_ROOT/bin"
NODE_BIN="${AUDIT_NODE_BIN:-$(command -v node)}"
TIMEOUT_BIN="$(command -v timeout)"
SETSID_BIN="$(command -v setsid)"
DEADLINE_SECONDS="${AUDIT_DEADLINE_SECONDS:-180}"
[[ "$DEADLINE_SECONDS" =~ ^[1-9][0-9]*$ ]] || { echo '[RUNNER] Invalid AUDIT_DEADLINE_SECONDS' >&2; exit 2; }
for executable in "$NODE_BIN" "$TIMEOUT_BIN" "$SETSID_BIN" "$PG_TOOLS/initdb" "$PG_TOOLS/pg_ctl"; do
  [[ -x "$executable" ]] || { echo "[RUNNER] Required executable unavailable: $executable" >&2; exit 2; }
done
[[ -f "$DEPS_DIR/pg/package.json" ]] || { echo "[RUNNER] pg dependency missing at $DEPS_DIR" >&2; exit 2; }
[[ -f "$DEPS_DIR/express/package.json" ]] || { echo "[RUNNER] express dependency missing at $DEPS_DIR" >&2; exit 2; }

if [[ "${AUDIT_DEADLINE_WRAPPED:-}" != '1' ]]; then
  exec "$TIMEOUT_BIN" --signal=TERM --kill-after=30 "${DEADLINE_SECONDS}s" \
    env AUDIT_DEADLINE_WRAPPED=1 "$BASH" "$ROOT_DIR/run_isolated_audit.sh" "$@"
fi

export LD_LIBRARY_PATH="$SHARED_TOOL_ROOT/lib64${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export PATH="$PG_TOOLS:$PATH"
export TMPDIR=/tmp
unset DATABASE_URL PGDATABASE PGUSER PGHOST PGHOSTADDR PGPORT PGSERVICE PGSERVICEFILE PGOPTIONS PGPASSWORD \
  PGSSLMODE PGSSLROOTCERT PGSSLCERT PGSSLKEY PGSSLCRL PGTARGETSESSIONATTRS PHASE_B_SYNTHETIC PHASE_B_ADMIN_KEY || true

PG_TEMP=''
PG_DATA=''
PG_SOCKET=''
PGPORT=''
WORKER_PID=''
WORKER_GROUP=''
CLEANUP_OK=1
EXIT_CODE=0
cleanup() {
  local incoming=$?
  trap - EXIT INT TERM HUP
  EXIT_CODE=$incoming
  if [[ -n "$WORKER_GROUP" ]] && kill -0 -- "-$WORKER_GROUP" 2>/dev/null; then
    echo "[RUNNER] Stopping test worker group $WORKER_GROUP" >&2
    kill -TERM -- "-$WORKER_GROUP" 2>/dev/null || true
    for _ in 1 2 3 4 5; do
      kill -0 -- "-$WORKER_GROUP" 2>/dev/null || break
      sleep 1
    done
    if kill -0 -- "-$WORKER_GROUP" 2>/dev/null; then kill -KILL -- "-$WORKER_GROUP" 2>/dev/null || true; fi
  fi
  if [[ -n "$WORKER_PID" ]]; then
    wait "$WORKER_PID" 2>/dev/null || { [[ $EXIT_CODE -ne 0 ]] || EXIT_CODE=$?; }
    WORKER_PID=''
  fi
  if [[ -n "$PG_TEMP" && -d "$PG_TEMP" ]]; then
    if [[ "${AUDIT_TEST_FAULT:-}" == 'shutdown-failure' ]]; then
      echo '[RUNNER] Controlled PostgreSQL shutdown failure injected.' >&2
      CLEANUP_OK=0
    elif [[ -n "$PG_DATA" && -f "$PG_DATA/PG_VERSION" ]]; then
      if "$PG_TOOLS/pg_ctl" -D "$PG_DATA" -m fast -w -t 15 stop >/dev/null 2>&1; then
        if "$PG_TOOLS/pg_ctl" -D "$PG_DATA" status >/dev/null 2>&1; then
          echo '[RUNNER] PostgreSQL still reports active after shutdown; preserving its directory.' >&2
          CLEANUP_OK=0
        else
          echo '[RUNNER] PostgreSQL shutdown confirmed.'
        fi
      else
        if "$PG_TOOLS/pg_ctl" -D "$PG_DATA" status >/dev/null 2>&1; then
          echo '[RUNNER] PostgreSQL shutdown failed and postmaster is still running.' >&2
          CLEANUP_OK=0
        else
          echo '[RUNNER] PostgreSQL postmaster confirmed stopped after shutdown command failure.'
        fi
      fi
    fi
    if (( CLEANUP_OK )); then
      rm -rf -- "$PG_TEMP"
      if [[ -e "$PG_TEMP" ]]; then
        echo "[RUNNER] Temporary directory cleanup failed: $PG_TEMP" >&2
        CLEANUP_OK=0
      else
        echo '[RUNNER] Temporary database directory removed; child processes reaped.'
      fi
    else
      local pid='unknown'
      if [[ -s "$PG_DATA/postmaster.pid" ]]; then IFS= read -r pid < "$PG_DATA/postmaster.pid" || true; fi
      echo "[RUNNER] CLEANUP INCOMPLETE. Retained directory: $PG_TEMP" >&2
      echo "[RUNNER] Recovery: inspect PID $pid, stop only this owned cluster with '$PG_TOOLS/pg_ctl -D $PG_DATA -m fast -w stop', verify it stopped, then remove '$PG_TEMP'." >&2
      (( EXIT_CODE != 0 )) || EXIT_CODE=1
    fi
  fi
  echo "[RUNNER] CLEANUP_STATUS=$([[ $CLEANUP_OK == 1 ]] && echo success || echo failure) EXIT_CODE=$EXIT_CODE"
  exit "$EXIT_CODE"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
trap 'exit 129' HUP

export NODE_PATH="$DEPS_DIR"
"$NODE_BIN" "$ROOT_DIR/server/test/audit_connection_guard.test.js"
"$NODE_BIN" "$ROOT_DIR/server/test/audit_http_timeout.test.js"

if [[ "${AUDIT_TEST_FAULT:-}" == 'port-allocation' ]]; then
  echo '[RUNNER] Controlled port allocation failure injected.' >&2
  exit 31
fi
if [[ "${AUDIT_TEST_FAULT:-}" == 'initdb' ]]; then
  PG_TEMP="$(mktemp -d /tmp/perazim-antigravity-runner.XXXXXX)"
  chmod 700 "$PG_TEMP"
  PG_DATA="$PG_TEMP/data"
  echo '[RUNNER] Controlled PostgreSQL startup failure injected.' >&2
  exit 32
fi

PG_TEMP="$(mktemp -d /tmp/perazim-antigravity-runner.XXXXXX)"
chmod 700 "$PG_TEMP"
PG_DATA="$PG_TEMP/data"
PG_SOCKET="$PG_TEMP/socket"
mkdir -m 700 "$PG_SOCKET"

if ! PGPORT="$("$NODE_BIN" -e 'const net=require("net"); const s=net.createServer(); s.listen(0,"127.0.0.1",()=>{const p=s.address().port;s.close(()=>process.stdout.write(String(p)));});')"; then
  echo '[RUNNER] Loopback port allocation failed.' >&2
  exit 33
fi
[[ "$PGPORT" =~ ^[0-9]+$ ]] || { echo '[RUNNER] Invalid allocated port' >&2; exit 33; }

echo "[RUNNER] Source root: $ROOT_DIR"
echo "[RUNNER] Source branch: $(git -C "$ROOT_DIR" branch --show-current)"
echo "[RUNNER] Source revision: $(git -C "$ROOT_DIR" rev-parse HEAD)"
STATUS_RECORD="$(git -C "$ROOT_DIR" status --porcelain=v1 --untracked-files=all)"
echo "[RUNNER] Source worktree identity: $( [[ -z "$STATUS_RECORD" ]] && echo clean || echo dirty ); changed paths: $(printf '%s\n' "$STATUS_RECORD" | sed '/^$/d' | wc -l | tr -d ' '); status fingerprint: $(printf '%s' "$STATUS_RECORD" | sha256sum | awk '{print $1}')"
echo "[RUNNER] PostgreSQL tool root: $SHARED_TOOL_ROOT; disposable data: $PG_DATA"
"$PG_TOOLS/initdb" -D "$PG_DATA" -U postgres -L "$SHARED_TOOL_ROOT/share/pgsql" \
  --auth-local=trust --auth-host=trust --locale=C.UTF-8 >/dev/null

if [[ "${AUDIT_TEST_FAULT:-}" == 'pg-start' ]]; then
  echo '[RUNNER] Controlled PostgreSQL postmaster startup failure injected.' >&2
  exit 34
fi
"$PG_TOOLS/pg_ctl" -D "$PG_DATA" -l "$PG_TEMP/postgres.log" \
  -o "-h 127.0.0.1 -p $PGPORT -k $PG_SOCKET -c dynamic_library_path=$SHARED_TOOL_ROOT/lib64/pgsql" -w start >/dev/null

export PHASE_B_TEST_ADMIN_URL="postgres://postgres@127.0.0.1:$PGPORT/postgres"
export PHASE_B_TEST_EXPECTED_PORT="$PGPORT"
export PHASE_B_TEST_EXPECTED_DATA_DIR="$PG_DATA"
export NODE_PATH="$DEPS_DIR"
NODE_ARGS=("$ROOT_DIR/server/test/antigravity_security_audit.test.js")
case "${AUDIT_TEST_FAULT:-}" in
  child-failure) NODE_ARGS+=(--self-test-child-failure) ;;
  child-pass) NODE_ARGS+=(--self-test-child-pass) ;;
  shutdown-failure) NODE_ARGS+=(--self-test-child-pass) ;;
  timeout) NODE_ARGS+=(--self-test-sleep) ;;
  node-start) NODE_BIN="$PG_TEMP/no-such-node" ;;
esac

echo "[RUNNER] Running audit with ${DEADLINE_SECONDS}s overall deadline."
setsid "$TIMEOUT_BIN" --signal=TERM --kill-after=3s "${DEADLINE_SECONDS}s" \
  "$NODE_BIN" "${NODE_ARGS[@]}" &
WORKER_PID=$!
WORKER_GROUP=$WORKER_PID
if wait "$WORKER_PID"; then
  WORKER_PID=''
  echo '[RUNNER] Audit child exited successfully.'
else
  EXIT_CODE=$?
  WORKER_PID=''
  echo "[RUNNER] Audit child failed with exit code $EXIT_CODE." >&2
  exit "$EXIT_CODE"
fi
