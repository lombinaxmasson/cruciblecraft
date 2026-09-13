# 目录身份现代 id 详细计划

> 计划 slug：`registry/catalog-modern-ids`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。closing blocker 已修
> （existing_item 注册门禁 + meta 25302 Osmium）。
> 正式名称：目录身份现代 id
> 性质：把活注册里所有 `gt_mte/mte_<meta>` / `gt_multiitem/…_m<meta>` /
> `gt_block/…_m<meta>` / `gt_stone/…_m<meta>` / `gt_object/…_mN` 编号路径
> 改成现代语义路径。改名时散落物与活主机保持两个物品、不留 NeoForge alias；
> 这是本卡改名安全，不是产品终态。产品终态见
> [MTE 全量 Prep 总索引 §0.1](../prep/MTE全量Prep总索引.md)。
> 关闭目标：`runtime_ready`。不是 `player_complete`。
>
> Java/tick 源：本地 `gt6_code/gregtech6`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。不 fetch GitHub。
> 权威表：`tools/catalog_modern_id_map.json`。禁止再走 `slug_m{meta}` 公式。

```text
lane                         = closed
capability_slug              = registry/catalog-modern-ids
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = 空
```

## 0. 合同

- 改名时能对上的活主机也不合并。编号散落物只换现代 id，仍是独立 catalog 物品。
  这是本卡安全，不是「永远两件」的产品设计。
- 活着的 `slicer`、`tin/wire`、`tin/item_pipe`、`steel_dust_funnel`，以及材料门 /
  generation flags 注册的 `{材料}/fluid_pipe`、`{材料}/item_pipe`、`{材料}/wire`
  等活形态不动。`live_host_paths()` 必须占住这些路径；它只防改名抢 id，
  **不是**未来折回的对应关系权威。
- 撞号用家族前缀（`electric_wire/tin`、`item_pipe_tile/tin`、`fluid_pipe_tile/adamantium`），
  禁止抢活主机 id。
- 同名多行用 GT6 档位/变体消歧（`lv`/`mv`、砖/苔石、bale 轴），禁止再加 `_m20381`。
- `GT Multitileentity 0`、`GT bale crop m0` 禁止入库；对不上 Loader 就 blocked，不准退回 `mte_0`。
- 本卡接手的 live ledger 已是 `identity_only=1501` /
  `realized_natively=283` / `attachment_candidate=33`：相对 R0 关闭快照，
  sanding 4 与 tungsten-carbide oven/roaster 2 已被后续能力重分类。
  本卡不得再改 disposition / family / meta，只改 `registry_path` + `runtime_id`；
  关闭 R0 正文仍保留历史口径。
- 本卡只把铁锭 dummy **模型 JSON 跟新 path 搬走**，不从 `gregtech6_w` 导入真图，
  不 alias。铁锭不是可发行美术。
- 产品终态（由后续 runtime 收，见总索引 §0.1）不按名称或 R0 disposition
  猜：必须逐 meta 比较 Loader 注册、材料、class/spec、live item 与 live block。
  精确 live BlockItem 才完整折回；只有普通 item 时先解决 BlockItem 升级；
  像但不是或没有宿主则保留现代 id、原地实现并迁 GT6 图。
  禁止再开一张「只改名、永久双物品」的卡。
- 不宣称 `player_complete`。

## 1. 分母

活注册里凡是编号公式路径都在本卡内：熔炉 catalog MTE、Bath MTE 重叠行、semantic /
bath identity multiitem、gt_block、bath remainder 方块、gt_stone 变体、切片/清洗
操作数、温度计等同族硬编码。工具头 remainder 验收时断言为空。

不改已经是现代格式的 `aluminium/plate`、`cover_blank_cover`、`slicer`。

## 2. 验收

- 活注册 / catalog / `src/recipe_generated` 中零 `gt_mte/mte_<digits>`、`_m<digits>` 公式路径。
- 权威表行数 = 各 catalog 编号行并集；撞号全部有非编号消歧。
- `registry_kind=existing_item` 的每一行都必须真的解析到 runtime registry：
  活主机 **或** 另一份 catalog 的 `registry_kind=item` dummy。2026-09-12
  点名的 **24** 行（Steel/Galvanized Steel quadruple/nonuple fluid pipe 4，
  Lead/Gold 未注册线规 20）是 Bath 重叠：Bath 已按 `item` 注册 dummy，熔炉
  侧保持 `existing_item` 以免双注册。门禁用 `registered_holdable_paths()`，
  不只 `live_host_paths()`。
- meta `25302` 的 GT6 `MT.Os` 必须解析成 `osmium_elemental`，不得落到
  `item_pipe/germanium`。Germanium 的 OreDict 别名 `Osmium`（FakeOsmium = Ge）
  不得压过 `MT.Os.setLocal("Osmium")`。已迁到
  `item_pipe_tile/osmium_elemental`。同类门禁禁止材料 A 的 meta 借材料 B
  的路径消歧（`steel` 作为 `steel_galvanized` 的词素不算）。
- R0 current counts 保持 `1501/283/33`，本卡只迁 path，不再重算 disposition。
- 本卡只移动铁锭模型 JSON，不复制 `gregtech6_w`；折回/迁图由后续 runtime child 负责。
- `python tools/verify.py integration --profile capability-runtime` 绿。
- 后续 runtime 按总索引 §0.1 的逐 meta identity-resolution ledger 收双物品；
  本卡不实现折回或迁真图。
