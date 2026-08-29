# T42-Owner 详细计划：retained execution gap owner partition

> 阶段：T42-Owner · T42 内部 owner-partition repair gate  
> 状态：✅ 已关闭（2026-08-28）；`T42_OWNER_READY`  
> 性质：`owns_families=0`、`completion_delta=0`、`publication_delta=0`，不占用 T43  
> 前置：`T42_PARTITION_READY`、`T42_REPAIR_READY`，均为 `failed_gates=[]`  
> opening：T42-Repair closing execution gap，5,300 families  
> closing：1,817 proven Smelter recovery family 进入 `later:recycling`；current execution
> gap `5,300 - 1,817 = 3,483`  
> 后继：T43 已作为独立 Smelter stone bulk card 签发；Storage 保持 T43 closeout 后的串行候选

## 1. 原因与决策

T42 已完成 source family 覆盖和五桶诊断，但 `needs_unique_block_or_mte` 仍是包含
4,980 families 的 residual primary bucket。其中有 1,819 个 Smelter MTE、803 个 Bath MTE，
而另有 1,214 个 residual 没有 `unique_kinds`，它们实际来自 unmapped identity、未证明
vanilla 或 B0 acquisition，而不是独立对象。

原 T42 lock 只将 1 个 `already_expressed` 和 4 个
`later:combinatorial/<host>` 移出 execution gap，closing 为 5,300。它正确地拒绝了
“kind 标签即自动 `later:object_expression`”，但没有为 retained gap 提供可签发的 owner
track；同时现行 storage interleave 只要求已有 deferred ledger，门槛过弱。

本卡保留 T42 的五桶作为历史诊断，不重写 T42 / T42-Repair closed artifacts。它新增一层
owner partition：

```text
T42 repaired retained gap (5,300)
  -> owner-track overlay + recovery evidence
  -> append-only owner disposition lock
  -> effective current execution gap
  -> T42_OWNER_READY
  -> T43 / storage serial issue gate
```

每个最终 retained family 必须有一个非空的 `current_owner`；仅实际 phase-deferred 的 family
有 `future_owner`。这不会为了填 owner 字段而把 unresolved family 从 gap 扣掉。

## 2. Owner taxonomy

权威 owner track 是有限枚举，禁止使用 primary bucket 名、`unclassified` 或空字符串：

| Track | 使用条件 |
| --- | --- |
| `recipe_wave/<host>` | `current_closure_ready`，待内容 production lock |
| `material_expression/{form,molten,fluid,mixed}` | 当前已知 material，但缺 form / molten / chemical expression |
| `recycling/{proven,evidence_needed}` | clean MTE 回收，或 MTE 回收证据仍不足 |
| `object_expression/<kind>` | 单一、边界明确的 block/multiitem/tool-head/GT prefix 等对象表达 |
| `identity_mapping/unmapped` | GT object/stone/hazmat 等没有可验证 runtime identity |
| `registry_proof/vanilla` | `minecraft:` source 未进入显式 runtime allowlist |
| `acquisition/b0` | identity 已证明但 source/player path 尚未闭合 |
| `coordination/multi_axis` | 多类 blocker 同时存在，须先做明确拆分设计 |

`secondary_owner_tracks`、`blocking_axes` 保留其余原因，不允许被主 owner 隐去。无
`unique_objects` 的 residual 依次进入 identity mapping、vanilla registry proof、B0
acquisition 或 multi-axis；它们不再伪装成 unique-object 工作包。

## 3. MTE 回收与 object expression

新增 `t42_owner_recovery_evidence`，从 T42 compact source snapshot 逐 relation 重算，而不以
host、relation count 或 `unique_kinds=["mte"]` 直接判定。一个 family 只有在**全部**
relation 均满足下列条件时才能成为 `later:recycling` 候选：

1. 所有 item input 都是被 `CONSUME` 的 MTE；
2. 没有额外 fluid input；
3. 所有输出都是已注册 material-form item，或已有 material identity 的、已注册 molten /
   chemical fluid；
4. 无 partial、unmapped、unproven vanilla、missing form/fluid 等 secondary blocker。

因此 Smelter 的 MTE → molten output 不会再因“没有 item output”漏掉回收形状；但无
material identity / runtime registration 证明的 molten output 仍留在 gap。Bath 的 MTE 也用相同
family-atomic 条件判断，不能因其 1,116 relation 规模延期。

`object_expression/<kind>` 是 retained family 的 current owner，不是自动 defer。只有未来建立
对象注册、行为和获取边界的独立证据卡后，才能在 append-only lock 中变为
`later:object_expression/<kind>`。

## 4. 机器可读产物与账本

新增：

```text
tools/t42_owner_pre_freeze.json
tools/t42_owner_track_overlay.json
tools/t42_owner_recovery_evidence.json
tools/t42_owner_object_expression_evidence.json
tools/t42_owner_disposition_lock.json
tools/t42_owner_gap_partition.json
tools/t42_owner_readiness.json
```

并为每个 JSON 提供 schema、builder、currentness target、verification builder-policy 与
dependency-DAG node。

- `pre_freeze` 是 write-once 的 post-repair hash freeze，绑定 repair readiness、T42 overlay、
  T42 lock、T42 gap 和 topology。
- `track_overlay` 覆盖 5,300 retained families，给出 `current_owner`、state、axes、secondary
  tracks 与 `interleave_disposition`。
- `recovery_evidence` 为所有含 MTE 的 retained family 记录 relation-level shape、material
  outputs、blockers 和 phase-defer eligibility。
- `object_expression_evidence` 是显式人工 proof input；每项必须绑定 object boundary、
  runtime behavior 和 recheck condition，默认空表不是一项自动批准。
- `disposition_lock` append-only；首次写入须显式 `--approve-initial-lock`。它只让 relation
  全量证明的 MTE recovery 进入 `later:recycling`；不自动 defer object expression。
- `gap_partition` 从 5,300 减去 owner lock 中新证明的 `later:recycling` /
  `later:object_expression`，`completion_delta` 固定为 0。

## 5. Topology、验证与退出门

[`tools/t42_card_topology.json`](../../../../tools/t42_card_topology.json) 增加不占用 T43 的
`T42-Owner` internal gate。T43 依赖 `T42-Repair` 与 `T42-Owner`，storage/presentation/
multiblock 的串行交错条件由“deferred ledger locked”收紧为：

```text
T42_OWNER_READY && owner_partition_complete
```

本 gate 关闭时 T43 仍未预分配 host/family；Drying 的 1-family ready slice 只保留为
priority intent。后续 T43 作为独立 card 在 R0 重冻结，不由本 gate 的排序提示指定范围。

`T42_OWNER_READY` 关闭快照要求：

- 5,300 opening 和 final effective gap 可复算；
- 每个 retained family 恰有一个有效 `current_owner`，每个 deferred family 恰有一个
  `later:* future_owner`，两者不能混用；
- recovery evidence 对所有 MTE family 有完整 source proof；
- 无 object-expression kind-only auto-defer；
- 关闭时 T43 未签发，且本 gate 无 recipe/form/runtime publication；
- `failed_gates=[]`；
- `recipe-partition` 与 `census-replay` profile 纳入 owner gate，且 currentness/clean
  `--check` 零漂移。

不做：不重抓 dump，不制作 MTE、方块、form、fluid、配方或 storage 内容，不改 T14 hard
ceiling，不改写 T42/T42-Repair 的 closed plan / archive / lock。T43 在本 gate 关闭后才
独立签发；其 R0、生产 lock 与 runtime publication 均不回写本计划的 closing evidence。
