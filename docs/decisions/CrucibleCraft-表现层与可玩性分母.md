# CrucibleCraft 表现层与可玩性分母

> 补丁文档：给现有七表分母体系加上第八、第九张表
> 日期：2026-08-18 · 基于 8-18 工作树快照实测
> 状态：**规划提案，非开工授权**。当前唯一 active T 是 T27
> 定位：不改产品契约，不改三本账，不改"一次一张 T"；只补一个从来没有仪表盘的轴

---

## 0. 一句话诊断

> **项目对「来源」有一套状态词表，对「表现」没有。**

`SOURCE_BACKED / SOURCE_DERIVED / DESIGN_POLICY / PLACEHOLDER / UNVERIFIED` 让每一条
配方事实都必须自报出身。而一个用通用底纹糊出来的坩埚，在账本上和一台精心绘制的
离心机**完全等价**——因为表现层没有任何状态字段可以填。

于是产生一个可证的结论：**`v1_required = 0` 可以为真，而 mod 从第一分钟就玩不了。**
这不是记账出错，是没有度量它的表。

---

## 1. 三类缺口，三种病，现有门禁对每一种都亮绿灯

### 1.1 表现占位（`ART_PLACEHOLDER`）

实测：**7 个语义无关的对象共用同一张 `block/metal_surface`**

```
anvil · crucible · bath · mortar · smelter · sifter · bronze_steam_engine
```

`firebox` 走 `cubeAll(firebrick)`，即整块砖纹立方体，无专属纹理。

`crucible.json` / `anvil.json` 都是手写几何（`parent: null`，有 elements），但
`textures` 只有 `{"all": "metal_surface", "particle": "metal_surface"}`——**几何做了，
六个面全是同一张灰色金属底纹**。

> **为什么现有检查抓不到**：文件全都在，引用全都解析得通。任何"资产完备性"
> 静态检查都会绿。这正是这个盲区能活到今天的原因——**它不是缺失，是占位**。

### 1.2 生存不可达（`UNREACHABLE`）

石子块（`RockBlock` / `ROCK_BLOCKS`）：**注册了、有专属几何模型 `material_rock.json`、
有专属纹理 `block/rock`、支持按材质 tint**。资产层面完全健康。

但 `worldgen_catalog/` 只有两个文件：

```
ore_veins.json      129 条（ore.large / ore.small）
fluid_deposits.json
```

**没有地表散布。** 石子在世界里从不生成，也没有合成路径（GT6 里它本来就是起点物）。
于是：石子不可得 → 燧石工具不可得 → 锤子不可得 → **整条开局链断在第 0 步**。

> **为什么现有检查抓不到**：G1 类资产检查绿，T20 worldgen readiness 也绿——因为
> T20 的交付边界就是矿脉，它没撒谎。缺的是**没有人负责"注册对象是否可获得"这件事**。

### 1.3 分母漏检（`UNOWNED`）

T13c 从 GT6 的 `Loader_MultiTileEntities` 扫出 2,328 条展开注册，其中
**763 个 call site / 1,701 条展开注册被排除**，理由字段是
`outside_fixed_machine_energy_behavior_tree`。

这个排除**在方法论上是正确的**——T13c 的交付边界写明是
`T13C_MACHINE_AND_ENERGY_DENOMINATORS_ONLY`，料斗确实不是机器/能源行为。

问题是：**排除之后没有任何一张表接收它们。**

| category | call sites | expanded |
|---|---:|---:|
| Storage | 28 | **624** |
| Panels | 6 | **348** |
| Molds | 117 | 117 |
| Multiblock Machines | 117 | 117 |
| Fluid Containers | 86 | 86 |
| Misc Tool Blocks | 86 | 86 |
| Reactors | 46 | 46 |
| Smelting Crucibles | 39 | 39 |
| Crucibles Faucets | 39 | 39 |
| Batteries | 37 | 37 |
| Sensors | 21 | 21 |
| Portals | 19 | 19 |
| Untyped | 18 | 18 |
| Lasers / Automatic Tools | 10 / 10 | 10 / 10 |
| Extenders / Redstone Wires / Ropes | 8 / 6 / 6 | 8 / 6 / 6 |
| Heaters / Engines / Motors / Magnets / Dynamos / Coolers | 5 各 | 5 各 |
| **Logistics / Pumps / Sorting** | 4 各 | 4 各 |
| Portable Power Cells | 4 | 4 |
| Chests / Safes / Crafting Tables | 3 / 2 / 2 | 同 |
| **Hoppers** | **2** | **2** |
| Long Distance Transport / Computing | 2 / 2 | 同 |
| Scaffolds / ZPM / Laser Wires / C-Foam / Coins | 1 各 | 1 各 |
| **合计** | **763** | **1,701** |

**`machine_kinds` 是七表里唯一 `out_of_scope = 0` 的表**（96 = 23 v1 + 73 post_1_0）。
不是因为没有范围外的东西，是因为范围外的东西**在分类之前就被排除了**，所以永远不会
出现在 `unclassified` 里。

> **为什么现有检查抓不到**：`unclassified = 0` 是对 765 项算的，而这 1,701 条从来不在
> 那 765 里。料斗、物品桶（Fluid Containers 86）、模具（Molds 117）、坩埚龙头
> （Crucibles Faucets 39）——全部零 disposition、零 owner、零 recheck point。

---

## 2. 两张新表

### 2.1 第八表 `presentation_slots` — 表现层

**关键区别：分母从 CC 自己的注册表派生，不从 GT6 派生。**

这解释了它为什么一直不在账上：七表全部是 GT6 源的投影，而表现层**没有 GT6 源可投影**
（贴图不能搬，许可证不允许）。一个没有源的轴，在这个方法论里就没有分母。

**行** = 每个注册对象（block id / item id / fluid id / 多方块结构 id）
**列** = 表现槽位

| 槽位 | 判据 |
|---|---|
| `blockstate` | 存在且解析 |
| `model` | 存在，parent 链闭合 |
| `texture` | 全部引用路径存在 |
| `art_status` | 见下 |
| `lang_en` / `lang_zh` | 键存在（复用 O-15 分账口径） |
| `acquisition` | 生存可达（见 G3） |
| `creative_visibility` | 在某创造栏页，或显式 `hidden_by_design` |

**表现状态词表（与来源词表平行）**

| 状态 | 含义 |
|---|---|
| `ART_AUTHORED` | 为该对象专门绘制/建模 |
| `ART_DERIVED` | 从声明的美术族系统性派生（机器外壳 + overlay、管道按材质着色）——**合法**，但必须声明 family 与派生规则 |
| `ART_PLACEHOLDER` | 用通用底纹或临时几何顶着 ← anvil / crucible / firebox 当前状态 |
| `ART_MISSING` | 引用链断裂或未注册模型 |
| `ART_INTENTIONAL_VANILLA` | 故意沿用原版外观，需理由 |

**规则**：`disposition = v1_required` 的对象不得停留在 `ART_PLACEHOLDER` 或 `ART_MISSING`。

### 2.2 第九表 `excluded_object_reclaim` — 被排除对象回收

**行** = T13c 的 763 条 exclusion（保留 category / behavior_class / source_identity / multiplicity）
**列** = 与七表相同的 `disposition / owner / reason / replacement_condition / recheck_point`

**唯一新规则**：这一次 `out_of_scope` 必须**写进表里**，不得再用"排除"处理。
排除是分类的对立面；一个对象要么有 disposition，要么就是 `unclassified`。

---

## 3. `v1_required` 怎么判——可复算，不拍脑袋

不要人工挑"哪些是玩家第一小时看得见的"。用**分阶段可达性**从配方图派生：

```
S0 = 原版可获得物 ∪ worldgen 放置物 ∪ 破坏掉落
S(k+1) = S(k) ∪ { 所有输入 ⊆ S(k) 的配方产物 }
```

- `first_hour` = S0…S3（或按实际链长标定的 N）内首次出现的对象
- 这个集合是**输入的确定性函数**，任何人重跑得到同一个集合
- 石子进 S0 之后，整条开局链的对象会自动落进 first_hour；石子不在 S0 时，
  下游全部落进"不可达"，G3 直接红

`first_hour` 集合默认 `v1_required`。其余按现有 portfolio 规则分类。

---

## 4. 测试方法（本文档的核心）

七道门禁。每道都写明：**测什么 / 实现在哪层 / 失败形状 / 为什么现有门禁抓不到**。

### G1 · 引用链闭合

- **测**：`注册 id → blockstate → model(递归 parent) → texture PNG` 全链解析
- **层**：Python，构建后（**必须在 datagen 之后跑**——多数 blockstate 由
  `ModBlockStateProvider` 生成到 `src/generated/resources`，只查 `src/main/resources` 会大量误报）
- **失败形状**：`ASSET_CHAIN_BROKEN: cruciblecraft:xxx → model/parent/texture`
- **现状实测**：44 个被引用的纹理路径，**0 断裂**。这道门禁今天全绿
- **价值**：纯防倒退。它抓不到现存问题，但能保证不再产生新的

> 实现注意：**不要用"同名文件是否存在"做判据**。我用同名法测物品侧得到 85 个"缺失"，
> 其中绝大多数是方块物品复用方块模型的正常情况。同名法会淹没在假阳性里。

### G2 · 占位检测（抓 1.1 类）

- **测**：从模型引用图**反向**建 `纹理 → 使用它的对象集合`。集合 size > 1 且这些对象
  不属于同一个已声明的美术 family → 判 `ART_PLACEHOLDER`
- **层**：Python
- **失败形状**：
  ```
  SHARED_TEXTURE_WITHOUT_FAMILY: block/metal_surface
    used by: anvil, crucible, bath, mortar, smelter, sifter, bronze_steam_engine
    declared family: none
  ```
- **现状实测**：立刻抓到上面这 7 个 + firebox
- **设计要点**：合法共享必须**显式声明**，写法与 `DESIGN_POLICY` 一致——
  声明 family id、成员集合、派生规则、理由。管道按材质着色是合法族；
  砧和坩埚共用一张灰皮不是
- **为什么现有门禁抓不到**：文件都在，链都通，G1 全绿

### G3 · 生存可达性（抓 1.2 类，威力最大）

- **测**：按 §3 的 S0/S(k) 迭代求闭包，检查每个 `v1_required` 对象是否可达
- **边的来源**：datapack recipe（当前 3,243 条）+ MaterialRule 展开 + 熔炼 +
  破坏掉落表 + worldgen 放置
- **层**：Python（纯图算法，无需启动游戏）
- **失败形状**：
  ```
  UNREACHABLE_IN_SURVIVAL: cruciblecraft:rock/*
    no recipe produces it; no worldgen feature places it; no loot table drops it
    downstream blocked: <自动列出依赖它的全部对象>
  ```
- **现状预期**：石子及其全部下游会红，一次性暴露开局断裂
- **为什么现有门禁抓不到**：资产健康、T20 readiness 绿（T20 的边界就是矿脉，
  它没有撒谎）。缺的是"注册对象是否可获得"这件事**没有 owner**

### G4 · 世界放置闭合

- **测**：每个标注 `acquisition = worldgen` 的对象，必须出现在某个
  `worldgen_catalog/*.json` 中
- **层**：Python
- **失败形状**：`WORLDGEN_SLOT_EMPTY: rock scatter declared but no catalog entry`
- **现状**：catalog 只有 `ore_veins.json` + `fluid_deposits.json`，地表散布槽位为空
- **来源锚点**：GT6 worldgen dump 里的 `indicator_rocks_expression` 可作为
  `SOURCE_BACKED` 起点（注意它原义是矿脉指示岩，与开局散布是否同一机制需 R 级取证确认，
  不得直接当等价物）

### G5 · 开局顺序 GameTest（G3 的时序加强版）

- **测**：不只是"最终可达"，而是"在**还没有电、还没有任何机器**时能否做出第一台机器"
- **实现**：不模拟玩家操作。做成**分阶段执行器**——第 k 步只允许消费 S(k-1) 里的东西，
  逐步推进到第一台可运行机器，每步断言输入齐备
- **层**：GameTest（需要真实方块放置与交互：砧敲击、火箱点火、坩埚浇注）
- **失败形状**：`ONBOARDING_STALLED_AT_STEP_k: required <x> not in S(k-1)`
- **为什么必须是 GameTest 而不是 Python**：交互型步骤（敲砧、点火）不在配方图里

### G6 · 创造栏与 EMI 可见性

- **测**：每个注册对象要么在某创造栏页可达，要么显式 `hidden_by_design` 并给理由
- **层**：JUnit（注册表快照）
- **失败形状**：`INVISIBLE_REGISTERED_OBJECT: cruciblecraft:xxx`
- **写法**：复用现有 `emi_recipe_map_ids` 契约的双向相等写法

### G7 · 防倒退双向相等（最重要的一条）

- **测**：`注册对象集合` ↔ `presentation_slots 行集合`，**双向相等**
- **层**：JUnit
- **失败形状**：
  ```
  SLOT_TABLE_DESYNC
    registered but no slot row: <...>
    slot row but not registered: <...>
  ```
- **价值**：这条保证**盲区不会再次产生**。任何人新增一个注册对象而不填表 → 红。
  没有这条，前六条门禁修完的东西会慢慢重新腐化

---

## 5. 载荷记账

表现层不产生 GT `logical/eager/lazy` 行，但它**不是零成本**。接到已有的 T14 独立轴：

| 轴 | 现有 | 表现层贡献 |
|---|---|---|
| `client_reload_ms` | soft 5,000 / hard 10,000 | 模型与图集加载 |
| `client_index_ms` | soft 1,000 / hard 3,000 | 同 |
| `retained_memory_bytes` | hard 512 MiB | 图集常驻 |
| `sync_bytes` | hard 64 MiB | 不受影响（客户端资源不走同步） |
| **`texture_atlas_bytes`（新增）** | 待实测 | 733 张 PNG 当前的图集占用 |

**新轴必须实测后才能定 soft/hard**，未测期间是 `BLOCKED_PENDING_MEASUREMENT`，
不填猜测值——与 T14 协议一致。

---

## 6. 怎么把「零零碎碎」变成有界卡

不按对象拆卡（会拆出几十张），也不做成一张"美术全补完"的巨卡（T27c 明令禁止）。

**按可达性阶段拆**，因为阶段是 §3 的确定性函数，不是人工分组：

| 卡 | identity 集合 | 边界 |
|---|---|---|
| **P0** | 无（纯工具卡） | 建两张表 + 七道门禁，跑一次全量，产出**当前红项清单**。publication delta = 0 |
| **P1** | `first_hour` 集合（S0…S3） | 石子 worldgen + anvil/crucible/firebox 脱离占位 + 该阶段全部对象可达 |
| **P2** | 蒸汽时代（S4…Sk） | 按 P0 实测结果定 |
| **P3+** | 由 P0 的红项清单生成 | 同 T27 的生成规则 |

**P0 必须先做，且必须零内容。** 理由和 T22.5 / T27 一样：在知道红项有多少之前，
任何内容卡的范围都是猜的。P0 跑完才知道 P1 到底是 8 个对象还是 80 个。

**美术族是横切维度，不是卡的边界。** 你之前那份 CC 美术计划（多重线缆改原版贴图那类）
应该作为 `ART_DERIVED` 的 **family 声明**进入第八表，而不是单独一条工作线——
这样它就自动被 G2 承认为合法共享，而不是被判占位。

---

## 7. 与现有体系的接口

**T27 portfolio**：`tables` 从 7 张扩到 9 张。`unclassified = 0` 的含义随之扩大，
覆盖 1,701 条被排除对象与全部注册对象的表现槽位。

**RC 条件**新增一条：

> `first_hour` 集合中不存在 `ART_PLACEHOLDER` / `ART_MISSING` / `UNREACHABLE`。

**`v1_required` 的对外表述**（承接上一轮建议）：凡出现 `v1_required = 0` 的地方，
必须同时打印分母三元组，例如 `765 项：147 v1 / 463 post-1.0 / 157 out-of-scope`。
两个数一起出现，就没人会把它读成"全部完成"。

**执行时机**：P0 属于 v1 问题，不是 1.x 轨。建议排在 T28 / T29 之后、RC 之前。
它可能是**唯一真正值得在 RC 前插的东西**——漏斗还能用现有 9 个 cover
（conveyor / robot_arm / retriever_item / pump / filter）绕过去，
没有石子的开局绕不过去。

---

## 8. 首批实测清单（2026-08-18 工作树，P0 复核）

| 项 | 实测 | 门禁 |
|---|---|---|
| 共用 `block/metal_surface` 的对象 | 7（anvil / crucible / bath / mortar / smelter / sifter / bronze_steam_engine） | G2 |
| `firebox` | `cubeAll(firebrick)`，无专属纹理 | G2 |
| 模型引用的纹理路径 | 44，**0 断裂** | G1 全绿 |
| 纹理总量 | 733 张（block 468 / item 176 / gui 89） | 载荷轴待测 |
| 石子块 | 注册 ✓ 模型 ✓ 纹理 ✓ tint ✓ · **worldgen 未放置** | G3 / G4 |
| `worldgen_catalog` | 仅 `ore_veins.json` + `fluid_deposits.json` | G4 |
| T13c 被排除对象 | **763 call sites / 1,701 expanded**，零 disposition | 第九表 |
| 其中物流相关 | Hoppers 2 · Logistics 4 · Sorting 4 · Storage 28(624) | 第九表 |
| 其中开局相关 | Molds 117 · Smelting Crucibles 39 · Crucibles Faucets 39 | 第九表 |
| `machine_kinds` 表 `out_of_scope` | **0**（七表中唯一） | 第九表要修正的正是这个 |

---

## 9. 明确不做

| 不做 | 理由 |
|---|---|
| 像素级视觉回归比对 | 极脆弱，一次调色就全红；G2 只判"是否占位"，不判美丑 |
| 把美术质量做成门禁 | 主观。门禁只测客观事实：链是否闭合、是否共享未声明底纹、是否可达 |
| 搬 GT6 贴图 | 许可证。表现层没有 GT6 源，这正是它需要独立分母的原因 |
| 在 P0 里顺手补纹理 | P0 是纯工具卡，publication delta = 0；先知道红项有多少 |
| 把第九表的 1,701 条全部实现 | 分母不是义务。只有判 `v1_required` 的才生成卡 |
| 引入"子阶段"概念 | 项目里没有它；一张卡就是一张卡，引入会让三本账的记法失效 |

---

## 10. 与其他文档的关系

- 不可违反的不变量：《CrucibleCraft-总体规划.md》
- 五轨与 1.x portfolio：《CrucibleCraft-第五阶段总体规划.md》——**本文补的是 v1 侧，不是轨**
- 当前 v1 工作集：`tools/t27_portfolio.json`（765 项 / v1_work_set 2 项）
- 载荷权威：`tools/t14_load_budget_policy.json`

本文在 P0 未开卡前只是规划材料。§8 全部数字来自 2026-08-18 快照，P0 开卡时须从
artifact 重新派生；不一致以 artifact 为准。
