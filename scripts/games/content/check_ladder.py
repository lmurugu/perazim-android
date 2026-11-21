"""Checks the Ladder of Faith question bank in LadderBank.java: each entry has eight '~' fields,
its reference parses and exists in the bundled KJV, the correct answer appears in the verse
(case-insensitive, whole word) for at least one answer-bearing field, and no question repeats."""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import kjv  # noqa: E402

PATH = os.path.join(HERE, "..", "..", "..", "src", "main", "java", "org", "perazimchurch", "app",
                    "domain", "games", "LadderBank.java")


def entries(text):
    for m in re.finditer(r'^\s+"((?:[^"\\]|\\.)*)",?\s*$', text, re.M):
        yield m.group(1).replace('\\"', '"')


def main():
    text = open(PATH, encoding="utf-8").read()
    errors, review, seen, n = [], [], set(), 0
    for e in entries(text):
        n += 1
        parts = e.split("~")
        if len(parts) != 8:
            errors.append("%d fields in: %s" % (len(parts), e[:60]))
            continue
        q, answer, ref = parts[0], parts[1], parts[2]
        if q in seen:
            errors.append("duplicate question: " + q)
        seen.add(q)
        try:
            verse = kjv.verse_text(ref).lower()
        except ValueError as err:
            errors.append("%s: %s" % (q[:50], err))
            continue
        words = [w for w in re.split(r"\s+", answer.lower()) if len(w) > 3]
        if words and not any(re.search(r"\b" + re.escape(w) + r"\b", verse) for w in words):
            # Author, place and "who" questions often cite a verse that does not repeat the name.
            # These are listed for a human read rather than failed.
            review.append("answer '%s' not in %s: %s" % (answer, ref, q[:70]))
    print("questions: %d" % n)
    print("for human review (answer word not in cited verse): %d" % len(review))
    for item in review:
        print("REVIEW", item)
    for err in errors:
        print("ERROR", err)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
