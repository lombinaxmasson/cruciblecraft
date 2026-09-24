# GT6 余量能源设备详细计划

> 计划 slug：`energy/gt6-remainder-devices`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 余量能源设备
> 性质：五个行为各异的 GT6 能源设备，各自一个宿主类，合在一张卡里落地。
> 总计划第 10 张落地卡，见 [GT6 批量移植总计划](GT6批量移植总计划.md)。
> 涉及 LU 与多能量输出：不走 prep 实施分支，晋升后直接在 unique-active 上做。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep（仅计划；实施直接在 unique-active）
capability_slug              = energy/gt6-remainder-devices
unique_active_wave           = null
hosts                        = 33（太阳能 2、大电池箱 10、晶体充能器 10、大晶体充能器 10、魔法场吸收器 1）
landing_owned_paths          = energy/** 新宿主类 / ModBlocks / ModItems / ModBlockEntities / ModMenus /
                               mte_inplace_catalog.json（大电池箱）/ textures/block/energy/** /
                               src/test/.../gametest/
landing_depends_on           = 当前 unique-active 空窗；激光磁铁 ZPM 转换器卡先关（共用 LU 消费端）
partial_close_allowed        = true（按设备逐个关，未关的留 blocked）
```

---

## 0. 开场判断

覆盖页第 4 节还把 `MultiTileEntityReactorCore2x2` 记为 `denominator_only`，但 CC 已经有
`ReactorCoreBlock` 的 2x2 形态（`ModBlocks` 349–356）。那是覆盖页证据扫描漏报，
归 [翻译链与覆盖证据修复](GT6翻译链映射修复详细计划.md)，不在本卡。

剩下五个设备行为不同，没有共同框架可折，但每个都小。合一张卡是为了一次改完共享注册文件。

## 1. 分母

| GT6 类 | sourceId | 变体 | 行为 | CC 现状 |
| --- | --- | --- | --- | --- |
| `MultiTileEntitySolarPanelElectric` | 10050（Si，输出 8）、10051（Ge，输出 16） | 2 | 露天白天发 EU | 无；分母注“需要天气语义” |
| `MultiTileEntityBatteryBoxLarge` | 10090–10099 | 10 | 16 电池槽，发 EU | 小电池箱只是 MteInPlace 哑块 |
| `MultiTileEntityCrystalCharger` | 10130–10139 | 10 | 收 EU，给 4 槽晶体充 LU | 无 |
| `MultiTileEntityCrystalChargerLarge` | 10140–10149 | 10 | 同上，16 槽；配方以小充能器为 M | 无 |
| `MultiTileEntityMagicFieldAbsorber` | 10180 | 1 | 读上方方块，按类型发 KU/HU/LU/CU/QU/TU | 无 |

注册行：本地 `Loader_MultiTileEntities.java` 893–895、900–902、969–971、1005；
魔法场吸收器 tick 逻辑在其类 85–108 行。

## 1.1 获得格（D0）

开卡第一步逐台 `gt6_resolve`。已知依赖：大电池箱以小电池箱 + 更粗电缆 / 导线合成，
小电池箱当前是哑块，必须先让它能真正存取电池，否则大电池箱获得格与行为都 `blocked`。
魔法场吸收器依赖的“奖杯”方块若 CC 没有，整台 `blocked`。

## 2. 实施顺序

1. 太阳能板：天空可见 + 昼夜 / 天气判定，按 GT6 常量发 EU。
2. 电池箱：先把小电池箱从哑块改成真宿主，再加大电池箱 10 档。
3. 晶体充能器小 / 大：LU 充入 LU 晶体物品；没有 LU 物品就只做 EU 缓冲并 `blocked` 充能行为。
4. 魔法场吸收器：按 GT6 表读上方方块并发对应能量；读表缺项即 `blocked`。

## 3. 验证与试玩

```powershell
python tools/verify.py integration --profile capability-runtime
.\gradlew.bat runGameTestServer -PwaveRecipes=energy/gt6-remainder-devices
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
```

人工 `runClient`：太阳能板白天 / 夜晚 / 下雨三态；大电池箱装电池后给机器供电。

## 4. 明确不接管

- ReactorCore2x2（已 live，只修覆盖页证据）
- ZPM 放电器（激光磁铁 ZPM 转换器卡）
- Thaumcraft 视能量与任何 `out_of_scope` 能量身份

## 5. 关闭清单

- [ ] 每个设备 GameTest 覆盖发电 / 存储 / 充能主行为
- [ ] 小电池箱不再是哑块
- [ ] 获得格 source-exact 或 `blocked`
- [ ] 人工 `runClient` 签收
