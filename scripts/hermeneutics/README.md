# Core hermeneutics lexicon pack

`build_core.py` builds the lexicon pack that the offline assistant uses for word meanings
(`LexiconGuide`, `LexiconIndex`). It reads source files from a checkout of each resource and writes a SQLite
database, a gzip copy, `manifest.json` and `NOTICE.txt`. It does not download anything and does not write into the app.

## Sources

| Source id | Use in the app | Licence (see the manifest) |
|---|---|---|
| `step-tbesg` | Greek word meanings (lexicon) | CC BY 4.0 (STEP-Data); `(ML)` rows excluded (Perseus, CC BY-SA) |
| `step-tbesh` | Hebrew word meanings (lexicon) | CC BY 4.0 edits; Online Bible permission **pending** |
| `bdb-openscriptures` | Hebrew lexicon by lemma | CC BY 4.0 data; BDB text public domain |
| `step-tvtms` | Verse references (not used by retrieval yet) | CC BY 4.0 |
| `step-tipnr` | Names table (ESV name forms: **pending** STEP Bible check; never shown to the model) | CC BY 4.0 |
| `macula-greek-sblgnt` | Greek token table (not used by retrieval yet) | CC BY 4.0; glosses Cherith CC BY 4.0, Berean PD |

Only the three lexicon ids are approved in `assets/reference/approvals.json` (`bdb-openscriptures`,
`step-tbesg`, `step-tbesh`). The names and token tables are packaged but never read by the app.

## Building

```
python3 scripts/hermeneutics/build_core.py <repos_dir> <out_dir>            # candidate: includes TBESH and ESV name forms
python3 scripts/hermeneutics/build_core.py <repos_dir> <out_dir> --release  # public release: both pending items removed
```

`<repos_dir>` holds `step-bible/`, `oshb-lexicon/` and `macula-greek/` checkouts. The input SHA-256 values
are in `manifest.json`, which is the pin for the source files.

## Installing into the app

The current pack is the candidate build (decisions Q1 to Q7 of 2026-11-27 and Levi's approval of 2026-11-28).
It is in `src/main/assets/reference/`:

- `lexicon-core.db.gz` (gzip of the release pack: `derive_release.py` on a candidate, or `build_core.py --release`)
- `lexicon-core.manifest.json`
- `lexicon-core.NOTICE.txt`

For a public release:

1. Build with `--release` (or, without the source repos, run `python3 scripts/hermeneutics/derive_release.py <candidate.sqlite> <out_dir>` on a candidate) and copy the three release outputs over the assets above.
2. Remove `step-tbesh` from `approvedSourceIds` in `approvals.json` (its rows are absent in the release build anyway).
3. Bump `UNPACKED_NAME` in `LexiconIndex.java` (for example `lexicon-core-v2.db`), so phones unpack the new copy.
4. Obtain the Online Bible permission and STEP Bible's confirmation for the ESV name forms, or keep them out.
5. Keep generative AI off in release builds unless gate L7 passes (AGENTS.md).

## Rules the code keeps

- Lexicon cards are evidence for word meaning only. They are never quoted as Scripture and are dropped for doctrine
  questions (`OnDeviceAi` runs `LexiconGuide` only when `!doctrineQuestion`).
- Each card is at most 300 characters, labelled `LEXICAL_REFERENCE`, and names its source and licence.
- Scripture evidence comes first in the evidence array. Lexicon cards are added after it, at most two per question.
- KJV is the only Bible text quoted.
