# CrucibleCraft 阶段档案 · 1.x 联合退出门

> 状态：`ONE_X_JOINT_EXIT_READY`（2026-09-02）
> 计划 slug：`portfolio/one-x-joint-exit`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：current execution gap = 0；deferred ledger = 0（1,817 complete +
> 28 independent post-1.x scope）；`one_x_joint_exit = false`
> Closing：六条退出条件 GREEN；load 口径 A；capability-map seed 22 行交给
> `portfolio/source-capability-map`；`one_x_joint_exit = true`；
> `unique_active_wave = null`；`next_unassigned = true`；核能 Track C
> `started = false`

## 关闭结果

- 零 family 联合验收。`owns_families = 0`。`completion_delta = 0`。未发配方，
  未改 Java hard ceiling，未重写 T35 foundation。
- R0 把 census / energy / load 标 YELLOW 并强制 replay；execution gap 与
  deferred 28 条独立 scope 已 GREEN；storage T44 sidecar 当时已 current。
- Census overlay：5,718 行 closing（implemented 5,679 + phase_deferred 11 +
  post_1x_scope 28）。execution gap = 0。`t42_owner_readiness` 只作 checkpoint。
- Energy：已选 33 行对 live 85 行 catalog 无损；`automaticKindTierCompletion`
  仍为 `false`。不是笛卡尔补全。
- Storage：T44 28/624 + logistics 1/1 仍 current。GameTest 未重跑。
- Load 口径 A：硬门只看已测量轴。count 超旧参考只报 `UNVERIFIED_SCALE` /
  `REPORT_ONLY`。未抬 `ALL_PUBLISHED_RECIPE_BUDGET`。

```text
census_disposition_owner           GREEN
energy_matrix_selected_projection  GREEN
current_recipe_execution_gap       GREEN
deferred_ledger_or_independent     GREEN (28 scoped, not RED)
storage_28_624_logistics_1_1       GREEN
load_ceiling_interpretation        GREEN (interpretation A)
```

`partial_family_count=0`。Program 本身 `owns_families=0`。不签发里程碑编号。
不把它写成 GT6 全量完成。

## 权威 artifacts

- `tools/waves/portfolio/one-x-exit-r0/`
- `tools/waves/portfolio/census-disposition-replay/`
- `tools/waves/portfolio/energy-matrix-replay/`
- `tools/waves/portfolio/storage-currentness-replay/`
- `tools/waves/portfolio/load-ceiling-interpretation/`
- `tools/waves/portfolio/one-x-joint-exit/`
  （`gap_replay.json`、`exit_condition_ledger.json`、`capability_map_seed.json`、
  `readiness.json`、`closeout_seal.json`）

`failed_gates=[]`。后继默认尚未签发的 `portfolio/source-capability-map`，未预写
其 production lock 或 nuclear census。核能 Track C 保持 `started = false`。
