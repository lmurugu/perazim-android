# Perazim Mission Church — Phase B Architectural Design Specification

> **Status:** DESIGN PROPOSAL ONLY (No implementation code written in Phase A)  
> **Target Audience:** Church Leadership (Levi), Pastoral Council, and Engineering Team  
> **Repository:** `github.com/lmurugu/perazim-android`  
> **Version:** 1.0.0-PROPOSAL  
> **Date:** November 2026

---

## 1. Executive Summary

Phase A established a hardened, offline-first foundation with PostgreSQL persistence, atomic transaction integrity, JWT authentication with live revocation, and multi-client prayer wall synchronization.

Phase B expands Perazim Mission Church into a full digital sanctuary with:
1. **Granular Role-Based Access Control (RBAC):** Distinct capability boundaries across five church roles (Guest, Member, Ministry Leader, Pastor, and System Administrator).
2. **Media & Attachments Architecture:** Robust, bandwidth-conscious infrastructure for sermon audio, video streams, devotional study guides (PDF), and testimony images optimized for Kenyan mobile networks (Safaricom and Airtel).

---

## 2. Section 9: Roles & Permissions Matrix

### 2.1 Role Definitions

* **Guest (Unauthenticated / Fresh Install):** Visitors exploring the church app. Access to scripture, sermon previews, public prayers, and guest AI guidance.
* **Member (Authenticated Saint):** Registered member of Perazim Mission Church. Personal streak tracking, prayer petition submissions, amen intercession, personal praise reports, and M-Pesa tithing tracking.
* **Ministry Leader (Cell / Fellowship Leader):** Coordinates prayer teams, leads home cells, reviews cell petitions, and schedules ministry fellowship events.
* **Pastor (Clergy / Pastoral Staff):** Spiritual oversight, sermon publication, official church announcements, prayer wall pastoral responses, and member pastoral counseling.
* **Admin (System / Church Administrator):** Full administrative authority, role elevation, member directory oversight, financial audit reporting, and server configuration.

### 2.2 Permissions Capability Matrix

| System Domain | Guest | Member | Ministry Leader | Pastor | Admin |
|---|:---:|:---:|:---:|:---:|:---:|
| **Scripture & Bible Reader** | Read & Search | Read, Highlight, Bookmark | Read, Highlight, Share | Full Access | Full Access |
| **Sermon Audio & Video** | Preview (Sample) | Stream & Download Offline | Stream, Download & Share | **Publish & Manage** | Full Management |
| **Prayer Wall: Petitions** | Read Public Wall | Submit Petition (Max 3/day) | Submit & Pin Group Petitions | Submit Pastoral Intercessions | Full Management |
| **Prayer Wall: Intercession** | View Amen Counts | **Amen (1 per prayer)** | Amen & Pastoral Note | Amen & Pastoral Endorsement | Audit All |
| **Prayer Wall: Testimonies** | Read Testimonies | **Add to Own Prayer Only** | Verify Group Testimonies | Endorse Official Testimonies | Delete / Moderate |
| **Content Moderation** | Report Content | Flag Abuse | Review Flagged (Cell) | **Moderate & Hide** | **Delete & Ban User** |
| **Church Announcements** | View Public | View Targeted Campus | Propose Announcements | **Publish & Broadcast** | Full Management |
| **M-Pesa Giving & Tithes** | View Giving Numbers | Record Personal Giving Ref | View Cell Stewardship | View Ministry Totals | **Full Financial Audit** |
| **User & Role Administration** | Self-Registration | Edit Personal Profile | View Cell Roster | Pastoral Member Notes | **Assign Roles & Deactivate** |
| **Media Attachments Upload** | None | Images (Testimonies, max 2MB) | Images & PDF Guides (max 10MB) | Audio & Video (max 250MB) | Unlimited / Override |
| **Offline Sync Cache** | Ephemeral | Persistent Encrypted Room | Persistent Encrypted Room | Persistent Encrypted Room | Master Audit Cache |

### 2.3 Enforcement Strategy

* **Server-Side Enforcement (Zero Trust):**
  - JWT claims include `{ userId, phone, role, campusId }`.
  - Express middleware `requireRole(['PASTOR', 'ADMIN'])` guards elevated routes.
  - Row-Level Security (RLS) in PostgreSQL ensures members cannot mutate petitions authored by others.
* **Android Client-Side Guards:**
  - `SessionManager.hasPermission(Permission.PUBLISH_SERMON)` dynamically controls UI affordances.
  - Unauthorized actions are hidden from navigation, and network calls fail fast if tampered with.

---

## 3. Section 10: Media & Attachments Architecture

### 3.1 Content Types & Specifications

| Media Type | Typical File Size | Permitted Formats | Primary Distribution Channel |
|---|:---:|:---:|:---|
| **Audio Sermons** | 15 MB – 45 MB | MP3, AAC, M4A | Direct Stream / Offline Download |
| **Video Sermons / Highlights** | 50 MB – 250 MB | MP4 (H.264 / AAC) | Adaptive HLS Streaming (Cloudflare Stream) |
| **Study Materials & Bulletins** | 500 KB – 5 MB | PDF | Encrypted In-App PDF Viewer |
| **Testimony Praise Images** | 300 KB – 2 MB | JPEG, WebP, PNG | CDN Cached Static Storage |

### 3.2 Storage & Delivery Infrastructure

```
[ Android App ]
      │
      ├── 1. POST /api/media/upload-ticket (Authorized Pastor/Member)
      ▼
[ Node.js API Server ] ── Authenticates Role & Quotas
      │
      ├── 2. Issues Short-Lived S3/GCS Presigned PUT URL (15 min expiry)
      ▼
[ Cloudflare R2 / AWS S3 / MinIO Object Storage ]
      ▲
      │ 3. Direct Multipart Upload from Android Device (Bypasses API Server)
      │
[ Cloudflare CDN Edge Cache ]
      │
      ├── 4. Global Low-Latency Delivery (Nairobi / Mombasa PoPs)
      ▼
[ Android Client Player & Room Download Manager ]
```

### 3.3 Network & Bandwidth Optimization for Kenya

* **Local CDN Edge Caching:** All media routes route through Cloudflare Edge with regional points of presence in Nairobi and Mombasa to eliminate international transit latency on Safaricom and Airtel connections.
* **Smart Offline Pre-Caching:** Members can tap "Save for Offline" while on church Wi-Fi; the download manager downloads chunks with resume support (`Range: bytes=...`) into app-private internal storage (`context.getFilesDir()`).
* **Adaptive Audio Bitrates:** Low-bandwidth mode defaults to 48 kbps AAC for remote rural areas, scaling to 128 kbps stereo on broadband.

### 3.4 Quotas, Virus Scanning & Lifecycle

* **Upload Quotas:**
  - Members: Max 5 images per week (testimony proof).
  - Pastors: Up to 5 GB weekly sermon uploads.
* **Automated Sanitization:** Uploaded files undergo MIME-type header inspection, image EXIF metadata stripping (protecting member location privacy), and ClamAV automated scanning before becoming public.

---

## 4. Phase B Verification & Zero-Code Confirmation

* **Zero Implementation Code:** This document is purely architectural. No Phase B database tables, API routes, or Android classes were committed or deployed in Phase A.
* **Backward Compatibility:** All schemas and APIs designed for Phase B build seamlessly on Phase A's PostgreSQL tables (`users`, `prayers`, `prayer_amens`, `sync_events`).
