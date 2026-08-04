# CrucibleCraft 总体规划

> GregTech 6 → Minecraft 1.21.1 NeoForge 移植
> 最后更新：2026-08-03　基线：`src-2026-7-31-2107`

---

## 0. 这份文档怎么用

**它不是路线图，是导航仪。** 路线图告诉你终点在哪，导航仪告诉你现在该往哪拐。

- **每次开工前**：读第 1 节（我在哪）+ 当前阶段的判据卡（我要证明什么）。三分钟。
- **卡住 / 想不起来在干嘛**：直接跳到第 4 节。
- **发现一个 bug，不确定要不要修**：第 4.2 节有决策树。
- **想顺手改点别的**：第 4.3 节。
- **要开新阶段**：第 6 节的开工检查表。

**唯一的硬规则：任何时刻最多一个阶段是"进行中"。** 阶段间允许明确写「无（等待下一阶段开工检查）」，但禁止为了填空提前启动下一阶段。

---

## 1. 项目坐标系

### 一句话目标

> 让"加一种材料 / 一条配方 / 一台机器"变成写 JSON，而不是写 Java。
> GT6 的内容规模（1,773 材料 / 数万配方）决定了任何手写路径都是死路。

这句话是所有技术决策的裁决标准。当两个方案难以取舍时，问：**哪个更接近"下一个内容改动不需要碰 Java"？**

### 层级体系（唯一，禁止再造）

| 阶段 | 名称 | 状态 |
|---|---|---|
| T0 | 材料/前缀基础设施 | ✅ 已关闭 |
| T0b | 1.21.1 侧的 `c:` 标签面 | ✅ 已关闭 |
| T1 | 材料表批量导入 | ✅ 已关闭 |
| **T2** | **矿物加工链** `ore → crushed → dust → ingot` | ✅ **已关闭** |
| **T3** | **组件形态扩展 plate/rod/gear/wire…** | ✅ **已关闭** |
| **T4** | **工具系统** | ✅ **11 类材料工具与批量配方已关闭** |
| **T5** | **化学与流体** | ✅ **145 / 145 terminal 路由、152 条 live recipe 与流体玩法闭环已关闭** |
| **T6** | **电力时代** | ✅ **T6a–T6d 电气运行时与硬化验收已关闭** |
| **T7** | **材料事实进入规则语言** | ✅ **62 / 62 标签闭合、220 条标签规则与材料事实纵切已关闭** |

> **历史教训**：曾经存在过第二套编号（`t1_generated` 目录 vs 代码里的 `T2_MACHINES`），两套指向不同阶段，差点读不懂自己的 commit。已在 2026-07-31 统一。
> **规则**：新产物一律用**语义命名**（`ore_chain_generated`），不用阶段编号命名。阶段编号只出现在这份文档、代码里的阶段常量（`T2_MACHINES` / `T3_MACHINES`）和 commit message 里。

### 现在的位置

**T5、T6 与 T7 已关闭。** 固定 revision 的 720,841 条 GT6 dump 重放把原始 162 条 terminal dust 分成 145 条可执行 source route 与 17 条仅有包装循环的 `gt6_dead_end`。145 条 terminal recipe 全部直接消费对应 dust；6 条固定 source fluid producer 关闭输入流体缺口，另以 1,517 行 distillery 全分类台账生成 `water → water_distilled`，live 总数为 152。15 种非熔融 fluid、O-17 多罐原子事务、输出排空与便携罐、通用 EMI、O-19 哨兵兼容边界、最终预算以及 T5/T6 电气纵切纳入前 38 条 GameTest，T7 再增加 1 条材料事实纵切，当前为 39 / 39。

T6 的直接事实已从固定 GT6 Java revision 恢复并接入运行时：raw 2,214 与 live 1,773 种材料全部分类，30 / 29 种导体、28 种 cable-capable source conductor、0 unclassified；115 个 source-backed cable blocks 与 29 个 `wireGt01` blocks 组成 144-block / 9,216-state 运行域。实现包含 loaded-only 稳定 DFS、逐段 exact loss、持久 burn/衰减、下一安全 tick 着火、上一 tick wattage 接触伤害、`ANY.Rubber` 绝缘，以及端点违约不逃逸 tick 的耗散式提交。

T7 将 23 个 `PROCESSING.*` 与 39 个 `PROPERTIES.*` 标签全部纳入固定 revision 的 policy/readiness 台账，`unclassified = 0`。两条 authored mortar 规则按 `MORTAR_GRINDABLE` 展开为 ingot→dust 126 与 gem→dust 94，共新增 220 条；live mortar 为 691、全局 publication 为 17,189 / 18,000。damage 数值事实与 952 个 formula tooltip 已进入运行时，`EnergyType` 收敛到 4 / 4，O-22 零损耗电缆域已关闭。

具体见第 3 节。

---

## 2. 阶段判据卡

每张卡有四格：**判据**（一句话，可执行、可证伪）、**交付**、**范围之外**、**完成信号**。

判据的写法有三个刻意的约束，沿用 T1 的经验：

1. **全链路** —— 单点不算，要验证链条能接上
2. **不是手写的** —— 验证的是管线，不是内容
3. **换材料不改代码** —— 验证的是泛化，否则等于换个方式手写

---

### T0 · 材料/前缀基础设施 ✅

**判据**（已达成）
> 新建一个 `materials/tungsten.json`，跑一遍构建，进游戏能看到钨的锭/板/粉/块，`c:ingots/tungsten` 标签挂上了，粉碎机能把钨锭打成钨粉。全程没碰 Java。

**关键架构产物**

```
materials/*.json ──generation_flags──► MaterialPrefixCatalog.resolve() ──► forms()
                                                                            │
material_registration_gate.json ───────────────────────────────────► registeredForms()
                                                                            │
                                                                      ModItems 注册
```

- `forms()` 是**事实**：GT6 说这个材料有哪些形态，永不随构建策略变化
- `registeredForms()` 是**策略**：本次构建实际注册哪些
- 两者分离是 T0 最重要的遗产，后续所有阶段都要遵守（见第 5.3 条）

**已知边界**：判据里写的是"锭/板/粉/块"，**恰好绕开了矿石方块**。这个洞留给 T2a。

---

### T0b · `c:` 标签面 ✅

**判据**（已达成）
> 跨 mod 互通的词汇表定死：哪些形态用 `c:`，哪些用 `cruciblecraft:`。

**落地结果**

- T0b 关闭时 `c:` 覆盖 9 个约定前缀：`dusts / gems / ingots / nuggets / plates / rods / storage_blocks / wires / raw_materials`；T2 新增 `ores` 后当前为 10
- `cruciblecraft:` 覆盖其余 33 个 GT 专有前缀
- 判定规则：**别的 mod 可能也产出这个形态吗？** 是 → `c:`，否 → `cruciblecraft:`

**T2/T3 新增形态时，先过一遍这个判定，别等标签生成完再返工。**（`raw_ore` 就返工过一次，重建了 420 个标签文件。）

---

### T1 · 材料表批量导入 ✅

**判据**（已达成）
> GT6 的稳定材料全部变成 CC 的材料定义，物品 id 无碰撞，配方引用未注册物品在构造上不可能发生。

**交付**：1,773 材料定义 / 14,547 注册形态 / 42 个前缀 / 106 条前缀生成规则

---

### T2 · 矿物加工链 ✅ 已关闭

**判据**（已达成）
> 在材料数据里声明钨有矿，跑一遍构建，生存模式可从真实 configured feature 挖到钨矿石（stone / deepslate），真实 loot 得到 raw ore，再经 `crusher → sluice → centrifuge → shredder → sifter → smelter` 得到钨锭；内容声明不需要新增材料专用 Java。

**落地结果**

- 第 43 个运行时前缀 `ore` 已进入统一材料管线；137 种 ore 材料 × 2 种宿主岩 = 274 个生成矿石方块，方块、模型、标签与 loot 都由材料声明派生
- 5 条语义矿脉 JSON 生成 5 组 configured / placed feature 与聚合 biome modifier；新增矿脉仍是写数据，不是写 Java
- 六阶段链路共 1,980 条 concrete recipe：crusher / sluice / centrifuge / shredder / sifter 各 357，smelter 195；其中 1,060 条采用直接 GT6 阶段证据，920 条采用 topology fallback。T5 恢复 `crushedCentrifugedTiny` 的准确前缀后，旧 centrifuge 证据不再被错误等同为普通 `centrifuged_crushed_ore`。
- 每条 concrete recipe 都携带运行时 provenance（`source_kind`、selected source、evidence hashes），生成器强制上游闭包且输入签名无重复
- 两本原始待分类集合实际为 349 条 crusher-without-worldgen 与 162 条 sifter-dust-without-smelter；现已逐条归类，`unclassified=0`
- 13 / 13 GameTest 通过，含 copper / tin / iron / gold 泛化链与「真实钨矿脉注册 → 放置 → 双宿主方块 → loot → 六机器 → 钨锭」

#### T2a　`ore` 前缀进管线 ✅

- `MaterialPrefixes.ORE`、gate、注册、动态方块与双宿主资源均已闭合
- ore 域以数据事实为主；钨不是 `PROPERTIES.COMMON_ORE`，因此用材料声明中的显式 `include_prefixes: ["ore"]` 表达设计选择
- 形态爆炸按实际数先算后做：137 × 2 = 274，而不是拿加工链材料数乘未来宿主数

#### T2b　矿脉数据化 ✅

- 5 个 authored vein 文档覆盖 copper / tin / iron / gold / tungsten，builder 双向校验材料事实、gate 注册与运行时 registry id
- GameTest 从运行时 configured-feature registry 取出钨矿脉、重算锚点并真实放置，不再拿文件存在冒充世界生成验收

#### T2c　清空两本台账 ✅

| 台账 | 原始集合 | 分类结果 | 未分类 |
|---|---:|---|---:|
| 有 crusher 配方但无当前矿源 | 349 | vein 129 / byproduct_only 110 / t5_chemical 110 | 0 |
| 终端 dust（无 smelter 配方） | 162 | t5_chemical 145 / gt6_dead_end 17 | 0 |

**归零的是未分类项，不是死路本身。** 分类结果保留在 builder 派生的 closure artifact 里，并由测试锁定。

#### T2d　平衡 ✅

- 固定策略：**熔炉捷径与六段链的主产物单位相同；六段链的收益是分阶段副产物；捷径有意保留为 compatibility route**
- 187 raw + 196 crushed = 383 对熔炼/高炉配方，共 766 个 JSON；全部带 `"group": "cruciblecraft:compat_shortcut"`，gate 推导集合与生成物做双向全量比对
- 820 个 generated recipe 不再生成 recipe advancement；动态材料模型、item/block tag 只由运行时生成包提供，不再保留编译期材料快照
- copper 的 sluice / centrifuge / shredder 概率分别锁定到所选 GT6 evidence，而不是只检查非空或套全局概率模板
- 新增真实 duration GameTest：铜 crusher 持续供能，完成前观测到 `duration - 1`，只在声明时长耗尽后原子提交；端到端链仍保留加速

**范围之外（明确不做）**
- 不做组件形态（plate/rod/gear/wire）
- 不做 extruder 模板展开
- 不碰流体与化学
- 不动电力
- 不做单文件打包（O-1 的热命中、校验与失效语义已修；产物分发结构仍归 O-10）
- **不为了性能收紧门控**（创造栏已按前缀语义拆分；门控继续只表达内容策略）

**完成信号**
1. `tungsten.json` 的 ore 声明 → 真实 configured feature → stone / deepslate 钨矿 → loot raw ore → 六机器 → 钨锭，GameTest 已通过
2. 349 / 162 两本台账全部有证据分类，closure `unclassified=0`
3. provenance、compatibility group、主产物单位策略与副产物 chance 均由 builder 派生并有 Python / Java 交叉断言

---

### T3 · 组件形态扩展 ✅ 已关闭

**判据（已达成）**
> 从材料锭出发，由紧凑语义规则在运行时展开轧板、装配齿轮、车杆、带可复用 shape 的挤压、拉丝、切箔和细线路线；铜 / 锡 / 铁 / 金四种材料不增加材料专用 Java，均由真实 live machine map 按声明时长产出并转移到输出槽。

**落地结果**

- 31 个 extruder shape；62 个 GT6 模板已全量分类为 playable 20 / skipped 42 / unclassified 0。普通 CI 校验提交的 compact index/report、selector policy、builder 投影和测试；本机缺少 O-10 raw/cache 时 `--verify` 明确记为 `SKIP`，不冒充 full replay。
- 2,810 条 T3 紧凑 MaterialRule 加 21 条 T4 工具规则展开为 11,593 条 live T3-map 配方：T3 component 8,141、T4 tool 3,452，分别低于 10,000 / 4,000 / 13,000 分层预算，shadow signature 0。分 map：extruder 2,782、press 1,191、lathe 929、cutter 651、bender 638、assembler 4,020、rollbender 438、rollingmill 336、welder 321、wiremill 287。
- selector 策略按配方族显式分开：extruder 使用 `concrete_shape`，support 规则使用 `explicit_sparse_relation`；不能把 shape 模式外推到 mixer / bath / shredder，这些族以后必须先建立自己的可证明选择器。
- `Iron generates_wire` 不是 GT6 原始 gate 事实，而是四材料 T3 验收所需的显式修正，分类固定为 `t3_acceptance_required_not_gt6_original_gate`；它为普通 wiremill 规则增加 2 条展开，并由 importer、gate、builder 和测试共同锁定。
- builder 记录源文件、builder source、selector/index/report/gate 输入及生成树 SHA-256；最终验证报告同时绑定 2,810 文件的 component tree、34 文件的 shape resource tree、artifact manifest 和 GameTest 接受证据。

#### T3a　模板与 selector 证据 ✅

- 把 62 个 extruder 模板及 31 个 shape 压成可提交的 index/report；playable / skipped 的理由可追溯，未分类为 0。
- 对不同配方族禁止“看起来相似就套同一个选择器”；selector policy 是 builder 输入，也是 currentness hash 的一部分。

#### T3b　紧凑规则与预算 ✅

- 语义源经 `build_component_rules.py` 生成 committed MaterialRule JSON；recipe id、signature、shadow signature 和 generated tree 均有稳定摘要。
- 8,141 条展开中 extruder 2,782、非 extruder 5,359；预算 10,000，重复输入/目标 shadow 为 0。

#### T3c　运行时四材料验收 ✅

- copper / tin / iron / gold 都走：`ingot → rollingmill plate → assembler gear`、`ingot → lathe rod`、`ingot + exact reusable shape → extruder long_rod`、`ingot → wire`、`plate → cutter foil → fine_wire`。
- GameTest 观测真实机器输出转移、声明 duration 与 extruder shape 保留：共同 duration 为 rollingmill 100t、assembler 240t、lathe 120t、wiremill-ingot 120t、cutter 80t、wiremill-foil 80t；extruder 分别为 copper 64t / tin 64t / iron 185t / gold 311t。
- 最终 18 / 18 required GameTest 通过；不是用 JSON 存在或字符串快照代替运行时判据。

#### T3d　收尾与可复现性 ✅

- 两次 `runData` 的规范树 SHA-256 同为 `bd463ccab39abb02577cf9bac386e564337359f64dabd51f802c9d4ff9635aab`，drift files 0。
- `full_verification_report.json` 只有在 builder、双 runData、Java、GameTest、Python 全部由 verifier 协议记录后才能进入 `READY`；最终状态已为 `READY`。
- O-10 采用 metadata-only cache policy：仓库提交哈希、重建命令和紧凑证据，不要求普通 CI 恢复大 cache。当前树删除 cache **不会**清除旧 commit 中已经存在的 blob；若要缩减远端历史，仍需干净根、显式 history migration 或 Git LFS。
- O-12 已用 source length 4,096、parser/evaluator nesting depth 64 和带 rule id 的失败诊断关闭。

**范围之外（明确不做）**
- 不启动 T4 工具、T5 化学/流体或 T6 电力内容
- 不把 mixer / bath / shredder 强行解释为 extruder shape 模式
- 不恢复 O-10 大型 raw/cache，也不在本阶段改写 Git 历史
- 不处理 O-13 存档 VERSION=2 降级保物品策略；它仍是实际 bump 前的阻断项
- 不优化 O-14 全局 item tick；它仍只在性能剖析命中后处理

**完成信号**
1. 31 / 62 模板证据闭合，分类 20 / 42 / 0；2,810 规则展开 8,141，预算内且 shadow 0
2. 四材料 live machine 路线观测到声明 duration、真实输出转移和 shape 保留，18 / 18 GameTest 通过
3. builder / generated trees / selector / artifact policy / GameTest 被同一 currentness snapshot 绑定，双 runData 无漂移，最终报告为 `READY`

---

### T4 · 工具系统 ✅

**判据**
> `PROPERTIES.HAS_TOOL_STATS` 定义工具材料域，`tool.types` 与显式工具策略决定资格，durability / quality / speed 只驱动属性；任何满足资格且原料路线闭合的新材料，无需新增 Java 或注册 Item 即获得对应工具，并且组件配方查找保持 0 unindexed、机器催化剂以扣耐久事务提交。

#### T4a　开工准备 ✅

- GT6 `mToolTypes` 已确认是 0–3 有序等级，不是位掩码：0 无工具、1 木/石/燧石、2 early、3 advanced；具体工具仍由显式 `tool_rules` 决定。
- 工具域严格由 `HAS_TOOL_STATS` 定义，共 546 种，恰好等于 `types > 0`；`durability > 0` 额外命中 44 种噪声材料，禁止用于选域。
- `types` 分布 1 / 2 / 3 = 232 / 28 / 286；`NO_ADVANCED_TOOLS` 不能替代具体工具资格。11 类工具逐项移植 GT6 listener / prefix / full-tool 的 `typemin`、`qualmin` / `qualmax`、精确 `MT.Wood.NOT` 和 tag 排除条件，不再使用 blanket `advanced` 推断。
- 资格谓词不是无来源常量：`tools/t4_tool_policy.json/eligibility_predicate_sources` 为 types、ANTIMATTER、COATED、WOOD、BOUNCY、STRETCHY、quality 上下界逐项记录 reason、GT6 revision、文件与行号，并锁定受影响 tag 数 2 / 5 / 128 / 6 / 1。两处不对称同样是来源事实：Wrench 的 `Loader_Tools.java:310` 没有 COATED，Screwdriver 的 `OP.java:248` / `Loader_Tools.java:306` 没有 BOUNCY / STRETCHY；不得从相邻工具插值补齐。
- 当前注册形态：rod 542、bolt 543、dust 522、screw 433、plate 292、ingot 205、gem 130；plate + rod 同时具备 290，至少缺一项 256（rod 无 plate 252、plate 无 rod 2、两者皆无 2），不再把 252 与 256 混写。
- 工具制造路线是逐工具、逐 GT6 pattern 的显式移植策略，不再用一套 plate / gem / rod 模板外推全部工具。精确材料 `stone` 只保留 Pickaxe 的 `stone_rod_exception`；`MT.Wood.NOT` 按材料身份 `wood` 解释，不误写成 `PROPERTIES.WOOD` tag。
- 材料身份例外必须在 `material_identity_exceptions` 中同时声明 material、tool、pattern、condition_routes、reason 与固定 source revision；规则中的不同 `material.is(...)` 字面量当前只有 `stone` / `wood` 两个，由 Python 台账和 Java 资源测试共同锁定，避免通用规则静默退化为查找表。
- MaterialRule 语法把形态语义拆为 `has_form`（GT6 事实形态）与 `has_registered`（registration gate 可获得形态）；工具路线只允许使用后者。`GTRecipeMapLoader` 每次 reload 只加载、解析一次 gate，再把同一份双索引传给全部规则。
- `tools/t4_tool_readiness.json` 对每类工具的 546 种候选材料给出 listener / prefix / full-tool / route-ready 分层结果与跳过原因，unclassified = 0；总投影 3,452 条且输入 signature collision = 0。
- 选型固定为「每工具类型一个 Item + `TOOL_MATERIAL` 组件」，不做 546 × N 注册；配方索引必须按 item id + 规范组件指纹建立二级键，发布门禁保持 unindexed = 0。
- quality → 1.21.1 挖掘档位是独立策略：0 / 1 / 2 / 3 / 4..15 压成 wood / stone / iron / diamond / netherite；speed 同样是策略而非原样事实，钳制到 0.1..20.0（上界约为 vanilla netherite 9 的 2.2 倍）。台账锁定 12 种被钳制材料：anti_vibranium、cosmic_neutronium、draconium_awakened、infinity、infused_balance、infused_dull、infused_entropy、red_matter、vibramantium、vibranium、vibranium_silver、vibranium_steel；避免异常导入值直接进入游戏，且不伪造 10 档全序标签。
- durability 使用饱和转换到 1..2,147,483,647；工具催化剂必须扩展为「同槽同物品、count 不变、damage 改变」的事务，不能复用 presence-only（它会预留 1 supply，且有 12 的求解上限）。

#### T4b　首个可合并纵切 ✅

- `material_pickaxe` / `material_file` 均为单 Item + `TOOL_MATERIAL` 组件；stack 只持久化材料身份与实际 damage，max damage / mining speed / correct-for-drops / attack attributes 在读取时从当前 MaterialCatalog + T4 策略派生，`/give` 的默认 iron Pickaxe/File 同样完整可用，runtime tuning 不留下铸造时快照；隔离材料返回正 durability 哨兵但禁止玩家与机器实际磨损，不会因第一次使用被销毁。
- assembler 扩展为 3 material + 3 tool/pattern 输入槽；File、Smithing Hammer、固定 Flint Knife 等催化剂使用 `WEAR(1)`，图样选择器使用 `PRESERVE`。成功时催化剂 count 保持 1 且 damage +1；输出阻塞、能源失败或事务重验证失败时，多个催化剂与图样全部不变。
- `DataComponentIngredient` 仅对显式白名单中的字符串组件（`tool_material` / `machine_material`）建立 `(item, component id, value)` 二级索引；未知组件、未知 custom ingredient 或 removed-only patch 按 recipe 逐条拒绝并记录 map / recipe / ingredient 类型，旧 epoch 不因单条附属配方失效；核心运行时保持 rejected = 0、unindexed = 0。
- quarantine 状态使用固定 `unsupported_version` / `material_quarantined` / `unknown` 词表，版本号与原始材料 ID 作为参数展示；anvil / crucible actionbar、Jade、tooltip 与服务端 warn-once 已接入真实隔离原因。
- 外部组件验证改为 item 实例级 `MaterialComponentPolicy`；CrucibleCraft 自有 material-component item 未声明策略时 fail-closed，NBT / 网络边界不得调用抛异常的 `requireAllowed`。
- 工具模型已注册材料 tint；当前仍复用 vanilla 占位纹理，但不同材料不再全部显示为同一铁色。

#### T4c　11 类工具批量生成 ✅

- 21 条紧凑 MaterialRule 覆盖 pickaxe / shovel / axe / hoe / sword / smithing hammer / file / chisel / saw / screwdriver / wrench；按各自 GT6 资格、材料输入多重集、柄数量与催化剂展开为 3,452 条配方。各类 route-ready 数为 330 / 412 / 329 / 329 / 412 / 317 / 92 / 307 / 307 / 309 / 308。
- `item-eligible` 是 GT6 材料能力，`route-ready` 是当前 registration gate 下的生存可制造性，两者不再被伪装成同一集合。各类 eligible-without-route gap 为 208 / 126 / 209 / 209 / 126 / 80 / 56 / 2 / 2 / 0 / 2，总计 1,020；Pickaxe 的 208 条包含 `oak` 等 126 种木材。台账保存完整材料清单，Python 与 GameTest 同时断言数量。`variant("oak")` 仍是合法的命令/addon/future-gate stack，但当前无生存配方；创造栏只展示 iron / diamond / stone 三种 route-ready 代表，避免暗示 oak 当前可制造。
- 每条 flattened assembler 配方保留 GT6 材料数量和催化剂，同时增加按工具类型唯一的 `tool_pattern_*` `PRESERVE` 选择器。因此即使两类工具材料输入相同，`validateNoShadows` 也不会依赖偶然顺序；投影与 live map 均为 0 collision / 0 unindexed / 0 rejected。
- MaterialRule 输出只写 `$material` 对应的 `TOOL_MATERIAL`，max damage、挖掘速度、挖掘档位与攻击属性不进入 JSON 或 stack patch。固定 Flint Knife 提供 File 的 bootstrap；材料 Smithing Hammer 复用原注册 item，并可继续在砧上按材料 tier 工作。
- `MaterialDiggerItem` 统一 pickaxe / shovel / axe / hoe 的动态挖掘、掉落判据、攻击属性与耐久；axe / shovel / hoe 通过 NeoForge tool-modified-state API 恢复剥皮、铲路/灭营火、锄地行为。Axe 不再添加原版没有的「副手持盾则 PASS」分支，三种 use-on 均只保留各自 vanilla/NeoForge 条件；Sword 与工坊工具保持各自的资格和耐久策略。

#### T4d　预算与性能门禁 ✅

- 数量预算分层为 T3 component 10,000、T4 tool 4,000、live T3-map 13,000；当前分别为 8,141 / 3,452 / 11,593。13,000 来自完整投影加至少 10% 余量后向上取整，不再把关闭的 T3 内容与新增 T4 内容混成一个 10,000 上限。
- 2026-08-02 本机重复 GameTest 的保守观测上界：完整 reload 5,332 ms、index build 100 ms；从 11 类工具均匀抽取 256 条配方、循环 4 轮的 1,024 次 assembler lookup 平均不高于 218,226 ns、平均 10 个候选。reload / index / lookup / candidate 门禁分别为 10,000 ms / 1,000 ms / 1,000,000 ns / 64 candidates，并接入 GameTest。lookup 基准只在显式验证时运行，不再让每次生产 reload 白付约 2,300 次查询。
- 旧 `used_after - used_before` heap delta 在无 GC 时会被任意并发分配和负差钳制污染，`0 MiB` 不是 retained-memory 证据；该失效指标与 256 MiB 伪门禁已删除。若重新引入内存门禁，必须使用可变异验证的 allocation / retained-size 工具。
- 基准首次暴露了公共 File / Hammer / Pattern 催化剂把 lookup union 扩到数千候选（2,624,751 ns）；索引现只把 `CONSUME` 输入作为主候选键，仅在配方没有消费输入时回退到 `WEAR` / `PRESERVE`。优化后语义不变，候选和耗时均进入可审计指标。

**T4 首批范围**
- 核心手工具：pickaxe / shovel / axe / hoe / sword
- 工坊与机器催化工具：smithing hammer / file / chisel / saw / screwdriver / wrench
- 不含电动工具、枪械、多功能工具

**开工门禁**
1. `python tools/build_t4_tool_readiness.py --check` 通过，546 / 546 已分类且 unclassified = 0
2. 组件二级索引和 damage transaction 先落底层测试，再批量生成工具内容
3. 新增材料的验收同时覆盖资格、形态路线、工具属性、配方索引和催化剂耐久，不允许只验证 Item 存在

**完成信号**
1. 11 类工具、3,452 条 live recipe 与 21 条规则全部有 GT6 来源、资格/闭包台账、1,020 条显式 eligibility-route gap 和唯一输入 signature
2. assembler 多催化剂原子提交、动态属性、vanilla use-on、组件索引与 quarantine 语义由 Java 单测和 20 / 20 GameTest 覆盖
3. 数量与性能预算均由运行时观测值驱动并在测试中响亮失败；T4 readiness 为 unclassified 0 / collision 0

---

### T5 · 化学与流体 ✅

**判据（已达成）**
> 化学链从固定 GT6 revision 的组成、机器路由与流体形态数据声明式生成；原始 162 条 terminal dust 中有 source transformation 的 145 / 145 条均有直接消费 dust、可执行、可追溯的去处，另外 17 条以完整 dump 证明为 `gt6_dead_end`；增加或修改满足注册闭包的材料只改数据，不改 Java。

**依赖**：T2c 的台账是 T5 的输入。T2 做得马虎，T5 就没有边界。

**已落地**
- O-17 的多 tank `fill` / `drain` 已采用 simulate plan、pre-image、execute post-image 校验和全量 rollback；O-19 的三条 legacy hammer recipe 已删除持久化 `MAX_DAMAGE`，第三方 raw-component reader 被明确列为兼容边界。
- `t5_chemical_policy.json` / readiness ledger 固定 GT6 revision `3703e403...`，对 162 terminal dust、110 crusher debt、224 种去重化学材料和 119 个非熔融 fluid 候选逐条分类；`unclassified=0`。
- 只注册当前 live recipe 需要的 15 个非熔融化学流体；规则解析支持显式 `material_fluid=chemical|molten`，不把气体/酸/溶液压进 `molten_fluid`。
- T5 specs 支持最多 6 item input、6 item output、4 fluid input、6 fluid output、独立 sided I/O 和逐 tank 同步/UI；新增 electrolyzer / mixer / distillery / autoclave / drying / compressor，并按 source-driven 需要复用 assembler / bath / centrifuge / smelter。
- 最小电力纵切为 `steam engine → bronze dynamo → adjacent ELECTRIC buffer → electrolyzer`；材料电压、电缆、线损仍留 T6。
- 固定 dump 投影为 152 条：assembler 1 / autoclave 17 / bath 6 / centrifuge 14 / compressor 5 / distillery 1 / drying 5 / electrolyzer 62 / mixer 34 / smelter 7。其中 145 条关闭 terminal dust，6 条关闭 hydrochloric acid / hydrogen fluoride / glue / molten aluminium / molten cryolite / molten aluminium fluoride 的生产缺口，1 条关闭基础 distillery 纵切；所有配方均能由真实 host validator 接受并由 `findMatch` 反查。
- 17 条 source dead-end 在全部 720,841 条 recipe 中只有 boxinator / canner / unboxinator 包装循环；它们不进入 145 的可执行分母，也不生成 CrucibleCraft 特供 chemical recipe。
- 通用 64,000 mB 便携流体罐、输入维护排空和输出侧排空使产流体配方可重复执行；所有 23 台 configured machine 由同一份 `ProcessingMachineSpec` 驱动 EMI category、workstation、槽位/罐位和 recipe 投影。
- 便携罐在输入侧按“可提供流体”优先选择填充，空罐才进入维护排空；GameTest 已证明 electrolyzer 的氯气能经便携罐进入 mixer 并被 hydrochloric acid 配方消费。GUI 的 progress / duration / FluidStack amount 直接读取客户端 block entity，tank capacity 直接读取 spec，不再把大数值绕回 `ContainerData` 的 16 位网络通道。
- `findMatch` 继续剔除被支配的较弱候选；多个互不可比的极大元按稳定声明序选择第一条。相同输入签名仍在发布期由 `validateNoShadows` 拒绝，玩家组合多个合法输入不再在 block-entity tick 中抛异常。
- 最终发布观测为 T5 152、全 map 16,969，reload 1,297 ms、index 34 ms；显式 GameTest lookup 基准平均 52,506 ns / 10 candidates。生产 reload 只发布与记录结构/时长，不再执行 lookup 基准或伪 heap 采样；最终数量预算固定为 T5 200、全局 18,000。

---

### T6 · 电力时代 ✅ 运行时闭环

**判据**
> 固定 GT6 source 中的直接 `addElectricWires` 注册驱动导体资格和每种 wire/cable specification 的精确电压、电流、线损、绝缘与接触伤害；新增 source-backed 导体只改数据即可获得对应电缆。每次注入都按稳定拓扑守恒传输，逐块扣除来源线损，过压/过流按已声明策略可观测地烧毁电缆。

**运行时闭环（已达成）**
- 固定 revision `3703e403...` 的 30 个直接导体、28 个 cable-capable 导体和 `ANY.Rubber` 绝缘组均有文件 hash 与行锚点；不能用 `ITEMGENERATOR.WIRES` 或 OreDict 前缀反推电气资格。
- importer 将 exact `max_voltage / max_amperage / loss_per_meter / insulated / contact_damage` 按 specification 写入 29 个 live 导体材料；raw 2,214 与 live 1,773 的 conductor / insulator / N/A 分类均 `unclassified=0`。
- 五种 live cable gauge 为 1 / 2 / 4 / 8 / 12，共 118 个已注册 recipe/item 形态；其中 3 个无 electrical specification 的单股 cable 继续保持普通 item。运行时严格取 115 个 source-backed cable blocks，再加入 29 个 source-backed `wireGt01` blocks，最终为 144 blocks / 9,216 logical states。
- 每个 conductor block 只有六方向 boolean state，compact multipart 资源不展开 64 份 variant；网络采用 loaded-only 稳定 DFS、每次注入独立 visited set、逐块 exact signed loss 与同 tick 负载聚合。过载采用 16-hit / 512-tick burn，counter 与衰减相位持久化，下一安全 tick 可观测着火。
- 电气提交以 source-first、逐端点串行重验为边界：自有端点保持 simulate/execute 守恒，外部违约端点被限频报告并耗散差额，不得把异常抛出 block-entity tick。模拟计划只在同 tick、同 ingress/size/amount 下复用，正式串行预检前主动失效旧 demand cache；64 种连接 shape 按 gauge 预计算复用。
- 仅 `contact_damage=true` 的裸线按上一 tick wattage 使用 pinned `tierMax(wattage) * 4`；绝缘 cable、Graphene 等 source flag=false 裸线与无流量裸线均不伤害。`ANY.Rubber` 由数据组生成 aggregate plate tag。
- `tools/t6_electrical_readiness.json` 与 `tools/full_verification_report.json` 均绑定当前 source/runtime guards；状态 `READY` 表示 T6a–T6d 运行时与验收已关闭，而非仅开工许可。

---

## 3. 现状快照

> 每次阶段切换时更新一次。数字对不上就是有东西漂移了。

| 项 | 数值 |
|---|---|
| 材料定义 | 1,773 |
| 其中 metadata_only | 663 |
| 注册形态 | 14,799 |
| 前缀（已落地） | 45（含 `ore`） |
| 前缀（`c:` / `cruciblecraft:`） | 11 / 34 |
| ore_chain 配方 | 1,980（1,060 GT6 证据 / 920 topology fallback） |
| ore_chain 分阶段 | crusher / sluice / centrifuge / shredder / sifter 各 357；smelter 195 |
| 熔炉捷径 | 383 对（raw 187 / crushed 196）/ 766 文件，全部 `compat_shortcut` group |
| 矿石材料 / 方块 | 137 / 274（全部生成） |
| 矿脉 feature | 5 |
| 宿主岩 | 2（stone / deepslate） |
| T3 component rules | 2,810 条规则 → 8,141 条配方（extruder 2,782；预算 10,000；shadow 0） |
| T4 tool batch | 21 条规则 → 3,452 条配方（11 类工具；collision / unindexed / rejected = 0） |
| Extruder shapes / templates | 31 / 62（playable 20 / skipped 42 / unclassified 0） |
| GameTest | 39 / 39 通过 |
| Java / Python 单测 | 399 / 399；206 / 206 通过（Python 可选 raw/cache replay 仍显式 skip） |
| generated recipe advancement | 0（runData 的 832 个 recipe 全部不生成 advancement） |
| 编译期动态材料 item tag 快照 | 0（运行时生成包单源） |
| 台账：无当前矿源的 crusher 入口 | 原始集合 349；已分类 349；未分类 0 |
| 台账：终端 dust | 原始集合 162；t5_chemical 145 / gt6_dead_end 17；未分类 0 |
| T5 chemical readiness | 224 种材料全部分类；可执行 terminal denominator 145 |
| T5 live projection | 152 条 recipe；terminal dust 145 / 145，fluid closure 6 / 6，distillery vertical 1，未闭合 0 |
| T5 non-molten fluids | 候选 119 全分类；live closed registration 15 |
| T5.5 machine crafting | 23 台 configured machine + 7 条 steam-chain recipe 全分类；direct projectable 0，前置缺口已声明 |
| T6 electrical readiness | raw 2,214 / live 1,773 全分类；118 cable item forms → 115 runtime cable blocks；29 wire blocks；144 blocks / 9,216 states；READY |
| T7 material facts | 62 / 62 标签分类；ingot 126 + gem 94 = 220；formula 952；EnergyType 4 / 4；READY |
| recipe publication | T3 8,141 / T4 3,452 / T5 152 / T7 220 / all maps 17,189 |

---

## 4. 迷路急救

### 4.1　症状：不知道下一步该干什么

按顺序问自己三个问题，第一个答不上来的就是你要做的事：

1. **我在哪个阶段？** → 看第 1 节状态；若写「无」，先做下一阶段开工检查，不能擅自把下一阶段标蓝
2. **这个阶段的判据是什么？** → 一字不差地念出来。念不出来就回去读判据卡
3. **我手上正在做的这件事，能推进那句判据吗？**
   - 能 → 继续做，别想别的
   - 不能 → 停下，写进第 6 节悬案登记表，回到判据

如果三个都答得上来但还是不知道做什么，说明**判据太粗了**。把它拆成能在一个工作单元内验证的子判据（参考 T2a/T2b/T2c 的粒度）。

### 4.2　症状：debug 钻进去了

**先分类。只有第一类可以打断主线：**

| 类型 | 判断 | 处理 |
|---|---|---|
| **正确性 bug** | 判据跑不通 | 修。这就是主线 |
| **完备性 gap** | 判据能跑通，但覆盖不全（比如有配方但无当前矿源） | 记进台账，**不修** |
| **本阶段修复引入的回归** | 原判据仍通过，但新增长尾成本、延迟报错或适配器数量语义风险 | 在阶段关闭前修，并补能捕获该形状的回归测试 |
| **性能** | 能跑通，只是慢 | 记进悬案，除非它拖慢你自己的迭代（如 O-1） |
| **洁癖** | 能跑通，覆盖也够，只是不好看 | 记下来，永远不在阶段中期做 |

**三条止损规则：**

- **30 分钟规则**：一个 bug 卡满 30 分钟没有进展，写进悬案登记表，回主线。不是放弃，是排队。
- **"数据源是不是已经标注了"**：卡住时的第一反应永远是这句。T0 的三次系统性错误全是因为在用别的字段推一个数据源已经直接标注的东西。（详见 5.1）
- **判据测试**：修完这个 bug，判据那句话会不会更接近成立？不会 → 它不属于现在。

### 4.3　症状：想顺手改点别的

**"零风险重构"是有的，但要满足全部三条：**

1. 它在当前阶段的代码路径上（不是隔壁模块）
2. 它不改变任何外部可观测行为
3. 你能在改完后立刻用现有测试证明它没坏

三条不全满足 → 写进悬案登记表。

**T1 时把 Crusher 参数化成 `ProcessingMachineSpec` 就是合格的例子**：正好在改这块、行为不变、有 GameTest 兜着。

### 4.4　禁止事项（出现即代表跑偏）

- ❌ 在没有一句话判据的情况下开始写代码
- ❌ 同时推进两个阶段
- ❌ 造第二套编号 / 命名体系
- ❌ 为了性能去收紧门控（O-4 已明确：门控是体验项，不是性能项）
- ❌ 因为"反正要重构"而在阶段中期做大改
- ❌ 把台账数字写进文档但不接进测试断言（它一定会漂移）

---

## 5. 方法论（不可协商的三条 + 一条）

### 5.1　用间接信号代替直接观测，是这个项目里所有系统性错误的共同根因

| 错误的间接信号 | 正确的直接观测 |
|---|---|
| 穷举 ITEMGENERATOR 标签组合去猜 `ore` 的域 | `PROPERTIES.COMMON_ORE` 一直就在数据里 |
| 给 `block` / `wire` 找生成规则 | `prefixes.json` 里 GT6 自己标了 `materialBased=false` |
| 用 `state` 判断哪些材料该没有物品 | 「GT6 前缀 ∩ CC 前缀表 = ∅」 |
| 自己写 snake_case 函数做映射 | `gt6_metadata.source_name` 是权威字段 |
| 只用 `PROPERTIES.COMMON_ORE` 判断所有设计矿石 | 事实域之外的明确设计选择要显式声明；tungsten 有 `ITEMGENERATOR.ORES`，但没有 `COMMON_ORE`，因此用 `include_prefixes: ["ore"]` |
| 测试断言磁盘上有 JSON 文件 | 断言 `RecipeMap.findMatch()` 在 live map 上命中 |
| 断言 SHA-256 字符串长度为 64 | 断言摘要等于提交的黄金值；前者只测试摘要工具能运行，不观测配方内容 |
| 断言 `zh_cn.keySet() == en_us.keySet()` | 直接统计值确实不同的翻译条目；复制英文只能补齐外壳，既不增加覆盖率还会隐藏缺口 |

**卡住时的第一反应**：这个判据是不是已经被数据源直接标注了，而我在用别的字段推它？

**推论**：验收测试要断言**运行时可观测的状态**，不是构建产物的存在。最后那一行是 T2 前夜刚交的学费——磁盘字符串验收改成六张 live `RecipeMap.findMatch` 之后，**立刻捕获了一个 crusher 多输出的非法配方**，而字符串验收放它过了。

### 5.2　让失败响亮：许可 vs 声明

`allow_empty_forms` 把「本该报错」变成「静默通过」，93 个材料因此丢失全部物品而无人察觉。改成 `metadata_only` 之后能双向校验。

> **「允许为空」只能单向放行，「声明无物品」才能反向查错。**

设计新字段时问一句：它是**许可**还是**声明**？许可会掩盖 bug，声明能暴露 bug。

**声明还必须选对报错时刻**：最早能完整判定不变量、且依赖事实已经确立的位置，才是应该失败的位置。`form_items` 的语法可在 catalog bootstrap 检查，但物品存在性依赖所有 mod 的 `RegisterEvent`，只能在 common setup 检查；既不能等玩家打开创造栏才崩，也不能在 DeferredRegister 尚未落表时误报。

**边界判据必须与消费判据同义**：只检查 `MaterialCatalog.contains` 不能保护随后调用 `MachineMaterialRules.requireAllowed` 的设备路径。持久化/网络边界要按「item + component + device」判定并 quarantine；运行时内部构造仍可 `requireAllowed` 响亮失败。测试若用比生产更窄的假谓词，会证明机制能跑，却证明不了真实接线没有空档。

**当前最好的正面例子**：`OreResourceTest` 里的熔炉捷径检查——`expectedDerived` 从 gate + 材料定义**推导**出来，再 `assertEquals` 比对磁盘。多一个文件报错，少一个也报错。

**T2 新增的边界**：`ore` 声明不是一个孤立方块开关。它必须同时闭合 raw ore、crushed / washed / centrifuged、purified dust、dust、ingot 的中间形态和生产者；builder / gate / GameTest 要从两端检查，不能靠某个 JSON 存在就推定整条链成立。

### 5.3　把事实和策略分开

`forms()` 是事实（GT6 说铁有板），门控是策略（这次构建不做铁板）。策略的变化频率远高于事实。焊在一起的话，改一次策略要重写 1,773 个 JSON，而且报警器失效。

新加字段时问一句：**它多久变一次？** 变化频率不同的东西不要放进同一个文件。

### 5.4　判据先于实现（新增）

T0b 的教训是范围失控，T1 的成功是判据先定。

**一个阶段开工的第一件事，是把那句判据写下来并让它可证伪。** 判据写不出来，说明这个阶段的目标还没想清楚——那就不是该开工的时候，是该继续想的时候。

判据要能回答："我怎么知道这件事做完了？"如果答案是"感觉差不多了"，判据不合格。

**拆分聚合物时同时检查乘数**：不能只看每份产物的大小，还要检查「份数 × 每份求值成本」。O-4 把单栏 15k 条目拆成 10 栏后，若每个栏都重算完整计划，形状判据通过但总成本反而放大；因此计划按 runtime revision 只构建一次，所有栏共享。

---

## 6. 悬案登记表

> 规则：任何在 4.2 / 4.3 被排队的东西都记到这里。**记录本身就是完成**，不要边记边修。
> 每次阶段切换时过一遍，决定哪些提升为主线。

| 编号 | 内容 | 类型 | 排期 |
|---|---|---|---|
| **O-1** | **已关闭缓存层缺陷**：普通 `ensure` 先验 manifest，热命中时生成计划 Supplier 调用 0 次；指纹覆盖 gate/registered forms、tier、prefix/model、pack format、schema 与生成器 class bytes；`ensureStrict` 逐文件校验 SHA-256；写入拒绝越界路径并缓存父目录。单文件分发结构不再属于缓存正确性，归 O-10 | 性能 / 正确性 | 缓存层关闭；分发结构跟 O-10 |
| **O-3** | **已关闭**：20k 场景在 21,815 entries 时按协议上限明确拒绝；4,000 entries 做真实 codec encode/decode round-trip；两种 20k full-server 场景 GameTest 均通过 | 正确性 | T2 验证完成 |
| **O-4** | **已关闭创造栏问题**：约 14,922 个动态条目按 ore / ore-processing / dust / metal-gem / plate / parts / mechanical / wire / cable / misc 拆分，当前最大栏 3,248；分组计划与运行时 unification preference 共用解析与 first-owner 去重，并按 `MaterialCatalog.runtimeRevision()` 每轮只构建一次 | 体验 / 性能 | T2 审计修复完成 |
| **O-5** | **已关闭**：1,980 条 concrete recipe 全部携带运行时 provenance；index 与 JSON 交叉断言 selected source / evidence hashes / source kind | 可维护性 | T2 完成 |
| **O-6** | **已关闭**：捷径数从 gate + 生成物派生为 raw 187 对 + crushed 196 对 = 383 对 / 766 文件；Python 与 Java 都做全量计数，不再手填 `388` | 台账漂移 | T2 完成 |
| **O-7** | **已关闭**：766 / 766 熔炉捷径 JSON 均为 `group=cruciblecraft:compat_shortcut`，policy 与 `OreResourceTest` 交叉断言 | 可维护性 | T2d 完成 |
| **O-8** | **已关闭**：新增 copper crusher 真实 duration GameTest，持续供能并观测到 `duration - 1`，不调用 `forceLastTick`；端到端链保留加速 | 覆盖率 | T2d 完成 |
| **O-9** | **已关闭**：钨验收从运行时 configured-feature registry 真实放置，验证双宿主方块与真实 loot，再送入六机器链；文件结构测试只作为低层护栏 | 间接信号 | T2b 完成 |
| **O-10** | **已关闭当前树策略**：采用 metadata-only cache policy，仓库提交 SHA-256、重建命令与紧凑证据，普通 CI 不依赖本地大 cache；当前树删除文件不会清除旧 commit blob，缩减历史仍需干净根、显式 history migration 或 Git LFS | 工程 | T3 收尾关闭；历史体积后果已记录 |
| **O-11** | 生成包 identity 当前哈希 `GeneratedMaterialPack.class`，注释/行号变化也会触发冷写；server/client fingerprint 仍各自构建 canonical 文本。若实测阻碍开发循环，改为显式 `OUTPUT_SCHEMA` + 测试期完整 plan 快照摘要，并复用结构指纹 | 开发体验 / 性能 | 先测量；达到可感知成本再做 |
| **O-12** | **已关闭**：`RuleExpression` 限制源串长度 4,096、parser / evaluator 嵌套深度 64，超限以带 rule id 的 `IllegalArgumentException` 失败，并有边界测试 | 外部输入边界 | T3 关闭 |
| **O-13** | **已关闭降级丢库存与永久闩锁风险**：未来版本以显式 `unsupportedVersion` 控制字段锁停，状态字符串只用于显示；保存时回写原始未来版本并保留已知能量/进度字段，重新升级到支持版本会自动解除 quarantine。anvil / crucible 外部材料也改为不可用但可加载、可提取的 quarantine | 存档兼容 | T4a 关闭；真正 bump 时仍需为新增未知字段写迁移 |
| **O-14** | `HeatMaintenanceEvents.itemEntityTick` 监听全局 `EntityTickEvent.Post`，但已先做最低成本的 `instanceof ItemEntity` 过滤；**T3 范围外，维持观察项** | 性能观察 | 只记录；性能剖析命中后再改 |
| **O-15** | `zh_cn` 当前声明 131 个真实翻译（含 extruder shapes、11 类工具/图样、quarantine UI 与 T5 内容），相对 `en_us` 2,558 键覆盖率约 5.12%；不复制英文值伪装完整。完整中文化是材料与界面数据工作，应单独立项 | 本地化 / 数据完整性 | 未排期；现有测试锁定真实覆盖数与无英文占位 |
| **O-16** | **已完成当前 API 结论**：构造期材料/前缀事件只对显式排序在 CrucibleCraft 之前、监听器已注册的 addon 有交付保证；无序或更晚构造者可能完全收不到。限制已写入两事件 Javadoc 与相邻 API README；不把当前同步分发行为包装成稳定承诺 | 架构兼容 / 上游假设 | T4a 文档关闭；升级 NeoForge/FML 前仍需复核 |
| **O-17** | **已关闭**：`SidedFluidHandler.fill` 与 `drain(FluidStack)` 均先用 `SIMULATE` 规划并保存所有暴露 tank 的 pre-image，`EXECUTE` 后核对返回值与完整 post-image；任一违约或异常会恢复全部暴露 tank，回滚本身失败则明确报告潜在不一致。聚焦测试覆盖多 tank 分配/聚合、simulate 污染、execute 少填/少排、完整回滚、回滚失败诊断、成功回调恰好一次与失败零回调，并以总量断言守恒 | 事务原子性 | T5-readiness 关闭；仅不可回滚的恶意 `FluidTank.setFluid` 实现保留显式故障边界 |
| **O-18** | **已关闭**：仅显式白名单中的字符串组件可进入 `DataComponentIngredient` 二级索引；removed-only / 非白名单组件 / 未知 custom ingredient 按 recipe 逐条拒绝并记录 recipe id / map / ingredient 类型，核心 GameTest 锁 rejected = 0 / unindexed = 0，单条附属配方不再拖垮整个 epoch | 数据包兼容 / 诊断粒度 | T4b 关闭 |
| **O-19** | **本地审计已关闭**：三份旧 smithing hammer 配方已移除持久化 `minecraft:max_damage`，资源测试与 GameTest 锁定 stack patch 只保存材料身份；README 明确受支持读取契约为 `ItemStack.getMaxDamage()` / 对应 stack 行为 API。prototype 仍需 `MAX_DAMAGE=1` / `DAMAGE=0` 哨兵满足 vanilla `isDamageableItem()`，直接读取原始组件的第三方 tooltip、JEI/EMI 或背包整理集成仍可能看到 1 | 第三方兼容 / 表现层 | 本地持久化审计关闭；绕过 `ItemStack` API 的第三方读取器保留为兼容边界，按集成提供适配 |
| **O-20** | 大数值 progress / duration 改由客户端 block entity 精确读取后，刷新频率从 `ContainerData` 的每 tick 降为 `CHECKPOINT_INTERVAL=20` 的 block update；T3 的 64–240 tick 短配方会出现约 8%–31% 的可见步进。若要恢复平滑，在当前两槽后新增一个恒在 0..1000 的 progress 千分比槽供进度条每 tick 使用，精确值继续由 BE 提供 | 表现层 / 同步频率 | 不阻塞 T5；进入下一轮机器 UI 工作时处理 |
| **O-21** | **已关闭**：configured machine 的 `ContainerData` 已从 electrolyzer 的 17 槽收敛为 `status / statusArgument` 两槽；progress / duration / fluid identity / amount 继续读客户端 BE，capacity 读 spec，旧 aggregate 与逐罐 `tankData` 全部删除 | 性能 / 可维护性 | 当前轮关闭；O-20 的可选平滑进度槽独立保留 |

---

## 7. 阶段开工检查表

开一个新阶段时，逐条打勾：

- [ ] 判据写成一句话，包含「全链路 / 不是手写的 / 换材料不改代码」三个约束
- [ ] 判据可证伪（能回答"我怎么知道做完了"）
- [ ] 范围之外的清单写下来了（至少 5 条）
- [ ] 拆成 3–4 个子项，每个子项能独立验证
- [ ] 如果有形态/数量爆炸的可能，**先把数字算出来写在纸上**
- [ ] 任何跨所有权边界搬运资源的类（`IItemHandler` / `IFluidHandler` / `IEnergyHandler` / `AbstractContainerMenu`）必须有守恒测试：搬运前后两侧总量相等；部分成功正确；模拟与执行或对端返回值违反契约时响亮失败
- [ ] 悬案登记表过了一遍，决定哪些提升为主线
- [ ] 现状快照更新了
- [ ] 上一个阶段标记为 ✅ 已关闭

## 8. 阶段收尾检查表

- [ ] 判据那句话真的能跑通（GameTest 或等价的运行时观测，不是文件存在性）
- [ ] 泛化验证：换 2–3 个材料不改代码直接跑通
- [ ] 新增的台账数字接进了测试断言
- [ ] 本阶段新增断言做一次变异检查：把期望值改成明显错误值时，测试必须变红
- [ ] 本阶段的判断失误和本阶段修复引入的新成本都记进第 5 节（哪句话说错了 / 新成本从哪来 / 实际是什么 / 教训）
- [ ] 现状快照更新
- [ ] 悬案登记表更新

---

## 附：判断失误记录

> 记下来是为了避免重犯。写这一节不是自责，是把教训从记忆搬到文档里。

| 我说的 | 实际 | 教训 |
|---|---|---|
| `metadata_only` 判据用 `gt6_metadata.state` | 8 个误判（`ice`/`mercury`/`oil_sand` 等 state 是液气但有固体形态） | state 是间接信号，前缀覆盖是直接观测 |
| 门控能把 19k 压到 5–8k | 实际 14,473，只削减 24.5% | GT6 配方本身也是自动生成的，「出现在配方里」几乎等价于「存在」，判别力天然就低 |
| 报了 2 个 `metadata_only` 差异 + 5 个 flag 缺失 | 全是自写 snake_case 函数与项目不一致造成的误报 | 有权威映射字段就别自己重算 |
| 磁盘字符串验收足以证明配方可用 | 换成 live `RecipeMap.findMatch` 后立刻捕获 crusher 多输出非法配方 | 断言运行时状态，不是构建产物 |
| tungsten 会由 `COMMON_ORE` 规则自动进入 ore 域 | tungsten 有 `ITEMGENERATOR.ORES`，但没有 `PROPERTIES.COMMON_ORE`；最终用显式 `include_prefixes: ["ore"]` 表达设计选择 | 生成事实规则和策划例外要分开，例外必须声明且可反向校验 |
| 声明 `ore` 就等于矿链闭合 | 方块能注册不代表 raw / crushed / washed / centrifuged / purified / dust / ingot 都有生产者 | 新入口必须做中间形态闭包和运行时端到端验收 |
| 给 tungsten 新增矿源会让旧的「263 个无矿源 crusher 入口」减一 | tungsten 不在那个旧 crusher 集合里，集合差不会因此减少；扩展到真实 processing candidates 后，待分类全集实际是 349 / 162 | 台账变化先写集合定义，再看元素属于哪一侧；不能凭“新增了一个矿”做算术 |
| GT6 原始 gate 会让 iron 自动生成 wire | 原始 gate 没有该事实；四材料 T3 验收要求 iron 的普通 wiremill 路线，因此以 `t3_acceptance_required_not_gt6_original_gate` 显式修正，并同步 importer / gate / builder | 数据事实与验收策略必须分层；策划修正要有分类、原因、展开增量和反向校验 |
| 三条 T3 黄金摘要先断言 `length() == 64` 就算完成 | 任何 SHA-256 十六进制摘要长度都恒为 64，8,141 条配方怎样漂移都不会红；现已改为三个提交的黄金摘要 | 为判据写的工具代码不等于判据本身；收尾必须对新断言做一次错误期望值变异 |
| 用 `zh_cn` 与 `en_us` 键集相等修复“中文覆盖不可见” | 2,225 个英文占位把文件补到 2,256 键，但真实翻译仍只有 31，且原本明显的 31 / 2,256 差距被隐藏 | 收到覆盖反馈时先区分覆盖率与可见性；修复后必须保留能发现问题的直接观测量 |
| 把偏序贪心改成“极大元歧义即响亮失败”是纯粹改进 | 需求集不相交的多条配方同时可做是正常运行态；在每 tick 的玩家输入路径抛异常会把选错配方升级成崩服 | 响亮失败要放在最早能完整判定不变量的位置；相同签名在发布期拒绝，运行期不可比较候选采用明确的稳定策略 |
| 加入便携罐就等于关闭流体运输 | 交互先按“容器仍有余量”选择 DRAIN，导致未装满的 64,000 mB 便携罐不能把流体填回机器；原测试还锁死了错误分支 | 新能力必须有端到端目标判据；“A 产出的流体能被 B 消费”比策略分支表更接近真正要证明的量 |
