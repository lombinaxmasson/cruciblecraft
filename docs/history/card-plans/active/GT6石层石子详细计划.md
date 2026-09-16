# GT6 石层石子

> 计划 slug：`worldgen/gt-stone-layer-rocks`
> 状态：unique-active，目标 `runtime_ready`。
> 本文件位于 `card-plans/active/`。
> 正式名称：GT6 石层石子
> 性质：把 GT6 `WorldgenStoneLayers` 接到主世界：按噪声把原版石头/
> 圆石/深板岩换成层立方体（黑色花岗岩等），并在不透明石面 1/128 放
> 32757（`tLastRock`）。同一扫描写无模组 `StoneLayerOres`（层内 /
> 交界 / 1/100 随机小宝石），并吃原版矿格。`BlockRockOres` 8 层致密
> 立方体计入 LAYERS 权重；meta 8 下界石英不进主世界 LAYERS，由
> `WorldgenNetherQuartz` 在下界岩里铺。`GENERATE_STONE` 时关掉主世界
> 大矿脉；GT6 `PREVENTED_ORES QUARTZ` 关掉原版下界石英矿。
> 不撒 catalog `ItemEntity`，不重开行星岩 prep。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图：层石从 `gregtech6_w` `blocks/stones/<folder>/` 拷进
> `block/gt6/stones/`；致密矿从 `iconsets/ore_*.png` 拷进
> `block/gt6/rock_ores/`。

```text
lane                         = unique-active
capability_slug              = worldgen/gt-stone-layer-rocks
unique_active_wave           = worldgen/gt-stone-layer-rocks
maturity                     = runtime_ready
workflow                     = active
depends_on                   =
close_target                 = runtime_ready
```

关闭目标不是 `player_complete`。月/火/行星岩仍在 prep。

## 门禁

- [ ] 无模组 GT6 `StoneLayer.LAYERS` 加权 131 条（Loader 123 + BlockRockOres 8）
- [ ] 列扫描替换石头/圆石/深板岩为层立方体（45 个 GT 方块）
- [ ] BlockRockOres 8 个致密立方体（煤/褐煤/盐/钾盐/铝土/油页岩/石膏/乳白石英）
- [ ] `WorldgenNetherQuartz` 在下界岩写 meta 8 下界石英；不进主世界 LAYERS
- [ ] 关掉原版下界石英矿（GT6 `PREVENTED_ORES` QUARTZ）
- [ ] 列扫描 1/128 在石/基岩/圆石面空气放置 `gt_surface_rock`
- [ ] 掉落 `tLastRock` 的 `rock`（granite_black 等），不是整表 `c:rocks`
- [ ] 7 格扫描写无模组 `StoneLayerOres`（层内 / 交界 MAP / 1/100 宝石）
- [ ] 原版矿格可被层石或层矿替换（GT6 REPLACEABLE_BLOCKS）
- [ ] `setNoDeep` 在 `minBuildHeight+24` 以下用 deepslate
- [ ] 主世界大矿脉在石层开启时移除（GT6 GENERATE_STONE）
- [ ] 隔离 GameTest `-PwaveRecipes=worldgen/gt-stone-layer-rocks`
- [x] 不以 catalog `ItemEntity` 当获得；不写 moon/mars/planet.rocks
