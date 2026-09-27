# 严禁：目录世界生成当获得路径

把 catalog 物品以掉落物撒在主世界上，或把整本 stone / block-object catalog
倒在 dirt/sand 上，**不是** GT6，也**不是**生存获得。禁止再做、再扩、再写成
完成档。

Closed remainder / MTE / tiny-washed catalog cards once used empty-input
overworld scatter：整本 remainder / MTE / food tag 丢到 dirt/sand 上（约
1/128），GameTest 再捡一个。那是关卡替身。GT6 地表散落是 `WorldgenRocks`
小**石块**（本仓库 `SurfaceRockFeature`），不是满地机器、食物、木螺栓。

那八条假 scatter（六条 ItemEntity + `gt_stone_scatter` +
`gt_block_object_scatter`）已经删除。注册数必须保持 **0**。

## 禁止

- 新增 `GtItemScatterFeature` / `GtStoneScatterFeature` /
  `GtBlockObjectScatterFeature` / `ItemScatterConfiguration`
- 任何在区块生成里 `addFreshEntity(ItemEntity)` 或按目录往地表倒方块
- 计划、`player_path_support`、`kind: worldgen_drop` 把目录散落写成获得
- 用「empty-input overworld item scatter」或「用主世界掉落物顶 player_complete」关卡

## 允许

- GT6 有的世界生成：石块、树、矿脉、地牢战利品、作物——对照
  钉住的 GregTech 6 翻译，不另发明目录倾倒
- 真实 RecipeManager 配方（无 stand-in 配料）
- 缺真实路径时保持 `runtime_ready` / `blocked`；`survival_access` 可标
  `blocked` / `partial`，不挡 runtime 关闭

代理规则：`.cursor/rules/gt6-no-item-entity-worldgen-acquisition.mdc`。
交付门：[capability-delivery-workflow.md](capability-delivery-workflow.md)。
