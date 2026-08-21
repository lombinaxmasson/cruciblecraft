# CrucibleCraft

NeoForge 1.21.1 的 GT6 风格工业模组。当前仓库是一个**源码阶段留档**：
`0.1.0-rc.1` 不是 `1.0.0`、不是 GA，也不表示面向玩家发行。

玩家安装与玩法材料仍在 [玩家指南](docs/CrucibleCraft-玩家指南.md)，但它们不构成
本次源码留档的发行声明。

## 当前状态

- T26–T31 的历史工程门禁有对应的 `_READY` 工件；详见
  [阶段档案 T31](CrucibleCraft-阶段档案-T31.md) 与
  [`tools/full_verification_report.json`](tools/full_verification_report.json)。
- 下一条内容路线是：地表石子可达性闭环 → 开局对象表现层 → 一个小型配方长尾族。
  它不进入 RC soak 或 GA；路线解释见
  [总体规划](CrucibleCraft-总体规划.md)。
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

```powershell
.\gradlew.bat test
python tools/run_python_tests.py --suite fast
python tools/run_python_tests.py --suite affected --path <changed-path>
```

修改 datagen 时连续运行两次 `.\gradlew.bat runData` 并比较生成树。封板才使用：

```powershell
$env:PYTHONUTF8 = "1"
python tools/run_full_verification.py --check
python tools/run_full_verification.py --record --new-session
```

`--check-ready` 校验的是已提交的历史 READY 快照，不适合作为内容开发时的
日常通过条件。完整 builder/replay 说明见 [tools README](tools/README.md)。

## 文档地图

- [总体规划](CrucibleCraft-总体规划.md)：唯一总体规划、项目规则、当前路线。
- [阶段档案](CrucibleCraft-阶段档案-T31.md)：阶段关闭证据；根目录的
  `CrucibleCraft-阶段档案-T*.md` 保留全部历史档案。
- [卡级计划](.plans/)：当前和近期 T 卡执行细节。
- [工具链说明](tools/README.md)：生成器、来源重放与验证规则。
- [来源与归属](CREDITS.md)、[第三方通知](NOTICE)、[变更记录](CHANGELOG.md)。
- [玩家指南](docs/CrucibleCraft-玩家指南.md)：玩家向说明，不是源码状态权威。

## 仓库布局

```text
src/main/java/          运行时实现
src/main/resources/     手写数据和资源
src/*_generated/        受工具维护的已跟踪生成资源
tools/                  构建器、来源投影和验证工具
.plans/                 卡级执行计划
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
