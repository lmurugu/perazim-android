---
name: perazim-ai-roadmap-closure
description: Enforces roadmap-driven implementation and evidence-based acceptance for the Perazim Android app AI. Use whenever working on this app's AI behavior, online or offline inference, knowledge packages, grounding, retrieval, citations, package lifecycle, AI backend routes, or their acceptance reports.
---

# Perazim app AI roadmap closure

This skill is specific to the Perazim app project. It keeps work focused on
roadmap completion and verifiable acceptance. It does not grant authorization
beyond the current task or override project safeguards.

## Start from current project evidence

1. Read this project's `AGENTS.md`, latest approved master project memory,
   `docs/PERAZIM_AI_PROJECT/` roadmap/specifications, latest completion report,
   and open dependencies.
2. Inspect the working tree before editing. Preserve pre-existing and unrelated
   user changes. Reconcile historical reports with current source and test
   evidence; a previous report is not proof that current source still passes.
3. Map every roadmap criterion to `passed with evidence`, `incomplete`, or
   `blocked on a named owner decision`. Tie each passed item to a reproducible
   command, test, or device result. Keep the checklist current at every
   milestone.
4. Identify the next incomplete dependency and proceed with authorized work
   without waiting for routine engineering choices.

## Follow the roadmap in dependency order

1. **Server export:** Validate authenticated export against a uniquely named,
   disposable PostgreSQL database using synthetic approved, restricted,
   expired, and withdrawn sources. Cover missing/invalid signing configuration
   and publication-to-export withdrawal races. Verify exported bytes with the
   package verifier.
2. **Android trust and installation:** Using an injected synthetic trust
   anchor, verify package download, signature and digest checks, version/expiry
   policy, safe bounds, atomic installation, metadata persistence, and
   persistence across process/app restart. Never replace the main app with the
   validation build.
3. **Package lifecycle:** Verify updates, signed controls, withdrawal, expiry,
   anti-rollback, replay rejection, and rollback only with exact authorization.
   Exercise server-to-device flows where feasible.
4. **Grounding and inference:** Ensure retrieval uses only installed, verified,
   eligible local evidence and citations identify passages actually retrieved.
   Run actual Qwen inference with the device disconnected from the internet.
   Investigate prior crashes or process kills; unknown causes remain unresolved
   and cannot be marked passed.
5. **Values and complex requests:** Turn documented Perazim principles into a
   reviewable scenario matrix for ordinary, ambiguous, sensitive, and complex
   requests. Evaluate factuality, empathy, appropriate limits, escalation where
   project documents require it, and consistency between online and offline
   behavior. Do not invent doctrine where approved material is silent.
6. **Acceptance:** After final changes, run all relevant Android and backend
   suites. Update the roadmap/completion report with commands, actual results,
   device/network conditions, residual risks, and owner-controlled prerequisites.

## Stay focused and report progress

- Work only on changes that directly satisfy an incomplete acceptance
  criterion. Do not start tangential cleanup, broad refactors, or optional
  features while required criteria remain incomplete.
- After each milestone, update the checklist and give a concise progress
  report. During sustained work, provide an update at least every 30 minutes.
  State what passed, what failed or remains, and the next concrete action.
- On failure, inspect logs and reproduce narrowly, make bounded repair
  attempts, and continue independent roadmap work. After two unsuccessful
  bounded attempts, record the exact blocker, evidence, and needed action/input;
  do not repeat the same failed attempt or call the whole task blocked while
  independent work remains.
- Do not add or run tests unrelated to acceptance. Never claim a criterion
  passed without a result. Distinguish unit, integration/database, device,
  offline-inference, and production-acceptance evidence.

## Preserve project safeguards

- Use synthetic data and isolated test infrastructure only. Never use real
  church membership information for testing or debugging.
- Preserve existing working-tree changes, app data, accounts, and local
  configuration. Do not reset, clean, stage, commit, push, deploy, or restart
  live services unless separately authorized.
- Keep online and offline AI flags disabled. Do not provision or expose
  production signing keys, publish content, configure a production provider,
  deploy, or install/update the main app without explicit owner authorization.
- Treat production signing custody, trust distribution, corpus/licensing
  approval, provider setup, and release activation as owner decisions. Record
  these separately while completing all independent authorized synthetic
  engineering work.

## Completion gate

Call the engineering task complete only when every safely actionable roadmap
criterion passes with current reproducible evidence, the full relevant suites
pass, and the completion report agrees with the open-dependency list. Otherwise
report exact remaining criteria and named external decisions. Elapsed time, a
build, a focused test, or substantial progress does not establish completion.
