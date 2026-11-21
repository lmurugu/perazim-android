"""Builds assets/games/bible_trivia.json from the trivia source modules and checks every reference."""
import json
import os
import random
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
import kjv  # noqa: E402
import trivia_ot  # noqa: E402
import trivia_nt  # noqa: E402
import trivia_more  # noqa: E402
from drop_list import DROP_PREFIXES  # noqa: E402

OUT = os.path.join(HERE, "..", "..", "..", "src", "main", "assets", "games", "bible_trivia.json")
DIFF = {"E": "easy", "M": "medium", "H": "hard"}
warnings = []


def slug(text):
    return re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")[:60]


def build():
    raw = trivia_ot.OT + trivia_nt.NT + trivia_more.MORE
    items, seen_q = [], set()
    dropped = 0
    for row in raw:
        diff, q, answer, wrong, ref, expl, hint = row
        if any(q.startswith(p) for p in DROP_PREFIXES):
            dropped += 1
            continue
        if q in seen_q:
            raise SystemExit("duplicate question: " + q)
        seen_q.add(q)
        if len(wrong) != 3 or answer in wrong or len(set(wrong)) != 3:
            raise SystemExit("bad options: " + q)
        book, ch, nums = kjv.parse_ref(ref)  # raises on unparseable reference
        text = kjv.verse_text(ref)           # raises when the verse is not in the KJV
        if kjv.norm(answer) not in kjv.norm(text):
            warnings.append(f"answer not in verse text: {q} -> {answer} [{ref}]")
        options = [answer] + wrong
        random.Random(slug(q)).shuffle(options)
        items.append({
            "id": "trivia_" + slug(q),
            "difficulty": DIFF[diff],
            "question": q,
            "answer": answer,
            "options": options,
            "reference": kjv.canonical_ref(ref),
            "explanation": expl,
            "hint": hint,
        })
    ids = [i["id"] for i in items]
    if len(ids) != len(set(ids)):
        raise SystemExit("duplicate ids")
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump({"game": "bible_trivia", "version": 1, "items": items}, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    counts = {d: sum(1 for i in items if i["difficulty"] == d) for d in ("easy", "medium", "hard")}
    print("items:", len(items), "dropped:", dropped, counts)
    print("answer-not-literal-in-verse (review list, informational):", len(warnings))


if __name__ == "__main__":
    build()
