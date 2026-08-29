# CrucibleCraft 阶段档案 · T42-Repair

> 状态：✅ `T42_REPAIR_READY`（2026-08-28）
> 性质：T42 内部 snapshot / inventory / classifier fidelity repair gate
> `owns_families=0`、`completion_delta=0`、`publication_delta=0`
> 不占用 T43，不撤销 `T42_PARTITION_READY`

## 关闭结果

- 五个 T42 缺陷已修：molten 计数拆分、allowlist 不是 1.21.1 scrape、count=0/PRESERVE/WEAR、
  empty-slot 不再匹配 `gt.metaitem.01`、`prefix_item_to_gt_prefix` 给出诚实 `unique_kinds`
- `T42_PARTITION_READY` 保留为 5,305-family partition 历史关闭
- repaired overlay buckets：ready 28、prefix/molten 292、unique 4,980、
  combinatorial 4、already_expressed 1
- unique 桶是残差桶：约 3,766 族有 `unique_kinds`，约 1,214 族 `unique_kinds` 为空
  （`residual_not_ready` / unmapped / unproven vanilla / B0）。`unique_kind_counts.mte`
  = 2,646 ≠ 4,980，也 ≠「全部是独立物体」。`gt.stone.andesite`、`gt.armor.hazmat.*`
  仍 unmapped
- `cruciblecraft:programmed_circuit` 只是映射：`t35_runtime_registry` 与 B0 都没有它；
  overlay 标 `runtime_id_not_registered` / `needs_current_expression`。T43 不得把
  autoclave/mixer 的 circuit 槽当成已可关
- molten：generation-tag 184，top-level flag 0，mapping `molten.*` 203；generation-tag
  不是 CC molten 注册证明
- allowlist：T37/T41 alias 的 1.21.1 live ID **加上显式** `minecraft:oak_planks`，加
  water/lava；不是 dump 的 143 个 `minecraft:*` scrape。anvil / iron_ingot / bucket
  不在 allowlist。dump 的 `noteblock` 走 vanilla_alias
- `t42_gap_partition.json` 的 `partial_family_count: 0` 表示**没有从 gap 扣掉
  partial**；overlay 仍有 1 个 partial 族留在 5,300 里
- lock 仍扣除 already_expressed 1 与 combinatorial 4；closing gap 5,300；
  无自动 `later:object_expression`
- `next_issue_id=T43`，host/family 未预分配；T43 R0 必须从 repaired overlay 重冻结

## 权威 artifacts

- `tools/t42_repair_pre_freeze.json`（write-once 预修复 hashes）
- `tools/t42_repair_readiness.json`（`T42_REPAIR_READY`，`failed_gates=[]`）
- `tools/t42_family_operand_snapshot.json`（interned material/form/alias）
- `tools/t42_runtime_expression_inventory.json` / `tools/t42_vanilla_item_allowlist.json`
- `tools/t42_blocker_overlay.json`（`unique_kind_counts`、`unique_object_kind_family_count`、
  `unique_residual_without_kind_count`）
- `tools/t42_disposition_lock.json`（`--approve-repair-lock` 后 append-only）
- `tools/t42_gap_partition.json` / `tools/t42_census_delta.json` / `tools/t42_card_topology.json`

## 验证

- `python -m unittest discover -s tools/tests -p "test_build_t42*.py"`
- Java `T42LogicalRelationIdentityTest` / `T42RuntimeInventoryClassifierTest`
- `python tools/verify.py integration --profile recipe-partition`
- 二次 `build_t42_disposition_lock.py --write` 零漂移；freeze `--write` 拒绝覆盖
