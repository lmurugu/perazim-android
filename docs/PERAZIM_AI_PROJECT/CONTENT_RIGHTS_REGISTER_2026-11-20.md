# Content rights register (2026-11-20)

**Status: no blanket permission granted. Each item is classified; none is cleared for the knowledge package. This is a record, not legal advice.**

## 1. Bible translations

| Item | Translation / edition | Source in repo | Use in app today | Intended use (owner) | Permission status |
|---|---|---|---|---|---|
| KJV verses and starter chapters | King James Version (edition details in `kjv_manifest.json`; source and licence not recorded) | `src/main/assets/bible/` | Bible reader | Quotation and explanation | Public-domain status of the KJV text is commonly relied on, but this specific digital edition's source and licence are **not recorded**. Owner to confirm |
| ESV | English Standard Version | **Not in the repo** | Not in app | Owner's preferred translation for teaching and quotations | Requires publisher permission (Crossway) for use beyond the published terms. **Not established.** Owner or licensing team to confirm before any ESV text enters the package |
| NIV, NKJV | Listed in the remediation plan (D4), plan recommends removal | Not confirmed in assets | Unknown | None | Pending owner decision D4 |
| Swahili Union Version | Listed for confirmation of rights | Not confirmed in assets | Unknown | Swahili answers | Rights to confirm (D4) |

## 2. Hymns and worship material

Classification is by title and category only. Legal status depends on the authors' dates, translations and arrangements. None of these is cleared.

| # | Title (seed) | Category | Classification | Basis | Action |
|---|---|---|---|---|---|
| 1 | Holy, Holy, Holy! Lord God Almighty | Worship | Likely older text; check translation and arrangement | Title only | Verify source and publication |
| 2 | Great Is Thy Faithfulness | Faith | Possibly protected; verify | Title only | Permission required before use |
| 3 | Amazing Grace (How Sweet the Sound) | Grace | Likely older text; check arrangement | Title only | Verify |
| 4 | To God Be the Glory | Praise | Likely older text; check arrangement | Title only | Verify |
| 5 | How Great Thou Art | Worship | Possibly protected (English version); verify | Title only | Permission required before use |
| 6 | 'Tis So Sweet to Trust in Jesus | Faith | Likely older text; check arrangement | Title only | Verify |
| 7 | Grace Greater Than Our Sin | Grace | Possibly protected; verify | Title only | Permission required before use |
| 8 | Blessed Assurance, Jesus Is Mine | Praise | Likely older text; check arrangement | Title only | Verify |
| 9 | Rock of Ages, Cleft for Me | Grace | Likely older text; check arrangement | Title only | Verify |
| 10 | Praise to the Lord, the Almighty | Praise | Likely older text; check translation | Title only | Verify |
| 11 | Standing on the Promises of Christ My King | Faith | Likely older text; check arrangement | Title only | Verify |
| 12 | Crown Him with Many Crowns | Worship | Likely older text; check arrangement | Title only | Verify |

**Important:** the seed file also contains lyrics and chord arrangements. Chord sheets are often separately protected even when the words are old. Exclude lyrics and chords from the package until the owner confirms rights.

## 3. Sermons and teaching recordings

| # | Title | Preacher (as recorded) | Date | Source | Ownership / permission | Status |
|---|---|---|---|---|---|---|
| 1 | Supernatural Breakthrough | Bishop (named in seed) | Seed timestamp; verify | `sermons.json` | Not established | Excluded pending permission |
| 2 | The Baal-Perazim Mandate | Same | Seed timestamp; verify | same | Not established | Excluded |
| 3 | Walking in Faith and Favor | Same | same | same | Not established | Excluded |
| 4 | Overcoming Mountains | Same | same | same | Not established | Excluded |
| 5 | The Breakthrough Anointing | Same | same | same | Not established | Excluded |
| 6 | Possessing Your Possessions | Same | same | same | Not established | Excluded |

Also excluded: audio and video links in the seed file. Reproduction, transcription and AI ingestion each need permission. Preaching at Perazim does not by itself establish that.

## 4. Decisions for the owner
1. Confirm the KJV edition source and licence.
2. Decide whether ESV is licensed for this use, and by whom.
3. Decide D4 (NIV, NKJV, Swahili Union Version).
4. Decide, for each hymn, whether to clear it, exclude it or obtain permission.
5. Decide whether sermon material may be used, and provide written permission if so.

---

## 6. Research addendum (Session 4, 2026-11-20)

**Status: research record only. No rights are approved by this addendum. Nothing here is legal advice. Access dates are 2026-11-20.**

Classification key: **Verified** = read in the publisher's or rights holder's own text. **Apparent** = indicated by a secondary or partial source. **Uncertain** = not yet established. **Permission required** = the source's terms say written permission is needed for the use in question.

### 6.1 ESV (Crossway)

| Point | Finding | Class | Source (accessed 2026-11-20) |
|---|---|---|---|
| Quotation without written permission | Up to and including 500 verses (print, digital, audio). Not more than one-half of any one book; not 25% or more of the total text of the new work | Verified | https://www.crossway.org/rights-and-permissions/esv/ (same text at /support/esv-bible-permissions/) |
| Copyright notice | Required. Begins "Scripture quotations are from the ESV® Bible" and includes "ESV Text Edition: 2025" and "Used by permission. All rights reserved." | Verified | same |
| Audio and sermons | Quotes must be verbatim and credited "ESV". Sermons are listed among audio uses | Verified | same |
| Translation | Not permitted "in whole or in part into any other language" | Verified | same |
| AI | The text "may not be used, accessed, or processed by any artificial intelligence (AI) system." Covers training, fine-tuning, interpretation and AI-generated responses based on the text | Verified | same |
| Church, bulletins, classes | Bulletins and class use listed; no full notice needed in non-saleable media but "(ESV)" must follow the quotation | Verified | same |
| App or website use | ESV API free for websites and apps for non-commercial use; commercial app use and AI use fall outside the standard guidelines | Verified | same; API at https://api.esv.org/ |
| Storage in the app package | Not addressed directly for offline bundled text. Treated as **Permission required** | Uncertain | — |
| Trademark | "ESV" and "English Standard Version" are registered trademarks of Crossway | Verified | same |

**Consequence for the register:** the ESV's 500-verse allowance is the most restrictive limit found so far for a bundled offline app. Quoting in the offline AI answers, translation, and any AI processing each appear to need written permission. Keep ESV excluded from the package.

### 6.2 NASB (The Lockman Foundation)

| Point | Finding | Class | Source |
|---|---|---|---|
| Quotation without written permission | Up to 1,000 verses in any form (written, visual, electronic, audio). Not a complete book; not more than 50% of the work; not stored in an electronic retrieval system beyond 1,000 verses | Verified | https://www.lockman.org/permission-to-quote-copyright-trademark-information/ (page modified 2026-10-29) |
| Notice | Required. Full notice with © line and Lockman.org link. Web pages and apps must use the full notice with a clickable link | Verified | same |
| Electronic and app use | Site content for personal, noncommercial use. Redistribution needs written consent, except an insubstantial portion shared occasionally with attribution | Verified | same |
| AI | AI systems may quote up to 1,000 verses in a single response, scope limited to dynamic user queries (RAG and search). Training, fine-tuning, evaluating or developing AI models is **not** permitted without prior written permission. Each quote names the translation | Verified | same ("AI Usage & Attribution") |
| Translation | Page does not address translation. Trademarks need permission | Verified (silence) | same |
| Edition | Notice year rule: use only the last year of the edition quoted (for example, not 2020 for NASB 1995) | Verified | same |
| Contact | The Lockman Foundation, PO Box 2279, La Habra, CA 90632-2279, (714) 879-3055; request form at lockman.org. No fee stated | Verified | same |

**Consequence:** the app's existing rule (NASB only from a user-supplied file, kept out of git) remains correct. Local storage of a user's own file is a separate question. AI training on NASB text is not permitted without written permission. Use in offline answers would need to fit the RAG-limited, 1,000-verse rule and attribution.

### 6.3 KJV (Crown / Cambridge University Press)

| Point | Finding | Class | Source |
|---|---|---|---|
| Rights holder | "Rights in the Authorized (King James) Version of the Bible are vested in the Crown." Administered in the UK by the Crown's patentee, Cambridge University Press | Verified | https://assets.cambridge.org/97805211/63347/copyright/9780521163347_copyright_info.pdf and .../97805212/28626/copyright/9780521228626_copyright_info.pdf (both CUP copyright sheets, 2011 copyright year) |
| Liturgical or non-commercial educational use | Up to 500 verses (or less than a full book) needs no permission, per these two CUP sheets | Verified for these two editions | same |
| Other uses | Written permission from the Permissions Manager, Cambridge University Press | Verified | same |
| Edition in the repo | `kjv_manifest.json` does not record the source edition; the seed text is not matched to a CUP edition | Uncertain | repo |
| Public domain status | The text is often treated as public domain, but the CUP sheets describe Crown rights in the UK. Status outside the UK and US, and for a specific digital edition, is not established | Uncertain | — |
| Notice in the Reference System | Adapted reference notes carry "Copyright Zondervan. Used by permission." This covers the reference system, not the KJV text | Verified | same (2011 sheet) |

**Consequence:** Keep the KJV entry as "Verify edition source and licence" (already in section 1). Any app text should follow the CUP 500-verse rule only where its edition's sheet confirms it. Do not treat the KJV as cleared for the package.

### 6.4 Hymns and worship material

| Item | Finding | Class |
|---|---|---|
| Public domain classification by title | The search results so far (hymnary.org index pages) show many editions and arrangements but no rights statement for any seed title. Authorship dates and first-publication dates are not yet verified for each hymn | Uncertain |
| Great Is Thy Faithfulness, How Great Thou Art, Grace Greater Than Our Sin | Listed as "possibly protected" in section 2. Rights and publisher not confirmed in this pass | Permission required until verified |
| Lyrics and chord charts | Chord arrangements and modern arrangements are separately protected. Planning Center's copyright page (US-focused) states that sharing "audio, charts, or lyrics you do not have licenses for is illegal, even if you don't allow downloading" and names CCLI Church Copyright, Streaming and SongSelect licences as the routes | Verified for the US licence scheme; Kenya position not established |
| Church licensing in Kenya | Not established. The CCLI page at ccli.com blocked automated retrieval (robots.txt). Whether CCLI or another scheme covers Embu, Kenya is **Uncertain** | Uncertain |

Source for the licence summary: https://help.planningcenter.com/en/copyright-concerns.html (accessed 2026-11-20).

**Consequence:** keep all 12 hymns excluded from the package, including lyrics and chords, until each has a rights check (author death dates, publisher, arrangement) and any needed licence.

### 6.5 Sermons, teaching recordings and transcripts

| Point | Finding | Class | Source |
|---|---|---|---|
| Are sermons protected works? | Berne Convention Article 2bis(2) lets member states allow reproduction of lectures, addresses and similar works delivered in public, for informatory purposes, and reserves the author's right to make a collection (Art. 2bis(3)). The word "sermons" does not appear in Art. 2bis. Protection of spoken sermons is therefore a matter of national law | Verified (treaty text) | https://law.cornell.edu/treaties/berne/2bis.html (accessed 2026-11-20) |
| Kenya | The Copyright Act (Cap. 130, 2022 consolidation) is the current text. Its protected-works list, term and exceptions were **not read** in this pass. The WIPO Lex PDF link (ke045en_1.pdf) failed to fetch (URL too long for the proxy) | Uncertain | https://www.wipo.int/wipolex/en/legislation/details/23267 (page read) |
| Recordings and video | Reproduction, transcription and AI ingestion each need permission, per section 3 | Permission required | section 3 |
| Streaming licences | US-focused guidance says streamed services need a streaming licence for any music used | Verified for US; Kenya not established | Planning Center page, above |

**Consequence:** keep all sermons, audio, video and transcripts excluded. Ask the owner for written permission from each preacher, and check the Kenya Act text before any reliance on an exception.

### 6.6 Sources not yet checked

- Kenya Copyright Act (2022 consolidation) full text, sections on works, term, exceptions and collective management. Recheck at WIPO Lex or the Kenya Gazette.
- Copyright (Collective Management) Regulations, 2020 (Legal Notice 178 of 2020): https://www.wipo.int/wipolex/en/legislation/details/23416
- Copyright Regulations, 2020 (Legal Notice 177 of 2020): https://www.wipo.int/wipolex/en/legislation/details/23415
- CCLI coverage for Kenya or Africa: https://www.ccli.com/ (not retrieved)
- Authorship and death dates for each hymn, and the publisher of each arrangement.

### 6.7 Decisions for the owner (new)

1. Confirm whether the app needs ESV text at all. Its 500-verse limit and AI prohibition conflict with offline AI answers.
2. Decide whether to pursue Crossway's written permission, or to use another translation for AI answers.
3. Confirm whether NASB should be used in AI answers, given that AI training is not permitted and RAG use needs the attribution format.
4. Provide the KJV edition used in `kjv_manifest.json` so it can be matched to its publisher sheet.
5. Obtain the Kenya Copyright Act text from the Kenya Gazette or legal counsel so the sermon and hymn analysis can rely on Kenyan law.

### 6.8 KJV edition match attempt (added 2026-11-20)

| Check | Result | Class |
|---|---|---|
| `src/main/assets/bible/kjv_manifest.json` | Contains only book metadata (`bookNumber`, `id`, `name`, `testament`, `chapterCount`). No source, edition, publisher or licence field | Verified (file read) |
| `src/main/assets/bible/kjv_verses.json` and `text/KJV.tsv.gz` | Verse text only. No source or copyright field | Verified (file read) |
| `docs/bible-source.md` §2.1 | States "Authorized Version, 1611 / 1769 Blayney Oxford Revision" and "Public Domain worldwide (United States and Universal)". Says Crown copyright applies only to printed copies in the UK under Letters Patent, and that digital use is "completely free of copyright restrictions" | Repo assertion, not verified |
| `docs/bible-source.md` §3 (archive sources) | Cites an XML file named `SF_2009-01-23_ENG_KJV_(KING JAMES VERSION).xml` in a local scratch folder, plus Project Gutenberg eBook #10 and the CrossWire SWORD `KJV` module. The archive is outside the repo | Repo assertion; source not in repo |
| Match to a CUP sheet | **Not possible.** The two CUP sheets (ISBN 978-0-521-16334-7 and 978-0-521-22862-6) cover printed CUP editions. Neither addresses digital text. The repo's text is not tied to either ISBN | Uncertain |
| Digital-use claim | The CUP sheets set a 500-verse liturgical or educational allowance and require permission for other uses, but they do not say whether that covers digital copies. The repo's claim that digital use is free worldwide is therefore not confirmed by the CUP sheets | Uncertain |
| Kenya/SUV "Section 23, 50 years" claim (`docs/bible-source.md` §2.2) | Could not be checked. The Kenya Act full text was not retrieved (see 6.5) | Uncertain |

**Result:** the KJV text is probably derived from a public-domain base (Gutenberg / CrossWire / Blayney text), as the repo says. This has not been verified. The edition can't be matched to a CUP sheet from the files. Owner action: supply the archive file named in `docs/bible-source.md` §3 with its hash, so its provenance can be checked. Keep the KJV marked "verify before package" until then.

### 6.9 Source archives retrieved from the internet (added 2026-11-21)

Retrieved from the internet on 2026-11-21 to give the owner archives with hashes. These are **not** the repo's original `SF_2009` XML source (that file is outside the repo and was not obtained). They are candidate sources for a provenance check. Each hash is of the file as downloaded on 2026-11-21.

| Archive | URL | Size (bytes) | SHA-256 | What the file says about rights |
|---|---|---|---|---|
| CrossWire SWORD module `KJV` v3.1 | https://www.crosswire.org/ftpmirror/pub/sword/packages/rawzip/KJV.zip | 4,026,621 | `873815aa4b4123025616d1f41eae75f412111275f4c3884e36f92d4f46dcba1d` | `mods.d/kjv.conf` (About): "The rights to the base text are held by the Crown of England." `DistributionLicense=GPL`, which covers the module packaging, not the base text. Module history runs to 2023-07-27 |
| eBible.org `eng-kjv_html.zip` (generated 15 Nov 2026) | https://ebible.org/Scriptures/eng-kjv_html.zip | 4,356,434 | `a16325156cbc381e7c40ebd5872952ce4d9ed2cc84208f4034d13b6ad4650db3` | `copyright.htm` inside the archive labels the text "Public Domain". It states that letters patent mean printing in the UK, or importing printed copies into the UK, needs permission, currently held by CUP, OUP and Collins; the patent "has no effect outside of the UK." It cites no CUP sheet and does not address digital copies in the UK |
| Project Gutenberg eBook #10 | https://www.gutenberg.org/cache/epub/10/pg10.txt | — | **Not obtained.** Server returned HTTP 503 on all tested URLs | — |

**Checks made:**
- The CrossWire `About` text and the eBible `copyright.htm` text are both recorded above. Neither is a CUP sheet.
- Spot check: John 3:16 in the repo's `KJV.tsv.gz` matches standard KJV wording. A full verse-by-verse comparison against either archive has not been done.
- Kenya Copyright Act: the WIPO Lex record (https://www.wipo.int/wipolex/en/legislation/details/23267) lists the 2022 consolidation as the latest version (KE045). Its PDF link returned an HTML page, not the Act. Kenya Law pages returned 404 or an empty 202 response. **The Act text was not obtained.** The Kenya sections in 6.5 and the SUV "section 23, 50 years" claim in `docs/bible-source.md` remain uncertain.

**Status:** no rights approved. The UK letters-patent point is a publisher statement and is not a legal conclusion for the app's use.
