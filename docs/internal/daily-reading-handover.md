# Daily Reading handover

Home previously requested 2 Samuel 5:20 directly, and the routine dialog
reused the same fallback even when the Home scripture card could show another
passage. Both now show the same current reading from the offline catalog.

## What readers can do

- Open **Home → Reading plans** or **Reading plans** inside the Daily Reading
  dialog. **Daily verse** selects a stable passage for the Nairobi calendar
  date. The card refreshes when the app comes to the foreground or Daily Reading opens.
- Choose one of six topics, or type a topic to find matching themes. Choose
  **Days** for 1 to 30 days or **Weeks** for 1 to 4 weeks.
- Read a KJV passage, reflect on its theme, and pray. The plan keeps the same
  passage and step after a restart or a missed day. Completing a reading
  opens the next one on a later date. A finished plan stays visible until
  the reader chooses another plan or returns to Daily verse.
- Returning to Daily verse preserves that day's separately saved progress.
  Replacing an unfinished plan requires confirmation.

## Code and device storage

- `HomeUiBinder` owns the Home card, the Daily Reading dialog, and plan
  selection. `HomeViewModel` receives the selected passage for the Home feed.
  Reading a chapter in the Bible reader still counts for the original Read
  step in Daily verse mode. A topic plan advances through its own passage.
- `src/main/assets/reading/daily-readings.json` ships in the APK as the
  read-only `assets/reading/` folder. It has 180 topic entries, with KJV
  passages drawn from the project's bundled KJV corpus and authored prompts.
  The same reference can appear in multiple themes with different prompts.
- `ReadingCatalog` shuffles the daily rotation in a repeatable way and
  matches typed words against topic keywords and its passages. A typed query
  stays in memory during selection, and no network request or generative
  inference is needed. The existing Bible Search and Guide paths still work
  independently.
- `DailyReadingStore` saves plan IDs, completion count, date, and current
  step under the active account in Android app-private
  `shared_prefs/perazim_daily_reading_v1.xml`. Daily verse and active plan
  use separate records. No Room schema or backend change is needed.

## Integration and validation

The feature was integrated in `/home/murugu/Android/app` on
`codex/daily-reading`. It uses the authorized available Bible Search tip
`91ae4a9` with the Games integration as its parent. The phone build is
validation versionCode 31, and it uses the existing validation signing
configuration. The later unpublished Bible Search branch is outside this
build.

Build and test with the existing approved pilot API configuration if installing
over the pilot app. The passages and reading-plan progress remain available
offline:

```bash
export ANDROID_HOME=/home/murugu/Android/sdk
./gradlew --offline --no-daemon assembleValidation testValidationUnitTest \
  --tests 'org.perazimchurch.app.reading.*' \
  --tests 'org.perazimchurch.app.domain.games.*' \
  --tests 'org.perazimchurch.app.games.*'
```

The signed validation APK built successfully and the final source passed 228
targeted unit tests, including three Daily Reading tests. The tests cover
the 30-day bound, topic matching, account separation, date rollover, and
resume. The 180 catalog entries were checked against the bundled KJV text.

On the connected Infinix, validation version 30 displayed the new verse and
reflection. Typing `worry` suggested **Peace and anxiety**, a one-week plan
opened at Day 1, and force-stopping and reopening the app restored the same
reflection step. The temporary plan was removed and the phone returned to
Daily verse mode. No fixture routine was marked complete.

The final version 31 installed in place with the same signer and adds a
foreground date refresh. Android reported version 31. The phone locked and
then disconnected during the repeat screen check, so the exact final build's
UI replay remains pending. No app data was cleared.
