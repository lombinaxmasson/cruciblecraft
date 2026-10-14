# CrucibleCraft

[中文](README.md)

GregTech 6 on Minecraft 1.21.1 / NeoForge: materials, machines, recipes,
energy, logistics, and worldgen, ported from a pinned upstream with a
data-driven pipeline. Unofficial. The target is full coverage; progress is
closed capabilities, and remaining gaps stay on the ledger until they are
done for real.

**The project was wrapped up in October 2026 and is no longer under active
development.** The last version is the development snapshot
`0.1.0-test.20260927.1`, kept as a source tree; there was never a formal
release. Play notes: [player guide](docs/current/player-guide.md) (new worlds).
Community development continues at
[SaltNya/GregTech6](https://github.com/SaltNya/GregTech6).

- [Project status](docs/current/project-status.md)
- [GT6 full-coverage assessment](docs/current/gt6-full-coverage.md)
- [Unimplemented gaps](docs/current/unimplemented-gap.md)
- [Roadmap](docs/current/roadmap.md)
- [Issue tracker](https://github.com/lombinaxmasson/cruciblecraft/issues)
  (left open; replies are not guaranteed)

## State at wrap-up

- 135 runtime capabilities are closed (`runtime_ready` accepted). No card was
  in progress at wrap-up. The full list is in
  [project status](docs/current/project-status.md).
- 14 prep plans were never started, including GT6 crops and food, the
  PUV2+ / OMEGA tech line, the large bath, the printer, GT Center, dungeons,
  planet rocks, and a few bulk-port umbrella plans. They are listed under Prep
  in project status.
- A closed capability does not mean players can obtain it in survival.
  Obtainability is tracked separately; gaps and blockers stay in
  [unimplemented-gap.md](docs/current/unimplemented-gap.md) and
  [blocked.md](docs/current/blocked.md). For coverage against GT6 as a whole,
  see the [GT6 full-coverage assessment](docs/current/gt6-full-coverage.md).
- There is no DataFixer. A new version always needs a new world.

## In the tree

- Materials, prefixes, ore processing, and source-generated recipe sets
- Fire / heat / steam / kinetic / rotational / EU and multiple running machine
  families. Some content is bounded or paused, and survival access is a
  separate axis.
- Use the [GT6 full-coverage assessment](docs/current/gt6-full-coverage.md)
  for source/runtime/published/survival reconciliation rather than capability
  counts or generated-file counts
- Large veins, underground oil and gas, surface rocks, stone layers, bedrock
  ores
- Item pipes, fluid pipes, cables, covers, automation
- Crude oil and natural gas through distillation into engines and power
- EMI recipes, Jade overlays, Reliable EMI form / tool / machine / building / furniture folding; optional unbundled KubeJS

Status and gaps: [project status](docs/current/project-status.md),
[unimplemented-gap.md](docs/current/unimplemented-gap.md),
[blocked.md](docs/current/blocked.md).

## Porting principles

GT6 is CrucibleCraft's primary source and comprehensive porting target. Where
Minecraft or NeoForge version differences require interpretation, the project
distinguishes three kinds of information:

- `SOURCE_BACKED`: facts directly traceable to a pinned upstream source
- `SOURCE_DERIVED`: results derived from source data or behavior
- `DESIGN_POLICY`: project decisions made for compatibility, playability, or
  implementation constraints

These labels document the basis for an implementation; they are not a
substitute for player documentation. Pinned upstream revisions, licenses, and
third-party attribution are listed in [CREDITS.md](CREDITS.md) and
[NOTICE](NOTICE).

## Build and run

Java 21, Minecraft 1.21.1, NeoForge 21.1.243. Plan on ≥16 GiB; the first
world load takes a while. Start a new save.

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat test
```

Artifacts land in `build/libs/` as
`cruciblecraft-0.1.0-test.20260927.1.jar`. `distBeta` zips the docs with it.
Keep Gradle proxies in user-level `~/.gradle/gradle.properties`.

## Development model

### Data and rules

Routine changes to materials, recipes, and machine families are expressed
through data, catalogs, and generation rules instead of one Java
implementation per object. Authored resources, generated resources, and
capability declarations live in separate locations:

```text
src/main/resources/     Hand-authored data and assets
src/*_generated/        Tool-generated resources tracked in version control
tools/waves/<slug>/     Domain inputs, production locks, and census data
tools/capabilities/     Verifiable capability declarations
```

Recipe content generally follows this pipeline:

```text
Source Pack (GT6 source or an explicit project design)
  -> select porting scope and freeze a production lock
  -> tools/recipe_bulk compiles exact / exact_multi
  -> publication group / shard
  -> runtime materialization
```

New work uses semantic slugs such as
`logistics/fluid-network/basic-transfer` and
`smelter/ordinary-closure`. Early numbered identifiers remain only in
historical records and compatibility maps; new runtime IDs, recipe paths, and
test paths use semantic names.

### Capability states

Capabilities track whether a specification is defined and whether the runtime
works. Player obtainability is a separate `survival_access` field:

- `frozen`: scope, sources, and dependencies are defined
- `runtime_ready`: the runtime mechanism works and can close; content or
  obtain paths may still be incomplete
- `survival_access`: independent obtainability (`unreviewed` / `blocked` /
  `partial` / `complete` / `not_applicable`); does not block runtime close

The roadmap counts accepted `runtime_ready` capabilities as mechanism
progress. Problems found in a human `runClient` become their own cards; there
is no project-level sign-off. The generated
set is [project status](docs/current/project-status.md).
See the [capability delivery workflow](docs/current/capability-delivery-workflow.md)
for the complete contract.

### Verification

Common verification entry points are:

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile verification
python tools/verify.py integration --profile runtime-java
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile recipe-generators
python tools/verify.py integration --profile capability-runtime
```

`dev` selects relevant checks from the working-tree changes: ordinary runtime
Java runs JUnit without datagen; datagen providers and generated trees run the
double `runData` profile. CI never auto-runs `runClient`. The
[verification guide](docs/current/verification.md) and
[tooling guide](tools/README.md) explain profile selection and result locations.

## Code layout

Daily work lives in `src/main`, `src/test`, `tools/waves/`, `tools/tests/`,
and `docs/current/`. Local `run*/` folders, reference source trees, and caches
are not part of the tracked layout; see
[code tree and working copy](docs/current/code-tree.md).

The main runtime code is under
`src/main/java/com/masson/cruciblecraft/`:

- `registry`, `content`, and `machine`: blocks, items, processing machines, and
  content catalogs
- `recipe` and `recipe.gt`: RecipeMaps, compact recipe families, and
  publication
- `material`: material definitions, prefix catalogs, and generated packs
- `energy`, `heat`, `steam`, and `fluid`: energy and fluid systems
- `logistics`: pipes, cables, hoppers, covers, and transport networks
- `worldgen`: ore veins, oil and gas deposits, and other world generation
- `census`, `scale`, and `gametest`: coverage accounting, load verification,
  and GameTests
- `datagen` and `client`: data generation and client integrations

Current specifications live in [docs/current/](docs/current/). Earlier plans
and stage records are retained as a read-only archive in
[docs/history/](docs/history/INDEX.md).

## Documentation

For players and project status:

- [Player guide](docs/current/player-guide.md)
- [Roadmap](docs/current/roadmap.md)
- [Known issues](docs/current/known-issues.md)
- [Changelog](CHANGELOG.md)

For development and contributions:

- [Verification guide](docs/current/verification.md)
- [Code tree and working copy](docs/current/code-tree.md)
- [Capability delivery workflow](docs/current/capability-delivery-workflow.md)
- [Recipe-wave workflow](docs/current/recipe-wave-workflow.md)
- [Semantic naming rules](docs/current/semantic-naming.md)
- [Tooling guide](tools/README.md)
- [Complete documentation index](docs/README.md)

Issues: version, steps, logs.

## License

Source code and original project assets are licensed under
[LGPL-3.0-or-later](LICENSE). GT6-derived data, third-party assets, templates,
and their respective licenses are documented in [CREDITS.md](CREDITS.md) and
[NOTICE](NOTICE). GT6 default assets are CC0 1.0 upstream; this project does
not use the GregTech logo (CC-BY-NC-4.0).

EMI, Jade, Reliable EMI (REMI / EMI++), and KubeJS integrations are optional
and are not bundled.
