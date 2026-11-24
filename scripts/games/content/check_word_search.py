"""Checks assets/games/word_search_themes.json: each theme has 6 words, every word is 3 to 8 letters,
and each word occurs as a whole word in the KJV verse it cites."""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import kjv  # noqa: E402

PATH = os.path.join(HERE, "..", "..", "..", "src", "main", "assets", "games", "word_search_themes.json")


def main():
    data = json.load(open(PATH, encoding="utf-8"))
    errors = []
    for th in data["themes"]:
        if len(th["words"]) < 6:
            errors.append("fewer than 6 words: " + th["title"])
        seen = set()
        for w in th["words"]:
            word = w["word"]
            if not re.fullmatch(r"[A-Z]{3,8}", word):
                errors.append("bad word %s in %s" % (word, th["title"]))
            if word in seen:
                errors.append("repeated word %s in %s" % (word, th["title"]))
            seen.add(word)
            try:
                text = kjv.verse_text(w["reference"])
            except ValueError as e:
                errors.append("%s: %s" % (th["title"], e))
                continue
            if not re.search(r"\b" + re.escape(word.lower()) + r"\b", text.lower()):
                errors.append("%s: %s not found in %s" % (th["title"], word, w["reference"]))
    print("themes: %d" % len(data["themes"]))
    for e in errors:
        print("ERROR", e)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
