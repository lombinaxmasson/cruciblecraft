# GT6 流体管获得格详细计划

> 计划 slug：`content/gt6-fluid-pipe-acquisition`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 流体管获得格
> 性质：按 GT6 精确格补流体管生存获得。组合管 pack/unpack 落地；五规格工作台缺弯板保持 blocked。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-fluid-pipe-acquisition
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-fluid-pipe-runtime, content/gt6-fluid-combo-pipe-runtime
close_target                 = runtime_ready
```

不改 R0，不改基线 `identity_resolution_ledger.json`，不改 25 行非金属 `PipeAcquisitionRecipeCatalog`。`depends_on` 不得填写没有 `capability.json` 的 prep slug。

---

## 0. 边界

- GT6 `MultiTileEntityPipeFluid`：tiny/small/medium/large 用 `OP.plateCurved` + `s/w/z/h`；huge 用 `OP.plateDouble`。缺弯板 / 双板时 `explicitly_blocked`，禁止用平板、其它材料或 `programmed_circuit` 顶格。
- quadruple `"PP","PP"` medium、nonuple 3×3 small **always**；shapeless unpack 4 medium / 9 small。
- 已有 extruder `material_rule`（plate + shape）是 GT6 机器获得路径，不另发明工作台替身。
- 关闭目标 `runtime_ready`。晋级 `player_complete` 需要 EMI、重载与玩家签收，本卡不做。

## 1. 验收

- [x] copper quadruple 由 4 根 medium 合成；unpack 回到 4 根 medium
- [x] copper tiny 用 live `curved_plate`；copper huge 用 live `double_plate`；不以平板顶格
- [x] 非金属 25 行 catalog 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-fluid-pipe-acquisition`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
