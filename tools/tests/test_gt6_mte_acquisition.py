#!/usr/bin/env python3
"""GT6 in-place MTE acquisition: 14 independent source-exact D0 children."""
from __future__ import annotations

import unittest

from tools import blockers
from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_mte_inplace_acquisition as runtime
from tools import gt6_mte_inplace_runtime as inplace
from tools import gt6_resolve
from tools import io_common as io

ROOT = census.ROOT
CONTRACT = runtime.CONTRACT_WAVE
PLAN_ACTIVE = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "active"
    / "MTE原地获得格详细计划.md"
)
PLAN_CLOSED = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "closed"
    / "MTE原地获得格详细计划.md"
)


class Gt6MteAcquisitionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.recipes = runtime.parse_loader_recipes()
        cls.errors = runtime.check()

    def test_check_passes(self) -> None:
        self.assertEqual([], self.errors)

    def test_fourteen_independent_families(self) -> None:
        self.assertEqual(14, len(runtime.CHILD_ORDER))
        self.assertEqual(14, len(inplace.DOMAINS))
        contract = census.load_json(CONTRACT / "contract.json")
        self.assertEqual(list(runtime.CHILD_ORDER), contract["child_order"])
        self.assertIs(False, contract["auto_promote_player_complete"])
        self.assertIs(True, contract["does_not_merge_counts"])
        self.assertEqual(runtime.BLOCKER_ID, contract["blocker_id"])
        self.assertEqual(14, len(contract["families"]))
        self.assertTrue(PLAN_ACTIVE.is_file() ^ PLAN_CLOSED.is_file())

    def test_gt6_source_path_and_resolver_tokens(self) -> None:
        self.assertTrue(runtime.LOADER.is_file())
        chest = gt6_resolve.resolve("OD.craftingChest")
        self.assertEqual("ok", chest["status"])
        self.assertEqual("minecraft:chest", chest["item"])
        circuit = gt6_resolve.resolve("OD_CIRCUITS[3]")
        self.assertEqual("cruciblecraft:circuit_advanced", circuit["item"])
        cable = gt6_resolve.resolve("MT.DATA.CABLES_01[3]")
        self.assertEqual("ok", cable["status"])
        self.assertEqual("cruciblecraft:gold/cable", (cable.get("form") or {}).get("item"))
        graphene_slot = gt6_resolve.resolve("MT.DATA.CABLES_01[6]")
        self.assertEqual("wireGt01", (graphene_slot.get("prefix") or {}).get("gt"))
        self.assertNotEqual(
            "cruciblecraft:graphene/cable",
            (graphene_slot.get("form") or {}).get("item"),
        )
        pump = gt6_resolve.resolve("IL.PUMPS[3]")
        self.assertEqual("cruciblecraft:compact_electric_pump_hv", pump["item"])
        plank = gt6_resolve.resolve("PlankData.PLANKS[0]")
        self.assertEqual("unmapped", plank["status"])
        self.assertEqual("unmapped", gt6_resolve.resolve("OD.itemLubricant")["status"])
        self.assertEqual("unmapped", gt6_resolve.resolve("IL.Cover_Logistics_Generic_Storage")["status"])
        lead = gt6_resolve.resolve("aRegistry.getItem(0)")
        self.assertEqual("cruciblecraft:lead/chest", lead["item"])

    def test_extender_two_source_exact_isolated_recipes(self) -> None:
        matrix = census.load_json(runtime._wave("extender") / "d0_obtain_matrix.json")
        self.assertEqual(2, matrix["counts"]["source_exact"])
        self.assertEqual(0, matrix["counts"]["explicitly_blocked"])
        by_meta = {int(row["meta"]): row for row in matrix["hosts"]}
        self.assertEqual(["Xh ", " M ", " wX"], by_meta[30001]["pattern"])
        self.assertEqual(["h  ", "XMX", "  w"], by_meta[30501]["pattern"])
        self.assertEqual(
            "cruciblecraft:steel/fluid_pipe",
            by_meta[30001]["operands"]["X"]["cc"],
        )
        for host in matrix["hosts"]:
            recipe = runtime._wave("extender") / "recipes" / (
                host["stable_id"].replace("/", "_") + ".json"
            )
            self.assertTrue(recipe.is_file(), recipe)
            document = census.load_json(recipe)
            self.assertEqual("cruciblecraft:shaped_catalyst", document["type"])
            self.assertEqual(host["runtime_id"], document["result"]["id"])

    def test_fluid_attachment_family_is_complete(self) -> None:
        matrix = census.load_json(
            runtime._wave("attachments") / "d0_obtain_matrix.json"
        )
        self.assertEqual(46, matrix["counts"]["hosts"])
        self.assertEqual(46, matrix["counts"]["source_exact"])
        self.assertEqual(0, matrix["counts"]["explicitly_blocked"])
        by_id = {row["stable_id"]: row for row in matrix["hosts"]}
        self.assertEqual(
            "smelting",
            by_id["fluid_attachment/ceramic_tap"]["recipe_kind"],
        )
        self.assertEqual(
            "cruciblecraft:raw_ceramic_tap",
            by_id["fluid_attachment/ceramic_tap"]["operands"]["ingredient"]["cc"],
        )
        self.assertTrue(
            all(row["live_recipe"]["matches_source"] for row in matrix["hosts"])
        )

    def test_furniture_chest_live_recipes_are_generated(self) -> None:
        catalog = census.load_json(runtime.LIVE_CATALOG)
        missing = []
        for row in catalog["recipes"]:
            if row.get("domain") != "furniture_chest":
                continue
            path = runtime._live_recipe_path(row["path"])
            if not path.is_file():
                missing.append(row["path"])
                continue
            document = census.load_json(path)
            self.assertEqual("cruciblecraft:shaped_catalyst", document["type"])
            self.assertEqual(row["result"]["id"], document["result"]["id"])
            self.assertEqual(row["pattern"], document["pattern"])
        self.assertEqual([], missing)

    def test_no_stand_in_and_same_material_chest(self) -> None:
        for domain in runtime.CHILD_ORDER:
            matrix = census.load_json(runtime._wave(domain) / "d0_obtain_matrix.json")
            with self.subTest(domain=domain):
                self.assertEqual(
                    inplace.DOMAINS[domain]["expected"],
                    matrix["counts"]["hosts"],
                )
                self.assertTrue(
                    all(
                        row["source_path"].endswith("Loader_MultiTileEntities.java")
                        for row in matrix["hosts"]
                    )
                )
                self.assertEqual([], runtime._stand_in_errors(matrix))
                for host in matrix["hosts"]:
                    for operand in (host.get("operands") or {}).values():
                        self.assertNotEqual(
                            "cruciblecraft:programmed_circuit",
                            operand.get("cc"),
                        )
                        if "getItem" in str(operand.get("gt") or ""):
                            self.assertNotEqual("minecraft:chest", operand.get("cc"))

    def test_live_item_gate_keeps_circuits_and_blocks_missing_plates(self) -> None:
        self.assertTrue(runtime._item_is_live("cruciblecraft:circuit_ultimate"))
        self.assertTrue(runtime._item_is_live("cruciblecraft:circuit_quantum"))
        self.assertTrue(runtime._item_is_live("cruciblecraft:steel/plate"))
        self.assertFalse(runtime._item_is_live("cruciblecraft:wood_treated/plate"))
        barrel = census.load_json(runtime._wave("furniture_barrel") / "d0_obtain_matrix.json")
        treated = next(
            row for row in barrel["hosts"] if row["stable_id"] == "furniture/wooden_item_barrel"
        )
        self.assertEqual("explicitly_blocked", treated["status"])
        self.assertEqual("missing_form", treated["operands"]["P"]["reason"])
        self.assertEqual("cruciblecraft:wood_treated/plate", treated["operands"]["P"]["cc"])

    def test_decorative_wood_panels_stay_blocked(self) -> None:
        matrix = census.load_json(runtime._wave("decorative") / "d0_obtain_matrix.json")
        self.assertEqual(1, matrix["counts"]["source_exact"])
        self.assertEqual(23, matrix["counts"]["explicitly_blocked"])
        rope = next(row for row in matrix["hosts"] if row["kind"] == "ROPE")
        self.assertEqual("source_exact", rope["status"])
        self.assertEqual("cruciblecraft:steel/fine_wire", rope["operands"]["P"]["cc"])
        for host in matrix["hosts"]:
            if host["kind"] == "WOOD_PANEL":
                self.assertEqual("explicitly_blocked", host["status"])
                self.assertEqual(
                    "missing_plank_identity",
                    host["operands"]["P"]["reason"],
                )

    def test_steam_turbines_match_live_source_and_luv_uses_gt6_wire(self) -> None:
        matrix = census.load_json(
            runtime._wave("converter_remainder") / "d0_obtain_matrix.json"
        )
        by_meta = {int(row["meta"]): row for row in matrix["hosts"]}
        bronze = by_meta[1512]
        self.assertEqual("source_exact", bronze["status"])
        self.assertTrue(bronze["live_recipe"]["matches_source"])
        luv = by_meta[10086]
        self.assertEqual("source_exact", luv["status"])
        self.assertEqual("cruciblecraft:graphene/wire", luv["operands"]["C"]["cc"])
        self.assertTrue(luv["live_recipe"]["present"])
        self.assertTrue(luv["live_recipe"]["matches_source"])

    def test_live_extender_catalog_and_no_player_complete(self) -> None:
        compiled = ledger.compile_ledger()
        parent = next(
            row for row in compiled["capabilities"]
            if row["slug"] == "content/gt6-mte-inplace-acquisition"
        )
        self.assertEqual("runtime_ready", parent["maturity"])
        self.assertNotEqual("player_complete", parent["maturity"])
        if parent["workflow"] == "active":
            self.assertEqual(
                "content/gt6-mte-inplace-acquisition",
                compiled.get("unique_active_slug"),
            )
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", parent["workflow"])
            self.assertNotEqual(
                "content/gt6-mte-inplace-acquisition",
                compiled.get("unique_active_slug"),
            )
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        extender = runtime._live_recipe_path("extender/tank_extender")
        self.assertTrue(extender.is_file(), extender)
        document = census.load_json(extender)
        self.assertEqual("cruciblecraft:shaped_catalyst", document["type"])
        self.assertEqual("cruciblecraft:extender/tank_extender", document["result"]["id"])
        catalog = census.load_json(runtime.LIVE_CATALOG)
        self.assertIs(False, catalog["auto_promote_player_complete"])
        paths = {row["path"] for row in catalog["recipes"]}
        self.assertIn("extender/tank_extender", paths)
        self.assertIn("steel/rope", paths)
        self.assertIn("boxwood/battery_luv", paths)
        self.assertNotIn("steam/turbine_bronze", paths)
        self.assertTrue((runtime.WAVE_PACK / "structure" / "empty.nbt").is_file())
        self.assertTrue(
            (runtime.WAVE_PACK / "gametest" / "structure" / "empty.nbt").is_file()
        )

    def test_independent_locks_do_not_promote_runtime(self) -> None:
        compiled = ledger.compile_ledger()
        catalog = {row["id"]: row for row in blockers.load_catalog()["entries"]}
        blocker = catalog[runtime.BLOCKER_ID]
        self.assertEqual("resolved", blocker["status"])
        self.assertEqual("content/gt6-mte-inplace-acquisition", blocker["resolved_by"])
        self.assertEqual(14, int(blocker["count"]))
        self.assertEqual([], blocker.get("claimed_by") or [])
        for domain in runtime.CHILD_ORDER:
            spec = inplace.DOMAINS[domain]
            capability = next(
                row for row in compiled["capabilities"] if row["slug"] == spec["slug"]
            )
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual("player_complete", capability["maturity"])
            self.assertNotIn(blockers.OBTAIN_MARKER, str(capability.get("note") or ""))
            self.assertFalse(
                (census.TOOLS / "capabilities" / runtime.acquisition_slug(domain) / "capability.json").is_file()
            )
            overlay = census.load_json(runtime._wave(domain) / "acquisition_overlay.json")
            self.assertIs(False, overlay["writes_machine_acquisition"])
            self.assertIs(True, overlay["live_recipes_written"])
            self.assertIs(False, overlay["auto_promote_player_complete"])
            self.assertNotIn("stays open", overlay["note"])
            lock = census.load_json(runtime._wave(domain) / "production_lock.json")
            self.assertIs(True, lock["independent"])
            self.assertIs(False, lock["isolated_only"])
            self.assertIs(True, lock["live_recipes"])
            gametest = runtime._wave(domain) / "gametest"
            self.assertTrue(any(gametest.glob("*.java")))
            self.assertFalse(
                (ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft" / "gametest" / "prep").is_dir()
            )

    def test_closed_batch_and_independent_family_locks(self) -> None:
        batches = blockers.load_batches()
        self.assertNotIn(
            "batch/obtain-mte-inplace-closure",
            {row["id"] for row in batches["batches"]},
        )
        closed = next(
            row
            for row in batches["closed_before_batch_selection"]
            if runtime.BLOCKER_ID in (row.get("blocker_ids") or [])
        )
        self.assertIn("content/gt6-mte-inplace-acquisition", closed["resolution"])
        contract = census.load_json(CONTRACT / "contract.json")
        self.assertEqual(14, len(contract["families"]))
        self.assertEqual(list(runtime.CHILD_ORDER), contract["child_order"])
        for domain in runtime.CHILD_ORDER:
            lock = census.load_json(runtime._wave(domain) / "production_lock.json")
            self.assertIs(True, lock["independent"])
            runtime_lock = (
                census.TOOLS
                / "waves"
                / "content"
                / inplace.DOMAINS[domain]["slug"].split("/", 1)[-1]
                / "production_lock.json"
            )
            self.assertTrue(runtime_lock.is_file(), runtime_lock)

    def test_r0_and_machine_acquisition_are_untouched(self) -> None:
        contract = census.load_json(CONTRACT / "contract.json")
        self.assertEqual(runtime._sha256(inplace.R0), contract["r0_sha256"])
        self.assertIs(False, contract["rewrites_r0"])
        self.assertIs(False, contract["writes_machine_acquisition"])
        self.assertNotIn(
            "tools/gt6_mte_inplace_acquisition.py",
            io.relative(runtime.MACHINE_ACQUISITION),
        )


if __name__ == "__main__":
    unittest.main()
