# Church identity: screen and location inventory (2026-11-20)

**Status: documentation only. No application text, branding or code has been changed.**
Base: `origin/feature/offline-qwen-phone` at `15c6964`. Branch for this work: `ai/s3-identity`.

This inventory maps every place in the Android app where institutional identity wording appears: name, motto, slogan, vision, mission, core values, leadership and headquarters. It also records whether each location can be reached by a member. Companion documents: `CHURCH_IDENTITY_COMPARISON_2026-11-20.md`, `CHURCH_IDENTITY_DECISION_BRIEF_2026-11-20.md` and `CHURCH_IDENTITY_DOSSIER_2026-11-20.md`.

## How "live" was decided

- **LIVE**: the method is called from the app's running flow. Checked by tracing callers from `MainActivity.onCreate` / `initApp`, the presentation binders (`HomeUiBinder`, `ProfileUiBinder`, `SermonsUiBinder`, `WorshipUiBinder`) and the dialogs they open.
- **DEAD**: the method has no call site in the repository at `15c6964`. Checked with `git grep` for `methodName(`. Code can still be read in the file, but no member can reach it.
- **UNVERIFIED**: the wording is in the app, but the launch path was not traced.

Counts were taken from the code at `15c6964`. Line numbers are as read on 2026-11-20 and may drift after later commits.

## Element key

N = name · M = motto · S = slogan · V = vision · MI = mission · CV = core values · L = leadership · H = headquarters or location

## A. Launch, splash and first run (LIVE)

| # | Location | Element | Current wording | Status | Note |
|---|---|---|---|---|---|
| A1 | `AndroidManifest.xml` lines 52 and 63 (`application` and launcher `MainActivity`) | N | "Perazim" | LIVE | The home-screen and app-drawer label is **"Perazim"**, not "Perazim Mission Church". This corrects the earlier decision brief. |
| A2 | `ui/components/StartupSplash.java` line 70, `begin()` | N | "Perazim Mission Church" | LIVE | Cold start only (`MainActivity.onCreate`, `savedInstanceState == null`). |
| A3 | `ui/components/StartupSplash.java` line 80, `begin()` | M | "The Place of Great Breakthrough" | LIVE | Cold start only. |
| A4 | `presentation/ui/OnboardingDialog.java` line 109 | N | "🏛️ PERAZIM MISSION CHURCH" | LIVE | First launch only (`OnboardingDialog.showIfNeeded`, `MainActivity.initApp` line 223). |
| A5 | `OnboardingDialog.java` line 119 | M | "The Place of Great Breakthrough" (title) | LIVE | First launch only. |
| A6 | `OnboardingDialog.java` line 138 | S | "A place where everybody is somebody, and **nobody** is a nobody." | LIVE | Spelling variant (see section F). |

## B. Home (LIVE)

| # | Location | Element | Current wording | Status | Note |
|---|---|---|---|---|---|
| B1 | `presentation/ui/HomeUiBinder.java` line 270, `initUi()` | N + H | "Perazim Mission Church · Embu Headquarters" | LIVE | Home sub-line. |
| B2 | `HomeUiBinder.java` line 328, `createHeaderBanner()` | N | "PERAZIM MISSION CHURCH" | LIVE | Home banner. |
| B3 | `HomeUiBinder.java` line 329 | M | "The Place of Great Breakthrough" | LIVE | Home banner. |
| B4 | `HomeUiBinder.java` line 868, `renderAnnouncementCards()` | N | Appends "— Perazim Mission Church" to announcement dialogs | LIVE | |

## C. Profile, about and pastoral care (LIVE unless marked)

| # | Location | Element | Current wording | Status | Note |
|---|---|---|---|---|---|
| C1 | `ProfileUiBinder.java` line 486, `createOptionsListCard()` | N | Option row "About Perazim Mission Church" | LIVE | Opens the About dialog (line 488). |
| C2 | `ProfileUiBinder.java` line 792, `showPastoralCareDialog()` | N + H | Footer "Perazim Mission Church · Embu Headquarters" | LIVE | Opened from option row line 485. |
| C3 | `ProfileUiBinder.java` line 839, `showAboutDialog()` | N | Tag "ABOUT PERAZIM MISSION CHURCH" | LIVE | |
| C4 | `ProfileUiBinder.java` line 858, `showAboutDialog()` | S | "A place where everybody is somebody, and no body is a nobody." | LIVE | Third spelling variant (see section F). |
| C5 | `ProfileUiBinder.java` lines 865 and 866 (`tvContent` block) | L + H | "Leadership: Bishop Dr. David Mutweri" and "Embu Headquarters, Kenya" | LIVE | Leadership and location shown to members. Not yet confirmed by a document. |
| C6 | `ProfileUiBinder.java` line 867 | MI | **"Mission: Spreading the Gospel of Christ across Kenya & Beyond"** (Mission B) | LIVE | The only mission statement a member can see in the live app. |
| C7 | `ProfileUiBinder.java` lines 1395 and 1402, `createChurchIdentityCard()` | N, M | "PERAZIM MISSION CHURCH"; "The Place of Great Breakthrough (2 Samuel 5:20)" | DEAD | No call site. |
| C8 | `ProfileUiBinder.java` lines 1503 to 1524, `createPastoralSupportCard()` | N, H | Email subject "Perazim Church Inquiry"; secretariat email row | DEAD | No call site. The contact row itself is not in use. |

## D. Giving, worship, sermons and sharing (LIVE)

| # | Location | Element | Current wording | Status | Note |
|---|---|---|---|---|---|
| D1 | `presentation/ui/GivingDialog.java` line 131, `show()` | N | "Give to Perazim Mission Church" | LIVE | Opened from the Profile and Home giving controls. |
| D2 | `presentation/ui/WorshipUiBinder.java` line 224, `buildArtBanner()` | N | "Perazim Mission Church · Songs of Contemplation" | LIVE | |
| D3 | `WorshipUiBinder.java` line 762, `showHymnDetailSheet()` | N | Hymn share footer "— Perazim Mission Church" | LIVE | Appears when a member shares a hymn. |
| D4 | `presentation/ui/SermonsUiBinder.java` line 370, `buildLiveStreamBanner()` | L + H | "Bishop Dr. David Mutweri · Central Campus" | LIVE | Says "Central Campus". Home and Profile say "Embu Headquarters". The public website says "Embu Central Campus, Manyatta". |
| D5 | `SermonsUiBinder.java` line 829, `createSermonCard()` | N | Default preacher "Perazim Mission Church" | LIVE | |
| D6 | `SermonsUiBinder.java` line 1118 | N | Preacher set to "Perazim Mission Church" | LIVE | Inside a load callback in the live binder. |
| D7 | `data/repository/YouTubeFeedRepository.java` line 249, `shareSermonToWhatsApp()` | N | Share text "Perazim Mission Church - Breakthrough Word" | LIVE | |
| D8 | `navigation/ChurchIntentManager.java` line 34 | N + H | Map query "Perazim Mission Church Embu" | LIVE | Directions to the church. |
| D9 | `SermonsActivity.java` line 158, `onCreate()` | L | Dialog "Presiding Bishop · Perazim Mission Church" | UNVERIFIED | Activity is declared in the manifest. Its launch path was not traced. |

## E. Account and privacy (LIVE)

| # | Location | Element | Current wording | Status | Note |
|---|---|---|---|---|---|
| E1 | `presentation/ui/AuthDialog.java` line 143 | N (abbreviation) | "I acknowledge that PMCI has provided me with the Perazim App Privacy Notice…" | LIVE | A member sees the abbreviation **PMCI** in the sign-up flow. The app's other screens do not use it. |
| E2 | `AuthDialog.java` line 316, `onError()` | N (abbreviation) | "PMCI Privacy Policy · Draft v0.1" | LIVE | Shown when the policy cannot load. |
| E3 | `src/main/assets/privacy/perazim-app-privacy-notice.md` lines 3 and 14 | N (legal) | "PERAZIM MISSIONS CHURCH INTERNATIONAL"; operator "[exact registered legal name of PMCI]" | LIVE (via `PrivacyDocumentDialog`) | The legal name is a placeholder. No registered name is in the repository. |
| E4 | `src/main/assets/privacy/pmci-public-privacy-policy.md` lines 4, 11 and 15 | N (legal) | "PERAZIM MISSIONS CHURCH INTERNATIONAL"; "Perazim Missions Church International (PMCI)"; data controller "[Insert exact registered legal name of PMCI]" | LIVE (via `PrivacyDocumentDialog`) | Labelled "DRAFT FOR REVIEW". |

## F. AI wording (LIVE)

| # | Location | Element | Current wording | Status | Note |
|---|---|---|---|---|---|
| F1 | `ai/local/LocalPrompt.java` line 22 | N (AI-facing) | "…the digital ministry assistant of Perazim Missions Church International." | LIVE | Matches the owner-confirmed AI name (PMCI). |
| F2 | `ai/PerazimAiEngine.java` line 339 | N (AI-facing) | Out-of-scope reply: "…relating to Perazim Missions Church International…" | LIVE | Same name. |

## G. Bundled content (LIVE as seeded content)

| # | Location | Element | Current wording | Status | Note |
|---|---|---|---|---|---|
| G1 | `src/main/assets/seed/announcements.json` lines 5 and 41 | N | "Perazim Mission Church" in announcement bodies | LIVE | Seeded content. |
| G2 | `src/main/assets/seed/events.json` lines 8, 19, 30 and 41 | H | "Embu Headquarters" | LIVE | Event locations. |
| G3 | `src/main/assets/seed/sermons.json` line 27 | N | Sermon summary: "…mandate given to Perazim Mission Church." | LIVE | |
| G4 | `src/main/assets/seed/riddles.json` line 38 | N | "…the foundational name of Perazim Mission Church." | LIVE | |
| G5 | `src/main/assets/seed/reflections.json` line 65 | N | "…ministries and outreaches of Perazim Church." | LIVE | "Perazim Church" variant. |
| G6 | `src/main/assets/player/church_video_player.html` line 34 | N | Subtitle "Perazim Mission Church" | UNVERIFIED | Launch path not traced. |

## H. Legacy code in `MainActivity.java` (all DEAD except H8)

`MainActivity` still contains an older full UI (`buildLegacyHomeScreen`, `buildLegacySermonsScreen`, `buildLegacyFellowshipScreen`, `buildLegacyProfileScreen`). `initApp` builds the binder-based UI instead. None of the legacy builders has a call site. Dialogs and listeners attached only to those builders are therefore also unreachable. The identity wording in them is included so that nothing is missed, but a member cannot see it at `15c6964`.

| # | Lines | Method | Element | Current wording | Status |
|---|---|---|---|---|---|
| H1 | 906, 914 | `buildLegacyHomeScreen` | N, M, S | "PERAZIM MISSION CHURCH"; motto and slogan (no-body variant) | DEAD |
| H2 | 1040 to 1041 | `buildLegacyHomeScreen` | Verse-card button | "WhatsApp Story" button; its listener opens the verse card | DEAD |
| H3 | 1533, 1560, 1611, 1629, 1650 | `showWhatsAppVerseCardDialog` | N, M, L, H | "PERAZIM MISSION CHURCH — 'The Place of Great Breakthrough'"; "Bishop Dr. David Mutweri · Embu, Kenya"; share text | DEAD (only reached from H2) |
| H4 | 1565, 2958, 3096 to 3112 | `showContactDialog`, `showPrayerRequestDialog` | N, H, L | Subtitle "Perazim Mission Church Headquarters, Embu … Presiding Bishop" | DEAD |
| H5 | 1854 | `buildLegacySermonsScreen` | L | "Speaker: Bishop Dr. David Mutweri \| Central Campus" | DEAD |
| H6 | 2105 to 2153 | `buildLegacyFellowshipScreen` | S | Slogan with "no body" wording | DEAD |
| H7 | 2177 to 2233 | `buildLegacyFellowshipScreen` | N, M, **V, MI, CV** | Name; motto; **vision "To be a Center for Missions"**; **Mission A**; **core values** | DEAD. These three elements appear **only** here in code. |
| H8 | 3359, 3368 | `createSplashScreen` | N, M | "PERAZIM MISSION CHURCH"; "The Place of Great Breakthrough" | Reachable only when an intent extra `show_splash` is set. No code in this repository sets it, so it is effectively unreachable from in-app navigation. |
| H9 | 2286 | `buildLegacyFellowshipScreen` | L | "Presiding Bishop · Perazim Mission Church" | DEAD |
| H10 | 2509 to 2628 | `buildLegacyProfileScreen` | N, M, S | Member subtitle; name; motto; slogan | DEAD |
| H11 | 2706, 2811, 2827 | `buildLegacyProfileScreen`, `shareInvite` | H, N | Directions row; invite share text ("Perazim Mission Church — The Place of Great Breakthrough") | DEAD |

## I. Other repositories (public and shared content)

These are not part of the Android app. They matter because members see them, or because other teams may copy from them.

| # | Repo and location | Element | Current wording | Status | Note |
|---|---|---|---|---|---|
| I1 | `perazim-mission-church` (GitHub Pages website), `about/index.html` lines 116 to 147 | MI, H, L | "Our mission: Transforming the city of Embu with the gospel of Jesus Christ." "Manyatta, Embu" as location. "Bishop Dr. David Mutweri is the founder and presiding bishop…since 2003." | PUBLIC | **A third mission statement.** Added 2026-11-18 by commit `13d4d27`, with no approval record. |
| I2 | `perazim-mission-church`, `about/index.html` lines 157 to 159 | CV | "What we value": **Jesus is our center**; **People are our heart**; **Generosity in action** | PUBLIC | **Different core values** from the app's five (Evangelism, Discipleship, Fellowship, Worship, Ministry). Added by `61fc7ff` / `13d4d27` on 2026-11-18. |
| I3 | `perazim-mission-church`, `about/index.html` lines 165, 197 and 200 | Doctrine, constitution, membership | "Our full statement of faith is in our church constitution." "A short summary of our 2017 constitution." "Membership: … the pastor and local church council decide." | PUBLIC | **A 2017 constitution is cited publicly and is not in any repository.** The website also states membership rules. Added by `9d7290d` on 2026-11-18. |
| I4 | `perazim-mission-church`, `about/index.html` lines 208 and 209 | L | "Rev. Margaret Mutweri … Known as Mama Bishop, co-founder…" | PUBLIC | Leadership claim. Not confirmed by any document in the repositories. |
| I5 | `perazim-mission-church`, footer of all pages | N, S | "The Place of Great Breakthrough. A Pentecostal and Evangelical church family in Embu, Kenya." | PUBLIC | Footer tagline. |
| I6 | `perazim-app`, `src/lib/feed/snapshot/config.json` lines 4 and 5 | N, M | `"church_name": "Perazim Mission Church"`, `"motto": "The Place of Great Breakthrough"` | SHARED FEED | The Android app does not read these keys (no match in `src/main/java` or `server/src`). The feed is a separate copy of the motto. |
| I7 | `perazim-app`, `src/lib/content.ts` lines 7 to 12 | N, tagline | "Perazim Mission Church"; tagline "God of the Breakthrough" | PUBLIC (web app) | A fourth tagline. "God of the Breakthrough" also appears in Android `MainActivity` share text (line 3045, dead legacy code) and in the website's meta description. It is not in any approval record. |

## J. Android README (not shown to members)

| # | Location | Element | Current wording | Status | Note |
|---|---|---|---|---|---|
| J1 | `README.md` line 3 | N | "A church companion app for Perazim Mission Church (Embu, Kenya)…" | REPO | The motto, slogan, vision, mission, values and leadership block was **removed on 2026-11-18** by commit `a39ff52` ("docs: legal drafts v0.2…"). It was added on 2026-09-12 by `523300f`. |
| J2 | `README.md` line 99 | H + contacts | Church contact details come from `ChurchConfig` | REPO | Phone, email and paybill are kept in `ChurchConfig.java`. They are not repeated here. |

## Summary of counts

- **Live member-facing locations** (sections A to G, plus D9 and G6 as unverified): about 35.
- **Live mission statements a member can see**: one only (C6, Mission B, in the About dialog).
- **Live vision or core values a member can see**: none. Both exist only in dead legacy code (H7).
- **Live motto**: yes, in several places (A3, A5, B3).
- **Live slogan**: yes, with three different spellings (A6, C4, H6 is dead).
- **Public website**: different mission, different core values, different location wording, and a cited constitution that is not in any repository (I1 to I3).

## Proposed branding-change map (for leadership decision only, nothing changed)

Each proposal depends on a decision in `CHURCH_IDENTITY_DOSSIER_2026-11-20.md`, section 9.

| Screen | Current | Proposed (pending decision) | Depends on |
|---|---|---|---|
| Launcher label (A1) | "Perazim" | Keep until the app name is decided. | Decision 4 |
| Splash, onboarding, home header (A2 to A5, B1 to B3) | "Perazim Mission Church"; motto | Keep the motto only after leadership confirms it. Otherwise hide it. Keep the name until decision 4. | Decisions 1, 4 |
| Slogan (A6, C4) | Three spellings | Use one approved spelling, or hide the slogan. | Decisions 2, 3 |
| About dialog mission line (C6) | Mission B | Hide it until a mission is approved. Replace with the approved text. | Decision 2 |
| About dialog leadership and location (C5, D4) | "Embu Headquarters"; "Central Campus" | Use one approved location name. Confirm the leadership title. | Decision 5 |
| Legal and privacy text (E1 to E4) | "PMCI", "PERAZIM MISSIONS CHURCH INTERNATIONAL", placeholders | Confirm the registered legal name before publishing. Do not change until then. | Decision 4 and the legal name |
| AI text (F1, F2) | "Perazim Missions Church International" | No change. Matches the confirmed AI name. | None |
| Dead legacy code (H1 to H11, C7, C8) | Mixed | Not visible. Recommend deleting in a later cleanup, after leadership decisions. | Engineering choice |

## Limits

- Live status was traced by method call sites in this repository only. Reflection, intents from other apps and runtime data from the server were not checked.
- The inventory covers the Android app and the two sibling repositories listed. The church's Facebook and YouTube channels were not checked.
- No device was used. All evidence is from source files and git history.
