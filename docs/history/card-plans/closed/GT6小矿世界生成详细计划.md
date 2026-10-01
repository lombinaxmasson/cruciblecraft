# GT6 小矿世界生成

> 计划 slug：`worldgen/gt-small-ores`
> 状态：runtime_ready / accepted。本文件位于 `card-plans/closed/`。2026-09-30 冷却器关闭后重新占锁并关闭。
> 正式名称：GT6 小矿世界生成
> 性质：独立 `WorldgenOresSmall` 散点（主世界 GEN_GT + 下界 GEN_NETHER）
> 与 `WorldgenColtan` 热点。GENERATE_STONE 时关掉剩余 catalog 主世界大矿脉。
> 不改 T20 `add_worldgen_catalog.json`，不撒 catalog `ItemEntity`，
> 不开 End / 行星 / GEN_GEMS 独立散点。关 `runtime_ready`，不是 `player_complete`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = worldgen/gt-small-ores
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = worldgen/gt-stone-layer-rocks
close_target                 = runtime_ready
```

机制已在 `src/main`。本卡让出 unique-active，冷却器对齐结束后再回来关。

## 门禁

- [x] Loader 无模组 GEN_GT 37 行 + GEN_NETHER 19 行，41 个名字；rocksalt→sylvite
- [x] 石层之后 `fluid_springs` 放置 `gt_small_ore`，公式 `max(1, amount/2 + random(1+amount)/2)`
- [x] `WorldgenColtan` seed+5 gaussian×1500、range 480、3:1:1 coltan/columbite/tantalite
- [x] 第二份 remove_features 关掉 catalog 129 条主世界大矿脉；不改已关石层卡的 5 脉名单
- [x] 声明的 6 条 GameTest 在 `cruciblecraft_worldgen` 上通过。持有者挂在世界生成网格，不是 `-PwaveRecipes` 临时命名空间
- [x] 不以 catalog 掉落物、stand-in 配料或 T20 椭球顶小矿
- [x] `gt_small_ore` / `gt_small_bedrock_ore` 用 GT6 `METALLIC/oresmall.png`，不再和大矿共用 `ore_flecks`

2026-09-30 重新占锁，不改生成行为。`tools.tests.test_gt_small_ores` 7 条通过。
GameTest 过滤 `smallore,coltanhotspot,remainingoverworldlargeveins` 跑了 7 条
（含声明的 6 条），`All 7 required tests passed`。`survival_access` 仍是 `partial`。
