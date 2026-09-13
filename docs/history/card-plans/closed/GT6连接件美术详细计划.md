# GT6 连接件美术详细计划

> 计划 slug：`content/gt6-connector-art`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 连接件美术
> 性质：从本地 `gregtech6_w` 迁流体管 / 物品管 / EU 线缆 iconset，并清掉 dummy 铁锭模型。
>
> 美术源：`gt6_referencable_port_code/gregtech6_w`。
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-connector-art
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = content/gt6-fluid-pipe-runtime, content/gt6-item-pipe-runtime, content/gt6-eu-wire-cable-runtime
close_target                 = runtime_ready
```

基线 prep `content/gt6-pipe-cable-baseline` 已关闭。capability `depends_on` 不得填写没有 `capability.json` 的 prep slug。红石铜线 iconset 已由关闭的红石身份卡拥有，本卡不重新复制。

---

## 0. 边界

- 只迁 fluid `pipetiny...pipehuge`（含 quadruple/nonuple dummy）、item pipe、wire/overlay、insulation、restrictor。GT6 没有独立 `cable.png`。
- `already_shared` / `fold_live_block` 行新增 PNG 数为 0。
- `keep_distinct` / in-place dummy 不得继续使用铁锭模型。
- 不重新复制红石 `copper/wire.png`。绝缘红石 27006/27056/27506 另开 child。
- 关闭目标 `runtime_ready`，不是 `player_complete`。

## 1. 验收

- [x] 三份 domain manifest 的 `source` / `gt6_source` / `destination` / hash 能在本地 `gregtech6_w` 解析
- [x] fold 行额外 PNG 为 0；EU 不重拷红石铜线
- [x] keep_distinct dummy 模型不再引用 `iron_ingot`
- [x] 不把新方法写进 `CrucibleCraftGameTests`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
