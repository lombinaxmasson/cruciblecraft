# Jade 玩家表面整顿详细计划

> 计划 slug：`presentation/jade-player-surface`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：Jade 玩家表面整顿
> 性质：把已有 Jade provider 的行去重、去调试、补缺口、补状态判断，并用行级测试
> 锁住玩家实际看到的内容。只动显示层与只读访问器。
> 不占 unique-active。不改机器行为、配方、形态、获得格。不是 `player_complete`。
>
> 行事实源：`gt6_tfru/gregtech6-TFRU` 的 `IWailaTile` 与各 tile 的 `getWailaBody`。
> 原版 `gt6_code/gregtech6` 的机器没有 Waila，不能拿来证明某一行该有。
> 状态/警告的写法参考 `gt6_referencable_port_code/gregtech6-reborn` 的 Jade provider
> 字符串，只借"显示什么"，不抄文本（该 JAR 元数据为 All Rights Reserved）。
> 物品悬停另见 [GT6 物品说明对齐](GT6物品说明对齐详细计划.md)，本卡不写物品悬停。

```text
lane                    = prep
capability_slug         = presentation/jade-player-surface
unique_active_wave      = null
prep_owned_paths        = src/main/java/com/masson/cruciblecraft/compat/jade/**
                          src/test/java/com/masson/cruciblecraft/compat/jade/**
                          tools/waves/prep/jade-player-surface/**
landing_owned_paths     = 只读访问器所在的方块实体：CrucibleBlockEntity、
                          EnergyBatBoxBlockEntity、MteInPlaceBlockEntity、
                          储罐 / 大型坩埚 / 太阳能 / 魔法场吸收器 / 传感器 /
                          长距离变压器的方块实体；
                          ModLanguageProvider 与生成的 en_us.json / zh_cn.json；
                          docs/current/gt6-tfru-waila-jade-format.md；
                          tools/waves/presentation/gt6-tfru-waila-jade/live_provider_mapping.json
landing_depends_on      = 无。当前 unique-active 为空；落地时仍需占 unique-active
survival_access         = not_applicable
```

签发只合本文件。不创建 `capability.json`，不改 `unique_active_wave`，不开实施分支。

## 0. 开场判断

账本说绿，玩家看到的是乱的。`live_provider_mapping.json` 里 24 个 provider 有
15 个 `migrated`，坩埚也是 `migrated`。`migrated` 只证明 TFRU 那几行加上了，
没有证明旧行删掉了，也没有任何测试看最终行表。现有 Jade 测试只有
`JadeObservationTest`，测的是 server data 序列化。

### 0.1 重复与调试行

坩埚：TFRU `MultiTileEntitySmeltery.getWailaBody` 是三行：内容物、温度当前/上限、重量。
[CrucibleJadePlugin.java](../../../../src/main/java/com/masson/cruciblecraft/compat/jade/CrucibleJadePlugin.java)
的坩埚 provider 大约十一行：

- 温度两次：TFRU 温度行，加 `jade.cruciblecraft.temperature_k`（`%.2f` K）；
- 内容物两次：每种金属一条 `source.contents`，再一条 `material_amount`；
- 熔毁点、缓冲热、填充率各一行，另有 `render_state`、`cache_slot` 两个内部字段；
- 重量行调用 `SourceWailaRows.weight(tooltip, SourceWailaRows.unavailable())`，
  永远显示"不可用"。

加工机：每个非空罐打印三次（server data 罐行、`processing_tank`、TFRU 罐行）；
空罐仍输出 `0/容量 mB 不可用`，而 TFRU `addTankDesc` 遇空罐直接跳过。
加工机 provider 没有 `tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE)`，
Jade 自带的流体条大概率又显示一遍，A 阶段实测确认。

根因之一是账本策略 `"missing_runtime_fields": "unavailable"`：缺字段就显示
"不可用"。TFRU 的做法是缺就不显示。

### 0.2 覆盖缺口

TFRU 有 Waila 的 tile 类共 25 个。下列在 CC 已 live，但没有任何 Jade provider：

| TFRU tile | CC 方块 | 现成只读数据 |
| --- | --- | --- |
| `TileEntityBase10EnergyBatBox` | `EnergyBatBoxBlock` | `stored` / `capacity` / `outputSize` / `items` 已有 |
| `MultiTileEntitySolarPanelElectric` | `SolarPanelBlock` | 待查方块实体 |
| `MultiTileEntityMagicFieldAbsorber` | `MagicFieldAbsorberBlock` | `offer` / `outputSize` 已有 |
| `MultiTileEntityTank` | `TankBlock` | 待查方块实体 |
| `MultiTileEntityCrucible`（大型） | `LargeCrucibleBlock` | 待查方块实体 |
| `MultiTileEntityMultiBlockPart` | 多方块端口 | `MultiblockPortBlockEntity` 已有罐、能量、运行状态、`controllerPosition` |
| `MultiTileEntitySensorTE` | `SensorBlock` | 待查方块实体 |
| `MultiTileEntityLongDistanceTransformer` | `LongDistanceTransformerBlock` | `profile` 已有 |

`TileEntityBase08Barrel`、`MultiTileEntityAutoToolIgniter`、`MultiTileEntityMotorLiquid`
的 CC 对应物在 A 阶段确认；没有 live 对应物就不写。

另外，`MteInPlaceBlock` 上只挂了坩埚、砧、陶模、大锅炉四个 provider。in-place 的
能源与传动类（LuV/ZPM 电池箱、蒸汽涡轮、轴、齿轮箱、旋转引擎）没有任何 Jade 行。

### 0.3 只有数字，没有判断

我们的行几乎都是原始值，例如 `Power: x/t, Progress: a/b (status)`。reborn 在同类
方块上多给了玩家能直接行动的判断：坩埚"接近熔毁"、锅炉"过压 / 过热 / 干烧"、
锅炉结垢后的效率、引擎"停机（超容）/ 关闭 / 预热 % / 运行 %"、基础机器按输入/输出
标注罐并给百分比。TFRU 没有这些行，所以它们不能记成 GT6 迁移结果，只能作为
CC 派生行（见 1.4）。

中英文语言键目前 105/105 齐全，不是问题。

## 1. 做

### 1.1 A：行表盘点（prep）

把每个 provider 的行组装抽成纯函数：输入 observation，输出有序行列表
（翻译键 + 参数）。provider 只负责取数和调用它。这样行表可以在 JUnit 里直接断言，
不依赖 Jade 客户端。

对现有 24 个 provider、0.2 的 8 个缺口、in-place 能源/传动类，逐个写入
`tools/waves/prep/jade-player-surface/row_matrix.json`：

```text
provider / live identity
current_rows        当前实际输出（由行函数产出，不手抄）
target_rows         目标行，每行标 source = tfru | cc_derived | cc_existing
dropped_rows        删除或下放到详情的行及原因
accessor            ready | needs_accessor | blocked
```

同时在 `runClient` 里对坩埚和一台加工机截图（放 `tools/scratch/jade-player-surface/`），
确认 Jade 自带流体条是否重复。

### 1.2 B：去重与去调试

规则写进 [gt6-tfru-waila-jade-format.md](../../../current/gt6-tfru-waila-jade-format.md)：

1. 同一事实在一个 tooltip 里只出现一次。有 TFRU 行就用 TFRU 行，删掉旧 CC 行。
2. 缺运行时数据的行不显示。账本策略从 `"unavailable"` 改为 `"omit"`；
   坩埚重量行在有可信重量前直接删除。
3. 温度按 TFRU 显示整数 K。百分比取整。
4. 次要行（缓冲热、填充率、罐容量明细）放进 Jade 详情，即 `accessor.showDetails()`。
5. `render_state`、`cache_slot` 这类内部字段挂到插件配置开关后面，默认关。
6. 自己输出罐行的 provider 必须移除 `JadeIds.UNIVERSAL_FLUID_STORAGE`。

先做坩埚和加工机，再按同一规则过完其余 22 个。

### 1.3 C：补缺 provider

按 0.2 的表补 8 个，再补 in-place 的电池箱、蒸汽涡轮、轴、齿轮箱、旋转引擎。

- 能复用 TFRU 标准行（状态、能量输入输出范围、推荐值、罐、能量缓冲）的直接复用
  `SourceWailaRows`，不另写一套格式。
- 多方块端口转发控制器的数据，不复制一份自己的机器状态（格式文档第 4 条）。
- `accessor = needs_accessor` 的，只加只读 getter，不改 tick 和存档字段。
- `accessor = blocked` 的留在矩阵里，本卡不写假行。

### 1.4 D：CC 派生状态与警告行

新增 `cc_derived` 行类型：只由当前运行时值计算，不声称来自 GT6。账本新增
`"cc_derived_rows": true` 并要求每条注明计算来源。首批：

- 坩埚：温度到熔毁点 90% 以上时显示红色"接近熔毁"；
- 锅炉：无水且有热、蒸汽满（已有 `no_water` / `steam_full` 状态，改成警告行）；
- 引擎与转换器：把现有 status 字段翻成一行人话状态；
- 加工机：`pausedReason` 非空时单独一行显示原因，不再塞在进度括号里。

阈值只用已有字段；没有字段的警告不做。

### 1.5 E：测试与文档（落地）

- 每个 provider 的行函数一个 JUnit：断言有序翻译键列表、无重复键、无 `unavailable`
  行、详情行只在 `showDetails` 时出现。
- 语言键：新增键中英齐全；删掉的旧键同步从 `ModLanguageProvider` 去掉，不留孤键。
- 更新格式文档与 `live_provider_mapping.json`：`migrated` 之外加 `row_contract` 字段，
  指向本卡的行表。

### 1.6 F：人工实测

落地后在 `runClient` 里逐个 family 看一遍，截图放到
`tools/scratch/jade-player-surface/`（已被 `.gitignore` 忽略，不提交），
由用户确认观感后才关卡。单测过了不等于能关。

## 2. 不做

- 物品悬停（归 `presentation/gt6-item-tooltip`）、EMI、GUI、创造栏。
- 机器行为、tick 逻辑、存档格式、配方、材料形态、获得格。
- 新贴图或渲染器。
- 为补 Jade 行去扩材料 JSON 或能源 `EnergyType`。
- 照抄 reborn 的文本或类结构；它只提供"该显示哪些判断"的参考。
- 为了让矩阵变绿，给 `blocked` 的 family 写静态文字或猜测值。

## 3. 验收

开工前本文件只在 `card-plans/prep/`。`unique_active_wave` 保持 `null`。

落地后：

```text
provider 行表中重复翻译键                      = 0
显示为 unavailable 的行                        = 0
自带罐行且未移除 UNIVERSAL_FLUID_STORAGE 的 provider = 0
0.2 表中 accessor=ready 的方块缺 provider       = 0
cc_derived 行缺计算来源说明                      = 0
新增 / 删除语言键中英不一致                      = 0
```

- 坩埚默认视图不超过 TFRU 的三类信息加一条警告行；详情视图再加缓冲热与填充率。
- 加工机每个非空罐只出现一次，空罐不出现。
- `python tools/build_capability_ledger.py --check` 与
  `python tools/build_project_status.py --check` 绿。
- 用户在 `runClient` 截图上确认。
- 关闭走 `python tools/close_capability.py --capability presentation/jade-player-surface --change-class major`。
  Jade 是玩家看的界面，不用 `minor`。本卡未关。

## 4. 阻塞与撤回

必须 `BLOCKED`：

- 某行只能靠静态文字或猜测值才能显示；
- 补访问器需要改 tick、存档字段或机器行为；
- 需要改别的 unique-active 卡的 owned paths；
- 为通过测试放宽断言或删掉有效的 live provider。

撤回只撤本卡，保留 `row_matrix.json`；截图在 `tools/scratch/`，不随卡提交。关闭时把本文件移到 `card-plans/closed/`，
更新 [history INDEX](../../INDEX.md) 与 [known-issues](../../../current/known-issues.md)；
不自动创建 Git commit。
