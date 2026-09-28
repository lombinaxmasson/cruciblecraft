# 未实现与尾账索引

> 本页只指路，不手抄库存数字。过期长文已归档。
> 机器主键仍是 `tools/capabilities/**/capability.json` 与
> `tools/blockers/catalog.json`。[project-status.md](project-status.md)
> 与 [blocked.md](blocked.md) 是它们的投影，不要手改。
> 全量 GT6 源覆盖、RecipeMap 对账和四轴缺口见
> [gt6-full-coverage.md](gt6-full-coverage.md)；本页不再承担全量进度判断。
> unique-active 与 prep 文件列表以状态页为准；
> blocker 排期以 catalog / blocked 为准。
> `*_READY` 只表示分母、来源或机制可跑，不表示游戏里已经有完整内容。
> Prep 计划文件存在 ≠ runtime 没做。R0 disposition 标签 ≠ 剩余 dummy。

## 读哪

| 用途 | 权威 |
| --- | --- |
| 当前 unique-active / prep | [project-status.md](project-status.md) |
| 跨能力 blocked 总账 | [blocked.md](blocked.md) / [`catalog.json`](../../tools/blockers/catalog.json) / [`ledger.json`](../../tools/blockers/ledger.json) |
| 批处理关系 | [`batches.json`](../../tools/blockers/batches.json) |
| 状态与交付流程 | [capability-delivery-workflow.md](capability-delivery-workflow.md) |
| 材料形态排队 | [recipe-wave-workflow §1.1](recipe-wave-workflow.md) 与 prep [材料形态需求普查](../history/card-plans/prep/材料形态需求普查详细计划.md)；冻结核 [`census.json`](../../tools/waves/prep/material-form-demand-census/census.json) |
| 未认领 family | [`leftover_later.json`](../../tools/waves/portfolio/source-capability-map-r0/leftover_later.json) |
| MTE 身份分母（冻结快照） | [`disposition_ledger.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json) / [`family_map.json`](../../tools/waves/portfolio/mte-identity-disposition-r0/family_map.json)；R0 计划 [MTE 身份分母处置 R0](../history/card-plans/closed/MTE身份分母处置R0详细计划.md) |
| 已关 MTE runtime / 折回 | [project-status.md](project-status.md) 的 `runtime_ready` 表与 `docs/history/card-plans/closed/` |
| 手持工具余量 | [blocked.md](blocked.md) `tools/world-behaviors`；对照钉住的 GregTech 6 里 `gregtech/loaders/b/Loader_Tools.java` |
| 严禁目录掉落物世界生成 | [gt6-no-item-entity-worldgen.md](gt6-no-item-entity-worldgen.md) |
| GT6 贴图 | [gt6-art-policy.md](gt6-art-policy.md) |

旧的人读缺口快照已删除；MTE 家族合同仍在
[archived-prep](../history/card-plans/closed/archived-prep/MTE全量Prep总索引.md)，
但只能作为契约附件阅读，不能把归档目录或 R0 标签抄成待办。

仍在 `card-plans/prep/` 的签发计划（普查、印刷机、地牢、行星岩、Center、PUV 线）只以状态页 Prep 表为准。
