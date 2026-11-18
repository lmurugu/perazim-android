#!/usr/bin/env bash
# Encrypted PostgreSQL dump with retention.
# Usage: backup-db.sh <database-url> <backup-dir> <key-file> [retention-days]
# The dump is encrypted while it is written (no plaintext file on disk). The key file lives outside the
# project and must be copied to a second, separate place: without it the backups cannot be restored.
set -euo pipefail
DB_URL="${1:?database url required}"
BACKUP_DIR="${2:?backup directory required}"
KEY_FILE="${3:?key file required}"
RETENTION_DAYS="${4:-14}"

[ -s "$KEY_FILE" ] || { echo "key file missing or empty: $KEY_FILE" >&2; exit 2; }
umask 077
mkdir -p "$BACKUP_DIR"; chmod 700 "$BACKUP_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
NAME="$(echo "$DB_URL" | sed -E 's#.*/([^/?]+)(\?.*)?$#\1#')"
OUT="$BACKUP_DIR/${NAME}-${STAMP}.dump.enc"
TMP="$OUT.partial"

pg_dump --format=custom --no-owner "$DB_URL" \
  | openssl enc -aes-256-cbc -pbkdf2 -iter 200000 -salt -pass "file:$KEY_FILE" -out "$TMP"
mv "$TMP" "$OUT"
chmod 600 "$OUT"
# Verify: decrypt to a pipe (never to disk) and check the archive table of contents.
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass "file:$KEY_FILE" -in "$OUT" \
  | pg_restore --list > /dev/null
echo "backup ok: $OUT ($(stat -c %s "$OUT") bytes, encrypted)"

find "$BACKUP_DIR" -maxdepth 1 -name "${NAME}-*.dump.enc" -mtime +"$RETENTION_DAYS" -print -delete
