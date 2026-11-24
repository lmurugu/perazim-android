#!/usr/bin/env python3
"""Checks every scripture reference in the guide's eval cases against the KJV verse-count index.

Reports references to books, chapters or verses that do not exist (for example Ezekiel 50:4). Some cases use
such references on purpose to test refusal, so the report lists them for review instead of failing silently.
Exit code 0 always; pass --strict to exit 1 when any reference is invalid.

Usage: python3 scripts/knowledge/check_eval_references.py [--strict]
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
INDEX = os.path.join(ROOT, "src/main/assets/ai/library/scripture/kjv-verse-counts.json")
CASES = os.path.join(ROOT, "ai-eval/perazim-guide/cases.jsonl")

REF = re.compile(r"^((?:[1-3] )?[A-Za-z][A-Za-z ]*?)\s+(\d+)(?::(\d+))?(?:\s*[-–].*)?$")
ALIASES = {"psalm": "psalms", "song of songs": "song of solomon", "revelations": "revelation"}


class Table:
    def __init__(self, path=INDEX):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        self.books = {b["name"].lower(): b["chapterVerseCounts"] for b in data["books"]}

    def check(self, ref):
        """Returns (valid, reason). valid is None when the reference cannot be parsed."""
        m = REF.match(ref.strip())
        if not m:
            return None, "unparsed"
        name = ALIASES.get(m.group(1).strip().lower(), m.group(1).strip().lower())
        if name not in self.books:
            return False, "unknown book"
        counts = self.books[name]
        chapter = int(m.group(2))
        if not 1 <= chapter <= len(counts):
            return False, "chapter %d does not exist (%d chapters)" % (chapter, len(counts))
        if m.group(3) is not None:
            verse = int(m.group(3))
            if not 1 <= verse <= counts[chapter - 1]:
                return False, "verse %d does not exist (%d verses)" % (verse, counts[chapter - 1])
        return True, "ok"


def scan(path=CASES, table=None):
    table = table or Table()
    results = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            case = json.loads(line)
            for ref in case.get("scripture_refs") or []:
                valid, reason = table.check(ref)
                results.append((case["id"], ref, valid, reason))
    return results


def main():
    results = scan()
    invalid = [r for r in results if r[2] is False]
    unparsed = [r for r in results if r[2] is None]
    print("references checked: %d" % len(results))
    print("invalid: %d" % len(invalid))
    for case_id, ref, _valid, reason in invalid:
        print("  %s  %s  (%s)" % (case_id, ref, reason))
    print("unparsed (chapter-range or free text, not checked): %d" % len(unparsed))
    if "--strict" in sys.argv and invalid:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
