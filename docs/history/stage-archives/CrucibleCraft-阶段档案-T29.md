# CrucibleCraft 阶段档案 · T29

> 阶段：T29 · `cruciblecraft:large_crucible` 有界热/炼钢多方块
> 状态：● 已关闭（`T29_READY`，2026-08-19 `--check-ready`）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> identity：`multiblock_kinds/crucible`
> 权威产物：`tools/t29_readiness.json` · 取证：`tools/t29_crucible_source_evidence.json` · 载荷：`tools/t29_load_projection.json` · 账本：`tools/t29_publication_delta.json`

## 1. 关闭判据

T29 用固定 revision 把 GT6 多方块坩埚的结构、端口层、容量和能量身份落到 CC schema v1：
注册并验证 `cruciblecraft:large_crucible`，3×3×3 开顶，**27** 位（不是 T23 误写的 28），容量 **432** 锭单位，底层吃 HU、中间层模具、顶层物品/流体，复用单块坩埚过程核。单块 `cruciblecraft:crucible`（8 锭）继续存在。完成 T29 **不等于** 1.0 发货。

## 2. 三轴

**Closure**

- `cruciblecraft:large_crucible` 已注册，不与 `cruciblecraft:crucible` 抢 id；
- 结构 27 位可形成、可拆解；容量 432；
- 六生命周期 + 432 + 炼钢 + 模具浇注 GameTest 全绿；
- plugin 为 `thermal_steelmaking_host` + `heat_energy_input` + `shared_port_supply`，不是 `processing_host`；
- portfolio 中 `multiblock_kinds/crucible` 的 replacement 已满足；`v1_work_set` 为空。

**Fidelity**

- 固定 revision 结构/容量/层角色 SOURCE_BACKED；
- 热标度 DESIGN_POLICY（未改成 GT6 `1 HU / 100 kg → +1 K`）；
- 炼钢 SOURCE_DERIVED（不宣称与 GT6 逐 tick 相等）；
- T23 crucible positions 已是 27。

**Load**

| 轴 | T29 opening（T28 live） | T29 delta | measured |
|---|---:|---:|---:|
| logical | 18,766 | 0 | 0 |
| eager | 16,541 | 0 | 0 |
| lazy | 2,225 | 0 | 0 |

vanilla 控制器合成是非 GT 轴，已记账。eager 仍低于 21,000。T27 opening 冻结 **19087 / 16862 / 2225** 未改。

## 3. 验证与交接

```text
PYTHONUTF8=1 python tools/run_full_verification.py --record --new-session
PYTHONUTF8=1 python tools/run_full_verification.py --check-ready
```

**2026-08-19 绑定事实（`--check-ready` 退出 0）：**

| 项 | 状态 |
|---|---|
| 报告 | `status = READY`；绑定 session `20260819T130022.811338Z-579e7de08452-98f53cb1`；确认 session `20260819T132354.161208Z-579e7de08452-c373155b` |
| SHA-256 | `7e7f97d8fe5069335f6234513d4072d756da6800eca4a3e7d54f2bd10e0f82a4` |
| T29 | `t29_readiness_acceptance.status = T29_READY` |
| T28 / T27 | 仍 `T28_READY` / `T27_READY`；T28 产物里 `large_crucible_unregistered` 是历史冻结 |
| JUnit / GameTest | **593** 全绿 / **131/131** |
| Python / datapack | **790** / report **3,243** |
| publication | opening **18766 / 16541 / 2225**；GT delta **0/0/0** |
| RC | `rc_number = null`；F003/F005 仍挂 T27 RC candidate，本卡未标 closed |

只读 replay：拓扑 `card_count = 0`、`started = false`、`work_set` 空；`t28_plus_card_count` 仍不要手填成 RC。

**T29 之后（不得跳）：**

1. Portfolio replay 已证明未关闭 `v1_required` = 0（本卡关闭后立刻做完）；
2. **RC candidate**（编号由 replay 生成，不在本卡猜 T30）：声明 `>= 16 GB RAM`，重测 T24-F003 / T24-F005，打包/协议/迁移；仍禁止 `--write-tooling-snapshot`；
3. **GA / v1.0**：RC 全绿后的发货命名；
4. Hopper、物品桶、其余多方块、Track A–E 仍是 post_1_0，不因 T29 升级。

v1 实现工作集已空。下一张允许讨论的卡是 RC candidate，不是 GA，不是 Track A。
