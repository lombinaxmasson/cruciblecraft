# Sensors 详细计划

> 计划 slug：`content/sensors`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-12）。本文件位于 `card-plans/closed/`。
> 正式名称：Sensors
> 性质：GT6 `Loader_MultiTileEntities.sensors()` 21 个 Sensor MTE
> （31000–31023，空号 31008 / 31009 / 31014）。不是 RecipeMap 卡。
> `compact_sensor_*` 是 `IL.SENSORS` 零件，不是本卡分母。
> 关闭目标：`runtime_ready`。不是 `player_complete`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。
> 父冻结：exclusion-reclaim R0 Sensors `requires_new_runtime`。
> Grindstone `32703` 不在本卡。

```text
lane                         = closed
capability_slug              = content/sensors
unique_active_wave           = null
dump_map                     = none
hosts                        = 21（31000–31023，缺 31008/31009/31014）
energy                       = 无 RU/EU 消耗；弱红石 0–15
covers                       = 禁止（非管道，盖板装不上）
prep_owned_paths             = 无独立 prep 波
landing_owned_paths           = SensorKind / SensorBlock / SensorBlockEntity；
                               ModBlocks / ModBlockEntities / ModItems；
                               ModRecipeProvider D0；SensorGameTests；
                               tools/waves/content/sensors/**
landing_depends_on           = unique-active 关闭后为空；熔炉已关 runtime_ready
```

来源：`Loader_MultiTileEntities.java` 1979–1999。
基类：`gregapi.tileentity.machines.MultiTileEntitySensorTE`。
贴图：`textures/blocks/machines/redstone/sensors/<name>/{colored,overlay}/{front,back,side}`。

---

## 0. 开场判断

熔炉已关 `runtime_ready`。本卡分母是 21 个 Sensor 方块身份，不是零件、
不是 Panels 348、不是装饰 MTE、不是红石线。
`IL.Electro_Meter` / `IL.Tacho_Meter` 已按 GT6 格注册。Gibbl / 质量 / 转速
走真实宿主，不得用比较器、现有 cover、`compact_sensor_*` 或其他物品顶槽。

合同：邻接检测（TPS / 玩家计数除外）、8 种比较模式、弱红石 0–15、禁止盖板。
ComputerCraft `ITileEntityComputerizable` 本卡 blocked。

---

## 1. 分母

| sourceId | CC path | 检测 | D0 | 行为 |
| --- | --- | --- | --- | --- |
| 31000 | thermometer | 温度 K | source_exact（水银温度计） | live：坩埚/模具 °C→K |
| 31001 | gibblometer | 压力 | source_exact（ANY.SiO2 gem → glass/gem） | live：流体管 / 加工机输入罐 / 锅炉蒸汽 / 坩埚 OM.total |
| 31002 | luminometer | 亮度 | source_exact | live |
| 31003 | chronometer | 昼夜 | source_exact | live |
| 31004 | itemometer | 物品数 | source_exact | live |
| 31005 | stackometer | 组数 | source_exact | live |
| 31006 | fluidometer | mB | source_exact | live |
| 31007 | bucketometer | 桶 | source_exact | live |
| 31010–31013 | *_weightometer | 质量 | source_exact | live：库存 OM.weight / 坩埚内容 |
| 31015 | electrometer | EU 流量 | source_exact（IL.Electro_Meter m10003） | live：EU 电缆 wattageLast |
| 31016 | tps_meter | TPS×100 | source_exact | live |
| 31017 | player_counter | 在线玩家 | source_exact | live |
| 31018 | progress_meter | 进度 | source_exact（brass small_gear） | live：加工机 / 焦炉 |
| 31019 | tachometer | RU | source_exact（IL.Tacho_Meter m10004） | live：轴 / 齿轮箱 transferredLast |
| 31020 | geiger_counter | 中子 | source_exact（装满盖革 + 铅双板） | live：堆芯 neutronSum |
| 31021 | laserometer | LU | source_exact（compact_sensor_lv 作配料） | live：LU 光纤 wattageLast |
| 31022 | kilo_bucketometer | 千桶 | source_exact | live |
| 31023 | kilo_gibblometer | 千吉伯 | source_exact（diamantine/gem） | live：Gibbl / 1e6 |

共享格：锡合金双板/螺栓、红合金细线、红石、玻璃、比较器。

---

## 2. 明确不在本卡

- `compact_sensor_*` 零件本身
- Panels / 装饰 MTE / 红石线
- ComputerCraft 外设
- 物流 / 封面网
- Grindstone `32703`
- 用 HU 冒充 Kelvin、用盖板压力冒充 Gibbl、用转速存量冒充 `mTransferredLast`

---

## 3. 关闭清单

- [x] 21 个身份独立 registry / runtime id
- [x] 真实宿主接入（Gibbl / 质量 / 转速不再 fail-closed）
- [x] Electro_Meter / Tacho_Meter 已注册并发 D0
- [x] 来源贴图 + art manifest
- [x] GameTest：注册、D0、朝向、弱红石、活宿主非零
- [x] 本文件在 closed；不得宣称 `player_complete`
