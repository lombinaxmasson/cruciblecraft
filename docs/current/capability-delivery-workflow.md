# 能力交付流程

> 适用范围：对照图之后的机制、runtime 与内容工作。
> 现行流程。工作包可以更严，不得弱化本文件的底线。
> 配方域的 Source Pack / production lock / GameTest / load / census 仍遵守
> [recipe-wave-workflow.md](recipe-wave-workflow.md)；那是本流程的领域插件，不是全项目进度单位。

## 1. 进度单位

落地（unique-active）同时只允许一条 **active delivery lane**。
独立大卡可以走 **prep 车道**（§8）：签发计划与分支实施不占这条锁，
合入共享注册表时才晋升落地。工作包必须写清 owned paths。
这不是「无限 GT6」的许可证。

工作包（issue / 计划文档）只管理 WIP。它不进入：

- 文件名、runtime id、schema、Gradle 任务、closeout seal、`required_tokens`

持久主键是 **capability slug**，例如 `logistics/fluid-network/basic-transfer`。
工作包可以归档，**不生成** `*_READY`。

路线图与缺口账本只把下列两项计为完成进度：

1. 能力声明 `maturity = player_complete`，且当前 Git revision 上刚完成一次
   `player-complete` fresh PASS
2. 与工作包解耦的 `release` checkpoint

`frozen` 与 `runtime_ready` 是阶段，不是完成。

`capability.json` 不保存 `evidence=current`。运行结果只写到被 Git 忽略的
`build/verification/latest.json`；删除该文件不会改变能力声明，下一次验证仍会完整重跑。

## 2. 两个声明字段

权威：`tools/capabilities/<slug>/capability.json`，由
`python tools/build_capability_ledger.py --check` 汇总。
旧 `tools/waves/**/readiness.json` 只读适配，不得把 `*_READY` 读成玩家完成。

| 字段 | 取值 | 含义 |
| --- | --- | --- |
| `maturity` | `frozen` / `runtime_ready` / `player_complete` | 规格 / 机制可跑 / 玩家可完成 |
| `workflow` | `active` / `paused` / `accepted` | 这条 lane 是否还开着 |

禁止用一个 `*_READY` 同时表示声明状态和本次执行结果。

## 3. 分层硬门

### frozen

- 分母、来源、所有权、依赖完整
- 每条新身份有 disposition：`reuse_canonical` / `new_distinct` / `bridge` / `blocked`
- 不得叫 READY，不得扣 recipe gap
- 默认：还没有把机制做成可宣称的 `runtime_ready`。缺口第 2 节的「尚未签发」
  只适用于这种卡
- 例外：PUV / OMEGA 六张选择 `frozen`+`paused` 关卡——`src/main` 已有代码，
  但是 CC 扩展，不是原版 GT6 高压线。它们写在缺口第 1 节，禁止写回第 2 节
  「尚未签发」，也禁止晋级 `player_complete`

### runtime_ready

- `src/main` 机制可运行；内容可以尚未导入
- 全局 registry path 唯一；semantic key 无未声明冲突
- 本卡每条 `disposition=blocked` 与「Obtain stays explicitly_blocked」
  必须绑定 [blocked.md](blocked.md) / `tools/blockers/catalog.json` 的 `id`
- 声明的 JUnit / GameTest / reload / 网络契约通过
- impact graph 上所有受影响能力的 profile 在本次调用中 fresh PASS

### player_complete

必须同时具备：

- 生存获得路径（真实 RecipeManager，不是 GameTest 注入冒充）。
  **配方必须按 GT6 源逐格合成 / 制造才算过。** 允许把 `OP.wireGt01` /
  `MT.Os` 这类前缀与材料名翻译成 CC 里**已经存在的同一对象**
  （`wire`、`osmium_elemental`）。禁止用别的材料、别的形态、原版零件或
  单一 `programmed_circuit` 顶缺失格，来假装「能合成」。缺任何一格真实
  配料：该配方不算过，本能力不得晋级 `player_complete`；要么把缺件纳入
  本卡做出真物，要么保持 `runtime_ready` 并写明配方未完成。
  「创造栏能拿到」或「用已有材料 DESIGN_POLICY 生存获得」不能当完成。
  闭卡替身配方不是稳定面：真 GT6 零件一旦存在，下次碰到该格就换成真物，
  不必另开回收卡；不得再发明新替身去保住闭卡 `player_complete`。
  电池 `B`/`C` 槽与空芯灌液：铅酸 / 碱性 / 镍镉 / 锂钴 / 锂锰已是 filled
  cell + `OD_CIRCUITS[档]` + FluidContainerData。energium 获得路径是
  `form_items` 并到 LU 晶体（GT6 `setTarget`），不是 shaped 表；不得用板、
  尘、杆或单个电路伪造一张。
- EMI 注册与可见性（本仓库配方 UI 是 EMI，不是 JEI）
- 创造栏归属与 `en_us` / `zh_cn` 翻译
- 同一次 `player-complete` 调用实际运行隔离 GameTestServer。
  `runClient` 只在晋级（`runtime_ready → player_complete`）或显式
  `--client` 时强制。临时 receipt 位于 `build/verification/receipts/`，
  只供该次调用消费，不提交
- `required_test_ids` 固定本能力必须出现的 GameTest 方法名；receipt 的测试 ID
  集合必须与声明精确相等，删除或改名任一要求测试都会失败
- 本能力声明不触及客户端时可由
  `player-complete` profile 豁免 GUI 项（流体/物品盖板触及客户端，不豁免）
- 存档 / 重载
- `player_signoff.json` 人工签收：命名、排序、玩家能否看懂

只有这一档可写入路线图「已实现」。PR CI 只在 `runtime_ready → player_complete`
晋级时运行完整 player-complete；普通低风险改动不自动跑客户端。`release` 仍跑
全部 release profiles。

### release checkpoint

全 profile、真客户端、无受影响的 open verification debt。与单个工作包解耦。

## 4. 共享面与 impact

改管道、盖板、loader、codec、创造栏、EMI 插件时：

1. impact graph 按 owned_paths glob 选出受影响能力
2. 本次 verification run 必须 fresh 执行这些能力声明的 profiles
3. 只有当前 revision 的 PASS 可以用于晋级或发布
4. **不**重签、不读取历史 closeout seal 来替代本次执行
5. **不**把卡号写进运行时类型

## 5. 债务分类

| 类 | 放哪 | 挡晋级？ |
| --- | --- | --- |
| accepted divergence | 能力 `identity_disposition`（如 Low Heat Extruder Shape = `new_distinct`） | 否；必须显式 |
| blocked claim | [blocked.md](blocked.md)（catalog `id` 绑定 `identity_disposition` / obtain note） | 挡该能力的 `runtime_ready` / `player_complete` 关卡；不同 `unit` 不得相加 |
| verification debt | `tools/known_issues/verification-debt.json` | 挡**该 profile**；不挡范围外能力的范围内门 |
| deferred capability | [unimplemented-gap.md](unimplemented-gap.md) 第 1–2 节 | 不是 known-issue；不得写成 READY |

known-issues 不得再充当「未关门但下一张卡照开」的垃圾桶。

## 6. 日常命令

```powershell
python tools/build_capability_ledger.py --check
python tools/build_blockers.py --check
python tools/build_project_status.py --check
python tools/verify.py integration --profile capability-runtime
python tools/verify.py promotion
python tools/build_player_complete.py --run --all
python tools/build_player_complete.py --run --client --capability <slug>
python tools/close_capability.py --capability <slug>
python tools/verify.py integration --profile player-complete
```

`--run` 创建临时 receipt、运行 isolated GameTestServer、校验结构化结果，然后写
本地 latest report。`--run --client` 再加真实 `runClient`，只给晋级用。
单独的 `--check` 只做静态声明检查；它不能把提交库中的旧 receipt 当成玩家完成证明。
日常 `--check` 只要 `--gametest-receipt`；`--client-receipt` 可选。

`python tools/build_<slug>.py --check` 与机制卡 `*_READY` **不是** player_complete。

## 7. 贴图

新内容禁止占位图。有 GT6 原图就从本地 `gregtech6_w` 迁，合同见
[gt6-art-policy.md](gt6-art-policy.md)。Prep 分支上的本机贴图目录也遵守该合同；
禁止在别人的 unique-active owned paths 上顺手扩历史美术债。

## 8. Prep 车道

Prep 拆开原先捆在「一条 lane」里的三件事：人读 WIP、共享文件落地锁、
`player_complete` 晋级门。只降低第一项。晋级门与无 stand-in 获得格不降。

| 步 | 进 master？ | `unique_active_wave` | `capability.json` |
| --- | --- | --- | --- |
| 1. 签发计划 | 可以。只合计划文档 | 保持当前落地卡的值；本卡自己是 `null` | 不创建 |
| 2. 开工分支 | 不合实施。只在 `prep/<slug>` / 独立 worktree | 分支里的 wave 也必须是 `null` | 不要 `workflow=active` |
| 3. 晋升落地 | 当前 unique-active 已关才合共享文件 | 这时才改成本卡 slug | 这时才建，`workflow=active` |

计划放 [`docs/history/card-plans/prep/`](../history/card-plans/prep/)，**不要**放进
`card-plans/active/`。现行 unique-active 只写在
[project-status.md](project-status.md)，不准改成 prep 卡。
`card-plans/prep/` 与 `active/` 一样禁止里程碑卡号。

签发页眉必须有：

```text
lane                    = prep
capability_slug         = …
unique_active_wave      = null
prep_owned_paths        = 新 Java 包、本机贴图目录、tools/waves/prep/<slug>/**
landing_owned_paths     = ModBlocks / ModItems / ModBlockEntities / ModMenus /
                          ModRecipeMaps / ModProcessingMachines / ModCapabilities /
                          machine_kinds.json / machine_tiers.json /
                          machine_acquisition.json / machine_delivery.json /
                          EnergyType.java / src/recipe_generated/**
landing_depends_on      = 当前 unique-active 关闭（加工机还要第一张 live 小图已证明加入流程）
```

D0 获得格在签发当天写成 GT6→CC 或 `explicitly_blocked`。用
`python tools/gt6_resolve.py` 翻译；未映射不得换零件。

开工允许：未挂进 `ALL` 的 spec 源文件、本机贴图与 art manifest、
`tools/waves/prep/<slug>/` 下的 Source Pack、`import-source`、人工
`production_lock`、临时目录 isolated compile。
禁止：改 `landing_owned_paths`、改当前 unique-active 的 owned_paths、
把 live `src/recipe_generated` 写进 `import-source` 的 `output_paths`。

同时开工的 prep **实施**分支最多两张；计划可以多排队。
需要改 `EnergyType`、load 硬顶或与 unique-active 抢同一前缀文件的卡
（聚变、QU Energizer、massfab、Boxinator、Polarizer/磁选的 MU）**不要走 prep**。

`prep 做好了` 不是 `player_complete`。分支上 D0、未注册 spec、来源贴图、
isolated compile PASS、相对 master 不含 `landing_owned_paths` 即可停手等落地槽。

晋升：计划从 `prep/` 挪到 `active/`，创建 capability，一次挂 spec / sidecar /
RecipeMap / live compile，再跑内容卡 GameTest。`recipes` profile 不能当晋级。

签发不等于开工。本步只合计划文档；不创建 `capability.json`、`tools/waves/**`
或实施分支，也不改 `unique_active_wave`。
