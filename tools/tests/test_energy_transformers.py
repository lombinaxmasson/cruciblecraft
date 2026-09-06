#!/usr/bin/env python3
"""Energy transformers unique-active card: 9 voltage-step machines in-game."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/transformers"
CAPABILITY_SLUG = "energy/transformers"
ROOT = io.ROOT
ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active"
CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed"
PLAN = CLOSED / "变压器详细计划.md"
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "energy" / "transformers" / "capability.json"
)
KINDS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "energy_transformer_kinds.json"
)
TIERS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "energy_transformer_tiers.json"
)
JAVA = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "energy"
    / "transformer"
)
WAVE = io.TOOLS / "waves" / "runtime" / "transformers"
CENSUS = ROOT / "tools" / "census_excluded_object_reclaim.json"
MACHINE_TIERS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "machine_tiers.json"
)
MACHINE_KINDS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "machine_kinds.json"
)
HISTORICAL = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "energy_converters.json"
)
SEALED = (
    ROOT
    / "tools"
    / "waves"
    / "portfolio"
    / "source-capability-growth-order"
    / "growth_order.json"
)
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_transformers_art_manifest.json"
)
CASING_MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_machine_casings_art_manifest.json"
)


class EnergyTransformersCardTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        topology = io.load_json(WAVE / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        census = io.load_json(WAVE / "census_delta.json")
        self.assertEqual(9, census["work_set"]["source_rows"])

    def test_transformer_plan_is_archived(self) -> None:
        names = sorted(path.name for path in ACTIVE.iterdir() if path.is_file())
        self.assertNotIn("变压器详细计划.md", names)
        self.assertTrue(PLAN.is_file())
        self.assertTrue((CLOSED / "电池详细计划.md").is_file())
        self.assertTrue((CLOSED / "能量转换机目录详细计划.md").is_file())
        self.assertTrue((CLOSED / "能量系统余量详细计划.md").is_file())

    def test_plan_uses_local_gt6_code_not_github_fetch(self) -> None:
        text = PLAN.read_text(encoding="utf-8")
        self.assertIn("energy/transformers", text)
        self.assertIn("gt6_code/gregtech6", text)
        self.assertIn("3703e40308c8c030763fd6297dea8b210d2a77b1", text)
        self.assertIn("Loader_MultiTileEntities", text)
        self.assertIn("TileEntityBase11Bidirectional", text)
        self.assertIn("MultiTileEntityTransformerElectric", text)
        self.assertIn("energy_transformer_kinds.json", text)
        self.assertIn("gregtech6_w", text)
        self.assertIn("10040", text)
        self.assertIn("10048", text)
        self.assertIn("10064", text)
        self.assertIn("PUV1", text)
        self.assertIn("material_monkey_wrench", text)
        self.assertIn("rotational_gearbox", text)
        self.assertIn("BlockLongDistWire", text)
        self.assertIn("unique_active_wave", text)
        self.assertIn("禁止写进", text)
        self.assertIn("machine_kinds.json", text)
        self.assertIn("不预写后继", text)
        self.assertIn("必须按 GT6 逐格", text)
        self.assertIn("严禁", text)
        self.assertIn("缺任何一格真实配料", text)
        self.assertNotIn("缺形态时 DESIGN_POLICY 用已有材料做生存获得", text)
        self.assertNotIn("raw.githubusercontent.com", text)

    def test_player_complete_forbids_recipe_stand_ins(self) -> None:
        workflow = (
            ROOT / "docs" / "current" / "capability-delivery-workflow.md"
        ).read_text(encoding="utf-8")
        self.assertIn("配方必须按 GT6 源逐格合成", workflow)
        self.assertIn("用已有材料 DESIGN_POLICY 生存获得", workflow)
        self.assertIn("不得晋级 `player_complete`", workflow)
        rule = (
            ROOT
            / ".cursor"
            / "rules"
            / "gt6-recipe-no-placeholder.mdc"
        ).read_text(encoding="utf-8")
        self.assertIn("no stand-in ingredients", rule)
        self.assertIn("缺形态时 DESIGN_POLICY 用已有材料做生存获得", rule)

    def test_kinds_and_java_are_implemented(self) -> None:
        kinds = io.load_json(KINDS)
        tiers = io.load_json(TIERS)
        self.assertEqual(1, len(kinds["kinds"]))
        self.assertEqual(9, len(tiers["tiers"]))
        self.assertEqual(
            "EU",
            kinds["kinds"][0]["energy"],
        )
        self.assertEqual(
            "cruciblecraft:electric_transformer",
            kinds["kinds"][0]["id"],
        )
        ids = {row["id"] for row in tiers["tiers"]}
        self.assertIn("cruciblecraft:electric_transformer_ulv_lv", ids)
        self.assertIn("cruciblecraft:electric_transformer_uv_puv1", ids)
        source_ids = {row["source_id"] for row in tiers["tiers"]}
        self.assertEqual(set(range(10040, 10049)), source_ids)
        self.assertNotIn(10064, source_ids)
        materials = [row["material"] for row in tiers["tiers"]]
        self.assertEqual(
            [
                "tin_alloy",
                "steel_galvanized",
                "aluminium",
                "stainless_steel",
                "chromium",
                "titanium",
                "iridium",
                "osmium_elemental",
                "trinitanium",
            ],
            materials,
        )
        for row in tiers["tiers"]:
            casing = row["recipe"]["keys"]["M"]
            self.assertEqual("machine_casing", casing["prefix"])
            self.assertEqual(row["material"], casing["material"])
        self.assertTrue((JAVA / "TransformerBlock.java").is_file())
        self.assertTrue((JAVA / "TransformerBlockEntity.java").is_file())
        block = (JAVA / "TransformerBlock.java").read_text(encoding="utf-8")
        self.assertIn("MaterialMonkeyWrenchItem", block)
        self.assertIn("toggleReversed", block)
        self.assertNotIn("setValue(FACING", block.split("useItemOn")[1][:800])
        machine_blob = str(io.load_json(MACHINE_TIERS)) + str(
            io.load_json(MACHINE_KINDS)
        )
        self.assertNotIn("electric_transformer", machine_blob)
        catalog = io.load_json(HISTORICAL)
        historical_ids = [row["id"] for row in catalog["profiles"]]
        self.assertEqual(5, len(historical_ids))
        self.assertNotIn(
            "cruciblecraft:electric_transformer_ulv_lv", historical_ids
        )

    def test_art_manifest_is_local_gt6_w(self) -> None:
        manifest = io.load_json(MANIFEST)
        self.assertEqual(
            "gt6_referencable_port_code/gregtech6_w",
            manifest["source"],
        )
        self.assertEqual(15, len(manifest["imports"]))
        assets = ROOT / "src" / "main" / "resources"
        for row in manifest["imports"]:
            self.assertTrue(row["gt6_source"].startswith("assets/gregtech/"))
            self.assertIn(
                "textures/block/machine/transformer/",
                row["destination"],
            )
            self.assertNotIn("multiblock_casing", row["destination"])
            self.assertNotIn("conveyor_cover", row["destination"])
            self.assertTrue(
                (assets / row["destination"]).is_file(), row["destination"]
            )
        casing = io.load_json(CASING_MANIFEST)
        self.assertEqual(
            "gt6_referencable_port_code/gregtech6_w",
            casing["source"],
        )
        self.assertTrue(
            (
                assets
                / "assets/cruciblecraft/textures/block/material/machine_casing.png"
            ).is_file()
        )
        self.assertTrue(
            (
                ROOT
                / "src"
                / "main"
                / "resources"
                / "assets"
                / "cruciblecraft"
                / "models"
                / "block"
                / "machine_casing.json"
            ).is_file()
        )

    def test_capability_is_player_complete(self) -> None:
        capability = ledger.load_capability(CAPABILITY)
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(CAPABILITY_SLUG, capability["slug"])
        self.assertEqual(SLUG, capability["wave_slug"])
        self.assertEqual(
            "tools/capabilities/energy/transformers/player_signoff.json",
            capability["player_signoff"],
        )
        self.assertEqual(["energy/batteries"], capability["depends_on"])
        self.assertEqual(
            ["capability-runtime", "player-complete"],
            capability["profiles"],
        )
        keys = {
            row["semantic_key"] for row in capability["identity_disposition"]
        }
        self.assertEqual(
            {
                "transformer:electric",
                "transformer:long_distance",
                "transformer:rotation_gearbox",
            },
            keys,
        )
        electric = next(
            row
            for row in capability["identity_disposition"]
            if row["semantic_key"] == "transformer:electric"
        )
        self.assertEqual(9, len(electric["runtime_ids"]))
        self.assertIn(
            "cruciblecraft:electric_transformer_uv_puv1",
            electric["runtime_ids"],
        )
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(
            {"transformer:long_distance", "transformer:rotation_gearbox"},
            blocked,
        )
        self.assertIn("no stand-in ingredients", capability["note"])
        self.assertIn("missing parts block player_complete", capability["note"])
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_runtime_transformers"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())

    def test_census_and_sealed_growth_order_stay_untouched(self) -> None:
        census = io.load_json(CENSUS)
        batteries = next(
            row
            for row in census["category_summaries"]
            if row["category"] == "Batteries"
        )
        self.assertEqual(37, batteries["source_sites"])
        growth = io.load_json(SEALED)
        self.assertIn("tracks", growth)
        spec = spec_for("portfolio/exclusion-reclaim-r0")
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)


if __name__ == "__main__":
    unittest.main()
