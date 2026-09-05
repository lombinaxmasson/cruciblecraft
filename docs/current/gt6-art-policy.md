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
