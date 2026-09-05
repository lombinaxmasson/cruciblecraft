# 显示 CPU 详细计划

> capability slug: `logistics/display-cpu`
> wave slug: `runtime/display-cpu`
> 状态：已关闭（2026-09-05）。本文件位于 `card-plans/closed/`。
> `maturity = player_complete`，`workflow = accepted`。
> 工作类型：GT6 `CoverLogisticsDisplayCPU*` 四件状态盖板 + Core 每 20 tick
> 把 used/capacity 写进盖板视觉与红石。
> 来源只读本地 `gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 机器可读 `unique_active_wave` 仍为 `null`。
> 明确不包含：处理器单元方块、处理器晶体物品、Display 以外的 misc 封面、
> Batteries、GT6 电线宿主、重做已 `player_complete` 的 Core 导入/导出/整理。
> 玩家中文名：物流监视器(逻辑/控制/存储/转换处理器)。
> 玩家完成命令：`python tools/build_player_complete.py --run --capability logistics/display-cpu`。

## 目标

兑现对照图 seed「Display CPU」以及物流 R0 标成该切片 `out_of_scope` 的
四个 kind。它们是管子上的**状态盖板**，不是内核 CPU 方块：

- `cruciblecraft:logistics_display_cpu_logic`
- `cruciblecraft:logistics_display_cpu_control`
- `cruciblecraft:logistics_display_cpu_storage`
- `cruciblecraft:logistics_display_cpu_conversion`

GT6 物品名 Logistics Display (CPU Logic/Control/Storage/Conversion)，
tooltip：Emits Redstone and Displays Status of Logistics Core。

## 来源（本地，不 fetch GitHub）

- `gt6_code/gregtech6/src/main/java/gregapi/cover/covers/AbstractCoverAttachmentLogisticsDisplay.java`
- `gt6_code/gregtech6/src/main/java/gregapi/cover/covers/CoverLogisticsDisplayCPULogic.java`
  （Control / Storage / Conversion 同类）
- `gt6_code/gregtech6/src/main/java/gregtech/tileentity/multiblocks/MultiTileEntityLogisticsCore.java`
  （`onServerTickPre` 里对 Display 盖板写 `value` / `visual`）
- `gt6_code/gregtech6/src/main/java/gregtech/items/MultiItemTechnological.java`
  （id 1086–1089；shapeless 四向循环；shaped `Cover_Blank` + `OD_CIRCUITS[2]`
  + Lumium `wireGt01` + 螺丝刀方位）

物流 R0 `inherited_denominator.json` 的 `display_cpu_out_of_scope` **不改**。
那是该切片的历史排除，不是「永远不要做」。本卡是新 runtime 能力，
不得复用 `portfolio/logistics-cover-net-r0`，也不得重写 sealed R0 JSON。

## 运行时契约

1. **宿主（SOURCE_DERIVED）**：贴在 Core 已经扫描的物品管 / 流体管上。
   GT6 文案还有 Wiring；CC 没有 GT6 物流电线，本卡不发明电线宿主。
2. **不是传输盖板**。`usePriorities()` 关、`useTargetStackSize()` 关。
   不搬物品、不搬流体、不参与 Dump。
3. **写回（SOURCE_BACKED）**：Core 每 `SYNC_SECOND`（约 20 tick）快照上一窗
   `oCPU_*`（NBT `gt.cpu.*.used`），再清零本窗计数。对扫描到的 Display 盖板：
   - 红石 `bind4`：used≤0 → 0；used≥capacity → 15；否则
     `14 - clamp(0,13, (capacity-used)*14/capacity)`
   - 视觉帧：used≤0 → 0；used≥capacity → 10；否则
     `9 - clamp(0,8, (capacity-used)*9/capacity)`
   贴图每 kind 0–10 + underlay。
4. **used 从哪来（按 GT6 本文件，不发明）**：
   - **Control**：扫描半径实际用到的切比雪夫 `max(used, distance-2)`。
   - **Conversion**：搬运内层循环 `j+1`（Dump 已按 conversion 步进）。
   - **Logic**：GT6 是 Core 自己的导入/导出/整理循环次数。CC Core **不做**
     那三档（管网六行已做）。本卡用 Dump 成功步进当作 Logic used
     （SOURCE_DERIVED），不偷偷把 Core 传输补回来。
   - **Storage**：GT6 `MultiTileEntityLogisticsCore` **从不增加**
     `oCPU_Storage`。CC 同样保持 0，禁止编造仓储负荷。
5. **fail-closed**：四个 path 已加入
   `LogisticsDumpKinds.KNOWN_LOGISTICS_PATHS`。sidecar 四行可安装。
6. **sidecar**：`logistics_display_cpu_cover_definitions.json` 四行。
   相邻封面底表是 `src/main/resources/data/cruciblecraft/cover_definitions.json`：
   过滤器 / 单向阀 / 泵 / 传送带 / 机械臂等 **9 条相邻搬运盖板**（8 个
   `registerBuiltin` 行为）。那是管子对管子的封面，不是物流网。物流盖板
   （仓储 / 导入 / 导出 / Dump / Display）走各自 sidecar，禁止写进这份底表。
7. **配方**：shaped 几何 SOURCE_BACKED（空白盖板位 + 电路 + 线 + 螺丝刀
   方位）；缺 `Cover_Blank` / `OD_CIRCUITS[2]` / Lumium 线时用已有
   `programmed_circuit` 与原版红石零件做 **DESIGN_POLICY 生存获得**。
   shapeless 四向循环 SOURCE_BACKED。
8. **贴图**：从本地 `gregtech6_w` 迁入 `gt6_import/`，记 manifest。
   禁止拿 casing / 传送带 / 泵当占位。

## 本卡做什么 / 不做什么

做：

- 四件盖板物品、sidecar、生存配方、EMI、创造栏、中英名。
- Core 扫描到 Display 盖板时写红石与视觉；管子能输出该红石。
- 管面 BER 用 0–10 帧。
- GameTest：贴上能写回、红石随 used 变、缺 Core 为 0、shapeless 循环、
  生存合成。
- 新 capability `logistics/display-cpu`。

不做：

- 处理器单元方块（已在 `logistics/logistics-core`）。
- 处理器晶体物品（材料/电路债）。
- Core 自己的导入/导出/整理。
- 流体 Dump、Batteries、detector / controller 余量封面。
- GT6 电线宿主。
- 改 sealed R0 artifact / `growth_order.json` / census。
- 把 Display 写进相邻封面底表 `cover_definitions.json` 或 Generic sidecar。

## 验收门

- `maturity = player_complete` 且 `workflow = accepted`。
- `tools/capabilities/ledger.json` 的 `declared_player_complete` 包含本 slug。
- 相邻封面底表仍 9 条、`registerBuiltin` 仍 8；四件 Display 走本卡 sidecar。
- 物流 R0 `display_cpu_out_of_scope` 原字节保留。
- 不创建 Git commit（除非用户另行要求）。
- `player_signoff` 已签收（中文名 物流监视器(逻辑/控制/存储/转换处理器)）。

## 验收命令

```powershell
python tools/build_capability_ledger.py --check
python tools/build_registry_identity.py --check
python tools/verify.py integration --profile capability-runtime
python tools/build_player_complete.py --run --capability logistics/display-cpu
```
