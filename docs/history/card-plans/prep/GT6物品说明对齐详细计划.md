# GT6 物品说明对齐详细计划

> 计划 slug：`presentation/gt6-item-tooltip`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 物品说明对齐
> 性质：把工具、金属形态、加工机、管缆的物品悬停补到 GT6-TFRU 已有的行。
> 不占 unique-active。不新增物品、配方、形态。不改获得格。
> 不是 `player_complete`。
>
> Java/tick 源：`gt6_tfru/gregtech6-TFRU`。
> kTFRUAddon 没有自己的悬停事件；它的机器 `addToolTips` 后调用 `super`，
> 所以对齐 GT6 共享行即可，不把 addon 专有结构句抄进本卡。
> Jade / Waila 是另一张表面，合同见
> [gt6-tfru-waila-jade-format.md](../../../current/gt6-tfru-waila-jade-format.md)。
> 本卡只写物品悬停。

```text
lane                    = prep
capability_slug         = presentation/gt6-item-tooltip
unique_active_wave      = null
prep_owned_paths        = src/main/java/com/masson/cruciblecraft/client/tooltip/**
                          src/test/java/com/masson/cruciblecraft/client/tooltip/**
landing_owned_paths     = MaterialToolItem / MaterialElectricToolItem 及已有额外行的工具子类；
                          PipeBlockItem / CableBlockItem；
                          MaterialMachineBlockItem / LargeCrucibleBlockItem；
                          已有 appendHoverText 的能源 BlockItem；
                          ProcessingMachineIoTooltips / ProcessingMachineItemTooltip；
                          ModLanguageProvider 与生成的 en_us.json / zh_cn.json
landing_depends_on      = 当前 unique-active machines/gt6-multiblock-tanks 关闭
survival_access         = not_applicable
```

签发只合本文件。不创建 `capability.json`，不改 `unique_active_wave`，不开实施分支。

## 0. 开场判断

GT6 的物品说明是两层叠在一起：

1. 物品自己的 `addInformation`。机器物品按 meta 造临时方块实体，调用 `addToolTips`。工具走 `MultiItemTool.addAdditionalToolTips`。金属前缀物品的 `addInformation` 是空的。
2. 客户端 `GT_API_Proxy_Client.onItemTooltip`。化学式、工具材料 Q/S/D、可燃、F3+H 成分都在这里。

CC 已经有这三处入口，行数短一截：

| 物品 | 现在 | GT6 同位置还有 |
| --- | --- | --- |
| 公共形态 / 长尾前缀 | 公共形态一行化学式。长尾 `PrefixMaterialItem` 没有 | Q/S/D、可燃标记、高级提示里的熔点沸点 |
| 工具 | 材料名、材质等级、耐久。电动工具再加电量 | 等级、挖掘速度、近战、合成次数 |
| 单方块加工机 | 能量和物品流体的面，螺丝刀，活动扳手 | 配方表名、效率、并行、能量最小–最大、廉价超频 |
| 管 / 缆 | 容量或电压电流损耗 | 流体管还有 L/t、耐性、接触伤害；电缆还有电压等级名 |

数字必须来自已经导入的 metadata、`TierProfile`、`ElectricToolCatalog`、`PipeCatalog`。字段不在这些对象上的行，本卡不写，也不为了说明去扩材料 JSON。

颜色用 `ChatFormatting`，对应 GT6 的黄 / 蓝 / 绿 / 红 / 灰。不复制 `§` 控制码，不做闪烁。

## 1. 做

行生成器放在 `client/tooltip`，单元测试直接断言行文本。接到现有 `appendHoverText` 和三个 `ItemTooltipEvent` 上，是落地那一步。

### 金属

`MaterialFormItem` 与 `PrefixMaterialItem` 走同一条。长尾材料从 `PREFIX_MATERIAL` 解析，规则与 `MaterialUnits.resolve` 相同。

已有数据就写：

- 化学式。前缀必须落在 GT6 `TD.Prefix.TOOLTIP_MATERIAL` 那一组（锭、粉、板、杆等，见 `OP.java` 的 `.add(..., TOOLTIP_MATERIAL)`）。不给每个前缀都加化学式。
- `tool.types > 0` 时写 `Q` / `S` / `D`，读 `GT6MaterialMetadata.ToolStats`。
- `material_tags` 里已有的 `PROPERTIES.FLAMMABLE`、`PROPERTIES.EXPLOSIVE`、`PROPERTIES.UNBURNABLE`。
- 高级提示（`TooltipFlag.isAdvanced()`，即 F3+H）：单位数用 `MaterialUnits`，熔点沸点用已导入的开尔文。重量公斤只在和 GT6 `OreDictMaterialStack.weight()` 对过样之后再写；对不上就停在熔点沸点，不另造公式。
- `source_id` 在 1–7999 时写「元素周期表」。不猜测模组名。

### 工具

`MaterialToolItem` 保留耐久和现有用途句（撬棍、软锤、活塞、口袋多工具模式）。

- 电动工具：等级、挖掘速度、近战、合成可用次数，读 `ElectricToolCatalog` 的 `baseQuality` / `baseDamage` / `speedMultiplier` 加上材料 `ToolStats`。电量行保留，并带上已有的推荐包大小。
- 手持工具：挖掘速度用 `ToolMaterialRules.miningSpeed`。等级、近战、合成次数只有在该工具的 live 属性或合成扣耐久已经使用同一组数字时才写。`ToolKind` 只有准入门，没有 GT6 `IToolStats` 的基础伤害和每次合成消耗。本卡不改挖掘和攻击，也不印一套和实际属性不一致的数字。

### 机器、管、缆

加工机继续走 `ProcessingMachineIoTooltips`。在现有面列表之外，用 `TierProfile` / `MachineKindSpec` 补：

- 配方表显示名
- 效率百分比（`efficiency` 是万分比，和 GT6 `getToolTipEfficiency` 一样）
- 并行上限；`parallelDuration` 用 GT6 的并行时长措辞，否则用并行次数
- `OverclockPolicy.CHEAP` 时写廉价超频
- 能量 `inputMinimum - inputMaximum`，单位跟 `EnergyType`，面列表留在后面

灰字工具提示只保留 CC 里已经生效的交互：螺丝刀切换、活动扳手自动面、放大镜查看（`magnifyingInspect` 已在）。软锤重置、建筑权杖成型、扳手改朝向，先对一下 live `ToolAction`；没有这条交互就不写这句话。本卡不补交互。

流体管在现有容量和耐温上补：流量按 GT6 写成 `capacityMb / 2` L/t，以及 `FluidPipeProperties` 上已有的气密、耐酸、耐等离子体、耐魔、接触伤害。物品管已有每秒组数和步进，保持。电缆在现有电压、电流、损耗上补 `GT6VoltageTiers.tierMax` 的等级名；接触伤害沿用 `contactDamage`。

坩埚、砧、大型坩埚、已有能源 `BlockItem`：材料、耐久、温度、HU、效率这些已有行保留。能量机在 profile 已有最小、最大、效率时补能量范围行。没有 profile 数字的机器不新造范围。

大型多方块已有的结构句保留，本卡不重写每台的结构散文。控制器如果同时有 `TierProfile`，结构句后面接上面的共享统计行。

## 2. 不做

- Jade、Waila、`IWailaTile`。物品说明不迁进 Jade。
- kTFRUAddon 专有机器和它的结构句。微型燃气涡轮已因 AGPL 从 CC 移除。
- 附魔列表、装柄材料、矿石「Source of」、无序合成数量、模组来源名。这些不在 `GT6MaterialMetadata` 里。
- 为了说明去改 `material_registration_gate`、公共 16、材料 JSON schema。
- 改 `MteInPlaceBlock`、储罐阀门、`machines/gt6-multiblock-tanks` 正在动的文件。储罐说明留在储罐卡。
- 新配方、新形态、stand-in 配料、占位贴图、主世界 `ItemEntity` 撒目录。
- 闪烁色、彩虹色、把 GT6 书里的「先读完 tooltip」抄进游戏。

## 3. 验收

开工前本文件只在 `card-plans/prep/`。`unique_active_wave` 仍是储罐卡。

落地后：

- 铁锭一类公共形态：普通悬停有化学式；工具材料有 Q/S/D；F3+H 有熔点沸点。长尾前缀在 `TOOLTIP_MATERIAL` 集合内时同样有化学式。
- 电动工具悬停的等级和速度与 `ElectricToolCatalog` 一致。
- 一台有 `TierProfile` 的加工机悬停含配方表名、效率、能量最小–最大，以及现有的面。
- 流体管悬停含 L/t 和已导入的耐性标记。
- `python tools/build_capability_ledger.py --check` 与 `python tools/build_project_status.py --check` 绿。
- 行生成器有 JUnit。不靠 GameTest 当说明文本的门。
- 关闭走 `python tools/close_capability.py --capability presentation/gt6-item-tooltip --change-class major`。悬停是玩家看的界面，不用 `minor`。本卡未关。
