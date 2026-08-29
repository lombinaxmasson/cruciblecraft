# CrucibleCraft 阶段档案 · T36-Repair

> 状态：✅ `T36_REPAIR_READY`（2026-08-28）
> 性质：T36 内部机器档位扩展 repair gate
> `owns_families=0`、`completion_delta=0`
> 不占用 T44，不撤销 `T36_READY`，不扩张 live 85 行

## 关闭结果

- 加工机 variant / 壳体 / kind 取得模板 / 装置材质收成数据权威
- 生产 `machine_tiers.json` 仍 85 行 / 27 kind；`automaticKindTierCompletion=false`
- `bronze_crusher` 继续 `skipGenericRegistration` + 独立 `CrusherBlock`
- 同 kind 双模板保留：opening mixer/sluice/compressor 等走 `machine_generic`，高档走壳体模板
- 壳体不等于机器材料：`invar_smelter` / `invar_drying` / `invar_distillery` 用钢壳体，
  `invar_roaster` 用殷钢壳体；蒸馏线材按机器材料、电解电缆按壳体材料
- Opening 33 从 `machine_acquisition.json` 投影，不再以 Java `OPENING_VARIANT_IDS` 为权威
- L1/L2/装置 L1 用 test-only overlay 证明：加档不改 Java、不 mutation 生产静态 catalog
- 第二份名单 inventory `blocking_count=0`；`ModProcessingMachines` 显式 `later:kind_behavior`
- 生产三档坩埚 / 四档砧 / 三档锤数值与 R0 freeze 相等
- T43 closing gap 仍为 3076；`next_issue_id=T44`；`preassigned_host=false`；
  `unique_active_content_card=null`

## 权威 artifacts

- `tools/t36_repair_pre_freeze.json`（write-once；含 85 台取得配方语义快照）
- `tools/t36_repair_second_list_inventory.json`
- `tools/t36_repair_readiness.json`（`T36_REPAIR_READY`，`failed_gates=[]`）
- `src/main/resources/data/cruciblecraft/machine_kinds.json`
- `src/main/resources/data/cruciblecraft/machine_casings.json`
- `src/main/resources/data/cruciblecraft/device_materials.json`
- `src/main/resources/data/cruciblecraft/machine_acquisition.json`
- `src/test/resources/data/cruciblecraft/t36_repair_overlay/`
- `src/test/java/com/masson/cruciblecraft/machine/processing/T36RepairExtensibilityTest.java`
- `src/test/java/com/masson/cruciblecraft/machine/processing/CatalogTestSupport.java`

## 验证

- `python -m unittest discover -s tools/tests -p "test_build_t36_repair*.py"`
- `.\gradlew.bat test --tests com.masson.cruciblecraft.machine.processing.T36RepairExtensibilityTest --tests com.masson.cruciblecraft.machine.MachineMaterialRulesTest --tests com.masson.cruciblecraft.registry.T36RuntimeEqualityTest --no-daemon`
- `.\gradlew.bat runGameTestServer -Pt36Machines --no-daemon`：隔离 `cruciblecraft_t36` **8/8**
- `python tools/verify.py integration --profile census-replay`：退出 0
- T38/T39 isolated GameTest 在本卡 Java 与本地 recovery/support 树上重跑后收据 rebound；
  T40/T41/T43 收据 behavior 哈希未漂移

`failed_gates=[]`。T44 仅保留连续编号，未预分配 host/families。Storage 28/624 不预分配。
