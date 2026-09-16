# GT6 石层石子

> 计划 slug：`worldgen/gt-stone-layer-rocks`
> 状态：unique-active，目标 `runtime_ready`。
> 本文件位于 `card-plans/active/`。
> 正式名称：GT6 石层石子
> 性质：把 GT6 `WorldgenStoneLayers` 在不透明石面 1/128 放置的 32757
> （`tLastRock` / 层表面材料，如黑色花岗岩）接到主世界。
> 不替换原版石头立方体，不生成 `StoneLayerOres`，不撒 catalog
> `ItemEntity`，不重开行星岩 prep。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图：复用已导入的 `rockgt`；本卡无新占位图。

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
- [ ] 列扫描 1/128 在石/基岩/圆石面空气放置 `gt_surface_rock`
- [ ] 掉落 `tLastRock` 的 `rock`（granite_black 等），不是整表 `c:rocks`
- [ ] 不替换石头立方体，不写 `StoneLayerOres`
- [ ] y<24 的 `setNoDeep` 层用 deepslate
- [ ] 隔离 GameTest `-PwaveRecipes=worldgen/gt-stone-layer-rocks`
- [x] 不以 catalog `ItemEntity` 当获得；不写 moon/mars/planet.rocks
