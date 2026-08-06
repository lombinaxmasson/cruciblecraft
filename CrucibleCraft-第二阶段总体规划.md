# CrucibleCraft 第二阶段总体规划

> GregTech 6 → Minecraft 1.21.1 NeoForge 移植 · 第二阶段 T7–T12
> 状态：✅ 2026-08-06 完整关闭；本文为关闭摘要与第三阶段交接导航
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 当前执行入口：《[CrucibleCraft-第三阶段总体规划.md](CrucibleCraft-第三阶段总体规划.md)》

---

## 0. 文档职责

第二阶段已经结束。本文只保留四件事：

1. T7–T12 的三轴关闭摘要；
2. 架构支柱的交付状态；
3. G1–G10 在阶段边界上的刻度；
4. 交给第三阶段的悬案和事实边界。

完整判据卡已经归档：

- T7–T9：《[CrucibleCraft-阶段档案-T7-T9.md](CrucibleCraft-阶段档案-T7-T9.md)》
- T10–T12：《[CrucibleCraft-阶段档案-T10-T12.md](CrucibleCraft-阶段档案-T10-T12.md)》

方法论、开工 / 收尾检查表和架构不变量继续以《[CrucibleCraft-总体规划.md](CrucibleCraft-总体规划.md)》为唯一权威。**任何时刻最多一个 T 阶段进行中。**

---

## 1. 第二阶段关闭摘要

| 阶段 | 名称 | 闭包 | 保真 | 载荷 | 关闭要点 |
|---|---|:-:|:-:|:-:|---|
| **T7** | 材料事实进入规则语言 | ● | ◐ | ● | 62 / 62 标签、101-tag fail-fast、220 / 256 authored mortar |
| **T8** | 物流骨架 | ● | ◐ | ● | 282 pipe blocks、3 covers、257 / 320 recipes；O-27/O-28 交接 |
| **T9** | 世界生成数据化 | ● | ○ | ● | 129 / 129 vein、137 ore materials、2 fluid deposits；几何 O-29 |
| **T10** | 容器与形态补全 | ● | ◐ | ● | 967 形态、1,288 routes、双通用 cell；冷却 O-36 |
| **T11** | 石油天然气与第二发电路线 | ● | ◐ | ● | rows 872 / 553 / 14 / 2、非耗尽抽取、两条发电纵切；身份 O-37 |
| **T12** | 机器分级与多方块通用层 | ● | ◐ | ● | 25+12 ledger、3×3 tier、RU/KU/EU、2 JSON structures |

图例：● 本轴按阶段口径关闭；◐ 有明确保真残留；○ 本轴未关闭。

### T12 scoped-closure

`tools/t12_closure_readiness.json` 固定最终证据：

- `status = T12_READY`
- 5,985 / 6,600 datapack entries
- 18,875 / 21,000 publication
- compression 3.154 ≥ 3.0
- 53 / 53 GameTest
- 25 / 25 processing kind：19 source-tiered / 4 fixed utility / 2 deferred
- 12 / 12 non-spec kind：5 tier participant / 3 material capability / 3 infrastructure / 1 deferred
- 9 machine variants，新增 tier 的 publication delta = 0
- Coke Oven 与 Large Centrifuge 共用 JSON catalog / validator

这只证明架构通用层和首批纵切成立，不代表 GT6 全机器、全 RecipeMap、全 cover 或全多方块已经移植。

---

## 2. 架构支柱交付

| 支柱 | 第二阶段交付 | 第三阶段消费方式 |
|---|---|---|
| **MaterialRule / provenance** | authored rule、来源事实与独立期望集分离；未知阶段 fail-closed | T13 建 canonical 分母，T14 按配方族证明压缩 |
| **事务 / epoch / 双预算** | 多罐、并行输入输出、能量提交、recipe 原子发布与握手共用 epoch | T14 重测批量投影下的 server/client 物化边界 |
| **RU / KU / EU** | `KINETIC_ROTATION` / `KINETIC_PUSH` 分离；RU axle/gearbox、KU adjacent、EU cable 三纵切 | T16/T17 按 source-tiered ledger 批量接档 |
| **MachineKind / TierProfile** | kind 行为、tier 数值、variant identity 与 v2 存档分离 | T15 修残余，T16/T17 扩机器；fixed/deferred 不强制分档 |
| **多方块 JSON / validator** | Coke Oven 与 Large Centrifuge 共用数据结构层 | 后续结构继续零专用 `*Structure` Java；controller 行为可用插件 |

### 交接时发现的 T12 残余

1. **Large Centrifuge profile**：运行时已选择 `CHEAP` policy，但借用 titanium centrifuge tier；固定来源中的独立 input / efficiency 数值尚未成为专用 profile。
2. **readiness 历史 / 当前混写**：`tools/t12_machine_readiness.json` 是 T12a preprojection artifact，仍声称 9 craft 因 casing fail-closed；当前 live 注册、六种 casing recipe、九种 variant recipe 与 `T12_READY` 已证明该状态过时。
3. **identity quarantine 回归**：saved kind / tier / material / energy mismatch 已有运行时隔离路径，但缺直接持久化回归测试。
4. **port / matcher 边界**：T12 交接曾误记 expected-16（漏减 controller）并写入无 parser 来源的“六份 runtime supply”。T15e 已从 live JSON / spec 修正为 15 个 item/fluid port、2 个 energy port、1 个 controller；物理 port 共用一个 host，matcher 的 item/fluid input supply 各 1 份，cap 12 不触发。

因此第三阶段不得重新实现已经存在的 casing；应先消除台账漂移并补足 Large Centrifuge 保真。

---

## 3. G1–G10 阶段边界刻度

> 这些是玩家可感知系统的工作量坐标，不是第二套里程碑。文件数和 `18,875 / 720,841` 都只能作规模信号，不能直接写成完成百分比。

| ID | 大类 | 第二阶段边界 | 第三阶段入口 |
|---|---|---|---|
| **G1** | 材料 / 前缀 / OreDict | 1,773 live + 1 supplemental；56 prefixes；ITEMGENERATOR 16 / 25 | T13 重建全 prefix/domain canonical 分母 |
| **G2** | 配方语言 / RecipeMap | 95 source maps；18,875 live publication，口径不可直接相除 | T13 map ledger，T14 物化/压缩证明 |
| **G3** | 单块加工机器 | 25 local kind ledger；3 kind × 3 tier 纵切 | T13 GT6 全 kind；T16/T17 批量接档 |
| **G4** | 能源物理 / 传输 | EU cable、RU axle/gearbox、KU adjacent | T16–T18 扩工业能源阶梯 |
| **G5** | 物流 / Cover | 282 pipe blocks、3 cover behavior | T13 canonical cover kind；T19 高价值批次 |
| **G6** | 多方块 | 2 个 JSON structures，通用 validator 已闭合 | T13 只清分母；实现批次后排 |
| **G7** | 世界生成 | 134 vein features、2 deposits；闭包绿、几何保真红 | O-29 后续独立 T 阶段 |
| **G8** | 流体 / 化学 / 燃料 | 油气加工与双发电纵切；Raw Oil bridge 未证 | T18 能源转换与 O-37 |
| **G9** | 容器 / 热态 / 工具形态 | hot/multi/cell 闭包；冷却曲线未证 | O-36 独立保真债 |
| **G10** | 外围轴 | 农业、护甲、弹药、UUM 等明确推迟 | 第四阶段候选 |

---

## 4. 交给第三阶段的悬案

| 编号 | 内容 | 第三阶段 owner |
|---|---|---|
| **O-20** | 20-tick progress update 导致短配方可见步进 | T19 UI 顺带处理 |
| **O-26** | extruder 2,782 sparse rows 的可证明压缩 | ✅ T14 已关闭；20 authored ↔ 2,782 logical，仍不外推全局比例 |
| **O-27** | 25 个非金属管形态的 source-backed 生存路线 | T19 |
| **O-28** | pipe predicate 必须按 specification / gauge 查询 | T19 |
| **O-29** | 129 条 catalog vein 的 GT6 几何导入与映射 | 第三阶段后续候选，不与活跃 T 并行 |
| **O-33** | GT6 machine / map / prefix / domain / cover / multiblock / energy 全分母 | ✅ T13 已关闭 |
| **O-36** | hot ingot 冷却曲线的 GT6 等价来源 | 独立保真债 |
| **O-37** | Raw Oil / `CrudeOil` 身份桥 | T18 |

O-30 / O-31 / O-32 已分别由 T12 首批能源拓扑、多方块通用层和持续 load gate 关闭，不重新开项；O-31 的旧端口计数错误已由 T15e 以审计修正，不改变 T12d 的架构关闭结论。

---

## 5. 第二阶段最终快照

| 项 | 关闭值 |
|---|---:|
| 材料定义 | 1,773 GT6 live + 1 `natural_gas` supplemental |
| 注册形态 | 16,048 |
| 前缀 / handshake | 56 / 1,829 |
| `ITEMGENERATOR` 域 | 16 / 25 |
| worldgen | 134 vein features / 2 fluid deposits |
| pipe / cover | 282 blocks / 3 behavior types |
| processing / non-spec kind | 25 / 12 |
| tier variants | 9 |
| JSON multiblock | 2 |
| datapack / publication | 5,985 / 18,875 |
| compression | 3.154 : 1 |
| GameTest | 53 / 53 |
| Java / Python tests | 449 / 272 |

---

## 6. 第三阶段交接

第三阶段从 **T13 · GT6 可比较分母清点** 开始。开工前只读：

1. 《[CrucibleCraft-总体规划.md](CrucibleCraft-总体规划.md)》§5、§7–§9；
2. 《[CrucibleCraft-第三阶段总体规划.md](CrucibleCraft-第三阶段总体规划.md)》T13 卡；
3. `tools/t12_closure_readiness.json` 与 `tools/t12_machine_readiness.json` 的状态差异。

第三阶段不能把“约 65 cover”“44 个类文件”或 720,841 source rows 直接当作实现分母；T13 必须先规范化 abstract/helper/variant，建立 source / in-scope / selected / published 的可比较口径。
