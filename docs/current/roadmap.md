# CrucibleCraft 总体规划

> 唯一总体规划与项目导航
> 最后更新：2026-08-24
> 当前状态：T32 工程卫生与可验证性重构、T33 地表石子可达性闭环、T34 开局与 T18 青铜
> 能源链表现层、T35 1.x 全域 census 与预算基线、T36 机器注册与等级矩阵重构、
> T37 ordinary_optional Assembler 校准均已完成；
> T38 Roaster 29-family 固定波次是唯一 active 内容卡。
> 不进行玩家发行、RC soak 或 GA。

## 1. 项目目标

CrucibleCraft 是 Minecraft 1.21.1 NeoForge 上的 GT6 风格工业模组。技术目标是让
材料、配方和机器族的常规内容变化优先由数据与规则驱动，而不是按单个对象复制 Java。

本项目不宣称是 GT6、GT6U 或任何其他模组的完整移植。来源事实、派生规则和设计决策
必须分别记录为 `SOURCE_BACKED`、`SOURCE_DERIVED` 或 `DESIGN_POLICY`。

## 2. 阶段状态与证据

- T0–T31 的历史阶段档案在 [docs/history/stage-archives](../history/stage-archives/)。
- T27–T31 的封板证据见 [T31 阶段档案](../history/stage-archives/CrucibleCraft-阶段档案-T31.md)、
  [T31 工作日志](../history/work-logs/T31-工作日志.md) 与机器可读
  [`tools/t31_readiness.json`](../../tools/t31_readiness.json)。
- `0.1.0-rc.1` 是历史工程候选版本，不是 `1.0.0`、GA 或玩家发行承诺。
- [`tools/full_verification_report.json`](../../tools/full_verification_report.json) 是历史
  verification snapshot；内容开发不以 `--check-ready` 通过与否作为日常完成判据。
- T32 已建立 `dev` / `integration` / `release` 分层验证、机器契约与文档历史区；
  当前使用方式见 [验证指南](verification.md)，已知验证债务见
  [`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)。

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

同一时刻只允许一张内容 T 卡处于 active 状态。T32 已完成，当前固定卡序为：

1. **T33 · 地表石子可达性闭环**（已关闭，2026-08-22）
   - 验证 `SurfaceRockFeature` 在新生成区块真实放置；
   - 让 surface scatter 进入 worldgen catalog 或等价的可审计声明；
   - 将运行时 `c:rocks` 放置纳入 S0，使 `rock_pack` 在生存可达性图中闭合；
   - 关闭 `CC-4.5-P4`，但不扩展到岩层替换、矿脉指示岩或逐材质平衡。
   - 闭卡证据见 [T33 阶段档案](../history/stage-archives/CrucibleCraft-阶段档案-T33.md)、
     [T33 工作日志](../history/work-logs/T33-工作日志.md) 与
     [`tools/t33_readiness.json`](../../tools/t33_readiness.json)。
   - 闭卡运行 `python tools/verify.py integration --profile worldgen`。已登记的
     `T32-VD-001` 是 recipes profile 的历史 T5 ledger 债务，不构成 T33 的
     worldgen 门禁通过，也不得被写成 recipes 已通过或债务已豁免。
2. **T34 · 开局与 T18 青铜能源链表现层**（已关闭，`T34_GT6_ART_READY`，2026-08-22）
   - 原 `T34_PRESENTATION_READY` 只覆盖 three-member 自制表现层，未经 GT6 原始资源
     的全量核对；该闭卡已撤回，历史文件保留为可追溯的错误记录，不是当前证据。
   - 已核验的运行时分母为 firebrick、所有模具、砧、坩埚、Brick Burning Box（firebox）、
     Diesel Engine（fuel engine）、Gas Burning Box（burning gas generator）、bronze boiler、
     steam engine、dynamo 与物品模型。`bronze_gas_generator` 经 GT6 class 核对是 Gas
     Burning Box；独立小型燃气发电机以及 CC 未登记的 Solid/Liquid Burning Box 都固定为
     `not_registered`，不得借现有方块身份或贴图声明已实现。
   - 每项以 GT6 loader/renderer、原始 PNG/`.mcmeta` 与模型 JSON 建立 runtime-to-art
     manifest；权威产物为 [`tools/t34_gt6_art_manifest.json`](../../tools/t34_gt6_art_manifest.json)，
     `tmp-baked_textures` 未作为来源证据。
   - 资源身份、方块/物品模型、状态层、tint 与生成资源均经 manifest 验证；两次 `runData`
     后 receipt 仍 current，且 `python tools/verify.py integration --profile presentation`
     已通过。`T32-VD-001` 保持 open，recipes profile 不是本卡通过声明。
3. **T35 · 1.x 全域 census 与预算基线**（已关闭，`T35_CENSUS_READY`，2026-08-22）
   - 以 T13 七表、T13 分类前排除带、CC 实际注册、worldgen、acquisition、
     presentation 与 T22.5 配方行为输入，建立不漏 storage/utility 的当前内容分母；
   - 将内容分为 P0 推进阻塞、P1 高复用工业内容、P2 可选内容与 P3 兼容/巨大组合；
   - 在消费 recipes profile 前处理 `T32-VD-001`：`.gitignore` 不得继续作为 T5
     chemical readiness 的 currentness 输入；只允许在完整 T5 rebuild 与已提交 ledger
     除声明 hash 外完全相等时进行受保护的 hash rebase。重跑严格 recipes integration
     成功后才能关闭该债务；
   - 统一 T14/T31 各 load 轴和 F005 测量语义，正常规划不越 soft budget，
     hard ceiling 不因单个 family 放不下而临时上调；
   - 本卡 `publication_delta = 0`，只生成 T38+ 依赖拓扑，不实现内容；
   - T35R 为全部 8,996 identities 签发 `portfolio_scope`，锁定 33 variants / 11 kinds、
     EU voltage 与 TU/TIME 的 A 结论，storage 28/624 + logistics 1/1 scope，以及 T37
     作为每张 Epoch A generated card 的全局执行门；
   - 验证包括 `census` compact integration、仅一次 machine/recipe full replay 的
     `census-replay` gate、`gradle test` 与隔离 `cruciblecraft_census` GameTest 1/1；
     证据见 [T35 工作日志](../history/work-logs/T35-工作日志.md) 与
     [`tools/t35_readiness.json`](../../tools/t35_readiness.json)。
4. **T36 · 机器注册与等级矩阵重构**（已关闭，`T36_READY`，2026-08-23）
   - 独立冻结 85 行 / 27 kind 目标（opening 33 保留稳定 id）；catalog schema v3
     是唯一投影源；block / item / BE / menu / creative tab / datagen 从 catalog
     投影；
   - 三类分母互斥：RU/KU/HU material 行、EU voltage 行、TU host 行；禁止笛卡尔补全；
   - `roaster` 与 `coagulator` 进入 1.x host 分母，TIME/TU 是独立 energy identity；
   - 权威 overlay：[`tools/t36_machine_target.json`](../../tools/t36_machine_target.json)、
     [`tools/t36_census_delta.json`](../../tools/t36_census_delta.json)、
     [`tools/t36_readiness.json`](../../tools/t36_readiness.json)。历史 T35 文件只读。
5. **T37 · ordinary_optional Assembler 校准与通用 compact family 底座**（已关闭，`T37_READY`，2026-08-24）
   - 固定选择 `cruciblecraft:assembler` 的 `#0002`–`#0051`，恰好 50 families /
     50 source rows；不含 `#0000` / `#0001`；
   - 生产策略由 1x=50 实测派生为 hybrid（14 eager / 36 lazy / cache 8）；
   - 权威 overlay：[`tools/t37_readiness.json`](../../tools/t37_readiness.json)、
     [`tools/t37_census_delta.json`](../../tools/t37_census_delta.json)、
     [`tools/t37_card_topology.json`](../../tools/t37_card_topology.json)；
   - 闭卡证据见 [T37 阶段档案](../history/stage-archives/CrucibleCraft-阶段档案-T37.md)
     与 [T37 工作日志](../history/work-logs/T37-工作日志.md)。
6. **T38 · Roaster 29-family 固定波次**（当前唯一 active）
   - host `cruciblecraft:roaster`；冻结 29 family（`#0000`、`#0001`、`#0003`–`#0029`）；
   - load opening 取 T37 closing（authored 3,616）；不得套用 T37 hybrid 比例；
   - 执行计划见
     [T38 详细计划](../history/card-plans/active/T38详细计划.md)。

### 4.1 T38+ 内容轨（T36 新 topology epoch）

T36 撤销未启动的旧 T38–T46 storage-first 编号，并从 T38 重新连续签发。T36/T37 固定
不变。新 epoch 在 [`tools/t36_card_topology.json`](../../tools/t36_card_topology.json)：

```text
T36 complete
T37 calibration complete
T38 Roaster 29-family wave (unique active)
bounded recipe waves until 1.x recipe gap = 0
storage implementation bundles
1.x exit gate
nuclear source/physics census
```

Storage 不得排在未关闭的 recipe wave 之前。P0–P3 只控制顺序，不再决定 `portfolio_scope`。
同一时刻仍只有一张 active T 卡。

## 5. 可玩性与表现层

注册数、`v1_work_set`、矿脉 catalog 行数或资源引用链均不能独自证明可玩性。可玩性至少
要求：

- 生存 S0 包含开局必需的 worldgen 放置物、掉落或原版来源；
- S(k) 配方闭包能推进到第一台可运行机器；
- first-hour 对象有可辨的表现层，且不存在 `ART_PLACEHOLDER` /
  `ART_MISSING`；
- 创造栏可见性与生存获得性分别验收。

现有 [表现层与可玩性分母](../decisions/CrucibleCraft-表现层与可玩性分母.md) 是此方向的设计提案；
其中的 P0/P1 命名不等于当前 T33/T34 卡号。

## 6. 机器可读契约

- Phase 4 历史产品范围：[`tools/phase4_v1_planning_contract.json`](../../tools/phase4_v1_planning_contract.json)
- Phase 5 历史 portfolio：[`tools/phase5_portfolio_contract.json`](../../tools/phase5_portfolio_contract.json)
- T13 机器分母与排除带：[`tools/t13_denominators/machine_kinds.json`](../../tools/t13_denominators/machine_kinds.json)
- T22.5 配方行分类：[`tools/t22_5_row_classification.json`](../../tools/t22_5_row_classification.json)
- T14 load budget：[`tools/t14_load_budget_policy.json`](../../tools/t14_load_budget_policy.json)
- 当前机器 tier catalog：[`machine_tiers.json`](../../src/main/resources/data/cruciblecraft/machine_tiers.json)
- T22.5 历史 beta wording：[`tools/contracts/narrative_archive.json`](../../tools/contracts/narrative_archive.json)
- 已知问题：[`tools/t26_known_issues.json`](../../tools/t26_known_issues.json)
- 验证分层：[`tools/verification_profiles.json`](../../tools/verification_profiles.json)
- Builder 策略：[`tools/verification_builder_policy.json`](../../tools/verification_builder_policy.json)
- 验证债务：[`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)

这些 JSON 是历史 builder 的消费对象。修改其路径、字段或计数时，必须同时更新拥有它的
builder 和测试；不要为让旧 READY 快照变绿而手工改 hash。

以下历史 Beta wording 由 T22.5 builder 消费，保留在此作为归档契约：

> T21 的普通化学与 T22 的石油化工中，`v1_required` 全部发布；
> `ordinary_optional` 属于 1.0 后 portfolio，逐类登记 owner 与 replacement condition。

历史 T16/T17 兼容摘要：`T16_READY`，selected 5，deferred 13，publication delta = 0；
`T17_READY`，selected 3，deferred 24，publication delta = 0。这些字符串仅保留给历史
readiness policy 的可读证据，不重新定义卡的验收范围。
T19 is now `T19_READY` 是同一时期的历史闭包记录。

## 7. 1.x 阶段退出门与下一阶段

只有以下条件同时成立，当前 1.x portfolio 才允许结束：

- T35 census 对当前分母 current，所有条目均有 disposition 与 owner；
- RU、KU、HU 的已选 source-backed material matrix 已由 T35R 固定；T36 负责无损 catalog
  投影与实际迁移，TU/TIME 不属于 33 行 material matrix；
- EU 的三行 voltage pilot 已由 T35R 固定；T36 负责其 catalog / runtime 语义迁移；
- 所有 `in_scope` source-backed storage family 全部关闭；
- P0/P1 ordinary_optional family 全部关闭，P2/P3 有合法去向；
- closure / fidelity / load 三轴无 pending blocker，且各 load 轴不越 hard ceiling。

退出门通过后才启动下一阶段。下一阶段的默认顺序是：

1. 核能 source/physics census；
2. 裂变燃料、反应堆与废物闭环；
3. 聚变条件、能量收支与产物闭环；
4. 等离子来源、约束与消费端。

核能 Track C 在当前联合退出门前保持 `started = false`。裂变、聚变和等离子不作为当前
机器等级、配方校准或存储卡的附带范围。

## 8. 日常开发与留档纪律

- 日常改动运行 `python tools/verify.py dev`；
- 修改 datagen 时必须连续双跑并比较生成树；
- 内容卡闭合运行 `python tools/verify.py integration --profile <name>`；
- 只有未来玩家发行卡才运行 `python tools/verify.py release` 或历史 `--record`；
- `4.5Fix/`、本地参考 dump、`build/`、`run*/` 与 `src/src/` 重复树不是 canonical
  主树，不得纳入主分支提交；
- 玩家发行重新开启时，必须显式消费 deferred release gates，并重新定义 soak、
  provenance、独立审计和发行 jar smoke 的验收条件。
