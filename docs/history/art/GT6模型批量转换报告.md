# GT6 方块模型批量转换报告

来源：GregTech6 `RendererBlockTextured` 空态循环（`getRenderPasses2` / `usesRenderPass2` / `setBlockBounds2` / `getTexture2`），
由 `tools/gt6_isbrh_to_json.py` 导出为 Minecraft JSON `elements`。只保留空机器身：熔融液面、槽位物品、NEI 水印、储能条都不写入。

## 转换规则说明

- 朝向取南（`mFacing = SIDE_Z_POS`）；`aShouldSideBeRendered` 取 `SIDES_ITEM_RENDER`（六面都画）
- `getTexture2` 返回 `null` / molten / fluid / `BI.nei()` / 槽位贴图的面被丢弃；六面皆空则丢弃该盒子
- 坐标为 0–16 像素；`PX_P` / `PX_N` / `PX_OFFSET` 与 GT6 `CS` 一致
- 贴图占位为 `#texture`（机身）。动态内容请用 BER 或已有 filled 变体，不要把液面烤进空态 JSON

## 转换结果（74 个成功 / 74 个目标）

| 类名（文件名） | 名称 | 盒子数 | 备注 |
|---|---|---|---|
| MultiTileEntityAneutronicFusion | 聚变电池 | 1 |  |
| MultiTileEntityAnvil | 砧 | 5 |  |
| MultiTileEntityBarometerGasCylinder | 储气瓶（气压计） | 5 |  |
| MultiTileEntityBasin | 浇铸盆 | 5 |  |
| MultiTileEntityBathingPot | 浸洗盆 | 5 |  |
| MultiTileEntityBathingPotTable | 浸洗盆桌 | 6 |  |
| MultiTileEntityBathingPotTableWood | 木浸洗盆桌 | 6 |  |
| MultiTileEntityBatteryAdvEU128 | 高级 EU 电池 (128) | 1 |  |
| MultiTileEntityBatteryAdvEU2048 | 高级 EU 电池 (2048) | 1 |  |
| MultiTileEntityBatteryAdvEU32 | 高级 EU 电池 (32) | 1 |  |
| MultiTileEntityBatteryAdvEU512 | 高级 EU 电池 (512) | 1 |  |
| MultiTileEntityBatteryAdvEU8 | 高级 EU 电池 (8) | 1 |  |
| MultiTileEntityBatteryEU128 | EU 电池 (128) | 1 |  |
| MultiTileEntityBatteryEU2048 | EU 电池 (2048) | 1 |  |
| MultiTileEntityBatteryEU32 | EU 电池 (32) | 1 |  |
| MultiTileEntityBatteryEU512 | EU 电池 (512) | 1 |  |
| MultiTileEntityBatteryEU8 | EU 电池 (8) | 1 |  |
| MultiTileEntityBatteryLU128 | LU 电池 (128) | 1 |  |
| MultiTileEntityBatteryLU2048 | LU 电池 (2048) | 1 |  |
| MultiTileEntityBatteryLU32 | LU 电池 (32) | 1 |  |
| MultiTileEntityBatteryLU512 | LU 电池 (512) | 1 |  |
| MultiTileEntityBatteryLU8 | LU 电池 (8) | 1 |  |
| MultiTileEntityBatteryLU8192 | LU 电池 (8192) | 1 |  |
| MultiTileEntityBookShelf | 书架 | 7 |  |
| MultiTileEntityBottleCrate | 瓶装箱 | 9 |  |
| MultiTileEntityBush | 灌木 | 2 |  |
| MultiTileEntityButtonAdvanced | 高级按钮 | 1 |  |
| MultiTileEntityCell | 流体单元 | 3 |  |
| MultiTileEntityCertificate | 证书牌 | 1 |  |
| MultiTileEntityCrank | 手摇曲柄 | 2 |  |
| MultiTileEntityCrossing | 十字轨 | 11 |  |
| MultiTileEntityCrucible | 坩埚（多方块） | 5 |  |
| MultiTileEntityCup | 杯子 | 5 |  |
| MultiTileEntityDustFunnel | 粉末漏斗 | 6 |  |
| MultiTileEntityDynamite | 炸药 | 1 |  |
| MultiTileEntityEnderGarbageBin | 末影垃圾桶 | 2 |  |
| MultiTileEntityEngineElectric | 电动引擎 | 5 |  |
| MultiTileEntityEngineFlux | 通量引擎 | 5 |  |
| MultiTileEntityEngineSteam | 蒸汽引擎 | 7 |  |
| MultiTileEntityFaucet | 浇铸口 | 3 |  |
| MultiTileEntityFluidCapNozzle | 封盖喷嘴 | 2 |  |
| MultiTileEntityFluidFunnel | 流体漏斗 | 3 |  |
| MultiTileEntityFluidNozzle | 流体喷嘴 | 2 |  |
| MultiTileEntityFluidTap | 流体龙头 | 3 |  |
| MultiTileEntityGrindStone | 砂轮 | 4 |  |
| MultiTileEntityHopper | 漏斗 | 3 |  |
| MultiTileEntityJug | 水壶 | 5 |  |
| MultiTileEntityJuicer | 榨汁机 | 6 |  |
| MultiTileEntityLargeTurbine | 大涡轮转子 | 2 |  |
| MultiTileEntityLaserBuildcraft | BC 激光 | 5 |  |
| MultiTileEntityMassStorageBarrel | 大容量桶 | 2 |  |
| MultiTileEntityMassStorageBox | 大容量箱 | 2 |  |
| MultiTileEntityMassStorageLogistics | 大容量储物（物流） | 2 |  |
| MultiTileEntityMassStorageStandard | 大容量储物（标准） | 2 |  |
| MultiTileEntityMeasuringPot | 量杯 | 5 |  |
| MultiTileEntityMixingBowl | 搅拌碗 | 5 |  |
| MultiTileEntityMixingBowlTable | 搅拌碗桌 | 6 |  |
| MultiTileEntityMold | 坩埚模具 | 42 |  |
| MultiTileEntityMoldCoinage | 压币模具 | 6 |  |
| MultiTileEntityMortar | 研钵 | 6 |  |
| MultiTileEntityPlantPot | 花盆 | 2 |  |
| MultiTileEntityPowerCell | 能量电池 | 1 |  |
| MultiTileEntityQueueHopper | 队列漏斗 | 3 |  |
| MultiTileEntityReactorCore1x1 | 反应堆堆芯 1×1 | 7 |  |
| MultiTileEntityReactorCore2x2 | 反应堆堆芯 2×2 | 10 |  |
| MultiTileEntityReactorRodBase | 燃料棒底座 | 1 |  |
| MultiTileEntityRope | 绳子 | 1 |  |
| MultiTileEntitySapBag | 树脂袋 | 1 |  |
| MultiTileEntityScaffold | 脚手架 | 7 |  |
| MultiTileEntitySensor | 传感器 | 7 |  |
| MultiTileEntitySiftingTable | 筛台 | 7 |  |
| MultiTileEntitySmeltery | 小冶炼炉 | 5 |  |
| MultiTileEntityThermos | 保温瓶 | 1 |  |
| MultiTileEntityZPM | ZPM 电池 | 2 |  |

## 失败项

无。

## 未转换项

- `TileEntityBase04/06Covers`、`TileEntityBase10/11Connector*`：基类，管线/覆膜模型按连接状态程序化生成，无固定空态形状
- 动画 TESR（蒸汽引擎活塞 TESR 注释块、Mass Storage 计数器 TESR）不在 ISBRH 循环内
