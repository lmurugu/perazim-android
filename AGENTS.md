# Perazim app: agent instructions

## Current authorization (2026-11-17)

The owner's Portfolio-Grade Remediation & Launch Plan is the current authority. It supersedes the older "no deployment / no Phase C / validation-only" limits that used to be here. The dated instructions are in `docs/internal/agents-history.md`. The plan's summary and status are in `docs/internal/remediation-plan.md`.

Under this authority, agents may:
- follow the plan's phases in order, committing each task on a feature branch;
- draft legal, privacy and compliance text, marked as unapproved until the owner approves it.

Agents must not, without a separate owner decision:
- merge into `main`, force-push, rewrite published history, or push to `phase-B-community`;
- delete branches or tags, or change branch protection;
- deploy, publish a backend, purchase hosting, or change provider configuration;
- make the decisions the plan reserves for the owner (migration 0007, Bible licensing, minimum age, registration and legal approval, church name).

## Mandatory gates

- No real member data in tests, fixtures, commits or logs.
- No secrets, keystores, `.env` files, `google-services.json`, `local.properties` or runtime databases in Git.
- No production launch until every Launch Gate in the plan is checked.
- Generative AI stays off in release builds unless gate L7 passes.
- Steps marked [OWNER] stop for a human action or decision.

## Data protection

- Do not process, import or adopt a real membership database until gates L4 and L8 pass.
- Use uniquely named disposable databases for tests. Never point tests at the live application database.
- Do not activate a real administrator account. Use synthetic accounts only.
- Preserve existing accounts, local data and the untracked project specifications.

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
