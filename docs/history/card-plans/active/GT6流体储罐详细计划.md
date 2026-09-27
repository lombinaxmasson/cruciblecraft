# GT6流体储罐详细计划

> 计划 slug：`content/gt6-fluid-barrel`
> 状态：进行中 `runtime_ready` / `workflow=active`。
> 本文件位于 `card-plans/active/`。
> 正式名称：GT6流体储罐详细计划
> 性质：把 GT6 Barrel、Drum、Plastic Canister 与 Logistics Tank 做成独立方块。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 美术源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = active
capability_slug              = content/gt6-fluid-barrel
unique_active_wave           = content/gt6-fluid-barrel
maturity                     = runtime_ready
workflow                     = active
depends_on                   = registry/catalog-modern-ids
close_target                 = runtime_ready
```

不改家具木桶 `MteInPlaceKind.BARREL`。那是物品大容量箱，不是流体桶。

---

## 0. 边界

- 36 个 `Loader_MultiTileEntities` 身份：14 木桶、1 塑料罐、20 个金属流体储罐、1 物流储罐。
- 行为对齐 `TileEntityBase08Barrel`：密封、软锤、扳手自动输出、活塞清空 1000 mB、温度计、放大镜、发酵、熔毁、酸/魔法/气体/等离子体/导电流体。
- 物流罐不能密封，排空后保留流体种类。
- 木桶只收 GT6 `FluidsGT.SIMPLE` 名单里的流体。
- 配方只在每个格子都 `source_exact` 时写出。20 个金属流体储罐和物流储罐已落地。木桶仍卡在未映射的 `OD.itemGlue`。塑料罐在 GT6 里没有合成。
- 贴图从本地木桶、金属储罐、塑料罐、物流储罐图集拷入 `textures/block/gt6_import/fluid_barrel/`。
- 不实现 3x3x3 多方块罐、杯、壶、电池、保温瓶。
- 装饰盖板没有接到这 36 个方块上。流体管、漏斗和龙头走流体能力。
- 没有 Thaumcraft 通量方块。魔法流体破坏后变成空气，并留下沸腾伤害。

## 1. 验收

- [ ] 36 个方块、物品、流体能力与长整型流体能力已注册
- [ ] `FluidBarrelTest` 通过
- [ ] 20 个金属流体储罐和物流储罐是源配方；木桶和塑料罐保持 blocked
- [ ] 关闭目标 `runtime_ready`
