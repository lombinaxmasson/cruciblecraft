# CrucibleCraft

[English](README.en.md)

Minecraft 1.21.1 NeoForge 上的 GT6 **风格**工业模组。材料、配方和机器的常规变化走数据和规则，而不是按对象复制 Java。

这不是 GT6 / GT6U 的完整移植，也不是给玩家发行的仓库。`0.1.0-rc.1` 只是历史工程候选，不是 `1.0.0`、GA 或「可以给玩家玩了」。来源事实、派生规则和设计决策分别记为 `SOURCE_BACKED`、`SOURCE_DERIVED`、`DESIGN_POLICY`。

玩法说明在 [玩家指南](docs/current/player-guide.md)，它不是源码或进度权威。

## 现在做到哪

进度只计 **capability** 的 `player_complete`，而且必须是当前 Git revision 上刚跑过的 fresh PASS。`frozen` / `runtime_ready` 是阶段，历史 `*_READY` 只证明当时那张卡的分母成立，不证明游戏里已经有这些东西。合同见 [能力交付流程](docs/current/capability-delivery-workflow.md)。

- 现行能力：`logistics/fluid-network/basic-transfer`（流体网基础传输，`player_complete`）
- 物品封面网两行（存储 / 传输）是 `runtime_ready`，不是七 kind 完成
- Generic / Dump 封面、作物 / 蜂箱、原版熔炉替换、核电等仍在 [冻结与未实现账本](docs/current/unimplemented-gap.md)
- 编号卡时代（T7–T49）已结束；不再签发新的里程碑号。不进行玩家发行、RC soak 或 GA

同一时刻只有一条 active delivery lane。工作包可以归档，**不生成** `*_READY`。

## 构建

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
.\gradlew.bat test
```

代理写在用户级 `~/.gradle/gradle.properties`，不要提交进仓库。`run-client-smoke/`、`run-wire-codec-*`、`build/`、本地 GT6 dump 也不是主树，不要提交。

## 开发

现行单位是 **semantic slug**（例如 `logistics/fluid-network/basic-transfer`、`smelter/ordinary-closure`）。配方导入链：

```text
Source Pack（GT6 / 未来 GT6U / design，append-only）
  -> 人判定范围并冻结 production lock
  -> tools/recipe_bulk 编译 exact / exact_multi
  -> publication group / shard
  -> CompactRecipeFamilyProvider 运行时物化
```

未知 `parameterized()` 模板 fail-closed（展开为空，不抛错）。Markdown 计划不是 production authority。能力声明在 `tools/capabilities/<slug>/capability.json`。

日常验证看**刚执行的命令结果**，不看提交库里的 currentness sidecar、seal 或历史 READY 报告：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile verification
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile capability-runtime
python tools/verify.py integration --profile player-complete
```

`dev` 按 dirty paths 选 profile。结果写到 gitignore 的 `build/verification/latest.json`（revision、命令、PASS/FAIL，没有工作流内容摘要）。`player-complete` 必须当场跑 isolated GameTestServer 和真实 `runClient`。

改什么跑什么见 [开发与验证指南](docs/current/verification.md)。完整工具说明见 [tools/README.md](tools/README.md)。

## 代码与仓库结构

运行时在 `src/main/java/com/masson/cruciblecraft/`：

| 包 | 职责 |
| --- | --- |
| `registry` / `content` / `machine` | 方块、物品、处理机、catalog 投影 |
| `recipe` / `recipe.gt` | RecipeMap、compact family、publication |
| `material` | 材料定义、前缀 catalog、生成包 |
| `energy` / `heat` / `steam` / `fluid` | 已选能量与流体链 |
| `logistics` | 管、缆、hopper、封面、物品/流体网 |
| `worldgen` | 矿脉、油气、石子等已选世界生成 |
| `census` / `scale` / `gametest` | 分母、载荷、GameTest |
| `datagen` / `client` | 数据和客户端 |

数据分三层，不要混：

```text
src/main/resources/     手写 authored 数据与资源
src/*_generated/        工具写出、入库跟踪的生成树
tools/waves/<slug>/     某一张历史/领域卡的 lock / census（只读档案或领域插件）
tools/capabilities/     现行能力声明
```

`build.gradle` 把若干 generated 树挂进 `sourceSets.main.resources`。语义波的 compact 配方在 `src/recipe_generated/` 和 `src/recipe_support_generated/`，不要往 `recipe/tXX/` 下写新内容。

工具也分两层：

- **现行入口**：`tools/verify.py`、`tools/recipe_bulk/`、`tools/build_capability_ledger.py`、`tools/build_player_complete.py`、`tools/check_no_workflow_hashes.py`
- **闭卡账本**：`tools/build_t*.py` 一类。文件名带着里程碑号，是为了历史收据稳定，不是现行作者 API

文档同样分开：[docs/current/](docs/current/) 是现行规范，[docs/history/](docs/history/INDEX.md) 是只读档案。

## 命名历史债

早期用 T7、T20、T45 这种编号卡推进，号漏进了路径、测试名和运行时 ID。活代码表层改成了语义路径：`recipe/mortar/`、`recipe/pipe/`、`assembler/compact`、capability slug。

剩下的 `TXX` 是未做完的迁移。唯一计划保留处是 `docs/history/card-plans/`。测试加载历史 fixture 时用 `archive/sealed/forward-v2/semantic_id_map.json` 映射到语义组名，不改冻结字节。完整批次见 [语义命名长期清单](docs/current/semantic-naming.md)。不要在内容卡里顺手改名。`GT6` / `gt6_*` 是 GregTech 6，不是卡号。

## 文档地图

- [总体规划](docs/current/roadmap.md)
- [能力交付流程](docs/current/capability-delivery-workflow.md)
- [冻结与未实现账本](docs/current/unimplemented-gap.md)
- [开发与验证指南](docs/current/verification.md)
- [Ordinary recipe wave 规范](docs/current/recipe-wave-workflow.md)
- [语义命名残留](docs/current/semantic-naming.md)
- [当前已知问题](docs/current/known-issues.md)
- [文档索引](docs/README.md) · [历史档案](docs/history/INDEX.md)
- [工具链说明](tools/README.md)
- [来源与归属](CREDITS.md) · [第三方通知](NOTICE) · [变更记录](CHANGELOG.md)
- [玩家指南](docs/current/player-guide.md)

## 许可证

源码与自有资源采用 [LGPL-3.0-or-later](LICENSE)。GT6/GTM 来源数据、默认 CC0 资产、logo 的 CC-BY-NC 例外、GT6 砧几何和 MDK 模板见 [CREDITS.md](CREDITS.md) 与 [NOTICE](NOTICE)。

Jade、EMI 与 KubeJS 是可选且不捆绑的集成，见 [`neoforge.mods.toml`](src/main/templates/META-INF/neoforge.mods.toml)。
