# GT6 前缀规则类配方批量详细计划

> 计划 slug：`recipe/gt6-prefix-regular-bulk`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 前缀规则类配方批量
> 性质：用 Rule IR 一次导入十二张前缀规则类 GT6 图的全部可翻译行。
> 总计划第 6 张落地卡，见 [GT6 批量移植总计划](GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = recipe/gt6-prefix-regular-bulk
unique_active_wave           = null
prep_owned_paths             = tools/waves/prep/gt6-prefix-regular-bulk/**
landing_owned_paths          = tools/waves/recipe/gt6-prefix-regular-bulk/<map>/**；
                               src/recipe_generated/** 下对应图；src/component_rule_generated/** 被替换规则；
                               src/test/.../gametest/
landing_depends_on           = 挤压机配方批量已关（复用 shape_transform handler）；当前 unique-active 空窗
partial_close_allowed        = false；每张图是独立 publication group
```

---

## 0. 开场判断

这些图的主体都是“同材料换形态”，和挤压机同一套 Rule IR（`prefix_transform` / `shape_transform` +
`exact_remainder`）。一张卡带多个 `(target_map, publication_group)` 是工作流允许的。

## 1. 分母（首轮 `translatable_missing`）

| GT6 图 | 可翻译缺配方 | 现有 material_rule（文件 → 展开） | 备注 |
| --- | ---: | --- | --- |
| cutter | 20,774 | 2 → 651 | |
| mortar | 4,699 | 4 条 | 与 shredder 有同形行，归属以源图为准 |
| welder | 2,442 | 1 → 321（另有 126 行 `translated_io_only`） | |
| lathe | 2,261 | 2 → 929 | |
| rollingmill | 1,532 | 1 → 336（297 行 io_only） | 与 compressor 有同形行 |
| crusher | 1,257 | 2 条；另有 361 行 reference 追溯 | ore-chain 投影行要先去重 |
| press | 592 | 5 → 1,191 | |
| anvil.bend.big / small | 442 / 271 | bender 2 → 638 | 查 CC 承载图 |
| rollbender | 337 | 1 → 438（307 行 io_only） | |
| wiremill | 326 | 10 → 356（31 行 io_only） | |
| sifter | 208 | —；1,720 行 reference 追溯 | ore-chain |
| 合计 | 约 35,100 | | |

`translated_io_only`（时长或 EU/t 不同）的现有行在本卡按 GT6 数值改正。

## 1.1 获得格（D0）

无新网格，全部主机已 live。

## 2. 实施

1. 每张图一个 Source Pack + `recipe_import.json` + Rule IR；共用 handler。
2. crusher / sifter 的 ore-chain reference 行：先确认与 dump 行的对应，避免同一产物两套配方。
3. 被逐行覆盖的 `material_rule` 删除，规则同挤压机卡 §3。
4. 按容量门给本卡的预算发布。

## 3. 验证与试玩

```powershell
python tools/build_recipe_bulk.py import-source --spec <每张图的 recipe_import.json> --check
python tools/build_recipe_bulk.py compile --wave all --check
python tools/verify.py integration --profile recipes
.\gradlew.bat runGameTestServer -PwaveRecipes=recipe/gt6-prefix-regular-bulk
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

GameTest：每张图抽三行（规则行、余量行、边界材料）执行。
人工 `runClient`：切割机、车床、线材轧机各跑一条，EMI 可见。

## 4. 明确不接管

- 挤压机（上一张卡）
- hammer 图（手锤，不是机器；blocker `tools/world-behaviors`）
- `gt.recipe.anvil`（旧排除待决策）

## 5. 关闭清单

- [ ] 十二张图各自覆盖等式成立
- [ ] io_only 行改正为 GT6 数值
- [ ] 被覆盖规则已删
- [ ] 容量在预算内
- [ ] 人工 `runClient` 签收
