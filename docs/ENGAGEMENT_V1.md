# Engagement v1: plan and status

Status: in progress on `feature/engagement-v1`. Date: 2026-11-21.
Source assessment: the fit assessment doc (draft). Decisions: Levi, 2026-11-21 (Q1 to Q5 below). Q6 was left blank.
Approved for build by the coordinator on 2026-11-21. Technical decisions are the coordinator's.

## Decisions

- Q1: reading a chapter counts toward the routine's Read step. No extra XP.
- Q2: daily star cap of 50 (`StarCap.DEFAULT_DAILY_CAP`).
- Q3: the leaderboard is for adult accounts only, pending safeguarding review. Children can still play offline; their stars stay on the phone.
- Q4: aggregated daily counts on our own server, with no user identifier and no third party. Not built yet.
- Q5: no experiments for now.

## Principles

1. Growth, not session length, is the aim.
2. Nothing the member sees is rigged. No forced losses, no hidden odds.
3. Works offline. Remote settings are a cache with compiled-in defaults.
4. Data stays on the phone unless an adult opts into the leaderboard.
5. No ads and no third-party SDKs.

## Steps

| # | Step | Status | Evidence |
|---|------|--------|----------|
| 1 | Merge `feature/games-leaderboard` onto the games engine | Done | Merge commit `ad54f99`. Migration kept at `0022` (see note). |
| 2 | Adults-only gate for the leaderboard (server and app) | Done | `server/test/games_leaderboard.test.js` passes against a disposable PostgreSQL database. App shows the closed message when `eligible` is false. Not compiled in Gradle here. |
| 3 | Review scheduler (question choice from the player's own results) | Domain done | `ReviewScheduler.java`, 7 unit tests pass (JUnit 4 run with `javac`). Not yet wired into the games. |
| 4 | "Practise this verse" link after a lost round | Not started | |
| 5 | Routine Read step counts chapter reading (Q1) | Not started | |
| 6 | Star cap and remote game settings with offline defaults (Q2) | Cap logic done | `StarCap.java` with unit tests. Not yet wired into star uploads. Remote settings not started. |
| 7 | Aggregated measurement (Q4) | Not started | |

## Notes

- **Migration number.** The coordinator asked for `0028`. The migration runner requires a gap-free sequence from `0001`, and this base ends at `0021`. Renaming to `0028` made the server test fail at boot. The migration therefore stays `0022` until the other branches that claim 0022 to 0027 are merged. Their numbers must then be settled in one place.
- **Not verified.** Android code has not been built with Gradle in this session. The domain classes compile and pass their tests with `javac`. The server leaderboard test passes.
- **Remaining work** is steps 4, 5, 6 (wiring) and 7.
