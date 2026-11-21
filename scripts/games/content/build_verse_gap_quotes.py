"""Builds Verse Builder, Fill the Gap and Who Said It? JSON banks from verse_and_gap.py, checking every item against the KJV."""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
import kjv  # noqa: E402
import verse_and_gap as src  # noqa: E402
import quotes_more  # noqa: E402
import testament_bank  # noqa: E402

ASSETS = os.path.join(HERE, "..", "..", "..", "src", "main", "assets", "games")


def slug(text):
    return re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")[:60]


def tokens(text):
    return kjv.norm(text).split()


def write(name, payload):
    path = os.path.join(ASSETS, name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(payload, fh, ensure_ascii=False, indent=1)
        fh.write("\n")


def verse_builder():
    items, rejected, seen = [], [], set()
    for ref in src.VERSE_REFS:
        try:
            text = kjv.verse_text(ref)
        except ValueError as e:
            rejected.append(str(e))
            continue
        canon = kjv.canonical_ref(ref)
        if canon in seen:
            rejected.append("duplicate " + canon)
            continue
        seen.add(canon)
        n = len(text.split())
        diff = "easy" if n <= 11 else ("medium" if n <= 18 else "hard")
        items.append({"id": "verse_" + slug(canon), "difficulty": diff, "reference": canon,
                      "text": text, "words": n})
    write("verse_builder.json", {"version": 1, "items": items})
    return items, rejected


def fill_the_gap():
    items, rejected, seen = [], [], set()
    for ref, word, wrong in src.GAPS:
        try:
            text = kjv.verse_text(ref)
        except ValueError as e:
            rejected.append(str(e))
            continue
        toks = tokens(text)
        if toks.count(kjv.norm(word)) != 1:
            rejected.append(f"gap word not unique in {ref}: {word}")
            continue
        if len(set(wrong)) != 3 or any(kjv.norm(w) in toks or kjv.norm(w) == kjv.norm(word) for w in wrong):
            rejected.append(f"distractor problem in {ref}: {wrong}")
            continue
        raw = text.split()
        idx = next(i for i, t in enumerate(raw) if kjv.norm(t) == kjv.norm(word))
        canon = kjv.canonical_ref(ref)
        if canon in seen:
            rejected.append("duplicate " + canon)
            continue
        seen.add(canon)
        items.append({"id": "gap_" + slug(canon), "reference": canon, "text": text,
                      "gapIndex": idx, "answer": word, "choices": [word] + wrong})
    write("fill_the_gap.json", {"version": 1, "items": items})
    return items, rejected


def who_said_it():
    items, rejected = [], []
    for speaker, ref, phrase in src.QUOTES + quotes_more.QUOTES_MORE:
        try:
            text = kjv.verse_text(ref)
        except ValueError as e:
            rejected.append(str(e))
            continue
        if kjv.norm(phrase) not in kjv.norm(text):
            rejected.append(f"phrase not in {ref}: {phrase}")
            continue
        items.append({"id": "quote_" + slug(speaker + "_" + phrase), "speaker": speaker,
                      "quote": phrase, "reference": kjv.canonical_ref(ref), "verseText": text})
    speakers = sorted({i["speaker"] for i in items})
    write("who_said_it.json", {"version": 1, "speakers": speakers, "items": items})
    return items, rejected, speakers


def testaments():
    items, rejected, seen = [], [], set()
    for kind, name, testament, fact, ref in testament_bank.BOOKS + testament_bank.PEOPLE:
        if testament not in ("OLD", "NEW"):
            rejected.append("bad testament: " + name)
            continue
        try:
            kjv.verse_text(ref)
        except ValueError as e:
            rejected.append(str(e))
            continue
        key = (kind, name)
        if key in seen:
            rejected.append("duplicate " + name)
            continue
        seen.add(key)
        items.append({"id": kind + "_" + slug(name), "kind": kind, "name": name,
                      "testament": testament, "fact": fact, "reference": kjv.canonical_ref(ref)})
    write("old_or_new.json", {"version": 1, "items": items})
    return items, rejected


if __name__ == "__main__":
    v, vr = verse_builder()
    g, gr = fill_the_gap()
    q, qr, sp = who_said_it()
    counts = {d: sum(1 for i in v if i["difficulty"] == d) for d in ("easy", "medium", "hard")}
    print("verse_builder:", len(v), counts, "rejected:", len(vr))
    for r in vr:
        print("  ", r)
    print("fill_the_gap:", len(g), "rejected:", len(gr))
    for r in gr:
        print("  ", r)
    print("who_said_it:", len(q), "speakers:", len(sp), "rejected:", len(qr))
    for r in qr:
        print("  ", r)
    t, tr = testaments()
    print("old_or_new:", len(t), "books:", sum(1 for i in t if i["kind"] == "book"), "rejected:", len(tr))
    for r in tr:
        print("  ", r)
