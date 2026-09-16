# GT6 石层石子

> 计划 slug：`worldgen/gt-stone-layer-rocks`
> 状态：unique-active，目标 `runtime_ready`。
> 本文件位于 `card-plans/active/`。
> 正式名称：GT6 石层石子
> 性质：把 GT6 `WorldgenStoneLayers` 接到主世界：按噪声把原版石头/
> 圆石/深板岩换成层立方体（黑色花岗岩等），并在不透明石面 1/128 放
> 32757（`tLastRock`）。不生成 `StoneLayerOres`，不撒 catalog
> `ItemEntity`，不重开行星岩 prep。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图：从 `gregtech6_w` `blocks/stones/<folder>/{stone,cobble,cobble_mossy}.png` 拷进已有族 `block/gt6/stones/`。

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

- [ ] 无模组 GT6 `StoneLayer.LAYERS` 加权 123 条，噪声选层
- [ ] 列扫描替换石头/圆石/深板岩为层立方体（45 个 GT 方块）
- [ ] 列扫描 1/128 在石/基岩/圆石面空气放置 `gt_surface_rock`
- [ ] 掉落 `tLastRock` 的 `rock`（granite_black 等），不是整表 `c:rocks`
- [ ] 不写 `StoneLayerOres`，不替换原版矿石格
- [ ] y<24 的 `setNoDeep` 层用 deepslate
- [ ] 隔离 GameTest `-PwaveRecipes=worldgen/gt-stone-layer-rocks`
- [x] 不以 catalog `ItemEntity` 当获得；不写 moon/mars/planet.rocks
