# GT6 流体危险介质运行时详细计划

> 计划 slug：`content/gt6-fluid-dangerous-media-runtime`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`（2026-09-13）。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 流体危险介质运行时
> 性质：plasma/magic 先入罐再 tick 销毁；flammable / contactDamage 做成纯行为。不注册新规格。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = content/gt6-fluid-dangerous-media-runtime
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = registry/catalog-modern-ids, content/gt6-fluid-pipe-runtime
close_target                 = runtime_ready
```

不改 R0，不改基线 `identity_resolution_ledger.json`，不改已关闭流体管 `runtime_notes.json`。`depends_on` 不得填写没有 `capability.json` 的 prep slug。

---

## 0. 边界

- GT6 `MultiTileEntityPipeFluid`：plasma/magic **先填充**，再在 tick 里 trash / fizz / 伤害；magic 1% 换成 Thaumcraft Flux，CC 无 TC，诚实回退为空气。
- 不把 plasma/magic 记成 GAS_LEAK / CORROSION。over-temp 填充拒绝保留；罐内超温才点燃相邻空气。
- `flammable`：`ignitedByLava` + `getFlammability=150`。`contactDamage`：非空罐 `entityInside` magic 伤害。
- 不发新管规格、不 alias huge、不发 stand-in 配方。关闭目标 `runtime_ready`，不是 `player_complete`。

## 1. 验收

- [x] plasma/magic 可以 fill，不记 gas/acid；tick 按 64 / 16 / 4 trash
- [x] 木质管超温点燃相邻火；`contactDamage` 伤害生物
- [x] 四套网络仍然隔离
- [x] 隔离 GameTest `-PwaveRecipes=content/gt6-fluid-dangerous-media-runtime`
- [x] `python tools/verify.py integration --profile capability-runtime`
- [x] 关闭目标 `runtime_ready`
