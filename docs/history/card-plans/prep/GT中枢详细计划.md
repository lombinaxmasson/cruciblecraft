# GT Center 详细计划

> 计划 slug：`worldgen/gt-center`
> 状态：prep 已签发（2026-09-11）。本文件位于 `card-plans/prep/`。
> 正式名称：GT Center
> 性质：非矿 worldgen R0 的 Center 实现卡。一次冻结
> `center.biomes` / `center.streets` / `center.nexus` / `center.beacon` /
> `center.testing` **5** 个 feature。dump 全部 `enabled: false`；
> **enabled=false 不是 skip**。五条共用原点枢纽与 `GENERATE_*` 开关，
> 不是五张卡。不占 unique-active。关闭目标 `runtime_ready`（生成器存在且
> 默认保持关闭），不是 `player_complete`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。
> 签发当下不创建 `capability.json`、不写 `tools/waves/prep/gt-center/**`、
> 不改 `landing_owned_paths`、不写 live worldgen JSON。

```text
lane                         = prep
capability_slug              = worldgen/gt-center
unique_active_wave           = null
feature_count                = 5
dump_enabled                 = false × 5（不是 skip）
owns_families                = 0
new_processing_machine       = 0
prep_owned_paths             = 签发当下空；开工后才写未注册枢纽源文件 /
                               本机贴图与 art manifest /
                               tools/waves/prep/gt-center/**
landing_owned_paths           = ModBlocks / ModItems / ModBlockEntities /
                               ModFeatures；
                               区块生成 / 结构或等价枢纽 runtime；
                               census/runtime_registry_gate.json
landing_depends_on           = unique-active 关闭；
                               建议在 `worldgen/gt-trees` 落地之后再占落地锁
```

前置冻结：[非矿世界生成 R0](../closed/非矿世界生成R0详细计划.md)
`requires_new_runtime`。合同见
[`worldgen_contract.json`](../../../../tools/waves/portfolio/non-ore-worldgen-r0/worldgen_contract.json)。
同类切片：[GT 树](../closed/GT树详细计划.md)、[GT 地牢](GT地牢详细计划.md)、
[GT 行星岩](GT行星岩详细计划.md)。不要并进一张卡。

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

Loader 五个构造都是 `aDefault = F`，只挂 `GEN_OVERWORLD`（外加 GT / PFAA /
TFC 表，那些维度不进本卡）。GT6 默认关，CC 默认也关。落地是把生成器做成
可开关的 runtime，不是把出生点广场写进每个新世界。

`center.testing` 是创造测试厅（作弊工具、演示机），不是生存内容。本卡冻结
它的身份与区块坐标，生存世界默认保持关闭；不得为了「玩家能逛」而在默认
世界打开测试厅。

沥青 / 混凝土路面是枢纽方块。已有 `gt_block/asphalt_m*` 散落身份不是
`BlocksGT.Asphalt` 世界路面，禁止把街道折进那些 leftover 行。缺真格路面
就记 blocked，不得用原版黑色混凝土顶整条 GT 沥青路。

Glowtus 只出现在沼泽象限口袋，属于 Crops 切片，本卡不实现作物。

---

## 1. 分母

来源：`Loader_Worldgen.java` 646–650。Height 默认 `WD.waterLevel()+4`（约 66）。
每个类在构造里把 `mEnabled` 写进对应 `GENERATE_*`。

| feature | 类 | 开关 | 作用域 |
| --- | --- | --- | --- |
| `center.biomes` | `WorldgenCenterBiomes` | `GENERATE_BIOMES` | 区块原点 `minX,minZ ∈ [-96, 80]` 的 12×12 区块改写地形与群系 |
| `center.streets` | `WorldgenStreets` | `GENERATE_STREETS` | 出生点沥青/混凝土广场与 X/Z 轴道路；可折叠信标 |
| `center.nexus` | `WorldgenNexus` | `GENERATE_NEXUS` | 区块 `(16, -48)` 的黑曜石/传送门建筑 |
| `center.beacon` | `WorldgenBeacon` | `GENERATE_BEACON` | 街道关闭时在出生点放铁金字塔 + 4 信标；街道开着则由街道折叠 |
| `center.testing` | `WorldgenTesting` | `GENERATE_TESTING` | 区块 `(32\|48, -32\|-48)` 创造测试厅 |

群系改写（`WorldgenCenterBiomes.generate`，仅上述正方形内）：

| 条件 | 群系 | 备注 |
| --- | --- | --- |
| 街道开且 `min ∈ [-32, 16]` | river | 给广场让路 |
| nexus 开且 `(16, -48)` | plains | |
| testing 开且测试厅 2×2 | plains | |
| `minX` 或 `minZ` ∈ `{-16, 0}` | river | 四条河；写 `BlocksGT.River` 与出生点 `(0, Height+5, 0)` |
| X<0,Z<0 口袋 `(-80\|-64, -80\|-64)` | icePlains | 冰原口袋 |
| 其余 X<0,Z<0 | coldTaiga | |
| X<0,Z≥0 口袋 `(-80\|-64, 48\|64)` | forest | |
| 其余 X<0,Z≥0 | plains | 可放 32757/32756 石头与花草 |
| X≥0,Z<0 口袋 `(48\|64, -80\|-64)` | mesa | |
| 其余 X≥0,Z<0 | desert | |
| X≥0,Z≥0 口袋 `(48\|64, 48\|64)` | swampland | 含 Glowtus；作物身份 blocked |
| 其余 X≥0,Z≥0 | jungle | |

1.21 群系名对照原版：`icePlains` → `snowy_plains`，`coldTaiga` → `snowy_taiga`，
`mesa` → `badlands`，`swampland` → `swamp`。不得发明 GT 专属群系来顶这些口袋。

街道会清掉附近非玩家活物。地牢卡的原点避让依赖 `GENERATE_STREETS`。本卡与
地牢卡共享这个开关，不得各写一套互斥默认值。

Nexus 在街道开启时用混凝土/CFoam/玻璃；关闭时走另一套石头外观。末地门框架
若出现在源里，缺维度 runtime 则门格 blocked，不得用假传送门顶。

信标：原版 `Blocks.beacon` + 速度效果。铁金字塔用原版铁块即可（源如此）。

测试厅：源往箱子里塞创造工具与演示机。本卡只冻结坐标与「默认关」；
生存获得路径不在分母内。

---

## 2. 明确不接管

- 树 9、地牢 1、行星岩 3
- `other_features.json` 其余 172
- Crops（含 Glowtus）/ Food / Bees
- 额外维度、Portals 专卡
- 把 `gt_block/asphalt_m*` / `concrete_*` leftover 折成枢纽路面
- 默认打开测试厅或把作弊工具算进 `player_complete`
- Sensors / Panels / 印刷机染料 / LuV–PUV1 零件

---

## 3. 贴图

沥青、混凝土、CFoam、染色玻璃从 `gregtech6_w` 迁到 CrucibleCraft 路径，
写 art manifest。禁止用 `multiblock_casing` 或原版黑色混凝土当 GT 沥青。
缺路面方块则记 blocked，不画占位图。

---

## 4. 验收

Prep 停手（分支相对 master 不含 `landing_owned_paths`）：

- 五条 feature、`GENERATE_*`、区块坐标、群系口袋、dump enabled=false
  写进 `tools/waves/prep/gt-center/`
- 测试厅与 Glowtus / 传送门格记 blocked 或 out_of_scope
- 未注册；默认世界生成 JSON 不启用这五条

落地后 `runtime_ready`：

- 五个生成器可按 `GENERATE_*` 独立开关；默认 false
- 打开 biomes 时 ±96 区块口袋与河流对照上表
- 打开 streets 时出生点广场生成；testing 默认仍关
- GameTest：默认关、开关正反例、census
- 不得宣称 `player_complete`；不得把测试厅写成生存内容

## 5. 关闭清单

- [x] 签发：5 个 feature + `GENERATE_*` + enabled=false 不是 skip 写进本计划
- [ ] Prep 冻结：开关 / 坐标 / 群系口袋 / 贴图 / `tools/waves/prep/gt-center/`；未注册
- [ ] 本文件从 prep 经 active 挪到 closed
- [ ] `capability.json` 在晋升落地时创建，`workflow=active`
- [ ] 关闭 `runtime_ready`（默认关），不是 `player_complete`
