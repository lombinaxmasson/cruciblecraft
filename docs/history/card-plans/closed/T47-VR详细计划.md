# T47-VR 详细计划：关闭验证单调性修复

> 阶段：T47-VR · Closeout Verification Monotonicity Repair  
> 状态：已关闭（`T47_VR_READY`，2026-08-31）。本文件位于 `card-plans/closed/`  
> 性质：工程/验证修复；`owns_families=0`、`gap_delta=0`、`completion_delta=0`、
> `publication_delta=0`  
> 不占用 T48 编号  
> 前置：工作树 `tools/t47_readiness.json#status = T47_READY` 且 `failed_gates=[]`  
> T47 production lock：395 families / 13,708 relations / 283 identities，SHA-256
> `884643b2ba569e4eb96c9fcfe63caeebc61be6bb2df3334f85d3cf13490614db`  
> 当前 ordinary gap：1,499 families（不得被本卡改写）  
> 后继：T48 只保留连续编号，当前未签发；本卡不预分配 T48 host、family IDs 或
> production lock  
> 签发层级：当前工作树；本次关闭不创建 Git commit

## 1. 背景、目标与边界

[T47](../closed/T47详细计划.md) 的内容证据链已经站住：`T47_READY`、`failed_gates=[]`、
N=395 / L=13,708 / R=0 / identities=283 / closing gap=1,499，isolated `-Pt47Recipes`
10/10 PASS，forward-v2 15 groups current。不稳的不是这组分母，而是**下一张卡一追加，
上一张卡的验证就开始撒谎**。

T40-VR 修好了门闸 overlay、atomic writer、sidecar 和 DAG。它没有切断这条链：

```text
composed identity/runtime v2
  -> historical GameTest receipt bound_artifacts
  -> player_gametest_present()
  -> census complete_family_count
  -> topology t{N}_complete / unique_active_card
  -> INDEX/roadmap 机器 --check
```

T47 往 v2 追加 delta、或把 T47 写进 `verification_profiles.json` 之后，T46 census
`--check` 会把磁盘上仍为 803 的 `complete_family_count` 重算成 0。那不是 T46 内容回退，
也不是 gap 算术错了，而是关闭语义被活收据重新派生。

本卡按 T40-VR 方式做内部 verification repair gate。它证明的是“已关闭卡的 `--check`
对后波 delta 单调”，不是“T47 重新实现”或“T48 Bath 尾账开工”。

权威输入：

- [ordinary recipe wave 流程与规范](../../../current/recipe-wave-workflow.md)；
- [验证指南](../../../current/verification.md)；
- [总体规划](../../../current/roadmap.md)；
- `tools/t46_readiness.json`、`tools/t47_readiness.json`；
- `tools/t46_census_delta.json`、`tools/t47_census_delta.json`；
- `tools/t46_card_topology.json`、`tools/t47_card_topology.json`；
- `tools/t46_gametest_receipt.json`、`tools/t47_gametest_receipt.json`；
- `tools/verification_profiles.json`、`tools/verify.py`。

本卡不撤销 `T47_READY`，不重签 T46/T47 production lock，不改变 395/13,708/283 或
1,499 gap，不生成配方、form、GameTest 或 T48 family ID，也不把 Bath 150-family
blocked 集合预写成下一张内容卡。

## 2. T47 完成面：哪些站住、哪些仍是遗留

| 关闭面 | 当前结果 | 本卡是否处理 |
| --- | --- | --- |
| Readiness | `T47_READY`，45/45 gates，`failed_gates=[]` | 只冻结，不重开 |
| Census | N=395，L=13,708，partial=0，R=0，gap=1,499 | 改 `--check` 语义，不改数字 |
| Topology | `t47_complete=true`，`unique_active_card=null`，`next_issue_id=T48` | 把 `complete` 变成封板快照 |
| GameTest | 10/10 PASS，log `tools/t47_gametest.log` | 缩小 receipt 绑定面 |
| Load | 15 groups；eager 14 / lazy 16,466 / cache 478 / authored 6,113 | 不重测；allocation soft warning 保留 |
| Git 持久化 | `HEAD` 下 0 个 `tools/t47_*` / `src/t47_*` 被跟踪 | R0 卫生：关闭证据必须可 clean-checkout |
| 详细计划页眉 | 仍写“执行中 / `card-plans/active/`” | R0 卫生：改成已关闭 |

283 是新增 block identities，不是 reclassification R。census 的 R 仍为 0。T14 口径
supersession（T46 closing 14/2758/222 不得当 T48 opening）是设计内 stale，不是本卡缺陷。

## 3. 四个验证缺陷

### 3.1 历史卡 currentness 随后波一起碎

现场：

- T46/T47 `bound_gametest_artifacts()` 绑定 live
  `tools/global_build_identity_ledger.v2.json` 与
  `tools/compact_recipe_runtime_manifest.v2.json`。
- `gametest_receipt_errors()` 拿 receipt 里的 composed v2 哈希与**当前工作树**比较。
- T47 追加 `t47_identity_ledger_delta.json` / `t47_runtime_manifest_delta.json` 后，
  T46 receipt 立即 drift。
- `build_t46_census_delta.py` 的 `complete_family_count` 不是磁盘冻结值，而是
  `player_gametest_present()` 为真才写 803，否则写 0。
- receipt 还间接受 `verification_profiles.json` 与 workflow 影响：这些文件进了
  runtime dependency manifest，后波改 profile 就会重绑历史收据。

这和“T35–T46 只读”对着干。封口时重绑 T46 收据、重写 T46 runtime manifest，只是把同一
问题再推迟一波。

### 3.2 census profile 其实不是 census

`tools/verification_profiles.json#profiles.census` 当前：

- `gradle_tasks: ["test"]`（全量 Java）；
- Python 含 T35 readiness 一类超重模块；
- builders 含 T35 以及 T38–T47 全历史 census/topology/readiness。

查账本和回归整个模组被做成一件事。失败点也分不清：T47 追加后 T46 integrated harness
若再钉全局组数，census profile 会红，尽管 topology/gap 没错。

### 3.3 topology 的 complete 是派生布尔，不是封板快照

`t47_complete` / `unique_active_card` 跟着 census closeout 走，closeout 又跟着 live
GameTest、strategy、equivalence、support 和 integrated load 走。后波改共享权威，前波
在 `--check` 语义上会重新变成未完成。INDEX 写着关闭，机器检查却可能说没关。

### 3.4 card-closeout 三条串行，Gradle 重复付费

```text
CLOSEOUT_PROFILES = ("recipes", "census", "census-replay")
```

`python tools/verify.py integration --profile card-closeout` 串行跑这三条。recipes
已经跑过全量 `test` 和 GameTest 时，census 再跑一遍 `test`。census-replay 虽无
Gradle，但仍重跑历史 load/receipt builders。代价随卡数线性涨，信号几乎不涨。

workflow §4.2.1 已禁止 recipes 与 census 并行（抢 `tools/t{N}_*.json`），但没有把
“查账”从“全量回归”拆开。

## 4. R0 · 事故冻结与 repair contract

新增 machine-readable repair artifacts：

```text
tools/t47_vr_pre_repair_freeze.json
tools/t47_vr_repair_readiness.json
tools/build_t47_vr_pre_repair_freeze.py
tools/build_t47_vr_repair_readiness.py
tools/tests/test_build_t47_vr_pre_repair_freeze.py
tools/tests/test_build_t47_vr_repair_readiness.py
```

R0 必须冻结并校验：

| 事实 | 冻结值 |
| --- | ---: |
| T47 production lock | 395 families / 13,708 relations |
| T47 identities | 283 |
| T47 remaining ordinary gap | 1,499 |
| T47 reclassification R | 0 |
| T46 complete families | 803 |
| T46 remaining gap（opening） | 1,894 |
| composed runtime groups | 15 |
| T47 GameTest | 10/10 PASS |
| source revision | `3703e40308c8c030763fd6297dea8b210d2a77b1` |

冻结文件绑定 T46/T47 production lock、census、topology、readiness、GameTest receipt、
runtime dependency manifest、composed identity/runtime v2、verification profiles 与
workflow 的 hashes。Repair 期间：

- 禁止 `--approve-resign` T46/T47 lock；
- 禁止通过修改 family/gap/census identity 使 repair 通过；
- 禁止把 open upstream debt 伪装成 PASS、skip 或 inherited READY；
- 禁止签发 T48 或预写 T48 host/family IDs。

R0 卫生，不属于内容变更：

1. 把 `T47详细计划.md` 页眉改成已关闭 / `card-plans/closed/`；
2. 在用户明确授权 commit 之前，列出 T47 全工件链 + forward-v2 manifests +
   roadmap/INDEX 为待持久化集合。clean checkout 到当前 `HEAD` 仍无 T47 工件，这是
   仓库级关闭缺口，不是内容回退。本卡不得把“未 commit”写成 `T47_READY` 失败。

## 5. R1 · 关闭卡 closeout seal

为每张已关闭 recipe/repair 卡建立不可变 seal，与 live census builder 解耦：

```text
tools/t{N}_closeout_seal.json
tools/closeout_seal.schema.json
tools/closeout_seal.py
```

Seal 至少包含：

- `status`、`card_id`、`sealed_at_wave`；
- N / L / R / gap 与 production lock SHA-256；
- 本波 generated recipe tree、support tree、GameTest Java、committed log fingerprint；
- 本波 publication group / shard aggregate root；
- **关闭当时** composed identity/runtime v2 的哈希（快照，不是 live pin）；
- census / topology / readiness 文件哈希。

规则：

- 已关闭卡的 `--check` 只比 seal 字节/哈希与 N/L/R/gap 算术，**不再**调用
  `player_gametest_present()` 或 live composed v2。
- `complete_family_count` 对已关闭卡是 seal 字段，不是 `build()` 再派生的布尔。
- topology `t{N}_complete` 对已关闭卡读取 seal；`unique_active_card` 不得因后波
  delta 把前波打回 incomplete。
- 当前 active 卡仍走 live closeout；封板时写 seal，之后只读。
- 不重写 T35–T46 巨型 census 正文。历史 `--check` 切到 seal/sidecar；语义漂移或
  corrupt JSON 仍 fail closed。

T46/T47 是第一批强制 seal。T38–T45 按同一 schema 补 sidecar，不改历史 READY 叙述。

## 6. R2 · GameTest receipt 绑定面

Receipt 只绑定本波会改变测试行为的对象：

```text
gametest_java
production_lock
t{N}_generated_recipes
t{N}_locked_support
publication_group_manifest
shard_manifest
runtime_dependency_manifest   # 本波快照，关闭后不再与 live 文件重比
material_registration_gate_java / semantic root   # 仅当本波改了 gate
```

明确移出历史 receipt 的 live pin：

- `identity_ledger_v2` composed 文件；
- `runtime_manifest_v2` composed 文件；
- `verification_profiles.json`；
- `docs/current/recipe-wave-workflow.md`。

这些共享权威改由**当前** integration gate 管理：

- forward-v2 readiness / composed v2 `--check` 属于当前波或 `verification` profile；
- 历史 runtime dependency manifest 保存 close-time snapshot；`--check` 比对 snapshot
  自身字节，不要求它等于 live 文件。
- 后波 ABI 变化走 workflow 已有 compatibility / migration gate，不回溯改写已归档
  receipt。

允许本卡重绑 T46/T47 receipt：**合同本身被修复**，不是为了让红灯变绿而少绑证据。
新绑定面必须仍能证明 isolated GameTest 对着正确的本波 Java、lock 和 recipe/support
树。禁止顺便丢掉 production lock 或 generated tree。

T47 receipt 的 `t46groupunchanged` 一类跨波断言保留，但它们检查的是 T46 group
membership root，不是全局 composed 组数。

## 7. R3 · census / topology 解耦

`player_gametest_present()` 对已关闭卡改为：

```text
seal.present
  and seal.gametest_status == PASS
  and receipt 字节与 seal 记录一致
```

不再读取 live composed v2，也不因后波 profile 变更把 `complete_family_count` 打成 0。

topology builder：

- 已关闭卡的 sequence entry `status: complete` 来自 seal；
- `unique_active_card` 只反映**内容卡**；T47-VR 这种 `owns_families=0` 工程卡不得
  占用该字段；
- `next_issue_id` 仍为 `T48`，不得因本卡变成 `T47-VR`。

新增回归：在不改 T46 seal 的前提下追加伪造 T48 delta fixture，T46 `--check` 必须
仍报 current，且 `complete_family_count` 保持 803。

## 8. R4 · profile 边界与 card-closeout 成本

### 8.1 census

census profile：

- `gradle_tasks: []`；
- 只跑 census / topology / readiness builders 与对应 Python；
- 全量 Gradle 不在这条路上。

T35 超重 readiness 模块若仍必须跑，单独留在 `census` 的 T35 子集或 `release`，不得
挡住 T46/T47 账本 `--check`。

### 8.2 recipes

recipes 继续支付一次 datagen + 全量 `test` + 本波 isolated GameTest。这是内容卡闭卡
的行为回归，不是账本。

### 8.3 census-replay

默认变成 hash/seal `--check` 与当前波 load decision。全历史 load builder 重跑放到
`release` 或显式 `--full-replay`。card-closeout 不再无条件串行重跑 T37–T{N}
benchmark。

### 8.4 card-closeout

重写 `CLOSEOUT_PROFILES`：

```text
当前内容卡 recipes（唯一支付 Gradle/GameTest）
  -> 当前卡 census/topology/readiness（无 Gradle）
  -> 已关闭卡 seal --check（无 Gradle，无 live GameTest）
```

census-replay 全量不再是每张卡必付。workflow §4.2.1 的“禁止 recipes 与 census 并行”
保留（仍可能写同一 `tools/t{N}_*.json`），但 census 不再内含第二份 `test`。

`verification.md` 必须改掉 “T40-VR 闭卡 = recipes + census + census-replay” 的旧
形状，写明新的支付点。

## 9. R5 · 历史 harness 切片

历史 integrated measurement harness 不得 `assertEquals(N, groups.size())` 钉死
**全局** composed 组数。

已部分修复的形状必须成为合同：

- T46 harness 只取 `wave_id <= 46` 的 13-group 切片，再断言切片大小；
- T47 harness 断言 `groups.size() >= 13` 且含 `t47_bath_exact` /
  `t47_bath_exact_multi`，不禁止 T48 以后追加；
- 新增 Java 测试：composed v2 再追加一个假 group 时，T46 切片仍为 13。

禁止为了让历史 harness 通过而重写 T47 的 15-group closing 测量。T47 T14 closing
（eager 14 / lazy 16,466 / cache 478 / authored 6,113）保持冻结，作为后继内容卡
opening，不是本卡重测对象。

## 10. 验证与关闭

至少验证：

```powershell
python -m unittest discover -s tools/tests -p "test_build_t47_vr*.py"
python -m unittest discover -s tools/tests -p "test_closeout_seal.py"
python -m unittest discover -s tools/tests -p "test_build_t46_census_delta.py"
python -m unittest discover -s tools/tests -p "test_build_t47_census_delta.py"
python -m unittest discover -s tools/tests -p "test_build_t46_gametest_receipt.py"
python -m unittest discover -s tools/tests -p "test_build_t47_gametest_receipt.py"
python tools/build_t46_census_delta.py --check
python tools/build_t47_census_delta.py --check
python tools/build_t46_card_topology.py --check
python tools/build_t47_card_topology.py --check
python tools/build_t46_readiness.py --check
python tools/build_t47_readiness.py --check
python tools/verify.py integration --profile census --report-all --json build/verification/t47-vr-census.json
python tools/verify.py integration --profile card-closeout --report-all --json build/verification/t47-vr-closeout.json
```

故障模拟（必须 fail closed，且不得改写 T46/T47 lock 或 gap）：

1. 追加伪造 composed v2 group 后，T46 seal `--check` 仍 current；
2. 改 `verification_profiles.json` 后，T46/T47 receipt `--check` 仍 current；
3. 损坏 T46 seal 字节后 `--check` fail closed；
4. census profile 不再调用 `gradlew test`；
5. card-closeout 只出现一次 Gradle `test` 支付点。

关闭时更新 verification guide、recipe-wave workflow、known issues、roadmap、history
index；创建 T47-VR stage archive / work log。只有全部 repair gates 通过才把本文件移到
`closed/`。

Git commit、tag 或 push 由用户另行明确授权。T47 工件链的首次入库可以与本卡关闭
commit 一起做，但不得混进 Bath family 或 T48 lock。

## 11. 退出门

- `T47_VR_READY`、`failed_gates=[]`、`owns_families=0`、`gap_delta=0`；
- T47 lock 395/13,708/283、gap 1,499、T46 803/1,517 不变；T48 未签发；
- 已关闭卡 `--check` 不调用 live GameTest，不因 composed v2 / profiles 变更把
  `complete_family_count` 打成 0；
- GameTest receipt 不再 live-pin composed identity/runtime v2；
- census profile 无 Gradle；card-closeout 只让 recipes 支付一次 Gradle/GameTest；
- 历史 harness 按 wave 切片，不钉全局组数；
- 伪造后波 delta 的回归通过；T46/T47 seal 与 readiness 全 current。

## 12. 非目标

- 不新增 recipe family、material form、worldgen 或机器功能；
- 不重签 T46/T47 production lock，不修改 gap，不提前选择 T48 host/families；
- 不关闭 Bath 剩余 150 families，不申请本卡 `N<300` 例外（那是后继内容卡的事）；
- 不把 Mixer 评估提前到 Bath 清零之前；
- 不批量重写 T35–T46 历史 semantic JSON 来掩盖 hash drift；
- 不以“少绑证据”缩短收口：缩小的是**错误的共享面 live pin**，不是本波 lock/tree；
- 不把本卡写成 T48，也不把 `next_issue_id` 改成别的编号。

## 13. 本卡之后：T48 只是备忘，不是本卡范围

Bath 未清零。T47 R0 留下的 blocked 集合是后继内容卡的候选宇宙，**本卡不冻结、不签发**：

```text
blocked families     150
blocked relations    34,186
blocking axes        identity_or_operand 150
                     object_identity     150
                     material_form        47
                     b0_acquisition       26
unique_kinds         multiitem 60
                     tool_head 36
                     empty     18
                     gt_prefix:crushedPurifiedTiny 13
                     其余零散 gt_prefix
```

后继内容卡若开工，必须另签详细计划，并从 T47 closing 重冻，不能沿用本备忘当 lock。
那张卡需要显式申请 `N<300` 尾账例外，不得拿其它 host 凑 300，也不得在 Bath 清零前
切 Mixer。静态 all-lazy 约 50,652（T47 closing 16,466 + 34,186）低于 lazy hard
56,000，仍必须实测 allocation / reload / sync / routing。那些测量属于后继内容卡，
不属于 T47-VR。
