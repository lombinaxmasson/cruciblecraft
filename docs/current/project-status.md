# 项目状态

> 本页由 `python tools/build_project_status.py --write` 从
> `tools/capabilities/**/capability.json` 与 `docs/history/card-plans/`
> 生成。不要手改。ledger.json 与本页都是可重建投影，不是权威。

## Unique active

无。`capability.json` 里没有 `workflow=active`。

## Prep（不占落地锁）

- `machines/printer` — [印刷机详细计划](../history/card-plans/prep/印刷机详细计划.md)

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

## runtime_ready（accepted，非玩家完成）（7）

RecipeMap / 主机已挂，获得格或配方仍 blocked。不得宣称 `player_complete`。

- `machines/injector` — Injector — [注射机详细计划](../history/card-plans/closed/注射机详细计划.md)
- `machines/laminator` — Laminator — [层压机详细计划](../history/card-plans/closed/层压机详细计划.md)
- `machines/loom` — Loom — [织机详细计划](../history/card-plans/closed/织机详细计划.md)
- `machines/melter` — Melter — [熔融机详细计划](../history/card-plans/closed/熔融机详细计划.md)
- `machines/nanofab` — Nanoscale Fabricator — [纳米加工机详细计划](../history/card-plans/closed/纳米加工机详细计划.md)
- `machines/pressure-washer` — Pressure Washer — [压力清洗机详细计划](../history/card-plans/closed/压力清洗机详细计划.md)
- `machines/slicer` — Slicer — [切片机详细计划](../history/card-plans/closed/切片机详细计划.md)

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
