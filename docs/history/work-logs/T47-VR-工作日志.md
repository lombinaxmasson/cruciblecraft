# T47-VR 工作日志

## 2026-08-31 · 关闭验证单调性修复

### 边界

- 不撤销 `T47_READY`，不 `--approve-resign` T46/T47 lock
- 395/13,708/283、gap 1,499、T46 complete 803 / remaining 1,894、composed groups 15 冻结
- `owns_families=0`、`gap_delta=0`；T48 未签发
- Open debt 保持 open：`T32-VD-002`、`T32-VD-004`
- 不 commit（除非另行授权）

### R0–R5

- R0 freeze + repair readiness 状态机；分母换成 T46/T47
- R1 `closeout_seal` schema/module；T38–T47 sidecar；closed `--check` 离 live rebuild
- R2 收窄 T46/T47 `bound_gametest_artifacts()`；runtime manifest snapshot `--check`；`--write --from-log` 重绑收据
- R3 seal-backed `player_gametest_present()`；`unique_active_card` 内容卡 only
- R4 census 空 Gradle；census-replay 默认普通 `--check`；`CLOSEOUT_PROFILES = recipes, census, closeout-seals`
- R5 `CompactRecipeRuntimeWaveSlice` + T47 membership asserts

### 验证

- `python -m unittest discover -s tools/tests -p "test_build_t47_vr*.py"`
- `python -m unittest discover -s tools/tests -p "test_closeout_seal.py"`
- T46/T47 census / receipt unit tests
- `python tools/build_t{46,47}_{census_delta,card_topology,readiness}.py --check`
- `python tools/closeout_seal.py --check`
- `python tools/verify.py integration --profile census --report-all --json build/verification/t47-vr-census.json` → PASS，`gradle: null`
- `python tools/verify.py integration --profile closeout-seals --report-all --json build/verification/t47-vr-seals.json` → PASS
- `.\gradlew.bat test --tests com.masson.cruciblecraft.recipe.gt.CompactRecipeRuntimeWaveSliceTest --no-daemon` → BUILD SUCCESSFUL
- `.\gradlew.bat test --no-daemon --rerun-tasks --max-workers=1` → BUILD SUCCESSFUL，782 / 0（并行全量曾 OOM，串行重跑）
- `python tools/verify.py integration --profile card-closeout --report-all --json build/verification/t47-vr-closeout.json` → **PASS**（recipes + census + closeout-seals；Gradle 782 / 0；GameTest receipts PASS；约 16.4 min）
- 故障模拟（unit）：假 T48 composed group 后 T46 `complete_family_count` 保持 803；损坏 T46 seal fail closed；census `--check` 不调用 live `player_gametest_present` / `build()`；receipt 不再 live-pin v2。未改写 T46/T47 lock 或 gap。

T46 lock `454a49b25ff329dbb053b9195841fe9bb51dcbc311fd293f990b354218a762f4`  
T47 lock `884643b2ba569e4eb96c9fcfe63caeebc61be6bb2df3334f85d3cf13490614db`

### 结果

```text
T47_VR_READY
failed_gates = []
owns_families = 0
gap_delta = 0
T48 not issued
open debt: T32-VD-002, T32-VD-004
```
