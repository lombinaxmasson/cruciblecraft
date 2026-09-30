# GameTest 数量断言派生化详细计划

> 计划 slug：`tooling/gametest-derived-counts`
> 状态：active。本文件位于 `card-plans/active/`。
> 性质：把 GameTest 里手抄的配方行数、目录大小、发布快照换成从真实来源派生，
> 并把「数字变了必须列 id」做成工具。不改配方、不改生成器输出、不开形态。
>
> 开工条件：[GameTest 红灯清理](../closed/GameTest红灯清理详细计划.md) 已于 2026-09-30 关卡。
> 红灯清理期间照旧用「列 id 再改数字」的办法，不提前套用本卡。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                  = active
capability_slug       = tooling/gametest-derived-counts
unique_active_wave    = tooling/gametest-derived-counts
prep_owned_paths      = 本文件；tools/scratch/**（盘点脚本，gitignored）
landing_owned_paths   = src/test/java/com/masson/cruciblecraft/gametest/**（只动数量断言）；
                        src/main/resources/data/cruciblecraft/*_publication_baseline.json；
                        tools/waves/tooling/gametest-derived-counts/**（id 差异工具与测试）；
                        python_test_policy.json（登记新 Python 测试）
landing_depends_on    = tooling/gametest-red-cleanup 关卡
promotion_trigger     = R0 盘点结论经用户确认后，转 unique-active
```

---

## 0. 开场判断

2026-09-30 红灯清理中，化学 168、织布机 476、熔炉 3547、回收 1817、层压机 492、
发布快照都是「测试里的数字没跟上已提交的配方」，不是运行时 bug。

现状分三层：

```text
加载器    CompactPublicationPolicy.validateLiveSources 已经逐族比对
          family_count、relation_count、membership_root_sha256，对不上直接抛异常。
          所以「紧凑族实际行数 == 策略 relation_count」开服时已强制。
GameTest  又把同一个数字抄成字面量（层压机测试 492、策略 486，就是这么漂的）。
          还把整张图、全局快照锁死，别的卡加一族配方，无关测试就红。
基线链    13 个 *_publication_baseline.json，其中 9 个带手写 *_publication_delta，
          GameTest 用「基线 + 各波 delta 之和」比全局行数，本质也是冻结账。
```

结论：运行时防漂已经在加载器里。GameTest 里的字面量大多是重复记账。
真正缺的是生成器输出变化时，能机械列出多了、少了哪些 stable id 的工具。

## 1. 盘点（2026-09-30，正则粗扫，R0 复核）

`(size|logicalRecipeCount|Recipes|ROWS) == 三位以上数字`，14 个文件 36 处：

| 类别 | 例子 | 处理方向 |
|---|---|---|
| a. 紧凑族行数 | 织布机 476、层压机 492、熔炉 3547、洗矿 189 / 13519、团簇轧机 307、注入机 103、压力清洗 312、石头熔炼 407 | 删字面量。改为断言族存在且 `logicalRecipeCount()` 等于该族策略的 `relation_count`（一行派生，不再手抄） |
| b. 整张图行数 | 织布机整图 1166、层压机整图 492、注入机整图 103 | 改为「该图所有策略 `relation_count` 之和 + 该图具体 JSON 数」，或直接删掉整图断言，只留本族 |
| c. 具体 JSON 计数 | 化学 168 | 用 `ResourceManager.listResources` 数 `recipe/chemical/**`，与 live 条目逐一对账 |
| d. 目录 / 注册表大小 | 储物 625 / 624、机器 314 / 312、粉块 963、岩层 131、矿脉 137、洗矿残余 283 | 能从目录 JSON 派生就派生；GT6 固定事实保留字面量，并在旁边注明 GT6 文件与行号 |
| e. 几何常量 | 5×5×5 罐 125 | 保留，写成 `5 * 5 * 5` |
| f. 全局发布快照 | `componentRecipes`、`allPublishedRecipes` 等 7 个数 | 改为从策略文件与具体 JSON 求和派生；或者退役，见 D1 |
| g. 基线链 | kinetic / heat 基线 + 9 个 delta | 见 D1 |

另有组件图逐图 `authoredEntries` 期望表（挤出 240 … 冲压 1315，合计 14708），
归入 c 类处理。

红灯清理留下的临时状态，本卡要收掉：

- `LoomGameTests` 整图断言 1166（476 试点 + 690 `loom/chemical_misc`），把织布机测试和化学杂项族绑在了一起。
- `everylivecomponentrecipetracestogeneratedcomponentjson` 快照已按 `abdb2bb61` 之后 30 个新族手工改到 583471。

## 2. 要用户拍板

```text
D1  基线链怎么处理
    甲  退役：删 kinetic/heat 基线比对与 9 个 delta，全局数改为派生求和
    乙  保留文件，但改为生成物：builder 从策略目录重算，--check 防手改
    Python 侧 tools/rebuild_artifacts.py 也读这些基线，R0 先查清它的用途再定

D2  重载耗时断言（现 20 s 验证线）是否移到 measurement 组
    测试注释已写「Reload time is portfolio/publication-reload-performance, not this grid」，
    但断言仍在 default 组，冷 JVM 抖动会让无关的计数测试一起红

D3  d 类目录大小：保留「GT6 事实 + 行号注释」的字面量，还是一律派生
```

## 3. 做法

### R0 盘点（prep 内做，不改 src）

- scratch 脚本逐条列出 36 处字面量与全局快照：所在测试、类别、候选派生来源、是否已有 GT6 行号可引。
- 查清 compact 族的 stable id 存在哪些生成 JSON 里，供 R1 工具读取。
- 查 `tools/rebuild_artifacts.py` 对基线文件的依赖。
- 结论写进本文件 §6，交用户确认 D1–D3。

### R1 id 差异工具

`tools/waves/tooling/gametest-derived-counts/policy_delta.py`：

```text
输入   git 修订（默认 HEAD）与工作区
输出   每个 publication_group：relation_count 旧→新、membership 哈希是否变化、
       新增 / 删除的 stable id（可截断，给出总数）
用途   提交前人工核对；红灯清理里手写的 _tmp_policy_diff.py 就是它的原型
```

配套 Python 测试登记到 `python_test_policy.json`（`active`）。

### R2 a 类：紧凑族

逐个测试把 `== 476` 这类字面量换成「本族策略 `relation_count`」派生值。
测试方法名不改（如 `sharedMapPublishesFourHundredSixtyFiveRows`），保持测试 id 稳定。

### R3 b / c / f 类

整图与全局数改为求和派生，或按 D1 退役。化学改为资源清单对账。

### R4 d / e / g 类

按 D2、D3 落地。基线链按 D1 执行。

### R5 验证

- 过滤开服跑受影响的全部测试名（方法同红灯清理计划）。
- 全量 `python tools/verify.py release` 一次。

## 4. 不做

- 删测试方法，或把 `==` 放宽成 `>=` / 区间
- 接受 compact `gt6/<hash>` id 顶替具体配方 id
- 让 GameTest 读生成器输出后再「重算一遍」当防回退：生成器是否正确归 Python 侧 builder `--check` 与 GT6 对照测试管
- 改配方、改生成器、开形态；数字对不上时仍按红灯清理的规矩列 id
- `git checkout` / `restore` / `reset --hard` / `clean` / `stash`；碰参考树

## 5. 验收

- [x] D1–D3 已拍板
- [x] `policy_delta.py` 能列出 `abdb2bb61` 以来的 30 个新族，以及 `dd4ea9892` 从织布机试点删掉的那 1 个稳定 id
- [x] a 类没有剩下与策略 `relation_count` 相同的手抄字面量。退役挤出族 `logicalRecipeCount() == 0` 留下
- [x] D3：和目录同一份数据的数字已删；粉块与矿脉没有第二份清单，数字已删、类型检查留下；5×5×5 写成乘积
- [x] 织布机试点只锁 `loom/pilot/loom`。兄弟族 `loom/chemical_misc` 已在活策略里，试点测试不提它。见 `PublicationFamilyIsolationTest`
- [x] 过滤开服与全量 release 各通过一次。全量 `verify.py release` 为 PASS；多方块格 Gradle 退出码是 1，因为关服卡住后进程被停掉，日志仍是 59 个测试全过

## 6. 记录区

### R0（2026-09-30，未晋升）

盘点脚本（gitignored）：`tools/scratch/gametest-derived-counts/r0_inventory.py`。
扫的是工作区，不是 HEAD。红灯清理改过的数字还没提交，本卡不还原、不代提交：

- `CrucibleCraftGameTests`、`LoomGameTests`、`LaminatorGameTests`、`MelterGameTests` 有未提交 diff
- `prefix_regular_publication_baseline.json` 仍是未跟踪文件
- 织布机 HEAD 是 465，工作区是 476。净增 11，对应验收里的稳定 id +12 / −1

计划正文写的「14 个文件 36 处」是当时的行内正则。工作区里同一类锁更多，因为大量数字已经收进 `LOCKED_RELATIONS` / `LIVE_ROWS`。下面按处理方向记，不把流体转移量、结构等待 tick、机器 meta id 算进来。

#### 紧凑族字面量（a，以及整图 b）

策略目录 `src/recipe_generated/.../recipe/publication_policy/` 现有 **172** 份。
`relation_count` 合计 **558338**。`eager_stable_ids` 非空的只有 1 份，全量稳定 id 不在策略文件里。

稳定 id 在紧凑配方 JSON 的 `relations[].stable_id`，用同文件的 `publication_group` 归组。
成员哈希就是 `CompactPublicationPolicy.membershipRoot`：族 id 排序、稳定 id 排序、逐行换行、SHA-256。
R1 读这两处即可，不必开服。

和策略对得上的手抄数（族断言改成读该族 `relation_count`；整图断言删掉，避免把兄弟族绑进来）：

| 测试 | 手抄 | 策略 |
|---|---|---|
| 织布机族 | 476 | `loom/pilot/loom` = 476 |
| 织布机整图 | 1166 | 476 + `loom/chemical_misc` 690。这就是红灯清理留下的跨族绑定 |
| 层压机族 | 486 | `laminator/pilot/laminator` = 486 |
| 层压机整图 | 492 | 486 再加 6 条具体红石缆配方。未提交注释已写明，数字仍是手抄 |
| 熔炉 | `LIVE_ROWS` 3547 | `melter/pilot/melter` = 3547。整图与族是同一个数 |
| 洗矿 | 189 / 13519 | 对应 bath remainder 两族。同文件还有 13708、1517 等常量，四份洗矿测试互相抄 |
| 团簇轧机 | 307，整图再写一遍 307 | `clustermill/pilot/cluster_mill` |
| 注入机 | 103，整图再写一遍 103 | `injector/pilot/injector` |
| 压力清洗 | 族 312；整图是 `312 + woodRows` | 族改派生。整图留下「派生族行数 + 现场木板行」，不再出现 312 |
| 石头熔炼 | 407，出现 4 次 | `smelter/stone` = 407 |
| 回收 | `FAMILY_COUNT` 1817，另有材料组 81 | 81 份 `smelter/deferred_recycling/*`，`relation_count` 之和正好 1817 |
| 打磨 | `LIVE_ROWS` 7637 | `sanding/pilot/sanding` |
| 挤压机批量 | `LIVE_ROWS` 299143 | `target_map=extruder` 的策略之和 |
| 其余同形常量 | 组装木板 242 / 防火 144、粉块对象 379 / 271 / 108、纳米 7、辊压 26、切片 33、榨汁机 15、烘烤 73、组装紧凑 50、离心 19 / 13、电解 11 | 各自策略的 `relation_count`。验收「a 类不留手抄」把这些两位数也算上，不只正文那 36 处 |

纳米加工机只锁了试点族 7，没有锁 `nanofab/chemical_misc` 的 55。这是「新族只碰自己的测试」已经成立的样子。织布机整图 1166 是反例。

#### 具体 JSON 与组件图（c）

- 化学 168：`everyLiveComponentRecipeTracesToGeneratedComponentJson` 已经逐条对 `recipe/<path>.json`，最后再用 168 封口。封口删掉，留下清单对账。快照里的 `chemicalPublishedRecipes == 168` 是同一个数。
- 组件图期望表（挤出 240 … 冲压 1315，合计 14708）同样删掉。逐条「活配方对得上 JSON」留下。不要换成另一份手写总数。

#### 目录大小（d）与几何（e）

这些测试目前都没有 GT6 文件行号。

| 字面量 | 另一份清单 | 行号 |
|---|---|---|
| 储物 625 / 624 | `storage_variants.json`。加载器里已有 `STORAGE_COUNT = 624` | 无。GT6 没有一行写着 624 |
| 机器 314 / 312 | `MachineTierCatalog.entries()`，`StorageGameTests` 与 `MachineRuntimeGameTests` 各写一遍 | 无 |
| 粉块 963 | 只出现在 `PrefixPackGameTests` | 无 |
| 岩层 131 | `stone_layer_rocks.json` 的 `layers`。加载器照数组长度建表 | GT6 `StoneLayer.java:136` 只是空的 `LAYERS` 列表，不是数字 131 |
| 矿脉 137，其中 profile v2 为 132 | 运行时 `large_*_vein` 特征数。失败信息仍写着「/ 129」，说明这数字漂过 | 无 |
| 洗矿残余 283 | `bath_remainder_identity_catalog.json` 的 `variant_count`。加载器再锁一遍 | 无 |
| 5×5×5 罐 125 | 几何 | 写成 `5 * 5 * 5` |

`Tank5x5GameTests` 里的 17042 / 17065 是机器 meta id，不是行数，不动。

#### 全局快照与基线链（f / g）

磁盘上有 **14** 份 `*_publication_baseline.json`，不是正文写的 13。
带 `*_publication_delta` 的是 **10** 份（Java `registeredPublicationDelta` 的名单）。
`cover` 与 `energy` 有 `publication_totals`，没有 delta，也没有任何测试或 Python 读取。

动能 / 热能基线的 `logical_rows` 都是 311528。十份 delta 的 `logical_rows_added` 之和是 271943。
311528 + 271943 = **583471**，就是快照里的 `allPublishedRecipes`。
同一次快照还冻着 `componentRecipes` 348432、`toolRecipes` 4119、`chemicalPublishedRecipes` 168、`mortarAuthoredMaterialRules` 231、`pipeMaterialRules` 238、`ingotFormMaterialRules` 967、`liveComponentMapRecipes` 352552、`eagerPublishedRecipes` 88561、`lazyLogicalRecipes` 494910。

策略 `relation_count` 之和 558338 小于 583471。差额是具体 JSON 配方，不是又一份基线。
加载器已经用「具体行 + 族逻辑行」算出 `allPublishedRecipes`，并用每族 `relation_count` 对过活数据。
GameTest 再求和，就是把生成器输出重算一遍，正文第 4 节禁止拿这个当防回退。

`tools/rebuild_artifacts.py` **不读**这些基线。它只在结束时打印一句警告，点名的是旧的 `t1x_publication_baseline.json`。
Java 注释里的「Python 侧同一套 publication-delta 助手」在当前树里没有对应函数。
会写 `later_wave_publication_baseline.json` 的是已关卡 `tools/waves/machines/hammer-squeezer-laser/build_squeezer_dump.py`。
两份已关 capability 的 `owned_paths` 列了 `chemical_misc_bulk` 与 `steamcracking_bulk` 基线，那是历史路径，不是运行时读者。

基线测试里不该跟数字一起删的部分：配方图 id 是否仍被活图包含、EMI 枚举是否与活机器自洽、动能 / 热能获得格的合成 id 清单。
EMI 图 id 现在是与基线文件精确相等，加一台新机器会红。那是另一把锁，甲落地时改成「EMI 计划覆盖活的已配置机器」，不再对一份冻结 id 表。

重载耗时在 `everyLiveComponentRecipeTracesToGeneratedComponentJson`（`cruciblecraft_default_grid`）前半，断言 `reloadMillis <= 20_000`（`ModProcessingMachines.VERIFICATION_RECIPE_RELOAD_BUDGET_MS`）。
同方法后半还有 `indexMillis` 与 lookup p95。measurement 组是 `tools/gametest_grids.json` 里的 `cruciblecraft_scale` 与 `cruciblecraft_census`。

### 已确认（D1–D3，用户选了建议项）

```text
D1  甲。退役动能/热能 publication_totals、10 份 delta，以及 cover/energy 两份无人读的旧账。
    全局 7 个数删掉，不在 GameTest 里重算。
    配方图包含关系、EMI 自洽、获得格合成 id 留下。
    已关的 build_squeezer_dump.py 停止改写 later_wave 基线。
    乙不取：生成基线仍会在无关卡的 diff 里改动，过不了「新族只碰该族测试」。

D2  移。reloadMillis、indexMillis、lookup p95 拆到 measurement 组。
    计数和对 JSON 留在 default。不改 20 秒预算。

D3  不一律派生。和策略或资源清单对得上的改派生。
    和被测集合是同一份数据的（储物 JSON、岩层 JSON、洗矿残余 JSON）删掉测试里的重复数字，
    只留「注册表包含这份目录」；加载器里的那一份常量不动（不在本卡落地路径里）。
    粉块 963、矿脉 137 / 132 没有第二份清单，也没有 GT6 行号，数字删掉，
    已有的「每条都是预期类型」留下。
    5×5×5 写成乘积。
```

D1–D3 已按上面三条落地，本卡占 unique-active。
`policy_delta.py` 比较两个 git 修订（默认 HEAD 与工作区）。
`abdb2bb61` 到当前工作区是 30 个新族、0 个删除。
织布机试点的「+12 / −1」是测试数字从 465 到 476 的故事，不是同一次稳定 id 差。
`f8b528aa9` 到 `8514d9805` 行数 +12，同时改写了 id（+101 / −89）。
`dd4ea9892` 只从 `loom/loom/gt_recipe_loom_0000.json` 删掉 1 个稳定 id（477 → 476）。
工具验收锁的是这 30 个新族，以及 `dd4ea9892^` 到 `dd4ea9892` 的那一次删除。
