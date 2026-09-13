# 回收运行时与 Deferred 账本收口详细计划

> 计划 slug：`recycling/deferred-ordinary-runtime`
> 状态：已关闭（2026-09-02）。本文件位于 `card-plans/closed/`。
> `DEFERRED_ORDINARY_RUNTIME_READY`：current execution gap = 0；Smelter MTE
> recovery completion = 1,817；independent post-1.x scope = 28；deferred
> recycling = 0；deferred ledger total = 0；hanging `later:*` = 0。
> `unique_active_wave = null`，`next_unassigned = true`。后继默认 1.x joint
> exit gate，不进入核能，不预写 exit lock。
> 性质：串行 program card。先做零 completion 的 ledger 枚举，再建立 Smelter MTE
> 可持有 identity，再按 cohort 关闭 deferred ordinary ledger。
> opening current execution gap：0
> opening deferred recycling：1,843
> opening deferred ledger total：1,845
> 目标：每一条 deferred 项完成 recipe 关闭，或留下独立、明确的 post-1.x scope
> decision；`later:*` 不得无限期悬挂
> 明确不包含：语义命名大迁移、1.x joint exit、核能 census、玩家发行、RC/GA
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 前置 program：
> [Ordinary 尾账收口与封板修复](Ordinary尾账收口与封板修复详细计划.md)
> 签发层级：当前工作树；本次计划签发不创建 Git commit

## 读法修订（2026-09-12）

`recovery completion = 1,817` 是按 GT6 meta 冻结的回收关系，不证明需要
1,817 个永久独立 dummy item。后续 MTE 身份收口若把某 meta 精确折回 live
BlockItem，Smelter 关系输入必须随同指向 canonical live id，不能为保住 1,817
计数继续注册第二件铁锭物品；keep-distinct/in-place 行则保留现代 id。
关系完成也不豁免美术门：仍独立的可持有 MTE 在玩家完成前必须迁真实 GT6 art。
本节不改本卡历史 relation 数或关闭收据。

权威流程仍是
[Ordinary Recipe Wave 流程与规范](../../../current/recipe-wave-workflow.md)。本计划增加的
约束是：

1. inherited recycling 1,819 在尾账 closing 中 `enumerated = false`。本 program 的
   第一张 child 必须机器枚举全部 1,845 条，不能再引用未展开的 count；
2. 1,817 条已证明的 Smelter MTE → molten singleton 是主生产波。表示法是 builder
   从 recovery evidence 生成 compact `exact`，不是手写 1,817 个特例，也不是尚未实现的
   `ParameterizedSpec`；
3. Smelter `#1829` / `#1884` 与 Autoclave 24 条不是同一 operand 形状，必须独立
   cohort。Autoclave 24 条的 `later:recycling` 标签要在 R0 重新证明，不得照抄尾账
   理由；
4. centrifuge `#0010` 与 `#0207` 不是 recycling 生产波。它们走 envelope / cross-mod
   轨道：实现，或写下独立 post-1.x scope decision；
5. 语义命名已转入
   [semantic-naming.md](../../../current/semantic-naming.md)，本计划不继续大规模改名；
6. 新 artifact 使用 semantic slug，不签发里程碑编号。

Markdown 不是 production authority。每个 child 必须有机器可读 work set、readiness、
measurement、census/topology 和 seal。拥有 recipe 的 child 还必须有 source receipt、
candidate、family-atomic lock、operand disposition、publication groups、shards、
equivalence、player path 与真实 GameTest。

---

## 0. Program contract

### 0.1 串行顺序

```text
recipe-portfolio/ordinary-remainder-closure current closing
  -> recycling/deferred-ordinary-ledger-r0
  -> recycling/smelter-mte-identity
  -> smelter/deferred-recycling
  -> smelter/deferred-recycling-edge
  -> autoclave/deferred-recycling
  -> recycling/non-recycling-scope
  -> recycling/deferred-ordinary-runtime
```

同一时刻只有一个 `unique_active_wave`。后继只能读取前一 child 的 sealed closing。
R0 与 identity 可以解除后继 blocker，但不拥有 recipe families，不能产生
completion delta。

`recycling/non-recycling-scope` 可以在同一 child 内串行处理两条非回收项，也可以拆成
两个更小的 scope child。无论哪种写法，它们都不并入 Smelter 生产 lock。

### 0.2 Opening 账本

当前机器可读事实来自
[`tools/waves/recipe-portfolio/ordinary-remainder-closure/deferred_ledger.json`](../../../../tools/waves/recipe-portfolio/ordinary-remainder-closure/deferred_ledger.json)
与 [`tools/t42_owner_recovery_evidence.json`](../../../../tools/t42_owner_recovery_evidence.json)：

```text
current execution gap          = 0
deferred recycling             = 1843
deferred ledger total          = 1845
inherited recycling            = 1819   enumerated = false
proven new autoclave recycling = 24
later:execution_envelope       = 1      centrifuge#0010
later:cross_mod                = 1      centrifuge#0207
partial families               = 0
next_unassigned                = true   # 签发前；签发后改为 false
```

R0 对照，不是 production lock：

```text
smelter proven MTE recovery    1817 families / 1817 relations / 1817 unique metas
smelter evidence-incomplete       2 families /    2 relations
autoclave tagged recycling       24 families /   24 relations   eligible = 0
centrifuge envelope               1 family
centrifuge cross_mod              1 family
total deferred                 1845
```

1817 条全部是：

```text
host            = cruciblecraft:smelter
reason          = proven_consumed_mte_material_recovery
relation_count  = 1
input           = gregtech:gt.multitileentity@<unique meta>
output          = one already-registered molten fluid
output materials = 81 distinct CC materials
shape_blockers  = []
```

这两条 Smelter 边沿不是同一缺口：

```text
gt.recipe.smelter#1829
  input  = gregtech:gt.multitileentity@32075
  output = cruciblecraft:molten_alumina
  blockers = no_proven_material_identity_output,
             non_registered_material_fluid_output

gt.recipe.smelter#1884
  input  = gregtech:gt.multitileentity@32766
  output = minecraft:lava
  blockers = no_proven_material_identity_output,
             non_registered_material_fluid_output
```

Autoclave 24 条在 recovery evidence 中全部 `eligible_phase_deferred = false`。共同
blockers 是：

```text
additional_fluid_input
b0_unreachable_inputs
non_consumed_mte_input
non_mte_item_input
non_positive_mte_input
non_registered_material_item_output
```

样本输入是 `gregapi:gt.integrated_circuit@0` + `gregtech:gt.meta.dust@8298`，输出是
`cruciblecraft:water_distilled`。这不是 1817 条那种“消耗一台 MTE、吐一份熔融金属”
的形状。R0 必须重新判定它们是不是 recycling。

### 0.3 Program 完成定义

```text
ledger R0 READY and enumerated = true
smelter MTE identity catalog READY
1817 proven families WAVE_READY
smelter edge 2 closed or formally scoped
autoclave 24 closed, evidence-reclassified, or formally scoped
centrifuge#0010 implemented or post-1.x scoped
centrifuge#0207 implemented or post-1.x scoped
deferred ledger = 0
  OR every remaining item has an independent post-1.x scope decision
partial_family_count = 0
current execution gap stays 0 unless a recheck explicitly returns a family
unique_active_wave = null
next_unassigned = true
DEFERRED_ORDINARY_RUNTIME_READY
```

Completion 只计实际生成、运行并验收的 family。Reclassification 必须增加对应 ledger
或 scope-decision 记录，不能把 deferred 写成 completion。

本 program 关闭不等于 1.x joint exit，也不启动核能。

### 0.4 N<300 例外（必须在 R0 写明）

`smelter/deferred-recycling` 的 1,817 家满足默认 production lock。下列 child 不足
300，合法理由如下，不得用其它 host filler 凑数：

| child | owns families | 例外依据 |
| --- | ---: | --- |
| `recycling/deferred-ordinary-ledger-r0` | 0 | 基础设施：枚举与分桶 |
| `recycling/smelter-mte-identity` | 0 | 基础设施：身份 / 取得路径 |
| `smelter/deferred-recycling-edge` | 2 | 独立 evidence-incomplete cohort |
| `autoclave/deferred-recycling` | 24 | 不同 host 与不同 operand 形状 |
| `recycling/non-recycling-scope` | 0 或 2 | envelope / cross-mod，不是回收生产 |

---

## 1. Ledger R0

### 1.1 Slug 与边界

```text
slug            = recycling/deferred-ordinary-ledger-r0
owns_families   = 0
completion_delta = 0
reclassification_delta = 0 unless R0 proves a label is wrong
```

R0 不生成配方，不注册物品，不改已关闭 ordinary-closure 的 production lock 或
generated tree。

### 1.2 必须枚举的权威面

从下列权威面重建 1,845 条逐族名单，每条只出现一次：

```text
tools/waves/recipe-portfolio/ordinary-remainder-closure/deferred_ledger.json
tools/t42_owner_recovery_evidence.json
tools/t42_owner_disposition_lock.json
tools/waves/smelter/ordinary-closure/production_lock.json
tools/waves/autoclave/ordinary-closure/production_lock.json
tools/waves/centrifuge/ordinary-closure/production_lock.json
tools/waves/mixer/ordinary-closure/census_delta.json
gt6_dump/gt6_recipe_dump/maps/gt.recipe.{smelter,autoclave,centrifuge}.json
```

必须证明：

```text
enumerated recycling        = 1843
enumerated ledger total     = 1845
1817 + 2 + 24 + 1 + 1       = 1845
inherited 1819              = 1817 proven + 2 smelter edge
every family has host, template_key, relation hashes, future_owner,
                  recheck_condition, eligible flag, shape blockers
no completed ordinary family re-enters this ledger
no silent discard of inherited 1819
```

Closing artifact 至少包含：

```text
tools/waves/recycling/deferred-ordinary-ledger-r0/deferred_universe.json
tools/waves/recycling/deferred-ordinary-ledger-r0/cohort_partition.json
tools/waves/recycling/deferred-ordinary-ledger-r0/readiness.json
tools/waves/recycling/deferred-ordinary-ledger-r0/closeout_seal.json
```

`deferred_universe.json` 是后继唯一 opening 分母。之后禁止再写
`inherited_recycling.enumerated = false`。

### 1.3 分桶规则

每个 family 只能进入一个 cohort：

```text
smelter_proven_mte_recovery
smelter_recovery_edge
autoclave_tagged_recycling
centrifuge_execution_envelope
centrifuge_cross_mod
mislabeled_needs_reclass
```

`mislabeled_needs_reclass` 只用于 R0 能逐族证明 `future_owner` 写错的条目。纠正后
必须记录旧标签、新标签、证据 hash 与 recheck。不得为了让主波好看而把异形条目塞进
1817。

Autoclave 24 条的默认假设是：**标签待证，不是已证明的 MTE recovery**。若 R0 证明
它们是普通 Autoclave processing，转入 `mislabeled_needs_reclass`，由
`autoclave/deferred-recycling` 做 identity/B0 后生产或再次正式 deferred。若 R0 证明
它们确属另一种回收 runtime，仍作为独立 autoclave cohort，不并入 Smelter builder。

### 1.4 R0 退出门

```text
universe enumerated
partition covers 1845 exactly once
N<300 exceptions written
identity child candidate recomputed from this closing
no recipe files generated
no family completion claimed
RECYCLING_DEFERRED_LEDGER_R0_READY
```

---

## 2. Smelter MTE identity catalog

### 2.1 Slug 与边界

```text
slug            = recycling/smelter-mte-identity
owns_families   = 0
completion_delta = 0
```

缺的不是 RecipeMap 或 Smelter 机器本体，而是 **1,817 个
`(gregtech:gt.multitileentity, exact meta)` 的玩家可持身份与取得路径**。Bath 已有
先例：[`BathMteIdentityCatalog`](../../../../src/main/java/com/masson/cruciblecraft/content/item/BathMteIdentityCatalog.java)
覆盖 118 个 meta / 76 个新物品。本 child 建立平行的 Smelter recovery catalog，不改写
Bath catalog。

### 2.2 Catalog 规则

每个 identity 行必须包含：

```text
source_item
source_meta
runtime_id
registry_kind          # existing_item | new_item
mapping_class          # exact_item, never meta-range
acquisition_authority
display_requirements   # holdable + distinguishable
source_revision
```

禁止：

- meta 区间算术或按名字相似做 alias；
- 把 unknown MTE 映射成占位物品；
- 重注册已经由 Bath / 管线 / 线缆 / 已有机器物品表达的同一 meta；
- 用创造栏可见性代替 survival 取得。

与 Bath 重叠的 meta 必须映射到已有 `runtime_id`，并在 catalog 里写清
`registry_kind = existing_item`。R0 对照若不能同时复现“唯一 meta 数 = 本波将要锁定的
proven family 数”，identity child 保持 BLOCKED。

### 2.3 取得路径

对 catalog 中每个将进入 1817 lock 的 identity：

```text
B0 = 当前 survival 可取得，或明确的已有机器/管线身份
B1 = B0 + 本 child 新增的可操作取得
B2 = B1 + smelter/deferred-recycling 生产关系
```

Support 必须是实际 recipe、loot、worldgen 或可操作机器路径。declaration-only token
不算。创造栏不是 B0。

本 child 可以生成 identity 物品、模型和必要的取得 support，但不能发布 1817 条
recovery recipes，也不能提前从 deferred ledger 扣减。

### 2.4 Identity 退出门

```text
catalog current for every proven-family meta
overlap with Bath mapped, not duplicated
acquisition tests pass for locked identities
no recovery recipe completion claimed
SMELTER_MTE_IDENTITY_READY
```

---

## 3. Smelter proven recovery 生产波

### 3.1 Opening

```text
slug      = smelter/deferred-recycling
families  = 1817
relations = 1817
metas     = 1817
```

Production lock 只能来自 R0 `smelter_proven_mte_recovery` 与 identity closing。不得从
`t42_owner_recovery_evidence.json` 直接跳过 R0 锁 1,817。

### 3.2 表示法

`ParameterizedSpec` 运行时仍 fail-closed。本波使用 **compile-time compact `exact`**
singleton：

```text
one family = one relation = one consumed MTE identity -> one molten fluid
```

生成器读取 R0 universe + identity catalog + pinned GT6 dump，写出规范 JSON。禁止：

- 手写 1,817 个 recipe 文件；
- 按 recipe index 区间切 publication group；
- 把 `#1829` / `#1884` 或 Autoclave 24 条混进这个 lock；
- 为过 load gate 删 relation 或改成 representative 抽样后记 completion。

Publication groups 按可查询的机器身份 / 输出材料 / 取得语义分组，再按 query 可直接
求出的 keys 分 shard。Group 数由材料与身份结构决定，不预先指定 64/128 行硬切。

### 3.3 保真与玩家路径

每一条 logical relation 必须：

- generated/runtime 等价；
- 输入是 catalog 中的可持有 MTE identity，数量与来源一致；
- 输出是已注册 molten fluid，材料与数量 source-backed；
- 在真实 `ModProcessingMachines.SMELTER` 上可执行；
- 不能通过重复熔化同一台已放置机器制造无来源材料（输入必须是消耗物品，不是世界方块
  复制）。

GameTest 覆盖 publication groups 的真实执行，而不是只数文件。

### 3.4 Load

沿用已修复的 `wave_ready`：任何 measurement 为 `null`、`PENDING_MEASUREMENT`、
`measurement_unavailable`、missing-event 或 zero-filled 时，不得 `WAVE_READY`，也不得
写 seal。

分别采集 card-only 与 integrated（历史 compact + 已关闭 ordinary + 本波 groups）。
Count telemetry 超过旧 21,000 / 56,000 只报告 `UNVERIFIED_SCALE`；真实 reload /
allocation / retained memory / lookup 轴继续阻塞。不得用配方数硬上限在 runtime 里
抛掉已经可加载的树。

### 3.5 本波退出门

```text
production lock = 1817 / 1817
equivalence current
player path current
GameTest receipt current
load.status = LOAD_READY
eliminate_reasons = []
deferred recycling decreases by exactly 1817
no partial families
SMELTER_DEFERRED_RECYCLING_READY
```

---

## 4. Smelter edge cohort

### 4.1 Opening

```text
slug      = smelter/deferred-recycling-edge
families  = 2
relations = 2
```

`#1829` 缺 alumina 材料身份证明；`#1884` 的 lava 走 builtin，且
`runtime_registered = false`。它们不能进 1817 builder，直到输出身份被证明或被正式
scope。

### 4.2 允许的结果

每条只能是：

```text
proven_equivalent     # 补齐材料/流体身份后发布 exact
phase_deferred        # 仍缺身份，写新的 future_owner + recheck
post_1x_scope         # 独立 scope decision，离开 1.x deferred ledger
unsupported           # 来源无法无损表达，同样要正式记录
```

禁止把 lava 静默映射成任意熔融金属，也禁止把 alumina 流体登记成“差不多的铝”。

### 4.3 退出门

```text
both families closed, reclassified, or post-1.x scoped
N<300 exception recorded
no silent merge into 1817
SMELTER_DEFERRED_RECYCLING_EDGE_READY
```

---

## 5. Autoclave tagged-recycling cohort

### 5.1 Opening

```text
slug      = autoclave/deferred-recycling
families  = 24
relations = 24
```

Opening 以 R0 纠正后的名单为准。若 R0 把部分或全部条目标成
`mislabeled_needs_reclass`，本 child 仍拥有这 24 条，但 lock 与表示法按新标签走。

### 5.2 工作方式

不要复用 Smelter MTE → molten builder。先按 R0 形状重做 operand disposition：

- 普通 Autoclave processing：补 identity / B0，发 compact exact 或 exact_multi；
- 另一种回收 runtime：先写清消耗/不消耗、流体输入、物品输出，再决定要不要新的
  identity catalog；
- 仍无法在 1.x 表达：独立 post-1.x scope decision。

Mixed family 不允许只发布普通 relations 后提前 completion。

### 5.3 退出门

```text
24 families accounted
each has completion, evidence reclass, or post-1.x scope
deferred recycling decreases only for still-recycling closures
N<300 exception recorded
AUTOCLAVE_DEFERRED_RECYCLING_READY
```

---

## 6. 非回收 deferred

### 6.1 Slug

```text
slug          = recycling/non-recycling-scope
owns_families = 0 for infrastructure decisions;
                2 if this child actually publishes recipes
```

两条权威项：

```text
portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0010
  future_owner = later:execution_envelope/gt6_panel
  reason       = exceeds centrifuge envelope 1/6/1/6 with 64000 mB tanks
  recheck      = panel or higher-tier variant must accept shape/tank/EUt

portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0207
  future_owner = later:cross_mod
  reason       = ic2pahoehoelava
  recheck      = in-mod identity or dedicated later:cross_mod owner
```

### 6.2 允许的结果

`#0010`：扩大当前 centrifuge envelope、引入更高 tier 变体并发布该 family，或写下
post-1.x scope decision。扩大 envelope 若改变产品语义，不能为了清账而改。

`#0207`：给出模组内 pahoehoe / 熔岩语义身份并发布，或写下 post-1.x / dedicated
cross-mod owner。不得把 IC2 流体偷偷改成普通 lava 来绿门。

这两条关闭后，deferred ledger 不再保留 `later:execution_envelope/gt6_panel` 或
`later:cross_mod` 悬挂项，除非 scope decision 明确把它们移出 1.x。

### 6.3 退出门

```text
both items implemented or independently scoped
ledger no longer contains unsigned later:* for these two
NON_RECYCLING_SCOPE_READY
```

---

## 7. Program closeout

当六个 child 都 READY 后，program 本身 `owns_families = 0`，只做账本重放：

```text
tools/waves/recycling/deferred-ordinary-runtime/gap_replay.json
tools/waves/recycling/deferred-ordinary-runtime/deferred_ledger.json
tools/waves/recycling/deferred-ordinary-runtime/readiness.json
tools/waves/recycling/deferred-ordinary-runtime/closeout_seal.json
```

必须证明：

```text
current execution gap = 0
deferred ledger = 0 or only post-1.x scoped remnants
no inherited unenumerated count
wave_ready derivation still rejects pending/missing load
unique_active_wave = null
next_unassigned = true
DEFERRED_ORDINARY_RUNTIME_READY
```

关闭时新增语义 work log 与 stage archive，更新 roadmap、workflow、verification 与
历史索引。本计划不自动创建 Git commit。

后继默认是 **1.x joint exit gate**，不是核能。exit gate 仍要独立验收 census、RU/KU/HU
matrix、storage 28/624、以及 closure / fidelity / load 三轴。本 program 不得预写
exit lock 或 nuclear census work set。

---

## 8. 明确不包含

- 剩余语义命名迁移；
- 解开 `ParameterizedSpec` 作为本卡完成条件；
- GT metadata multiplexing 或按 meta 区间计算身份；
- 把 Bath 118 个 identity 重注册一遍；
- 重写 frozen T38–T49 与 ordinary-closure seals；
- 为过 hard gate 临时删 relations 或手改 status/hash；
- 把 deferred 算 ordinary completion；
- 1.x joint exit、核能 Track C、玩家发行、RC soak、GA。

---

## 9. 撤回

任一 child 发现阻塞时只有三种合法结果：

1. 在本 child 内增加有界、可计数的 identity / acquisition / 流体身份；
2. 保持 BLOCKED，先修 runtime 或表达能力；
3. 正式撤回并重签，保留撤回原因和旧 selection hash。

撤回只撤当前 child。不得：

- 用其它 host family 填 N；
- 把 1817、2、24、2 四组混成一张 lock；
- 把 Autoclave 24 条强行喂给 Smelter recovery builder；
- 为过门把 `#0010` / `#0207` 改成“差不多的”流体或 envelope；
- 恢复里程碑编号命名。
