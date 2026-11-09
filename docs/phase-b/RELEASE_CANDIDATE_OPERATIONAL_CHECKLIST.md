# Phase B controlled acceptance and operations checklist

This checklist separates local software evidence from operational actions. No step below has been run against a live service or real member records.

## Required before controlled APK acceptance

- [ ] Keep the server's active privacy notice fail-closed until PMCI completes review and a separately authorised publisher publishes an approved version. The current configured notice remains draft version 0.1; the corrected version 0.2 is an inactive review draft.
- [x] Inspect the replacement internal QA APK for client relay credentials and spreadsheet endpoints. On 2026-11-09 the existing release APK passed the gate at SHA-256 `23c2b3255a8e5ab0a6d5591d81c70f4f27cad2b5c32fa3a808800b15d40f5769` (branch `phase-B-community`, HEAD `db1dce3b8d01aa08a00883c6abca16afcb601aad`, dirty working tree). Treat it as INTERNAL QA only; both AI paths remain unavailable.
- [ ] Confirm the online AI relay's server-side account eligibility checks, authoritative quota, retention behaviour, and Gemini data handling from its deployed source and configuration. The repository contains a legacy Worker source now locally revised for signed Express grants, but its deployed source/configuration is unverified and all runtime gates remain off.
- [ ] Attach a dedicated validation device or emulator. Install only the `.validation` package, connect it only to a disposable synthetic backend, and exercise registration, acknowledgement, expiry, membership review, avatar selection, directory loading, AI failure/offline fallback, and logout. Do not install the main package or connect validation to a live member service.
- [ ] Review final policy and notice content with Kenyan counsel and PMCI's authorised institutional reviewers. Legal citations, lawful bases, controller identity, contacts, providers, retention, children, and publication status are not determined by software tests.

## Designated administrator provisioning

- The existing provisioning program is synthetic-only and will create the pilot administrator solely in a uniquely named local test database. It generates a TOTP enrolment secret, grants the defined membership and administrator review capabilities, and writes an audit event; it deliberately does not grant `privacy.document.publish`.
- The Android workflow uses the server-selected identity, PIN plus TOTP step-up, short-lived privileged sessions, server-side capabilities, revocation, and audit history. Synthetic end-to-end checks pass.
- No real account was selected, inspected, or activated in this task. The controlled environment explicitly prohibits activating a real administrator or using a production database. An operational owner must map the designated project owner to an existing account through the separately controlled provisioning environment and securely deliver initial PIN/TOTP enrollment material. Do not create a universal administrator or use ordinary registration to grant authority.
- Recovery for the synthetic pilot rotates the TOTP secret, changes the PIN, increments the security version, applies an authentication cutoff, revokes privileged sessions, and writes an audit event. Production recovery has not been operationally exercised.

## Infrastructure and data operations

- [ ] Select and document the real PostgreSQL host, TLS, least-privilege database roles, encrypted storage, backup provider, retention/expiry cycle, restore test, monitoring, and incident contact.
- [ ] Keep member-register import disabled until the church-approved source, provenance, access rules, and authorised operator are established. The automated tests used synthetic records only.
- [ ] Verify public HTTPS and server-side rate limiting / AI eligibility on the intended deployment before any production connection.
- [ ] Obtain and approve the `ai.online.use` permission contract, authoritative atomic quota design, pastoral-data safety control, Worker deployment/configuration, provider model/terms and secret delivery before online AI activation. This identifier is proposed only; no catalogue permission or grant has been created.
- [ ] Obtain separate approval for production migration, member-data import, deployment, public exposure, and privacy-document publication. None is part of local Release Candidate preparation.
