# CrucibleCraft

[English](README.en.md)

本项目是把 GregTech 6 移植到 Minecraft 1.21.1 / NeoForge 上的一个尝试：材料、机器、配方、能源、物流、世界生成都照着钉死的上游版本走数据驱动的移植流程，目标是完整覆盖，而不是挑几个系统做个演示。

当前版本是开发快照 `0.1.0-test.20260919`，以源码仓库的形式发布。想直接上手玩，先看[玩家指南](docs/current/player-guide.md)。

- [项目状态](docs/current/project-status.md)
- [未实现与缺口](docs/current/unimplemented-gap.md)
- [总体规划](docs/current/roadmap.md)
- [问题反馈](https://github.com/icodestuljh/cruciblecraft/issues)

## 目前进了运行时的东西

材料、前缀和矿物处理这套配方都是按 GT6 来源生成的；火、热、蒸汽、动能、旋转能、电力几条能源链路都在跑，电加热器、电引擎、LU 光纤、裂变堆芯已经能用，大型热交换器、蒸汽涡轮和聚变作为 CrucibleCraft 的扩展也进了运行时（冷却器还没开）。加工机从青铜一路做到高档，蒸馏塔、大型锅炉、储罐这些多方块也在。世界生成这边有大型矿脉、地下油气、地表石子、石层和基岩矿。物流上是物品管、流体管、电缆、封面加自动化。原油和天然气可以蒸馏出燃油，送去引擎和燃气发电。第三方集成方面，EMI 能看配方，Jade 能看方块信息，KubeJS 是可选项——这些都不会强制捆绑进包里。

具体进度和缺口记在 [项目状态](docs/current/project-status.md)、[未实现与缺口](docs/current/unimplemented-gap.md)、[已阻塞项](docs/current/blocked.md) 里。

## 移植上的几条原则

GT6 是这个项目的主要来源，也是完整移植的目标。Minecraft 和 NeoForge 版本之间总有些差异需要取舍，遇到这种情况时，项目会把信息分成三类：

- `SOURCE_BACKED`：能直接追溯到某个固定上游来源的事实；
- `SOURCE_DERIVED`：从来源数据或行为推导出来的结果；
- `DESIGN_POLICY`：为了兼容性、可玩性或实现限制而做的项目决策。

这几个标签是用来说明依据的，不是替代玩家文档。固定的来源版本、许可证和第三方归属信息见 [CREDITS.md](CREDITS.md) 和 [NOTICE](NOTICE)。

## 构建与运行

需要 Java 21、Minecraft 1.21.1、NeoForge 21.1.243。内存建议留够 16 GiB 以上，第一次进世界要等它把东西都加载完。材料身份和铸造方块刚改过一版，建议开新档。

直接 clone 下来就能编译。`gt6_code/` 和 `gt6_referencable_port_code/` 是本机的参考源码树，不进 Git，改运行时代码用不着它们；只有要重放 GT6 来源数据或者搬贴图时才需要本地准备一份，具体见[代码树说明](docs/current/code-tree.md)。

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat test
```

构建产物在 `build/libs/` 下，当前是 `cruciblecraft-0.1.0-test.20260919.jar`。`distBeta` 会把文档一起打进 zip。如果要配代理，写在用户级的 `~/.gradle/gradle.properties` 里。

## 开发方式



### 数据与规则

材料、配方、机器族这些经常变动的东西，优先用数据、目录和生成规则来表达，而不是每加一个对象就手写一份 Java 实现。手写资源、生成资源、能力声明分别放在：

```text
src/main/resources/     手写数据与资源
src/*_generated/        由工具生成、纳入版本控制的资源
tools/waves/<slug>/     领域输入、production lock 与 census
tools/capabilities/     可验证的能力声明
```

配方内容大致走这样一条管线：

```text
Source Pack（GT6 来源或明确的项目设计）
  -> 确定移植范围，冻结 production lock
  -> tools/recipe_bulk 编译 exact / exact_multi
  -> publication group / shard
  -> 运行时物化
```

新工作用语义化的 slug 命名，比如 `logistics/fluid-network/basic-transfer`、`smelter/ordinary-closure`。早期的编号只留在历史档案和兼容映射里，新的运行时 ID、配方路径、测试路径都不再用它。

### 能力状态

项目用 capability 来跟踪一个东西的规格是否定清楚了、机制能不能跑起来；玩家能不能拿到手是单独的字段。试玩是项目级别的一个周期：

- `frozen`：范围、来源和依赖都定下来了；
- `runtime_ready`：运行时机制能用了，可以关闭这张卡，至于内容或获取路径是否补齐是另一回事；
- `survival_access`：单独描述获得性（`unreviewed` / `blocked` / `partial` / `complete` / `not_applicable`），不会卡住 runtime 的关闭。

路线图把 accepted 的 `runtime_ready` 算作机制进度，但试玩签收只认人工跑一遍 `runClient` 之后的结果。现行的能力集合见[项目状态](docs/current/project-status.md)，完整定义见[能力交付流程](docs/current/capability-delivery-workflow.md)。

### 验证

常用的验证命令：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile verification
python tools/verify.py integration --profile runtime-java
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile recipe-generators
python tools/verify.py integration --profile capability-runtime
python tools/playtest.py check
```

`dev` 会根据工作区改了什么来挑要跑的检查：普通的运行时 Java 改动只跑 JUnit，不碰 datagen；碰到 datagen provider 或生成树的改动才会跑两遍 `runData`。试玩签收是先人工跑一遍 `runClient`，再执行 `python tools/playtest.py record-accept`，CI 不会自动帮你跑 `runClient`。具体该跑哪些 profile、结果记在哪，见[开发与验证指南](docs/current/verification.md)和[工具链说明](tools/README.md)。

## 代码结构

平时开发只需要打开 `src/main`、`src/test`、`tools/waves/`、`tools/tests/` 和 `docs/current/`。本地的 `run*/` 目录、参考源码树和各种缓存都不算仓库结构的一部分，完整的地图在[代码树与工作副本](docs/current/code-tree.md)。

主要运行时代码在 `src/main/java/com/masson/cruciblecraft/` 下面：

- `registry`、`content`、`machine`：方块、物品、处理机和内容目录；
- `recipe`、`recipe.gt`：RecipeMap、紧凑配方族与发布；
- `material`：材料定义、前缀目录与生成包；
- `energy`、`heat`、`steam`、`fluid`：能源与流体系统；
- `logistics`：管道、线缆、漏斗、封面和传输网络；
- `worldgen`：矿脉、油气和其他世界生成；
- `census`、`scale`、`gametest`：覆盖统计、载荷验证与 GameTest；
- `datagen`、`client`：数据生成与客户端集成。

现行规范都放在 [docs/current/](docs/current/)，早期的计划和阶段记录作为只读档案留在 [docs/history/](docs/history/INDEX.md)。

## 文档

面向玩家和项目概览：

- [玩家指南](docs/current/player-guide.md)
- [总体规划](docs/current/roadmap.md)
- [当前已知问题](docs/current/known-issues.md)
- [变更记录](CHANGELOG.md)

面向开发和贡献：

- [开发与验证指南](docs/current/verification.md)
- [代码树与工作副本](docs/current/code-tree.md)
- [能力交付流程](docs/current/capability-delivery-workflow.md)
- [配方波次规范](docs/current/recipe-wave-workflow.md)
- [语义命名规范](docs/current/semantic-naming.md)
- [工具链说明](tools/README.md)
- [完整文档索引](docs/README.md)

报 issue 的时候带上版本号、复现步骤和日志。缺的零件按 GT6 里对应的对象补齐，不会拿别的东西顶替充数。

## 许可证

源码和项目自有资源用 [LGPL-3.0-or-later](LICENSE)。GT6 来源数据、第三方资产、模板各自的许可证列在 [CREDITS.md](CREDITS.md) 和 [NOTICE](NOTICE) 里。GT6 的默认资产在上游是 CC0 1.0；本项目不使用 GregTech 的 logo（CC-BY-NC-4.0）。

EMI、Jade 和 KubeJS 都是可选的集成，不会被捆绑进包里。