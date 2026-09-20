# GT6 小矿世界生成

> 计划 slug：`worldgen/gt-small-ores`
> 状态：prep（不占落地锁）。unique-active 让给 `energy/cooler`。
> 本文件位于 `card-plans/prep/`。
> 正式名称：GT6 小矿世界生成
> 性质：独立 `WorldgenOresSmall` 散点（主世界 GEN_GT + 下界 GEN_NETHER）
> 与 `WorldgenColtan` 热点。GENERATE_STONE 时关掉剩余 catalog 主世界大矿脉。
> 不改 T20 `add_worldgen_catalog.json`，不撒 catalog `ItemEntity`，
> 不开 End / 行星 / GEN_GEMS 独立散点。关 `runtime_ready`，不是 `player_complete`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = worldgen/gt-small-ores
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = paused
depends_on                   = worldgen/gt-stone-layer-rocks
close_target                 = runtime_ready
```

机制已在 `src/main`。本卡让出 unique-active，冷却器对齐结束后再回来关。

## 门禁

- [ ] Loader 无模组 GEN_GT 37 行 + GEN_NETHER 19 行，41 个名字；rocksalt→sylvite
- [ ] 石层之后 `fluid_springs` 放置 `gt_small_ore`，公式 `max(1, amount/2 + random(1+amount)/2)`
- [ ] `WorldgenColtan` seed+5 gaussian×1500、range 480、3:1:1 coltan/columbite/tantalite
- [ ] 第二份 remove_features 关掉 catalog 129 条主世界大矿脉；不改已关石层卡的 5 脉名单
- [ ] 隔离 GameTest `-PwaveRecipes=worldgen/gt-small-ores`
- [x] 不以 catalog 掉落物、stand-in 配料或 T20 椭球顶小矿
- [x] `gt_small_ore` / `gt_small_bedrock_ore` 用 GT6 `METALLIC/oresmall.png`，不再和大矿共用 `ore_flecks`
