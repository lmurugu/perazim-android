"""Writes assets/games/story_chapters.json and the generated Java bank from story_chapters_src.py.
check_story_chapters.py verifies every reference and that each chapter is in text order."""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from story_chapters_src import CHAPTERS  # noqa: E402

ROOT = os.path.join(HERE, "..", "..", "..")
JSON_OUT = os.path.join(ROOT, "src", "main", "assets", "games", "story_chapters.json")
JAVA_OUT = os.path.join(ROOT, "src", "main", "java", "org", "perazimchurch", "app", "domain",
                        "games", "catalog", "StoryChapterBank.java")


def lit(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def main():
    chapters = []
    for i, (title, events) in enumerate(CHAPTERS, start=1):
        chapters.append({"id": "c%02d" % i, "title": title,
                         "events": [{"title": t, "reference": r} for t, r in events]})
    with open(JSON_OUT, "w", encoding="utf-8") as fh:
        json.dump({"translation": "KJV", "chapters": chapters}, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    rows = []
    for c in chapters:
        evs = ", ".join("ev(%s, %s)" % (lit(e["title"]), lit(e["reference"])) for e in c["events"])
        rows.append("        chapter(%s, %s, new Event[] {%s})," % (lit(c["id"]), lit(c["title"]), evs))
    rows[-1] = rows[-1].rstrip(",")
    java = open(os.path.join(HERE, "StoryChapterBank.template"), encoding="utf-8").read()
    java = java.replace("@@ROWS@@", "\n".join(rows))
    with open(JAVA_OUT, "w", encoding="utf-8") as fh:
        fh.write(java)
    print("chapters:", len(chapters), "events:", sum(len(c["events"]) for c in chapters))


if __name__ == "__main__":
    main()
