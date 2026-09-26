# GT6 化学杂项配方批量详细计划

> 计划 slug：`recipe/gt6-chemical-misc-bulk`
> 状态：已关（`runtime_ready`，`workflow=accepted`）。本文件位于 `card-plans/closed/`。
> 正式名称：GT6 化学杂项配方批量
> 性质：用 `exact` / `exact_multi` dump 导入化学、热处理与杂项图的全部可翻译行。
> 总计划第 7 张落地卡，见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = recipe/gt6-chemical-misc-bulk
unique_active_wave           = null
landing_owned_paths          = tools/waves/recipe/gt6-chemical-misc-bulk/**；
                               src/recipe_generated/** 下各图 chemical_misc/；
                               publication_policy/<图>_chemical_misc.json；src/test/.../gametest/
landing_depends_on           = 容量门已关；前缀规则类卡已关
partial_close_allowed        = false；每张图是独立 publication group
```

---

## 0. 开场判断

这些图的行没有统一的形态规律，只能逐行导入。多数行身份已就绪，缺的只是没被选进任何 production lock：
例如 mixer 64,245 行里 92% 就绪，但 `mixer/ordinary-closure` 只锁了 7,162 行。
不是 overflow，也不是身份问题。

## 1. 分母（2026-09-25 重跑，`translatable_missing`）

| GT6 图 | 可翻译缺配方 | 现有 | 路径 |
| --- | ---: | --- | --- |
| mixer | 51,804 | `mixer/ordinary-closure` 7,162 行 | 扩选现有 ordinary 波。另有缺流体 570 |
| generifier | 8,650 | 6 行 | 新 Source Pack |
| smelter | 1,542 | ordinary 波已证明 16,821 | 扩选。另有缺形态 3,442 |
| centrifuge | 1,000 | ordinary 波已证明 252 | 扩选 |
| fermenter | 919 | 1 行 | 新。另有缺流体 3,988，不在本卡顶 |
| freezer | 901 | 4 行未追溯 | 新 |
| distillery | 888 | 7 行 | 新。另有缺流体 620 |
| polarizer | 880 | 3 行未追溯 | 新（MU 主机已 live） |
| loom | 691 | `machines/loom` 476 行 | 扩选；858 行 overflow 另有 blocker |
| laserengraver | 577 | 8 行 | 新。另有缺物品 1,210 |
| magneticseparator | 179 | 少量 | 新 |
| compressor | 183 | ordinary 波已证明 1,289 | 扩选 |
| electrolyzer | 94 | ordinary 波 | 扩选 |
| nanofab | 55 | 少量 | 扩选 |
| 合计 | 68,363 | | |

mixer 另有 4,556 行 `translated_item_io`（物品对、流体不对），先查是否翻译链问题，再决定改正。

容量预算（`recipe/gt6-bulk-capacity`）：68,363 行走 on-demand `matrix_v1`，按图向上取整共 28 个 holder，每个不超过 4,096 行。每个新 family 的 cache 声明不超过 16。不进 eager。

## 1.1 获得格（D0）

无新网格，全部主机已 live。steamcracking（7,746 行）等主机在同类基础加工机卡落地后另开；2026-09-26 已由 `recipe/gt6-steamcracking-bulk` 导入。

主机按 GT6 面板补齐，不把这些行记 blocked：

- generifier 补 1 个物品输入、1 个物品输出（GT6 `RM.Generifier` 面板 1/1/1/1）。物品行 0 EU/t、时长按 dump；流体行仍是 1 tick。
- smelter 补 1 个流体输入槽（4,000 mB），收煎炸油、熟铁、退火铜这类流体转流体行。输出罐 8,000 mB 不动。
- distillery 允许不消耗的选择电路（`PRESERVE`）。
- laser engraver 不再限定 64 tick，GT6 有 16 tick 的行。
- mixer / distillery / compressor / electrolyzer 是专用化学图，接受 dump 导入的 `gt6/` 配方 id。

fermenter 只有 Large Fermenter 一个主机，GameTest 只查匹配，不跑多方块。

## 2. 实施

1. 十四张图各一个 Source Pack + `recipe_import.json` + `work_set` + 人工 `production_lock`，
   slug 是 `<图>/chemical-misc`，publication group 是 `cruciblecraft:<图>/chemical_misc`，全部 on-demand。
   mixer 不再扩选 `mixer/ordinary-closure`：那一波已封口且带 eager 组，新行走容量门要求的 on-demand，
   另开一组。已发布的 7,198 行原样保留。
2. 选行：已被 hash 证明或翻译后完全一致的行记 `existing`，不重复发布；其余行按主机 Java 验证器过滤，
   与同图已有配方输入签名相同的记 blocked。`published + existing + blocked = source`。
3. 每行带 `evidence_hashes` 与 `selected_source_recipe`。
4. 按容量门给本卡的预算发布；每个 holder 不超过 2,048 行，单图超预算就拆成本卡内多个 family，不拆卡。

## 3. 验证与试玩

```powershell
python tools/waves/recipe/gt6-chemical-misc-bulk/build_chemical_misc.py
python tools/build_recipe_bulk.py import-source --spec tools/waves/recipe/gt6-chemical-misc-bulk/<图>/recipe_import.json --check
python tools/build_recipe_bulk.py compile --wave <图>/chemical-misc --check
.\gradlew.bat runGameTestServer -PgameTestGrid=content
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

提交的 GameTest 在 `cruciblecraft_content`：`chemicalMiscGroupsStayOnDemand`、`chemicalMiscSamplesRun`。
样本在 `src/test/resources/gt6_chemical_misc_samples.json`，每张图一到两行；样本的实际匹配必须产出 GT6 的输出。
人工 `runClient`：mixer、generifier、distillery 各跑一条，EMI 可见；dedicated 联机进服时间在预算内。

## 4. 明确不接管

- steamcracking、catalyticcracking 等尚无主机的图
- loom / injector / nanofab / melter 的 overflow（各有 blocker）
- 缺流体、缺形态的行

## 4.1 本轮发布

十四张图 `import-source --check` 与 `compile --check` 都当前。三个网格跑过：

- content：本卡两条 GameTest 通过；其余 6 条失败都已在 `tools/waves/prep/test-authoring-workflow/disposition.json`。
- machines：`laserEngraverKeepsGt6FoilRows` 原来写死激光雕刻图 8 行，改成手写 8 行加上本卡 on-demand family 的行数；
  其余 17 条失败都已在 disposition。
- default：三条发布快照测试失败。本卡的 72,626 行已登记为
  `chemical_misc_bulk_publication_baseline.json`；剩下 60,055 行是挤压机补发卡和前缀规则类卡没登记的增量，
  记进 disposition，不在本卡改它们的字面值。

| 图 | 源行 | 发布 | 已有 | blocked | holder |
| --- | ---: | ---: | ---: | ---: | ---: |
| mixer | 64,245 | 56,370 | 7,228 | 647 | 37 |
| generifier | 10,236 | 8,647 | 7 | 1,582 | 14 |
| smelter | 21,969 | 1,582 | 16,820 | 3,567 | 6 |
| centrifuge | 1,296 | 647 | 604 | 45 | 8 |
| fermenter | 6,435 | 941 | 2 | 5,492 | 15 |
| freezer | 957 | 896 | 2 | 59 | 16 |
| distillery | 1,517 | 886 | 6 | 625 | 4 |
| polarizer | 943 | 870 | 0 | 73 | 15 |
| loom | 1,334 | 690 | 476 | 168 | 2 |
| laser engraver | 1,787 | 569 | 0 | 1,218 | 4 |
| magnetic separator | 179 | 178 | 0 | 1 | 3 |
| compressor | 1,472 | 183 | 1,289 | 0 | 4 |
| electrolyzer | 290 | 112 | 178 | 0 | 3 |
| nanofab | 64 | 55 | 9 | 0 | 1 |
| 合计 | 112,724 | 72,626 | 26,621 | 13,477 | 132 |

“已有”是已被 hash、dump 行号或翻译后完全一致证明的行，留在原来的波，不重复发布。
发布数比 §1 的 68,363 多，是因为 `translated_item_io` 也发布了：mixer 的 4,556 行里 4,553 行是
建筑泡沫配方换了一种水（矿泉水、光谱露等），CC 已有的是蒸馏水或水那一行；另 3 行没有物品输入。
它们是 GT6 各自独立的行，输入签名不同，不是翻译链问题。其他图的 `translated_item_io` 同样处理。

同形行（时长、EU/t、数量、动作都相同）不少于 16 行的走 `matrix_v1`，每个 holder 不超过 2,048 行；
更小的同形组合并成普通 relation holder，每个不超过 512 行。全部 on-demand，cache 16。
全局 on-demand 逻辑行约 484,461，在 500,000 硬顶以内。

`recipe_bulk` 修了一处：派生 wave 的 `compile --write` 原来按主机目录清理，会删掉同主机其他波的 holder。
现在只清理本波 production lock 里的 cohort 目录，`--check` 同样只读这些目录。

## 4.2 本卡补不完、以后再查

逐行账本：`tools/waves/recipe/gt6-chemical-misc-bulk/<图>/blocked.json`，`reason` 带 GT6 物品或流体 id。
十四张图 blocked 合计 13,477。下表是关卡时的分类，减去 2026-09-26 补做移出的行；加总 13,479，比逐行账本多 2。这 2 行是离心机在输入罐放宽后一并发布的，关卡分类时不在「主机拒收」那 4 行里。

| 类 | 行 | 落在 | 以后 |
| --- | ---: | --- | --- |
| 缺流体 | 5,359 | fermenter 3,988，mixer 624，distillery 620，smelter 58，generifier 56，centrifuge 13 | 果汁、醋、染料流体、药水等。蒸馏塔药水流体用户 2026-09-26 决定先不补，留在 distillery 的 620 行里。其他 mod 的流体走全覆盖页 §17.3 排除候选；GT6 自己的走流体卡 |
| 没有现代目录 id | 3,300 | smelter 1,558，fermenter 1,226，generifier 357，laser engraver 85，loom 37，mixer 23，其他 14 | 食物 577、草捆 576、`plantGt*` 形态约 1,445、`storage.raw` 280 等。形态走普查，不在配方卡开门 |
| `bulletGt*` | 2,853 | smelter 1,872，generifier 921，polarizer 36，freezer 24 | 总计划 §1.3 / §6。不补映射、不开形态 |
| 缺装饰石头变种 | 1,248 | laser engraver 1,125，generifier 123 | 同前缀规则类卡 §4.2 的装饰石头缺口，另开建筑方块卡 |
| ore-chain 规则占了同一输入 | 6 | centrifuge 3，smelter 3 | 352 行纯净矿离心已改成 GT6 的 9 个微小离心矿。剩下 3 行矿链仍是 topology，加上熔炉 3 行 |
| 未映射 MTE | 292 | loom 130，fermenter 128，smelter 14，centrifuge 13，generifier 7 | 没有 CC 对应的 MTE，不发明 |
| 其他未注册方块 | 130 | fermenter 128，smelter 2 | 荧光睡莲等，无目录 id |
| 主机拒收 | 0 | — | 熔炉输出罐已抬到 GT6 配方上限 13,032 mB；离心机青铜输入罐 200,000 mB。原先 127 行已发布 |
| 工具头 | 98 | smelter 47，polarizer 27，freezer 24 | 已关闭的工具头重映射，同前缀规则类卡 |
| 其他 mod 物品 | 74 | generifier 73，smelter 1 | `ForgeMicroblock:stoneRod` 等。总计划 §6 |
| 重复 | 59 | generifier 33，polarizer 10，smelter 8，freezer 6，distillery 2 | 重复源行或同一运行时身份，已丢掉，不是待补 |
| 1.7 旧 vanilla id | 28 | fermenter 22，freezer 4，generifier 1，smelter 1 | `minecraft:planks`、`red_flower`、`snow_layer`、`stonebrick` 等，1.21 没有这些 id |
| 与同图已有配方同一输入 | 24 | generifier 11，laser engraver 8，smelter 3，freezer 1，magnetic separator 1 | 已有手写或旧波配方，不重复发布 |
| 旧波的无编号电路 | 1 | loom 1 | nanofab 旧波 7 行已带上 GT6 电路号，17 行已发布。loom 那 1 行仍无编号 |
| 有损夹具别名 | 7 | centrifuge 7 | `FIXTURE_ONLY_LOSSY_ITEM_ALIASES`，禁止发布 |

## 5. 关闭清单

- [x] 每张图 `published + existing + blocked = source`。分类在 §4.2，逐行在各图 `blocked.json`
- [x] mixer 的 `translated_item_io` 有结论：是不同的水，不是翻译链问题，已发布
- [x] 容量在预算内；content 网格 `chemicalMiscGroupsStayOnDemand` 与 `chemicalMiscSamplesRun` 通过
- [ ] dedicated 联机同步实测
- [ ] 人工 `runClient` 签收
