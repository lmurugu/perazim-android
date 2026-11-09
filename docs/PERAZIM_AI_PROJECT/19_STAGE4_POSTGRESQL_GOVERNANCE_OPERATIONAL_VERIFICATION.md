# Stage 4 — PostgreSQL, Governance Hardening, and Operational Verification

**Date:** 2026-11-09

**Status:** Stage 4 code and disposable-database checks implemented; the official all-migrations runner remains blocked by its deliberate migration `0007` guard. No deployment or push occurred.

## A. Initial repository condition

- Branch: `phase-B-community`
- Starting HEAD: `836462572ebaf20196e0742511cfba47a02e9bf7` (Stage 3)
- No staged changes at start.
- Existing user work preserved and never staged: modified `server/src/routes/prayers.js`, `server/src/routes/sync.js`, `server/test/api.test.js`; untracked `server/test/prayer_guest_identity.test.js`, `docs/phase-b/PHASE_F2_COMMUNITY_VERIFICATION_2026-11-09.md`, and `docs/phase-b/PHASE_F_PRODUCTION_READINESS_2026-11-09.md`.
- Read the Stage 4 handoff, repository and parent `AGENTS.md` instructions, Stage 3 report and implementation, migrations, migration runner and test setup, AI route, Worker contract, governance services, and project memory bank. The unrelated runtime PostgreSQL data path was not read or changed.

## B. PostgreSQL environment

The normal executable path had no PostgreSQL commands, and Podman still failed while trying to change the read-only `/run/user/1000/libpod` runtime. A bundled PostgreSQL 18.6 toolchain was found at `server/.runtime/pgsql/usr/bin`; its private library directory was supplied through `LD_LIBRARY_PATH`.

A new cluster was initialized under `/tmp/perazim-stage4-pgdata`, listening only on `127.0.0.1:55439` and a private `/tmp` socket. It was not connected to an application or production database. The test suite created random `perazim_phaseb_<random>_test` databases and a temporary non-superuser role, then dropped them. The cluster was stopped and its temporary files were removed after verification.

## C. Migration verification

The integration suite applied the actual SQL for migrations `0001`–`0006` and `0008`–`0014` in order. It did not run `0007`: `server/src/migrations.js` deliberately rejects every migration plan containing that version before opening a database connection, and `npm run test:migration-0007-guard` confirms that boundary. The Stage 4 suite therefore does not claim a complete official migration-runner history or a Stage 4 migration-ledger entry.

On the disposable database, migrations `0013` and `0014` were applied together inside a transaction, deliberately failed, rolled back, and verified absent. Both were then applied successfully. The test checked expected governance and corpus tables, the partial unique indexes, JSON checks, reviewer foreign keys, allowed state constraints, and that institutional approval remains inactive. It also exercised source review, approval, publication, withdrawal, supersession, persisted state from a new connection, replay rejection, and concurrent review, approval, publication, and withdrawal attempts. Only one competing transition succeeded in each race. The test suite separately verified that the Stage 3 migration SQL can roll back as a unit; migrations are forward-only in the official runner, so no destructive rollback was invented.

`npm run test:migrations` also passed against its own uniquely named local test databases. That suite tests migration-ledger, runner, checksum, baseline, and rollback safeguards with its test migration fixtures. It does not remove the `0007` guard or establish a full production migration history for `0013`/`0014`.

## D. Source governance

PostgreSQL-backed service and real Express-route checks verified authentication, no-store headers, current permission enforcement, separation between reviewer and source submitter, the ordered review/approval/publication lifecycle, replay prevention, withdrawal, supersession links, audit persistence, and concurrency serialization. An authenticated request without active registry permissions was denied. An attempt to publish an unapproved source returned a conflict. The institutional approval route returned `403` because the permission remains inactive.

As a non-superuser role with direct `UPDATE`, `DELETE`, and `TRUNCATE` grants, the test could not edit or remove historical governance events, mutate source identity/content, re-publish a withdrawn version, delete sources or reviews, edit/delete conflict classifications or their source membership, or truncate those records and Scripture corpus events. The conflict regression also reproduced the case where one side is withdrawn: the other side remained marked conflicted and was not sent as an uncontested answer context.

## E. Structured response validation

The validator now verifies the registered source passage SHA-256 and every returned verse hash/sequence before accepting references. It no longer treats token overlap as proof that an official source supports a church-wide claim. Until an independently reviewed claim-to-source entailment rule exists, institutional claims fail closed, even when an official-class source is present.

Adversarial synthetic tests cover misleading lexical overlap, “according to Perazim policy” wording, tampered source text, corrupted verse hashes, false quotations, wrong corpus identity, out-of-scope advice, fabricated testimony, and malformed responses. Exact citations and hashes prove provenance and integrity, not theological entailment. General interpretation still needs human review; claims of official Perazim teaching remain unavailable.

## F. Security and privacy

The review found and fixed two in-scope defects: weak lexical overlap was accepted as institutional support, and a withdrawn/superseded counterpart could disappear from conflict detection. The validator now fails closed on the former; retrieval reads unresolved conflict membership and escalates on the latter. Real-route tests confirmed denial for no authentication, inactive permission, inactive institutional approval, and premature publication.

All database fixtures were synthetic. No member, prayer, pastoral, or production database was queried. No live provider was called, no new external processor or network service was introduced, and no user or source content was sent outside the local environment. PostgreSQL tests require elevated loopback access in this environment; the database remained bound to loopback only.

## G. Constitution and approval authority

The project workspace search found no church constitution, adoption minute, or verified delegation authorizing doctrinal approval. Existing AI foundation and architecture documents describe product requirements and do not prove church adoption. Institutional approval is still disabled in the database and denied by the actual route.

## H. Scripture corpus and licensing

The project’s `docs/bible-source.md` makes public-domain and redistribution assertions for bundled KJV/SUV materials, but the cited upstream archives are not in the repository and the assertions have not been independently verified as redistribution evidence. The Stage 3 source review also found no verified rights for the proposed ESV material. No translation was imported into the AI corpus; no corpus was published. Import and publication still require documented rights evidence and a separately granted permission. Translation IDs supported in code are not evidence of license authorization.

## I. Test results

Passed:

- `npm run test:governed-ai` — synthetic service, validator, online-AI contract, and Worker tests.
- `node relay/test/conversation.test.mjs` — relay authentication, provenance, prompt-boundary, crisis, and provider-mock cases.
- `npm run test:unit` — all 19 backend API contract tests using their synthetic/in-memory fixtures.
- `npm run test:migrations` — migration safety suite against its own disposable PostgreSQL databases.
- `npm run test:migration-0007-guard` — verifies the migration plan is rejected before database connection.
- `npm run test:governed-ai:pg` — actual PostgreSQL 18.6, ordered prerequisite SQL except guarded `0007`, migrations `0013`/`0014`, database-backed governance services and API routes, synthetic records only. Set `MIGRATIONS_TEST_ADMIN_URL`, `PHASE_B_TEST_EXPECTED_PORT`, and `PHASE_B_TEST_EXPECTED_DATA_DIR` for the disposable local cluster first.
- `node --check` on changed JavaScript files and `git diff --check`.

The Stage 4 PostgreSQL suite reads no credentials from committed files. It requires a local admin URL targeting the `postgres` maintenance database, the exact local port, and the disposable cluster data directory. The test itself selects and drops only a random `*_test` database and its temporary role.

## J. Preservation

All pre-existing user edits and untracked files listed in section A remain untouched and unstaged. Two further unstaged Android changes in `src/main/java/org/perazimchurch/app/ai/PerazimAiEngine.java` and `src/main/java/org/perazimchurch/app/presentation/ui/AiBreakthroughGuideDialog.java` appeared during the work; both were preserved without inspection or modification. Existing local PostgreSQL runtime data and app configuration were not inspected or altered. The only database cluster created was the new disposable one under `/tmp`. No live service was restarted.

## K. Git and commits

Only Stage 4 files were selected for the implementation commit. The pre-existing community files and untracked user documents were excluded. The commit hash is recorded in the project memory addendum only after creation. No push occurred.

## L. Completion classification

- **Implemented and locally tested:** institutional-claim fail-closed rule, source/verse integrity checks, persistent unresolved-conflict escalation, repeatable PostgreSQL integration suite.
- **Verified against actual disposable PostgreSQL:** migrations `0013`/`0014` SQL after prerequisite SQL (excluding guarded `0007`), schema constraints, rollback, persistence on a new connection, lifecycle services, concurrency, append-only controls, and API authorization gates.
- **Tested with fixtures:** migration-runner ledger safeguards, prior Stage 1–3 regression suites, non-PostgreSQL source governance/response cases.
- **Partially verified:** full official migration ordering and Stage 4 version-ledger application, because the official runner refuses plans containing `0007`.
- **Not operationally enabled:** church-source approval, official doctrinal response, real Bible corpus import/publication, live provider calls, and production migration/deployment.

## M. Remaining blockers

1. The repository’s migration runner intentionally refuses a plan containing `0007`; a complete official migration ledger through `0014` cannot be produced without a separately authorized, reviewed migration plan that preserves that guard.
2. Church constitution/adoption/delegation evidence is absent; institutional approval and claims about official church teaching remain disabled.
3. Independent redistribution-rights evidence for Scripture text is absent; corpus import and publication remain unavailable.
4. Deterministic provenance checks cannot establish theological entailment or literary/historical context; human review remains necessary.

## N. Next implementation stage

Resolve the migration-history boundary in a dedicated reviewed task, without changing `0007` or applying it to a non-disposable database. Then, through a controlled channel, obtain and verify church authority and Scripture redistribution evidence before granting source approval or corpus publication permissions. Keep generation fail-closed until those prerequisites are satisfied.
