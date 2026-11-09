# Administrator provisioning procedure and current boundary

## Implemented capability boundary

`server/scripts/bootstrap-synthetic-admin.js` is intentionally restricted to local uniquely named Phase B test databases and synthetic pilot IDs. It cannot be used against production. It grants the existing 11 membership-review and administrator-management permissions and does not grant `privacy.document.publish`, alter ecclesiastical authority, or change an ordinary user's membership status.

The interactive synthetic pilot setup (`--bootstrap --create-account`) prompts for the name and phone, asks the operator to acknowledge the TEST-only notice, and reads the 6–12 digit PIN twice without echoing it. It does not put the PIN in command arguments, shell history, or a file. It creates only a `synthetic_pilot_*` account in the confirmed disposable database and leaves its membership status `REGISTERED`. The TOTP key is written to an owner-only temporary file outside the repository (directory `0700`, file `0600`), displayed once in that terminal for authenticator enrollment, then deleted when the operator presses Enter. The existing non-interactive recovery path still accepts a PIN only through an owner-only file. Do not place PINs, TOTP values or recovery material in chat, shell history, issue trackers or logs.

## Synthetic verification only

The historical isolated Phase B harness is currently blocked before cluster creation because its full migration plan includes prohibited migration 0007. Do not run it to exercise administrator persistence. The bootstrap script remains restricted to `NODE_ENV=test`, `PHASE_B_SYNTHETIC=true`, a loopback database host, a database name matching `perazim_phaseb_<random>_test`, and exact target confirmation. Never change those guards to reach a real database. Tests and fixtures must use IDs with the `synthetic_pilot_` prefix. Database-free enrollment-file checks remain available through `npm run test:admin-enrollment`.

The recover operation rotates the synthetic PIN and TOTP, revokes active privileged sessions, advances the security version and records an audit event. A recovery PIN must be supplied through an owner-only file, not a process argument. Keep test enrollment data synthetic and disposable.

## Real designated administrator

No real account was activated. The current script is test-only, and this checkout has no verified production database, controlled account mapping, operational secret manager or approved execution environment. The mapped account must be the already existing ordinary account selected through a separately controlled identity-mapping process; this procedure must not create an account or use membership matching to decide administrator identity. Initial authentication must use that account's existing PIN plus the separately enrolled TOTP and the existing privileged-session protections.

A production-capable operator tool and its live execution remain blocked. An attempted implementation of a runnable remote administrator provisioning/recovery script was rejected by the automatic approval review because it could grant privileged capabilities, change PIN/TOTP state and revoke sessions against a remote database. No workaround was attempted. A separately approved production runbook/tool and controlled execution environment are required before real activation. Do not point the synthetic bootstrap at a real database or copy its test-only safeguards into production as a substitute for operational review.

The approved capability set remains exactly:

- `membership.record.read`
- `membership.register.import`
- `membership.claim.review`
- `membership.verification.confirm`
- `membership.record.correct`
- `membership.verification.correct`
- `membership.verification.revoke`
- `admin.permissions.read`
- `admin.permissions.grant`
- `admin.permissions.revoke`
- `admin.audit.read`

Any future production procedure must grant only these existing capabilities, be transactionally audited, verify the target database and independently approved account mapping, require secure PIN/TOTP enrollment and recovery, and must not infer pastoral, church-office or membership authority from application-administrator status.

PostgreSQL-backed synthetic workflow coverage is presently omitted because the default migration plan contains migration 0007. Restore that coverage only through a reviewed fixture plan that does not execute 0007; the current database-free independent test mode is not a substitute for administrator persistence acceptance.
