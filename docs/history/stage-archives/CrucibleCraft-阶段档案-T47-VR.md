# CrucibleCraft 阶段档案 · T47-VR

> 状态：✅ `T47_VR_READY`（2026-08-31）
> 性质：T47 内部 closeout-verification repair gate；`owns_families=0`、`gap_delta=0`
> 不占用 T48，不重签 T46/T47 production lock，不改 395 / 13,708 / 283 或 gap 1,499

## 关闭结果

- 已关闭卡 `--check` 只比 `t{N}_closeout_seal.json` 字节/哈希与 N/L/R/gap
- T38–T47 census / topology / readiness 不再 `check_document(OUTPUT, build())` 走 live closeout
- T46/T47 GameTest receipt 去掉 live `identity_ledger_v2` / `runtime_manifest_v2` pin
- 历史 runtime dependency manifest `--check` 比对 snapshot 文件字节，不重算 listed-file live hashes
- `player_gametest_present()` 对已关闭卡读 seal PASS + receipt 字节
- `unique_active_card` 仍为内容卡字段；T47-VR 不占用；`next_issue_id` 保持 `T48`
- census `gradle_tasks: []`；`card-closeout` = recipes → census → closeout-seals
- census-replay 默认 hash/seal `--check` + 当前波 load；T37–T46 load 重跑要 `--full-replay`
- T46 integrated harness 切片抽出 `CompactRecipeRuntimeWaveSlice`；假 T48 group 后切片仍为 13
- T47 harness 断言 `groups.size() >= 13` 且含 `t47_bath_exact` / `t47_bath_exact_multi`，不钉死全局 15

冻结分母未变：T47 395/13,708/283、gap 1,499、R=0；T46 complete 803、opening remaining 1,894；composed groups 15；T47 GameTest 10/10。T48 未签发。

## 权威 artifacts

- `tools/t47_vr_pre_repair_freeze.json`
- `tools/t47_vr_repair_readiness.json`
- `tools/closeout_seal.py` / `tools/closeout_seal.schema.json`
- `tools/t38_closeout_seal.json` … `tools/t47_closeout_seal.json`
- `build/verification/t47-vr-census.json`（census；`gradle: null`）
- `build/verification/t47-vr-closeout.json`（recipes + census + closeout-seals）
