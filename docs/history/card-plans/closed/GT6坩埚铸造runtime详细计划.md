# GT6坩埚铸造runtime详细计划

> 计划 slug：`content/gt6-mte-crucible-foundry-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6坩埚铸造runtime详细计划
> 性质：在 dummy 现代 id 上原地实现独立 BlockItem。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 美术源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = content/gt6-mte-crucible-foundry-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids
close_target                 = runtime_ready
```

不改 R0，不改连接件基线 `identity_resolution_ledger.json`。`depends_on` 只引用已有 capability slug。

---

## 0. 边界

- 覆盖 R0 `crucible_foundry` 本 child 的 85 行。
- dummy 现代 id 原地升级为 `MteInPlaceBlock` + `CatalogNamedBlockItem`。禁止再注册 `*_real`，禁止 alias 已有主机。
- 获得格保持 `explicitly_blocked`。不造 stand-in 配方。
- dummy 罐 millibucket 后来由 `content/gt6-crucible-mold-behavior-correction` 按 GT6 `U` 对齐（熔炼 2304 / 模具 144 / 盆 1296 / 交叉 0）。仍不是 `MultiTileEntitySmeltery` / `ITileEntityMold`。
- 贴图从本地 `gregtech6_w` 迁入 `textures/block/gt6_import/mte/`，写 art manifest。
- 存档：同一 registry path 变成 BlockItem；无 NeoForge alias。

## 1. 验收

- [x] 85 行 live BlockItem
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-mte-crucible-foundry-runtime`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
