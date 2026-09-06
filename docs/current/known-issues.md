# 当前已知问题

机器可读台账：[tools/known_issues/verification-debt.json](../../tools/known_issues/verification-debt.json)

分类合同见 [capability-delivery-workflow.md](capability-delivery-workflow.md) §5。
本页不再把「未关门但下一张照开」和「有理由的 divergence」混在一段里。

## Accepted divergence

这些是显式身份决策，不是漏做的冲突。

- mapped tool head 已折回 `材料 × 前缀`（`TOOL_HEAD_PREFIX_READY`；bath identity `71`，semantic `244`；remap `7990/0`）。钥匙 / 电路线 / Low Heat Extruder Shape 仍是 unique identity：meta 对不上 `material_id_to_cc`。Low Heat 是温度档，不能并进 34 件 `ExtruderShapeCatalog`；disposition = `new_distinct`，权威在 `tools/capabilities/registry/tool-head-remainder/capability.json`。不开放「任意油」tag。
- 电池芯 `battery_part:filled_cell` 已注册 GT6 `IL.Battery_*_Cell_Empty` / `IL.Battery_*_Cell_Filled`（`20000–20009`）。铅酸 / 碱性 / 镍镉空芯配方和电池 `B` 槽是 SOURCE_BACKED；锂钴 / 锂锰空芯配方仍缺 `OD_CIRCUITS[4]`/`[6]`。灌液走精确流体量，没有 Canner 灌液表。`recipe:energium_crystal_shaped` 仍 `blocked`（GT6 LU 晶体无 shaped 表）。历史 stand-in 见 [电池详细计划](../history/card-plans/closed/电池详细计划.md)「配方保真债」。GT6 工作台小写工具格不是原版 `crafting_shaped`；走 `shaped_catalyst`（固定 3×3，`catalysts` 不消耗）。电池空芯、不锈钢/钨钢墙、铱线圈、LU 光纤共用此类型。
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

## Deferred capability

游戏里还没有、以后另开能力，不要写进 known-issues 当「已关卡的尾巴」。总账：[unimplemented-gap.md](unimplemented-gap.md)。

- Display CPU 四件物流监视器已由 [显示 CPU](../history/card-plans/closed/显示CPU详细计划.md) `player_complete`（`logistics/display-cpu`）。Dump 封面与 Logistics Core 已 [物流核心](../history/card-plans/closed/物流核心详细计划.md) `player_complete`。`dump_policy` 见已关闭的 [物流封面网余量](../history/card-plans/closed/物流封面网余量详细计划.md)。
- 能量：unique active 为空。已关闭 [变压器](../history/card-plans/closed/变压器详细计划.md)（`energy/transformers`，`player_complete` / `accepted`；9 台电变压器）。已关闭 [能源后续卡收口](../history/card-plans/closed/能源后续卡收口详细计划.md)（电转换 10 台、LU 光纤、裂变堆芯、聚变 18 条、电池芯；`runtime_ready`，未签新 capability）。已关闭 [电池](../history/card-plans/closed/电池详细计划.md)（`energy/batteries`，`player_complete`）。已关闭 [能量转换机目录](../history/card-plans/closed/能量转换机目录详细计划.md)（`energy/converter-catalog`）把 Burning Box / 锅炉 / 蒸汽机 / 燃油引擎 / 发电机 / 电机做成 kind × 材质分档（169 行 `player_complete`；活 JSON 179）。余量卡已删除 `firebox` / `bellows` / 独立 `coal_coke`。长距变压器与齿轮箱不在变压器卡。裂变/聚变生存配方与 `nuclear_started` 仍未开。
- 作物 / 树 / 原版熔炉替换余量仍 `frozen`
- 原版替换 MVP 只覆盖了纸 3→1；熔炉仍是原版 8 圆石
- 首小时 mortar / sifter / smelter / bath 已脱离 `metal_surface`。`smelter` 现为 `basicmachines/smelter` 立方机；工作态 `overlay_active` 未接 `LIT`

## 历史记录

- 早期 Electrolyzer 闭卡曾被门闸 overlay、跨卡摘要链与超大 JSON 拖住。现场工作日志已从工作树删除，工具链仍在 legacy index
- 历史 census/topology 曾因后波 composed v2 / profiles 变更把已关闭卡重算成 incomplete；现已从 active verification 彻底断开
- Java CRLF：工程卫生阶段已把当时的 31 个文件转为 LF
