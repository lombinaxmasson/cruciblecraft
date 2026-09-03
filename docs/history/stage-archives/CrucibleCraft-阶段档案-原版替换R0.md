# CrucibleCraft 阶段档案 · 原版替换 R0

> 状态：`VANILLA_REPLACE_R0_READY`（2026-09-02）
> 计划 slug：`portfolio/vanilla-replace-r0`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`NON_ORE_WORLDGEN_R0_READY`；核能 Track C `started = false`
> Closing：两类 vanilla replace loader 与三份 T13 源文件 blob
> byte-identical 继承；Vanilla / Replace 可行性均为
> `requires_new_runtime`；不替换原版配方；不预分配 implementation child；
> `nuclear_started = false`；`unique_active_wave = null`；
> `next_unassigned = true`；`production_lock = null`

## 关闭结果

- 零 family 机制卡。`owns_families = 0`。`completion_delta = 0`。
  `generated_recipe_count = 0`。未发配方，未改 Java，未写
  `data/minecraft/recipe/**`，未跑 GameTest。
- 两类从 capability map 逐行继承：Vanilla
  （`Loader_Recipes_Vanilla`）、Replace（`Loader_Recipes_Replace` +
  ASM `Replacements`）。三份 T13 tree blob 未改。Replace 保持一行合并
  口径；nested `RecipeReplacement` / `RecipeReplacer` 不是额外
  category。空 dump `mc.recipe.furnace` / `furnacefuel` /
  `autocrafting`（`recipe_count = 0`）不是分母。capability map 两行未改。
- 现有 additive `cruciblecraft` datapack、少数模具 / 坩埚
  `minecraft:smelting`、已关闭 T30 121 hopper vanilla 配方与 generic
  Source Pack 导入器都不覆盖这两类。缺口是机制。无
  `data/minecraft/recipe` override，也无 GT6 ASM transformer。
- `feasibility.json` 逐类均为 `requires_new_runtime`。不预分配
  per-category implementation child / crops / food / bees / 其余
  worldgen dump / nuclear / `count-ceiling-kind-envelope`。
- sealed growth-order JSON 未改写。两类 loader 不得写成 census
  complete，也不得写成 GT6 原版替换完成。

```text
VANILLA_REPLACE_R0_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成 GT6 vanilla
replace 完成。

## 权威 artifacts

- `tools/waves/portfolio/vanilla-replace-r0/`
  （`inherited_denominator.json`、`source_semantics.json`、
  `existing_mechanism.json`、`replace_contract.json`、
  `feasibility.json`、`readiness.json`、`closeout_seal.json`）
- `python tools/build_vanilla_replace_r0.py --check`

`failed_gates=[]`。不重写已 sealed 的 source-capability-map growth-order。
