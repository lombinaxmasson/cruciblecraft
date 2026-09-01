# CrucibleCraft 阶段档案 · 回收运行时与 Deferred 账本收口

> 状态：`DEFERRED_ORDINARY_RUNTIME_READY`（2026-09-02）
> 计划 slug：`recycling/deferred-ordinary-runtime`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：current execution gap = 0；deferred recycling 1,843；ledger total 1,845
> Closing：execution gap = 0；proven complete 1,817；post-1.x scope 28；
> deferred recycling = 0；deferred ledger total = 0；`unique_active_wave = null`

## 关闭结果

- R0 枚举 opening ledger 1,845 恰好一次。inherited 1,819 展开为 1,817 proven +
  2 edge。Autoclave 24 条改标为 ordinary processing（`reclassification_delta=24`，
  不是 completion）。
- Smelter MTE identity：1,817 exact `(gregtech:gt.multitileentity, meta)`。Bath
  重叠 46 为 `existing_item`。1,771 个新物品走 B1 scatter。未发 recovery recipes。
- Smelter deferred recycling 发布 1,817 compact exact singleton，81 publication
  groups。Isolated GameTest 7/7 PASS。Integrated load `LOAD_READY`。count 超旧
  参考只报 `UNVERIFIED_SCALE`。
- 边沿 2、Autoclave 24、centrifuge `#0010` / `#0207` 均为独立 `post_1x_scope`。
  未把 alumina 登记成铝、未把 lava 映射成熔融金属、未把 Autoclave 喂给 Smelter
  builder、未把 IC2 pahoehoe 改成普通 lava。

```text
smelter_proven_mte_recovery     1817 complete
smelter_recovery_edge              2 post-1.x
ordinary autoclave processing     24 post-1.x
centrifuge envelope #0010          1 post-1.x
centrifuge cross_mod #0207         1 post-1.x
accounted                       1845
```

`partial_family_count=0`。Program 本身 `owns_families=0`。不签发里程碑编号。

## 权威 artifacts

- `tools/waves/recycling/deferred-ordinary-ledger-r0/`
- `tools/waves/recycling/smelter-mte-identity/`
- `tools/waves/smelter/deferred-recycling/`
- `tools/waves/smelter/deferred-recycling-edge/`
- `tools/waves/autoclave/deferred-recycling/`
- `tools/waves/recycling/non-recycling-scope/`
- `tools/waves/recycling/deferred-ordinary-runtime/`
  （`gap_replay.json`、`deferred_ledger.json`、`readiness.json`、`closeout_seal.json`）

`failed_gates=[]`。后继默认 1.x joint exit gate，未预写 exit lock 或 nuclear
census。核能 Track C 保持 `started = false`。
