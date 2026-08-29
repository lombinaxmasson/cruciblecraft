# CrucibleCraft 阶段档案 · T40

> 状态：✅ `T40_READY`（2026-08-26）
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Production lock SHA-256：`26cca29698c34039cc8aa19ff8e584a0ec7a521165ec39e8dcece03cf75488e5`

## 关闭结果

- 61 families / 151 relations 只作为 test-only catalog fixture，证明 source replay、
  codec 与 router/load capacity；不得进入 policy、census 或 READY 分母；
- production lock 关闭 13 families / 22 relations；
- publication groups：singleton `11/11`、multi `2/11`（`#0002`=4，`#0005`=7）；
  combinatorial 不在 lock 内；
- minimal locked support：0 routes；
- combinatorial `#0000`（20）与 `#0001`（15）保持 `phase_deferred` /
  `combinatorial_player_path_unproven`，future owner `later:electrolyzer_combinatorial`，
  不扣 gap；
- closing ordinary gap：`5610 - 13 - 0 = 5597`。

`reclassified=0`。T40 实际关闭量为 13。61/151 不是 READY 分母。

## Player path

分层证明：

```text
B0 = T21 typed closure + T37/T38/T39 production，不含 T40 production/support
B1 = B0 + 0 locked support routes
B2 = B1 + 22 production relations
```

22/22 production inputs 在 B1 可达，22/22 outputs 已注册并进入 B2；
`unproven_lossy_alias=0`。

## Runtime 与 load

- production suite 精确验证 13/22；catalog suite 隔离验证 61/151；
- isolated namespace `cruciblecraft_t40` GameTest 8/8 通过并提交 UTF-8 receipt；
- Electrolyzer 允许 GT6 programmed_circuit 的 count-0 PRESERVE；不提高 T5 EU ceiling；
- main JAR 不含 `t40_catalog` 或 recovery；
- load projection 绑定同一 production lock hash；production 路径 `unproven_lossy_alias=0`；
- singleton winner：`on_demand`（cache 11）；
- multi winner：`hybrid`（0 eager）；
- card load projection：13 authored / 22 logical / 0 eager / 22 lazy / cache 22；
- hard ceiling 未提高，pending 未 zero-fill。

## 权威 artifacts

- `tools/t40_production_lock.json`
- `tools/t40_operand_disposition.json`
- `tools/t40_layered_player_path.json`
- `tools/t40_runtime_dependency_manifest.json`
- `tools/t40_gametest_receipt.json`
- `tools/t40_materialization_decision.json`
- `tools/t40_publication_delta.json`
- `tools/t40_load_projection.json`
- `tools/t40_census_delta.json`
- `tools/t40_card_topology.json`
- `tools/t40_readiness.json`

`failed_gates=[]`。T41 仅保留连续编号，未预分配 host/families；`unique_active_card = null`。
