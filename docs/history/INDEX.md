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
| `tools/full_verification_report.json` | 同路径，只读收据 | 历史 READY |
| numbered builders / currentness / sessions / DAG / seals | [`tools/legacy_verification_index.json`](../../tools/legacy_verification_index.json) | 原字节只读；不参与 active verification |

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

## 阶段档案

| 文档 | 类型 | 状态 |
| --- | --- | --- |
| [T7–T9](stage-archives/CrucibleCraft-阶段档案-T7-T9.md) | 阶段档案 | 关闭 |
| [T10–T12](stage-archives/CrucibleCraft-阶段档案-T10-T12.md) | 阶段档案 | 关闭 |
| [T13–T16](stage-archives/CrucibleCraft-阶段档案-T13-T16.md) | 阶段档案 | 关闭 |
| [T17](stage-archives/CrucibleCraft-阶段档案-T17.md) | 阶段档案 | 关闭 |
| [T18](stage-archives/CrucibleCraft-阶段档案-T18.md) | 阶段档案 | 关闭 |
| [T19](stage-archives/CrucibleCraft-阶段档案-T19.md) | 阶段档案 | 关闭 |
| [T20](stage-archives/CrucibleCraft-阶段档案-T20.md) | 阶段档案 | 关闭 |
| [T21](stage-archives/CrucibleCraft-阶段档案-T21.md) | 阶段档案 | 关闭 |
| [T22](stage-archives/CrucibleCraft-阶段档案-T22.md) | 阶段档案 | 关闭 |
| [T22.5](stage-archives/CrucibleCraft-阶段档案-T22.5.md) | 阶段档案 | 关闭 |
| [T23](stage-archives/CrucibleCraft-阶段档案-T23.md) | 阶段档案 | 关闭 |
| [T24](stage-archives/CrucibleCraft-阶段档案-T24.md) | 阶段档案 | 关闭 |
| [T25](stage-archives/CrucibleCraft-阶段档案-T25.md) | 阶段档案 | 关闭 |
| [T26](stage-archives/CrucibleCraft-阶段档案-T26.md) | 阶段档案 | 关闭 |
| [T28](stage-archives/CrucibleCraft-阶段档案-T28.md) | 阶段档案 | 关闭 |
| [T29](stage-archives/CrucibleCraft-阶段档案-T29.md) | 阶段档案 | 关闭 |
| [T30](stage-archives/CrucibleCraft-阶段档案-T30.md) | 阶段档案 | 关闭 |
| [T31](stage-archives/CrucibleCraft-阶段档案-T31.md) | 阶段档案 | 关闭 |
| [T33](stage-archives/CrucibleCraft-阶段档案-T33.md) | 阶段档案 | 关闭 |
| [T34](stage-archives/CrucibleCraft-阶段档案-T34.md) | 阶段档案 | 关闭（GT6 重开） |
| [T35](stage-archives/CrucibleCraft-阶段档案-T35.md) | 阶段档案 | 关闭（T35R 修复） |
| [T36](stage-archives/CrucibleCraft-阶段档案-T36.md) | 阶段档案 | 关闭（`T36_READY`） |
| [T37](stage-archives/CrucibleCraft-阶段档案-T37.md) | 阶段档案 | 关闭（`T37_READY`） |
| [T38](stage-archives/CrucibleCraft-阶段档案-T38.md) | 阶段档案 | 关闭（`T38_READY`） |
| [T39](stage-archives/CrucibleCraft-阶段档案-T39.md) | 阶段档案 | 关闭（`T39_REPAIR_READY` / `T39_READY`） |
| [T40](stage-archives/CrucibleCraft-阶段档案-T40.md) | 阶段档案 | 关闭（`T40_READY`） |
| [T40-VR](stage-archives/CrucibleCraft-阶段档案-T40-VR.md) | 阶段档案 | 关闭（`T40_VR_READY`） |
| [T41](stage-archives/CrucibleCraft-阶段档案-T41.md) | 阶段档案 | 关闭（`T41_READY`） |
| [T42](stage-archives/CrucibleCraft-阶段档案-T42.md) | 阶段档案 | 关闭（`T42_PARTITION_READY`） |
| [T42-Repair](stage-archives/CrucibleCraft-阶段档案-T42-Repair.md) | 阶段档案 | 关闭（`T42_REPAIR_READY`） |
| [T43](stage-archives/CrucibleCraft-阶段档案-T43.md) | 阶段档案 | 关闭（`T43_READY`） |
| [T36-Repair](stage-archives/CrucibleCraft-阶段档案-T36-Repair.md) | 阶段档案 | 关闭（`T36_REPAIR_READY`） |
| [T44](stage-archives/CrucibleCraft-阶段档案-T44.md) | 阶段档案 | 关闭（`T44_STORAGE_READY`） |
| [T45](stage-archives/CrucibleCraft-阶段档案-T45.md) | 阶段档案 | 关闭（`T45_READY`） |
| [T46](stage-archives/CrucibleCraft-阶段档案-T46.md) | 阶段档案 | 关闭（`T46_READY`） |
| [T47](stage-archives/CrucibleCraft-阶段档案-T47.md) | 阶段档案 | 关闭（`T47_READY`） |
| [T47-VR](stage-archives/CrucibleCraft-阶段档案-T47-VR.md) | 阶段档案 | 关闭（`T47_VR_READY`） |
| [T48](stage-archives/CrucibleCraft-阶段档案-T48.md) | 阶段档案 | 关闭（`T48_READY`） |
| [T49](stage-archives/CrucibleCraft-阶段档案-T49.md) | 阶段档案 | 关闭（`T49_READY`） |
| [Ordinary 尾账收口](stage-archives/CrucibleCraft-阶段档案-Ordinary尾账收口.md) | 阶段档案 | 关闭（`ORDINARY_REMAINDER_CLOSURE_READY`） |
| [回收运行时与 Deferred 账本收口](stage-archives/CrucibleCraft-阶段档案-回收运行时与Deferred账本收口.md) | 阶段档案 | 关闭（`DEFERRED_ORDINARY_RUNTIME_READY`） |
| [1.x 联合退出门](stage-archives/CrucibleCraft-阶段档案-1.x联合退出门.md) | 阶段档案 | 关闭（`ONE_X_JOINT_EXIT_READY`） |
| [源能力对照图](stage-archives/CrucibleCraft-阶段档案-源能力对照图.md) | 阶段档案 | 关闭（`SOURCE_CAPABILITY_MAP_READY`） |
| [通用 Source Pack 导入器](stage-archives/CrucibleCraft-阶段档案-通用Source-Pack导入器.md) | 阶段档案 | 关闭（`GENERIC_RECIPE_IMPORT_READY`） |
| [物流封面网络 R0](stage-archives/CrucibleCraft-阶段档案-物流封面网络R0.md) | 阶段档案 | 关闭（`LOGISTICS_COVER_NET_R0_READY`） |
| [T13c 排除表收回 R0](stage-archives/CrucibleCraft-阶段档案-T13c排除表收回R0.md) | 阶段档案 | 关闭（`T13C_EXCLUSION_RECLAIM_R0_READY`） |
| [非矿世界生成 R0](stage-archives/CrucibleCraft-阶段档案-非矿世界生成R0.md) | 阶段档案 | 关闭（`NON_ORE_WORLDGEN_R0_READY`） |
| [原版替换 R0](stage-archives/CrucibleCraft-阶段档案-原版替换R0.md) | 阶段档案 | 关闭（`VANILLA_REPLACE_R0_READY`） |
| [作物食物蜜蜂 R0](stage-archives/CrucibleCraft-阶段档案-作物食物蜜蜂R0.md) | 阶段档案 | 关闭（`CROPS_FOOD_BEES_R0_READY`） |
| [首小时表现与阶段账本](stage-archives/CrucibleCraft-阶段档案-首小时表现与阶段账本.md) | 阶段档案 | 关闭（`FIRST_HOUR_PRESENTATION_READY`） |
| [原版替换 MVP](stage-archives/CrucibleCraft-阶段档案-原版替换MVP.md) | 阶段档案 | 关闭（`VANILLA_REPLACE_MVP_READY`） |
| [紧凑配方传输编解码](stage-archives/CrucibleCraft-阶段档案-紧凑配方传输编解码.md) | 阶段档案 | 关闭（`COMPACT_RECIPE_WIRE_CODEC_READY`） |
| [紧凑配方作者矩阵](stage-archives/CrucibleCraft-阶段档案-紧凑配方作者矩阵.md) | 阶段档案 | 关闭（`COMPACT_RECIPE_AUTHORED_MATRIX_READY`） |
| [工具头前缀折回](stage-archives/CrucibleCraft-阶段档案-工具头前缀折回.md) | 阶段档案 | 关闭（`TOOL_HEAD_PREFIX_READY`） |
| [物品网络核心](stage-archives/CrucibleCraft-阶段档案-物品网络核心.md) | 阶段档案 | 关闭（`ITEM_NETWORK_CORE_READY`） |

## 工作日志

| 文档 | 类型 | 状态 |
| --- | --- | --- |
| [T24 工作日志](work-logs/T24-工作日志.md) | 工作日志 | 关闭 |
| [T24 开放项](work-logs/T24-开放项.md) | 工作日志 | 关闭 |
| [T25 工作日志](work-logs/T25-工作日志.md) | 工作日志 | 关闭 |
| [T26 工作日志](work-logs/T26-工作日志.md) | 工作日志 | 关闭 |
| [T27 工作日志](work-logs/T27-工作日志.md) | 工作日志 | 关闭 |
| [T28 工作日志](work-logs/T28-工作日志.md) | 工作日志 | 关闭 |
| [T29 工作日志](work-logs/T29-工作日志.md) | 工作日志 | 关闭 |
| [T30 工作日志](work-logs/T30-工作日志.md) | 工作日志 | 关闭 |
| [T31 工作日志](work-logs/T31-工作日志.md) | 工作日志 | 关闭 |
| [T33 工作日志](work-logs/T33-工作日志.md) | 工作日志 | 关闭 |
| [T34 工作日志](work-logs/T34-工作日志.md) | 工作日志 | 关闭（GT6 重开） |
| [T35 工作日志](work-logs/T35-工作日志.md) | 工作日志 | 关闭（T35R 修复） |
| [T36 工作日志](work-logs/T36-工作日志.md) | 工作日志 | 关闭（`T36_READY`） |
| [T37 工作日志](work-logs/T37-工作日志.md) | 工作日志 | 关闭（`T37_READY`） |
| [T38 工作日志](work-logs/T38-工作日志.md) | 工作日志 | 关闭（`T38_READY`） |
| [T39 工作日志](work-logs/T39-工作日志.md) | 工作日志 | 关闭（`T39_READY`） |
| [T40 工作日志](work-logs/T40-工作日志.md) | 工作日志 | 关闭（`T40_READY`） |
| [T40-VR 工作日志](work-logs/T40-VR-工作日志.md) | 工作日志 | 关闭（`T40_VR_READY`） |
| [T41 工作日志](work-logs/T41-工作日志.md) | 工作日志 | 关闭（`T41_READY`） |
| [T42 工作日志](work-logs/T42-工作日志.md) | 工作日志 | 关闭（`T42_PARTITION_READY`） |
| [T42-Repair 工作日志](work-logs/T42-Repair-工作日志.md) | 工作日志 | 关闭（`T42_REPAIR_READY`） |
| [T43 工作日志](work-logs/T43-工作日志.md) | 工作日志 | 关闭（`T43_READY`） |
| [T36-Repair 工作日志](work-logs/T36-Repair-工作日志.md) | 工作日志 | 关闭（`T36_REPAIR_READY`） |
| [T44 工作日志](work-logs/T44-工作日志.md) | 工作日志 | 关闭（`T44_STORAGE_READY`） |
| [T45 工作日志](work-logs/T45-工作日志.md) | 工作日志 | 关闭（`T45_READY`） |
| [T46 工作日志](work-logs/T46-工作日志.md) | 工作日志 | 关闭（`T46_READY`） |
| [T47 工作日志](work-logs/T47-工作日志.md) | 工作日志 | 关闭（`T47_READY`） |
| [T47-VR 工作日志](work-logs/T47-VR-工作日志.md) | 工作日志 | 关闭（`T47_VR_READY`） |
| [T48 工作日志](work-logs/T48-工作日志.md) | 工作日志 | 关闭（`T48_READY`） |
| [T49 工作日志](work-logs/T49-工作日志.md) | 工作日志 | 关闭（`T49_READY`） |
| [Ordinary 尾账收口 工作日志](work-logs/Ordinary尾账收口-工作日志.md) | 工作日志 | 关闭（`ORDINARY_REMAINDER_CLOSURE_READY`） |
| [回收运行时与 Deferred 账本收口 工作日志](work-logs/回收运行时与Deferred账本收口-工作日志.md) | 工作日志 | 关闭（`DEFERRED_ORDINARY_RUNTIME_READY`） |
| [1.x 联合退出门 工作日志](work-logs/1.x联合退出门-工作日志.md) | 工作日志 | 关闭（`ONE_X_JOINT_EXIT_READY`） |
| [源能力对照图 工作日志](work-logs/源能力对照图-工作日志.md) | 工作日志 | 关闭（`SOURCE_CAPABILITY_MAP_READY`） |
| [通用 Source Pack 导入器 工作日志](work-logs/通用Source-Pack导入器-工作日志.md) | 工作日志 | 关闭（`GENERIC_RECIPE_IMPORT_READY`） |
| [物流封面网络 R0 工作日志](work-logs/物流封面网络R0-工作日志.md) | 工作日志 | 关闭（`LOGISTICS_COVER_NET_R0_READY`） |
| [T13c 排除表收回 R0 工作日志](work-logs/T13c排除表收回R0-工作日志.md) | 工作日志 | 关闭（`T13C_EXCLUSION_RECLAIM_R0_READY`） |
| [非矿世界生成 R0 工作日志](work-logs/非矿世界生成R0-工作日志.md) | 工作日志 | 关闭（`NON_ORE_WORLDGEN_R0_READY`） |
| [原版替换 R0 工作日志](work-logs/原版替换R0-工作日志.md) | 工作日志 | 关闭（`VANILLA_REPLACE_R0_READY`） |
| [作物食物蜜蜂 R0 工作日志](work-logs/作物食物蜜蜂R0-工作日志.md) | 工作日志 | 关闭（`CROPS_FOOD_BEES_R0_READY`） |
| [首小时表现与阶段账本 工作日志](work-logs/首小时表现与阶段账本-工作日志.md) | 工作日志 | 关闭（`FIRST_HOUR_PRESENTATION_READY`） |
| [原版替换 MVP 工作日志](work-logs/原版替换MVP-工作日志.md) | 工作日志 | 关闭（`VANILLA_REPLACE_MVP_READY`） |
| [紧凑配方传输编解码 工作日志](work-logs/紧凑配方传输编解码-工作日志.md) | 工作日志 | 关闭（`COMPACT_RECIPE_WIRE_CODEC_READY`） |
| [紧凑配方作者矩阵 工作日志](work-logs/紧凑配方作者矩阵-工作日志.md) | 工作日志 | 关闭（`COMPACT_RECIPE_AUTHORED_MATRIX_READY`） |
| [工具头前缀折回 工作日志](work-logs/工具头前缀折回-工作日志.md) | 工作日志 | 关闭（`TOOL_HEAD_PREFIX_READY`） |
| [物品网络核心 工作日志](work-logs/物品网络核心-工作日志.md) | 工作日志 | 关闭（`ITEM_NETWORK_CORE_READY`） |
| [T31 RC soak](work-logs/T31-RC-soak.md) | 发行实验 | 关闭，非玩家 GA |

## 决策与关闭计划

| 文档 | 类型 | 替代 |
| --- | --- | --- |
| [T10d ADR](../decisions/T10d-容器身份与边界ADR.md) | ADR | 仍有效 |
| [表现层与可玩性分母](../decisions/CrucibleCraft-表现层与可玩性分母.md) | 设计提案 | 现行路线见 roadmap |
| [GT6U 搬运差距](../decisions/CrucibleCraft-GT6U搬运差距分析.md) | 来源分析 | 仍有效 |
| [T9 世界生成计划](closed-plans/T9-世界生成数据化实施计划.md) | 关闭计划 | T20 档案 |
| [4.5 体验精修](closed-plans/CrucibleCraft-4.5-体验精修规划.md) | 关闭计划 | T26 档案 |
| [T33 详细计划](card-plans/closed/T33详细计划.md) | 关闭计划 | T33 档案 |
| [T34 详细计划](card-plans/closed/T34详细计划.md) | 已撤回关闭计划 | T34 重开条目 |
| [T35 详细计划](card-plans/closed/T35详细计划.md)、[T35 修复计划](card-plans/closed/T35修复计划.md) | 关闭计划 | [T35 阶段档案](stage-archives/CrucibleCraft-阶段档案-T35.md) |
| [T36 详细计划](card-plans/closed/T36详细计划.md) | 关闭计划 | [T36 阶段档案](stage-archives/CrucibleCraft-阶段档案-T36.md) |
| [T36-Repair 详细计划](card-plans/closed/T36-Repair详细计划.md) | 关闭内部 gate（`T36_REPAIR_READY`） | [T36-Repair 阶段档案](stage-archives/CrucibleCraft-阶段档案-T36-Repair.md)；不占用 T44 |
| [T37 详细计划](card-plans/closed/T37详细计划.md) | 关闭计划 | [T37 阶段档案](stage-archives/CrucibleCraft-阶段档案-T37.md) |
| [T38 详细计划](card-plans/closed/T38详细计划.md) | 关闭计划 | [T38 阶段档案](stage-archives/CrucibleCraft-阶段档案-T38.md) |
| [T39 详细计划](card-plans/closed/T39详细计划.md)、[T39-Repair 详细计划](card-plans/closed/T39-Repair详细计划.md) | 关闭计划 | [T39 阶段档案](stage-archives/CrucibleCraft-阶段档案-T39.md) |
| [T40 详细计划](card-plans/closed/T40详细计划.md) | 关闭计划 | [T40 阶段档案](stage-archives/CrucibleCraft-阶段档案-T40.md) |
| [T40-VR 详细计划](card-plans/closed/T40-VR详细计划.md) | 关闭计划 | [T40-VR 阶段档案](stage-archives/CrucibleCraft-阶段档案-T40-VR.md) |
| [T41 详细计划](card-plans/closed/T41详细计划.md) | 关闭计划 | [T41 阶段档案](stage-archives/CrucibleCraft-阶段档案-T41.md) |
| [T42 详细计划](card-plans/closed/T42详细计划.md) | 关闭计划 | [T42 阶段档案](stage-archives/CrucibleCraft-阶段档案-T42.md) |
| [T42-Repair 详细计划](card-plans/closed/T42-Repair详细计划.md) | 关闭计划 | [T42-Repair 阶段档案](stage-archives/CrucibleCraft-阶段档案-T42-Repair.md) |
| [T42-Owner 详细计划](card-plans/closed/T42-Owner详细计划.md) | 关闭内部 gate（`T42_OWNER_READY`） | [`tools/t42_owner_readiness.json`](../../tools/t42_owner_readiness.json)；current gap 3,483，`completion_delta=0` |
| [T43 详细计划](card-plans/closed/T43详细计划.md) | 关闭计划 | [T43 阶段档案](stage-archives/CrucibleCraft-阶段档案-T43.md) |
| [T44 详细计划](card-plans/closed/T44详细计划.md) | 关闭计划 | [T44 阶段档案](stage-archives/CrucibleCraft-阶段档案-T44.md) |
| [T45 详细计划](card-plans/closed/T45详细计划.md) | 关闭计划 | [T45 阶段档案](stage-archives/CrucibleCraft-阶段档案-T45.md) |
| [T46 详细计划](card-plans/closed/T46详细计划.md) | 关闭计划 | [T46 阶段档案](stage-archives/CrucibleCraft-阶段档案-T46.md) |
| [T47 详细计划](card-plans/closed/T47详细计划.md) | 关闭计划 | [T47 阶段档案](stage-archives/CrucibleCraft-阶段档案-T47.md) |
| [T47-VR 详细计划](card-plans/closed/T47-VR详细计划.md) | 关闭计划 | [T47-VR 阶段档案](stage-archives/CrucibleCraft-阶段档案-T47-VR.md) |
| [T48 详细计划](card-plans/closed/T48详细计划.md) | 关闭计划 | [T48 阶段档案](stage-archives/CrucibleCraft-阶段档案-T48.md) |
| [T49 详细计划](card-plans/closed/T49详细计划.md) | 关闭计划 | [T49 阶段档案](stage-archives/CrucibleCraft-阶段档案-T49.md) |
| [Smelter / Mixer 收口与语义命名迁移](card-plans/closed/Smelter-Mixer收口与语义命名迁移详细计划.md) | 关闭计划（slug `recipe-portfolio/semantic-closure`） | 配方主体已产出；命名残留见 [semantic-naming.md](../current/semantic-naming.md) |
| [Ordinary 尾账收口与封板修复](card-plans/closed/Ordinary尾账收口与封板修复详细计划.md) | 关闭计划（slug `recipe-portfolio/ordinary-remainder-closure`） | execution gap = 0；后继已签发 `recycling/deferred-ordinary-runtime` |
| [回收运行时与 Deferred 账本收口](card-plans/closed/回收运行时与Deferred账本收口详细计划.md) | 关闭计划（slug `recycling/deferred-ordinary-runtime`） | 1,817 complete + 28 post-1.x；deferred ledger = 0；后继已关闭 `portfolio/one-x-joint-exit` |
| [1.x 联合退出门](card-plans/closed/1.x联合退出门详细计划.md) | 关闭计划（slug `portfolio/one-x-joint-exit`） | 六条 GREEN；load 口径 A；后继已关闭 `portfolio/source-capability-map` |
| [源能力对照图](card-plans/closed/源能力对照图详细计划.md) | 关闭计划（slug `portfolio/source-capability-map`） | 113 行对照；leftover 39；后继已关闭 `portfolio/generic-recipe-generator` |
| [通用 Source Pack 导入器](card-plans/closed/通用Source-Pack导入器详细计划.md) | 关闭计划（slug `portfolio/generic-recipe-generator`） | 声明式导入 READY；四条 combinatorial accounted 且 completed=0；无 production lock |
| [物流封面网络 R0](card-plans/closed/物流封面网络R0详细计划.md) | 关闭计划（slug `portfolio/logistics-cover-net-r0`） | 7-kind 分母冻结；可行性 `requires_new_runtime`；无 core 后继 |
| [T13c 排除表收回 R0](card-plans/closed/T13c排除表收回R0详细计划.md) | 关闭计划（slug `portfolio/t13c-exclusion-reclaim-r0`） | 五类 129/471 分母冻结；逐类 feasibility；无 MTE 实现；无 implementation child |
| [非矿世界生成 R0](card-plans/closed/非矿世界生成R0详细计划.md) | 关闭计划（slug `portfolio/non-ore-worldgen-r0`） | 四类 18/190 分母冻结；逐类 `requires_new_runtime`；无世界生成实现；无 implementation child |
| [原版替换 R0](card-plans/closed/原版替换R0详细计划.md) | 关闭计划（slug `portfolio/vanilla-replace-r0`） | 两类 loader + 三份 T13 blob 分母冻结；逐类 `requires_new_runtime`；无原版配方替换；无 implementation child |
| [作物食物蜜蜂 R0](card-plans/closed/作物食物蜜蜂R0详细计划.md) | 关闭计划（slug `portfolio/crops-food-bees-r0`） | 三类 12/190 dump 特征 + 13,373 recipeCount 规模冻结；逐类 `requires_new_runtime`；无作物 / 食物图 / 蜂箱实现；无 implementation child |
| [首小时表现与阶段账本](card-plans/closed/首小时表现与阶段账本详细计划.md) | 关闭计划（slug `presentation/first-hour-and-stage-ledger`） | 四台 host 脱离 `metal_surface`；smelter 立方机纠正；三态账本；无 production lock；无 implementation child |
| [原版替换 MVP](card-plans/closed/原版替换MVP详细计划.md) | 关闭计划（slug `content/vanilla-replace-mvp`） | 纸 3→1；熔炉 / 骨头 deferred；firestarter 标签；R0 seal 未改；无 production lock；无 implementation child |
| [紧凑配方传输编解码](card-plans/closed/紧凑配方传输编解码详细计划.md) | 关闭计划（slug `runtime/compact-recipe-wire-codec`） | 线上有界字典 StreamCodec，Holder 仍是 1 family；5651 encode ≤ 512 KiB；dedicated 不再撞 NbtAccounter；整包靠 splitter；T48 seal 未改；无 production lock；无 implementation child |
| [紧凑配方作者矩阵](card-plans/closed/紧凑配方作者矩阵详细计划.md) | 关闭计划（slug `runtime/compact-recipe-authored-matrix`） | 作者矩阵 `matrix_v1` + StreamCodec v2 编矩阵；1394/5651 写矩阵，其余 inline；JSON >1 MiB 0；dedicated 进世界无 NbtAccounter / Packet too large；T48 seal 未改；无 production lock；无 implementation child |
| [工具头前缀折回](card-plans/closed/工具头前缀折回详细计划.md) | 关闭计划（slug `registry/tool-head-prefix`） | mapped tool head 折回材料 × 前缀；remap 7990/0；bath 71 / semantic 244；T48 145/34091 未改；B 项留下；无 production lock；无 implementation child |
| [物品网络核心](card-plans/closed/物品网络核心详细计划.md) | 关闭计划（slug `runtime/item-network-core`） | 物品两行封面网 `runtime_ready`；sidecar 3 定义；T19 9/8 仍活锁；排除 Fluid / Generic / Dump 与 `MultiTileEntityLogisticsCore`；无 production lock；无七 kind `player_complete` |
| [交接说明](handoffs/CrucibleCraft-交接说明.md) | 交接 | 以 README 为准 |
