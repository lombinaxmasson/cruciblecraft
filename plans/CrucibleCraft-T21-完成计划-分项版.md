# CrucibleCraft T21 完成计划（分项独立版）

> 用途：T21 收尾工作的**可委托执行**版本。每张卡自带输入、动作、验收和交付物，
> 单独完成、单独验收，不需要执行者理解 T21 全貌，也不需要在全部完成后再做一次
> 整体人工复查。
>
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 当前状态：T21 进行中。档案中已写入的 `T21_READY` 为草稿，**不是关闭证据**。
> 基线快照：32 RecipeMap、18,875 logical / 16,650 eager / 2,225 lazy、EMI 24。

---

## 0. 给执行者的通用规矩

在动任何一张卡之前先读这一节。这些规矩优先于卡片内的具体步骤；卡片和规矩冲突时，
以规矩为准并回报。

1. **一次只做一张卡。** 卡与卡之间不要顺手改。看到相邻问题就记进卡片的「顺带发现」
   一栏，不要就地修。
2. **不许改期望值去迁就实现。** 任何 `expected` / `readiness` / 分母 JSON 出现不匹配，
   默认结论是**实现或 currentness 链有问题**，不是期望写错了。确实要改期望，必须
   在卡片交付里单独写出理由和固定来源。
3. **raw / cache 缺失只能显式 SKIP，不得冒充 PASS。**
4. **`READY` 不许手写。** 任何文档里的 `T21_READY` 只能由验证流程派生（见 T21-A）。
5. **数量单位不可混写。** `source_fact` / `authored_rule` / `datapack_file` /
   `logical_row` / `eager_row` / `lazy_row` 是六种不同的东西。报告里写数字必须带
   单位名和来源产物名。
6. **每张卡的回报格式**（贴回来即可，不需要写散文）：

```text
卡片：T21-x
状态：DONE / BLOCKED / PARTIAL
验收项：逐条 PASS / FAIL，FAIL 附实际值
交付物：文件路径列表
顺带发现：（可为空，只记录不处理）
执行命令与输出：（关键片段）
```

---

## 1. 依赖图

```text
第一梯队（无前置，可并行，随便谁先做）
  T21-A  撤销草稿 READY 并锁死生成机制
  T21-B  GameTest 计数差额消歧
  T21-C  operand 可达性工具落地
  T21-G  compact artifact 体积审计
  T21-H  59 builder clean-checkout 验证
  T21-J  T16 / T17 currentness 修复

第二梯队（需要 T21-C）
  T21-D  coal_coke / charcoal 桥接决策与实施
  T21-E  partially-reachable 拆分规则入 policy

第三梯队（需要 T21-D + T21-E）
  T21-F  Beta seed 分层与必要性闭包重算
  T21-I  gunpowder family 隔离 load 实测

第四梯队（需要以上全部 DONE）
  T21-K  dedicated GameTest server 全量执行
  T21-L  source-replay + closure
  T21-M  单次 clean full verification 与文档同步
```

**硬约束**：T21-M 是唯一允许写 `T21_READY` 的卡，且只能跑一次成功的 record。
若 T21-M 失败，回到失败项对应的卡重做，**不要在 T21-M 内就地修**。

---

## T21-A · 撤销草稿 READY 并锁死生成机制

**为什么**：档案和 readiness 里已经写了 `T21_READY`，但完整验证从未执行。这个状态
会被下游 currentness 链当成真值消费。当前文档同步和最终验证挤在同一个工作单元里，
是这次提前写入的直接成因。

**前置**：无。

**输入**
- `CrucibleCraft-阶段档案-T21.md`
- `CrucibleCraft-第四阶段总体规划.md`
- `tools/t21_readiness.json`

**动作**
1. 把 `tools/t21_readiness.json` 的 status 字段**整体删除**，不要改成
   `T21_IN_PROGRESS` 之类的新值——半旧半新的字段比缺字段更危险，下游可能已经在
   读它。同时在文件里加 `"status_owner": "run_full_verification"`。
2. 在 readiness builder 里加一条断言：status 字段只接受由验证会话写入；任何其他
   写入路径直接抛错。
3. 档案与第四阶段规划里所有 `T21_READY` 字样改为「T21 进行中，关闭证据未生成」。
4. 全仓搜索 `T21_READY`，确认没有第二处硬编码。

**验收**
- [ ] `grep -rn "T21_READY" .` 只在验证流程代码和本计划中出现，不在任何 readiness
      或档案的状态字段里
- [ ] 手动往 `t21_readiness.json` 写 status 会被 builder 拒绝（实测一次，贴报错）
- [ ] 下游读 status 的地方在字段缺失时 fail closed，不是静默当成未就绪

**交付物**：修改后的 readiness、档案、规划；builder 断言的代码位置。

---

## T21-B · GameTest 计数差额消歧

**为什么**：源码树里 `@GameTest` 实际是 **86** 个。T19/T20 基线是 83，T21 新增 2
（carbon family + gunpowder）应为 85。多出的 1 个来源不明——要么是别的卡顺带加了没
登记，要么 83 这个基线本身已经漂移。这个数字会进最终验证报告，必须先说清。

**前置**：无。

**输入**
- `src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java`
- `tools/full_verification_report.json`（T19/T20 时期那份，取其 gametest 名单）
- `tools/t19_readiness.json`、`tools/t20_readiness.json`

**动作**
1. 从当前源码提取 86 个 `@GameTest` 方法名，排序成清单。
2. 从上一份 READY 报告提取 83 个方法名清单。
3. 做集合差。预期差集是 2 个 T21 方法
   （`compositionGeneratedCarbonFamilyExecutesTwoMaterials`、
   `mixerGunpowderTemplateExecutesAllMembers`），实际会多出 1 个。
4. 把多出的那个归属清楚：属于哪张卡、什么时候加的、有没有对应 readiness 记录。
5. 若它是无主的，判定保留还是删除，并写进 T21 交付账。

**验收**
- [ ] 三份清单（86 / 83 / 差集）落盘为可复算 artifact
- [ ] 差集里每一项都有明确 owner 卡号
- [ ] T21 新增 GameTest 的准确数字写定（是 2 还是 3），后续所有卡引用这个数字

**交付物**：`tools/t21_gametest_delta.json` 或等价物。

**注意**：不要因为「86 看起来是对的」就直接把预期改成 86。先解释差额来源。

---

## T21-C · operand 可达性工具落地

**为什么**：当前 `unreachable = 0` 是从**模板投影**算的，不是从 **operand 生产者
闭包**算的。T15c / T18 用的是后者。两个口径在 gunpowder family 上给出相反答案。
这张卡只负责把工具装好、让 CI 能问出正确的问题；**修问题是 T21-D 的事**。

**前置**：无。

**输入**
- 现成脚本 `t21_operand_reachability.py`（随本计划提供，已验证 `--check` fail-closed）
- 全部 recipe 资源根、`material_registration_gate.json`、`material_prefixes/`、
  `materials/`、`worldgen_catalog/ore_veins.json`

**动作**
1. 脚本放进 `tools/`，按仓库惯例调整路径常量。
2. 登记进 `tools/verification_builder_policy.json`：`proof_tier` 用 `compact`
   （它只读已提交的资源树，不碰 raw dump）。注意 builder 总数会从 59 变 60，
   `run_full_verification.py` 里那个 `len(builders) != 59` 的硬断言要同步改。
3. 登记进 `tools/python_test_policy.json` 的 affected 规则：recipe 资源根或
   材料定义变动时必须触发。
4. 跑一次 `--json`，把完整诊断账落盘。

**验收**
- [ ] `python tools/t21_operand_reachability.py --check` 当前**退出码为 1**
      （这是正确行为：它应该抓到 2 条 t21 不可达行）
- [ ] `--json` 输出包含 `unreachable_operand_recipes_t21` 且长度为 2
- [ ] builder policy 断言数字已从 59 改为 60，`run_full_verification.py` 能加载
- [ ] 脚本 docstring 里的三条近似（不建模流体 / 不建模掉落 / 无法解析条件视为满足）
      原样保留，不要删

**交付物**：`tools/t21_operand_reachability.py`、`tools/t21_operand_reachability.json`、
两份 policy 的改动。

**已知基线数字**（用于比对，跑出来应该接近）：
3,184 条具体配方、90 个 material_rule 文件、约 10,000 条规则展开、13,152 条边、
129 个有矿脉的矿物做种子、5 轮收敛、2,436 个可达 item id、
1,953 条配方存在不可达 operand（ore_chain 1,219 / generated 646 / t5 75 / main 11 / t21 2）。

---

## T21-D · coal_coke / charcoal 桥接决策与实施

**为什么**：T21 唯一的 v1 template（4 行 gunpowder）里，**2 行的输入在生存中拿不到**。

已确认的事实链：
- 焦炉产出 `cruciblecraft:coal_coke`（`ModItems.COAL_COKE` 简单物品）；
  gunpowder 要的是 `c:dusts/coal_coke`，即 `cruciblecraft:coal_coke/dust`。两者无关。
- `GeneratedMaterialPack.canonicalItemId()` 只在材料声明了 `form_items` 时才把形态
  绑到既有物品。全仓仅 7 个材料用了它：lead / gold / tin / zinc / nickel / iron /
  copper。**coal_coke 和 charcoal 都不在其中**，所以 `c:dusts/charcoal` 也不含
  `minecraft:charcoal`。
- 两者都没有 `ore` 形态、不在 `ore_veins.json`、ore_chain 无目录。
- 可达性闭包结论：**coal_coke 0/18 形态可达，charcoal 0/18 形态可达**。
- GameTest 没抓到，是因为 `loadRecipeInputs()` 直接
  `setStackInSlot(recipe.itemInputs().get(i).getItems()[0])`，完全绕过获取路径。

**前置**：T21-C（需要工具确认修复生效）。

**输入**
- `src/main/resources/data/cruciblecraft/materials/coal_coke.json`、`charcoal.json`
- `src/main/java/com/masson/cruciblecraft/material/gen/GeneratedMaterialPack.java`
- `src/main/java/com/masson/cruciblecraft/material/MaterialFingerprint.java`
- `material_registration_gate.json`

**动作**

**第一步先做调查，不要直接改。** 确认 `cruciblecraft:coal_coke/gem` 和
`cruciblecraft:charcoal/gem` 当前**是否真的注册出了实体物品**。这决定方案可行性：

- 若已注册 → 改 `form_items` 等于移除一个已注册物品，必须走存档 quarantine 路径。
- 若未注册 → 改 `form_items` 是纯新增绑定，成本低得多。

然后三选一（推荐 A）：

| 方案 | 做法 | 代价 |
|---|---|---|
| **A（推荐）** | charcoal 加 `form_items: {gem: "minecraft:charcoal"}`；coal_coke 加 `{gem: "cruciblecraft:coal_coke"}`。T7 `gem_to_dust` 规则立刻打通 | 零新增配方，publication delta 为 0；但要重跑 `MaterialFingerprint`（它把 `formItems` 计入指纹）和存档兼容检查 |
| **B** | 新增 2 条登记为 `DESIGN_POLICY_NON_GT6` 的 vanilla crafting 获取配方，照 T19 非金属管先例 | 新增 2 条 datapack entry，需重算 load |
| **C** | 把 2 个成员从 v1 模板拆出单独 deferred，v1 template 降为 2 行 | 最省事，但 v1 内容变少，且要写 replacement condition |

A 最符合既有架构——`form_items` 本来就是为「材料形态由既有物品承载」设计的，
现在只覆盖了 7 个原版金属，是覆盖不全而不是机制不对。

**验收**
- [ ] `python tools/t21_operand_reachability.py --check` **退出码变为 0**
- [ ] `--explain c:dusts/coal_coke` 和 `--explain c:dusts/charcoal` 都显示 REACHABLE
      且列出具体生产者路径
- [ ] 选 A 时：`MaterialFingerprint` 变更已记录，存档兼容测试通过
- [ ] 选 B 时：新增 2 条明确标 `DESIGN_POLICY_NON_GT6`，GT row delta 仍为 0
- [ ] 选 C 时：2 条 deferred 各有 reason / replacement condition / recheck point
- [ ] 无论哪个方案，`en_us` 和模型不新增 placeholder

**交付物**：改动文件 + 方案选择理由（写进 T21 档案）。

**顺带记录不处理**：闭包发现 1,953 条既有配方存在不可达 operand。绝大多数应是
已登记的 post-1.0 材料债，**不属于 T21 范围**，但 T27 portfolio freeze 需要这份
清单。落盘存档即可，不要在这张卡里修。

---

## T21-E · partially-reachable 拆分规则入 policy

**为什么**：现有 policy 有一条好规则——「禁止 partially-covered template，出现部分
覆盖时先拆」。但没有对应的 partially-**reachable** 规则。gunpowder 恰好是 4 成员里
2 个可达，正该触发同一个拆分动作，规则却管不到。

**前置**：T21-C。可与 T21-D 并行，但两者结论要对齐。

**输入**
- `tools/t21_template_denominator_policy.json`
- T21-C 的可达性输出

**动作**
1. 把拆分规则扩写为：**partially-covered 或 partially-reachable 的 template 一律
   先拆**，拆后每个子单元独立分类。
2. 在每个 `v1_required` 单元的记录结构里加第二个必填证明位：

```text
consumer_proof : 产物在 Beta 主链有真实消费端        （已有）
operand_proof  : 全部 operand 在 available-identity
                 闭包内可达，闭包输入可复算          （新增）
```

3. 两个证明位缺一，该单元不得分类为 `v1_required`。
4. 加一条 Python 测试：构造一个 partially-reachable fixture，确认门禁变红。

**验收**
- [ ] policy schema 版本号递增，两个证明位都是必填
- [ ] gunpowder family 按新规则重新分类，结果与 T21-D 的方案一致
- [ ] 变异测试：人为把某成员的 operand 标为不可达，分类门禁 FAIL

**交付物**：更新后的 policy、新增测试。

---

## T21-F · Beta seed 分层与必要性闭包重算

**为什么**：「只得到 1 个 v1 template」这个结论目前建立在人工 seed 列表上。更麻烦的是，
seed 来源里的 **T22 石油入口**和 **T23 多方块需求**是两张还没开工的卡——T23 连 30 个
kind 的行为分类都没做，它的「需求」此刻并不存在，只能靠估。含人工估值的闭包不该以
「1」这个确定数字的形式进档案。

**前置**：T21-D、T21-E。

**输入**
- `tools/t21_template_denominator.json`（93,133 units）
- T20 worldgen catalog、T5 terminal closure、当前注册的能源 / 物流对象
- T21-C 的可达 identity 集合（2,436 个）

**动作**
1. seed 集拆两层：

| 层 | 来源 | 要求 |
|---|---|---|
| `derivable` | 世界资源、T5 terminal closure、当前能源 / 物流注册对象、T21-C 可达集 | **必须能从现有 artifact 机器生成**，禁止人工列表 |
| `forward_declared` | T22 石油入口、T23 多方块需求 | 逐条写 owner、理由、recheck point |

2. 用 `derivable` 层单独跑一次闭包，得到一个不含人工估值的下界。
3. 加上 `forward_declared` 再跑一次，得到上界。
4. **两个数都写进档案**，并注明 T22 / T23 开工时必须回头重算。
5. 档案里「唯一 v1 template」的表述改成带方法的版本，例如：
   「由 derivable seed 自动闭包得出 N 个，闭包输入为 X 条 seed identity，
   forward_declared 层另有 M 个待 T22/T23 确认」。

**验收**
- [ ] `derivable` 层 100% 由脚本生成，无人工条目（抽查 5 条，每条能指回源 artifact）
- [ ] 上下界两个数字都落盘
- [ ] `forward_declared` 每条有 owner 和 recheck point
- [ ] 结论可复算：同样输入重跑得到同样数字

**交付物**：分层后的 seed artifact、两次闭包结果、档案表述修订。

**心理准备**：v1 template 数字很可能从 1 变大。这是好事——现在发现比 Beta 门禁时发现便宜得多。

---

## T21-G · compact artifact 体积审计

**为什么**：template denominator 约 11 MB，还没确定 compact 策略。T13 已有现成先例，
照抄即可，不需要发明新方案。

**前置**：无。

**输入**
- template denominator 相关 artifact
- `tools/verification_builder_policy.json` 里 T13 的三层证明配置（compact /
  hash-fast / full-replay）

**动作**
1. 照 T13 先例分层：ordinary CI 只消费带 input/output hash 的 compact receipt；
   全量 64,245 行 replay 归 `--source-replay` **独占 owner**。
2. 给 compact artifact 定一个明确的体积上界（建议按 T13 同类 artifact 的量级定，
   不要拍脑袋），写进 policy 并加 CI 断言。
3. 确认 clean-checkout（隐藏全部 raw / cache / fetched-source）下 compact 层仍可通过。

**验收**
- [ ] ordinary CI 路径不读 raw dump（实测：删掉本地 cache 后仍 PASS）
- [ ] compact artifact 体积在声明上界内，超限 CI 硬失败
- [ ] full replay 仍能从 compact receipt 的 hash 验证一致性
- [ ] 同一 raw corpus 只有一个 canonical full-replay owner（T17 已定的规矩）

**交付物**：compact artifact、policy 改动、体积上界数字及其依据。

---

## T21-H · 59→60 builder clean-checkout 验证

**为什么**：`run_full_verification.py` 当前硬断言 builder 数量为 **59**（T19/T20 时期
是 50），说明 T21 已经注册了 9 个新 builder，但完整 closure 从未跑过。这 9 个的
ordinary argv 与 proof tier 是否都能在 clean-checkout 下通过，**完全未验证**。
这会 hard-fail 整个 `--record`，所以要早做。

**前置**：无。（T21-C 会把数字变成 60，两张卡都改到这个断言时以后完成的为准。）

**输入**
- `tools/verification_builder_policy.json`
- `run_full_verification.py`

**动作**
1. 列出 9 个新 builder（与 T19 时期的 50 个做差集）。
2. **逐个**单独跑 ordinary argv，记录耗时和退出码。不要一次跑全部——失败时定位成本太高。
3. 检查每个的 `proof_tier` 登记是否正确：读 raw dump 的必须是 `rederived` 且有
   full-replay 命令；只读已提交 artifact 的用 `compact`。
4. 用 clean-checkout guard 隐藏全部本地 raw / cache / fetched-source，再跑一次全量。
5. 记录总耗时。参考基线：50 builder 首轮 23.248 秒，暖运行中位数 22.996 秒，
   clean-checkout guard 下 27.290 秒。新增 builder 后耗时上升属正常，但要有数字。

**验收**
- [ ] 9 个新 builder 逐个 PASS，各自耗时已记录
- [ ] clean-checkout 下全量 PASS，无一冒充（raw 缺失处显式 SKIP）
- [ ] proof_tier 登记与实际读取行为一致（抽查 3 个）
- [ ] 总耗时落盘；性能退化只产生 WARN，hash / 集合 / 语义 / currentness 漂移仍 hard-fail

**交付物**：逐 builder 耗时表、clean-checkout 运行日志。

---

## T21-I · gunpowder family 隔离 load 实测

**为什么**：现在的 load 数字（4 source facts / 1 authored rule / 4 datapack files /
4 logical / 4 eager / 0 lazy）是**生成器侧的保守估算**，不是隔离的运行时实测。
全局 18,875 → 18,879 这个增量必须从实际 publication metrics 取，不能从文件数推。

**前置**：T21-D、T21-E（family 成员可能变化，先定下来再测）。

**输入**
- `GTRecipeMapLoader` 的 publication metrics
- T14 建立的 family load projection schema 与 zero-workload fixture

**动作**
1. 从运行时 `GTRecipeMapLoader` 取 publication metrics，而不是数 JSON 文件。
2. 测四类：server reload / index、client re-expansion、峰值内存、steady-state lookup。
   复用 T14 的协议，不要另发明。
3. 确认全局账目：32 RecipeMap 不变，logical / eager / lazy 从
   18,875 / 16,650 / 2,225 变为实测值，EMI 24 不变。
4. family load projection 跑出 PASS。

**验收**
- [ ] 每个数字标注来源是 `MEASURED` 而非 `STATIC_INFERENCE`
- [ ] 全局 logical / eager / lazy 增量与 family 增量对得上
- [ ] RecipeMap 数、EMI configured map 数与 T20 基线相等
- [ ] 若 T21-D 选了方案 B（新增 2 条 vanilla crafting），datapack entry 增量单独记账，
      且 GT row delta 仍为 0

**交付物**：`t21_load_projection_input.json` / `t21_load_projection.json`。

---

## T21-J · T16 / T17 currentness 修复

**为什么**：fast Python 有两项 currentness stale 失败。

**重要线索**：我已核对 `machine_tiers.json` 有 **33** 个 variant，
`t17_machine_tier_expected.json` 的 `catalog_count` 也是 **33**——**数值是对的**。
所以大概率不是数值漂移，而是 readiness 依赖链的 digest 没随 T21 新增 builder 刷新。

**前置**：无。

**输入**
- `tools/t16_readiness.json`、`tools/t17_readiness.json`
- `src/test/resources/t16_machine_tier_expected.json`、`t17_machine_tier_expected.json`
- `src/main/resources/data/cruciblecraft/machine_tiers.json`

**动作**
1. **先诊断再改。** 跑失败的两项测试，抓出它比较的到底是什么：是数值、集合、
   还是 digest / currentness 链。
2. 若是 digest 未刷新 → 沿 currentness 依赖链重新生成 readiness，**不动 expected**。
3. 若真是数值漂移 → 停下来回报，不要自行决定改哪边。

**验收**
- [ ] `python tools/run_python_tests.py --suite fast` 全绿
- [ ] `t16_machine_tier_expected.json` / `t17_machine_tier_expected.json` 的内容
      **未被修改**（除非有单独书面理由）
- [ ] T16 / T17 的历史关闭计数未被回写（T16 初次 475/392/62、T17 508/441/69 保持历史值）
- [ ] RecipeMap publication、运行时能力模型均未变化

**交付物**：诊断结论 + 刷新后的 readiness。

**红线**：如果你发现「把 expected 里的 33 改成别的数就绿了」——**不要改**，回报。

---

## T21-K · dedicated GameTest server 全量执行

**为什么**：新增的 T21 GameTest 还没在 dedicated server 上跑过。开发环境通过不等于
专用服务端通过。

**前置**：T21-A 到 T21-J 全部 DONE。

**动作**
1. `.\gradlew.bat runGameTestServer`
2. 全量执行（数量以 T21-B 定下的为准，86 或调整后的值）。
3. 特别确认 `mixerGunpowderTemplateExecutesAllMembers` 的全部成员通过——
   若 T21-D 改了 family 成员，这个测试要同步改。

**验收**
- [ ] 全部 GameTest PASS，数量与 T21-B 的结论一致
- [ ] 无 skip、无 flaky 重试
- [ ] 日志落盘进验证会话

---

## T21-L · source-replay 与 closure

**前置**：T21-K DONE。

**动作**
```text
python tools/build_t21_chemical_axis.py --check --full-replay
python tools/gt6_mixer_templates.py --check --full-replay
python tools/build_t21_template_denominator.py --check --full-replay
python tools/build_t21_mixer_gunpowder.py --check --full-replay
python tools/run_python_tests.py --suite closure
python tools/run_python_tests.py --suite source-replay
.\gradlew.bat test
```

**验收**
- [ ] Mixer 64,245 行 canonical multiset replay：0 missing / 0 extra
- [ ] membership unassigned / duplicate = 0
- [ ] 全部 Python closure 与 source-replay 通过
- [ ] Java 单测全绿
- [ ] raw 缺失处显式 SKIP，未冒充 PASS

---

## T21-M · 单次 clean full verification 与文档同步

**为什么这张卡最后且不可拆**：文档同步和最终验证挤在一起，正是这次 `T21_READY`
被提前写入的原因。所以这张卡的顺序是**先验证、后同步**，不可颠倒。

**前置**：T21-A 到 T21-L 全部 DONE。

**动作（严格按序）**
1. 跑**一次** `python tools/run_full_verification.py --record --new-session`。
2. **成功之后**，由验证会话派生 `T21_READY` 写入 `tools/t21_readiness.json`
   （走 T21-A 建立的机制，不许手写）。
3. 再同步文档：
   - `CrucibleCraft-阶段档案-T21.md`
   - `CrucibleCraft-第四阶段总体规划.md`（T21 状态、下一张卡转 T22）
   - `tools/phase4_v1_planning_contract.json`
   - README

**验收**
- [ ] full verification 一次通过，未重跑掩盖失败
- [ ] `T21_READY` 只绑定这一次会话
- [ ] 三轴账（closure / fidelity / load）分别有状态和证据
- [ ] 第四阶段统一门禁逐条勾选，其中这几条**这次必须真的能勾**：
      「新增注册对象获取可达」「可投影全集与独立 expected 双向相等」
      「blocked / deferred 有 reason、replacement condition、recheck point」
- [ ] 档案里不再有任何未由验证派生的数字

**若失败**：回到失败项对应的卡，**不要在这张卡里就地修**。修完重走 T21-K → L → M。

---

## 2. 执行者常见误区速查

| 现象 | 正确反应 |
|---|---|
| 改一下 expected 就绿了 | 停，回报。默认是实现有问题 |
| 某个数字对不上，但差得不多 | 停，差多少都要解释来源 |
| raw dump 本地没有，测试报错 | 显式 SKIP，不许改成 PASS |
| 顺手发现相邻系统有 bug | 记进「顺带发现」，不要修 |
| full verification 失败了，再跑一次就好了 | 不行。失败必须定位到卡 |
| 文档里写 `T21_READY` 更省事 | 不行，只能派生 |
| 「1,953 条不可达配方」看着吓人 | 不是 T21 范围，落盘存档给 T27 |

---

## 3. 本计划相对上一版的改动

1. **补上 operand proof**：上一版的必要性只做消费侧（「有人要」），没做获取侧
   （「拿得到」）。这是 coal_coke / charcoal 漏网的根因——第四阶段统一门禁其实两条
   都写了，是计划漏译了门禁。
2. **拆分规则扩写**：partially-covered → partially-covered **或 partially-reachable**。
3. **Beta seed 分层**：把含人工估值的 T22 / T23 需求隔离出去，给出上下界而非单一数字。
4. **READY 生成机制**：从「文档同步和验证同一个工作单元」拆成两卡，且 READY 只能派生。
5. **粒度从 6 项细化到 13 项**：新增体积审计、builder 验证、currentness 修复三类
   ——它们都是执行中才冒出来的，说明上一版风险预估留白不够。
6. **每张卡自带验收**：不依赖完成后的整体人工复查。

---

*每张卡都可以单独交给不同的人。做完一张贴回报格式即可，不需要等其他卡。*
