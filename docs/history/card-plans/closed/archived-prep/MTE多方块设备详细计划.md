# MTE 多方块设备详细计划

> 计划 slug：`content/mte-multiblock`
> 状态：prep 已冻结（2026-09-11）；2026-09-12 修订物品重复与贴图终态，仍不落地 runtime。2026-09-20 已从 prep 归档到 `closed/archived-prep/`。对应 runtime child 已关；本文件是 2026-09-11 冻结合同，不是剩余待办。
> 性质：MTE 全量身份分母的家族 prep 卡，不是 runtime 实现卡。
> 本卡覆盖 `74` 个待规划身份；该家族在全量 catalog 中共有
> `74` 行，另有 `0` 行已标记 `realized_natively`，
> 不重复开行为实现。
> GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = content/mte-multiblock
unique_active_wave           = null
catalog_family               = multiblock
catalog_family_count         = 74
prep_row_count               = 74
dispositions                 = `identity_only`=74
owns_families                = 0
new_processing_machine       = 0
prep_owned_paths             = none at issuance; future implementation branch only
landing_owned_paths          = future runtime child only; none in this card
landing_depends_on           = unique-active closed; exact runtime host identities
                               and real GT6 source/art evidence frozen first
runtime_status               = not implemented by this prep card
```

权威分母是已关闭的
[`MTE 身份分母处置 R0`](../MTE身份分母处置R0详细计划.md) 与其
`disposition_ledger.json`；本卡不得修改 1,817 行的原始 disposition。
本卡身份范围以 R0 的 `disposition_ledger.json` 为准；本卡只冻结该
family + disposition 的范围，不复制第二份身份 ledger。

---

## 0. 边界

GT6 家族标签：MultiTileEntityCokeOven / Multiblock Machines, MultiTileEntityCrucible / Multiblock Machines, MultiTileEntityDistillationTower / Multiblock Machines, MultiTileEntityLargeBoiler / Multiblock Machines, MultiTileEntityLargeDynamo / Multiblock Machines, MultiTileEntityLargeTurbineSteam / Multiblock Machines, MultiTileEntityMultiBlockPart / Multiblock Machines, MultiTileEntityTank3x3x3Metal / Multiblock Machines, MultiTileEntityTank3x3x3Wood / Multiblock Machines, MultiTileEntityTank5x5x5Metal / Multiblock Machines。

本卡运行时边界：GT6 Multiblock Machines 的 MTE 身份。

当前判定：`身份目录中的 MTE 不是现有 named ProcessingMachine；不得用任意机器 casing、programmed_circuit 或单一主机覆盖`。现有同名多方块也不能
仅凭名称折回，必须证明 controller/part 的同一 Loader meta。

`identity_only` 表示当前只有可持有/散落身份，不是行为实现，也不是
玩家完成。`attachment_candidate` 表示已经点名真实行为缺口，不允许用
相邻方块、Cover、原版方块、错误材料或单一 programmed circuit 顶格。

后续卡约束：不从本卡启动电力多方块或新 EnergyType；多方块罐体/墙体仅保留后续 logistics runtime 规划入口，不能把身份目录当成已实现机器。

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

本家族逐 meta 对照现有 multiblock controller / part。若没有相同 source
identity，则在 dummy id 上原地替换（或进入后续 logistics runtime）并迁
GT6 源图；禁止用任意 casing 当身份或贴图宿主。

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
