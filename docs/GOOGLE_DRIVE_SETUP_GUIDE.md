# Perazim Church Embu — Multi-Media & Google Drive Automated Database Architecture

## 1. System Overview & The Multi-User Problem

When building a production-grade church mobile application for thousands of congregants, using Google Drive directly presents a major technical hurdle: **Google Drive API Bandwidth Quotas and Concurrency Rate Limits**.

If hundreds or thousands of believers in Embu, Nairobi, and worldwide try to simultaneously download or stream a 50MB sermon audio file directly from a standard Google Drive link during Sunday morning or fellowship hours, Google's anti-abuse filters will return:
```http
HTTP 403 Forbidden: User rate limit exceeded / Download quota exceeded for this file
```

### The Solution: Zero-Cost Decoupled Streaming Pipeline
Our architecture decouples **content indexing (metadata)** from **media streaming**:

```
[Church Media Team]
       │ (Uploads MP3 audio, PDF notes, MP4 recordings)
       ▼
[Google Drive Shared Folder] 
       │ (Auto-scanned every hour via Google Apps Script)
       ▼
[PerazimDriveSync.gs (Web App Endpoint)]
       │ (Caches JSON catalog via CacheService - 0ms overhead, 0 cloud cost)
       ▼
[Perazim Android App (Room Offline SQLite)]
       │
       ├──► 🎥 Videos: Streamed via YouTube CDN (Unlimited bandwidth, 0 buffer)
       ├──► 🎙 Audio: Progressive streaming + Offline Local Room Cache (`getExternalFilesDir`)
       ├──► 📄 Study Notes: Native Android PDF viewer with Drive web fallback
       └──► 💬 Fellowship: 1-click WhatsApp community broadcast with scripture quotes
```

---

## 2. Multi-Format Asset Pairing

When the media team uploads Sunday service materials, they can name the files using a simple, human-friendly convention:
- **Audio Recording:** `2026-02-15_Supernatural_Breakthrough_Bishop_Mutweri.mp3`
- **Study Notes:** `2026-02-15_Supernatural_Breakthrough_Bishop_Mutweri.pdf`
- **Video Recording:** `2026-02-15_Supernatural_Breakthrough_Bishop_Mutweri.mp4` (or uploaded to YouTube `@perazimchurchembu`)

`PerazimDriveSync.gs` automatically strips extensions and pairs matching names together into a **single unified Sermon entry** in the app with:
- `audioUrl`
- `pdfUrl` (Study Notes)
- `videoUrl`
- `preacher` ("Bishop Dr. David Mutweri")
- `passage` ("2 Samuel 5:20")
- `series` ("Breakthrough 2026")

---

## 3. How to Deploy the Google Apps Script Web App (5 Minutes)

1. Open your web browser and go to: [https://script.google.com](https://script.google.com)
2. Click **New Project** and name it `Perazim Sermon Sync`.
3. Paste the contents of [`scripts/PerazimDriveSync.gs`](../scripts/PerazimDriveSync.gs) into the editor.
4. Replace `PERAZIM_FOLDER_ID` with the ID from your Google Drive folder:
   - For example, if your folder link is:
     `https://drive.google.com/drive/folders/1aBcDeFgHiJkLmNoPqRsTuVwXyZ`
   - Your ID is: `1aBcDeFgHiJkLmNoPqRsTuVwXyZ`.
5. Click **Deploy** -> **New deployment**.
6. Select **Web app**:
   - **Description:** `Perazim Android Live Sermon Feed`
   - **Execute as:** `Me (your email)`
   - **Who has access:** `Anyone` (required so the Android app can read the JSON catalog without asking members to log in).
7. Click **Deploy**, authorize Drive read-only permissions, and copy the generated **Web App URL**.
8. Paste this URL into `YouTubeFeedRepository.java` or `SermonsViewModel.java`.

---

## 4. Kenyan Telco Optimization (Safaricom & Airtel)

Kenyan mobile users frequently experience fluctuating network conditions and mobile data limits. The Perazim app is engineered with three vital optimizations:

1. **Offline-First Audio Caching**:
   - Believers can tap **📥 Download** while at church connected to Wi-Fi.
   - The sermon audio is saved to encrypted application storage (`getExternalFilesDir("sermons")`).
   - Members can listen while traveling or at home without spending mobile data bundles.
2. **YouTube Video Streaming Integration**:
   - Video feeds from `@perazimchurchembu` stream directly from Google/YouTube edge nodes located in Nairobi (IXP/Google GGC), ensuring low latency and adaptive bitrate (144p to 1080p).
3. **WhatsApp Fellowship Sharing**:
   - One-tap sharing formats the sermon title, Bishop's message, Bible scripture, and YouTube/audio links into a formatted WhatsApp message with bold markdown and emojis, ready to forward to family and home cell groups.
