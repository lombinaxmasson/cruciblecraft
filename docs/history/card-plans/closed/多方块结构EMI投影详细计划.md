# 多方块结构 EMI 投影详细计划

> 计划 slug：`presentation/multiblock-emi-projection`
> 状态：已关，runtime_ready。本文件位于 `card-plans/closed/`。
> 正式名称：多方块结构 EMI 投影
> 性质：把数据包里的多方块在 EMI 里画成可转的方块模型，换掉现在按 Y 层铺开的物品格。
> 不新增结构、机器、配方、形态。不改形成检查和建筑权杖。
> 不是获得路径。

```text
lane                    = active
capability_slug         = presentation/multiblock-emi-projection
unique_active_wave      = presentation/multiblock-emi-projection
prep_owned_paths        = src/main/java/com/masson/cruciblecraft/compat/emi/multiblock/**
                          src/test/java/com/masson/cruciblecraft/compat/emi/multiblock/**
landing_owned_paths     = CrucibleCraftEmiPlugin.registerMultiblockBlueprints
                          MultiblockEmiRecipe
                          ModLanguageProvider 与生成的 en_us.json / zh_cn.json
landing_depends_on      = 当前没有 workflow=active。晋升时若已有别的 unique-active，等它关闭
survival_access         = not_applicable
```

签发只合本文件，以及由计划列表生成的 [project-status.md](../../../current/project-status.md)。
不创建 `capability.json`，不改 `unique_active_wave`，不开实施分支。

`CrucibleCraftEmiPlugin.java` 也写在
`logistics/logistics-core`、`logistics/generic-network/core`、
`logistics/item-network-core` 的 `owned_paths` 里。落地时 impact graph
选出的受影响 profile 要在同一次验证里 fresh PASS。

## 0. 开场判断

`data/cruciblecraft/multiblock_structures/` 有 23 份结构 JSON，全部走
`MultiblockStructureDefinition`。控制器在本地 `[0, 0, 0]`。JSON 按朝北编写，
东、南、西用已有的 `Offset.rotate`。

EMI 已经在 `CrucibleCraftEmiPlugin.registerMultiblockBlueprints` 里用
`MultiblockStructureCatalog.prepare` 读这些文件，避免客户端注册替换校验器
用的那份快照。`MultiblockEmiRecipe` 给每个 Y 层单独一张配方，把该层 XZ
铺成 18 像素物品格，旁边是按调色板键计数的材料槽。23 份结构因此是 77 张
配方。`tank_5x5x5` 有 125 个元素、5 层，页面跟着网格变大。材料槽能被搜索，
中空、朝向和层与层的叠法在这张格子图上看不出来。

加工行的 `EmiProjection` 继续只做加工配方。结构投影是另一份格子表，放在
`compat/emi/multiblock`。

EMI 1.1.24 的配方 `Widget` 只收到 `mouseClicked`。`RecipeScreen` 的
`mouseDragged` 和 `mouseScrolled` 进 `EmiScreenManager`。旋转、缩放、换层
用 `WidgetHolder.addButton`。拖拽只允许在投影 `render` 里读鼠标按键，并且
按下的起点已经落在投影矩形内。滚轮留给 EMI 自己的滚动。不 mixin EMI。

## 1. 做

一种结构 id 一张配方。分类仍是 `cruciblecraft:multiblock_blueprint`，图标
仍是焦炉方块。配方 id 去掉现在的 `/layer_N`。工作站仍是控制器调色板解析出
的每个方块；控制器是标签时，标签里每个方块都继续 `addWorkstation`。
`hideCraftable` 保持 true。

格子表和绘制控件放在 `compat/emi/multiblock`。单元测试直接断言格子，不建
GL 上下文。`registerMultiblockBlueprints` 改调用这份格子表，是落地那一步。
落地时 `MultiblockEmiRecipe` 改成消费它，按层铺开的物品格去掉，材料槽留下。

### 格子

每个非空气元素一条：本地偏移、调色板键、要画的方块。

- 调色板写了 `block`，就画这个方块，有没有物品形态都画。
- 只有标签时，画该标签里注册名最小、并且已经注册的方块。
- 同一个 `uniform_group` 的格子画同一种方块。组里有明确 `block` 就用它；
  否则用组内各标签交集里注册名最小的方块。交集为空时这些格子不画模型。
- 储罐墙 `tank_wall`、电炉线圈 `oven_coils` 整组一个样子。运行时同组必须
  是同一种方块，投影跟着这条规则。样子用的那一个方块只是示例。

材料槽保持现在的口径：每个非空气调色板键一个 `EmiIngredient`，数量是格子
数；标签展开成有物品形态的成员；没有物品的方块不进槽。控制器槽仍是催化剂。
EMI 搜索靠这些槽。投影格子不是槽。

朝向按钮循环北、东、南、西，画之前走 `Offset.rotate`。默认北，和 JSON、
校验器的北朝向一致。现有控制器不用上、下朝向，投影也不提供这两档。

### 画面

投影占一块固定矩形，结构的本地包围盒缩进这块矩形。页面高度取投影和材料
槽列里较高的一边。

偏航、俯仰、缩放、朝向、当前层记在配方对象上。`addWidgets` 再次调用时保留
这个视角。

按钮：左右转、俯仰两档、放大、缩小、朝向、层。层可以是全部，也可以是单独
一个本地 Y。单独一层时只画该 Y。

绘制用 `BlockRenderDispatcher.renderSingleBlock`。画完调用
`bufferSource().endBatch()`，并把绘制剪进投影矩形。

悬停打到某一格时，提示该格的 `PalettePredicate.description()`。打不中就
不额外提示。材料槽继续用 EMI 自己的提示。

### 样本

- 焦炉：27 个元素里的空气不画；耐火砖按计数进材料槽；控制器在原点；3 个
  Y 只产生 1 张配方。
- 大型电炉：线圈标签整组同一个代表方块。
- 3×3×3 储罐：`tank_wall` 整组同一个代表方块；材料槽仍是墙标签。
- 朝东之后，焦炉里一个非原点格子的坐标等于 `Offset.rotate(EAST)`。
- 现有 23 份 JSON 各 1 张配方。没有控制器的定义沿用现在的跳过和警告。
  这 23 份都有控制器。

## 2. 不做

- 世界里的幽灵方块，以及从 EMI 点下去放置。建筑权杖
  `MultiblockBuilderInteraction` 保持原样。
- `SpecialMultiblockBuilderAdapter` 上的 Java 结构：物质制造机、聚变、
  物流核心、基岩钻、避雷针、范德格雷、大型热交换器、大型动力机、蒸汽涡轮、
  大型燃气轮机。它们没有 `MultiblockStructureDefinition`。本卡不把它们改写
  成 JSON。
- 结构 codec、校验器、形成逻辑、端口聚合、机器方块实体。
- 新结构文件、新机器、新配方、新贴图。分类图标继续用焦炉。
- mixin EMI，或再引入一个多方块预览库。
- 把投影格子再做成 EMI 槽。

## 3. 验收

开工前本文件只在 `card-plans/prep/`。`unique_active_wave` 仍是空。

落地后：

- EMI 蓝图分类里每种结构 id 一张配方。焦炉能看到中空外壳；朝向按钮转到
  东、南、西；层按钮能只留一层。
- 材料槽仍能按方块或标签搜到这张结构。
- `python tools/build_capability_ledger.py --check` 与
  `python tools/build_project_status.py --check` 绿。
- 格子表有 JUnit。画面不靠 GameTest。
- 关闭走
  `python tools/close_capability.py --capability presentation/multiblock-emi-projection --change-class major`。
  这是玩家看的界面。
