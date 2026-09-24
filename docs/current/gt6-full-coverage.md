# GT6 全量覆盖重评估

> 本页由 `tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py`
> 从本地 GT6 分母、当前 capability、blocker、wave overflow 和工作树配方生成，不要手改。
> 何时重跑、各列口径和 CI 检测见 [`gt6-full-coverage-workflow.md`](gt6-full-coverage-workflow.md)。
> 各节单位不同（配方行、机器 kind、多方块、前缀、身份、blocker），**不得相加**，
> 也不把 `runtime_ready` 当作完整 GT6 覆盖。

GT6 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`。

## 1. 总览（按轴）

| 轴 | 分母 | 当前状态分布 |
| --- | --- | --- |
| 配方图 | 95 maps / 720841 源行 | `denominator_only` 17，`runtime_only` 4，`bounded_subset` 52，`full_replay` 6，`empty_source` 14，`legacy_exclusion_pending` 2 |
| **配方移植进度** | 目标 706969 源行（720841 − 决策排除 13872） | **已证明 17.4%**（122768）；身份就绪 67.0%；按机器平均 27.2%（73 台，完成 7，未开始 33） |
| 配方源行逐行分类 | 720841 源行 | 已证明 122768（17.0%）；部分一致 9014（1.3%）；缺配方 341806（47.4%）；缺身份 224035（31.1%）；展示用 13576（1.9%）；旧排除待决策 9642（1.3%）（互斥，合计等于分母） |
| 机器 kind | 96 canonical kinds | `denominator_only` 16，`identity_only` 1，`runtime_code_uncarded` 1，`runtime_only` 8，`runtime_paused` 2，`bounded_subset` 43，`runtime_accepted` 21，`full_replay` 4 |
| 多方块控制器 | 30 canonical kinds | `identity_only` 2，`runtime_code_uncarded` 2，`runtime_paused` 6，`runtime_accepted` 20 |
| 盖板 | 47 canonical kinds | 有 CC live id 35，无 12 |
| 能量身份 | 20 identities | 有 CC `EnergyType` 10，无 10 |
| 物品/流体生成域 | 25 domains | `deferred_with_reason` 3，`in_scope` 16，`out_of_scope` 6（冻结分母） |
| 材料前缀 | 452 canonical prefixes | `deferred_with_reason` 271，`in_scope` 55，`out_of_scope` 126；CC live 已映射 152（冻结分母记 55） |
| MTE 身份 | 1817 identities | `identity_only` 746，`inplace_runtime` 788，`realized_natively` 283 |
| 材料形态需求 | 68 demand pairs | openable 5，gated_unresolved 59，ungated 规模 5768（规模，非待办） |
| Capability | 112 | `frozen:accepted` 2，`frozen:paused` 12，`runtime_ready:accepted` 95，`runtime_ready:paused` 3；survival_access `blocked` 2，`not_applicable` 2，`partial` 23，`unreviewed` 21，`unset` 64 |
| Blocker | 62 | `open` 29，`out_of_scope` 6，`partial` 2，`resolved` 20，`superseded` 5 |
| 项目试玩 | `2026-09-15-obtain-reset` | `pending` |

配方源行逐行分类（每一条 GT6 源行只落一类，合计等于分母；口径见工作流文档第 3.3 节）：

- **已证明** 122768（17.0%）：`source_exact` 119375，`translated_exact` 3393
- **部分一致** 9014（1.3%）：`translated_io_only` 1387，`translated_item_io` 7627
- **缺配方** 341806（47.4%）：`translatable_missing` 341806
- **缺身份** 224035（31.1%）：`missing_material_form` 194334，`missing_material` 0，`missing_fluid` 10475，`missing_object` 19226
- **展示用** 13576（1.9%）：`display_only` 13576
- **旧排除待决策** 9642（1.3%）：`legacy_exclusion_pending` 9642

翻译链校准：在 119168 对 hash 已证明的“CC 行 ↔ GT6 源行”上，翻译后完全一致 118618（99.5%），不一致 524，不可翻译 26。不一致的是真实移植差异（例如缺电路编号、有意替换），样例见 `semantic_coverage.json`。

按交付深度的源行数：

- `denominator_only`：37386 源行，已追溯 0
- `runtime_only`：29648 源行，已追溯 0
- `bounded_subset`：635812 源行，已追溯 111022
- `full_replay`：8353 源行，已追溯 8353
- `empty_source`：0 源行，已追溯 0
- `legacy_exclusion_pending`：9642 源行，已追溯 0

## 2. 全部 GT6 配方图（95）

列口径（详见工作流文档第 3 节）：

- **逐行已证明**：CC 运行时配方行上的 evidence hash 对上 GT6 dump 行后，按不同源行去重计数，与“源行”同单位，括号是占比。`full_replay` 只看这一列是否等于源行数；逐行分类见第 2.2 节。
- **reference 追溯**：ore-chain 这类按材料族投影的配方只能追到 `gt6_recipe_normalized_reference.json` 的归一化行；归一化会合并多条 dump 行，所以单列、不相加、不判 `full_replay`。
- **CC 承载图**：这些源行落在哪些 CC RecipeMap 上，数字是 CC 配方行数；一条 GT6 行可展开成多条 CC 行（OreDict 备选），所以可以大于已追溯源行。GT6 把很多相同的行同时注册进几张图（melter/smelter、compressor/rollingmill、mortar/shredder），归属以配方自己声明的源图为准。
- **CC 未追溯行**：落在本图对应 CC 图上、但没有行级 GT6 evidence 的配方行（datagen 手写、`gt6_java_source`、bootstrap、design policy 等），只证明有内容，不算源行。
- **材料规则**：`material_rule` 文件数 → `component_rule_manifest.json` 记录的离线展开数；没有展开数的规则由运行时按材料展开。

扫描范围：12 个运行时资源根（含 `src/generated/resources`），按 `source-sets.gradle` 排除 5 个 pattern；CC 配方行 121854，材料规则文件 118。源行归属钉在 `tools/waves/portfolio/gt6-full-coverage-reassessment/source_attribution.json`（覆盖 0，未能在 dump 中找到 0，多图歧义 0）。

| GT6 map | 源行 | 历史源分母分类 | 当前交付深度 | 逐行已证明 | reference 追溯 | CC 承载图（CC 行） | CC 未追溯行 | 材料规则 | overflow | capability | blocker |
| --- | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- | --- |
| `gt.recipe.bedrockorelist` | 52 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.bumblequeen` | 80 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.burnmixer` | 29 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.byproductlist` | 289 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.catalyticcracking` | 3 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.chisel` | 2 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.cncmachine` | 38 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.crystallisationcrucible` | 132 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.fuels.turbine` | 1 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.hammer` | 309 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | identity/processing-ungated-families, tools/world-behaviors |
| `gt.recipe.juicer` | 96 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | recipe/squeezer-dump-5322, worldgen/food |
| `gt.recipe.other` | 71 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | obtain/injector-mv-hv-iv-hosts, obtain/wooden-bathing-pot-glue, recipe/loom-overflow |
| `gt.recipe.scannervisuals` | 50 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.steamcracking` | 7746 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.toolhead` | 10960 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.trees` | 15 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | fluid/resin-rubber, fluid/sap-maple, worldgen/crops-glowtus-bush |
| `gt.recipe.unboxinator` | 17513 | deferred_with_reason | `denominator_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.boxinator` | 27291 | deferred_with_reason | `runtime_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.bumblelyzer` | 1440 | deferred_with_reason | `runtime_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.printer` | 22 | deferred_with_reason | `runtime_only` | 0 | 0 | — | 0 | — | 22 | — | recipe/printer-dye-fluids |
| `gt.recipe.replicator` | 895 | deferred_with_reason | `runtime_only` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.anvil.bend.big` | 723 | in_scope | `bounded_subset` | 0 | 0 | — | 0 | 3 条 | 0 | — | — |
| `gt.recipe.anvil.bend.small` | 507 | in_scope | `bounded_subset` | 0 | 0 | — | 0 | 2 条 | 0 | — | — |
| `gt.recipe.assembler` | 1582 | in_scope | `bounded_subset` | 343（21.7%） | 0 | assembler 343 | 0 | 32 条 → 568 | 0 | — | architecture/combinatorial-leftover |
| `gt.recipe.autoclave` | 392 | in_scope | `bounded_subset` | 366（93.4%） | 0 | autoclave 366 | 0 | — | 0 | — | — |
| `gt.recipe.bath` | 59855 | in_scope | `bounded_subset` | 49417（82.6%） | 0 | bath 49417 | 58 | 1 条 | 0 | machines/bath, machines/large-bathing-vat | identity/bath-large-vat-17104, obtain/wooden-bathing-pot-glue, recipe/bath-identity-families, recipe/bath-remainder-families |
| `gt.recipe.canner` | 3609 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 32 | — | 0 | — | — |
| `gt.recipe.centrifuge` | 1296 | in_scope | `bounded_subset` | 252（19.4%） | 816 | centrifuge 610 | 20 | — | 0 | machines/large-centrifuge | — |
| `gt.recipe.coagulator` | 5 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 2 | — | 0 | machines/large-coagulator | — |
| `gt.recipe.cokeoven` | 124 | in_scope | `bounded_subset` | 0 | 0 | — | 77 | — | 0 | machines/coke-oven | — |
| `gt.recipe.compressor` | 1472 | in_scope | `bounded_subset` | 1289（87.6%） | 0 | compressor 1289 | 0 | — | 0 | — | — |
| `gt.recipe.crusher` | 12932 | in_scope | `bounded_subset` | 0 | 361 | crusher 352 | 152 | 2 条 | 0 | — | — |
| `gt.recipe.cryodistillationtower` | 3 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 1 | — | 0 | machines/distillation-tower | — |
| `gt.recipe.cryomixer` | 57 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 4 | — | 0 | — | — |
| `gt.recipe.cutter` | 27454 | in_scope | `bounded_subset` | 0 | 0 | — | 0 | 2 条 → 651 | 0 | — | — |
| `gt.recipe.distillationtower` | 8 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 6 | — | 0 | machines/distillation-tower | — |
| `gt.recipe.distillery` | 1517 | in_scope | `bounded_subset` | 6（0.4%） | 0 | distillery 7 | 0 | — | 0 | — | — |
| `gt.recipe.drying` | 217 | in_scope | `bounded_subset` | 198（91.2%） | 0 | drying 198 | 0 | — | 0 | — | — |
| `gt.recipe.electrolyzer` | 290 | in_scope | `bounded_subset` | 178（61.4%） | 0 | electrolyzer 178 | 0 | — | 0 | machines/large-electrolyzer | architecture/combinatorial-leftover |
| `gt.recipe.extruder` | 325595 | in_scope | `bounded_subset` | 0 | 0 | — | 2 | 28 条 → 2782 | 0 | — | — |
| `gt.recipe.fermenter` | 6435 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 1 | — | 0 | machines/large-fermenter | worldgen/food |
| `gt.recipe.freezer` | 957 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 4 | — | 0 | — | — |
| `gt.recipe.fuels.burn` | 49 | in_scope | `bounded_subset` | 23（46.9%） | 0 | fuels_gas 23 | 0 | — | 0 | — | — |
| `gt.recipe.fuels.engine` | 21 | in_scope | `bounded_subset` | 6（28.6%） | 0 | fuels_engine 6 | 1 | — | 0 | — | — |
| `gt.recipe.fuels.hot` | 12 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 8 | — | 0 | — | — |
| `gt.recipe.fusionreactor` | 18 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 18 | — | 0 | — | energy/reactor-fusion |
| `gt.recipe.generifier` | 10236 | in_scope | `bounded_subset` | 6（<0.1%） | 0 | generifier 6 | 0 | — | 0 | — | — |
| `gt.recipe.implosioncompressor` | 1072 | deferred_with_reason | `bounded_subset` | 776（72.4%） | 0 | implosion_compressor 776 | 0 | — | 0 | machines/implosion-compressor | — |
| `gt.recipe.injector` | 638 | deferred_with_reason | `bounded_subset` | 103（16.1%） | 0 | injector 103 | 0 | — | 535 | machines/injector | fluid/ic2-coolant, fluid/thorium-salt, obtain/injector-mv-hv-iv-hosts, recipe/injector-overflow |
| `gt.recipe.laminator` | 498 | deferred_with_reason | `bounded_subset` | 486（97.6%） | 0 | laminator 486 | 6 | — | 12 | machines/laminator | identity/redstone-insulated-extras, obtain/redstone-wiregt01, recipe/laminator-overflow |
| `gt.recipe.laserengraver` | 1787 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 8 | — | 0 | — | — |
| `gt.recipe.lathe` | 2528 | in_scope | `bounded_subset` | 0 | 0 | — | 0 | 2 条 → 929 | 0 | — | — |
| `gt.recipe.lightning` | 12 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 1 | — | 0 | — | — |
| `gt.recipe.loom` | 1334 | deferred_with_reason | `bounded_subset` | 476（35.7%） | 0 | loom 476 | 0 | — | 858 | machines/loom | recipe/loom-overflow |
| `gt.recipe.magneticseparator` | 179 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 1 | — | 0 | — | — |
| `gt.recipe.massfab` | 920 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 1 | — | 0 | — | — |
| `gt.recipe.melter` | 6756 | deferred_with_reason | `bounded_subset` | 3601（53.3%） | 0 | melter 3601 | 0 | — | 3155 | machines/melter | recipe/melter-overflow |
| `gt.recipe.mixer` | 64245 | in_scope | `bounded_subset` | 7198（11.2%） | 0 | mixer 7218 | 3 | — | 0 | machines/large-mixer | — |
| `gt.recipe.mortar` | 6318 | in_scope | `bounded_subset` | 0 | 0 | — | 0 | 4 条 | 0 | — | — |
| `gt.recipe.nanofab` | 64 | deferred_with_reason | `bounded_subset` | 7（10.9%） | 0 | nanofab 7 | 2 | — | 57 | machines/nanofab | obtain/nanofab-hosts, recipe/nanofab-overflow |
| `gt.recipe.polarizer` | 943 | deferred_with_reason | `bounded_subset` | 0 | 0 | — | 3 | — | 0 | — | identity/processing-ungated-families |
| `gt.recipe.press` | 8160 | in_scope | `bounded_subset` | 0 | 0 | — | 23 | 5 条 → 1191 | 0 | — | recipe/pressure-washer-stone |
| `gt.recipe.roaster` | 115 | deferred_with_reason | `bounded_subset` | 73（63.5%） | 0 | roaster 73 | 1 | — | 0 | — | — |
| `gt.recipe.rollbender` | 940 | in_scope | `bounded_subset` | 0 | 0 | — | 0 | 3 条 → 1076 | 0 | — | — |
| `gt.recipe.rollformer` | 28 | deferred_with_reason | `bounded_subset` | 26（92.9%） | 0 | rollformer 26 | 0 | — | 0 | — | recipe/roll-former-rail-gt |
| `gt.recipe.rollingmill` | 2438 | in_scope | `bounded_subset` | 0 | 0 | — | 0 | 1 条 → 336 | 0 | — | — |
| `gt.recipe.shredder` | 41246 | in_scope | `bounded_subset` | 29365（71.2%） | 14803 | shredder 29722 | 0 | — | 11881 | machines/large-shredder | — |
| `gt.recipe.sifter` | 2877 | in_scope | `bounded_subset` | 0 | 1720 | sifter 351 | 6 | — | 0 | — | — |
| `gt.recipe.sluice` | 4840 | in_scope | `bounded_subset` | 0 | 3183 | sluice 355 | 2 | — | 0 | machines/large-sluice | — |
| `gt.recipe.smelter` | 21969 | in_scope | `bounded_subset` | 16812（76.5%） | 2003 | smelter 16933 | 91 | 1 条 | 0 | — | — |
| `gt.recipe.squeezer` | 5322 | deferred_with_reason | `bounded_subset` | 15（0.3%） | 0 | squeezer 15 | 5 | — | 0 | machines/large-squeezer | identity/processing-ungated-families, recipe/squeezer-dump-5322, worldgen/crops-glowtus-bush |
| `gt.recipe.welder` | 4762 | in_scope | `bounded_subset` | 0 | 0 | — | 0 | 15 条 → 321 | 0 | — | — |
| `gt.recipe.wiremill` | 455 | in_scope | `bounded_subset` | 0 | 0 | — | 0 | 10 条 → 356 | 0 | — | — |
| `gt.recipe.clustermill` | 307 | deferred_with_reason | `full_replay` | 307（100.0%） | 0 | clustermill 307 | 0 | — | 0 | — | — |
| `gt.recipe.fuels.fluidbed` | 55 | deferred_with_reason | `full_replay` | 55（100.0%） | 0 | fuels_fluidbed 55 | 0 | — | 0 | — | — |
| `gt.recipe.fuels.gas` | 9 | deferred_with_reason | `full_replay` | 9（100.0%） | 0 | fuels_gas_turbine 9 | 0 | — | 0 | — | — |
| `gt.recipe.pressurewasher` | 312 | deferred_with_reason | `full_replay` | 312（100.0%） | 0 | pressurewasher 312 | 24 | — | 0 | — | — |
| `gt.recipe.sharpener` | 7637 | deferred_with_reason | `full_replay` | 7637（100.0%） | 0 | sanding 7637 | 0 | — | 0 | machines/sanding | identity/sanding-grindstone-32703 |
| `gt.recipe.slicer` | 33 | deferred_with_reason | `full_replay` | 33（100.0%） | 0 | slicer 33 | 0 | — | 0 | machines/slicer | material-form/paper-tiny-plate |
| `(unnamed)` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.autocrafting` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.blastfurnace` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.calciner` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.cooker` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.cruciblesmelting` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.fuels.magic` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.fuels.plasma` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | energy/reactor-fusion |
| `gt.recipe.microwave` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.plantalyzer` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.scannermolecular` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.vacuumfreezer` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `mc.recipe.furnace` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | recipe/oven-cooking-oil-xp |
| `mc.recipe.furnacefuel` | 0 | deferred_with_reason | `empty_source` | 0 | 0 | — | 0 | — | 0 | — | — |
| `gt.recipe.anvil` | 9228 | out_of_scope | `legacy_exclusion_pending` | 0 | 0 | — | 0 | 7 条 | 0 | — | — |
| `gt.recipe.cruciblealloying` | 414 | out_of_scope | `legacy_exclusion_pending` | 0 | 0 | — | 0 | — | 0 | — | — |

### 2.1 没有归属 GT6 图的 CC 配方图

这些 CC RecipeMap 上有配方行或材料规则，但不对应任何 GT6 分母行，上表不计。“已追溯到 GT6”表示其中的行已经算进上表别的 GT6 图。

| CC map | 已注册 | CC 行 | 已追溯到 GT6 | 未追溯 | 材料规则 |
| --- | --- | ---: | ---: | ---: | ---: |
| `fusion_extension` | 是 | 1 | 0 | 1 | — |

### 2.2 逐行分类（每张 GT6 图）

每条 GT6 源行都用 recipe wave 的翻译链（`dialects/gt6.compile_row` + `emit`）翻成 CC 身份，再和该图对应 CC RecipeMap 上的全部运行时配方行、材料规则展开逐条比较。类别含义：

- `source_exact`：hash 逐行证明
- `translated_exact`：翻译后完全一致
- `translated_io_only`：输入输出一致，时间/功率不同
- `translated_item_io`：物品一致，流体不同
- `translatable_missing`：可翻译但 CC 无此配方
- `missing_material_form`：CC 有这个材料，但缺这个形态
- `missing_material`：CC 没有这个材料
- `missing_fluid`：CC 缺流体
- `missing_object`：CC 缺物品/方块/模具
- `display_only`：GT6 NEI 展示行（fake/hidden）
- `legacy_exclusion_pending`：旧分母排除，待重新决策

“目标”= 源行 − 决策排除；“进度”= 已证明 / 目标；“就绪”= 不缺身份、不待决策的目标行占比。

| GT6 map | 源行 | 目标 | 进度 | 就绪 | `source_exact` | `translated_exact` | `translated_io_only` | `translated_item_io` | `translatable_missing` | `missing_material_form` | `missing_material` | `missing_fluid` | `missing_object` | `display_only` | `legacy_exclusion_pending` |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `gt.recipe.extruder` | 325595 | 325519 | 0.9% | 69.8% | 0 | 2787 | 0 | 0 | 224439 | 90032 | 0 | 0 | 8337 | 0 | 0 |
| `gt.recipe.mixer` | 64245 | 64239 | 11.2% | 92.0% | 7198 | 30 | 0 | 4556 | 47307 | 4502 | 0 | 565 | 87 | 0 | 0 |
| `gt.recipe.bath` | 59855 | 59791 | 82.7% | 87.8% | 49417 | 0 | 0 | 2918 | 145 | 3665 | 0 | 3707 | 1 | 2 | 0 |
| `gt.recipe.shredder` | 41246 | 41245 | 71.6% | 73.2% | 29365 | 178 | 0 | 0 | 630 | 10754 | 0 | 0 | 319 | 0 | 0 |
| `gt.recipe.cutter` | 27454 | 27446 | 0.0% | 75.7% | 0 | 0 | 0 | 0 | 20774 | 4432 | 0 | 0 | 2248 | 0 | 0 |
| `gt.recipe.boxinator` | 27291 | 27291 | 0.0% | 27.8% | 0 | 0 | 0 | 0 | 7588 | 19291 | 0 | 0 | 412 | 0 | 0 |
| `gt.recipe.smelter` | 21969 | 21968 | 76.6% | 79.5% | 16812 | 9 | 0 | 0 | 640 | 4181 | 0 | 221 | 106 | 0 | 0 |
| `gt.recipe.unboxinator` | 17513 | 17505 | 0.0% | 30.6% | 0 | 0 | 0 | 0 | 5355 | 12055 | 0 | 0 | 95 | 8 | 0 |
| `gt.recipe.crusher` | 12932 | 12932 | 2.3% | 12.1% | 0 | 302 | 0 | 0 | 1257 | 11147 | 0 | 0 | 226 | 0 | 0 |
| `gt.recipe.toolhead` | 10960 | 0 | — | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 10960 | 0 |
| `gt.recipe.generifier` | 10236 | 10162 | 0.1% | 70.7% | 6 | 1 | 0 | 16 | 7159 | 2575 | 0 | 56 | 423 | 0 | 0 |
| `gt.recipe.anvil` | 9228 | 9228 | 0.0% | 0.0% | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 9228 |
| `gt.recipe.press` | 8160 | 8160 | 0.0% | 7.2% | 0 | 0 | 0 | 0 | 592 | 7247 | 0 | 0 | 321 | 0 | 0 |
| `gt.recipe.steamcracking` | 7746 | 7746 | 0.0% | 68.8% | 0 | 0 | 0 | 0 | 5330 | 2400 | 0 | 16 | 0 | 0 | 0 |
| `gt.recipe.sharpener` | 7637 | 7637 | 100.0% | 100.0% | 7637 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.melter` | 6756 | 6755 | 53.4% | 55.9% | 3601 | 9 | 0 | 0 | 168 | 2687 | 0 | 180 | 111 | 0 | 0 |
| `gt.recipe.fermenter` | 6435 | 6370 | 0.0% | 11.3% | 0 | 2 | 0 | 44 | 677 | 644 | 0 | 3586 | 1482 | 0 | 0 |
| `gt.recipe.mortar` | 6318 | 6317 | 0.0% | 74.4% | 0 | 0 | 0 | 0 | 4699 | 1136 | 0 | 0 | 483 | 0 | 0 |
| `gt.recipe.squeezer` | 5322 | 5322 | 0.3% | 0.8% | 15 | 1 | 0 | 0 | 27 | 5190 | 0 | 21 | 68 | 0 | 0 |
| `gt.recipe.sluice` | 4840 | 4840 | 0.0% | 27.9% | 0 | 0 | 0 | 0 | 1352 | 3058 | 0 | 430 | 0 | 0 | 0 |
| `gt.recipe.welder` | 4762 | 4762 | 0.0% | 80.2% | 0 | 0 | 752 | 0 | 3068 | 935 | 0 | 0 | 7 | 0 | 0 |
| `gt.recipe.canner` | 3609 | 3609 | 0.1% | 2.4% | 0 | 3 | 0 | 0 | 82 | 2343 | 0 | 72 | 1109 | 0 | 0 |
| `gt.recipe.sifter` | 2877 | 2877 | 0.0% | 7.2% | 0 | 0 | 0 | 0 | 208 | 2667 | 0 | 0 | 2 | 0 | 0 |
| `gt.recipe.lathe` | 2528 | 2528 | 0.0% | 89.4% | 0 | 0 | 0 | 0 | 2261 | 260 | 0 | 0 | 7 | 0 | 0 |
| `gt.recipe.rollingmill` | 2438 | 2438 | 0.0% | 75.0% | 0 | 0 | 297 | 0 | 1532 | 607 | 0 | 0 | 2 | 0 | 0 |
| `gt.recipe.laserengraver` | 1787 | 1787 | 0.0% | 32.3% | 0 | 0 | 0 | 0 | 577 | 0 | 0 | 0 | 1210 | 0 | 0 |
| `gt.recipe.assembler` | 1582 | 1582 | 21.7% | 21.7% | 343 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1239 | 0 | 0 |
| `gt.recipe.distillery` | 1517 | 1517 | 0.4% | 58.9% | 6 | 0 | 0 | 0 | 888 | 3 | 0 | 620 | 0 | 0 | 0 |
| `gt.recipe.compressor` | 1472 | 1472 | 87.6% | 93.9% | 1289 | 0 | 0 | 0 | 93 | 90 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.bumblelyzer` | 1440 | 0 | — | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1440 | 0 |
| `gt.recipe.loom` | 1334 | 1334 | 35.7% | 87.5% | 476 | 0 | 0 | 0 | 691 | 34 | 0 | 0 | 133 | 0 | 0 |
| `gt.recipe.centrifuge` | 1296 | 1296 | 19.4% | 41.1% | 252 | 0 | 0 | 7 | 274 | 729 | 0 | 11 | 23 | 0 | 0 |
| `gt.recipe.implosioncompressor` | 1072 | 1072 | 72.4% | 72.4% | 776 | 0 | 0 | 0 | 0 | 296 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.freezer` | 957 | 957 | 0.2% | 93.0% | 0 | 2 | 0 | 6 | 882 | 63 | 0 | 4 | 0 | 0 | 0 |
| `gt.recipe.polarizer` | 943 | 943 | 0.0% | 90.9% | 0 | 0 | 0 | 0 | 857 | 86 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.rollbender` | 940 | 940 | 0.0% | 68.5% | 0 | 0 | 307 | 0 | 337 | 296 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.massfab` | 920 | 920 | 0.0% | 1.1% | 0 | 0 | 0 | 0 | 10 | 182 | 0 | 728 | 0 | 0 | 0 |
| `gt.recipe.replicator` | 895 | 307 | 0.0% | 38.1% | 0 | 0 | 0 | 0 | 117 | 20 | 0 | 110 | 60 | 588 | 0 |
| `gt.recipe.anvil.bend.big` | 723 | 723 | 0.0% | 61.1% | 0 | 0 | 0 | 0 | 442 | 281 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.injector` | 638 | 638 | 16.1% | 26.5% | 103 | 0 | 0 | 14 | 52 | 0 | 0 | 21 | 448 | 0 | 0 |
| `gt.recipe.anvil.bend.small` | 507 | 507 | 0.0% | 53.4% | 0 | 0 | 0 | 0 | 271 | 236 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.laminator` | 498 | 498 | 97.6% | 97.6% | 486 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 12 | 0 | 0 |
| `gt.recipe.wiremill` | 455 | 455 | 0.0% | 78.5% | 0 | 0 | 31 | 0 | 326 | 98 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.cruciblealloying` | 414 | 414 | 0.0% | 0.0% | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 414 |
| `gt.recipe.autoclave` | 392 | 392 | 93.4% | 94.9% | 366 | 0 | 0 | 0 | 6 | 0 | 0 | 0 | 20 | 0 | 0 |
| `gt.recipe.pressurewasher` | 312 | 312 | 100.0% | 100.0% | 312 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.hammer` | 309 | 309 | 0.0% | 44.7% | 0 | 0 | 0 | 0 | 138 | 0 | 0 | 0 | 171 | 0 | 0 |
| `gt.recipe.clustermill` | 307 | 307 | 100.0% | 100.0% | 307 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.electrolyzer` | 290 | 290 | 61.4% | 94.8% | 178 | 0 | 0 | 18 | 79 | 15 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.byproductlist` | 289 | 0 | — | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 289 | 0 |
| `gt.recipe.drying` | 217 | 217 | 91.2% | 92.6% | 198 | 0 | 0 | 2 | 1 | 0 | 0 | 16 | 0 | 0 | 0 |
| `gt.recipe.magneticseparator` | 179 | 179 | 0.0% | 59.2% | 0 | 0 | 0 | 0 | 106 | 73 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.crystallisationcrucible` | 132 | 132 | 0.0% | 100.0% | 0 | 0 | 0 | 0 | 132 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.cokeoven` | 124 | 124 | 30.6% | 96.8% | 0 | 38 | 0 | 1 | 81 | 4 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.roaster` | 115 | 115 | 63.5% | 93.0% | 73 | 0 | 0 | 34 | 0 | 8 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.juicer` | 96 | 96 | 0.0% | 15.6% | 0 | 0 | 0 | 0 | 15 | 0 | 0 | 18 | 63 | 0 | 0 |
| `gt.recipe.bumblequeen` | 80 | 0 | — | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 80 | 0 |
| `gt.recipe.other` | 71 | 0 | — | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 71 | 0 |
| `gt.recipe.nanofab` | 64 | 64 | 14.1% | 87.5% | 7 | 2 | 0 | 0 | 47 | 8 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.cryomixer` | 57 | 56 | 0.0% | 21.4% | 0 | 0 | 0 | 0 | 12 | 0 | 0 | 45 | 0 | 0 | 0 |
| `gt.recipe.fuels.fluidbed` | 55 | 55 | 100.0% | 100.0% | 55 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.bedrockorelist` | 52 | 0 | — | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 52 | 0 |
| `gt.recipe.scannervisuals` | 50 | 0 | — | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 50 | 0 |
| `gt.recipe.fuels.burn` | 49 | 49 | 53.1% | 69.4% | 23 | 3 | 0 | 8 | 0 | 0 | 0 | 15 | 0 | 0 | 0 |
| `gt.recipe.cncmachine` | 38 | 38 | 0.0% | 100.0% | 0 | 0 | 0 | 0 | 38 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.slicer` | 33 | 33 | 100.0% | 100.0% | 33 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.burnmixer` | 29 | 29 | 0.0% | 100.0% | 0 | 0 | 0 | 0 | 29 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.rollformer` | 28 | 28 | 92.9% | 92.9% | 26 | 0 | 0 | 0 | 0 | 2 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.printer` | 22 | 1 | 0.0% | 0.0% | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 21 | 0 |
| `gt.recipe.fuels.engine` | 21 | 21 | 38.1% | 38.1% | 6 | 2 | 0 | 0 | 0 | 0 | 0 | 13 | 0 | 0 | 0 |
| `gt.recipe.fusionreactor` | 18 | 18 | 100.0% | 100.0% | 0 | 18 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.trees` | 15 | 0 | — | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 15 | 0 |
| `gt.recipe.fuels.hot` | 12 | 12 | 0.0% | 8.3% | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 11 | 0 | 0 | 0 |
| `gt.recipe.lightning` | 12 | 12 | 8.3% | 75.0% | 0 | 1 | 0 | 0 | 8 | 2 | 0 | 1 | 0 | 0 | 0 |
| `gt.recipe.fuels.gas` | 9 | 9 | 100.0% | 100.0% | 9 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.distillationtower` | 8 | 8 | 37.5% | 37.5% | 0 | 3 | 0 | 0 | 0 | 0 | 0 | 5 | 0 | 0 | 0 |
| `gt.recipe.coagulator` | 5 | 5 | 20.0% | 40.0% | 0 | 1 | 0 | 1 | 0 | 0 | 0 | 3 | 0 | 0 | 0 |
| `gt.recipe.cryodistillationtower` | 3 | 3 | 33.3% | 100.0% | 0 | 1 | 0 | 1 | 1 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.catalyticcracking` | 3 | 3 | 0.0% | 100.0% | 0 | 0 | 0 | 0 | 3 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.chisel` | 2 | 2 | 0.0% | 100.0% | 0 | 0 | 0 | 0 | 2 | 0 | 0 | 0 | 0 | 0 | 0 |
| `gt.recipe.fuels.turbine` | 1 | 1 | 0.0% | 100.0% | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 |

## 3. 化学 / 热处理子集

- 相关 maps：21，GT6 源行 172327，已追溯 79138
- 明确 `CHEMICAL_OR_THERMAL_PIPELINE` deferred：11 maps / 15811 源行
- 逐图明细见第 2 节同名行；这里不重复。

## 4. 机器 kind（96）

有 RecipeMap 的 kind 取对应配方图的深度；GT6 dump 为空的图（原版熔炉、NBT 假配方、合成台）不代表机器不用做，按 CC 主机判：有主机 → `runtime_only`，没有 → `denominator_only`。无 RecipeMap 的 kind（发电、转换、储能、传动等）按 CC 侧证据判定：capability 已 accepted → `runtime_accepted`；只有 paused/frozen 卡 → `runtime_paused`；有运行时代码但没建卡 → `runtime_code_uncarded`；只在 CC 目录里有身份 → `identity_only`；都没有 → `denominator_only`。

| 行为类 | RecipeMap / FuelMap | 能量 in→out | 历史源分母分类 | GT6 source ids | 变体 | CC 主机 / 证据 | 当前交付深度 |
| --- | --- | --- | --- | ---: | ---: | --- | --- |
| `MultiTileEntityBasicMachine` | RM.BurnMixer | RU→NONE | in_scope | 4 | 4 | — | `denominator_only` |
| `MultiTileEntityBasicMachine` | RM.CatalyticCracking | HU→NONE | in_scope | 4 | 4 | — | `denominator_only` |
| `MultiTileEntityBasicMachine` | RM.CrystallisationCrucible | HU→NONE | in_scope | 4 | 4 | — | `denominator_only` |
| `MultiTileEntityBasicMachine` | RM.SteamCracking | HU→NONE | in_scope | 4 | 4 | — | `denominator_only` |
| `MultiTileEntityBasicMachineElectric` | RM.ScannerVisuals | EU→NONE | in_scope | 5 | 5 | — | `denominator_only` |
| `MultiTileEntityBasicMachineElectric` | RM.Unboxinator | EU→NONE | in_scope | 5 | 5 | — | `denominator_only` |
| `MultiTileEntityBatteryBoxLarge` | — | NONE→EU | in_scope | 1 | 1 | — | `denominator_only` |
| `MultiTileEntityCrystalCharger` | — | NONE→LU | deferred_with_reason | 1 | 1 | — | `denominator_only` |
| `MultiTileEntityCrystalChargerLarge` | — | NONE→LU | deferred_with_reason | 1 | 1 | — | `denominator_only` |
| `MultiTileEntityLaserAbsorberElectric` | — | LU→EU | in_scope | 5 | 5 | — | `denominator_only` |
| `MultiTileEntityLaserElectric` | — | EU→LU | in_scope | 5 | 5 | — | `denominator_only` |
| `MultiTileEntityMagicFieldAbsorber` | — | NONE→CU+HU+KU+LU+QU+TU | in_scope | 1 | 1 | — | `denominator_only` |
| `MultiTileEntityMagnetElectric` | — | EU→MU | in_scope | 5 | 5 | — | `denominator_only` |
| `MultiTileEntitySolarPanelElectric` | — | NONE→EU | in_scope | 2 | 2 | — | `denominator_only` |
| `MultiTileEntityZPMDechargerEU` | — | QU→EU | in_scope | 1 | 1 | — | `denominator_only` |
| `MultiTileEntityZPMDechargerQU` | — | QU→QU | deferred_with_reason | 1 | 1 | — | `denominator_only` |
| `MultiTileEntityQuantumEnergizerLaser` | — | LU→QU | deferred_with_reason | 5 | 5 | 目录 1 个文件，无运行时 | `identity_only` |
| `MultiTileEntityEngineRotation` | — | RU→KU | in_scope | 13 | 13 | 代码 2 个文件，无 capability | `runtime_code_uncarded` |
| `MultiTileEntityBasicMachine` | RM.Furnace | HU→NONE | in_scope | 4 | 4 | cruciblecraft:oven | `runtime_only` |
| `MultiTileEntityBasicMachine` | RM.Replicator | QU→NONE | deferred_with_reason | 5 | 5 | cruciblecraft:replicator | `runtime_only` |
| `MultiTileEntityBasicMachine` | RM.ScannerMolecular | QU→NONE | deferred_with_reason | 1 | 1 | cruciblecraft:scanner | `runtime_only` |
| `MultiTileEntityBasicMachineElectric` | RM.Autocrafter | EU→NONE | in_scope | 5 | 5 | cruciblecraft:autocrafter | `runtime_only` |
| `MultiTileEntityBasicMachineElectric` | RM.Boxinator | EU→NONE | in_scope | 5 | 5 | cruciblecraft:boxinator | `runtime_only` |
| `MultiTileEntityBasicMachineElectric` | RM.Bumblelyzer | EU→NONE | in_scope | 5 | 5 | cruciblecraft:bumblelyzer | `runtime_only` |
| `MultiTileEntityBasicMachineElectric` | RM.Plantalyzer | EU→NONE | in_scope | 5 | 5 | cruciblecraft:plantalyzer | `runtime_only` |
| `MultiTileEntityBasicMachineElectric` | RM.Printer | EU→NONE | in_scope | 5 | 5 | cruciblecraft:printer | `runtime_only` |
| `MultiTileEntityCoolerElectric` | — | EU→CU | in_scope | 5 | 5 | energy/cooler（runtime_ready/paused） | `runtime_paused` |
| `MultiTileEntityTurbineSteam` | — | STEAM→RU | in_scope | 15 | 15 | energy/steam-turbine（frozen/paused） | `runtime_paused` |
| `MultiTileEntityBasicMachine` | RM.Autoclave | TU→NONE | in_scope | 1 | 1 | cruciblecraft:autoclave | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Bath | TU→NONE | in_scope | 1 | 1 | cruciblecraft:bath | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Centrifuge | RU→NONE | in_scope | 4 | 4 | cruciblecraft:centrifuge | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Coagulator | TU→NONE | in_scope | 1 | 1 | cruciblecraft:coagulator | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Compressor | KU→NONE | in_scope | 4 | 4 | cruciblecraft:compressor | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Crusher | KU→NONE | in_scope | 4 | 4 | cruciblecraft:bronze_crusher | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.CryoMixer | CU→NONE | deferred_with_reason | 5 | 5 | cruciblecraft:cryo_mixer | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Cutter | RU→NONE | in_scope | 4 | 4 | cruciblecraft:cutter | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Distillery | HU→NONE | in_scope | 4 | 4 | cruciblecraft:distillery | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Drying | HU→NONE | in_scope | 4 | 4 | cruciblecraft:drying | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Extruder | HU→NONE | in_scope | 4 | 4 | cruciblecraft:extruder | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Fermenter | HU→NONE | in_scope | 1 | 1 | — | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Freezer | CU→NONE | deferred_with_reason | 5 | 5 | cruciblecraft:freezer | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Generifier | TU→NONE | in_scope | 1 | 1 | cruciblecraft:generifier | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Laminator | HU→NONE | in_scope | 4 | 4 | cruciblecraft:laminator | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.LaserEngraver | LU→NONE | deferred_with_reason | 5 | 5 | cruciblecraft:laser_engraver | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Lathe | RU→NONE | in_scope | 4 | 4 | cruciblecraft:lathe | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Loom | RU→NONE | in_scope | 4 | 4 | cruciblecraft:electricloom, cruciblecraft:loom | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.MagneticSeparator | MU→NONE | deferred_with_reason | 5 | 5 | cruciblecraft:magnetic_separator | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Massfab | QU→NONE | deferred_with_reason | 5 | 5 | cruciblecraft:massfab | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Melter | HU→NONE | in_scope | 1 | 1 | cruciblecraft:melter | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Mixer | RU→NONE | in_scope | 4 | 4 | cruciblecraft:electric_mixer, cruciblecraft:mixer | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Nanofab | EU→NONE | in_scope | 5 | 5 | cruciblecraft:nanofab | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Polarizer | MU→NONE | deferred_with_reason | 5 | 5 | cruciblecraft:polarizer | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Press | KU→NONE | in_scope | 4 | 4 | cruciblecraft:press | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Roasting | HU→NONE | in_scope | 4 | 4 | cruciblecraft:roaster | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.RollBender | RU→NONE | in_scope | 4 | 4 | cruciblecraft:rollbender | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.RollFormer | RU→NONE | in_scope | 4 | 4 | cruciblecraft:rollformer | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.RollingMill | RU→NONE | in_scope | 4 | 4 | cruciblecraft:rollingmill | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Shredder | RU→NONE | in_scope | 4 | 4 | cruciblecraft:shredder | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Sifting | KU→NONE | in_scope | 4 | 4 | cruciblecraft:sifter | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Sluice | RU→NONE | in_scope | 4 | 4 | cruciblecraft:sluice | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Smelter | HU→NONE | in_scope | 4 | 4 | cruciblecraft:smelter | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Squeezer | KU→NONE | in_scope | 4 | 4 | cruciblecraft:squeezer | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Welder | LU→NONE | deferred_with_reason | 5 | 5 | cruciblecraft:laser_welder | `bounded_subset` |
| `MultiTileEntityBasicMachine` | RM.Wiremill | RU→NONE | in_scope | 4 | 4 | cruciblecraft:wiremill | `bounded_subset` |
| `MultiTileEntityBasicMachineElectric` | RM.Canner | EU→NONE | in_scope | 5 | 5 | cruciblecraft:canner | `bounded_subset` |
| `MultiTileEntityBasicMachineElectric` | RM.Electrolyzer | EU→NONE | in_scope | 5 | 5 | cruciblecraft:electrolyzer | `bounded_subset` |
| `MultiTileEntityBasicMachineElectric` | RM.Injector | EU→NONE | in_scope | 5 | 5 | cruciblecraft:injector | `bounded_subset` |
| `MultiTileEntityBasicMachineElectric` | RM.Lightning | EU→NONE | in_scope | 5 | 5 | cruciblecraft:lightning | `bounded_subset` |
| `MultiTileEntityBasicMachineElectric` | RM.Loom | EU→NONE | in_scope | 5 | 5 | cruciblecraft:electricloom, cruciblecraft:loom | `bounded_subset` |
| `MultiTileEntityBasicMachineElectric` | RM.Mixer | EU→NONE | in_scope | 5 | 5 | cruciblecraft:electric_mixer, cruciblecraft:mixer | `bounded_subset` |
| `MultiTileEntityBasicMachineElectric` | RM.Sifting | EU→NONE | in_scope | 5 | 5 | cruciblecraft:sifter | `bounded_subset` |
| `MultiTileEntityAxle` | — | RU→RU | in_scope | 52 | 52 | content/gt6-mte-drive-runtime（runtime_ready/accepted） | `runtime_accepted` |
| `MultiTileEntityBatteryBox` | — | NONE→EU | in_scope | 1 | 1 | content/gt6-mte-converter-remainder-runtime（runtime_ready/accepted） | `runtime_accepted` |
| `MultiTileEntityBoilerTank` | — | HU→STEAM | in_scope | 26 | 26 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityDynamoElectric` | — | RU→EU | in_scope | 5 | 5 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityEngineElectric` | — | EU→KU | in_scope | 5 | 5 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityEngineSteam` | — | STEAM→KU | in_scope | 28 | 28 | energy/converter-catalog（runtime_ready/accepted）, energy/nuclear-fission-observation-safety（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityGearBox` | — | RU→RU | in_scope | 13 | 13 | content/gt6-mte-drive-runtime（runtime_ready/accepted） | `runtime_accepted` |
| `MultiTileEntityGeneratorBrick` | FM.Furnace | NONE→HU | in_scope | 1 | 1 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityGeneratorFluidBed` | FM.FluidBed | NONE→HU | in_scope | 26 | 26 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityGeneratorGas` | FM.Burn | NONE→HU | in_scope | 22 | 22 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityGeneratorHotFluid` | FM.Hot | NONE→HU | in_scope | 8 | 8 | energy/heat-exchangers（runtime_ready/accepted） | `runtime_accepted` |
| `MultiTileEntityGeneratorLiquid` | FM.Burn | NONE→HU | in_scope | 22 | 22 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityGeneratorMetal` | FM.Furnace | NONE→HU | in_scope | 26 | 26 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityHeaterElectric` | — | EU→HU | in_scope | 5 | 5 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityLongDistanceTransformer` | — | EU→EU | in_scope | 5 | 5 | energy/transformers（runtime_ready/accepted） | `runtime_accepted` |
| `MultiTileEntityMotorElectric` | — | EU→RU | in_scope | 5 | 5 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityMotorLiquid` | FM.Engine | NONE→RU | in_scope | 8 | 8 | energy/converter-catalog（runtime_ready/accepted）, energy/small-gas-turbine（frozen/paused） | `runtime_accepted` |
| `MultiTileEntityReactorCore1x1` | — | NONE→NONE | deferred_with_reason | 2 | 2 | energy/nuclear-fission-hot-fluids（runtime_ready/accepted）, energy/nuclear-fission-observation-safety（runtime_ready/accepted）, energy/nuclear-fission-survival（runtime_ready/accepted） | `runtime_accepted` |
| `MultiTileEntityReactorCore2x2` | — | NONE→NONE | deferred_with_reason | 1 | 1 | energy/nuclear-fission-hot-fluids（runtime_ready/accepted）, energy/nuclear-fission-observation-safety（runtime_ready/accepted）, energy/nuclear-fission-survival（runtime_ready/accepted） | `runtime_accepted` |
| `MultiTileEntityTransformerElectric` | — | EU→EU | in_scope | 9 | 9 | content/puv-omega-parts（frozen/paused）, energy/transformers（runtime_ready/accepted） | `runtime_accepted` |
| `MultiTileEntityTransformerRotation` | — | RU→RU | in_scope | 13 | 13 | energy/transformers（runtime_ready/accepted） | `runtime_accepted` |
| `MultiTileEntityBasicMachine` | RM.ClusterMill | RU→NONE | in_scope | 4 | 4 | cruciblecraft:clustermill | `full_replay` |
| `MultiTileEntityBasicMachine` | RM.PressureWasher | RU→NONE | in_scope | 4 | 4 | cruciblecraft:pressurewasher | `full_replay` |
| `MultiTileEntityBasicMachine` | RM.Sharpening | RU→NONE | in_scope | 4 | 4 | cruciblecraft:sanding | `full_replay` |
| `MultiTileEntityBasicMachineElectric` | RM.Slicer | EU→NONE | in_scope | 5 | 5 | cruciblecraft:slicer | `full_replay` |

## 5. 多方块控制器（30）

按分母里的原始 GT6 控制器类名在 CC capability、运行时代码和数据目录里查证据。

| GT6 控制器 | 分母 | CC 证据 | 交付深度 |
| --- | --- | --- | --- |
| `matter_fabricator` | out_of_scope | 目录 1 个文件，无运行时 | `identity_only` |
| `tank_5x5x5_metal` | deferred_with_reason | 目录 1 个文件，无运行时 | `identity_only` |
| `large_boiler` | in_scope | 代码 5 个文件，无 capability | `runtime_code_uncarded` |
| `tank_3x3x3` | in_scope | 代码 1 个文件，无 capability | `runtime_code_uncarded` |
| `autoclave` | deferred_with_reason | machines/large-autoclave（frozen/paused，survival=partial） | `runtime_paused` |
| `electrolyzer` | deferred_with_reason | machines/large-electrolyzer（frozen/paused，survival=partial） | `runtime_paused` |
| `fermenter` | deferred_with_reason | machines/large-fermenter（frozen/paused，survival=partial） | `runtime_paused` |
| `fusion_reactor` | out_of_scope | energy/fusion-quantum（frozen/paused，survival=unset） | `runtime_paused` |
| `large_heat_exchanger` | deferred_with_reason | energy/large-heat-exchanger（frozen/paused，survival=unset） | `runtime_paused` |
| `large_turbine_steam` | deferred_with_reason | energy/steam-turbine（frozen/paused，survival=unset） | `runtime_paused` |
| `bath` | deferred_with_reason | machines/large-bathing-vat（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `bedrock_drill` | out_of_scope | machines/bedrock-drill（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `centrifuge` | in_scope | machines/large-centrifuge（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `coagulator` | deferred_with_reason | machines/large-coagulator（runtime_ready/accepted，survival=blocked） | `runtime_accepted` |
| `coke_oven` | in_scope | machines/coke-oven（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `crucible` | deferred_with_reason | machines/large-crucible（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `crusher` | deferred_with_reason | machines/large-crusher（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `cryo_distillation_tower` | deferred_with_reason | machines/distillation-tower（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `distillation_tower` | in_scope | machines/distillation-tower（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `implosion_compressor` | deferred_with_reason | machines/implosion-compressor（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `large_dynamo` | deferred_with_reason | machines/gt6-coil-hosts（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `large_turbine_gas` | deferred_with_reason | energy/large-gas-turbine（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `lightning_rod` | out_of_scope | machines/gt6-coil-hosts（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `logistics_core` | deferred_with_reason | logistics/logistics-core（runtime_ready/accepted，survival=unreviewed） | `runtime_accepted` |
| `mixer` | deferred_with_reason | machines/large-mixer（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `oven` | deferred_with_reason | machines/large-oven（runtime_ready/accepted，survival=partial）, machines/oven（runtime_ready/accepted，survival=unset） | `runtime_accepted` |
| `shredder` | deferred_with_reason | machines/large-shredder（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `sluice` | deferred_with_reason | machines/large-sluice（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `squeezer` | deferred_with_reason | machines/large-squeezer（runtime_ready/accepted，survival=partial） | `runtime_accepted` |
| `von_da_graagg` | out_of_scope | machines/gt6-coil-hosts（runtime_ready/accepted，survival=partial） | `runtime_accepted` |

## 6. 盖板（47）

当前 CC 盖板定义：54 条。“历史源分母分类”来自冻结基线，不是当前实现状态；“CC live ids”才是当前工作树证据。

| GT6 盖板 | 历史源分母分类 | CC live ids |
| --- | --- | --- |
| `controller_auto` | deferred_with_reason | cruciblecraft:controller_auto |
| `controller_auto_redstone` | deferred_with_reason | cruciblecraft:controller_auto_redstone |
| `controller_covers` | deferred_with_reason | cruciblecraft:controller_covers |
| `controller_display` | deferred_with_reason | cruciblecraft:controller_display |
| `controller_redstone` | deferred_with_reason | cruciblecraft:controller_redstone |
| `detector_running_actively` | deferred_with_reason | cruciblecraft:detector_running_actively |
| `detector_running_passively` | deferred_with_reason | cruciblecraft:detector_running_passively |
| `detector_running_possible` | deferred_with_reason | cruciblecraft:detector_running_possible |
| `detector_running_successfully` | deferred_with_reason | cruciblecraft:detector_running_successfully |
| `display_energy` | deferred_with_reason | cruciblecraft:display_energy |
| `logistics_fluid_storage` | deferred_with_reason | cruciblecraft:logistics_fluid_storage |
| `logistics_generic_dump` | deferred_with_reason | cruciblecraft:logistics_generic_dump |
| `logistics_generic_storage` | deferred_with_reason | cruciblecraft:logistics_generic_storage |
| `logistics_item_storage` | deferred_with_reason | cruciblecraft:logistics_item_storage |
| `redstone_emitter` | deferred_with_reason | cruciblecraft:redstone_emitter |
| `redstone_repeater` | deferred_with_reason | cruciblecraft:redstone_repeater |
| `redstone_torch` | deferred_with_reason | cruciblecraft:redstone_torch |
| `scale_energy` | deferred_with_reason | cruciblecraft:scale_energy |
| `scale_progress` | deferred_with_reason | cruciblecraft:scale_progress |
| `selector_button_panel` | deferred_with_reason | cruciblecraft:selector_button_panel |
| `selector_redstone` | deferred_with_reason | cruciblecraft:selector_redstone |
| `selector_tag` | deferred_with_reason | cruciblecraft:selector_tag |
| `vent` | deferred_with_reason | cruciblecraft:vent |
| `conveyor` | in_scope | cruciblecraft:conveyor |
| `filter_fluid` | in_scope | cruciblecraft:filter_fluid |
| `pressure_valve` | in_scope | cruciblecraft:pressure_valve |
| `pump` | in_scope | cruciblecraft:pump |
| `retriever_item` | in_scope | cruciblecraft:retriever_item |
| `robot_arm` | in_scope | cruciblecraft:robot_arm |
| `selector_manual` | in_scope | cruciblecraft:selector_manual |
| `shutter` | in_scope | cruciblecraft:shutter |
| `logistics_display_cpu_control` | out_of_scope | cruciblecraft:logistics_display_cpu_control |
| `logistics_display_cpu_conversion` | out_of_scope | cruciblecraft:logistics_display_cpu_conversion |
| `logistics_display_cpu_logic` | out_of_scope | cruciblecraft:logistics_display_cpu_logic |
| `logistics_display_cpu_storage` | out_of_scope | cruciblecraft:logistics_display_cpu_storage |
| `controller_auto_timer` | deferred_with_reason | — |
| `logistics_fluid_transfer` | deferred_with_reason | — |
| `logistics_generic_transfer` | deferred_with_reason | — |
| `logistics_item_transfer` | deferred_with_reason | — |
| `redstone_conductor` | deferred_with_reason | — |
| `filter_item` | in_scope | — |
| `asphalt` | out_of_scope | — |
| `crafting` | out_of_scope | — |
| `drain` | out_of_scope | — |
| `texture_canvas` | out_of_scope | — |
| `texture_multi` | out_of_scope | — |
| `texture_simple` | out_of_scope | — |

## 7. 能量身份（20）

“CC `EnergyType`”读自 live `EnergyType.java`；冻结分母里的本地类型只作对照。

| GT6 能量 | 名称 | 分母 | CC `EnergyType` | 冻结分母记录 |
| --- | --- | --- | --- | --- |
| `CRYO` | Cryo Energy | deferred_with_reason | CU | — |
| `LIGHT` | Light Energy | deferred_with_reason | LU | — |
| `MAGNETIC` | Magnetic Energy | deferred_with_reason | MU | — |
| `NEUTRON` | Neutron Energy | deferred_with_reason | — | — |
| `QUANTUM` | Quantum Energy | deferred_with_reason | QUANTUM | — |
| `AIR` | Air Pressure | in_scope | AIR | AIR |
| `ELECTRICITY` | Electric Energy | in_scope | ELECTRIC | ELECTRIC |
| `HEAT` | Heat Energy | in_scope | HEAT | HEAT |
| `KINETIC_PUSH` | Kinetic Energy | in_scope | KINETIC_PUSH | KINETIC_PUSH |
| `KINETIC_ROTATION` | Rotation Energy | in_scope | KINETIC_ROTATION | KINETIC_ROTATION |
| `STEAM` | Steam | in_scope | — | — |
| `TIME` | Time | in_scope | TIME | — |
| `MINECRAFT_JOULES` | Minecraft Joules | out_of_scope | — | — |
| `REDSTONE_FLUX` | Redstone Flux | out_of_scope | — | — |
| `VIS_AER` | Aer Vis | out_of_scope | — | — |
| `VIS_AQUA` | Aqua Vis | out_of_scope | — | — |
| `VIS_IGNIS` | Ignis Vis | out_of_scope | — | — |
| `VIS_ORDO` | Ordo Vis | out_of_scope | — | — |
| `VIS_PERDITIO` | Perditio Vis | out_of_scope | — | — |
| `VIS_TERRA` | Terra Vis | out_of_scope | — | — |

## 8. 物品/流体生成域（25）

本节整列来自冻结分母 `itemgenerator_domains.json`，不会随工作树变化；具体形态是否开放看第 9 节 live 前缀和第 11 节形态需求普查。

| GT6 域 | 分母 | 冻结分母记 CC 域 | 源材料数 |
| --- | --- | --- | ---: |
| `ITEMGENERATOR.CONTAINERS` | deferred_with_reason | False | 95 |
| `ITEMGENERATOR.LENSES` | deferred_with_reason | False | 121 |
| `ITEMGENERATOR.RAILS` | deferred_with_reason | False | 28 |
| `ITEMGENERATOR.CONTAINERS_FLUID` | in_scope | True | 61 |
| `ITEMGENERATOR.CONTAINERS_GAS` | in_scope | True | 48 |
| `ITEMGENERATOR.DENSEPLATES` | in_scope | True | 436 |
| `ITEMGENERATOR.DUSTS` | in_scope | True | 1096 |
| `ITEMGENERATOR.FOILS` | in_scope | True | 566 |
| `ITEMGENERATOR.GEMS` | in_scope | True | 217 |
| `ITEMGENERATOR.INGOTS` | in_scope | True | 483 |
| `ITEMGENERATOR.INGOTS_HOT` | in_scope | True | 433 |
| `ITEMGENERATOR.MOLTEN` | in_scope | True | 184 |
| `ITEMGENERATOR.MULTIINGOTS` | in_scope | True | 435 |
| `ITEMGENERATOR.MULTIPLATES` | in_scope | True | 434 |
| `ITEMGENERATOR.ORES` | in_scope | True | 618 |
| `ITEMGENERATOR.PARTS` | in_scope | True | 572 |
| `ITEMGENERATOR.PLATES` | in_scope | True | 862 |
| `ITEMGENERATOR.STICKS` | in_scope | True | 846 |
| `ITEMGENERATOR.WIRES` | in_scope | True | 14 |
| `ITEMGENERATOR.ARMORS` | out_of_scope | False | 613 |
| `ITEMGENERATOR.EMPTY` | out_of_scope | False | 1 |
| `ITEMGENERATOR.GASES` | out_of_scope | False | 32 |
| `ITEMGENERATOR.LIQUID` | out_of_scope | False | 32 |
| `ITEMGENERATOR.PLANTS` | out_of_scope | False | 1179 |
| `ITEMGENERATOR.PROJECTILES` | out_of_scope | False | 854 |

## 9. 材料前缀（452）

“CC 已映射”由 `tools/gt6_resolve.py` 的 `resolve_prefix` 对 live 前缀 JSON 现算。前缀分母只说明形态类型是否在范围内；具体 `(材料, 前缀)` 是否开放看第 11 节形态需求普查。

- **deferred_with_reason（CC 已映射）**（92）：`arrowGtPlastic`、`arrowGtWood`、`billet`、`block`、`blockDust`、`blockPlate`、`blockRaw`、`block_`、`capcellcon`、`casingMachine`、`casingMachineDense`、`casingMachineDouble`、`casingMachineQuadruple`、`casingSmall`、`chain`、`chunk`、`chunkGt`、`crushedPurifiedTiny`、`dustDiv72`、`gemChipped`、`gemExquisite`、`gemFlawed`、`gemFlawless`、`gemLegendary`、`ingotQuadruple`、`ingotQuintuple`、`lens`、`minecartWheels`、`pipeNonuple`、`pipeQuadruple`、`pipeRestrictiveHuge`、`pipeRestrictiveLarge`、`pipeRestrictiveMedium`、`plantGtBerry`、`plantGtBlossom`、`plantGtFiber`、`plantGtTwig`、`plantGtWart`、`plateCurved`、`plateGemTiny`、`plateTiny`、`railGt`、`rock`、`round`、`scrap`、`scrapGt`、`toolHeadArrow`、`toolHeadAxe`、`toolHeadAxeDouble`、`toolHeadBuilderwand`、`toolHeadBuzzSaw`、`toolHeadChainsaw`、`toolHeadChisel`、`toolHeadConstructionPickaxe`、`toolHeadDrill`、`toolHeadFile`、`toolHeadHammer`、`toolHeadHoe`、`toolHeadPickaxe`、`toolHeadPickaxeGem`、`toolHeadPlow`、`toolHeadRawArrow`、`toolHeadRawAxe`、`toolHeadRawAxeDouble`、`toolHeadRawChisel`、`toolHeadRawHoe`、`toolHeadRawPickaxe`、`toolHeadRawPlow`、`toolHeadRawSaw`、`toolHeadRawSense`、`toolHeadRawShovel`、`toolHeadRawSpade`、`toolHeadRawSword`、`toolHeadRawUniversalSpade`、`toolHeadSaw`、`toolHeadScrewdriver`、`toolHeadSense`、`toolHeadShovel`、`toolHeadSpade`、`toolHeadSword`、`toolHeadUniversalSpade`、`toolHeadWrench`、`wireGt03`、`wireGt05`、`wireGt06`、`wireGt07`、`wireGt09`、`wireGt10`、`wireGt11`、`wireGt13`、`wireGt14`、`wireGt15`
- **deferred_with_reason（CC 未映射）**（179）：`armor`、`armorBoots`、`armorChestplate`、`armorHelmet`、`armorLeggings`、`arrow`、`battery`、`batterySingleuse`、`beam`、`blockBamboo`、`blockGlass`、`blockPlateGem`、`blockSolid`、`blockWool`、`book`、`bottle`、`bouleGt`、`bucket`、`bulletGtLarge`、`bulletGtMedium`、`bulletGtSmall`、`capsule`、`cell`、`chemtube`、`circuit`、`cleanGravel`、`clump`、`cluster`、`cobblestone`、`compressed`、`compressedCobblestone`、`compressedDirt`、`compressedGravel`、`compressedSand`、`compressedStone`、`computer`、`craft`、`crafting`、`craftingTool`、`crateGt64Dust`、`crateGt64Gem`、`crateGt64Ingot`、`crateGt64Plate`、`crateGt64PlateGem`、`crateGt64Raw`、`crateGtDust`、`crateGtGem`、`crateGtIngot`、`crateGtPlate`、`crateGtPlateGem`、`crateGtRaw`、`crystal`、`crystalPure`、`crystalline`、`dirtyGravel`、`dustImpure`、`dustRefined`、`dye`、`dyeCeramic`、`dyeMixable`、`fence`、`frameGt`、`gemOre`、`gemPolished`、`gemRaw`、`gemUncut`、`glass`、`item`、`item_`、`log`、`oreAndesite`、`oreBasalt`、`oreBedrock`、`oreBetweenstone`、`oreBlackgranite`、`oreBlackstone`、`oreBlueschist`、`oreCallisto`、`oreCeres`、`oreDarkprismarine`、`oreDeadrock`、`oreDeepslate`、`oreDeimos`、`oreDense`、`oreDiorite`、`oreEnd`、`oreEndstone`、`oreEris`、`oreEuropa`、`oreGanymede`、`oreGneiss`、`oreGravel`、`oreGrayschist`、`oreGreenschist`、`oreHolystone`、`oreIapetus`、`oreIo`、`oreJupiter`、`oreKepler22b`、`oreKimberlite`、`oreKomatiite`、`oreLightprismarine`、`oreLimestone`、`oreLivingrock`、`oreMarble`、`oreMars`、`oreMercury`、`oreMoon`、`oreMud`、`oreNeptune`、`oreNether`、`oreNetherrack`、`oreNormal`、`oreOberon`、`orePhobos`、`orePinkschist`、`orePitstone`、`orePluto`、`orePoor`、`oreQuartzite`、`oreRedSand`、`oreRedgranite`、`oreRhea`、`oreRich`、`oreSand`、`oreSandstone`、`oreSaturn`、`oreShale`、`oreSiltstone`、`oreSlate`、`oreSmall`、`oreSpace`、`oreStrangesand`、`oreTitan`、`oreTitania`、`oreTriton`、`oreUmberstone`、`oreUranus`、`oreVanillagranite`、`oreVanillastone`、`oreVenus`、`oreberry`、`orebush`、`paneGlass`、`paper`、`pebbles`、`pipe`、`pipeRestrictiveSmall`、`pipeRestrictiveTiny`、`plank`、`plateSteamcraft`、`rawOreChunk`、`record`、`reduced`、`rubble`、`scraps`、`sheetGt`、`slab`、`stainedClay`、`stair`、`stone`、`stoneBricks`、`stoneChiseled`、`stoneCobble`、`stoneCracked`、`stoneMossy`、`stoneMossyBricks`、`stonePolished`、`stoneSmooth`、`tool`、`toolAxe`、`toolHoe`、`toolPickaxe`、`toolShears`、`toolShovel`、`toolSword`、`tree`、`treeLeaves`、`treeSapling`
- **in_scope（CC 已映射）**（55）：`blockGem`、`blockIngot`、`bolt`、`cableGt01`、`cableGt02`、`cableGt04`、`cableGt08`、`cableGt12`、`crushed`、`crushedCentrifuged`、`crushedCentrifugedTiny`、`crushedPurified`、`crushedTiny`、`dust`、`dustPure`、`dustSmall`、`dustTiny`、`foil`、`gearGt`、`gearGtSmall`、`gem`、`ingot`、`ingotDouble`、`ingotHot`、`ingotTriple`、`nugget`、`ore`、`oreRaw`、`pipeHuge`、`pipeLarge`、`pipeMedium`、`pipeSmall`、`pipeTiny`、`plate`、`plateDense`、`plateDouble`、`plateGem`、`plateQuadruple`、`plateQuintuple`、`plateTriple`、`ring`、`rockGt`、`rotor`、`screw`、`spring`、`springSmall`、`stick`、`stickLong`、`wireFine`、`wireGt01`、`wireGt02`、`wireGt04`、`wireGt08`、`wireGt12`、`wireGt16`
- **out_of_scope（CC 已映射）**（5）：`boule`、`cable`、`gear`、`rod`、`wire`
- **out_of_scope（CC 未映射）**（121）：`alloy`、`bamboo`、`bar`、`bars`、`bauble`、`beach`、`beans`、`bee`、`berrybush`、`bit`、`blade`、`bowl`、`brick`、`bud`、`cactus`、`chest`、`chipset`、`cloth`、`coin`、`component`、`cones`、`consumable`、`cooking`、`coral`、`crop`、`desert`、`dinosaur`、`dirt`、`door`、`drop`、`element`、`elven`、`epiphyte`、`essence`、`fabric`、`fern`、`fertilizer`、`floating`、`flower`、`food`、`forest`、`frame`、`fuel`、`fungus`、`ganys`、`gate`、`glowstone`、`grafter`、`grass`、`gravel`、`ground`、`handle`、`hanging`、`head`、`immersed`、`jungle`、`junk`、`ladder`、`lamp`、`leaf`、`leafy`、`liquid`、`list`、`lumar`、`lump`、`mana`、`material`、`mffs`、`molecule`、`motor`、`mountain`、`mushroom`、`mystic`、`obsidian`、`ocean`、`orb`、`panel`、`part`、`pearl`、`pellet`、`petal`、`plains`、`plant`、`plasma`、`plating`、`pole`、`powder`、`projred`、`quartz`、`raw`、`reactor`、`reed`、`river`、`rune`、`sand`、`savanna`、`scoop`、`seed`、`shard`、`shears`、`sheet`、`sheetDouble`、`shrub`、`skull`、`soulsand`、`stainedGlass`、`storage`、`tiny`、`tome`、`torch`、`trapdoor`、`travelgear`、`tube`、`turbine`、`vine`、`wafer`、`wall`、`water`、`wax`、`wetlands`、`wood`

## 10. MTE 身份处置（按家族）

“R0 账本”是冻结快照；“live”在账本上叠加 `mte_inplace_catalog.json`：目录里有 `MteInPlaceKind` 宿主的身份记为 `inplace_runtime`。`inplace_runtime` 只说明有运行时宿主，不说明数值与获得格已核对。

| 家族 | R0 账本 | live |
| --- | --- | --- |
| `connector` | `identity_only` 617，`realized_natively` 46 | `identity_only` 617，`realized_natively` 46 |
| `crucible_foundry` | `realized_natively` 85 | `realized_natively` 85 |
| `decorative` | `identity_only` 24 | `inplace_runtime` 24 |
| `drive` | `identity_only` 63 | `inplace_runtime` 63 |
| `energy_converter` | `identity_only` 79 | `identity_only` 71，`inplace_runtime` 8 |
| `extender` | `identity_only` 2 | `inplace_runtime` 2 |
| `fluid_attachment` | `attachment_candidate` 33 | `inplace_runtime` 33 |
| `furniture_storage` | `identity_only` 563 | `identity_only` 17，`inplace_runtime` 546 |
| `hopper` | `realized_natively` 101 | `realized_natively` 101 |
| `misc_tool` | `identity_only` 39 | `inplace_runtime` 39 |
| `multiblock` | `identity_only` 74 | `identity_only` 1，`inplace_runtime` 73 |
| `processing_machine` | `identity_only` 36，`realized_natively` 50 | `identity_only` 36，`realized_natively` 50 |
| `reactor` | `realized_natively` 1 | `realized_natively` 1 |
| `redstone_wire` | `identity_only` 3 | `identity_only` 3 |
| `untyped` | `identity_only` 1 | `identity_only` 1 |

## 11. 材料形态需求普查

- 来源：`tools/waves/prep/material-form-demand-census/census.json`
- `already_gated_live`：4
- `demand_pairs`：68
- `gated_unresolved`：59
- `not_form`：68
- `openable`：5
- `skipped`：0
- `ungated_generated_flag_pairs`：5768

## 12. Capability（112）

| capability | maturity | workflow | survival_access |
| --- | --- | --- | --- |
| `content/electric-wire-cable-mte-fold` | runtime_ready | accepted | — |
| `content/gt6-connector-alias-repair` | runtime_ready | accepted | — |
| `content/gt6-connector-art` | runtime_ready | accepted | — |
| `content/gt6-crucible-mold-behavior-correction` | runtime_ready | accepted | — |
| `content/gt6-crucible-mold-interaction` | runtime_ready | accepted | unreviewed |
| `content/gt6-electric-tools` | frozen | paused | partial |
| `content/gt6-eu-cable-acquisition` | runtime_ready | accepted | — |
| `content/gt6-eu-missing-wire-gauges-runtime` | runtime_ready | accepted | — |
| `content/gt6-eu-wire-cable-runtime` | runtime_ready | accepted | — |
| `content/gt6-fluid-combo-pipe-runtime` | runtime_ready | accepted | — |
| `content/gt6-fluid-dangerous-media-runtime` | runtime_ready | accepted | — |
| `content/gt6-fluid-pipe-acquisition` | runtime_ready | accepted | — |
| `content/gt6-fluid-pipe-runtime` | runtime_ready | accepted | — |
| `content/gt6-foundry-art` | runtime_ready | accepted | unreviewed |
| `content/gt6-insulated-redstone-runtime` | runtime_ready | accepted | — |
| `content/gt6-item-pipe-acquisition` | runtime_ready | accepted | — |
| `content/gt6-item-pipe-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-converter-host-fold` | runtime_ready | accepted | — |
| `content/gt6-mte-converter-remainder-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-crucible-foundry-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-decorative-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-drive-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-extender-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-fluid-attachments-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-furniture-barrel-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-furniture-chest-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-furniture-safe-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-furniture-scaffold-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-furniture-storage-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-furniture-table-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-hopper-host-fold` | runtime_ready | accepted | — |
| `content/gt6-mte-inplace-acquisition` | runtime_ready | accepted | — |
| `content/gt6-mte-misc-tool-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-multiblock-runtime` | runtime_ready | accepted | — |
| `content/gt6-mte-processing-host-fold` | runtime_ready | accepted | — |
| `content/gt6-mte-reactor-rod-host-fold` | runtime_ready | accepted | — |
| `content/gt6-paper-tiny-plate` | runtime_ready | accepted | — |
| `content/gt6-redstone-wire-acquisition` | runtime_ready | accepted | — |
| `content/gt6-redstone-wire-correction` | runtime_ready | accepted | — |
| `content/gt6-restrictive-item-pipe-runtime` | runtime_ready | accepted | — |
| `content/gt6-storage-art` | runtime_ready | accepted | unreviewed |
| `content/mte-redstone-wire` | runtime_ready | accepted | — |
| `content/puv-omega-parts` | frozen | paused | — |
| `content/sensors` | runtime_ready | accepted | — |
| `content/technological-parts-foundation` | runtime_ready | accepted | — |
| `energy/batteries` | runtime_ready | accepted | unreviewed |
| `energy/converter-catalog` | runtime_ready | accepted | unreviewed |
| `energy/cooler` | runtime_ready | paused | unreviewed |
| `energy/flux-converters` | runtime_ready | paused | unreviewed |
| `energy/fusion-quantum` | frozen | paused | — |
| `energy/heat-exchangers` | runtime_ready | accepted | unreviewed |
| `energy/large-gas-turbine` | runtime_ready | accepted | partial |
| `energy/large-heat-exchanger` | frozen | paused | — |
| `energy/nuclear-fission-hot-fluids` | runtime_ready | accepted | unreviewed |
| `energy/nuclear-fission-observation-safety` | runtime_ready | accepted | unreviewed |
| `energy/nuclear-fission-survival` | runtime_ready | accepted | unreviewed |
| `energy/quantum-massfab` | frozen | paused | — |
| `energy/small-gas-turbine` | frozen | paused | unreviewed |
| `energy/steam-turbine` | frozen | paused | — |
| `energy/transformers` | runtime_ready | accepted | unreviewed |
| `localization/language-key-display-name-normalization` | runtime_ready | accepted | — |
| `logistics/cover-net-r0` | frozen | accepted | — |
| `logistics/cover-remainder` | runtime_ready | accepted | — |
| `logistics/display-cpu` | runtime_ready | accepted | unreviewed |
| `logistics/fluid-network/basic-transfer` | runtime_ready | accepted | unreviewed |
| `logistics/generic-network/core` | runtime_ready | accepted | unreviewed |
| `logistics/item-network-core` | runtime_ready | accepted | unreviewed |
| `logistics/logistics-core` | runtime_ready | accepted | unreviewed |
| `machines/bath` | runtime_ready | accepted | partial |
| `machines/bedrock-drill` | runtime_ready | accepted | partial |
| `machines/cluster-mill` | runtime_ready | accepted | unreviewed |
| `machines/coke-oven` | runtime_ready | accepted | partial |
| `machines/distillation-tower` | runtime_ready | accepted | partial |
| `machines/gt6-coil-hosts` | runtime_ready | accepted | partial |
| `machines/hammer-squeezer-laser` | runtime_ready | accepted | unreviewed |
| `machines/implosion-compressor` | runtime_ready | accepted | partial |
| `machines/injector` | runtime_ready | accepted | — |
| `machines/laminator` | runtime_ready | accepted | — |
| `machines/large-autoclave` | frozen | paused | partial |
| `machines/large-bathing-vat` | runtime_ready | accepted | partial |
| `machines/large-centrifuge` | runtime_ready | accepted | partial |
| `machines/large-coagulator` | runtime_ready | accepted | blocked |
| `machines/large-crucible` | runtime_ready | accepted | partial |
| `machines/large-crusher` | runtime_ready | accepted | partial |
| `machines/large-electrolyzer` | frozen | paused | partial |
| `machines/large-fermenter` | frozen | paused | partial |
| `machines/large-mixer` | runtime_ready | accepted | partial |
| `machines/large-oven` | runtime_ready | accepted | partial |
| `machines/large-processing-parts` | runtime_ready | accepted | blocked |
| `machines/large-shredder` | runtime_ready | accepted | partial |
| `machines/large-sluice` | runtime_ready | accepted | partial |
| `machines/large-squeezer` | runtime_ready | accepted | partial |
| `machines/loom` | runtime_ready | accepted | — |
| `machines/melter` | runtime_ready | accepted | — |
| `machines/nanofab` | runtime_ready | accepted | — |
| `machines/oven` | runtime_ready | accepted | — |
| `machines/pressure-washer` | runtime_ready | accepted | — |
| `machines/puv-omega-matrix` | frozen | paused | — |
| `machines/roll-former` | runtime_ready | accepted | unreviewed |
| `machines/sanding` | runtime_ready | accepted | — |
| `machines/slicer` | runtime_ready | accepted | — |
| `registry/catalog-modern-ids` | runtime_ready | accepted | — |
| `registry/hybrid-material-identity` | runtime_ready | accepted | not_applicable |
| `registry/prefix-material-component` | runtime_ready | accepted | not_applicable |
| `registry/tool-head-prefix-reclaim` | runtime_ready | accepted | — |
| `registry/tool-head-remainder` | frozen | accepted | — |
| `worldgen/gt-crops` | runtime_ready | accepted | — |
| `worldgen/gt-dungeon` | frozen | paused | — |
| `worldgen/gt-small-ores` | runtime_ready | paused | partial |
| `worldgen/gt-stone-layer-rocks` | runtime_ready | accepted | partial |
| `worldgen/gt-surface-rocks` | runtime_ready | accepted | partial |
| `worldgen/gt-trees` | runtime_ready | accepted | — |

## 13. 未关闭 blocker（31）

| blocker | 状态 | 排期桶 | 规模 | 根因 | 标题 |
| --- | --- | --- | --- | --- | --- |
| `architecture/building-block-identity` | open | audit_first | n/a | unclaimed_domain | 建筑方块 identity/behavior 无 owner |
| `energy/cooler` | open | audit_first | n/a | missing_runtime | 电力/通量冷却器 |
| `energy/reactor-backpack-radioactivity` | open | audit_first | n/a | missing_form | 无 CC 材料放射性等级表 |
| `energy/reactor-world-explode` | open | audit_first | 1 behaviors | missing_runtime | 反应堆世界爆炸仍是注释掉的 TODO |
| `identity/eu-blocked-gauges` | partial | audit_first | n/a | unmapped_identity | EU 导线缺线规（部分已落地） |
| `tools/world-behaviors` | open | audit_first | n/a | missing_runtime | 手持工具世界行为与缺形态仍缺 |
| `worldgen/bees` | open | audit_first | n/a | missing_runtime | Bees 仍缺 runtime |
| `worldgen/center` | open | audit_first | n/a | missing_worldgen | Center 维度仍是 prep |
| `worldgen/dungeon-room-contents` | open | audit_first | n/a | missing_worldgen | 地牢房间内容与 GT 石材仍 blocked |
| `worldgen/planet-rocks` | open | audit_first | 3 families | missing_worldgen | 行星岩仍是 prep |
| `architecture/combinatorial-leftover` | open | not_work | 4 families | unclaimed_domain | 组合导入 leftover 4 条仍未开工 |
| `energy/reactor-temperature-kelvin` | open | not_work | n/a | invariant | 反应堆温度不得用 HU 伪造 Kelvin |
| `peripheral/sensors-computercraft` | open | not_work | 1 integrations | missing_mod_bridge | Sensors ComputerCraft 外设 |
| `recipe/injector-overflow` | open | scale_not_todo | 535 rows | unmapped_operand | 注射机 overflow 仍 blocked |
| `recipe/loom-overflow` | open | scale_not_todo | 857 rows | unmapped_operand | 织机 overflow 仍 blocked |
| `recipe/melter-overflow` | partial | scale_not_todo | 2796 rows | unmapped_operand | 熔融机 overflow（规模未逐行核实） |
| `recipe/nanofab-overflow` | open | scale_not_todo | 57 rows | unmapped_operand | 纳米加工机 overflow 仍 blocked |
| `worldgen/food` | open | scale_not_todo | n/a | missing_runtime | Food 榨汁/发酵仍缺 runtime |
| `fluid/ic2-coolant` | open | schedulable | 1 fluids | missing_fluid | IC2 工业冷却液不是 CC 流体 |
| `fluid/sap-maple` | open | schedulable | 1 fluids | missing_fluid | FL.Sap_Maple 缺失 |
| `fluid/thorium-salt` | open | schedulable | 1 fluids | missing_fluid | 钍盐到 LiCl 无 CC 身份 |
| `identity/converter-turbines-battery-boxes` | open | schedulable | 8 items | unmapped_identity | 转换机折回 6 轮机 + 2 电池箱 |
| `identity/processing-ungated-families` | open | schedulable | 3 families | unmapped_identity | 加工机折回未开门家族 |
| `material-form/copper-family-curved-plate` | open | schedulable | n/a | missing_form | 流体/物品管五档工作台缺 live curved_plate/double_plate |
| `obtain/injector-mv-hv-iv-hosts` | open | schedulable | 3 hosts | missing_form | 注射机 MV/HV/IV 主机获得格仍 blocked |
| `obtain/redstone-wiregt01` | open | schedulable | 3 items | missing_obtain | 红石 wireGt01 获得格仍 blocked |
| `obtain/wooden-bathing-pot-glue` | open | schedulable | 2 hosts | unmapped_operand | 木浸洗盆胶水获得格仍 blocked |
| `recipe/laminator-overflow` | open | schedulable | 12 rows | unmapped_operand | 层压机 unmapped MTE overflow |
| `recipe/oven-cooking-oil-xp` | open | schedulable | 2 fluids | missing_fluid | 熔炉烹饪油与 XP 流体 |
| `recipe/printer-dye-fluids` | open | schedulable | 22 rows | missing_fluid | 印刷机 22 行染料流体（prep） |
| `recipe/roll-former-rail-gt` | open | schedulable | 2 rows | unmapped_operand | 辊压成型机 rail_gt 两行 |

## 14. 旧分母排除项（全量目标下需逐项重新决策）

这些是旧冻结分母当年标的 `out_of_scope`（“third-stage excluded axis”）。本页以完整 GT6 为目标，它们不能默认算作“不用做”，需要逐项决定：真正移植、明确作为设计排除，或确认 GT6 本身未使用。

- 配方图：`gt.recipe.anvil`（9228 行）、`gt.recipe.cruciblealloying`（414 行）
- 多方块控制器：`bedrock_drill`（CC：`runtime_accepted`）、`fusion_reactor`（CC：`runtime_paused`）、`lightning_rod`（CC：`runtime_accepted`）、`matter_fabricator`（CC：`identity_only`）、`von_da_graagg`（CC：`runtime_accepted`）
- 盖板：`asphalt`、`crafting`、`drain`、`logistics_display_cpu_control`、`logistics_display_cpu_conversion`、`logistics_display_cpu_logic`、`logistics_display_cpu_storage`、`texture_canvas`、`texture_multi`、`texture_simple`
- 能量身份：`REDSTONE_FLUX`、`MINECRAFT_JOULES`、`VIS_ORDO`、`VIS_AER`、`VIS_AQUA`、`VIS_TERRA`、`VIS_IGNIS`、`VIS_PERDITIO`
- 物品/流体生成域：`ITEMGENERATOR.ARMORS`、`ITEMGENERATOR.EMPTY`、`ITEMGENERATOR.GASES`、`ITEMGENERATOR.LIQUID`、`ITEMGENERATOR.PLANTS`、`ITEMGENERATOR.PROJECTILES`
- 材料前缀：126 个，其中 126 个是 GT6 源里声明但显式未使用（`explicit_unused`），其余 0 个需要决策；名单见第 9 节 `out_of_scope` 组。

## 15. 读法

- `full_replay`：已追溯的不同 GT6 源行数等于源行数且无 overflow；仍需 wave/runtime 验证。
- `empty_source`：GT6 固定 dump 里这张图是空的（0 行）。配方不在 RecipeMap 里（原版熔炉表、坩埚物理、NBT 假配方），不等于没有东西要做；对应机器看第 4 节。
- `bounded_subset`：有已追溯源行、reference 追溯、未追溯 CC 行或材料规则中的任意一种，不代表全图完成。
- `runtime_only`：有 RecipeMap/主机路由，但当前没有任何运行时配方行或规则。
- `identity_only`：只在 CC 目录里有身份或部件登记，没有运行时。
- `runtime_code_uncarded`：CC 有运行时代码或声明式多方块结构，但没有任何 capability 卡，状态未验收。
- `runtime_paused`：有 capability，但是 frozen/paused，未 accepted。
- `runtime_accepted`：无 RecipeMap 的机制（发电/转换/传动等）已有 accepted capability；数值与全部变体仍需逐卡核对。
- `denominator_only`：只有 GT6 分母，CC 侧没有找到任何实现证据。
- `legacy_exclusion_pending`：历史分母曾排除；当前全量目标尚未重新决策，见第 14 节。
- `survival_access`、获得格和项目试玩是独立轴，不由上面任何一列推出。

## 16. 各轴来源与新鲜度

| 轴 | 来源 | 类型 |
| --- | --- | --- |
| 配方图分母、机器 kind、多方块、盖板、能量、生成域、前缀的“分母/历史分类” | `tools/machine_tree_denominators/*.json` | 冻结分母（GT6 revision 固定，不随工作树变） |
| 已追溯源行 / CC 行 / 材料规则 | 运行时资源根里的配方 JSON + `source_attribution.json` | live 扫描；归属钉需本地 `gt6_dump` 刷新 |
| 材料规则展开数 | `tools/component_rule_manifest.json` | 上游产物（`build_component_rules.py`） |
| overflow | `tools/waves/**/overflow.json` | 上游产物（各 wave builder） |
| 机器 kind / 多方块证据 | capability、`src/main/java`、`machine_delivery.json`、多方块结构 | live 扫描 |
| 盖板 live id | `*cover_definitions.json` | live 扫描 |
| 能量 `EnergyType` | `EnergyType.java` | live 扫描 |
| 前缀映射 | `tools/gt6_resolve.py` → `material_prefixes/` | live 扫描 |
| MTE 身份 | R0 账本 + `mte_inplace_catalog.json` | 冻结账本 + live 叠加 |
| 形态需求 | `tools/waves/prep/material-form-demand-census/census.json` | 上游产物（census builder） |
| Capability / Blocker / 试玩 | `tools/capabilities/**`、`tools/blockers/catalog.json`、`tools/playtest/current_cycle.json` | live 扫描 |
| 逐行分类 / 进度 / 行动清单 | `semantic_coverage.json`、`exclusions.json` | live 扫描 + 翻译链（`--write` 需本地 dump） |

## 17. 缺口行动清单（按杠杆排序，自动生成）

本节只列事实和提示，不是 unique-active 队列；开工仍按能力交付流程开卡。每条不可翻译的源行只记它**第一个**缺的身份，补上后可能还卡在下一个，所以“受影响源行”是解锁数的上界。已被 `exclusions.json` 排除的行不计入。

### 17.1 缺身份（前 40 项）

“材料形态”是 GT6 配方实际用到、CC 材料已存在但没开的 (材料, 形态)，按仓库规则应进材料形态需求普查，再由开形态卡打开，不在配方卡上顺手开。

| 类型 | 缺什么 | 受影响源行 | 涉及材料 | 主要机器 | 来自别的 mod |
| --- | --- | ---: | ---: | --- | --- |
| 材料形态 | `blockDust` | 19408 | 951 | extruder 8510, mixer 4492, boxinator 3756 |  |
| 材料形态 | `plateTiny` | 12659 | 265 | extruder 10808, cutter 1048, shredder 264 |  |
| 材料形态 | `plateCurved` | 11293 | 258 | extruder 10376, shredder 258, rollbender 148 |  |
| 材料形态 | `casingSmall` | 8775 | 263 | extruder 8317, shredder 263, generifier 98 |  |
| 材料形态 | `toolHeadHammer` | 7018 | 252 | extruder 6761, shredder 252, smelter 4 |  |
| 材料形态 | `scrapGt` | 5787 | 581 | extruder 4511, shredder 581, mortar 329 |  |
| 材料形态 | `machine.dense` | 5553 | 208 | extruder 4703, welder 410, shredder 208 |  |
| 材料形态 | `bulletGtLarge` | 4672 | 854 | press 1225, bath 1148, unboxinator 724 |  |
| 材料形态 | `bulletGtMedium` | 4672 | 854 | press 1225, bath 1148, unboxinator 724 |  |
| 材料形态 | `bulletGtSmall` | 4672 | 854 | press 1225, bath 1148, unboxinator 724 |  |
| 材料形态 | `crushedPurifiedTiny` | 4394 | 268 | sluice 3043, centrifuge 268, shredder 268 |  |
| 材料形态 | `chain` | 4358 | 263 | extruder 3990, shredder 263, generifier 98 |  |
| 材料形态 | `toolHeadFile` | 4044 | 159 | extruder 3883, shredder 159, smelter 2 |  |
| 材料形态 | `crushedCentrifugedTiny` | 3943 | 300 | steamcracking 2400, centrifuge 443, shredder 300 |  |
| 材料形态 | `ingotQuadruple` | 3800 | 171 | extruder 2639, cutter 676, shredder 171 |  |
| 材料形态 | `ingotQuintuple` | 3800 | 171 | extruder 2639, cutter 676, shredder 171 |  |
| 材料形态 | `chunkGt` | 3591 | 177 | extruder 2652, boxinator 706, shredder 175 |  |
| 材料形态 | `blockPlate` | 3249 | 121 | extruder 2524, boxinator 361, shredder 120 |  |
| 材料形态 | `round` | 2977 | 180 | extruder 2060, press 528, shredder 180 |  |
| 材料形态 | `crate.64.dust` | 2911 | 971 | boxinator 1940, unboxinator 971 |  |
| 材料形态 | `crate.dust` | 2859 | 971 | boxinator 1888, unboxinator 971 |  |
| 材料形态 | `ring` | 2749 | 87 | extruder 2532, shredder 87, rollbender 49 |  |
| 材料形态 | `gearGt` | 2696 | 116 | extruder 2561, shredder 116, generifier 19 |  |
| 材料形态 | `gearGtSmall` | 2696 | 116 | extruder 2561, shredder 116, generifier 19 |  |
| 材料形态 | `plantGtFiber` | 2431 | 1038 | squeezer 1038, shredder 986, smelter 276 |  |
| 材料形态 | `storage.raw` | 2428 | 493 | boxinator 1439, crusher 493, unboxinator 493 |  |
| 材料形态 | `crate.64.plate` | 2398 | 563 | boxinator 1836, unboxinator 562 |  |
| 材料形态 | `plantGtWart` | 2384 | 1038 | squeezer 1038, shredder 971, smelter 284 |  |
| 材料形态 | `plantGtBlossom` | 2378 | 1038 | squeezer 1038, shredder 971, smelter 276 |  |
| 材料形态 | `plantGtBerry` | 2365 | 1038 | squeezer 1038, shredder 971, smelter 276 |  |
| 材料形态 | `chemtube` | 2351 | 1066 | canner 2323, extruder 26, melter 1 |  |
| 材料形态 | `crate.64.ingot` | 1847 | 374 | boxinator 1474, unboxinator 373 |  |
| 材料形态 | `minecartWheels` | 1807 | 87 | extruder 1716, shredder 87, smelter 2 |  |
| 材料形态 | `arrowGtPlastic` | 1775 | 387 | press 539, unboxinator 539, melter 253 |  |
| 材料形态 | `gemFlawed` | 1732 | 56 | press 1173, cutter 212, crusher 112 |  |
| 材料形态 | `dustDiv72` | 1661 | 242 | shredder 524, boxinator 484, unboxinator 440 |  |
| 材料形态 | `crate.ingot` | 1487 | 374 | boxinator 1114, unboxinator 373 |  |
| 材料形态 | `wireFine` | 1432 | 54 | extruder 1219, wiremill 98, shredder 54 |  |
| 材料形态 | `crate.64.raw` | 1399 | 493 | boxinator 906, unboxinator 493 |  |
| 材料形态 | `plantGtTwig` | 1394 | 1038 | squeezer 1038, smelter 276, melter 80 |  |

按类型合计（第一缺口口径）：材料形态 194334，物品/方块 19065，流体 10340

### 17.2 缺配方（按机器，前 20 项）

这些行已经能完整翻译成 CC 身份，只差配方本身。“提示”按已有内容给出，仅供排期参考。

| GT6 map | 缺配方行 | 部分一致 | 已证明 | 交付深度 | 提示 |
| --- | ---: | ---: | ---: | --- | --- |
| `gt.recipe.extruder` | 224439 | 0 | 2787 | `bounded_subset` | 补材料规则模板 |
| `gt.recipe.mixer` | 47307 | 4556 | 7228 | `bounded_subset` | 扩展已有 wave |
| `gt.recipe.cutter` | 20774 | 0 | 0 | `bounded_subset` | 补材料规则模板 |
| `gt.recipe.boxinator` | 7588 | 0 | 0 | `runtime_only` | 新开 dump wave |
| `gt.recipe.generifier` | 7159 | 16 | 7 | `bounded_subset` | 扩展已有 wave |
| `gt.recipe.unboxinator` | 5355 | 0 | 0 | `denominator_only` | 先做机器，再做 wave |
| `gt.recipe.steamcracking` | 5330 | 0 | 0 | `denominator_only` | 先做机器，再做 wave |
| `gt.recipe.mortar` | 4699 | 0 | 0 | `bounded_subset` | 补材料规则模板 |
| `gt.recipe.welder` | 3068 | 752 | 0 | `bounded_subset` | 补材料规则模板 |
| `gt.recipe.lathe` | 2261 | 0 | 0 | `bounded_subset` | 补材料规则模板 |
| `gt.recipe.rollingmill` | 1532 | 297 | 0 | `bounded_subset` | 补材料规则模板 |
| `gt.recipe.sluice` | 1352 | 0 | 0 | `bounded_subset` | 新开 dump wave |
| `gt.recipe.crusher` | 1257 | 0 | 302 | `bounded_subset` | 补材料规则模板 |
| `gt.recipe.distillery` | 888 | 0 | 6 | `bounded_subset` | 扩展已有 wave |
| `gt.recipe.freezer` | 882 | 6 | 2 | `bounded_subset` | 新开 dump wave |
| `gt.recipe.polarizer` | 857 | 0 | 0 | `bounded_subset` | 新开 dump wave |
| `gt.recipe.loom` | 691 | 0 | 476 | `bounded_subset` | 扩展已有 wave |
| `gt.recipe.fermenter` | 677 | 44 | 2 | `bounded_subset` | 新开 dump wave |
| `gt.recipe.smelter` | 640 | 0 | 16821 | `bounded_subset` | 补材料规则模板 |
| `gt.recipe.shredder` | 630 | 0 | 29543 | `bounded_subset` | 扩展已有 wave |

### 17.3 排除候选（待决策，不会自动生效）

下列缺口来自 GT6 以外的 mod，或是旧分母排除的图。决定不移植的，把规则写进 `tools/waves/portfolio/gt6-full-coverage-reassessment/exclusions.json` （要写理由和决策人），它们就会从进度目标里移出；决定移植的，留在上面的清单里。

- 别的 mod：`dye.watermixed.blue`（97）、`dye.watermixed.brown`（94）、`dye.watermixed.cyan`（85）、`dye.chemical.black`（83）、`dye.watermixed.yellow`（83）、`binnie.juicelemon`（82）、`for.honey`（80）
- 旧分母排除的图：`gt.recipe.anvil`（9228）、`gt.recipe.cruciblealloying`（414）
