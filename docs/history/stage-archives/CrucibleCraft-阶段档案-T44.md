# CrucibleCraft 阶段档案 · T44

> 状态：✅ `T44_STORAGE_READY`（2026-08-29）
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Selection SHA-256：`5c16820e514d48012c8c52f1ab1572fe5c395a3be7e77225728faddb5287b4de`

## 关闭结果

- Storage bundle：28 source sites / 624 registrations；
- 独立 `mass_storage_logistics`：1 source site / 1 registration；
- 6 个共享 Block/BE host，runtime id 由 source numeric id 投影；
- 18 条 source-visible 取得配方；607 hidden 行保留身份、无伪造配方；
- ordinary recipe gap 不变：`3076 + 0 = 3076`。

`reclassified=0`。T36 live machine catalog 仍 85 行。T43 closing gap 未改写。

## Player path

18 条可见行从 T44 外部 B0（T43 关闭 + 机器 + hopper + 材料）取得。创造栏可见与
GameTest 注入都不是生存证明。Hidden plank / mod-wood / logistics 行不发配方。

## Runtime 与 load

- isolated namespace `cruciblecraft_t44` GameTest 10/10 通过并提交 UTF-8 receipt；
- JUnit 创建 BlockEntity，GameTest 另创建 BlockEntity；拒绝 catalog-only 零测量；
- authored datapack closing：4466 + 18 = 4484；eager/lazy ordinary 不加；
- T14 hard ceiling 未提高。

## 权威 artifacts

- `tools/t44_storage_production_lock.json`
- `tools/t44_storage_catalog.json`
- `tools/t44_storage_equivalence.json`
- `tools/t44_storage_player_path.json`
- `tools/t44_storage_publication_delta.json`
- `tools/t44_storage_gametest_receipt.json`
- `tools/t44_storage_load_measurements.json`
- `tools/t44_storage_load_projection.json`
- `tools/t44_storage_census_delta.json`
- `tools/t44_card_topology.json`
- `tools/t44_readiness.json`

`failed_gates=[]`。T45 未签发，不预分配 host/families；
`unique_active_card = null`。
