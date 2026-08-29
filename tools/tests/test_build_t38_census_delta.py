"""Contract tests for the T38 census overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t38_census_delta as builder  # noqa: E402
from tools import t38_common as t38  # noqa: E402


class T38CensusDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t35_foundation_denominators_are_unchanged(self) -> None:
        self.assertEqual(t38.T35_FOUNDATION, self.document["t35_foundation"])
        self.assertTrue(self.document["t35_foundation_unchanged"])
        self.assertEqual(78682, self.document["t35_foundation"]["recipe_rows_accounted"])
        self.assertEqual(5718, self.document["t35_foundation"]["recipe_families"])
        self.assertEqual(0, self.document["validators"]["t35_foundation_unchanged"])

    def test_exactly_twenty_nine_frozen_identities_are_overlayed(self) -> None:
        ids = [row["canonical_id"] for row in self.document["identities"]]
        self.assertEqual(29, len(ids))
        self.assertEqual(29, len(set(ids)))
        self.assertTrue(all(
            row.startswith(
                "portfolio:track_a/cruciblecraft:roaster/gt.recipe.roaster#"
            )
            for row in ids
        ))
        self.assertEqual(
            "portfolio:track_a/cruciblecraft:roaster/gt.recipe.roaster#0000",
            ids[0],
        )
        self.assertEqual(
            "portfolio:track_a/cruciblecraft:roaster/gt.recipe.roaster#0029",
            ids[-1],
        )
        self.assertFalse(any(row.endswith("#0002") for row in ids))
        self.assertTrue(all(row["owner"] == t38.OWNER for row in self.document["identities"]))
        self.assertEqual(0, self.document["validators"]["owner_closing"])

    def test_opening_statuses_are_t38_planned_incomplete_pending(self) -> None:
        for row in self.document["identities"]:
            opening = row["opening"]
            self.assertEqual("planned", opening["disposition"])
            self.assertEqual("incomplete", opening["closure"])
            self.assertEqual("pending", opening["load"])
            self.assertEqual("source_backed", opening["fidelity"])
            self.assertEqual("out_of_scope", opening["t35_historical"]["disposition"])
            self.assertEqual(
                "portfolio:track_a/missing_host",
                opening["t35_historical"]["owner"],
            )
        self.assertIn("SOURCE_BACKED", self.document["fidelity"]["provenance_kinds"])
        self.assertFalse(self.document["fidelity"]["silent_drop"])
        self.assertEqual(0, self.document["validators"]["fidelity_recorded_not_dropped"])
        self.assertEqual(0, self.document["validators"]["opening_planned_incomplete_pending"])

    def test_remaining_ordinary_families_are_not_bulk_complete(self) -> None:
        remaining = self.document["remaining_ordinary"]
        self.assertEqual(5639, remaining["remaining_ordinary_families"])
        self.assertEqual(5668, remaining["t37_remaining_opening"])
        self.assertEqual(5639, remaining["expected_remaining_ordinary_families"])
        self.assertTrue(remaining["overlay_contains_only_frozen_ids"])
        self.assertFalse(remaining["t35_files_rewritten"])
        self.assertFalse(remaining["t36_bootstrap_in_overlay"])
        self.assertEqual([], remaining["t37_ids_in_overlay"])
        self.assertEqual(0, self.document["validators"]["remaining_ordinary_not_bulk_complete"])
        self.assertEqual(0, self.document["validators"]["t37_not_stolen"])

    def test_t14_opening_is_t37_closing_and_closing_axes_are_measured(self) -> None:
        t14 = self.document["t14_load"]
        self.assertEqual(t38.T14_OPENING_LOAD_SOURCE, t14["opening_source"])
        self.assertFalse(t14["hard_ceiling_raised"])
        self.assertEqual(3616, t14["opening"]["datapack_authored_entries"])
        self.assertEqual(t38.T14_AUTHORED_CLOSING, t14["closing"]["datapack_authored_entries"])
        self.assertEqual(t38.T14_EAGER_CLOSING, t14["closing"]["eager_publication_rows"])
        self.assertEqual(2334, t14["closing"]["lazy_logical_rows"])
        self.assertEqual(24, t14["closing"]["lazy_cache_ceiling_rows"])
        self.assertEqual(12731, t14["closing"]["sync_bytes"])
        self.assertEqual(12731, t14["closing"]["retained_memory_bytes"])
        self.assertEqual(473152, t14["closing"]["allocation_bytes"])
        self.assertEqual(2, t14["closing"]["server_reload_ms"])
        self.assertEqual(685725, t14["closing"]["lookup_p95_ns"])
        self.assertEqual(73, t14["closing"]["lookup_candidate_count"])
        self.assertEqual(0, self.document["validators"]["t14_hard_ceiling_not_raised"])
        self.assertEqual(0, self.document["validators"]["t14_pending_not_zero_filled"])
        if t38.production_strategy()["blocked"]:
            pending = [row for row in t14["axes"] if row["pending"]]
            self.assertGreater(len(pending), 0)
        else:
            runtime_pending = [
                row["axis"]
                for row in t14["axes"]
                if row["pending"] and row["axis"] not in t38.STRATEGY_AXES
            ]
            self.assertEqual([], runtime_pending)

    def test_identity_closeout_is_honest_when_measurements_block(self) -> None:
        closeout = self.document["identity_closeout"]
        self.assertEqual(0, closeout["partial_families"])
        player_path = t38.load_json(t38.PLAYER_PATH)
        player_ok = (
            int(player_path.get("families") or 0) == t38.FAMILY_COUNT
            and int(player_path.get("inputs_reachable") or 0) == t38.SOURCE_ROWS
            and int(player_path.get("outputs_registered") or 0) == t38.SOURCE_ROWS
        )
        blocked = (
            t38.production_strategy()["blocked"]
            or not t38.player_gametest_present()
            or not player_ok
        )
        if blocked:
            self.assertFalse(closeout["complete"])
            self.assertEqual("T38_CENSUS_DELTA_BLOCKED", self.document["status"])
            if not player_ok:
                self.assertIn("player_path", closeout["blockers"])
            for row in self.document["identities"]:
                self.assertEqual("implemented", row["closing"]["disposition"])
                self.assertEqual("incomplete", row["closing"]["closure"])
        self.assertEqual(29, self.document["publication"]["authored_compact_family_entries"])
        self.assertEqual(t38.AUTHORED_DATAPACK_COUNT, self.document["publication"]["authored_datapack_entries"])
        self.assertEqual(73, self.document["publication"]["logical"])
        self.assertEqual(t38.SUPPORT_EAGER_COUNT, self.document["publication"]["eager"])

    def test_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t38_census_delta.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
