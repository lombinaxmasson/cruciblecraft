# 历史文档索引

这些文件是只读历史。Git 历史仍可追溯原文；工作树路径以本表为准。
机器校验不再消费这些 Markdown。路径映射见 [path-map.json](path-map.json)。

现行 unique-active、prep 与 `player_complete` 只写在
[project-status.md](../current/project-status.md)。

## 当前替代

| 历史入口 | 当前文件 | 状态 |
| --- | --- | --- |
| `CrucibleCraft-总体规划.md` | [docs/current/roadmap.md](../current/roadmap.md) | 现行 |
| （无历史入口；现行缺口总账） | [docs/current/unimplemented-gap.md](../current/unimplemented-gap.md) | 人读权威；机制卡 READY ≠ 已实现；Prep 文件 ≠ 待办 |
| `docs/CrucibleCraft-玩家指南.md` | [docs/current/player-guide.md](../current/player-guide.md) | 现行 |
| `.plans/` | [card-plans/active](card-plans/active/) | unique-active 计划 |
| （无历史入口） | [card-plans/prep](card-plans/prep/) | 已签发、不占落地锁的 prep 计划 |
| `plans/` | [card-plans/closed](card-plans/closed/) | 关闭计划 |
| numbered builders / currentness / sessions / DAG | [`tools/legacy_verification_index.json`](../../tools/legacy_verification_index.json) | 原字节只读；不参与 active verification |

## 已删除的非计划历史

工作日志、阶段档案、handoff 与 closed-plans/ 已从工作树删除。关闭计划正文只保留在 [card-plans/closed](card-plans/closed/)。原文仍可从 Git 历史读出。

## 决策与关闭计划

| 文档 | 类型 | 替代 |
| --- | --- | --- |
| [容器身份与边界 ADR](../decisions/容器身份与边界ADR.md) | ADR | 仍有效 |
| [材料身份分层混合 ADR](../decisions/材料身份分层混合ADR.md) | ADR | 现行目标；`registry/hybrid-material-identity` 已关 `runtime_ready` |
| [材料前缀组件身份 ADR](../decisions/材料前缀组件身份ADR.md) | ADR | 历史落地 / 事故态；不再是目标 |
| [表现层与可玩性分母](../decisions/CrucibleCraft-表现层与可玩性分母.md) | 设计提案 | 现行路线见 roadmap |
| [GT6U 搬运差距](../decisions/CrucibleCraft-GT6U搬运差距分析.md) | 来源分析 | 仍有效 |
| [关闭计划目录](card-plans/closed/) | 关闭计划 | 唯一保留的历史计划树 |
