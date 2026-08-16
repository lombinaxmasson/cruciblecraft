# CrucibleCraft 阶段档案 · T22

> 阶段：T22 · 石油化工全量与纵深
> 状态：✅ 已关闭（内容侧 2026-08-12 收盘；`T22_READY` 于 2026-08-13 由第二次
> record 派生，作为 T22.5 P0 的前置收盘）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 权威产物：`tools/t22_readiness.json`；分支 `t22-5`（T22.5 开工时统一提交）

## 1. 开工门禁与基线冻结

从 T21 收盘冻结基线：32 RecipeMap、EMI configured 24、GameTest 85、
Java 539、Python 575、datagen tree `6878b9c8…` / 2,558 files；publication
18,879 logical / 16,654 eager / 2,225 lazy。G3 先算余量：logical 硬上限
21,000 − 18,879 = **2,121**，`petroleum_t22` 63 units / 1,071 行装得进。

## 2. A1 · 石油 exact 分母

T21 模板分母只覆盖十张 map，**distillery 不在其中**（T11 收盘 1,517 行只选了
872）。本卡补上 T21 未覆盖 map 的石油行：

| 来源 | 行数 |
|---|---:|
| T21 `petroleum_t22`（63 units） | 1,071 |
| distillery 石油行 | 6 |
| generifier 石油行 | 7 |
| **total** | **1,084** |

逐行分类 `unclassified = 0`：

| 分类 | 行数 |
|---|---:|
| v1_required | **2** |
| ordinary_optional | 10 |
| cross_mod_compat（construction foam） | 1,057 |
| ore_processing_byproduct | 9 |
| post_1_0_g10（bulletGt* 回收） | 3 |
| generic_processing | 2 |
| already_covered | 1 |

（原 `cross_mod_compat_note` 写"IC2 命名空间依赖"属理由错误，T22.5 P1 已修为
"CC 无消费端 + recheck condition"。）

## 3. A2/B1 · family 前置能力盘点

7 个 family 五项前置（source 全集 / CC 映射 / 执行机器 / energy identity /
真实消费端）逐项落盘：9 台石油机器全部已实现（distillery T17、centrifuge
T16/T17、mixer T16、smelter T17、compressor T16、electrolyzer T17、
fuels_engine T18、fuels_gas T18、generifier T11），**无新机器 Kind/Tier**；
energy identity 逐 family 显式映射；容器复用 T10 cell/便携罐 allowlist。

## 4. B2 · 首个 family 全量投影纵切

`crude_oil_distillation`（distillery：oil → fuel + lubricant）：1 行，
expected ↔ authored **全集双向相等**，双 multiset 哈希全等，全部字段
（amount / duration / EU/t / buffering）保留。投影 evidence = MEASURED。
在此卡验证了 delta 累加账本：`16,650 + T21(4) + T22(3)` 与 GameTest eager
断言一致。

## 5. B3 · 逐 family 批量投影（诚实交付）

8 个 family 落盘 `t22_family_manifest.json`：

| family | 状态 | 投影 |
|---|---|---|
| crude_oil_distillation | **PROJECTED** | 1 logical / 1 eager（MEASURED） |
| kerosine_normalization（generifier 拼写归一） | **PROJECTED** | 1 / 1（MEASURED） |
| natural_gas_processing（centrifuge 32 行） | MATERIAL_RULE_COVERED | 9 / 9（STATIC_INFERENCE，运行时规则覆盖） |
| mixer_petroleum_chemistry（49 模板 / 1,057 行） | NEEDS_GT6_DUMP | 1,057 / 200 eager + 857 lazy（PROJECTED） |
| oil_sand_processing（smelter 3 行） | NEEDS_GT6_DUMP | 3 / 3 |
| compressor_petroleum / electrolyzer_petroleum（各 1 行） | NEEDS_GT6_DUMP | 1 / 1 |
| fuel_combustion（fuels_engine） | NOT_RECORDED | 0（无 source 行，map 角色由 B2 lubricant 承担） |

**两次诚实回滚**：① centrifuge 32 行**实投 0/32**（配方 JSON 生成需 GT6 dump，
未假装投影）；② **Cutter 方案回滚**——Cutter 挂在 deprecated KINETIC 上属
T16-deferred，润滑油消费端改由 T18 已接受的 fuels_engine 承接
（DESIGN_POLICY：润滑油可燃，烧它是真实用途不是 void）。

## 6. C1 · 三种下游产物与真实消费端

| 产物 | 生产 | 消费 | 状态 |
|---|---|---|---|
| fuel | T11 原油 + T22 B2 蒸馏 | fuels_engine → KU 电力（16 KU/t @ 25 mB） | CLOSED |
| methane | T11 generifier + T20 天然气矿藏 | fuels_gas → HU 热 | CLOSED |
| lubricant | T22 B2 蒸馏副产 | fuels_engine 低档燃料（4 KU/t @ 50 mB，DESIGN_POLICY） | CLOSED_DESIGN_POLICY |

三者的 `o37_boundary` 均 INTACT——`liquid_medium_oil` 等 GT6 流体名未直接绑定
CC 材料（O-37 边界未被偷换成 source direct binding）。

## 7. C2/C3 · 玩家循环与异常路径 GameTest

新增 3 个 GameTest（85 → **88**）：
`t22PetroleumDistillationFuelsEngineAndCutter`（蒸馏 → 引擎 → 消费端到端，
真实配方走通）、`t22DistilleryFluidConservationAfterNbtReload`（reload 守恒）、
`t22DistilleryFireboxEmptyPreservesProgress`（缺燃料保进度）。

## 8. D1–D3 · 三轴账、重建与收盘

**Closure**：1,084 行 `unclassified = 0`；v1_required 2 行全部发布；
in-scope runtime blockers 0；排除行有 disposition。
**Fidelity**：发布行全集双向等价，multiset missing / extra / unassigned /
duplicate 全 0；identity 边界未动；CC 平衡调整单独标 DESIGN_POLICY。
**Load**：每 family 独立投影 + 策略依据；projection PASS；发布后
18,882 logical / 16,657 eager / 2,225 lazy，headroom **2,118**，delta
**+3 / +3 / 0**（3 个 datapack 文件：distillery、generifier、fuels_engine）。
**D2**：`rebuild_artifacts --keep-going` + diff 复核，历史 baseline（T16–T21
五份 + t12a 结论）零改动。**D3**：单次 clean record 五步全 PASS。

## 9. 验证管线返工（本阶段单独一课）

返工单诊断与处置：① `compare_gt6_recipes --write-reference` 修
`source_evidence` 导致 `gt6_ore_chain.json` 记录的 reference hash 滞留——
真实漂移，按正确顺序沿链刷新解决；② 该 builder 是唯一游离在 rebuild 之外
却有下游的 builder（写 310 MB 报告 / 130 MB reference，有意 SKIP），文档已加
警告句；③ `gt6_l1b_selected_recipe_operands.json`（23 MB）定位为提交
（committed_compact_evidence 与实际行为一致）；④ import manifest 里 6 个
"外来 hash"（一个 builder 记录了另外五个 builder 输出的 hash）属历史遗留，
**移交 T22 之后重构**——T22.5 未触碰，T23 之前仍待办。

## 10. 交接

- T22 收盘绑定：`t22_readiness.json`（`T22_READY`，completed stages
  T22a–d）、`t22_petroleum_denominator.json`、`t22_family_manifest.json`、
  `t22_b2_crude_oil_manifest.json`、`t22_c1_consumer_audit.json`、
  `t22_load_projection.json`、`src/main/resources/data/cruciblecraft/
  t22_publication_baseline.json`。
- 关闭时下一张卡为 **T22.5**；T22.5–T25 已随后关闭。当前唯一 active T
  是 T26（公开 Beta 门禁，等试玩）。权威档案现位于仓库根目录。
- 遗留：import manifest "外来 hash" 重构（见 §9④）；centrifuge 32 行与
  1,057 行 mixer 石油长尾在 dump 恢复后按 T22.5 C1 口径归入 post-1.0
  portfolio（可解锁列 1,002 行的一部分）。
