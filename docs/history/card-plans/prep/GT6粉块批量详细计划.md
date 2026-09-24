# GT6 粉块批量详细计划

> 计划 slug：`registry/gt6-storage-dust-blocks`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 粉块批量
> 性质：按 GT6 dump 实际用到的材料批量开 `storage_dust`（GT6 `OP.blockDust` / `gt.meta.storage.dust`），
> 并退役与 `block` 重复的 `storage_ingot`。
> 总计划第 1 张落地卡，见 [GT6 批量移植总计划](GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = registry/gt6-storage-dust-blocks
unique_active_wave           = null
prep_owned_paths             = tools/waves/prep/gt6-storage-dust-blocks/**
landing_owned_paths          = tools/waves/registry/gt6-storage-dust-blocks/required_forms.json；
                               tools/material_form_authority.json（新 gate_section）；
                               src/main/resources/data/cruciblecraft/material_registration_gate.json（builder 产物）；
                               src/generated/resources/**（datagen 产物）；
                               material_prefixes/storage_ingot.json（退役）；tools/bath_identities.py；
                               src/recipe_generated/** 下引用 storage_ingot 的 bath 波；注册表别名
landing_depends_on           = 翻译链映射修复与形态需求普查改造已完成；当前 unique-active 空窗
partial_close_allowed        = false
```

---

## 0. 开场判断

机制已经齐全，只差名单：

- 方块类 `MaterialDustBlock` / `MaterialDustBlockItem`；`ModBlocks` 963–980 行按 gate 自动注册，
  registry path `cruciblecraft:{material}/storage_dust`。
- 美术：`gt6_dust_forms_art_manifest.json` 已把 GT6 `materialicons/powder/blockdust.png` 与 overlay
  迁到 `textures/item/material/storage_dust*.png`；方块模型 `material_storage_dust.json` 按材料色染色。
- 合成：`ModRecipeProvider` 3629–3641 行的 `prefix_pack/dust_to_storage_dust`、`storage_dust_to_dust`、
  `storage_dust_to_small_dust` 按 gate 生成。对应 GT6 `Loader_Recipes_Handlers.java` 568、569、605 行。

当前 gate 只有 12 种材料（燃料床 11 + 焦炉 `oil_shale`），dump 需求 963 种。

## 1. 分母

以改造后的普查 `census.json` 中 `form == storage_dust` 的 `openable` 为准，预计约 951 对。
不按 `generation_flag` 全开：GT6 对所有粉材料都生成 blockDust，但本卡只开 dump 里真正被配方用到的。

## 1.1 获得格（D0）

GT6 获得：9 粉 → 1 粉块（合成），粉块 → 9 粉 / 36 小粉（合成）。CC 已按上面的 `prefix_pack` 配方生成，
无新网格。前提是该材料的 `dust` 已开；普查会把“粉块开了但粉没开”的对标为 `gated_unresolved` 或跳过。

## 2. 实施

1. `tools/waves/registry/gt6-storage-dust-blocks/required_forms.json` 写入普查名单。
2. `tools/material_form_authority.json` 登记新 gate_section。
3. `python tools/build_gt6_material_form_gate.py --write` 重建 gate；再跑 datagen。
4. 注册量增加约 950 个方块 + 950 个方块物品：跑 registry census，确认启动时间与内存可接受。

## 2.1 退役 `storage_ingot`（负责人 2026-09-24 决定）

GT6 `OP.blockIngot` 在 CC 只留 `block`（486 种材料）。`storage_ingot`（186 种，全是 `block` 的子集）退役：

1. 普查所有引用 `*/storage_ingot` 的配方、标签、EMI 与测试；已知 bath 身份波经
   `tools/bath_identities.py` 用它，需要改成 `block` 后重编译对应 bath 波。
2. 从 gate 删掉 `storage_ingot`，前缀 JSON 退役。
3. 旧存档：用注册表别名把 `{material}/storage_ingot` 指到 `{material}/block`，不做兑换壳。
4. `compile --wave all --check` 的变化只能来自这次改名，逐行核对。

## 3. 美术合同

沿用现有 `storage_dust` 合同：GT6 powder 图标集 + 材料染色（负责人 2026-09-24 确认）。
不按 GT6 材料图标集区分。

## 4. 验证与试玩

```powershell
python tools/build_gt6_material_form_gate.py --check
python tools/waves/prep/material-form-demand-census/census.py --check
python -m pytest tools/tests/test_build_gt6_material_form_gate.py tools/tests/test_material_form_authority.py
.\gradlew.bat runData
.\gradlew.bat runGameTestServer -PrecipeCensus
python tools/verify.py integration --profile capability-runtime
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_semantic_coverage.py --write
```

人工 `runClient`：抽 3 种材料（金属、宝石、有机）合成粉块、放置、挖回、拆回粉；EMI 可见合成。

## 5. 明确不接管

- `storage.raw` / `storage.gem` / `storage.plateGem`：CC 无前缀 JSON；需求主要来自 boxinator（决策暂缓）
- 任何配方图上用到粉块的加工配方（配方卡负责）

## 6. 关闭清单

- [ ] gate 中 `storage_dust` = 普查名单，且每种材料的 `dust` 已开
- [ ] `storage_ingot` 无残留引用，旧存档别名到 `block` 有 GameTest
- [ ] registry census 通过，启动与内存有测量数
- [ ] 逐行分类重跑，`form:storage.dust` 缺口归零或只剩 `gated_unresolved`
- [ ] 人工 `runClient` 签收
