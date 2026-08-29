# T36-Repair 工作日志

## 2026-08-28 · 签发与 R0 freeze

### 边界

- 不撤销 `T36_READY`，不占用 T44
- `owns_families=0`、`completion_delta=0`
- 不扩张 live 85 行，不签发真实第 86 台或第四档生存坩埚
- `unique_active_content_card=null`；本卡是唯一 active repair gate

### 已完成

- 计划从 `queued/` 移入 `active/`
- write-once `tools/t36_repair_pre_freeze.json`：T36/T43 hashes、85 台取得配方语义快照、装置数值
- 第二份名单 inventory：`tools/t36_repair_second_list_inventory.json`

### 结果

```text
T36-Repair issued
T43_READY preserved
live_variant_count = 85
unique_active_content_card = null
next_issue_id = T44
```

## 2026-08-28 · R1–R6 闭卡

### 数据权威

- `machine_kinds.json`：27 kind 默认取得模板与语言键（含 `sifter`）
- `machine_casings.json`：壳体 catalog；电解电缆 extras 在 EU 壳体行
- `device_materials.json`：坩埚 3 / 砧 4 / 锤 3
- `machine_acquisition.json`：opening 33 + 稀疏 template/casing 覆盖
- 稀疏覆盖：mixer/sluice/compressor/cutter/extruder/rollbender 的 opening 用
  `machine_generic`；`invar_smelter` / `invar_drying` / `invar_distillery` 用钢壳体

### 投影

- 加工机注册 Map + 历史常量薄别名；取得配方走 template adapter，删除 authored Set /
  `switch(kind)` / `casingFor` / 电缆线材 Java switch / lang 权威表 / `OPENING_VARIANT_IDS`
- `MachineMaterialRules` 与创造栏从 device catalog 投影；温度公式不变
- `CatalogTestSupport` 另建 Catalog 实例，不 mutation 生产静态 catalog

### Overlay 证明

- L1 `invar_lathe`、L2 `iron_machine_casing`、装置 L1 iron 坩埚，仅 test overlay
- `T36RepairExtensibilityTest`：fixture 不进生产 JSON / census / Java symbol

### 回归

- 85 台取得配方与 R0 快照语义相等
- `MachineMaterialRulesTest` 生产三材质数值不变
- `T36RuntimeEqualityTest` + 隔离 `cruciblecraft_t36` 8/8
- inventory `blocking_count=0`；`ModProcessingMachines` = `later:kind_behavior`

### census-replay

- `t36_repair_readiness` 接入 DAG / census-replay / builder policy / currentness
- T38 player-path recovery 与 T39 locked support 本地树与收据 behavior 哈希对齐：
  隔离 `-Pt38Recipes` 5/5、`-Pt39Recipes` 8/8 后 `--write --from-log` rebound
- `python tools/verify.py integration --profile census-replay` 退出 0

### 结果

```text
T36_REPAIR_READY
failed_gates = []
live_variant_count = 85
owns_families = 0
completion_delta = 0
t43_closing_gap = 3076
next_issue_id = T44
preassigned_host = false
unique_active_content_card = null
```
