# 能力交付流程

> 适用范围：对照图之后的机制、runtime 与内容工作。
> 现行流程。工作包可以更严，不得弱化本文件的底线。
> 配方域的 Source Pack / production lock / GameTest / load / census 仍遵守
> [recipe-wave-workflow.md](recipe-wave-workflow.md)；那是本流程的领域插件，不是全项目进度单位。

## 1. 进度单位

同时只允许一条 **active delivery lane**。工作包必须写清 owned paths。
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
- 不得叫 READY，不得扣 recipe gap，不得从缺口第 1 节删行

### runtime_ready

- `src/main` 机制可运行；内容可以尚未导入
- 全局 registry path 唯一；semantic key 无未声明冲突
- 声明的 JUnit / GameTest / reload / 网络契约通过
- impact graph 上所有受影响能力的 profile 在本次调用中 fresh PASS

### player_complete

必须同时具备：

- 生存获得路径（真实 RecipeManager，不是 GameTest 注入冒充）
- EMI 注册与可见性（本仓库配方 UI 是 EMI，不是 JEI）
- 创造栏归属与 `en_us` / `zh_cn` 翻译
- 同一次 `player-complete` 调用实际运行 GameTestServer 与 `runClient`；
  临时 receipt 位于 `build/verification/receipts/`，只供该次调用消费，不提交
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
| verification debt | `tools/known_issues/verification-debt.json` | 挡**该 profile**；不挡范围外能力的范围内门 |
| deferred capability | [unimplemented-gap.md](unimplemented-gap.md) 第 1–2 节 | 不是 known-issue；不得写成 READY |

known-issues 不得再充当「未关门但下一张卡照开」的垃圾桶。

## 6. 日常命令

```powershell
python tools/build_capability_ledger.py --check
python tools/build_registry_identity.py --check
python tools/verify.py integration --profile capability-runtime
python tools/verify.py promotion
python tools/build_player_complete.py --run --capability logistics/fluid-network/basic-transfer
python tools/verify.py integration --profile player-complete
```

`--run` 创建临时 receipt、依次运行 isolated GameTestServer 与真实 runClient、校验
结构化结果，然后写本地 latest report。单独的 `--check` 只做静态声明检查；它不能把
提交库中的旧 receipt 当成玩家完成证明。

`python tools/build_<slug>.py --check` 与机制卡 `*_READY` **不是** player_complete。
