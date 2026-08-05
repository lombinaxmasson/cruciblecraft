# T10d 容器身份与边界 ADR

状态：Accepted  
范围：T10d–e  
日期：2026-08-05

## 决策

CrucibleCraft 使用两个通用、组件承载内容的可复用容器：

- `cruciblecraft:fluid_cell`：仅接受 `ITEMGENERATOR.CONTAINERS_FLUID`
- `cruciblecraft:gas_cell`：仅接受 `ITEMGENERATOR.CONTAINERS_GAS`
- 两者容量均为 1,000 mB
- 空 cell 最多堆叠 64；含有任意流体时最大堆叠为 1
- cell 的流体身份存放在各自的 `SimpleFluidContent` data component
- cell 只负责流体搬运；机器配方仍匹配机器 fluid tank

液/气边界以固定 GT6 generation tag 域为准，不根据材料名称、
`gt6_metadata.state`、温度、密度或已注册 FluidType 反推。
`chlorine` 即使物态为 gas，只要来源域是 `CONTAINERS_FLUID`，
也只能进入 fluid cell。

## 身份与索引

filled cell 不是 material-prefix item，也不按内容注册独立物品。
因此：

- 不新增 `cell` material prefix
- 不修改 `ComponentIngredientIndex`
- 不生成 per-fluid canner/boxinator recipe
- 不把 runtime component 状态计入 recipe publication 或 handshake entry
- `MaterialFingerprint` 仍只覆盖材料与前缀结构

配方需要流体时，玩家先把 cell 内容转移到机器 tank；配方索引继续按
`GTRecipe.fluidInputs` 工作。

## 堆叠与事务

NeoForge `FluidHandlerItemStack` 要求容器数量为 1。对 count > 1 的空
cell，交互代码不得直接变异原 stack：

1. 拆出一个 count=1 的副本
2. 模拟目标 fluid handler 与玩家背包容量
3. 只有两项模拟都成功时才执行流体事务
4. 原手持 stack 减一，把结果 cell 放入背包；背包状态变化时再次验证
5. 任一步失败都保持原 cell、目标 tank 和流体量不变

count=1 的空/满 cell 继续暴露标准 `IFluidHandlerItem` capability。
自动化若提交 count>1 的 cell stack 必须 fail-closed，由上游 item handler
先拆栈。

## 与其他运输方式的边界

- cell：1,000 mB，手持、小批量、可经 item pipe 搬运
- portable tank：64,000 mB，保持 T5 的 bulk hand-carry 与原子事务语义
- fluid pipe：连续流体运输；gas-proof、温度和腐蚀约束仍由 T8 管道负责

本 ADR 不重写 portable tank，不增加 Canner/Boxinator 机器，也不把 cell
内容作为 item ingredient。

## 域关闭策略

以下 14 个材料只有 `ITEMGENERATOR.CONTAINERS`，没有 fluid/gas cell
来源证据，因此作为显式非流体例外关闭，不生成 cell allowlist：

`aerotheum`, `anti_gravitonium`, `anti_mac_guffium`, `cryotheum`,
`gravitonium`, `honey`, `honeydew`, `ice`, `mac_guffium`, `methane_ice`,
`milk`, `nitro_carbon`, `petrotheum`, `pyrotheum`.

T10e 只激活 61 个 fluid-domain 和 48 个 gas-domain 成员。例外列表、
allowlist 和 chemical-fluid registration 必须由独立 readiness builder
双向验证；缺失、交叉或多余成员均阻止启动账本进入 READY。

## 验收

- 一个液体 cell 与一个气体 cell 分别完成 fill → hand transport →
  machine drain/consume
- cross-domain 与上述 14 项均被拒绝
- 空栈拆分、满背包、simulation 与执行失败不复制或丢失资源
- full drain 删除内容 component 并恢复空 cell 堆叠能力
- runtime cell 内容不增加 MaterialRule 展开、publication 或
  material handshake entry
