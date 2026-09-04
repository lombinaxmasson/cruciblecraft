# 语义命名收口执行清单

配合 [`semantic-naming.md`](semantic-naming.md) 使用。本文件只记录当前工作树
的执行顺序、已知断点和验收命令；长期规则、历史背景和命名方向以主文档为准。

## 当前状态快照

- [x] 现行物流 / capability 快速扫描：零命中。
- [x] `tools/recipe_bulk` Python 语法检查通过。
- [x] 冻结 v2 identity/runtime 组合可以读取。
- [x] 现行 compact runtime manifest 构建通过。
- [x] 配方 bulk、shadow、baseline 和 runtime manifest 定向测试全绿。
- [x] 活动收据、状态字段、路径和测试引用全部收口。
- [x] 删除本轮临时脚本。

A–E 已完成。F 的定向验收（compileall、`--quick`、受影响 Python 测试、
`--check`、Gradle compile、v2 compose、冻结哈希）已通过。

全量 `--summary` 不是日常门，也不再当作清零 blocker：冻结 v2、id map、
以及更早独立阶段的编号收据会合法命中。日常只跑 `--quick`。

## A. 先固定工作树边界

- [x] 运行 `git status --short`，确认本轮只处理命名收口，不覆盖用户已有改动。
- [x] 不改 `docs/history/card-plans/**`。
- [x] 不改冻结的 v2 JSON；只修复活动 v1、delta、currentness 和测试输入。
- [x] 不重新生成巨型 Bath source；只更新其消费者、收据或明确的 currentness
      记录。
- [x] 不为旧名称增加运行时兼容别名。

## B. 修复核心 Python 依赖

### B1. 配方波次注册和生成树

- [x] `COMPILE_ORDER`、`SHADOW_ORDER`、`FORWARD_COMPILE_ORDER`、`BATH_WAVES`
      全部只使用以下语义 slug：

      `assembler/compact`
      `roaster/compact`
      `centrifuge/compact`
      `electrolyzer/compact`
      `assembler/wood`
      `smelter/stone`
      `block/object`
      `storage/lock`
      `bath/mte`
      `bath/remainder`
      `bath/identity`
      `bath/tiny-purified`

- [x] `recipe_wave(slug)`、`compile_wave(slug)`、shadow、baseline 和 runtime
      loader 的输入输出键保持一致。
- [x] block/object 的 `tree_prefixes` 只覆盖 `smelter/block` 和 `drying/block`。
- [x] `compile_readiness._byte_parity()`、baseline 和 shadow 的文件遍历都遵守
      `tree_prefixes`。
- [x] `runtime.resolved_publication_group()` 能为每个 block/object 文件得到
      正确的 semantic publication group。
- [x] dedup rule 的 Python、JSON、Java fixture 三者的 ID 和 selector 完全一致。

### B2. Identity ledger

- [x] v1 identity ledger 的活动 `source_key`、`authority` 和 wave 关联全部使用
      semantic slug。
- [x] 活动 delta 的 `wave_id`、`status`、`order`、路径和 consumed-delta 记录一致。
- [x] `candidate_keys()`、resolver、operand map 和活动 ledger 使用同一套 slug。
- [x] block/object 的 path-aware stable ID remap 同时覆盖 smelter 与 drying。
- [x] 运行时只接受新的 stable ID；旧值只允许作为测试迁移输入被
      `semantic_id_map.json` 转换。
- [x] `identity_v2.compose()` 与 `runtime_v2.compose()` 通过，并确认冻结 v2 文件
      的字节哈希没有变化。

### B3. 调度和回放入口

- [x] `build_recipe_bulk.py` 使用现行 block/object 编译入口和
      `replay_smelter_stone()`。
- [x] `cutover_readiness.py` 的 policy 路径、wave 参数和 gate 名称与当前生成树
      一致。
- [x] `recipe_bulk/replay.py`、`shadow.py`、`ordinary_wave.py`、`analyze.py`
      的状态字符串和错误信息改用语义描述。
- [x] `compile_readiness.py` 的默认说明、错误信息和 binding 输出不再依赖历史
      编号。

## C. 处理活动 JSON 和生成资源

- [x] 活动 publication policy、runtime manifest、identity ledger delta、
      compile report、load projection、receipt 和 dependency manifest 的正文
      统一使用语义路径。
- [x] publication group 统一使用：
      `cruciblecraft:<host>/<cohort>`，Bath 的细分组保留
      `exact`、`exact_multi`、`tool_head` 等语义后缀。
- [x] stable ID 统一使用：
      `cruciblecraft:<host>/<cohort>/<hex>`。
- [x] 现行 recipe/support/policy 生成根和 authority manifest 一致。
- [x] 删除或迁移残留的编号 support-generated 根；迁移后检查内容哈希、路径哈希
      和 Java 常量。
- [x] 不触碰冻结 v2 JSON，不把冻结文件重新格式化。

## D. 修复测试和 Java 侧消费者

- [x] `test_recipe_bulk.py` 改用 semantic compile order、slug identity key、
      当前模块导入和当前回放入口。
- [x] `test_compact_recipe_runtime_manifest.py` 改用 semantic publication group、
      semantic stable ID 前缀和 slug loader。
- [x] `test_recipe_wave_shadow_parity.py` 与
      `test_recipe_wave_production_baseline.py` 改用 semantic shadow order、
      生成根和写保护断言。
- [x] 修复 baseline 测试中生成文件变量引用错误。
- [x] `test_recipe_load_projection.py` 只迁移 compact wave 的 delivery phase 和
      projection ID；较早的独立阶段保持原有语义。
- [x] `test_build_global_build_identity_ledger.py`、currentness、verification DAG
      和 workflow 测试改用当前 authority/slugs。
- [x] `OrdinaryCloseoutIntegratedMeasurementHarness` 委托
      `SemanticIdMap.remapStableId()`，不保留单独的旧前缀特判。
- [x] Measurement harness 的 status、opening 字段、输出路径和 Gradle filter
      与当前类名和 semantic wave slug 一致。
- [x] 运行时 Java、GameTest、fixture loader 不再拼接旧 recipe namespace。

## E. 文档和临时文件

- [x] 更新 `semantic-naming.md` 顶部状态、审计数量和已完成批次，避免与本清单
      的当前状态冲突。
- [x] 更新 `README.md`、`README.en.md`、`docs/current/roadmap.md`、
      `docs/current/unimplemented-gap.md` 和 `tools/README.md` 的运营叙述。
- [x] 修复文档中的活动路径链接；计划文档链接保持在唯一豁免目录内。
- [x] 删除 `_finish_remaining.py` 和 `_patch_runtime.py`。
- [x] 删除后确认没有脚本、Gradle、测试或文档依赖临时入口。

## F. 分阶段验收

先运行快速、低 I/O 的检查：

```text
python -m compileall -q tools/recipe_bulk
python tools/check_zero_milestone_names.py --quick
python -m unittest tools.tests.test_check_zero_milestone_names
```

再运行 Python 受影响测试：

```text
python -m unittest tools.tests.test_recipe_bulk
python -m unittest tools.tests.test_compact_recipe_runtime_manifest
python -m unittest tools.tests.test_recipe_wave_shadow_parity
python -m unittest tools.tests.test_recipe_wave_production_baseline
python -m unittest tools.tests.test_build_global_build_identity_ledger
```

然后运行生成和 Java 检查：

```text
python tools/build_semantic_recipes.py --check
python tools/material_form_authority.py --check
python tools/verify.py integration --profile verification
.\gradlew.bat compileJava compileTestJava
```

最后只运行一次全量扫描：

```text
python tools/check_zero_milestone_names.py --summary
```

全量扫描只用于发布前验收；日常迭代使用 `--quick` 和上面的定向测试，避免重复
读取 generated 巨树。

## 收口判定

只有同时满足以下条件才算完成：

- [x] 快速扫描零命中。全量 `--summary` 不要求清零（冻结 v2 / id map / 历史收据会命中）。
- [x] `runtime.build()`、`identity_v2.compose()`、`runtime_v2.compose()` 通过。
- [x] 受影响 Python 测试全绿。
- [x] `compileJava` 和 `compileTestJava` 通过。
- [x] semantic generated roots 受到写保护。
- [x] 冻结 v2 JSON 字节哈希未变化。
- [x] 活动作者入口与 Gradle 波次参数不再接受编号 token；本轮临时脚本已删除。
