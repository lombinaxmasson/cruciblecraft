# 材料身份：分层混合

**目标权威：** [材料身份分层混合 ADR](../decisions/材料身份分层混合ADR.md)。  
历史落地（事故态，已被本卡改掉）：[材料前缀组件身份 ADR](../decisions/材料前缀组件身份ADR.md)，计划 `registry/prefix-material-component` 已关 `runtime_ready`。  
已关能力：`registry/hybrid-material-identity` `runtime_ready` / `accepted`，计划 `docs/history/card-plans/closed/材料身份分层混合详细计划.md`。  
代理规则：`.cursor/rules/material-prefix-identity.mdc`。

## 目标

公共交换前缀一人一 Item，进精确 `c:` 标签。GT6 内部长尾才用前缀 Item + `prefix_material`。每个 `(材料, 前缀)` 一种 live 后端。

公共 16 个（不得私自加减）：

`ingot`, `nugget`, `dust`, `small_dust`, `tiny_dust`, `plate`, `rod`, `long_rod`, `bolt`, `screw`, `ring`, `gear`, `small_gear`, `gem`, `foil`, `fine_wire`

`formItems()` 外部映射优先。哑方块继续按材料独立 Block。管 / 缆 / hosted ore 保持现有 hosted，切片 C 未授权。

不要「一个材料一个 Item，形态当组件」。不要 damage。不要双注册。不要兑换壳。

## live

公共 16 前缀是独立 `MaterialItem`，路径 `cruciblecraft:{material}/{form}`，进入精确 `c:` 标签。  
其余库存前缀是共享 `PrefixMaterialItem` + `prefix_material`。长尾 slash id 只是解析期别名。  
共享前缀 Item 不进 `c:dusts/iron`。`formItems()` 仍用外部 Item。

这是 2026-09-18 用户下令的混合落地，不是 09-17 全量组件事故的延续。

## 禁止

- 把公共 16 再收进组件，或把长尾改成一人一 id
- 发明双注册或兑换壳
- 继续做管/缆切片 C
- 用 stand-in 配料或 ItemEntity 世界生成顶「材料全开了」
- 把已关计划 `registry/prefix-material-component` 读成现行目标

## 允许

- 修混合后端下的 codec、标签、EMI、测试
- leftover mill JSON 把公共前缀 Item + 组件解析成 unique Item（单向，不双 live）
- 按普查 `openable` 开 gated 形态（另开 unique-active；本身份卡已关，不再扩名单）
