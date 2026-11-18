# Phase F2 Community integration and PostgreSQL verification — 2026-11-09

## Outcome

The primary checkout is `/home/murugu/Android/app`, branch `phase-B-community`, at HEAD `cad3758e5469534c01142bbdeadc62371fa4e7f6` before this work. PostgreSQL Community verification is **BLOCKED — DEPENDENCY REQUIRED**: the operating system denied both TCP and Unix-domain socket creation (`Operation not permitted`). The disposable cluster initialized but never started; no application test database was created and no migration was applied.

The production score remains **35.20%**. Source integration and focused route tests do not establish PostgreSQL persistence, cross-account operation, Android acceptance, or production acceptance.

## Repository and ownership

- The primary checkout had one existing untracked Phase F report, `docs/phase-b/PHASE_F_PRODUCTION_READINESS_2026-11-09.md`. It was preserved.
- Other worktrees were inspected read-only. `worktrees/avatar-feature` has uncommitted edits to `server/src/routes/community.js` and avatar Android files. `worktrees/antigravity-audit` has untracked audit work. The two audit checkouts were clean. None of that work was edited, executed, staged, or incorporated.
- This change edits only `server/src/routes/prayers.js`, `server/src/routes/sync.js`, `server/test/api.test.js`, and adds `server/test/prayer_guest_identity.test.js` plus this report.
- Nothing was committed, pushed, merged, deployed, or switched to another branch.

## PostgreSQL and migration evidence

Bundled PostgreSQL binaries:

| Binary | Path | Version |
|---|---|---|
| `postgres` | `server/.runtime/pgsql/usr/bin/postgres` | PostgreSQL 18.6 |
| `psql` | `server/.runtime/pgsql/usr/bin/psql` | PostgreSQL 18.6 |
| `pg_ctl` | `server/.runtime/pgsql/usr/bin/pg_ctl` | PostgreSQL 18.6 |
| `initdb` | `server/.runtime/pgsql/usr/bin/initdb` | PostgreSQL 18.6 |

The binaries require `LD_LIBRARY_PATH` to include `server/.runtime/pgsql/usr/lib64`. No configured database was used.

Environment versions observed: Node.js `v26.7.0`; npm `11.19.0`. Calling `psql --version` before setting the private library path failed to load `libpq.so.private18-5`; after setting `LD_LIBRARY_PATH`, all four PostgreSQL `--version` commands succeeded.

Before cluster creation, `npm run test:migration-0007-guard` passed (exit 0): `migrate()` refused the default plan with `PROHIBITED_MIGRATION_IN_PLAN` before calling `pool.connect()`, and the isolated-runner preflight confirmed its guard precedes temporary-directory and database/cluster creation commands.

A reviewed temporary plan contained only `0001_current_schema_baseline.sql`; it excluded `0007` and every later migration. `initdb` initialized `/tmp/perazim-phasef2-pg.Jw3086/data` (exit 0). Starting with loopback TCP failed because the OS denied binding `127.0.0.1`. Starting without TCP and using the private Unix socket at `/tmp/perazim-phasef2-pg.Jw3086/socket` failed for the same OS restriction. PostgreSQL logged `FATAL: could not create any TCP/IP sockets` and `FATAL: could not create any Unix-domain sockets`. `pg_ctl status` confirmed there was no running server. The test-owned temporary cluster and migration plan were then removed successfully. No `CREATE DATABASE`, `psql` connection, baseline application, or full migration command ran. **Migration 0007 was not applied.**

## Community capability-to-evidence map

| Capability | Android and Express/PostgreSQL source path | Evidence and classification |
|---|---|---|
| Feed and posts | `CommunityRemoteSource.fetchPosts/createPost/amenPost` calls `/api/community/posts`; Express reads/writes `community_posts` and `post_amens`. | Paths and authenticated ownership checks are present. No PostgreSQL run. **RESOLVED — OPERATIONAL VERIFICATION PENDING** |
| Comments | `fetchComments/addComment` calls `/api/community/posts/:id/comments`; Express reads/writes `post_comments`, checks post visibility, and attributes the author from the authenticated session. | Paths are present; no cross-account database run. **RESOLVED — OPERATIONAL VERIFICATION PENDING** |
| Prayer requests | Android `PrayerRemoteSource` calls `/api/prayers` and `/api/sync`; Express writes `prayers` and `sync_events`. | Guest ID remediation and socket-free route regression passed. PostgreSQL persistence, restart persistence, and Android sync were not run. **RESOLVED — OPERATIONAL VERIFICATION PENDING** |
| Groups | Android `fetchGroups/fetchGroupMembers`; Express group routes use `community_groups` and `group_memberships` with owner/member/admin checks. | Source paths and guards present; no two-account database run. **RESOLVED — OPERATIONAL VERIFICATION PENDING** |
| Messaging | Android `fetchConversations/fetchMessages/sendMessage`; Express reads/writes `messages`, limits retrieval to the authenticated sender/recipient pair, and emits recipient-scoped notifications. | Source paths and privacy filters present; no persistence or two-account run. **RESOLVED — OPERATIONAL VERIFICATION PENDING** |
| Events and RSVP | Android creates EVENT posts and calls RSVP routes; Express stores event posts in `community_posts`, responses in `event_rsvps`, and scopes RSVP identity to the session. | Source paths and checks present; no PostgreSQL execution. **RESOLVED — OPERATIONAL VERIFICATION PENDING** |
| Notifications | Android fetches/marks notifications read; Express stores `in_app_notifications` and filters updates and reads by `req.user.id`. | Source paths and user scoping present; delivery/restart behavior unverified. **RESOLVED — OPERATIONAL VERIFICATION PENDING** |
| Classes | Android `CommunityUiBinder` calls `/api/community/classes`; Express stores `community_classes` and restricts listing/creation/cancellation to authorized group members, owners, or administrators. | Source paths and permission checks present; no PostgreSQL execution. **RESOLVED — OPERATIONAL VERIFICATION PENDING** |

The Android HTTP/API test suite could not listen on loopback, so none of the above was operationally verified through HTTP. The broader `community_api.test.js` suite was not invoked because it requires an available disposable PostgreSQL service and server listener. No two-account PostgreSQL persistence claim is made. Local drafts/caches do not count as shared-server evidence.

| Capability | Implemented | Android → Express → PostgreSQL source integration | Automated test evidence in this run | Cross-account PostgreSQL operationally verified | Production accepted |
|---|---|---|---|---|---|
| Feed/posts | Yes | Present in source; not exercised | No F2 test completed | No | No |
| Comments | Yes | Present in source; not exercised | No F2 test completed | No | No |
| Prayer requests | Yes | Present in source; not exercised against PostgreSQL | Targeted synthetic route regression passed; PostgreSQL assertions did not run | No | No |
| Groups | Yes | Present in source; not exercised | No F2 test completed | No | No |
| Messaging | Yes | Present in source; not exercised | No F2 test completed | No | No |
| Events/RSVP | Yes | Present in source; not exercised | No F2 test completed | No | No |
| Notifications | Yes | Present in source; not exercised | No F2 test completed | No | No |
| Classes | Yes | Present in source; not exercised | No F2 test completed | No | No |

## Prayer guest identity correction

Before the change, direct guest prayer creation trusted `req.body.authorId`, and offline sync trusted `payload.authorId`; sync audit attribution also trusted `req.body.userId`. A guest could therefore attach a prayer to an existing member ID and pass an ownership check keyed to that ID.

The server now assigns `author_id = 'guest'` to unauthenticated direct and synced prayer creation. Authenticated submissions continue to use `req.user.id`. Unauthenticated sync audit attribution also uses the server-owned `guest` identity. Guest submission remains publicly available. Sync replay of a member prayer with a forged guest ID is rejected as an identity conflict. Testimony still requires authentication and is checked against the stored author ID.

`server/test/prayer_guest_identity.test.js` passed (exit 0) using a synthetic in-memory query adapter and direct route-handler invocation. It asserts:

- Authenticated author ownership comes from session context even if the request includes another ID.
- A public guest can submit, but direct guest `authorId` is ignored.
- A missing or invalid bearer identity remains guest and is assigned `guest` server-side.
- Guest sync ignores supplied `authorId` and `userId` for ownership and audit attribution.
- A guest cannot replay a member prayer by supplying that member's ID.
- Another member cannot add testimony to the prayer; the stored record remains unchanged, while the owner can.

This is a route-level regression test with a fake query adapter, not a PostgreSQL or HTTP integration test. The same guest assertions were added to `server/test/api.test.js`; that suite could not run because Node could not bind `127.0.0.1:3001` (`listen EPERM`). The test emitted its expected warning that an ephemeral `JWT_SECRET` was used.

The test and code changes do not establish guest identity persistence in PostgreSQL. Guest display names remain presentation values, not verified identities. Existing guest Amen attribution behavior was not changed as part of this author-ownership correction.

## Commands and results

| Command/action | Exit | Result |
|---|---:|---|
| `npm run test:migration-0007-guard` | 0 | 0007 refusal and runner-order guards passed before any database/cluster creation. |
| `postgres --version`, `psql --version`, `pg_ctl --version`, `initdb --version` with bundled `LD_LIBRARY_PATH` | 0 | All reported PostgreSQL 18.6. |
| `initdb -D /tmp/perazim-phasef2-pg.Jw3086/data --username=postgres --auth-local=trust --auth-host=trust --no-instructions` | 0 | Isolated synthetic-only cluster initialized with trust auth scoped to the task-owned temp cluster; startup options restricted listening to loopback before startup failed. |
| `pg_ctl -D <temp>/data -l <temp>/postgres.log -o "-h 127.0.0.1 -p 55439 -k <temp>/socket -c listen_addresses=127.0.0.1 -c unix_socket_directories=<temp>/socket" -w start` | 1 | OS denied TCP socket bind (`EPERM`). |
| `pg_ctl -D <temp>/data -l <temp>/unix.log -o "-c listen_addresses='' -c unix_socket_directories=<temp>/socket" -w start` | 1 | OS denied Unix-domain socket bind (`EPERM`). |
| `pg_ctl ... status` and removal of the task-owned `/tmp` cluster | 0 | No server was running; temporary cluster and filtered migration directory were removed. |
| `node test/prayer_guest_identity.test.js` | 0 | Focused route regression assertions passed with synthetic in-memory query adapter. |
| `npm run test:unit` | 1 | Server test could not bind `127.0.0.1:3001` (`listen EPERM`); the HTTP suite stopped before its assertions. |
| `git diff --check` | 0 | No whitespace errors. |

Android source/device tests, Gradle build, ADB enumeration, and backend restart were not run in this server-only patch. The preceding Phase F report records Gradle cache/socket and ADB loopback restrictions. No APK was produced or relabeled.

## Score and next actions

The prior Community weight is 12 points at maturity 0.45 (5.40 points); the overall score remains **35.20%**. This task adds source-level remediation and synthetic route-handler evidence, but no verified PostgreSQL persistence, two-account operation, Android acceptance, or production acceptance. No score points are added. The existing weighting denominator and maturity definitions still need an approved product acceptance checklist before the percentage can be treated as objective.

Next, provide a controlled environment where the test process may create a loopback or Unix-domain socket, then rerun the approved temporary plan containing only the baseline `0001` migration and run feature-specific PostgreSQL tests. Preserve the explicit no-`0007` guard and do not run later migrations until separately approved. After successful database evidence, obtain the approved Android test device/build access and repeat the cross-account checks across all eight capabilities. Recompute production credit only against an approved acceptance checklist.
