# T40 闭卡拖延回顾

> 日期：2026-08-26
> 性质：本轮实际遇到的问题与结构缺口，供人工检查。不含重构建议。
> 范围：T40 Electrolyzer 收口（R5/R6）期间的现场记录。T40 生产内容本身已是 `T40_READY`。

T40 的生产锁、隔离加载、player path、GameTest 8/8 在收口前已经具备。拖延发生在
**闭卡验证**：为了让 T38 支撑计数回到 READY 分母，动了材料门闸，随后 recipes /
census / census-replay 的哈希链、Java 门闸契约和几份超大 JSON 把收口拉成数小时。

## 1. 时间实际花在哪

| 阶段 | 预期 | 实际 |
| --- | --- | --- |
| T40 census / topology / readiness 两次零漂移 | R5/R6 主体 | 已完成，不是主耗时 |
| `recipes` profile + `gradlew test` | 一次绿灯 | 先被 T38 支撑不足、再被 24 个 Java `IllegalStateException`、再被一串 `--check` 哈希失败打断 |
| `census` / `census-replay` | 闭卡例行 | 门闸哈希改完后 T35–T40 几乎全部 currentness 过期；`test_build_t35_readiness` 单测约 2 分钟 |
| T38 矿 `ore` overlay | 不应属于 T40 收口 | 成为本轮最长路径 |

T40 自身未 resign production lock（`26cca296…`）。`#0000` / `#0001` 仍是
`combinatorial_player_path_unproven`，不扣 gap。剩余普通配方 5597。T41 未签发。

## 2. 矿物注册与门闸

### 2.1 T38 支撑计数依赖门闸里的 `ore`

T38 publication 的 authored / eager 支撑分母是 **19 / 15**（5 条 GT recovery + 10 条
ore-block crush + 4 条 packing）。现场一度只有 **10 / 6**：crush 行只剩 iridium。

原因：`src/main/resources/data/cruciblecraft/material_registration_gate.json`
被回到不含 T38 acquisition overlay 的状态。注册矿从 **147** 退回 **137**。
`tools/t38_source_backed_acquisition.json` 的 `required_forms` 仍要求那些材料带
`ore`，但门闸 `materials` 段没有对应 overlay。

`build_gt6_ore_chain.py` 只给**已注册 ore** 的材料发 `crush_ore_block_to_crushed`。
没有门闸 overlay，就没有那 9 条 crush，T38 publication 就不能报 READY 19/15。

T38 `new_ores` 相对 `ALREADY_FACTUAL_OVERLAY_ORES`（`gold`、`molybdenum`）多出来的
acquisition 矿包括 arsenopyrite、chalcopyrite、cooperite、iridium、molybdenite、
palladium、powellite、pyrite、sperrylite、wulfenite 等。它们多数**没有**
`cruciblecraft:generates_ore`。L3 ore 计划仍是 base 135 + include `Iron` = 136，
不覆盖这批 overlay 矿。

### 2.2 Python 门闸允许 overlay `ore`，Java 加载器不允许

`tools/build_gt6_material_form_gate.py` 对 T38 acquisition 的检查是：

```text
unsupported = required_forms - factual_forms - {"ore"}
```

也就是 **只有 `ore` 可以超出 GT6 factual**。选中集合写入门闸时 **不与 factual 求交**，
所以 committed gate 里可以出现 arsenopyrite 的 `ore`。

`MaterialRegistrationGate.load` 原先要求：

```text
definition.forms().containsAll(registered)
```

`definition.forms()` 来自 generation flags / include_prefixes，不含这批 overlay
`ore`。结果：任何加载门闸的 JUnit 在启动时抛 `IllegalStateException`。本轮观察到
24 个失败，几乎全是这个异常，第一个是
`MaterialRegistrationGateTest.shortJsonFormIdsMatchNamespacedPrefixIds`。
`OreResourceTest` 已经按 **147** 矿断言，但过不了 `load()`。

Python `compare_gt6_recipes.py` / `build_gt6_veins.py` 允许
`t38_source_backed_acquisition_forms` 等 overlay 段超出 factual。Java 运行时没有读
这些 overlay 段。这是 **同一份 JSON、两套契约**。

本轮在 Java 加载器里并入了门闸 JSON 的 overlay 段：
`t38_source_backed_acquisition_forms`、`t38_required_forms`、`t39_required_forms`、
`t40_required_forms`。`compare_gt6_recipes.py` 在本轮才补上 `t40_required_forms`。
此前 T40 hydroxide `small_dust` 碰巧已在 factual 里，所以 Python `--check` 没先炸。

### 2.3 不能把 T38 矿脉并进 ore-chain 的 T5 分母

`build_gt6_ore_chain.py` 的 `worldgen_ore_materials` 权威是 **8 个 base 矿**
（copper、gold、iron、lead、nickel、tin、tungsten、zinc）。把 T38 acquisition 矿
union 进去会让 `crusher_t5_chemical` 从 **110 掉到 109**。T38 矿脉留在 worldgen
catalog，不进这条 T5 ledger。

测试 `test_t2_coverage_debts_are_committed_as_ledger` 现在区分：ore-chain ledger
= base 8；T38 矿可以存在且不必等于该 ledger。

### 2.4 门闸扩张边界（本轮遵守、但现场曾混淆）

T40 compact 需要的形态必须**已经在门闸里**。`build_t40_electrolyzer_recipes.py` 拒绝
往门闸加新 form。本轮只恢复 T38 acquisition overlay + T40 hydroxide `small_dust`
（3 个 `t40_required_forms`）。没有做一次“全量门闸扩张”。

`OreResourceTest` / GeneratedMaterialPack 按 **registered** 形态规划 stone/deepslate
矿块，不是按 L3 factual。T38 overlay `ore` 会进入运行时注册规划，即使材料 JSON 没有
`generates_ore`。

## 3. 构建器问题

### 3.1 T39 support 发现数 ≠ lock

`build_t39_player_path_support.py --write` 重新发现 **35** 条 route，lock 是 **34**。
生产 family 不得因此 resign。本轮改为按 lock 钉死 emit；顺序来自
`tools/t39_layered_player_path.json` 的 `support_routes`（拓扑序），不能按 lock key
排序。`mixer#17793` 依赖先有熔融铜/金的 smelter。

Lock `--check` 仍哈希 **routes 数组**。按 lock 再发出的 34 条 dump 配方无法复现原来的
`support.routes_sha256`（`150f5e3f…`）。sidecar 已不在工作树。最终只改了 lock 里这
一个字段，变为 `0d06098b…`。template keys / 32 relations 未动。

### 3.2 `--write` 默认会重算语义，闭卡经常只需要换哈希

多个 builder 没有“只重绑 owned_inputs 哈希”的模式。语义字段（family 数、gap、
winner、status）未变时，`--check` 仍因上游文件 sha256 失败。本轮对 T21
`t21_template_denominator.json`、T35 census/recipe_families、T39/T40 census lock
字段采用 **只补哈希、不 `--write` 整份 census 历史**。

例外：`build_t35_census_inputs.py --write` 本身就是输入哈希快照，可以写。写完后
`t35_census.json` 的 `currentness.owned_inputs` 立刻过期。

T21 chemical axis / source / template：`--write` 需要 `--full-replay`。compact
计数未变时只补 `inputs` 哈希。

T40 production lock：普通 `--write` 必须拒绝覆盖；resign 需要 `--approve-resign`。
本轮未 resign。Candidate 的 withdrawn 理由仍写着 “157-family”，以保持 lock
currentness；未对 candidate `--write`。

### 3.3 回执绑定面比 GameTest 本身更宽

T38/T39/T40 GameTest receipt 绑定：

- `MaterialRegistrationGate.java`
- `material_registration_gate.json`
- 各卡 recipe 树
- T39/T40 还绑 production lock、publication-group、shard、runtime dependency
  manifest

因此：**改 Java 门闸加载器 ≠ 配方行为变化**，但三张卡的 receipt `--check` 全挂。
正确顺序是 runtime manifest `--write` **之后**再 `--write --from-log`。本轮先写
receipt 再写 T39 runtime manifest，T39-Repair 报 `gametest_receipt_current`。

`--write --from-log tools/tXX_gametest.log` 只重绑哈希，不重跑 GameTest。配方行为
没变时这是合法收口。被 gitignore 的 `run-tXX-recipes/` 不是证据。

T40 第一次隔离 GameTest 因缺少 `cruciblecraft_t40:empty` 结构失败；补模板后 8/8
通过。后续 receipt 重绑用的是已有 UTF-8 log。

### 3.4 构建器 CLI 与默认写策略不一致

`t39_common.parse_write_check` / `t40_common.parse_write_check`：必须恰好一个
`--write` 或 `--check`。T39 runtime manifest 的 `main` 却是 “非 `--check` 即写”。
T39-Repair 是 `if args.write` 才写。同一家族脚本不能按同一套 argv 习惯调用。

`build_component_rules.py`：无 `--check` 即写 48 条规则 + manifest。门闸一变，
recipes profile 的下一步就是这份 manifest drift。

`compare_gt6_recipes.py --check --reference-only`：T38 矿一进门闸，compact
reachability fingerprint 出现一批 `*_ore` / `dust_div72` 的 removed 行。需要
`--update-baseline --reference-only`，不是全量 GT6 replay。

## 4. 改一块代码牵动的数据

门闸 JSON 或 `MaterialRegistrationGate.java` 任一变化，本轮实际扫到的过期面：

```text
material_registration_gate.json / .java
  -> T38/T39/T40 GameTest receipt
  -> tools/component_rule_manifest.json
  -> compare_gt6_recipes compact snapshot
  -> T39/T40 runtime dependency manifest（aggregate_root_sha256）
  -> T39 repair readiness / publication delta / load projection
  -> T40 publication delta / load projection
  -> T35 census_inputs（owned input hashes）
  -> T35 recipe_families.source_hashes（本轮是 t21_template_denominator.json）
  -> T35 census.currentness.owned_inputs
  -> T35 card_topology（嵌入 census sha256）
  -> T35 readiness（嵌入 census + topology sha256）
  -> T39 census_delta / card_topology / readiness 的 production_lock_sha256
  -> T40 card_topology.sequence[3] 里嵌着的 T39 lock sha256
```

T39 production lock 文件只改过 `support.routes_sha256`，但 census overlay 仍把整份
lock sha256 当 currentness。T40 topology 的 sequence 里抄了 T39 的旧 lock 哈希
（`cc3f673c…` → `b9034b29…`），T40 census_delta 本身已经是新值，所以
`--check` 失败点在 topology 而不在 T40 census。

没有机器可读的“哈希过期 vs 语义漂移”分类。`--check` 失败字符串长得一样。对
`t35_census.json`（约 23MB）跑 `first_json_diff` 时，若文件已损坏则
`JSONDecodeError`，报错退化成 `stale (on_disk_sha256=… rebuilt_len=…)`，没有 JSON
路径。

## 5. 测试直接改磁盘上的权威文件

`tools/tests/test_build_t35_census.py` 的
`test_corrupt_recipe_membership_fails_closed` 曾把
`tools/t35_recipe_families.json`（约 6.8MB / 17 万行）deepcopy 后写回同一路径，把
`counts.assigned_rows` 改成 1。`finally` 用 `t27_common.write_stable` 还原。

`t27_common.write_stable` 是对目标路径 **原地 `write_bytes`**。
`t35_common.write_stable` 是写 `*.tmp` 再 `os.replace`。本轮还原走 t27 路径，在
Windows 上对已存在的大文件报 `OSError: [Errno 22] Invalid argument`。try 里的篡改
已经落盘，后续同 class 的 `build()` 全部变成 `assigned_rows != 78682`。

同目录还有其他测试对权威 JSON `write_text`：

- `test_build_t35_census_inputs.py`（tamper 输出）
- `test_build_t35_excluded_object_reclaim.py`（`MACHINE_KINDS`）
- `test_build_t35_readiness.py`
- `test_build_t35_recipe_families.py`（写非法 JSON）

这些测试与 census profile 串行跑时，失败会污染后续 builder `--check`。本轮把
T35 census 的 corrupt 用例改成 mock `reference_only_check`，不再写那份大文件。

`git checkout -- tools/t35_recipe_families.json` 会丢掉工作区里已打上的
`t21_template_denominator` 哈希补丁。HEAD 上该字段曾是 `81bb61ac…`，工作区目标是
`883c43c5…`，中间还出现过 `c7d8b7d1…`。checkout 后必须重新对照磁盘文件哈希，不能
假设“还原 = 回到刚才的 currentness”。

## 6. 超大哈希 JSON 与 Windows 写失败

本轮碰到的体积（约数）：

| 文件 | 量级 | 在 currentness 中的角色 |
| --- | --- | --- |
| `tools/t35_census.json` | ~23MB | T35 聚合 census；被 topology/readiness 按文件 sha256 绑定 |
| `tools/t35_recipe_families.json` | ~6.8MB，17 万行 | T35 配方家庭；source_hashes + 被 census owned_inputs 绑定 |
| `material_registration_gate.json` | ~2.3 万行 | 几乎所有卡的运行时形态权威 |

Windows 上 `Path.write_text(..., newline='\n')` 对上述大文件多次
`OSError: [Errno 22] Invalid argument`。用 Python 整文件读成 str、替换哈希、再
`write_text` 曾把 `t35_census.json` 写成 **未终止字符串**（`JSONDecodeError` at
line 229407）。之后只能 `git checkout` 再 **按字节** `replace` 64 位哈希。

哈希补丁必须保持原文件换行与长度。`stable_json(build())` 与磁盘字节不等价时，
`--check` 会要求整文件重写；对 T35 census 那就是重写 23MB 历史聚合，和“不改写
T35–T39 census 历史”冲突。

`stale_error` 的 `first_json_diff(expected, actual)`：expected = 重建，actual =
磁盘。报错里先出现的哈希是 **builder 想要的值**。

## 7. 验证配置的隐性成本

- `recipes` 末尾的 `gradle:test` **不会** `--rerun-tasks`。若刚才用
  `gradlew test --tests SomeClass` 跑过子集，Gradle 会把整个 `:test` 标 UP-TO-DATE。
  本轮出现过 recipes 报 `gradle:test` 9 秒 PASS，实际上 710 个测试没重跑。过滤跑
  3 个门闸测试成功 ≠ 716 全绿。全量 `--rerun-tasks` 约 1 分钟，716/0。
- `census` profile 的 `gametest: true` 只 **打印** 需要
  `runGameTestServer -Pt35Census`，并不执行。
- `census` 会再跑一遍 `gradle test`（同样可被 UP-TO-DATE 骗过），再跑 T35 Python。
  `test_build_t35_readiness` 单独 118–162 秒。整个 census profile 约 4–5 分钟。
- `census-replay` 不跑 Gradle，本轮约 20 秒；它仍检查 T38/T39/T40 receipt，所以门闸
  Java 一改，这个 profile 也会先红。
- `recipes` 在 compare 失败处 **立刻退出**，不会继续跑后面已经绿的 T40 lock。一次
  门闸改动要多轮 “修一个 builder → 再跑到下一个 FAIL”。

## 8. 依赖缺漏与结构问题（只陈述，供检查）

1. **门闸双契约**：Python 构建器 + compare/veins 允许 T38 overlay `ore` 超出
   factual；Java `MaterialRegistrationGate.load` 在本轮修补前不读 overlay 段。
   没有测试把“committed gate 含非 factual `ore`”当作 Java 加载的正向用例（直到本轮
   给 arsenopyrite 加了断言）。
2. **compare / veins overlay 列表落后于门闸 JSON 段**：门闸已有
   `t40_required_forms`，compare 在本轮才加入；T8/T10 走材料 flag 解析而不是门闸
   段，三套规则并行。
3. **T5 化学分母与 T38 世界矿脉共用 ore-chain 构建器**，但分母字段
   `worldgen_ore_materials` 不是 worldgen catalog。两条线没有在类型上分开，只能靠
   注释和一条单测约束。
4. **T38 publication READY 依赖 ore-chain crush 行，crush 行依赖门闸 registered
   ore，registered ore 在 Java 侧又曾被 factual 卡住。** 三条链没有单一
   machine-readable “T38 overlay 已闭合”收据覆盖 Python+Java+ore-chain。
5. **GameTest receipt 把“门闸文件哈希”和“配方行为”绑在同一张证明上。** 加载器实现
   变化会作废 T38/T39/T40 三张卡的 PASS 收据，即使 log 里仍是 All N tests passed。
6. **Runtime dependency manifest 与 receipt 互相绑定，写入顺序是隐式协议**，脚本
   没有强制。
7. **T39 support 发现器与 production lock 不是同一权威。** `--write` 默认信任发现
   结果；lock `--check` 信任 routes 字节。两者冲突时没有“发现仅诊断、emit 必须 lock”
   的默认。
8. **T35 census / recipe_families 把上游文件 sha256 嵌进 23MB / 6.8MB 文档。**
   任意 compact 上游（本轮是 `t21_template_denominator.json`）变化都要补丁或重写
   这两份历史聚合。没有独立的 currentness sidecar。
9. **两套 `write_stable`。** 测试若 import `t27_common` 写 T35 大文件，Windows 上
   失败模式与 `t35_common` 不同。
10. **单元测试以权威产物为可变夹具。** 失败可留下 `assigned_rows=1` 的
    recipe_families，让整个 census profile 后续步骤失去意义。
11. **Gradle 过滤测试与 verify.py 的 `gradle:test` 共享同一 UP-TO-DATE 状态。**
    recipes/census 的 Java 绿灯可以是假的。
12. **Catalog / candidate / production lock 三集合在现场仍被混读。** 61/151 是
    T40 catalog fixture，不是 READY 分母；13/22 才是。T38 支撑 19/15 又是另一套
    publication 计数。本轮多次在“门闸 147 矿”和“T5 110”之间来回，因为没有一张表
    同时列出：factual L3 ore、gate overlay ore、ore-chain T5 八矿、worldgen
    catalog。
13. **T40 闭卡验证隐式依赖 T38 publication 与 T35 census currentness 全绿。**
    verification profile 没有“只验 T40 production lock，允许上游卡哈希待重绑”的
    通道。T40 R5/R6 无法在 T38 门闸 overlay 缺失时单独闭合。
14. **Agent 上下文压缩后，哈希补丁的“旧值”会过期。** 对 23MB 文件做错误的
    str replace 会损坏 JSON；`git checkout` 又丢工作区补丁。这不是产品契约，但是
    本轮真实损失了大量时间。

## 9. 本轮已落地、与拖延直接相关的改动（供对照，不是建议清单）

- Java 门闸加载允许上述四个 overlay 段；`MaterialRegistrationGateTest` 断言
  arsenopyrite 含 `ore`。
- T39 support emit 钉在 lock；lock 仅更新 `support.routes_sha256`。
- ore-chain `worldgen_ore_materials` 保持 8 个 base；测试不再要求 T38 矿属于该
  ledger。
- T35 census corrupt 用例改为 mock，不再写 `t35_recipe_families.json`。
- T38/T39/T40 receipt 按现有 log 重绑；T39/T40 runtime manifest 已按新门闸哈希重写。
- T35/T39/T40 census 相关文件只补了 currentness 哈希，未 resign T40 lock，未签发
  T41。
