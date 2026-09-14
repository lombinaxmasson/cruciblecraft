# GT6 缺线规运行时详细计划

> 计划 slug：`content/gt6-eu-missing-wire-gauges-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 缺线规运行时
> 性质：真实 `wireGt07/09/10/11/13/14/15` CableBlockItem，不 alias 到 `wire`/`octuple_wire`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-eu-missing-wire-gauges-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-eu-wire-cable-runtime, content/gt6-connector-art, content/gt6-connector-alias-repair
close_target                 = runtime_ready
```

基线 prep `content/gt6-pipe-cable-baseline` 已关闭。capability `depends_on` 不得填写没有 `capability.json` 的 prep slug。不改 R0，不改基线 `identity_resolution_ledger.json`。已关闭 EU execution subset 保持历史 `keep_distinct`；当前 catalog 投影写在本卡 `missing_gauge_overlay.json`。

---

## 0. 边界

- GT6：`wireGt07` 直径同 `wireGt08`（`PX_P[8]`），安培 7；其余 09/10/11/13/14/15 直径与安培同号。CC `wire` 单位 72，故 07=504、09=648、10=720、11=792、13=936、14=1008、15=1080。
- 只为注册门允许且有 GT6 电学表的材料生成。T6 `electrical_wire_forms` 分母保持 29。
- 红合金、Signalum、Lumium 永久排除 EU，不进 `ElectricalConductorCatalog`。
- `upgrade_live_item`（blue_alloy / electrotine_alloy / naquadah 2x/4x 与 YBCO cable 等）保持 dummy。
- 绝缘红石 27006/27056/27506 与危险介质不在本卡。
- 关闭目标 `runtime_ready`，不是 `player_complete`。不发 stand-in 配方。

## 1. 验收

- [x] 七种缺线规是独立 CableBlock，不是 `wire` / `octuple_wire` 别名
- [x] catalog dummy 折到 `{material}/{septuple,nonuple,decuple,undecuple,tredecuple,tetradecuple,pentadecuple}_wire`（HSLA → `hslasteel`）
- [x] 红合金 / Signalum / Lumium 不进 EU catalog
- [x] R0 与基线 ledger sha256 不变
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-eu-missing-wire-gauges-runtime`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
