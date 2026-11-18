# Security and privacy items to address later

Parked on 2026-11-18 so the pilot can focus on working features. Address these before any public release or app-store review.

- Rotate the pilot database password, JWT secret and admin key. All three appeared in session transcripts. Rotating the JWT secret signs out all members.
- Confirm whether the pilot's 12 accounts are real members, and apply the consent and privacy rules before wider use.
- Finish the security audit (`docs/security/audit-2026-11.md`) and fix its high and critical findings.
- Move the pilot off Tailscale Funnel-style public exposure; keep the private tailnet route only.
- Set up the own rclone client ID before the shared one is retired in 2026.
- Decide the pilot database's long-term name and settings for production (done for the pilot: `perazim_pilot`).
- Finish the privacy policy and legal review (Phase C and Phase K).
- Restore an off-site backup account of your own and test a restore.
- Fix the 0007 data check and document the outcome.
