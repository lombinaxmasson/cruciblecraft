# CrucibleCraft 阶段档案 · T48

> 状态：`T48_READY`（2026-08-31）
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Production lock SHA-256：`40414d2e7ab1c60631601752e81f1241741fdc53ca8b2b31d6dc5696d980decd`
> Selection SHA-256：`4f5fbdb0aad900a2ab98a1506919d3fff340a73fbac26b50fd8bdfe24bfabd98`

## 关闭结果

- 在既有 forward-v2 上追加 T48 identity/runtime delta；v1 三件套与 T46/T47 delta 保持冻结；
- production lock 签发 145 complete families / 34091 relations
  （47 exact + 62 exact_multi + 36 tool_head）；3532 个 identity
  （3461 tool_head + 71 multiitem）；required forms 1679 pairs / 23 new prefixes；
- 2 条真实 Bath B1 support 配方；5 个 recycling_candidate family 未升 R；
- 三组 `cruciblecraft:t48_bath_exact` / `t48_bath_exact_multi` /
  `t48_bath_tool_head`；无 T48 Java historical whitelist；
- closing ordinary gap：`1499 - 145 - 0 = 1354`。

`reclassified=0`。`partial=0`。Deferred recycling 仍为 1817。余下 5 个 Bath
`crushedPurifiedTiny` recycling_candidate family 仍计在当前 gap。Mixer 未签发。
显式 `N<300` 例外：`bath_host_remainder_identity_closeout`。

## Player path

分层证明：

```text
B0 = T46+T47 cumulative typed closure without T48 production or support
B1 = B0 + 2 条真实 Bath support 配方 + remainder scatter
B2 = B1 + 34091 Bath remainder relations
```

34091/34091 production inputs 在 B1 可达。创意栏、注册成功与 GameTest 注入都不是
取得证明。真实 support 配方在
`recipe/t48_player_path_support/`，GameTest `t48_locked_support` 绑定该树，空 stub
目录不能当 PASS。

## Runtime 与 load

- isolated namespace `cruciblecraft_t48` GameTest 12/12 通过并提交 UTF-8 receipt；
- 真实 `ModBlocks.BATH`；T46 组 1517 关系不变；T47 两组 13708 关系不变；
  错 identity 不启动；programmed_circuit 允许 PRESERVE；
- JUnit 对锁定字段断言；exact 组 tag overflow 3，其它组 overflow 0；
- production winner：card 与三组均为 `on_demand`
  （card 0 eager / 34091 lazy / cache 128）；
- authored datapack closing：6113 + 150 = 6263（145 families + 3 publication
  policies + 2 real B1 support recipes）；logical 34091；
- T14 closing 口径：18 组 compact production mix 同载重测，eager 14 / lazy 50557 /
  cache 781。这不是均一 on_demand，也不是历史 Java 图删除。18 组 integrated
  allocation 686579392 超过冻结的 512 MiB hard；T48 未抬 gate。Card 1x
  on_demand allocation 467210984 低于 hard。
- T14 hard ceiling 未提高；authored 超 6000 为 REPORT_ONLY。

## 权威 artifacts

- `tools/t48_production_lock.json`
- `tools/t48_layered_player_path.json`
- `tools/t48_runtime_dependency_manifest.json`
- `tools/t48_gametest_receipt.json`
- `tools/t48_materialization_decision.json`
- `tools/t48_publication_delta.json`
- `tools/t48_load_projection.json`
- `tools/t48_census_delta.json`
- `tools/t48_card_topology.json`
- `tools/t48_readiness.json`
- `tools/forward_recipe_authority_v2_readiness.json`

`failed_gates=[]`。T49 仅保留连续编号，未预分配 host/families；
`unique_active_card = null`。
