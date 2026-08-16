# CrucibleCraft

NeoForge 1.21.1 metalworking inspired by GregTech 6 and TerraFirmaCraft.

## Installation

1. Minecraft 1.21.1 with NeoForge ≥ 21.1.243;
2. drop `cruciblecraft-0.1.0-beta.1.jar` into `mods/`;
3. optional: EMI (recipes), Jade (block info), KubeJS (startup material scripts).

Player-facing install, first-play path, save-compatibility and known-issue
information lives in [docs/CrucibleCraft-玩家指南.md](docs/CrucibleCraft-玩家指南.md).
Attribution: [CREDITS.md](CREDITS.md). Changes: [CHANGELOG.md](CHANGELOG.md).

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
uses the pinned source conservation of 200 mB steam into 50 KU (4 mB/KU).
Source row 1302 and `STEAM_PER_EU = 2` derive a nominal `mOutput` of
`24 / 2 = 12 KU`; CrucibleCraft intentionally fixes emission at 12 KU/t.
GT6 instead varies that source engine from 6–24 KU/t by engine state, and that
dynamics replacement remains explicitly deferred. The signed push/return
piston phase is observable through the public
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
- [Current phase: T27 v1.0 portfolio freeze](CrucibleCraft-%E7%AC%AC%E4%BA%94%E9%98%B6%E6%AE%B5%E6%80%BB%E4%BD%93%E8%A7%84%E5%88%92.md)
- [Handover (2026-08-14)](CrucibleCraft-%E4%BA%A4%E6%8E%A5%E8%AF%B4%E6%98%8E.md)
- [Phase 3 closure summary: T13–T19](CrucibleCraft-%E7%AC%AC%E4%B8%89%E9%98%B6%E6%AE%B5%E6%80%BB%E4%BD%93%E8%A7%84%E5%88%92.md)
- [Phase 2 closure summary](CrucibleCraft-%E7%AC%AC%E4%BA%8C%E9%98%B6%E6%AE%B5%E6%80%BB%E4%BD%93%E8%A7%84%E5%88%92.md)
- [T7–T9 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T7-T9.md)
- [T10–T12 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T10-T12.md)
- [T13–T16 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T13-T16.md)
- [T17 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T17.md)
- [T18 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T18.md)
- [T19 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T19.md)
- [T20 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T20.md)
- [T21 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T21.md)
- [T22 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T22.md)
- [T22.5 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T22.5.md)
- [T23 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T23.md)
- [T24 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T24.md)
- [T25 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T25.md)
- [T26 archive](CrucibleCraft-%E9%98%B6%E6%AE%B5%E6%A1%A3%E6%A1%88-T26.md)

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
equal to T16 with zero GT publication delta.

T18 is now `T18_READY`: its 29-kind denominator is classified with zero
unclassified, but only 6 converters are selected; 3 existing transport/motor
kinds are references and 20 kinds remain explicitly deferred. The six
converter acquisition routes have reachable operands and bidirectionally
closed profile/block/item/model/language/loot/tag resources. Converter and
identity adjustments add zero GT rows; 32 RecipeMap ids, 18,875 logical /
16,650 eager / 2,225 lazy rows, and EMI enumeration remain equal to T17.
O-37 is permanently closed as a zero-publication `DESIGN_POLICY`, without a T9
identity migration.

T19 is now `T19_READY`: all 47 canonical cover kinds are classified as
4 implemented, 5 selected, 28 deferred-with-reason, and 10 out of scope, with
zero unclassified. The selected cover batch adds 5 reachable crafting routes
and O-27 adds 25 reachable nonmetal-pipe routes. Those exact 30 generated
vanilla datapack entries remain separate from GT publication: 32 RecipeMap ids,
18,875 logical / 16,650 eager / 2,225 lazy rows, and EMI enumeration over 24
maps are unchanged. The readiness ledger also locks the five-tick schedule,
32,768-pipe route-discovery bound, 256-entry per-pipe route cache, 13-byte
configuration payload, and blocked-transfer conservation. O-20, O-27, and O-28
are closed. Third phase T13–T19 and fourth-phase T20–T26 are complete; T27
is the v1.0 portfolio freeze. T26 delivered public Beta `0.1.0-beta.1` with
a 15-row known-issue ledger (no Beta blockers), O-15 closed, and anvil_bend
reserved as `post_1_0`.
T21 closed on replay-verified Mixer templates (3,414 units, gunpowder 4/4)
rather than the 45,044 input-touch diagnostic rows. T22 closed petroleum
`v1_required` at 0 with three consumed downstream products. T23 shipped
distillation tower, large boiler and 3×3×3 tank. T24 recorded a reproducible
scale baseline with zero blocking findings; T25 closed with selected = 0.
T20's fixed-source
audit observes 40 GT6 large, 75 explicit-small, and one dynamic
small-gem rule; the 129 CC catalog identities classify as 73 SOURCE_DERIVED and
56 explicit DESIGN_POLICY profiles, not as 129 canonical GT6 large veins.

Pre-release cleanup is complete without reopening T19 or the third phase.
Unpublished machine save migrations, parallel Anvil/Crusher recipe APIs, and
unused compatibility entry points are gone; blank/current identity acceptance,
quarantine, future-version handling, MaterialRule Anvil/Crusher execution,
`INTEGRATED_CLIENT`, and the eight-host KINETIC audit remain. The final closure
locks 3,173 English keys / 874 real Chinese translations, 2 RecipeTypes / 2
serializers, and expected suites of 560 JUnit / 120 GameTest / 705 Python tests.
O-15 is closed: v1 critical domains are complete, while 1,566 long-tail
material names are explicit `post_1_0` fallbacks.

Phase 4 has closed O-29 worldgen fidelity, template-classified ordinary chemistry
in T21, petrochemical `v1_required` plus downstream depth in T22, three selected
industrial multiblocks, a reproducible scale baseline, a zero-blocker T25, and
the public Beta gate. T27 classifies the complete portfolio and generates only
the remaining v1-required family cards. CrucibleCraft v1.0 means a complete, stably publishable
GT6-style industrial mainline; G10, nuclear/fusion/plasma, and GT6U are post-1.0
work, not v1 blockers. The current concrete datapack recipe count is 3,243;
logical/eager/lazy publication remains 18,882 / 16,657 / 2,225. See
[`tools/phase4_v1_planning_contract.json`](tools/phase4_v1_planning_contract.json)
for the machine-readable scope and counter definitions.

The `ru_tier_*`, `ku_tier_*`, `eu_tier_*`, and `hu_tier_*` values in
`machine_tiers.json` are stable shared tier-band identities. The `tierBand`
field name describes that role; it does not authorize renaming those ids or
churning persisted machine identity.

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

On Windows GBK consoles, prefix rebuild and `--record` with `PYTHONUTF8=1` so
child-process output does not fail to decode.

The ordered builder list is owned by
`tools/verification_builder_policy.json`. Ordinary CI uses either `rederived`
checks or `compact` receipts and never requires the gitignored GT6 corpus.
`hash-fast` streams the pinned blobs and validates their sizes/SHA-256 values;
`full-replay` re-parses source rows and is invoked explicitly by the
`source-replay` suite. A compact PASS must not be described as source replay,
and one raw corpus has exactly one canonical full-replay owner.

The 2026-08-07 acceptance measurement covered all 50 current builders:
23.248 seconds for the first pass and a 22.996-second warm median, compared
with the former 161-second baseline. A clean-checkout guard that denied every
listed raw/cache/fetched-source path still passed all 50 builders in
27.290 seconds. T13 compact/hash-fast/full-replay measured
0.143/0.823/51.066 seconds respectively; timings are soft observations, while
hash, set, semantic, and currentness drift remain hard failures.

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
