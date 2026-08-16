# CrucibleCraft 第五阶段总体规划

> GregTech 6 → Minecraft 1.21.1 NeoForge 移植 · 第五阶段
> 版本：v2（2026-08-15）。v1 只有 T27→RC→GA 一条线；v2 按作者意图扩为
> **一张分类卡 + 四条内容轨**，并用工作树 artifact 重算了全部分母
> 状态：◯ **未开工**。硬前置 = `T26_READY`（T26 仍 OPEN，等试玩）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 机器可读契约（待建）：`tools/phase5_portfolio_contract.json`
> 上游规则：《[CrucibleCraft-总体规划.md](CrucibleCraft-总体规划.md)》第 5 / 7 / 8 / 9 节

---

## 0. 与 v1 规划的差异（先读这段）

v1 把第五阶段写成"分类 → 清 v1_required → 发 1.0"，止步于既有 v1 契约。
作者意图更大：ordinary_optional 全量、核物理、多方块补完、GT6U。

v2 保留 **T27 作为第一张卡**（作者已认可），把其后拆成四条内容轨，并**用工作树里的
artifact 把每条轨的真实体量算了出来**。结论先给：

| 轨 | 名义目标 | 实测体量 | 性质 | 风险 |
|---|---|---|---|---|
| **A** | ordinary_optional 全量 | **78,682 行 / 10 个 map**，95% 集中在 3 个 map | 已有架构上的**数据填充** | 中（压缩比未知） |
| **B** | 多方块补完 | 19 个 post_1_0，其中 **13 个是纯数据** | 通用层**已经写好了** | 低 |
| **C** | 裂变 / 聚变 / 等离子 | 配方仅 **18 行**，其余全是新物理 | **新子系统** | 高（不可估算） |
| **D** | GT6U 全量 | **仓库里没有 GT6U 源** | **新来源** | 最高（今天不可证伪） |

三条重要更正，均来自 artifact 而非推断：

1. **载荷天花板绑的是 eager，不是 logical。** v1 沿用了文档里的"余量 2,118"，
   那是 T22/T22.5 把 21,000 套在 logical 上算的。权威门禁
   `t14_load_budget_policy.json` 与 `full_verification_report.json` 的
   `t14_load_acceptance.load_gate.budgets` 都只定义 `eager_publication_rows`
   soft 18,000 / hard 21,000，**没有 logical 硬顶**。真实余量见 §2.3。
2. **v1 说的"574,000 行未分类"是错的。** `gt6_map_roadmap.json` 已给全部 95 个
   map 落了 planning status，`unclassified = 0`。准确说法见 §1.2。
3. **"写好通用结构"这件事已经完成了。** T12 的 JSON/validator + T23 的 5 个
   plugin 白名单就是通用层；19 个待补多方块里 13 个不需要新 Java。见 §4。

---

## 1. 第五阶段是什么

前四个阶段：搬事实 → 搭架构 → 填工业阶梯 → 收成公开 Beta。

第五阶段是**在已封闭的架构上做大规模内容填充**，同时决定 v1.0 在这条时间线上
的位置。它有一个不可跳过的入口（T27）和四条可独立排序的轨。

**唯一硬规则不变：任何时刻最多一张 T 进行中。** 四条轨是**分类维度**，不是并行
执行许可。

### 1.1 阶段形状

```
T27 portfolio freeze  ← 唯一入口，纯工具卡，publication delta = 0
      │
      ├── 轨 A  ordinary_optional 长尾      （一机一卡，10 张：3 大 7 小）
      ├── 轨 B  多方块补完                  （13 张数据 + 4~6 张行为）
      ├── 轨 C  裂变/聚变/等离子            （先测量卡，再实现）
      └── 轨 D  GT6U                        （先源获取与分母卡）
      │
   RC → GA   ← 位置由 §6 的契约决策确定
```

### 1.2 95 个 map 的真实状态（更正 v1）

`gt6_map_roadmap.json`，720,841 行全部有 map 级 status：

| status | map 数 | 行数 | 含义 |
|---|---:|---:|---|
| `PORTED` | 16 | 441,899 | 已移植（含 extruder 325,595 行） |
| `BOUNDED_SUBSET_PORTED` | 13 | 163,141 | 机器已实现，**长尾未补** ← 轨 A 在这里 |
| `DEFERRED` | 64 | 106,159 | **机器本身没做** ← 不在轨 A 范围内 |
| `OUT_OF_SCOPE` | 2 | 9,642 | anvil 基座图等 |

**关键区分**：轨 A（ordinary_optional）只覆盖 `BOUNDED_SUBSET_PORTED` 的
10 个 map，**不新增任何机器**。那 64 个 `DEFERRED` map 是"新机器"，是另一件事：

| 行数 | map | deferral_category |
|---:|---|---|
| 27,291 | boxinator | PACKAGING_OR_AUTOMATION |
| 17,513 | unboxinator | PACKAGING_OR_AUTOMATION |
| 10,960 | toolhead | UNIMPLEMENTED_SPECIALIZED_MACHINE |
| 7,746 | steamcracking | CHEMICAL_OR_THERMAL_PIPELINE |
| 7,637 | sharpener | UNIMPLEMENTED_SPECIALIZED_MACHINE |
| 6,756 | melter | CHEMICAL_OR_THERMAL_PIPELINE |
| … | 其余 58 个 | 合计 28,256 |

**如果作者的"GT6 配方基本全量"包含这 64 个 map，那是第五条轨（轨 E），
必须显式加进契约**，不能默认它被轨 A 覆盖了。

**准确的分类现状**：map 级 `unclassified = 0`；**行级**四列 disposition 只做过
10 个 map（T22.5 的 146,841 行宇宙）。`DEFERRED` 的 106,159 行只有 map 级
deferral_category，没有行级 disposition —— 这是 T27 的实际增量工作。

---

## 2. T27 · portfolio freeze（唯一入口，保持不变）

**性质**：纯工具卡。参照 T22.5 / T25 先例，publication delta = 0 / 0 / 0，零新注册对象（机器断言）。
**依赖**：`T26_READY`。

### 2.1 开工硬前置

T27 不得在下列任一条未满足时开卡：

- [ ] `T26_READY` 已由一次完整验证会话绑定；
- [ ] T26 known-issue 台账存在，每条含 severity / workaround / owner / release disposition；
- [ ] T24-F003 / T24-F005 的 target/stress 复测已在 ≥16 GB 声明环境完成或落显式 disposition；
- [ ] `anvil_bend_big` / `anvil_bend_small`（registered_zero_logical）已拍板；
- [ ] 未选中的 v1_required 多方块 `crucible` 已复核并给出 owner；
- [ ] O-15 按玩家可见域关闭；
- [ ] 工作树干净、`--check-ready` 全绿。

### 2.2 一句话判据（继承第四阶段原文）

> 重新消费七类 T13 denominator、T20–T26 的 READY / 测量结果、全部 deferred 与
> open item，为每个 source identity 建立唯一 CC implementation 和
> `v1_required / post_1_0 / out_of_scope` disposition，使 `unclassified = 0`；
> 然后只为实际剩余的 v1_required 项生成连续、单 family / 单物理 / 单玩家循环卡。

v2 增补：T27 还必须为**四条轨各自**输出一份可独立排序的 identity 集合，
以及每条轨的初始 publication 投影。

### 2.3 T27 必须先算的数字（已用工作树预算，T27 复核）

**载荷余量（更正口径）**——权威定义在 `t14_load_budget_policy.json`：

| 轴 | 当前 | soft | hard | 到 soft | 到 hard |
|---|---:|---:|---:|---:|---:|
| `eager_publication_rows` | 16,657 | 18,000 | 21,000 | **1,343** | **4,343** |
| `lazy_logical_rows` | 2,225 | 16,000 | 56,000 | 13,775 | 53,775 |
| `datapack_authored_entries` | 3,243 | 6,000 | 6,600 | 2,757 | 3,357 |
| logical（= eager + lazy） | 18,882 | — | **无独立硬顶** | — | — |

> **单卡可新增 logical 的理论上限 = 4,343 (eager) + 53,775 (lazy) = 58,118。**
> 文档里流传的"余量 2,118 / 2,121"来自 `t22_g3_headroom.json` 把 21,000 套在
> logical 上，与权威 policy 不一致。T27 必须二选一并写进契约：
> (a) 采纳 eager 口径（余量 58,118），或 (b) 显式新增一条 logical 硬顶并给出测量依据。
> **在澄清之前，轨 A 的所有容量结论都不成立。**

**七表分母**（`t13_denominator_manifest.json`，`unclassified = 0`）：

| 表 | 基数 | in_scope | deferred | out_of_scope |
|---|---:|---:|---:|---:|
| recipe_maps | 95（720,841 行） | 29 | 64 | 2 |
| prefixes | 452 | 54 | 272 | 126 |
| machine_kinds | 96 | 81 | 15 | 0 |
| itemgenerator_domains | 25 | 16 | 3 | 6 |
| energy_identities | 20 | 7 | 5 | 8 |
| cover_kinds | 47 | 9 | 28 | 10 |
| multiblock_kinds | 30 | 5 | 20 | 5 |

**禁止求和伪装覆盖率**：T16 的 selected 5、T17 的 3、T18 的 6、T19 的 5 各自
对应不同子分母，不得相加除以 96 或 47。比例只能由 portfolio 行派生。

### 2.4 T27 三轴与完成信号

**Closure** — 七表 + deferred/open-item union 全部有唯一记录；`unclassified = 0`；
**行级** disposition 覆盖到轨 A/E 需要的 map；四条轨的卡列表可由 portfolio 确定性生成。

**Fidelity** — source identity → CC implementation 映射有固定来源；"无实现"不被
同名对象伪装；`DESIGN_POLICY` 与 GT6 直接事实分开。

**Load** — 每条轨给出初始 publication 投影；无法估算的新 subsystem（轨 C）先生成
测量卡，不伪造成本；本卡 publication delta = 0 / 0 / 0。

**完成信号**：`unclassified = 0`；四条轨的卡序可重算；本卡不实现任何 family，
不直接编号 RC；`T27_READY` 绑定一次完整验证。

---

## 3. 轨 A · ordinary_optional 长尾（一机一卡）

### 3.1 真实分母：78,682 行，10 个 map

`t22_5_row_classification.json` 把 T21 ledger-2 的 146,841 行宇宙精分为 9 类，
其中 `ordinary_optional` = **78,682**（其余为 cross_mod_compat 48,247、
post_1_0_g10 13,042、out_of_scope 5,860、ore_processing_byproduct 894、
generic_processing 102、already_covered 8、post_1_0_nuclear 6、v1_required 0）。

**先定口径**：轨 A 是补 78,682（精分后）还是 146,841（ledger-2 原始宇宙）？
差 1.87 倍。cross_mod_compat 的 48,247 行是别的模组的材料，没有那些模组时无意义，
**建议只取 78,682**，并把 cross_mod_compat 显式留在 O-41 的翻译层轨上。

### 3.2 分布极度倾斜 —— 所以"一机一卡"是对的

| ordinary_optional 行 | map | 卡的量级 |
|---:|---|---|
| 49,411 | bath | 大卡（63%） |
| 18,173 | smelter | 大卡（23%） |
| 7,193 | mixer | 大卡（9%） |
| 1,581 | assembler | 小卡 |
| 1,284 | compressor | 小卡 |
| 373 | autoclave | 小卡 |
| 250 | centrifuge | 小卡 |
| 193 | drying | 小卡 |
| 151 | electrolyzer | 小卡 |
| 73 | roaster | 小卡 |
| **78,682** | **10 个 map** | **3 大 + 7 小** |

7 张小卡合计 3,905 行（5%）。**建议先做 7 张小卡**：它们能在真实内容上验证整条
补长尾的流水线（分类 → MaterialRule 编写 → 展开 → 双向等价 → 载荷记账），
成本低，且失败代价小。三张大卡放在流水线被证明之后。

### 3.3 决定成败的唯一数字：source → logical 压缩比

**这个数今天不存在，必须先算。** 它决定轨 A 是"几乎免费"还是"数学上做不到"：

- **先例（好的一端）**：extruder **325,595 source 行 → 2,782 logical**，比值 **117 : 1**。
  T14 的 20 条 compact rule ↔ 2,782 logical 全字段双向等价。
- **先例（另一端）**：T21 覆盖 mixer 64,245 行denominator，实际只发布 **+4 logical**——
  因为 v1_required 只有 1 个 unit。这不是压缩，是没做。

两种极端下的轨 A 载荷：

| 假设比值 | 78,682 行 → logical | 对上限 58,118 |
|---|---:|---|
| 117 : 1（extruder 级） | ≈ 673 | 轻松装下 |
| 10 : 1 | ≈ 7,868 | 装得下，需 lazy 承接 |
| 1 : 1（无压缩） | 78,682 | **超出 20,564 行** |

**因此轨 A 的第一张卡必须是一张"压缩比测算卡"**，对 10 个 map 各跑一次
normalization dry-run，输出每 map 的 (source 行, 预期 authored rule, 预期 logical,
eager/lazy 分配)。这是纯工具卡，publication delta = 0，成本很低，但没有它，
后面每一张内容卡都在赌。

这条完全符合总体规划第 7 节"如果有形态/数量爆炸的可能，**先把数字算出来写在纸上**"。

### 3.4 每张内容卡的门禁

除总体规划第 8 节外：

- [ ] 本卡 map 的行级 disposition 来自 T27 portfolio，不现场发明；
- [ ] source 可投影全集与独立期望集双向相等（不抽样）；
- [ ] `UNIFORM_*` 压平检测通过（总体规划 5.7）；
- [ ] authored / logical / eager / lazy 四个数分别落账，余量重新公布；
- [ ] 压缩比若低于测算卡的预测值，**先停下改测算模型**，不得直接超预算发布；
- [ ] 新增注册对象的 `en_us` / 模型 / 真实 `zh_cn` 债按域落账（5.8 不倒退）。

---

## 4. 轨 B · 多方块补完（通用层已经写好了）

### 4.1 30 kind 的现状（`t23_multiblock_behavior_classification.json`）

- **v1_required 6**：centrifuge ✓、coke_oven ✓、distillation_tower ✓、
  large_boiler ✓、tank_3x3x3 ✓（5 个已实现）+ **crucible（未实现）**
- **post_1_0 19**
- **out_of_scope 5**：bedrock_drill、fusion_reactor、lightning_rod、
  matter_fabricator、von_da_graagg

### 4.2 19 个 post_1_0 按 controller behavior 分解 —— 这才是工作量

| behavior kind | 数量 | 结构 | 现有 plugin | 边际成本 |
|---|---:|---|---|---|
| `processing_host` | **12** | autoclave / bath / coagulator / crusher / cryo_distillation_tower / electrolyzer / fermenter / implosion_compressor / mixer / shredder / sluice / squeezer | ✅ 已有（2 个消费者） | **纯数据**：结构 JSON + 获取配方 + GameTest |
| `conversion` | 4 | large_dynamo / large_heat_exchanger / large_turbine_gas / large_turbine_steam | 部分（`steam_conversion` / `heat_energy_input`） | 涡轮/换热是**新物理** |
| `thermal` | 1 | oven | ❌ 由 crucible 建立 | 做完 crucible 后**变成数据** |
| `storage` | 1 | tank_5x5x5_metal | ✅ 已有（`storage_host`） | **纯数据** |
| `logistics` | 1 | logistics_core | ❌ 无先例 | 新行为家族 |

`t23_plugin_whitelist.json` 实测：`processing_host` 已有 large_centrifuge +
distillation_tower 两个真实消费者，`storage_host` 有 tank_3x3x3。

### 4.3 结论：轨 B 比想象中小得多

> **"多方块结构全补完或写好通用结构"——通用结构在 T12/T23 已经写好了。**
> 剩下的 19 个里，**13 个（12 processing_host + 1 storage）不需要新 Java**。

建议卡序：

1. **crucible**（v1_required，thermal 家族）→ 顺带解锁 oven；
2. **批量数据卡 ×2~3**：13 个 data-only 结构按 RecipeMap 归属合批，每卡 4~5 个结构。
   合批的合法性来自 T23c 已证的"同 plugin 新实例只改数据"；
3. **conversion 行为卡 ×2**：涡轮（gas/steam 共享转子物理）、换热器；
4. **logistics_core**（新家族，独立卡）；
5. out_of_scope 5 个：fusion_reactor 并入轨 C；bedrock_drill / lightning_rod
   需要 `world_interaction` 新家族；matter_fabricator 与 von_da_graagg 建议保持
   out_of_scope。

轨 B 的 publication 影响接近 0（多方块本身不产生 GT logical 行；T23 三个结构的
delta 是 0/0/0 + 3 条 vanilla datapack 条目）。**轨 B 不消耗 eager 余量**，
可以与轨 A 交错排布。

---

## 5. 轨 C · 裂变 / 聚变 / 等离子（配方几乎为零，成本全在物理）

### 5.1 实测体量

| 项 | 实测 | 出处 |
|---|---|---|
| `gt.recipe.fusionreactor` | **18 行** | gt6_map_roadmap |
| `gt.recipe.fuels.plasma` | **0 行（空 map）** | gt6_map_roadmap |
| 核相关 canonical machine kind | 2（ReactorCore1x1 / ReactorCore2x2） | t13 machine_kinds |
| 反应堆棒（Absorber/Breeder/Moderator/Nuclear/Reflector/Depleted/Product/Base） | **被排除在 96 之外**，标 `reactor_part_not_machine_behavior` | t13 machine_kinds |
| fusion_reactor 多方块 | 当前 `out_of_scope` | t23 分类 |
| energy identity | 20 个里 8 个 out_of_scope，含 `DEFERRED_REACTOR_INTERNAL` | t13 energy_identities |

### 5.2 结论：这是 `blocked_by_new_subsystem`，不是"补配方"

18 行配方说明**GT6 的核系统不靠 RecipeMap 表达**，而靠反应堆棒的相邻交互、
中子/热模拟、冷却回路。CC 现在：

- 没有反应堆棒的行为模型（它们连 machine kind 分母都不在）；
- 没有 plasma 能源身份；
- 没有 fusion 档位的能量上限语义；
- `MultiblockControllerPluginRegistry` 里没有对应 behavior 家族。

按项目自己的规则（T27c 第 5 条 + 三轴 Load"无法估算的新 subsystem 先生成测量卡，
不伪造精确成本"），轨 C 的第一张卡**必须是测量/设计卡**，输出：

- 反应堆棒相邻交互与热/中子模型的 source 事实（固定 revision，可复算）；
- 需要新增的 energy identity 与它们和 RU/KU/EU/HU/STEAM 的边界（9.2 不得折叠）；
- 每 tick 操作计数上限（T24 协议）；
- 与 T23 plugin 白名单的接口。

**在这张卡关闭前，轨 C 的工期不可估算，也不得进入任何发布承诺。**

---

## 6. 轨 D · GT6U（今天不可证伪）

### 6.1 硬事实

**工作树里没有任何 GT6U 源。** 全仓库唯一的固定 dump 是
`gt6_dump/gt6_recipe_dump`（96 个 map 文件、689 MB），来自固定 GT6 revision
`3703e40308c8c030763fd6297dea8b210d2a77b1`。

GT6U 在代码与数据里只出现在三处，全部是**否定式声明**：
`phase4_v1_planning_contract.json` 的 `"it does not claim a complete GT6 or GT6U port"`、
`out_of_scope` 列表、以及 `GT6ImportUnits` 的注释。

没有 GT6U 的 revision、tree manifest、blob manifest、symbol inventory 或 dump。

### 6.2 所以第一张卡是"源获取与分母清点"，不是"移植配方"

按总体规划第 7 节开工检查表——"固定来源的 revision、文件摘要、符号/提取键与
人工行锚点已经复核；任一不一致时不开工"——轨 D 现在**连开工检查都过不了**。

轨 D 的第一张卡必须是 **T13 等价卡**：固定 GT6U revision → tree/blob manifest →
dump → 七表等价物 → 与现有 GT6 分母的 identity 冲突解析（同名不同义、
GT6U 覆盖 GT6 的项、纯新增项）。这张卡的产出才让"GT6U 全量移植"变成可证伪命题。

### 6.3 冲突解析是真正的难点

GT6U 是 GT6 的**非官方续作**，会改动既有配方与材料。CC 当前所有 authored rule、
独立期望集与 `SOURCE_BACKED` 声明都绑定在固定 GT6 revision 上。引入第二个来源
意味着每个 identity 要回答"以哪个来源为准"，并且 `SOURCE_BACKED` 词表要扩展
（`SOURCE_BACKED_GT6` / `SOURCE_BACKED_GT6U` / 冲突项的显式裁决）。

**这是一次架构级变更，不是内容轨。** 建议它独立于 v1.0，且在轨 A/B 稳定之后再评估。

---

## 7. 必须由作者拍板的契约决策

四条轨的体量与风险差了几个数量级，因此 **v1.0 放在哪** 不能默认，必须显式决定
并写进 `tools/phase5_portfolio_contract.json`（带日期与理由）。三个方案：

| 方案 | v1.0 内容 | 1.0 可交付性 | 代价 |
|---|---|---|---|
| **① 先发 1.0** | T27 清完既有 `v1_required`（crucible + O-36 等）即 RC → GA；轨 A/B/C/D 全部作为 1.x | 最快、最确定 | 1.0 内容比作者期望少 |
| **② 全进 v1**（作者原话的字面读法） | 轨 A+B+C+D 全部关闭才 RC | **不可估算**：轨 C 无成本模型，轨 D 无来源 | 1.0 被绑在一个还不存在的 GT6U 源上，可能永远发不出 |
| **③ 折中（推荐）** | v1 = 既有 v1_required + **轨 A** + **轨 B 的 13 个数据结构 + crucible**；轨 C / D / 轨 B 行为卡 / 轨 E（64 个 DEFERRED map）留 1.x | 可估算：全部是**已有架构上的数据填充** | 核与 GT6U 推后 |

**推荐方案 ③，理由是分界线天然存在**：轨 A 和轨 B 的 13 个结构消费的是
MaterialRule、JSON structure、plugin 白名单这些**已经付过成本的架构**，边际成本
可算、风险可控；轨 C 是新物理，轨 D 是新来源，两者都要从零建分母。把可交付的
东西和不可估算的东西捆在同一个版本号里，是让前者陪后者一起延期。

采用 ③ 时，v1.0 的产品声明需要相应扩写（现文本是"GT6 风格工业主链完整且可稳定
发布"），例如改为"工业主链完整 + 10 台已实现机器的 GT6 长尾配方全覆盖 +
GT6 标准多方块数据层全覆盖"，并保留"不表示 GT6 或 GT6U 全量移植"。
**改契约必须是显式动作**，不能靠某张卡的范围蔓延实现。

### 7.1 轨 E 需要显式表态

§1.2 的 64 个 `DEFERRED` map（106,159 行、boxinator / unboxinator / toolhead /
steamcracking / melter / fermenter …）是**新机器**，不在轨 A 内。作者说的
"GT6 配方基本全量移植"如果包含它们，必须显式建立轨 E 并进契约；否则默认它们
保持 `post_1_0`。

---

## 8. 第五阶段方法论增补

### 8.1 公开 Beta 期间的缺陷入口（新规则，需拍板）

前四阶段闭门开发，"一次一张 T"没有外部中断源；第五阶段全程有 Beta 在跑。
建议四类分流，**只有第一类可以打断 active T**：

| 类型 | 判断 | 处理 |
|---|---|---|
| **回归** | 已关闭 T 的判据现在跑不通 | 在当前 active 卡内作为有界修复单元处理；补能捕获该形状的回归测试；在被推翻卡的阶段档案追加修订行——**不重开已关闭的 T** |
| **正确性缺陷** | 判据没被推翻但玩家链路错 | 进 known-issue 台账定级；blocking 级进 portfolio |
| **完备性缺口** | 能跑通、覆盖不全 | 进 portfolio 分类，**不修** |
| **体验 / 性能 / 洁癖** | 能跑通 | 进台账；性能项需 T24 协议证据才能升级 |

### 8.2 版本列车与 hotfix 例外（新规则，需拍板）

轨 A 的三张大卡各自可能跨很长时间。若期间不能发版，Beta 会长期停在
`0.1.0-beta.1`。建议唯一例外：

- **允许**发 `0.1.0-beta.N` 而不新开 T，条件全部满足：① 变更仅由已登记的修复单元
  与当前卡已关闭的子判据构成；② publication delta 已声明并进容量账；
  ③ 完成一次 `run_full_verification.py --record`；④ CHANGELOG 与台账同步。
- **不允许**借 hotfix 夹带新 family、新注册对象或未登记工作。
- 序列：`0.1.0-beta.N` → `1.0.0-rc.N` → `1.0.0` → `1.x`。RC 编号权只在 RC 卡手里。

### 8.3 大卡必须先证流水线，再上体量

轨 A 的 bath（49,411 行）单张卡就超过既有全部 publication 的 2.6 倍。规则：
**同一流水线未在小卡上跑通前，不得开体量超过 5,000 行的卡。**
这是 T14（先 Extruder 单 family 证压缩，再谈全局）的直接沿用。

### 8.4 分类段不得被实现冲动打断

T27 的 publication delta 必须是 0，机器断言，不是自觉。T22.5 与 T25 都以零内容
关闭过，先例充分。分类期间产生的所有实现想法进 portfolio 排队。

### 8.5 每卡 `v1_required` 严格递减

每张实现卡关闭后，portfolio 的 `v1_required` 计数必须严格递减，递减量等于本卡
identity 数。**它让"还差多少"变成一个单调下降的数**；没有它，内容轨会退化成
"感觉快好了"。

---

## 9. 悬案 owner（第五阶段视角）

| 编号 | 内容 | owner |
|---|---|---|
| **O-15** | 本地化分账（`en_us` 3,173 / 真实 `zh_cn` 874 / 208 + 1,566） | T26 关闭；GA 卡全量核对 |
| **O-36** | hot-ingot 冷却曲线仍 `DESIGN_POLICY + UNVERIFIED` | T27 → 生成 v1-required fidelity 卡 |
| **O-41** | 1,002 行翻译层接线 | T27 freeze 复核；建议与 cross_mod_compat 48,247 行合并为独立翻译轨 |
| **T24-F003 / F005** | target/stress 复测，需 ≥16 GB 声明环境 | T26 落 disposition；RC 复测 |
| **anvil_bend ×2** | registered_zero_logical | T26 freeze → T27 portfolio |
| **`crucible`** | v1_required 未实现，thermal 家族 | T27 判定；建议作为轨 B 第一张卡 |
| **载荷天花板轴** | 21,000 绑 eager（权威）vs 绑 logical（T22 用法） | **T27 开卡即澄清并写进契约** |
| **轨 A 分母口径** | 78,682（精分）vs 146,841（ledger-2） | T27 拍板 |
| **轨 E 是否存在** | 64 个 DEFERRED map / 106,159 行 | **作者拍板** |
| **v1.0 位置** | §7 三方案 | **作者拍板** |
| **Beta 缺陷入口 / hotfix 例外** | §8.1 / §8.2 | **作者拍板** |

---

## 10. 风险与止损

| 风险 | 征兆 | 止损 |
|---|---|---|
| **轨 A 压缩比不及预期** | 测算卡给出 <5:1 | 停下，先做规则层压缩（T14 协议），或按 map 缩范围；**不得超预算发布** |
| **bath 大卡吞掉整个阶段** | 一张卡做了一个月还在分类 | 先做 7 张小卡验证流水线；bath 内部按 fluid family 再拆 |
| **轨 C 无限膨胀** | 开始写反应堆代码却没有测量卡 | 强制先关测量卡；未关闭前不进任何发布承诺 |
| **轨 D 拖住 1.0** | 1.0 的 RC 条件里出现 GT6U | 采用 §7 方案 ③，把 GT6U 移出 v1 |
| **两个来源打架** | `SOURCE_BACKED` 不再唯一 | 轨 D 的分母卡必须先扩来源词表并解析冲突 |
| **Beta 反馈冲散主线** | 每周都在处理 issue，active T 三周没动 | §8.1 四类入口；只有回归可打断 |
| **发布前记账退化** | 文档里 `post_1_0` 条目在变少 | §8.5 单调递减断言；GA 卡全量核对 |

---

## 11. 与其他文档的关系

- 不可违反的规则与架构不变量：《[CrucibleCraft-总体规划.md](CrucibleCraft-总体规划.md)》
- 第四阶段关闭事实与 T27 原始定义：《[CrucibleCraft-第四阶段总体规划.md](CrucibleCraft-第四阶段总体规划.md)》
- 当前进度：《[CrucibleCraft-交接说明.md](CrucibleCraft-交接说明.md)》、《[CrucibleCraft-阶段档案-T26.md](CrucibleCraft-阶段档案-T26.md)》

**本文在 `T26_READY` 之前只是规划材料，不具备执行权威。** §2.3 与 §3 的所有数字
来自 2026-08-14 工作树快照，T27 开卡时必须从 artifact 重新派生；不一致以 artifact 为准。
