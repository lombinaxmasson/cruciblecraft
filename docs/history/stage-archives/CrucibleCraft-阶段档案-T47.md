# CrucibleCraft 阶段档案 · T47

> 状态：`T47_READY`（2026-08-30）
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Production lock SHA-256：`884643b2ba569e4eb96c9fcfe63caeebc61be6bb2df3334f85d3cf13490614db`
> Selection SHA-256：`91617eae23a29de82cce4a3b054deafee9c57d44b175014c571ede18281523dc`

## 关闭结果

- 在既有 forward-v2 上追加 T47 identity/runtime delta；v1 三件套与 T46 delta 保持冻结；
- production lock 签发 395 complete families / 13708 relations
  （189 exact + 206 exact_multi）；283 个新 block identity；3 个流体 overlay；
- 10 条真实 Bath B1 support 配方；395 行非回收 disposition；required forms 0；
- 两组 `cruciblecraft:t47_bath_exact` / `cruciblecraft:t47_bath_exact_multi`；无 T47
  Java historical whitelist；
- closing ordinary gap：`1894 - 395 - 0 = 1499`。

`reclassified=0`。`partial=0`。Deferred recycling 仍为 1817。余下 150 个 Bath remainder
family 因 identity/form 未注册未进入 lock，仍计在当前 gap。Mixer 未签发。

## Player path

分层证明：

```text
B0 = T46 cumulative typed closure without T47 production or support
B1 = B0 + T47 overlay fluids / 10 条真实 Bath support 配方 + remainder scatter
B2 = B1 + 13708 Bath remainder relations
```

13708/13708 production inputs 在 B1 可达。创意栏、注册成功与 GameTest 注入都不是
取得证明。真实 support 配方在
`recipe/t47_player_path_support/`，GameTest `t47_locked_support` 绑定该树，空 stub
目录不能当 PASS。

## Runtime 与 load

- isolated namespace `cruciblecraft_t47` GameTest 10/10 通过并提交 UTF-8 receipt；
- 真实 `ModBlocks.BATH`；T46 组 1517 关系不变；错 identity 不启动；
- JUnit 对锁定字段与 `shardCount == relationCount` 断言；
- production winner：card 与两组均为 `on_demand`（card 0 eager / 13708 lazy / cache 128）；
- authored datapack closing：5706 + 407 = 6113（395 families + 2 publication
  policies + 10 real B1 support recipes）；logical 13708；
- T14 closing 口径：15 组 compact production mix 同载重测，eager 14 / lazy 16466 /
  cache 478。这不是均一 on_demand，也不是历史 Java 图删除；T48 必须从这个 closing 重开。
- T14 hard ceiling 未提高；authored 超 6000 为 REPORT_ONLY。

## 权威 artifacts

- `tools/t47_production_lock.json`
- `tools/t47_layered_player_path.json`
- `tools/t47_runtime_dependency_manifest.json`
- `tools/t47_gametest_receipt.json`
- `tools/t47_materialization_decision.json`
- `tools/t47_publication_delta.json`
- `tools/t47_load_projection.json`
- `tools/t47_census_delta.json`
- `tools/t47_card_topology.json`
- `tools/t47_readiness.json`
- `tools/forward_recipe_authority_v2_readiness.json`

`failed_gates=[]`。T48 仅保留连续编号，未预分配 host/families；
`unique_active_card = null`。
