# Perazim Mission Church Server (API & Database)

A lightweight, robust backend for the Perazim Mission Church Android app.

## Quick Start (Deploy on VPS with Docker)

1. Make sure Docker and Docker Compose are installed on your Linux VPS.
2. In this `server` directory, run:
   ```bash
   docker compose up -d
   ```
3. Your server is now running 24/7 on port `3000` with PostgreSQL database persistence!

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
