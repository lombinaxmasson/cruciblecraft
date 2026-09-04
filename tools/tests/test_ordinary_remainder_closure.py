#!/usr/bin/env python3
"""Ordinary remainder program closeout: gap 0, deferred ledger, no active wave."""
from __future__ import annotations

import unittest
from pathlib import Path

from tools import closeout_seal
from tools import census_common as census
from tools.recipe_bulk import ordinary_r0 as r0
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "recipe-portfolio/ordinary-remainder-closure"
ROOT = census.TOOLS / "waves" / "recipe-portfolio" / "ordinary-remainder-closure"
HOSTS = (
    "drying/ordinary-closure",
    "electrolyzer/ordinary-closure",
    "centrifuge/ordinary-closure",
    "autoclave/ordinary-closure",
    "compressor/ordinary-closure",
)


class OrdinaryRemainderClosureTest(unittest.TestCase):
    def test_slug_and_spec_are_registered(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)

    def test_opening_r0_stays_334_by_2047(self) -> None:
        document = r0.global_remainder_r0()
        self.assertEqual([], document["errors"])
        self.assertEqual(334, document["family_total"])
        self.assertEqual(2047, document["relation_total"])

    def test_live_remainder_replay_is_zero(self) -> None:
        live = r0.live_remainder_replay()
        self.assertEqual([], live["errors"])
        self.assertEqual(0, live["family_total"])
        self.assertEqual(0, live["relation_total"])
        for host in (
            "cruciblecraft:drying",
            "cruciblecraft:electrolyzer",
            "cruciblecraft:centrifuge",
            "cruciblecraft:autoclave",
            "cruciblecraft:compressor",
            "cruciblecraft:smelter",
            "cruciblecraft:mixer",
        ):
            self.assertEqual(0, live["hosts"][host]["family_count"], host)

    def test_child_completion_plus_reclass_is_334(self) -> None:
        completion = 0
        reclass = 0
        for slug in HOSTS:
            census = census.load_json(
                census.TOOLS / "waves" / Path(*slug.split("/")) / "census_delta.json"
            )
            completion += int(census["completion_delta"])
            reclass += int(census["reclassification_delta"])
            self.assertEqual(0, int(census["partial_family_count"]), slug)
        self.assertEqual(308, completion)
        self.assertEqual(26, reclass)
        self.assertEqual(334, completion + reclass)

    def test_program_artifacts_are_ready(self) -> None:
        readiness = census.load_json(ROOT / "readiness.json")
        census = census.load_json(ROOT / "census_delta.json")
        deferred = census.load_json(ROOT / "deferred_ledger.json")
        gap = census.load_json(ROOT / "gap_replay.json")
        self.assertEqual("WAVE_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        self.assertEqual(
            "ORDINARY_REMAINDER_CLOSURE_READY",
            readiness["evidence"]["program_status"],
        )
        self.assertFalse(readiness["evidence"]["one_x_joint_exit"])
        self.assertEqual(0, int(census["remaining_recipe_gap"]))
        self.assertEqual(0, int(census["complete_family_count"]))
        self.assertEqual(334, int(census["program_accounted_families"]))
        self.assertEqual(0, int(gap["remaining_recipe_gap"]))
        self.assertEqual(334, int(gap["accounted_families"]))
        self.assertEqual(1819, int(deferred["inherited_recycling"]["count"]))
        self.assertFalse(deferred["inherited_recycling"]["silently_discarded"])
        self.assertEqual(1843, int(deferred["closing_deferred_recycling"]))
        self.assertEqual(1845, int(deferred["closing_deferred_total"]))
        self.assertEqual(
            "recycling/deferred-ordinary-runtime", deferred["next_owner"]
        )
        self.assertFalse(deferred["one_x_joint_exit"])

    def test_proven_new_deferred_have_owner_and_recheck(self) -> None:
        deferred = census.load_json(ROOT / "deferred_ledger.json")
        rows = deferred["proven_new_deferred"]
        self.assertEqual(26, len(rows))
        recycling = [row for row in rows if row["future_owner"] == "later:recycling"]
        other = [row for row in rows if row["future_owner"] != "later:recycling"]
        self.assertEqual(24, len(recycling))
        self.assertEqual(2, len(other))
        owners = {row["future_owner"] for row in other}
        self.assertEqual(
            {"later:execution_envelope/gt6_panel", "later:cross_mod"},
            owners,
        )
        for row in rows:
            self.assertTrue(row["future_owner"], row)
            self.assertTrue(row["recheck_condition"], row["family_id"])
            self.assertTrue(row["family_id"])

    def test_program_seal_is_current(self) -> None:
        self.assertEqual([], closeout_seal.check_wave_seal(SLUG))
        seal = census.load_json(ROOT / "closeout_seal.json")
        self.assertEqual("SEALED", seal["status"])
        self.assertEqual(0, int(seal["remaining_recipe_gap"]))
        self.assertEqual(0, int(seal["complete_family_count"]))

    def test_card_plan_is_archived(self) -> None:
        closed = (
            census.ROOT
            / "docs"
            / "history"
            / "card-plans"
            / "closed"
            / "Ordinary尾账收口与封板修复详细计划.md"
        )
        active = (
            census.ROOT
            / "docs"
            / "history"
            / "card-plans"
            / "active"
            / "Ordinary尾账收口与封板修复详细计划.md"
        )
        self.assertTrue(closed.is_file())
        self.assertFalse(active.is_file())


if __name__ == "__main__":
    unittest.main()
