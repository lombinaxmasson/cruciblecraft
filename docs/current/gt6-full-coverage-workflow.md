# GT6 全量覆盖：工作流与检测

[`gt6-full-coverage.md`](gt6-full-coverage.md) 是生成物。本页说明它怎么生成、各列按什么口径计数、
什么时候必须重跑，以及 CI 怎么拦截过期。

这份覆盖文档不是进度权威（进度看 [项目状态](project-status.md)），也不是移植队列
（队列看 [已阻塞项](blocked.md) 和形态需求普查）。它只把 GT6 分母和工作树现状并排放在一起。
配方源行总览采用三档互斥证据分级，不再把一个逐行 hash 百分比当作总体完成率。

## 1. 产物与命令

Builder：`tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py`

| 产物 | 内容 |
| --- | --- |
| `docs/current/gt6-full-coverage.md` | 人读页 |
| `…/coverage.json` | 全部轴的机器可读结果 |
| `…/chem_thermal.json` | 化学 / 热处理子集 |
| `…/source_attribution.json` | 配方源行归属钉（见第 3 节） |

```powershell
# 重建全部产物
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write
# 只检查，不写；CI 用这个
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --check
# 用本地 gt6_dump 重新核对归属钉，不写
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --verify-source
```

`--check` 和“配方证据没变”时的 `--write` 都不需要参考树。只有配方证据变了，`--write`
才需要本地 `gt6_dump/` 和 `tools/gt6_recipe_normalized_reference.json`。
dump 行 hash 缓存在 `build/gt6_full_coverage/`（gitignored），首次约 40 秒，之后约 15 秒。

## 2. 什么时候会过期

下列任一输入变化，页面就过期，`--check` 会失败：

- `tools/capabilities/**/capability.json`、`tools/blockers/catalog.json`、`tools/playtest/current_cycle.json`
- 运行时资源根里的配方 JSON（`src/main/resources` 加 `tools/generated_resource_roots.json` 列出的根），
  以及 `gradle/scripts/source-sets.gradle` 的 exclude
- `tools/component_rule_manifest.json`、`tools/waves/**/overflow.json`、形态需求普查 `census.json`
- `ModRecipeMaps.java`、`machine_delivery.json`、`*cover_definitions.json`、`EnergyType.java`、
  `material_prefixes/`、`mte_inplace_catalog.json`、`src/main/java` 下被证据扫描命中的类

自动重建的路径：

- `python tools/close_capability.py …` 关卡时，在 ledger 和 `project-status.md` 之后重建本页。
- `python tools/rebuild_artifacts.py` 按 builder policy 顺序重建（排在 `build_blockers` 之后）。

其他情况（recipe wave `--write`、`runData`、改 blocker、开新卡）改完后手动跑一次 `--write`，
和改动一起提交。

## 3. 配方覆盖的口径

### 3.1 扫描什么

扫描运行时真正加载的资源根：`src/main/resources` 加上 `generated_resource_roots.json` 的全部根，
其中包括 datagen 的 `src/generated/resources`。`source-sets.gradle` 里
`sourceSets.main.resources` 的 `exclude(...)` 会被解析并照样排除，例如
`player_path_recovery/roaster/**`。

计三种配方：

| 类型 | 目标图字段 | 计数单位 |
| --- | --- | --- |
| `cruciblecraft:gt_recipe` | `map` | 1 行 |
| `cruciblecraft:compact_gt_recipe_family` | `target_map` | 每条 `relations`，或 `matrix_v1` 的每个 `rows` |
| `cruciblecraft:material_rule` | `target` | 1 条规则（展开数另计） |

`compact_publication_policy` 是加载策略，`compact_dedup_rule` 只删 player-path 重复行，
都不改变源行覆盖，所以不计。CC 行数是静态文件行数，没有扣除运行时 dedup。

### 3.2 已追溯源行

一条 CC 配方行上的 `evidence_hashes` 如果是 64 位 hex，它就是某条 GT6 dump 行的
`sha256(json.dumps(row, sort_keys=True, separators=(",", ":"), ensure_ascii=False))`。
这个 hash 归哪张 GT6 图，按下面顺序取：

1. `selected_source_recipe` 形如 `gt.recipe.<map>#NNNN`（`#NNNN` 是模板号，不是行号）
2. `selected_source_recipe` 形如 `gt6_dump/…/maps/<map>.json#recipes[i]`（同时记下行号）
3. 文件的 `family_id` 形如 `gt.recipe.<map>#NNNN`
4. 都没有时，取 CC 目标图同名的 GT6 图

GT6 会把大量相同的行同时注册进几张图：melter 和 smelter 有 6,743 行相同，
compressor/rollingmill、mortar/shredder 也有重合。所以**只要声明的图里有这条行，就归声明图**。
只有声明图里没有、而恰好另一张图有时，才写进 `source_attribution.json` 的 `full_overrides`。

每张 GT6 图按不同 hash 去重计数，与“源行”同单位。`full_replay` 只在
“已追溯源行 = 源行数且 overflow = 0”时成立。

### 3.3 三档源行证据分级

总览中的三档是互斥分区，严格加总到 GT6 源行分母：

1. **`source_exact` / 逐行已证明**：已经用 dump 行 evidence hash 对上的源行。
2. **`implementation_evidence_pending` / 已有 CC 实现证据、待语义确认**：该图已有
   CC 配方行、材料规则或归一化 reference，但剩余源行还没有逐行语义比对。
   这是“可能已经覆盖但证据不足”的桶，不能直接当完成。
3. **`no_cc_evidence_or_legacy_pending` / 无 CC 证据或旧排除待决策**：
   `denominator_only`、`runtime_only` 和 `legacy_exclusion_pending` 的源行。
   其中旧排除单独列出，避免和真正没有 CC 证据混淆。

因此页面里的三档不是三个完成度百分比：第一档是下界，第二档是待确认区，
第三档是明确缺口与范围决策区。reference 行、材料规则展开数和无 provenance 的
CC 行会作为第二档的辅助信号单独显示，不会伪装成 GT6 源行数。

### 3.4 其他三列

- **reference 追溯**：ore-chain 的 20 位短 hash 指向 `gt6_recipe_normalized_reference.json` 的归一化行。
  归一化会合并多条 dump 行，所以单列显示，不与已追溯源行相加，也不参与 `full_replay` 判定。
- **CC 未追溯行**：没有行级 GT6 evidence 的 CC 行，例如 datagen 手写、`gt6_java_source`、
  bootstrap、design policy。它们只证明这张图有内容。
- **材料规则**：规则文件数，以及 `component_rule_manifest.json` 里的离线展开数。
  datagen 规则的展开数由运行时决定，这里不估算。

每个 CC 图的未追溯行和规则只归到一个 GT6 图，避免重复计入。没有 GT6 归属的 CC 图
列在第 2.1 节，不会被静默丢掉。

### 3.5 新 wave 要怎样才会被计入

要让一条 CC 行计入“已追溯源行”，它必须带上：

- `provenance.evidence_hashes`：GT6 dump 行的 64 位 sha256（算法同 3.2）；
- `provenance.selected_source_recipe`：`gt.recipe.<map>#NNNN` 模板号或 dump 路径，
  或者文件级 `family_id` 带上模板号。

`recipe_bulk` 的 `emit.py`/`matrix.py` 已经这样写。手写或 datagen 的 `gt_recipe` 如果不带
provenance，只会落在“CC 未追溯行”，永远达不到 `full_replay`。这不是报告的 bug，而是证据缺失。

### 3.6 归属钉与 CI

CI 上没有 `gt6_dump`。`source_attribution.json` 记录了：

- `evidence_digest`：全树所有 `(声明图, hash)` 与 dump 行号引用的摘要；
- `full_overrides` / `short_overrides`：声明图里没有该行时的实际归属；
- `unverified_*_hashes`、`ambiguous_hashes`：dump 或 reference 里找不到、或无法唯一归属的 hash，
  这些不计数；
- `dump_refs`：`maps/<map>.json#recipes[i]` 对应的行 hash，用于和 hash 引用去重。

`--check` 会重算 `evidence_digest`，和钉不一致就失败，并提示去有 dump 的机器上跑 `--write`。
所以只要配方证据有增减，CI 都会拦下来，不会用旧的归属静默算出新数字。

## 4. CI 与测试

- `tools/verification_builder_policy.json` 的 `build_gt6_full_coverage` 挂在 `capability-runtime`
  profile，CI 每次都会跑 `--check`：

  ```powershell
  python tools/verify.py integration --profile capability-runtime
  ```

- `tools/tests/test_gt6_full_coverage_reassessment.py`（`verification` profile）只读产物，
  检查分母完整性、单位不相加、以及第一版报告出过的错误：漏扫 datagen 根、漏读 matrix 行、
  按文件数判 `full_replay`、燃料图名错配、同形行跨图泄漏。

`--check` 失败时：

| 输出 | 处理 |
| --- | --- |
| `… is stale` | 跑 `--write`，把产物和改动一起提交 |
| `source_attribution.json does not match the recipe tree` | 在有 `gt6_dump/` 的机器上跑 `--write` |
| `local GT6 source revision differs from frozen scope` | 本地 `gt6_code` 不是 `scope.json` 的 revision；先对齐参考树，不要改 scope |

## 5. 各轴哪些会自动更新

| 轴 | 来源 | 自动更新 |
| --- | --- | --- |
| 配方源行、CC 行、规则 | 运行时资源根 + 归属钉 | 是（证据变化时需 dump） |
| 机器 kind / 多方块证据 | capability、Java、`machine_delivery.json`、多方块结构 | 是 |
| 盖板 | `*cover_definitions.json` | 是 |
| 能量 | `EnergyType.java` | 是 |
| 材料前缀映射 | `tools/gt6_resolve.py` → `material_prefixes/` | 是 |
| MTE 身份 | R0 账本 + `mte_inplace_catalog.json` 叠加 | 叠加部分是 |
| capability / blocker / 试玩 / 形态普查 | 各自 ledger | 是（普查本身由 census builder 刷新） |
| 各轴“历史源分母分类” | `tools/machine_tree_denominators/*.json` | 否，冻结分母 |
| 物品/流体生成域的“CC 域” | `itemgenerator_domains.json` | 否，冻结分母 |

冻结的列在页面上已标成“历史”或“冻结分母”。它们只能说明 GT6 有什么、当年怎么分类，
不能说明 CC 现在做到哪里。

## 6. 改写本页或 builder 时的规则

- 新增的每一列都必须从被跟踪的 live 来源算出来，或者明确标成冻结分母。
  不要往 builder 里写只能手工维护的数字；只有人能判断的事（例如试玩签收），链接过去，不要抄进来。
- 如果某个缺口没法从工作树自动判断，先改证据来源（让 wave 写出 provenance、让目录记录宿主），
  不要在本页加手写表。
- `LOCAL_MAP_ALIASES`、`CC_MAP_GT6_OWNERS` 这类别名只能根据行级证据或 normalized reference 添加，
  并在注释里写明依据。
- 各节单位不同，不得相加；也不要用本页当 capability 验收或 `player_complete` 证据。

## 7. 已知局限

- datagen / 手写 `gt_recipe` 大多没有 provenance，只能算“未追溯”。要计入，需要在各自的卡上补 evidence。
- 材料规则只有 component rule 有离线展开数；其他规则的展开数要到运行时才知道。
- reference 追溯和 dump 精确追溯无法互相去重（例如 shredder 两列都有值），所以分开显示。
- blocker 与配方图的关联按图名子串匹配，只作提示。
