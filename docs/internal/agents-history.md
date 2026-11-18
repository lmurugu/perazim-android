# Agent instruction history (moved from AGENTS.md, 2026-11-18)

These dated owner instructions are kept for the record. Where they conflict with the current authorization in `AGENTS.md`, the current block wins.

# Perazim app: complete every authorized update

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

---

# Dated sections moved from AGENTS.md on merge

## Cost-conscious model preference

Use the owner's preferred model and effort when available. The recorded
preference is GPT-6.1 Sol with low effort (owner's “Light”) for the main agent
and Luna with high effort for bounded sub-agent tasks. If a requested model
identifier is unavailable, report the actual available identifier and never
claim or silently imply that a different model was used. Keep delegation
minimal; delegate only when it materially improves the outcome. Avoid duplicate
reads/tests. Do not increase model cost, effort, or delegation beyond the
owner's stated preference without approval.

## Historical authorisation record: Phase B B1–B7 (2026-11-07)

The owner recorded authorisation on 2026-11-07 for bounded Phase B B1–B7 work:
synthetic membership verification/persistence, current backend authority, private
claims/review, synthetic administrator security, Android integration, and isolated
acceptance tests. This dated record does not itself authorise a new task or phase;
verify its current validity and boundaries against the latest approved project
memory and explicit owner decisions before relying on it.

The recorded limits are: no real membership records or real administrator
activation; no migration/adoption of an existing member database; no production
deployment, push, live-service restart, provider configuration changes, or Phase
C; uniquely named disposable databases for security/migration fixtures; preserve
project specs, Decisions Register, baseline migration checksum and existing data;
keep Room destructive fallback until historical support is resolved and removal
separately authorised; preserve public/offline and ordinary registered features;
client roles and snapshots never establish current authority. These limits remain
in force wherever the latest owner-approved instructions retain them.

Device-level acceptance must use the separate validation app with synthetic,
isolated fixtures until the owner explicitly authorises another route. Do not
install a new main-app build or connect validation to the running member backend.
This overrides older phone-install or automatic restart workflows.

## Historical development scope (owner instruction, 2026-11-06)

This is a dated historical record, not permanent authority. Verify it against the
latest approved project memory and current explicit owner decisions before using
it to justify a new task or consequential action.

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
3. Android code may be built and tested with authorised automated checks. Do not
   install a new main-app build. Device-level acceptance tests must use only the
   separate validation app and synthetic fixtures until the owner explicitly
   authorises otherwise. Never clear data, uninstall, downgrade, log out, or
   interrupt unrelated device activity.
4. Never restart production or live member-facing services under this
   authorisation. A local development backend may be restarted only after
   positively identifying it as the configured development instance and
   preserving its runtime data. Then verify its health endpoint. If the instance
   or data boundary is uncertain, do not restart it; complete independent work
   and report the limitation. Never silently change an account's server endpoint
   or publish the PC backend to the internet.
5. Before committing, establish which changes belong to the assigned task and
   which predate it. Stage only attributable task changes. Never include
   unrelated, pre-existing, untracked, or unfinished user work. If changes
   overlap and cannot be isolated safely, preserve the working tree and report
   the limitation instead of committing. Review staged files for secrets and
   personal captures. Keep credentials, runtime databases, signing keys, APKs,
   and local configuration out of Git. Do not amend commits, switch branches,
   merge, force-push, or push remotely without separate authorisation.
6. Report the commit hash if committed, exact check results, remaining working-tree
   changes, device verification status, backend operational status, and whether
   the commit was pushed. If a check fails or access is unavailable, finish
   independent work and state the exact remaining action; never claim everything
   is synchronized.

These are standing instructions for agents working in the repository. They do
not install a background watcher, commit unfinished edits on every file save, or
update an offline phone. Documentation-only changes need no app rebuild/reinstall
or backend restart when their running code already matches the verified build.
