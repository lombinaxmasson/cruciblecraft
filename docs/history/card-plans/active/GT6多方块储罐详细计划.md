# GT6 多方块储罐详细计划

> 计划 slug：`machines/gt6-multiblock-tanks`
> 状态：unique-active。本文件位于 `card-plans/active/`。
> 性质：机器与多方块总计划的第 2 张，并收进原先单独排的第 7 张。
> 总计划：[GT6 机器与多方块总计划](../prep/GT6机器与多方块总计划.md)。
> 正式名称：GT6 多方块储罐
> 性质：对照 `MultiTileEntityTank3x3x3` 与 `MultiTileEntityTank5x5x5`。
> 不是连续边长。不是单方块蒸汽锅炉罐 1200–1262。不是储罐延长件或桥。
> 不发明形态。不 stand-in 配料。不占位贴图。不主世界 ItemEntity 撒目录。
> 不是 `player_complete`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = active
capability_slug              = machines/gt6-multiblock-tanks
unique_active_wave           = machines/gt6-multiblock-tanks
maturity                     = runtime_ready
workflow                     = active
source                       = MultiTileEntityTank3x3x3 / MultiTileEntityTank5x5x5
geometry                     = 3x3x3 one air cell; 5x5x5 inner 3x3x3 air
energy                       = none
close_target                 = runtime_ready
landing_depends_on           = machines/large-boiler yielded paused
out_of_scope                 = other spans; wood 5x5x5; steam boiler tanks 1200-1262
                               tank extender 30001; tank bridge 30501; logistics tank 32072
                               form-open; stand-in 配料; 占位贴图; 主世界 ItemEntity 撒目录
```

来源：`Loader_MultiTileEntities.java` 1195–1222，
`MultiTileEntityTank3x3x3.checkStructure2`，
`MultiTileEntityTank5x5x5.checkStructure2`。

## 0. 形状

GT6 没有边长滑杆。多方块储罐只有两套空心方块，共用 `MultiTileEntityTank` 的容量、proof 和自动输出。

| 形状 | 类 | 空气 | 墙端口 | 阀门 |
| --- | --- | --- | --- | --- |
| 3×3×3 | `MultiTileEntityTank3x3x3` | 中心 1 格 | 25 | 木 17001，金属 17002–17007，致密 17022–17027 |
| 5×5×5 | `MultiTileEntityTank5x5x5` | 内部 3×3×3，共 27 格 | 97 | 金属 17042–17047，致密 17062–17067。没有木 |

中心是 `getOffsetN(facing, radius)`。3×3×3 的半径是 1，5×5×5 的半径是 2。结构 JSON 按朝北的阀门来写，朝内是 +Z。

## 1. 容量与墙

容量和 proof 按 Loader 的 `NBT_TANK_CAPACITY` 与四项 proof。5×5×5 用同材质的 3×3×3 墙：普通金属墙 18002–18007，致密墙 18022–18027。致密精金 17065 的容量是 2,048,000,000 mB，仍放得进 `FluidTank` 的 int。

阀门路径：

- 木：`wood/tank_main_valve`
- 3×3×3：`multiblock/small_{dense_}{material}_tank_main_valve`
- 5×5×5：`multiblock/large_{dense_}{material}_tank_main_valve`

`controllerId()` 用显式的 span 和 dense。不能再用 `meta >= 17022` 判断致密，因为 17042 已经大于 17022，但它是普通大罐。

## 2. 行为

两套阀门共用 `TankBlockEntity`。结构 id、端口数和融化半径来自 profile。

- 一面墙的 meta 必须等于该阀门的 `NBT_DESIGN`。
- 自动输出：水平朝向，或气体，或朝上且更轻，或朝下且更重。从阀门正面推出去。
- 融化以结构锚点 `center` 为心，烧半径 1 或 2 的整立方。锚点就是 `getOffsetN(facing, radius)`，朝北时在 +Z，朝上时在 +Y。以前 3×3×3 把中心放在阀门正面外侧。
- 酸、魔法、等离子、气体的既有失败路径继续共用。酸仍然拆掉已绑定的壳，不改成 GT6 的随机三分之一。

旧 `cruciblecraft:tank_3x3x3` 仍是存档壳，不成形。

## 3. 明确不接管

- 2×2、4×4 或其他边长
- 木制 5×5×5
- 单方块蒸汽锅炉罐 1200–1262
- 储罐延长件、桥、物流罐
- 在这张卡上开材料形态，或用别的零件顶获得格

## 4. 关闭清单

- [x] 13 个 3×3×3 阀门和 12 个 5×5×5 阀门都进 `TankControllerProfiles`
- [x] `tank_5x5x5.json`：125 格，27 空气，97 流体端口，1 阀门
- [x] 大罐阀门不进 `tank_3x3x3_controllers`
- [ ] 声明的 GameTest 通过后 `workflow=accepted`，并跑覆盖表重建
