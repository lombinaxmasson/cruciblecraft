# CrucibleCraft 项目交接说明（2026-08-18）

> 收件人：原作者 · 交接时状态：**T20–T28 `_READY`；T28 已关闭；下一张唯一 active T 是 T29（尚未开工）**

## 一、怎么接手

当前工作树在分支 **`master`**。最新已提交点：

`4c8fcea Snapshot T18-T26 in-progress work before importing 4.5 polish from the other machine.`

之上有大量**未提交**改动：4.5 P1–P9、T26–T28 绑定与 pin-chain。`4.5Fix/` 是另一台机器的对照目录，**不要提交**。

## 二、进度速览

| 卡 | 状态 |
|---|---|
| T20–T26 | `_READY`，阶段档案在仓库根目录 |
| T27 | **已关闭**：portfolio freeze；opening **19087 / 16862 / 2225**；delta 0/0/0；拓扑 2 张（T28=O-36，T29=crucible），`started=false` |
| T28 | **已关闭**：`--check-ready` 退出 0；报告 `READY`；`t28_readiness_acceptance.status = T28_READY`；O-36 replacement = 无被动转换 |
| T29 | **下一张唯一 active T**。尚未写 Java / JSON / builder。入口：《[T29详细计划.md](../card-plans/closed/T29详细计划.md)》 |

试玩包仍是 `cruciblecraft-0.1.0-beta.1.jar`。T28 细节：《[T28-工作日志.md](T28-工作日志.md)》·《[CrucibleCraft-阶段档案-T28.md](CrucibleCraft-阶段档案-T28.md)》。

### T28 绑定证据

- `--record --new-session` `20260818T203334.712120Z-9e88ae92c5dd-d9395b36`；随后 `--check-ready` 退出 0
- 报告 SHA-256 `4d2b5ac0a85f7ca2e65b7c52e7ff64ee36b5aed12b9409b03f08cee24e672878`
- JUnit **584**；GameTest **121/121**；Python **776**；datapack report **3,243**
- cooling expansion **0**；live publication **18766 / 16541 / 2225**；delta **−321 / −321 / 0**
- 产品决策 `strict_no_conversion`；freezer 仍 post_1_0；`large_crucible` 未注册

### 当前卡（T29）

开卡前必须再跑一次 `$env:PYTHONUTF8=1; python tools/run_full_verification.py --check-ready`，并同时看到 `READY` 与 `T28_READY`。
T29 opening 用 T28 live **18766 / 16541 / 2225**，不要写回 19,087。
完成 T29 ≠ 1.0 发货。不启动 Track A–E。

禁止用 `--write-tooling-snapshot` 强行写 acceptance。
`compare_gt6_recipes.py --update-baseline` 会走 full replay 并把 mortar UNREVIEWED 整表打红。

## 三、接手必改项（重要）

- Windows GBK 控制台：所有 Python 验证命令前缀 **`$env:PYTHONUTF8=1`**。
- 部分大 JSON 的 `Path.write_text(..., newline="\n")` 会 **`OSError 22`**。已对若干 builder 改成 `write_bytes` 或 `.tmp` + `replace`。
- 全量 `python tools/rebuild_artifacts.py --verify` 在本机曾被判过宽；按 `verification_builder_policy.json` 拓扑逐个 `--write` 更稳。**不要**整份重写 `t12a_machine_readiness.json`。
- T27 opening 的 **19087 / 16862 / 2225** 与 JUnit 切片 **582** 是冻结口径，不要用 T28 实测去改。
- T11 compact datapack **3243** 与 T14 `logical_recipes == 18875` 仍分轴。
- T28 published runtime 只含稳定字段（cooling / GameTest / report / T27 status）。不要把 `elapsed_seconds` 打进 acceptance 全等。

## 四、T28 交给 T29 的冻结项

- O-36 replacement 已由 `T28_READY` 独立证明；T27 freeze 里 O-36 仍是 `v1_required`，不要回写 portfolio。
- 拓扑 `started=false`、`card_count=2` 仍是 T27 冻结快照。T29 开工不要改 T28 卡的 `started`。
- F003/F005 仍在 T27 RC 合同；≥16 GB 复测不在 T29 范围内。
- 4.5 不插卡。`4.5Fix/` 不提交。

## 五、根目录杂项

- 根目录 `*.7z`、`4.5Fix/`、`build/`、`run/` 未跟踪或不应进提交。
- `gt6_dump/gt6_recipe_dump/` 在本机可用（full-replay 需要），被 gitignore。
