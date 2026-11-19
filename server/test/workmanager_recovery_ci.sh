#!/usr/bin/env bash
set -euo pipefail

# Right after boot the package service can lag behind sys.boot_completed; wait for it.
timeout 300 bash -c 'until adb shell service check package 2>/dev/null | grep -q "package: found"; do sleep 2; done'

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
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.perazimchurch.app.sync.BackgroundSyncSchedulerTest,org.perazimchurch.app.sync.BackgroundSyncProcessRecoveryTest \
  --stacktrace

# Kill the app process, not force-stop it, so JobScheduler may restart persisted work.
# Do not open the app again; restore network and wait for the mock API to receive the row.
# Start the app in the background so a real process exists for the kill. am kill does not
# touch a process with a visible activity, so send HOME after launch. Network stays off here.
adb shell monkey -p org.perazimchurch.app -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
sleep 5
adb shell input keyevent KEYCODE_HOME || true
app_pid=''
for _ in $(seq 1 15); do
  app_pid=$(adb shell pidof org.perazimchurch.app 2>/dev/null | tr -d '\r' || true)
  [[ -n "$app_pid" ]] && break
  sleep 2
done
echo "app pid before kill: ${app_pid:-none}"
if [[ -z "$app_pid" ]]; then
  echo 'FAIL: app process is not running before the kill, so the process-death check proves nothing.' >&2
  exit 1
fi
adb shell am kill org.perazimchurch.app
sleep 3
echo "app pid after kill: $(adb shell pidof org.perazimchurch.app 2>/dev/null | tr -d '\r' || echo none)"
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
  # Keep evidence for the report: app log, JobScheduler and network state, and the mock log.
  mkdir -p build/reports/androidTests/connected
  {
    echo '== mock server log'; cat /tmp/workmanager-mock.log 2>/dev/null || true
    echo '== jobscheduler (app)'; adb shell dumpsys jobscheduler 2>/dev/null | grep -i -A4 'org.perazimchurch' | head -150 || true
    echo '== connectivity'; adb shell dumpsys connectivity 2>/dev/null | grep -i 'active default\|NetworkAgentInfo\|VALIDATED' | head -40 || true
    echo '== logcat (app and WorkManager)'; adb logcat -d 2>/dev/null | grep -i 'perazim\|WorkManager\|JobScheduler\|JobService\|BackgroundSync' | tail -400 || true
  } > build/reports/androidTests/connected/workmanager-diagnostics.txt 2>&1
  echo '== mock server log (job log copy)'; cat /tmp/workmanager-mock.log 2>/dev/null || true
  echo '== pending jobs for the app (job log copy)'; adb shell dumpsys jobscheduler 2>/dev/null | grep -i -A4 'org.perazimchurch' | head -60 || true
  echo 'FAIL: queued synthetic mutation was not delivered after process death without reopening the app.' >&2
  exit 1
fi

echo 'PASS: queued synthetic mutation reached the mock API after the app process was killed, without reopening the app.'
