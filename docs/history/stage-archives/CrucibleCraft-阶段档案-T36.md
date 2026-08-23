# CrucibleCraft 阶段档案 · T36

> 阶段：T36 · 1.x 机器目标分母、统一 catalog 与全量样式闭合
> 状态：● 已关闭（`T36_READY`，2026-08-23）
> 权威产物：[`tools/t36_readiness.json`](../../../tools/t36_readiness.json)
> 闭卡 profile：`census-replay`、`census`（T35 基础分母只读）、隔离 `cruciblecraft_t36` GameTest

## 闭卡判据

- 独立冻结目标 **85 行 / 27 kind**（opening 33 稳定 id 全部保留）；catalog schema v3 是唯一投影源，不以当时整表 `machine_tiers.json` 定义 target。
- 三类分母互斥：material 76 行、EU voltage 5 行、TU host 4 行（含 missing `coagulator`）；禁止笛卡尔补全，不重命名 opening ids。
- `cruciblecraft:roaster` 进入 host 分母（29 family 不再因缺 host 自动 out_of_scope）；`EnergyType.TIME` 为 TU identity。
- opening 33 → closing 85；`target − catalog = 0`；generic 84 行与 `ModMachineVariants.ALL` / block / item 对齐（`bronze_crusher` 仍走独立 CrusherBlock）。
- T35 基础分母 **765 / 763 / 1,701 / 78,682 / 5,718** 不变；历史 T35 文件只读。
- 新 topology epoch：T36 complete → T37 calibration → recipe waves → storage bundles → 1.x exit。旧未启动 T38–T46 撤销；新 T38 从 recipe wave 重新编号。
- 权威 overlay：[`tools/t36_census_delta.json`](../../../tools/t36_census_delta.json)、[`tools/t36_readiness.json`](../../../tools/t36_readiness.json)、[`tools/t36_card_topology.json`](../../../tools/t36_card_topology.json)。

## 验证

- `python tools/build_t36_machine_target.py --check --full-replay` 与 T36 Python suites 通过。
- `python tools/build_t36_census_delta.py --check`、`python tools/build_t36_readiness.py --check` 通过。
- `python tools/verify.py integration --profile census-replay`：退出 0。
- `python tools/verify.py integration --profile census`：退出 0；T35 基础分母不变。
- `.\gradlew.bat runGameTestServer -Pt36Machines --no-daemon`：隔离 `cruciblecraft_t36` required GameTest **8/8** 通过。

本档案不把 T12 9→13 variants 历史债务写成已关闭，也不授权实现 T37 校准或批量 ordinary families。

## 交接

闭卡时唯一 active 内容卡转为 **T37 · ordinary_optional Assembler 校准**。T37 现已关闭；当前唯一 active 见 [T37 阶段档案](CrucibleCraft-阶段档案-T37.md) 交接节（T38）。本档案不授权并行第二张内容卡。
