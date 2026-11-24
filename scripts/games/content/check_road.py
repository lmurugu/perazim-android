"""Checks road_src.py: every reference parses and exists in the bundled KJV, the answer appears in
the verse (whole word, for words longer than three letters), the four options are distinct and
contain the answer, and no question repeats. Prints each verse so a person can read it."""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
sys.path.insert(0, HERE)
import kjv  # noqa: E402
import road_src  # noqa: E402


def main():
    errors, seen, n = [], set(), 0
    for tier, rows in (("EASY", road_src.EASY), ("MEDIUM", road_src.MEDIUM), ("HARD", road_src.HARD)):
        for q, answer, w1, w2, w3, ref, expl in rows:
            n += 1
            if q in seen:
                errors.append("duplicate question: " + q)
            seen.add(q)
            if len({answer, w1, w2, w3}) != 4:
                errors.append("options not distinct: " + q)
            try:
                verse = kjv.verse_text(ref).replace("\u00e6", "ae").replace("\u00c6", "Ae")
            except ValueError as err:
                errors.append("%s: %s" % (ref, err))
                continue
            words = [w for w in re.split(r"[\s']+", answer.lower()) if len(w) > 3]
            if words and not any(re.search(r"\b" + re.escape(w) + r"\b", verse.lower()) for w in words):
                errors.append("answer '%s' not in %s" % (answer, ref))
            print("%s %s: %s" % (tier, ref, verse[:140]))
    print("stages: %d" % n)
    for e in errors:
        print("ERROR", e)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
