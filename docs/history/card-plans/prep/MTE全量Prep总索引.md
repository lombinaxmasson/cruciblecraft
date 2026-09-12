# MTE 全量 Prep 总索引

> 计划 slug：`content/mte-prep-index`
> 状态：prep 已冻结（2026-09-11）。本文件位于 `card-plans/prep/`。
> 性质：覆盖 GT6 MTE catalog 全部 1,817 行的规划索引，不是 runtime 卡。
> GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = content/mte-prep-index
unique_active_wave           = null
catalog_count                = 1817
prep_row_count               = 1540
realized_natively_count      = 277
owns_families                = 0
new_processing_machine       = 0
landing_owned_paths          = none; future runtime child only
runtime_status               = prep inventory only
```

## 0. 结论

旧的 `portfolio/mte-identity-disposition-r0` 已经冻结全量分母，
本索引不改它的 1,817 行 disposition。当前精确分解为：

- catalog 总数：`1817`
- 进入 12 张 prep 卡：`1540`
- 已由 CC 原生机制覆盖、仅保留审计记录：`277`
- 不创建 capability、不修改 `src/main`、不发布配方、不写 production lock

身份目录不是行为目录。`identity_only` 只能说明当前有一个可持有/散落
身份；`attachment_candidate` 只能说明后续需要真实 runtime。两者都不能
被本索引宣称为 `runtime_ready`。

## 1. 卡片分配

- `content/mte-connector`：MTE 连接件，`617` 行；Markdown-only prep
- `content/mte-decorative`：MTE 装饰件，`24` 行；Markdown-only prep
- `content/mte-drive`：MTE 传动件，`63` 行；Markdown-only prep
- `content/mte-energy-converter`：MTE 能源转换器，`79` 行；Markdown-only prep
- `content/mte-extender`：MTE Extender，`2` 行；Markdown-only prep
- `content/mte-fluid-attachments`：MTE 流体附件，`33` 行；Markdown-only prep
- `content/mte-furniture-storage`：MTE 家具储物，`563` 行；Markdown-only prep
- `content/mte-misc-tool`：MTE 杂项工具，`39` 行；Markdown-only prep
- `content/mte-multiblock`：MTE 多方块设备，`74` 行；Markdown-only prep
- `content/mte-processing-machine`：MTE 加工机身份，`42` 行；Markdown-only prep
- `content/mte-redstone-wire`：MTE 红石线，`3` 行；Markdown-only prep
- `content/mte-untyped`：MTE 未分类余量，`1` 行；Markdown-only prep

## 2. 例外与禁止误读

- 加工机身份中的 Polarizer 5 / Magnetic Separator 5 保持 MU/EnergyType blocked，不生成 runtime prep
- Roll Former 4 / Cluster Mill 4 已有 CC 主机，只能未来做身份折回，不得重复注册
- Squeezer 4 等作物依赖项等待 Crops；能源转换器不派生需要新EnergyType 的 Fusion / Quantum Energizer / Massfab / Boxinator
- 多方块电力设备不从本索引启动；罐体/墙体只保留后续 logistics 入口
- Sensors 21、Panels 348 和 printer/planet/Center 不属于这 1,817 行

## 3. 全量覆盖门

- [x] 所有 1,817 个 `registry_path` / `meta` 继续由 R0 ledger 权威保存；本索引不复制逐行身份
- [x] 12 张卡按 R0 `family_map` 的 family/disposition 分区，身份归属不重叠
- [x] 1,507 个 `identity_only` 与 33 个 `attachment_candidate`
      均进入对应 prep 卡
- [x] 277 个 `realized_natively` 保留在索引但不重复实现
- [x] 家族计数和 disposition 计数与 R0 ledger / family_map 对齐
- [x] 由 R0 disposition ledger、family_map 和 12 张 Markdown 卡共同冻结

## 4. 完成定义

这组卡的完成定义是 prep 规格冻结：来源、分母、所有权、依赖、
blocked/no-stand-in 边界和后续 runtime child 入口齐全。它不等于
`runtime_ready`，更不等于 `player_complete`。未来 runtime child
必须在真实身份、行为宿主、生存获得格和 GameTest 都明确后另开。

相关权威文件：

- [`disposition_ledger.json`](../../../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json)
- [`family_map.json`](../../../../tools/waves/portfolio/mte-identity-disposition-r0/family_map.json)
- [`feasibility.json`](../../../../tools/waves/portfolio/mte-identity-disposition-r0/feasibility.json)
