# GT6 挤压机配方补发详细计划

> 计划 slug：`recipe/gt6-extruder-remainder`
> 状态：已关（`runtime_ready`）。本文件位于 `card-plans/closed/`。
> 正式名称：GT6 挤压机配方补发
> 性质：补完 [GT6 挤压机配方批量](../closed/GT6挤压机配方批量详细计划.md) 被“主机拒收”挡下的可翻译行。
> 总计划见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)（§2：不符合模板的行进 `exact_remainder`）。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = active
capability_slug              = recipe/gt6-extruder-remainder
unique_active_wave           = recipe/gt6-extruder-bulk（复用同一 wave 与 publication group）
dump_map                     = gt.recipe.extruder（325,595 源行）
landing_owned_paths          = tools/waves/recipe/gt6-extruder-bulk/**；src/recipe_generated/**/extruder/**；
                               src/component_rule_generated/**/extruder/**；
                               ModProcessingMachines 挤压机槽位 / 验证器；machine_delivery.json 挤压机条目
landing_depends_on           = recipe/gt6-extruder-bulk 已关；当前 unique-active 空窗
partial_close_allowed        = false
```

---

## 0. 开场判断

上一张卡关卡时 171,713 行记为 `extruder host rejected recipe shape`。复查结论：

- Python `_host_legal` 先要求 `shape_transform.matches`（同材料换形态），这比 Java 主机严。
  Java `validateComponentRecipe` 只看结构：2 个输入、第 1 格消耗、第 2 格是模具且数量 0、
  无流体、输出不超过 1、EU/t 在 1–256、每格数量不超过 64。
  工具头、MTE 输入 / 输出等单输出行 Java 能跑，只是不符合模板。它们该进 `exact_remainder`，
  总计划 §2 本来就这么写，`exact_remainder` handler 也已经在 `rule_ir.json` 里，只是 0 行。
- 约 6,800 行是 GT6 把大数量产物拆成两格（两个输出）。GT6 挤压机面板是 2 进 2 出
  （`machine_delivery.json` 的 `gt6_panel.out_items = 2`），CC 主机只开了 1 个输出格。
  这是 CC 主机少开了一格，不是 GT6 的形状。
- 关卡报告写的“EU/t 超过 256”几乎不存在，本卡更正。

## 1. 分母

| 分类（上卡关卡时） | 行数 |
| --- | ---: |
| 已发布（`shape_transform`） | 127,449 |
| 主机拒收 | 171,713 |
| 缺物品 / 缺形态 / 运行时重复 / 其它 | 26,431 |

本卡只动“主机拒收”。缺形态、缺物品、工具头重映射已关决定、`storage.gem` / `plateGem`
继续 blocked，不在本卡开形态、不顶配料。

容量：容量门给挤压机 302,175 行预算；补发后总量须不超过该预算，否则本卡停下。

## 2. 做法

1. 主机判定对齐 Java：`_host_legal` 去掉模板要求，改为逐条复刻 `validateComponentRecipe`
   （输出上限改为 2，每个输出数量 1–64）。符合模板的行仍走 `shape_transform`，其余走
   `exact_remainder`，每行保留 `source_row_sha256`。
2. 挤压机主机开第 2 个输出格：`SlotLayout` 4 格（0 材料、1 模具、2/3 产物），
   验证器对挤压机允许 2 个输出，GUI 使用面板已有的第 2 个输出位，
   `machine_delivery.json` 的 `slots.item_outputs` 改为 2。旧 3 格存档按
   `LayoutAwareItemStackHandler` 读入 4 格布局，不隔离。
3. 同输入对（材料 + 模具）出现多行时按 GT6 先注册者优先；后来者记 blocked
   “shadowed by earlier row”，不发布。
4. 重跑流水线；被逐行覆盖的挤压机 `material_rule` 文件删除，差集为空才删。
   比对用运行时物品 id（含原版 `form_items`），不用操作数上的 material/form 字段。
   17 个文件的每一行都对上已发布 GT6 行，全部退出线上数据包；20 张 T14 表由
   `build_component_rules.py` 改写到 `src/test/resources/extruder_compact_rule_fixture/`，
   只作 `MaterialRule` 稀疏表编解码 / 展开的测试夹具，`--check` 仍校验夹具与源一致。
5. 这条规则（不符合模板 ≠ 主机拒收，先对 Java 验证器）写进
   [recipe-wave-workflow.md](../../../current/recipe-wave-workflow.md)，后续配方卡共用。

## 3. 验证

```powershell
python tools/waves/recipe/gt6-extruder-bulk/build_extruder_bulk.py
python tools/build_recipe_bulk.py import-source --spec tools/waves/recipe/gt6-extruder-bulk/recipe_import.json --write
python tools/waves/recipe/gt6-extruder-bulk/finish_extruder_bulk.py
python tools/waves/recipe/gt6-extruder-bulk/record_unaccounted.py
python tools/waves/recipe/gt6-extruder-bulk/write_publication_policy.py
python tools/waves/recipe/gt6-extruder-bulk/cover_material_rules.py
python tools/build_recipe_bulk.py compile --wave recipe/gt6-extruder-bulk --write
python tools/build_recipe_bulk.py compile --wave all --check
python tools/verify.py integration --profile recipes
.\gradlew.bat runGameTestServer -PwaveRecipes=recipe/gt6-extruder-bulk
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

GameTest：抽样一条 `exact_remainder` 行（非同材料换形态）与一条双输出行，真机器跑完，
产物落在两个输出格；router 对无关输入返回空。
JUnit：挤压机 4 格布局、GUI 位置、验证器接受 2 输出、拒绝 3 输出。
人工 `runClient`：挤压机做一个工具头、一次双输出挤压，EMI 显示两个产物。

## 4. 明确不接管

- 缺形态 / 缺物品 / 工具头重映射 / `storage.gem`、`plateGem`（身份卡或已关决定）
- 其它配方图
- 模板之外的新 handler

## 5. 实施中发现的问题

- 低温模具（`Shape_SimpleEx` 10200–10231）靠 `extruder_shapes` 标签被 Java 认作模具；
  Python 只认 `extruder_shape_` 前缀，误拒 6.3 万行。改为读同一标签。
- `record_unaccounted.py` 把证明里的 `host_rejected_rows` 覆盖成“新补记行数”，
  上卡的 171,713 由此而来。改记 `unaccounted_rows`；拒收计数从 `blocked.json` 原因统计。
- 批量翻译器读 `centrifuge_common.FIXTURE_ONLY_LOSSY_ITEM_ALIASES`（锆碎屑 → 锆粉等）。
  挤压机命中的 17 行记 blocked。离心机 / 浴锅 / 大型粉碎机源账本约 75 行也引用该表，
  是否已线上发布待另卡核查。
- `finish` 剪 `source.json` 使 `import-source --check` 永远不过；现在同步修剪
  `work_set.json` 并重排 `shadow_order`，流水线跑两轮 import + finish。
- 4,096 行一组时补发行的同步负载达 618 KB（上限 512 KiB）；分组改为 2,048 行，共 9,713 组。
- 上卡删 3 个规则文件时没改生成器，`build_component_rules --check`、
  `ExtruderCompactRuleTest`、`CompactGTRecipeFamilySyncSizeTest` 已是红的；本卡一并修复。
- 与本卡无关、HEAD 已有：block-object compile spec 的 lock 哈希不符，
  `compile --wave all --check` 因此失败。

## 6. 关闭清单

- [x] 主机判定与 Java 验证器一致；拒收只剩 2 行 EU/t 超限与 17 行有损别名，按原因分桶
- [x] 挤压机 2 输出格；JUnit 与隔离 GameTest（`bulkMapPublishesTranslatedRows`、
  `remainderAndTwoOutputRowsRunOnMachine`）通过；旧 3 格存档按布局读入不隔离
- [x] 覆盖等式成立：发布 299,143（模板 132,211 + 逐行 166,932，其中双输出 6,808），
  blocked 26,450，合计 325,593 + 2 行 dump 内完全重复；在 302,175 预算内
- [x] 被覆盖的 material_rule 已删，差集为空（17 个文件全部行对上）
- [x] 上卡关卡报告已更正；工作流规则写入 `recipe-wave-workflow.md` §3.2
- [x] 覆盖文档重建：挤压机已证明 299,145（目标 93.7%），全项目配方进度 60.1%
- [x] `recipes` / `recipe-generators` profile：`recipe-generators` 通过；`recipes` 只剩
  `test_prep_machines` 两条旧断言（关卡前 unique-active 非空；`ModMenus` 已含 printer）
- [ ] 全量 GameTest 加载指标更新：`cruciblecraft_default_grid` 在本卡前已大面积失败并在
  `MteInPlaceBlockEntity.itemHandler` 空指针处崩服，跑不到指标断言；`CrucibleCraftGameTests`
  里已去掉旧 compact 挤压机家族的逐行检查、组件链改为按输入查批量配方，指标常数待该套修复后重录
- [ ] 人工 `runClient` 签收
