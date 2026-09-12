# GT 树详细计划

> 计划 slug：`worldgen/gt-trees`
> 状态：已关闭 `runtime_ready`（2026-09-11）。本文件位于 `card-plans/closed/`。
> 正式名称：GT 树
> 性质：非矿 worldgen R0 的 Trees 实现卡。一次冻结并落地全部
> `WorldgenTree*` **9** 个 feature：rubber / maple / willow / bluemahoe /
> hazel / cinnamon / coconut / rainbowood / bluespruce。
> 9 个 feature ≠ 9 种木材配方。关闭目标 `runtime_ready`，不是 `player_complete`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = worldgen/gt-trees
unique_active_wave           = null
feature_count                = 9
tree_hole_count              = 3（32762 rubber / 32761 maple / 32760 rainbowood）
owns_families                = 0
new_processing_machine       = 0
prep_owned_paths             = 已并入落地 owned_paths
landing_owned_paths           = ModBlocks / ModItems / ModBlockEntities /
                               ModFeatures；
                               worldgen configured_feature / placed_feature /
                               biome_modifier；
                               GameTests
landing_depends_on           = unique-active `content/technological-parts-foundation` 已关闭
```

前置冻结：[非矿世界生成 R0](../closed/非矿世界生成R0详细计划.md)
`requires_new_runtime`。合同见
[`worldgen_contract.json`](../../../../tools/waves/portfolio/non-ore-worldgen-r0/worldgen_contract.json)。

权威边界来自
[总体规划](../../../current/roadmap.md)、
[冻结与未实现账本](../../../current/unimplemented-gap.md)、
[能力交付流程](../../../current/capability-delivery-workflow.md)
和本地 `gt6_code/gregtech6`。

---

## 0. 决策

现行 unique-active 已关闭本卡 `runtime_ready`。九棵共用同一套 sapling grow /
placed feature / biome modifier runtime；树种是 profile，不是九套机制。

已有 `GtWoodCatalog` 木板和 `wood_rubber` / `maple` / `willow` / `blue_mahoe` /
`hazel` / `cinnamon` / `cinnamonwood` / `coconutwood` / `rainbowood` /
`blue_spruce` 材料，以及 `gt_block/log_*` 散落身份。那些不是树。本卡
`new_distinct`：可生长的 sapling / log / leaves / tree hole。禁止把 catalog
原木当世界树干，禁止把 `compact_sensor_*` 或其它零件当成树内容。

`DESIGN_POLICY_OVERWORLD_ACCESS` 只读：只在主世界放置。GT6 还往 Twilight /
Erebus / Alfheim / Aether / Tropics 注册，那些维度不进本卡。

关闭目标保持 `runtime_ready`。史莱姆球→橡胶板桥不在本卡删除；树脂→橡胶的
GT6 路径要榨汁/提取机（CC 没有 squeezer）或凝固机吃乳胶，缺真格就
`explicitly_blocked`，不用熔炉或其它材料顶。

---

## 1. 分母

来源：`Loader_Worldgen.java` 608–616；生长
`BlockTreeSaplingAB` / `BlockTreeSaplingCD`；树洞
`Loader_MultiTileEntities.java` 2027–2029。

| feature | 类 | sapling | log | leaves | amount | probability |
| --- | --- | --- | --- | --- | ---: | ---: |
| `tree.rubber` | `WorldgenTreeRubber` | AB meta 0 | LogA 0 | Leaves_AB 8 | 1 | 5 |
| `tree.maple` | `WorldgenTreeMaple` | AB 1 | LogA 1 | Leaves_AB 9 | 1 | 5 |
| `tree.willow` | `WorldgenTreeWillow` | AB 2 | LogA 2 | Leaves_AB 10 | 1 | 4 |
| `tree.bluemahoe` | `WorldgenTreeBlueMahoe` | AB 3 | LogA 3 | Leaves_AB 11 | 1 | 3 |
| `tree.hazel` | `WorldgenTreeHazel` | AB 4 | LogB 0 | Leaves_AB 12 | 1 | 32 |
| `tree.cinnamon` | `WorldgenTreeCinnamon` | AB 5 | LogB 1 | Leaves_AB 13 | 1 | 3 |
| `tree.coconut` | `WorldgenTreeCoconut` | AB 6 | LogB 2 | Leaves_AB 14 | 1 | 1 |
| `tree.rainbowood` | `WorldgenTreeRainbowood` | AB 7 | LogB 3 | Leaves_AB 15 | 1 | 4 |
| `tree.bluespruce` | `WorldgenTreeBlueSpruce` | CD 0 | LogC 0 | Leaves_CD 8 | 1 | 32 |

CC 用九组独立方块，不搬 1.7.10 十六元数据打包。轴朝向用原版 `AXIS`，不要
meta 0/4/8/12 那套。

树洞（不是独立 worldgen feature）。**只有橡胶**在 sapling `grow` 里写入树干；
枫和彩虹是对着对应原木水平面 `TOOL_drill` 后才变成树洞：

| sourceId | GT6 名 | 写入时机 | 物品 | 流体 250 mB |
| ---: | --- | --- | --- | --- |
| 32762 | Rubber Resin Hole | `BlockTreeSaplingAB.grow` case 0，邻近 256 xz 去重 | `IL.Resin`（落地具名 `rubber_resin`） | `FL.Resin_Rubber`（缺流体则灌液 blocked，不得用乳胶顶） |
| 32761 | Tapped Maple | `BlockTreeLogA.onToolClick` drill，LogA maple | 无物品 | `FL.Sap_Maple`（缺流体则灌液 blocked） |
| 32760 | Tapped Rainbowood | `BlockTreeLogB.onToolClick` drill，LogB rainbowood | 无物品 | `FL.Sap_Rainbow`（CC 已有 `rainbow_sap`） |

橡胶树脂物品已经在 semantic catalog 里以
`gt_multiitem/multiitem_food_m12050` 出现，那是 leftover 食物身份，不是树洞掉落。
本卡迁 `gregtech6_w` 的 `12050.png` 到 `textures/item/tree/rubber_resin.png`，落地时用
具名 `rubber_resin`。不得继续用铁锭或其它材料贴图占位。

### 1.1 主世界生物群系（DESIGN_POLICY）

GT6 集合含大量其它模组名。本卡只映射 1.21 原版主世界生物群系：

| 树 | GT6 集合（vanilla 1.7 核） | CC 1.21 |
| --- | --- | --- |
| rubber | taiga / taigaHills / coldTaiga / megaTaiga | `taiga` / `snowy_taiga` / `old_growth_pine_taiga` / `old_growth_spruce_taiga` |
| maple | forest / forestHills | `forest` / `flower_forest` / `birch_forest` / `old_growth_birch_forest` |
| willow | swampland | `swamp` / `mangrove_swamp` |
| blue mahoe / cinnamon | jungle / jungleHills / jungleEdge | `jungle` / `sparse_jungle` / `bamboo_jungle` |
| hazel | plains | `plains` / `sunflower_plains` / `meadow` |
| coconut | beach | `beach`；椰树苗可在沙上成活（`BlockTreeSaplingAB.canBlockStay`） |
| rainbowood | `Enchanted Forest` + `nextInt(8192)==0` | 原版没有 Enchanted Forest；主世界稀有路径是 **1/8192** |
| blue spruce | extremeHills / Edge / Plus / stoneBeach | `windswept_hills` / `windswept_forest` / `windswept_gravelly_hills` / `stony_peaks` / `jagged_peaks` / `stony_shore` |

椰树 `canGenerate` 在群系同时含山脉/冰原/针叶林/沼泽/森林时返回 0。CC 用
biome modifier 白名单表达「只在海滩」，不要再抄 1.7 排除表。

放置算法对照 `WorldgenOnSurface`：chunk 内 `amount` 次打点，再
`1/probability` 命中，从天空往下碰到第一块不透明非树木方块后调用 sapling
`grow`。不要用原版 `TreeGrower` 替换 GT6 树冠/树脂孔几何。

### 1.2 树洞 runtime

空手当面收获（`MultiTileEntityTreeHole.onBlockActivated3`）：有产物则给物品
或把手持容器灌进对应流体。再生看叶片计数，周期 600 tick。橡胶孔还有
256 格邻近去重。掉落是对应原木，不是树洞物品；创造栏不展示树洞。

枫糖 / 彩虹树液：流体身份缺失则灌液 `explicitly_blocked`，树洞方块和生长仍
要做。不得用乳胶或其它流体顶枫糖/彩虹树液。彩虹树液已有
`cruciblecraft:rainbow_sap`，复用。

### 1.3 叶片掉落

对照 `BlockTreeLeavesAB.getDrops`：低概率树苗；柳/蓝木槿/榛树额外木棍；
榛子 / 椰子是食物身份。食物掉落本卡 `explicitly_blocked`（Food 卡）。木棍
只在对应材料 `rod` / `stick` 已注册时发，缺形态不发替身棍。

---

## 2. 明确不接管

- 地牢 1、行星岩 3、Center 5（已分别签发 [GT 地牢](GT地牢详细计划.md) /
  [GT 行星岩](GT行星岩详细计划.md) / [GT Center](GT中枢详细计划.md)；不要并进本卡）
- Crops / Food / Bees（榛子、椰子、蜂巢）
- 防火原木 / 梁 / 木板；斧剥树皮变梁 + `MT.Bark`
- 整树砍倒（工具卡）
- squeezer / juicer / IC2 extractor 配方；不得用熔炉把树脂烧成橡胶
- 删除 `press/slime_ball_to_rubber_plate`
- 把 `gt_block/log_*` 散落物折成可生长的树
- 额外维度 worldgen
- Sensors / Panels / Portals / 流体附件 33
- 印刷机染料、LuV–PUV1 零件

---

## 3. 贴图

新方块禁止占位图。原木侧面/顶面已有
`textures/block/gt6/iconsets/log_side_*` / `log_top_*`（橡胶、枫、柳、蓝木槿、
榛、肉桂、椰、彩虹、蓝云杉）。树苗、树叶、树洞孔/树脂面从
`gregtech6_w` 迁到 `textures/block/tree/<id>/`（或同等 CrucibleCraft 路径），
写 art manifest。彩虹叶的彩虹着色是 SOURCE_BACKED 客户端着色，不是另画九色贴图。

---

## 4. 验收

Prep 停手（分支相对 master 不含 `landing_owned_paths`）：

- 九棵生长几何对照 GT6 `grow` 的 Java/测试夹具
- 九组贴图与 manifest
- `tools/waves/prep/gt-trees/` 写下 feature / 树洞 / 生物群系映射 / 缺流体账

落地后 `runtime_ready`：

- 九个 placed feature 只在上表主世界群系生成；彩虹树另有 1/8192 稀有路径
- 树苗可骨粉/随机生长成对应 log + leaves；橡胶生长写入树脂孔；枫/彩虹原木可钻出树洞
- 橡胶树洞空手指出 `rubber_resin`；流体格按 1.2
- GameTest：九棵生长、橡胶孔放置与收获、biome filter 正反例、census 新方块
- 不得宣称 `player_complete`；不得把史莱姆球桥写成已替换

## 5. 关闭清单

- [x] 签发：九棵 feature + 三树洞 + 主世界群系映射写进本计划
- [x] Prep 冻结：九棵 grow / 树洞账 / gregtech6_w 贴图 / `tools/waves/prep/gt-trees/`；未注册
- [x] 本文件从 prep 挪到 active；`capability.json` `workflow=active`
- [x] 关闭 `runtime_ready`，不是 `player_complete`
