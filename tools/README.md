# GT6 recipe regression

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
definitions. The 43 already-registered runtime prefixes may create startup
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
stress task benchmarks 20,000 metadata-only and 20,000 single-dust additions.
The 4,096 handshake cap remains a hard encode/decode budget; the current
production-scale 1,815-entry payload round-trips below it.

The vein builder owns the semantic author documents in
`src/main/resources/data/cruciblecraft/veins` and the runtime output in
`src/worldgen_generated/resources`. Each schema-version-1 source names four
non-empty weighted material layers plus placement parameters, a globally unique
salt, and provenance. The builder accepts exactly one of `--write`, `--check`,
or `--review`; it validates both factual and gate-registered `ore` forms before
generating all configured features, placed features, and the aggregate biome
modifier.

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
extruder sparse rows. Runtime expansion is 8,136 recipes: assembler 568, bender
638, cutter 651, extruder 2,782, lathe 929, press 1,191, roll bender 438,
rolling mill 336, welder 321, and wire mill 282. It also locks unit
conservation, recipe/shadow signature digests (zero shadows), the 10,000-recipe
budget, 31 reusable shapes, and all 62 extruder-template classifications
(20 playable, 42 skipped, zero unclassified). Runtime expansion remains owned
solely by `MaterialRuleExpansion` and `GTRecipeMapLoader`; CI needs no
gitignored GT6 replay artifact.

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

```text
python tools/build_component_rules.py --check
python tools/gt6_extruder_templates.py --verify
./gradlew runData
./gradlew runData
./gradlew test
./gradlew runGameTestServer
python -m unittest discover -s tools/tests -p "test_*.py"
python tools/import_gt6_oredict.py --check --reference-only
python tools/compare_gt6_recipes.py --check
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

`T2ChainIntegrationTest` expands the production `T2ChainRules` against the
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

`T3ComponentDataTest` validates the normalized GT6 prefix amount and
registered-material evidence for every activated component prefix, expands the
committed compact rules through the production projector, and checks exact
target ratios, atomic multi-input shapes, duplicate signatures, and all ten
playable maps. Expansion uses the immutable prefix-to-material index and has a
20-second/10,000-plan CI budget; the current reference expands 8,136 T3 recipes,
including 2,782 shape-selected extruder recipes. Import generation closes
component prerequisites to a fixed point:
2,470 material/component forms are retained and the committed unproduced count
is zero. The 112 retained cable forms consume matching conductor wire plus
1/1/2/3/4 Rubber plates for gauges 1/2/4/8/12. This insulation model follows
`Loader_OreProcessing.java:183-184` and `OP.java:644-648`; it deliberately adds
no electrical rating.

The extruder, cutter, lathe, rolling mill, roll bender, wire mill, bender,
assembler, welder, and press are registered configured processing blocks backed
by the shared block entity/menu/screen. Place a Bronze Steam Engine immediately
behind a machine, facing toward the machine: the engine's exposed front face
then feeds the machine's KU back input. Every T3 machine buffers 4,096 KU and
accepts packets up to 256 KU; the engine's 24 KU packets accumulate without
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

The final report is not hand-edited to `READY`. Refresh it after all source and
documentation edits, then record each actual run:

```text
python tools/verify_full_verification_report.py --write-tooling-snapshot
python tools/verify_full_verification_report.py --mark-builder-passed --builder-elapsed-ms <ms> --extruder-replay-result <PASS|SKIP>
python tools/verify_full_verification_report.py --mark-datagen-passed --datagen-run-1-hash <sha256> --datagen-run-2-hash <sha256> --datagen-run-1-seconds <s> --datagen-run-2-seconds <s>
python tools/verify_full_verification_report.py --mark-java-passed --java-elapsed-seconds <s>
python tools/verify_full_verification_report.py --mark-gametest-passed --gametest-log run/logs/latest.log --gametest-elapsed-seconds <s>
python tools/verify_full_verification_report.py --mark-python-passed --python-elapsed-seconds <s>
python tools/verify_full_verification_report.py --mark-ready
python tools/verify_full_verification_report.py --check
```

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
