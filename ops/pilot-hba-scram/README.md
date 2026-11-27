# Coordinated local-access cutover (SCRAM host auth) for the pilot

Goal: every TCP connection to the pilot Postgres needs a SCRAM password. The local socket keeps peer access for the postgres OS user, which is the admin recovery path (`podman exec -u postgres ... psql -p 55438`). Rotation of the role password is a separate step, run after this one.

Files:
- `hba-scram-cutover.js`: dry run by default. `--apply --confirm=<db>` needs `PRE_CHANGE_DUMP` (a fresh dump with its .sha256). It refuses if any login role has a non-SCRAM password or the API role has none, writes a backup of pg_hba.conf, writes the new file, reloads, and checks that the file has no errors and every host rule is scram-sha-256. Then it checks that the current password connects, and that a wrong or empty password is refused. Any failure restores the backup. `--rollback <backup> --confirm=<db>` restores the backup and reads it back to confirm it matches.
- `scratch-up.sh`: scratch-only Postgres 16 cluster owned by the postgres OS user, on 127.0.0.1:55556, starting from trust rules (the pilot baseline).
- `test-hba.js`: scratch tests. Runs the cutover, the refusal for md5 roles, the bad-line restore, rotation under SCRAM, and both rollbacks.

Pilot environment mapping (to confirm before running, read-only):
- `hba_file`: `SHOW hba_file` through the admin command (the path inside the container).
- ADMIN_CMD: `podman exec -i -u postgres perazim-pilot-postgres psql -X -q -v ON_ERROR_STOP=1 -p 55438 -U postgres -d postgres`
- HBA_GET_CMD: `podman exec -e HBA_FILE -u postgres perazim-pilot-postgres sh -c 'cat "$HBA_FILE"'`
- HBA_PUT_CMD: `podman exec -i -e HBA_FILE -u postgres perazim-pilot-postgres sh -c 'umask 077; cat > "$HBA_FILE.new" && mv "$HBA_FILE.new" "$HBA_FILE"'`
- `-e HBA_FILE` passes the path from the host environment into the container.

Order (after Levi or Codex approves):
1. Confirm no other process is changing the pilot DB or credentials.
2. Fresh dump, verified (`backup-pilot.sh`, then `sha256sum -c`).
3. `hba-scram-cutover.js` dry run, then `--apply --confirm=perazim_pilot` with PRE_CHANGE_DUMP.
4. Check the API still connects with its current password (the API is restarted only if needed).
5. `rotate-db-password.js` dry run, then `--apply --confirm=<role>` with the API and local-review units stopped. Rotation now proves the old password is refused.
6. Start the units. Health checks as in the PLAN.

Rollback, in reverse: `rotate-db-password.js --rollback <env backup> --confirm=<role>`, then `hba-scram-cutover.js --rollback <hba backup> --confirm=<db>`. Test T6a and T6b cover both.

Container-specific checks: the container's published port must stay on 127.0.0.1 on the host (`podman port perazim-pilot-postgres`). The new host rule covers all addresses, so connections from the podman network are covered too.
