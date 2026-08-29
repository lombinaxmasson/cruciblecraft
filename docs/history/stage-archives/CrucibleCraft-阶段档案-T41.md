# CrucibleCraft 阶段档案 · T41

> 状态：✅ `T41_READY`（2026-08-27）
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Production lock SHA-256：`ddfe50fabb0944dc2001f5b09b205c1ee82a6f55d5493733878e21b52eed8349`

## 关闭结果

- 294 families / 1,531 relations 只作为 test-only catalog fixture，证明 source replay、
  codec 与 router/load capacity；不得进入 policy、census 或 READY 分母；
- production lock 签发 292 singleton families / 292 authored relations（`#0052–#0343`）；
- live unique identities：242；50 条 leftover-vanilla（`#0290–#0339`）与 T37
  `#0002–#0051` 运行时身份相同，记为 `expressed_by=t37`，不作为独立 T41 live 行；
- publication groups：planks 85/85、fireproof 144/144、planks2 63 authored / 13 live；
- minimal locked support：33 shapeless token routes；
- combinatorial `#0000`（620）与 `#0001`（619）保持 `phase_deferred` /
  `combinatorial_player_path_unproven`，future owner `later:assembler_combinatorial`，
  不扣 gap；
- closing ordinary gap：`5597 - 242 - 50 = 5305`。

`reclassified=0`。T41 独立关闭量是 242。292 authored 与 294/1531 fixture 都不是 live
READY 分母。

## Player path

分层证明：

```text
B0 = T21 typed closure + T37/T38/T39/T40 production，不含 T41 production/support
B1 = B0 + 33 locked support routes
B2 = B1 + 292 authored production relations
```

292/292 production inputs 在 B1 可达，292/292 outputs 已注册并进入 B2。

## Runtime 与 load

- production suite 验证 292 authored / 242 live；catalog suite 隔离验证 294/1531；
- isolated namespace `cruciblecraft_t41` GameTest 8/8 通过并提交 UTF-8 receipt；
- T37 drop 使用 logical input identity，不是 Ingredient `toString`；
- 同 target 多 group 与 T37 原子 reload；
- main JAR 不含 `t41_catalog` 或 recovery；
- load projection 绑定同一 production lock hash；
- planks / fireproof winner：`hybrid`（cache 16，eager 0）；
- planks2 winner：`on_demand`（cache 16）；
- card load projection：292 authored / 292 logical / 0 eager / 292 lazy / cache 48；
- hard ceiling 未提高，pending 未 zero-fill。

## 权威 artifacts

- `tools/t41_production_lock.json`
- `tools/t41_operand_disposition.json`
- `tools/t41_layered_player_path.json`
- `tools/t41_runtime_dependency_manifest.json`
- `tools/t41_gametest_receipt.json`
- `tools/t41_materialization_decision.json`
- `tools/t41_publication_delta.json`
- `tools/t41_load_projection.json`
- `tools/t41_census_delta.json`
- `tools/t41_card_topology.json`
- `tools/t41_readiness.json`

`failed_gates=[]`。T42 仅保留连续编号，未预分配 host/families；`unique_active_card = null`。
