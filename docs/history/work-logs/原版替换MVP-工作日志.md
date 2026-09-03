# 原版替换 MVP 工作日志

> 计划 slug：`content/vanilla-replace-mvp`
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`

## 2026-09-02 · 计划签发

卡已签发，位于
[原版替换 MVP 详细计划](../card-plans/closed/原版替换MVP详细计划.md)。

决策：兑现原版替换 R0 的第一小时工作台 / 熔炉政策，不搬 ASM，不建
Replace 全量扫描器。分母是 Vanilla.java 前半带行号的 allowlist，不是
空 furnace dump。当时机器可读 `unique_active_wave` 仍为 `null`。不签
production lock，核能 Track C 仍 `started = false`。不创建 Git commit。

## 2026-09-02 · 关闭

对着 pinned revision `3703e40308c8c030763fd6297dea8b210d2a77b1`、
Vanilla.java blob `4c459acd2c7729d4186c5ada9ccd76181745bacf` 冻结
[`vanilla_replace_lock.json`](../../../tools/waves/content/vanilla-replace-mvp/vanilla_replace_lock.json)。

**implemented：** `minecraft:paper` 覆盖为 shaped `XXX` 甘蔗 → **1** 纸
（源 L43–44 `CR.remove(reeds)` + L52 `CR.shaped(paper, "XXX", reeds)`）。
原版 1.21.1 是 3→3。

**§7 缩小熔炉：** `CR.delate(Blocks.furnace)` 需要铁前
`OD.craftingFirestarter`。`LoaderItemData` L517–518 只登记
`fire_charge` / `flint_and_steel`。`MultiItemRandomTools` Fire Starter
要 `OD.itemGrassDry` / `OD.itemBarkDry`（无 1.21 物；禁止手编干草 id
或搬割草世界生成）。Match 工作台是磷粉 + 木螺栓，不是第一小时。
铁锁熔炉会砍火箱 / 研钵 / 陶瓷烧制。熔炉整行 `deferred`，原版 8 圆石
配方保留。火箱仍用方块 `minecraft:furnace`。

**骨头 deferred：** L136 锤槽（耐久不消耗）无 vanilla serializer。
不得先删 `minecraft:bone_meal`。玻璃瓶与 1.21 V 图案相同，
`identical_1_21`。`RM.rem_smelting` L40–41 在 1.21 无对应物，不删邻近
smelting id。

登记 `cruciblecraft:crafting_firestarter`（flint_and_steel /
fire_charge / match）作为支撑标签，不当作本卡熔炉解锁。

R0 checker 不再把活树 `data/minecraft/recipe` 当 R0 失败；sealed
`minecraft_recipe_override_count` 保持 0。`--write` 未跑。

关闭结果：`VANILLA_REPLACE_MVP_READY`。计划移到 `card-plans/closed/`。

```text
owns_families          = 0
completion_delta       = 0
generated_recipe_count = 0
production_lock        = null
unique_active_wave     = null
next_unassigned        = true
nuclear_started        = false
substituted            = minecraft:paper
removed / added        = []
```

不重写 sealed R0。不预分配下一张实现 child。无 wave JSON（可选）。

## 2026-09-02 · 验收

`python tools/build_vanilla_replace_mvp.py --check`：current。
`python tools/build_vanilla_replace_r0.py --check`：current；R0 seal
未改。

`python tools/tests/test_vanilla_replace_mvp.py` 与
`test_vanilla_replace_r0.py`：通过。

`.\gradlew.bat test --tests com.masson.cruciblecraft.datagen.VanillaReplaceMvpResourceTest`：
5/5 通过。

`.\gradlew.bat runGameTestServer -PwaveRecipes=vanilla-replace-mvp --no-daemon`：
隔离 namespace `cruciblecraft_wave_vanilla_replace_mvp` **2/2 passed**
（纸 3→1、熔炉 / 工作台 / 骨粉 fail-closed、datapack reload）。无 T38 式
receipt。`run-wave-vanilla-replace-mvp/` 不是证据。

`python tools/verify.py integration --profile recipes --report-all`：本卡
builder `build_vanilla_replace_r0` / `build_vanilla_replace_mvp` PASS。
同 profile 另有 scope-external 失败，**不是**本卡引入：
`compare_gt6_recipes --check --reference-only` compact snapshot
`13845 != 14201`（mortar / assembler fingerprints）；recipes Python 里
历史 T38–T47 receipt / T5 chemical 等；GameTest receipt `--check` 若干
已关闭波。本卡 `test_vanilla_replace_mvp` / `test_vanilla_replace_r0`
均为 ok。

Gradle `test` 810 项里 `VanillaReplaceMvpResourceTest` 5/5、
`SteamChainResourceTest` 仍断言火箱吃 `minecraft:furnace`。同一次
suite **20 failures / 0 errors**（catalog fixture 计数、Bath harness、
材料 gate 等），与首小时关卡时同一批 scope-external，不是本卡
paper / firestarter 引入的。

`python tools/verify.py dev --path docs/`（加 `--path README.md`
`--path README.en.md`）：markdown-links 与 docs Python 通过。计划写的
`--path docs` 无斜杠会被判 unmatched，与首小时相同。

不把 scope-external 写成 PASS。不跑 R0 `--write`。不创建 Git commit。
