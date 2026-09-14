# GT 作物世界生成

> 计划 slug：`worldgen/gt-crops`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT 作物世界生成
> 性质：`WorldgenGlowtus` 16 色睡莲与 `WorldgenBushes` / MTE 32759。
> 不带 squeezer dump，不折到 `lilypad_glowtus/white_glowtus`，
> 不掉落 string。关 `runtime_ready`，不是 `player_complete`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = worldgen/gt-crops
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = worldgen/gt-trees
close_target                 = runtime_ready
```

## 门禁

- [x] 16 色 glowtus BlockItem，meta 0 = black
- [x] 灌木默认 sweet_berries，可设 glow_berries / plant_gt_berry
- [x] 主世界 jungle / plains+woods 排除 frozen 生物群系修饰
- [x] 隔离 GameTest `-PgameTestNamespaces=cruciblecraft_wave_worldgen_gt_crops`
- [x] 不以 string、错误睡莲或盖板顶替
