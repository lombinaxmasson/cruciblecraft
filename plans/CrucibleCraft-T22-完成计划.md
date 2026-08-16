# CrucibleCraft T22 完成计划（分项独立版 · 修订）

> 阶段：T22 · 石油化工全量与纵深
> 前置：T21 已关闭（`verified_on 2026-08-11`、五步全 PASS、85 GameTest、
> `T21_READY` 由 session 派生）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
>
> 本版相对初稿的改动：预算判断改用 T21 产物中的真实数字（初稿写的
> "几百到几千行"没有来源，是错的）；新增 T22.5 提案；新增 translation
> blocker 登记表；新增 Beta 措辞收紧建议。

---

# Part 0 · 从 T21 带过来的六条硬规矩

T21 的实质工作两天够用，多出来的时间几乎全花在下面六件事上。**开工前先读完。**

**一 · 测量记录不可调整。**
记录"某次运行观测到了什么"的字段只能由那次运行写入：`*_sha256`、
`elapsed_seconds`、`runs`、`drift_files`、`*_passing`、
`missing`/`extra`/`unassigned`/`duplicate`、`result: PASS`。
对不上就重新测量，或标 `NOT_RECORDED` / `DEFERRED`。
推论：等式两边不能来自同一表达式；布尔测量不许写字面量；`0` 也是测量结果。

**二 · 期望值对不上，默认是实现有问题。**
历史 baseline（`captured_before` / `baseline: "TXX"` / 已关闭阶段计数）一律只读。

**三 · 流程步骤不进数据门禁。**
`closure_policy.pending` 只放实质工作项。"跑一次 full verification" 写进去就自指死锁。
`reason` 照抄 T20/T21 措辞：
> "T22a-d implementation and currentness gates are complete;
> repository-wide verification is bound separately by the full snapshot report."

**四 · status 是派生值，不是授予值。**
`build_t22_readiness.py` 从自己的门禁推导 status，`check()` 重算比对。
不要让 `run_full_verification` 拥有 status——那会造成循环依赖。

**五 · 改共享文件后必须沿链重建。**
```bash
python tools/rebuild_artifacts.py --keep-going
git diff --stat      # 历史 baseline 被改动就是 bug
```
**每张卡结束时都跑**，不要攒到最后。

**六 · 同一个 check 连续两次红就停下汇报。**
前两次通常是刷新问题，第三次尝试往往开始改被检测的对象。

---

# Part 1 · 开工门禁

## G1 · T21 归档与基线冻结

- [ ] T21 阶段档案写完，草稿标记清除
- [ ] `t22_publication_baseline.json` 建立，`baseline: "T21"`，
      totals = **18,879 / 16,654 / 2,225**
- [ ] 确认 T14 delta 账本能容纳多个 delta（`16,650 + Σ registered_delta`，
      T21 已贡献 +4）
- [ ] 冻结基线：32 RecipeMap、EMI configured 24、GameTest 85、Java 539、
      Python 575、datagen tree `6878b9c8…` / 2,558 files

## G2 · readiness 骨架先行 ⭐ 最省时间的一条

T21 最大的浪费是 readiness 结构最后才成型，门禁键缺失导致 KeyError 连撞三轮，
每轮 8 分半。**T22 反过来：第一张卡就把完整形状建好，内容填占位也行。**

- [ ] `build_t22_readiness.py` + `t22_readiness_policy.json`
- [ ] `verify_full_verification_report.py` 的 T22 段门禁**同时写好**，
      并加前置键存在性检查：

```python
required = {...}                       # T22 closure 要求的全部键
missing = required - t22_closure.keys()
if missing:
    errors.append(f"T22 readiness closure is missing keys: {sorted(missing)}")
    return errors                       # 提前返回，避免 KeyError 爆栈
```

- [ ] 结构照 T21 最终形状（status=null、pending_stages 四项、
      `currentness.full_verification = BOUND_TO_FULL_VERIFICATION_REPORT`）

**验收**：手写 `status: T22_READY`，`check()` 必须判 stale（实测贴报错）。

## G3 · 先算的数字（已知部分 + 待算部分）

### 已知（来自 T21 产物，可直接引用）

| 项 | 值 | 来源 |
|---|---:|---|
| T21 收盘 publication | 18,879 / 16,654 / 2,225 | `full_verification_report.json` |
| logical 硬上限 | 21,000 | T14 |
| **当前余量** | **2,121** | 21,000 − 18,879 |
| `petroleum_t22` units | **63** | `t21_template_denominator.json` |
| `petroleum_t22` 展开行 | **1,071** | 同上 |
| 逐 map 分布 | mixer 49 / centrifuge 9 / smelter 3 / electrolyzer 1 / compressor 1 | 同上 |

**1,071 装得进 2,121，发布后仍余约 1,050 行。**
初稿说的"预算冲击"不成立——那个数字是我拍的，撤销。

### 待算（T22a 的真正工作）

T21 的模板分母只覆盖 **10 张 map**：

```
electrolyzer, centrifuge, mixer, autoclave, bath,
drying, compressor, smelter, roaster, assembler
```

**`distillery` 不在其中**，而石油主链必走 distillery。T11 关闭时的数字是
**1,517 条 distillery row**（当时只选了固定行 872），另有 10,236 条 generifier
row（选了 553）。这些行里有多少属于石油 v1，**目前没有数**。

- [ ] distillery 及其他 T21 未覆盖 map 中的石油行数（按 family 分组）
- [ ] 与 1,071 相加后的总增量，对照 2,121 余量
- [ ] 新增 fluid / material / 容器形态数
- [ ] 新增注册对象的 en_us / 模型 / zh_cn 增量（5.8 不倒退）
- [ ] 新增 GameTest 数（当前 85）

> **若总增量逼近或超过 2,121**，在 T22a 就决定：部分 family 走 lazy/on-demand，
> 或按 T14 协议实测后调整 ceiling（四类实测齐全才能改数字）。
> **不要发布到一半才发现。**

---

# Part 2 · 执行卡

```text
第一梯队（无前置，可并行）
  A1  petroleum exact denominator（含 T21 未覆盖 map）
  A2  family 前置能力盘点
  A3  载荷投影与策略

第二梯队（需 A1+A2+A3）
  B1  family 前置补齐
  B2  首个 family 全量投影纵切

第三梯队
  B3  逐 family 批量投影
  B4  逐 family load 策略与 delta 登记

第四梯队
  C1  三种下游产物与真实消费端
  C2  玩家循环 GameTest（dedicated server）
  C3  守恒 / 堵塞 / 断电 / reload / 存档

收尾
  D1  三轴账与 readiness 派生
  D2  沿链重建 + git diff
  D3  单次 clean record
```

---

## A1 · petroleum exact denominator

**目标**：石油化工 source-row 全集，逐行分类，`unclassified = 0`。

**动作**
1. 分母 = **`petroleum_t22` 63 units / 1,071 行**（T21 已分类）
   **＋ T21 十张 map 之外的石油行**（distillery 为主）。
   两部分都要有来源，不另扫私有候选。
2. 建 `tools/build_t22_petroleum_denominator.py`，带 schema version、
   source digest、生成命令、currentness check。
3. 分类词表：`v1_required` / `ordinary_optional` / `post_1_0_*` / `out_of_scope`。
   非 v1 行必须有 reason / owner / replacement condition / recheck point。
4. **排除只允许两类**：明确依赖 nuclear/fusion/plasma、G10 或其他 post-1.0 subsystem。
   "暂时做不了"不是排除理由，是 B1 的工作项。
5. **口径纪律**：`unit` 与 `row` 是两个单位。mixer 那 49 个 unit 是模板
   （18.6 行/unit），其余是裸行。报告里任何数字必须带单位名和来源产物名。

**验收**
- [ ] `unclassified = 0`
- [ ] expected set 与生产 normalizer 分开构造
- [ ] 变异测试：改任一行分类，门禁变红
- [ ] compact receipt 进 ordinary CI，全量 replay 归 `--source-replay` 独占
- [ ] artifact 体积上界明确，且**不是"当前值 × 2"**

---

## A2 · family 前置能力盘点

每个 v1 family 落一张表，五项缺一不可：

| 项 | 要求 |
|---|---|
| source row 全集 | 行数 + 行 key |
| CC material / fluid 映射 | source-backed / source-derived / 已登记 DESIGN_POLICY |
| 执行机器 | 已实现 或 本卡内补齐（B1） |
| energy identity | RU/KU/EU/HU/STEAM，不折叠 |
| **真实消费端** | 至少一个，且 **operand 可达** |

**消费端必须双向证明**（T21-D 的教训）：

```
consumer proof : 产物在 Beta 主链有真实消费端
operand proof  : 全部 operand 在 available-identity 闭包内可达
```

用 `tools/t21_operand_reachability.py`（建议改名为通用版）验证。
**partially-reachable 的 family 一律先拆**——不允许"4 个成员里 2 个可达"就发布。

**验收**
- [ ] 每个 v1 family 五项齐全
- [ ] `--check` 对全部 v1 family 返回 `unreachable = 0`
- [ ] 缺前置的 family 有 B1 工作项，不允许无 owner deferred

---

## A3 · 载荷投影与策略

**目标**：发布任何一行之前，账先算清。

**动作**
1. 用 A1 的行数算全部 v1 family 的 logical 增量预测。
2. 与 2,121 余量对比：
   - 装得下 → immediate
   - 装不下 → 按使用分布决定哪些走 lazy/on-demand
   - 确需抬 ceiling → **必须有 T14 协议的四类实测**
     （server reload/index、client re-expansion、峰值内存、steady-state lookup）
3. **高频主链反应不许因为 lazy 总预算充足就默认扔进 lazy。**

**参考**：T14 的 20× Hybrid 实测为 server reload 11.041 ms、dedicated client
11.455 ms、lookup p95 946 ns、retained 21,669,656 B、JFR allocation 1,271,728 B
——全部远低于 hard ceiling。说明 21,000 更可能是**政策上限而非容量上限**，
但改它必须有本阶段的实测，不能引用 T14 的旧数。

**验收**
- [ ] 预测区间落盘，含每 family 策略与依据
- [ ] 若调 ceiling，四类实测齐全并标 `MEASURED`
- [ ] 决策写进《总体规划》9.5 或等价位置

---

## B1 · family 前置补齐

- 缺机器 → 按 T16/T17 的 Kind/Tier 架构接档，不新造第二套
- 缺 energy identity → 复用 T18 转换边界，不折叠 RU/KU/EU/HU/STEAM
- 缺容器 → 复用 T10 的 cell / 便携罐 allowlist
- 缺 fluid identity → 走 T11 的 source-backed 路线

**验收**
- [ ] 新增注册对象有 en_us、模型是可接受的生成/继承结果、无 placeholder texture
- [ ] `zh_cn` 真实覆盖率不低于本卡开工值
- [ ] 新增对象生存获取可达（`unreachable = 0`）
- [ ] **收尾跑 `rebuild_artifacts.py --keep-going`**（这张卡必然改共享文件）

---

## B2 · 首个 family 全量投影纵切

**目标**：选一个规模适中、前置齐全的 family，把"全量等价"走通一遍，作为后续模板。

**动作**
1. 独立 expected ↔ authored/expanded **全集双向相等**，不用抽样。
2. 保留 exact input/output amount、container action、chance、duration、EU/t、
   special value、顺序与 fallback。
3. 双 multiset 哈希（照 T21 Mixer），`expected` 与 `actual` 来自**两条独立路径**。
4. 变异测试：改任一字段，门禁变红。
5. **在这张卡就验证 delta 累加**：`16,650 + T21的4 + 本family的N` 必须等于
   GameTest 里的 eager 断言。不要等到第五个 family 才发现账本处理不了多个 delta。

**验收**
- [ ] `missing`/`extra`/`membership_unassigned`/`membership_duplicate` 全 0
- [ ] 双 multiset 哈希相等且独立计算
- [ ] publication delta 登记进 `t22_publication_baseline.json`
- [ ] `rebuild_artifacts.py` + `git diff`

**不同 family 的压缩比与策略不得互相外推。**

---

## B3 · 逐 family 批量投影

每个 family 一个独立工作单元，固定六条收尾：

- [ ] 全集双向等价，四个计数为 0
- [ ] operand proof + consumer proof 齐全
- [ ] publication delta 登记，全局 totals 更新
- [ ] load projection PASS，策略有依据
- [ ] 本地化 / 模型不倒退
- [ ] `rebuild_artifacts.py --keep-going` + `git diff`

**范围之外**：没有当前消费端的化学品陈列。

---

## B4 · 逐 family load 策略与 delta 登记

- [ ] 高频路径的 lookup candidate、transaction allocation、sync 进入测量
- [ ] 每次发布后更新全局 logical/eager/lazy，确认仍在 hard gate 内
- [ ] delta 累加，`16,650 + Σ delta` 等于 GameTest eager 断言
- [ ] 数字标 `MEASURED`；推断值标 `STATIC_INFERENCE` 并说明

---

## C1 · 三种下游产物与真实消费端

- 入口复用 T9/T11 已关闭的 `crude_oil` / `natural_gas` identity 与抽取语义
- 三种产物各自连接机器 / 物流 / 燃料 / 润滑 / 聚合物前体 或其他实际 consumer
- **不许假 void water / CO₂ / lubricant**（T18 已定边界）

**⚠️ 保真红线**：T18 的 `O37_CLOSED_PERMANENT_DESIGN_POLICY` 与 T11 的
identity 边界**不得被偷换成 source direct binding**。material 9852 只保留
`SOURCE_MATERIAL_LAYER_ONLY` 角色。石油化工正是最容易手滑把
`liquid_medium_oil` 直接绑上去的地方。

**验收**
- [ ] 三种产物各有真实消费端，非陈列
- [ ] identity 边界未被改写，T9 identity / publication delta 独立记账
- [ ] 任何 CC 平衡调整单独标 `DESIGN_POLICY`

---

## C2 · 玩家循环 GameTest

- 输入获取 → 连续处理 → 容器返回 → 管道运输 → 消费，端到端
- **不要用 `loadRecipeInputs()` 直接塞成品**——那绕过获取路径，
  T21 的 gunpowder 就是这样漏掉两条不可达行的
- 至少一个自动化循环跑满一轮

**验收**
- [ ] `runGameTestServer` 全绿，数量与账目一致
- [ ] 获取路径由真实配方走通，不是直接注入
- [ ] GameTest 总数更新（85 + 本卡新增）

---

## C3 · 守恒 / 堵塞 / 断电 / reload / 存档

- [ ] 五种异常路径各有 GameTest
- [ ] 失败方向是损失不是复制；source-first 提交顺序不颠倒
- [ ] 守恒在 reload 与重载后保持
- [ ] 存档 identity mismatch 走 quarantine，库存可恢复

---

## D1 · 三轴账与 readiness 派生

**Closure**
- [ ] petroleum source-row 全集 `unclassified = 0`
- [ ] 全部非 post-1.0 行已发布，petroleum `v1_required_remaining = 0`
- [ ] 每个发布 family 的 identity / machine / energy / storage 与关键 consumer 闭合
- [ ] 三种下游产物被真实消费
- [ ] 排除行有 disposition，不以抽样代表全集

**Fidelity**
- [ ] 全部发布 rows 按 family 双向等价
- [ ] Raw Oil / `crude_oil` / `natural_gas` 边界未被偷换
- [ ] CC 平衡调整单独标 `DESIGN_POLICY`

**Load**
- [ ] 每 family 独立 projection + 策略依据
- [ ] 高频路径 lookup / allocation / sync 已测量
- [ ] 全局 publication、reload/index、lazy cache 在 hard gate 内

**readiness**
- [ ] status 由门禁派生，`check()` 重算一致
- [ ] `closure_policy.pending` 只含实质工作项
- [ ] 全部门禁键存在（G2 已建，此处只填值）
- [ ] `gametest_passing` 由 C2 真实结果填

---

## D2 · 沿链重建与 diff 复核

- [ ] 历史 baseline（T16–T21 五份 + t12a 历史结论）未被修改
- [ ] 已关闭阶段计数未被回写
- [ ] 重建列表写进交付账

## D3 · 单次 clean record

```bash
python tools/run_python_tests.py --suite closure
python tools/run_python_tests.py --suite source-replay
.\gradlew.bat test
.\gradlew.bat runGameTestServer
python tools/run_full_verification.py --record --new-session
```

- [ ] 一次通过，未重跑掩盖失败
- [ ] `T22_READY` 由 session 触发 builder 派生，不手写

---

# Part 3 · T22.5 提案（T22 之后、T23 之前）

## 为什么需要这张卡

T21 关闭时留下三组数字，**目前没有任何一张卡拥有它们**：

**一 · `ordinary_optional` = 89,643 units / 146,841 rows**

主要压在 bath（59,722）与 smelter（21,614）。对着 21,000 的 logical 硬上限，
这批永远不可能全部发布。它不阻断 v1（见下），但"GT6 搬完"的口径必须有明确答复。

**二 · 九张 map 从未做过模板分析**

| map | units | rows | rows/unit | 有模板 |
|---|---:|---:|---:|---:|
| mixer | 3,249 | 60,447 | **18.60** | 3,249 |
| bath | 59,722 | 59,722 | **1.00** | **0** |
| smelter | 21,614 | 21,614 | **1.00** | **0** |
| 其余七张 | 5,058 | 5,058 | 1.00 | 0 |

历史上只造过两个提取器：`gt6_extruder_templates.py`（2,782 → 20，**139:1**）与
`gt6_mixer_templates.py`（64,245 → 3,414，**18.8:1**）。bath 那种规模几乎不可能
不是矩阵生成的。

**但要清楚一件事**：模板压缩减少的是 **authored / datapack entries**，
不是 **logical rows**。extruder 是 20 authored ↔ 2,782 logical，那 2,782 照样
全额计入 logical 总账。所以压缩帮不上 21,000 这个顶。

**三 · translation blocker 已分类但无 owner**

`t21_source_denominator.json` 的 `rejection_summary`（针对 45,353 条
input-touch row）：

| 阻塞原因 | 行数 | 占比 | 建议 owner |
|---|---:|---:|---|
| **`fluid_mapping`** | **30,437** | **71.4%** | 流体 identity 批量导入（T11 路线） |
| `chemical_fluid_state` | 3,646 | 8.5% | 流体状态语义 |
| `item_mapping` | 3,068 | 7.2% | 形态映射（T10 路线） |
| `item_registration` | 2,508 | 5.9% | 形态注册（T10 路线） |
| `machine_shape` | 2,039 | 4.8% | 机器能力（T16/T17 路线） |
| `external_item` | 953 | 2.2% | 多半 out-of-scope（第三方物品） |
| `item_stack` | 6 | 0.0% | — |
| `energy` | 1 | 0.0% | — |

**71% 集中在一个原因上**。这意味着不是"四万行各有各的难处"，而是一个子系统缺口
撑起了七成——一次聚焦的流体 identity 导入可能就把三万行从 blocked 变成 translatable。

（口径提醒：这是 45,353 条 input-touch 子集的统计，不是全部 151,433 行。
全集翻译率没人算过。）

## T22.5 的形状：只出结论，不搬配方

- **a · 统一 unit 口径**
  mixer 模板与其余九张裸行不能混数。分母表按 `template_unit` / `raw_row`
  分列，任何汇总数字带单位名。
- **b · shape 分析（不发布）**
  bath / smelter / 其余七张跑一遍模板提取，只产出压缩比与 family 结构结论，
  不进 publication。
- **c · blocker 派 owner**
  八类各写 owner、replacement condition、recheck point，登记为 open item
  （建议 **O-40**）。
- **d · 口径收紧**
  见下节。

**载荷 ceiling 重测可以往后放。** `ordinary_optional` 不进 v1 的话，
21,000 在 v1 之前不会被顶破——T22 之后还剩约 1,050 行余量。真要搬 bath/smelter
是 1.0 之后 portfolio 的事，那时再按 T14 协议重测不迟。

## 一处措辞需要收紧

《第四阶段总体规划》0.2 已明确：

> **明确不阻断 v1.0**：……一次性投影全部 95 个 RecipeMap；
> "GT6 720,841 行全部移植"这一产品声明

v1.0 契约也写着"不表示 GT6 或 GT6U 全量移植"，判据一直是 `v1_required = 0`。
T21 以 `v1_required_remaining: 0` 关闭、89,643 units 不发布，**完全符合契约**。

**但 Beta 契约那句会打架**：

> T21 的普通化学与 T22 的石油化工在明确排除 post-1.0 subsystem 后**完整发布**

"普通化学完整发布"与"`ordinary_optional` 不发布"字面冲突。建议改成：

> T21 的普通化学与 T22 的石油化工中，**`v1_required` 全部发布**；
> `ordinary_optional` 属于 1.0 后 portfolio，逐类登记 owner 与 replacement condition。

这样 T26 的 Beta 门禁判起来没有歧义。**建议在 T26 之前改完。**

---

# Part 4 · 风险预判

| 维度 | T21 | T22 |
|---|---|---|
| publication delta | +4 | 1,071 + distillery 未知量 |
| 余量 | — | 2,121，大概率够 |
| 新增注册对象 | 0 | fluid / 容器 / 可能有机器 |
| family 数量 | 1 | 多个 |
| 前置完整度 | 高 | 有缺口（B1 补） |

**最大的三个风险**

1. **A1 的"待算部分"是唯一的未知量。** distillery 里有多少石油 v1 行，
   现在没有数。这是 A1 的第一优先，不是 A3。
2. **B1 补前置会改运行时 Java 和材料定义**——正是 T21 引发约 50 个产物级联的动作。
   每张卡收尾就跑 `rebuild_artifacts.py`，不要攒。
3. **delta 账本第一次要处理多个 delta。** 在 B2（首个 family）就验证累加，
   不要等第五个。

---

# Part 5 · 回报格式

```text
卡片：A1 / B2 / ...
状态：DONE / BLOCKED / PARTIAL
验收项：逐条 PASS / FAIL，FAIL 附实际值
交付物：文件路径
rebuild + diff：跑了 / 未跑；diff 中历史 baseline 是否被动
顺带发现：（只记录不处理）
```

**一次只做一张卡。** 看到相邻问题记进"顺带发现"，不要就地修。

---

# 附录 · 真实数字速查

| 项 | 值 |
|---|---|
| T21 收盘 publication | 18,879 logical / 16,654 eager / 2,225 lazy |
| logical 硬上限 / 当前余量 | 21,000 / **2,121** |
| eager soft / hard | 18,000 / 21,000 |
| lazy soft / hard | 16,000 / 56,000 |
| datapack soft / hard | 6,000 / 6,600 |
| `petroleum_t22` | 63 units / **1,071 rows** |
| `ordinary_optional` | 89,643 units / 146,841 rows |
| `already_covered` | 129 units / 145 rows |
| `post_1_0_g10` / `post_1_0_nuclear` | 2,774 / 521 units |
| T21 模板分母覆盖 map | 10（**不含 distillery**） |
| T11 distillery / generifier | 1,517 rows（选 872）/ 10,236 rows（选 553） |
| translation blocked（input-touch 子集） | 42,658，其中 `fluid_mapping` 30,437 |
| 基线 | 32 RecipeMap / EMI 24 / GameTest 85 / Java 539 / Python 575 |
| datagen tree | `6878b9c8…` / 2,558 files |
