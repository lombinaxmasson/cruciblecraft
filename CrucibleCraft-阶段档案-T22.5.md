# CrucibleCraft 阶段档案 · T22.5

> 阶段：T22.5 · 前置调查与名称映射（T22 关闭之后、T23 之前）
> 状态：✅ 已关闭（`T22_5_READY` 由单次 clean record 派生，2026-08-13）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 权威产物：`tools/t22_5_readiness.json`；分支 `t22-5`（18 提交）
> 性质：纯工具阶段——**publication delta = 0**，零流体注册，Java 侧仅新增 1 个
> GameTest（C0 运行时护栏）。

## 1. 开工纠偏

- **两个账本术语冲突消除（A0）**：`t21_source_denominator.json` 的
  `ordinary_v1_required`（45,044 行，input-touch 诊断账）显式标注
  `superseded_by tools/t21_template_denominator.json`；账本二回指
  `supersedes`。**所有数字零改动**（45,044 / 89,643 units / 146,841 行）。
- **缺口实数修正**：计划 §0.4 的"13 个材料缺流体"实测为
  **15 材料 / 18 流体名**——计划漏计 concrete、glass、mercury、mac_guffium；
  hslasteel 实际有熔融形态（`molten hsla` 空格变体在 `fluid_form()` 中按
  molten 处理）。修正记录在 `t22_5_fluid_gap_disposition.json` 的
  `scope_correction`。
- T22 的 `cross_mod_compat_note` 理由修正：从"IC2 命名空间依赖"改为
  "CC 无消费端 + recheck condition"（原措辞使 recheck 永远不触发）。

## 2. A1 · 九张 map shape 分析（只出结论，未发布）

87,188 条 enabled 行 → 5,335 个模板，整体 16.34 rows/unit：

| map | enabled 行 | 模板 | rows/unit | 最大 family |
|---|---:|---:|---:|---:|
| bath | 59,855 | 1,375 | **43.53** | **1,148** |
| smelter | 21,969 | 2,983 | 7.36 | 624（2,873 单例） |
| compressor | 1,472 | 57 | 25.82 | 321 |
| centrifuge | 1,296 | 224 | 5.79 | 474 |
| assembler | 1,582 | 344 | 4.60 | 620 |
| autoclave / electrolyzer / drying / roaster | 各 392 / 290 / 217 / 115 | — | 6.53 / 2.71 / 1.40 / 3.83 | — |

**bath 是矩阵生成的**（计划假设证实）；未来搬长尾的成本比 1.00 rows/unit
口径低一个量级。membership 双射（unassigned / duplicate = 0 / 0）。
compact receipt 进 ordinary CI，全量 replay 归 `--source-replay`。

## 3. A2 · GT6 流体名 → CC 流体 id 映射表（零手写条目）

`tools/t22_5_fluid_mapping.json`，纯派生：322 条 normalized fluids × 激活政策
× CC 已注册流体全集（三 gate 110 + 材料 `molten_fluid` 204 + 静态 2 + builtin）。

| disposition | 数量 |
|---|---:|
| mapped | 302 |
| no_cc_fluid | 18（15 材料） |
| out_of_scope | 2（molten.euphemium / molten.schrabidium） |
| unclassified | 0 |

- 4 个 fixture 逐字断言：`molten.brass → cruciblecraft:molten_brass`、
  `molten.asphalt → cruciblecraft:molten_asphalt`、
  `ic2constructionfoam → cruciblecraft:construction_foam`、
  `ic2distilledwater → cruciblecraft:water_distilled`。
- **51 个 322 之外的流体名**（T21 行引用但 oredict 无条目）各自带显式身份决定：
  `spectral_dew`/`potion.mineralwater`（各 ~9.5k 行）→ magic/brewing 范围外；
  12 个 `cfoam.{color}` → construction_foam 等价（颜色通道丢弃）；
  16 个 `dye.chemical.*` + 4 个 `dye.watermixed.*` → G10 染色；4 个
  `liquid_*_oil` → 由 T11/T22 石油身份覆盖（O-37 边界不绑定 GT6 名）。
- 变异测试：翻转任一材料 status，受影响行分类跟着变。
- `build_t5_source_projection.fluid_form()` 仍是运行时推导参照；翻译层接入
  本表是 **T23** 的接线点（artifact `consumers` 注记）。

## 4. A3 · 物品层归类（规则先落盘）

`t22_5_item_classification_rules.json`：6 类，每类定义/判别式/recheck point，
理由一律是类别定义。49 keys / **115,481 次**全归类，unclassified = 0：

| 类 | 次数 |
|---|---:|
| equivalence_crushed_ore（ore.broken/normal × 宿主岩） | 13,413 |
| equivalence_block_variant（storage.ingot → CC block，1296 units） | 9,686 |
| equivalence_dense_plate（storage.plate → CC dense_plate） | 15,096 |
| out_of_scope_packaging_form（crate 64× 箱装） | 20,179 |
| out_of_scope_machine_item_variant（machine 物品，T12/T16 身份体系） | 23,492 |
| out_of_scope_storage_variant（dust/gem/plateGem/raw 存储块，CC 无压缩形态） | 33,615 |

## 5. B1 · 146,841 行配方分类 + B2 · 流体缺口处置

九类词表（沿用 T22）对账本二 ordinary_optional 展开行全量分类，
`unclassified = 0`，账本二只读（hash 断言进测试）：

| 类 | 行数 |
|---|---:|
| ordinary_optional | 78,682 |
| cross_mod_compat | **48,247**（cfoam / 跨模组流体 / magic） |
| post_1_0_g10 | 13,042（食物/酿造/染色/弹药/维度气体） |
| out_of_scope | 5,860 |
| ore_processing_byproduct | 894 |
| generic_processing | 102 |
| already_covered | 8 |
| post_1_0_nuclear | 6（候选级 `ATOMIC.ACTINIDE` 判据，同账本一） |
| v1_required | 0 |

B2：15 个缺口材料逐条处置——**全部 no_registration**（零 v1 消费端），
材料树 digest 钉死，`materials/*.json` 零改动。若未来有 v1 消费端，注册走
`ChemicalFluidRegistrationGate` 文件，绝不触碰材料 JSON。

## 6. C0 · 逐机器可玩性审计 ⭐

32 台已注册 RecipeMap 全落三态：**30 可玩 / 2 阻断**。计数 = authored 扫描
（gt_recipe map 字段 + material_rule target 字段）+ 已提交 manifest 展开归因；
`0` 是推导值。新增运行时护栏 GameTest
`t22_5RegisteredMapsHaveLogicalRecipes` 在服务端逐台断言零/非零判定
（GameTest 88 → **89**，实测通过）。

**v1 blocker（已登记 owner）**：

| map | 状态 | 处置 |
|---|---|---|
| cruciblecraft:anvil_bend_big | registered_zero_logical | T26 Beta 前需拍板：补弯曲配方 / 降级为预留槽位 / 声明 deferred |
| cruciblecraft:anvil_bend_small | registered_zero_logical | 同上 |

每台机器有一句 v1 角色（如 bath = 洗矿 + T5 钨链化学；cutter = 润滑油消费端）。

## 7. C1 · 真实分母重算（四列）+ C2 · Beta 措辞

146,841 行四列，和 = 总数（双路径断言：账本二诊断 vs B1 operand 分类）：

| 列 | 行数 |
|---|---:|
| v1 | 0 |
| post_1_0_portfolio | 140,981 |
| out_of_scope | 4,858 |
| **可解锁（映射到位即可）** | **1,002** |

**ceiling 不调**：v1 增量为 0，21,000 逻辑上限在 v1 前不会被触及（余量
2,118）；T14 四类实测协议的触发条件已写入 artifact。

C2：总体规划 0.1 Beta 契约改为"`v1_required` 全部发布；`ordinary_optional`
属于 1.0 后 portfolio，逐类登记 owner 与 replacement condition"。
`phase4_v1_planning_contract.json` 已与新措辞一致，未动（CORE_ARTIFACT
hash 稳定）。

## 8. D1 · 验证面与关闭

- `build_t22_5_readiness.py` + `t22_5_readiness_policy.json` + 验证器 T22.5
  门禁键**同时建好**；status 由门禁派生、`check()` 重算、缺键给一句人话
  （无 KeyError 级联）；验证器门禁从第一次 record 即完整（无放宽注释）。
- 8 个新 builder 注册：policy **66 → 74**（两处断言 + 测试同步），
  `check_builder_graph` 0 back-edge；rebuild WRITE_ARGV 补 dump 门控写入；
  python_test_policy 增 affected 规则与 source-replay 模式。
- 单次 clean record：builder 74/74、datagen×2、Java 539、GameTest 89/89、
  Python 636 —— `T22_5_READY` 由 session 派生，`--check-ready` 通过。

## 9. 顺带发现（只登记，未处理）

1. `build_t22_readiness.py:256` 的 `MATERIAL_RULE_COVERED` 状态名消解计数
   是 B1 门禁 (b) 所禁的反例；T22.5 的 builder 由测试保证不犯，T22 原文件
   **未改**（改它等于重开 T22 收盘）。
2. `import_gt6_oredict` full-replay 对 7 个原版金属材料文件（copper / gold /
   iron / lead / nickel / tin / zinc）报 stale——T22 时代 gate 漂移遗留；
   compact 检查不受影响，replay 套件按 skip 处理。
3. 锁定的 worktree `t22-g1-baseline`（ffbbeb1f）已过时，保持不动。

## 10. 交接

- 权威产物：`t22_5_readiness.json`（`T22_5_READY`）、
  `t22_5_fluid_mapping.json`、`t22_5_item_classification.json`、
  `t22_5_shape_analysis.json`、`t22_5_row_classification.json`、
  `t22_5_fluid_gap_disposition.json`、`t22_5_machine_playability.json`、
  `t22_5_denominator_recompute.json`。
- 关闭时下一张卡为 **T23**；T23–T25 已随后关闭。当前唯一 active T 是 T26。
- T22 阶段档案已补写于仓库根目录《[CrucibleCraft-阶段档案-T22.md](CrucibleCraft-阶段档案-T22.md)》。
- 2 个 bend map blocker 在 T23 已记显式 deferred，owner = T26 freeze。
