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
- 九个 HU variant 的 adjacent-bottom firebox 拓扑、三个 Electrolyzer buffered-cable reference、资源、生存获取和 current-only identity/quarantine 均闭合，`unreachable = 0`。
- `t17_load_projection_input.json` / `t17_load_projection.json` 为 delivery T17、`PASS`；authored / logical / eager / lazy / cache / sync 与全部 runtime interval 增量均为 0。
- RecipeMap stable id 32 个、logical / eager / lazy 18,875 / 16,650 / 2,225、EMI configured map 24 个及完整 recipe enumeration 均与 T16 baseline 完全相等。
- 只新增 9 条 vanilla shaped crafting machine-acquisition recipe；GT recipe row 新增 0，`publication delta = 0`。
- builder proof policy 门禁已关闭：同一 raw corpus 只保留一个 canonical full-replay owner；下游 readiness 只消费 compact evidence，完整重放由显式 `source-replay` 负责。
- T17a–d complete，pending 为空；builder、双 `runData`、508 Java tests、69 GameTests 与 441 Python tests 由最终统一 verification session 绑定。

## 2026-08-07 · tier band 身份 currentness

- 九个 HU variant 与三个 EU reference 已改用 `tierBand` API/expected 字段；完整身份仍由 variant id 给出。
- Heat / Electric 同 band 的材料、能量、输入窗口、容量与效率保持一致，parallel limit 继续按 variant 保存；T17 三条迁移 tuple 值不变。
- readiness/currentness 仅刷新命名、schema 与 NBT 兼容证据，不改变 HU/EU 拓扑、RecipeMap publication 或历史关闭计数。

## 2026-08-07 · proof policy 终验

- 随 T18/T19 接入，统一策略当前覆盖 50 个 builder；每项都有 ordinary argv 与 `compact` / `rederived` proof tier，raw 重放不允许绕过策略直接进入普通 closure。
- 50-builder 首轮为 23.248 秒；三次暖运行中位数 22.996 秒，相对旧 161 秒 baseline 下降 85.7%。隐藏全部本地 raw/cache/fetched-source 后，50/50 仍在 27.290 秒内通过。
- T13 compact / hash-fast / full-replay 分别为 0.143 / 0.823 / 51.066 秒；时间目标只告警，哈希、集合、语义与 currentness 漂移继续 hard-fail。

下一阶段为 T18 蒸汽、燃油与能量转换。
