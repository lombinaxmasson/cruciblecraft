# 历史文档索引

这些文件是只读历史。Git 历史仍可追溯原文；工作树路径以本表为准。
机器校验不再消费这些 Markdown。路径映射见 [path-map.json](path-map.json)。

## 当前替代

| 历史入口 | 当前文件 | 状态 |
| --- | --- | --- |
| `CrucibleCraft-总体规划.md` | [docs/current/roadmap.md](../current/roadmap.md) | 现行 |
| （无历史入口；现行缺口总账） | [docs/current/unimplemented-gap.md](../current/unimplemented-gap.md) | 现行；机制卡 READY ≠ 已实现 |
| `docs/CrucibleCraft-玩家指南.md` | [docs/current/player-guide.md](../current/player-guide.md) | 现行 |
| `.plans/` | [card-plans/active](card-plans/active/) | 历史卡计划 |
| `plans/` | [card-plans/closed](card-plans/closed/) | 关闭计划 |
| numbered builders / currentness / sessions / DAG | [`tools/legacy_verification_index.json`](../../tools/legacy_verification_index.json) | 原字节只读；不参与 active verification |

## 当前 active 卡

当前无 human-readable unique active plan。已关闭的
[物品网络核心](card-plans/closed/物品网络核心详细计划.md)
（slug `runtime/item-network-core`）为
`ITEM_NETWORK_CORE_READY`。机器可读
`unique_active_wave = null`；`next_unassigned = true`。
物品两行 `runtime_ready`；排除 Fluid / Generic / Dump 与
`MultiTileEntityLogisticsCore`。账本下一张仍是未签发的 Fluid /
Generic Network。
已关闭的
[工具头前缀折回](card-plans/closed/工具头前缀折回详细计划.md)
（slug `registry/tool-head-prefix`）为
`TOOL_HEAD_PREFIX_READY`。mapped tool head 折回 `材料 × 前缀`；
bath identity `71`，semantic `244`；T48 `145 / 34091` 未改。
已关闭的
[紧凑配方作者矩阵](card-plans/closed/紧凑配方作者矩阵详细计划.md)
（slug `runtime/compact-recipe-authored-matrix`）为
`COMPACT_RECIPE_AUTHORED_MATRIX_READY`。改的是作者写法 + 线上矩阵
正文，不是 Holder 粒度或开放 tag；JSON >1 MiB 从 39 降到 0；T48
seal 未改。
已关闭的
[紧凑配方传输编解码](card-plans/closed/紧凑配方传输编解码详细计划.md)
（slug `runtime/compact-recipe-wire-codec`）为
`COMPACT_RECIPE_WIRE_CODEC_READY`。修的是线上写法，不是 Holder
粒度；dedicated 进世界不再撞 `NbtAccounter`；T48 seal 未改。
已关闭的
[原版替换 MVP](card-plans/closed/原版替换MVP详细计划.md)
（slug `content/vanilla-replace-mvp`）为 `VANILLA_REPLACE_MVP_READY`。
纸 3 甘蔗 → 1 纸；熔炉 / 骨头 deferred；R0 seal 未改写。
已关闭的
[首小时表现与阶段账本](card-plans/closed/首小时表现与阶段账本详细计划.md)
（slug `presentation/first-hour-and-stage-ledger`）为
`FIRST_HOUR_PRESENTATION_READY`。mortar / sifter / bath 迁入 GT6 工具贴图；
`smelter` 改走立方机，不再与坩埚共用小锅。阶段账本三态与后续顺序见
[冻结与未实现账本](../current/unimplemented-gap.md)。
机制卡 `*_READY` 不是内容完成。已关闭的
[作物食物蜜蜂 R0](card-plans/closed/作物食物蜜蜂R0详细计划.md)
（slug `portfolio/crops-food-bees-r0`）为 `CROPS_FOOD_BEES_R0_READY`。
冻结 capability map 点名的三类 crops / food / bees 与现有机制适配面；
三类可行性均为 `requires_new_runtime`；不实现作物、食物图或蜂箱，
不签 production lock，不预分配 implementation child。
已关闭的
[原版替换 R0](card-plans/closed/原版替换R0详细计划.md)
（slug `portfolio/vanilla-replace-r0`）为 `VANILLA_REPLACE_R0_READY`。
冻结 capability map 点名的两类 vanilla replace loader 与现有 datapack /
导入器适配面；两类可行性均为 `requires_new_runtime`；不替换原版配方，
不签 production lock，不预分配 implementation child。
已关闭的
[非矿世界生成 R0](card-plans/closed/非矿世界生成R0详细计划.md)
（slug `portfolio/non-ore-worldgen-r0`）为 `NON_ORE_WORLDGEN_R0_READY`。
冻结 capability map 点名的四类非矿 worldgen dump 特征（18/190）与现有 T20
catalog 适配面；四类可行性均为 `requires_new_runtime`；不实现世界生成，
不签 production lock，不预分配 implementation child。
已关闭的
[T13c 排除表收回 R0](card-plans/closed/T13c排除表收回R0详细计划.md)
（slug `portfolio/t13c-exclusion-reclaim-r0`）为 `T13C_EXCLUSION_RECLAIM_R0_READY`。
冻结五类 T13c exclusion（129 source sites / 471 expanded）的 lineage 与现有机制
适配面；不实现 MTE，不签 production lock，不预分配 implementation child。
已关闭的
[物流封面网络 R0](card-plans/closed/物流封面网络R0详细计划.md)
（slug `portfolio/logistics-cover-net-r0`）为 `LOGISTICS_COVER_NET_R0_READY`。
可行性 `requires_new_runtime`；不预分配 core。
[通用 Source Pack 导入器](card-plans/closed/通用Source-Pack导入器详细计划.md)
（slug `portfolio/generic-recipe-generator`）为 `GENERIC_RECIPE_IMPORT_READY`。
[源能力对照图](card-plans/closed/源能力对照图详细计划.md) 的 growth-order
JSON 保持 sealed，不重写。1.x joint exit 已 `ONE_X_JOINT_EXIT_READY`。
不自动启动核能。不自动签发 `portfolio/count-ceiling-kind-envelope`、
combinatorial。语义命名不占用内容卡，见
[semantic-naming.md](../current/semantic-naming.md)。


## 已删除的非计划历史

工作日志、阶段档案、handoff 与 closed-plans/ 已从工作树删除。关闭计划正文只保留在 [card-plans/closed](card-plans/closed/)。原文仍可从 Git 历史读出。

## 决策与关闭计划

| 文档 | 类型 | 替代 |
| --- | --- | --- |
| [容器身份与边界 ADR](../decisions/容器身份与边界ADR.md) | ADR | 仍有效 |
| [表现层与可玩性分母](../decisions/CrucibleCraft-表现层与可玩性分母.md) | 设计提案 | 现行路线见 roadmap |
| [GT6U 搬运差距](../decisions/CrucibleCraft-GT6U搬运差距分析.md) | 来源分析 | 仍有效 |
| [关闭计划目录](card-plans/closed/) | 关闭计划 | 唯一保留的历史计划树 |
