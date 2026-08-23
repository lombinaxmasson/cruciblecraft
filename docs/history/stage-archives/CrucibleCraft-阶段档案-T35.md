# CrucibleCraft 阶段档案 · T35

> 阶段：T35 · 1.x 全域 census 与预算基线（含 T35R 修复）
> 状态：● 已关闭（`T35_CENSUS_READY`，2026-08-22）
> 权威产物：[`tools/t35_readiness.json`](../../../tools/t35_readiness.json)
> 闭卡 profile：`census`、`census-replay` 与隔离 `cruciblecraft_census` GameTest

## 闭卡判据

- 聚合 census 保持 **765** T13 identities、**763 / 1,701 / 194** exclusion 分母、
  **20,553** runtime expected/mapped、**78,682** ordinary-optional rows / **5,718**
  recipe families，以及 `publication_delta = 0/0/0`。
- T35R 为全部 **8,996** identities 签发 `portfolio_scope`；`unscoped`、scope、owner、
  dependency 和 cycle validator 均为 0。
- Machine receipt 固定 **33 variants / 11 kinds** 精确 source lineage：RU/KU/HU material
  rows、EU 三行 voltage pilot 与 TU/TIME 语义彼此分离，结论均为 A；没有由此生成 Epoch A
  machine/EU/TU 内容卡。
- Storage receipt 保留 **28 / 624** 与 logistics **1 / 1**。Chest、Safe、Tank、Pump、
  Sorting 转 post-1.x；Fluid Container 仍为 measurement-gated candidate；Hopper 与九个
  storage family 为 in-scope。
- Epoch A 只含未启动 T38–T46 storage cards；固定节点 T36/T37 未重编号，每张 generated
  card 均依赖 T37。Epoch B 仅允许 append。

## 验证

- `python tools/verify.py integration --profile census-replay`：退出 0；machine full replay
  0.142s、recipe full replay 9.029s，各一次。
- `python tools/verify.py integration --profile census`：退出 0（925.925s）；compact builders、
  T35 Python suites 与 `gradle test` 通过。
- `.\gradlew.bat runGameTestServer -Pt35Census --no-daemon`：退出 0；隔离
  `cruciblecraft_census` required GameTest **1/1** 通过。
- `python tools/verify.py integration --profile recipes`：退出 0（162.458s）。

启动 GameTest 时保留既有 online T14 soft-budget warning；T35 不改变 hard ceiling 或内容
publication，且该警告不构成玩家发行或后续内容卡的通过声明。

## 交接

唯一 active 内容卡转为 **T36 · 机器注册与等级矩阵重构**。T37 及 T38–T46 保持
`started = false`；本档案不授权并行实现任何后续卡。
