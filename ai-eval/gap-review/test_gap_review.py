import importlib.util
import json
import os
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("gap_review", os.path.join(HERE, "gap_review.py"))
gap_review = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gap_review)


def record(question, reason="no-verified-source", count=1, route="routed:no-go:x", first=1, last=2):
    return {"schema": "perazim-gap-v1", "question": question, "reason": reason, "route": route,
            "app": "1.0 (24)", "first": first, "last": last, "count": count}


class GapReviewTest(unittest.TestCase):
    def test_redacts_again_and_keeps_only_safe_text(self):
        self.assertEqual("mail me at [email]", gap_review.redact_again("mail me at a@b.com"))
        self.assertEqual("my id [number] is", gap_review.redact_again("my id 23456789 is"))
        self.assertEqual("Is baptism needed? [link]", gap_review.redact_again("Is baptism needed? https://x.org/a"))

    def test_duplicates_are_merged_and_counted(self):
        rows, stats = gap_review.build([
            (record("Is baptism needed?", count=2, first=5, last=9), "a"),
            (record("is  BAPTISM needed", count=1, first=3, last=7), "b"),
        ], "")
        self.assertEqual(1, len(rows))
        self.assertEqual(3, rows[0]["count"])
        self.assertEqual(3, rows[0]["first"])
        self.assertEqual(9, rows[0]["last"])
        self.assertEqual(1, stats["unique"])

    def test_library_match_marks_access_failure_else_missing(self):
        library = "the holy spirit baptism is described in acts and romans. speaking of tongues."
        self.assertEqual("access-failure", gap_review.classify("What does the holy spirit baptism teach?", library))
        self.assertEqual("library-missing", gap_review.classify("What is the doctrine of purgatory?", library))

    def test_topics_group_questions(self):
        self.assertEqual("Baptism and communion", gap_review.topic_for("Is baptism needed for salvation?"))
        self.assertEqual("Other", gap_review.topic_for("Something unrelated"))

    def test_loader_skips_other_schemas_and_bad_lines(self):
        with tempfile.TemporaryDirectory() as d:
            path = os.path.join(d, "export.jsonl")
            with open(path, "w", encoding="utf-8") as fh:
                fh.write(json.dumps(record("Is baptism needed?")) + "\n")
                fh.write("not json\n")
                fh.write(json.dumps({"schema": "other"}) + "\n")
            rows, stats = gap_review.build(gap_review.load_records([d]), "")
            self.assertEqual(1, stats["read"])
            self.assertEqual(1, len(rows))

    def test_end_to_end_writes_markdown(self):
        with tempfile.TemporaryDirectory() as d:
            export = os.path.join(d, "e.jsonl")
            with open(export, "w", encoding="utf-8") as fh:
                fh.write(json.dumps(record("What is purgatory?")) + "\n")
            out = os.path.join(d, "review.md")
            gap_review.main(["--in", export, "--out", out])
            text = open(out, encoding="utf-8").read()
            self.assertIn("library-missing (1)", text)
            self.assertIn("What is purgatory?", text)


if __name__ == "__main__":
    unittest.main()
