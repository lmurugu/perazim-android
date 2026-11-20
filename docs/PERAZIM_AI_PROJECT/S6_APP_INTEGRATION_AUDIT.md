# Session 6: Android application integration audit (Perazim AI)

Status: Implemented on branch `ai/s6-integration`. Host-side verification only. No phone install, no on-device runtime test, online AI remains disabled. Synthetic data only.

Baseline: `feature/offline-qwen-phone` at `15c6964` (verified at start; matches the mandate). Changes are on `ai/s6-integration` and are not pushed to the baseline branch.

## Scope

Reviewed how the offline assistant (`PerazimAiEngine`, `OnDeviceAi`, `AiRelayClient`, `AiBreakthroughGuideDialog`) integrates with the app:

- Navigation and entry points: `MainActivity.showAiBreakthroughGuide` and `executeAction`.
- Chip and button routing into app screens.
- Claims about app state (XP, streak, Grace Points) and app operations (prayer, membership, messages).
- Room compatibility and migrations.
- Permissions and privacy-relevant logging.

Out of scope and not modified: governance and cryptography (Sessions 1–2), offline Qwen runtime internals (Session 5), Bible reader files.

## Integration defect register

| ID | Severity | Finding | Evidence (baseline `15c6964`) | Status |
|---|---|---|---|---|
| D1 | Important | The assistant invented a level table (250 XP steps, titles "Disciple of the Breach", "Covenant Seeker", "Watchman on the Walls", "Baal-Perazim Conqueror"). It also said "Progress to Level 3: 750" where the next threshold was 1000, and at level 4 said "Level 5: 0 XP needed". The app uses 500 XP per level with the title "Disciple". | `PerazimAiEngine.handleXpQuery`, `calculateLevel`, `getLevelTitle`; app rule in `ProfileUiBinder` (`liveXp / 500 + 1`) | Fixed: shared `SpiritualLevel` used by engine, Profile and HUD |
| D2 | Important | The Home HUD showed a hard-coded "Level 3 Disciple" regardless of XP. | `MainActivity` XP metric card | Fixed: computed from `SpiritualLevel.levelFor` |
| D3 | Important | False claim: the assistant said a streak freeze is lifted by "complete today's devotion to unfreeze". No code path clears the freeze. It is bought with 2 Grace Points for 48 hours (`MainActivity`, "Activate 48-Hour Streak Freeze"). | `PerazimAiEngine.handleStreakQuery`; `MainActivity` freeze button; no `streakFrozen = false` writer | Fixed wording |
| D4 | Minor | The assistant used "Grace Tokens"; the app says "Grace Points". It also said "Toggle Grace streak freeze", but the app only activates a freeze. | `handleGraceQuery`, `handleProfileQuery`, chips | Fixed wording |
| D5 | Minor | Typo "ACTIVE ALTAIRE" in streak status. | `handleStreakQuery` | Fixed |
| D6 | Important | Chip "💬 Direct Chat" fell through to the off-topic guardrail. Chip "🌟 Praise Reports" routed to the hymnal, because "praise" matched worship first. Both features exist in Fellowship. | `processQuery` routing order | Fixed: "praise report(s)" and "direct chat" route to Fellowship before worship |
| D7 | Minor | Hymn title "Holy Holy Holy" does not match the seeded hymn "Holy, Holy, Holy! Lord God Almighty". | `hymns.json` seed | Fixed wording |
| D8 | Important | App-action claim guard missed contractions ("I've submitted"), passive forms ("Your request was sent"), promises ("I'll submit this for you"), and forwarding ("I passed this on"). | `CLAIMED_APP_ACTION` (baseline) | Fixed in `AppActionClaims`, with tests |
| D9 | Important | Pre-existing: the human-authority pattern required a space before `'m`, so "I'm your pastor" was not caught. | `CLAIMED_HUMAN_AUTHORITY` (baseline) | Fixed, with test |
| D10 | Important | The online relay returned server answers to the user without any client-side claim check. | `AiRelayClient` success path | Fixed: fail-closed check for app-action, human-authority and official-status claims (provenance cannot be verified on the phone) |
| D11 | Minor | "Open Reflection" text said the Reflections section; the button lands on Home, where the routine is. | `handleReflectionsQuery`, `MainActivity.onStartReflections` | Fixed wording |

## Verified without defects

- Room: `PerazimDatabase` version 12, migration chain `MIGRATION_4_5` … `MIGRATION_11_12` with no destructive fallback, `schemas/…/12.json` present. The AI changes add no entities.
- Logging: no `Log.*` calls in `ai/` or `AiBreakthroughGuideDialog`. The local model's stderr is drained and discarded (`OnDeviceAi`).
- Local storage: AI preferences and control state use `MODE_PRIVATE` SharedPreferences. No new storage is added.
- Canned replies (crisis, pastoral, human decision, professional, out of scope, church information) are covered by a test asserting they never claim an app action.
- Chip-to-route check: every chip that routes to a feature was run through `processQuery` on a stubbed engine and reaches its intended handler.

## Recommendations (not implemented)

1. **Server-side claim guard (backend owner).** `server/src/phase-b/onlineAi.js` returns the provider answer without an app-action or official-status check. The phone check (D10) is a second line, not the gateway. Add the same patterns server-side and test them there.
2. **Remove unused `RECORD_AUDIO` permission (owner decision).** It is declared in `AndroidManifest.xml`, and no code in `src/main/java` uses `AudioRecord`, `MediaRecorder` or `SpeechRecognizer`. `CAMERA` is used for the Profile avatar. Removing a permission changes install-time consent, so this needs the owner's decision.
3. **Chip label "✍️ Submit Prayer Request".** The reply correctly navigates and says it does not submit. The label still implies submission. Consider "Open Prayer Wall".
4. **Copyright of hymn titles (Session 4).** The assistant lists "Great Is Thy Faithfulness" and the app ships its lyrics in the seed. Rights are not verified. This is referred to Session 4; no change made here.
5. **Coordination with Session 5.** `MainActivity` (tab routing, HUD) is shared navigation. The changes here are limited to one HUD label and the `onStartReflections` path is unchanged.

## Verification performed

- Baseline HEAD confirmed as `15c6964` before any change.
- New pure-Java tests `AppActionClaimsTest` (6 test methods) and `SpiritualLevelTest` (3 test methods) compiled with `javac` against JUnit 4.13.2 and run: **9 tests, all passing**.
- `PerazimAiEngine`, `AppActionClaims`, `SpiritualLevel` and `AiResponse` compiled from source against minimal stubs for Android and `org.json` types (stubs held outside the repository).
- Replay of 42 claim fixtures from the existing test files (`AuthorityBoundaryTest`, `AuthorityCoverageTest`, `SyntheticInjectionAndValidatorTest`, `ConflictAndHistoryTest`) against the new guard: no regressions. The four official-status fixtures are correctly rejected by `claimsOfficialStatus`, which is a separate check.
- `processQuery` routing and XP text run on the stubbed engine (see defect list).
- `ProfileUiBinder`, `MainActivity` and `AiRelayClient` syntax-checked: no parse errors. Their remaining compile errors are unresolved Android symbols because no Android SDK is installed here.

## Not verified

- **Gradle unit suite not run.** This container has no Android SDK (`ANDROID_HOME` unset, no `android.jar`), so `./gradlew test` and Robolectric tests could not run. The new tests are written to run under the existing Gradle setup and should be run in CI.
- No phone install and no device or runtime test (not authorised).
- Online AI is disabled in this build. The relay change is tested by reading only.
