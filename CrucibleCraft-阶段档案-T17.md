# CrucibleCraft 阶段档案 · T17

> 状态：历史档案；T17 已关闭。
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 最终状态：`T17_READY`
> 当前执行入口：《[CrucibleCraft-第三阶段总体规划.md](CrucibleCraft-第三阶段总体规划.md)》T18

## T17 · HU / EU 机器档位批次 ✅

- canonical denominator 为 HU 12 + EU 16 = 28 kind，`unclassified = 0`。
- 本批 selected 3：Distillery、Drying、Smelter；每种只实现 Heat_T 1–3，共 selected 3 × 3 = 9 variants。
- Electrolyzer 1 kind / 3 variants 是 preimplemented EU reference，不是 T17 新增实现。
- deferred 24；Heat tier 4 deferred 10，Electric tier 4–5 deferred 32。T17 没有宣称 28 kind 全实现。
- 九个 HU variant 的 adjacent-bottom firebox 拓扑、三个 Electrolyzer buffered-cable reference、资源、生存获取和三个 tier-1 精确迁移均闭合，`unreachable = 0`。
- `t17_load_projection_input.json` / `t17_load_projection.json` 为 delivery T17、`PASS`；authored / logical / eager / lazy / cache / sync 与全部 runtime interval 增量均为 0。
- RecipeMap stable id 32 个、logical / eager / lazy 18,875 / 16,650 / 2,225、EMI configured map 24 个及完整 recipe enumeration 均与 T16 baseline 完全相等。
- 只新增 9 条 vanilla shaped crafting machine-acquisition recipe；GT recipe row 新增 0，`publication delta = 0`。
- T17a–d complete，pending 为空；builder、双 `runData`、508 Java tests、69 GameTests 与 441 Python tests 由最终统一 verification session 绑定。

下一阶段为 T18 蒸汽、燃油与能量转换。
