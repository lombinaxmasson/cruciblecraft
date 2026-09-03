# 当前已知问题

机器可读台账：[tools/known_issues/verification-debt.json](../../tools/known_issues/verification-debt.json)

分类合同见 [capability-delivery-workflow.md](capability-delivery-workflow.md) §5。
本页不再把「未关门但下一张照开」和「有理由的 divergence」混在一段里。

## Accepted divergence

这些是显式身份决策，不是漏做的冲突。

- mapped tool head 已折回 `材料 × 前缀`（`TOOL_HEAD_PREFIX_READY`；bath identity `71`，semantic `244`；remap `7990/0`）。钥匙 / 电路线 / Low Heat Extruder Shape 仍是 unique identity：meta 对不上 `material_id_to_cc`。Low Heat 是温度档，不能并进 34 件 `ExtruderShapeCatalog`；disposition = `new_distinct`，权威在 `tools/capabilities/registry/tool-head-remainder/capability.json`。不开放「任意油」tag。
- 配方生成器 / 运行时曾把签发卡号当成类型，并在 Bath 上混用青铜化学信封与 GT6 remainder compact。待重构，见 [recipe-wave-workflow §4.3.1](recipe-wave-workflow.md)。现在不要为了改名去动已封板路径。

## Verification debt

这些行保持 open，不得标 PASS。它们挡所列 profile，不挡范围外能力的范围内门。

- compact family 作者正文已改为 `matrix_v1`（`COMPACT_RECIPE_AUTHORED_MATRIX_READY`），线上 `StreamCodec` v2 编矩阵而不是展开表。修的仍是写法，不是 Holder 粒度。compact snapshot `13845 != 14201` 仍是 scope-external 债（mortar / assembler fingerprints），未 `--update-baseline`。它不挡 `semantic-generators` 的 fresh 结构/字节比较。
- 物品网关卡后仍开着的证明债（不挡 `ITEM_NETWORK_CORE_READY`，也不是流体网 `player_complete` 的替身）：chunk unload GameTest 只调 `onChunkUnloaded()` / `onLoad()`，没有真卸 chunk；`/reload` 测的是 classpath 静态 catalog；load 轴只有 `load_axis.json` 软顶。T35 隔离 census 的 `cover_behaviors` 冻结表已扩到含物品网 + 流体网行为；extra 仍用 identity gate 精确比对。三件**物品**封面贴图暂借 conveyor / retriever / robot_arm。
- 化学语义 artifact 与现行生成器存在结构漂移：留给后续配方工作
- 历史 READY 已退出 active verification；日常门不再消费历史收据
- `full_verification_report.json` 是历史报告，不是当前执行结果
- GameTest T18 方法名已语义化，`t18_readiness*` token 仍钉旧名。语义命名总账见 [semantic-naming.md](semantic-naming.md)。
- 物流 live `logistics` profile 不再重导 T8/T19 历史信封。那些 builder 在 legacy index。T19 9 行 `cover_definitions.json` 语义集合仍由物品网 Python 测试覆盖。
- 历史 catalog/count/policy fixture 已按当前权威目录与 semantic ID map 对齐；日常硬门仍是 semantic-generators 的 builder `--check` 与 active Python suite。全量 `gradle test` 未作为每次提交的必跑门。

## Deferred capability

游戏里还没有、以后另开能力，不要写进 known-issues 当「已关卡的尾巴」。总账：[unimplemented-gap.md](unimplemented-gap.md)。

- Generic / Dump 封面网与 GT6 `MultiTileEntityLogisticsCore` 仍 `frozen`
- 作物 / 树 / 原版熔炉替换余量仍 `frozen`
- 原版替换 MVP 只覆盖了纸 3→1；熔炉仍是原版 8 圆石
- 首小时 mortar / sifter / smelter / bath 已脱离 `metal_surface`。`smelter` 现为 `basicmachines/smelter` 立方机；工作态 `overlay_active` 未接 `LIT`

## 历史记录

- 早期 Electrolyzer 闭卡曾被门闸 overlay、跨卡摘要链与超大 JSON 拖住：现场记录在 `docs/history/work-logs/`。这些工具已冻结为 legacy
- 历史 census/topology 曾因后波 composed v2 / profiles 变更把已关闭卡重算成 incomplete；现已从 active verification 彻底断开
- Java CRLF：工程卫生阶段已把当时的 31 个文件转为 LF
