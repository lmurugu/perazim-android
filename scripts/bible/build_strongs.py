#!/usr/bin/env python3
"""Build the offline Strong's dictionary asset (Greek and Hebrew).

Sources and licences:
  * Greek: openscriptures/strongs greek/strongsgreek.dat. Strong's 1890 text is public domain;
    OpenScriptours' corrections are stated as GPL 3.0 in the file header (see SOURCES.txt).
  * Hebrew: openscriptures/HebrewLexicon HebrewStrong.xml, released under CC BY 4.0
    (the underlying Strong's Hebrew text is public domain).

Usage:
    python3 scripts/bible/build_strongs.py --greek strongsgreek.dat --hebrew HebrewStrong.xml \
        --out src/main/assets/bible/strongs/strongs.tsv.gz

Output: gzip TSV, one entry per line:
    code<TAB>original<TAB>transliteration<TAB>definition
where code is G1234 or H1234. Original is empty for Greek (the Greek letters come from the
interlinear). Definition is whitespace-collapsed text.
"""

import argparse
import gzip
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

HEADER = re.compile(r"^\s*(\d{1,5})\s+(\S+)\s+(\S+)\s*$")
NS = "{http://openscriptures.github.com/morphhb/namespace}"


def clean(text):
    return re.sub(r"\s+", " ", text or "").strip()


def greek_entries(path):
    blocks = path.read_text(encoding="utf-8", errors="replace").split("$$T")
    for block in blocks[1:]:
        lines = block.splitlines()
        header = None
        body = []
        for line in lines[1:]:
            if header is None and HEADER.match(line):
                header = HEADER.match(line)
                continue
            if header is not None:
                body.append(line)
        if header is None:
            continue
        number = int(header.group(1))
        translit = "%s (%s)" % (header.group(2), header.group(3))
        yield "G%d" % number, "", translit, clean(" ".join(body))


def hebrew_entries(path):
    root = ET.parse(path).getroot()
    for entry in root.iter(NS + "entry"):
        code = entry.get("id", "")
        if not code.startswith("H"):
            continue
        word = entry.find(NS + "w")
        original = clean(word.text) if word is not None else ""
        translit = clean(word.get("xlit")) if word is not None else ""
        parts = []
        for tag in ("source", "meaning", "usage"):
            node = entry.find(NS + tag)
            if node is not None:
                parts.append("".join(node.itertext()))
        yield code, original, translit, clean(" ".join(parts))


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--greek", required=True)
    parser.add_argument("--hebrew", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args(argv)

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    counts = {"G": 0, "H": 0}
    with gzip.open(out, "wt", encoding="utf-8", newline="\n") as handle:
        for code, original, translit, definition in list(greek_entries(Path(args.greek))) + list(hebrew_entries(Path(args.hebrew))):
            if not definition and not translit:
                continue
            counts[code[0]] += 1
            handle.write("%s\t%s\t%s\t%s\n" % (code, original, translit.replace("\t", " "), definition.replace("\t", " ")))
    print("greek=%d hebrew=%d -> %s" % (counts["G"], counts["H"], out))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
