# CrucibleCraft

NeoForge 1.21.1 metalworking inspired by GregTech 6 and TerraFirmaCraft.

## Current progression

Coal coke is produced in a 3x3x3 hollow coke oven. Place the controller in the
middle of one outer face, use firebrick for the other 25 shell blocks, leave
the center hollow, and place a burning solid-fuel firebox below the center of
the bottom layer. Insert coal, light the controller with flint and steel, and wait
3,600 ticks for one coal coke and 500 mB of creosote.

Steelmaking currently uses a block bellows. Charge a molten crucible with
exactly three parts iron to one part carbon, keep the block above the crucible
open to air, and place the bellows with its piston face pointing directly into
the crucible. Right-clicking it produces a 40-tick stroke at 16 air per tick.
The crucible pulls that shared per-tick output only while its open, molten,
valid steel charge has buffer room; cold or invalid charges consume no air.
Airflow removes carbon until the charge enters the steel carbon range; a future
GT6-style steam engine will expose the same sided air capability.

The first powered processing chain is now survival-complete: coke oven coal
coke crafts into one canonical carbon dust (144 material units), while a
firebox below a bronze boiler supplies heat for steam. The boiler converts
exactly 80 HU plus 1 mB water into 160 mB steam, accumulating low-rate inputs
without rounding loss. Water enters from the bottom or horizontal faces and
steam leaves from the top.

A bronze steam engine accepts steam on every face except its front output and
converts 2 mB steam into 1 KU. This keeps the GT6-inspired `2 SU = 1 EU`
accounting while giving the bronze engine a nominal, rate-limited 24 KU/t
packet. Its signed push/return piston phase is observable through the public
kinetic capability; consumers extract magnitude with simulation before
mutation. Place a bronze crusher directly in front of the engine. It requires
a full 16 KU/t for 128 sustained powered ticks and turns any supported
144-unit raw-ore form—including vanilla raw copper, iron, and gold—into one
canonical crushed ore. Blocked outputs and underpowered ticks pause progress.

The original bronze steam slice remains adjacency-based KU, but the project no
longer defers every other power topology. T12 now separates KU push, RU
rotation, and EU: electric motors feed rotational axles and gearboxes into
tiered centrifuges, while EU cables feed tiered electrolyzers. These are
verified vertical slices rather than claims of complete GT6 energy coverage.

Crucibles retain a material identity under the existing `crucible` item and
block id. Firing the clay vessel creates a ceramic crucible; surround it with
four bronze plates to upgrade it to bronze, then use four steel plates for the
steel casing. The casing controls process capability, thermal mass, and the
GT6-style maximum temperature of 125% of its material melting point. Ceramic
and bronze accept through tier-2 charges to preserve the established
copper/bronze and iron/carbon progression; steel accepts tier 3.

Shape seven clay balls into an unfired crucible, or five into a mold blank,
press the blank with the desired template, then fire it in a furnace. Fired
ingot, plate, rod, and bolt molds are placed directly beside a crucible.
Empty-hand use pours the smallest exact material batch; use it again after the
metal cools below its melting point to retrieve the cast parts.

Molten materials are exposed through the crucible's fluid capability on every
side. One ingot is 144 mB (and 144 internal material units). Only exact,
integral material or alloy amounts can be inserted or extracted; alloy
composition remains the crucible's source of truth.
Molten buckets can be used directly. Draining resolves the exact current alloy
first (so bronze drains as bronze), while non-alloy mixtures cannot drain.
Breaking a crucible preserves only its casing material; contents, temperature,
stored air, and reaction progress are deliberately not portable.

## Development roadmap

- [Overall rules and architecture](CrucibleCraft-%E6%80%BB%E4%BD%93%E8%A7%84%E5%88%92.md)
- [Current phase: T13–T19](CrucibleCraft-%E7%AC%AC%E4%B8%89%E9%98%B6%E6%AE%B5%E6%80%BB%E4%BD%93%E8%A7%84%E5%88%92.md)
- [Phase 2 closure summary](CrucibleCraft-%E7%AC%AC%E4%BA%8C%E9%98%B6%E6%AE%B5%E6%80%BB%E4%BD%93%E8%A7%84%E5%88%92.md)
- [T7–T9 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T7-T9.md)
- [T10–T12 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T10-T12.md)
- [T13–T16 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T13-T16.md)
- [T17 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T17.md)

The second phase closed the Kind/Tier, RU/KU/EU, and JSON multiblock
architecture with selected vertical slices. The third phase has closed T13
canonical denominators, T14 measured Hybrid recipe materialization, T15
residual/acquisition evidence, and the T16 RU/KU selected tier batch.
`T16_READY` means 5 selected kinds across 3 tiers, 2 preimplemented kinds, and
13 explicitly deferred kinds—not all 20 denominator kinds implemented. Its 15
machine acquisition recipes are vanilla crafting recipes; GT publication stays
at zero delta.

T17 is now `T17_READY`: the HU/EU denominator contains 28 kinds with zero
unclassified, but T17 implements only 3 selected HU kinds across 3 tiers. One
Electrolyzer kind is a preimplemented EU reference and 24 kinds remain
explicitly deferred; this is not a claim that all 28 kinds are implemented.
The nine acquisition recipes are vanilla crafting, while RecipeMap stable ids,
18,875 logical / 16,650 eager / 2,225 lazy rows, and EMI enumeration remain
equal to T16 with zero GT publication delta. T18 steam, fuel, and energy
conversion is the current entry.

## Development verification

Python verification uses only the standard-library `unittest` runner:

```text
python tools/run_python_tests.py --suite fast
python tools/run_python_tests.py --suite affected --path tools/example.py
python tools/run_python_tests.py --suite closure
python tools/run_python_tests.py --suite source-replay
```

`fast` is the daily logic/fixture gate. `affected` maps changed paths to owned
test modules and automatically escalates unknown paths to `closure`. `closure`
discovers every Python test exactly once and prints the slowest tests.
`source-replay` is the explicit raw/cache GT6 audit; unavailable source
artifacts are reported as `SKIP`, never as an implicit pass.

Phase closure and CI use the fail-fast unified entry point:

```text
python tools/run_full_verification.py --check-ready
python tools/run_full_verification.py --record
python tools/run_full_verification.py --record --resume
```

Use `fast` / `affected` while developing and run one final `--record` closure.
Do not run a full closure in a child task and then repeat it in the parent task.
`--resume` reuses only digest-checked evidence from the same tooling snapshot,
policy, and toolchain; snapshot drift opens a new session. `--check-ready`
builds the current snapshot and validates the committed `READY` report without
starting Gradle, GameTest, or Python. The compatible `--check` mode still runs
the full read-only closure.

The report is replaced atomically only after every step passes. If a late step
fails, keep the same snapshot, fix the cause, and use `--record --resume`; the
runner prints each digest-validated heavy step it skips. A final
`--check-ready` is metadata-only and must not start Gradle.

## Modpack integration

Material JSON files in `config/cruciblecraft/materials` are loaded at startup.
Files may add a material or override a bundled material with the same id.
Definitions whose composition references a missing material are logged and
skipped, together with alloys that depend on them.

### Material tool durability

Material tool stacks persist material identity only. Integrations must use
`ItemStack.getMaxDamage()` (and the corresponding `ItemStack`/`Item` behavior
methods) for the effective durability and other material-derived properties;
they must not treat raw component values as authoritative material stats.

The item prototype intentionally carries `minecraft:max_damage=1` and
`minecraft:damage=0` sentinels because vanilla `ItemStack.isDamageableItem()`
requires those components. Direct component readers may therefore observe the
sentinel instead of the effective material durability. Compatibility with
third-party displays or inventory tools that bypass `ItemStack.getMaxDamage()`
requires an adapter in that integration.

### KubeJS materials

With KubeJS installed, startup scripts can add materials before CrucibleCraft
registers its generated items. Existing ids cannot be replaced or removed.

```js
CrucibleCraftMaterials.add(event => {
  event.add('zinc', material => {
    material
      .color('#B8C4C2')
      .forms('ingot', 'dust', 'plate', 'rod', 'bolt')
      .meltingPoint(419.5)
      .boilingPoint(907)
      .density(7.14)
      .moltenFluid(true)
  })

  event.add('brass', material => {
    material
      .color('#D5A93E')
      .forms('ingot', 'dust', 'plate', 'rod', 'bolt')
      .meltingPoint(930)
      .boilingPoint(2_000)
      .density(8.5)
      .moltenFluid(true)
      .component('copper', 3)
      .component('zinc', 1)
  })
})
```

Scripts belong in `kubejs/startup_scripts` and require a full restart. Recipe
JSON can be added with KubeJS's normal `ServerEvents.recipes` and
`event.custom(...)` API. The legacy `cruciblecraft:anvil` and
`cruciblecraft:crusher` RecipeTypes remain loadable for addon/datapack
compatibility, but are deprecated for new authored data; new bulk material
recipes should migrate to `cruciblecraft:material_rule`. They may be removed
only after an addon/datapack consumer audit, a documented compatibility window,
and usable migration diagnostics. Legacy anvil recipes support an optional
`material` field; unknown materials simply produce no matching recipe.

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'cruciblecraft:anvil',
    input: 'ingot',
    output: 'plate',
    output_count: 1,
    hits: 6,
    material: 'zinc'
  }).id('kubejs:cruciblecraft/zinc_plate')
})
```

Datapack recipes support NeoForge's standard conditional wrapper, including
`neoforge:mod_loaded`, so compatibility recipes can be omitted when their
dependency is absent.

## EMI

EMI is optional. When installed, CrucibleCraft registers crucible alloying,
anvil working, coke oven, and ceramic mold casting categories.

## Ores and large veins

Every factual, gate-registered `ORE` material has a dedicated stone/deepslate
block pair. Silk Touch preserves the block; normal mining drops one raw ore and
Fortune applies the vanilla ore-drop formula. Raw and crushed ore represent 144
material units (one ingot).

Large-vein authors edit one schema-version-1 document per family under
`src/main/resources/data/cruciblecraft/veins`. The `top`, `bottom`, `between`,
and `spread` fields contain weighted CC material IDs, never block registry
paths. Y range, ellipsoid radii, density, region size, generation chance,
globally unique salt, and provenance live in the same document.
`python tools/build_gt6_veins.py --write` validates factual and registered ore
forms, then creates configured features, placed features, and their aggregate
biome modifier under `src/worldgen_generated/resources`. Runtime states compile
consistently to the stone variant; host adaptation selects stone or deepslate
during placement. The five T2 family declarations are copper, tin, iron, gold,
and tungsten and jointly cover eight materials.

T9 adds `src/main/resources/data/cruciblecraft/worldgen_catalog/ore_veins.json`.
`python tools/build_worldgen_catalog.py --write` expands its T2c-ledger-backed
129-material batch into configured and placed features under the independent
`src/worldgen_catalog_generated/resources` root. The original eight materials plus
the T9 batch cover all 137 registered ores without adding a third host:
the block budget remains 137 × stone/deepslate = 274. The builder also compiles
crude-oil and natural-gas deposits. Each persistent marker records material,
legacy initial/remaining mB, depth-band placement, the replaced host, and an
independent non-depleting T11 production state. Old methane markers migrate to
natural gas. A surface extractor links vertically to the marker and exposes an
extract-only fluid capability; uncaptured gas vents into bounded transient
clouds. Fixed GT6 rows then drive distillation, natural-gas generification, and
the two direct-electric fuel generators.

The placed features run once per candidate chunk, but the feature hashes the
world seed and region coordinates to select exactly one anchor chunk in each
`region_size_chunks` square. Only that anchor may generate, and
`generation_chance` is evaluated from the same stable hash. This avoids
order-dependent duplicate ellipsoids. The implementation writes a bounded
cross-chunk ellipsoid through the world-generation region; it is intentionally
an adaptation to vanilla stone/deepslate rather than GT6's full stone-layer
system. Before placement, each cataloged CrucibleCraft ore
states is converted to its stone or deepslate counterpart according to the
actual replaceable host block. Unknown external states are preserved, while a
missing cataloged counterpart safely skips placement.

The placed-feature JSON intentionally has an empty `placement` list. In
Minecraft 1.21.1, `PlacedFeature` starts with a singleton stream containing the
chunk's supplied origin and folds each placement modifier over it. With no
modifiers, the configured feature is therefore invoked exactly once at that
origin; the region-anchor check inside the feature performs the distribution.
