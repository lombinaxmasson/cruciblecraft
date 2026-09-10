# MTE 身份分母处置 R0 详细计划

> 计划 slug：`portfolio/mte-identity-disposition-r0`
> 状态：**已关闭**（2026-09-10）。本文件位于 `card-plans/closed/`。
> `MTE_IDENTITY_DISPOSITION_R0_READY`：catalog **1,817** 个 `mte_item`
> 全部有家族与 disposition（`unmatched = 0`）；流体附件子集 **33**
> （龙头 22 + 流体龙头 3 + 喷嘴 2 + 帽喷嘴 3 + 流体漏斗 3）可行性
> `requires_new_runtime`；Dust Funnel `32704` `realized_natively`；
> Wooden Panel **23** 只进装饰族一次；Loader tag `Axles and Gearboxes`
> 活目录 **63** 行 `identity_only`（预读 54 是名称抽样；同 tag 含轴 /
> 齿轮箱 / 旋转引擎与变压器，不得拆族）；`nuclear_started = false`；
> `allows_implementation_child = false`；不实现行为、不发布配方、
> 不创建 `capability.json`；`owns_families = 0`；`completion_delta = 0`；
> `generated_recipe_count = 0`；`production_lock = null`；
> `unique_active_wave = null`；`next_unassigned = true`。
> 正式名称：MTE 身份分母处置 R0
> 性质：零 family 的冻结卡。给
> `smelter_mte_identity_catalog.json` 全部 1,817 个 `mte_item` 落
> 家族归属与 disposition，并判定 catalog 内流体附件子集的可行性。
> **不实现任何行为、不发布任何配方、不创建 `capability.json`。**
> Opening：切片机已关为 `runtime_ready`；七张加工机 prep 排队，不占
> 落地锁；对照图「Bath / Smelter MTE holdable identity」行已是
> `correspondence_class = identity_only`（身份不是行为生成器）。
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 目标状态：`MTE_IDENTITY_DISPOSITION_R0_READY`
> 目标计数：`owns_families = 0`、`completion_delta = 0`、
> `generated_recipe_count = 0`、`production_lock = null`
> 所属 growth-order：不改 sealed `growth_order.json`。本卡**不是**第三轨
> `portfolio/existing-mechanism-bounded-domains` 的第三条 `owns` 义务。
> 前置：源能力对照图、通用 Source Pack 导入器、
> `recycling/smelter-mte-identity`（`SMELTER_MTE_IDENTITY_READY`：
> 1,817 meta，1,771 scatter + 46 `bath_mte`）
> 明确不包含：任何行为实现、任何配方或 production lock、新 runtime、
> capability 晋级、storage/lock 分母改动、机器身份重映射、GT6 未进
> catalog 的 MTE 变体注册、核能、玩家发行
> 签发层级：当前工作树；签发与关闭都不自动创建 Git commit

```text
lane                         = closed
capability_slug              = portfolio/mte-identity-disposition-r0
unique_active_wave           = null
prep_owned_paths             = tools/waves/portfolio/mte-identity-disposition-r0/**
                               tools/build_mte_identity_disposition_r0.py
landing_owned_paths           = 无。不改 ModBlocks / ModItems / ModBlockEntities /
                               ModMenus / ModRecipeMaps / ModProcessingMachines /
                               capability.json / src/recipe_generated/**
landing_depends_on           = 冻结卡已关；不占加工机落地锁
partial_close_allowed         = false
```

权威边界来自
[总体规划](../../../current/roadmap.md)、
[能力交付流程](../../../current/capability-delivery-workflow.md)
以及已 sealed 的 source-capability growth-order。Markdown 不是 production
authority。本计划只签发工作边界；R0 的机器可读 artifact 在实施时由
builder 生成。签发当下不创建 `tools/waves/**`、不翻转
`unique_active_wave`。

---

## 0. 决策

`smelter_mte_identity_catalog.json` 是已经进游戏、仍未按行为认领的大分母：
1,817 个 GT6 `MultiTileEntity` 物品身份全部是无行为 `CatalogNamedItem`
（1,771 靠 `smelter_mte_scatter`，46 靠 `bath_mte`）。对照图已经有一行
「Bath / Smelter MTE holdable identity」：`identity_only`，
`recheck_condition = Identity is not a behavior generator.`
本卡不重开那一行，也不把它改成 complete。

这不是「缺 1,817 个实现」。按 GT6 `Loader_MultiTileEntities.java` 的
`add(name, tag, id, …)` 与循环注册归族之后，绝大多数是家具/储物散落物
或 CC 已有原生机制的遗留身份。真正牵动新 runtime 的，是 catalog 里已经
注册的流体附件子集：**33** 个身份、4 种行为。

缺口账本第 2 节写过「37 行 `none` 不是 37 张新冻结 R0」。本卡不是那类
卡：身份行已经判过；本卡只把 1,817 个散落物逐行写清去向，并给从未单独
判过可行性的附件行为做 R0。建筑方块 identity、聚变、Bath form/object
仍按短队列，不因本卡自动让路。本卡值得做，是因为这 1,817 个物品**已经
在世界里当垃圾掉落**。

结构是两层。第二层本卡不签发、不预分配：

```text
portfolio/mte-identity-disposition-r0   （本卡：冻分母、判 disposition）
  -> 后续 runtime / 内容卡               （候选名不定；本卡 topology 后继保持未分配）
```

---

## 1. 完成后应该成立的工作流

```text
pinned capability-map row
  "Bath / Smelter MTE holdable identity"  byte-identical
  + smelter_mte_identity_catalog.json     1,817 identities
  + Loader_MultiTileEntities.java         add()/loop id → tag/class
  -> inherited_denominator.json
  -> source_semantics.json
       catalog 1,817 是本卡分母
       排除表 Crucibles Faucets 39 是 GT6 注册上下文，不是本卡行数
       未进 catalog 的 GT6 变体本卡不给 disposition
  -> family_map.json
       每个 meta = GT6 MTE id；家族来自 Loader tag / 循环方法
       映射失败必须人工行 + evidence；关闭时未归类 = 0
  -> disposition_ledger.json
       1,817 行，无空 disposition
  -> existing_mechanism.json
  -> attachment_contract.json
       后续实现卡必须回答什么；本卡不得发明答案
  -> feasibility.json
       仅 catalog 流体附件子集
       bounded_extension | requires_new_runtime | defer_to_portfolio | blocked
  -> MTE_IDENTITY_DISPOSITION_R0_READY
       或 BLOCKED / DEFERRED，缺的证据写名
```

关键分界：

1. R0 不写 Java、不注册方块/BE、不改盖板、不跑 GameTest、不签
   production lock、不写 `src/recipe_generated/**`、不创建
   `capability.json`。
2. `owns_families` 仍为 0。不得把 1,817 个身份写成 census complete。
3. 本卡分母是 catalog 身份，不是 GT6 全量 MTE 注册表，也不是排除表
   第九表的 39 个 Faucets。
4. 只有可行性为 `bounded_extension` 才允许**另签** implementation
   child。预读附件子集是 `requires_new_runtime`，本卡 topology 后继
   保持未分配。
5. capability map 身份行、sealed growth-order、已关闭
   `SMELTER_MTE_IDENTITY_READY` 在本卡只读。
6. `identity_only` 是本卡冻结默认值，不是永久产品禁令。后续
   realization 卡可以改 `out_of_scope` 或折回原生物品，必须另开卡，
   不得在本卡假装已经折回。

---

## 2. Card contract

### 2.1 一张卡，不是 parent program

```text
本计划签发（unique_active_wave 仍为 null）
  -> 未来 R0 --write 才把 unique_active_wave
       设为 portfolio/mte-identity-disposition-r0
  -> MTE_IDENTITY_DISPOSITION_R0_READY
       或 BLOCKED / DEFERRED
  -> unique_active_wave = null
  -> next_unassigned = true
```

签发当下机器可读事实：

```text
unique_active_wave = null
```

全程固定：

```text
owns_families             = 0
completion_delta          = 0
partial_family_count      = 0
production_lock           = null
generated_recipe_count    = 0
gametest_status           = NONE
nuclear_started           = false
allows_implementation_child = false
```

不适用普通 recipe wave 的 300-family 下限。不得借这个例外发布任意
recipe。

### 2.2 实施时才生成的标准 artifact

签发当下不创建下列路径。实施 R0 时 builder 必须写出：

```text
tools/waves/portfolio/mte-identity-disposition-r0/wave.json
tools/waves/portfolio/mte-identity-disposition-r0/census_delta.json
tools/waves/portfolio/mte-identity-disposition-r0/topology.json
tools/waves/portfolio/mte-identity-disposition-r0/readiness.json
tools/waves/portfolio/mte-identity-disposition-r0/closeout_seal.json
```

以及本卡特有：

```text
tools/waves/portfolio/mte-identity-disposition-r0/inherited_denominator.json
tools/waves/portfolio/mte-identity-disposition-r0/source_semantics.json
tools/waves/portfolio/mte-identity-disposition-r0/family_map.json
tools/waves/portfolio/mte-identity-disposition-r0/disposition_ledger.json
tools/waves/portfolio/mte-identity-disposition-r0/existing_mechanism.json
tools/waves/portfolio/mte-identity-disposition-r0/attachment_contract.json
tools/waves/portfolio/mte-identity-disposition-r0/feasibility.json
```

builder（实施时，不是本签发）：

```text
python tools/build_mte_identity_disposition_r0.py --write|--check
```

关闭 artifact 只证明分母、家族映射、disposition 与附件可行性 current。
不允许出现 production publication、GameTest receipt、load winner 或
`src/` 行为 diff。

---

## 3. 权威分母

固定 GT6 revision `3703e40308c8c030763fd6297dea8b210d2a77b1`。
权威表：

```text
src/main/resources/data/cruciblecraft/smelter_mte_identity_catalog.json
tools/waves/recycling/smelter-mte-identity/readiness.json
tools/waves/portfolio/source-capability-inventory/capability_map.json
gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java
```

`inherited_denominator.json` 必须同时钉住：

1. catalog `identities` 长度 = **1,817**，`kind = mte_item`，
   `registry_path = gt_mte/mte_<meta>`，`meta` 等于 GT6 MTE id；
2. `acquisition_authority`：`smelter_mte_scatter` **1,771**、
   `bath_mte` **46**（与 catalog `bath_overlap_count` 一致）；
3. 对照图「Bath / Smelter MTE holdable identity」行 byte-identical：
   `correspondence_class = identity_only`，不得改成 complete。

签发预读（实施时必须用 catalog 重放，不得手填）：

| 子集 | catalog 行数 | 预读 identity（`english_name` / meta） |
| --- | ---: | --- |
| Crucible Faucet | 22 | Stone 1700、Bronze 1720、Invar 1721、Steel 1722、Titanium 1723、Tungsten 1724、Stainless Steel 1725、Knightmetal 1727、Fiery Steel 1728、Thaumium 1729、Void Metal 1730、Meteoric Iron 1731、Meteoric Steel 1732、Chromium 1733、Molybdenum 1734、Osmium 1737、Vanadium 1738、Iridium 1739、HSLA-Steel 1741、Octine 1742、Netherite 1744、Adamantium 1749 |
| Fluid Tap | 3 | Stainless 32730、Tungsten 32731、Adamantium 32732 |
| Fluid Nozzle | 2 | Tungsten 32749、Adamantium 32750 |
| Cap Nozzle | 3 | Tungsten 32061、Adamantium 32062、Tantalum Hafnium Carbide 32082 |
| Fluid Funnel | 3 | Stainless 32725、Tungsten 32726、Adamantium 32727 |
| **附件候选合计** | **33** | 上五类；disposition = `attachment_candidate` |
| Dust Funnel | 1 | 32704；disposition = `realized_natively`（CC `steel_dust_funnel`） |

这 33+1 是本卡附件相关的**精确** catalog 行。漏斗是 3 个，不是 2。
关闭 `--check` 必须复算这 34 个 meta，不得用「约 32–33」过门。

### 3.1 三套数字，只冻一套

| 数字 | 来源 | 本卡用法 |
| --- | --- | --- |
| **1,817** | `smelter_mte_identity_catalog.json` | **唯一分母**。每行一条 disposition |
| Crucibles Faucets **39** | 表现层分母 / GT6 tag `Crucibles Faucets` 注册行 | 上下文。含 `NBT_HIDDEN`、跨模组石材、陶瓷 1705 等未进 catalog 的 id |
| GT6 龙头 6 / 漏斗 6 / 喷嘴 6 / 帽喷嘴 6 | `Loader_MultiTileEntities.java` Misc Tool Blocks | 上下文。catalog 分别只有 3 / 3 / 2 / 3 |

未进 catalog 的 GT6 变体（陶瓷龙头 1705、塑料龙头、隐藏玄武岩龙头 1701
等）本卡**不写 ledger 行**。后续 runtime 卡若要注册它们，是新身份
（`new_distinct`），不是改这 1,817 行。

排除表「零 disposition、零 owner」描述的是历史第九表，不是现行
catalog 已注册、对照图已标 `identity_only` 的事实。`source_semantics.json`
必须把这两句话分开。

---

## 4. 家族映射

映射源：`Loader_MultiTileEntities.java` 的 `aRegistry.add(name, tag, id, …)`
字面量，以及 `metalset` / `storages` / `crucible` 等循环里用 `aID` /
`500+aID` 算出的 id。catalog `meta` 就是这个 id（预读：Stone Faucet
`meta = 1700` 对 Loader 1700）。

规则：

1. 先按 GT6 `tag`（创造页名）归族；同一 tag 不得拆进两个家族，除非
   ledger 写明子簇（例如 Misc Tool Blocks 里的附件子集）。
2. 映射不到的 meta 必须人工行：`gt6_class_or_tag = unmatched`，
   `evidence` 写 Loader 行号或「Loader 无此 id」。关闭时 `unmatched = 0`。
3. Wooden Panel 23 只进装饰/家具族一次，禁止同时计入「其他」。
4. Bath 46 在 ledger 标 `realized_natively`，`evidence` 指向
   `BathMteIdentityCatalog`；本卡回声所有权，不抢。
5. 家族人数以 `family_map.json` / ledger 为准。下表是 2026-09-10
   **预读线索**，不是关闭计数；`--check` 不得拿估计数当真。

| 家族线索 | catalog 预读 | 默认 disposition 方向 | 不得当成关闭数的原因 |
| --- | --- | --- | --- |
| 家具/储物 | 箱/柜/抽屉/桶等为主 | `identity_only` | 名称抽样，不是 tag 全量 |
| 机器 / 能量机 | 含 Basic Machines、锅炉、引擎等 | `realized_natively` | CC 已有 kind 主机；散落物不是那台方块 |
| 坩埚 / 模具 / Basin / Crossing | Basin 22、Crossing 22 已见 | 坩埚/模具/Basin/Crossing → `realized_natively`；**Faucet 22 不在此列** | Faucet 走附件候选 |
| 传动 | Axle 36 + Gearbox 18 = **54** | `identity_only` | CC 旋转能是另一套对象；不得因「已有 KU」标 `realized_natively` |
| 装饰 | Wooden Panel 23、Steel Rope 1 | `identity_only` | — |
| 红石线线索 | Red Alloy Wire 27000、Signalum Wire 27050、Lumium Wirelamp 27500 | 冻结时按 Loader tag 落 | 名称像线，未必是红石线 |
| 延伸件 | Tank Extender 30001 **1** 个 | 冻结时按类落 | 不是「Extenders 2」 |
| 附件子集 | **33** + Dust Funnel 1 | 见 §3 | 已钉死 |
| 其他 | 映射余量 | 关闭前必须消化到 0 | 预读不得预留「约 90」当合法桶 |

堆芯 / 棒相关 MTE 若出现在这 1,817 里：ledger 标 `realized_natively` 或
`deferred`，evidence 指向已关闭裂变卡；不改 `nuclear_started`，本卡不是
核电 owner。

---

## 5. 判定规则（disposition 四类）

这是**散落物去向**轴，不是 `capability.json` 的
`identity_disposition`（`reuse_canonical` / `new_distinct` / `bridge` /
`blocked`），也不是 `player_complete`。两套词不得混写。

每个身份在 freeze 时落一个 disposition，写入
`disposition_ledger.json`：

| disposition | 含义 | 适用 |
| --- | --- | --- |
| `identity_only` | 本卡冻结为展示/散落身份，**不要求**本卡补行为或配方。后续卡可以再分类，本卡不折回 | 家具/储物主体；传动 54；面板、绳子等装饰件 |
| `realized_natively` | CC 已有具名原生机制；本身份是遗留散落物。本卡不重映射 registry path | 已有 ProcessingMachine / 能量机 kind；坩埚与陶瓷模具；Dust Funnel；Bath 46 |
| `attachment_candidate` | 可行性见 `feasibility.json`；点名后续 runtime 卡，本卡不预分配 | Faucet 22、Tap 3、Nozzle 2、Cap Nozzle 3、Fluid Funnel 3 |
| `deferred` | 归属另一个**已点名**域，本卡不判可行性 | 仅当 evidence 写得出目标 slug；不得当未归类垃圾桶 |

规则：

1. 不得把任何身份直接标 `player_complete`。本卡是冻结卡。
2. 不因「身份已注册」就要求补配方。`identity_only` 是合法冻结终态。
3. `attachment_candidate` 只标可行性，不写 runtime 计划、不写 owned
   paths、不定后续卡 slug。
4. 每行必有：`registry_path`、`meta`、`english_name`、`family`、
   `gt6_class_or_tag`、`disposition`、`evidence`。
5. `realized_natively` 的 evidence 必须点名现行 CC 类型或 catalog
   （例如 `DustFunnelBlock`、`CeramicMoldBlock`、
   `BathMteIdentityCatalog`），不得写「CC 大概有类似的」。

---

## 6. 现有机制适配面

`existing_mechanism.json` 必须把每个相关机制写成「覆盖什么 / 不覆盖什么」：

| 机制 | 现行路径 | R0 用法 |
| --- | --- | --- |
| Smelter / Bath MTE 身份 | `SmelterMteIdentityCatalog` / `BathMteIdentityCatalog` | 本卡分母来源。身份 ≠ 行为 |
| 仓储 | storage/lock 28/624 | 已关闭注册与单物品容量。前缀单位换算仍缺（缺口账本第 4 节）。本卡不改那张分母，不把箱子散落物折回原生物品 |
| 加工机 / 转换机 | `machine_tiers.json` / `ModProcessingMachines` / 转换机目录 | 机器系 `realized_natively` 的证据。散落 MTE 物品不是那台方块 |
| 坩埚浇铸 | 陶瓷坩埚方块 + `CeramicMoldBlock` 邻接倒液 | **已经能浇铸，不用龙头。** 缺的是 GT6 浇铸口附件，不是「完全不能浇」 |
| 粉尘漏斗 | `steel_dust_funnel` / `DustFunnelBlock` | Dust Funnel `realized_natively`。不做物品桶、不加 `blockDust` |
| 旋转能 | 独立 KU/RU 轴与齿轮 | 不是 GT6 `MultiTileEntityAxle` / `GearBox` 身份。传动 54 默认 `identity_only` |
| 管道盖板 | `CoverBehaviorRegistry` | **不是** `TileEntityBase11AttachmentSmall`。可作「邻面有注册」的弱类比，不得当可复用实现，也不得据此判 `bounded_extension` |
| 流体容器 | `CellItem` / `CellFluidHandler`；[容器 ADR](../../../decisions/容器身份与边界ADR.md) 排除 Canner | 漏斗日后的目标容器线索，不是本卡实现 |
| 气体云 | `GasCloudBlock` | 喷嘴世界落点线索，不是本卡实现 |
| 对照图 misc | Display CPU 已 `player_complete`；其余 unowned 仍 none | 附件**行为**以前落在这里。身份行不在这里 |

不得把盖板栈、邻接模具浇铸或 `FluidUtil` 调用点写成已经覆盖龙头 / 喷嘴 /
流体漏斗。

---

## 7. 契约与可行性（仅 catalog 流体附件子集）

`attachment_contract.json` 只冻结后续实现卡必须回答的问题，不实现它：

```text
host_bounds          哪些 BE 实现 tapDrain / nozzleDrain / funnelFill /
                     坩埚倒液；GT6 还打在桶、基础机、堆芯、热流体机、
                     多方块储罐上，不得暗示「储罐+坩埚+管道三个接口就够」
placement            AttachmentSmall 贴面放置 vs 管道盖板；禁止塞进
                     CoverBehaviorRegistry 冒充完成
four_behaviors       浇铸口 / 龙头 / 喷嘴 / 流体漏斗 四套结算，不得合成
                     一个 ITileEntityTapAccessible 就宣称四种都齐
uncatalogued_gt6     是否注册 catalog 没有的变体（陶瓷龙头 1705 等）；
                     若注册则是新身份，不改本卡 1,817 行
player_acquisition   生存如何获得 33 个已注册身份（本卡不写配方）
fail_closed          未知附件变体不得 silent no-op
delete_later         某一行为 later out_of_scope 时如何保留本卡 hash
```

四种行为共享的是 GT6 `TileEntityBase11AttachmentSmall` **放置**，不是
同一套 drain 接口：

| catalog 身份 | 行数 | GT6 类 | 宿主合同 |
| --- | ---: | --- | --- |
| Crucible Faucet | 22 | `MultiTileEntityFaucet`（**不是** `MultiTileEntitySmeltery`） | 贴坩埚；`ITileEntityMold`；向下方模具倒熔融金属 |
| Fluid Tap | 3 | `MultiTileEntityFluidTap` | `ITileEntityTapAccessible.tapDrain`；向下倒液体、拒气 |
| Fluid Nozzle / Cap Nozzle | 2+3 | `MultiTileEntityFluidNozzle` / `FluidCapNozzle` | `nozzleDrain` 向世界喷气 |
| Fluid Funnel | 3 | `MultiTileEntityFluidFunnel` | `ITileEntityFunnelAccessible.funnelFill`；对容器物品装/取液 |
| Dust Funnel | 1 | `MultiTileEntityDustFunnel` extends `TileEntityBase07Paintable` | **不是**附件件；已 `realized_natively` |

`feasibility.json` 对附件子集取值（与作物 R0 同一词表）：

```text
bounded_extension     可由具名现有 CC runtime 有界扩展承载
requires_new_runtime  必须另开附件放置 + 各行为结算
defer_to_portfolio    合法去向是另一个已命名 portfolio
blocked               缺 catalog、缺 Loader 映射或缺证据，不能下结论
```

预读（实施时必须用源码重放，不得手填）：附件子集
`requires_new_runtime`。理由：

- 33 个身份仍是无行为 `CatalogNamedItem`；无对应方块 / BE / GameTest。
- CC 没有 `ITileEntityTapAccessible` / `ITileEntityFunnelAccessible` /
  浇铸口贴面放置。
- 邻接模具浇铸已经存在，所以缺口是附件行为，不是铸造 RecipeMap。
- `CoverBehaviorRegistry` 不能当 `bounded_extension` 的证据。

不得引用路线图「选卡顺序第 2 条」（那是配方波 family / exact-relation
模板，不是附件 runtime）。`requires_new_runtime` 不得在本卡 topology
预分配实现后继。R0 仍可 `MTE_IDENTITY_DISPOSITION_R0_READY`：只证明
1,817 行 disposition 与附件可行性 current。

---

## 8. 验收门

签发当下无机器门。未来 R0 `--check` 必须同时成立：

```text
MTE_IDENTITY_DISPOSITION_R0_READY   # 或 BLOCKED / DEFERRED，不得假装 READY

owns_families          = 0
completion_delta       = 0
partial_family_count   = 0
generated_recipe_count = 0
production_lock        = null
gametest_status        = NONE
nuclear_started        = false
allows_implementation_child = false

catalog identities     = 1817
scatter + bath         = 1771 + 46
capability-map identity row byte-identical
unmatched metas        = 0
empty dispositions     = 0
attachment_candidate   = 33
  faucet 22 + tap 3 + nozzle 2 + cap 3 + fluid_funnel 3
dust_funnel            = realized_natively
wooden_panel           = 23 and counted once
axle+gearbox           = 63 identity_only (live Loader tag; preread 54 was name sampling)
growth_order hashes unchanged
SMELTER_MTE_IDENTITY_READY seal unchanged
no src/ behavior or recipe delta
no capability.json created
```

READY 时 `unique_active_wave` 回到 `null`，`next_unassigned = true`。
不在 topology 写入 implementation child。

关闭时按缺口账本维护契约同步
[unimplemented-gap.md](../../../current/unimplemented-gap.md)：

1. 第 1 节增加流体附件行：冻的是 catalog 33 个身份的**行为**可行性
   `requires_new_runtime`；`allows_*_child = false`。
2. 关闭说明第一句写清：「游戏里仍然没有龙头 / 喷嘴 / 流体漏斗 / 浇铸口；
   邻接模具浇铸仍是现有简化路径。」
3. 不把对照图 identity_only 行从账本删掉，也不把 1,817 写成已实现。
4. 第 2 节 misc 兜底改写为：附件行为已由本 R0 判过，其余 unowned 仍 none。

---

## 9. 明确不包含

- 龙头倒液、喷嘴喷气、漏斗装取、浇铸口浇铸，或任何
  `AttachmentSmall` / tap / funnel 接口的 Java；
- 任何配方、production lock、player path、load 测量、census 扣减；
- `capability.json` 创建或晋级；
- 签发阶段翻转 `unique_active_wave`（实施 `--write` 才翻，关闭再清）；
- storage/lock 28/624 分母改动，或把箱子散落物折回原生物品；
- 机器 / 轴 / 齿轮箱身份重映射到原生存档；
- 注册 catalog 没有的 GT6 MTE 变体；
- Canner / 灌装机（容器 ADR 已排除）；Hopper 流体感知；
- 熔融金属世界凝固与放置；
- 建筑方块 identity、聚变 / 等离子、Bath form/object 链（短队列上的
  别的卡，本卡不抢 unique-active 以外的签发权）；
- 核能 Track C 实现、`nuclear_started = true`；
- 玩家发行、RC soak、GA；
- 本签发阶段创建 `tools/waves/**`、builder、tests、Java/resource
  实现、GameTest receipt、stage archive 或 Git commit。

---

## 10. 撤回

实施中发现阻塞时只有三种合法结果：

1. 在本卡内补齐验收证据；
2. 保持 `BLOCKED`，写清缺的是 catalog、Loader 映射还是可行性证据；
3. 正式撤回并重签，保留旧 denominator hash。

不得用「先做几个龙头再补 ledger」绕过 R0。不得把 1,817 个散落物写成
GT6 MultiTileEntity 完成。不得把邻接模具浇铸或盖板栈写成已经覆盖附件
行为。某一类以后不想要，走 realization 卡的 `out_of_scope`，不在本 R0
把行删掉。

关闭时：本计划移到 `card-plans/closed/`；同步缺口账本第 1 节（§8）；
不重写已 sealed 的 growth-order JSON；不自动创建 Git commit。
