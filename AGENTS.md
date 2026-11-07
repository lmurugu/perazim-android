# Perazim app: complete every authorized update

## Cost-conscious model preference

Use GPT-6.1 Sol with low effort (owner's “Light”) for the main agent and
Luna with high effort for bounded sub-agent tasks. This session exposes
`gpt-6-luna`, not `gpt-6.1-luna`; report that distinction rather than silently
claiming an unavailable version. Keep delegation minimal and avoid duplicate
reads/tests. Do not escalate model/effort automatically without the owner's approval.

## Active authorisation: Phase A foundation only (2026-11-07)

The owner authorised foundation, baseline documentation, migration infrastructure,
synthetic preservation fixtures, backup/restore verification and regression tests
only. Do not begin membership, permission, AI gateway, knowledge, attendance,
escalation or other feature phases. Stop after reporting Phase A for review.
Preserve pre-existing working-tree content, including the untracked specification
folder. Never run migration/restore commands on existing member databases during
foundation testing. Keep destructive Room fallback until every supported upgrade
path is demonstrated; do not infer supported installed versions from source alone.
Do not deploy, change provider configuration/credentials or modify production.
This narrow authorisation supersedes the broader historical scope below.

## Historical development scope (owner instruction, 2026-11-06)

The owner has deferred public deployment and authorized community development:
feed, groups, classes, private chat, games and better AI. The previous Phase A-only
hold is superseded. Do not purchase hosting or publish the PC backend. Preserve
zero starting grace points and the progress-helper safeguards. Public acceptance
remains unverified; local tests are development evidence, not internet acceptance.
The owner explicitly authorizes opening the app over YouTube or other personal
apps on the connected test phone. Preserve accounts and data; never clear data
or log out except after a deliberate user-selected account/server change.

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
   log out, or interrupt unrelated actions beyond opening/testing this app as authorized above. Verify the installed
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
