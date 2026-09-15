# GT6 坩埚模具交互详细计划

> 计划 slug：`content/gt6-crucible-mold-interaction`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 坩埚模具交互
> 性质：对照 `gt6_code/gregtech6` 小坩埚 / 模具 / 浇铸口，把仍可见的交互差异接到已活着的陶瓷主机上。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 不重开 `content/gt6-crucible-mold-behavior-correction` 的 `required_test_ids`。
> 不重开 `content/gt6-mte-crucible-foundry-runtime` 身份卡。

```text
lane                         = closed
capability_slug              = content/gt6-crucible-mold-interaction
unique_active_wave           = null
depends_on                   = content/gt6-crucible-mold-behavior-correction
close_target                 = runtime_ready
maturity                     = runtime_ready
workflow                     = accepted
```

关闭目标不是 `player_complete`。不写新获得格，不撒 catalog `ItemEntity`。

---

## 0. 边界

只动已经活着的陶瓷坩埚 / 陶瓷模具，以及已 live 的石浇铸口在面对陶瓷坩埚时的 `fillMold`：

- `CrucibleBlock` / `CrucibleBlockEntity` / `CrucibleProcessCore.singleBlock()`
- `CeramicMoldBlock` / `CeramicMoldBlockEntity`
- `MteInPlaceBlockEntity` 的 `FAUCET`：邻接 `CruciblePour` 时走 `fillMoldAtSide`，否则仍倒 NeoForge 流体（保留 `stoneFaucetPoursIntoTankBelow`）

**不在本卡：**

- 85 个材质铸造 dummy 罐的 tick / `ITileEntityMold`
- 铲子刮 `OP.scrapGt`、雨水、酸、未知物品冒烟、生物化材质
- 大坩埚库存 `cast(form)` 端口层
- GT6 全量工具头模具图 / `COOL2CRYSTAL`

---

## 1. GameTest 合同

隔离命名空间 `cruciblecraft_wave_content_gt6_crucible_mold_interaction`。

| 测试 id | 必须看见 |
| --- | --- |
| `moldChiselORsBitAndCastsNuggets` | 凿子 OR 一个 5×5 格；未知掩码按粒浇 |
| `moldTopEdgePoursOnlyThatNeighbor` | 顶面边缘只浇那一侧坩埚 |
| `fillMoldConsumesOneIngot` | `fillMold` 正好消耗一锭 |
| `pincersPickupSkipsHeatDamage` | 钳子取凝固件不烫伤 |
| `emptyHandPickupCanBurn` | 空手取热件走温度伤害阈值 |
| `moldContactBurns` | 热模具接触烫伤 |
| `crucibleMeltdownBecomesLava` | 超温变成岩浆，不是直接拆方块 |
| `wrenchRotatesIngotRecipe` | 扳手转四次回到锭模 |
| `faucetPoursCeramicCrucibleIntoMold` | 石浇铸口对着陶瓷坩埚，下面模具被浇满 |
| `moldCuCoolsTemperature` | 模具吃 CU 降温 |

上一张校正卡的 8 个 GameTest 仍必须绿。

---

## 2. 余量（本卡不做）

1. 材质熔炼坩埚 / 模具 / 盆 / 交叉仍是 dummy 罐。
2. 浇铸口面对非坩埚时仍倒普通流体。
3. 铲子刮废料、雨水、酸、生物入锅。

---

## 3. 验收

- [x] 上表 10 个 GameTest
- [x] `MoldRecipes` JUnit
- [x] 本卡 python 门（`test_gt6_crucible_mold_interaction`）
- [x] 关闭目标 `runtime_ready`
