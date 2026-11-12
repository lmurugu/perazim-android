# Perazim Mission Church Server (API & Database)

A lightweight, robust backend for the Perazim Mission Church Android app.

## Phase A public deployment is pending

No VPS/public HTTPS endpoint has been deployed. See the
[prepared deployment stack and access/budget checklist](deploy/README.md).
Running the development Compose file alone does not establish public HTTPS or
mobile-data acceptance. Multi-user acceptance tests are on hold until the
owner's public HTTPS/mobile-data gate is satisfied; do not start Phase B work.

## Local Run Without Docker (Development / Testing)

```bash
npm install
npm test    # Runs full 10-point automated test suite
npm start   # Starts server on port 3000
```

## API Summary
- `GET /api/health` — Server health check
- `GET /api/campuses` — List church campuses (Embu, Nairobi, Meru, Online)
- `POST /api/auth/register` — Create account with Phone Number, 4-Digit PIN, and Full Name
- `POST /api/auth/login` — Sign in with Phone Number and 4-Digit PIN
- `GET /api/auth/me` — Get logged-in user profile
- `GET /api/prayers` — List public prayers and praise reports
- `POST /api/prayers` — Submit new prayer petition
- `POST /api/prayers/:id/amen` — Add an "Amen" count to a prayer
- `POST /api/prayers/:id/testimony` — Mark prayer as answered with testimony
- `POST /api/sync` — Offline-first batch mutation sync endpoint

## Offline AI knowledge export (disabled by default)

`GET /api/ai/offline-package/current` exports only approved, published public
sources explicitly permitted for offline use. Export remains unavailable unless
all of these server-side settings are supplied:

- `PERAZIM_AI_OFFLINE_PACKAGE_EXPORT_ENABLED=true`
- `PERAZIM_AI_PACKAGE_SIGNING_KEY_ID`
- `PERAZIM_AI_PACKAGE_PRIVATE_KEY` (PEM, supplied through a protected secret
  store; never commit or package the private key)
- `PERAZIM_AI_PACKAGE_MIN_APP_VERSION`
- `PERAZIM_AI_PACKAGE_TTL_MS` (positive safe integer; the resulting expiry is
  included in the signed package)

No production signing key or trust anchor is provisioned in this checkout.
Keep export disabled until key custody and Android trust distribution are
approved. The TTL is intentionally an operator setting rather than a hardcoded
retention period. When no eligible public offline source remains, the endpoint
returns a signed `EMPTY` full snapshot instead of a 404. A client that verifies
and installs this snapshot clears stale packaged passages after withdrawal; a
disconnected client cannot receive the change until it synchronizes. Serialized
envelopes are limited to 2 MiB, matching Android's bounded streaming response
reader.

`GET /api/ai/offline-package/control/current` returns the separately signed
withdrawal/supersession control manifest. It additionally requires a positive
`PERAZIM_AI_OFFLINE_CONTROL_TTL_MS` and migration
`0016_offline_ai_control_sequence.sql`; the manifest includes only public
offline source IDs, versions, and reason categories. The Android client verifies
the signature, expiry, sequence and persisted checkpoint, then excludes
withdrawn source versions from local retrieval. Package update checks fetch the
control before the package, and a missing or invalid control blocks packaged
church passages. Neither export nor control delivery is enabled by default, and
no control TTL or migration has been configured operationally. The server does
not apply either migration operationally in this checkout.

Signed rollback authorization uses `POST /api/ai/offline-package/rollback/authorize`
and exact-target delivery via `GET /api/ai/offline-package/rollback/current`.
Issuance additionally requires `PERAZIM_AI_OFFLINE_ROLLBACK_AUTHORIZATION_ENABLED=true`,
`PERAZIM_AI_ROLLBACK_TTL_MS` (positive and no more than seven days), a fresh
privileged session, and the `knowledge.package.rollback.authorize` permission.
No user grant is created by migration `0017_ai_offline_rollback_authorizations.sql`;
each signed authorization and its audit event are stored append-only. The Android
client requests authorization only for its locally verified previous package,
then verifies and applies the exact-target signature. The rollback gate is off by
default; no production key, permission grant, TTL, or migration is provisioned.

## Production prerequisites

No deployment target or public hostname is configured in this repository. Before
running Compose, set `POSTGRES_PASSWORD`, `DATABASE_URL` (host `db`, database
`perazim_church`, user `perazim_user`, matching URL-encoded password), and a unique
`JWT_SECRET` of at least 32 characters through the host's secret configuration.
Do not commit these values. Compose requires them and starts the API in production
mode. PostgreSQL stays private; the API binds host loopback port 3000. Configure
an HTTPS reverse proxy and the Android release API URL before making it public.
Back up both database and uploads volumes. Provision administrator roles through
an operator-controlled database action: registration always creates MEMBER users;
a phone number never confers administrator access. Default groups/challenges are
seeded only after an administrator exists. New accounts start with 0 grace points;
existing balances are preserved when the schema default changes.

`npm test` requires a disposable PostgreSQL database on localhost port 5433 using
the test-only connection in the integration suites. `npm run test:unit` verifies
the core API using transient local storage; community routes require PostgreSQL.
`npm run test:restart` additionally verifies real process restart persistence.

## This PC's reversible local runtime

The prepared Fedora runtime is under ignored `.runtime/`. Run
`scripts/local-backend.sh start`, `status`, or `stop` from this directory. The
script initializes PostgreSQL once, preserves its data, creates a separate local
app database, and generates private credentials once. PostgreSQL listens only on
loopback 5433; the API listens on port 3000 for local-network development. It does
not install a boot service. The extracted Fedora runtime is specific to this PC;
copying this repository alone does not install PostgreSQL on another computer.
Do not delete `.runtime` unless intentionally removing local data and credentials.
See [the verification report](../docs/phase_a_completion_2026-11-06.md#always-on-pc-backend-local-development)
for connection and test evidence. Existing phone accounts are not migrated into
this separate local database.

## PostgreSQL migration foundation

Ordered migrations and their explicit safety rules are documented in
[`docs/POSTGRES_MIGRATIONS.md`](docs/POSTGRES_MIGRATIONS.md). API startup behavior
is unchanged; migration status/plan are separate read-only commands, while
schema changes and verified baselining require explicit CLI commands.
