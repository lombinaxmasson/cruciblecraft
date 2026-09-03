# CrucibleCraft 阶段档案 · 物品网络核心

> 状态：`ITEM_NETWORK_CORE_READY`（2026-09-03）
> 计划 slug：`runtime/item-network-core`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`TOOL_HEAD_PREFIX_READY`；核能 Track C `started = false`
> Closing：物品封面网运行时（`logistics_item_storage` /
> `logistics_item_transfer`）；sidecar 3 定义 / `register()` 2 behavior；
> T19 9/8 仍活锁；`network_id` 1–16；连通 = 同维 ∩ 已加载 ∩ 同号 ∩
> 物品管分量；dedicated 11/11；`nuclear_started = false`；
> `unique_active_wave = null`；`next_unassigned = true`；
> `production_lock = null`；`leftover_later_count = 39`

## 关闭结果

- 零 GT family 内容卡。`owns_families = 0`。`completion_delta = 0`。
  `generated_recipe_count = 0`。未签 production lock。未灌
  `src/recipe_generated/**`。未改写物流 R0 / T13c 封印。
- **T19 9/8 仍活锁；物品网在 sidecar。**
  `cover_definitions.json` 仍正好 9 行、无 `logistics_` id、sha256
  `207b4d03…`。`registerBuiltin("...")` 仍正好 8。新定义在
  `item_network_cover_definitions.json`；新 behavior 走
  `CoverBehaviorRegistry.register(...)`，不是 `registerBuiltin`。
- 宿主只在物品管。三件 `PipeCoverItem`（storage / import / export）；
  import 与 export 共用 behavior `logistics_item_transfer`，方向由
  definition id 映射。`network_id` 0 / 缺省 = fail-closed，不是全服默认网。
- 发现与搬运：`ItemLogisticsNetwork` 独立访问上界 4096、每分量端点
  256、每封面每 tick ≤ 8。不复用 `ItemPipeNetworkTraversal` 的 visit
  账，不调用 conveyor `activeTransfer`。无合法 sink/source 时停且不吞物品。
- 生命周期：打掉物品管掉落封面；空手潜行点已贴面卸下；chunk unload
  leave 发现集，NBT 身份仍在，再加载自动 join。
- T35 日常 census 仍钉 `cover_behaviors: 8` / items 门面；那是隔离
  `cruciblecraft_census`，本卡不重钉。live `registeredIds()` 为 10。
- 账本：物品两行 `runtime_ready`。Fluid / Generic / Dump +
  `logistics_core` 仍 `frozen`。下一张仍是未签发的 Fluid / Generic
  Network。发展计划 1.2 手工闭环不占本卡 READY。
- READY **只**证明物品两行封面网可运行、可获取、可存档、dedicated
  绿。不是七 kind `player_complete`，也不是 GT6
  `MultiTileEntityLogisticsCore`。关卡后仍开着的证明债见
  [known-issues.md](../../current/known-issues.md) 与工作日志：
  unload / reload / load 门偏软；census 隔离钉 8、live 10；贴图暂借。
  已清：`LAST_BY_POS` 全局 probe 表。

```text
ITEM_NETWORK_CORE_READY
owns_families          = 0
completion_delta       = 0
generated_recipe_count = 0
production_lock        = null
unique_active_wave     = null
next_unassigned        = true
nuclear_started        = false
leftover_later_count   = 39
R0 7-kind hashes       = unchanged
```

不签发里程碑编号。不把它写成物流七 kind 完成。

## 权威 artifacts

- `src/main/java/com/masson/cruciblecraft/logistics/itemnet/`
- `src/main/resources/data/cruciblecraft/item_network_cover_definitions.json`
- `src/main/java/com/masson/cruciblecraft/gametest/ItemNetworkCoreGameTests.java`
- `tools/waves/runtime/item-network-core/`
  （`wave.json` / `readiness.json` / `topology.json` / `load_axis.json` /
  `census_delta.json` / `gametest_receipt.json` / `gametest.log`）
- `tools/tests/test_item_network_core.py`
- `.\gradlew.bat runGameTestServer -PwaveRecipes=runtime/item-network-core --offline`
- `python tools/build_logistics_cover_net_r0.py --check`
- `python tools/verify.py integration --profile logistics`
- `python tools/verify.py dev --path docs/`

物流 live 不重导 T8/T19 整树信封 sha；那些 builder 在 `archive`。
全量 `gradle test` 由 recipes 付。

不重写 `tools/waves/portfolio/logistics-cover-net-r0/**`。
