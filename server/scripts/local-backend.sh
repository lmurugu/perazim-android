#!/usr/bin/env bash
set -euo pipefail
SERVER_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUNTIME="$SERVER_DIR/.runtime"
PGROOT="$RUNTIME/pgsql/usr"
export LD_LIBRARY_PATH="$PGROOT/lib64${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export PATH="$PGROOT/bin:$PATH"
mkdir -p "$RUNTIME/socket"
chmod 700 "$RUNTIME" "$RUNTIME/socket"
case "${1:-status}" in
  start-db)
    if [[ ! -f "$RUNTIME/data/PG_VERSION" ]]; then
      initdb -D "$RUNTIME/data" -L "$PGROOT/share/pgsql" --username=runtime_owner --auth-local=trust --auth-host=scram-sha-256 --locale=C.UTF-8
      cat >> "$RUNTIME/data/postgresql.conf" <<CONFIG
listen_addresses = '127.0.0.1'
port = 5433
unix_socket_directories = '$RUNTIME/socket'
dynamic_library_path = '$PGROOT/lib64/pgsql'
CONFIG
    fi
    if ! pg_ctl -D "$RUNTIME/data" status >/dev/null 2>&1; then
      pg_ctl -D "$RUNTIME/data" -l "$RUNTIME/postgres.log" -w start
    fi
    ;;
  start)
    "$0" start-db
    if [[ ! -f "$RUNTIME/backend.env" ]]; then
      node - "$RUNTIME/backend.env" <<'JS'
const fs=require('fs'),crypto=require('crypto');const password=crypto.randomBytes(32).toString('hex');
fs.writeFileSync(process.argv[2],`DATABASE_URL=postgres://perazim_local:${password}@127.0.0.1:5433/perazim_local\nJWT_SECRET=${crypto.randomBytes(48).toString('hex')}\nNODE_ENV=production\nPORT=3000\n`,{mode:0o600});
JS
    fi
    set -a
    source "$RUNTIME/backend.env"
    set +a
    # Create the app role using stdin; never print private configuration.
    node - <<'JS' | psql -h "$RUNTIME/socket" -p 5433 -U runtime_owner -d postgres -v ON_ERROR_STOP=1 >/dev/null
const u=new URL(process.env.DATABASE_URL);console.log(`SELECT 'CREATE ROLE perazim_local LOGIN PASSWORD ''${u.password}''' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname='perazim_local')\n\\gexec\nSELECT 'CREATE DATABASE perazim_local OWNER perazim_local' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='perazim_local')\n\\gexec`);
JS
    if [[ -f "$RUNTIME/api.pid" ]] && kill -0 "$(cat "$RUNTIME/api.pid")" 2>/dev/null; then
      echo 'API already running'; exit 0
    fi
    cd "$SERVER_DIR"
    nohup setsid node src/index.js >> "$RUNTIME/api.log" 2>&1 < /dev/null &
    echo "$!" > "$RUNTIME/api.pid"
    # Keep the launching shell alive until the detached API has actually bound
    # its socket; returning immediately can race process startup in task runners.
    ready=false
    for attempt in {1..15}; do
      if curl --fail --silent --max-time 2 http://127.0.0.1:3000/api/health >/dev/null; then
        ready=true
        break
      fi
      sleep 1
    done
    if [[ "$ready" != true ]]; then
      echo 'Local API did not become healthy; inspect .runtime/api.log.' >&2
      exit 1
    fi
    echo 'Started local API on port 3000; PostgreSQL stays on loopback port 5433.'
    ;;
  stop)
    if [[ -f "$RUNTIME/api.pid" ]]; then
      pid="$(cat "$RUNTIME/api.pid")"
      if [[ -r "/proc/$pid/cmdline" ]] && tr '\0' ' ' < "/proc/$pid/cmdline" | grep -q 'node src/index.js'; then kill "$pid"; fi
      rm -f "$RUNTIME/api.pid"
    fi
    pg_ctl -D "$RUNTIME/data" -w stop
    ;;
  status)
    pg_ctl -D "$RUNTIME/data" status
    curl --fail --silent http://127.0.0.1:3000/api/health
    ;;
  *) echo 'Usage: local-backend.sh start-db|start|stop|status' >&2; exit 2 ;;
esac
