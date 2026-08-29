# T42-Repair 工作日志

## 2026-08-28 · snapshot / inventory / classifier fidelity

### 边界

- 不撤销 `T42_PARTITION_READY`，不占用 T43
- `owns_families=0`、`completion_delta=0`、`publication_delta=0`
- 不生成配方、form、GameTest 或 T43 family ID
- 不把 dump 的 143 个 `minecraft:*` 刮进 allowlist
- 不把 armor / tool-head 折进 `prefix_item_to_form`

### 修复

- R0 write-once hash freeze + `T42_REPAIR_READY`；topology `partition_repair`；
  lock `--approve-repair-lock`
- Snapshot：T37 empty markers；`count==0` / `notConsumed` → PRESERVE；wear；intern
  material/form/alias；circuit `gregapi:gt.integrated_circuit` → programmed_circuit +
  `circuit_config`（映射，不是 T35/B0 证明）
- Inventory：ITEMGENERATOR.MOLTEN 184；top-level molten flag 0；mapping molten.* 203；
  allowlist provenance `t37_t41_aliases_plus_explicit`，显式多一个 `minecraft:oak_planks`
- Overlay：`prefix_item_to_gt_prefix` unique_kind；`unique_kind_counts.mte` 2,646 /
  unique-bucket 4,980；其中约 3,766 有 unique_kinds，约 1,214 为空残差；
  `gt.stone.andesite` / `gt.armor.hazmat.*` 仍 unmapped
- Gap：`partial_family_count=0` 是「未从 gap 扣除 partial」；overlay 仍有 1 个 partial 族

### 验证

- `python -m unittest discover -s tools/tests -p "test_build_t42*.py"`
- `test_verification_dag` / `test_verification_profiles`
- `.\gradlew.bat test --tests ...T42LogicalRelationIdentityTest --tests ...T42RuntimeInventoryClassifierTest --no-daemon`
- `python tools/verify.py integration --profile recipe-partition`
- 二次 lock `--write` 零漂移

### 结果

```text
T42_REPAIR_READY
failed_gates = []
already_expressed_delta = 1
reclassification_delta = 4
completion_delta = 0
closing_execution_gap = 5300
unique_kind_counts.mte = 2646
unique_bucket_family_count = 4980
unique_object_kind_family_count = 3766
unique_residual_without_kind_count = 1214
next_issue_id = T43
unique_active_content_card = null
```

T43 未签发。T43 R0 必须从 repaired overlay 重新冻结，而不是 2026-08-28 T42 lock。
Circuit 映射、unique 残差族、autoclave/mixer circuit 槽都不是可关闭证明。
