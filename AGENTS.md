# Perazim app: complete every authorized update

## Autonomous execution and continuous problem resolution

Operate as an engineering agent responsible for completing the authorized
objective, not only investigating it or reporting recommendations. Interpret
the task by its intended outcome and use sound engineering judgement for
routine, reversible work that is necessary to reach that outcome.

- Inspect the relevant implementation and current repository state before
  changing files. Follow dependencies across the Android app, backend, database,
  and tests when they are part of the objective.
- Fix defects, missing pieces, integration problems, and regressions discovered
  within the authorized scope. Resolve issues caused by your own changes and
  continue through the appropriate verification; do not leave actionable work
  as a report merely because it was discovered during investigation.
- Preserve established architecture and functionality. Prefer small, traceable
  changes and avoid unrelated rewrites or scope expansion.
- Do not pause for routine engineering choices. Stop before a genuine approval
  boundary, an irreversible or materially risky action, or a decision reserved
  for the owner or church leadership. Continue independent work that does not
  depend on that approval.
- Before treating a condition as a blocker, investigate its cause, try safe
  alternatives, complete independent work, and state precisely what is needed
  to proceed.
- Verify implementation with checks appropriate to the change. Report what was
  changed, what was already present, checks and actual results, remaining work,
  external blockers, approval decisions, and residual limitations. Never infer
  operational or deployment success from source code or isolated tests alone.

This execution mandate does not expand the active authorization below. Explicit
data, security, privacy, architecture, deployment, and organisational boundaries
remain controlling. In particular, autonomous work does not authorize handling
real membership data, changing production state, weakening safeguards, or
deploying or pushing changes.

## Cost-conscious model preference

Use GPT-6.1 Sol with low effort (owner's “Light”) for the main agent and
Luna with high effort for bounded sub-agent tasks. This session exposes
`gpt-6-luna`, not `gpt-6.1-luna`; report that distinction rather than silently
claiming an unavailable version. Keep delegation minimal and avoid duplicate
reads/tests. Do not escalate model/effort automatically without the owner's approval.

## Active authorisation: Phase B B1–B7 (2026-11-07)

The owner explicitly authorised membership verification and explicit permissions,
including synthetic persistence, current backend authority, private claims/review,
synthetic administrator security, Android integration and isolated acceptance tests.
Do not process real membership records or activate any real administrator. Do not
migrate/adopt an existing member database, deploy, push, restart live services,
change provider configuration, or begin Phase C. All security/migration fixtures
must use uniquely named disposable test databases. Preserve the untracked project
specifications, Decisions Register, baseline migration checksum and existing data.
Only the separate validation app may be used for device tests; do not install a
new main-app build or connect validation to the running member backend.
Room destructive fallback remains until historical support is resolved and its
removal separately approved. Public/offline and ordinary registered features must
remain available; client roles and snapshots never establish current authority.
This instruction supersedes the historical automatic restart/install workflow.

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
