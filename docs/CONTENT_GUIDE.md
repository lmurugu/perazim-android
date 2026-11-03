# Perazim Church Mobile App — Content Management Guide

> **Audience**: Church Secretary, Media Team, Pastoral Secretariat  
> **Purpose**: How to update hymns, riddles, jokes, church events, announcements, sermons, and the verse of the day using Google Sheets without touching code or reinstalling the app.

---

## ⚠️ Important Rules Before You Begin

1. **The Google Sheet is Public**: Anyone with the link can view the content. **Never** put private phone numbers, personal home addresses, confidential prayer requests, accounting passwords, or private bank balances in the sheet.
2. **Do Not Rename Tabs or Headers**: The first row (the header row) contains specific column names (like `id`, `title`, `active`). Keep all column names in **lowercase** exactly as they appear in the template.
3. **Always Keep IDs Unique**: The `id` column identifies each record. Never create two rows with the same `id` in the same sheet.

---

## 1. How the Google Sheet Works

You maintain a single Google Spreadsheet with 8 tabs:
1. `announcements`
2. `events`
3. `hymns`
4. `riddles`
5. `jokes`
6. `sermons`
7. `verses`
8. `config`

When you publish each tab as a CSV link (**File → Share → Publish to web → Comma-separated values (.csv)**), the mobile app downloads your changes automatically in the background (or whenever a member taps **Refresh Content** in their Profile).

---

## 2. Column-by-Column Explanations

### Tab 1: `announcements`
Shows notice cards and pastoral bulletins on the church Fellowship screen.
* **`id`**: Unique identifier (e.g. `ann_2026_easter_fasting`).
* **`title`**: Headline of the announcement (e.g. `21 Days of Breakthrough Fasting & Prayer`).
* **`body`**: Full announcement text.
* **`date`**: Publishing date in format `YYYY-MM-DD HH:mm` (e.g. `2026-11-03 08:00`).
* **`priority`**: Priority badge: `NORMAL`, `HIGH`, or `URGENT`.
* **`campus`**: Campus filter (e.g. `campus_central`, `campus_nairobi`, `campus_meru`, or `all`).
* **`active`**: `TRUE` to display in the app; `FALSE` to hide.

---

### Tab 2: `events`
Displays the upcoming church calendar and service schedules.
* **`id`**: Unique identifier (e.g. `event_sun_breakthrough`).
* **`title`**: Name of the service or gathering (e.g. `Sunday Breakthrough Service`).
* **`start`**: Starting date & time in Nairobi time: `YYYY-MM-DD HH:mm` (e.g. `2026-11-08 09:00`).
* **`end`**: Ending date & time: `YYYY-MM-DD HH:mm` (e.g. `2026-11-08 12:30`).
* **`venue`**: Physical room or sanctuary (e.g. `Main Sanctuary, Embu Headquarters`).
* **`campus`**: Relevant campus (`campus_central` or `all`).
* **`description`**: What attendees should expect and prepare for.
* **`active`**: `TRUE` or `FALSE`.

---

### Tab 3: `hymns`
Powers the in-app hymnal, lyrics search, and chord progressions for the worship team.
* **`id`**: Unique identifier (e.g. `hymn_1`, `hymn_2`).
* **`title`**: Hymn title (e.g. `Holy, Holy, Holy! Lord God Almighty`).
* **`writer`**: Composer or author (e.g. `Reginald Heber`).
* **`year`**: Year composed (e.g. `1826`).
* **`key`**: Musical key for musicians (e.g. `D Major`, `G Major`, `Bb Major`).
* **`bpm`**: Tempo beats per minute (e.g. `80`).
* **`tags`**: Category tag (`Worship`, `Faith`, `Grace`, `Praise`).
* **`lyrics`**: Full lyrics including stanza breaks (`Verse 1:`, `Refrain:`, `Verse 2:`).
* **`chords`**: Chord progression reference for guitar/keyboard (e.g. `D - Bm - G - A - D`).
* **`active`**: `TRUE` or `FALSE`.

---

### Tab 4: `riddles`
Devotional Bible trivia riddles for fellowship and youth engagement.
* **`id`**: Unique identifier (e.g. `riddle_methuselah`).
* **`category`**: Difficulty or topic (`EASY`, `MEDIUM`, `HARD`, `PROPHETS`, `MIRACLES`).
* **`question`**: The riddle prompt.
* **`answer`**: The biblical answer revealed when tapped.
* **`xp`**: Spiritual XP awarded when solved (e.g. `10`, `20`).
* **`active`**: `TRUE` or `FALSE`.

---

### Tab 5: `jokes`
Clean Christian humor for the fellowship bulletin.
* **`id`**: Unique identifier (e.g. `joke_noah_ark`).
* **`text`**: The question and punchline.
* **`active`**: `TRUE` or `FALSE`.

---

### Tab 6: `sermons`
Recent audio and video messages from Bishop Dr. David Mutweri and guest speakers.
* **`id`**: Unique identifier (e.g. `sermon_2026_supernatural_breakthrough`).
* **`title`**: Sermon title.
* **`speaker`**: Minister's name (e.g. `Bishop Dr. David Mutweri`).
* **`date`**: Date preached: `YYYY-MM-DD` (e.g. `2026-11-01`).
* **`series`**: Sermon theme (e.g. `Breakthrough Power Series`).
* **`scripture`**: Primary scripture passage (e.g. `2 Samuel 5:17-21`).
* **`youtube_url`**: YouTube video link or channel feed.
* **`audio_url`**: Direct MP3 link (or leave empty if video only).
* **`active`**: `TRUE` or `FALSE`.

---

### Tab 7: `verses`
The Cornerstone Scripture shown at the top of the Home screen for each calendar day.
* **`date`**: Specific date in `YYYY-MM-DD` format (e.g. `2026-11-04`).
* **`reference`**: Bible reference (e.g. `Isaiah 40:31`).
* **`text`**: The full scripture text.

> **Pilot Scope & Fallback**: The initial template covers 7 days (2026-11-03 to 2026-11-09). After the last row (or on any calendar date not listed in the sheet), the app automatically falls back to showing the church foundation scripture: **2 Samuel 5:20** (*"And David came to Baal-perazim, and David smote them there, and said, The LORD hath broken forth upon mine enemies before me, as the breach of waters."*).

---

### Tab 8: `config`
Global switches for app features.
* **`min_app_version`**: Lowest supported app version (e.g. `2.2.0`).
* **`ai_enabled`**: `true` to allow the AI Guide; `false` to turn it off instantly for all users.
* **`ai_daily_limit`**: Maximum AI questions per member per day (e.g. `3`).
* **`community_enabled`**: `true` to allow community prayer wall features; `false` to pause them.

---

## 3. How to Add a New Event or Sermon

1. Open your Google Sheet in your web browser.
2. Click on the tab you want to update (for example, `events`).
3. Scroll to the bottom and add a new row.
4. Fill out every column following the format described above.
5. Make sure you set the `active` column to **`TRUE`**.
6. The changes are immediately published by Google and downloaded by phones on their next background check!

---

## 4. How to Hide or Archive an Item

**Never delete old rows completely** if you want to keep records for the future.
Instead:
* In the `active` column, change `TRUE` to **`FALSE`**.
* The app will immediately hide this row from all member phones on the next sync, while keeping your historical records in the spreadsheet.

---

## 5. Summary Checklist Before Publishing
- [ ] Are dates formatted properly? (`YYYY-MM-DD` or `YYYY-MM-DD HH:mm`)
- [ ] Is the `id` column filled and unique?
- [ ] Is `active` set to `TRUE` (or `FALSE` to hide)?
- [ ] Are all columns matching their original lowercase names?
- [ ] Is there **zero** confidential or private church financial/personal data included?
