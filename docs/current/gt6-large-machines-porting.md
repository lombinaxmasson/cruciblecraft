# GT6 大型机器与移植缺口说明

> 快照日期：2026-09-22  
> 范围：GT6 `Multiblock Machines` 机器、主机身份、结构部件与移植缺口。  
> 本文只使用本地 GT6 源码、当前 `capability.json`、当前工作树源码/资源/测试作为证据。

## 1. 这份文档解决什么问题

GT6 的“大型机器”不是一个简单的 Block 清单。一个 GT6 大型机器通常同时包含：

- 一个具体的 `MultiTileEntity...` 控制器类；
- 一个 GT6 注册身份与 meta；
- 结构尺寸、方向、形成和拆除规则；
- 墙、致密墙、线圈、专用部件、输入/输出端口；
- 能源类型、能量输入输出、流体槽、物品槽和自动输出；
- 对应的 RecipeMap、并行、超频、输入数量和输出语义；
- 存档、重载、破坏、输出堵塞和断电行为；
- 物品身份、配方、贴图、模型、EMI 和 GameTest。

因此，下面这些状态必须分开：

1. **运行时已接受**：当前能力卡为 `maturity=runtime_ready`、`workflow=accepted`。
2. **暂停代码**：有源码、资源或测试，但能力卡是 `frozen/paused`。
3. **当前工作树 WIP**：主机代码、结构或测试已经出现，但还没有对应的 accepted 能力卡。
4. **身份/部件已落地**：MTE BlockItem 或专用部件存在，但不能据此声称大型主机已完成。
5. **缺主机**：只有配方图、部件或源类记录，没有可形成的主机。

`runtime_ready` 只表示机制可以运行；`survival_access` 是独立维度。创造栏、MTE catalog 记录、配方地图或一个 Java 类本身，都不能单独证明完整移植。

## 2. GT6 的源头应该怎么看

### 2.1 控制器类

Java/tick 的本地源头是：

```text
gt6_code/gregtech6/src/main/java/gregtech/tileentity/multiblocks/
```

其中的具体类包括：

- `MultiTileEntityCokeOven`
- `MultiTileEntityCentrifuge`
- `MultiTileEntityElectrolyzer`
- `MultiTileEntityCoagulator`
- `MultiTileEntityAutoclave`
- `MultiTileEntityBath`
- `MultiTileEntityMixer`
- `MultiTileEntityFermenter`
- `MultiTileEntityOven`
- `MultiTileEntitySluice`
- `MultiTileEntityCrusher`
- `MultiTileEntityShredder`
- `MultiTileEntitySqueezer`
- `MultiTileEntityDistillationTower`
- `MultiTileEntityCryoDistillationTower`
- `MultiTileEntityImplosionCompressor`
- `MultiTileEntityLargeBoiler`
- `MultiTileEntityLargeTurbineSteam`
- `MultiTileEntityLargeTurbineGas`
- `MultiTileEntityLargeDynamo`
- `MultiTileEntityLargeHeatExchanger`
- `MultiTileEntityFusionReactor`
- `MultiTileEntityMatterFabricator`
- `MultiTileEntityLogisticsCore`
- `MultiTileEntityTank3x3x3Metal`
- `MultiTileEntityTank3x3x3Wood`
- `MultiTileEntityTank5x5x5Metal`
- `MultiTileEntityVonDaGraagg`
- `MultiTileEntityLightningRod`
- `MultiTileEntityBedrockDrill`
- `MultiTileEntityCrucible`

注册名、meta、RecipeMap、能源类型和主机配方集中在：

```text
gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java
```

这份 Loader 是确认“GT6 这个名字到底是控制器还是部件”的首要依据。比如：

- `Small Tank Main Valve` 和 `Large Tank Main Valve` 在 GT6 中是储罐控制器身份，不是普通装饰方块；
- `Boiler Main Barometer` 是 `MultiTileEntityLargeBoiler` 的控制器身份；
- `Steam/Dynamo/Gas Turbine Main Housing` 是相应大型能源转换器的控制器身份；
- `Centrifuge Part`、`Electrolyzer Part`、`Crusher Wheels`、`Shredder Blades` 是部件，不是主机；
- `Lightning Rod Electric Output` 是避雷针多方块的输出控制器身份，`Lightning Rod` 是结构部件。

### 2.2 部件类

GT6 的多方块通用部件主要由：

```text
gt6_code/gregtech6/src/main/java/gregapi/tileentity/multiblocks/MultiTileEntityMultiBlockPart.java
```

承载，具体材料和 meta 在 Loader 中注册。墙、致密墙、线圈、专用部件和端口都必须结合它们在目标控制器中的角色判断，不能只按名称计数。

## 3. 当前 CrucibleCraft 的证据层

当前能力状态的权威入口是：

```text
tools/capabilities/<slug>/capability.json
```

字段含义和验收边界见：

```text
docs/current/capability-delivery-workflow.md
```

当前源码侧要同时检查：

```text
src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java
src/main/java/com/masson/cruciblecraft/registry/ModBlockEntities.java
src/main/resources/data/cruciblecraft/multiblock_structures/
src/main/resources/data/cruciblecraft/mte_inplace_catalog.json
src/test/java/com/masson/cruciblecraft/gametest/
```

其中：

- `ModBlocks` 证明 Block 身份是否注册；
- `ModBlockEntities` 证明是否有对应 BlockEntity；
- `multiblock_structures` 证明是否有声明式结构；
- `mte_inplace_catalog` 证明 GT6 MTE 身份是否有 live BlockItem；
- GameTest 证明形成、tick、输入输出、重载或拆除等行为是否被覆盖；
- `capability.json` 决定当前能否称为 accepted runtime。

## 4. 当前这批 50 个条目的快照

这次用户清单共有 50 个名称。其中：

- 27 个是主机或主机身份；
- 23 个是部件、端口或 MTE 身份；
- 20 个条目属于当前 accepted runtime；
- 6 个有明确的 frozen/paused 能力卡；
- 10 个在当前工作树中已有主机/结构代码，但尚未进入 accepted 能力账本；
- 10 个只有身份或部件；
- 3 个目前没有大型主机。

### 4.1 已接受运行时：20 个条目

主机或主机身份：

- 范德格雷起电机；
- 物流核心；
- 蒸馏塔；
- 低温蒸馏塔；
- 大型物质制造器；
- Large Shredder；
- Dynamo Main Housing；
- Gas Turbine Main Housing；
- Lightning Rod Electric Output。

部件或端口：

- Large Copper Coil；
- Large Niobium-Titanium Coil；
- Large Nichrome Coil；
- Large Carborundum Coil；
- Large Osmium Coil；
- Large Iridium Coil；
- Heat Transmitter；
- Ventilation Unit；
- Quadcore Processor Unit；
- Distillation Tower Part；
- Lightning Rod。

主要能力证据：

- `tools/capabilities/machines/gt6-coil-hosts/capability.json`
- `tools/capabilities/machines/large-shredder/capability.json`
- `tools/capabilities/machines/distillation-tower/capability.json`
- `tools/capabilities/logistics/logistics-core/capability.json`
- `tools/capabilities/energy/large-gas-turbine/capability.json`
- `tools/capabilities/content/gt6-mte-multiblock-runtime/capability.json`

### 4.2 有代码但暂停：6 个条目

- Large Electrolyzer；
- Large Autoclave；
- Large Fermenter；
- Large Heat Exchanger；
- Fusion Reactor；
- Steam Turbine Main Housing。

对应能力卡分别位于：

```text
tools/capabilities/machines/large-electrolyzer/
tools/capabilities/machines/large-autoclave/
tools/capabilities/machines/large-fermenter/
tools/capabilities/energy/large-heat-exchanger/
tools/capabilities/energy/fusion-quantum/
tools/capabilities/energy/steam-turbine/
```

这些项目不能标成“缺失”，因为代码、结构、资源或测试已经存在；也不能标成“已完成”，因为当前声明仍是 `frozen/paused`。

### 4.3 当前工作树已有主机代码，但尚未 accepted：11 个条目

- Coke Oven；
- Fire Bricks；
- Large Centrifuge；
- Large Coagulator Array；
- Large Bathing Vat；
- Large Batch Mixer；
- Large Electric Oven；
- Large Crusher；
- Large Crucible；
- Bedrock Mining Drill Controller；
- Bedrock Mining Drill Head。

这类条目可以在当前源码中找到 Block、BlockEntity、结构 JSON 或 GameTest，但当前没有对应的 accepted 能力卡。后续要关闭它们，至少需要补齐：

- 独立 capability slug；
- owned paths；
- 结构、形成、拆除和重载证据；
- 真实能源/物品/流体行为；
- 配方和获得性边界；
- 受影响能力的验证记录。

特别注意：

- `content/gt6-mte-multiblock-runtime` 接受的是 MTE 原地身份运行时，不等于专用 `coke_oven` 主机已经完成；
- `machines/large-processing-parts` 接受的是电解器部件、破碎机辊轮、研磨机刀片和相关配方图，不等于三个大型主机都已形成；
- `machines/bath` 接受的是单方块 Bath 和 Bathing Pot，不等于 Large Bathing Vat；
- 单方块 Oven/Squeezer 的能力不能替代 Large Electric Oven/Large Squeezer。

### 4.4 只有部件或身份：10 个条目

- Wall；
- Dense Wall；
- Small Tank Main Valve；
- Large Tank Main Valve；
- Centrifuge Part；
- Electrolyzer Part；
- Sluice Part；
- Crusher Wheels；
- Shredder Blades；
- Boiler Main Barometer。

这些身份可以在：

```text
src/main/resources/data/cruciblecraft/mte_inplace_catalog.json
```

中存在，也可能已经有配方、模型和材质，但必须另外确认对应主机是否存在。

### 4.5 Large Sluice 17107 已转为 accepted runtime

Large Sluice 已进入独立 `machines/large-sluice` delivery lane，不能与
Large Bathing Vat（17104）或单方块 `cruciblecraft:sluice` 合并统计。当前实现
严格对照本地 GT6 `MultiTileEntitySluice.checkStructure2`：

- `3×7×3`，两层 18006 Titanium Wall，顶层 18106 Sluice Part；
- 近侧底层中心为 `cruciblecraft:large_sluice`，远侧顶层 3 个
  `ONLY_ITEM_FLUID_IN`，近侧底层两侧为 `ONLY_ITEM_FLUID_OUT`；
- 中层远侧两侧为 `ONLY_ENERGY_IN`，并在结构外相邻位置切换两个 RU 能量源；
- 运行态按 GT6 `tD` 映射到 Sluice Part 0–7 设计；
- `RM.Sluice` 使用 1 物品输入、9 物品输出、1 水输入、1 Sluice Juice
  输出，RU 512–4096、efficiency 5000、parallel 64、parallel duration、
  cheap overclock。

对应能力卡为：

```text
tools/capabilities/machines/large-sluice/capability.json
```

主机、结构、端口绑定、active-state 设计切换、GT6 艺术清单、主机生存配方、
水洗汁流体和测试证据均由该卡拥有。当前 `survival_access=partial` 的边界仍
独立于 `runtime_ready`；缺少真实材料形态的额外 GT6 行继续留在 blocked
overflow，不使用 stand-in。

目前仍没有大型主机的条目只剩：

- Large Squeezer。

### 4.5.1 Large Shredder 17109 已转为 accepted runtime

`cruciblecraft:large_shredder` 现在有独立主机、BlockEntity、结构 JSON、
18108 Shredder Blades 适配器、RU 相邻能源联动和 5 点运行伤害。结构严格
对应 GT6 `MultiTileEntityShredder.checkStructure2`：5×5×3、56 个
18003 钨钢墙、9 个中层结构刀片、9 个顶层输入刀片、2 个能量孔，底层
24 个输出墙端口。

能力边界仍然分开记录：

- `maturity=runtime_ready`、`workflow=accepted`；
- `survival_access=partial`：主机和 18108 部件有真实合成获得；
- `RM.Shredder` wave 只发布当前真实 operand 可解析的源行；
- 缺少 GT6 形态的行保留在 `overflow/blocked`，不使用 stand-in，也不
  宣称这些行 `player_complete`。

### 4.6 清单之外的两个容易混淆项

用户清单写的是部件/控制器身份，但当前工作树还出现了：

- `Large Boiler` 主机及多个材料结构；
- `tank_3x3x3` 主机。

因此，`Boiler Main Barometer`、`Small Tank Main Valve`、`Large Tank Main Valve` 的状态不能直接等同于锅炉或储罐主机的状态。统计时必须分别记录“身份”和“主机”。

## 5. 移植时最容易遗漏的内容

### 5.1 身份缺漏

- GT6 meta 没有映射到唯一 CC runtime id；
- MTE dummy id 与专用 CC controller 重复；
- 同一个 `(机器, 材料/等级)` 被注册成两个不同后端；
- 只注册了主机，没有注册控制器实际需要的部件；
- 把部件错误地注册成可独立运行的机器。

### 5.2 结构缺漏

- 尺寸或方向错误；
- 墙与致密墙角色混用；
- 线圈顺序、材料一致性或整圈要求缺失；
- 专用部件只作为方块存在，却没有绑定到主机；
- 形成后破坏部件、重载世界或区块卸载时状态错误；
- 空主机可以运行，或者未形成结构也能接受输入。

### 5.3 能源和流体缺漏

- GT6 的 TU/RU/EU/HU/CU/QU/LU/STEAM 被错误折叠；
- 输入、输出、排废和自动输出方向不一致；
- 缓冲容量、消耗速率、过量流体和堵塞行为缺失；
- 断电时仍继续运行；
- 用一个通用能源接口替代了 GT6 的专用能源语义。

### 5.4 共享端口不是 GT6 的逐格端口

GT6 的结构方块按具体格子承担模式。一个多方块可以同时存在：

- 物品输入；
- 流体输入；
- 能量输入；
- 物品输出；
- 流体输出；
- 只允许输入或只允许输出的方向限制。

当前 CC 的 `shared_port_supply` 采用“每个主机一份物品供应、一份流体供应”的聚合模型，而不是按每个物理端口格子分别提供 supply。它解决的是共享吞吐和库存绑定，不等于复现 GT6 的端口交互语义。

本轮已保留该单 host 模型，并由 `PortCapabilityGate` 按绑定的
`PortType` 逐格门控 item insert/extract、fluid fill/drain 和 electric
input；聚合库存不再扩大物理端口的权限。

因此，以下机器必须额外检查端口格子和方向，不能只检查总吞吐量：

- Large Boiler；
- Tank 3×3×3；
- Large Fermenter；
- Large Centrifuge；
- 以及所有同时拥有 item/fluid/energy 多种端口的主机。

验证重点是“从哪一个格子、哪一个面、以哪一种模式接受或输出”，而不是“总共能传多少”。

### 5.5 配方缺漏

- RecipeMap 名称相同但槽位布局不同；
- GT6 的最小/最大输入量、并行数或超频规则丢失；
- 输出副产物没有保留；
- 缺失材料形态时用别的材料、原版物品或电路顶替；
- 只有 RecipeMap，没有真正的主机输入输出实现。

缺形态时应保持 blocked，并进入材料形态需求普查，不应为了让配方显示可用而制造 stand-in。

### 5.6 部件与端口缺漏

- Heat Transmitter 被当成普通能源端口；
- Distillation Tower Part 的不同高度/绑定模式没有保留；
- Crusher Wheels、Shredder Blades 等部件没有参与形成条件；
- Boiler Main Barometer、Turbine Main Housing 等控制器身份被当作普通装饰方块；
- Lightning Rod 与 Lightning Rod Electric Output 的输入/输出方向混淆。

### 5.7 获得、渲染和测试缺漏

- 有 BlockItem 但没有真实生存配方；
- 把创造栏或 GameTest 注入误当成 survival access；
- 贴图使用 CC 其他机器作为替代；
- 没有 GameTest 覆盖形成、tick、输出堵塞、重载、拆除和存档；
- 只测试“能放置”，没有测试“主机真的按照 GT6 语义运行”。

### 5.8 本轮对照补充的重点缺口

下面记录的是上一轮发现的具体行为差距。状态以当前工作树为准：有些项目已经在 WIP 代码中补上了本地实现，但在 GT6 对照、方向性测试和能力验收完成前，仍应保留为移植风险。

#### 5.8.1 Large Boiler：不能只记录转换公式

GT6 的基础转换是：

```text
80 HU + 1 mB water -> 160 mB steam
```

GT6 `MultiTileEntityLargeBoiler` 还同时实现：

- 材料档位决定的蒸汽输出、HU 容量和蒸汽容量；
- 水箱和蒸汽箱的长容量；
- 非纯水造成的结垢与效率下降；
- 凿子清垢；
- 气压计；
- 结构损坏、HU 过量或蒸汽过满时爆炸；
- 过热/过满时的安全处理；
- 停机后的冷却和排汽；
- Heat Transmitter 的专用进热方向；
- 固定的进水格、出汽格和 pipe-hole 输出位置。

当前 CC 的 `LargeBoilerTier` 和 `LargeBoilerBlockEntity` 已经出现了部分对应实现：

- 五个材料档位；
- 128,000 mB 水容量；
- 按档位计算的蒸汽容量；
- 80/160 转换；
- efficiency、cooldown、barometer；
- decalcify、overfill、结构损坏和爆炸路径；
- 独立的进水/出汽端口绑定。

因此，上一轮记录的“8,000 mB 水、16,000 mB 蒸汽、4,096 HU 固定容量”应标为**历史快照**，不能继续当作当前工作树的数值。当前仍需补的不是简单容量字段，而是逐档与 GT6 的验收证据：

- 每个材料档位的 `mOutput`、HU capacity、水 capacity、蒸汽 capacity 是否逐项相同；
- Heat Transmitter、进水和出汽的结构坐标与面方向是否逐项相同；
- 结垢概率、效率下限、冷却损失和爆炸阈值是否相同；
- 破坏、凿子、重载、输出堵塞和过满时是否与 GT6 同步；
- 气压计显示与实际蒸汽量/结构风险是否一致。

证据入口：

```text
gt6_code/gregtech6/src/main/java/gregtech/tileentity/multiblocks/MultiTileEntityLargeBoiler.java
gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java
src/main/java/com/masson/cruciblecraft/content/blockentity/LargeBoilerTier.java
src/main/java/com/masson/cruciblecraft/content/blockentity/LargeBoilerBlockEntity.java
src/main/resources/data/cruciblecraft/multiblock_structures/large_boiler_*.json
```

#### 5.8.2 Large Electrolyzer：底面 GT6 能量端口已对齐

GT6 大型电解机底面包含 `ONLY_ITEM_FLUID_ENERGY_IN`。CC 现在用独立的
`item_fluid_energy_in` 端口类型声明底层 8 个实际 18105 部件，顶层仍为
`item_fluid_out`：

```text
src/main/resources/data/cruciblecraft/multiblock_structures/large_electrolyzer.json
```

底层的第 9 个逻辑目标是控制器自身；GT6 的
`checkAndSetTarget` 对该目标直接视为成功，因此 CC 保留控制器在底层
side-bottom 中心的位置，不额外放置重复部件。`item_fluid_energy_in`
只提供物品/流体输入和 Electric 能量输入；顶部部件只提供物品/流体输出。
未成形或未绑定时，部件不提供这些能力。

对应的结构、能力方向、未成形拒绝和端口 landing check 已验证。剩余的
shared-port 聚合行为属于 §5.8.4 的独立端口语义问题，不在本项关闭范围内。

#### 5.8.3 3×3×3 Tank：GT6 阀门控制器与完整流体合同已对齐

CC 现在使用 GT6 Small Tank Main Valve MTE（17001–17007、17022–17027）
作为 live controller。控制器位于外侧面中心，空心位于其后方中心；
其余 25 个格子必须是同材质墙，并通过 `FLUID` tag-backed port 绑定到
一个 shared host tank。旧 `cruciblecraft:tank_3x3x3` 仍保留注册壳以读取
旧存档，但不再形成结构、进入创造页或拥有新配方。

当前实现的直接证据是：

```text
src/main/resources/data/cruciblecraft/multiblock_structures/tank_3x3x3.json
src/main/java/com/masson/cruciblecraft/content/blockentity/TankBlockEntity.java
src/main/java/com/masson/cruciblecraft/content/multiblock/TankControllerProfiles.java
```

`TankControllerProfiles` 固化 GT6 Loader 的材质容量、wall meta 和
gas/acid/plasma/magic proof；`TankBlockEntity` 仍是单 host/单 tank，
但容量由阀门身份决定：

- 直接 controller fill/drain 与墙面 `FLUID` 双向视图；
- 无 proof 的气体、酸、等离子、魔法流体的 server-side hazard；
- 导电流体、超材料熔点流体和木罐 non-simple 流体拒绝；
- 超温熔毁、结构破坏/危险流体清空；
- 按水平面、气体和重力方向执行自动输出，堵塞时不丢液；
- 5×5×5 Tank 仍是 post-1_0 独立项。

GT6 profile 的 source revision 与容量证据保存在
`TankControllerProfiles` 和结构 JSON 的 `source` 字段中；不得再引入
固定 `256,000 mB` 作为储罐容量。

#### 5.8.4 共享端口聚合与逐格能力门控

`shared_port_supply` 仍把多个物理端口汇聚为一份主机 item/fluid
supply；这是吞吐/库存主机语义，不再承担方向授权。`PortCapabilityGate`
按每个结构格绑定的 `PortType` 暴露 item insert/extract、fluid fill/drain
和 electric input：

- `ITEM_FLUID_ENERGY_IN` 只能 item/fluid 输入和 electric 输入；
- `ITEM_FLUID_OUT` 只能 item/fluid 输出；
- `FLUID` 只提供双向 fluid；
- boiler、mixer、electrolyzer 和 tank wall 的特例只在 host adapter
  中保留容量/视图路由。

回归测试必须按“端口坐标 + 端口模式 + 端口面”断言，不能只断言库存
最终数量。

相关实现：

```text
src/main/java/com/masson/cruciblecraft/content/multiblock/MultiblockPortAggregator.java
src/main/java/com/masson/cruciblecraft/content/multiblock/PortCapabilityGate.java
src/main/java/com/masson/cruciblecraft/registry/ModMultiblockPlugins.java
src/main/resources/data/cruciblecraft/multiblock_plugins.json
```

#### 5.8.5 Large Fermenter：固定背面偏移输出仍需实证

GT6 发酵机使用固定的背面偏移输出位置和方向，不是“把全部输出放入共享库存”。

当前 CC 已有专门的：

```text
src/main/java/com/masson/cruciblecraft/content/blockentity/LargeFermenterAutoOutput.java
```

该类尝试使用 GT6 风格的远端偏移：流体在控制器高度，物品在上方一格，并在 5×5 结构背面外侧寻找目标。但当前文档不能只因为代码存在就宣称完全对齐，仍需 GameTest 明确验证：

- 目标位置是否是 GT6 的固定背面偏移；
- item 与 fluid 是否使用不同的高度；
- 目标方块的接收面是否正确；
- 前方、侧面或其他共享端口不能抢走输出；
- 输出堵塞时是否保留物品/流体，而不是丢失或转入聚合库存；
- 重载、区块未加载和自动输出脉冲是否保持 GT6 时序。

因此 Large Fermenter 的输出状态应记录为“已有专用输出实现，固定坐标/方向仍是验证缺口”。

## 6. 推荐的重复审计顺序

以后统计任何一批 GT6 大型机器时，按以下顺序执行：

1. 在本地 `Loader_MultiTileEntities.java` 找到名称、meta、控制器类、RecipeMap 和能源类型。
2. 在 `gregtech/tileentity/multiblocks` 确认它是具体控制器，不是抽象基类或通用部件。
3. 在 `mte_inplace_catalog.json` 中确认 GT6 身份是否只是 live MTE BlockItem。
4. 检查 CC 是否有：
   - `ModBlocks` 注册；
   - `ModBlockEntities` 注册；
   - 结构 JSON；
   - tick/IO/配方实现；
   - GameTest。
5. 检查对应 `capability.json`：
   - `runtime_ready + accepted`：已接受运行；
   - `frozen + paused`：代码存在但暂停；
   - 没有能力卡：只能写 WIP，不能写已完成。
6. 单独记录部件和主机，不能把两者相加当作机器数量。
7. 最后单独检查 `survival_access`，不要把它与 runtime 状态混为一谈。

常用检查命令：

```powershell
python tools/build_capability_ledger.py --check
python tools/build_project_status.py --check
python tools/verify.py integration --profile capability-runtime
rg -n "MultiTileEntity|Multiblock Machines" gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java
rg -n "large_|multiblock|coke_oven|tank_3x3x3" src/main/java src/main/resources src/test/java
```

## 7. 本文的维护规则

- 新增大型主机时，同时补充 GT6 meta、CC runtime id、控制器类、结构、部件、RecipeMap、能力卡和测试证据。
- 只新增 MTE 身份时，明确写成“identity/part only”，不要增加主机完成数。
- 专用大型主机与同名单方块机器必须使用不同条目。
- `capability.json` 是当前能力状态入口；历史计划只作为实现背景，不能替代当前声明。
- 状态统计应同时输出“条目数”和“主机数”。
- 任何缺项都应写出具体缺口类型：identity、structure、controller、IO、recipe、art、acquisition、test 或 verification。

