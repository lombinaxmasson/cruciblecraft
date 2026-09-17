# 材料前缀组件身份 ADR

状态：Accepted  
范围：材料前缀物品 / 可放置材料方块的 registry 粒度  
日期：2026-09-17  
类别：`DESIGN_POLICY`

人读合同：[material-prefix-identity.md](../current/material-prefix-identity.md)。  
落地计划：[材料前缀组件身份详细计划](../history/card-plans/closed/材料前缀组件身份详细计划.md)（已关 `runtime_ready`）。

## 背景

GT6 用少量 multiitem + damage/meta 表达「前缀 × 材料」。CrucibleCraft 1.21 把同一对展开成独立 `DeferredItem` / `DeferredBlock`（`MaterialItem` 把 `materialId` 和 `form` 烤进物品类）。`material_registration_gate` 在 2026-09-17 记录约 39,813 个已启用形态对、1,776 个材料定义。加一种材料的成本随前缀数线性增长，所以即使用配方 mill，live 物品仍被门禁挡住。

工具、机器、流体单元、hosted ore 已经用 DataComponent 选材料。粉/锭/板等库存件还没有。

更早的卡曾写「独立 registry id；不折进 prefix，不用共享物品+材料 component」（见 [紧凑配方传输编解码](../history/card-plans/closed/紧凑配方传输编解码详细计划.md)、[工具头前缀折回](../history/card-plans/closed/工具头前缀折回详细计划.md) 的 deferred）。那是当时为了避开组件栈、用独立 id 顶住 compact 传输。它解决的是 RecipeHolder NBT，不是材料开放性。本 ADR 废止「材料形态必须一人一 id」这条，不废止 compact 的 exact-relation 合同，也不要求本卡去改 Holder 粒度。

## 决策

**自身开放性优先于其他模组标签兼容。** 目标是：前缀集合冻结之后，新材料主要是 JSON（颜色、性质、名字、它有哪些前缀），不必再为每种库存形态占一个 Item id。

三分身份（不要做成「万物一个 Block」）：

1. **库存前缀**（粉、粒、锭、板、杆、螺栓、齿轮、箔、宝石、碎矿、工具头等）：一个 Item 一种前缀；材料写在 DataComponent 上。这是 GT6 meta 在 1.21 的对应物。
2. **已有方块实体的可放置前缀**（管、缆、hosted/broken/bedrock ore）：一个 Block 一种前缀（或已有共享方块）；材料进方块实体 / 已有 `ORE_MATERIAL`。管缆已经为网络付了实体税，不应再为每个材料复制一份带六面连接的 BlockState 表。
3. **哑方块**（存储块、机壳、卵石/岩块）：继续按材料注册独立 Block。没有 tick 的实心格用实体区分材料，比多几个 Block id 更贵。

禁止：

- 回到原版 damage/meta。
- 「一个材料一个 Item，形态当组件」。形态决定模型、单位、堆叠和行为，前缀才是物品种类。
- 为了公共 `c:` 标签继续把 GT6 内部中间态展开成独立 id。
- 在别人的 unique-active 上做这场迁移。
- 用 stand-in 配料或目录 ItemEntity 散落顶「全开了」。

旧存档不在本决策范围内：落地时可以丢掉 `cruciblecraft:{material}_{form}` 这类旧 id，不为它们做 DataFixer。`formItems()` 指向的原版/外部物品仍然是外部身份，不是 CC 前缀物品。

## 现状与目标的差

现在：`ModItems.registerMaterials` 对每个 gate 形态 `ITEMS.register(material.registryName(form), () -> new MaterialItem(material, form, …))`。`MaterialLookup.item()` 返回该 Item。配方配料走 `c:{tag_directory}/{material}` 物品标签。

目标：`MaterialLookup.stack(material, form)` 是规范构造（前缀 Item + 材料组件）。`MaterialLookup.item(material, form)` 对 CC 自有库存形态返回共享前缀 Item。gate 的含义从「注册这些 Item」变成「这些 `(material, prefix)` 允许作为 live 栈」。CC 自有配方用 `DataComponentIngredient`（已有工具/机器先例），不再把共享 `dust` 放进 `c:dusts/iron`。

流体不在本 ADR：GT6 `FL.xxx` 本来就是独立 Fluid；化学流体门禁仍独立。

## 后果

- 注册表按前缀封顶；新材料不再涨 Item id。
- EMI / 创造栏仍可能枚举大量**栈**；省的是握手和模型文件，不是目录行数。
- 公共物品标签不再能区分同前缀的不同材料。其他模组兼容后置，需要时再做薄适配，不作为内核约束。
- 统一器「同前缀换材料」变成改组件。跨前缀单位换算（仓储桶粉/小撮粉）仍是另一张缺口，本 ADR 不解。
- compact family 仍可保持 exact relation；后续可以用前缀 Item + 组件谓词缩小 JSON，但那是配方表示问题，不是本决策的关门条件。

## 废止

下列读法作废，仅对**材料前缀形态**生效：

- 「CC 材料形态 = 独立 registry id」
- 「共享物品 + 材料 DataComponent 已拒绝」作为材料线永久政策

下列仍然有效：

- 电路 `CIRCUIT_CONFIG` 仍后置，不并进材料组件。
- 工具头已经折回「材料 × 前缀」；落地时再把那些独立 `MaterialItem` 收成前缀物品，不重开 identity 分母。
- 配方不得 stand-in；不得 ItemEntity 世界生成当获得。
