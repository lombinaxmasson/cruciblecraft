# CrucibleCraft 阶段档案 · T33

> 阶段：T33 · 地表石子可达性闭环
> 状态：● 已关闭（`T33_READY`，2026-08-22）
> 权威产物：[`tools/t33_readiness.json`](../../../tools/t33_readiness.json)
> 闭卡 profile：`python tools/verify.py integration --profile worldgen`

## 1. 关闭判据

T33 将动态 runtime `c:rocks` tag 的地表散布纳入 survival S0，使 `rock_pack.json`
可达，同时保留 T20 地下矿脉 catalog 的历史分母。它不是岩层替换、GT6 大矿脉等价实现，
也不包含逐材质平衡。

## 2. 三轴

**Closure**

- `surface_rock_scatter` declaration、configured feature、placed feature 与 Overworld
  biome modifier 相互一致；
- GameTest 从真实 registry 验证 `c:rocks` 和实际 dirt/sand 表面放置，且不会覆盖非空气格；
- S0 从 material-gate `rock` form 重建 688 个 concrete seed；`rock_pack` 不再不可达；
- `CC-4.5-P4` 由 T27 `closed_open_item` 记录为 T33 closed。

**Fidelity**

- 表面散布是明确 `SOURCE_DERIVED` / `DESIGN_POLICY`，不是 GT6 大矿脉或指示岩的等价声明；
- rarity、tag、biome step 与 S0 来源均有机器可读声明。

**Load**

- 无新矿脉、方块、物品、配方 publication 或 T20 catalog 分母变化；
- T20 计数固定为 generated **263**、all worldgen **274**、closure
  classifications **129**。

## 3. 验证

- scoped T33 `verify.py dev`：退出 0；
- `verify.py integration --profile worldgen`：退出 0；
- GameTest：**138/138** required tests passed；
- 默认 datagen 连续两次：退出 0；排除内部 `.cache` 后 generated resources tree
  SHA-256 一致；
- `t33_readiness.json`：`status = T33_READY`。

`T32-VD-001`（T5 chemical readiness source-hash drift）仍为 open。它只使 recipes
profile 保持红灯，未被标记为 passing / waived，也不属于 T33 声明的 worldgen 闭卡门。

## 4. 交接

下一张唯一 active 内容卡是 **T34 · 开局对象表现层**：以 T33 关闭后的开局可达性为输入，
优先 anvil、crucible 与 firebox 的表现层；不在 T34 附带修复 T5 recipe debt。
