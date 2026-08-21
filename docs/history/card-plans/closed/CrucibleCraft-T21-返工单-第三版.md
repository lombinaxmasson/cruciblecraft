# CrucibleCraft · T21 第三版返工单

> 复核对象：`tools-2026-8-10-0832.7z` + `src-2026-8-10-0813.7z`
> 复核方式：拼装仓库后实跑 `run_python_tests.py --suite fast`（454 tests）
> 结果：**核心证据通过；47 项失败，其中约 42 项属于同一个根因**
>
> 这一版分四部分。**Part 0 是技术说明，请先读完再动手**——本轮大部分问题
> 不是代码写错了，而是这个项目有一层"产物刷新"纪律，之前的卡没触发过它。

---

# Part 0 · 技术说明：currentness 链是什么

## 0.1 一句话

这个项目里，**每一份生成产物都记录了"我是从哪些文件、什么内容算出来的"**。
只要上游文件变了一个字节，下游产物就自动变成"过期"，并且会硬失败。

## 0.2 长一点的说明

打开任意一个 `tools/tXX_*.json`，会看到类似这样的段落：

```json
"currentness": {
  "owned_inputs": {
    "tools/t16_machine_denominator_policy.json": "25f7d595...",
    "src/main/resources/data/cruciblecraft/machine_tiers.json": "35db6364..."
  }
}
```

这不是元数据装饰，是**契约**：

- 产物声明了它消费的每个输入及其 SHA-256；
- 对应的 builder 用 `--check` 重算一遍，哈希对不上就抛
  `stale generated file: ...`；
- 因为是 SHA-256，**换行符、BOM、空格都算内容变化**。

这套设计的好处在本轮已经证明了：47 个失败没有一个是伪 PASS，没有一个静默通过，
每一个都指名道姓报出了是哪个文件漂移。**这是它在正常工作，不是它坏了。**

## 0.3 为什么这次会级联

`tools/verification_builder_policy.json` 里 60 个 builder 是**按依赖顺序排列**的：
`processing_machine_energy_audit` → `t12` → `t13` → `t14` → `t15` → `t16/t17` →
`t18/t19` → `t20` → `t21`。

后面的产物把前面产物的哈希记进自己的 `currentness`。所以：

```text
改 1 个共享文件
   → 直接消费它的 3 个产物过期
      → 消费那 3 个产物的 8 个产物过期
         → ... 一路传到 t21
```

T21 改了 5 个共享文件（3 个 Java + 2 个材料 JSON）加若干 policy，
**下游约 20 个产物一个都没重新生成**，于是整条链一起报警。

具体证据（这条最能说明问题）：

| 文件 | 8/08 pre-T21 | 当前 |
|---|---|---|
| `ModProcessingMachines.java` | `d41a3419...` | `8934e836...` |
| `CrucibleCraftGameTests.java` | `e5e0d445...` | `50eee9ce...` |

而 `t15_matcher_boundary.json` / `machine_crafting_readiness.json` /
`t12a_machine_readiness.json` 里记的仍是 `d41a3419`，
`t6_electrical_readiness.json` 里记的仍是 `e5e0d445`——**全是 8/08 的值**。

## 0.4 之前的卡为什么没遇到

翻 T13–T20 的档案会发现一个规律：那七张卡几乎不碰共享 Java。
它们的关闭快照全都写着「新增内容只有 N 条 vanilla crafting recipe，
GT row 新增 0，`publication delta = 0`」。

**T21 是第一张同时修改运行时 Java、材料定义并让 publication 真的变化的卡**
（18,875 → 18,879）。所以这个刷新缺口一直存在，只是到 T21 才第一次被触发。

这不是谁写错了代码，是覆盖面第一次到达。

## 0.5 三条不可违反的规则

这三条在前两版返工单里已经踩过坑，这里集中说明。

### 规则一 · 历史基线 ≠ 当前账，永远不要"更新"历史

有一类文件专门保存**某个阶段开工前的状态**，例如：

```json
{ "status": "T16D_PUBLICATION_BASELINE", "captured_before": "T16",
  "publication_totals": { "logical_rows": 18875, ... } }
```

它存在的唯一目的是证明 T16 的 `publication delta = 0`。
把 18,875 改成今天的 18,879，那条断言就退化成「今天 == 今天」，**永久失去检测能力**。

> 判断方法：文件里出现 `captured_before` / `baseline: "TXX"` /
> `status: "TXXX_BASELINE"` / `*_machine_readiness` 的历史段落 → **只读**。
> 当前值变了，要新建一份当前 baseline，不是改旧的。

### 规则二 · 期望值对不上时，默认是实现有问题

看到 `expected 33 != actual 35` 这类失败，**不要改 expected**。
默认结论是实现或 currentness 链有问题。

真的需要改期望值，必须单独写出理由和固定来源，并在交付里点名。

### 规则三 · 检测 ≠ 刷新

`--check` 只告诉你过期了，**不会帮你重算**。
重算要用 builder 的写入模式（见 Part 4 的脚本）。

**这是本轮所有问题的核心**：60 个 builder 的 `ordinary_args` 全是 `--check`，
项目里从来没有一个"按序全量重建"的入口，所以刷新只能靠人记住顺序、逐个跑。

## 0.6 一个真实例子（我实测过）

```bash
$ python tools/build_t17_readiness.py --check
T17 readiness failed: processing-machine energy audit is stale:
  stale generated file: tools/processing_machine_energy_audit.json
```

T17 的 readiness 报错，但**问题根本不在 T17**——是排在它前面很远的
`processing_machine_energy_audit` 没刷新。

按顺序先重建那一个，T17 的这条错误就消失了。

> **所以：不要逐个 artifact 去对付这些 stale。** 那是在手工模拟一个本该自动化的
> 过程，而且必然漏。先按顺序批量重建，剩下的失败才是真问题。


---

# Part 1 · 架构修复（优先，做完再碰 T21）

## A1 · 引入按序重建入口 ⭐ 最优先

**问题**：60 个 builder 只有 `--check`，没有任何批量重建方式。
拓扑序其实已经存在于 `verification_builder_policy.json` 的排列里，缺的只是一个遍历。

**做法**：使用随本单提供的 `tools/rebuild_artifacts.py`（用法见 Part 4）。
我已经在实际仓库上跑通过：T16 那条链 3 个 builder 全部重建成功，
`build_t16_machine_denominator.py --check` 从 stale 变成
`T16_MACHINE_DENOMINATOR_READY`。

写入模式是从每个 builder 自己的 argparse 自动探测的：

- 声明了 `--write` → 用 `--write`（23 个）
- 只声明 `--check` → 不带参数即写入（35 个）
- `build_t21_readiness` → **跳过**（status 归 `run_full_verification` 独占）
- `compare_gt6_recipes` → **跳过**（写 310 MB 报告，写入模式必须手动选）

**验收**
- [ ] `python tools/rebuild_artifacts.py --dry-run` 列出 58 rebuild + 2 skip
- [ ] 脚本进 `tools/`，并在 `tools/README.md` 的 verification workflow 里登记
- [ ] 明确写清：这是刷新工具，不是验证工具；重建后必须读 `git diff`

## A2 · `affected_rules` 改为自动生成 ⭐ 根因修复

**问题**：`python_test_policy.json` 里 25 条 `affected_rules` 是**手写的**
依赖图，而真实依赖图已经机器可读地存在于每个产物的 `currentness` 块里。
两份表示必然发散。

**实测偏差**（我算过）：

| 改动的文件 | affected 选中 | 实际会红的 25 个里漏掉 |
|---|---:|---:|
| `materials/charcoal.json` | 15 | **24** |
| `t21_template_denominator_policy.json` | 11 | **24** |
| `ModProcessingMachines.java` | 10 | **17** |
| `CrucibleCraftGameTests.java` | 24 | **15** |

改 `charcoal.json` 会让 25 个 readiness 过期，`affected` 只提醒其中 1 个。

这正是《总体规划》§5.1 说的那件事：

> 用间接信号代替直接观测，是这个项目里所有系统性错误的共同根因

**做法**：写一个生成器，扫描全部产物的 `currentness.owned_inputs` /
`tracked_inputs`，反转成 `输入路径 → 消费它的 builder / test module` 映射，
用它替换手写的 `affected_rules`。手写表只保留自动推导不到的特例，并标注理由。

**验收**
- [ ] `affected_rules` 由脚本生成，可复算，两次运行结果一致
- [ ] 用 `charcoal.json` 验算：选中的模块集合 ⊇ 实际会失败的模块集合
- [ ] 保留的人工特例逐条有理由

## A3 · 开工检查表补一条

在《总体规划》§7 加入：

> - [ ] 本卡是否会修改被 ≥2 个 readiness 记录为 input 的文件？
>       若是，收尾前必须按 `verification_builder_policy.json` 顺序重建下游产物，
>       并把重建列表与 `git diff` 摘要写进交付。

**为什么**：现在的检查表只有 §8 的「完整 verification 通过」兜底，
意味着发现时点永远落在最贵的那一刻（60 builder + 双 runData + 全 JUnit +
85 GameTest 跑完之后）。这条把它提前到开工时。

---

# Part 2 · T21 收尾

## B1 · 按序全量重建 ⭐ 先做这个

用 A1 的脚本：

```bash
python tools/rebuild_artifacts.py --dry-run          # 先看计划
python tools/rebuild_artifacts.py --keep-going       # 全量重建
git diff --stat                                      # 必读
```

**读 diff 时逐条确认**：任何**历史基线**被修改都是 bug，不是刷新（见规则一）。
重点检查这几类文件有没有被动：

- `t1x_publication_baseline.json`（四份 + 新增的 t21 一份）
- `t12a_machine_readiness.json`（immutable T12a history）
- 各阶段档案里的历史关闭计数

**验收**
- [ ] 重建后 `--suite fast` 的失败数从 47 大幅下降
- [ ] `git diff` 中没有历史基线被修改
- [ ] 重建列表写进 T21 交付账

## B2 · T14 source contract ⚠️ 唯一不能机械刷新的

```
ValueError: T14 source contract game_test is incomplete:
  ['metrics.eagerPublishedRecipes() == 16_650']
```

GameTests 里那句断言已改成 `16_654`，而 T14 的 source contract 硬要求源码里
存在 `16_650` 这个字面量。

**这是规则一的陷阱，而且长得非常像一次普通的数字更新。**
最省事的"修法"是把契约里的 16,650 改成 16,654——那等于篡改 T14 的历史关闭证据。

**正确做法**：让契约表达「T14 基线 + 后续各阶段 delta」，而不是把 T14 的数字
顶到今天。具体形式请先提方案再改。

**验收**
- [ ] T14 的历史值 16,650 / 18,875 在任何产物里都未被改写
- [ ] 契约能表达当前值 = T14 基线 + 已登记 delta
- [ ] 方案与理由写进交付

## B3 · T21 readiness 测试与 builder API 不匹配

`test_build_t21_readiness.py` 6 个测试全部 ERROR：

```
TypeError: check() takes 0 positional arguments but 1 was given
AttributeError: module has no attribute 'load'. Did you mean: '_load'?
KeyError: 'closure' / 'fidelity' / 'load'
```

测试期望 `check(document)`、`load()`、键 `closure`/`fidelity`/`load`/`runtime`/
`pending_stages`/`status`；实际 builder 提供 `check()`、`_load()`、键
`closure_summary`/`completed_stages`/`currentness`/`policy`/`status_owner`。

T21-A 的 status 守卫机制本身做得很好，只是重写时没同步测试。

**验收**
- [ ] 6 个测试全绿
- [ ] `status` 守卫的行为仍被测试覆盖（手写 status 会报错）

## B4 · `gt6_process_expectations.json` 补 7 条 source_evidence

7 个子系统缺 `source_evidence`：air / firebox / fuels / steam / steam_engine /
steelmaking / thermal。

`compare_gt6_recipes.py:3345` 是 `errors.append()` 硬失败（**不是**静默跳过，
这点是好的），所以这是 full verification 的确定性阻塞。

顺带两条：

- `load_builder_policy()` 的必填校验里加上 `source_evidence`（当前 60 行全没有）
- 文案 bug：数量断言已改成 `!= 60`，报错文案仍写 `"must contain 59 unique builders"`

**验收**
- [ ] `python tools/compare_gt6_recipes.py --check --reference-only` 通过
- [ ] `source_evidence` 缺失时 `load_builder_policy()` 抛错（实测贴报错）
- [ ] 报错文案与实际断言数字一致

## B5 · `extruder_shapes.json` 的 BOM 与哈希漂移

`tools/component_rule_sources/extruder_shapes.json` 是**全仓唯一**带 UTF-8 BOM
的 JSON（`src` 里 0 个）。同时它记录的 `index_sha256` / `report_sha256` 与实际的
`gt6_extruder_templates_index_v5.json` / `_report.json` 都对不上，导致
`build_component_rules` 抛 `SourceError`。

**先判断哪边是对的**：是产物被重新生成了没同步这里，还是这里记错了。
不要直接重记哈希了事。

**验收**
- [ ] BOM 去掉，全仓 JSON 无 BOM
- [ ] 哈希不一致的原因写清，再决定改哪边

## B6 · GameTest 计数：85，不是 86

`t21_gametest_delta.json` 自己记了正确答案（`unique_method_count: 85`），
但结论写成了 86。

**差额来源已查明**：源码里 `@GameTest(` 出现 **85** 次，第 86 个 `@GameTest` 是
`@GameTestHolder(CrucibleCraft.MODID)` 里的子串。而
`verify_full_verification_report.py:509` 的计数实现正是：

```python
path.read_text(encoding="utf-8").count("@GameTest(")
```

**验证器数的就是 85。** 83（基线）+ 2（T21）= 85，差额完全对上，
**不存在"多出的 1 个测试"**。

**另外**：这份 artifact 记的 "current sha256" 是 `e5e0d445`，
那是 **8/08 pre-T21** 的文件——说明整份分析是在 T21 改动之前的状态上跑的，
需要在当前树上重跑。

**验收**
- [ ] 期望 GameTest 数改为 85，并说明 86 是 `@GameTestHolder` 子串造成的
- [ ] `extra_test_unaccounted` 改为 0，三条"重新指派 owner"的建议撤销
- [ ] artifact 在当前树上重新生成

## B7 · 遗留项（前两版未关闭）

| 项 | 内容 |
|---|---|
| **R2** | `coal_coke/gem` / `charcoal/gem` 存档迁移。`ModItems.java:436` 确认 `form_items` 覆盖会跳过物品注册，这两个原本已注册的物品现在不再注册；`MaterialFingerprint` 也把 `formItems` 计入指纹。需要 quarantine 路径 |
| **R6a** | `GTRecipeMapLoader.java:173` 的 `t5ChemicalRecipes` 现在由 `isChemicalRecipe()` 计算，已含 t5/t11/t21。改名或至少加注释 |
| **R9** | 四份手写 T21 产物（`t21_beta_seed_layers` / `t21_gametest_delta` / `t21_builder_source_evidence_survey` / `t21_compact_artifact_policy`）零脚本引用。`t21_beta_seed_layers.json` 的 `"generated_by": "tools/t21_operand_reachability.py"` 与事实不符——那个脚本不写这个文件 |
| **R10** | compact 体积政策把上界定成"当前值 × 2"、总计 42 MB。这不是分层，是把现状合法化。需要真分层或明确承认为例外并给绝对上界 |
| **R11** | `t21_load_projection_input.json` 的时间类 metric 标着 `STATIC_INFERENCE`、区间统一是 `[0, 上界]`、`server_reload_ms` 含 Python 启动时间。要么真测，要么状态改成 `DEFERRED` 而非 `PASS` |
| **R12** | 可达性闭包 `material_rule` 计数 48（我独立算是 90）。方向安全不阻塞，但值得查是过滤规则不同还是漏解析 |

## B8 · 政策问题：fast suite 依赖 raw dump

`test_wrong_expected_amount_is_detectable` 在 **fast suite** 里，却要读
`gt6_dump/gt6_recipe_dump/maps/gt.recipe.mixer.json`。

按 T17 定的规矩，ordinary CI 不该依赖 raw dump。要么移进 `source-replay`，
要么改成读 compact evidence。

---

# Part 3 · 还差两个文件

| 文件 | 为什么该有 |
|---|---|
| `gt6_recipe_expectations.json` | `local_artifact_manifest.json` 把它列在 **`committed_compact_evidence`**，说明本该提交，不是本地缓存 |
| `gt6_l1b_selected_recipe_operands.json` | 同上 |

上一轮缺的 5 个 Python 模块和 mixer templates 三件套都已补齐。

**顺带清理**：`t12_machine_readiness.json` 被引用但不存在——T15a 已改名为
`t12a_machine_readiness.json`（该文件在），属于残留的过时引用。

---

# Part 4 · `rebuild_artifacts.py` 用法

```bash
# 看计划，什么都不跑
python tools/rebuild_artifacts.py --dry-run

# 全量按序重建（推荐首次这样跑）
python tools/rebuild_artifacts.py --keep-going

# 只重建某几段（顺序仍按 policy）
python tools/rebuild_artifacts.py --only t16,t17

# 从某个 builder 开始往后重建
python tools/rebuild_artifacts.py --from t13

# 重建后立刻用各自的 --check argv 复验
python tools/rebuild_artifacts.py --verify
```

**输出示例**（我实测过的真实输出）：

```text
[OK  ] build_t16_machine_denominator   0.085s
[OK  ] build_t16_machine_acquisition   0.081s
[FAIL] build_t17_readiness             0.133s
        T17 readiness failed: processing-machine energy audit is stale:
          stale generated file: tools/processing_machine_energy_audit.json
```

这条 FAIL 说明的正是 Part 0.6 讲的事：T17 的错误根源在排它前面的
`processing_machine_energy_audit`。全量按序跑就不会有这个问题。

**注意事项**

1. 这是**刷新**工具，不判断刷新是否合法。跑完**必须读 `git diff`**。
2. 部分 builder 需要 raw dump / fetched source；本机没有时会失败，用
   `--keep-going` 跳过并记录，不要为了让它绿而伪造输入。
3. `build_t21_readiness` 和 `compare_gt6_recipes` 被有意跳过，理由写在脚本
   `SKIP` 字典里。

---

# 执行顺序

```text
A1  引入 rebuild_artifacts.py          （无前置）
 │
B1  按序全量重建 + 读 git diff          （需要 A1）
 │
 ├─ B2  T14 source contract            ⚠️ 需要先提方案
 ├─ B3  T21 readiness 测试 API
 ├─ B4  source_evidence 7 条
 ├─ B5  extruder_shapes BOM / 哈希
 ├─ B6  GameTest 计数改 85
 └─ B7  遗留项 R2 / R6a / R9 / R10 / R11 / R12
 │
A2  affected_rules 自动生成            （可与 B 组并行）
A3  §7 检查表补一条
 │
B8  fast suite 移除 raw dump 依赖
 │
最终：85 GameTest（dedicated server）→ source-replay → closure
     → 单次 clean `run_full_verification.py --record --new-session`
```

`T21_READY` 在全部完成前不要写进任何文档。
当前 `t21_readiness.json` 无 `status` 字段的设计是对的，保持不动。

---

# 附：本轮通过的部分

这些已经验证成立，不需要返工：

- **`gt6_mixer_templates` 三个测试全绿**，包括
  `test_membership_is_a_total_bijection_over_source_indexes` 与
  `test_compact_evidence_is_complete_and_current`。
  **64,245 行零差异重放这条 T21 最核心的证据独立验证通过。**
- `t21_operand_reachability` 15 个测试全过，含
  `test_partially_reachable_fixture_gate_fails` 与 6 个 schema v2 双证明测试。
- `t21_template_denominator` 的 `test_v1_template_has_positive_reachability_evidence`
  与 `test_coverage_is_split_before_denominator_publication`。
- `recipe_load_projection` 的 T21 投影测试。
- 第一版 R1（历史基线回退）、R3（CRLF）、R7（嵌套目录）与 T21-A（status 守卫）。
