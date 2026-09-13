# GT6 流体管运行时详细计划

> 计划 slug：`content/gt6-fluid-pipe-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 流体管运行时
> 性质：五规格流体管 cadence / capacity / fail-closed，并折回精确 live BlockItem。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-fluid-pipe-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids
close_target                 = runtime_ready
```

基线 prep `content/gt6-pipe-cable-baseline` 已关闭。capability `depends_on` 不得填写没有 `capability.json` 的 prep slug。

---

## 0. 边界

- 身份子集只读自已关闭的 `identity_resolution_ledger.json` fluid 行。不改 R0，不改基线 ledger。
- 只折 `fold_live_block` 的 tiny/small/medium/large/huge 精确 live BlockItem。
- quadruple/nonuple 保持 dummy / blocked，不抬 block budget，不发 stand-in。
- 5 tick 只服务 cover pump。管间分配每 server tick，even/odd 只保留 GT6 扫描顺序。
- 去掉 `min(capacity, 8000)`。plasma/magic 拒绝填充，不得映射成 gas/acid。flammable/contact 保持 blocked。
- 关闭目标 `runtime_ready`，不是 `player_complete`。

## 1. 验收

- [x] 五规格容量等于 GT6 `aStat` 倍率，不再 8000 封顶
- [x] 管间 1 tick 即流动；cover 仍 5 tick
- [x] gas/acid/over-temp fail-closed；plasma/magic 拒绝且不记 gas/acid
- [x] chunk unload 保留储罐并关闭 discovery
- [x] 侧合同：流体/物品/EU/红石网络隔离；quadruple 未伪造
- [x] 不修改 `FluidNetworkCoreGameTests`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
