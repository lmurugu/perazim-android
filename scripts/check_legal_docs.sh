#!/usr/bin/env bash
# Fails if legal documents contain placeholders that must not ship.
# Rules:
#   1. Draft documents may contain [PENDING OWNER INPUT: ...] placeholders.
#   2. No document in docs/legal may contain TODO, "[insert" or "{{".
#   3. A document whose status line says "Status: Approved" must contain no placeholders at all.
set -Eeuo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$root/docs/legal"
failed=0
for f in *.md; do
  [ "$f" = "README.md" ] && continue
  if grep -nE 'TODO|\[insert|\{\{' "$f"; then
    echo "FAIL: forbidden token in $f" >&2; failed=1
  fi
  if grep -q '^Status: Approved' "$f" && grep -n 'PENDING OWNER INPUT' "$f"; then
    echo "FAIL: approved document $f still has placeholders" >&2; failed=1
  fi
done
[ "$failed" -eq 0 ] && echo "legal docs check passed"
exit "$failed"
