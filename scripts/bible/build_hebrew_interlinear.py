#!/usr/bin/env python3
"""Build the Hebrew Old Testament interlinear asset from the Open Scriptures Hebrew Bible (OSHB).

Source: https://github.com/openscriptures/morphhb (wlc/*.xml, OSIS). Licence: the morphology and
lemma data are CC BY 4.0 (Open Scriptures Hebrew Bible Project). The WLC text is public domain.

Usage:
    python3 scripts/bible/build_hebrew_interlinear.py --src /path/to/morphhb/wlc \
        --out src/main/assets/bible/interlinear/hebrew_ot.tsv.gz

Output: gzip TSV, one verse per line:
    bookNumber<TAB>chapter<TAB>verse<TAB>word|strongs|morph word|strongs|morph ...
Prefix particles are joined to the word (the "/" in the source is removed). The Strong's number is
the lemma's main number (for example 7225 in lemma "b/7225"). Morph is the OSHB code, for example Ncfsa.
"""

import argparse
import gzip
import html
import re
import sys
from pathlib import Path

# OSIS book name -> canonical book number (OT 1-39).
OSIS_BOOKS = [
    "Gen", "Exod", "Lev", "Num", "Deut", "Josh", "Judg", "Ruth", "1Sam", "2Sam", "1Kgs", "2Kgs",
    "1Chr", "2Chr", "Ezra", "Neh", "Esth", "Job", "Ps", "Prov", "Eccl", "Song", "Isa", "Jer", "Lam",
    "Ezek", "Dan", "Hos", "Joel", "Amos", "Obad", "Jonah", "Mic", "Nah", "Hab", "Zeph", "Hag", "Zech", "Mal",
]
BOOK_NUMBER = {name: index + 1 for index, name in enumerate(OSIS_BOOKS)}

VERSE_START = re.compile(r'<verse osisID="([^"]+)"\s*/?>')
WORD = re.compile(r'<w ([^>]*)>([^<]*)</w>')
ATTR = re.compile(r'(\w+)="([^"]*)"')


def strongs_of(lemma):
    main = lemma.split("/")[-1]
    match = re.search(r"\d+", main)
    return int(match.group(0)) if match else 0


def parse_file(text):
    """Yield (book, chapter, verse, [cells]) for every verse in an OSIS book file."""
    positions = [(m.start(), m.group(1)) for m in VERSE_START.finditer(text)]
    for index, (start, osis_id) in enumerate(positions):
        end = positions[index + 1][0] if index + 1 < len(positions) else len(text)
        segment = text[start:end]
        book_name, chapter, verse = osis_id.split(".")[0], int(osis_id.split(".")[1]), int(osis_id.split(".")[2])
        cells = []
        for match in WORD.finditer(segment):
            attrs = dict(ATTR.findall(match.group(1)))
            surface = html.unescape(match.group(2)).replace("/", "").strip()
            if not surface:
                continue
            cells.append("%s|%d|%s" % (surface, strongs_of(attrs.get("lemma", "")), attrs.get("morph", "")))
        yield book_name, chapter, verse, cells


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--src", required=True, help="Directory containing the OSIS wlc/*.xml files")
    parser.add_argument("--out", required=True)
    args = parser.parse_args(argv)

    src = Path(args.src)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    verses = 0
    words = 0
    missing = [name for name in OSIS_BOOKS if not (src / (name + ".xml")).exists()]
    file_names = {"Gen": "Gen", "Exod": "Exod"}
    with gzip.open(out, "wt", encoding="utf-8", newline="\n") as handle:
        for name in OSIS_BOOKS:
            path = src / (name + ".xml")
            if not path.exists():
                # Some OSIS files use different names for the same book; fall back to the osisID prefix.
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            for book_name, chapter, verse, cells in parse_file(text):
                if book_name not in BOOK_NUMBER or not cells:
                    continue
                handle.write("%d\t%d\t%d\t%s\n" % (BOOK_NUMBER[book_name], chapter, verse, " ".join(cells)))
                verses += 1
                words += len(cells)
    print("verses=%d words=%d missing_files=%s -> %s" % (verses, words, ",".join(missing) or "none", out))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
