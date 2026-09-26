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
hosts                        = 17（激光 5、激光吸收器 5、电磁铁 5、ZPM 放电器 2）
landing_owned_paths          = energy_converter_kinds.json / energy_converter_tiers.json /
                               EnergyConverterProfiles.java / EnergyConverterCatalog.java /
                               DirectedWasteConverter* / ZpmDecharger* /
                               textures/block/energy/{laser_electric,laser_absorber,magnet_electric,zpm_decharger,zpm_decharger_qu}/** /
                               src/test/.../gametest/LaserMagnetZpmGameTests.java
landing_depends_on           = energy/converter-catalog
partial_close_allowed        = true（零点模块无配方，地牢图书馆尚未摆放；放电已接上）
survival_access              = partial
```

---

## 0. 开场判断

`EnergyType` 已有 LU、MU、QUANTUM，本卡不加枚举。
框架仍是 “kind JSON + tier JSON → `EnergyConverterProfile` → 宿主方块”。
五个目标类折进该目录后，档位行从 186 增到 203，kind 从 19 增到 24。11170 在关卡后按同一宿主补上。

## 1. 分母

| GT6 类 | sourceId | 档位 | 收 → 发 | NBT_INPUT / OUTPUT | 备注 |
| --- | --- | --- | --- | --- | --- |
| `MultiTileEntityLaserElectric` | 10101–10105 | 一至五档 | EU → LU | 32/16 … 8192/4096 | `WASTE_ENERGY` |
| `MultiTileEntityLaserAbsorberElectric` | 10151–10155 | 一至五档 | LU → EU | 同档 | 背收前发 |
| `MultiTileEntityMagnetElectric` | 10031–10035 | 一至五档 | EU → MU | 同档 | 双极；`WASTE_ENERGY` |
| `MultiTileEntityZPMDechargerEU` | 11171 | 1 | QU → EU | V[7] = 131072 | 单格，只收 `IL.ZPM` |
| `MultiTileEntityZPMDechargerQU` | 11170 | 1 | QU → QU | V[7] = 131072 | 同一槽与抽能；正面发 QU |

注册行：本地 `Loader_MultiTileEntities.java` 865–869、930–934、976–980、1000–1001。

## 1.1 获得格

| 机器 | 网格 | 结果 |
| --- | --- | --- |
| 激光 | ` L ` / ` W ` / `CMC` 及后续档的变体 | 已发出。`L` 是 `cruciblecraft:laser_gas_co2`（GT6 11008 二氧化碳激光发射器），由空发射器灌装二氧化碳气体得到；格子不直接吃二氧化碳流体 |
| 激光吸收器 | `SCW` / `SMP` / `SCW` | 已发出。电路是 `OD_CIRCUITS[档+1]`，电缆是 `CABLES_01[档]` |
| 电磁铁 | `CxC` / `CMC` / `CwC` | 已发出。`x` 剪线钳、`w` 扳手是催化剂，不消耗。加载器里没有电路 |
| 电动 ZPM 放电器 | `PCP` / `CMC` / `FCF` | 机器配方已发出。`P` 是蓝宝石晶体处理器。槽只收 `cruciblecraft:zero_point_module`（GT6 14999）。模块不能充电，放电器每秒从模块抽出 QU，正面发出 EU |
| 量子 ZPM 放电器 | `PCP` / `CMC` / `FCF` | 机器配方已发出。`P` 是红宝石晶体处理器。槽和抽能与电动那台相同，正面发出 QU |

氦、氖、氩、氪、氙、氦氖、一氧化碳、二氧化碳八支发射器都按 `MultiItemTechnological` 396–403 灌装：空发射器 + 对应气体 1000 mB，16 EU，128 tick。没有用别的气体单元顶格。

## 2. 实施

1. 五个 kind、17 个档位行进现有目录。`EXPECTED_SIZE` 为 179 + 7 + 17。
2. 激光、吸收器、电磁铁共用 `DirectedWasteConverterBlockEntity`：输出包大小是换算量、数量为 1；`WASTE_ENERGY` 在输出被挡住时仍清空输入上限。电磁铁正面发包、背面发负包。选择器盖板模式大于 0 时，输出和废电都按 `(16 - 模式) / 16` 缩小。
3. 两台 ZPM 放电器共用单槽宿主。槽只收零点模块。模块拒绝充电，所以外部 QU 仍然进不去；存量从模块里抽出后，电动那台正面发 EU，量子那台正面发 QU。
4. 贴图在 `textures/block/energy/`，清单 `gt6_laser_magnet_zpm_art_manifest.json`。模型用 front/back/side。

## 3. 验证与试玩

```powershell
python tools/verify.py integration --profile capability-runtime
.\gradlew.bat runGameTestServer -PgameTestGrid=energy "-PgameTestFilter=laser,absorber,electromagnet,zpm"
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_semantic_coverage.py --write
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

GameTest 在 `cruciblecraft_energy`，不进 `cruciblecraft_wave_`：提交的测试不属于 wave 命名空间。
2026-09-27 过滤跑过：激光废能、激光→吸收器→电热机、吸收器只收背面 LU、电磁铁→极化器 MU、满电零点模块被抽出 40 包 QU 并打出一包 EU、铁锭和外部 QU 仍被拒绝。量子放电器正面打出一包 QU、不打 EU。

人工 `runClient` 未签：电磁铁给极化器供 MU 跑一条配方。激光合成已不再被发射器挡住。

## 4. 明确不接管

- QU Energizer、聚变、massfab
- 发明激光气体或用别的气体单元顶格
- 给零点模块写配方，或把模块塞进简化地牢

## 5. 关闭清单

- [x] 17 台主机按 GT6 数值收发
- [x] LU / MU 消费端各有一个 GameTest（`cruciblecraft_energy`，2026-09-26）
- [x] 获得格 source-exact 或 `blocked`。五台电激光可合成；ZPM 机器可合成；零点模块无配方，CC 地牢还没摆图书馆里的模块，放电逻辑已接上
- [x] 贴图来自 GT6，清单齐
- [ ] 人工 `runClient` 签收
