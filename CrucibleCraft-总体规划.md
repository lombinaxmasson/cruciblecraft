# CrucibleCraft 总体规划

> 唯一总体规划与项目导航
> 最后更新：2026-08-21
> 当前状态：T0–T31 留档完成；当前不进行玩家发行、RC soak 或 GA。

## 1. 项目目标

CrucibleCraft 是 Minecraft 1.21.1 NeoForge 上的 GT6 风格工业模组。技术目标是让
材料、配方和机器族的常规内容变化优先由数据与规则驱动，而不是按单个对象复制 Java。

本项目不宣称是 GT6、GT6U 或任何其他模组的完整移植。来源事实、派生规则和设计决策
必须分别记录为 `SOURCE_BACKED`、`SOURCE_DERIVED` 或 `DESIGN_POLICY`。

## 2. 阶段状态与证据

- T0–T26 的历史阶段档案在根目录 `CrucibleCraft-阶段档案-T*.md`。
- T27–T31 的封板证据见 [T31 阶段档案](CrucibleCraft-阶段档案-T31.md)、
  [T31 工作日志](T31-工作日志.md) 与机器可读
  [`tools/t31_readiness.json`](tools/t31_readiness.json)。
- `0.1.0-rc.1` 是历史工程候选版本，不是 `1.0.0`、GA 或玩家发行承诺。
- [`tools/full_verification_report.json`](tools/full_verification_report.json) 是历史
  verification snapshot；内容开发不以 `--check-ready` 通过与否作为日常完成判据。

阶段关闭的三个独立轴仍是：

1. **闭包**：分类、注册、资源和运行时路径是否闭合；
2. **保真**：数值与行为是否有固定来源或明确设计依据；
3. **载荷**：注册、配方、索引、内存和同步成本是否在声明预算内。

任一历史阶段显示 READY，只说明其当时约定的分母和判据成立；它不自动证明玩家可玩性、
视觉完成度或完整 GT6 覆盖。

## 3. 2026-08-21 路线修订

本次 `1.0` 的含义是**源码阶段留档**。留档由 canonical Git commit、annotated tag 与
GitHub push 承担，不创建 GitHub Release、不上传 jar、不累计 RC soak 时间。

T31 审计中发现的发行工程缺口（readiness 自证、source replay/SKIP、F005 指标语义、
发行 jar smoke/provenance）统一转交未来的玩家发行卡。它们不阻断本次源码留档，也不能
被写成“已关闭”。

## 4. 当前内容顺序

同一时刻只允许一张内容 T 卡处于 active 状态：

1. **T32 · 地表石子可达性闭环**
   - 验证 `SurfaceRockFeature` 在新生成区块真实放置；
   - 让 surface scatter 进入 worldgen catalog 或等价的可审计声明；
   - 将运行时 `c:rocks` 放置纳入 S0，使 `rock_pack` 在生存可达性图中闭合；
   - 关闭 `CC-4.5-P4`，但不扩展到岩层替换、矿脉指示岩或逐材质平衡。
2. **T33 · 开局对象表现层**
   - 以 T32 生成的 first-hour 集合为范围；
   - 优先 anvil、crucible、firebox，消除未声明的共享占位外观；
   - 使用 `ART_AUTHORED` 或声明完整的 `ART_DERIVED` family；
   - 不做全纹理翻新、不搬运受限资产。
3. **T34 · 首个配方长尾校准族**
   - 在开卡时只选择一个小型、玩家路径可闭合的 family；
   - 同卡进行 relation/load 投影并以硬预算决定范围；
   - 不以全量 ordinary_optional、RecipeMap 数量或注册数替代玩家路径验收。

## 5. 可玩性与表现层

注册数、`v1_work_set`、矿脉 catalog 行数或资源引用链均不能独自证明可玩性。可玩性至少
要求：

- 生存 S0 包含开局必需的 worldgen 放置物、掉落或原版来源；
- S(k) 配方闭包能推进到第一台可运行机器；
- first-hour 对象有可辨的表现层，且不存在 `ART_PLACEHOLDER` /
  `ART_MISSING`；
- 创造栏可见性与生存获得性分别验收。

现有 [表现层与可玩性分母](CrucibleCraft-表现层与可玩性分母.md) 是此方向的设计提案；
其中的 P0/P1 命名不等于当前 T32/T33 卡号。

## 6. 机器可读契约

- Phase 4 历史产品范围：[`tools/phase4_v1_planning_contract.json`](tools/phase4_v1_planning_contract.json)
- Phase 5 portfolio：[`tools/phase5_portfolio_contract.json`](tools/phase5_portfolio_contract.json)
- T22.5 历史 beta wording：[`tools/t22_5_readiness_policy.json`](tools/t22_5_readiness_policy.json)
- 已知问题：[`tools/t26_known_issues.json`](tools/t26_known_issues.json)
- Builder 与验证策略：[`tools/verification_builder_policy.json`](tools/verification_builder_policy.json)

这些 JSON 是历史 builder 的消费对象。修改其路径、字段或计数时，必须同时更新拥有它的
builder 和测试；不要为让旧 READY 快照变绿而手工改 hash。

以下历史 Beta wording 由 T22.5 builder 消费，保留在此作为归档契约：

> T21 的普通化学与 T22 的石油化工中，`v1_required` 全部发布；
> `ordinary_optional` 属于 1.0 后 portfolio，逐类登记 owner 与 replacement condition。

历史 T16/T17 兼容摘要：`T16_READY`，selected 5，deferred 13，publication delta = 0；
`T17_READY`，selected 3，deferred 24，publication delta = 0。这些字符串仅保留给历史
readiness policy 的可读证据，不重新定义卡的验收范围。
T19 is now `T19_READY` 是同一时期的历史闭包记录。

## 7. 日常开发与留档纪律

- 日常改动运行受影响的 Gradle/Python 测试和对应 `build_* --check`；
- 修改 datagen 时必须连续双跑并比较生成树；
- 只有内容里程碑或未来玩家发行卡才运行完整 `--record`；
- `4.5Fix/`、本地参考 dump、`build/`、`run*/` 与 `src/src/` 重复树不是 canonical
  主树，不得纳入主分支提交；
- 玩家发行重新开启时，必须显式消费 deferred release gates，并重新定义 soak、
  provenance、独立审计和发行 jar smoke 的验收条件。
