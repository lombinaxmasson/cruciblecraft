# CrucibleCraft

NeoForge 1.21.1 的 GT6 风格工业模组。当前仓库是一个**源码阶段留档**：
`0.1.0-rc.1` 不是 `1.0.0`、不是 GA，也不表示面向玩家发行。

玩家安装与玩法材料仍在 [玩家指南](docs/current/player-guide.md)，但它们不构成
本次源码留档的发行声明。

## 当前状态

- T26–T31 的历史工程门禁有对应的 `_READY` 工件；详见
  [阶段档案 T31](docs/history/stage-archives/CrucibleCraft-阶段档案-T31.md) 与
  只读收据 [`tools/full_verification_report.json`](tools/full_verification_report.json)。
- T35 1.x census 与预算基线已闭卡：8,996 identities 都有 portfolio scope，runtime
  expected/mapped 均为 20,553，且 `cruciblecraft_census` GameTest 1/1 通过。历史 T35
  文件只读。
- T36 把 1.x 机器目标冻成 85 行 / 27 kind（opening 33 稳定 id 保留），`machine_tiers.json`
  schema v3 是唯一投影源；隔离 GameTest `cruciblecraft_t36` 8/8。旧未启动 T38–T46
  storage-first 编号已撤销；新 topology 是 T37 校准 → recipe waves → storage。
- T37 Assembler 50-family 校准已关闭，生产策略由实测派生为 hybrid（14 eager / 36 lazy /
  cache 8）。T38 Roaster 29-family 固定波次已关闭（`T38_READY`，compact on_demand 0/73/16；
  T14 authored 3,664 / eager 16,626）；
  T39 原 157/250 host-complete 签发已撤回为 test fixture；22-family / 32-relation
  production lock 与 T39-Repair 已关闭（`T39_READY`），T40 尚未签发。路线解释见
  [总体规划](docs/current/roadmap.md)。
- GT6 式石子开局和第一小时表现层仍未闭环；不要把历史 portfolio 的
  `v1_work_set = []` 解读为“模组已完整可玩”。

## 构建要求

| 工具 | 版本 |
| --- | --- |
| Java | 21 |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.243 |
| Gradle Wrapper | 9.2.1 |
| ModDevGradle | 2.0.142 |

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

如果网络环境需要代理，请把代理配置放在用户级
`~/.gradle/gradle.properties`；仓库不提交任何机器本地代理。

## 日常验证

改什么跑什么，见 [开发与验证指南](docs/current/verification.md)。最短入口：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile worldgen
python tools/verify.py archive-inspect
```

`dev` 只跑受影响的 Python/Java 测试和文档链接检查。内容卡闭合才使用
`--profile`。玩家发行才使用 `release`。不要把 `--check-ready` 当作日常通过条件。

完整 builder/replay 说明见 [tools README](tools/README.md)。

## 文档地图

- [总体规划](docs/current/roadmap.md)：唯一总体规划、项目规则、当前路线。
- [开发与验证指南](docs/current/verification.md)：改 X 跑什么、何时升级。
- [当前已知问题](docs/current/known-issues.md)：验证债务台账入口。
- [文档索引](docs/README.md)：当前规范与历史档案入口。
- [历史档案](docs/history/INDEX.md)：工作日志、阶段档案、关闭计划。
- [工具链说明](tools/README.md)：生成器、来源重放与验证规则。
- [来源与归属](CREDITS.md)、[第三方通知](NOTICE)、[变更记录](CHANGELOG.md)。
- [玩家指南](docs/current/player-guide.md)：玩家向说明，不是源码状态权威。

## 仓库布局

```text
src/main/java/          运行时实现
src/main/resources/     手写数据和资源
src/*_generated/        受工具维护的已跟踪生成资源
tools/                  构建器、来源投影和验证工具
docs/current/           当前路线与开发指南
docs/history/           只读历史档案
docs/decisions/         ADR 与仍有效的专题决策
```

`4.5Fix/`、`build/`、`run*/`、本地 GT6/GTCEu dump 和 `src/src/` 重复树不属于
canonical 主树，也不应提交。

## 许可证与来源

CrucibleCraft 的源码与自有资源采用
[LGPL-3.0-or-later](LICENSE)。GT6/GTM 的来源数据、默认 CC0 资产、logo 的
CC-BY-NC 例外、TFC 砧几何和 MDK 模板的独立许可均见
[CREDITS.md](CREDITS.md) 与 [NOTICE](NOTICE)。

Jade、EMI 与 KubeJS 是可选且不捆绑的集成；详见
[`neoforge.mods.toml`](src/main/templates/META-INF/neoforge.mods.toml)。
