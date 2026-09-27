# PUV2+ / OMEGA 科技线

> 计划 slug：`content/puv-omega-tech-line`
> 状态：prep 分母已冻结。本文件位于 `card-plans/prep/`。
> 不是一张 unique-active 卡。按 capability 分波落地。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = prep then sequential landing
capability_slugs             = energy/large-heat-exchanger
                               energy/steam-turbine
                               energy/fusion-quantum
                               energy/quantum-massfab
                               content/puv-omega-parts
                               machines/puv-omega-matrix
unique_active_wave           = null
source_revision              = 3703e40308c8c030763fd6297dea8b210d2a77b1
```

## 硬口径

- 纵向：大型热交换器 17197 → 16 台普通蒸汽涡轮 + 4 台大型蒸汽涡轮 → 高阶材料 → 聚变 18 行 → CC_EXTENSION neutral matter → QU Energizer / Massfab → Neutronium → PUV2–PUV5 / OMEGA。
- `all_machine_variants`：显式行，禁止 `automaticKindTierCompletion`。
- 电压：GT6 `CS.VN` 已有 ULV–PUV5 与两档同名 XV。紧凑零件只占索引 0–14。OMEGA = `IL.*_OMEGA` = 源 `VN[14]=XV`。`VN[15]` 不注册第二套零件。
- PUV2+ 机器行使用 CC ID band `81000+`，不沿用 GT6 meta 算术。换号不是把这些机器算成 CC 发明。
- GT6 `MT.Neutronium` 是 `unused` 桩。18 行 Fusion 不输出 MatterNeutral。本项目按 `fusion_quantum_massfab` 增加一条标注 `CC_EXTENSION` 的 neutral matter bootstrap，再由 Quantum Energizer 做 LU→QU、Matter Fabricator 用 QU 压制真正的 Neutronium。禁止 `cosmic_neutronium` 别名。
- `FM.Plasma` 保持空。聚变 I/O 是气体/熔融同位素。
- 范围内 blocker 清零；crops / ASM / 家具 / RF-Flux 双胞胎不在验收范围。
- 禁止 stand-in 配方与贴图。

## 对象政策

| 对象 | 政策 |
| --- | --- |
| 17197 Large HEX、18101 Heat Transmitter、16 singles、17211–17214 | `source_backed` |
| Fusion 18 行 | `source_backed` |
| Fusion controller 获得格 | `source_backed`（`FIELD_GENERATORS[5]`） |
| neutral matter bootstrap | `CC_EXTENSION` |
| QE 10121–10125、Massfab 20411–20415 / 17199 | `source_backed` |
| QE/Massfab OMEGA 宿主 | `CC_EXTENSION` |
| Neutronium / Superconductor 材料 | `CC_EXTENSION` 生存链（源码为 unused / 无物品） |
| compact parts ULV–OMEGA | `source_backed`（`IL.*_ULV` … `IL.*_OMEGA`） |
| native 缺失 EU Basic Machine LV–IV | `source_backed` |
| PUV2–OMEGA 加工机/能源宿主 | `source_backed` 行为；注册号用 CC band `81000+` |

## 蒸汽涡轮分母

`STEAM_PER_EU = 2`。单机冷凝 `STEAM_PER_WATER = 200`，大型 `170`。网格 `"TwT","GSG","TMT"`。壳体材料 `Kinetic_T[1..4]` = Bronze / Steel / Ti / TungstenSteel。转子为行材料。

大型 3×3×4、35 面墙：17211 Magnalium + 18022；17212 Trinitanium + 18026；17213 Graphene + 18023；17214 Vibramantium + 18025。

## 大型 HEX 分母

`MultiTileEntityLargeHeatExchanger` 17197。`NBT_OUTPUT=16384` HU，packet size 1，8 个 18101 顶部分流。底 8×18024 + 控制器；顶 8×18101 + 中心 18024。配方 `"DDD","PMP","DDD"`。

`energy/large-heat-exchanger` 已关 `runtime_ready` / `accepted`，`survival_access=unreviewed`。底圈 18024 是输入舱，废液从主机底面出。蒸汽涡轮及后面几张仍停在 prep，没有开 unique-active。

## 不接管

- RF/Flux 110xx/111xx
- `energy/cooler` 独立 blocker
- Bath / ASM / ComputerCraft / crops
- `VN[15]` 第二套 XV 零件
