"""Contract tests for T39 production withdrawal."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_production_selection as builder  # noqa: E402
from tools import t39_common as t39  # noqa: E402


class T39ProductionSelectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_catalog_stays_withdrawn_while_subset_is_only_candidate(self) -> None:
        self.assertEqual("T39_PRODUCTION_CANDIDATE", self.document["status"])
        withdrawn = self.document["withdrawn_catalog"]
        self.assertEqual(t39.CATALOG_FAMILY_COUNT, withdrawn["family_count"])
        self.assertEqual(t39.CATALOG_RELATION_COUNT, withdrawn["relations"])
        self.assertEqual(t39.EXPECTED_SELECTION_SHA256, withdrawn["selection_sha256"])
        candidate = self.document["candidate"]
        self.assertGreater(candidate["families"], 0)
        self.assertLess(candidate["families"], t39.CATALOG_FAMILY_COUNT)
        self.assertEqual(len(candidate["family_ids"]), candidate["families"])
        self.assertEqual(
            t39.selection_sha256(candidate["family_ids"]),
            candidate["selection_sha256"],
        )
        self.assertNotEqual(t39.EXPECTED_SELECTION_SHA256, candidate["selection_sha256"])
        self.assertEqual(
            candidate["player_path"]["inputs_reachable"],
            candidate["relations"],
        )
        rejected = {row["template_key"] for row in self.document["review"]["rejected"]}
        self.assertIn("gt.recipe.centrifuge#0111", rejected)
        self.assertIn("gt.recipe.centrifuge#0207", rejected)
        self.assertNotIn("gt.recipe.centrifuge#0111", candidate["template_keys"])
        self.assertNotIn("gt.recipe.centrifuge#0207", candidate["template_keys"])

    def test_alias_fail_close_reduces_trusted_reachability(self) -> None:
        player = self.document["player_path"]
        self.assertEqual(t39.CATALOG_RELATION_COUNT, player["relations"])
        self.assertGreater(player["alias_fail_closed_relations"], 0)
        self.assertLess(player["inputs_reachable"], player["relations"])
        self.assertEqual(
            player["inputs_reachable"] + player["relations_with_unreachable_inputs"],
            player["relations"],
        )

    def test_family_atomic_trusted_is_computed_not_invented(self) -> None:
        trusted = self.document["family_atomic_trusted"]
        self.assertEqual(
            trusted["relations"],
            trusted["singleton_families"] + trusted["multi_relations"],
        )
        self.assertEqual(
            trusted["families"],
            trusted["singleton_families"] + trusted["multi_families"],
        )
        self.assertFalse(trusted["includes_t39_support"])
        self.assertEqual("t21_without_t39_or_support", trusted["baseline"])
        self.assertLess(trusted["families"], t39.CATALOG_FAMILY_COUNT)
        self.assertEqual(
            t39.T38_REMAINING_ORDINARY_FAMILIES - trusted["families"],
            self.document["gap_projection"]["if_trusted_subset_candidate"],
        )
        self.assertEqual(
            t39.WITHDRAWN_CATALOG_CLOSING_REMAINING,
            self.document["gap_projection"]["withdrawn_catalog_closing"],
        )
        self.assertEqual(
            t39.T38_REMAINING_ORDINARY_FAMILIES
            - self.document["candidate"]["families"],
            self.document["gap_projection"]["if_candidate_remaining"],
        )
        projection = self.document["min_support_projection"]
        if projection.get("status") in {"DRAFT_UNREVIEWED", "REVIEWED"}:
            self.assertGreaterEqual(projection["families"], trusted["families"])
            self.assertLess(projection["families"], t39.CATALOG_FAMILY_COUNT)
            self.assertEqual(
                t39.T38_REMAINING_ORDINARY_FAMILIES - projection["families"],
                projection["if_candidate_remaining"],
            )

    def test_lossy_oil_sand_alias_is_unproven(self) -> None:
        self.assertTrue(
            t39.source_operand_is_unproven_lossy_alias(
                {
                    "mapping": "source_derived_alias",
                    "runtime_id": "minecraft:suspicious_sand",
                    "source": {
                        "displayName": "Sand Oil Sand",
                        "item": "gregtech:gt.meta.ore.normal.sand",
                        "meta": 9851,
                    },
                }
            )
        )
        self.assertFalse(
            t39.source_operand_is_unproven_lossy_alias(
                {
                    "mapping": "source_derived_alias",
                    "runtime_id": "cruciblecraft:molten_celenegil",
                    "source": {"fluid": "molten.celenegil", "amount": 288},
                }
            )
        )


if __name__ == "__main__":
    unittest.main()
