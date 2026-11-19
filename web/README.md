# Perazim web client

An installable web app (PWA) for members, built as plain HTML, CSS and JavaScript with no build step. It uses the Express API in `server/` as its only backend.

## What it does now

- **Home:** welcome and links.
- **Sermons:** read from the church's public feed, so they are open to everyone.
- **Community:** reads the public feed. Signed-in members can report posts and block members.
- **Account:** sign-in with phone and PIN. Account deletion is in the Perazim app (see the church's delete-account page).

## Run it

Serve this folder from any static server, for example:

```bash
cd web && python3 -m http.server 8080
```

Point the app at an API by editing `config.js`. For a same-origin deployment leave `apiBase` empty. For a separate API, add the page's origin to `ALLOWED_ORIGINS` on the server, because browsers only receive CORS headers for listed origins.

## Store wrappers (Capacitor)

The same folder is the web directory for Capacitor (`capacitor.config.json` at the repo root). The native projects are not committed yet, for two reasons:

1. `android/` would sit beside the existing Java Android project in the repository root. Which Android app ships is a decision to make first.
2. The iOS project needs Xcode on macOS.

To add the wrappers on a machine with the tools installed:

```bash
npm install
npx cap add android   # needs the Android SDK
npx cap add ios       # needs macOS and Xcode
npx cap sync
```

Before a store build:

- set `apiBase` in `config.js` to the approved public HTTPS origin;
- add `capacitor://localhost,http://localhost` to the server's `ALLOWED_ORIGINS`;
- check the privacy policy, the data safety form and the App Store privacy labels against what the app collects.

## Security notes

- Member text is written with `textContent` only, never as HTML.
- The service worker caches the app shell only. API responses are never cached.
- The session token is kept in `localStorage` for this browser. Sign out clears it.
