#!/usr/bin/env python3
"""GT6 distillation / cryo distillation tower: structure, recipes, art, lane."""
from __future__ import annotations

import json
import shutil
import stat
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import census_common as census
from tools import io_common as io
from tools.waves.prep import machine_prep_common as common

SLUG = "machines/distillation-tower"
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
WAVE = ROOT / "tools" / "waves" / "machines" / "distillation-tower"
CAPABILITY = ROOT / "tools" / "capabilities" / SLUG / "capability.json"
PLAN_STEM = "蒸馏塔GT6对齐详细计划.md"
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / PLAN_STEM
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / PLAN_STEM
STRUCTURE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "multiblock_structures"
    / "distillation_tower.json"
)
RECIPE_HOT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "hydrocarbon"
    / "distillation_tower"
)
RECIPE_CRYO = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "hydrocarbon"
    / "cryo_distillation_tower"
)
CRAFT_HOT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "machines"
    / "distillation_tower.json"
)
CRAFT_CRYO = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "machines"
    / "cryo_distillation_tower.json"
)
DELIVERY = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "machine_delivery.json"
)
MANIFEST_HOT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_distillation_tower_art_manifest.json"
)
MANIFEST_CRYO = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_cryo_distillation_tower_art_manifest.json"
)
MANIFEST_PARTS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_distillation_tower_part_art_manifest.json"
)
DUMMY_CONTROLLER_PATH = "multiblock/distillation_tower"
LIVE_CONTROLLER_PATH = "distillation_tower"
DUMMY_CONTROLLER_ID = f"cruciblecraft:{DUMMY_CONTROLLER_PATH}"
LIVE_CONTROLLER_ID = f"cruciblecraft:{LIVE_CONTROLLER_PATH}"
INPLACE_CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "mte_inplace_catalog.json"
)
DUMMY_CRAFT = (
    ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "multiblock"
    / "distillation_tower.json"
)
ACQUISITION_CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "mte_inplace_acquisition.json"
)
SMELTER_CATALOGS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "smelter_mte_identity_catalog.json",
    ROOT / "tools" / "smelter_mte_identity_catalog.json",
)
MODERN_MAP = ROOT / "tools" / "catalog_modern_id_map.json"
IDENTITY_MANIFEST = ROOT / "tools" / "registry_identity_manifest.json"
RECYCLE_0755 = (
    ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "smelter"
    / "deferred_recycling"
    / "stainless_steel"
    / "gt_recipe_smelter_0755.json"
)
DUMMY_ASSETS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "blockstates"
    / "multiblock"
    / "distillation_tower.json",
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "models"
    / "item"
    / "multiblock"
    / "distillation_tower.json",
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "loot_table"
    / "blocks"
    / "multiblock"
    / "distillation_tower.json",
)
GT6_MULTIBLOCK_PARTS = (
    common.GT6_ART
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "blocks"
    / "machines"
    / "multiblockparts"
)
ASSETS = common.ASSETS
CHEM_SRC = (
    "gt6_code/gregtech6/src/main/java/gregtech/loaders/c/"
    "Loader_Recipes_Chem.java"
)
SMALL_ORES_CAPABILITY = (
    ROOT / "tools" / "capabilities" / "worldgen" / "gt-small-ores" / "capability.json"
)
COOLER_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "冷却器详细计划.md"
COOLER_PREP = ROOT / "docs" / "history" / "card-plans" / "prep" / "冷却器详细计划.md"
COOLER_CAPABILITY = (
    ROOT / "tools" / "capabilities" / "energy" / "cooler" / "capability.json"
)
COOLER_TOPOLOGY = ROOT / "tools" / "waves" / "runtime" / "cooler" / "topology.json"
COOLER_READINESS = ROOT / "tools" / "waves" / "runtime" / "cooler" / "readiness.json"

EXPECTED_TESTS = [
    "distillationTowerFidelity",
    "distillationTowerFormation",
    "distillationTowerPortSupplySingleHost",
    "distillationTowerTeardown",
    "distillationTowerOutputJam",
    "distillationTowerPowerLoss",
    "distillationTowerReload",
    "distillationTowerSaveQuarantine",
    "distillationTowerHeightRoutesFuelAndPetrol",
    "cryoDistillationTowerFormation",
]


def _dump(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def _element(x: int, y: int, z: int, predicate: str) -> dict[str, Any]:
    return {"offset": [x, y, z], "predicate": predicate}


def structure_document() -> dict[str, Any]:
    """GT6 north-facing: tower sits behind the controller (local +Z)."""
    structure: list[dict[str, Any]] = [_element(0, 0, 0, "C")]
    for z in range(0, 3):
        for x in range(-1, 2):
            structure.append(_element(x, -1, z, "E"))
    for z in range(0, 3):
        for x in range(-1, 2):
            if x == 0 and z == 0:
                continue
            structure.append(_element(x, 0, z, "P"))
    for y in range(1, 8):
        for z in range(0, 3):
            for x in range(-1, 2):
                structure.append(_element(x, y, z, "F"))
    return {
        "schema_version": 1,
        "palette": {
            "C": {
                "type": "controller",
                "tag": "cruciblecraft:distillation_tower_controllers",
            },
            "E": {
                "type": "port",
                "block": "cruciblecraft:multiblock/heat_transmitter",
                "port": "energy_input",
            },
            "P": {
                "type": "port",
                "block": "cruciblecraft:multiblock/distillation_tower_part",
                "port": "item_fluid",
            },
            "F": {
                "type": "port",
                "block": "cruciblecraft:multiblock/distillation_tower_part",
                "port": "fluid_out",
            },
        },
        "structure": structure,
        "anchors": {
            "center": [0, 0, 1],
            "bottom_energy_input": [0, -1, 1],
            "back_hole": [0, 0, 3],
        },
        "source": {
            "repository": "gt6_code/gregtech6",
            "revision": SOURCE_REVISION,
            "class": "gregtech.tileentity.multiblocks.MultiTileEntityDistillationTower",
            "method": "checkStructure2",
        },
    }


def _gt_recipe(
    *,
    recipe_map: str,
    duration: int,
    eut: int,
    fluid_inputs: list[dict[str, Any]],
    fluid_outputs: list[dict[str, Any]],
    item_outputs: list[dict[str, Any]] | None = None,
    chances: list[int] | None = None,
    line: int,
) -> dict[str, Any]:
    body: dict[str, Any] = {
        "can_be_buffered": True,
        "duration": duration,
        "eut": eut,
        "fluid_inputs": fluid_inputs,
        "fluid_outputs": fluid_outputs,
        "map": recipe_map,
        "provenance": {
            "evidence_hashes": [SOURCE_REVISION],
            "selected_source_recipe": f"{CHEM_SRC}:{line}",
            "source_kind": "gt6_java_loader",
        },
        "type": "cruciblecraft:gt_recipe",
    }
    if item_outputs:
        body["item_outputs"] = item_outputs
    if chances:
        if len(chances) != len(item_outputs or []):
            raise ValueError(
                "GT6 distillation output chances must match item outputs: "
                f"{len(chances)} != {len(item_outputs or [])}"
            )
        body["output_chances"] = chances
    return body


def _fluid(path: str, amount: int) -> dict[str, Any]:
    return {"amount": amount, "id": f"cruciblecraft:{path}"}


def _tiny(material: str) -> dict[str, Any]:
    return {"count": 1, "item": f"cruciblecraft:{material}/tiny_dust"}


def hot_recipes() -> dict[str, dict[str, Any]]:
    dusts = [_tiny("wax_paraffin"), _tiny("asphalt"), _tiny("petroleum_coke")]
    recipes = {
        "biomass.json": _gt_recipe(
            recipe_map="cruciblecraft:distillation_tower",
            duration=16,
            eut=64,
            fluid_inputs=[_fluid("biomass", 80)],
            fluid_outputs=[
                _fluid("ethanol", 20),
                _fluid("glycerol", 20),
                _fluid("methane", 4),
                _fluid("water_distilled", 50),
            ],
            line=350,
        ),
        "oil_extra_heavy.json": _gt_recipe(
            recipe_map="cruciblecraft:distillation_tower",
            duration=256,
            eut=64,
            fluid_inputs=[_fluid("oil_extra_heavy", 25)],
            fluid_outputs=[
                _fluid("fuel", 35),
                _fluid("diesel", 25),
                _fluid("kerosine", 25),
                _fluid("petrol", 20),
                _fluid("propane", 5),
                _fluid("butane", 5),
                _fluid("lubricant", 50),
            ],
            item_outputs=dusts,
            chances=[5000, 5000, 5000],
            line=352,
        ),
        "oil_heavy.json": _gt_recipe(
            recipe_map="cruciblecraft:distillation_tower",
            duration=196,
            eut=64,
            fluid_inputs=[_fluid("oil_heavy", 25)],
            fluid_outputs=[
                _fluid("fuel", 30),
                _fluid("diesel", 20),
                _fluid("kerosine", 20),
                _fluid("petrol", 15),
                _fluid("propane", 10),
                _fluid("butane", 10),
                _fluid("lubricant", 40),
            ],
            item_outputs=dusts,
            chances=[4000, 4000, 4000],
            line=353,
        ),
        "oil_medium.json": _gt_recipe(
            recipe_map="cruciblecraft:distillation_tower",
            duration=128,
            eut=64,
            fluid_inputs=[_fluid("oil_medium", 25)],
            fluid_outputs=[
                _fluid("fuel", 25),
                _fluid("diesel", 15),
                _fluid("kerosine", 15),
                _fluid("petrol", 15),
                _fluid("propane", 15),
                _fluid("butane", 15),
                _fluid("lubricant", 25),
            ],
            item_outputs=dusts,
            chances=[3000, 3000, 3000],
            line=355,
        ),
        "oil.json": _gt_recipe(
            recipe_map="cruciblecraft:distillation_tower",
            duration=128,
            eut=64,
            fluid_inputs=[_fluid("oil", 25)],
            fluid_outputs=[
                _fluid("fuel", 25),
                _fluid("diesel", 15),
                _fluid("kerosine", 15),
                _fluid("petrol", 15),
                _fluid("propane", 15),
                _fluid("butane", 15),
                _fluid("lubricant", 25),
            ],
            item_outputs=dusts,
            chances=[3000, 3000, 3000],
            line=356,
        ),
        "oil_light.json": _gt_recipe(
            recipe_map="cruciblecraft:distillation_tower",
            duration=64,
            eut=64,
            fluid_inputs=[_fluid("oil_light", 25)],
            fluid_outputs=[
                _fluid("fuel", 15),
                _fluid("diesel", 10),
                _fluid("kerosine", 10),
                _fluid("petrol", 10),
                _fluid("propane", 25),
                _fluid("butane", 25),
                _fluid("lubricant", 15),
            ],
            item_outputs=dusts,
            chances=[2000, 2000, 2000],
            line=358,
        ),
    }
    return recipes


def cryo_recipes() -> dict[str, dict[str, Any]]:
    return {
        "air.json": _gt_recipe(
            recipe_map="cruciblecraft:cryo_distillation_tower",
            duration=64,
            eut=64,
            fluid_inputs=[_fluid("air", 200)],
            fluid_outputs=[
                _fluid("nitrogen", 143),
                _fluid("oxygen", 50),
                _fluid("carbon_dioxide", 10),
                _fluid("helium", 1),
                _fluid("neon", 1),
                _fluid("argon", 1),
            ],
            item_outputs=[_tiny("ice")],
            chances=[9000],
            line=363,
        )
    }


def craft_hot() -> dict[str, Any]:
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "key": {
            "M": {"item": "cruciblecraft:multiblock/distillation_tower_part"},
            "P": {"item": "cruciblecraft:stainless_steel/nonuple_fluid_pipe"},
        },
        "pattern": ["PPP", "PMP", "PPP"],
        "result": {"count": 1, "id": "cruciblecraft:distillation_tower"},
    }


def craft_cryo() -> dict[str, Any]:
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "key": {
            "M": {"item": "cruciblecraft:multiblock/distillation_tower_part"},
            "P": {"item": "cruciblecraft:copper/nonuple_fluid_pipe"},
        },
        "pattern": ["PPP", "PMP", "PPP"],
        "result": {"count": 1, "id": "cruciblecraft:cryo_distillation_tower"},
    }


def _owned_paths(*, closed: bool) -> list[str]:
    lane = "closed" if closed else "active"
    return [
        f"docs/history/card-plans/{lane}/{PLAN_STEM}",
        "src/main/java/com/masson/cruciblecraft/content/block/DistillationTowerBlock.java",
        "src/main/java/com/masson/cruciblecraft/content/block/CryoDistillationTowerBlock.java",
        "src/main/java/com/masson/cruciblecraft/content/block/MultiblockPortBlocks.java",
        "src/main/java/com/masson/cruciblecraft/content/block/MultiblockPortBlock.java",
        "src/main/java/com/masson/cruciblecraft/content/block/DistillationTowerParts.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/AbstractDistillationTowerBlockEntity.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/DistillationTowerBlockEntity.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/CryoDistillationTowerBlockEntity.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/DistillationTowerAutoOutput.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/DistillationTowerFluidRouting.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/DistillationTowerPartVisuals.java",
        "src/main/java/com/masson/cruciblecraft/client/color/DistillationTowerPartColor.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/MultiblockPortBlockEntity.java",
        "src/main/java/com/masson/cruciblecraft/content/multiblock/MultiblockStructureDefinition.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModMultiblockControllers.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModRecipeMaps.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModMenus.java",
        "src/main/resources/data/cruciblecraft/multiblock_structures/distillation_tower.json",
        "src/main/resources/data/cruciblecraft/recipe/hydrocarbon/distillation_tower/**",
        "src/main/resources/data/cruciblecraft/recipe/hydrocarbon/cryo_distillation_tower/**",
        "src/generated/resources/data/cruciblecraft/recipe/multiblock/distillation_tower.json",
        "src/main/resources/data/cruciblecraft/recipe/machines/cryo_distillation_tower.json",
        "src/main/resources/data/cruciblecraft/machine_delivery.json",
        "src/main/resources/assets/cruciblecraft/gt6_distillation_tower_art_manifest.json",
        "src/main/resources/assets/cruciblecraft/gt6_cryo_distillation_tower_art_manifest.json",
        "src/main/resources/assets/cruciblecraft/gt6_distillation_tower_part_art_manifest.json",
        "src/main/resources/assets/cruciblecraft/textures/block/machine/distillation_tower/**",
        "src/main/resources/assets/cruciblecraft/textures/block/machine/cryo_distillation_tower/**",
        "src/main/resources/assets/cruciblecraft/textures/block/machine/distillation_tower_part/**",
        "src/main/resources/assets/cruciblecraft/textures/block/machine/heat_transmitter/**",
        "tools/capabilities/machines/distillation-tower/**",
        "tools/waves/machines/distillation-tower/**",
        "tools/gt6_distillation_tower.py",
        "tools/tests/test_distillation_tower.py",
        "src/test/java/com/masson/cruciblecraft/content/blockentity/DistillationTowerPartVisualsTest.java",
        "src/test/java/com/masson/cruciblecraft/content/block/DistillationTowerPartsTest.java",
    ]


def capability_document(*, closed: bool) -> dict[str, Any]:
    return {
        "schema_version": 2,
        "slug": SLUG,
        "title": "Distillation Tower GT6 Alignment",
        "maturity": "runtime_ready" if closed else "frozen",
        "workflow": "accepted" if closed else "active",
        "survival_access": "partial",
        "owned_paths": _owned_paths(closed=closed),
        "depends_on": [],
        "profiles": ["capability-runtime"],
        "wave_slug": "machines/distillation-tower",
        "required_test_ids": EXPECTED_TESTS,
        "identity_disposition": [
            {
                "semantic_key": "machine:distillation_tower:17101",
                "disposition": "reuse_canonical",
                "runtime_ids": ["cruciblecraft:distillation_tower"],
                "reason": "Keep live controller id. Catalog dummy multiblock/distillation_tower is folded onto this BlockItem. GT6 17101, RM.DistillationTower 1/3/1/9, HU buffered 4096/1024.",
            },
            {
                "semantic_key": "machine:cryo_distillation_tower:17111",
                "disposition": "new_distinct",
                "runtime_ids": ["cruciblecraft:cryo_distillation_tower"],
                "reason": "GT6 MultiTileEntityCryoDistillationTower 17111. Same 18101/18102 parts, CU instead of HU, copper nonuple craft.",
            },
            {
                "semantic_key": "mte:heat_transmitter:18101",
                "disposition": "reuse_canonical",
                "runtime_ids": ["cruciblecraft:multiblock/heat_transmitter"],
                "reason": "GT6 18101 live ENERGY_INPUT under the tower. Same catalog id, now a MultiblockPort.",
            },
            {
                "semantic_key": "mte:distillation_tower_part:18102",
                "disposition": "reuse_canonical",
                "runtime_ids": ["cruciblecraft:multiblock/distillation_tower_part"],
                "reason": "GT6 18102 live structure part. One block, ITEM_FLUID at y=0 and FLUID_OUT at y=1..7 by bind mode.",
            },
            {
                "semantic_key": "multiblock:port:fluid_out",
                "disposition": "new_distinct",
                "runtime_ids": ["cruciblecraft:multiblock_fluid_out_port"],
                "reason": "Generic FLUID_OUT remains for other JSON hosts. Tower structure uses catalog 18102.",
            },
        ],
        "note": "Full GT6 tower alignment. Catalog dummy 17101 folded onto live distillation_tower. Skip soulsandoil, BiomassIC2, Oil_Heavy2/HotCrude/Light2, netherair/enderair. No form-open. No CONFIGURED_MACHINES. No stand-in ingredients. No placeholder art. Close at runtime_ready, not player_complete.",
    }


def plan_markdown(*, closed: bool) -> str:
    if closed:
        header = """# 蒸馏塔 GT6 对齐详细计划

> 计划 slug：`machines/distillation-tower`
> capability_slug         = machines/distillation-tower
> unique_active_wave      = null
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：蒸馏塔 / 低温蒸馏塔
> 性质：推翻 v1 把 `RM.DistillationTower` 并进 `RM.Distillery` 的 DESIGN_POLICY。
> 热塔是核心；低温塔共用 18101/18102 几何，CU 缓冲。
> 不是 `player_complete`。机器可读 `unique_active_wave = null`（已闭卡）。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = machines/distillation-tower
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
hosts                        = 17101 Distillation Tower + 17111 Cryo Distillation Tower
parts                        = 18101 heat transmitter ENERGY_INPUT + 18102 tower parts
panel                        = RM.DistillationTower / Cryo  1/3 item, 1/9 fluid
energy                       = HU 4096/1024 buffered；低温 CU 同窗
out_of_scope                 = soulsandoil；BiomassIC2；Oil_Heavy2/HotCrude/Light2；netherair/enderair；form-open
close_target                 = runtime_ready
```
"""
    else:
        header = """# 蒸馏塔 GT6 对齐详细计划

> 计划 slug：`machines/distillation-tower`
> capability_slug         = machines/distillation-tower
> unique_active_wave      = machines/distillation-tower
> 状态：unique-active / frozen
> 正式名称：蒸馏塔 / 低温蒸馏塔
> 性质：推翻 v1 把 `RM.DistillationTower` 并进 `RM.Distillery` 的 DESIGN_POLICY。
> 热塔是核心；低温塔共用 18101/18102 几何，CU 缓冲。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = unique-active
capability_slug              = machines/distillation-tower
unique_active_wave           = machines/distillation-tower
hosts                        = 17101 Distillation Tower + 17111 Cryo Distillation Tower
parts                        = 18101 heat transmitter ENERGY_INPUT + 18102 tower parts
panel                        = RM.DistillationTower / Cryo  1/3 item, 1/9 fluid
energy                       = HU 4096/1024 buffered；低温 CU 同窗
out_of_scope                 = soulsandoil；BiomassIC2；Oil_Heavy2/HotCrude/Light2；netherair/enderair；form-open
```
"""
    return header + """
来源：`MultiTileEntityDistillationTower` / `CryoDistillationTower`，
`Loader_MultiTileEntities.java` 17101/17111/18101/18102，
`Loader_Recipes_Chem.java` 350–365。

## 0. 开场判断

Live id 保持 `cruciblecraft:distillation_tower`。catalog `multiblock/distillation_tower` 折进这个 live BlockItem，不再注册第二份 17101。
几何 81 格：9 ENERGY_INPUT（y=-1）、8 ITEM_FLUID（y=0 除控制器）、63 FLUID_OUT（y=1..7）、1 控制器。
北向 JSON 时塔在控制器背后（local +Z）。远面孔 `relative(facing.opposite, 3)`。

不进 `CONFIGURED_MACHINES` / `CHEMICAL_HOST_MACHINES`。菜单与 EMI 走 `MULTIBLOCK_MENU_HOSTS`。

## 1. 获得格

| 身份 | GT6 | CC |
| --- | --- | --- |
| 17101 `M` | 18102 Distillation Tower Part | `multiblock/distillation_tower_part` |
| 17101 `P` | `OP.pipeNonuple(MT.StainlessSteel)` | `stainless_steel/nonuple_fluid_pipe` |
| 17111 `M` | 同一 18102 | 同一 part |
| 17111 `P` | `OP.pipeNonuple(ANY.Cu)` | `copper/nonuple_fluid_pipe` |

禁止 invar distillery + 8 个通用端口顶控制器。禁止 stand-in。

## 2. 配方

热塔 6 行：biomass、oil_extra_heavy、oil_heavy、oil_medium、oil（Oil_Normal）、oil_light。
低温 1 行：air。`liquid(U50)`=20 mB；`gas(U7,T)`=143、`U20`=50、`U100`=10、`U1000`=1。

高度：propane/methane +7，butane +6，petrol/ethanol +5，kerosine/glycerol +4，diesel +3，fuel +2，其余 +1。
低温：helium +7，neon +6，nitrogen +5，oxygen +4，argon +3，carbon_dioxide/sulfur_dioxide +2，其余 +1。物品走 y+0 远面孔。

## 3. 明确不接管

- 公共 16 / 长尾形态开闸
- 管缆切片 C
- 把塔 spec 塞进 CONFIGURED_MACHINES
- netherair / enderair / soulsandoil 缺流体顶别的油

## 4. 关闭清单

- [x] 热塔独立 map 1/3/1/9，不再跑 distillery 行
- [x] FLUID_OUT 端口 + 拆分 JSON 9/8/63/1
- [x] 高度自动输出
- [x] GT6 获得格与 basicmachines 贴图/GUI
- [x] 形成后 18102 design 0→1 贴图切换（通用端口 TOWER_SKIN / BACK_HOLE）
- [x] 结构用 live 18101/18102；一颗 18102 按 bind 当 ITEM_FLUID 或 FLUID_OUT
- [x] 能量只从 18101 进；控制器壳体 energy NONE
- [x] 零件 covers / 工具转发
- [x] catalog dummy 17101 折进 live `distillation_tower`
- [x] 低温塔 CU 共用结构 tag
- [x] GameTest / JUnit / later_wave +7 / unique-active 账本
"""


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": "DISTILLATION_TOWER_FROZEN",
        "source_revision": SOURCE_REVISION,
        "hot_recipe_count": 6,
        "cryo_recipe_count": 1,
        "geometry": {"energy": 9, "item_fluid": 8, "fluid_out": 63, "controller": 1},
    }


def readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": "DISTILLATION_TOWER_FROZEN",
        "source_revision": SOURCE_REVISION,
        "generated_by": "tools/gt6_distillation_tower.py",
        "evidence": {
            "hot_recipes": 6,
            "cryo_recipes": 1,
            "skipped": [
                "soulsandoil",
                "BiomassIC2",
                "Oil_Heavy2",
                "Oil_HotCrude",
                "Oil_Light2",
                "netherair",
                "enderair",
            ],
        },
    }


def dummy_craft() -> dict[str, Any]:
    return {
        "ingredients": {
            "M": {"item": "cruciblecraft:multiblock/distillation_tower_part"},
            "P": {"item": "cruciblecraft:stainless_steel/nonuple_fluid_pipe"},
        },
        "pattern": ["PPP", "PMP", "PPP"],
        "result": {"count": 1, "id": LIVE_CONTROLLER_ID},
        "type": "cruciblecraft:shaped_catalyst",
    }


INPLACE_DUMMY_ROW = """    {
      "chinese_name": "Distillation Tower",
      "english_name": "Distillation Tower",
      "family": "multiblock",
      "gt6_class": "MultiTileEntityDistillationTower / Multiblock Machines",
      "kind": "MULTIBLOCK_PART",
      "meta": 17101,
      "registry_path": "multiblock/distillation_tower",
      "runtime_id": "cruciblecraft:multiblock/distillation_tower"
    },
"""


def _rewrite_quoted_dummy(path: Path) -> bool:
    """Remap the exact dummy id. Never a prefix of distillation_tower_part."""
    if not path.is_file():
        return False
    text = path.read_text(encoding="utf-8")
    updated = text.replace(f'"{DUMMY_CONTROLLER_ID}"', f'"{LIVE_CONTROLLER_ID}"')
    updated = updated.replace(
        f'"registry_path": "{DUMMY_CONTROLLER_PATH}"',
        f'"registry_path": "{LIVE_CONTROLLER_PATH}"',
    )
    updated = updated.replace(
        f'"runtime_id": "{DUMMY_CONTROLLER_ID}"',
        f'"runtime_id": "{LIVE_CONTROLLER_ID}"',
    )
    if updated == text:
        return False
    path.write_text(updated, encoding="utf-8", newline="\n")
    return True


def fold_17101_dummy() -> dict[str, Any]:
    """Unregister catalog 17101 and fold its craft onto live distillation_tower."""
    inplace_text = INPLACE_CATALOG.read_text(encoding="utf-8")
    if INPLACE_DUMMY_ROW in inplace_text:
        INPLACE_CATALOG.write_text(
            inplace_text.replace(INPLACE_DUMMY_ROW, "", 1),
            encoding="utf-8",
            newline="\n",
        )
    elif any(
        int(row.get("meta") or -1) == 17101
        for row in census.load_json(INPLACE_CATALOG).get("identities") or []
    ):
        raise ValueError("17101 dummy still in inplace catalog but row text drifted")
    withdrawn = 0
    for path in DUMMY_ASSETS:
        if path.is_file():
            path.unlink()
            withdrawn += 1
    if CRAFT_HOT.is_file():
        CRAFT_HOT.unlink()
    dummy = dummy_craft()
    _dump(DUMMY_CRAFT, dummy)
    acquisition = census.load_json(ACQUISITION_CATALOG)
    recipes = [
        row
        for row in acquisition.get("recipes") or []
        if row.get("path") != DUMMY_CONTROLLER_PATH
    ]
    recipes.append(
        {
            "catalysts": {},
            "domain": "multiblock",
            "ingredients": dummy["ingredients"],
            "path": DUMMY_CONTROLLER_PATH,
            "pattern": dummy["pattern"],
            "result": dummy["result"],
            "type": dummy["type"],
        }
    )
    acquisition["recipes"] = recipes
    _dump(ACQUISITION_CATALOG, acquisition)
    remapped = []
    for path in (
        ACQUISITION_CATALOG,
        RECYCLE_0755,
        *SMELTER_CATALOGS,
        MODERN_MAP,
    ):
        if _rewrite_quoted_dummy(path):
            remapped.append(census.relative(path))
    identity_dummy = """    {
      "registry_path": "multiblock/distillation_tower",
      "runtime_id": "cruciblecraft:multiblock/distillation_tower",
      "semantic_key": "smelter_mte:gregtech:gt.multitileentity:17101",
      "source": "src/main/resources/data/cruciblecraft/smelter_mte_identity_catalog.json"
    },
"""
    identity_text = IDENTITY_MANIFEST.read_text(encoding="utf-8")
    if identity_dummy in identity_text:
        IDENTITY_MANIFEST.write_text(
            identity_text.replace(identity_dummy, "", 1),
            encoding="utf-8",
            newline="\n",
        )
        remapped.append(census.relative(IDENTITY_MANIFEST))
    part_recipe = (
        ROOT
        / "src"
        / "generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "recipe"
        / "multiblock"
        / "distillation_tower_part.json"
    )
    part_text = part_recipe.read_text(encoding="utf-8")
    if f'"{DUMMY_CONTROLLER_ID}"' in part_text:
        raise ValueError("fold corrupted distillation_tower_part recipe")
    return {"withdrawn": withdrawn, "remapped": remapped}


def copy_part_art() -> dict[str, Any]:
    """18102 design 0/1 plus 18101 heat-transmitter cubes from GT6 parts."""
    copies = (
        ("distillationtowerparts", "distillation_tower_part", ("0", "1")),
        ("heatacceptor", "heat_transmitter", ("0",)),
    )
    imports: list[dict[str, str]] = []
    for gt6_name, cc_name, designs in copies:
        for design in designs:
            source_root = GT6_MULTIBLOCK_PARTS / gt6_name / design
            if not source_root.is_dir():
                raise FileNotFoundError(f"missing GT6 part art {source_root}")
            dest_root = (
                ASSETS / "textures" / "block" / "machine" / cc_name / design
            )
            for src in sorted(source_root.rglob("*.png")):
                rel = src.relative_to(source_root).as_posix()
                dest = dest_root / rel
                dest.parent.mkdir(parents=True, exist_ok=True)
                if dest.exists():
                    dest.chmod(stat.S_IWRITE)
                shutil.copyfile(src, dest)
                if dest.read_bytes() != src.read_bytes():
                    raise ValueError(f"part art copy drifted: {cc_name}/{design}/{rel}")
                imports.append(
                    {
                        "destination": census.relative(dest).removeprefix(
                            "src/main/resources/"
                        ),
                        "gt6_source": src.relative_to(common.GT6_ART).as_posix(),
                        "source": "gt6_referencable_port_code/gregtech6_w",
                    }
                )
    document = {
        "imports": imports,
        "source": "gt6_referencable_port_code/gregtech6_w",
        "source_present": True,
        "source_revision": SOURCE_REVISION,
    }
    _dump(MANIFEST_PARTS, document)
    return document


def _pause_small_ores() -> None:
    capability = census.load_json(SMALL_ORES_CAPABILITY)
    if capability.get("workflow") == "active":
        capability["workflow"] = "paused"
        owned = []
        for path in capability.get("owned_paths") or []:
            owned.append(
                path.replace(
                    "docs/history/card-plans/active/GT6小矿世界生成详细计划.md",
                    "docs/history/card-plans/prep/GT6小矿世界生成详细计划.md",
                )
            )
        capability["owned_paths"] = owned
        _dump(SMALL_ORES_CAPABILITY, capability)


def _park_cooler_plan() -> None:
    if COOLER_ACTIVE.is_file() and not COOLER_PREP.is_file():
        text = COOLER_ACTIVE.read_text(encoding="utf-8")
        text = text.replace(
            "> unique_active_wave      = runtime/cooler\n> 状态：unique-active / runtime_ready",
            "> unique_active_wave      = null\n> 状态：prep（不占落地锁） / frozen",
        )
        text = text.replace(
            "unique_active_wave           = runtime/cooler",
            "unique_active_wave           = null",
        )
        COOLER_PREP.write_text(text, encoding="utf-8")
        COOLER_ACTIVE.unlink()


def _pause_cooler() -> None:
    capability = census.load_json(COOLER_CAPABILITY)
    if capability.get("workflow") == "active":
        capability["workflow"] = "paused"
        owned = []
        for path in capability.get("owned_paths") or []:
            owned.append(
                path.replace(
                    "docs/history/card-plans/active/冷却器详细计划.md",
                    "docs/history/card-plans/prep/冷却器详细计划.md",
                )
            )
        capability["owned_paths"] = owned
        note = str(capability.get("note") or "")
        capability["note"] = note.replace(
            "unique_active_wave is runtime/cooler while workflow=active.",
            "Paused so machines/distillation-tower can occupy unique-active.",
        )
        _dump(COOLER_CAPABILITY, capability)
    _park_cooler_plan()
    for path in (COOLER_TOPOLOGY, COOLER_READINESS):
        document = census.load_json(path)
        if document.get("unique_active_wave") == "runtime/cooler":
            document["unique_active_wave"] = None
            _dump(path, document)


def prepare_close() -> dict[str, Any]:
    document = census.load_json(CAPABILITY)
    if document.get("workflow") == "accepted":
        raise ValueError(f"{SLUG} already closed")
    document["owned_paths"] = _owned_paths(closed=True)
    document["note"] = capability_document(closed=True)["note"]
    _dump(CAPABILITY, document)
    PLAN_ACTIVE.write_text(plan_markdown(closed=True), encoding="utf-8")
    _dump(WAVE / "topology.json", topology(False))
    _dump(WAVE / "readiness.json", readiness(False))
    return {"owned_paths": document["owned_paths"]}


def write() -> dict[str, Any]:
    closed = (
        CAPABILITY.is_file()
        and census.load_json(CAPABILITY).get("workflow") == "accepted"
    )
    if not closed:
        _pause_small_ores()
        _pause_cooler()
    _dump(STRUCTURE, structure_document())
    for name, recipe in hot_recipes().items():
        _dump(RECIPE_HOT / name, recipe)
    for name, recipe in cryo_recipes().items():
        _dump(RECIPE_CRYO / name, recipe)
    fold_17101_dummy()
    _dump(CRAFT_CRYO, craft_cryo())
    delivery = census.load_json(DELIVERY)
    aliases = dict(delivery.get("texture_aliases") or {})
    aliases.pop("distillation_tower", None)
    delivery["texture_aliases"] = aliases
    _dump(DELIVERY, delivery)
    common.copy_basicmachine_art(
        "distillationtower",
        "distillation_tower",
        gui_source="distillationtower.png",
        gui_dest_stems=("distillationtower",),
        manifest_name="gt6_distillation_tower_art_manifest.json",
    )
    common.copy_basicmachine_art(
        "cryodistillationtower",
        "cryo_distillation_tower",
        gui_source="cryodistillationtower.png",
        gui_dest_stems=("cryodistillationtower",),
        manifest_name="gt6_cryo_distillation_tower_art_manifest.json",
    )
    copy_part_art()
    _dump(CAPABILITY, capability_document(closed=closed))
    plan_path = PLAN_CLOSED if closed else PLAN_ACTIVE
    plan_path.parent.mkdir(parents=True, exist_ok=True)
    plan_path.write_text(plan_markdown(closed=closed), encoding="utf-8")
    _dump(WAVE / "topology.json", topology(not closed))
    _dump(WAVE / "readiness.json", readiness(not closed))
    return {"slug": SLUG, "hot": 6, "cryo": 1, "cells": 81, "closed": closed}


def check() -> list[str]:
    errors: list[str] = []
    capability = census.load_json(CAPABILITY)
    if capability["slug"] != SLUG:
        errors.append("capability slug drifted")
    if capability["required_test_ids"] != EXPECTED_TESTS:
        errors.append("required_test_ids drifted")
    structure = census.load_json(STRUCTURE)
    kinds = {"E": 0, "P": 0, "F": 0, "C": 0}
    for element in structure["structure"]:
        kinds[element["predicate"]] = kinds.get(element["predicate"], 0) + 1
    if kinds != {"E": 9, "P": 8, "F": 63, "C": 1}:
        errors.append(f"structure counts drifted: {kinds}")
    palette = structure["palette"]
    if palette["C"].get("tag") != "cruciblecraft:distillation_tower_controllers":
        errors.append("controller tag missing")
    if palette["E"].get("block") != "cruciblecraft:multiblock/heat_transmitter":
        errors.append("18101 heat transmitter palette missing")
    if palette["P"].get("block") != "cruciblecraft:multiblock/distillation_tower_part":
        errors.append("18102 item_fluid palette missing")
    if palette["F"].get("block") != "cruciblecraft:multiblock/distillation_tower_part":
        errors.append("18102 fluid_out palette missing")
    if palette["F"].get("port") != "fluid_out":
        errors.append("fluid_out palette missing")
    identity_keys = {
        row["semantic_key"] for row in capability.get("identity_disposition") or []
    }
    for key in (
            "machine:distillation_tower:17101",
            "mte:heat_transmitter:18101",
            "mte:distillation_tower_part:18102",
    ):
        if key not in identity_keys:
            errors.append(f"missing identity {key}")
    if "mte:distillation_tower:catalog-dummy" in identity_keys:
        errors.append("catalog dummy identity still listed")
    if not (RECIPE_HOT / "biomass.json").is_file():
        errors.append("missing biomass tower recipe")
    if not (RECIPE_CRYO / "air.json").is_file():
        errors.append("missing cryo air recipe")
    if CRAFT_HOT.is_file():
        errors.append("vanilla machines/ distillation_tower craft still duplicates EMI")
    if not DUMMY_CRAFT.is_file():
        errors.append("folded 17101 craft missing")
    else:
        craft = census.load_json(DUMMY_CRAFT)
        if craft["result"]["id"] != LIVE_CONTROLLER_ID:
            errors.append("folded 17101 craft result drifted")
        if craft.get("type") != "cruciblecraft:shaped_catalyst":
            errors.append("folded 17101 craft type drifted")
        if "invar_distillery" in json.dumps(craft):
            errors.append("tower craft still uses invar distillery stand-in")
    inplace_metas = {
        int(row["meta"])
        for row in census.load_json(INPLACE_CATALOG).get("identities") or []
    }
    if 17101 in inplace_metas:
        errors.append("17101 dummy still in inplace catalog")
    if 18102 not in inplace_metas:
        errors.append("18102 tower part missing from inplace catalog")
    for path in DUMMY_ASSETS:
        if path.is_file():
            errors.append(f"dummy identity file still present: {census.relative(path)}")
    acquisition = census.load_json(ACQUISITION_CATALOG)
    dummy_rows = [
        row
        for row in acquisition.get("recipes") or []
        if row.get("path") == DUMMY_CONTROLLER_PATH
    ]
    if len(dummy_rows) != 1:
        errors.append("acquisition dummy-path recipe missing")
    elif dummy_rows[0].get("result", {}).get("id") != LIVE_CONTROLLER_ID:
        errors.append("acquisition 17101 result still dummy")
    recycle = census.load_json(RECYCLE_0755)
    if DUMMY_CONTROLLER_ID in json.dumps(recycle):
        errors.append("smelter 0755 still consumes dummy 17101")
    if LIVE_CONTROLLER_ID not in json.dumps(recycle):
        errors.append("smelter 0755 did not fold onto live 17101")
    for path in SMELTER_CATALOGS:
        by_meta = {
            int(row["meta"]): row
            for row in census.load_json(path).get("identities") or []
        }
        identity = by_meta.get(17101) or {}
        if identity.get("registry_path") != LIVE_CONTROLLER_PATH:
            errors.append(f"{census.relative(path)} 17101 not folded")
        if identity.get("runtime_id") != LIVE_CONTROLLER_ID:
            errors.append(f"{census.relative(path)} 17101 runtime still dummy")
    part_recipe = (
        ROOT
        / "src"
        / "generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "recipe"
        / "multiblock"
        / "distillation_tower_part.json"
    )
    if f'"{DUMMY_CONTROLLER_ID}"' in part_recipe.read_text(encoding="utf-8"):
        errors.append("distillation_tower_part recipe corrupted by dummy fold")
    if f'"{DUMMY_CONTROLLER_ID}"' in IDENTITY_MANIFEST.read_text(encoding="utf-8"):
        errors.append("identity manifest still lists dummy 17101")
    delivery = census.load_json(DELIVERY)
    if "distillation_tower" in (delivery.get("texture_aliases") or {}):
        errors.append("distillation_tower still aliases distillery art")
    if not MANIFEST_HOT.is_file() or not MANIFEST_CRYO.is_file():
        errors.append("art manifests missing")
    if not MANIFEST_PARTS.is_file():
        errors.append("part art manifest missing")
    else:
        errors.extend(
            common.check_art_manifest(
                "gt6_distillation_tower_part_art_manifest.json",
                min_art_imports=18,
            )
        )
    compiled_slug = None
    try:
        from tools import capability_ledger as ledger

        compiled_slug = ledger.compile_ledger().get("unique_active_slug")
    except Exception as exc:  # pragma: no cover - ledger compile surfaces separately
        errors.append(f"ledger compile failed: {exc}")
    closed = capability["workflow"] == "accepted"
    if closed:
        if capability.get("maturity") != "runtime_ready":
            errors.append("closed maturity drifted")
        if PLAN_ACTIVE.is_file():
            errors.append("active plan still present after close")
        if not PLAN_CLOSED.is_file():
            errors.append("closed plan missing")
        if census.load_json(WAVE / "topology.json").get("unique_active_wave") is not None:
            errors.append("topology still unique-active")
        if census.load_json(WAVE / "readiness.json").get("unique_active_wave") is not None:
            errors.append("readiness still unique-active")
        if compiled_slug == SLUG:
            errors.append("closed card still unique-active")
        if "docs/history/card-plans/closed/" + PLAN_STEM not in (
            capability.get("owned_paths") or []
        ):
            errors.append("owned_paths still point at active plan")
    elif capability["workflow"] == "active" and compiled_slug not in {SLUG, None}:
        if compiled_slug != SLUG:
            errors.append(f"unique-active occupied by {compiled_slug}")
    return errors


def main() -> int:
    parser_write = len(sys.argv) > 1 and sys.argv[1] in {"--write", "write"}
    parser_check = len(sys.argv) > 1 and sys.argv[1] in {"--check", "check"}
    parser_prepare = len(sys.argv) > 1 and sys.argv[1] in {
        "--prepare-close",
        "prepare-close",
    }
    if parser_write:
        print(json.dumps(write(), indent=2))
        return 0
    if parser_prepare:
        print(json.dumps(prepare_close(), indent=2))
        return 0
    errors = check()
    if errors:
        print("\n".join(errors))
        return 1
    if parser_check:
        print("ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
