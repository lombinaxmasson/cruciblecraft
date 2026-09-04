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
- The numbered-card era is over. No new milestone numbers. No player release, RC soak, or GA

Only one active delivery lane at a time. Work packets can be archived; they **do not** mint `*_READY`.

## How to author content

Work units are **semantic slugs** (`logistics/fluid-network/basic-transfer`, `assembler/compact`, `smelter/ordinary-closure`), not numbers such as `T38`. New recipes, receipts, and test paths use those names.

Live waves include compact / wood / stone / `block/object` / `storage/lock`, Bath (`bath/mte`, `bath/remainder`, `bath/identity`, `bath/tiny-purified`), ordinary-closure, and `smelter/deferred-recycling`. Publication groups look like `cruciblecraft:<host>/<cohort>`. Historical numbered ids map through `tools/semantic_id_map.json`.

Recipe import:

```text
Source Pack (GT6 / future GT6U / design, append-only)
  -> human scope + frozen production lock
  -> tools/recipe_bulk compiles exact / exact_multi
  -> publication groups / shards
  -> CompactRecipeFamilyProvider materializes at runtime
```

Generated trees live in `src/recipe_generated/` and `src/recipe_support_generated/`. `build.gradle` mounts them into `sourceSets.main.resources`. Do not add new content under numbered recipe paths or `src/tXX_*_generated`. The `build_t*.py` builders are gone. Gradle `-PwaveRecipes` rejects `T` plus digits. Giant Bath sources (`tools/bath_*_source.json`) and `owner_family_operand_snapshot.json` stay local replay inputs; they are not tracked.

Unknown `parameterized()` templates fail closed (empty expansion, no throw). Markdown plans are not production authority. Capability declarations live in `tools/capabilities/<slug>/capability.json`.

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

Put proxies in user-level `~/.gradle/gradle.properties`. Do not commit them. `run/`, `run-client-smoke/`, `run-wire-codec-*`, `build/`, and local GT6 dumps are not the canonical tree either.

## Daily verification

Daily verification is the command you just ran, not a committed currentness sidecar, seal, or historical READY report:

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile verification
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile capability-runtime
python tools/verify.py integration --profile player-complete
```

The `verification` profile runs `python tools/check_zero_milestone_names.py --quick`. That scan covers the live logistics / capability surface and must stay at zero hits.

Do not treat `python tools/check_zero_milestone_names.py --summary` as a daily gate. The full scan reads generated trees and historical receipts, takes a long time, and legitimately hits frozen v2 ledgers, `semantic_id_map.json`, and earlier independent-stage numbered JSON. Live authoring is already semantic; a zero-hit full scan is not the current acceptance bar.

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

Tools are also split:

- **Current entry points:** `tools/verify.py`, `tools/recipe_bulk/`, `tools/build_semantic_recipes.py`, `tools/build_capability_ledger.py`, `tools/build_player_complete.py`, `tools/check_no_workflow_hashes.py`, `tools/check_zero_milestone_names.py --quick`
- **Historical receipts:** earlier independent-stage numbered JSON, frozen v2 ledgers, and `tools/semantic_id_map.json`. Compact / Bath / ordinary-closure live ledgers already use semantic names; they are not the current authoring API

Docs: [docs/current/](docs/current/) is live, [docs/history/](docs/history/INDEX.md) is read-only archive.

## Where numbers still appear

Early work used numbered cards. Those numbers leaked into paths, test names, and runtime ids. Closeout checklist A–E moved live waves, receipts, currentness, test consumers, and operational docs onto semantic slugs.

Numbers still appear in:

- `docs/history/card-plans/`: the only planned keep for numbered plan docs
- Frozen v2 identity / runtime / load-budget JSON: bytes stay unchanged
- `tools/semantic_id_map.json`: maps historical fixtures onto semantic group names
- Earlier independent-stage closed-card receipts, plus test strings that reject old numbers (for example `T50`)

Do not rename those as a side effect of a content card. `GT6` / `gt6_*` means GregTech 6, not a card number. Long-term rules: [semantic-naming.md](docs/current/semantic-naming.md). This round’s execution record: [closeout checklist](docs/current/semantic-naming-closeout-checklist.md).

## Docs map

- [Roadmap](docs/current/roadmap.md)
- [Capability delivery](docs/current/capability-delivery-workflow.md)
- [Unimplemented gap ledger](docs/current/unimplemented-gap.md)
- [Verification](docs/current/verification.md)
- [Ordinary recipe-wave rules](docs/current/recipe-wave-workflow.md)
- [Semantic naming inventory](docs/current/semantic-naming.md)
- [Semantic naming closeout checklist](docs/current/semantic-naming-closeout-checklist.md)
- [Known issues](docs/current/known-issues.md)
- [Doc index](docs/README.md) · [History](docs/history/INDEX.md)
- [Tools](tools/README.md)
- [Credits](CREDITS.md) · [NOTICE](NOTICE) · [Changelog](CHANGELOG.md)
- [Player guide](docs/current/player-guide.md)

## License

Source and original assets are [LGPL-3.0-or-later](LICENSE). GT6/GTM source data, default CC0 assets, the logo CC-BY-NC exception, GT6 anvil geometry, and the MDK template are in [CREDITS.md](CREDITS.md) and [NOTICE](NOTICE).

Jade, EMI, and KubeJS are optional unbundled integrations. See [`neoforge.mods.toml`](src/main/templates/META-INF/neoforge.mods.toml).
