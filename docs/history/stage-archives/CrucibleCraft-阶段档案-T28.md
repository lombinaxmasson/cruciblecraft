# CrucibleCraft 阶段档案 · T28

> 阶段：T28 · O-36 热锭生命周期保真
> 状态：● 已关闭（`T28_READY`，2026-08-18 `--check-ready`）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 产品决策：`strict_no_conversion`
> 权威产物：`tools/t28_readiness.json` · 取证：`tools/t28_hot_ingot_source_evidence.json` · 载荷：`tools/t28_load_projection.json` · 账本：`tools/t28_publication_delta.json`

## 1. 关闭判据

T28 用固定 revision 证明 GT6 没有被动 `ingotHot → ingot` 或通用冷却 RecipeMap，
退役 CrucibleCraft 的 T10 `DESIGN_POLICY` 冷却规则与自动换物，保留热锭身份和
静态接触伤害。O-36 的 replacement 是 **absence of conversion**，不是另找一条
GT6 冷却配方填进去。本卡不实现 `cruciblecraft:large_crucible`，不启动 Track A–E。

## 2. 三轴

**Closure**

- `recipe/t10/cooling/hot_ingot_to_ingot.json` 已删除；
- `MaterialItemCooling` 已删除；`HeatMaintenanceEvents` 不再查询 cooling map；
- smelter `ingot_to_hot_ingot.json` 仍在；空 `ModRecipeMaps.COOLING` 仍注册；
- GameTest `hotIngotSmeltsHurtsAndKeepsIdentity` 证明会烫、不换物品 id；
- T29 仍 `started=false`。

**Fidelity**

- GT6 `ingotHot.mHeatDamage` / 材料叠加伤害仍由 `MaterialContactHeat` 提供；
- freezer 仍 `post_1_0`，未冒充冷却机；
- `large_crucible` 未注册。

**Load**

| 轴 | T27 opening | T28 delta | live |
|---|---:|---:|---:|
| logical | 19,087 | −321 | 18,766 |
| eager | 16,862 | −321 | 16,541 |
| lazy | 2,225 | 0 | 2,225 |

−321 是退役的 cooling expansion，不是当前 T10 `recipe_count`（smelter 热锭仍计 321）。
T14 `logical_recipes == 18875` 与 T11 compact datapack **3243** 仍分轴，不得拿 T28
live publication 去改那些钉。eager 仍低于 21,000。

## 3. 验证与交接

```text
PYTHONUTF8=1 python tools/run_full_verification.py --record --new-session
PYTHONUTF8=1 python tools/run_full_verification.py --check-ready
```

**2026-08-18 绑定事实（`--check-ready` 退出 0）：**

| 项 | 状态 |
|---|---|
| 报告 | `status = READY`；session `20260818T203334.712120Z-9e88ae92c5dd-d9395b36` |
| SHA-256 | `4d2b5ac0a85f7ca2e65b7c52e7ff64ee36b5aed12b9409b03f08cee24e672878` |
| T28 | `t28_readiness_acceptance.status = T28_READY` |
| T27 | 仍 `T27_READY`；opening 19087/16862/2225 未改 |
| JUnit / GameTest | **584** 全绿 / **121/121** |
| Python / datapack | **776** / report **3,243** |
| cooling | live expansion **0** |
| publication | **18766 / 16541 / 2225**；delta **−321 / −321 / 0** |

T27 拓扑 `started=false`、card_count=2 保持冻结。T28_READY 独立证明 O-36
replacement；不要回写 T27 portfolio 把 O-36 改成 closed。

**交给 T29**：

- identity 只有 `multiblock_kinds/crucible`；
- 新 controller id 必须是 `cruciblecraft:large_crucible`，不得与单块 `cruciblecraft:crucible` 冲突；
- T29 opening publication 以本卡 live **18766 / 16541 / 2225** 为起点，不能继续写 19,087；
- T29 关闭 ≠ 1.0 发货；之后仍要 portfolio replay、RC（F003/F005、≥16 GB）、打包。
