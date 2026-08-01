import importlib.util
import json
import sys
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "verify_full_verification_report",
    TOOLS / "verify_full_verification_report.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class FullVerificationReportTest(unittest.TestCase):
    def test_committed_report_matches_current_tooling_snapshot(self):
        document = json.loads(MODULE.REPORT.read_text(encoding="utf-8"))
        snapshot = MODULE.current_snapshot()
        self.assertEqual(
            [],
            MODULE.validate_report_document(document, snapshot),
        )
        self.assertEqual(
            snapshot["python_test_count"],
            document["tests"]["python_unit_tests"]["tests"],
        )

    def test_stale_ready_report_is_rejected(self):
        document = json.loads(MODULE.REPORT.read_text(encoding="utf-8"))
        snapshot = MODULE.current_snapshot()
        document["status"] = "READY"
        document["ready_binding"] = {
            "tooling_snapshot_sha256": "stale",
        }
        errors = MODULE.validate_report_document(document, snapshot)
        self.assertTrue(
            any("READY is not bound" in error for error in errors),
            errors,
        )

    def test_tooling_or_process_hash_tampering_is_rejected(self):
        document = json.loads(MODULE.REPORT.read_text(encoding="utf-8"))
        snapshot = MODULE.current_snapshot()
        for path in (
            "tools/compare_gt6_recipes.py",
            "tools/tests/test_compare_gt6_recipes.py",
            "tools/gt6_process_expectations.json",
        ):
            with self.subTest(path=path):
                changed = json.loads(json.dumps(document))
                changed["artifact_sha256"][path] = "stale"
                errors = MODULE.validate_report_document(changed, snapshot)
                self.assertTrue(
                    any("stale artifact hashes" in error for error in errors),
                    errors,
                )

    def test_ore_closure_summary_cannot_be_hand_edited(self):
        document = json.loads(MODULE.REPORT.read_text(encoding="utf-8"))
        snapshot = MODULE.current_snapshot()
        document["ore_pipeline_acceptance"]["unclassified_count"] = 1

        errors = MODULE.validate_report_document(document, snapshot)
        self.assertTrue(
            any("not derived from current artifacts" in error for error in errors),
            errors,
        )


if __name__ == "__main__":
    unittest.main()
