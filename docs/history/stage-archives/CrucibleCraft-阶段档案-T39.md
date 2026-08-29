# CrucibleCraft 阶段档案 · T39

> 状态：✅ `T39_READY`（2026-08-26）
> Repair：✅ `T39_REPAIR_READY`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Production lock SHA-256：`cc3f673c7c757012a00e08e1420792e2ddcf87150264cfb59b110b74453e9afb`

## 关闭结果

- 原 157 families / 250 relations host-complete issuance 正式撤回；
- 157/250 保留在 test-only catalog fixture，仅证明 source replay、codec、router/load
  capacity；
- production lock 关闭 22 families / 32 relations；
- publication groups：singleton `19/19`、multi `3/13`；
- minimal locked support：34 routes；
- 七个 stateful reactor rod families 重分类到 `post_1x:nuclear`；
- closing ordinary gap：`5639 - 22 - 7 = 5610`。

`reclassified_to_nuclear=7` 是分母维护，不是完成 7 个配方。T39 实际关闭量为 22。

## Player path

分层证明：

```text
B0 = T21 without T39/support
B1 = B0 + 34 locked support routes
B2 = B1 + 32 production relations
```

32/32 production inputs 在 B1 可达，32/32 outputs 已注册并进入 B2；support 输入按顺序
从前一 frontier 可达，target recipe 不参与自证。

## Runtime 与 load

- Java registry bootstrap 改为共享幂等入口；
- production suite 精确验证 22/32，catalog suite 隔离验证 157/250；
- isolated namespace `cruciblecraft_t39` GameTest 8/8 通过并提交 UTF-8 receipt，含 34 locked support 存在性断言；
- 撤回 recovery tree 在 `src/test/resources/t39_withdrawn_recovery/`，不进 main JAR；
- load projection 绑定同一 production lock hash；production 路径 `unproven_lossy_alias=0`；
- singleton winner：`on_demand`；
- multi winner：`hybrid`（0 eager / 13 lazy，与 card on-demand partition 等价）；
- card load projection：22 authored / 32 logical / 0 eager / 32 lazy / cache 32；
- hard ceiling 未提高，pending 未 zero-fill。

## 权威 artifacts

- `tools/t39_production_lock.json`
- `tools/t39_operand_disposition.json`
- `tools/t39_layered_player_path.json`
- `tools/t39_runtime_dependency_manifest.json`
- `tools/t39_gametest_receipt.json`
- `tools/t39_materialization_decision.json`
- `tools/t39_publication_delta.json`
- `tools/t39_load_projection.json`
- `tools/t39_census_delta.json`
- `tools/t39_card_topology.json`
- `tools/t39_repair_readiness.json`
- `tools/t39_readiness.json`

两个 readiness 均为 `failed_gates=[]`。T40 仅保留连续编号，未预分配 host/families。
