"""Unit tests for build_vine_words.py. Run from the repository root:

    python3 -m unittest discover -s scripts/games/content -p "test_*.py"
"""

import importlib.util
import json
import random
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("build_vine_words", HERE / "build_vine_words.py")
bvw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bvw)


class ValidLayoutTest(unittest.TestCase):

    def test_crossing_words_are_valid(self):
        placements = [("VINE", 0, 0, True), ("NET", 0, 2, False)]
        self.assertTrue(bvw.valid_layout(placements))

    def test_words_touching_without_crossing_are_invalid(self):
        placements = [("NOT", 0, 0, True), ("BE", 0, 3, True)]
        self.assertFalse(bvw.valid_layout(placements))

    def test_parallel_neighbours_are_invalid(self):
        placements = [("CAT", 0, 0, True), ("DOG", 1, 0, True)]
        self.assertFalse(bvw.valid_layout(placements))

    def test_clashing_letters_are_invalid(self):
        placements = [("VINE", 0, 0, True), ("TREE", 0, 0, False)]
        self.assertFalse(bvw.valid_layout(placements))


class CandidateWordsTest(unittest.TestCase):

    def test_proper_names_and_stop_words_are_excluded(self):
        verses = [("Test 1:1", "And the Lord said unto Moses, the vine and the branch.")]
        words = bvw.candidate_words(verses, {"min_len": 3, "max_len": 8})
        self.assertIn("VINE", words)
        self.assertIn("BRANCH", words)
        self.assertNotIn("MOSES", words)
        self.assertNotIn("LORD", words)
        self.assertNotIn("AND", words)

    def test_length_band_is_respected(self):
        verses = [("Test 1:1", "vine branch fruit abide")]
        words = bvw.candidate_words(verses, {"min_len": 4, "max_len": 5})
        self.assertEqual(words, ["ABIDE", "FRUIT", "VINE"])  # "branch" is six letters, out of band


class PassageParsingTest(unittest.TestCase):

    def setUp(self):
        self.books = {"john": 43, "psalms": 19, "psalm": 19}
        self.kjv = {(43, 15, 1): "one", (43, 15, 2): "two", (43, 15, 3): "three", (43, 15, 4): "four"}

    def test_single_and_range_references(self):
        verses = bvw.passage_verses("John 15:1-3", self.books, self.kjv)
        self.assertEqual([label for label, _ in verses], ["John 15:1", "John 15:2", "John 15:3"])

    def test_more_than_three_verses_is_refused(self):
        with self.assertRaises(ValueError):
            bvw.passage_verses("John 15:1-4", self.books, self.kjv)

    def test_missing_verse_is_refused(self):
        with self.assertRaises(ValueError):
            bvw.passage_verses("John 15:9", self.books, self.kjv)


class GrowGridTest(unittest.TestCase):

    def test_grown_grid_is_valid_connected_and_within_side(self):
        pool = ["VINE", "BRANCH", "FRUIT", "ABIDE", "NEAR", "GRAPE", "EARTH", "HEART"]
        rng = random.Random(3)
        grown = bvw.build_grid(pool, 5, rng, 7)
        self.assertIsNotNone(grown)
        words, placements = grown
        self.assertEqual(len(set(words)), 5)
        self.assertTrue(bvw.valid_layout(placements))
        _, _, rows, cols = bvw.bounding_box(placements)
        self.assertLessEqual(max(rows, cols), 7)

    def test_same_seed_gives_same_grid(self):
        pool = ["VINE", "BRANCH", "FRUIT", "ABIDE", "NEAR", "GRAPE"]
        first = bvw.build_grid(pool, 4, random.Random(11), 7)
        second = bvw.build_grid(pool, 4, random.Random(11), 7)
        self.assertEqual(first, second)


class WheelTest(unittest.TestCase):

    def test_wheel_holds_the_most_each_letter_is_needed(self):
        wheel = bvw.wheel_letters(["VINE", "NEVER"], None)
        self.assertEqual(sorted(wheel), sorted("EEINRV"))  # two E, one of each other letter

    def test_bonus_adds_at_most_two_letters(self):
        need = bvw.Counter("VINE")
        self.assertIsNone(bvw.choose_bonus(["ZZZZZ"], ["VINE"], 20, random.Random(1), need))
        bonus = bvw.choose_bonus(["VINES"], ["VINE"], 20, random.Random(1), need)
        self.assertEqual(bonus, "VINES")


class ShippedFileTest(unittest.TestCase):
    """The committed level file must pass the same checks the build runs."""

    @classmethod
    def setUpClass(cls):
        path = bvw.OUT
        cls.document = json.loads(path.read_text(encoding="utf-8"))

    def test_sixty_levels_in_three_bands(self):
        levels = self.document["levels"]
        self.assertEqual(len(levels), bvw.LEVEL_COUNT)
        self.assertEqual({lv["band"] for lv in levels}, {1, 2, 3})

    def test_every_level_passes_the_rules(self):
        for level in self.document["levels"]:
            self.assertEqual(bvw.verify_level(level), [], f"level {level['level']}")


if __name__ == "__main__":
    unittest.main()
