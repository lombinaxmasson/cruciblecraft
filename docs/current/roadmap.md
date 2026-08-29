# CrucibleCraft 总体规划

> 唯一总体规划与项目导航
> 最后更新：2026-08-29
> 当前状态：T32 工程卫生与可验证性重构、T33 地表石子可达性闭环、T34 开局与 T18 青铜
> 能源链表现层、T35 1.x 全域 census 与预算基线、T36 机器注册与等级矩阵重构、
> T37 ordinary_optional Assembler 校准、T38 Roaster 29-family 固定波次已完成。
> T39-Repair 与 T39 已关闭：原 157/250 host-complete 签发保留为 test-only fixture，
> production lock 22 families / 32 relations 已达到 `T39_READY`。T40 Electrolyzer
> production lock 13 families / 22 relations 已达到 `T40_READY`；catalog 61/151 只是
> test fixture。T40-VR 验证基础设施已达到 `T40_VR_READY`（`owns_families=0`）。
> T41 Assembler bulk-singleton production lock 已达到 `T41_READY`：292 authored /
> 242 live unique，50 条 leftover-vanilla 由 T37 表达，当时 gap 5,305。T42、T42-Repair 与
> T42-Owner 分别达到 `T42_PARTITION_READY`、`T42_REPAIR_READY`、`T42_OWNER_READY`；
> Owner lock 将 1,817 个 proven Smelter recovery 记入 deferred ledger。T43 Smelter
> stone bulk production lock 已达到 `T43_READY`：407 complete families / 407
> relations，closing current execution gap 为 3,076。T36-Repair 已达到
> `T36_REPAIR_READY`（`owns_families=0`，不占用内容卡编号）。T44 Storage bundle 已达到
> `T44_STORAGE_READY`：storage 28/624 与 logistics 1/1，recipe gap 仍为 3,076。T45 Recipe
> Bulk Compiler + block-object 生产波已达到 `T45_READY`：379 complete families /
> 379 relations，closing current execution gap 为 2,697。T46 未签发，不预写 host 或
> family IDs。不进行玩家发行、RC soak 或 GA。

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
- T39 起的统一签发、dependency manifest、receipt、load、census 与 clean-checkout
  规则见 [ordinary recipe wave 流程与规范](recipe-wave-workflow.md)。

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
6. **T38 · Roaster 29-family 固定波次**（已关闭，`T38_READY`，2026-08-25）
   - host `cruciblecraft:roaster`；冻结 29 family（`#0000`、`#0001`、`#0003`–`#0029`）；
   - T35 family ledger 对应 **73 条 ordinary source rows**，不是 29 条：`#0000`=38、
     `#0001`=3、`#0003`=2、`#0010`=3、`#0014`=3，其余 24 个 family 各 1 条；
   - 生产策略由 73-row 实测派生为 on_demand（0 eager / 73 lazy / cache 16），未套用
     T37 的 14 eager / 36 lazy / cache 8；累计 cache closing 24；
   - ordinary recipe gap 5,668 → 5,639；T35 78,682 / 5,718 不变；authored closing **3,664**
     （compact 29 + 支撑 19）；eager closing **16,626**（compact on_demand 0 + GT 支撑 15）；
   - Player Path 已闭合：73/73 输入对照当前 T21 typed closure 可达。GameTest 注入仍不是
     player path 本身；生存取得走 source-backed 矿脉、风箱空气、T5 电解氧与回收配方；
   - 权威 overlay：[`tools/t38_readiness.json`](../../tools/t38_readiness.json)、
     [`tools/t38_census_delta.json`](../../tools/t38_census_delta.json)、
     [`tools/t38_card_topology.json`](../../tools/t38_card_topology.json)；
   - 闭卡证据见 [T38 阶段档案](../history/stage-archives/CrucibleCraft-阶段档案-T38.md)
     与 [T38 工作日志](../history/work-logs/T38-工作日志.md)。执行计划已归档为
     [T38 详细计划](../history/card-plans/closed/T38详细计划.md)。
7. **T39 · Centrifuge production-lock wave**（`T39_READY`）
   - 157/250 host-complete issuance 已因 lossy aliases、phase-owner 与 player-path
     问题正式撤回；原 hash 保留为 source replay 和 test-only router/load fixture；
   - 动态 candidate 为 22/32，但不具生产权威；人工 production lock 冻结 22 families /
     32 relations，分为 19/19 singleton group 与 3/13 multi group；
   - 七个 reactor rod families 重分类到 post-1.x nuclear，不再映射到普通 rod/ingot；
   - 玩家路径固定 `B0=T21 without T39/support`、`B1=B0+34 locked support`、
     `B2=B1+32 production relations`，禁止 target recipe 与 support 循环自证；
   - opening gap 5,639；closing 分列 22 completed 与 7 nuclear reclassified，planning
     baseline 为 5,610。Reclassification 不是 recipe completion；
   - `T39_REPAIR_READY` 与 `T39_READY` 均为 `failed_gates=[]`。详细见
     [T39 计划](../history/card-plans/closed/T39详细计划.md)与
     [T39-Repair 计划](../history/card-plans/closed/T39-Repair详细计划.md)。
8. **T40 · Electrolyzer production-lock wave**（`T40_READY`）
   - 61/151 只是 catalog fixture；动态 candidate 后人工冻结 13 families / 22 relations；
   - publication groups：singleton 11/11、multi 2/11；combinatorial `#0000`/`#0001`
     保持 BLOCKED，future owner `later:electrolyzer_combinatorial`，不扣 gap；
   - 玩家路径固定 `B0=T21+T37/T38/T39 without T40/support`、`B1=B0+0 locked support`、
     `B2=B1+22 production relations`；`unproven_lossy_alias=0`；
   - opening gap 5,610；closing `5610 - 13 - 0 = 5,597`；
   - `failed_gates=[]`；`unique_active_card = null`；当时 T41 只保留连续编号。详细见
     [T40 计划](../history/card-plans/closed/T40详细计划.md)。
9. **T41 · Assembler bulk-singleton production-lock wave**（`T41_READY`）
   - 294/1531 只是 catalog fixture；production lock 292 singletons `#0052–#0343`；
   - live unique 242；leftover-vanilla `#0290–#0339` 与 T37 `#0002–#0051` 运行时身份
     相同，记为 already expressed，不作为独立 T41 live 行；
   - publication groups：planks 85、fireproof 144、planks2 63 authored / 13 live；
     combinatorial `#0000`/`#0001` 保持 BLOCKED，future owner
     `later:assembler_combinatorial`，不扣 gap；
   - 玩家路径固定 `B0=T21+T37/T38/T39/T40 without T41/support`、`B1=B0+33 locked
     support`、`B2=B1+292 authored production relations`；
   - opening gap 5,597；closing `5597 - 242 - 50 = 5,305`；
   - `failed_gates=[]`；`unique_active_card = null`。详细见
     [T41 计划](../history/card-plans/closed/T41详细计划.md)。
10. **T42 · Remaining ordinary family partition**（`T42_PARTITION_READY`）
   - 编号连续的诊断/拓扑卡，`owns_families=0`，不生成配方或 GameTest；
   - opening 5,305；overlay：ready 28、prefix/molten 302、unique-object 4,970、
     combinatorial 4、already_expressed 1；
   - lock 扣除 already_expressed 1 与 combinatorial deferred 4；unique MTE/block
     无充分表达边界时保留在 gap；
   - closing `5305 - 1 - 4 = 5,300`；T14 delta 0；
   - `failed_gates=[]`；`unique_active_content_card = null`；T43 只保留连续编号，
     不预分配 host/families。详细见
     [T42 计划](../history/card-plans/closed/T42详细计划.md)。
11. **T42-Repair · snapshot / inventory / classifier fidelity**（`T42_REPAIR_READY`）
    - T42 内部 gate，不占用 T43，不撤销 `T42_PARTITION_READY`；
    - molten 拆成 generation-tag 184 / top-level 0 / mapping 203，generation-tag 不是
      CC molten 注册证明；
    - allowlist 为 T37/T41 alias 的 1.21.1 live 子集 **加上显式 oak_planks** +
      water/lava，不是 dump 的 143 个 `minecraft:*` scrape；anvil / iron_ingot /
      bucket 仍未证明；
    - snapshot 对齐 T37 empty-slot 与 PRESERVE/WEAR；operand intern material/form/alias；
      circuit 映射不是 T35/B0 证明，T43 不得把 autoclave/mixer circuit 槽当成已可关；
    - overlay `unique_kind_counts.mte` 2,646 ≠ unique-bucket 4,980；unique 桶仍是残差
      （约 3,766 有 kind，约 1,214 为空）；`partial_family_count: 0` 是未从 gap 扣
      partial，overlay 仍有 1 个 partial 族在 5,300 里；
    - closing gap 仍为 5,300；`completion_delta=0`；T43 R0 必须从 repaired overlay
      重冻结。详细见
      [T42-Repair 计划](../history/card-plans/closed/T42-Repair详细计划.md)。
12. **T42-Owner · retained execution gap owner partition**（已关闭，`T42_OWNER_READY`）
    - T42 内部 gate，`owns_families=0`、`completion_delta=0`、`publication_delta=0`，
      不占用 T43；
    - 为 post-repair 5,300 families 写入唯一 `current_owner`，并记录
      `secondary_owner_tracks` / `blocking_axes`；`needs_unique_block_or_mte` 仅保留为
      历史 residual bucket，不能直接签发工作；
    - MTE recovery 必须在所有 relation 上证明「consume MTE → 已注册 material identity
      output」；molten/chemical fluid output 可作为输出形状，kind 标签或 host 规模不得
      自动变为 `later:*`；
    - owner disposition lock 将 1,817 个 proven Smelter MTE recovery family 移入
      `later:recycling`；这是 `reclassification_delta`，不是 completed recipe。closing
      current execution gap 为 `5300 - 1817 = 3483`，`failed_gates=[]`；
    - storage/presentation/multiblock 的串行交错门为 `T42_OWNER_READY &&
      owner_partition_complete`。详细见
     [T42-Owner 计划](../history/card-plans/closed/T42-Owner详细计划.md)。
13. **T43 · Smelter stone bulk production-lock wave**（已关闭，`T43_READY`，2026-08-28）
    - 从 repaired T42 overlay 重冻结 407 singleton families / 407 relations；
    - 119 source identities / 406 Block+BlockItem；B1 为 406 条 worldgen scatter；
    - publication group `cruciblecraft:t43_smelter_stone`，datapack policy
      `hybrid`（0/407/24）；无 T43 Java whitelist；
    - closing gap `3483 - 407 = 3076`；`failed_gates=[]`；`unique_active_card = null`。
      闭卡证据见
      [T43 阶段档案](../history/stage-archives/CrucibleCraft-阶段档案-T43.md)、
      [T43 工作日志](../history/work-logs/T43-工作日志.md) 与
      [`tools/t43_readiness.json`](../../tools/t43_readiness.json)。
14. **T44 · Storage bundle projection**（已关闭，`T44_STORAGE_READY`，2026-08-29）
    - 内容 bundle，不是 ordinary recipe wave：storage 28/624 + logistics 1/1；
    - 18 条 source-visible 取得配方；recipe `completion_delta=0`，gap 仍 3076；
    - isolated `-Pt44Storage` GameTest 10/10；T14 hard ceiling 未上调；
    - `next_issue_id=T45`；T45 随后已关闭。闭卡证据见
      [T44 阶段档案](../history/stage-archives/CrucibleCraft-阶段档案-T44.md)、
      [T44 工作日志](../history/work-logs/T44-工作日志.md) 与
      [`tools/t44_readiness.json`](../../tools/t44_readiness.json)。
15. **T45 · Recipe Bulk Compiler + block-object wave**（已关闭，`T45_READY`，2026-08-29）
    - 可复用 bulk compiler；T43 407-family 只做 test-only replay；
    - production lock 379 singleton families（smelter 271 / drying 108）；catalog 365；
    - groups `t45_smelter_block`（hybrid）+ `t45_drying_block`（on_demand）；无 Java whitelist；
    - closing gap `3076 - 379 = 2697`；`failed_gates=[]`；`unique_active_card = null`。
      闭卡证据见
      [T45 阶段档案](../history/stage-archives/CrucibleCraft-阶段档案-T45.md)、
      [T45 工作日志](../history/work-logs/T45-工作日志.md) 与
      [`tools/t45_readiness.json`](../../tools/t45_readiness.json)。

### 4.1 T39+ 内容轨（T36 新 topology epoch）

T36 撤销未启动的旧 T38–T46 storage-first 编号，并从 T38 重新连续签发。T36/T37 固定
不变。新 epoch 在 [`tools/t36_card_topology.json`](../../tools/t36_card_topology.json)：

```text
T36 complete
T37 calibration complete
T38 Roaster 29-family wave complete
T39 Centrifuge production-lock wave complete
T40 Electrolyzer production-lock wave complete
T40-VR verification-infrastructure repair complete
T41 Assembler bulk-singleton production-lock wave complete
T42 remaining ordinary family partition complete
T42-Repair snapshot/inventory/classifier fidelity complete
T42-Owner retained-gap owner partition complete
T43 Smelter stone bulk production-lock wave complete
T36-Repair machine-tier extensibility gate complete (`T36_REPAIR_READY`; not a content card)
T44 Storage bundle complete (28/624 + logistics 1/1; recipe gap unchanged)
T45 block-object production-lock wave complete (379 families; bulk compiler reusable)
subsequent bounded recipe waves until current execution gap = 0
1.x exit gate
nuclear source/physics census
```

T42-Owner 已关闭，storage 不再等待 execution gap 清零才能串行签发；但它仍要求
`T42_OWNER_READY && owner_partition_complete`，不能只因 deferred ledger 非空而放行。
当前无 unique active 内容卡。T46 未签发，不预写 host 或 family IDs。
[T36-Repair](../history/card-plans/closed/T36-Repair详细计划.md)
已关闭（`T36_REPAIR_READY`；`owns_families=0`）。[T44](../history/card-plans/closed/T44详细计划.md)
已关闭（`T44_STORAGE_READY`）：Storage 28/624 + logistics 1/1 不计 ordinary recipe
completion。[T45 详细计划](../history/card-plans/closed/T45详细计划.md) 已关闭：可复用
bulk compiler + 379 complete block-object families；Bath MTE 审计保持只读。仍禁止并行
active 内容卡。P0–P3 只控制顺序，不再决定 `portfolio_scope`。

T39 已按 production lock 规则关闭；T40 Electrolyzer、T41 Assembler bulk-singleton、T42
partition、T42-Repair、T42-Owner、T43 Smelter stone bulk、T44 Storage bundle 与 T45
block-object bulk 同样关闭。后续 bounded wave
必须在前一张 closing artifact 上重新签发，按以下顺序选择：

1. 已有可运行 host；
2. 输入获得、机器供能、输出消费或明确终端用途能闭合；
3. family 结构相似，可复用已经验收的 exact-relation 或参数模板；
4. 在 current T14 opening 下可形成有界、可实测的批次。

T39 起不再把 50–200 source rows 当成 Card 上限。普通 recipe wave 的 production lock 默认
以 **至少 300 complete families** 为目标；不足 300 的 ready/form/B0 切片先并入能够共享
source-object、acquisition 或 fluid 语义能力的大 cohort，而非单独占用内容卡。仅在最终
尾账、解除关键基础设施阻塞或独立退出门明确要求时可例外，并且例外必须在 R0 写明。
Card 是 ownership/census 单位，可以拥有多个 `(target_map, publication_group)` policy/load
单位；每个 group 再由 query 可直接求出的 item/tag/component/fluid/shape keys 自动分成
Shards。T14 64/128 控制的是 routed candidate interval，不是整张 Card 的 source-row 总数。

每张卡仍必须同时公布 `family_count` 与 `source_rows`，并额外公布 representation
breakdown、publication group/shard count、overflow 与 worst routed candidate。Bath、
Mixer、Smelter 等大分母按可评审的 family/玩家路径/load capacity 拆成连续 Cards，但
一张 Card 可以覆盖数百 families 或数千 parameterized source rows。如果切片跨越同一
family，该 family 在全部 relations 实际生成、进入运行时并验收前只能记为 `partial`，
不得提前从 family gap 扣减。

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
```

模板、provider 或生成器存在不等于 family 完成。只有进入本波冻结范围、实际生成、运行并
通过 closure / fidelity / load 验收的 family 才能扣减 recipe gap。

统一的 Source Pack、work-set freeze、publication group、query-addressable shard、
dependency manifest、committed aggregate receipt、integrated load、census/topology 与撤回规则见
[ordinary recipe wave 流程与规范](recipe-wave-workflow.md)。后续 card plan 可以增加
更严格门禁，不得弱化该规范。

GT6、未来 GT6U 与 design recipes 使用独立 append-only Source Packs。完全重复只增加
provenance alias；新增 relation 建 extension contribution；override 建 compatibility
overlay。新来源是否扩大 1.x gap 由新 census intake 明确决定，不回写历史 compact source、
stable ids 或 closing gap。Closed receipt 固定 `source_revision + runtime_abi`；未来核心
runtime 变化走集中 compatibility/migration gate，不逐卡重签。

Recipe gap 清零后，尚未关闭的 deferred ordinary ledger 仍挡住 1.x exit；storage 本身
可在 `T42_OWNER_READY`、完整 owner partition 与 deferred ledger 均已锁定后，按共享运行时
架构串行签发 bundle，而不是一族一张卡。其 canonical
Storage 分母包括 T35 冻结的 28 source sites / 624 expanded registrations；跨分类的
`mass_storage_logistics` 1/1 保持独立计数，不得把 Storage 624 写成 625。Storage 卡号从
当时的下一连续编号继续，因此 **T39 不是预留的 storage 卡**。

T43 后的 recipe 主线按可复用 blocker cohort 排序，而非按 machine opening 排序。T45 已关闭
379 complete block-object families 并留下可复用 bulk compiler；Bath MTE
treatment/reconditioning 审计保持只读，不单独占用零产出卡。Mixer
bulk 仍排在其后，优先用构建期有限展开，而不是先开 runtime `ParameterizedSpec`。Bath 的
MTE 加浴液不是 `later:recycling`；Mixer 的 B0/fluid 依赖不能用 support token 或已注册流体
冒充取得。每一 cohort 仍须达到完整 family、玩家路径和 T14 load 的 production-lock 证据，
不能将多轴数量相加当作 closing delta。

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
- current closing artifact 派生的 **current recipe execution gap = 0**；不能用模板存在、文件数或
  P0/P1 完成替代全量 family 关闭；
- deferred ordinary ledger 中每一项均已关闭，或经独立、明确的 post-1.x scope decision
  处理；`later:*` 不能无限期删除；
- 所有 `in_scope` source-backed storage bundle 全部关闭，T35 Storage 28/624 分母与
  独立的 cross-category owner 均有 current disposition；
- closure / fidelity / load 三轴无 pending blocker，玩家路径、census 与 T14 load ledger
  全部 current，且各 load 轴不越 hard ceiling。

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
