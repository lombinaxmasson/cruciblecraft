#!/usr/bin/env python3
"""1.x joint-exit program registration, seed, and sealed artifacts."""
from __future__ import annotations

import unittest

from tools import portfolio_one_x as one_x
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for


class OneXJointExitRegistrationTest(unittest.TestCase):
    def test_slugs_are_registered(self) -> None:
        expected = {
            "portfolio/one-x-exit-r0": "portfolio/census-disposition-replay",
            "portfolio/census-disposition-replay": "portfolio/energy-matrix-replay",
            "portfolio/energy-matrix-replay": "portfolio/storage-currentness-replay",
            "portfolio/storage-currentness-replay": (
                "portfolio/load-ceiling-interpretation"
            ),
            "portfolio/load-ceiling-interpretation": "portfolio/one-x-joint-exit",
        }
        for slug, nxt in expected.items():
            self.assertIn(slug, KNOWN_SEMANTIC_SLUGS)
            spec = spec_for(slug)
            self.assertEqual(nxt, spec.unique_active_wave)
            self.assertFalse(spec.next_unassigned)
            self.assertEqual(0, spec.owns_families)
            self.assertIsNone(spec.production_lock)

    def test_program_is_terminal(self) -> None:
        slug = "portfolio/one-x-joint-exit"
        self.assertIn(slug, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(slug)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)

    def test_seed_covers_required_rows(self) -> None:
        rows = one_x.capability_map_seed_rows()
        self.assertGreaterEqual(len(rows), 22)
        domains = {row["gt6_domain"] for row in rows}
        self.assertTrue({"oredict", "loader", "tileentity", "worldgen", "cover"} <= domains)
        classes = {row["correspondence_class"] for row in rows}
        self.assertIn("capability_missing", classes)
        self.assertIn("out_of_scope_historical", classes)
        for row in rows:
            self.assertNotEqual("1.x failure", row["recheck_condition"])
            if row["correspondence_class"] == "out_of_scope_historical":
                self.assertIn("source-capability-map", row["recheck_condition"])


class OneXJointExitArtifactsTest(unittest.TestCase):
    def test_r0_when_present(self) -> None:
        root = census.TOOLS / "waves" / "portfolio" / "one-x-exit-r0"
        if not (root / "readiness.json").is_file():
            self.skipTest("R0 artifacts not written yet")
        readiness = census.load_json(root / "readiness.json")
        self.assertEqual("ONE_X_EXIT_R0_READY", readiness["status"])
        self.assertEqual(
            "portfolio/census-disposition-replay", readiness["unique_active_wave"]
        )
        self.assertEqual(0, int(readiness["evidence"]["execution_gap"]))
        self.assertEqual("A", readiness["evidence"]["load_interpretation"])
        self.assertFalse(readiness["evidence"]["recipe_files_generated"])
        ledger = census.load_json(root / "exit_condition_ledger.json")
        colors = {row["condition"]: row["color"] for row in ledger["conditions"]}
        self.assertEqual("GREEN", colors["current_recipe_execution_gap"])
        self.assertEqual("GREEN", colors["deferred_ledger_or_independent_scope"])
        self.assertEqual("YELLOW", colors["census_disposition_owner"])
        self.assertEqual("YELLOW", colors["energy_matrix_selected_projection"])
        self.assertEqual("YELLOW", colors["load_ceiling_interpretation"])
        post = census.load_json(root / "post_1x_scope_replay.json")
        self.assertEqual(28, int(post["count"]))
        self.assertTrue(post["still_independent"])
        replay = census.load_json(root / "replay_needed.json")
        decisions = {row["child"]: row["decision"] for row in replay["children"]}
        self.assertEqual("MUST_REPLAY", decisions["portfolio/census-disposition-replay"])
        self.assertEqual("MUST_REPLAY", decisions["portfolio/energy-matrix-replay"])
        self.assertEqual(
            "MUST_REPLAY", decisions["portfolio/load-ceiling-interpretation"]
        )
        self.assertIn(
            decisions["portfolio/storage-currentness-replay"],
            {"MUST_REPLAY", "ALREADY_CURRENT"},
        )

    def test_census_when_present(self) -> None:
        root = census.TOOLS / "waves" / "portfolio" / "census-disposition-replay"
        if not (root / "readiness.json").is_file():
            self.skipTest("census replay artifacts not written yet")
        ledger = census.load_json(root / "disposition_ledger.json")
        self.assertEqual(5718, int(ledger["family_count"]))
        self.assertEqual(28, int(ledger["post_1x_scope_count"]))
        self.assertEqual(0, int(ledger["remaining_execution_gap"]))
        self.assertTrue(ledger["owner_runtime_checkpoint"]["not_current_proof"])
        update = census.load_json(root / "exit_condition_update.json")
        self.assertEqual("GREEN", update["color"])

    def test_program_when_present(self) -> None:
        root = census.TOOLS / "waves" / "portfolio" / "one-x-joint-exit"
        if not (root / "readiness.json").is_file():
            self.skipTest("program artifacts not written yet")
        readiness = census.load_json(root / "readiness.json")
        self.assertEqual("ONE_X_JOINT_EXIT_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        self.assertTrue(readiness["evidence"]["one_x_joint_exit"])
        self.assertEqual(
            "portfolio/source-capability-map", readiness["evidence"]["next_major"]
        )
        self.assertFalse(readiness["evidence"]["nuclear_track_c_started"])
        ledger = census.load_json(root / "exit_condition_ledger.json")
        for row in ledger["conditions"]:
            self.assertEqual("GREEN", row["color"], row["condition"])


if __name__ == "__main__":
    unittest.main()
