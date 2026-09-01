# Smelter / Mixer 收口与语义命名迁移详细计划

> 计划 slug：`recipe-portfolio/semantic-closure`
> 状态：已关闭（2026-09-01）。本文件位于 `card-plans/closed/`。semantic-wave
> bootstrap、load allocation split、Smelter ordinary closure 与 Mixer ordinary
> closure 已产出；剩余语义命名工作转入
> [semantic-naming.md](../../../current/semantic-naming.md) deferred，不阻塞后继内容卡
> 性质：串行 program card；包含一个负载策略 repair gate、两个 ordinary recipe
> production wave 和一个 active naming migration gate
> 历史 opening：`T49_READY`；current execution gap = 1,349；Bath ordinary = 0
> 本计划不占用、也不签发 `T50`；旧 topology 中的 `next_issue_id=T50` 仅作为冻结历史
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Recipe 目标：Smelter 当前 ordinary 约 352 families / 15,678 relations；Mixer
> 当前 ordinary 约 664 families / 7,193 relations
> 预期 closing：完成或有证据地重分类上述 1,016 families，current execution gap
> 降至约 333；精确值以两个 live R0 source receipt 为准
> 明确不包含：Smelter `later:recycling` 1,817 families、cross-mod compatibility、
> post-1.0、out-of-scope 与其它 host 的约 333-family 尾账
> 命名目标：active source/tool/resource/generated/docs-current 零里程碑式 TXX；
> `docs/history/**` 与 sealed archive 保留 legacy 编号
> 实际 closing：Smelter 338 completion + 14 reclassification；Mixer 648 completion +
> 15 reclassification；current execution gap = 334。两个 wave 的封板 load 判定缺口交由
> 后继 `ordinary-wave/closeout-integrity-repair` 修复
> 后继：[Ordinary 尾账收口与封板修复](Ordinary尾账收口与封板修复详细计划.md)
> 签发层级：当前工作树；本次计划签发不创建 Git commit

历史 ordinary wave 的约束仍以
[Ordinary Recipe Wave 流程与规范](../../../current/recipe-wave-workflow.md)为来源，但本计划
会先建立 slug-based v2 card/closeout 合同，替代新工作继续沿用连续 T 编号。旧 T38–T49
seal、ledger、receipt 与历史文档保持 byte-identical；新工作不得创建 `t50_*` 文件、
`recipe/t50/` 路径、`cruciblecraft_t50` namespace、`-Pt50Recipes` 或
`isT50CompactRecipe` 一类符号。

Markdown 不是 production authority。每个 recipe wave 仍必须具备 source receipt、
candidate audit、family-atomic production lock、operand disposition、publication
groups、shards、equivalence、player path、真实 GameTest、card-only/integrated load、
census delta、topology、readiness 与 closeout seal。

---

## 0. Program contract

### 0.1 串行阶段

```text
historical T49_READY
  -> semantic-wave-bootstrap
  -> runtime-load/allocation-split
  -> smelter/ordinary-closure
  -> mixer/ordinary-closure
  -> naming/active-semantic-migration
  -> RECIPE_PORTFOLIO_SEMANTIC_CLOSURE_READY
```

同一时刻只有一个 `active_child`. 后继 child 只能读取前一 child 的 sealed closing，不允许
并行改 production lock、运行时 manifest 或 census。Smelter 和 Mixer 是两个独立
production waves，不因同属本 program card 而共享或替换 family：

```text
smelter failure != mixer filler
mixer failure   != smelter filler
partial family  != completion
reclassification != completion
```

### 0.2 Opening 是冻结事实，不是新编号

Opening 从以下已关闭证据重放：

```text
tools/t49_readiness.json
tools/t49_census_delta.json
tools/t49_card_topology.json
tools/t49_closeout_seal.json
tools/global_build_identity_ledger.v2.json
tools/compact_recipe_runtime_manifest.v2.json
```

历史值：

```text
current execution gap        = 1349
compact production groups    = 19
eager publication rows       = 14
lazy logical rows            = 50652
lazy cache ceiling rows      = 876
authored entries             = 6269
sync bytes                   = 5021175
retained memory bytes        = 5021175
server reload ms             = 459
client reload ms             = 429
lookup candidates p95        = 10
lookup p95 ns                = 214887
integrated allocation bytes  = 687226880
```

`687226880 > 536870912` 是 opening 中必须处理的既有口径矛盾。不得把 T49 card-only
allocation 当 integrated PASS，也不得继续用 report-only 绕过 blocking allocation。

### 0.3 完成定义

本计划完成要求：

1. 新波次全部使用 semantic slug，无新 TXX 命名；
2. 负载策略拆分 reload transient allocation 与 lookup allocation，方法和 hard gates
   在 recipe measurement 前冻结；
3. Smelter ordinary universe 全量进入 live R0，除有逐族证据的 reclassification 外全部
   completion；
4. Mixer ordinary universe 全量进入 live R0，除有逐族证据的 reclassification 外全部
   completion；
5. 两波 closing 后 current execution gap 由 census replay 精确重算，预计约 333；
6. active code/tool/resource/generated/docs-current 通过 zero-TXX gate；
7. legacy seals 从 immutable archive 继续 `--check`，不因 active rename 重写历史。

---

## 1. Semantic wave bootstrap

### 1.1 新 card identity

新 schema 使用：

```text
wave_slug
host
cohort
representation
depends_on_slugs
unique_active_wave
next_unassigned
```

不再使用：

```text
card_id = T{N}
next_issue_id = T{N+1}
status = T{N}_READY
delivery_phase = T{N}
```

新 readiness 使用通用状态：

```text
SOURCE_READY
PRODUCTION_LOCK_READY
PUBLICATION_READY
PLAYER_PATH_READY
LOAD_READY
CENSUS_DELTA_READY
WAVE_READY
```

状态必须同时带 `wave_slug`，不能仅靠文件路径推断 wave。

### 1.2 Tooling 与路径

扩展：

```text
tools/build_recipe_bulk.py
tools/recipe_bulk/waves.py
tools/closeout_seal.py
tools/verify.py
```

新 active artifacts：

```text
tools/waves/runtime-load/allocation-split/**
tools/waves/smelter/ordinary-closure/**
tools/waves/mixer/ordinary-closure/**
tools/waves/naming/active-semantic-migration/**

src/recipe_generated/resources/**
src/recipe_support_generated/resources/**
```

CLI：

```text
python tools/build_recipe_bulk.py compile --wave smelter/ordinary-closure
python tools/build_recipe_bulk.py compile --wave mixer/ordinary-closure
.\gradlew.bat runGameTestServer -PwaveRecipes=smelter/ordinary-closure
.\gradlew.bat runGameTestServer -PwaveRecipes=mixer/ordinary-closure
```

旧 `--wave T37`–`T49` 和 `-PtXXRecipes` 只能读取 frozen legacy artifacts；新 builder
不得把 semantic wave 映射成一个虚构的 T number。

### 1.3 Ledger 与 runtime manifest v3

新增：

```text
tools/global_build_identity_ledger.v3.json
tools/compact_recipe_runtime_manifest.v3.json
```

组成规则：

```text
v3 semantic root
  = frozen v2 file hash
  + semantic alias/equivalence root
  + ordered slug deltas
```

v2 文件不修改。新 delta 只写：

```text
wave_slug = smelter/ordinary-closure | mixer/ordinary-closure
```

禁止新写 `wave_id=T50` 或 `source_key=T50|...`。

### 1.4 Bootstrap exit gate

```text
legacy closeout seals all current
v2 files byte-identical
slug parser rejects T50 for new schema
semantic compile dry-run deterministic
v3 empty composition equals frozen v2 logical identities
unique_active_wave = runtime-load/allocation-split
SEMANTIC_WAVE_BOOTSTRAP_READY
```

---

## 2. Runtime load allocation split

### 2.1 问题定义

现有 `allocation_bytes` 的 512 MiB hard 来源是 lookup-only controlled workload；T48/T49
integrated measurements 却把完整 reload 的瞬时分配与它直接比较。两者生命周期、工作量和
单位语义不同。本 gate 修正指标定义，不把同一指标简单改成 report-only，也不直接抬旧
512 MiB 数值。

### 2.2 v3 metrics

新 active policy 使用语义文件名：

```text
tools/runtime_load_budget_policy.v3.json
```

至少区分：

```text
reload_transient_allocation_bytes
lookup_allocation_bytes_per_operation
retained_memory_bytes
sync_bytes
server_reload_ms
client_reload_ms
server_index_ms
client_index_ms
lookup_p95_ns
lookup_candidate_count
eager_publication_rows
lazy_logical_rows
lazy_cache_ceiling_rows
datapack_authored_entries
```

规则：

- `reload_transient_allocation_bytes` 只在显式 reload window 内采样；
- `lookup_allocation_bytes_per_operation` 只在稳定 epoch 的 lookup window 内采样；
- retained memory 继续在 controlled GC 后采样；
- authored entries 继续精确报告但不阻塞；
- 其它 hard gates 保持 blocking；
- pending、missing event、zero-filled 与 workload drift 均 fail closed。

### 2.3 Calibration

在新 recipe lock 前冻结 benchmark：

1. 当前 19-group integrated opening，至少 5 次 dedicated server/client repetition；
2. 同一 opening 的 lookup-only 20x workload；
3. 预声明的 80k logical-row synthetic reload workload，覆盖本计划最坏约 73.5k rows；
4. JFR event、GC、JVM、heap、warmup、measurement window 与 aggregation 全部写入
   workload manifest；
5. 根据 calibration 生成 soft/hard，而不是在 Smelter/Mixer 候选失败后追改门限。

旧 v2 `allocation_bytes=512 MiB` 保留在 frozen policy。v3 readiness 必须证明：

```text
old metric was split, not silently weakened
lookup hard gate remains blocking
reload transient hard gate is independently measured
retained memory hard gate remains blocking
no recipe candidate measurement participated in calibration
```

### 2.4 Load repair exit gate

```text
current 19-group opening remeasured
all v3 metrics have value/unit/workload
no pending or zero-filled axis
v3 policy hash frozen before recipe production lock
forward-v3 decision recomputable
RUNTIME_LOAD_ALLOCATION_SPLIT_READY
```

---

## 3. Smelter ordinary closure

### 3.1 Source-complete R0

权威来源：

```text
tools/t42_remaining_catalog.json
tools/t42_owner_disposition_lock.json
tools/t42_family_operand_snapshot.json
tools/t42_blocker_overlay.json        # historical diagnosis only
gt6_dump/gt6_recipe_dump/maps/gt.recipe.smelter.json
```

从 current registries、form authority、fluid/item projection 和 acquisition graph live
重放，不直接把 T42 overlay 当 production truth。初始预期：

```text
families                  ≈ 352
logical relations         ≈ 15678
exact singleton families  ≈ 253
exact_multi families      ≈ 99
deferred recycling        = 1817 excluded
```

R0 必须解释任何 count drift。不得为了保持 352/15,678 而补 family 或丢 relation。

### 3.2 Operand cohorts

逐项审计：

```text
coordination/multi_axis          ≈ 114 families
object_expression/multiitem      ≈ 106
object_expression/tool_head      ≈ 36
material_expression/form         ≈ 26
identity_mapping/unmapped        ≈ 15
object_expression/block tail     ≈ 15
acquisition/b0                   ≈ 8
recycling/evidence_needed        ≈ 2
other gt_prefix cohorts          ≈ 30
```

需要实现或证明：

- `small_dust` 等 required forms，通过 material form authority 汇总，card builder 不直写
  registration gate；
- multiitem、tool head、GT prefix 与 meta object identity；
- vanilla operand proof；
- 8 个 acquisition/B0 支持路径；
- `#0024` stone slab remainder 等可审计 source identity；
- machine execution 与 output consumption/terminal purpose。

`#0111` 若仍无可审计 identity，进入
`later:object_expression/unprovable_residue`。两个 recovery evidence family 若仍依赖
MTE dismantling runtime，进入现有 `later:recycling`。重分类必须有 family/relation/source
hash 和 recheck condition；不得写成 completion。

### 3.3 Representation 与 publication groups

使用 compile-time `exact` / `exact_multi`。本波不实现
`CompactGTRecipeFamilyDefinition.ParameterizedSpec`。

建议 groups：

```text
cruciblecraft:smelter/ordinary_closure/singleton
cruciblecraft:smelter/ordinary_closure/multiitem
cruciblecraft:smelter/ordinary_closure/tool_head
cruciblecraft:smelter/ordinary_closure/material_form
cruciblecraft:smelter/ordinary_closure/gt_prefix
cruciblecraft:smelter/ordinary_closure/unmapped
cruciblecraft:smelter/ordinary_closure/acquisition
```

Recipe path：

```text
data/cruciblecraft/recipe/smelter/ordinary_closure/<cohort>/
```

Shard 必须由 query-addressable item/tag/fluid keys 派生；禁止按 family 序号、文件数或 stable-id
hash 盲切。每个 relation 进入至少一个 deterministic shard，overflow 显式有界。

### 3.4 Static publication envelope

初始静态候选：

```text
exact_multi relations  ≈ 15425 -> eager-dominant
singleton relations    ≈   253 -> lazy-dominant
```

加 opening 后：

```text
eager ≈ 15439 / hard 21000
lazy  ≈ 50905 / hard 56000
```

这只是 measurement candidates，不是预定 winner。每个 group 都必须独立 card measurement，
再做 opening + Smelter integrated measurement。若 hard gate 失败：

1. 保持 production set 不变，比较已预声明的 policy candidates；
2. 若所有 candidate 失败，wave BLOCKED；
3. 不临时删 relation、不提高 hard gate、不用 Mixer family 替换。

### 3.5 Equivalence、player path 与 GameTest

要求：

- 每个 locked relation 完成 source/compiled/runtime 三层等价；
- every input registered and obtainable；
- every output registered and consumable/terminal；
- 每个 publication group 至少一个真实 Smelter execution representative；
- heat adjacency、buffering、duration、EUt/special value、fluid/item conservation 保持；
- isolated namespace 使用语义名，不使用 `cruciblecraft_t50`。

### 3.6 Smelter closing

```text
opening gap                  = 1349
candidate families           = live R0 count
completion_delta             = complete locked families
reclassification_delta       = evidence-backed deferred families
partial_family_count         = 0
deferred recycling           = 1817 + proven additions only
unique_active_wave           = null after WAVE_READY
next_unassigned              = true
```

关闭后才能签发 `mixer/ordinary-closure`。

---

## 4. Mixer ordinary closure

### 4.1 Source-complete R0

权威来源：

```text
gt6_dump/gt6_recipe_dump/maps/gt.recipe.mixer.json
tools/gt6_mixer_templates_index.json
tools/gt6_mixer_templates_membership.json
tools/t22_5_row_classification.json
tools/t35_recipe_families.json
tools/t42_remaining_catalog.json
```

历史口径存在 7,192 / 7,193 一行差异。R0 必须以 ordinary classification row membership
重建 source receipt，并同时证明：

```text
every scoped source row belongs to exactly one family
every family is complete
cross_mod/post_1_0/out_of_scope rows are absent
historical 64,245-row dump remains replayable
```

初始预期：

```text
families          ≈ 664
logical relations ≈ 7193
material_matrix   ≈ 297 families / 6528 rows
opaque            ≈ 367 families / 665 rows
```

### 4.2 Live blocker audit

T42 的 `current_closure_ready=23` 只是冻结快照，不是本波 production denominator。必须用
current item/fluid/form/acquisition registries 重新投影。

重点闭合：

```text
construction foam          ≈ 217 families / 4540 rows
fluid mapping              ≈ 141 unique source fluids
item/form registration     ≈ 35 materials/forms
external/multiitem         ≈ 14 object kinds
machine-shape residual     ≈ 53 rows
B0 acquisition             live replay
```

`ic2constructionfoam` ordinary family 默认实现 CrucibleCraft 自有、来源等价且可取得的
`construction_foam` fluid，不把 4,540 rows 整批丢回 cross-mod。长尾 potion/tea/foreign
fluid 必须逐项判断：

- GT6 语义可在当前 1.x 产品中原生表达：注册/映射并提供玩家路径；
- source classification 错误或依赖外部 mod 行为：family-atomic reclassification；
- 语义未知：BLOCKED，不能用名字相似的 fluid alias。

### 4.3 Mixer execution envelope

当前 T5 validator 与 GT6 panel envelope 不同。新 compact runtime 必须按
`publication_group + execution_envelope` 选择校验合同，不能按 `t50/` 路径或 Java
`isT50...` helper 绕过。

至少区分：

```text
t5_bronze
gt6_panel
```

Envelope 记录 item/fluid input/output、tank capacity、preserved catalyst 和 circuit 行为。
Circuit 存在不代表 B0/operand closure。

### 4.4 Representation 与 groups

继续使用 compile-time exact/exact_multi；664 compact families 比 7,193 个展开
`gt_recipe` 更可控。Parameterized runtime 留作后续维护优化，不阻塞本波。

建议 groups：

```text
cruciblecraft:mixer/ordinary_closure/construction_foam_matrix
cruciblecraft:mixer/ordinary_closure/material_matrix
cruciblecraft:mixer/ordinary_closure/opaque
```

必要时按 source-backed fluid semantics 增加 group；不得按 row count 或 card number 切组。

Recipe path：

```text
data/cruciblecraft/recipe/mixer/ordinary_closure/<cohort>/
```

### 4.5 Static publication envelope

在 Smelter 初始 candidate 成立时，Mixer 可采用：

```text
construction_foam rows ≈ 4540 -> eager-dominant
other Mixer rows       ≈ 2653 -> lazy-dominant
```

合并 opening + Smelter + Mixer：

```text
eager ≈ 14 + 15425 + 4540 = 19979 / hard 21000
lazy  ≈ 50652 + 253 + 2653 = 53558 / hard 56000
```

该分配证明 row capacity 有静态可行解，但不是负载 PASS。最终 group winner 由 v3
card-only/integrated measurements 决定，cache、lookup、sync、reload、retained 和两类
allocation 仍必须全门通过。

### 4.6 Mixer closing

要求：

- source membership 与 7,192/7,193 差异已解释；
- production lock family-atomic；
- all locked relations equivalence PASS；
- construction foam 与其它新增 operands 有真实玩家路径；
- GT6 panel envelope 真实执行；
- card-only 与 integrated v3 load 无 blocking failure；
- partial families = 0；
- closing census 从 Smelter closing 重放，不用 `1349 - 352 - 664` 替代。

---

## 5. Portfolio accounting

预期但不预锁的算术：

```text
opening current execution gap       = 1349
Smelter candidate families          ≈ 352
Mixer candidate families            ≈ 664
candidate total                     ≈ 1016
expected completion                 ≈ 1013..1016
expected reclassification           ≈ 0..3
expected closing execution gap      ≈ 333
partial_family_count                = 0
Smelter deferred recycling baseline = 1817
```

Closing 必须分别公布：

```text
family_count
source_rows
logical_relations
selection_sha256
representation_breakdown
publication_group_count
shard_count
worst_shard_candidate_count
completion_delta
reclassification_delta
remaining_execution_gap
deferred_ledger_count
```

若 live R0 发现更多真正 cross-mod/post-1.0 families，允许有证据重分类；若只是缺
CrucibleCraft identity/form/fluid/B0，则属于本计划基础设施工作，不得用 reclassification
逃避导入目标。

---

## 6. Active semantic naming migration

### 6.1 三层边界

```text
ACTIVE
  source, tests, tools, Gradle, runtime resources, generated resources,
  docs/current, new slug closeouts

SEALED_ARCHIVE
  byte-identical T38-T49 seal-bound artifacts and source snapshots

HISTORY
  docs/history narrative, old card IDs, old stage archives and work logs
```

最终 zero-TXX gate 只豁免：

```text
archive/sealed/**
docs/history/**
```

不允许用一个无限期 allowlist 把仍在 active roots 的 `tXX_*` 文件伪装成历史。

### 6.2 Archive before rename

对每张 sealed legacy card 建立 manifest-driven archive。必须归档 seal 实际绑定的全部字节，
不仅是 recipe generated tree：

```text
generated recipe tree
support tree
production lock
census/topology/readiness
publication/shard/runtime manifests
identity/runtime deltas
GameTest Java source snapshot
GameTest log and receipt
closeout seal
```

Archive 后：

1. 对每个 artifact 重算 hash；
2. 必须等于原 seal；
3. `closeout_seal --check` 通过 resolver 读取 archive；
4. 原 seal JSON 不修改；
5. active rename 才能开始。

### 6.3 Active rename targets

Java：

```text
T2ChainRules                         -> MaterialChainRules
T4ToolRules                          -> ToolRules
T5*Chemical*                         -> Chemical*
T14*Load*                            -> CompactLoad*
T35*Census*                          -> RecipeCensus*
T49RecipeGameTests                   -> BathTinyPurifiedGameTests
TXX_*_PUBLICATION_GROUP              -> semantic host/cohort constants
```

Publication groups 集中到 host/cohort registry，避免继续把历史波次常量堆在
`CompactGTRecipeFamilyDefinition`。

Python/tooling：

```text
tools/build_t49_*.py
  -> tools/waves/bath/tiny-purified/build_*.py

tools/t49_*.json
  -> tools/waves/bath/tiny-purified/*.json

per-card duplicated builders
  -> generic command + declarative wave spec
```

Generated roots 按领域迁移：

```text
src/t5_chemical_generated        -> src/chemical_recipe_generated
src/t11_hydrocarbon_generated    -> src/hydrocarbon_recipe_generated
src/t21_chemical_generated       -> semantic chemistry root
src/t37_recipe_generated ...     -> src/recipe_generated
src/t39_support_generated ...    -> src/recipe_support_generated
```

Runtime IDs：

```text
cruciblecraft:t49_bath_exact_multi
  -> cruciblecraft:bath/tiny_purified/exact_multi

recipe/t49/bath/**
  -> recipe/bath/tiny_purified/**

cruciblecraft_t49
  -> cruciblecraft_wave_bath_tiny_purified
```

本计划选择 active runtime ID breaking rename，不在 active registry 保留 TXX alias。
历史 IDs 只存在 sealed archive。若需要旧世界迁移，另建不含 legacy literal 的数据迁移
机制；不得为兼容方便破坏 active zero-TXX 目标。

### 6.4 Current docs 与 workflow

更新：

```text
docs/current/roadmap.md
docs/current/recipe-wave-workflow.md
docs/current/verification.md
docs/current/known-issues.md
docs/history/INDEX.md current-active section
```

现行文档改用 wave slug、`next_unassigned` 与 v2 seal。历史链接可以指向
`docs/history/**` 中的 legacy card，但现行流程不得要求下一张使用 T number。

### 6.5 Automated zero-TXX gate

新增 `tools/check_zero_milestone_names.py`，扫描：

```text
src/main/**
src/test/**
active generated roots
tools/** excluding archive manifest inputs
build.gradle
gradle.properties
settings.gradle*
docs/current/**
docs/history/card-plans/active/**
```

检测至少覆盖：

```text
\b[Tt]\d{1,2}\b
\b[Tt]\d{1,2}[._-]
[/\\]t\d{1,2}[/\\_]
cruciblecraft_t\d{1,2}
-P[tT]\d{1,2}
portfolio:track_a/t\d{1,2}
next_issue_id.*T\d{1,2}
```

排除真实非里程碑名称，如 `cobalt60`、`TankBlock`、`ToolMaterial`、`TierProfile` 与
`gt6_*`。临时 allowlist 必须逐条带 owner、reason 和 expiry；program closeout 时 allowlist
为空。

---

## 7. Verification matrix

### 7.1 Python/currentness

```text
python tools/build_recipe_bulk.py compile --check --wave smelter/ordinary-closure
python tools/build_recipe_bulk.py compile --check --wave mixer/ordinary-closure
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile census-replay
python tools/verify.py integration --profile card-closeout
python tools/verify.py integration --profile closeout-seals
python tools/check_zero_milestone_names.py
python tools/verify.py dev
```

Source dump 不存在时允许日常 `--check` 验证 frozen source receipt，但状态必须是
`SOURCE_RECEIPT_CURRENT_WITH_DUMP_UNAVAILABLE`，不能写成 full replay PASS。正式 closeout
必须在固定 revision dump 上完成 full replay。

### 7.2 Java

```text
.\gradlew.bat test --no-daemon
.\gradlew.bat runGameTestServer -PwaveRecipes=smelter/ordinary-closure --no-daemon
.\gradlew.bat runGameTestServer -PwaveRecipes=mixer/ordinary-closure --no-daemon
```

JUnit 至少覆盖：

- slug/v1 legacy schema dispatch；
- ledger/runtime v3 composition；
- publication group 与 envelope registry；
- exact/exact_multi codec、router、shadow order 与 dedup；
- Smelter/Mixer 每个 group 的全 relation equivalence；
- v3 load metric window、pending、zero-event 与 hard failure；
- zero-TXX scanner false positive/false negative；
- legacy archive seal resolver。

### 7.3 Load

每个 production group：

```text
1x card-only
20x declared stress where applicable
opening + current wave integrated
server + dedicated client
reload + lookup windows separated
controlled GC retained measurement
sync payload
```

Program closing 还要测：

```text
historical 19 groups
+ Smelter groups
+ Mixer groups
```

不得用各 card 数值相加冒充 integrated measurement。

---

## 8. Block / withdraw rules

任一条件成立时，当前 child BLOCKED：

- source membership 不完整或 family 被切开；
- operand 只能靠损失性 alias 表达；
- B0/B1 玩家路径是 declaration-only；
- machine envelope 依赖 card/path 特判；
- shard query 需要全表扫描或 candidate hard failure；
- exact/runtime relation 不等价；
- v3 任一 blocking load axis pending 或 hard fail；
- legacy seal/archive hash 漂移；
- new active artifact 出现 TXX 命名。

撤回只撤当前 child，不重写已关闭 child。撤回记录必须保留 source root、candidate root、
selection hash、失败 families/groups 和原因。不得通过删 relation、换 host、提高 hard gate、
放宽等价或新增 T number 保住表面 READY。

---

## 9. Program closeout

最终 readiness：

```text
SEMANTIC_WAVE_BOOTSTRAP_READY
RUNTIME_LOAD_ALLOCATION_SPLIT_READY
smelter/ordinary-closure = WAVE_READY
mixer/ordinary-closure = WAVE_READY
ACTIVE_SEMANTIC_NAMING_READY
legacy seals current
zero-TXX active findings = 0
partial families = 0
remaining execution gap recomputed
unique_active_wave = null
next_unassigned = true
RECIPE_PORTFOLIO_SEMANTIC_CLOSURE_READY
```

Closeout 时新增语义 work log 和 stage archive，更新 current roadmap/workflow/verification
与历史索引。本计划不自动创建 Git commit；提交仍需用户明确授权。
