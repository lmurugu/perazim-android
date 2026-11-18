# Remediation and launch plan: summary and status

This is a summary of the owner's "Portfolio-Grade Remediation & Launch Plan" (2026-11-17, updated 2026-11-18). The owner's full text governs where this summary is shorter. Status is as of the last commit on `fix/perazim-remediation-start`.

## Status by phase

| Phase | Scope | Status |
|---|---|---|
| 0 | Repo baseline, branches, conventions | Partial. Done: contributor rules and CODEOWNERS (0.2), AGENTS.md authorization block (0.4). Open: `main` update and branch cleanup (0.1, owner), branch protection (0.3, owner). |
| 1 | Public-repo sanitization and docs | Partial. Done: API host, personal paths, serial and personal email removed (1.1). Open: Bible licensing (1.2, owner decision D4), docs tree (1.3), README (1.4), licence check (1.5, owner), repo hygiene (1.6). |
| 2 | Android modernization | Not started. |
| 3 | Backend | Partial. Done: Docker build context (3.2). Open: migrations at boot (3.1, blocked on D1), layering, security hardening, tests. |
| 4 | CI/CD | Not started. |
| 5 | Deployment | Not started. Needs owner infrastructure. |
| 6 | Launch gates L1–L10 | None checked. |
| S | Security audit | Partial report: `docs/security/audit-2026-11.md`. Owner approval (S.2) and fixes (S.3) open. |
| C | Legal and app safety | In progress. See the plan's Phase C for the full list. |

## Owner decisions (agents do not decide these)

- **D1:** Migration 0007. Plan recommends keeping it and applying it.
- **D2:** Git identity. Resolved; commits use the owner's GitHub noreply identity.
- **D3:** Push and merge of `phase-B-community` into `main`.
- **D4:** Bible texts. Plan recommends removing NIV, ESV and NKJV; confirm Swahili Union Version rights.
- **Church name:** "Perazim Mission Church" or "PMCI".
- **Minimum age** for accounts (plan recommends 13+ with parental consent for 13–17).
- **Legal approval** of every published legal document.
