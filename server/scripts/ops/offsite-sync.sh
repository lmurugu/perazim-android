#!/usr/bin/env bash
# Copies the encrypted backup files to an off-site rclone remote. Credentials live only in rclone's own
# config (rclone config), never in this repository. Usage: offsite-sync.sh <remote:path>
# Only *.dump.enc files are copied. Plaintext dumps are never created by the backup script.
set -euo pipefail
REMOTE="${1:?remote path required, e.g. offsite:perazim-backups}"
SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
command -v rclone >/dev/null || { echo "rclone is not installed" >&2; exit 2; }
rclone copy "$SERVER_DIR/.runtime/backups-pilot" "$REMOTE/pilot" --include "*.dump.enc" --log-level INFO
rclone copy "$SERVER_DIR/.runtime/backups-synthetic" "$REMOTE/synthetic" --include "*.dump.enc" --log-level INFO
rclone check "$SERVER_DIR/.runtime/backups-pilot" "$REMOTE/pilot" --include "*.dump.enc" --one-way
echo "offsite sync ok: $REMOTE"
