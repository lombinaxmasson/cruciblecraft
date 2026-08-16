# CrucibleCraft 阶段档案 · T7–T9

> 从《CrucibleCraft-第二阶段总体规划》迁移的已关闭阶段判据与关闭快照
> 归档：2026-08-05　关闭结论与历史数字保持不变
> 当前执行入口：《[CrucibleCraft-第三阶段总体规划.md](CrucibleCraft-第三阶段总体规划.md)》

---

## 0. 阅读说明

- 本档案保存 T7–T9 关闭时使用的完整判据、交付、范围、完成信号和关闭快照。
- 文中的数字是对应关闭快照的历史事实，不替代《[CrucibleCraft-第二阶段总体规划.md](CrucibleCraft-第二阶段总体规划.md)》的阶段最终快照或第三阶段当前账目。
- 已关闭阶段仍可保留独立的保真或载荷债，例如 T9 几何 O-29；这不会重开已经证明的闭包轴。
- 正文中的 RU/KU→`KINETIC` 与 `methane` 矿床是当时快照：O-30 已由 T12 首批能源分域关闭，O-38 已由 T11 的 `natural_gas` identity、旧 NBT 迁移与 generifier 553 关闭；详《[CrucibleCraft-阶段档案-T10-T12.md](CrucibleCraft-阶段档案-T10-T12.md)》。
- 历史正文中的章节号按迁移前语境保留；当前不可违反的方法论与架构规则以《[CrucibleCraft-总体规划.md](CrucibleCraft-总体规划.md)》第 5、7、8、9 节为准。

---

## 1. 已关闭阶段判据与快照

### T7 · 材料事实进入规则语言 ✅

**判据**

> 在 `MaterialRule` 里写一条「所有带 `PROCESSING.MORTAR_GRINDABLE` 且已注册 `ingot` 形态的材料，都有 mortar 配方」的规则，跑一遍构建，live map 上 `findMatch` 能命中，且展开数与从材料数据独立推导的期望集合双向相等；新增或修改一个材料标签只改数据，不改 Java。

**为什么排第一**

它最便宜（不新增方块、不新增物品、不动世界生成），但后面每个阶段都要用它。T8 的管道要按材料属性决定容量、温度与 `ACID` 行为，T11 的油气要消费 `PROPERTIES.FLAMMABLE`，T12 的机器分级要按材料域选材。**先把语言修好，再去说话。**

**依赖**：无。可以立即开工。

**交付**

- 沿用已有 `material.tag(...)` 与 `material.thermal.*` 数值比较，不新增重复别名；补入 `material.explosion_damage` / `material.heat_damage`
- 62 个 `PROCESSING.*` / `PROPERTIES.*` 标签逐个分类为 **rule-input / build-time-only / not-applicable / deferred**，`unclassified = 0`，附来源 revision 与理由
- `formula`（952 个）接入 tooltip；其中 605 个已有注册形态并对玩家可见，347 个等待 T10 载体形态
- `EnergyType` 的 `ROTATION` / `MAGNETIC` / `COOLING` 做出去留决定，并写进本卡（见下方开工门禁）

**开工门禁**

1. 三个零引用的 `EnergyType` 常量：要么在 T7 内给出实现阶段编号（进 T11 或 T12 的判据），要么删除并在 commit 里写清理由。**不允许带着三个不可证伪的声明进入第二阶段。**
2. 62 个标签的分类台账先落地，再写规则。顺序反了就会变成「先写规则再找哪些标签能用」，那是间接信号。

**先算的数字**

- 62 个标签中，能直接作为规则输入的有几个？把这个数写在纸上再动手
- 如果 `MORTAR_GRINDABLE`（603 个材料）真的展开成配方，是多少条？是否顶破 T7 当时的分层预算；最终以关闭快照中的 256 / 21,000 为准。

**范围之外**

- 不实现任何新前缀、新物品、新方块
- 不动 T3/T4/T5 已发布的 16,969 条配方（新规则只允许**新增**，不允许重写既有展开）
- 不做等离子体（`plasma_point` 留到更后面）
- 不做 `ATOMIC.*` 的化学推导（那是 T10 之后的事）

**完成信号**

1. 62 / 62 标签分类完成，`unclassified = 0`，来源可追溯
2. 至少两条新规则完全由标签驱动，展开集合与独立推导的期望集合双向相等（多一条报错，少一条也报错）
3. 三个 `EnergyType` 常量的去留已落定并写入 policy/readiness 台账

**关闭快照（2026-08-04）**

- 固定 GT6 revision 下 62 / 62 标签已分类：30 rule-input / 7 build-time-only / 4 not-applicable / 21 deferred，`unclassified = 0`
- authored mortar 规则展开为 ingot→dust 126、gem→dust 94，共新增 220；独立 `T7_AUTHORED_MATERIAL_RULE_BUDGET = 256`，高版本矿块入口补正后的基线为 17,106，生产 reload 对应 mortar 471→691、全局 publication 17,106→17,326，低于 21,000
- 当前全资源树 2,848 份 MaterialRule 中，65 份跨材料、53 份带条件、2,783 份材料特定；新增 `has_registered_for(selector, prefix)` 后，22 份 component target-transform 规则已回填目标材料注册形态守卫：有守卫的缺形态是显式不匹配，无守卫的 required target 缺形态仍为加载错误。所有 `material.tag(...)` 引用都必须属于 101-tag 词表。`crushed_to_dust` 明确归属 T2 ore-chain，不受 `MORTAR_GRINDABLE` 管辖，并记录 GT6 / CC 行锚点
- T8 复核确认 extruder 的 2,782 条不能无损压成 11 条 shape-only 规则：稀疏源至少需要 27 个 IO 变体，若按 `EXTRUDABLE_SIMPLE` 拆 EU/t 约 54 条；duration 明确为 `formula_verified=false` 的逐材料 exact lookup，另受 ingot/dust/gem 输入选择、`mTargetForging`、`plate`/`plateGem` 与模板 shadow 顺序约束。所谓 plate 集合“各差 5 个”中，4 个是 forging 重定向、1 个是 `plateGem` 未注册，另 5 个是合法 ingot fallback；在规则语言能表达这些门禁前保留 replay-verified sparse relation，并由测试锁定该决策
- iron / amber / cinnabar 均通过真实 `RecipeMap.findMatch()`、供能、声明时长和输出转移；无标签 ruby 不命中；39 / 39 GameTest 通过
- `EnergyType` 在引用闭包上收敛到 4 / 4；当时 policy 将 RU 映射为 `KINETIC`。2026-08-05 的固定来源复核发现 GT6 还区分 RU / KU，因此只重开 O-30 保真债，不重开 T7 的零引用清理与标签闭包
- `formula` 共 952 份事实，其中 605 份玩家可见、347 份等待 T10 载体；O-22 零损耗传输已修复

---

### T8 · 物流骨架 ✅

**判据**

> 在材料数据里声明一种材料有 pipe 形态，跑一遍构建，生存里能造出该材料的流体管与物品管；管道按该材料的 GT6 容量 / 温度上限传输，超限按已声明策略可观测地失效；从 distillery 产出的流体能经管道自动进入 mixer 并被消费；铜 / 锡 / 铁三种材料不增加材料专用 Java。

**为什么排第二**

见 1.3 ①。这是唯一一个「不做它，前面六个阶段的成果就用不上」的阶段。

**依赖**：T7（管道属性用标签和 thermal 驱动）

**交付**

- pipe 前缀族进入统一材料管线（gauge 分档同 cable 的做法）
- 流体管：容量 / 温度上限 / 内容物腐蚀行为由材料事实驱动
- 物品管：吞吐上限、堵塞语义
- **cover 系统的最小可用面**：至少覆盖「过滤 / 单向阀 / 输出泵」三种，因为没有它管道只是水管不是逻辑
- 复用 T6 的骨架：loaded-only 稳定遍历、逐段属性衰减、每次注入独立 visited、端点契约违约限频降级不抛异常

**先算的数字（开工前必须落纸）**

- GT6 的 pipe 前缀分几档？× source-backed 材料数 = 多少个方块？多少 logical states？
- 对照 T6 的教训：当时先算出 `1,773 × 5 = 567,360` 不可行，才收敛到 144 blocks / 9,216 states。**pipe 必须走同一道算术，且要在写第一行 Java 之前算完。**
- cover 是「每个管道面一个状态」还是「block entity 侧数据」？前者会让 states 爆炸，后者要设计持久化

**范围之外**

- 不做管道的视觉多方块合并渲染
- 不做无线 / 跨维度运输
- 不做 GT6 全部 cover 类型，只做三种最小可用面并显式声明其余为未实现
- 不动电缆的既有语义（cable 与 pipe 共享遍历代码，但不合并成一套「通用网络」——那是过度抽象，等第三种网络出现再说）

**完成信号**

1. 「distillery 产流体 → 管道 → mixer 消费」端到端 GameTest 通过，不是文件存在性
2. 三种材料的管道路线不增加材料专用 Java
3. 管道超温 / 超容量按声明策略可观测失效，且失效不抛出 block-entity tick
4. 物品管 + 三种 cover 组成的最小自动化回路能跑满一轮配方

**关闭快照（2026-08-04）**

- 固定 GT6 `3703e403...` 与 GTM `de5d2c4a...` 来源；`gtceu_code` Git 跟踪集合为空，运行时无 GTM 依赖
- 5 档流体管 + 3 档物品管由 63 个 source-backed 材料投影为 210 + 72 = 282 个方块，六向连接共 18,048 logical states，均低于 300 / 20,000 门禁；六面 cover 保存在 BE NBT
- 8 条通用 MaterialRule 展开 185 条流体管与 72 条物品管配方，共 257 条；独立 T8 pipe 预算为 320，全局 publication 17,326→17,583 / 21,000，shadow signature 0
- carbon / plastic / rubber / wood / wood_treated 的 5 档流体管共 25 个形态保持 creative 可见，但 GT6 五档 `recipe=false`，当前无生存获取路线；按 O-27 记录，不伪造 extruder 配方
- GTM 式 item route cache 与逐段 fluid buffer、filter / valve / pump、实际提交计量和确定性失败状态均已落地；distillery→fluid pipe→mixer、铜/锡/铁通用目录、超限失败与完整物品自动化回路 GameTest 通过

---

### T9 · 世界生成数据化 ✅

**判据**

> 写一份矿脉声明文件，跑一遍构建，生存里能挖到对应矿脉的双宿主矿石；129 条已分类为 `vein` 的材料全部有真实 configured feature 与放置证据，`unclassified = 0`；新增矿脉与流体矿床只改数据，不改 Java。

**依赖**：开工时无强依赖；实际在 T8 关闭后启动，始终满足「最多一个阶段进行中」。

**交付**

- 5 个手写 vein → 数据驱动的批量矿脉生成器
- 129 条 vein 分类逐条闭合，与 T2c 台账双向校验
- **流体矿床**（油 / 气）的 worldgen 形态：储量、深度带、宿主判据
- 宿主岩从 2 种（stone / deepslate）扩展的决策：**先算数字**——每加一种宿主，矿石方块数 × 137

**先算的数字**

- authored MaterialRule 统一使用 `tN/` 路径分类：T7 实际 220 / 预算 256，T8 pipe 实际 257 / 预算 320。高版本矿块 crusher 入口使当前 publication 增加 137 至 17,583。T10 预投影按 `INGOTS_HOT ∩ registered ingot = 321` 的两条 route 得 642，按 `MULTIINGOTS ∩ registered ingot = 323` 的 double/triple route 得 646；已知 post-T10 publication 为 `17,583 + 0 + 642 + 646 = 18,871`。沿现有 10% 后向上取整到千位，T9 开工前全局预算仍为 21,000，已知路线后余量 2,129。三个 `CONTAINERS*` 域 204 个 membership / 123 个去重材料只计形态压力，实际容器配方仍须在 T10 设计时落账
- 129 条 vein 各生成 configured / placed feature，共 258 份；2 个流体矿床再生成 4 份，另有 1 个聚合 biome modifier，T9 共新增 263 份运行时 worldgen 资源
- 129 条 catalog vein 采用 `region_size_chunks=32`、`generation_chance=0.75`，期望 `0.094482421875 / chunk`；加上原有 5 个 family 后总计 `0.142012722329 / chunk`，约每 7.04 个区块命中一次

**范围之外**

- 不做新维度
- 不做矿脉勘探工具（GT6 的 prospector 留到 T12 之后）
- 不扩展宿主岩数量，除非数字算完后明确决定
- 不做植物 / 农业类 worldgen（`ITEMGENERATOR.PLANTS` 1,179 个材料整体推迟，见第 5 节）
- 不在 T9 内把 GT6 worldgen dump 映射为逐矿脉几何；129 条 catalog 当前使用统一占位几何，来源导入登记为 O-29

**完成信号**

1. 129 / 129 vein 分类闭合，从运行时 configured-feature registry 取出并真实放置验证
2. 原油矿床真实放置后能读取材料、初始/剩余储量和被替换宿主；抽取设备仍属于 T11
3. 新增一个矿脉声明文件不需要碰 Java

**关闭快照（2026-08-04）**

- `worldgen_catalog/ore_veins.json` 与 `fluid_deposits.json` 成为 authored 单源；builder 对 T2c closure、材料能力、注册门禁、ID / salt 和输出树做双向校验
- 129 个 catalog vein 与原有 5 个 family 覆盖 137 / 137 ore material；继续使用 stone / deepslate 两种宿主，矿石方块保持 274
- 129 条 catalog 的 `hr=5 / vr=2 / density=0.22 / y=-48..48` 与四层单矿布局是统一 CrucibleCraft balance placeholder，不是 GT6 source-backed 几何；O-29 负责导入与映射固定 revision 的 worldgen dump
- `crude_oil` / `methane` 通过通用 subsurface-fluid feature 生成；隐藏 marker block entity 持久化材料、初始/剩余储量和原宿主
- 新增 263 份生成资源；worldgen readiness 与生成树均为 current，45 / 45 GameTest、421 项 Java 单测、236 项 Python 单测通过
