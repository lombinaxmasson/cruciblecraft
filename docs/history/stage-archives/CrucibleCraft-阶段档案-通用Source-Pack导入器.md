# CrucibleCraft 阶段档案 · 通用 Source Pack 导入器

> 状态：`GENERIC_RECIPE_IMPORT_READY`（2026-09-02）
> 计划 slug：`portfolio/generic-recipe-generator`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`SOURCE_CAPABILITY_MAP_READY`；growth-order `next_major` 已消费；
> 核能 Track C `started = false`
> Closing：schema 冻结；声明式导入权威 current；两个异构 host fixture 零注册
> parity；四条 combinatorial capability available、content deferred；
> `nuclear_started = false`；`unique_active_wave = null`；
> `next_unassigned = true`；`production_lock = null`

## 关闭结果

- 零 family 机制 program。`owns_families = 0`。`completion_delta = 0`。
  `generated_recipe_count = 0`。未发配方，未改 Java，未跑 GameTest。
- R0 冻结 Source Pack / RecipeImportSpec schema，并按职责记下手工胶水。
- Core 用数据 spec 发现导入，不维护新的 Python `WaveSpec` tuple。`--write`
  只写 `source.json` / receipt / review / `lock_candidate.json`。
- Proof 用已关闭 Smelter singleton 与 Mixer exact_multi 切片证明少写胶水。
- 四条 combinatorial family accounted，不 complete。未来内容卡 owner 是
  `post_generator/combinatorial-family-intake`，`started = false`。
- `portfolio/count-ceiling-kind-envelope` 重新评估后仍是 telemetry /
  report-only，不预分配为后继。

```text
GENERIC_RECIPE_IMPORT_R0_READY
GENERIC_RECIPE_IMPORT_CORE_READY
GENERIC_RECIPE_IMPORT_PROOF_READY
GENERIC_RECIPE_IMPORT_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成配方长尾完成。

## 权威 artifacts

- `tools/waves/portfolio/generic-recipe-generator-r0/`
- `tools/waves/portfolio/generic-recipe-import-core/`
- `tools/waves/portfolio/generic-recipe-import-proof/`
- `tools/waves/portfolio/generic-recipe-generator/`
  （`import_contract.json`、`onboarding_proof.json`、
  `later_star_disposition.json`、`readiness.json`、`closeout_seal.json`）
- `src/test/resources/generic_recipe_import/`
- `python tools/build_generic_recipe_import.py --check`

`failed_gates=[]`。不重写已 sealed 的 source-capability-map growth-order。
