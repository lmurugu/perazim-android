#!/usr/bin/env python3
"""Builds the Kenyan pastoral-support topics. Every verse text is taken from the bundled KJV, never typed.

Input: the TOPICS spec below (written by hand: topic, summary, verse references, notes).
Output: src/main/assets/ai/library/pastoral/kenya-topics.json, status DRAFT_PENDING_LEVI (fail-closed).

A reference that does not exist in the KJV stops the build. Run after editing TOPICS:
    python3 scripts/knowledge/build_pastoral_topics.py
"""
import gzip
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import check_eval_references as cer  # noqa: E402  (range table)

KJV = os.path.join(ROOT, "src/main/assets/bible/text/KJV.tsv.gz")
OUT = os.path.join(ROOT, "src/main/assets/ai/library/pastoral/kenya-topics.json")
BOOK_NUMBER = {"Deuteronomy": 5, "1 Kings": 11, "Psalms": 19, "Proverbs": 20, "Matthew": 40, "Galatians": 48,
               "Ephesians": 49, "1 Timothy": 54, "Romans": 45, "1 John": 62}

TOPICS = [
    {
        "id": "debt-and-betting",
        "topic": "Debt, loans and betting",
        "summary": "Debt and betting can take hold of a family and bring shame and fear. Scripture warns about borrowing without end and about the hope of quick money. It calls us to honest work and to open talk with trusted people in the church.",
        "verses": ["Proverbs 22:7", "Proverbs 13:11", "Proverbs 28:22", "1 Timothy 6:10", "Romans 13:8"],
        "resources": "No verified helpline for debt or betting is recorded in the approved crisis list. Do not give a number for this. Refer to a trusted church leader.",
        "notes": "Gap: a verified gambling or debt-counselling resource needs a source check before it can be shown.",
    },
    {
        "id": "witchcraft-and-curses",
        "topic": "Fear of witchcraft and curses",
        "summary": "Fear of curses or witchcraft is real for many people. Scripture names witchcraft among the works of the flesh and warns against it. It also tells believers that God's protection is greater than any power that can be raised against them.",
        "verses": ["Galatians 5:20", "Deuteronomy 18:10", "1 John 4:4", "Psalms 34:7", "Ephesians 6:12"],
        "resources": "If someone threatens you or a child, call 999 or 112 now. Then talk to a trusted church leader.",
        "notes": "Not a verdict on any person or family. Do not confirm or deny that any specific curse is real.",
    },
    {
        "id": "false-prophets",
        "topic": "False prophets and miracle claims",
        "summary": "Scripture asks believers to test what they hear against the Word, and to judge teaching by its fruit. Be careful with claims of miracles or messages that lead people away from Christ. This is not a judgment on any person.",
        "verses": ["Matthew 7:15", "Matthew 7:16", "1 John 4:1", "Matthew 24:24", "Galatians 1:8"],
        "resources": "If you are being pressured for money or obedience by a leader, talk to a trusted church leader before you act.",
        "notes": "Describes what Scripture asks of believers. Names no leader and gives no verdict on any teacher.",
    },
    {
        "id": "mental-health-stigma",
        "topic": "Depression, anxiety and the stigma around them",
        "summary": "Depression and anxiety are not a sign that God has left you, and seeking help is not a lack of faith. Scripture is honest about deep sorrow, and it invites weary people to rest in God. Reaching out to a doctor or counsellor is wise.",
        "verses": ["Psalms 34:18", "Psalms 42:5", "1 Kings 19:5", "Matthew 11:28"],
        "resources": "If you think you might harm yourself, call 999 or 112 now, or tell a trusted person today. Befrienders Kenya is listed for weekdays 9am to 5pm, not 24 hours: +254 722 178 177.",
        "notes": "Crisis answers use the fixed crisis wording. Do not improvise safety advice.",
    },
]


def load_kjv():
    verses = {}
    with gzip.open(KJV, "rt", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            b, c, v, text = line.rstrip("\n").split("\t", 3)
            verses[(int(b), int(c), int(v))] = text
    return verses


def parse(ref):
    book, cv = ref.rsplit(" ", 1)
    chap, verse = cv.split(":")
    return book, int(chap), int(verse)


def build():
    kjv = load_kjv()
    table = cer.Table()
    out = []
    for t in TOPICS:
        rows = []
        for ref in t["verses"]:
            book, c, v = parse(ref)
            ok, reason = table.check(ref)
            if not ok:
                raise SystemExit("bad reference %s: %s" % (ref, reason))
            key = (BOOK_NUMBER[book], c, v)
            if key not in kjv:
                raise SystemExit("not in KJV: %s" % ref)
            rows.append({"ref": ref, "translation": "KJV", "text": kjv[key]})
        out.append({
            "id": t["id"], "topic": t["topic"], "status": "DRAFT_PENDING_LEVI",
            "summary": t["summary"], "verses": rows, "resources": t["resources"], "notes": t["notes"],
        })
    return {
        "_comment": "Generated by scripts/knowledge/build_pastoral_topics.py. Verse text comes from the bundled KJV. "
                    "Fail-closed: every topic is DRAFT_PENDING_LEVI and must not be shown until Levi approves.",
        "approver": "Levi",
        "topics": out,
    }


if __name__ == "__main__":
    data = build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print("wrote %s: %d topics, %d verses" % (OUT, len(data["topics"]), sum(len(t["verses"]) for t in data["topics"])))
