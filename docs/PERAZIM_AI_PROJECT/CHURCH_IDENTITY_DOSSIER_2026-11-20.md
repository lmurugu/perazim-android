# Church identity decision dossier (2026-11-20)

**For leadership review. Nothing in this dossier approves a name, motto, slogan, vision, mission, value, leadership title or doctrinal statement.** Where a source is quoted, the quote shows what the source says, not that it is official. Repository history shows when text entered a file. It does not show who approved it. Commit messages are not approval evidence.

Base: `origin/feature/offline-qwen-phone` at `15c6964`. Three repositories were read: `perazim-android` (app), `perazim-mission-church` (public GitHub Pages website, HEAD `96e4c3b`, 2026-11-19) and `perazim-app` (web app and shared feed, HEAD `bfd336e`). Companion files: `CHURCH_IDENTITY_SCREEN_INVENTORY_2026-11-20.md` (every location, with live/dead status), `CHURCH_IDENTITY_COMPARISON_2026-11-20.md` and `CHURCH_IDENTITY_DECISION_BRIEF_2026-11-20.md`.

---

## 1. Summary

1. **One name is confirmed.** The AI-facing name is **Perazim Missions Church International**, abbreviated **PMCI** (owner decision, 2026-11-20). Document 02, section 2.1 states it, and the on-device prompt and out-of-scope reply use it (`LocalPrompt.java:22`, `PerazimAiEngine.java:339`).
2. **Everything else is unapproved.** No document in the three repositories records approval of a motto, slogan, vision, mission or values statement.
3. **The public channels disagree with each other.** The app and the website give different missions, different core values and different locations. The app also has more than one slogan spelling.
4. **A member sees less than the earlier brief suggested.** Vision, Mission A and the five core values appear only in legacy `MainActivity` code that no member can reach (inventory H7). The only mission a member sees in the live app is Mission B, in the About dialog (inventory C6).
5. **A 2017 church constitution is cited but missing.** The public website says its statement of faith and membership summary come from "our church constitution" and a "2017 constitution". No copy is in any repository. The AI documents also rely on a constitution that is not present (inventory I3; section 6).
6. **Public membership rules are stated without a record.** The website says any born-again Christian may join, and that "the pastor and local church council decide" (I3). No membership policy document was found.
7. **The legal name is unknown.** The privacy documents still hold a placeholder for "exact registered legal name" (E3, E4). Publishing them needs that name.

---

## 2. Corrections to the earlier documents

The decision brief and comparison from earlier today contain these errors or gaps. They are corrected here, and each earlier document carries a pointer to this section.

| # | Earlier claim | Correction | Evidence |
|---|---|---|---|
| C1 | Brief, section 4: app name and launcher are "Perazim Mission Church". | The launcher label and the application label are **"Perazim"**. "Perazim Mission Church" is shown inside the app, not as the app name. | `AndroidManifest.xml` lines 52 and 63 |
| C2 | Comparison, section B: vision, mission and values are hard-coded and shown in the app. | Vision, Mission A and the five core values exist only in `buildLegacyFellowshipScreen`, which has no call site. A member cannot see them at `15c6964`. | `MainActivity.java` 2177 to 2233; `grep` for call sites |
| C3 | Comparison, section A: "Perazim Mission Church" appears in "app UI and share texts, e.g. `MainActivity.java` (pastor attribution, share text, lines ~1480–1534, ~2731–2747)". | Those lines are in dead legacy code. Live identity wording is in the binders (inventory section B to D). | Inventory H3, H11 |
| C4 | Brief, section 2: slogan appears in one place. | Three spellings exist: "nobody is a nobody" (`OnboardingDialog` 138), "no body is a nobody." with a full stop (`ProfileUiBinder` 858), and "no body is a nobody" (legacy `MainActivity`). | Inventory A6, C4, H6 |
| C5 | Brief: Mission A wording "differs slightly from the owner's quoted version". | The owner's quotation is not in the repositories. I could not verify it. The only text in the repository is the one added by `523300f`. | `git show 523300f` |
| C6 | Brief: the motto, slogan, vision, mission A and values were introduced in `523300f` on 2026-09-12. | Confirmed. `523300f` is the earliest commit in the history containing those strings (12:36 +0300). Later commits `3ac4f8c` (14:50) and `9b4270a` (18:25) on the same day add more copies. | `git log -S` |
| C7 | Brief, section 2: Mission B came in `97f2396`, 2026-11-05. | Confirmed. The string first appears there. | `git log -S` |
| C8 | Not mentioned in the brief. | The README identity block (motto, slogan, vision, mission, values, bishop's contacts) was **removed** on 2026-11-18 by `a39ff52`. The block was added by `523300f`. | `git show a39ff52 -- README.md` |
| C9 | Not mentioned. | The public website has its own mission, core values and location wording. Added 2026-11-18 by commits authored as "Claude" (`13d4d27`, `61fc7ff`, `9d7290d`). | Section 4 |

---

## 3. Element-by-element comparison

Status key: **Confirmed** = owner decision recorded. **Known** = the source and first commit are known, but no approval record exists. **Uncertain** = sources disagree or the wording varies. **Needs leadership** = a decision or document is needed before the element can be used as official.

### 3.1 Name

| Form | Where used | Status |
|---|---|---|
| Perazim Missions Church International (PMCI) | Document 02 s.2.1, AI prompts, privacy drafts (`PMCI` in `AuthDialog`), commit `58f593b` (2026-11-09) | **Confirmed** for AI use (owner, 2026-11-20). Legal use **needs leadership**. |
| Perazim Mission Church | Splash, home, profile, giving, worship, website, feed `config.json` (`church_name`) | **Known**. Used everywhere in member-facing text. First use in `57ad05c` (2026-09-12, "initial public release"). |
| PERAZIM MISSION CHURCH | Home banner, About tag, onboarding badge | **Known**. Upper-case styling of the same name. |
| PERAZIM MISSIONS CHURCH INTERNATIONAL | Privacy drafts (`pmci-public-privacy-policy.md` line 4) | **Needs leadership** (legal name). |
| Perazim Church | Email subject "Perazim Church Inquiry" (dead code), reflections seed (`reflections.json` 65) | **Uncertain**. Informal short form. |
| Perazim (launcher) | `AndroidManifest.xml` | **Known**. The label a member sees on the home screen. |

**Leadership question:** which name is the legal name, which name members see, and whether the local congregation keeps a separate name. See decision 5.

### 3.2 Motto

| Wording | Where | Status |
|---|---|---|
| "The Place of Great Breakthrough" | App home, splash, onboarding, About (dead), feed `config.json` (`motto`), website footer tagline, website meta | **Known** source: `523300f`, 2026-09-12. No approval record. Scripturally tied to 2 Samuel 5:20 and to the name "Baal-Perazim". |

**Leadership question:** is it the official motto? See decision 1.

### 3.3 Slogan

| Wording | Where | Status |
|---|---|---|
| "A place where everybody is somebody, and no body is a nobody" | README (removed in `a39ff52`), legacy `MainActivity` (dead) | **Known** source: `523300f`. |
| "A place where everybody is somebody, and nobody is a nobody." | `OnboardingDialog` 138 (live, first launch) | **Uncertain**. Different wording. |
| "A place where everybody is somebody, and no body is a nobody." | `ProfileUiBinder` 858 (live, About dialog) | **Uncertain**. Different punctuation. |

The wording must be fixed before the slogan can be used. See decision 3.

### 3.4 Vision

| Wording | Where | Status |
|---|---|---|
| "To be a Center for Missions" | Legacy `MainActivity` 2209 (dead). README (removed in `a39ff52`). | **Known** source `523300f`. Member-invisible at `15c6964`. No approval record. |

See decision 4.

### 3.5 Mission

| Wording | Where | Status |
|---|---|---|
| **Mission A**: "Perazim exists to draw people to Christ, to disciple them to belong to His family, and to have them glorify GOD with their lives and their services." | Legacy `MainActivity` 2217 (dead). README (removed). | **Known** source `523300f`. Not shown to members. |
| **Mission B**: "Spreading the Gospel of Christ across Kenya & Beyond" | `ProfileUiBinder` 867 (live, About dialog). | **Known** source `97f2396` (2026-11-05, a UI-fix commit). Only mission text a member sees. |
| **Mission C (website)**: "Transforming the city of Embu with the gospel of Jesus Christ." | `perazim-mission-church` `about/index.html` line 133 (public). | **Known** source `13d4d27` (2026-11-18, author "Claude"). Public. |

**Three different missions are in use.** None is approved. See decision 2.

### 3.6 Core values

| Set | Where | Status |
|---|---|---|
| Evangelism • Discipleship • Fellowship • Worship • Ministry | Legacy `MainActivity` 2233 (dead). README (removed). | **Known** source `523300f`. Not shown to members. |
| Jesus is our center · People are our heart · Generosity in action | Website `about/index.html` lines 157 to 159 (public) | **Known** source `61fc7ff` / `13d4d27` (2026-11-18). Public. |

**The two sets share no words.** See decision 3.

### 3.7 Leadership

| Claim | Where | Status |
|---|---|---|
| "Leadership: Bishop Dr. David Mutweri" (no title on the live About dialog); "presiding bishop" on the website | App About dialog (`ProfileUiBinder` 865, live); website `about/index.html` | **Uncertain**. Stated in every channel. No appointment or succession record found. |
| "founder and presiding bishop… since 2003" | Website `about/index.html` line 147 | **Uncertain**. Date not supported by any document. |
| Rev. Margaret Mutweri, "Mama Bishop", co-founder | Website `about/index.html` lines 208 and 209 | **Uncertain**. Not in the app. Added `1436e92`, 2026-11-18. |
| Bishop's title "Dr." and "Bishop Dr. David Mutweri · Central Campus" | App sermons banner (`SermonsUiBinder` 370, live) | **Uncertain** wording. The location is "Central Campus", not the About dialog's "Embu Headquarters". |

### 3.8 Location

| Wording | Where | Status |
|---|---|---|
| "Embu Headquarters, Kenya" | App home, About dialog, maps query | **Uncertain**. Used by the app. |
| "Central Campus" | App sermons banner (live) | **Uncertain**. |
| "Embu Central Campus, Manyatta, Embu County" | Website (about, contact, footer) | **Uncertain**. Website is public. |

**Leadership question:** which name is the official location. See decision 6.

---

## 4. Cross-source comparison

| Element | App (`perazim-android`, live) | Website (`perazim-mission-church`, public) | Feed config (`perazim-app`) | AI documents (`docs/PERAZIM_AI_PROJECT`) |
|---|---|---|---|---|
| Name | Perazim Mission Church (member text); "Perazim" (launcher) | Perazim Mission Church | `church_name`: Perazim Mission Church | Perazim Missions Church International (PMCI) |
| Motto | The Place of Great Breakthrough | The place of great breakthrough (footer tagline) | `motto`: The Place of Great Breakthrough | Not stated |
| Slogan | Three spellings (section 3.3) | Not used | Not used | Not used |
| Vision | Not shown to members | Not used | Not used | Not used |
| Mission | Mission B (About dialog) | Mission C ("Transforming the city of Embu…") | Not used | Not used |
| Core values | Not shown to members (five-word set) | Three-value set ("Jesus is our center"…) | Not used | Not used |
| Location | Embu Headquarters / Central Campus | Embu Central Campus, Manyatta | Not used | Not used |
| Constitution / statement of faith | Not referenced | "2017 constitution" cited; statement of faith "in our church constitution" | Not referenced | Documents 03, 04 and 07 cite "the Constitution" and an "officially adopted Statement of Faith" that are **not present** in any repository |

**Finding:** the website is public and is the source most members will read. Its mission and values differ from the app's. Neither is approved. The feed config is shared with the website. The Android app does not read its `motto` or `church_name` keys (no match in `src/main/java`).

---

## 5. Name variants (quick reference)

- Full AI name: "Perazim Missions Church International" (Document 02, AI prompts). Note the plural "Missions".
- Public and app name: "Perazim Mission Church" (singular "Mission").
- Upper case: "PERAZIM MISSION CHURCH" (app banners).
- Legal drafts: "PERAZIM MISSIONS CHURCH INTERNATIONAL" (plural, upper case) with "[exact registered legal name]" placeholder.
- Informal: "Perazim Church", "Perazim".
- Abbreviations: "PMCI" (AI and privacy), "PMC" (website `README.md`: "Perazim Mission Church (PMC)").

The app and the AI use different plurals. This is a real discrepancy and needs a decision (decision 5).

---

## 6. Missing-document register (additions)

These extend `MISSING_DOCUMENTS_REGISTER_2026-11-20.md`.

| # | Document | Why it matters | Evidence | Action for owner |
|---|---|---|---|---|
| M1 | **2017 church constitution** | Cited publicly on the website. Documents 03, 04, 07 and 11 treat it as an authority. Membership, governance and doctrine depend on it. | `perazim-mission-church` `about/index.html` lines 165, 197. `17_GOVERNED_KNOWLEDGE_IMPLEMENTATION.md` line 24 says a file "PERAZIM CONTITUTION.pdf" was referenced by a task but not found in the repository. | Provide the signed copy and its date, or state that none exists. |
| M2 | **Official statement of faith** | The website publishes a long "Article of Faith" section, and says the full statement is in the constitution. The AI documents require an "officially adopted Statement of Faith". | Website lines 165 to 185 (approx.). | Confirm whether the published text is the adopted statement. |
| M3 | **Registered legal name and ODPC registration** | Privacy drafts have placeholders. Publication is blocked. | `pmci-public-privacy-policy.md` line 15; `docs/legal/privacy-policy.md` line 8. | Provide the registration certificate details. |
| M4 | **Appointment record for presiding bishop and co-founder titles** | Leadership is stated in every channel. Dates ("since 2003") and the co-founder title are not supported. | Website lines 147, 208, 209. | Confirm titles, dates and whether they may be published. |
| M5 | **Membership policy** | The website states membership rules. `13_MEMBERSHIP_AND_ADMINISTRATION.md` line 82 says an approved verification workflow exists "for the pilot", but no approval record is in the repository. | Website line 200; Document 13 lines 80 to 82 and 297. | Provide the approval record for the pilot workflow, or state that it is a draft. |
| M6 | **Official contacts and location name** | Four location names and two email addresses appear in public text. | Inventory B1, C2, D4, I1. | Confirm one location name and the approved contacts. |
| M7 | **Approval record for identity text (motto, slogan, vision, mission, values)** | Repeated across app and website, but no document approves it. The 2026-09-12 commit has no source citation. | `523300f` message and diff. | Confirm, amend or reject each element (decisions 1 to 4). |
| M8 | **Source for Mission A's owner-quoted wording** | The brief says the owner's quoted version differs from the repository. The quoted version is not in the repositories. | Brief section 2. | Provide the owner's exact text, if it exists. |

---

## 7. Decisions awaiting leadership approval

Each decision gives options, a recommendation and an "other". Items 1 to 6 change what members or the AI may say.

**Decision 1: Official motto.**
- A. Confirm "The Place of Great Breakthrough" with a dated leadership record. **(Recommended.)**
- B. Hold: no official motto until leadership writes one.
- Other: state a different motto.
*Why:* the phrase is used in every channel and matches the name and 2 Samuel 5:20. It still needs a written decision.

**Decision 2: Official mission statement.**
- A. Mission A (the 2026-09-12 text in the app's legacy code). Not shown to members today.
- B. Mission B ("Spreading the Gospel of Christ across Kenya & Beyond"), the only one members see now.
- C. Mission C from the website ("Transforming the city of Embu…").
- D. A new single text written by leadership and used on both the app and the website. **(Recommended.)**
- Other: state a different choice.
*Why:* three texts are in use and none is approved. One text for both channels avoids a conflict on public pages.

**Decision 3: Core values and slogan wording.**
- A. Use the app's five values (Evangelism, Discipleship, Fellowship, Worship, Ministry).
- B. Use the website's three values (Jesus is our center, People are our heart, Generosity in action).
- C. A new set written by leadership, used on both channels, with one slogan spelling. **(Recommended.)**
- Other: state a different choice.

**Decision 4: Vision.**
- A. Keep "To be a Center for Missions", confirmed in writing. **(Recommended, as a holding position, once confirmed. Until then it should not appear to members.)**
- B. Drop the vision.
- Other: state a different vision.
*Why:* the vision is not shown to members at present, so no change is urgent. It should still be decided before it is shown.

**Decision 5: Name for member-facing text.**
- A. Keep "Perazim Mission Church" in app and website, and use "Perazim Missions Church International" only in legal, AI and privacy text. **(Recommended until the legal name is confirmed.)**
- B. Use "Perazim Missions Church International" everywhere, including the app.
- C. Keep the current mix and do nothing.
- Other: state a different choice.
*Why:* the owner has confirmed the AI name only. The legal name is still missing, and the app has used "Perazim Mission Church" since its first release.

**Decision 6: Location name.**
- A. "Embu Central Campus, Manyatta" (website). **(Recommended if this is the address members use. Confirm first.)**
- B. "Embu Headquarters" (app).
- C. "Central Campus" (sermons banner).
- Other: state a different name.

**Decision 7: Public website statements (mission, values, membership, constitution summary).**
- A. Mark them "pending approval" on the website until leadership signs them off. **(Recommended.)**
- B. Approve them as published.
- C. Remove them.
- Other.
*Why:* they were written on 2026-11-18 by an AI session, not by leadership, and the website is the most public channel.

**Decision 8: Constitution and statement of faith.**
- A. Provide the 2017 constitution and the adopted statement of faith, with dates, so they can be added to the approved source list. **(Recommended.)**
- B. Confirm that no constitution is in force, so the AI and the website must stop citing one.
- Other.

**Decision 9: Leadership titles and dates.**
- A. Confirm the presiding bishop title, the "since 2003" date and the co-founder title in writing. **(Recommended.)**
- B. Remove the dates and unconfirmed titles from public text until confirmed.
- Other.

**Decision 10: Legal name and privacy publication.**
- A. Provide the exact registered legal name and ODPC registration details, to unblock the privacy policy. **(Recommended.)**
- B. Keep the privacy drafts unpublished until a later date.
- Other.

---

## 8. Interim rule for the AI and for the app

Until the decisions above are recorded:

- The assistant must not state a motto, slogan, vision, mission or set of values as official Perazim positions. It must say that the official statement has not been established in its approved sources.
- It must not name a legal entity, a registration number or a constitution clause.
- It may use "Perazim Missions Church International (PMCI)" as its AI-facing name, which is confirmed.
- It must not describe membership or governance as official. Membership decisions go to leadership.

This rule matches section 6 of the decision brief and section E of the comparison.

---

## 9. What is not established

- Whether any of the identity statements was approved outside the repositories. The repositories hold no approval record. A paper or verbal approval would not appear here.
- Whether the owner's quoted mission wording exists in a document.
- Whether the church's Facebook and YouTube channels use the same text. They were not checked.
- Whether the Play Store listing or the App Store copy uses any of these wordings. Not checked.
- Whether the website's `about` page text matches the printed church materials. Not checked.

---

## 10. Evidence index

| Claim | Evidence |
|---|---|
| Motto, slogan, vision, Mission A and values first added | `523300f`, 2026-09-12 12:36 +0300, "feat(identity): integrate canonical church name…". Diff in `MainActivity.java` and `README.md`. |
| Earlier copies of the motto on the same day | `3ac4f8c` (14:50) and `9b4270a` (18:25) |
| Mission B first added | `97f2396`, 2026-11-05 10:58 +0300, "fix(ui): repair usability defects…"; `ProfileUiBinder.java` diff |
| Name "Perazim Mission Church" first used | `57ad05c`, 2026-09-12, "initial public release" |
| PMCI first used | `58f593b`, 2026-11-09, "DATA-01 privacy governance and Phase B membership identity…" |
| README identity block removed | `a39ff52`, 2026-11-18, "docs: legal drafts v0.2…" |
| Website mission and values added | `13d4d27` and `61fc7ff`, 2026-11-18, author "Claude" |
| Website membership and constitution summary added | `9d7290d`, 2026-11-18, author "Claude" |
| Website leadership co-founder added | `1436e92`, 2026-11-18 |
| Website repository created | `07410ed`, 2026-09-11, Levi Murugu ("initialize … website repository") |
| Owner-confirmed AI name | Decision brief section 1 (owner, 2026-11-20). Document 02 section 2.1 states it; that file was last edited in `661ca7a`, 2026-11-20, Levi Murugu. |
| Brand logos and splash | `639aa67`, 2026-11-20, Levi Murugu. Logo wording not checked. |
| Live vs dead status of each string | Inventory; `git grep` for each method's call sites at `15c6964` |

---

## 11. Limits

- Evidence is from repository files and git history only. No device or live website was checked.
- Live status was judged from call sites in the repository. Reflection and runtime data were not checked.
- "Approved" means a written record in one of the three repositories. Records held outside the repositories cannot be seen here.
- No application text, branding or code was changed.
