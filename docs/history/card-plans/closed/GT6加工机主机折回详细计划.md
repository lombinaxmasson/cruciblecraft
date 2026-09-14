# GT6 加工机主机折回详细计划

> 计划 slug：`content/gt6-mte-processing-host-fold`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 加工机主机折回
> 性质：只折 `machine_tiers.json.sourceId == meta` 的 58 个加工机身份。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-mte-processing-host-fold
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids
close_target                 = runtime_ready
```

不改 R0。Hammer / Squeezer / Polarizer / MagSep / Laser 不混入。

---

## 0. 边界

- 只折 `machine_tiers.json.sourceId` 精确等于 R0 meta 的 58 行。
- 其余 28 行保持 dummy。
- 撤 dummy，删除铁锭模型；无 NeoForge alias。

## 1. 验收

- [x] 58 行折到 live processing BlockItem
- [x] 28 行 keep_distinct
- [x] R0 sha256 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-mte-processing-host-fold`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
