# Tools

Live content uses semantic paths (`tool/assembler/`, `mortar/`, `pipe/`,
`ingot_form/`, wave slugs such as `assembler/compact` and `bath/identity`).
Daily verification is `python tools/verify.py`. Compact / Bath /
ordinary-closure live ledgers, receipts, and currentness sidecars already use
those slugs.

`build_t*.py` builders were deleted. Numbered JSON that remains is either an
earlier independent stage, a frozen v2 snapshot, or the translation map
`semantic_id_map.json`. Those are **historical receipts / wave replay**, not
the current authoring API. Bound runtime ids such as `pipe/extruder` and
`hydrocarbon_materials/` are already semantic. Remaining docs and the full
scan: `docs/current/semantic-naming.md` and
`docs/current/semantic-naming-closeout-checklist.md`.

The rest of this file is the historical GT6 regression and closed-card
runbook. Use it to replay a closed ledger, not to choose the next content
path.

## GT6 recipe regression

## Verification workflow

The layered entry point is:

```text
python tools/verify.py dev
python tools/verify.py integration --profile <name>
python tools/verify.py promotion
python tools/verify.py release
```

- `dev` maps changed paths onto profiles. Unmatched code paths are reported and
  fail closed; they do not escalate to the 110-builder closure.
- Pure Markdown or `docs/history/` edits only run link/index checks.
- `runtime-java` owns ordinary Java/test/resource paths and runs `gradle test`.
  `semantic-generators` owns datagen providers, generated trees, and art /
  resource-gate tools, and runs builders plus double `runData`.
  `recipe-generators` owns semantic recipe compile and `src/recipe_generated`.
  Overlap on a datagen provider path selects runtime-java plus
  semantic-generators.
- `integration --profile` runs that profile's builders, Python modules, and
  declared Gradle tasks. `--if-changed` skips when a diff base exists and the
  profile owns none of the changed paths; without a diff base it still runs.
- `promotion` runs fresh GameTestServer and `runClient` only when a capability
  maturity becomes `player_complete`.
- `release` executes every release profile against the current checkout.

The standard-library runner still owns Python test selection:

```text
python tools/run_python_tests.py --suite fast
python tools/run_python_tests.py --suite affected --path <changed-path>
python tools/run_python_tests.py --suite closure
python tools/run_python_tests.py --suite source-replay
```

- `fast` excludes repository currentness and raw/cache replay tests.
- `affected` uses `python_test_policy.json`; unmatched code paths are reported
  instead of escalating to `closure`. Documentation paths select the docs
  modules.
- `closure` must equal unittest discovery exactly once and reports slow tests.
- `source-replay` is explicit and reports unavailable raw/cache inputs as
  `SKIP`.

The historical phase-closing runner (`run_full_verification.py`) was deleted.
Daily gates are `python tools/verify.py` and the semantic recipe compiler.

`verification_builder_policy.json` is the single ordered owner for all 50
current builder checks. `rederived` rebuilds from ordinary repository inputs;
`compact` validates canonical evidence and tracked hashes without a raw/cache
corpus; `full_replay` runs only through
`python tools/run_python_tests.py --suite source-replay`. Missing replay inputs
are explicit `SKIP`, never a compact pass mislabeled as replay.
Compact receipts now cover the T5 chemical recipe tree, four T11 hydrocarbon
rows, historical T12a evidence, and T13 OP/ITEMGENERATOR denominators in
addition to the raw-replay families listed below. Their ordinary checks verify
builder/input/output hashes and semantic closure; the explicit replay suite
re-derives them from the pinned corpus/source tree.

The builder stage records argv, proof tier, result, and wall time for every
row. Performance regression is a report-only `WARN`; correctness drift remains
a hard failure. The 2026-08-07 acceptance run measured 23.248 seconds for the
first 50-builder pass and warm passes of 22.996, 22.645, and 23.814 seconds
(22.996-second median). That is an 85.7% reduction from the previous
161-second baseline. With every local raw/cache/source path denied, all 50
ordinary builders still passed in 27.290 seconds. T13 measured 0.143 seconds
for compact, 0.823 seconds for hash-fast, and 51.066 seconds for explicit full
replay. These are machine-local observations, not correctness thresholds.
PR CI still runs both datagen passes, Java, production GameTest, and Python
closure; the current committed report binds 538 Java tests, 83 GameTests, and
501 Python tests in one `READY` session.

These rules are non-negotiable: fast/affected results are not closure evidence;
closure tests run once, not once per verifier; caches are process-local,
defensively returned, and clearable; unknown dependencies select closure; test
deletion, stale-ledger trust, and source-gate bypass are not performance
optimizations. `--check` modes are read-only currentness proofs, while `--write`
modes intentionally regenerate owned artifacts.

During development run only `fast` or `affected`, then run one final
`--record`. A child task must not run a full closure that its parent task will
repeat; hand the same resumable session upward instead.

## GT6 ore-dictionary import

`import_gt6_oredict.py` validates the authoritative `oredict/index.json`,
`materials.json`, `prefixes.json`, and `fluid_map.json` dump before normalizing
it. The committed normalized references contain all 2,214 materials, 468
prefixes, and 322 fluid mappings. `gt6_material_activation_policy.json` assigns
all 1,773 canonical stable-ID materials an `ACTIVE` catalog row; the remaining
441 name-only/duplicate records are `OUT_OF_SCOPE`. Normalized records and
runtime registration remain intentionally separate.

The importer writes complete factual forms for 1,110 item-bearing materials.
The other 663 are `metadata_only` because none of their GT6-registered prefixes
maps to a CrucibleCraft prefix. Recipe evidence no longer removes factual forms.

To regenerate with the local dump:

```text
python tools/import_gt6_oredict.py --write
python tools/import_gt6_oredict.py --check
```

CI does not need the gitignored dump:

```text
python tools/import_gt6_oredict.py --check --reference-only
```

The importer preserves exact GT6 U numerators whenever an amount is not an
integral CrucibleCraft unit; runtime decomposition is disabled rather than
rounding the source ratio.
All 468 prefixes are normalized; the importer activates no new runtime prefix
definitions. The 45 already-registered runtime prefixes may create startup
material forms when supported by the activation policy.
Reference-only checking verifies hashes for every indexed material definition,
the exact index order/file set, a preserved structural hash for each of the
12 authored gameplay definitions, and hashes for the form gate, operand
projection, and performance budget.

### Committed form gate and stress budget

```text
python tools/build_gt6_ore_chain.py --write
python tools/build_gt6_ore_chain.py --check
python tools/build_gt6_veins.py --write
python tools/build_gt6_veins.py --check
python tools/build_gt6_veins.py --review
python tools/build_worldgen_catalog.py --write
python tools/build_worldgen_catalog.py --check
python tools/build_worldgen_catalog.py --review
python tools/build_t20_worldgen_source.py --check --reference-only
python tools/build_t20_worldgen_source.py --check --full-replay
python tools/build_t20_worldgen_projection.py --check
python tools/build_t20_readiness.py --check
python tools/build_gt6_material_form_gate.py --write
python tools/build_gt6_material_form_gate.py --check
python tools/build_gt6_material_form_gate.py --review
python tools/run_material_registry_stress.py --write
python tools/run_material_registry_stress.py --check
```

An enabled recipe is selected when it touches at least one L1b `CORE` material;
all mappable item operands in that recipe are projected. The compact committed
operand artifact drives `material_registration_gate.json`, and runtime never
rescans GT6 recipes. The ore-chain builder writes content-addressed concrete
recipes to the independent committed `src/ore_chain_generated/resources` root; its operand
projection is merged into the gate after factual-form validation. The manual
T5 source projection adds exactly seven route-required `plate_gem` /
`tiny_centrifuged_crushed_ore` registrations; those newly mapped prefixes do
not recursively enable unrelated pinned recipe operands. The manual
stress task benchmarks 20,000 metadata-only and 20,000 single-dust additions.
The 4,096 handshake cap remains a hard encode/decode budget; the current
production-scale 1,818-entry payload round-trips below it.

The vein builder owns the semantic author documents in
`src/main/resources/data/cruciblecraft/veins` and the runtime output in
`src/worldgen_generated/resources`. Each schema-version-1 source names four
non-empty weighted material layers plus placement parameters, a globally unique
salt, and provenance. The builder accepts exactly one of `--write`, `--check`,
or `--review`; it validates both factual and gate-registered `ore` forms before
generating all configured features, placed features, and the aggregate biome
modifier.

The T9/T20 builder owns the batch declarations in
`src/main/resources/data/cruciblecraft/worldgen_catalog` and writes the independent
`src/worldgen_catalog_generated/resources` root. T20 independently normalizes
the fixed GT6 source as 40 large facts, 75 explicit-small facts, and one dynamic
random-small rule. The 129-entry T2c target is then classified as 73
`SOURCE_DERIVED` plus 56 explicit `DESIGN_POLICY` profiles; the latter are not
claimed as GT6 parity. Schema/profile v2 stores per-row source kind, field
status, transformations, geometry, distribution, stable id, and stable salt.
The expected builder derives from the fixed source and T2c ledger, while the
production builder consumes only explicit authored rows and requires exact
bidirectional equality. `PLACEHOLDER`, `UNVERIFIED`, and unclassified are all
hard-zero gates. The original 8 worldgen materials plus those 129 entries still
cover all 137 factual, registered ore materials. The same pipeline also compiles
`crude_oil` and `natural_gas` deposits with legacy reserve metadata plus depth,
host, region, chance, and salt into the same configured/placed-feature pipeline.
T11 gives those markers an independent non-depleting production state and
migrates old `methane` markers to `natural_gas`. The compact
`tools/worldgen_catalog_readiness.json` records counts, density arithmetic, host
policy, profile classification, and input hashes; `tools/t20_readiness.json`
separately records closure, fidelity, codec/save boundaries, resource bytes, and
zero RecipeMap publication delta.

Ore-chain artifacts use neutral names because `T2`/`T3` in Java denote the
repository's machine/component tiers, not delivery milestones. The builder
requires every non-crusher stage to have an emitted upstream producer and
records crusher-without-worldgen, dust-without-smelter, and incomplete-route
ledgers in `gt6_ore_chain.json`. Generated raw/crushed-ore furnace recipes remain
an intentional compatibility route grouped as `cruciblecraft:compat_shortcut`.
Their main-output units match the six-stage route; staged byproducts are the
machine chain's additional value. Shortcut counts are derived into the ledger,
but are not evidence that the six-machine route was exercised.

### Component-rule sources

The T3 component rules are authored as compact semantic JSON under
`tools/component_rule_sources`. They compile to runtime
`cruciblecraft:material_rule` recipes in the independent committed
`src/component_rule_generated/resources` root:

```text
python tools/build_component_rules.py
python tools/build_component_rules.py --check
```

The generated manifest locks 28 non-extruder semantic rules plus 2,782 playable
extruder sparse rows. Runtime expansion is 8,141 recipes: assembler 568, bender
638, cutter 651, extruder 2,782, lathe 929, press 1,191, roll bender 438,
rolling mill 336, welder 321, and wire mill 287. The five additional wire-mill
rows are an explicit T6 source-backed wire-form overlay. It also locks unit
conservation, recipe/shadow signature digests (zero shadows), the 10,000-recipe
budget, 31 reusable shapes, and all 62 extruder-template classifications
(20 playable, 42 skipped, zero unclassified). Runtime expansion remains owned
solely by `MaterialRuleExpansion` and `GTRecipeMapLoader`; CI needs no
gitignored GT6 replay artifact.

The legacy `cruciblecraft:anvil` and `cruciblecraft:crusher` RecipeTypes and
serializers remain registered for addon/datapack compatibility and are adapted
into the same runtime rule path. They are deprecated for new authored data;
use `cruciblecraft:material_rule`. Removal is blocked until consumer auditing,
a documented compatibility window, and migration diagnostics are complete.

Topology fallbacks retain reviewed gameplay defaults when GT6 has no directly
expressible row. When a normalized GT6 row can be represented within the target
machine limits, its duration, EU/t, chance outputs, and ordered stage
byproducts are selected deterministically and identified by source hash.

### Material L1 rule provenance

`gt6_l1b_layer_select.py` declares every classification rule in
`RULE_DEFINITIONS`. Each rule has a `basis`:

- `verified`: protected by a direct data invariant/query;
- `inferred`: heuristic, therefore prioritized for a falsifier query;
- `external`: intentionally waits on a human/T0b input.

Rows carry `rule_basis`, `rule_basis_reason`, `rule_falsifier`, and
`rule_falsifier_test`. The layer report counts inferred rules with no falsifier
separately; review is organized by rule blast radius, not by manually checking
every material. IDs 1000..1180 complete periodic Z=100..118 (plus isotope
Flerovium298); IDs 1181..7999 are the verified fictional-material shelf.
L1a cross-checks element/isotope IDs against `protons`, `neutrons`,
`electrons`, and `mass`. Those fields are not identity classifiers by
themselves: compounds and fictional materials also carry aggregate nuclear
totals.
Residual hidden rows are emitted to `gt6_l1b_needs_review.json`, not silently
dropped.

The L1b closure gate reports `edge_type × target_layer`. A CORE `components`
edge promotes its target transitively to the strongest consumer layer until a
fixed point is reached. Missing targets or explicitly excluded promotions are
hard failures. `targets.*` are written to `gt6_l1b_degraded_paths.json` with a
T2/T3 prerequisite stage; out-of-CORE `byproducts` are marked for omission by
L3 and recorded in `gt6_l1b_byproduct_stripping.json`. External policy
rules may declare `external_dependency`, and the report groups their hit
counts by the input they await.

Materials with `source_id < 0` use `name:<nameInternal>` identity in L1a and
can never be appearance lookup keys. L1a reports stable/name-only edge
directions separately, so name-only target nodes cannot hide stable processing
gaps. L1b retains all name-only rows in the dropped artifact and compatibility
diagnostics, but every tier/rule/conflict/histogram summary declares
`excludes_sentinels: true` and covers only the 1773 stable-ID materials.

### Recipe template accounting

`gt6_recipe_templates.py` is the exploratory extractor for mixer, bath, and
shredder. Extruder uses the dedicated lossless extractor because shape and
form are many-to-many:

```text
python tools/gt6_recipe_templates.py
python tools/gt6_extruder_templates.py
python tools/gt6_extruder_templates.py --verify
python tools/gt6_tag_domain_analysis.py
python tools/build_gt6_generation_bits.py
```

`component_selector_policy.json` locks the machine-specific selector decision:
extruder uses one concrete shape plus an explicit sparse support relation;
bath retains its T2 byproduct selector; shredder retains its T2
`processing_target` selector; mixer is deferred to T5. Shape selection is not a
generic component-map pattern.

### T4 tool readiness

`t4_tool_policy.json` separates imported GT6 facts from CrucibleCraft porting
strategy. `HAS_TOOL_STATS` defines the material domain; `tool.types` is a 0–3
ordinal eligibility level, while durability, quality, and speed are values only.
The policy also fixes the component-driven item architecture, zero-unindexed
recipe budget, mining-tier compression, durability saturation, and explicit
damage-based catalyst transaction requirement.

```text
python tools/build_t4_tool_readiness.py
python tools/build_t4_tool_readiness.py --check
```

The generated `t4_tool_readiness.json` classifies all 546 candidates separately
for each of the 13 tools. `closure.unclassified` must remain zero. Eligibility,
material multisets, handle counts, catalysts, and route precedence come from
the pinned GT6 pattern for that tool; there is no shared metal/gem/rod template.
The exact `stone` identity is a source-backed Pickaxe exception, while
`MT.Wood.NOT` is represented by exact `wood` identity rather than the broader
`PROPERTIES.WOOD` tag. Every identity literal carries a reason and source
revision, and distinct `material.is` IDs are ledgered. Rule expressions spell
out `has_form` for factual metadata and `has_registered` for obtainable forms;
the runtime loads the registration gate once per recipe reload.

Source eligibility and current route closure are separate audited domains.
`strategy_projections.eligibility_route_gaps` records every source-eligible
tool/material pair that lacks a currently registered manufacturing route,
including the 208-material Pickaxe gap. The policy's
`eligibility_predicate_sources` section pins every types/tag/quality predicate,
including the Wrench COATED and Screwdriver BOUNCY/STRETCHY asymmetries, to a
GT6 revision and line-level reason.

The policy also records the measured 8,141 / 3,452 / 11,593 count projection
and reload/index/lookup budgets. The lookup benchmark runs only during explicit
GameTest verification, not every production reload. The old unconstrained heap
delta was removed because it did not measure retained memory. Runtime lookup
indexes consumed inputs as primary discriminators; common `WEAR` and
`PRESERVE` catalysts are fallback keys only, preventing tool-pattern and
File/Hammer inputs from flooding every assembler query with thousands of
candidates.

### T5.5/T6 electrical readiness

`gt6_electrical_source.json` pins the direct electric-wire registrations and
source hashes at GT6 revision
`3703e40308c8c030763fd6297dea8b210d2a77b1`. Electrical eligibility comes from
explicit `addElectricWires` calls, not material names, `ITEMGENERATOR.WIRES`, or
generic OreDict wire/cable registrations. The calls define maximum voltage,
gauge-scaled amperage, and integral EU loss per traversed block. They do not
define physical resistance.

The importer projects those exact registration values into
`gt6_metadata.electrical_by_specification`. The readiness ledger classifies
every raw and live material, checks the acceptance conductors, derives the live
five-gauge cable/recipe domain, and quantifies material-specific blockstate
growth:

```text
python tools/build_t6_electrical_readiness.py
python tools/build_t6_electrical_readiness.py --check
python -m unittest tools.tests.test_build_t6_electrical_readiness
```

An installed official checkout can additionally verify the extracted source
files:

```text
python tools/build_t6_electrical_readiness.py --check --source-root <gregtech6-checkout>
```

`t6_electrical_policy.json` closes the runtime architecture decisions:
exact source voltage ceilings, source EU loss per traversed block, GT6's
burn-counter overload behavior, stateless per-injection traversal, exact
1/2/4/8/12 cable gauges, and one generated material-specific block per
source-backed form. The runtime implements 115 insulated cable blocks and 29
bare `wireGt01` blocks (144 blocks / 9,216 logical states), loaded-only stable
DFS, exact signed segment loss, persistent 16-hit/512-tick burn state, safe-tick
fire replacement, last-tick source-flagged contact damage, and data-driven
`ANY.Rubber` insulation. Cable telemetry uses position-phased client sync rather
than per-block per-tick updates; connection shapes are precomputed for all 64
masks. Energy commits re-simulate consumers serially, extract source packets
first, contain external endpoint failures as bounded dissipation, and reuse only
the fresh same-tick cable plan selected by the final preflight. The generated
gate is `READY` and source-guards these T6a–T6d contracts; full
unit/resource/GameTest execution remains owned by the full verification report.

### T5a chemical readiness

`t5_chemical_policy.json` pins `Loader_Recipes_Decomp.java` at GT6 revision
`3703e40308c8c030763fd6297dea8b210d2a77b1`. Pinned replay classifies 17 of the
original 162 terminal-dust rows as GT6 source dead-ends because all 720,841
recipes expose only packaging loops for them. The generated readiness ledger
therefore keeps the 145 executable terminal-dust debts, 110 crusher chemical
debts, 110 byproduct-only debts, and all 119 non-molten fluid candidates
separate:

```text
python tools/build_t5_chemical_readiness.py
python tools/build_t5_chemical_readiness.py --check
python -m unittest tools.tests.test_build_t5_chemical_readiness
```

The 224-material chemical union records composition, source tags, the exact
loader conjunction, the CrucibleCraft `no_decompose` quarantine, destination
maps, and replay status. Every required map is hashed from the fixed dump;
loader-only fluid candidates not named by the selected routes remain classified
but are not registered.

`build_t5_distillery_projection.py` separately classifies every one of the
1,517 pinned distillery rows before recipe projection. One row is currently
projectable (`water -> water_distilled`); 1,516 rows are retained with explicit
identity gaps rather than silently dropped:

```text
python tools/build_t5_distillery_projection.py
python tools/build_t5_distillery_projection.py --check
python -m unittest tools.tests.test_build_t5_distillery_projection
```

`build_t5_chemical_recipes.py` projects only source-backed,
registration-closed recipes into `src/t5_chemical_generated/resources`; it also
writes the closed non-molten fluid registry and
`t5_chemical_recipe_manifest.json`:

```text
python tools/build_t5_chemical_recipes.py
python tools/build_t5_chemical_recipes.py --check
python -m unittest tools.tests.test_build_t5_chemical_recipes
```

The current bounded projection is 152 recipes across assembler, autoclave,
bath, centrifuge, compressor, distillery, drying, electrolyzer, mixer, and
smelter, with 15 registered chemical fluids. It gives a traceable live route to
every 145 / 145 terminal dust in the executable denominator, six source-backed
fluid-closure producers, and one distillery vertical. The 17 source dead-ends
remain explicit ledger rows and never become CrucibleCraft-only chemical
recipes.

### T5.5 machine-crafting readiness

`machine_crafting_policy.json` pins the official
`GregTech6/gregtech6` Java source at revision
`3703e40308c8c030763fd6297dea8b210d2a77b1`. The generated readiness ledger
derives all 23 configured machine IDs from `ModProcessingMachines` and the
placeholder calls in `ModRecipeProvider`; it separately discovers the seven
existing `bronze_*.json` main-resource crafts. The recipe-map dump is not used
as machine-block crafting evidence because those shaped registrations live in
`Loader_MultiTileEntities.java`.

```text
python tools/build_machine_crafting_readiness.py
python tools/build_machine_crafting_readiness.py --check
python -m unittest tools.tests.test_build_machine_crafting_readiness
```

To reproduce the compact official-source evidence without vendoring the GT6
repository:

```text
python tools/build_machine_crafting_readiness.py --fetch-source build/gt6-machine-crafting-source
python tools/build_machine_crafting_readiness.py --check --verify-source build/gt6-machine-crafting-source
```

T5.5 authorizes no placeholder replacement. Every configured row remains
blocked on an explicit source-identity, tier-collapse, or exact component
mapping prerequisite; `unclassified` must remain zero.

### T12a machine source and scaling readiness

`t12_machine_policy.json` supersedes the historical T5.5
`tier_collapsed_pending_t6` owner without rewriting that artifact. The generated
`t12a_machine_readiness.json` ledger covers 25 / 25 processing kinds and 12 / 12
non-spec kinds, pins RU/KU/EU/HU identities and the first three
Centrifuge/Sifter/Electrolyzer tiers, and records source-backed scaling
expectations independently from production code. Its
`T12A_PREPROJECTION_READY` load state is immutable historical evidence, not the
current phase readiness. Missing machine casings, runtime kind/tier migration,
RU topology, and the shared multiblock validator were the original fail-closed
T12b-e boundary.

```text
.\gradlew.bat t12CapacityMatcherBenchmark --no-daemon
python tools/build_t12_machine_readiness.py
python tools/build_t12_machine_readiness.py --check
python -m unittest tools.tests.test_build_t12_machine_readiness
```

To replay all seven official source blobs and their symbol/line anchors:

```text
python tools/build_t12_machine_readiness.py --fetch-source build/t12-gt6-source
python tools/build_t12_machine_readiness.py --check --verify-source build/t12-gt6-source
```

### T12 closure readiness

`build_t12_closure_readiness.py` consumes the immutable historical
`t12a_machine_readiness.json` source/preprojection ledger and verifies the live
Kind/Tier catalog, v2 state migration, RU/KU/EU runtime identities,
Axle/Gearbox route, two JSON multiblocks, matcher budget and the
zero-publication load account. It emits `T12_READY`; the full verification
runner checks the historical T12a artifact for drift without calling it current.

```text
python tools/build_t12_closure_readiness.py
python tools/build_t12_closure_readiness.py --check
python -m unittest tools.tests.test_build_t12_closure_readiness
```

### T13 canonical denominators

T13 pins the official revision tree as a 1,229-Java-blob manifest and derives
seven independently tested denominator tables. Ordinary checks are offline;
the optional source replay downloads/validates the full selected Java source
set under `build/t13-gt6-source`.

```text
python tools/build_t13_recipe_map_denominator.py --check --reference-only
python tools/build_t13_recipe_map_denominator.py --check --hash-fast
python tools/build_t13_prefix_domain_denominators.py --check --reference-only
python tools/build_t13_machine_energy_denominators.py --check
python tools/build_t13_cover_multiblock_denominators.py --check
python tools/build_t13_denominator_readiness.py --check
python -m unittest tools.tests.test_build_t13_recipe_map_denominator tools.tests.test_build_t13_prefix_domain_denominators tools.tests.test_build_t13_machine_energy_denominators tools.tests.test_build_t13_cover_multiblock_denominators tools.tests.test_build_t13_denominator_readiness
```

Full fixed-source replay:

```text
python tools/build_t13_recipe_map_denominator.py --fetch-source build/t13-gt6-source --write-source-inventory
python tools/build_t13_recipe_map_denominator.py --check --full-replay
python tools/build_t13_recipe_map_denominator.py --check --reference-only --verify-source build/t13-gt6-source
```

Compact validates the canonical 95-row table, policy/disposition closure,
builder/policy/tree/inventory hashes, and the externally anchored replay receipt
in `t13_denominator_manifest.json`. Hash-fast additionally streams each map once
for SHA-256 and byte-size equality. Only full-replay parses every recipe array
and re-derives all 720,841 rows.

### T14 Extruder materialization and load projection

T14 replaces 2,782 flattened Extruder recipe resources with 20 compact authored
rules while preserving the complete logical set. The equivalence builder checks
full-field expected/legacy/compact equality. The benchmark builder verifies the
predeclared immediate/on-demand/Hybrid decision from committed 1x/5x/20x
measurements; production uses Hybrid with 557 eager rows, 2,225 lazy rows, and a
512-row per-epoch cache ceiling.

Every T15-T19 recipe family must provide a
`recipe_load_projection.schema.json` input. The evaluator records authored,
logical, eager, lazy, cache, sync, server/client reload/index, retained memory,
allocation, lookup p95, and candidate count separately. Soft excess is reported;
hard excess fails. Zero-workload examples for every downstream phase are
committed under `tools/tests/fixtures`.

```text
python tools/build_t14_extruder_equivalence.py --check
python tools/build_t14_recipe_load_benchmark.py --check
python tools/recipe_load_projection.py tools/t14_extruder_load_projection_input.json
python tools/build_t14_readiness.py --check
python -m unittest tools.tests.test_build_t14_extruder_equivalence tools.tests.test_build_t14_recipe_load_benchmark tools.tests.test_recipe_load_projection tools.tests.test_build_t14_readiness
```

### T15 residual readiness

`t15_readiness.json` is the only current T15 readiness artifact and now reports
`T15_READY` with T15a-e complete and no pending gate. It keeps the frozen T12a
preprojection artifact in a separate immutable-history section, while its
currentness chain starts from `T12_READY`, `T13_READY`, and `T14_READY` and
binds every completed-stage source contract.

`t15_matcher_boundary.json` is the T15e authority. Its builder derives 15 physical item/fluid ports,
two energy ports, and one controller from the live
Large Centrifuge structure JSON. It independently parses
`ModProcessingMachines.CENTRIFUGE` as one item input, one fluid input, six item
outputs, and two fluid outputs. All physical ports bridge the same host
inventory, so the item matcher receives one item matcher supply rather than a
per-port expansion; the presence-only cap of 12 is not triggered. The artifact
also preserves the historical expected-16 error audit: the old projection
counted every non-energy cell as a port and failed to subtract the controller.
The 12/16/32/64 consuming benchmark and 16/32/64 presence-cap rejection
scenarios remain current, within budget, and do not justify a matcher rewrite.

```text
python tools/build_t12_machine_readiness.py --check
python tools/build_t15_matcher_boundary.py --check
python tools/build_t15_readiness.py
python tools/build_t15_readiness.py --check
python -m unittest tools.tests.test_build_t15_matcher_boundary tools.tests.test_build_t15_readiness
```

### T16 RU/KU readiness

`t16_machine_denominator.json` closes the 20-kind RU/KU owner denominator at
`unclassified = 0`: selected 5, preimplemented 2, deferred 13, plus 20 explicit
tier-4 deferrals. The selected set is five kinds across bronze, steel, and
titanium, for 15 runtime variants; `T16_READY` does not claim that all 20 kinds
are implemented.

`t16_machine_acquisition.json` proves all 15 selected variants have registered
survival crafting results and reachable operands. Blank/current identities are
accepted while kind/tier/material/energy mismatches quarantine. T16 adds only
vanilla shaped crafting acquisition recipes.

`t16_load_projection_input.json` and `t16_load_projection.json` are the formal
delivery-T16 zero-workload account. Authored, logical, eager, lazy, cache, sync,
and every runtime interval are zero and `PASS`. The independent publication
baseline locks 32 RecipeMap ids, 18,875 logical / 16,650 eager / 2,225 lazy
rows, and exact EMI enumeration over 24 configured maps; GT publication delta
is 0.

```text
python tools/build_t16_machine_denominator.py --check
python tools/build_t16_machine_acquisition.py --check
python tools/build_t16_readiness.py
python tools/build_t16_readiness.py --check
python -m unittest tools.tests.test_build_t16_machine_denominator tools.tests.test_build_t16_machine_acquisition tools.tests.test_recipe_load_projection tools.tests.test_build_t16_readiness
```

### T17 HU/EU readiness

`t17_machine_denominator.json` closes the 28-kind HU/EU owner denominator at
`unclassified = 0`: selected 3, one preimplemented Electrolyzer reference, and
deferred 24. The selected Distillery, Drying, and Smelter kinds each implement
Heat_T tiers 1–3 for nine runtime variants. Ten Heat tier-4 and 32 Electric
tier-4/5 variant slots remain explicitly deferred; `T17_READY` does not claim
that all 28 kinds are implemented.

`t17_machine_acquisition.json` proves all nine selected HU variants have
registered survival crafting results and reachable operands. Current-only
identity and quarantine rules apply, the existing three Electrolyzer variants
remain reference evidence, and T17 adds only vanilla shaped crafting
acquisition recipes. Extruder and Compressor remain explicit deferred
dispositions; deferral is not reported as implementation.

`t17_load_projection_input.json` and `t17_load_projection.json` are the formal
delivery-T17 zero-workload account. Authored, logical, eager, lazy, cache, sync,
and every runtime interval are zero and `PASS`. The T17 publication baseline is
bidirectionally locked to T16: 32 RecipeMap stable ids, 18,875 logical / 16,650
eager / 2,225 lazy rows, and exact EMI enumeration over 24 configured maps.
The only recipe delta is nine vanilla crafting rows; GT publication delta is
zero.

```text
python tools/build_t17_machine_denominator.py --check
python tools/build_t17_machine_acquisition.py --check
python tools/build_t17_readiness.py
python tools/build_t17_readiness.py --check
python -m unittest tools.tests.test_build_t17_machine_denominator tools.tests.test_build_t17_machine_acquisition tools.tests.test_recipe_load_projection tools.tests.test_build_t17_readiness
```

### T18 energy-converter readiness

`t18_machine_energy_denominator.json` closes 29 machine kinds plus STEAM/AU at
`unclassified = 0`: selected 6, three preimplemented references, and deferred
20. `T18_READY` does not claim that all 29 kinds are implemented.

`t18_converter_acquisition.json` independently binds the six complete converter
profiles to six vanilla shaped recipes, registered blocks/items, reachable
operand producers, blockstates, block/item models, bilingual language rows,
loot, and the pickaxe tag. Every component set is bidirectional and
`unreachable = 0`.

`t18_load_projection_input.json` and `t18_load_projection.json` are the formal
delivery-T18 zero-workload account. Converter profile/tier/identity changes add
zero GT RecipeMap rows. The T18 publication baseline is bidirectionally locked
to T17: 32 RecipeMap stable ids, 18,875 logical / 16,650 eager / 2,225 lazy
rows, and exact EMI enumeration over 24 configured maps.

O-37 is recorded separately from converter publication. Full fixed-revision
Java-tree replay found zero direct binding candidates between
`liquid_medium_oil` and material 9852, so the permanent resolution is
`O37_CLOSED_PERMANENT_DESIGN_POLICY`; material 9852 remains source-layer-only,
T9 identity migration is `NONE`, and publication delta is zero.

```text
python tools/build_t18_machine_energy_denominator.py --check
python tools/build_t18_converter_acquisition.py --check
python tools/build_t18_o37_identity_projection.py --check --reference-only
python tools/recipe_load_projection.py tools/t18_load_projection_input.json --output tools/t18_load_projection.json
python tools/build_t18_readiness.py
python tools/build_t18_readiness.py --check
python -m unittest tools.tests.test_build_t18_machine_energy_denominator tools.tests.test_build_t18_converter_acquisition tools.tests.test_build_t18_o37_identity_projection tools.tests.test_recipe_load_projection tools.tests.test_build_t18_readiness
```

### T19 cover and pipe closure

`t19_readiness.json` closes T19a–d and final readiness at `T19_READY`. The
canonical cover denominator remains 47 = 4 implemented + 5 selected + 28
deferred + 10 out of scope, with zero unclassified. Cover acquisition contributes
5 vanilla crafting rows and O-27 contributes 25; all 30 are derived from the
actual generated set and remain separate from GT RecipeMap publication.

`t19_load_projection_input.json` uses recipe-load schema 2. It records 30
`vanilla_datapack_entries`, zero GT authored/logical/publication/sync rows, and
therefore cannot mislabel crafting as GT. The publication baseline locks 32
RecipeMap ids, 18,875 logical / 16,650 eager / 2,225 lazy rows, and EMI
enumeration over 24 maps.

The same readiness artifact records bounded runtime costs: position-phased
five-tick scheduling, at most 32,768 visited pipes per route discovery, 256
cached route keys per item pipe, six cover slots, a 768-character summary, and
a 13-byte cover-configuration payload. Production GameTests bind retriever,
robot-arm, and pressure-valve blocked retention/conservation. O-20, O-27, and
O-28 have no pending work.

The pre-release closure also locks 3,167 English keys, 366 real Chinese
translations, visible debt 2,801 (including 1,774 material names), exactly two
active RecipeTypes and two serializers (`gt_recipe`, `material_rule`), and the
538 JUnit / 83 GameTest / 501 Python suite expectations. All readiness reports
bind to the snapshot full-verification report with no remaining pending gate.

```text
python tools/build_t19_cover_denominator.py --check
python tools/build_t19_cover_acquisition.py --check
python tools/build_t19_pipe_acquisition.py --check
python tools/recipe_load_projection.py tools/t19_load_projection_input.json --output tools/t19_load_projection.json
python tools/build_t19_readiness.py --check
python -m unittest tools.tests.test_build_t19_cover_denominator tools.tests.test_build_t19_cover_acquisition tools.tests.test_build_t19_pipe_acquisition tools.tests.test_recipe_load_projection tools.tests.test_build_t19_readiness
```

### T21 Mixer template denominator

T21 is in progress: the evidence below exists, but Beta necessity
re-validation, isolated load measurement, the dedicated GameTest server run,
and the final full verification are still open; the drafted `T21_READY` is
not closure evidence.

T21 keeps the 224-material classification and Carbon electrolysis calibration,
but `t21_source_denominator.json` is diagnostic-only. `analyze_map_shape.py`
first probes the raw Mixer schema, resolves GT meta ids to material axes, and
uses non-overlapping leave-one-out groups. The resulting 64,245 rows are
partitioned into 3,414 authored units: 2,628 material matrices, 103 enumerated
families, and 683 opaque units.

`gt6_mixer_templates.py` assigns content-addressed ids, explicit sparse support,
and total source-index membership. Compact and full replay both require zero
missing/extra rows and zero unassigned/duplicate membership.
`t21_template_denominator.json` combines those templates with exact singleton
units for the other nine T5 maps and classifies necessity from explicit Beta
seeds. The sole v1-required template is the four-row gunpowder family; it is
published eagerly with a passing family load projection.

```text
python tools/gt6_mixer_templates.py --check --reference-only
python tools/gt6_mixer_templates.py --check --full-replay
python tools/build_t21_template_denominator.py --check --reference-only
python tools/build_t21_template_denominator.py --check --full-replay
python tools/build_t21_mixer_gunpowder.py --check --full-replay
python tools/recipe_load_projection.py tools/t21_load_projection_input.json --output tools/t21_load_projection.json
python tools/build_t21_readiness.py --check
```

Extruder v5 uses one template per concrete shape (31 normal + 31 low-heat);
material/external identity, input/output form, counts, EU, and duration are
stored in a sparse support relation. Axes are stored once per template and
support rows are positional arrays of template-local integer indexes; support
rows do not repeat SHA-256 IDs or complete input/output stacks. Stack metas are
rebound to the stored form skeletons during replay. Pure-material EU is derived
from verified `PROCESSING.EXTRUDABLE[_SIMPLE]` domains (plus the Graphite
exception).
Repeated constant parameter buckets use exact
`PROCESSING tag ∩ registered prefix domains ± exceptions` expressions instead
of material ID lists when that representation is smaller. Both materials and
prefixes are content-hashed dependencies. The extractor uses stable IDs and
hard-fails unless replay reproduces the pinned 325595-recipe multiset with zero
missing/extra rows. `gt6_tag_domain_analysis.py` writes the reproducible
namespace/domain audit to `gt6_tag_domain_report.json`.

The expanded `gt6_extruder_templates_v5.json` is a gitignored local replay
cache. The committed `gt6_extruder_templates_index_v5.json` and
`gt6_extruder_templates_report.json` are the compact T3 builder inputs and are
checked for internal count, identity, policy, partition, and replay consistency
without the expanded cache. `--verify` performs full replay only when both the
expanded cache and authoritative dump are installed. Otherwise it exits
successfully with an explicit `SKIP` message; that message does not claim a full
replay, and ordinary CI still verifies compact evidence through the builder and
unit tests.

`build_gt6_generation_bits.py` writes `gt6_generation_bits.json` v3. It retains
the 13 exact `ITEMGENERATOR` single-tag domains covering 70 prefixes, then
searches the 126 stable-material tags with union, intersection, and difference.
One- and two-tag domains are searched globally; three-tag searches first rank
direct target anchors and residual discriminators to keep the search bounded.
The exception budget is `max(16, ceil(5% * target size))`. The pinned dump has
155 accepted rules (110 exact, 45 with explicit exceptions). `ore` resolves as
`ORES & COMMON_ORE`, and `plate` resolves with 25 exceptions. Generic `block`
and deprecated `wire` are classified as compatibility absorption, while
`wireGt01` is a replay-verified 32-material explicit domain. No core prefix
remains unresolved.

The same artifact connects `ITEMGENERATOR.MOLTEN` to the 200 stable
`molten.*` fluid-map materials with 16 explicit additions and reports the two
name-only fluid records separately. It also records that
`MOLTEN | LIQUID` is worse on the pinned dump (48 exceptions), rather than
silently adopting the proposed union. All equality and replay checks use
stable `nameInternal` after requiring `material.id >= 0`; the 441 `id=-1`
records are diagnostic only.

Coverage remains on expanded-recipe accounting until
mixer, extruder, bath, and shredder all have replay-verified indexes;
verdicts are not keyed to templates yet.

The committed baseline is a reviewed artifact. CI only recomputes the current
snapshot and compares it with that artifact; it never updates the baseline.
The normalized reference is serialized without indentation to avoid doubling
the size of its exact expanded rows; its embedded semantic fingerprint is
independent of JSON whitespace.

## CI-equivalent check

The committed-evidence gate is
`python tools/run_full_verification.py --check-ready`; it runs no external test
commands. The compatible full read-only closure remains
`python tools/run_full_verification.py --check`. The expansion below documents
the stages owned by the full orchestrator; do not run it and a separate Python
closure as two independent closure proofs.

```text
# The 50 ordered builder commands come from verification_builder_policy.json.
python tools/build_gt6_material_form_gate.py --check --reference-only
python tools/build_gt6_ore_chain.py --check --reference-only
python tools/build_t5_chemical_readiness.py --check --reference-only
python tools/build_t5_chemical_recipes.py --check --reference-only
python tools/build_t5_distillery_projection.py --check --reference-only
python tools/build_t11_preflight_projection.py --check --reference-only
python tools/build_t11_hydrocarbon_recipes.py --check --reference-only
python tools/build_t12_machine_readiness.py --check --reference-only
python tools/build_t13_recipe_map_denominator.py --check --reference-only
python tools/build_t13_prefix_domain_denominators.py --check --reference-only
./gradlew runData
./gradlew runData
./gradlew test
./gradlew runGameTestServer
python tools/run_python_tests.py --suite closure
python tools/verify_full_verification_report.py --check
```

The comparator's ordinary `--check` validates current CC fingerprints,
reachability, constants, compact GT6 reference metadata, roadmap accounting,
expectations, and their evidence digests against the reviewed baseline. It
prints an explicit full-replay skip when no local expanded cache is used. Run
`python tools/compare_gt6_recipes.py --check --full-replay` to require a full
GT6 replay; this fails with an actionable cache/dump restoration message when
the required local data is absent. `build_gt6_ore_chain.py --check` follows the
same model: it validates committed concrete recipes and compact ledgers without
the normalized cache, while write/review regeneration still requires that
cache.

This checks all of the following:

- the normalized GT reference fingerprint matches the reviewed baseline;
- every CC/GT comparison row has an explicit verdict in
  `gt6_recipe_expectations.json`;
- all normalized CC recipes are covered, including source-derived GT6
  furnace comparisons and explicitly out-of-scope 1.21 blasting recipes;
- `EXACT` rows have zero deltas and `INTENTIONAL` rows retain their pinned
  deltas;
- each family is partitioned into exactly one of `EXACT`, `FORM_PATH`,
  `SEMANTIC`, or `NONE`;
- all 95 active and empty GT maps have a planning status, reason, and pinned
  recipe count;
- every process-constant group has a verdict and authoritative GT6 source
  evidence (or a specific semantic/data gap);
- hand-authored recipe, programmatic process, energy constant, and item
  reachability snapshots match the baseline;
- the ceramic crucible, bronze ingot, and steel ingot progression nodes remain
  reachable.

## Explicit baseline update

After reviewing the generated report and policy changes:

```text
python tools/compare_gt6_recipes.py --update-baseline --reference-only
```

Commit the baseline update in the same review as the intentional source or
expectation change. Do not run this command in CI.

## Refreshing the GT6 reference

Place the local dump at `gt6_dump/gt6_recipe_dump`, replace the legacy values in
`gt6_reference_metadata.json` with the exact GT6 version, configuration digest,
dump-tool version, and refreshed name-only compatibility-surface metrics, then
run:

```text
python tools/compare_gt6_recipes.py --write-reference
python tools/compare_gt6_recipes.py --write-automated-expectations
python tools/compare_gt6_recipes.py --write-roadmap-template
python tools/compare_gt6_recipes.py --update-baseline
python tools/compare_gt6_recipes.py --write-report
```

Review `gt6_recipe_compare_report.json`, update expectations or roadmap entries
explicitly, then update the baseline. A changed reference fingerprint is
reported separately from CC recipe drift.

`gt6_recipe_normalized_reference.json` and `gt6_recipe_compare_report.json` are
gitignored local caches. `local_artifact_manifest.json` records their measured
byte sizes, SHA-256 digests, provenance, and rebuild commands. The report is
human/full-replay evidence; the baseline, expectations, roadmap, and manifest
are the committed compact evidence used by ordinary CI.

### Expectations are inputs, never regenerated outputs

`gt6_recipe_expectations.json` is an audited input (same class of bug as
treating baseline and report as the same artifact). Every row records whether
its evidence was human-reviewed or deterministically classified. Automated
classification never claims human review.

- `--write-expectations-template` writes only
  `gt6_recipe_expectations_suggested.json` for review/merge.
- `--append-missing-expectations` may add **absent** keys into the committed
  file; it never replaces an existing entry.
- `--write-automated-expectations` explicitly replaces the file with
  deterministic classifications carrying source/method/evidence digests.
- Hand-edited verdicts (e.g. bronze melting delta, iron crush OUT_OF_SCOPE)
  survive reference refreshes because regeneration cannot clobber them.

Discipline target (materials and recipes share this): **rules cover the bulk;
human verdicts only cover exceptions / needs_review**. Auto-filled `TODO_PORT`
rows that merely satisfy the schema are not “reviewed.” Until classification
rules replace bulk autofill, treat suggested dumps as merge fodder — not as
proof that someone looked at every row.

When only source-derived families change (currently `mc.recipe.furnace`), refresh
them without requiring the gitignored dump:

```text
python tools/compare_gt6_recipes.py --reference-only --refresh-source-reference
python tools/compare_gt6_recipes.py --reference-only --write-expectations-template
python tools/compare_gt6_recipes.py --reference-only --write-roadmap-template
python tools/compare_gt6_recipes.py --reference-only --write-report
```

Review `gt6_recipe_expectations_suggested.json` (and roadmap) before appending
any missing keys or updating the baseline. The roadmap generator preserves
reviewed statuses and reasons while refreshing pinned map counts.

The full dump, normalized reference, expanded extruder document, and
human-oriented comparison report are local artifacts. The compact baseline,
expectations, roadmap, extruder index/report, selector policy, artifact
manifest, scripts, fixtures, and tests are version-controlled.

Deleting a large artifact from the current tree does not remove its blob from
older Git commits. A future push can therefore still require a clean new root,
an explicit history migration, or Git LFS; ordinary follow-up deletion alone
does not shrink existing history.

## T2 ore-chain verification

`MaterialChainIntegrationTest` expands the production `MaterialChainRules` against the
imported runtime material index and verifies every configured map, water input,
ordered byproducts, the 6×16-unit anvil yield, and raw-ore-to-ingot
reachability. `CrucibleCraftGameTests` runs the production block-world suite on
the dedicated NeoForge GameTest server. It covers live concrete recipes,
Crusher pause/rollback/resume and declared-duration completion, water capability
policy, Firebox-to-Smelter HEAT, placed copper/tin/iron/gold chains, real
tungsten configured-feature placement and loot through its full machine chain,
atomic cable assembly plus NBT/update-tag persistence, and all ten placed T3
machines resolving and advancing real loaded recipes.

## T3 component-data verification

`ComponentRuleDataTest` validates the normalized GT6 prefix amount and
registered-material evidence for every activated component prefix, expands the
committed compact rules through the production projector, and checks exact
target ratios, atomic multi-input shapes, duplicate signatures, and all ten
playable maps. Expansion uses the immutable prefix-to-material index and has a
20-second/10,000-plan CI budget; the current reference expands 8,136 T3 recipes,
including 2,782 shape-selected extruder recipes. Import generation closes
component prerequisites to a fixed point:
2,470 material/component forms are retained and the committed unproduced count
is zero. The 118 retained cable forms consume matching conductor wire plus
1/1/2/3/4 Rubber plates for gauges 1/2/4/8/12. This insulation model follows
`Loader_OreProcessing.java:183-184` and `OP.java:644-648`; it deliberately adds
no electrical rating.

The extruder, cutter, lathe, rolling mill, roll bender, wire mill, bender,
assembler, welder, and press are registered configured processing blocks backed
by the shared block entity/menu/screen. Place a Bronze Steam Engine immediately
behind a machine, facing toward the machine: the engine's exposed front face
then feeds the machine's KU back input. Every T3 machine buffers 4,096 KU and
accepts packets up to 256 KU; the engine's fixed 12 KU/t CC packets accumulate without
creating energy. Wire mill, assembler, welder, and press provide two item inputs;
the other machines provide one. All provide one item output, while assembler
and welder additionally reserve one 4,000 mB fluid input tank.
Rubber insulation is survival-reachable through the intentional compatibility
bridge `minecraft:slime_ball -> rubber:plate` in the press before cable assembly;
the comparator records this non-GT bridge as `INTENTIONAL`.

Configured machine blockstates use the horizontal `FACING` property with an
orientable model: the distinct furnace-textured front is item output and the
opposite face is KU input. External fluid capabilities remain input-only and
accept only fluids currently referenced by that machine's loaded recipe map;
empty maps reject every fluid. A non-sneaking player with an empty fluid
container uses a separate maintenance drain view, while filled containers use
the recipe-gated input view. Sneaking still opens the menu. Tank amount/capacity
and processing status are synchronized in the generic menu.

Unit tests cover spec/map shape, side placement, generated resource coverage,
atomic cable rollback, cache revision, and the expansion budget. The dedicated
GameTest server covers real block entities, capabilities, recipe maps, ticking,
HEAT/KU progress, rollback, completion, and persistence. Automated client-side
visual inspection remains unavailable; models and menu resources are validated
structurally by datagen tests rather than claimed as browser-verified.

The final report is not hand-edited to `READY`. The phase orchestrator owns the
snapshot refresh, stage recording, and single `READY` binding:

```text
python tools/run_full_verification.py --record
python tools/run_full_verification.py --record --resume
python tools/run_full_verification.py --check-ready
python tools/run_full_verification.py --check
```

Individual `verify_full_verification_report.py --mark-*` flags are implementation
and diagnostic interfaces. They must not be used to repeat closure stages that
the orchestrator has already executed.

The report binds builder/source/manifest hashes, the complete generated
component tree, shape catalog/resources, selector policy, compact artifact
manifest, Java XML metrics, the four material GameTest routes with declared
durations and outputs, and both drift-free datagen hashes. The metadata-only
cache policy means ordinary CI does not restore O-10 artifacts. Removing a
large file from the current tree does not erase its blob from older commits;
history reduction still needs a clean root, history migration, or LFS.

At T3 closeout the exact suites are 282 Java unit tests (73 suites), 148 Python
unit tests, and 18 production GameTests. The GameTests include four independent
copper/tin/iron/gold component routes, live per-map count/source tracing, all
ten placed T3 machines, and the retained T2 runtime acceptance coverage.
