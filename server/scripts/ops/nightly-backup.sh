#!/usr/bin/env bash
# Runs the encrypted backups for the pilot and synthetic databases. Scheduled by cron at 02:00.
set -uo pipefail
SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
KEY="$HOME/.config/perazim/backup.key"
LOG="$SERVER_DIR/.runtime/backup.log"
mkdir -p "$SERVER_DIR/.runtime"
{
  echo "== $(date -u +%FT%TZ) nightly backup"
  ( set -a; . "$SERVER_DIR/.runtime/pilot/api.env"; set +a
    "$SERVER_DIR/scripts/ops/backup-db.sh" "$DATABASE_URL" "$SERVER_DIR/.runtime/backups-pilot" "$KEY" 14 )
  echo "pilot exit=$?"
  "$SERVER_DIR/scripts/ops/backup-db.sh" "postgres://postgres@127.0.0.1:55470/perazim_phaseb_synthetic01_test" \
    "$SERVER_DIR/.runtime/backups-synthetic" "$KEY" 14
  echo "synthetic exit=$?"
  # Off-site copy. A failure here is logged and does not undo the local backups.
  "$SERVER_DIR/scripts/ops/offsite-sync.sh" offsite:perazim-backups
  echo "offsite exit=$?"
} >> "$LOG" 2>&1
