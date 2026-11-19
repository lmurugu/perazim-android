#!/usr/bin/env python3
"""Add a transliteration and a short English gloss to every word of the bundled interlinears.

Inputs (already in the repository):
  * src/main/assets/bible/interlinear/greek_nt.tsv.gz   (word|strongs|morph cells)
  * src/main/assets/bible/interlinear/hebrew_ot.tsv.gz  (word|strongs|morph cells)
  * src/main/assets/bible/strongs/strongs.tsv.gz        (Strong's dictionary, public domain Greek;
                                                         Hebrew CC BY 4.0, Open Scriptures)

Output: the same files, one verse per line, cells of five fields:
    original|strongs|morph|transliteration|gloss
Spaces inside transliteration and gloss are written as "_" because cells are space-separated.

Transliteration is computed from the surface word with a fixed letter table below (no outside data).
The gloss is the first English rendering in the Strong's dictionary entry for the lemma.

Usage:
    python3 scripts/bible/enrich_interlinear.py --assets src/main/assets/bible
"""

import argparse
import gzip
import re
import sys
import unicodedata
from pathlib import Path

# ---------- Greek transliteration (surface form, accents removed) ----------
GREEK_SINGLE = {
    "α": "a", "β": "b", "γ": "g", "δ": "d", "ε": "e", "ζ": "z", "η": "ē", "θ": "th",
    "ι": "i", "κ": "k", "λ": "l", "μ": "m", "ν": "n", "ξ": "x", "ο": "o", "π": "p",
    "ρ": "r", "σ": "s", "ς": "s", "τ": "t", "υ": "u", "φ": "ph", "χ": "ch", "ψ": "ps", "ω": "ō",
}
GREEK_PAIRS = {"γγ": "ng", "γκ": "nk", "γξ": "nx", "γχ": "nch", "αι": "ai", "ει": "ei",
               "οι": "oi", "υι": "ui", "αυ": "au", "ευ": "eu", "ου": "ou", "ηυ": "ēu"}


def strip_marks(text):
    decomposed = unicodedata.normalize("NFD", text)
    return "".join(ch for ch in decomposed if unicodedata.category(ch) != "Mn"), decomposed


def translit_greek(word):
    base, decomposed = strip_marks(word.lower())
    out = []
    i = 0
    rough = "̔" in decomposed  # rough breathing mark (a leading h)
    while i < len(base):
        ch = base[i]
        if ch == "ρ" and i == 0:
            out.append("rh")
            i += 1
            continue
        pair = base[i:i + 2]
        if ch == "υ" and i == 0 and base[i:i + 2] == "υι":
            out.append("hui")
            i += 2
            continue
        if pair in GREEK_PAIRS and ch in "γαευοηυ":
            out.append(GREEK_PAIRS[pair])
            i += 2
            continue
        out.append(GREEK_SINGLE.get(ch, ch))
        i += 1
    text = "".join(out)
    if rough and text and not text.startswith("r"):
        text = "h" + text
    return text


# ---------- Hebrew transliteration (points kept; consonants and vowels mapped) ----------
HEB_CONSONANTS = {
    "א": "'", "ב": "v", "ג": "g", "ד": "d", "ה": "h", "ו": "v",
    "ז": "z", "ח": "ḥ", "ט": "ṭ", "י": "y", "כ": "kh", "ך": "kh",
    "ל": "l", "מ": "m", "ם": "m", "נ": "n", "ן": "n", "ס": "s",
    "ע": "'", "פ": "f", "ף": "f", "צ": "ts", "ץ": "ts", "ק": "q",
    "ר": "r", "ש": "sh", "ת": "t",
}
HEB_STOPS = {"ב": "b", "ג": "g", "ד": "d", "כ": "k", "פ": "p", "ת": "t"}
HEB_VOWELS = {
    "ְ": "e", "ֱ": "e", "ֲ": "a", "ֳ": "o", "ִ": "i", "ֵ": "e",
    "ֶ": "e", "ַ": "a", "ָ": "a", "ֹ": "o", "ֺ": "o", "ֻ": "u",
    "ׇ": "o",
}
HEB_SHIN_SIN = {"ׁ": "s", "ׂ": "s"}  # shin dot / sin dot


def translit_hebrew(word):
    out = []
    chars = list(word)
    for index, ch in enumerate(chars):
        if ch in HEB_CONSONANTS:
            dagesh = any(mark == "ּ" for mark in chars[index + 1:index + 3])
            if ch in HEB_STOPS and dagesh:
                out.append(HEB_STOPS[ch])
            elif ch == "ש":
                shin = "sh"
                for mark in chars[index + 1:index + 3]:
                    if mark in HEB_SHIN_SIN:
                        shin = HEB_SHIN_SIN[mark]
                out.append(shin)
            elif ch == "ו" and any(m == "ּ" for m in chars[index + 1:index + 2]):
                out.append("u")
            else:
                out.append(HEB_CONSONANTS[ch])
        elif ch in HEB_VOWELS:
            out.append(HEB_VOWELS[ch])
    text = "".join(out)
    # A final consonant without a vowel is spoken; a leading yod followed by a vowel is kept.
    return text.replace("__", "_")


# ---------- Glosses from the Strong's dictionary ----------
PAREN = re.compile(r"\([^)]*\)")


def gloss_from_definition(definition, fallback):
    text = definition or ""
    if ":--" in text:
        clause = text.split(":--", 1)[1]
        clause = re.split(r"[,;.]", clause)[0]
    else:
        clause = text.split(";", 1)[1] if ";" in text else text
        clause = PAREN.sub("", clause)
        clause = re.split(r"[,;.:]", clause)[0]
    clause = PAREN.sub("", clause).strip(" -×")
    clause = clause.replace("×", "").strip()
    clause = re.sub(r"^(to|the|a|an|of)\s+", "", clause.strip(), flags=re.IGNORECASE)
    words = clause.split()
    if not words or clause.lower().startswith("see "):
        return fallback
    word = words[0].strip(" ,.;:×")
    if word.lower() in {"in", "from", "an", "a", "the", "properly", "of", "and", "by", "with"} and len(words) > 1:
        word = words[1].strip(" ,.;:×")
    return word


def load_strongs(path):
    table = {}
    with gzip.open(path, "rt", encoding="utf-8") as handle:
        for line in handle:
            parts = line.rstrip("\n").split("\t", 3)
            if len(parts) != 4:
                continue
            code, _original, translit, definition = parts
            name = translit.split(" (", 1)[0]
            table[code] = (name, definition)
    return table


def clean_cell(text):
    return re.sub(r"[\s|]+", "_", text.strip()) or "-"


def enrich(path, strongs, language, out_lines):
    lines = []
    with gzip.open(path, "rt", encoding="utf-8") as handle:
        for line in handle:
            parts = line.rstrip("\n").split("\t", 3)
            if len(parts) != 4:
                lines.append(line)
                continue
            book, chapter, verse, cells = parts
            new_cells = []
            for cell in cells.split(" "):
                fields = cell.split("|")
                if len(fields) != 3:
                    continue
                surface, strongs_no, morph = fields
                code = ("G" if language == "greek" else "H") + str(int(strongs_no or 0)) if strongs_no and strongs_no != "0" else ""
                name, definition = strongs.get(code, ("", ""))
                if language == "greek":
                    translit = translit_greek(surface)
                else:
                    translit = translit_hebrew(surface)
                gloss = gloss_from_definition(definition, name)
                new_cells.append("|".join([surface, strongs_no, morph,
                                           clean_cell(translit), clean_cell(gloss)]))
            lines.append("%s\t%s\t%s\t%s\n" % (book, chapter, verse, " ".join(new_cells)))
    return lines


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--assets", required=True, help="Path to src/main/assets/bible")
    args = parser.parse_args(argv)
    base = Path(args.assets)
    strongs = load_strongs(base / "strongs" / "strongs.tsv.gz")
    for name, language in (("greek_nt", "greek"), ("hebrew_ot", "hebrew")):
        path = base / "interlinear" / (name + ".tsv.gz")
        lines = enrich(path, strongs, language, [])
        with gzip.open(path, "wt", encoding="utf-8", newline="\n") as handle:
            handle.writelines(lines)
        print("%s: %d verses -> %s" % (name, len(lines), path))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
