# GT6 漏斗主机折回详细计划

> 计划 slug：`content/gt6-mte-hopper-host-fold`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 漏斗主机折回
> 性质：把 101 个 Hopper 身份折到 T30 活主机。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-mte-hopper-host-fold
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids
close_target                 = runtime_ready
```

不改 R0。证据是 `hopper_hopper_source_evidence.json` 的 hopper_gt6_id / queue_gt6_id 与 `hopper_variants.json` 材料投影。

---

## 0. 边界

- 50 hopper + 50 queue hopper 折到 `{material}_hopper` / `{material}_queue_hopper`。
- Dust Funnel meta 32704 折到 `steel_dust_funnel`。
- 撤 dummy，删除铁锭模型；无 NeoForge alias。

## 1. 验收

- [x] 101 行 `registry_kind=existing_item`
- [x] R0 sha256 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-mte-hopper-host-fold`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
