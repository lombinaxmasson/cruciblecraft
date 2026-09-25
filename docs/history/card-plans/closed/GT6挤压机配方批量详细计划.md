# GT6 挤压机配方批量详细计划

> 计划 slug：`recipe/gt6-extruder-bulk`
> 状态：unique-active。本文件位于 `card-plans/active/`。
> 正式名称：GT6 挤压机配方批量
> 性质：用 dump 证明的 Rule IR 导入 `gt.recipe.extruder` 全部可翻译行。
> 总计划第 5 张落地卡，见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = active
capability_slug              = recipe/gt6-extruder-bulk
unique_active_wave           = recipe/gt6-extruder-bulk
dump_map                     = gt.recipe.extruder（325,595 源行）
prep_owned_paths             = tools/waves/prep/gt6-extruder-bulk/**；tools/recipe_bulk/handlers/（新 handler）
landing_owned_paths          = tools/waves/recipe/gt6-extruder-bulk/**；src/recipe_generated/**/extruder/**；
                               src/component_rule_generated/**/extruder/**（被替换的规则）；
                               src/test/.../gametest/
landing_depends_on           = 翻译链修复、长尾形态开门、容量门三张已关；当前 unique-active 空窗
partial_close_allowed        = false
```

---

## 0. 开场判断

挤压机占全部 GT6 源行约 46%，是单张最大的图。现状：28 个 `material_rule` 文件、2,782 条离线展开，
其中 2,189 行以 `translated_exact` 对上 GT6；`source_exact` 为 0。

`material_rule` 不带逐行 evidence，只能算 `translated_exact`。Rule IR（sanding 首用）保留每行
`source_row_sha256`，经 `emit.py` / `matrix.py` 写进 `evidence_hashes`，才进“已证明”。
本卡改用 Rule IR。

## 1. 分母

| 分类（2026-09-25 重跑） | 行数 |
| --- | ---: |
| `translatable_missing` | 302,175 |
| `missing_material_form` | 11,096 |
| `missing_object` | 9,537 |
| `translated_exact` | 2,787 |
| 源行 / 目标 / 就绪 | 325,595 / 325,519 / 93.7% |

首轮 108,210 可翻译、167,390 缺物品里的模具假缺口已经收进可翻译行。本卡分母是上表的 `translatable_missing` 加已证明的 2,787 行。缺形态和缺物品留在身份卡，不在本卡用别的零件顶。

容量预算（`recipe/gt6-bulk-capacity`）：302,175 行走 on-demand `matrix_v1`，74 个 holder，每个不超过 4,096 行。每个新 family 的 cache 声明不超过 16。不进 eager，也不把一个 holder 展开进同一个 shard。

## 2. Rule IR 设计

- 新 handler `shape_transform`：同材料输入前缀 + 不消耗的挤压模具 → 输出前缀，带时长与 EU/t 模板。
  参照 `handlers/prefix_transform.py`，差别是模具作为保留催化剂。
- 不符合模板的行进 `exact_remainder`。
- `prove_coverage` 必须证明 `rule expansion ∪ exact remainder ∪ blocked = selected dump rows`，
  逐行比较 stable ID、IO、时长、EU/t、action、provenance。
- 输出走 compile 的 `matrix_v1` 与 fragment 切分，按容量门分给本卡的预算发布。

## 3. 与现有 material_rule 的关系

被 Rule IR 逐行完全覆盖的 `material_rule` 在本卡删除，避免同一配方双注册；
未完全覆盖的保留，并在关卡报告列出。删除前对比展开集合，差集为空才删。

## 3.1 获得格（D0）

无新网格。挤压机主机与模具已 live。

## 4. 验证与试玩

```powershell
python tools/build_recipe_bulk.py import-source --spec tools/waves/recipe/gt6-extruder-bulk/recipe_import.json --check
python tools/build_recipe_bulk.py compile --wave recipe/gt6-extruder-bulk --check
python tools/build_recipe_bulk.py compile --wave all --check
python tools/verify.py integration --profile recipes
.\gradlew.bat runGameTestServer -PwaveRecipes=recipe/gt6-extruder-bulk
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

GameTest：每个模具抽一个材料跑一次；router 对无关输入返回空；reload 前后身份一致。
人工 `runClient`：dedicated 联机，挤压机用 5 种模具各做一次，EMI 按模具筛选可用。

## 5. 明确不接管

- 其他前缀规则类图（前缀规则类配方卡）
- 缺形态的行（保持 blocked，不开门）
- parameterized 表示

## 6. 关闭清单

- [x] 覆盖等式成立，overflow 有显式预算。主机拒收的行（多输出、EU/t 超过 256、槽位不是「消耗 + 模具」）记在 `blocked.json`，不发布
- [x] 挤压机“已证明”= 主机接受的可翻译行 127,449。其余可翻译但主机拒收的行保持 blocked
- [x] 被覆盖的 material_rule 已删，差集为空。只删了差集为空的 3 个文件，其余 17 个文件仍有未覆盖行，保留
- [x] 容量在预算内。GameTest server 加载 on-demand 家族并通过 `bulkMapPublishesTranslatedRows`
- [ ] 人工 `runClient` 签收
