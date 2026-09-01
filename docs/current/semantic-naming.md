# 语义命名残留

> 2026-09-01 停手。活代码表层（作者规则树、测试类名、main Java 卡号文案）已经清过一截。
> 剩下的 TXX 不是没看见，是搬了会牵 seal / 绑定 ID / 闭卡账本，先留下。
> 本文件给以后还想继续改名的人看；日常加配方、改机器不要从这里开工。

零-TXX 扫描器：`python tools/check_zero_milestone_names.py`。
路径里带里程碑 token 的文件**只记路径、不读正文**。全量 `--summary` 会在
`src/recipe_generated` 上拖很久，不要当日常门。

`GT6` / `gt6_*` 是 GregTech 6，不是卡号。扫描器按假阳性处理，不要改。

## 1. 已经搬走的（不要再搬一遍）

作者规则树：

| 旧 | 新 |
|---|---|
| `recipe/t7/mortar/` | `recipe/mortar/` |
| `recipe/t8/extruder/` | `recipe/pipe/extruder/` |
| `recipe/t10/anvil/`、`smelter/` | `recipe/ingot_form/anvil/`、`ingot_form/smelter/` |

`GTRecipeMapLoader.authoredMaterialRuleStage` 按语义前缀认 stage 7/8/10；
`t`+digit 只留给 **stage ≥ 11** 的 authored 树（`t11/future_rule` 仍是 hydrocarbon
material-rule stage，不要盲改成 `hydrocarbon/future_rule`）。

Java 测试类已去卡号，例如：

```text
MortarMaterialRuleDataTest
PipeRuleDataTest
HotIngotRuleDataTest / MultiIngotRuleDataTest
MachineTierArchitectureTest
EnergyChainResourceTest
ComponentRuleDataTest
ChemicalMachineResourceTest / ChemicalResourceTest
CoverResourceTest / PipeAcquisitionResourceTest
CensusRuntimeInventoryClassifierTest
RecipeLogicalRelationIdentityTest
```

`src/main/java` 里后波次 javadoc、报错和私有校验方法（`validateT3` / `validateT5` /
`validateT18a/b/c`）已改成语义名。JSON 绑定字段本身没动。

## 2. 故意不搬

按再动手的代价从高到低。**先改内容引用，再改文件名。** 不要整批 `git mv`
`tools/build_t*.py`：路径 skip 是有意的，上次盲搬会让 ledger 扫描爆掉。

### 2.1 绑定 ID（没有专门迁移卡就不要动）

这些字符串进了 NBT、family id、闸门字段或运行时资源路径，改名等于存档/账本断裂：

```text
FAMILY_ID = "t14_extruder"
NBT  t11_schema_version
/data/cruciblecraft/t11_materials/
t16_publication_baseline.json / t17_publication_baseline.json
以及 t18/t19/t21/t22/t23/t26_5/t28_publication_baseline.json
闸门字段  t38_ / t39_ / t40_ / t48_required_forms
tools/t24_workload_manifest.json
tools/t35_runtime_registry.json
/census/t35_runtime_registry_gate.json
energy_converters.json  的  "stage": "T18a" / "T18b" / "T18c"
t3_acceptance_required_not_gt6_original_gate
t37_t41_aliases_plus_explicit
tools/t42_*.json
```

`EnergyConverterCatalog` 的 Java 方法已经不叫 `validateT18*`，但 catalog JSON 的
`stage` 和 `EnergyConverterCatalogTest` 里按 stage 过滤的断言仍指向这些字面量。

### 2.2 工具链文件名（约 396 个 `build_t*.py`）

`tools/build_t*.py`、`tools/t16_*.json` 一类是**闭卡账本**，不是现行作者 API。
历史 `--check` 收据按文件名找 builder。新工具、新机器、新 wave 不要从这些脚本抄
起步；现行入口是 `tools/verify.py` 和 `tools/build_recipe_bulk.py` 的 slug wave。

`tools/README.md` 开头也写了这两层。

相关 Python 测试类名（`T19PipeAcquisitionTest` 等）跟着 builder 文件名，一并留下。

### 2.3 贴图目录 `t34_gt6`

`src/main/resources/assets/cruciblecraft/textures/block/t34_gt6/`，
加上模型引用和：

```text
assets/cruciblecraft/t34_gt6_art_manifest.json
assets/cruciblecraft/t34_gt6_energy_art_manifest.json
```

要连模型、`ModItemModelProvider` / `ModBlockStateProvider`、
`EnergyChainResourceTest` / `SteamChainResourceTest` 一起改。
manifest 文件名偏出处绑定，改目录时别只改文件夹。

### 2.4 测试夹具与 benchmark

```text
src/test/resources/t39_*_fixture … t45_*_fixture     （约 1300 文件，贴 seal）
src/t14Benchmark/
src/test/java 里 T37–T49 measurement harness
```

夹具树和 seal 字节相邻。改名等于重签或让 `--check` 对不上。没有专门迁移卡不要动。

### 2.5 GameTest 方法名

`CrucibleCraftGameTests` 里仍有 `t18b*` / `t18c*` 方法名。
`@GameTestHolder` namespace 必须是编译期常量；方法名被
`tools/t18_readiness.json` 和 `t18_readiness_policy.json` 的 `required_tokens` 钉住。
要改就 Java + readiness token **同一提交**，不要 `--write` 历史收据来刷绿。

### 2.6 历史与收据（永远不必搬）

```text
docs/history/**
archive/sealed/**
tools/full_verification_report.json
T38–T49 / frozen v2 closeout seals
```

扫描器已豁免 `archive/sealed/**` 和 `docs/history/**`（`card-plans/active` 除外）。

## 3. 再动手也必须守的硬规则

- 不得出现 `t50_*`、`recipe/t50/`、`cruciblecraft_t50`、`-Pt50Recipes`、
  `isT50CompactRecipe`。测试里若已有 T50 **拒绝/负例**字符串，不要扩成真实配方族。
- 不得改写 frozen v2 或 T38–T49 seal 字节。
- 不要为了扫描变绿去 `--write` `t16`/`t17`/历史 readiness。那些 `--check` 可能已经
  stale（分母、缺文件），stale 不是授权重签。
- 不要把 Bath / MTE GameTest harness 当成功门。
- 不要改 `t11/future_rule` 的路径语义，除非单独证明它不再是 authored stage 11。
- 先清正文引用，再改文件名。

## 4. 以后若要继续，建议顺序

1. `t34_gt6` 贴图 + 模型 + manifest（独立、看得见、不碰 seal）。
2. GameTest 方法名 + 对应 `t18_readiness*` token（同一提交）。
3. 绑定 ID：单独开迁移卡，写存档/NBT/family 兼容方案。
4. `build_t*.py` 文件名：最好永远不搬；若搬，先让 closeout resolver 走 archive，
   再改 active 路径。
5. `t39–t45_*_fixture` 最后动，且必须能证明 seal `--check` 仍绿。

每一步：`compileJava compileTestJava`、定向 JUnit、`python tools/closeout_seal.py --check`。
不要跑全量 `recipe_generated` 扫描当完成条件。

## 5. 停手时已知的账本差

这些不是改名引入的，也先不修：

- `ComponentRuleDataTest` 活锁已收到 8426 条 expansion（闸门认了 `t48_required_forms`）。
  `tools/t4_tool_policy.json` 和 `full_verification_report.json` 仍写 8141。
- `t16`/`t17` readiness `--check` 可能 stale。不要为了绿去 `--write`。
- `full_verification_report.json` 里还记着旧测试类路径（`T19CoverResourceTest` 等）。
  那是历史收据，不是 live currentness。

## 6. 日常怎么认路

| 你想做的事 | 看哪里 |
|---|---|
| 加/改现行配方或机器 | 语义路径：`recipe/mortar/`、`recipe/pipe/`、`recipe/ingot_form/`、`recipe/machines/`、wave slug |
| 重放一张已关闭卡 | `tools/build_t*.py` + `docs/history/`，不要改文件名 |
| 怀疑某个 TXX 还能不能动 | 先在本文件 §2 对号；对不上再跑扫描器看它是路径 skip 还是正文命中 |
| 验证改动 | [verification.md](verification.md)，日常 `python tools/verify.py` |
