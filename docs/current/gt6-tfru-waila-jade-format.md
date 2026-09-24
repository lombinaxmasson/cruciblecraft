# GT6-TFRU Waila → Jade 映射格式

本格式是迁移账本，不是 Jade Provider 注册表。源事实来自本地
`gt6_tfru/gregtech6-TFRU` 与 `ktfruaddon/kTFRUAddon`。

## 数据结构

每个 machine family 在
`tools/waves/presentation/gt6-tfru-waila-jade/mapping.json` 中记录：

- `source`：实际类、接口和 Waila 方法；
- `surface`：`waila` 或 `item_tooltip`；
- `head/body/tail`：显示顺序和行语义；
- `server_data`：Waila NBT key、类型、来源；
- `jade_projection`：未来 Jade Provider、同步路径和 display sections。

`surface` 必须严格区分：

- `waila`：`IWailaTile` 回调真正输出的内容；
- `item_tooltip`：`addToolTips()` 输出，只用于参考数值/颜色，不能直接当作 Jade 行。

## 可复用标准行

| Waila provider | Jade section | 数据/颜色语义 |
|---|---|---|
| `instanceInfoState` | `state` | 强制停止黄、运行绿、被动蓝、待机红+绿、停止红、节能青 |
| `instanceInfoEnergyIORange` | `energy_io_range` | 输入/输出范围；数值白色，多安培数量青色 |
| `instanceInfoEnergyIORec` | `energy_io_recommended` | 推荐输入/输出 |
| `InfoTank` / `addTankDesc` | `fluid_tanks` | 数量白、`L` 青、流体名白 |
| `addFluidStackDesc` | `fluid_output` | 数量白、`L` 青、流体名白 |
| `addEnergyAmountDesc` | `energy_buffer` | 数量白、能量单位沿用 GT6 |

## 迁移规则

1. 先写服务端同步字段，再写客户端显示行。
2. Jade 行按 `head → body → tail` 排序，不把一个 Waila 长字符串拆成无语义的
   通用 packet/window 字段。
3. `IWailaTile.super` 只表示标准 provider 链；如果源类没有覆写
   `getWailaInfos()`、`getWailaNBT()` 或 `getWailaBody()`，不得为它添加专属
   Waila/Jade 行。
4. 多方块 dummy 必须转发 controller 的数据，不能复制一份虚假的机器状态。
5. GT6 物品 tooltip 的颜色只记录为参考 token；Jade 使用自己的
   `Component` 样式，不复刻旧版格式控制码。

## 当前三个重点机器

- 大型燃气/蒸汽涡轮：状态、涡轮耐久、输入罐和输出罐。
- 蒸汽机：GT6-TFRU 中实现了 `IWailaTile`，但没有专属 provider/body/NBT；
  彩色转换、效率、输入输出范围属于物品 tooltip。
- 燃烧室：`MultiTileEntityGeneratorLiquid/Gas` 没有专属 Waila rows；
  配方、效率、空气/点火和危险提示属于物品 tooltip。

具体源类、NBT key、颜色 token 和未来 Jade sections 以 JSON 账本为准。
