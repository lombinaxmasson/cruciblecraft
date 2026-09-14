# GT6 流体附件 runtime 详细计划

> 计划 slug：`content/gt6-mte-fluid-attachments-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 流体附件 runtime
> 性质：在 dummy 现代 id 上原地实现 33 个流体附件 BlockItem，不是盖板。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 美术源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = closed
capability_slug              = content/gt6-mte-fluid-attachments-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-fluid-pipe-runtime
close_target                 = runtime_ready
```

不改 R0，不改连接件基线 `identity_resolution_ledger.json`。`depends_on` 只引用已有 capability slug。

---

## 0. 边界

- 覆盖 R0 `fluid_attachment` 全家 33 行：Faucet 22、Tap 3、Funnel 3、Nozzle 2、Cap Nozzle 3。
- dummy 现代 id 原地升级为 `MteInPlaceBlock` + `CatalogNamedBlockItem`。禁止再注册 `*_real`。
- 放置是 AttachmentSmall：点击面朝向宿主。不是 `PipeCover`，不进四套管网。
- Faucet 从朝向宿主的 `IFluidHandler` 向下浇注，可穿过叠放龙头；Tap/Nozzle 同向倒出；Funnel/CapNozzle 从上方灌入宿主。
- 获得格保持 `explicitly_blocked`。不造 stand-in 配方。
- 贴图从本地 `gregtech6_w` 迁入 `textures/block/gt6_import/mte/`，写 art manifest。龙头用 `blocksolid` icon-set，其余用对应 tools 贴图。
- 存档：同一 registry path 变成 BlockItem；无 NeoForge alias。

## 1. 验收

- [x] 33 行 `registry_kind=existing_item` 指向同一 dummy 现代 id 的 live BlockItem
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-mte-fluid-attachments-runtime`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
