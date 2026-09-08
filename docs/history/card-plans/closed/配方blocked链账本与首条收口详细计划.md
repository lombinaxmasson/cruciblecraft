# 配方 blocked 链账本与首条收口详细计划

> 计划 slug：`recipe/blocked-chain-ledger`
> 状态：已关闭（2026-09-08）。本文件位于 `card-plans/closed/`。
> 正式名称：配方 blocked 链账本与首条收口
> 性质：把分散的 blocked 配方记录收成一份可复算账本，并对第一条
> source-backed 链做收口决策。第一条链（fluidbed 49）决策为
> `explicitly_blocked`：缺 `storage.dust` 身份、未注册 `dust_div72` /
> 灰形态，以及 GT6 `needsOutputs: false` 尚无 outputless 燃料模型。
> 已发 6 条保持不变。本卡不是全量清零卡，也不是普通 recipe wave。
> 机器可读 `unique_active_wave = null`。不创建 `*_READY`。不晋级
> `energy/converter-catalog`。当前工作树不创建 Git commit。

本卡曾是人读 unique active 内容卡，现已关闭。已关闭的
[验证精简与动态测试契约](验证精简与动态测试契约详细计划.md)
是工具化计划归档，不占内容 lane，也不拥有配方 family。

权威边界：

- [总体规划](../../../current/roadmap.md)
- [冻结与未实现账本](../../../current/unimplemented-gap.md)
- [能力交付流程](../../../current/capability-delivery-workflow.md)
- [开发与验证指南](../../../current/verification.md)
- [Ordinary Recipe Wave 语义合同](../../../current/recipe-wave-workflow.md)

Java/tick 源：本地 `gt6_code/gregtech6`，revision
`3703e40308c8c030763fd6297dea8b210d2a77b1`。不从 GitHub 补证据。
缺真实形态或输出时保持 `blocked`，禁止 stand-in。

---

## 0. 开场判断

当前仓库**没有**一份新鲜、全局、去重后的 blocked 配方统计。能核对到的
数字来自不同 epoch，不能相加：

| 来源 | 口径 | 读法 |
| --- | --- | --- |
| [`tools/energy_converter_fluidbed_blocked.json`](../../../../tools/energy_converter_fluidbed_blocked.json) | 49 行 / 已发 6 条 | 当前最明确的单一 map blocked ledger；11 条 `unmapped_item_input`，38 条 fail-closed（缺 `dust_div72` / 灰形态，或 GT6 `needsOutputs: false`） |
| [`tools/bath_remainder_candidate_selection.json`](../../../../tools/bath_remainder_candidate_selection.json) | 150 family / remainder 395 production | Bath remainder 的 identity / form / object 链；关系数按 family 膨胀，不能当 150 条配方 |
| [`tools/bath_identity_candidate_selection.json`](../../../../tools/bath_identity_candidate_selection.json) | 5 blocked | 回收候选未证明，不是同一条形态链 |
| [`tools/petroleum_b4_operand_proof.json`](../../../../tools/petroleum_b4_operand_proof.json) | 702 blocked / 54 translatable（756 抽样） | 2026-08-11 历史 optional proof；不是当前总数 |
| ordinary-closure `candidate_selection.json` | blocked = 0 | Smelter / Mixer / Autoclave / Centrifuge / Compressor / Drying / Electrolyzer 当前 candidate 无 blocked family |
| `remaining_recipe_gap` / `p2_blocked_count` / reachability `blocked` | 未分配 family、测量排除、可达性层 | **不是** recipe blocked 数 |
| 裂变热流体 `Coolant_IC2` / `Thorium_Salt` | `out_of_scope_external` | 外部兼容身份，不是配方债 |

闭卡 blocked 修复已放宽：缺件可以在碰到的卡上换成真物，不必另开回收卡。
本卡要的是**可复算的链**，不是把所有历史 blocked 一次做完。

---

## 1. 卡片合同

```text
capability_slug           = recipe/blocked-chain-ledger
wave_slug                 = none
unique_active_wave        = null
owns_families             = 0
new_recipe_map            = 0
new_machine               = 0
production_lock           = null
partial_close_allowed     = false
nuclear_started           = true（由裂变生存卡保持；本卡不改）
converter_catalog         = already player_complete; this card does not re-promote it
```

本卡允许：

- 生成 canonical blocked ledger 和按根阻塞原因的链排序；
- 对 **fluidbed 49 行**做第一条链的 source-backed 决策（发真配方、另立
  outputless 燃料模型，或继续 blocked）；
- 修本卡 owned 的 builder / 测试 / 文档。

本卡不允许：

- 把 Bath 150 family、petroleum 702 行、`remaining_recipe_gap` 混进同一分母；
- 用 stand-in、跨材料 alternatives 或伪造 chance 把 blocked 改绿；
- 为了凑数重开 ordinary recipe wave 或降低 load hard ceiling；
- 接管裂变观测、热力机器、聚变、Jade 或验证精简卡的 owned paths。

---

## 2. In scope

### 2.1 Canonical ledger

生成一份可由固定 source revision 重放的账本。每条至少包含：

```text
source revision
host / recipe map
family id / relation or dump index
relation count (if family)
blocker root
missing identity / form / shape / output model
owner
replacement condition
recheck point
player-path disposition
freshness = current | historical_optional | out_of_scope_external
```

三种计数分开报告，禁止相加冒充总量：

```text
blocked_families
blocked_relations_or_rows
blocker_roots
```

读取现有 artifact，不重写已关闭 wave 的 production lock：

- `tools/recipe_bulk/ordinary_source.py` / `ordinary_wave.py` 的 family
  classification；
- fluidbed / Bath / petroleum / capability `identity_disposition`；
- 各 wave `candidate_selection.json`、readiness、census。

### 2.2 链分层

按根阻塞原因形成候选链，不按文件名堆：

1. **fluidbed 形态 / 输出模型**（本卡默认第一条）；
2. Bath identity / form / object；
3. petroleum / mixer fluid 与 item mapping（标 historical_optional，除非
   freshness 重放证明仍 current）。

capability blocked（Kelvin、Jade、外部流体）和 verification / measurement
blocked 另栏记录，不进 recipe 总数。

### 2.3 第一条链：fluidbed 49

默认先验证
[`tools/energy_converter_fluidbed_blocked.json`](../../../../tools/energy_converter_fluidbed_blocked.json)。

决策门（缺一则该行保持 blocked）：

- 输入物品在 CC 有真实 registry identity，不得用无关 dust 顶 GT6
  `gt.meta.storage.dust` meta；
- 输出灰 / `dust_div72` 必须是已注册形态，或证明 GT6 该行本就无输出并
  另立 outputless 燃料模型；
- 不得把 `needsOutputs: false` 的燃料伪造 item output chance；
- 已发的 6 条可加载配方保持不变，除非来源证明它们错了。

若 49 行无法 source-backed 闭合，本卡只完成账本与排序，不强行发 JSON。
Bath 150 family 不塞进同一张卡。

---

## 3. Out of scope

- 裂变观测与安全、Reactor Jade、温度计、辐射；
- 热交换器、蒸汽涡轮、冷却器；
- 聚变控制器配方与等离子流体；
- 验证精简卡的 profile / Gradle 隔离实现；
- 重开 Bath remainder / identity 生产 lock；
- 把 petroleum mixer 1057 行改成 v1 required；
- 闭卡替身表的全面清扫（碰到真 GT6 零件时仍按工作流就地替换，不另开
  全量回收卡）。

---

## 4. Owned paths

实施允许触及：

```text
tools/energy_converter_fluidbed_blocked.json
tools/recipe_bulk/**
tools/tests/test_*blocked*
src/main/resources/data/cruciblecraft/recipe/energy/fuels_fluidbed/**
tools/extract_energy_converter_catalog.py（仅当 fluidbed 生成器是 blocker）
docs/current/**
docs/history/INDEX.md
docs/history/card-plans/closed/配方blocked链账本与首条收口详细计划.md
```

只有第一条链证明必须改 runtime 时，才允许有界触及
`src/main/java/**` 里 fluidbed / fuels 校验。`run/**`、`build/**` 不提交。
验证精简卡的 `tools/verify.py` 等路径不属于本卡。

---

## 5. 实施阶段

### A. D0 ledger

实施开始前先写出 ledger JSON（路径在实施时新建，例如
`tools/blocked_recipe_ledger.json`）。未写出前不发新配方。

### B. 链排序

按 freshness、player path、单一 root 是否可闭合排序。fluidbed 是默认
第一候选；若 D0 证明它不能闭合，记录原因并把下一条标为后续卡，不在
本卡扩大 Bath / petroleum。

### C. 第一条链决策

对 fluidbed 49 逐行：`ready` 则发 source-backed JSON；否则保持 blocked
并写 owner / replacement / recheck。不得为了把 49 改成 0 而发明形态。

### D. Closeout

账本可重放；三种计数无重复；第一条链要么闭合要么显式 blocked。
`unique_active_wave` 关闭时回到 `null`。不重新晋级
`energy/converter-catalog`。

---

## 6. 验证策略

遵循 [能力交付流程](../../../current/capability-delivery-workflow.md)
与 [验证指南](../../../current/verification.md)。本卡不把
`promotion` / `release` 写成日常门。

日常：

```powershell
python tools/verify.py dev
```

只跑 changed-path 选出的受影响 profile。预期：

- ledger / recipe_bulk / Python 测试 → `recipe-generators` 或对应 Python
  suite；
- 若改了 Java / 测试 / `src/main/resources` 才命中 `runtime-java`；
- 不默认 `runClient`，不默认全量 `player-complete`，不重跑已接受能力。

晋级：

- 本卡默认 **不** 把任何 capability 从 `runtime_ready` 推到
  `player_complete`；
- 只有真的发生晋级时才运行 `python tools/verify.py promotion`（含
  GameTestServer 与 `--client`）；
- `release` 仍由独立 checkpoint 负责。

禁止把全仓库 `gradlew test`、十张已接受卡的 GameTest 或客户端启动写成
本卡 checkbox。

---

## 7. 验收门

- ledger 可由固定 source revision 重放；`blocked_families` /
  `blocked_relations_or_rows` / `blocker_roots` 分开且无重复；
- 每个未关闭条目都有 blocker、owner、replacement condition、recheck
  point、freshness；
- fluidbed 已发 6 条不被本卡静默改坏；
- 第一条链可关闭行通过受影响 profile 的静态 / 定向检查；不能关闭的行
  仍是 blocked JSON，不是 stand-in；
- 不改裂变热流体 `player_complete`，不改 `nuclear_started`。

---

## 8. BLOCKED / 撤回

必须保持 blocked：

- 缺真实 CC identity / 形态却发配方；
- 把 `needsOutputs: false` 写成假输出；
- 把 Bath / petroleum / gap / 测量 blocked 加进 fluidbed 49；
- 用跨材料 alternatives 压数量；
- 为了本卡顺手做 Jade、热力机器或聚变。

撤回时保留 D0 ledger；已确认不能发的行继续 blocked。撤回不得修改
`energy/converter-catalog` 或裂变卡的 closed seal。

---

## 9. 关闭清单

只有以下条件同时成立才关闭：

- [x] D0 ledger 可重放，三种计数分开；
- [x] fluidbed 49 逐行有 disposition；
- [x] 第一条链已决策：闭合或显式 blocked，无 stand-in；
- [x] 受影响 profile 的 `verify.py dev` fresh PASS；
- [x] 未触发则未跑 `promotion` / `runClient`；
- [x] 本文件移到 `card-plans/closed/`；
- [x] 更新 `roadmap.md`、`unimplemented-gap.md`、`known-issues.md`、
  `history/INDEX.md`；
- [x] `unique_active_wave = null`。

权威账本：[`tools/blocked_recipe_ledger.json`](../../../../tools/blocked_recipe_ledger.json)，
由 `python tools/blocked_recipe_ledger.py --check` 重放。三种计数：

```text
blocked_families          bath remainder 150 / bath identity 5 / ordinary 0
blocked_relations_or_rows fluidbed 49 / petroleum historical 702
blocker_roots             fluidbed: storage.dust 11 / missing input form 11 /
                          missing dust_div72 21 / outputless model 6
```

禁止把这些数字相加。Bath 下一条链标 `deferred_next_card`，本卡不签发。
