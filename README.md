# CrucibleCraft

[English](README.en.md)

CrucibleCraft 是一个面向 Minecraft 1.21.1 / NeoForge 的非官方 GregTech 6
移植项目。长期目标是在现代 Minecraft 中完整移植 GT6 的材料、机器、配方、
能源、物流与世界生成体系，同时保留可维护、可验证的数据驱动实现。

- [玩家指南](docs/current/player-guide.md)
- [项目状态](docs/current/project-status.md)
- [当前进度与规划](docs/current/roadmap.md)
- [问题反馈](https://github.com/icodestuljh/cruciblecraft/issues)

## 截图

> 截图与演示动图待补充。

<!-- 建议后续展示：世界生成、机器产线、物流网络和多方块结构。 -->

## 主要内容

当前代码库已经包含以下基础系统，并会继续朝 GT6 的完整覆盖扩展：

- 材料、前缀与矿物处理体系，以及由来源数据生成的大规模配方集合；
- 火、热、蒸汽、动能、旋转能与电力组成的多阶段能源链；电加热器 / 电引擎、
  LU 光纤、裂变堆芯与聚变控制器已进运行时（生存配方未齐，不算
  `player_complete`）；
- 从早期加工到高阶处理的机器族，以及蒸馏塔、大型锅炉和储罐等多方块；
- 大型矿脉、地下油气与地表资源等世界生成；
- 物品、流体和电力传输，以及管道、线缆、封面与自动化组件；
- 石油加工、天然气处理及其下游燃料与发电路径；
- EMI 配方展示与 Jade 方块信息集成；KubeJS 作为可选兼容项。

尚未完成的系统与后续顺序记录在
[冻结与未实现账本](docs/current/unimplemented-gap.md)。玩家可见的玩法说明和已知问题
见 [玩家指南](docs/current/player-guide.md)。

## 移植原则

CrucibleCraft 以 GT6 为主要来源和完整移植目标。面对 Minecraft 与 NeoForge
版本差异时，项目会区分三类信息：

- `SOURCE_BACKED`：可以直接追溯到固定上游来源的事实；
- `SOURCE_DERIVED`：根据来源数据或行为推导出的结果；
- `DESIGN_POLICY`：为兼容性、可玩性或实现约束作出的项目决策。

这些标签用于说明依据，而不是替代玩家文档。固定来源版本、许可证与第三方归属见
[CREDITS.md](CREDITS.md) 和 [NOTICE](NOTICE)。

## 构建与运行

需要 Java 21。项目面向 Minecraft 1.21.1、NeoForge 21.1.243，使用仓库自带的
Gradle Wrapper 构建。

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat test
```

构建产物位于 `build/libs/`。开发代理应配置在用户级
`~/.gradle/gradle.properties`，不要写入仓库。

## 开发方式

### 数据与规则

材料、配方和机器族的常规变化优先通过数据、目录和生成规则表达，避免为每个对象复制
一份 Java 实现。手写资源、生成资源和能力声明分别位于：

```text
src/main/resources/     手写数据与资源
src/*_generated/        由工具生成并纳入版本控制的资源
tools/waves/<slug>/     领域输入、production lock 与 census
tools/capabilities/     可验证的能力声明
```

配方内容大致经过以下管线：

```text
Source Pack（GT6 来源或明确的项目设计）
  -> 确定移植范围并冻结 production lock
  -> tools/recipe_bulk 编译 exact / exact_multi
  -> publication group / shard
  -> 运行时物化
```

新工作使用语义化 slug，例如 `logistics/fluid-network/basic-transfer` 和
`smelter/ordinary-closure`。早期编号只保留在历史档案和兼容映射中，不再用于新的
运行时 ID、配方路径或测试路径。

### 能力状态

项目用 capability 跟踪“规格是否明确、机制是否可运行、玩家是否能够完整使用”：

- `frozen`：范围、来源和依赖已经确定；
- `runtime_ready`：运行时机制已经可用，但内容或玩家路径可能尚未补齐；
- `player_complete`：生存获取、运行、界面、翻译、存档和验证路径均已闭合。

路线图只把 `player_complete` 计为玩家层面的实现进度。现行完成集合见
[项目状态](docs/current/project-status.md)。完整定义见
[能力交付流程](docs/current/capability-delivery-workflow.md)。

### 验证

常用入口如下：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile verification
python tools/verify.py integration --profile runtime-java
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile recipe-generators
python tools/verify.py integration --profile capability-runtime
python tools/verify.py integration --profile player-complete
python tools/verify.py promotion
```

`dev` 会根据工作区改动选择相关检查：普通 runtime Java 跑 JUnit，不跑 datagen；
datagen provider 或生成树才会跑两次 `runData`。`promotion` 只在能力晋级到
`player_complete` 时跑完整 GameTestServer 与客户端。需要运行哪些 profile、测试结果写到哪里，以及
`player_complete` 如何启动 GameTestServer 和客户端，见
[开发与验证指南](docs/current/verification.md)和
[工具链说明](tools/README.md)。

## 代码结构

主要运行时代码位于 `src/main/java/com/masson/cruciblecraft/`：

- `registry`、`content`、`machine`：方块、物品、处理机与内容目录；
- `recipe`、`recipe.gt`：RecipeMap、紧凑配方族与发布；
- `material`：材料定义、前缀目录与生成包；
- `energy`、`heat`、`steam`、`fluid`：能源与流体系统；
- `logistics`：管道、线缆、漏斗、封面和传输网络；
- `worldgen`：矿脉、油气与其他世界生成；
- `census`、`scale`、`gametest`：覆盖统计、载荷验证与 GameTest；
- `datagen`、`client`：数据生成与客户端集成。

现行规范位于 [docs/current/](docs/current/)，早期计划和阶段记录位于只读的
[docs/history/](docs/history/INDEX.md)。

## 文档

玩家与项目概览：

- [玩家指南](docs/current/player-guide.md)
- [总体规划](docs/current/roadmap.md)
- [当前已知问题](docs/current/known-issues.md)
- [变更记录](CHANGELOG.md)

开发与贡献：

- [开发与验证指南](docs/current/verification.md)
- [能力交付流程](docs/current/capability-delivery-workflow.md)
- [配方波次规范](docs/current/recipe-wave-workflow.md)
- [语义命名规范](docs/current/semantic-naming.md)
- [工具链说明](tools/README.md)
- [完整文档索引](docs/README.md)

## 许可证

源码与项目自有资源采用 [LGPL-3.0-or-later](LICENSE)。GT6 来源数据、第三方资产、
模板与各自许可证见 [CREDITS.md](CREDITS.md) 和 [NOTICE](NOTICE)。

EMI、Jade 与 KubeJS 均为可选且不捆绑的集成。
