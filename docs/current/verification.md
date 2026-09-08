# 开发与验证指南

现行入口是 `tools/verify.py`。验证的权威是当前 Git revision 上刚执行的命令结果，
不是提交库中的 currentness sidecar、receipt、seal、snapshot 或历史 READY 报告。
Git 负责保存和审查变更；内容摘要不作为开发流程的防篡改机制。

全项目进度按
[能力交付流程](capability-delivery-workflow.md) 晋级。编号卡、历史波次和
编号卡历史收据都已退出工作树；不要再把它们当现行门。

## 日常入口

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile verification
python tools/verify.py integration --profile runtime-java
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile recipe-generators
python tools/verify.py integration --profile capability-runtime
python tools/verify.py integration --profile player-complete
python tools/verify.py promotion
python tools/verify.py release
```

- `dev` 根据显式 `--path` 或 Git dirty paths 选择 active profile。
- `runtime-java` 跑全量 `gradle test`，不跑 datagen。普通 runtime Java / 测试 /
  `src/main/resources` 只命中这个 profile。
- `semantic-generators` 跑 generated-art / resource-gate builders、相关 Python
  tests 和两次 `runData`。命中面是 datagen provider、`src/generated` 和贴图
  工具；不跑 JUnit，也不重编已封板的 semantic recipe wave。
- `recipe-generators` 跑 material-form 与 semantic recipe `--check`。命中面是
  `tools/recipe_bulk`、`src/recipe_generated` 和 compact policy 树。
- 改 datagen provider 会同时命中 `runtime-java` 与 `semantic-generators`：
  JUnit 加上双 datagen。
- `integration --profile` 默认 fresh 执行该 profile 的 builders、Python tests、
  Gradle tasks 和 datagen；不复用旧 PASS。CI 对 `runtime-java`、
  `recipe-generators` 与 `semantic-generators` 加 `--if-changed`：有 diff base
  且本 profile 未命中则 SKIP；没有 diff base（无 `GITHUB_BASE_REF`，且
  `GITHUB_EVENT_BEFORE` 为空或全零）时仍执行，避免覆盖收缩。`release` 始终跑全部
  release profiles。
- 新增方块或物品但只改 registry、不改 `datagen/` 时，这次 PR 不会跑 `runData`。
  `*ResourceTest` 与 `release` 仍覆盖生成树。
- `promotion` 只在 capability 从非 `player_complete` 晋级时运行 GameTestServer 与
  `runClient`（`--client`）。已接受卡的日常 `player-complete` profile 只跑
  GameTest。普通 PR 和低风险改动不会自动启动客户端。
- `release` 对当前 checkout fresh 执行 release profiles。它不读取历史报告来代替运行。
- 每次结果写到被 Git 忽略的 `build/verification/latest.json`。报告只包含 revision、
  dirty paths、命令、测试计数、环境和 PASS/FAIL，不包含文件摘要。

无法映射的代码路径必须报错。纯文档改动只运行文档链接与 profile 合同检查。

Gradle 验证任务使用 `--rerun`，只强制命令行上的目标任务执行；未变化的
`compileJava` / `processResources` 可以 UP-TO-DATE。本地和同一 CI job 复用
Gradle daemon。`release` 通过 `CRUCIBLECRAFT_GRADLE_ISOLATED=1` 隔离 daemon。

workflow-hash 扫描器在 `dev` 和带 diff base 的 CI `integration` 中只扫变更文件；
policy / 可达性输入变化以及 `release` 仍全量扫描。CI 不再单独重复执行一次扫描。

## 即时证据

`player-complete` 日常只跑 isolated GameTestServer。晋级
（`runtime_ready → player_complete`）或显式 `--client` 才再跑真实 `runClient`。
收据只写到 `build/verification/receipts/`；消费者验证 capability、runtime、状态
和测试计数后即可删除。提交旧 receipt、复制旧日志或刷新时间戳均不能通过。

```powershell
python tools/build_player_complete.py --run `
  --capability logistics/fluid-network/basic-transfer
```

人工 `player_signoff.json` 是受版本控制的评审声明，不是机器执行收据。

## 生成物与 JSON 合同

- datagen 连续 fresh 生成两次，按相对路径清单和逐文件字节比较确认确定性。
- builder 的 `--check` 在临时目录重建输出，并直接比较解析后的 JSON 结构或文件字节。
- production lock 冻结人工审核的语义集合、计数和来源 revision，不绑定内部文件摘要。
- 外部 Source Pack 的文件摘要继续校验；它用于确认外部输入身份。

## 保留的功能性摘要

以下摘要参与运行时协议、缓存或外部分发，不能因本次切换删除：

- `MaterialFingerprint`、`GeneratedMaterialPackCache`、`RegistryCodecHash`
- compact publication/shard membership 与 recipe identity
- 多人握手、生成包缓存
- `tools/recipe_bulk/source_pack.py` 的外部输入完整性
- 发布产物 checksum

边界由 `python tools/check_no_workflow_hashes.py --check` 锁定。active profile 可达文件中
不得新增 `inputs_sha256`、`builder_sha256`、`semantic_root_sha256`、
`currentness_root_sha256`、`output_hashes` 或等价的开发证明链。
`verification` profile 同时跑 `python tools/check_zero_milestone_names.py --quick`。

## 历史档案

编号 builders、currentness sidecars、verification sessions 与 DAG
列入 `tools/legacy_verification_index.json`。它们不会被 active policy import、
调度、重签或用于路线图进度。numbered closeout seal 与 `archive/sealed` 已从工作树
删除；需要历史调查时从 Git 历史读原字节。不要把它们重新接回 CI。

已知验证债务见
[`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)。
配方域仍适用
[ordinary recipe wave 的语义合同](recipe-wave-workflow.md)，但 Card/Seal 不再是
active verification 单位。
