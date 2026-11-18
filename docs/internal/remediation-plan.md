# Remediation and launch plan: summary and status

This is a summary of the owner's "Portfolio-Grade Remediation & Launch Plan" (2026-11-17, updated 2026-11-18). The owner's full text governs where this summary is shorter. Status is as of the last commit on `fix/perazim-remediation-start`.

## Status by phase (updated 2026-11-18)

| Phase | Status | Done | Still open |
|---|---|---|---|
| 0 Baseline | Partly | Contributor rules and CODEOWNERS; AGENTS.md updated | `main` and `phase-B-community` fast-forward and archive tags: push denied by the permission check, awaiting owner approval; branch protection (owner) |
| 1 Public repo | Mostly | Personal paths and serial removed; church contact details in one class (`ChurchConfig.java`); NOTICE; README corrected; docs index; editorconfig | Full README rewrite; SECURITY.md reporting section; LFS for binaries; docs reorganisation into the target tree; Bible text changes deferred by owner |
| 2 Android | Partly | Build guard; in-app account deletion; 18+ confirmation; Room fallback already removed on this line | Toolchain upgrade (targetSdk 35 needs the platform installed); layering; Material theme; Hilt; WorkManager wiring |
| 3 Backend | Partly | Boot runs the migration runner; migration 0007 applied; Docker context fixed; CORS allow-list, headers, body limits, PIN 6+, bcrypt 12, random IDs | Layering split, zod validation, config validation, persistent rate-limit store, OpenAPI spec |
| 4 CI | Written | Android, backend, CodeQL, release, Dependabot workflows, pinned actions | Not yet run on GitHub; relay workflow waits for relay tests |
| 5 Deployment | Not started | Nothing yet | Needs a server, domain and accounts (owner) |
| 6 Launch gates | None ticked | Several inputs drafted | Owner decisions and approvals |
| S Security | Partly | Report with an unverified-items list; findings S-02, S-03, S-04, S-07, S-08 fixed | Remaining audit items; findings S-01 and S-06 need device and product decisions |
| C Compliance | Partly | Drafts (not approved); erasure carried out on the server; deletion and 18+ in the app; inventories; CI placeholder check | Consent screens in the app; report and block; Play data safety form |

## Owner decisions (agents do not decide these)

- **D1:** Migration 0007. Plan recommends keeping it and applying it.
- **D2:** Git identity. Resolved; commits use the owner's GitHub noreply identity.
- **D3:** Push and merge of `phase-B-community` into `main`.
- **D4:** Bible texts. Plan recommends removing NIV, ESV and NKJV; confirm Swahili Union Version rights.
- **Church name:** "Perazim Mission Church" or "PMCI".
- **Minimum age** for accounts (plan recommends 13+ with parental consent for 13–17).
- **Legal approval** of every published legal document.

## Open toolchain items (plan 2.1)
- `compileSdk` is 34 and the Android Gradle plugin is 8.2.2. Both are below the Play minimum. Upgrade to targetSdk 35 (or 36 if required) and AGP 8.7+ is still open.
