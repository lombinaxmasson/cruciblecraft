# GT6 导线电缆运行时详细计划

> 计划 slug：`content/gt6-eu-wire-cable-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 导线电缆运行时
> 性质：把已有 canonical prefix 的 EU wire/cable 原地升级为 CableBlockItem，并按 GT6 规格重放电压/安培/损耗/过载。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-eu-wire-cable-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = content/electric-wire-cable-mte-fold, registry/catalog-modern-ids
close_target                 = runtime_ready
```

基线 prep `content/gt6-pipe-cable-baseline` 已关闭。capability `depends_on` 不得填写没有 `capability.json` 的 prep slug。已关闭的 `content/electric-wire-cable-mte-fold` mapped 259 只作输入证据，不是 dummy 删除集合。

---

## 0. 边界

- 身份子集只读自已关闭的 `identity_resolution_ledger.json` eu 行。不改 R0，不手改基线 ledger。
- 扩展 `ElectricalConductorCatalog` 到 GT6 `addElectricWires` 且 CC 已有 canonical prefix 的规格：`wireGt01/02/03/04/05/06/08/12/16` 与 `cableGt01/02/04/08/12`。
- 每个升级后的材料 item 必须先成为 `CableBlockItem`，再撤 dummy。
- `red_alloy` / `signalum` / `lumium` 永不进入 ElectricalConductorCatalog。
- `wireGt07/09/10/11/13/14/15` 与无 prefix 的 graphene/superconductor 形态保持 blocked，不发假线规。
- 关闭目标 `runtime_ready`，不是 `player_complete`。

## 1. 验收

- [x] tin/gold `wireGt01` 以及可映射更高线规是 live `CableBlockItem`
- [x] 精确 dummy `electric_wire/*` 折到 live host；`wireGt07` 等 keep_distinct 未伪造
- [x] 逐规格损耗/绝缘/过载与 GT6 一致；红石材料不是 EU 导体
- [x] 侧合同：EU / 流体 / 物品 / 红石网络隔离
- [x] 不把新方法写进 `CrucibleCraftGameTests`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
