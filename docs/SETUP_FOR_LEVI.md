# Setup & Operational Guide for Levi

> **Stage 1: Google Sheet Dynamic Content Setup**  
> Complete these human steps to link the Perazim Church Google Sheet to the mobile app.

---

## 1. Create the Church Content Spreadsheet

1. Log into your Google account (e.g., church secretariat or personal Gmail).
2. Go to [Google Sheets](https://sheets.google.com) and create a **Blank spreadsheet**.
3. Name the spreadsheet: **`Perazim Church Content Master`**.

---

## 2. Import the 8 Content Tabs

Inside your cloned repository, navigate to `docs/sheet-template/`. You will find 8 pre-populated CSV files:

| Template File | Sheet Tab Name | Purpose |
|---------------|----------------|---------|
| `announcements.csv` | `announcements` | Weekly bulletins & emergency church news |
| `events.csv` | `events` | Fellowship meetings, Sunday services, rallies |
| `hymns.csv` | `hymns` | Hymnal lyrics, musical keys, BPM, and chord charts |
| `riddles.csv` | `riddles` | Biblical trivia riddles with answers & scripture refs |
| `jokes.csv` | `jokes` | Clean, family-safe Christian fellowship humor |
| `sermons.csv` | `sermons` | Bishop Dr. David Mutweri teachings & YouTube links |
| `verses.csv` | `verses` | Daily scripture readings mapped by calendar date (`YYYY-MM-DD`) |
| `config.csv` | `config` | Church motto, Paybill numbers, contact details |

### How to Import Each File:
1. In Google Sheets, at the bottom bar, rename `Sheet1` to **`announcements`**.
2. Click **File > Import > Upload**.
3. Drag & drop `docs/sheet-template/announcements.csv`.
4. Import location: **Replace current sheet**. Click **Import data**.
5. Click the **+** (Add Sheet) button at the bottom to add the next tab: **`events`**.
6. Repeat for all 8 files.

---

## 3. Publish the Tabs to the Web (CSV Format)

To allow the Android app to read updates without authentication or API keys:

1. In Google Sheets, click **File > Share > Publish to web**.
2. Click the **Link** tab.
3. Instead of "Entire Document", select the first tab: **`announcements`**.
4. Instead of "Web page", select **Comma-separated values (.csv)**.
5. Click **Publish** and confirm.
6. Copy the published URL. It will look like:
   ```
   https://docs.google.com/spreadsheets/d/e/2PACX-1v.../pub?gid=0&single=true&output=csv
   ```
7. Repeat step 3–6 for each of the other 7 tabs, copying down their respective CSV URLs.

---

## 4. Configure the URLs in the App

Levi sends the 8 URLs in chat and Antigravity enters them. Levi does not run any commands or builds.

Once Antigravity enters the URLs into `sheet_urls.properties` and builds the app, the endpoints are embedded directly into `SheetConfig`.

### Verifying the Setup:
1. Open the Perazim App on your Android device.
2. Tap the **Profile** tab (bottom right).
3. Scroll down to **🏛️ CHURCH IDENTITY & AFFILIATION**.
4. Tap the **🔄 Refresh Content** row.
5. You will see a toast: `🔄 Syncing church content from Google Sheets...` followed by `✅ Church content updated (X items synced)`.
6. The subtitle will update to `Last updated: <current date and time>`.

---

## 5. Ongoing Content Updates for Church Secretary

Share `docs/CONTENT_GUIDE.md` with the church secretary. The secretary only needs to:
- Open the Google Spreadsheet in a browser or phone app.
- Add or edit rows.
- Set `active` to `FALSE` to hide an announcement, sermon, riddle, or event without deleting history.
- The mobile app will automatically pick up changes within 6 hours, or immediately whenever a member taps "Refresh Content" in their profile.
