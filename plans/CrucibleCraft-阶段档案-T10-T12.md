# CrucibleCraft 阶段档案 · T10–T12

> 状态：历史档案；T10–T12 均已关闭。
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 归档基线：2026-08-06；最终状态 `T12_READY`
> 当前执行入口：《[CrucibleCraft-第三阶段总体规划.md](CrucibleCraft-第三阶段总体规划.md)》

本文保存第二阶段后三张完整判据卡、关闭边界和阶段特有方法论。它不再承担当前路线导航；历史数字只描述当时关闭状态。

---

## T10 · 容器与形态补全 ✅

**判据**

> `MULTIINGOTS`、`INGOTS_HOT` 与三个 `CONTAINERS*` 域全部分类；double / triple ingot 与 hot ingot 走材料前缀管线，经 ADR 选定的 fluid / gas cell 能装载 gate 中的对应流体。smelter 产出的 hot ingot 具有来源正确的接触伤害，并经可执行冷却路线变为常温 ingot；新增材料只改数据。

**依赖**：T7 材料事实、T8 物流边界。

**交付**

- `double_ingot` / `triple_ingot`：独立材料前缀，单位为 288 / 432。
- `ingot_hot`：独立形态，与 `HEAT` 温度组件正交；固定来源的 `OP.ingotHot.mHeatDamage = 3.0` 进入运行时。
- 两种 1,000 mB 通用 cell：空容器可堆 64，装液后为 1；fluid / gas allowlist 与 NeoForge 标准事务闭合。
- T10 独立 MaterialRule 预算、创造栏、handshake / fingerprint、datapack / publication / reload / index 门禁。

**开工门禁关闭记录**

1. 固定 revision、`OP.java` / `UT.java` blob、规范化前缀摘要和提取键进入 policy。
2. T10 独立 authored rule 预算为 1,500，未知未来阶段 fail-closed。
3. 前缀事实 codec、catalog、fingerprint 与 runtime consumer 完成。
4. `tools/t10_preflight_projection.json` 为 `T10_READY`。
5. 三枚新前缀进入 creative、legacy alias、registration gate 与 handshake。

**子判据**

- ✅ **T10a · 开工门禁与前缀事实层**：预算、前缀事实和工具链关闭。
- ✅ **T10b · MULTIINGOTS**：323 + 323 个形态及 646 条路线全量双向相等。
- ✅ **T10c · INGOTS_HOT**：321 个形态、642 条冷热路线、接触伤害与完整冷却纵切关闭。
- ✅ **T10d · 容器 ADR 与纵切**：61 fluid / 48 gas allowlist、14 denied 与 93 个补充流体关闭。
- ✅ **T10e · 集成与账目**：便携罐、cell、pipe 分工及五项 load gate 关闭。

**范围之外**

- 不做 `LENSES` / `RAILS` / `PROJECTILES` / `ARMORS`。
- 不重写 T5 多罐事务。
- 不在 T10 实现环境气体扩散、燃烧或爆炸。
- 热锭 20°C 环境、1°C/tick 冷却曲线是 `DESIGN_POLICY + UNVERIFIED`，由 O-36 继续追踪。

**关闭快照**

- gate：multi / hot 为 323 / 323 / 321，新增注册形态 967。
- route：hot 642 + multi 646 = 1,288 / 1,500。
- load：5,958 datapack entries、18,871 publication、3.167:1。
- prefix / handshake：56 / 1,829。

---

## T11 · 石油、天然气与第二发电路线 ✅

**判据**

> 从 T9 流体矿床抽取原油 / 天然气；矿床身份为 GT6 `natural_gas`，不能用 methane 冒充。天然气经固定行 553 转换为 methane，原油进入 distillery；产物经 T8 管道或 T10 cell 运输，至少一种燃料驱动非蒸汽发电设备并向电网投入。新增一条已具来源映射且 identity / producer / consumer 闭合的烃类 source row 不需要材料专用 Java。

**依赖**：T8 管道、T9 流体矿床、T10 气体容器。

**固定来源与身份边界**

- 1,517 条 distillery row 全量分类；选择固定行 872。
- 10,236 条 generifier row 全量分类；选择固定行 553：`natural_gas → methane`。
- 21 条 engine fuel 与 9 条 gas fuel 全量分类；选择 14 / 2。
- `liquid_medium_oil` 的 Raw Oil 显示语义不能单独证明 material 9852 `CrudeOil` 等价；T9 `crude_oil` bridge 保持 `DESIGN_POLICY + UNVERIFIED`，由 O-37 跟踪。

**交付**

- 矿床抽取采用 simulate / execute、并发重验和一次提交；初版产能不衰减、不耗尽。
- 旧 methane 矿床兼容迁移为 `natural_gas`，历史 reserve 仅作迁移 / 诊断。
- 原油链：deposit → extractor → pipe → distillery 872。
- 天然气链：deposit → extractor / gas cell → generifier 553 → methane。
- Fuel Engine 14 与 Burning Gas Generator 2 使用共享 generator runtime 向 T6 电网投入。
- gas cloud 按 fluid identity 与材料属性驱动上浮、有限扩散、泄漏、点燃和持续燃烧。

**子判据**

- ✅ **T11a · 流体身份与投影门禁**：872 / 14 / 2 选定，T11 authored MaterialRule 预算为 0。
- ✅ **T11b · 天然气身份、喷井与抽取**：25 mB/20t 原油、5 mB/20t 天然气、1,000 mB 有界排放作为 CC 策略落地。
- ✅ **T11c · 物流与油气加工**：两条加工纵切与真实 lubricant 输出关闭。
- ✅ **T11d · 第二发电路线**：两台发电设备、排气容量与原子事务关闭。
- ✅ **T11e · 危险语义**：非 gas-proof 管泄漏与属性驱动燃烧关闭。

**范围之外**

- 不做核能、聚变、UUM、钻井平台多方块。
- 不一次发布全部 1,517 条 distillery row。
- 不把 T9 矿床分布改写成 GT6 worldgen 保真。
- 不实现 GTM 式递减 / 耗尽；未来只能作为默认关闭、独立存档的配置策略。

**关闭快照**

- source rows：1,517 / 10,236 / 21 / 9 全量分类，selected 872 / 553 / 14 / 2。
- load：5,966 datapack entries、18,875 publication、3.164:1。
- `T11_READY`；两条完整 GameTest 和危险语义测试通过。

---

## T12 · 机器分级与多方块通用层 ✅ scoped-closure

**判据**

> 同一工艺的 machine kind 与 tier profile 分离：所有档位共享 RecipeMap 和规范配方，kind 决定行为与缩放策略，profile 的 `tierBandId` 只标识共享的材料、能量、输入窗口、容量与效率带；parallel 上限仍是每个 variant 的 profile 数值，完整机器身份由 variant id 给出。RU / KU / EU 身份按固定 GT6 source 保留。至少三种机器覆盖三个档位，同时 Coke Oven 与第二种真实结构由同一 JSON 多方块层验证；新增档位或结构布局只改数据。

**固定 GT6 直接证据**

- `gregapi/data/TD.java`：RU 与 KU 是不同 `TagData`。
- `gregapi/data/MT.java:3689-3691`：`Heat_T[]` / `Kinetic_T[]` / `Electric_T[]` 是机器建造材料来源，不是材料 JSON 的 `tier`。
- `Loader_MultiTileEntities.java:1288-1401`：同一机器类和 RecipeMap 使用不同材料、accepted energy、`NBT_INPUT`、parallel 与 overclock NBT 注册档位。
- `MultiTileEntityBasicMachine.java:126-131,489-518,712-815`：输入窗口、standard / cheap overclock、parallel / parallelDuration 与失败语义。
- `MultiTileEntityAxle` / `MultiTileEntityGearBox`：RU 独立传输拓扑。

**对象边界**

- 25 个 processing kind：19 `source_tiered`、4 `fixed_utility`、2 `deferred_with_reason`。
- 12 个非 spec kind：5 `tier_profile_participant`、3 `material_capability`、3 `fixed_infrastructure`、1 `deferred`。
- Anvil / Crucible 保留材料能力轴，不因同名 `tier` 被强行并入机器功率档。

**开工门禁关闭记录**

1. 25 / 25 processing 与 12 / 12 non-spec ledger 均 `unclassified = 0`。
2. input window、standard / cheap、parallel / parallelDuration、efficiency 与四类失败态形成独立期望矩阵。
3. `MachineKindSpec` / `TierProfile` / `MachineVariant` 分离；tier band 不能注入行为，`parallelDuration` 仍属于 kind，parallel limit 仍按 variant profile 保存。
4. `ProcessingMachineState.VERSION = 3`；只读写 `tier_band`。输入出现已移除的 `tier_profile` 即持久化 quarantine 并 fail closed；blank/current identity 仍可接受，future version 仍保持 unsupported。
5. Coke Oven 无行为漂移迁入 JSON；Large Centrifuge 复用同一 catalog / validator。
6. `CapacityMatcher` 在 12 / 16 / 32 / 64 supply 基准内有界通过。
7. T12 authored MaterialRule 预算为 0；增加 9 个 tier variant 的 publication delta 为 0。

**子判据**

- ✅ **T12a · 来源、缩放与边界台账**：25 + 12 ledger、9 行矩阵与七份固定源关闭。
- ✅ **T12b · Kind / Tier 架构与迁移**：一 map 多 kind、v3 `tier_band` 存档与 v2 兼容 quarantine 机制关闭。
- ✅ **T12c · 三机三档纵切**：Centrifuge(RU)、Sifter(KU)、Electrolyzer(EU) 各三档关闭。
- ✅ **T12d · 多方块通用层**：Coke Oven + Large Centrifuge 共用 JSON schema / validator。
- ✅ **T12e · 传输与性能边界**：EU motor→axle→gearbox→RU、KU adjacent 与 EU cable 三域纵切关闭。

**范围之外**

- 不一次完成 GT6 全机器、全部能量类型或全部多方块。
- 不把 12 个非 spec kind 强迁入 processing runtime。
- 不做机器升级件、芯片、动态伸缩结构或合并渲染。

**关闭快照**

- machine：25 + 12 ledger；3 kind × 3 tier = 9 variants。
- multiblock：2 个 JSON structure；Large Centrifuge 扫描体积 18。
- load：5,985 / 6,600 datapack entries；18,875 / 21,000 publication；3.154 ≥ 3.0。
- tests：53 / 53 GameTest；449 Java / 272 Python。
- 状态：`tools/t12_closure_readiness.json = T12_READY`。

**scoped-closure 说明**

T12 关闭的是通用架构和首批纵切，不是全量保真：

- Large Centrifuge 已使用 `CHEAP` kind policy，但当前 variant 借用 titanium centrifuge tier；固定来源给出的独立 4,096 input / 5,000 efficiency profile 尚未落成独立 tier。
- `tools/t12_machine_readiness.json` 是 T12a preprojection artifact，仍写 `full_t12_closure_claimed=false`、9 craft fail-closed；live 注册、六种 casing producer、九种 machine recipe 与 `t12_closure_readiness.json` 已越过该状态。第三阶段必须消除这份历史 / 当前混写，而不是重新实现已经存在的 casing。
- processing identity mismatch 已覆盖 saved kind / tier band / material / energy、空 identity、旧字段、新字段、双字段同值与双字段冲突；冲突 quarantine 在重存与重载后保持。
- T12 交接时曾写成 16 个 item/fluid port 与 6 个 runtime supply；T15e 审计确认前者漏减 controller 位，后者没有 source parser。当前 live authority 为 15 个 item/fluid port、2 个 energy port、1 个 controller；全部物理 port 桥接同一 host，matcher 从 host 只取得 item/fluid 各 1 份 input supply，presence cap 12 不触发。旧 expected-16 与旧“六份”陈述仅作为 superseded history 保留。

---

## O-22–O-38 阶段边界快照

| 编号 | 第二阶段结论 | 交接 |
|---|---|---|
| **O-22** | 零损耗 cable packet 与 metadata codec 已关闭 | 历史关闭 |
| **O-23** | cable / pipe 遥测按实际提交量计账，端点降级不再制造计划量 | 历史关闭 |
| **O-24** | 16,048 注册形态的完整视觉表现仍是独立美术工程 | 第三阶段 5.8 禁止新增债，历史债保留 |
| **O-25** | `ingotHot.mHeatDamage = 3.0` 前缀事实已进入 runtime | T10a 关闭 |
| **O-26** | extruder 2,782 sparse relation 需全字段双向等价后压缩 | T14 |
| **O-27** | 25 个非金属流体管缺 source-backed 生存路线 | T19 |
| **O-28** | pipe recipe predicate 需按 specification / gauge 查询 | T19 |
| **O-29** | 129 条 catalog vein 仍为统一 placeholder 几何 | T19 后独立 T 候选 |
| **O-30** | RU/KU identity 与首批 topology | T12 首批关闭 |
| **O-31** | JSON 多方块通用层；T15e 修正旧 expected-16 的 controller 漏减并锁定 physical-port/shared-host 边界 | T12d 架构关闭；T15e 审计关闭 |
| **O-32** | datapack / logical/eager/lazy publication、server/client reload/index、memory/lookup 双轴账目 | ✅ T14 关闭：measured soft/hard policy、family projection 与 CI hard-fail 已落地 |
| **O-33** | GT6 全量 canonical denominator | ✅ T13 已以七类 canonical denominator 和 `T13_READY` 关闭 |
| **O-34** | T10 独立 budget 与 hot-ingot runtime consumer | T10a 关闭 |
| **O-35** | T11 composition 判据改为固定 row projection | T11a 关闭 |
| **O-36** | hot-ingot 冷却曲线仍为 `DESIGN_POLICY + UNVERIFIED` | 独立 fidelity 债 |
| **O-37** | Raw Oil / material 9852 `CrudeOil` 身份未证 | T18 |
| **O-38** | `natural_gas` identity、旧 methane 迁移与 generifier 553 | T11 关闭 |

---

## 第二阶段特有方法论遗产

### 已导入事实不等于已实现功能

每阶段收尾统计新增数据字段的运行时消费点。零消费字段要么接入，要么登记 owner；不允许用“数据已经在 JSON 里”代替功能完成。

### 跨所有权边界使用限频报告与有界损失

- CC 自有端点保持 simulate / execute 严格守恒，违约响亮失败。
- 外部端点违约时限频报告并耗散差额，不把异常抛出 block-entity tick。
- 失败方向必须是损失而不是复制；source-first 提交顺序不可颠倒。

### 形态爆炸先算数量和求值成本

开工前同时计算“份数 × 每份求值成本”，不能只算注册数量；T6 cable、T8 pipe、T10 container 与 T12 tier 都沿用此规则。

### 快速验证不能替代关闭证据

`fast` / `affected` 只缩短反馈；阶段关闭仍以 `python tools/run_full_verification.py --record` 的完整 closure 为证据。raw/cache 不可用只能显式 `SKIP`，不得冒充 PASS。
