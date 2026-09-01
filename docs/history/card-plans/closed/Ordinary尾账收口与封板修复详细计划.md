# Ordinary 尾账收口与封板修复详细计划

> 计划 slug：`recipe-portfolio/ordinary-remainder-closure`
> 状态：已关闭（2026-09-01）。本文件位于 `card-plans/closed/`。
> `ORDINARY_REMAINDER_CLOSURE_READY`：current execution gap = 0；completion 308 +
> reclass 26 = 334；deferred recycling = 1,843；deferred ledger total = 1,845
> （1,819 inherited recycling + 24 autoclave `later:recycling` + centrifuge
> `#0010` envelope + `#0207` cross_mod）。`unique_active_wave = null`，
> `next_unassigned = true`。下一张 major program 默认
> `recycling/deferred-ordinary-runtime`，不进入 1.x joint exit，不启动核能。
> 性质：串行 program card；包含一个零 completion 的 closeout-integrity repair gate、
> 一个共享 operand/acquisition foundation 和五个 host-level ordinary recipe waves
> 唯一 opening：`mixer/ordinary-closure` 当前 closing；current execution gap = 334
> opening ordinary relations：2,047
> opening deferred recycling：1,819
> 目标：以 completion 或有逐族证据的 reclassification 将 current execution gap 清零
> 明确不包含：deferred recycling runtime、剩余语义命名、核能、玩家发行、RC/GA
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 前置 program：
> [Smelter / Mixer 收口与语义命名迁移](Smelter-Mixer收口与语义命名迁移详细计划.md)
> 签发层级：当前工作树；本次计划签发不创建 Git commit

权威流程仍是
[Ordinary Recipe Wave 流程与规范](../../../current/recipe-wave-workflow.md)。本计划增加的
约束是：

1. 先修复 semantic wave readiness 可以在 `LOAD_PENDING_MEASUREMENT` 时错误产生
   `WAVE_READY/SEALED` 的封板漏洞；
2. 剩余 334-family final remainder 采用一张 program card、五个串行 host child；
3. 每个 child 的 `N<300` 例外来自同一个固定、穷尽式 final-remainder denominator，
   不得用其它 host filler；
4. semantic naming 已转入
   [semantic-naming.md](../../../current/semantic-naming.md)，本计划不继续大规模改名；
5. 新 artifact 仍使用 semantic slug，不恢复里程碑编号。

Markdown 不是 production authority。Repair 与每个 child 必须有机器可读 work set、
readiness、measurement、census/topology 和 seal；recipe child 还必须有 source receipt、
candidate、family-atomic lock、operand disposition、publication groups、shards、
equivalence、player path 与真实 GameTest。

---

## 0. Program contract

### 0.1 串行顺序

```text
mixer/ordinary-closure current closing
  -> ordinary-wave/closeout-integrity-repair
  -> ordinary-remainder/operand-foundation
  -> drying/ordinary-closure
  -> electrolyzer/ordinary-closure
  -> centrifuge/ordinary-closure
  -> autoclave/ordinary-closure
  -> compressor/ordinary-closure
  -> recipe-portfolio/ordinary-remainder-closure
```

同一时刻只有一个 `unique_active_wave`。后继只能读取前一 child 的 sealed closing。
Foundation 可以解除多个 host 的共同 blocker，但不拥有 recipe families，不能产生
completion delta。

### 0.2 Opening 账本

当前机器可读事实：

```text
Smelter completion             = 338
Smelter reclassification       = 14
Smelter closing gap            = 997

Mixer completion               = 648
Mixer reclassification         = 15
Mixer closing gap              = 334

current execution gap          = 334
current ordinary relations     = 2047
deferred recycling             = 1819
partial families               = 0
next_unassigned                = true
```

334-family 候选按 frozen owner/catalog 重建的 opening 预期：

```text
centrifuge    128 families /  211 relations
autoclave      60 families /  373 relations
compressor     56 families / 1284 relations
electrolyzer   46 families /   94 relations
drying         44 families /   85 relations
total         334 families / 2047 relations
```

这些数字是 R0 对照，不是 production lock。Live registries、已关闭 Smelter/Mixer
reclassification 和 source membership 漂移必须在 global R0 中解释。

### 0.3 Program 完成定义

```text
closeout repair READY
shared operand foundation READY
five host children WAVE_READY
all locked relations equivalent and executable
partial_family_count = 0
remaining_recipe_gap = 0
every reclassified family has future_owner + recheck condition
deferred ledger recomputed, not silently discarded
unique_active_wave = null
next_unassigned = true
ORDINARY_REMAINDER_CLOSURE_READY
```

---

## 1. Closeout integrity repair

### 1.1 已确认的漏洞

Smelter 与 Mixer 目前同时存在互相矛盾的机器可读状态：

```text
readiness.status   = WAVE_READY
closeout seal      = SEALED
census.load.status = LOAD_PENDING_MEASUREMENT
```

两波均缺少至少以下 integrated measurements：

```text
client_reload_ms
client_index_ms
reload_transient_allocation_bytes
lookup_allocation_bytes_per_operation
retained_memory_bytes
```

当前 `tools/recipe_bulk/ordinary_wave.py` 的 topology/readiness 判定只看
`unique_active_wave` 或 `complete`，没有把 load verdict、eliminate reasons 与缺失 measurement
纳入 `WAVE_READY`。这是 closeout derivation bug，不是 Smelter/Mixer family 或 GameTest
失败。

### 1.2 Repair 边界

Repair：

- `owns_families = 0`；
- `completion_delta = 0`；
- `reclassification_delta = 0`；
- 不改 Smelter/Mixer source、production lock、generated recipes、stable IDs、groups 或
  relation counts；
- 不改 frozen T38–T49 seals、v2 identity ledger 或 runtime manifest；
- 只修 semantic wave readiness/topology/seal derivation，并补齐当前实测。

### 1.3 Readiness 单一判定

新增共享 derivation：

```text
wave_ready =
  source_current
  AND production_lock_current
  AND equivalence_current
  AND player_path_current
  AND gametest_receipt_current
  AND load.status == LOAD_READY
  AND load.decision.eliminate_reasons == []
  AND census_current
  AND partial_family_count == 0
  AND topology_consistent
```

任何 measurement 为 `null`、`PENDING_MEASUREMENT`、`measurement_unavailable`、
missing-event 或 zero-filled 时：

```text
readiness != WAVE_READY
topology != WAVE_READY
closeout seal write = forbidden
```

`closeout_seal.py --write --wave` 必须重新计算 readiness，不能只相信 JSON 中已有的
`status` 字符串。

### 1.4 Measurement repair

分别对以下实际组合重测，不用 card sum 或 opening 数据填充：

```text
historical compact groups + Smelter groups
historical compact groups + Smelter groups + Mixer groups
```

两组都采集：

- dedicated server reload/index；
- dedicated client reload/index；
- reload window transient allocation；
- stable epoch lookup allocation；
- controlled-GC retained memory；
- sync payload；
- lookup p50/p95/candidates；
- concrete eager、compact eager、lazy logical、cache telemetry。

Count telemetry 超过旧 21,000 / 56,000 / 4,096 只报告 `UNVERIFIED_SCALE`；真实性能
measurement 与 correctness gates 继续阻塞。

### 1.5 Seal repair

现有 semantic seals 不当作 frozen history。Repair 必须：

1. 记录旧 seal 全文件 SHA；
2. 保存为 `pre_repair_closeout_seal.json`，只作缺陷证据；
3. 补测后重建 census/readiness/topology；
4. 生成 schema v2 semantic seal，写入 `supersedes_sha256`；
5. 证明 production lock/generated tree hash 与 repair 前一致。

Repair closing：

```text
smelter/ordinary-closure = WAVE_READY
mixer/ordinary-closure   = WAVE_READY
load pending axes        = 0
family/relation delta    = 0
opening gap              = 334
ORDINARY_WAVE_CLOSEOUT_INTEGRITY_READY
```

Repair 未关闭前，不允许签发 Drying production lock。

---

## 2. Global remainder R0

### 2.1 Source universe

从 repaired Mixer closing 重建 334-family universe：

```text
tools/t42_remaining_catalog.json
tools/t42_owner_disposition_lock.json
tools/t42_family_operand_snapshot.json
tools/t42_blocker_overlay.json        # diagnosis only
all closed production locks
all semantic reclassification ledgers
gt6_dump/gt6_recipe_dump/maps/gt.recipe.<host>.json
```

必须证明：

```text
every remaining family appears exactly once
sum(families by host) = 334
sum(source relations) = 2047
no Smelter/Mixer/Bath family remains in execution gap
no deferred family re-enters execution gap without explicit recheck
no completed family appears in candidate
```

### 2.2 Opening blocker 对照

Frozen overlay 预期，仅用于发现 live drift：

```text
primary needs_unique_block_or_mte ≈ 315 families
primary needs_prefix_or_molten    ≈ 18
primary current_closure_ready     ≈ 1

coordination/multi_axis           ≈ 150
acquisition/b0                    ≈ 96
identity_mapping/unmapped         ≈ 25
recycling/evidence_needed         ≈ 24
material expression              ≈ 18
object expression                ≈ 20
recipe_wave/drying               ≈ 1
```

Secondary blockers 可重叠，预期包括：

```text
b0_unreachable_inputs ≈ 217
unique_objects        ≈ 122
unmapped_operands     ≈ 111
residual_not_ready    ≈ 97
acquisition_open      ≈ 96
unproven_vanilla      ≈ 71
missing_forms         ≈ 16
missing_fluids        ≈ 13
```

R0 必须重新运行 current projection。不得因为 frozen overlay 名字是
`needs_unique_block_or_mte` 就假设每家都真缺 MTE。

### 2.3 Disposition

每个 operand/family 只能进入：

```text
proven_equivalent
needs_current_expression
phase_deferred
unsupported
```

缺 CrucibleCraft identity/form/fluid/B0 属于本 program 的 foundation 工作，不得直接
reclassify。只有以下情况允许 deferred：

- 来源语义确属 MTE dismantling/recycling runtime；
- family 依赖明确的外部 mod 行为；
- source identity 无法审计且不存在无损表达；
- 当前机器 execution envelope 无法表达且扩 envelope 会改变产品语义。

每条 reclassification 必须记录：

```text
family_id
host
relation_count
source_row_sha256
reason
future_owner
recheck_condition
```

---

## 3. Shared operand and acquisition foundation

### 3.1 Foundation slug

```text
ordinary-remainder/operand-foundation
```

它在五个 host lock 前统一解决跨 host 能力，避免每张卡重复注册同一 object。

### 3.2 Object identity

建立 typed catalog，至少覆盖 live R0 中：

- block / meta object；
- multiitem；
- MTE item identity；
- GT prefix object；
- vanilla object proof；
- material form；
- mapped fluid。

禁止：

- meta 区间算术；
- 名字相似但行为不同的 alias；
- 把 unknown object 映射成占位物品；
- 由第一个 host builder 私自写全局 registration gate。

Material forms 继续由 material form authority 汇总。Object/fluid catalog 必须有 source
revision、identity kind、runtime ID、fidelity 与 owner。

### 3.3 B0/B1 acquisition

对约 96 个 `acquisition/b0` owner 和 live `b0_unreachable_inputs` 建共享玩家路径：

```text
B0 = 当前 survival sources
B1 = B0 + foundation support
B2 = B1 + current host production relations
```

Support 必须是实际 recipe、loot、worldgen 或可操作机器路径，不能是 declaration-only token。
空输入 worldgen 必须有放置、掉落和可重复取得证据。

### 3.4 Foundation exit gate

```text
global R0 current
typed object/fluid/form catalog current
all shared support generated
registry and acquisition tests pass
no family completion claimed
Drying candidate recomputed from foundation closing
ORDINARY_REMAINDER_OPERAND_FOUNDATION_READY
```

---

## 4. Host child contract

每个 child 使用同一固定流程：

```text
previous child READY
  -> host source freeze
  -> live candidate
  -> family-atomic production lock
  -> exact/exact_multi representation
  -> semantic publication groups
  -> query-addressable shards
  -> source/compiled/runtime equivalence
  -> player path + real machine GameTest
  -> actual card-only + integrated load
  -> census/topology/readiness/seal
```

### 4.1 `N<300` final-remainder 例外

五个 host 均不足 300 families，但例外合法：

```text
exception_kind = final_execution_gap_host_partition
program_denominator = 334 complete remaining families
partition_rule = target host
padding_forbidden = true
minimum_N = all production-eligible families for that host
```

例外必须出现在 program R0、child work set、production lock、census 和 readiness。
不能从别的 host 拿 ready family 凑数，也不能留下无理由的同 host family。

### 4.2 Representation

默认 compile-time `exact` / `exact_multi`。只有 source family 有稳定、有限参数空间且
runtime/template equivalence 已独立验收时才允许 parameterized。不得为降低 authored 文件数
改变 logical relation denominator。

### 4.3 Publication identity

```text
cruciblecraft:<host>/ordinary_closure/<cohort>
data/cruciblecraft/recipe/<host>/ordinary_closure/<cohort>/
```

Group 由 source/object/acquisition/fluid semantics 划分，不按 child 顺序、文件数或 row
count 切分。Shard key 必须能由 lookup query 直接计算。

---

## 5. Drying child

### 5.1 Opening

```text
slug      = drying/ordinary-closure
families  ≈ 44
relations ≈ 85
```

Frozen owner 对照：

```text
coordination/multi_axis       ≈ 20
object_expression/multiitem   ≈ 10
acquisition/b0                ≈ 8
recipe_wave/drying            = 1 known ready
other form/object owners      = remainder
```

### 5.2 Scope

- 以 `gt.recipe.drying#0149` 作为已知 live-ready receipt 对照，不代表其它 43 家自动 ready；
- 关闭 multiitem、容器、流体、B0 与 machine envelope；
- 每个 exact_multi family 全 relation 完成后才扣 gap；
- 真实 Drying execution 覆盖 item/fluid conservation、duration、energy 与 output handling。

### 5.3 Closing

目标是关闭或有证据地重分类 Drying 全 host remainder。Closing gap 由 334 live 重算，
不预写为 `334 - 44`。

---

## 6. Electrolyzer child

### 6.1 Opening

```text
slug      = electrolyzer/ordinary-closure
families  ≈ 46
relations ≈ 94
```

Frozen owner 对照：

```text
coordination/multi_axis      ≈ 39
acquisition/b0               ≈ 6
material_expression/fluid    ≈ 1
```

### 6.2 Scope

- 审计 preserved catalyst、cell/container、fluid identity 与电力 envelope；
- 复用已有 Electrolyzer compact publication，但不得扩写已关闭 group；
- 新 group 与历史 T5/support recipes 做显式 dedup；
- 每个 fluid input/output 必须有注册、容量和玩家路径。

---

## 7. Centrifuge child

### 7.1 Opening

```text
slug      = centrifuge/ordinary-closure
families  ≈ 128
relations ≈ 211
```

Frozen owner 对照：

```text
acquisition/b0                ≈ 72
identity_mapping/unmapped     ≈ 25
coordination/multi_axis       ≈ 16
form/fluid/block/multiitem    = remainder
```

### 7.2 Scope

- withdrawn broad Centrifuge fixture 只能作为 router/codec 参考，不是 production source；
- production lock 必须从 current 128-family remainder 重建；
- 与历史 compact、T39 support 和 Mixer/Smelter support 做 deterministic dedup；
- sands/food/multiitem/block identities 按 foundation catalog，无损映射；
- B0 为主门，不能用“物品已注册”代替 survival acquisition。

---

## 8. Autoclave child

### 8.1 Opening

```text
slug      = autoclave/ordinary-closure
families  ≈ 60
relations ≈ 373
```

Frozen owner 对照：

```text
coordination/multi_axis        ≈ 30
recycling/evidence_needed      ≈ 24
acquisition/b0                 ≈ 6
```

### 8.2 Recycling disposition

24 个 `recycling/evidence_needed` 不能按 owner 名称直接 deferred。R0 必须逐族判断：

- 如果来源是普通 Autoclave processing，完成 identity/B0 后进入 production；
- 如果来源确属 MTE dismantling/recovery 且依赖尚不存在的 recycling runtime，转入
  `later:recycling`；
- mixed family 不允许只发布普通 relations 后提前 completion。

Reclassification 会使 current gap 下降，但必须使 deferred ledger 同量增加。不得把它计入
Autoclave completion。

---

## 9. Compressor child

### 9.1 Opening

```text
slug      = compressor/ordinary-closure
families  ≈ 56
relations ≈ 1284
```

Frozen owner 对照：

```text
coordination/multi_axis      ≈ 45
material_expression/form     ≈ 7
acquisition/b0               ≈ 4
```

### 9.2 Why last

Compressor relation density 最高。放在最后可以：

- 使用四个前序 child 已闭合的 object/form/B0 能力；
- 在最大 integrated production mix 上完成最终 load measurement；
- 避免早期高 relation group 让其它 host 的 correctness blocker 难以定位。

所有 material-matrix relation 必须完成 full equivalence。不得只测 representative 后把
1,284 relations 全记 completion。

---

## 10. Load and publication gates

### 10.1 Opening telemetry

Repair 后以实测值冻结 opening。当前未补齐版本仅作对照：

```text
all eager published recipes     ≈ 37004
lazy logical rows               ≈ 55622
lazy cache ceiling rows         ≈ 1772
server reload ms                ≈ 5273
server index ms                 ≈ 88
sync bytes                      ≈ 8449197
```

Client、allocation 与 retained-memory 当前为空，不能用于后继 projection。

### 10.2 Count telemetry

以下是 extensibility telemetry，不再作为 runtime throw：

```text
concrete eager recipes
compact eager relations
lazy logical relations
cache ceiling
authored entries
```

超过 historical reference 产生 `UNVERIFIED_SCALE`，要求扩测，但不单独证明性能失败。

### 10.3 Blocking gates

每个 child 与最终 integrated closing 都阻塞于：

- correctness/invariants；
- server/client reload 与 index SLO；
- lookup p95/candidate/operation allocation；
- reload transient allocation；
- controlled-GC retained memory；
- sync payload；
- no pending/missing/zero-filled measurement。

真实性能必须来自当前 full mix。不得把 repair opening 或前一 child 指标复制到后一 child。

---

## 11. Accounting

每个 child closing：

```text
opening_execution_gap
candidate_family_count
candidate_relation_count
complete_family_count
completion_delta
reclassification_delta
partial_family_count
remaining_recipe_gap
deferred_ledger_count
publication_group_count
shard_count
load status
```

Program 算术目标：

```text
opening gap              = 334
completion + reclass     = 334
closing gap              = 0
partial families         = 0
opening deferred         = 1819
closing deferred         = 1819 + proven new deferred families
```

Closing gap 必须从 universe replay 得到，不能只用减法生成。若 family 消失、重复或跨 child，
program BLOCKED。

---

## 12. Artifacts

### 12.1 Repair

```text
tools/waves/ordinary-wave/closeout-integrity-repair/
  wave.json
  affected_seals.json
  pre_repair_hashes.json
  measurement_manifest.json
  smelter_integrated_measurements.json
  mixer_integrated_measurements.json
  readiness.json
  closeout_seal.json
```

### 12.2 Foundation

```text
tools/waves/ordinary-remainder/operand-foundation/
  work_set.json
  blocker_audit.json
  object_catalog.json
  required_forms.json
  fluid_mapping.json
  acquisition_manifest.json
  readiness.json
```

### 12.3 Host children

每个 `tools/waves/<host>/ordinary-closure/` 至少有：

```text
wave.json
source_pack_manifest.json
source.json
source_receipt.json
candidate_selection.json
operand_runtime_map.json
production_lock.json
publication_group_manifest.json
shard_manifest.json
equivalence.json
player_path.json
load_projection.json
integrated_measurements.json
gametest_receipt.json
census_delta.json
topology.json
readiness.json
closeout_seal.json
```

Generated roots：

```text
src/recipe_generated/resources/data/cruciblecraft/recipe/<host>/ordinary_closure/
src/recipe_support_generated/resources/data/cruciblecraft/recipe/<host>/ordinary_closure_support/
```

---

## 13. Verification

### 13.1 Repair

```text
python tools/build_ordinary_wave.py --check --wave smelter/ordinary-closure
python tools/build_ordinary_wave.py --check --wave mixer/ordinary-closure
python tools/verify.py integration --profile semantic-wave-closeout
python tools/closeout_seal.py --check --wave smelter/ordinary-closure
python tools/closeout_seal.py --check --wave mixer/ordinary-closure
```

### 13.2 Per child

```text
python tools/build_recipe_bulk.py compile --check --wave <host>/ordinary-closure
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile census-replay
python tools/verify.py integration --profile card-closeout
.\gradlew.bat runGameTestServer -PwaveRecipes=<host>/ordinary-closure --no-daemon
```

### 13.3 Program closeout

```text
.\gradlew.bat test --no-daemon
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile census-replay
python tools/verify.py integration --profile closeout-seals
python tools/verify.py integration --profile card-closeout
python tools/verify.py dev
```

正式 closeout 必须在固定 GT6 dump 上 full replay。日常 dump-unavailable check 只能证明
committed receipt current，不能替代 source replay。

---

## 14. Block and withdrawal rules

当前 child 遇到以下任一条件时 BLOCKED：

- repaired Mixer closing 不 current；
- source universe 不是 334/2,047 且 drift 无解释；
- family 被部分发布；
- operand 只能靠损失性 alias 或 placeholder 表达；
- B0/B1 support 是 declaration-only；
- group/shard lookup 需要全表扫描；
- equivalence 或 machine execution 失败；
- load 存在 pending/missing/zero-filled 或 blocking failure；
- readiness 与 census/load verdict 不一致；
- seal 未重算即可写入；
- 新 artifact 使用里程碑式编号命名。

撤回只撤当前 child，保留 source/candidate/selection hash 与原因。不得：

- 用其它 host family 填 N；
- 把 deferred 算 completion；
- 为过 hard gate 临时删 relations；
- 手改 status/hash 使 seal 变绿；
- 重写 frozen T38–T49 history；
- 顺手恢复 semantic naming 大迁移。

---

## 15. Program closeout 与后继

当五个 host closing 后：

```text
current execution gap = 0
ordinary remainder program = READY
deferred ordinary ledger >= 1819
semantic naming = deferred
unique_active_wave = null
next_unassigned = true
```

Execution gap 清零不等于 1.x exit。下一张 major program 默认是
`recycling/deferred-ordinary-runtime`：

- 重新审计 deferred 1,819+ families；
- 建立 MTE dismantling/recovery runtime；
- 按 source-backed recovery semantics 分批关闭；
- 无法纳入 1.x 的内容必须经过独立 post-1.x scope decision。

Deferred ledger 未关闭或未正式 scope decision 前，不进入 1.x joint exit，也不启动核能
source/physics census。

本计划关闭时新增语义 work log/stage archive，更新 roadmap、workflow、verification 与历史
索引。本计划不自动创建 Git commit；提交仍需用户明确授权。
