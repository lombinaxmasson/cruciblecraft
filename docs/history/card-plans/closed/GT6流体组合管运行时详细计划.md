# GT6 流体组合管运行时详细计划

> 计划 slug：`content/gt6-fluid-combo-pipe-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 流体组合管运行时
> 性质：真实 4/9 tank 的 `pipeQuadruple` / `pipeNonuple` BlockItem，不 alias huge。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-fluid-combo-pipe-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-fluid-pipe-runtime, content/gt6-connector-art, content/gt6-connector-alias-repair
close_target                 = runtime_ready
```

基线 prep `content/gt6-pipe-cable-baseline` 已关闭。capability `depends_on` 不得填写没有 `capability.json` 的 prep slug。不改 R0，不改基线 `identity_resolution_ledger.json`。已关闭流体 execution subset 保持历史 `keep_distinct`；当前 catalog 投影写在本卡 `combo_overlay.json`。

---

## 0. 边界

- GT6：quad `NBT_TANK_COUNT=4`、容量同 medium `aStat*6`；nonuple 9 罐、容量同 small `aStat*2`。直径 `PX_P[16]`，但贴图用 `pipequadruple` / `pipenonuple`，不是 huge。
- T8 `pipe_forms` 分母保持 282。组合管走注册门 `materials` + `combo_pipe_forms`，`PipeCatalog.MAX_RUNTIME_BLOCKS` 升到 400。
- 已有 copper iconset 导入复用，不另迁一套 GT6 art。
- 2×2 medium / 3×3 small 是 GT6 精确格，写入 datagen；Boxinator 与生存 EMI 签收留给获得格 child。
- 关闭目标 `runtime_ready`，不是 `player_complete`。不发 stand-in 配方。

## 1. 验收

- [x] 四/九罐独立存取，NBT `tanks` 列表，匹配优先再空罐
- [x] 组合管不是 huge 别名（容量、tankCount、textureKey）
- [x] catalog dummy 折到 `{material}/quadruple_fluid_pipe` 与 `nonuple_fluid_pipe`（HSLA → `hslasteel`）
- [x] R0 与基线 ledger sha256 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-fluid-combo-pipe-runtime`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
