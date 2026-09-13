# 未实现与尾账索引

> 这是当前缺口的人读索引，不是 production authority。
> unique-active、prep 与 `player_complete` 以
> [project-status.md](project-status.md) 为准；机器分母以各波
> `readiness.json` / `overflow.json` 和 capability JSON 为准。
> `*_READY` 只表示分母、来源或可行性已经冻结，不表示对应内容已经进游戏。

本页只保留：当前未实现、仍 blocked 的余量、非阻塞但容易误读的残余分母，
以及选择后续内容时必须知道的依赖。已经完整交付的能力不在这里展开；已关闭卡只
通过权威状态页或历史计划引用，不在本页重复做阶段日志。

## 0. 读法与当前边界

| 状态 | 含义 |
| --- | --- |
| `frozen` | 分母、来源或可行性已冻结，尚未因此获得 runtime |
| `runtime_ready` | `src/main` 主机或机制可运行，但内容、配方或玩家路径仍可能 blocked |
| `player_complete` | 生存可获得、可运行、可存档，并满足对应 production lock / census |

当前边界：

- unique-active 以 [project-status.md](project-status.md) 为准，本页不复述；
  prep 是 `machines/printer`、
  `worldgen/gt-dungeon`、`worldgen/gt-planet-rocks`、`worldgen/gt-center`，
  以及 `content/mte-prep-index` 下 12 张 MTE 家族 prep 卡。
  地牢已退回 prep，结构载体仍在仓库里，但房间几何与 GT 石材形态未完成，
  不得宣称 `runtime_ready`。
  具体状态见 [project-status.md](project-status.md)。
- 跨域审计 [GT6 管道与线缆语义重基线](../history/card-plans/closed/GT6管道与线缆语义重基线详细计划.md)
  已关闭（无 capability）。它冻结了 GTCEu 不得覆盖 GT6、四套网络隔离、
  以及 catalog 666 行 live/dummy 处置；流体/物品/EU/红石 runtime 仍须
  按该卡 envelope 另开 child，不得再把 GTCEu 合同当成 GT6。
- `player_complete` 清单只看 [project-status.md](project-status.md)，不在本页复制。
- catalog **1,817 个 MTE 身份**是独立的身份分母，不等于机器 overflow，也不等于
  1,817 个待实现机制；详见第 0.1 节。
- 实现必须另开 runtime / 内容卡，补齐 Source Pack、production lock、玩家路径、
  load、census 和 fresh capability profile；不得从冻结卡直接推导 stand-in。

权威入口：

| 用途 | 权威 |
| --- | --- |
| 当前 active / prep / `player_complete` | [project-status.md](project-status.md) |
| 状态与交付流程 | [capability-delivery-workflow.md](capability-delivery-workflow.md) |
| MTE 身份分母 | [`disposition_ledger.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json) / [`family_map.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/family_map.json) |
| 机器尾账 | 各 `tools/waves/machines/**/readiness.json`、`overflow.json` |
| GT6 语义与形态 | 本地 `gt6_code/gregtech6` 与对应 source-backed artifact |
| GT6 贴图迁移 | [gt6-art-policy.md](gt6-art-policy.md) 与本地 `gt6_referencable_port_code/gregtech6_w` |

## 0.1 分类总览（2026-09-12）

人读索引，不是 production authority。数字以各波 `readiness.json` /
`overflow.json`、[project-status.md](project-status.md)、
`tools/capabilities/**/capability.json` 以及
[`disposition_ledger.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json)
为准。本节只分类，不关卡、不改分母、不把 overflow 行数写成新机制数量。

四块：**机器配方尾账**、**MTE catalog 分母 1,817**、**LuV–PUV1 零件**、
**下一阶段内容切片**。1,817 是 `gt_mte` catalog 行，不是织机/层压机 overflow。

现行 unique-active 见 [project-status.md](project-status.md)。盖板余量已关闭 `runtime_ready`。Sensors 已关闭 `runtime_ready`。熔炉已关闭 `runtime_ready`。打磨机已关闭 `runtime_ready`。`worldgen/gt-trees` 已关闭
`runtime_ready`。地牢退回 prep，上一轮的 `runtime_ready` 声明仍撤回。技术中间件
基础已关闭 `runtime_ready`。印刷机与两张非矿 worldgen（行星岩 / Center）
仍是 prep。LuV–PUV1 不在本卡分母内。

### 当前机器配方尾账

获得格（主机能不能做出来）和配方表是两套账。技术中间件卡只接通 ULV–IV 中间件
获得格。行数是 dump / selected / overflow，不是「还要做 N 个新机制」。

| 能力 | 声明 | 主机获得 | 配方余量（权威 JSON） | 真实原因 |
| --- | --- | --- | --- | --- |
| 切片机 `machines/slicer` | `runtime_ready` | 五档 `source_exact` | dump 33 / selected 32 / overflow **1** | `paper:tiny_plate` 未注册 |
| 注射机 `machines/injector` | `runtime_ready` | 五档 `source_exact` | dump 638 / selected 103 / overflow **535**（其中 runtime operand 508） | 未映射流体（`ic2coolant` / `soda` / `thoriumsalt` 等）、shadow 行、未注册运行时操作数 |
| 织机 `machines/loom` | `runtime_ready` | 九档 `source_exact` | dump 1334 / selected 465；MTE+纤维 **232** + shadow **637** | 未映射导线/电缆 MTE、`plant_gt_fiber`、shadow 签名 |
| 层压机 `machines/laminator` | `runtime_ready` | 四档 `source_exact` | dump 498 / selected 438；MTE **54** + 运行时操作数 **6** | 未映射 MTE；6 行原木块操作数 |
| 熔融机 `machines/melter` | `runtime_ready` | 一档 `source_exact` | dump 6756 / selected 3960 / overflow **2796** | 12 行未注册的 multiitem / GT block 身份与 1 行超过 8000 mB 出液罐上限的关系已明确 blocked；其余规模 `UNVERIFIED_SCALE`，不得把 2796 抄成待办清单 |
| 纳米加工机 `machines/nanofab` | `runtime_ready` | 五档仍 `explicitly_blocked` | dump 64 / selected 7；石墨烯/MTE **12** + shadow **45** | 获得格仍缺 `IL.Comp_Laser_Gas_Ar/Kr/Xe` 与蓝宝石晶体处理器。`compact_signal_emitter_*` / `compact_sensor_*` 0–5 已存在，但 D0 / `machine_tiers.json` 文案仍写发射器/传感器 missing（过期）。石墨烯 `small_casing` / `curved_plate` 仍 blocked |
| 压力清洗机 `machines/pressure-washer` | `runtime_ready` | 四档 `source_exact` | dump 312 / selected 192 / overflow **120** | 未映射 GT 石头 |
| 打磨机 `machines/sanding` | `runtime_ready`（已关） | 四档 `source_exact` | dump 7637 / selected 7637 / overflow **0** | 工具头 remap、sharpener 所需材质形态和 14502/14512 MTE overlay 已按真实对象补齐。Grindstone `32703` 不在本卡。关闭目标不是 `player_complete` |
| 熔炉 `machines/oven` | `runtime_ready`（已关） | 四档 `source_exact` Heat_T | dump `mc.recipe.furnace` **0**；live 为原版 `SMELTING` 快照 | 烹饪油肉类加成与 XP 流体 blocked。不替换原版熔炉方块。20001–20003 不在 R0 ledger。关闭目标不是 `player_complete` |
| Sensors `content/sensors` | 已关 `runtime_ready` | 21 个 GT6 Sensor MTE | 无 RecipeMap | Electro_Meter / Tacho_Meter 与 Gibbl/质量/转速已按真实对象接入。ComputerCraft 外设仍 blocked。不得宣称 `player_complete` |
| 印刷机 `machines/printer` | **prep** | 五档 `source_exact` | dump 22 / selected **0** / overflow **22** | 全部 `fluid dye.chemical.*`，`out_of_scope_g10_dyeing`。无 live family。盖板 `compact_electric_conveyor_*` **就是** `IL.CONVEYERS` 槽 |
| 导线电缆 MTE 折回 `content/electric-wire-cable-mte-fold` | `runtime_ready` | — | Loader id mapped **259** / unmapped **61** | 只折配方操作数；mapped 不是 dummy 删除集合，须另判 live BlockItem。不等于 catalog 1,817 已折完 |

切片机 / 注射机 / 织机 **不再卡 0–5 档电机、活塞、传送带**。橡胶板当前仍走
`press/slime_ball_to_rubber_plate`（`component_rules.json`：
`survival bridge pending GT rubber-tree`）。
`machines/roll-former` 的 `railGt` **2** 行是非阻塞尾账；能力状态仍由
[project-status.md](project-status.md) 管理，不列入当前交付缺口。

### MTE catalog 分母（1,817；不是 1,817 个待实现机制）

权威：[`disposition_ledger.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json)
与 [`family_map.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/family_map.json)。
R0 计划 [MTE 身份分母处置 R0](../history/card-plans/closed/MTE身份分母处置R0详细计划.md)。
这是原 `gt_mte/mte_*`、现由 `catalog_modern_id_map.json` 定址的 catalog
**holdable 身份**，不是加工机 overflow 行数。
其中 `identity_only` 是没有行为的散落 / 展示身份，`attachment_candidate` 是仍缺
runtime 的附件行为；`realized_natively` 只说明 CC 在该领域有具名机制，
不等于该 meta 已与 live item / block 合并。
因此不得把 1,817 写成 1,817 个已实现机制，也不得把它们全部当成同一种待办。
现已新增 [MTE 全量 Prep 总索引](../history/card-plans/prep/MTE全量Prep总索引.md)
与 12 张家族 prep 卡；索引引用当前 ledger 的逐行事实，其中 1,534 行进入
后续规划，283 行保留为 `realized_natively` 审计项。prep 卡只冻结来源、
所有权、依赖、blocked 边界和 **物品重复/贴图终态（索引 §0.1）**，不创建
capability 或 runtime，也不复制 png。

`registry/catalog-modern-ids` 改名时不合并活主机，所以现在会同时存在活形态
（`{材料}/fluid_pipe` 等）与 dummy（`fluid_pipe_tile/…` 等，铁锭模型）。
这不是产品终态。JEI 双物品是债。runtime child 必须逐 meta 比较 Loader
注册、材料/class/spec、live item 与 live block。精确 live BlockItem 才完整
折回；配方只能映射到普通 item 时，仍须解决可放置身份；像但不是或没有宿主
则在现代 dummy id 上原地实现并迁 `gregtech6_w`。铁锭不是可发行美术。
`registry/catalog-modern-ids` 已关 `runtime_ready`。24 个 Bath 重叠
`existing_item` 指向 Bath 已注册 dummy；meta 25302 Osmium 现为
`item_pipe_tile/osmium_elemental`。

`content/gt6-pipe-cable-baseline` 已关闭 prep 审计（无 capability）。
权威在 [GT6 管道与线缆语义重基线](../history/card-plans/closed/GT6管道与线缆语义重基线详细计划.md)
与 `tools/waves/content/gt6-pipe-cable-baseline/`。它冻结了 GTCEu 不得覆盖
GT6、四套网络隔离、以及 catalog 666 行的 live/dummy 处置；**没有**把流体管、
物品管、EU 线或红石线晋级为新的 `player_complete`。后续 child 必须按该卡
envelope 分开落地，禁止 stand-in。

| disposition | 个数 | 含义 |
| --- | --- | --- |
| `identity_only` | **1,501** | 已注册散落物 / 展示身份（多数仍是铁锭模型）。这不是“永不折回”：管/线、能源转换机、部分加工机和储物仍须逐 meta 对 live host |
| `realized_natively` | **283** | CC 在该领域已有机制，但可能是 already_shared、精确重复或仅相似机制；不能整类自动折回 |
| `attachment_candidate` | **33** | 流体附件：浇铸口 22 / 流体龙头 3 / 喷嘴 2 / 帽喷嘴 3 / 流体漏斗 3。行为 `requires_new_runtime`；游戏里没有龙头/喷嘴/漏斗/浇铸口 |
| `deferred` | 0 | — |
| **合计** | **1,817** | `unmatched = 0` |

家族（`family_map.json`，合计 1,817）：

| 家族 | 个数 | GT6 tag |
| --- | --- | --- |
| connector | 663 | Electric Wires / Fluid Pipes / Item Pipes |
| furniture_storage | 563 | Chests / Crafting Tables / Safes / Scaffolds / Storage |
| hopper | 101 | Hoppers / Misc Tool Blocks |
| processing_machine | 86 | Automatic Tools / Basic Machines |
| crucible_foundry | 85 | Molds / Smelting Crucibles |
| energy_converter | 79 | Battery Boxes / Burning Boxes / Engines / Heaters / Steam Boilers / Turbines |
| multiblock | 74 | Multiblock Machines |
| drive | 63 | Axles and Gearboxes（活目录 63；轴 36 + 齿轮箱 9 + 旋转引擎 9 + 旋转变压器 9。CC 旋转能是另一套对象） |
| misc_tool | 39 | Misc Tool Blocks |
| fluid_attachment | 33 | Crucibles Faucets / Misc Tool Blocks |
| decorative | 24 | Panels / Ropes（Wooden Panel 23 只计一次；不是 exclusion-reclaim 的 Panels 348） |
| redstone_wire | 3 | Redstone Wires |
| extender | 2 | Extenders |
| reactor | 1 | Reactors |
| untyped | 1 | Untyped |

导线电缆折回卡只处理 **配方操作数** 里已有 CC conductor 的 Loader id
（mapped **259** / unmapped **61**），**没有**撤掉对应散落 dummy。
mapped 还包含 catalog 外 id 和只有普通 item、没有 BlockItem 的规格，不能
机械当 dummy 删除集合。连接件 runtime 只折回精确 live BlockItem；item-only
规格必须先解决 canonical item 的 BlockItem 升级。流体管、物品管同样按
meta/material/kind/size 对 PipeCatalog；未注册规格保持 dummy + 迁图。
轴/齿轮箱与红石线对 KU / 原版红石是「像但不是」，keep-both + 迁 GT6 图。
家具中的 Chest/Safe/Table/Scaffold 无宿主；Bookshelf/Crate/Locker/Drawer/
Mass Storage 先对 `storage_variants.source_legacy_id`。织机 / 层压机剩余
unmapped MTE 行是缺规格或未注册材料，不是这 1,817 里「已经折完」的证明。

283 个 `realized_natively` 的去重审计仍有明确入口：connector 46、hopper 101、
crucible_foundry 85、processing_machine 50、reactor 1。特别是
`realized_natively` 不能把材料化 Smeltery/Mold 折进单一陶瓷坩埚，也不能把
错误 kind 的加工机按名称吞掉。

### LuV–PUV1 技术零件（延期，另立专卡）

GT6 紧凑零件循环是 `VN[0..9]`：ULV, LV, MV, HV, EV, IV, **LuV, ZPM, UV, PUV1**。
盖板 `CoverComponentTiers` 同样十档。本卡冻结并落地的是 `Electric_T[0..5]`。
**LuV ≠ `wireGt07`**：LuV 是电压档 `VN[6]` / `Electric_T[6]=Ir`；`wireGt07` 是
七线规，从 **ZPM 电机** `MOTORS[7]` 才开始。

| i | VN | EU | `Electric_T` | CC 材料 | 零件身份 | 真源网格 |
| --- | --- | --- | --- | --- | --- | --- |
| 6 | LuV | 32768 | Ir | `iridium.json` 有 | 传送带/泵/机械臂盖板有；电机/活塞/力场/发射器/传感器 **无** | 盖板是历史 `programmed_circuit` 升级，不是 `MultiItemTechnological` 网格 |
| 7 | ZPM | 131072 | Os | `osmium_elemental.json` 有 | 同上 | 同上 |
| 8 | UV | 524288 | Trinitanium | `trinitanium.json` 有 | 同上 | 同上 |
| 9 | PUV1 | 2097152 | Trinaquadalloy | `trinaquadalloy.json` 有 | 同上 | 同上 |

`Electric_T[10+]` 才是 Neutronium。CC 没有 `neutronium.json`。那一截不在十档紧凑
零件循环里；不要把计划里的「LuV–OMEGA」读成 VN[9] 叫 OMEGA。OMEGA / PUV2+ 是
更后面的材料档，尚未冻结为实现卡分母。

**已具备（不够做真网格）**

- 铱 / 锇 / 三钛 / 三硅合金材料文件 live
- `circuit_ultimate`（`OD_CIRCUITS[6]`，LuV 机械臂 / 力场 / 发射器 / 传感器）
- 退火铜 `sextuple_wire`（LuV 电机 `wireGt06`）与 `octuple_wire`（`wireGt08`）
- 铱 / 锇 / 钕磁在 `material_registration_gate.json` 的 `materials` 里已有 `curved_plate`、`rod`、`long_rod`、`screw`、`rotor`、`small_gear`
- 石墨烯 live 有 `wire`（LuV 起 `CABLES_01` 改成石墨烯单线，不是普通电缆）
- 电变压器 `10045–10048` 已覆盖 IV–LuV … UV–PUV1，**不用**紧凑电机

**缺失（禁止 stand-in）**

- 三钛 / 三硅合金 live 没有 `curved_plate`（UV / PUV1 壳体）。电机、泵、发射器、传感器都要 `OP.plateCurved`
- 石墨烯 live 没有 `quadruple_wire`（LuV 发射器 `WIRES_04`）
- `gt6_prefix_mapping.json` 没有 `wireGt07` / `wireGt09` / `wireGt10` / `wireGt14`（有 01–06、08、12、16）。ZPM 电机要七线规；LuV 力场要锇 `wireGt10`；UV 力场要锇 `wireGt14`；PUV1 电机要 `wireGt09`
- `OD_CIRCUITS[7+]` 指向 Quantum；目录没有 `circuit_quantum`，也没有 `quantum` 材料
- 现有 LuV–PUV1 盖板获得式仍是上一档 + `programmed_circuit`，收回真网格必须另开卡

**当前加工主机用不到这一档。** 切片机 / 注射机 / 织机 / 印刷机 / 纳米加工机源注册都是
`Electric_T[1..5]`（最高 IV / 8192）。先做高压零件不会把机器配方尾账的 overflow 转绿，
也不会解开纳米加工机的激光气体与蓝宝石处理器。真正吃 `FIELD_GENERATORS[6–9]` 的是
晶体充电器一类能源主机，不在当前 unique-active 加工账本里。

### 下一阶段内容切片

长链（Trees → Sensors/Panels → Crops → Food → 维度/Bees/Portals/Center）仍在第 0 节。
这里只写当前分母清楚、能切成垂直切片的项。

| 项 | 分母 | 读法 |
| --- | --- | --- |
| GT 树（九棵，已关 `runtime_ready`） | `WorldgenTree*` **9**（rubber / maple / willow / bluemahoe / hazel / cinnamon / coconut / rainbowood / bluespruce） | 已关 `runtime_ready`，不是 `player_complete`。计划 [GT 树详细计划](../history/card-plans/closed/GT树详细计划.md)。9 个 feature ≠ 9 种木材配方。含三树洞。不要带地牢 1、行星岩 3、Center 5。史莱姆→橡胶板桥本卡不删 |
| GT 地牢（prep / `frozen`） | `WorldgenDungeonGT` **1**（`overworld.structure.dungeon.large`） | 结构，不是 scatter / 矿脉。计划 [GT 地牢详细计划](../history/card-plans/prep/GT地牢详细计划.md)。Structure + 多区块 Piece 已注册，但格点、typed layout、GT 石材形态和房间几何未完成；钥匙 `IL.KEYS`、跨模组图书室、跨维传送门与 MTE 房间内容仍 blocked。不得宣称 `runtime_ready` / `player_complete` |
| GT 行星岩（已签发 prep） | `moon.rocks` / `mars.rocks` / `planet.rocks` **3** | 共用 32757 放置器。计划 [GT 行星岩详细计划](../history/card-plans/prep/GT行星岩详细计划.md)。禁止写进现行主世界 catalog，不得别名 `surface_rock_scatter`。落地依赖额外维度 |
| GT Center（已签发 prep） | `center.biomes` / `streets` / `nexus` / `beacon` / `testing` **5** | dump `enabled: false` 不是 skip。计划 [GT Center 详细计划](../history/card-plans/prep/GT中枢详细计划.md)。五条共用 `GENERATE_*`。测试厅不是生存内容。默认保持关闭 |
| Sensors | 21 / 21 | 已关 `runtime_ready`。`compact_sensor_*` 仍是 `IL.SENSORS` **零件物品**，不是这 21 个 Sensor MTE |
| Crops 世界生成 | `plant.glowtus` / `plant.bush` **2** | 已冻。不要带 squeezer dump 5322。无榨汁主机。切片机 selected 32 行缺作物生存来源，但作物卡建议在 Sensors 之后、且不得和 Food/Bees 捆一张 |

### 路线结论

1. 树已关 `runtime_ready`，不宣称 `player_complete`，不把 LuV–PUV1 拉进下一张卡。
2. catalog **1,817** 个 MTE 身份已登记：1,501 个 `identity_only`、283 个
   `realized_natively`、33 个流体附件。R0 disposition 与物品去重是两条轴；
   全部 1,817 行都要逐 meta 的 live item/live block、迁图和存档处置。
   12 张 prep + 索引已冻结合同，仍不是 runtime 实现。
3. 有对应高压主机（LuV 加工机、晶体充电器等）和能源路线之前，高压零件另立专卡；
   不得用 `programmed_circuit` 或错误线规顶缺格。
4. Sensors 已关 `runtime_ready`，不宣称 `player_complete`。行星岩 / Center 已签发 prep，排队不占落地锁。
5. 同一时刻仍只允许一条 unique-active。印刷机继续留 prep，直到染料流体身份存在。
   行星岩 / Center 同样等落地槽，且行星岩还要额外维度 runtime。

---

## 1. 当前已冻结但仍有缺口

这些条目已经冻结分母，但仍有明确的 runtime、内容或玩家路径缺口。实现要另开
runtime / 内容卡，不把关闭冻结卡当成实现证明。

| 人类名 | slug / `--check` | 冻了什么 | 判定 | 可行性文件 |
| --- | --- | --- | --- | --- |
| Panels | `portfolio/exclusion-reclaim-r0` | 6 sites / 348 expanded | `requires_new_runtime` | [`feasibility.json`](../../tools/waves/portfolio/exclusion-reclaim-r0/feasibility.json) |
| Sensors | `content/sensors` | 21 / 21 | 已关 `runtime_ready`；ComputerCraft 外设仍 blocked | [Sensors详细计划](../history/card-plans/closed/Sensors详细计划.md) |
| Portals | 同上 | 19 / 19 | `requires_new_runtime` | 同上 |
| 非矿树 | `portfolio/non-ore-worldgen-r0` | `WorldgenTree*` 9 | 已关 `runtime_ready` | [`feasibility.json`](../../tools/waves/portfolio/non-ore-worldgen-r0/feasibility.json)；已关 [GT 树](../history/card-plans/closed/GT树详细计划.md) |
| 非矿地牢 | 同上 | `WorldgenDungeonGT` 1 | prep / `frozen` | 同上；prep [GT 地牢](../history/card-plans/prep/GT地牢详细计划.md)，房间内容与钥匙/传送门保持 blocked |
| 非矿行星岩 | 同上 | moon / mars / planet rocks 3 | `requires_new_runtime` | 同上；prep [GT 行星岩](../history/card-plans/prep/GT行星岩详细计划.md) |
| 非矿 Center | 同上 | biomes / streets / nexus / beacon / testing 5 | `requires_new_runtime` | 同上；prep [GT Center](../history/card-plans/prep/GT中枢详细计划.md) |
| Vanilla loader | `portfolio/vanilla-replace-r0` | `Loader_Recipes_Vanilla` | `requires_new_runtime`；纸的 MVP 已不再是缺口，熔炉 / 骨头 / Vanilla.java 后半仍 `frozen` | [`feasibility.json`](../../tools/waves/portfolio/vanilla-replace-r0/feasibility.json)；[`vanilla_replace_lock.json`](../../tools/waves/content/vanilla-replace-mvp/vanilla_replace_lock.json) |
| Replace / ASM | 同上 | `Loader_Recipes_Replace` + `Replacements` | `requires_new_runtime`；本卡未实现扫描器 / ASM，余量仍 `frozen`。**不是** `player_complete` | 同上 |
| Crops | `portfolio/crops-food-bees-r0` | `plant.glowtus` / `plant.bush` + dump `gt.recipe.squeezer` 5322（规模，非 census） | `requires_new_runtime` | [`feasibility.json`](../../tools/waves/portfolio/crops-food-bees-r0/feasibility.json) |
| Food | 同上 | dump juicer 96 + fermenter 6435（规模，非 census） | `requires_new_runtime` | 同上 |
| Bees | 同上 | `WorldgenHives` 10 + bumblequeen 80 + bumblelyzer 1440（规模，非 census） | `requires_new_runtime` | 同上 |
| 流体附件（catalog 33） | `content/mte-fluid-attachments` | catalog 浇铸口 22 / 流体龙头 3 / 喷嘴 2 / 帽喷嘴 3 / 流体漏斗 3 的**行为** | prep 已冻结，仍为 `requires_new_runtime`；游戏里仍然没有龙头 / 流体龙头 / 喷嘴 / 流体漏斗 / 浇铸口。邻接模具浇铸仍是现有简化路径；未来 runtime child 必须单独回答 host、face placement、四类行为、生存获得格，并在 dummy 现代 id 上原地替换、迁 `gregtech6_w`（禁止 alias 管道盖板）。 | [`MTE流体附件详细计划`](../history/card-plans/prep/MTE流体附件详细计划.md)；[`feasibility.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/feasibility.json) |

`GENERIC_RECIPE_IMPORT_READY` 只表示已有 host 时可以 `import-source`，不创建新的
RecipeMap，也不实现封面网或世界内容。Prep 与已完成 capability 的状态只看
[project-status.md](project-status.md)；本页只保留仍有缺口的部分。

`fuels_fluidbed` 目前只发 6 条可加载配方，另外 49 条仍在
[`tools/blocked_recipe_ledger.json`](../../tools/blocked_recipe_ledger.json) 中保持
`explicitly_blocked`：缺输入映射、缺形态 / 输出或需要 outputless fuel 模型。

印刷机 prep 当前保留为明确 blocked：`gt.recipe.printer` 的 22 行全部依赖
未映射的 `fluid dye.chemical.*`，`tools/machine_fluid_mapping.json` 将其分类为
`out_of_scope_g10_dyeing`，因此 selected family 为 0、production lock 为空。
五档主机获得格已由技术中间件基础卡按真实 `IL.CONVEYERS` 打成 `source_exact`；
GT6 里传送带零件就是盖板 `compact_electric_conveyor_*`，不再另开 `_module_` 身份。
在真实染料流体身份进入后续内容卡前，不生成空 live family，也不使用
其它流体替代。见第 0.1 节。

打磨机是 MTE 加工机身份卡的第一张 runtime child，已关闭 `runtime_ready`：
`gt.recipe.sharpener` 7637 行，selected 7637 / overflow 0，四台 Kinetic 主机 20511–20514 获得格
source-exact。工具头 remap、sharpener 材质形态和 14502/14512 MTE overlay 已补齐，overflow 为 0。
Grindstone `32703` 不在本卡。关闭目标不是 `player_complete`。
计划 [打磨机详细计划](../history/card-plans/closed/打磨机详细计划.md)。
余量回收记录见
[工具头前缀与打磨机余量回收详细计划](../history/card-plans/closed/工具头前缀与打磨机余量回收详细计划.md)。
现行 unique-active 见 [project-status.md](project-status.md)。盖板余量、Sensors 与熔炉已关闭
`runtime_ready`。

剩余的 `controller_*`、`detector_*`、redstone 盖板已由 `logistics/cover-remainder`
关闭为 `runtime_ready`；绝缘红石线宿主仍 blocked。建筑方块 identity / behavior
仍没有 owner，不能从 `cc_mechanism = none` 直接推导工作量。盖板分母与合同见
[盖板余量详细计划](../history/card-plans/closed/盖板余量详细计划.md)。

---

## 2. 尚未冻结的领域

这些领域已经被对照图点名，但还没有可直接执行的实现分母；它们不是自动下一张卡。

| 项 | 在哪 | 现状 |
| --- | --- | --- |
| 物流余量 | `redstone_torch` / `redstone_repeater` 绝缘红石线宿主 | 已关 `runtime_ready` `logistics/cover-remainder`；CC 红石线 MTE 仍是 prep。见[盖板余量详细计划](../history/card-plans/closed/盖板余量详细计划.md) |
| 配方引擎 | `ShapedCatalystRecipe` 占用包围盒 vs 玩家 2×2 | 1.21 `CraftingInput.of` 去空边后两列网格仍能 `matches()`；`canCraftInDimensions` 已要求 3×3。不是下一张卡。 |
| 计数上限与 kind envelope | capability map 的 report-only / count-ceiling 行 | 新 RecipeMap 前要先明确处理方式 |
| 蒸汽涡轮 / 冷却器 | 热交换器第一切片不等于这两类主机 | 仍需独立 runtime、配方和玩家路径 |
| 聚变 / 等离子 | 控制器生存配方、独立等离子流体和燃料 / 输出链 | 仍 blocked；不从已有 fusion 行推导 |
| 建筑方块 identity / behavior | identity、hardness、multiblock parts、decorative behavior | 仍没有当前 owner |

---

## 3. 仍未认领的 family

权威：[`leftover_later.json`](../../tools/waves/portfolio/source-capability-map-r0/leftover_later.json)。
这里只列仍需要内容卡的 family；已由现有卡消化的历史桶不再列为当前缺口。

| 桶 | 条数 | 读法 |
| --- | --- | --- |
| `later:assembler_combinatorial` + `later:electrolyzer_combinatorial` | 4 | 导入器能力已有，`content_owner = post_generator/combinatorial-family-intake`，`started = false`。要进游戏仍要内容卡 + production lock |

---

## 4. 余量数字不是待办清单

不要把这些整数抄成「还要做 N 个」：

| 数字 | 来源 | 为什么不是 todo |
| --- | --- | --- |
| T35 切走五类后 634 sites / 1,230 expanded | `t13c-exclusion-reclaim-r0` `remainder_after_slice` | 排除表余量，混着 1.x 已经做过的 Storage / 漏斗等。要当待办必须先对照现行机器树，不能整表开工 |
| `other_features.json` 190；非矿切 18 后余 172；再切 hive+plant 12 后余 160 | 两张 worldgen/crops R0 | 余量含 `WorldgenStone` 90、`WorldgenOresVanilla` 24、流体泉 16、`WorldgenRocks` 4 等。矿脉 / 油井 / 地表石子已有 T20/T33 子集。不能当 160 个新 feature |
| dump recipeCount 合计 13,373（作物卡） | squeezer+juicer+fermenter+bumble* | 规模上下文。`owns_families` 仍为 0。`gt.recipe.plantalyzer` recipeCount=0 不是分母 |
| capability map 37 行 `none` | inventory | 混合了已交付、已冻结和未认领项，不等于 37 个新机制 |

**仓储桶前缀单位换算仍缺**（不是 unique-active；T44 catalog 身份登记不等于这条完成）。
GT6 `MultiTileEntityMassStorage` 用 `mPartialUnits` / `getUnitAmount` 把同材料不同前缀并进桶里已存的那一种；CC `MassStorageHandler` 只收 `isSameItemSameComponents`。
T30 `steel_dust_funnel` 只做 dust / small_dust / tiny_dust 的 1/4/9，计划写明不做物品桶、不加 `blockDust`/`dustDiv72`。漏斗不能顶桶上的换算。另开内容卡，不要把 `storage/lock` 或漏斗当已完成。

| 族 | GT6 | CC 桶 |
| --- | --- | --- |
| 粉 | `TD.Prefix.DUST_BASED`：dust / small / tiny / div72 / blockDust | 拒收不同物品 |
| 锭 | `INGOT_BASED`：ingot / nugget / chunkGt / billet / blockIngot | 同上 |
| 线 | `WIRE_BASED`：wireGt01–16 | 同上 |
| 宝石 / 板 / 板宝石 / 碎矿 / 原矿 | gem↔blockGem、plate↔blockPlate、plateGem、crushed↔tiny、oreRaw↔blockRaw | 同上 |

料斗螺丝刀与粉尘漏斗已有独立处理；它们不替代仓储桶的前缀单位换算。

---

## 5. 工作台与手持工具余量

对照 `gt6_code/gregtech6/.../Loader_Tools.java` 与
`gregtech/items/tools/**`。不是 unique active，也不是 37 行 `none` 里的一张卡。
点击契约已抽成 `api.tool.ToolAction` / `ToolInteractable` / `ToolClick`；
后面补行为走这套，不要再 `instanceof MaterialXxxItem`。
创造栏 TOOLS 仍只放 assembler harvest 一次性，新 kind 不进创造栏是有意的。

本节只列仍缺 GT6 世界行为、配方或动作协议的工具；已经有生存路径的工具不再逐一
复述。工具行为补实现时仍以本地 GT6 源为准。

### 5.1 物品在，GT6 世界行为缺

点击 / 交互：

| 工具 | GT6 来源 | 现状 | 缺什么 |
| --- | --- | --- | --- |
| 建筑杖 | `Behavior_Builderwand` | `MaterialWorkshopToolItem` | 按品质在点击面铺同种方块 |
| 钳子 | `TOOL_pincers`；仓储 `onToolClick2` | `PINCERS` 已登记，仓储桶未回答 | 仓储部分抽出；GT6 还打模具/漏斗/堆芯/书架/龙蛋 |
| 口袋多功能 | `Behavior_Switch_Metadata` | 单一 workshop 物品 | 在刀/锯/锉/螺丝/剪线钳/剪刀/凿之间切换并继承对应动作 |
| 手钻 | `Behavior_Tool(TOOL_drill)` | workshop 物品 | 不是挖掘工具；回答 `TOOL_drill` 点击 |
| 万能锹 | `GT_Tool_UniversalSpade` | 铲 `useOn` + 撬棍 | 工作台配方（见 5.2）；还挖斧/锯类方块、铺路/水田/火把、堵漏 |
| 刀 | `TOOL_knife` | 无世界 `useOn` | 方块/实体上的刀动作 |
| 凿 | `Behavior_Tool(TOOL_chisel)` | 只当催化剂 | 回答 `TOOL_chisel` 点击 |
| 软锤 × 仓储 | `TOOL_softhammer` 倒出槽位 | 只转灯/铁轨/朝向 | 点仓储桶把内容弹出 |
| `ToolCompat` 原版表 | `gregapi/block/ToolCompat.java` | 只有灯、动力铁轨、铁轨撬棍、capability 皮搋子 | 熔炉/箱子/发射器/活塞等扳手旋转未搬 |

挖掘 / 掉落：

| 工具 | GT6 来源 | 现状 | 缺什么 |
| --- | --- | --- | --- |
| 建筑镐 | `GT_Tool_PickaxeConstruction` | 原版镐挖掘 | 非矿 ×2 速、矿石 /4、末影箱精准 |
| 斧 / 双斧 | `GT_Tool_Axe` 非潜行伐整棵 | 原版斧速 + 剥皮 | 整树砍倒（与 Trees 卡独立，原版原木也缺） |
| 硬锤 | `GT_Tool_HardHammer.convertBlockDrops` | 只当催化剂 | 挖方块走 Hammer 配方，矿变粉碎 |
| 棒 | `GT_Tool_Club.convertBlockDrops` | 无挖掘 | 石头/砖/下界岩等砸成 `rockGt` |
| 锯 | `GT_Tool_Saw` | 只当催化剂 | 伐木/叶/冰、放置树苗/工作台 |
| 锉 | `GT_Tool_File` | 只当催化剂 | 铁栅栏等更快 |
| 撬棍挖 | `GT_Tool_Crowbar.isMinableBlock` | 只有右键盖板/桶 | 其它工具挖不了的方块兜底收获 |
| 扳手拆机 | `GT_Tool_Wrench.isMinableBlock` | 只有右键连管 | 左键拆机器/活塞/漏斗/发射器一类 |
| 剪线钳挖 | `GT_Tool_WireCutter` | 只有右键连线 | 电缆/导线更快收获 |
| 修枝剪 | `GT_Tool_BranchCutter` grafter | 快砍叶 + 斧剥皮 | 剪叶出树苗（树卡未开时仍对照 GT6） |

### 5.2 配方 blocked 或未发（缺形态，禁止 stand-in）

| 工具 | 阻塞 | 读法 |
| --- | --- | --- |
| 宝石镐工作台 | `assemblies()` 无 `gem_pick`；无 `tool_head_pickaxe_gem` 头配方（GT6 用 `gemFlawed`） | 物品和近似精准在；生存合成没有 |
| 万能锹工作台 | GT6 `aUseNormalHandle=F`，头来自机器，成品格 `AT`/`Sd` | 物品可创造拿到；工作台不发 |
| 放大镜 | `AdvancedCraftingTool(MAGNIFYING_GLASS, lens)` | 物品未注册；`lens` 形态未当工具材料发 |
| 手枪 / 卡宾 / 步枪 | 格要 `plateCurved` + 燧石；还要弹药/弹道 | 整族跳过 |
| 电动工具 | LV–HV 电钻/链锯/扳手/螺丝/电锯/修剪机/搅拌器/镐钻、HV 镐 | 要电机、电池盒、弯板；整族跳过 |
| 非电链锯 / 采矿钻头成品 | `toolHeadChainsaw` / `toolHeadDrill` + 钢环/弯板 | 未注册对应手持物品 |

缺零件就保持 `blocked`，不要用电路、木棍以外的手柄或别的材料顶。

### 5.3 协议还没接到的动作

`ProvidedToolActions` 目前只有扳手/活扳手/剪线钳/螺丝/撬棍/皮搋子/软锤/钳子/万能锹→撬棍。
还没有 `KNIFE`、`CHISEL`、`DRILL`、`MAGNIFYING_GLASS`、`BUILDER_WAND`。
钳子已有枚举，仓储桶 `useTool` 只处理撬棍。

补的时候对照 `gt6_code/gregtech6` 对账，`gregtech6_w` 只当 NeoForge 译稿。

---

## 6. 本页怎么更新

- 分类总览（第 0.1 节）过期时按 readiness / overflow / capability JSON 以及
  `tools/waves/portfolio/mte-identity-disposition-r0/` 的 ledger / family_map 改总览，
  不要改 sealed R0 正文去「对齐观感」。1,817 必须作为独立分类出现，不得只写在流体附件一行。
- 新关一张冻结 R0：把类别从第 2 节搬到第 1 节，链到新的 `feasibility.json`。关闭说明第一句必须写清「游戏里仍然没有 X」，不得只写 `_READY`。
- 真做进游戏：从第 1 节删掉或改成「已由 `<slug>` 实现」，并指向那张内容卡的 production lock / census。同时改第 0.1 节对应行。
- 正式不要：在 realization 卡写 `out_of_scope`，本页改成不要，不要假装 R0 没点过名。
- 不要为了「好看」重写 sealed `growth_order.json` 或已关 R0 artifact。
- 不要再把冻结卡当成默认下一张工作。默认下一张若用户要的是进游戏的东西，开 runtime / 内容卡，或先问清楚，不要再盖一张 READY 冻结收据。
- 本页不维护已关闭计划的明细；关闭状态、`player_complete` 和 active/prep 只看
  [project-status.md](project-status.md)，实现细节回看对应 `card-plans/closed/`。
- `known-issues.md` 只记录 unload / reload / load 等关卡证明债；不要从 `*_READY`
  反推已经完成真卸 chunk。
