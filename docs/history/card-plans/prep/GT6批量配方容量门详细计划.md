# GT6 批量配方容量门详细计划

> 计划 slug：`recipe/gt6-bulk-capacity`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 正式名称：GT6 批量配方容量门
> 性质：在批量配方卡之前，测量并重定集成加载、同步与查询的容量上限。本卡不导入 GT6 配方。
> 总计划第 4 张落地卡，见 [GT6 批量移植总计划](GT6批量移植总计划.md)。
> 改 load 硬顶：按 [能力交付流程 §8](../../../current/capability-delivery-workflow.md) 不走 prep 实施分支。

```text
lane                         = prep（仅计划；实施直接在 unique-active）
capability_slug              = recipe/gt6-bulk-capacity
unique_active_wave           = null
landing_owned_paths          = tools/recipe_bulk/ordinary_wave.py（集成轴上限）；tools/recipe_bulk/transport.py；
                               CompactRecipeWireLimits.java；CompactRecipeShardRouter.java；
                               GTRecipeMapLoader.java；OrdinaryCloseoutIntegratedMeasurementHarness.java；
                               docs/current/recipe-wave-workflow.md §6
landing_depends_on           = 当前 unique-active 空窗；应在挤压机配方卡之前关
partial_close_allowed        = false
```

---

## 0. 开场判断

现在的上限是按“smelter + mixer 收口”那一轮定的：

| 门 | 现值 | 位置 |
| --- | --- | --- |
| 集成加载 eager / lazy / authored | ≤41,000 / ≤56,000 / ≤6,600 | `ordinary_wave.py` 1164–1190 |
| 单 shard 候选 | 128 | `CompactRecipeShardRouter.java` 43 |
| lazy 缓存 | 4,096 | `ordinary_wave.py` 1190 |
| 单 holder relations / 字典 / wire | 4,096 / 8,192 / 512 KiB | `CompactRecipeWireLimits.java` |

mixer 收口时实测：lazy 逻辑行 53,447，同步约 8.6 MB，查询 p95 约 73 µs，驻留约 17 MB
（`tools/waves/mixer/ordinary-closure/measurements.json`）。

后续三张配方卡预计新增：挤压机约 10.8 万行（翻译链修复后可能到 27 万），前缀规则类约 3.5 万，
化学杂项约 6 万。按现有上限，lazy 轴第一张就会超。

## 1. 要回答的问题

1. 30 万到 50 万逻辑行时，服务器 reload、dedicated `update_recipes` 同步体积、客户端内存、EMI 建索引各是多少？
2. 哪些图能走 on-demand（只在机器查询时展开），哪些必须 eager？
3. 同步体积超过可接受值时，是否要按图延迟同步或客户端按需请求？
4. 新上限按“每张图”还是“全局”记账？

## 2. 实施

1. 用测量 harness 做合成负载：按挤压机、切割机、mixer 的真实形状生成 10 万 / 30 万 / 50 万行夹具
   （只在测试源集，不进 live）。
2. 测 dedicated server + 客户端联机，不只测 integrated。
3. 定新上限并写进 `ordinary_wave.py`、Java 常量与工作流文档 §6；每张后续配方卡的计划写明本卡分给它的预算。
4. 如果测量表明现有架构扛不住，本卡产出的是架构改动方案，后续配方卡等它落地。

## 3. 验证

```powershell
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile capability-runtime
.\gradlew.bat runGameTestServer -PwaveRecipes=recipe/gt6-bulk-capacity
```

人工：dedicated server + 客户端联机进服，记录进服时间、内存与 EMI 打开时间。

## 4. 明确不接管

- 任何 GT6 配方导入
- 为了过门而删现有配方

## 5. 关闭清单

- [ ] 三档合成负载的 reload / 同步 / 内存 / EMI 测量数
- [ ] 新上限写进代码与工作流文档，负责人确认
- [ ] 后续三张配方卡的预算表
