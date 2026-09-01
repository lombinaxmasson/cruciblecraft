# CrucibleCraft 阶段档案 · Ordinary 尾账收口

> 状态：`ORDINARY_REMAINDER_CLOSURE_READY`（2026-09-01）
> 计划 slug：`recipe-portfolio/ordinary-remainder-closure`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：Mixer closing 334 families / 2,047 relations；deferred recycling 1,819
> Closing：current execution gap = 0；`unique_active_wave = null`

## 关闭结果

- closeout-integrity repair 抽出共享 `wave_ready`。pending / missing / zero-filled
  load 不得 `WAVE_READY` 或写 seal。Smelter/Mixer 补齐 integrated 实测后写 v2 seal；
  production lock 与 generated tree hash 不变。
- operand foundation 重建 opening R0 334/2047，建立 typed catalog 与 B0/B1
  诊断面，`owns_families=0`，不扣 gap。
- 五个 host ordinary-closure 串行关闭：

```text
drying        44 complete /   85 relations
electrolyzer  46 complete /   94 relations
centrifuge   126 complete + 2 reclass / 207 relations
autoclave     36 complete + 24 later:recycling / 349 relations
compressor    56 complete / 1284 relations
accounted    308 + 26 = 334
```

- live remainder replay = 0 families / 0 relations。opening R0 证明仍为 334/2047。
- deferred recycling = 1,843（1,819 + 24 autoclave）。deferred ledger total =
  1,845，另含 centrifuge `#0010`（`later:execution_envelope/gt6_panel`）与
  `#0207`（`later:cross_mod`）。每条新 deferred 都有 `future_owner` 与
  `recheck_condition`。inherited 1,819 未 silently discard。

`partial=0`。Program 本身 `owns_families=0`。不签发里程碑编号。

## Player path 与 runtime

每个 host child 有独立 GameTest、publication groups 与 full-mix integrated load。
Centrifuge 使用 `gt6_panel` envelope（1/6/1/6，64k tanks）。Autoclave 输入罐
4,000,000 mB，TIME 机器不要求 EUt > 0。Compressor 全 1,284 relation 做 full
equivalence，禁止 representative 抽样后记 completion。

lookup candidate p95 在 autoclave/compressor 混合 RecipeMap 上可到 2270；remainder
host 记 `UNVERIFIED_SCALE`，不改冻结的 `runtime_load_budget_policy.v3.json`。

## 权威 artifacts

- `tools/waves/ordinary-wave/closeout-integrity-repair/`
- `tools/waves/ordinary-remainder/operand-foundation/`
- `tools/waves/{drying,electrolyzer,centrifuge,autoclave,compressor}/ordinary-closure/`
- `tools/waves/recipe-portfolio/ordinary-remainder-closure/`
  （`gap_replay.json`、`deferred_ledger.json`、`readiness.json`、`closeout_seal.json`）

`failed_gates=[]`。下一张 major program 默认 `recycling/deferred-ordinary-runtime`，
未预分配 host 或 family IDs。不进入 1.x joint exit，核能 Track C 保持
`started = false`。
