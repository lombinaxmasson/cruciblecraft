# Blocker 总账

> 本页由 `python tools/build_blockers.py --write` 从
> `tools/blockers/catalog.json` 与 `tools/blockers/batches.json` 生成，不要手改。
> 条目权威是 catalog，批处理关系权威是 batches；本页和 `tools/blockers/ledger.json` 都是投影。
> 不同条目、不同 `unit` **不得相加**。发现旧缺口不是任务制造了缺口。
> `count` 不是剩余工作量。排期看 `planning_bucket`，不要按数字选最大的卡。
> 选批只读 current recipe ledger / catalog / batches；一致性失败时先停排期，历史 candidate selection 不能重新打开已关闭条目。

## 统计

- 条目 60：open 38，partial 2，resolved 10，superseded 5，out_of_scope 5
- 未关闭排期桶：数字是规模，不是待办 5，分母已冻，可当卡排 18，有名字，分母未冻成工作量 14，不是活 3

## 排期分类（未关闭）

A 的整数是 dump/shadow/未核实规模。B 才是可以抽 unique-active 的冻结核。
C 先审计分母。D 不是任务。同类条目仍不得相加。

### A. 数字是规模，不是待办（5）

- `recipe/injector-overflow`：535 rows
- `recipe/loom-overflow`：869 rows
- `recipe/melter-overflow`：2796 rows
- `recipe/nanofab-overflow`：57 rows
- `worldgen/food`：n/a

### B. 分母已冻，可当卡排（18）

- `cover/torch-repeater-wire-host`：2 covers
- `fluid/ic2-coolant`：1 fluids
- `fluid/resin-rubber`：1 fluids
- `fluid/sap-maple`：1 fluids
- `fluid/thorium-salt`：1 fluids
- `identity/converter-turbines-battery-boxes`：8 items
- `identity/electric-unregistered-gauges`：61 loader_ids
- `identity/processing-ungated-families`：3 families
- `material-form/copper-family-curved-plate`：n/a
- `obtain/injector-mv-hv-iv-hosts`：3 hosts
- `obtain/nanofab-hosts`：5 hosts
- `obtain/redstone-wiregt01`：3 items
- `recipe/fluidbed-overflow`：49 rows
- `recipe/laminator-overflow`：60 rows
- `recipe/oven-cooking-oil-xp`：2 fluids
- `recipe/pressure-washer-stone`：120 rows
- `recipe/printer-dye-fluids`：22 rows
- `recipe/roll-former-rail-gt`：2 rows

### C. 有名字，分母未冻成工作量（14）

- `architecture/building-block-identity`：n/a
- `cover/redstone-wire-covers`：n/a
- `energy/cooler`：n/a
- `energy/reactor-backpack-radioactivity`：n/a
- `energy/reactor-world-explode`：1 behaviors
- `identity/connector-art-keep-distinct-dummies`：n/a
- `identity/eu-blocked-gauges`：n/a
- `identity/hsla-ungated-gauges`：n/a
- `storage/mass-storage-prefix-units`：n/a
- `tools/world-behaviors`：n/a
- `worldgen/bees`：n/a
- `worldgen/center`：n/a
- `worldgen/dungeon-room-contents`：n/a
- `worldgen/planet-rocks`：3 families

### D. 不是活（3）

- `architecture/combinatorial-leftover`：4 families
- `energy/reactor-temperature-kelvin`：n/a
- `peripheral/sensors-computercraft`：1 integrations

## 批处理前已关闭的条目

这些条目保留在 catalog 作为历史结算，但不进入未关闭批次；candidate selection 的旧 blocked 数不能覆盖后继卡的 current closeout。

- 成员：`recipe/bath-remainder-families`, `recipe/bath-identity-families`
  - 收口：T48 完成 145 个 Bath remainder family；T49 完成最后 5 个 tiny-purified family。当前 Bath ordinary remainder 为 0。
  - 当前权威：`docs/history/card-plans/closed/T48详细计划.md`, `docs/history/card-plans/closed/T49详细计划.md`, `tools/waves/bath/tiny-purified/readiness.json`, `tools/waves/bath/tiny-purified/card_topology.json`
- 成员：`obtain/mte-inplace-runtime`
  - 收口：content/gt6-mte-inplace-acquisition 落地 732 条 source-exact shaped_catalyst 格；150 个缺形态/OD/plank 缺口留在各族 current_gap.json。14 个 runtime 仍 runtime_ready。
  - 当前权威：`tools/capabilities/content/gt6-mte-inplace-acquisition/capability.json`, `tools/waves/prep/gt6-mte-inplace-acquisition/contract.json`, `tools/waves/content/gt6-mte-inplace-acquisition/gametest_receipt.json`

## 批处理关系（不是分母）

以下只登记共享审计、Source Pack 或验收流程；不同 `unit`、不同 production lock 仍分别核算，批次不会自动晋级 `player_complete`。

### `batch/identity-eu-gauge-closure`：EU / 线规身份收口

- 建议排序依据：61 个 loader_ids 是最大的已冻结 identity 数；EU partial 与 HSLA 线仍需分子任务审计。
- 类型：`shared_identity_audit`；成员角色按各 blocker 保留
- production lock：`separate_subtask_validation`
- 成员：
  - `identity/electric-unregistered-gauges`（`primary`）
  - `identity/eu-blocked-gauges`（`audit_only`）
  - `identity/hsla-ungated-gauges`（`audit_only`）
- 已有 lock：
  - `tools/waves/content/gt6-eu-wire-cable-runtime/production_lock.json`
  - `tools/waves/content/gt6-eu-missing-wire-gauges-runtime/production_lock.json`
  - `tools/waves/content/gt6-connector-alias-repair/production_lock.json`
- 明确排除：
  - `identity/connector-art-keep-distinct-dummies`
- 边界：
  - 已有真实对象必须 exact mapping；不能把 61 个 loader_ids 当成 61 个必然可解条目。
  - 石墨烯与超导缺形态继续 blocked。
  - HSLA alias / gauge 是独立子任务。
  - 连接件美术 dummy 不是 runtime identity 工作。

### `batch/machines-nanofab-host-closure`：纳米加工机主机获得格收口

- 建议排序依据：5 个已冻结 host 是有界的机器获得格目标；57 条 overflow 只保留为规模上下文。
- 类型：`machine_closure`；成员角色按各 blocker 保留
- production lock：`host_lock_then_overflow_recompute`
- 成员：
  - `obtain/nanofab-hosts`（`primary`）
  - `recipe/nanofab-overflow`（`scale_context`）
- 已有 lock：
  - `tools/waves/machines/nanofab/production_lock.json`
- 边界：
  - 5 个 host 是实现目标。
  - 57 条 overflow 是重算上下文，不承诺顺便清掉全部行。
  - 不得用 programmed_circuit 或其它零件替代缺失的激光气体 / 蓝宝石形态。

### `batch/machines-injector-host-closure`：注射机主机获得格收口

- 建议排序依据：3 个已冻结 host 是有界的机器获得格目标；535 条 overflow 只保留为规模上下文。
- 类型：`machine_closure`；成员角色按各 blocker 保留
- production lock：`host_lock_then_overflow_recompute`
- 成员：
  - `obtain/injector-mv-hv-iv-hosts`（`primary`）
  - `recipe/injector-overflow`（`scale_context`）
- 已有 lock：
  - `tools/waves/machines/injector/production_lock.json`
- 边界：
  - 3 个 MV/HV/IV host 是实现目标。
  - 535 条 overflow 是重算上下文，不承诺顺便清掉全部行。
  - source-exact LV 与 Chromium EV host 不授权为其余 host 使用 stand-in。

### `batch/fluid-fission-identity`：裂变流体身份收口

- 建议排序依据：2 个相关裂变流体 identity 共享热流体与注射机影响面。
- 类型：`domain_fluid_closure`；成员角色按各 blocker 保留
- production lock：`separate_fluid_identity_validation`
- 成员：
  - `fluid/ic2-coolant`（`primary`）
  - `fluid/thorium-salt`（`primary`）
- 已有 lock：无（按能力/流体身份分别验收）
- 明确排除：
  - `recipe/printer-dye-fluids`
  - `recipe/oven-cooking-oil-xp`
- 边界：
  - 两个 identity 都影响 energy/nuclear-fission-hot-fluids 与注射机 source review。
  - 不能因为 root cause class 都是 missing_fluid，就并入印刷机染料或熔炉油 / XP 流体。
  - 解决这些 identity 不会重开或自动重关已经完成的 hot-fluid capability。

### `batch/fluid-gt-tree-identity`：GT 树流体身份收口

- 建议排序依据：2 个缺失流体共享 GT 树 worldgen owner 与验收面。
- 类型：`domain_fluid_closure`；成员角色按各 blocker 保留
- production lock：`separate_fluid_identity_validation`
- 成员：
  - `fluid/resin-rubber`（`primary`）
  - `fluid/sap-maple`（`primary`）
- 已有 lock：无（按能力/流体身份分别验收）
- 明确排除：
  - `recipe/printer-dye-fluids`
  - `recipe/oven-cooking-oil-xp`
- 边界：
  - 两个 identity 都属于 worldgen/gt-trees owner 及其 freeze / drill source path。
  - Latex 不是 Resin_Rubber 的 stand-in，另一种树液也不是 Sap_Maple 的 stand-in。
  - 印刷机染料与熔炉油 / XP 保持在 GT 树批次之外。

### `batch/cover-redstone-host-audit`：红石盖板宿主审计

- 建议排序依据：2 个 cover host 共享红石宿主审计；无数量的 policy 轨道保持独立。
- 类型：`shared_cover_host_audit`；成员角色按各 blocker 保留
- production lock：`separate_host_policy_validation`
- 成员：
  - `cover/torch-repeater-wire-host`（`primary`）
  - `cover/redstone-wire-covers`（`audit_only`）
- 已有 lock：无（按能力/流体身份分别验收）
- 明确排除：
  - `identity/redstone-not-eu-or-pipe`
- 边界：
  - 普通 redstone-wire 与 EU-cable cover policy 要与两个 torch/repeater host 分开审计。
  - redstone-not-eu-or-pipe 是 out_of_scope 不变量，不是 missing host 任务。
  - 不得从已落地的 insulated-redstone identity extras 推导 cover hosting。

## 按根因（未关闭）

- `invariant`（1）
  - `energy/reactor-temperature-kelvin`
- `missing_cover_host`（2）
  - `cover/redstone-wire-covers`
  - `cover/torch-repeater-wire-host`
- `missing_fluid`（6）
  - `fluid/ic2-coolant`
  - `fluid/resin-rubber`
  - `fluid/sap-maple`
  - `fluid/thorium-salt`
  - `recipe/oven-cooking-oil-xp`
  - `recipe/printer-dye-fluids`
- `missing_form`（4）
  - `energy/reactor-backpack-radioactivity`
  - `material-form/copper-family-curved-plate`
  - `obtain/injector-mv-hv-iv-hosts`
  - `obtain/nanofab-hosts`
- `missing_mod_bridge`（1）
  - `peripheral/sensors-computercraft`
- `missing_obtain`（1）
  - `obtain/redstone-wiregt01`
- `missing_runtime`（6）
  - `energy/cooler`
  - `energy/reactor-world-explode`
  - `storage/mass-storage-prefix-units`
  - `tools/world-behaviors`
  - `worldgen/bees`
  - `worldgen/food`
- `missing_worldgen`（3）
  - `worldgen/center`
  - `worldgen/dungeon-room-contents`
  - `worldgen/planet-rocks`
- `unclaimed_domain`（2）
  - `architecture/building-block-identity`
  - `architecture/combinatorial-leftover`
- `unmapped_identity`（6）
  - `identity/connector-art-keep-distinct-dummies`
  - `identity/converter-turbines-battery-boxes`
  - `identity/electric-unregistered-gauges`
  - `identity/eu-blocked-gauges`
  - `identity/hsla-ungated-gauges`
  - `identity/processing-ungated-families`
- `unmapped_operand`（8）
  - `recipe/fluidbed-overflow`
  - `recipe/injector-overflow`
  - `recipe/laminator-overflow`
  - `recipe/loom-overflow`
  - `recipe/melter-overflow`
  - `recipe/nanofab-overflow`
  - `recipe/pressure-washer-stone`
  - `recipe/roll-former-rail-gt`

## 条目

### `architecture/building-block-identity`

- 标题：建筑方块 identity/behavior 无 owner
- 状态：`open`
- 根因：`unclaimed_domain` / `no_current_owner`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`none`
- 发现卡：`logistics/cover-remainder`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：—
- 权威：`docs/current/unimplemented-gap.md`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Cannot derive workload from cc_mechanism = none.

### `architecture/combinatorial-leftover`

- 标题：组合导入 leftover 4 条仍未开工
- 状态：`open`
- 根因：`unclaimed_domain` / `post_generator_combinatorial_family_intake_not_started`
- 数量：4 families
- 排期：`not_work`
- 挡住：`none`
- 发现卡：`recipe/blocked-chain-ledger`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：—
- 权威：`tools/waves/portfolio/source-capability-map-r0/leftover_later.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：later:assembler_combinatorial + later:electrolyzer_combinatorial. started = false.

### `cover/redstone-wire-covers`

- 标题：红石线 MTE 盖板未接入
- 状态：`open`
- 根因：`missing_cover_host` / `connector_covers_not_hosted`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`content/mte-redstone-wire`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/mte-redstone-wire`, `logistics/cover-remainder`
- 权威：`tools/capabilities/content/mte-redstone-wire/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：CC EU cables also do not host covers. Separate from torch/repeater.

### `cover/torch-repeater-wire-host`

- 标题：红石火把/中继器盖板仍缺绝缘线宿主
- 状态：`open`
- 根因：`missing_cover_host` / `cover_only_attaches_to_insulated_redstone_wire`
- 数量：2 covers
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`logistics/cover-remainder`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`logistics/cover-remainder`
- 权威：`tools/capabilities/logistics/cover-remainder/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Insulated metas are live; these covers still refuse current hosts.

### `energy/cooler`

- 标题：电力/通量冷却器
- 状态：`open`
- 根因：`missing_runtime` / `cooler_later_card`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`energy/heat-exchangers`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/heat-exchangers`
- 权威：`tools/capabilities/energy/heat-exchangers/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Independent runtime, recipes and player path.

### `energy/reactor-backpack-radioactivity`

- 标题：无 CC 材料放射性等级表
- 状态：`open`
- 根因：`missing_form` / `no_material_radioactivity_table`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-observation-safety`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/nuclear-fission-observation-safety`
- 权威：`tools/capabilities/energy/nuclear-fission-observation-safety/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Backpack radioactivity stays blocked.

### `energy/reactor-temperature-kelvin`

- 标题：反应堆温度不得用 HU 伪造 Kelvin
- 状态：`open`
- 根因：`invariant` / `heat_is_hu_not_kelvin`
- 数量：n/a
- 排期：`not_work`
- 挡住：`none`
- 发现卡：`energy/nuclear-fission-observation-safety`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/nuclear-fission-observation-safety`, `energy/nuclear-fission-hot-fluids`
- 权威：`tools/capabilities/energy/nuclear-fission-observation-safety/capability.json`, `tools/capabilities/energy/nuclear-fission-hot-fluids/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Do not invent Kelvin as HU times a constant.

### `energy/reactor-world-explode`

- 标题：反应堆世界爆炸仍是注释掉的 TODO
- 状态：`open`
- 根因：`missing_runtime` / `gt6_explode_todo_commented`
- 数量：1 behaviors
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-observation-safety`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/nuclear-fission-observation-safety`
- 权威：`tools/capabilities/energy/nuclear-fission-observation-safety/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：GT6 explode() TODO stays commented.

### `fluid/ic2-coolant`

- 标题：IC2 工业冷却液不是 CC 流体
- 状态：`open`
- 根因：`missing_fluid` / `ic2_coolant_not_cc_owned`
- 数量：1 fluids
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-hot-fluids`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/nuclear-fission-hot-fluids`, `machines/injector`
- 权威：`tools/capabilities/energy/nuclear-fission-hot-fluids/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Not substituted. Injector overflow also names ic2coolant.

### `fluid/resin-rubber`

- 标题：FL.Resin_Rubber 缺失
- 状态：`open`
- 根因：`missing_fluid` / `missing_resin_rubber_fluid`
- 数量：1 fluids
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`worldgen/gt-trees`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`worldgen/gt-trees`
- 权威：`tools/capabilities/worldgen/gt-trees/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Latex is not a stand-in. Rubber hole still harvests rubber_resin.

### `fluid/sap-maple`

- 标题：FL.Sap_Maple 缺失
- 状态：`open`
- 根因：`missing_fluid` / `missing_maple_sap_fluid`
- 数量：1 fluids
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`worldgen/gt-trees`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`worldgen/gt-trees`
- 权威：`tools/capabilities/worldgen/gt-trees/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Maple hole still freezes and drills.

### `fluid/thorium-salt`

- 标题：钍盐到 LiCl 无 CC 身份
- 状态：`open`
- 根因：`missing_fluid` / `thorium_salt_not_hot_fluid_output`
- 数量：1 fluids
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-hot-fluids`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/nuclear-fission-hot-fluids`, `machines/injector`
- 权威：`tools/capabilities/energy/nuclear-fission-hot-fluids/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Not a hot-fluid output and has no CC-owned input identity.

### `identity/connector-art-keep-distinct-dummies`

- 标题：连接件美术卡仍保留 dummy 物品
- 状态：`open`
- 根因：`unmapped_identity` / `dummy_items_keep_distinct_until_runtime_hosts`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`content/gt6-connector-art`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-connector-art`
- 权威：`tools/capabilities/content/gt6-connector-art/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Replace iron-ingot models with the shared GT6 iconset; do not invent BlockItems.

### `identity/converter-turbines-battery-boxes`

- 标题：转换机折回 6 轮机 + 2 电池箱
- 状态：`open`
- 根因：`unmapped_identity` / `no_converter_catalog_host`
- 数量：8 items
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`content/gt6-mte-converter-host-fold`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-mte-converter-host-fold`
- 权威：`tools/capabilities/content/gt6-mte-converter-host-fold/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Keep dummy for a later runtime child. Do not edit R0.

### `identity/electric-unregistered-gauges`

- 标题：导线电缆 MTE 折回仍有未映射 Loader id
- 状态：`open`
- 根因：`unmapped_identity` / `superconductor_graphene_or_unregistered_gauge`
- 数量：61 loader_ids
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`content/electric-wire-cable-mte-fold`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/electric-wire-cable-mte-fold`
- 权威：`tools/capabilities/content/electric-wire-cable-mte-fold/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Mapped 259 is not a dummy-removal set. Graphene wireGt07 is not in the missing-gauge runtime set.

### `identity/hsla-ungated-gauges`

- 标题：HSLA 未开门线规仍 dummy
- 状态：`open`
- 根因：`unmapped_identity` / `ungated_hsla_wire_gauges`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`content/gt6-connector-alias-repair`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-connector-alias-repair`
- 权威：`tools/capabilities/content/gt6-connector-alias-repair/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Alias repair only. HSLA cableGt12 is not in the landed missing-gauge set.

### `identity/processing-ungated-families`

- 标题：加工机折回未开门家族
- 状态：`open`
- 根因：`unmapped_identity` / `no_matching_sourceid_host`
- 数量：3 families
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`content/gt6-mte-processing-host-fold`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-mte-processing-host-fold`
- 权威：`tools/capabilities/content/gt6-mte-processing-host-fold/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Hammer, Squeezer and Laser stay dummy (18 metas). Polarizer 20221-20225 and MagSep 20301-20305 fold onto live sourceId hosts.

### `material-form/copper-family-curved-plate`

- 标题：流体/物品管五档工作台缺 live curved_plate/double_plate
- 状态：`open`
- 根因：`missing_form` / `missing_plate_curved_for_table_crafts`
- 数量：n/a
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`content/gt6-fluid-pipe-acquisition`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-fluid-pipe-acquisition`, `content/gt6-item-pipe-acquisition`
- 权威：`tools/capabilities/content/gt6-fluid-pipe-acquisition/capability.json`, `tools/capabilities/content/gt6-item-pipe-acquisition/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Combo pack/unpack is live. Five-gauge table crafts stay blocked. Overlay on reuse_canonical rows, not a blocked identity_disposition.

### `obtain/injector-mv-hv-iv-hosts`

- 标题：注射机 MV/HV/IV 主机获得格仍 blocked
- 状态：`open`
- 根因：`missing_form` / `missing_host_acquisition_parts`
- 数量：3 hosts
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`machines/injector`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/injector`
- 权威：`tools/capabilities/machines/injector/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：LV and Chromium EV hosts are source-exact. Do not stand in other motors.

### `obtain/nanofab-hosts`

- 标题：纳米加工机五档主机获得格仍 blocked
- 状态：`open`
- 根因：`missing_form` / `missing_laser_gas_and_sapphire_processor`
- 数量：5 hosts
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`machines/nanofab`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/nanofab`
- 权威：`tools/capabilities/machines/nanofab/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：IL.Comp_Laser_Gas_Ar/Kr/Xe and sapphire crystal processor. Emitters/sensors already exist.

### `obtain/redstone-wiregt01`

- 标题：红石 wireGt01 获得格仍 blocked
- 状态：`open`
- 根因：`missing_obtain` / `no_real_wiregt01_recipe`
- 数量：3 items
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`content/mte-redstone-wire`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/mte-redstone-wire`, `content/gt6-redstone-wire-acquisition`
- 权威：`tools/capabilities/content/mte-redstone-wire/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Insulated cable laminator obtain is a different card. This is the bare wireGt01 recipe.

### `peripheral/sensors-computercraft`

- 标题：Sensors ComputerCraft 外设
- 状态：`open`
- 根因：`missing_mod_bridge` / `computercraft_peripheral_out_of_card`
- 数量：1 integrations
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`content/sensors`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/sensors`
- 权威：`tools/capabilities/content/sensors/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：ITileEntityComputerizable stays blocked. Sensors are not in the cover net.

### `recipe/fluidbed-overflow`

- 标题：流化床 49 行 blocked chain
- 状态：`open`
- 根因：`unmapped_operand` / `unmapped_storage_dust_or_div72`
- 数量：49 rows
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`recipe/blocked-chain-ledger`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/converter-catalog`
- 权威：`tools/blocked_recipe_ledger.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Must not be added to bath families or petroleum 702.

### `recipe/injector-overflow`

- 标题：注射机 overflow 仍 blocked
- 状态：`open`
- 根因：`unmapped_operand` / `unregistered_glass_slab_fluid_or_shadow`
- 数量：535 rows
- 排期：`scale_not_todo`
- 挡住：`player_complete`
- 发现卡：`machines/injector`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/injector`
- 权威：`tools/waves/machines/injector/overflow.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：27 prep overflow + 449 glass/slab + 59 shadow. 0 rows are leftover MTE identity.

### `recipe/laminator-overflow`

- 标题：层压机 unmapped MTE overflow
- 状态：`open`
- 根因：`unmapped_operand` / `unmapped_mte_or_gt_block_log`
- 数量：60 rows
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`machines/laminator`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/laminator`
- 权威：`tools/waves/machines/laminator/overflow.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：54 unmapped MTE + 6 gt_block log. Connector identity work turned 0 rows green.

### `recipe/loom-overflow`

- 标题：织机 overflow 仍 blocked
- 状态：`open`
- 根因：`unmapped_operand` / `unmapped_mte_plant_fiber_or_shadow`
- 数量：869 rows
- 排期：`scale_not_todo`
- 挡住：`player_complete`
- 发现卡：`machines/loom`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/loom`
- 权威：`tools/waves/machines/loom/overflow.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：232 unmapped MTE/plant_gt_fiber + 637 shadow. Connector identity work turned 0 rows green.

### `recipe/nanofab-overflow`

- 标题：纳米加工机 overflow 仍 blocked
- 状态：`open`
- 根因：`unmapped_operand` / `graphene_mte_dolamide_or_shadow_circuit`
- 数量：57 rows
- 排期：`scale_not_todo`
- 挡住：`player_complete`
- 发现卡：`machines/nanofab`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/nanofab`
- 权威：`tools/waves/machines/nanofab/overflow.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：12 prep overflow + 45 shadowed programmed_circuit signatures.

### `recipe/oven-cooking-oil-xp`

- 标题：熔炉烹饪油与 XP 流体
- 状态：`open`
- 根因：`missing_fluid` / `optional_furnace_fluids_out_of_child`
- 数量：2 fluids
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`machines/oven`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/oven`
- 权威：`tools/capabilities/machines/oven/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Live oven spec has zero fluid tanks.

### `recipe/pressure-washer-stone`

- 标题：压力清洗机 gt.stone overflow
- 状态：`open`
- 根因：`unmapped_operand` / `unmapped_gt_stone`
- 数量：120 rows
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`machines/pressure-washer`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/pressure-washer`
- 权威：`tools/waves/machines/pressure-washer/overflow.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Do not replace GT6 stone identities with unrelated blocks.

### `recipe/printer-dye-fluids`

- 标题：印刷机 22 行染料流体（prep）
- 状态：`open`
- 根因：`missing_fluid` / `missing_dye_chemical_fluids`
- 数量：22 rows
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`prep:machines/printer`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`prep:machines/printer`
- 权威：`docs/current/unimplemented-gap.md`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：gt.recipe.printer dump 22 / selected 0. No live family. Capability does not exist yet.

### `recipe/roll-former-rail-gt`

- 标题：辊压成型机 rail_gt 两行
- 状态：`open`
- 根因：`unmapped_operand` / `unmapped_rail_gt`
- 数量：2 rows
- 排期：`schedulable`
- 挡住：`player_complete`
- 发现卡：`machines/roll-former`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/roll-former`
- 权威：`tools/waves/machines/roll-former/overflow.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Non-blocking tail. Do not stand in tungsten_carbide or obsidian_steel rails.

### `storage/mass-storage-prefix-units`

- 标题：仓储桶前缀单位换算仍缺
- 状态：`open`
- 根因：`missing_runtime` / `prefix_unit_conversion_missing`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`none`
- 发现卡：`registry/catalog-modern-ids`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：—
- 权威：`docs/current/unimplemented-gap.md`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：GT6 MassStorage merges prefix units; CC MassStorageHandler only accepts isSameItemSameComponents. Hopper dust 1/4/9 is not the barrel converter.

### `tools/world-behaviors`

- 标题：手持工具世界行为与缺形态仍缺
- 状态：`open`
- 根因：`missing_runtime` / `gt6_tool_world_behaviors_missing`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`none`
- 发现卡：`registry/tool-head-remainder`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：—
- 权威：`docs/current/unimplemented-gap.md`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Pincers, hand drill, softhammer-on-storage, crowbar harvest, magnifier, electric tools. Clustered in unimplemented-gap.md §5 (click/mine/missing forms/unwired ToolActions). Not a recipe-row count. Do not bundle electric tools with builder wand.

### `worldgen/bees`

- 标题：Bees 仍缺 runtime
- 状态：`open`
- 根因：`missing_runtime` / `bumble_requires_new_runtime`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`worldgen/gt-crops`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`worldgen/gt-crops`
- 权威：`tools/waves/portfolio/crops-food-bees-r0/feasibility.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Not in the crops worldgen denominator.

### `worldgen/center`

- 标题：Center 维度仍是 prep
- 状态：`open`
- 根因：`missing_worldgen` / `requires_new_dimension_runtime`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`prep:worldgen/gt-center`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`prep:worldgen/gt-center`
- 权威：`docs/current/unimplemented-gap.md`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Not unique-active. See project-status prep list.

### `worldgen/dungeon-room-contents`

- 标题：地牢房间内容与 GT 石材仍 blocked
- 状态：`open`
- 根因：`missing_worldgen` / `keys_zpm_portals_fixtures_bedrock_ore_missing_forms`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`worldgen/gt-dungeon`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`worldgen/gt-dungeon`
- 权威：`tools/capabilities/worldgen/gt-dungeon/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Paused back to prep. Do not call runtime_ready until carrier and room geometry re-verify together.

### `worldgen/food`

- 标题：Food 榨汁/发酵仍缺 runtime
- 状态：`open`
- 根因：`missing_runtime` / `juicer_fermenter_requires_new_runtime`
- 数量：n/a
- 排期：`scale_not_todo`
- 挡住：`player_complete`
- 发现卡：`worldgen/gt-crops`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`worldgen/gt-crops`
- 权威：`tools/waves/portfolio/crops-food-bees-r0/feasibility.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：juicer 96 + fermenter 6435 are scale context, not census.

### `worldgen/planet-rocks`

- 标题：行星岩仍是 prep
- 状态：`open`
- 根因：`missing_worldgen` / `requires_extra_dimension`
- 数量：3 families
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`prep:worldgen/gt-planet-rocks`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`prep:worldgen/gt-planet-rocks`
- 权威：`docs/history/card-plans/prep/GT行星岩详细计划.md`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：moon/mars/planet rocks. Do not alias surface_rock_scatter.

### `identity/eu-blocked-gauges`

- 标题：EU 导线缺线规（部分已落地）
- 状态：`partial`
- 根因：`unmapped_identity` / `graphene_superconductor_or_unprefixed_gauges`
- 数量：n/a
- 排期：`audit_first`
- 挡住：`player_complete`
- 发现卡：`content/gt6-eu-wire-cable-runtime`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-eu-wire-cable-runtime`, `content/gt6-eu-missing-wire-gauges-runtime`
- 权威：`tools/capabilities/content/gt6-eu-wire-cable-runtime/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：wireGt07/09/10/11/13/14/15 for 24 materials landed on the missing-gauge child. Graphene/superconductor remain dummy. Same semantic_key stays on the parent.

### `recipe/melter-overflow`

- 标题：熔融机 overflow（规模未逐行核实）
- 状态：`partial`
- 根因：`unmapped_operand` / `unmapped_fluids_identities_or_tank_limit`
- 数量：2796 rows
- 排期：`scale_not_todo`
- 挡住：`player_complete`
- 发现卡：`machines/melter`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/melter`
- 权威：`tools/waves/machines/melter/overflow.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Only a classified subset is verified; the rest is UNVERIFIED_SCALE. Do not copy 2796 into a todo list.

### `energy/large-heat-exchanger-17197`

- 标题：大型热交换器 17197
- 状态：`resolved`
- 根因：`missing_runtime` / `large_hex_follow_up_card`
- 数量：1 metas
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`energy/heat-exchangers`
- 由本卡引入：否（发现既有缺口）
- 解决卡：energy/large-heat-exchanger
- 影响：`energy/heat-exchangers`, `energy/large-heat-exchanger`
- 权威：`tools/capabilities/energy/large-heat-exchanger/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：17197 3x3x2 HEX, HU from transmitters, FM.Hot execution, survival obtain grid, and GameTests landed on energy/large-heat-exchanger.

### `energy/reactor-fusion`

- 标题：聚变控制器获得格与等离子链（18 行已发布）
- 状态：`resolved`
- 根因：`missing_runtime` / `fusion_waits_on_fission_heat_contracts`
- 数量：n/a
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-survival`
- 由本卡引入：否（发现既有缺口）
- 解决卡：energy/fusion-quantum
- 影响：`energy/nuclear-fission-survival`, `energy/fusion-quantum`
- 权威：`tools/capabilities/energy/fusion-quantum/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：18 source-backed Fusion rows execute. Controller obtain grid uses IV field emitters. FUELS_PLASMA stays empty. Neutral-matter bootstrap is CC_EXTENSION on FUSION_EXTENSION.

### `energy/steam-turbine`

- 标题：蒸汽涡轮 STEAM→RU
- 状态：`resolved`
- 根因：`missing_runtime` / `steam_turbine_later_card`
- 数量：n/a
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`energy/heat-exchangers`
- 由本卡引入：否（发现既有缺口）
- 解决卡：energy/steam-turbine
- 影响：`energy/heat-exchangers`, `energy/steam-turbine`
- 权威：`tools/capabilities/energy/steam-turbine/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：15 singles plus 4 large housings convert STEAM to RU with distilled-water recovery. Dedicated MTE hosts, not HEX or transformers.

### `energy/transformer-long-distance`

- 标题：长距变压器 10064–10068
- 状态：`resolved`
- 根因：`missing_runtime` / `needs_block_long_dist_wire`
- 数量：5 metas
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`energy/transformers`
- 由本卡引入：否（发现既有缺口）
- 解决卡：content/puv-omega-parts
- 影响：`energy/transformers`, `content/puv-omega-parts`
- 权威：`tools/capabilities/content/puv-omega-parts/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Dedicated 10064-10068 endpoints plus five LongDistWire voltage hosts. Same-voltage EU with max(64, distance/8) loss. Not in the voltage-step transformer catalog.

### `material-form/luv-puv1-parts`

- 标题：LuV–OMEGA 紧凑零件已落地（CC 扩展，非原版 PUV2+）
- 状态：`resolved`
- 根因：`missing_form` / `missing_high_voltage_technological_parts`
- 数量：4 tiers
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`content/technological-parts-foundation`
- 由本卡引入：否（发现既有缺口）
- 解决卡：content/puv-omega-parts
- 影响：`content/technological-parts-foundation`, `content/puv-omega-parts`
- 权威：`tools/capabilities/content/puv-omega-parts/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Compact electric parts 0-14 including LuV-OMEGA, Quantum circuit canonical item, and long-voltage transformers through OMEGA. VN[15] is not registered.

### `material-form/paper-tiny-plate`

- 标题：纸微型板已注册，切片机 overflow 为 0
- 状态：`resolved`
- 根因：`missing_form` / `paper_tiny_plate_was_missing`
- 数量：1 forms
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`machines/slicer`
- 由本卡引入：否（发现既有缺口）
- 解决卡：content/gt6-paper-tiny-plate
- 影响：`content/gt6-paper-tiny-plate`, `machines/slicer`
- 权威：`tools/capabilities/content/gt6-paper-tiny-plate/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：GT6 OP.plateTiny.forceItemGeneration(MT.Paper). Not a stand-in plate.

### `obtain/mte-inplace-runtime`

- 标题：14 张 in-place MTE runtime 获得格已按 source-exact 收口
- 状态：`resolved`
- 根因：`missing_obtain` / `runtime_without_source_exact_obtain`
- 数量：14 capabilities
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`content/gt6-mte-fluid-attachments-runtime`
- 由本卡引入：否（发现既有缺口）
- 解决卡：content/gt6-mte-inplace-acquisition
- 影响：`content/gt6-mte-converter-remainder-runtime`, `content/gt6-mte-crucible-foundry-runtime`, `content/gt6-mte-decorative-runtime`, `content/gt6-mte-drive-runtime`, `content/gt6-mte-extender-runtime`, `content/gt6-mte-fluid-attachments-runtime`, `content/gt6-mte-furniture-barrel-runtime`, `content/gt6-mte-furniture-chest-runtime`, `content/gt6-mte-furniture-safe-runtime`, `content/gt6-mte-furniture-scaffold-runtime`, `content/gt6-mte-furniture-storage-runtime`, `content/gt6-mte-furniture-table-runtime`, `content/gt6-mte-misc-tool-runtime`, `content/gt6-mte-multiblock-runtime`
- 权威：`tools/capabilities/content/gt6-mte-converter-remainder-runtime/capability.json`, `tools/capabilities/content/gt6-mte-crucible-foundry-runtime/capability.json`, `tools/capabilities/content/gt6-mte-decorative-runtime/capability.json`, `tools/capabilities/content/gt6-mte-drive-runtime/capability.json`, `tools/capabilities/content/gt6-mte-extender-runtime/capability.json`, `tools/capabilities/content/gt6-mte-fluid-attachments-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-barrel-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-chest-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-safe-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-scaffold-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-storage-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-table-runtime/capability.json`, `tools/capabilities/content/gt6-mte-misc-tool-runtime/capability.json`, `tools/capabilities/content/gt6-mte-multiblock-runtime/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Audit complete. Source-exact grids live on content/gt6-mte-inplace-acquisition. Remaining D0 gaps stay in per-family current_gap.json, not this clustered blocker. Closing does not promote the 14 runtimes to player_complete.

### `recipe/bath-identity-families`

- 标题：Bath identity 5 家族（T49 已完成）
- 状态：`resolved`
- 根因：`unmapped_operand` / `bath_identity_unmapped_families`
- 数量：5 families
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`recipe/blocked-chain-ledger`
- 由本卡引入：否（发现既有缺口）
- 解决卡：T49详细计划
- 影响：—
- 权威：`tools/blocked_recipe_ledger.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：T49 tiny-purified cohort 已关闭这 5 个 family。Bath identity 与 ordinary remainder 已完成。

### `recipe/bath-remainder-families`

- 标题：Bath remainder 150 家族（T48/T49 已完成）
- 状态：`resolved`
- 根因：`unmapped_operand` / `bath_remainder_unmapped_families`
- 数量：150 families
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`recipe/blocked-chain-ledger`
- 由本卡引入：否（发现既有缺口）
- 解决卡：T49详细计划
- 影响：—
- 权威：`tools/blocked_recipe_ledger.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：T48 完成 145 个 family，T49 完成最后 5 个。Bath ordinary remainder 已为 0；不要把历史 family 分母加入 fluidbed 49。

### `worldgen/crops-glowtus-bush`

- 标题：作物世界生成 16 glowtus + 灌木已落地
- 状态：`resolved`
- 根因：`missing_worldgen` / `crops_worldgen_was_missing`
- 数量：2 families
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`worldgen/gt-trees`
- 由本卡引入：否（发现既有缺口）
- 解决卡：worldgen/gt-crops
- 影响：`worldgen/gt-crops`
- 权威：`tools/capabilities/worldgen/gt-crops/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Closed runtime_ready, not player_complete. Squeezer dump stays out of scope.

### `energy/reactor-hot-fluids-deferred`

- 标题：生存卡上的热流体延期键（已另卡落地）
- 状态：`superseded`
- 根因：`deferred_key` / `deferred_to_hot_fluids_card`
- 数量：1 keys
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-survival`
- 由本卡引入：否（发现既有缺口）
- 解决卡：energy/nuclear-fission-hot-fluids
- 影响：`energy/nuclear-fission-survival`, `energy/nuclear-fission-hot-fluids`
- 权威：`tools/capabilities/energy/nuclear-fission-survival/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Distilled water to steam stayed on the survival card.

### `energy/reactor-observation-safety-deferred`

- 标题：热流体/生存卡上的观察安全延期键（已另卡落地）
- 状态：`superseded`
- 根因：`deferred_key` / `deferred_to_observation_safety_card`
- 数量：2 keys
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-survival`
- 由本卡引入：否（发现既有缺口）
- 解决卡：energy/nuclear-fission-observation-safety
- 影响：`energy/nuclear-fission-survival`, `energy/nuclear-fission-hot-fluids`, `energy/nuclear-fission-observation-safety`
- 权威：`tools/capabilities/energy/nuclear-fission-survival/capability.json`, `tools/capabilities/energy/nuclear-fission-hot-fluids/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Those rows still say blocked so later close still binds this id.

### `identity/fluid-quadruple-nonuple`

- 标题：四联/九联流体管（runtime 已另卡落地）
- 状态：`superseded`
- 根因：`unmapped_identity` / `combo_pipe_was_dummy_on_parent`
- 数量：2 forms
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`content/gt6-fluid-pipe-runtime`
- 由本卡引入：否（发现既有缺口）
- 解决卡：content/gt6-fluid-combo-pipe-runtime
- 影响：`content/gt6-fluid-pipe-runtime`, `content/gt6-fluid-combo-pipe-runtime`
- 权威：`tools/capabilities/content/gt6-fluid-pipe-runtime/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Parent identity_disposition still says blocked. Combo runtime owns the live BlockItems.

### `identity/item-restrictive`

- 标题：Restrictive 物品管（runtime 已另卡落地）
- 状态：`superseded`
- 根因：`unmapped_identity` / `restrictive_pipe_was_dummy_on_parent`
- 数量：3 forms
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`content/gt6-item-pipe-runtime`
- 由本卡引入：否（发现既有缺口）
- 解决卡：content/gt6-restrictive-item-pipe-runtime
- 影响：`content/gt6-item-pipe-runtime`, `content/gt6-restrictive-item-pipe-runtime`
- 权威：`tools/capabilities/content/gt6-item-pipe-runtime/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Parent identity_disposition still says blocked.

### `identity/redstone-insulated-extras`

- 标题：绝缘红石 27006/27056/27506（runtime 已另卡落地）
- 状态：`superseded`
- 根因：`unmapped_identity` / `laminator_extras_not_in_catalog_1817`
- 数量：3 metas
- 排期：`not_work`
- 挡住：`player_complete`
- 发现卡：`content/mte-redstone-wire`
- 由本卡引入：否（发现既有缺口）
- 解决卡：content/gt6-insulated-redstone-runtime
- 影响：`content/mte-redstone-wire`, `content/gt6-insulated-redstone-runtime`
- 权威：`tools/capabilities/content/mte-redstone-wire/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Parent identity_disposition still says blocked.

### `energy/transformer-rotation-gearbox`

- 标题：旋转齿轮箱不得冒充电变压器
- 状态：`out_of_scope`
- 根因：`invariant` / `rotational_gearbox_is_not_transformer_rotation`
- 数量：1 identities
- 排期：`not_work`
- 挡住：`none`
- 发现卡：`energy/transformers`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/transformers`
- 权威：`tools/capabilities/energy/transformers/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：CC rotational_gearbox must not impersonate MultiTileEntityTransformerRotation.

### `historical/petroleum-sampled-702`

- 标题：石油采样 702（历史可选，非当前合计）
- 状态：`out_of_scope`
- 根因：`historical_optional` / `historical_optional_petroleum_sample`
- 数量：702 rows
- 排期：`not_work`
- 挡住：`none`
- 发现卡：`recipe/blocked-chain-ledger`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：—
- 权威：`tools/blocked_recipe_ledger.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Do not treat the 2026-08-11 702 as a current total.

### `identity/redstone-not-eu-or-pipe`

- 标题：红石线不得挂 EU/管网（不变量）
- 状态：`out_of_scope`
- 根因：`invariant` / `redstone_must_not_attach_energy_or_pipe`
- 数量：n/a
- 排期：`not_work`
- 挡住：`none`
- 发现卡：`content/gt6-redstone-wire-correction`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-redstone-wire-correction`
- 权威：`tools/capabilities/content/gt6-redstone-wire-correction/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Must not attach ENERGY, CableNetworkTraversal, or PipeTopology.

### `identity/sanding-grindstone-32703`

- 标题：打磨机不包含 Grindstone 32703
- 状态：`out_of_scope`
- 根因：`out_of_child` / `grindstone_is_separate_mte_class`
- 数量：1 metas
- 排期：`not_work`
- 挡住：`none`
- 发现卡：`machines/sanding`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/sanding`
- 权威：`tools/capabilities/machines/sanding/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Also binds RM.Sharpening but is MultiTileEntityGrindStone.

### `recipe/squeezer-dump-5322`

- 标题：squeezer dump 5322 不是作物卡分母
- 状态：`out_of_scope`
- 根因：`out_of_child` / `crops_card_does_not_own_squeezer`
- 数量：5322 rows
- 排期：`not_work`
- 挡住：`none`
- 发现卡：`worldgen/gt-crops`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`worldgen/gt-crops`
- 权威：`docs/current/unimplemented-gap.md`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Scale context only. No juicer host on the crops card.

## 怎么用

关 `runtime_ready` / `player_complete` 时，能力上每一条
`disposition=blocked` 或「Obtain stays explicitly_blocked」必须绑定本账的 `id`。
新缺口写进 `tools/blockers/catalog.json` 再 `--write`，并填 `planning_bucket`。
修根因时按 `root_cause_class` 集中收口。从 B 抽卡；A 按根因切片，不要按行数选最大。
