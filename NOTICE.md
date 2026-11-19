# Notices and third-party licences

The **source code** in this repository is licensed under the MIT License (see `LICENSE`). Content assets are covered separately below. A row marked **pending** has not been verified and must be resolved before public release (plan gate L3).

## Bible texts (bundled in `src/main/assets/database/perazim_database.db`)

| Translation | Status in this repository | Action |
|---|---|---|
| King James Version (KJV) | Public domain; documented in `docs/bible-source.md` §2.1 | None |
| World English Bible (WEB), American Standard Version (ASV, 1901), Berean Standard Bible (BSB) | Public domain (eBible.org statements; BSB dedicated to the public domain 30 April 2023). Full text bundled in `src/main/assets/bible/text/`, see `SOURCES.txt` there | Owner to confirm with decision D4 |
| Greek New Testament interlinear (Antoniades 1904/1912 with parsing and Strong's numbers) | Public domain (byztxt/greektext-antoniades README). Asset in `src/main/assets/bible/interlinear/`, see `SOURCES.txt` there | None beyond attribution |
| Strong's Greek and Hebrew dictionary (offline lookup). Hebrew entries: CC BY 4.0 (Open Scriptures Hebrew Bible Project), Strong's text public domain. Greek entries: Strong's 1890 text public domain; OpenScriptures corrections stated as GPL 3.0 in the source header, which also quotes MIT text. **Owner to confirm the Greek correction licence** | `src/main/assets/bible/strongs/`, see `SOURCES.txt` there | Owner confirmation pending |
| New American Standard Bible (NASB) | Copyright The Lockman Foundation. **Not in the repository.** Loaded only from a local file the owner supplies (`Android/data/org.perazimchurch.app/files/bible/NASB.tsv.gz`); ignored by `.gitignore` | Owner licence required before any distribution |
| Swahili Union Version (SUV, 1952) | Documented as public domain in `docs/bible-source.md` §2.2; the owner has not yet confirmed it with the Bible Society of Kenya | **Pending** owner confirmation (decision D4) |
| New International Version (NIV) | Copyrighted (Biblica). Bundled in the database | **Pending removal** (decision D4) |
| English Standard Version (ESV) | Copyrighted (Crossway). Bundled in the database | **Pending removal** (decision D4) |
| New King James Version (NKJV) | Copyrighted (Thomas Nelson). Bundled in the database | **Pending removal** (decision D4) |

## Vendored and bundled code

| Component | Location | Licence |
|---|---|---|
| Nayuki QR Code generator (Java) | `src/main/java/org/perazimchurch/app/community/qr/qrcodegen/` | MIT (Project Nayuki), notice in each file |
| llama.cpp native libraries | `src/main/jniLibs/arm64-v8a/` (`libllama*.so`, `libggml*.so`) | MIT (llama.cpp), licence text in `src/main/assets/licenses/llama.cpp-LICENSE.txt` |
| `libperazim_llama.so` | `src/main/jniLibs/arm64-v8a/` | SHA-256 `149d13ab4c8a6890ad1a974394b4dfd3b4ed58d787fe29fdc5f2b30c6c68656b` (4592 bytes). **Pending** owner-supplied source revision and build recipe; see audit F-18 |

## Other content assets

| Asset | Location | Status |
|---|---|---|
| Hymn texts and chord charts | `src/main/assets/database/`, `docs/CONTENT_GUIDE.md` | **Pending** rights confirmation |
| Audio | `src/main/assets/audio/` | **Pending** rights confirmation |
| Background images and vignettes | `src/main/assets/backgrounds/`, `vignettes/` | **Pending** rights confirmation |
| Leadership portrait | `src/main/assets/bishop_portrait.jpg` | **Pending** consent and rights confirmation |
| Default avatars | `src/main/assets/default_avatars/` | **Pending** rights confirmation |

## Build-time dependencies

Dependencies are listed in `build.gradle` and `server/package.json`. Their licences are the licences of the upstream projects.
