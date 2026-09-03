# CrucibleCraft 阶段档案 · T13c 排除表收回 R0

> 状态：`T13C_EXCLUSION_RECLAIM_R0_READY`（2026-09-02）
> 计划 slug：`portfolio/t13c-exclusion-reclaim-r0`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`LOGISTICS_COVER_NET_R0_READY`；核能 Track C `started = false`
> Closing：五类 T13c exclusion 129/471 byte-identical 继承；Panels /
> Sensors / Portals / Batteries 可行性 `requires_new_runtime`；Reactors
> `defer_to_portfolio` → `portfolio/nuclear`；不实现 MTE；不预分配
> implementation child；`nuclear_started = false`；
> `unique_active_wave = null`；`next_unassigned = true`；
> `production_lock = null`

## 关闭结果

- 零 family 机制卡。`owns_families = 0`。`completion_delta = 0`。
  `generated_recipe_count = 0`。未发配方，未改 Java，未跑 GameTest。
- 五类从 `tools/t35_excluded_object_reclaim.json` 逐项继承：Panels 6/348、
  Sensors 21/21、Portals 19/19、Batteries 37/37、Reactors 46/46。未合并、
  拆分或改 multiplicity。全表 763/1,701 未改。capability map 6 行 T13c
  未改。
- 现有 T18 青铜转换链、T19 9/8 cover 栈、T30 hopper 与 T44 storage 都不覆盖
  这五类 MTE。缺口是机制，不是漏登记的 definition 行。
- `feasibility.json` 逐类：四类 `requires_new_runtime`；Reactors
  `defer_to_portfolio`。不预分配 per-category implementation child /
  剩余 exclusion / logistics core / combinatorial / nuclear /
  `count-ceiling-kind-envelope`。
- T13 / T35 / sealed growth-order JSON 未改写。129/471 不得写成 census
  complete，也不得写成全表 763/1,701 收回。

```text
T13C_EXCLUSION_RECLAIM_R0_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成 GT6 排除表完成。

## 权威 artifacts

- `tools/waves/portfolio/t13c-exclusion-reclaim-r0/`
  （`inherited_denominator.json`、`source_semantics.json`、
  `existing_mechanism.json`、`reclaim_contract.json`、
  `feasibility.json`、`readiness.json`、`closeout_seal.json`）
- `python tools/build_t13c_exclusion_reclaim_r0.py --check`

`failed_gates=[]`。不重写已 sealed 的 source-capability-map growth-order。
