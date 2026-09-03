# 原版替换 MVP 详细计划

> 计划 slug：`content/vanilla-replace-mvp`
> 状态：已关闭（2026-09-02）。本文件位于 `card-plans/closed/`。
> `VANILLA_REPLACE_MVP_READY`：冻结 `vanilla_replace_lock.json`；
> 兑现 `minecraft:paper`（3 甘蔗 → 1 纸）；熔炉 / 骨头因缺铁前
> firestarter 与锤槽 serializer 按 §7 缩进 `deferred`；登记
> `cruciblecraft:crafting_firestarter`；R0 seal 未改写；
> `owns_families = 0`；`completion_delta = 0`；
> `generated_recipe_count = 0`；`production_lock = null`；
> `nuclear_started = false`；`unique_active_wave = null`；
> `next_unassigned = true`。
> 正式名称：原版替换 MVP
> 性质：零 GT family 的实现卡。兑现
> [原版替换 R0](原版替换R0详细计划.md) 点名的两类 loader 里
> **第一小时会碰到的工作台 / 熔炉政策**，不是把 92 KB 的
> `Loader_Recipes_Vanilla.java` 整文件搬进来。
> Opening：`FIRST_HOUR_PRESENTATION_READY`；`unique_active_wave = null`；
> `next_unassigned = true`。
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 来源：`Loader_Recipes_Vanilla` / `Loader_Recipes_Replace` /
> `Replacements` 三份 T13 blob（R0 `inherited_denominator.json`）。
> 目标状态：`VANILLA_REPLACE_MVP_READY`，或诚实的 `BLOCKED`
> 目标计数：`owns_families = 0`、`completion_delta = 0`、
> `generated_recipe_count = 0`（GT compact 不扩）、`production_lock = null`
> 前置：首小时表现与阶段账本；`VANILLA_REPLACE_R0_READY`
> 明确不包含：物流网、作物、核电、T30 hopper 121、模具烧制、
> Vanilla.java 后半的 `RM.smash` / Lathe / boxunbox、全量 Replace 扫描器、
> ASM 僵尸 / 岩浆行为、玩家发行
> 签发层级：当前工作树；本次计划关闭不创建 Git commit

权威边界仍来自
[总体规划](../../../current/roadmap.md)、
[冻结与未实现账本](../../../current/unimplemented-gap.md)、
[Ordinary Recipe Wave 流程与规范](../../../current/recipe-wave-workflow.md)
以及已 sealed 的
[`replace_contract.json`](../../../../tools/waves/portfolio/vanilla-replace-r0/replace_contract.json)。
Markdown 不是 production authority。

---

## 0. 决策

首小时表现卡已关。账本下一张是原版替换 MVP。R0 只冻了两类
`requires_new_runtime`，游戏里 `data/minecraft/recipe` 覆盖数仍是 0。

三份源文件不是同一件事：

| 文件 | 实际在干什么 | 本卡 |
| --- | --- | --- |
| `Loader_Recipes_Vanilla.java`（约 92 KB） | 前半：删 / 改原版工作台与熔炉；后半：往 GT RecipeMap 塞 smash / lathe / pack / generify / boxunbox | 只做前半里、第一小时会碰到的条目 |
| `Loader_Recipes_Replace.java` | 运行时扫全部 shaped 配方，把锭形工具换成板 / 曲板 | 本卡不建全量扫描器 |
| `Replacements.java`（ASM） | 僵尸转化村民、岩浆 tick，**不是配方** | `out_of_scope` |

空 dump（`mc.recipe.furnace` recipeCount = 0）不是分母。分母是实施时从
源文件冻结的 allowlist。

来源跟首小时贴图同一条纪律：GT6 源可以 `SOURCE_BACKED` 用。不要手编一套
「看起来像 GT6」的替换表。不要搬 ASM transformer。NeoForge / datapack
等价物随便选，以能删、能加、reload 稳定为准。

```text
content/vanilla-replace-mvp
```

---

## 1. 完成后应该成立的工作流

```text
read three T13-pinned source files
  -> split Vanilla.java:
       crafting/furnace policy   = this card
       RM.smash / lathe / …      = deferred, not this lock
  -> freeze vanilla_replace_lock.json
       removed[] / added[] / substituted[]
       each row: minecraft or cruciblecraft recipe id
                 + GT6 source pointer
                 + add | remove | substitute
  -> implement on NeoForge 1.21.1
       datapack override and/or recipe events
  -> player path still reaches first machine
  -> recipes profile + reload
  -> VANILLA_REPLACE_MVP_READY
       or BLOCKED
```

关键分界：

1. Vanilla 与 Replace 两条轴分开记账。本卡主要做 Vanilla 的
   crafting / furnace 政策。Replace 扫描器不在本卡。
2. 不在 lock 上的原版配方不得删除。未知 id fail closed，不是 silent no-op。
3. 允许写 `data/minecraft/recipe/**`。R0 把它列成开放问题，本卡答：可以。
4. 通用导入器仍然不删原版配方。本卡自带删除 / 替换机制。
5. T30 hopper 121、五条模具 `minecraft:smelting` 已有主人，不重开。
6. 关卡后账本第 1 节的 Vanilla / Replace 改成「部分由本卡实现」，
   未进 lock 的余量继续 `frozen`。不得写成两类 loader 做完。
7. `next_unassigned = true`。不预分配物流网 / 核电 child。

---

## 2. Card contract

### 2.1 一张卡

```text
FIRST_HOUR_PRESENTATION_READY
  -> 本计划签发
  -> 实施时 unique_active_wave = content/vanilla-replace-mvp
  -> VANILLA_REPLACE_MVP_READY
       或 BLOCKED
  -> unique_active_wave = null
  -> next_unassigned = true
```

全程固定：

```text
owns_families             = 0
completion_delta          = 0
partial_family_count      = 0
production_lock           = null
generated_recipe_count    = 0
nuclear_started           = false
leftover_later_count      = 39
vanilla_replace_r0 seal   = unchanged
```

不适用 300-family 下限。不得借这个例外往 `src/recipe_generated/**` 灌
GT compact。

### 2.2 R0 契约题，本卡给出的答

| 题 | 答 |
| --- | --- |
| `add_vs_remove` | lock 分三列：remove / add / substitute。不加进同一行充数 |
| `asm_vs_datapack` | 不搬 ASM。用 datapack 覆盖或 NeoForge 配方事件。`Replacements.java` 本卡不要 |
| `minecraft_namespace` | 允许 `data/minecraft/recipe` |
| `player_acquisition` | 生存仍能走到第一台可运行机器。删熔炉配方就必须有替代 |
| `fail_closed` | lock 外的原版配方不删 |
| `load` | 增加、删除、替换分开数 |
| `disposition_policy` | 见 §3 |

### 2.3 实施时的交付

```text
tools/waves/content/vanilla-replace-mvp/vanilla_replace_lock.json
（或等价路径；实施时钉下来，关闭后不要手改计数）

src/ 里真实的删除 / 覆盖 / 新增
docs/current/unimplemented-gap.md   # Vanilla / Replace 改成部分实现
docs/current/roadmap.md
```

可选：本波 `wave.json` / `readiness.json` / `closeout_seal.json`。
没有也可以关，只要 lock 与游戏内加减一致。

---

## 3. 权威分母

实施时对着 GT6 revision `3703e40308c8c030763fd6297dea8b210d2a77b1`
重读源文件，冻结 lock。下面是选课规则，不是假 census。

**进 lock（本卡 `implemented`）**

从 `Loader_Recipes_Vanilla` 前半的 `CR.remove` / `CR.delate` /
`RM.rem_smelting` / 对应 `CR.shaped` 里挑：**玩家指南节点 1–2 会碰到、
或会绕开研钵 / 筛分 / 熔炼 / 坩埚 / 火箱** 的原版工作台与熔炉配方。

源文件里已经写明的第一小时邻居包括（实施时按 1.21.1 配方 id 对上再锁）：

- 原版熔炉合成（`CR.delate(Blocks.furnace)` + 带 firestarter 的新配方）
- 甘蔗纸、骨头、玻璃瓶等会改开局材料流的条目
- 与第一小时双轨冲突的原版熔炼，**仅限 Vanilla.java 里真正 `rem_smelting` 的那些**

条目进 lock 必须有 GT6 行号或调用指针。没有指针的不要猜。

**本卡 `deferred`**

- `Loader_Recipes_Vanilla` 后半：`RM.smash` / `RM.Lathe` / `RM.Extruder` /
  `RM.pack` / `RM.generify` / `RM.boxunbox`（这是 GT 机器配方，不是原版替换）
- 附魔台、末影箱、金苹果、鞍等非第一小时条目
- `Loader_Recipes_Replace` 全量扫描器（锭 → 板的工具族）

**本卡 `out_of_scope`**

- `Replacements.java`：僵尸村民、岩浆 tick
- 未点名的其他 ASM（矿车速度等）
- T30 hopper 121、五条模具烧制
- `WorldgenOresVanilla` 24

---

## 4. 实现约束

- 来源是那三份 Java，不是空 furnace dump，也不是现编一张「合理替换表」。
- 1.21.1 没有的 1.7 物品（染料 meta、旧石板）在 lock 里标 `no_1_21_equivalent`，
  不删一个凑数的邻近 id。
- 其他 datapack / mod 已经改过同一 `minecraft:` id 时，fail closed 或在
  lock 写冲突，不许 silent 覆盖。
- reload 后加减集合与 lock 一致。
- 生存仍能：地表石子 / 矿脉 → 研钵或粉碎 → 筛分 → 坩埚或熔炼炉 →
  火箱。本卡不得把这条砍断还假装 READY。

---

## 5. 验收

```text
vanilla_replace_lock 冻结且每条有源指针
lock 内 remove 在 reload 后不存在
lock 内 add / substitute 存在且输入输出与源一致
lock 外原版配方仍在
玩家路径节点 1–2 仍通
python tools/verify.py integration --profile recipes
python tools/verify.py dev --path docs
```

GameTest 至少覆盖 lock 里的代表删除与代表新增，加上一次 reload。
不要求普通 recipe wave 的 300-family 收据。

关卡计数：

```text
VANILLA_REPLACE_MVP_READY   # 或 BLOCKED

owns_families          = 0
generated_recipe_count = 0
production_lock        = null
nuclear_started        = false
R0 seal                = unchanged
账本 Vanilla / Replace = 部分实现，余量仍 frozen
```

---

## 6. 明确不包含

- 把整个 `Loader_Recipes_Vanilla.java` 当本卡分母；
- 把 Vanilla.java 后半的 GT RecipeMap 行当原版替换；
- 实现 Replace 全量扫描器或搬 ASM；
- T30 hopper、模具烧制、作物、物流网、核电、TU/LU；
- 为了「完成两类 loader」把余量写进 lock；
- 本签发阶段创建 `tools/waves/**`、Java 实现、或 Git commit。

---

## 7. 撤回

实施中发现阻塞时只有三种合法结果：

1. 在本卡内缩小 lock 并写清为何某条 `no_1_21_equivalent`；
2. 保持 `BLOCKED`，写清缺的是源对照、NeoForge 删除面还是玩家路径；
3. 正式撤回并重签，保留旧 lock hash。

不得用模具烧制或 hopper 121 冒充本卡。不得把 lock 外的熔炉配方删掉
再声称 fail closed。不得把本卡写成 Vanilla / Replace 两类已
`player_complete`。

关闭时：本计划移到 `card-plans/closed/`，补 stage archive 与 work log；
更新 history index、roadmap、workflow、verification 与账本第 1 节；
不重写 sealed R0 artifact；不自动创建 Git commit。
