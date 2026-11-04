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
