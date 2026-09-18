# 材料身份分层混合 ADR

状态：Accepted（**目标策略；live 已按本文件落地**）  
范围：材料前缀物品的 registry 后端；不改流体、不改哑方块、不改管/缆 hosted  
日期：2026-09-18  
类别：`DESIGN_POLICY`

人读合同：[material-prefix-identity.md](../current/material-prefix-identity.md)。  
代理规则：`.cursor/rules/material-prefix-identity.mdc`。  
对照评估（非授权）：会话 canvas「材料身份方案再评估」。  
被本文件取代为目标策略的记录：[材料前缀组件身份 ADR](材料前缀组件身份ADR.md)（只作事故落地的历史说明，不再当终态）。

**本文件是目标策略。** 2026-09-18 用户下令 unique-active `registry/hybrid-material-identity` 按本文件重写 live；该卡已关 `runtime_ready` / `accepted`。

## 0. 为什么要再立一份

2026-09-17 对 GTCEu 做了再分析。当时的推荐不是「全部库存前缀收成一个 Item + 组件」，而是 **无双身份的分层混合**：

1. `formItems()` 已有外部规范 Item：用外部身份，不注册 CC 副本。
2. **公共交换前缀**：每个 `(材料, 前缀)` 一个独立 Item，进入精确 `c:` 标签。
3. **GT6 内部长尾前缀**：前缀 Item + `prefix_material`；不导出伪精确标签。
4. 管 / 缆 / 哑方块各自决策，不绑进库存身份。
5. 每个 `(材料, 前缀)` 只许一种 live 后端。禁止双注册、禁止兑换壳。

同一次评估还写明：GTCEu 式独立 Item 是更安全的生态默认；全量组件只在必须低成本摊开数万内部组合时占优；薄兼容层不能把共享 `dust` 塞进 `c:dusts/iron`。

之后 unique-active `registry/prefix-material-component` 仍按更早那份「自身开放性优先、库存前缀全部组件化」落地，并关了 `runtime_ready`。再分析没有改合同，实现把已推翻的方向当成了终态。2026-09-18 认定这是 **大方向事故**，不是小修。

本文件冻结目标。代理不得再靠自己的权衡、canvas、或「更现代 / 加材料更便宜」去改身份后端。

## 1. 目标策略（推荐混合）

| 种类 | 身份 | 标签 |
| --- | --- | --- |
| 外部 `formItems()` | 外部 Item，不注册 CC 副本 | 外部已有的 |
| 公共交换前缀 | 每材料独立 `MaterialItem`（或同等一人一 id） | 精确 `c:{directory}/{material}`，例如 `c:dusts/iron` 只含铁粉这一个 Item |
| GT6 内部长尾库存前缀 | 一个前缀一个 `PrefixMaterialItem`；材料在 `prefix_material` | 禁止把共享前缀 Item 放进按材料划分的 `c:` 标签 |
| 已有 BE 的可放置前缀（管、缆、hosted ore） | **保持现有 hosted**；切片 C 仍未做，本 ADR 不授权去做 | 不在本决策 |
| 哑方块（storage `block`、机壳、岩块） | 继续按材料独立 Block | 不在本决策 |

不要用「一个材料一个 Item，形态当组件」。不要回到 damage。不要给哑方块强行加实体。

公共库存 Item 的 registry 路径回到 `cruciblecraft:{material}/{form}`（与 mill 逻辑 id 一致）。该 id 只对公共前缀 live；长尾不得再注册同路径的第二套 Item。

## 2. 公共前缀冻结名单

规模证据来自 2026-09-17 `material_registration_gate.json` 计数（约 1,776 材料、39,813 形态对）。名单本身是 `DESIGN_POLICY`，不是 GT6 dump。

**核心 7**（公共面下限；当时 3,524 对）：

`ingot`, `nugget`, `dust`, `plate`, `rod`, `gear`, `gem`

**扩展 16**（推荐混合的公共交换前缀；当时 9,069 对）。**不得私自加减：**

`ingot`, `nugget`, `dust`, `small_dust`, `tiny_dust`, `plate`, `rod`, `long_rod`, `bolt`, `screw`, `ring`, `gear`, `small_gear`, `gem`, `foil`, `fine_wire`

上表有 `formItems()` 映射的 `(材料, 前缀)` 仍走外部 Item，不另注册 CC 副本。

其余当时已切成共享库存前缀的形态（`tool_head_*`、碎矿洗涤链、`purified_dust`、`dust_div72`、`ingot_hot`、多层板、转子、弹簧等）属于内部长尾，目标仍是前缀 Item + 组件。

增减公共名单、把长尾改成一人一 id、或把 16 个公共前缀再收进组件，都是新的 `DESIGN_POLICY`，必须用户当场确认。

## 3. live 与目标（已对齐）

| | live | 目标（本 ADR） |
| --- | --- | --- |
| 上列 16 个公共前缀 | 每材料独立 Item + 精确 `c:` 标签 | 每材料独立 Item + 精确 `c:` 标签 |
| 其他库存前缀 | 共享前缀 Item + 组件 | 维持组件（与 live 同向） |
| 管 / 缆 / 红石线 / hosted ore | 按材料 unique hosted Block | 不变，除非另开用户下令的卡 |
| 哑方块 | 按材料独立 Block | 不变 |
| `cruciblecraft:{material}/{form}` 与前缀 Item | 禁止同时 live；slash 只是解析期别名 | 公共前缀：slash 就是 live Item。长尾：slash 仍只是解析期别名 |

关卡之后仍禁止：

- 把公共 16 再收进组件。
- 把其余库存前缀继续往「更彻底的组件化」推（含管/缆切片 C）。
- dual-register。
- 发明兑换壳、双身份、或把共享 `dust` 放进 `c:dusts/iron`。
- leftover mill JSON 可以把公共前缀 Item + 组件单向解析成 unique Item。

## 4. 代理禁令（本文件存在的原因）

材料身份后端是 `DESIGN_POLICY` 大方向。下面任一行为都算事故：

- 用户没有在**同一轮对话**里明确说「按推荐混合重写 / 改身份策略 / 把公共前缀改回一人一 id」，代理自己落地。
- 把权衡表、canvas、再分析、GTCEu 源码阅读、「更安全 / 更现代 / 加材料更便宜」当成落地授权。
- 发现 live 与目标不一致，就「顺便改掉」或「先改合同再改代码」。
- 用新 ADR 覆盖旧 ADR 却继续按旧 ADR 实现，或反过来：实现已经偏了，还把偏了的实现写回合同当终态。
- 为了兼容或为了开放性，把三分法收成单一后端（全 GTCEu 或全组件）。

允许：解释 live 与目标的差；把缺口记在合同里；修组件路由、codec、测试，只要不改「谁是 Item、谁是组件」。

再改身份后端只在用户明确下令之后另开 unique-active。本 ADR 不占落地锁。

## 5. 废止与仍有效

废止：

- 「自身开放性优先于 `c:` 标签兼容」作为库存身份的终态优先级。
- 「所有库存前缀 = 一个 Item 一种前缀 + 组件」作为终态。
- 把 [材料前缀组件身份 ADR](材料前缀组件身份ADR.md) 或已关计划 `registry/prefix-material-component` 读成现行目标。

仍有效：

- 禁止 damage/meta。
- 禁止「一个材料一个 Item，形态当组件」。
- 禁止 stand-in 配料和目录 ItemEntity 世界生成。
- 电路 `CIRCUIT_CONFIG` 仍后置。
- 工具 / 机器 / 单元 / hosted ore 的已有组件不是本 ADR 的重写对象。
- 流体仍独立 FluidType；化学流体门禁仍独立。
- 旧存档不做 DataFixer，除非重写卡另写。

## 6. unique-active 已关

`registry/hybrid-material-identity` 已关 `runtime_ready` / `accepted`。当时用户下令后完成了：

1. 按第 2 节把 16 个公共前缀从 `PREFIX_ITEMS` 挪回 per-material Item，写入精确 `c:` 标签。
2. 长尾留在 `PrefixMaterialItem`。
3. `MaterialLookup` / mill codec：公共前缀解析成 unique Item，长尾仍 rewrite 成组件栈。
4. 每个 `(material, prefix)` 单一后端测试：无双 live id；公共 `c:dusts/iron` 只含铁粉 Item；共享长尾前缀不进按材料标签。
5. 更新合同、本 ADR 现状段、代理规则。
