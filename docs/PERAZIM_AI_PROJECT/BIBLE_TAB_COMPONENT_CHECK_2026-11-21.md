# Bible tab component check (2026-11-21)

Branch `feature/offline-qwen-phone`, HEAD at time of writing: `58cded6`. Installed on phone: version 8, same HEAD.

## Method
- Component list taken from `src/main/java/org/perazimchurch/app/presentation/ui/BibleReaderDialog.java` (3,685 lines) and the `bible` data package.
- Automated host tests run: `org.perazimchurch.app.bible.*` and `org.perazimchurch.app.games.*`. All pass.
- Not yet done: UI interaction tests of the reader screen on a device. The emulator froze in its system UI (2 GB RAM), and the existing screenshot tests render only the Home, tab screens and the Daily reading screen. The Bible reader screen itself is not in the screenshot suite.

## Component inventory and status

| Component | Where | Automated coverage | Device check |
|---|---|---|---|
| Book and chapter selector | `showBookSelectorDialog`, `populateBooksInContainer`, `showChapterSelectorGrid` | None (UI) | Not done |
| Chapter navigation (previous/next) | `navigateChapter` | None (UI) | Not done |
| Translation selector | `showTranslationSelectorDialog`, `updateTranslationBadge` | None (UI) | Not done |
| Verse loading | `loadChapterVerses`, `BibleVerseReader` | `BibleCorpusTest`, `BibleTextImporterTest` (data) | Not done |
| Last-position restore and save | `restoreLastPosition`, `saveLastPosition` | None | Not done |
| Verse size and weight | `changeVerseSize`, `changeVerseWeight`, `saveVerseStyle` | None | Not done |
| Appearance (theme, font) | `toggleAppearance`, `applyAppearance` | None | Not done |
| Search | `toggleSearchMode`, `performSearch`, `highlightSearchTerms` | `ScriptureReferenceTest` (reference parsing only) | Not done |
| Verse picker and scroll-to-verse | `showVersePicker`, `scrollToVerse` | None | Not done |
| Verse select mode and multi-select | `enterSelectMode`, `openSelectCard`, `applyHighlightToSelected` | None | Not done |
| Whole-verse highlights and colour picker | `applyHighlight`, `buildColourPicker`, `showUndoPill` | None | Not done |
| Word and character-range highlights | `applyWordHighlight`, `clearWordHighlight`, `setWordHighlights` | None | Not done |
| Verse action bar (copy, share, compare, notes, interlinear, Strong's) | `showVerseActionBar`, `copyVerse`, `shareVerse`, `showCompareDialog` | None | Not done |
| Interlinear view | `showInterlinearDialog`, `appendInterlinearVerse` | `GreekInterlinearStoreTest`, `GrammarDescriberTest` (data and grammar) | Not done |
| Strong's dictionary | `showStrongsDialog` | `StrongsStoreTest` | Not done |
| Study notes and rich-text editor | `showNotesDialog`, `showNoteEditor`, `applyFormat`, `toggleCheckbox` | None | Not done |
| Topics | `showTopicsCard`, `fillTopics`, `showTopicDetail`, `showSaveAllToTopic` | None | Not done |
| Bible games (Daily games in the Bible area) | `games` package | `BibleGamesTest`, `LadderOfFaithTest`, `GameStreakTest`, `GamingGamesTest` (all pass) | Not done |

## Known risks found from code reading (not yet confirmed on device)
- The reader is one 3,685-line class. Many paths are untested by any automated test.
- Word-range highlights were merged from the highlight branch; they have no automated test.
- Notes and topics store user data. The data path has not been checked on a device with the migration to Room v12.

## Next steps
1. Robolectric interaction tests for the reader: open, navigate chapters, search, select and highlight a verse, add a note.
2. Device checks on the phone (after your approval) or on a working emulator.
3. Fix any defect found, with a regression test.

## On-device results (Levi's phone, version 8, 2026-11-21 ~10:00 EAT)
Method: UI taps on the phone through adb; read-only where possible. No highlights, notes or topics were saved.

| Component | Result |
|---|---|
| Opening the Bible reader | PASS: opened at 2 Samuel 5 (last position) |
| Translation selector | PASS: lists ASV, BSB, ESV, GRK, HEB, KJV, NIV, NKJV, SUV, WEB; closes. NASB not listed |
| Chapter navigation (Next, Previous) | PASS: 2 Samuel 5 → 6 → 5 |
| Text size (A+, A−) | PASS: changes and returns to the original size |
| Book and chapter selector | PASS: Old/New Testament lists; Ruth → chapter grid → chapter 2 |
| Verse picker | PASS: choosing verse 5 scrolls to it and closes |
| Highlight tools and select-verses mode | PASS: Select verses opens; select card shows Copy, Highlight colours, Save and notes; Done exits. No colour applied |
| Bible search | PASS: "Boaz" returns "Found 22 verses" with Ruth 2 hits |
| Search close | PASS: returns to reader |
| Whole-verse highlight apply/undo | NOT TESTED (would write data) |
| Word-range highlight | NOT TESTED (would write data) |
| Interlinear and Strong's dialogs | NOT TESTED (entry point not found yet) |
| Study notes editor and topics | NOT TESTED (write paths) |
| Verse size and weight persistence | NOT TESTED beyond the A+/A− round trip |

Reading position is now Ruth 2 instead of 2 Samuel 5. This is a saved position, not data loss. Levi can return to 2 Samuel 5 from the book selector.
No crashes (FATAL count 0 at each check). The app process stayed running.
Also noticed: the translation list includes NIV, NKJV and ESV in the reader. Licences for NIV and NKJV are still pending (remediation decision D4), and ESV stays in the reader only per Levi's decision.
