# GT6 EU 线缆获得格详细计划

> 计划 slug：`content/gt6-eu-cable-acquisition`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 EU 线缆获得格
> 性质：按 GT6 精确格补 EU 裸线 / 绝缘线生存获得。红合金、Signalum、Lumium 永久排除 EU。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-eu-cable-acquisition
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-eu-wire-cable-runtime, content/gt6-eu-missing-wire-gauges-runtime
close_target                 = runtime_ready
```

不改 R0，不改基线 `identity_resolution_ledger.json`。`depends_on` 不得填写没有 `capability.json` 的 prep slug。

---

## 0. 边界

- `wireGt01` 工作台：`OreProcessing_CraftFrom` `"Px"`（plate + wirecutter）。缺 plate 保持 `explicitly_blocked`，禁止 programmed_circuit。
- `cableGt01/02` shapeless：对应线规 + `plate.dat(ANY.Rubber)` → live tag `cruciblecraft:any_rubber_plates`。不是 EU 以外的绝缘红石。
- 线规 packing：`AdvancedCraftingXToY` 仅当 `tAmount < 10`；unpack `AdvancedCrafting1ToY` 总是。只为注册门允许且有电学表的材料生成。
- 已有 wiremill `ingot_to_wire` 与 assembler `wire_and_rubber_to_cable` 是 GT6 机器获得路径，不另发明替身。
- 红合金 / Signalum / Lumium 永久排除 EU。
- 关闭目标 `runtime_ready`。晋级 `player_complete` 需要 EMI、重载与玩家签收，本卡不做。

## 1. 验收

- [x] copper plate + cutter 合成 `copper/wire`；不以 programmed_circuit 顶格
- [x] copper wire + rubber plate 合成 `copper/cable`
- [x] copper 两根 1x 合成 double；double unpack 回两根 1x
- [x] red_alloy 没有 EU plate2wire / shapeless cable
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-eu-cable-acquisition`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
