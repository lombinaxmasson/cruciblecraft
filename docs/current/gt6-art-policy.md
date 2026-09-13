# GT6 贴图纪律

新方块 / 新物品 **不使用占位图**。有 GT6 原图就从本地
`gt6_referencable_port_code/gregtech6_w` 迁入
`src/main/resources/assets/cruciblecraft/textures/**/gt6_import/`
（能量机等已有家族目录如 `gt6_energy_art/` 可继续用）。
Java / tick 的源码权威仍是 `gt6_code/gregtech6`。不 fetch GitHub。

禁止：

- 把已有 CC 贴图当替身（`multiblock_casing`、能量/流体端口、`conveyor_cover`、
  `pipe_filter_cover`、原版铜块/熔炉面）
- 手绘一套「差不多」的替代图，而 `gt6_w` 里已有对应机器/盖板/零件
- 把 1.7.10 的 `gt.meta.*`、multiitem 数字文件名原样当作 CC 资源路径

落地：

- 目标路径用 CrucibleCraft registry id
- 每批拷贝写一份 art manifest（`source` / `gt6_source` / `destination`）
- 改模型时手写或只重跑该批 datagen，不要为几张 PNG 清掉整棵
  `src/generated/resources`

历史债（旧机器贴图错位、已进库的 multiitem 1.7.10 文件名）另开卡批量改，
不在别的 unique active 上顺手扩范围。

## MTE catalog 的去重与迁图

`CatalogNamedItem` 使用的 `minecraft:item/iron_ingot` 只允许作为身份迁移期
诊断占位，不能作为可发行美术。MTE runtime child 必须先按 Loader `meta`
完成物品去重，再决定是否迁图：

- 精确折回已注册 live BlockItem：删除 dummy 模型，不复制第二套 GT6 资源；
  使用 live host 已有 datagen / art。
- 只有普通材料 item、没有可放置 block：配方复用不算 MTE 美术或身份完成。
  先决定把 canonical item 原地升级为 BlockItem，还是保留独立 GT6 对象。
- keep-distinct / in-place：从 `gregtech6_w` 迁真实来源；同一现代 registry id
  上补齐 blockstate、block model、BlockItem model、world/inventory 纹理层和
  材质 tint，禁止另注册 `*_real`。

GT6 MTE 资源常是 kind/icon-set 共用底图 + 材质染色，并非每个 meta 一张 png。
art manifest 按真实复用粒度记录 source icon/folder、GT6 路径、CC 目标与 tint/
overlay 用法；不得为了“每个物品有一行”复制同一底图几百次。

已确认的来源根包括
`assets/gregtech/textures/blocks/materialicons/<set>/pipe*.png`、
`blocks/iconsets/pipe_restrictor.png`、`blocks/iconsets/insulation_*.png`，
以及 `assets/gregtech/textures/blocks/machines/<profile>/**`。目标使用 CC
registry family，放在 `textures/**/gt6_import/` 或现有专属家族目录。

Prep 计划只冻结上述合同，不复制 png。任何 runtime child 都要有门禁：
折回行新增 GT6 png 数为 0；保留/原地实现行不存在 iron-ingot 模型，且 manifest
中的每个来源都能在本地 `gregtech6_w` 解析。
