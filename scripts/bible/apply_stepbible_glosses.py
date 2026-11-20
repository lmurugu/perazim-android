#!/usr/bin/env python3
"""Replace the English gloss of each interlinear word with the contextual gloss from STEPBible.

Source: STEPBible.org Translators Amalgamated data (Tyndale House, Cambridge), CC BY 4.0:
  * TAHOT (Hebrew Old Testament), files "TAHOT *.txt"
  * TAGNT (Greek New Testament), files "TAGNT *.txt"
  https://github.com/STEPBible/STEPBible-Data  (attribute Tyndale House and STEPBible.org)

Only the English gloss and the Strong's number are read from the STEPBible files. Their text is not copied
into the repository. Words are aligned to the bundled cells by Strong's number, so a verse whose word split
differs (for example Hebrew prefixes) keeps the existing gloss for unmatched words.

Usage:
    python3 scripts/bible/apply_stepbible_glosses.py --assets src/main/assets/bible --stepbible DIR
"""

import argparse
import difflib
import glob
import gzip
import os
import re
import sys
from collections import defaultdict
from pathlib import Path

OT = ("Gen Exo Lev Num Deu Jos Jdg Rut 1Sa 2Sa 1Ki 2Ki 1Ch 2Ch Ezr Neh Est Job Psa Pro Ecc Sng Isa Jer Lam "
      "Ezk Dan Hos Jol Amo Oba Jon Mic Nam Hab Zep Hag Zec Mal").split()
NT = ("Mat Mrk Luk Jhn Act Rom 1Co 2Co Gal Eph Php Col 1Th 2Th 1Ti 2Ti Tit Phm Heb Jas 1Pe 2Pe 1Jn 2Jn "
      "3Jn Jud Rev").split()
REF = re.compile(r"^([A-Za-z0-9]+)\.(\d+)\.(\d+)#(\d+)")
SKIP = {"the", "a", "an", "of", "to", "in", "and", "by", "with", "from", "at", "on", "for",
        "will", "shall", "would", "should", "can", "could", "may", "might", "must", "do", "does", "did"}
PRONOUNS = {"he", "she", "they", "it", "i", "we", "you", "his", "her", "its", "their", "him", "them", "us", "me"}
PLAIN = re.compile(r"^[a-z][a-z'-]*$")


def contextual_gloss(text):
    """First content word of the translators' gloss, with brackets and articles removed.

    Pronouns are used only when the gloss has no other word, so "he created" gives "created".
    """
    text = re.sub(r"\[[^\]]*\]", " ", text or "")
    text = re.sub(r"\([^)]*\)", " ", text)
    pronoun = ""
    for part in re.split(r"[/\s]+", text):
        word = part.strip(".,;:!?")
        lower = word.lower()
        if not word or lower in SKIP or not PLAIN.match(lower):
            continue
        if lower in PRONOUNS:
            pronoun = pronoun or lower
            continue
        return word
    return pronoun


# Reading fixes where the translators' context gloss is wrong for a very common word. Keyed by Strong's
# number; applied to every occurrence. Checked against standard English readings of Gen 1:1, John 1:1 and
# Psalm 23:1; the ESV and NASB text itself is not bundled.
GREEK_OVERRIDES = {1722: "in", 4314: "with"}  # en (ἐν), pros (πρὸς)
HEBREW_OVERRIDES = {7462: ("", "shepherd")}  # ro'eh (רֹעֶה), shepherd/tend


def sanitize_cell(cell):
    """Glosses that are a single letter, contain a digit or are a stray placeholder show a dash instead."""
    fields = cell.split("|")
    if len(fields) == 5:
        gloss = fields[4]
        if len(gloss) < 2 or re.search(r"[0-9()\[\]{}]", gloss) or gloss.lower() in {"x", "properly"}:
            fields[4] = "-"
    return "|".join(fields)


def strongs_of(field):
    numbers = re.findall(r"[GH](\d+)", field)
    return int(numbers[-1]) if numbers else 0


def read_stepbible(directory, pattern):
    rows = defaultdict(list)
    for path in glob.glob(os.path.join(directory, pattern)):
        with open(path, encoding="utf-8") as handle:
            for line in handle:
                match = REF.match(line)
                if match:
                    cols = line.rstrip("\n").split("\t")
                    rows[(match.group(1), int(match.group(2)), int(match.group(3)))].append(cols)
    return rows


def apply(path, step_rows, books, hebrew):
    out_lines = []
    changed = 0
    with gzip.open(path, "rt", encoding="utf-8") as handle:
        for line in handle:
            book, chapter, verse, cells = line.rstrip("\n").split("\t", 3)
            number = int(book)
            name = books[number - 1] if hebrew else books[number - 40]
            key = (name, int(chapter), int(verse))
            cell_list = cells.split(" ")
            rows = step_rows.get(key, [])
            if rows:
                ours = [int(c.split("|")[1] or 0) for c in cell_list]
                if hebrew:
                    theirs = [strongs_of(r[4]) for r in rows]
                    glosses = [contextual_gloss(r[3]) for r in rows]
                else:
                    theirs = [strongs_of(r[3]) for r in rows]
                    glosses = [contextual_gloss(r[2]) for r in rows]
                matcher = difflib.SequenceMatcher(a=ours, b=theirs, autojunk=False)
                for block in matcher.get_matching_blocks():
                    for offset in range(block.size):
                        i = block.a + offset
                        gloss = glosses[block.b + offset]
                        fields = cell_list[i].split("|")
                        if not gloss or len(fields) != 5:
                            continue
                        fields[4] = gloss
                        cell_list[i] = "|".join(fields)
                        changed += 1
                for i, c in enumerate(cell_list):
                    f = c.split("|")
                    if len(f) != 5:
                        continue
                    number = int(f[1] or 0)
                    if not hebrew and number in GREEK_OVERRIDES:
                        f[4] = GREEK_OVERRIDES[number]
                    elif hebrew and number in HEBREW_OVERRIDES and f[2].startswith(HEBREW_OVERRIDES[number][0]):
                        f[4] = HEBREW_OVERRIDES[number][1]
                    cell_list[i] = "|".join(f)
            cell_list = [sanitize_cell(c) for c in cell_list]
            out_lines.append("%s\t%s\t%s\t%s\n" % (book, chapter, verse, " ".join(cell_list)))
    return out_lines, changed


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--assets", required=True)
    parser.add_argument("--stepbible", required=True, help="Folder with the TAHOT and TAGNT .txt files")
    args = parser.parse_args(argv)
    base = Path(args.assets) / "interlinear"
    jobs = (
        ("hebrew_ot", read_stepbible(args.stepbible, "TAHOT*"), OT, True),
        ("greek_nt", read_stepbible(args.stepbible, "TAGNT*"), NT, False),
    )
    for name, rows, books, hebrew in jobs:
        path = base / (name + ".tsv.gz")
        lines, changed = apply(path, rows, books, hebrew)
        with gzip.open(path, "wt", encoding="utf-8", newline="\n") as handle:
            handle.writelines(lines)
        print("%s: %d words given a STEPBible gloss" % (name, changed))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
