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



OTHER_FAITH_TERMS = ("Catechism", "Qur'an", "Council of Trent", "Hindu", "Buddhis", "Latter-day Saint",
                     "Jehovah's Witness", "Seventh-day Adventist", "Christian Science")


class ClassifiedSourcesTest(unittest.TestCase):
    """Levi, 2026-11-25: Bible first; creeds and early fathers only where they agree with Bible-centred Protestant,
    Pentecostal and Evangelical teaching; other faiths and groups are false-doctrine examples, never citable sources."""

    ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))

    def _read(self, rel):
        with open(os.path.join(self.ROOT, rel), encoding="utf-8") as f:
            return f.read()

    def test_church_fathers_are_tier_two_candidates(self):
        sources = json.loads(self._read("scripts/reference/sources.json"))
        tiers = {c.get("id"): c.get("tier") for c in sources.get("candidates", [])}
        self.assertEqual(tiers.get("ante-nicene-fathers"), "TIER_2_CONDITIONAL")
        self.assertEqual(tiers.get("npnf"), "TIER_2_CONDITIONAL")

    def test_fathers_and_other_faith_material_are_not_approved(self):
        approvals = json.loads(self._read("src/main/assets/reference/approvals.json"))["approvedSourceIds"]
        for source_id in ("ante-nicene-fathers", "npnf", "comparative-kb-1.5.22"):
            self.assertNotIn(source_id, approvals)

    def test_register_lists_only_approved_or_candidate_sources(self):
        register = json.loads(self._read("src/main/assets/ai/library/sources/register.json"))
        approved = {s["id"] for s in register["sources"] if s["status"] == "APPROVED_FOR_GUIDE"}
        self.assertTrue({"smith-bible-dictionary", "creed-nicene-constantinopolitan-381"} <= approved)

    def test_comparative_entries_are_classified_and_at_least_two_passages(self):
        kb = json.loads(self._read("src/main/assets/ai/guide/comparative/comparative-kb.json"))
        for e in kb["entries"]:
            self.assertIn(e["classification"], ("FALSE_DOCTRINE_EXAMPLE", "POLICY_RULE"))
            self.assertGreaterEqual(len(set(e["anchors"])), 2, e["id"])

    def test_package_marks_other_faith_material_as_false_doctrine_examples_only(self):
        package = json.loads(self._read("ai-eval/knowledge-package-unsigned/package.unsigned.json"))["payload"]
        for d in package["documents"]:
            if d["sourceId"].startswith("comparative-"):
                self.assertEqual(d["authorityClass"], "FALSE_DOCTRINE_EXAMPLE", d["sourceId"])
        text = json.dumps(package)
        for doc in package["documents"]:
            if doc["authorityClass"] != "FALSE_DOCTRINE_EXAMPLE":
                for term in OTHER_FAITH_TERMS:
                    self.assertFalse(term in doc["text"], doc["sourceId"] + " mentions " + term)
        self.assertIn("comparative-", text)
