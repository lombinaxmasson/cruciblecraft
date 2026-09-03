# 通用 Source Pack 导入器 工作日志

> 计划 slug：`portfolio/generic-recipe-generator`
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`

## 2026-09-02 · 计划签发

Program 已签发。当时机器可读事实仍是上一 closing 的
`unique_active_wave = null` / `next_unassigned = true`，因为 R0 artifact 尚未
生成。不签 production lock，不发布配方，核能 Track C 仍 `started = false`。

## 2026-09-02 · R0

`python tools/build_generic_recipe_import_r0.py --write|--check`

- 冻结 `tools/source_pack_manifest.schema.json` 与
  `tools/recipe_import_spec.schema.json`
- `manual_glue_inventory.json` 按职责分类，不是行数
- 四条 combinatorial family accounted，`imported = false`，`completed = 0`
- 现有 `compile.py` / `emit.py` / `selection.py` / `resolver.py` 标
  `reuse_as_is`
- `GENERIC_RECIPE_IMPORT_R0_READY`
- `unique_active_wave = portfolio/generic-recipe-import-core`
- 未生成配方

## 2026-09-02 · Core

`python tools/build_generic_recipe_import_core.py --write|--check`

- 声明式导入模块：`source_pack.py` / `import_spec.py` / `source_import.py` /
  `spec_registry.py`；GT6 dialect 按来源系统拥有
- `python tools/build_recipe_bulk.py import-source --spec ... --write|--check`
- spec 从 `recipe_import.json` 发现，不往 `SEMANTIC_WAVES` 加 Python 行
- 无 `production_lock.json` 时 `recipe_wave()` fail closed
- `GENERIC_RECIPE_IMPORT_CORE_READY`
- `unique_active_wave = portfolio/generic-recipe-import-proof`

## 2026-09-02 · Proof

`python tools/build_generic_recipe_import_proof.py --write|--check`

- Smelter `gt.recipe.smelter#0490` exact singleton
- Mixer `sha256:088c098…` exact_multi（item/fluid/circuit）
- 接入只增加 Source Pack / Manifest / RecipeImportSpec / expected fixture output
- 负例 fail closed；两次 `--write` 零漂移
- 四条 combinatorial 仍未导入
- `GENERIC_RECIPE_IMPORT_PROOF_READY`
- `unique_active_wave = portfolio/generic-recipe-generator`

## 2026-09-02 · program close

`python tools/build_generic_recipe_import.py --write|--check`

- `GENERIC_RECIPE_IMPORT_READY`
- `import_contract.json` / `onboarding_proof.json` /
  `later_star_disposition.json`
- 四条 combinatorial 的 content owner 是
  `post_generator/combinatorial-family-intake`，`started = false`
- `portfolio/count-ceiling-kind-envelope` 仍是 telemetry / report-only，未写入
  topology 后继
- `unique_active_wave = null`；`next_unassigned = true`
- 卡计划移到 `docs/history/card-plans/closed/`
- 新文件登记进 recipes / verification owned_paths 与 builder policy
- `python -m unittest tools.tests.test_generic_recipe_import_r0 tools.tests.test_generic_recipe_import` 通过
- `import-source --check` 与四个 child `--check` current
- `verify.py dev` 已能把全部脏路径分到 profile（unmatched = []）；本工作树
  的 `run_python_tests` discovery 仍被已提交的 T48 load-benchmark 模块挡住
  （缺 `src/t48_recipe_generated`，`KeyError cruciblecraft:t48_bath_exact`），
  不是本 program 回归。`test_recipe_bulk` 的 T43 replay 同样缺
  `src/t43_recipe_generated`。`closeout-seals` 的 T47-VR freeze 因本机
  `t47_identities` 与 `composed_runtime_groups` 与冻结计数不一致而失败，
  与 importer slug 无关
- 未创建 Git commit
