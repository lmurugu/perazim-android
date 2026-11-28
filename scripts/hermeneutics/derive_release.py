#!/usr/bin/env python3
"""Derive the RELEASE lexicon pack from a candidate built by build_core.py.

Usage: python3 derive_release.py <candidate.sqlite> <out_dir>

Applies the same removals as `build_core.py --release`, so it can run where the source
repos are not available:
  - removes the step-tbesh rows and its sources row (Online Bible permission pending, Q6)
  - sets names.esv_form to NULL (ESV-derived name forms pending a STEP Bible check, Q7)
Then checks that every lemma is NFC and writes, into out_dir:
  lexicon-core.db, lexicon-core.db.gz (deterministic gzip), lexicon-core.manifest.json,
  lexicon-core.NOTICE.txt.
Nothing is bundled into the app or committed by this script; copy the outputs deliberately.
"""
import gzip
import hashlib
import json
import shutil
import sqlite3
import sys
import unicodedata
from datetime import datetime, timezone
from pathlib import Path

RELEASE_DROP_SOURCES = ["step-tbesh"]
TABLES = ["sources", "lexicon_entries", "verse_map", "names", "greek_tokens"]


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    candidate, out = Path(sys.argv[1]), Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    db = out / "lexicon-core.db"
    shutil.copyfile(candidate, db)

    con = sqlite3.connect(db)
    with con:
        for sid in RELEASE_DROP_SOURCES:
            con.execute("DELETE FROM lexicon_entries WHERE source_id = ?", (sid,))
            con.execute("DELETE FROM sources WHERE source_id = ?", (sid,))
        con.execute("UPDATE names SET esv_form = NULL")
    con.execute("VACUUM")

    # Checks: release composition, NFC lemmas, no step-tbesh anywhere.
    counts = {t: con.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0] for t in TABLES}
    per_source = {
        t: dict(con.execute(f"SELECT source_id, COUNT(*) FROM {t} GROUP BY source_id").fetchall())
        for t in ["lexicon_entries", "verse_map", "names", "greek_tokens"]
    }
    non_nfc = sum(1 for (lemma,) in con.execute("SELECT lemma FROM lexicon_entries WHERE lemma IS NOT NULL")
                  if lemma != unicodedata.normalize("NFC", lemma))
    esv_forms = con.execute("SELECT COUNT(*) FROM names WHERE esv_form IS NOT NULL").fetchone()[0]
    tbesh_rows = con.execute("SELECT COUNT(*) FROM lexicon_entries WHERE source_id = 'step-tbesh'").fetchone()[0]
    sources = [
        {"source_id": sid, "licence": lic, "attribution": attr, "notes": notes}
        for sid, lic, attr, notes in con.execute(
            "SELECT source_id, licence, attribution, notes FROM sources ORDER BY source_id")
    ]
    con.close()

    problems = []
    if non_nfc:
        problems.append(f"{non_nfc} non-NFC lemmas")
    if esv_forms:
        problems.append(f"{esv_forms} ESV name forms still present")
    if tbesh_rows or any(s["source_id"] == "step-tbesh" for s in sources):
        problems.append("step-tbesh still present")
    if problems:
        sys.exit("release checks failed: " + "; ".join(problems))

    gz = out / "lexicon-core.db.gz"
    data = db.read_bytes()
    gz.write_bytes(gzip.compress(data, compresslevel=9, mtime=0))

    manifest = {
        "pack": "core-hermeneutics",
        "version": "0.1-RELEASE-APP",
        "status": ("Packaged in the app as a release-only composition. TBESH and ESV-derived name forms removed. "
                   "Generative AI stays off in release builds unless gate L7 passes (AGENTS.md)."),
        "built_at_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "builder": "derive_release.py (applies build_core.py --release removals to a candidate)",
        "input_candidate_sqlite_sha256": sha256(candidate),
        "build_flags": {"release": True, "include_tbesh": False, "include_esv_name_forms": False},
        "decisions": {
            "Q1": "A. Public domain and open only. Middle Liddell rows excluded.",
            "Q2": "B. Church Fathers: evidence-only channel. None in this pack.",
            "Q3": "B. Jewish and background sources excluded. None in this pack.",
            "Q4": "A. KJV only for quotes. Lexicon cards are evidence, never quoted as Scripture.",
            "Q5": "A. 100 MB total, 25 MB per optional pack.",
            "Q6": "C in candidate; release removes TBESH until Online Bible permission is confirmed.",
            "Q7": "B in candidate; release removes ESV-derived name forms until STEP Bible is checked.",
        },
        "pending_before_public_release": [
            "Online Bible permission for the Abridged BDB base of TBESH (Q6)",
            "STEP Bible confirmation for ESV-derived name forms (Q7)",
            "Levi's confirmation of lexicon source ids in approvals.json",
            "Generative AI stays off in release until gate L7 passes",
        ],
        "excluded": {
            "step_tbesh": "removed in release (Q6, Online Bible permission pending)",
            "esv_name_forms": "names.esv_form set to NULL in release (Q7, STEP Bible check pending)",
            "tbesg_middle_liddell_rows": "excluded at candidate build (Q1 A, Perseus CC BY-SA)",
        },
        "sources": sources,
        "row_counts": counts,
        "per_source_rows": per_source,
        "esv_name_forms_stored": esv_forms,
        "lemma_normalisation": "NFC on lexicon_entries.lemma (checked: 0 non-NFC).",
        "sqlite_sha256": sha256(db),
        "sqlite_bytes": db.stat().st_size,
        "gzip_sha256": sha256(gz),
        "gzip_bytes": gz.stat().st_size,
    }
    (out / "lexicon-core.manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
                                                    encoding="utf-8")

    lines = [
        "NOTICE: core hermeneutics pack RELEASE BUILD (lexicon, packaged in the app)",
        "",
        "This pack contains data from the following sources, each under the licence shown.",
        "",
    ]
    for s in sources:
        lines += [f"- {s['source_id']}", f"  Licence: {s['licence']}",
                  f"  Attribution: {s['attribution']}", f"  Notes: {s['notes']}", ""]
    lines += [
        "Not in this build: step-tbesh (Online Bible permission pending), ESV-derived name forms (STEP Bible check",
        "pending), Middle Liddell rows (Perseus, CC BY-SA). Generative AI is not enabled by this file.",
        "",
    ]
    (out / "lexicon-core.NOTICE.txt").write_text("\n".join(lines), encoding="utf-8")

    print("rows:", counts)
    print("sources:", [s["source_id"] for s in sources])
    print("sqlite:", manifest["sqlite_bytes"], manifest["sqlite_sha256"])
    print("gzip:  ", manifest["gzip_bytes"], manifest["gzip_sha256"])


if __name__ == "__main__":
    main()
