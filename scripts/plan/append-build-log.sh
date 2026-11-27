#!/usr/bin/env bash
# Appends commits made since the last logged commit to docs/plan/BUILD-LOG.md.
# The log stores the last logged commit in a marker line, so each run only adds new work.
# Run by .github/workflows/plan-build-log.yml after each push to the plan branch.
set -euo pipefail
LOG="docs/plan/BUILD-LOG.md"
test -f "$LOG" || { echo "missing $LOG" >&2; exit 1; }

head_sha=$(git rev-parse HEAD)
last=$(grep -oE 'last-logged: [0-9a-f]{7,40}' "$LOG" | tail -1 | awk '{print $2}' || true)
if [ -n "$last" ] && git cat-file -e "$last^{commit}" 2>/dev/null; then
  range="$last..HEAD"
else
  range="HEAD~20..HEAD"
fi

entries=$(TZ=UTC git log --no-merges --reverse --format='- %ad · `%h` · %s' \
  --date=format:'%Y-%m-%d %H:%M UTC' $range -- . ':(exclude)docs/plan/BUILD-LOG.md' || true)

if [ "$last" = "$head_sha" ] || [ -z "$entries" ]; then
  echo "No new commits to log."
  exit 0
fi

sed -i '/last-logged: /d' "$LOG"
{
  echo
  echo "### $(TZ=UTC date +'%Y-%m-%d %H:%M UTC') · up to \`$(git rev-parse --short HEAD)\`"
  echo "$entries"
  echo "<!-- last-logged: $head_sha -->"
} >> "$LOG"
echo "Logged $(echo "$entries" | wc -l) commit(s)."
