#!/usr/bin/env python3
"""Contract tests for T47 forward-v2 identity/runtime append-only wiring."""
from __future__ import annotations

import unittest

from tools import t35_common as t35
from tools.recipe_bulk import identity as identity_v1
from tools.recipe_bulk import identity_v2
from tools.recipe_bulk import runtime_v2
from tools.recipe_bulk.waves import FORWARD_COMPILE_ORDER, WAVES, recipe_wave


class T47ForwardV2Test(unittest.TestCase):
    def test_delta_order_keeps_t46_first(self) -> None:
        self.assertEqual("T46", identity_v2.DELTA_ORDER[0])
        self.assertEqual("T47", identity_v2.DELTA_ORDER[1])
        self.assertEqual("T46", runtime_v2.DELTA_ORDER[0])
        self.assertEqual("T47", runtime_v2.DELTA_ORDER[1])

    def test_identity_v2_includes_t47_and_keeps_v1(self) -> None:
        v1 = t35.load_json(identity_v2.V1_PATH)
        composed = identity_v2.compose()
        self.assertEqual("GLOBAL_BUILD_IDENTITY_LEDGER_V2", composed["status"])
        self.assertEqual(
            v1["record_count"],
            composed["composition"]["base_record_count"],
        )
        self.assertEqual("GLOBAL_BUILD_IDENTITY_LEDGER_V1", v1["status"])
        self.assertEqual("pass", composed["composition"]["conflict_check"])
        consumed = [
            row["wave_id"] for row in composed["composition"]["consumed_deltas"]
        ]
        self.assertEqual(["T46", "T47"], consumed[:2])
        self.assertIn("T47", consumed)
        t47_records = [
            row
            for row in composed["records"]
            if "T47" in (row.get("authorities") or [])
        ]
        self.assertGreaterEqual(len(t47_records), 283 * 2 + 3)
        self.assertTrue(
            any(str(row["source_key"]).startswith("T47|item:") for row in t47_records)
        )
        self.assertTrue(
            any(str(row["source_key"]).startswith("item:gregtech:") for row in t47_records)
        )
        self.assertTrue(
            any(str(row["source_key"]).startswith("fluid:") for row in t47_records)
        )

    def test_v1_identity_module_is_unchanged(self) -> None:
        self.assertEqual("GLOBAL_BUILD_IDENTITY_LEDGER_V1", identity_v1.STATUS)
        v1 = t35.load_json(identity_v2.V1_PATH)
        self.assertEqual("GLOBAL_BUILD_IDENTITY_LEDGER_V1", v1["status"])
        self.assertEqual(1, v1["schema_version"])
        self.assertNotIn(
            "T47",
            {wave_id for wave_id, _path in identity_v1.OPERAND_MAPS},
        )

    def test_runtime_t47_groups_do_not_duplicate_t46(self) -> None:
        composed = runtime_v2.compose()
        groups = [str(row.get("publication_group") or "") for row in composed["groups"]]
        self.assertEqual(1, groups.count("cruciblecraft:t46_bath_mte"))
        self.assertEqual(1, groups.count("cruciblecraft:t47_bath_exact"))
        self.assertEqual(1, groups.count("cruciblecraft:t47_bath_exact_multi"))
        self.assertNotIn("cruciblecraft:t46_bath_mte", [
            str(row.get("publication_group") or "")
            for row in runtime_v2.load_delta("T47").get("groups") or []
        ])
        self.assertTrue(composed["composition"]["historical_membership_roots_unchanged"])
        self.assertEqual(12, composed["composition"]["base_group_count"])
        self.assertGreaterEqual(composed["composition"]["composed_group_count"], 15)

    def test_wave_spec_uses_candidate_lock_counts_without_lock(self) -> None:
        spec = recipe_wave("T47")
        self.assertEqual("lock_relation_set", spec.archetype)
        self.assertEqual("cruciblecraft:t47_bath_exact", spec.default_publication_group)
        lock_path = t35.TOOLS / "t47_production_lock.json"
        if lock_path.is_file():
            production = t35.load_json(lock_path).get("production") or {}
            self.assertEqual(int(production["family_count"]), spec.expected_family_count)
            self.assertEqual(int(production["relation_count"]), spec.expected_relation_count)
        else:
            self.assertEqual(395, spec.expected_family_count)
            self.assertEqual(13708, spec.expected_relation_count)
        self.assertIn("T47", FORWARD_COMPILE_ORDER)
        self.assertLess(
            FORWARD_COMPILE_ORDER.index("T46"),
            FORWARD_COMPILE_ORDER.index("T47"),
        )
        self.assertIs(WAVES["T47"], spec)


if __name__ == "__main__":
    unittest.main()
