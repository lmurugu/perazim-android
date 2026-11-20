# Church identity decision brief (2026-11-20)

> **Correction (2026-11-20).** The launcher and application label is "Perazim", not "Perazim Mission Church" (section 4 below is wrong on this). Vision, Mission A and the core values exist only in dead legacy code and are not shown to members. Mission B is the only mission a member sees. See `CHURCH_IDENTITY_DOSSIER_2026-11-20.md` section 2 for all corrections and `CHURCH_IDENTITY_SCREEN_INVENTORY_2026-11-20.md` for live/dead status.

**Purpose:** evidence for leadership decisions. **Nothing here approves a motto, mission, vision, slogan or values statement.** Repository history shows when text entered the code. It does not show who approved it. Commit messages are not approval evidence.

## 1. Confirmed decision (owner, 2026-11-20)
- AI-facing name: **Perazim Missions Church International**; abbreviation **PMCI**. Implemented in AI identity (Document 02 section 2.1 and standing instructions). No app branding was changed.

## 2. Statements and their history in the repository

| Statement | Wording | First appearing commit (repo history) | Commit message | Source cited? | Current location(s) |
|---|---|---|---|---|---|
| Motto | "The Place of Great Breakthrough" | `523300f` 2026-09-12 | "feat(identity): integrate canonical church name, motto, slogan, mission, vision, and core values" | No source cited in message | `MainActivity.java` (several), `ProfileUiBinder.java`, `HomeUiBinder.java`, `OnboardingDialog.java` |
| Slogan | "A place where everybody is somebody, and no body is a nobody" | `523300f` 2026-09-12 | same | No | `MainActivity.java` |
| Vision | "To be a Center for Missions" | `523300f` 2026-09-12 | same | No | `MainActivity.java` |
| Mission A | "Perazim exists to draw people to Christ, to disciple them to belong to His family, and to have them glorify GOD with their lives and their services." | `523300f` 2026-09-12 | same | No | `MainActivity.java` (wording differs slightly from the owner's quoted version: "disciple them to belong" and "glorify God through their lives and service") |
| Mission B | "Spreading the Gospel of Christ across Kenya & Beyond" | `97f2396` 2026-11-05 | "fix(ui): repair usability defects across Bible reader, sermons, AI guide, and campus selector" | No | `ProfileUiBinder.java` (profile dialog) |
| Core values | "Evangelism • Discipleship • Fellowship • Worship • Ministry" | `523300f` 2026-09-12 | same | No | `MainActivity.java` |
| Church name, app | "Perazim Mission Church" | `57ad05c` 2026-09-12 ("initial public release of Perazim Mission Church Android app") | Yes | App name at first release | App UI, share texts |
| Church name, AI-facing | "Perazim Missions Church International" | `58f593b` 2026-11-09 (Phase B membership work) | Membership and identity work | No | Docs and some code |

## 3. Findings
1. Motto, slogan, vision, mission A and core values were all introduced in one commit on 2026-09-12. That commit cites no source document. Its path (`src/main/java/com/example/app/MainActivity.java`) is an old package layout, so the code has since moved.
2. Mission B came in later, on 2026-11-05, in a commit about UI fixes. It appears to have been added as a profile line, not as a formal mission statement.
3. So mission A and mission B were introduced at different times, and neither has an approval record. Neither supersedes the other by any recorded decision.
4. No commit, document or file in the repository records formal leadership approval of any of these statements.

## 4. Application branding: do not harmonise yet
Proposed replacements are listed for decision only. They are **not** made.

| Location | Current text | Scope (international, local or historical) | Proposal | Owner decision |
|---|---|---|---|---|
| App name and launcher | "Perazim Mission Church" (app/manifest and share texts) | App identity | Keep until decided | Required |
| `MainActivity.java` pastor attribution and share text (~1480–1534, ~2731–2747) | "PERAZIM MISSION CHURCH" | Could be read as the international church | Decide | Required |
| `ProfileUiBinder.java` profile dialog | "Mission: Spreading the Gospel..." | Mission B | Hold until mission decided | Required |
| Historical records (commit history, archived docs) | Mixed | Historical | Do not change | No change |
| Database keys, package names, `ChurchConfig`, legal references | Various | Technical or legal | Do not change | No change |

## 5. Decisions needed from leadership
1. Which motto (if any) is official.
2. Which mission statement (A, B, neither, or a new one written by leadership) is official. Note: Mission A is not confirmed by any document, so do not treat it as adopted.
3. Vision, slogan and core values: approve, amend or reject.
4. Whether the app should show "Perazim Missions Church International" or keep "Perazim Mission Church" (for the app's name or for the local congregation).
5. Whether the local congregation keeps a separate name.

## 6. Interim rule (unchanged)
Until decided, the assistant must not state any motto, slogan, vision, mission or value as official, and must say the official statement has not been established in its approved sources.
