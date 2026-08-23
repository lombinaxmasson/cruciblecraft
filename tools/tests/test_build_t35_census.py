"""Tests for the T35 aggregate census builder."""
from __future__ import annotations

import copy
import hashlib
import json
import sys
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t35_census as builder  # noqa: E402
from tools import t27_common as common  # noqa: E402
from tools import t35_common as t35  # noqa: E402


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T35CensusTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_build_is_deterministic(self) -> None:
        first = builder.build()
        second = builder.build()
        self.assertEqual(common.stable_json(first), common.stable_json(second))

    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        self.assertEqual(builder.build(), on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_foundation_counts_and_recipe_membership(self) -> None:
        counts = self.document["counts"]
        self.assertEqual(765, counts["t13_identities"])
        self.assertEqual(194, counts["exclusion_families"])
        self.assertEqual(5718, counts["recipe_families"])
        self.assertEqual(78682, counts["recipe_rows_accounted"])
        self.assertEqual(763, counts["exclusion_source_sites"])
        self.assertEqual(1701, counts["exclusion_expanded_rows"])
        self.assertEqual(20553, counts["runtime_ids_expected"])
        self.assertEqual(20553, counts["runtime_ids_mapped"])

    def test_validators_are_zero(self) -> None:
        validators = self.document["validators"]
        self.assertEqual(0, validators["unclassified"])
        self.assertEqual(0, validators["unscoped"])
        self.assertEqual(0, validators["scope_errors"])
        self.assertEqual(0, validators["unmapped_runtime"])
        self.assertEqual(0, validators["owner_violations"])
        self.assertEqual(0, validators["dependency_errors"])
        self.assertEqual(0, validators["dependency_cycles"])
        self.assertEqual(0, validators["source_publication_delta"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, self.document["publication_delta"])

    def test_disposition_and_priority_are_separate(self) -> None:
        for record in self.document["identities"].values():
            self.assertIn(record["disposition"], t35.DISPOSITIONS)
            self.assertIn(record["portfolio_priority"], t35.PRIORITIES)
            self.assertIn(record["portfolio_scope"], t35.PORTFOLIO_SCOPES)
            self.assertIsInstance(record["scope_contract"], dict)
            self.assertIn("historical_classification", record)
            if record["domain"] == "recipe":
                self.assertNotEqual(record["disposition"], record["portfolio_priority"])

    def test_runtime_mapping_is_category_scoped_bijection(self) -> None:
        mapping = self.document["indexes"]["runtime_to_census"]
        self.assertEqual(20553, len(mapping))
        self.assertEqual(20553, len(set(mapping)))
        targets = set(mapping.values())
        for target in targets:
            self.assertIn(target, self.document["identities"])

    def test_no_fake_pending_zero_load(self) -> None:
        for record in self.document["identities"].values():
            load = (record.get("axes") or {}).get("load") or {}
            if load.get("status") != "pending":
                continue
            self.assertEqual(t35.PENDING_LOAD_VERDICT, load.get("verdict"))
            for key in common.FAKE_ZERO_LOAD_KEYS:
                self.assertNotEqual(0, load.get(key))

    def test_t37_pilot_is_deterministic_and_fixed(self) -> None:
        pilot = self.document["t37_pilot"]
        self.assertEqual("T37", pilot["fixed_card"])
        self.assertEqual("cruciblecraft:assembler", pilot["host_map"])
        self.assertEqual(50, pilot["source_rows"])
        self.assertEqual(50, len(pilot["family_ids"]))
        self.assertEqual(pilot, builder.build()["t37_pilot"])
        nodes = self.document["fixed_card_nodes"]
        self.assertEqual("T36", nodes["T36"]["id"])
        self.assertEqual("T37", nodes["T37"]["id"])

    def test_storage_track_families_are_planned_p1(self) -> None:
        for family in builder.STORAGE_PLANNED_P1:
            record = self.document["identities"][f"exclusion/{family}"]
            self.assertEqual("planned", record["disposition"])
            self.assertEqual("P1", record["portfolio_priority"])
            self.assertEqual("in_scope_1x", record["portfolio_scope"])
        logistics = self.document["identities"]["exclusion/mass_storage_logistics"]
        self.assertEqual("planned", logistics["disposition"])
        self.assertEqual("P1", logistics["portfolio_priority"])
        self.assertEqual("in_scope_1x", logistics["portfolio_scope"])

    def test_storage_adjacent_scope_decisions_are_explicit(self) -> None:
        expected = {
            "chest": "post_1x",
            "safe": "post_1x",
            "tank": "post_1x",
            "fluid_container": "candidate_1x",
            "pump": "post_1x",
            "sorting": "post_1x",
            "hopper": "in_scope_1x",
        }
        for family, scope in expected.items():
            record = self.document["identities"][f"exclusion/{family}"]
            self.assertEqual(scope, record["portfolio_scope"])
        locker = self.document["identities"]["exclusion/locker"]
        charging = locker["storage_scope"]["folded_source_behaviors"][1]
        self.assertEqual("MultiTileEntityLockerCharging", charging["behavior_class"])
        self.assertTrue(charging["behavior_diff"])

    def test_p2_candidates_are_not_admitted_without_measurement(self) -> None:
        p2_candidates = set(self.document["work_sets"]["p2_candidates"])
        for canonical_id in p2_candidates:
            record = self.document["identities"][canonical_id]
            self.assertEqual("BLOCKED_PENDING_MEASUREMENT", record.get("p2_admission"))

    def test_mandatory_work_set_excludes_closed_implementations(self) -> None:
        for canonical_id in self.document["work_sets"]["mandatory_p0_p1"]:
            record = self.document["identities"][canonical_id]
            self.assertEqual("in_scope_1x", record["portfolio_scope"])
            self.assertEqual("planned", record["disposition"])
            self.assertNotEqual(
                "closed",
                record["axes"]["closure"]["status"],
            )
            self.assertIn(record["portfolio_priority"], {"P0", "P1"})

    def test_foundation_artifact_check_is_read_only(self) -> None:
        before = _sha(t35.EXCLUSION_RECLAIM)
        builder.build()
        self.assertEqual(before, _sha(t35.EXCLUSION_RECLAIM))

    def test_missing_foundation_fails_closed(self) -> None:
        with mock.patch.object(t35, "INPUTS", t35.TOOLS / "missing_t35_inputs.json"):
            with self.assertRaises(FileNotFoundError):
                builder.build()

    def test_corrupt_recipe_membership_fails_closed(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        original = json.loads(t35.RECIPE_FAMILIES.read_text(encoding="utf-8"))
        tampered = copy.deepcopy(original)
        tampered["counts"]["assigned_rows"] = 1
        t35.RECIPE_FAMILIES.write_text(common.stable_json(tampered), encoding="utf-8")
        try:
            with self.assertRaises(ValueError):
                builder.build()
        finally:
            common.write_stable(t35.RECIPE_FAMILIES, original)


if __name__ == "__main__":
    unittest.main()
