# CrucibleCraft 阶段档案 · T25

> 阶段：T25 · 实测阻断修复
> 状态：✅ `T25_READY`
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 权威产物：`tools/t25_readiness.json` · disposition 台账：`tools/t25_findings_disposition.json`

## 1. 关闭判据

T25 卡规定："只修复 T24 以可复现证据证明会阻断公开 Beta 的热点；若没有
blocker，则以 selected = 0 且全部 disposition 完整关闭，不进行预防性重写。"

T24 的 finding 台账共 5 条，`blocking = 0`。执行规则第 1 条限定输入只能
来自 T24 ledger，第 2 条禁止把 slow_but_non_blocking、测量噪声和跨机器
不可比数据升级为 selected——5 条 finding 没有任何一条构成 Beta 阻断，
因此 **T25 以 selected = 0 零内容关闭**：零修复、零新增注册对象、
publication delta = 0 / 0 / 0。

## 2. T25a · T24 finding 全部落 disposition

`tools/build_t25_findings_disposition.py` 从 T24 ledger 派生 disposition
台账（双射校验，多/少/重复即 fail-closed）：

| id | disposition | 要点 |
|---|---|---|
| T24-F001 | non_blocking | 管道相位 500→100/每 tick 已验证，无动作 |
| T24-F002 | non_blocking | 多方块校验 2 ops/位置已验证，无动作 |
| T24-F003 | non_blocking | route 上限声明未规模实测（slow_but_non_blocking，不升级）；recheck 契约落账 |
| T24-F004 | non_blocking | cover 负载 13 bytes 已验证，无动作 |
| T24-F005 | non_blocking | target/stress 墙钟/内存/网络显式 SKIP；recheck 契约落账 |

- `blocks_beta == true` 的 finding 必须 disposition == `selected_fixed`
  （builder fail-closed：真正的 blocker 不能被静默丢弃）；
- F003 / F005 从 `t24_scale_evidence.json` 的 `measured_at_scale` 段逐字
  落账 `replacement_condition`（≥16 GB 声明环境重测）与
  `recheck_point = "T26 Beta candidate"`；
- counts：total 5 / selected_fixed 0 / non_blocking 5 /
  invalid_measurement 0 / post_1_0 0。

## 3. T25b · 零内容三轴收尾

- **零内容门禁**（"新增注册对象为 0" 的机器断言）：新报告 vs T24 冻结
  常量 —— GameTest == 119、JUnit == 560、datapack entries == 3243、
  publication delta 0/0/0；任何内容/测试漂移都会让 readiness gate
  直接失败；
- Fidelity：`fixes_applied = 0`，source-backed 输入输出/时长/能源/机会/
  顺序未变，无近似或降级策略引入（显式声明，非伪装等价）；
- Load：`before_after_pairs = 0`、`remeasurement =
  NOT_APPLICABLE_NO_SELECTED_FIXES`；T24 的两条 SKIP 契约完整转交
  T26；无新增跨机器硬秒数 CI。

## 4. 三轴账

**Closure** — 5/5 finding 落 disposition（双射、词汇合法），selected = 0，
零内容关闭（卡文允许）；pending = 0。

**Fidelity** — 零修复即零保真风险；无近似策略；无未声明变更。

**Load** — publication delta = 0 / 0 / 0；基线一致；headroom 2,118；
前后测量对 = 0（无 selected 即无复测义务）；墙钟类无新增 CI。

## 5. 验证与交接

关闭绑定：

```text
python tools/rebuild_artifacts.py --verify
python tools/run_python_tests.py --suite closure
.\gradlew.bat test
.\gradlew.bat runGameTestServer
python tools/run_full_verification.py --record --new-session
python tools/run_full_verification.py --check-ready
```

`T25_READY` 绑定一次完整验证会话：报告 `status = READY` 且
`t25_readiness_acceptance.status = "T25_READY"`。

当前唯一 active T 是 T26（前置工程完成，等试玩）。

**交给 T26**：

- **T25 blocker = 0**（T26 Load 轴的硬性要求已满足）；
- T24 target workload 需在 Beta candidate 上复跑（T26 Load 轴），
  F003/F005 的 `replacement_condition`（≥16 GB 声明环境）与
  `recheck_point = "T26 Beta candidate"` 见
  `tools/t25_findings_disposition.json` 的 `recheck_contract` 段；
- disposition 台账是 T26 的 known-issue 输入：全部 5 条 non_blocking，
  每条含 reason、owner、证据指针与复现命令。
