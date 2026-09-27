# GT6 机器与多方块总计划

> 计划 slug：`portfolio/gt6-machine-multiblock`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 性质：排期总表。本文件自己不开工、不建 `capability.json`、不改 `unique_active_wave`。
> 下列落地卡各自占一次 unique-active。已有暂停卡的，不另起 slug。
>
> 分母：[gt6-full-coverage.md](../../../current/gt6-full-coverage.md) 第 4、5 节。
> 行为快照：[gt6-large-machines-porting.md](../../../current/gt6-large-machines-porting.md)（2026-09-22，排期以本文件为准）。
> 配方批量：[GT6 批量移植总计划](GT6批量移植总计划.md)。本文件不重开那些配方卡。
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = prep
capability_slug              = portfolio/gt6-machine-multiblock
unique_active_wave           = null
prep_owned_paths             = 本文件
landing_owned_paths          = 由各落地卡的详细计划分别声明
landing_depends_on           = 当前 unique-active `content/gt6-fluid-barrel` 关闭；卡与卡严格串行
```

---

## 0. 开场判断

覆盖表的「当前交付深度」把两件事叠在一列：主机在不在，以及配方源行证明了多少。
对照 `machine_delivery.json`、`ModBlocks`、`multiblock_structures/` 和
`tools/capabilities/**/capability.json` 之后，主机缺口比这列看起来小。

96 个机器 kind 里：

- 11 个 `full_replay`，31 个无配方图的转换/发电/传动 kind 已是 `runtime_accepted`。
- 40 个 `bounded_subset` 的单机主机已经在 `machine_delivery.json`。缺的是配方行，归批量移植总计划，不在这里再开机器卡。
- 8 个 `runtime_only`（熔炉、复制机、分子扫描、自动合成、装箱机、蜂类分析、植物分析、印刷机）主机也已注册。深度是 0 行已证明，不是没方块。
- 真正没有 CC 主机的 kind：扫描视觉（5 台，20281–20285）、拆箱机（5 台，20591–20595）。单方块发酵机 22003 在覆盖表第 4 节主机列为空，`machine_kinds.json` 里也没有。

30 个多方块控制器里，20 个覆盖表记 `runtime_accepted`。代码侧还要单独处理的是：

- 大型锅炉、3×3×3 储罐：结构与方块实体已在，没有任何 capability。
- 大型电解机：capability 已是 `runtime_ready` / `accepted`。`survival_access` 仍是 `partial`，那是获得格，不是主机暂停。
- 大型发酵器：结构、GameTest、capability 都在，`frozen` / `paused`。
- 5×5×5 金属储罐：只有 `mte_inplace_catalog.json` 身份，没有结构运行时。

物质制造机 17199 和旋转引擎已经挂在已关卡上，覆盖表没认出来。见第 2 节，不新开卡。

安全阀与批量总计划相同：一次一张 unique-active；缺零件保持 blocked；禁止 stand-in、禁止占位贴图、禁止用 ItemEntity 世界生成顶获得。`runtime_ready` 不推出 `survival_access`。

## 1. 已有主机，本计划不重开

### 1.1 多方块：`runtime_accepted`

这些控制器有 live 方块，并且已有 accepted capability。`survival_access` 仍是 `partial` 或 `blocked` 的，另开获得/配方卡，不把主机重做一遍。

| 控制器 | capability | 生存 | 代码 |
| --- | --- | --- | --- |
| 大型浸洗器 17104 | `machines/large-bathing-vat` | partial | `LargeBathBlock` + `large_bath.json` |
| 基岩钻 | `machines/bedrock-drill` | partial | `BedrockDrillBlock` |
| 大型离心 | `machines/large-centrifuge` | partial | 结构 JSON |
| 大型凝结器 | `machines/large-coagulator` | blocked | 结构 JSON。blocked 是乳胶/化学链，不是缺主机 |
| 焦炉 | `machines/coke-oven` | partial | `coke_oven.json` |
| 大型坩埚 | `machines/large-crucible` | partial | 结构 JSON |
| 大型破碎 | `machines/large-crusher` | partial | 结构 JSON |
| 蒸馏塔 / 低温蒸馏塔 | `machines/distillation-tower` | partial | 两台控制器，结构 id 共用 `distillation_tower` |
| 大型电解机 17103 | `machines/large-electrolyzer` | partial | 结构 JSON。主机已 accepted，partial 是获得格 |
| 大型高压釜 17112 | `machines/large-autoclave` | partial | 结构 JSON。主机已 accepted，partial 是获得格 |
| 聚爆压缩机 | `machines/implosion-compressor` | partial | 结构 JSON。配方图仍有 296 行可翻译未发 |
| 大型动力机 / 避雷针 / 范德格雷 | `machines/gt6-coil-hosts` | partial | MTE inplace |
| 大型燃气轮机 | `energy/large-gas-turbine` | partial | `LargeGasTurbineBlockEntity` |
| 物流核心 | `logistics/logistics-core` | unreviewed | 5×5×5 几何在 `LogisticsCoreStructure` |
| 大型搅拌机 | `machines/large-mixer` | partial | 结构 JSON |
| 大型电炉 | `machines/large-oven` | partial | 结构 JSON |
| 大型粉碎 | `machines/large-shredder` | partial | 结构 JSON。配方图仍缺 6061 行可翻译 |
| 大型洗矿机 17107 | `machines/large-sluice` | partial | 结构 JSON。源行就绪 100%，已证明 0，是配方 wave |
| 大型压榨 | `machines/large-squeezer` | partial | 结构 JSON。图进度 0.3%，主因是缺形态 |

`docs/history/card-plans/prep/大型洗矿机详细计划.md` 仍写着「等电解机让出 unique-active」。那份文件对的是 17104 大型浸洗器，主机已经由 `machines/large-bathing-vat` 验收。不要按那份 prep 再开一张。洗矿机是 17107 sluice，已由 `machines/large-sluice` 验收。

### 1.2 覆盖表落后于代码

| 覆盖表 | 代码 | 处理 |
| --- | --- | --- |
| `matter_fabricator` = `identity_only` | `MteInPlaceKind.MATTER_FABRICATOR`、`MatterFabricatorStructure`（5×5×5 致密铅 + 锇线圈）、`ModMultiblockControllers.LARGE_MATTER_FABRICATOR`。`machines/gt6-coil-hosts` 的 note 点名 massfab 17199，`CoilHostGameTests` 断言成形 | 不新开主机卡。对账扫描仍写 identity_only 时修扫描，不重做结构 |
| `MultiTileEntityEngineRotation` = `runtime_code_uncarded` | `RotationEngineCatalog`（9 台）和 `MteInPlaceBlockEntity.convertRotationEngine`。GameTest：`rotationEngineConvertsRuToKu`、`rotationEngineSoftHammerStopsInput`。身份在已关的 `content/gt6-mte-drive-runtime` | 不新开传动卡。该卡 `required_test_ids` 没列这两条，所以覆盖表没认 |
| `MultiTileEntityQuantumEnergizerLaser` = `identity_only` | `QuantumEnergizerCatalog` 已注册物品。行为挂在暂停的 `energy/fusion-quantum`（测试 id `quantumEnergizerConvertsLuToQu`） | 跟第 4 节 PUV 链，不单开 |

`gt.recipe.massfab` 仍是 0% 已证明（891 行缺 `neutralmatter`）。那是流体与 PUV 链的事，不是再做一台物质制造机。

### 1.3 单机主机已在、缺的是配方

`machine_kinds.json` / `machine_delivery.json` 已有这些 kind。覆盖表深度低，是因为源行没证明。继续走配方 wave 或保持批量总计划里的暂缓决定。

| 主机 | 覆盖深度 | 归谁 |
| --- | --- | --- |
| 装箱机 | `runtime_only`，11439 行可翻译未发 | 批量总计划 §1.3 暂缓。主机留着，不写 crate 配方 |
| 蜂类分析、植物分析、分子扫描、自动合成、复制机 | `runtime_only` 或空 dump | 主机已在。空 dump（`empty_source`）不等于没机器 |
| 印刷机 | `runtime_only`，prep 已写完可停手项 | 见第 4 节。五台获得格卡在传送带模块 |
| 洗矿机、粉碎、挤压、浸洗、熔融、注射、聚爆 | `bounded_subset`，可翻译行未发 | 单机与大型主机都已验收。开 dump wave，不开新主机 |
| 自动锤 15001–15004 | 不在第 4 节 kind 表 | `AutomaticHammerBlockEntity` 是活塞敲击，不跑 `gt.recipe.hammer`。锤图 309 行仍是工具/世界行为 |

## 2. 本计划要排的卡

现行 unique-active 是流体储罐。那张卡写明不做 3×3×3 多方块罐。下面从它关闭之后开始，严格串行。第 0 张先做，详细计划见 [GT6 机器账本检查详细计划](GT6机器账本检查详细计划.md)。

| 序 | 卡 | slug | 代码现状 | 这张卡关闭时 |
| --- | --- | --- | --- | --- |
| 0 | 机器账本检查 | `portfolio/gt6-machine-ledger-audit` | 已关文书和后来的行为脱节。例：`energy/converter-catalog` 仍写蒸汽机动力学未做、青铜固定 12 KU/t；代码已是 `SteamEngineKuCurve`（名义 12，行程 6–24）、正负行程、满汽停机排气。高压釜 / 发酵器的计划在 `closed/`，capability 仍是 `paused`。大型电解机检查后已是 `accepted` | 只改账本。能按现有合同关掉的暂停卡改成 accepted。文书落后于代码的，补 note、测试 id 和已关计划末尾的读法修订。对不上的留在下面的序号，本卡不改机器行为 |
| 1 | 大型锅炉 | `machines/large-boiler` | `LargeBoilerBlock` / `LargeBoilerBlockEntity`，五份结构 JSON（不锈钢、殷瓦、钛、钨钢、精金）。另有 `MteInPlaceKind.LARGE_BOILER`。无 capability。覆盖表 `runtime_code_uncarded` | 确认专用锅炉与 MTE inplace 是同一后端。按 GT6 逐档核对蒸汽输出、HU/水/蒸汽容量、进水出汽坐标、结垢、凿子清垢、过满与结构损坏。关 `runtime_ready` |
| 2 | 多方块储罐 | `machines/gt6-multiblock-tanks` | 已 `runtime_ready` / `accepted`。3×3×3 与 5×5×5 共用 `TankBlockEntity` | 不再占落地锁。`survival_access` 仍是 `partial`。不加别的边长 |
| 3 | 大型电解机 | `machines/large-electrolyzer` | 已 `runtime_ready` / `accepted`。`survival_access` 仍是 `partial`。结构 3×3×2，底进顶出，EU 512–4096，并行 16 | 不再占落地锁。`partial` 留给获得格或配方 wave，不重开主机。不导入缺形态行，不开形态 |
| 4 | 大型高压釜 | `machines/large-autoclave` | 已 `runtime_ready` / `accepted`。`survival_access` 仍是 `partial`。3×3×3 空心 18022，TU 1–16，并行 16。墙别名主机库存，主机任意面 IO，朝下自动输出 | 不再占落地锁。`partial` 留给获得格或配方 wave。不把单方块高压釜 22004 折进来 |
| 5 | 大型发酵器 | `machines/large-fermenter` | 已有。`frozen` / `paused`。5×5×3，HU，并行 256。`LargeFermenterAutoOutput` 已写背面偏移，固定坐标仍缺 GameTest | 补输出坐标/堵塞/重载测试后 accepted。不导入 6435 行 dump。缺流体留在流体卡 |
| 6 | 单方块发酵机 | `machines/fermenter` | 无主机。GT6 22003，不锈钢，HU 16–64，图 `RM.Fermenter`。网格 `wMh` / `PPP` / `BCB`（`Loader_MultiTileEntities.java` 1654） | 第 5 张关了再开，避免两张卡同时改发酵图。获得格逐格 `gt6_resolve`。缺的零件保持 blocked |
| 7 | 5×5×5 金属储罐 | 并入第 2 张 | GT6 没有边长滑杆，只有 3×3×3 和 5×5×5 两套空心方块 | 不再单独占锁。`machines/tank-5x5x5` 不建卡 |

第 0 张若已经把第 3、4 或 5 张收成 accepted，那一张不再单独占落地锁。检查卡写明「还缺测试」的，仍按原序号做。

2026-09-27 账本检查：第 3 张 `machines/large-electrolyzer` 已 `accepted`，不再占落地锁。第 4 张高压釜、第 5 张发酵器仍按原序号。第 1、2、6、7 张在占上 unique-active 时才写详细计划。

2026-09-27 大型高压釜：第 4 张 `machines/large-autoclave` 已 `accepted`，不再占落地锁。成形后的 18022 不再占独立 PortStore，主机任意面仍收物品、流体和 TU，自动输出在主机正下方。`survival_access` 仍是 `partial`。详细计划在 [大型高压釜详细计划](../closed/大型高压釜详细计划.md)。

2026-09-27 大型锅炉：第 1 张 `machines/large-boiler` 曾占 unique-active，现为 `runtime_ready` / `paused`。详细计划在 [大型锅炉详细计划](../closed/大型锅炉详细计划.md)。GameTest 还没当关闭门跑过，所以不是 accepted。

2026-09-27 多方块储罐：第 2 张 `machines/gt6-multiblock-tanks` 已 `accepted`，并收进第 7 张。GT6 只有 3×3×3 和 5×5×5 两套固定空心方块，没有连续边长。声明的 17 条 GameTest 已通过。详细计划在 [GT6 多方块储罐详细计划](../closed/GT6多方块储罐详细计划.md)。

大型锅炉的逐档缺口清单在大型机器快照 §5.8.1。那一节的旧容量数字（8,000 mB 水）是历史快照，以当前 `LargeBoilerTier` 为准，再和 GT6 逐项对。

## 3. 每张新卡的共同门

- 关闭目标是 `runtime_ready`。生存获得格分开写，缺一格就 blocked，不用别的零件顶。
- 结构卡要有：成形、未成形拒绝、端口格子与面、拆一块之后停机、重载后绑定还在。
- 能量种类保持 GT6 的 HU / EU / TU / RU / STEAM。不把蒸汽收成一个新的随手 `EnergyType` 来让锅炉变绿；锅炉继续走蒸汽流体和现有热量合同。
- 贴图从本地 `gregtech6_w` 拷，记 art manifest。
- 关卡后跑 `build_reconciliation.py --write`，让覆盖表的 `runtime_code_uncarded` / `runtime_paused` 改口。

## 4. 已有计划，不在这里重排

这些卡已经签发。本总计划只标明它们和上面 7 张的关系：上面 7 张不插进这条链，也不改它们的 slug。

PUV / OMEGA 链（[PUV2OMEGA科技线详细计划](PUV2OMEGA科技线详细计划.md)）是 GT6 注册内容，不是 CC 高压扩展。代码已在。大型热交换器和蒸汽涡轮已关卡。其余四张仍 `frozen` / `paused`，只因为还没按 unique-active 关卡：

1. `energy/large-heat-exchanger`（17197）已关 `runtime_ready` / `accepted`，`survival_access=unreviewed`
2. `energy/steam-turbine`（15 台单机 + 4 台大型 17211–17214）已关 `runtime_ready` / `accepted`，`survival_access=unreviewed`。依赖大型热交换器
3. `energy/fusion-quantum`（聚变 17198 + LU→QU 充能激光）。依赖蒸汽涡轮。改 `EnergyType` 的落地不走 prep 实施
4. `energy/quantum-massfab`（物质制造器 17199；中子素生存链才是 `CC_EXTENSION`，因为 `MT.Neutronium` 是 `unused` 桩）。依赖聚变
5. `content/puv-omega-parts`、`machines/puv-omega-matrix`（`IL` 的 PUV2–OMEGA；`VN[14]` 源名 XV）

另外三张 prep，获得或能量边界已经写明：

| 卡 | 状态 | 和本计划的边界 |
| --- | --- | --- |
| `machines/printer` | prep 可停手。主机已在 delivery | 五台获得格卡在 `IL.CONVEYERS`。盖板传送带不是这格 |
| `energy/cooler` | `runtime_ready` / `paused` | 电力冷却器 EU→CU+HU。通量冷却器走 FE，不发明 `EnergyType.RF` |
| `energy/flux-converters` | `runtime_ready` / `paused` | 同上，RF 数量译成 FE |
| （已撤）微型燃气涡轮 | 不移植 | kTFRUAddon 单方块。纳入会把许可拉成 AGPL，已从 CC 移除。GT6 燃气轮机仍是已验收的大型多方块 |

扫描视觉 20281–20285 的获得格同样是 `IL.CONVEYERS`（同类基础加工机卡已明确不注册这 5 台）。拆箱机 20591–20595 还要 `IL.PISTONS`。模块没有 live 身份之前，这两台和印刷机一样保持 blocked。批量总计划同时暂缓拆箱图、装箱图、`crate.*`、`bulletGt*`、`arrowGt*`。要推翻这个决定，先改那份总计划，再开卡。

## 5. 明确不接管

- 第 1.1 节已 accepted 的多方块主机。`survival_access=partial` 用获得格或配方 wave 收，不重写结构。
- 批量总计划已经暂缓的装箱/拆箱、箱、子弹、箭。
- `gt.recipe.anvil`、`gt.recipe.cruciblealloying` 的旧排除决策。
- toolhead、bumblelyzer、byproduct、trees 等整图 `display_only`。
- 在机器卡上顺手开材料形态，或用 stand-in 把 `player_complete` 涂绿。
- 把单方块发酵机、扫描视觉塞进锅炉或储罐卡。储罐只做 GT6 的两套固定空心方块，不加别的边长。
- 管/缆切片 C，以及流体储罐卡已经声明不做的杯、壶、电池、保温瓶。
