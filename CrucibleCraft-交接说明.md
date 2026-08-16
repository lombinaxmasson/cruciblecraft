# CrucibleCraft 项目交接说明（2026-08-16）

> 收件人：原作者 · 交接时状态：T20–T26 全部 `_READY`，当前入口 T27 portfolio freeze

## 一、怎么接手

两个传输件，任选其一：

1. **`cruciblecraft-t24-scale-baseline.bundle`**（270 MB，git bundle，含完整历史与全部分支）
   - 全新接管：`git clone cruciblecraft-t24-scale-baseline.bundle`
   - 合并进你自己的仓库：`git fetch ../cruciblecraft-t24-scale-baseline.bundle "refs/heads/*:refs/remotes/交接/*"`，再自行合并/切分支
2. **`cruciblecraft-t24-工作树.zip`**（无历史的干净工作树，直接翻文件用）

所有新工作都在分支 **`t24-scale-baseline`**（T26 收尾与 T27 开卡继续在此分支）；`master` 停在 T11–T17。

## 二、进度速览

| 卡 | 状态 |
|---|---|
| T20–T26 | 全部 `_READY`，阶段档案在仓库根目录 |
| T27 | **进行中**：v1.0 portfolio freeze；零内容分类卡 |

试玩包：`cruciblecraft-0.1.0-beta.1.jar` + 《docs/CrucibleCraft-玩家指南.md》。

## 三、接手必改项（重要）

- Windows GBK 控制台下跑 `tools/rebuild_artifacts.py` / `tools/run_full_verification.py --record` 需前缀 **`PYTHONUTF8=1`**，否则子进程输出解码崩溃。

## 四、T26 交给 T27 的冻结项

- Known-issue 台账 `tools/t26_known_issues.json`：4.5 P0–P9（`post_beta_polish`）+ T25 五条；F003/F005 owner = T27 RC。
- O-15 已关闭；`anvil_bend_*` 为 `post_1_0` 预留槽位；`crucible` 仍为 `v1_required`，owner = T27。
- 4.5 不插卡、不占 active T。
- 载荷 21,000 口径拍板属于 T27（eager 轴权威；T22 logical=21,000 为 stale）。

## 五、根目录杂项

- 根目录 34 个 `*.7z` 是未跟踪的日常备份；`build/`、`run/` 也未跟踪。
