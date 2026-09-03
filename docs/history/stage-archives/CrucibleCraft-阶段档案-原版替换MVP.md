# CrucibleCraft 阶段档案 · 原版替换 MVP

> 状态：`VANILLA_REPLACE_MVP_READY`（2026-09-02）
> 计划 slug：`content/vanilla-replace-mvp`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`FIRST_HOUR_PRESENTATION_READY`；核能 Track C `started = false`
> Closing：第一小时纸配方按 GT6 3 甘蔗 → 1 纸覆盖；熔炉 / 骨头按 §7
> 缩进 `deferred`（无铁前 `OD.craftingFirestarter`、无锤槽 serializer）；
> 登记 `cruciblecraft:crafting_firestarter`；R0 seal 未改写；
> `nuclear_started = false`；`unique_active_wave = null`；
> `next_unassigned = true`；`production_lock = null`

## 关闭结果

- 零 GT family 内容卡。`owns_families = 0`。`completion_delta = 0`。
  `generated_recipe_count = 0`。未签 production lock，未往
  `src/recipe_generated/**` 灌 GT compact。
- lock 权威：
  [`tools/waves/content/vanilla-replace-mvp/vanilla_replace_lock.json`](../../../tools/waves/content/vanilla-replace-mvp/vanilla_replace_lock.json)。
  `substituted` 仅 `minecraft:paper`（Vanilla.java L43–44 + L52）。
  `removed` / `added` 为空。熔炉、骨头、附魔台 / 末影箱 / 鞍 / 岩浆膏、
  Vanilla.java 后半、Replace 扫描器进 `deferred`。
  `RM.rem_smelting` 骨/染料→史莱姆、Witchery 粗骨、干草 / 树皮 Fire
  Starter、GT basalt 熔炉进 `no_1_21_equivalent`。玻璃瓶
  `identical_1_21`。ASM / hopper 121 / 模具烧制 / WorldgenOresVanilla
  `out_of_scope`。
- 熔炉未删：`LoaderItemData` 只把 `flint_and_steel` / `fire_charge`
  登记进 `OD.craftingFirestarter`；GT6 Fire Starter 要
  `OD.itemGrassDry` / `OD.itemBarkDry`（无 1.21 物）；Match 要磷粉 +
  木螺栓（非第一小时）。铁锁熔炉会断节点 1–2（火箱 / 研钵 / 陶瓷
  烧制）。§7 缩小 lock，不编「燧石当 firestarter」表。
- 支撑标签 `cruciblecraft:crafting_firestarter` =
  flint_and_steel / fire_charge / `cruciblecraft:match`。火箱配方仍吃
  **方块** `minecraft:furnace`。
- R0 `--check` 只核 sealed artifact；活树 `data/minecraft/recipe`
  由本卡拥有，不把 R0 `minecraft_recipe_override_count = 0` 改写。
- 账本第 1 节 Vanilla / Replace：**部分由本卡实现**，余量仍 `frozen`。
  不得写成两类 loader `player_complete`。不预分配 Item Network Core /
  物流网 / 核电 child。

```text
VANILLA_REPLACE_MVP_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成 GT6 原版替换
完成。

## 权威 artifacts

- `tools/waves/content/vanilla-replace-mvp/vanilla_replace_lock.json`
- `tools/waves/content/vanilla-replace-mvp/readiness.json`
- `src/main/resources/data/minecraft/recipe/paper.json`
- `src/main/resources/data/cruciblecraft/tags/item/crafting_firestarter.json`
- `python tools/build_vanilla_replace_mvp.py --check`
- `python tools/build_vanilla_replace_r0.py --check`
- `.\gradlew.bat runGameTestServer -PwaveRecipes=vanilla-replace-mvp --no-daemon`

不重写已 sealed 的 vanilla-replace-r0。无 T38 式 GameTest receipt。
