# CrucibleCraft T23 完成计划 · 有界多方块批次

> 阶段：T23 · 30 个 canonical multiblock kind 行为分类 + 2–3 个代表结构实现
> 前置：T22.5 已关闭（`T22_5_READY`，2026-08-13）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
>
> **本阶段是第四阶段第一张真正"实现新玩家可见内容"的卡**——
> T20–T22.5 的 publication delta 分别是 0 / 4 / 3 / 0，T23 会真的加东西。

---

# §0 · 开工前必须先解决的一个冲突

**《第四阶段总体规划》和 T22.5 档案对 T23 的定义不一致：**

| 出处 | T23 是什么 |
|---|---|
| 第四阶段总体规划 T23 卡 | **有界多方块批次**（30 kind 分类 + 2–3 个结构） |
| T22.5 档案 §10 交接 | **翻译层接入**（A2 映射表 + A3 物品等价，1,002 行） |

**建议：T23 保持"多方块"，翻译层接入不进 T23。** 理由：

1. 第四阶段规划是阶段编号的权威，改编号会牵动 T24–T27 的引用
2. T22.5 的 C1 结论是 **`v1 = 0 行`**——那 1,002 行全部落在 post-1.0 portfolio，
   **不阻断 Beta**，没有理由抢在多方块前面
3. 两件事技术上毫无重叠，塞进一张卡违反"判据可证伪"

**处置**：1,002 行的翻译层接线登记为 open item（建议 **O-41**），
owner 写 post-1.0 portfolio，recheck point 定在 T27 freeze。
本计划 G1 有对应动作。

---

# §1 · 六条硬规矩（T21/T22 换来的，不重复解释）

**一 · 测量记录不可调整。** `*_sha256`、`elapsed_seconds`、`runs`、`*_passing`、
`missing`/`extra`/`unassigned`/`duplicate`、`result` 只能由那次运行写入。
对不上就重测，或标 `NOT_RECORDED` / `DEFERRED`。
推论：等式两边不能同源（`actual = expected − missing` 也算）；
布尔测量不写字面量；`0` 必须是数出来的，**不能用状态名消解**。

**二 · 期望值对不上，默认是实现有问题。** 历史 baseline 只读。

**三 · 流程步骤不进数据门禁。** `closure_policy.pending` 只放实质工作项。
"跑一次 record" 写进去就自指死锁。

**四 · status 是派生值。** builder 从自己的门禁推导，`check()` 重算比对。
**不要交给 `run_full_verification`**（T21 的循环依赖就是这么来的）。

**五 · 改共享文件后必须沿链重建。**
`rebuild_artifacts.py --keep-going` + `git diff`，**每张卡收尾都跑**。
T23 会新增方块/物品/模型/语言，这是自 T21 以来动静最大的一次。

**六 · 同一个 check 连续两次红就停下汇报。**

---

# §2 · 开工门禁

## G1 · 基线冻结与遗留项归属

- [ ] **T22 阶段档案补写**（T22.5 §10 已登记）。T22.5 已引用 T22 的分母与
      `cross_mod_compat` 词表，档案缺失会让 T23 找不到出处
- [ ] **T22.5 的 1,002 行口径矛盾解释一句**：
      B1 判 `out_of_scope 5,860`，C1 判 `out_of_scope 4,858 + 可解锁 1,002`，
      差值恰为 1,002。是"C1 从 out_of_scope 里重新切出可解锁"还是"取错源"，
      写明即可，不用改数
- [ ] **2 个 v1 blocker 定处置**：`anvil_bend_big` / `anvil_bend_small`
      为 `registered_zero_logical`。三选一——补弯曲配方 / 降级为预留槽位 /
      显式 deferred。**T26 Beta 前必须拍板，别让它漂到最后**
- [ ] **O-41 登记**：1,002 行翻译层接线，owner = post-1.0 portfolio
- [ ] **基线冻结**（T22.5 收盘值）：

```
publication : 18,882 logical / 16,657 eager / 2,225 lazy
RecipeMap   : 32          EMI configured : 24
GameTest    : 89          Java : 539      Python : 636
builders    : 74          logical 余量 : 2,118
multiblock  : 2 个 JSON structure（coke_oven、large_centrifuge）
```

- [ ] `t23_publication_baseline.json` 建立，`baseline: "T22.5"`

## G2 · readiness 骨架先行 ⭐

T21 撞过三轮 KeyError、T22 撞过一轮。**第一张卡就把完整形状建好，填占位也行。**

- [ ] `build_t23_readiness.py` + `t23_readiness_policy.json`
- [ ] `verify_full_verification_report.py` 的 T23 门禁键**同时写好**
- [ ] 门禁加前置键存在性检查，缺键给一句人话而非 traceback：

```python
missing = required_keys - t23_closure.keys()
if missing:
    errors.append(f"T23 readiness closure is missing keys: {sorted(missing)}")
    return errors
```

- [ ] 验收：手写 `status: T23_READY`，`check()` 必须判 stale（实测贴报错）

## G3 · 先算的数字（落纸后才动手）

T23 是第四阶段第一次真正增加注册对象，**形态爆炸的老规矩要用上**：

| 数字 | 为什么先算 |
|---|---|
| selected 结构数 × 每个结构的方块种类（controller / casing / port） | 注册对象总数 |
| 每种新方块的 blockstate / model / item model / loot / tag 文件数 | 生成资源增量 |
| 新增 en_us 键数、`zh_cn` 覆盖率影响 | §5.8 不倒退 |
| 新增 recipe（获取路线）的 logical / eager 增量 | 对 2,118 余量 |
| 结构扫描体积（方块数）× 每 tick 扫描频率 | T23d Load 轴 |
| 新增 GameTest 数（当前 89） | 收尾计数 |

> **参考**：Large Centrifuge 扫描体积 18 个结构位（15 item/fluid port +
> 2 energy port + 1 controller）。T23 若选 5×5×5 级结构，扫描体积会跳一个量级——
> **这个数必须在写第一行 Java 之前算完。**

---

# §3 · 执行卡

```text
A1  30 kind 行为需求分类          ← 无前置
      │
A2  selected 2–3 结构选定          ← 需 A1 完成，不得提前
      │
B1  schema 最小扩展（若需要）      ← 需 A2
B2  controller plugin 边界         ← 需 A2
      │
C1  逐结构实现（每结构一张卡）      ← 需 B1+B2
C2  生存获取与真实消费
C3  生命周期与异常路径
      │
D1  三轴收尾（含 Load 实测）
D2  沿链重建 + git diff
D3  单次 clean record
```

---

## A1 · 30 kind 行为需求分类

**当前 T13 分母**（`tools/t13_denominators/multiblock_kinds.json`）：

```
canonical : 30      raw_files : 44      excluded : 13
implemented : 2     post_t19 : 23       third_stage_deferred : 5
unclassified : 0
```

30 个 canonical id：

```
autoclave  bath  bedrock_drill  centrifuge✓  coagulator  coke_oven✓
crucible  crusher  cryo_distillation_tower  distillation_tower
electrolyzer  fermenter  fusion_reactor  implosion_compressor
large_boiler  large_dynamo  large_heat_exchanger  large_turbine_gas
large_turbine_steam  lightning_rod  logistics_core  matter_fabricator
mixer  oven  shredder  sluice  squeezer  tank_3x3x3  tank_5x5x5_metal
von_da_graagg
```

（✓ = 已实现：`centrifuge → large_centrifuge`、`coke_oven → coke_oven`）

**动作**：每个 kind 至少记录五组事实——

1. **source identity 与结构 family**（GT6 类文件 + 规范化理由）
2. **能否使用现有 schema v1**（`palette` + 显式 `offset` 列表 + `anchors`）；
   需要 aisle/repeat、动态端口、环境约束的单独标注
3. **是否需要专用 controller behavior**（事务 / UI / 环境交互）
4. **所需 RecipeMap、energy identity、输入获取路径、产品 consumer**
5. **分类**：`v1_required` / `post_1_0` / `out_of_scope` / `unclassified`，
   每条非 v1 带 reason / owner / replacement condition / recheck point

**⚠️ 硬规则（规划原文）**：
> 完成分类后才选择 2–3 个结构。**不得先写通用 controller，再寻找使用者。**

**验收**
- [ ] 30 / 30 有五组事实，`unclassified = 0`
- [ ] 分类依据是**类别定义**，不是"实现难度"（T22 犯过这个错）
- [ ] 现有 23 `post_t19` + 5 `third_stage_deferred` 的 disposition 复核并更新
- [ ] **publication delta = 0**（本卡不实现任何东西）

---

## A2 · selected 2–3 结构选定

**目标**：从 A1 的 `v1_required` 里选 2–3 个服务 v1 工业主链的结构。

**选择标准**（写进 artifact，不是口头判断）：

1. **服务已闭合的主链**——T18 的蒸汽/燃油链、T22 的石油链、T2 的矿处链
2. **输入可获取、产品有真实消费端**（双向证明，沿用 T22 的 operand proof）
3. **结构 geometry 能用 schema v1 表达**，或所需扩展在 B1 可控范围内
4. **两个结构之间有行为差异**——若都是同一种 processing controller，
   通用层就没被证明（T23c 要求"至少两个真实消费者"）

**候选提示**（仅供 A1 分类时参考，**不预先决定**）：
`large_boiler`（T18 蒸汽链的规模化）、`distillation_tower`（T22 石油链的规模化）、
`oven` / `crucible`（炼钢）、`tank_3x3x3`（流体储运）。

**验收**
- [ ] selected 数量 2–3，每个有上述四条依据
- [ ] 未选中的 `v1_required`（若有）有 reason + recheck point
- [ ] 选择在 A1 完成之后发生（提交顺序可查）

---

## B1 · Schema 最小扩展

**当前 schema v1** 的形状（`large_centrifuge.json`）：

```json
{"schema_version": 1,
 "palette": {"P": {"type":"port","block":"...","port":"item_fluid"},
             "E": {"type":"port","block":"...","port":"energy_input"},
             "C": {"type":"controller","block":"..."}},
 "structure": [{"offset":[-1,0,0],"predicate":"P"}, ...],
 "anchors": {...}, "source": {...}}
```

**每个位置一条显式 offset 记录**——18 个位置就是 18 条。
5×5×5 会是 125 条，这时才需要 aisle/repeat。

**规则（规划原文）**
- 只有 selected 结构的 source geometry **无法表达**时才加 schema
- schema、loader、validator、preview/debug 输出使用**同一语义**
- 未知 predicate / plugin / orientation / port role **fail closed**
- **继续禁止每个结构一个专用 `*Structure` Java parser**

**验收**
- [ ] 若未扩展 schema，写明现有 v1 足够的理由
- [ ] 若扩展，`schema_version` 递增，旧结构（coke_oven / large_centrifuge）
      **零改动即可继续加载**（实测）
- [ ] 未知值 fail closed 有变异测试
- [ ] loader / validator / preview 三处语义一致，有交叉测试

---

## B2 · Controller plugin 边界

**规则（规划原文）**
- 结构数据只描述**值与约束**；事务、UI、环境、特殊机器行为由**显式白名单 plugin**
- plugin id 稳定、可持久化、**可 quarantine**
- 每个 plugin 至少被一个 selected controller 消费
- **泛型 processing controller 只有在两个真实 selected consumer 共享相同行为后
  才能抽取**
- **port 到 shared host 的供应量不能按物理方块数重复计算**

> ⚠️ 最后一条是 T15e 的教训，原文值得抄下来：
> Large Centrifuge 有 **15 个物理 item/fluid port**，但它们**全部桥接同一个
> host inventory**，matcher 从 host 只取得 **item / fluid 各 1 份 input supply**，
> presence cap 12 **不触发**。
> T12 交接时曾误记 expected-16（把 controller 位算成 port）并写入无 parser 来源的
> "六份 runtime supply"——**新结构不要重犯**。

**验收**
- [ ] plugin 白名单落盘，每个 id 有稳定性与 quarantine 测试
- [ ] 每个 plugin 有 ≥1 个 selected consumer（零消费者的不许存在）
- [ ] 若抽取了泛型 controller，两个 consumer 的共享行为有证据
- [ ] port → host supply 的计数有独立断言，**不按物理方块数**

---

## C1 · 逐结构实现（每结构一张独立卡）

**每个 selected 结构走同一套六条**：

- [ ] 结构 JSON 落盘，geometry / predicate / port **逐项对 source 双向等价**
- [ ] controller 方块 + casing + port 方块注册
- [ ] blockstate / block model / item model / loot / 挖掘 tag 齐全
- [ ] `en_us` 齐全；模型是可接受的生成/继承结果，**无 placeholder texture**
- [ ] `zh_cn` 真实覆盖率不低于本卡开工值（§5.8）
- [ ] **收尾跑 `rebuild_artifacts.py --keep-going` + `git diff`**

**fidelity 铁律（规划原文）**：
> **一个结构通过不能代表其他 29 个结构。**
> 每个 selected 结构独立给 geometry/predicate/port 的双向等价证据。

---

## C2 · 生存获取与真实消费

- [ ] 每个结构的 controller / casing / port **全部可生存合成**，`unreachable = 0`
- [ ] 用 T22 的 operand 可达性闭包验证（`t21_operand_reachability.py` 或其通用版）
- [ ] 每个结构有**真实产品消费端**——不是陈列
- [ ] 若某结构的产物暂无消费端，**不许发布**（T22 范围之外的原文）

---

## C3 · 生命周期与异常路径

规划 T23d Closure 要求这六种可执行：

- [ ] **formation**：正确摆放后成型，错误摆放不成型且有可观测原因
- [ ] **拆除**：拆一块即解体，内容物不丢失
- [ ] **堵塞**：输出满时暂停而非 void
- [ ] **断能**：断电/断热保留进度或按声明策略失败，**失败方向是损失不是复制**
- [ ] **reload**：重载后结构与进度恢复
- [ ] **存档**：identity mismatch 走 quarantine，库存可恢复
- [ ] **升级路径**：若结构有档位，迁移路径明确

每种一个 GameTest，dedicated server 上跑。

---

## D1 · 三轴收尾

**Closure**
- [ ] 30 kind 行为需求 `unclassified = 0`
- [ ] selected 结构：方块/物品/模型/语言/loot/recipe 获取 + 真实消费闭合
- [ ] 六种生命周期路径可执行

**Fidelity**
- [ ] selected source geometry / predicate / port 逐项双向等价
- [ ] CC plugin 行为与 source 行为或 `DESIGN_POLICY` 边界明确
- [ ] **不宣称一个结构代表其余 29 个**

**Load** ⭐ 本轴 T23 最容易漏
- [ ] structure validation、port scanning、network sync、ticking operation count
      **各有上界**，且是**实测**（`MEASURED`，不是推断）
- [ ] **不把 chunk/layout 变化造成的扫描成本藏在 wall-clock 平均里**——
      要给最坏情况，不是均值
- [ ] 新增 recipe / map publication **按 family 单独记账**
- [ ] 全局 logical/eager/lazy 更新，delta 登记进 `t23_publication_baseline.json`，
      确认仍在 21,000 内

**readiness**
- [ ] `status` 由门禁派生，`check()` 重算一致
- [ ] `closure_policy.pending` 只含实质工作项
- [ ] `expected` 与 `actual` 来自两条独立路径
- [ ] `gametest_passing` 由真实运行填，未跑前为 `null`

---

## D2 · 沿链重建与 diff 复核

```bash
python tools/rebuild_artifacts.py --keep-going
git diff --stat
```

- [ ] 历史 baseline（T16–T22.5 各份 publication baseline、
      `t12a_machine_readiness.json` 历史结论）**未被修改**
- [ ] 已关闭阶段计数未被回写
- [ ] 重建列表写进交付账

---

## D3 · 单次 clean record

```bash
python tools/run_python_tests.py --suite closure
python tools/run_python_tests.py --suite source-replay
.\gradlew.bat test
.\gradlew.bat runGameTestServer
python tools/run_full_verification.py --record --new-session
```

- [ ] 一次通过，未重跑掩盖失败
- [ ] `T23_READY` 由 session 触发 builder 派生，**不手写**
- [ ] **自检**：`required_tests` 应 **> 89**；仍是 89 说明没真跑

---

# §4 · 风险

| 风险 | 说明 |
|---|---|
| **先写通用层再找使用者** | 规划明文禁止。A1 未完成前不许开 A2，提交顺序要可查 |
| **一个结构代表全部** | fidelity 铁律。每个 selected 独立给等价证据 |
| **port supply 重复计数** | T15e 的坑：15 个物理 port → 1 个 shared host → matcher 各 1 份供应 |
| **Load 轴用均值掩盖** | 结构扫描成本随 chunk/layout 变化，必须给最坏情况 |
| **注册对象扩大本地化债** | 这是自 T21 以来第一次大批新增方块，§5.8 逐条把关 |
| **分类按难度而非定义** | T22 犯过一次。A1 的 `post_1_0` 必须有类别依据 |

---

# §5 · 回报格式

```text
卡片：A1 / B2 / C1-<结构名> / ...
状态：DONE / BLOCKED / PARTIAL
验收项：逐条 PASS / FAIL，FAIL 附实际值
交付物：文件路径
rebuild + diff：跑了 / 未跑；历史 baseline 是否被动
顺带发现：（只记录不处理）
```

**一次只做一张卡。** 相邻问题记进"顺带发现"，不要就地修。

---

# §6 · 数字速查

| 项 | 值 | 来源 |
|---|---|---|
| canonical multiblock kind | 30（implemented 2 / post_t19 23 / third_stage_deferred 5） | `t13_denominators/multiblock_kinds.json` |
| raw → canonical | 44 → 30（excluded 13） | 同上 |
| 已实现结构 | `coke_oven`、`large_centrifuge` | `multiblock_structures/` |
| Large Centrifuge 结构位 | 18 = 15 item/fluid port + 2 energy + 1 controller | T15e |
| matcher input supply | item 1 份 / fluid 1 份（共享 host），cap 12 不触发 | T15e |
| schema | v1：`palette` + 逐位置 `offset` + `anchors` + `source` | `large_centrifuge.json` |
| publication（T22.5 收盘） | 18,882 / 16,657 / 2,225 | `t22_publication_baseline.json` + T22.5 |
| logical 硬上限 / 余量 | 21,000 / **2,118** | T14 |
| RecipeMap / EMI | 32 / 24 | 同上 |
| 基线测试数 | GameTest 89 / Java 539 / Python 636 | T22.5 record |
| builders | 74 | `verification_builder_policy.json` |
| 遗留 v1 blocker | `anvil_bend_big`、`anvil_bend_small`（零 logical） | T22.5 C0 |
| 遗留 open item | 1,002 行翻译层接线（建议 O-41，post-1.0） | T22.5 C1 |
