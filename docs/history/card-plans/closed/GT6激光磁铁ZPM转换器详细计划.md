# GT6 激光、磁铁与 ZPM 转换器详细计划

> 计划 slug：`energy/gt6-laser-magnet-zpm-converters`
> 状态：已关（`runtime_ready`，`workflow=accepted`）。本文件位于 `card-plans/closed/`。试玩未签。
> 正式名称：GT6 激光、磁铁与 ZPM 转换器
> 性质：把四个 GT6 能量转换类折进已有 `energy/converter-catalog` 框架。
> 总计划第 9 张落地卡，见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)。
> 涉及 LU / MU / QU：按 [能力交付流程 §8](../../../current/capability-delivery-workflow.md)
> 不走 prep 实施分支，直接落地后关闭。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = energy/gt6-laser-magnet-zpm-converters
unique_active_wave           = null
hosts                        = 16（激光 5、激光吸收器 5、电磁铁 5、ZPM 放电器 1）
landing_owned_paths          = energy_converter_kinds.json / energy_converter_tiers.json /
                               EnergyConverterProfiles.java / EnergyConverterCatalog.java /
                               DirectedWasteConverter* / ZpmDecharger* /
                               textures/block/energy/{laser_electric,laser_absorber,magnet_electric,zpm_decharger}/** /
                               src/test/.../gametest/LaserMagnetZpmGameTests.java
landing_depends_on           = energy/converter-catalog
partial_close_allowed        = true（激光气体与 ZPM 物品未映射，放电与激光合成保持 blocked）
survival_access              = partial
```

---

## 0. 开场判断

`EnergyType` 已有 LU、MU、QUANTUM，本卡不加枚举。
框架仍是 “kind JSON + tier JSON → `EnergyConverterProfile` → 宿主方块”。
四个目标类折进该目录后，档位行从 186 增到 202，kind 从 19 增到 23。

## 1. 分母

| GT6 类 | sourceId | 档位 | 收 → 发 | NBT_INPUT / OUTPUT | 备注 |
| --- | --- | --- | --- | --- | --- |
| `MultiTileEntityLaserElectric` | 10101–10105 | 一至五档 | EU → LU | 32/16 … 8192/4096 | `WASTE_ENERGY` |
| `MultiTileEntityLaserAbsorberElectric` | 10151–10155 | 一至五档 | LU → EU | 同档 | 背收前发 |
| `MultiTileEntityMagnetElectric` | 10031–10035 | 一至五档 | EU → MU | 同档 | 双极；`WASTE_ENERGY` |
| `MultiTileEntityZPMDechargerEU` | 11171 | 1 | QU → EU | V[7] = 131072 | 单格，只收 `IL.ZPM` |

注册行：本地 `Loader_MultiTileEntities.java` 865–869、930–934、976–980、1001。
11170 `MultiTileEntityZPMDechargerQU` 不在本卡。

## 1.1 获得格

| 机器 | 网格 | 结果 |
| --- | --- | --- |
| 激光 | ` L ` / ` W ` / `CMC` 及后续档的变体 | `IL.Comp_Laser_Gas_CO2` 未映射。格子记在 tier JSON，不发出配方 |
| 激光吸收器 | `SCW` / `SMP` / `SCW` | 已发出。电路是 `OD_CIRCUITS[档+1]`，电缆是 `CABLES_01[档]` |
| 电磁铁 | `CxC` / `CMC` / `CwC` | 已发出。`x` 剪线钳、`w` 扳手是催化剂，不消耗。加载器里没有电路 |
| ZPM 放电器 | `PCP` / `CMC` / `FCF` | 机器配方已发出。`IL.FIELD_GENERATORS[6]` 是已注册的 `compact_force_field_emitter_luv`（source 12106）。`IL.ZPM` 未映射，槽拒绝一切物品，不放电 |

激光气体见 [材料形态需求普查](../prep/材料形态需求普查详细计划.md)。没有用别的气体单元或别的电池顶格。

## 2. 实施

1. 四个 kind、16 个档位行进现有目录。`EXPECTED_SIZE` 为 179 + 7 + 16。
2. 激光、吸收器、电磁铁共用 `DirectedWasteConverterBlockEntity`：输出包大小是换算量、数量为 1；`WASTE_ENERGY` 在输出被挡住时仍清空输入上限。电磁铁正面发包、背面发负包。
3. ZPM 放电器是单独的单槽宿主。槽的 `isItemValid` 恒为 false。空槽不收 QU、不发 EU。
4. 贴图在 `textures/block/energy/`，清单 `gt6_laser_magnet_zpm_art_manifest.json`。模型用 front/back/side。

## 3. 验证与试玩

```powershell
python tools/verify.py integration --profile capability-runtime
.\gradlew.bat runGameTestServer -PgameTestGrid=energy "-PgameTestFilter=laser,absorber,electromagnet,zpm"
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_semantic_coverage.py --write
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

GameTest 在 `cruciblecraft_energy`，不进 `cruciblecraft_wave_`：提交的测试不属于 wave 命名空间。
2026-09-26 过滤跑过：激光废能、激光→吸收器→电热机、吸收器只收背面 LU、电磁铁→极化器 MU、ZPM 槽拒绝物品且不收 QU、吸收器/电磁铁/ZPM 配方在、激光配方不在。

人工 `runClient` 未签：电磁铁给极化器供 MU 跑一条配方；激光合成仍被 CO₂ 激光气体挡住。

## 4. 明确不接管

- QU Energizer、聚变、massfab
- 发明激光气体或用别的气体单元顶格
- `MultiTileEntityZPMDechargerQU`（QU → QU，决策暂缓）

## 5. 关闭清单

- [x] 16 台主机按 GT6 数值收发
- [x] LU / MU 消费端各有一个 GameTest（`cruciblecraft_energy`，2026-09-26）
- [x] 获得格 source-exact 或 `blocked`。激光整族不发配方；ZPM 机器可合成，放电 blocked
- [x] 贴图来自 GT6，清单齐
- [ ] 人工 `runClient` 签收
