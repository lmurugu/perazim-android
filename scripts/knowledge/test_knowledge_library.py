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


class PastoralTopicsTest(unittest.TestCase):
    def test_committed_topics_match_the_kjv_and_stay_fail_closed(self):
        import build_pastoral_topics as bpt
        rebuilt = bpt.build()
        path = bpt.OUT
        with open(path, encoding="utf-8") as f:
            committed = json.load(f)
        self.assertEqual(rebuilt, committed, "run build_pastoral_topics.py after editing the topics")
        for topic in committed["topics"]:
            self.assertEqual(topic["status"], "DRAFT_PENDING_LEVI")
            self.assertTrue(topic["verses"])
            for v in topic["verses"]:
                self.assertEqual(v["translation"], "KJV")


class DoctrineEntriesTest(unittest.TestCase):
    def test_committed_entries_match_the_kjv_and_stay_fail_closed(self):
        import build_doctrine_entries as bde
        with open(bde.OUT, encoding="utf-8") as f:
            committed = json.load(f)
        self.assertEqual(bde.build(), committed, "run build_doctrine_entries.py after editing the entries")
        for entry in committed["entries"]:
            self.assertEqual(entry["status"], "APPROVED_LEVI")
            self.assertEqual(entry["approval"]["approver"], "Levi")
            self.assertTrue(entry["verses"])


if __name__ == "__main__":
    unittest.main()


REMOVED_SOURCE_IDS = {"ante-nicene-fathers", "npnf", "comparative-kb-1.0.0", "comparative-kb-1.5.22"}
REMOVED_OTHER_FAITH_TERMS = ("Catechism", "Qur'an", "Council of Trent", "Hindu", "Buddhis", "Latter-day Saint",
                             "Jehovah's Witness", "Seventh-day Adventist", "Christian Science")


class RemovedSourcesTest(unittest.TestCase):
    """Sources removed 2026-11-25 must not come back into the register, the package or the reference build."""

    ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))

    def _read(self, rel):
        with open(os.path.join(self.ROOT, rel), encoding="utf-8") as f:
            return f.read()

    def test_removed_candidates_are_gone_from_the_source_list(self):
        sources = json.loads(self._read("scripts/reference/sources.json"))
        listed = {c.get("id") for c in sources.get("candidates", []) + sources.get("sources", [])}
        self.assertEqual(listed & REMOVED_SOURCE_IDS, set())

    def test_removals_are_recorded_with_reasons(self):
        sources = json.loads(self._read("scripts/reference/sources.json"))
        recorded = {r["id"]: r["reason"] for r in sources.get("removed", [])}
        for source_id in REMOVED_SOURCE_IDS:
            self.assertIn(source_id, recorded)
            self.assertTrue(recorded[source_id].strip())

    def test_register_carries_the_removals(self):
        register = json.loads(self._read("src/main/assets/ai/library/sources/register.json"))
        self.assertEqual({r["id"] for r in register["removedSources"]}, REMOVED_SOURCE_IDS)
        self.assertEqual(set(s["id"] for s in register["sources"]) & REMOVED_SOURCE_IDS, set())

    def test_unsigned_package_has_no_other_faith_documents(self):
        text = self._read("ai-eval/knowledge-package-unsigned/package.unsigned.json")
        self.assertNotIn("comparative", text)
        for term in REMOVED_OTHER_FAITH_TERMS:
            self.assertFalse(term in text, "package still mentions " + term)

    def test_comparative_source_file_is_not_present(self):
        self.assertFalse(os.path.exists(os.path.join(self.ROOT, "ai-eval/knowledge-package-unsigned/source/comparative-kb-1.5.22.json")))
        self.assertFalse(os.path.exists(os.path.join(self.ROOT, "src/main/assets/ai/guide/comparative/comparative-kb.json")))
