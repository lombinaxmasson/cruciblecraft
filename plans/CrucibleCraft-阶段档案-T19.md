# CrucibleCraft 阶段档案 · T19

> **工作副本。** 权威关闭档案为仓库根目录
> 《[../CrucibleCraft-阶段档案-T19.md](../CrucibleCraft-阶段档案-T19.md)》。

> 状态：`T19_READY`
> 关闭日期：2026-08-07
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`

## 关闭摘要

T19a–d 全部完成且无 pending。T13 的 47 个 canonical cover kind 保持完整分类：

- implemented 4；
- selected T19 5；
- deferred with reason 28；
- out of scope 10；
- unclassified 0。

selected 的 conveyor、retriever item、robot arm、pressure valve 与 manual selector 都具有注册物品、vanilla crafting、双语资源、受界配置和生产 GameTest。第二个 conveyor 定义只改 JSON 值，Java behavior plugin 增量为 0。

## 获取与 publication

Cover 获取 5 条，非金属管获取 25 条，共新增 30 条 `minecraft:crafting_shaped`。其中 Wood 五档来自固定 GT6 source，carbon / plastic / rubber / treated wood 共 20 条明确标为 CrucibleCraft `DESIGN_POLICY_NON_GT6`；GT6 的 `recipe=false` 未改，O-27 与 O-28 关闭。

T19 load schema 2 将这 30 条记为 vanilla datapack entries，不计为 GT logical rows。publication 保持 32 RecipeMap、18,875 logical / 16,650 eager / 2,225 lazy、EMI 24，GT row delta 为 0。generated recipe 总数由 T19 前实际集合 877 加精确 T19 集合 30 派生为 907。

O-28 只把既有 5 条 fluid 与 3 条 item T8 规则原地改为逐 gauge 的 output/specification predicate，没有新增规则。T8 logical expansion 在改动前后均为 257，因此 MaterialRule row delta 与 GT row delta 都是 0。

`machine_tiers.json` 中的 `ru_tier_*`、`ku_tier_*`、`eu_tier_*`、`hu_tier_*` 是稳定的共享 tier-band identity；`tierBand` 字段名说明其语义，不触发这些 id 的重命名或存档 churn。

## 运行时与性能边界

- 所有 pipe/cover 工作共享 position-phased 5-tick 调度；500-pipe fixture 每 tick 恰好 100 个到期。
- item route discovery 最多访问 32,768 根已加载管，单 pipe route cache 最多 256 项。
- 每根管最多 6 个 cover，summary 最多 768 字符。
- cover configuration payload 的编码硬上限为 13 bytes；fluid client sync 最多每 5 tick 一次。
- item 执行保持 `0 <= delivered <= consumed`，确认提交时 consumed 等于 delivered；retriever、robot arm 与 pressure valve 的 blocked retention/backpressure 均由生产 GameTest 证明。

O-20 由 bounded per-tick `progress_permille` 关闭，精确 block-entity progress 和长工时语义不变；Crusher 与 Coke Oven 保留既有充分的 per-tick `ContainerData`。

## 验证与交接

`tools/t19_readiness.json` 是当前派生门禁，状态 `T19_READY`。verification builder policy、Python affected selection 与 full report 都包含 T19 denominator、acquisition、load、publication、performance 和 currentness。

预发布 polish closure 已绑定 CoverDefinition record/JUnit、registry、configuration payload/JUnit、language provider/JUnit 与双语生成资源，并清除未发布的存档迁移、并行 Anvil/Crusher recipe API 和无调用兼容入口。MaterialRule 的 Anvil/Crusher target 及其 GTRecipeMap/validator/runtime/EMI 活路径仍在；blank/current identity、quarantine、future version、`INTEGRATED_CLIENT` 与 KINETIC8 audit 均保留。

当前生成资源为 `en_us = 3,167`、真实 `zh_cn = 366`、可见中文缺口 `2,801`（材料名 `1,774`）；NeoForge recipe registry 为 2 个 RecipeType / 2 个 serializer，仅 `gt_recipe` 与 `material_rule`。snapshot final closure 预期并执行 538 JUnit、83 GameTest、501 Python tests；builder、双 `runData` 与三套完整测试证据统一写入 full verification report。T19 仍为 `T19_READY`，第三阶段状态不变。

第三阶段 T13–T19 已完整关闭。T20 后续已独立关闭，T21 进行中；这不改变 T19 的关闭证据或第三阶段状态。
