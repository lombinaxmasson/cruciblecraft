# 冻结与未实现账本

> 现行人读索引，不是 production authority。
> 最后核对：2026-09-06（已关闭「配方加载与 EMI 稳定性」；当前 active 计划卡为
> 「生成资源、注册与 Jade 第一切片」；机器可读 `unique_active_wave = null`；
> 变压器 / 能源后续已关；电转换等为 `runtime_ready`；
> `nuclear_started = false`；电池 `player_complete` 已关；
> 转换机目录 169 行 `player_complete`，活 JSON 179；census Batteries 仍 37/37
> `requires_new_runtime`）。
> 读法：机制卡 `*_READY` 只记录当时冻结的分母和可行性，不证明游戏里有这些东西。
> 状态词三套互不替代：`frozen` / `runtime_ready` / `player_complete`。
> 路线图进度只计 `player_complete` 声明与当前 revision 的 fresh profile PASS，见
> [capability-delivery-workflow.md](capability-delivery-workflow.md)。
> 权威仍是下列 JSON；本页过期时以 JSON 为准，改本页对齐 JSON，不要改 sealed 正文。
>
> 本页存在的原因：1.x 已经付过「保守估计」的债（不全量移植声明、TXX 预估、硬顶、
> selected-only 矩阵，见 [1.x 联合退出门](../history/card-plans/closed/1.x联合退出门详细计划.md)）。
> 对照图之后的 R0 把「先冻分母」再盖 `_READY` 又走了一遍。阶段档案看起来像做完，
> 游戏里对应内容一个都没有。以后凡是冻结卡关闭，必须同步改本页；不得只归档 READY。

对照图之后开过的 R0 全是冻结卡。归档文件名和 `_READY` 看起来像做完了。
要找「还没进游戏、以后得另开 runtime / 内容卡」的东西，从本页进，不要从阶段档案倒推。
「搬全、回头再删」只表示名字进账本，不表示 runtime 或配方进了 `src/`。
那次签发不该被读成移植实现。

## 0. 状态词、后续顺序、核电体积

三套状态互不替代。`*_READY` 机制卡只证明 `frozen`。本页第 1 节的冻结项
**不是**整表已实现；部分进游戏的行要在判定里写清余量，不得因为关了一张
表现卡或做完切片就把整行标成完成。

| 状态 | 含义 | 现例 |
| --- | --- | --- |
| `frozen` | 分母、来源、可行性已冻 | 各 `*_R0_READY` |
| `runtime_ready` | `src/main` 机制可运行，内容可尚未导入 | 电转换 10 台；LU 光纤 + 雕刻机；裂变堆芯/棒；聚变控制器 + 18 条 fusion |
| `player_complete` | 生存可获得、可运行、可存档，且有 production lock / census（若需要） | 流体网基础传输；物品网仓储/导入/导出盖板；通用网仓储/导入/导出盖板；物流核心 + Dump；物流监视器；电池 37 储能块（三族芯已回收，锂钴/锂锰/energium 见第 2 节）；变压器 9 台电（已关） |

**后续顺序**（可玩垂直切片；同一时刻一条 delivery lane）。物流 1.2 不在这张表里，
由人手工测，不占 `unique_active_wave`。能力账本主键见
`tools/capabilities/`。

```text
首小时表现与阶段账本（已关）
  -> 原版替换 MVP（已关；纸 3→1；熔炉 deferred）
  -> 紧凑配方传输编解码（已关；compact family 网络 codec）
  -> 紧凑配方作者矩阵（已关；改写法，不是 tag / 拆 Holder）
  -> 工具头前缀折回（已关；tool head 折回材料 × 形态）
  -> Item Network Core（已关；物品两行 `player_complete`）
  -> Fluid / Generic Network（已关；仓储/导入/导出 `player_complete`；Dump 跟 Core 走）
  -> 物流封面网余量（已关；列 Dump / Core / 显示 CPU；不实现）
  -> 物流核心（已关；Dump + 5×5×5 `player_complete`）
  -> Display CPU（已关；四件状态盖板 `player_complete`）
  -> 能量系统余量（已关；删火箱/风箱/独立焦炭）
  -> 能量转换机目录（已关；kind × 材质 169 行 `player_complete`；活目录 179）
  -> Batteries（已关；37 储能块；`energy/batteries` `player_complete`）
  -> 变压器（已关；9 电变压器 `10040–10048`；`energy/transformers` `player_complete`）
  -> 电能转换（已关；10 台 EU→HU / EU→KU；`runtime_ready`）
  -> LU 骨架（已关；光纤可合成；雕刻机配方 blocked）
  -> 核电裂变（已关；堆芯 + 46 棒 runtime；`nuclear_started = false`；配方 blocked）
  -> 聚变 / 等离子（已关；18 条 fusion；等离子空图；控制器配方 blocked）
  -> 电池芯回收（已关；十件芯；3 族空芯 + `B` 槽；LiCo/LiMn/energium 仍 blocked）
  -> Trees
  -> Sensors / Panels
  -> Crops / Squeezer
  -> Food
  -> 维度策略 / Bees / Portals / Center
```

**候选队列与当前计划卡（2026-09-06 判断）**。上面的长链保留已关闭
交付历史和大领域关系；真正选择下一项时按下面的短队列。候选队列第 1、2 项已经
关闭为
[配方加载与 EMI 稳定性](../history/card-plans/closed/配方加载与EMI稳定性详细计划.md)；
它只修了配方加载 / EMI / reload / bake runtime，不创建内容 family。
候选队列第 3、4 项现已合并为当前 active 计划卡
[生成资源、注册与 Jade 第一切片](../history/card-plans/active/生成资源注册与Jade第一切片详细计划.md)；
机器可读 `unique_active_wave` 仍为 `null`。下面保留剩余候选
的原序号；每项启动时仍须重新定范围、owned paths 和验收门，不合成大卡。

1. **已关闭：配方数据正确性与 EMI 重复注册**：RecipeManager 解析错误归零（fluidbed
   无效行改为 source-backed `blocked`，不再伪造 chance / stand-in），校验
   holder / stable ID 和输入输出数组，并移除重复的 processing category / recipe
   注册。不能用跨材料 alternatives 压数量。
2. **已关闭：reload / bake 稳定性**：用 RecipeManager identity + 单调 generation
   token 去重同一数据状态；拆分测量 parse、family materialization、validation、
   index 和 EMI projection。只在证明关系语义等价后聚合；否则保留全量
   exact 索引，或另立懒查询 viewer 工作。生产 reload 10 s 硬顶未降低；allocation
   标 `PENDING_MEASUREMENT`。
3. **当前 active 卡：生成资源、注册身份与贴图门禁**：修 generated tree currentness、block/item model、
   全 catalog registry collision 检查和 live art manifest。这里只修基础设施，不顺手
   清理全部历史 `gt_object` / `gt_mte`。
4. **当前 active 卡同卡范围：既有机器 Jade 第一切片**：先统一已有数据合同，补坩埚的 K、HU、熔毁点、填充度、
   渲染/内容/缓存槽，以及变压器升降压模式和各面高低压。它不等待核能，也不扩成全机器
   GUI 卡。
5. **裂变基础生存闭环**：正式开 realization lane 时才把 `nuclear_started` 改为 true；
   冻结本地 GT6 来源，做真实 Canner host/语义、1×1/2×2 堆芯和 46 棒的精确配方，
   闭合蒸馏水→蒸汽的基础玩家路径。缺失零件只允许纳入本切片做成真实对象，或继续
   `blocked`，不得用其它材料、形态或单个电路顶替。
6. **裂变热流体与热量合同**：注册热锡、热钠、热重水、热氦等独立身份，钉死冷却剂
   转换、温度/HU、熔毁阈值和存档语义；不再用同流体 1:1 倒罐冒充热态。
7. **裂变观测、安全与后处理**：补 Reactor Jade/温度计/Geiger 读数、辐射和烫伤及
   防护语义，再接乏棒/产物棒 7 条离心。是否做专用 GUI 在数据合同稳定后单独决定；
   GT6 工具/管道交互足够时不强制为了“完整”造 GUI。
8. **热力机器逐台闭环**：优先热交换器，再按来源依赖分别做蒸汽涡轮和冷却器；每台
   独立验收输入流体、HU/KU/RU 能量语义、结构、转子/零件、配方和玩家用途，不绑定成
   一张核电大卡。
9. **GT6_w 建筑方块 / Multiitem 有界批次**：按 live catalog 家族逐批做命名、已有
   identity 复用/冲突、精确配方和来源贴图。Extruder 已有 compact 管线只做证据指出的
   缺口，不借机全量重构；核能真实配料需要的身份可在第 5–8 项中由对应切片认领。
10. **聚变 / 等离子**：等裂变与热力合同稳定后再开；控制器生存配方、等离子独立流体
    和燃料/输出链都必须有来源，不因已有 18 条 fusion runtime 就提前宣称完成。

若前项测量证明没有阻塞，后项可以重新排序；第 1–2 项已关，第 3–4 项由当前
active 卡处理，第 5 项仍是下一条核裂变内容候选，聚变仍位于裂变和热力合同之后。

`fuels_fluidbed` 现只发 6 条可加载配方。其余 49 条记在
`tools/energy_converter_fluidbed_blocked.json`（11 条 GT6 item 未映射，38 条
fail-closed，含未注册 `dust_div72` / 灰形态，以及 GT6 `needsOutputs: false`
的无输出燃料）。这不是新 recipe family，不得用 stand-in 顶替；要真实形态或
另立 outputless 燃料模型。

当前人读 active 计划卡为
[生成资源、注册与 Jade 第一切片](../history/card-plans/active/生成资源注册与Jade第一切片详细计划.md)。
机器可读 `unique_active_wave` 仍为 `null`。已关闭
[配方加载与 EMI 稳定性](../history/card-plans/closed/配方加载与EMI稳定性详细计划.md)
（slug `runtime/recipe-load-emi-stability`；`owns_families = 0`）。
机器可读 `unique_active_wave` 仍为 `null`。已关闭
[变压器](../history/card-plans/closed/变压器详细计划.md)
（capability `energy/transformers`，`player_complete` / `accepted`）。
已关闭
[能源后续卡收口](../history/card-plans/closed/能源后续卡收口详细计划.md)
（电转换 / LU / 裂变 / 聚变 / 电池芯；`runtime_ready`）。已关闭
[电池](../history/card-plans/closed/电池详细计划.md)
（capability `energy/batteries`，`player_complete` / `accepted`）。已关闭
[能量转换机目录](../history/card-plans/closed/能量转换机目录详细计划.md)
（capability `energy/converter-catalog`）。已关闭
[能量系统余量](../history/card-plans/closed/能量系统余量详细计划.md)：
固体燃料燃烧室已删除（余量卡）；转换机目录把 GT6 Burning Box / 锅炉 / 蒸汽机 /
燃油引擎 / 发电机 / 电机做成 kind × 材质数据包分档（`converter_catalog_policy`）。邻接 HU 用
`bronze_burning_box_gas`。`battery_policy` 钉死 37 个储能块，已由电池卡
`player_complete`；census 冻结表仍 37/37 `requires_new_runtime`。`transformer_policy`
钉死电变压器从未 R0，齿轮箱不得冒充；电变压器 `10040–10048` 已
`player_complete`，长距 `10064–10068` 仍不在该卡。
已关闭
[显示 CPU](../history/card-plans/closed/显示CPU详细计划.md)
为四件状态盖板 `player_complete`。
机器可读 `unique_active_wave = null`；`next_unassigned = true`。现行已声明
`player_complete` 的物流能力是
`logistics/fluid-network/basic-transfer`、
`logistics/item-network-core`、
`logistics/generic-network/core`、
`logistics/logistics-core`、
`logistics/display-cpu` 与
`energy/batteries` 与
`energy/transformers`。物品网络核心机制卡仍是
[物品网络核心](../history/card-plans/closed/物品网络核心详细计划.md)
（slug `runtime/item-network-core`）`ITEM_NETWORK_CORE_READY`；
玩家完成晋级见
[物品网络核心玩家完成晋级](../history/card-plans/closed/物品网络核心玩家完成晋级详细计划.md)。
物品两行封面网 `player_complete`；流体基础传输 `player_complete`。
Generic 仓储/导入/导出见
[通用网络核心](../history/card-plans/closed/通用网络核心详细计划.md)
（`player_complete`）。相邻封面底表 `cover_definitions.json` 仍 9 条、
`registerBuiltin` 仍 8；物流封面走 sidecar，不改这份底表。Dump 与
Logistics Core 已由 [`logistics/logistics-core`](../history/card-plans/closed/物流核心详细计划.md)
`player_complete`。`dump_policy` 见
[物流封面网余量](../history/card-plans/closed/物流封面网余量详细计划.md)：
Dump 盖板是标记（无优先级）；搬运在 Core tick 的最后一档，从 Generic
物品仓储搬进 Dump 邻接容器，不是虚空，也不是现有管网的第四件盖板。
已关闭的
[工具头前缀折回](../history/card-plans/closed/工具头前缀折回详细计划.md)
（slug `registry/tool-head-prefix`）为 `TOOL_HEAD_PREFIX_READY`：
`owns_families = 0` 的注册权威 + emit，mapped tool head 折回
`材料 × 形态`；bath identity `71`，semantic `244`。已关闭的
[紧凑配方作者矩阵](../history/card-plans/closed/紧凑配方作者矩阵详细计划.md)
（slug `runtime/compact-recipe-authored-matrix`）为
`COMPACT_RECIPE_AUTHORED_MATRIX_READY`。已关闭的
[紧凑配方传输编解码](../history/card-plans/closed/紧凑配方传输编解码详细计划.md)
为 `COMPACT_RECIPE_WIRE_CODEC_READY`。Dump / Logistics Core 已关。已关闭
[显示 CPU](../history/card-plans/closed/显示CPU详细计划.md)
为四件状态盖板 `player_complete`。当前 active 计划卡为
[生成资源、注册与 Jade 第一切片](../history/card-plans/active/生成资源注册与Jade第一切片详细计划.md)。
机器可读 `unique_active_wave` 仍为 `null`。已关闭
[变压器](../history/card-plans/closed/变压器详细计划.md)。已关闭
[能源后续卡收口](../history/card-plans/closed/能源后续卡收口详细计划.md)。

**核电体积**（纠正「大后期」读法）。`nuclear_started` 仍为 false，Track C
不因此自动开工。裂变堆芯 / 邻棒 / 聚变双能宿主已在能源卡落地；堆芯与聚变
控制器配方、棒 Canner、等离子流体仍 blocked。

| 项 | 分母 | 读法 |
| --- | --- | --- |
| Reactors（排除表） | 46 | 八类棒已注册；棒配方仍 blocked |
| 堆芯 | 1×1 + 2×2 | 已注册；配方仍缺 `OD_CIRCUITS[5]` / 活塞档 |
| leftover 离心 | 7 family | 废棒 / 产物棒回收；host 已有 |
| 聚变堆 | 1 controller | 控制器 + 钨钢墙 / 不锈钢墙 / 铱线圈已注册；19×19 八边形零件计数对齐 tooltip；不在 7×7 GameTest 里搭整机；控制器配方仍 blocked |
| `gt.recipe.fusionreactor` | 18 | 已发布到 `cruciblecraft:fusion`；`ST.tag(1)/(2)` → `programmed_circuit` + `CIRCUIT_CONFIG` PRESERVE |
| 无中子聚变电池 | 2 | T35 `14600` / `14601` |
| `gt.recipe.fuels.plasma` | 0 | `fuels_plasma` 空图；不发明等离子流体 |
| massfab / replicator | 920 / 895 | Track C 邻居，**不是**聚变配方 |

裂变配方仍 gated：`nuclear_started = false`，不发堆芯/棒合成。
聚变不发明等离子燃料；化学态仍只有 LIQUID/GAS。

`smelter` 曾错绑 GT6 `MultiTileEntitySmeltery` 小锅（与 T34 坩埚同一套几何）。
[首小时表现与阶段账本](../history/card-plans/closed/首小时表现与阶段账本详细计划.md)
已把它改到 port `basicmachines/smelter` 立方机（`machine_cube_2_layer`）。
坩埚保持 T34 小锅。工作态 `overlay_active` 未接（`ProcessingMachineBlock` 没有
`LIT`）。研钵 / 筛分 / 洗矿仍是 voxel，贴图 SOURCE_BACKED。

## 权威文件

| 用途 | 路径 |
| --- | --- |
| 113 行机制对照（37 行仍 `cc_mechanism=none`） | [`tools/waves/portfolio/source-capability-inventory/capability_map.json`](../../tools/waves/portfolio/source-capability-inventory/capability_map.json) |
| growth-order 五轨（sealed，不重写） | [`tools/waves/portfolio/source-capability-growth-order/growth_order.json`](../../tools/waves/portfolio/source-capability-growth-order/growth_order.json) |
| leftover 39 条 family | [`tools/waves/portfolio/source-capability-map-r0/leftover_later.json`](../../tools/waves/portfolio/source-capability-map-r0/leftover_later.json) |
| 各 R0 可行性 | 各波目录下 `feasibility.json`（下表有路径） |
| census 排除表 | [`tools/census_excluded_object_reclaim.json`](../../tools/census_excluded_object_reclaim.json) |
| 非矿 dump 特征 | `gt6_dump/gt6_recipe_dump/worldgen/other_features.json` |

`python tools/build_<slug>.py --check` 只检查那张 legacy 冻结 artifact，
不证明对应内容已实现，也不参与 active profile。

补全路径：另开 **runtime / 内容卡**（计划、缺失 runtime、Source Pack、
production lock、player path、load、census、fresh capability profile）。不得从已关 R0 的
topology 预分配 implementation child。不想要的类在 realization 卡标
`out_of_scope`，保留 R0 历史原字节。

---

## 1. 已冻结：判定要新 runtime（整表还没进游戏）

这些行已经 R0 关过。`allows_*_child = false`。以后实现是新卡，不是把 R0 再写一遍 Java。
**部分实现仍留在本节**：已经进游戏的写在判定里，余量没做完就不要整行删掉。

| 人类名 | slug / `--check` | 冻了什么 | 判定 | 可行性文件 |
| --- | --- | --- | --- | --- |
| 物流封面网 | `portfolio/logistics-cover-net-r0` | 7 个 kind：`logistics_item_storage` / `transfer`、`logistics_fluid_storage` / `transfer`、`logistics_generic_storage` / `transfer` / `dump` | **部分实现**。仓储/导入/导出盖板已 `player_complete`：[`logistics/item-network-core`](capability-delivery-workflow.md)、[`logistics/fluid-network/basic-transfer`](capability-delivery-workflow.md)、[`logistics/generic-network/core`](../history/card-plans/closed/通用网络核心详细计划.md)。Dump 与 `logistics_core` 已由 [`logistics/logistics-core`](../history/card-plans/closed/物流核心详细计划.md) `player_complete`。`dump_policy` 见 [物流封面网余量](../history/card-plans/closed/物流封面网余量详细计划.md)：Dump 是 Core 最后一档物品溢出，不是 Generic 管网盖板。不得把 Display CPU 算进这七 kind，也不得把本行从本节删掉。Display CPU 已由 [显示 CPU](../history/card-plans/closed/显示CPU详细计划.md) `player_complete`。当前 unique active 为空。 | [`feasibility.json`](../../tools/waves/portfolio/logistics-cover-net-r0/feasibility.json) |
| Panels | `portfolio/exclusion-reclaim-r0` | 6 sites / 348 expanded | `requires_new_runtime` | [`feasibility.json`](../../tools/waves/portfolio/exclusion-reclaim-r0/feasibility.json) |
| Sensors | 同上 | 21 / 21 | `requires_new_runtime` | 同上 |
| Portals | 同上 | 19 / 19 | `requires_new_runtime` | 同上 |
| Batteries | 同上 | 37 / 37 | `requires_new_runtime`。已关闭 [`energy/batteries`](../history/card-plans/closed/电池详细计划.md) 已进游戏并 `player_complete`；本行冻结表不得删。十件芯已注册；铅酸 / 碱性 / 镍镉空芯与 `B` 槽已回收。锂钴 / 锂锰空芯与 energium shaped 仍 blocked。 | 同上 |
| Reactors | 同上 | 46 / 46 | `defer_to_portfolio` → `portfolio/nuclear`；`nuclear_started = false`。**部分实现**：堆芯与 46 棒已注册（`runtime_ready`），配方仍 blocked。见 [能源后续卡收口](../history/card-plans/closed/能源后续卡收口详细计划.md)。 | 同上 |
| 非矿树 | `portfolio/non-ore-worldgen-r0` | `WorldgenTree*` 9 | `requires_new_runtime` | [`feasibility.json`](../../tools/waves/portfolio/non-ore-worldgen-r0/feasibility.json) |
| 非矿地牢 | 同上 | `WorldgenDungeonGT` 1 | `requires_new_runtime` | 同上 |
| 非矿行星岩 | 同上 | moon / mars / planet rocks 3 | `requires_new_runtime` | 同上 |
| 非矿 Center | 同上 | biomes / streets / nexus / beacon / testing 5 | `requires_new_runtime` | 同上 |
| Vanilla loader | `portfolio/vanilla-replace-r0` | `Loader_Recipes_Vanilla` | `requires_new_runtime`；**部分由** [`content/vanilla-replace-mvp`](../history/card-plans/closed/原版替换MVP详细计划.md) **实现**（`minecraft:paper` 3 甘蔗 → 1 纸）。熔炉 / 骨头 / Vanilla.java 后半仍 `frozen`。**不是** `player_complete` | [`feasibility.json`](../../tools/waves/portfolio/vanilla-replace-r0/feasibility.json)；[`vanilla_replace_lock.json`](../../tools/waves/content/vanilla-replace-mvp/vanilla_replace_lock.json) |
| Replace / ASM | 同上 | `Loader_Recipes_Replace` + `Replacements` | `requires_new_runtime`；本卡未实现扫描器 / ASM，余量仍 `frozen`。**不是** `player_complete` | 同上 |
| Crops | `portfolio/crops-food-bees-r0` | `plant.glowtus` / `plant.bush` + dump `gt.recipe.squeezer` 5322（规模，非 census） | `requires_new_runtime` | [`feasibility.json`](../../tools/waves/portfolio/crops-food-bees-r0/feasibility.json) |
| Food | 同上 | dump juicer 96 + fermenter 6435（规模，非 census） | `requires_new_runtime` | 同上 |
| Bees | 同上 | `WorldgenHives` 10 + bumblequeen 80 + bumblelyzer 1440（规模，非 census） | `requires_new_runtime` | 同上 |

导入器 `GENERIC_RECIPE_IMPORT_READY` 只表示**已有 host** 时可以 `import-source`。
它不创建 squeezer / juicer / fermenter / bumble RecipeMap，也不实现封面网或作物生长。

物流 R0 另外把四个 `logistics_display_cpu_*` 标成该切片
`out_of_scope`（见该波 `inherited_denominator.json`）。对照图 seed 仍有
「Display CPU and remaining unowned GT6 domains」一行，没有单独 R0。

---

## 2. 对照图点了名、连可行性都没冻

这些仍是 `cc_mechanism = none`（或 count-ceiling 的 live 常量行）。不是自动下一张卡。

| 项 | 在哪 | 现状 |
| --- | --- | --- |
| 核能 Track C | growth-order 第五轨 `portfolio/nuclear`；leftover 7 条 centrifuge；排除表 Reactors 已 defer 到这里 | `nuclear_started = false`。堆芯/棒 runtime 已落地，配方仍 blocked。未指定 `next_major` 前不自动开 Track C |
| 变压器 | capability map `GT6 transformers` | 电 `10040–10048` 已 [`energy/transformers`](../history/card-plans/closed/变压器详细计划.md) `player_complete`。长距 `10064–10068` 与齿轮箱不在该卡 |
| 电池芯 / 灌液格 | GT6 `IL.Battery_*_Cell_Empty/Filled`，`MultiItemTechnological` `20000–20009` | 十件芯已注册；铅酸 / 碱性 / 镍镉空芯与 `B` 槽已回收。锂钴 / 锂锰缺 `OD_CIRCUITS[4]`/`[6]`，Canner 灌液表仍缺。能力键 `battery_part:filled_cell`。LU 晶体无 GT6 合成表（`recipe:energium_crystal_shaped`）。不并进变压器卡 |
| 建筑方块 identity | `GT6 building-block item/block identities` | 从未 R0 |
| 建筑方块 behavior | hardness / multiblock parts / decorative machines | 从未 R0 |
| Display CPU / 其余未认领域 | seed「misc systems…」 | 四件 Display CPU 状态盖板已 [显示 CPU](../history/card-plans/closed/显示CPU详细计划.md) `player_complete`。misc 其余仍 none。R0 `display_cpu_out_of_scope` 不改 |
| 封面余量 `controller_*` | capability map | 物流 R0 没吃 |
| 封面余量 `detector_*` | 同上 | 物流 R0 没吃 |
| 封面余量 redstone | `controller_redstone` / `controller_auto_redstone` | 物流 R0 没吃 |
| count-ceiling / kind 信封 | growth-order 第二轨；capability map 四条 21000 / 56000 / 4096 / 128 | telemetry / report-only。新 RecipeMap 前要单独面对，不是内容待办清单上的一行配方 |

37 行 `none` **不是** 37 张新卡。第 1 节已经判过的不要再开一张「冻结 R0」。

---

## 3. leftover 39 条 family（不是 39 个新机制）

权威：`leftover_later.json`。`hanging_unaccounted = []`。

| 桶 | 条数 | 读法 |
| --- | --- | --- |
| `later:assembler_combinatorial` + `later:electrolyzer_combinatorial` | 4 | 导入器能力已有，`content_owner = post_generator/combinatorial-family-intake`，`started = false`。要进游戏仍要内容卡 + production lock |
| `post_1x:nuclear` | 7 | 跟核能轨走，不跟 1.x census 走 |
| 28 条 post-1.x recycling / autoclave / smelter edge / pahoehoe 等 | 28 | 1.x 已独立 scope。不是漏做的 ordinary 尾账 |

---

## 4. 余量数字不是待办清单

不要把这些整数抄成「还要做 N 个」：

| 数字 | 来源 | 为什么不是 todo |
| --- | --- | --- |
| T35 切走五类后 634 sites / 1,230 expanded | `t13c-exclusion-reclaim-r0` `remainder_after_slice` | 排除表余量，混着 1.x 已经做过的 Storage / 漏斗等。要当待办必须先对照现行机器树，不能整表开工 |
| `other_features.json` 190；非矿切 18 后余 172；再切 hive+plant 12 后余 160 | 两张 worldgen/crops R0 | 余量含 `WorldgenStone` 90、`WorldgenOresVanilla` 24、流体泉 16、`WorldgenRocks` 4 等。矿脉 / 油井 / 地表石子已有 T20/T33 子集。不能当 160 个新 feature |
| dump recipeCount 合计 13,373（作物卡） | squeezer+juicer+fermenter+bumble* | 规模上下文。`owns_families` 仍为 0。`gt.recipe.plantalyzer` recipeCount=0 不是分母 |
| capability map 37 行 `none` | inventory | 多数已在第 1 节判过 |

已有 1.x 子集、不要当成缺口重开：世界生成矿脉 129、流体矿 2、地表石子 scatter；item scatter（含 `block/object` 与 `bath/identity`）；`storage/lock` 28/624；9 张 adjacent cover。

---

## 5. 本页怎么更新

- 新关一张冻结 R0：把类别从第 2 节搬到第 1 节，链到新的 `feasibility.json`。关闭说明第一句必须写清「游戏里仍然没有 X」，不得只写 `_READY`。
- 真做进游戏：从第 1 节删掉或改成「已由 `<slug>` 实现」，并指向那张内容卡的 production lock / census。
- 正式不要：在 realization 卡写 `out_of_scope`，本页改成不要，不要假装 R0 没点过名。
- 不要为了「好看」重写 sealed `growth_order.json` 或已关 R0 artifact。
- 不要再把冻结卡当成默认下一张工作。默认下一张若用户要的是进游戏的东西，开 runtime / 内容卡，或先问清楚，不要再盖一张 READY 冻结收据。
- 已关闭：[物品网络核心](../history/card-plans/closed/物品网络核心详细计划.md)
  （`ITEM_NETWORK_CORE_READY`）与
  [物品网络核心玩家完成晋级](../history/card-plans/closed/物品网络核心玩家完成晋级详细计划.md)
  （`logistics/item-network-core` `player_complete`）。物品两行封面网
  `player_complete`。相邻封面底表 `cover_definitions.json` 仍 9 条、
  `registerBuiltin` 仍 8；物流封面走 sidecar。流体基础传输已
  `player_complete`。Generic 仓储/导入/导出已
  [通用网络核心](../history/card-plans/closed/通用网络核心详细计划.md)
  `player_complete`。Dump 与 `MultiTileEntityLogisticsCore` 已由
  [物流核心](../history/card-plans/closed/物流核心详细计划.md)
  `player_complete`。已关闭
  [显示 CPU](../history/card-plans/closed/显示CPU详细计划.md)
  为四件状态盖板 `player_complete`。当前 active 计划卡为
  [生成资源、注册与 Jade 第一切片](../history/card-plans/active/生成资源注册与Jade第一切片详细计划.md)。
  机器可读 `unique_active_wave` 仍为 `null`。已关闭
  [变压器](../history/card-plans/closed/变压器详细计划.md)。不得把 Display CPU 算进这七
  kind。
  关卡证明债（unload / reload / load 门偏软、census 8 对 live 10）写在
  [known-issues.md](known-issues.md)，不要从 READY 倒推
  「已经测过真卸 chunk」。物流盖板图标已按 [gt6-art-policy.md](gt6-art-policy.md)
  从 `gregtech6_w` 迁入，不再借记 conveyor / filter。
- 已关闭：[首小时表现与阶段账本](../history/card-plans/closed/首小时表现与阶段账本详细计划.md)
  （`FIRST_HOUR_PRESENTATION_READY`）。四台首小时 host 脱离 `metal_surface`；
  本页补了三态、后续顺序与核电体积。不把任何第 1 节冻结项标成已实现。
- 已关闭：[原版替换 MVP](../history/card-plans/closed/原版替换MVP详细计划.md)
  （`VANILLA_REPLACE_MVP_READY`）。纸 3→1；熔炉因无铁前 firestarter 未删；
  第 1 节 Vanilla / Replace 为部分实现，余量仍 `frozen`。不得写成两类
  loader `player_complete`。
