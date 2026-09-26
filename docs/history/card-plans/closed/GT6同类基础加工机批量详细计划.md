# GT6 同类基础加工机批量详细计划

> 计划 slug：`machines/gt6-basic-machine-batch`
> 状态：已关（`runtime_ready`，`workflow=accepted`）。本文件位于 `card-plans/closed/`。试玩未签。
> 正式名称：GT6 同类基础加工机批量
> 性质：落地四个 GT6 `MultiTileEntityBasicMachine` kind 及其三张全就绪小图。ScannerVisuals 已拆出。
> 总计划第 8 张落地卡，见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = machines/gt6-basic-machine-batch
unique_active_wave           = null
dump_maps                    = gt.recipe.burnmixer 29；gt.recipe.catalyticcracking 3；
                               gt.recipe.crystallisationcrucible 132；
                               gt.recipe.scannervisuals 50（display_only，本卡不导入）
hosts                        = 16（BurnMixer 4、CatalyticCracker 4、CrystallisationCrucible 4、
                               SteamCracker 4）。ScannerVisuals 5 台拆出。
prep_owned_paths             = machine/processing/prep/*PrepSpec.java（未注册）；
                               textures/block/machine/{burnmixer,catalyticcracker,crystallisationcrucible,
                               steamcracker,scannervisuals}/**；tools/waves/prep/gt6-basic-machine-batch/**
landing_owned_paths          = ModBlocks / ModItems / ModBlockEntities / ModMenus / ModRecipeMaps /
                               ModProcessingMachines / Gt6SidedIo / machine_kinds.json /
                               machine_tiers.json / machine_acquisition.json / machine_delivery.json /
                               src/recipe_generated/**（三张小图）/ src/test/.../gametest/
landing_depends_on           = 当前 unique-active 空窗；不依赖身份卡与配方卡，可提前排
partial_close_allowed        = false
```

---

## 0. 开场判断

五个 kind 在 GT6 都是同一个 `MultiTileEntityBasicMachine`（ScannerVisuals 是 `...Electric`），
只差 NBT：材质列、能量、NBT_INPUT、贴图键、侧面 IO。CC 有共用的
`ProcessingMachineBlock` / `ProcessingMachineBlockEntity` / Menu / Screen，每个 kind 只多一个
`ProcessingMachineSpec`、一组 sidecar 行和一张图。五个一起落，比逐台开卡少四轮共享文件冲突。

三张小图的 GT6 行在覆盖页上身份就绪 100%，可以随机器一起 `full_replay`，同时满足
“加工机还要第一张 live 小图已证明加入流程”。steamcracking 7,746 行不在本卡，归化学杂项配方卡。

## 1. 分母

| kind | GT6 类 | sourceId | 材质列 | 能量 | NBT_INPUT | 贴图键 | 面板（RM.java） |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BurnMixer | BasicMachine | 20521–20524 | `Kinetic_T[1–4]` | RU | 32/128/512/2048 | `burnmixer` | 物品 6/1，流体 6/2；`NEEDS_IGNITION`，`PARALLEL` 4/8/16/32 |
| CatalyticCracker | BasicMachine | 20481–20484 | `Heat_T[1–4]` | HU | 32–2048 | `catalyticcracker` | 物品 1/3，流体 2/9 |
| CrystallisationCrucible | BasicMachine | 20251–20254 | `Heat_T[1–4]` | HU | 32–2048 | `crystallisationcrucible` | 物品 1/1，流体 3/0；只右出 |
| SteamCracker | BasicMachine | 20491–20494 | `Heat_T[1–4]` | HU | 32–2048 | `steamcracker` | 物品 1/3，流体 2/9 |
| ScannerVisuals | BasicMachineElectric | 20281–20285 | `Electric_T[1–5]` | EU | 32–8192 | `scannervisuals` | 物品 2/2，无罐；`RecipeMapScannerVisuals` |

注册行：本地 `Loader_MultiTileEntities.java` 1437–1440、1457–1461、1570–1579、1595–1598。
RecipeMap：`gregapi/data/RM.java` 67、68、73、76、142。

## 1.1 获得格（D0）

GT6 网格（一档；高档板厚递增，按注册行逐档核）：

| 机器 | 网格 | 键 |
| --- | --- | --- |
| BurnMixer | `PMP` / `PRP` / `hSw` | M 机器外壳，S 杆，R Invar 转子，P Invar 板 |
| CatalyticCracker | `IPI` / `ZMZ` / `ICI` | M 双层外壳，C/I 铜 / Invar 双层板，P 四联管，Z 沸石粉 |
| CrystallisationCrucible | `wUh` / `PMP` / `BCB` | M 双层外壳，一档 U 石英熔炼坩埚 1018，二至四档 U 铱熔炼坩埚 1039，C 铜双层板，B 砖，P 中管 |
| SteamCracker | `IwI` / `PMP` / `ICI` | 同催化裂化，P 中管 |
| ScannerVisuals | `CPC` / `wXh` / `WMW` | M 外壳，X 传送带盖板[档]，C 电路[档]，W 电缆[档]，P Lumium 板 |

获得格已逐格 `gt6_resolve`。沸石粉、殷瓦板、铜板、管、外壳、转子、杆都已注册。
结晶坩埚一档的 U 是 GT6 registry item 1018 石英熔炼坩埚，已注册为
`foundry/smelting_crucible_nether_quartz`（下界石英宝石 ×7，锤子、凿子不消耗）。
二至四档仍用铱熔炼坩埚 1039。不用别的物品顶 1018。
ScannerVisuals 的 `RecipeMapScannerVisuals` 会把 NBT 抄到 USB 并把物品映射成方块外观，
不是普通配方，本卡不注册这 5 台。

## 2. 实施

1. **spec 数据化**（负责人 2026-09-24 批准）：先落 `machine_delivery.json` → `ProcessingMachineSpec`
   生成器，覆盖槽 / 罐 / 能量 / 侧面 IO / GUI 布局。生成器先对已 live 的 slicer、rollformer 等
   重新生成，与现有手写 spec 逐字段一致后，再用它产出这五个。点火、并行、扫描这类行为钩子仍是 Java，
   按 kind 挂在生成的 spec 上。落地时同步改 [recipe-wave-workflow.md](../../../current/recipe-wave-workflow.md) §1
   “新 kind 仍要一次人工 Java spec” 一句。
2. `ModRecipeMaps` 新增 5 张图；sidecar 四个 JSON 各加行；datagen 出方块、模型、语言、掉落、EMI。
3. BurnMixer 的点火与并行、ScannerVisuals 的方块↔物品扫描是非普通配方行为：
   前者进 validator；后者如果不能用普通配方表达，ScannerVisuals 从本卡拆出，其余四个照常关。
4. 三张小图走 `recipe_bulk`：Source Pack + `recipe_import.json` + 人工 `production_lock` + compile。
   每行带 `evidence_hashes` / `selected_source_recipe`。
5. 贴图从 `textures/blocks/machines/basicmachines/<键>/` 迁，写美术清单。

## 3. 验证与试玩

```powershell
python tools/build_recipe_bulk.py import-source --spec <每张图的 recipe_import.json> --check
python tools/build_recipe_bulk.py compile --wave all --check
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile capability-runtime
.\gradlew.bat runGameTestServer -PwaveRecipes=machines/gt6-basic-machine-batch
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

GameTest：每个 kind 至少一档放置、供能、跑一条抽样配方、产物落槽。
人工 `runClient`：生存合成一台一档 BurnMixer，以及一档 CrystallisationCrucible（石英熔炼坩埚）和二档及以上（铱熔炼坩埚）。一档结晶坩埚能量上限 64，最短配方在二档才能跑。接能量跑通，EMI 可见三张图。蒸汽裂化机只放置供能，steamcracking 配方不在本卡。

## 4. 明确不接管

- steamcracking 7,746 行（化学杂项配方卡的后续批次）
- Unboxinator（决策暂缓）
- 电驱同图机以外的任何新能量类型
- 缺格用替代材料凑获得格

## 5. 关闭清单

- [x] spec 生成器对已 live kind 逐字段一致；工作流文档 §1 已改
- [x] 16 台主机注册、GUI 与侧面 IO 对 GT6；ScannerVisuals 不在本卡
- [x] burnmixer / catalyticcracking / crystallisationcrucible `full_replay`，overflow 0
- [x] 获得格 source-exact。石英熔炼坩埚 1018 已注册，一档结晶坩埚用它；二至四档用铱坩埚 1039
- [x] 贴图来自 GT6，清单齐
- [x] GameTest `cruciblecraft_wave_machines_gt6_basic_machine_batch` 4 项通过（2026-09-26）
- [ ] 人工 `runClient` 签收
- [x] 覆盖页重建：BurnMixer、CatalyticCracking、CrystallisationCrucible 为 `full_replay`。SteamCracking 与 ScannerVisuals 仍 `denominator_only`（配方不在本卡）
