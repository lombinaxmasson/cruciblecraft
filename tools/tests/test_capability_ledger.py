#!/usr/bin/env python3
"""Capability ledger schema, adapter, and impact graph."""
from __future__ import annotations

import json
import unittest
from pathlib import Path
from unittest import mock

from tools import capability_ledger as ledger
from tools import io_common as io

ROOT = io.ROOT
SCHEMA = io.TOOLS / "capabilities" / "schema.json"
FLUID = "logistics/fluid-network/basic-transfer"
ITEM = "logistics/item-network-core"
GENERIC = "logistics/generic-network/core"
CORE = "logistics/logistics-core"
DISPLAY = "logistics/display-cpu"
CONVERTER = "energy/converter-catalog"
BATTERIES = "energy/batteries"
TRANSFORMERS = "energy/transformers"
HEAT_EXCHANGERS = "energy/heat-exchangers"
COOLER = "energy/cooler"
ROLL_FORMER = "machines/roll-former"
CLUSTER_MILL = "machines/cluster-mill"
HAMMER_SQUEEZER_LASER = "machines/hammer-squeezer-laser"
SLICER = "machines/slicer"
INJECTOR = "machines/injector"
LAMINATOR = "machines/laminator"
LOOM = "machines/loom"
MELTER = "machines/melter"
NANOFAB = "machines/nanofab"
PRESSURE_WASHER = "machines/pressure-washer"
WIRE_CABLE_FOLD = "content/electric-wire-cable-mte-fold"
CONNECTOR_ART = "content/gt6-connector-art"
ALIAS_REPAIR = "content/gt6-connector-alias-repair"
FLUID_COMBO = "content/gt6-fluid-combo-pipe-runtime"
RESTRICTIVE = "content/gt6-restrictive-item-pipe-runtime"
FLUID_RUNTIME = "content/gt6-fluid-pipe-runtime"
EU_RUNTIME = "content/gt6-eu-wire-cable-runtime"
MISSING_GAUGES = "content/gt6-eu-missing-wire-gauges-runtime"
INSULATED_REDSTONE = "content/gt6-insulated-redstone-runtime"
DANGEROUS_MEDIA = "content/gt6-fluid-dangerous-media-runtime"
FLUID_PIPE_ACQUISITION = "content/gt6-fluid-pipe-acquisition"
ITEM_PIPE_ACQUISITION = "content/gt6-item-pipe-acquisition"
EU_CABLE_ACQUISITION = "content/gt6-eu-cable-acquisition"
REDSTONE_WIRE_ACQUISITION = "content/gt6-redstone-wire-acquisition"
CONVERTER_HOST_FOLD = "content/gt6-mte-converter-host-fold"
HOPPER_HOST_FOLD = "content/gt6-mte-hopper-host-fold"
PROCESSING_HOST_FOLD = "content/gt6-mte-processing-host-fold"
REACTOR_ROD_HOST_FOLD = "content/gt6-mte-reactor-rod-host-fold"
FLUID_ATTACHMENTS_RUNTIME = "content/gt6-mte-fluid-attachments-runtime"
EXTENDER_RUNTIME = "content/gt6-mte-extender-runtime"
PAPER_TINY_PLATE = "content/gt6-paper-tiny-plate"
FOUNDATION = "content/technological-parts-foundation"
TREES = "worldgen/gt-trees"
CROPS = "worldgen/gt-crops"
DUNGEON = "worldgen/gt-dungeon"
LANGUAGE = "localization/language-key-display-name-normalization"
SANDING = "machines/sanding"
OVEN = "machines/oven"
SENSORS = "content/sensors"
CATALOG_MODERN = "registry/catalog-modern-ids"
MTE_REDSTONE = "content/mte-redstone-wire"
REDSTONE_CORRECTION = "content/gt6-redstone-wire-correction"
CRUCIBLE_MOLD_CORRECTION = "content/gt6-crucible-mold-behavior-correction"
NUCLEAR = "energy/nuclear-fission-survival"
HOT_FLUIDS = "energy/nuclear-fission-hot-fluids"
OBSERVATION = "energy/nuclear-fission-observation-safety"
LARGE_HEX = "energy/large-heat-exchanger"
STEAM_TURBINE = "energy/steam-turbine"
SMALL_GAS_TURBINE = "energy/small-gas-turbine"
FUSION_QUANTUM = "energy/fusion-quantum"
QUANTUM_MASSFAB = "energy/quantum-massfab"
PUV_OMEGA_PARTS = "content/puv-omega-parts"
PUV_OMEGA_MATRIX = "machines/puv-omega-matrix"
ELECTRIC_TOOLS = "content/gt6-electric-tools"
LARGE_GAS_TURBINE = "energy/large-gas-turbine"
LARGE_AUTOCLAVE = "machines/large-autoclave"
LARGE_ELECTROLYZER = "machines/large-electrolyzer"
LARGE_FERMENTER = "machines/large-fermenter"
LARGE_PROCESSING_PARTS = "machines/large-processing-parts"
LARGE_SHREDDER = "machines/large-shredder"
IMPLOSION_COMPRESSOR = "machines/implosion-compressor"
GT6_COIL_HOSTS = "machines/gt6-coil-hosts"


class CapabilityLedgerTest(unittest.TestCase):
    def test_schema_and_slugs_are_loadable(self) -> None:
        schema = json.loads(SCHEMA.read_text(encoding="utf-8"))
        self.assertEqual(2, schema["properties"]["schema_version"]["const"])
        self.assertNotIn("evidence", schema["properties"])
        self.assertIn("required_test_ids", schema["properties"])
        documents = [
            ledger.load_capability(path) for path in ledger.capability_files()
        ]
        self.assertTrue(all("evidence" not in row for row in documents))
        slugs = [row["slug"] for row in documents]
        self.assertIn(FLUID, slugs)
        self.assertIn("logistics/cover-net-r0", slugs)
        self.assertIn("logistics/cover-remainder", slugs)
        self.assertIn(ITEM, slugs)
        self.assertIn(GENERIC, slugs)
        self.assertIn(CORE, slugs)
        self.assertIn(DISPLAY, slugs)
        self.assertIn(CONVERTER, slugs)
        self.assertIn(BATTERIES, slugs)
        self.assertIn(TRANSFORMERS, slugs)
        self.assertIn(HEAT_EXCHANGERS, slugs)
        self.assertIn(COOLER, slugs)
        self.assertIn(ROLL_FORMER, slugs)
        self.assertIn(CLUSTER_MILL, slugs)
        self.assertIn(NUCLEAR, slugs)
        self.assertIn(WIRE_CABLE_FOLD, slugs)
        self.assertIn(FOUNDATION, slugs)
        self.assertIn(TREES, slugs)
        self.assertIn(CROPS, slugs)
        self.assertIn(DUNGEON, slugs)
        self.assertIn(SANDING, slugs)
        self.assertIn(OVEN, slugs)
        self.assertIn(SENSORS, slugs)
        self.assertIn(HOT_FLUIDS, slugs)
        self.assertIn(OBSERVATION, slugs)
        self.assertIn("registry/tool-head-remainder", slugs)
        self.assertIn(CATALOG_MODERN, slugs)
        self.assertIn(LANGUAGE, slugs)
        self.assertIn("content/gt6-mte-inplace-acquisition", slugs)
        self.assertIn(MTE_REDSTONE, slugs)
        self.assertIn(REDSTONE_CORRECTION, slugs)
        self.assertIn(CRUCIBLE_MOLD_CORRECTION, slugs)
        self.assertIn(INSULATED_REDSTONE, slugs)
        self.assertIn(DANGEROUS_MEDIA, slugs)
        self.assertIn(FLUID_PIPE_ACQUISITION, slugs)
        self.assertIn(ITEM_PIPE_ACQUISITION, slugs)
        self.assertIn(EU_CABLE_ACQUISITION, slugs)
        self.assertIn(REDSTONE_WIRE_ACQUISITION, slugs)
        self.assertIn(CONVERTER_HOST_FOLD, slugs)
        self.assertIn(HOPPER_HOST_FOLD, slugs)
        self.assertIn(PROCESSING_HOST_FOLD, slugs)
        self.assertIn(REACTOR_ROD_HOST_FOLD, slugs)
        from tools import gt6_mte_inplace_runtime as inplace_runtime
        for spec in inplace_runtime.DOMAINS.values():
            cap = (
                ROOT
                / "tools"
                / "capabilities"
                / spec["slug"]
                / "capability.json"
            )
            if cap.is_file():
                self.assertIn(spec["slug"], slugs)
        converter = next(row for row in documents if row["slug"] == CONVERTER)
        self.assertEqual("runtime_ready", converter["maturity"])
        self.assertEqual("accepted", converter["workflow"])
        batteries = next(row for row in documents if row["slug"] == BATTERIES)
        self.assertEqual("runtime_ready", batteries["maturity"])
        self.assertEqual("accepted", batteries["workflow"])
        transformers = next(
            row for row in documents if row["slug"] == TRANSFORMERS
        )
        self.assertEqual("runtime_ready", transformers["maturity"])
        self.assertEqual("accepted", transformers["workflow"])
        nuclear = next(row for row in documents if row["slug"] == NUCLEAR)
        self.assertEqual("runtime_ready", nuclear["maturity"])
        self.assertEqual("accepted", nuclear["workflow"])
        hot_fluids = next(row for row in documents if row["slug"] == HOT_FLUIDS)
        self.assertEqual("runtime_ready", hot_fluids["maturity"])
        self.assertEqual("accepted", hot_fluids["workflow"])
        observation = next(row for row in documents if row["slug"] == OBSERVATION)
        self.assertEqual("runtime_ready", observation["maturity"])
        self.assertEqual("accepted", observation["workflow"])
        heat = next(row for row in documents if row["slug"] == HEAT_EXCHANGERS)
        self.assertEqual("runtime_ready", heat["maturity"])
        self.assertEqual("accepted", heat["workflow"])
        self.assertEqual(
            ["capability-runtime"],
            heat["profiles"],
        )
        roll_former = next(row for row in documents if row["slug"] == ROLL_FORMER)
        self.assertEqual("runtime_ready", roll_former["maturity"])
        self.assertEqual("accepted", roll_former["workflow"])
        self.assertEqual(
            ["capability-runtime"],
            roll_former["profiles"],
        )
        cluster_mill = next(row for row in documents if row["slug"] == CLUSTER_MILL)
        self.assertEqual("runtime_ready", cluster_mill["maturity"])
        self.assertEqual("accepted", cluster_mill["workflow"])
        self.assertEqual(
            ["capability-runtime"],
            cluster_mill["profiles"],
        )
        fluid = next(row for row in documents if row["slug"] == FLUID)
        self.assertEqual("runtime_ready", fluid["maturity"])
        self.assertEqual("accepted", fluid["workflow"])
        cover = next(
            row for row in documents if row["slug"] == "logistics/cover-net-r0"
        )
        self.assertEqual("frozen", cover["maturity"])
        item = next(row for row in documents if row["slug"] == ITEM)
        self.assertEqual("runtime_ready", item["maturity"])
        self.assertEqual("accepted", item["workflow"])
        generic = next(row for row in documents if row["slug"] == GENERIC)
        self.assertEqual("runtime_ready", generic["maturity"])
        self.assertEqual("accepted", generic["workflow"])
        core = next(row for row in documents if row["slug"] == CORE)
        self.assertEqual("runtime_ready", core["maturity"])
        self.assertEqual("accepted", core["workflow"])
        display = next(row for row in documents if row["slug"] == DISPLAY)
        self.assertEqual("runtime_ready", display["maturity"])
        self.assertEqual("accepted", display["workflow"])
        self.assertEqual(
            [
                "coverIdentitySurvivesBlockEntityReload",
                "coversAreSurvivalCraftable",
                "differentIdentityIsInvisible",
                "disconnectedPipesAreNotOneNetwork",
                "importPullsFromStorage",
                "loadAxisCapsAreRecorded",
                "noTargetDoesNotSwallowFluids",
                "playerSurfaceIsRegistered",
                "sameIdentityConnectedExportsIntoStorage",
            ],
            fluid["required_test_ids"],
        )

    def test_compiled_ledger_has_no_legacy_progress_adapter(self) -> None:
        compiled = ledger.compile_ledger()
        self.assertNotIn("legacy_readiness", compiled)
        for row in compiled["capabilities"]:
            self.assertNotIn("legacy_readiness", row)
            self.assertNotIn("wave_slug", row)
        complete = compiled["declared_player_complete"]
        self.assertEqual([], complete)
        self.assertNotIn("player-complete", compiled["profiles"])
        self.assertNotIn(CLUSTER_MILL, complete)
        self.assertNotIn(ROLL_FORMER, complete)
        self.assertNotIn(HEAT_EXCHANGERS, complete)
        active = [
            row["slug"]
            for row in compiled["capabilities"]
            if row["workflow"] == "active"
        ]
        self.assertLessEqual(len(active), 1)
        self.assertEqual(
            compiled["unique_active_slug"],
            active[0] if active else None,
        )
        self.assertEqual(
            "runtime_ready is the close maturity; survival_access is independent "
            "and does not gate close; playtest is a project-level human cycle",
            compiled["progress_rule"],
        )

    def test_unique_active_is_the_single_workflow_active_capability(self) -> None:
        compiled = ledger.compile_ledger()
        active = [
            row["slug"]
            for row in compiled["capabilities"]
            if row["workflow"] == "active"
        ]
        self.assertLessEqual(len(active), 1)
        self.assertEqual(
            compiled["unique_active_slug"],
            active[0] if active else None,
        )
        plans = ledger.load_card_plan_index()
        if not active:
            self.assertEqual({}, plans["active"])
        else:
            self.assertEqual({active[0]}, set(plans["active"]))
        self.assertIn(
            MTE_REDSTONE,
            ledger.load_card_plan_index()["closed"],
        )
        self.assertIn(
            CATALOG_MODERN,
            ledger.load_card_plan_index()["closed"],
        )
        self.assertIn(
            "logistics/cover-remainder",
            ledger.load_card_plan_index()["closed"],
        )
        self.assertIn(SENSORS, ledger.load_card_plan_index()["closed"])
        self.assertIn(
            TREES,
            ledger.load_card_plan_index()["closed"],
        )
        self.assertIn(
            CROPS,
            ledger.load_card_plan_index()["closed"],
        )
        self.assertIn(
            WIRE_CABLE_FOLD,
            ledger.load_card_plan_index()["closed"],
        )
        self.assertNotIn(
            "worldgen/gt-trees",
            ledger.load_card_plan_index()["prep"],
        )
        self.assertIn(DUNGEON, ledger.load_card_plan_index()["prep"])
        self.assertNotIn(LANGUAGE, ledger.load_card_plan_index()["prep"])
        self.assertIn(LANGUAGE, ledger.load_card_plan_index()["closed"])
        plans = ledger.load_card_plan_index()
        self.assertTrue(
            ("content/gt6-mte-inplace-acquisition" in plans["active"])
            ^ ("content/gt6-mte-inplace-acquisition" in plans["closed"])
        )
        self.assertNotIn(SANDING, ledger.load_card_plan_index()["prep"])
        self.assertIn(SANDING, ledger.load_card_plan_index()["closed"])
        self.assertIn(OVEN, ledger.load_card_plan_index()["closed"])
        self.assertIn(
            "machines/slicer",
            ledger.load_card_plan_index()["closed"],
        )
        self.assertIn(HAMMER_SQUEEZER_LASER, ledger.load_card_plan_index()["closed"])

    def test_ledger_contains_profiles_and_impact_without_proof_fields(self) -> None:
        compiled = ledger.compile_ledger()
        self.assertEqual([], compiled["declared_player_complete"])
        self.assertNotIn("player-complete", compiled["profiles"])
        self.assertIn(CLUSTER_MILL, compiled["profiles"]["capability-runtime"])
        self.assertEqual(
            {
                BATTERIES,
                TRANSFORMERS,
                NUCLEAR,
                HOT_FLUIDS,
                OBSERVATION,
                HEAT_EXCHANGERS,
                "logistics/cover-net-r0",
                ITEM,
                FLUID,
                GENERIC,
                CORE,
                DISPLAY,
                CONVERTER,
                ROLL_FORMER,
                CLUSTER_MILL,
                SLICER,
                HAMMER_SQUEEZER_LASER,
                INJECTOR,
                LAMINATOR,
                LOOM,
                MELTER,
                NANOFAB,
                PRESSURE_WASHER,
                WIRE_CABLE_FOLD,
                ALIAS_REPAIR,
                CONNECTOR_ART,
                EU_RUNTIME,
                MISSING_GAUGES,
                FLUID_COMBO,
                FLUID_PIPE_ACQUISITION,
                ITEM_PIPE_ACQUISITION,
                EU_CABLE_ACQUISITION,
                RESTRICTIVE,
                FOUNDATION,
                SENSORS,
                TREES,
                DUNGEON,
                SANDING,
                OVEN,
                PAPER_TINY_PLATE,
                CROPS,
                "logistics/cover-remainder",
                LARGE_HEX,
                SMALL_GAS_TURBINE,
                STEAM_TURBINE,
                FUSION_QUANTUM,
                QUANTUM_MASSFAB,
                PUV_OMEGA_PARTS,
                PUV_OMEGA_MATRIX,
                ELECTRIC_TOOLS,
                LARGE_GAS_TURBINE,
                LARGE_AUTOCLAVE,
                "energy/gt6-laser-magnet-zpm-converters",
                "energy/gt6-remainder-devices",
                "machines/bedrock-drill",
                "machines/large-centrifuge",
                "machines/large-crusher",
                LARGE_ELECTROLYZER,
                LARGE_FERMENTER,
                "machines/large-mixer",
                "machines/large-oven",
                LARGE_PROCESSING_PARTS,
                LARGE_SHREDDER,
                "machines/large-sluice",
                "machines/large-squeezer",
                IMPLOSION_COMPRESSOR,
                GT6_COIL_HOSTS,
            },
            set(compiled["impact"]["logistics/cover-net-r0"]),
        )
        for capability in compiled["capabilities"]:
            self.assertNotIn("evidence", capability)
            self.assertNotIn("evidence_root", capability)
            self.assertNotIn("owned_hash_count", capability)

    def test_committed_ledger_is_deterministic(self) -> None:
        committed = (io.TOOLS / "capabilities" / "ledger.json").read_bytes()
        self.assertEqual(ledger.dumps(ledger.compile_ledger()), committed)

    def test_shared_cover_code_hits_cover_and_dependents(self) -> None:
        hit = ledger.affected_slugs(
            [
                "src/main/java/com/masson/cruciblecraft/logistics/pipe/cover/"
                "PipeCoverSet.java"
            ]
        )
        self.assertIn("logistics/cover-net-r0", hit)
        self.assertIn(ITEM, hit)
        self.assertIn(FLUID, hit)
        self.assertIn(GENERIC, hit)
        self.assertIn(CORE, hit)
        self.assertIn(DISPLAY, hit)
        self.assertIn(CONVERTER, hit)
        self.assertIn(BATTERIES, hit)
        self.assertIn(TRANSFORMERS, hit)
        self.assertIn(NUCLEAR, hit)
        self.assertIn(HOT_FLUIDS, hit)
        self.assertIn(OBSERVATION, hit)
        self.assertIn(HEAT_EXCHANGERS, hit)
        self.assertIn(ROLL_FORMER, hit)
        self.assertIn(CLUSTER_MILL, hit)
        self.assertNotIn("registry/tool-head-remainder", hit)

    def test_fluid_pipe_does_not_stale_tool_head_remainder(self) -> None:
        hit = ledger.affected_slugs(
            [
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                "FluidPipeBlockEntity.java"
            ]
        )
        self.assertEqual(
            [
                WIRE_CABLE_FOLD,
                ALIAS_REPAIR,
                CONNECTOR_ART,
                ELECTRIC_TOOLS,
                EU_CABLE_ACQUISITION,
                MISSING_GAUGES,
                EU_RUNTIME,
                FLUID_COMBO,
                DANGEROUS_MEDIA,
                FLUID_PIPE_ACQUISITION,
                FLUID_RUNTIME,
                ITEM_PIPE_ACQUISITION,
                EXTENDER_RUNTIME,
                FLUID_ATTACHMENTS_RUNTIME,
                PAPER_TINY_PLATE,
                RESTRICTIVE,
                PUV_OMEGA_PARTS,
                SENSORS,
                FOUNDATION,
                BATTERIES,
                CONVERTER,
                FUSION_QUANTUM,
                "energy/gt6-laser-magnet-zpm-converters",
                "energy/gt6-remainder-devices",
                HEAT_EXCHANGERS,
                LARGE_GAS_TURBINE,
                LARGE_HEX,
                HOT_FLUIDS,
                OBSERVATION,
                NUCLEAR,
                QUANTUM_MASSFAB,
                SMALL_GAS_TURBINE,
                STEAM_TURBINE,
                TRANSFORMERS,
                DISPLAY,
                FLUID,
                GENERIC,
                CORE,
                "machines/bedrock-drill",
                CLUSTER_MILL,
                GT6_COIL_HOSTS,
                HAMMER_SQUEEZER_LASER,
                IMPLOSION_COMPRESSOR,
                INJECTOR,
                LAMINATOR,
                LARGE_AUTOCLAVE,
                "machines/large-centrifuge",
                "machines/large-crusher",
                LARGE_ELECTROLYZER,
                LARGE_FERMENTER,
                "machines/large-mixer",
                "machines/large-oven",
                LARGE_PROCESSING_PARTS,
                LARGE_SHREDDER,
                "machines/large-sluice",
                "machines/large-squeezer",
                LOOM,
                MELTER,
                NANOFAB,
                OVEN,
                PRESSURE_WASHER,
                PUV_OMEGA_MATRIX,
                ROLL_FORMER,
                SANDING,
                SLICER,
                CROPS,
                DUNGEON,
                TREES,
            ],
            hit,
        )

    def test_player_complete_promotions_detect_maturity_change(self) -> None:
        previous = json.dumps({"maturity": "runtime_ready"})
        with mock.patch.object(
            ledger,
            "_git_show",
            side_effect=lambda revision, relative: (
                previous
                if relative.endswith("basic-transfer/capability.json")
                else json.dumps({"maturity": "player_complete"})
            ),
        ):
            self.assertEqual(
                [],
                ledger.player_complete_promotions("origin/main"),
            )

    def test_player_complete_promotions_ignore_already_complete(self) -> None:
        with mock.patch.object(
            ledger,
            "_git_show",
            side_effect=lambda revision, relative: json.dumps(
                {"maturity": "player_complete"}
            ),
        ):
            self.assertEqual([], ledger.player_complete_promotions("HEAD"))


if __name__ == "__main__":
    unittest.main()
