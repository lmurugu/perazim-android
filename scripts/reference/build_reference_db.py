#!/usr/bin/env python3
"""Build the offline reference database for the on-phone Perazim Guide.

Reads the raw downloads named in scripts/reference/sources.json (see fetch_reference_sources.py)
and writes one SQLite database with three kinds of data:

  * entries / entries_fts   dictionaries, topical index, confessions (FTS4 full-text search)
  * xrefs                   verse-to-verse cross-references (OpenBible.info, CC BY 4.0)
  * places                  Bible places with coordinates and passages (OpenBible.info)

Every row carries its source_id. The app decides which sources may be used (see
src/main/assets/reference/approvals.json). Presence in this database is not approval.

Usage:
    python3 scripts/reference/build_reference_db.py --raw <raw dir> --out <reference.db>
    python3 scripts/reference/build_reference_db.py --raw <raw dir> --out <reference.db> --only smith-bible-dictionary

Standard library only. Writes are atomic: the database is built beside the output and renamed on success.
"""

import argparse
import hashlib
import html
import json
import os
import re
import sqlite3
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
SOURCES_JSON = HERE / "sources.json"
SCHEMA_VERSION = "1"

# Standard OpenBible / SBL abbreviations to canonical book numbers (Genesis = 1 ... Revelation = 66).
BOOK_ABBREVIATIONS = [
    "Gen", "Exod", "Lev", "Num", "Deut", "Josh", "Judg", "Ruth", "1Sam", "2Sam", "1Kgs", "2Kgs",
    "1Chr", "2Chr", "Ezra", "Neh", "Esth", "Job", "Ps", "Prov", "Eccl", "Song", "Isa", "Jer", "Lam",
    "Ezek", "Dan", "Hos", "Joel", "Amos", "Obad", "Jonah", "Mic", "Nah", "Hab", "Zeph", "Hag", "Zech",
    "Mal", "Matt", "Mark", "Luke", "John", "Acts", "Rom", "1Cor", "2Cor", "Gal", "Eph", "Phil", "Col",
    "1Thess", "2Thess", "1Tim", "2Tim", "Titus", "Phlm", "Heb", "Jas", "1Pet", "2Pet", "1John", "2John",
    "3John", "Jude", "Rev",
]
BOOK_NUMBER = {abbr: i + 1 for i, abbr in enumerate(BOOK_ABBREVIATIONS)}
BOOK_NAMES = {i + 1: name for i, name in enumerate([
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
    "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah",
    "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah",
    "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah", "Nahum",
    "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi", "Matthew", "Mark", "Luke", "John", "Acts",
    "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians",
    "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews",
    "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation"])}

VERSE_RE = re.compile(r"^([1-3]?[A-Za-z]+)\.(\d{1,3})\.(\d{1,3})$")


def verse_key(book, chapter, verse):
    """Integer key that sorts in canonical order: BBCCCVVV-style with room for large chapters."""
    return book * 1_000_000 + chapter * 1000 + verse


def parse_openbible_ref(text):
    m = VERSE_RE.match(text.strip())
    if not m or m.group(1) not in BOOK_NUMBER:
        raise ValueError("Unrecognised verse reference: %r" % text)
    return verse_key(BOOK_NUMBER[m.group(1)], int(m.group(2)), int(m.group(3)))


def parse_openbible_xrefs(path):
    """Yield (from_key, to_start, to_end, votes) from the OpenBible.info cross-reference zip."""
    with zipfile.ZipFile(path) as archive:
        names = [n for n in archive.namelist() if n.endswith(".txt")]
        if len(names) != 1:
            raise ValueError("Expected one cross-reference text file in %s" % path)
        text = archive.read(names[0]).decode("utf-8")
    for line in text.splitlines():
        if not line or line.startswith("#") or line.startswith("From Verse"):
            continue
        parts = line.split("\t")
        if len(parts) != 3:
            continue
        source, target, votes = parts
        start_text, _, end_text = target.partition("-")
        from_key = parse_openbible_ref(source)
        to_start = parse_openbible_ref(start_text)
        to_end = parse_openbible_ref(end_text) if end_text else to_start
        yield from_key, to_start, to_end, int(votes)


def parse_openbible_geo(path):
    """Yield (name, lat, lon, passages, comment) rows from OpenBible.info merged.txt (tab separated)."""
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            if not line.strip() or line.startswith("#"):
                continue
            cols = line.rstrip("\n").split("\t")
            if len(cols) < 6:
                continue
            name, _root, lat, lon, passages, comment = cols[0], cols[1], cols[2], cols[3], cols[4], cols[5]
            if not name.strip():
                continue
            try:
                lat_v = float(lat) if lat.strip() else None
                lon_v = float(lon) if lon.strip() else None
            except ValueError:
                lat_v = lon_v = None
            yield name.strip(), lat_v, lon_v, passages.strip(), comment.strip()


_UNDERSCORE_RULE = re.compile(r"^\s*_{5,}\s*$")


_CCEL_FOOTER = re.compile(
    r"^\s*(References\s*$|This document is from the Christian|available for use under the Creative Commons"
    r"|\d+\.\s+(file:|https?:)|\[\d+\]\s*(\S*(http|file):|\d)|The cover art for this book)")


def ccel_body_lines(path):
    """Return the entry lines: after the CCEL title block, before the reference footer."""
    with open(path, encoding="utf-8", errors="replace") as handle:
        lines = handle.read().splitlines()
    rules = [i for i, line in enumerate(lines[:60]) if _UNDERSCORE_RULE.match(line)]
    start = rules[-1] + 1 if rules else 0
    end = len(lines)
    for i in range(start, len(lines)):
        if _CCEL_FOOTER.match(lines[i]):
            end = i
            break
    return lines[start:end]


def ccel_headword_entries(path):
    """Dictionary entries: a headword sits at exactly three spaces of indent, the body is indented further."""
    entries = []
    current = None
    body = []
    for line in ccel_body_lines(path):
        if not line.strip():
            continue
        indent = len(line) - len(line.lstrip(" "))
        if indent == 3:
            if current is not None:
                entries.append((current, " ".join(body)))
            current = line.strip()
            body = []
        elif current is not None:
            body.append(line.strip())
    if current is not None:
        entries.append((current, " ".join(body)))
    cleaned = []
    for headword, text in entries:
        text = re.sub(r"\s+", " ", text).strip()
        if len(headword) == 1 or not text:
            continue  # alphabet dividers such as "A", and index headings with no body
        cleaned.append((headword, text))
    return cleaned


def ccel_topical_entries(path):
    """Nave's Topical Bible: headings at three spaces; subtopics and references are the body."""
    entries = []
    current = None
    body = []
    for line in ccel_body_lines(path):
        if not line.strip():
            continue
        indent = len(line) - len(line.lstrip(" "))
        if indent == 3 and not line.strip().startswith(("-", ".")):
            if current is not None:
                entries.append((current, "\n".join(body)))
            current = line.strip()
            body = []
        elif current is not None:
            body.append(re.sub(r"\s+", " ", line.strip()))
    if current is not None:
        entries.append((current, "\n".join(body)))
    return [(h, t) for h, t in entries if len(h) > 1 and t.strip()]


SOURCE_SCHEMA = """
CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE sources(
    source_id TEXT PRIMARY KEY, title TEXT NOT NULL, author TEXT, edition TEXT, category TEXT NOT NULL,
    licence TEXT NOT NULL, licence_url TEXT, source_url TEXT, landing_url TEXT, attribution TEXT NOT NULL,
    theology_tag TEXT NOT NULL, tier TEXT NOT NULL, sha256 TEXT NOT NULL, bytes INTEGER NOT NULL,
    entry_count INTEGER NOT NULL DEFAULT 0);
CREATE TABLE entries(
    id INTEGER PRIMARY KEY, source_id TEXT NOT NULL, headword TEXT NOT NULL, location TEXT NOT NULL,
    body TEXT NOT NULL);
CREATE INDEX entries_source ON entries(source_id);
CREATE INDEX entries_headword ON entries(headword COLLATE NOCASE);
CREATE TABLE xrefs(
    from_key INTEGER NOT NULL, to_start INTEGER NOT NULL, to_end INTEGER NOT NULL, votes INTEGER NOT NULL,
    PRIMARY KEY (from_key, to_start, to_end)) WITHOUT ROWID;
CREATE TABLE places(
    id INTEGER PRIMARY KEY, name TEXT NOT NULL, lat REAL, lon REAL, passages TEXT NOT NULL, comment TEXT NOT NULL);
CREATE INDEX places_name ON places(name COLLATE NOCASE);
"""


def sha256_file(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_sources():
    with open(SOURCES_JSON, encoding="utf-8") as handle:
        return json.load(handle)["sources"]


def build(raw_dir, out_path, only=None):
    raw_dir = Path(raw_dir)
    out_path = Path(out_path)
    tmp_path = out_path.with_suffix(out_path.suffix + ".building")
    if tmp_path.exists():
        tmp_path.unlink()
    conn = sqlite3.connect(str(tmp_path))
    conn.executescript(SOURCE_SCHEMA)
    conn.execute("INSERT INTO meta(key, value) VALUES ('schema_version', ?)", (SCHEMA_VERSION,))
    report = []
    entry_id = 0
    for source in load_sources():
        sid = source["id"]
        if only and sid not in only:
            continue
        path = raw_dir / source["file"]
        if not path.exists():
            raise SystemExit("Missing raw file for %s: %s (run fetch_reference_sources.py)" % (sid, path))
        digest = sha256_file(path)
        parser = source["parser"]
        rows = 0
        if parser == "openbible_xrefs":
            conn.executemany("INSERT OR IGNORE INTO xrefs(from_key, to_start, to_end, votes) VALUES (?,?,?,?)",
                             parse_openbible_xrefs(path))
            rows = conn.execute("SELECT COUNT(*) FROM xrefs WHERE 1").fetchone()[0]
        elif parser == "openbible_geo":
            conn.executemany("INSERT INTO places(name, lat, lon, passages, comment) VALUES (?,?,?,?,?)",
                             parse_openbible_geo(path))
            rows = conn.execute("SELECT COUNT(*) FROM places").fetchone()[0]
        else:
            if parser == "ccel_headword":
                items = ccel_headword_entries(path)
            elif parser == "ccel_topical":
                items = ccel_topical_entries(path)
            else:
                raise SystemExit("Unknown parser %s for %s" % (parser, sid))
            for headword, body in items:
                entry_id += 1
                conn.execute("INSERT INTO entries(id, source_id, headword, location, body) VALUES (?,?,?,?,?)",
                             (entry_id, sid, headword, headword, body))
                rows += 1
        conn.execute(
            "INSERT INTO sources(source_id, title, author, edition, category, licence, licence_url, source_url, "
            "landing_url, attribution, theology_tag, tier, sha256, bytes, entry_count) "
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            (sid, source["title"], source.get("author"), source.get("edition"), source["category"],
             source["licence"], source.get("licenceUrl"), source.get("sourceUrl"), source.get("landingUrl"),
             source["attribution"], source["theologyTag"], source["tier"], digest,
             os.path.getsize(path), rows))
        report.append((sid, rows, os.path.getsize(path)))
    conn.commit()
    conn.execute("VACUUM")
    conn.close()
    os.replace(tmp_path, out_path)
    return report


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--raw", required=True, help="directory holding the downloaded raw files")
    parser.add_argument("--out", required=True, help="output reference.db path")
    parser.add_argument("--only", nargs="*", help="build only these source ids")
    args = parser.parse_args(argv)
    report = build(args.raw, args.out, set(args.only) if args.only else None)
    total = 0
    for sid, rows, size in report:
        print("%-28s rows=%-8d raw=%d bytes" % (sid, rows, size))
    out_size = os.path.getsize(args.out)
    print("reference.db: %d bytes (%.2f MB)" % (out_size, out_size / 1_000_000))
    return 0


if __name__ == "__main__":
    sys.exit(main())
