# 测试制作规范

> 适用范围：`src/test/java/**` 下的 JUnit 与 GameTest，以及卡计划里的测试条目。
> 落地卡：`portfolio/test-authoring-workflow`
> （[详细计划](../history/card-plans/prep/测试制作流程详细计划.md)）。
> 第 1–5 节现在就适用，靠作者自觉和评审；标「待落地」的帮手、检查器和
> 领域网格由落地卡交付。

## 0. 为什么要有这份规范

2026-09-26 盘点：

- GameTest 共 1030 个，默认网格 `cruciblecraft_default_grid` 只覆盖 17 个文件里的 244 个。
  其余 786 个分散在 133 个文件的单卡命名空间里，没有任何门在跑。
- GameTest 里有 222 处写死的三位数以上期望值；JUnit 里有 211 处
  `assertEquals(N, xxx.size())` 形式的投影计数。
- GameTest 里有 196 处裸 `orElseThrow()`、31 处直接强转 `helper.getBlockEntity`。
  未捕获异常会让测试服务器 tick 崩溃，后面所有测试都拿不到结果。
- 默认网格恢复时，约六成失败是测试本身写错或过期：夹具用了 GT6 不存在的零件、
  期望数字照当天加载抄、机器朝向和供能面写错。其余才是真实运行时 bug。

测试和实现通常是同一次改动里写的，测的往往是作者自己的假设。

## 1. 先选层级

| 层 | 工具 | 放什么 |
|---|---|---|
| 逻辑与数据 | JUnit | 执行时长、电缆损耗、侧面 IO 表、JSON/目录合同、配方来源追踪 |
| 世界行为 | GameTest | 放方块、接管道、供能、tick 后看产物 |
| 规模与性能 | 指标 + 预算测试 | 配方总数、eager/lazy、重载耗时 |
| 玩家体验 | 人跑 `runClient` | 试玩签收，见[能力交付流程](capability-delivery-workflow.md) |

默认先写 JUnit。只有必须真实世界才写 GameTest。
不要在 GameTest 里遍历几十万条配方做数据检查，放到 JUnit 或数据检查器。

## 2. 夹具只用已注册的 GT6 物件

- 材料、形态、管道、机器必须是 GT6 真的注册了的，并且在 CC 里已经注册。
  写之前查 `gt6_code/gregtech6` 与 `python tools/gt6_resolve.py`。
  反例：纯锡、纯铁流体管。GT6 的流体管名单
  （`Loader_MultiTileEntities.java` 1846 行起）没有 `MT.Sn` / `MT.Fe`；
  锡铁合金是 `MT.TinAlloy`。
- 要“不气密的管子”就用木 / 防腐木流体管（该名单里气密参数是 `F`），
  不要挑一个不存在的材料。
- 取不到就是普通断言失败，不许静默换成别的材料、前缀或原版物品。
- 带材料组件的物品要复制原 `ItemStack`；`new ItemStack(item)` 会丢组件。

## 3. 期望值分三类

1. **GT6 源码事实**：9 块叶片、56 面墙、某机器从底面接电。可以写死，
   断言信息或紧邻注释写明 GT6 文件和行号。
2. **我们自己数据的投影**：配方数、EMI 机器数、工具路线数、目录条数。
   不写死。和运行时目录、生成清单或 builder `--check` 产物比较。
   已有的写死投影计数是验证债 VD-2026-09-003，改到它时顺手转成比较。
3. **性能数字**：单独的预算测试，失败信息打出实际值。
   不许为了变绿抬预算常数；超预算要登记给负责人决定。

改任何写死期望值时，提交说明或 disposition 里写明是哪次数据 / 行为变化导致的。
只改数字不写原因，等于把漂移藏起来。

## 4. GameTest 不许抛未捕获异常

- 方块实体：先 `instanceof` 判断再用，或用 `requireBlockEntity`（待落地）。
  不直接强转 `helper.getBlockEntity(pos)`。
- capability、`Optional`：先 `helper.assertTrue(x != null / isPresent(), ...)`。
  不写裸 `orElseThrow()` / `get()`。
- `helper.getLevel().getCapability(...)` 用 `helper.absolutePos(pos)`；
  `helper.getBlockEntity` 自己会换算。
- 失败信息带实际值和期望值，但总长不超过 900 字符。超长文本在关服时会写进
  讲台书并报错，服务器卡住。
- 遍历大量数据的测试调大 `timeoutTicks`，或者挪去 JUnit。

## 5. 机器摆放按 GT6 侧面来

- 供能面、流体 / 物品输入面按 `Gt6SidedIo` 和 `MachineRelativeFace` 算，
  左右是玩家面对正面时的左右。
- 已知坑：电解机、离心机只从底面接电，机器要放在电缆上方、连接朝上；
  蒸馏器流体输入是顶面和左面。
- 待落地：摆放帮手按侧面表自动算朝向，测试不再手写方向。

## 6. 命名空间与运行

现行：

- 默认网格 `cruciblecraft_default_grid` 由 `game-tests` profile 与 `release` 跑。
- 内容卡用 `-PwaveRecipes=<slug>` 的隔离命名空间。
- 排错用 `-PgameTestFilter=方法名片段`，只用于定位，不能代替网格关卡。
- 不认历史 `gametest_receipt.json`。

目标（待落地）：

- 每个 GameTest 只属于少数几个领域网格之一，名单在一个 JSON 里。
- 卡进行中可临时用自己的命名空间，关卡时并入领域网格。
- `release` 跑全部网格，发现数下限从源码 `@GameTest` 计数自动算，不写死。
- 日常只跑改动碰到的领域网格。

## 7. 卡里的测试流程

1. **开卡**：计划列出要加的测试，每条写层级、所属网格和 GT6 依据。
2. **先红后绿**：行为测试先照 GT6 源码写，跑一次确认它因缺实现而失败，再写实现。
3. **开发中**：`-PgameTestFilter` 定位。
4. **关卡前**：新鲜跑碰到的网格和默认网格，0 失败、无崩溃、无未执行 required 测试。
5. **失败处理**：先登记是测试过期、运行时 bug 还是拆出，再决定改哪边。
   运行时 bug 的修复要带测试。
6. **release**：跑全部网格。

## 8. 禁止

- 删测试、去掉 `required`、改成跳过、缩小命名空间。
- 用 `--write` 重刷收据或清单来抹掉漂移。
- 用替身零件、创造栏或 GameTest 注入冒充玩家获得。
- 为了变绿抬预算常数、改写死数字而不写原因。
- 在内容卡里顺手搬 GameTest 类或合并命名空间（需落地卡）。
