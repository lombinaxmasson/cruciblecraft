# MTE 加工机身份详细计划

> 计划 slug：`content/mte-processing-machine`
> 状态：prep 已冻结（2026-09-11）；2026-09-12 修订物品重复与贴图终态，仍不落地 runtime。本文件位于 `card-plans/prep/`。
> 性质：MTE 全量身份分母的家族 prep 卡，不是 runtime 实现卡。
> 本卡覆盖 `36` 个待规划身份；该家族在全量 catalog 中共有
> `86` 行，另有 `50` 行已标记 `realized_natively`，
> 不重复开行为实现。
> GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = content/mte-processing-machine
unique_active_wave           = null
catalog_family               = processing_machine
catalog_family_count         = 86
prep_row_count               = 36
dispositions                 = `identity_only`=36
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

GT6 家族标签：MultiTileEntityAutoToolHammer / Automatic Tools, MultiTileEntityBasicMachine / Basic Machines。

本卡运行时边界：GT6 Basic Machines 与 Automatic Tools 的未覆盖身份。

当前 ledger 是 `identity_only=36` / `realized_natively=50`。这仍不是物品
折回分类。按 `machine_tiers.json.sourceId == meta`，当前 **58** 行有精确
live BlockItem（45 个 `realized_natively` + 13 个 `identity_only`）；
其余 28 行不能因名称或 R0 disposition 自动折回。

`identity_only` 表示当前只有可持有/散落身份，不是行为实现，也不是
玩家完成。`attachment_candidate` 表示已经点名真实行为缺口，不允许用
相邻方块、Cover、原版方块、错误材料或单一 programmed circuit 顶格。

后续卡约束：

- Rolling Mill 4（20111–20114）、Roll Former 4（20131–20134）、
  Cluster Mill 4（20141–20144）与 Dryer 20314 虽仍是 `identity_only`，
  但已有相同 `sourceId` 的 CC 主机，只允许折回，不重复注册、不迁第二套图；
- sanding 4 与 tungsten-carbide oven / roaster 2 已使 live ledger 从旧快照
  `42/44` 变为 `36/50`；
- Polarizer 5 与 Magnetic Separator 5 属于 MU/EnergyType 冲突；
  Squeezer 4 等作物依赖项等待 Crops；
- 无精确 `sourceId` 的 Automatic Hammer、Laser Engraver 等，在 dummy id
  上原地实现并迁 GT6 源图。`realized_natively` 但无相同 sourceId 的
  Laser Welder 5 也不能折到不相干的 `welder`。

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

本家族只认 `machine_tiers.json.sourceId == meta` 的 live BlockItem：
58 行精确命中时折回并复用现有机器 art；不命中的 28 行继续逐项判
keep_distinct / in_place，并按 Loader `NBT_TEXTURE` 目录迁 blockstate、
world model、BlockItem model、纹理层和材质 tint，不能只换 item 图。

未来 runtime child 必须把折回或迁图写进那张卡的合同；本 prep 卡仍不落地。

---

## 3. Prep 完成条件

- [x] 该家族的 catalog 分母、GT6 标签和 disposition 已冻结
- [x] 待规划行有唯一 prep card 归属
- [x] 50 个 `realized_natively` 行已从本卡行为范围排除，但保留在总索引的逐 meta 去重审计
- [x] blocked / no-stand-in 边界已写明
- [x] 2026-09-12 逐 meta 物品去重与完整迁图合同已写入（见总索引 §0.1）
- [x] 由 R0 disposition ledger 与本卡 family/disposition 对照完成
- [ ] 另开 runtime child 后，重新回答真实行为、获得格、存档、折回或迁图、和测试

本卡完成意味着 **prep 规格完成**，不意味着 `runtime_ready` 或
`player_complete`。
