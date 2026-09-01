# CrucibleCraft 阶段档案 · T49

> 状态：`T49_READY`（2026-08-31）
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Production lock SHA-256：`103c75d28e503240fbfcf4843b740ba38fb25acd467ec86b2b9d4bc0c6481d80`
> Selection SHA-256：`96f7c0766affc51c443ddf2f67f640027f941ac4773455729b4688c3c2d6253c`

## 关闭结果

- 冻结 overlay 保持 13 家 `recycling_candidate: true`；T49 append-only 校正把
  effective flag 设为 false。不改 `later:recycling` 1,817；
- production lock 签发 5 complete families / 95 relations（全部 exact_multi）；
  0 个新 identity；required forms 0；
- 5 家 Bath tiny-purified 锁入 `cruciblecraft:t49_bath_exact_multi`；8 家
  Centrifuge 只摘旗、仍在 gap；
- 无 T49 Java historical whitelist；T5 Bath 对 `t49/` 前缀跳过 bronze envelope；
- closing ordinary gap：`1354 - 5 - 0 = 1349`。Bath ordinary remainder 0。

`reclassified=0`。`partial=0`。Deferred recycling 仍为 1817。显式 `N<300` 例外：
`bath_host_remainder_recycling_correction`。Mixer 未签发。

## Player path

分层证明：

```text
B0 = T46+T47+T48 cumulative typed closure without T49 production or support
B1 = B0 + T49 tiny-washed scatter（空输入 worldgen）
B2 = B1 + 95 Bath remainder relations
```

B1 是 33 条 worldgen_drop，0 条 Bath support 配方。创意栏、注册成功与 GameTest
注入都不是取得证明。GameTest `t49_locked_support` 绑定 scatter 树。

## Runtime 与 load

- isolated namespace `cruciblecraft_t49` GameTest 12/12 通过并提交 UTF-8 receipt；
- 真实 `ModBlocks.BATH`；T46 组 1517、T47 两组 13708、T48 三组 34091 关系不变；
  错 fluid 不启动；programmed_circuit 允许 PRESERVE；
- JUnit 对锁定字段断言；exact_multi overflow 0；
- production winner：card 与组均为 `hybrid`
  （card 0 eager / 95 lazy / cache 95）；
- authored datapack closing：6263 + 6 = 6269（5 families + 1 publication policy +
  0 Bath support recipes）；logical 95；
- T14 closing 口径：19 组 compact production mix 同载重测，eager 14 / lazy 50652 /
  cache 876。这不是均一 on_demand，也不是历史 Java 图删除。19 组 integrated
  allocation 687226880 超过冻结的 512 MiB hard；T49 未抬 gate。Card hybrid
  allocation 低于 hard。
- T14 hard ceiling 未提高；authored 超 6000 为 REPORT_ONLY。

## 权威 artifacts

- `tools/t49_recycling_candidate_correction.json`
- `tools/t49_production_lock.json`
- `tools/t49_layered_player_path.json`
- `tools/t49_runtime_dependency_manifest.json`
- `tools/t49_gametest_receipt.json`
- `tools/t49_materialization_decision.json`
- `tools/t49_publication_delta.json`
- `tools/t49_load_projection.json`
- `tools/t49_census_delta.json`
- `tools/t49_card_topology.json`
- `tools/t49_readiness.json`
- `tools/t49_closeout_seal.json`
- `tools/forward_recipe_authority_v2_readiness.json`

`failed_gates=[]`。T50 仅保留连续编号，未预分配 host/families；
`unique_active_card = null`。
