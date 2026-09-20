# 蒸馏塔 GT6 对齐详细计划

> 计划 slug：`machines/distillation-tower`
> capability_slug         = machines/distillation-tower
> unique_active_wave      = null
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：蒸馏塔 / 低温蒸馏塔
> 性质：推翻 v1 把 `RM.DistillationTower` 并进 `RM.Distillery` 的 DESIGN_POLICY。
> 热塔是核心；低温塔共用 18101/18102 几何，CU 缓冲。
> 不是 `player_complete`。机器可读 `unique_active_wave = null`（已闭卡）。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = machines/distillation-tower
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
hosts                        = 17101 Distillation Tower + 17111 Cryo Distillation Tower
parts                        = 18101 heat transmitter ENERGY_INPUT + 18102 tower parts
panel                        = RM.DistillationTower / Cryo  1/3 item, 1/9 fluid
energy                       = HU 4096/1024 buffered；低温 CU 同窗
out_of_scope                 = soulsandoil；BiomassIC2；Oil_Heavy2/HotCrude/Light2；netherair/enderair；form-open
close_target                 = runtime_ready
```

来源：`MultiTileEntityDistillationTower` / `CryoDistillationTower`，
`Loader_MultiTileEntities.java` 17101/17111/18101/18102，
`Loader_Recipes_Chem.java` 350–365。

## 0. 开场判断

Live id 保持 `cruciblecraft:distillation_tower`。catalog `multiblock/distillation_tower` 折进这个 live BlockItem，不再注册第二份 17101。
几何 81 格：9 ENERGY_INPUT（y=-1）、8 ITEM_FLUID（y=0 除控制器）、63 FLUID_OUT（y=1..7）、1 控制器。
北向 JSON 时塔在控制器背后（local +Z）。远面孔 `relative(facing.opposite, 3)`。

不进 `CONFIGURED_MACHINES` / `CHEMICAL_HOST_MACHINES`。菜单与 EMI 走 `MULTIBLOCK_MENU_HOSTS`。

## 1. 获得格

| 身份 | GT6 | CC |
| --- | --- | --- |
| 17101 `M` | 18102 Distillation Tower Part | `multiblock/distillation_tower_part` |
| 17101 `P` | `OP.pipeNonuple(MT.StainlessSteel)` | `stainless_steel/nonuple_fluid_pipe` |
| 17111 `M` | 同一 18102 | 同一 part |
| 17111 `P` | `OP.pipeNonuple(ANY.Cu)` | `copper/nonuple_fluid_pipe` |

禁止 invar distillery + 8 个通用端口顶控制器。禁止 stand-in。

## 2. 配方

热塔 6 行：biomass、oil_extra_heavy、oil_heavy、oil_medium、oil（Oil_Normal）、oil_light。
低温 1 行：air。`liquid(U50)`=20 mB；`gas(U7,T)`=143、`U20`=50、`U100`=10、`U1000`=1。

高度：propane/methane +7，butane +6，petrol/ethanol +5，kerosine/glycerol +4，diesel +3，fuel +2，其余 +1。
低温：helium +7，neon +6，nitrogen +5，oxygen +4，argon +3，carbon_dioxide/sulfur_dioxide +2，其余 +1。物品走 y+0 远面孔。

## 3. 明确不接管

- 公共 16 / 长尾形态开闸
- 管缆切片 C
- 把塔 spec 塞进 CONFIGURED_MACHINES
- netherair / enderair / soulsandoil 缺流体顶别的油

## 4. 关闭清单

- [x] 热塔独立 map 1/3/1/9，不再跑 distillery 行
- [x] FLUID_OUT 端口 + 拆分 JSON 9/8/63/1
- [x] 高度自动输出
- [x] GT6 获得格与 basicmachines 贴图/GUI
- [x] 形成后 18102 design 0→1 贴图切换（通用端口 TOWER_SKIN / BACK_HOLE）
- [x] 结构用 live 18101/18102；一颗 18102 按 bind 当 ITEM_FLUID 或 FLUID_OUT
- [x] 能量只从 18101 进；控制器壳体 energy NONE
- [x] 零件 covers / 工具转发
- [x] catalog dummy 17101 折进 live `distillation_tower`
- [x] 低温塔 CU 共用结构 tag
- [x] GameTest / JUnit / later_wave +7 / unique-active 账本
