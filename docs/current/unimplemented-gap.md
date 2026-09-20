# 未实现与尾账索引

> 本页只指路，不手抄库存数字。过期长文已归档。
> 机器主键仍是 `tools/capabilities/**/capability.json` 与
> `tools/blockers/catalog.json`。[project-status.md](project-status.md)
> 与 [blocked.md](blocked.md) 是它们的投影，不要手改。
> unique-active、prep 文件列表与试玩 cycle 以状态页为准；
> blocker 排期以 catalog / blocked 为准。
> `*_READY` 只表示分母、来源或机制可跑，不表示游戏里已经有完整内容。
> Prep 计划文件存在 ≠ runtime 没做。R0 disposition 标签 ≠ 剩余 dummy。

## 读哪

| 用途 | 权威 |
| --- | --- |
| 当前 unique-active / prep / 试玩 cycle | [project-status.md](project-status.md) |
| 跨能力 blocked 总账 | [blocked.md](blocked.md) / [`catalog.json`](../../tools/blockers/catalog.json) / [`ledger.json`](../../tools/blockers/ledger.json) |
| 批处理关系 | [`batches.json`](../../tools/blockers/batches.json) |
| 状态与交付流程 | [capability-delivery-workflow.md](capability-delivery-workflow.md) |
| 材料形态排队 | [recipe-wave-workflow §1.1](recipe-wave-workflow.md) 与 prep [材料形态需求普查](../history/card-plans/prep/材料形态需求普查详细计划.md)；冻结核 [`census.json`](../../tools/waves/prep/material-form-demand-census/census.json) |
| 未认领 family | [`leftover_later.json`](../../tools/waves/portfolio/source-capability-map-r0/leftover_later.json) |
| MTE 身份分母（冻结快照） | [`disposition_ledger.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json) / [`family_map.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/family_map.json)；R0 计划 [MTE 身份分母处置 R0](../history/card-plans/closed/MTE身份分母处置R0详细计划.md) |
| 已关 MTE runtime / 折回 | [project-status.md](project-status.md) 的 `runtime_ready` 表与 `docs/history/card-plans/closed/` |
| 手持工具余量 | [blocked.md](blocked.md) `tools/world-behaviors`；对照本地 `gt6_code/gregtech6` 的 `Loader_Tools.java` |
| 严禁目录掉落物世界生成 | [gt6-no-item-entity-worldgen.md](gt6-no-item-entity-worldgen.md) |
| GT6 贴图 | [gt6-art-policy.md](gt6-art-policy.md) |

2026-09 人读长文（含过期 MTE 个数与仓储桶旧结论）已冻结在
[unimplemented-gap-2026-09.md](../history/card-plans/closed/archived-prep/unimplemented-gap-2026-09.md)。
同期从 prep 挪走的 12 张 MTE 家族合同在
[archived-prep](../history/card-plans/closed/archived-prep/MTE全量Prep总索引.md)。
那些文件的 runtime child 已经关 `runtime_ready`；不要把归档目录或 R0 标签抄成待办。

仍在 `card-plans/prep/` 的签发计划（普查、印刷机、地牢、行星岩、Center、PUV 线、微型燃气涡轮）只以状态页 Prep 表为准。
