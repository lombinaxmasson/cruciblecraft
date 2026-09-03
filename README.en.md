# CrucibleCraft

[中文](README.md)

A GT6-**style** industrial Minecraft mod for NeoForge 1.21.1. Routine material, recipe, and machine changes should go through data and rules, not one-off Java copies.

This is not a full port of GT6 or GT6U, and this repository is not a player release. `0.1.0-rc.1` is a historical engineering candidate, not `1.0.0`, GA, or “ready for players.” Source facts, derived rules, and design choices are recorded as `SOURCE_BACKED`, `SOURCE_DERIVED`, or `DESIGN_POLICY`.

The [player guide](docs/current/player-guide.md) is play-facing text. It is not the authority for source status.

## Current status

- **The claimed 1.x portfolio has jointly exited** (`ONE_X_JOINT_EXIT_READY`). Current execution gap = 0. Deferred ledger = 0 (1,817 Smelter MTE recoveries complete + 28 independent post-1.x scopes).
- **The source-capability map is closed** (`SOURCE_CAPABILITY_MAP_READY`). 113 mechanism rows (22 seed + 91 expanded). This is not a recipe census.
- **The generic Source Pack importer is closed** (`GENERIC_RECIPE_IMPORT_READY`). See the [closed plan](docs/history/card-plans/closed/通用Source-Pack导入器详细计划.md). An existing host plus a new Source Pack no longer needs a per-wave builder or a handwritten `WaveSpec`. Production still needs a separately reviewed content card. No recipes are published.
- **Logistics cover net R0 is closed** (`LOGISTICS_COVER_NET_R0_READY`). See the [closed plan](docs/history/card-plans/closed/物流封面网络R0详细计划.md). Seven T13 logistics cover kinds and their fit onto the existing T19/T44 stack are frozen. Feasibility is `requires_new_runtime`. It does not implement a network runtime and does not pre-assign a core child.
- **T13c exclusion reclaim R0 is closed** (`T13C_EXCLUSION_RECLAIM_R0_READY`). See the [closed plan](docs/history/card-plans/closed/T13c排除表收回R0详细计划.md). Five T13c categories (129/471) and their fit onto existing mechanisms are frozen. It does not implement MTEs.
- **Non-ore worldgen R0 is closed** (`NON_ORE_WORLDGEN_R0_READY`). See the [closed plan](docs/history/card-plans/closed/非矿世界生成R0详细计划.md). The four capability-map named non-ore worldgen dump categories (18/190) and their fit onto the existing T20 catalog are frozen. All four verdicts are `requires_new_runtime`. It does not implement worldgen and does not pre-assign an implementation child.
- **Vanilla replace R0 is closed** (`VANILLA_REPLACE_R0_READY`). See the [closed plan](docs/history/card-plans/closed/原版替换R0详细计划.md). The two capability-map named vanilla-replace loaders and their fit onto the existing datapack / importer stack are frozen. Both verdicts are `requires_new_runtime`. It does not replace vanilla recipes and does not pre-assign an implementation child.
- **Crops / food / bees R0 is closed** (`CROPS_FOOD_BEES_R0_READY`). See the [closed plan](docs/history/card-plans/closed/作物食物蜜蜂R0详细计划.md). The three capability-map named crops / food / bees categories and their fit onto existing mechanisms are frozen. All three verdicts are `requires_new_runtime`. It does not implement crops, food maps, or hives and does not pre-assign an implementation child.
- **First-hour presentation and the stage ledger are closed** (`FIRST_HOUR_PRESENTATION_READY`). See the [closed plan](docs/history/card-plans/closed/首小时表现与阶段账本详细计划.md). Mortar / sifter / bath imported GT6 tool textures; `smelter` is now a cube machine and no longer shares the crucible pot. Three-state ledger and next sequence: [unimplemented-gap](docs/current/unimplemented-gap.md). No next implementation child is pre-assigned.
- **Vanilla-replace MVP is closed** (`VANILLA_REPLACE_MVP_READY`). See the [closed plan](docs/history/card-plans/closed/原版替换MVP详细计划.md). Paper is 3 sugar cane → 1 paper; furnace / bones stay deferred (no pre-iron firestarter). The R0 seal is unchanged. Vanilla / Replace in the gap ledger are partial, remainder still frozen. No next implementation child is pre-assigned.
- **Item Network Core is closed** (`ITEM_NETWORK_CORE_READY`). See the [closed plan](docs/history/card-plans/closed/物品网络核心详细计划.md). Item storage / transfer covers are `runtime_ready`. T19 9/8 stays live-locked; new defs live in a sidecar. Fluid / Generic / Dump and `MultiTileEntityLogisticsCore` stay out. Not seven-kind `player_complete`. Machine-readable `unique_active_wave` is `null`; `next_unassigned = true`.
- Nuclear Track C stays `started = false`. No new milestone numbers. No player release, RC soak, or GA.
- **A mechanism-card `*_READY` is not shipped content.** The R0 cards after the capability map only froze denominators and feasibility. Gap ledger: [冻结与未实现账本](docs/current/unimplemented-gap.md).

A historical `_READY` seal only proves that card’s denominator. It does not mean the mod is fully playable or that GT6 is complete. The [roadmap](docs/current/roadmap.md) is the living plan.

## Build

| Tool | Version |
| --- | --- |
| Java | 21 |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.243 |
| Gradle Wrapper | 9.2.1 |
| ModDevGradle | 2.0.142 |

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Put proxies in user-level `~/.gradle/gradle.properties`. Do not commit machine-local proxy settings.

## How development works

The numbered-card era (T7–T49) is over. Work units are **semantic slugs** (`smelter/ordinary-closure`, `portfolio/source-capability-map`). Only one child is active at a time.

The current recipe import chain:

```text
Source Pack (GT6 / future GT6U / design, append-only)
  -> human scope + frozen production lock
  -> tools/recipe_bulk compiles exact / exact_multi
  -> publication groups / shards
  -> CompactRecipeFamilyProvider materializes at runtime
```

`parameterized()` is still fail-closed. The compiler is already generic. What is missing is the registration glue before a new pack becomes a lock — that is the importer card above. Markdown plans are not production authority. Machine-readable truth lives in `tools/waves/<slug>/` (lock, census, readiness, seal).

Daily verification:

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile recipes
```

See [verification.md](docs/current/verification.md) for “changed X, run Y.” Use `--profile` when closing a content card. `release` and historical `--check-ready` are not the daily green light. Tooling details: [tools/README.md](tools/README.md).

## Code and repository layout

Runtime code lives under `src/main/java/com/masson/cruciblecraft/`:

| Package | Role |
| --- | --- |
| `registry` / `content` / `machine` | Blocks, items, processing machines, catalog projection |
| `recipe` / `recipe.gt` | RecipeMaps, compact families, publication |
| `material` | Material definitions, prefix catalog, generated packs |
| `energy` / `heat` / `steam` / `fluid` | Selected energy and fluid chains |
| `logistics` | Pipes, cables, hoppers, covers |
| `worldgen` | Selected veins, fluid deposits, pebbles |
| `census` / `scale` / `gametest` | Denominators, load, GameTests |
| `datagen` / `client` | Data generation and client code |

Keep the three data layers separate:

```text
src/main/resources/     Hand-authored data and assets
src/*_generated/        Tool-written trees that are tracked in git
tools/waves/<slug>/     Per-card lock / census / seal
```

`build.gradle` mounts several generated trees into `sourceSets.main.resources`. Semantic-wave compact recipes go in `src/recipe_generated/` and `src/recipe_support_generated/`. Do not add new content under `recipe/tXX/`.

Tools are also split:

- **Current entry points:** `tools/verify.py`, `tools/recipe_bulk/`, `tools/build_ordinary_wave.py`, `tools/build_recipe_bulk.py`
- **Closed-card ledgers:** `tools/build_t*.py`, `tools/t16_*.json`, and similar. Milestone filenames stay so historical `--check` receipts remain stable. They are not the current authoring API.

Docs are split the same way: [docs/current/](docs/current/) is live, [docs/history/](docs/history/INDEX.md) is read-only archive. `4.5Fix/`, `build/`, `run*/`, `run-wave-*/`, local GT6 dumps, and the `src/src/` duplicate tree are not the canonical main tree. Do not commit them.

## Naming debt

Early work used numbered cards (T7, T20, T45). Those numbers leaked into paths, test names, and runtime ids, and later waves could not reuse the special cases. Live authoring now uses semantic paths: `recipe/mortar/`, `recipe/pipe/`, `recipe/ingot_form/`, and wave slugs.

Remaining `TXX` tokens are unfinished migration, not a permanent exemption. The only planned keep is `docs/history/card-plans/`. Inventory, batches, and verification: [semantic-naming.md](docs/current/semantic-naming.md). Do not rename them as a side effect of a content card. `GT6` / `gt6_*` means GregTech 6, not a card number.

Still visible in the tree:

- Bound ids (NBT, family ids, gate fields such as `t14_extruder`, `t11_materials/`)
- About 397 `tools/build_t*.py` scripts and their receipts
- Six fixture trees, 1321 files (`t39_*` … `t45_*`, including `t39_withdrawn_recovery`)
- Texture dir `t34_gt6/`; GameTest T18 method names are already semantic, readiness tokens are not

## Docs map

- [Roadmap](docs/current/roadmap.md)
- [Verification](docs/current/verification.md)
- [Ordinary recipe-wave rules](docs/current/recipe-wave-workflow.md)
- [Semantic naming inventory](docs/current/semantic-naming.md)
- [Known issues](docs/current/known-issues.md)
- [Doc index](docs/README.md) · [History](docs/history/INDEX.md)
- [Tools](tools/README.md)
- [Credits](CREDITS.md) · [NOTICE](NOTICE) · [Changelog](CHANGELOG.md)
- [Player guide](docs/current/player-guide.md)

## License

Source and original assets are [LGPL-3.0-or-later](LICENSE). GT6/GTM source data, default CC0 assets, the logo CC-BY-NC exception, GT6 anvil geometry, and the MDK template are in [CREDITS.md](CREDITS.md) and [NOTICE](NOTICE).

Jade, EMI, and KubeJS are optional unbundled integrations. See [`neoforge.mods.toml`](src/main/templates/META-INF/neoforge.mods.toml).
