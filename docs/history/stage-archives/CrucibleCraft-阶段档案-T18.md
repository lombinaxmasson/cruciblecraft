# CrucibleCraft 阶段档案 · T18

> 状态：历史档案；T18 已关闭。
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 最终状态：`T18_READY`
> 当前执行入口：《[CrucibleCraft-第三阶段总体规划.md](CrucibleCraft-第三阶段总体规划.md)》T19

## T18 · 蒸汽、燃油与能量转换 ✅

- canonical denominator 为 29 machine kinds + STEAM/AU 两个 energy identities，`unclassified = 0`。
- 本批 selected 6：Bronze Firebox、Boiler、Steam Engine、Dynamo、Fuel Engine、Burning Gas Generator；preimplemented reference 3：Axle、Gearbox、Electric Motor；deferred 20。`T18_READY` 不表示 29 kind 全部实现。
- 四条守恒链已关闭：Firebox → Boiler → Steam Engine、Fuel → RU、RU → EU、Gas → HU。Boiler 固定 80 HU + 1 mB water → 160 mB steam；Steam Engine 的 source row 1302 与 `STEAM_PER_EU = 2` 证明 200 mB→50 KU、4 mB/KU 为 `SOURCE_BACKED` 守恒，`24 / 2 = 12 KU` 只作为 `SOURCE_DERIVED_NOMINAL` mOutput，CC 恒定 12 KU/t 则是 `DESIGN_POLICY_FIXED_OUTPUT`；GT6 实际 6–24 KU/t state-dependent 行为保留带 replacement condition / recheck point 的 deferred replacement。Fuel Engine 固定 512 RU / fuel unit；Dynamo 固定 32 RU → 22 EU + 10 loss；methane 固定 1,536 source units → 1,152 HU + 9 mB exhaust。
- Fuel Engine 的精确 legacy ELECTRIC → KINETIC_ROTATION 与 Gas Generator 的精确 ELECTRIC → HEAT migration 已关闭；所有 near miss 均 quarantine。
- `t18_converter_acquisition.json` 从独立 policy 证明六个 profile 的 recipe/result/operand producer 全部可达，`unreachable = 0`；profile/block/item/blockstate/block model/item model/en_us/zh_cn/loot/pickaxe tag 集合双向闭合。
- `t18_load_projection_input.json` / `t18_load_projection.json` 为 delivery T18 zero-workload `PASS`：tier/profile/identity 调整与六条 vanilla crafting 获取路线不新增 GT RecipeMap row，全部 load interval 为 0。
- RecipeMap stable id 32 个、logical / eager / lazy 18,875 / 16,650 / 2,225、EMI configured map 24 个及完整 recipe enumeration 均与 T17 baseline 完全相等；GT publication delta 为 0。
- O-37 由完整固定 revision Java tree replay 永久关闭为 `O37_CLOSED_PERMANENT_DESIGN_POLICY`：`liquid_medium_oil` 与 material 9852 没有 direct binding，material 9852 只保留 `SOURCE_MATERIAL_LAYER_ONLY` 角色；不改 T9 identity、不新增 runtime registration，独立 publication delta 为 0。
- T18a–d complete，pending 为空；denominator、四链守恒、migration、acquisition、load、publication、EMI、O-37 与 currentness 均进入最终统一 verification session。

## 2026-08-07 · tier band 身份 currentness

- Processing machine NBT 为 v3：只写 `tier_band`；输入含已移除的 `tier_profile` 会 fail closed，并通过持久化 quarantine 保持到后续重载。
- T18 energy-converter profile 与运行时能力保持不变；Fuel Engine/Gas Generator 只接受完整 current identity，其余输入 quarantine。本修订不触碰 T9 identity 或 `net/`。
- T12–T18 readiness/currentness 随共享 processing identity 与蒸汽输出分类证据逐级刷新；full verification report 由同一 snapshot session 的完整 builder、双 runData、全 JUnit、全 GameTest 与 Python closure 重新记录为 READY。

下一阶段为 T19 Cover 与管道获取闭包。
