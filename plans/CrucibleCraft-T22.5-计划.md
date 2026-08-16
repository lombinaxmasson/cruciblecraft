# CrucibleCraft T22.5 计划 · 分母口径与阻塞归属（修订版）

> 阶段性质：**前置调查 + 名称映射**，不批量发布配方
> 位置：T22 关闭之后、T23 之前
> 前置：T21 已关闭；T22 内容侧已定（分母 1,084 全分类、v1 = 2 行、已发布）
>
> **本版推翻了初稿 §0.4。** 初稿说"114 个材料缺流体形态、需要批量注册"——错的。
> 实际缺口是 **13 个材料**，多为跨模组；真正的工作是**名称映射**，不是注册。

---

# §0 · 三轮验算的结果

## 0.0 ⚠️ 先说清：有两个账本，名字撞了

这是本阶段最容易把人绕晕的地方，**开工前必须先分清**。

**账本一 · `t21_source_denominator.json`**（45,353 行，input-touch 诊断账）

```
disposition                    translation
ordinary_v1_required  45,044   blocked_translation  42,658
post_1_0_nuclear         304   translatable          2,695
petroleum_t22              5
```

交叉：`ordinary_v1_required` 中 **42,452 行是 `blocked_translation`（94%）**。

**账本二 · `t21_template_denominator.json`**（93,133 units，模板分母）

```
v1_required          1 unit
ordinary_optional   89,643 units / 146,841 rows
```

**T21 重基线时已把账本一降级为诊断账**，改用账本二作 closure numerator。
但账本一里 `ordinary_v1_required` 这个**字段名没改**，导致两个文件并列时
一个说"45,044 行 v1 必需"、另一个说"v1 只有 1 unit"。

**这是术语残留，不是数据矛盾。权威分母是账本二。** A0 卡负责消除它。

## 0.1 判别式确认：`source_id == -1`

`tools/gt6_material_activation_policy.json`：

```
ACTIVE       : 1,773
OUT_OF_SCOPE :   441   理由全部一致：
                       "external/placeholder material has no stable GT6 source id"
```

441 条的 `source_id` **全部是 -1**；1,773 条 ACTIVE 中 `source_id == -1` 的**一条没有**。
双射、无例外，可直接复用——**这是你当年移植物品时定的规则，现在仍然有效。**

## 0.2 配方行没有 `source_id`，需经 operand 连接

`t21_source_denominator.json` 的行记录：

```
[map_index, recipe_index, candidate_material_indexes, disposition,
 shape, translation, rejection, source_row_sha256, projected_row_sha256]
```

`candidate_material_indexes` 指向 224 条化学候选字典，**被阻塞的流体/物品身份不在行记录里**。
两条连接路径已存在：

- **物品** → `gt6_l1b_selected_recipe_operands.json`（368,751 条，已解析为 material-prefix 对）
- **流体** → `gt6_oredict_fluids_normalized.json`（322 条，`fluid → material_id`）→ 材料层 status

## 0.3 ❌ 流体层没有筛选空间

322 条按材料 status：**ACTIVE 320 / OUT_OF_SCOPE 2**
（仅 `molten.euphemium`、`molten.schrabidium`）。**筛选收益 2/322。**

几条实测值得注意：

| GT6 流体名 | GT6 material | source_id | status | CC 材料 |
|---|---|---:|---|---|
| `ic2constructionfoam` | ConstructionFoam | 9883 | **ACTIVE** | `construction_foam` |
| `ic2distilledwater` | WaterDistilled | 9808 | **ACTIVE** | `water_distilled` |
| `molten.brass` | Brass | 8620 | ACTIVE | `brass` |
| `molten.asphalt` | Asphalt | 8266 | ACTIVE | `asphalt` |

> ⚠️ **名字带 `ic2` 前缀的两个，按项目自己的判别式是 ACTIVE，CC 侧材料早就有。**
> GT6 只是把自己的材料注册在 IC2 的流体名下做兼容。
>
> 若不发布，理由只能是 **"CC 无消费端"**，不能写"IC2 命名空间依赖"。
> **T22 的 `cross_mod_compat_note` 目前写了后者，需要改**——理由写错会让
> recheck condition 变成"若 CC 引入 IC2 兼容"，永远不会触发。

## 0.4 ✅ 流体覆盖率实测：只差 13 个材料

CC 有**三条**流体注册路径，初稿只查了第一条：

| 路径 | 机制 | 覆盖 |
|---|---|---:|
| 熔融金属 | 材料 JSON 的 `molten_fluid`，判据是 `import_gt6_oredict.py:1613` 的 `fluid.amount == 144` | **204** |
| **化学 / 常温流体** | `ChemicalFluidRegistrationGate` 读三个 gate 文件 | **110** |
| 静态 | `ModFluids` 手写 | creosote / steam |

**`molten_fluid` 只标"是不是熔融金属"，不是"有没有流体形态"。**
`air`、`ammonia`、`argon`、`ethanol`、`biomass`、`aqua_regia`、`construction_foam`、
`water_distilled` 这些初稿说"缺失"的，**全都已经在化学 gate 里注册了**。

320 个 ACTIVE 流体的真实覆盖：

```
molten 形态    : 204
化学 gate      : 102
静态注册       :   2
真正缺失       :  13
```

缺的 13 个材料（16 个流体名）：

```
aerotheum  cryotheum  petrotheum  pyrotheum        ← Thermal Expansion
honey  honeydew  milk（for./grc./grcmilk. 前缀）    ← Forestry / Growthcraft
blaze  ectoplasm  ice  plastic
```

**大部分是跨模组。流体注册这件事基本已经做完了。**

## 0.5 🔑 30,437 行 `fluid_mapping` 阻塞的真正原因

流体都注册了，阻塞就不可能是"没有流体"。**只剩一个解释：GT6 流体名到 CC 流体 id
的映射表不存在。**

T22 的 brass/asphalt 是活证据：`brass` 有 `molten_fluid: true`、CC 流体存在，
但翻译层解析不了 `molten.brass` 这个名字。

```
GT6                    CC
molten.brass        →  cruciblecraft:molten_brass
ic2constructionfoam →  cruciblecraft:construction_foam
ic2distilledwater   →  cruciblecraft:water_distilled
potion.mineralwater →  ？（不在 322 列表，需单独定身份）
```

**这是查表工作，不是注册工作，成本低一个量级。**
322 条给出 `fluid → material_id`，材料层给出 `material_id → cc_id`，
**映射表可以直接派生。**

## 0.6 ✅ 筛选的主战场在物品层

`gt6_l1b_selected_recipe_operands.json` 的 `unmapped_gt_meta_items`：

| GT6 meta item 族 | 引用次数 | 性质 |
|---|---:|---|
| `gt.meta.storage.*` | 58,397 | 存储方块变体 |
| `gt.meta.machine*` | 23,492 | 机器物品 |
| `gt.meta.crate.*` | 20,179 | 64 倍箱装 |
| `gt.meta.ore.broken.*` | 8,639 | 碎矿 × 22 种宿主岩 |
| `gt.meta.ore.normal.*` | 4,774 | 原矿 × 宿主岩变体 |
| **合计** | **115,481** | |

**这些才是能按定义大量归类的**：`ore.broken` 有 CC 等价（`crushed_ore`）；
22 种宿主岩你在 T9 就定了只做 stone/deepslate；箱装与机器物品变体明确不做。

---

# §1 · 修订后的目标

1. **映射**：建立 GT6 流体名 → CC 流体 id 的可派生映射表
2. **归类**：物品层 115,481 条引用，按定义分入等价映射 / 明确不做
3. **定口径**：重算真实分母，给出 v1 / post-1.0 划分，收紧 Beta 措辞

**注册流体和扩容都不在本阶段。**

---

# §2 · 执行卡

```text
A0  术语统一（两个账本）           ← 无前置，半小时，先做
A1  shape 分析（九张 map）        ← 需 gt6_dump，先确认环境
A2  流体名称映射表                ← 无前置  ⭐ 最高性价比
A3  物品层归类规则                ← 无前置
      │
B1  配方行分类                    ← 需 A2+A3
B2  剩余 13 个流体缺口处置         ← 需 A2
      │
C0  逐机器可玩性覆盖审计           ← 需 B1  ⭐ 回答"基本完整性"
C1  真实分母重算 + v1/post-1.0     ← 需 A1+B1+C0
C2  Beta 措辞收紧
      │
D1  readiness + 沿链重建 + 单次 record
```

**A0/A2/A3 不需要 raw dump，可以先做。**

---

## A0 · 术语统一（半小时，先做）

**为什么**：见 §0.0。两个账本用了几乎一样的词，结论相反。
**不做的话，每个翻到账本一的人都会重新懵一次。**

**动作**
1. `t21_source_denominator.json` 里的 `ordinary_v1_required` 显式标注为 superseded——
   改名（如 `legacy_input_touch_v1_candidate`）或加 `superseded_by` 字段
2. 两个账本互相引用，写明：
   - 账本一 = input-touch 诊断账，**不是 closure numerator**（T21 已降级）
   - 账本二 = 权威模板分母
3. 本阶段所有新产物只引用账本二的词表

**验收**
- [ ] 账本一的 disposition 字段带 superseded 标记或已改名
- [ ] 两个文件互相有指向对方的 `supersedes` / `superseded_by`
- [ ] 全仓搜 `ordinary_v1_required`，确认没有产物拿它当 v1 判据
- [ ] **不改任何数字**——只改标签与说明（历史值只读，规矩二）

---

## A1 · shape 分析（只出结论，不发布）

**为什么**：bath 59,722 行、smelter 21,614 行从未做过模板提取，目前 1.00 rows/unit。
不知道结构就无法判断可归类比例。

**⚠️ 环境门禁**：需要 `gt6_dump/gt6_recipe_dump/maps/*.json`。
**T22 已证明这是硬门槛**——五个 family 因缺 dump 做不了。
排期前先确认环境，没有 dump 就不要开这张卡。

**动作**：照 `gt6_mixer_templates.py` / `gt6_extruder_templates.py` 的形状，
对 bath / smelter / assembler / compressor / centrifuge / autoclave /
electrolyzer / drying / roaster 九张 map 跑模板提取。
**只产出压缩比与 family 结构结论**，不进 publication。

**验收**
- [ ] 九张 map 各有 template count、rows/unit、最大 family 规模
- [ ] membership 双射（`unassigned = 0`、`duplicate = 0`）
- [ ] compact receipt 进 ordinary CI，全量 replay 归 `--source-replay`
- [ ] **publication delta = 0**

**口径纪律**：`unit` 与 `row` 是两个单位，任何数字带单位名与来源产物名。

---

## A2 · GT6 流体名 → CC 流体 id 映射表 ⭐

**目标**：让翻译层能解析 GT6 流体名，解锁 30,437 行里的大部分。

**动作**
1. 从三份现有产物**派生**（不手写）：
   - `gt6_oredict_fluids_normalized.json`：`fluid_name → material_id`
   - `gt6_material_activation_policy.json`：`material_id → cc_id / status`
   - 三个 fluid gate + 材料 `molten_fluid` + `ModFluids` 静态：`cc_id → 运行时 fluid id`
2. 逐条落 disposition：

| 分类 | 条件 | 处置 |
|---|---|---|
| `mapped` | 材料 ACTIVE 且 CC 流体存在 | 进映射表 |
| `no_cc_fluid` | 材料 ACTIVE 但三条路径都没有 | 交 B2（预计 13） |
| `out_of_scope` | 材料 `source_id == -1` | 排除（预计 2） |
| `unknown_identity` | 不在 322 列表 | 单独定身份（`spectral_dew`、`potion.mineralwater`） |

**验收**
- [ ] 322 条全分类，`unclassified = 0`
- [ ] 映射表**可派生、可复算**，不含手写条目
- [ ] 变异测试：改任一材料 status，受影响映射跟着变
- [ ] 四条已知样本可作 fixture：`molten.brass` / `molten.asphalt` /
      `ic2constructionfoam` / `ic2distilledwater`
- [ ] **不注册任何流体，publication delta = 0**

---

## A3 · 物品层归类规则

先写规则，再跑分类（§5.4 判据先于实现）：

| GT6 族 | 建议归类 | 依据 |
|---|---|---|
| `gt.meta.ore.broken.*` | 映射到 CC `crushed_ore` | CC 已有等价；22 种宿主岩是 GT6 世界生成设定，CC 用 stone/deepslate（T9 已定） |
| `gt.meta.ore.normal.*` | 同上 | 同上 |
| `gt.meta.crate.*` | `out_of_scope: packaging_form` | 64 倍箱装属 GT6 物流打包层，CC 未实现且 G10 外围 |
| `gt.meta.machine*` | `out_of_scope: machine_item_variant` | 机器物品属 T12/T16 身份体系，不走配方 operand |
| `gt.meta.storage.*` | 逐 prefix 判断 | dust/ingot/plate 的存储方块 CC 有 `block` 前缀，需核对覆盖率 |

**验收**
- [ ] 规则先落盘再跑分类
- [ ] 115,481 条全部归入唯一类别，`unclassified = 0`
- [ ] 每类有定义、判别式、recheck point
- [ ] 归类理由是**类别定义**，不是"翻译不了"
- [ ] 变异测试可打红

---

## B1 · 配方行分类

**分类词表沿用 T22 实测跑通的那套**（`unclassified = 0`），不重新发明：

```
v1_required            ordinary_optional        already_covered
cross_mod_compat       ore_processing_byproduct generic_processing
post_1_0_g10           post_1_0_nuclear         out_of_scope
```

**动作**
1. 物品 operand 走 A3 归类；流体 operand 走 A2 映射表
2. 全量分类，结果落**新 artifact**
3. **不改写 `t21_template_denominator.json`**（T21 关闭快照只读）；
   新表显式声明 supersede 关系

**⚠️ 两条 T22 踩出来的门禁**

**a. 分母重分类后必须同步全部下游。**
T22 把 denominator 改对了，`t22_family_manifest.json` 没跟着改，
两个产物讲了两个故事，pending 还在要求投影已判为非 v1 的行。
**T22.5 是全量重分类，这个坑会放大十倍。**

**b. 禁止用状态名消解计数。**
T22 的 `build_t22_readiness.py` 有这样一行：

```python
v1_missing = sum(0 if f["status"] == "MATERIAL_RULE_COVERED"
                 else f["verification"]["missing"] for f in v1_families)
```

9 条 missing 被状态名一笔勾销。**任何"这条不算 missing"的状态都必须带可复算证据。**
同理 `actual` 不能由 `expected − missing` 减出来（同源，比对恒真）。

**验收**
- [ ] 全量分类 `unclassified = 0`，可复算
- [ ] 变异测试：改任一材料 status，受影响行分类跟着变
- [ ] T21 产物未被修改
- [ ] 消费该分类的下游产物已同步（列出清单）

---

## B2 · 剩余 13 个流体缺口处置

已知清单：

```
aerotheum  cryotheum  petrotheum  pyrotheum          Thermal Expansion
honey  honeydew  milk（for./grc./grcmilk. 前缀）      Forestry / Growthcraft
blaze  ectoplasm  ice  plastic
```

**动作**：逐条判断。**大概率全部是"无 CC 消费端"或跨模组**，登记不注册。

若确有 v1 消费端，走**现成的 `ChemicalFluidRegistrationGate`**——
往 gate 文件加条目，**不改 `materials/*.json`**。schema 照现有先例：

```json
{"id": "...", "material": "...", "state": "liquid|gas",
 "color": "#RRGGBB", "density": 1000, "temperature_kelvin": 300,
 "viscosity": 1000, "world_placeable": false,
 "source": {"path": "gt6_dump/.../<map>.json#recipes[N]",
            "reason": "<必须指向真实消费或产出路线>",
            "repository": "GregTech6/gregtech6",
            "revision": "3703e403..."}}
```

> **走 gate 而不是材料 JSON 有额外好处**：gate 是独立文件，不触碰材料目录，
> 因此不会让 12 个引用材料树 hash 的 builder 全部失效
> （`build_gt6_material_form_gate` #5、`build_t5_chemical_readiness` #7、
> `build_t6_electrical_readiness` #11、`build_t10_preflight_projection` #17 等）。

**验收**
- [ ] 13 个逐条有 disposition
- [ ] 若注册，每条 `source.reason` 指向真实消费或产出路线
- [ ] 新增流体满足 §5.8（en_us、模型、`zh_cn` 不倒退）
- [ ] 未修改 `materials/*.json`

---

## C0 · 逐机器可玩性覆盖审计 ⭐ 回答"基本完整性"

**为什么**：行数分母回答不了"这台机器能不能玩"。
一台已注册的机器如果配方为零，那是**坏内容**，不是"推迟的内容"——它会挡 Beta。
但这跟"少了几万行长尾"是两回事，必须分开衡量。

**三种状态要分清**

| 状态 | 含义 | 对 Beta |
|---|---|---|
| 机器未实现 | CC 没这台机器 | 无影响，属范围外 |
| **机器已注册但配方为零** | 玩家造得出、用不了 | **阻断** |
| 机器已注册、有可玩角色、缺长尾 | 能用，内容不全 | 不阻断，属 portfolio |

**当前实测**（32 个 RecipeMap 的已发布 authored 配方数）：

```
crusher 495   centrifuge 371   sifter 357   shredder 357   sluice 357
smelter 203   electrolyzer 62  mixer 38     assembler 29   extruder 28
autoclave 17  bath 7           wiremill 7   anvil 6        compressor 5
drying 5      press 5          mortar 3     distillery 3   bender 2
cutter 2      lathe 2          fuels_engine 2  generifier 2
coke_oven 1   rollingmill 1    rollbender 1  welder 1  fuels_gas 1  cooling 1
```

> **bath 不是空的。** 它有 `bath/crushed_to_washed` 这条 MaterialRule
> （洗矿，按材料展开覆盖全部 ore material）加 6 条 T5 钨链化学配方。
> **洗矿正是 bath 在 GT6 早期的主要角色**——它有可玩内容，缺的是长尾化学。
>
> 同理要注意：**authored 数 ≠ logical 数**。MaterialRule 一条能展开上百行，
> 所以这张表只能用来找"零"，不能用来比"多少"。

**动作**
1. 对 32 个已注册 RecipeMap 逐个判定落在上面三种状态的哪一种
2. 判定依据是 **logical 展开后的可达配方数**，不是 authored 文件数；
   `0` 必须是数出来的（规矩四推论 C）
3. 每台机器写一句"它在 v1 里的角色"——比如 bath = 洗矿 + 钨链，
   cutter = 润滑油消费端
4. 任何 logical = 0 的已注册机器，**升级为 v1 blocker**，登记 owner

**验收**
- [ ] 32 个 map 全部落到三态之一，`unclassified = 0`
- [ ] logical 计数由运行时或展开器给出，不是数文件
- [ ] 零配方机器（若有）已登记为 blocker 并有 owner
- [ ] 每台机器的 v1 角色一句话写清

**这张卡才是"基本完整性"的判据。** C1 的行数分母回答的是另一个问题
（GT6 搬了多少），两者不要混用。

---

## C1 · 真实分母重算与范围划分

**动作**
- 用 A1/A3/B1 结果重算 `ordinary_optional` 146,841 行的真实构成
- 四列：`v1` / `post_1_0_portfolio` / `out_of_scope` / `可解锁（映射到位即可）`
- **只有确认某批必须进 v1，才启动容量实测**（T14 四类协议），
  按实测调整 ceiling——**不能只改数字**

**预期结论**（待验证）：留在 v1 的很少，扩容不必要。

**验收**
- [ ] 四列相加等于总数
- [ ] 若调 ceiling，四类实测齐全并标 `MEASURED`
- [ ] 若不调，写明"v1 之前不会触及 21,000"的依据

---

## C2 · Beta 措辞收紧

现写：

> T21 的普通化学与 T22 的石油化工在明确排除 post-1.0 subsystem 后**完整发布**

建议改为：

> T21 的普通化学与 T22 的石油化工中，**`v1_required` 全部发布**；
> `ordinary_optional` 属于 1.0 后 portfolio，逐类登记 owner 与 replacement condition。

**验收**
- [ ] 措辞已改，T26 Beta 门禁判据无歧义

---

## D1 · 收尾

- [ ] `build_t22_5_readiness.py` **与验证器门禁键同时建好**
      （T21/T22 教训：结构后建会 KeyError 连撞三轮）
- [ ] 验证器加前置键存在性检查，缺键产生一句人话而非 traceback
- [ ] `status` 由门禁**派生**，`check()` 重算比对；
      **不要交给 `run_full_verification`**（T21 循环依赖就是这么来的）
- [ ] `closure_policy.pending` 只含实质工作项，**无流程步骤**
- [ ] `expected` 与 `actual` 来自两条独立路径
- [ ] `rebuild_artifacts.py --keep-going` + `git diff`（历史 baseline 未被动）
- [ ] 单次 clean `run_full_verification.py --record --new-session`
- [ ] **自检**：报告里 `required_tests` 应大于 88；仍是 88 说明没真跑

---

# §3 · 风险

| 风险 | 说明 |
|---|---|
| **筛选收益低于预期** | 流体层 2/322，量在物品层。若物品层归类后剩余仍远超 21,000，说明"搬完"这个目标本身需要重新定义——正是 C1 要回答的 |
| **A1 需要 raw dump** | T22 已证明是硬门槛，排期前先确认 |
| **归类被当成绕过困难的手段** | T22 犯过一次。判别必须是**类别定义**，不是"翻译不了" |
| **下游同步遗漏** | T22 的 denominator/manifest 分裂，全量重分类会放大 |

---

# §4 · 数字速查（本轮实测）

| 项 | 值 | 来源 |
|---|---|---|
| 材料 ACTIVE / OUT_OF_SCOPE | 1,773 / 441 | `gt6_material_activation_policy.json` |
| 排除判别式 | `source_id == -1`（双射） | 同上 |
| normalized fluid | 322（ACTIVE 320 / OOS 2） | `gt6_oredict_fluids_normalized.json` |
| **CC 流体覆盖** | molten 204 + 化学 gate 102 + 静态 2 = **308** | 材料 JSON + 三 gate + `ModFluids` |
| **真正缺失** | **13 个材料 / 16 个流体名**，多为跨模组 | 三方 join |
| 化学 gate 已注册 | 110 条 | `t5/t10/t11_*_fluid_gate.json` |
| `molten_fluid` 判据 | `fluid.amount == 144`（熔融金属专用） | `import_gt6_oredict.py:1613` |
| unmapped meta item | 115,481 | `gt6_l1b_selected_recipe_operands.json` |
| `ordinary_optional` | 89,643 units / 146,841 rows | `t21_template_denominator.json` |
| fluid_mapping 阻塞 | 30,437 行（71.4%） | `t21_source_denominator.json` |
| T22 收盘 publication | 18,882 / 16,657 / 2,225 | `t22_publication_baseline.json` |
| logical 硬上限 / 余量 | 21,000 / **2,118** | T14 |
| 基线 | GameTest 88 / Java 539 / Python 584 | `full_verification_report.json` |
