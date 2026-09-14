# GT6 扩展器 runtime 详细计划

> 计划 slug：`content/gt6-mte-extender-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 扩展器 runtime
> 性质：在 dummy 现代 id 上原地实现 tank extender 与 tank bridge。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 美术源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = content/gt6-mte-extender-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-fluid-pipe-runtime
close_target                 = runtime_ready
```

不改 R0，不改连接件基线 `identity_resolution_ledger.json`。

---

## 0. 边界

- 覆盖 R0 `extender` 2 行：`extender/tank_extender`（30001）、`extender/tank_bridge`（30501）。
- 原地 BlockItem，禁止 alias 管道或盖板，禁止再注册 `*_real`。
- Extender 从对面抽流体推到朝向面；Bridge 双向。独立 IFluidHandler，不是四套管网。
- 获得格 `explicitly_blocked`。贴图从 `gregtech6_w` `machines/extenders/` 迁入 `gt6_import/mte/`。
- 存档：同一 registry path 变成 BlockItem；无 NeoForge alias。

## 1. 验收

- [x] 2 行 live BlockItem
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-mte-extender-runtime`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
