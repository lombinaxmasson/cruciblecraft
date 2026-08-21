# GT6 方块模型批量转换报告

来源：GregTech6 官方仓库（master 分支）`setBlockBounds2` 建模代码，自动提取转换为 Minecraft 1.8+ JSON 方块模型（elements 格式），可直接用 Blockbench 打开。

## 转换规则说明

- 坐标已换算为 JSON 标准 0–16 像素刻度；`PX_OFFSET`（0.005）等微调常量已代入计算
- 带朝向（mFacing）的模型统一取**默认朝向（朝南 Z+）**，其他朝向在 blockstates 里用 y 旋转 90/180/270 即可
- 含运行时变量的条件（如模具雕刻状态 mShape、炸药埋入状态 mSunk）取默认状态
- 贴图为占位变量：`#texture`（本体材质，GT 里按材料自动生成）和 `#molten`（熔融金属顶面），请替换为实际贴图路径
- 「跳过 N」指该类还有 N 个动态盒子未转换（多为其他朝向变体或动画部件）

## 转换结果（74 个模型）

| 类名（文件名） | 名称 | 盒子数 | 备注 |
|---|---|---|---|
| MultiTileEntityBookShelf | 书架 | 6 | ，有动态部件未转 |
| MultiTileEntitySensor | 传感器 | 37 |  |
| MultiTileEntityAneutronicFusion | 聚变电池 | 1 |  |
| MultiTileEntityBatteryAdvEU128 | 高级 EU 电池 (128) | 1 | ，有动态部件未转 |
| MultiTileEntityBatteryAdvEU2048 | 高级 EU 电池 (2048) | 1 | ，有动态部件未转 |
| MultiTileEntityBatteryAdvEU32 | 高级 EU 电池 (32) | 1 | ，有动态部件未转 |
| MultiTileEntityBatteryAdvEU512 | 高级 EU 电池 (512) | 1 | ，有动态部件未转 |
| MultiTileEntityBatteryAdvEU8 | 高级 EU 电池 (8) | 1 | ，有动态部件未转 |
| MultiTileEntityBatteryEU128 | EU 电池 (128) | 1 | ，有动态部件未转 |
| MultiTileEntityBatteryEU2048 | EU 电池 (2048) | 1 | ，有动态部件未转 |
| MultiTileEntityBatteryEU32 | EU 电池 (32) | 1 | ，有动态部件未转 |
| MultiTileEntityBatteryEU512 | EU 电池 (512) | 1 | ，有动态部件未转 |
| MultiTileEntityBatteryEU8 | EU 电池 (8) | 1 | ，有动态部件未转 |
| MultiTileEntityPowerCell | 能量电池 | 1 |  |
| MultiTileEntityBatteryLU128 | LU 电池 (128) | 1 |  |
| MultiTileEntityBatteryLU2048 | LU 电池 (2048) | 1 |  |
| MultiTileEntityBatteryLU32 | LU 电池 (32) | 1 |  |
| MultiTileEntityBatteryLU512 | LU 电池 (512) | 1 |  |
| MultiTileEntityBatteryLU8 | LU 电池 (8) | 1 |  |
| MultiTileEntityBatteryLU8192 | LU 电池 (8192) | 1 |  |
| MultiTileEntityZPM | ZPM 电池 | 2 |  |
| MultiTileEntityEngineElectric | 电动引擎 | 5 |  |
| MultiTileEntityEngineFlux | 通量引擎 | 5 |  |
| MultiTileEntityEngineSteam | 蒸汽引擎 | 7 |  |
| MultiTileEntityLaserBuildcraft | BC 激光 | 5 | ，有动态部件未转 |
| MultiTileEntityReactorCore1x1 | 反应堆堆芯 1×1 | 8 |  |
| MultiTileEntityReactorCore2x2 | 反应堆堆芯 2×2 | 11 |  |
| MultiTileEntityReactorRodBase | 燃料棒底座 | 1 |  |
| MultiTileEntityBottleCrate | 瓶装箱 | 36 |  |
| MultiTileEntityEnderGarbageBin | 末影垃圾桶 | 2 |  |
| MultiTileEntityHopper | 漏斗 | 3 |  |
| MultiTileEntityMassStorageBarrel | 大容量桶 | 24 |  |
| MultiTileEntityMassStorageBox | 大容量箱 | 24 |  |
| MultiTileEntityMassStorageLogistics | 大容量储物（物流） | 36 |  |
| MultiTileEntityMassStorageStandard | 大容量储物（标准） | 36 |  |
| MultiTileEntityQueueHopper | 队列漏斗 | 3 |  |
| MultiTileEntityCertificate | 证书牌 | 1 |  |
| MultiTileEntityCrucible | 坩埚（多方块） | 5 | ，有动态部件未转，含熔融金属面 |
| MultiTileEntityLargeTurbine | 大涡轮转子 | 1 |  |
| MultiTileEntityBush | 灌木 | 2 |  |
| MultiTileEntityButtonAdvanced | 高级按钮 | 1 |  |
| MultiTileEntityBarometerGasCylinder | 储气瓶（气压计） | 5 |  |
| MultiTileEntityCell | 流体单元 | 3 |  |
| MultiTileEntityCup | 杯子 | 6 |  |
| MultiTileEntityJug | 水壶 | 6 |  |
| MultiTileEntityMeasuringPot | 量杯 | 6 |  |
| MultiTileEntityThermos | 保温瓶 | 1 |  |
| MultiTileEntityAnvil | 砧 | 8 |  |
| MultiTileEntityBasin | 浇铸盆 | 6 | ，含熔融金属面 |
| MultiTileEntityBathingPot | 浸洗盆 | 7 |  |
| MultiTileEntityBathingPotTable | 浸洗盆桌 | 8 |  |
| MultiTileEntityBathingPotTableWood | 木浸洗盆桌 | 8 |  |
| MultiTileEntityCrank | 手摇曲柄 | 2 |  |
| MultiTileEntityCrossing | 十字轨 | 11 |  |
| MultiTileEntityDustFunnel | 粉末漏斗 | 5 | ，有动态部件未转 |
| MultiTileEntityDynamite | 炸药 | 1 |  |
| MultiTileEntityFaucet | 浇铸口 | 3 |  |
| MultiTileEntityFluidCapNozzle | 封盖喷嘴 | 2 |  |
| MultiTileEntityFluidFunnel | 流体漏斗 | 3 |  |
| MultiTileEntityFluidNozzle | 流体喷嘴 | 2 |  |
| MultiTileEntityFluidTap | 流体龙头 | 3 |  |
| MultiTileEntityGrindStone | 砂轮 | 6 |  |
| MultiTileEntityJuicer | 榨汁机 | 8 |  |
| MultiTileEntityMixingBowl | 搅拌碗 | 7 |  |
| MultiTileEntityMixingBowlTable | 搅拌碗桌 | 8 |  |
| MultiTileEntityMold | 坩埚模具 | 44 | ，有动态部件未转，含熔融金属面 |
| MultiTileEntityMoldCoinage | 压币模具 | 7 |  |
| MultiTileEntityMortar | 研钵 | 7 |  |
| MultiTileEntityPlantPot | 花盆 | 2 |  |
| MultiTileEntityRope | 绳子 | 2 |  |
| MultiTileEntitySapBag | 树脂袋 | 1 |  |
| MultiTileEntityScaffold | 脚手架 | 14 |  |
| MultiTileEntitySiftingTable | 筛台 | 9 |  |
| MultiTileEntitySmeltery | 小冶炼炉 | 5 | ，有动态部件未转，含熔融金属面 |

## 未转换项

- `TileEntityBase04/06Covers`、`TileEntityBase10/11Connector*`：基类，管线/覆膜模型按连接状态程序化生成，无固定形状
- `MultiTileEntityChest`（example 目录）：教学示例
- 动画模型（蒸汽引擎活塞、Mass Storage 计数器等 TESR 部分）仅导出了静态主体