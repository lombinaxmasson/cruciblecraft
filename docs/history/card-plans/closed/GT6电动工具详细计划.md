# GT6 电动工具详细计划

> 计划 slug：`content/gt6-electric-tools`
> capability_slug         = content/gt6-electric-tools
> unique_active_wave      = null
> 状态：paused / frozen（让出 unique-active 给 `energy/large-gas-turbine`）
> 正式名称：GT6 电动工具
> 性质：对照 `Loader_Tools.java` 156–174 的 19 个 `addTool` 身份全量导入。
> 不是建筑杖、不是口袋多功能、不是枪。不发明形态。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = paused
capability_slug              = content/gt6-electric-tools
unique_active_wave           = null
addTool                      = MININGDRILL/CHAINSAW/WRENCH LV–HV
                               JACKHAMMER_HV_Normal + No_Ores
                               BUZZSAW/SCREWDRIVER/DRILL/MIXER/TRIMMER LV
                               MONKEY_WRENCH LV–HV
energy                       = EU NBT e/f/g → ELECTRIC_CHARGE/CAPACITY/VOLTAGE
                               EnergyStat.makeTool 只充不放
                               doDamage 有电必花 EU，耐久 1/max(10, quality*20)
hull                         = Electric_T LV steel_galvanized / MV aluminium / HV stainless_steel
voltage                      = V[1]=32 V[2]=128 V[3]=512
out_of_scope                 = form-open；builder wand；guns；pocket multitool
                               stand-in 配料；占位贴图；主世界 ItemEntity 撒目录
```

来源：`Loader_Tools.java` 156–174、308–309、356–378，
`gregtech/items/tools/electric/GT_Tool_*.java`，
`MultiItemTool.doDamage` / `EnergyStat.makeTool`。

## 0. 开场判断

GT6 电动工具是同一套 meta tool 上的偶数 meta；奇数是空壳。合成出来是空电。
容量来自配方里的 `gt:re-battery1/2/3`（CC 电池 BlockItem）。
LV 采矿钻 / 链锯另外还有无电池的机械配方（308–309），同一 Item，无能量组件。

扳手与活动扳手、风镐两种模式是 sneak 右键换 Item，复制材料与能量组件。

## 1. 获得格

配方按 GT6 网格翻译。缺 live 形态就跳过该行，不 stand-in，不在本卡开闸。
机械链锯若缺 `chain` 前缀则跳过。Monkey / 风镐无矿模式无独立合成，只从配对工具切换。

## 2. 明确不接管

- 公共 16 / 长尾形态开闸
- 管缆切片 C
- 建筑杖、口袋多功能、枪
- 用别的材料或 `programmed_circuit` 顶缺格

## 3. 关闭清单

- [ ] 19 个 ToolKind + 独立 Item
- [ ] EU 容量/电压组件；充电柜按堆容量充
- [ ] 挖掘 / 点击 / 风镐锤碎 / 链锯伐木
- [ ] 源网格配方（仅 live 形态）
- [ ] gregtech6_w iconsets / metallic toolhead 贴图
- [ ] 语言 / 模型 / EMI / 合成工具标签
- [ ] GameTest / JUnit / unique-active 账本
