# 当前已知问题

机器可读台账：[tools/known_issues/verification-debt.json](../../tools/known_issues/verification-debt.json)

分类合同见 [capability-delivery-workflow.md](capability-delivery-workflow.md) §5。
本页不再把「未关门但下一张照开」和「有理由的 divergence」混在一段里。

## Accepted divergence

这些是显式身份决策，不是漏做的冲突。

- mapped tool head 已折回 `材料 × 前缀`（`TOOL_HEAD_PREFIX_READY`；bath identity `71`，semantic `244`；remap `7990/0`）。钥匙 / 电路线 / Low Heat Extruder Shape 仍是 unique identity：meta 对不上 `material_id_to_cc`。Low Heat 是温度档，不能并进 34 件 `ExtruderShapeCatalog`；disposition = `new_distinct`，权威在 `tools/capabilities/registry/tool-head-remainder/capability.json`。不开放「任意油」tag。
- 电池芯 `battery_part:filled_cell` 已注册 GT6 `IL.Battery_*_Cell_Empty` / `IL.Battery_*_Cell_Filled`（`20000–20009`）。五族空芯、EU 电池 `B`/`C` 槽和 FluidContainerData 灌液是 SOURCE_BACKED（不是 Canner 表）。高氯酸锂尘走电解 ordinary-closure `0051`。Energium 宝石前缀已 `form_items` 并到 LU 晶体 BlockItem（`gemChipped`=ULV … `gemLegendary`=IV），这就是 GT6 `setTarget`，不是欠一张 shaped 表。历史 stand-in 见 [电池详细计划](../history/card-plans/closed/电池详细计划.md)「配方保真债」。GT6 工作台小写工具格不是原版 `crafting_shaped`；走 `shaped_catalyst`（固定 3×3）。工具催化剂按 GT6 `getToolDamagePerContainerCraft() / 100` 扣耐久；电路等非工具催化剂原样返还。电池空芯、不锈钢/钨钢墙、铱线圈、LU 光纤共用此类型。
- 配方生成器 / 运行时曾把签发卡号当成类型，并在 Bath 上混用青铜化学信封与 GT6 remainder compact。待重构，见 [recipe-wave-workflow §4.3.1](recipe-wave-workflow.md)。现在不要为了改名去动已封板路径。

## Verification debt

这些行保持 open，不得标 PASS。它们挡所列 profile，不挡范围外能力的范围内门。

- compact family 作者正文已改为 `matrix_v1`（`COMPACT_RECIPE_AUTHORED_MATRIX_READY`），线上 `StreamCodec` v2 编矩阵而不是展开表。修的仍是写法，不是 Holder 粒度。compact snapshot `13845 != 14201` 仍是 scope-external 债（mortar / assembler fingerprints），未 `--update-baseline`。它不挡 `semantic-generators` 的 fresh 结构/字节比较。
- 隔离 census 的 `cover_behaviors` 冻结表仍是 `registerBuiltin` 那 8 个；物品/流体/通用网行为走 identity gate 精确比对 extras。物流九件盖板物品图标已从本地 `gregtech6_w` 迁入 `gt6_import/`（含 Dump）。传送带 / 检索器 / 机械臂等管网盖板仍用各自现有 item 贴图，未在本卡重核。
- 化学语义 artifact 与现行生成器存在结构漂移：留给后续配方工作
- 历史 READY 已退出 active verification；日常门不再消费历史收据
- 历史 full verification report 已从工作树删除，不是当前执行结果
- 能量链 GameTest 方法名已语义化；历史 `energy_chain_readiness*` token 若仍存在，以 [semantic-naming.md](semantic-naming.md) 为准。
- 物流 live `logistics` profile 不再重导历史 pipe/cover 信封。那些 builder 在 legacy index。cover definitions 语义集合仍由物品网 Python 测试覆盖。
- 历史 catalog/count/policy fixture 已按当前权威目录与 semantic ID map 对齐；日常硬门仍是 semantic-generators 的 builder `--check` 与 active Python suite。全量 `gradle test` 由 `runtime-java` 在 Java/测试/资源改动和 `release` 时跑，不是每次提交都跑。
- 配方加载卡已关：fluidbed 无效 JSON 不再进 RecipeManager；allocation 轴为 `PENDING_MEASUREMENT`。生产 reload 10 s 硬顶未降低。旧 20.7 s 日志不得当 PASS。
- 日常验证已卸掉工作流哈希全量扫描、里程碑 TXX `--quick`、文档 profile、overlay 漂移锁和 `build_semantic_recipes --check`。还没拆、回头另开验证卡再改的危险门见 [verification.md §还没拆的危险门](verification.md) 与 `verification-debt.json` 的 `VD-2026-09-*`。

## Deferred capability

游戏里还没有、以后另开能力，不要写进 known-issues 当「已关卡的尾巴」。总账：[unimplemented-gap.md](unimplemented-gap.md)。

- Display CPU 四件物流监视器已由 [显示 CPU](../history/card-plans/closed/显示CPU详细计划.md) `player_complete`（`logistics/display-cpu`）。Dump 封面与 Logistics Core 已 [物流核心](../history/card-plans/closed/物流核心详细计划.md) `player_complete`。`dump_policy` 见已关闭的 [物流封面网余量](../history/card-plans/closed/物流封面网余量详细计划.md)。
- 能量：人读 unique active 现为 [裂变观测安全与能源 Jade](../history/card-plans/active/裂变观测安全与能源Jade详细计划.md)（`energy/nuclear-fission-observation-safety`；实施未开始；堆芯安全 + 电池/转换机 Jade）。[配方 blocked 链账本与首条收口](../history/card-plans/closed/配方blocked链账本与首条收口详细计划.md) 已关（`recipe/blocked-chain-ledger`；fluidbed 49 `explicitly_blocked`，不接管能量 lane）。已关闭 [裂变热流体与热量合同](../history/card-plans/closed/裂变热流体与热量合同详细计划.md)（`energy/nuclear-fission-hot-fluids`，`player_complete` / `accepted`；11/9/8 热流体与热量合同）。已关闭 [裂变生存闭环与全量棒堆芯](../history/card-plans/closed/裂变生存闭环与全量棒堆芯详细计划.md)（`energy/nuclear-fission-survival`，`player_complete` / `accepted`；46 棒 / 8 kind / 2 堆芯 / 48 条关系）。本波 `nuclear_started = true`，`phase5` tracks.C `started = true`；sealed growth-order 仍 `nuclear_started = false`。已关闭 [变压器](../history/card-plans/closed/变压器详细计划.md)（`energy/transformers`，`player_complete` / `accepted`；9 台电变压器）。已关闭 [能源后续卡收口](../history/card-plans/closed/能源后续卡收口详细计划.md)（电转换 10 台、LU 光纤、裂变堆芯、聚变 18 条、电池芯；`runtime_ready`，未签新 capability）。已关闭 [电池](../history/card-plans/closed/电池详细计划.md)（`energy/batteries`，`player_complete`）。已关闭 [能量转换机目录](../history/card-plans/closed/能量转换机目录详细计划.md)（`energy/converter-catalog`）把 Burning Box / 锅炉 / 蒸汽机 / 燃油引擎 / 发电机 / 电机做成 kind × 材质分档（169 行 `player_complete`；活 JSON 179）。余量卡已删除 `firebox` / `bellows` / 独立 `coal_coke`。长距变压器与齿轮箱不在变压器卡。热交换、涡轮、冷却器和聚变仍未开。
- 手持工具余量（建筑杖/钳子/口袋切换、宝石镐与万能锹工作台、电动与枪、放大镜、斧整树/锤碎矿/扳手拆机）见 [unimplemented-gap.md §5](unimplemented-gap.md)，不要写进已关工具卡的尾巴
- 作物 / 树 / 原版熔炉替换余量仍 `frozen`
- 原版替换 MVP 只覆盖了纸 3→1；熔炉仍是原版 8 圆石
- 首小时 mortar / sifter / smelter / bath 已脱离 `metal_surface`。`smelter` 现为 `basicmachines/smelter` 立方机；工作态 `overlay_active` 未接 `LIT`

## 历史记录

- 早期 Electrolyzer 闭卡曾被门闸 overlay、跨卡摘要链与超大 JSON 拖住。现场工作日志已从工作树删除，工具链仍在 legacy index
- 历史 census/topology 曾因后波 composed v2 / profiles 变更把已关闭卡重算成 incomplete；现已从 active verification 彻底断开
- Java CRLF：工程卫生阶段已把当时的 31 个文件转为 LF
