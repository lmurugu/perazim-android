# Backend, community, and classroom validation

## Results

| Area | Result | Evidence |
| --- | --- | --- |
| API contract | Pass, 19 assertions | `server/test/api.test.js` |
| PostgreSQL contract | Pass | `server/test/postgres.test.js`, connected to `perazim_test` on localhost:5433 |
| Community | Pass, 11 scenarios | Feed/photo upload, event and RSVP, notifications, group membership, private chat, scoped media, class privacy, and challenge replay |
| Classroom store | Pass | `server/test/classroom_store.test.js`: ownership, access, event cursor, limits, TTL, and leave |
| Two-peer WebRTC | Pass | Original `src/main/assets/classroom.html`; two synthetic authenticated Chromium peers against the real HTTP routes and disposable PostgreSQL database |

The browser call negotiated offer, answer, and ICE. Chromium reported a 640×480 remote video element, inbound video RTP with 4,593 bytes, 10 packets, and 1 decoded frame, plus inbound audio RTP with 206 bytes and 6 packets. The page's mute/unmute, camera on/off, and Leave controls were exercised; server-side removal left one participant. A synthetic non-member received HTTP 403 when trying to join the private class.

No production code or `classroom.html` was changed for the test. The harness serves the original file and supplies the Android native bridge as a test-only Playwright binding, which forwards to the real authenticated classroom routes. Test media is Chromium's synthetic camera and microphone; no real person or room is captured. The test database is guarded by the `_test` suffix preflight.

## Reproduction

The browser check is separate from `npm test` because Playwright and Chromium are optional local tools:

```sh
cd app/server
PLAYWRIGHT_MODULE=/path/to/playwright CHROMIUM_PATH=/path/to/chromium PORT=0 node test/classroom_browser_e2e.test.js
```

Use the local disposable database URL (`perazim_test`) when it is not the default. The browser test picks ephemeral localhost ports, creates uniquely named synthetic users and a private class, and deletes those fixtures on completion.

## Limits

These are local development checks. They do not establish internet/public deployment acceptance, real-device camera interoperability, or connectivity across separate networks. The later phone UI check uses a separate validation app and synthetic accounts; it must not use a real member account.
