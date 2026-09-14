# CrucibleCraft 总体规划

> 唯一总体规划与项目导航。
> 进度只计 `player_complete` 声明，加上当前 revision 的 fresh GameTest
> PASS。`runClient` 只在晋级（`runtime_ready → player_complete`）时强制。
> 合同见 [capability-delivery-workflow.md](capability-delivery-workflow.md)。
> 现行 unique-active、prep 与完成集合只写在
> [project-status.md](project-status.md)，不要在本页手抄。
>
> 关闭档案与机制卡 `*_READY` 见 [docs/history](../history/INDEX.md)。
> `*_READY` 不是游戏里已有这些内容。人读权威缺口页是
> [unimplemented-gap.md](unimplemented-gap.md)，不要从阶段档案或 Prep
> 计划文件倒推剩余工作。

## 1. 项目目标

CrucibleCraft 是 Minecraft 1.21.1 NeoForge 上的 GT6 风格工业模组。技术目标是让
材料、配方和机器族的常规内容变化优先由数据与规则驱动，而不是按单个对象复制 Java。

本项目不宣称是 GT6、GT6U 或任何其他模组的完整移植。来源事实、派生规则和设计决策
必须分别记录为 `SOURCE_BACKED`、`SOURCE_DERIVED` 或 `DESIGN_POLICY`。

「不全量移植 GT6」是历史产品声明，不是增长禁令。它不能再用来阻止生成器、
catalog 或对照工具。1.x 已关闭的分母也不因此作废。

## 2. 阶段状态与证据

- 已关闭阶段的档案、工作日志与编号卡计划在 [docs/history](../history/INDEX.md)。
- `0.1.0-rc.1` 是历史工程候选版本，不是 `1.0.0`、GA 或玩家发行承诺。
- 历史 full verification report 已从工作树删除；内容开发不以旧 `--check-ready`
  通过与否作为日常完成判据。
- 当前进度只接受当前 revision 上 fresh 执行的 capability profile PASS；
  `capability.json` 不保存可自行刷新的 `evidence=current`。
- 分层验证见 [验证指南](verification.md)；已知验证债务见
  [`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)。
- 现行 bounded recipe wave 规则见 [ordinary recipe wave 流程与规范](recipe-wave-workflow.md)。
- 对照图之后的冻结 / 未实现缺口见 [冻结与未实现账本](unimplemented-gap.md)。

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
semantic wave，不是下一张里程碑编号。

现行 unique-active、prep 与 `player_complete` 只写在
[project-status.md](project-status.md)。同一时刻只允许一张内容工作处于
active 状态；prep 不占落地锁，规则见
[能力交付流程 §8](capability-delivery-workflow.md)。
第 1–8 项第一张已关。冷却器仍未签发（[blocked.md](blocked.md) C 桶
`energy/cooler`）。蒸汽涡轮、大型热交换器 17197、聚变 / 量子物质已作为
CC 扩展落地，capability `frozen`+`paused`，不是 `player_complete`，
也不要当下一张 unique-active。读法见
[unimplemented-gap.md](unimplemented-gap.md) 第 0.1 / 第 1 节。
Bath remainder / identity 已由 T48/T49 关完，不是下一张 unique-active。后续排期
读 [blocked.md](blocked.md) 与 `tools/blockers/batches.json`，不要从只读
growth-order 档案倒推。

电能转换已 `player_complete`。聚变 18 源行是 `frozen`+`paused` 的 CC 扩展，
不是缺口第 2 节「尚未签发」。冷却器仍 blocked。Dump 的来源执行器是 Core，
不是 Generic 管网；`dump_policy` 见已关闭的
[物流封面网余量](../history/card-plans/closed/物流封面网余量详细计划.md)。
后继能源卡见已关闭的
[能源后续卡收口](../history/card-plans/closed/能源后续卡收口详细计划.md)
与 [unimplemented-gap.md](unimplemented-gap.md) 第 1–2 节。

已认领的 1.x / R0 / 导入器 / 对照图工作仍然有效；对照图不把它写成 GT6
全量完成。`portfolio/count-ceiling-kind-envelope` 仍是 telemetry /
report-only。语义命名剩余工作见 [semantic-naming.md](semantic-naming.md)
与 [semantic-naming-closeout-checklist.md](semantic-naming-closeout-checklist.md)，
不占用 active child。

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
保持独立计数。T44 关的是注册与单物品容量；GT6 储物桶 `mPartialUnits` 前缀单位换算
（大中小粉、锭/粒等）仍缺，见 [冻结与未实现账本](unimplemented-gap.md) 第 4 节。

## 5. 可玩性与表现层

注册数、`v1_work_set`、矿脉 catalog 行数或资源引用链均不能独自证明可玩性。可玩性至少
要求：

- 生存 S0 包含开局必需的 worldgen 放置物、掉落或原版来源；
- S(k) 配方闭包能推进到第一台可运行机器；
- first-hour 对象有可辨的表现层，且不存在 `ART_PLACEHOLDER` /
  `ART_MISSING`。mortar / sifter / smelter / bath 已脱离 `metal_surface`
  （`FIRST_HOUR_PRESENTATION_READY`）；T34 的 19 个目标仍为只读历史集合；
  smelter 工作态 overlay 未接；
- 创造栏可见性与生存获得性分别验收。生存获得是 GT6 源逐格合成 / 制造，
  禁止用别的物品顶缺失格来假装 `player_complete`（见
  [能力交付流程](capability-delivery-workflow.md)）。

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

## 7. 1.x 阶段退出门

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

退出门已经通过。growth-order 指定的
`next_major = portfolio/generic-recipe-generator` 已被
[通用 Source Pack 导入器](../history/card-plans/closed/通用Source-Pack导入器详细计划.md)
消费，没有 production lock。核能、新 kind 信封数据化仍只是图上的行，不是自动开工。
已有 kind 的显式档位已经走 `machine_tiers.json`。

裂变堆芯 / 棒 / Canner 生存配方已由 `energy/nuclear-fission-survival` 关闭。
热流体与热量合同已由 `energy/nuclear-fission-hot-fluids` 关闭。
聚变 18 源行已由 `energy/fusion-quantum` 落地（`frozen`+`paused`，
`energy/reactor-fusion` 已 `resolved`）。`FUELS_PLASMA` 保持空。
冷却器仍 blocked。体积与后继候选见
[冻结与未实现账本](unimplemented-gap.md)。

## 8. 日常开发与留档纪律

- 日常改动运行 `python tools/verify.py dev`；
- 修改 datagen 时必须连续双跑并比较生成树；
- 内容卡闭合运行 `python tools/verify.py integration --profile <name>`；
- 已接受能力的日常 `player-complete` 只跑隔离 GameTest；
  `python tools/verify.py promotion` 才加 `runClient`；
- 只有未来玩家发行卡才运行 `python tools/verify.py release` 或历史 `--record`；
- `4.5Fix/`、本地参考 dump、`build/`、`run*/` 与 `src/src/` 重复树不是 canonical
  主树，不得纳入主分支提交；
- 玩家发行重新开启时，必须显式消费 deferred release gates，并重新定义 soak、
  provenance、独立审计和发行 jar smoke 的验收条件。
