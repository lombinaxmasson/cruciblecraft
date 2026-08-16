# CrucibleCraft 第四阶段总体规划

> **工作副本，已停止维护进度。** 2026-08-14 起以仓库根目录
> 《[../CrucibleCraft-第四阶段总体规划.md](../CrucibleCraft-第四阶段总体规划.md)》为权威。
> 本文件只保留交接期内的执行草稿，不再同步 T 状态。

> GregTech 6 → Minecraft 1.21.1 NeoForge 移植 · 第四阶段 T20–T26
> 状态：T20 已关闭（`T20_READY`）；T21 进行中（早前写入的 `T21_READY` 为草稿，不作关闭证据）
> 权威起点：2026-08-07，`T19_READY` 与完整验证 `READY`
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 机器可读契约：[tools/phase4_v1_planning_contract.json](tools/phase4_v1_planning_contract.json)

---

## 0. 第四阶段是什么

第四阶段不是“把 95 个 RecipeMap 或 30 个 multiblock 全部填完”，也不是把
720,841 条 source row 换算成完成百分比。它只负责把已经存在的工业架构收束成
一条可以公开测试的产品主链：

> T20–T26 依次关闭世界资源保真、普通化学全量、石油化工全量与纵深、有界多方块、
> 可复现规模证据、实测阻断项与公开 Beta 门禁。

任何时刻最多一个 T 为“进行中”。T20 已在固定 revision、源码 blob、人工锚点
与 129-entry 目标复核后开工；复核同时证明 GT6 固定来源只有 40 条 large、
75 条显式 small 与 1 条动态随机宝石规则，因此 T20 不再把 129 条 CC catalog
伪称为 129 条 canonical GT6 large vein。

### 0.1 产品契约

**公开 Beta 契约**

- 新世界能取得 source-backed 世界资源，并沿矿物加工、能源、石油化工和自动化
进入一个可持续的工业循环；
- T21 的普通化学与 T22 的石油化工中，`v1_required` 全部发布；`ordinary_optional` 属于 1.0 后 portfolio，逐类登记 owner 与 replacement condition。
主链 family、机器和多方块具有真实消费端，不以“只做一个纵切”冒充 Beta 闭包；
- dedicated server、客户端联机、存档升级、构建包、来源署名和关键路径语言资源
可执行；
- 规模结论来自可复现 workload；没有跨机器硬秒数正确性门禁。

**v1.0 契约**

> CrucibleCraft v1.0 表示“GT6 风格工业主链完整且可稳定发布”，不表示 GT6 或
> GT6U 全量移植。

v1.0 RC 只能在工业主链 `v1_required = 0`、closure / fidelity / load 三轴
全部通过、首玩与升级路径可执行、且主链不存在未声明 `PLACEHOLDER` /
`UNVERIFIED` 时编号。

### 0.2 明确不阻断 v1.0

- G10：植物、护甲、弹药、镜片、轨道及其他外围玩法轴；
- 核裂变、聚变、等离子及其专用物理、冷却与末期能源体系；
- GT6U；
- 一次性投影全部 95 个 RecipeMap；
- 一次性实现全部 30 个 canonical multiblock kind；
- “GT6 720,841 行全部移植”这一产品声明。

这些项目进入 1.0 后 portfolio；不得因为还存在而把已满足本契约的 v1.0
改写为“不完整”，也不得反过来把它们混入第四阶段。

---



## 1. 权威起点与计量口径



### 1.1 单一开工快照


| 账目                            | 当前值                                                                        | 权威产物                                                                    | 语义                    |
| ----------------------------- | -------------------------------------------------------------------------- | ----------------------------------------------------------------------- | --------------------- |
| 完整验证                          | `READY`，2026-08-07                                                         | `tools/full_verification_report.json`                                   | 本轮纯规划文档改动前的运行时/数据闭包快照 |
| concrete datapack recipe file | 3,240                                                                      | `tools/full_verification_report.json` 的 `rules.datapack_recipe_entries` | 当前磁盘具体配方资源数           |
| RecipeMap                     | 32                                                                         | `tools/t19_readiness.json`                                              | 当前稳定注册集合              |
| publication                   | 18,875 logical / 16,650 eager / 2,225 lazy                                 | `tools/t19_readiness.json`                                              | 当前 GT recipe 运行时规模    |
| EMI map                       | 24                                                                         | `tools/t19_readiness.json`                                              | 当前客户端枚举集合             |
| 验证集合                          | 538 JUnit / 83 GameTest / 501 Python                                       | 两份 READY 产物                                                             | T20 开工前的预期规模          |
| 本地化                           | 3,167 `en_us` / 366 真实 `zh_cn` / 2,801 visible debt / 1,774 material names | `tools/t19_readiness.json`                                              | O-15 当前账              |
| T13 source 分母                 | 7 表 / 95 maps / 720,841 rows / `unclassified = 0`                          | `tools/t13_denominator_manifest.json`                                   | 来源分类宇宙，不是完成率          |


`3,230` 是总体规划曾记录的 T18 前历史 authored 快照；`3,260` 是 T12/T14
readiness 保留的历史 authored load snapshot；`3,270` 只来自未验证骨架的外推。
三者都不是当前仓库的 concrete recipe file 计数。第四阶段只把 3,240 用作当前
文件数，且每次引用必须同时写出 counter 名、来源产物和快照日期。

历史 readiness 中的数字不回写为“错误”：它们仍描述对应阶段曾使用的计量。
禁止的是把历史 `authored`、当前 `datapack file`、运行时 `logical row` 混写为
同一个“配方数”。

### 1.2 七类 source 分母只用于分类

T13 的 canonical 数量为：

- RecipeMap 95；
- prefix 452；
- ITEMGENERATOR domain 25；
- machine kind 96；
- energy identity 20；
- cover kind 47；
- multiblock kind 30。

`11/96`、`56/452`、`2/30` 等比值不再出现在当前完成度中。它们至多描述某次
实现集合与 source classification universe 的集合关系，不能证明工业主链是否
完整。当前规划只使用以下显式映射：

1. source identity：固定 revision 的 canonical id；
2. CC implementation：注册对象、规则、运行时行为或“无实现”；
3. disposition：`v1_required / post_1_0 / out_of_scope / unclassified`；
4. owner：T 卡或 post-1.0 portfolio；
5. replacement condition 与 recheck point。

T27 必须对七表重新执行该映射并令 `unclassified = 0`。T27 之前不得用旧
`in_scope` 直接等同于 v1 必需。

### 1.3 五种数量不可混写


| 名称                     | 定义                                         |
| ---------------------- | ------------------------------------------ |
| `source_fact`          | 固定 revision 经规范化后的一个 GT6 事实                |
| `authored_rule`        | 一条经审查的 CrucibleCraft 规则声明                  |
| `datapack_file`        | 当前完整验证统计的一份具体 recipe resource              |
| `logical_row`          | 规则展开后的一条完整运行时关系                            |
| `eager_row / lazy_row` | publication 时立即物化 / 按需保留的 logical relation |


同一 family 的报告必须同时给出适用的五种数量；不能用 lazy 余量替代 eager
预算，也不能把 source row 除以 CC logical row 生成“移植百分比”。

---



## 2. 第四阶段顺序

```text
T20 O-29 世界生成保真
  │
  ▼
T21 普通化学全量与规则轴
  │
  ▼
T22 石油化工全量与纵深
  │
  ▼
T23 有界多方块批次
  │
  ▼
T24 可复现规模基线
  │
  ▼
T25 实测阻断修复
  │
  ▼
T26 公开 Beta 门禁
```

T20–T26 不并行。后卡可以在文档中引用前卡接口，但不得提前生成内容、注册对象
或 READY 状态。

每张卡都交三份独立账：

- **closure**：全集分类、注册/获取/消费可达、端到端链路和失败边界；
- **fidelity**：固定来源、声明转换、独立期望集与双向全等；
- **load**：datapack、logical/eager/lazy、reload/index、内存、网络与操作数。

---



## T20 · O-29 世界生成保真

**状态：✅** `T20_READY`**。完整关闭证据见《[CrucibleCraft-阶段档案-T20.md](CrucibleCraft-阶段档案-T20.md)》。**

**一句话判据**

> 从固定 GT6 worldgen 来源规范化 40 large / 75 explicit-small / 1 dynamic
> 规则，再把 129 条 CC catalog identity 全部分类为可复算 `SOURCE_DERIVED`
> 或显式 `DESIGN_POLICY`；独立 expected 与 authored/runtime 字段逐项双向全等，
> `PLACEHOLDER / UNVERIFIED / unclassified = 0`，运行时 registry 能解码并
> 真实放置全部集合，身份保持的 profile v2 边界明确，RecipeMap publication
> 增量为零。



### T20a · 固定来源与规范化器

- 固定 `GT6WorldGenerator / WorldgenOresLarge / WorldgenOresSmall / Loader_Worldgen / MT` 的 revision、tree、blob、SHA-256、提取命令和人工锚点；
- source normalizer 必须观察到 40 large / 75 explicit-small / 1 dynamic rule，
不能从 129-entry CC 目标反推 source 分母；
- normalizer 输出原始材料角色、height、weight/density/size、amount、dimension
list 和 provenance，不读取 CC authored catalog；
- 固定来源 replay 缺失时只能显式 SKIP；blob、锚点、基数或字段分布漂移时 fail
closed。



### T20b · 材料与字段映射

- 每个 source material 通过固定 source id 显式映射到 CC material identity；
- 129 条按 `EXPLICIT_SMALL_SOURCE / RANDOM_SMALL_GEM_SOURCE / UNIQUE_LARGE_ROLE_SOURCE / DESIGN_POLICY_NO_GT6_WORLDGEN_FACT` 唯一分类；
- many-to-one、单位换算、截断、角色替换和默认值都有 transformation id 与可复算
公式；无唯一 GT6 worldgen fact 的行只能显式 `DESIGN_POLICY`，不得进入 GT6
parity 数；
- expected builder 从 T2c ledger 与固定 source evidence 独立推导；生产 builder
只消费显式 authored rows，不共享 selector 或默认值实现。



### T20c · Authored schema 与生成树

- catalog schema v2 能表达逐行 profile、source kind、field status、
transformation、材料角色、几何、密度、高度和分布；
- builder 对输入 id、输出 configured/placed feature、biome modifier 和材料引用
做双向集合检查；
- 129 条现有 `UNIFORM_PLACEHOLDER` 清零；当前固定分类为 73
`SOURCE_DERIVED` + 56 `DESIGN_POLICY`，但该分布由 builder 派生而非手填；
- 输出树 deterministic，删除、额外或 stale 文件都失败。



### T20d · 运行时与存档边界

- 从运行时 registry 解码并实际放置全部 129 条 catalog vein；
- configured/placed feature id 与 salt 保持不变；已生成区块不重写，未探索区块
使用 profile v2；未知 profile version 在 codec 边界失败，不静默降级；
- loot 到现有矿物加工链仍可执行，且不修改 T11 fluid deposit identity；
- 本卡不新增 RecipeMap、GT logical row 或隐式平衡捷径。



### T20e · 三轴收尾

**Closure**

- 40/75/1 source fact、129 catalog id、configured feature、placed feature、
biome modifier 和材料引用集合分别有独立分母并双向相等；
- 全部运行时对象可解码、放置和产出当前注册矿石；
- O-29 owner 唯一转为 T20，关闭后不留第二份 geometry ledger。

**Fidelity**

- 129 条逐字段 expected == actual，少一条、多一条、改一字段都失败；
- `PLACEHOLDER / UNVERIFIED / unclassified = 0`；
- `DESIGN_POLICY` 行不计入 GT6 parity，source-derived NeoForge 几何也不宣称与
1.7.10 placement algorithm 行为全等；
- 错误 expected 变异能使门禁变红。

**Load**

- worldgen resource 数、codec/startup 成本和存档体积增量单独记账；
- GT publication delta = 0；
- 完整验证通过后才生成 `T20_READY`。

**范围之外**

- 新 worldgen 玩法、prospector、植物/农业、地表散矿；
- T9/T11 fluid deposit 衰减策略；
- RecipeMap 或机器批量填充；
- 第三方模组原生 worldgen 的无固定 revision 行为复刻；
- 把 129 条 CC identity 改写成 129 条 canonical GT6 large vein 的对外声明；
- 已生成区块的追溯重写或无损矿体重塑。

---



## T21 · Mixer 模板分母与普通化学规则轴

**状态：🔶 进行中。草稿档案见《[CrucibleCraft-阶段档案-T21.md](CrucibleCraft-阶段档案-T21.md)》；早前写入的 `T21_READY` 未绑定完整验证，不作关闭证据。**

未收尾项（关闭前必须逐项清零）：

1. 独立复核 Beta seed、当前可达 identity 与 template necessity——“仅 1 个 v1 template”可能低估；
2. 审计 ~11 MB template denominator 的 compact artifact 体积与可重建策略；
3. gunpowder load 以隔离运行时实测替换生成器保守数字；
4. 新 GameTest 在 dedicated GameTest server 上执行；
5. 修复 fast Python 的 T16/T17 currentness stale；
6. source-replay、closure 与单次 clean `run_full_verification.py --record` 全部通过后才标记 `T21_READY`。

**一句话判据**

> 保留 224-material 化学轴与 Carbon 电解校准，将“碰到候选材料的 45,044 行”
> 降级为诊断账；以固定 revision 的 64,245 条 Mixer source row 构造
> content-addressed、完整 membership、零差异 replay 的 template 分母，再由
> Beta 反向依赖闭包分类模板必要性。`unclassified = 0`、全部 `v1_required`
> template 已发布、in-scope runtime blocker = 0 后关闭；expanded row 只作为
> fidelity/load 诊断，不再充当完成度。

### T21a · 化学轴与错误指标冻结

- `145 + 110 - 31 = 224` 是唯一材料候选分母；byproduct-only debt 不相加；
- 84 composition / 0 prefix / 70 named / 70 blocked 的 material shape 校准保留；
- coal/charcoal → carbon 纵切继续作为 composition 校准证据；
- `tools/t21_source_denominator.json` 保留为 mapping/rejection 诊断，但
  `45,353 / 45,044 / 2,592 / 42,452` 不再具有 READY/progress 语义。

### T21b · Mixer template replay

- schema-first 分析识别 GT meta material axis，并保证 leave-one-out group 不重复
  消费 source row；
- 64,245 行固定 Mixer source 精确分为 3,414 个 authored unit：
  2,628 material-matrix、103 enumerated、683 opaque；
- 每个 template 使用 content hash id、显式 sparse support 与完整 source-index
  membership；
- 全局和逐模板 canonical multiset replay 均为 0 missing / 0 extra；
- disabled/hidden/fake 参与 fidelity replay，但不自动进入 v1。

### T21c · Template necessity denominator

- Mixer 使用 verified template；其余九张 map 暂以 exact singleton/family unit
  分类；
- 每个 unit 唯一归入 `v1_required / ordinary_optional / already_covered /
  petroleum_t22 / post_1_0_nuclear / post_1_0_g10 / out_of_scope`；
- T5 已覆盖行与未覆盖行混在同一 template 时先拆分，不允许 partial coverage；
- Beta seed 与当前可获取 identity 形成反向依赖闭包，每个 v1 unit 有产品/消费者
  依据；
- 当前 denominator 为 93,133 units，`unclassified = 0`；唯一 v1-required unit
  是 4-row gunpowder Mixer family。

### T21d · 发布与三轴收尾

**Closure**

- 唯一 v1-required Mixer template 已发布 4/4 rows，remaining = 0；
- Carbon 校准和 gunpowder family 均通过 live machine GameTest；
- in-scope runtime blocker = 0，下一步转入 T22。

**Fidelity**

- Mixer 64,245 expected == replay，membership unassigned/duplicate = 0；
- gunpowder independent expected == template projection == runtime；
- amount、chance、duration、EU/t、specialValue、buffering 与 source provenance
  全部保留，support/membership 变异会使门禁失败。

**Load**

- gunpowder family：4 source facts / 1 authored rule / 4 datapack files /
  4 logical / 4 eager / 0 lazy；
- immediate family load projection 为 PASS；global publication 增量
  logical/eager/lazy = 4/4/0；
- construction-foam mega-family 分类为非 v1，不进入 eager publication。

**范围之外**

- 45,044 input-touch rows作为关闭分母；
- ordinary_optional 的批量发布；
- 石油 family（T22）；
- 核化学、G10 和其他 post-1.0 subsystem。

---



## T22 · 石油化工全量与纵深

**一句话判据**

> 从固定 revision 构造石油化工 source-row 全集，按 family 依次发布全部非
> post-1.0 行，使 petroleum `v1_required = 0`，并使原油/天然气至少进入三种
> 具有工业用途的下游产物；每个 family 证明 source 全量等价并用使用分布决定
> immediate / on-demand / Hybrid。



### T22a · 全集与 family 顺序

- 从 T21 分类与 T11 已关闭 identity 出发，不另扫一套私有候选；
- 从固定 source 生成 petroleum exact denominator；每个 family 在开工前写明
  source row 全集、CC material/fluid 映射、执行机器、
energy identity 和至少一个真实消费端；
- 缺 identity、机器、能源或存储/容器能力的 v1 petroleum family 在 T22 内补齐，
  不得因前置不全留作无 owner deferred；
- 只有明确依赖核能/聚变/等离子、G10 或其他 post-1.0 subsystem 的行可以排除，
  且必须保留逐行 disposition。



### T22b · 全量投影

- 对选中 family 的 source row 做独立 expected ↔ authored/expanded 全等；
- 保留 exact input/output amount、container action、chance、duration、EU/t、
special value、顺序和 fallback；
- 身份桥只允许 source-backed/source-derived 或已登记的 DESIGN_POLICY；
- 高频主链反应不能因为 lazy 总预算充足而默认放入 lazy。



### T22c · 玩家循环

- 原油/天然气入口复用 T9/T11 已关闭 identity 和抽取语义；
- 至少三种下游产物各自连接机器、物流、燃料、润滑、聚合物前体或其他实际工业
consumer；
- dedicated server 上验证输入获取、连续处理、容器返回、管道运输和消费；
- 失败、断电、输出堵塞、reload 与存档恢复保持物质和能量守恒。



### T22d · 三轴收尾

**Closure**

- petroleum source-row 全集 `unclassified = 0`，全部非 post-1.0 行已发布，
  petroleum `v1_required = 0`；
- 每个发布 family 的 identity / machine / energy / storage 与关键 consumer 闭合；
- 三种下游产物在首玩或自动化路径中被真实消费；
- 所有排除行都有 post-1.0/out-of-scope disposition，不以抽样代表全集。

**Fidelity**

- 全部发布 source rows 按 family 双向等价；
- T18 的 Raw Oil DESIGN_POLICY 与 T11 `crude_oil` / `natural_gas` 身份边界不被
偷换为 source direct binding；
- 任何 CC 平衡调整都单独标记 DESIGN_POLICY。

**Load**

- 每 family 有独立 load projection 和策略依据；
- 高频路径 lookup candidate、transaction allocation 和 sync 进入测量；
- 全局 publication、reload/index 和 lazy cache 仍在 hard gate 内。

**范围之外**

- fusion/plasma/nuclear fuel 及其专用反应；
- G10 或第三方扩展化学；
- 没有当前消费端的化学品陈列。

---



## T23 · 有界多方块批次

**一句话判据**

> 先对 30 个 canonical multiblock kind 建立行为需求分类，再只实现 2–3 个服务
> v1 工业主链的代表结构；结构继续数据化，controller 行为只通过受控 plugin
> 扩展，任何通用抽象必须由至少两个真实消费者证明。



### T23a · 行为需求分类

每个 canonical kind 至少记录：

- source identity 与结构 family；
- 是否能使用现有 schema；
- 是否需要 aisle/repeat、动态端口、环境约束或专用 controller behavior；
- 所需 RecipeMap、energy identity、输入获取和产品 consumer；
- `v1_required / post_1_0 / out_of_scope / unclassified` 及理由。

完成分类后才选择 2–3 个结构。不得先写通用 controller，再寻找使用者。

### T23b · Schema 最小扩展

- 只有选中结构的 source geometry 无法表达时才增加 aisle/repeat 等 schema；
- schema、loader、validator、preview/debug 输出使用同一语义；
- 未知 predicate、plugin、orientation 或 port role fail closed；
- 继续禁止每个结构一个专用 `*Structure` Java parser。



### T23c · Controller plugin 边界

- 结构数据只描述值与约束，事务、UI、环境和特殊机器行为由显式白名单 plugin
承担；
- plugin id 必须稳定、可持久化、可 quarantine，并由至少一个 selected controller
消费；
- 泛型 processing controller 只有在两个真实 selected consumer 共享相同行为后
才能抽取；
- port 到 shared host 的供应量不能按物理方块数重复计算。



### T23d · 三轴收尾

**Closure**

- 30 kind 行为需求 `unclassified = 0`；
- selected 2–3 结构具有方块/物品/模型/语言/loot/recipe 获取与真实产品消费；
- formation、拆除、堵塞、断能、reload、存档和升级路径可执行。

**Fidelity**

- selected source geometry/predicate/port 逐项双向等价；
- CC plugin 行为与 source 行为或 DESIGN_POLICY 边界明确；
- 一个结构通过不能代表其他 29 个结构。

**Load**

- structure validation、port scanning、network sync 与 ticking operation count 有界；
- 不把 chunk/layout 变化造成的扫描成本隐藏在 wall-clock 平均值中；
- 新增 recipe/map publication 单独按 family 记账。

**范围之外**

- 全 30 kind 实现；
- 核裂变/聚变/等离子 controller；
- 动态合并渲染和全结构预览 GUI，除非 selected 结构的可操作性确实阻断 Beta。

---



## T24 · 可复现规模基线

**一句话判据**

> 用确定 seed、版本与场景 manifest 生成 small / target / stress 三种声明规模的
> workload 或存档；CI 锁定结构、功能守恒和操作计数，墙钟、JFR、网络与内存只在
> 声明硬件、JVM 和版本的测量环境内比较。



### T24a · 三种 workload

- `small`：最小完整工业主链，用于快速复现与功能回归；
- `target`：Beta 预期持续运行规模，用于发布判断；
- `stress`：放大同一拓扑和工作分布，用于暴露非线性，不宣称是玩家常态。

每个 workload 固定 seed、维度/chunk、机器/管道/cover/多方块数量、配方/物流
分布、运行 ticks、warmup、采样窗口和成功摘要。生成器输出 manifest；禁止手工
维护一个无法重建的“黄金存档”。

### T24b · 正确性与性能证据分离

证据只使用三类：

- `STATIC_INFERENCE`：从算法边界和场景 manifest 推导；
- `SYNTHETIC_BENCHMARK`：隔离组件或受控 workload；
- `MEASURED_AT_SCALE`：声明环境中的完整目标/压力场景。

CI hard gate 只锁：

- 场景结构和 deterministic identity；
- 输入/输出/能量/流体守恒；
- 每 tick 或每操作的有界计数；
- cache、queue、route、candidate 等声明上限。

跨机器 wall time 不作为正确性门禁。比较 wall time/JFR/retained memory/network
前必须记录硬件、OS、JVM、mod/dependency 版本、manifest、warmup 和采样协议。

### T24c · 三轴收尾

**Closure**

- 三个场景都可从空目录重建并运行到确定摘要；
- dedicated server 与客户端观测使用同一 workload identity；
- 故意删除/修改场景对象时 currentness 或结构门禁失败。

**Fidelity**

- workload 的 recipe/物流/能源分布来自 T20–T23 实际运行集合；
- synthetic 结果不得标记为 measured-at-scale；
- 无法采集的指标显式 SKIP 并阻断相应结论，不伪装为零。

**Load**

- 产出 reload/index、lookup、tick operation、allocation、retained、network 和
save-size 证据；
- 为每个发现记录是否阻断 Beta、证据种类、可复现命令和失效边界；
- 只把已测 blocker 交给 T25。

---



## T25 · 实测阻断修复

**一句话判据**

> 只修复 T24 以可复现证据证明会阻断公开 Beta 的热点；每项具有前后测量、
> 回归测试和失效边界。若没有 blocker，则以 selected = 0 且全部 disposition
> 完整关闭，不进行预防性重写。



### 执行规则

1. 输入集合只能来自 T24 finding ledger。
2. `slow_but_non_blocking`、测量噪声和跨机器不可比数据不得升级为 selected。
3. 每个 selected finding 独占一个有界修复单元；不能借机改写相邻系统。
4. 修复前后使用同一 workload identity、环境和采样协议。
5. 正确性、守恒、cache invalidation、reload 原子性和存档兼容回归必须先绿，
  性能改善才可计入。



### 三轴收尾

**Closure**

- 每个 T24 finding 为 `selected_fixed / non_blocking / invalid_measurement / post_1_0` 之一；
- selected 全部关闭或明确证明不再阻断 Beta；
- 无 selected 时允许零内容关闭。

**Fidelity**

- 优化不改变 source-backed 输入输出、时长、能源、机会或顺序；
- cache/批处理不跨 epoch 使用 stale 结果；
- 近似或降级策略必须是显式 DESIGN_POLICY，不能伪装成等价。

**Load**

- 同协议前后证据和置信边界完整；
- target 与 stress workload 均复测；
- 不新增跨机器硬秒数 CI。

---



## T26 · 公开 Beta 门禁

**一句话判据**

> 从新世界和受支持旧存档分别完成世界资源→工业阶梯→石油化工→自动化首玩路径，
> 并通过 dedicated server、客户端联机、升级/quarantine、构建包、版本说明、
> 许可证/来源署名及关键路径本地化门禁，产出可公开安装的 Beta。



### T26a · 玩家路径

- fresh world 按文档取得并处理 T20 世界资源；
- 从早期热力/蒸汽进入已支持能源与机器阶梯；
- 执行 T22 的三种下游产品与至少一个自动化循环；
- 形成、运行并维护 T23 selected multiblock；
- 无 creative/command-only、未声明捷径或开发者强制 tick。



### T26b · 服务器、客户端与存档

- dedicated server 启动、双客户端连接、断线重连和资源同步；
- 支持的旧存档升级后保留机器 identity、库存、流体、能量和结构状态；
- 未知未来版本、未知 plugin/identity 与不兼容数据 quarantine，可加载、可诊断，
不静默删除玩家资源；
- protocol、fingerprint 和 datapack mismatch 明确拒绝。



### T26c · 发布包

- clean checkout 可复现构建发布 artifact；
- 版本号、changelog、安装/升级说明、已知问题和支持范围一致；
- 固定 GT6/GTM 来源、许可证与第三方署名完整；
- 不把 G10、nuclear/fusion/plasma 或 GT6U 写入 v1 阻断清单。



### T26d · 本地化与模型

- `en_us` 对 v1 玩家可见域完整；
- v1 关键路径的玩家可见文本在目标语言域内完整；
- `zh_cn` 按 material name、machine/block/item、GUI/status、guide/error 等可见域
分账和设定目标，不使用任意总百分比；
- 禁止复制英文值伪装翻译；无法纳入 v1 的长尾必须显式 `post_1_0`；
- 新增对象无 placeholder model/texture。

O-15 在本卡按玩家可见域重分类：v1 关键域清零、非 v1 长尾具有明确 disposition
后才可关闭，不能以“覆盖率不下降”冒充关闭。

### T26e · 三轴收尾

**Closure**

- fresh-world、multiplayer、upgrade 与 packaging checklist 全绿；
- known issue 均有 severity、workaround、owner 和 release disposition；
- 公开 artifact 能按说明安装并达到同一版本/fingerprint。

**Fidelity**

- 主链无未声明 PLACEHOLDER/UNVERIFIED；
- 玩家文档区分 SOURCE_BACKED、SOURCE_DERIVED 和 DESIGN_POLICY；
- 来源与许可证可从发行包追溯。

**Load**

- T24 target workload 在 Beta candidate 上复跑；
- T25 blocker 为零；
- 构建包、同步、reload/index、内存和存档大小均有当前 release evidence。

**关闭信号**

- `T26_READY`；
- 第四阶段状态为“公开 Beta 已交付”；
- 当前执行入口转为 T27 portfolio freeze，而不是预先虚构 T28+ 数量。

---



## T27 · v1.0 portfolio freeze（最终阶段入口）

T27 不属于第四阶段内容实现；它是公开 Beta 之后的最终阶段第一张卡。现在只固定
接口和退出条件，不预先决定 T28+ 的数量。

**一句话判据**

> 重新消费七类 T13 denominator、T20–T26 的 READY/测量结果、全部 deferred 与
> open item，为每个 source identity 建立唯一 CC implementation 和
> `v1_required / post_1_0 / out_of_scope` disposition，使 `unclassified = 0`；
> 然后只为实际剩余的 v1_required 项生成连续、单 family/单物理/单玩家循环卡。



### T27a · 输入全集

T27 的输入必须同时包含：

- `tools/t13_denominator_manifest.json` 的七张 canonical 表；
- T20–T26 的 current readiness、load projection、workload 与 release evidence；
- 所有 `deferred_with_reason`、`blocked_by_new_subsystem` 和 open item；
- 当前注册 RecipeMap、MachineKind/Tier、energy、cover、multiblock 与
ITEMGENERATOR implementation identity；
- 当前玩家可见本地化/模型、存档迁移、协议和发布债。

不能只消费“第三阶段 in_scope”，也不能只扫描当前 CC 已实现对象。T13 的
`in_scope` 是历史 source 分类，不自动等于 `v1_required`。

### T27b · 每项记录

每个 canonical identity 至少记录：

- denominator kind 与 canonical id；
- source revision、artifact 和稳定摘要；
- 当前 CC implementation id，或明确 `none`；
- disposition；
- 玩家/工业主链理由；
- owner；
- dependency；
- replacement condition；
- recheck point；
- closure / fidelity / load 当前状态；
- 若为 `post_1_0`，v1 中的替代路径或“不需要替代”的理由。

同一 identity 只能有一个 current owner。聚合统计从这些行派生，不手填
`11/96`、`56/452`、`2/30` 一类比例。

### T27c · 动态生成 T28+

只对 `v1_required` 生成后续卡，并遵守：

1. 一张卡只关闭一个 recipe family、一个新物理/行为边界或一个玩家循环；
2. 卡片引用确定 identity 集，不以“广度补齐”“剩余机器全部完成”命名；
3. 有依赖则按拓扑排序，仍然一次只激活一个 T；
4. 每卡独立给 closure / fidelity / load 和范围之外；
5. `blocked_by_new_subsystem` 只有在该 subsystem 本身被判为 v1_required 时才生成卡；
6. 后续发现新 identity 时先回到 portfolio 分类，不插入未登记工作。

T28+ 的数量、名称和估时在 T27 分类完成前保持未知。禁止为了路线图看起来完整
而伪造卡数或把多个未知 subsystem 压成一张巨卡。

### T27d · RC 编号条件

所有 generated v1-required 卡关闭后，才允许生成 RC 卡。RC 至少重新确认：

- 工业主链 `v1_required = 0`；
- O-36 hot-ingot cooling 的来源/行为 fidelity 已由 generated card 关闭；
- v1 关键本地化、模型与玩家文档完整；
- 存档迁移、客户端/服务端协议与 quarantine 路径通过；
- T24 workload 与任何 T25 hard blocker 在 RC candidate 上复测；
- 构建包、版本、changelog、许可证、来源署名和已知问题清单完整；
- 主链不存在未声明 `PLACEHOLDER / UNVERIFIED`。

`post_1_0` 可以非零，但每项必须有 owner、reason、replacement condition 与
recheck point。G10、nuclear/fusion/plasma 和 GT6U 默认保持 post-1.0，除非
未来产品契约经过显式变更；它们当前不阻断 RC。

### T27 三轴收尾

**Closure**

- 七表及 deferred/open-item union 全部有唯一记录；
- `unclassified = 0`；
- generated card 列表可由机器可读 portfolio 确定性生成。

**Fidelity**

- source identity 到 CC implementation 的映射有固定来源；
- “无实现”不会被当前同名对象或近似行为伪装成实现；
- DESIGN_POLICY 与 GT6 直接事实分开。

**Load**

- 每个 v1_required 项都有初始 publication/workload 影响分类；
- 无法估算的新 subsystem 先生成测量卡，不伪造精确成本；
- T28+ 不继承未证明的全局 lazy/eager 余量。

**T27 关闭信号**

- `unclassified = 0`；
- T28+ 顺序由实际 `v1_required` 集合生成；
- 本卡不实现 family 内容，也不直接编号 RC。

---



## 3. 最终 v1.0 完成信号

v1.0 不是 source 行数或 canonical 比例达到某个百分比，而是以下条件同时成立：

1. 工业主链 `v1_required = 0`；
2. closure / fidelity / load 三轴全部通过；
3. fresh-world、multiplayer 与支持的 upgrade path 可执行；
4. 无未声明 `PLACEHOLDER / UNVERIFIED`；
5. 性能 hard blocker 为零；
6. 发布包、来源/许可证、关键本地化/模型和已知问题清单完整；
7. 所有 post-1.0 项保持可追踪，但不被误写成 v1 已实现。

任何对外文本都不得把这组信号改写为“GT6 720,841 行全部移植”。

---



## 4. 第四阶段统一门禁

每张卡收尾除总体规划第 8 节外，还必须满足：

- [ ] 当前只有一张 active T；
- [ ] closure / fidelity / load 三轴分别给出状态和证据；
- [ ] source identity、CC implementation、disposition、owner 可机器读取；
- [ ] source facts、authored rules、datapack files、logical/eager/lazy 未混写；
- [ ] 可投影全集与独立 expected 双向相等；
- [ ] 未声明字段压平、额外输出和 stale artifact 都会失败；
- [ ] blocked/deferred 项具有 reason、replacement condition 与 recheck point；
- [ ] 新增注册对象的 `en_us`、模型和真实 `zh_cn` 债按域落账；
- [ ] publication 与 reload/index/memory/network 分别记账；
- [ ] 完整 verification 只绑定一次 READY；
- [ ] raw/source replay 缺失时为显式 SKIP，不冒充 PASS。

参考骨架《[CrucibleCraft-第四阶段总体规划-骨架.md](CrucibleCraft-第四阶段总体规划-骨架.md)》
只保留为规划讨论材料；其中百分比、3,270 authored 外推、并行阶段和全量 A/B
多方块安排均不具有权威性。