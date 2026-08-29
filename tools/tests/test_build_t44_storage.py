"""Contract tests for T44 storage builders."""
from __future__ import annotations

import unittest

from tools import t44_common as common
from tools import build_t44_storage_work_set as work_set
from tools import build_t44_storage_production_lock as lock
from tools import build_t44_storage_catalog as catalog
from tools import build_t44_storage_equivalence as equivalence
from tools import build_t44_storage_player_path as player_path
from tools import build_t44_storage_publication_delta as publication
from tools import build_t44_storage_census_delta as census
from tools import build_t44_card_topology as topology
from tools import build_t44_storage_readiness as readiness


class T44StorageBuildersTest(unittest.TestCase):
    def test_expansion_is_exactly_624_plus_one(self) -> None:
        variants = common.expand_variants()
        common.validate_variants(variants)
        self.assertEqual(625, len(variants))
        self.assertEqual(
            common.EXPECTED_SELECTION_SHA256,
            common.selection_sha256(common.variant_ids(variants)),
        )
        visible = [row for row in variants if row["visibility"] == "source_visible"]
        self.assertEqual(18, len(visible))
        self.assertEqual(
            "cruciblecraft:mass_storage_logistics_6200",
            variants[-1]["runtime_id"],
        )
        self.assertFalse(variants[-1]["counts_toward_storage_624"])

    def test_work_set_and_lock_split_denominators(self) -> None:
        document = work_set.build()
        self.assertEqual("T44_WORK_SET_FROZEN", document["status"])
        self.assertEqual(28, document["storage_source_sites"])
        self.assertEqual(624, document["storage_registrations"])
        self.assertEqual(1, document["logistics_source_sites"])
        frozen = lock.build()
        self.assertEqual(625, len(frozen["runtime_ids"]))
        self.assertEqual(common.EXPECTED_SELECTION_SHA256, frozen["selection_sha256"])
        self.assertEqual(3076, frozen["recipe_closing_execution_gap"])
        self.assertEqual(0, frozen["recipe_completion_delta"])

    def test_player_path_does_not_forge_hidden_recipes(self) -> None:
        path = player_path.build()
        self.assertEqual(0, path["forged_recipes"])
        self.assertEqual(18, path["source_visible"])
        self.assertTrue(path["hidden_retained_identities"])
        self.assertEqual(18, len(path["routes"]))

    def test_census_does_not_count_storage_as_recipe_completion(self) -> None:
        delta = census.build()
        self.assertEqual(624, delta["complete_storage_registrations"])
        self.assertEqual(1, delta["complete_logistics_registrations"])
        self.assertEqual(0, delta["completion_delta"])
        self.assertEqual(3076, delta["remaining_ordinary"]["remaining_ordinary_families"])
        self.assertEqual(9, len(delta["identities"]))
        self.assertFalse(
            any(row["counts_toward_ordinary_recipe_completion"] for row in delta["identities"])
        )

    def test_topology_and_readiness_leave_t45_unassigned(self) -> None:
        card = topology.build()
        self.assertEqual("T45", card["next_issue_id"])
        self.assertIsNone(card["unique_active_card"])
        self.assertFalse(card["preassigned_host"])
        self.assertFalse(card["preassigned_family_ids"])
        t44 = next(row for row in card["sequence"] if row.get("id") == "T44")
        self.assertEqual("complete", t44["status"])
        self.assertEqual("storage_bundle", t44["track"])
        self.assertTrue(
            any(row.get("id_policy") == "consecutive_from_T45" for row in card["sequence"])
        )
        account = readiness.build()
        self.assertEqual("T45", account["next_issue_id"])
        self.assertIsNone(account["unique_active_card"])
        self.assertEqual(3076, account["recipe_closing_execution_gap"])
        self.assertEqual(85, account["t36_live_machine_rows"])

    def test_catalog_and_equivalence_share_one_list(self) -> None:
        ledger = catalog.build()
        self.assertEqual(624, ledger["storage_count"])
        self.assertEqual(1, ledger["logistics_count"])
        self.assertEqual(18, ledger["source_visible"])
        evidence = equivalence.build()
        self.assertEqual("source_backed", evidence["fidelity"])
        self.assertEqual(8, len(evidence["profiles"]))
        self.assertEqual(0, publication.build()["recipe_completion_delta"])
