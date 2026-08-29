# T40-VR 工作日志

## 2026-08-27 · 验证基础设施修复

### 边界

- 不撤销 `T40_READY`，不 `--approve-resign` T39/T40 lock
- 13/22、gap 5,597、T39 support 34、T38 19/15、ore 137/147/10/8 冻结
- 不重写 23MB census / 10MB runtime registry / 6.8MB recipe families 的 semantic body
- `owns_families=0`、`gap_delta=0`；T41 未签发
- Open debt 保持 open：`T32-VD-002`、`T32-VD-004`

现场问题见 [T40 闭卡拖延回顾](t40-closeout-delay-review.md)。

### R0–R6

- R0 freeze + repair readiness 状态机
- R1 material-form authority、gate schema v2、cross-stack closure
- R2 `atomic_io` + write guard + structured stale classes
- R3 currentness sidecars + `rebind_currentness.py`
- R4 DAG、GameTest dual roots、T39 support contract / `--diagnose`
- R5 verify runner、Gradle XML、gametest 对象、builder_cli
- R6 双轨切权威、文档与计划关闭

### Semantic projection + sidecar ABI

整文件 SHA 不再写入下游正文当 currentness 权威。权威是 versioned semantic projection + sidecar：

- `tools/semantic_projection.py` / `SemanticProjection.java`（gate 根与 Python 对齐）
- sidecar schema v2：`artifact_file_sha256` 只做 body↔sidecar 配对；`dependency_semantic_roots` 绑 semantic root
- compact `--check --reference-only` 读 sidecar，不因 envelope/builder 哈希袋要求下游重写正文
- GameTest behavior 绑定 gate semantic root；receipt 从已提交 PASS log rebind，未 resign production lock

### 验证

- `python tools/verify.py integration --profile recipes --report-all --json build/verification/t40-vr-recipes.json` → PASS（Gradle 717 tests, GameTest receipts PASS）
- `python tools/verify.py integration --profile census --report-all --json build/verification/t40-vr-census.json` → PASS（GameTest DEFERRED）
- `python tools/verify.py integration --profile census-replay --report-all --json build/verification/t40-vr-census-replay.json` → PASS
- ABI 后隔离 GameTest 重跑并 `--write --from-log` 收口收据（未 resign production lock）：
  - `-Pt38Recipes`：All 5 required tests passed
  - `-Pt39Recipes`：All 8 required tests passed
  - `-Pt40Recipes`：All 8 required tests passed
- `python tools/verify.py integration --profile card-diagnostic-T40 --json build/verification/t40-vr-diagnostic.json` → DIAGNOSTIC（DAG stale=0；hash-only 只报告不升 PASS；open debt 仍为 T32-VD-002 / T32-VD-004）
- `python tools/verify.py integration --profile card-closeout --report-all --json build/verification/t40-vr-closeout.json` → PASS（顶层 Gradle 717 / receipts PASS，不被 census-replay SKIP 覆盖）

T39 lock `b9034b2950a41b8905ef6668de793d04136432070cd238e577754148d7aa8600`  
T40 lock `26cca29698c34039cc8aa19ff8e584a0ec7a521165ec39e8dcece03cf75488e5`

### 结果

```text
T40_VR_READY
failed_gates = []
owns_families = 0
gap_delta = 0
T41 not issued
open debt: T32-VD-002, T32-VD-004
```
