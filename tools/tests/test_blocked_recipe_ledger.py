#!/usr/bin/env python3
"""Blocked-recipe ledger card: replayable counts, fluidbed first chain blocked."""
from __future__ import annotations

import json
import unittest
from pathlib import Path

from tools import blocked_recipe_ledger as ledger
from tools import capability_ledger as caps
from tools import io_common as io

OWNER_SLUG = "recipe/blocked-chain-ledger"
ROOT = io.ROOT
FLUIDBED_OUT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "energy"
    / "fuels_fluidbed"
)
REQUIRED_FIELDS = (
    "source_revision",
    "host",
    "recipe_map",
    "family_id",
    "dump_index",
    "relation_count",
    "blocker_root",
    "missing",
    "owner",
    "replacement_condition",
    "recheck_point",
    "player_path_disposition",
    "freshness",
)
EMITTED = {
    "anthracite_dust_molten_calcite_47.json",
    "lonsdaleite_dust_molten_calcite_4.json",
    "lonsdaleite_small_dust_molten_calcite_36.json",
    "lonsdaleite_tiny_dust_molten_calcite_50.json",
    "peat_dust_molten_calcite_14.json",
    "prismane_dust_molten_calcite_49.json",
}


class BlockedRecipeLedgerCardTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = io.load_json(ledger.LEDGER)

    def test_ledger_replays_from_fixed_revision(self) -> None:
        self.assertEqual(0, ledger.main(["--check"]))
        self.assertEqual(io.SOURCE_REVISION, self.document["source_revision"])
        self.assertIsNone(self.document["unique_active_wave"])
        self.assertEqual(OWNER_SLUG, self.document["capability_slug"])

    def test_three_counts_stay_separate(self) -> None:
        counts = self.document["counts"]
        self.assertTrue(counts["do_not_add"])
        families = counts["blocked_families"]
        rows = counts["blocked_relations_or_rows"]
        self.assertEqual(0, families["bath_remainder"])
        self.assertEqual(0, families["bath_identity"])
        self.assertEqual(0, families["ordinary_closure"])
        self.assertEqual(49, rows["fluidbed_rows"])
        self.assertEqual(702, rows["petroleum_sampled_blocked_rows"])
        self.assertNotEqual(
            families["bath_remainder"] + rows["fluidbed_rows"],
            rows["petroleum_sampled_blocked_rows"],
        )
        roots = self.document["blocker_roots"]
        self.assertIn("unmapped_storage_dust", roots["fluidbed"])
        self.assertEqual(11, roots["fluidbed"]["unmapped_storage_dust"])
        self.assertEqual(11, roots["fluidbed"]["missing_input_form"])
        self.assertEqual(21, roots["fluidbed"]["missing_dust_div72_form"])
        self.assertEqual(6, roots["fluidbed"]["outputless_fuel_model"])
        self.assertEqual(
            49,
            sum(roots["fluidbed"].values()),
        )

    def test_fluidbed_rows_have_disposition_and_required_fields(self) -> None:
        fluidbed = [
            row
            for row in self.document["entries"]
            if row["chain"] == "fluidbed-form-output-model"
        ]
        self.assertEqual(49, len(fluidbed))
        indexes = {row["dump_index"] for row in fluidbed}
        self.assertEqual(49, len(indexes))
        for row in fluidbed:
            for field in REQUIRED_FIELDS:
                self.assertIn(field, row, field)
            self.assertEqual("blocked", row["player_path_disposition"])
            self.assertEqual("current", row["freshness"])
            self.assertEqual(io.SOURCE_REVISION, row["source_revision"])
            self.assertTrue(row["blocker_root"])
            self.assertTrue(row["owner"])
            self.assertTrue(row["replacement_condition"])
            self.assertTrue(row["recheck_point"])
            self.assertNotIn("stand-in", row["replacement_condition"].lower())
            self.assertNotIn("programmed_circuit", row["replacement_condition"])

    def test_emitted_fluidbed_files_stay_source_backed(self) -> None:
        files = {path.name for path in FLUIDBED_OUT.glob("*.json")}
        self.assertEqual(EMITTED, files)
        self.assertEqual(sorted(EMITTED), self.document["written_fluidbed"]["files"])
        self.assertEqual([4, 14, 36, 47, 49, 50], self.document["written_fluidbed"]["indexes"])
        for name in EMITTED:
            recipe = io.load_json(FLUIDBED_OUT / name)
            self.assertEqual("cruciblecraft:fuels_fluidbed", recipe["map"])
            self.assertTrue(recipe["item_outputs"])
            self.assertTrue(recipe["item_inputs"])
            self.assertIn("gt6_dump/gt6_recipe_dump/maps/gt.recipe.fuels.fluidbed.json", recipe["provenance"]["selected_source_recipe"])

    def test_first_chain_is_explicitly_blocked_without_stand_in(self) -> None:
        first = self.document["first_chain"]
        self.assertEqual("explicitly_blocked", first["decision"])
        self.assertEqual(49, first["rows"])
        self.assertEqual(0, first["ready_rows"])
        self.assertEqual(6, first["emitted_rows"])
        self.assertFalse(first["stand_in"])
        self.assertEqual(
            [
                "fluidbed-form-output-model",
                "bath-identity-form-object",
                "petroleum-mixer-mapping",
            ],
            [row["chain"] for row in self.document["chains"]],
        )
        self.assertEqual("resolved", self.document["chains"][1]["decision"])
        self.assertFalse(self.document["chains"][1]["next_card"])
        self.assertEqual("historical_optional", self.document["chains"][2]["freshness"])

    def test_petroleum_and_bath_stay_out_of_fluidbed_denominator(self) -> None:
        fluidbed = [
            row
            for row in self.document["entries"]
            if row["chain"] == "fluidbed-form-output-model"
        ]
        petroleum = [
            row
            for row in self.document["entries"]
            if row["freshness"] == "historical_optional"
        ]
        bath = [
            row
            for row in self.document["entries"]
            if row["chain"] == "bath-identity-form-object"
        ]
        self.assertEqual(49, len(fluidbed))
        self.assertEqual(1, len(petroleum))
        self.assertEqual(0, len(bath))
        self.assertEqual(702, petroleum[0]["relation_count"])
        self.assertTrue(
            all(row["host"] == "cruciblecraft:fuels_fluidbed" for row in fluidbed)
        )
        self.assertTrue(all(row["host"] != "cruciblecraft:fuels_fluidbed" for row in bath))

    def test_capability_blocked_is_out_of_band(self) -> None:
        rows = self.document["out_of_band"]["capability_blocked"]
        keys = {row["id"] for row in rows}
        self.assertIn("energy/nuclear-fission-hot-fluids/reactor:coolant_ic2", keys)
        self.assertIn("energy/nuclear-fission-hot-fluids/reactor:thorium_salt", keys)
        self.assertTrue(
            any(row["freshness"] == "out_of_scope_external" for row in rows)
        )
        recipe_ids = {row["id"] for row in self.document["entries"]}
        self.assertTrue(keys.isdisjoint(recipe_ids))
        self.assertEqual(
            {"autoclave": 0, "centrifuge": 0, "compressor": 0, "drying": 0, "electrolyzer": 0, "mixer": 0, "smelter": 0},
            self.document["ordinary_closure_blocked"],
        )

    def test_converter_catalog_and_nuclear_flags_stay_put(self) -> None:
        capability = caps.load_capability(
            caps.CAP_ROOT / "energy" / "converter-catalog" / "capability.json"
        )
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        hot = io.load_json(
            io.TOOLS / "waves" / "runtime" / "fission-hot-fluids" / "readiness.json"
        )
        self.assertTrue(hot["nuclear_started"])
        self.assertIsNone(hot["unique_active_wave"])
        self.assertEqual("player_complete", hot["evidence"]["hot_fluids_status"])


if __name__ == "__main__":
    unittest.main()
