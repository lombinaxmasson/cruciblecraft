# MTE 连接件详细计划

> 计划 slug：`content/mte-connector`
> 状态：prep 已冻结（2026-09-11）；2026-09-12 修订物品重复与贴图终态，仍不落地 runtime。本文件位于 `card-plans/prep/`。
> 性质：MTE 全量身份分母的家族 prep 卡，不是 runtime 实现卡。
> 本卡覆盖 `617` 个待规划身份；该家族在全量 catalog 中共有
> `663` 行，另有 `46` 行已标记 `realized_natively`，
> 不重复开行为实现。
> GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = content/mte-connector
unique_active_wave           = null
catalog_family               = connector
catalog_family_count         = 663
prep_row_count               = 617
dispositions                 = `identity_only`=617
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

GT6 家族标签：MultiTileEntityPipeFluid / Fluid Pipes, MultiTileEntityPipeItem / Item Pipes, MultiTileEntityWireElectric / Electric Wires。

本卡运行时边界：Electric Wires、Fluid Pipes、Item Pipes 的 GT6 MTE 身份。

改名卡 keep-both 之后，活主机与 dummy 现在可能是两件物品：

- 活：`{材料}/{tiny|small|fluid_pipe|large|huge}_fluid_pipe`、
  对应 item pipe、ElectricalConductor BlockItem 或普通材料 wire item
- dummy：`fluid_pipe_tile/…`、`item_pipe_tile/…`、`electric_wire/…`（`CatalogNamedItem`，铁锭模型）

产品终态见总索引 §0.1，废止「永远不得折成 CC conductor / FluidPipe / ItemPipe」的永久双物品读法：

- PipeCatalog 中同材料、同 kind、同尺寸且 source meta 相同的活 BlockItem
  → `fold_live_block`，撤 dummy，不迁第二套图；
- quadruple/nonuple fluid pipe、restrictive item pipe、未注册线规等没有活规格
  → 在 dummy id 上原地实现并迁 `gregtech6_w`；
- ElectricalConductor 当前只有 `wireGt01` 与 `cableGt01/02/04/08/12`
  是可放置 block。`double_wire` 等普通材料 item 即使能接配方，也只能先记
  `upgrade_live_item`，不能直接撤掉可放置 MTE dummy；
- `content/electric-wire-cable-mte-fold` 的 259 mapped 是**配方映射集合**，
  不是 dummy 删除集合；还包含 catalog 外 id。

当前判定：`CatalogNamedItem 只有散落/展示身份，没有 GT6 管道网络、连接规则、覆盖层或方块实体行为；铁锭不是可发行美术`。

`identity_only` 表示当前只有可持有/散落身份，不是行为实现，也不是
玩家完成。`attachment_candidate` 表示已经点名真实行为缺口，不允许用
相邻方块、Cover、原版方块、错误材料或单一 programmed circuit 顶格。

后续卡约束：Any runtime child must be independently source-backed; this prep card does not authorize implementation.。

后续若要行为实现，必须另开 runtime child，逐个身份按总索引 §0.1 折回或迁图，并补源类、放置/交互合同、真实获得格和 GameTest；本 prep 卡不把展示身份折成行为，也不落地贴图。
GTCEu/GTM 不得覆盖 GT6 tick、网络隔离和 live/dummy 处置，已由已关闭的
[GT6 管道与线缆语义重基线](../closed/GT6管道与线缆语义重基线详细计划.md)
冻结；本卡仍只拥有 1,817 行里的 connector 身份，不签发该审计的 runtime child。

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

本家族必须用 `meta + material + kind/spec/size` 对 PipeCatalog /
ElectricalConductor 逐行连接。活 BlockItem 精确命中才撤 dummy；只有普通
材料 item 的行必须先解决 BlockItem 原地升级；未注册规格在 dummy id 上实现，
并迁 GT6 的共享 pipe/wire icon、insulation/restrictor 层与材质 tint。
不得把配方 mapped 集合直接当注册身份折回集合。

未来 runtime child 必须把折回或迁图写进那张卡的合同；本 prep 卡仍不落地。

---

## 3. Prep 完成条件

- [x] 该家族的 catalog 分母、GT6 标签和 disposition 已冻结
- [x] 待规划行有唯一 prep card 归属
- [x] 46 个 `realized_natively` 行已从本卡行为范围排除，但保留在总索引的逐 meta 去重审计
- [x] blocked / no-stand-in 边界已写明
- [x] 2026-09-12 逐 meta 物品去重与完整迁图合同已写入（见总索引 §0.1）
- [x] 由 R0 disposition ledger 与本卡 family/disposition 对照完成
- [ ] 另开 runtime child 后，重新回答真实行为、获得格、存档、折回或迁图、和测试

本卡完成意味着 **prep 规格完成**，不意味着 `runtime_ready` 或
`player_complete`。
