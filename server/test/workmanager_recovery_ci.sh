#!/usr/bin/env bash
set -euo pipefail

# Right after boot the package service can lag behind sys.boot_completed; wait for it.
timeout 300 bash -c 'until adb shell service check package 2>/dev/null | grep -q "package: found"; do sleep 2; done'

pm_list=$(adb shell pm list packages 2>/dev/null | tr -d '\r' || true)
echo "packages matching perazim: $(echo "$pm_list" | grep -i perazim | tr '\n' ' ')"
app_pkg=$(echo "$pm_list" | sed 's/^package://' | grep -i perazimchurch | grep -v -i test | head -1 || true)
app_pkg=${app_pkg:-org.perazimchurch.app}
echo "installed app package: $app_pkg"
# No testApplicationId is set in build.gradle, so the instrumentation package is the app package plus .test.
test_pkg="${app_pkg}.test"

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
  if curl -fsS http://127.0.0.1:18080/__received >/dev/null 2>&1; then
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
echo 'mock server ready on 127.0.0.1:18080'

# Build and install both APKs ourselves. The Gradle connected task removes the app after its run,
# which would also delete the queued WorkManager row that the process-death check depends on.
./gradlew assembleDebug assembleDebugAndroidTest --stacktrace
app_apk=$(find build/outputs/apk/debug -maxdepth 1 -name '*.apk' | head -1 || true)
test_apk=$(find build/outputs/apk/androidTest/debug -maxdepth 1 -name '*.apk' | head -1 || true)
echo "app apk: ${app_apk:-missing}"
echo "test apk: ${test_apk:-missing}"
if [[ -z "$app_apk" || -z "$test_apk" ]]; then
  echo 'FAIL: debug app or test APK was not built.' >&2
  exit 1
fi
adb install -r "$app_apk"
adb install -r "$test_apk"

adb shell svc wifi disable
adb shell svc data disable || true  # the emulator image may not expose the phone service

mkdir -p build/reports/androidTests/connected
instrument_out=$(adb shell am instrument -w \
  -e class org.perazimchurch.app.sync.BackgroundSyncSchedulerTest,org.perazimchurch.app.sync.BackgroundSyncProcessRecoveryTest \
  "$test_pkg/androidx.test.runner.AndroidJUnitRunner" 2>&1 | tr -d '\r' || true)
echo "$instrument_out" | tee build/reports/androidTests/connected/instrumentation-output.txt
if ! grep -q '^OK (' <<<"$instrument_out"; then
  echo 'FAIL: sync instrumentation tests did not pass.' >&2
  exit 1
fi

if ! adb shell pm path "$app_pkg" 2>/dev/null | tr -d '\r' | grep -q '^package:'; then
  echo "FAIL: $app_pkg is not installed after the instrumentation run." >&2
  exit 1
fi

# Kill the app process, not force-stop it, so JobScheduler may restart persisted work.
# Do not open the app again; restore network and wait for the mock API to receive the row.
# Start the app in the background so a real process exists for the kill. am kill only reaches
# background processes, so send HOME after launch and wait before killing. Network stays off here.
monkey_out=$(adb shell monkey -p "$app_pkg" -c android.intent.category.LAUNCHER 1 2>&1 || true)
echo "monkey launch output: ${monkey_out:-empty}"
sleep 5
adb shell input keyevent KEYCODE_HOME || true
sleep 10
app_pid=''
for _ in $(seq 1 15); do
  app_pid=$(adb shell pidof "$app_pkg" 2>/dev/null | tr -d '\r' || true)
  [[ -n "$app_pid" ]] && break
  sleep 2
done
echo "app pid before kill: ${app_pid:-none}"
if [[ -z "$app_pid" ]]; then
  echo '== processes matching perazim (job log copy)'; adb shell ps -A 2>/dev/null | grep -i perazim || echo 'no perazim processes'
  echo '== fatal or crash lines (job log copy)'; adb logcat -d 2>/dev/null | grep -i -E 'FATAL|AndroidRuntime|Process .*(died|killed)' | tail -40 || true
  echo 'FAIL: app process is not running before the kill, so the process-death check proves nothing.' >&2
  exit 1
fi

# Retry the kill until the process is really gone. A single am kill can be ignored while the app
# is still settling into the background, and the old check then tested nothing.
killed=false
for _ in $(seq 1 10); do
  adb shell am kill "$app_pkg" >/dev/null 2>&1 || true
  sleep 2
  if [[ -z "$(adb shell pidof "$app_pkg" 2>/dev/null | tr -d '\r' || true)" ]]; then
    killed=true
    break
  fi
done
echo "app process killed: $killed"
if [[ "$killed" != true ]]; then
  echo '== process state (job log copy)'; adb shell dumpsys activity processes 2>/dev/null | grep -i -A3 perazim | head -40 || true
  echo 'FAIL: am kill did not stop the app process, so the process-death check proves nothing.' >&2
  exit 1
fi
echo 'app pid after kill: none'

adb shell svc wifi enable
adb shell svc data enable || true
# The emulator can take minutes to bring up a default network after svc wifi enable, and JobScheduler
# will not run the CONNECTED job until it has one. Wait for it before the delivery wait starts.
network_up=false
for _ in $(seq 1 90); do
  if adb shell dumpsys connectivity 2>/dev/null | tr -d '\r' | grep -q 'Active default network: [0-9]'; then
    network_up=true
    break
  fi
  sleep 2
done
echo "default network up: $network_up"
if [[ "$network_up" != true ]]; then
  echo '== connectivity (job log copy)'; adb shell dumpsys connectivity 2>/dev/null | grep -i 'active default' | head -3 || true
  echo 'FAIL: the emulator never brought up a default network after svc wifi enable.' >&2
  exit 1
fi

received=false
for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:18080/__received 2>/dev/null | grep -q 'wm_process_restart_probe'; then
    received=true
    break
  fi
  sleep 2
done
if [[ "$received" != true ]]; then
  # Keep evidence for the report: app log, JobScheduler and network state, and the mock log.
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
