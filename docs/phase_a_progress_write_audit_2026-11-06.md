# Phase A progress-write audit — 6 November 2026

Status: audit and local fixes only. Phase A remains pending public HTTPS deployment
and the owner's review. No multi-user acceptance suite was run during this audit.

## Scope and method

Searched the repository's source, scripts, tests, Gradle/configuration, relay,
SQL schema/migrations, and JSON seed assets for `streak`, `spiritualXp`, `xp`,
`grace_points`, `gracePoints`, setters/persistence methods, and
test/verification/debug/seed/demo helpers; followed callers of every progress
writer and test hook. Reviewed Android exported intent handling, startup,
repository/mapper/session persistence, seeder verification entry points, and all
five server test entry points. Generated build/dependency trees, Git internals,
private runtime/configuration, and archives were excluded as non-source.
The bundled SQLite asset was checked read-only: its `users` table has **0 rows**.
No live member data was edited or cleared for verification.

## Findings and changes

| Finding | Source | Action |
| --- | --- | --- |
| **Missed zero-to-2 defect:** mapping a stored grace balance of 0 still manufactured 2 in both initialized-context and no-context paths. Earlier statements that every path preserved 0 were incomplete. | `src/main/java/org/perazimchurch/app/data/mapper/UserMapper.java:89`, `:106` | Both paths now use the stored balance exactly. Added zero and positive-balance mapper regression tests. Existing legitimate balances are preserved. |
| Unused public `UserMapper.updateGamificationStats(userId, streak, xp, grace, frozen)` could overwrite all three metrics for any supplied member ID. Whole-source caller search found no callers. It was unnecessary runtime mutation surface, regardless of its original purpose. | Former method at `UserMapper.java:166` (removed) | Deleted the helper. Normal repository/entity mapping remains. |
| Public preferences injection and arbitrary-date completion hooks were packaged in the production store. No app caller invoked injection; only JVM tests did. | `src/main/java/org/perazimchurch/app/data/local/preference/GamificationStore.java:39`, `:275` | Hooks are now package-private. Public test access lives only under `src/test/.../GamificationTestAccess.java`, which is not packaged in the app. Injected test preferences apply only to null-context calls; a real Android Context always selects its own actual preferences. Normal routine completion supplies today's Nairobi date. |
| Core API test entry point could inherit a real `DATABASE_URL`. | `server/test/api.test.js:1` | Explicitly clears the inherited URL and selects test mode/transient memory before importing the app. It cannot use the PC member database. |
| PostgreSQL/restart helpers allowed `PERAZIM_TEST_SAFE=true` to bypass the non-test database check, and schema initialization could occur before the destructive-operation check. | `server/test/postgres.test.js:1`, `server/test/api_process_restart.test.js:16` | Added preflight before application imports or schema initialization. Database names must end in `_test`; production mode is refused and override no longer works. Active-database checks remain. |
| Existing community test uses fixture mutations, including PIN/admin changes. This is test isolation work, not Phase B feature work. | `server/test/community_api.test.js:6` | Same disposable-database preflight before importing/starting the app. The suite was **not run**. |
| Legacy multi-client helper sent mutation requests directly to the running localhost server without proving its database was disposable. | `server/test/api_multi_client.test.js:21` | Quarantined with an unconditional safety abort before imports/network activity. It must be replaced by a reviewed dedicated acceptance harness after public HTTPS deployment; its old localhost results are not acceptance evidence. |

The new pure guard is `server/test/test_database_safety.js:2`; its companion
`test_database_safety.test.js` checks valid disposable targets, rejects the PC/live
database, rejects production mode and malformed URLs, and proves the override
cannot bypass the guard. It opens no sockets and performs no database writes.

## Other progress writes reviewed and retained

These are feature/persistence paths, not verification helpers that credit a real
member simply because a test or startup runs:

- `MainActivity.java:425` persists the current HUD values; routine, prayer,
  share, riddle/joke, and grace-redemption handlers elsewhere in that activity
  award or spend progress following UI actions. No exported intent directly
  sets arbitrary streak/XP/grace values. `scripts/capture_full_app.py` opens
  screens/dialogs and playback; it does not invoke progress setters, complete a
  routine, answer a riddle, or submit a prayer. Its obsolete campus screenshot
  step is not proof of current UI behavior.
- `presentation/viewmodel/HomeViewModel.java:152` awards reflection XP/streak;
  `presentation/ui/HomeUiBinder.java:577` records daily completion;
  `ProfileViewModel.java:124` changes freeze state. These are real app actions,
  not audit fixtures. This audit does not assert their complete reward-design
  or anti-cheat acceptance.
- `data/repository/RoomUserRepository.java:114` and `data/local/dao/UserDao.java:38`
  persist requested feature updates. `UserMapper.toEntity` persists actual
  domain values rather than injecting sample amounts.
- `data/local/session/SessionManager.java:453` transfers earned guest XP/streak
  to the single claimed destination with `Math.max`; `:504` restores the
  authenticated server's actual grace balance. No allowance is added there.
- `server/src/routes/sync.js:147` accepts authenticated `USER_PROGRESS` for
  `req.user.id`, updating that member's XP/streak. It is a real synchronization
  endpoint, not test-only code; it does not update grace points. Existing
  `server/src/routes/community.js:882` has a user-action XP reward; this audit
  neither implements nor accepts Phase B functionality.
- `ContentDataSeeder` populates content, not member progress. Its repository
  checks read content. `BibleDataSeeder.verifyUsingRoomBibleRepository` can
  write a sample bookmark/note, **not** streak/XP/grace; its startup invocation
  had already been removed. There is no automatic gamification verifier call
  from app/database startup.
- JVM gamification tests write only their fake `SharedPreferences` and synthetic
  IDs. The seeded Bible SQLite asset has no member rows. The Bible database
  compiler defines a users table but inserts no users or progress fixtures.

## Starting grace points

There is no approved 2-point starting allowance. New accounts start at **0**:

- Server registration: `server/src/routes/auth.js:73` (client-granted amounts
  are ignored).
- Server schema/new inserts: `server/schema.sql:32`; existing database default
  migration at `:205` changes the default only, not existing balances.
- Android new entity: `data/local/entity/UserEntity.java:64`.
- Android guest, MainActivity, active Profile and AI guide defaults: 0.
- Mapper fallback: now fixed at `UserMapper.java:89` and `:106` as above.

No mass reset was performed. Historical balances cannot safely be classified as
earned versus previously manufactured from source alone.

## Verification and remaining limits

- Pure database-safety tests passed; changed JavaScript passed syntax checks.
- Android worker reports final `testDebugUnitTest assembleDebug --offline`
  succeeded with **49 tests**, including the two new mapper regressions, and
  the latest APK was installed in place with matching SHA-256.
- `git diff --check` passed for these changes.
- No API, PostgreSQL, restart, community, or multi-client integration/acceptance
  suite was executed during this audit. Public HTTPS/mobile-data acceptance is
  still blocked on deployment; local unit results do not replace that evidence.
