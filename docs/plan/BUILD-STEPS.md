# Build steps: how the Perazim components are built and checked

Status: working record, 2026-11-27. Commands are taken from the repository files named beside each step. A step marked **[R]** comes from a handover or thread note and was not re-run for this document.

Golden rule: **app first.** A fix is built into the app and seen working before it is committed or pushed. Every phone build is stress tested on the PC before Levi installs it.

## 1. Android app

| Step | Command or action | Source |
|---|---|---|
| Unit tests and debug build | `./gradlew testDebugUnitTest assembleDebug --no-daemon` | `.github/workflows/android.yml` |
| Instrumented migration tests | Emulator job runs `PerazimDatabaseMigrationTest` and `DatabaseEncryptionMigrationTest` | `.github/workflows/android.yml` |
| Validation build (phone) | Gradle `validation` build type. Output versionCode overridden to 36 on the integration branch. | `build.gradle` on the integration branch, lines 79–107 |
| Offline AI flag | `OFFLINE_QWEN_ENABLED` is true in debug and validation, false in release. `ONLINE_AI_ENABLED` is false everywhere. | `build.gradle` |
| Bible text and reference assets | `scripts/bible/build_bible_text.py` builds the bundled text; `scripts/compile_bible_database.py` builds the database | `src/main/assets/bible/text/SOURCES.txt`, `scripts/` |
| Games content | Game scripts live on the integration branch under `scripts/games/` | Integration branch |
| Stress test on the PC | Run the app on the PC emulator and walk the screens before any install. **[R]** The laptop cannot run an emulator and its SDK images were removed on 2026-11-27. Use a CI emulator or another PC. | Build register rule |
| Save APK | Copy to `~/perazim-phone-backups/perazim-validation-vNN.apk` with a checksum | `codex-handover/co-development-protocol.md` section 5 |
| Install | Levi runs `adb install -r <path>` himself and checks the screens | Protocol section 5 |
| Record | Add a row to `codex-handover/build-register.md`: versionCode, base commit, changes, tests, phone check | Build register |

Rules: versionCode must be higher than the phone's current build (38 or later next). The build includes the latest integration HEAD from both agents. Never run instrumented tests on Levi's phone.

## 2. Server (Node, Express, PostgreSQL)

| Step | Command or action | Source |
|---|---|---|
| Install | `cd server && npm ci` | `server/package.json` |
| Full test run | `npm test` (API, Postgres, migrations, logging privacy scan, community, prayer visibility) | `server/package.json` scripts |
| Database migrations | `npm run db:migrate` runs `scripts/db-migrate.js` against `DATABASE_URL`. Migrations are numbered 0001 to 0023 on the integration branch. | `server/scripts/db-migrate.js`, `server/migrations/` |
| Local backend | `server/scripts/local-backend.sh start` starts a local PostgreSQL on `127.0.0.1:5433`, creates the app role and database, and writes `backend.env` with mode 0600. Secrets are never printed. | `server/scripts/local-backend.sh` |
| Container | `server/Dockerfile` (Node 24 Alpine). Compose stacks in `server/docker-compose.yml` (local) and `server/deploy/compose.yml` (VPS with Caddy). | `server/Dockerfile`, `server/deploy/` |
| Health check | `GET /api/health` returns 200 when the API is up | `server/src/index.js` |
| Operations | Backup and health scripts in `server/scripts/ops/` | `server/scripts/ops/` |

Rules: never point a test at the live database. Use uniquely named disposable databases. Use synthetic accounts only. Secrets stay out of git and out of chat.

## 3. Relay (online AI, Cloudflare Worker)

| Step | Command or action | Source |
|---|---|---|
| Config | `relay/wrangler.toml` | `relay/` |
| Tests | `relay/test/conversation.test.mjs` | `relay/test/` |
| Deploy | **[R]** Not deployed. Off in every build until gate L7 and provider terms are approved. | Plan section 7.2 |

## 4. Continuous integration

| Workflow | What it runs |
|---|---|
| `android.yml` | Unit tests and debug build; emulator migration tests; background sync WorkManager test on an API 34 emulator |
| `backend.yml` | Each server test file against a fresh Postgres database |
| `codeql.yml` | CodeQL analysis |
| `legal-docs.yml` | Placeholder and legal document checks |
| `release.yml` | Release build; needs the `PERAZIM_RELEASE_API_BASE_URL` secret (an owner action) |
| `plan-build-log.yml` | Appends new commits on the plan branch to `docs/plan/BUILD-LOG.md` |

## 5. Public access (not yet live)

Preconditions before any tunnel, from `readiness/cloudflare-tunnel-and-privacy-v0.2-readiness-2026-11-27.md` **[R]**:

1. Secrets rotated (names only in docs).
2. Local checks pass: health 200, public community posts 200, groups 401 without a token, refresh round-trip works.
3. The tunnel targets the API only, never the database.
4. Admin routes confirmed to need auth from the code.
5. The YouTube key stays unset until Levi approves it going through the tunnel.

## 6. Working steps for this plan (2026-11-27)

1. Read the repository state on `claude/project-thread-eg17po`, clean, at `51a7952`.
2. Read the integration branch `codex/community-integration` at `19c2b17` without changing it.
3. Read the handover, readiness and design-rule files in `/mnt/project-files`.
4. Listed the open PRs (80) and recorded their bases and draft state.
5. Wrote the master plan with each claim labelled.
6. Published the plan as a private page, and saved a markdown copy.
7. Added the build-log script, the workflow and the initial log, and tested the script on a sample range.
8. Committed the plan and its build steps to `claude/project-thread-eg17po`, pushed, and opened a draft PR.
