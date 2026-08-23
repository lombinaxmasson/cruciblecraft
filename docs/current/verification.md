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
| T35 census currentness / topology | `python tools/verify.py integration --profile census` | T35 范围内闭卡；历史分母只读 |
| T36 census overlay / T37 recipe overlay | `python tools/verify.py integration --profile census-replay` | T36 机器闭合后与 T37 校准闭合后；delta/readiness，不重跑 GT6 inventory |
| T37 配方 runtime 隔离 | `.\gradlew.bat runGameTestServer -Pt37Recipes --no-daemon` | R5 起；namespace `cruciblecraft_t37`，不进日常网格 |
| T36 机器 runtime 隔离 | `.\gradlew.bat runGameTestServer -Pt36Machines --no-daemon` | R7 起；namespace `cruciblecraft_t36`，不进日常 137 网格 |
| 历史 READY 收据 | `python tools/verify.py archive-inspect` | 不重算 |
| 玩家发行 | `python tools/verify.py release` | 仅未来发行卡 |

Windows 示例：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile worldgen
python tools/verify.py integration --profile census
python tools/verify.py integration --profile census-replay
python tools/verify.py archive-inspect
```

`dev` 根据 `git diff` 或 `--path` 选择 profile。无法映射的代码路径会列出需要声明的
scope，既不静默通过，也不升级到 110 个 builder。纯文档改动只跑 Markdown 链接和
profile 静态检查。

`census` 使用 compact/reference currentness；不会在 census、topology 与 readiness 层
重复读取 recipe source dump。`census-replay` 在 T36 之后校验 `t36_census_delta` / `t36_readiness`，在 T37 之后校验
`t37_census_delta` / `t37_readiness` overlay，不再重跑 T35 machine/recipe full inventory。
T36 机器 runtime 用 `-Pt36Machines`，T37 配方 runtime 用 `-Pt37Recipes`，都不要混进日常
`cruciblecraft` 网格或 `-Pt35Census`。

## 硬门与非门

硬门：GT6 compact evidence、JSON schema、生成树一致性、内容卡 load budget。

非门：README、总体规划、阶段档案、工作日志、历史 READY 快照的 live currentness。
T35 census 只把 `docs/current/roadmap.md` 记为 narrative 引用（文件必须存在），
不把它的内容 hash 纳入 `census` currentness 或 stale 门禁。

已知未修债务见 [`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)。

## Profile 边界与验证债务

内容卡只把其声明且实际改动的 integration profile 作为闭卡门：worldgen 卡运行
`--profile worldgen`，配方卡运行 `--profile recipes`。通过一个 profile 不得推导其他
profile 已通过。

已登记债务导致的 profile 失败不能被忽略、标为 passing，或随不相关内容卡继承为 READY。
如果该失败 profile 不属于当前卡的范围，它不阻断当前卡的**范围内**闭包，但必须同时满足：

1. 当前卡没有改动该 profile 的 owned paths，且其自身 required profile 全部通过；
2. 债务条目保持 `open`，闭卡记录写明失败 profile、债务 id 与其 scope-external 身份；
3. 不增加 bypass / allow-failure 开关，也不把“债务存在”改写成“验证通过”。

`T32-VD-001` 是上述规则的当前实例：T33 只以 worldgen profile 闭卡；严格 recipes
integration 仍是红灯，必须在 T35 的 recipe-debt preflight 修复后重跑并关闭该债务。

## 历史全量入口

`python tools/run_full_verification.py --check` 仍可手工运行，但它校验的是历史
单一 READY 证明，不是 T32 之后的日常开发门。CI 的 PR 路径使用 `verify.py dev`。
