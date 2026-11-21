"""Shared KJV lookup for game content checks. Reads the bundled KJV.tsv.gz (public domain)."""
import gzip
import os
import re
import unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
KJV_PATH = os.path.join(HERE, "..", "..", "src", "main", "assets", "bible", "text", "KJV.tsv.gz")

BOOKS = [
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
    "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
    "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
    "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
    "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
    "Malachi", "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
    "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
    "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
    "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation",
]
# Accept common spellings used in the app ("Psalm", "Song of Songs", "Revelations").
ALIASES = {"psalm": "Psalms", "song of songs": "Song of Solomon", "revelations": "Revelation",
           "canticles": "Song of Solomon", "philemon": "Philemon", "acts of the apostles": "Acts"}
BOOK_NUM = {name.lower(): i + 1 for i, name in enumerate(BOOKS)}
BOOK_NUM.update({k: BOOK_NUM[v.lower()] for k, v in ALIASES.items()})

_verses = None


def norm(text):
    text = unicodedata.normalize("NFKC", text).lower()
    text = text.replace("’", "'").replace("‘", "'")
    text = re.sub(r"[^a-z0-9' ]+", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def load():
    global _verses
    if _verses is None:
        _verses = {}
        with gzip.open(KJV_PATH, "rt", encoding="utf-8") as fh:
            for line in fh:
                parts = line.rstrip("\n").split("\t")
                if len(parts) < 4:
                    continue
                _verses[(int(parts[0]), int(parts[1]), int(parts[2]))] = parts[3]
    return _verses


REF_RE = re.compile(r"^(?P<book>(?:[1-3] )?[A-Za-z ]+?)\s+(?P<ch>\d+):(?P<v1>\d+)(?:\s*[–-]\s*(?P<v2>\d+))?$")


def parse_ref(ref):
    """Parse 'Book C:V' or 'Book C:V–W' into (book_num, chapter, [verse numbers]). Raises ValueError."""
    m = REF_RE.match(ref.strip())
    if not m:
        raise ValueError("unparseable reference: " + ref)
    book = m.group("book").strip().lower()
    if book not in BOOK_NUM:
        raise ValueError("unknown book in: " + ref)
    v1 = int(m.group("v1"))
    v2 = int(m.group("v2")) if m.group("v2") else v1
    if v2 < v1:
        raise ValueError("reversed range: " + ref)
    return BOOK_NUM[book], int(m.group("ch")), list(range(v1, v2 + 1))


def verse_text(ref):
    """Return the KJV text for a single-verse or range reference (verses joined by a space)."""
    book, ch, nums = parse_ref(ref)
    table = load()
    out = []
    for n in nums:
        key = (book, ch, n)
        if key not in table:
            raise ValueError("verse not in KJV: " + ref)
        out.append(table[key])
    return " ".join(out)


def canonical_ref(ref):
    book, ch, nums = parse_ref(ref)
    name = BOOKS[book - 1]
    return f"{name} {ch}:{nums[0]}" if len(nums) == 1 else f"{name} {ch}:{nums[0]}–{nums[-1]}"
