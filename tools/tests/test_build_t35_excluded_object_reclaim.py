"""Tests for the T35 exclusion reclaim builder."""
from __future__ import annotations

import copy
import hashlib
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t35_excluded_object_reclaim as builder  # noqa: E402
from tools import t27_common as common  # noqa: E402


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T35ExcludedObjectReclaimTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_build_is_deterministic(self) -> None:
        first = builder.build()
        second = builder.build()
        self.assertEqual(
            common.stable_json(first),
            common.stable_json(second),
        )

    def test_expected_exclusion_counts(self) -> None:
        counts = self.document["counts"]
        self.assertEqual(counts["source_sites"], 763)
        self.assertEqual(counts["expanded_multiplicity"], 1701)
        self.assertEqual(counts["unmapped"], 0)
        self.assertEqual(counts["duplicate_family_membership"], 0)

    def test_storage_invariants(self) -> None:
        counts = self.document["counts"]
        self.assertEqual(counts["storage_source_sites"], 28)
        self.assertEqual(counts["storage_expanded_multiplicity"], 624)
        self.assertEqual(counts["mass_storage_logistics_source_sites"], 1)
        self.assertEqual(counts["mass_storage_logistics_expanded_multiplicity"], 1)

        storage_families = self.document["storage_families"]
        self.assertEqual(set(storage_families), set(builder.STORAGE_CORE_FAMILIES))
        for name, (sites, expanded) in builder.STORAGE_FAMILY_COUNTS.items():
            self.assertEqual(storage_families[name]["source_sites"], sites)
            self.assertEqual(
                storage_families[name]["expanded_multiplicity"], expanded
            )

        logistics = self.document["mass_storage_logistics"]
        self.assertFalse(logistics["counts_toward_storage_624"])
        self.assertEqual(logistics["category"], "Logistics")

        storage_rows = [
            row for row in self.document["source_sites"] if row["category"] == "Storage"
        ]
        self.assertEqual(len(storage_rows), 28)
        self.assertEqual(
            {row["canonical_family"] for row in storage_rows},
            set(builder.STORAGE_CORE_FAMILIES),
        )

    def test_every_site_maps_to_one_family(self) -> None:
        sites = self.document["source_sites"]
        self.assertEqual(len(sites), 763)
        keys = [row["site_key"] for row in sites]
        self.assertEqual(len(keys), len(set(keys)))
        for row in sites:
            self.assertTrue(row["canonical_family"])
            self.assertIn("source_identity", row)
            self.assertIn("historical_exclusion_reason", row)

    def test_committed_artifact_is_current_when_present(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        self.assertEqual(builder.check(), [])

    def test_machine_kinds_input_is_read_only(self) -> None:
        before = _sha(builder.MACHINE_KINDS)
        builder.build()
        self.assertEqual(_sha(builder.MACHINE_KINDS), before)

    def test_stale_input_is_detected(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        original = builder.MACHINE_KINDS.read_text(encoding="utf-8")
        document = json.loads(original)
        mutated = copy.deepcopy(document)
        exclusions = mutated["exclusions"]
        exclusions[0] = copy.deepcopy(exclusions[0])
        exclusions[0]["multiplicity"] = int(exclusions[0]["multiplicity"]) + 1
        builder.MACHINE_KINDS.write_text(
            json.dumps(mutated, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        try:
            with self.assertRaises(ValueError):
                builder.build()
        finally:
            builder.MACHINE_KINDS.write_text(original, encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
