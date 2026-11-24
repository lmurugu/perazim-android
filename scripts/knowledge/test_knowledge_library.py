"""Host tests for the knowledge library scripts. Run: python3 -m unittest scripts/knowledge/test_knowledge_library.py"""
import json
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import build_scripture_index as bsi  # noqa: E402
import check_eval_references as cer  # noqa: E402


class ScriptureIndexTest(unittest.TestCase):
    def test_committed_index_matches_the_bundled_kjv(self):
        rebuilt = bsi.build()
        with open(bsi.OUT, encoding="utf-8") as f:
            committed = json.load(f)
        self.assertEqual(rebuilt, committed, "run build_scripture_index.py after changing the KJV source")

    def test_sixty_six_books_and_all_verses(self):
        index = bsi.build()
        self.assertEqual(len(index["books"]), 66)
        self.assertEqual(index["totalVerses"], 31102)


class ReferenceCheckTest(unittest.TestCase):
    def setUp(self):
        self.table = cer.Table()

    def test_rejects_non_existent_chapter_and_verse(self):
        self.assertFalse(self.table.check("Ezekiel 50:4")[0])
        self.assertFalse(self.table.check("Ezekiel 48:36")[0])
        self.assertFalse(self.table.check("Jude 2:1")[0])

    def test_accepts_real_references(self):
        self.assertTrue(self.table.check("Ezekiel 48:35")[0])
        self.assertTrue(self.table.check("Psalm 119:176")[0])
        self.assertTrue(self.table.check("1 Corinthians 13:13")[0])

    def test_eval_cases_have_no_unexplained_invalid_references(self):
        invalid = [r for r in cer.scan() if r[2] is False]
        # The only invalid reference in the set is the deliberate trick prompt FW-079 (Ezekiel 50:4).
        self.assertEqual([r[0] for r in invalid], ["FW-079"])


if __name__ == "__main__":
    unittest.main()
