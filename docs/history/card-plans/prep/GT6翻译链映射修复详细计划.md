# GT6 翻译链映射修复详细计划

> 计划 slug：`registry/gt6-translator-mapping-repair`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 翻译链映射修复
> 性质：只修 GT6 → CC 的查找表和覆盖页证据，不注册、不开门、不写配方。
> 总计划 P1，见 [GT6 批量移植总计划](GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = registry/gt6-translator-mapping-repair
unique_active_wave           = null
prep_owned_paths             = tools/gt6_resolve.py；tools/gt6_oredict_cross_reference.json；
                               tools/recipe_bulk/ordinary_source.py（EXTRA_PREFIX_ITEM_TO_FORM）；
                               tools/machine_fluid_mapping.json；tools/machine_item_classification_rules.json；
                               tools/waves/portfolio/gt6-full-coverage-reassessment/**
landing_owned_paths          = 无。任何 src/main 改动都不在本卡
landing_depends_on           = 无；可在任何 unique-active 期间做
promotion_trigger            = 若 compile --wave all --check 输出变化，或需要删改 src/main 身份，转 unique-active
```

---

## 0. 开场判断

2026-09-24 首次逐行比较后发现，“缺身份” 345,826 行里有一大块是查找表没对上，不是 CC 真缺。
先修查找表，形态普查和后面的开门卡才有可信的分母。

## 1. 已查实的漏映射

| 项 | 现象 | 证据 | 修法 | 受影响行（第一缺口） |
| --- | --- | --- | --- | ---: |
| 挤压模具 19 个 | `gt6_resolve.extruder_shapes()` 按名字归一化匹配，`Shape_Extruder_Plate_Tiny` ↔ `tiny_plate` 词序不同，返回 `live: False` | `ExtruderShapeCatalog.java` 已有 meta 10000–10031 共 32 个 | 按 meta 匹配 | 162,800 |
| `gt.meta.storage.dust` | 对照表无键 | `blocked_recipe_ledger.py` 65 行已有 `storage_dust` | 加 `storage.dust → storage_dust` | 15,727（真缺 951 种材料，见粉块卡） |
| `gt.meta.storage.ingot` | 对照表无键；`bath_identities.py` 有 `storage_ingot`，`machine_item_classification_rules.json` 说 `block` | GT6 `Loader_PrefixBlocks.java` 43：`OP.blockIngot` | 加 `storage.ingot → block`；`bath_identities.py` 同步改为 `block` | 2,634 |
| `gt.meta.storage.plate` | 同上；分类规则说 `dense_plate` | GT6 44 行 `OP.blockPlate`；CC `storage_plate` 别名 `blockPlate` | 加 `storage.plate → storage_plate`，改正分类规则 | 2,399 |
| `gt.meta.storage.solid` | 对照表映射成 `blockIngot` | GT6 46 行是 `OP.blockSolid` | 改正；CC 无对应前缀则保持缺 | 待重算 |
| 流体 `sluicejuice` | 无别名 | `ModFluids` 已有 `cruciblecraft:sluice_juice` | 加别名 | 1,362 |
| 流体 `mercury` | `machine_fluid_mapping.json` 记 `no_cc_fluid` | `molten_mercury` 已 live | 改为 mapped | 213 |
| 流体 `glass` | 同上 | `molten_glass` 已 live | 改为 mapped | 133 |
| ReactorCore2x2 | 覆盖页第 4 节记 `denominator_only` | `ModBlocks` 349–356 已有 2x2 反应堆核心 | 修证据扫描 | 机器 1 |

## 2. 负责人决策（2026-09-24）

- **`blockIngot` 的 CC 身份保留 `block`**（gate 486 种材料，`c:storage_blocks`）。
  `storage_ingot`（186 种，全是 `block` 的子集）退役；退役改 `src/main`，在
  [粉块批量](GT6粉块批量详细计划.md) 卡里做，不在本卡。本卡只把 `storage.ingot` 映射到 `block`。

## 3. 需要查证后再定

- `gt.meta.ore.normal.<岩石>` / `ore.broken.<岩石>`：GT6 是岩石专属矿石方块
  （`Loader_Rocks.java` 57 行起）。分类规则把它们等同 `crushed_ore`，那是把方块换成物品，不接受。
  查 CC hosted ore 是否有对应岩石宿主：有则映射，没有则保持缺。
- `gt.meta.machine` / `.double` / `.quadruple` / `.dense`（各约 2,900 行）：查它在 GT6 是哪个前缀的方块形态，
  CC 的 `machine_casing*` 能否对上。
- `machine_item_classification_rules.json` 里 `storage.dust` 仍记 out-of-scope，已过期，改正。

### 3.1 查证结论（2026-09-25）

- **岩石矿石不折成 `crushed_ore`**：GT6 `Loader_Rocks` 把
  `ore.normal.<rock>` / `ore.broken.<rock>` 注册为带岩石宿主贴图和方块行为的
  `PrefixBlock_`，不是物品形态。CC 的 `OreStoneHost` / `GtHostedOreBlock` /
  `GtBrokenOreBlock` 覆盖已证明的宿主身份；没有逐项 material/meta 证据的变体继续
  `blocked`，分类规则改为 `hosted_ore_block`，不再宣称等价 `crushed_ore`。
- **机器外壳可以对上**：GT6 `Loader_PrefixBlocks` 的
  `machine`、`machine.dense`、`machine.double`、`machine.quadruple` 分别使用
  `OP.casingMachine*`；CC 已有 `machine_casing*` 四个材料前缀，因此四个原始
  `gt.meta.machine*` 入口按对应前缀翻译。
- `storage.plate` 的 CC 形态保留 `storage_plate`（不是 `dense_plate`）；`storage.ingot`
  翻译到 `block`。直接 `OP.blockSolid` 仍不进入 live 前缀映射；历史
  `gt.meta.storage.solid` 原始行保留已有、源证据支持的 `block` 兼容投影，并在覆盖比较中
  与 live `block` 归一化，不新注册形态也不开门。

## 4. 翻译链指纹盲区

`build_semantic_coverage.py` 的 `translator_fingerprint()` 不含对照表、`machine_fluid_mapping.json`、
材料开门名单。只改这些 JSON 时 `--check` 不报过期。本卡把这三份加进指纹。

## 5. 命令

```powershell
python tools/gt6_resolve.py "OP.blockDust(MT.Fe)"
python tools/build_recipe_bulk.py compile --wave all --check
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_semantic_coverage.py --write
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --check
python -m pytest tools/tests/test_gt6_full_coverage_reassessment.py
```

`compile --wave all --check` 必须保持不变（已编译波次读的是冻结 source pack，改查找表不应影响它）。
若有变化，停下，转 unique-active。

## 6. 关闭清单

- [x] 模具按 meta 匹配，19 个 token 全部 `live: True`
- [x] storage / 流体别名按 §1 落地，`storage.ingot → block`
- [x] 矿石与机器外壳查证结果写进本文件
- [x] 指纹覆盖三份 JSON
- [x] 逐行分类重跑，覆盖页“缺身份”与“缺配方”数字更新并提交
- [x] 翻译链校准一致率不下降（当前 99.5%）
