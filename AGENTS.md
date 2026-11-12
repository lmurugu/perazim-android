# Perazim app: complete every authorized update

## Mandatory AI roadmap closure

For any Perazim AI, online/offline model, knowledge-package, retrieval,
grounding, citation, or AI-backend task, use the project skill at
`.agents/skills/perazim-ai-roadmap-closure/SKILL.md`. Treat the current project
roadmap as acceptance criteria, track every criterion with evidence, work in
dependency order, and keep unrelated work out of scope. Provide milestone
updates at least every 30 minutes during sustained work. Do not declare
completion until all safely actionable criteria and full relevant suites pass;
record production/owner decisions separately. Existing synthetic-data,
validation-app, feature-flag, key-custody, and deployment restrictions in this
file remain controlling.

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

This execution mandate does not expand a valid task authorisation. Explicit
data, security, privacy, architecture, deployment, and organisational boundaries
remain controlling. In particular, autonomous work does not authorize handling
real membership data, changing production state, weakening safeguards, or
deploying or pushing changes.

## Instruction precedence and authorisation validity

Apply instructions in this order:

1. Latest explicit owner instruction.
2. Latest verified task-specific authorisation and restrictions.
3. Latest owner-approved PERAZIM master project memory bank.
4. Current repository instructions and technical specifications.
5. Historical instructions and previous agent reports.
6. General autonomous execution principles.

A newer explicit restriction overrides a broader standing mandate. Dated
authorisations are historical records, not permanent authority. Before relying
on one, verify it against the latest approved project memory and owner decisions.
Silence never authorises a new phase or consequential/restricted action. If the
current approved memory version cannot be identified, use explicit current
session instructions and verified repository evidence; do not silently treat a
draft or stale memory file as approved.

## Continuity and data-protection safeguards

At the start of each new implementation session, read the latest approved
master project memory bank and these repository instructions, inspect the actual
repository/working tree, and reconcile historical claims against current source.
Identify what is completed, incomplete, superseded, and awaiting a decision.
After a significant implementation task, update the relevant continuity record
with verified outcomes. Preserve history and mark superseded decisions rather
than silently deleting them. Never claim memory was updated unless the file was
actually changed and checked.

Never expose credentials, API keys, passwords, signing material, access tokens,
private member information, or sensitive runtime data in reports, commits, logs,
or generated files. Never use real membership records for testing, debugging,
demonstration, or synthetic-fixture generation. Do not send personal or
sensitive church data to a new external processor without required owner
approval and privacy assessment. Preserve authentication, authorisation, data
isolation, rate limiting, safeguarding, and existing privacy controls.

Autonomy permits technical problem-solving within the assigned objective. It
does not authorise unrelated feature expansion or changing established
requirements. Every additional change must directly support the intended
outcome. Distinguish source implementation, automated tests, device checks,
local operational verification, and production acceptance in completion reports.

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
