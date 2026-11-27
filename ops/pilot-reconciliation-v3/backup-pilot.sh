#!/usr/bin/env bash
# Takes a fresh custom-format dump BEFORE any reconciliation or migration, verifies it is readable,
# and writes a SHA-256 checksum beside it. Prints the dump path. Never prints the connection string.
set -euo pipefail
: "${DATABASE_URL:?set DATABASE_URL for perazim_pilot}"
dir="${BACKUP_DIR:-$HOME/perazim-pilot-backups}"
mkdir -p "$dir"
file="$dir/perazim_pilot_pre-0022-0023_$(date -u +%Y%m%dT%H%M%SZ).dump"
pg_dump "$DATABASE_URL" --format=custom --file="$file"
pg_restore --list "$file" > /dev/null          # fails if the archive is unreadable
sha256sum "$file" > "$file.sha256"
echo "$file"
