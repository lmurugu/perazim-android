#!/usr/bin/env python3
"""Builds the four doctrine entries Levi decided on 2026-11-24.

Each entry: the Perazim view first (Levi's wording), KJV verses copied verbatim from the bundled KJV and checked
against the verse-count index, then a short fair summary of other views. No verdict on any person.
Status: APPROVED_LEVI (Levi approved all four as written on 2026-11-24). Only that status may be shown.

Output: src/main/assets/ai/library/doctrine/entries.json
Run: python3 scripts/knowledge/build_doctrine_entries.py
"""
import gzip
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import check_eval_references as cer  # noqa: E402

KJV = os.path.join(ROOT, "src/main/assets/bible/text/KJV.tsv.gz")
OUT = os.path.join(ROOT, "src/main/assets/ai/library/doctrine/entries.json")

ENTRIES = [
    {
        "id": "holy-spirit-baptism",
        "topic": "Baptism in the Holy Spirit and tongues",
        "perazimView": "Spirit baptism is an experience distinct from conversion. Tongues are normal and desirable, but they are not the only evidence of Spirit baptism.",
        "verses": ["Acts 2:4", "Acts 8:17", "Acts 19:2", "Romans 8:9", "Ephesians 1:13", "1 Corinthians 12:30"],
        "otherViews": [
            "Classical Pentecostal view: Spirit baptism is a second experience after conversion, usually shown by speaking in tongues.",
            "Cessationist view: the miraculous sign gifts, including tongues, ended with the apostolic age. The Spirit's work continues through regeneration and sanctification.",
            "Many Christians hold views between these two, or hold that tongues may occur today without being required for every believer.",
        ],
    },
    {
        "id": "prosperity-and-giving",
        "topic": "Prosperity teaching, giving and seed offerings",
        "perazimView": "God calls believers to generous, cheerful giving. God's blessing cannot be bought with a 'seed' or a pledge. Manipulation and pressure in giving are warned against. Tithing is UNDECIDED and is not taught here as a position.",
        "verses": ["2 Corinthians 9:7", "Acts 20:35", "Acts 8:20", "Proverbs 22:16", "1 Timothy 6:17"],
        "otherViews": [
            "Prosperity teaching holds that faith and giving bring material return as a rule. Critics say this turns God's gifts into a transaction and can burden poorer believers.",
            "Some Christians hold that giving is an act of worship and trust, without a promised financial return.",
            "Views on tithing differ widely. This topic does not settle that question.",
        ],
    },
    {
        "id": "polygamy",
        "topic": "Polygamy and families already in polygamous marriages",
        "perazimView": "Marriage is one man and one woman. Families already in polygamy receive pastoral care. They are not told to break up or abandon wives and children.",
        "verses": ["Genesis 2:24", "Matthew 19:5", "Mark 10:8", "1 Timothy 3:2", "Titus 1:6"],
        "otherViews": [
            "Some interpreters read the polygamous households of the Old Testament as recorded, not endorsed. Others apply the 'husband of one wife' requirement for leaders (1 Timothy 3:2, Titus 1:6) more widely.",
            "Christians differ on pastoral responses to existing polygamous marriages, especially where leaving a marriage would harm wives and children.",
        ],
    },
    {
        "id": "dowry",
        "topic": "Dowry and bride wealth",
        "perazimView": "Dowry is an acceptable custom when it honours both families. It must not become a sale of a person, a debt trap, or a reason to delay marriage.",
        "verses": ["Genesis 2:24", "Exodus 22:16", "Proverbs 18:22", "1 Peter 3:7"],
        "otherViews": [
            "Some see dowry as honouring both families and marking a covenant between them.",
            "Others see a risk that dowry makes a person into a price, creates debt, or delays marriage. Local custom varies widely.",
        ],
    },
]


def load_kjv():
    verses = {}
    with gzip.open(KJV, "rt", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                b, c, v, text = line.rstrip("\n").split("\t", 3)
                verses[(int(b), int(c), int(v))] = text
    return verses


def book_numbers():
    with open(os.path.join(ROOT, "src/main/assets/ai/library/scripture/kjv-verse-counts.json"), encoding="utf-8") as f:
        return {b["name"]: b["book"] for b in json.load(f)["books"]}


def build():
    kjv = load_kjv()
    table = cer.Table()
    books = book_numbers()
    out = []
    for e in ENTRIES:
        rows = []
        for ref in e["verses"]:
            ok, reason = table.check(ref)
            if not ok:
                raise SystemExit("bad reference %s: %s" % (ref, reason))
            name, cv = ref.rsplit(" ", 1)
            c, v = (int(x) for x in cv.split(":"))
            text = kjv.get((books[name], c, v))
            if text is None:
                raise SystemExit("not in KJV: %s" % ref)
            rows.append({"ref": ref, "translation": "KJV", "text": text})
        out.append({
            "id": e["id"], "topic": e["topic"], "status": "APPROVED_LEVI",
            "approval": {"approver": "Levi", "date": "2026-11-24", "messageId": "cmsg_01TZR4y8Vg1KsGsVuQZmh5wy2YWVA7cCXDvZt9UHALpV1m",
                         "note": "Approved as written. Levi, 'Approve all four'."},
            "perazimView": e["perazimView"], "verses": rows, "otherViews": e["otherViews"],
            "noVerdict": "No verdict on any person is given or implied.",
        })
    return {
        "_comment": "Generated by scripts/knowledge/build_doctrine_entries.py from Levi's decisions of 2026-11-24. "
                    "Verse text from the bundled KJV. Approved by Levi as written on 2026-11-24 (APPROVED_LEVI). Any other status must not be shown.",
        "approver": "Levi",
        "entries": out,
    }


if __name__ == "__main__":
    data = build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print("wrote %s: %d entries, %d verses" % (OUT, len(data["entries"]), sum(len(e["verses"]) for e in data["entries"])))
