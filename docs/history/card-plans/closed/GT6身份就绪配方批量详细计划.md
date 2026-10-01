# GT6 身份就绪配方批量详细计划

> 计划 slug：`recipe/gt6-identity-ready-bulk`
> 状态：已关（`runtime_ready`，`workflow=accepted`）。本文件位于 `card-plans/closed/`。试玩未签。
> 正式名称：GT6 身份就绪配方批量
> 性质：把身份已经能翻译、主机 Java 也能收下、但还没进配方图的 dump 行发布出去。
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = recipe/gt6-identity-ready-bulk
unique_active_wave           = null
dump_map                     = sluice, shredder, extruder, bath, melter, injector,
                               implosioncompressor, canner, cokeoven, autoclave
landing_owned_paths          = tools/waves/recipe/gt6-identity-ready-bulk/**；
                               src/recipe_generated/**/<machine>/identity_ready/**；
                               publication_policy/*_identity_ready.json；
                               src/test/.../gametest/Gt6IdentityReadyBulkGameTests.java
landing_depends_on           = 化学杂项卡、挤压机批量卡、基础加工机、浸洗、熔融、注射、
                               聚爆、焦炉、大型高压釜已关
partial_close_allowed        = false
```

---

## 0. 开场判断

覆盖页上这十张图的缺口是 `translatable_missing`：物品和流体身份已经有了，缺的是配方行。洗矿机要新开 dump wave。粉碎机以及熔融、注射、聚爆、高压釜在已有图上加一组 on-demand 行，不重开已经封存的 production lock。

挤压机和浸洗的覆盖提示写的是「补材料规则模板」，因为图上还挂着旧规则文件。缺的这几千行并不是少数骨架乘材料表：挤压机大约三千四百个互不相同的骨架，没有一组达到 16 行；浸洗能并成大组的也只有一部分。所以这张卡按 dump 里已经生成的行逐条发布，不新写笛卡尔材料规则，也不把公共 16 或长尾形态再打开一遍。

洗矿机小机器原来只有 4 个物品输出槽，面板和验证器都是 9。矿石副产那 3,872 行能过加载，但出不了机器。这张卡把 `cruciblecraft:sluice` 的输出槽扩到 9。粉碎机缺的 6,061 行都在 4 个输出以内，槽位不动。

## 1. 分母

选行规则与化学杂项卡相同：`published + existing + blocked = source`。主机验证器拒绝的行、未映射物品、未开门形态、有损别名、以及和已有输入签名相撞的行留在 `blocked.json`。不发 stand-in。

| GT6 图 | 覆盖缺行 | 主机能收 | 留下的原因 |
| --- | ---: | ---: | --- |
| sluice | 4,840 | 4,840 | 无。输出槽扩到 9 之后能排出副产 |
| shredder | 6,061 | 6,061 | 无。现有 4 个输出槽够用 |
| extruder | 4,170 | 3,692 | 模具不是 `extruder_shape_*` 的 476 行，EU/t 超过 256 的 2 行 |
| bath | 2,505 | 2,477 | 流体输入超过 4,000 mB 的约 30 行 |
| melter | 825 | 825 | 无 |
| injector | 500 | 500 | 无 |
| implosioncompressor | 296 | 296 | 无 |
| canner | 82 | 82 | 无 |
| cokeoven | 81 | 81 | 无。焦炉本体有 9 个输出槽、输出罐 64,000 mB |
| autoclave | 26 | 26 | 无 |

浸洗另外 2,981 行是 `translated_item_io`：物品对得上，流体不同。这张卡不改它们，也不用别的流体顶。

容量：on-demand `matrix_v1`，同形行每个 holder 不超过 2,048，更小的同形组合并后每个不超过 512。cache 16。族号用 `#identity_ready_`，避开挤压机已经占用的 `#bulk_`。全局逻辑行上限 616,572；这批是逐行发布，不是材料规则展开。

## 2. 实施

1. 每张图一个 slug：`<machine>/identity-ready`，publication group `cruciblecraft:<machine>/identity_ready`。
2. 选行复用化学杂项导入器，再套这张卡的主机过滤。过滤条件和 Java 验证器一致，不额外加严。
3. 聚爆和焦炉没有 `ProcessingMachineBlockEntity`。GameTest 只核对匹配，不在结构里跑完。
4. 洗矿机槽位：`ModProcessingMachines.spec()` 与 `machine_delivery.json` 里 `cruciblecraft:sluice` 的 `item_outputs` 从 4 改为 9。

## 3. 验证

```powershell
python tools/waves/recipe/gt6-identity-ready-bulk/build_identity_ready.py
python tools/build_recipe_bulk.py import-source --spec tools/waves/recipe/gt6-identity-ready-bulk/<machine>/recipe_import.json --check
python tools/build_recipe_bulk.py compile --wave <machine>/identity-ready --check
.\gradlew.bat runGameTestServer -PwaveRecipes=recipe/gt6-identity-ready-bulk
```

GameTest：`identityReadyGroupsStayOnDemand`、`identityReadySamplesRun`。样本在 `src/test/resources/gt6_identity_ready_samples.json`。

## 4. 本轮发布

`published + existing + blocked = source`。族号是 `#identity_ready_`。最大 holder 是洗矿机 1,936 行，不超过 2,048。

| 图 | 源行 | 发布 | 已有 | blocked | holder | 最大 holder |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| sluice | 4,840 | 4,840 | 0 | 0 | 4 | 1,936 |
| shredder | 41,246 | 6,053 | 29,543 | 5,650 | 73 | 465 |
| extruder | 325,595 | 3,619 | 304,941 | 17,035 | 59 | 512 |
| bath | 59,855 | 5,419 | 49,416 | 5,020 | 37 | 585 |
| melter | 6,756 | 816 | 3,610 | 2,330 | 4 | 478 |
| injector | 638 | 469 | 103 | 66 | 5 | 112 |
| implosion | 1,072 | 296 | 776 | 0 | 7 | 74 |
| canner | 3,609 | 40 | 7 | 3,562 | 2 | 36 |
| coke oven | 124 | 81 | 38 | 5 | 2 | 72 |
| autoclave | 392 | 26 | 366 | 0 | 1 | 26 |

浸洗发布 5,419 行，比覆盖页「缺配方」2,505 多出来的，是物品已经对上、流体不同的那些行。它们的输入签名没有被已有配方占用，所以按 GT6 原行另发一条，不改原来的配方，也不换流体。主机拒绝的 38 行是流体量超过 4,000 mB，2 行是展示用 fake。

挤压机主机拒绝：模具不是 `extruder_shape_*` 的 476 行，EU/t 超过 256 的 2 行。其余 blocked 是缺形态、缺物品，或和已有输入相撞。装罐 42 行、注射 45 行是输入签名已经被活配方占用。

粉碎机 6,061 行里 6,053 行发布，8 行是缺身份或输入相撞。洗矿机 4,840 行全部发布。

## 5. 明确不接管

- 挤压机模具对不上或 EU/t 高于 256 的行
- 浸洗流体超过 4,000 mB 的行，以及只差流体的 2,981 行
- 装箱机、拆箱机、锤、数控、榨汁机、复制机
- 压机、榨汁机、熔炉、破碎机上更小的身份就绪缺口

## 6. 关闭清单

- [x] `published + existing + blocked = source`
- [x] on-demand，holder 不超过 2,048，cache 16
- [x] GameTest `cruciblecraft_wave_recipe_gt6_identity_ready_bulk` 2 项通过（2026-10-01）
- [x] 洗矿机 9 输出槽与 `machine_delivery.json` 一致
- [ ] 人工 `runClient` 签收
