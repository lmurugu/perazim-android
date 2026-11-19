#!/usr/bin/env bash
set -uo pipefail

# The emulator's system services can lag behind sys.boot_completed; wait for them before Gradle installs.
wait_for_system_services() {
  timeout 300 bash -c 'for svc in package mount activity; do until adb shell service check "$svc" 2>/dev/null | grep -q ": found"; do sleep 2; done; done'
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
  # Retry only when the emulator failed to install or lost the device. A failing test fails the job now.
  if ! grep -q -E 'Failed to install split APK|Failure calling service package|Failed to commit install session|device offline|Broken pipe' "$log"; then
    echo 'Gradle failed for a reason other than the emulator install; not retrying.' >&2
    exit 1
  fi
  echo "attempt $attempt could not install on the emulator; waiting 30 seconds before retrying"
  sleep 30
done

echo 'FAIL: the database suites could not install on the emulator after three attempts.' >&2
exit 1
