# GT6 长尾形态开门详细计划

> 计划 slug：`registry/gt6-long-tail-forms`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 长尾形态开门
> 性质：按改造后的形态需求普查 `openable` 批量开门。
> 总计划第 2 张落地卡，见 [GT6 批量移植总计划](GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = prep
capability_slug              = registry/gt6-long-tail-forms
unique_active_wave           = null
prep_owned_paths             = tools/waves/prep/gt6-long-tail-forms/**
landing_owned_paths          = tools/waves/registry/gt6-long-tail-forms/required_forms.json；
                               tools/material_form_authority.json；material_registration_gate.json（builder 产物）；
                               material_prefixes/chemtube.json（新）；textures/item/material/**（新前缀）；
                               src/generated/resources/**
landing_depends_on           = 粉块批量卡已关；当前 unique-active 空窗
partial_close_allowed        = false；量大时按“物品 / 方块”拆两张卡，每张独立关
```

---

## 0. 开场判断

按 [材料前缀身份合同](../../../current/material-prefix-identity.md)：

- 公共 16 前缀给新材料开门 = 新注册独立 `MaterialItem` `cruciblecraft:{material}/{form}`。
  不改公共名单本身。
- 其余库存前缀给新材料开门 = 共享 `PrefixMaterialItem` 多一种组件值，不新增 Item id。
- 方块形态（hosted）= 每材料独立 Block。

开门的唯一入口是卡的 `required_forms.json` → `build_gt6_material_form_gate.py --write`，
不手改 gate，不手改 `materials/*.json`。

## 1. 分母

以改造后的普查 `openable` 为准，排除：`storage_dust`（粉块卡）、`crate.*`、`bulletGt*`（决策暂缓）、
`gated_unresolved`（已开门，修查找）。首轮逐行分类里这类形态约 75,500 行（第一缺口上界），
翻译链修复后会下降。

主要形态与 CC 前缀（开卡时以普查为准）：

| GT6 | CC 前缀 JSON | 备注 |
| --- | --- | --- |
| `plateTiny` | `tiny_plate` | 别名 `platetiny` |
| `plateCurved` | `curved_plate` | 含铜族弯板 blocker |
| `crushedPurifiedTiny` / `crushedCentrifugedTiny` | `tiny_washed_crushed_ore` / `tiny_centrifuged_crushed_ore` | |
| `scrapGt` / `casingSmall` / `chain` / `chunkGt` / `round` / `billet` | `scrap` / `small_casing` / `chain` / `chunk` / `round` / `billet` | |
| `ingotQuadruple` / `ingotQuintuple` | `quadruple_ingot` / `quintuple_ingot` | |
| `gemFlawed` / `gemFlawless` / `gemExquisite` / `gemLegendary` / `plateGemTiny` | 同名 `gem_*` / `tiny_plate_gem` | |
| `dustDiv72` / `minecartWheels` / `spring` / `springSmall` / `rotor` | 同名 | |
| `arrowGtWood` / `arrowGtPlastic` / `plantGt*` | 同名 | 美术清单 `gt6_misc_prefix_art_manifest.json` |
| `toolHead*`（锤、锉、宝石镐、建筑杖、电锯、施工镐、螺丝刀） | 同名 `tool_head_*` | |
| `chemtube` | **无** | 先建前缀 JSON 与 GT6 原图，再开门 |
| 公共 16（`ring`、`gearGt`、`screw`、`dust`、`foil` 等）的新材料 | 独立 `MaterialItem` | 翻译链修复后先确认是否真缺 |

## 1.1 获得格（D0）

形态本身的获得来自配方（压模、切割、车床等），不在本卡。本卡只保证身份存在。
没有任何 live 获得路径的形态，在关卡报告里列出，交给后续配方卡。

## 2. 实施

1. 普查 `openable` 按上表分组，写 `required_forms.json`。
2. `chemtube`：从 GT6 迁原图，建前缀 JSON，写美术清单。
3. 重建 gate、跑 datagen。
4. 注册量评估：公共 16 新材料是真新 Item；共享前缀不增 id；方块形态增 Block。

## 3. 验证与试玩

```powershell
python tools/build_gt6_material_form_gate.py --check
python tools/waves/prep/material-form-demand-census/census.py --check
python -m pytest tools/tests/test_build_gt6_material_form_gate.py tools/tests/test_material_form_authority.py tools/tests/test_material_form_demand_census.py
.\gradlew.bat runData
.\gradlew.bat runGameTestServer -PrecipeCensus
python tools/verify.py integration --profile capability-runtime
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_semantic_coverage.py --write
```

人工 `runClient`：每组形态抽一个，确认名字、贴图、染色、EMI 与标签正确。

## 4. 明确不接管

- 把公共 16 收进组件，或把长尾改成一人一 id
- 按 `ungated_generated_flag_pairs` 全开
- `crate.*`、`bulletGt*`、`storage.raw/gem/plateGem`
- 任何配方

## 5. 关闭清单

- [ ] gate 新增 = 普查 `openable`（排除项除外）
- [ ] `chemtube` 前缀与 GT6 原图落地
- [ ] registry census 通过
- [ ] 逐行分类重跑，`missing_material_form` 只剩排除项与 `gated_unresolved`
- [ ] 人工 `runClient` 签收
