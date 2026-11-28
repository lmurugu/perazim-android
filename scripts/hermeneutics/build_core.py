#!/usr/bin/env python3
"""Build the core hermeneutics pack CANDIDATE from source files.

Usage: python3 build_core.py <repos_dir> <out_dir> [--release]

Decisions applied (Levi, 2026-11-27):
  Q1 A  Public domain and open only. Middle Liddell rows (Perseus, CC BY-SA) are excluded.
  Q2 B  Church Fathers: evidence-only channel. None are in this candidate.
  Q3 B  Jewish and background sources are excluded. None are in this candidate.
  Q4 A  KJV only for quotes.
  Q5 A  Storage: 100 MB total, 25 MB per optional pack.
  Q6 C  STEP TBESH included now. Online Bible permission is still pending.
  Q7 B  ESV-derived name forms kept (names.esv_form). STEP Bible check is still pending.

The two pending items are switched off by --release. The default build includes them and says so in
manifest.json and NOTICE.txt. Use --release for any public build unless both permissions are obtained.

Reads source files only. Writes core-candidate.sqlite, core-candidate.sqlite.gz, manifest.json and NOTICE.txt.
Not bundled into the app and not committed to Git.
"""
import csv, datetime, gzip, hashlib, html, json, re, shutil, sqlite3, sys, unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path

ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
if len(ARGS) != 2:
    sys.exit("usage: build_core.py <repos_dir> <out_dir> [--release]")
REPOS = Path(ARGS[0]).resolve()
OUT = Path(ARGS[1]).resolve()
RELEASE = "--release" in sys.argv
INCLUDE_TBESH = not RELEASE            # Q6 C: included now, Online Bible permission pending
INCLUDE_ESV_NAME_FORMS = not RELEASE   # Q7 B: kept now, STEP Bible check pending
OUT.mkdir(parents=True, exist_ok=True)

STEP = REPOS / "step-bible" / "src"
CC_BY_4 = "https://creativecommons.org/licenses/by/4.0/"
STEP_ATTR = "Data created by www.STEPBible.org based on work at Tyndale House Cambridge (CC BY 4.0)"

SOURCES = {
    "bdb-openscriptures": {
        "path": REPOS / "oshb-lexicon/src/BrownDriverBriggs.xml",
        "licence": "CC BY 4.0 (OpenScriptures Hebrew Lexicon data); BDB text public domain",
        "licence_url": CC_BY_4,
        "attribution": "Brown-Driver-Briggs Hebrew Lexicon, OpenScriptures Hebrew Lexicon project (CC BY 4.0 data)",
        "notes": "Primary Hebrew lexicon. Readme licence line still to be confirmed at release check.",
    },
    "step-tbesg": {
        "path": STEP / "Lexicons/TBESG - Translators Brief lexicon of Extended Strongs for Greek - STEPBible.org CC BY.txt",
        "licence": "CC BY 4.0 (STEP-Data)",
        "licence_url": CC_BY_4,
        "attribution": STEP_ATTR,
        "notes": "Rows marked (ML) (Middle Liddell, Perseus, CC BY-SA) are excluded under Q1 A. Rows marked (AS) depend on the Abbott-Smith release check. Unmarked rows were added by STEPBible.",
    },
    "step-tvtms": {
        "path": STEP / "Older Formats/TVTMS - till Mar 2025 KJV-based with old formats for subverses - STEPBible.org CC BY.txt",
        "licence": "CC BY 4.0 (STEP-Data)",
        "licence_url": CC_BY_4,
        "attribution": STEP_ATTR,
        "notes": "Verse references only. The ESV column holds reference numbers, not ESV text.",
    },
    "step-tipnr": {
        "path": STEP / "Proper Nouns/TIPNR - Translators Individualised Proper Names with all References - STEPBible.org CC BY.txt",
        "licence": "CC BY 4.0 (STEP-Data). Name forms in esv_form are ESV-derived: see pending items",
        "licence_url": CC_BY_4,
        "attribution": STEP_ATTR,
        "notes": ("PENDING (Q7 B): esv_form holds the ESV-derived English name for each entry. STEP Bible check not yet done. "
                  "Strong's numbers, references and significance are STEP text.") if INCLUDE_ESV_NAME_FORMS else
                 "esv_form not stored in this release build (Q7 B pending). Strong's numbers, references and significance only.",
    },
    "macula-greek-sblgnt": {
        "path": REPOS / "macula-greek/src/SBLGNT/tsv/macula-greek-SBLGNT.tsv",
        "licence": "CC BY 4.0 (MACULA LICENSE.md). Text is SBLGNT (CC BY 4.0, Faithlife). Glosses: Cherith CC BY 4.0; Berean Interlinear public domain",
        "licence_url": CC_BY_4,
        "attribution": "MACULA Greek, Clear Bible and Biblica (CC BY 4.0); SBL Greek New Testament, Logos Bible Software (CC BY 4.0); Cherith Glosses, Cherith Analytics (CC BY 4.0)",
        "notes": "TSV only. The MACULA sources/ folder is not used. Strong's numbers are stored without the G prefix, as in MACULA.",
    },
}
if INCLUDE_TBESH:
    SOURCES["step-tbesh"] = {
        "path": STEP / "Lexicons/TBESH - Translators Brief lexicon of Extended Strongs for Hebrew - STEPBible.org CC BY.txt",
        "licence": "CC BY 4.0 (STEP edits). Base text is Abridged BDB by Online Bible: permission PENDING",
        "licence_url": CC_BY_4,
        "attribution": STEP_ATTR + "; base lexicon Abridged BDB, Larry Pierce, OnlineBible.net (permission pending)",
        "notes": ("PENDING (Q6 C): the file says Online Bible permission must be obtained before these definitions are used "
                  "in any project. Included now as Levi decided. Removed by --release."),
    }

SCHEMA = """
CREATE TABLE sources(source_id TEXT PRIMARY KEY, licence TEXT NOT NULL, licence_url TEXT,
  attribution TEXT NOT NULL, input_file TEXT NOT NULL, input_sha256 TEXT NOT NULL,
  input_bytes INTEGER NOT NULL, notes TEXT);
CREATE TABLE lexicon_entries(source_id TEXT NOT NULL REFERENCES sources(source_id), lang TEXT NOT NULL,
  entry_id TEXT NOT NULL, lemma TEXT, translit TEXT, morph TEXT, gloss TEXT, definition TEXT);
CREATE TABLE verse_map(source_id TEXT NOT NULL REFERENCES sources(source_id), na TEXT, net2full TEXT,
  esv_ref TEXT, niv TEXT, nrsv TEXT, kjv TEXT);
CREATE TABLE names(source_id TEXT NOT NULL REFERENCES sources(source_id), strongs TEXT NOT NULL,
  first_ref TEXT, significance TEXT, esv_form TEXT);
CREATE TABLE greek_tokens(source_id TEXT NOT NULL REFERENCES sources(source_id), xmlid TEXT, ref TEXT,
  word_class TEXT, word_type TEXT, english TEXT, gloss TEXT, surface TEXT, after TEXT, lemma TEXT,
  normalized TEXT, strongs TEXT, morph TEXT);
CREATE INDEX lex_lemma ON lexicon_entries(lang, lemma);
CREATE INDEX lex_entry ON lexicon_entries(entry_id);
CREATE INDEX vm_kjv ON verse_map(kjv);
CREATE INDEX names_strongs ON names(strongs);
CREATE INDEX gt_ref ON greek_tokens(ref);
CREATE INDEX gt_strongs ON greek_tokens(strongs);
"""

def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()

def strip_html(s):
    s = re.sub(r"<[^>]+>", " ", s)
    return " ".join(html.unescape(s).split())

def text_no_status(el):
    parts = [el.text or ""]
    for ch in el:
        if not ch.tag.endswith("status"):
            parts.append(text_no_status(ch))
        parts.append(ch.tail or "")
    return "".join(parts)

def nfc_lemma_row(row):
    """Store lemmas in NFC. The sources mix canonically equivalent forms (e.g. U+1F71 and U+03AC), and lookups need one form."""
    lang, entry_id, lemma, *rest = row
    return (lang, entry_id, unicodedata.normalize("NFC", lemma) if lemma else lemma, *rest)

def bdb_rows(path):
    root = ET.parse(path).getroot()
    for el in root.iter():
        if not el.tag.endswith("entry") or not el.get("id"):
            continue
        w = el.find("{*}w")
        lemma = "".join(w.itertext()).strip() if w is not None else None
        definition = " ".join(text_no_status(el).split())
        yield ("heb", el.get("id"), lemma, None, None, None, definition)

STEP_LEX = {"G": "grk", "H": "heb"}

def step_lex_rows(path, prefix):
    """STEP lexicon rows. Columns: eStrong, dStrong =, uStrong, word, translit, morph, gloss, definition."""
    pat = re.compile(prefix + r"\d{4}\t")
    with open(path, encoding="utf-8-sig") as f:
        for line in f:
            if not pat.match(line) or "(ML)" in line:  # (ML) = Middle Liddell, Perseus, CC BY-SA: excluded
                continue
            cols = line.rstrip("\r\n").split("\t")
            if len(cols) < 8:
                continue
            entry_id = cols[1].split("=")[0].strip()
            yield (STEP_LEX[prefix], entry_id, cols[3].strip(), cols[4].strip(), cols[5].strip(),
                   cols[6].strip(), strip_html(cols[7]))

def count_ml(path, prefix):
    pat = re.compile(prefix + r"\d{4}\t")
    with open(path, encoding="utf-8-sig") as f:
        return sum(1 for line in f if pat.match(line) and "(ML)" in line)

VERSE = re.compile(r"^[1-3]?[A-Za-z]{2,4}\.\d+[:.]\d")

def tvtms_rows(path):
    with open(path, encoding="utf-8-sig") as f:
        for line in f:
            cols = [c.strip() for c in line.rstrip("\r\n").split("\t")]
            if len(cols) >= 6 and VERSE.match(cols[0]):
                yield tuple(cols[:6])

# Name before "@" is the ESV English form (STEP header). Kept only when INCLUDE_ESV_NAME_FORMS.
TIP = re.compile(r"^([^\t@]+)@([^\t=]+)=([HG]\d{4}[A-Z]?)\t")

def tipnr_rows(path):
    with open(path, encoding="utf-8-sig") as f:
        for line in f:
            m = TIP.match(line)
            if not m:
                continue
            cols = line.rstrip("\r\n").split("\t")
            sig = cols[1].strip() if len(cols) > 1 else ""
            esv_form = m.group(1).strip() if INCLUDE_ESV_NAME_FORMS else None
            yield (m.group(3), m.group(2).strip(), sig, esv_form)

def macula_rows(path):
    with open(path, encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f, delimiter="\t", quoting=csv.QUOTE_NONE):
            if not row.get("text"):
                continue
            yield (row["xml:id"], row["ref"], row["class"], row["type"], row["english"], row["gloss"],
                   row["text"], row["after"], row["lemma"], row["normalized"], row["strong"], row["morph"])

def main():
    db_path = OUT / "core-candidate.sqlite"
    if db_path.exists():
        db_path.unlink()
    con = sqlite3.connect(db_path)
    con.executescript(SCHEMA)

    src_rows = []
    for sid, meta in SOURCES.items():
        p = meta["path"]
        if not p.exists():
            sys.exit(f"missing input: {p}")
        row = (sid, meta["licence"], meta["licence_url"], meta["attribution"],
               str(p.relative_to(REPOS)), sha256(p), p.stat().st_size, meta["notes"])
        con.execute("INSERT INTO sources VALUES (?,?,?,?,?,?,?,?)", row)
        src_rows.append(row)

    p = SOURCES
    lex_sql = "INSERT INTO lexicon_entries VALUES (?,?,?,?,?,?,?,?)"
    con.executemany(lex_sql, (("bdb-openscriptures",) + nfc_lemma_row(r) for r in bdb_rows(p["bdb-openscriptures"]["path"])))
    con.executemany(lex_sql, (("step-tbesg",) + nfc_lemma_row(r) for r in step_lex_rows(p["step-tbesg"]["path"], "G")))
    if INCLUDE_TBESH:
        con.executemany(lex_sql, (("step-tbesh",) + nfc_lemma_row(r) for r in step_lex_rows(p["step-tbesh"]["path"], "H")))
    con.executemany("INSERT INTO verse_map VALUES (?,?,?,?,?,?,?)",
                    (("step-tvtms",) + r for r in tvtms_rows(p["step-tvtms"]["path"])))
    con.executemany("INSERT INTO names VALUES (?,?,?,?,?)",
                    (("step-tipnr",) + r for r in tipnr_rows(p["step-tipnr"]["path"])))
    con.executemany("INSERT INTO greek_tokens VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    (("macula-greek-sblgnt",) + r for r in macula_rows(p["macula-greek-sblgnt"]["path"])))
    con.commit()

    ml_excluded = {"step-tbesg": count_ml(p["step-tbesg"]["path"], "G")}
    counts = {t: con.execute(f"SELECT count(*) FROM {t}").fetchone()[0]
              for t in ["sources", "lexicon_entries", "verse_map", "names", "greek_tokens"]}
    per_source = {r[0]: r[1] for r in con.execute(
        "SELECT source_id, count(*) FROM (SELECT source_id FROM lexicon_entries UNION ALL SELECT source_id FROM verse_map "
        "UNION ALL SELECT source_id FROM names UNION ALL SELECT source_id FROM greek_tokens) GROUP BY source_id")}
    esv_forms = con.execute("SELECT count(esv_form) FROM names").fetchone()[0]
    print("BUILD", "RELEASE (pending items removed)" if RELEASE else "CANDIDATE (pending items included)")
    print("ROW COUNTS", json.dumps(counts))
    print("PER SOURCE", json.dumps(per_source))
    print("ESV NAME FORMS STORED", esv_forms)
    print("ML ROWS EXCLUDED", json.dumps(ml_excluded))

    print("LOOKUP G0026:", con.execute(
        "SELECT entry_id, gloss, substr(definition,1,100) FROM lexicon_entries WHERE entry_id='G0026'").fetchall())
    if INCLUDE_TBESH:
        print("LOOKUP H0001G (TBESH):", con.execute(
            "SELECT source_id, lemma, gloss, substr(definition,1,100) FROM lexicon_entries WHERE entry_id='H0001G'").fetchall())
    print("LOOKUP heb BDB lemma prefix 'אב':", con.execute(
        "SELECT entry_id, lemma FROM lexicon_entries WHERE source_id='bdb-openscriptures' AND lemma LIKE 'אב%' LIMIT 3").fetchall())
    print("LOOKUP names H0175:", con.execute(
        "SELECT strongs, first_ref, significance, esv_form FROM names WHERE strongs='H0175'").fetchall())
    print("LOOKUP verse_map Php.1:17:", con.execute(
        "SELECT na, net2full, esv_ref, niv, nrsv, kjv FROM verse_map WHERE kjv LIKE 'Php.1:17%' LIMIT 2").fetchall())
    print("LOOKUP MAT 1:1 tokens:", con.execute("SELECT count(*) FROM greek_tokens WHERE ref LIKE 'MAT 1:1!%'").fetchone())
    print("SAMPLE MAT 1:1 words:", con.execute(
        "SELECT surface, lemma, english FROM greek_tokens WHERE ref LIKE 'MAT 1:1!%' LIMIT 4").fetchall())
    print("ML LEAKAGE CHECK:", con.execute(
        "SELECT count(*) FROM lexicon_entries WHERE definition LIKE '%(ML)%' OR gloss LIKE '%(ML)%'").fetchone()[0])
    print("INTEGRITY:", con.execute("PRAGMA integrity_check").fetchone()[0])

    con.execute("VACUUM")
    con.close()

    gz_path = OUT / "core-candidate.sqlite.gz"
    with open(db_path, "rb") as src, gzip.open(gz_path, "wb", compresslevel=9) as dst:
        shutil.copyfileobj(src, dst)

    pending = []
    if INCLUDE_TBESH:
        pending.append({"item": "step-tbesh (Q6 C)", "needed": "Online Bible permission for the Abridged BDB base",
                        "owner": "Levi to decide; coordinator to request", "remove_with": "build with --release"})
    if INCLUDE_ESV_NAME_FORMS:
        pending.append({"item": "names.esv_form (Q7 B)", "needed": "STEP Bible confirmation that ESV-derived name forms may be used",
                        "owner": "Levi to decide; coordinator to request", "remove_with": "build with --release (or drop the column)"})
    decisions = {
        "Q1": "A. Public domain and open only. Middle Liddell rows excluded. SA and NC packs wait for separate approval.",
        "Q2": "B. Church Fathers: evidence-only channel. None in this candidate.",
        "Q3": "B. Jewish and background sources excluded. None in this candidate.",
        "Q4": "A. KJV only for quotes.",
        "Q5": "A. 100 MB total, 25 MB per optional pack.",
        "Q6": "C. STEP TBESH included now; Online Bible permission pending." if INCLUDE_TBESH
              else "C in the candidate. Removed in this release build (permission pending).",
        "Q7": "B. ESV-derived name forms kept in names.esv_form; STEP Bible check pending." if INCLUDE_ESV_NAME_FORMS
              else "B in the candidate. esv_form removed in this release build (STEP Bible check pending).",
    }
    manifest = {
        "pack": "core-hermeneutics",
        "version": "0.1-RELEASE-BUILD" if RELEASE else "0.1-CANDIDATE",
        "status": "NOT RELEASED. Not bundled, not in Git.",
        "built_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "builder": "build_core.py",
        "build_flags": {"release": RELEASE, "include_tbesh": INCLUDE_TBESH, "include_esv_name_forms": INCLUDE_ESV_NAME_FORMS},
        "decisions": decisions,
        "pending_before_public_release": pending,
        "conflict_flag": ("Standing rule: no ESV text is processed by AI. esv_form holds short ESV-derived name forms, "
                          "kept under Q7 B pending STEP Bible check.") if INCLUDE_ESV_NAME_FORMS else None,
        "excluded": {
            "esv": "ESV text is never processed by AI",
            "tbesg_middle_liddell_rows": f"{ml_excluded['step-tbesg']} rows marked (ML), Perseus CC BY-SA (Q1 A)",
            "step_tfbdb_totmm_tntmm_tbcwg_tagot": "not present in STEP-Data master; dropped from the plan",
        },
        "sources": [
            {"source_id": r[0], "licence": r[1], "licence_url": r[2], "attribution": r[3],
             "input_file": r[4], "input_sha256": r[5], "input_bytes": r[6], "notes": r[7]}
            for r in src_rows],
        "row_counts": counts,
        "per_source_rows": per_source,
        "esv_name_forms_stored": esv_forms,
        "sqlite_sha256": sha256(db_path),
        "sqlite_bytes": db_path.stat().st_size,
        "gzip_sha256": sha256(gz_path),
        "gzip_bytes": gz_path.stat().st_size,
    }
    (OUT / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")

    lines = [
        "NOTICE: core hermeneutics pack " + ("RELEASE BUILD" if RELEASE else "CANDIDATE 0.1 (not for release)"),
        "",
        "This pack contains data from the following sources, each under the licence shown.",
        "",
    ]
    for r in src_rows:
        lines += [f"- {r[0]}", f"  Licence: {r[1]}", f"  Attribution: {r[3]}", f"  Notes: {r[7]}", ""]
    lines += [
        "Excluded: ESV text (never used). Middle Liddell (Perseus, CC BY-SA) rows excluded under Q1 A: "
        f"{ml_excluded['step-tbesg']} TBESG rows.",
        "Church Fathers and Jewish or background sources: not in this pack (Q2 B, Q3 B).",
    ]
    if pending:
        lines += ["", "PENDING PERMISSIONS (remove before public release if not obtained):"]
        lines += [f"- {x['item']}: {x['needed']}. Remove with: {x['remove_with']}." for x in pending]
    lines += [
        "",
        "Open before release: Abbott-Smith base licence for the (AS) rows; BDB readme licence line; MACULA component licences; Kenyan rights review (not a legal opinion).",
    ]
    (OUT / "NOTICE.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("GZIP", gz_path, gz_path.stat().st_size, "bytes")
    print("SQLITE", db_path, db_path.stat().st_size, "bytes")

if __name__ == "__main__":
    main()
