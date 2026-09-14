# GT6 绝缘红石运行时详细计划

> 计划 slug：`content/gt6-insulated-redstone-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 绝缘红石运行时
> 性质：27006/27056/27506 在现代 id `*/cable` 原地成为 `RedstoneWireBlockItem`，不是 EU cable，也不折到 `tin/cable`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-insulated-redstone-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/mte-redstone-wire, content/gt6-redstone-wire-correction
close_target                 = runtime_ready
```

不改 R0，不改基线 `identity_resolution_ledger.json`，不改已关闭 `content/mte-redstone-wire` 的 `out_of_denominator`。`depends_on` 不得填写没有 `capability.json` 的 prep slug。

---

## 0. 边界

- GT6：`OP.cableGt01` → `WireRedstoneInsulated` 27006 / 27056 / 27506；直径 `PX_P[4]`，范围同裸线 16/64/16。流明绝缘线不发光（只有 27500 Wirelamp 发光）。
- 现代 id 原地：`red_alloy/cable`、`signalum/cable`、`lumium/cable`。不是 `CableBlock`，不进 `ElectricalConductorCatalog`，不 alias `tin/cable`。
- 裸线 `RedstoneWireKind.all()` / `EXPECTED_SIZE = 3` / `redstoneWireBlocksById()` 保持 3，已关闭 GameTest 继续成立。catalog 为 6。
- 层压机 plate/foil 获得格留给 acquisition child。不发 stand-in 配方。
- torch/repeater 宿主仍 blocked（`isBlockedWireHost`），盖板余量仍在物流盖板车道。
- 关闭目标 `runtime_ready`，不是 `player_complete`。

## 1. 验收

- [x] 三种绝缘电缆是 `RedstoneWireBlockItem`，4px，不是 EU `CableBlock`
- [x] 不折到 `tin/cable`，红合金 / Signalum / Lumium 不进 EU catalog
- [x] 与裸红石线同一 redstone network；流明绝缘线不发光
- [x] 已关闭红石卡 `out_of_denominator` 仍是 27006/27056/27506
- [x] R0 与基线 ledger sha256 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-insulated-redstone-runtime`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
