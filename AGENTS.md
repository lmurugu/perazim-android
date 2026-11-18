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
