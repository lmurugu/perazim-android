#!/bin/sh
# Scratch only. Starts a throwaway Postgres 16 cluster owned by the postgres OS user, the same
# user that runs admin commands in the pilot container. Starts with trust rules, the pilot baseline.
# Nothing here touches the pilot.
set -eu
PGBIN=/usr/lib/postgresql/16/bin
DATA=${SCRATCH_HBA_DATA:-/tmp/pgscratch/hba-data}
SOCK=${SCRATCH_HBA_SOCK:-/tmp/pgscratch/hba-sock}
PORT=${SCRATCH_HBA_PORT:-55556}
mkdir -p "$SOCK" && chown postgres:postgres "$SOCK"
if [ ! -f "$DATA/PG_VERSION" ]; then
  mkdir -p "$DATA" && chown postgres:postgres "$DATA"
  runuser -u postgres -- "$PGBIN/initdb" -D "$DATA" -U postgres --auth-local=trust --auth-host=trust >/dev/null
fi
if runuser -u postgres -- "$PGBIN/pg_ctl" -D "$DATA" status >/dev/null 2>&1; then
  echo "already running on $PORT"
else
  runuser -u postgres -- "$PGBIN/pg_ctl" -D "$DATA" -l "$DATA/server.log" \
    -o "-p $PORT -k $SOCK -c listen_addresses=127.0.0.1" -w start >/dev/null
  echo "started on 127.0.0.1:$PORT, socket $SOCK"
fi
