"""Writes assets/games/word_search_themes.json from word_search_src.py."""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from word_search_src import THEMES  # noqa: E402

OUT = os.path.join(HERE, "..", "..", "..", "src", "main", "assets", "games", "word_search_themes.json")

if __name__ == "__main__":
    themes = [{"id": "t%02d" % i, "title": t, "words": [{"word": w, "reference": r} for w, r in ws]}
              for i, (t, ws) in enumerate(THEMES, start=1)]
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump({"translation": "KJV", "themes": themes}, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    print("themes:", len(themes))
