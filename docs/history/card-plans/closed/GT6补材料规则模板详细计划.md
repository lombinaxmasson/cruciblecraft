# GT6 补材料规则模板详细计划

> 计划 slug：`recipe/gt6-material-rule-remainder`
> 状态：已关闭。本文件位于 `card-plans/closed/`。
> 正式名称：GT6 补材料规则模板
> 性质：把覆盖页上还差配方、而且图上已经有材料规则的小尾巴，按主机能收的 dump 行发布。
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = unique-active
capability_slug              = recipe/gt6-material-rule-remainder
unique_active_wave           = recipe/gt6-material-rule-remainder
dump_map                     = extruder, press, squeezer, injector, smelter, bath,
                               crusher, fermenter, massfab
landing_owned_paths          = tools/waves/recipe/gt6-material-rule-remainder/**；
                               src/recipe_generated/**/<machine>/rule_remainder/**；
                               publication_policy/*_rule_remainder.json；
                               src/test/.../gametest/Gt6MaterialRuleRemainderGameTests.java；
                               ModProcessingMachines 大型压榨机流体产出校验
landing_depends_on           = 身份就绪配方批量、前缀规则类、化学杂项、挤压机批量已关
partial_close_allowed        = false
```

## 0. 开场判断

覆盖页 §17.2 从挤压机 495 行起，同一档还有冲压 66、压榨 40、注射 39、熔炼 35、浸洗 31、粉碎 24，再往下发酵 22、物质制造 14。提示写「补材料规则模板」，是因为这些图上还挂着旧规则文件。

缺的这些行并不是少数骨架乘材料表。挤压机最大的一组是 9 种石头共用一个低温模具；冲压是空工具头配另一种宝石；粉碎的副产是另一种材料的碎矿。新写笛卡尔 `material_rule` 会造出 dump 里没有的行。这张卡按 dump 里已经生成、而且 Java 主机验证器收得下的行逐条发布。

大型压榨机原来要求至少一个物品产出。GT6 压榨可以只出流体，小压榨机已经收。本卡把大型校验改成和小型一样：没有物品产出时必须有流体产出。

## 1. 分母

选行规则与身份就绪卡相同：`published + existing + blocked = source`。未映射物品、未开门形态、有损别名、主机拒绝、以及和已有输入签名相撞的行留在 `blocked.json`。不发 stand-in。

主机过滤复刻 `ModProcessingMachines`：

- 浸洗：输入槽按 GT6 算法，`max(1000, 该流体最大配方 × 并行 × 2)`。并行是 1，全图最大进料是蒸汽 1,440,000 mB，共享槽 2,880,000 mB。TIME 能量允许 0 EU/t
- 挤压机：最大行 512 HU/t，和 T3 挤压机的 `NBT_INPUT` 一致。模具必须在 `extruder_shapes` 标签里，第 2 格 PRESERVE
- 物质制造：GT6 配方表是 2 物品进，电路是 count-0 PRESERVE。EU/t ≤ 8192
- 注射：输入槽 41,472 mB（最大进料 20,736 × 2），2 物品 / 1 物品出 / 2 流体进 / 1 流体出，EU/t ≤ 8192
- 冲压：最多 3 进 1 出，EU/t ≤ 256
- 压榨：1 进，物品出 ≤ 2，流体出 ≤ 1，EU/t ≤ 4096；物品和流体不能都空
- 熔炼：1 物品进、4 物品出、1 流体进、1 流体出。图上最大流体进料是 1 mB，4,000 mB 槽没有挡住行
- 粉碎：青铜主机 1 进、最多 12 出，EU/t ≤ 1024
- 发酵：1/1/1/1，允许电路 PRESERVE，流体进 ≤ 32,000 mB，EU/t ≤ 4096

## 2. 实施

1. 每张图一个 slug：`<machine>/rule-remainder`，publication group `cruciblecraft:<machine>/rule_remainder`。
2. 族号 `#rule_remainder_`，避开已经占用的 `#bulk_` 和 `#identity_ready_`。
3. on-demand `matrix_v1`，同形行每个 holder 不超过 2,048，更小的同形组合并后每个不超过 512。cache 16。
4. 发酵器和物质制造没有单方块主机。GameTest 只核对匹配。

## 3. 验证

```powershell
python tools/waves/recipe/gt6-material-rule-remainder/build_rule_remainder.py
python tools/build_recipe_bulk.py compile --wave <machine>/rule-remainder --write
.\gradlew.bat runGameTestServer -PwaveRecipes=recipe/gt6-material-rule-remainder
```

GameTest：`ruleRemainderGroupsStayOnDemand`、`ruleRemainderSamplesRun`。样本在 `src/test/resources/gt6_material_rule_remainder_samples.json`。

## 4. 明确不接管

- 没注册的形态、有损别名、未拆开的染色通配、以及输入签名已经有活配方的行
- 装箱机、拆箱机、锤、数控、榨汁机、复制机
- 新的笛卡尔材料规则，以及公共 16 / 长尾形态开门

## 5. 这一轮发布

`build_rule_remainder.py` 的分母已经对齐。主机收得下、而且还没活配方的行：

| 图 | source | published | existing | blocked |
| --- | ---: | ---: | ---: | ---: |
| extruder | 325595 | 473 | 308616 | 16506 |
| press | 8160 | 0 | 1917 | 6243 |
| squeezer | 5322 | 15 | 16 | 5291 |
| injector | 638 | 45 | 572 | 21 |
| smelter | 21969 | 0 | 18410 | 3559 |
| bath | 59855 | 38 | 54864 | 4953 |
| crusher | 12932 | 0 | 12213 | 719 |
| fermenter | 6435 | 11 | 943 | 5481 |
| massfab | 920 | 14 | 0 | 906 |

这次覆盖重算里，注射可翻译缺口是 0（45 行新配方加上原来的活配方）。剩下 21 行是缺流体，不是缺配方。挤压机 17、压榨 25、发酵 11、冲压 66、熔炼 35、粉碎 24 仍在。浸洗和物质制造的可翻译缺口是 0。`1.7` 的 `minecraft:yellow_flower` 折成 `minecraft:dandelion`。`minecraft:red_flower` 的 meta 是通配，不收成某一种花。

九张图里，主机拒绝现在是 0。对过 GT6 槽公式之后，改过的限制是：

- 浸洗输入槽从 4,000 mB 改成 2,880,000 mB（蒸汽 1,440,000 × 并行 1 × 2）。新发 38 行
- 挤压机能量包从 256 改成 512 HU/t，对齐 T3。新发 2 行
- 物质制造改成 2 格物品进，并允许 count-0 电路 PRESERVE。14 行都发了。没有单方块主机，GameTest 只核对匹配
- 注射输入槽从 4,000 mB 改成 41,472 mB。槽本身没有挡住行。16 色 `cfoam.<color>` 和 16 色 `cfoam.owned.<color>` 原先都折成 `cruciblecraft:construction_foam`，输入签名撞在一起。现在各是一种流体。无色 `ic2constructionfoam` 仍是 construction foam。新发 45 行。GT6 的 C-Foam 方块 meta 按 `DYE_NAMES`（黑、红、绿……），不是 1.21 染料序号；已经按这个顺序改了活配方和身份账本里的路径

冲压、压榨、熔炼、粉碎、发酵的槽和能量已经盖住 dump 里的最大值，没有再收紧或放宽。

留下的覆盖缺口按具体原因停着，不开形态、不换零件：

- 冲压：`empty:tool_head_pickaxe_gem` 还没注册
- 注射：还剩 21 行，流体本身没有注册（`ic2coolant`、`soda`、`thoriumsalt`、`bawls`）
- 熔炼：`plant_gt_*` 形态没开
- 浸洗：铅块加熔融迈达斯合金的那一行，活配方原先产出 `cruciblecraft:gold/block`。GT6 写的是 `minecraft:gold_block`，和同图金锭行一致，已经改到活配方上。可翻译缺口是 0
- 粉碎：有损别名，或未拆开的染色通配
- 发酵：`minecraft:red_flower` 通配
- 挤压机：还剩 17 行有损别名

六张有发布的图已经 `compile --write`。holder 是 `rule_remainder`，policy `on-demand`、cache 16。注射样本只核对红泡沫加空气能匹配到红色新鲜泡沫，不跑完机器。

## 6. 关闭清单

- [x] `published + existing + blocked = source`
- [x] on-demand，holder 不超过 2,048，cache 16
- [x] GameTest `cruciblecraft_wave_recipe_gt6_material_rule_remainder` 通过
- [x] 人工 `runClient` 签收：关卡时明确跳过，不作为本卡门槛
