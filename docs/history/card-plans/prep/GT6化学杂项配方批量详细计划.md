# GT6 化学杂项配方批量详细计划

> 计划 slug：`recipe/gt6-chemical-misc-bulk`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 化学杂项配方批量
> 性质：用 `exact` / `exact_multi` dump 导入化学、热处理与杂项图的全部可翻译行。
> 总计划第 7 张落地卡，见 [GT6 批量移植总计划](GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = recipe/gt6-chemical-misc-bulk
unique_active_wave           = null
prep_owned_paths             = tools/waves/prep/gt6-chemical-misc-bulk/**
landing_owned_paths          = tools/waves/mixer/ordinary-closure/**（扩选）；
                               tools/waves/recipe/gt6-chemical-misc-bulk/<map>/**；
                               src/recipe_generated/** 下对应图；src/test/.../gametest/
landing_depends_on           = 容量门已关；当前 unique-active 空窗
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

无新网格，全部主机已 live。steamcracking（7,746 行）等主机在同类基础加工机卡落地后，作为本卡的后续批次另开。

## 2. 实施

1. mixer：扩大 `build_ordinary_wave.py` 的选择到全图可翻译行，保持现有 publication group 与 shadow order。
2. 其余图：每张图 Source Pack + `recipe_import.json` + `work_set` + 人工 `production_lock`。
3. 每行带 `evidence_hashes` 与 `selected_source_recipe`。
4. 按容量门给本卡的预算发布；mixer 单图若超预算，拆成本卡内多个 shard，不拆卡。

## 3. 验证与试玩

```powershell
python tools/build_recipe_bulk.py import-source --spec <每张图的 recipe_import.json> --check
python tools/build_recipe_bulk.py compile --wave all --check
python tools/verify.py integration --profile recipes
.\gradlew.bat runGameTestServer -PwaveRecipes=recipe/gt6-chemical-misc-bulk
.\gradlew.bat runGameTestServer -PwaveRecipes=mixer/ordinary-closure
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

GameTest：每张图抽样执行；mixer 保持 `mixerReleasePerformanceGates` 并按新预算更新阈值。
人工 `runClient`：mixer、generifier、distillery 各跑一条，EMI 可见；dedicated 联机进服时间在预算内。

## 4. 明确不接管

- steamcracking、catalyticcracking 等尚无主机的图
- loom / injector / nanofab / melter 的 overflow（各有 blocker）
- 缺流体、缺形态的行

## 5. 关闭清单

- [ ] 每张图“已证明”= 当时可翻译行全数
- [ ] mixer 的 `translated_item_io` 有结论
- [ ] 容量在预算内，dedicated 同步通过
- [ ] 人工 `runClient` 签收
