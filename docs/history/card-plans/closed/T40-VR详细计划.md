# T40-VR 详细计划：验证基础设施修复

> 阶段：T40-VR · T40 内部 verification repair gate
> 状态：已关闭（`T40_VR_READY`，2026-08-27）
> 性质：工程/验证修复；`owns_families=0`、`gap_delta=0`，不占用 T41 编号
> 前置：`tools/t40_readiness.json#status = T40_READY`，`failed_gates=[]`
> T40 production lock：13 families / 22 relations，SHA-256 `26cca29698c34039cc8aa19ff8e584a0ec7a521165ec39e8dcece03cf75488e5`
> 当前 ordinary gap：5,597 families
> 后继：T41 在 `T40_VR_READY` 且 `failed_gates=[]` 前保持未签发

## 1. 背景、目标与边界

[T40 闭卡拖延回顾](../../../current/t40-closeout-delay-review.md) 确认 T40 Electrolyzer
生产内容已闭合；拖延来自材料门闸 overlay、跨卡 hash/currentness、可变权威测试夹具、
receipt/manifest 顺序和验证 profile 的结构缺口。

本卡修复这些结构问题，建立下一张 recipe wave 可重复使用的验证基础设施。它不撤销
`T40_READY`，不重签 T39/T40 production lock，不改变 13/22 生产分母或 5,597 gap，也不
预分配 T41 host/families。

权威输入：

- [ordinary recipe wave 流程与规范](../../../current/recipe-wave-workflow.md)；
- [验证指南](../../../current/verification.md)；
- [T40 闭卡拖延回顾](../../../current/t40-closeout-delay-review.md)；
- `tools/t40_readiness.json`、`tools/t40_census_delta.json`、
  `tools/t40_card_topology.json`；
- `tools/known_issues/verification-debt.json`。

T40-VR 完成后，证明的是“验证和 currentness 基础设施已修复”，不是“历史卡重新实现”。
所有历史 recipe/census 身份、已关闭的 family、production lock selection 和 source revision
保持原语义。

## 2. R0 · 事故冻结与 repair contract

新增以下 machine-readable repair artifacts 与 builders/tests：

```text
tools/t40_vr_pre_repair_freeze.json
tools/t40_vr_repair_readiness.json
tools/build_t40_vr_pre_repair_freeze.py
tools/build_t40_vr_repair_readiness.py
tools/tests/test_build_t40_vr_pre_repair_freeze.py
tools/tests/test_build_t40_vr_repair_readiness.py
```

R0 必须冻结并校验：

| 事实 | 冻结值 |
| --- | ---: |
| T40 production lock | 13 families / 22 relations |
| T40 remaining ordinary gap | 5,597 |
| T39 locked support | 34 routes |
| T38 support ledger | authored 19 / eager 15 |
| factual ore | 137 |
| registered ore | 147 |
| T38 acquisition ore delta | 10 |
| T5 semantic vein ledger | 8 base materials |

冻结文件绑定 T35/T38/T39/T40 production locks、gate、receipt、runtime manifests、large
census artifacts、verification profile/policy 和当前 debt ledger 的 hashes。Repair 期间：

- 禁止 `--approve-resign` T39/T40 lock；
- 禁止通过修改 family/gap/census identity 使 repair 通过；
- 禁止将 open upstream debt 伪装成 PASS、skip 或 inherited READY；
- 修复新增/关闭的每项债务必须有 `scope`、`detect`、`blocks_profiles`、`owner` 和
  `repair_rule`。

## 3. R1 · 单一 material-form authority

现有 Python gate builder、compare、veins、ore-chain 和 Java gate loader 分别维护 overlay
规则，导致同一 `ore` 在 Python 可写入 gate、Java 却不能加载。重构为单一 authority：

```text
tools/material_form_authority.schema.json
tools/material_form_authority.json
tools/material_form_authority.py
```

Authority 逐 source 声明 path、gate section、owner、允许超过 factual 的 form、必要 factual
前置 form、Java runtime visibility 和 source hash。以下消费者只能读取该 authority，不再
维护硬编码 T38/T39/T40 overlay section list：

- `tools/build_gt6_material_form_gate.py`；
- `tools/compare_gt6_recipes.py`；
- `tools/build_gt6_veins.py`；
- `tools/build_gt6_ore_chain.py`；
- `MaterialRegistrationGate.java`。

Gate 升级为兼容 schema v2，写入 authority semantic root 和已解析的 Java overlay sections；
Java 保留 schema-v1 fallback 以读取历史 gate，但 schema-v2 不得依赖硬编码卡号列表。

以下 ore 集合必须作为不同类型、不同字段独立报告，严禁为“补齐 worldgen”而 union：

```text
factual_ore_materials         = 137
registered_ore_materials      = 147
t38_acquisition_ore_delta    = 10
t5_semantic_vein_ledger      = 8
worldgen_catalog             = independent catalog, not the T5 ledger
```

T38 overlay ore 仍可驱动 runtime block registration 与 ore-block crush，但不得进入 T5
eight-base vein denominator；`materials` 的有效 union 必须与 R0 完全相同。T40-VR 不借机
注册新 material form。

新增 Python+Java+ore-chain cross-stack closure receipt/test，证明：

1. gate forms 属于 factual、flag-backed 或有界 authority overlay；
2. Java 能加载同一 gate；
3. gate 注册的 ore 与 ore-block crush rows 一致；
4. T38 support ledger 仍为 19/15；
5. T5 semantic vein ledger 仍为 8。

所有 card recipe builder 移除直接写 material gate 的路径；新 card 只能写
`t{N}_required_forms.json`，由 authority/gate builder 汇总。

## 4. R2 · 原子写入与权威测试夹具隔离

新增 `tools/atomic_io.py`，统一所有 committed JSON 的写入：

```text
same-directory unique temporary file
-> UTF-8 LF bytes write
-> flush/fsync
-> os.replace
-> cleanup in finally
```

`t27_common.write_stable`、`t35_common.write_stable`、material gate、compare baseline、
verification session/context 及其调用链都委托这个实现。权威 artifact 禁止裸
`Path.write_text` / `Path.write_bytes`。

新增：

```text
tools/authority_manifest.json
tools/tests/support/authority_sandbox.py
tools/tests/test_atomic_io.py
tools/tests/test_authority_write_guard.py
```

`run_python_tests.py` 安装 write guard：测试进程直接写受保护 authority path 时立即失败；
只有 patch 到 temporary output/input 或显式 `authority_sandbox` 内的副本才可写。迁移所有
会改真实文件的 test，包括：

- `test_build_t35_runtime_registry.py`；
- `test_build_t35_readiness.py`；
- `test_build_t35_excluded_object_reclaim.py`；
- `test_build_t35_recipe_families.py`；
- `test_build_t21_readiness.py`；
- `test_build_t38_roaster_recipes.py`；
- `test_build_t39_readiness.py`。

Windows 回归测试必须覆盖 ≥25MB JSON、并发 writer、中断清理、bytes/line-ending 保持。
任何 tamper test 失败或被中断后，受保护文件的 digest 均须与运行前一致。

`t35_common.stale_error` 改为结构化分类：

```text
MISSING
CORRUPT          # 具 JSON line/column 与文件大小
HASH_ONLY_DRIFT
SEMANTIC_DRIFT
```

不得再用 disk hash 冒充 builder 期望 hash，也不得把 corrupt JSON 降级为普通 stale。

## 5. R3 · semantic body 与 currentness sidecar

T35 巨型 artifact 将业务语义与可再绑定 currentness 分离。新增 `tools/currentness.py`、
sidecar schema 与第一批 sidecar：

```text
tools/t35_recipe_families.currentness.json
tools/t35_runtime_registry.currentness.json
tools/t35_census.currentness.json
tools/t35_card_topology.currentness.json
tools/t35_readiness.currentness.json
tools/t39_census_delta.currentness.json
tools/t39_card_topology.currentness.json
tools/t39_readiness.currentness.json
tools/t40_census_delta.currentness.json
tools/t40_card_topology.currentness.json
tools/t40_readiness.currentness.json
```

Semantic root 只覆盖 schema 声明的 family、identity、count、validator、lock selection 与
业务状态；排除 `generated_by`、owned input hashes、路径 hash、timestamp 等 currentness
metadata。迁移规则：

1. 不重新序列化 23MB `t35_census.json`、10MB runtime registry 或 6.8MB recipe families；
2. 将 legacy embedded currentness 视为兼容字段；sidecar 存在后 sidecar 是唯一 currentness
   authority；
3. 下游绑定 `{semantic_root_sha256, currentness_root_sha256}`，不再复制 whole-file SHA；
4. 任何现有语义 body 或 lock selection 变化都仍是 `SEMANTIC_DRIFT`。

新增 `tools/rebind_currentness.py`：

```powershell
python tools/rebind_currentness.py --plan --scope t35
python tools/rebind_currentness.py --check --scope card-closeout
python tools/rebind_currentness.py --write --scope card-closeout
```

以及受管 builder 的 `--rebind-currentness-only`。该模式只有 semantic root 不变、artifact
可解析且 dependency DAG 完整时才允许写 KB 级 sidecar；它不能重签 production lock、修复
corrupt artifact 或覆盖 semantic diff。禁止手工 string replace、64-byte hash patch 和
`git checkout` 作为 closeout rebind 流程。

回归必须模拟 `t21_template_denominator` 的 hash-only drift，证明 T35→T40 currentness 可在
不改写巨型 semantic body 的前提下恢复 current。

## 6. R4 · dependency DAG、receipt 与 canonical hash

新增：

```text
tools/verification_dependency_dag.schema.json
tools/verification_dependency_dag.json
tools/run_verification_dag.py
```

每个 node 显式声明 check/write/rebind mode、semantic/currentness dependencies、outputs 与
receipt prerequisites。DAG 必须无环并以拓扑顺序执行：

```text
source and material gate
  -> generated resources and runtime manifest
  -> Java and GameTest behavior evidence
  -> receipt
  -> census delta
  -> topology
  -> readiness
```

manifest stale 时 runner 必须拒绝 receipt write；receipt 不得反向成为 manifest 的输入。
`--plan` 输出当前 stale nodes、drift class、合法 rebind 顺序；`--check` 是纯读取。

GameTest receipt 升级为两个 root，且不削弱行为证明：

- **behavior root**：effective material-gate semantic root、loader/matcher/machine behavior、
  production lock selection、recipe/support semantic trees、测试 namespace/IDs；
- **currentness root**：可再绑定的路径 hash、packaging metadata、非行为 provenance。

只有 behavior root 不变、committed PASS log 可重新解析时，才可 currentness-only rebind。
任何 `MaterialRegistrationGate.java`、effective registered forms、recipe/support、machine、
loader/matcher 或 lock selection 变化都属于 behavior drift，必须重新运行相应 isolated
GameTest 并生成 fresh log/receipt。

统一 T38/T39/T40 等价 hash：采用 canonical `semantic_tree_sha256`、
per-stable-ID relation fingerprint map 和 `equivalence_root_sha256`。per-file hash 留作调试，
不再形成第二个 currentness authority。

T39 support 采用兼容 `t39_support_contract` sidecar：绑定旧 lock hash、order-independent
route-content root 与显式 topological emit-order root。生产 emit 只读 34 locked routes；
discovery 只允许 `--diagnose`，发现 35 routes 不能改变 production 或 lock。

对 work-set 的 `unique_active_card`、candidate 的 closing 前说明、readiness 中错误命名的 B0
字段新增 lifecycle metadata audit。冻结 issuance snapshot 不回写；T41+ schema 使用
`snapshot_phase`、`authority`、`b0/b1/production_inputs_reachable` 等准确字段。

## 7. R5 · verification runner、CLI 与 profile 诚实性

重构 `tools/verify.py` 为共享 runner/evidence writer，同时保留默认 strict fail-fast。新增：

```text
--report-all     # 收集全部失败，但最终仍为 FAIL
--json PATH      # 输出 schema-validated verification_result.json
currentness      # 只生成 DAG/rebind plan 或执行受审 rebind
```

每个 full Java profile 必须：

1. 传 `--rerun-tasks`，或调用永不 UP-TO-DATE 的 `verifyFullTestSuite`；
2. 解析 `build/test-results/test/TEST-*.xml`；
3. 记录实际 test/failure/error/skipped 数量与 `rerun_tasks=true`；
4. 计数不足、XML 缺失、错误或过滤 run 的旧结果均不得作为 full-suite PASS。

将 profile 的 `gametest: true` 改为结构对象：

```text
receipt_check
run_isolated
manual_deferred
```

strict profile 缺少 receipt 或 isolated PASS 必须失败；`manual_deferred` 只能在结果中显示为
DEFERRED，不能变成 green PASS。

卡级 profile 分为两类：

- `card-diagnostic-T40`：验证本卡 production chain，并把上游 stale/debt 作为结构化报告；
  它不产生 closeout PASS；
- `card-closeout`：仍严格要求声明的 recipes/census/census-replay/upstream gates 全绿；
  不允许把 upstream hash stale 降为 warn 或 bypass。

新增 `tools/builder_cli.py` 并迁移所有受管 builder 到显式、互斥的：

```text
--check
--write
--rebind-currentness-only
```

特殊受审 mode（例如 receipt `--from-log`、lock `--approve-resign`）由
`verification_builder_policy.json` 声明。`build_component_rules.py` 裸运行必须报 usage，
不得默认写；`rebuild_artifacts.py` 只读取 policy，不再猜测 builder argv。

扩展 debt schema：每项需要 profile/path scope、failure class、检测命令、是否阻塞 profile、
owner 和关闭证据。`--debt-aware` 只能标注 scope-external debt，绝不能将 required failure
转成 PASS。

## 8. R6 · 迁移、验证与关闭

采用 additive migration：authority、atomic writer、sidecar、DAG、runner 与旧规则并行，
双轨检查一致后才切换权威，最后删除 legacy hardcoded list/default-write path。每一阶段先
完成 machine-readable repair gate 再继续，避免一次大爆炸式重写。

至少验证：

```powershell
python -m unittest discover -s tools/tests -p "test_*authority*.py"
python -m unittest discover -s tools/tests -p "test_*currentness*.py"
python -m unittest discover -s tools/tests -p "test_*verification*.py"
python -m unittest discover -s tools/tests -p "test_build_t35*.py"
python -m unittest discover -s tools/tests -p "test_build_t38*.py"
python -m unittest discover -s tools/tests -p "test_build_t39*.py"
python -m unittest discover -s tools/tests -p "test_build_t40*.py"
.\gradlew.bat test --rerun-tasks --no-daemon
.\gradlew.bat runGameTestServer -Pt38Recipes --no-daemon
.\gradlew.bat runGameTestServer -Pt39Recipes --no-daemon
.\gradlew.bat runGameTestServer -Pt40Recipes --no-daemon
python tools/verify.py integration --profile recipes --report-all --json build/verification/t40-vr-recipes.json
python tools/verify.py integration --profile census --report-all --json build/verification/t40-vr-census.json
python tools/verify.py integration --profile census-replay --report-all --json build/verification/t40-vr-census-replay.json
```

并模拟六类故障：gate overlay 变化、hash-only upstream 变化、semantic drift、corrupt large
JSON、filtered Gradle run、测试进程中断。每种情况都必须得到结构化、fail-closed 结果。

关闭时更新 verification guide、recipe-wave workflow、known issues、roadmap、history index；
创建 T40-VR stage archive/work log。只有所有 repair gates 通过才把计划移到 `closed/`。

## 9. 退出门

- `T40_VR_READY`、`failed_gates=[]`、`owns_families=0`、`gap_delta=0`；
- T40 lock 13/22、T40 gap 5,597、T39 support 34、T38 19/15 不变；T41 未签发；
- 单一 material authority；typed ore denominators 保持 137/147/10/8；
- 零测试写 committed authority artifact；统一 atomic writer 通过 Windows large-file、
  concurrent 和 interrupted-write 测试；
- hash-only drift 只写 sidecar；semantic/corrupt drift fail closed，不改写 T35 巨型
  semantic body；
- DAG 无环，manifest/receipt 顺序由工具强制；currentness-only rebind 不能绕过 GameTest
  behavior evidence；
- T39 support 固定 34 routes；discovery 不能漂移 production emit；
- full Java verification 不能被 filtered-test 的 UP-TO-DATE 状态欺骗；GameTest 不再只是
  打印提示；verification result 为 machine-readable；
- 所有受管 builder 无隐式写默认；required profile failure 不得被 debt-aware 降级；
- strict recipes、census、census-replay 与 fresh T38/T39/T40 receipt 全部通过。

## 10. 非目标

- 不新增 recipe family、material form、worldgen 或机器功能；
- 不重签 T39/T40 production lock，不修改 gap，不提前选择 T41 host/families；
- 不以减少 receipt binding、放宽 currentness、跳过 full suite 或把 upstream warning 写成
  PASS 缩短收口；
- 不批量重写 T35–T40 历史 semantic JSON 来掩盖 hash drift。
