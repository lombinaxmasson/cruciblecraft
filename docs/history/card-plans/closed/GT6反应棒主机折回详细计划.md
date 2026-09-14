# GT6 反应棒主机折回详细计划

> 计划 slug：`content/gt6-mte-reactor-rod-host-fold`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 反应棒主机折回
> 性质：把 reactor meta 9203 折到已有 `neutron_reflector_rod`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-mte-reactor-rod-host-fold
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids
close_target                 = runtime_ready
```

不改 R0。不要把 `materials/desh.json` 的 `source_id: 9203` 当成这根棒。

---

## 0. 边界

- 只折 family `reactor` meta 9203 到 `neutron_reflector_rod`。
- 撤 dummy `neutron/reflector_rod`，删除铁锭模型。

## 1. 验收

- [x] catalog `existing_item` → `neutron_reflector_rod`
- [x] R0 sha256 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-mte-reactor-rod-host-fold`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
