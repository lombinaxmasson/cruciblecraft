# CrucibleCraft 阶段档案 · 作物食物蜜蜂 R0

> 状态：`CROPS_FOOD_BEES_R0_READY`（2026-09-02）
> 计划 slug：`portfolio/crops-food-bees-r0`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`VANILLA_REPLACE_R0_READY`；核能 Track C `started = false`
> Closing：三类 crops / food / bees 与 12/190 dump 特征、13,373 dump
> recipeCount 规模上下文 byte-identical 继承；Crops / Food / Bees
> 可行性均为 `requires_new_runtime`；不实现作物、食物图或蜂箱；
> 不预分配 implementation child；`nuclear_started = false`；
> `unique_active_wave = null`；`next_unassigned = true`；
> `production_lock = null`

## 关闭结果

- 零 family 机制卡。`owns_families = 0`。`completion_delta = 0`。
  `generated_recipe_count = 0`。未发配方，未改 Java，未改
  `worldgen_catalog/**`，未跑 GameTest。
- 三类从 capability map 逐行继承：Crops（`plant.glowtus` /
  `plant.bush` + `gt.recipe.squeezer` 5322）、Food（`gt.recipe.juicer`
  96 / `gt.recipe.fermenter` 6435）、Bees（10 个 `WorldgenHives` +
  `gt.recipe.bumblequeen` 80 / `gt.recipe.bumblelyzer` 1440）。
  dump 配方数只作规模上下文，合计 13,373，不是 census。
  `gt.recipe.plantalyzer` recipeCount = 0，不当成分母。capability map
  三行未改。
- 12/190 dump 特征从非矿 worldgen remainder 172 划入本卡；切后 remainder
  160。T20 矿脉 / 流体矿 / 石子 catalog 不是 plant.* 或 hive。
- 现有 `machine_tiers.json` / `ModProcessingMachines` 无 squeezer /
  juicer / fermenter / bumble host。T35 已排除对应 MTE，不是漏登记。
  generic Source Pack 导入器 READY 不创建新 RecipeMap。count-ceiling /
  kind envelope 仍 report-only。缺口是机制，不是漏编的 smelter 配方。
- `feasibility.json` 逐类均为 `requires_new_runtime`。不预分配
  per-category implementation child / 其余 worldgen dump / nuclear /
  `count-ceiling-kind-envelope`。
- sealed growth-order JSON 未改写。三类不得写成 census complete，也不得
  写成 GT6 作物 / 食物 / 蜜蜂完成。

```text
CROPS_FOOD_BEES_R0_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成 GT6 crops /
food / bees 完成。

## 权威 artifacts

- `tools/waves/portfolio/crops-food-bees-r0/`
  （`inherited_denominator.json`、`source_semantics.json`、
  `existing_mechanism.json`、`crops_contract.json`、
  `feasibility.json`、`readiness.json`、`closeout_seal.json`）
- `python tools/build_crops_food_bees_r0.py --check`

`failed_gates=[]`。不重写已 sealed 的 source-capability-map growth-order。
