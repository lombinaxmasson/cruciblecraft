# GT6 蒸汽裂化配方批量详细计划

> 计划 slug：`recipe/gt6-steamcracking-bulk`
> 状态：已关（`runtime_ready`，`workflow=accepted`）。本文件位于 `card-plans/closed/`。试玩未签。
> 正式名称：GT6 蒸汽裂化配方批量
> 性质：把 `gt.recipe.steamcracking` 整图的可翻译行导入已落地的蒸汽裂化机。
> 总计划第 7 张的后续批次，见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)。
> 主机在第 8 张 [GT6 同类基础加工机批量](../closed/GT6同类基础加工机批量详细计划.md) 落地。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = recipe/gt6-steamcracking-bulk
unique_active_wave           = null
dump_map                     = gt.recipe.steamcracking（源行 7,746；可翻译 7,714；缺流体 32）
landing_owned_paths          = tools/waves/recipe/gt6-steamcracking-bulk/**；
                               src/recipe_generated/**/steam_cracker/steamcracking/**；
                               publication_policy/steam_cracker_steamcracking.json；
                               src/test/.../gametest/Gt6SteamcrackingBulkGameTests.java
landing_depends_on           = 化学杂项卡已关；同类基础加工机卡已关；当前 unique-active 空窗
partial_close_allowed        = false
```

---

## 0. 开场判断

蒸汽裂化机四档已经能放置、供能。开工时配方图是 `denominator_only`：7,746 源行里 7,714 行身份就绪，32 行缺流体。没有统一的形态模板，按化学杂项卡的逐行导入发。关卡后覆盖页是 `bounded_subset`，进度 99.6%。

## 1. 分母

| GT6 图 | 源行 | 可翻译 | 缺流体 | 主机 |
| --- | ---: | ---: | ---: | --- |
| steamcracking | 7,746 | 7,714 | 32 | `cruciblecraft:steam_cracker`，面板物品 1/3、流体 2/9，HU，允许不消耗催化剂，EU/t 上限 4,096，输入罐 64,000 mB |

选行规则与化学杂项卡相同：`published + existing + blocked = source`。主机 Java 验证器接受的行发布；缺流体、未映射物品、与已有输入签名相撞的行留在 `blocked.json`。不发 stand-in。

容量：on-demand `matrix_v1`，同形行每个 holder 不超过 2,048，更小的同形组合并后每个不超过 512。cache 16。不进 eager。

## 2. 实施

1. Source Pack + `recipe_import.json` + `work_set` + 人工 `production_lock`。
   slug 是 `steam-cracker/steamcracking`，publication group 是 `cruciblecraft:steam_cracker/steamcracking`。
2. 选行复用化学杂项卡的主机过滤。蒸汽裂化机验证器是 `validateSteamCracker`：物品 1/3、流体 2/9、输入罐 64,000 mB、输出罐不设上限、允许 `PRESERVE`、能量不超过 4,096。
3. 每行带 `evidence_hashes` 与 `selected_source_recipe`。
4. 配方进图之后，加载器不再把空的蒸汽裂化图当成延期。

## 3. 验证与试玩

```powershell
python tools/waves/recipe/gt6-steamcracking-bulk/build_steamcracking.py
python tools/build_recipe_bulk.py import-source --spec tools/waves/recipe/gt6-steamcracking-bulk/steam_cracker/recipe_import.json --check
python tools/build_recipe_bulk.py compile --wave steam-cracker/steamcracking --check
.\gradlew.bat runGameTestServer -PwaveRecipes=steam-cracker/steamcracking
```

GameTest：`steamCrackingGroupStaysOnDemand`、`steamCrackingSampleRuns`。样本在 `src/test/resources/gt6_steamcracking_samples.json`，一档钢制蒸汽裂化机能跑的短配方。
人工 `runClient`：放一台蒸汽裂化机，EMI 可见本图，接热量跑通一条。试玩未签不挡 `runtime_ready`。

## 4. 本轮发布

`published + existing + blocked = source`。

| 图 | 源行 | 发布 | 已有 | blocked | holder |
| --- | ---: | ---: | ---: | ---: | ---: |
| steamcracking | 7,746 | 7,714 | 0 | 32 | 5 |

五个 holder 的行数是 2,048、1,792、2,048、1,792、34，都不超过 2,048。同形大组走 `matrix_v1`，全部 on-demand，cache 16。

32 行 blocked 都是未注册流体：`for.honey` 16 行，`honeydew` 16 行。这是其他 mod 的蜂蜜流体，总计划 §6 先不补，不发明替代流体。

## 5. 明确不接管

- `for.honey`、`honeydew` 这 32 行
- 蒸馏塔药水流体
- 尚无主机的其他化学图

## 6. 关闭清单

- [x] `published + existing + blocked = source`
- [x] on-demand，holder 不超过 2,048
- [x] GameTest `cruciblecraft_wave_steam_cracker_steamcracking` 2 项通过（2026-09-26）
- [x] 空图延期已去掉
- [ ] 人工 `runClient` 签收
