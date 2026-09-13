# 目录身份现代 id 详细计划

> 计划 slug：`registry/catalog-modern-ids`
> 状态：unique-active `runtime_ready` / `workflow=active`。
> 正式名称：目录身份现代 id
> 性质：把活注册里所有 `gt_mte/mte_<meta>` / `gt_multiitem/…_m<meta>` /
> `gt_block/…_m<meta>` / `gt_stone/…_m<meta>` / `gt_object/…_mN` 编号路径
> 改成现代语义路径。散落物与活主机保持两个物品，不合并、不留 NeoForge alias。
> 关闭目标：`runtime_ready`。不是 `player_complete`。
>
> Java/tick 源：本地 `gt6_code/gregtech6`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。不 fetch GitHub。
> 权威表：`tools/catalog_modern_id_map.json`。禁止再走 `slug_m{meta}` 公式。

```text
lane                         = active
capability_slug              = registry/catalog-modern-ids
unique_active_wave           = registry/catalog-modern-ids
maturity                     = runtime_ready
workflow                     = active
depends_on                   = 空
```

## 0. 合同

- 能对上的活主机也不合并。编号散落物只换现代 id，仍是独立 catalog 物品/方块。
- 活着的 `slicer`、`tin/wire`、`tin/item_pipe`、`steel_dust_funnel` 不动。
- 撞号用家族前缀（`electric_wire/tin`、`item_pipe_tile/tin`），禁止抢活主机 id。
- 同名多行用 GT6 档位/变体消歧（`lv`/`mv`、砖/苔石、bale 轴），禁止再加 `_m20381`。
- `GT Multitileentity 0`、`GT bale crop m0` 禁止入库；对不上 Loader 就 blocked，不准退回 `mte_0`。
- R0 1817 行 disposition / family / meta 不动，只改 `registry_path` + `runtime_id`。
- 贴图/模型跟新 registry path 走，迁文件，不 alias。
- 不宣称 `player_complete`。

## 1. 分母

活注册里凡是编号公式路径都在本卡内：熔炉 catalog MTE、Bath MTE 重叠行、semantic /
bath identity multiitem、gt_block、bath remainder 方块、gt_stone 变体、切片/清洗
操作数、温度计等同族硬编码。工具头 remainder 验收时断言为空。

不改已经是现代格式的 `aluminium/plate`、`cover_blank_cover`、`slicer`。

## 2. 验收

- 活注册 / catalog / `src/recipe_generated` 中零 `gt_mte/mte_<digits>`、`_m<digits>` 公式路径。
- 权威表行数 = 各 catalog 编号行并集；撞号全部有非编号消歧。
- `python tools/verify.py integration --profile capability-runtime` 绿。
- 后续家具/红石线 runtime 仍面对「现代 id 的 dummy + 以后的真方块」两套对象；那是行为卡。
