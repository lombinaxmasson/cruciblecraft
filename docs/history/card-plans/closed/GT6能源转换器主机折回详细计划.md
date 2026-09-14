# GT6 能源转换器主机折回详细计划

> 计划 slug：`content/gt6-mte-converter-host-fold`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 能源转换器主机折回
> 性质：把 71 个已有 live converter BlockItem 的 R0 身份折回，撤 dummy。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-mte-converter-host-fold
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids
close_target                 = runtime_ready
```

不改 R0，不改连接件基线 `identity_resolution_ledger.json`。`depends_on` 只引用已有 capability slug。

---

## 0. 边界

- 只折 `energy_converter_tiers.json.source_id == R0 meta` 且 catalog 仍是 dummy 的 71 行。
- 6 个 Steam Turbine 与 2 个 LuV/ZPM Battery Box 保持 dummy，留给后续 runtime。
- 撤 dummy `CatalogNamedItem`，删除铁锭模型；旧 dummy 堆无 NeoForge alias，未注册 id 视为空气。
- 不迁第二套 GT6 贴图，不发获得格，不造 stand-in 配方。

## 1. 验收

- [x] 71 行 `registry_kind=existing_item` 指向 live converter BlockItem
- [x] 8 行 keep_distinct 仍是 dummy
- [x] R0 与基线 ledger sha256 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-mte-converter-host-fold`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
