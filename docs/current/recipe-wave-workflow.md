# Ordinary Recipe Wave 语义合同

> 全项目进度单位是
> [能力晋级](capability-delivery-workflow.md)。Recipe Card 只用于限定内容工作集，
> 不是 runtime 类型、验证身份或路线图完成单位。编号 builders、历史 receipts 与
> closeout seals 已归档，不参与 active verification。

## 1. 固定流程

```text
Source Pack
  -> RecipeImportSpec
  -> catalog / candidate
  -> human-reviewed production lock
  -> recipe_bulk compile
  -> generated/runtime equivalence
  -> player path + machine execution
  -> load + census
  -> capability profile PASS
```

现有 host 接新来源统一走 `recipe_import.json` 与 `import-source`，不得新增 per-wave
builder 或把卡号写入 `WaveSpec`、Java 类型、配方路径、codec、cache key。

同一时刻只允许一条 active delivery lane。一个 Card 可以拥有多个
`(target_map, publication_group)`；一个 group 可以拥有多个 query-addressable shards。
Shard 是运行时索引单位，不是另一张卡。

## 2. Source Pack 与 production lock

Source Pack 声明：

```text
source_system
source_revision
source_recipe_identity
semantic_family_identity
external file checksums
```

外部文件 checksum 用于确认输入身份，属于明确保留的边界。它不能扩散成内部 builder、
receipt、currentness 或 seal 的摘要链。

`lock_candidate.json` 可以重算；`production_lock.json` 是人工审核后的语义集合。lock
保留 family/template/stable IDs、计数、来源 revision、representation、publication
groups、player-path baseline 和 phase-owner disposition。它不绑定内部 Python/Java/JSON
文件摘要。candidate 漂移不得自动覆盖 production lock。

Operand 必须逐项分类为 `proven_equivalent`、`needs_current_expression`、
`phase_deferred` 或 `unsupported`。只有等价已证明或当前表达已实现的 operand 才能进入
production。family 在全部 relations 实现前只能是 `partial`，不得扣 gap。

## 3. 表示、publication group 与 shard

每个 family 明确选择一种表示：

- singleton：exact relation
- 小型 combinatorial：exact multi-relation + global shadow order
- 大型 material/template expansion：parameterized template + slots
- 无法安全表达：blocked/overflow，禁止静默展开

生产 policy identity 是 `(target_map, publication_group)`。Query router 必须从当前
query 可见的 item/tag/fluid/component/shape keys 直接选择 shard，不能先扫描全部 shard。

compact publication、recipe identity 与 shard membership 摘要属于运行时功能性身份，
继续保留。验证必须同时证明：

- 每条 logical relation 恰好进入声明的 group/shard
- router 对相关与不相关 query 都返回正确候选
- priority、shadow order、consume/preserve 语义不变
- overflow 有显式预算且超限 fail closed
- reload 与网络同步前后身份一致

这些摘要不能被复用为开发流程的“已验证”凭据。

## 4. Fresh 验证

active recipe profile 每次 fresh 执行：

1. Source Pack 外部完整性与 spec schema
2. 在临时目录重新 compile
3. 解析后 JSON 结构或逐文件字节比较
4. Java/JUnit 的真实 provider、codec、router 与 RecipeManager 测试
5. 需要时 isolated GameTestServer
6. datagen 两次运行的目录清单与逐文件字节比较
7. load/census 结构与预算断言

结果写入 `build/verification/latest.json`，不提交，不含内部内容摘要，也不复用旧 PASS。

```powershell
python tools/build_recipe_bulk.py import-source --spec <path> --check
python tools/build_recipe_bulk.py compile --wave all --check
python tools/verify.py integration --profile recipes
```

GameTest、load 与 census 的历史 JSON/日志只用于调查当时发生过什么。当前能力晋级必须在
同一次 verification 调用中重新执行所需 runtime 测试。

## 5. 玩家路径与等价

Recipe family 完成必须覆盖：

- source → generated 的全部字段和全部 relations
- generated root → Java codec/provider
- server/client/reload/EMI 的实际可见集合
- 生存获取输入、供能、执行、输出消费或明确终端用途

禁止用 `relations[0]`、test-only catalog、GameTest 注入、静态类名搜索或历史 receipt
冒充玩家路径。配方 UI 是 EMI。

## 6. Load 与容量

同时公布 `family_count`、`source_rows`、`expected_logical_relations`、
representation breakdown、publication group/shard count、overflow 与 worst routed
candidate。容量门约束 query 实际候选区间和 integrated load，不把“每 N 行一张卡”当性能
策略。card-only 通过不能掩盖 integrated regression。

## 7. 历史与迁移

历史编号波的源码和 artifacts 保持原字节，并列入
`tools/legacy_verification_index.json`。Active policy 不 import、调度、重签或重建它们。
核心 runtime ABI 变化由当前 compatibility profile 回归，而不是批量刷新历史 seals。

历史流程、分母、compact 波与关闭记录见 [docs/history](../history/INDEX.md)。冻结但未进入
游戏的能力见 [unimplemented-gap.md](unimplemented-gap.md)；不得根据历史 `_READY`
反推玩家完成。
