# GT6 绝缘红石获得格详细计划

> 计划 slug：`content/gt6-redstone-wire-acquisition`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 绝缘红石获得格
> 性质：按 GT6 层压机格签发 27006/27056/27506。原地 `*/cable`，不是 EU，不折到 `tin/cable`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-redstone-wire-acquisition
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-insulated-redstone-runtime
close_target                 = runtime_ready
```

不改 R0，不改基线 `identity_resolution_ledger.json`。`depends_on` 不得填写没有 `capability.json` 的 prep slug。

---

## 0. 边界

- `Loader_MultiTileEntities` 1904–1910：`ANY.Rubber` plate×1 或 foil×4 + 27000/27050/27500 → 27006/27056/27506，`RM.Laminator.addRecipe2(T, 16, 16)`。
- 活成员目前只有 `rubber`。缺 plate/foil 保持 `explicitly_blocked`，禁止 programmed_circuit。
- 原地 `red_alloy/cable`、`signalum/cable`、`lumium/cable`。不是 EU cable，不折到 `tin/cable`。
- Torch/repeater 宿主仍 blocked。
- 关闭目标 `runtime_ready`。晋级 `player_complete` 需要 EMI、重载与玩家签收，本卡不做。

## 1. 验收

- [x] red_alloy / signalum / lumium 层压机 plate 格产出对应 `*/cable`
- [x] foil×4 仅在 live `rubber/foil` 时签发
- [x] 结果不是 `tin/cable`，也不是 programmed_circuit
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-redstone-wire-acquisition`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
