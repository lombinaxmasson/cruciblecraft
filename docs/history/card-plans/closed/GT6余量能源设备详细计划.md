# GT6 余量能源设备详细计划

> 计划 slug：`energy/gt6-remainder-devices`
> 状态：已关（`runtime_ready`，`workflow=accepted`）。本文件位于 `card-plans/closed/`。试玩未签。
> 正式名称：GT6 余量能源设备
> 性质：五个行为各异的 GT6 能源设备，各自一个宿主类，合在一张卡里落地。
> 总计划第 10 张落地卡，见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)。
> 涉及 LU 与多能量输出：不走 prep 实施分支，直接在 unique-active 上做完后关闭。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = energy/gt6-remainder-devices
unique_active_wave           = null
hosts                        = 43（太阳能 2、小电池箱 10、大电池箱 10、晶体充能器 10、大晶体充能器 10、魔法场吸收器 1）
landing_owned_paths          = energy/remainder/** / ModBlocks / ModItems / ModBlockEntities / ModMenus /
                               MteInPlaceKind / MteInPlaceBlockEntity /
                               mte_inplace_catalog.json（LuV、ZPM 小电池箱）/
                               textures/block/energy/{solar_panel,battery_box,battery_box_large,crystal_charger,crystal_charger_large,magic_absorber}/** /
                               src/test/.../gametest/RemainderDeviceGameTests.java
landing_depends_on           = energy/batteries、energy/transformers、energy/gt6-laser-magnet-zpm-converters
partial_close_allowed        = true（原始电路、大电池箱 PUV1、魔法电路与暮色奖杯未发明）
survival_access              = partial
```

---

## 0. 开场判断

覆盖页第 4 节还把 `MultiTileEntityReactorCore2x2` 记为 `denominator_only`，但 CC 已经有
`ReactorCoreBlock` 的 2x2 形态（`ModBlocks` 349–356）。那是覆盖页证据扫描漏报，
归 [翻译链与覆盖证据修复](../prep/GT6翻译链映射修复详细计划.md)，不在本卡。

剩下五个设备行为不同，没有共同框架可折，但每个都小。合一张卡是为了一次改完共享注册文件。

## 1. 分母

| GT6 类 | sourceId | 变体 | 行为 | CC 现状 |
| --- | --- | --- | --- | --- |
| `MultiTileEntitySolarPanelElectric` | 10050（Si，输出 8）、10051（Ge，输出 16） | 2 | 露天白天发 EU | 无；分母注“需要天气语义” |
| `MultiTileEntityBatteryBoxLarge` | 10090–10099 | 10 | 16 电池槽，发 EU | 小电池箱只是 MteInPlace 哑块 |
| `MultiTileEntityCrystalCharger` | 10130–10139 | 10 | 只写 `NBT_ENERGY_EMITTED=LU`，收发都是 LU，4 槽 | 无 |
| `MultiTileEntityCrystalChargerLarge` | 10140–10149 | 10 | 同上，16 槽；配方以小充能器为 M | 无 |
| `MultiTileEntityMagicFieldAbsorber` | 10180 | 1 | 读上方方块，按类型发 KU/HU/LU/CU/QU/TU | 无 |

注册行：本地 `Loader_MultiTileEntities.java` 893–895、900–902、969–971、1005；
魔法场吸收器 tick 逻辑在其类 85–108 行。

## 1.1 获得格（D0）

开卡时按源码重读获得格。大电池箱中心是同档变压器 `getItem(10040+tier)`，不是小电池箱。
tier 9 没有变压器 10049，配方不发。ULV 用 `OD_CIRCUITS[0]` 原始电路，配方不发。
魔法场吸收器的 `IL.Circuit_Magic` 没有映射，配方不发；暮色奖杯不存在，龙蛋和原版头颅仍可读。

## 2. 实施顺序

1. 太阳能板：天空可见 + 昼夜 / 天气判定，按 GT6 常量发 EU。
2. 电池箱：先把小电池箱从哑块改成真宿主，再加大电池箱 10 档。
3. 晶体充能器小 / 大：LU 充入 LU 晶体物品；没有 LU 物品就只做 EU 缓冲并 `blocked` 充能行为。
4. 魔法场吸收器：按 GT6 表读上方方块并发对应能量；读表缺项即 `blocked`。

## 3. 验证与试玩

```powershell
python tools/verify.py integration --profile capability-runtime
.\gradlew.bat runGameTestServer -PgameTestGrid=energy
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_semantic_coverage.py --write
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

人工 `runClient` 未签：太阳能板白天 / 夜晚 / 下雨三态；大电池箱装电池后给机器供电。

## 4. 明确不接管

- ReactorCore2x2（已 live，只修覆盖页证据）
- ZPM 放电器（激光磁铁 ZPM 转换器卡）
- Thaumcraft 视能量与任何 `out_of_scope` 能量身份

## 5. 关闭清单

- [x] 每个设备 GameTest 覆盖发电 / 存储 / 充能主行为（`cruciblecraft_energy`，2026-09-27）
- [x] 小电池箱不再是哑块。LuV / ZPM 仍是 `boxwood/battery_luv` 与 `boxwood/battery_zpm`，四槽 EU 箱；其余八档是新方块
- [x] 获得格 source-exact 或未发配方。ULV 四台因原始电路未发配方；大电池箱 PUV1 因没有变压器 10049 未发配方；魔法场吸收器因 `IL.Circuit_Magic` 未发配方。暮色森林奖杯不存在，吸收器仍读龙蛋（QU）和原版头颅（TU）
- [x] 贴图来自 GT6，清单 `gt6_remainder_devices_art_manifest.json`
- [ ] 人工 `runClient` 签收

## 6. 落地记录

宿主 43。新方块 41。大电池箱配方中心是同档变压器 `10040+tier`，不是小电池箱。晶体充能器按源码收发 LU。选择器盖板没有接上，模式字段默认 0。
