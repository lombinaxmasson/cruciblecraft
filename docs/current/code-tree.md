# 代码树与工作副本

日常改运行时、验证和文档时只需要打开这些路径。其余本地目录是参考树、
Minecraft 运行实例或缓存，不要整理、删除或链接它们。

## 日常入口

| 打开 | 用途 |
| --- | --- |
| `src/main/java/com/masson/cruciblecraft/` | 运行时 Java |
| `src/main/resources/` | 手写数据与资源 |
| `src/test/java/` | JUnit |
| `tools/waves/<slug>/` | 波次输入、production lock、census |
| `tools/tests/` 且列入 `python_test_policy.json` 的 `active_test_modules` | 现行 Python 测试 |
| `docs/current/` | 现行规范（含 [材料身份](material-prefix-identity.md)） |

验证入口是 `python tools/verify.py`。Python 套件只有 `active`、`affected`、
`modules`，见 [tools/README.md](../../tools/README.md) 和
[开发与验证指南](verification.md)。

## 工作副本里不要动的东西

Explorer 里会看到很多 Git 忽略的目录。它们不是仓库结构的一部分：

- `run/` 和 `run-*/`：Gradle 客户端 / GameTest 实例。日常只保留一个 `run/`。
- `gt6_code/`、`gt6_referencable_port_code/`、`gtceu_code/`、`gt6u_code/`、
  `gt6_dump/`：本地只读参考树，不在 Git 里。禁止 `git clean -fdx`、删除、
  junction / symlink。
- `build/`、`bin/`、`.gradle/`、`logs/`、`tmp-baked_textures/`、`src/src/`、
  `__pycache__/`、崩溃日志：缓存或误生成副本。
- `tools/_tmp_*.py` 和 `tools/scratch/`：本地草稿。坩埚卡结束前不要删除现有
  `_tmp_extract_gt6_*.py`；结束后再决定并入正式提取器或丢掉。

Cursor 的 `.vscode/settings.json` 会把运行目录和缓存从资源管理器里藏起来，
但 **不** 把 GT6 / GTCEu 参考树排除出搜索。

## Python 工具生命周期

不要再往 `tools/` 根目录堆新文件。新代码按层放置：

| 层 | 放置 |
| --- | --- |
| 共享库 | 已有 `tools/*.py` 模块，例如 `atomic_io.py`；不要新开平行根文件 |
| GT6 导入 / 解析 | 现有 `gt6_*.py` / `import_gt6_*.py`，或未来的 `tools/gt6/` |
| 确定性生成器 | `tools/build_*.py` 或已有子包；输出必须写进冻结的生成根 |
| 验证 | `tools/verify.py`、`tools/run_python_tests.py`、profile JSON |
| 波次账本 | `tools/waves/<slug>/` |
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
