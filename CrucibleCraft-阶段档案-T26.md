# CrucibleCraft 阶段档案 · T26

> 阶段：T26 · 公开 Beta 门禁  
> 状态：✅ `T26_READY`  
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`  
> 权威产物：`tools/t26_readiness.json` · 台账：`tools/t26_known_issues.json` · 本地化：`tools/t26_localization_ledger.json`  
> 试玩包：`cruciblecraft-0.1.0-beta.1.jar` · 玩家指南：[docs/CrucibleCraft-玩家指南.md](docs/CrucibleCraft-玩家指南.md)

## 1. 关闭判据

T26 是公开 Beta 门禁。前置工程（本地化分账、0.1.0-beta.1 打包、未来版本隔离
GameTest）已在 OPEN 期完成。收尾把试玩 known-issue、O-15、anvil_bend 与
crucible freeze、readiness 与一次绑定完整验证写成 `T26_READY`。本卡 publication
delta 为 0/0/0：不新增配方或注册对象。

## 2. T26a–d · 已落地证据

| 阶段 | 证据 |
|---|---|
| T26a 玩家路径 | 试玩观察写入 4.5 P0–P9；无一 `blocks_beta` |
| T26b 服端/存档 | GameTest 120，含 `t26UnknownFutureProcessingVersionQuarantinedAndPreserved` |
| T26c 发布包 | `mod_version=0.1.0-beta.1`；CHANGELOG / CREDITS / 玩家指南 |
| T26d 本地化 | `en_us` 3,173 / `zh_cn` 874；材料 208 + 1,566 `post_1_0`；O-15 关闭 |

## 3. T26e · freeze 与三轴

**Known-issue 台账**（15 条，`blocks_beta = 0`）：

- CC-4.5-P0–P9：`post_beta_polish`，owner = 4.5，不升 Beta 阻断；
- T24-F001/F002/F004：`non_blocking`，无后续动作；
- T24-F003/F005：`non_blocking`，owner = T27 RC，`recheck_point = T27 RC candidate`，本卡不复测。

**Freeze**

- O-15：按玩家可见域关闭；
- `anvil_bend_big` / `anvil_bend_small`：`post_1_0` 预留槽位，v1 替代路径为 `cruciblecraft:bender`；
- `crucible`：仍为 `v1_required`，owner = T27，implementation = `none`，新 controller/block id 不得与单块 `cruciblecraft:crucible` 冲突。

**Closure** — 台账完整，freeze 词汇合法，打包与未来版本隔离证据在。

**Fidelity** — 零新注册；主链无未声明 PLACEHOLDER；4.5 项保持体验债。

**Load** — publication 18,882 / 16,657 / 2,225；headroom 2,118（沿用 T25 logical 口径）；F003/F005 的 SKIP 契约转交 T27 RC。21,000 轴的 eager/logical 拍板属于 T27。

## 4. 验证与交接

关闭绑定：

```text
PYTHONUTF8=1 python tools/rebuild_artifacts.py --verify
PYTHONUTF8=1 python tools/run_python_tests.py --suite closure
.\gradlew.bat test
.\gradlew.bat runGameTestServer
PYTHONUTF8=1 python tools/run_full_verification.py --record --new-session
PYTHONUTF8=1 python tools/run_full_verification.py --record --new-session
PYTHONUTF8=1 python tools/run_full_verification.py --check-ready
```

`T26_READY` 绑定一次完整验证会话：报告 `status = READY` 且
`t26_readiness_acceptance.status = "T26_READY"`。GameTest 119→120 需要双
record 收敛 report-owned runtime。

当前执行入口为 T27 portfolio freeze。4.5 不插卡、不占 active T。

**交给 T27**：

- 七表 + T20–T26 READY / 台账 / freeze；
- 剩余 `v1_required` 至少包括 `crucible` 与 O-36；
- F003/F005 在 RC candidate、≥16 GB 声明环境复测；
- 载荷口径拍板（eager 21,000 权威 vs T22 logical=21,000 stale）；
- 不得生成 GT 行或注册对象。
