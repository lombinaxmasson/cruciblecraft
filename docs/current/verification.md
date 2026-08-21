# 开发与验证指南

日常入口是 `tools/verify.py`。不要把历史 `full_verification_report.json` 或
Markdown 叙述当作当前代码必须匹配的证明。

## 改了什么，跑什么

| 改动 | 命令 | 何时升级 |
| --- | --- | --- |
| Markdown / `docs/` / 历史档案 | `python tools/verify.py dev` | 不升级 |
| `tools/` 验证入口、profile、policy | `python tools/verify.py dev` | 关闭验证卡时再跑 integration `--profile verification` |
| 材料 / 前缀 / oredict | `python tools/verify.py integration --profile materials` | 内容卡闭合 |
| 配方 / 化学 / 石油 | `python tools/verify.py integration --profile recipes` | 内容卡闭合 |
| 世界生成 / 矿脉 / 石子 | `python tools/verify.py integration --profile worldgen` | 内容卡闭合 |
| 机器 / 能量 / 容器 | `python tools/verify.py integration --profile machines` | 内容卡闭合 |
| 管道 / 覆盖板 / hopper | `python tools/verify.py integration --profile logistics` | 内容卡闭合 |
| 贴图 / 语言 / 客户端外观 | `python tools/verify.py integration --profile presentation` | 内容卡闭合 |
| 历史 READY 收据 | `python tools/verify.py archive-inspect` | 不重算 |
| 玩家发行 | `python tools/verify.py release` | 仅未来发行卡 |

Windows 示例：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile worldgen
python tools/verify.py archive-inspect
```

`dev` 根据 `git diff` 或 `--path` 选择 profile。无法映射的代码路径会列出需要声明的
scope，既不静默通过，也不升级到 110 个 builder。纯文档改动只跑 Markdown 链接和
profile 静态检查。

## 硬门与非门

硬门：GT6 compact evidence、JSON schema、生成树一致性、内容卡 load budget。

非门：README、总体规划、阶段档案、工作日志、历史 READY 快照的 live currentness。

已知未修债务见 [`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)。

## 历史全量入口

`python tools/run_full_verification.py --check` 仍可手工运行，但它校验的是历史
单一 READY 证明，不是 T32 之后的日常开发门。CI 的 PR 路径使用 `verify.py dev`。
