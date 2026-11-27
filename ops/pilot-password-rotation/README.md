# Database password rotation (pilot)

`rotate-db-password.js` rotates one login role's password. Order: check the current password, `ALTER ROLE` (through the admin command on stdin, never on the command line), check the new password works and the old one is refused, then replace the `DATABASE_URL` line in the env file (atomic rename, mode 600). Any failure after `ALTER ROLE` restores the old password. A backup of the env file is kept for `--rollback`.

Tests (scratch Postgres with a password-checked rule for the test role): `node test-rotate.js`. Plan, dependents and order: `../pilot-rotation-plan/PLAN.md`.

Scratch setup for the test: the test role must authenticate by password over TCP. Add `host perazim_rot_test perazim_rot_app 127.0.0.1/32 scram-sha-256` to the scratch cluster's `pg_hba.conf` and reload before running; the test does not edit `pg_hba.conf`. Without it, the "old password refused" checks fail (trust auth accepts any password). Revert the scratch cluster to its trust rules afterwards.
