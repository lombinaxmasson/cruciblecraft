# 配方加载与 EMI 稳定性详细计划

> 计划 slug：`runtime/recipe-load-emi-stability`
> 状态：已关闭（2026-09-06）。本文件位于 `card-plans/closed/`。
> 正式名称：配方加载与 EMI 稳定性
> 性质：零内容 family 的 runtime / loader repair 卡，合并候选队列中的
> 「配方数据正确性与 EMI 重复注册」与「reload / bake 稳定性」两项。
> 本卡不创建新的 RecipeMap，不发布新的配方，不改变核能、Jade 或 GT6_w
> 内容范围。`owns_families = 0`；`nuclear_started = false`；
> 机器可读 `unique_active_wave = null`；未生成 `*_READY`。
> 当前工作树不创建 Git commit。

权威边界来自
[总体规划](../../../current/roadmap.md)、
[冻结与未实现账本](../../../current/unimplemented-gap.md)、
[能力交付流程](../../../current/capability-delivery-workflow.md)、
[Ordinary Recipe Wave 语义合同](../../../current/recipe-wave-workflow.md)、
[runtime load budget v3](../../../../tools/runtime_load_budget_policy.v3.json)
以及现有 RecipeMap / EMI runtime 合同。本文只冻结实施边界和验收门，
不是 production authority。

---

## 0. 当前证据与决策

本卡签发前的仓库证据：

- `run-game-test-filtered/logs/latest.log` 当前记录了 38 条
  `Parsing error loading recipe`，不是已被当前仓库复现的 27.7 万条
  `RecipeManager` lookup 错误。27.7 万的口径必须在本卡 baseline 中重新确认，
  不能直接用它决定索引语义。
- 同一日志记录约 94,956 条逻辑配方、约 20,683 ms reload 和约 173 ms
  index。现阶段优先怀疑完整 family materialization、validation、重复 reload
  或 EMI projection，而不是最终 `RecipeMap` index 本身。
- [CrucibleCraftEmiPlugin.java](../../../../src/main/java/com/masson/cruciblecraft/compat/emi/CrucibleCraftEmiPlugin.java)
  连续调用两次 `registerProcessingMachines(registry)`。
- [GTRecipeMapLoader.java](../../../../src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipeMapLoader.java)
  当前 reload 已经是 `synchronized`，并通过 `GTRecipeRuntimeEpoch` 原子发布；
  本卡先修事件层请求去重和阶段测量，不先破坏这两个安全边界。
- [GTRecipeMapEvents.java](../../../../src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipeMapEvents.java)
  和
  [GTRecipeReloadDecision.java](../../../../src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipeReloadDecision.java)
  目前主要按 manager identity 判断 tags/server-started 关系，不能证明同一
  recipe/tag generation 在所有事件序列下只发布一次。
- [extract_energy_converter_catalog.py](../../../../tools/extract_energy_converter_catalog.py)
  曾对空 item output 生成 `[10000]`，而
  [GTRecipe.java](../../../../src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipe.java)
  要求 `output_chances` 与 item outputs 一一对应；当前生成器还需要对映射后的
  registry object 做真实存在性校验。

核心决策：

1. 先修数据正确性，再修注册 / reload 重复，再做 bake 优化；不能用性能优化
   掩盖被 RecipeManager 丢弃的配方。
2. 配方身份分成三层维护：RecipeManager holder ID、`RecipeMap.Entry` /
   compact `stable_id`、EMI 的 category + recipe ID。新增 marker 只能用于来源、
   publication group、representation、epoch 诊断等明确合同；不能把 card number、
   wall-clock 或临时 epoch 塞进持久 recipe ID。
3. 不把不同材料的不同输入输出改成 alternatives。只有完整证明输入、输出、
   概率、时间、能耗、consume/preserve/wear、special value、shadow order
   全部语义等价时，才允许在后续独立决策中聚合。
4. 如果安全修复后仍超过 load hard ceiling，本卡只能报告为 `BLOCKED` 并留下
   阶段数据；不得降低 hard ceiling、静默丢关系或关闭 EMI 来伪造完成。

---

## 1. 卡片合同

```text
owns_families             = 0
completion_delta          = 0
partial_family_count      = 0
new_recipe_map             = 0
new_machine                = 0
production_lock            = null
nuclear_started            = false
unique_active_wave         = null
```

本卡允许修复已有生成器和已有运行时快照，但不拥有任何 recipe family，
不扣 recipe census，不把已有解析失败行改成新的产品内容。生成文件只有在
canonical source-backed 修复后才能按生成器重写；禁止手工补一条“能加载”的
替代配方。

### 1.1 In scope

- 现有 GTRecipe / compact family 的解析结构和字段计数；
- holder ID、stable ID、EMI category / recipe ID 的唯一性与 lineage；
- processing EMI 重复注册；
- tags/server-started/client recipe update 的同 generation reload 去重；
- epoch、stable fingerprint、旧快照拒绝和最新快照发布；
- parse、source collection、dedup、family prepare/materialization、validation、
  temporary index、final index、EMI projection 的分阶段耗时和分配测量；
- 同一 epoch 的日志聚合、重复错误抑制和结构化诊断；
- 现有测试、integration profile 和 fresh load report。

### 1.2 Out of scope

- 新建 Canner、冷却器、热交换器、蒸汽涡轮或任何其它机器；
- 堆芯、燃料棒、热流体、辐射、GUI、Jade 内容和 `nuclear_started`；
- 聚变、等离子或 GT6U；
- 新 RecipeMap、新 recipe family、新 production lock、新 player path；
- 把 exact relation 改成开放 tag / alternatives / parameterized；
- 把多个材料的结果简单塞进一个 viewer 查询；
- 提高或取消 NBT、sync、candidate、relation、reload 等 hard ceiling；
- 全量 GT6_w 命名、建筑方块、Multiitem、贴图迁移；
- 旧 closed card、历史 seal、历史 census 的重签。

---

## 2. Owned paths

实现时允许触及以下范围；新增测试可以放在对应 package 下：

```text
src/main/java/com/masson/cruciblecraft/recipe/gt/**
src/main/java/com/masson/cruciblecraft/compat/emi/**
src/test/java/com/masson/cruciblecraft/recipe/gt/**
src/test/java/com/masson/cruciblecraft/compat/emi/**
tools/extract_energy_converter_catalog.py
tools/tests/**（仅与本卡生成器 / 解析合同直接相关的测试）
docs/current/known-issues.md
docs/current/unimplemented-gap.md
docs/current/roadmap.md
```

`src/recipe_generated/**` 只允许由 source-backed 生成器修复确定性漂移时重生；
不得新增本卡之外的 recipe family。`run-game-test-filtered/**`、`build/**` 和
临时 benchmark 输出不是提交对象。

---

## 3. 实施阶段

### A. Fresh baseline 与错误分类

先建立同一数据 revision 的可重复 baseline，至少记录：

- RecipeManager parse error 总数、按异常类型 / map / holder ID / 字段计数；
- loaded recipe 数、GT concrete / compact family / logical / eager / lazy 数；
- reload request 数、实际 publication 数、epoch、stable fingerprint；
- manager identity、tag/resource reload generation 或等价 request token；
- EMI category、workstation、processing recipe 数及 category + recipe ID 集合；
- parse、source collection、dedup、family prepare、validation、temporary index、
  final index、EMI projection 的 wall time；
- reload transient allocation、retained memory、sync bytes、server/client ticks
  behind；
- 任何 `unindexed`、duplicate stable ID、duplicate EMI registration 和
  stale snapshot。

baseline 必须保留当前 27.7 万说法与实际计数的口径差异；没有测量的维度写
`PENDING_MEASUREMENT`，不能填零。

### B. 配方数据正确性

1. 对所有现有生成源增加 fail-closed 校验：
   - item input / count / action 数量一致；
   - item output / `output_chances` 数量一致；
   - fluid input / output 非空对象和数量有效；
   - item、fluid、map 和 target registry object 都真实存在。
2. 修复 `extract_energy_converter_catalog.py` 等已确认的生成器错误。空 item
   output 必须生成空 chance 列表，不能生成伪造的 `[10000]`。
3. 未映射的 GT6 object 不得自动改成 vanilla item、其它材料、ash、dust、
   programmed circuit 或其它 stand-in。不能证明来源对象时，保留
   `blocked` 并输出可定位的 gap。
4. 保持 [GTRecipe.java](../../../../src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipe.java)
   的现有输出合同；若未来确需允许 outputless runtime，必须另立模型决策，
   不能在本卡把校验放宽后吞掉语义。
5. 解析通过后，生成树、RecipeManager live 集合与 `RecipeMap` snapshot 的
   stable ID / relation fingerprint 必须可比对；被丢弃的配方数量必须为零。

### C. ID、marker 与 EMI 注册

1. 冻结三个 ID 面：
   - RecipeManager holder ID：datapack / reload identity；
   - `RecipeMap.Entry.id()` 或 compact `stable_id`：运行时查询和 fingerprint；
   - EMI category + recipe ID：viewer registration identity。
2. 明确 `source_kind`、`publication_group`、representation 和 epoch 诊断字段
   的来源与寿命。epoch 可以用于 snapshot cache key，不能污染持久 stable ID。
3. 移除
   [CrucibleCraftEmiPlugin.java](../../../../src/main/java/com/masson/cruciblecraft/compat/emi/CrucibleCraftEmiPlugin.java)
   中的 processing machine 双重调用。
4. 扩展
   [ProcessingEmiRegistrationPlan.java](../../../../src/main/java/com/masson/cruciblecraft/compat/emi/ProcessingEmiRegistrationPlan.java)
   的验证：
   - category ID 不重复；
   - map ID 不重复；
   - category + EMI recipe ID 不重复；
   - 每个 live `RecipeMap.Entry` 恰好有一个 EMI projection；
   - RecipeMap stable ID 与 EMI stable ID 集合无 missing / extra / duplicate。
5. 注册 plan 必须能报告 `configured machines`、`live entries`、`registered entries`
   三者差异，而不是只报告最终数量。

### D. Reload generation、并发和 epoch

1. 保留 `GTRecipeMapLoader.reload` 的串行准备和
   `GTRecipeRuntimeEpoch.publish` 的原子快照语义。
2. 在事件协调层引入明确 request identity，至少包含：
   `RecipeManager` identity、tag/resource generation 或等价单调 token、
   runtime side 和 request cause。
3. 对 `TagsUpdated → ServerStarted`、重复 `TagsUpdated`、`ServerStarted → recipe
   update`、client reload、integrated server 等序列写纯策略测试和实际事件序列
   集成测试：
   - 同一数据状态只允许一次 publication；
   - 不同 generation 必须重新加载；
   - 多个并发请求不能让旧 generation 覆盖新 generation；
   - 旧 snapshot 在新 epoch 发布后不可继续作为 current view。
4. 如果事件只提供 manager identity 而没有 generation，必须在 coordinator 中
   建立可验证的 token；不得用时间戳猜测“这是新 reload”。
5. 日志和 publication 计数以“实际发布的 epoch”为单位，不能以收到的 event
   次数冒充 reload 次数。

### E. Bake 分段测量与安全优化

1. 给 `PublicationMetrics` 或等价 report 增加阶段计时和 allocation 维度，
   至少能区分：
   `parse`、`source_collection`、`dedup`、`family_prepare`、
   `complete_validation`、`temporary_index`、`final_index`、`emi_projection`。
2. 先消除重复完整遍历、重复临时 index 和重复 EMI materialization；每一步都
   保留 relation fingerprint、stable ID 集合和 snapshot 数量对比。
3. 对 EMI display projection 可以做按 epoch 的 cache，但 cache 必须绑定完整
   published snapshot；不能让 EMI 读到旧 epoch 或未索引 relation。
4. 只有在 exact semantic equivalence 证明完成后才允许减少存储表示。不同材料、
   不同输入输出、不同概率或不同 machine semantics 保留独立 relation。
5. 如果安全修复后 reload 仍超过 hard ceiling，输出阶段占比、分配量和下一步
   假设，转为 `BLOCKED` 或另开“懒查询 viewer / materialization”卡；不在本卡
   静默改变 viewer 语义。

### F. 日志治理

- 同一 epoch、同一 error fingerprint 只输出一次聚合摘要；
- 保留 map、holder、字段、source path 和计数，不能只输出“加载失败”；
- 同一请求被去重时记录 suppressed count；
- 新 generation、新 fingerprint、新 map 必须重新报警；
- warning、info、stack trace 不能因重复 reload 成倍放大；
- 日志治理不得把真正的 parse error 降级为成功。

---

## 4. 验收门

### 4.1 正确性硬门

```text
RecipeManager parse errors                         = 0
unknown map / registry object                     = 0
item input count/action mismatch                  = 0
item output / output_chances mismatch             = 0
duplicate holder / stable / EMI identity          = 0
unindexed published relation                      = 0
same data generation actual publication count     = 1
RecipeMap stable IDs == EMI projection IDs        = exact
old epoch overwriting latest epoch                = impossible
```

与本卡无关的旧 blocked 内容继续 blocked；不能为了通过上述门给核电、热流体、
Canner 或其它缺件补 stand-in。

### 4.2 载荷与性能门

沿用 [runtime_load_budget_policy.v3.json](../../../../tools/runtime_load_budget_policy.v3.json)：

- server reload ≤ 10 s；
- client reload ≤ 10 s；
- server index ≤ 1 s；
- client index ≤ 3 s；
- sync ≤ 64 MiB；
- lookup p95 ≤ 2 ms、candidate max ≤ 128；
- retained memory、reload transient allocation 和其它轴不得被零填充或
  猜测通过。

soft budget 只产生可审计 warning；hard ceiling 仍 fail closed。当前约 20.7 s
的 reload 不能直接被写成 PASS，必须由 fresh 修复后重新测。

### 4.3 Regression

至少覆盖：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile player-complete
```

Java 侧补强或更新：

```text
src/test/java/com/masson/cruciblecraft/recipe/gt/GTRecipeReloadDecisionTest.java
src/test/java/com/masson/cruciblecraft/compat/emi/ProcessingEmiRegistrationPlanTest.java
```

必要时新增：

```text
src/test/java/com/masson/cruciblecraft/recipe/gt/GTRecipeReloadEpochTest.java
src/test/java/com/masson/cruciblecraft/recipe/gt/GTRecipePublicationMetricsTest.java
```

生成器侧必须覆盖空 item output、未知 registry object、chance 数量不匹配、
重复 stable ID、重复写入零漂移和 source-backed blocked。

---

## 5. 关闭、阻塞与撤回

### 可以关闭

- 正确性硬门全部通过；
- EMI processing registration 不重复；
- 同一 generation 不重复 publication，跨 generation 不丢 reload；
- phase metrics 和 allocation metrics fresh 可复现；
- 载荷硬门通过，或所有未测维度都明确标成 pending 并由另一个独立 gate
  接管；
- 生成树 / RecipeManager / RecipeMap / EMI 的 relation fingerprint 等价；
- `nuclear_started = false`、无新 family、无 production lock。

### 必须 BLOCKED

- 仍有未知 registry object 或 parse error；
- 需要把不同语义配方塞进 alternatives；
- 需要提高 hard ceiling 才能通过；
- 无法区分同 manager 的不同 generation；
- EMI 或 viewer 只能通过丢 recipe、读旧 epoch 或全量无界扫描；
- reload 仍超过 hard ceiling 且没有完整阶段证据；
- 只能用旧日志或旧 receipt 代替当前 revision 的 fresh run。

### 撤回

撤回只撤本卡，保留 baseline、失败样本、error fingerprint、测量和原因。
不得回滚或重签已关闭 compact codec、作者矩阵、ordinary wave 或其它历史
closeout seal。

关闭时把本文件移至 `card-plans/closed/`，更新
[history INDEX](../../INDEX.md)、[roadmap](../../../current/roadmap.md)、
[unimplemented-gap](../../../current/unimplemented-gap.md) 和必要的
known-issues；不自动创建 Git commit。

## 6. 关闭记录（2026-09-06）

本卡按正确性优先关闭，不是靠放宽 hard ceiling。

### Baseline 口径

- `run-game-test-filtered/logs/latest.log` 在签发前记录 **38** 条
  `Parsing error loading recipe`，全部落在 `energy/fuels_fluidbed/*`。
- 同一 revision **没有**复现「27.7 万 RecipeManager lookup 错误」；关闭口径保留
  这个差异，不以旧数字决定索引语义。
- 约 94,956 逻辑配方、reload ≈ 20,683 ms、index ≈ 173 ms。`compactFamilyUnindexedRelations=5`
  是 shard overflow，不是 RecipeMap unindexed。

### 落地

- 生成器 `extract_energy_converter_catalog.py` fail-closed：空 item output 发空
  `output_chances`；未知 registry object / 无输出 / 计数不齐 → `blocked`，不写
  stand-in。`fuels_fluidbed` 现为 **6** 条可加载 JSON，**49** 条记在
  `tools/energy_converter_fluidbed_blocked.json`（11 unmapped GT6 item，
  38 fail-closed，含 `dust_div72` / 无输出燃料）。
- EMI：去掉 `registerProcessingMachines` 双重调用；`ProcessingEmiRegistrationPlan`
  报告 configured / live / registered，并校验 category+recipe 唯一；
  `ProcessingEmiProjectionCache` 绑定 published epoch。
- Reload：`GTRecipeReloadCoordinator` 用 RecipeManager identity + 单调 generation +
  runtime side + cause。`TagsUpdated → ServerStarted` 与重复 Tags 对同一 generation
  只 publication 一次；更低 generation 不能覆盖。
- Bake：`validateCompleteReloadRows` 用 `RecipeMap.wouldBeUnindexed` 代替第二次
  `prepareRecipes(complete)`；`temporaryIndexMillis = 0`。`PublicationMetrics`
  增加 control / phaseTimings / allocation；allocation 两轴为
  `PENDING_MEASUREMENT`，不填零。同一 epoch 的 map INFO 收成
  `RecipeLoadLog.publicationSummary`。
- 计划所列 `python tools/verify.py integration --profile recipes` **不存在**：
  `recipes` profile 已退役，等价门是 `runtime-java`。不把该 profile 加回去。

### 仍不在本卡

- 49 条 fluidbed 继续 blocked，直到有真实 `dust_div72` / 灰形态或另立 outputless
  燃料模型；不在本卡放宽 `GTRecipe`「至少一个输出」。
- 生产 reload hard ceiling 仍为 10 s（`runtime_load_budget_policy.v3.json` /
  `RECIPE_RELOAD_BUDGET_MS`）。GameTest 验证轴仍用既有
  `VERIFICATION_RECIPE_RELOAD_BUDGET_MS = 15 s`。本卡不把 20.7 s 旧日志写成 PASS。
- 未签发 capability、未创建 `tools/waves/**`、未翻转 `unique_active_wave`。
