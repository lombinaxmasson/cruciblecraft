# 材料身份：前缀物品 + 材料组件

GT6 meta 在 1.21 的对应物是 **DataComponent，不是一人一 id**。
权威决策：[材料前缀组件身份 ADR](../decisions/材料前缀组件身份ADR.md)。
落地：[材料前缀组件身份详细计划](../history/card-plans/closed/材料前缀组件身份详细计划.md)。
本页是现行合同。代理规则：`.cursor/rules/material-prefix-identity.mdc`。

## 优先级

加 CC 自己的材料要便宜，高于其他模组认不认 `c:dusts/iron`。
公共物品标签不能区分同 Item 上的组件；那是后置适配，不是内核。

## 三分

| 种类 | 例子 | 身份 |
| --- | --- | --- |
| 库存前缀 | dust / ingot / plate / rod / gem / crushed_ore / tool_head_* | 一个 Item 一种前缀；材料在组件上 |
| 已有 BE 的可放置前缀 | fluid/item pipe、cable、hosted/broken/bedrock ore | 共享 Block；材料在 BE / `ORE_MATERIAL` |
| 哑方块 | storage `block`、machine_casing、rock | 继续按材料独立 Block |

不要用「一个材料一个 Item，形态当组件」。不要回到 damage。不要给哑方块强行加实体。

## 现在的模型

`ModItems.registerMaterials` 为每个库存前缀注册一次 `PrefixMaterialItem`。
材料写在 `prefix_material` 上。`MaterialLookup.stack` 是规范构造。
gate 许可 live 栈，不再为库存前缀分配 per-material Item id。

不要 dual-register 旧的 `cruciblecraft:{material}/{form}`。`formItems()` 外部映射保留。

## 禁止

- 给库存前缀按材料再注册一套 `MaterialItem`，或让 `iron/dust` 与 `dust` 同时 live
- 把共享前缀 Item 放进按材料划分的 `c:` 标签
- 用原版物品、错误前缀或 `programmed_circuit` 顶缺失形态
- 为了「全开」去注册对方模组材料或发明获得路径

## 允许

- 工具 / 机器 / 单元 / hosted ore 继续用已有组件
- 新材料 JSON 与 gate 仍按 `(material, prefix)` 记账；含义将改为 live 栈而不是新 Item id
- 其他模组兼容作为独立薄层，不挡本内核
