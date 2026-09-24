# GT6 激光、磁铁与 ZPM 转换器详细计划

> 计划 slug：`energy/gt6-laser-magnet-zpm-converters`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 激光、磁铁与 ZPM 转换器
> 性质：把四个 GT6 能量转换类折进已有 `energy/converter-catalog` 框架。
> 总计划第 9 张落地卡，见 [GT6 批量移植总计划](GT6批量移植总计划.md)。
> 涉及 LU / MU / QU：按 [能力交付流程 §8](../../../current/capability-delivery-workflow.md)
> 不走 prep 实施分支，晋升后直接在 unique-active 上做。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep（仅计划；实施直接在 unique-active）
capability_slug              = energy/gt6-laser-magnet-zpm-converters
unique_active_wave           = null
hosts                        = 16（激光 5、激光吸收器 5、电磁铁 5、ZPM 放电器 1）
landing_owned_paths          = energy_converter_kinds.json / energy_converter_tiers.json /
                               EnergyConverterProfiles.java / EnergyConverterCatalog.java /
                               energy/converter/** / ModBlocks / ModItems / ModBlockEntities /
                               textures/block/energy/{laser,laser_absorber,magnet,zpm_decharger}/** /
                               src/test/.../gametest/
landing_depends_on           = 当前 unique-active 空窗
partial_close_allowed        = true（ZPM 放电器可因 ZPM 物品缺失单独留 blocked）
```

---

## 0. 开场判断

`EnergyType` 已有 LU（28–31 行）、MU（47–48 行）、QUANTUM（39–40 行），本卡不加枚举。
converter-catalog 现在覆盖 11 个 GT6 发电 / 电机类、186 个档位行，框架是
“kind JSON + tier JSON → `EnergyConverterProfile` → 宿主方块”。四个目标类在 GT6 里都继承
`TileEntityBase10EnergyConverter` / `TileEntityBase11Bipolar`，行为是“收 A 能量、按比例发 B 能量”，
适合加 kind 行而不是写新框架。

## 1. 分母

| GT6 类 | sourceId | 档位 | 收 → 发 | NBT_INPUT / OUTPUT | 备注 |
| --- | --- | --- | --- | --- | --- |
| `MultiTileEntityLaserElectric` | 10101–10105 | 一至五档 | EU → LU | 32/16 … 8192/4096 | `WASTE_ENERGY` |
| `MultiTileEntityLaserAbsorberElectric` | 10151–10155 | 一至五档 | LU → EU | 同档 | 背收前发 |
| `MultiTileEntityMagnetElectric` | 10031–10035 | 一至五档 | EU → MU | 同档 | 双极；`WASTE_ENERGY` |
| `MultiTileEntityZPMDechargerEU` | 11171 | 1 | QU → EU | V[7] | 有 ZPM 物品槽 |

注册行：本地 `Loader_MultiTileEntities.java` 865–869、929–934、976–980、1001。

## 1.1 获得格（D0）

| 机器 | 网格 | 关键格 |
| --- | --- | --- |
| 激光 | ` L ` / ` W ` / `CMC` | 外壳，CO₂ 激光气体单元，电路[档]，电缆[档] |
| 激光吸收器 | `SCW` / `SMP` / `SCW` | 硅宝石板，电缆，电路[档+1]，蓝宝石处理器 |
| 电磁铁 | `CxC` / `CMC` / `CwC` | 外壳，导线[档]，电路[档] |
| ZPM 放电器 | `PCP` / `CMC` / `FCF` | 电路[6]，蓝宝石处理器，力场发生器[6]，致密外壳 |

激光气体是已知非前缀缺口（见 [材料形态需求普查](材料形态需求普查详细计划.md) §2），
激光大概率整族 `blocked`。开卡第一步逐格 `gt6_resolve`，缺格不替代。

## 2. 实施

1. `energy_converter_kinds.json` 加 4 个 kind（`accepts` / `emits` / `gt6_class` / 面），
   `energy_converter_tiers.json` 加 16 行。
2. `EnergyConverterProfiles` 补 `WASTE_ENERGY` 与双极语义；ZPM 放电器补物品槽。
3. 每个 kind 至少一个 LU / MU 消费端 GameTest：激光 → 激光吸收器回环；电磁铁 → 已有极化器 / 磁选机。
4. 贴图从 GT6 迁，写清单。

## 3. 验证与试玩

```powershell
python tools/verify.py integration --profile capability-runtime
.\gradlew.bat runGameTestServer -PwaveRecipes=energy/gt6-laser-magnet-zpm-converters
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

人工 `runClient`：电磁铁给极化器供 MU 跑一条配方；有获得格的激光档做一次回环。

## 4. 明确不接管

- QU Energizer、聚变、massfab
- 发明激光气体或用别的气体单元顶格
- `MultiTileEntityZPMDechargerQU`（QU → QU，决策暂缓）

## 5. 关闭清单

- [ ] 16 台主机按 GT6 数值收发
- [ ] LU / MU 消费端各有一个 GameTest
- [ ] 获得格 source-exact 或 `blocked`
- [ ] 人工 `runClient` 签收
