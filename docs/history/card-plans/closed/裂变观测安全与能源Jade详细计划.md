# 裂变观测安全与能源 Jade 详细计划

> 计划 slug：`energy/nuclear-fission-observation-safety`
> 状态：closed / player_complete（2026-09-08）。本文件位于
> `card-plans/closed/`。
> 正式名称：裂变观测安全与能源 Jade
> 性质：把裂变堆芯收成可观测、可防护、可失败的安全合同，并顺手做能源
> Jade 第二切片（电池 + 转换机）。本卡不是热力机器卡，也不发明堆芯
> Kelvin。机器可读 `unique_active_wave = null`。
> 不创建 `*_READY`。当前工作树不创建 Git commit。
>
> Java/tick 源：`gt6_code/gregtech6`，revision
> `3703e40308c8c030763fd6297dea8b210d2a77b1`。贴图源：本地
> `gt6_referencable_port_code/gregtech6_w`。不从 GitHub 补证据。

上一张内容卡
[裂变热流体与热量合同](../closed/裂变热流体与热量合同详细计划.md)
已关闭 11/9/8 热流体。候选队列第 7 项（裂变观测与安全）是本卡主合同；
电池 / 转换机 Jade 是同一套「读现有运行时」的第二切片，不是另开
Jade 卡。热交换器 / 蒸汽涡轮 / 冷却器是第 8 项，下一张再开。

权威边界来自
[总体规划](../../../current/roadmap.md)、
[冻结与未实现账本](../../../current/unimplemented-gap.md)、
[能力交付流程](../../../current/capability-delivery-workflow.md)、
[验证指南](../../../current/verification.md)、
[Jade 第一切片](../closed/生成资源注册与Jade第一切片详细计划.md)
和本地 `gt6_code/gregtech6`。

---

## 0. 开场判断

堆芯 Jade 单独做偏薄，但「仨都是 Jade」不是把辐射丢掉的理由。本卡固定为
两块合同叠在一张 unique-active 上：

```text
裂变收尾（主合同）
  2 堆芯 Jade
  + 温度计 HU / 盖革中子
  + 辐射 + 烫伤
  + 辐射/隔热防护服
  + 来源失败语义（杆毁 + 爆炸声 + 辐射脉冲）

能源 Jade 第二切片（并入，不另开卡）
  37 储能块共用一种显示
  + 活目录转换机一族共用一种显示
```

不能并的仍然不能并：热交换器、蒸汽涡轮、冷却器、罐 / Logistics Core /
大型多方块 Jade、聚变 Jade、Hopper 库存 Jade、虫/生化/霜/万能防护服、
盖革/温度计红石传感器方块（Sensors/Panels）。

8 个热流体仍然没有消费端。本卡可以关闭裂变观测与安全，不能宣称核电
能量环已经走完。

---

## 1. 固定分母与卡片合同

机器可读 wave / capability 在实施开始前不创建。计划签发阶段保持：

```text
capability_slug              = energy/nuclear-fission-observation-safety
core_identity_count          = 2
jade_new_families             = 3   (reactor_core, battery, converter_dynamo)
battery_identity_count       = 37
converter_live_rows          = 179  (catalog 分母 169；多出的 10 台电加热器/电引擎共用同一 provider)
handheld_tool_behaviors       = 2   (thermometer HU, geiger neutrons)
hazmat_piece_count           = 8   (radiation 4 + heat 4)
radiation_apply_paths        = 3   (contact, area, fail burst)
owns_families                = 0
new_processing_machine       = 0
unique_active_wave           = null until implementation starts
nuclear_started              = true (由裂变生存卡保持)
partial_close_allowed        = false
```

坩埚 / 变压器 Jade 已由第一切片 `player_complete`，本卡不重做。锅炉 /
蒸汽机已有 client tooltip；本卡只给缺 server-data 的转换机 kind 补合同，
不得拆掉已有锅炉水位/蒸汽/HU 或蒸汽机 KU/行程字段。

### 1.1 堆芯 Jade（HU，不是 K）

GT6 `MultiTileEntityReactorCore` 温度计工具聊天是
`"Heat Levels: " + oEnergy + " HU"`。堆芯不是 `ITileEntityTemperature`。
Crucible Jade 的 `temperature_k` / `meltdown_at_k` 不得抄到堆芯。

两核共用一个 `IServerDataProvider`，字段只能来自已有 runtime：

| section | 来源 | 单位 |
| --- | --- | --- |
| `heat` | `ReactorCoreBlockEntity.heat` | HU |
| `last_heat` | `lastHeat`（GT6 `oEnergy`） | HU |
| `neutrons` | `oldNeutrons` 之和（GT6 `oNeutronCounts`） | n |
| `coolant` | 输入罐流体与量 | mB |
| `output` | 输出罐流体与量 | mB |
| `running` / `stopped` | `running` / `stopped` | bool |
| `safety` | 棒是否被毁、输出满导致转换停滞 | 状态词 |

禁止：`temperature_k`、`meltdown_at_k`、`HU × 常数 → K`。

### 1.2 电池 Jade

37 个 `EnergyBatteryCatalog` 身份共用一种显示，不写 37 个 provider。
字段来自 `BatteryBlockEntity` / `BatteryEnergyStore` / `EnergyBatteryProfile`：

- `energy_type`：EU 或 LU
- `stored` / `capacity`
- `packet`：`sizeMin` / `sizeMax` / `inputSize`

同步路径：`jade_server_data`。创造栏电荷或 `displayedEnergy` 字节不能冒充
真实 `stored`。

### 1.3 转换机 Jade

活目录 179 行共用一种 observation 适配，按 `EnergyConverterProfile` 投影，
不按 GT6 数字 MTE id 分派。已有锅炉 / 蒸汽机 tooltip 保留；本卡补：

- `accepts` / `emits`
- 输入/输出 packet 与窗口
- 活动与缓冲（各 BE 真实字段；没有的 section 标 `unavailable`，不编数字）

电加热器 / 电引擎这 10 台在活目录里，跟着同一 provider 走；不得因为转换机
卡分母是 169 就把它们从 Jade 里删掉，也不得把它们算进已关闭
`energy/converter-catalog` 的 `player_complete` 配方分母。

### 1.4 手持读数

| 工具 | GT6 身份 | CC 现状 | 行为 |
| --- | --- | --- | --- |
| 温度计 | `IL.Thermometer_Quicksilver` meta 10000 | **目录缺失** | 对堆芯聊天 `"Heat Levels: {lastHeat} HU"` |
| 空盖革 | `IL.Geiger_Counter_Empty` m10001 | 已注册，无行为 | 未充气，不读数 |
| 盖革 | `IL.Geiger_Counter` m10002 | 已注册，无行为 | `"Neutron Levels: {sum}n"` + ON/OFF |

盖革传感器方块 `31020` 与温度计传感器方块 `31000` 是 Sensors/Panels，
**out of scope**。放大镜 `TOOL_magnifyingglass` 仍在工具余量，Jade 已覆盖
其罐/开关信息，本卡不注册放大镜。

### 1.5 辐射与烫伤

来源 `MultiTileEntityReactorCore` / `1x1` / `2x2` 与 `UT.Entities`：

1. **接触**：`mRunning` 时 `applyHeatDamage(entity, 5)` +
   `applyRadioactivity(entity, 3, 1)`。
2. **区域**：每 20 tick 的第 10 tick，
   `tCalc = sum(oNeutronCounts)/256`，水平 200 格内
   `strength = bindInt(tCalc - distance)`，
   `applyRadioactivity(level = divup(strength, 10), amount = strength)`。
3. **失败脉冲**：`tIsExploding && !invempty()` 时 `tCalc *= 2`，水平 500 格。

`applyRadioactivity`：非亡灵、非节肢、未穿满辐射服；效果为缓慢、挖掘疲劳、
反胃、虚弱、饥饿；无 IC2 / `PotionsGT.ID_RADIATION` 时走 GT6 的凋零回退。
`EntityFoodTracker` 属于 Food 轨，本卡跳过，不发明食物辐射条。

`applyHeatDamage`：非烈焰、无抗火、未穿满隔热服；伤害 5。
这与 `MaterialContactHeat`（锭/块接触热）不是同一条路径。

背包物品 `Enchantment_Radioactivity` / `getRadioactivityLevel` 只有在 D0
找到 CC 材料已有辐射等级时才接线；没有就 `blocked`，不编等级表。

### 1.6 防护服

GT6 `isWearingFullRadioHazmat` / `isWearingFullHeatHazmat` 检查装备槽 1–4
（靴、腿、胸、头）。完整套装才免疫。创造模式免疫。

| 套装 | GT6 四件 | CC 现状 |
| --- | --- | --- |
| 辐射 | `gt.armor.hazmat.radiation.{head,chest,legs,boots}` | 只有胸/腿/靴，且是 `CatalogNamedItem`，**不能穿**；贴图是铁锭占位 |
| 隔热 | `gt.armor.hazmat.heat.{head,chest,legs,boots}` | 同上 |

本卡必须：把这 8 件做成可穿戴护甲、补两件头罩、从本地 `gregtech6_w` 拷来源
贴图并写 art manifest。禁止继续用铁锭、其它套装或原版锁链顶。

虫/生化气/霜/万能套装 out of scope。

### 1.7 「熔毁」= 来源失败语义，不是坩埚 K

GT6 `tIsExploding` 在冷却转换失败或空罐且 `oEnergy > 0` 时置位，然后：

- `slotKill`（毁棒）—— CC 缺冷却剂路径已有；
- `UT.Sounds.send(SFX.MC_EXPLODE)` —— CC 还没有；
- 辐射脉冲见 §1.5；
- `explode(8)` / `explode(10)` 在源里是注释掉的 TODO。世界方块爆炸保持
  `blocked`，与 GT6 一致，不解开。

热流体卡已经把**输出满背压**定为 DESIGN_POLICY：保留热量与输入，不走
`tIsExploding`。本卡不得静默改回 GT6 毁棒。Jade `safety` 必须能区分
「缺冷却剂已毁棒」和「输出满、转换停滞」。

---

## 2. In scope / Out of scope

### 2.1 In scope

- 2 堆芯 Jade server-data 与 en_us / zh_cn 键；
- 37 电池 Jade；179 活转换机一行 observation 合同；
- 温度计身份 10000 + 堆芯 HU 读数；盖革空/满行为；
- 接触 / 区域 / 失败三条辐射与烫伤 5；
- 辐射 + 隔热各 4 件可穿戴护甲与满套免疫；
- 失败：毁棒 + 爆炸声 + 辐射脉冲；Jade 安全状态；
- 阻塞本卡获得路径的真实零件（见第 5 节 D0），缺则 `blocked` 不 stand-in；
- 更新 `tools/jade_observation_matrix.json` 本卡三个 family。

### 2.2 Out of scope

- 热交换器、蒸汽涡轮、冷却器、大型锅炉；
- `temperature_k` / `meltdown_at_k` / 解开 GT6 `explode()` TODO；
- 罐 / Logistics Core / 大型多方块 / Hopper 库存 / 聚变 Jade；
- 盖革/温度计传感器 MTE、放大镜、电动工具世界行为；
- 虫/生化/霜/万能防护服；
- 食物辐射条；
- Bath 150、fluidbed 49、验证债 `VD-2026-09-*`；
- 用其它材料、prefix、原版零件或单个 `programmed_circuit` 顶盖革空壳 /
  头罩 / 温度计。

---

## 3. Owned paths

实施允许触及：

```text
src/main/java/com/masson/cruciblecraft/compat/jade/**
src/main/java/com/masson/cruciblecraft/content/block/ReactorCoreBlock.java
src/main/java/com/masson/cruciblecraft/content/blockentity/ReactorCoreBlockEntity.java
src/main/java/com/masson/cruciblecraft/nuclear/**
src/main/java/com/masson/cruciblecraft/content/item/  (hazmat armor, thermometer, geiger behavior)
src/main/java/com/masson/cruciblecraft/registry/ModItems.java
src/main/java/com/masson/cruciblecraft/datagen/ModLanguageProvider.java
src/main/java/com/masson/cruciblecraft/gametest/NuclearFission*GameTests.java
src/main/resources/assets/cruciblecraft/gt6_fission_observation_art_manifest.json
src/main/resources/assets/cruciblecraft/textures/**/gt6_import/**
src/main/resources/data/cruciblecraft/semantic_object_catalog.json  (仅本卡 8 件护甲 + 温度计)
src/test/java/com/masson/cruciblecraft/nuclear/**
src/test/java/com/masson/cruciblecraft/compat/jade/**
tools/jade_observation_matrix.json
tools/capabilities/energy/nuclear-fission-observation-safety/**
tools/waves/runtime/fission-observation-safety/**
tools/tests/test_energy_nuclear_fission_observation_safety.py
docs/current/**
```

不改已关闭裂变生存 / 热流体卡的 seal。不改 `presentation/live-art-jade`
的坩埚/变压器合同。验证精简债不占用本 lane。

---

## 4. Jade 与安全合同（不得发明）

```text
reactor Jade unit                 = HU
reactor kelvin fields             = 0
world explode uncommented       = 0
battery providers                = 1 shared
converter providers               = 1 shared observation adapter
hazmat iron_ingot textures       = 0
```

背压保留是已关闭热流体卡的 DESIGN_POLICY。失败声与辐射脉冲只挂在
**已经毁棒**的路径上（空罐 / 冷却剂不足），不挂在输出满停滞上。

---

## 5. D0 获得矩阵（实施第一天必须写盘）

配方必须 source-exact。缺格 = 该行 `explicitly_blocked`，能力不得靠创造栏
晋级 `player_complete`。

| 行 | GT6 来源 | 已知风险（签发时） |
| --- | --- | --- |
| 辐射头罩 shaped | 铅板 + 玻璃板 + 螺丝起子/锉 | 身份缺失；`x`/`l` 是工具催化剂 |
| 辐射胸/腿/靴 | 铅板 + 工具催化剂 | 身份在 catalog，但是普通物品 |
| 隔热头罩 | 铝箔 + 黑色染色玻璃板 + 工具 | 身份缺失 |
| 隔热胸/腿/靴 | 铝箔 + 工具 | 同上 |
| 温度计 shaped | 铜板 + 玻璃板 + 红染料 + `OD.itemQuicksilver` | 10000 未注册；水银/朱砂映射 D0 核对 |
| 空盖革 shaped | `OP.capcellcon(Al)` + 铝板 + 铝螺丝 + `OD_CIRCUITS[1]` | `capcellcon` 是胶囊单元容器 MTE（Al = 32610），不是活 prefix；**极可能 blocked** |
| 盖革 Canner | 空盖革 + 1000 mB He / Ne / Ar，16 EU/t 64 tick | 空壳 blocked 则三行一起 blocked |

`circuit_basic` 已由裂变生存卡做出，可以当 `OD_CIRCUITS[1]`。不得把空盖革
的 `capcellcon` 换成铝板或电池壳。若 D0 确认胶囊容器超出本卡，盖革获得
保持 `explicitly_blocked`；手持行为仍要接线，创造栏不能当 `player_complete`。

防护服与温度计若零件齐全，必须进 `player_complete` 分母。盖革获得行单独记账，
不得从分母里删掉来假装绿。

---

## 6. 验收门

### 6.1 分母

```text
reactor cores                      = 2
reactor Jade kelvin fields         = 0
battery Jade identities            = 37
converter live rows on one contract = 179
hazmat wearable pieces             = 8
world explode                      = blocked
hot-fluid consumers added          = 0
```

### 6.2 Runtime

- 两核 Jade 的 HU / 中子 / 罐 / 开关 / 安全与 runtime exact；
- 运行中接触：无服烫伤 5 + 辐射效果；满辐射服免辐射，满隔热服免烫伤；
- 区域辐射按 `neutrons/256` 与距离；失败脉冲在毁棒时加倍、500 格；
- 缺冷却剂毁棒 + 爆炸声；输出满不毁棒、不丢热；
- 电池 stored/capacity/packet 与存档一致；
- 转换机 Jade 不编没有的缓冲；
- 旧裂变生存 / 热流体 GameTest 回归。

### 6.3 日常验证（实施期）

不要把全量 `player-complete` / `runClient` / 全量 `gradle test` 写进每日
checkbox。日常：

```powershell
python tools/verify.py dev
```

只跑 dirty path 命中的 profile。本卡若改 Java / 测试 / `src/main/resources`，
`dev` 会拉 `runtime-java`。`promotion` 与 `--client` 只在
`runtime_ready → player_complete` 时强制。`release` 是另一道检查点。

---

## 7. BLOCKED / 撤回

必须保持 blocked：

- 堆芯 Kelvin / `HU × 常数`；
- 解开 GT6 `explode()` TODO；
- 输出满改回毁棒（除非显式推翻热流体 DESIGN_POLICY，本卡不这么做）；
- 空盖革用铝板 / 电路 / 原版物品顶 `capcellcon`；
- 防护服继续铁锭贴图或不能穿；
- 注册热交换器、涡轮、冷却器、传感器 MTE、聚变 Jade。

撤回时保留 D0 矩阵与 blocked 行；不要删已确认的护甲/工具身份。

---

## 8. 关闭清单

只有以下条件同时成立才关闭：

- [x] 2 堆芯 Jade HU/中子/罐/开关/安全 exact，无 Kelvin；
- [x] 37 电池 Jade 一种合同；
- [x] 179 活转换机一种 observation 合同；已有锅炉/蒸汽机字段未丢；
- [x] 接触 / 区域 / 失败辐射与烫伤 5 有测试；
- [x] 8 件辐射+隔热可穿戴、满套免疫、来源贴图；
- [x] 失败 = 毁棒 + 声 + 脉冲；世界爆炸仍 blocked；
- [x] 温度计 / 盖革行为接线；获得行要么 exact 要么 `explicitly_blocked`；
- [x] 无 stand-in、无铁锭护甲贴图、无热力机器偷渡；
- [x] capability、player signoff、wave receipt、Jade matrix、docs currentness；
- [x] 本文件移到 `card-plans/closed/`；
- [x] `unique_active_wave = null`。
