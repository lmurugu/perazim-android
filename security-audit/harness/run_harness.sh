#!/usr/bin/env bash
# Re-runs the S2 crypto harnesses against the REAL Android sources in this repo.
# Synthetic keys only. Requires: node, javac/java, json.jar (org.json, path via JSON_JAR).
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$HERE/../../src/main/java/org/perazimchurch/app/ai/local"
JSON_JAR="${JSON_JAR:-/tmp/s2jar/json.jar}"
OUT="$HERE/out"; rm -rf "$OUT"; mkdir -p "$OUT"
cd "$HERE"
node gen_vectors.js vectors.json >/dev/null
javac -nowarn -cp "$JSON_JAR" -d "$OUT" "$REPO/Ed25519Verifier.java" java/org/perazimchurch/app/ai/local/*.java
echo "== Ed25519 verifier (real Ed25519Verifier.java) =="
java -cp "$OUT:$JSON_JAR" org.perazimchurch.app.ai.local.VerifyHarness vectors.json
echo "== canonical JSON: Node signer vs Java string escaping =="
node fuzz_strings.js
