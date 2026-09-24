# CrucibleCraft 总体规划

> 唯一总体规划与项目导航。
> 进度计 accepted `runtime_ready` 与 `release` checkpoint。
> 玩家获得性是独立 `survival_access`，试玩是项目级 cycle。
> 合同见 [capability-delivery-workflow.md](capability-delivery-workflow.md)。
> 现行 unique-active、prep 与试玩 cycle 只写在
> [project-status.md](project-status.md)，不要在本页手抄。
>
> 关闭档案与机制卡 `*_READY` 见 [docs/history](../history/INDEX.md)。
> `*_READY` 不是游戏里已有这些内容。缺口入口是
> [unimplemented-gap.md](unimplemented-gap.md)，现行库存只看
> [project-status.md](project-status.md) 与 [blocked.md](blocked.md)，
> 不要从阶段档案、归档长文或 Prep 计划文件倒推剩余工作。

## 1. 项目目标

CrucibleCraft 是 Minecraft 1.21.1 NeoForge 上的 GT6 风格工业模组。技术目标是让
材料、配方和机器族的常规内容变化优先由数据与规则驱动，而不是按单个对象复制 Java。
库存材料形态身份是分层混合（公共前缀一人一 Item，内部长尾才用组件），见
[材料身份合同](material-prefix-identity.md) 与
[分层混合 ADR](../decisions/材料身份分层混合ADR.md)。
live 已由 `registry/hybrid-material-identity` 对齐该目标（已关 `runtime_ready`）。

本项目不宣称是 GT6、GT6U 或任何其他模组的完整移植。来源事实、派生规则和设计决策
必须分别记录为 `SOURCE_BACKED`、`SOURCE_DERIVED` 或 `DESIGN_POLICY`。

「不全量移植 GT6」是历史产品声明，不是增长禁令。它不能再用来阻止生成器、
catalog 或对照工具。全量 GT6 源码目标与当前 runtime portfolio 分开核算；
唯一的全量覆盖入口是 [GT6 全量覆盖重评估](gt6-full-coverage.md)。

## 2. 阶段状态与证据

- 已关闭阶段的档案、工作日志与编号卡计划在 [docs/history](../history/INDEX.md)。
- `0.1.0-rc.1` 是历史工程候选版本，不是 `1.0.0`、GA 或玩家发行承诺。
- `0.1.0-test.20260922.1` 是小群私测快照，不创建 GitHub Release、不累计 RC soak。
- 历史 full verification report 已从工作树删除；内容开发不以旧 `--check-ready`
  通过与否作为日常完成判据。
- 当前进度只接受当前 revision 上 fresh 执行的 capability profile PASS；
  `capability.json` 不保存可自行刷新的 `evidence=current`。
- 分层验证见 [验证指南](verification.md)；已知验证债务见
  [`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)。
- 现行 bounded recipe wave 规则见 [ordinary recipe wave 流程与规范](recipe-wave-workflow.md)。
- 对照图之后的冻结 / 未实现缺口入口见 [unimplemented-gap.md](unimplemented-gap.md)。

阶段关闭的三个独立轴仍是：

1. **闭包**：分类、注册、资源和运行时路径是否闭合；
2. **保真**：数值与行为是否有固定来源或明确设计依据；
3. **载荷**：注册、配方、索引、内存和同步成本是否在声明预算内。

任一历史阶段显示 READY，只说明其当时约定的分母和判据成立；它不自动证明玩家可玩性、
视觉完成度或完整 GT6 覆盖。

## 3. 历史源码留档语义

仓库历史中的 `1.0` / `1.x` 只表示当时定义的源码阶段 portfolio 与留档边界，
不是正式 `1.0.0`、GA、完整 GT6 覆盖或玩家发行承诺。当前版本仍是
`0.1.0-test.20260922.1`；历史退出门不能替代本地 GT6 全量 reconciliation。

## 4. 当前内容顺序

同一时刻只允许一张内容工作处于 active 状态。编号卡时代已经结束；现行顺序是
semantic wave，不是下一张里程碑编号。

现行 unique-active、prep 与试玩 cycle 只写在
[project-status.md](project-status.md)。同一时刻只允许一张内容工作处于
active 状态；prep 不占落地锁，规则见
[能力交付流程 §8](capability-delivery-workflow.md)。
当前 active/prep/paused 排期只读
[project-status.md](project-status.md)；跨能力排期只读 [blocked.md](blocked.md)
与 `tools/blockers/batches.json`。历史 `1.x` / R0 / READY 记录只作来源和审计背景，
不代表 GT6 全量完成，也不代表玩家完成。

完整 GT6 源码目标、当前 RecipeMap 对应关系、已发布行、overflow、身份/形态/流体/
获得性缺口统一读 [GT6 全量覆盖重评估](gt6-full-coverage.md)。缺形态进入
[材料形态需求普查](../history/card-plans/prep/材料形态需求普查详细计划.md)，
不能按生成旗标全开长尾。

已关闭且仍约束现行账本的事实：

- 历史 census、compact wave、ordinary closeout 和 load measurement 仍保留为各自证据，
  但不作为全量 GT6 覆盖率或单一剩余数字；
- 当前 fresh reconciliation 的 source/runtime/published/survival 四轴见
  [GT6 全量覆盖重评估](gt6-full-coverage.md)。

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
overlay。新来源是否扩大 GT6 full-coverage gap 由新 census intake 明确决定，不回写历史 compact source、
stable ids 或 closing gap。未来核心 runtime 变化走当前 compatibility/migration profile，
不逐卡重签历史记录。

Recipe gap 清零后，deferred ordinary ledger 也已关闭或独立 scope。storage 本身
已作为独立 bundle 关闭，不再等待 execution gap 清零。Storage 分母仍是 census 冻结的
28 source sites / 624 expanded registrations；跨分类的 `mass_storage_logistics` 1/1
保持独立计数。T44 关的是注册与单物品容量。仓储桶前缀合并已在 runtime；
剩余差异以代码与试玩为准，不要抄归档缺口页。

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

## 7. 全量 GT6 目标退出门

当前没有“全量 GT6 已完成”的退出结论。新退出门必须由
[GT6 全量覆盖重评估](gt6-full-coverage.md) fresh 派生，并同时满足：

- 本地 GT6 分母、机器 kind、身份、配方图和明确 out-of-scope 项均有 current disposition；
- source coverage、runtime coverage、published recipe coverage、survival/obtain
  coverage 分开报告，不能以 capability 数、文件数或历史 READY 替代；
- 化学/热处理和其他 recipe map 的 bounded subset、full replay、overflow 与 deferred
  pipeline 已逐图对账；
- identity、material form、fluid、obtain、worldgen、runtime 和 verification 缺口均有
  owner 或明确的 out-of-scope 决策；
- `tools/playtest/current_cycle.json` 已由人工 `runClient` 签收，或者明确保持未签收。

历史 `1.x` portfolio 退出门继续作为历史审计上下文保存，但不再作为全量 GT6 目标的完成证明。

## 8. 日常开发与留档纪律

- 日常改动运行 `python tools/verify.py dev`；
- 修改 datagen 时必须连续双跑并比较生成树；
- 内容卡闭合运行 `python tools/verify.py integration --profile <name>`；
- 试玩是项目级 cycle：`python tools/playtest.py check`。人跑 `runClient`
  后才能 `record-accept`；CI 不自动签收；
- 只有未来玩家发行卡才运行 `python tools/verify.py release` 或历史 `--record`；
- `4.5Fix/`、本地参考 dump、`build/`、`run*/` 与 `src/src/` 重复树不是 canonical
  主树，不得纳入主分支提交；
- 玩家发行重新开启时，必须显式消费 deferred release gates，并重新定义 soak、
  provenance、独立审计和发行 jar smoke 的验收条件。
