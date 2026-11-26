#!/usr/bin/env bash
set -euo pipefail

mock_pid=''
cleanup() {
  if [[ -n "$mock_pid" ]]; then
    kill "$mock_pid" 2>/dev/null || true
    wait "$mock_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT

node server/test/workmanager_recovery_mock_server.js >/tmp/workmanager-mock.log 2>&1 &
mock_pid=$!
mock_ready=false
for _ in $(seq 1 20); do
  if curl -fsS http://127.0.0.1:18080/__received >/dev/null; then
    mock_ready=true
    break
  fi
  sleep 1
done
if [[ "$mock_ready" != true ]]; then
  cat /tmp/workmanager-mock.log
  echo 'FAIL: synthetic mock server did not start.' >&2
  exit 1
fi

adb shell svc wifi disable
adb shell svc data disable || true  # the emulator image may not expose the phone service
./gradlew connectedValidationAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.perazimchurch.app.sync.BackgroundSyncSchedulerTest,org.perazimchurch.app.sync.BackgroundSyncProcessRecoveryTest \
  --stacktrace

# Kill the app process, not force-stop it, so JobScheduler may restart persisted work.
# Do not open the app again; restore network and wait for the mock API to receive the row.
adb shell am kill org.perazimchurch.app
adb shell svc wifi enable
adb shell svc data enable || true

received=false
for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:18080/__received | grep -q 'wm_process_restart_probe'; then
    received=true
    break
  fi
  sleep 2
done
if [[ "$received" != true ]]; then
  echo 'Diagnostics: connectivity and job state after the app process was killed.' >&2
  adb shell dumpsys connectivity | grep -iE 'NetworkAgentInfo|CONNECTED|VALIDATED' | head -n 20 >&2 || true
  adb shell dumpsys jobscheduler | grep -i 'org.perazimchurch' | head -n 40 >&2 || true
  adb logcat -d -s WorkManager:* BackgroundSync:* ApiClient:* JobScheduler:* 2>/dev/null | tail -n 200 >&2 || true
  echo 'Mock server received:' >&2
  curl -fsS http://127.0.0.1:18080/__received >&2 || true
  echo >&2
  echo 'FAIL: queued synthetic mutation was not delivered after process death without reopening the app.' >&2
  exit 1
fi

echo 'PASS: queued synthetic mutation reached the mock API after the app process was killed, without reopening the app.'
