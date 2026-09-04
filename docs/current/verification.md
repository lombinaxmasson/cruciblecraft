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
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile capability-runtime
python tools/verify.py integration --profile player-complete
python tools/verify.py release
```

- `dev` 根据显式 `--path` 或 Git dirty paths 选择 active profile。
- `integration --profile` 总是 fresh 执行该 profile 的 builders、Python tests、
  Gradle tasks 和 datagen；不复用旧 PASS。
- `release` 对当前 checkout fresh 执行 release profiles。它不读取历史报告来代替运行。
- 每次结果写到被 Git 忽略的 `build/verification/latest.json`。报告只包含 revision、
  dirty paths、命令、测试计数、环境和 PASS/FAIL，不包含文件摘要。

无法映射的代码路径必须报错。纯文档改动只运行文档链接与 profile 合同检查。

## 即时证据

`player-complete` 必须在同一次调用中运行 isolated GameTestServer 和真实 `runClient`。
两端只向 `build/verification/receipts/` 写临时结构化 receipt；消费者验证 capability、
runtime、状态和测试计数后即可删除。提交旧 receipt、复制旧日志或刷新时间戳均不能通过。

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
