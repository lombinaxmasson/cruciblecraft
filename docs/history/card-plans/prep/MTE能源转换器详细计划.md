# MTE 能源转换器详细计划

> 计划 slug：`content/mte-energy-converter`
> 状态：prep 已冻结（2026-09-11）；2026-09-12 修订物品重复与贴图终态，仍不落地 runtime。本文件位于 `card-plans/prep/`。
> 性质：MTE 全量身份分母的家族 prep 卡，不是 runtime 实现卡。
> 本卡覆盖 `79` 个待规划身份；该家族在全量 catalog 中共有
> `79` 行，另有 `0` 行已标记 `realized_natively`，
> 不重复开行为实现。
> GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = content/mte-energy-converter
unique_active_wave           = null
catalog_family               = energy_converter
catalog_family_count         = 79
prep_row_count               = 79
dispositions                 = `identity_only`=79
owns_families                = 0
new_processing_machine       = 0
prep_owned_paths             = none at issuance; future implementation branch only
landing_owned_paths          = future runtime child only; none in this card
landing_depends_on           = unique-active closed; exact runtime host identities
                               and real GT6 source/art evidence frozen first
runtime_status               = not implemented by this prep card
```

权威分母是已关闭的
[`MTE 身份分母处置 R0`](../closed/MTE身份分母处置R0详细计划.md) 与其
`disposition_ledger.json`；本卡不得修改 1,817 行的原始 disposition。
本卡身份范围以 R0 的 `disposition_ledger.json` 为准；本卡只冻结该
family + disposition 的范围，不复制第二份身份 ledger。

---

## 0. 边界

GT6 家族标签：MultiTileEntityBoilerTank / Steam Boilers, MultiTileEntityEngineSteam / Engines, MultiTileEntityGeneratorFluidBed / Burning Boxes, MultiTileEntityGeneratorGas / Burning Boxes, MultiTileEntityGeneratorLiquid / Burning Boxes, MultiTileEntityGeneratorMetal / Burning Boxes, MultiTileEntityHeaterElectric / Heaters, MultiTileEntityMotorLiquid / Engines, MultiTileEntityTurbineSteam / Turbines, unknown / Battery Boxes。

本卡运行时边界：Battery Boxes、Burning Boxes、Engines、Heaters、Steam Boilers、Turbines 等能源转换身份。

当前 79 行不能按 family 一刀切。`energy_converter_tiers.json.source_id`
与其中 **71** 个 meta 精确相交：boiler / strong boiler、steam engine /
strong steam engine、burning box、diesel engine 和 electric heater `10005`
已有同 Loader 行的 live BlockItem，应折回这些活 id，不迁第二套图。
其余 **8** 行是 Steam Turbine 6 与 Battery Box 2，没有 converter-catalog
宿主，继续在 dummy 现代 id 上原地实现并迁 GT6 源图。

`identity_only` 表示当前只有可持有/散落身份，不是行为实现，也不是
玩家完成。`attachment_candidate` 表示已经点名真实行为缺口，不允许用
相邻方块、Cover、原版方块、错误材料或单一 programmed circuit 顶格。

后续卡约束：本卡不派生 Fusion、Quantum Energizer、Massfab、Boxinator 或任何需要新 EnergyType 的 runtime child。Diesel Engine 8 行已由
`energy_converter_tiers.json` 的相同 `source_id` 覆盖，未来只做身份折回；
Turbine 6 / Battery Box 2 才是无宿主实现入口。

后续若要行为实现，必须另开 runtime child，逐个身份按总索引 §0.1 折回或迁图，并补源类、放置/交互合同、真实获得格和 GameTest；本 prep 卡不把展示身份折成行为，也不落地贴图。

---

## 1. 来源与覆盖

- [x] 固定 GT6 source revision `3703e40308c8c030763fd6297dea8b210d2a77b1`
- [x] 从 R0 disposition ledger 重放本卡行数与 family
- [x] 每个 `registry_path` / `meta` 只在全量索引中出现一次
- [x] 本卡没有 `capability.json`、live registry、recipe、production lock
- [x] 本卡没有把 catalog identity 变成行为主机

全量覆盖入口：[MTE 全量 Prep 总索引](MTE全量Prep总索引.md)。

---

## 2. 物品重复与美术

本卡不复制 png、不改 `src/main`。铁锭模型不是可发行美术。
R0 的 `identity_only` / `realized_natively` 不是物品去重结论。

产品终态服从 [MTE 全量 Prep 总索引 §0.1](MTE全量Prep总索引.md)。
未来 runtime child 必须逐 `meta` 记录 Loader 注册点、材料与规格、
live item、live block、处置、贴图和存档证据。只有同一 Loader meta +
同一材料/class/spec 且已有 live BlockItem 才能完整折回；配方映射到普通
item 不等于可放置 MTE 已折回。

本家族以 `energy_converter_tiers.json.source_id == meta` 为折回权威：
当前 71 行命中 live BlockItem，撤 dummy 并复用该能源转换机已经迁入的 GT6
模型/纹理；不得复制第二套。Turbine 6 / Battery Box 2 不命中，必须在 dummy
id 上实现真实方块并迁对应 GT6 art。仅名称相似的电池/锅炉仍不能充当宿主。

未来 runtime child 必须把折回或迁图写进那张卡的合同；本 prep 卡仍不落地。

---

## 3. Prep 完成条件

- [x] 该家族的 catalog 分母、GT6 标签和 disposition 已冻结
- [x] 待规划行有唯一 prep card 归属
- [x] `realized_natively` 行已从本卡实现范围排除并保留在总索引
- [x] blocked / no-stand-in 边界已写明
- [x] 2026-09-12 逐 meta 物品去重与完整迁图合同已写入（见总索引 §0.1）
- [x] 由 R0 disposition ledger 与本卡 family/disposition 对照完成
- [ ] 另开 runtime child 后，重新回答真实行为、获得格、存档、折回或迁图、和测试

本卡完成意味着 **prep 规格完成**，不意味着 `runtime_ready` 或
`player_complete`。
