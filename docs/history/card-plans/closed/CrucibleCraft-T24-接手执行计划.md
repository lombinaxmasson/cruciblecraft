# CrucibleCraft T24 · 可复现规模基线 —— 接手执行计划

> 交接对象：接手人（本文默认你不熟悉这个仓库）
> 编写日期：2026-08-14
> 上游依据：《CrucibleCraft-第四阶段总体规划.md》T24 卡
> 起点证据：`tools/full_verification_report.json` = `READY`，`verified_on = 2026-08-14`
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`

---

## 0. 先读这一页（5 分钟，别跳过）

### 0.1 你要做的这张卡是什么

T24 叫"可复现规模基线"。**它不加任何游戏内容**——不加配方、不加机器、不加方块、不加物品、不加多方块。

它只做一件事：

> 把"这个模组跑多大规模不会炸"从一句感觉，变成一份别人能在另一台电脑上按同样步骤重建出来的证据。

产出的是：三个规模的场景定义（small / target / stress）、能自动跑出确定结论的门禁、一份诚实的测量记录、以及一张"发现清单"交给下一张卡 T25。

### 0.2 三条铁律（违反任何一条，这张卡就作废）

1. **同一时刻只有一张 T 卡在进行中。** 你只做 T24。看到 T25/T26 的东西，记下来，不要动手。
2. **测不到的东西写 `SKIP`，不写 0，不写估计值。** 这个项目宁可少一个结论，也不要一个假结论。写 `SKIP` 不丢人，编数字会让整条链条失效。
3. **不许手改自动生成的文件。** 所有 `tools/t*.json`（除了明确标注 policy / evidence 的授权输入）都由 Python builder 生成。你改了值，`--check` 会立刻报 `stale generated file`。

### 0.3 你接手时仓库是绿的

第一天什么都别写，先自己验一遍（见第 2 节）。如果验不过，**先停下来记录**，不要边修边做新卡。

---

## 1. 当前基线（这些数字是你的起点，不是你要改的东西）

| 项目 | 当前值 | 权威来源 |
|---|---:|---|
| 完整验证状态 | `READY`（2026-08-14） | `tools/full_verification_report.json` |
| Java 单元测试 | 550（142 suites） | 同上 `tests.java_unit_tests` |
| 生产 GameTest | 114 | 同上 `tests.production_game_tests` |
| Python 测试（closure） | 652 | 同上 `tests.python_unit_tests` |
| 具体 datapack 配方文件 | 3,243 | 同上 `rules.datapack_recipe_entries` |
| publication（运行时当前值） | 18,882 logical / 16,657 eager / 2,225 lazy | `src/main/resources/data/cruciblecraft/t23_publication_baseline.json` 的 `delta_ledger_policy` |
| builder 数量 | 77（另有 3 个 pre-chain） | `tools/verification_builder_policy.json` |
| 已关闭阶段 | T20 / T21 / T22 / T22.5 / T23 全部 `_READY` | 各自 `tools/t2*_readiness.json` |
| T23 选定多方块 | `distillation_tower` / `large_boiler` / `tank_3x3x3` | `tools/t23_readiness.json` |

> ⚠️ **注意**：`full_verification_report.json` 里的 `rules.all_published_total = 18,875` 和上表的 18,882 **不是同一个计数器**。前者来自 T11 preflight 的投影口径，后者是当前运行时 logical 总数。**两个都对，不要互相"修正"。** 这类历史/当前双口径在本项目里很多，第 10 节有完整清单。

---

## 2. 第一天：接手体检（必做，约 2 小时）

按顺序执行，每一步都要绿：

```bat
:: 1. 仓库干净
git status

:: 2. 只读校验已提交的 READY（不跑 Gradle，几秒钟）
python tools\run_full_verification.py --check-ready

:: 3. builder 依赖图没有反向边
python tools\check_builder_graph.py

:: 4. 换行写入规范
python tools\check_text_write_newline.py

:: 5. Python 快速套件
python tools\run_python_tests.py --suite fast

:: 6. Java 单测（约 2 分钟）
.\gradlew.bat test

:: 7. 生产 GameTest（约 5 分钟）
.\gradlew.bat runGameTestServer
```

**完成判定**：7 条全绿，且 GameTest 输出 `All 114 required tests passed`。

**如果不绿**：不要修。在仓库根新建 `T24-接手异常记录.md`，写下命令、完整报错、时间，然后停下来联系原作者。**已关闭阶段变红是重大事件，不是你应该顺手修的东西。**

---

## 3. 已经替你做好的设计决定（照做，不要重新设计）

这几条是我读完 T24 卡和现有代码后定的，目的是让你走**风险最低、复用最多**的那条路。

| 编号 | 决定 | 理由 |
|---|---|---|
| **D1** | 场景用 **JSON 数据描述 + 运行时构建**，绝不做"黄金存档" | T24 卡原文禁止"手工维护一个无法重建的黄金存档"。数据描述可以从空目录重建。 |
| **D2** | CI 硬门禁**只锁四类**：场景身份、守恒、每 tick/每操作计数、声明上限 | T24 卡原文规定。墙钟时间**永远不做正确性门禁**。 |
| **D3** | 硬门禁**优先复用已有边界**，不发明新物理 | T19/T23 已经量化过大量上限（见第 7 节），你的活是"汇总成场景级台账"，不是重新测。 |
| **D4** | T24 的 publication delta 必须是 **0 / 0 / 0** | 不加配方内容。任何非零都说明你误改了东西。 |
| **D5** | 三个规模的具体数字标为 CrucibleCraft `DESIGN_POLICY` | 它们不来自 GT6 源码，必须显式声明，不能冒充 source-backed。 |
| **D6** | 代码风格**全部抄现成的** | 见第 5 节"抄哪个文件"列。这个项目的 builder 结构高度一致，照抄成功率最高。 |

---

## 4. 三个 workload 的规格（写进 policy 文件，之后不再手改）

下面是建议值。字段名固定，数值可以在开工时调，但**一旦写进 `t24_workload_policy.json` 并提交，后面就不许改**（改了所有测量都要重跑）。

| 字段 | small | target | stress | 说明 |
|---|---:|---:|---:|---|
| `seed` | 24001 | 24002 | 24003 | 固定随机种子 |
| `dimension` | overworld | overworld | overworld | |
| `loaded_chunks` | 3×3 | 8×8 | 8×8 | stress 不扩地图，只加密度 |
| `processing_machines` | 3 | 24 | 96 | RU/KU/EU 各 1/8/32 |
| `energy_converters` | 2 | 8 | 32 | firebox→boiler→steam engine→dynamo |
| `fluid_pipes` | 10 | 250 | 1,000 | |
| `item_pipes` | 10 | 250 | 1,000 | |
| `pipes_total` | 20 | **500** | **2,000** | 500/2,000 是刻意选的，见下 |
| `covers` | 4 | 24 | 96 | |
| `multiblocks` | 1（tank_3x3x3） | 3（T23 全部三种） | 12（每种 4 座） | |
| `petroleum_chain` | 0 | 1 | 4 | 复用 T22 已关闭的原油链 |
| `warmup_ticks` | 200 | 600 | 600 | |
| `sampling_ticks` | 1,200 | 12,000 | 12,000 | |
| `evidence_class` | `MEASURED_AT_SCALE` | 见第 9 节分叉 | 见第 9 节分叉 | |

**为什么 pipes 选 500 和 2,000**：仓库里已有 `PipeTransferPhaseTest` 证明 500 根管在 5-tick 位置相位调度下每 tick 恰好 100 根到期。所以：

- 500 根 → 每 tick 恰好 **100** 根到期；
- 2,000 根 → 每 tick 恰好 **400** 根到期；

这两个数字是**可静态推导的确定值**，可以直接写成 CI 硬门禁，不需要任何计时。这是 T24 最便宜、也最有说服力的一类证据（`STATIC_INFERENCE`）。

---

## 5. 交付物清单（文件级别）

| 路径 | 类型 | 谁写 | 抄哪个文件 |
|---|---|---|---|
| `tools/t24_workload_policy.json` | **授权输入**（你手写） | 你 | `tools/t23_multiblock_policy.json` |
| `tools/build_t24_workload_manifest.py` | builder | 你 | `tools/build_t23_load_bounds.py` |
| `tools/t24_workload_manifest.json` | 生成物 | builder | — |
| `tools/t24_scale_evidence.json` | **测量证据**（由测试运行产出后提交） | 测试 | `tools/t23_load_evidence.json` |
| `tools/build_t24_scale_bounds.py` | builder | 你 | `tools/build_t23_load_bounds.py` |
| `tools/t24_scale_bounds.json` | 生成物 | builder | — |
| `tools/build_t24_findings.py` | builder | 你 | 同上 |
| `tools/t24_findings.json` | 生成物 | builder | — |
| `tools/t24_readiness_policy.json` | **授权输入** | 你 | `tools/t23_readiness_policy.json` |
| `tools/build_t24_readiness.py` | builder | 你 | `tools/build_t23_readiness.py` |
| `tools/t24_readiness.json` | 生成物 | builder | — |
| `src/test/java/com/masson/cruciblecraft/scale/ScaleWorkloadBoundTest.java` | JUnit | 你 | `.../content/multiblock/MultiblockLoadBoundTest.java` + `.../logistics/pipe/PipeTransferPhaseTest.java` |
| `src/main/java/.../gametest/CrucibleCraftGameTests.java` | GameTest（追加方法） | 你 | 该文件里任意一个已有测试 |
| `tools/tests/test_build_t24_workload_manifest.py` 等 4 个 | Python 测试 | 你 | `tools/tests/test_build_t23_multiblock_classification.py` |
| `tools/verification_builder_policy.json` | 注册（追加 4 行条目） | 你 | 文件末尾 T23 三条 |
| `tools/verify_full_verification_report.py` | 注册（加 1 个函数 + 3 处登记） | 你 | 搜索 `t23_readiness_acceptance` 的全部出现位置 |
| `tools/phase4_v1_planning_contract.json` | 状态更新 | 你 | 见第 12 节（有陷阱） |
| `CrucibleCraft-阶段档案-T24.md` | 归档文档 | 你 | `CrucibleCraft-阶段档案-T20.md` |

---

## 6. 任务队列（8 步，每步一个 commit）

> 每步都写了：**目标 / 动作 / 完成判定 / 验证命令 / commit 信息**。
> 顺序不要打乱。每步做完提交一次，方便回滚。

---

### Step 0 · 建立工作分支与日志（0.5 天）

**动作**

```bat
git checkout -b t24-scale-baseline
```

在仓库根建 `T24-工作日志.md`，每天记三行：今天做了什么 / 卡在哪 / 明天做什么。

把 `tools/phase4_v1_planning_contract.json` 里的 `execution_policy.current_active_t` 从 `null` 改成 `"T24"`。

> ⚠️ **陷阱**：这个文件的 sha256 被 `tools/build_t20_readiness.py` 钉住了。改完必须：
> ```bat
> python tools\build_t20_readiness.py --write
> python tools\check_builder_graph.py
> python tools\run_python_tests.py --suite affected --path tools/phase4_v1_planning_contract.json
> ```
> 否则 T20 的 `--check` 会报 stale。

**完成判定**：`check_builder_graph.py` 绿，`--suite affected` 绿。

**commit**：`T24 Step0: open T24 as the active card`

---

### Step 1 · 写 workload policy（1 天）

**目标**：把第 4 节的表变成机器可读的授权输入。

**动作**：新建 `tools/t24_workload_policy.json`。骨架：

```json
{
  "schema_version": 1,
  "status_policy": "Scenario values are CrucibleCraft DESIGN_POLICY, not GT6 source facts.",
  "fidelity_class": "DESIGN_POLICY",
  "derivation_notes": {
    "pipes_total": "500 and 2000 are chosen so the 5-tick position-phased schedule yields exactly 100 and 400 due pipes per tick (PipeTransferPhaseTest).",
    "multiblocks": "Only the three T23 selected structures are used; no new structure is introduced.",
    "petroleum_chain": "Reuses the T22 closed crude-oil family; no new recipe row."
  },
  "scenarios": {
    "small": {
      "purpose": "minimum complete industrial mainline; fast functional regression",
      "seed": 24001,
      "dimension": "minecraft:overworld",
      "loaded_chunks": {"x": 3, "z": 3},
      "counts": {
        "processing_machines": 3,
        "energy_converters": 2,
        "fluid_pipes": 10,
        "item_pipes": 10,
        "covers": 4,
        "multiblocks": 1,
        "petroleum_chains": 0
      },
      "composition": {
        "processing_machines": {"RU": 1, "KU": 1, "EU": 1},
        "multiblock_ids": ["tank_3x3x3"]
      },
      "ticks": {"warmup": 200, "sampling": 1200},
      "declared_evidence_class": "MEASURED_AT_SCALE"
    },
    "target": { "...": "同结构，按第 4 节表填" },
    "stress": { "...": "同结构，按第 4 节表填" }
  },
  "success_summary_fields": [
    "items_consumed", "items_produced", "fluids_consumed_mb",
    "fluids_produced_mb", "energy_supplied", "energy_consumed",
    "energy_declared_loss", "recipes_completed",
    "pipes_due_per_tick_max", "route_discovery_visited_max",
    "route_cache_entries_max", "port_scan_ops_max",
    "structure_validation_ops_max"
  ]
}
```

**完成判定**：三个场景字段齐全，每个数字在 `derivation_notes` 里能找到理由（哪怕理由是"CC 平衡策略，无 GT6 来源"）。

**commit**：`T24a: authored workload policy for small/target/stress`

---

### Step 2 · workload manifest builder（2 天）

**目标**：从 policy 确定性地生成 manifest，并给出一个**场景身份指纹**。

**动作**：新建 `tools/build_t24_workload_manifest.py`。直接照抄 `build_t23_load_bounds.py` 的骨架（`build()` / `check()` / `write()` / `main()` 四段），替换内容。核心：

```python
def build() -> dict[str, Any]:
    policy = _load_if_exists(POLICY) or {}
    scenarios = {}
    for name, row in sorted((policy.get("scenarios") or {}).items()):
        counts = row.get("counts") or {}
        pipes_total = counts.get("fluid_pipes", 0) + counts.get("item_pipes", 0)
        scenarios[name] = {
            **row,
            "derived": {
                "pipes_total": pipes_total,
                # 5-tick 位置相位调度：每 tick 到期 = 总数 / 5
                "pipes_due_per_tick": pipes_total // 5,
                "pipes_partition_exact": pipes_total % 5 == 0,
            },
        }
    document = {
        "schema_version": 1,
        "status_owner": "build_t24_workload_manifest",
        "fidelity_class": policy.get("fidelity_class"),
        "scenarios": scenarios,
        "status": "MANIFEST_COMPLETE" if _complete(scenarios) else None,
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
                _relative(POLICY): _sha256(POLICY),
            }
        },
    }
    # 场景身份指纹：不含 currentness，保证同输入同指纹
    document["workload_identity"] = hashlib.sha256(
        _stable({"scenarios": scenarios}).encode("utf-8")
    ).hexdigest()
    return document
```

**四个必须注意的点**：

1. 写文件必须是 `OUTPUT.write_text(_stable(document), encoding="utf-8", newline="\n")`。**漏掉 `newline="\n"` 会被 `check_text_write_newline.py` 直接拦下**（这是历史上炸过一次的坑）。
2. `status` 在证据不全时写 `None`（fail-closed），**不要写空字符串或 "PENDING"**。
3. `_stable()` 用 `sort_keys=True` + 末尾换行，和其它 builder 保持字节一致。
4. `workload_identity` 计算时**排除 `currentness`**，否则改 builder 注释都会让指纹变。

然后在 `tools/verification_builder_policy.json` 的 `builders` 数组**末尾**追加：

```json
{
  "name": "build_t24_workload_manifest",
  "script": "tools/build_t24_workload_manifest.py",
  "ordinary_args": ["--check"],
  "proof_tier": "rederived",
  "outputs": ["t24_workload_manifest.json"]
}
```

再把 `t24_workload_policy.json` 和 `t24_workload_manifest.json` 两个文件名加进 `tools/verify_full_verification_report.py` 顶部的 `CORE_ARTIFACTS` 元组（加在 T23 那几行后面）。

写 `tools/tests/test_build_t24_workload_manifest.py`，至少三个用例：
- `test_committed_output_is_current`（跑 `check()` 应无错）
- `test_check_mode_does_not_modify_files`（比对 mtime/sha256）
- `test_workload_identity_is_stable_across_rebuilds`

**验证**

```bat
python tools\build_t24_workload_manifest.py
python tools\build_t24_workload_manifest.py --check
python tools\check_builder_graph.py
python tools\check_text_write_newline.py
python tools\run_python_tests.py --suite affected --path tools/build_t24_workload_manifest.py
```

**完成判定**：五条全绿，`t24_workload_manifest.json` 里三个场景都有 `pipes_partition_exact: true`。

**commit**：`T24a: deterministic workload manifest builder and identity`

---

### Step 3 · 有界计数的 JUnit 证据（2–3 天）

**目标**：把"这个规模下每 tick 最多做多少次操作"变成**不依赖计时**的断言。这是 T24 最有价值的部分，也是最安全的部分（纯 JVM 测试，不开游戏）。

**动作**：新建 `src/test/java/com/masson/cruciblecraft/scale/ScaleWorkloadBoundTest.java`。

抄两个现成文件的写法：
- `PipeTransferPhaseTest.java` —— 纯逻辑循环 + `assertEquals`；
- `MultiblockLoadBoundTest.java` —— 假 accessor + 计数器 + worst-of-N。

至少写这几个测试：

| 测试 | 断言内容 | 期望值来源 |
|---|---|---|
| `targetScenarioSchedulesExactlyOneHundredDuePipesPerTick` | 500 根管，每 tick 恰好 100 根到期 | `PipeTransferPhaseTest` 已证 |
| `stressScenarioSchedulesExactlyFourHundredDuePipesPerTick` | 2,000 根管 → 400 根 | 同一分区规则 |
| `scenarioMultiblockValidationOpsAreBounded` | 每座结构 validation ops = 2 × positions | `t23_load_evidence.json` |
| `scenarioPortScanOpsAreBounded` | 每个 port 2 次操作 | 同上 |
| `scenarioRouteDiscoveryStaysUnderDeclaredCap` | 访问管数 ≤ 32,768 | `t19_readiness.json` |
| `scenarioRouteCacheStaysUnderDeclaredCap` | 单管 route cache ≤ 256 项 | 同上 |
| `scenarioCoverPayloadStaysUnderDeclaredCap` | cover 配置负载 ≤ 13 bytes | 同上 |
| `manifestCountsMatchDeclaredBounds` | 从 `t24_workload_manifest.json` 读数，逐条比对上面所有上限 | manifest |

最后一个测试是关键：**它把 manifest 和硬门禁绑在一起**，以后谁改了场景数字导致越界，Java 测试直接红。

测试跑完把汇总写进 `tools/t24_scale_evidence.json`（结构照抄 `t23_load_evidence.json`）：

```json
{
  "schema_version": 1,
  "status": "MEASURED",
  "worst_case": true,
  "measured_by": "src/test/java/com/masson/cruciblecraft/scale/ScaleWorkloadBoundTest.java (JUnit, counter accessors, 16 rounds, worst-of-N)",
  "scenarios": {
    "small":  {"pipes_due_per_tick_max": 4,   "route_discovery_visited_max": 20,   "...": "..."},
    "target": {"pipes_due_per_tick_max": 100, "route_discovery_visited_max": 500,  "...": "..."},
    "stress": {"pipes_due_per_tick_max": 400, "route_discovery_visited_max": 2000, "...": "..."}
  },
  "declared_caps": {
    "route_discovery_visited": 32768,
    "route_cache_entries_per_pipe": 256,
    "cover_slots_per_pipe": 6,
    "cover_configuration_payload_bytes": 13,
    "multiblock_scan_volume": 4096,
    "menu_container_data_ints": 3
  }
}
```

再写 `tools/build_t24_scale_bounds.py`（照抄 `build_t23_load_bounds.py`，几乎一模一样），从 evidence 派生 `status = MEASURED`，注册进 builder policy + `CORE_ARTIFACTS` + Python 测试。

**验证**

```bat
.\gradlew.bat test
python tools\build_t24_scale_bounds.py --check
python tools\run_python_tests.py --suite affected --path tools/build_t24_scale_bounds.py
```

**完成判定**：Java 测试数从 550 增加（增加多少无所谓，报告会自动重算），`t24_scale_bounds.json` 状态为 `MEASURED`。

**commit**：`T24b: bounded operation-count evidence for three scenarios`

---

### Step 4 · small 场景真实运行 GameTest（3–5 天，最难的一步）

**目标**：证明 small 场景能**真的在游戏里搭起来、跑到确定摘要、且守恒**。

这是 T24 卡里"三个场景都可从空目录重建并运行到确定摘要"的核心证据。small 场景足够小，能塞进一个 GameTest。

**动作**：在 `src/main/java/.../gametest/CrucibleCraftGameTests.java` 里追加方法（该文件已有 9,886 行、114 个测试，你只是加在末尾）。

命名用统一前缀，后面 readiness builder 会按名字扫描：

| 方法名 | 内容 |
|---|---|
| `t24SmallWorkloadBuildsToDeclaredIdentity` | 按 manifest 的 counts 摆放方块，断言实际放置数 == manifest 声明数 |
| `t24SmallWorkloadRunsToDeterministicSummary` | 跑 warmup + sampling ticks，断言摘要字段等于固定期望值 |
| `t24SmallWorkloadConservesItemsFluidsEnergy` | 断言守恒（见第 8 节常量表） |
| `t24SmallWorkloadRespectsDeclaredOperationCaps` | 断言运行中各计数不超上限 |
| `t24WorkloadMutationFailsStructureGate` | 故意少放一个机器，断言身份门禁**失败**（这条证明门禁真的有效） |

**写法提示**（照抄已有测试的模式）：

```java
@GameTest(template = TEMPLATE, timeoutTicks = 2000)
public static void t24SmallWorkloadRunsToDeterministicSummary(
        GameTestHelper helper) {
    // 1. 按 manifest 摆放（数量硬编码为 small 场景的声明值，
    //    并由 ScaleWorkloadBoundTest 保证与 manifest 一致）
    // 2. helper.runAfterDelay(WARMUP + SAMPLING, () -> { ... 断言 ... })
    // 3. 所有断言都是整数相等，不要断言时间
}
```

**如果 GameTest 超时**：`timeoutTicks` 可以调大，但**不要为了通过而缩短 sampling ticks**。如果 1,200 tick 实在跑不完，把 small 的 sampling 降到能跑完的值，**回 Step 1 改 policy 并重跑 Step 2/3**，在 `derivation_notes` 里写清楚为什么降。这是允许的（数值是 DESIGN_POLICY），偷偷改测试不允许。

**验证**

```bat
.\gradlew.bat runGameTestServer
```

**完成判定**：输出 `All N required tests passed`，N = 114 + 你新增的数量。报告里的 GameTest 数量是**自动从源码扫描的**，不用手改。

**commit**：`T24b: small workload runs to a deterministic summary in GameTest`

---

### Step 5 · target / stress 观测（1–3 天，或 0.5 天走 SKIP 分叉）

**目标**：拿到 target/stress 的规模数据 —— **或者诚实地记录拿不到**。

**先做判断题**：

```
你的电脑能否跑起 dedicated server 并加载 8×8 区块 + 96 台机器 + 2,000 根管，
且持续 12,600 tick 不崩？
```

**分叉 A：能跑**

1. 用 `.\gradlew.bat runServer` 起 dedicated server；
2. 按 manifest 搭场景（可以先只做 target，stress 可选）；
3. 记录环境 —— **必须完整**，格式照抄 `tools/t14_recipe_load_benchmark.json` 的 `measurement.runtime` 段：

```json
"runtime": {
  "os_name": "...", "os_version": "...", "os_arch": "...",
  "processors": 0,
  "java_vendor": "...", "java_version": "...", "java_vm_version": "...",
  "jvm_arguments": ["..."],
  "heap_max_bytes": 0
},
"method": {
  "clock": "System.nanoTime",
  "warmup_ticks": 600,
  "sampling_ticks": 12000,
  "samples": 0,
  "timing_error_bar": "nonparametric 95% median order statistic"
}
```

4. 把结果写进 `t24_scale_evidence.json` 的 `measured_at_scale` 段，`evidence_class` 标 `MEASURED_AT_SCALE`。

**分叉 B：跑不动（完全可以接受）**

在 `t24_scale_evidence.json` 里显式写：

```json
"measured_at_scale": {
  "small": {"status": "MEASURED_AT_SCALE"},
  "target": {
    "status": "SKIP",
    "reason": "No declared measurement environment available during handover; the operator's machine cannot host the declared target topology.",
    "blocked_conclusions": [
      "target-scale wall-clock reload/index",
      "target-scale retained memory",
      "target-scale network sync bytes"
    ],
    "replacement_condition": "Re-run on a declared environment with >= 16 GB RAM and record tools/t24_scale_evidence.json measured_at_scale.target",
    "recheck_point": "T26 Beta candidate"
  },
  "stress": { "同上" }
}
```

**这不是失败。** T24 卡明确写了"无法采集的指标显式 SKIP 并阻断相应结论，不伪装为零"。走分叉 B 的后果只是：T25 的输入里少几条 finding，T25 大概率以 `selected = 0` 关闭（这也是卡里明确允许的）。

**无论走哪个分叉**，Step 3 的 `STATIC_INFERENCE` + `SYNTHETIC_BENCHMARK` 证据都保留 —— target/stress 的**操作计数门禁照样成立**，只是墙钟/内存/网络那几条没有 measured 数据。

**完成判定**：`t24_scale_evidence.json` 里三个场景各自有一个明确的 `status`，没有空值、没有 0 冒充。

**commit**：`T24b: scale observation for target/stress (measured | explicit SKIP)`

---

### Step 6 · findings 台账（1 天）

**目标**：把 T24 发现的每一条东西整理成 T25 能直接消费的清单。

**动作**：新建 `tools/build_t24_findings.py` → `tools/t24_findings.json`：

```json
{
  "schema_version": 1,
  "status_owner": "build_t24_findings",
  "policy": "Only findings with reproducible evidence may be handed to T25 as blocking. Measurement noise and cross-machine comparisons are never blocking.",
  "findings": [
    {
      "id": "T24-F001",
      "title": "...",
      "evidence_class": "STATIC_INFERENCE | SYNTHETIC_BENCHMARK | MEASURED_AT_SCALE",
      "evidence_artifact": "tools/t24_scale_evidence.json#/scenarios/target/...",
      "reproduce_command": "python tools/... 或 .\\gradlew.bat ...",
      "blocks_beta": false,
      "disposition": "non_blocking",
      "failure_boundary": "...",
      "owner": "T25"
    }
  ],
  "counts": {"total": 0, "blocking": 0, "non_blocking": 0, "skipped_measurement": 0}
}
```

**关键纪律**：`blocks_beta: true` 只能给**有可复现证据**的项。你觉得"这里好像有点慢"——那是 `non_blocking` + 一句 reason，不是 blocker。如果一条 blocker 都没有，`findings` 是空数组也完全合法。

注册 builder + `CORE_ARTIFACTS` + Python 测试（同 Step 2）。

**commit**：`T24c: finding ledger for T25 consumption`

---

### Step 7 · readiness builder 与报告注册（2 天）

**目标**：让 `t24_readiness.json` 的状态由证据自动派生，并接进完整验证报告。

**动作 A**：新建 `tools/t24_readiness_policy.json`（照抄 `t23_readiness_policy.json`）：

```json
{
  "schema_version": 1,
  "status_policy": "The status field is derived by build() from the gates below; check() recomputes and compares. Missing status is fail-closed (not ready).",
  "stages": {
    "T24a": "Three declared-scale workloads defined as data with a deterministic manifest and workload identity; rebuildable from an empty directory.",
    "T24b": "Correctness and performance evidence separated into STATIC_INFERENCE / SYNTHETIC_BENCHMARK / MEASURED_AT_SCALE; CI hard gates lock only scenario identity, conservation and bounded counts.",
    "T24c": "Three-axis closeout with a finding ledger, explicit SKIP for uncollectable metrics, and zero publication delta."
  },
  "dependencies": {
    "workload_policy": "tools/t24_workload_policy.json",
    "workload_manifest": "tools/t24_workload_manifest.json",
    "scale_evidence": "tools/t24_scale_evidence.json",
    "scale_bounds": "tools/t24_scale_bounds.json",
    "findings": "tools/t24_findings.json",
    "publication_baseline": "src/main/resources/data/cruciblecraft/t23_publication_baseline.json",
    "full_verification_report": "tools/full_verification_report.json",
    "gametest_sources": "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java"
  },
  "closure_policy": {
    "final_closure_attempted": true,
    "pending": [],
    "reason": "T24 scenario definition, bounded-count gates, evidence classification and finding ledger are complete; repository-wide verification is bound separately by the full snapshot report."
  },
  "refresh_policy": {
    "status": "BOUND_TO_FULL_VERIFICATION_REPORT",
    "evidence": "tools/full_verification_report.json"
  }
}
```

**动作 B**：新建 `tools/build_t24_readiness.py`。**直接复制 `build_t23_readiness.py` 再改**，重点保留这几个机制：

- `REPORT_OWNED = ("currentness", "runtime", "status", "completed_stages", "pending_stages")` —— 这些字段由验证报告拥有，不参与过期比对；
- 按后缀扫描 GameTest 方法名统计（把 `LIFECYCLE_SUFFIXES` 换成你的 `t24Small*` 前缀）；
- publication delta 从 `t23_publication_baseline.json` 读，断言 **0 / 0 / 0**；
- `status` 只在所有 gate 通过时才写 `"T24_READY"`，否则 `None`。

三轴字段建议：

```python
"closure": {
    "scenarios_defined": 3,
    "scenarios_rebuildable_from_empty": True,
    "workload_identity": "...",
    "gametest_scenarios_executed": 1,      # small
    "mutation_gate_present": True,
},
"fidelity": {
    "distribution_source": "T20-T23 runtime sets",
    "synthetic_marked_as_measured": False,  # 必须 False
    "skipped_metrics": [...],               # 分叉 B 时非空
    "design_policy_declared": True,
},
"load": {
    "publication_delta": {"logical": 0, "eager": 0, "lazy": 0},
    "bounded_counts_status": "MEASURED",
    "findings_blocking": 0,
}
```

**动作 C**：改 `tools/verify_full_verification_report.py`，**三处**（搜 `t23_readiness_acceptance` 就能找到全部）：

1. 加函数 `derived_t24_readiness_acceptance()`（照抄 `derived_t23_readiness_acceptance`，约 1586 行附近）；
2. 在检查段（约 3022 行）加 T24 gate；
3. 在 mark-ready 写入段（约 3615 行）加 `document["t24_readiness_acceptance"] = t24_readiness`；
4. 顺便在 `REQUIRED_READINESS_BUILDERS` 元组（约 192 行）末尾加 `("tools/build_t24_readiness.py", "T24 readiness gate")`。

**验证**

```bat
python tools\build_t24_readiness.py --check
python tools\check_builder_graph.py
python tools\run_python_tests.py --suite closure
```

**完成判定**：closure 套件全绿，`t24_readiness.json` 存在但此时 `status` 可能还是 `null`（因为还没跑完整验证，这是正常的 fail-closed 行为）。

**commit**：`T24c: readiness builder and full-verification registration`

---

### Step 8 · 完整验证与归档（1 天 + 1 次跑机时间）

**动作**（严格按顺序，中间不要改任何文件）：

```bat
:: 1. 更新契约状态
::    execution_policy.current_active_t -> null
::    execution_policy.next_t -> "T25"
::    新增 execution_policy.t24_closure 段
python tools\build_t20_readiness.py --write

:: 2. 前向重建，确认不动点
python tools\rebuild_artifacts.py --verify

:: 3. 一次性完整验证（会跑 builder / 两次 datagen / Java / GameTest / Python / report）
python tools\run_full_verification.py --record --new-session

:: 4. 只读复核
python tools\run_full_verification.py --check-ready
```

> ⚠️ `--record` 一次跑完约 10–15 分钟（当前 Java 102 秒 + GameTest 272 秒 + Python 137 秒 + 两次 datagen）。
> **不要反复跑 `--record`。** 中途失败用 `--record --resume` 从失败步继续；只有当输入真的变了才 `--new-session`。

**动作 B**：写 `CrucibleCraft-阶段档案-T24.md`，结构照抄 `CrucibleCraft-阶段档案-T20.md`：关闭判据 / 各子阶段 / 三轴账 / 验证与交接。

**动作 C**：在《CrucibleCraft-第四阶段总体规划.md》的 T24 卡上方加一行状态：

```markdown
**状态：✅** `T24_READY`**。完整关闭证据见《CrucibleCraft-阶段档案-T24.md》。**
```

**完成判定**：`full_verification_report.json` 的 `status = READY`，且包含 `t24_readiness_acceptance` 段，其中 `status = "T24_READY"`。

**commit**：`T24: close with T24_READY bound to a single full verification session`

---

## 7. CI 硬门禁清单（直接抄，不用自己测）

这些上限已经在之前的阶段量化并写进产物了。**T24 的活是把它们汇总成场景级台账**。

| 门禁 | 上限值 | 来源产物 |
|---|---:|---|
| pipe 调度间隔 | 5 tick（位置相位精确 1/5 分区） | `t19_readiness.json` |
| 500 根管每 tick 到期 | 恰好 100 | 同上 + `PipeTransferPhaseTest` |
| item route 访问管数 | ≤ 32,768 | `t19_readiness.json` |
| 单管 route cache | ≤ 256 项 | 同上 |
| 每根管 cover 槽 | ≤ 6 | 同上 |
| cover summary 字符 | ≤ 768 | 同上 |
| cover 配置负载 | ≤ 13 bytes | 同上 |
| fluid 客户端同步 | 最多每 5 tick 一次 | 同上 |
| 多方块扫描体积 | ≤ 4,096 位置 | `t23_load_bounds.json` |
| 结构校验操作 | 每位置 2 次（isLoaded + blockState） | 同上 |
| port 扫描操作 | 每 port 2 次 | 同上 |
| menu 同步 | 3 个 ContainerData int | 同上 |
| item presence matcher cap | 12 | `t12_capacity_matcher_benchmark.json` |
| server reload | soft 5,000 ms / hard 10,000 ms | `t14_load_budget_policy.json` |
| server index | soft 500 ms / hard 1,000 ms | 同上 |
| client reload | soft 5,000 ms / hard 10,000 ms | 同上 |
| lookup p95 | soft 1 ms / hard 2 ms | 同上 |
| lookup 候选数 | soft 64 / hard 128 | 同上 |
| retained memory | soft 128 MiB / hard 512 MiB | 同上 |
| sync bytes | soft 16 MiB / hard 64 MiB | 同上 |
| eager publication | soft 18,000 / hard 21,000 | 同上 |

> 表里**时间类**（reload / index / lookup）在 T24 只做"声明环境内比较"，**不做跨机器正确性门禁**。计数类才是 CI 硬门禁。

---

## 8. 守恒断言清单（可直接用的既有常量）

这些都是前面阶段已经证明过的固定值，**照抄到断言里即可，不要自己重新推导**：

| 转换 | 固定关系 | 来源 |
|---|---|---|
| Boiler | 80 HU + 1 mB water → 160 mB steam | T18 |
| Steam Engine | 200 mB steam → 50 KU；4 mB/KU | T18（`SOURCE_BACKED`） |
| Steam Engine 输出 | CC 固定 12 KU/t | T18（`DESIGN_POLICY_FIXED_OUTPUT`，**不要写成 source-backed**） |
| Fuel Engine | 512 RU / fuel unit | T18 |
| Dynamo | 32 RU → 22 EU + 10 loss | T18 |
| Gas Generator | methane 1,536 units → 1,152 HU + 9 mB exhaust | T18 |
| item 管道 | `0 <= delivered <= consumed`；确认提交时 consumed == delivered | T19 |
| cable | 逐块精确损耗，零损耗 packet | T6/T22 |

**守恒断言写法**：`输入总量 == 输出总量 + 缓冲留存 + 声明损耗`。三项都要单独记账，**不许把差额丢进"损耗"**。

---

## 9. 证据分级与 SKIP 写法

三类证据，每条结论必须标明属于哪类：

| 分级 | 含义 | 你会用在哪 |
|---|---|---|
| `STATIC_INFERENCE` | 从算法边界 + 场景 manifest 推导 | 每 tick 到期管数、port 扫描次数、结构校验次数 |
| `SYNTHETIC_BENCHMARK` | 隔离组件或受控 workload | `ScaleWorkloadBoundTest` 的计数器结果 |
| `MEASURED_AT_SCALE` | 声明环境下的完整目标/压力场景 | small 的 GameTest；target/stress 视 Step 5 分叉 |

**三条红线**：

1. `SYNTHETIC_BENCHMARK` 的结果**绝不能标成** `MEASURED_AT_SCALE`。这是 T24 卡里明文禁止的。
2. 采不到就 `SKIP` + `blocked_conclusions` + `replacement_condition` + `recheck_point`（四个字段一个都不能少）。
3. 比较任何墙钟/内存/网络数字前，必须先记录硬件、OS、JVM、mod 版本、manifest 指纹、warmup 和采样协议。少一项就不能比较。

---

## 10. 绝对不要做的事

### 10.1 看起来像 bug、其实是历史的东西（**不要"修正"**）

| 现象 | 真相 |
|---|---|
| `rules.all_published_total = 18,875` vs 基线台账 `18,882` | 两个不同计数器：前者是 T11 preflight 投影口径，后者是当前运行时 logical。都对。 |
| `t19_readiness.json` 里写 538 JUnit / 83 GameTest / 501 Python，但现在是 550 / 114 / 652 | T19 关闭时的历史快照，被验证器硬校验。**改了会让 T19 gate 变红。** |
| `tools/t12a_machine_readiness.json` 声称 9 craft fail-closed | 不可变的 T12a 历史产物，已被 T15 取代。别碰。 |
| 文档里出现 3,230 / 3,260 / 3,270 三种配方数 | 历史或非权威口径。当前值只用 `full_verification_report.json` 的 `datapack_recipe_entries`。 |
| `tools/README.md` 说"50 个 builder"，实际 77 个 | README 是历史文字，不是门禁。要改就写当前值 + 日期。 |
| `t23_readiness.json` 里 `headroom_remaining: 2118` | T23 口径下的余量，不是你要维护的数。 |

### 10.2 操作红线

- ❌ 手改任何 `tools/t*.json` 生成物 → 改 policy/evidence，然后重跑 builder
- ❌ 手改 `src/generated/**` → 由 `.\gradlew.bat runData` 生成
- ❌ 删除或跳过任何测试来"让 CI 变绿"
- ❌ 顺手重构相邻系统（哪怕你看出它写得不好）
- ❌ 新增任何配方 / 机器 / 方块 / 物品 / 多方块
- ❌ 反复跑 `--record`（会产生 session churn，且浪费一小时）
- ❌ 在 T24 里顺手做 T25 的"优化"

---

## 11. 常见故障自救表

| 报错 | 原因 | 处理 |
|---|---|---|
| `stale generated file: tools/xxx.json` | 输入变了但没重跑 builder | 跑该 builder 的写模式（无 `--check`） |
| `BACK_EDGE` (check_builder_graph) | 早的 builder 钉了晚的 builder 的输出 | 调整 `verification_builder_policy.json` 里的**顺序**，新条目一律加末尾 |
| `OUTPUT_DECLARATION` | builder 写了没在 policy 里声明的文件 | 补全该条目的 `outputs` 数组 |
| `check_text_write_newline` 报你的新文件 | `write_text()` 漏了 `newline="\n"` | 加上，重跑 |
| 验证 session 拒绝复用 | 工具链/policy/快照有漂移 | `--record --new-session` |
| GameTest 数量对不上 | 不用管 | 数量从源码自动扫描，会自己更新 |
| Java 测试数从 550 变了 | 正常 | 报告自动重算，不是硬编码 |
| `git diff --check` 报 CRLF 警告 | 已知历史现象 | 警告不是错误，`exit_code = 0` 即可 |
| Python `affected` 套件把整轮升级成 `closure` | 你改的路径没在 policy 里 | 正常行为，让它跑完 |

---

## 12. 卡住的时候怎么办

**不要猜，不要绕，不要伪造。** 按这个模板在 `T24-开放项.md` 里记一条，然后跳过该项继续下一步：

```markdown
## OPEN-T24-00X
- 发生时间：
- 所在步骤：Step N
- 现象（原文粘贴报错）：
- 我试过什么：
- 影响到哪条结论：
- 建议的替代条件（什么条件满足后可以重做）：
- 状态：BLOCKED / SKIPPED_WITH_REASON
```

**必须停下来找原作者的三种情况**：

1. 接手体检（第 2 节）就不绿 —— 说明仓库在你接手前已经有问题；
2. 你发现必须修改任何**已关闭阶段**的产物才能继续；
3. 你发现必须新增配方 / 机器 / 内容才能完成 T24 —— **那说明方案跑偏了**，T24 一行内容都不该加。

---

## 13. 验收清单（全部打勾才算 T24_READY）

**Closure**

- [ ] `small` / `target` / `stress` 三个场景都由 `t24_workload_policy.json` 定义，`t24_workload_manifest.json` 可确定性重建
- [ ] 每个场景有 `workload_identity` 指纹，同输入同指纹
- [ ] `small` 场景在 GameTest 里真实搭建并跑到确定摘要
- [ ] 故意改动场景对象时，身份/结构门禁**失败**（有专门测试证明）
- [ ] `t24_findings.json` 每条 finding 都有证据种类、可复现命令和失效边界

**Fidelity**

- [ ] 场景的配方/物流/能源分布来自 T20–T23 的实际运行集合，没有凭空造
- [ ] 没有任何 `SYNTHETIC_BENCHMARK` 被标成 `MEASURED_AT_SCALE`
- [ ] 采不到的指标显式 `SKIP`，且四个字段（reason / blocked_conclusions / replacement_condition / recheck_point）齐全
- [ ] 场景数值显式标为 CrucibleCraft `DESIGN_POLICY`

**Load**

- [ ] `publication delta = 0 / 0 / 0`（logical / eager / lazy）
- [ ] 操作计数类门禁全部进入 CI 硬门禁
- [ ] 墙钟/内存/网络类结论全部附带完整环境声明，或显式 SKIP
- [ ] 没有跨机器硬秒数正确性门禁

**统一门禁（第四阶段第 4 节）**

- [ ] 当前只有一张 active T
- [ ] 三轴分别给出状态和证据
- [ ] 五种数量（source_fact / authored_rule / datapack_file / logical_row / eager+lazy）没有混写
- [ ] 新增注册对象为 0，所以本地化/模型债增量为 0（在档案里写明）
- [ ] `READY` 只绑定一次完整验证

---

## 14. 交给 T25 的东西

T24 关闭后，T25 的**唯一输入**是 `tools/t24_findings.json`。请在档案里明确写清：

- finding 总数、blocking 数、SKIP 数；
- 如果 `blocking = 0`，直接在档案里写明：**T25 允许以 `selected = 0` 且全部 disposition 完整关闭，不做预防性重写**（这是第四阶段规划原文允许的）；
- 每条 SKIP 的 `replacement_condition` 和 `recheck_point`，让 T26 的 Beta candidate 复跑时知道该补什么。

---

## 附：每日固定动作

**开工前**

```bat
git status
git pull
python tools\run_python_tests.py --suite fast
```

**收工前**

```bat
python tools\check_builder_graph.py
python tools\check_text_write_newline.py
python tools\run_python_tests.py --suite affected --path <你今天改的路径>
git add -A && git commit -m "T24 StepN: ..."
git push
```

在 `T24-工作日志.md` 写三行：今天做了什么 / 卡在哪 / 明天做什么。

---

## 附：工期估算

| 步骤 | 估时 |
|---|---|
| Step 0 接手体检与开卡 | 0.5 天 |
| Step 1 workload policy | 1 天 |
| Step 2 manifest builder | 2 天 |
| Step 3 JUnit 有界计数证据 | 2–3 天 |
| Step 4 small 场景 GameTest | 3–5 天 |
| Step 5 target/stress 观测 | 0.5–3 天（视分叉） |
| Step 6 findings 台账 | 1 天 |
| Step 7 readiness + 报告注册 | 2 天 |
| Step 8 完整验证与归档 | 1 天 |
| **合计** | **约 13–19 个工作日** |

估算前提：不新增任何游戏内容，不重构现有系统，遇到卡点按第 12 节记录后跳过。
