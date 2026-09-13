# MTE 全量 Prep 总索引

> 计划 slug：`content/mte-prep-index`
> 状态：prep 已冻结（2026-09-11）；2026-09-12 二次审订物品重复与贴图终态，
> 仍不落地 runtime。
> 本文件位于 `card-plans/prep/`。
> 性质：覆盖 GT6 MTE catalog 全部 1,817 行的规划索引，不是 runtime 卡。
> GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = prep
capability_slug              = content/mte-prep-index
unique_active_wave           = null
catalog_count                = 1817
prep_row_count               = 1534
realized_natively_count      = 283
duplicate_resolution_count   = 1817
owns_families                = 0
new_processing_machine       = 0
landing_owned_paths          = none; future runtime child only
runtime_status               = prep inventory only
```

## 0. 结论

旧的 `portfolio/mte-identity-disposition-r0` 已经冻结全量分母，
本索引不改它的 1,817 行 disposition。当前精确分解为：

- catalog 总数：`1817`
- 进入 12 张 prep 卡：`1534`（`identity_only=1501` +
  `attachment_candidate=33`）
- `realized_natively` 审计行：`283`
- 不论 R0 disposition，全部 `1817` 行都必须有逐 meta 的物品去重与贴图处置
- 不创建 capability、不修改 `src/main`、不发布配方、不写 production lock

身份目录不是行为目录。`identity_only` 只能说明当前有一个可持有/散落
身份；`attachment_candidate` 只能说明后续需要真实 runtime。两者都不能
被本索引宣称为 `runtime_ready`。`realized_natively` 也只说明 CC 在该领域
已有机制，**不等于**该 meta 已和活物品合并，更不自动授权折回。

## 0.1 物品重复与贴图（2026-09-12）

`registry/catalog-modern-ids` 把编号散落物改成现代 id，**改名时不合并活主机**。
所以现在会同时存在：

- 活形态：`{材料}/{pipe_form}`、ElectricalConductor、具名加工机、
  能源转换机、Hopper、核棒等；
- dummy：`fluid_pipe_tile/…`、`item_pipe_tile/…`、`electric_wire/…`、`lead/chest` 等
  `CatalogNamedItem`，物品模型是铁锭占位。

这不是产品终态。JEI 双物品是债。以后每张 runtime child 必须先按 `meta`
写 identity-resolution ledger，至少记录：

`meta`、GT6 Loader 注册点、class/tag、材料与 `OP.*` 规格、当前
`registry_path/kind`、候选 live item id、候选 live block id、处置、贴图策略、
存档策略和证据。英文名相同、同 family 或 `realized_natively` 都不能代替这张表。

逐 meta 只能作以下处置：

1. **already_shared**：catalog 已是 `existing_item`，且目标经 runtime registry
   验证为真的活 item / BlockItem。没有 dummy 可撤，也不迁第二套图。
2. **fold_live_block**：同一 Loader meta、材料、class/kind 和规格/尺寸都相同，
   且 CC 已注册同一可放置 BlockItem。catalog/散落/配方改指活 id，撤 dummy，
   删除铁锭模型；使用活对象现有 datagen / art。
3. **upgrade_live_item**：配方可复用同 meta 的普通材料 item，但 GT6 MTE 是可放置
   对象、CC 尚无对应 block。配方复用不等于身份已折回；runtime 必须把该 canonical
   item 原地升级为 BlockItem，或以源证据判为不同对象。禁止直接删 dummy 后丢失放置语义。
4. **keep_distinct / in_place**：CC 只有像但不是的机制，或根本没有活宿主。
   dummy 的现代 id 留给 GT6 对象，并在该 id 上把 `CatalogNamedItem` 原地替换成
   真 Block/BlockItem；禁止再注册 `*_real`。

第 4 类以及第 3 类最终判为不同对象时，必须从本地
`gt6_referencable_port_code/gregtech6_w` 迁真实资源，写 art manifest。
迁图合同同时覆盖 blockstate、block model、BlockItem model、纹理层、材质 tint
和 inventory/world render；不是只复制一张 item png。禁止 alias 活管、容器、
casing、原版方块。

已核到的 source selector 也必须进入 runtime 子卡，不能只写「以后迁图」：

- 管/线：`assets/gregtech/textures/blocks/materialicons/<set>/pipe*.png`
  与 `*_overlay.png`，以及 `blocks/iconsets/pipe_restrictor.png`、
  `insulation_{tiny|small|medium|large|huge}.png`；目标放进 CC
  `textures/block/**/gt6_import/`（或既有 family 目录），由模型 `tintindex`
  复原材料色；
- 机器/储物/传动：按 Loader 的 `NBT_TEXTURE` / source profile 解析
  `assets/gregtech/textures/blocks/machines/<profile>/**`，目标以最终 CC
  registry family 命名，禁止沿用 `gt.meta.*` 数字名；
- art manifest 至少写 `source`、`gt6_source`、`destination`、profile/tint/
  overlay、对应 runtime id 与 source hash。同一 icon-set 共用底图，不按 meta
  复制几百份。

补充约束：

- `content/electric-wire-cable-mte-fold` 只折了 **配方操作数**（259 mapped）。
  mapped 中还包含 catalog 外 id 和只有普通 item、没有 BlockItem 的规格。
  连接件 runtime 必须逐 meta 走上面的 1–4 类，不能机械撤掉全部 mapped dummy。
- 本索引与 12 张家族卡 **仍然不复制 png、不改 `src/main`**。贴图迁移是 runtime child 的交付物，不是 prep 已做完。
- 旧句「不得把 Fluid Pipes 折成 CC FluidPipe」废止其「永久双物品」读法：那是禁止用活管冒充 **尚未实现** 的 GT6 行为（覆盖层、未注册规格）。对得上的规格必须折回。
- 存档：改名卡不留 NeoForge alias。折回 dummy 时旧 dummy 堆可以变成空气或做一次明确物品替换；必须写在那张 runtime 卡的存档合同里，不得偷偷 alias。
- 门禁：`registry_kind=existing_item` 必须真的存在于 runtime registry；
  `live_host_paths()` 只防止改名抢 id，不是物品折回权威。

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
- `content/mte-processing-machine`：MTE 加工机身份，`36` 行；Markdown-only prep
- `content/mte-redstone-wire`：MTE 红石线，`3` 行；已关 `runtime_ready`，`upgrade_live_item` 折到活 `*/wire`
- `content/mte-untyped`：MTE 未分类余量，`1` 行；Markdown-only prep

### 1.1 未进 12 张卡的 283 行

这些行不能因 `realized_natively` 从物品去重审计中消失：

- connector `46`：先区分真 `already_shared`、错误 `existing_item` 与仍存 dummy；
- hopper `101`：逐 meta 对照 T30 的 `<material>_hopper` /
  `<material>_queue_hopper` 和 `steel_dust_funnel`，精确命中才折回；
- crucible_foundry `85`：陶瓷坩埚/模具行为不能自动吞掉材料化 GT6
  Smeltery/Mold；默认 keep_distinct/in_place，除非证明同一 Loader meta；
- processing_machine `50`：以 `machine_tiers.json.sourceId` 精确匹配，
  不能靠 kind 名或 `realized_natively`；
- reactor `1`：meta `9203` 应对照 live `neutron_reflector_rod` 折回。

本索引只冻结这些入口，不签发新的 unique-active。

### 1.2 跨域连接件基础设施 prep 卡（不新增分母）

另有一张不占 1,817 行、不拥有 connector family 行数的跨域审计卡：

- `content/gt6-pipe-cable-baseline` — [GT6 管道与线缆语义重基线详细计划](../closed/GT6管道与线缆语义重基线详细计划.md)
  （2026-09-13 **prep 审计已关闭**；不创建 capability。后续流体/物品/EU/红石
  行为必须另开 child，不得再把 GTCEu 合同当成 GT6。）

这张卡已经完成 provenance / 身份 / 行为缺口 / 隔离 / 贴图 selector 审计，
不改变 R0 disposition，不创建 capability，不替代 `MTE 连接件` 的身份 prep，
也不直接执行 runtime。

## 2. 例外与禁止误读

- 加工机身份中的 Polarizer 5 / Magnetic Separator 5 保持 MU/EnergyType blocked，不生成 runtime prep
- Rolling Mill 4 / Roll Former 4 / Cluster Mill 4 与 Dryer `20314`
  已有相同 `sourceId` 的 CC 主机，只能未来做身份折回，不得重复注册
- Squeezer 4 等作物依赖项等待 Crops；能源转换器不派生需要新EnergyType 的 Fusion / Quantum Energizer / Massfab / Boxinator
- 多方块电力设备不从本索引启动；罐体/墙体只保留后续 logistics 入口
- Sensors 21、Panels 348 和 printer/planet/Center 不属于这 1,817 行

## 3. 全量覆盖门

- [x] 所有 1,817 个 `registry_path` / `meta` 继续由 R0 ledger 权威保存；本索引不复制逐行身份
- [x] 12 张卡按 R0 `family_map` 的 family/disposition 分区，身份归属不重叠
- [x] 1,501 个 `identity_only` 与 33 个 `attachment_candidate`
      均进入对应 prep 卡
- [x] 283 个 `realized_natively` 保留在索引，并进入逐 meta 去重审计
- [x] 家族计数和 disposition 计数与 R0 ledger / family_map 对齐
- [x] 由 R0 disposition ledger、family_map 和 12 张 Markdown 卡共同冻结
- [x] 物品重复与贴图终态写入 §0.1（2026-09-12）

## 4. 完成定义

这组卡的完成定义是 prep 规格冻结：来源、分母、所有权、依赖、
blocked/no-stand-in 边界、**物品重复/贴图终态（§0.1）** 和后续 runtime
child 入口齐全。它不等于 `runtime_ready`，更不等于 `player_complete`。
未来 runtime child 必须在真实身份、行为宿主、生存获得格、GameTest、
以及 §0.1 的折回或迁图合同都明确后另开。

相关权威文件：

- [`disposition_ledger.json`](../../../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json)
- [`family_map.json`](../../../../tools/waves/portfolio/mte-identity-disposition-r0/family_map.json)
- [`feasibility.json`](../../../../tools/waves/portfolio/mte-identity-disposition-r0/feasibility.json)
- [`catalog_modern_id_map.json`](../../../../tools/catalog_modern_id_map.json)
- [GT6 贴图纪律](../../../current/gt6-art-policy.md)
