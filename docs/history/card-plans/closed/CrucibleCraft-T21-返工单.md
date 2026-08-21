# T21 交付复核 · 返工单

> 复核对象：`src-2026-8-10-0736.7z`
> 比对基准：`src-2026-8-08-1355.rar`（逐文件 SHA-256）
> 复核结论：**T21-D 通过并已独立验证**；另有 1 项必须回退、5 项待补。
>
> 比对事实：0 新增文件、0 删除文件、166 个文件变动，其中
> **7 个真实内容变更**，159 个为行尾（CRLF）转换。

---

## 0. 怎么用这份单子

每张卡独立，做完贴回报即可，不必按顺序——**除了 R1，它要最先做**，因为后面所有
publication 账都依赖它。

回报格式：

```text
卡片：R-x
状态：DONE / BLOCKED / 有异议
验收项：逐条 PASS / FAIL，FAIL 附实际值
交付物：文件路径
```

**通用规矩不变**：期望值 / 基线 / 历史快照对不上时，默认结论是实现或 currentness
链有问题，不是数字写错了。要改历史数字必须单独写理由。

---

## R1 · 回退 T16 / T17 历史 publication 基线 ⛔ 最高优先级

**症状**

`main/resources/data/cruciblecraft/t16_publication_baseline.json` 与
`t17_publication_baseline.json` 的 `publication_totals` 被从
`18,875 / 16,650` 改成 `18,879 / 16,654`。

**为什么不能这样**

这两个文件是**历史快照，不是当前账**。t16 那份的元数据写得很明确：

```json
"status": "T16D_PUBLICATION_BASELINE",
"captured_before": "T16"
```

它的语义是「T16 开工前的状态」，而这个 baseline 存在的唯一目的，就是证明 T16 的
`publication delta = 0`。把它顶到今天的数字，那条断言就退化成「今天 == 今天」的
同义反复，**永久失去检测能力**。

更直接的问题是：四份同类基线只改了两份，链条现在自相矛盾。

| 文件 | 元数据语义 | 当前值 |
|---|---|---|
| `t16_publication_baseline.json` | `captured_before: T16` | **18,879** ← 被改 |
| `t17_publication_baseline.json` | `baseline: T16` | **18,879** ← 被改 |
| `t18_publication_baseline.json` | `baseline: T17` | 18,875 |
| `t19_publication_baseline.json` | `baseline: T18` | 18,875 |

顺着这条链读：pre-T16 = 18,879 → post-T16 = 18,879 → post-T17 = 18,875。
**等于断言 T17 删掉了 4 条 row**，与 T17 自己的 `publication delta = 0` 关闭证据
直接冲突。原本四份全是 18,875，正是因为 T16–T19 每张卡的 delta 都是 0，链条本该是平的。

**正确做法**

1. 四份历史 baseline 全部回到 `18,875 / 16,650 / 2,225`，一个字都不改。
2. 新建 `t21_publication_baseline.json`，`baseline: "T19"`，值同为 18,875。
3. T21 的 `+4 logical / +4 eager` 记为 **T21 自己的 delta**，写进 T21 readiness，
   不回写任何历史文件。
4. `CrucibleCraftGameTests.java` 里 `allPublishedRecipes() == 18_879` 和
   `eagerPublishedRecipes() == 16_654` 这两处**保持不动**——它们测的是当前值，是对的。

**验收**

- [ ] 四份历史 baseline 的 `publication_totals` 与 8/08 版本逐字节相同
- [ ] 新增 t21 baseline 存在，`baseline` 字段为 `T19`
- [ ] T21 readiness 中 delta 明确记为 +4 / +4 / 0
- [ ] 历史关闭计数（T16 初次 475/392/62、T17 508/441/69）未被回写

**如果之前的 fast Python currentness 失败是靠改这两个数修好的**：请把原始报错贴出来，
这项要重新按「刷新 readiness digest、不动 expected」的路子做一遍。

---

## R2 · 补 `coal_coke/gem` 与 `charcoal/gem` 的存档迁移

**症状**

`form_items` 改动本身是对的（见文末「已通过」一节），但有一个副作用没处理。

**证据**

`registry/ModItems.java:436`：

```java
if (form.equals(MaterialPrefixes.ORE)
        || material.formItems().containsKey(form)) {
    continue;   // 跳过注册
}
```

加了 `form_items` 之后，**`cruciblecraft:coal_coke/gem` 和
`cruciblecraft:charcoal/gem` 这两个原本已注册的物品从此不再注册**。
同时 `MaterialFingerprint.materialCanonical()`（第 137 行）把 `formItems` 计入指纹，
所以材料指纹也变了。

包里没有对应的迁移或 quarantine 代码。

**正确做法**

按 T12 / T15 已有的 identity quarantine 先例处理，需要回答三件事：

1. 老存档里持有这两个物品的玩家，加载时会发生什么？有没有路径接住？
   （参照 T15d 四类 mismatch 的 quarantine：库存与原始坏 identity 可恢复）
2. `material_registration_gate.json` 的 `registered_forms: 16048` 未变——确认这个数的
   单位是「形态」还是「注册物品」。若是后者，实际已降到 16,046，需要同步。
3. 材料指纹变更是否已进 currentness 链。

**验收**

- [ ] 存档兼容测试覆盖「持有已移除的 `<material>/gem` 物品」这一场景
- [ ] 该场景走 quarantine 而非静默丢失
- [ ] `registered_forms` 的单位有明确定义，数值与定义一致
- [ ] 指纹变更已随 currentness 链刷新

**补充**：材料形态的 lang / model 由 `GeneratedMaterialPack` 运行时生成，不在
`generated/` 里，所以这次没有产生孤儿资源文件——这一项不用查。

---

## R3 · 159 个 JSON 的 CRLF 转换

**症状**

旧树 **0** 个 JSON 含 CRLF；新树顶层有 **159** 个。这 159 个正好是那批
「变动但内容相同」的文件——129 个 worldgen configured feature、几份 schema、
几份 baseline 等。

`.java` 是干净的（0 个 CRLF），所以 2026-08-06 定下的 `*.java text eol=lf` 契约和
那个 Python closure 扫描不会报警。

**为什么要管**

扫描只覆盖 `src/**/*.java`，JSON 从底下溜过去了。而 currentness 体系大量依赖逐文件
SHA-256（T13 的 hash-fast 层、compact receipt 的 input/output hash）。行尾变了字节就变了，
这 159 个文件的所有历史 digest 全部失效，**语义却没有任何变化**。

T20 明确保证过「feature id 与 salt 不变」「少一条、多一条、stale 文件或任一字段变异
均使门禁失败」——现在会有一批伪变异需要人工甄别。

**正确做法**

1. 这 159 个文件转回 LF。
2. 行尾契约从 `*.java` 扩到文本资源（`*.json` 至少要覆盖），并把 Python closure
   扫描范围同步扩大，防止回归。

**验收**

- [ ] 顶层树内 `.json` 含 CRLF 的文件数为 0
- [ ] 扩大后的扫描能在人为引入一个 CRLF JSON 时变红（实测一次）
- [ ] 这 159 个文件与 8/08 版本逐字节相同

---

## R4 · `compare_gt6_recipes` 缺 `source_evidence`（要查覆盖面）

**症状**（你自己发现的那个）

`compare_gt6_recipes` builder 的 process evidence validation 缺 GT6
`source_evidence` 配置。

**结构性成因**

`run_full_verification.py` 的 `load_builder_policy()` 只校验 4 个字段：

```python
if (not isinstance(row.get("name"), str)
        or not isinstance(row.get("script"), str)
        or not isinstance(row.get("ordinary_args"), list)
        or row.get("proof_tier") not in {"compact", "rederived"}):
    raise ValueError(...)
```

**`source_evidence` 根本不在必填项里**，而且 `proof_tier` 虽然被记录，加载器并不检查
`rederived` 的行是否真的配了 full-replay 命令。

**风险分两种，取决于 evidence validation 的实现**

- 缺配置时**抛错** → `--record` 在 builder 阶段炸掉，属于响亮失败，可接受；
- 缺配置时**跳过校验** → **vacuous PASS**，即「没有证据可查所以判通过」。

后者是 T17 关掉的那个洞的另一种形态。T17 定的规矩是「同一 raw corpus 只有一个
canonical full-replay owner，下游 readiness 只消费带 hash 的 compact evidence，
raw 重放不许绕过策略直接进普通 closure」。一个 builder 缺 `source_evidence`，
它的 proof tier 就是**声明了但没执行**。

**正确做法**

1. 先确认 `verification_builder_policy.json` 里 `compare_gt6_recipes` 那行的
   `proof_tier` 是什么。若是 `rederived` 却没有 `source_evidence` / full-replay 命令，
   说明它一直在 ordinary CI 里裸跑。
2. **扫全部 builder，统计有多少行缺这个字段。** 如果 validation 是「跳过」语义，
   这个洞不会只影响一个 builder。
3. 把 `source_evidence` 加进 `load_builder_policy()` 的必填校验，对
   `proof_tier == "rederived"` 的行强制要求非空。
4. 加一条断言：禁止「配置缺失 → 跳过校验」，改为加载期 fail closed。

**验收**

- [ ] 缺 `source_evidence` 的 builder 完整清单已落盘（数量 + 名单）
- [ ] `load_builder_policy()` 在字段缺失时抛错，实测一次贴报错
- [ ] `rederived` 行强制要求 full-replay 命令
- [ ] 修复后全部 builder 重跑通过，无一 vacuous PASS

---

## R5 · GameTest 计数差额仍未消歧（原 T21-B）

**症状**

`@GameTest` 仍是 **86** 个。`CrucibleCraftGameTests.java` 这次只加了一段 re-power
断言，没有新增方法，也没有 delta artifact。

**为什么要管**

T19/T20 基线是 83，T21 新增 2（carbon family + gunpowder）应为 85。多出的 1 个来源不明，
而这个数字要进最终验证报告。

**正确做法**

1. 从当前源码提取 86 个方法名，排序成清单。
2. 从上一份 READY 报告提取 83 个方法名清单。
3. 做集合差，把多出的那个归属清楚：属于哪张卡、什么时候加的、有没有 readiness 记录。
4. 若无主，判定保留还是删除，写进 T21 交付账。

**验收**

- [ ] 三份清单（86 / 83 / 差集）落盘为可复算 artifact
- [ ] 差集里每一项都有明确 owner
- [ ] T21 新增 GameTest 的准确数字写定，后续所有卡引用这个数字

**注意**：不要因为「86 看起来是对的」就把预期直接改成 86。先解释差额来源。

---

## R6 · 两处口径问题

### R6a · T21 配方被计进 `t5ChemicalRecipes`

GameTest 断言里 `metrics.t5ChemicalRecipes()` 从 154 变成 158——T21 的 4 条 row 被算进了
名为 t5 的计数器。

可能是刻意的（该计数器也许语义是「dedicated chemical map 的配方」），但名字叫 t5 却装
t21 的行，违反「数量单位不可混写」。

**做法**：确认语义。若确实是「dedicated chemical map 总数」，改名或至少加注释写清；
若不是，T21 需要独立计数器。

### R6b · `T21_AUTHORED_MATERIAL_RULE_BUDGET = 256`

实际 authored 是 4 条，余量 64 倍。对照惯例：T7 是 220/256、T8 是 257/320、
T11/T12 直接是 0。

256 看起来是从 T7 抄的默认值，不是按「先算数字」算出来的。

**做法**：按 T21 实际投影重算预算，或写明为什么留这么大余量。

---

## R7 · 压缩包里的 `src/src/` 嵌套目录

包内存在一个多余的 `src/src/` 目录，2,384 个文件，是 `generated/` 和
`component_rule_generated/` 的部分副本，还带着 `.cache`。这批全部是 CRLF。

**做法**：确认它只是打包失误。如果它真的进了仓库，就是 stale resource root，
会被「stale 文件即失败」门禁抓到。

**验收**

- [ ] 确认仓库中不存在该目录，或已删除
- [ ] 若曾提交，确认 stale 文件门禁能抓到这类情况

---

## ✅ 已通过：T21-D（可直接标 DONE）

`form_items` 方案 A 改法完全正确：

```json
charcoal.json   →  "form_items": { "gem": "minecraft:charcoal" }
coal_coke.json  →  "form_items": { "gem": "cruciblecraft:coal_coke" }
```

用 operand 可达性闭包在新树上实测：

```
recipes with unreachable operand : 1946   (原 1953)
  of which t21            : 0             (原 2)   ← 退出码 0
reachable item ids        : 2438          (原 2436)
```

`--explain` 两条均 REACHABLE，生产者路径清楚：
`coal_coke/gem → t7/mortar/gem_to_dust → coal_coke/dust`，charcoal 同理再加一条 sifter 路线。

**T21-D 全部验收项 PASS。** 配套的三处 Java 改动也合理：`GTRecipeMapLoader` 放行
`t21/` 前缀进 dedicated map、`ModProcessingMachines` 加 T21 预算槽、GameTest 补 re-power 断言。

除 R2 的存档迁移外，这张卡不需要返工。

---

## 需要你补充材料才能复核的部分

这次压缩包只有 `src/`，`tools/` 不在里面，所以下面这些**我无法确认状态**：

| 卡 | 内容 | 需要的产物 |
|---|---|---|
| T21-A | 撤销草稿 READY 并锁死生成机制 | `tools/t21_readiness.json`、builder 断言代码位置 |
| T21-C | operand 可达性工具入 CI | `tools/t21_operand_reachability.py` 及两份 policy 改动 |
| T21-E | partially-reachable 拆分规则 | `tools/t21_template_denominator_policy.json` |
| T21-F | Beta seed 分层与闭包重算 | 分层 seed artifact、上下界两个数字 |
| T21-G | compact artifact 体积审计 | compact artifact + 体积上界及依据 |
| T21-H | 59→60 builder clean-checkout | 逐 builder 耗时表、clean-checkout 日志 |
| T21-I | gunpowder 隔离 load 实测 | `t21_load_projection*.json` |

**麻烦下次把 `tools/` 一起打包。** 另外 T21-B（= 本单 R5）从源码看确实还没做——
`@GameTest` 数量没变、也没有 delta artifact，请确认是漏了还是在别处交付。

---

## 建议顺序

1. **R1**（历史 baseline 回退）——不修这个，后面所有 publication 账都建在错地基上
2. R4（`source_evidence` 覆盖面）——可能不止一个 builder
3. R2、R3、R5 可并行
4. R6、R7 收尾
5. 全部 DONE 后再走 GameTest → source-replay → closure → **单次** clean
   `run_full_verification.py --record`，成功后才由验证会话派生 `T21_READY`

`T21_READY` 在上述全部完成前不要写进任何文档。
