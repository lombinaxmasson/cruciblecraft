# GT6 物品管运行时详细计划

> 计划 slug：`content/gt6-item-pipe-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 物品管运行时
> 性质：普通物品管内库存、禁用 I/O、10-tick 发送，并折回精确 live BlockItem。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-item-pipe-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids
close_target                 = runtime_ready
```

基线 prep `content/gt6-pipe-cable-baseline` 已关闭。capability `depends_on` 不得填写没有 `capability.json` 的 prep slug。

---

## 0. 边界

- 身份子集只读自已关闭的 `identity_resolution_ledger.json` item 行。不改 R0，不改基线 ledger。
- 只折普通 medium/large/huge 的精确 live BlockItem。
- restrictive 三规格保持 dummy / blocked（无 canonical prefix、无真实 BlockItem、不抬 budget）。
- 管内库存按 GT6 `invSize`（CC `stacks_per_second`）持久化。`getStackInSlot` / `extractItem` 不再恒空。满管返回剩余，不丢物品。
- 管内发送 `gameTime % 10 == 0`；cover pump 仍走 5 tick 物流节拍，不得把 cover 当管内库存。
- `mDisabledInputs` / `mDisabledOutputs` 持久化；猴扳手按 GT6 循环切换侧 I/O。路由 cost 仍是 GT6 `stepSize`。
- 关闭目标 `runtime_ready`，不是 `player_complete`。

## 1. 验收

- [x] 管内库存可读可抽，满管不 void
- [x] 禁用输入拒绝插入；禁用输出不向该侧发送
- [x] 管内 10 tick 发送；cover 仍 5 tick
- [x] restrictive dummy 未伪造；普通 stepSize 与 GT6 一致
- [x] 侧合同：物品/流体/EU/红石网络隔离
- [x] 不修改 `ItemNetworkCoreGameTests`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
