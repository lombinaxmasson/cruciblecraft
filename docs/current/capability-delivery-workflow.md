# 能力交付流程

> 适用范围：对照图之后的机制、runtime 与内容工作。
> 现行流程。工作包可以更严，不得弱化本文件的底线。
> 配方域的 Source Pack / production lock / GameTest / load 仍遵守
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

路线图与缺口账本把 **accepted `runtime_ready`** 当机制进度。玩家获得性是独立字段
`survival_access`（`unreviewed` / `blocked` / `partial` / `complete` /
`not_applicable`），**不挡** runtime 关闭，也不再有 `player_complete` 成熟度。
试玩是项目级 cycle：大改（新能力，或世界生成 / 玩法主循环 / 获得 / GUI /
存档 / 网络语义）打开或延长 pending；小改不作废已接受的 cycle。
签收只能来自人跑的 `runClient` 报告。CI 不得自动 `runClient` 当试玩。

`frozen` 是规格阶段。`runtime_ready` 是可关闭档。

`capability.json` 不保存 `evidence=current`。运行结果只写到被 Git 忽略的
`build/verification/latest.json`；删除该文件不会改变能力声明，下一次验证仍会完整重跑。

## 2. 声明字段

权威：`tools/capabilities/<slug>/capability.json`，由
`python tools/build_capability_ledger.py --check` 汇总。
旧 `tools/waves/**/readiness.json` 只读适配，不得把 `*_READY` 读成玩家完成。

| 字段 | 取值 | 含义 |
| --- | --- | --- |
| `maturity` | `frozen` / `runtime_ready` | 规格 / 机制可跑并可关卡 |
| `workflow` | `active` / `paused` / `accepted` | 这条 lane 是否还开着 |
| `survival_access` | `unreviewed` / `blocked` / `partial` / `complete` / `not_applicable` | 机制入口获得性；独立、非关卡门 |

禁止用一个 `*_READY` 同时表示声明状态和本次执行结果。
禁止 `maturity = player_complete`。历史 14 张已降为 `runtime_ready`，旧
`player_signoff.json` 布尔签收作废。

## 3. 分层硬门

### frozen

- 分母、来源、所有权、依赖完整
- 每条新身份有 disposition：`reuse_canonical` / `new_distinct` / `bridge` / `blocked`
- 不得叫 READY，不得扣 recipe gap
- 默认：还没有把机制做成可宣称的 `runtime_ready`。尚未签发的领域看
  [blocked.md](blocked.md) 与
  [`leftover_later.json`](../../tools/waves/portfolio/source-capability-map-r0/leftover_later.json)，
  不要从归档缺口长文倒推
- 例外：PUV / OMEGA 六张选择 `frozen`+`paused` 关卡——`src/main` 已有代码，
  但是 CC 扩展，不是原版 GT6 高压线。它们列在
  [project-status.md](project-status.md) 的 frozen 表，禁止写成「尚未签发」

### runtime_ready

- `src/main` 机制可运行；内容可以尚未导入
- 全局 registry path 唯一；semantic key 无未声明冲突
- 本卡每条 `disposition=blocked` 与「Obtain stays explicitly_blocked」
  必须绑定 [blocked.md](blocked.md) / `tools/blockers/catalog.json` 的 `id`
- 声明的 JUnit / GameTest / reload / 网络契约通过
- impact graph 上所有受影响能力的 profile 在本次调用中 fresh PASS
- `survival_access` 为 `blocked` / `partial` / `unreviewed` **不挡**关闭
- 新能力默认 `change-class=major`，打开或延长项目试玩 cycle

配方仍必须按 GT6 源逐格翻译，禁止 stand-in 配料。配方卡上缺格就保持缺口，不要用
别的材料、原版零件或 `programmed_circuit` 顶。那是安全阀，不是缺形态的工作顺序。
已经能对上 GT6 的 `(材料, 前缀)` 走
[材料形态需求普查](../history/card-plans/prep/材料形态需求普查详细计划.md)
（prep，不占落地锁），再开 bounded form-open unique-active。不要做到配方才补，
不要把 `blocked.md` 当形态排队，也不要按生成旗标全开长尾。
**严禁**把 catalog 物品以 `ItemEntity` 撒在主世界，或把 stone / block-object
目录倒在 dirt/sand 上，当成生存获得。不要写「用主世界掉落物顶 player_complete」。合同见
[gt6-no-item-entity-worldgen.md](gt6-no-item-entity-worldgen.md)。

### survival_access 与试玩 cycle

`survival_access` 只描述机制入口，不按 catalog 行关账。
`complete` 需要真实 RecipeManager 或 GT6 世界生成（石块/树/矿/地牢/作物），
不是创造栏、不是 GameTest 注入。

试玩权威是 `tools/playtest/current_cycle.json`：

- `major`：新能力，或大改世界生成 / 玩法主循环 / 获得 / GUI / 存档 / 网络
- `minor` / `none`：不使已接受的 cycle 作废
- 接受：人跑 `.\gradlew.bat runClient` 之后
  `python tools/playtest.py record-accept --id … --signer … --i-playtested`
- CI 与代理不得把 startup smoke / `--client` 当成试玩签收

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
| blocked claim | [blocked.md](blocked.md)（catalog `id` 绑定 `identity_disposition` / obtain note） | 挡该能力的 `runtime_ready` 关卡；不同 `unit` 不得相加 |
| verification debt | `tools/known_issues/verification-debt.json` | 挡**该 profile**；不挡范围外能力的范围内门 |
| deferred capability | [unimplemented-gap.md](unimplemented-gap.md)（只指路）与 [blocked.md](blocked.md) / [`leftover_later.json`](../../tools/waves/portfolio/source-capability-map-r0/leftover_later.json) | 不是 known-issue；不得写成 READY |

known-issues 不得再充当「未关门但下一张卡照开」的垃圾桶。

## 6. 日常命令

```powershell
python tools/build_capability_ledger.py --check
python tools/build_blockers.py --check
python tools/build_project_status.py --check
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --check
python tools/playtest.py check
python tools/verify.py integration --profile capability-runtime
python tools/close_capability.py --capability <slug> --change-class major
.\gradlew.bat runClient
```

`--run` 创建临时 receipt、运行 isolated GameTestServer、校验结构化结果，然后写
本地 latest report。`--run --client` 只是启动 smoke，**不是**试玩签收。
单独的 `--check` 只做静态声明检查。

`python tools/build_<slug>.py --check` 与机制卡 `*_READY` **不是**玩家试玩。

## 7. 贴图

新内容禁止占位图。有 GT6 原图就从本地 `gregtech6_w` 迁，合同见
[gt6-art-policy.md](gt6-art-policy.md)。Prep 分支上的本机贴图目录也遵守该合同；
禁止在别人的 unique-active owned paths 上顺手扩历史美术债。

## 8. Prep 车道

Prep 拆开原先捆在「一条 lane」里的三件事：人读 WIP、共享文件落地锁、
runtime 关闭与试玩 cycle。只降低第一项。无 stand-in 获得格不降。

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

材料形态普查是例外：计划在 `card-plans/prep/`，产物只写
`tools/waves/prep/material-form-demand-census/**`，仍不创建 capability、
不改 `unique_active_wave`、不写 `landing_owned_paths`。开门必须另占 unique-active。
