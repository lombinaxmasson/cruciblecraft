# 未实现与尾账索引

> **人读权威缺口页。** 本页必须与磁盘对齐；过期即错。
> 机器主键仍是 `tools/capabilities/**/capability.json` 与
> `tools/blockers/catalog.json`。[project-status.md](project-status.md)
> 与 [blocked.md](blocked.md) 是它们的投影，不要手改。
> unique-active、prep 文件列表与 `player_complete` 以状态页为准；
> blocker 排期以 catalog / blocked 为准。本页解释那些投影该怎么读，
> 并列出投影里容易漏掉的当前缺口。
> `*_READY` 只表示分母、来源或可行性已经冻结，不表示对应内容已经进游戏。
> Prep 计划文件存在 ≠ runtime 没做。R0 disposition 标签 ≠ 剩余 dummy。

本页只保留：当前未实现、仍 blocked 的余量、非阻塞但容易误读的残余分母，
以及选择后续内容时必须知道的依赖。已经完整交付的能力不在这里展开；已关闭卡只
通过权威状态页或历史计划引用，不在本页重复做阶段日志。

## 0. 读法与当前边界

| 状态 | 含义 |
| --- | --- |
| `frozen` | 分母、来源或可行性已冻结，尚未因此获得 runtime |
| `runtime_ready` | `src/main` 主机或机制可运行，但内容、配方或玩家路径仍可能 blocked |
| `player_complete` | 生存可获得、可运行、可存档，并满足对应 production lock |

当前边界：

- unique-active 以 [project-status.md](project-status.md) 为准，本页不复述。
  状态页 Prep 里仍列着 `machines/printer`、地牢 / 行星岩 / Center，以及
  `content/mte-prep-index` 下 12 张 MTE 家族 prep **计划文件**。
  那些 prep 文件是 2026-09-11 的冻结合同，**不是**「家族 runtime 还没做」。
  对应 `content/gt6-mte-*-runtime` / `*-host-fold` 已关 `runtime_ready`。
  地牢退回 prep：结构载体在仓库里，房间几何与 GT 石材形态未完成，
  不得宣称 `runtime_ready`。PUV 六张 capability 是 `frozen` + `paused` 的
  CC 扩展，计划文件也还在 Prep；不要读成原版 PUV2+ 科技线或
  `player_complete`。语言键与显示名规范收口
  （`localization/language-key-display-name-normalization`）是现行 unique-active，
  已关 `runtime_ready`：点号 translation key 与显示名规范已进生成器；
  关闭 `player_complete` 时才删 dual slash/dot lookup。不改 registry / 模型 /
  loot / 存档 ID，不得把手改 generated lang 当成实现。
- 跨域审计 [GT6 管道与线缆语义重基线](../history/card-plans/closed/GT6管道与线缆语义重基线详细计划.md)
  已关闭（无 capability）。它冻结了 GTCEu 不得覆盖 GT6、四套网络隔离、
  以及 catalog 666 行 live/dummy 处置。流体 / 物品 / EU / 红石 runtime 与
  获得格 child **已经按该 envelope 落地并关** `runtime_ready`，不是
  `player_complete`。不得再把 GTCEu 合同当成 GT6。
- `player_complete` 清单只看 [project-status.md](project-status.md)，不在本页复制。
- catalog **1,817 个 MTE 身份**是独立的身份分母，不等于机器 overflow，也不等于
  1,817 个待实现机制；详见第 0.1 节。
- 实现必须另开 runtime / 内容卡，补齐 Source Pack、production lock、玩家路径、
  load 和 fresh capability profile；不得从冻结卡直接推导 stand-in。

权威入口：

| 用途 | 权威 |
| --- | --- |
| 当前 active / prep / `player_complete` | [project-status.md](project-status.md) |
| 跨能力 blocked 总账 | [blocked.md](blocked.md)（排期桶 A–D） / [`catalog.json`](../../tools/blockers/catalog.json) / [`ledger.json`](../../tools/blockers/ledger.json) |
| 批处理关系 | [`batches.json`](../../tools/blockers/batches.json)；只登记当前 open scope，已关闭条目先在 `closed_before_batch_selection` 对账 |
| 状态与交付流程 | [capability-delivery-workflow.md](capability-delivery-workflow.md) |
| MTE 身份分母 | [`disposition_ledger.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json) / [`family_map.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/family_map.json) |
| 机器尾账 | 各 `tools/waves/machines/**/readiness.json`、`overflow.json` |
| GT6 语义与形态 | 本地 `gt6_code/gregtech6` 与对应 source-backed artifact |
| GT6 贴图迁移 | [gt6-art-policy.md](gt6-art-policy.md) 与本地 `gt6_referencable_port_code/gregtech6_w` |

## 0.1 分类总览（2026-09-14）

人读分类必须跟 blocker catalog 与 capability JSON 一致。blocker 机器权威是
[blocked.md](blocked.md) 与 `tools/blockers/ledger.json`（源是
`tools/blockers/catalog.json`）。排期看 `planning_bucket`（A 规模不是待办，
B 可抽 unique-active，C 先审计分母，D 不是活），不要按 `count` 选最大的卡。
本页不再复述那份总账。数字以各波
`readiness.json` / `overflow.json`、[project-status.md](project-status.md)、
`tools/capabilities/**/capability.json` 以及
[`disposition_ledger.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json)
为准。本节只分类，不关卡、不改分母、不把 overflow 行数写成新机制数量。

四块：**机器配方尾账**、**MTE catalog 分母 1,817（runtime 波次已过）**、
**高压零件与 CC 扩展（frozen，不晋级）**、**下一阶段内容切片**。
1,817 是 `gt_mte` catalog 行，不是织机/层压机 overflow。

现行 unique-active 见 [project-status.md](project-status.md)。盖板余量、Sensors、
熔炉、打磨机、技术中间件基础、树与作物已关 `runtime_ready`。
连接件与 MTE 家族 runtime child 同样已关 `runtime_ready`（清单见状态页）。
地牢退回 prep。印刷机与行星岩 / Center 仍是 prep。
高压零件与 HEX/蒸汽/聚变/量子物质是 `frozen`+`paused` 的 CC 扩展，
不是下一张 unique-active。

### 历史 candidate 与 current closeout 的读法

正常排期不需要人工回查 T48/T49。当前 blocker 的状态应由 current
`tools/blocked_recipe_ledger.json`、`tools/blockers/catalog.json` 和
`tools/blockers/batches.json` 自洽给出；历史 candidate / lock 只用于追溯，不是
当前排期输入。若这些 current projection 互相矛盾，应当让校验失败并修复生成链，
而不是让每个排期者自行翻历史卡。

Bath 只是这条数据链的回归样例：旧的 150 / 5 数字仍可在历史 candidate 中找到，
但 current recipe ledger 的 `bath_remainder` / `bath_identity` blocked 计数都是 0，
所以它们只能作为 resolved 历史范围，不能进入新的 blocker 批次。

#### 2026-09-14 这次误判的原因与修正

这次的主要故障是**收口后的投影没有同步**，不是流程要求排期者回查历史：

- `tools/blocked_recipe_ledger.py` 仍读取旧的根级 candidate selection，把历史
  R0 分母当成 current blocked 输入；T49 之后的 current selection 没有接管它。
- recipe ledger 更新后，`tools/blockers/catalog.json` 没有被同一条校验链检查，
  因而 catalog、recipe ledger、batch index 可以同时保持“各自格式正确”但语义
  不一致。
- 我在这个 stale catalog 上继续做了数量排序，放大了投影同步故障；这属于错误
  地信任 stale current projection，而不是应该由人工补做 T48/T49 审计。

修正现在落在生成链上：recipe ledger 改读 current selection；blocker 校验增加
recipe-ledger ↔ catalog 一致性门；resolved blocker 不能进入 `batches.json`；
旧 candidate 只保留为历史解释。相关代码是 `tools/blocked_recipe_ledger.py`、
`tools/blockers/__init__.py`，current 规则见 `recipe-wave-workflow.md`。

### 当前机器配方尾账

获得格（主机能不能做出来）和配方表是两套账。技术中间件卡只接通 ULV–IV 中间件
获得格。行数是 dump / selected / overflow，不是「还要做 N 个新机制」。

| 能力 | 声明 | 主机获得 | 配方余量（权威 JSON） | 真实原因 |
| --- | --- | --- | --- | --- |
| 切片机 `machines/slicer` | `runtime_ready` | 五档 `source_exact` | dump 33 / selected **33** / overflow **0** | `paper:tiny_plate` 已由 `content/gt6-paper-tiny-plate` 按 GT6 `OP.plateTiny.forceItemGeneration(MT.Paper)` 注册。关闭目标不是 `player_complete` |
| 注射机 `machines/injector` | `runtime_ready` | 五档 `source_exact` | dump 638 / selected 103 / overflow **535**（其中 runtime operand 508） | 未映射流体（`ic2coolant` / `soda` / `thoriumsalt` 等）、shadow 行、未注册运行时操作数 |
| 织机 `machines/loom` | `runtime_ready` | 九档 `source_exact` | dump 1334 / selected 465；MTE+纤维 **232** + shadow **637** | 未映射导线/电缆 MTE、`plant_gt_fiber`、shadow 签名 |
| 层压机 `machines/laminator` | `runtime_ready` | 四档 `source_exact` | dump 498 / selected 438；MTE **54** + 运行时操作数 **6** | 未映射 MTE；6 行原木块操作数 |
| 熔融机 `machines/melter` | `runtime_ready` | 一档 `source_exact` | dump 6756 / selected 3960 / overflow **2796** | 12 行未注册的 multiitem / GT block 身份与 1 行超过 8000 mB 出液罐上限的关系已明确 blocked；其余规模 `UNVERIFIED_SCALE`，不得把 2796 抄成待办清单 |
| 纳米加工机 `machines/nanofab` | `runtime_ready` | 五档仍 `explicitly_blocked` | dump 64 / selected 7；石墨烯/MTE **12** + shadow **45** | 获得格仍缺 `IL.Comp_Laser_Gas_Ar/Kr/Xe` 与蓝宝石晶体处理器。`compact_signal_emitter_*` / `compact_sensor_*` 0–5 已存在，但 D0 / `machine_tiers.json` 文案仍写发射器/传感器 missing（过期）。石墨烯 `small_casing` / `curved_plate` 仍 blocked |
| 压力清洗机 `machines/pressure-washer` | `runtime_ready` | 四档 `source_exact` | dump 312 / selected 192 / overflow **120** | 未映射 GT 石头 |
| 打磨机 `machines/sanding` | `runtime_ready`（已关） | 四档 `source_exact` | dump 7637 / selected 7637 / overflow **0** | 工具头 remap、sharpener 所需材质形态和 14502/14512 MTE overlay 已按真实对象补齐。Grindstone `32703` 不在本卡。关闭目标不是 `player_complete` |
| 熔炉 `machines/oven` | `runtime_ready`（已关） | 四档 `source_exact` Heat_T | dump `mc.recipe.furnace` **0**；live 为原版 `SMELTING` 快照 | 烹饪油肉类加成与 XP 流体 blocked。不替换原版熔炉方块。20001–20003 不在 R0 ledger。关闭目标不是 `player_complete` |
| Sensors `content/sensors` | 已关 `runtime_ready` | 21 个 GT6 Sensor MTE | 无 RecipeMap | Electro_Meter / Tacho_Meter 与 Gibbl/质量/转速已按真实对象接入。ComputerCraft 外设仍 blocked。不得宣称 `player_complete` |
| 印刷机 `machines/printer` | **prep** | 五档 `source_exact` | dump 22 / selected **0** / overflow **22** | 全部 `fluid dye.chemical.*`，`out_of_scope_g10_dyeing`。无 live family。盖板 `compact_electric_conveyor_*` **就是** `IL.CONVEYERS` 槽 |
| 导线电缆 MTE 折回 `content/electric-wire-cable-mte-fold` | `runtime_ready` | — | Loader id mapped **259** / unmapped **61** | 只折配方操作数；mapped 不是 dummy 删除集合，须另判 live BlockItem。不等于 catalog 1,817 已折完 |

切片机 / 注射机 / 织机 **不再卡 0–5 档电机、活塞、传送带**。橡胶板当前仍走
`press/slime_ball_to_rubber_plate`（`component_rules.json`：
`survival bridge pending GT rubber-tree`）。
`machines/roll-former` 的 `railGt` **2** 行是非阻塞尾账；能力状态仍由
[project-status.md](project-status.md) 管理，不列入当前交付缺口。

连接件规格、HSLA 折回、MTE 精确宿主折回与 in-place family runtime 落地后，
重新计算 loom / laminator / injector overflow：**0** 行因这些 identity 变绿。
织机 869 / 层压机 60 / 注射机 535 仍 blocked（未映射 MTE meta、shadow 签名、
未注册流体、缺线规电缆等），不得把这些整数抄成待办。石墨烯 `wireGt07` 与
HSLA `cableGt12` 不在已落地的缺线规集合里。注射机 overflow 仍是玻璃 /
`ic2coolant` / soda / thoriumsalt / shadow，**0** 行是 MTE。

### MTE catalog 分母（1,817；runtime 波次已过，不是 1,817 个待办）

权威分母仍是 [`disposition_ledger.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json)
与 [`family_map.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/family_map.json)。
R0 计划 [MTE 身份分母处置 R0](../history/card-plans/closed/MTE身份分母处置R0详细计划.md)。
这是原 `gt_mte/mte_*`、现由 `catalog_modern_id_map.json` 定址的 catalog
**holdable 身份**，不是加工机 overflow 行数。不得把 1,817 写成 1,817 个
待实现机制，也不得整表开工。

R0 标签是 2026-09-11 快照，runtime 卡写明 **Do not edit R0**，所以个数
不会跟着折回变绿：

| disposition | 个数 | 现在怎么读 |
| --- | --- | --- |
| `identity_only` | **1,501** | 当时的散落 / 展示身份标签。其中能对上 live BlockItem 的已经折回或原地做成活方块；剩下才是 dummy / 未开门规格。**不是**「还有 1,501 个铁锭」 |
| `realized_natively` | **283** | CC 在该领域已有机制。不能整类自动折回（坩埚不能吞进陶瓷、错误 kind 不能按名合并） |
| `attachment_candidate` | **33** | 标签未改。流体附件 runtime 已由 `content/gt6-mte-fluid-attachments-runtime` 关 `runtime_ready`；不得写成「游戏里没有龙头」 |
| `deferred` | 0 | — |
| **合计** | **1,817** | `unmatched = 0` |

[MTE 全量 Prep 总索引](../history/card-plans/prep/MTE全量Prep总索引.md) 与 12 张
家族 prep **计划**仍冻着 2026-09-11 的合同（1,534 行规划 + 283 行
`realized_natively` 审计）。那些文件当时规定不创建 capability、不改
`src/main`。那是 prep 车道的历史合同，**不要**拿来当 9 月 14 日之后的
剩余清单。

`registry/catalog-modern-ids` 已关 `runtime_ready`。改名时不合并活主机，
所以仍可能同时存在活形态（`{材料}/fluid_pipe` 等）与未折完的 dummy
（`fluid_pipe_tile/…` 等）。JEI 双物品只算还没 `fold_live_block` /
`in_place` 的债。铁锭不是可发行美术。24 个 Bath 重叠 `existing_item`
指向 Bath 已注册 dummy；meta 25302 Osmium 现为
`item_pipe_tile/osmium_elemental`。

家族个数（`family_map.json`，合计 1,817）没变：connector 663、
furniture_storage 563、hopper 101、processing_machine 86、
crucible_foundry 85、energy_converter 79、multiblock 74、drive 63、
misc_tool 39、fluid_attachment 33、decorative 24、redstone_wire 3、
extender 2、reactor 1、untyped 1。drive 活目录是轴 36 + 齿轮箱 9 +
旋转引擎 9 + 旋转变压器 9；CC 旋转能是另一套对象。decorative 的
Wooden Panel 23 只计一次，不是 exclusion-reclaim 的 Panels 348。

#### 已关 `runtime_ready` 的 child（不是 `player_complete`）

清单以 [project-status.md](project-status.md) 为准。按家族：

- **连接件**：流体管 / 物品管 / EU 线 / 裸红石线 runtime、combo 4/9 罐、
  catalog 内 6 行 restrictive、gated 缺线规、HSLA 漏匹配、绝缘红石
  27006/27056/27506、危险介质、流体/物品管获得格、EU 电缆获得格、
  绝缘红石获得格、连接件美术。轴/齿轮箱与红石线对 KU / 原版红石是
  「像但不是」，keep-both + 迁 GT6 图。
- **折回**：漏斗 101、转换器 71 活主机、反应棒、加工机 `sourceId` 对齐的
  68 个 meta（含 Polarizer 20221–20225、MagSep 20301–20305）。
- **原地 in-place**：流体附件、家具箱/柜/台/架/桶/储物、装饰、传动、
  扩展器、多方块、坩埚铸造、杂项工具、转换器余量。Chest/Safe/Table/Scaffold
  **已有**活 BlockItem，不是「无宿主」。

`content/gt6-pipe-cable-baseline` 仍是已关闭的 prep 审计（无 capability），
没有把管/线晋级为 `player_complete`。

#### 现在还剩的 MTE 余量

- 加工机未开门家族（`identity/processing-ungated-families`）：Hammer /
  Squeezer / Laser 共 18 个 dummy meta。Polarizer / MagSep **已经折到**
  活主机。
- 连接件：配方操作数 mapped **259** / unmapped **61**（mapped 不是 dummy
  删除集合）。restrictive Loader-out 57 行仍冻结。石墨烯 / 超导 EU dummy
  与 HSLA 未开门线规仍 blocked。层压机获得格与 torch/repeater 宿主仍 blocked。
- in-place 获得格：`obtain/mte-inplace-runtime` 已由
  [`MTE In-place 获得格收口`](../history/card-plans/closed/MTE原地获得格详细计划.md)
  关成 `resolved`。732 条 source-exact 工作台格在 live `RecipeManager`；
  150 个缺形态 / plank / 润滑剂 / OD 缺口留在各族 `current_gap.json`。
  14 张 runtime 仍是 `runtime_ready`，不是 `player_complete`。
- 织机 / 层压机剩余 unmapped 行是缺规格或未注册材料，**不是**
  「1,817 没折完」。注射机 overflow **0** 行是 MTE。
- `realized_natively` 去重审计入口仍在：connector 46、hopper 101、
  crucible_foundry 85、processing_machine 50、reactor 1。

### 高压零件与 CC 扩展（已落地，frozen，不是原版 PUV2+）

**LuV ≠ `wireGt07`**：LuV 是电压档 `VN[6]` / `Electric_T[6]=Ir`；`wireGt07`
是七线规。盖板 `CoverComponentTiers` 仍是十档（ULV–PUV1）。GT6 原版没有
PUV2+ 科技线，只有 `VN[14]=XV` 这种枚举位。

`content/technological-parts-foundation` 已关 `runtime_ready`：ULV–IV
紧凑零件。`content/puv-omega-parts` 把紧凑电零件做到 **index 14（OMEGA）**，
并注册 `circuit_quantum`。`neutronium.json` 已在材料树里。石墨烯有
`generates_quadruple_wire` 等线规，**没有** `generates_ingot`。
`gt6_prefix_mapping.json` 已含 `wireGt07` / `09` / `10` 等。

同一次 closeout 还把大型热交换器 17197、蒸汽涡轮、聚变 18 行 + CC 中性物质
扩展、QUANTUM / 物质制造放进仓库。对应六张 capability 是 **`frozen` +
`paused`**，当时 unique-active 为空，**不是** `player_complete`，也不得当成
原版 GT6 高压线已完成。计划文件仍在 Prep：
[PUV2+ / OMEGA 科技线](../history/card-plans/prep/PUV2OMEGA科技线详细计划.md)。
主机 ID 带 81000+ 的是 `CC_EXTENSION`。

切片机 / 注射机 / 织机 / 印刷机 / 纳米加工机源注册仍是 `Electric_T[1..5]`
（最高 IV）。高压零件**不会**把那些 overflow 转绿，也解不开纳米加工机的
`IL.Comp_Laser_Gas_Ar/Kr/Xe` 与蓝宝石晶体处理器。不要把 PUV 再拉进下一张
unique-active。

仍 blocked、且与高压相关的余量见 [blocked.md](blocked.md)：
`identity/eu-blocked-gauges`（石墨烯/超导 dummy）、
`identity/electric-unregistered-gauges`（61）、`obtain/nanofab-hosts`。

### 下一阶段内容切片

长链（Trees → Sensors/Panels → Crops → Food → 维度/Bees/Portals/Center）仍在第 0 节。
这里只写当前分母清楚、能切成垂直切片的项。

| 项 | 分母 | 读法 |
| --- | --- | --- |
| GT 树（九棵，已关 `runtime_ready`） | `WorldgenTree*` **9**（rubber / maple / willow / bluemahoe / hazel / cinnamon / coconut / rainbowood / bluespruce） | 已关 `runtime_ready`，不是 `player_complete`。计划 [GT 树详细计划](../history/card-plans/closed/GT树详细计划.md)。9 个 feature ≠ 9 种木材配方。含三树洞。不要带地牢 1、行星岩 3、Center 5。史莱姆→橡胶板桥本卡不删 |
| GT 地牢（prep / `frozen`） | `WorldgenDungeonGT` **1**（`overworld.structure.dungeon.large`） | 结构，不是 scatter / 矿脉。计划 [GT 地牢详细计划](../history/card-plans/prep/GT地牢详细计划.md)。Structure + 多区块 Piece 已注册，但格点、typed layout、GT 石材形态和房间几何未完成；钥匙 `IL.KEYS`、跨模组图书室、跨维传送门与 MTE 房间内容仍 blocked。不得宣称 `runtime_ready` / `player_complete` |
| GT 行星岩（已签发 prep） | `moon.rocks` / `mars.rocks` / `planet.rocks` **3** | 共用 32757 放置器。计划 [GT 行星岩详细计划](../history/card-plans/prep/GT行星岩详细计划.md)。禁止写进现行主世界 catalog，不得别名 `surface_rock_scatter`。落地依赖额外维度 |
| GT Center（已签发 prep） | `center.biomes` / `streets` / `nexus` / `beacon` / `testing` **5** | dump `enabled: false` 不是 skip。计划 [GT Center 详细计划](../history/card-plans/prep/GT中枢详细计划.md)。五条共用 `GENERATE_*`。测试厅不是生存内容。默认保持关闭 |
| Sensors | 21 / 21 | 已关 `runtime_ready`。`compact_sensor_*` 仍是 `IL.SENSORS` **零件物品**，不是这 21 个 Sensor MTE |
| Crops 世界生成（已关 `runtime_ready`） | `plant.glowtus` / `plant.bush` **2** | 已关 `runtime_ready`，不是 `player_complete`。计划 [GT 作物世界生成详细计划](../history/card-plans/closed/GT作物世界生成详细计划.md)。16 色 glowtus 不折 `lilypad_glowtus/white_glowtus`；灌木默认 `sweet_berries`，不掉落 string。不要带 squeezer dump 5322，无榨汁主机 |

### 路线结论

1. 树与作物已关 `runtime_ready`，不宣称 `player_complete`。PUV / OMEGA 已作为
   CC 扩展挂在树上（`frozen`+`paused`），不要再当下一张 unique-active，
   也不要宣称原版高压线完成。
2. catalog **1,817** 个 MTE 身份仍是 R0 分母快照：1,501 `identity_only`、
   283 `realized_natively`、33 流体附件标签。家族 runtime / 折回 child
   **已经关** `runtime_ready`。12 张 prep + 索引还是历史合同，不是
   「仍不是 runtime」。剩余是 dummy 余量、未开门规格，以及 in-place 获得格
   里仍缺的形态 / OD / plank（clustered blocker 已关）。
3. 纳米加工机获得格（激光气体 + 蓝宝石处理器）和连接件 61 个未映射
   Loader id 才是还可能排期的高压/连接件活。禁止 `programmed_circuit` 顶格。
4. Sensors 已关 `runtime_ready`，不宣称 `player_complete`。行星岩 / Center
   仍是 prep，排队不占落地锁。
5. 同一时刻仍只允许一条 unique-active。印刷机继续留 prep，直到染料流体
   身份存在。地牢仍是 prep / `frozen`：结构载体在仓库里，GT 石材、房间
   几何与 `IL.KEYS` 仍 blocked。加工机身份折回 **不是** 地牢完成。
   行星岩还要额外维度 runtime。Center 保持后置。

---

## 1. 当前已冻结但仍有缺口

这些条目已经冻结分母，但仍有明确的 runtime、内容或玩家路径缺口。实现要另开
runtime / 内容卡，不把关闭冻结卡当成实现证明。

PUV / OMEGA 六张是例外：`src/main` **已经有**主机，capability 故意停在
`frozen`+`paused`（CC 扩展，不是原版 GT6 高压线）。它们不属于第 2 节
「尚未签发」。不要 unique-active 它们，也不要宣称 `player_complete`。

| 人类名 | slug / `--check` | 冻了什么 | 判定 | 可行性文件 |
| --- | --- | --- | --- | --- |
| Panels | `portfolio/exclusion-reclaim-r0` | 6 sites / 348 expanded | `requires_new_runtime` | [`feasibility.json`](../../tools/waves/portfolio/exclusion-reclaim-r0/feasibility.json) |
| Sensors | `content/sensors` | 21 / 21 | 已关 `runtime_ready`；ComputerCraft 外设仍 blocked | [Sensors详细计划](../history/card-plans/closed/Sensors详细计划.md) |
| Portals | 同上 | 19 / 19 | `requires_new_runtime` | 同上 |
| 非矿树 | `portfolio/non-ore-worldgen-r0` | `WorldgenTree*` 9 | 已关 `runtime_ready` | [`feasibility.json`](../../tools/waves/portfolio/non-ore-worldgen-r0/feasibility.json)；已关 [GT 树](../history/card-plans/closed/GT树详细计划.md) |
| 非矿地牢 | 同上 | `WorldgenDungeonGT` 1 | prep / `frozen` | 同上；prep [GT 地牢](../history/card-plans/prep/GT地牢详细计划.md)，房间内容与钥匙/传送门保持 blocked |
| 非矿行星岩 | 同上 | moon / mars / planet rocks 3 | `requires_new_runtime` | 同上；prep [GT 行星岩](../history/card-plans/prep/GT行星岩详细计划.md) |
| 非矿 Center | 同上 | biomes / streets / nexus / beacon / testing 5 | `requires_new_runtime` | 同上；prep [GT Center](../history/card-plans/prep/GT中枢详细计划.md) |
| Vanilla loader | `portfolio/vanilla-replace-r0` | `Loader_Recipes_Vanilla` | `requires_new_runtime`；纸的 MVP 已不再是缺口，熔炉 / 骨头 / Vanilla.java 后半仍 `frozen` | [`feasibility.json`](../../tools/waves/portfolio/vanilla-replace-r0/feasibility.json)；[`vanilla_replace_lock.json`](../../tools/waves/content/vanilla-replace-mvp/vanilla_replace_lock.json) |
| Replace / ASM | 同上 | `Loader_Recipes_Replace` + `Replacements` | `requires_new_runtime`；本卡未实现扫描器 / ASM，余量仍 `frozen`。**不是** `player_complete` | 同上 |
| Crops | `worldgen/gt-crops` | `plant.glowtus` / `plant.bush` **2**；dump `gt.recipe.squeezer` 5322 仍是规模上下文、非本卡分母 | 已关 `runtime_ready`；榨汁/Food/Bees 仍 blocked | [GT 作物世界生成详细计划](../history/card-plans/closed/GT作物世界生成详细计划.md)；[`feasibility.json`](../../tools/waves/portfolio/crops-food-bees-r0/feasibility.json) |
| Food | 同上 | dump juicer 96 + fermenter 6435（规模，非 census） | `requires_new_runtime` | 同上 |
| Bees | 同上 | `WorldgenHives` 10 + bumblequeen 80 + bumblelyzer 1440（规模，非 census） | `requires_new_runtime` | 同上 |
| 流体附件（catalog 33） | `content/gt6-mte-fluid-attachments-runtime` | catalog 浇铸口 / 流体龙头 / 喷嘴 / 帽喷嘴 / 流体漏斗 的 in-place runtime | 已关 `runtime_ready`，不是 `player_complete`。R0 `attachment_candidate` 标签未改。source-exact 获得格已落地；缺 `round` / curved / small_casing 仍 blocked | [GT6流体附件runtime详细计划](../history/card-plans/closed/GT6流体附件runtime详细计划.md) |
| 大型热交换器 17197 | `energy/large-heat-exchanger` | 3×3×2 HEX，blocker `energy/large-heat-exchanger-17197` 已 `resolved` | `frozen`+`paused`。代码在仓库。不是 `player_complete` | [PUV2+ / OMEGA 科技线](../history/card-plans/prep/PUV2OMEGA科技线详细计划.md) |
| 蒸汽涡轮 | `energy/steam-turbine` | 15 单机 + 4 大型，STEAM→RU | `frozen`+`paused`。blocker 已 `resolved`。不是 HEX / 冷却器 | 同上 |
| 聚变 / 量子 | `energy/fusion-quantum`、`energy/quantum-massfab` | 18 源行 + CC 中性物质扩展 | `frozen`+`paused`。`energy/reactor-fusion` 已 `resolved`。`FUELS_PLASMA` 保持空 | 同上 |
| 高压零件 / 矩阵 | `content/puv-omega-parts`、`machines/puv-omega-matrix` | 紧凑零件 0–14、`circuit_quantum`、长距变压器 | `frozen`+`paused`。`material-form/luv-puv1-parts` 已 `resolved`。盖板仍十档 ULV–PUV1 | 同上 |

`GENERIC_RECIPE_IMPORT_READY` 只表示已有 host 时可以 `import-source`，不创建新的
RecipeMap，也不实现封面网或世界内容。Prep 与已完成 capability 的状态只看
[project-status.md](project-status.md)；本页只保留仍有缺口的部分。

`fuels_fluidbed` 目前只发 6 条可加载配方，另外 49 条仍在
[`tools/blocked_recipe_ledger.json`](../../tools/blocked_recipe_ledger.json) 中保持
`explicitly_blocked`：缺输入映射、缺形态 / 输出或需要 outputless fuel 模型。

印刷机 prep 当前保留为明确 blocked：`gt.recipe.printer` 的 22 行全部依赖
未映射的 `fluid dye.chemical.*`，`tools/machine_fluid_mapping.json` 将其分类为
`out_of_scope_g10_dyeing`，因此 selected family 为 0、production lock 为空。
五档主机获得格已由技术中间件基础卡按真实 `IL.CONVEYERS` 打成 `source_exact`；
GT6 里传送带零件就是盖板 `compact_electric_conveyor_*`，不再另开 `_module_` 身份。
在真实染料流体身份进入后续内容卡前，不生成空 live family，也不使用
其它流体替代。见第 0.1 节。

打磨机、盖板余量、Sensors 与熔炉已关闭 `runtime_ready`（打磨机曾是 MTE
加工机身份卡的第一张 runtime child；后续家族 child 见第 0.1 节）。
Grindstone `32703` 不在打磨机卡。关闭目标不是 `player_complete`。
计划见 [打磨机详细计划](../history/card-plans/closed/打磨机详细计划.md)
与 [工具头前缀与打磨机余量回收详细计划](../history/card-plans/closed/工具头前缀与打磨机余量回收详细计划.md)。
现行 unique-active 见 [project-status.md](project-status.md)。

剩余的 `controller_*`、`detector_*`、redstone 盖板已由 `logistics/cover-remainder`
关闭为 `runtime_ready`；绝缘红石线 27006/27056/27506 也已关
`runtime_ready`。仍 blocked 的是 torch/repeater 宿主。建筑方块 identity /
behavior 仍没有 owner，不能从 `cc_mechanism = none` 直接推导工作量。盖板分母与合同见
[盖板余量详细计划](../history/card-plans/closed/盖板余量详细计划.md)。

---

## 2. 尚未冻结的领域

这些领域已经被对照图点名，但还没有可直接执行的实现分母；它们不是自动下一张卡。
蒸汽涡轮、大型热交换器 17197、聚变 / 量子物质 **已经**冻结并落地，见第 1 节，
不要再写进本表。

| 项 | 在哪 | 现状 |
| --- | --- | --- |
| 物流余量 | `redstone_torch` / `redstone_repeater` 绝缘红石线宿主 | 已关 `runtime_ready` `logistics/cover-remainder`；裸红石线 MTE 已关 `runtime_ready`。绝缘 27006/27056/27506 已关 `runtime_ready` `content/gt6-insulated-redstone-runtime`。torch/repeater 宿主仍 blocked。见[盖板余量详细计划](../history/card-plans/closed/盖板余量详细计划.md) |
| 配方引擎 | `ShapedCatalystRecipe` 占用包围盒 vs 玩家 2×2 | 1.21 `CraftingInput.of` 去空边后两列网格仍能 `matches()`；`canCraftInDimensions` 已要求 3×3。不是下一张卡。 |
| 计数上限与 kind envelope | capability map 的 report-only / count-ceiling 行 | 新 RecipeMap 前要先明确处理方式 |
| 冷却器 | `energy/cooler` | 仍缺独立 runtime。排期 [blocked.md](blocked.md) C 桶。热交换器第一切片 ≠ 冷却器；蒸汽涡轮已落地也 ≠ 冷却器 |
| 建筑方块 identity / behavior | identity、hardness、multiblock parts、decorative behavior | 仍没有当前 owner |

---

## 3. 仍未认领的 family

权威：[`leftover_later.json`](../../tools/waves/portfolio/source-capability-map-r0/leftover_later.json)。
这里只列仍需要内容卡的 family；已由现有卡消化的历史桶不再列为当前缺口。

| 桶 | 条数 | 读法 |
| --- | --- | --- |
| `later:assembler_combinatorial` + `later:electrolyzer_combinatorial` | 4 | 导入器能力已有，`content_owner = post_generator/combinatorial-family-intake`，`started = false`。要进游戏仍要内容卡 + production lock |

---

## 4. 余量数字不是待办清单

不要把这些整数抄成「还要做 N 个」：

| 数字 | 来源 | 为什么不是 todo |
| --- | --- | --- |
| T35 切走五类后 634 sites / 1,230 expanded | `t13c-exclusion-reclaim-r0` `remainder_after_slice` | 排除表余量，混着 1.x 已经做过的 Storage / 漏斗等。要当待办必须先对照现行机器树，不能整表开工 |
| `other_features.json` 190；非矿切 18 后余 172；再切 hive+plant 12 后余 160 | 两张 worldgen/crops R0 | 余量含 `WorldgenStone` 90、`WorldgenOresVanilla` 24、流体泉 16、`WorldgenRocks` 4 等。矿脉 / 油井 / 地表石子已有 T20/T33 子集。不能当 160 个新 feature |
| dump recipeCount 合计 13,373（作物卡） | squeezer+juicer+fermenter+bumble* | 规模上下文。`owns_families` 仍为 0。`gt.recipe.plantalyzer` recipeCount=0 不是分母 |
| capability map 37 行 `none` | inventory | 混合了已交付、已冻结和未认领项，不等于 37 个新机制 |

**仓储桶前缀单位换算仍缺**（不是 unique-active；T44 catalog 身份登记不等于这条完成）。
GT6 `MultiTileEntityMassStorage` 用 `mPartialUnits` / `getUnitAmount` 把同材料不同前缀并进桶里已存的那一种；CC `MassStorageHandler` 只收 `isSameItemSameComponents`。
T30 `steel_dust_funnel` 只做 dust / small_dust / tiny_dust 的 1/4/9，计划写明不做物品桶、不加 `blockDust`/`dustDiv72`。漏斗不能顶桶上的换算。另开内容卡，不要把 `storage/lock` 或漏斗当已完成。

| 族 | GT6 | CC 桶 |
| --- | --- | --- |
| 粉 | `TD.Prefix.DUST_BASED`：dust / small / tiny / div72 / blockDust | 拒收不同物品 |
| 锭 | `INGOT_BASED`：ingot / nugget / chunkGt / billet / blockIngot | 同上 |
| 线 | `WIRE_BASED`：wireGt01–16 | 同上 |
| 宝石 / 板 / 板宝石 / 碎矿 / 原矿 | gem↔blockGem、plate↔blockPlate、plateGem、crushed↔tiny、oreRaw↔blockRaw | 同上 |

料斗螺丝刀与粉尘漏斗已有独立处理；它们不替代仓储桶的前缀单位换算。

---

## 5. 工作台与手持工具余量

对照 `gt6_code/gregtech6/.../Loader_Tools.java` 与
`gregtech/items/tools/**`。不是 unique active，也不是 37 行 `none` 里的一张卡。
点击契约已抽成 `api.tool.ToolAction` / `ToolInteractable` / `ToolClick`；
后面补行为走这套，不要再 `instanceof MaterialXxxItem`。
创造栏 TOOLS 仍只放 assembler harvest 一次性，新 kind 不进创造栏是有意的。

本节只列仍缺 GT6 世界行为、配方或动作协议的工具；已经有生存路径的工具不再逐一
复述。工具行为补实现时仍以本地 GT6 源为准。

### 5.1 物品在，GT6 世界行为缺

点击 / 交互：

| 工具 | GT6 来源 | 现状 | 缺什么 |
| --- | --- | --- | --- |
| 建筑杖 | `Behavior_Builderwand` | `MaterialWorkshopToolItem` | 按品质在点击面铺同种方块 |
| 钳子 | `TOOL_pincers`；仓储 `onToolClick2` | `PINCERS` 已登记，仓储桶未回答 | 仓储部分抽出；GT6 还打模具/漏斗/堆芯/书架/龙蛋 |
| 口袋多功能 | `Behavior_Switch_Metadata` | 单一 workshop 物品 | 在刀/锯/锉/螺丝/剪线钳/剪刀/凿之间切换并继承对应动作 |
| 手钻 | `Behavior_Tool(TOOL_drill)` | workshop 物品 | 不是挖掘工具；回答 `TOOL_drill` 点击 |
| 万能锹 | `GT_Tool_UniversalSpade` | 铲 `useOn` + 撬棍 | 工作台配方（见 5.2）；还挖斧/锯类方块、铺路/水田/火把、堵漏 |
| 刀 | `TOOL_knife` | 无世界 `useOn` | 方块/实体上的刀动作 |
| 凿 | `Behavior_Tool(TOOL_chisel)` | 只当催化剂 | 回答 `TOOL_chisel` 点击 |
| 软锤 × 仓储 | `TOOL_softhammer` 倒出槽位 | 只转灯/铁轨/朝向 | 点仓储桶把内容弹出 |
| `ToolCompat` 原版表 | `gregapi/block/ToolCompat.java` | 只有灯、动力铁轨、铁轨撬棍、capability 皮搋子 | 熔炉/箱子/发射器/活塞等扳手旋转未搬 |

挖掘 / 掉落：

| 工具 | GT6 来源 | 现状 | 缺什么 |
| --- | --- | --- | --- |
| 建筑镐 | `GT_Tool_PickaxeConstruction` | 原版镐挖掘 | 非矿 ×2 速、矿石 /4、末影箱精准 |
| 斧 / 双斧 | `GT_Tool_Axe` 非潜行伐整棵 | 原版斧速 + 剥皮 | 整树砍倒（与 Trees 卡独立，原版原木也缺） |
| 硬锤 | `GT_Tool_HardHammer.convertBlockDrops` | 只当催化剂 | 挖方块走 Hammer 配方，矿变粉碎 |
| 棒 | `GT_Tool_Club.convertBlockDrops` | 无挖掘 | 石头/砖/下界岩等砸成 `rockGt` |
| 锯 | `GT_Tool_Saw` | 只当催化剂 | 伐木/叶/冰、放置树苗/工作台 |
| 锉 | `GT_Tool_File` | 只当催化剂 | 铁栅栏等更快 |
| 撬棍挖 | `GT_Tool_Crowbar.isMinableBlock` | 只有右键盖板/桶 | 其它工具挖不了的方块兜底收获 |
| 扳手拆机 | `GT_Tool_Wrench.isMinableBlock` | 只有右键连管 | 左键拆机器/活塞/漏斗/发射器一类 |
| 剪线钳挖 | `GT_Tool_WireCutter` | 只有右键连线 | 电缆/导线更快收获 |
| 修枝剪 | `GT_Tool_BranchCutter` grafter | 快砍叶 + 斧剥皮 | 剪叶出树苗（树卡未开时仍对照 GT6） |

### 5.2 配方 blocked 或未发（缺形态，禁止 stand-in）

| 工具 | 阻塞 | 读法 |
| --- | --- | --- |
| 宝石镐工作台 | `assemblies()` 无 `gem_pick`；无 `tool_head_pickaxe_gem` 头配方（GT6 用 `gemFlawed`） | 物品和近似精准在；生存合成没有 |
| 万能锹工作台 | GT6 `aUseNormalHandle=F`，头来自机器，成品格 `AT`/`Sd` | 物品可创造拿到；工作台不发 |
| 放大镜 | `AdvancedCraftingTool(MAGNIFYING_GLASS, lens)` | 物品未注册；`lens` 形态未当工具材料发 |
| 手枪 / 卡宾 / 步枪 | 格要 `plateCurved` + 燧石；还要弹药/弹道 | 整族跳过 |
| 电动工具 | LV–HV 电钻/链锯/扳手/螺丝/电锯/修剪机/搅拌器/镐钻、HV 镐 | 要电机、电池盒、弯板；整族跳过 |
| 非电链锯 / 采矿钻头成品 | `toolHeadChainsaw` / `toolHeadDrill` + 钢环/弯板 | 未注册对应手持物品 |

缺零件就保持 `blocked`，不要用电路、木棍以外的手柄或别的材料顶。

### 5.3 协议还没接到的动作

`ProvidedToolActions` 目前只有扳手/活扳手/剪线钳/螺丝/撬棍/皮搋子/软锤/钳子/万能锹→撬棍。
还没有 `KNIFE`、`CHISEL`、`DRILL`、`MAGNIFYING_GLASS`、`BUILDER_WAND`。
钳子已有枚举，仓储桶 `useTool` 只处理撬棍。

补的时候对照 `gt6_code/gregtech6` 对账，`gregtech6_w` 只当 NeoForge 译稿。

---

## 6. 本页怎么更新

- 本页是人读权威。capability / catalog 变了，当天改本页。不要让状态页 Prep
  列表或 sealed R0 标签把读者带回过期结论。
- 分类总览（第 0.1 节）过期时按 readiness / overflow / capability JSON、
  [blocked.md](blocked.md) 以及
  `tools/waves/portfolio/mte-identity-disposition-r0/` 的 ledger / family_map
  改总览。不要改 sealed R0 正文去「对齐观感」。1,817 必须作为独立分类出现，
  不得只写在流体附件一行。R0 个数变绿靠 runtime child 与 blocker 说明，
  不靠改 disposition 标签。
- 12 张 MTE prep 计划是历史冻结合同；家族已经有 runtime child 之后，本页
  写「child 已关、prep 文件仍在」，不要把 prep 列表抄成待办，也不要回改
  prep 正文假装它交付了 runtime。
- `frozen`+`paused` 且代码已进 `src/main` 的卡（PUV 六张）写在第 1 节，
  不要写回第 2 节「尚未签发」。
- 新关一张冻结 R0：把类别从第 2 节搬到第 1 节，链到新的 `feasibility.json`。关闭说明第一句必须写清「游戏里仍然没有 X」，不得只写 `_READY`。
- 真做进游戏并关 `runtime_ready` / `player_complete`：从第 1 节删掉或改成
  「已由 `<slug>` 实现」。`frozen`+`paused` 关卡（PUV 六张）留在第 1 节。
  同时改第 0.1 节对应行。
- 正式不要：在 realization 卡写 `out_of_scope`，本页改成不要，不要假装 R0 没点过名。
- 不要为了「好看」重写 sealed `growth_order.json` 或已关 R0 artifact。
- 不要再把冻结卡当成默认下一张工作。默认下一张若用户要的是进游戏的东西，开 runtime / 内容卡，或先问清楚，不要再盖一张 READY 冻结收据。
- 本页不维护已关闭计划的明细；关闭状态、`player_complete` 和 active/prep 只看
  [project-status.md](project-status.md)，实现细节回看对应 `card-plans/closed/`。
- `known-issues.md` 只记录 unload / reload / load 等关卡证明债；不要从 `*_READY`
  反推已经完成真卸 chunk。
