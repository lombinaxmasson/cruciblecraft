#!/usr/bin/env python3
"""Catalog and bounded-recipe checks for the Hammer/Squeezer/Laser landing."""
from __future__ import annotations

import importlib.util
import json
import unittest
from pathlib import Path

from tools import capability_ledger as ledger
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import recipe_wave

ROOT = census.ROOT
SLUG = "machines/hammer-squeezer-laser"
CAPABILITY = ROOT / "tools/capabilities/machines/hammer-squeezer-laser/capability.json"
WAVE = ROOT / "tools/waves/machines/hammer-squeezer-laser"
PLAN_ACTIVE = ROOT / "docs/history/card-plans/active/锤榨汁机激光详细计划.md"
PLAN_CLOSED = ROOT / "docs/history/card-plans/closed/锤榨汁机激光详细计划.md"
TIERS = ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
CATALOG = ROOT / "src/main/resources/data/cruciblecraft/smelter_mte_identity_catalog.json"
GAME_TESTS = ROOT / (
    "src/test/java/com/masson/cruciblecraft/gametest/"
    "HammerSqueezerLaserGameTests.java"
)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def _load_dump_builder():
    path = WAVE / "build_squeezer_dump.py"
    spec = importlib.util.spec_from_file_location("squeezer_dump_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


dump_builder = _load_dump_builder()


class HammerSqueezerLaserCardTest(unittest.TestCase):
    def test_squeezer_dump_slug_compiles_as_squeezer_host(self) -> None:
        self.assertIn("machines/hammer-squeezer-laser", KNOWN_SEMANTIC_SLUGS)
        spec = recipe_wave("machines/hammer-squeezer-laser")
        self.assertEqual("squeezer", spec.path_prefix)
        self.assertEqual("cruciblecraft:squeezer", spec.host)
        self.assertEqual("lock", spec.publication_policy)

    def test_squeezer_dump_builder_check_passes(self) -> None:
        self.assertEqual([], dump_builder.check())

    def test_capability_and_game_tests_are_scoped(self) -> None:
        capability = load(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotIn("player-complete", capability.get("profiles") or [])
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        source = GAME_TESTS.read_text(encoding="utf-8")
        for test_id in capability["required_test_ids"]:
            self.assertIn(f"void {test_id}", source)

    def test_machine_tier_rows_are_exactly_the_new_hosts(self) -> None:
        document = load(TIERS)
        rows = {row["id"]: row for row in document["variants"]}
        self.assertEqual(298, len(rows))
        generic = [
            row for row in document["variants"]
            if not row.get("resourceProfile", {}).get("skipGenericRegistration", False)
        ]
        self.assertEqual(296, len(generic))
        self.assertTrue(rows["cruciblecraft:laser_engraver"]
                        ["resourceProfile"]["skipGenericRegistration"])
        for path in (
            "squeezer",
            "steel_squeezer",
            "titanium_squeezer",
            "tungstensteel_squeezer",
            "laser_welder",
            "aluminium_laser_welder",
            "stainless_steel_laser_welder",
            "chromium_laser_welder",
            "titanium_laser_welder",
        ):
            self.assertIn(f"cruciblecraft:{path}", rows)

    def test_processing_fold_has_no_remaining_dummies(self) -> None:
        overlay = load(ROOT / "tools/waves/content/gt6-mte-processing-host-fold/fold_overlay.json")
        self.assertEqual(86, overlay["counts"]["fold_live_block"])
        self.assertEqual(0, overlay["counts"]["keep_distinct"])
        self.assertEqual(
            "cruciblecraft:automatic_hammer",
            next(row for row in overlay["rows"] if row["meta"] == 15001)
            ["live_block"],
        )
        self.assertEqual(
            "cruciblecraft:squeezer",
            next(row for row in overlay["rows"] if row["meta"] == 20071)
            ["live_block"],
        )

    def test_squeezer_dump_imports_resolvable_rows(self) -> None:
        overflow = load(WAVE / "squeezer_overflow.json")
        self.assertEqual(5322, overflow["source_rows"])
        self.assertEqual(15, overflow["selected_rows"])
        self.assertEqual(5307, overflow["overflow_rows"])
        self.assertEqual(5215, overflow["ignored_rows"])
        self.assertEqual(92, overflow["actionable_overflow_rows"])
        self.assertEqual(
            5215, overflow["buckets"]["ignore_unobtainable_plant_gt"])
        self.assertEqual(0, overflow["buckets"].get("shadow", 0))
        self.assertEqual(
            "crop_breeding_obtain_for_gt_base_crop_drops_only",
            overflow["ignore_policy"]["future_reclaim"],
        )
        self.assertEqual("do_not_import", overflow["ignore_policy"]["this_card"])
        self.assertNotIn("never_import", overflow)
        rows = load(WAVE / "overflow.json")["overflow"]
        ignored = [row for row in rows if row.get("disposition") == "ignore"]
        self.assertEqual(5215, len(ignored))
        self.assertTrue(all(row.get("status") == "ignored" for row in ignored))
        self.assertGreater(
            sum(
                1
                for row in ignored
                if "Potassium Berry" == row.get("input")
            ),
            0,
        )
        self.assertEqual(1, load(CATALOG)["new_item_count"])

    def test_gui_mapping_and_t1_laser_tint_follow_gt6(self) -> None:
        assets = ROOT / "src/main/resources/assets/cruciblecraft"
        gui = assets / "textures/gui/machines"
        for name in (
                "squeezer.png",
                "laserengraver.png",
                "welder.png",
                "anvilbendingsmall.png",
                "anvilbendingbig.png",
        ):
            self.assertTrue((gui / name).is_file(), name)
        manifest = load(assets / "gt6_hammer_squeezer_laser_art_manifest.json")
        dests = [row["destination"] for row in manifest["imports"]]
        self.assertIn(
            "assets/cruciblecraft/textures/gui/machines/squeezer.png", dests)
        self.assertIn(
            "assets/cruciblecraft/textures/gui/machines/laserengraver.png", dests)
        self.assertIn(
            "assets/cruciblecraft/textures/gui/machines/anvilbendingsmall.png", dests)
        self.assertIn(
            "assets/cruciblecraft/textures/gui/machines/anvilbendingbig.png", dests)
        self.assertIn(
            "assets/cruciblecraft/textures/block/machine/laser_engraver/colored/front.png",
            dests,
        )
        gui_map = (
            ROOT
            / "src/main/java/com/masson/cruciblecraft/client/screen"
            / "MachineGuiTextures.java"
        ).read_text(encoding="utf-8")
        self.assertIn('Map.entry("laser_welder", "welder")', gui_map)
        self.assertIn('Map.entry("laser_engraver", "laserengraver")', gui_map)
        self.assertNotIn('"laserwelder"', gui_map)
        color = (
            ROOT
            / "src/main/java/com/masson/cruciblecraft/client/color"
            / "MachineBlockColor.java"
        ).read_text(encoding="utf-8")
        self.assertIn('case "laser_engraver" -> "steel_galvanized"', color)
        hammer = (
            ROOT
            / "src/main/java/com/masson/cruciblecraft/machine/autotool"
            / "AutomaticHammerCatalog.java"
        ).read_text(encoding="utf-8")
        self.assertIn("return input * 2L", hammer)
        self.assertIn("overchargeExplosionStrength", hammer)
        entity = (
            ROOT
            / "src/main/java/com/masson/cruciblecraft/content/blockentity"
            / "AutomaticHammerBlockEntity.java"
        ).read_text(encoding="utf-8")
        self.assertIn("pendingExplosion", entity)
        self.assertIn("EnergyPackets.units(stored, 10L)", entity)
        self.assertIn("hardness * 50.0F > stored", entity)
        laser = (
            ROOT
            / "src/main/java/com/masson/cruciblecraft/content/block"
            / "LaserEngraverBlock.java"
        ).read_text(encoding="utf-8")
        self.assertIn("cruciblecraft:lu_tier_1", laser)
        self.assertIn("steel_galvanized", laser)


    def test_kinetic_welder_placeholder_host_is_gone(self) -> None:
        kinds = {
            row["id"]
            for row in load(ROOT / "src/main/resources/data/cruciblecraft/machine_kinds.json")["kinds"]
        }
        self.assertNotIn("cruciblecraft:welder", kinds)
        self.assertIn("cruciblecraft:laser_welder", kinds)
        rows = {row["id"]: row for row in load(TIERS)["variants"]}
        self.assertNotIn("cruciblecraft:welder", rows)
        delivery = load(
            ROOT / "src/main/resources/data/cruciblecraft/machine_delivery.json")
        host_ids = {row["id"] for row in delivery["hosts"]}
        self.assertNotIn("cruciblecraft:welder", host_ids)
        self.assertIn("cruciblecraft:laser_welder", host_ids)
        blocks = (
            ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java"
        ).read_text(encoding="utf-8")
        self.assertNotIn('tieredProcessing("welder")', blocks)
        maps = (
            ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModRecipeMaps.java"
        ).read_text(encoding="utf-8")
        self.assertIn('create("welder")', maps)
        recipe = (
            ROOT / "src/generated/resources/data/cruciblecraft/recipe/machines/welder.json"
        )
        self.assertFalse(recipe.is_file())

    def test_laser_welder_live_slots_match_gt6_panel(self) -> None:
        delivery = load(
            ROOT / "src/main/resources/data/cruciblecraft/machine_delivery.json")
        host = next(
            row for row in delivery["hosts"]
            if row["id"] == "cruciblecraft:laser_welder")
        self.assertEqual(9, host["slots"]["item_inputs"])
        self.assertEqual(1, host["slots"]["item_outputs"])
        self.assertEqual(1, host["slots"]["fluid_inputs"])
        self.assertEqual(host["gt6_panel"]["in_items"], host["slots"]["item_inputs"])
        self.assertEqual(host["gt6_panel"]["out_items"], host["slots"]["item_outputs"])
        self.assertEqual(host["gt6_panel"]["in_fluids"], host["slots"]["fluid_inputs"])
        spec = (
            ROOT
            / "src/main/java/com/masson/cruciblecraft/registry"
            / "ModProcessingMachines.java"
        ).read_text(encoding="utf-8")
        self.assertIn(
            "List.of(0, 1, 2, 3, 4, 5, 6, 7, 8),\n                        List.of(9)",
            spec,
        )
        self.assertIn("recipe.itemInputs().size() > 9", spec)
        self.assertIn("laserWelderHostMatchesGt6WelderSlots", GAME_TESTS.read_text(encoding="utf-8"))
        self.assertIn("anvilMode", (
            ROOT
            / "src/main/java/com/masson/cruciblecraft/content/blockentity"
            / "AutomaticHammerBlockEntity.java"
        ).read_text(encoding="utf-8"))
        self.assertIn("laserWelderRunsWelderRecipe", GAME_TESTS.read_text(encoding="utf-8"))
        self.assertIn("automaticHammerSideHitBendsPlate", GAME_TESTS.read_text(encoding="utf-8"))
        self.assertIn("liveSqueezerHasGt6JavaLatexRows", GAME_TESTS.read_text(encoding="utf-8"))
        self.assertIn("laserEngraverKeepsGt6FoilRows", GAME_TESTS.read_text(encoding="utf-8"))

    def test_gt6_java_recipe_maps_are_authored(self) -> None:
        recipes = ROOT / "src/main/resources/data/cruciblecraft/recipe/machine"
        for relative in (
            "laser_engraver/circuit_wire_copper.json",
            "laser_engraver/circuit_wire_copper_annealed.json",
            "squeezer/rubber_resin.json",
            "squeezer/rubber_leaves.json",
            "squeezer/rubber_sapling.json",
            "squeezer/wood_rubber_dust.json",
        ):
            self.assertTrue((recipes / relative).is_file(), relative)
        compact = (
            ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe"
            / "squeezer/hammer_squeezer_laser/gt_recipe_squeezer_0000.json"
        )
        self.assertTrue(compact.is_file(), compact.name)
        document = load(compact)
        blob = json.dumps(document)
        for dead in (
            "minecraft:fish",
            "minecraft:double_plant",
            "minecraft:cooked_fished",
        ):
            self.assertNotIn(dead, blob)
        inputs = [
            (relation.get("item_inputs") or [{}])[0].get("item")
            for relation in document.get("relations") or []
        ]
        self.assertIn("minecraft:cod", inputs)
        self.assertIn("minecraft:salmon", inputs)
        self.assertIn("minecraft:tropical_fish", inputs)
        self.assertIn("minecraft:sunflower", inputs)
        provider = (
            ROOT / "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java"
        ).read_text(encoding="utf-8")
        self.assertIn("anvil_bend_big/plate_to_curved_plate", provider)
        self.assertIn("anvil_bend_small/plate_to_foils", provider)
        self.assertIn("welder/rods_to_long_rod", provider)
        self.assertIn("welder/ingots_to_double_ingot", provider)
        self.assertIn("welder/ingots_to_triple_ingot", provider)
        self.assertIn("welder/ingots_to_block", provider)
        self.assertIn("welder/curved_plates_and_ring_to_rotor", provider)
        self.assertIn(
            "welder/double_plates_and_long_rods_to_machine_casing_double",
            provider,
        )

    def test_yellow_lenses_use_shared_prefix_item(self) -> None:
        recipes = ROOT / "src/generated/resources/data/cruciblecraft/recipe/machines"
        for name in (
            "laser_welder.json",
            "aluminium_laser_welder.json",
            "stainless_steel_laser_welder.json",
            "chromium_laser_welder.json",
            "titanium_laser_welder.json",
        ):
            document = load(recipes / name)
            lens = document["ingredients"]["L"]
            self.assertEqual("neoforge:compound", lens["type"])
            materials = [
                child["components"]["cruciblecraft:prefix_material"]
                for child in lens["children"]
            ]
            self.assertIn("yellow_sapphire", materials)
            self.assertTrue(
                all(child["items"] == "cruciblecraft:lens" for child in lens["children"])
            )
            self.assertNotIn(
                "c:crafting_lenses/yellow",
                json.dumps(document),
            )


if __name__ == "__main__":
    unittest.main()
