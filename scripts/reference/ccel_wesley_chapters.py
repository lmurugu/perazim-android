"""Build the chapter-keyed Wesley's Notes on the New Testament from the CCEL web pages.

CCEL (Christian Classics Ethereal Library, public domain text) publishes each chapter of Wesley's Notes on its own page,
under notes.i.* (https://ccel.org/ccel/wesley/notes/notes.i.html). This script walks the "Next" links from the first New
Testament book page, keeps each chapter's commentary, and checks every book against its chapter count.

Output: scripts/reference/ccel/wesley-nt-chapters.json, read by build_reference_db.py (parser ccel_wesley_chapters).

    python3 scripts/reference/ccel_wesley_chapters.py --out scripts/reference/ccel/wesley-nt-chapters.json

The crawl makes one request per page with a half-second pause. Stop at Revelation 22; the walk otherwise continues
into the Old Testament.
"""
import argparse
import html
import json
import re
import subprocess
import sys
import time
from pathlib import Path

START = "https://ccel.org/ccel/wesley/notes/notes.i.ii.html"
BOOKS = ["Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians", "2 Corinthians", "Galatians",
         "Ephesians", "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy",
         "Titus", "Philemon", "Hebrews", "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude",
         "Revelation"]
CHAPTERS = [28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22]
ROMAN = {"I": 1, "V": 5, "X": 10, "L": 50, "C": 100}


def roman_to_int(text):
    s = text.strip().upper()
    if not s or any(ch not in ROMAN for ch in s):
        return None
    total = 0
    for i, ch in enumerate(s):
        if i + 1 < len(s) and ROMAN[ch] < ROMAN[s[i + 1]]:
            total -= ROMAN[ch]
        else:
            total += ROMAN[ch]
    return total


def page_text(page):
    """Visible text of a CCEL page: the header and navigation removed, and the commentary kept."""
    body = re.sub(r"<script.*?</script>|<style.*?</style>", "", page, flags=re.S)
    text = re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", body)))
    start = text.find("Next » ")
    end = text.find("Please login or register", start if start >= 0 else 0)
    if start < 0 or end <= start:
        return "", ""
    seg = text[start + 7:end]
    seg = re.sub(r"^(?:«\s*Prev[^»]*»\s*)+", "", seg).strip()
    seg = re.sub(r"\s*«\s*Prev[^»]*»\s*(?:[^«]*)?$", "", seg).strip()
    # A chapter page starts with its Roman numeral heading ("IX 1. His own city ..."); the heading is not commentary.
    seg = re.sub(r"^[IVXLC]+\s+(?=\d)", "", seg)
    title_match = re.match(r"^(\S+(?:\s\S+)?)", seg)
    return (title_match.group(1) if title_match else ""), seg


def crawl(start, pause=0.5):
    """Yields (url, title, content) for each page from start, following the Next links."""
    url, seen = start, set()
    while url and url not in seen:
        seen.add(url)
        page = subprocess.run(["curl", "-sS", "--max-time", "40", "--retry", "2", url],
                              capture_output=True).stdout.decode("utf-8", "replace")
        title, content = page_text(page)
        yield url, title, content
        match = re.search(r'<a[^>]+href="([^"]+)"[^>]*>\s*(?:<[^>]*>\s*)*Next', page) or \
            re.search(r'href="([^"]+)"[^>]*>\s*Next', page)
        if not match:
            break
        nxt = match.group(1)
        if nxt.startswith("http"):
            url = nxt
        elif nxt.startswith("/"):
            url = "https://ccel.org" + nxt
        else:
            url = "https://ccel.org/ccel/wesley/notes/" + nxt
        time.sleep(pause)


def assemble(rows):
    """rows: dicts with url, title and content in reading order. Returns {book: {chapter: text}} for the New Testament."""
    book_index = -1
    chapters = {}
    for row in rows:
        title = row["title"]
        if title.upper().startswith("NOTES ON"):
            book_index += 1
            continue
        number = roman_to_int(title)
        if number is None or "Introduction" in title or "/notes.i." not in row["url"]:
            continue
        if not 0 <= book_index < len(BOOKS) or not row["content"]:
            continue
        book = BOOKS[book_index]
        chapters.setdefault(book, {})
        chapters[book].setdefault(number, row["content"])
    problems = []
    for book, expected in zip(BOOKS, CHAPTERS):
        found = sorted(chapters.get(book, {}))
        if found != list(range(1, expected + 1)):
            problems.append("%s: found %d of %d chapters" % (book, len(found), expected))
    if problems:
        raise SystemExit("Chapter check failed:\n" + "\n".join(problems))
    return {"books": BOOKS, "chapters": {b: {str(k): v for k, v in sorted(chapters[b].items())} for b in BOOKS}}


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True)
    parser.add_argument("--from-jsonl", help="Use crawled pages saved as JSON lines instead of crawling.")
    args = parser.parse_args(argv)
    if args.from_jsonl:
        rows = [json.loads(line) for line in Path(args.from_jsonl).read_text(encoding="utf-8").splitlines() if line]
    else:
        rows = []
        for url, title, content in crawl(START):
            rows.append({"url": url, "title": title, "content": content})
            if "/notes.xxx" in url or "/notes.ii." in url:
                break
    data = assemble(rows)
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    Path(args.out).write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    print("chapters", sum(len(v) for v in data["chapters"].values()), file=sys.stderr)


if __name__ == "__main__":
    main()
