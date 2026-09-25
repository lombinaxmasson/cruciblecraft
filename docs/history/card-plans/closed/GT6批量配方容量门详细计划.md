# GT6 批量配方容量门详细计划

> 计划 slug：`recipe/gt6-bulk-capacity`
> 状态：已关。本文件位于 `card-plans/closed/`。
> 正式名称：GT6 批量配方容量门
> 性质：在批量配方卡之前，测量并重定集成加载、同步与查询的容量上限。本卡不导入 GT6 配方。
> 总计划第 4 张落地卡，见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)。
> 改 load 硬顶：按 [能力交付流程 §8](../../../current/capability-delivery-workflow.md) 不走 prep 实施分支。

```text
lane                         = closed
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

后续三张配方卡的可翻译缺配方，按 2026-09-25 缺失流体关卡后的逐行分类：

| 卡 | `translatable_missing` |
| --- | ---: |
| 挤压机 | 302,175 |
| 前缀规则类 12 张图 | 45,363 |
| 化学杂项 | 68,363 |
| 合计 | 415,901 |

挤压机就绪率 93.7%。模具假缺口已收进可翻译行，不再按 27 万上界估。旧 lazy 上限 56,000 扛不住第一张；本卡把它改成 500,000。

## 1. 要回答的问题

1. 30 万到 50 万逻辑行时，服务器 reload、dedicated `update_recipes` 同步体积、客户端内存、EMI 建索引各是多少？
2. 哪些图能走 on-demand（只在机器查询时展开），哪些必须 eager？
3. 同步体积超过可接受值时，是否要按图延迟同步或客户端按需请求？
4. 新上限按“每张图”还是“全局”记账？

### 1.1 2026-09-25 合成测量

夹具在 `BulkCapacitySyntheticLoadHarness`，不进 live datapack。三档都是 `matrix_v1`，单 holder 4,096 行。数字见 `tools/waves/recipe/gt6-bulk-capacity/measurements.json`。

| 形状 | 50 万行 holder | 编码字节 | 编码时间 |
| --- | ---: | ---: | ---: |
| 挤压机 | 123 | 20,398,619 | 780 ms |
| 切割机 | 123 | 19,400,095 | 698 ms |
| mixer | 123 | 18,902,309 | 677 ms |

单 holder 展开 4,096 行，约 40–250 ms。50 万行编码约 20 MB，低于现有同步硬顶 64 MB。Dedicated 进服与 EMI 建索引仍是人工项，本表不含。

结论：这三张配方卡走 on-demand，不 eager 展开。全局 lazy 逻辑行上限改为 500,000。单 holder 4,096、单 shard 128、cache 总和 4,096、传输分片 256 都不改。每个新 family 声明的 cache 不超过 16。

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

- [x] 三档合成负载的编码体积与单 holder 展开（dedicated 进服与 EMI 仍待人工）
- [x] 新上限写进代码与工作流文档
- [x] 负责人确认新上限（2026-09-25：500,000 先用，全库缺配方以后再抬）
- [x] 后续三张配方卡的预算表
