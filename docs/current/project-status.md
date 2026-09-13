# 项目状态

> 本页由 `python tools/build_project_status.py --write` 从
> `tools/capabilities/**/capability.json` 与 `docs/history/card-plans/`
> 生成。不要手改。ledger.json 与本页都是可重建投影，不是权威。

## Unique active

无。`capability.json` 里没有 `workflow=active`。

## Prep（不占落地锁）

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
- `machines/printer` — [印刷机详细计划](../history/card-plans/prep/印刷机详细计划.md)
- `worldgen/gt-center` — [GT Center 详细计划](../history/card-plans/prep/GT中枢详细计划.md)
- `worldgen/gt-dungeon` — [GT 地牢详细计划](../history/card-plans/prep/GT地牢详细计划.md)
- `worldgen/gt-planet-rocks` — [GT 行星岩详细计划](../history/card-plans/prep/GT行星岩详细计划.md)

## player_complete（14）

只计 `maturity=player_complete` 且 `workflow=accepted`。
声明不是证明；晋级仍要 fresh GameTest，`runClient` 只在晋级时强制。

- `energy/batteries` — Energy Batteries
- `energy/converter-catalog` — Energy Converter Catalog
- `energy/heat-exchangers` — Heat Exchangers — [热交换器第一切片详细计划](../history/card-plans/closed/热交换器第一切片详细计划.md)
- `energy/nuclear-fission-hot-fluids` — Nuclear Fission Hot Fluids — [裂变热流体与热量合同详细计划](../history/card-plans/closed/裂变热流体与热量合同详细计划.md)
- `energy/nuclear-fission-observation-safety` — Nuclear Fission Observation Safety — [裂变观测安全与能源 Jade 详细计划](../history/card-plans/closed/裂变观测安全与能源Jade详细计划.md)
- `energy/nuclear-fission-survival` — Nuclear Fission Survival — [裂变生存闭环与全量棒堆芯详细计划](../history/card-plans/closed/裂变生存闭环与全量棒堆芯详细计划.md)
- `energy/transformers` — Energy Transformers
- `logistics/display-cpu` — Logistics Display CPU
- `logistics/fluid-network/basic-transfer` — Fluid network basic transfer
- `logistics/generic-network/core` — Generic network core
- `logistics/item-network-core` — Item network core
- `logistics/logistics-core` — Logistics Core
- `machines/cluster-mill` — Cluster Mill — [集群轧机详细计划](../history/card-plans/closed/集群轧机详细计划.md)
- `machines/roll-former` — Roll Former — [辊压成型机详细计划](../history/card-plans/closed/辊压成型机详细计划.md)

## runtime_ready（accepted，非玩家完成）（22）

RecipeMap / 主机已挂，获得格或配方仍 blocked。不得宣称 `player_complete`。

- `content/electric-wire-cable-mte-fold` — Electric Wire/Cable MTE Fold — [导线电缆 MTE 折回详细计划](../history/card-plans/closed/导线电缆MTE折回详细计划.md)
- `content/gt6-connector-art` — GT6 Connector Art — [GT6 连接件美术详细计划](../history/card-plans/closed/GT6连接件美术详细计划.md)
- `content/gt6-eu-wire-cable-runtime` — GT6 EU Wire/Cable Runtime — [GT6 导线电缆运行时详细计划](../history/card-plans/closed/GT6导线电缆运行时详细计划.md)
- `content/gt6-fluid-pipe-runtime` — GT6 Fluid Pipe Runtime — [GT6 流体管运行时详细计划](../history/card-plans/closed/GT6流体管运行时详细计划.md)
- `content/gt6-item-pipe-runtime` — GT6 Item Pipe Runtime — [GT6 物品管运行时详细计划](../history/card-plans/closed/GT6物品管运行时详细计划.md)
- `content/gt6-redstone-wire-correction` — GT6 Redstone Wire Correction — [GT6 红石线行为校正详细计划](../history/card-plans/closed/GT6红石线行为校正详细计划.md)
- `content/mte-redstone-wire` — MTE Redstone Wire — [MTE 红石线详细计划](../history/card-plans/closed/MTE红石线详细计划.md)
- `content/sensors` — Sensors — [Sensors 详细计划](../history/card-plans/closed/Sensors详细计划.md)
- `content/technological-parts-foundation` — Technological Parts Foundation — [技术中间件基础详细计划](../history/card-plans/closed/技术中间件基础详细计划.md)
- `logistics/cover-remainder` — Cover remainder — [盖板余量详细计划](../history/card-plans/closed/盖板余量详细计划.md)
- `machines/injector` — Injector — [注射机详细计划](../history/card-plans/closed/注射机详细计划.md)
- `machines/laminator` — Laminator — [层压机详细计划](../history/card-plans/closed/层压机详细计划.md)
- `machines/loom` — Loom — [织机详细计划](../history/card-plans/closed/织机详细计划.md)
- `machines/melter` — Melter — [熔融机详细计划](../history/card-plans/closed/熔融机详细计划.md)
- `machines/nanofab` — Nanoscale Fabricator — [纳米加工机详细计划](../history/card-plans/closed/纳米加工机详细计划.md)
- `machines/oven` — Oven — [熔炉详细计划](../history/card-plans/closed/熔炉详细计划.md)
- `machines/pressure-washer` — Pressure Washer — [压力清洗机详细计划](../history/card-plans/closed/压力清洗机详细计划.md)
- `machines/sanding` — Sanding Machine — [打磨机详细计划](../history/card-plans/closed/打磨机详细计划.md)
- `machines/slicer` — Slicer — [切片机详细计划](../history/card-plans/closed/切片机详细计划.md)
- `registry/catalog-modern-ids` — Catalog modern IDs — [目录身份现代 id 详细计划](../history/card-plans/closed/目录身份现代id详细计划.md)
- `registry/tool-head-prefix-reclaim` — Sharpener overflow reclaim — [工具头前缀与打磨机余量回收详细计划](../history/card-plans/closed/工具头前缀与打磨机余量回收详细计划.md)
- `worldgen/gt-trees` — GT Trees — [GT 树详细计划](../history/card-plans/closed/GT树详细计划.md)

## 关闭计划（有 capability 的 player_complete）

- [热交换器第一切片详细计划](../history/card-plans/closed/热交换器第一切片详细计划.md)（`energy/heat-exchangers`）
- [裂变热流体与热量合同详细计划](../history/card-plans/closed/裂变热流体与热量合同详细计划.md)（`energy/nuclear-fission-hot-fluids`）
- [裂变观测安全与能源 Jade 详细计划](../history/card-plans/closed/裂变观测安全与能源Jade详细计划.md)（`energy/nuclear-fission-observation-safety`）
- [裂变生存闭环与全量棒堆芯详细计划](../history/card-plans/closed/裂变生存闭环与全量棒堆芯详细计划.md)（`energy/nuclear-fission-survival`）
- [集群轧机详细计划](../history/card-plans/closed/集群轧机详细计划.md)（`machines/cluster-mill`）
- [辊压成型机详细计划](../history/card-plans/closed/辊压成型机详细计划.md)（`machines/roll-former`）

权威与流程见 [capability-delivery-workflow.md](capability-delivery-workflow.md)
与 [unimplemented-gap.md](unimplemented-gap.md)。
关闭一张卡：`python tools/close_capability.py --capability <slug>`。
