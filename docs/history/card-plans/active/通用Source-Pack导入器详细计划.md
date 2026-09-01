# 通用 Source Pack 导入器详细计划

> 计划 slug：`portfolio/generic-recipe-generator`
> 状态：已签发（2026-09-02），尚未实现；本文件位于 `card-plans/active/`
> 正式名称：通用 Source Pack 导入器
> 性质：零 family 的机制 program；收掉 Source Pack → canonical source /
> compile spec 之间的手工胶水，不重造已经完成的 `recipe_bulk` 编译器
> Opening：`SOURCE_CAPABILITY_MAP_READY`；`unique_active_wave = null`；
> `next_unassigned = true`；growth-order 的
> `next_major = portfolio/generic-recipe-generator`；无 production lock
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 目标状态：`GENERIC_RECIPE_IMPORT_READY`
> 目标计数：`owns_families = 0`、`completion_delta = 0`、
> `generated_recipe_count = 0`
> 前置 program：
> [源能力对照图](../closed/源能力对照图详细计划.md)
> 明确不包含：GT6U 内容导入、新 RecipeMap、新 processing kind、机器槽/罐/GUI
> 数据化、`ParameterizedSpec`、count ceiling 调整、combinatorial production、
> 玩家路径补全、配方 load winner、封面网、T13c 收回、核能、玩家发行
> 签发层级：当前工作树；本次计划签发不创建 Git commit

权威边界仍来自
[总体规划](../../../current/roadmap.md)、
[Ordinary Recipe Wave 流程与规范](../../../current/recipe-wave-workflow.md)
以及已关闭的 source-capability growth-order。Markdown 不是 production
authority。本计划只签发工作边界；R0 的机器可读 artifact 在实施时生成。

---

## 0. 决策

下一张按 growth-order 签发
`portfolio/generic-recipe-generator`，但不按字面再造一个“通用配方编译器”。
现有链条中：

```text
reviewed production lock
  -> tools/recipe_bulk
  -> compact exact / exact_multi
  -> publication groups / shards
  -> Java RecipeMap runtime
```

已经能够完成大规模 ordinary 长尾。缺口在它之前：

```text
new Source Pack
  -> per-host source builder
  -> handwritten WaveSpec / CLI registration
  -> canonical source / lock candidate
```

本 program 只把第二段改成声明式导入。它不从来源自动推导产品范围，也不把
`dump` 自动变成 production lock。

第二候选 `portfolio/count-ceiling-kind-envelope` 不并入本卡。21000 count
已经按 Interpretation A 定性为 telemetry / report-only，新 kind 的 Java 信封也不
阻挡“已有 host 接新 Source Pack”。该轨在本 program 关闭后重新评估，不在本计划
预分配为后继。

---

## 1. 完成后应该成立的工作流

```text
Source Pack
  source_system + source_revision + immutable file hashes
  -> RecipeImportSpec
       existing target RecipeMap
       source-map selector
       family/work-set selector
       operand mapping authorities
       exact / exact_multi representation ceiling
  -> generic import authority
       canonical source.json
       source_receipt.json
       source_review.json
       lock_candidate.json
  -> human review in a future content card
       alias | extension | overlay | reject
       player path / envelope / load decision
  -> production_lock.json
  -> existing recipe_bulk compiler
```

关键分界：

1. `lock_candidate.json` 永远是草案，不具 production authority；
2. 只有未来内容卡人工冻结的 `production_lock.json` 才允许写
   `src/recipe_generated/**`；
3. import READY 不等于 family complete，不扣 census gap；
4. 一个已有 host 接一个新 Source Pack 时，只新增数据 spec 和来源文件，不新增
   `build_<wave>_source.py`，不在 `tools/recipe_bulk/waves.py` 手写
   `WaveSpec`；
5. 新 source system 可以增加一个“来源方言 adapter”，但 adapter 按来源系统拥有，
   不能按 host、card 或 wave 复制；
6. 机器信封、配方分类和产品 owner 仍由人工卡决定。

---

## 2. Program contract

### 2.1 串行 child

```text
SOURCE_CAPABILITY_MAP_READY
  -> portfolio/generic-recipe-generator-r0
  -> portfolio/generic-recipe-import-core
  -> portfolio/generic-recipe-import-proof
  -> portfolio/generic-recipe-generator
  -> GENERIC_RECIPE_IMPORT_READY
```

同一时刻只有一个 active child。后一 child 只能读取前一 child 的 sealed
closing。

四个 child 全程固定：

```text
owns_families             = 0
completion_delta          = 0
partial_family_count      = 0
production_lock           = null
generated_recipe_count    = 0
nuclear_started           = false
```

不适用普通 recipe wave 的 300-family 下限，因为这是工具机制 program，不是
recipe production card。不得借这个例外发布任意 recipe。

### 2.2 标准 portfolio artifact

每个 child 至少生成：

```text
tools/waves/portfolio/<child>/wave.json
tools/waves/portfolio/<child>/census_delta.json
tools/waves/portfolio/<child>/topology.json
tools/waves/portfolio/<child>/readiness.json
tools/waves/portfolio/<child>/closeout_seal.json
```

Program closeout 额外生成：

```text
tools/waves/portfolio/generic-recipe-generator/import_contract.json
tools/waves/portfolio/generic-recipe-generator/onboarding_proof.json
tools/waves/portfolio/generic-recipe-generator/later_star_disposition.json
```

关闭 artifact 只证明导入能力和 currentness，不允许出现 production
publication、GameTest receipt 或 load winner。

---

## 3. R0：冻结边界，不写生成器

### 3.1 Slug

```text
portfolio/generic-recipe-generator-r0
```

R0 builder：

```text
python tools/build_generic_recipe_import_r0.py --write|--check
```

### 3.2 手工胶水 inventory

R0 输出
`tools/waves/portfolio/generic-recipe-generator-r0/manual_glue_inventory.json`，
至少逐项记录：

- `tools/build_t37_assembler_source.py`、
  `tools/build_t40_electrolyzer_source.py`、
  `tools/build_t41_assembler_source.py` 等 per-wave source builder 中重复的
  dump 读取、membership、operand mapping、fingerprint、receipt 和 review；
- `tools/recipe_bulk/waves.py` 的 `SEMANTIC_WAVES` /
  `SEMANTIC_COMPILE_ORDER` 与逐行 `WaveSpec`；
- `tools/build_recipe_bulk.py` 的静态 semantic wave choices；
- `tools/recipe_bulk/ordinary_source.py` 已经通用的部分，以及仍按 Mixer、
  envelope 或历史 TXX 特判的部分；
- 现有 Source Pack manifest 只钉 counts / files / revision、不能独立描述导入
  规则的缺口；
- `tools/recipe_bulk/compile.py`、`emit.py`、`selection.py`、
  `resolver.py` 已经完成且不得重写的 compile authority。

每条 inventory 标成：

```text
reuse_as_is
extract_to_source_dialect
replace_with_declarative_spec
legacy_frozen
out_of_scope
```

R0 不能以文件行数作为完成证据；它必须指出职责和替换边界。

### 3.3 冻结两份 schema

R0 冻结：

```text
tools/source_pack_manifest.schema.json
tools/recipe_import_spec.schema.json
```

`SourcePackManifest` 至少包含：

```text
source_pack_id
source_system
source_revision
source_dialect
files[] { path, sha256, role }
provenance_policy
full_replay { required_for_first_generation, skip_is_not_pass }
```

`RecipeImportSpec` 至少包含：

```text
import_slug
source_pack
source_maps[]
target_map
host
family_membership_source
selection_rule
operand_authorities[]
representation_policy
stable_id_policy
output_paths
```

约束：

- `target_map` 必须已经存在于 `ModRecipeMaps.ALL`；
- `host` 必须是现有 CC host；本卡不能创建 host；
- representation 只允许现有 `exact` / `exact_multi`；
- mapping authority 只能引用已登记的数据 authority 或 source-dialect
  adapter，不能引用任意 per-card Python delegate；
- spec 不包含 publication winner、shard、load、player-path PASS 或 census
  completion；
- source revision 与旧 Source Pack append-only；不得把 GT6U 或 design rows
  写进当前 GT6 revision；
- 未知字段、未知 adapter、未知 target map、路径逃逸和 hash drift 全部
  fail closed。

### 3.4 R0 的 later 账本

R0 继承四条 combinatorial family，不把它们当 fixture：

```text
assembler #0000 = 620 relations
assembler #0001 = 619 relations
electrolyzer #0000 = 20 relations
electrolyzer #0001 = 15 relations
```

总计 4 families / 1,274 relations，其中 Assembler 为 1,239。

R0 状态固定为：

```text
accounted_by_program = true
imported             = false
production_locked    = false
completed            = false
```

### 3.5 R0 退出门

```text
manual glue inventory complete
two schemas frozen
existing compiler marked reuse_as_is
four later:* families accounted, none completed
no recipe/source fixture generated
unique_active_wave = portfolio/generic-recipe-import-core
GENERIC_RECIPE_IMPORT_R0_READY
```

---

## 4. Core：声明式导入权威

### 4.1 新模块边界

新增或重构为：

```text
tools/recipe_bulk/source_pack.py
tools/recipe_bulk/import_spec.py
tools/recipe_bulk/source_import.py
tools/recipe_bulk/spec_registry.py
```

职责固定：

- `source_pack.py`：验证来源 identity、revision、文件 hash 和 replay tier；
- `import_spec.py`：schema 验证和 typed model；
- `source_import.py`：来源方言 → canonical relation / family IR；
- `spec_registry.py`：按 semantic slug 从数据 spec 发现 import，不维护新
  Python tuple；
- 现有 `compile.py` / `emit.py` 继续只消费 reviewed source + production
  lock。

具体文件名允许在实现时小幅调整，但上述四个职责不得重新揉成一个新的
per-wave builder。

### 4.2 CLI

扩展现有入口：

```text
python tools/build_recipe_bulk.py import-source \
  --spec tools/waves/<slug>/recipe_import.json \
  --write|--check
```

`--write` 只允许写：

```text
tools/waves/<slug>/source.json
tools/waves/<slug>/source_receipt.json
tools/waves/<slug>/source_review.json
tools/waves/<slug>/lock_candidate.json
```

默认禁止写：

```text
production_lock.json
src/recipe_generated/**
publication_group_manifest.json
shard_manifest.json
census completion
```

未来内容卡完成 review 后，`recipe_wave(<slug>)` 可以从
`recipe_import.json + production_lock.json` 派生 compile spec；不得要求在
`SEMANTIC_WAVES` 追加一行。

### 4.3 Import 不能替人决定的事

Importer 只允许机械判定：

- canonical relation 完全相同：输出 `exact_duplicate_candidate`；
- 同 semantic family 出现额外 relation：输出 `extension_candidate`；
- 与已关闭 relation 同 identity 但行为不同：输出
  `overlay_review_required`；
- operand、语义字段、family membership 或 target envelope 不明：输出
  `blocked`。

Importer 不能自动把后三类签成 alias、extension 或 compatibility overlay。
`lock_candidate` 中任何 unresolved / blocked 项都不得进入 future production
lock。

### 4.4 Operand 与 family 规则

- 复用现有 resolver、identity ledger、material form authority、fluid/object
  overlay；不再从 T37/T40/T41 复制一套词表；
- preserve / catalyst / wear、chance、duration、energy、special value、
  buffering、shadow order 全字段保真；
- family membership 必须由 manifest 指向的 frozen ledger 或显式 work-set
  给出；Importer 不凭显示名或行相似度发明 family；
- stable id 由 `source_system + source_revision + semantic family +
  canonical relation` 派生，不含 card number；
- 大型 relation set 仍可进入 canonical source，但本卡不证明它可发布；任何
  future shard / load admission 属于内容卡。

### 4.5 Core 退出门

```text
arbitrary semantic import spec discovered without Python registry entry
source pack and import spec schemas enforced
write/check deterministic
reference-only check works without source dump
full replay requires pinned source and never SKIP-as-PASS
compile without reviewed production lock fails closed
legacy compile path remains current
unique_active_wave = portfolio/generic-recipe-import-proof
GENERIC_RECIPE_IMPORT_CORE_READY
```

---

## 5. Proof：证明少写胶水，不证明新内容

### 5.1 Test-only fixture

在：

```text
src/test/resources/generic_recipe_import/
```

建立两个有界 Source Pack fixture：

1. 已关闭 Smelter 的 exact singleton 样本；
2. 已关闭 Mixer 的 exact_multi + item/fluid/circuit 样本。

fixture 只从已关闭 canonical source 取有代表性的最小关系集，记录原 source
path、relation identity 和 hash。不得取 Assembler / Electrolyzer 的四条
combinatorial family 作为成功证明。

原因：这四条历史阻塞是 enumeration、operand/player-path 和 future load 问题，
不是缺 `WaveSpec`。拿它们证明 importer READY 会把机制卡重新变成配方长尾。

### 5.2 零注册证明

Proof child 必须通过机器可读
`onboarding_proof.json` 证明两个 fixture 的接入只增加：

```text
Source Pack files
SourcePackManifest
RecipeImportSpec
expected test fixture output
```

不得增加：

```text
new build_<wave>_source.py
new host-specific adapter
new WaveSpec(...) row
new SEMANTIC_COMPILE_ORDER entry
new WAVE_CHOICES entry
new Java RecipeMap or machine spec
```

这里的“零注册”只承诺 Source Pack 导入和 compile-spec 发现。未来 production
内容卡仍必须拥有计划、production lock、player path、load、census 和 closeout；
本计划不声称把整套 card governance 自动化。

### 5.3 Parity

两个 fixture 分别验证：

- canonical relation 全字段与来源样本一致；
- family cardinality 与 shadow order 一致；
- operand identity / action / component / fluid 一致；
- source receipt 绑定 manifest、source revision、spec 和 adapter ABI；
- 同一输入连续两次 `--write` 零漂移；
- `--check` 不修改文件；
- 修改 source、spec、adapter 或 authority 任一 hash 后 currentness 失效；
- 未知字段、未映射 operand、重复 stable id、路径逃逸、错误 target map 均
  fail closed。

### 5.4 历史兼容

- T37–T49 与现行 semantic waves 的 closed source、production lock、
  generated tree、receipt 和 seal 不重写；
- historical `WAVES` 可以保留为 frozen adapter；
- 本卡可以改共享 Python 实现，但必须先证明所有已关闭 compile/check 与
  closeout seal current；
- 不为让 parity 通过而修改历史 hash 或 canonical artifact。

### 5.5 Proof 退出门

```text
two heterogeneous existing hosts pass import parity
zero per-host Python registration proven
negative matrix fail-closed
legacy artifacts byte-identical
four combinatorial families still not imported or completed
unique_active_wave = portfolio/generic-recipe-generator
GENERIC_RECIPE_IMPORT_PROOF_READY
```

---

## 6. 四条 combinatorial 的最终去向

Program closeout 生成
`later_star_disposition.json`，逐 family 记录：

```text
family_id
template_key
relation_count
opening_owner
capability_owner = portfolio/generic-recipe-generator
content_owner = post_generator/combinatorial-family-intake
disposition = capability_available_content_deferred
started = false
production_lock = null
completion_delta = 0
recheck_condition
```

`recheck_condition` 至少要求未来内容卡同时具备：

- family-atomic source enumeration；
- operand 与 output identity 全闭合；
- B0 → B1 → B2 player path；
- host execution envelope；
- query-addressable shards；
- card-only 与 integrated load；
- production GameTest receipt；
- census completion delta。

这满足 growth-order 的“必须有去向”，但不把：

```text
Assembler 1,239 relations
Electrolyzer 35 relations
```

编进本卡 production。`post_generator/combinatorial-family-intake` 是明确
future owner，不是下一张自动 assignment；`started = false`。

---

## 7. 明确不包含

- 不导入或伪造 GT6U Source Pack；
- 不建立“直接分析 GT6U generator”的 extractor；
- 不修改当前 GT6 revision 或旧 selection hash；
- 不新增 RecipeMap、processing kind、机器 tier、槽、罐、GUI 或能源信封；
- 不打开 `CompactGTRecipeFamilyDefinition.ParameterizedSpec`；
- 不删除 exact / exact_multi runtime；
- 不调整 21000 / 41000 / 56000 / 4096 / 128 等 count 或 load 口径；
- 不签发任何 content production lock；
- 不生成 recipe、support、publication policy 或 dedup runtime 资源；
- 不运行 GameTest 来冒充无 recipe 的机制证明；
- 不修改 nuclear `started = false`；
- 不收回 cover、T13c、vanilla replace、worldgen、crop、food 或 bee 内容；
- 不顺手清理全部历史 per-wave builder。历史 builder 由 archive / replay
  继续持有；新 importer 只保证后继不再复制。

---

## 8. Verification

### 8.1 R0 / schema

```powershell
python tools/build_generic_recipe_import_r0.py --write
python tools/build_generic_recipe_import_r0.py --check
python -m unittest tools.tests.test_generic_recipe_import_r0
```

### 8.2 Core / fixture

```powershell
python -m unittest tools.tests.test_generic_recipe_import
python tools/build_recipe_bulk.py import-source --spec <smelter-fixture> --write
python tools/build_recipe_bulk.py import-source --spec <smelter-fixture> --check
python tools/build_recipe_bulk.py import-source --spec <mixer-fixture> --write
python tools/build_recipe_bulk.py import-source --spec <mixer-fixture> --check
python -m unittest tools.tests.test_recipe_bulk
```

连续执行两次 fixture `--write`，生成树和 artifact hash 必须零漂移。

### 8.3 Regression

```powershell
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile closeout-seals
python tools/source_capability_map.py --check
python tools/verify.py dev
```

若 implementation 修改了 `tools/recipe_bulk` 的共享编译面，追加现行所有
semantic compile `--check`。只有改变 Java runtime 时才需要 `gradlew test`；
本计划正常实现不应改 Java。

本 program 不运行 `runGameTestServer`，因为没有 production recipe。无 GameTest
不是缺证据；反过来，创建空或 synthetic GameTest receipt 视为失败。

---

## 9. Block / withdraw

任一条件成立时当前 child BLOCKED：

- 为新 fixture 增加 per-host Python builder 或 `WaveSpec` 行；
- manifest 不能独立重放 source identity、revision 和 file hashes；
- import 自动签 production lock；
- import 写入 `src/recipe_generated/**`；
- canonical relation 丢字段、改 shadow order 或猜 operand；
- full replay 缺源却被写成 PASS；
- existing compile / seal 发生未解释漂移；
- combinatorial family 被记成 complete；
- count-ceiling、new kind 或 nuclear 被顺手带入。

撤回只撤当前 child，保留 schema/version、失败 fixture、输入 hash 和原因。
不得通过减少字段、放宽 fail-closed、修改历史 artifact 或把 fixture 当 production
来保住 READY。

---

## 10. Program closeout

Program closeout builder：

```text
python tools/build_generic_recipe_import.py --write|--check
```

最终 readiness 必须证明：

```text
GENERIC_RECIPE_IMPORT_R0_READY
GENERIC_RECIPE_IMPORT_CORE_READY
GENERIC_RECIPE_IMPORT_PROOF_READY
GENERIC_RECIPE_IMPORT_READY

existing host + new Source Pack needs no per-wave builder
existing host + new Source Pack needs no handwritten WaveSpec row
source/revision/provenance remains append-only
production still requires a separately reviewed content card

owns_families          = 0
completion_delta       = 0
partial_family_count   = 0
generated_recipe_count = 0
production_lock        = null

four combinatorial families accounted
four combinatorial families completed = 0
nuclear_started = false
unique_active_wave = null
next_unassigned = true
```

关闭时：

- 本计划移动到 `card-plans/closed/`；
- 新增 work log 与 stage archive；
- 更新 history index、roadmap、workflow 与 capability-map 的现行解释；
- 不重写已 sealed 的 source-capability-map growth-order；
- 重新评估 `portfolio/count-ceiling-kind-envelope`，但不在 topology 里预分配；
- 不自动创建 Git commit。
