# 项目状态

> 本页由 `python tools/build_project_status.py --write` 从
> `tools/capabilities/**/capability.json` 与 `docs/history/card-plans/`
> 生成。不要手改。ledger.json 与本页都是可重建投影，不是权威。

## Unique active

`content/gt6-foundry-art`（GT6 Foundry Art，`workflow=active`，`maturity=runtime_ready`）；计划 [GT6 铸造美术详细计划](../history/card-plans/active/GT6铸造美术详细计划.md)。

## Prep（不占落地锁）

计划文件存在就会列在这里。MTE 家族 prep 的 runtime child 已关时，不要把本表当剩余待办。读法见 [unimplemented-gap.md](unimplemented-gap.md)。

- `content/mte-connector` — [MTE 连接件详细计划](../history/card-plans/prep/MTE连接件详细计划.md)
- `content/mte-decorative` — [MTE 装饰件详细计划](../history/card-plans/prep/MTE装饰件详细计划.md)
- `content/mte-drive` — [MTE 传动件详细计划](../history/card-plans/prep/MTE传动件详细计划.md)
- `content/mte-energy-converter` — [MTE 能源转换器详细计划](../history/card-plans/prep/MTE能源转换器详细计划.md)
- `content/mte-extender` — [MTE Extender详细计划](../history/card-plans/prep/MTE扩展器详细计划.md)
- `content/mte-fluid-attachments` — [MTE 流体附件详细计划](../history/card-plans/prep/MTE流体附件详细计划.md)
- `content/mte-furniture-storage` — [MTE 家具储物详细计划](../history/card-plans/prep/MTE家具储物详细计划.md)
- `content/mte-misc-tool` — [MTE 杂项工具详细计划](../history/card-plans/prep/MTE杂项工具详细计划.md)
- `content/mte-multiblock` — [MTE 多方块设备详细计划](../history/card-plans/prep/MTE多方块设备详细计划.md)
- `content/mte-prep-index` — [MTE 全量 Prep 总索引](../history/card-plans/prep/MTE全量Prep总索引.md)
- `content/mte-processing-machine` — [MTE 加工机身份详细计划](../history/card-plans/prep/MTE加工机身份详细计划.md)
- `content/mte-untyped` — [MTE 未分类余量详细计划](../history/card-plans/prep/MTE未分类余量详细计划.md)
- `content/puv-omega-tech-line` — [PUV2+ / OMEGA 科技线](../history/card-plans/prep/PUV2OMEGA科技线详细计划.md)
- `machines/printer` — [印刷机详细计划](../history/card-plans/prep/印刷机详细计划.md)
- `worldgen/gt-center` — [GT Center 详细计划](../history/card-plans/prep/GT中枢详细计划.md)
- `worldgen/gt-dungeon` — [GT 地牢详细计划](../history/card-plans/prep/GT地牢详细计划.md)
- `worldgen/gt-planet-rocks` — [GT 行星岩详细计划](../history/card-plans/prep/GT行星岩详细计划.md)

## Playtest cycle

`2026-09-15-obtain-reset` **pending**. Major worldgen / gameplay / obtain / GUI / save / network changes open or extend this cycle. Ordinary bugfix does not invalidate it. Accept only after a human `runClient`; CI never auto-signs.

Retired catalog scatter obtain and abolished player_complete. First real project playtest is required.; closed content/gt6-crucible-mold-interaction

## runtime_ready accepted（70）

机制可跑。`survival_access` 独立、不挡关闭。不得再用 catalog scatter 或历史 `player_complete` 签收当获得证明。

- `content/electric-wire-cable-mte-fold` — Electric Wire/Cable MTE Fold — [导线电缆 MTE 折回详细计划](../history/card-plans/closed/导线电缆MTE折回详细计划.md)
- `content/gt6-connector-alias-repair` — GT6 Connector Alias Repair — [GT6 连接件身份漏匹配详细计划](../history/card-plans/closed/GT6连接件身份漏匹配详细计划.md)
- `content/gt6-connector-art` — GT6 Connector Art — [GT6 连接件美术详细计划](../history/card-plans/closed/GT6连接件美术详细计划.md)
- `content/gt6-crucible-mold-behavior-correction` — GT6 Crucible Mold Behavior Correction — [GT6 坩埚模具行为校正详细计划](../history/card-plans/closed/GT6坩埚模具行为校正详细计划.md)
- `content/gt6-crucible-mold-interaction` — GT6 Crucible Mold Interaction — [GT6 坩埚模具交互详细计划](../history/card-plans/closed/GT6坩埚模具交互详细计划.md)
- `content/gt6-eu-cable-acquisition` — GT6 EU Cable Acquisition — [GT6 EU 线缆获得格详细计划](../history/card-plans/closed/GT6EU线缆获得格详细计划.md)
- `content/gt6-eu-missing-wire-gauges-runtime` — GT6 Missing EU Wire Gauges Runtime — [GT6 缺线规运行时详细计划](../history/card-plans/closed/GT6缺线规运行时详细计划.md)
- `content/gt6-eu-wire-cable-runtime` — GT6 EU Wire/Cable Runtime — [GT6 导线电缆运行时详细计划](../history/card-plans/closed/GT6导线电缆运行时详细计划.md)
- `content/gt6-fluid-combo-pipe-runtime` — GT6 Fluid Combo Pipe Runtime — [GT6 流体组合管运行时详细计划](../history/card-plans/closed/GT6流体组合管运行时详细计划.md)
- `content/gt6-fluid-dangerous-media-runtime` — GT6 Fluid Dangerous Media Runtime — [GT6 流体危险介质运行时详细计划](../history/card-plans/closed/GT6流体危险介质运行时详细计划.md)
- `content/gt6-fluid-pipe-acquisition` — GT6 Fluid Pipe Acquisition — [GT6 流体管获得格详细计划](../history/card-plans/closed/GT6流体管获得格详细计划.md)
- `content/gt6-fluid-pipe-runtime` — GT6 Fluid Pipe Runtime — [GT6 流体管运行时详细计划](../history/card-plans/closed/GT6流体管运行时详细计划.md)
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
- `content/mte-redstone-wire` — MTE Redstone Wire — [MTE 红石线详细计划](../history/card-plans/closed/MTE红石线详细计划.md)
- `content/sensors` — Sensors — [Sensors 详细计划](../history/card-plans/closed/Sensors详细计划.md)
- `content/technological-parts-foundation` — Technological Parts Foundation — [技术中间件基础详细计划](../history/card-plans/closed/技术中间件基础详细计划.md)
- `energy/batteries` — Energy Batteries
- `energy/converter-catalog` — Energy Converter Catalog
- `energy/heat-exchangers` — Heat Exchangers — [热交换器第一切片详细计划](../history/card-plans/closed/热交换器第一切片详细计划.md)
- `energy/nuclear-fission-hot-fluids` — Nuclear Fission Hot Fluids — [裂变热流体与热量合同详细计划](../history/card-plans/closed/裂变热流体与热量合同详细计划.md)
- `energy/nuclear-fission-observation-safety` — Nuclear Fission Observation Safety — [裂变观测安全与能源 Jade 详细计划](../history/card-plans/closed/裂变观测安全与能源Jade详细计划.md)
- `energy/nuclear-fission-survival` — Nuclear Fission Survival — [裂变生存闭环与全量棒堆芯详细计划](../history/card-plans/closed/裂变生存闭环与全量棒堆芯详细计划.md)
- `energy/transformers` — Energy Transformers
- `localization/language-key-display-name-normalization` — Language Key and Display Name Normalization — [语言键与显示名规范收口详细计划](../history/card-plans/closed/语言键与显示名规范收口详细计划.md)
- `logistics/cover-remainder` — Cover remainder — [盖板余量详细计划](../history/card-plans/closed/盖板余量详细计划.md)
- `logistics/display-cpu` — Logistics Display CPU
- `logistics/fluid-network/basic-transfer` — Fluid network basic transfer
- `logistics/generic-network/core` — Generic network core
- `logistics/item-network-core` — Item network core
- `logistics/logistics-core` — Logistics Core
- `machines/cluster-mill` — Cluster Mill — [集群轧机详细计划](../history/card-plans/closed/集群轧机详细计划.md)
- `machines/injector` — Injector — [注射机详细计划](../history/card-plans/closed/注射机详细计划.md)
- `machines/laminator` — Laminator — [层压机详细计划](../history/card-plans/closed/层压机详细计划.md)
- `machines/loom` — Loom — [织机详细计划](../history/card-plans/closed/织机详细计划.md)
- `machines/melter` — Melter — [熔融机详细计划](../history/card-plans/closed/熔融机详细计划.md)
- `machines/nanofab` — Nanoscale Fabricator — [纳米加工机详细计划](../history/card-plans/closed/纳米加工机详细计划.md)
- `machines/oven` — Oven — [熔炉详细计划](../history/card-plans/closed/熔炉详细计划.md)
- `machines/pressure-washer` — Pressure Washer — [压力清洗机详细计划](../history/card-plans/closed/压力清洗机详细计划.md)
- `machines/roll-former` — Roll Former — [辊压成型机详细计划](../history/card-plans/closed/辊压成型机详细计划.md)
- `machines/sanding` — Sanding Machine — [打磨机详细计划](../history/card-plans/closed/打磨机详细计划.md)
- `machines/slicer` — Slicer — [切片机详细计划](../history/card-plans/closed/切片机详细计划.md)
- `registry/catalog-modern-ids` — Catalog modern IDs — [目录身份现代 id 详细计划](../history/card-plans/closed/目录身份现代id详细计划.md)
- `registry/tool-head-prefix-reclaim` — Sharpener overflow reclaim — [工具头前缀与打磨机余量回收详细计划](../history/card-plans/closed/工具头前缀与打磨机余量回收详细计划.md)
- `worldgen/gt-crops` — GT Crops — [GT 作物世界生成](../history/card-plans/closed/GT作物世界生成详细计划.md)
- `worldgen/gt-trees` — GT Trees — [GT 树详细计划](../history/card-plans/closed/GT树详细计划.md)

## frozen（9）

分母已冻。`paused` 的 PUV/OMEGA 六张代码已在 `src/main`，是 CC 扩展，不是原版高压线。地牢是结构载体，房间内容仍 blocked。

- `content/puv-omega-parts` — Compact parts, Quantum circuit, wires and transformers to OMEGA（`workflow=paused`）
- `energy/fusion-quantum` — Fusion execution, QUANTUM, and LU to QU（`workflow=paused`）
- `energy/large-heat-exchanger` — Large Heat Exchanger 17197（`workflow=paused`）
- `energy/quantum-massfab` — Matter Fabricator and Neutronium bootstrap（`workflow=paused`）
- `energy/steam-turbine` — Steam Turbines STEAM to RU（`workflow=paused`）
- `logistics/cover-net-r0` — Logistics cover network R0（`workflow=accepted`）
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
