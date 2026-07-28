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

This slice intentionally keeps power adjacency-only. Boiler pressure
explosions and calcification, shaft networks, rotational RU machinery, and
electrical EU/wires/transformers are deferred.

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

## Modpack integration

Material JSON files in `config/cruciblecraft/materials` are loaded at startup.
Files may add a material or override a bundled material with the same id.
Definitions whose composition references a missing material are logged and
skipped, together with alloys that depend on them.

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
`event.custom(...)` API. Anvil recipes support an optional `material` field;
unknown materials simply produce no matching recipe.

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

Copper, tin, iron, gold, zinc, lead, and nickel have dedicated stone and
deepslate ore blocks. Silk Touch preserves the block; normal mining drops one
raw ore and Fortune applies the vanilla ore-drop formula. Raw and crushed ore
represent 144 material units (one ingot), and either form can be smelted or
blasted into one ingot. Copper, iron, and gold use Minecraft's canonical raw
items and vanilla raw-ore cooking recipes; CrucibleCraft only adds cooking
recipes for their crushed forms.

Large veins are configured in
`data/cruciblecraft/worldgen/configured_feature`. The `top`, `bottom`,
`between`, and `spread` properties are weighted lists of objects containing a
block `state` and positive integer `weight`. Other fields set the Y range,
ellipsoid radii, block density, replacement block tag, region size, and region
generation chance. Every configuration also requires a unique integer `salt`;
it separates that family's anchor, chance roll, and center seed from every
other family. Four initial families (copper, tin, iron, and gold) mix all seven
ore materials.

The placed features run once per candidate chunk, but the feature hashes the
world seed and region coordinates to select exactly one anchor chunk in each
`region_size_chunks` square. Only that anchor may generate, and
`generation_chance` is evaluated from the same stable hash. This avoids
order-dependent duplicate ellipsoids. The implementation writes a bounded
cross-chunk ellipsoid through the world-generation region; it is intentionally
an adaptation to vanilla stone/deepslate rather than GT6's full stone-layer
system. Before placement, each of the fourteen cataloged CrucibleCraft ore
states is converted to its stone or deepslate counterpart according to the
actual replaceable host block. Unknown external states are preserved, while a
missing cataloged counterpart safely skips placement.

The placed-feature JSON intentionally has an empty `placement` list. In
Minecraft 1.21.1, `PlacedFeature` starts with a singleton stream containing the
chunk's supplied origin and folds each placement modifier over it. With no
modifiers, the configured feature is therefore invoked exactly once at that
origin; the region-anchor check inside the feature performs the distribution.
