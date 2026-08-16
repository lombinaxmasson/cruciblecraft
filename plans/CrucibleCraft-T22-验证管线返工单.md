# T22 验证管线返工单

> 范围：只处理 `run_full_verification.py --record` 跑不通的问题。
> T22 的内容侧（2 行 v1_required、跨模组行移出分母）方向正确，本单不动。
>
> **先读 Part 0 再动手。** 这次的诊断和之前那份 AI 报告结论不同，
> 按报告改会掩盖真问题。

---

# Part 0 · 先纠正三个判断

## 0.1 「套娃」不存在，manifest 是自洽的

拿到 `gt6_l1b_selected_recipe_operands.json` 装进仓库后，我用 import 自己的
`stable_hash()` 逐个校验了 manifest 里全部 18 个 hash：

```
MISSING  : []
MISMATCH : 0

python tools/import_gt6_oredict.py --check --reference-only
→ EXIT=0
```

**`import_gt6_oredict` 一次通过，没有任何 hash 漂移。**

之前那份报告说的"后面的 builder 改了 `gt6_l1b_selected_recipe_operands.json`
导致 import 的 manifest 对不上"——实测不成立。

## 0.2 `acceptance_form_corrections.json` 的路径判定是**对的**，别改

```python
ACCEPTANCE_FORM_CORRECTIONS = tools/component_rule_sources/acceptance_form_corrections.json
```

`validate_manifest_artifact_hashes()` 里那个三元表达式的作用，是把 manifest 的
**扁平键名**映射到**实际路径**。`material_registration_gate.json` 走同一套特判，
它的真实位置在 `src/main/resources/data/cruciblecraft/`。

文件一直在 `component_rule_sources/` 下，与代码期望一致。**这不是 bug，不要动。**

## 0.3 ❌ 不要加 `--rebuild-first`

那份报告建议在 builder check 之前先跑一次 rebuild。**这条否决。**

`--record` 的意义是"在当前提交状态上独立复核"。先 rebuild 再 check，
check 永远通过——测的不再是"仓库自洽"，而是"rebuild 跑完了"。

这与 T21 那次改 datagen 哈希是同一类动作，只是包装成流程改进。
`rebuild_artifacts.py` 的 docstring 第一句就写着：**这是刷新工具，不是验证工具。**

报告里的第二个方案（用 Python API 做 in-memory check）更不行——
会绕过子进程隔离，让 builder 共享内存状态，`--record` 的审计价值归零。

---

# Part 1 · 真正的失败点

我逐个跑了前六个 builder：

| builder | 结果 |
|---|---|
| `import_gt6_oredict` | ✅ PASS |
| `build_component_rules` | ✅ PASS（48 sources → 8,141 expansions） |
| **`build_gt6_ore_chain`** | ❌ `tools/gt6_ore_chain.json: GT6 reference hash` |
| `build_gt6_ore_chain_closure` | ✅ PASS |
| `build_gt6_material_form_gate` | ❌ 缺 `gt6_dump/...`（我这边没有 dump，你那边应正常） |

## 唯一的真实漂移

`build_gt6_ore_chain.py:1298-1313` 比对的是两个**已提交 JSON** 里的值，
和本地有没有那个 130 MB 文件无关：

```python
manifest = load(LOCAL_ARTIFACT_MANIFEST)
reference_metadata = next(row for row in manifest["artifacts"]
                          if row["path"] == "tools/gt6_recipe_normalized_reference.json")
if index["inputs"]["gt6_reference"]["sha256"] != reference_metadata["sha256"]:
    errors.append("tools/gt6_ore_chain.json: GT6 reference hash")
```

实测两边的值：

```
tools/gt6_ore_chain.json  → inputs.gt6_reference.sha256
    9cc36733316182976726cd9842f62ba73f2befabe2023eec86b2737f179faef8

tools/local_artifact_manifest.json → artifacts[gt6_recipe_normalized_reference].sha256
    84f90e5a5b6c70246e758f4063879ec23ce8e2ed7d8bb08ec59e2b26d60a4985
```

## 为什么会漂

`gt6_recipe_normalized_reference.json` 是 `compare_gt6_recipes --write-reference`
的产物。为修 `source_evidence` 跑过一次 `--write-reference` 之后：

```
reference 重新生成  →  hash 变了
    ↓
local_artifact_manifest.json 更新了
    ↓
gt6_ore_chain.json 里记的旧 hash  没跟着刷   ← 断在这里
```

## 为什么 rebuild 全绿但 record 挂

`rebuild_artifacts.py` 里 `compare_gt6_recipes` 是**有意 SKIP** 的
（它写 310 MB 报告 / 130 MB reference，写入模式必须手动选，不能扫进批处理）。

所以它是**唯一游离在自动链之外、却有下游依赖的 builder**。
手动跑过它之后如果不跟一次完整 rebuild，下游记录的 hash 就会滞留。

**不是"write 阶段改了 check 阶段的文件"，是手动跑的那一步没把下游带上。**

---

# Part 2 · 要做的事

## W1 · 先判断 reference 该不该变 ⭐ 先做这个

那次 `--write-reference` 是为了补 `source_evidence`。**如果 reference 的内容本可以
不变，最省事的修法是回滚，一步都不用跑。**

```bash
git log -p --follow tools/local_artifact_manifest.json | grep -n "84f90e5a\|9cc36733"
git log --oneline -- tools/gt6_recipe_normalized_reference.json
```

判断标准：

- **reference 内容实质变了**（GT6 归一化结果不同）→ 走 W2，刷新下游
- **只是重新生成、内容等价** → 从 git 恢复旧 reference 与 manifest 条目，
  `gt6_ore_chain.json` 的旧 hash 自动重新对上

**验收**
- [ ] 写明那次 `--write-reference` 到底改变了什么（一两句话即可）
- [ ] 给出选择 W2 还是回滚的理由

## W2 · 若确需保留新 reference，按正确顺序刷新

```bash
# 1. reference 已经是新的（不用重跑）
# 2. 把下游全部带上
python tools/rebuild_artifacts.py --keep-going
git diff --stat

# 3. 再验证
python tools/run_full_verification.py --record --new-session
```

**验收**
- [ ] `python tools/build_gt6_ore_chain.py --check` 通过
- [ ] `git diff` 中历史 baseline（T16–T21 五份 publication baseline、
      `t12a_machine_readiness.json` 的历史结论）**未被修改**
- [ ] 66 个 builder 的 `--check` 全部通过

## W3 · 把这条依赖写进文档，避免复发

`compare_gt6_recipes` 是唯一游离在 `rebuild_artifacts.py` 之外、却有下游的 builder。
这个坑会重复踩。

在 `tools/README.md`（或 `rebuild_artifacts.py` 的 docstring）加一句：

> `compare_gt6_recipes` 被 `rebuild_artifacts.py` 有意 SKIP（写入模式必须手动选择）。
> 手动执行 `--write-reference` 或 `--write-report` 之后，**必须**跟一次
> `python tools/rebuild_artifacts.py --keep-going`，否则
> `gt6_ore_chain.json` 等下游产物记录的 reference hash 会滞留。

**验收**
- [ ] 文档已加，位置在执行者会看到的地方

## W4 · `gt6_l1b_selected_recipe_operands.json` 的定位要定下来

现在它处在一个中间状态：

- `local_artifact_manifest.json` 把它列在 **`committed_compact_evidence`**（应提交）
- 但它有 **23 MB**，按"大文件不传"的习惯每次交接都被排除
- 结果：接收方永远少这个文件，`import_gt6_oredict` 永远报
  `missing manifest integrity artifact`

**二选一，写明理由：**

| 方案 | 做法 | 代价 |
|---|---|---|
| **A** | 真正提交它 | 仓库多 23 MB |
| **B** | 移进 `artifacts`（本地缓存），import 的校验改为缺失时显式 SKIP | 少一层完整性校验，但和 `gt6_dump` 同级处理，一致 |

**验收**
- [ ] 定位明确，`local_artifact_manifest.json` 与实际行为一致
- [ ] 若选 B，缺失时是 `SKIP` 而不是 `PASS`

---

# Part 3 · 顺带记录，本单不做

## 一 · import manifest 混了 5 个"外来 hash"

`import_gt6_oredict` 的 `artifact_hashes` 里，18 个 hash 分两类：

- **自产**（12 个）：`stable_hash(normalized_material_doc)` 等，从自己刚算的
  内存文档取，永远自洽
- **外来**（6 个）：`gt6_l1b_selected_recipe_operands`、`gt6_ore_chain`、
  `gt6_ore_chain_operands`、`material_registration_gate`、
  `acceptance_form_corrections`、`gt6_generation_bits`——读磁盘现状，
  而这些文件由排在它**后面**的 builder 产出

```
[0] import_gt6_oredict            ← 在这里记录 hash
[3] build_gt6_ore_chain           ← 产出 gt6_ore_chain.json
[5] build_gt6_material_form_gate  ← 产出 l1b_selected_recipe_operands
```

**现在能过，因为文件恰好都是最新的。** 但只要下游任一 builder 重新生成，
import 就会失败——这才是"套娃"这个感觉的真实来源。

**未来的正确形状**：谁产出谁校验。外来 hash 从 import 移出，各归各的 builder。
这是一次性重构，**建议放到 T22 关闭之后**，不要现在动。

## 二 · 违反的是"产物所有权"边界，不是"不改 Java"

你之前的直觉方向对，但具体不是"跨阶段改 Java"那条。

真正被跨越的边界是：**一个 builder 记录了另外五个 builder 输出的 hash**。
本质上是 index 0 的 builder 声称拥有 index 3/5 的产物。所以：

- rebuild（只 write）→ 自洽
- record（只 check）→ 任一下游重新生成后就炸

这是历史遗留，不是 T22 引入。只是 T21/T22 连着做了大量 rebuild，
才第一次被频繁触发。

---

# Part 4 · 规范提醒（复用 T21 六条）

这一轮触到的是其中三条，重点重申：

**规矩三 · 检测 ≠ 刷新。**
`--check` 只告诉你过期了；`rebuild_artifacts.py` 负责重算。
**不要让验证流程在检查前先刷新** ——那样检查恒真。

**规矩五 · 改共享文件后必须沿链重建。**
`compare_gt6_recipes --write-reference` 就是典型的"改共享文件"。
跑完必须 `rebuild_artifacts.py --keep-going` 再 `git diff`。

**规矩六 · 同一个 check 连续两次红就停下汇报。**
这次的"无限套娃"正是第三次、第四次尝试的产物——每次修一个 builder 就跑一次
record，看到下一个红再修。**正确动作是第二次红时停下来看整条链**，
而不是逐个追。

---

# 附：本轮实测速查

| 项 | 值 |
|---|---|
| import manifest hash 校验 | 18/18 自洽，0 mismatch |
| `import_gt6_oredict --check --reference-only` | EXIT=0 |
| `build_component_rules --check` | PASS，48 sources → 8,141 expansions |
| `build_gt6_ore_chain --check` | **FAIL：GT6 reference hash** |
| `gt6_ore_chain.json` 记录值 | `9cc36733…f179faef8` |
| `local_artifact_manifest.json` 记录值 | `84f90e5a…d60a4985` |
| builder 总数 | 66 |
| `compare_gt6_recipes` 在 rebuild 中 | **SKIP（有意）** |
