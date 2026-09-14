# MTE In-place 获得格收口

> 计划 slug：`content/gt6-mte-inplace-acquisition`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-15）。
> 本文件位于 `card-plans/closed/`。
> 性质：14 个已关闭 in-place runtime capability 的 source-exact 生存获得格落地。
> 关闭目标：`runtime_ready`。不是 `player_complete`。
> 不改 14 张已关闭 runtime 计划正文、R0 `disposition_ledger.json` 或
> `machine_acquisition.json`。
> GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-mte-inplace-acquisition
unique_active_wave           = null
blocker_id                  = obtain/mte-inplace-runtime
separate_per_capability     = true
does_not_merge_counts       = true
auto_promote_player_complete = false
maturity                     = runtime_ready
workflow                     = accepted
close_target                 = runtime_ready
```

权威格是本地
[`Loader_MultiTileEntities.java`](../../../../gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java)。
Java/tick 源仍是 `gt6_code/gregtech6`。每族保持独立 D0 / lock / gap；计数不得相加。
缺形态 / OD / IL / plank / 润滑剂继续 `explicitly_blocked`，禁止 stand-in。

蒸汽涡轮已由 `SteamTurbineCatalog` 发出的格不重复写。共享 blocker
`obtain/mte-inplace-runtime` 已 `resolved`；14 张 runtime 仍是
`runtime_ready`。

---

## 0. 边界

14 个 runtime family 已经是 `maturity=runtime_ready`。获得格合同独立：每族
自己的 D0 matrix、production lock、isolated pack。Live 配方来自同一 GT6 格，
经 `mte_inplace_acquisition.json` 与 `ModRecipeProvider` 发出。

关闭共享 blocker 的条件已经满足：

1. 每个 family 都有独立 D0 结论（`source_exact` 或 `explicitly_blocked`）。
2. 每个 grid/slot 与同一 GT6 对象逐格等价；缺形态继续 blocked。
3. unique-active 落地后的隔离 GameTest（含 `RecipeType.CRAFTING`）、datapack
   load，以及 registry census 探针。EMI 客户端与 `runClient` 留给
   `player_complete`。

关闭 blocker **不**把 14 个 runtime capability 改成 `player_complete`。

禁止：原版箱子顶 `aRegistry.getItem(chest meta)`、`programmed_circuit`、
无关平板 / cover / casing、把蒸汽涡轮现成配方复制成别族 stand-in。

---

## 1. 子任务顺序

共享生成器：`tools/gt6_mte_inplace_acquisition.py`。
解析层：`tools/gt6_resolve.py`。

| 顺序 | domain | 落地 |
| --- | --- | --- |
| 1 | extender | 2/2 live |
| 2 | decorative | 1/24 live（钢绳）；木板 `PlankData` blocked |
| 3 | attachments | 24/33 live；缺 `round` / curved / small_casing 停 |
| 4 | furniture_chest | 100/101 live |
| 5 | furniture_scaffold | 50/50 live |
| 6 | furniture_safe | 100/100 live |
| 7 | furniture_table | 50/50 live |
| 8 | furniture_barrel | 4/12 live；`wood_treated/plate` 与 plank OD 停 |
| 9 | furniture_storage | 200/250 live；缺件 blocked |
| 10 | drive | 27/63 live；缺润滑剂 blocked |
| 11 | converter_remainder | 8/8 source-exact；蒸汽涡轮不重复写；LuV/ZPM 真电路 |
| 12 | crucible_foundry | 85/85 live |
| 13 | misc_tool | 32/39 live |
| 14 | multiblock | 58/74 live；`gt:re-battery1` 与 dense wall 无格停 |

Live 目录 732 条（741 source-exact 减 9 条蒸汽涡轮已有格）。材料门禁挡住
`wood_treated/plate`；电路走 `technological_parts.json`。

---

## 2. 验收

- [x] `python tools/build_gt6_mte_inplace_acquisition.py --check`
- [x] `python tools/build_blockers.py --check`
- [x] `test_gt6_mte_acquisition`
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-mte-inplace-acquisition`：**5/5 PASS**（2026-09-15），含 `sourceExactRecipesAreCraftingType`
- [x] `runData` 写出 732 条 live 配方
- [x] `obtain/mte-inplace-runtime` 已 `resolved`
- [x] 关闭目标 `runtime_ready`（不是 `player_complete`）

Registry census `-PrecipeCensus` 已跑。该门是物品/方块 registry 等式，不是配方计数；当前 fixture 相对 live registry 已有无关漂移（缺 `aluminium/capcellcon` / `nether_star/gem`，并多出大量材料形态），本卡不扩写整份 gate。

---

## 3. 完成门

- source-exact 格已进 live `RecipeManager`（`shaped_catalyst` / `RecipeType.CRAFTING`）。
- 仍缺的形态保持 `explicitly_blocked`。
- `obtain/mte-inplace-runtime` 已 resolved。
- 14 个 runtime capability 仍是 `runtime_ready`。
- 不写 `machine_acquisition.json`，不改 R0。
