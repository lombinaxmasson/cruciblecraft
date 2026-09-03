# CrucibleCraft 总体规划

> 唯一总体规划与项目导航
> 最后更新：2026-09-03
> 当前状态：能力交付合同已生效，见
> [capability-delivery-workflow.md](capability-delivery-workflow.md)。
> 进度只计 `player_complete`。现行能力
> `logistics/fluid-network/basic-transfer`（wave
> `runtime/fluid-network-basic-transfer`）。
> [物品网络核心](../history/card-plans/closed/物品网络核心详细计划.md)
> 仍是物品两行 `runtime_ready`，不是七 kind 完成。
> Generic / Dump / `MultiTileEntityLogisticsCore` 仍 `frozen`。
> 人读无 unique active **card**；机器可读 `unique_active_wave = null`。
> [工具头前缀折回](../history/card-plans/closed/工具头前缀折回详细计划.md)
> （slug `registry/tool-head-prefix`）为
> `TOOL_HEAD_PREFIX_READY`。mapped tool head 折回 `材料 × 前缀`；
> bath identity `71`，semantic `244`；T48 `145 / 34091` 未改。
> [紧凑配方作者矩阵](../history/card-plans/closed/紧凑配方作者矩阵详细计划.md)
> （slug `runtime/compact-recipe-authored-matrix`）为
> `COMPACT_RECIPE_AUTHORED_MATRIX_READY`。改 compact family 作者写法
> （矩阵 + 允许元组），展开后仍是 exact；不改 tag 匹配、不拆
> Holder。
> [紧凑配方传输编解码](../history/card-plans/closed/紧凑配方传输编解码详细计划.md)
> （slug `runtime/compact-recipe-wire-codec`）为
> `COMPACT_RECIPE_WIRE_CODEC_READY`。修的是线上写法，不是 Holder
> 粒度；dedicated 进世界不再撞 `NbtAccounter`；整包仍靠 splitter；
> T48 语义分母未改。
> [原版替换 MVP](../history/card-plans/closed/原版替换MVP详细计划.md)
> （slug `content/vanilla-replace-mvp`）为 `VANILLA_REPLACE_MVP_READY`。
> 纸 3 甘蔗 → 1 纸；熔炉 / 骨头 deferred（无铁前 firestarter）。
> 历史 R0 档案未改写。不预分配下一张实现 child。
> [首小时表现与阶段账本](../history/card-plans/closed/首小时表现与阶段账本详细计划.md)
> （slug `presentation/first-hour-and-stage-ledger`）为
> `FIRST_HOUR_PRESENTATION_READY`。四台首小时 host 已脱离 `metal_surface`；
> `smelter` 改走 `basicmachines/smelter` 立方机，不再与坩埚共用小锅。
> 阶段账本三态 / 后续顺序 / 核电体积写在
> [冻结与未实现账本](unimplemented-gap.md)。不预分配下一张实现 child。
> 1.x joint exit、源能力对照图、通用 Source Pack 导入器、
> 物流封面网络 R0、T13c 排除表收回 R0、非矿世界生成 R0、原版替换 R0
> 与作物食物蜜蜂 R0 均已关闭。
> [作物食物蜜蜂 R0](../history/card-plans/closed/作物食物蜜蜂R0详细计划.md)
> （slug `portfolio/crops-food-bees-r0`）为 `CROPS_FOOD_BEES_R0_READY`。
> 三类 crops / food / bees 与 12/190 dump 特征已冻结；三类可行性均为
> `requires_new_runtime`；不实现作物、食物图或蜂箱，不预分配
> implementation child。
> [原版替换 R0](../history/card-plans/closed/原版替换R0详细计划.md)
> （slug `portfolio/vanilla-replace-r0`）为 `VANILLA_REPLACE_R0_READY`。
> 两类 vanilla replace loader 与三份 T13 源文件 blob 已冻结；两类可行性
> 均为 `requires_new_runtime`；不替换原版配方，不预分配 implementation
> child。
> [非矿世界生成 R0](../history/card-plans/closed/非矿世界生成R0详细计划.md)
> （slug `portfolio/non-ore-worldgen-r0`）为 `NON_ORE_WORLDGEN_R0_READY`。
> 四类非矿 worldgen dump 特征（18/190）与现有 T20 catalog 适配面已冻结；
> 四类可行性均为 `requires_new_runtime`；不实现世界生成，不预分配
> implementation child。
> [T13c 排除表收回 R0](../history/card-plans/closed/T13c排除表收回R0详细计划.md)
> （slug `portfolio/t13c-exclusion-reclaim-r0`）为 `T13C_EXCLUSION_RECLAIM_R0_READY`。
> 五类 T13c exclusion（129/471）与现有机制适配面已冻结；不实现 MTE，
> 不预分配 implementation child。
> [物流封面网络 R0](../history/card-plans/closed/物流封面网络R0详细计划.md)
> （slug `portfolio/logistics-cover-net-r0`）为 `LOGISTICS_COVER_NET_R0_READY`。
> 可行性 `requires_new_runtime`；不预分配 core。
> [源能力对照图](../history/card-plans/closed/源能力对照图详细计划.md)
> （slug `portfolio/source-capability-map`）为 `SOURCE_CAPABILITY_MAP_READY`。
> [通用 Source Pack 导入器](../history/card-plans/closed/通用Source-Pack导入器详细计划.md)
> （slug `portfolio/generic-recipe-generator`）为 `GENERIC_RECIPE_IMPORT_READY`。
> 现有 host 接新 Source Pack 不再需要 per-wave builder 或手写 `WaveSpec`；
> production 仍要单独审查的内容卡。没有 production lock，也不发布配方。
> current execution gap = 0；deferred ledger = 0。不自动
> 启动核能。`portfolio/count-ceiling-kind-envelope` 仍是 telemetry /
> report-only，未预分配为后继。机制卡 `*_READY` 不是内容完成；缺口总账
> [冻结与未实现账本](unimplemented-gap.md)。语义命名剩余项见
> [semantic-naming.md](semantic-naming.md)（长期清单，不占用 active child）。不签发新的里程碑编号。不进行
> 玩家发行、RC soak 或 GA。

## 1. 项目目标

CrucibleCraft 是 Minecraft 1.21.1 NeoForge 上的 GT6 风格工业模组。技术目标是让
材料、配方和机器族的常规内容变化优先由数据与规则驱动，而不是按单个对象复制 Java。

本项目不宣称是 GT6、GT6U 或任何其他模组的完整移植。来源事实、派生规则和设计决策
必须分别记录为 `SOURCE_BACKED`、`SOURCE_DERIVED` 或 `DESIGN_POLICY`。

## 2. 阶段状态与证据

- 已关闭阶段的档案、工作日志与编号卡计划在 [docs/history](../history/INDEX.md)。
- `0.1.0-rc.1` 是历史工程候选版本，不是 `1.0.0`、GA 或玩家发行承诺。
- [`tools/full_verification_report.json`](../../tools/full_verification_report.json) 是历史
  verification report；内容开发不以 `--check-ready` 通过与否作为日常完成判据。
- 当前进度只接受当前 revision 上 fresh 执行的 capability profile PASS；
  `capability.json` 不保存可自行刷新的 `evidence=current`。
- 分层验证、机器契约与文档历史区的现行用法见 [验证指南](verification.md)；
  已知验证债务见
  [`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)。
- 现行 bounded recipe wave 规则见 [ordinary recipe wave 流程与规范](recipe-wave-workflow.md)。
- 对照图之后的冻结 / 未实现缺口见 [冻结与未实现账本](unimplemented-gap.md)。机制卡 `*_READY` 不是游戏里已有这些内容。

阶段关闭的三个独立轴仍是：

1. **闭包**：分类、注册、资源和运行时路径是否闭合；
2. **保真**：数值与行为是否有固定来源或明确设计依据；
3. **载荷**：注册、配方、索引、内存和同步成本是否在声明预算内。

任一历史阶段显示 READY，只说明其当时约定的分母和判据成立；它不自动证明玩家可玩性、
视觉完成度或完整 GT6 覆盖。

## 3. 2026-08-21 路线修订

本次 `1.0` 的含义是**源码阶段留档**。留档由 canonical Git commit、annotated tag 与
GitHub push 承担，不创建 GitHub Release、不上传 jar、不累计 RC soak 时间。

历史审计中发现的发行工程缺口（readiness 自证、source replay/SKIP、F005 指标语义、
发行 jar smoke/provenance）统一转交未来的玩家发行卡。它们不阻断本次源码留档，也不能
被写成“已关闭”。

## 4. 当前内容顺序

同一时刻只允许一张内容工作处于 active 状态。编号卡时代已经结束；现行顺序是
semantic wave，不是下一张里程碑编号。机制卡 `*_READY` 不是内容完成；从
[冻结与未实现账本](unimplemented-gap.md) 找缺口，不要从阶段档案倒推。

当前无 human-readable unique active plan。机器可读
`unique_active_wave = null`；`next_unassigned = true`。账本下一张仍是
未签发的 Fluid / Generic Network。已关闭
[物品网络核心](../history/card-plans/closed/物品网络核心详细计划.md)
（slug `runtime/item-network-core`）为
`ITEM_NETWORK_CORE_READY`。物品两行 `runtime_ready`；不包含 Fluid /
Generic / Dump，也不包含 `MultiTileEntityLogisticsCore`。已关闭
[工具头前缀折回](../history/card-plans/closed/工具头前缀折回详细计划.md)
（slug `registry/tool-head-prefix`）为
`TOOL_HEAD_PREFIX_READY`。`owns_families = 0`。已关闭
[紧凑配方作者矩阵](../history/card-plans/closed/紧凑配方作者矩阵详细计划.md)
（slug `runtime/compact-recipe-authored-matrix`）为
`COMPACT_RECIPE_AUTHORED_MATRIX_READY`。`owns_families = 0`。已关闭
[紧凑配方传输编解码](../history/card-plans/closed/紧凑配方传输编解码详细计划.md)
（slug `runtime/compact-recipe-wire-codec`）为
`COMPACT_RECIPE_WIRE_CODEC_READY`。`owns_families = 0`。已关闭
[原版替换 MVP](../history/card-plans/closed/原版替换MVP详细计划.md)
（slug `content/vanilla-replace-mvp`）为 `VANILLA_REPLACE_MVP_READY`。
兑现原版替换 R0 的第一小时纸配方（3 甘蔗 → 1 纸）；熔炉未删。不搬
ASM，不建 Replace 全量扫描器。

已关闭
[首小时表现与阶段账本](../history/card-plans/closed/首小时表现与阶段账本详细计划.md)
（slug `presentation/first-hour-and-stage-ledger`）为
`FIRST_HOUR_PRESENTATION_READY`。mortar / sifter / bath 迁入 GT6 工具贴图
并保持 voxel；`smelter` 纠正为 `machine_cube_2_layer` 方块机，不再错绑
坩埚小锅。工作态 overlay 未接。阶段账本（三态、后续顺序、核电体积）写在
[冻结与未实现账本](unimplemented-gap.md)。物流 1.2 不在该卡。
机器可读 `unique_active_wave = null`。人读无 unique active plan。
不预分配 Fluid / Generic Network child。后续内容顺序从该账本读，
不要从只读 growth-order 档案倒推。

已关闭的
[作物食物蜜蜂 R0](../history/card-plans/closed/作物食物蜜蜂R0详细计划.md)
（slug `portfolio/crops-food-bees-r0`）为 `CROPS_FOOD_BEES_R0_READY`。
它是 `portfolio/large-content-branches` 的第三个切片：冻结 capability map
点名的三类 crops / food / bees 与现有机制适配面，三类可行性均为
`requires_new_runtime`。不实现作物、食物图或蜂箱，不签 production
lock，不发布配方，也不预分配实现 child。

已关闭的
[原版替换 R0](../history/card-plans/closed/原版替换R0详细计划.md)
（slug `portfolio/vanilla-replace-r0`）为 `VANILLA_REPLACE_R0_READY`。
它是 `portfolio/large-content-branches` 的第二个切片：冻结 capability map
点名的两类 vanilla replace loader 与现有 datapack / 导入器适配面，
两类可行性均为 `requires_new_runtime`。不替换原版配方，不签 production
lock，不发布配方，也不预分配实现 child。

已关闭的
[非矿世界生成 R0](../history/card-plans/closed/非矿世界生成R0详细计划.md)
（slug `portfolio/non-ore-worldgen-r0`）为 `NON_ORE_WORLDGEN_R0_READY`。
它是 `portfolio/large-content-branches` 的第一个切片：冻结 capability map
点名的四类非矿 worldgen dump 特征（18 / 190）与现有 T20 catalog 适配面，
四类可行性均为 `requires_new_runtime`。不实现世界生成，不签 production
lock，不发布配方，也不预分配实现 child。

已关闭的
[T13c 排除表收回 R0](../history/card-plans/closed/T13c排除表收回R0详细计划.md)
（slug `portfolio/t13c-exclusion-reclaim-r0`）为 `T13C_EXCLUSION_RECLAIM_R0_READY`。
它是 `portfolio/existing-mechanism-bounded-domains` 的第二个切片：冻结五类
T13c exclusion（129 source sites / 471 expanded）的 lineage 与现有机制
适配面，不实现 MTE，不签 production lock，不发布配方，也不预分配实现
child。

已关闭的
[物流封面网络 R0](../history/card-plans/closed/物流封面网络R0详细计划.md)
（slug `portfolio/logistics-cover-net-r0`）为 `LOGISTICS_COVER_NET_R0_READY`。
可行性 `requires_new_runtime`，不实现网络运行时，也不预分配 core。

已关闭的
[通用 Source Pack 导入器](../history/card-plans/closed/通用Source-Pack导入器详细计划.md)
（slug `portfolio/generic-recipe-generator`）为 `GENERIC_RECIPE_IMPORT_READY`。
它收掉了“新 Source Pack → canonical source / compile spec”之间的 per-host
builder / handwritten `WaveSpec` 胶水；`owns_families = 0`，不重造已经完成的
`recipe_bulk` 编译器，不签 production lock，不打开 `ParameterizedSpec`，不把
四条 combinatorial family 编进 production。后续内容卡仍必须自带计划、
production lock、player path、load、census 与 closeout。

已关闭的
[源能力对照图](../history/card-plans/closed/源能力对照图详细计划.md)
把 GT6 源域对照到现行 CC 机制（或 `none`），并写出增长顺序。已关闭的
[1.x 联合退出门](../history/card-plans/closed/1.x联合退出门详细计划.md)
把六条退出条件收到 GREEN，并留下 22 行 capability-map seed。对照检查在
`source-capability-inventory`（113 行：22 seed + 91 new），不在 1.x seed
里。growth-order 指定的 `next_major = portfolio/generic-recipe-generator`
已被上述导入器消费；仍没有 production lock。历史 growth-order JSON 不重写。

```text
portfolio/generic-recipe-generator-r0
  -> portfolio/generic-recipe-import-core
  -> portfolio/generic-recipe-import-proof
  -> portfolio/generic-recipe-generator
```

当前无 human-readable unique active plan。
[物品网络核心](../history/card-plans/closed/物品网络核心详细计划.md)
已 `ITEM_NETWORK_CORE_READY`。
[工具头前缀折回](../history/card-plans/closed/工具头前缀折回详细计划.md)
已 `TOOL_HEAD_PREFIX_READY`。
[紧凑配方作者矩阵](../history/card-plans/closed/紧凑配方作者矩阵详细计划.md)
已 `COMPACT_RECIPE_AUTHORED_MATRIX_READY`。
[紧凑配方传输编解码](../history/card-plans/closed/紧凑配方传输编解码详细计划.md)
已 `COMPACT_RECIPE_WIRE_CODEC_READY`。
[首小时表现与阶段账本](../history/card-plans/closed/首小时表现与阶段账本详细计划.md)
已 `FIRST_HOUR_PRESENTATION_READY`。机器可读 `unique_active_wave = null`；
`next_unassigned = true`。
`portfolio/count-ceiling-kind-envelope` 重新评估后仍是
telemetry / report-only，不写入 topology 后继。不自动启动核能 census。
已关闭的 `portfolio/logistics-cover-net-r0` 为
`LOGISTICS_COVER_NET_R0_READY`，可行性 `requires_new_runtime`，不预分配
core。已关闭的 `portfolio/t13c-exclusion-reclaim-r0` 为
`T13C_EXCLUSION_RECLAIM_R0_READY`，不预分配 implementation child。已关闭
`portfolio/non-ore-worldgen-r0` 为 `NON_ORE_WORLDGEN_R0_READY`，四类
可行性均为 `requires_new_runtime`，不预分配 implementation child。已关闭
`portfolio/vanilla-replace-r0` 为 `VANILLA_REPLACE_R0_READY`，两类
可行性均为 `requires_new_runtime`，不预分配 implementation child。已关闭
`portfolio/crops-food-bees-r0` 为 `CROPS_FOOD_BEES_R0_READY`，三类
可行性均为 `requires_new_runtime`，不预分配 implementation child。1.x
已认领的关闭工作仍然有效；对照图不把它写成 GT6 全量完成。

剩余语义命名工作见 [semantic-naming.md](semantic-naming.md)，不占用 active child。
历史 compact 波、storage bundle、census 与验证修复的关闭证据只在
[docs/history](../history/INDEX.md)。那些档案可以继续使用当时的卡号文件名；现行
文档不得要求下一张工作使用里程碑编号。

已关闭且仍约束现行账本的事实：

- census foundation 仍是只读的 78,682 rows / 5,718 families；
- compact 生产波从 assembler compact 走到 bath tiny-purified；
- Smelter / Mixer ordinary-closure 已关闭；
- Ordinary 尾账五 host 已关闭，current execution gap = 0；
- deferred recycling = 0，deferred ledger total = 0（1,817 complete + 28
  independent post-1.x scope）；hanging `later:*` = 0；
- opening execution gap 为 1,349，Bath ordinary 为 0；
- forward-v2 与历史 closeout 记录只作档案，不是 active `--check` 绑定面；
- compact-load closing 是 19 组 production mix 同载重测
  （eager 14 / lazy 50,652 / cache 876 / authored 6,269）。

后续 bounded wave 必须在前一张 closing artifact 上重新签发。选择顺序仍是：

1. capability / dependency closure 已闭合（输入获得、供能、输出消费或明确终端用途）；
2. family 结构相似，可复用已经验收的 exact-relation 或参数模板；
3. 已有可运行 host；
4. 在当前 compact-load opening 下可形成有界、可实测的批次。

普通 recipe wave 的 production lock 默认以 **至少 300 complete families** 为目标；
不足 300 的 ready/form/B0 切片先并入能够共享 source-object、acquisition 或 fluid
语义能力的大 cohort。仅在最终尾账、解除关键基础设施阻塞或独立退出门明确要求时可例外，
并且例外必须在 R0 写明。Card 是 ownership/census 单位，可以拥有多个
`(target_map, publication_group)`；每个 group 再按 query 可直接求出的
item/tag/component/fluid/shape keys 自动分成 Shards。compact-load 的 64/128
控制 routed candidate interval，不是整张 Card 的 source-row 总数。

每张卡仍必须同时公布 `family_count` 与 `source_rows`，并额外公布 representation
breakdown、publication group/shard count、overflow 与 worst routed candidate。
若切片跨越同一 family，该 family 在全部 relations 实际生成、进入运行时并验收前只能
记为 `partial`，不得提前从 family gap 扣减。

每张 recipe wave 固定执行：

```text
source freeze
  -> catalog/candidate/production-lock + phase owner
  -> exact / parameterized representation
  -> publication groups + query-addressable shards
  -> generated/runtime equivalence
  -> player path
  -> per-shard / card-only / integrated load measurement
  -> census delta
  -> remaining-gap recompute
  -> next_unassigned
```

模板、provider 或生成器存在不等于 family 完成。只有进入本波冻结范围、实际生成、运行并
通过 closure / fidelity / load 验收的 family 才能扣减 recipe gap。

统一的 Source Pack、work-set freeze、publication group、query-addressable shard、
fresh runtime 验证、integrated load、census/topology 与撤回规则见
[ordinary recipe wave 语义合同](recipe-wave-workflow.md)。后续 card plan 可以增加
更严格门禁，不得弱化该规范。

GT6、未来 GT6U 与 design recipes 使用独立 append-only Source Packs。完全重复只增加
provenance alias；新增 relation 建 extension contribution；override 建 compatibility
overlay。新来源是否扩大 1.x gap 由新 census intake 明确决定，不回写历史 compact source、
stable ids 或 closing gap。未来核心 runtime 变化走当前 compatibility/migration profile，
不逐卡重签历史记录。

Recipe gap 清零后，deferred ordinary ledger 也已关闭或独立 scope。storage 本身
已作为独立 bundle 关闭，不再等待 execution gap 清零。Storage 分母仍是 census 冻结的
28 source sites / 624 expanded registrations；跨分类的 `mass_storage_logistics` 1/1
保持独立计数。

## 5. 可玩性与表现层

注册数、`v1_work_set`、矿脉 catalog 行数或资源引用链均不能独自证明可玩性。可玩性至少
要求：

- 生存 S0 包含开局必需的 worldgen 放置物、掉落或原版来源；
- S(k) 配方闭包能推进到第一台可运行机器；
- first-hour 对象有可辨的表现层，且不存在 `ART_PLACEHOLDER` /
  `ART_MISSING`。mortar / sifter / smelter / bath 已脱离 `metal_surface`
  （`FIRST_HOUR_PRESENTATION_READY`）；T34 的 19 个目标仍为只读历史集合；
  smelter 工作态 overlay 未接；
- 创造栏可见性与生存获得性分别验收。

现有 [表现层与可玩性分母](../decisions/CrucibleCraft-表现层与可玩性分母.md) 是此方向的设计提案；
其中的 P0/P1 命名不等于当前内容卡号。

## 6. 机器可读契约

- Phase 4 历史产品范围：[`tools/phase4_v1_planning_contract.json`](../../tools/phase4_v1_planning_contract.json)
- Phase 5 历史 portfolio：[`tools/phase5_portfolio_contract.json`](../../tools/phase5_portfolio_contract.json)
- 当前机器 tier catalog：[`machine_tiers.json`](../../src/main/resources/data/cruciblecraft/machine_tiers.json)
- 验证分层：[`tools/verification_profiles.json`](../../tools/verification_profiles.json)
- Builder 策略：[`tools/verification_builder_policy.json`](../../tools/verification_builder_policy.json)
- 验证债务：[`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)

编号时代的分母、load policy 与 row classification JSON 留在 `tools/` 与
[docs/history](../history/INDEX.md)，由 legacy index 标记为只读。Active verification
不 import 或重建它们，也不要求为当前代码刷新历史 READY。

## 7. 1.x 阶段退出门与下一阶段

只有以下条件同时成立，当前 1.x portfolio 才允许结束：

- census 对当前分母 current，所有条目均有 disposition 与 owner；
- RU、KU、HU 的已选 source-backed material matrix 与 EU voltage pilot 已固定，
  并由机器 catalog 无损投影；
- current closing artifact 派生的 **current recipe execution gap = 0**；不能用模板存在、
  文件数或 P0/P1 完成替代全量 family 关闭；
- deferred ordinary ledger 中每一项均已关闭，或经独立、明确的 post-1.x scope decision
  处理；`later:*` 不能无限期删除；
- 所有 `in_scope` source-backed storage bundle 全部关闭，Storage 28/624 分母与
  独立的 cross-category owner 均有 current disposition；
- closure / fidelity / load 三轴无 pending blocker，玩家路径、census 与 compact-load
  ledger 全部 current，且各 load 轴不越 hard ceiling。

退出门已经通过。
[源能力对照图](../history/card-plans/closed/源能力对照图详细计划.md)
已 `SOURCE_CAPABILITY_MAP_READY`：GT6 源域对照到现行 CC 机制（或 `none`），
区分内容缺失和能力缺失。growth-order 指定
`next_major = portfolio/generic-recipe-generator`；该建议已被
[通用 Source Pack 导入器](../history/card-plans/closed/通用Source-Pack导入器详细计划.md)
消费并关闭为 `GENERIC_RECIPE_IMPORT_READY`，没有 production lock。核能、
新 kind 信封数据化仍只是图上的行，不是自动开工。作物/食物/蜜蜂已关闭
[作物食物蜜蜂 R0](../history/card-plans/closed/作物食物蜜蜂R0详细计划.md)
为 `CROPS_FOOD_BEES_R0_READY`：三类可行性均为 `requires_new_runtime`，
不实现作物、食物图或蜂箱，不预分配 implementation child。物流封面网已关闭
[物流封面网络 R0](../history/card-plans/closed/物流封面网络R0详细计划.md)
为 `LOGISTICS_COVER_NET_R0_READY`：可行性 `requires_new_runtime`，
仍不实现运行时，也不预分配 core。T13c 排除表收回已关闭
[T13c 排除表收回 R0](../history/card-plans/closed/T13c排除表收回R0详细计划.md)
为 `T13C_EXCLUSION_RECLAIM_R0_READY`：五类 129/471 分母冻结，不实现 MTE，
不预分配 implementation child。已关闭
[非矿世界生成 R0](../history/card-plans/closed/非矿世界生成R0详细计划.md)
为 `NON_ORE_WORLDGEN_R0_READY`：四类 18/190 分母冻结，可行性均为
`requires_new_runtime`，不实现世界生成，不预分配 implementation child。
已关闭
[原版替换 R0](../history/card-plans/closed/原版替换R0详细计划.md)
为 `VANILLA_REPLACE_R0_READY`：两类 loader + 三份 T13 blob 分母冻结，
可行性均为 `requires_new_runtime`，不替换原版配方，不预分配
implementation child。
已有 kind 的显式档位已经走 `machine_tiers.json`。

核能 Track C 保持 `started = false`。裂变、聚变和等离子不作为当前 importer、
机器等级、配方校准、存储卡、T13c R0、非矿 worldgen R0、原版替换 R0、
作物食物蜜蜂 R0 或首小时表现卡或原版替换 MVP 的附带范围。体积与后置原因见
[冻结与未实现账本](unimplemented-gap.md)。机器可读 `unique_active_wave`
= `null`。人读无 unique active plan。已关闭的
[物品网络核心](../history/card-plans/closed/物品网络核心详细计划.md)
为 `ITEM_NETWORK_CORE_READY`。
已关闭的工具头前缀
折回为 `TOOL_HEAD_PREFIX_READY`。已关闭的紧凑配方作者
矩阵为 `COMPACT_RECIPE_AUTHORED_MATRIX_READY`。已关闭的紧凑配方传输
编解码为 `COMPACT_RECIPE_WIRE_CODEC_READY`。

「不全量移植 GT6」是历史产品声明，不是增长禁令。它不能再用来阻止生成器、
catalog 或对照工具。1.x 已关闭的分母也不因此作废。

## 8. 日常开发与留档纪律

- 日常改动运行 `python tools/verify.py dev`；
- 修改 datagen 时必须连续双跑并比较生成树；
- 内容卡闭合运行 `python tools/verify.py integration --profile <name>`；
- 只有未来玩家发行卡才运行 `python tools/verify.py release` 或历史 `--record`；
- `4.5Fix/`、本地参考 dump、`build/`、`run*/` 与 `src/src/` 重复树不是 canonical
  主树，不得纳入主分支提交；
- 玩家发行重新开启时，必须显式消费 deferred release gates，并重新定义 soak、
  provenance、独立审计和发行 jar smoke 的验收条件。
