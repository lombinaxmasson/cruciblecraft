# 回收运行时与 Deferred 账本收口 工作日志

## 2026-09-02 · `RECYCLING_DEFERRED_LEDGER_R0_READY`

Child slug `recycling/deferred-ordinary-ledger-r0` 关闭。opening deferred
ledger 1,845 已逐族枚举，每条只出现一次。`owns_families = 0`。
`completion_delta = 0`。未生成配方，未注册物品。

### 分桶

```text
smelter_proven_mte_recovery     1817
smelter_recovery_edge              2   #1829 alumina, #1884 lava
autoclave_tagged_recycling         0
mislabeled_needs_reclass          24   ordinary autoclave processing
centrifuge_execution_envelope      1   #0010
centrifuge_cross_mod               1   #0207
ledger total                    1845
```

inherited recycling 1,819 已展开为 1,817 proven + 2 edge。
`inherited_recycling.enumerated = true`。`silently_discarded = false`。

Autoclave 24 条的 lock 理由写 “MTE dismantling”，dump+evidence 证明输入是
circuit + dust + steam，输出是 MTE crystal + distilled water，不是消耗一台
MTE 的回收。R0 `reclassification_delta = 24`（改标签，不是 completion）。
`autoclave/deferred-recycling` 仍拥有这 24 条。

Identity candidate：1,817 unique `(gregtech:gt.multitileentity, meta)`，与
proven family 数相等。Bath 重叠 46 个 meta。后继
`unique_active_wave = recycling/smelter-mte-identity`。

## 2026-09-02 · `SMELTER_MTE_IDENTITY_READY`

Child slug `recycling/smelter-mte-identity` 关闭。1,817 个 exact
`(gregtech:gt.multitileentity, meta)` 可持身份已入 catalog。Bath 重叠 46 个
meta 映射为 `existing_item`，复用已有 `runtime_id`。新建 1,771 个
`gt_mte/mte_<meta>` 物品，B1 scatter `cruciblecraft:smelter_mte_scatter` /
tag `cruciblecraft:smelter_mte_items`。未发布 recovery recipes，未从
deferred ledger 扣减。Isolated GameTest
`-PwaveRecipes=recycling/smelter-mte-identity` 3/3 PASS。后继
`unique_active_wave = smelter/deferred-recycling`。

## 2026-09-02 · `SMELTER_DEFERRED_RECYCLING_READY`

Child slug `smelter/deferred-recycling` 关闭。1,817 条 proven MTE recovery 以
compact exact singleton 发布，81 个 publication group，全部 `on_demand`。未混入
`#1829` / `#1884` 或 Autoclave 24。Isolated GameTest
`-PwaveRecipes=smelter/deferred-recycling` 7/7 PASS。Integrated load
`deferred_recycling` mix 128 groups，hybrid candidate `LOAD_READY`。count 超旧
21k/56k 只报 `UNVERIFIED_SCALE`。census：`completion_delta = 1817`，
deferred recycling 1,843 → 26，deferred total 1,845 → 28。后继
`unique_active_wave = smelter/deferred-recycling-edge`。

## 2026-09-02 · `SMELTER_DEFERRED_RECYCLING_EDGE_READY`

Child slug `smelter/deferred-recycling-edge` 关闭。`#1829` alumina 与 `#1884`
lava 均为独立 `post_1x_scope`。不得把 alumina 登记成铝，也不得把 lava 映射成任意
熔融金属。`completion_delta = 0`。deferred recycling 26 → 24，deferred total
28 → 26。后继 `unique_active_wave = autoclave/deferred-recycling`。

## 2026-09-02 · `AUTOCLAVE_DEFERRED_RECYCLING_READY`

Child slug `autoclave/deferred-recycling` 关闭。R0 已把 24 条改标为 ordinary
autoclave processing。1.x 缺少 crystal identity / B0，不得复用 Smelter
MTE→molten builder，也不得提前 mixed-family completion。独立 `post_1x_scope`。
`completion_delta = 0`。deferred recycling 24 → 0，deferred total 26 → 2。后继
`unique_active_wave = recycling/non-recycling-scope`。

## 2026-09-02 · `NON_RECYCLING_SCOPE_READY`

Child slug `recycling/non-recycling-scope` 关闭。`#0010` 超 centrifuge envelope
1/6/1/6，扩大 envelope 会改产品语义。`#0207` 依赖 `ic2pahoehoelava`，不得改成
普通 lava。两条均为独立 `post_1x_scope`。`completion_delta = 0`。deferred
recycling 保持 0，deferred total 2 → 0。后继
`unique_active_wave = recycling/deferred-ordinary-runtime`。

## 2026-09-02 · `DEFERRED_ORDINARY_RUNTIME_READY`

Program slug `recycling/deferred-ordinary-runtime` 关闭。ledger replay：

```text
DEFERRED_ORDINARY_RUNTIME_READY
current execution gap = 0
proven complete = 1817
post-1.x scope = 28
deferred recycling = 0
deferred ledger total = 0
later:recycling = 0
later:cross_mod = 0
later:execution_envelope/gt6_panel = 0
inherited_recycling.enumerated = true
silently_discarded = false
unique_active_wave = null
next_unassigned = true
one_x_joint_exit = false
```

卡计划已移到 `docs/history/card-plans/closed/`。后继默认 1.x joint exit gate，
本 program 不预写 exit lock，不启动核能。
