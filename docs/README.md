# 文档索引

仓库根目录有两份 README：[中文](../README.md) · [English](../README.en.md)。
它们说明当前能力进度、验证入口和仓库结构。本页只做文档入口。

## 现行

- [总体规划](current/roadmap.md)
- [冻结与未实现账本](current/unimplemented-gap.md)（机制卡 `*_READY` ≠ 游戏里有；从这里找缺口，不要从阶段档案倒推）
- [能力交付流程](current/capability-delivery-workflow.md)（工作包 ≠ 进度；`player_complete` 才计入完成）
- [GT6 贴图纪律](current/gt6-art-policy.md)（禁止占位；从本地 `gregtech6_w` 迁入）
- [开发与验证指南](current/verification.md)
- [Ordinary recipe wave 与 Card / Group / Shard / Source Pack 规范](current/recipe-wave-workflow.md)
- [当前已知问题](current/known-issues.md)
- [语义命名长期清单](current/semantic-naming.md)（日常门是 `--quick`；计划文档豁免在 `card-plans/`）
- [语义命名收口执行清单](current/semantic-naming-closeout-checklist.md)
- [玩家指南](current/player-guide.md)
- [决策记录](decisions/)

## 历史

- [历史档案](history/INDEX.md)（关闭计划在 `card-plans/closed/`）
- 当前没有 unique active 内容卡。最近关闭：[生成资源、注册与 Jade 第一切片](history/card-plans/closed/生成资源注册与Jade第一切片详细计划.md)（slug `presentation/live-art-jade`；registry / generated-art / Jade 第一切片，`owns_families = 0`）。最近关闭：[配方加载与 EMI 稳定性](history/card-plans/closed/配方加载与EMI稳定性详细计划.md)（slug `runtime/recipe-load-emi-stability`；loader / EMI / reload repair，`owns_families = 0`）。最近关闭：[能源后续卡收口](history/card-plans/closed/能源后续卡收口详细计划.md)（电转换 / LU / 裂变 / 聚变 / 电池芯；`runtime_ready`）。最近关闭：[变压器](history/card-plans/closed/变压器详细计划.md)（capability `energy/transformers`；9 台电变压器 `10040–10048`，`player_complete` / `accepted`）。最近关闭：[电池](history/card-plans/closed/电池详细计划.md)（capability `energy/batteries`；census 37 个储能块，`player_complete`）。最近关闭：[能量转换机目录](history/card-plans/closed/能量转换机目录详细计划.md)（capability `energy/converter-catalog`；GT6 转换机 kind × 材质 169 行，`player_complete`；活 JSON 179）。最近关闭：[能量系统余量](history/card-plans/closed/能量系统余量详细计划.md)（删火箱/风箱/独立焦炭）。最近关闭：[显示 CPU](history/card-plans/closed/显示CPU详细计划.md)（四件状态盖板；capability `logistics/display-cpu`，`player_complete`）。现行 `player_complete` 能力：`logistics/fluid-network/basic-transfer`、`logistics/item-network-core`、`logistics/generic-network/core`、`logistics/logistics-core`、`logistics/display-cpu`、`energy/converter-catalog`、`energy/batteries`、`energy/transformers`。最近关闭的能力卡：[物流核心](history/card-plans/closed/物流核心详细计划.md)（Dump + 5×5×5 `player_complete`）。最近关闭的余量卡：[物流封面网余量](history/card-plans/closed/物流封面网余量详细计划.md)（钉死 `dump_policy`，不写 Java）。最近关闭的 Generic 卡：[通用网络核心](history/card-plans/closed/通用网络核心详细计划.md)（Generic 仓储/导入/导出 `player_complete`）。最近关闭的能力晋级卡：[物品网络核心玩家完成晋级](history/card-plans/closed/物品网络核心玩家完成晋级详细计划.md)（物品两行 `player_complete`）。最近关闭的 runtime 机制卡：[物品网络核心](history/card-plans/closed/物品网络核心详细计划.md)（slug `runtime/item-network-core`；`ITEM_NETWORK_CORE_READY`）
- 上一张关闭的注册卡：[工具头前缀折回](history/card-plans/closed/工具头前缀折回详细计划.md)（slug `registry/tool-head-prefix`）
- 上一张关闭的 runtime 卡：[紧凑配方作者矩阵](history/card-plans/closed/紧凑配方作者矩阵详细计划.md)（slug `runtime/compact-recipe-authored-matrix`）
- 上一张关闭的 runtime 卡：[紧凑配方传输编解码](history/card-plans/closed/紧凑配方传输编解码详细计划.md)（slug `runtime/compact-recipe-wire-codec`）
- 最近关闭的内容卡：[原版替换 MVP](history/card-plans/closed/原版替换MVP详细计划.md)
- 最近关闭的表现卡：[首小时表现与阶段账本](history/card-plans/closed/首小时表现与阶段账本详细计划.md)
- 最近关闭的机制卡：[作物食物蜜蜂 R0](history/card-plans/closed/作物食物蜜蜂R0详细计划.md)
- 上一张关闭的机制卡：[原版替换 R0](history/card-plans/closed/原版替换R0详细计划.md)
- 再上一张关闭的机制卡：[非矿世界生成 R0](history/card-plans/closed/非矿世界生成R0详细计划.md)
- 更早关闭的机制卡：[T13c 排除表收回 R0](history/card-plans/closed/T13c排除表收回R0详细计划.md)
- 更早关闭的机制卡：[物流封面网络 R0](history/card-plans/closed/物流封面网络R0详细计划.md)
- 最近关闭的机制 program：[通用 Source Pack 导入器](history/card-plans/closed/通用Source-Pack导入器详细计划.md)
