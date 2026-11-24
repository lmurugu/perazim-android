"""Checks assets/games/story_chapters.json: every reference verifies in the bundled KJV, events
within a chapter run in text order (book, chapter, verse strictly increasing), titles unique in a chapter."""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import kjv  # noqa: E402

PATH = os.path.join(HERE, "..", "..", "..", "src", "main", "assets", "games", "story_chapters.json")


def main():
    data = json.load(open(PATH, encoding="utf-8"))
    errors = []
    ids = set()
    events = 0
    for ch in data["chapters"]:
        if ch["id"] in ids:
            errors.append("duplicate chapter id " + ch["id"])
        ids.add(ch["id"])
        if len(ch["events"]) < 4:
            errors.append("fewer than 4 events: " + ch["title"])
        titles = set()
        last = None
        for ev in ch["events"]:
            events += 1
            if ev["title"] in titles:
                errors.append("duplicate event in %s: %s" % (ch["title"], ev["title"]))
            titles.add(ev["title"])
            try:
                book, c, nums = kjv.parse_ref(ev["reference"])
                kjv.verse_text(ev["reference"])
            except ValueError as e:
                errors.append("%s: %s" % (ch["title"], e))
                continue
            pos = (book, c, nums[0])
            if last is not None and pos <= last:
                errors.append("out of text order in %s at %s" % (ch["title"], ev["reference"]))
            last = pos
    print("chapters: %d, events: %d" % (len(data["chapters"]), events))
    for e in errors:
        print("ERROR", e)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
