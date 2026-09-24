from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

MODULE_PATH = (
    ROOT
    / "tools"
    / "waves"
    / "prep"
    / "material-form-demand-census"
    / "census.py"
)


def _load_census():
    spec = importlib.util.spec_from_file_location(
        "material_form_demand_census", MODULE_PATH
    )
    if spec is None or spec.loader is None:
        raise RuntimeError("failed to load material-form demand census")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


census = _load_census()


class MaterialFormDemandCensusTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = census.build_document()

    def test_parse_cc_form(self) -> None:
        self.assertEqual(
            census.parse_cc_form("cruciblecraft:lead/small_casing"),
            ("lead", "small_casing"),
        )
        self.assertEqual(census.parse_cc_form("adamantium/round"), ("adamantium", "round"))
        self.assertIsNone(census.parse_cc_form("c:dusts/iron"))
        self.assertIsNone(census.parse_cc_form(None))

    def test_classify_helpers(self) -> None:
        gated = {"lead": {"ingot"}}
        prefixes = {
            "small_casing": {"generation_flag": "cruciblecraft:generates_small_casing"},
            "ingot": {"generation_flag": "cruciblecraft:generates_ingot"},
        }
        materials = {
            "lead": {"cruciblecraft:generates_ingot"},
            "iron": {"cruciblecraft:generates_ingot"},
        }
        self.assertEqual(
            census.classify_pair(
                "lead",
                "small_casing",
                gated=gated,
                prefixes=prefixes,
                materials=materials,
            ),
            "openable_dump_proven",
        )
        self.assertEqual(
            census.classify_pair(
                "lead",
                "ingot",
                gated=gated,
                prefixes=prefixes,
                materials=materials,
            ),
            "already_gated",
        )
        self.assertEqual(
            census.classify_pair(
                "lead",
                "nope",
                gated=gated,
                prefixes=prefixes,
                materials=materials,
            ),
            "no_prefix_json",
        )

    def test_openable_is_ungated_prefix_demand(self) -> None:
        gated = census.load_gated_forms(
            census.load_json(census.GATE)
        )
        prefixes = census.load_prefixes()
        openable = self.document["openable"]
        self.assertGreater(len(openable), 0)
        seen = set()
        for row in openable:
            key = (row["material"], row["form"])
            self.assertNotIn(key, seen)
            seen.add(key)
            self.assertIn(row["form"], prefixes)
            self.assertNotIn(row["form"], gated.get(row["material"], set()))
            self.assertIn(row["generation"], {"flagged", "dump_proven"})
            self.assertTrue(row["sources"])
        self.assertIn("openable_rows_by_form", self.document)
        self.assertGreater(
            self.document["openable_rows_by_form"].get("plate", 0),
            0,
        )

    def test_dump_demand_is_primary_and_retains_legacy_openable_pairs(self) -> None:
        counts = self.document["counts"]
        self.assertGreater(counts["dump_demand_pairs"], 0)
        self.assertGreater(counts["dump_demand_rows"], 0)
        legacy: dict[tuple[str, str], dict[str, object]] = {}
        not_form = census.Counter()
        census.collect_gap_demand(legacy, not_form)
        census.collect_catalog_demand(legacy, [])
        gate = census.load_gated_forms(census.load_json(census.GATE))
        prefixes = census.load_prefixes()
        materials = census.load_material_flags()
        old_openable = {
            key
            for key in legacy
            if census.classify_pair(
                key[0],
                key[1],
                gated=gate,
                prefixes=prefixes,
                materials=materials,
            )
            in {"openable_flagged", "openable_dump_proven"}
        }
        current_openable = {
            (row["material"], row["form"]) for row in self.document["openable"]
        }
        self.assertTrue(old_openable <= current_openable)

    def test_deferred_decisions_never_enter_openable(self) -> None:
        deferred = {
            (row["material"], row["form"])
            for row in self.document["deferred_by_decision"]
        }
        openable = {
            (row["material"], row["form"]) for row in self.document["openable"]
        }
        self.assertTrue(deferred.isdisjoint(openable))

    def test_gated_unresolved_is_not_a_reopen_queue(self) -> None:
        gated = census.load_gated_forms(
            census.load_json(census.GATE)
        )
        unresolved = self.document["gated_unresolved"]
        self.assertGreater(len(unresolved), 0)
        self.assertIn("small_casing", self.document["gated_unresolved_by_form"])
        self.assertIn("curved_plate", self.document["gated_unresolved_by_form"])
        for row in unresolved:
            self.assertIn(row["form"], gated.get(row["material"], set()))

    def test_unmapped_reasons_stay_out_of_openable(self) -> None:
        reasons = self.document["not_form_by_reason"]
        self.assertTrue(reasons)
        self.assertNotIn("unmapped", {row.get("generation") for row in self.document["openable"]})
        self.assertNotIn("obtain/nanofab-hosts", self.document["catalog_not_prefix"])
        self.assertNotIn(
            census.COPPER_FAMILY_BLOCKER,
            self.document["catalog_not_prefix"],
        )

    def test_full_generated_dump_is_not_the_queue(self) -> None:
        counts = self.document["counts"]
        self.assertLessEqual(counts["openable"], counts["dump_demand_pairs"])
        self.assertNotEqual(
            counts["ungated_generated_flag_pairs"],
            counts["dump_demand_pairs"],
        )
        self.assertEqual(self.document["status"], "MATERIAL_FORM_DEMAND_CENSUS")
        self.assertEqual(self.document["lane"], "prep")


if __name__ == "__main__":
    unittest.main()
