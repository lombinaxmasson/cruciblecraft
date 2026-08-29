# T36-Repair 详细计划：机器档位扩展闭环

> 阶段：T36-Repair · T36 内部扩展能力 repair gate  
> 状态：✅ 已关闭（`T36_REPAIR_READY`，2026-08-28）  
> 性质：工程修复；`owns_families=0`、`completion_delta=0`  
> 不占用 T44 编号，不撤销 `T36_READY`，不改写 T36 历史 READY 正文  
> 前置：`T43_READY` 且 `failed_gates=[]`  
> 后继：才允许串行签发 Storage 28/624 + logistics 1/1；T44 仍只保留连续编号  
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`  
> 本文件只存在于 `card-plans/closed/`；不得放回 `active/`。

权威对照：

- [T36 详细计划 §3.1](T36详细计划.md)（单一数据源，Java 不得维护第二份 variant 列表）；
- [T36 工作日志](../../work-logs/T36-工作日志.md)（实际按 `target == catalog == runtime` 闭卡）；
- [当前总体规划](../../../current/roadmap.md)；
- [ordinary recipe wave 流程](../../../current/recipe-wave-workflow.md)。

Markdown 不能代替 freeze、second-list inventory、readiness 或 GameTest 收据。T43 已关闭，
无 unique active 内容卡；本文件位于 `card-plans/closed/`。

---

## 0. 先读这一节

T36 完成了 1.x 选定矩阵的注册投影：opening 33 → closing 85 行 / 27 kind，
`target − catalog = 0`。那张卡的退出门测的是**冻结集合相等**，不是
**再加一条明确档位时不必改 Java**。

因此 `T36_READY` 仍然成立：1.x 已选机器行在 runtime 里。T36-Repair 补的是 §3.1 没被
验收的那截：

```text
显式 variant / 显式装置材质
  → 注册、模型、语言、创造栏、壳体、取得配方、装置能力
  → 零 Java 第二份名单
```

禁止自动笛卡尔补全仍然有效。`automaticKindTierCompletion` 必须保持 `false`。本卡不是
「每种机器补全全部材料档」，也不是把 85 行 1.x 分母作废。

坩埚 / 砧 / 锤不在 T36 的 85 行里，但用户要的「材质 → 属性」同一条裂缝也在
`MachineMaterialRules`。本卡把它们收进同一张 repair，避免再关一张只修加工机、坩埚
仍写死允许名单的卡。

---

## 1. 目标与非目标

### 1.1 一句话判据

> 不重开 T36 内容分母；把加工机 variant、壳体物品、kind 取得模板、装置材质规则收成
> 数据权威；用 **test-only overlay** 证明 L1/L2 扩展不改 Java；生产 85 行与现有三档
> 坩埚的玩家可见 identity 保持不变。

### 1.2 扩展等级（必须分别证明）

| 级 | 输入 | 允许改的数据 | 禁止改的 Java |
| --- | --- | --- | --- |
| **L1 加工机** | 已有 kind 行为 + 已有壳体材料 + 一条新的显式 variant 行 | `machine_tiers.json`（或 test overlay）+ 必要 source pin | 注册、datagen、casing `switch`、kind 合成 `switch`、语言 `switch`、`ModBlocks`/`ModItems` 常量名单 |
| **L2 壳体** | `MaterialCatalog` 中已有材料 + 一条壳体 catalog 行 | 壳体 JSON + 模型/语言键 | `ModItems` 逐个 casing 常量、`casingFor()` |
| **装置 L1** | `MaterialCatalog` 中已有材料 + 一条装置材质行 | 装置材质 JSON | `MachineMaterialRules` 允许名单 / processing tier 表 / 创造栏三常量 |

新 **kind 行为**（新 RecipeMap、新槽位、新 BE 逻辑）仍需要 Java adapter。那是 L3，本卡
明确不做。`bronze_crusher` 继续走独立 `CrusherBlock` 与 `skipGenericRegistration`。

### 1.3 生产分母冻结

- live `machine_tiers.json` 仍是 85 行 / 27 kind；opening 33 稳定 id 不改名。
- 不得把 test fixture variant 写进生产 datapack、创造栏或 1.x machine target。
- 不得为了「证明能加档」而签发真实的第 86 台 1.x 机器或第四档生存坩埚。那是后续
  source-backed **内容卡**。本卡只交付能力。
- ordinary family gap、T43 production lock、deferred ledger 本卡 `completion_delta=0`。
- 若统一 authored 取得配方导致 datagen 输出变化，必须证明 85 台生产机器的合成
  **语义等价**（原料集合与结果 variant 不变）；配方路径 id 若必须归一，写入
  migration 表，禁止静默换掉玩家已记住的 recipe id 而不记。

### 1.4 明确不做

- 不撤销 `T36_READY`，不改写 T36 阶段档案的历史闭卡叙述；
- 不自动生成 kind × material / kind × voltage 笛卡尔积；
- 不移植 T13 排除的 kinds，不把 96 kinds 拉进 1.x；
- 不实现 Storage 28/624、不批量 ordinary families、不提高 T14 hard ceiling；
- 不把 compact `publication_group` 白名单从 `GTRecipeMapLoader` 拆走（T43 已要求
  schema-driven group；那是配方波次债，本卡不抢）；
- 不实现 `ParameterizedSpec`；
- 不把坩埚温度公式改成别的倍数，除非 source 或 DESIGN_POLICY 另有冻结；
- 未经授权不 commit。

---

## 2. 已确认的第二份名单（R0 必须建成机器可读 inventory）

下列是 2026-08-28 工作树核对结果。R0 builder 必须逐条哈希/计数，禁止在计划里把
「大概这些 switch」当成关闭证据。

### 2.1 加工机

| 位置 | 缺陷 |
| --- | --- |
| `ModBlocks` / `ModItems` 数十个 `tieredProcessing("…")` 常量 | 注册循环已吃 catalog，但新增 live 行仍要手写别名，否则调用方找不到常量 |
| `ModRecipeProvider.authored` `Set.of` | 跳过 generic 的第二份 path 名单；bath/autoclave/coagulator 等在名单内 |
| `addSourceBackedMachineRecipe` 的 `switch(kind)` | kind → 合成模板不在 catalog |
| `casingFor` / `electrolyzerCableMaterial` / `distilleryWireMaterial` | 材料 → 壳体/线材在 Java |
| `ModLanguageProvider` 的 T16/T17/`materialZh`/`kindZh` | 材料与 kind 显示名第二份表 |
| `machine_tiers.json` 的 `acquisitionBlocker` | schema 有字段，生产行未用；取得权威实际在 Java |
| `ModMachineVariants.OPENING_VARIANT_IDS`、T16/T17 选择集与 kind 静态常量 | 历史开局/分组选集仍由 Java 维护；R0 必须区分“兼容别名”与“新增行必须新增 Java symbol”的权威名单 |
| 手写取得配方与 generic loop 的双路径 | 不能只清点 `authored`：必须为全部 85 台机器冻结输入、结果与配方路径，防止归一化时漏掉原有手写语义 |

通过面（不要拆掉）：`ModMachineVariants.ALL` ← catalog；`registerTieredProcessingBlocks()`
循环；blockstate/model/loot/creative tab 已 `forEach(ALL)`。

### 2.2 装置（坩埚 / 砧 / 锤）

| 位置 | 缺陷 |
| --- | --- |
| `MachineMaterialRules` 允许名单 | ceramic/bronze/steel；stone/iron/bronze/steel；iron/bronze/steel |
| processing tier `Map` | 与 `MaterialDefinition.tier()` 分离，且不在数据里 |
| 砧/锤耐久常数 | Java `switch` |
| `ModCreativeTabs` 坩埚三变体 | 手写 accept |
| 温度 / 壳体质量 | **已经**读 `MaterialCatalog` 熔点与密度；本卡保持该投影，不要改回硬编码温度 |

### 2.3 对照：漏斗

`HopperVariantCatalog` + `registerHopperBlocks()` 是「加行不改 Java」的已有范例。
加工机壳体与装置材质应收敛到同一投影风格，而不是再复制一套 T36 常量别名。

---

## 3. 数据权威（目标形状）

R1 冻结 schema，数字在 builder 复算前保持 `null`。下列是契约，不是预填 JSON。

### 3.1 Kind 取得与显示

每个 processing **kind**（不是每条 variant）在独立 `machine_kinds.json` 中声明。不得把这类
元数据重复写进 85 条 variant 行，或再以 Java `switch` 维护：

```text
acquisition_template   centrifuge | t16_kinetic | t17_heat | electrolyzer
                       | compressor | sifter | machine_generic | none
lang_key_zh / lang_key_en   或 datapack lang 条目，禁止 Java switch
overclock / parallelDuration   已在 variant/kind 行，保持
```

`none` 仅允许 catalog 显式 `acquisitionBlocker` 且 creative-only 或后续内容卡
owner；T36 已要求玩家可见机器有取得路径，本卡不得把 85 行改成 `none`。

### 3.2 壳体 catalog

独立 JSON（不要把壳体物品塞进 `machine_tiers` 的 variant 行里重复 85 次）：

```text
id, material, energy_family (kinetic_double | eu_single | …)
item_id, recipe, model/lang
```

`casingFor(material, energy)` 变为 catalog lookup；未知材料 fail-closed。
电解电缆按壳体材料 extras 键控；蒸馏线材按**机器材料** extras 键控，不放壳体行。

### 3.3 Variant 行

继续持有 source 数字（input 窗、容量、并行、效率、overclock）。新增行只增加
catalog 行 + `source.variantRows` pin。禁止从材料熔点/密度**推导** HU/RU 窗口；
那是 GT source NBT，不是 MaterialDefinition。稀疏覆盖只写偏离 kind 默认的
`acquisition_template` / `casing_item`。

### 3.4 装置材质 catalog

坩埚 / 砧 / 锤各一份允许材料表：

```text
material_id
processing_tier          装置能力，不是材料 tier
durability               若该装置有
creative_visible         bool
```

温度继续 `(°C+273.15)×1.25−273.15` 再 floor。未知 NBT 继续 quarantine 到装置默认材料。
创造栏从 `creative_visible` 投影。

### 3.5 Test-only overlay 装载契约

`MachineTierCatalog` 当前以静态 bundled catalog 装载；R1/R2 必须提供仅测试可用的可注入
resource root 或显式 `CatalogTestSupport`，使 machine tiers、kind、casing 与 device material
catalog 能从同一 test overlay 解析。禁止测试改写生产静态 catalog、写入生产 resources，或把
fixture id 补进 Java 常量 / `Set`。

生产注册仍发生在 NeoForge registry 冻结前的模组启动期。这里的「数据驱动」不承诺 datapack
热重载动态新增 Block/Item；新增行须随模组资源重新构建或重启装载。

---

## 4. 内部波次

这些是同一张 T36-Repair 的里程碑，不另发 T 号。

### R0 · Freeze 与 second-list inventory

新增（名称可在签发时微调，语义不可少）：

```text
tools/t36_repair_pre_freeze.json
tools/t36_repair_second_list_inventory.json
tools/build_t36_repair_pre_freeze.py
tools/build_t36_repair_second_list_inventory.py
```

Freeze 绑定：`machine_tiers.json`、T36 target/census/readiness hashes、T43 closing
readiness（签发时写入）、上述 Java 名单的文件 hash。Inventory 列出每一处第二份名单
的符号、行数与是否会阻止新增行；至少包括 `OPENING_VARIANT_IDS`、T16/T17 选择集、kind
静态常量、取得配方 `authored` 集、generic loop 与 casing/device/lang 表。另冻结全部 85 台的
取得配方语义快照（输入集合、结果 variant、模板、路径 id）。`--check` 在未清零前不得写
`T36_REPAIR_READY`。

### R1 · Schema

- 壳体 catalog + schema；
- `machine_kinds.json` + schema：kind 取得模板与语言键；
- 装置材质 catalog + schema；
- 为 R4 建立仅测试可用的 overlay 装载入口；生产 catalog 不可被测试 mutation；
- `automaticKindTierCompletion=false` 保持加载期硬闸。

### R2 · 加工机投影去第二名单

- `ModBlocks` / `ModItems` 生产路径只保留 `Map<ResourceLocation, Deferred*>`；
  历史常量若保留，必须是 **map lookup 的薄别名**，并由测试证明：catalog 多一行时
  不必增加常量文件；
- 85 台机器统一经 catalog-driven acquisition emitter；Java 只保留以
  `acquisition_template` 为键的通用模板 adapter，不得按 variant path 分流。现有手写配方若
  归一为新路径，必须由 R0 migration 表逐项证明语义等价；
- `casingFor` 与线材 `switch` 删除；
- 语言从材料/kind 数据或 lang 文件投影，删除 T16/T17/materialZh/kindZh 作为权威。

### R3 · 装置投影

- `MachineMaterialRules` 改为读 catalog；
- 创造栏、tooltip、Jade 走同一权威；
- 生产三档坩埚 / 四档砧 / 三档锤的 runtime 数值与闭卡前字节级或语义级相等
  （R0 先冻结期望）。

### R4 · L1/L2 扩展证明（test-only）

隔离测试资源，例如：

```text
src/test/resources/…/t36_repair_overlay/
  extra processing variant: 已有 lathe kind + 已有 invar 壳体
  extra casing row: 仅当测 L2 时使用已有 MaterialCatalog 材料
  extra crucible material: 已有 MaterialCatalog 材料，不得进生产 datapack
```

overlay 经 R1 定义的 test-only loader 一次性装载四类 catalog；它不得 mutation 生产静态
catalog，也不得改变 production registry/census 的输入 root。

必须证明：

1. overlay 加载后多一个 processing block/item id，取得配方能生成且不走
   `IllegalStateException("No kinetic/heat casing")` / `"no source-backed acquisition"`；
2. 装置 overlay 加载后该材质 `isAllowed`、温度跟材料熔点走、创造栏多一个变体；
3. 生产 `machine_tiers.json` 行数仍为 85；census machine runtime 不含 fixture id；
4. overlay 新行不新增 `ModBlocks` / `ModItems` Java symbol、常量别名、`Set` 或 `switch` 分支；
5. 测试失败当且仅当又出现 Java 白名单。禁止用「把 fixture 写进 Java Set」让测试变绿。

### R5 · 生产 85 行回归

- opening 33 id 与 T36 GameTest 语义保持；
- 隔离 `cruciblecraft_t36` 8/8 仍通过，或证明替换套件覆盖同等契约后更新收据；
- datagen 双跑零变化（相对本卡归一化后的生成树）；
- R0 冻结的 85 台取得配方输入集合、结果 variant 与取得路径语义相等；若路径 id 归一，
  migration 表逐项可审计；
- 85 台取得配方可放置为预期 variant。

### R6 · Readiness

```text
tools/t36_repair_readiness.json
tools/build_t36_repair_readiness.py
```

`T36_REPAIR_READY` 当且仅当 `failed_gates=[]`。
`owns_families=0`；`completion_delta=0`；ordinary gap 相对 T43 closing 不变。
`publication_delta` 只允许记录壳体/机器 shaped 配方的规范化，不得出现 compact GT
family。`next_issue_id` 仍为 `T44`；`preassigned_host=false`。
`unique_active_content_card=null`（本卡不是内容卡）。

---

## 5. 验证

签发后至少：

```powershell
python -m unittest discover -s tools/tests -p "test_build_t36_repair*.py"
.\gradlew.bat test --tests com.masson.cruciblecraft.machine.processing.T36RepairExtensibilityTest --tests com.masson.cruciblecraft.machine.MachineMaterialRulesTest --no-daemon
.\gradlew.bat test --tests com.masson.cruciblecraft.registry.T36RuntimeEqualityTest --no-daemon
.\gradlew.bat runGameTestServer -Pt36Machines --no-daemon
python tools/verify.py integration --profile census-replay
```

PowerShell 不得使用 `&&`。具体 test 类名在 R0 冻结；缺测试不得宣称 READY。

扩展证明必须是 **overlay 加载**，不是手工改生产 catalog 再改回去。

---

## 6. 退出门

- [x] `T36_READY` 历史身份未撤销；85 行 live catalog 未扩张进 1.x target
- [x] `automaticKindTierCompletion=false`
- [x] second-list inventory 计数为 0（或每一条剩余项有显式 `later:` owner 与理由；默认不允许残留）
- [x] L1 加工机 overlay 证明：无 Java 名单变更
- [x] L2 壳体 overlay 证明：无 `casingFor` / `ModItems` casing 常量新增
- [x] 装置 L1 overlay 证明：无 `MachineMaterialRules` 允许名单新增
- [x] overlay loader 只读取 test resources，不 mutation 生产 catalog 或污染 production census
- [x] 生产坩埚三材质数值与 R0 freeze 相等
- [x] opening 33 + T36 行为回归通过；85 台取得配方语义与 R0 freeze 相等
- [x] fixture id 未进入生产注册 / census runtime
- [x] `completion_delta=0`；T43 closing gap 不被本卡改写
- [x] `t36_repair_readiness.status=T36_REPAIR_READY` 且 `failed_gates=[]`
- [x] 本计划移入 `closed/`；Storage 仍未预分配编号

---

## 7. 签发与关闭纪律

已签发并关闭（2026-08-28）：

1. 本文件位于 `card-plans/closed/`；
2. T43 closing hashes 已写入 R0 freeze，且 closing gap 仍为 3076；
3. `unique_active_content_card` 仍为 null；本卡关闭后无 active repair gate；
4. Storage / T44 保持未签发；`next_issue_id=T44`、`preassigned_host=false`。
