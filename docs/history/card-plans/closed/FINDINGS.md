# CrucibleCraft full_verification_report 阻塞分析

分析基线：`tools/src 2026-08-08 13:55` 与 `2026-08-12 22:00` 两份存档，
还原为 `repo/tools` + `repo/src` 后跑完 66 条 builder 链并做写入探测。

---

## 1. 结论摘要

原判断「regen 改了源数据 → 下游 hash 全变 → 每个 builder 重建一次即可」
方向对了一半，但漏掉了两个使它**不可能靠重跑收敛**的因素：

1. **8 条回边**：早序号 builder 的 artifact 里钉死了晚序号 builder 才产出的
   文件 hash。正向单遍遍历在数学上无法收敛。
2. **1 处人工镜像漂移**：`gt6_ore_chain.json` 与
   `local_artifact_manifest.json` 必须一致，但后者**没有任何脚本会写**，
   且其唯一相关 writer（`compare_gt6_recipes`）在 `rebuild_artifacts.SKIP` 里。

另外发现 1 处**闭合阶段证据被批量刷新覆盖**（T14），需要回滚。

---

## 2. 阻塞点 A —— 人工镜像漂移（当前第一个真失败）

`build_gt6_ore_chain --check --reference-only` 比对：

| 位置 | 值 |
|---|---|
| `gt6_ore_chain.json → inputs.gt6_reference.sha256` | `9cc36733…`（新） |
| `local_artifact_manifest.json → tools/gt6_recipe_normalized_reference.json` | `84f90e5a…`（旧） |

08-08 存档里两者一致（均 `84f90e5a`）。本地 130 MB 的
`gt6_recipe_normalized_reference.json` 被重新生成，`--write` 忠实记下新 sha，
但 manifest 没跟。全仓搜索确认 `local_artifact_manifest.json` **零 writer**，
只有三处 reader。08-08→08-12 该文件唯一改动是新增 `gt6_code` 条目。

**语义零漂移**：`gt6_ore_chain.json` 的 `recipes`(2117)、`counts`、
`source_accounting`、`coverage_ledger` 逐字节相同，只有 `inputs` 变了。

> 附带风险：`inputs.normalizer.sha256` 钉的是 `compare_gt6_recipes.py` 自身
> 字节 hash。本次该文件唯一改动是一段 `MISSING → SKIP` 容错（不影响输出），
> 但仍使 `gt6_ore_chain.json` 作废，而重写它需要那 130 MB 缓存。

---

## 3. 阻塞点 B —— 回边导致不收敛

四个「stale」artifact 重写后**各自只差一条 input hash 指针，语义零变化**：

| artifact | 唯一变化叶子 |
|---|---|
| `t6_electrical_readiness.json` | `input_sha256[component_rule_manifest.json]` |
| `t8_pipe_readiness.json` | `input_sha256[t19_pipe_acquisition.json]` |
| `t10_preflight_projection.json` | `sources.material_tree_sha256` |
| `gt6_ore_chain_closure.json` | `inputs.material_registration_gate.sha256` |

跑完一整轮正向写入后重新 check，`build_t8_pipe_readiness` 与
`build_worldgen_catalog` **再次失败** —— 非收敛已实证。

### 已确认回边（`tools/check_builder_graph.py` 可复算）

| 读方 | artifact 钉住 | 写方 |
|---|---|---|
| #0 `import_gt6_oredict` | `gt6_ore_chain.json` / `gt6_ore_chain_operands.json` | #3 `build_gt6_ore_chain` |
| #0 `import_gt6_oredict` | `gt6_l1b_selected_recipe_operands.json` / `material_registration_gate.json` | #5 `build_gt6_material_form_gate` |
| #4 `build_gt6_ore_chain_closure` | `material_registration_gate.json` | #5 |
| #13 `build_t8_pipe_readiness` | `t19_pipe_acquisition.json` | #48 `build_t19_pipe_acquisition` |
| #15 `build_worldgen_catalog` | `t20_worldgen_expected.json` | #51 `build_t20_worldgen_projection` |
| #23 `build_t12_closure_readiness` | `processing_machine_energy_audit.json` | #24 |

### 策略外 writer（rebuild 永远跑不到）

| 被钉文件 | writer | 在 66 条策略里？ |
|---|---|:-:|
| `gt6_generation_bits.json` | `build_gt6_generation_bits.py` | ✗ |
| `material_registry_budget.json` | `run_material_registry_stress.py` | ✗ |
| `gt6_l1b_selected.json` | `gt6_l1b_layer_select.py` | ✗ |
| `gt6_recipe_normalized_reference.json` | `compare_gt6_recipes.py --write-reference` | 在策略但在 SKIP |

`tools/*.py` 共 24 个不在策略里。

---

## 4. 为什么报告一个字都出不来

`run_full_verification.run_builder_checks()` 严格 fail-fast：
`run_command` 一遇非零退出即 `raise RuntimeError`，session 在 builder 阶段中止，
datagen / Java / GameTest / Python 全不跑，`record_ready()` 到不了。
**任何一条 hash 指针错位 = 零报告**，后面有没有别的问题完全看不见。

---

## 5. 附带发现 —— T14 闭合证据被批量刷新覆盖（需回滚）

`build_t14_recipe_load_benchmark` 只有 `--check`，因此
`detect_write_argv` 判为「无 flag = write」，被 rebuild 扫入。其 write 模式
从 `build/t14-benchmark/raw_measurements.json` 重新派生**已关闭的 T14 决策**。

`t14_materialization_decision.json` 有 **1,819 个语义叶子**变化，
`t14_readiness.json` 有 42 个。三候选排名发生翻转：

| | 1st | 2nd | 3rd |
|---|---|---|---|
| 08-08 | on_demand 1.000 | **immediate 1.298** | **hybrid 1.654** |
| 08-12 | on_demand 1.000 | **hybrid 1.629** | **immediate 2.513** |

关键测量位移（20×）：server reload p95 56.0→80.4 ms、dedicated client
re-expansion 53.9→101.4 ms、enumeration 7.6→21.1 ms。

未突破 hard ceiling（故 `build_t14_readiness --check` 仍 PASS），但：

- 第三阶段 T14 卡明确写「误差带并列时按既定 preference 选 hybrid，
  **不存在事后改评分**」——排名被批量重写正是该条禁止的情形；
- `t14_load_budget_policy.json` 的 `source` 叙述仍引用旧值
  （53.864 ms / 32.2413 ms / 21934784 B / 1543 ns / 2950 ns），
  已与测量 artifact 脱节。

**建议**：把 `t14_materialization_decision.json`、`t14_readiness.json`
的测量段回滚到 08-08 值，并把该 builder 加入 `SKIP`。

---

## 6. 历史基线体检（好消息）

89 个变更的 `tools/*.json` 中，**74 个是纯 hash/时间戳 churn**，15 个含语义叶子。
逐份核对已关闭阶段：

| artifact | 差异叶子 / 总叶子 | 性质 |
|---|---|---|
| `t12a_machine_readiness.json` | 5 / 1264 | 全为 `source_hashes` |
| `t12_closure_readiness.json` | 6 / 134 | 全为 currentness 依赖 hash |
| `t20_worldgen_expected.json` | 1 / 4071 | `closure_ledger.sha256` |

**已关闭阶段的语义内容零漂移。** 唯一例外是上节的 T14。

`t16/t17/t18_readiness.json` 的 `full_suite_test_count` 85→88 属于既有
currentness 链设计（历史值 62→63→69→83→88），非违规，但值得确认。

---

## 7. 建议处理顺序

### 立即解封

1. 用本地实际文件重算，更新 `local_artifact_manifest.json` 中
   `tools/gt6_recipe_normalized_reference.json` 的 `sha256` 与 `bytes`
   （另两条 cache 条目一并核对）。**不修则 `build_gt6_ore_chain` 永远挂。**
2. 应用 `rebuild_artifacts.py` 定点迭代补丁（见 `rebuild_artifacts.py`）：
   循环至「本轮零文件改写」，上限 5 轮，超限把仍在变的文件当环报出。
   沙箱实测 **需要 4 轮**（改写 25 → 7 → 2 → 0）才收敛。
3. 把 `build_t14_recipe_load_benchmark` 加入 `SKIP`，并回滚 T14 测量段。
4. 前置跑三个策略外 writer（补丁中的 `PRE_PASS`）。

补丁应用后，先前振荡的 6 个 builder 全部 PASS：
`build_t6_electrical_readiness` / `build_t8_pipe_readiness` /
`build_t10_preflight_projection` / `build_gt6_ore_chain_closure` /
`build_worldgen_catalog` / `build_t12_closure_readiness`。

### 结构性修复（符合既有方法论）

回边应当消除，而不是靠多跑几轮盖住：

- **降级为诊断**：`t8_pipe_readiness` 钉 `t19_pipe_acquisition.json` 的 hash，
  等于让已关闭的 T8 证据依赖 T19 当前字节。把这类下游 hash 从 `input_sha256`
  移到不参与 currentness 门禁的 `observed_downstream` 块。
- **或反转所有权**：由 T19 证明「我与 T8 pipe 域一致」，而非 T8 钉 T19。
  `#0 ↔ #3/#5` 同理：import manifest 不该收录 ore_chain / form_gate 的产物 hash。
- **把 writer 声明进策略**：给 `verification_builder_policy.json` 每条加
  `outputs` 字段，检查器即可直接比对序号，不必静态推断。

### CI 门禁

把 `tools/check_builder_graph.py` 接入 `run_python_tests --suite closure`：
BACK_EDGE 与 MIRROR_DRIFT 硬失败，OFF_POLICY 告警。
以后不会再靠人肉一个一个撞出来。
