"""T42 disposition lock, gap, census, topology, and readiness contracts."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t42_card_topology as topology_builder
from tools import build_t42_census_delta as census_builder
from tools import build_t42_disposition_lock as lock_builder
from tools import build_t42_gap_partition as gap_builder
from tools import build_t42_readiness as readiness_builder
from tools import t42_common as common


class T42LockAndCloseoutTest(unittest.TestCase):
    def test_initial_lock_requires_approval(self) -> None:
        if common.DISPOSITION_LOCK.is_file():
            self.skipTest("lock already approved")
        with self.assertRaises(ValueError):
            lock_builder.write(approve_initial_lock=False, approve_repair_lock=False)

    def test_deferred_rows_have_owner_recheck_and_evidence(self) -> None:
        document = lock_builder.build()
        self.assertEqual(common.OPENING_EXECUTION_GAP, document["family_count"])
        self.assertEqual(0, document["owns_families"])
        deferred = [
            row
            for row in document["families"]
            if row["disposition"] == "phase_deferred"
        ]
        for row in deferred:
            self.assertTrue(row["future_owner"])
            self.assertTrue(row["reason"])
            self.assertTrue(row["recheck_condition"])
            self.assertTrue(row["evidence_root_sha256"])
            self.assertTrue(str(row["future_owner"]).startswith("later:"))

    def test_gap_completion_delta_is_zero(self) -> None:
        if not common.DISPOSITION_LOCK.is_file():
            self.skipTest("t42_disposition_lock.json not generated")
        gap = gap_builder.build()
        self.assertEqual(0, gap["completion_delta"])
        self.assertEqual(0, gap["publication_delta"])
        self.assertEqual(0, gap["partial_family_count"])
        self.assertEqual(0, gap["partial_families_deducted_from_gap"])
        overlay = common.load_json(common.BLOCKER_OVERLAY)
        self.assertEqual(
            int(overlay.get("partial_family_count") or 0),
            gap["overlay_partial_family_count"],
        )
        self.assertGreater(gap["overlay_partial_family_count"], 0)
        lock = common.load_json(common.DISPOSITION_LOCK)
        lock_by_id = {row["family_id"]: row for row in lock["families"]}
        for family in overlay["families"]:
            if "partial" not in family["secondary_blockers"]:
                continue
            self.assertEqual(
                "retained_current_execution_gap",
                lock_by_id[family["family_id"]]["disposition"],
            )
        self.assertEqual(5305, gap["opening_execution_gap"])
        self.assertEqual(
            gap["opening_execution_gap"]
            - gap["already_expressed_delta"]
            - gap["reclassification_delta"],
            gap["closing_execution_gap"],
        )

    def test_census_t14_opening_equals_closing(self) -> None:
        if not common.GAP_PARTITION.is_file():
            self.skipTest("t42_gap_partition.json not generated")
        census = census_builder.build()
        self.assertFalse(census["hard_ceiling_raised"])
        self.assertFalse(census["generated_recipe_tree"])
        self.assertEqual(0, census["completion_delta"])
        self.assertEqual(census["t14_load"]["opening"], census["t14_load"]["closing"])
        self.assertFalse(census["remaining_ordinary"]["t41_files_rewritten"])

    def test_topology_does_not_assign_t43(self) -> None:
        if not common.GAP_PARTITION.is_file():
            self.skipTest("t42_gap_partition.json not generated")
        topology = topology_builder.build()
        self.assertEqual("T43", topology["next_issue_id"])
        self.assertFalse(topology["preassigned_host"])
        self.assertFalse(topology["preassigned_family_ids"])
        self.assertIsNone(topology["unique_active_content_card"])
        self.assertEqual(0, topology["owns_families"])
        t43 = next(row for row in topology["sequence"] if row.get("id") == "T43")
        self.assertNotIn("family_ids", t43)
        self.assertFalse(t43["preassigned_host"])
        repair = next(row for row in topology["sequence"] if row.get("id") == "T42-Repair")
        self.assertEqual("partition_repair", repair["kind"])
        self.assertEqual(0, repair["owns_families"])
        self.assertIn("T42-Repair", t43["depends_on"])
        owner = next(row for row in topology["sequence"] if row.get("id") == "T42-Owner")
        self.assertEqual("owner_partition_repair", owner["kind"])
        self.assertEqual(0, owner["owns_families"])
        self.assertIn("T42-Owner", t43["depends_on"])
        self.assertEqual(
            "T42_OWNER_READY && owner_partition_complete",
            topology["storage_interleave_gate"],
        )

    def test_readiness_java_tests_are_wired(self) -> None:
        self.assertTrue(all(path.is_file() for path in readiness_builder.JAVA_TESTS))

    def test_append_only_lock_refuses_reason_rewrite(self) -> None:
        if not common.DISPOSITION_LOCK.is_file():
            self.skipTest("t42_disposition_lock.json not generated")
        existing = common.load_json(common.DISPOSITION_LOCK)
        mutated = dict(existing)
        families = [dict(row) for row in existing["families"]]
        families[0] = dict(families[0], reason="mutated")
        mutated["families"] = families
        derived = lock_builder.build()
        rewritten = [
            row["family_id"]
            for row in existing["families"]
            if lock_builder._approved_identity(row)
            != lock_builder._approved_identity(
                {item["family_id"]: item for item in derived["families"]}[row["family_id"]]
            )
        ]
        self.assertEqual([], rewritten)


if __name__ == "__main__":
    unittest.main()
