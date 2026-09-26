# GT6 前缀规则类配方批量详细计划

> 计划 slug：`recipe/gt6-prefix-regular-bulk`
> 状态：已关（`runtime_ready`，`workflow=accepted`）。本文件位于 `card-plans/closed/`。
> 正式名称：GT6 前缀规则类配方批量
> 性质：用 Rule IR 一次导入十二张前缀规则类 GT6 图的全部可翻译行。
> 总计划第 6 张落地卡，见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = recipe/gt6-prefix-regular-bulk
unique_active_wave           = null
prep_owned_paths             = tools/waves/prep/gt6-prefix-regular-bulk/**
landing_owned_paths          = tools/waves/recipe/gt6-prefix-regular-bulk/<map>/**；
                               src/recipe_generated/** 下对应图；src/component_rule_generated/** 被替换规则；
                               src/test/.../gametest/
landing_depends_on           = 挤压机配方批量已关；容量门已关
partial_close_allowed        = false；每张图是独立 publication group
```

---

## 0. 开场判断

这些图的主体都是“同材料换形态”，和挤压机同一套 Rule IR（`prefix_transform` / `shape_transform` +
`exact_remainder`）。一张卡带多个 `(target_map, publication_group)` 是工作流允许的。

## 1. 分母（2026-09-25 重跑，`translatable_missing`）

| GT6 图 | 可翻译缺配方 | 现有 material_rule（文件 → 展开） | 备注 |
| --- | ---: | --- | --- |
| cutter | 25,206 | 2 → 651 | 另有缺物品 2,248 |
| mortar | 5,834 | 4 条 | 与 shredder 有同形行，归属以源图为准 |
| welder | 3,664 | 1 → 321（另有 1,045 行 `translated_io_only`） | |
| lathe | 2,521 | 2 → 929 | |
| rollingmill | 2,139 | 1 → 336（297 行 io_only） | 与 compressor 有同形行 |
| press | 1,818 | 5 → 1,191 | 另有缺形态 5,997 |
| crusher | 1,587 | 2 条；另有 361 行 reference 追溯 | ore-chain 投影行要先去重；另有缺形态 10,817 |
| anvil.bend.big / small | 723 / 507 | bender 2 → 638 | 查 CC 承载图。不是被排除的 `gt.recipe.anvil` |
| rollbender | 535 | 1 → 438（405 行 io_only） | |
| wiremill | 424 | 10 → 356（31 行 io_only） | |
| sifter | 405 | —；1,720 行 reference 追溯 | ore-chain；另有缺形态 2,468 |
| 合计 | 45,363 | | |

`translated_io_only`（时长或 EU/t 不同）的现有行在本卡按 GT6 数值改正。

容量预算（`recipe/gt6-bulk-capacity`）：45,363 行走 on-demand `matrix_v1`。每个 holder 不超过 2,048 行，避免字典和线缆上限。每个 publication group 的 cache 声明是 16。不进 eager。

## 1.1 获得格（D0）

无新网格，全部主机已 live。切割机补了 3 个物品输出和 1 个流体槽，车床补了 2 个物品输出，青铜粉碎机和筛分机补到 12 个物品输出。砧座 `specialValue == 0` 按 GT6 一锤完成。

## 2. 实施

1. 每张图一个 Source Pack + `recipe_import.json` + Rule IR；`prefix_transform` 加 `exact_remainder`。
2. crusher / sifter 的 ore-chain reference 行：先确认与 dump 行的对应，避免同一产物两套配方。
3. 被逐行覆盖的 `material_rule` 删除，规则同挤压机卡 §3。差集为空才删。
4. 按容量门给本卡的预算发布。

主机槽位不够的行先扩槽，不把它们记成 blocked。焊机的选择电路是 `PRESERVE`，照 GT6 发布。切割机流体槽和水配方校验是 4096 mB；超过这个量的行仍 blocked，见 §4.2。

## 3. 验证与试玩

```powershell
python tools/build_recipe_bulk.py import-source --spec <每张图的 recipe_import.json> --check
python tools/build_recipe_bulk.py compile --wave <每张图的 slug> --check
python tools/verify.py integration --profile recipes
.\gradlew.bat runGameTestServer -PgameTestGrid=content -PgameTestFilter=prefixRegular
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

提交的 GameTest 在 `cruciblecraft_content`。`-PwaveRecipes` 只用于进行中的临时命名空间。

GameTest：每张图抽三行（规则行、余量行、边界材料）执行。样本在 `src/test/resources/gt6_prefix_regular_samples.json`。
人工 `runClient`：切割机、车床、线材轧机各跑一条，EMI 可见。

## 4. 明确不接管

- 挤压机（上一张卡）
- hammer 图（手锤，不是机器；blocker `tools/world-behaviors`）
- `gt.recipe.anvil`（旧排除待决策）

## 4.1 本轮发布

`import-source --check` 与 `compile --check` 十二张图都当前。content 网格 GameTest 两条都通过（`prefixRegularGroupsStayOnDemand`、`prefixRegularSamplesRun`）。

| 图 | 发布 | blocked | 族 |
| --- | ---: | ---: | ---: |
| cutter | 25,389 | 2,065 | 73 |
| mortar | 5,832 | 486 | 41 |
| welder | 4,760 | 2 | 181 |
| lathe | 2,525 | 3 | 45 |
| rollingmill | 2,436 | 2 | 51 |
| press | 1,904 | 6,256 | 24 |
| crusher | 12,210 | 722 | 104 |
| anvil.bend.big | 722 | 1 | 18 |
| anvil.bend.small | 505 | 2 | 14 |
| rollbender | 940 | 0 | 28 |
| wiremill | 455 | 0 | 16 |
| sifter | 2,377 | 500 | 23 |

石头宿主已接上：17 种碎矿石（含深色/浅色海晶石）、主世界石头、下界岩，以及末地石、砂岩、砂砾、沙、红沙、泥。粉碎机因此多发布 10,324 行，筛分机多发布 1,968 行。`storage.raw` 仍无 runtime。筛分机剩下的 500 行是 anti 材料没有 `washed_crushed_ore`，不是缺石头。

已有 runtime 的 1.7 id 换成了现代表达：木板/木半砖、砖块、双层石半砖、头颅、染料，以及床、船、告示牌、树苗、木栅栏门的通配标签。装饰石头、染色玻璃、科技零件、能量水晶、流体/物品管道只在注册表里已有对应 id 时发布。科技零件 meta 以 `technological_parts.json` 为准。和 `recipe/machine/press/` 里已有电路配方同一输入的 13 行留在原配方，不重复发布。切割机水槽和水配方校验只放到 4096 mB。箭和子弹仍排除，不注册新形态。

拿开箭和子弹之后还剩 4,755 行。分类、行数和以后谁收见 §4.2。逐行原因在各图 `blocked.json`。`minecraft:quartz_ore` 译成 `minecraft:nether_quartz_ore`。石半砖、石砖、泥土的 meta 按 1.7 拆开。

`verify.py integration --profile recipes` 仍失败：`PrepMachinesTest` 要求 `ModMenus.java` 不含 `printer`，而打印机菜单早已注册。这不是本卡改动。reconciliation `--write` 还没跑。

## 4.2 本卡补不完、以后再查

这些行现在不能在本卡补完。缺形态不能在配方卡上开门，缺的方块和物品不能用别的 id 顶上，总计划 §6 已经排除的保持排除。`docs/current/gt6-full-coverage.md` 和 `blocked.md` 都是生成物，不手写这份索引。逐行账本：

`tools/waves/recipe/gt6-prefix-regular-bulk/<map>/blocked.json`

十二张图 blocked 合计 10,039。下表加总等于这个数。

| 类 | 行 | 落在 | 以后 |
| --- | ---: | --- | --- |
| `bulletGt*` | 4,206 | press | 总计划 §1.3 / §6。不补映射、不开形态 |
| 装饰石头缺变种或半砖 | 2,091 | cutter 1,920（半砖 1,285、整块 635），crusher 171 | 注册表里没有的 `cruciblecraft:{stone}/{variant}` 或 `slab_{face}`。另开建筑方块卡，本卡不发明方块 |
| `arrowGtWood` / `arrowGtPlastic` | 1,078 | press | 与子弹同一决定。不注册 `arrow_gt_wood` / `arrow_gt_plastic` |
| 工具头 | 779 | press | 713 行是已关闭的工具头重映射；66 行是未注册的 `tool_head_pickaxe_gem`。总计划 §6 的 toolhead，不开 unique 工具头 |
| 没有现代目录 id | 644 | mortar 481，cutter 120，crusher 31，press 7，lathe 3，rollingmill 2 | 蜜蜂 320 看 `worldgen/bees`；食物 71 看 `worldgen/food`。树苗 109、植物方块 48、书 29、旧 `rockores` / `crystalores` / `vanillaores` 28、`randomtools` 39 仍无 runtime。不发明 |
| anti `washed_crushed_ore` | 500 | sifter | 125 种 anti 材料 × 砂砾/沙/红沙/泥。形态需求走普查，下一张开门卡只消化 `openable`。本卡不开 |
| `storage.raw` | 493 | crusher | 总计划 §6。不映射到 `block_raw` |
| 未登记科技零件 | 125 | press | 电极与未进 `technological_parts.json` 的电路零件。没有 source_id 就不发 |
| `plantGtFiber@*` | 48 | press | 没有「任意植物纤维」的物品或标签 |
| 有损夹具别名 | 24 | crusher 21，anvil 3 | `FIXTURE_ONLY_LOSSY_ITEM_ALIASES`（油砂 meta 9851、砧座锆废料）。禁止发布 |
| 与手写配方同一输入 | 13 | press | `recipe/machine/press/` 里已有电路配方。不是缺口 |
| 流体超过 4096 mB | 12 | cutter | 4480–7168 mB 的水、蒸馏水、`spectral_dew`。槽仍是 4096。再抬容量另说 |
| `ForgeMicroblock:stoneRod` | 9 | cutter 8，mortar 1 | 其他 mod。总计划 §6 |
| 重复运行时签名 | 8 | cutter 5，crusher 3 | 已丢掉。不是待补 |
| 染色玻璃 / 染色陶瓦通配 | 5 | crusher 3，mortar 2 | 没有对应物品标签。不发白色顶上 |
| 钽铪碳墙 | 2 | welder | meta 18012 / 18032。没有 wall id，不映射到机壳 |
| 荧光睡莲 | 1 | mortar | `gt.block.lilypad.glowtus` 无目录 id |
| 茶 `plant_gt_blossom` | 1 | mortar | 形态未注册。归普查，本卡不开 |

## 5. 关闭清单

- [x] 十二张图各自覆盖等式成立：`published + blocked = source`。余量分类在 §4.2，逐行在各图 `blocked.json`
- [ ] io_only 行改正为 GT6 数值。旧 `material_rule` 还在，时长和 EU/t 尚未替换。差集未证明为空，关卡时保留，不挡 `runtime_ready`
- [ ] 被覆盖规则已删。差集未证明为空，规则文件保留
- [x] 容量在预算内。content 网格 GameTest `prefixRegularGroupsStayOnDemand` 与 `prefixRegularSamplesRun` 通过
- [ ] 人工 `runClient` 签收
