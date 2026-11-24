"""Checks assets/games/who_am_i.json: unique answers, 3+ clues each, every reference verifies in the bundled KJV."""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import kjv  # noqa: E402

PATH = os.path.join(HERE, "..", "..", "..", "src", "main", "assets", "games", "who_am_i.json")


def main():
    data = json.load(open(PATH, encoding="utf-8"))
    errors = []
    seen = set()
    for i, p in enumerate(data["people"]):
        name = p["answer"]
        key = kjv.norm(name)
        if key in seen:
            errors.append(f"duplicate answer: {name}")
        seen.add(key)
        if len(p["clues"]) < 3:
            errors.append(f"fewer than 3 clues: {name}")
        for part in p["reference"].split(";"):
            try:
                kjv.verse_text(part.strip())
            except ValueError as e:
                errors.append(f"{name}: {e}")
    print(f"people: {len(data['people'])}")
    for e in errors:
        print("ERROR", e)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
