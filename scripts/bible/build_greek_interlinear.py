#!/usr/bin/env python3
"""Build the Greek New Testament interlinear asset from Robinson's parsed Antoniades text.

Source: https://github.com/byztxt/greektext-antoniades (parsed/*.UAN), public domain per
the repository README ("Public Domain. Copy freely.").

Usage:
    python3 scripts/bible/build_greek_interlinear.py --src /path/to/greektext-antoniades/parsed \
        --out src/main/assets/bible/interlinear/greek_nt.tsv.gz

Output: gzip TSV, one verse per line:
    bookNumber<TAB>chapter<TAB>verse<TAB>word|strongs|morph word|strongs|morph ...
Greek words are written in Unicode without accents, as found in the parsed source.
Words in square brackets (doubtful readings) keep their letters; "<<" ">>" markers are removed.
"""

import argparse
import gzip
import re
import sys
from pathlib import Path

# File stem -> canonical book number (NT books are 40-66).
BOOK_FILES = {
    "MT": 40, "MR": 41, "LU": 42, "JOH": 43, "AC": 44, "RO": 45, "1CO": 46, "2CO": 47,
    "GA": 48, "EPH": 49, "PHP": 50, "COL": 51, "1TH": 52, "2TH": 53, "1TI": 54, "2TI": 55,
    "TIT": 56, "PHM": 57, "HEB": 58, "JAS": 59, "1PE": 60, "2PE": 61, "1JO": 62, "2JO": 63,
    "3JO": 64, "JUDE": 65, "RE": 66,
}

# Beta-code style transliteration used by the parsed files -> Greek letters.
BETA = {
    "a": "α", "b": "β", "g": "γ", "d": "δ", "e": "ε", "z": "ζ", "h": "η", "q": "θ",
    "i": "ι", "k": "κ", "l": "λ", "m": "μ", "n": "ν", "c": "χ", "o": "ο", "p": "π",
    "r": "ρ", "s": "σ", "t": "τ", "u": "υ", "f": "φ", "x": "ξ", "y": "ψ", "w": "ω",
    "v": "ς",
}

VERSE_REF = re.compile(r"^\d+:\d+$")
STRONGS = re.compile(r"^\d+$")
MORPH = re.compile(r"^\{[^}]*\}$")
WORD = re.compile(r"^[A-Za-z]+$")


def to_greek(token):
    out = []
    for ch in token.lower():
        out.append(BETA.get(ch, ch))
    greek = "".join(out)
    # A final sigma is written as ς at the end of a word.
    if greek.endswith("σ"):
        greek = greek[:-1] + "ς"
    return greek


def clean_token(token):
    return token.replace("[", "").replace("]", "").replace("<<", "").replace(">>", "")


def parse_book(text):
    """Yield (chapter, verse, [word tuples]) for each verse in a parsed book file."""
    verses = []
    current = None
    words = []
    for raw in text.split():
        if VERSE_REF.match(raw):
            if current is not None:
                verses.append((current[0], current[1], words))
            chapter, verse = raw.split(":")
            current = (int(chapter), int(verse))
            words = []
            continue
        if current is None:
            continue
        if MORPH.match(raw):
            if words:
                greek, strongs, _ = words[-1]
                words[-1] = (greek, strongs, raw[1:-1])
            continue
        if STRONGS.match(raw):
            # The first number after a word is its Strong's lemma; a second number is a sub-code.
            if words and words[-1][1] is None:
                greek, _, morph = words[-1]
                words[-1] = (greek, int(raw), morph)
            continue
        cleaned = clean_token(raw)
        if WORD.match(cleaned):
            words.append((to_greek(cleaned), None, None))
    if current is not None:
        verses.append((current[0], current[1], words))
    return verses


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--src", required=True, help="Directory containing the parsed *.UAN files")
    parser.add_argument("--out", required=True, help="Output .tsv.gz path")
    args = parser.parse_args(argv)

    src = Path(args.src)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    total_verses = 0
    total_words = 0
    missing = []
    with gzip.open(out, "wt", encoding="utf-8", newline="\n") as handle:
        for stem, book in sorted(BOOK_FILES.items(), key=lambda item: item[1]):
            path = src / (stem + ".UAN")
            if not path.exists():
                missing.append(stem)
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            for chapter, verse, words in parse_book(text):
                if not words:
                    continue
                cells = []
                for greek, strongs, morph in words:
                    cells.append("%s|%s|%s" % (greek, "" if strongs is None else strongs, morph or ""))
                handle.write("%d\t%d\t%d\t%s\n" % (book, chapter, verse, " ".join(cells)))
                total_verses += 1
                total_words += len(words)
    if missing:
        print("missing source files: " + ", ".join(missing), file=sys.stderr)
        return 1
    print("verses=%d words=%d -> %s" % (total_verses, total_words, out))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
