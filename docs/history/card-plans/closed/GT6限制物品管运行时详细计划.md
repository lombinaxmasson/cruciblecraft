# GT6 限制物品管运行时详细计划

> 计划 slug：`content/gt6-restrictive-item-pipe-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 限制物品管运行时
> 性质：真实 `pipeRestrictiveMedium/Large/Huge` BlockItem，`stepSize ×100`，restrictor 层，不 alias 普通物品管。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-restrictive-item-pipe-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-item-pipe-runtime, content/gt6-connector-art, content/gt6-connector-alias-repair
close_target                 = runtime_ready
```

基线 prep `content/gt6-pipe-cable-baseline` 已关闭。capability `depends_on` 不得填写没有 `capability.json` 的 prep slug。不改 R0，不改基线 `identity_resolution_ledger.json`。已关闭物品 execution subset 保持历史 `keep_distinct`；当前 catalog 投影写在本卡 `restrictive_overlay.json`。Loader-out 57 行单独冻结分母，不 dummy 折回。

---

## 0. 边界

- GT6：medium `aStepSize*100`，large `*50`，huge `*25`，相对同规格普通管都是 ×100。前缀单位 medium `U*3`=432、large `U*6`=864、huge `U*12`=1728。
- Catalog 内 6 行：`elven_elementium` / `vibranium_silver` × medium/large/huge。先折这 6 行。
- T8 `pipe_forms` 分母保持 282。restrictive 走注册门 `materials` + `restrictive_pipe_forms`，`PipeCatalog.MAX_RUNTIME_BLOCKS` 升到 500。
- 已导入 `iconsets/pipe_restrictor.png` 复用，不另迁一套 GT6 art。贴图键 `restrictive_8/12/16`，不得与普通 `"8"` 模型共用。
- Hammer + `OP.ring.dat(ANY.Steel)` 是 GT6 精确格，留给获得格 child。不发 stand-in 配方。
- 关闭目标 `runtime_ready`，不是 `player_complete`。

## 1. 验收

- [x] 普通同规格 `stepSize ×100`；stacksPerSecond 不变
- [x] 不是普通物品管别名（textureKey、stepSize、独立 BlockItem）
- [x] catalog dummy 折到 `{material}/{restrictive,large_restrictive,huge_restrictive}_item_pipe`
- [x] Loader-out 57 行冻结，不折 dummy
- [x] R0 与基线 ledger sha256 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-restrictive-item-pipe-runtime`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
