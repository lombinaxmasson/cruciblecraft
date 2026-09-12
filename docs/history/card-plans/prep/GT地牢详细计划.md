# GT 地牢详细计划

> 计划 slug：`worldgen/gt-dungeon`
> 状态：退回 prep（2026-09-11）。`frozen` / `workflow=paused`。
> 本文件位于 `card-plans/prep/`。上一轮的 `runtime_ready` 已撤回，也不再占
> unique-active。结构载体仍在仓库里；房间几何与 GT 石材形态未完成，不得宣称
> `runtime_ready`。适合在加工机身份落地之后再重新晋升。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。
> 落地实现使用 NeoForge `Structure` + 单个可序列化多区块
> `StructurePiece`，不把 5×5–9×9 地牢伪装成 scatter Feature。

```text
lane                         = prep
capability_slug              = worldgen/gt-dungeon
unique_active_wave           = null
feature_count                = 1
owns_families                = 0
new_processing_machine       = 0
prep_owned_paths             = tools/waves/prep/gt-dungeon/**
landing_owned_paths          = ModStructures / GtDungeonStructure /
                               GtDungeonPiece；
                               worldgen structure / structure_set；
                               GtDungeonGameTests
landing_depends_on           = unique-active 关闭后重新晋升；当前主动让位给
                               `machines/sanding`
```

前置冻结：[非矿世界生成 R0](../closed/非矿世界生成R0详细计划.md)
`requires_new_runtime`。合同见
[`worldgen_contract.json`](../../../../tools/waves/portfolio/non-ore-worldgen-r0/worldgen_contract.json)。
同类切片：[GT 树](../closed/GT树详细计划.md)、
[GT 行星岩](GT行星岩详细计划.md)、
[GT Center](GT中枢详细计划.md)。不要并进一张卡。

权威边界来自
[总体规划](../../../current/roadmap.md)、
[冻结与未实现账本](../../../current/unimplemented-gap.md)、
[能力交付流程](../../../current/capability-delivery-workflow.md)
和本地 `gt6_code/gregtech6`。

---

## 0. 决策

本卡已让出 unique-active，改走 prep。树的落地已完成，但本卡上一轮将仅有的
结构载体过早关闭为 `runtime_ready`；退回 prep 后只保留已确认的结构载体，
不扩展到其它世界生成卡，也不把未完成的房间几何写成已关闭。

`DESIGN_POLICY_OVERWORLD_ACCESS` 只读：只在主世界生成。Loader 还把
`GEN_GT` / `GEN_PFAA` / `GEN_TFC` 写进同一构造，那些维度不进本卡。

结构不是 scatter。禁止把本卡写成 `surface_rock_scatter`、矿脉 catalog
或 item scatter 的又一行。禁止用原版地牢 / 试炼密室顶 `WorldgenDungeonGT`。

目标仍是可验证的 `runtime_ready`，但在石材形态、房间调度和多区块几何
完成前不得重新关闭。钥匙、跨模组图书室、跨维度传送门缺真格就
`explicitly_blocked`，不得用绊线钩、铁锭或其它材料顶 `IL.KEYS`。

---

## 1. 分母

来源：`Loader_Worldgen.java` 652；实现
`gregapi.worldgen.dungeon.WorldgenDungeonGT`。

构造实参（`aDefault = T`，dump `enabled: true`）：

| 键 | 值 |
| --- | --- |
| name | `overworld.structure.dungeon.large` |
| Probability | 100 |
| MinSize / MaxSize | 3 / 7（房间网格边长，单位：区块） |
| MinY / MaxY | 20 / 20 |
| RoomChance | 6 |
| Overworld / Nether / End | T / F / F |
| PortalNether / End / Twilight / Aether / Myst | 全部 T |
| ZPMs | 配置默认 T |

生成闸（`generate`）：

1. `nextInt(Probability) == 0`，且不是 major worldgen 冲突。
2. 距原点 `|minX|,|minZ| < 256 + MaxSize*16`（即 **368** 格）则跳过；
   若 `GENERATE_STREETS` 开着，轴向上也要躲开同样半径。
3. 区块对齐：`abs(minX/16) % (MaxSize+4) == (MaxSize+4)/2`（默认 `% 11 == 5`），
   Z 同理。
4. 接触点必须是基岩：GT6 查 `(minX+8, 0, minZ+8)`。1.21 主世界地板不是 Y=0；
   落地时对照 `WD.bedrock` 语义迁到现行世界底，不得在 Y=0 空放一层假基岩。

房间表（`ROOMS`，不是独立 dump feature）：

| 类 | 标签 | 本卡 |
| --- | --- | --- |
| `DungeonChunkRoomWorkshop` | `gt.dungeon.workshop` | 生成结构壳；MTE 内容 blocked |
| `DungeonChunkRoomMiningBedrock` | `gt.dungeon.mining.bedrock` | blocked；无 `WorldgenOresBedrock` |
| `DungeonChunkRoomLibraryNormal` | `gt.dungeon.library.normal` | 生成结构壳；GT 书架身份 blocked |
| `DungeonChunkRoomLibraryMystcraft` | `gt.dungeon.library.mystcraft` | **blocked**（无 Myst） |
| `DungeonChunkRoomLibraryThaumcraft` | `gt.dungeon.library.thaumcraft` | **blocked**（无 Thaum） |
| `DungeonChunkRoomFarmMobs` | `gt.dungeon.farm.mobs` | 生成结构壳；农场 MTE 内容 blocked |
| `DungeonChunkRoomFarmCrop` | `gt.dungeon.farm.crop` | blocked；不得用小麦顶 GT 作物 |
| `DungeonChunkRoomFarmFish` | `gt.dungeon.farm.fish` | 生成结构壳；鱼场 MTE 内容 blocked |

死胡同（`DEAD_END`）：

| 类 | 本卡 |
| --- | --- |
| `DungeonChunkRoomStorage` | blocked；存储/战利品 MTE 未注册 |
| `DungeonChunkRoomPortalNether` | **blocked**（跨维传送门另卡） |
| `DungeonChunkRoomPortalEnd` | **blocked** |
| `DungeonChunkRoomPortalTwilight` | **blocked** |
| `DungeonChunkRoomPortalAether` | **blocked** |
| `DungeonChunkRoomPortalMyst` | **blocked** |

基础设施（始终参与布局，不是 dump feature）：`DungeonChunkPillar` /
`RoomEmpty` / `DoorPiston` / `Corridor` / `Corridor3` / `Corridor4` /
`Entrance` / `Barracks`。

钥匙：`IL.KEYS` = Brass / Bronze / Copper / Gold / Iron / Lead / Plastic /
Platinum / Silver / Tin。CC 目前没有这些钥匙物品。钥匙格
`explicitly_blocked`，不得用原版钥匙或任意金属锭顶。有钥匙房间可以先生成
锁结构，不发假钥匙。

ZPM：配置默认开。CC 没有 ZPM 内容则 ZPM 战利品 `explicitly_blocked`，
不得用电池或电路板顶。

主石 / 次石来自 `BlocksGT.stones` 随机一对。当前 CC 载体只能抽取已注册的
`gt_stone/*` full-block identities（多数石种只有 `m8` / `m9`）；这能保证
运行时没有假方块，但不能冒充
GT6 的 brick / tile / smooth / chiseled / lamp / glass 形态。缺形态必须
继续记录为 blocked，不能把 m8/m9 的抽样结果写成源等价。

1.21 用独立结构件，不搬 1.7.10 十六元数据打包。

---

## 2. 明确不接管

- 树 9、行星岩 3、Center 5
- `other_features.json` 其余 172（`WorldgenStone`、流体泉、蜂巢、枯木等）
- Aether / Erebus / Alfheim / Twilight 维度与对应传送门
- Crops / Food / Bees；作物农场不得发明作物
- Sensors / Panels / Portals 专卡；本卡传送门房间只记 blocked
- 印刷机染料、LuV–PUV1 零件
- 把本结构折进已关闭的矿脉 / 流体矿 / 地表石子 catalog

---

## 3. 贴图

本轮没有为缺失 GT 地牢形态新增方块或物品；墙体只使用已注册的
`cruciblecraft:gt_stone/*` full-block 载体形态。GT brick / tile / smooth /
chiseled / lamp / glass、活塞门和 MTE 设施没有迁移假贴图，均记录在
`tools/waves/prep/gt-dungeon/blocked.json` 与 `art_manifest.json`，禁止用
`multiblock_casing` 或 vanilla 方块冒充。

---

## 4. 当前进度与验收

本卡暂不再宣称 `runtime_ready`。已保留可编译的结构载体，但下面的源对照
仍未完成：

- 构造实参、房间/死胡同表、原点避让、区块对齐、基岩接触点写进
  `tools/waves/prep/gt-dungeon/`
- 用 `worldgen/structure/gt_dungeon.json` + `structure_set/gt_dungeon.json`
  注册结构；用一个可序列化 `GtDungeonPiece` 跨 5×5–9×9 区块写入，
  不使用 scatter Feature 伪装多区块结构
- 结构墙暂时只从 `cruciblecraft:gt_stones` 取已注册 full-block 载体形态；
  这不是 GT6 房间装饰的完成证明
- 钥匙 / 跨模组图书室 / 传送门 / ZPM 记入 blocked 账

- 当前已修正 StructureSet 的候选频率，改为每区块交给 Java 闸判断
  `abs(chunk)%11 == 5`，并把布局持久化为 GT6 的 typed room/corridor codes
- 仍需补齐：精确 room dispatch、farm/entrance/barracks 的多区块几何、
  GT 石材装饰形态、活塞门/MTE 内容、bedrock ore、keys、ZPM 和 portal rooms
- GameTest 需覆盖 `(0,5)` / `(0,6)` 单轴格点和 `(0,25)` 原点避让案例
- 不得宣称 `player_complete`；不得宣称传送门已通

## 5. 重新关闭前清单

- [x] 1 个 feature、房间表、主世界闸和 blocked 账写进本计划
- [x] `capability.json` 登记结构 / piece / GameTest
- [x] StructureSet 不再以 `random_spread` 假装 `%11==5` 格点
- [x] 布局载体改为 typed room/corridor codes，并保留 NBT 序列化
- [ ] 补齐 GT6 room dispatch 与 Entrance/Barracks/Corridor3/4 几何
- [ ] 为真实 GT 石材形态补齐注册、来源贴图和 art manifest
- [ ] 完成 fresh GameTest / compile / capability-ledger 验收后再关闭
