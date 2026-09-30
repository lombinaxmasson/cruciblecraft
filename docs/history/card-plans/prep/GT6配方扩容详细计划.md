# GT6 配方扩容详细计划

> 计划 slug：`recipe/gt6-recipe-capacity-expansion`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 配方扩容
> 性质：给后面的配方卡腾地方。在约 60 万逻辑行上做同一次冷启动实测，并在不过同步硬顶的前提下抬高 lazy 上限。本卡不导入 GT6 配方。
> 上一张容量门：[GT6 批量配方容量门](../closed/GT6批量配方容量门详细计划.md)（`recipe/gt6-bulk-capacity`，lazy 硬顶 500,000，只包当时后三张配方卡）。
> 改 load 硬顶：按 [能力交付流程 §8](../../../current/capability-delivery-workflow.md) 不走 prep 实施分支。签发不等于开工。

```text
lane                         = prep
capability_slug              = recipe/gt6-recipe-capacity-expansion
unique_active_wave           = null
prep_owned_paths             = 本文件
landing_owned_paths          = ModProcessingMachines.java（lazy 逻辑行硬顶；不抬同步字节硬顶）；
                               tools/recipe_bulk/ordinary_wave.py；
                               tools/recipe_load_load_budget_policy.json；
                               若 60 万行冷启动同步超过 64 MiB：登录同步路径
                               （GTRecipeMapLoader 所计的真实同步字节，以及客户端按需取配方或等价压缩）；
                               冷启动测量夹具（测试源集，不进 live datapack）；
                               docs/current/recipe-wave-workflow.md §6
landing_depends_on           = 当前 unique-active 空窗。落地时直接占 unique-active。
                               后继配方导入、形态开门、crate 都等本卡关闭
partial_close_allowed        = false
```

---

## 0. 开场判断

现行全局 lazy 逻辑行硬顶是 500,000（`ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING`，
`LAZY_LOGICAL_HARD_CEILING`）。同步硬顶是 64 MiB（`RECIPE_SYNC_BUDGET_BYTES` =
67,108,864 字节）。单 holder 4,096 行、单 shard 128 条 relation、cache 总和 4,096、
每个新 family 的 cache 声明不超过 16，这些本卡先不动。

后面还要进的配方已经顶着这道门：

| 口径 | 行数 | 来源 |
| --- | ---: | --- |
| 现在的惰性逻辑行 | 494,910 | 开卡基线。上次冻在已关的 GameTest 数量断言里的 `lazyLogicalRecipes`。开工时用本卡同一次冷启动重记 |
| 已经能翻译、只差配方 | 39,033 | [gt6-full-coverage.md](../../../current/gt6-full-coverage.md) 逐行分类 `translatable_missing` |
| 两者相加 | 533,943 | 只够把当前可翻译缺配方放进来 |
| 本卡实测负载 | 约 600,000 | 开形态、做 crate 还会再涨。按这一档测，不按下一张配方卡的缺口测 |

惰性行本身已经是 494,910。再放入 39,033 行可翻译缺配方就是 533,943，超过现行 500,000。缺身份以后变成配方、以及总计划里暂缓的 crate，还会再高。所以这张卡的实测负载是约 60 万逻辑行，关闭后写出的新 lazy 硬顶至少覆盖这一档。超过这次实测通过的负载时再另卡。

风险在同步体积。开卡时登录同步大约 58 MB，硬顶是 64 MiB（67,108,864 字节）。仓库里没有一份已提交的 58 MB 测量文件。若体积随行数线性涨，60 万 / 494,910 × 58 MB ≈ 70 MB。按十进制兆字节或 70 MiB 计，都已经大于 67,108,864 字节。只把 lazy 硬顶改成 600,000，会在同步门上失败。

上一张容量门的合成测量不能代替这次：`BulkCapacitySyntheticLoadHarness` 把 50 万行 `matrix_v1` 编成大约 20 MB，测的是夹具编码，不是现在这套 live 发布的登录包。`estimateStubPayloadBytes` 也不算同步证明。

## 1. 要回答的问题

同一次冷启动、大约 60 万逻辑行（现有 live 发布，加上测试源集里的合成行，不写进 live datapack）：

1. 重载、真实同步字节、查找 p95 各是多少？三个数必须来自同一次启动，不能各取最好的一次。
2. 同步若超过 64 MiB，登录包怎么缩小，使客户端仍能按查询拿到同一套逻辑配方？
3. 通过之后，新的 lazy 硬顶写多少？它必须覆盖约 60 万行，并且不抬高 64 MiB 同步硬顶。

客户端按需拿配方是同步超顶时的预期做法。等价的压缩也可以，只要同一次冷启动满足第 3 节，并且机器查询和 EMI 看到的逻辑集合不比现在少。把 `RECIPE_SYNC_BUDGET_BYTES` 改大不算答案。已关的紧凑配方传输把「绕开 `update_recipes` 的自定义清单」留过以后；本卡只在 60 万行实测过不了 64 MiB 时把它做掉，不为了换协议而换协议。

## 2. 实施

落地时直接占 unique-active，不走 `prep/<slug>` 实施分支。

1. 先用与关闭相同的冷启动路径记下当前 live 发布的惰性行、重载、真实同步字节、查找 p95。这组数替换开卡时的 494,910 与约 58 MB；线性风险用这组数重算。
2. 在测试源集把逻辑行加到约 600,000。合成行不进 live datapack，不注册成玩家配方。形状沿用现有批量图的 `matrix_v1`，单 holder 仍不超过 4,096 行。
3. Dedicated server 加客户端做这一次冷启动。集成单人不能代替 `update_recipes`。
4. 同步超过 64 MiB，或重载超过 15 秒，或查找 p95 超出现行预算时，在本卡里改同步或加载，直到同一次冷启动同时过门。不要把架构方案留到下一张卡，也不要删已有配方来过门。
5. 过门之后把新 lazy 硬顶写进 `ModProcessingMachines`、`ordinary_wave.py`、`recipe_load_load_budget_policy.json` 与 [recipe-wave-workflow.md](../../../current/recipe-wave-workflow.md) §6。策略文件里的 lazy 硬顶仍是容量门之前的 56,000，落地时与 Java 常数对齐。同步硬顶保持 64 MiB。

单 holder、单 shard、cache 总和、每个 family 的 cache 声明，只有在这次 60 万行测量里它们本身挡住第 3 节时才改。

## 3. 关闭条件

同一次冷启动，负载约 600,000 逻辑行，三个数一起记：

| 门 | 条件 |
| --- | --- |
| 重载 | ≤ 15 秒 |
| 同步 | ≤ 64 MiB。不抬 `RECIPE_SYNC_BUDGET_BYTES` |
| 查找 p95 | 这次冷启动 ≤ 3 ms（`VERIFICATION_RECIPE_LOOKUP_P95_BUDGET_NS`）。生产门仍是 2 ms（`RECIPE_LOOKUP_P95_BUDGET_NS`）。两道门都保持原值 |

现有生产重载预算是 10 秒，冷启动验证包络是 20 秒（冷 JVM 曾记到 14,556 ms、16,112 ms、16,516 ms）。本卡的关闭门是 15 秒。20 秒包络不算这次通过。

同步字节是登录路径上 `GTRecipeMapLoader` 拿去和 `RECIPE_SYNC_BUDGET_BYTES` 比较的那一笔（挤压机与其余 compact family 的真实载荷之和）。若这笔记账和 dedicated `update_recipes` 的线上字节不一致，关闭以线上字节为准，并让记账与线上一致。

## 4. 验证

```powershell
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile capability-runtime
```

外加第 3 节那一次 dedicated 冷启动。`runGameTestServer` 可以守查找正确性，不能代替这次同步测量。

## 5. 明确不接管

- 任何 GT6 配方导入、形态开门、crate
- 把同步硬顶改到 64 MiB 以上
- 用删配方、stand-in 配料或缩短逻辑集合来过门
- 用 2026-09-25 的 50 万行合成编码，或 `estimateStubPayloadBytes`，充当本次同步证明

## 6. 关闭清单

- [ ] 当前 live 发布的冷启动基线（惰性行、重载、真实同步字节、查找 p95）
- [ ] 约 60 万逻辑行的同一次冷启动：重载 ≤ 15 秒，同步 ≤ 64 MiB，查找 p95 在现行预算内
- [ ] 新 lazy 硬顶写入 Java、`ordinary_wave.py`、负载策略与工作流 §6，且不低于这次实测负载
- [ ] 同步硬顶仍是 64 MiB
