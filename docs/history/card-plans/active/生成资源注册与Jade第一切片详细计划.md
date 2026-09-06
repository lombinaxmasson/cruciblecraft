# 生成资源、注册与 Jade 第一切片详细计划

> 计划 slug：`presentation/live-art-jade`
> 状态：进行中（active 计划卡；尚未完成实现）
> 正式名称：生成资源、注册与 Jade 第一切片
> 性质：合并候选队列第 3、4 项的有界 registry / generated-art /
> observability 卡。目标是让当前 live catalog 的身份、生成资源、贴图门禁和
> 首批 Jade 状态信息形成可复验闭环。
>
> 本卡不等于全量 GT6_w 搬运，不等于全量 Jade，不实现核电或聚变，不拥有电池
> recipe type。它不创建新的 recipe family、production lock 或 `*_READY` 能力
> token。实施代码真正开始前，机器可读 `unique_active_wave` 保持 `null`。

权威边界来自
[总体规划](../../../current/roadmap.md)、
[冻结与未实现账本](../../../current/unimplemented-gap.md)、
[能力交付流程](../../../current/capability-delivery-workflow.md)、
[GT6 贴图纪律](../../../current/gt6-art-policy.md)、
[验证指南](../../../current/verification.md) 以及本地
`gt6_referencable_port_code/gregtech6_w` / `gt6_code/gregtech6`。
本文只冻结工作范围、归属和验收门，不是 production authority。

---

## 0. 开场证据与归属决策

上一张
[配方加载与 EMI 稳定性](../closed/配方加载与EMI稳定性详细计划.md) 已关闭。配方加载相关
JUnit 已通过；一次 `runtime-java` fresh 执行记录为 **857 tests、4 failures、
约 24 分钟**，因此 `player-complete` 尚未开始。当前失败是 census / registration
基线没有随工作树内容更新，不应回写成配方加载卡的失败。

当前四条差异：

| 测试 | 期望 | 实际 | 本卡处理 |
| --- | ---: | ---: | --- |
| `OreResourceTest` 生成配方 | 1732 | 1745 | 归属新增 13 条的真实 source；若是 live generated resource，本卡负责对齐 |
| `ProcessingMachineResourceTest` `en_us` | 8046 | 8144 | 逐键分类；属于 live machine / Jade / GT6 identity 的由本卡负责 |
| `MaterialCreativeTabTest` `PLATES` | 3195 | 3196 | 识别新增板的 canonical identity、tab owner 和 art/lang |
| `ModRecipesRegistrationTest` recipe type | 5 | 6 | `battery_cell_crafting` 属于电池内容，本卡明确不接管、不改成 5 |

前三条只有在确认其来源属于本卡 owned paths 后才能更新 census。不能为了让数字
相等而删除有效身份、删语言键或隐藏创造栏物品。第四条保留为
scope-external verification debt，电池卡或专门的电池内容工作负责它。

现状基线也必须 fresh 重测：

- [CrucibleJadePlugin.java](../../../../src/main/java/com/masson/cruciblecraft/compat/jade/CrucibleJadePlugin.java)
  已有管道、锅炉、蒸汽机、Crusher、坩埚、锻造砧等 provider，但没有本卡要求的
  Transformer provider，也没有完整的坩埚 K / HU / meltdown / fill / cache
  数据合同。
- 既有资源审计曾发现 block model 自引用、缺失 block item model、registry
  identity manifest 覆盖不足和 generated-tree currentness 漂移；这些数字不能
  直接当作本卡最终分母，必须由当前 revision 的 `--check` 重新产生。
- [TransformerBlockEntity.java](../../../../src/main/java/com/masson/cruciblecraft/energy/transformer/TransformerBlockEntity.java)
  已有 `reversed()`、profile、buffer 和 per-side I/O 语义，可作为 Transformer
  Jade 的 source-backed 数据入口。

决策：

1. 先建立 live catalog → registry → generated resource → art/lang → test
   的归属矩阵，再修数字。
2. 只认当前 live catalog 使用的身份；历史未注册模型、孤儿 `gt_object`、
   未被当前注册表引用的旧 `gt_mte` 不在本卡自动清理。
3. 新贴图只能从本地 GT6 参考树迁移并写入 art manifest；禁止借用另一个 CC
   方块、铁锭、机器 casing 或其它占位图。
4. Jade 只做可由 block entity / capability 真实提供的状态；缺少状态就补
   typed read-only accessor 或标记 blocked，不把静态文字伪装成运行时读数。

---

## 1. 卡片合同

```text
owns_families             = 0
completion_delta          = 0
new_recipe_family         = 0
new_recipe_map            = 0
production_lock           = null
nuclear_started           = false
unique_active_wave        = null until implementation starts
```

本卡可以新增或修正 live registry identity、generated model / blockstate /
loot / tag / language / art manifest 和 Jade projection，但不发布新的 recipe
family。已有工作树生成文件只有在 canonical catalog、datagen 或 source-backed
art 变更后按生成器重生；不能手工删文件来通过计数。

### 1.1 In scope

- live GT block、GT stone、semantic object、Multiitem、Bath / Smelter MTE、
  building-block identity 的注册路径和命名审计；
- registry collision / duplicate identity / existing-item reuse 的完整检查；
- blockstate、block model、block item model、loot、mineable tag、language 和
  art manifest 的 currentness；
- `runData` 的隔离输出、双跑确定性和 generated tree 不误删；
- 当前 live catalog 使用的 GT6 贴图迁移；
- `OreResourceTest`、`ProcessingMachineResourceTest`、
  `MaterialCreativeTabTest` 中属于本卡来源的 census 对齐；
- 首批 Jade：Crucible + Transformer，以及它们所需的翻译和 typed telemetry。

### 1.2 Out of scope

- `battery_cell_crafting` recipe type、Battery Cell 配方语义和电池内容闭环；
- Ore / machine 配方新 family、recipe load / EMI runtime 重写；
- Reactor Core、Fusion Reactor、核安全、热流体、Canner、冷却器、热交换器、
  蒸汽涡轮和 `nuclear_started`；
- 全量 Jade provider（Logistics Core、Tank、Dynamo、Fusion 等）；
- 专用 GUI；Jade 是本卡的观测出口；
- 全量历史 `gt_object` / `gt_mte` 孤儿资源清理；
- 用统一铁锭、copper/furnace cube、其它 CC 方块或 `metal_surface` 代替真实
  GT6 art；
- 修改 load hard ceiling、修改旧 closed card 的 sealed artifact 或自动创建
  capability。

---

## 2. Owned paths

实现时允许触及以下路径；新增测试放在对应 package 下：

```text
src/main/java/com/masson/cruciblecraft/datagen/**
src/main/java/com/masson/cruciblecraft/compat/jade/**
src/main/java/com/masson/cruciblecraft/registry/ModItems.java
src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java
src/main/java/com/masson/cruciblecraft/registry/ModBlockEntities.java
src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java
src/main/java/com/masson/cruciblecraft/content/item/*Catalog.java
src/main/java/com/masson/cruciblecraft/content/blockentity/CrucibleBlockEntity.java
src/main/java/com/masson/cruciblecraft/energy/transformer/**
src/main/resources/assets/cruciblecraft/**
src/generated/resources/assets/cruciblecraft/**
src/generated/resources/data/cruciblecraft/**
src/test/java/com/masson/cruciblecraft/datagen/**
src/test/java/com/masson/cruciblecraft/registry/MaterialCreativeTabTest.java
tools/block_art.py
tools/build_block_art.py
tools/multiitem_art.py
tools/build_multiitem_art.py
tools/registry_identity.py
tools/build_registry_identity.py
tools/tests/**（仅本卡对应的 generator / registry / art 测试）
build.gradle（仅 runData / generated-output 隔离）
docs/current/**
```

`src/main/java/com/masson/cruciblecraft/registry/ModRecipes.java`、
`src/main/java/com/masson/cruciblecraft/recipe/crafting/**` 和电池 cell
recipe JSON 不属于本卡，即使它们在当前工作树中存在。`run-game-test-filtered/**`
和 `build/**` 是证据，不是提交对象。

---

## 3. 实施阶段

### A. Census 与 ownership matrix

先为每个新增或变化的 live object 记录：

```text
canonical identity
source catalog / source revision
CC registry path
existing_item | new_distinct | reuse_canonical | blocked
block / item / loot / tag / language / art outputs
creative-tab owner
test owner
```

对 runtime-java 的四条失败逐项给出：

- actual source file / generator；
- 是否属于本卡；
- 若属于本卡，新的 expected census 从哪个 live manifest 派生；
- 若不属于本卡，转交哪个 capability / 内容卡；
- 不允许用 `expected = actual` 作为无来源修复。

本阶段结束前不大规模重命名，也不批量删除历史模型。

### B. Registry identity 与命名

1. 扩展 registry identity 检查覆盖面，至少纳入当前 live 使用的：
   `SemanticObjectCatalog`、Multiitem / Bath、Smelter MTE、GT block object、
   GT stone 和 building-block catalog。
2. 对每个 collision 判断：
   - 完全同一对象：复用 canonical registry path；
   - 同源不同语义：保留 distinct identity 并记录 disposition；
   - 只有显示名相似：不能据此合并；
   - 历史资源未被 live catalog 引用：归档/报告，不自动删除。
3. live 命名使用 CrucibleCraft semantic path；不把 `gt.meta.*`、数字 meta、
   `m<meta>` 或 card number 作为当前 registry authority。
4. `registry_identity_manifest.json` 必须由完整 collector 生成，`--check`
   能发现 stale manifest、重复 registry path、缺失 live entry 和额外未声明
   entry。
5. `MaterialCreativeTabTest` 的 PLATES `+1` 必须能追溯到 canonical identity、
   source material form、item model、lang 和 tab owner；若不是本卡对象则转出，
   不删除它。

### C. Generated resource 与 art gate

1. `runData` 输出到隔离的 generated tree / 临时树后比较；不能在检查前无条件
   删除整棵现有 generated 目录，不能把别的 generator 的有效输出变成“缺失”。
2. 对每个 live block / item 检查：
   - blockstate 的所有实际 variant 有模型；
   - block model 不自引用；
   - block item model 存在并指向正确 block / item geometry；
   - loot 和 mineable tag 与注册身份一致；
   - language key 在生成树中有明确来源；
   - `gt6_import` 或现有 GT6 family art manifest 记录 source → gt6 source →
     destination。
3. 真实 GT6 art 缺失时记录 `blocked` / `ART_MISSING`，不得以铁锭、铜块、
   炉子、`multiblock_casing` 或其它 CC 纹理过门。
4. 只处理 live catalog 使用的建筑方块、Multiitem 和机器资源。历史 2347
   个未绑定 `gt_object` 模型不作为本卡清理分母。
5. 连续两次运行 datagen，比较文件清单、内容和 manifest hash；generated
   currentness 漂移必须能定位到 source input。

### D. Jade 第一切片

建立最小 typed telemetry projection，禁止在 provider 中拼装不可验证的 NBT
字符串。

**Crucible：**

- Temperature(K)；
- Buffered Heat(HU)；
- Meltdown at(K)；
- Fill level(%)；
- Render / active state；
- Contents；
- Cache slot（只有 block entity 真正有缓存槽时显示；否则明确显示
  unavailable，不虚构槽位）。

坩埚现有 Celsius 温度、填充比例、内容物和 casing 数据可以作为 projection
输入；单位转换必须集中在显示层，canonical runtime 值不改。

**Transformer：**

- profile / tier；
- step-up / step-down 或 `reversed` mode；
- buffer / activity；
- 每一面是输入还是输出；
- 每一面允许的 voltage / packet size；
- 对未知方向、无连接方向和客户端缺少数据的显示策略。

Transformer 的方向和 mode 必须来自现有 `EnergyTransformerProfile`、
`reversed()`、`outputSides()` 等真实接口；不能用方块朝向猜测高低压。

Jade provider 的 UID、common data provider、客户端组件和中英文 translation
key 必须一一对应。此阶段不加入 Reactor Core、Fusion、Logistics Core 或
全机器 generic provider。

### E. Verification census 与门禁

本卡实施中可以更新三类归属明确的测试基线：

- `OreResourceTest` 的 1732 → 1745：只有 13 条确实来自本卡 live resource
  generator 时才能改；
- `ProcessingMachineResourceTest` 的 8046 → 8144：逐键归属后更新；
- `MaterialCreativeTabTest` 的 PLATES 3195 → 3196：完成 identity / art /
  tab owner 后更新。

`ModRecipesRegistrationTest` 的 5 → 6 仍由
`battery_cell_crafting` 内容所有者处理。本卡不得删掉该 recipe type、改写
电池 serializer、或把 expected 5 改成泛化的“至少 5”。

---

## 4. 验收门

### 4.1 Registry / generated-art

```text
live registry path collision                  = 0
stale identity manifest                      = 0
live entry without declared owner            = 0
self-parent live block model                = 0
missing live block item model               = 0
missing live loot / tag / language owner    = 0
unmanifested live GT6 art                   = 0
datagen double-run drift                    = 0
creative-tab duplicate entry                = 0
owned census delta without source           = 0
```

`ART_MISSING`、`ART_PLACEHOLDER`、未解释 collision 或 generator currentness
drift 都是本卡 `BLOCKED`，不能通过减少检查范围关闭。

### 4.2 Jade

```text
Crucible provider fields have source-backed values
Crucible K / HU / meltdown / fill units are explicit
Transformer mode and per-side voltage are source-backed
provider UID collision                          = 0
owned Jade en_us / zh_cn keys missing           = 0
client tooltip reads stale epoch/state          = 0
```

没有实际 cache slot 或 render state 时，验收允许记录 `unavailable`，不允许
显示一个看似真实的固定值。

### 4.3 Test

至少 fresh 运行：

```powershell
python tools/build_block_art.py --check
python tools/build_multiitem_art.py --check
python tools/registry_identity.py --check
python tools/verify.py dev
```

并运行本卡涉及的：

```text
OreResourceTest
ProcessingMachineResourceTest
MaterialCreativeTabTest
MaterialVisualResourceTest
Jade provider / language coverage tests
```

`runtime-java` 必须重新跑一次以确认影响面。若唯一剩余失败是
`battery_cell_crafting` 的 `ModRecipesRegistrationTest`，本卡可以记录自身
门已通过但不能把全局 `player-complete` 标为通过；该 external debt 必须继续
留在 known-issues / 电池内容卡。

---

## 5. 明确阻塞与撤回

必须 `BLOCKED` 的情况：

- live identity 没有 canonical source 或出现未决 collision；
- 只能用其它 CC 纹理、铁锭、vanilla 方块或历史模型充当新 art；
- generated tree 只能靠删除其它 generator 输出才能通过；
- 三个 owned census delta 没有 source / manifest / test owner；
- Jade 字段没有运行时来源，只能写静态或猜测值；
- 为了通过全局 `runtime-java` 而接管 `battery_cell_crafting`；
- 需要改写 closed card、历史 seal 或放宽 test 断言。

撤回只撤本卡，保留 ownership matrix、失败 manifest、collision、art gap 和
Jade field gap。关闭时把本文件移至 `card-plans/closed/`，更新
[history INDEX](../../INDEX.md)、
[roadmap](../../../current/roadmap.md)、
[unimplemented-gap](../../../current/unimplemented-gap.md) 和
[known-issues](../../../current/known-issues.md)；不自动创建 Git commit。
