# MTE 红石线详细计划

> 计划 slug：`content/mte-redstone-wire`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：MTE 红石线
> 性质：GT6 Redstone Wires 27000 / 27050 / 27500 原地实现为活 `OP.wireGt01`。
> 关闭目标：`runtime_ready`。不是 `player_complete`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = content/mte-redstone-wire
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
catalog_family               = redstone_wire
catalog_family_count         = 3
dispositions                 = upgrade_live_item = 3
landing_owned_paths           = RedstoneWireKind / RedstoneWireBlock /
                               RedstoneWireBlockEntity / RedstoneWireBlockItem；
                               ModBlocks / ModBlockEntities / ModItems；
                               GeneratedMaterialPack；
                               RedstoneWireGameTests；
                               tools/waves/content/mte-redstone-wire/**
                               tools/catalog_modern_id_map.json
                               smelter_mte_identity_catalog.json
landing_depends_on           = registry/catalog-modern-ids closed
```

权威分母是已关闭的
[`MTE 身份分母处置 R0`](../closed/MTE身份分母处置R0详细计划.md)。
本卡不得修改 1,817 行的原始 disposition。R0 仍是 `identity_only`；
runtime 按 [MTE 全量 Prep 总索引 §0.1](../prep/MTE全量Prep总索引.md) 第 3 类
`upgrade_live_item` 落地。

---

## 0. 边界

GT6 家族：`MultiTileEntityWireRedstone` / Redstone Wires。
Loader：`Loader_MultiTileEntities.java` 1893–1910。
`setTarget_(OP.wireGt01, material, MTE)`：一根物品，既是材料线也是可放置
红石连接件。RedAlloy / Signalum / Lumium **不在** `addElectricWires` 里，
CC `electrical_by_specification` 也没有它们。先前把 `red_alloy/wire`
当成 EU 线、另开 `redstone_wire/red_alloy` dummy，是误分类。

三行 `upgrade_live_item`：

| meta | 英文名 | 活 id | 范围 | 发光 |
| --- | --- | --- | --- | --- |
| 27000 | Red Alloy Wire | `red_alloy/wire` | 16 | 否 |
| 27050 | Signalum Wire | `signalum/wire` | 64 | 否 |
| 27500 | Lumium Wirelamp | `lumium/wire` | 16 | 是（`TD.Properties.GLOWING`） |

显示名仍用 GT6（Lumium 叫 Wirelamp）。物品/方块路径是 `wireGt01`。
不是原版红石粉，也不是 `CableBlock`。

绝缘 27006 / 27056 / 27506 是 laminator 产出，不在 1817 catalog，blocked。
GT6 连接件盖板不在本卡。

获得格仍 `explicitly_blocked`：折回身份不等于发明第二张 shaped / wiremill
格。等真 `wireGt01` 配方发布后，产出就是这根可放置 MTE。

---

## 1. 行为合同

- 2px 连接件，线钳开面，放置只开朝向点击面（`Gt6StyleConnections`）。
- 原版红石输入/输出；弱电与强电同值。对另一根本卡导线走内部
  `MAX_RANGE` 损耗，不走原版耦合。
- 比较器：`bind4(mRedstone / MAX_RANGE)`。
- Lumium：`getLightValue = mState`（blockstate `POWER` 0–15）。
- `RedstoneWireBlockItem` 实现 `MaterialFormItem`（材料 + `wire`），
  盖板等已有配方继续吃同一 id。
- 撤 dummy：`redstone_wire/red_alloy`、`redstone_wire/signalum`、
  `lumium/wirelamp` 不再注册。存档：旧 dummy 方块/物品缺映射变空气；
  不做 NeoForge alias。

贴图：copper icon-set `wire.png` / `wire_overlay.png`，tint 取材料色。

---

## 2. 验收

- [x] 固定 GT6 source revision `3703e40308c8c030763fd6297dea8b210d2a77b1`
- [x] 三行 identity-resolution ledger 为 `upgrade_live_item`
- [x] 活路径就是 BlockItem；dummy 不再注册
- [x] catalog 三行 `existing_item`；`catalog_modern_id_map` 允许折到 live host
- [x] GameTest：三身份、折回 `*/wire`、非原版粉、非 `CableBlock`、弱/强红石、
      lumium 光、无第二张生存配方
- [x] `python tools/verify.py integration --profile capability-runtime` 绿后
      保持 `runtime_ready`
- [x] 关闭目标 `runtime_ready`，不是 `player_complete`

全量覆盖入口：[MTE 全量 Prep 总索引](../prep/MTE全量Prep总索引.md)。
