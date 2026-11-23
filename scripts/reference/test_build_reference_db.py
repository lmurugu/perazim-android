#!/usr/bin/env python3
"""Unit tests for the reference database build. Run: python3 -m unittest scripts/reference/test_build_reference_db.py"""

import io
import json
import os
import sqlite3
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock

import sys

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import build_reference_db as b  # noqa: E402


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


class KeysTest(unittest.TestCase):
    def test_verse_key_orders_canonically(self):
        self.assertLess(b.verse_key(43, 3, 16), b.verse_key(43, 3, 17))
        self.assertLess(b.verse_key(43, 9, 1), b.verse_key(43, 10, 1))
        self.assertLess(b.verse_key(43, 21, 25), b.verse_key(44, 1, 1))

    def test_openbible_reference_parses_book_abbreviations(self):
        self.assertEqual(b.parse_openbible_ref("Gen.1.1"), 1001001)
        self.assertEqual(b.parse_openbible_ref("1John.2.1"), 62002001)
        self.assertEqual(b.parse_openbible_ref("Rev.22.21"), 66022021)
        with self.assertRaises(ValueError):
            b.parse_openbible_ref("Gospel.1.1")


class CrossReferenceTest(unittest.TestCase):
    def test_ranges_and_negative_votes(self):
        with tempfile.TemporaryDirectory() as tmp:
            zip_path = Path(tmp) / "x.zip"
            body = ("# header\nFrom Verse\tTo Verse\tVotes\n"
                    "Gen.1.1\tPs.8.3\t77\n"
                    "Gen.1.1\tCol.1.16-Col.1.17\t178\n"
                    "Gen.1.1\tExod.20.11\t-42\n")
            with zipfile.ZipFile(zip_path, "w") as archive:
                archive.writestr("cross_references.txt", body)
            rows = list(b.parse_openbible_xrefs(zip_path))
        self.assertEqual(rows[0], (1001001, 19008003, 19008003, 77))
        self.assertEqual(rows[1], (1001001, 51001016, 51001017, 178))
        self.assertEqual(rows[2][3], -42)


class GeographyTest(unittest.TestCase):
    def test_places_skip_comments_and_bad_coordinates(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "merged.txt"
            write(path, "#ESV\tRoot\tLat\tLon\tVerses\tComment\n"
                         "Abana\t\t33.545097\t36.224661\t2 Kgs 5:12\tNow Barada River\n"
                         "Abel\t\t\t\t2 Sam 20:14\t\n")
            rows = list(b.parse_openbible_geo(path))
        self.assertEqual(rows[0][0], "Abana")
        self.assertAlmostEqual(rows[0][1], 33.545097)
        self.assertIsNone(rows[1][1])
        self.assertEqual(rows[1][3], "2 Sam 20:14")


class CcelTest(unittest.TestCase):
    HEADER = ("     ____________\n\n   Title\n     ____________\n     ____________\n\n"
              "   Alpha\n          first letter\n          more text\n\n"
              "   Aaron\n          the brother of Moses.\n\n"
              "   Abba\n          See Father.\n")
    FOOTER = ("\n   Ending\n          tail text\n"
              "References\n1. file:///ccel/x.html\n")

    def test_headword_entries_skip_header_and_footer(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "dict.txt"
            write(path, self.HEADER + self.FOOTER)
            items = b.ccel_headword_entries(path)
        self.assertEqual([h for h, _ in items], ["Alpha", "Aaron", "Abba", "Ending"])
        by_name = dict(items)
        self.assertEqual(by_name["Aaron"], "the brother of Moses.")
        self.assertEqual(by_name["Abba"], "See Father.")
        self.assertEqual(by_name["Alpha"], "first letter more text")

    def test_topical_entries_keep_subtopics_as_body(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "nave.txt"
            write(path, self.HEADER
                  + "   Aaron\n          -Lineage of Ex 6:16\n          -Marriage of Ex 6:23\n" + self.FOOTER)
            items = b.ccel_topical_entries(path)
        by_name = dict(items)
        self.assertIn("Lineage of Ex 6:16", by_name["Aaron"])
        self.assertIn("Marriage of Ex 6:23", by_name["Aaron"])


class EndToEndTest(unittest.TestCase):
    def test_build_writes_sources_entries_and_xrefs(self):
        with tempfile.TemporaryDirectory() as tmp:
            raw = Path(tmp) / "raw"
            write(raw / "ccel" / "dict.txt",
                  CcelTest.HEADER + CcelTest.FOOTER)
            (raw / "xref").mkdir(parents=True, exist_ok=True)
            with zipfile.ZipFile(raw / "xref" / "x.zip", "w") as archive:
                archive.writestr("cross_references.txt", "From Verse\tTo Verse\tVotes\nGen.1.1\tPs.8.3\t77\n")
            write(raw / "geo" / "merged.txt", "#h\nAbana\t\t1\t2\t2 Kgs 5:12\tc\n")
            register = {"sources": [
                {"id": "t-dict", "title": "Test dictionary", "author": "A", "category": "DICTIONARY",
                 "licence": "Public domain", "attribution": "Test", "theologyTag": "NONE", "tier": "default",
                 "file": "ccel/dict.txt", "parser": "ccel_headword"},
                {"id": "t-xref", "title": "Xrefs", "category": "CROSS_REFERENCE", "licence": "CC BY 4.0",
                 "attribution": "Test", "theologyTag": "NONE", "tier": "default",
                 "file": "xref/x.zip", "parser": "openbible_xrefs"},
                {"id": "t-geo", "title": "Places", "category": "GEOGRAPHY", "licence": "CC BY 4.0",
                 "attribution": "Test", "theologyTag": "NONE", "tier": "default",
                 "file": "geo/merged.txt", "parser": "openbible_geo"},
            ]}
            reg_path = Path(tmp) / "sources.json"
            reg_path.write_text(json.dumps(register), encoding="utf-8")
            out = Path(tmp) / "reference.db"
            with mock.patch.object(b, "SOURCES_JSON", reg_path):
                b.build(raw, out)
            conn = sqlite3.connect(str(out))
            try:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM sources").fetchone()[0], 3)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM entries WHERE source_id='t-dict'").fetchone()[0], 4)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM xrefs").fetchone()[0], 1)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM places").fetchone()[0], 1)
                row = conn.execute("SELECT body FROM entries WHERE headword = 'aaron' COLLATE NOCASE").fetchone()
                self.assertEqual(row[0], "the brother of Moses.")
                tables = {r[0] for r in conn.execute("SELECT name FROM sqlite_master")}
                self.assertNotIn("entries_fts", tables)
            finally:
                conn.close()
            self.assertFalse((out.parent / "reference.db.building").exists())


if __name__ == "__main__":
    unittest.main()


class CommentaryTest(unittest.TestCase):
    HEADER = "     ____________\n\n   Title\n     ____________\n     ____________\n\n"

    def test_roman_numerals_and_chunking(self):
        self.assertEqual(b.roman_to_int("XIV"), 14)
        self.assertEqual(b.roman_to_int("III"), 3)
        chunks = b._chunk_paragraphs(["aaaa bbbb", "cccc", "dddd eeee ff"], limit=10)
        self.assertEqual(chunks, ["aaaa bbbb", "cccc", "dddd eeee ff"])
        self.assertEqual(b._chunk_paragraphs(["one", "two"], limit=1800), ["one two"])

    def test_henry_chapters_get_their_own_entries(self):
        body = ("G E N E S I S\n\nIntroduction to the book.\n\nCHAP. I.\n\n"
                "In the beginning God created the heaven.\n\nSecond paragraph here.\n\n"
                "CHAP. II.\n\nThus the heavens were finished.\n")
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "mhc1.txt"
            write(path, self.HEADER + body)
            state = {}
            entries = b.ccel_commentary_henry_file(path, state)
        self.assertEqual(state["book"], "Genesis")
        labels = [label for label, _, _ in entries]
        self.assertEqual(labels, ["Genesis, introduction", "Genesis 1", "Genesis 2"])
        by_label = {label: body for label, body, _ in entries}
        self.assertEqual(by_label["Genesis 1"], "In the beginning God created the heaven. Second paragraph here.")
        self.assertEqual(by_label["Genesis 2"], "Thus the heavens were finished.")

    def test_henry_state_carries_book_across_volume_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            first = Path(tmp) / "mhc1.txt"
            second = Path(tmp) / "mhc2.txt"
            write(first, self.HEADER + "G E N E S I S\n\nCHAP. I.\n\nText one.\n")
            write(second, self.HEADER + "CHAP. I.\n\nText two continues.\n")
            state = {}
            b.ccel_commentary_henry_file(first, state)
            entries = b.ccel_commentary_henry_file(second, state)
        self.assertEqual(entries[0][0], "Genesis 1")
        self.assertEqual(entries[0][1], "Text two continues.")

    def test_wesley_sections_titled_in_capitals(self):
        body = ("NOTES ON THE GOSPEL ACCORDING TO ST. LUKE\n\nFirst paragraph of notes.\n\nSecond paragraph.\n\n"
                "NOTES ON THE ACTS OF THE APOSTLES\n\nAnother note.\n")
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "wesley_notes.txt"
            write(path, self.HEADER + body)
            entries = b.ccel_commentary_books(path)
        self.assertEqual(entries, [
            ("Notes on the Gospel According to St. Luke, part 1", "First paragraph of notes. Second paragraph."),
            ("Notes on the Acts of the Apostles, part 1", "Another note."),
        ])

    def test_wesley_captions_ending_in_comma_are_not_sections(self):
        body = ("THIS CONTAINS,\n\nA caption note.\n\n"
                "NOTES ON THE GOSPEL ACCORDING TO ST. LUKE\n\nA real note.\n\nMARK,\n\nAnother caption.\n")
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "wesley_notes.txt"
            write(path, self.HEADER + body)
            entries = b.ccel_commentary_books(path)
        # The caption before any section is dropped; "MARK," is not a heading, so it stays as body text.
        self.assertEqual(entries, [("Notes on the Gospel According to St. Luke, part 1", "A real note. MARK, Another caption.")])
        self.assertIsNone(b._capital_heading("THIS CONTAINS,"))
        self.assertEqual(b._capital_heading("NOTES ON THE GOSPEL ACCORDING TO ST. LUKE"),
                         "Notes on the Gospel According to St. Luke")

    def test_build_accepts_pending_tier_and_default_excludes_it(self):
        with tempfile.TemporaryDirectory() as tmp:
            raw = Path(tmp) / "raw"
            write(raw / "ccel" / "wesley_notes.txt", self.HEADER + "NOTES ON THE GOSPEL ACCORDING TO ST. LUKE\n\nA note.\n")
            fake = [{
                "id": "wesley-notes-nt", "title": "Wesley", "author": "John Wesley", "category": "commentary",
                "licence": "Public domain", "attribution": "Wesley", "theologyTag": "ARMINIAN_WESLEYAN_COMMENTARY_PENDING_LEVI",
                "tier": "pending", "parser": "ccel_commentary_books", "file": "ccel/wesley_notes.txt",
                "sizeBytes": 0,
            }]
            with mock.patch.object(b, "load_sources", return_value=fake):
                pending_db = Path(tmp) / "pending.db"
                b.build(raw, pending_db, tier="pending")
                default_db = Path(tmp) / "default.db"
                b.build(raw, default_db, tier="default")
            pending_rows = sqlite3.connect(pending_db).execute("SELECT COUNT(*) FROM entries").fetchone()[0]
            default_rows = sqlite3.connect(default_db).execute("SELECT COUNT(*) FROM entries").fetchone()[0]
        self.assertEqual(pending_rows, 1)
        self.assertEqual(default_rows, 0)
