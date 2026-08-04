import importlib.util
import json
import re
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
        self.assertEqual(399, snapshot["java_source_test_count"])
        self.assertEqual(
            snapshot["java_source_test_count"],
            document["tests"]["java_unit_tests"]["tests"],
        )
        plan = (MODULE.ROOT / "CrucibleCraft-总体规划.md").read_text(
            encoding="utf-8"
        )
        match = re.search(r"Java / Python 单测 \| (\d+) / (\d+)", plan)
        self.assertIsNotNone(match)
        self.assertEqual(
            (snapshot["java_source_test_count"],) * 2,
            tuple(map(int, match.groups())),
        )
        t4 = document["t4_tool_acceptance"]
        self.assertEqual(21, t4["source_rules"])
        self.assertEqual(11, t4["tool_types"])
        self.assertEqual(3452, t4["expanded_recipes"])
        self.assertEqual(0, t4["unclassified"])
        self.assertEqual(0, t4["signature_collisions"])
        self.assertEqual(1020, t4["eligible_without_route"]["total"])
        self.assertEqual(
            208,
            t4["eligible_without_route"]["by_tool"]["pickaxe"],
        )
        self.assertEqual(
            9,
            t4["eligibility_predicate_sources"]["predicate_count"],
        )
        self.assertEqual(
            {"stone", "wood"},
            set(t4["identity_literals"]),
        )
        component = document["component_pipeline_acceptance"]
        self.assertEqual(31, component["shape_count"])
        self.assertEqual(
            {"playable": 20, "skipped": 42},
            component["template_classifications"],
        )
        self.assertEqual(8141, component["expanded_recipes"])
        self.assertEqual(2782, component["extruder_expanded_recipes"])
        self.assertEqual(0, component["shadowed_recipes"])
        self.assertEqual(
            2811,
            snapshot["trees"]["component_rule_generated"]["files"],
        )
        t5 = document["t5_chemical_acceptance"]
        self.assertEqual("SOURCE_REPLAY_VERIFIED", t5["readiness_status"])
        self.assertEqual(145, t5["terminal_dust_denominator"])
        self.assertEqual(
            {
                "decomposable_without_route": 36,
                "route_ready": 18,
                "route_tagged_but_quarantined": 57,
                "unresolved_deferred": 34,
            },
            t5["terminal_readiness_classifications"],
        )
        self.assertEqual(145, t5["terminal_dust_live_routes"])
        self.assertEqual(0, t5["terminal_dust_unresolved"])
        self.assertEqual(152, t5["generated_recipes"])
        self.assertEqual(15, t5["registered_chemical_fluids"])
        self.assertTrue(t5["closure_ready"])
        self.assertEqual(
            153,
            snapshot["trees"]["t5_chemical_generated"]["files"],
        )
        t7 = document["t7_material_fact_acceptance"]
        self.assertEqual("READY", t7["readiness_status"])
        self.assertTrue(t7["ledger_current"])
        self.assertEqual(62, t7["classified_tags"])
        self.assertEqual(0, t7["unclassified_tags"])
        self.assertEqual(
            {"gem_to_dust": 94, "ingot_to_dust": 126},
            t7["mortar_rule_counts"],
        )
        self.assertEqual(
            220, t7["fact_counts"]["new_mortar_rule_expansion_count"]
        )
        self.assertEqual(
            17189,
            t7["runtime_publication"][
                "post_t7_all_published_recipes"
            ],
        )
        self.assertTrue(t7["within_publication_budget"])
        self.assertFalse(
            document["artifact_policy"]["ordinary_ci_requires_local_cache"]
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
            "tools/build_component_rules.py",
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
