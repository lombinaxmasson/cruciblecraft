# CrucibleCraft 4.5 · 体验精修规划

> 性质：**玩家可见体验精修**，不是配方 / 能量 / 多方块通用层  
> 状态：◯ **planning-only，未开卡、未授权开工**（2026-08-15）  
> 来源：T26 试玩观察，不自动升格为 T26 Beta 阻断项  
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`  
> 当前唯一 active T 仍是 `T26`

---

## 0. 这份文件是什么

第四阶段（T20–T26）把工业主链收成可公开试玩的 Beta。试玩里冒出来一批
**不影响架构、但每天都会碰到** 的缺口：扳手手感、剪线钳、创造栏分类、石子、
EMI 分组、工具变体，以及第一次进世界的卡顿。

它们不适合塞进：

- **T26**：T26 是公开 Beta 门禁。这些项目前没有被标成 hard blocker；
  未分类前不得借 4.5 改写 Beta 退出条件。
- **T27 / 第五阶段轨 A–E**：那些是 v1.0 portfolio freeze 与 1.x 内容轨。
  精修不是 ordinary_optional 长尾，也不是新多方块。

因此单独记为 **4.5**：第四阶段主链之后、第五阶段内容轨之前的体验补丁阶段。
正式 T 编号（T26.5 / T27 前插卡 / 并入 T25 类修复卡）开卡时再定；在此之前
禁止生成注册对象、配方或 READY 状态。

**一句话判据（规划口径，不是开工授权）**

> 玩家用手设置线缆/管道、在创造栏和 EMI 里找到同类物品与已有档位/变体、
> 在地表捡到石子、用 cell 拿到已注册流体时，行为与展示对齐格雷 6 的日常手感；
> 第一次进世界的卡顿有日志证据和 disposition，而不是猜配方展开。

### 0.1 明确不是 4.5

- 配方规则语言、能量网络、多方块通用层、Cover 协议；
- 新 RecipeMap、新机器档位、ordinary_optional 长尾；
- 把任一项升格为 v1.0 / Beta 阻断（必须先改产品契约）；
- 在 T26 仍 OPEN 时并行开工。

### 0.2 建议开工顺序

| 顺序 | 项 | 理由 |
|---|---|---|
| 1 | P1 + P2 九宫格与剪线钳 | 同一套连接交互；线缆/管道每天都用 |
| 2 | P3 / P6 / P7 创造栏枚举 | 展示层：线缆分页、工具变体、漏掉的机器档位 |
| 3 | P8 标签命名空间 + P5 EMI 统合 | P8 是 P5 的前置；先把 `c:` 补齐再分组 |
| 4 | P9 创造栏流体/灌装 cell | 化学试玩现在几乎拿不到流体 |
| 5 | P4 石子 | 要加 `rockGt` 前缀 + 地表 feature |
| 6 | P0 首次进世界卡顿 | 配方展开已排除；先取证再改 |

P1 与 P2 必须同一张实现卡，不要先做整面扳手再推翻成九宫格。
P6 与 P7 都是「物品已经注册、创造栏手写漏了」，可同一张展示卡。

---

## P0 · 第一次生成世界卡住几分钟

**玩家观察**：第一次生成世界会卡住几分钟。

**已排除**：配方 epoch 展开不是主因。2026-08-15 试玩日志：

```
Published recipe epoch 1 with 8398 T3 component, 3452 T4 tool, 158 T5 chemical,
220 T7 authored, 257 T8 pipe, and 1288 T10 known-form material-rule recipes;
T14c Extruder 2782 logical = 557 eager + 2225 lazy (cache ceiling 512, 20 authored)
(11851 live T3-map, 18882 logical total, 16657 eager total)
across 32 maps in 3034 ms; indexes 60 ms
(count budgets 10000/4000/13000/200/256/320/1500/21000)
```

展开 3034 ms、索引 60 ms，都在 10 s / 预算内。回头不要再把 P0 当成
`GTRecipeMapLoader.reload` 问题，除非新日志推翻这条。

**仍待取证（按可能性，不是结论）**

1. `GeneratedMaterialPack` 首次（或材料指纹变化后）把整包标签/模型/战利品表写进
   config 缓存；之后命中缓存会快得多。核对是否只卡「这台机器第一次启动」。
2. EMI 在客户端为上万条配方建索引；与服务器 3 s 配方日志不是同一段。
3. 客户端为海量独立方块/物品烘焙模型（矿、缆、管）。
4. 出生点区块：134 条大矿脉特征每区块都会跑，非锚点应早退；不太像分钟级，
   但会叠在上面。
5. 集成端「准备出生区域」墙钟 = 服务端生成 + 客户端资源，不是单条日志。

**4.5 退出**：有一段带时间戳的因果链（哪条线程、哪段工作、是否仅首次），
以及 disposition（修 / 延后 / 接受）。禁止在没有这段证据时改世界生成或配方管线。

---

## P1 · 扳手 / 剪线钳九宫格

**玩家观察**：格雷 6 是点一个面的九宫格，分别设置六个方向的连接。

**现状**：`Gt6StyleConnections.wrench` 只用 `hit.getDirection()`，点哪面开哪面。
放置语义已经是格雷 6 风格（只打开对着的兼容邻居，不自动四通），缺的是设置手感。

**格雷 6 来源**：`gregapi.util.UT.Code.getSideWrenching`  
中心 = 被点的面，四边 = 邻面，四角 = 对面。线缆与管道共用这套命中。

**建议**

- 用 hit UV 复刻九宫格，管道与导体走同一辅助函数。
- **必须做扳手/剪线钳覆盖层**，否则九宫格几乎不可用。
- 不要改自动连接政策。

**耦合**：只动 `Gt6StyleConnections`、方块 `useItemOn`、客户端 overlay。
不碰能量传输与管道拓扑算法本身（开关连接仍走现有 `setConnection`）。

---

## P2 · 剪线钳缺失，电缆应当能设置

**玩家观察**：没有剪线钳；电缆连接应当用它来设置。

**现状**

- 没有 `ToolKind.WIRE_CUTTER`、物品、图样、装配配方。
- 电缆和裸线**可以**改连接，但用的是扳手，而且是整面开关。
- 工具图样目录到扳手为止（`ToolPatternCatalog`）。

**格雷 6 分工**

| 连接体 | `getFacingTool()` |
|---|---|
| 电线 / 电缆 / 光纤 / 红石线 / 物流线 | `TOOL_cutter` |
| 物品管 / 流体管 / 轴 | `TOOL_wrench` |

剪线钳配方与门槛：`Loader_Tools.java` 的 `WIRECUTTER`，
`{"PfP","hPd","STS"}`，`typemin(2)`，排除 BOUNCY / STRETCHY。
材料量 4U。另外可更快收线缆，那是次要手感，可后置。

**建议**

- 补剪线钳全链（kind / 物品 / 图样 / T4 装配规则 / 创造栏 / 着色）。
- 电缆与裸 `wire` 方块改由剪线钳 + 九宫格设置。
- 管道继续用扳手 + 九宫格。
- 工具提示从「Wrench: toggle connection on the clicked face」改成按连接体分工具。

**载荷**：新工具会增加一批 T4 assembler 行（与现有扳手同量级，扳手现 308 条）。
开卡时要投影 eager 行，不得 silently 突破 21,000 eager 门禁。

---

## P3 · 创造栏里「线」和 cable 分开了

**玩家观察**：名叫「线」的线缆和 cable 不在同一页。

**现状（刻意拆分，不是漏放）**

| 页 | 内容 | 规模 |
|---|---|---:|
| 线 `WIRES` | `fine_wire`、`wire`、倍线… | 288 |
| 电缆 `CABLES` | `cable`… **外加全部流体管/物品管** | 400 |

可放置导体跨两页：裸 `wire` 是 `CableBlock`，绝缘 `cable` 也是。
倍线（`double_wire` 等）只是物品；倍缆才是方块。

标签也不对称：`wire.json` 使用 `c:wires`；`cable.json` 未写 `tag_namespace`，
落到默认 `cruciblecraft:cables`。EMI / 标签检索会继续把它们拆开。

**建议**

- 创造栏：可放置的线与缆放一起（合并一页，或缆页里线/缆相邻）。
- 流体管 / 物品管单独成页，不要继续挤在「电缆」里。
- 倍线：留在零件，或与对应倍缆并排。
- 给电缆补 `tag_namespace: "c"`，与线对齐。这会改生成标签路径，要回归
  `MaterialCreativeTabTest` 与生成资源包测试。

---

## P4 · 世界生成没有石子

**玩家观察**：地表石子完全缺失。

**现状**：前缀表 56 项里没有 `rockGt`；世界生成只有大矿脉和地下流体。
`tools/t13_denominators/prefixes.json` 里能看到 `rockGt` / `pebbles` 分母，
从未导入。

**格雷 6**

- 前缀 `OP.rockGt`，条件 `ORES || STONE`。
- 地表小石块：`WorldgenRocks`（常见生物群系）+ `WorldgenStoneLayers` 表面 1/128。
- `RM.pack(rockGt × 4 → cobble / netherrack / end stone)`；石头石子也可 2×2 合成圆石。
- 早期没石镐时，石子是圆石来源。

**建议**

- 增加 `rockGt` 前缀与物品（小方块或物品+薄方块，保真优先小方块）。
- 一条独立地表 placed feature，不要塞进 134 条大矿脉。
- 4 → 圆石（及对应岩石）的打包/合成。
- 创造栏归矿石加工或杂项，不要进锭页。

**注意**：加前缀会碰前缀目录、生成资源包、创造栏计数、可能的 unification。
仍比配方/能量/多方块局部，但比 P3/P6 重。不要顺手做全套岩石层替换。

---

## P5 · EMI 按类 / 标签统合

**玩家观察**：EMI 里相同类/标签应折叠在一起，例如所有锭、粉/小粉/微粉、管道和线缆。

**现状**

- 生成包已经在写形态标签：锭 `c:ingots` / `c:ingots/copper`，粉 `c:dusts`。
- EMI 插件只注册机器配方，没有 grouping、alias、`Comparison`。
- `small_dust` / `tiny_dust` 默认 `cruciblecraft:small_dusts` 等，不在 `c:` 下。
- 电缆标签同样不在 `c:` 下（见 P3）。
- 工具是单物品 + 材料组件，EMI 默认只显示默认铁变体。

**建议（两层，不要糊成「任意锭」）**

1. **物品列表**：按形态标签折叠（锭、粉、缆/管），展开后再按材料。
2. **粉族**：同一材料的粉 / 小粉 / 微粉做 alias，或至少保证 4/9 换算能互相搜到。

禁止把所有铜锭合成一条「任意锭」配方。材料路线必须仍按具体材料检索。

P6 的工具变体也应作为带组件的 `EmiStack` 出现在同一插件里。

---

## P6 · 工具材料变体缺失

**玩家观察**：创造栏 / EMI 里几乎看不到各材料工具。

**现状：运行时有，展示没有。** 工具是单注册项 + `TOOL_MATERIAL`。
创造栏主页写死了几条（镐：铁/钻石/石；扳手：铁）。装配机配方是齐的。

试玩期 gametest 钉死的规模：

| 工具 | 装配配方 | 有资格但无路线 |
|---|---:|---:|
| 镐 | 330 | 208 |
| 铲 | 412 | 126 |
| 斧 | 329 | 209 |
| 锄 | 329 | 209 |
| 剑 | 412 | 126 |
| 锻造锤 | 317 | 80 |
| 锉 | 92 | 56 |
| 凿 | 307 | 2 |
| 锯 | 307 | 2 |
| 螺丝刀 | 309 | 0 |
| 扳手 | 308 | 2 |

无路线缺口是格雷 6 板/杆形态闭合问题（例如橡木有资格但无路线），不是漏做物品。
4.5 默认只展示**有配方的变体**；无路线材料若进创造栏，必须标成不可合成，
不能假装能做。

**建议**：创造栏按 `ToolMaterialRules.isAllowed` ∩ 现有 T4 路线枚举；
EMI 用带组件的 `EmiStack` 列出同一集合。剪线钳（P2）用同一枚举，不要再写死铁一把。

---

## P7 · 创造栏漏了已注册的机器档位

**玩家观察**：和 P6 同类——东西做了，创造栏没放全。

`ModMachineVariants` 和配方里都有钢/钛/殷钢档，EMI 加工机分类也会
`forKind` 挂上对应工作站。主创造栏 `ModCreativeTabs.MAIN` 却是手写清单，
只点了离心机、筛选机、电解机的高档，其余漏光。

**在创造栏里的高档**：钢/钛离心机、钢/钛筛选机、铝/不锈钢电解机。

**已注册、有合成、创造栏没有**（18 项）：

| 机器 | 漏掉的档 |
|---|---|
| 粉碎机 shredder | steel、titanium |
| 车床 lathe | steel、titanium |
| 轧机 rollingmill | steel、titanium |
| 线材轧机 wiremill | steel、titanium |
| 冲压机 press | steel、titanium |
| 熔炉 smelter | invar、titanium |
| 蒸馏器 distillery | invar、titanium |
| 干燥机 drying | invar、titanium |

基础档（无后缀那台）都在。不是「没做钢车床」，是手写 `output.accept` 漏行。

**建议**：主页不要再手抄机器列表，改为枚举已注册 processing / 能源方块；
或至少按 `ModMachineVariants.ALL` 补齐。P6/P7 同一张展示卡即可。

---

## P8 · 形态标签只有少数进了 `c:`

**玩家观察**：P5 想按「所有锭 / 所有粉 / 所有缆」折叠，但标签命名空间不齐。

显式写了 `"tag_namespace": "c"` 的前缀只有 11 个：
`block`、`dust`、`gem`、`ingot`、`nugget`、`ore`、`plate`、`plate_gem`、
`raw_ore`、`rod`、`wire`。

其余默认 `cruciblecraft`，包括 P5 点名的：

- `small_dust` → `cruciblecraft:small_dusts`（不是 `c:small_dusts`）
- `tiny_dust` → `cruciblecraft:tiny_dusts`
- `cable` 及倍缆 → `cruciblecraft:cables`
- 全部流体管 / 物品管 → `cruciblecraft:fluid_pipes` 等
- `crushed_ore`、`foil`、`gear`、`fine_wire`、倍线……

P3 的线/缆分裂，标签层也是同一原因。

**建议**：4.5 里把常用形态补到 `c:`（至少粉族、缆、管、破碎矿），再做 P5。
不要在 EMI 里写死 `cruciblecraft:ingots` 特例。这会改生成资源包路径，
`MaterialCreativeTabTest` 和 pack 测试要跟着回归。

---

## P9 · 创造栏几乎没有流体

**玩家观察**：化学和熔融在创造模式很难拿到。

- 桶只有木馏油和蒸汽。
- 110 种化学流体、约 204 种熔融流体**没有桶**（`ModFluids.registerMaterials`
  不挂 bucket）。
- 创造栏只有空的 `fluid_cell` / `gas_cell`，没有按流体预灌装。

生存路径用机器产流体是对的；创造/EMI 试配方时会觉得「流体没做」。

**建议**：创造栏为每种允许进 cell 的流体放一个灌装 cell（液/气分页或挂在
MISC）。不要给熔融金属人手提桶，除非明确要世界里泼熔融。EMI 流体栈本身
一般能从注册表看到，缺的是物品形态。

---

## 候补（默认不进 4.5）

扫过、和「写死」长得像，但属于缺内容或产品冻结，不要混进展示补丁：

| 项 | 为什么不是 4.5 |
|---|---|
| 硬锤 / 软锤 / 撬棍 / 菜刀 / 活扳手 | 整类工具没做，不是漏枚举。硬锤打矿是早期手感，要单独立项 |
| 砧/坩埚/锻造锤材料白名单只有 3–4 种 | `MachineMaterialRules` 的设备政策，不是创造栏漏列 |
| 可放置导体只有 `wireGt01` + 电缆，没有倍线方块 | T6 `ElectricalConductorCatalog` 域冻结；倍线仍是物品 |
| `PipeCatalog` 里 copper/tin/iron | 启动断言「这两种管都有」，不是只生成这三种管 |
| EMI 砧工作站写死 stone/iron/bronze/steel | 和砧白名单一致，不是漏档 |
| 测试里写死 copper/tin | 夹具，不是运行时限制 |

电缆 tooltip 仍写 `Wrench: toggle connection…`，随 P2 改，不另开项。

---

## 试玩点名、但不属于 4.5 的内容保真

这四条是格雷 6 的**材料档位 / 岩层世界 / 工具种类**，不是创造栏漏列。
T18 / T20 / T4 已经有明确冻结；要做就进 1.x portfolio，不要塞进体验精修。

### A · 燃烧室 / 蒸汽机 / 锅炉罐 / 汽轮机的金属档

格雷 6 用同一套 MTE、不同金属贴图和 NBT（效率、输出、容量）。T18 分母：
**333 条 source variant，selected 6，deferred 20。**

| 格雷 6 kind | source 档数 | CC 现状 |
|---|---:|---|
| 固体燃烧室 `GeneratorMetal` | 26 | 只做青铜 1102（24 HU/t、75%） |
| 液体燃烧室 `GeneratorLiquid` | 22 | **整类 deferred**（和燃油机 RU 竖切分开） |
| 气体燃烧室 `GeneratorGas` | 22 | 只做青铜 1602 |
| 蒸汽锅炉罐 `BoilerTank` | 26 | 只做青铜 1202 |
| 蒸汽机 `EngineSteam` | 28 | 只做青铜 1302（KU，不是汽轮机） |
| 蒸汽汽轮机 `TurbineSteam` | 15 | **整类 deferred**：出 RU，不能和蒸汽机 KU 混 |

贴图按金属着色是档位实现的一部分，不是单独换皮。砖燃烧室 1199、Dense 系列、
Invar/Steel/Ti 锅炉都还在 T18 deferred 账上，replacement condition 写得很清楚。

### B · 岩层（花岗岩、黑花岗岩等）与指示矿

格雷 6 的 `WorldgenStoneLayers` + `BlockStones` 会把原版石头换成黑花岗、大理、
玄武等层，层间接触带出指示矿，地表石子材质跟着底层走。这和 T20 的
**40 条 large / 75 条 small 矿脉**不是同一套几何。

CC 现在：

- 矿宿主只有 `STONE` / `DEEPSLATE`；
- `granite` / `granite_black` / `granite_red` / `basalt` / `marble` 是材料
  JSON（粉、板、杆），**没有世界方块、没有岩层 feature**；
- P4 石子也还没做；即使做了，没有岩层也变不成「黑花岗石子指示矿」。

这是 T20 之后的世界生成保真，体量远大于 4.5。

### C · 镐/斧因岩石而不同

两条叠在一起：

1. **用岩石做的工具**：数据已在。`stone` 耐久 16 / 品质 1 / 速度 2；
   `granite_black` 64 / 3 / 3（品质 3 → 钻石级挖掘）。T4 只有精确
   `material.is("stone")` 走石镐例外；黑花岗若有板+杆会走金属镐配方，
   和格雷 6 用 `rockGt` 做石质工具不一致。
2. **挖不同岩石**：格雷 6 的 `BlockStones.mHarvestLevel` 决定黑花岗更硬，
   破碎配方时间 `16+harvestLevel*16`。CC 世界里还是原版石头硬度。

没有 B 的岩层方块，C 的挖掘差异落不到世界上。工具数值可以在有配方的材料上先
露出来（P6 枚举时黑花岗镐会以钻石级出现），但那不是岩层系统。

### D · 螺丝刀、撬棍等

| 工具 | CC | 世界用途 |
|---|---|---|
| 螺丝刀 | 有物品、309 条装配配方 | **没有**点方块行为；只当合成催化剂 |
| 撬棍 | 无 | 格雷 6 拆 cover、拆轨道类 |
| 硬锤 / 软锤 | 无（锻造锤不是硬锤） | 硬锤打矿出粉碎矿；软锤旋转/开关 |
| 剪线钳 | 无 | 已记 P2 |
| 菜刀 / 活扳手 / 插销等 | 无 | G10 / 1.x |

螺丝刀「做了物品、没做世界逻辑」和 P6 的展示漏列不同：缺的是 cover/机器
交互，要跟 Cover 协议一起做，不能只补创造栏。

---

## 1. 与 T26 / 第五阶段的边界

| 问题 | 4.5 | T26 | T27 / 轨 A–E |
|---|---|---|---|
| 九宫格、剪线钳 | 是 | 仅当被标成 Beta hard blocker | 否 |
| 创造栏 / EMI / 工具 / 机器档 / 流体 cell | 是 | 否（非门禁） | 否 |
| `c:` 标签补齐 | 是（P5 前置） | 否 | 否 |
| 石子 | 是 | 否（T20 矿脉闭包已完成；石子是另一条地表玩法） | 否 |
| 硬锤等整类新工具、岩层、蒸汽金属档、汽轮机 | 否（1.x / T18 deferred） | 否 | 是 |
| 首次卡顿 | 取证在 4.5；若证明阻断安装则再升 T26 disposition | 目前不是 | 否 |
| 配方 / 能量 / 多方块 | 否 | 否 | 是 |

T26 试玩 known-issue 台账可以**引用**本文件的 P0–P9，但 owner 写 4.5，
release disposition 默认 `post_beta_polish`，除非试玩证明某条阻断五节点首玩。

---

## 2. 开卡前还要拍板的三件事

1. **正式编号**：T26 关闭后的插卡，还是 Beta 后单独里程碑。在 T26 OPEN 期间
   不得占用 active T。
2. **P0 取证环境**：同一世界第二次进入是否仍卡；dedicated server vs 集成端；
   是否删除过 `.generated-material-pack`。
3. **P2 载荷**：剪线钳 T4 行数投影是否仍低于 eager 21,000。当前日志 eager
   16,657，余量约 4,343，大概率装得下，但要开卡时重测，不拿本次日志当关门数字。

---

## 3. 代码锚点（规划时的位置，开卡时复核）

| 项 | 主要位置 |
|---|---|
| P0 配方日志 | `GTRecipeMapLoader.reload`；资源包 `GeneratedMaterialPack` |
| P1 / P2 连接 | `Gt6StyleConnections`；`CableBlock` / `AbstractPipeBlock.useItemOn` |
| P2 工具 | `ToolKind`、`ToolPatternCatalog`、`T4ToolRules`、`ModCreativeTabs` |
| P3 创造栏 | `MaterialCreativeTab`；`wire.json` / `cable.json` 的 `tag_namespace` |
| P4 石子 | 前缀目录目前无 `rockGt`；GT6 `OP.rockGt`、`WorldgenRocks` |
| P5 EMI | `CrucibleCraftEmiPlugin`；生成包 `c:ingots` / `c:dusts` |
| P6 工具展示 | `ModCreativeTabs.MAIN` 写死变体；配方在 assembler `t4/assembler/<tool>/` |
| P7 机器档 | `ModCreativeTabs.MAIN` 手写清单 vs `ModMachineVariants.ALL` |
| P8 标签 | `material_prefixes/*.json` 的 `tag_namespace`；默认 `cruciblecraft` |
| P9 流体 | `ModCreativeTabs` 只有空 cell；`ModFluids` 化学/熔融无 bucket |
