# Ordinary Recipe Wave 语义合同

> 全项目进度单位是
> [能力晋级](capability-delivery-workflow.md)。Recipe Card 只用于限定内容工作集，
> 不是 runtime 类型、验证身份或路线图完成单位。编号 builders、历史 receipts 与
> closeout seals 已归档，不参与 active verification。

## 1. 固定流程

机器身份与 RecipeMap 编译分开：

- `machine_delivery.json` 是 kind-level sidecar：RecipeMap、spec family、能量、
  槽/罐签名、GT6 source、贴图 profile。名称、acquisition 与 variant 行仍在
  `machine_kinds.json` / `machine_tiers.json` / `machine_acquisition.json`。
- 新 kind 仍要一次人工 Java `ProcessingMachineSpec`（槽位、流体容量、能量模式、
  输入动作、GUI）。之后屏幕、模型、语言、掉落、标签、EMI 与合成由 catalog/datagen
  推导，不再改固定白名单。
- 编译单位是 RecipeMap/family，不是机器档位。RU/EU 同图只导入、锁定、编译一次。

配方接入：

```text
Source Pack
  -> RecipeImportSpec + work_set
  -> import-source（source.json + lock_candidate.json）
  -> 人工审核 production_lock.json
  -> recipe_bulk compile
  -> generated/runtime equivalence
  -> player path + machine execution
  -> load
  -> capability profile PASS
```

新 RecipeMap 只增加 `SourcePack + recipe_import.json + work_set + 人工
production_lock`。不得新增 per-wave builder，也不得把卡号写入 `WaveSpec`、Java
类型、配方路径、codec、cache key。`import-source` 不得写 `production_lock.json`
或 `src/recipe_generated`。缺形态、缺身份或跨域依赖保持 `blocked`，不发 stand-in。

落地同时只允许一条 active delivery lane。Prep 卡（计划在
`docs/history/card-plans/prep/`）可以签发并在分支上 `import-source` /
isolated compile，但不得写 live `src/recipe_generated`，也不得把
`unique_active_wave` 改成自己。一个 Card 可以拥有多个
`(target_map, publication_group)`；一个 group 可以拥有多个 query-addressable shards。
Shard 是运行时索引单位，不是另一张卡。缺失单块机器晋升落地仍要 unique-active
空窗；prep 签发不因工具管线自动改 unique-active。

## 2. Source Pack 与 production lock

Source Pack 声明：

```text
source_system
source_revision
source_recipe_identity
semantic_family_identity
external file checksums
```

外部文件 checksum 用于确认输入身份，属于明确保留的边界。它不能扩散成内部 builder、
receipt、currentness 或 seal 的摘要链。

`lock_candidate.json` 可以重算；`production_lock.json` 是人工审核后的语义集合。lock
保留 family/template/stable IDs、计数、来源 revision、representation、publication
groups、player-path baseline 和 phase-owner disposition。它不绑定内部 Python/Java/JSON
文件摘要。candidate 漂移不得自动覆盖 production lock。

Operand 必须逐项分类为 `proven_equivalent`、`needs_current_expression`、
`phase_deferred` 或 `unsupported`。只有等价已证明或当前表达已实现的 operand 才能进入
production。family 在全部 relations 实现前只能是 `partial`，不得扣 gap。

## 3. 表示、publication group 与 shard

每个 family 明确选择一种表示：

- singleton：exact relation
- 小型 combinatorial：exact multi-relation + global shadow order
- 大型 material/template expansion：parameterized template + slots
- 无法安全表达：blocked/overflow，禁止静默展开

生产 policy identity 是 `(target_map, publication_group)`。Query router 必须从当前
query 可见的 item/tag/fluid/component/shape keys 直接选择 shard，不能先扫描全部 shard。

compact publication、recipe identity 与 shard membership 摘要属于运行时功能性身份，
继续保留。验证必须同时证明：

- 每条 logical relation 恰好进入声明的 group/shard
- router 对相关与不相关 query 都返回正确候选
- priority、shadow order、consume/preserve 语义不变
- overflow 有显式预算且超限 fail closed
- reload 与网络同步前后身份一致

这些摘要不能被复用为开发流程的“已验证”凭据。

## 3.1 Transport fragments

物理 RecipeHolder 必须满足 `relations<=4096`、dictionary `<=8192`、wire `<=512KiB`。
超过产品门时，编译器按 shared shape 切成 `_fragment_NNNN` 文件；JSON 内 semantic
`family_id` 不变，不得把分片后缀冒充新的 GT6 family。`CompactRecipeShardRouter`
继续只负责运行时查询路由。加载时 fail-closed 重组：缺片、重片、digest/header
漂移全部拒绝。publication policy 看到的仍是重组后的 1 个 semantic family。
dedicated client 的 `update_recipes` 必须能编码每一个物理 holder；integrated
singleplayer 不能替代该门。

## 3.2 Dump-proven Rule IR

权威合同是固定 GT6 dump + Rule IR + explicit exact remainder + blocked ledger。
生成的 compact / material-rule JSON 仍提交为快照，不引入运行时动态数据包。Rule IR
只生成已注册且 dump-proven 的精确 operand；例外逐条保留，禁止模糊 tag 匹配和占位
配方。编译器必须证明
`rule expansion ∪ exact remainder ∪ blocked = selected dump rows`，并逐条比较
stable ID、输入输出、时长、EU/t、action 与 provenance。family 单独迁移、单独关闭：
sanding 是首个 dump-backed pilot（tool-head cycle、nugget→round prefix
transform、其余 exact remainder）。之后只迁移 press/lathe/bender/wiremill/cutter
等 prefix-regular family；化学、MTE identity 和一次性 DESIGN_POLICY 继续 exact。

## 4. Fresh 验证

active `recipes` profile 每次 fresh 执行：

1. Source Pack 外部完整性与 spec schema（`import-source --check`）
2. 机器 delivery sidecar 与 kind/tier catalog 一致
3. 在临时目录对小图（exact singleton）和大图（exact_multi）重新 compile
4. 公布 `family_count`、source rows、logical relations、representation、
   publication groups/shards、overflow；reload/sync 指标在 isolated compile 中
   标 `not_executed`，不得用旧 receipt 填 PASS
5. Java/JUnit 的真实 provider、codec、router 与 RecipeManager 测试由
   `runtime-java` 承担。物理 RecipeHolder 与 semantic family 分开记账；
   publication policy 的 `family_count` 是重组后的 semantic family 数
6. Rule IR 波必须在 emit 前证明
   `rule expansion ∪ exact remainder ∪ blocked = selected dump rows`
7. 内容卡需要 isolated GameTest 时，用 Gradle 属性隔离，不把
   `gametest_receipt.json` / `readiness.json` / closeout seal 当当前 PASS

```powershell
python tools/build_recipe_bulk.py import-source --spec <path> --check
python tools/build_recipe_bulk.py compile --wave all --check
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile recipes --if-changed
.\gradlew.bat runGameTestServer -PwaveRecipes=<host/cohort>
```

`recipes` 在 active profiles，不进 release：它不启动 GameTestServer。内容导入卡
必须带 `-PwaveRecipes=<slug>`；工具 profile 只断言该属性存在。表示只允许
`exact` / `exact_multi` / `matrix_v1`；没有 source-row 等价证明和 Java runtime
支持时禁止 parameterized。

GameTest 与 load 的历史 JSON/日志只用于调查当时发生过什么。当前能力晋级必须在
同一次 verification 调用中重新执行所需 runtime 测试。Registry census
（`-PrecipeCensus`）是冻结子集的物品/方块 registry 探针，不是普通配方卡闭卡门；
日常门是材料闸门与手写配方的材料形态测试。release 或改注册表后再跑：

```powershell
.\gradlew.bat runGameTestServer -PrecipeCensus
```

## 5. 玩家路径与等价

Recipe family 完成必须覆盖：

- source → generated 的全部字段和全部 relations
- generated root → Java codec/provider
- server/client/reload/EMI 的实际可见集合
- 生存获取输入、供能、执行、输出消费或明确终端用途

禁止用 `relations[0]`、test-only catalog、GameTest 注入、静态类名搜索或历史 receipt
冒充玩家路径。配方 UI 是 EMI。

## 6. Load 与容量

同时公布 `family_count`、`source_rows`、`expected_logical_relations`、
representation breakdown、publication group/shard count、overflow 与 worst routed
candidate。容量门约束 query 实际候选区间和 integrated load，不把“每 N 行一张卡”当性能
策略。card-only 通过不能掩盖 integrated regression。

## 7. 历史与迁移

历史编号波的源码和 artifacts 保持原字节，并列入
`tools/legacy_verification_index.json`。Active policy 不 import、调度、重签或重建它们。
核心 runtime ABI 变化由当前 compatibility profile 回归，而不是批量刷新历史 seals。

历史 candidate / lock 与当前 blocker 不是同一层：

- `candidate_selection.json` 是某个 wave 的候选快照；它可以保留当时的
  `blocked` 数，即使后继 wave 已经消费了这些 family。
- 正常排期只读 current recipe ledger、blocker catalog 与 batch index；不要求
  排期者人工回查每一张历史 closeout。
- 生成链必须把后继 wave 的 current selection 投影到 recipe ledger，再由一致性
  校验把 recipe ledger 与 blocker catalog 对齐。若两者不一致，检查应 fail closed，
  先修复投影，不能用历史文件手工“猜”出当前分母。
- 后继 wave 已消费 family 时，旧 candidate 不得重新打开 blocker，也不得把旧
  分母加入新的批处理卡。Bath T48/T49 的旧 150 / 5 正是历史快照，不是当前
  recipe wave。

历史流程、分母、compact 波与关闭记录见 [docs/history](../history/INDEX.md)。冻结但未进入
游戏的能力见 [unimplemented-gap.md](unimplemented-gap.md)；不得根据历史 `_READY`
反推玩家完成。
