"""Contract tests for the T42 remaining-family freeze."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t42_partition_freeze as builder
from tools import t42_common as common


class T42PartitionFreezeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.documents = builder.build()
        cls.catalog = cls.documents["catalog"]
        cls.freeze = cls.documents["freeze"]

    def test_remaining_catalog_covers_5305_without_duplicates(self) -> None:
        ids = [row["family_id"] for row in self.catalog["families"]]
        self.assertEqual(common.OPENING_EXECUTION_GAP, self.catalog["family_count"])
        self.assertEqual(common.OPENING_EXECUTION_GAP, len(ids))
        self.assertEqual(len(ids), len(set(ids)))
        self.assertEqual(ids, sorted(ids))
        self.assertEqual(4, len(self.catalog["combinatorial_remaining_family_ids"]))

    def test_exclude_set_fail_closed_when_prior_identity_removed(self) -> None:
        exclude = list(common.exclude_family_ids())
        self.assertEqual(50 + 29 + 22 + 7 + 13 + 292, len(exclude))
        removed = exclude.pop()
        rows = common.remaining_family_rows(exclude=exclude)
        self.assertEqual(common.OPENING_EXECUTION_GAP + 1, len(rows))
        self.assertTrue(any(row["family_id"] == removed for row in rows))
        catalog_ids = {row["family_id"] for row in self.catalog["families"]}
        self.assertNotIn(removed, catalog_ids)
        for family_id in common.t37_closed_family_ids()[:1]:
            self.assertNotIn(family_id, catalog_ids)

    def test_host_and_source_row_reconcile_to_t35(self) -> None:
        self.assertEqual(
            common.ORDINARY_OPTIONAL_ROWS,
            self.catalog["ordinary_source_rows"]
            + sum(
                int(row.get("expanded_count") or 0)
                for row in common.t35_families()
                if str(row.get("family_id")) in set(common.exclude_family_ids())
            ),
        )
        self.assertIn("cruciblecraft:drying", self.catalog["by_host"])
        self.assertEqual(
            152,
            self.catalog["by_host"]["cruciblecraft:drying"]["families"],
        )
        self.assertEqual(
            2,
            self.catalog["by_host"]["cruciblecraft:assembler"]["families"],
        )

    def test_freeze_pins_t41_5305_and_history_readonly(self) -> None:
        self.assertEqual("T42_PARTITION_FREEZE", self.freeze["status"])
        self.assertEqual(0, self.freeze["owns_families"])
        self.assertEqual(5305, self.freeze["t41_remaining_opening"])
        self.assertEqual("T41_READY", self.freeze["t41_status"])
        self.assertFalse(self.freeze["immutable_history"]["t35_files_rewritten"])
        self.assertIn("tools/t41_readiness.json", self.freeze["semantic_roots"])
        self.assertIn("tools/t35_recipe_families.json", self.freeze["semantic_roots"])

    def test_check_is_read_only_when_artifacts_exist(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t42_partition_freeze.json not generated")
        before = builder.OUTPUT.read_bytes()
        catalog_before = builder.CATALOG.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())
        self.assertEqual(catalog_before, builder.CATALOG.read_bytes())


if __name__ == "__main__":
    unittest.main()
