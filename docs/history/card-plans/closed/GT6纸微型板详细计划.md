# GT6 纸微型板

> 计划 slug：`content/gt6-paper-tiny-plate`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 纸微型板
> 性质：按 GT6 `OP.plateTiny.forceItemGeneration(MT.Paper)` 注册
> `paper:tiny_plate`，并重算切片机 overflow。不是其它板的替身。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-paper-tiny-plate
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = machines/slicer
close_target                 = runtime_ready
```

## 门禁

- [x] `paper:tiny_plate` 在 material registration gate 中
- [x] 切片机 selected 33 / overflow 0
- [x] 隔离 GameTest `paperTinyPlateItemIsRegistered`
- [ ] 不以 programmed circuit 或其它板顶格
