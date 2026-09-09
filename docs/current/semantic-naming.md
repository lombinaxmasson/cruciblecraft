# 语义命名长期清单

> 2026-09-04：批次 0–4 与收口清单 A–E 已落地。活动账本、收据、currentness、
> Python 合同测试、Java harness 和运营文档已改语义 slug。日常命名门是 `--quick`
> （零命中）。全量 `--summary` 会命中冻结 v2、id map 与更早独立阶段收据，不是清零
> blocker。日常加配方、改机器仍不要从这里开工。实施代码前必须先确认当前工作树
> 基线（见 §6）。numbered currentness sidecar、closeout seal 与 verification
> session 已列入 `tools/legacy_verification_index.json`，不参与 active
> verification，也不作为本清单的进度门。

目标：**除已关闭的历史计划外，仓库零里程碑卡号**（路径、文件名、类/方法/字段、注释、
字符串、Gradle 任务、哈希/收据依赖）。旧存档与旧收据允许失效或删除，不保留
运行时兼容别名。

唯一豁免：[`docs/history/card-plans/closed/`](../history/card-plans/closed/)
（已关闭的历史计划）。[`card-plans/active/`](../history/card-plans/active/)
与 [`card-plans/prep/`](../history/card-plans/prep/)
unique-active / prep 计划、工作日志、阶段档案、handoff、`archive/sealed/**`、`tools/`
闭卡账本 **不在豁免内**。

领域术语不是卡号，不要改：

- `GT6` / `gt6_*` / `Gt6StyleConnections` / `GT6MaterialMetadata`（GregTech 6）
- 游戏内电路/机器 tier 文案，如 `Circuit T1 (Basic)`、`Laser Engraver (T1)`
- 同位素与材质名，如 `cobalt60`
- 物理单位 `mB/t`、GT 列名 `Heat_T`
- 扫描器已过滤的 `TankBlock` / `ToolMaterial` / `TierProfile`

负例仍禁止扩成真配方族：`t50_*`、`recipe/t50/`、`cruciblecraft_t50`、
`-Pt50Recipes`、`isT50CompactRecipe`。测试里已有的 T50 **拒绝**字符串可保留到
对应测试改语义名时一并删掉。

零卡号扫描器：`python tools/check_zero_milestone_names.py`。这是手工审计，
不再挂在 `verify.py` 的 `verification` profile。`--quick` 只扫物流 /
verification Java 和 unique-active 计划，不扫 `tools/capabilities/`
（电压档 T2 / T5 不是卡号）。全量 `--summary` 仍不可当日常门。剩余盲区见 §4。

## 1. 已经搬走的（不要再搬一遍）

作者规则树：

| 旧 | 新 |
|---|---|
| `recipe/t7/mortar/` | `recipe/mortar/` |
| `recipe/t8/extruder/` | `recipe/pipe/extruder/` |
| `recipe/t10/anvil/`、`smelter/` | `recipe/ingot_form/anvil/`、`ingot_form/smelter/` |

`GTRecipeMapLoader.authoredMaterialRuleStage` 按语义前缀认 mortar / pipe /
ingot_form（历史 stage 7/8/10）。`t`+digit 解析仍留给 **stage ≥ 11** 的 authored
树；主资源里**没有** `recipe/t11/`。测试夹具字符串 `t11/future_rule/copper`
只出现在 `GTRecipeMapBudgetTest`，不要先假设有活路径再去「保护」它。

Java 测试**类名**已去卡号，例如：

```text
MortarMaterialRuleDataTest
PipeRuleDataTest
HotIngotRuleDataTest / MultiIngotRuleDataTest
MachineTierArchitectureTest
EnergyChainResourceTest
ComponentRuleDataTest
ChemicalMachineResourceTest / ChemicalResourceTest
CoverResourceTest / PipeAcquisitionResourceTest
CensusRuntimeInventoryClassifierTest
RecipeLogicalRelationIdentityTest
```

`src/main/java` 里后波次 javadoc、报错和私有校验方法（`validateT3` /
`validateT5` / `validateT18a/b/c`）已改成语义名。JSON 绑定字段本身没动。

`CrucibleCraftGameTests` 的 T18 能量链方法**已经语义化**（例如
`crudeOilFuelEngineDynamoPowersElectrolyzer`、
`naturalGasConvertsAndHeatsBoiler`）。`tools/t18_readiness.json` 与
`t18_readiness_policy.json` 的 `required_tokens` **仍钉旧名**。这是账本分叉，
不是「方法名还没改」。

配方 generated 活树已走语义路径（`assembler/compact/`、`bath/identity/` 等）。
历史编号路径映射见 [`tools/semantic_id_map.json`](../../tools/semantic_id_map.json)。

## 2. 审计基线（2026-09-03；A–D 之后部分过时）

下表是收口开始前的定向 glob / grep，**不是**扫描器全量 `--summary`（该命令扫
`src/recipe_generated` 会拖十几分钟，且曾被中止）。A–D 已迁走 compact / Bath /
ordinary-closure 活动账本、收据、currentness 和测试消费者。数量会随工作树漂移，
实施前重数；以 [收口执行清单](semantic-naming-closeout-checklist.md) 的当前状态为准。

| 对象 | 数量 | 备注 |
|---|---|---|
| `tools/build_t*.py` 卡号 builder | **397** | glob 398，其中 `build_tool_head_prefix_remap.py` 是 `tool` 假阳性 |
| `tools/**/t[0-9]*.json` | **675** | 含 `tools/waves/**`；根级约 642 |
| `tools/t[0-9]*.py` 共享模块 | **28** | `t35_common.py`、`t48_identities.py` 等 |
| `tools/tests/test_build_t*.py` | **229** | 文件名；类名 `T*` 约 249 |
| 卡号测试夹具文件 | **1321 / 6 目录** | 见 §8.5；无 live `t42`/`t44`/`t46` 夹具目录 |
| `archive/sealed/**` | **2763** | T38–T41、T43–T49 + `forward-v2`；无 T42 |
| 活 closeout seal | **11** | `tools/t38`–`t41`、`t43`–`t49_closeout_seal.json` |
| `src/main` 生产绑定/资源 | 约 **46 文件 / 584 命中** | 含假阳性过滤前的粗算；真卡号见 §8.4 |
| `build.gradle` 卡号 | 约 **43 处** | sourceSet、run 目录、20+ task |
| `docs/current/` 正文引用 | **7 文件** | 本文件除外后仍有运营叙述 |
| `verification_builder_policy.json` 中 `build_t` | **0** | 2026-09-03 晚已是 6 个语义 builder；661 是过时审计 |

夹具分目录：

| 目录 | 文件数 |
|---|---|
| `src/test/resources/t39_catalog_fixture/` | 157 |
| `src/test/resources/t39_withdrawn_recovery/` | 22（旧报告未列） |
| `src/test/resources/t40_catalog_fixture/` | 61 |
| `src/test/resources/t41_catalog_fixture/` | 294 |
| `src/test/resources/t43_catalog_fixture/` | 407 |
| `src/test/resources/t45_compiler_fixture/` | 380 |

`verification_profiles.json` 仍引用 **不存在的** `src/test/resources/t46_catalog_fixture/**`。

## 3. 旧报告过时点

2026-09-01 停手稿把剩余项写成「故意不搬」。下列条目以仓库现状为准：

| 旧说法 | 现状 |
|---|---|
| GameTest 仍有 `t18b*` / `t18c*` **方法名** | Java 已改；readiness JSON 未改 |
| `t11/future_rule` 是活 authored stage 11 路径 | 主资源无 `recipe/t11/`；仅测试字符串 |
| 约 396 个 `build_t*.py` | 397 个卡号 builder |
| `t39–t45_*_fixture` 约 1300 | 6 目录 1321 文件；缺 t42/t44；多 `t39_withdrawn_recovery` |
| `src/test/java` 里 T37–T49 measurement **类名** | 类已语义化（`*MeasurementHarness`）；status 字符串、Gradle filter、冻结 JSON 文件名仍带卡号 |
| Gradle `includeTestsMatching CompactRecipeFamilyT37…T49MeasurementHarness` | 指向**已不存在**的类 |
| 扫描器「路径 skip 是有意的」 | 扫描器**会**报 `build_t*` 路径；真正风险是 ledger / import 断链 |
| `docs/history/**` 与 `archive/sealed/**` 永远不必搬 | 与现行目标冲突；只豁免 `card-plans/closed/` |
| 绑定 ID 无迁移卡不要动 / 可留兼容别名 | 现行策略是一次性改名，**不留旧别名** |

## 4. 扫描器现状（批次 0 已落地）

[`tools/check_zero_milestone_names.py`](../../tools/check_zero_milestone_names.py)
现已：

- 豁免 **仅** `docs/history/card-plans/closed/**`。`card-plans/active/` 与
  `card-plans/prep/` 不得出现 TXX。
- `SCAN_ROOTS` 含 `src/t14Benchmark/`、`docs/history`、`docs/decisions`；
  `SCAN_FILES` 含根 README 与 `docs/README.md`。路径命中仍读正文。
- 匹配 `t18b` / `T13c`。`--quick` 由 `verification` profile 调度。
- `ALLOWLIST` 空。

仍不是最终门的部分：

- `--quick` 只覆盖已干净的物流 / capability 面，避免批次 2–4 完成前把 CI 打红。
- 不扫 `.png` 等二进制；`t34_gt6` 贴图只靠 JSON/Java 间接可见。
- 全量 `--summary` 在 `src/recipe_generated` 上不可当日常门。
- 批次 1 删树之前，非 `--quick` 扫描会在 `docs/history` 非计划子树上爆红。这是预期。

单测：`python -m unittest tools.tests.test_check_zero_milestone_names`。

## 5. 前置调查（改名之前先定性）

这些不是改名本身，但会让批次 4/5 的 diff 说不清：

1. **`cruciblecraft:t48_stained_*` tag** 被 3 条 bath identity 配方引用
   （`gt_recipe_bath_1338`–`1340`），仓库内**没有**对应 tag JSON。先确认是生成缺口
   还是死引用，再改成 `stained_glass` / `stained_glass_panes` / `stained_terracotta`。
2. **`compact_publication_policy.schema.json`** 仍 `const: "t39-shard-v1"`；活数据已是
   `compact-shard-v1`。schema 落后，不是第二套路由。
3. **`t16` / `t17` readiness `--check`** 可能已经 stale。不要为了改名去 `--write`
   历史收据；该删的删，该语义化的在新文件上重建 currentness。
4. **`ComponentRuleDataTest` 8426** vs `tools/t4_tool_policy.json` /
   `full_verification_report.json` 的 8141：闸门认了 `t48_required_forms` 后的分母差，
   与改名正交，但改闸门键时会一起暴露。

## 6. 实施前置：工作树基线

2026-09-03 晚 `master` 已与 `origin/master` 对齐。工作树只剩未跟踪的本地
`run-*` 目录，不参与本迁移。批次 1 在这棵树上删除 `archive/sealed`。

## 7. 硬规则

- 先改内容引用，再改文件名。不要整批盲 `git mv tools/build_t*.py`。
- 不保留 NBT / family / 资源路径旧键。需要的话只在**同一提交**里做一次性转换，
  提交后代码路径只认新名。
- 不把 Bath / MTE GameTest harness 当成功门。
- 不跑全量 `recipe_generated` 扫描当完成条件。
- 每批结束：定向编译/测试 + 收紧后的扫描器（排除 generated 巨树的快速模式）。
- 领域 GT6 术语保持不动。

## 8. 批次清单

依赖方向：扫描器与治理 → 可删历史/seal → 仍被 `verify.py` 使用的工具链 →
生产绑定与资源 → 测试/夹具/Gradle → 非计划文档 → 全量验收。

```text
0 scanner
  -> 1 delete history-or-seal
  -> 2 live toolchain
  -> 3 production ids and assets
  -> 4 tests fixtures gradle
  -> 5 non-plan docs
  -> 6 final gate
```

### 8.0 扫描器与治理 — 已完成

| 项 | 内容 |
|---|---|
| 对象 | `check_zero_milestone_names.py`、其单测 |
| 已做 | 豁免仅 `docs/history/card-plans/closed/**`；`active/` 与 `prep/` 进 `--quick`；`SCAN_ROOTS` 含 history / decisions / benchmark；已从 `verify.py` 日常门卸下；`--quick` 不再扫 capability JSON；单测覆盖新豁免与 `t18b`；ALLOWLIST 空 |
| 故意未做 | 不把 `--quick` 扩成全量活代码扫描（生产绑定仍有卡号，扩了会打断 CI） |

### 8.1 删除历史档案与 seal — 已完成

- `archive/sealed/` 整树删除；`semantic_id_map.json` 迁到 `tools/`。
- 根级 numbered closeout seal 与 `full_verification_report.json` 删除。
- `docs/history/work-logs/`、`stage-archives/`、`handoffs/`、`closed-plans/` 删除。
- `closeout_seal.py --check` 默认只认 `tools/waves/**/closeout_seal.json`。
- ADR 改名为 [`docs/decisions/容器身份与边界ADR.md`](../decisions/容器身份与边界ADR.md)。
- `legacy_seal_resolver.py` 仍在，只服务 wave slug closeout；编号 seal 文件已删。

### 8.2 现行工具链语义化或删除

先分「`verify.py` / `build_recipe_bulk.py` 还在调度」和「纯闭卡回放」。

批次 2：活工具改 import [`tools/io_common.py`](../../tools/io_common.py)；
`t35_common` 的 JSON/路径助手转到它。`build_t37_assembler_source.py` 已改名为
`build_assembler_source.py`。`recipe_bulk` 的语义 compile 路径已延迟加载历史
`t37`–`t45_common`，`python tools/build_semantic_recipes.py --check` 不再导入
任何 `build_t*`。磁盘上的 `build_t*.py` 与 `test_build_t*.py`、
`run_full_verification.py`、`verify_full_verification_report.py` 已删除。
历史 wave（`recipe_wave("T37")` 等）仍会按需加载 `t*_common`。

**已删除：** `tools/build_t*.py`、`tools/tests/test_build_t*.py`、
`run_full_verification.py`、`verify_full_verification_report.py`。
**仍在：** `t35_common` 等编号共享模块、根级 `t16_*.json` 等卡号 JSON、
`build_assembler_source.py`。

| 对象 | 代表路径 | 依赖 | 策略 |
|---|---|---|---|
| 仍被 profile 调用的 builder | 活 policy 已是 6 个语义 builder，0× `build_t` | 无 | 不要再给 352 个磁盘上的 `build_t*` 起别名；批次 2 默认删除 |
| 纯历史 builder | 其余 ~397 脚本中未被 live profile 引用者 | 历史 `--check` | **删除**；不要为扫描变绿去 `--write` |
| 卡号 JSON | `tools/t16_*.json`、`t18_readiness*.json`、`t24_workload_manifest.json`、`t35_runtime_registry.json`、`t42_*.json`（约 64）、waves 下 `t49_*.json` | builder 路径、currentness sidecar | live 需要的改语义名；其余随 8.1 删除 |
| 共享模块 | `tools/t35_common.py` 等 28 个 | 几乎所有旧 builder | 随调用方改名或内联进 `recipe_bulk` / `wave_*` |
| Python 测试 | `test_build_t*.py` 229；类 `T19PipeAcquisitionTest` 等 | `required_tokens`、`python_test_policy.json` | 跟文件走；grep token 后再改类名 |
| T18 token 分叉 | `t18_readiness.json` / `t18_readiness_policy.json` | 已改名的 GameTest；仍带 `t18b*` 的 JUnit | **同一提交**把 token 改成现行方法名，或删掉这条 numbered readiness |

**完成：** `tools/` 无 `build_t[0-9]`、无根级 `t[0-9]*` 文件名；`python tools/verify.py dev`
不引用旧脚本名。

语义名方向（示例，实施时按 host/cohort 定）：

| 旧 | 新方向 |
|---|---|
| `build_t34_gt6_art_manifest.py` | `build_gt6_art_manifest.py` |
| `build_t14_readiness.py` | `build_extruder_readiness.py` 或并入 recipe-load |
| `t35_runtime_registry.json` | `runtime_registry.json` |
| `t24_workload_manifest.json` | `scale_workload_manifest.json` |
| `tools/waves/bath/tiny-purified/t49_*.json` | 去掉 `t49_` 前缀，只留 wave 目录 |

### 8.3 生产绑定 ID 与资源

一次性改名，不留旧键。按耦合拆提交，但每类必须 Java + JSON + 测试同批。

| ID / 路径 | 定义 | 消费者 | 语义方向 |
|---|---|---|---|
| `FAMILY_ID = "t14_extruder"` | `ExtruderRecipeFamilyProvider` | `GTRecipeMapLoader`、GameTest | `pipe/extruder` 或 `extruder/compact` |
| NBT `t11_schema_version` | `SubsurfaceFluidDepositBlockEntity` | 世界存档 | `fluid_deposit_schema_version`（提交后只认新键） |
| `/data/cruciblecraft/t11_materials/` | `MaterialLoader`；`t11_materials/index.json`、`natural_gas.json` | 材质加载 | `hydrocarbon_materials/` |
| 闸门键 `t38_*` / `t39_*` / `t40_*` / `t48_required_forms` | `material_registration_gate.json` | `MaterialRegistrationGate`、`SemanticProjection`（后者缺 t48） | 按域：`compact_acquisition_forms`、`bath_required_forms` 等 |
| `t3_acceptance_required_not_gt6_original_gate`、`t10_known_forms` | 同上 | 闸门 / `ComponentRuleDataTest` | 语义 gate 名 |
| `energy_converters.json` `"stage": "T18a"|"T18b"|"T18c"` | 资源 + `energy_converters.schema.json`（含未用 `T18d`） | catalog 字段；`EnergyConverterCatalogTest` 过滤 | `steam_ku_chain` / `liquid_fuel_ru_chain` / `gas_hu_chain` |
| publication baseline 9 文件 | `t16`–`t28_publication_baseline.json`（含 `t26_5_`） | `CrucibleCraftGameTests` 硬编码名 + `t16_acquisition` 键 | 按能力域文件名 |
| census fixture | `census/t35_runtime_registry_gate.json` | `RecipeCensusRuntimeRegistryGateFixture.RESOURCE` | `census/runtime_registry_gate.json` |
| schema | `t30_art_asset_provenance.schema.json`；`storage_variants.schema.json` title T44；worldgen schema title T20 | 校验 `$id` / const | 语义 `$id` |
| 世界生成 registry | `large_t38_{gold_sulfide,platinum_group,molybdenum}_vein` | configured/placed feature + biome_modifier | 去掉 `t38_` |
| dedup `rule_id` `*_t39_support_post_enumeration` | `recipe_generated` 7 个规则文件 | 运行时 dedup | `player_path_support_post_enumeration` |
| 死前缀 `t39_player_path_support/` | 同上 `victim_selector.prefixes` | 已无对应 recipe 目录 | 删该行，只留 `player_path_support/` |
| bootstrap `selected_source_recipe` | `t36_coagulator_bootstrap` / `t36_roaster_bootstrap` | machine bootstrap JSON | 去掉 `t36_` |
| 贴图 `t34_gt6/` | `textures/block/t34_gt6/`、`t34_gt6_*_manifest.json` | `ModItemModelProvider`、block model、Energy/Steam resource 测试 | `gt6_import/` 或 `gt6_energy_art/` |

低风险 Java 文案可并进本批或单独小提交：`CoverDefinitionCatalog` 局部变量 `t19`
与 `"T19 cover definitions drifted"`；GameTest L10731 注释 `t18a`/`t18b`。

`material_tag_policy.json` 的 `expected_consumer: T3`–`T12` **无 Java 消费者**，
可随政策 JSON 语义化。`bath_identity_catalog.json` 的
`"acquisition_authority": "T48"` 同样不被 `BathIdentityCatalog` 反序列化。

**完成：** `src/main` 与打包进 jar 的 generated source set 无卡号路径/字段；
`compileJava` + 定向 GameTest/JUnit 绿。旧世界存档不保证可读。

### 8.4 测试、夹具、Gradle、benchmark

| 对象 | 代表路径 | 依赖 | 策略 |
|---|---|---|---|
| 夹具树 1321 文件 | §2 六目录；内部还有 `recipe/t39_catalog/` 等路径段 | `CompactGTRecipeFamilyGeneratedSupport`；profile glob；**路径参与 tree hash** | 语义目录名 + 更新 Java 常量；numbered seal 已删则不必重签旧 seal |
| Measurement | 16 个 `*MeasurementHarness.java`；`tools/t37`–`t49_materialization_measurements.json` | Gradle task 写文件；status `"T43_MATERIALIZATION_MEASUREMENT_READY"` | 输出文件名跟 harness 走；status 去卡号 |
| JUnit 方法 | `EnergyConverterCatalogTest.t18b*` / `t18c*`；`FireboxHeatBufferTest.t18BronzeProfile…` | T18 readiness token | 与 8.2 token 同一提交 |
| `src/t14Benchmark/` | 15 个已语义化的 Java 文件；目录名仍卡号 | `sourceSets.t14Benchmark` | 目录 `src/recipeLoadBenchmark/` 或类似 |
| Gradle | `t31Compat`、`run-t31-compat-*`、`t14Benchmark`、`t12CapacityMatcherBenchmark`、`t14RecipeLoad*`、`t37`–`t49*Measurements` | 开发者脚本 / CI 文档 | 语义 task 名；**先把 filter 改到现存 harness 类**（当前已断链） |
| profiles stale glob | `t46_catalog_fixture/**` | `verification_profiles.json` | 删或改到真实夹具 |

**完成（2026-09-04）：** 夹具目录、`src/recipeLoadBenchmark`、Gradle measurement 任务已改语义名；
`compileJava` / `compileTestJava` + 定向夹具 JUnit 绿。根级历史收据 JSON 与 `t*_common.py`、夹具 `stable_id` 前缀仍待清。

### 8.5 非计划文档

豁免外的 Markdown / README 去掉卡号**文件名**；正文里历史叙述改成 slug 或删。
`docs/history/card-plans/closed/**` 可继续出现 TXX；`card-plans/active/` 与
`card-plans/prep/` 不行。

| 对象 | 说明 |
|---|---|
| 根 `README.md` / `README.en.md` | 关闭卡摘要里的 T13/T19/T20 等 |
| `docs/README.md`、`docs/current/{roadmap,verification,recipe-wave-workflow,unimplemented-gap,known-issues,player-guide}.md` | 运营叙述；改完后本文件应是 current 里唯一系统讲卡号残留的地方 |
| `docs/history/INDEX.md`、`path-map.json` | 随 8.1 删子树更新；INDEX 正文可保留对 closed plan 的 T 号链接 |
| `tools/README.md` | 去掉「闭卡账本文件名必须留 T 号」的过时政策 |

**完成：** 扫描器在 `docs/current`、README、`tools/*.md` 上为零（本文件批次说明里
举旧名作**例子**时，改用代码块中的历史字面量并确保扫描器对「清单内的举例」有
明确规则：要么本文件也去 token，只用 slug；要么扫描器豁免本文件。**推荐本文件
最终也只留 slug**，旧名只出现在 `card-plans/`）。

### 8.6 最终验收

```text
python tools/check_zero_milestone_names.py --quick
python -m unittest tools.tests.test_check_zero_milestone_names
python tools/verify.py dev
.\gradlew.bat compileJava compileTestJava
```

再按改动面跑定向 JUnit / `runGameTestServer -PwaveRecipes=<slug>` /
`python tools/verify.py integration --profile …`。不要把
`python tools/closeout_seal.py --check` 的 numbered-card 绿当作成功条件
（那套 seal 应已在 8.1 删除或改为 slug）。

全量 generated 树扫描只作发布前一次，不当日常门。

## 9. 验证矩阵（按批次）

| 批次 | 必跑 | 不要跑 |
|---|---|---|
| 0 扫描器 | 扫描器单测；`--quick` 冒烟 | 全量 recipe_generated |
| 1 删历史/seal | `check_markdown_links`；`verify.py dev` 文档路径 | `--write` 任何 readiness；改 frozen v2 字节（直接删） |
| 2 工具链 | 受影响 Python 模块；`verify.py integration --profile verification` 若仍存在 | 用 `--write` 刷旧 t16/t17 |
| 3 生产 ID | `compileJava`；相关 GameTest/JUnit；datagen 若动模型 | 指望旧档自动升级 |
| 4 测试/Gradle | `compileTestJava`；夹具相关 JUnit | 全量 `gradle test` 除非本批动了公共 harness |
| 5 文档 | `verify.py dev` | Gradle |
| 6 收口 | §8.6 清单 | 把 stale `--check` 当回归 |

## 10. 日常怎么认路

| 你想做的事 | 看哪里 |
|---|---|
| 加/改现行配方或机器 | 语义路径：`recipe/mortar/`、`recipe/pipe/`、`recipe/ingot_form/`、`recipe/machines/`、wave slug |
| 查某条 TXX 还在不在 | 本文件 §2 / §8；对不上再跑 `--quick` 扫描器 |
| 继续本清理 | 按 §8 批次，不要从 `build_t*.py` 抄新 API |
| 验证改动 | [verification.md](verification.md)，日常 `python tools/verify.py` |
