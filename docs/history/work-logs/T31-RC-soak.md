# T31 RC soak ledger

> 候选：`0.1.0-rc.1` · 绑定：`T31_READY`（2026-08-21 `--check-ready`）
> RC1 不是 GA。本表只收 release blocker；一般体验进 1.x。
> 每个 hotfix：full record → 重建 jar/zip → 更新派生 hash。不要手改 SHA-256。

## 接受范围

- crash / hang
- 存档损坏或无法加载（含复制的 beta.1 世界）
- 复制不同步或丢件
- 协议 / handshake（`NETWORK_VERSION` 仍为 `"1"`）
- 工业主链断裂
- hard-load 回归（T14 硬顶、eager 21000、datapack 6600）

## 不接受为本 RC 阻断

- 4.5 体验 / `post_beta_polish`（仍在 T26 known-issues）
- 新机器、方块、物品、材料、RecipeMap
- barrels / mass storage / drawers / fluid funnel / canner 等 post-1.0

## Opening 阻断集

| id | 类 | 状态 |
|---|---|---|
| （无） | release blocker | G0 `release_blockers` 为空 |
| T24-F003 | 规模 | 当前 `CLOSED`（`tools/t31_scale_recheck.json`）；T24 历史 SKIP 未改 |
| T24-F005 | 规模 | 当前 `CLOSED`（同上） |

## Soak 记录

| 日期 | 现象 | 处置 |
|---|---|---|
| 2026-08-21 | 进入 soak | 无 opening blocker |
