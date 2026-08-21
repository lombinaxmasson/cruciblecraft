# CrucibleCraft × GT6U 搬运差距分析

> 目的：对照「现计划基础」与「GT6U 全部内容」，回答还差什么才能开始、以及全量搬运还差哪些层。  
> 日期：2026-08-15  
> 状态：分析文档（非开工授权；不改变 T26 / T27 / v1.0 契约）

---

## 0. 一句话结论

**源码已经进仓，但 GT6U 仍不可移植。**  
当前缺的是：与 GT6 同级的 **dump / tree-blob manifest / 分母 / 双来源裁决**，以及大量尚未存在的 **RecipeMap、机器、多方块行为、材料/流体、核与生物子系统**。  
多方块 JSON 通用层与材料规则管线是可复用的；它们不能替代上述缺口。

| 层 | 现计划基础 | GT6U 全量还差什么 |
|---|---|---|
| 来源固定 | GT6 rev `3703e403…` + dump | GT6U rev 可固定，但无 dump / manifest / 分母 |
| 材料 / 前缀 | 1,773 ACTIVE + oredict 管线 | `MT.GT6U.*`、新增流体、与 GT6 冲突裁决 |
| RecipeMap | CC 运行 32；GT6 分母 95 | GT6U 新 map ≥14；且大量 GT6 map 仍 DEFERRED |
| 单块机器 | T16–T18 selected 子集 | Heat Mixer / Laser Cutter / Incubator / Circuit Assembler 等 |
| 多方块通用层 | T12 schema + T23 5 plugin | 可用；GT6U 新结构多数仍要新 BE/spec/测试 |
| 配方内容 | 有界主链 + 长尾未补 | 石油/聚合物/电路/铬铁矿等整条 GT6U 树 |
| 物理子系统 | 无核 / 无粒子对撞 | Reactor DistW、Fusion、Particle Collider、BioLab |
| 契约位置 | 轨 D · post-1.0 | **不阻断 v1.0 / RC** |

---

## 1. 坐标：现计划把 GT6U 放在哪

权威规划见《[CrucibleCraft-第五阶段总体规划.md](CrucibleCraft-第五阶段总体规划.md)》§3.4 **轨 D**：

1. 固定可再现的 GT6U revision 与许可证边界；  
2. 建立 tree/blob/symbol manifest 和 recipe dump；  
3. 派生可复算的 canonical denominator；  
4. 对 GT6 / GT6U 共名、冲突、新增 identity 做显式 lineage；  
5. 扩展 fidelity 词表，禁止 GT6 与 GT6U 事实混写。

产品契约（《[tools/phase4_v1_planning_contract.json](tools/phase4_v1_planning_contract.json)》）明确：

> v1.0 = GT6 风格工业主链可稳定发布；**不表示 GT6 或 GT6U 全量移植**。

因此：本文是 **1.x / 轨 D 差距图**，不是把 GT6U 抬进 v1.0 的提案。

当前执行位置：

- 唯一 active：`T26`（公开 Beta 门禁，等试玩）  
- 下一入口：`T27` portfolio freeze  
- GT6U：轨 D，且 **D0（源获取）未开工**

---

## 2. 两侧来源快照（2026-08-15）

### 2.1 CrucibleCraft / GT6（已接通）

| 项 | 值 |
|---|---|
| GT6 revision | `3703e40308c8c030763fd6297dea8b210d2a77b1` |
| 本地 dump | `gt6_dump/gt6_recipe_dump/`（gitignored） |
| dump 工具 | `gt6dump-1.0`（见 `tools/gt6_reference_metadata.json`） |
| dump maps | **95** 个 `gt.recipe.*.json` |
| oredict | materials / prefixes / fluid_map |
| worldgen / tools / textures | 已有子目录 |
| tree/blob | `tools/t13_gt6_tree_manifest.json` 等 |
| CC 材料定义 | ~1,774 JSON |
| CC 运行时 RecipeMap | **32**（`ModRecipeMaps`） |
| CC 多方块 JSON | 5：`coke_oven` / `large_centrifuge` / `distillation_tower` / `large_boiler` / `tank_3x3x3` |
| GT6 多方块分母 | **30** kind（T13/T23） |

### 2.2 GT6U（源码在，管线未接通）

| 项 | 值 |
|---|---|
| 源码路径 | `gt6u_code/GregTech6-Unofficial/` |
| remote | `https://github.com/GregTech6-Unofficial/GregTech6-Unofficial.git` |
| 工作树 HEAD | `4972d0468ee2ea0e896af1e4afe4018d4e2294e6`（Changelog） |
| recipe dump | **无** `gt6u_dump/` |
| tree/blob/symbol manifest | **无** |
| 七表分母 | **无** |
| 与 GT6 dump 的 map 差集（粗测） | GT6U `RM.java` 多出约 **14** 个 `gt.recipe.*` id |

> 规划草稿曾写「仓库里没有 GT6U 源」——**已过时**。源码在，证据链仍缺。

---

## 3. 分层差距（按搬运依赖顺序）

### 3.1 Dump 与 provenance（最大硬缺口）

现有全部 builder / currentness / `--full-replay` 都绑死在：

```text
gt6_dump/gt6_recipe_dump/{maps,oredict,worldgen,tools,textures,schema,index.json}
```

| 子项 | GT6 | GT6U | 差距 |
|---|---|---|---|
| 运行时 recipe dump | 有 | 无 | 需适配 `gt6dump` 或新导出器，对 GT6U jar/dev 环境跑一遍 |
| oredict materials/prefixes/fluids | 有 | 无 | 否则无法批量导入 PVC/PTFE/Epoxid 与新流体 |
| worldgen dump | 有 | 无 | 椰子树、井、油井等无法 SOURCE_BACKED |
| tools / textures dump | 有 | 无 | 工具头与贴图指纹 |
| tree/blob manifest | T13 已有 | 无 | 无法做 currentness / symbol inventory |
| `gt6_reference_metadata.json` 等价物 | 有 | 无 | 版本、config digest、mod 清单 |
| license / attribution 边界 | GT6 | LGPL + assets CC0/logo 限制 | 需单独记录「只搬 GT6U 新增内容」的边界 |

**没有 dump，就没有可证伪的「全量」。** 这是轨 D 的 D0 门禁，与内容实现卡无关。

---

### 3.2 双来源架构（第二硬缺口）

CC 今天的 provenance 词表默认单一 GT6：

- `SOURCE_BACKED` / `SOURCE_DERIVED` / `DESIGN_POLICY` / …

引入 GT6U 后，至少要能表达：

| 新能力 | 为何必须 |
|---|---|
| `SOURCE_BACKED_GT6` vs `SOURCE_BACKED_GT6U` | 禁止混写 |
| lineage / conflict disposition | 同名材料、同 map 不同行、GT6U 覆盖官方配方 |
| import policy：overlay / replace / dual-track | 决定电路板、裂解、铬铁矿等以谁为准 |
| currentness 双树 | `t13_gt6_*` 与 `t??_gt6u_*` 并列，不能互相冒充 |

这是**架构卡**，不是「多导入几个 JSON」。在 D0 关闭前，不得把 GT6U 行灌进现有 GT6 expected / publication。

---

### 3.3 材料与流体

**已有基础（可复用）**

- 材料 JSON → prefix catalog → registration gate  
- `import_gt6_oredict.py` + L1/L2/L3 策略  
- 流体 cell、generifier、危险语义（T11）  
- 管/桶由材料事实投影（T8）

**GT6U 增量（`MT.GT6U`，ID 约 15000–15607）示例**

| 类 | 代表 |
|---|---|
| 聚合物 | PVC、PTFE、PE、Epoxid |
| 酸/中间体 | H3PO4、ElectroEtchingSolution、H2S2O8、PF5、碳酸酯类… |
| 铬铁矿链 | Na2CrO4、Na2Cr2O7、Cr2O3、AlCl3… |
| 其它 | Nb2Ti3C4、LiF、LiPF6、CaO、电离 H/He… |

**还差**

1. GT6U oredict dump → normalized materials/prefixes/fluids  
2. activation policy（哪些进 ACTIVE catalog）  
3. 管道/罐/cell 投影（PVC 物品管、PTFE 流体管/桶已在 GT6U 注册逻辑里）  
4. 与现有 1,773 材料的 ID/名称冲突表  
5. 流体危险/燃烧/食物属性（椰子油、生物培养液等）

---

### 3.4 RecipeMap 与配方内容

**CC 已有 32 个运行时 map**（主链子集）：  
coke_oven、crusher、anvil*、sluice、bath、centrifuge、shredder、sifter、smelter、cooling、mortar、extruder、cutter、lathe、rollingmill、rollbender、wiremill、bender、assembler、welder、press、electrolyzer、mixer、distillery、autoclave、drying、compressor、generifier、fuels_engine、fuels_gas。

**GT6 分母**：95 map；其中 64 个仍为 map 级 `DEFERRED`（轨 E，与 GT6U 正交）。

**相对当前 GT6 dump，GT6U `RM.java` 多出的 map id（粗测，供 D0 复核）**

| map id | 用途 |
|---|---|
| `gt.recipe.heatmixer` | 聚合物/铬铁矿/有机化学主工艺 |
| `gt.recipe.oilcleaner` | 含硫燃料清洗（Fuel Cleaner） |
| `gt.recipe.crackingtower` | 裂解塔 |
| `gt.recipe.particlecollider` | 粒子对撞 |
| `gt.recipe.maskaligner` | 光刻对准 |
| `gt.recipe.lasercutter` | 激光切割 |
| `gt.recipe.incubator` | 培养箱 |
| `gt.recipe.biolab` | 生化实验室 |
| `gt.recipe.well` | 排水井 / 水井 |
| `gt.recipe.aquaticfarm` | 水产相关 |
| `gt.recipe.ionizer` | 电离 |
| `gt.recipe.radiationprocessing` | 辐射处理 |
| `gt.recipe.fluidsolidifier` | 流体固化（若 dump 命名不一致需复核） |
| `gt.recipe.debarker` | 与 dump 差集有关，D0 需核对是否空 map/改名 |

**配方层还差的不止「新 map」**

- 既有 map 被 GT6U **改写**（电路板改 Epoxid、石油链加含硫中间体等）→ 冲突裁决  
- `Loader_Recipes_Chem.java` 中 PVC/PTFE/Epoxid/铬铁矿/光刻胶整段  
- 电路：`MultiItemTechnological.java` 的 Epoxid 板 + 电解蚀刻液  
- 燃油清洗动态图：`RecipeMapFuelCleaner`  
- 生物：`MultiItemBiology.java` + BioLab/Incubator 行  

轨 A（ordinary_optional 78,682）与轨 E（64 DEFERRED）解决的是 **GT6** 长尾；**不能**代替 GT6U 增量。

---

### 3.5 单块机器（kind / tier）

**已有基础**

- MachineKind × TierProfile × RecipeMap 分离（T12）  
- RU/KU/HU/EU 批次 selected 子集（T16–T18）  
- 换档不复制 publication

**GT6U 新增小机器（注册在 `Loader_MultiTileEntities`）**

| 机器 | 备注 |
|---|---|
| Heat Mixer（多档） | 化学主链核心；CC 无 |
| Electric Autoclave（多档） | CC 有 autoclave，需核对是否同 map/同行为 |
| Circuit Assembler（T1–T5） | LU + Assembler map |
| Laser Cutter（T1–T5） | 新 map |
| Incubator | 新 map + 生物物品 |

**还差**：kind 分母扩表、tier 矩阵、获取配方、GameTest、EMI、能耗身份（含 LU 作为工艺能）。

---

### 3.6 多方块：通用层 vs GT6U 结构

**已有基础（可复用，不是「做完了」）**

| 资产 | 状态 |
|---|---|
| JSON structure schema + validator | T12 关闭 |
| plugin 白名单 | 5 个：processing_host / shared_port_supply / heat_energy_input / steam_conversion / storage_host |
| 已实现结构 | 5 个 JSON + 专用 BE/controller |
| GT6 30 kind 分类 | T23：6 v1_required / 19 post_1_0 / 5 out_of_scope |

**GT6U 大型机器（相对 GT6 分母多为新增）**

| 结构 | 实现类 | 相对 CC |
|---|---|---|
| Large Roasting Oven | `MultiTileEntityRoasting` | 无；roaster map 在 GT6 侧还 DEFERRED |
| Industry Coke Oven | `MultiTileEntityPyrolyseOven` | 有小型 coke_oven；大型不同 |
| Fuel Cleaner | `MultiTileEntityOilCleaner` | 无 |
| Large Dryer | `MultiTileEntityDryer` | 有 drying 单机；大型无 |
| Cracking Tower | `MultiTileEntityCrackingTower` | 无 |
| Particle Collider | `MultiTileEntityParticleCollider` | 无；近聚变复杂度 |
| Mask Aligner | `MultiTileEntityMaskAligner` | 无 |
| Large Heat Mixer | `MultiTileEntityHeatMixer` | 无 |
| Draining Well | `MultiTileEntityDrainingWell` | 无；世界高度/井管约束 |
| Biochemical Research Lab | `MultiTileEntityBiolab` | 无 |
| Large Extruder / Crystallisation Crucible | 对应类 | extruder 有单机；大型无 |
| GT6U 改过的 Distillation / Cryo / Fusion | 既有类被改 | 与 GT6 行为差需 lineage |

GT6U `tileentity/multiblocks` 约 **54** 个 Java 文件；GT6 分母只有 **30** kind。  
轨 B 的「13 个可数据填充」只覆盖 **GT6 post_1_0**，**不覆盖**上表 GT6U 新 kind。

**还差**

1. 把 GT6U 结构纳入新的 multiblock denominator（或扩展 T13 表并标 `SOURCE_BACKED_GT6U`）  
2. B0 式逐 kind 判定：纯数据复用 vs 新 plugin vs 新物理  
3. 对撞机 / 生化 / 排水井等几乎肯定要新 Java + 新测试协议  
4. 合成配方、机壳、本地化、存档 id

---

### 3.7 能源、核与高端物理

| 子系统 | CC | GT6U | 差距 |
|---|---|---|---|
| RU/KU/EU/HU/蒸汽/燃油 | 有界闭合 | 继承并扩展 | 部分可复用 |
| LU（激光）作工艺能 | 部分机器概念 | Circuit Assembler / Mask Aligner / BioLab 消毒 | 需正式 energy identity + 机器接受矩阵 |
| 裂变堆 DistW 冷却 | 无 ReactorCore | `FL.distw` 冷却分支 | 属轨 C ∩ 轨 D，需唯一 owner |
| 聚变 | out_of_scope | 有 Fusion + 改动能 | 轨 C |
| 粒子对撞 / 反物质流体 | 无 | Particle Collider + 等离子体流体 | 新物理 |
| MESU 等储能多方块 | 无/未选 | GT6U README 提及 | 另开 identity |

轨 C（核/聚变/等离子）与轨 D（GT6U）在「反应堆冷却、聚变改动」上重叠——portfolio 必须指定 **唯一 owner**，禁止双算。

---

### 3.8 世界生成与杂项玩法

| 内容 | 源位置 | CC |
|---|---|---|
| 椰子树 | `WorldgenTreeCoconut` + Loader_Worldgen | 无 GT6U worldgen dump；T9/T20 仅 GT6 vein |
| Fluid / Draining Well | MTE + `RM.Well` | 无 |
| 成就页 | `Loader_Achievements` + lang | 无对应系统（1.21 成就模型也不同） |
| 生物培养皿等物品 | `MultiItemBiology` | 无物品域 |
| 粒子物理物品 | `MultiItemPhysics` | 无 |

成就在 1.21 上不能 1:1 搬 Forge 1.7 `AchievementPage`；若要做，需独立设计为进度/advancement 数据，不算「dump 一行」。

---

### 3.9 工具链与验证

| 能力 | 现状 | GT6U 全量还要 |
|---|---|---|
| `import_gt6_oredict.py` | 只认 GT6 dump 路径 | 双根或 `gt6u_dump` 参数 |
| `compare_gt6_recipes.py` / templates | 绑 GT6 maps | GT6U maps + 冲突 diff |
| T13 denominator builders | 单 revision | 第二套 inventory 或 union 表 |
| `run_full_verification` | GT6 currentness | GT6U compact/replay 收据 |
| GameTest 预算 | T23/T24 协议 | 新结构/新物理测量方案 |
| 载荷门禁 | eager 21k hard 等 | 聚合物+化学长尾可能逼近 ceiling；需 D0 投影 |

---

## 4. 与五条内容轨的关系（避免范围串味）

```text
                    ┌─ 轨 A  GT6 ordinary_optional 长尾（已有机器）
T27 freeze ────────┼─ 轨 B  GT6 30 kind 多方块补完
                    ├─ 轨 C  裂变 / 聚变 / 等离子（新物理）
                    ├─ 轨 D  GT6U 来源与内容（本文）
                    └─ 轨 E  GT6 64 个 DEFERRED map（新机器/空图等）
```

| 容易误判 | 正确归属 |
|---|---|
| 「有多方块通用层就能搬 GT6U 大机」 | 通用层 ∈ 轨 B 基础；GT6U 新 kind ∈ 轨 D |
| 「补完 78k ordinary_optional 等于石油化学完成」 | 那是 GT6 长尾；PVC/裂解/清洗 ∈ 轨 D |
| 「核堆蒸馏水冷却算 GT6U 小功能」 | 物理上属轨 C；来源若只在 GT6U 则 D 提供 dump，C 拥有实现 |
| 「gt6u_code 在就是能 SOURCE_BACKED」 | 只有源码 ≠ dump；无 dump 不得标 SOURCE_BACKED_GT6U |

---

## 5. 建议的轨 D 卡序（规划级，非开卡）

| 卡 | 性质 | 交付 | publication |
|---|---|---|---|
| **D0** | 源获取 | 固定 rev（建议钉 `4972d046…` 或上游 release tag）、license 纪要、`gt6u_dump`、tree/blob/symbol、reference metadata | 0 |
| **D1** | 分母 | RecipeMap / material / fluid / machine / multiblock 差集表；与 GT6 冲突矩阵；disposition 词表扩展 | 0 |
| **D2** | 材料流体纵切 | 聚合物 + 关键中间体 ACTIVE；管/罐投影；最小化学环（如 Vinyl→PVC） | 有界 |
| **D3+** | 按玩家闭环拆 | 例：含硫油清洗→裂解；Epoxid 电路；铬铁矿；Heat Mixer 档位；大型结构分批 | 按载荷门禁 |

**明确不做（除非改契约）**

- 不把 GT6U 全量设为 v1.0 / RC 阻断项  
- 不在 D0 前把 GT6U 行写入 GT6 expected  
- 不宣称「54 个 multiblock Java = 54 个可数据填充结构」

---

## 6. 差距清单（检查用）

### 6.1 D0 前必须为否 → 是

- [ ] 存在可复现的 `gt6u_dump/`（maps + oredict + …）  
- [ ] 存在 GT6U tree/blob/symbol manifest  
- [ ] `gt6u_reference_metadata.json`（rev、dump 工具、MC/Forge、config digest）  
- [ ] 许可证与「只搬新增内容」边界成文  
- [ ] fidelity 词表支持双来源且测试覆盖混写拒绝  

### 6.2 全量搬运仍缺的大块（D0 之后）

- [ ] 材料/流体/前缀 activation 与管道投影  
- [ ] ≥14 个新 RecipeMap 的 host + 行级 disposition  
- [ ] Heat Mixer / Laser Cutter / Incubator / Circuit Assembler 等机器矩阵  
- [ ] GT6U 新多方块 denominator + 逐结构 Java/数据判定  
- [ ] 被 GT6U 改写的既有配方冲突全部裁决  
- [ ] 电路 Epoxid 线、铬铁矿线、生物线、对撞线  
- [ ] 核/聚变/对撞与轨 C 的 owner 划界  
- [ ] worldgen / 成就（或显式 out_of_scope）  
- [ ] 载荷与 GameTest 预算重测  

### 6.3 已具备、可少重复造的

- [x] 材料规则语言与 registration gate  
- [x] RecipeMap 运行时 + Hybrid eager/lazy  
- [x] 机器 kind/tier 模型  
- [x] 多方块 JSON schema / validator / 部分 plugin  
- [x] 流体 cell、管网、cover 骨架  
- [x] 全量验证管线（需扩第二来源，而不是重写）  
- [x] GT6U **源码树**（`gt6u_code/`）  

---

## 7. 文档关系

| 文档 | 角色 |
|---|---|
| 《CrucibleCraft-总体规划.md》 | 不变量与「换内容不改 Java」裁决 |
| 《CrucibleCraft-第五阶段总体规划.md》 | 轨 D 权威边界 |
| 《plans/CrucibleCraft-第五阶段总体规划草稿.md》 | 讨论稿；其中「无 GT6U 源」已过时 |
| 《tools/phase4_v1_planning_contract.json》 | v1.0 不承诺 GT6U |
| 本文 | **GT6U 全量 vs 现基础** 的差距台账 |

T26_READY → T27 之后，应用机器可读契约 `tools/phase5_portfolio_contract.json` 吸收本文的检查项；在此之前本文不改变项目状态。

---

## 8. 附录：GT6U README「What We Added」→ 层映射

| README 条目 | 主要落点 | 当前 CC |
|---|---|---|
| 新流体 / 石油有机化学 | dump fluids + Chem recipes + HeatMixer/Cracking/OilCleaner | 石油主链有界；无 GT6U 增量 |
| 成就页 | achievements → 1.21 advancements（需重设计） | 无 |
| 大型机器列表 | multiblocks + 新 map | 仅 5 个 GT6 结构 |
| 小型机器列表 | machine kinds + tiers | 无对应 kind |
| PVC/PTFE/Epoxid | materials + pipes/tanks + Chem | 无 |
| 铬铁矿处理 | Chem + HeatMixer/BurnMixer | 无 |
| 电路改聚合物 | MultiItemTechnological | 电路线未接 Epoxid |
| 核堆蒸馏水 / 椰子 / 水井 | reactors + worldgen + Well | 无 |

源码索引（便于人工跳转）：

- 流体：`gt6u_code/.../loaders/a/Loader_Fluids.java`  
- 材料：`.../gregapi/data/MT.java` → `class GT6U`  
- 化学：`.../loaders/c/Loader_Recipes_Chem.java`  
- 机器注册：`.../loaders/b/Loader_MultiTileEntities.java`  
- RecipeMap：`.../gregapi/data/RM.java`  
- 成就：`.../loaders/c/Loader_Achievements.java`  
