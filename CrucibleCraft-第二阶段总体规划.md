# CrucibleCraft 第二阶段总体规划

> GregTech 6 → Minecraft 1.21.1 NeoForge 移植 · 第二阶段
> 起草：2026-08-04　架构重基线：2026-08-05 T10 完整关闭工作树
> 前置：第一阶段 T0–T6 已全部关闭（材料 / 标签 / 矿链 / 组件 / 工具 / 化学流体 / 电力）

---

## 0. 使用说明与三套坐标

### 0.1 编号：继续用 T7–T12，不重新从 T0 开始

第一阶段文档第 1 节写着「层级体系（唯一，禁止再造）」，第 4.4 节把「造第二套编号 / 命名体系」列为出现即代表跑偏。2026-07-31 那次 `t1_generated` 与 `T2_MACHINES` 指向不同阶段的事故就是这条规则的来源。

所以第二阶段的六个阶段编号是 **T7 – T12**，接在同一条阶梯上。下表的「序」列只是阅读顺序，不是编号，不进 commit message、不进代码常量、不进目录名。

| 序 | 编号 | 名称 | 一句话 |
|---:|---|---|---|
| 0 | **T7** | 材料事实进入规则语言 | 已导入的 GT6 事实要能被规则读到 |
| 1 | **T8** | 物流骨架 | 机器之间的东西要会自己动 |
| 2 | **T9** | 世界生成数据化 | 矿脉和流体矿床要能批量声明 |
| 3 | **T10** | 容器与形态补全 | 流体/热锭要有载体形态 |
| 4 | **T11** | 石油天然气与第二发电路线 | 化石能源链闭环 |
| 5 | **T12** | 机器分级与多方块通用层 | 技术树的纵深 |

如果你确实想让第二阶段本地从 T0 开始，那必须同时做一次全局改名（文档 + 代码常量 + commit 约定），不能两套并存。**我的建议是不改。**

### 0.2 这份文档和第一份的关系

第一份文档不作废。方法论（第 5 节）、开工/收尾检查表（第 7/8 节）、架构不变量（第 9 节）和判断失误记录（附录）全部继续生效。本文是第二阶段的前台导航，只保留当前跑道、活跃阶段、快照、悬案与第二阶段特有增补。

**唯一的硬规则不变：任何时刻最多一个阶段是「进行中」。**

### 0.3 移植口径与完成度

CrucibleCraft 的目标是：**GT6 核心玩法与物理语义高保真，代码架构允许现代化。** 因此可以用 JSON catalog、DataComponent、统一事务和现代网络能力重写实现，但不能因为接口复用而折叠 RU / KU，不能用统一平衡模板冒充 GT6 数值，也不能把阶段闭包写成完整移植。

本文所有阶段状态沿用第一份规划新增的三轴口径：

- 闭包：数据、注册、运行时和泛化链路是否闭合。
- 保真：来源事实或明确设计策略是否可追溯、可全量对账。
- 载荷：datapack、publication 与 reload / index 是否分别过预算。

估「还剩多少」时，再用第 **2** 节的 GT6 十大类（G1–G10）锚定位置：进度按玩法系统记账，不按 `gt6_code` 文件个数记账。

三套坐标不能互相冒充：

- **T7–T12**：唯一的执行顺序，同一时刻最多一个阶段进行中。
- **G1–G10**：GT6 玩家可感知系统的工作量范围，不是里程碑。
- **架构支柱**：跨阶段依赖门禁，没有编号，也不新增阶段。

### 0.4 阅读入口

- **今天做什么**：T10 已关闭；先看第 1 节架构跑道，再从第 4 节的 T11a 继续。
- **还剩多少工作**：看第 2 节 G1–G10 工作量地图。
- **当前事实与阻断**：看第 3 节差距/成熟资产与第 5 节快照。
- **为什么 T7–T9 已关闭**：查《[CrucibleCraft-阶段档案-T7-T9.md](CrucibleCraft-阶段档案-T7-T9.md)》。
- **哪些规则不可违反**：查《[CrucibleCraft-总体规划.md](CrucibleCraft-总体规划.md)》第 5、7、8、9 节。

---

## 1. 架构跑道仪表盘

> 支柱在文档中前置，代码仍由实际消费它的阶段负责。T10 只关闭规则/来源和持续事务门禁，不等待完整 RU 网络、机器分级或多方块框架。

| 架构支柱 | 当前状态 | 直接来源 | 当前阻断 | 最晚决策点 | 实现 owner |
|---|---|---|---|---|---|
| **MaterialRule / provenance** | T10 已加入 1,500 条独立预算、未知未来阶段 fail-closed、前缀事实 codec/fingerprint 与 1,288 条 known-form 路线 | 固定 GT6 `OP.java` / `UT.java` Git blob、normalized prefix dump；现有 T7/T8/T10 rule epoch | T10 阻断已关闭；热锭冷却属于明确 CC 玩法 ADR | T11 新增流体路线前复算 | ✅ **T10 已关闭** |
| **事务 / epoch / 双预算** | 多罐、物品/流体/能量提交与 recipe 原子发布已是公共底座；datapack、publication、压缩比、reload/index 五项门禁已闭合 | CC T5–T10 事务、loader、handshake 与测试；总体规划 §5.2 / §9.4 | 当前 5,958 entries / 18,871 publication / 3.167:1，均在预算内 | T11/T12 每阶段持续复算 | ✅ **T10e 已关账** |
| **RU / KU 能源语义** | 接口已参数化；当前 `KINETIC` 仍折叠旋转 RU 与推压 KU | GT6 `TD.Energy.RU/KU`、`MultiTileEntityAxle`、`MultiTileEntityGearBox` | 24 台 host 尚无逐机能源 ledger，机器 spec 与拓扑未保留身份 | Kind/Tier schema 固化前 | **T12a** 身份，**T12e** 拓扑 |
| **MachineKind / TierProfile** | 来源模型已确认；CC 仍是单档且 `BY_MAP` 强制一 map 一 spec | GT6 `MT.DATA.*_T[]`、`Loader_MultiTileEntities` | kind、tier、存档 schema 与 runtime capability 仍耦合 | 注册首个多档机器前 | **T12b–c** |
| **多方块 JSON / validator** | 只有 Coke Oven 一次性 Java 结构 | CC `CokeOvenStructure*`；GT6 multiblock classes 与固定结构来源 | 几何、谓词、端口、朝向没有 catalog/schema；`CapacityMatcher` 边界未测 | 第二个真实多方块前 | **T12d**；性能边界在 **T12e** |

依赖关系只约束最晚关闭点，不要求把 T12 的实现塞进 T10：

```text
MaterialRule / provenance ──T10a──► T10b–e ──► T11
事务 / epoch / 双预算 ─────持续门禁──────────► T10 ──► T11 ──► T12
RU / KU 来源台账 ─────────────────────────────────────► T12a ──► T12c/e
MachineKind / TierProfile ─────────────────────────────► T12b ──► T12c
多方块 JSON / validator ───────────────────────────────────────► T12d/e
```

---

## 2. GT6 源码大类工作量地图

> 用途：评估「还剩多少」时先锚定大类，不要按 1,229 个 Java 文件逐个算。
> 刻度：Java 文件数只是**工作量代理**；真正的进度仍按闭包 / 保真 / 载荷三轴。
> 固定 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`；dump：95 RecipeMap / 720,841 recipes。

GT6 源码不要按 `gregapi` / `gregtech` 目录原样抄成进度表。下面十个大类按**玩家能感知的系统边界**划分，每个大类都有明确的源码锚点、规模代理和当前三轴状态。

| ID | 大类 | GT6 源码锚点（固定 revision） | 规模代理 | 闭包 | 保真 | 载荷 / 备注 | 排期 |
|---|---|---|---:|:-:|:-:|---|---|
| **G1** | 材料 / 前缀 / OreDict | `gregapi/data`（`MT`/`OP`/`TD`）、`oredict`、`item/prefixitem`、`block/prefixblock` | 1,773 材料；ITEMGENERATOR **16 / 25**；56 前缀 / 16,048 注册形态 | ● | ◐ | T10 multi/hot 967 个形态已激活；外围植物等域未注册 | 其余见 G8/G10 |
| **G2** | 配方语言与 RecipeMap | `gregapi/recipes`、`gt6_dump` 95 maps | 720,841 source recipes；CC live publication 18,871 | ● | ◐ | 5,958 entries，压缩比 3.167:1；大量 map 未投影 | 持续；T11/T12 增 map |
| **G3** | 单块加工机器 | `gregapi/tileentity/machines`、`Loader_MultiTileEntities` Basic Machines | CC **24** host（23 configured + crusher），全单档 | ● | ○ | 同 RecipeMap 多档未建；`BY_MAP` 仍 1:1 | **T12b–c** |
| **G4** | 能源物理与传输 | `TD.Energy`、`tileentity/energy`、`connectors`（cable/axle）、`GearBox` | EU cable 完整；RU/KU 折叠为 KINETIC；HU/CU/磁/量子等未开 | ◐ | ○ | 接口多类型已有；物理身份与拓扑未齐 | **T12a/e**；O-30 |
| **G5** | 物流与 Cover | `connectors` pipe、`gregapi/cover`（65 Java）、`logistics` | 282 pipe blocks；cover **3 / ~65 类** | ● | ◐ | 最小自动化已闭合；全量 cover/非金属管获取未完 | O-27/O-28；第三阶段扩展 |
| **G6** | 多方块 | `tileentity/multiblocks`（gregtech 35 + gregapi 9） | CC **1**（Coke Oven 硬编码） | ○ | ○ | 通用 JSON 层未建 | **T12d**；O-31 |
| **G7** | 世界生成 | `gregapi/worldgen`、`gregtech/worldgen`（约 87 Java） | 134 vein features；2 fluid deposits；几何 `UNIFORM_PLACEHOLDER` | ● | ○ | 分类/放置闭合；GT6 几何未导入 | O-29（不重开 T9） |
| **G8** | 流体 / 化学 / 燃料链 | `fluid`、chemical/distillery dump、`fuels.*` maps | live 非熔融 **108 / 119**；distillery projectable **1 / 1,517** | ◐ | ○ | T10 补齐 cell 域身份；油气与第二发电未开 | **T11**；O-35 |
| **G9** | 容器 / 热态 / 工具形态补全 | `ITEMGENERATOR` 的 HOT/MULTI/CONTAINERS*；工具已在 T4 | hot 321 + multi 646 路线；61/48 双通用 cell allowlist；14 例外 | ● | ◐ | 闭包和载荷已关闭；热锭冷却曲线以 `DESIGN_POLICY` 自描述，GT6 等价性见 O-36 | ✅ **T10 已关闭** |
| **G10** | 外围轴（暂不进主线） | `plants`/`food`、`portals`、`sensors`、`batteries`、`computer`、`compat`、`asm`、护甲/弹药 item | plants 1,179 / armors 613 / projectiles 854 等 | — | — | 明确推迟；不计第二阶段完成度 | 第 7 节范围之外 |

图例：● 该轴已可宣称关闭　◐ 部分关闭或有显式债　○ 未关闭　— 本阶段不计量

**怎么用这张表估工作量**

1. **先看大类，不看文件数。** `gregtech/tileentity` 有 243 个 Java，但真正卡住进度的是 G3+G4+G6 三条架构债，不是 243 个类逐个移植。
2. **同一大类可以闭包绿、保真红。** 例如 G7：T9 闭包已关，几何保真仍在 O-29。
3. **第二阶段主路径只吃 G1 残项 → G9 → G8 → G3/G4/G6。** G5 最小面已关；G2 随各阶段增量投影；G10 不抢主线。
4. **第三阶段起再清点 G3 全机器表、G6 全多方块表、G5 全 cover 表**（O-33），禁止继续用 Wiki 或 CC 当前数量反推 GT6 总量。

**与 T 阶段的对应（阅读顺序，不是第二套编号）**

```text
G1 材料事实 ──已关──► G2 规则/RecipeMap ──增量──►
G5 物流最小面 ──已关──► G7 worldgen 闭包 ──已关──►
G9 容器/热锭（T10）──► G8 油气发电（T11）──►
G3 机器分级 + G4 RU/KU + G6 多方块层（T12）
G10 外围轴：独立立项
```

---

## 3. 当前差距与成熟资产

这一节全部用**直接观测**得到，不用我对 GT6 的记忆推断。当前观测对象是工作树里的 1,773 份材料 JSON、56 个前缀定义、运行时代码，以及 policy 固定到 revision `3703e40308c8c030763fd6297dea8b210d2a77b1` 和两个 Git blob 的 GT6 源码与 dump。

### 3.1 最硬的证据：已导入但运行时消费不到的事实

第一阶段的 importer 把 GT6 的材料事实相当完整地搬进来了。问题是**搬进来的事实里，绝大部分现在没有任何运行时规则能读到**。

| 事实 | 数据里有 | 运行时消费点 | 状态 |
|---|---:|---:|---|
| `material_tags` · `PROCESSING.*` | 23 种 / 3,733 条标注 | **1 种** | T7 mortar 规则已消费 `MORTAR_GRINDABLE` |
| `material_tags` · `PROPERTIES.*` | 39 种 / 3,019 条标注 | **5 种** | T4 消费 `HAS_TOOL_STATS`、`WOOD`、`BOUNCY`、`STRETCHY`；T8 消费 `ACID` |
| `material_tags` · `ATOMIC.*` | 34 种 / 1,871 条 | **1 种** | T4 已消费 `ANTIMATTER` |
| `material_tags` · `COMPOUNDS.*` | 5 种 / 784 条 | **1 种** | T4 已消费 `COATED` |
| `gt6_metadata.explosion_damage` | 1,773 | **1 个数值变量** | `material.explosion_damage` 已接入；当前 1,773 份数据全为 0，正值行为由 synthetic fixture 锁定 |
| `gt6_metadata.heat_damage` | 1,773 | **1 个数值变量** | `material.heat_damage` 已接入；当前 1,773 份数据全为 0，正值行为由 synthetic fixture 锁定 |
| `MaterialPrefixDefinition.heat_damage` | 56 | **1 个运行时 consumer** | 唯一非零项 `ingot_hot = 3.0` 与材料级事实求和；普通/未知/非材料形态保持惰性 |
| `gt6_metadata.formula` | 952 | **1 个通用 tooltip** | 605 份已有注册形态、玩家可见；347 份零注册形态，待 T10 容器/热锭形态补全后可见 |
| `gt6_metadata.source_thermal.plasma_point` | 1,773 | 0 | 等离子体整个系统未开 |
| `gt6_metadata.generation_tags` | 25 种域 | 间接（见 3.2） | 只通过前缀表间接生效 |
| `tier` | 1,773（其中 **1,763 个为 0**） | 0 | 见下方警告 |
| `EnergyType` | 4 种 | **4** | 接口闭包；GT6 RU / KU 当前都折叠为 `KINETIC`，物理语义保真未关闭 |

> **注意口径**：其中一部分标签在 Python builder 里被消费过，生成物已提交并被测试锁定。上表统计的是**运行时的 MaterialRule / 机器逻辑能不能把它当输入**，不是「这个字段从来没用过」。这两件事必须分开说，否则会重犯 5.1 那类错误。

> **警告：`tier` 不是机器电压分级。** 1,773 个材料里 1,763 个 tier 为 0，非零的只有 10 个（1 级 4 个 / 2 级 5 个 / 3 级 1 个）。T12 做机器分级时**不能**拿这个字段当分级依据，那是典型的「用间接信号代替直接观测」。GT6 的机器电压分级是另一套来源，开工前必须先从固定 revision 里定位它。

**这张表就是第二阶段的第一个判据来源。** 按 5.2「许可 vs 声明」：数据里躺着一个字段而没有任何东西读它，等于一个永远不会红的断言。它不是「以后可以用」，它是**当前不成立的声明**。

### 3.2 GT6 自己的物品生成器域覆盖率

`generation_tags` 里的 `ITEMGENERATOR.*` 是 GT6 自己划分的物品生成域，一共 25 种。T10 已激活 `MULTIINGOTS`、`INGOTS_HOT`、`CONTAINERS_FLUID` 与 `CONTAINERS_GAS`，当前覆盖 16 / 25：

| 域 | 材料数 | 已实现 | 备注 |
|---|---:|:---:|---|
| `DUSTS` | 1,096 | ✅ | dust / small_dust / tiny_dust |
| `PLATES` | 862 | ✅ | |
| `STICKS` | 846 | ✅ | rod / long_rod |
| `ORES` | 618 | ✅ | 134 个 configured vein feature；129 条 catalog 几何仍是 placeholder |
| `PARTS` | 572 | ✅ | bolt / screw / ring / rotor / gear / spring |
| `FOILS` | 566 | ✅ | |
| `INGOTS` | 483 | ✅ | |
| `DENSEPLATES` | 436 | ✅ | |
| `MULTIPLATES` | 434 | ✅ | double / triple / quadruple / quintuple |
| `GEMS` | 217 | ✅ | |
| `MOLTEN` | 184 | ✅ | 204 种 molten fluid 已注册 |
| `WIRES` | 14 | ✅ | T6 关闭 |
| **`PLANTS`** | **1,179** | ❌ | 完全未开 |
| **`PROJECTILES`** | **854** | ❌ | 箭 / 弹药 |
| **`ARMORS`** | **613** | ❌ | 无任何护甲 Item |
| **`MULTIINGOTS`** | **435** | ❌ | double/triple 前缀事实已落地，形态与路线仍为零注册 |
| **`INGOTS_HOT`** | **433** | ❌ | hot 前缀事实与接触伤害 consumer 已落地，熔炼→冷却形态仍为零注册 |
| **`LENSES`** | **121** | ❌ | |
| **`CONTAINERS`** | **95** | ❌ | 罐 / 桶 |
| **`CONTAINERS_FLUID`** | **61** | ❌ | 流体单元 |
| **`CONTAINERS_GAS`** | **48** | ❌ | 气体单元 |
| **`LIQUID` / `GASES`** | 32 / 32 | ⚠️ | 119 候选中只注册 15 个 live |
| **`RAILS`** | 28 | ❌ | |

12 个域已实现，13 个域未实现。**这是一份用 GT6 自己的词汇表写的待办清单**，比我凭印象列 GT6 有什么可靠得多。

### 3.3 当前架构缺口（五类，按最晚决策点排序）

**① T10a 的两个代码级开工阻断已关闭**

- `GTRecipeMapLoader` 已把 `tN/` 解析与预算白名单拆开，T10 独立预算为 1,500；已知未来 1,288 条通过、1,501 条失败，未知未来阶段在 epoch 发布前 fail-closed。
- `ingotHot.mHeatDamage = 3.0` 已由前缀 codec/catalog/fingerprint 加载，并在每 20 tick 的玩家物品栏分片中与材料级事实求和，使用 vanilla heat damage source；1,773 份材料级零值未被改写。

三个 `CONTAINERS*` 域的 204 个 membership / 123 个去重材料已关闭：61 fluid + 48 gas 进入两个 1,000 mB 通用组件 cell，14 个 `CONTAINERS`-only 材料进入显式非流体例外；不扩展 `ComponentIngredientIndex`，也不生成 per-fluid item。

**② RU / KU 物理语义被当前 `KINETIC` 折叠**

`IEnergyHandler` 已经是多类型参数化接口，问题不在方法签名。固定 GT6 revision 明确区分 `TD.Energy.RU`（旋转）与 `TD.Energy.KU`（推压）：离心机 / crusher 等接 RU，sifter / compressor 等接 KU；RU 另有 axle / gearbox 拓扑。当前 16 台 KINETIC configured machine 与 Crusher 没有这层身份，只有相邻 `EnergyEmitter`。

完整传动轴网络可以晚于 T10，但逐机器能源台账和 spec 身份不能拖到机器分级之后，否则 T12 会把错误语义固化进档位模型。

**③ 机器分级来源已找到，运行时模型仍不存在**

GT6 直接来源是 `MT.DATA.Kinetic_T[]` / `Electric_T[]` 与 `Loader_MultiTileEntities` 的重复注册：同一 `RecipeMap`，不同建造材料、输入功率、并行和其他 NBT。当前 CC 的 24 台 processing host 全是单档，`ProcessingMachineSpec` 把工艺、槽位和能力冻结在一起，`BY_MAP` 还强制一张 map 对应一个 spec。

T12 的阻断不是配方数量本身，而是必须先拆 `MachineKindSpec` / `TierProfile`，把发布期规范配方与运行时实例能力分开。

**④ 多方块没有通用结构层**

只有 `CokeOvenStructure` / `CokeOvenStructureLayout` 一次性硬编码；结构几何、方块谓词、朝向与热源坐标都在 Java。项目目标已确定会继续移植 GT6 / GT6U 多方块，因此「≤3 个继续硬编码」不再是有效分支。第二个多方块之前必须先建立 JSON structure catalog 与单一 validator。

**⑤ 闭包、保真与载荷尚未成为统一阶段账目**

T9 已证明这个缺口：129 / 129 分类与真实放置全绿，同时全部 catalog vein 仍用统一几何。T10 已把 datapack / publication / compression / reload / index 纳入同一门禁，关闭值为 5,958 → 18,871（3.167 : 1）。后续阶段必须按第一份规划 5.5 / 第 9 节持续复算。

### 3.4 已经成熟的基础（避免重复劳动）

为了不让第二阶段回头补已经关闭的东西，明确记下来：

- 材料 / 前缀 / 标签基础设施：**闭包成熟**，1,773 材料 / 16,048 注册形态 / 56 前缀（T10 三前缀精确注册 323 / 323 / 321）
- 矿物加工六段链：**运行时闭合**，1,980 条主链 concrete recipe + 137 条高版本矿块 crusher 入口 + 766 条熔炉捷径
- 组件形态与工具：**运行时闭合**，8,141 + 3,452 条 live recipe，11 类工具
- 化学与流体机器：**23 台 configured machine**，152 条 T5 recipe，多罐原子事务
- 电力传输：**ELECTRIC 域完整**，144 导体方块，逐段线损、过载烧断、端点契约降级；不代表 RU / KU 拓扑完成
- 配方索引 / 事务 / 存档 quarantine / EMI / Jade：**已成熟，第二阶段直接复用**

---

## 4. 当前路线与阶段卡

每张卡沿用四格：**判据**（一句话，可执行、可证伪）、**交付**、**范围之外**、**完成信号**。第二阶段新增两格：**依赖**和**先算的数字**。

判据的三个约束不变：全链路 / 不是手写的 / 换材料不改代码。

---

### 已关闭阶段与子阶段摘要（T7–T10）

- **T7 · 材料事实进入规则语言**：闭包已关闭——62 / 62 标签分类、101-tag fail-fast 词表与 220 条 authored mortar 展开已由运行时纵切证明；保真为部分关闭——现有来源台账有效，但 RU/KU 折叠作为 O-30 独立债继续跟踪；载荷已关闭——T7 220 / 256、全局 publication 17,326 / 21,000。
- **T8 · 物流骨架**：闭包已关闭——282 个管方块、三种 cover 与物品/流体端到端自动化已通过；保真为部分关闭——管属性有固定来源，25 个非金属管获取路线与完整 cover 面保留 O-27/O-28；载荷已关闭——257 / 320，关闭时全局 publication 17,583 / 21,000。
- **T9 · 世界生成数据化**：闭包已关闭——129 / 129 vein 分类、双宿主放置与两种流体矿床已通过；保真未关闭——统一几何仍是 `PLACEHOLDER`，由 O-29 继续处理且不重开闭包；载荷已关闭——新增 263 份 worldgen 资源，关闭时 45 / 45 GameTest、421 Java / 236 Python 单测通过。
- **T10 · 容器与形态补全**：T10a–e 全部关闭——gate 精确激活 323 / 323 / 321 个 multi/hot 形态，四条 MaterialRule 发布 1,288 条路线；热锭具独立 HEAT、接触伤害与 COOLING 数据闭环；双通用 cell 闭合 61 fluid / 48 gas、93 个补充流体与 14 个显式例外。载荷为 5,958 datapack entries、18,871 / 21,000 publication、3.167:1，56-prefix / 1,829-handshake 不变。

完整判据、范围、完成信号和关闭快照见《[CrucibleCraft-阶段档案-T7-T9.md](CrucibleCraft-阶段档案-T7-T9.md)》。

---

---

### T10 · 容器与形态补全 ✅

**判据**

> `MULTIINGOTS`、`INGOTS_HOT` 与三个 `CONTAINERS*` 域全部分类；double / triple ingot 与 hot ingot 走材料前缀管线，经 ADR 选定的流体 / 气体 cell 能装载 gate 中的对应流体。smelter 产出的 hot ingot 具有来源正确的接触伤害，并经可执行冷却路线变为常温 ingot；新增材料只改数据。

**依赖**：T7（形态资格用标签驱动）、T8（容器与管道的语义边界要一起定）

**为什么在这个位置**

`CONTAINERS_FLUID`（61）/ `CONTAINERS_GAS`（48）是 GT6 化学链的实际载体；`INGOTS_HOT`（433）是熔炼流程的中间态，缺了它 smelter → anvil 这条线在 GT6 里的手感是错的。这两件事都卡在 T11 的化学 / 冶金深化前面。

**交付**

- `double_ingot` / `triple_ingot`：独立材料前缀与 `MaterialItem`，单位分别为 288 / 432；不改成组件化材料查找。
- `ingot_hot`：独立材料前缀，形态身份与 `HEAT` 温度组件正交。不能只用 `ingot + HEAT`，否则冷热堆叠、tag ingredient 与冷却配方无法可靠区分。
- 前缀事实运行时：`MaterialPrefixDefinition` 或等价 catalog 能承载并消费 `ingotHot.mHeatDamage = 3.0`，不污染材料级 metadata。
- 容器 ADR：在「每材料 cell Item」与「通用 empty cell + fluid/gas 内容组件」之间做出可审计决定。默认倾向通用 cell；bulk 运输继续由 64,000 mB 便携罐和管道承担。
- T10 的 stage budget、创造栏分栏、组件索引、handshake / fingerprint 与容器 recipe projection 一起进入门禁。

**先算的数字**

- 已知注册与路线投影：hot ingot 321 个形态 × 2 条 route = 642；multi-ingot 323 个材料 × 2 个前缀 = 646。
- 关闭值为 17,583 + 1,288 = 18,871 publication；三个容器域共 204 membership / 123 个去重材料，最终由 61 / 48 allowlist、14 denied 和 93 个补充流体闭合，cell runtime 内容不计 publication。
- 若容器走 component ingredient，`ComponentIngredientIndex` 当前只允许 `TOOL_MATERIAL` / `MACHINE_MATERIAL`，必须先设计索引与 fail-closed 边界；若不走 component ingredient，则要证明 cell 配方不依赖内容组件匹配。

**开工门禁**

1. ✅ 固定 revision 为 `3703e40308c8c030763fd6297dea8b210d2a77b1`；`OP.java` / `UT.java` Git blob、规范化前缀 dump 摘要与提取键均已进入 policy。
2. ✅ `ModProcessingMachines.AUTHORED_MATERIAL_RULE_BUDGETS` 已增加 T10 独立 1,500 预算和边界测试，未知未来阶段 fail-closed。
3. ✅ 前缀事实 codec / catalog / runtime consumer 已落最小纵切；1,773 份材料级零值只作反证，不从 melting point 猜接触伤害。
4. ✅ `tools/t10_preflight_projection.json` 已重建为 `T10_READY`；642 + 646 路线、容器 61 / 48 / 14 / 93、datapack、publication、压缩比与 reload/index 契约全部 fail-closed。
5. ✅ 三枚新前缀明确进入 `METALS_GEMS`、legacy alias、registration gate 零计数与 handshake；`MISC` 内建前缀数仍为 0。

**子判据**

- ✅ **T10a · 开工门禁与前缀事实层**：T10 stage budget、前缀事实 runtime、creative tab / legacy / gate 工具链已关闭；未新增批量内容。
- ✅ **T10b · MULTIINGOTS**：double / triple ingot 的 646 个形态与 anvil 路线全量双向相等，新增材料不改 Java。
- ✅ **T10c · INGOTS_HOT**：321 个 hot ingot 形态、smelter→hot 与 COOLING→ingot 路线全量等价；所有 hot 产物按形态直接加热，creative / command 等无 `HEAT` 入口由维护层补齐；真实背包接触与完整冷却时长由端到端 GameTest 验收。
- ✅ **T10d · 容器 ADR 与纵切**：双通用 cell、1,000 mB、空 64 / 满 1、fluid/gas allowlist 和 NeoForge 标准 stack-safe 交互已落地；单个及堆叠 cell 均可手动灌装，并完成 machine consumption 纵切。
- ✅ **T10e · 集成与账目**：三个容器域、93 个补充 chemical fluids、14 个例外、便携罐 / cell / pipe 分工和五项 load gate 已关闭。

**当前子阶段**：T10a–e 与 T10 整体已关闭；下一步为 T11a。publication 为 18,871 / 21,000，三枚新前缀 gate 注册为 323 / 323 / 321，容器 ADR 见《T10d-容器身份与边界ADR.md》。

**范围之外**

- 不做 `LENSES` / `RAILS` / `PROJECTILES` / `ARMORS`
- 不做背包 / 存储扩容类内容
- 不重写 T5 的多罐事务语义
- 不在 T10 实现环境气体扩散、燃烧或爆炸；这些属于 T11

**完成信号**

1. 三个 `CONTAINERS*` 域 + `INGOTS_HOT` + `MULTIINGOTS` 分类闭合，所有 placeholder / design policy 在数据内自描述。
2. 热锭接触伤害由前缀事实驱动；「smelter 出 hot ingot → 玩家接触受伤 → 冷却 → ingot」端到端 GameTest 通过。
3. liquid / gas cell 均完成 fill → transport → machine drain 纵切，便携罐仍保持 T5 事务语义。
4. 已知集合与独立期望集全量双向相等；T10 stage budget、datapack、publication、reload / index 全部过门禁。

---

### T11 · 石油天然气与第二发电路线 ⚪

**判据**

> 从 T9 的流体矿床守恒抽取原油 / 天然气，经 T8 管道或 T10 cell 送入 distillery；配方由流体 registration gate 与固定 GT6 recipe dump 投影台账发布，其中至少一种产物驱动非蒸汽发电设备并向 T6 电网投入。新增一种已具来源映射的烃类只改数据。

**依赖**：T8（管道）、T9（流体矿床）、T10（气体容器）—— **三个都必须先关闭**

**为什么不能提前**

天然气抽上来如果只能装便携罐、只能手搬、只能烧成蒸汽，这条链做出来是个玩不了的孤岛。第一阶段的教训「加入便携罐就等于关闭流体运输」说的就是这个形状：**新能力必须有端到端目标判据**。

当前 1,517 行 distillery 台账已经分类，但只有 `water → water_distilled` 1 行可投影，1,516 行缺运行时 fluid identity；`crude_oil` 的 composition 为空，`methane` 仍为 metadata-only。这里的来源模型是固定 GT6 行投影，不是 composition 自动生成。

**交付**

- `SubsurfaceFluidDepositBlockEntity` 的守恒抽取契约：simulate / execute、remaining 扣减、存档与并发重验
- 烃类 fluid gate：至少覆盖首条原油链、甲烷链与发电消费端所需 identity
- 原油分馏链：接入已有 distillery 与 1,517 行分类台账，只发布 identity / producer / consumer 已闭合的 source rows
- 第二条发电路线（燃气 / 内燃），与 steam engine → dynamo 并列而非替代
- `state=gas`、`PROPERTIES.FLAMMABLE` / `EXPLOSIVE` / `BURNING` 分层进入运行时；首个纵切不冒充 199 / 18 / 11 全量语义已完成

**先算的数字**

- 从 1,517 条 source rows 中独立计算本阶段可闭合的烃类最小集合；projectable 分母在开工门禁写死，不能用「台账已分类」替代「配方可发布」。
- 48 种 `state=gas` 材料里，只有生产者、容器/管道与消费者都闭合的才进入 live gate；其余保留显式分类。
- T10 收尾 publication 加上本阶段 projected rows 后，重新计算 datapack / publication 双预算和 T5 chemical 独立预算；不得沿用 200 条旧上限而不重算。

**子判据**

- **T11a · 流体身份与投影门禁**：扩展 chemical fluid gate，锁定首批 source rows 与独立期望集。
- **T11b · 矿床抽取**：放置 deposit → 抽取 N mB → remaining 精确减少 N mB，重载后保持。
- **T11c · 物流与分馏**：deposit → extractor → pipe / cell → distillery → source-backed outputs。
- **T11d · 第二发电路线**：至少一种分馏产物经非蒸汽 generator → cable → processing machine 做功。
- **T11e · 危险语义**：先锁 gas-proof / leak 与发电机失败态，再按来源扩大 flammable / explosive 行为。

**范围之外**

- 不做核能 / 聚变（`FUSION_SYNTHESISABLE` 63 个材料整体推迟）
- 不做 UUM 合成（`UUM_SYNTHESISABLE` 664 个）
- 不做载具 / 钻井平台多方块；需要大型结构时先完成 T12 通用层
- 不要求一次发布全部 1,517 条 distillery rows
- 不把 T9 的 CrucibleCraft 矿床分布策略改写成 GT6 worldgen 保真

**完成信号**

1. 「矿床 → 抽取 → 管道 → 分馏 → 燃气发电 → 电网 → 机器做功」整条纵切 GameTest 通过
2. 首批 source rows 与独立期望集双向相等；新增一条具备 identity 闭包的烃类 source row 不需要材料专用 Java
3. 非 gas-proof 管输送 methane 的失败，以及至少一种材料属性驱动的燃烧 / 泄漏行为有运行时测试
4. 未发布的 1,517-row 子集仍有显式原因，不因 `unclassified = 0` 被写成已实现

---

### T12 · 机器分级与多方块通用层 ⚪

**判据**

> 同一工艺的机器 kind 与 tier profile 分离：所有档位共享同一 RecipeMap 和规范配方，档位决定建造材料、输入功率、速度 / 并行、容量与失效策略；RU / KU / EU 身份按固定 GT6 source 保留。至少三种机器覆盖三个档位，同时 Coke Oven 与第二种真实结构由同一 JSON 多方块层验证；新增档位或结构布局只改数据。

**依赖**：T7（材料域选材）、T8（自动化）、第一份规划 9.1–9.4（不可违反）

**固定 GT6 直接证据**

- `gregapi/data/TD.java`：RU（`KINETIC_ROTATION`）与 KU（`KINETIC_PUSH`）是不同 TagData。
- `gregapi/data/MT.java:3689-3691`：`Heat_T[]` / `Kinetic_T[]` / `Electric_T[]` 是机器建造材料档位来源，不是材料 JSON 的 `tier`。
- `gregtech/loaders/b/Loader_MultiTileEntities.java`：同一基础机器类、同一 RecipeMap，以不同材料、accepted energy、input / parallel 等 NBT 注册多个档位。
- `MultiTileEntityAxle` / `MultiTileEntityGearBox`：RU 有独立传输拓扑；不能用 ELECTRIC cable 或 KU 相邻推压冒充。

**开工门禁**

1. 建立 24 台当前 processing host 的逐机器 source ledger：GT6 machine id、RecipeMap、RU / KU / EU / HU、tier material array、input、parallel、来源摘要与行锚点，`unclassified = 0`。
2. 先设计 `MachineKindSpec` / `TierProfile`（名称可调整）和 `RecipeMap → kind`，解除当前 `BY_MAP` 的一 map 一 spec 限制；不得用 tier-specific RecipeMap 绕过。
3. `ProcessingMachineState.VERSION` 的 tier / material / energy identity schema 与旧存档迁移先定，再注册首个多档方块。
4. 多方块 JSON schema、catalog 与 validator 必须先迁移 Coke Oven；「≤3 个继续硬编码」分支已由架构不变量废止。
5. 多方块大槽位投影前评估 `CapacityMatcher` 的小型稠密图假设；没有测量证据时不提前重写，也不得默认可无限扩展。
6. 机器档位新增后的 publication 增量必须为 0；datapack / publication / reload / index 与注册方块数分别落账。

**先算的数字**

- 24 个 machine kind × 各自 source-backed tier 数 = block / item / model / menu / EMI workstation 投影；recipe publication 不乘这个数。
- RU / KU / EU 各有多少机器、需要哪些传输端点与首批网络对象；先按 ledger 计数，不从当前 `KINETIC` 反推。
- Coke Oven 与第二个代表结构（优先固定来源的 Large Centrifuge）的 part、predicate、port、朝向和扫描体积；validator 成本按结构体积与重检周期估算。

**子判据**

- **T12a · 来源与能源台账**：24 / 24 machine host 分类完成，RU / KU / EU 身份与 tier 参数可直接追溯。
- **T12b · Kind / Tier 架构**：拆 spec 与 validator；同一 map 可供多个 tier profile 运行，publication 保持不变。
- **T12c · 三机三档纵切**：至少覆盖 RU、KU、EU 三种机器语义；档位改变输入上限、速度 / 并行和超压行为。
- **T12d · 多方块通用层**：Coke Oven 行为无漂移地迁移 JSON；第二个 source-backed 结构不增加专用 Structure Java。
- **T12e · 传输与性能边界**：RU 首批 shaft / gearbox 或明确分期边界；CapacityMatcher、validator、datapack / publication、reload / index 分别验收。

**范围之外**

- 不做超出已实现电压档的机器
- 不做机器升级件 / 芯片系统
- 不做 GT6 全部多方块，只做清单内的
- 不要求 T12 一次完成 GT6 全部能量类型；但已进入机器 ledger 的 RU / KU / EU 不得继续折叠
- 不在通用层第一版做动态伸缩结构或合并渲染

**完成信号**

1. 三种机器 × 三档位由数据声明生成，共享 RecipeMap / 配方；新增档位前后 publication 增量为 0。
2. RU / KU / EU 机器只接受各自声明的能源语义，超功率 / 超压失败可观测；来源 ledger 全分类。
3. Coke Oven 与第二个多方块由同一 JSON schema / validator 解释，结构布局不新增 Java 类。
4. 存档迁移、注册数量、双轴配方账目、reload / index 与结构匹配性能全部接进测试断言。

---

## 5. 当前架构快照（2026-08-05 重基线）

> 与第一阶段文档第 3 节同源。数字对不上就是有东西漂移了；闭包值不能替代保真或载荷状态。

| 项 | 数值 | 第二阶段目标方向 |
|---|---|---|
| 材料定义 | 1,773 | 不变 |
| 注册形态 | 16,048 | T10 精确增加 967 |
| 前缀（已落地） | 56（T10 新增 3，注册 323 / 323 / 321） | T11 保持 |
| `ITEMGENERATOR` 域覆盖 | **16 / 25** | T10 已关闭四个域 |
| `material_tags` 运行时可读 | **词表 101 / 101 且未知标签 fail-fast；已发布规则消费 8 种；T7 台账 62 / 62** | 后续按台账阶段消费 |
| `EnergyType` / handler | **4 / 4 有引用；`IEnergyHandler` 已参数化** | 接口闭包已完成；RU / KU 当前都折叠为 KINETIC |
| 能源传输拓扑 | ELECTRIC cable 完整；KINETIC 仅相邻传输 | T12 前恢复 RU / KU 身份，shaft / gearbox 分期 |
| 矿脉 feature | **134**（原有 5 + catalog 129） | T9 已覆盖 129 / 129 条分类 |
| 流体矿床 | **2**（crude_oil / methane） | 抽取设备留给 T11 |
| 已注册 live 非熔融流体 | 108 / 119 候选 | T10 cell 域补齐 93 |
| molten fluid | 204 | 不变 |
| 物品 / 流体管道 | **72 / 210 blocks** | T8 已关闭 |
| cover 类型 | **3** | filter / valve / output pump |
| 多方块 | **1**（Coke Oven，结构硬编码） | 第二个结构前先建 JSON catalog / validator |
| 机器 | 24（全单档） | T12 后分级 |
| configured machine | 23 | |
| RecipeMap → machine spec | **1 map : 1 spec** | T12 拆 kind / tier，档位共享 map |
| datapack recipe entries | **5,958 / 6,600** | 独立 CI 预算已关闭 |
| recipe publication | **18,871 / 21,000** | T10 known-form +1,288 |
| 规则压缩比 | **3.167 : 1**（18,871 / 5,958） | CI 下限 3.0 |
| T9 三轴 | closure 129 / 129；fidelity `PLACEHOLDER`（数据值 `UNIFORM_PLACEHOLDER`）；load 通过 | O-29 只关闭几何保真，不重开 T9 闭包 |
| T10 关闭账目 | hot 642 + multi 646；known-form 1,288 / 1,500；cell 61 / 48 / 14 / 93 | `T10_READY` |
| GT6 大类进度（见第 2 节） | G1/G2/G5 主闭包●；G7 闭包●保真○；G3/G4/G6/G8/G9 未齐 | 第二阶段主线：G9→G8→G3/G4/G6 |
| GameTest | 47 / 47 | 每阶段递增 |
| Java / Python 单测 | 433 / 252 | |

---

## 6. 悬案登记表（继承 + 新增）

继承第一阶段未关闭项，并继续按代码与固定来源审计追加。关闭闭包轴不会自动关闭同编号下的保真或载荷债。

| 编号 | 内容 | 类型 | 排期 |
|---|---|---|---|
| **O-11** | 生成包 identity 哈希 `GeneratedMaterialPack.class`，注释/行号变化触发冷写 | 开发体验 / 性能 | 先测量 |
| **O-14** | `HeatMaintenanceEvents.itemEntityTick` 全局 `EntityTickEvent.Post` | 性能观察 | 剖析命中后再改 |
| **O-15** | 当前 generated lang 中 `zh_cn` 真实翻译 293 / `en_us` 3,010 键，覆盖率 9.73% | 本地化 | 未排期，单独立项 |
| **O-20** | progress / duration 同步降为 20 tick，短配方可见步进 8%–31% | 表现层 | 下一轮机器 UI 工作时处理 |
| **O-22** | `CableNetworkTraversal.applySegmentLoss` 的零损耗域：`lossPerMeter == 0` 原样返回正/负 packet，负损耗仍拒绝；metadata codec 同步允许 0。当前 source-backed 导体仍全部 `loss >= 1` | 正确性 / 数据域 | ✅ **T7 已关闭** |
| **O-23** | `CableTransferPlan.execute` 现按 terminal 实际提交量返回，并按每段覆盖的实际 terminal 区间记 `CableLoad`；T8 item/fluid pipe 同样只统计实际 delivered/transferred，端点降级不再制造计划量遥测 | 表现层 / 账目 | ✅ **T8 已关闭** |
| **O-24** | 材料前缀的 `model_texture` 是资源作者维护的输入，允许 `minecraft:` 或自定义命名空间；L3 生成器只回写结构性 `generation_flag`，不得把自定义纹理恢复成 vanilla 占位。16,048 个注册形态的完整视觉表现仍是独立大工程 | 表现层 / 美术 | 未排期，单独立项 |
| **O-25** | 1,773 份材料级 `heat_damage` / `explosion_damage` 均为 0；固定 GT6 revision 的真实热锭伤害为前缀事实 `OP.ingotHot.mHeatDamage = 3.0`，normalized prefix dump 锁定唯一非零项 | T10 开工门禁 | ✅ **T10a 已关闭**：前缀事实已导入 fingerprint/handshake 并由运行时接触伤害消费，未伪造材料级正值 |
| **O-26** | extruder 2,782 条 sparse relation 仍是逐材料文件；紧凑方案至少需 27 个 IO 变体，按 EU/t 标签拆分约 54 条，并保留 `formula_verified=false` 的逐材料 duration lookup、输入 fallback、forging target、`plateGem` 与 shadow 顺序语义 | 规则语言 / 数据规模 | 排在 T9 关闭后；必须以 2,782 条 IO / duration / EU/t 双向全量等价测试证明后再压缩，禁止猜 duration 或仅按数量替换 |
| **O-27** | carbon / plastic / rubber / wood / wood_treated × 5 档流体管共 25 个形态已注册且在 CABLES 页可见，但固定 GT6 来源的 `pipe_properties.recipe` 五档均为 false，全库没有生存获取路线 | T8 获取闭包 / T10 容器 | 保留 source-backed 形态；GT6 非金属管真实合成路线待定位，预期 T10 容器阶段一并处理 |
| **O-28** | `material.pipe.fluid_recipe` / `item_recipe` 当前按任意 gauge 的 `recipe=true` 返回 1；现有数据恰为 37/5 与 24/0 的全 true / 全 false 分组，逐档近似暂时等价，但不能表达未来混合 gauge | 规则语言 / T8 后续 | T9 关闭后改为带 specification 参数的谓词（如 `material.pipe.fluid_recipe(\"pipeHuge\")`），并与 `PipeCatalog` 的逐档 key 共用映射 |
| **O-29** | T9 的 129 条 catalog vein 共用 `hr=5 / vr=2 / density=0.22 / y=-48..48` 和四层单矿布局；固定 GT6 dump 虽含 worldgen，但尚无规范化器、材料映射或参数语义换算 | T9 数据来源 / 平衡 | 保留已关闭的分类、注册与放置管线；建立独立 GT6 worldgen 导入链，按来源等价类覆盖并对全部已映射 vein 做逐字段双向等价；统一模板保持 `UNIFORM_PLACEHOLDER`（归类为 `PLACEHOLDER`） |
| **O-30** | GT6 直接区分 RU / KU，当前 CC 将两者折叠为 `EnergyType.KINETIC`；接口已多类型化，但机器 spec、诊断与传输拓扑没有保留物理身份 | 架构 / 保真 | T12a 前完成 24 / 24 机器能源台账；RU / KU 身份进入 spec，shaft / gearbox 可分期但不得继续折叠 |
| **O-31** | 当前只有 `CokeOvenStructure` / `Layout` 一次性 Java；第二个多方块会复制结构类、端口与校验逻辑 | 架构 | T12d：先迁移 Coke Oven 到 JSON catalog / validator，再落第二个 source-backed 结构 |
| **O-32** | datapack entries、publication、压缩比与 reload / index 五项载荷账目 | 载荷 / 性能 | ✅ **T10e 已关闭**：5,958 / 6,600、18,871 / 21,000、3.167 ≥ 3.0，runtime 继续执行 10 s / 1 s 门禁 |
| **O-33** | 机器分级已有 `MT.DATA.*_T[]` 与代表机器注册锚点，但全部机器、RecipeMap、prefix 与 multiblock 的固定 revision 清点仍未形成统一台账 | 来源台账 | T12a / 第三阶段门禁；禁止用当前 CC 数量或 Wiki 近似代替直接清点 |
| **O-34** | T10 独立阶段预算与 `ingotHot.mHeatDamage` Java runtime consumer | T10 架构阻断 | ✅ **T10a 已关闭**：1,500 budget、未知阶段 fail-closed、publication metric 与 consumer 均有定向测试 |
| **O-35** | T11 原判据写 composition 自动生成，但当前实现是固定 GT6 row projection；1,517 行中仅 1 行可投影，`crude_oil` composition 为空 | 判据 / 来源 | T11a 以 fluid gate + source-row ledger 重建可投影分母，不把全分类写成全实现 |
| **O-36** | hot ingot 以摄氏熔点为初温、20°C 环境温度和 1°C/tick 冷却；当前为明确的 CC 平衡曲线，不宣称 GT6 等价 | T10 保真 / 平衡 | `hot_ingot_to_ingot.json.balance_policy` 标记 `DESIGN_POLICY + UNVERIFIED`；定位并规范化固定 revision 的 GT6 冷却来源后方可替换 |

---

## 7. 全阶段范围之外（显式推迟，附理由）

写下来是为了防止中途「顺手做一下」。每条都要有理由，不能只写「以后再说」。

| 内容 | 规模 | 推迟理由 |
|---|---:|---|
| `ITEMGENERATOR.PLANTS` | 1,179 材料 | 农业 / 食物是另一条独立玩法轴，与工业链耦合弱；单独立项 |
| `ITEMGENERATOR.ARMORS` | 613 材料 | 依赖 T4 工具的组件模式，但护甲属性面更宽；等 T12 后再评估 |
| `ITEMGENERATOR.PROJECTILES` | 854 材料 | 战斗轴，与本阶段判据无交集 |
| `ITEMGENERATOR.LENSES` / `RAILS` | 121 / 28 | 依赖尚未实现的机器（激光 / 运输） |
| `PROCESSING.UUM_SYNTHESISABLE` | 664 材料 | 末期内容，需要完整电压体系做前置 |
| `PROCESSING.FUSION_SYNTHESISABLE` | 63 材料 | 同上 |
| 等离子体（`plasma_point`） | 1,773 有字段 | 需要聚变前置 |
| 核能 | — | 需要完整化学与独立冷却契约；T7 已删除无消费点的 `COOLING` enum，不代表冷却玩法取消 |
| 新维度 | — | 与移植判据无关 |
| GT6 逐矿脉几何对齐（O-29） | 129 条 catalog / 176 条本地 GT6 定义 | 两侧数量、材料名和 size/density/y 语义不一；需独立规范化、全量映射与双向等价项目，不能在 T9 关闭时猜值 |
| 完整中文化（O-15） | `en_us` 3,010 键，`zh_cn` 293 个真实翻译 | 数据工作，单独立项，不占主线 |
| 材料纹理（O-24） | 15,081 形态 | 美术工作，单独立项 |

---

## 8. 第二阶段特有方法论增补

第一阶段第 5.1–5.6 节与第 9 节架构不变量全部继续生效。下面四条是第二阶段特有补充。

### 8.1　「已导入的事实」不等于「已实现的功能」

T6 之前，`electrical_by_specification` 在 1,773 个材料里躺了好几个阶段，看起来像「电力已经做了一部分」。实际上运行时一个字节都没读。

**新增检查：每个阶段收尾时，统计本阶段引入的数据字段有几个有运行时消费点。** 零消费的字段要么当场接上，要么进悬案并注明预期消费阶段。不允许「先导入着，以后总会用上」。

第 3.1 节那张表就是这条规则第一次被系统性执行的结果。

### 8.2　「响亮失败」在跨所有权边界要降级为「限频报告 + 有界损失」

T6 修复过程中确立的结论，写下来防止回退：

- **自有端点**（CrucibleCraft 自己实现的 handler）：保持 simulate / execute 严格守恒，违约响亮失败
- **外部端点**（第三方 mod 的能力实现）：违约时限频报告 + 耗散差额，**不得把异常抛出 block-entity tick**
- 失败方向必须是「能量丢失」而非「能量复制」——source-first 提交顺序不可颠倒

T8 的管道、T11 的燃气设备都跨这条边界，必须沿用同一策略，不要每种网络重新发明一次。

### 8.3　形态爆炸的算术必须在写第一行 Java 之前完成

T6 的正面例子：先算出 `1,773 × 5 = 567,360` states 不可行，才收敛到 `144 blocks / 9,216 states`。

T8 的 pipe、T10 的 containers、T12 的机器分级都有同样的爆炸风险。**开工检查表里那条「先把数字算出来写在纸上」不是形式**，它在 T6 救了一次。

补充一条口径：算的时候要同时算**份数 × 每份求值成本**（O-4 的教训），不能只算总量。

### 8.4　验证分层不改变阶段关闭口径

日常使用 `python tools/run_python_tests.py --suite fast`；按改动选测使用 `affected`，任何未归属路径自动升级为 `closure`。这两层只缩短反馈时间，不是关闭证据。第二阶段收尾与 CI 统一执行 `python tools/run_full_verification.py --check`；需要刷新 `READY` 证据时由维护者执行 `--record`。

`closure` 必须与 unittest discovery 数量完全相等且每项恰好一次；材料/前缀/配方缓存只限当前进程、可清除、不可被调用方污染。raw/cache GT6 重放保留在显式 `source-replay`，缺失来源写 `SKIP`，禁止借优化删除测试、沿用旧 ledger 或绕过 source gate。

---

## 9. 开工与收尾入口（仅第二阶段增补）

完整开工/收尾清单只维护在《[CrucibleCraft-总体规划.md](CrucibleCraft-总体规划.md)》第 7 / 8 节，架构不变量只维护在其第 9 节；本文不复制。第二阶段额外执行以下检查：

**开工增补**

- [ ] 第 1 节中由本阶段拥有、且已到最晚决策点的支柱门禁已经关闭。
- [ ] 形态与运行时成本按第 8.3 节同时计算「份数 × 每份求值成本」，并写入阶段卡。
- [ ] 跨外部所有权边界时按第 8.2 节声明失败策略，并有守恒/耗散测试。
- [ ] 新的 `tN/` MaterialRule 已有独立阶段预算，datapack / publication / reload / index 投影分别落账。
- [ ] 变更路径已有 affected owner；无法证明依赖时按第 8.4 节预期升级 closure。

**收尾增补**

- [ ] 本阶段新增的数据字段都有运行时消费点，或按第 8.1 节进入悬案并写明 owner。
- [ ] 架构跑道、G1–G10 状态、三轴快照和悬案已同步更新，不把单轴关闭写成完整移植。
- [ ] 统一 closure 编排已通过且 `READY` 只绑定一次；fast/affected 没有被写成关闭证据，source replay 的 PASS/SKIP 明确记录。

---

## 附：第二阶段下一件事

T7–T9 已按闭包判据关闭，T10a 的两个代码级开工门禁也已关闭。下一件事是 **T10b · MULTIINGOTS**：按 323 个材料 × double/triple 两前缀完成 646 个形态与路线的双向等价，不改 Java。

随后依次进入 T10c 的 INGOTS_HOT 与 T10d 容器 ADR。T10a 的 1,500 预算只覆盖已知 1,288 条 hot/multi 路线，不构成容器路线许可。
