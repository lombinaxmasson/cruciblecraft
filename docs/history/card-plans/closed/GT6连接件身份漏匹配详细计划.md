# GT6 连接件身份漏匹配详细计划

> 计划 slug：`content/gt6-connector-alias-repair`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 连接件身份漏匹配
> 性质：把 HSLA 连接件 dummy 精确折到已有 `hslasteel` live BlockItem。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-connector-alias-repair
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-fluid-pipe-runtime, content/gt6-eu-wire-cable-runtime, content/gt6-connector-art
close_target                 = runtime_ready
```

基线 prep `content/gt6-pipe-cable-baseline` 已关闭。capability `depends_on` 不得填写没有 `capability.json` 的 prep slug。不改 R0，不改基线 `identity_resolution_ledger.json`。

---

## 0. 边界

- 根因：`tools/gt6_pipe_cable_baseline.py` 的 `_cc_material` 没有读取 `gt6_resolve.resolve()` 顶层 `cc_material`，退回把 `MT.HSLA` 的源名 `HSLA-Steel` slug 成 `hsla_steel`。活材料 id 是 `hslasteel`。
- 只折注册门允许、且已有 live BlockItem 的同一规格：五规格流体管 + `wire` / `triple_wire` / `quintuple_wire` / `sextuple_wire`。
- quadruple / nonuple 与缺线规保持 dummy。不抬 `PipeCatalog.MAX_RUNTIME_BLOCKS`。
- 已关闭流体/EU execution subset 与 R0 保持历史证据；当前 catalog 投影写在本卡 `alias_overlay.json`。
- 关闭目标 `runtime_ready`，不是 `player_complete`。不发获得格、不造 stand-in 配方、不另迁一套 HSLA 贴图。

## 1. 验收

- [x] 26360–26364 折到 `hslasteel/{tiny,small,fluid,large,huge}_fluid_pipe`
- [x] 28250 / 28252 / 28254 / 28255 折到 `hslasteel/{wire,triple_wire,quintuple_wire,sextuple_wire}`
- [x] 26365/26366 与未开门线规仍是 dummy
- [x] R0 与基线 ledger sha256 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-connector-alias-repair`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
