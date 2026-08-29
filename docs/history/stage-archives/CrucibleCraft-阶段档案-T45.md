# CrucibleCraft 阶段档案 · T45

> 状态：✅ `T45_READY`（2026-08-29）
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Production lock SHA-256：`c64576f8f14cf19d6a6f62dfc3995355318ecd34fb0537ab406fab741b99aaba`
> Selection SHA-256：`086ae3899b89efca2d049bca40196720176f399e00d2beba4c7ea5173dbdc63d`

## 关闭结果

- 落地可复用 Recipe Bulk Compiler；T43 407-family 只做 test-only semantic replay；
- production lock 签发 379 singleton families / 379 relations（smelter 271 +
  drying 108）；
- catalog 365 source identities / Block+BlockItem；B1 为 365 条 worldgen scatter；
- publication groups 2：`cruciblecraft:t45_smelter_block`、
  `cruciblecraft:t45_drying_block`；无 T45 Java whitelist；
- closing ordinary gap：`3076 - 379 = 2697`。

`reclassified=0`。`partial=0`。Centrifuge sands 3 与 15 个 wildcard-meta family
不进入 lock。编译器存在不计 completion。

## Player path

分层证明：

```text
B0 = T21 + T42 item/fluid added + T43 layered，不含 T45 production/support
B1 = B0 + 365 catalog worldgen scatter routes
B2 = B1 + 379 Smelter/Drying relations
```

379/379 production inputs 在 B1 可达。创意栏、注册成功与 GameTest 注入都不是
取得证明。

## Runtime 与 load

- isolated namespace `cruciblecraft_t45` GameTest 6/6 通过并提交 UTF-8 receipt；
- datapack policy 要求 membership root / family / relation count；load 与 live
  compact sources 对账；未知 map/group fail closed；
- Centrifuge 预合并白名单未改；
- production winner：card `hybrid`（0 eager / 379 lazy / cache 24）；smelter
  `hybrid`；drying `on_demand`；
- authored datapack closing：4484 + 381 = 4865（379 families + 2 publication
  policies）；logical 379；eager delta 0；lazy 379；cache 24 只加一次；
- T14 hard ceiling 未提高。

## 权威 artifacts

- `tools/t45_production_lock.json`
- `tools/t45_layered_player_path.json`
- `tools/t45_runtime_dependency_manifest.json`
- `tools/t45_gametest_receipt.json`
- `tools/t45_materialization_decision.json`
- `tools/t45_publication_delta.json`
- `tools/t45_load_projection.json`
- `tools/t45_census_delta.json`
- `tools/t45_card_topology.json`
- `tools/t45_readiness.json`

`failed_gates=[]`。T46 仅保留连续编号，未预分配 host/families；
`unique_active_card = null`。
