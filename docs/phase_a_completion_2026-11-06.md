
## Server and connected services verification

- Core API contract suite (`npm run test:unit`): all 19 test groups passed after changes on 2026-11-06. Covers registration, login, revocation, prayer sync, anonymity, replay handling, pagination, startup safeguards, and zero grace-point preservation. The registration test submits 999 points and verifies the server grants 0.
- New accounts now start with 0 grace points. Login/profile responses preserve stored balances. Schema initialization changes the default for existing installations without updating existing balances. PostgreSQL regression assertions for the client-granted-points attempt and persisted zero also passed.
- Community administrator access now depends on the ADMIN role. Removed phone-based privilege bypasses, automatic promotion, and insertion of a hardcoded administrator. Fixtures explicitly provision their administrator. Seed data requires an already provisioned administrator and PostgreSQL; memory-only core tests no longer log community seed failures.
- JavaScript syntax checks passed for changed auth/community routes and PostgreSQL/community tests. Server diff whitespace check passed.
- Full `npm test` now exits 0 against workspace PostgreSQL: 19 core API groups, real PostgreSQL contract checks, and all 11 community groups passed. `npm run test:restart` also exits 0 and verifies database persistence across real server process restart. Fixed community test teardown to close its connection pool, allowing a clean test exit.
- The configured AI relay returned HTTP 401 with a JSON unauthorized response to an unauthenticated POST. This confirms reachability and an authentication boundary; authenticated generation remains unverified.
- All eight configured public sheet feeds returned HTTP 200, nonempty CSV responses: announcements, events, hymns, riddles, jokes, sermons, verses, and config. This verifies feed availability, not every in-app display or sync flow.

## Deployment readiness and remaining dependency

No hosting target or public hostname has been supplied or verified, and no deployment was performed. The Compose configuration now requires operator-supplied database/JWT secrets, enables production mode, keeps PostgreSQL private, binds the API to host loopback, persists uploads, and includes health checks. Docker is unavailable here, so Compose runtime validation remains outstanding.

Follow [server deployment prerequisites](../server/README.md#production-prerequisites): choose the host, supply secrets without committing them, configure an HTTPS reverse proxy, provision an administrator through an operator-controlled database action, back up database/uploads volumes, set the Android release API URL, and run database/community/restart verification against a disposable test database before production rollout.


## Android build, device evidence, and account isolation

- Full Android unit suite and debug assembly passed: 47 tests, zero failures (reported by build worker, including the four queue-mechanics tests and zero-balance regression). Latest APK: `build/outputs/apk/debug/app-debug.apk`. The final profile-fallback update was installed in place successfully on physical Infinix X689C, Android 11, without uninstalling or clearing data. Existing member session was preserved; no logout or test registration was performed.
- Actual screenshots and UI hierarchy dumps are in [phase_a_2026-11-06](review/phase_a_2026-11-06/). `02-kjv-2sam5`, `04-suv`, `05-niv`, `06-esv`, and `07-nkjv` each show 2 Samuel 5 with distinct translation text matching its selected label. These prove those passages in the **preserved device database**, not complete translation coverage or fresh-install availability. No Bible source content was added during device validation. `03-translations` captures the selector.
- `08-profile` verifies the campus selector is absent and all four displayed balances are zero. It predates the final profile fallback fix: the preserved authenticated state displayed Guest Pilgrim incorrectly. The final APK now selects the exact session user and uses a truthful member placeholder when cached details are unavailable. That final fallback change is build/test verified, not visually rechecked.
- `09-sermons` and `12-sermons-font130` show full Play Sermon and Stop controls at font scales 0.85 and 1.30. The phone's original font scale **0.85 was restored and read back**. Playback audio, pause/stop lifecycle, background handling, and external YouTube handoff were not exercised; the controller explicitly labels its source as sample audio preview.
- `13-offline-guide` and `14-guide-before-query` show the actual guide opening with an Offline Guide (On-Device Rules) welcome and visible input/send controls, without a consent gate. Visual inspection confirms the input and send controls are visible despite misleading clipped bounds in the UI hierarchy. A submitted answer and network-disconnected guide behavior were not verified.
- Device navigation stopped when the phone switched to an unrelated carrier flow, indicating concurrent personal use. Unrelated captures were removed. The final APK was subsequently installed in the background without launching it; no further foreground testing occurred.
- Session/queue hardening clears stale account cache, snapshots account ID and token atomically, adds owner metadata to queued payloads, uploads only the matching authenticated owner's operations, and scopes sync checkpoints by user. In-flight requests retain their original token. Unowned legacy drafts remain retained for manual recovery rather than being silently assigned to the next account. Guest migration durably claims one destination before copying, serializes migrations, transfers owned pending/failed/syncing drafts in a Room transaction, and clears copied XP/streak only after completion. Concurrent-account and process-kill scenarios were reviewed in code; they were not accepted as new live multi-client tests.
- The phone currently has no custom server URL preference, so the app uses `http://127.0.0.1:3000`. A runtime setter exists with no in-app caller/settings screen. Its USB tether route is `10.249.159.0/24`; this does not establish a stable Wi-Fi pilot endpoint. No endpoint or account was switched against a new database during validation.


## Always-on PC backend (local development)

A reversible PostgreSQL 18.6 runtime was extracted from signature-verified Fedora RPMs into ignored `server/.runtime/`; no system PostgreSQL package was installed. Its persistent data and private backend configuration live there with restricted permissions. The database listens only on 127.0.0.1:5433. The disposable `perazim_test` database used by automated tests is separate from the new `perazim_local` app database.

From the repository directory:

```bash
server/scripts/local-backend.sh start
server/scripts/local-backend.sh status
server/scripts/local-backend.sh stop
```

The start command preserves the existing database and private generated JWT/database credentials. The detached process can continue after the terminal command ends; these commands do not install a boot-time service. Restart it after PC reboot. Back up `server/.runtime/data` while stopped before changing or moving the runtime. Do not delete `.runtime` to restart: that deletes the local database and credentials.

The API uses port 3000. Loopback health was verified at `http://127.0.0.1:3000/api/health`. The PC's observed physical USB-network address was `10.249.159.129`; the prospective phone URL is `http://10.249.159.129:3000`, subject to same-network routing, host firewall, and address changes. This is local HTTP, not public HTTPS. No router exposure or firewall change was made. The phone's existing account/session endpoint was not switched: the new local database has independent accounts and existing credentials/tokens do not automatically transfer. A future phone connection must explicitly select this development endpoint and authenticate against this database.

Final PC-only network check: the API remained running as a detached process after the launch command exited. At the current DHCP address `http://10.249.159.129:3000`, health returned HTTP 200 and an unauthenticated profile request returned HTTP 401. This checks the host's network binding and authentication; physical-phone network acceptance remains separate.

- After local-PC backend startup, established `adb reverse tcp:3000 tcp:3000` (confirmed in ADB reverse list), connecting the app's existing localhost endpoint to PC port 3000 without changing member preferences. A bounded device-shell health probe returned no response, so HTTP success from the phone is **not verified**. PC/LAN health is documented separately by the server worker. The USB tunnel is temporary and depends on the connected, authorized phone and ADB; it does not establish an independent network pilot. Existing tokens may require a fresh sign-in against the new backend database. No account was changed to test this.
