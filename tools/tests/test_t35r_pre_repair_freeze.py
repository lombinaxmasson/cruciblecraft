"""Validate the fixed pre-repair T35R freeze reference."""
from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FREEZE = ROOT / "tools" / "t35r_pre_repair_freeze.json"

EXPECTED_OWNED_INPUT_HASHES = {
    "tools/build_t35_readiness.py": "c459db0e77318fe4be1651fe2a5ef218c8db523468e2d3d42ec11377c386f23a",
    "tools/known_issues/verification-debt.json": "e911ad9bbffe29a08cbb56d0eada1b37d4f33c9ffd84b610716c710371d1457d",
    "tools/t35_card_topology.json": "73f618e314550f070ed5d333d4debc3c6629bfc7236f70f89270d3c150268dca",
    "tools/t35_census.json": "7ba9abdb030cf2163a258a712b283a09ad4a91cc582b2e778bf1979e1edd8d10",
    "tools/t35_census_inputs.json": "d14dd56d0501350ff9a4f4756b775e712712f611fa56f36213d514088c7bf8bd",
    "tools/t35_census_policy.json": "f49adb72265df5d36424c68eb8bb6ae05bc9af1e95c33f1bc0e084b65fe0324f",
    "tools/t35_excluded_object_reclaim.json": "e9f697209038fe988e6a2bd4d17a8c832167893dbc3542625a1794ed9dd60363",
    "tools/t35_load_baseline.json": "fbacf5da6dc04883088e0d92a7518e85855112597a8fdf7a3285e4babae65850",
    "tools/t35_recipe_families.json": "48eac35b97d0b9d56d1ce8cc062b4344976607cb45466a030649d00a19992bb2",
    "tools/t35_runtime_registry.json": "402881606bda236dc913aa524ab587175787c8a136dfb162354c575987b78ede",
}

EXPECTED_GENERATED_CARDS = [
    {
        "id": "T38",
        "started": False,
        "depends_on": ["T36"],
        "owner_keys": ["portfolio:storage/bookshelf"],
        "canonical_identity_ids": ["exclusion/bookshelf"],
    },
    {
        "id": "T39",
        "started": False,
        "depends_on": ["T36"],
        "owner_keys": ["portfolio:storage/bottle_crate"],
        "canonical_identity_ids": ["exclusion/bottle_crate"],
    },
    {
        "id": "T40",
        "started": False,
        "depends_on": ["T36"],
        "owner_keys": ["portfolio:storage/drawer"],
        "canonical_identity_ids": ["exclusion/drawer"],
    },
    {
        "id": "T41",
        "started": False,
        "depends_on": ["T36"],
        "owner_keys": ["portfolio:storage/locker"],
        "canonical_identity_ids": ["exclusion/locker"],
    },
    {
        "id": "T42",
        "started": False,
        "depends_on": ["T36"],
        "owner_keys": ["portfolio:storage/mass_storage_barrel"],
        "canonical_identity_ids": ["exclusion/mass_storage_barrel"],
    },
    {
        "id": "T43",
        "started": False,
        "depends_on": ["T36"],
        "owner_keys": ["portfolio:storage/mass_storage_box"],
        "canonical_identity_ids": ["exclusion/mass_storage_box"],
    },
    {
        "id": "T44",
        "started": False,
        "depends_on": ["T36"],
        "owner_keys": ["portfolio:storage/mass_storage_logistics"],
        "canonical_identity_ids": ["exclusion/mass_storage_logistics"],
    },
    {
        "id": "T45",
        "started": False,
        "depends_on": ["T36"],
        "owner_keys": ["portfolio:storage/mass_storage_standard"],
        "canonical_identity_ids": ["exclusion/mass_storage_standard"],
    },
    {
        "id": "T46",
        "started": False,
        "depends_on": ["T36"],
        "owner_keys": ["portfolio:storage/storage_inserter"],
        "canonical_identity_ids": ["exclusion/storage_inserter"],
    },
]


class T35RPreRepairFreezeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = json.loads(FREEZE.read_text(encoding="utf-8"))

    def test_freeze_metadata_is_fixed_pre_repair_reference(self) -> None:
        document = self.document
        self.assertEqual(1, document["schema_version"])
        self.assertEqual("T35R_PRE_REPAIR_FREEZE", document["artifact"])
        self.assertEqual("freeze_reference", document["reference_kind"])
        self.assertEqual("pre_repair", document["evidence_scope"])
        self.assertEqual(
            "This is a freeze reference, not post-repair evidence.",
            document["statement"],
        )
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            document["source_revision"],
        )
        self.assertEqual({"utc": "2026-08-22", "local": "2026-08-22"}, document["dates"])

    def test_pre_repair_artifact_statuses_are_preserved(self) -> None:
        self.assertEqual(
            {
                "readiness": {
                    "path": "tools/t35_readiness.json",
                    "status": "T35_CENSUS_READY",
                    "status_owner": "static_gates_only",
                },
                "topology": {
                    "path": "tools/t35_card_topology.json",
                    "status": "T35_CARD_TOPOLOGY",
                    "census_status": "T35_CENSUS_AGGREGATED",
                    "started": False,
                },
            },
            self.document["pre_repair_artifact_status"],
        )

    def test_owned_input_hashes_are_the_fixed_pre_repair_baseline(self) -> None:
        # This intentionally does not read live readiness/topology artifacts: they change post-repair.
        self.assertEqual(
            EXPECTED_OWNED_INPUT_HASHES,
            self.document["readiness_currentness_owned_inputs"],
        )

    def test_cited_counts_and_publication_delta_match_baseline(self) -> None:
        self.assertEqual(
            {
                "t13_identities": 765,
                "exclusion": {
                    "source_sites": 763,
                    "expanded_rows": 1701,
                    "families": 194,
                },
                "runtime_ids": {"expected": 20553, "mapped": 20553},
                "recipe_rows_accounted": 78682,
                "recipe_families": 5718,
                "identities_total": 8996,
                "publication_delta": {"eager": 0, "lazy": 0, "logical": 0},
            },
            self.document["cited_counts"],
        )

    def test_generated_cards_match_fixed_pre_repair_baseline(self) -> None:
        cards = self.document["generated_cards"]
        self.assertEqual(EXPECTED_GENERATED_CARDS, cards)
        self.assertEqual(
            [f"T{number}" for number in range(38, 47)],
            [card["id"] for card in cards],
        )
        self.assertTrue(self.document["assertions"]["all_pre_repair_generated_cards_started_false"])
        self.assertTrue(all(card["started"] is False for card in cards))


if __name__ == "__main__":
    unittest.main()
