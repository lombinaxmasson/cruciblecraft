# CrucibleCraft 阶段档案 · 首小时表现与阶段账本

> 状态：`FIRST_HOUR_PRESENTATION_READY`（2026-09-02）
> 计划 slug：`presentation/first-hour-and-stage-ledger`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`CROPS_FOOD_BEES_R0_READY`；核能 Track C `started = false`
> Closing：四台首小时 host 脱离 `metal_surface`；`smelter` 纠正为
> `basicmachines/smelter` 立方机；阶段账本三态 / 后续顺序 / 核电体积写入
> 现行缺口总账；不实现冻结项 runtime；不预分配 implementation child；
> `nuclear_started = false`；`unique_active_wave = null`；
> `next_unassigned = true`；`production_lock = null`

## 关闭结果

- 零 family 表现 + 文档卡。`owns_families = 0`。`completion_delta = 0`。
  `generated_recipe_count = 0`。未发配方，未签 production lock，未重开
  T34 19 目标。
- 贴图 SOURCE_BACKED，从 `gt6_referencable_port_code/gregtech6_w` 迁入
  （CC0；已小写）。`mortar` / `sifter` / `bath` 保持 voxel 几何，按
  `getTexture2` 分槽重绑；`table*` 只拷不绑；工具机动态（杵头材料色、
  筛上粉尘、浴槽液面）标以后 BER，不接 runtime。
- `smelter` 曾错绑 `MultiTileEntitySmeltery` 小锅（与坩埚同一套几何）。
  本卡把它改走 `machine_cube_2_layer` + `textures/block/machine/smelter/`。
  坩埚保持 T34。拷入 `overlay_active` / `overlay_running`，未接 `LIT`
  （`ProcessingMachineBlock` 只有 `FACING`）。
- 档位别名走现有 `machineTextureId`。不重跑 `tools/gt6_voxel_to_cc.py`；
  该脚本不再写出 `smelter.json` 小锅。
- [冻结与未实现账本](../../current/unimplemented-gap.md) 写入三态
  （`frozen` / `runtime_ready` / `player_complete`）、后续可玩顺序、核电
  体积。第 1 节冻结项不得标成已实现。物流 1.2 不在顺序里。
- 不写空 wave JSON。sealed `growth_order.json` 未改写。

```text
FIRST_HOUR_PRESENTATION_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成 GT6 表现完成或
核电 / 物流网已实现。

## 权威 artifacts

- `src/main/resources/assets/cruciblecraft/textures/block/machine/{mortar,sifter,bath,smelter}/**`
- `src/main/resources/assets/cruciblecraft/models/block/{mortar,sifter,bath}.json`
- datagen `machine_cube_2_layer` 生成的 `smelter` / 档位别名 / `textureProfile: smelter` 焙烧机
- `python tools/verify.py integration --profile presentation`
- `python tools/verify.py dev --path docs`

无 `tools/waves/presentation/**`。T34 manifest / T30 hopper provenance 只读。
