# CrucibleCraft 阶段档案 · T38

> 阶段：T38 · Roaster 29-family 固定波次
> 状态：● 已关闭（`T38_READY`，2026-08-25）
> 权威产物：[`tools/t38_readiness.json`](../../../tools/t38_readiness.json)
> 闭卡 overlay：`T38_CENSUS_DELTA_READY`；`unique_active_card = null`；T39 未签发

2026-08-24 曾签发过一次 `T38_READY`，因关闭门不诚实撤回。2026-08-25 早先 READY 因 `MaterialRegistrationGate` prefix ID 比较崩溃、GameTest receipt 可陈旧通过、load delta 少计支撑配方、以及收据指向被忽略的 `run-t38-recipes` 日志，不得作为闭卡证据。当前 READY 由 694/0 单测、schema 3 收据（committed `tools/t38_gametest.log`）与 T14 authored **3,664** / eager **16,626** 重新派生。

## 闭卡判据

- 固定 29 family：`cruciblecraft:roaster` `#0000`、`#0001`、`#0003`–`#0029`；独立生成根恰好 29 authored entries / 73 logical relations。`#0002` 不存在，未补 placeholder。
- Player path 对照当前 T21 typed identity closure，不是命名空间或流体白名单：`inputs_reachable = 73`，`relations_with_unreachable_inputs = 0`。
- 生存取得路径是加法，不改写 T20 129 行核心 catalog：3 条 GT6 大脉（`ore.large.gold` / `ore.large.platinum` / `ore.large.molybdenum`）覆盖 arsenopyrite、chalcopyrite、cooperite、molybdenite；风箱暴露 `cruciblecraft:air`，氧气走可达 T5 电解；V/Nb/Ta/blaze/diamantine 走 mortar / shredder / GT6 `AnyDiamond` 回收。注册矿材 **147**、矿块 **294**、catalog 文件 **269**、全部 worldgen 文件 **280**。
- 生产策略由 73-row 实测派生为 **on_demand**（compact 0 eager / 73 lazy / T38 cache 16）；累计 cache closing **24**。未套用 T37 的 14 / 36 / 8。T14 hard ceiling 未提高。lookup 73 对 soft 64 为 `SOFT_BUDGET_EXCEEDED`，hard 128 未提高。
- T35 基础分母 78,682 / 5,718 不变；ordinary recipe gap 5,668 → **5,639**。T14 authored closing **3,664**（compact 29 + 支撑 19）；eager closing **16,626**（compact 0 + GT 支撑 15）。
- T36 的 2 条 Roaster bootstrap 保持独立，不计入 29/73。
- 隔离 GameTest `cruciblecraft_t38` 收据 schema 3、5/5 PASS；绑定 `MaterialRegistrationGate`、T38 配方资源树与 committed `tools/t38_gametest.log`；源码字符串、自指指纹或被忽略的 run 目录都不是通过。
- `unique_active_card = null`；T39 仅 `next_issue_id`，host/family 未预分配；storage 仍在 recipe gap = 0 之后。

撤回时纠正并保留的门禁：`full_replay_dump_verified = dump_verified and not dump_skip`；GameTest 门消费 `tools/t38_gametest_receipt.json` 且必须对照 committed `tools/t38_gametest.log`；累计 cache 必须加 T37 的 8；JSON form id 必须规范成 `MaterialPrefix.id()`；支撑配方计入 authored / eager delta。

## 验证

- T38 `--check` builders（publication / census / load projection / topology / readiness）与 materialization decision 可复算：退出 0。Roaster source full replay 与 GameTest receipt `--check` 对照 committed `tools/t38_gametest.log`，不是 SKIP，也不依赖 `run-t38-recipes/`。
- T38 Python 合同测试 64 项通过（2 skip）。worldgen / reachability / ore-chain / form-gate 回归 41 项通过。
- `WorldgenCatalogResourceTest` 当前通过（147 / 294 / 269 / 280）。全量 `.\gradlew.bat test`：**694 / 0**。
- `.\gradlew.bat runGameTestServer -Pt38Recipes --no-daemon`：5/5 passed；收据 `tools/t38_gametest_receipt.json` schema 3，证据日志 `tools/t38_gametest.log`（UTF-8，受版本控制）。`run-t38-recipes/` 不是证据。
- 未宣称 `census` / `recipes` / `machines` integration profile 在本次闭卡通过。

## 交接

T38 已关闭。T39 现可另行签发，但必须先从当前 census overlay 重算 remaining recipe gap，再冻结 host 与 family。本档案不预分配 T39 内容，也不授权并行第二张内容卡。Storage 仍在 recipe gap = 0 之后。
