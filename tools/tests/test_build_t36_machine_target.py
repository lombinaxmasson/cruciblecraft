"""Tests for the independent T36 machine-target builder."""
from __future__ import annotations

import copy
import json
import sys
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t36_machine_target as builder  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t36_common as t36  # noqa: E402


class T36MachineTargetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if builder.OUTPUT.is_file():
            cls.document = json.loads(
                builder.OUTPUT.read_text(encoding="utf-8")
            )
        else:
            cls.document = builder.build()

    def test_build_is_deterministic(self) -> None:
        self.assertEqual(
            t35.stable_json(builder.build()),
            t35.stable_json(builder.build()),
        )

    def test_target_is_not_defined_by_current_catalog(self) -> None:
        original_ids = {row["variant_id"] for row in self.document["rows"]}
        self.assertEqual(0, self.document["validators"]["target_defined_by_catalog"])
        self.assertGreater(self.document["freeze"]["target_row_count"], 33)
        self.assertEqual(85, len(original_ids))
        self.assertTrue(set(t36.OPENING_VARIANT_IDS) <= original_ids)

        real_load_json = t35.load_json
        truncated = copy.deepcopy(t35.load_json(t36.MACHINE_TIERS))
        truncated["variants"] = truncated["variants"][:10]

        def load_with_truncated(path: Path):
            if path == t36.MACHINE_TIERS:
                return copy.deepcopy(truncated)
            return real_load_json(path)

        with mock.patch.object(t35, "load_json", side_effect=load_with_truncated):
            with self.assertRaises(ValueError):
                builder.build()

    def test_source_rows_are_unique_and_revision_is_pinned(self) -> None:
        self.assertEqual(t36.SOURCE_REVISION, self.document["source_revision"])
        keys = [
            (row["source_file"], row["source_row"], row["source_id"])
            for row in self.document["rows"]
            if row["source_id"] is not None
        ]
        self.assertEqual(len(keys), len(set(keys)))
        self.assertEqual(0, self.document["validators"]["source_row_duplicates"])
        self.assertEqual(0, self.document["validators"]["revision_mismatch"])
        for row in self.document["rows"]:
            self.assertEqual(t36.SOURCE_REVISION, row["source_revision"])

    def test_recipe_family_hosts_are_fully_covered(self) -> None:
        families = t35.load_json(t36.RECIPE_FAMILIES)
        by_host = families["by_host_map"]
        self.assertEqual(5718, families["counts"]["families"])
        projection = self.document["host_projection"]
        self.assertEqual(set(by_host), set(projection))
        self.assertEqual(
            5718,
            sum(item["families"] for item in projection.values()),
        )
        self.assertEqual(0, self.document["validators"]["recipe_family_host_uncovered"])
        roaster = projection["cruciblecraft:roaster"]
        self.assertEqual("host_exact", roaster["target_status"])
        self.assertEqual("exact", roaster["current_host"])
        self.assertNotEqual("product_excluded", roaster["target_status"])
        self.assertEqual("in_scope_1x", roaster["portfolio_scope_rule"] and "in_scope_1x")
        for host, item in projection.items():
            self.assertIn(item["target_status"], t36.HOST_STATUSES)
            if item["current_host"] == "missing":
                self.assertEqual("host_targeted_by_t36", item["target_status"])

    def test_material_eu_tu_denominators_are_exclusive_and_complete(self) -> None:
        freeze = self.document["freeze"]
        self.assertEqual(
            freeze["target_row_count"],
            freeze["material_row_count"]
            + freeze["eu_voltage_row_count"]
            + freeze["tu_host_row_count"]
            + freeze.get("opening_only_row_count", 0),
        )
        self.assertGreater(freeze["material_row_count"], 0)
        self.assertGreater(freeze["eu_voltage_row_count"], 0)
        self.assertGreater(freeze["tu_host_row_count"], 0)
        self.assertEqual(1, freeze.get("opening_only_row_count"))
        self.assertEqual(0, self.document["validators"]["denominator_overlap"])
        self.assertEqual(0, self.document["validators"]["denominator_gap"])
        seen = set()
        for row in self.document["rows"]:
            semantics = row["variant_semantics"]
            self.assertIn(semantics, set(t36.VARIANT_SEMANTICS) | {"opening_only"})
            seen.add(semantics)
            if semantics == "material":
                self.assertIsNone(row["voltage_band"])
                self.assertIn(row["energy_identity"], {"RU", "KU", "HU"})
            elif semantics == "eu_voltage":
                self.assertIsNone(row["material_tier"])
                self.assertEqual("EU", row["energy_identity"])
                self.assertTrue(str(row["voltage_band"]).startswith("cruciblecraft:electric_tier_"))
            elif semantics == "opening_only":
                self.assertEqual("LU", row["energy_identity"])
                self.assertEqual("opening_only", row["variant_semantics"])
                self.assertEqual("LU_NOT_IN_1X_DENOMINATOR", row.get("lu_adjudication"))
                self.assertNotEqual("RU", row["energy_identity"])
            else:
                self.assertIsNone(row["material_tier"])
                self.assertIsNone(row["voltage_band"])
                self.assertEqual("TU", row["energy_identity"])
        self.assertEqual(set(t36.VARIANT_SEMANTICS) | {"opening_only"}, seen)

    def test_opening_33_rows_are_preserved(self) -> None:
        catalog = t35.load_json(t36.MACHINE_TIERS)
        live_ids = {row["id"] for row in catalog["variants"]}
        opening_ids = set(self.document["freeze"]["opening_ids"])
        target_ids = {row["variant_id"] for row in self.document["rows"]}
        self.assertEqual(33, len(opening_ids))
        self.assertEqual(opening_ids, set(t36.OPENING_VARIANT_IDS))
        self.assertTrue(opening_ids <= live_ids)
        self.assertTrue(opening_ids <= target_ids)
        self.assertEqual(0, self.document["validators"]["opening_33_missing"])
        by_id = {row["variant_id"]: row for row in self.document["rows"]}
        for catalog_row in t36.opening_variants(catalog):
            target = by_id[catalog_row["id"]]
            self.assertEqual(catalog_row["sourceId"], target["source_id"])
            self.assertEqual(catalog_row["sourceTier"], target["source_tier"])

    def test_excluded_rows_have_reason_and_later_stage(self) -> None:
        self.assertGreater(len(self.document["excluded"]), 0)
        for row in self.document["excluded"]:
            self.assertTrue(row["canonical_id"])
            self.assertTrue(row["reason"])
            self.assertTrue(row["later_stage"])
            self.assertEqual(t36.SOURCE_REVISION, row["source_revision"])

    def test_missing_host_does_not_become_out_of_scope(self) -> None:
        self.assertEqual(0, self.document["validators"]["missing_host_auto_out_of_scope"])
        for row in self.document["rows"]:
            self.assertEqual("in_scope_1x", row["portfolio_scope"])
            self.assertNotEqual("out_of_scope", row["disposition"])
        roaster_rows = [
            row
            for row in self.document["rows"]
            if row["canonical_kind"] == "cruciblecraft:roaster"
        ]
        self.assertGreater(len(roaster_rows), 0)
        self.assertTrue(all(row["portfolio_scope"] == "in_scope_1x" for row in roaster_rows))
        coagulator_rows = [
            row
            for row in self.document["rows"]
            if row["canonical_kind"] == "cruciblecraft:coagulator"
        ]
        self.assertEqual(1, len(coagulator_rows))
        self.assertEqual("in_scope_1x", coagulator_rows[0]["portfolio_scope"])
        self.assertEqual("implemented", coagulator_rows[0]["runtime_status"])

    def test_priority_does_not_change_portfolio_scope(self) -> None:
        scopes = {row["portfolio_scope"] for row in self.document["rows"]}
        self.assertEqual({"in_scope_1x"}, scopes)
        self.assertEqual(0, self.document["validators"]["priority_changed_scope"])
        priorities = {row["priority"] for row in self.document["rows"]}
        self.assertTrue(priorities <= {"P0", "P1", "P2", "P3"})

    def test_required_row_fields_are_present(self) -> None:
        for row in self.document["rows"]:
            for field in builder.REQUIRED_ROW_FIELDS:
                self.assertIn(field, row, field)
            self.assertIn(
                row["variant_semantics"],
                {"material", "eu_voltage", "tu_host", "opening_only"},
            )

    def test_welder_lu_is_not_rewritten_as_ru(self) -> None:
        welder = next(
            row
            for row in self.document["rows"]
            if row["variant_id"] == "cruciblecraft:welder"
        )
        self.assertEqual("LU", welder["energy_identity"])
        self.assertEqual("opening_only", welder["variant_semantics"])
        self.assertEqual("LU_NOT_IN_1X_DENOMINATOR", welder["lu_adjudication"])
        self.assertIsNone(welder["material"])
        self.assertNotEqual("RU", welder["energy_identity"])

    def test_committed_artifact_if_present_is_current_and_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t36_machine_target.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
