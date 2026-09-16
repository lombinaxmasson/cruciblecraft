# GT6 地表石子保真

> 计划 slug：`worldgen/gt-surface-rocks`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 地表石子保真
> 性质：把 T33 `surface_rock_scatter` 从整表 `c:rocks` 1/128
> 改成 GT6 `WorldgenRocks` `overworld.rocks`：每区块 2 条射线、1/3 命中，
> 放置 32757（空石头 / 燧石 / 陨铁）。物品贴图用本地 `gt6_w` `rockgt`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。
> 不重开行星岩 prep。不撒 catalog `ItemEntity`。

```text
lane                         = closed
capability_slug              = worldgen/gt-surface-rocks
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   =
close_target                 = runtime_ready
```

关闭目标不是 `player_complete`。Twilight / Tropics / Atum 不在本卡。

## 门禁

- [x] `WorldgenOnSurface` amount=2 probability=3，接触草/土/沙，跳过耕地
- [x] 放置 `gt_surface_rock`，不是随机铜/铁 `RockBlock`
- [x] 掉落石头石子 / 燧石 / 陨铁 rock 或 raw_ore
- [x] 隔离 GameTest `-PwaveRecipes=worldgen/gt-surface-rocks`
- [x] 不以 catalog `ItemEntity` 或 `c:rocks` 整表倾倒当获得
