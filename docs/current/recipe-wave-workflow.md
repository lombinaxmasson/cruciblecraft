# Ordinary Recipe Wave 流程与规范

> 适用范围：现行 `ordinary_optional` bounded recipe waves 与编号连续的
> partition 诊断卡，直到 current execution gap = 0、deferred ordinary ledger
> 已关闭或明确 scope，且 owner-partition gate 已完成 retained owner partition
> 状态：现行流程；每张卡可增加更严格的门，不得弱化本文件的底线
> 当前已签发、尚未实现的机制 program：
> [通用 Source Pack 导入器](../history/card-plans/active/通用Source-Pack导入器详细计划.md)
> （slug `portfolio/generic-recipe-generator`）。它 `owns_families = 0`，
> 不签 production lock，不发布配方。`portfolio/source-capability-map` 已
> `SOURCE_CAPABILITY_MAP_READY`；对照检查在已关闭的 inventory child。
> 不适用：并行内容卡、核能阶段、玩家发行

## 1. 固定顺序与权威

```text
previous wave READY
  -> issue one bounded wave
  -> source freeze
  -> operand disposition + phase owner audit
  -> freeze production lock
  -> choose exact / parameterized representation
  -> generate publication groups and query-addressable shards
  -> generated/runtime equivalence for every logical relation
  -> player path + machine execution
  -> actual card-only and integrated load
  -> census delta
  -> remaining-gap recompute
  -> next card stays unassigned
```

同一时刻只允许一张 active 内容卡。验证 repair gate 可以 `owns_families=0`，
不占用内容卡编号。当前没有 active content child；已签发的 importer 是零 family
机制计划，R0 artifact 尚未生成。已关闭的
[源能力对照图](../history/card-plans/closed/源能力对照图详细计划.md)
（slug `portfolio/source-capability-map`）为 `SOURCE_CAPABILITY_MAP_READY`。
已关闭的
[1.x 联合退出门](../history/card-plans/closed/1.x联合退出门详细计划.md)
（slug `portfolio/one-x-joint-exit`）把已声明的 1.x portfolio 联合验收为
`ONE_X_JOINT_EXIT_READY`。已关闭的
[回收运行时与 Deferred 账本收口](../history/card-plans/closed/回收运行时与Deferred账本收口详细计划.md)
（slug `recycling/deferred-ordinary-runtime`）把 deferred ordinary ledger 收到 0。
已关闭的
[Ordinary 尾账收口与封板修复](../history/card-plans/closed/Ordinary尾账收口与封板修复详细计划.md)
把 current execution gap 收到 0；
[Smelter / Mixer 收口与语义命名迁移](../history/card-plans/closed/Smelter-Mixer收口与语义命名迁移详细计划.md)
以及 compact 波、storage bundle、census 与验证修复的证据只在
[docs/history](../history/INDEX.md)。

semantic-wave bootstrap 关闭后，新工作改用 `wave_slug + next_unassigned`。
Circuit 映射、unique 残差族（空 `unique_kinds`）、autoclave/mixer circuit 槽都不是
可关闭证明。gap overlay 的 `partial_family_count: 0` 表示未从 gap 扣除 partial。
新卡只写 `tools/waves/<slug>/required_forms.json`，由
`tools/material_form_authority.py` 与 gate builder 汇总；card recipe builder 不得直写
material registration gate。hash-only currentness 走 sidecar / `--rebind-currentness-only`，
compact `--check` 绑 versioned semantic root，禁止 `git checkout` 或 64-byte hash patch 当 closeout。

owner-partition 关闭后采用三本互不替代的账：

```text
ordinary family universe     = census foundation 5,718 historical source-backed families
current recipe execution gap = families still waiting for a content lock
deferred ordinary ledger     = phase-reclassified families with future_owner + recheck
```

`already_expressed` 从 execution gap 扣除，但不是 recipe completion。
`later:combinatorial/<host>` / `later:recycling` / `later:object_expression/<kind>`
进入 deferred ledger，同样不是 completion。storage / presentation / multiblock 只有在
owner partition 已锁定后才能串行交错，仍只有一张 active 内容卡。

每张卡的 opening 只能取前一张 closing artifact；census foundation 的
78,682 rows / 5,718 families 保持只读。Gap 的扣减单位是**完整关闭的 family**，不是
文件数、模板数或已生成 relation 数。

后续 recipe runtime 固定区分四类边界：

```text
Source Pack
  = GT6 / GT6U / design 等来源与 revision；决定 provenance，不决定运行时切片

Card
  = work-set ownership / closure / census / gap 的人工评审单位

Publication Group
  = (target_map, publication_group) 对应的 materialization policy 与 load 记账单位

Shard
  = 根据 query 可直接求出的 index keys 自动生成的 lookup/cache 单位
```

一张 Card 可以拥有多个 Publication Groups；一个 Group 可以拥有多个 Shards。Shard 不是
另一张小卡，也不是 source-row 文件分卷。

Centrifuge compact 的 concrete proof 把 157-family / 250-row withdrawn catalog 留作
test fixture，生产锁定 22 families / 32 relations，拥有 `centrifuge/singleton`
（19 / 19）与 `centrifuge/multi`（3 / 13）两个同-target Groups。两组各自派生
policy/cache，内部 shard routing 后原子 publish；fixture 测量不能成为生产 winner。

## 2. 签发规则

### 2.1 候选顺序

统一导入第三阶段（Compile Authority Cutover）已关闭：
`UNIFIED_RECIPE_COMPILE_READY`。`tools/build_recipe_bulk.py compile --wave all`
是历史 recipe wave 的唯一 production family generate/check 入口。Bath compact
各波、Smelter / Mixer ordinary-closure 与 Ordinary 尾账五 host 已关闭。
current execution gap = 0。不签发新的里程碑编号。
compact-load policy、global identity ledger 与 runtime manifest 的 v1
base 保持 byte-identical，后续波通过长期 forward-v2 和逐波 delta 组合。Shadow 账本与
runtime compatibility manifest 必须保持 READY。后续仍按以下顺序选择，不再把“host
已可运行”放在 capability/dependency closure 之前：

1. capability / dependency closure 可闭合（输入获得、供能、输出消费或明确终端用途）；
2. family 结构相似，能复用已验收的 exact-relation / parameterized runtime；
3. host 已注册且可运行；
4. current compact-load opening 下可形成有界、可实测 workload。

Centrifuge compact 是 catalog/candidate/production-lock 三集合与 shard 架构启动卡。
其 157/250 catalog 不属于生产，22/32 production lock 才是 closure/load 分母。
此后不再使用固定“50–200 source rows = 一张卡”的上限；Card 大小由结构分类、玩家路径与
上张卡实测得到的 capacity envelope 决定。普通 singleton card 可以包含数百 families；
parameterized card 可以覆盖数千 source rows，只要所有 logical relations 都完成等价验证，
integrated load 也通过。

除非本文件明确的例外适用，普通 recipe wave 的 production lock 默认不得少于 **300 个
complete families**。不足 300 的 ready、material-form、B0 或同 host 小切片必须保留在
其 owner track，或与同一 source-object / acquisition / fluid-semantics cohort 合并；不能
为了占用连续编号单独签发。合法例外仅为：

1. current execution gap 即将清零时的最终完整尾账；
2. 只有独立、有限的 runtime/catalog 基础设施卡才能解除的 blocker；
3. 已由独立 exit gate 固定的完整分母。

例外必须在 work set、production lock 和 readiness 中列出理由、实际 family/relation 数及
后续合并/清账条件。`priority_intent` 不是例外，也不能取代 R0 production lock。

每张卡仍必须同时公布：

```text
family_count
source_rows
expected_authored_entries
expected_logical_relations
selection_rule
selection_sha256
representation_breakdown
publication_group_count
shard_count
worst_shard_candidate_count
```

若 family 被切开，它在全部 relations 完成前只能是 `partial`，不得扣 gap。能选择完整
family slice 时，不为凑整数切开 family。`source_rows` 是必须全量验收的透明分母，不是
Card 的机械限速器。

Shard 的 admission gate 不是“最多 N 行”，而是：

```text
query can derive shard keys without scanning every shard
AND every relation belongs to at least one deterministic shard
AND global shadow order is preserved
AND worst routed candidate interval passes compact-load soft/hard interpretation
AND overflow/unindexed shard stays explicitly bounded
```

### 2.2 固定与撤回

签发后 work set 不得静默替换。R0 发现阻塞时只有三种合法结果：

1. 在本卡内增加有界、可计数的 acquisition/support 内容；
2. 保持 BLOCKED，先修 runtime/表达能力；
3. 正式撤回并重签，保留撤回原因和旧 selection hash。

不得用另一个 family 替换失败项来保持表面计数，也不得为单波次提高 hard ceiling。应用
300-family 默认下限的卡在 R0 缩小到下限以下时，必须撤回并与兼容 cohort 重签；不能用
无关 family、test fixture 或 partial relation 补足数量。

### 2.3 Catalog、candidate、production lock 与 phase owner

每张波次必须区分：

```text
catalog
  = source-complete universe；可作为 test-only replay/router fixture
candidate
  = 由 current acquisition/fidelity 诊断得到；允许重算，不具生产权威
production lock
  = 人工评审冻结的 family-atomic set；唯一 production/census/readiness 分母
```

Production lock 至少绑定 family/template/stable IDs、counts、selection hash、source
revision、support IDs/hash、player-path baseline、publication groups 和 phase-deferred
ledger。普通生成命令不得因 candidate 漂移而覆盖现有 lock；缺 lock 或 hash 不一致时下游
fail closed。

Operand 必须逐项分类为 `proven_equivalent`、`needs_current_expression`、
`phase_deferred` 或 `unsupported`。只有语义等价已证明、或当前表达已实现的 operand
可以进入 production。依赖 post-1.x object 的 family 按 family-atomic 规则进入明确
future owner；reclassification 与 completed 分列，不能冒充 gap closure。

Catalog fixture 必须位于 test-only resources。Fixture source replay、codec/router 与
capacity 是有效测试，但不是 production equivalence、player path、load winner 或 census
证据。

Centrifuge compact 原 157/250 host-complete 签发已撤回为上述 test fixture。验证
repair gate 只恢复 closing 资格，不拥有 family、不扣 gap；生产分母仍是不可自动漂移的
production lock。

### 2.4 后继不预分配

Closing topology 只写 `next_unassigned = true`。下一张卡的 host/families 必须在
current gap、player path 和 load closing 上重新选择。即使同一 host 有明显余量，也只能
记录为诊断，不能提前成为下一张 assignment。

## 3. 每波 artifacts

以 `<slug>` / `<host>` 表示现行语义波：

```text
docs/history/card-plans/active/<slug>详细计划.md

tools/waves/<slug>/common.py
tools/waves/<slug>/work_set.json
tools/waves/<slug>/source_pack_manifest.json
tools/waves/<slug>/<host>_source.json
tools/waves/<slug>/<host>_source_receipt.json
tools/waves/<slug>/<host>_source_review.json
tools/waves/<slug>/runtime_dependency_manifest.json
tools/waves/<slug>/publication_group_manifest.json
tools/waves/<slug>/shard_manifest.json
tools/waves/<slug>/operand_runtime_map.json
tools/waves/<slug>/player_path.json
tools/waves/<slug>/required_forms.json
tools/waves/<slug>/<host>_equivalence.json
tools/waves/<slug>/materialization_policy.json
tools/waves/<slug>/materialization_measurements.json
tools/waves/<slug>/materialization_decision.json
tools/waves/<slug>/publication_delta.json
tools/waves/<slug>/load_projection.json
tools/waves/<slug>/gametest.log
tools/waves/<slug>/gametest_receipt.json
tools/waves/<slug>/census_delta.json
tools/waves/<slug>/card_topology.json
tools/waves/<slug>/readiness.json

src/recipe_generated/resources/**
tools/build_recipe_bulk.py
```

历史编号波的 builder 与 JSON 仍留在 `tools/` 原路径，只作 sealed archive / replay。
Builder 使用 `--write` / `--check` 对称接口；JSON 由 builder 生成，不为“变绿”手改。
连续两次 `--write` 和连续两次 datagen 必须确定性零漂移。

Group/shard manifests 必须列出 canonical ids、routing keys、relation membership、policy
id 与 hash，并生成 aggregate root。人工不为每个 shard 写独立 plan、receipt 或 work log。

## 4. Source、分类与 dependency

### 4.1 Source replay

- 固定 GT6 revision；
- 首次生成、source/mapping 变化、闭卡必须 full replay；
- full replay 缺 dump、revision mismatch 或 skip 都是失败；
- ordinary CI 可使用 committed compact source + receipt；
- template 必须与历史 row classification 交叉，不能把 `template_total_rows` 当作
  ordinary rows；
- inputs/outputs/actions/chances/duration/energy/special/buffering/shadow order 全字段冻结；
- unmapped 或 runtime 无法表达时 fail closed。

### 4.2 历史只读与 live dependency

旧 census/readiness/source/measurement 是只读 closing evidence，不得重签。但旧 runtime
代码、policy 与 generated resources 仍可能是新卡的 live dependency。

每张卡生成 `runtime_dependency_manifest`，记录 path + SHA-256，至少覆盖：

- schema、codec、provider、loader、matcher；
- 前序 publication groups/policies；
- target machine、variants、energy/capabilities；
- material/prefix/registration gate；
- 本卡 generated/support trees、GameTest、Gradle isolation；
- verification profiles 与 builder policy。

Receipt、load decision、readiness 必须引用同一 manifest。Dependency 漂移后旧证据自动
失效；不得只 hash 测试类或 receipt 自身。这里的“失效”指 active/closing
`currentness`：已归档的 closed receipt 不被改写，后续 ABI 变化由 §11 compatibility
regression / migration gate 接管。

### 4.2.1 共享面 pin（block-object 波起强制，第一轮 recipes 之前付清）

closeout-seal 修复之后，已关闭波的 census / topology / readiness / GameTest receipt /
runtime dependency manifest `--check` 只比 **closeout seal** 字节与 N/L/R/gap，不再
live rebuild，也不再 pin live composed v2 或 `verification_profiles.json`。改
profiles 或后波 composed v2 **不会**把已关闭卡 `complete_family_count` 打成 0。

当前内容卡仍走 live closeout。碰了下表中仍被**当前波** live pin 的路径，必须在第一轮
recipes **之前**付清当前卡的 hash-only currentness；不要把第一轮
`python tools/verify.py integration --profile recipes` 当作发现器。这不是重写
已关闭 generated roots，也不是把新 group 写进 Java historical `compactPolicies.put`。

| 本卡改了什么 | 会咬谁 | 闭卡前怎么付 |
| --- | --- | --- |
| `GTRecipeMapLoader`、publication codec/policy definition、`CompactRecipeFamilyProvider` | 仍 hash 这些 Java 的 runtime dependency manifest 及其 GameTest receipt | `--write` 各卡 manifest，再 `--write --from-log` 已提交 PASS log |
| `tools/recipe_load_projection.py`（`DELIVERY_PHASES` / `STRATEGIES` / schema digest） | compact-load readiness 与已在 tuple 里的 load projection 的 `currentness.projection_schema_sha256` | 不重写历史投影语义；只刷新 schema digest pin。把新 phase 写进 tuple 后立刻 `--check` 已关闭投影 |
| `verification_profiles.json`、`verification_builder_policy.json`、本 workflow | 钉了这些文件的 runtime manifest | 同上，manifest + receipt |
| `ModBlockTagProvider` / `ModLanguageProvider` / `src/generated` tags 与 lang | `OreResourceTest` 静态 tag 白名单；`ProcessingMachineResourceTest` 的 `en_us`/`zh_cn` 计数 | 全量 `.\gradlew.bat test` 早于 recipes；isolated `-PwaveRecipes=<slug>` 覆盖不到这些断言 |
| `worldgen/configured_feature` 或 `placed_feature` 新 JSON | census `test_worldgen_source_dirs_are_hashed` | 冻结样本必须是 freeze 里已有的文件（主资源树用 `surface_rock_scatter.json`），禁止 `sorted(glob)[0]`；不得为加 scatter 重开 census id freeze |
| `ModBlockEntities` 新 `register` | 机器 census delta 的 `delta.evidence.block_entity_types` 是 live 扫描 | `census-replay` 前 `--write` 机器 census delta。repair freeze 不比对 live 文件哈希，但 delta `--check` 会 rebuild |

另外四条操作约束：

- 禁止 recipes 与 census / census-replay **并行**：它们会抢 `tools/waves/<slug>/*.json` 的原子替换。
- 全量 `.\gradlew.bat test` 只在 **recipes** 支付；census 与 closeout-seals 的 `gradle_tasks` 为空。`verify.py` 调用 Gradle 时固定 `--rerun-tasks --no-daemon --max-workers=1`。
- `integration --profile` 对 listed Python modules 一次跑完（`--suite modules`），不按文件走 `affected` glob；`dev` 仍按脏路径 `affected`。
- `python tools/verify.py dev` 默认吃整个 dirty tree。文档扫应用 `--path` 限定 docs；不要让未认领的 `src/generated` loot / `hs_err_*.log` 把 docs 扫打成 unmatched。

### 4.3 同 host 多波次

Production policy identity 必须是：

```text
(target_map, publication_group)
```

不能继续假设一个 target 永远只有一张 wave。每张波次独立测量，显式声明稳定 group；
文件路径不是 runtime policy。多个 group 在同一 target 下必须原子 reload、确定性
enumerate/lookup，并分别记录 cache 后按 compact-load policy 聚合。

#### 4.3.1 注意事项：卡号不是运行时类型（待重构）

签发 epoch 只应出现在 production lock、census、topology、seal 和 `wave_id`。
它们不是机器种类、配方格式或校验信封。

现状把卡号漏进了生成器和运行时：历史配方路径、按卡号复制的 compile 分支、
consume-identity 特例。换人读会把卡号当成类型。

Bath 是已发生的例子。同一台 host 上叠了两套信封：青铜化学 1×4 / 1×1（4000/8000 mB），
与 GT6 面板 6 物出 / 3 液出。remainder compact 走后者；青铜化学专用配方仍走前者。
`isBathRemainderCompactRecipe` 才是类型名。代表关系必须按 live host 槽与罐筛选，
不能按青铜信封或组内第一条碰运气。

重构方向（未开工；closeout seal 钉了路径，不在普通 recipe wave 里顺手改）：

```text
runtime / compile 主键 = host + cohort + representation
  例：bath / remainder / exact_multi
envelope class          = bronze_chemical | gt6_panel
card id                 = lock / census / seal / wave_id only
```

下一张卡（含 Mixer 新切片）不得再新增按卡号的运行时类型。新豁免、
新 compile 分支和新配方路径用 cohort / envelope，不用签发编号。

### 4.4 Query-addressable shard

Shard key 必须来自当前 query 可直接提取的 index signature，例如：

```text
target_map
+ primary item/tag/fluid identity
+ component/config identity
+ query-visible input shape
```

Router 先从倒排索引得到相关 shards，再执行 relation matcher。按 stable id hash、文件序号
或“每 128 rows”盲切都不合格，因为 query 无法选择 shard，最终仍会全扫。

默认 deterministic builder：

1. 把每条 relation 的 consume/preserve/catalyst item、tag、component 与 fluid operand
   规范成 index keys；
2. 在 group 内计算 posting frequency；
3. 按最少 posting、最短 tuple、canonical lexical tie-break，选择一到两个 query-visible
   keys 作为该 relation 的 `route_key`；
4. `shard_id = hash(target_map, publication_group, routing_schema_version, route_key)`；
5. query 从自己拥有的 operands 生成受限 single/pair key tuples，直接查 shard index，
   合并后按 stable id 去重；
6. builder 若不能让每个 routed posting 低于 hard ceiling，必须细化可表达的 compound
   key 或 fail closed。

这个算法允许每条 relation 只归属一个 primary shard，同时 query 不必预先知道哪一个
operand 被选作 primary：它只枚举自己已经持有的有界 key tuples。

Priority、shadow order 与 consume/preserve semantics 属于 candidate evaluation/order，
不是 query-visible routing key；不得为提高选择性把 query 无法知道的 recipe metadata
塞进 `route_key`。

Broad tag、缺失 index adapter 或 fallback relation 必须进入显式 overflow shard。Overflow
超过 candidate hard ceiling 时 publication fail closed；不得退回全 target scan。

### 4.5 Representation class

每个 family 在 R0 固定一种表示：

- singleton：exact relation；
- 小型 combinatorial：exact multi-relation + global shadow order；
- 大型 material/template expansion：parameterized template + slots；
- 无法安全参数化的异常项：独立 blocked/overflow，不静默 exact bulk expansion。

Parameterized 不减少 fidelity 分母。Builder/test 必须能枚举或等价证明全部 source rows；
它只减少 authored/runtime expansion 与 lookup fan-out。

### 4.6 Source Pack 与新来源

Source identity 至少包含：

```text
source_system
source_revision
source_recipe_identity
semantic_family_identity
```

GT6、未来 GT6U 与 design rows 使用独立 Source Pack manifest。新来源不得合并进历史
compact source 或追溯改变旧 selection hash。

## 5. Equivalence、玩家路径与机器执行

### 5.1 三层等价

1. Python：source → generated 全字段、全 relation；
2. Java：真实 generated root 经过 codec/provider；
3. Runtime：server / dedicated client / EMI / reload 集合与 fingerprint 一致。

禁止 `relations[0]` 特例。Duplicate authored/stable/logical identity 与未声明 shadow
全部 fail closed。

Shard 额外验证：

- relation membership 无丢失、无未声明重复；
- query router 返回的 shard 集合包含正确 relation；
- 不相关 query 不进入该 shard；
- 跨 shard 的 priority / shadow order 与未切片语义相同；
- aggregate manifest root 与 generated/runtime membership 一致。

### 5.2 玩家路径

Current typed closure 是 opening input reachability 权威。每张生产波次固定三层：

```text
B0 = previous typed closure without current production/support
B1 = B0 + production-lock-owned minimal support
B2 = B1 + production-lock recipes
```

Support 的输入必须在加入前的 closure 可达，且属于 production lock 的反向依赖切片；
target recipe 不得自证输入。每条 production relation 要求 B1 输入可达、输出已注册并
进入 B2。

typed closure 扫描必须包含已关闭 compact wave 的 generated root（当前
assembler compact / roaster compact），并解析
`cruciblecraft:compact_gt_recipe_family` 的每条 relation。当前卡的 generated tree
与 support generated tree 不得进入 B0，避免 support 自引用。

以下都不能单独证明玩家可达：

- namespace whitelist；
- source 中历史 `reachable` 字段；
- item/fluid 已注册；
- GameTest 注入输入；
- EMI 能显示 recipe。

每条 relation 必须记录 consume/preserve/catalyst、fluid source、output downstream 或
terminal use。新增 recovery、packing、worldgen 或 form gate 时，必须声明 provenance、
owner、source fingerprint，并计入 publication/load delta。Support emitter 不得丢弃
`count <= 0` 输入；它们是 preserve/catalyst。Unpack 走 unboxinator dump，不得用
boxinator 冒充。

`source_derived_alias` 在没有语义等价证明时 fail-closed。Hosted ore、状态物
（Resin/Comb/Goo/Egg）、depleted fuel rod、Black Sand 等有损折叠不得靠别名 id
进入玩家路径。无法用有界 support 闭合时，正式撤回并重签生产集合；完整 catalog
可以保留为隔离 load fixture，但不得继续扣 family gap。

材料形态注册门禁（`material_registration_gate.json` + `MaterialRegistrationGate`）
保持 fail closed：`materials[id]` ⊆ 该材料 factual forms ∪ 已声明 extras。Extras 只能来自
本卡 `tools/waves/<slug>/required_forms.json`、前序 wave 的 required forms，或 source-backed
acquisition overlay。不得把 Java 门禁改成“gate 里写了就算数”，也不得为过门禁而清空
`exclude_prefixes`。

Overlay 按材料逐条写入，禁止按 `octuple_cable` 一类共享形态对整表 `replace_all`。
`exclude_prefixes` 里的形态默认不注册 CrucibleCraft 物品。当前 1.21.1 原版已有对应物
的形态走 vanilla item id（铁/金 nugget），不得再注册 `cruciblecraft:{id}/{form}`。
1.21.1 没有 `minecraft:copper_nugget`，铜 nugget 与锡/铅一样列入本卡 required forms
并注册 CC 物品。没有 vanilla 对应物、且配方真正消耗或产出该形态时，才把它列入 extras。
全量 `build_gt6_material_form_gate.py --write` 若因体积无法作为日常入口，
用 `--check --reference-only` 加本卡 overlay；不得手改 `materials[]` 把 extras 喷到未
声明材料上。门禁失败与“item output chance 个数对不上”是两类问题：前者是目录能否启动，
后者是未注册输出在 codec 里被丢掉。

### 5.3 机器执行

对 data-driven variants 验证 energy window、item/fluid capacity、catalyst/preserve、
成功与 reject path。每条 relation 至少被 data-driven harness 覆盖；isolated GameTest
选择代表关系、reload、stable id 与 EMI，而不是用少量样本代替全 relation harness。
代表必须按 **live host 槽与罐** 筛选，不能按青铜化学信封或组内第一条碰运气。共享
Bath 的青铜信封与 GT6 remainder 分界见 §4.3.1。

## 6. Receipt 与 clean-checkout 规则

### 6.1 GameTest receipt

```text
run-wave-<slug>/                 # gitignored scratch，不是证据
tools/waves/<slug>/gametest.log  # committed UTF-8 LF evidence
tools/waves/<slug>/gametest_receipt.json
```

`--write --from-log` 接受平台日志并规范化 committed log，再生成 receipt。`--check` 必须：

- read-only，不启动 server；
- 在无 run directory 的 clean checkout 通过；
- 重新解析 committed log；
- 校验 command、namespace、test ids、passed/failed、skip-is-not-pass；
- 校验 log fingerprint、dependency manifest、publication-group/shard aggregate root 与
  bound trees；
- 缺失、失败、旧 schema 或依赖漂移时 fail。

收据指向被 `.gitignore` 排除的日志、只对比 receipt 内两个自保存 hash、或只搜索 Java
marker 都是无效证据。

### 6.2 证据生命周期

Active card 或 closing currentness 中，下列变化至少使 GameTest receipt 失效：

- test ids/namespace/Gradle run config；
- recipe generated/support tree；
- publication-group/shard manifest 或 router schema；
- codec/provider/loader/matcher；
- machine/energy/capacity；
- material/prefix/registration gate；
- dependency manifest schema。

Load receipt/decision 同理必须绑定实际 measurement artifact、policy、work set、opening
closing 和 runtime dependency manifest。

## 7. Load 规则

### 7.1 Policy-before-measure

每张卡先冻结 immediate / on-demand / hybrid 候选、cache、hybrid boundary 和 tie-break，
再运行实际 workload。禁止复制其他 host 的 eager/lazy/cache 比例。

### 7.2 两类场景

必须同时测：

1. card-only：隔离本卡增量，便于比较策略；
2. integrated current：现有 eager recipes、所有 compact groups、真实 target index 与
   前序 policies，防止增量测试掩盖累计回归。

测量覆盖 server、dedicated client、reload/index、enumeration、lookup/reject、sync、
retained memory、allocation。使用多样本 p50/p95；pending 不得填 0。

Lookup 证据同时记录：

- 每个 shard 的 relation count 与 routed candidate p50/p95/max；
- overflow shard candidate；
- 每个 publication group 的 cache/retained/sync；
- target-level integrated candidate 与总 reload/index；
- shard count 增长对 router 自身的成本。

Card 的 source-row 总数可以大于 200，但任何 query 的 routed candidate interval 仍必须
满足 current compact-load hard ceiling。

### 7.3 Winner 与 ledger

```text
correctness/equivalence
  -> all hard ceilings
  -> cumulative soft-budget interpretation
  -> predeclared tie-break
```

forward-v2 解释是：非 authored hard ceiling 淘汰；`datapack_authored_entries`
始终 REPORT_ONLY；其余 soft 超线只警告、不淘汰。共用实现是
`tools/forward_v2_budget_decision.py`。历史 builder 与 artifacts 保持冻结。
不使用事后 composite score。compact-load 各轴按各自 `sum`、`sum_interval` 或
`max_interval` 聚合，不能机械全相加。

Publication delta 分开列出 compact 与 support：

- authored datapack entries；
- eager publication rows；
- lazy logical rows；
- per-group cache；
- card-level runtime axes。

Support GT recipes、ore-chain、crafting 或 worldgen 不因“只是玩家路径修复”而漏记。

## 8. Census、topology 与 READY

每个 family identity 明确 opening/closing：

```text
planned / incomplete / fidelity / pending
  -> implemented / closed / retained fidelity / measured
```

只有全部 required identities 完成才扣 gap。Closing artifacts 必须满足：

```text
census_delta.status = CENSUS_DELTA_READY
remaining_recipe_gap = prior closing - complete_family_count
card_topology.next_unassigned = true
preassigned_host = false
preassigned_family_ids = false
unique_active_card = null
readiness.status = WAVE_READY
readiness.failed_gates = []
```

关闭后：

- active plan 移到 `card-plans/closed/`；
- 新增 stage archive 与 work log；
- 更新 history index 和 roadmap；
- 工作日志记录实际命令、test count、GameTest ids、日志/receipt hash、profiles 与未认领
  的 scope-external debt。

## 9. 验证矩阵

每张 recipe wave 至少：

碰过 §4.2.1 共享面时，先付清历史 pin，再跑下面的矩阵。矩阵本身按顺序、单进程执行。

```powershell
python -m unittest discover -s tools/tests -p "test_build_recipe_bulk.py"
.\gradlew.bat test --no-daemon
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile census
python tools/verify.py integration --profile closeout-seals
# 或闭卡组合（顶层 Gradle/GameTest 来自 recipes；Python 为 listed modules 一次，Gradle --max-workers=1）：
python tools/verify.py integration --profile card-closeout
.\gradlew.bat runGameTestServer -PwaveRecipes=<slug> --no-daemon
python tools/build_recipe_bulk.py gametest-receipt --wave <slug> --write --from-log <fresh-log>
python tools/build_recipe_bulk.py gametest-receipt --wave <slug> --check
```

按实际 owned paths 追加 materials/worldgen/machines/presentation 等 profile。一个 profile
通过不能推导其他 profile 通过；scope-external 已登记债务不得写成 PASS，也不得借机
加 bypass。

闭卡前执行 clean-checkout currentness：无本地 run dir、无 untracked evidence、普通
`--check` 不依赖 GT6 dump。Full replay closing receipt 在有 pinned dump 的环境取得。

## 10. 常见失败模式

- 只报 family 数，不报 source rows；
- template 存在就扣 gap；
- source dump skip 被当成 pass；
- namespace/注册状态冒充玩家路径；
- GameTest 注入冒充生存获得；
- receipt 依赖 ignored run log；
- receipt 未绑定 live runtime dependencies；
- 新卡覆盖同 target 的旧 production policy；
- 把签发卡号当成运行时类型（新卡号谓词、新编号配方路径当信封）；
- GameTest 代表按组内第一条或青铜化学信封挑选，而不是 live host 槽/罐；
- 把 shard 当成 Card，导致每 100–200 rows 一套人工收据；
- 按文件序号或 stable-id hash 分 shard，query 最终仍全扫；
- 只验证 shard hash，不验证全部 logical relations；
- harness 只读 `relations[0]`；
- support recipes 未进入 authored/eager delta；
- cumulative cache 被覆盖而不是聚合；
- card-only load 通过就忽略 integrated current regression；
- 为让 READY 变绿手改旧 hash、提高 hard ceiling 或 zero-fill pending；
- 改共享 loader/codec/projection/datagen 后靠第一轮 recipes 发现历史 pin 漂移；
- 只跑 isolated GameTest，跳过全量 JUnit 里的 datagen tag/语言断言；
- recipes 与 census 并行写同一批 `tools/waves/<slug>/*.json`；
- 为通过历史冻结测试而重开 census id freeze 或改写 write-once snapshot。

出现上述情况时撤回 READY，修复后从原始 evidence 重建，不在叙述中把旧失败改写成“当时
已通过”。

## 11. GT6U 与后续 Source Pack

新来源按 append-only census intake 处理：

1. 与现有 relation 完全相同：增加 provenance alias，不增加 gap；
2. 同 semantic family 的新增 relation：建立新的 source contribution / extension
   identity；旧 family 保留 `closed_at_revision`；
3. 修改/覆盖旧行为：建立显式 compatibility overlay、precedence 与 migration gate；
4. 是否进入当前 1.x gap：由新 census delta 明确决定，不追溯改写旧 gap。

Closed receipt 证明“该 source revision 在当时 runtime ABI 下关闭”。新增 GT6U recipe
tree 不要求重签已关闭波；核心 runtime ABI 变化走集中 compatibility regression /
migration gate。Dependency manifest 绑定相关 Source Pack、publication groups 与 runtime
ABI，不绑定未来整个全局 recipe tree。

因此“以后新增 GT6U 配方”不是自动历史债。只有旧计划曾承诺覆盖它，或新代码破坏旧
publication group 且没有 migration gate，才形成债务。

## 12. Partition 之后与 Gap = 0

owner-partition 已把剩余 ordinary families 划入明确 owner，并将经 family-atomic
evidence 证明的 Smelter recovery 移入 `later:recycling`。reclassification 不等于
recipe completion。后续 storage 与 bounded wave 必须从当时 closing artifact
重算 remaining gap。Circuit 映射、unique 残差族、gap `partial_family_count=0`
都不是 closure。此后顺序是：

```text
gap partition
  -> snapshot/inventory/classifier fidelity gate
  -> owner-partition gate
  -> smelter/stone
  -> machine-tier extensibility gate
  -> storage bundle
  -> block-object
  -> bath/mte
  -> bath/remainder
  -> closeout-verification repair
  -> bath/identity
  -> bath/tiny-purified
  -> smelter/ordinary-closure
  -> mixer/ordinary-closure
  -> ordinary-wave closeout-integrity repair
  -> ordinary-remainder shared operand foundation
  -> drying/ordinary-closure
  -> electrolyzer/ordinary-closure
  -> centrifuge/ordinary-closure
  -> autoclave/ordinary-closure
  -> compressor/ordinary-closure
  -> recipe-portfolio/ordinary-remainder-closure
  -> recycling/deferred-ordinary-runtime
  -> portfolio/one-x-joint-exit
  -> portfolio/source-capability-map
  -> tracks chosen from the capability map
  -> nuclear only if that map assigns it an owner
```

Smelter stone 已关闭 407 singleton families。Drying singleton 以及其它 Smelter
unmapped/object/form/B0/multi-axis family 不得混入或回填该波。

Storage 不再要求 current execution gap 先到 0。Storage 28/624 + logistics 1/1
已关闭（不计 ordinary recipe completion）。block-object、Bath 各波与
Smelter / Mixer ordinary-closure 与 Ordinary 尾账五 host 已关闭。closeout-integrity
repair 修复了两波 load pending 与 seal/readiness 矛盾。current execution gap = 0。
deferred ordinary ledger 已关闭：1,817 Smelter MTE recovery complete + 28
independent post-1.x scope；hanging `later:*` = 0。语义命名残留见
[semantic-naming.md](semantic-naming.md)，不阻塞后继内容波。

execution gap = 0 不等于 1.x joint exit。`recycling/deferred-ordinary-runtime` 已
`DEFERRED_ORDINARY_RUNTIME_READY`。`portfolio/one-x-joint-exit` 已
`ONE_X_JOINT_EXIT_READY`：六条退出条件 GREEN，load 口径 A，capability-map
seed 交给已关闭的 `portfolio/source-capability-map`。对照检查在该 program 的
inventory child（113 行），不是 1.x seed。`SOURCE_CAPABILITY_MAP_READY`。
growth-order 指定的 `next_major = portfolio/generic-recipe-generator` 已签发为
通用 Source Pack 导入器计划，尚未实现且没有 production lock。
1.x 已认领的关闭工作仍然成立，但不把它写成 GT6 终点。核能 Track C 保持
`started = false`。`later:*` 四条 combinatorial 已并入 next_major 建议；7 条
nuclear 与 28 条 recycling 仍独立。`unique_active_wave = null`。
