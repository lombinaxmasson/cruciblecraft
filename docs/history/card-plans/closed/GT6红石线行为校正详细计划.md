# GT6 红石线行为校正详细计划

> 计划 slug：`content/gt6-redstone-wire-correction`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 红石线行为校正
> 性质：校正已关闭身份卡上的 sender-loss / vanilla cache / sink 合同。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-redstone-wire-correction
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = content/mte-redstone-wire
close_target                 = runtime_ready
```

已关闭的 [`MTE 红石线详细计划`](../closed/MTE红石线详细计划.md)
保持 `workflow=accepted`。本卡不重新打开身份折回、不复制红石贴图、不添加配方。

---

## 0. 边界

- 活身份仍是 `red_alloy/wire`、`signalum/wire`、`lumium/wire`。
- 损耗按发送端：`getRedstoneMinusLoss = sender.mRedstone - sender.mLoss`。
- 每 tick 重置并缓存每侧 vanilla 输入，对应 GT6 `mVanillaSides`。
- `REDSTONE_SINKS` 按 1.21 映射 TNT / powered+activator rail / noteblock /
  door / trapdoor / piston / dispenser / dropper / redstone lamp。
- 不得接入 `CableBlock`、`ENERGY`、`CableNetworkTraversal` 或 `PipeTopology`。
- 绝缘 27006 / 27056 / 27506 仍 blocked，等待独立 child。

## 1. 验收

- [x] 混材 hop 使用发送端损耗
- [x] vanilla 输入按 tick、按侧缓存
- [x] sink 不向导线回灌
- [x] 三身份仍不是 `CableBlock`，且无 ENERGY
- [x] 不修改 `RedstoneWireGameTests` 的五个 `required_test_ids`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
