# Blocker 总账

> 本页由 `python tools/build_blockers.py --write` 从
> `tools/blockers/catalog.json` 生成，不要手改。
> 权威是 catalog；本页和 `tools/blockers/ledger.json` 都是投影。
> 不同条目、不同 `unit` **不得相加**。发现旧缺口不是任务制造了缺口。

## 统计

- 条目 60：open 46，partial 2，resolved 2，superseded 5，out_of_scope 5
- 未关闭数量按条目列出（不得相加）：
  - `architecture/combinatorial-leftover`：4 families
  - `cover/torch-repeater-wire-host`：2 covers
  - `energy/large-heat-exchanger-17197`：1 metas
  - `energy/reactor-fusion`：18 runtime_rows
  - `energy/reactor-world-explode`：1 behaviors
  - `energy/transformer-long-distance`：5 metas
  - `fluid/ic2-coolant`：1 fluids
  - `fluid/resin-rubber`：1 fluids
  - `fluid/sap-maple`：1 fluids
  - `fluid/thorium-salt`：1 fluids
  - `identity/converter-turbines-battery-boxes`：8 items
  - `identity/electric-unregistered-gauges`：61 loader_ids
  - `identity/processing-ungated-families`：5 families
  - `material-form/luv-puv1-parts`：4 tiers
  - `obtain/injector-mv-hv-iv-hosts`：3 hosts
  - `obtain/mte-inplace-runtime`：14 capabilities
  - `obtain/nanofab-hosts`：5 hosts
  - `obtain/redstone-wiregt01`：3 items
  - `peripheral/sensors-computercraft`：1 integrations
  - `recipe/bath-identity-families`：5 families
  - `recipe/bath-remainder-families`：150 families
  - `recipe/fluidbed-overflow`：49 rows
  - `recipe/injector-overflow`：535 rows
  - `recipe/laminator-overflow`：60 rows
  - `recipe/loom-overflow`：869 rows
  - `recipe/melter-overflow`：2796 rows
  - `recipe/nanofab-overflow`：57 rows
  - `recipe/oven-cooking-oil-xp`：2 fluids
  - `recipe/pressure-washer-stone`：120 rows
  - `recipe/printer-dye-fluids`：22 rows
  - `recipe/roll-former-rail-gt`：2 rows
  - `worldgen/planet-rocks`：3 families

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
- `missing_form`（5）
  - `energy/reactor-backpack-radioactivity`
  - `material-form/copper-family-curved-plate`
  - `material-form/luv-puv1-parts`
  - `obtain/injector-mv-hv-iv-hosts`
  - `obtain/nanofab-hosts`
- `missing_mod_bridge`（1）
  - `peripheral/sensors-computercraft`
- `missing_obtain`（2）
  - `obtain/mte-inplace-runtime`
  - `obtain/redstone-wiregt01`
- `missing_runtime`（10）
  - `energy/cooler`
  - `energy/large-heat-exchanger-17197`
  - `energy/reactor-fusion`
  - `energy/reactor-world-explode`
  - `energy/steam-turbine`
  - `energy/transformer-long-distance`
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
- `unmapped_operand`（10）
  - `recipe/bath-identity-families`
  - `recipe/bath-remainder-families`
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
- 挡住：`player_complete`
- 发现卡：`energy/heat-exchangers`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/heat-exchangers`
- 权威：`tools/capabilities/energy/heat-exchangers/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Independent runtime, recipes and player path.

### `energy/large-heat-exchanger-17197`

- 标题：大型热交换器 17197
- 状态：`open`
- 根因：`missing_runtime` / `large_hex_follow_up_card`
- 数量：1 metas
- 挡住：`player_complete`
- 发现卡：`energy/heat-exchangers`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/heat-exchangers`
- 权威：`tools/capabilities/energy/heat-exchangers/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：First HEX slice is not the large machine.

### `energy/reactor-backpack-radioactivity`

- 标题：无 CC 材料放射性等级表
- 状态：`open`
- 根因：`missing_form` / `no_material_radioactivity_table`
- 数量：n/a
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-observation-safety`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/nuclear-fission-observation-safety`
- 权威：`tools/capabilities/energy/nuclear-fission-observation-safety/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Backpack radioactivity stays blocked.

### `energy/reactor-fusion`

- 标题：聚变控制器生存配方与等离子链
- 状态：`open`
- 根因：`missing_runtime` / `fusion_waits_on_fission_heat_contracts`
- 数量：18 runtime_rows
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-survival`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/nuclear-fission-survival`
- 权威：`tools/capabilities/energy/nuclear-fission-survival/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：18 fusion runtime rows stay runtime_ready only. Do not derive from existing fusion rows.

### `energy/reactor-temperature-kelvin`

- 标题：反应堆温度不得用 HU 伪造 Kelvin
- 状态：`open`
- 根因：`invariant` / `heat_is_hu_not_kelvin`
- 数量：n/a
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
- 挡住：`player_complete`
- 发现卡：`energy/nuclear-fission-observation-safety`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/nuclear-fission-observation-safety`
- 权威：`tools/capabilities/energy/nuclear-fission-observation-safety/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：GT6 explode() TODO stays commented.

### `energy/steam-turbine`

- 标题：蒸汽涡轮 STEAM→RU
- 状态：`open`
- 根因：`missing_runtime` / `steam_turbine_later_card`
- 数量：n/a
- 挡住：`player_complete`
- 发现卡：`energy/heat-exchangers`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/heat-exchangers`
- 权威：`tools/capabilities/energy/heat-exchangers/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Must not impersonate HEX or electric transformer.

### `energy/transformer-long-distance`

- 标题：长距变压器 10064–10068
- 状态：`open`
- 根因：`missing_runtime` / `needs_block_long_dist_wire`
- 数量：5 metas
- 挡住：`player_complete`
- 发现卡：`energy/transformers`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`energy/transformers`
- 权威：`tools/capabilities/energy/transformers/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Same-voltage pair scan, not this voltage-step card.

### `fluid/ic2-coolant`

- 标题：IC2 工业冷却液不是 CC 流体
- 状态：`open`
- 根因：`missing_fluid` / `ic2_coolant_not_cc_owned`
- 数量：1 fluids
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
- 数量：5 families
- 挡住：`player_complete`
- 发现卡：`content/gt6-mte-processing-host-fold`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-mte-processing-host-fold`
- 权威：`tools/capabilities/content/gt6-mte-processing-host-fold/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Hammer, Squeezer, Polarizer, MagSep and Laser stay dummy.

### `material-form/copper-family-curved-plate`

- 标题：流体/物品管五档工作台缺 live curved_plate/double_plate
- 状态：`open`
- 根因：`missing_form` / `missing_plate_curved_for_table_crafts`
- 数量：n/a
- 挡住：`player_complete`
- 发现卡：`content/gt6-fluid-pipe-acquisition`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-fluid-pipe-acquisition`, `content/gt6-item-pipe-acquisition`
- 权威：`tools/capabilities/content/gt6-fluid-pipe-acquisition/capability.json`, `tools/capabilities/content/gt6-item-pipe-acquisition/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Combo pack/unpack is live. Five-gauge table crafts stay blocked. Overlay on reuse_canonical rows, not a blocked identity_disposition.

### `material-form/luv-puv1-parts`

- 标题：LuV–PUV1 紧凑零件网格仍缺
- 状态：`open`
- 根因：`missing_form` / `missing_high_voltage_technological_parts`
- 数量：4 tiers
- 挡住：`player_complete`
- 发现卡：`content/technological-parts-foundation`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/technological-parts-foundation`
- 权威：`docs/current/unimplemented-gap.md`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：LuV ≠ wireGt07. Existing covers still use programmed_circuit upgrades. Separate dedicated card.

### `obtain/injector-mv-hv-iv-hosts`

- 标题：注射机 MV/HV/IV 主机获得格仍 blocked
- 状态：`open`
- 根因：`missing_form` / `missing_host_acquisition_parts`
- 数量：3 hosts
- 挡住：`player_complete`
- 发现卡：`machines/injector`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/injector`
- 权威：`tools/capabilities/machines/injector/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：LV and Chromium EV hosts are source-exact. Do not stand in other motors.

### `obtain/mte-inplace-runtime`

- 标题：14 张 in-place MTE runtime 获得格仍 blocked
- 状态：`open`
- 根因：`missing_obtain` / `runtime_without_source_exact_obtain`
- 数量：14 capabilities
- 挡住：`player_complete`
- 发现卡：`content/gt6-mte-fluid-attachments-runtime`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/gt6-mte-converter-remainder-runtime`, `content/gt6-mte-crucible-foundry-runtime`, `content/gt6-mte-decorative-runtime`, `content/gt6-mte-drive-runtime`, `content/gt6-mte-extender-runtime`, `content/gt6-mte-fluid-attachments-runtime`, `content/gt6-mte-furniture-barrel-runtime`, `content/gt6-mte-furniture-chest-runtime`, `content/gt6-mte-furniture-safe-runtime`, `content/gt6-mte-furniture-scaffold-runtime`, `content/gt6-mte-furniture-storage-runtime`, `content/gt6-mte-furniture-table-runtime`, `content/gt6-mte-misc-tool-runtime`, `content/gt6-mte-multiblock-runtime`
- 权威：`tools/capabilities/content/gt6-mte-converter-remainder-runtime/capability.json`, `tools/capabilities/content/gt6-mte-crucible-foundry-runtime/capability.json`, `tools/capabilities/content/gt6-mte-decorative-runtime/capability.json`, `tools/capabilities/content/gt6-mte-drive-runtime/capability.json`, `tools/capabilities/content/gt6-mte-extender-runtime/capability.json`, `tools/capabilities/content/gt6-mte-fluid-attachments-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-barrel-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-chest-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-safe-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-scaffold-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-storage-runtime/capability.json`, `tools/capabilities/content/gt6-mte-furniture-table-runtime/capability.json`, `tools/capabilities/content/gt6-mte-misc-tool-runtime/capability.json`, `tools/capabilities/content/gt6-mte-multiblock-runtime/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Clustered note_marker bind. Finding the gap is not introducing it.

### `obtain/nanofab-hosts`

- 标题：纳米加工机五档主机获得格仍 blocked
- 状态：`open`
- 根因：`missing_form` / `missing_laser_gas_and_sapphire_processor`
- 数量：5 hosts
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
- 挡住：`player_complete`
- 发现卡：`content/sensors`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`content/sensors`
- 权威：`tools/capabilities/content/sensors/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：ITileEntityComputerizable stays blocked. Sensors are not in the cover net.

### `recipe/bath-identity-families`

- 标题：Bath identity 5 家族
- 状态：`open`
- 根因：`unmapped_operand` / `bath_identity_unmapped_families`
- 数量：5 families
- 挡住：`player_complete`
- 发现卡：`recipe/blocked-chain-ledger`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：—
- 权威：`tools/blocked_recipe_ledger.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Separate from remainder 150.

### `recipe/bath-remainder-families`

- 标题：Bath remainder 150 家族
- 状态：`open`
- 根因：`unmapped_operand` / `bath_remainder_unmapped_families`
- 数量：150 families
- 挡住：`player_complete`
- 发现卡：`recipe/blocked-chain-ledger`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：—
- 权威：`tools/blocked_recipe_ledger.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Family count, not recipe rows. Do not add to fluidbed 49.

### `recipe/fluidbed-overflow`

- 标题：流化床 49 行 blocked chain
- 状态：`open`
- 根因：`unmapped_operand` / `unmapped_storage_dust_or_div72`
- 数量：49 rows
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
- 挡住：`none`
- 发现卡：`registry/tool-head-remainder`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：—
- 权威：`docs/current/unimplemented-gap.md`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Pincers, hand drill, softhammer-on-storage, crowbar harvest, magnifier, electric tools. Clustered; not a recipe-row count.

### `worldgen/bees`

- 标题：Bees 仍缺 runtime
- 状态：`open`
- 根因：`missing_runtime` / `bumble_requires_new_runtime`
- 数量：n/a
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
- 挡住：`player_complete`
- 发现卡：`machines/melter`
- 由本卡引入：否（发现既有缺口）
- 解决卡：—
- 影响：`machines/melter`
- 权威：`tools/waves/machines/melter/overflow.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：Only a classified subset is verified; the rest is UNVERIFIED_SCALE. Do not copy 2796 into a todo list.

### `material-form/paper-tiny-plate`

- 标题：纸微型板已注册，切片机 overflow 为 0
- 状态：`resolved`
- 根因：`missing_form` / `paper_tiny_plate_was_missing`
- 数量：1 forms
- 挡住：`player_complete`
- 发现卡：`machines/slicer`
- 由本卡引入：否（发现既有缺口）
- 解决卡：content/gt6-paper-tiny-plate
- 影响：`content/gt6-paper-tiny-plate`, `machines/slicer`
- 权威：`tools/capabilities/content/gt6-paper-tiny-plate/capability.json`
- 禁止 stand-in：Do not substitute an unrelated material, prefix, vanilla item, or programmed_circuit.
- 说明：GT6 OP.plateTiny.forceItemGeneration(MT.Paper). Not a stand-in plate.

### `worldgen/crops-glowtus-bush`

- 标题：作物世界生成 16 glowtus + 灌木已落地
- 状态：`resolved`
- 根因：`missing_worldgen` / `crops_worldgen_was_missing`
- 数量：2 families
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
新缺口写进 `tools/blockers/catalog.json` 再 `--write`。
修根因时按 `root_cause_class` 集中收口，不要把 overflow 行数抄成待办。
