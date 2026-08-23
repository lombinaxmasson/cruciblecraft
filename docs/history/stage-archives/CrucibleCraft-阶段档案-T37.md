# CrucibleCraft 阶段档案 · T37

> 阶段：T37 · ordinary_optional Assembler 校准与通用 compact family 底座
> 状态：● 已关闭（`T37_READY`，2026-08-24）
> 权威产物：[`tools/t37_readiness.json`](../../../tools/t37_readiness.json)
> 闭卡 profile：T37 builders、`census-replay`、隔离 `cruciblecraft_t37` GameTest、`census`、`recipes`。`machines` 仍因 T12 9→13 variants 历史债务红灯，不冒充通过

## 闭卡判据

- 固定 50 family：`cruciblecraft:assembler` `#0002`–`#0051`；独立生成根恰好 50 authored entries。
- 生产策略由实测派生为 **hybrid**（14 eager / 36 lazy / cache 8）；T14 Extruder 20 / 2,782 / 557 / 2,225 / 512 不变；hard ceiling 未提高。
- T35 基础分母 78,682 / 5,718 不变；其余 5,668 ordinary families 未批量宣布完成。
- T38 仍是冻结的 29-family Roaster wave；load opening 指向 T37 closing（authored 3,616）。
- 隔离 GameTest `cruciblecraft_t37` **4/4**。

## 验证

- T37 `--check` builders 与 assembler source `--full-replay`：退出 0；`proof_tier` 为 full replay。
- `python tools/verify.py integration --profile census-replay`：退出 0。
- `.\gradlew.bat test`：685 tests，0 failed。
- 双 `runData`：T37 独立生成根与 receipt 零非预期漂移。
- `.\gradlew.bat runGameTestServer -Pt37Recipes --no-daemon`：4/4 passed。
- `census` profile：T15–T18 acquisition 与 `t35_census_inputs.json` 已由 builder 重建；`t35_census.json` 仅 inputs hash 过期，排除 currentness 后正文相等，已重签并重建 topology/readiness。未改写 T35 READY 正文。
- `recipes` profile PASS。T14 Extruder 合同与 live T12 全局 datapack 计数解耦；compact `compare_gt6_recipes --check --reference-only` 通过；full GT6 replay 为 SKIP。
- `machines` profile：`machine_crafting_readiness.json` 仅 source hash 过期，已重签。T12 `--check` 仍因 HEAD builder 期望 9 variants、live `machine_tiers` schema v3 为 13 而失败。这是 machines 轨历史债务，不是 recipes 阻塞。

## 交接

唯一 active 内容卡转为 **T38 · Roaster 29-family fixed wave**。本档案不授权实现 T38，也不授权并行第二张内容卡。
