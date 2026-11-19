#!/usr/bin/env bash
set -uo pipefail

# The emulator's system services can lag behind sys.boot_completed; wait for them before Gradle installs.
wait_for_system_services() {
  timeout 300 bash -c 'for svc in package mount activity; do until adb shell service check "$svc" 2>/dev/null | grep -q ": found"; do sleep 2; done; done'
}

# Print the emulator's crash buffer and recent logcat so the job log shows what the system was doing.
dump_logcat() {
  echo '--- emulator crash buffer ---'
  timeout 60 adb logcat -d -b crash -t 200 2>/dev/null | tr -d '\r' || true
  echo '--- emulator logcat (last 400 lines) ---'
  timeout 60 adb logcat -d -t 400 2>/dev/null | tr -d '\r' || true
  echo '--- end of emulator logcat ---'
}

mkdir -p build/reports
log=build/reports/database-suites-gradle.log

for attempt in 1 2 3; do
  wait_for_system_services
  if ./gradlew connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.class=org.perazimchurch.app.data.local.DatabaseEncryptionMigrationTest,org.perazimchurch.app.data.local.PerazimDatabaseMigrationTest \
      --stacktrace 2>&1 | tee "$log"; then
    exit 0
  fi
  # Retry only when the emulator failed to install, lost the device, or aborted the run mid-way.
  # A test that fails on its own assertion still fails the job at once.
  if ! grep -q -E 'Failed to install split APK|Failure calling service package|Failed to commit install session|device offline|Broken pipe|INSTRUMENTATION_ABORTED|System has crashed|Test run failed to complete' "$log"; then
    echo 'Gradle failed for a reason other than the emulator; not retrying.' >&2
    dump_logcat
    exit 1
  fi
  echo "attempt $attempt lost the emulator (install or aborted run); waiting 30 seconds before retrying"
  dump_logcat
  sleep 30
done

echo 'FAIL: the database suites could not finish on the emulator after three attempts.' >&2
exit 1
