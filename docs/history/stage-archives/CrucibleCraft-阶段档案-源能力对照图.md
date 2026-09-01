# CrucibleCraft 阶段档案 · 源能力对照图

> 状态：`SOURCE_CAPABILITY_MAP_READY`（2026-09-02）
> 计划 slug：`portfolio/source-capability-map`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`ONE_X_JOINT_EXIT_READY`；22 行 capability_map_seed；leftover 39
> 仍挂着；核能 Track C `started = false`
> Closing：schema 冻结；inventory 113 行（22 seed + 91 new）；leftover 39
> accounted；`next_major = portfolio/generic-recipe-generator`（未签发）；
> `nuclear_started = false`；`unique_active_wave = null`；
> `next_unassigned = true`；`production_lock_for_next_major = null`

## 关闭结果

- 零 family 工具 program。`owns_families = 0`。`completion_delta = 0`。未发配方，
  未改 Java hard ceiling，未打开 `automaticKindTierCompletion`，未实现任何
  GT6 域。
- R0 冻结 7 字段 schema 和三个封闭枚举，按行字段继承 22 行 seed，收编 leftover
  39（phase_deferred 11 + post_1x_scope 28）。
- Inventory 对着 pinned dump 展开对照。对照是机制账本，不是 720841 行 recipe
  census。`dump_present = true`。
- Growth-order 把 `no_generator` 写成下一张机制轨。4 条 combinatorial `later:*`
  并入 `portfolio/generic-recipe-generator`。7 条 nuclear 与 28 条 recycling
  仍独立。核能不是自动下一张。
- Program 把对照表和增长顺序交给后继，不预写 production lock。

```text
SOURCE_CAPABILITY_MAP_R0_READY
SOURCE_CAPABILITY_INVENTORY_READY
SOURCE_CAPABILITY_GROWTH_ORDER_READY
SOURCE_CAPABILITY_MAP_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成 GT6 全量完成。

## 权威 artifacts

- `tools/waves/portfolio/source-capability-map-r0/`
- `tools/waves/portfolio/source-capability-inventory/`
- `tools/waves/portfolio/source-capability-growth-order/`
- `tools/waves/portfolio/source-capability-map/`
  （`capability_map.json`、`growth_order.json`、`readiness.json`、
  `closeout_seal.json`）
- `python tools/source_capability_map.py --check`

`failed_gates=[]`。后继建议尚未签发的 `portfolio/generic-recipe-generator`。
