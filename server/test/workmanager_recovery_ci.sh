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

# 1. Scheduler test: must pass normally, with no process death.
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.perazimchurch.app.sync.BackgroundSyncSchedulerTest \
  --stacktrace

# 2. Recovery test: the test enqueues the work, waits until WorkManager reports it ENQUEUED or BLOCKED,
#    then kills its own process with SIGKILL. The crash is expected, so Gradle's exit status is ignored
#    here, but the test case must have started and recorded the crash.
set +e
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.perazimchurch.app.sync.BackgroundSyncProcessRecoveryTest \
  -Pandroid.testInstrumentationRunnerArguments.simulateProcessDeath=true \
  --stacktrace
gradle_status=$?
set -e
echo "Recovery test Gradle exit status: $gradle_status (non-zero is expected after the self-kill)" >&2

report_dir='build/outputs/androidTest-results/connected'
recovery_report="$(grep -rl 'queuedSyntheticMutationIsPersistedForBackgroundDelivery' "$report_dir" 2>/dev/null | head -n 1 || true)"
if [[ -z "$recovery_report" ]]; then
  echo 'FAIL: the recovery test case did not run, so the process death could not be simulated.' >&2
  exit 1
fi
echo "Recovery test report: $recovery_report" >&2
grep -i -E 'crash|killed|SIGKILL' "$recovery_report" | head -n 5 >&2 || true

# The process must be gone after the self-kill. Restore network with no app relaunch.
app_pid="$(adb shell pidof org.perazimchurch.app 2>/dev/null | tr -d '\r' | awk '{print $1}' || true)"
if [[ -n "$app_pid" ]]; then
  echo "FAIL: app process $app_pid still alive after the self-kill; the process death was not simulated." >&2
  exit 1
fi
echo 'Package state after the simulated process death (stopped flag and JobScheduler entry):' >&2
adb shell dumpsys package org.perazimchurch.app | grep -i 'stopped' | head -n 4 >&2 || echo 'stopped flag: not reported by dumpsys package' >&2
adb shell dumpsys jobscheduler | grep -i -B2 -A6 'org.perazimchurch' | head -n 30 >&2 || echo 'JobScheduler: no job for org.perazimchurch before network restore' >&2

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
  adb shell dumpsys package org.perazimchurch.app | grep -i 'stopped' | head -n 4 >&2 || true
  adb shell dumpsys jobscheduler | grep -i -B2 -A8 'org.perazimchurch' | head -n 40 >&2 || true
  adb logcat -d -s WorkManager:* BackgroundSync:* ApiClient:* JobScheduler:* 2>/dev/null | tail -n 200 >&2 || true
  echo 'Mock server received:' >&2
  curl -fsS http://127.0.0.1:18080/__received >&2 || true
  echo >&2
  echo 'FAIL: queued synthetic mutation was not delivered after process death without reopening the app.' >&2
  exit 1
fi

echo 'PASS: queued synthetic mutation reached the mock API after the app process was killed, without reopening the app.'
