"""Checks assets/games/memory_sets.json: at least five pairs per set, at least one word of each pair appears as a whole
word in the KJV verse cited, and no word repeats inside a set."""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import kjv  # noqa: E402

PATH = os.path.join(HERE, "..", "..", "..", "src", "main", "assets", "games", "memory_sets.json")


def main():
    data = json.load(open(PATH, encoding="utf-8"))
    errors = []
    for s in data["sets"]:
        if len(s["pairs"]) < 5:
            errors.append("fewer than 5 pairs: " + s["title"])
        seen = set()
        for p in s["pairs"]:
            for word in (p["a"], p["b"]):
                if not re.fullmatch(r"[A-Z]{3,12}", word):
                    errors.append("bad word %s in %s" % (word, s["title"]))
            if p["a"] == p["b"]:
                errors.append("pair of identical words in " + s["title"])
            try:
                text = kjv.verse_text(p["reference"]).lower()
            except ValueError as e:
                errors.append("%s: %s" % (s["title"], e))
                continue
            if not any(re.search(r"\b" + re.escape(w.lower()) + r"\b", text) for w in (p["a"], p["b"])):
                errors.append("%s: neither %s nor %s in %s" % (s["title"], p["a"], p["b"], p["reference"]))
            for word in (p["a"], p["b"]):
                if word in seen:
                    errors.append("repeated word %s in %s" % (word, s["title"]))
                seen.add(word)
    print("sets: %d" % len(data["sets"]))
    for e in errors:
        print("ERROR", e)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
