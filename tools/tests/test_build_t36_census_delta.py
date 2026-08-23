"""Contract tests for the T36 census overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t36_census_delta as builder  # noqa: E402
from tools import t36_common as t36  # noqa: E402


class T36CensusDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t35_foundation_denominators_are_unchanged(self) -> None:
        self.assertEqual(builder.T35_FOUNDATION, self.document["t35_foundation"])
        self.assertTrue(self.document["t35_foundation_unchanged"])

    def test_target_equals_catalog(self) -> None:
        self.assertEqual([], self.document["sets"]["missing"])
        self.assertEqual([], self.document["delta"]["catalog_minus_target"])
        self.assertEqual(0, self.document["validators"]["target_catalog_equal"])
        self.assertEqual(85, self.document["closing"]["target_rows"])
        self.assertEqual(33, self.document["opening"]["catalog_rows"])

    def test_opening_ids_remain(self) -> None:
        catalog_ids = set(self.document["sets"]["catalog"])
        self.assertTrue(set(t36.OPENING_VARIANT_IDS) <= catalog_ids)
        self.assertEqual(0, self.document["validators"]["opening_33_present"])

    def test_unowned_missing_hosts_are_empty(self) -> None:
        self.assertEqual([], self.document["host_projection"]["unowned_missing_hosts"])
        self.assertEqual(0, self.document["validators"]["unowned_missing_hosts"])

    def test_runtime_ids_are_measured_not_copied(self) -> None:
        self.assertIn("ModBlocks", self.document["closing"]["runtime_source"])
        self.assertEqual(85, len(self.document["closing"]["runtime_machine_ids"]))
        self.assertEqual(
            set(self.document["sets"]["catalog"]),
            set(self.document["closing"]["runtime_machine_ids"]),
        )
        publication = self.document["delta"]["publication"]
        self.assertGreater(publication["eager"], 0)
        self.assertTrue(publication["measured"])

    def test_roaster_family_scope_is_reissued(self) -> None:
        roaster = self.document["family_scope"]["hosts"]["cruciblecraft:roaster"]
        self.assertEqual("host_exact", roaster["closing_host_status"])
        self.assertEqual("exact", roaster["closing_current_host"])
        self.assertEqual("in_scope_1x", roaster["closing_scope"])
        self.assertEqual(29, roaster["families"])
        self.assertEqual(29, len(roaster["family_ids"]))
        self.assertIn("cruciblecraft:roaster", self.document["family_scope"]["reissued_hosts"])
        self.assertFalse(self.document["family_scope"]["t35_files_rewritten"])

    def test_t14_pending_axes_are_not_zero_filled(self) -> None:
        pending = [
            row
            for row in self.document["t14_load"]["axes"]
            if row["pending"]
        ]
        self.assertGreater(len(pending), 0)
        for row in pending:
            self.assertIsNone(row["delta"])
        self.assertFalse(self.document["t14_load"]["hard_ceiling_raised"])

    def test_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t36_census_delta.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
