# Admin TOTP re-encryption (pilot)

`count-totp-rows.sql` is read-only. `reencrypt-totp.js` decrypts every `admin_security.totp_secret_cipher` row with the old `PHASE_B_ADMIN_KEY` and re-encrypts it with the new one, using the server's own `encryptSecret`/`decryptSecret`. It writes only that column, in one transaction, and rolls back on any failure. A dry run is the default. `--apply --confirm=<db>` needs `PRE_CHANGE_DUMP` (a dump with `.sha256`). With zero rows, it only validates the new key (bootstrap path).

Tests (scratch Postgres only): `node test-reencrypt.js`. They cover a dry run, a full apply, a wrong old key, a malformed row, a failure on the last row (full rollback), zero rows, identical keys, a malformed new key, a missing confirmation, the key-swap rollback, and the dump restore. Keys are generated per run and never printed.

Plan and order: `../pilot-rotation-plan/PLAN.md`.
