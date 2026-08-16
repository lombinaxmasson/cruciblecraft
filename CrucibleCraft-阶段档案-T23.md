# CrucibleCraft 阶段档案 · T23

> 阶段：T23 · 有界多方块批次（30 kind 行为分类 + 3 个代表结构实现）
> 状态：✅ 已关闭（`T23_READY` 由单次 clean record 的严格门禁派生，2026-08-14）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 权威产物：`tools/t23_readiness.json`；分支 `t22-5`
> 性质：第四阶段第一张真正新增玩家可见内容的卡。publication delta：
> GT 行 0，vanilla datapack 条目 +3，RecipeMap 32 不变，EMI 24 不变。

## 1. 开工门禁（G1）

- 基线冻结（T22.5 收盘）：publication 18,882 logical / 16,657 eager /
  2,225 lazy；RecipeMap 32；EMI 24；GameTest 89；Java 539；Python 636；
  builders 74；multiblock 结构 2。`t23_publication_baseline.json` 建立
  （`baseline: "T22.5"`），GameTest `registeredPublicationDelta` 基线
  列表同步加入。
- **1,002 行口径矛盾解释**（T22.5 B1/C1）：C1 的 1,002 行可解锁全部重新
  切出 B1 的 `cross_mod_compat` 类（out_of_scope 贡献 0 行）；四列算术
  `out_of_scope = 5,860 − 1,002 = 4,858` 是列运算恒等而非行源归属，列标签
  与行源不一致。数字不变，解释落纸于 O-41 登记行。
- **2 个 v1 blocker 处置 = 显式 deferred**（用户拍板）：
  `anvil_bend_big` / `anvil_bend_small`（registered_zero_logical），
  owner = T26 gate，deadline = T26 freeze。零代码改动。
- **O-41 登记**：1,002 行翻译层接线（T22.5 A2 映射表 + A3 物品等价），
  owner = post-1.0 portfolio，recheck point = T27 freeze。
- G2 readiness 骨架先行：`build_t23_readiness.py` +
  `t23_readiness_policy.json` + verify 门禁键一次建好；手写 `T23_READY`
  判 stale 验收实测通过。

## 2. A1 · 30 kind 行为需求分类

`tools/t23_multiblock_behavior_classification.json`（policy →
`build_t23_multiblock_classification.py` 派生），30/30 五组事实，
unclassified = 0，publication delta = 0：

| 分类 | 数量 | 内容 |
|---|---|---|
| v1_required | 6 | centrifuge✓ / coke_oven✓（已实现）+ distillation_tower / large_boiler / tank_3x3x3 / crucible |
| post_1_0 | 19 | 单块机器已覆盖或主链未闭合（bath/crusher/oven/电气化家族等） |
| out_of_scope | 5 | fusion_reactor / lightning_rod / bedrock_drill / matter_fabricator / von_da_graagg |

分类依据一律引用类别定义（v1_required = 已闭合主链的 GT6 标准多方块代表），
非难度。t13 分母沿链更新：third_stage_deferred 5 → 1 → 0（A2 后），
out_of_scope 5，post_t19 22 → 20，selected_t23 3。

## 3. A2 · selected 决议（A1 之后，commit 顺序可查）

**distillation_tower + large_boiler + tank_3x3x3**，四标准逐条落纸：
① 服务已闭合主链（石油 T22 / 蒸汽 T18 / 流体储运缺口）；② 输入可获取、
产品有真实消费端（fuel/lubricant → fuels_engine；steam → steam_engine）；
③ 全部 schema v1 可表达（81 / 36 / 26 位，均低于 125 位 aisle/repeat
触发线）；④ 行为家族互异（processing_host / conversion / storage）。
未选中的 v1_required（crucible）记 reason + recheck point（下一批，
T26 freeze 前）。

## 4. B1/B2 · 零扩展 schema + plugin 白名单

- **B1 = 零扩展证据卡**：三结构全部固定几何、显式 offset 可表达；
  `schema_version=2` 拒绝变异测试 + 旧结构零改动回归（JUnit）。
- **B2**：`MultiblockControllerPluginRegistry`（未知 id fail closed，
  重复注册拒绝）+ `PluginQuarantinePolicy`（镜像 MachineIdentityPolicy：
  空白首存采纳，不匹配保持原值可恢复）+ 落盘镜像
  `multiblock_plugins.json`（双射测试）。5 个 plugin 各 ≥1 真实消费者：
  `processing_host`（centrifuge+tower）、`shared_port_supply`（T15e 规则）、
  `heat_energy_input`（boiler：端口能量身份 = HU 非 KU 默认）、
  `steam_conversion`（SteamConversion 常量）、`storage_host`（tank）。
  端口桥接经 `MultiblockPortHost` 泛化（处理型/转换型/存储型三宿主）。
  GameTest：plugin id 篡改 → 隔离 + 内容保持 + 修复可恢复（89 → 90）。

## 5. C1–C3 · 三结构实现与生命周期

每结构独立 fidelity 证据（逐位对 GT6 checkStructure2 双向等价 + GameTest
断言），一个结构不代表其余 29 个：

| 结构 | 几何 | 宿主 | 获取配方（主资源手写） |
|---|---|---|---|
| distillation_tower | 81 位（9 HU 能源端口 + 71 物品流体端口 + controller） | HEAT-BUFFERED 处理宿主，distillery map（DESIGN_POLICY：RM 合并） | 8×端口 + invar_distillery |
| large_boiler | 36 位（9 能源 + 25 物品流体 + 1 空气 + controller 在壳内） | 转换宿主（80 HU + 1 水 → 160 蒸汽，SteamConversion） | 端口环 + bronze_boiler |
| tank_3x3x3 | 27 位（25 双向端口 + 1 空气 + controller 在壳上） | 存储宿主（256,000 mB，无能量无配方；容量 DESIGN_POLICY） | 端口环 + portable_fluid_tank |

- **C2 获取双路径证据**：`t21_operand_reachability` 闭包 tank 链全可达
  （bucket/chest/hopper 补入 vanilla seeds，修复端口配方的历史假阴性）；
  塔/锅炉唯一闭包外 operands = 合金机器核心，其可生存获取由 T16
  acquisition 闭包（`machine_acquisition_unreachable == 0`）证明。
- **C3 六生命周期 ×3 结构 = 18 个 GameTest**（formation / teardown /
  output jam / power loss / reload / save quarantine），GameTest
  89 → 114。
- GameTest 空模板高度 8 → 12（9 层塔放不下）。

## 6. D1 · Load 轴（MEASURED 最坏情况）

`MultiblockLoadBoundTest`（JUnit + 假 StructureAccess + 调用计数器）：
4,096 位最坏结构 = 8,192 次访问器调用（每元素 isLoaded+blockState 各 1），
墙钟仅记录不设门禁。`t23_load_evidence.json` 落盘：每结构
validation_ops_max（2×位置数）、port_scan_ops_max（2×端口数，代码派生 +
供给 GameTest 实测）、tick 摊还（validation/20）、菜单同步 3 个
ContainerData ints。`build_t23_load_bounds.py` 算术校验后 status MEASURED。

## 7. D2/D3 · 重建、修复与收盘

- D2：runData + gradle test 550/550 + GameTest 114/114 + 沿链重建定点。
  发现并修复三处卡级缺陷：塔主机 sidedIo 误带 ADJACENT 蒸馏器能量策略
  （九底座供热被拒）；锅炉 JSON 重复 controller 格 [0,0,0]；teardown 测试
  误破空气格。
- **D3 的 record 修复**（本阶段单独一课，`plans/T23-record-修复方案.md`）：
  根因 = `build_t23_readiness` 的 runtime 取自 committed report，而 report
  派生字段又参与 `check()` 过期比对，T23 与报告互相咬死。修复三补丁：
  ① 去掉写死的 `> 89` 阈值（数量正确性由 `record_gametest` 与 READY
  分支把关）；② `REPORT_OWNED`（currentness/runtime/status/completed/
  pending）退出过期比对，`derived_t23_readiness_acceptance` 同步剥离；
  ③ T23 门禁恢复严格 status 断言（首次 record 即完整，无放宽）。顺带：
  (a) 第四阶段总体规划路径统一到仓库根（`build_t22_5_readiness` +
  policy 两处），BETA 契约缺失改显式报错，根/plans 副本第 31 行措辞
  同步；(b) `record_builder` 的 23 个恒真 `*_result` 键改为从真实
  per-builder 行派生 `builder_results`（77 键），READY 分支改
  `REQUIRED_READINESS_BUILDERS` 循环（t55 删除、t8/worldgen_catalog 纳入、
  t22/t22_5 补键），t16/t17 policy 的 source token 同步。
- **单次 clean record（严格门禁）**：builder 44.5s / datagen 144.6s /
  java 102.1s / gametest 271.5s / python 137.1s 五阶段一次全 PASS；
  `T23_READY` 由 session 派生，`required_tests = 114` 自检通过；
  builders 74 → 77；fast 套件 589/589。

## 8. 顺带发现（已登记，未处理）

1. `t21_operand_reachability` 不建模坩埚合金（铜+锡→青铜等），合金机器
   核心在闭包中读作不可达——由 T16 acquisition 闭包交叉证明；工具文档
   approximations 已含"fluids not modelled"，合金路径建议后续补入。
2. `plans/` 与仓库根的文档副本机制容易漂移（本次已通过 (a) 修复路径引用，
   副本同步仍是人工约定）。
3. 候选报告机制（`CRUCIBLECRAFT_FULL_VERIFICATION_REPORT` env + datagen/
   gametest 证据盖章）为本次 record 循环中补齐——后续阶段改 datagen 树时
   直接受益，无需再走放宽路线。

## 9. 交接

- 权威产物：`t23_readiness.json`（`T23_READY`）、
  `t23_multiblock_behavior_classification.json`、`t23_plugin_whitelist.json`、
  `t23_load_bounds.json`、`t23_load_evidence.json`、
  `t23_publication_baseline.json`（registered_deltas：3 个 vanilla 条目）、
  三份 `multiblock_structures/*.json`。
- 关闭时下一张卡为 **T24**；T24–T25 已随后关闭。当前唯一 active T 是 T26。
  T23 selected 结构仍是 T24 规模场景与 T26a 首玩节点的输入。
- 遗留 open item：O-41（T27 freeze 复核）；2 个 bend map blocker
  （T26 freeze 拍板）；crucible 未选中（下一批多方块选择，T26 freeze 前）。
