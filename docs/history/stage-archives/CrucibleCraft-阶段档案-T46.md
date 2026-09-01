# CrucibleCraft 阶段档案 · T46

> 状态：`T46_READY`（2026-08-30）
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Production lock SHA-256：`454a49b25ff329dbb053b9195841fe9bb51dcbc311fd293f990b354218a762f4`
> Selection SHA-256：`cdb829e48df59007cc1153833fdc94fdab255d7eb519a5f279e33d36c7128e37`

## 关闭结果

- 建立长期 forward-v2 load/identity/runtime authorities；v1 三件套保持冻结；
- production lock 签发 803 exact-relation families / 1517 relations
  （172 exact + 631 exact_multi）；118 个显式 MTE identity；
- 30 个流体 overlay + 七条 B0 油品真实 Bath support 配方；803 行非回收 disposition；
- 单组 `cruciblecraft:t46_bath_mte`；无 T46 Java historical whitelist；
- closing ordinary gap：`2697 - 803 = 1894`。

`reclassified=0`。`partial=0`。Deferred recycling 仍为 1817。余下 545 Bath
family 不进入 lock。

## Player path

分层证明：

```text
B0 = T45 cumulative typed closure + 既有非 T46 自证的 Bath host/energy
B1 = B0 + 有限 MTE scatter / 37 条真实 Bath support 配方
B2 = B1 + 803 Bath MTE relations
```

1517/1517 production inputs 在 B1 可达。创意栏、注册成功与 GameTest 注入都不是
取得证明。真实 support 配方在
`recipe/t46_player_path_support/`，GameTest `t46_locked_support` 绑定该树，空 stub
目录不能当 PASS。

## Runtime 与 load

- isolated namespace `cruciblecraft_t46` GameTest 12/12 通过并提交 UTF-8 receipt；
- 真实 `ModBlocks.BATH` / TIME+BUFFERED，无火箱；错 MTE / 错流体不启动；
- T2 `bath/crushed_to_washed` 展开式与 T5 六条 Bath 仍在；
- JUnit 对 1517 条 relation 断言 `shadow_order` 与 `source_mte_meta`（pinned source
  pack → generated JSON → live relation / GTRecipe）；
- production winner：card `hybrid`（0 eager / 1517 lazy / cache 24）；
- authored datapack closing：4865 + 841 = 5706（803 families + 1 publication
  policy + 37 real B1 support recipes）；logical 1517；
- T14 closing 口径：13 组 compact hybrid 同载重测，eager 14 / lazy 2758 /
  cache 222。这不是历史 Java 图删了 16,645 行；T47 必须从这个 closing 重开。
- T14 eager hard ceiling 未提高；lazy-inclusive logical 不再误用 21000 轴。

## 权威 artifacts

- `tools/t46_production_lock.json`
- `tools/t46_layered_player_path.json`
- `tools/t46_runtime_dependency_manifest.json`
- `tools/t46_gametest_receipt.json`
- `tools/t46_materialization_decision.json`
- `tools/t46_publication_delta.json`
- `tools/t46_load_projection.json`
- `tools/t46_census_delta.json`
- `tools/t46_card_topology.json`
- `tools/t46_readiness.json`
- `tools/forward_recipe_authority_v2_readiness.json`

`failed_gates=[]`。T47 仅保留连续编号，未预分配 host/families；
`unique_active_card = null`。
