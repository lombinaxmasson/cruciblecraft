# MTE 加工机身份详细计划

> 计划 slug：`content/mte-processing-machine`
> 状态：prep 已冻结（2026-09-11）。本文件位于 `card-plans/prep/`。
> 性质：MTE 全量身份分母的家族 prep 卡，不是 runtime 实现卡。
> 本卡覆盖 `42` 个待规划身份；该家族在全量 catalog 中共有
> `86` 行，另有 `44` 行已标记 `realized_natively`，
> 不重复开行为实现。
> GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = content/mte-processing-machine
unique_active_wave           = null
catalog_family               = processing_machine
catalog_family_count         = 86
prep_row_count               = 42
dispositions                 = `identity_only`=42
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

当前判定：`现有具名加工机只覆盖已声明主机，不等于散落 MTE；缺主机、RecipeMap、能量和获得格时保持 identity_only`。

`identity_only` 表示当前只有可持有/散落身份，不是行为实现，也不是
玩家完成。`attachment_candidate` 表示已经点名真实行为缺口，不允许用
相邻方块、Cover、原版方块、错误材料或单一 programmed circuit 顶格。

后续卡约束：Polarizer 5 与 Magnetic Separator 5 属于 MU/EnergyType 冲突，不得开 runtime prep；Roll Former 4 与 Cluster Mill 4 已有 CC 主机，只允许未来做身份折回，不重复注册；Squeezer 4 等作物依赖项等待Crops 卡；其余缺主机身份也必须逐种补源合同后再开 child。

后续若要行为实现，必须另开 runtime child，逐个身份补源类、放置/交互合同、真实获得格和 GameTest；本 prep 卡不把展示身份折成行为。

---

## 1. 来源与覆盖

- [x] 固定 GT6 source revision `3703e40308c8c030763fd6297dea8b210d2a77b1`
- [x] 从 R0 disposition ledger 重放本卡行数与 family
- [x] 每个 `registry_path` / `meta` 只在全量索引中出现一次
- [x] 本卡没有 `capability.json`、live registry、recipe、production lock
- [x] 本卡没有把 catalog identity 变成行为主机

全量覆盖入口：[MTE 全量 Prep 总索引](MTE全量Prep总索引.md)。

---

## 2. 美术与实现门

本卡不新增方块/物品贴图，不复制占位图。未来 runtime child 如需新内容，
必须从本地 `gt6_referencable_port_code/gregtech6_w` 迁移真实来源并单独写
art manifest；现有 CC 方块、管道、容器、casing 和 vanilla 方块不得作为
身份等价替代。

---

## 3. Prep 完成条件

- [x] 该家族的 catalog 分母、GT6 标签和 disposition 已冻结
- [x] 待规划行有唯一 prep card 归属
- [x] `realized_natively` 行已从本卡实现范围排除并保留在总索引
- [x] blocked / no-stand-in 边界已写明
- [x] 由 R0 disposition ledger 与本卡 family/disposition 对照完成
- [ ] 另开 runtime child 后，重新回答真实行为、获得格、存档和测试

本卡完成意味着 **prep 规格完成**，不意味着 `runtime_ready` 或
`player_complete`。
