# 项目状态

> 本页由 `python tools/build_project_status.py --write` 从
> `tools/capabilities/**/capability.json` 与 `docs/history/card-plans/`
> 生成。不要手改。ledger.json 与本页都是可重建投影，不是权威。
> GT6 全量源覆盖另见 [gt6-full-coverage.md](gt6-full-coverage.md)。

## Unique active

`machines/gt6-multiblock-tanks`（GT6 Multiblock Tanks 17001-17067，`workflow=active`，`maturity=runtime_ready`）；计划 [GT6 多方块储罐详细计划](../history/card-plans/active/GT6多方块储罐详细计划.md)。

## Prep（不占落地锁）

计划文件存在就会列在这里。不要把本表当剩余待办。读法见 [unimplemented-gap.md](unimplemented-gap.md)。

- `content/gt6-crop-food-split` — [GT6 作物与 Foods 可选拆分详细计划](../history/card-plans/prep/GT6作物与Foods可选拆分详细计划.md)
- `content/puv-omega-tech-line` — [PUV2+ / OMEGA 科技线](../history/card-plans/prep/PUV2OMEGA科技线详细计划.md)
- `energy/cooler` — [冷却器详细计划](../history/card-plans/prep/冷却器详细计划.md)
- `energy/flux-converters` — [通量转换器详细计划](../history/card-plans/prep/通量转换器详细计划.md)
- `energy/small-gas-turbine` — [微型燃气涡轮详细计划](../history/card-plans/prep/微型燃气涡轮详细计划.md)
- `machines/large-bath` — [大型洗矿机详细计划](../history/card-plans/prep/大型洗矿机详细计划.md)
- `machines/printer` — [印刷机详细计划](../history/card-plans/prep/印刷机详细计划.md)
- `portfolio/gt6-bulk-port` — [GT6 批量移植总计划](../history/card-plans/prep/GT6批量移植总计划.md)
- `portfolio/gt6-bulk-port-verification` — [GT6 批量移植基础设施收口](../history/card-plans/prep/GT6批量移植基础设施收口详细计划.md)
- `portfolio/gt6-machine-multiblock` — [GT6 机器与多方块总计划](../history/card-plans/prep/GT6机器与多方块总计划.md)
- `registry/gt6-translator-mapping-repair` — [GT6 翻译链映射修复详细计划](../history/card-plans/prep/GT6翻译链映射修复详细计划.md)
- `registry/material-form-demand-census` — [材料形态需求普查详细计划](../history/card-plans/prep/材料形态需求普查详细计划.md)
- `worldgen/gt-center` — [GT Center 详细计划](../history/card-plans/prep/GT中枢详细计划.md)
- `worldgen/gt-dungeon` — [GT 地牢详细计划](../history/card-plans/prep/GT地牢详细计划.md)
- `worldgen/gt-planet-rocks` — [GT 行星岩详细计划](../history/card-plans/prep/GT行星岩详细计划.md)
- `worldgen/gt-small-ores` — [GT6 小矿世界生成](../history/card-plans/prep/GT6小矿世界生成详细计划.md)

## Playtest cycle

`2026-09-15-obtain-reset` **pending**. Major worldgen / gameplay / obtain / GUI / save / network changes open or extend this cycle. Ordinary bugfix does not invalidate it. Accept only after a human `runClient`; CI never auto-signs.

Retired catalog scatter obtain and abolished player_complete. First real project playtest is required.; closed content/gt6-crucible-mold-interaction; closed content/gt6-foundry-art; closed worldgen/gt-surface-rocks; closed worldgen/gt-stone-layer-rocks; closed registry/prefix-material-component; Opened unique-active registry/hybrid-material-identity: public 16 prefixes are unique Items again; closed registry/hybrid-material-identity; Opened unique-active content/gt6-storage-art: GT6 kind-level storage textures on live barrels/boxes/lockers; closed content/gt6-storage-art; closed machines/bath; closed machines/hammer-squeezer-laser; closed machines/distillation-tower; closed energy/large-gas-turbine; GT6 rubber tree sap, hole harvest, coagulator latex to nugget, and rubber-log squeezer; closed machines/large-processing-parts; closed machines/implosion-compressor; closed fluid/gt6-missing-fluids; closed recipe/gt6-bulk-capacity; closed recipe/gt6-extruder-bulk; closed recipe/gt6-extruder-remainder; closed recipe/gt6-prefix-regular-bulk; closed recipe/gt6-chemical-misc-bulk; closed machines/gt6-basic-machine-batch; Opened unique-active recipe/gt6-steamcracking-bulk: import the whole gt.recipe.steamcracking map; closed recipe/gt6-steamcracking-bulk; closed energy/gt6-laser-magnet-zpm-converters; closed energy/gt6-remainder-devices; closed content/gt6-fluid-barrel

## runtime_ready accepted（113）

机制可跑。`survival_access` 独立、不挡关闭。不得再用 catalog scatter 或历史 `player_complete` 签收当获得证明。

- `content/electric-wire-cable-mte-fold` — Electric Wire/Cable MTE Fold — [导线电缆 MTE 折回详细计划](../history/card-plans/closed/导线电缆MTE折回详细计划.md)
- `content/gt6-connector-alias-repair` — GT6 Connector Alias Repair — [GT6 连接件身份漏匹配详细计划](../history/card-plans/closed/GT6连接件身份漏匹配详细计划.md)
- `content/gt6-connector-art` — GT6 Connector Art — [GT6 连接件美术详细计划](../history/card-plans/closed/GT6连接件美术详细计划.md)
- `content/gt6-crucible-mold-behavior-correction` — GT6 Crucible Mold Behavior Correction — [GT6 坩埚模具行为校正详细计划](../history/card-plans/closed/GT6坩埚模具行为校正详细计划.md)
- `content/gt6-crucible-mold-interaction` — GT6 Crucible Mold Interaction — [GT6 坩埚模具交互详细计划](../history/card-plans/closed/GT6坩埚模具交互详细计划.md)
- `content/gt6-eu-cable-acquisition` — GT6 EU Cable Acquisition — [GT6 EU 线缆获得格详细计划](../history/card-plans/closed/GT6EU线缆获得格详细计划.md)
- `content/gt6-eu-missing-wire-gauges-runtime` — GT6 Missing EU Wire Gauges Runtime — [GT6 缺线规运行时详细计划](../history/card-plans/closed/GT6缺线规运行时详细计划.md)
- `content/gt6-eu-wire-cable-runtime` — GT6 EU Wire/Cable Runtime — [GT6 导线电缆运行时详细计划](../history/card-plans/closed/GT6导线电缆运行时详细计划.md)
- `content/gt6-fluid-barrel` — GT6 fluid storage tanks — [GT6流体储罐详细计划](../history/card-plans/closed/GT6流体储罐详细计划.md)
- `content/gt6-fluid-combo-pipe-runtime` — GT6 Fluid Combo Pipe Runtime — [GT6 流体组合管运行时详细计划](../history/card-plans/closed/GT6流体组合管运行时详细计划.md)
- `content/gt6-fluid-dangerous-media-runtime` — GT6 Fluid Dangerous Media Runtime — [GT6 流体危险介质运行时详细计划](../history/card-plans/closed/GT6流体危险介质运行时详细计划.md)
- `content/gt6-fluid-pipe-acquisition` — GT6 Fluid Pipe Acquisition — [GT6 流体管获得格详细计划](../history/card-plans/closed/GT6流体管获得格详细计划.md)
- `content/gt6-fluid-pipe-runtime` — GT6 Fluid Pipe Runtime — [GT6 流体管运行时详细计划](../history/card-plans/closed/GT6流体管运行时详细计划.md)
- `content/gt6-foundry-art` — GT6 Foundry Art — [GT6 铸造美术详细计划](../history/card-plans/closed/GT6铸造美术详细计划.md)
- `content/gt6-insulated-redstone-runtime` — GT6 Insulated Redstone Runtime — [GT6 绝缘红石运行时详细计划](../history/card-plans/closed/GT6绝缘红石运行时详细计划.md)
- `content/gt6-item-pipe-acquisition` — GT6 Item Pipe Acquisition — [GT6 物品管获得格详细计划](../history/card-plans/closed/GT6物品管获得格详细计划.md)
- `content/gt6-item-pipe-runtime` — GT6 Item Pipe Runtime — [GT6 物品管运行时详细计划](../history/card-plans/closed/GT6物品管运行时详细计划.md)
- `content/gt6-mte-converter-host-fold` — GT6 Converter Host Fold — [GT6 能源转换器主机折回详细计划](../history/card-plans/closed/GT6能源转换器主机折回详细计划.md)
- `content/gt6-mte-converter-remainder-runtime` — GT6 Converter Remainder Runtime — [GT6能源转换器余量runtime详细计划](../history/card-plans/closed/GT6能源转换器余量runtime详细计划.md)
- `content/gt6-mte-crucible-foundry-runtime` — GT6 Crucible Foundry Runtime — [GT6坩埚铸造runtime详细计划](../history/card-plans/closed/GT6坩埚铸造runtime详细计划.md)
- `content/gt6-mte-decorative-runtime` — GT6 Decorative Runtime — [GT6装饰件runtime详细计划](../history/card-plans/closed/GT6装饰件runtime详细计划.md)
- `content/gt6-mte-drive-runtime` — GT6 Drive Runtime — [GT6传动件runtime详细计划](../history/card-plans/closed/GT6传动件runtime详细计划.md)
- `content/gt6-mte-extender-runtime` — GT6 Extender Runtime — [GT6 扩展器 runtime 详细计划](../history/card-plans/closed/GT6扩展器runtime详细计划.md)
- `content/gt6-mte-fluid-attachments-runtime` — GT6 Fluid Attachments Runtime — [GT6 流体附件 runtime 详细计划](../history/card-plans/closed/GT6流体附件runtime详细计划.md)
- `content/gt6-mte-furniture-barrel-runtime` — GT6 Furniture Barrel Runtime — [GT6家具木桶runtime详细计划](../history/card-plans/closed/GT6家具木桶runtime详细计划.md)
- `content/gt6-mte-furniture-chest-runtime` — GT6 Furniture Chest Runtime — [GT6家具箱子runtime详细计划](../history/card-plans/closed/GT6家具箱子runtime详细计划.md)
- `content/gt6-mte-furniture-safe-runtime` — GT6 Furniture Safe Runtime — [GT6家具保险箱runtime详细计划](../history/card-plans/closed/GT6家具保险箱runtime详细计划.md)
- `content/gt6-mte-furniture-scaffold-runtime` — GT6 Furniture Scaffold Runtime — [GT6家具脚手架runtime详细计划](../history/card-plans/closed/GT6家具脚手架runtime详细计划.md)
- `content/gt6-mte-furniture-storage-runtime` — GT6 Furniture Storage Runtime — [GT6家具储物runtime详细计划](../history/card-plans/closed/GT6家具储物runtime详细计划.md)
- `content/gt6-mte-furniture-table-runtime` — GT6 Furniture Crafting Table Runtime — [GT6家具工作台runtime详细计划](../history/card-plans/closed/GT6家具工作台runtime详细计划.md)
- `content/gt6-mte-hopper-host-fold` — GT6 Hopper Host Fold — [GT6 漏斗主机折回详细计划](../history/card-plans/closed/GT6漏斗主机折回详细计划.md)
- `content/gt6-mte-inplace-acquisition` — GT6 In-place MTE Acquisition — [MTE In-place 获得格收口](../history/card-plans/closed/MTE原地获得格详细计划.md)
- `content/gt6-mte-misc-tool-runtime` — GT6 Misc Tool Runtime — [GT6杂项工具runtime详细计划](../history/card-plans/closed/GT6杂项工具runtime详细计划.md)
- `content/gt6-mte-multiblock-runtime` — GT6 Multiblock Runtime — [GT6多方块设备runtime详细计划](../history/card-plans/closed/GT6多方块设备runtime详细计划.md)
- `content/gt6-mte-processing-host-fold` — GT6 Processing Host Fold — [GT6 加工机主机折回详细计划](../history/card-plans/closed/GT6加工机主机折回详细计划.md)
- `content/gt6-mte-reactor-rod-host-fold` — GT6 Reactor Rod Host Fold — [GT6 反应棒主机折回详细计划](../history/card-plans/closed/GT6反应棒主机折回详细计划.md)
- `content/gt6-paper-tiny-plate` — GT6 Paper Tiny Plate — [GT6 纸微型板](../history/card-plans/closed/GT6纸微型板详细计划.md)
- `content/gt6-redstone-wire-acquisition` — GT6 Redstone Wire Acquisition — [GT6 绝缘红石获得格详细计划](../history/card-plans/closed/GT6绝缘红石获得格详细计划.md)
- `content/gt6-redstone-wire-correction` — GT6 Redstone Wire Correction — [GT6 红石线行为校正详细计划](../history/card-plans/closed/GT6红石线行为校正详细计划.md)
- `content/gt6-restrictive-item-pipe-runtime` — GT6 Restrictive Item Pipe Runtime — [GT6 限制物品管运行时详细计划](../history/card-plans/closed/GT6限制物品管运行时详细计划.md)
- `content/gt6-storage-art` — GT6 Storage Art — [GT6仓储美术详细计划](../history/card-plans/closed/GT6仓储美术详细计划.md)
- `content/mte-redstone-wire` — MTE Redstone Wire — [MTE 红石线详细计划](../history/card-plans/closed/MTE红石线详细计划.md)
- `content/sensors` — Sensors — [Sensors 详细计划](../history/card-plans/closed/Sensors详细计划.md)
- `content/technological-parts-foundation` — Technological Parts Foundation — [技术中间件基础详细计划](../history/card-plans/closed/技术中间件基础详细计划.md)
- `energy/batteries` — Energy Batteries
- `energy/converter-catalog` — Energy Converter Catalog
- `energy/gt6-laser-magnet-zpm-converters` — GT6 laser, magnet, and ZPM converters — [GT6 激光、磁铁与 ZPM 转换器详细计划](../history/card-plans/closed/GT6激光磁铁ZPM转换器详细计划.md)
- `energy/gt6-remainder-devices` — GT6 remainder energy devices — [GT6 余量能源设备详细计划](../history/card-plans/closed/GT6余量能源设备详细计划.md)
- `energy/heat-exchangers` — Heat Exchangers — [热交换器第一切片详细计划](../history/card-plans/closed/热交换器第一切片详细计划.md)
- `energy/large-gas-turbine` — Large Gas Turbine 17231-17234 — [大型燃气轮机详细计划](../history/card-plans/closed/大型燃气轮机详细计划.md)
- `energy/nuclear-fission-hot-fluids` — Nuclear Fission Hot Fluids — [裂变热流体与热量合同详细计划](../history/card-plans/closed/裂变热流体与热量合同详细计划.md)
- `energy/nuclear-fission-observation-safety` — Nuclear Fission Observation Safety — [裂变观测安全与能源 Jade 详细计划](../history/card-plans/closed/裂变观测安全与能源Jade详细计划.md)
- `energy/nuclear-fission-survival` — Nuclear Fission Survival — [裂变生存闭环与全量棒堆芯详细计划](../history/card-plans/closed/裂变生存闭环与全量棒堆芯详细计划.md)
- `energy/transformers` — Energy Transformers
- `fluid/gt6-missing-fluids` — GT6 Missing Fluids — [GT6 缺失流体详细计划](../history/card-plans/closed/GT6缺失流体详细计划.md)
- `localization/language-key-display-name-normalization` — Language Key and Display Name Normalization — [语言键与显示名规范收口详细计划](../history/card-plans/closed/语言键与显示名规范收口详细计划.md)
- `logistics/cover-remainder` — Cover remainder — [盖板余量详细计划](../history/card-plans/closed/盖板余量详细计划.md)
- `logistics/display-cpu` — Logistics Display CPU
- `logistics/fluid-network/basic-transfer` — Fluid network basic transfer
- `logistics/generic-network/core` — Generic network core
- `logistics/item-network-core` — Item network core
- `logistics/logistics-core` — Logistics Core
- `machines/bath` — Bath / Bathing Pot — [洗矿浴池详细计划](../history/card-plans/closed/洗矿浴池详细计划.md)
- `machines/bedrock-drill` — Bedrock Mining Drill 17999/18103
- `machines/cluster-mill` — Cluster Mill — [集群轧机详细计划](../history/card-plans/closed/集群轧机详细计划.md)
- `machines/coke-oven` — Coke Oven 17000 with Fire Bricks 18000
- `machines/distillation-tower` — Distillation Tower GT6 Alignment — [蒸馏塔 GT6 对齐详细计划](../history/card-plans/closed/蒸馏塔GT6对齐详细计划.md)
- `machines/gt6-basic-machine-batch` — GT6 basic machine batch — [GT6 同类基础加工机批量详细计划](../history/card-plans/closed/GT6同类基础加工机批量详细计划.md)
- `machines/gt6-coil-hosts` — GT6 coils 18040-18045 and host machines
- `machines/hammer-squeezer-laser` — Hammer / Squeezer / Laser — [锤 / 榨汁机 / 激光详细计划](../history/card-plans/closed/锤榨汁机激光详细计划.md)
- `machines/implosion-compressor` — Implosion Compressor 17110 — [聚爆压缩机详细计划](../history/card-plans/closed/聚爆压缩机详细计划.md)
- `machines/injector` — Injector — [注射机详细计划](../history/card-plans/closed/注射机详细计划.md)
- `machines/laminator` — Laminator — [层压机详细计划](../history/card-plans/closed/层压机详细计划.md)
- `machines/large-bathing-vat` — Large Bathing Vat 17104
- `machines/large-centrifuge` — Large Centrifuge 17100
- `machines/large-coagulator` — Large Coagulator Array 17105
- `machines/large-crucible` — Large Crucible 17101
- `machines/large-crusher` — Large Crusher 17108 with Crusher Wheels 18107
- `machines/large-electrolyzer` — Large Electrolyzer 17103 — [大型电解机详细计划](../history/card-plans/closed/大型电解机详细计划.md)
- `machines/large-mixer` — Large Batch Mixer 17102
- `machines/large-oven` — Large Electric Oven 17106
- `machines/large-processing-parts` — Large processing multiblock parts and maps — [大型加工多方块零件与配方图详细计划](../history/card-plans/closed/大型加工多方块零件与配方图详细计划.md)
- `machines/large-shredder` — Large Shredder 17109
- `machines/large-sluice` — Large Sluice 17107
- `machines/large-squeezer` — Large Squeezer 17114
- `machines/loom` — Loom — [织机详细计划](../history/card-plans/closed/织机详细计划.md)
- `machines/melter` — Melter — [熔融机详细计划](../history/card-plans/closed/熔融机详细计划.md)
- `machines/nanofab` — Nanoscale Fabricator — [纳米加工机详细计划](../history/card-plans/closed/纳米加工机详细计划.md)
- `machines/oven` — Oven — [熔炉详细计划](../history/card-plans/closed/熔炉详细计划.md)
- `machines/pressure-washer` — Pressure Washer — [压力清洗机详细计划](../history/card-plans/closed/压力清洗机详细计划.md)
- `machines/roll-former` — Roll Former — [辊压成型机详细计划](../history/card-plans/closed/辊压成型机详细计划.md)
- `machines/sanding` — Sanding Machine — [打磨机详细计划](../history/card-plans/closed/打磨机详细计划.md)
- `machines/slicer` — Slicer — [切片机详细计划](../history/card-plans/closed/切片机详细计划.md)
- `portfolio/default-gametest-recovery` — Default GameTest Grid Recovery — [默认 GameTest 主测试集复原详细计划](../history/card-plans/closed/默认GameTest主测试集复原详细计划.md)
- `portfolio/gt6-machine-ledger-audit` — GT6 machine ledger audit — [GT6 机器账本检查详细计划](../history/card-plans/closed/GT6机器账本检查详细计划.md)
- `portfolio/publication-reload-performance` — Publication Reload Performance — [配方发布重载性能详细计划](../history/card-plans/closed/配方发布重载性能详细计划.md)
- `portfolio/test-authoring-workflow` — Test Authoring Workflow — [测试制作流程详细计划](../history/card-plans/closed/测试制作流程详细计划.md)
- `recipe/gt6-bulk-capacity` — GT6 Bulk Recipe Capacity Gate — [GT6 批量配方容量门详细计划](../history/card-plans/closed/GT6批量配方容量门详细计划.md)
- `recipe/gt6-chemical-misc-bulk` — GT6 Chemical and Misc Recipe Bulk — [GT6 化学杂项配方批量详细计划](../history/card-plans/closed/GT6化学杂项配方批量详细计划.md)
- `recipe/gt6-extruder-bulk` — GT6 Extruder Recipe Bulk — [GT6 挤压机配方批量详细计划](../history/card-plans/closed/GT6挤压机配方批量详细计划.md)
- `recipe/gt6-extruder-remainder` — GT6 Extruder Recipe Remainder — [GT6 挤压机配方补发详细计划](../history/card-plans/closed/GT6挤压机配方补发详细计划.md)
- `recipe/gt6-prefix-regular-bulk` — GT6 Prefix Regular Recipe Bulk — [GT6 前缀规则类配方批量详细计划](../history/card-plans/closed/GT6前缀规则类配方批量详细计划.md)
- `recipe/gt6-steamcracking-bulk` — GT6 Steam Cracking Recipe Bulk — [GT6 蒸汽裂化配方批量详细计划](../history/card-plans/closed/GT6蒸汽裂化配方批量详细计划.md)
- `registry/catalog-modern-ids` — Catalog modern IDs — [目录身份现代 id 详细计划](../history/card-plans/closed/目录身份现代id详细计划.md)
- `registry/gt6-long-tail-forms` — GT6 Long-tail Forms — [GT6 长尾形态开门详细计划](../history/card-plans/closed/GT6长尾形态开门详细计划.md)
- `registry/gt6-storage-dust-blocks` — GT6 Storage Dust Blocks — [GT6 粉块批量详细计划](../history/card-plans/closed/GT6粉块批量详细计划.md)
- `registry/hybrid-material-identity` — Hybrid material identity — [材料身份分层混合详细计划](../history/card-plans/closed/材料身份分层混合详细计划.md)
- `registry/prefix-material-component` — Prefix Material Component — [材料前缀组件身份详细计划](../history/card-plans/closed/材料前缀组件身份详细计划.md)
- `registry/tool-head-prefix-reclaim` — Sharpener overflow reclaim — [工具头前缀与打磨机余量回收详细计划](../history/card-plans/closed/工具头前缀与打磨机余量回收详细计划.md)
- `worldgen/gt-crops` — GT Crops — [GT 作物世界生成](../history/card-plans/closed/GT作物世界生成详细计划.md)
- `worldgen/gt-stone-layer-rocks` — GT Stone Layer Rocks — [GT6 石层石子](../history/card-plans/closed/GT6石层石子详细计划.md)
- `worldgen/gt-surface-rocks` — GT Surface Rocks — [GT6 地表石子保真](../history/card-plans/closed/GT6地表石子保真详细计划.md)
- `worldgen/gt-trees` — GT Trees — [GT 树详细计划](../history/card-plans/closed/GT树详细计划.md)

## runtime_ready paused（4）

已有运行时代码或机制证据，但 workflow 仍暂停，不能视为 accepted。

- `energy/cooler` — Electric and Flux Coolers — [冷却器详细计划](../history/card-plans/prep/冷却器详细计划.md)
- `energy/flux-converters` — Flux FE to GU Converters — [通量转换器详细计划](../history/card-plans/prep/通量转换器详细计划.md)
- `machines/large-boiler` — Large Boiler 17201-17205 — [大型锅炉详细计划](../history/card-plans/closed/大型锅炉详细计划.md)
- `worldgen/gt-small-ores` — GT Small Ores — [GT6 小矿世界生成](../history/card-plans/prep/GT6小矿世界生成详细计划.md)

## frozen（13）

分母已冻。`paused` 的 PUV/OMEGA 六张代码已在 `src/main`，是 CC 扩展，不是原版高压线。地牢是结构载体，房间内容仍 blocked。

- `content/gt6-electric-tools` — GT6 Electric Tools（`workflow=paused`） — [GT6 电动工具详细计划](../history/card-plans/closed/GT6电动工具详细计划.md)
- `content/puv-omega-parts` — Compact parts, Quantum circuit, wires and transformers to OMEGA（`workflow=paused`）
- `energy/fusion-quantum` — Fusion execution, QUANTUM, and LU to QU（`workflow=paused`）
- `energy/large-heat-exchanger` — Large Heat Exchanger 17197（`workflow=paused`）
- `energy/quantum-massfab` — Matter Fabricator and Neutronium bootstrap（`workflow=paused`）
- `energy/small-gas-turbine` — Small Gas Turbine（`workflow=paused`） — [微型燃气涡轮详细计划](../history/card-plans/prep/微型燃气涡轮详细计划.md)
- `energy/steam-turbine` — Steam Turbines STEAM to RU（`workflow=paused`）
- `logistics/cover-net-r0` — Logistics cover network R0（`workflow=accepted`）
- `machines/large-autoclave` — Large Autoclave 17112（`workflow=paused`） — [大型高压釜详细计划](../history/card-plans/closed/大型高压釜详细计划.md)
- `machines/large-fermenter` — Large Fermenter 17113（`workflow=paused`） — [大型发酵器详细计划](../history/card-plans/closed/大型发酵器详细计划.md)
- `machines/puv-omega-matrix` — All machine variants through OMEGA（`workflow=paused`）
- `registry/tool-head-remainder` — Tool-head remainder identities（`workflow=accepted`）
- `worldgen/gt-dungeon` — GT Dungeon（`workflow=paused`） — [GT 地牢详细计划](../history/card-plans/prep/GT地牢详细计划.md)

## 关闭计划

已关卡的计划在 `docs/history/card-plans/closed/`。完成档不再使用 `player_complete`；runtime_ready 见上表。

权威与流程见 [capability-delivery-workflow.md](capability-delivery-workflow.md)
与 [unimplemented-gap.md](unimplemented-gap.md)。
跨能力 blocked 总账见 [blocked.md](blocked.md)。
关闭一张卡：`python tools/close_capability.py --capability <slug> --change-class major`。
试玩签收：`python tools/playtest.py record-accept --id <cycle> --signer <name> --i-playtested`。
