# CrucibleCraft 阶段档案 · T40-VR

> 状态：✅ `T40_VR_READY`（2026-08-27）
> 性质：T40 内部 verification repair gate；`owns_families=0`、`gap_delta=0`
> 不占用 T41，不重签 T39/T40 production lock

## 关闭结果

- 单一 `tools/material_form_authority.json`；gate schema v2 + Java v1 fallback
- versioned semantic projection + sidecar ABI：整文件 SHA 只做 body↔sidecar 配对，
  下游 compact `--check` 绑 `semantic_root_sha256` / `dependency_semantic_roots`
- typed ore denominators 保持 137 / 147 / 10 / 8，禁止 union
- atomic UTF-8 LF writer + authority write guard；测试不得写 committed 权威文件
- T35/T39/T40 currentness sidecar；giant census/registry/families 不因 hash-only 重写
- verification DAG 强制 source → manifest → GameTest → receipt → census → topology → readiness
- GameTest receipt 拆 behavior / currentness root；behavior 绑 gate semantic root；
  T39 support 固定 34 routes
- `verify.py --report-all --json`、Gradle `--rerun-tasks` + `TEST-*.xml`、
  `gametest` 对象、`card-diagnostic-T40` / `card-closeout`
- 受管 builder 无隐式写；`T32-VD-002` / `T32-VD-004` 保持 open

冻结分母未变：T40 13/22、gap 5,597、T39 support 34、T38 19/15。T41 未签发。

## 权威 artifacts

- `tools/t40_vr_pre_repair_freeze.json`
- `tools/t40_vr_repair_readiness.json`
- `tools/material_form_authority.json`
- `tools/semantic_projection.py` / `SemanticProjection.java`
- `tools/material_registration_gate.currentness.json`
- `tools/t40_vr_material_cross_stack_closure.json`
- `tools/authority_manifest.json`
- `tools/verification_dependency_dag.json`
- `tools/t39_support_contract.json`
- `build/verification/t40-vr-closeout.json`（recipes + census + census-replay）
