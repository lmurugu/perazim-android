#!/usr/bin/env bash
# Checks the API health endpoint and appends one line to a log. Usage: health-check.sh <base-url> <log-file>
# Exit 0 when healthy, 1 when not. Pair with cron or a systemd timer, and an alert on exit 1.
set -uo pipefail
BASE_URL="${1:?base url required}"
LOG_FILE="${2:?log file required}"
STAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
if curl -fsS --max-time 10 "${BASE_URL%/}/api/health" > /dev/null 2>&1; then
  echo "$STAMP OK $BASE_URL" >> "$LOG_FILE"; exit 0
fi
echo "$STAMP FAIL $BASE_URL" >> "$LOG_FILE"; exit 1
