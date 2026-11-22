#!/usr/bin/env python3
"""Builds the Vine Words level set from the bundled KJV.

Writes src/main/assets/games/vine_words.json. Run from anywhere:

    python3 scripts/games/content/build_vine_words.py

Standard library only. The output is deterministic: the same KJV text and the same
constants always give the same file. Every target word, bonus word and verse reference
is checked against the bundled KJV before the file is written; any failure stops the
build with a non-zero exit code.

Rules (see docs/games/research/MECHANICS.md, section 2.1):
  * Each level is built from one passage of one to three KJV verses.
  * Target words are lowercase words in that passage, 3 to 8 letters, with no proper names.
  * Words are placed as a crossword: every word crosses at least one other word, and no
    two words touch unless they cross. Placement is a backtracking search with 200 attempts
    per seed; a failing seed moves on to the next one.
  * The wheel holds the most each letter is needed for across all target words, plus up to
    two extra letters for an optional bonus word.
"""

import argparse
import gzip
import itertools
import json
import random
import re
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
KJV_TSV = ROOT / "src/main/assets/bible/text/KJV.tsv.gz"
KJV_MANIFEST = ROOT / "src/main/assets/bible/kjv_manifest.json"
OUT = ROOT / "src/main/assets/games/vine_words.json"

LEVEL_COUNT = 60
MAX_TARGETS = 10
MAX_SEED_TRIES = 300
SUBSETS_PER_SEED = 4
MAX_EXTRA_LETTERS = 2
MAX_BONUS_LENGTH = 6
MIN_BONUS_LENGTH = 3
MAX_VERSES = 3

# first level, last level, shortest word, longest word, largest grid side, wheel letter limit
BANDS = [
    (1, 20, 3, 5, 7, 14),
    (21, 40, 4, 6, 8, 17),
    (41, 60, 4, 8, 9, 20),
]

# Curated passages, one to three verses each. Every reference is checked against the KJV below.
PASSAGES = [
    "John 15:1-3", "John 15:12-14", "John 14:1-3", "John 10:11-13", "John 3:16-17",
    "John 1:1-3", "John 8:12", "John 11:25-26", "Psalm 23:1-3", "Psalm 23:5-6",
    "Psalm 1:1-3", "Psalm 19:1-2", "Psalm 27:1", "Psalm 46:1-2", "Psalm 100:1-3",
    "Psalm 121:1-2", "Psalm 139:13-14", "Psalm 119:105", "Psalm 34:8", "Proverbs 3:5-6",
    "Proverbs 22:6", "Proverbs 15:1", "Isaiah 40:29-31", "Isaiah 41:10", "Isaiah 53:5-6",
    "Matthew 5:3-5", "Matthew 5:14-16", "Matthew 6:26", "Matthew 7:7-8", "Matthew 11:28-30",
    "Matthew 28:19-20", "Mark 4:30-32", "Luke 2:10-11", "Luke 15:4-6", "Romans 8:28",
    "1 Corinthians 13:4-6", "Galatians 5:22-23", "Ephesians 6:10-11", "Philippians 4:6-7",
    "Colossians 3:23", "2 Timothy 1:7", "Hebrews 11:1", "James 1:5", "1 Peter 5:7",
    "1 John 4:8", "Revelation 21:4", "Genesis 1:1-3", "Genesis 2:7", "Joshua 1:9",
    "Ruth 1:16", "1 Samuel 16:7", "Micah 6:8", "Exodus 14:13-14", "Deuteronomy 6:4-5",
]

# Function words and common speech. They make poor crossword answers.
STOP_WORDS = set("""
a about above after again against all also am an and any are as at be because been
before being below between both but by can could did do does doing down during each
even every except for from further had has have having he her here hers herself him
himself his how i if in into is it its itself just let me more most my myself no nor
not now of off on once only or other our ours ourselves out over own same she should
so some such than that the their theirs them themselves then there these they this
those thou thee thy thine through to too under until up upon us very was we were what
when where which while who whom why will with would yet you your yours yourself
unto saith shall hath thereof therein thereby wherefore lo yea nay one all
""".split())


def band_for(level):
    for first, last, lo, hi, side, wheel in BANDS:
        if first <= level <= last:
            return {"first": first, "last": last, "min_len": lo, "max_len": hi,
                    "side": side, "wheel": wheel, "number": BANDS.index((first, last, lo, hi, side, wheel)) + 1}
    raise ValueError(f"no band for level {level}")


def target_count(level):
    return min(MAX_TARGETS, 3 + (level - 1) // 7)


def load_book_numbers():
    manifest = json.loads(KJV_MANIFEST.read_text(encoding="utf-8"))
    numbers = {entry["name"].lower(): entry["bookNumber"] for entry in manifest}
    numbers["psalm"] = numbers["psalms"]  # the reference style used in the passage list
    return numbers


def load_kjv():
    verses = {}
    with gzip.open(KJV_TSV, "rt", encoding="utf-8") as handle:
        for line in handle:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 4:
                continue
            verses[(int(parts[0]), int(parts[1]), int(parts[2]))] = parts[3]
    return verses


REF_RE = re.compile(r"^(?P<book>.+?)\s+(?P<chapter>\d+):(?P<first>\d+)(?:-(?P<last>\d+))?$")


def passage_verses(ref, book_numbers, kjv):
    """Returns [(label, text)] for a passage reference such as 'John 15:1-5'."""
    match = REF_RE.match(ref)
    if not match:
        raise ValueError(f"cannot parse reference {ref!r}")
    book = book_numbers.get(match.group("book").lower())
    if book is None:
        raise ValueError(f"unknown book in {ref!r}")
    chapter = int(match.group("chapter"))
    first = int(match.group("first"))
    last = int(match.group("last") or first)
    if last < first or last - first + 1 > MAX_VERSES:
        raise ValueError(f"{ref!r} must have one to {MAX_VERSES} verses")
    result = []
    for number in range(first, last + 1):
        text = kjv.get((book, chapter, number))
        if text is None:
            raise ValueError(f"{ref!r}: verse {number} is not in the bundled KJV")
        label = f"{match.group('book')} {chapter}:{number}"
        result.append((label, text))
    return result


def passage_tokens(verses):
    return re.findall(r"[A-Za-z]+", " ".join(text for _, text in verses))


def candidate_words(verses, band):
    """Lowercase words from the passage that suit the band. Proper names are excluded:
    a word qualifies only when it appears in the text entirely in lowercase."""
    words = set()
    for token in passage_tokens(verses):
        if token.islower() and band["min_len"] <= len(token) <= band["max_len"] and token not in STOP_WORDS:
            words.add(token.upper())
    return sorted(words)


def grid_of(placements):
    """Map (row, col) -> letter for placements, or None when two words disagree on a cell."""
    cells = {}
    for word, row, col, across in placements:
        for index, letter in enumerate(word):
            cell = (row, col + index) if across else (row + index, col)
            if cells.get(cell, letter) != letter:
                return None
            cells[cell] = letter
    return cells


def valid_layout(placements):
    """True when every maximal run of two or more letters is exactly one placed word.
    That rules out words that touch without crossing, parallel overlaps and clashes."""
    cells = grid_of(placements)
    if cells is None:
        return False
    found = set()
    for (row, col) in cells:
        for across in (True, False):
            dr, dc = (0, 1) if across else (1, 0)
            if (row - dr, col - dc) in cells:
                continue  # not the start of a run
            word = ""
            r, c = row, col
            while (r, c) in cells:
                word += cells[(r, c)]
                r, c = r + dr, c + dc
            if len(word) >= 2:
                found.add((word, row, col, across))
    return found == set(placements)


def box_side(placements):
    if grid_of(placements) is None:
        return 99
    _, _, rows, cols = bounding_box(placements)
    return max(rows, cols)


def fits_side(placements, side):
    return box_side(placements) <= side


def build_grid(pool, count, rng, side):
    """Grows a crossword from the passage words. Each new word must cross a word already
    placed and keep the grid within side x side. Returns (words, placements) or None.

    Each step picks at random among the most compact legal placements, so the search
    finds dense, connected grids without a fixed word list to fit.
    """
    start = pool[rng.randrange(len(pool))]
    placements = [(start, 0, 0, True)]
    used = {start}
    while len(used) < count:
        cells = grid_of(placements)
        options = []
        for word in pool:
            if word in used:
                continue
            for (row, col), letter in cells.items():
                for offset, ch in enumerate(word):
                    if ch != letter:
                        continue
                    for candidate in ((word, row, col - offset, True), (word, row - offset, col, False)):
                        trial = placements + [candidate]
                        if fits_side(trial, side) and valid_layout(trial):
                            options.append(candidate)
        if not options:
            return None
        best = min(box_side(placements + [c]) for c in options)
        compact = [c for c in options if box_side(placements + [c]) == best]
        choice = compact[rng.randrange(len(compact))]
        placements.append(choice)
        used.add(choice[0])
    return [w for w, _, _, _ in placements], placements


def bounding_box(placements):
    cells = grid_of(placements)
    rows = [r for r, _ in cells]
    cols = [c for _, c in cells]
    return min(rows), min(cols), max(rows) - min(rows) + 1, max(cols) - min(cols) + 1


def wheel_letters(words, bonus):
    need = Counter()
    for word in words:
        for letter, count in Counter(word).items():
            need[letter] = max(need[letter], count)
    if bonus:
        deficit = Counter(bonus) - need
        need.update(deficit)
    return "".join(sorted(need.elements()))


def spellable(word, wheel):
    return not (Counter(word) - Counter(wheel))


def choose_bonus(passage_words, targets, wheel_budget, rng, need_counter):
    options = []
    for word in passage_words:
        if word in targets or not (MIN_BONUS_LENGTH <= len(word) <= MAX_BONUS_LENGTH):
            continue
        deficit = Counter(word) - need_counter
        if sum(deficit.values()) <= MAX_EXTRA_LETTERS:
            options.append(word)
    rng.shuffle(options)
    for word in options:
        if len(wheel_letters(targets, word)) <= wheel_budget:
            return word
    return None


def build_level(level, book_numbers, kjv, passages):
    band = band_for(level)
    count = target_count(level)
    eligible = []
    for ref in passages:
        verses = passage_verses(ref, book_numbers, kjv)
        pool = candidate_words(verses, band)
        if len(pool) >= count + 2:
            eligible.append((ref, verses, pool))
    if not eligible:
        raise RuntimeError(f"level {level}: no passage has enough words")
    for seed in range(level * 1000, level * 1000 + MAX_SEED_TRIES):
        rng = random.Random(seed)
        ref, verses, pool = eligible[rng.randrange(len(eligible))]
        for _ in range(SUBSETS_PER_SEED):
            grown = build_grid(pool, count, rng, band["side"])
            if grown is None:
                continue
            targets_list, placements = grown
            targets = sorted(targets_list)
            top, left, rows, cols = bounding_box(placements)
            need = Counter()
            for word in targets:
                for letter, n in Counter(word).items():
                    need[letter] = max(need[letter], n)
            bonus = choose_bonus([w for w in pool if w not in targets], targets, band["wheel"], rng, need)
            wheel = wheel_letters(targets, bonus)
            if len(wheel) > band["wheel"]:
                continue
            return {
                "id": f"vine-{level:02d}",
                "level": level,
                "band": band["number"],
                "passage": ref,
                "seed": seed,
                "verses": [{"ref": label, "text": text} for label, text in verses],
                "rows": rows,
                "cols": cols,
                "words": [
                    {"word": word, "row": row - top, "col": col - left, "across": across}
                    for word, row, col, across in sorted(placements, key=lambda p: (p[0], p[1], p[2]))
                ],
                "wheel": wheel,
                "bonus": bonus,
            }
    raise RuntimeError(f"level {level}: no layout found in {MAX_SEED_TRIES} seeds")


def verify_level(level):
    """Checks a built level against the rules. Returns a list of problems (empty when valid)."""
    problems = []
    lowered_verse_tokens = set()
    for verse in level["verses"]:
        lowered_verse_tokens.update(t.upper() for t in re.findall(r"[A-Za-z]+", verse["text"]))
    placements = [(w["word"], w["row"], w["col"], w["across"]) for w in level["words"]]
    if not valid_layout(placements):
        problems.append("grid is not a valid crossword")
    for word in level["words"]:
        if word["word"] not in lowered_verse_tokens:
            problems.append(f"{word['word']} is not in the passage")
        if not spellable(word["word"], level["wheel"]):
            problems.append(f"{word['word']} cannot be spelled from the wheel")
    if level["bonus"] and not spellable(level["bonus"], level["wheel"]):
        problems.append("bonus word cannot be spelled from the wheel")
    if level["bonus"] and level["bonus"] in {w["word"] for w in level["words"]}:
        problems.append("bonus word repeats a target word")
    return problems


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", type=Path, default=OUT)
    parser.add_argument("--levels", type=int, default=LEVEL_COUNT)
    args = parser.parse_args(argv)

    book_numbers = load_book_numbers()
    kjv = load_kjv()
    levels = []
    for number in range(1, args.levels + 1):
        level = build_level(number, book_numbers, kjv, PASSAGES)
        problems = verify_level(level)
        if problems:
            print(f"level {number} failed checks: {problems}", file=sys.stderr)
            return 1
        levels.append(level)

    document = {"version": 1, "levelCount": len(levels), "levels": levels}
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(document, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"wrote {len(levels)} levels to {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
