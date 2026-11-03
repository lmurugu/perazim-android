#!/usr/bin/env python3
"""
Compile Complete Bible Database for Perazim Android App.
Extracts all 66 books and 31,102+ verses from 5 translations:
- KJV (King James Version - English)
- SUV (Swahili Union Version - Kiswahili)
- NIV (New International Version - English)
- ESV (English Standard Version - English)
- NKJV (New King James Version - English)

Generates pre-populated Room database at:
app/src/main/assets/database/perazim_database.db
"""

import os
import sys
import json
import sqlite3
import xml.etree.ElementTree as ET

CANONICAL_BOOKS = [
    (1, "GEN", "Genesis", "OLD", 50),
    (2, "EXO", "Exodus", "OLD", 40),
    (3, "LEV", "Leviticus", "OLD", 27),
    (4, "NUM", "Numbers", "OLD", 36),
    (5, "DEU", "Deuteronomy", "OLD", 34),
    (6, "JOS", "Joshua", "OLD", 24),
    (7, "JDG", "Judges", "OLD", 21),
    (8, "RUT", "Ruth", "OLD", 4),
    (9, "1SA", "1 Samuel", "OLD", 31),
    (10, "2SA", "2 Samuel", "OLD", 24),
    (11, "1KI", "1 Kings", "OLD", 22),
    (12, "2KI", "2 Kings", "OLD", 25),
    (13, "1CH", "1 Chronicles", "OLD", 29),
    (14, "2CH", "2 Chronicles", "OLD", 36),
    (15, "EZR", "Ezra", "OLD", 10),
    (16, "NEH", "Nehemiah", "OLD", 13),
    (17, "EST", "Esther", "OLD", 10),
    (18, "JOB", "Job", "OLD", 42),
    (19, "PSA", "Psalms", "OLD", 150),
    (20, "PRO", "Proverbs", "OLD", 31),
    (21, "ECC", "Ecclesiastes", "OLD", 12),
    (22, "SNG", "Song of Solomon", "OLD", 8),
    (23, "ISA", "Isaiah", "OLD", 66),
    (24, "JER", "Jeremiah", "OLD", 52),
    (25, "LAM", "Lamentations", "OLD", 5),
    (26, "EZK", "Ezekiel", "OLD", 48),
    (27, "DAN", "Daniel", "OLD", 12),
    (28, "HOS", "Hosea", "OLD", 14),
    (29, "JOL", "Joel", "OLD", 3),
    (30, "AMO", "Amos", "OLD", 9),
    (31, "OBA", "Obadiah", "OLD", 1),
    (32, "JON", "Jonah", "OLD", 4),
    (33, "MIC", "Micah", "OLD", 7),
    (34, "NAM", "Nahum", "OLD", 3),
    (35, "HAB", "Habakkuk", "OLD", 3),
    (36, "ZEP", "Zephaniah", "OLD", 3),
    (37, "HAG", "Haggai", "OLD", 2),
    (38, "ZEC", "Zechariah", "OLD", 14),
    (39, "MAL", "Malachi", "OLD", 4),
    (40, "MAT", "Matthew", "NEW", 28),
    (41, "MRK", "Mark", "NEW", 16),
    (42, "LUK", "Luke", "NEW", 24),
    (43, "JHN", "John", "NEW", 21),
    (44, "ACT", "Acts", "NEW", 28),
    (45, "ROM", "Romans", "NEW", 16),
    (46, "1CO", "1 Corinthians", "NEW", 16),
    (47, "2CO", "2 Corinthians", "NEW", 13),
    (48, "GAL", "Galatians", "NEW", 6),
    (49, "EPH", "Ephesians", "NEW", 6),
    (50, "PHP", "Philippians", "NEW", 4),
    (51, "COL", "Colossians", "NEW", 4),
    (52, "1TH", "1 Thessalonians", "NEW", 5),
    (53, "2TH", "2 Thessalonians", "NEW", 3),
    (54, "1TI", "1 Timothy", "NEW", 6),
    (55, "2TI", "2 Timothy", "NEW", 4),
    (56, "TIT", "Titus", "NEW", 3),
    (57, "PHM", "Philemon", "NEW", 1),
    (58, "HEB", "Hebrews", "NEW", 13),
    (59, "JAS", "James", "NEW", 5),
    (60, "1PE", "1 Peter", "NEW", 5),
    (61, "2PE", "2 Peter", "NEW", 3),
    (62, "1JN", "1 John", "NEW", 5),
    (63, "2JN", "2 John", "NEW", 1),
    (64, "3JN", "3 John", "NEW", 1),
    (65, "JUD", "Jude", "NEW", 1),
    (66, "REV", "Revelation", "NEW", 22)
]

BOOK_NUM_TO_ID = {b[0]: b[1] for b in CANONICAL_BOOKS}

TRANSLATIONS = [
    ("KJV", "KJV", "King James Version", "en", "Authorized King James Version (1611/1769)", 1, "Public Domain"),
    ("SUV", "SUV", "Swahili Union Version", "sw", "Union Version (Biblia ya Kiswahili 1952)", 0, "Public Domain"),
    ("NIV", "NIV", "New International Version", "en", "New International Version", 0, "Biblica"),
    ("ESV", "ESV", "English Standard Version", "en", "English Standard Version", 0, "Crossway"),
    ("NKJV", "NKJV", "New King James Version", "en", "New King James Version", 0, "Thomas Nelson")
]

XML_SOURCES = [
    ("KJV", "/home/murugu/.gemini/antigravity/scratch/bibles/KJV_King_James_Version.xml"),
    ("SUV", "/home/murugu/.gemini/antigravity/scratch/bibles/Swahili_SUV_Union_Version.xml"),
    ("NIV", "/home/murugu/.gemini/antigravity/scratch/bibles/NIV_New_International_Version.xml"),
    ("ESV", "/home/murugu/.gemini/antigravity/scratch/bibles/ESV_English_Standard_Version.xml"),
    ("NKJV", "/home/murugu/.gemini/antigravity/scratch/bibles/NKJV_New_King_James_Version.xml"),
]

ROOM_IDENTITY_HASH = "53a741cee3e31f2fb267923e75eed394"

SCHEMA_STATEMENTS = [
    "CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `name` TEXT, `email` TEXT, `phone` TEXT, `role` TEXT, `campusId` TEXT, `avatarUrl` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `streakCount` INTEGER NOT NULL, `spiritualXp` INTEGER NOT NULL, `gracePoints` INTEGER NOT NULL, `streakFrozen` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `campuses` (`id` TEXT NOT NULL, `name` TEXT, `code` TEXT, `location` TEXT, `leaderName` TEXT, `phone` TEXT, `email` TEXT, `isMainCampus` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `sermons` (`id` TEXT NOT NULL, `title` TEXT, `preacher` TEXT, `series` TEXT, `passage` TEXT, `audioUrl` TEXT, `videoUrl` TEXT, `dateMillis` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `summary` TEXT, `notes` TEXT, `isDownloaded` INTEGER NOT NULL, `localAudioPath` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `hymns` (`id` TEXT NOT NULL, `number` INTEGER NOT NULL, `title` TEXT, `lyrics` TEXT, `chords` TEXT, `keySignature` TEXT, `timeSignature` TEXT, `category` TEXT, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `riddles` (`id` TEXT NOT NULL, `question` TEXT, `answer` TEXT, `explanation` TEXT, `scriptureRef` TEXT, `difficulty` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `jokes` (`id` TEXT NOT NULL, `text` TEXT, `cleanRating` TEXT, `category` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `events` (`id` TEXT NOT NULL, `title` TEXT, `description` TEXT, `startTimeMillis` INTEGER NOT NULL, `endTimeMillis` INTEGER NOT NULL, `location` TEXT, `campusId` TEXT, `imageUrl` TEXT, `category` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `announcements` (`id` TEXT NOT NULL, `title` TEXT, `body` TEXT, `priority` TEXT, `publishedMillis` INTEGER NOT NULL, `expiresMillis` INTEGER NOT NULL, `campusId` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `prayers` (`id` TEXT NOT NULL, `userId` TEXT, `authorName` TEXT, `title` TEXT, `content` TEXT, `isAnonymous` INTEGER NOT NULL, `isAnswered` INTEGER NOT NULL, `prayerCount` INTEGER NOT NULL, `createdMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `reflections` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT, `content` TEXT, `passageRef` TEXT, `createdMillis` INTEGER NOT NULL, `updatedMillis` INTEGER NOT NULL, `isPrivate` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `bible_translations` (`id` TEXT NOT NULL, `shortName` TEXT, `fullName` TEXT, `language` TEXT, `versionInfo` TEXT, `isDefault` INTEGER NOT NULL, `licenseInfo` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `bible_books` (`id` TEXT NOT NULL, `translationId` TEXT, `bookNumber` INTEGER NOT NULL, `name` TEXT, `testament` TEXT, `chapterCount` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `bible_chapters` (`id` TEXT NOT NULL, `bookId` TEXT, `chapterNumber` INTEGER NOT NULL, `verseCount` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `bible_verses` (`id` TEXT NOT NULL, `translationId` TEXT, `chapterId` TEXT, `bookNumber` INTEGER NOT NULL, `chapterNumber` INTEGER NOT NULL, `verseNumber` INTEGER NOT NULL, `text` TEXT, `isFavorite` INTEGER NOT NULL, `note` TEXT, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_bible_verses_translationId_bookNumber_chapterNumber_verseNumber` ON `bible_verses` (`translationId`, `bookNumber`, `chapterNumber`, `verseNumber`)",
    "CREATE INDEX IF NOT EXISTS `index_bible_verses_bookNumber_chapterNumber_verseNumber` ON `bible_verses` (`bookNumber`, `chapterNumber`, `verseNumber`)",
    "CREATE TABLE IF NOT EXISTS `conversations` (`id` TEXT NOT NULL, `title` TEXT, `type` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `lastMessageText` TEXT, `lastMessageMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `conversationId` TEXT, `senderId` TEXT, `senderName` TEXT, `content` TEXT, `sentMillis` INTEGER NOT NULL, `deliveryStatus` TEXT, `syncStatus` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `connections` (`id` TEXT NOT NULL, `userId` TEXT, `peerId` TEXT, `peerName` TEXT, `status` TEXT, `createdMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `notifications` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT, `body` TEXT, `type` TEXT, `actionTarget` TEXT, `isRead` INTEGER NOT NULL, `createdMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `sync_queue` (`id` TEXT NOT NULL, `operationType` TEXT, `entityType` TEXT, `entityId` TEXT, `payloadJson` TEXT, `timestampMillis` INTEGER NOT NULL, `status` TEXT, `retryCount` INTEGER NOT NULL, `lastError` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `sync_metadata` (`entityType` TEXT NOT NULL, `lastSyncTimestamp` INTEGER NOT NULL, `syncToken` TEXT, `status` TEXT, PRIMARY KEY(`entityType`))",
    "CREATE TABLE IF NOT EXISTS `downloaded_content` (`id` TEXT NOT NULL, `contentType` TEXT, `contentId` TEXT, `localUri` TEXT, `fileSizeBytes` INTEGER NOT NULL, `downloadMillis` INTEGER NOT NULL, `status` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
    f"INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '{ROOM_IDENTITY_HASH}')"
]

def main():
    target_db_path = "/home/murugu/Android/app/src/main/assets/database/perazim_database.db"
    if os.path.exists(target_db_path):
        os.remove(target_db_path)
    os.makedirs(os.path.dirname(target_db_path), exist_ok=True)

    print(f"Connecting to database: {target_db_path}")
    conn = sqlite3.connect(target_db_path)
    cur = conn.cursor()

    # Apply SQLite pragmas for bulk speed
    cur.execute("PRAGMA synchronous = OFF;")
    cur.execute("PRAGMA journal_mode = MEMORY;")

    print("Creating Room tables and indexes...")
    for stmt in SCHEMA_STATEMENTS:
        cur.execute(stmt)

    # 1. Insert Translations
    print(f"Seeding {len(TRANSLATIONS)} Bible Translations...")
    cur.executemany(
        "INSERT INTO bible_translations (id, shortName, fullName, language, versionInfo, isDefault, licenseInfo) VALUES (?, ?, ?, ?, ?, ?, ?)",
        TRANSLATIONS
    )

    # 2. Insert 66 Canonical Books
    print(f"Seeding {len(CANONICAL_BOOKS)} Canonical Books...")
    book_rows = [
        (b[1], "KJV", b[0], b[2], b[3], b[4])
        for b in CANONICAL_BOOKS
    ]
    cur.executemany(
        "INSERT INTO bible_books (id, translationId, bookNumber, name, testament, chapterCount) VALUES (?, ?, ?, ?, ?, ?)",
        book_rows
    )

    # 3. Parse XML Bibles & Collect Chapters + Verses
    chapter_verse_counts = {} # (bookId, chapterNumber) -> max_verse
    total_inserted_verses = 0

    for trans_id, xml_path in XML_SOURCES:
        if not os.path.exists(xml_path):
            print(f"ERROR: XML file missing: {xml_path}", file=sys.stderr)
            sys.exit(1)
        
        print(f"Parsing XML for {trans_id} from {os.path.basename(xml_path)}...")
        tree = ET.parse(xml_path)
        root = tree.getroot()
        
        verse_batch = []
        for testament in root.findall('testament'):
            for book in testament.findall('book'):
                b_num = int(book.attrib.get('number', '0'))
                b_id = BOOK_NUM_TO_ID.get(b_num, f"BK{b_num}")
                for chapter in book.findall('chapter'):
                    c_num = int(chapter.attrib.get('number', '0'))
                    v_count_for_ch = 0
                    for verse in chapter.findall('verse'):
                        v_num = int(verse.attrib.get('number', '0'))
                        v_text = (verse.text or '').strip()
                        v_count_for_ch += 1
                        
                        v_id = f"{trans_id}.{b_id}.{c_num}.{v_num}"
                        ch_id = f"{b_id}.{c_num}"
                        
                        verse_batch.append((
                            v_id,
                            trans_id,
                            ch_id,
                            b_num,
                            c_num,
                            v_num,
                            v_text,
                            0,
                            None
                        ))
                    
                    if (b_id, c_num) not in chapter_verse_counts or v_count_for_ch > chapter_verse_counts[(b_id, c_num)]:
                        chapter_verse_counts[(b_id, c_num)] = v_count_for_ch

        print(f"Inserting {len(verse_batch)} verses for {trans_id}...")
        cur.executemany(
            "INSERT INTO bible_verses (id, translationId, chapterId, bookNumber, chapterNumber, verseNumber, text, isFavorite, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            verse_batch
        )
        total_inserted_verses += len(verse_batch)

    # 4. Insert Chapters into bible_chapters
    print(f"Seeding {len(chapter_verse_counts)} Chapters into bible_chapters...")
    chapter_rows = [
        (f"{b_id}.{c_num}", b_id, c_num, v_count)
        for (b_id, c_num), v_count in sorted(chapter_verse_counts.items(), key=lambda x: (BOOK_NUM_TO_ID.get(x[0][0], 0), x[0][1]))
    ]
    cur.executemany(
        "INSERT INTO bible_chapters (id, bookId, chapterNumber, verseCount) VALUES (?, ?, ?, ?)",
        chapter_rows
    )

    # 5. Seed content tables from assets/seed/ if available
    seed_dir = "/home/murugu/Android/app/src/main/assets/seed"
    if os.path.exists(seed_dir):
        # Sermons
        sermons_file = os.path.join(seed_dir, "sermons.json")
        if os.path.exists(sermons_file):
            with open(sermons_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                s_rows = [
                    (s["id"], s.get("title"), s.get("preacher"), s.get("series"), s.get("passage"),
                     s.get("audioUrl"), s.get("videoUrl"), s.get("dateMillis", 0), s.get("durationSeconds", 0),
                     s.get("summary"), s.get("notes"), 1 if s.get("isDownloaded") else 0, s.get("localAudioPath"))
                    for s in data
                ]
                cur.executemany("INSERT OR IGNORE INTO sermons VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", s_rows)
                print(f"Seeded {len(s_rows)} sermons from assets/seed/sermons.json")

        # Hymns
        hymns_file = os.path.join(seed_dir, "hymns.json")
        if os.path.exists(hymns_file):
            with open(hymns_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                h_rows = [
                    (h["id"], h.get("number", 0), h.get("title"), h.get("lyrics"), h.get("chords"),
                     h.get("keySignature"), h.get("timeSignature"), h.get("category"), 1 if h.get("isFavorite") else 0)
                    for h in data
                ]
                cur.executemany("INSERT OR IGNORE INTO hymns VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", h_rows)
                print(f"Seeded {len(h_rows)} hymns from assets/seed/hymns.json")

        # Riddles
        riddles_file = os.path.join(seed_dir, "riddles.json")
        if os.path.exists(riddles_file):
            with open(riddles_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                r_rows = [
                    (r["id"], r.get("question"), r.get("answer"), r.get("explanation"),
                     r.get("scriptureRef"), r.get("difficulty"))
                    for r in data
                ]
                cur.executemany("INSERT OR IGNORE INTO riddles VALUES (?, ?, ?, ?, ?, ?)", r_rows)
                print(f"Seeded {len(r_rows)} riddles from assets/seed/riddles.json")

        # Jokes
        jokes_file = os.path.join(seed_dir, "jokes.json")
        if os.path.exists(jokes_file):
            with open(jokes_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                j_rows = [
                    (j["id"], j.get("text"), j.get("cleanRating"), j.get("category"))
                    for j in data
                ]
                cur.executemany("INSERT OR IGNORE INTO jokes VALUES (?, ?, ?, ?)", j_rows)
                print(f"Seeded {len(j_rows)} jokes from assets/seed/jokes.json")

        # Events
        events_file = os.path.join(seed_dir, "events.json")
        if os.path.exists(events_file):
            with open(events_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                e_rows = [
                    (e["id"], e.get("title"), e.get("description"), e.get("startTimeMillis", 0),
                     e.get("endTimeMillis", 0), e.get("location"), e.get("campusId"), e.get("imageUrl"),
                     e.get("category"))
                    for e in data
                ]
                cur.executemany("INSERT OR IGNORE INTO events VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", e_rows)
                print(f"Seeded {len(e_rows)} events from assets/seed/events.json")

        # Announcements
        ann_file = os.path.join(seed_dir, "announcements.json")
        if os.path.exists(ann_file):
            with open(ann_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                a_rows = [
                    (a["id"], a.get("title"), a.get("body"), a.get("priority"),
                     a.get("publishedMillis", 0), a.get("expiresMillis", 0), a.get("campusId"))
                    for a in data
                ]
                cur.executemany("INSERT OR IGNORE INTO announcements VALUES (?, ?, ?, ?, ?, ?, ?)", a_rows)
                print(f"Seeded {len(a_rows)} announcements from assets/seed/announcements.json")

        # Reflections
        ref_file = os.path.join(seed_dir, "reflections.json")
        if os.path.exists(ref_file):
            with open(ref_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                rf_rows = [
                    (rf["id"], rf.get("userId"), rf.get("title"), rf.get("content"),
                     rf.get("passageRef"), rf.get("createdMillis", 0), rf.get("updatedMillis", 0),
                     1 if rf.get("isPrivate") else 0)
                    for rf in data
                ]
                cur.executemany("INSERT OR IGNORE INTO reflections VALUES (?, ?, ?, ?, ?, ?, ?, ?)", rf_rows)
                print(f"Seeded {len(rf_rows)} reflections from assets/seed/reflections.json")

    conn.commit()

    # Optimize database with vacuum and analyze
    print("Optimizing SQLite database (VACUUM and ANALYZE)...")
    cur.execute("PRAGMA vacuum;")
    cur.execute("PRAGMA analyze;")

    # Verify counts
    print("\n--- Verification Gate ---")
    cur.execute("SELECT count(*) FROM bible_translations")
    t_count = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM bible_books")
    b_count = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM bible_chapters")
    c_count = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM bible_verses")
    v_count = cur.fetchone()[0]
    cur.execute("SELECT identity_hash FROM room_master_table WHERE id = 42")
    m_hash = cur.fetchone()[0]

    print(f"Translations count : {t_count}")
    print(f"Books count        : {b_count}")
    print(f"Chapters count     : {c_count}")
    print(f"Verses count       : {v_count}")
    print(f"Room identity hash : {m_hash}")

    for tid in ["KJV", "SUV", "NIV", "ESV", "NKJV"]:
        cur.execute("SELECT text FROM bible_verses WHERE translationId = ? AND bookNumber = 10 AND chapterNumber = 5 AND verseNumber = 20", (tid,))
        row = cur.fetchone()
        verse_text = row[0] if row else "NOT FOUND"
        print(f"  2 Samuel 5:20 [{tid}]: {verse_text[:75]}...")

    conn.close()

    db_size_mb = os.path.getsize(target_db_path) / (1024 * 1024)
    print(f"\nSUCCESS: Pre-populated database generated at {target_db_path} ({db_size_mb:.2f} MB)")

if __name__ == "__main__":
    main()
