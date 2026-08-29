# CrucibleCraft 阶段档案 · T43

> 状态：✅ `T43_READY`（2026-08-28）
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Production lock SHA-256：`c6a63280ee036a9cad53fbbddc721480f2479d51598ef99e18d57495b48da484`

## 关闭结果

- production lock 签发 407 singleton families / 407 relations（Cohort A 406
  `gt.stone.*` + Cohort B `#2932` noteblock）；
- catalog 119 source identities、406 distinct `(item, meta)` Block+BlockItem；
  slab 不折叠；
- publication group 1：`cruciblecraft:t43_smelter_stone`；
- locked B1 support：406 empty-input worldgen scatter routes，0 vanilla/GT
  support recipes；
- closing ordinary gap：`3483 - 407 = 3076`。

`reclassified=0`。`partial=0`。14 hazmat/storage、`#0111` 与 Drying 不进入本卡。

## Player path

分层证明：

```text
B0 = T21/T42 累计 typed closure，不含 T43 production/support
B1 = B0 + 406 catalog worldgen scatter routes
B2 = B1 + 407 Smelter relations
```

407/407 production inputs 在 B1 可达，407/407 outputs 已注册并进入 B2。
创意栏、注册成功与 GameTest 注入都不是取得证明。

## Runtime 与 load

- isolated namespace `cruciblecraft_t43` GameTest 6/6 通过并提交 UTF-8 receipt；
- loader 使用历史 T37–T41 Java policy ∪ datapack manifest；未声明 group 与
  跨 group 碰撞 fail closed；无 T43 Java whitelist；
- shard routing `t39-shard-v1`；worst shard 与 overflow 均 ≤ 128；
- production winner：`hybrid`（0 eager / 407 lazy / cache 24）；
- authored datapack closing：4058 + 408 = 4466（407 families + 1 publication
  policy）；logical 407；eager delta 0；lazy 407；cache 24 只加一次；
- T14 hard ceiling 未提高；pending 未 zero-fill。

## 权威 artifacts

- `tools/t43_production_lock.json`
- `tools/t43_layered_player_path.json`
- `tools/t43_runtime_dependency_manifest.json`
- `tools/t43_gametest_receipt.json`
- `tools/t43_materialization_decision.json`
- `tools/t43_publication_delta.json`
- `tools/t43_load_projection.json`
- `tools/t43_census_delta.json`
- `tools/t43_card_topology.json`
- `tools/t43_readiness.json`

`failed_gates=[]`。T44 仅保留连续编号，未预分配 host/families；
`unique_active_card = null`。T36-Repair 不占用 T44。
