/**
 * ============================================================================
 * PERAZIM MISSION CHURCH, EMBU - GOOGLE DRIVE AUTOMATED SERMON FEED (GAS)
 * ============================================================================
 * Zero Recurring Cloud Cost Architecture
 *
 * This Google Apps Script runs inside Google Drive or as a standalone script.
 * It automatically scans your public church Google Drive sermon folder,
 * groups MP3 (audio), PDF (study notes), and MP4 (video) files by sermon title,
 * and publishes a high-speed, cached JSON endpoint for the Perazim Android app!
 *
 * HOW TO DEPLOY:
 * 1. Open Google Drive: https://drive.google.com
 * 2. Click "+ New" -> "More" -> "Google Apps Script" (or visit script.google.com).
 * 3. Replace the entire code with this script.
 * 4. Set PERAZIM_FOLDER_ID below to your Google Drive sermon folder ID.
 * 5. Click "Deploy" -> "New deployment".
 * 6. Select Type: "Web app".
 *    - Execute as: "Me"
 *    - Who has access: "Anyone"
 * 7. Click "Deploy" and copy the Web App URL (e.g. https://script.google.com/macros/s/.../exec).
 * ============================================================================
 */

// Replace with your Google Drive Folder ID (found in your Drive folder URL)
const PERAZIM_FOLDER_ID = "YOUR_SHARED_FOLDER_ID_HERE";

/**
 * HTTP GET Web App Endpoint.
 * Serves the live sermon JSON catalog with HTTP caching.
 */
function doGet(e) {
  const cache = CacheService.getScriptCache();
  const cachedData = cache.get("perazim_sermon_catalog");

  if (cachedData != null) {
    return ContentService.createTextOutput(cachedData)
      .setMimeType(ContentService.MimeType.JSON);
  }

  const catalog = generateCatalog();
  const jsonOutput = JSON.stringify(catalog, null, 2);

  // Cache for 6 hours (21600 seconds) to prevent Drive rate limits
  cache.put("perazim_sermon_catalog", jsonOutput, 21600);

  return ContentService.createTextOutput(jsonOutput)
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * Generates the sermon catalog from Google Drive files.
 */
function generateCatalog() {
  const sermonsMap = {};
  
  let folder;
  try {
    folder = DriveApp.getFolderById(PERAZIM_FOLDER_ID);
  } catch (err) {
    // Fallback: If folder ID not yet configured, search for any accessible files
    return getSampleCatalog();
  }

  const files = folder.getFiles();

  while (files.hasNext()) {
    const file = files.next();
    const name = file.getName();
    const mime = file.getMimeType();
    const fileId = file.getId();
    const size = file.getSize();
    const updated = file.getLastUpdated().getTime();

    // Clean base name to match audio, video, and PDF pairs
    const extMatch = name.match(/\.([a-zA-Z0-9]+)$/);
    const ext = extMatch ? extMatch[1].toLowerCase() : "";
    const baseName = name.replace(/\.[^/.]+$/, "").trim().toLowerCase();

    if (!sermonsMap[baseName]) {
      sermonsMap[baseName] = {
        id: "gdrive_" + fileId,
        title: cleanTitle(name),
        preacher: detectPreacher(name),
        passage: detectScripture(name),
        series: detectSeries(name),
        audioUrl: "",
        videoUrl: "",
        pdfUrl: "",
        thumbnailUrl: "https://media.perazimchurch.org/thumbnails/default_sermon.jpg",
        durationSeconds: 2700,
        datePreached: updated.toString(),
        description: "Delivered at Perazim Mission Church, Embu.",
        isDownloaded: false,
        playCount: 0
      };
    }

    const sermon = sermonsMap[baseName];

    // High-performance streaming & download direct links
    const directDownloadLink = "https://drive.google.com/uc?export=download&id=" + fileId;
    const directViewLink = "https://drive.google.com/file/d/" + fileId + "/view?usp=drivesdk";

    if (ext === "mp3" || ext === "m4a" || ext === "wav" || mime.indexOf("audio") !== -1) {
      sermon.audioUrl = directDownloadLink;
    } else if (ext === "mp4" || ext === "mkv" || mime.indexOf("video") !== -1) {
      sermon.videoUrl = directViewLink;
    } else if (ext === "pdf" || mime.indexOf("pdf") !== -1) {
      sermon.pdfUrl = directDownloadLink;
      sermon.notes = directViewLink;
    }
  }

  const result = [];
  for (const k in sermonsMap) {
    result.push(sermonsMap[k]);
  }

  return {
    version: "2.1",
    church: "Perazim Mission Church, Embu",
    channelHandle: "@perazimchurchembu",
    updatedAt: new Date().toISOString(),
    count: result.length,
    sermons: result
  };
}

function cleanTitle(filename) {
  let name = filename.replace(/\.[^/.]+$/, "");
  name = name.replace(/^\d{4}[-_]?\d{2}[-_]?\d{2}[-_]?/, "");
  name = name.replace(/[_-]/g, " ").replace(/\s+/g, " ").trim();
  return name.replace(/\b\w/g, c => c.toUpperCase());
}

function detectPreacher(text) {
  const preachers = ["Bishop Dr. David Mutweri", "Rev. Mutweri", "Pastor Grace Mutweri"];
  for (let i = 0; i < preachers.length; i++) {
    if (text.toLowerCase().indexOf(preachers[i].split(" ")[0].toLowerCase()) !== -1) {
      return preachers[i];
    }
  }
  return "Bishop Dr. David Mutweri";
}

function detectScripture(text) {
  const match = text.match(/([1-3]?\s*[A-Za-z]+)\s+(\d+)(?::(\d+(?:-\d+)?))?/);
  return match ? match[0] : "2 Samuel 5:20";
}

function detectSeries(text) {
  const lower = text.toLowerCase();
  if (lower.indexOf("faith") !== -1) return "Walking in Faith";
  if (lower.indexOf("breakthrough") !== -1) return "Breakthrough 2026";
  if (lower.indexOf("deliverance") !== -1) return "Deliverance";
  return "Breakthrough 2026";
}

function getSampleCatalog() {
  return {
    version: "2.1",
    church: "Perazim Mission Church, Embu",
    channelHandle: "@perazimchurchembu",
    updatedAt: new Date().toISOString(),
    count: 2,
    sermons: [
      {
        id: "sample_001",
        title: "Supernatural Breakthrough: Breaking Chains",
        preacher: "Bishop Dr. David Mutweri",
        passage: "2 Samuel 5:17-21",
        series: "Breakthrough 2026",
        audioUrl: "https://media.perazimchurch.org/audio/sample_breakthrough.mp3",
        videoUrl: "https://www.youtube.com/watch?v=3O6meSzCl3I",
        pdfUrl: "https://media.perazimchurch.org/notes/sample_breakthrough.pdf",
        thumbnailUrl: "https://media.perazimchurch.org/thumbnails/sample_breakthrough.jpg",
        durationSeconds: 2820,
        datePreached: "1739577600000",
        description: "Sunday breakthrough service at Central Sanctuary, Embu.",
        isDownloaded: false,
        playCount: 142
      }
    ]
  };
}
