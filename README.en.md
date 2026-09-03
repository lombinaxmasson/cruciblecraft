# CrucibleCraft

[中文](README.md)

A GT6-**style** industrial Minecraft mod for NeoForge 1.21.1. Routine material, recipe, and machine changes go through data and rules, not one-off Java copies.

This is not a full port of GT6 or GT6U, and this repository is not a player release. `0.1.0-rc.1` is a historical engineering candidate, not `1.0.0`, GA, or “ready for players.” Source facts, derived rules, and design choices are recorded as `SOURCE_BACKED`, `SOURCE_DERIVED`, or `DESIGN_POLICY`.

The [player guide](docs/current/player-guide.md) is play-facing text. It is not the authority for source status or progress.

## Where things stand

Progress only counts a **capability** at `player_complete`, and only after a fresh PASS on the current Git revision. `frozen` and `runtime_ready` are stages. A historical `*_READY` seal only proves that card’s denominator; it does not mean the feature exists in-game. Contract: [capability-delivery-workflow.md](docs/current/capability-delivery-workflow.md).

- Current capability: `logistics/fluid-network/basic-transfer` (`player_complete`)
- Item storage / transfer covers are `runtime_ready`, not seven-kind complete
- Generic / Dump covers, crops / bees, vanilla furnace replace, nuclear, and the rest live in [unimplemented-gap.md](docs/current/unimplemented-gap.md)
- The numbered-card era (T7–T49) is over. No new milestone numbers. No player release, RC soak, or GA

Only one active delivery lane at a time. Work packets can be archived; they **do not** mint `*_READY`.

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
.\gradlew.bat test
```

Put proxies in user-level `~/.gradle/gradle.properties`. Do not commit them. `run-client-smoke/`, `run-wire-codec-*`, `build/`, and local GT6 dumps are not the canonical tree either.

## How development works

Work units are **semantic slugs** (`logistics/fluid-network/basic-transfer`, `smelter/ordinary-closure`). Recipe import:

```text
Source Pack (GT6 / future GT6U / design, append-only)
  -> human scope + frozen production lock
  -> tools/recipe_bulk compiles exact / exact_multi
  -> publication groups / shards
  -> CompactRecipeFamilyProvider materializes at runtime
```

Unknown `parameterized()` templates fail closed (empty expansion, no throw). Markdown plans are not production authority. Capability declarations live in `tools/capabilities/<slug>/capability.json`.

Daily verification is the command you just ran, not a committed currentness sidecar, seal, or historical READY report:

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile verification
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile capability-runtime
python tools/verify.py integration --profile player-complete
```

`dev` maps dirty paths onto profiles. Results go to gitignored `build/verification/latest.json` (revision, commands, PASS/FAIL; no workflow content digests). `player-complete` must launch an isolated GameTestServer and a real `runClient` in the same call.

See [verification.md](docs/current/verification.md) for “changed X, run Y.” Tooling details: [tools/README.md](tools/README.md).

## Code and repository layout

Runtime code lives under `src/main/java/com/masson/cruciblecraft/`:

| Package | Role |
| --- | --- |
| `registry` / `content` / `machine` | Blocks, items, processing machines, catalog projection |
| `recipe` / `recipe.gt` | RecipeMaps, compact families, publication |
| `material` | Material definitions, prefix catalog, generated packs |
| `energy` / `heat` / `steam` / `fluid` | Selected energy and fluid chains |
| `logistics` | Pipes, cables, hoppers, covers, item / fluid nets |
| `worldgen` | Selected veins, fluid deposits, pebbles |
| `census` / `scale` / `gametest` | Denominators, load, GameTests |
| `datagen` / `client` | Data generation and client code |

Keep the three data layers separate:

```text
src/main/resources/     Hand-authored data and assets
src/*_generated/        Tool-written trees that are tracked in git
tools/waves/<slug>/     Per-card lock / census (archive or domain plugin)
tools/capabilities/     Live capability declarations
```

`build.gradle` mounts several generated trees into `sourceSets.main.resources`. Semantic-wave compact recipes go in `src/recipe_generated/` and `src/recipe_support_generated/`. Do not add new content under `recipe/tXX/`.

Tools are also split:

- **Current entry points:** `tools/verify.py`, `tools/recipe_bulk/`, `tools/build_capability_ledger.py`, `tools/build_player_complete.py`, `tools/check_no_workflow_hashes.py`
- **Closed-card ledgers:** `tools/build_t*.py` and similar. Milestone filenames stay so historical receipts remain stable. They are not the current authoring API

Docs: [docs/current/](docs/current/) is live, [docs/history/](docs/history/INDEX.md) is read-only archive.

## Naming debt

Early work used numbered cards (T7, T20, T45). Those numbers leaked into paths, test names, and runtime ids. Live authoring now uses semantic paths: `recipe/mortar/`, `recipe/pipe/`, `assembler/compact`, capability slugs.

Remaining `TXX` tokens are unfinished migration. The only planned keep is `docs/history/card-plans/`. Tests load frozen fixtures through `archive/sealed/forward-v2/semantic_id_map.json` and do not rewrite those bytes. Inventory: [semantic-naming.md](docs/current/semantic-naming.md). Do not rename them as a side effect of a content card. `GT6` / `gt6_*` means GregTech 6, not a card number.

## Docs map

- [Roadmap](docs/current/roadmap.md)
- [Capability delivery](docs/current/capability-delivery-workflow.md)
- [Unimplemented gap ledger](docs/current/unimplemented-gap.md)
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
