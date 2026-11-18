# ADR 0002: Apply migration 0007

Status: Accepted by the owner's decision on 2026-11-18 ("make the best fitting choice").
Plan reference: decision D1.

## Plain summary
Migration 0007 is a privacy fix. It turns old, unverified "member" marks that came from a spreadsheet back into ordinary "registered" accounts, and it records each change. On a new, empty database it changes nothing. We keep it and apply it, so the sequence of database updates has no gap.

## Context
- The migration runner refused any plan that included 0007. That blocked every database from being upgraded past version 6.
- Removing the number from the sequence would leave a gap, and the runner refuses gaps.
- Replacing the migration with an empty file would change its checksum and lose the protective rule it carries.

## Decision
- Apply 0007 as written. Its file is unchanged.
- The runner no longer refuses a plan that contains 0007 (`server/src/migrations.js`, `PROHIBITED_VERSION = null`).
- The guard test now checks that 0007 is part of the ordered plan and accepted.

## Consequences
- A database that has not applied 0007 will apply it on its next upgrade. On an empty database nothing changes.
- Any database that already holds real member rows that were verified from the old spreadsheet will have those marks changed to "registered". That is the intended privacy outcome. Before upgrading a live database, take a backup (plan 3.1).
- The original guard test that refused 0007 is replaced, not deleted.
