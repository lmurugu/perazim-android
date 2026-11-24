#!/usr/bin/env python3
"""Convert public-domain USFM Bible archives into the compact text assets Perazim bundles.

Usage:
    python3 scripts/bible/build_bible_text.py --out src/main/assets/bible/text \
        KJV=/path/eng-kjv_usfm.zip WEB=/path/eng-web_usfm.zip \
        ASV=/path/eng-asv_usfm.zip BSB=/path/engbsb_usfm.zip

Each output file is gzip-compressed TSV with one verse per line:
    bookNumber<TAB>chapter<TAB>verse<TAB>text

Only the 66 canonical books are kept. Footnotes, cross-references, headings and
Strong's attributes are removed so that only the verse text is bundled.
The script fails if a translation's verse count differs from the count expected
for it (see EXPECTED_VERSES).
"""

import argparse
import gzip
import re
import sys
import zipfile
from pathlib import Path

# Canonical order; USFM book codes match the codes used by BibleDataSeeder.
CANONICAL_USFM = [
    "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA",
    "1KI", "2KI", "1CH", "2CH", "EZR", "NEH", "EST", "JOB", "PSA", "PRO",
    "ECC", "SNG", "ISA", "JER", "LAM", "EZK", "DAN", "HOS", "JOL", "AMO",
    "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL",
    "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH",
    "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS",
    "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV",
]
BOOK_NUMBER = {code: index + 1 for index, code in enumerate(CANONICAL_USFM)}
# Verse counts as published by each source. Modern texts omit disputed verses
# (for example Matthew 17:21 and John 5:4) and some use other verse numbering,
# so the counts differ from the 31,102 of the KJV versification.
EXPECTED_VERSES = {"KJV": 31102, "WEB": 31098, "ASV": 31086, "BSB": 31086}

FOOTNOTE = re.compile(r"\\f .*?\\f\*", re.S)
XREF = re.compile(r"\\x .*?\\x\*", re.S)
# Word-level markers: \w word|Strong's attributes\w* (and the nested \+w form inside \wj).
WORD_MARKER = re.compile(r"\\\+?w ([^|\\]*)(?:\|[^\\]*)?\\\+?w\*")
MARKER = re.compile(r"\\\+?[a-zA-Z0-9]+\*?")
HEADING_PREFIXES = ("\\s", "\\r", "\\d", "\\mt", "\\h", "\\toc", "\\ms", "\\mr", "\\cl", "\\cp", "\\ip", "\\iot", "\\io", "\\imt", "\\is")


def clean_verse_text(raw):
    text = FOOTNOTE.sub("", raw)
    text = XREF.sub("", text)
    text = WORD_MARKER.sub(r"\1", text)
    text = MARKER.sub(" ", text)
    # Paragraph marks (KJV pilcrows) are layout, not scripture text.
    text = text.replace("\u00b6", " ")
    text = re.sub(r"\s+", " ", text).strip()
    # Some sources put a space before punctuation (for example "LORD ." or "art ,"): remove it,
    # and remove the space some sources leave after an opening bracket or quote.
    text = re.sub(r"\s+([,.;:!?)\]\u201d\u2019])", r"\1", text)
    text = re.sub(r"([(\[\u201c\u2018])\s+", r"\1", text)
    return text


def parse_usfm(lines):
    """Return (book_code, chapter, verse, raw_text) tuples for every verse in a USFM book."""
    state = {"book": None, "chapter": 0, "verse": None, "buffer": []}
    records = []

    def flush():
        if state["book"] is not None and state["verse"] is not None:
            records.append((state["book"], state["chapter"], state["verse"], " ".join(state["buffer"])))
        state["verse"] = None
        state["buffer"] = []

    def consume(piece):
        if piece.startswith("\\id "):
            flush()
            state["book"] = piece[4:7].upper()
            state["chapter"] = 0
        elif state["book"] is None:
            return
        elif piece.startswith("\\c "):
            flush()
            state["chapter"] = int(re.match(r"\\c (\d+)", piece).group(1))
        elif piece.startswith("\\v "):
            flush()
            match = re.match(r"\\v (\d+)(?:-\d+)?\s?(.*)", piece)
            state["verse"] = int(match.group(1))
            state["buffer"] = [match.group(2)]
        elif piece.startswith(HEADING_PREFIXES):
            return
        elif state["verse"] is not None and piece:
            state["buffer"].append(piece)

    for raw_line in lines:
        stripped = raw_line.strip()
        if not stripped:
            continue
        if stripped.startswith("\\id ") or stripped.startswith(HEADING_PREFIXES):
            consume(stripped)
            continue
        # Chapter and verse markers may follow a paragraph marker on the same line,
        # so split before each marker and process the pieces in order.
        for piece in re.split(r"(?=\\[cv] )", stripped):
            piece = piece.strip()
            if piece:
                consume(piece)
    flush()
    return records


def convert_archive(zip_path):
    rows = []
    with zipfile.ZipFile(zip_path) as archive:
        usfm_names = sorted(n for n in archive.namelist() if n.lower().endswith(".usfm"))
        for name in usfm_names:
            with archive.open(name) as handle:
                text_lines = handle.read().decode("utf-8-sig").splitlines()
            # Remove footnotes that span lines before line-based parsing.
            joined = "\n".join(text_lines)
            joined = FOOTNOTE.sub("", joined)
            joined = XREF.sub("", joined)
            for code, chapter, verse, raw in parse_usfm(joined.splitlines()):
                if code not in BOOK_NUMBER:
                    continue
                text = clean_verse_text(raw)
                if not text:
                    continue
                rows.append((BOOK_NUMBER[code], chapter, verse, text))
    rows.sort()
    return rows


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", required=True, help="Output directory for <ID>.tsv.gz files")
    parser.add_argument("sources", nargs="+", help="ID=path/to/usfm.zip pairs")
    args = parser.parse_args(argv)

    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    failures = []
    for pair in args.sources:
        if "=" not in pair:
            failures.append("bad source pair: " + pair)
            continue
        translation_id, zip_path = pair.split("=", 1)
        rows = convert_archive(zip_path)
        expected = EXPECTED_VERSES.get(translation_id)
        if expected is None:
            failures.append("no expected verse count for " + translation_id)
            continue
        if len(rows) != expected:
            failures.append("%s has %d verses, expected %d" % (translation_id, len(rows), expected))
            continue
        target = out_dir / (translation_id + ".tsv.gz")
        with gzip.open(target, "wt", encoding="utf-8", newline="\n") as handle:
            for book_number, chapter, verse, text in rows:
                handle.write("%d\t%d\t%d\t%s\n" % (book_number, chapter, verse, text))
        print("%s: %d verses -> %s" % (translation_id, len(rows), target))
    if failures:
        for failure in failures:
            print("FAILED: " + failure, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
