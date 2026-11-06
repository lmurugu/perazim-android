# Perazim app: complete every authorized update

## Phase A acceptance hold (owner instruction, 2026-11-06)

Phase A is NOT accepted. Work only on its five blockers; do not start or extend
Phase B. Do not publish a Phase A complete report until all five are addressed:
real verse screenshots for KJV/SUV/NIV/ESV/NKJV, public HTTPS deployment,
complete campus label/row removal, progress-writing helper audit/remediation,
and zero starting grace points unless the owner explicitly approves otherwise.

Do not run multi-user acceptance tests until the public HTTPS endpoint is up
and reachable from a phone using normal mobile data with Wi-Fi and USB tunneling
excluded. Local, LAN, emulator, and `adb reverse` results are development evidence
only. Never count them as network acceptance. This gate overrides the general
test/synchronization workflow below where it would run those acceptance tests.

The owner wants changes followed through without reminders. Apply this workflow
at the end of each authorized implementation task in this repository.

1. Inspect the working tree first. Preserve unrelated work, existing accounts,
   local configuration, and device data. Stage only the task's intended changes.
2. Run checks appropriate to the change and `git diff --check`. For Android
   code/resources, run `./gradlew testDebugUnitTest assembleDebug --offline` when
   dependencies are available. For backend changes, run the applicable npm
   suites; use the disposable test database, never the live app database.
3. When Android changes, update the connected, authorized phone in place with
   `adb install -r build/outputs/apk/debug/app-debug.apk`. Select an explicit
   device if more than one is connected. Never uninstall, clear data, downgrade,
   log out, or navigate a phone being used personally. Verify the installed
   base APK's SHA-256 matches the built APK; a version label alone is insufficient.
4. When backend code changes, restart the configured local PC backend using
   `server/scripts/local-backend.sh stop` then `start`, and verify
   `http://127.0.0.1:3000/api/health`. Preserve `server/.runtime/` and its data.
   Re-establish `adb reverse tcp:3000 tcp:3000` for the connected development
   phone when its configured backend is localhost. Never silently change an
   account's server endpoint or publish the PC backend to the internet.
5. After successful relevant checks, commit the completed task on the current
   working branch without asking for another reminder or confirmation. Review
   staged files for secrets and personal captures. Keep credentials, runtime
   databases, signing keys, APKs, and local configuration out of Git. Do not
   include unrelated unfinished work, amend existing commits, merge into main,
   force-push, or push remotely unless that action is separately authorized.
6. Report the commit hash, check results, working-tree status, phone installation
   status, backend health, and whether the commit was pushed. If a phone is
   disconnected, a check fails, or access is unavailable, finish independent work
   and state the exact remaining update; never claim everything is synchronized.

These are standing instructions for agents working in the repository. They do
not install a background watcher, commit unfinished edits on every file save, or
update an offline phone. Documentation-only changes need no app rebuild/reinstall
or backend restart when their running code already matches the verified build.
