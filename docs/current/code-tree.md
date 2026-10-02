# 代码树与工作副本

日常改运行时、验证和文档时只需要打开这些路径。Minecraft 运行实例、缓存，
以及对照上游用的可选检出，都不是这个仓库的一部分。

## 日常入口

| 打开 | 用途 |
| --- | --- |
| `src/main/java/com/masson/cruciblecraft/` | 运行时 Java |
| `src/main/resources/` | 手写数据与资源 |
| `src/addons/crops/` `src/addons/foods/` | 可选作物 / 食物附属（独立 jar） |
| `src/test/java/` | JUnit |
| `tools/waves/<slug>/` | 波次输入、production lock、census |
| `tools/tests/` 且列入 `python_test_policy.json` 的 `active_test_modules` | 现行 Python 测试 |
| `docs/current/` | 现行规范（含 [材料身份](material-prefix-identity.md)） |

验证入口是 `python tools/verify.py`。Python 套件只有 `active`、`affected`、
`modules`，见 [tools/README.md](../../tools/README.md) 和
[开发与验证指南](verification.md)。

## 工作副本里不算仓库的东西

Explorer 里会看到很多 Git 忽略的目录。它们不是仓库结构的一部分：

- `run/` 和 `run-*/`：Gradle 客户端 / GameTest 实例。日常只保留一个 `run/`。
- 参考源检出：见下一节。仓库里没有这些目录；克隆后的日常开发也不需要它们。
- `build/`、`bin/`、`.gradle/`、`logs/`、`tmp-baked_textures/`、`src/src/`、
  `__pycache__/`、崩溃日志：缓存或误生成副本。
- `tools/_tmp_*.py` 和 `tools/scratch/`：本地草稿。坩埚卡结束前不要删除现有
  `_tmp_extract_gt6_*.py`；结束后再决定并入正式提取器或丢掉。

Cursor 的 `.vscode/settings.json` 会把运行目录和缓存从资源管理器里藏起来。
搜索范围仍包含你另外放在工作副本里的参考源检出。

## 参考源

这些上游不随本仓库发布。普通编译、运行和阅读现行文档都不需要检出。
只有对照 GT6 行为、迁贴图，或重算配方覆盖时才需要，并且必须停在下表的 revision，
不要跟上游的更新提交。类路径都相对于对应检出的 `src/main/java`。

| 用途 | 上游 | 固定 revision | 脚本查找位置（可选检出，不入库） |
| --- | --- | --- | --- |
| Java / tick 权威 | [GregTech6/gregtech6](https://github.com/GregTech6/gregtech6) | `3703e40308c8c030763fd6297dea8b210d2a77b1` | `gt6_code/gregtech6` |
| 贴图 | [wolfram0108/gregtech6_w](https://github.com/wolfram0108/gregtech6_w) | `936083c247a70b1bbc5f19996a83d75c27d196e2` | `gt6_referencable_port_code/gregtech6_w` |
| 管道命名与几何（不是运行时依赖） | [GregTechCEu/GregTech-Modern](https://github.com/GregTechCEu/GregTech-Modern) | `de5d2c4a4c863b94a10bfb5d0839df2de8246628` | `gtceu_code` |
| GT6U（不在当前范围） | [GregTech6-Unofficial/GregTech6-Unofficial](https://github.com/GregTech6-Unofficial/GregTech6-Unofficial) | `4972d0468ee2ea0e896af1e4afe4018d4e2294e6` | `gt6u_code` |
| TFRU 附加。不是 GT6 权威 | [kuzuanpa/kTFRUAddon](https://github.com/kuzuanpa/kTFRUAddon) | `75cebb71abb9b5ac5ceb447e3324bd161b6fa7ab` | `ktfruaddon/kTFRUAddon` |
| TFRU 的 Waila 表面 | [kuzuanpa/gregtech6-TFRU](https://github.com/kuzuanpa/gregtech6-TFRU) | `35402b05b35d4c7e666233cb37f99d305eeffc8b` | `gt6_tfru/gregtech6-TFRU` |

kTFRUAddon 里只依赖 GT6、不绑 Advanced Rocketry / TerraFirmaCraft 的部分见
[ktfruaddon-gt6-surface.md](ktfruaddon-gt6-surface.md)。
许可证和资产说明见 [CREDITS.md](../../CREDITS.md)。

配方 dump 不是上游仓库，而是从钉住的 GregTech 6 导出的配方 JSON。
重算脚本在 `gt6_dump/gt6_recipe_dump` 找它。
覆盖页的 `--check` 不需要它；只有重新核对源行归属时才需要。

## Python 工具生命周期

不要再往 `tools/` 根目录堆新文件。新代码按层放置：

| 层 | 放置 |
| --- | --- |
| 共享库 | 已有 `tools/*.py` 模块，例如 `atomic_io.py`；不要新开平行根文件 |
| GT6 导入 / 解析 | 现有 `gt6_*.py` / `import_gt6_*.py`，或未来的 `tools/gt6/` |
| 确定性生成器 | `tools/build_*.py` 或已有子包；输出必须写进冻结的生成根 |
| 验证 | `tools/verify.py`、`tools/run_python_tests.py`、profile JSON |
| 波次账本 | `tools/waves/<slug>/` |
| 仓库瘦身 | `tools/repo_slimming/`：新增大文件棘轮、历史删除清单、测量记录 |
| 测试 | `tools/tests/`，并登记 `active` / `manual_replay` / `historical` |

路径引用先查 `tools/python_test_policy.json` 和
`tools/verification_profiles.json`，再移动文件。一次只迁一个领域。

本轮试点：`tools/build_recipe_fresh.py` →
`tools/recipe_bulk/build_recipe_fresh.py`。新的 fresh-recipe 检查跟着
`recipe_bulk` 走，不再在 `tools/` 根上新增同名脚本。

### Python 测试分层

`tools/python_test_policy.json` 的 `test_tiers` 覆盖全部 `tools/tests/test_*.py`：

- `active`：`active_test_modules`，CI / `verify.py` 会跑。
- `manual_replay`：本地 dump / 源码回放或手工审计，缺输入就 SKIP；不删。
- `historical`：编号卡、currentness / seal / 已关闭 portfolio。不进 CI，不删。

未分层的新 `test_*.py` 会让 `test_python_test_workflow` 失败。

### 关卡证据不进 git

已关闭波次的 GameTest 日志和巨型输入（`tools/waves/**/source.json`、
`dump_slice.json`、player-path / census-delta / load-projection 等）写在
`.gitignore` 里。同样的证据文件名在 `tools/` 根上也忽略（`*_source.json`、
`census.json`、`*.currentness.json` 以及 publication-delta / shard-manifest
等同名 dump），本机回放仍可读盘；新克隆没有它们。
`gametest_receipt.json`、`production_lock.json`、`topology.json`、
`readiness.json`、`denominator.json` 仍跟踪。现行 unique-active
`tools/waves/worldgen/gt-stone-layer-rocks/` 全部仍跟踪。不要忽略
`--check` 对着比的清单（`python_test_policy.json`、art / registry
manifests、`gt6_pipe_source.json`、`hopper_hopper_source_evidence.json` 等）。

## 冻结的生成资源根

权威清单是 [`tools/generated_resource_roots.json`](../../tools/generated_resource_roots.json)，
与 [`gradle/scripts/source-sets.gradle`](../../gradle/scripts/source-sets.gradle)
必须一致。禁止再增加 `src/<name>_generated` 同级根。Java `runData` 和 Python
生成器的所有权还没拆干净之前，不要把 Python 树合并进 `src/generated`。

Gradle 脚本按职责拆在 `gradle/scripts/`：`repositories`、`source-sets`、
`runs`、`dependencies`、`verification`、`publishing`。拆分是等价搬迁，不要顺便
改运行行为。

## GameTest source set

GameTest Java 在 `src/test/java/com/masson/cruciblecraft/gametest/`
（`ScaleGameTests` / `RecipeCensusGameTests` 仍用原来的包名，只是文件在
`src/test`）。`gameTestServer`、`client`、`server` 的 run 使用
`sourceSets.test`，并把 test 绑进 mod；发布 `jar` 只有 `main`。
`ModIdNamespaceGameTests` 仍给裸 `runGameTestServer` 垫一场测试。

结构模板继续放在 `src/main/resources/data/<namespace>/structure/empty.nbt`
和 `gametest/structure/empty.nbt`。关闭波次收据里的旧
`src/main/.../gametest` 路径是历史记录，不改。新 GameTest 只加到
`src/test`，不要再写进 `src/main`。
