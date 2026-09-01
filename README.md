# CrucibleCraft

[English](README.en.md)

Minecraft 1.21.1 NeoForge 上的 GT6 **风格**工业模组。常规材料、配方和机器变化优先走数据和规则，而不是按单个对象复制 Java。

这不是 GT6 / GT6U 的完整移植，也不是面向玩家的发行仓库。`0.1.0-rc.1` 只是历史工程候选版本，不是 `1.0.0`、GA 或「可以给玩家玩了」。来源事实、派生规则和设计决策分别记为 `SOURCE_BACKED`、`SOURCE_DERIVED`、`DESIGN_POLICY`。

玩法说明在 [玩家指南](docs/current/player-guide.md)，它不是源码状态权威。

## 当前状态

- **1.x 已声明的 portfolio 已联合退出**（`ONE_X_JOINT_EXIT_READY`）。current execution gap = 0；deferred ledger = 0（1,817 条 Smelter MTE recovery 完成 + 28 条独立 post-1.x scope）。
- **源能力对照图已关闭**（`SOURCE_CAPABILITY_MAP_READY`）。113 行机制对照（22 行 seed + 91 行展开），不是 recipe census。
- **下一张已签发、尚未实现**： [通用 Source Pack 导入器](docs/history/card-plans/active/通用Source-Pack导入器详细计划.md)（slug `portfolio/generic-recipe-generator`）。它只收掉「新 Source Pack → canonical source / compile spec」之间的手工胶水，不重造已经完成的 `recipe_bulk` 编译器，不发配方。
- 核能 Track C 仍 `started = false`。不签发新的里程碑编号。不进行玩家发行、RC soak 或 GA。

历史卡的 `_READY` 只证明当时那张卡的分母成立，不证明模组已完整可玩或 GT6 做完了。路线与规则以 [总体规划](docs/current/roadmap.md) 为准。

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
```

代理写在用户级 `~/.gradle/gradle.properties`，不要提交进仓库。

## 开发模式

编号卡时代（T7–T49）已经结束。现行单位是 **semantic slug**（例如 `smelter/ordinary-closure`、`portfolio/source-capability-map`），同一时刻只有一张 active child。

配方导入的现行链：

```text
Source Pack（GT6 / 未来 GT6U / design，append-only）
  -> 人判定范围并冻结 production lock
  -> tools/recipe_bulk 编译 exact / exact_multi
  -> publication group / shard
  -> CompactRecipeFamilyProvider 运行时物化
```

`parameterized()` 仍然 fail-closed。编译器已经通用；缺的是新 pack 接到 lock 之前的登记胶水，也就是上一节那张导入器卡。Markdown 计划不是 production authority：机器可读的是 `tools/waves/<slug>/` 里的 lock、census、readiness 和 seal。

日常验证：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile recipes
```

改什么跑什么见 [开发与验证指南](docs/current/verification.md)。内容卡闭合才上 `--profile`。`release` 和历史 `--check-ready` 不是日常绿灯。完整工具说明见 [tools/README.md](tools/README.md)。

## 代码与仓库结构

运行时在 `src/main/java/com/masson/cruciblecraft/`：

| 包 | 职责 |
| --- | --- |
| `registry` / `content` / `machine` | 方块、物品、处理机、catalog 投影 |
| `recipe` / `recipe.gt` | RecipeMap、compact family、publication |
| `material` | 材料定义、前缀 catalog、生成包 |
| `energy` / `heat` / `steam` / `fluid` | 已选能量与流体链 |
| `logistics` | 管、缆、hopper、封面 |
| `worldgen` | 矿脉、油气、石子等已选世界生成 |
| `census` / `scale` / `gametest` | 分母、载荷、GameTest |
| `datagen` / `client` | 数据和客户端 |

数据分三层，不要混：

```text
src/main/resources/     手写 authored 数据与资源
src/*_generated/        工具写出、入库跟踪的生成树
tools/waves/<slug>/     某一张卡的 lock / census / seal
```

`build.gradle` 把若干 generated 树挂进 `sourceSets.main.resources`。语义波的 compact 配方在 `src/recipe_generated/` 和 `src/recipe_support_generated/`，不再往 `recipe/tXX/` 下写新内容。

工具也分两层：

- **现行入口**：`tools/verify.py`、`tools/recipe_bulk/`、`tools/build_ordinary_wave.py`、`tools/build_recipe_bulk.py`
- **闭卡账本**：`tools/build_t*.py`、`tools/t16_*.json` 一类。文件名带着里程碑号，是为了历史 `--check` 收据稳定，不是现行作者 API

文档同样分开：[docs/current/](docs/current/) 是现行规范，[docs/history/](docs/history/INDEX.md) 是只读档案。`4.5Fix/`、`build/`、`run*/`、`run-wave-*/`、本地 GT6 dump 和 `src/src/` 重复树不是 canonical 主树，不要提交。

## 命名历史债

早期用 T7、T20、T45 这种编号卡推进。后来证明编号会漏进路径、测试名和运行时 ID，新工作又很难从旧特例复用，所以活代码表层改成了语义路径：`recipe/mortar/`、`recipe/pipe/`、`recipe/ingot_form/`、wave slug。

剩下的 `TXX` **不是没看见，是故意留下的**：

- 绑定 ID（NBT、family id、闸门字段，例如 `t14_extruder`、`t11_materials/`）
- 约 396 个 `tools/build_t*.py` 和对应收据
- 测试夹具 `t39_*_fixture` … `t45_*_fixture`（和 seal 字节绑在一起）
- 贴图目录 `t34_gt6/`、部分 GameTest 方法名

没有专门的迁移卡，不要整批改名。新配方、新机器、新工具不要从这些 T 号文件抄起步。完整清单和再动手顺序见 [语义命名残留](docs/current/semantic-naming.md)。`GT6` / `gt6_*` 是 GregTech 6，不是卡号。

## 文档地图

- [总体规划](docs/current/roadmap.md)
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
