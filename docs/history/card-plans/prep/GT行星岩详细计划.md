# GT 行星岩详细计划

> 计划 slug：`worldgen/gt-planet-rocks`
> 状态：prep 已签发（2026-09-11）。本文件位于 `card-plans/prep/`。
> 正式名称：GT 行星岩
> 性质：非矿 worldgen R0 的 Planets 实现卡。一次冻结
> `moon.rocks` / `mars.rocks` / `planet.rocks` **3** 个 feature。
> 三者共用 `MultiTileEntityRock` sourceId **32757** 放置器，不是三套机制，
> 也不是主世界 `surface_rock_scatter`。不占 unique-active。
> 关闭目标：prep 冻结分母与放置合同；**不得**靠写进现行主世界 catalog 来
> 宣称 `runtime_ready`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。
> 签发当下不创建 `capability.json`、不写 `tools/waves/prep/gt-planet-rocks/**`、
> 不改 `landing_owned_paths`、不写 live worldgen JSON。

```text
lane                         = prep
capability_slug              = worldgen/gt-planet-rocks
unique_active_wave           = null
feature_count                = 3
rock_source_id              = 32757（MultiTileEntityRock）
owns_families                = 0
new_processing_machine       = 0
prep_owned_paths             = 签发当下空；开工后才写未注册放置器源文件 /
                               本机贴图与 art manifest /
                               tools/waves/prep/gt-planet-rocks/**
landing_owned_paths           = ModBlocks / ModItems / ModBlockEntities /
                               ModFeatures；
                               worldgen configured_feature / placed_feature /
                               biome_modifier（仅额外维度，禁止主世界 catalog）；
                               census/runtime_registry_gate.json
landing_depends_on           = unique-active 关闭；
                               额外维度 runtime（GEN_MOON / GEN_MARS / GEN_PLANETS）
```

前置冻结：[非矿世界生成 R0](../closed/非矿世界生成R0详细计划.md)
`requires_new_runtime`。合同见
[`worldgen_contract.json`](../../../../tools/waves/portfolio/non-ore-worldgen-r0/worldgen_contract.json)。
同类切片：[GT 树](../closed/GT树详细计划.md)、[GT 地牢](GT地牢详细计划.md)、
[GT Center](GT中枢详细计划.md)。不要并进一张卡。

权威边界来自
[总体规划](../../../current/roadmap.md)、
[冻结与未实现账本](../../../current/unimplemented-gap.md)、
[能力交付流程](../../../current/capability-delivery-workflow.md)
和本地 `gt6_code/gregtech6`。

---

## 0. 决策

现行 unique-active 仍是技术中间件基础。本卡只签发计划；实施走 prep 分支，
合入共享注册表要等落地槽空出来。树已经占一张实施分支，本卡签发后不得立刻
再开实施分支。

`DESIGN_POLICY_OVERWORLD_ACCESS` 只读：GT6 维度是来源事实，不是现行主世界
catalog 对等。禁止把 moon / mars / planet rocks 写进
`surface_scatter.json`、`add_surface_rocks` 或任何 `#minecraft:is_overworld`
placed feature。禁止把已有 `SurfaceRockFeature` / `RockBlock` /
`c:rocks` 标签别名成这三行。

32757 在机器树里是 exclusion（`MultiTileEntityRock`），catalog 身份是
散落物，不是加工机。本卡要的是**行星表面放置器 + 陨铁战利品 NBT**，不是把
`gt_mte/mte_32757` 折成可合成机器。

陨铁材料 `meteoric_iron` 已在 CC 材料门里。战利品必须是
`OP.oreRaw` / `OP.rockGt` 的陨铁，不得换成普通铁卵石或主世界矿石。

---

## 1. 分母

来源：`Loader_Worldgen.java` 626–628。

| feature | 类 | 维度表 | 每区块射线 | 陨铁战利品 |
| --- | --- | --- | ---: | --- |
| `moon.rocks` | `WorldgenMoonRocks` | `GEN_MOON` | `1 + nextInt(2)` | `nextInt(6)==0`，再 `nextInt(4)==0` 则 `oreRaw` 否则 `rockGt` |
| `mars.rocks` | `WorldgenMarsRocks` | `GEN_MARS` | 同上 | `nextInt(10)==0`，再同样 1/4 `oreRaw` |
| `planet.rocks` | `WorldgenPlanetRocks` | `GEN_PLANETS` | 同上 | `nextInt(4)==0`，再同样 1/4 `oreRaw` |

放置算法三份源几乎相同：从 `world.height()-50` 往下找第一块不透明非空气、
非液体方块，在其上方 `easyRep` 处 `placeBlock(..., 32757, nbt)`。
液体截断。没有群系白名单。

NBT：命中战利品概率时 `ST.save(NBT_VALUE, (oreRaw|rockGt).mat(MT.MeteoricIron, 1))`；
否则 `null`（空石头）。

CC 用三个独立 placed feature（或同等维度 feature），共用一个 32757 放置
runtime。概率写进配置，不要三份复制粘贴后改错数字。

切片**不含** dump 余量里的 `aether.rocks` / `erebus.rocks` / `alfheim.rocks`。

额外维度 runtime 不存在时：本卡保持 `explicitly_blocked` 于放置，
冻结合同仍要写完。不得为了「玩家在主世界能捡到陨铁」把三行塞进主世界。

---

## 2. 明确不接管

- 树 9、地牢 1、Center 5
- `WorldgenRocks` 主世界 4 行与已关闭的 `surface_rock_scatter`
- Aether / Erebus / Alfheim rocks；其它 172 余量
- 月球/火星/行星的矿脉、空气、地形、传送门
- 把 32757 折成加工机或配方操作数
- Crops / Food / Bees / Sensors / Panels
- 印刷机染料、LuV–PUV1 零件

---

## 3. 贴图

32757 石头与陨铁 `oreRaw` / `rockGt` 用 GT6 原图，迁到 CrucibleCraft 路径，
写 art manifest。禁止用主世界卵石或 `multiblock_casing` 占位。
已有 `meteoric_iron` 形态贴图可复用，不要另画一套假陨铁。

---

## 4. 验收

Prep 停手（分支相对 master 不含 `landing_owned_paths`）：

- 三行 feature、维度表、射线次数、三档陨铁概率写进
  `tools/waves/prep/gt-planet-rocks/`
- 写明「禁止主世界 catalog」与 32757 / 陨铁形态对照
- 未注册；无 live overworld JSON

落地（额外维度 runtime 齐之后）`runtime_ready`：

- 三个 feature 只在对应维度生成；census 不含主世界 placed feature
- 空石头 vs 陨铁 `oreRaw`/`rockGt` 按上表概率
- GameTest：概率夹具、禁止 overworld biome modifier、census
- 不得宣称 `player_complete`；不得宣称月球/火星已可生存抵达

## 5. 关闭清单

- [x] 签发：3 个 feature + 32757 放置合同 + 主世界禁写写进本计划
- [ ] Prep 冻结：放置器 / 概率账 / 贴图 / `tools/waves/prep/gt-planet-rocks/`；未注册
- [ ] 本文件从 prep 经 active 挪到 closed
- [ ] `capability.json` 在晋升落地时创建，`workflow=active`
- [ ] 额外维度未齐时不得关 `runtime_ready`；禁止用主世界生成顶维度
