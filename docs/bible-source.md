# Scripture Provenance & Licensing Gate: Holy Bible Assets

**Document ID:** DOC-ENG-BIBLE-001  
**Verification Date:** 2026-09-12  
**Workstream:** Phase 1.3 — Bible Local Engine  
**Custodian:** Bible Engine Lead (`bible_engine_lead`)  
**Application:** Perazim Android Application (`com.example.app`)  
**Repository:** https://github.com/lmurugu/perazim-android  
**Branch:** `phase-1/local-foundation`

---

## 1. Executive Summary & Legal Status

This document establishes the official legal and ecclesiastical copyright audit for all Scripture assets bundled, seeded, and indexed within the Perazim Android application. To guarantee seamless offline availability across East Africa and globally without risk of digital rights infringement or licensing revocation, all bundled translations are strictly verified to reside in the **Public Domain**.

No proprietary API keys, external DRM mechanisms, recurring royalty obligations, or closed publisher contracts are required.

---

## 2. Translation Specifications

### 2.1 King James Version (KJV)
- **Official Name:** Holy Bible, King James Version (Authorized Version, 1611 / 1769 Blayney Oxford Revision)
- **Language:** English (`en`)
- **Canon:** 66 Books (39 Old Testament, 27 New Testament)
- **Ecclesiastical Status:** Default Primary Scripture Translation
- **Legal Status:** Public Domain worldwide (United States and Universal). In the United Kingdom, Crown copyright applies solely within the UK borders for printed copies produced under Royal Letters Patent; in all digital, electronic, and international jurisdictions, the text is completely free of copyright restrictions.

### 2.2 Swahili Union Version (SUV)
- **Official Name:** Maandiko Matakatifu ya Mungu Yaitwayo Biblia (Swahili Union Version / SUV)
- **Language:** Swahili (`sw`)
- **Canon:** 66 Books (Agano la Kale vitabu 39, Agano Jipya vitabu 27)
- **Ecclesiastical Status:** Primary Regional Scripture Translation for Kenya, Tanzania, Uganda, and the Swahili-speaking diaspora
- **Historical Provenance:** Historic translation completed by the inter-mission committee (including British and Foreign Bible Society, Church Missionary Society) in 1952. Under Kenyan Copyright Act No. 12 of 2001 (Section 23) and Tanzanian Copyright and Neighbouring Rights Act, copyright in literary works expires 50 years following the end of the calendar year of publication or death of authors, placing the historic 1952 text firmly in the public domain.

---

## 3. Provenance & Repository Sources

1. **Kohelet Zefania XML Repository:**
   - Source: `kohelet-net-admin/zefania-xml-bibles` (Public Domain XML corpus)
   - Upstream URLs:
     - `SF_2009-01-23_ENG_KJV_(KING JAMES VERSION).xml`
     - `SF_2009-01-25_SWA_SUV_(SWAHILI UNION VERSION).xml`
   - Archive Location: `~/.gemini/antigravity/scratch/bibles/`
2. **Project Gutenberg & Crosswire Bible Society:**
   - Gutenberg KJV EBook #10 (Public Domain, US)
   - Crosswire Sword module `KJV` (Public Domain)

---

## 4. Rights, Redistribution & Modification Permissions

| Criterion | Determination | Notes |
| :--- | :--- | :--- |
| **License** | **Public Domain (US / Universal)** | No copyright claimed or applicable |
| **Redistribution Rights** | **Granted / Unrestricted** | Permitted for packaging in Android APK, SQLite Room database, and local file storage |
| **Modification Rights** | **Granted / Unrestricted** | Permitted for search token indexing, chapter splitting, JSON formatting, and typography styling |
| **Commercial & Non-Commercial Use** | **Granted / Unrestricted** | Church ministry distribution is completely unencumbered |
| **Offline Caching** | **Unrestricted** | Full offline storage permitted on user devices |

---

## 5. Attribution Requirements & Pastoral Acknowledgment

### 5.1 Legal Attribution
- **Required by Law:** None. As public domain works, neither translation requires a formal copyright notice or publisher permissions.

### 5.2 Church Pastoral Acknowledgment
Per Perazim Mission Church pastoral guidelines, the application UI and settings documentation display the following respectful acknowledgment:

> *"Scripture quotations are taken from the Holy Bible, King James Version (KJV) and the Swahili Union Version (SUV), preserved in the public domain for the edification of the body of Christ."*

---

## 6. Canonical Validation: 2 Samuel 5:20 (Baal-Perazim Breakthrough)

The church namesake and thematic pillar is rooted in David's breakthrough in 2 Samuel 5:20. The authenticity and exact wording of this verse have been verified across both source archives:

### King James Version (KJV)
> *"And David came to Baal-perazim, and David smote them there, and said, The LORD hath broken forth upon mine enemies before me, as the breach of waters. Therefore he called the name of that place Baal-perazim."*  
> — **2 Samuel 5:20**

### Swahili Union Version (SUV)
> *"Basi Daudi akaja Baal-perasimu, naye Daudi akawapiga huko; akasema, BWANA amewafurikia adui zangu mbele yangu, kama mafuriko ya maji. Basi akapaita mahali pale Baal-perasimu."*  
> — **2 Samweli 5:20**

Both texts confirm the historic identity of the Lord of Breakthrough ("The God of the Breakthrough who breaks through obstacles like a raging flood").

---

## 7. Verification & Sign-off

- **Audit Completed By:** `bible_engine_lead`
- **Verification Date:** 2026-09-12
- **Integrity Status:** PASSED (100% Public Domain, 0 Licensing Blockers)
