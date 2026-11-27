#!/usr/bin/env bash
# Recovery restores the pre-change dump into a NEW database, so the damaged database is never
# dropped or overwritten. Stop the API first. After verifying the new database, point
# DATABASE_URL at it (and keep the damaged database for review).
# Usage: PILOT_HOST_URL=postgresql://postgres@127.0.0.1:55438 ./recover-from-backup.sh <dump> <new_db_name>
set -euo pipefail
: "${PILOT_HOST_URL:?set PILOT_HOST_URL to the server URL without a database name}"
dump="${1:?usage: recover-from-backup.sh <dump file> <new database name>}"
newdb="${2:?usage: recover-from-backup.sh <dump file> <new database name>}"
[[ "$newdb" =~ ^[a-z0-9_]+$ ]] || { echo "bad database name"; exit 1; }
sha256sum --check "$dump.sha256"
pg_restore --list "$dump" > /dev/null
psql -X -v ON_ERROR_STOP=1 "$PILOT_HOST_URL/postgres" -c "CREATE DATABASE $newdb TEMPLATE template0"
pg_restore --no-owner --exit-on-error --dbname="$PILOT_HOST_URL/$newdb" "$dump"
echo "restored into $newdb; verify with pre-flight.sql, then point DATABASE_URL at it"
