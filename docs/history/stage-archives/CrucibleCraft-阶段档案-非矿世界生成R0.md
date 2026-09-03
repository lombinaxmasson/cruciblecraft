# CrucibleCraft 阶段档案 · 非矿世界生成 R0

> 状态：`NON_ORE_WORLDGEN_R0_READY`（2026-09-02）
> 计划 slug：`portfolio/non-ore-worldgen-r0`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`T13C_EXCLUSION_RECLAIM_R0_READY`；核能 Track C `started = false`
> Closing：四类非矿 worldgen dump 特征 18/190 byte-identical 继承；Trees /
> Dungeons / Planets / Center 可行性均为 `requires_new_runtime`；不实现
> 世界生成；不预分配 implementation child；`nuclear_started = false`；
> `unique_active_wave = null`；`next_unassigned = true`；
> `production_lock = null`

## 关闭结果

- 零 family 机制卡。`owns_families = 0`。`completion_delta = 0`。
  `generated_recipe_count = 0`。未发配方，未改 Java，未改
  `worldgen_catalog/**`，未跑 GameTest。
- 四类从 `gt6_dump/gt6_recipe_dump/worldgen/other_features.json` 逐项继承：
  Trees 9（`WorldgenTree*`）、Dungeons 1（`WorldgenDungeonGT`）、
  Planets 3（`moon.rocks` / `mars.rocks` / `planet.rocks`）、Center 5
  （`center.*`）。未把 `WorldgenStreets` 算进 Trees，也未吸入 Aether /
  Erebus / Alfheim rocks。全表 190 未改；remainder 172 本卡不拥有。
  capability map 四行未改。
- 现有 T20 129 矿脉、2 流体矿、地表石子与已关闭 T33 / T45 / T48 item
  scatter 都不覆盖这四类。缺口是机制，不是漏登记的 catalog 行。Trees
  的木材 identity 不是树放置。Planets 不得写入现行 overworld catalog。
- `feasibility.json` 逐类均为 `requires_new_runtime`。不预分配
  per-category implementation child / 其余 172 dump 特征 / vanilla
  replace / crops / food / bees / nuclear /
  `count-ceiling-kind-envelope`。
- T20 policy / sealed growth-order JSON 未改写。18/190 不得写成 census
  complete，也不得写成 GT6 世界生成完成。

```text
NON_ORE_WORLDGEN_R0_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成 GT6 非矿 worldgen
完成。

## 权威 artifacts

- `tools/waves/portfolio/non-ore-worldgen-r0/`
  （`inherited_denominator.json`、`source_semantics.json`、
  `existing_mechanism.json`、`worldgen_contract.json`、
  `feasibility.json`、`readiness.json`、`closeout_seal.json`）
- `python tools/build_non_ore_worldgen_r0.py --check`

`failed_gates=[]`。不重写已 sealed 的 source-capability-map growth-order。
