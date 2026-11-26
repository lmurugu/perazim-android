#!/usr/bin/env python3
"""Turns exported unanswered-question logs into a review list for the next guide update.

Input: the JSON-lines text a member shares from the app ("Questions the guide could not answer" > Send), one file per
export, or a folder of them. Output: a Markdown review list grouped by topic, with each question marked either
"library-missing" (add approved material, as a DRAFT for Levi) or "access-failure" (the library has the material, so
fix retrieval or routing and test with several phrasings). Standard library only.

Usage:
  python3 ai-eval/gap-review/gap_review.py --in exports/ --out review.md
  python3 ai-eval/gap-review/gap_review.py --in one-export.jsonl --library src/main/assets/ai/library --out review.md

Privacy: the script redacts again before it writes anything, and drops any record that still shows a long digit run
or an email address. Nothing is uploaded; the review list is for the team.
"""
import argparse
import json
import os
import re
import sys
from collections import OrderedDict

SCHEMA = "perazim-gap-v1"
EMAIL = re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+")
LINK = re.compile(r"(?i)(?:https?://|www\.)\S+")
DIGIT_RUN = re.compile(r"\d{6,}")
WORD = re.compile(r"[a-z]{5,}")

STOPWORDS = set("""about after again against their there these those which while would could should where
other shall unto thou thee doth hath hast does have from with that this what when into upon your yours
being because really teach taught believe believes christian christians church bible verse verses mean means
explain tell know someone think something always never every also just only very much more most many good""".split())

TOPICS = OrderedDict([
    ("Baptism and communion", r"baptis|communion|lord's supper|sacrament"),
    ("Holy Spirit and tongues", r"holy spirit|tongue|spirit baptism|prophecy|healing|gifts?"),
    ("Salvation and assurance", r"salvation|saved|assurance|eternal security|lose (?:salvation|place)|apostas|fall away"),
    ("Marriage and gender", r"marriage|husband|wife|divorce|polygam|dowry|submission|headship|women|woman|\bmen\b"),
    ("Giving and prosperity", r"tithe|tithing|offering|prosperity|seed|giving|money"),
    ("Israel and end times", r"israel|rapture|tribulation|millennium|end times|antichrist|second coming"),
    ("Mary, saints and prayer to the dead", r"\bmary\b|saint|purgatory|pray to|intercess"),
    ("Bible reference and word search", r"verse|chapter|kjv|nasb|quote|\bword\b|phrase|psalm|genesis|romans|john \d"),
    ("Other faiths and groups", r"islam|muslim|quran|qur'an|mormon|jehovah|catholic|orthodox|hindu|buddh|reincarn"),
    ("Church life and ministry", r"pastor|elder|sermon|worship|ministry|membership|church|fellowship"),
])


def redact_again(text):
    """Second pass, in case an older app build let something through. Returns None when unsafe."""
    if not isinstance(text, str) or not text.strip():
        return None
    s = " ".join(text.split())
    s = EMAIL.sub("[email]", s)
    s = LINK.sub("[link]", s)
    s = DIGIT_RUN.sub("[number]", s)
    if "@" in s or DIGIT_RUN.search(s):
        return None
    return s[:300]


def topic_for(question):
    q = question.lower()
    for name, pattern in TOPICS.items():
        if re.search(pattern, q):
            return name
    return "Other"


def load_records(paths):
    """Yields (record, file name) for every valid line. Lines that are not perazim-gap-v1 records are counted, not guessed at."""
    files = []
    for p in paths:
        if os.path.isdir(p):
            for name in sorted(os.listdir(p)):
                if name.endswith((".jsonl", ".json", ".txt")):
                    files.append(os.path.join(p, name))
        else:
            files.append(p)
    bad = 0
    for f in files:
        with open(f, encoding="utf-8") as fh:
            for line in fh:
                line = line.strip()
                if not line:
                    continue
                try:
                    rec = json.loads(line)
                except json.JSONDecodeError:
                    bad += 1
                    continue
                if not isinstance(rec, dict) or rec.get("schema") != SCHEMA:
                    bad += 1
                    continue
                yield rec, os.path.basename(f)
    if bad:
        print(f"Skipped {bad} line(s) that were not perazim-gap-v1 records.", file=sys.stderr)


def library_text(library_dir):
    chunks = []
    if not library_dir or not os.path.isdir(library_dir):
        return ""
    for root, _, names in os.walk(library_dir):
        for name in names:
            if name.endswith((".md", ".txt", ".json", ".jsonl")):
                with open(os.path.join(root, name), encoding="utf-8", errors="ignore") as fh:
                    chunks.append(fh.read().lower())
    return "\n".join(chunks)


def classify(question, library):
    """'access-failure' when at least half of the question's content words appear in the library, else 'library-missing'."""
    words = [w for w in set(WORD.findall(question.lower())) if w not in STOPWORDS]
    if not words or not library:
        return "library-missing"
    found = sum(1 for w in words if w in library)
    return "access-failure" if found * 2 >= len(words) else "library-missing"


def build(records, library):
    grouped = OrderedDict()
    dropped = 0
    total = 0
    for rec, _source in records:
        total += 1
        question = redact_again(rec.get("question"))
        if question is None:
            dropped += 1
            continue
        reason = str(rec.get("reason", "other"))
        key = (" ".join(re.sub(r"[^\w]+", " ", question.lower()).split()), reason)
        count = int(rec.get("count", 1) or 1)
        item = grouped.get(key)
        if item is None:
            grouped[key] = {
                "question": question, "reason": reason, "route": str(rec.get("route", "")),
                "app": str(rec.get("app", "")), "count": count,
                "first": rec.get("first", 0), "last": rec.get("last", 0),
            }
        else:
            item["count"] += count
            item["first"] = min(item["first"], rec.get("first", item["first"]))
            item["last"] = max(item["last"], rec.get("last", item["last"]))
    rows = []
    for item in grouped.values():
        item["topic"] = topic_for(item["question"])
        item["status"] = classify(item["question"], library)
        rows.append(item)
    rows.sort(key=lambda r: (-r["count"], r["topic"], r["question"]))
    return rows, {"read": total, "dropped_unsafe": dropped, "unique": len(rows)}


def render(rows, stats, library_dir):
    out = ["# Unanswered questions: review list", ""]
    out.append(f"Records read: {stats['read']}. Unique questions: {stats['unique']}. "
               f"Dropped as unsafe to keep: {stats['dropped_unsafe']}.")
    out.append("")
    out.append("Library checked: " + (library_dir or "(not given; every question is marked library-missing)"))
    out.append("")
    out.append("- **library-missing**: the approved library does not hold this topic. Draft Protestant, Pentecostal or "
               "Evangelical material with two KJV passages, mark it DRAFT, and send it to Levi.")
    out.append("- **access-failure**: the library has the material. Find why retrieval or routing did not reach it, then "
               "test with several different phrasings. Do not add a per-question route.")
    out.append("")
    for status in ("library-missing", "access-failure"):
        section = [r for r in rows if r["status"] == status]
        out.append(f"## {status} ({len(section)})")
        out.append("")
        if not section:
            out.append("None.")
            out.append("")
        by_topic = OrderedDict()
        for r in section:
            by_topic.setdefault(r["topic"], []).append(r)
        for topic, items in by_topic.items():
            out.append(f"### {topic}")
            out.append("")
            for r in items:
                out.append(f"- ×{r['count']} · {r['reason']} · route `{r['route']}` · app {r['app']}: {r['question']}")
            out.append("")
    return "\n".join(out) + "\n"


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--in", dest="inputs", nargs="+", required=True, help="export files or folders")
    ap.add_argument("--library", default="", help="library folder, e.g. src/main/assets/ai/library")
    ap.add_argument("--out", required=True, help="Markdown review list to write")
    args = ap.parse_args(argv)
    rows, stats = build(load_records(args.inputs), library_text(args.library))
    with open(args.out, "w", encoding="utf-8") as fh:
        fh.write(render(rows, stats, args.library))
    print(json.dumps({"unique": stats["unique"], "read": stats["read"], "dropped_unsafe": stats["dropped_unsafe"],
                      "library_missing": sum(r["status"] == "library-missing" for r in rows),
                      "access_failure": sum(r["status"] == "access-failure" for r in rows), "out": args.out}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
