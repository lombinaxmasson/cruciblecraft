# GT6 批量移植总计划

> 计划 slug：`portfolio/gt6-bulk-port`
> 状态：prep 已签发。本文件位于 `card-plans/prep/`。
> 性质：排期总表。本文件自己不开工、不建 `capability.json`、不改 `unique_active_wave`。
> 下列每张落地卡各有详细计划、各自占一次 unique-active、各自跑验证与玩家测试。
>
> 分母与现状：[gt6-full-coverage.md](../../../current/gt6-full-coverage.md)（2026-09-24 首次逐行比较）。
> Java/tick 源：`gt6_code/gregtech6`。贴图源：`gt6_referencable_port_code/gregtech6_w`。

```text
lane                         = prep
capability_slug              = portfolio/gt6-bulk-port
unique_active_wave           = null
prep_owned_paths             = 本文件；tools/waves/prep/material-form-demand-census/**；
                               tools/waves/portfolio/gt6-full-coverage-reassessment/**
landing_owned_paths          = 由各落地卡的详细计划分别声明
landing_depends_on           = 当前 unique-active 空窗；卡与卡严格串行
```

---

## 0. 开场判断

过去几轮按“一台机器一张卡、一张贴图一次”推进。逐行比较后，目标 706,980 源行里
已证明 17.2%，按机器平均 27.2%。剩余缺口按性质分三类，每类都可以成批处理：

| 类 | 规模（第一缺口口径） | 批量方式 |
| --- | ---: | --- |
| 缺配方，已可完整翻译 | 221,422 行 | `recipe_bulk` 按图族导入 |
| 缺身份 | 345,826 行 | 先修翻译链（一大块是假缺口），再按普查批量开门 |
| 缺机器：只有分母 17 kind | — | 同类合一张；转换器折进已有框架；异类合一张 |

改的是卡的切法，不是安全阀：

- 仍然一次一张 unique-active，串行。
- 每张卡照常跑 capability profile、`-PwaveRecipes=<slug>` GameTest、容量与 integrated load，
  并做一次人工 `runClient` 玩家测试（EMI 可见、机器实跑抽样配方、产物可用）。
- 禁止 stand-in、禁止无逐行等价证明的 parameterized、禁止 ItemEntity 世界生成顶获得。
- 批量产出仍是 `runtime_ready`，生存获得格另算。

## 1. 已确认的决策（2026-09-24）

1. 形态需求普查的需求源改成 **GT6 dump 实际用到的 `(材料, 前缀)`**（来自逐行分类），
   不再只读 `current_gap.json` / blocker 目录。旧来源基于过期文档，只有 68 对。
   仍然禁止按 `generation_flag` 全开长尾。
2. 一次一张 unique-active，每张都跑验证加玩家测试。
3. **暂不处理**：boxinator / unboxinator 两图，`crate.*` 与 `bulletGt*` 形态。
   不补映射、不开形态、不写 `exclusions.json`；它们留在目标里，等以后单独决定。
4. 锭块已有（双身份按 §2.1 处理）。
5. 粉块 `storage_dust` 要批量建立（当前 gate 只有 12 种材料，dump 需求 963 种）。

## 2. 已确认的决策（2026-09-24 第二轮）

1. **`blockIngot` 的 CC 身份保留 `block`**（486 种材料，`c:storage_blocks`），退役 `storage_ingot`
   （186 种，是前者子集）。翻译链把 `storage.ingot` 映射到 `block`；退役在粉块卡里做。
2. **批准 spec 数据化**：同类基础加工机卡先落 `machine_delivery.json` → `ProcessingMachineSpec` 生成器，
   落地时同步改 [recipe-wave-workflow.md](../../../current/recipe-wave-workflow.md) §1。
3. **移植** `fieryblood`、`fierytears`、`petrotheum`，作为 GT6 定义的流体注册。
4. **粉块外观**沿用 powder 图标集 + 材料染色。

## 3. 2026-09-24 调查修正

写详细计划时核实了首轮缺口，以下几项改变了排期：

- **挤压模具不缺**：`ExtruderShapeCatalog` 已有 GT6 meta 10000–10031 共 32 个。
  `gt6_resolve.extruder_shapes()` 按名字匹配、词序对不上，把 19 个判成缺，挡住 162,800 行。
  原“挤压模具卡”取消，并入翻译链修复。
- **一批 storage 与流体是漏映射**：`storage.ingot`、`storage.plate`、`sluicejuice`、`mercury`、`glass`
  在 CC 已有身份，只缺查找表；对照表还把 `storage.solid` 错映射成 `blockIngot`。
- **2x2 反应堆核心已 live**：覆盖页误记 `denominator_only`。
- **容量是硬约束**：集成加载 lazy 上限 56,000，mixer 收口时已 53,447。后面约 20 万行配方
  必须先过容量门。

## 4. 卡序

### 4.1 Prep（不占落地锁）

| 步 | 计划 | 内容 |
| --- | --- | --- |
| P1 | [GT6 翻译链映射修复](GT6翻译链映射修复详细计划.md) | 模具按 meta 匹配、storage / 流体别名、证据漏报、指纹盲区 |
| P2 | [材料形态需求普查](材料形态需求普查详细计划.md) “需求源改造” | 主需求源改为 dump 用到的缺形态 |

P1、P2 做完重跑逐行分类，后面各卡的分母以重跑结果为准。

### 4.2 落地卡（每张一次 unique-active，严格串行）

| 序 | 卡 | slug | 首轮规模 |
| --- | --- | --- | ---: |
| 1 | [GT6 粉块批量](GT6粉块批量详细计划.md) | `registry/gt6-storage-dust-blocks` | 约 951 种材料 / 15,727 行；退役 `storage_ingot` |
| 2 | [GT6 长尾形态开门](GT6长尾形态开门详细计划.md) | `registry/gt6-long-tail-forms` | 约 75,500 行（上界） |
| 3 | [GT6 缺失流体](GT6缺失流体详细计划.md) | `fluid/gt6-missing-fluids` | 7 种流体 |
| — | 重跑逐行分类 | — | 第二层缺口出现后调整 4–7 |
| 4 | [GT6 批量配方容量门](GT6批量配方容量门详细计划.md) | `recipe/gt6-bulk-capacity` | 不导配方；重定上限与各卡预算 |
| 5 | [GT6 挤压机配方批量](GT6挤压机配方批量详细计划.md) | `recipe/gt6-extruder-bulk` | 108,210 行起（模具修复后可能更多） |
| 6 | [GT6 前缀规则类配方批量](GT6前缀规则类配方批量详细计划.md) | `recipe/gt6-prefix-regular-bulk` | 约 35,100 行，12 张图 |
| 7 | [GT6 化学杂项配方批量](GT6化学杂项配方批量详细计划.md) | `recipe/gt6-chemical-misc-bulk` | 约 59,600 行 |
| 8 | [GT6 同类基础加工机批量](GT6同类基础加工机批量详细计划.md) | `machines/gt6-basic-machine-batch` | 5 kind / 19 台 + 3 张小图 |
| 9 | [GT6 激光、磁铁与 ZPM 转换器](GT6激光磁铁ZPM转换器详细计划.md) | `energy/gt6-laser-magnet-zpm-converters` | 4 kind / 16 台 |
| 10 | [GT6 余量能源设备](GT6余量能源设备详细计划.md) | `energy/gt6-remainder-devices` | 5 种 / 33 台 |

第 8 张不依赖身份卡与配方卡，可以插到任何空窗里提前做。第 8 张关后，steamcracking（7,746 行）
作为第 7 张的后续批次另开。

## 5. 每张卡的共同门

- 配方卡：`rule expansion ∪ exact remainder ∪ blocked = selected dump rows`，逐行比较 stable ID、
  IO、时长、EU/t、provenance。每行带 `evidence_hashes` 与 `selected_source_recipe`，
  否则进不了“已证明”。
- 容量：公布 relations、holder、shard、worst routed candidate；integrated load 与 dedicated
  `update_recipes` 实测，在容量门分配的预算内。
- 身份卡：只开普查 `openable` 或翻译链修复确认的真缺；新方块 / 物品有 GT6 原图和美术清单；
  改注册表后跑 `-PrecipeCensus`。
- 关卡后：`build_semantic_coverage.py --write` 与 `build_reconciliation.py --write`，
  产物随卡提交；人工 `runClient` 签收写进试玩 cycle。

## 6. 明确不接管

- boxinator / unboxinator、`crate.*`、`bulletGt*`、`storage.raw/gem/plateGem`（见 §1.3）
- `gt.recipe.anvil`、`gt.recipe.cruciblealloying` 的旧排除决策
- toolhead、bumblelyzer 等 `display_only` 行
- 来自其他 mod 的物品与流体（逐项决定后再进计划）
- 按 `ungated_generated_flag_pairs` 全开长尾
- 管 / 缆切片 C
