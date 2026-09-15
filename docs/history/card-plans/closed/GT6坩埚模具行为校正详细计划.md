# GT6 坩埚模具行为校正详细计划

> 计划 slug：`content/gt6-crucible-mold-behavior-correction`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6 坩埚模具行为校正
> 性质：用 GameTest 钉住陶瓷坩埚 / 陶瓷模具相对 GT6 小坩埚 / 模具的可玩合同，并改 `src/main`。
> 不重新打开 `content/gt6-mte-crucible-foundry-runtime`，不改 85 个材质铸造身份，不改龙头 `IFluidHandler` 合同。铸造 dummy 罐容量按 GT6 `U` 对齐（不再 8000 mB），仍不是 `MultiTileEntitySmeltery` / `ITileEntityMold`。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 关闭当时 unique-active 已空；本卡 `workflow=accepted`，不占落地锁。

```text
lane                         = closed
capability_slug              = content/gt6-crucible-mold-behavior-correction
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = content/gt6-mte-crucible-foundry-runtime
close_target                 = runtime_ready
```

关闭目标不是 `player_complete`。不写新获得格。

---

## 0. 边界

只动已经活着的陶瓷主机：

- `CrucibleBlock` / `CrucibleBlockEntity` / `CrucibleProcessCore.singleBlock()`
- `CeramicMoldBlock` / `CeramicMoldBlockEntity`

验收以隔离 GameTest 为准，不以再扫一遍 GT6 当完成证明。合同从玩家能看见的动作写成测试：掉落物、容量、顶面漏斗、模具默认不抽、活动扳手开自动抽、红石门、冷却后漏斗取出、铸造 dummy 罐 millibucket。

**不在本卡：**

- 85 个 `CRUCIBLE_FOUNDRY` 材质坩埚 / 模具 / 盆 / 交叉的 tick / `fillMold` / 掉落吸入（罐容已按 GT6 `U` 改成 2304 / 144 / 1296 / 0 mB，仍是 dummy `IFluidHandler`）
- 龙头往模具浇熔体（流体附件 runtime 已关成 NeoForge 流体倒罐）
- 凿子 5×5 选形、CU 催冷、接触烫伤、钳子空手温差
- 大坩埚 `fillMoldAtSide` 端口层（T29 已把模具写成控制器库存）

T29 把陶瓷写成 8 锭是「不得改小」。本卡改到 **16 锭**是对齐 GT6 `MAX_AMOUNT = 16*U`，不是改小。

---

## 1. GameTest 合同

隔离命名空间 `cruciblecraft_wave_content_gt6_crucible_mold_behavior_correction`。

| 测试 id | 必须看见 |
| --- | --- |
| `crucibleSucksDroppedIngot` | 掉在坩埚口的可分解锭进入熔体，不是只靠右键 |
| `crucibleAcceptsSixteenIngots` | 16 锭成功，第 17 锭 `FULL` |
| `crucibleHopperInsertsFromTop` | 上方朝下漏斗能塞进顶面 1 格缓冲 |
| `moldDoesNotAutoPullByDefault` | 邻接熔融坩埚时默认模具保持空 |
| `moldAutoPullsWhenSideEnabled` | 打开该侧面自动抽后模具被浇满 |
| `moldRedstoneGatesAutoPull` | 红石门开着、没信号时不抽；有信号才抽 |
| `moldHopperExtractsWhenCool` | 已凝固且接近环境温度时下方漏斗能取出 |
| `foundryTankCapacitiesMatchGt6Units` | invar 熔炼坩埚 2304 mB、石模具 144 mB、石盆 1296 mB、石交叉 0 mB |

右键往陶瓷坩埚塞材质、右键从邻接坩埚浇模具、模具每 tick 降温：保留，不是本卡要删的路径。

---

## 2. 余量（本卡不做，缺口页要写明）

这些以前被 `runtime_ready` 身份卡盖住，GameTest 本卡**不**写成绿测去锁死错误行为：

1. 材质熔炼坩埚仍不是 `MultiTileEntitySmeltery`（罐容已是 16 锭 millibucket）。
2. 材质模具 / 盆 / 交叉仍不是 `ITileEntityMold` / 坩埚路由（罐容已是 1 锭 / 1 块 / 0）。
3. 浇铸口仍倒普通流体，不 `fillMold`。

后续若做，另开 child，不要改本卡 `required_test_ids`。

---

## 3. 验收

- [x] 上表 8 个 GameTest
- [x] 单块容量 JUnit 16 锭；大坩埚仍 432
- [x] `python tools/verify.py integration --profile capability-runtime` 的本卡 python 门
- [x] 关闭目标 `runtime_ready`
