"""Writes assets/games/memory_sets.json from memory_src.py. A candidate pair is kept only when both
words appear as whole words in the KJV verse it cites and each word is used once in the set,
so every card has exactly one partner. Sets with fewer than five pairs after filtering are left out and reported."""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
from memory_src import SETS  # noqa: E402
import kjv  # noqa: E402

MIN_PAIRS = 5  # Hard uses five pairs; sets with fewer are kept out of the bank.
OUT = os.path.join(HERE, "..", "..", "..", "src", "main", "assets", "games", "memory_sets.json")


def verse_has(word, reference):
    text = kjv.verse_text(reference).lower()
    return re.search(r"\b" + re.escape(word.lower()) + r"\b", text) is not None


def main():
    sets, short = [], []
    for i, (title, candidates) in enumerate(SETS, start=1):
        used, pairs = set(), []
        for a, b, ref in candidates:
            if a in used or b in used or a == b:
                continue
            if not (verse_has(a, ref) or verse_has(b, ref)):
                continue
            used.update([a, b])
            pairs.append({"a": a, "b": b, "reference": ref})
        if len(pairs) < MIN_PAIRS:
            short.append("%s (%d)" % (title, len(pairs)))
            continue
        sets.append({"id": "m%02d" % i, "title": title, "pairs": pairs})
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump({"translation": "KJV", "sets": sets}, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    print("sets:", len(sets), "pairs:", sum(len(s["pairs"]) for s in sets))
    if short:
        print("short of 5 pairs:", "; ".join(short))


if __name__ == "__main__":
    main()
