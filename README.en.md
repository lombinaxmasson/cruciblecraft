# CrucibleCraft

[中文](README.md)

CrucibleCraft is an unofficial port of GregTech 6 to Minecraft 1.21.1 on
NeoForge. Its long-term goal is to port GT6's complete material, machine,
recipe, energy, logistics, and world-generation systems to modern Minecraft,
backed by a maintainable and verifiable data-driven implementation.

- [Player guide](docs/current/player-guide.md)
- [Roadmap and current progress](docs/current/roadmap.md)
- [Issue tracker](https://github.com/icodestuljh/cruciblecraft/issues)

## Screenshots

> Screenshots and gameplay demonstrations will be added here.

<!-- Suggested subjects: world generation, machine lines, logistics networks, and multiblocks. -->

## Features

The current codebase includes the following foundations, with coverage
continuing to expand toward the full GT6 target:

- A material, prefix, and ore-processing system with large source-generated
  recipe sets
- Multi-stage energy chains spanning fire, heat, steam, kinetic, rotational,
  and electrical power; electric heaters/engines, LU fiber, fission cores,
  and a fusion controller are in runtime (survival recipes incomplete, not
  `player_complete`)
- Processing-machine families from early industry onward, plus multiblocks
  such as distillation towers, large boilers, and tanks
- World generation for large ore veins, underground oil and gas, and surface
  resources
- Item, fluid, and electrical transport using pipes, cables, covers, and
  automation components
- Oil and natural-gas processing with downstream fuel and power paths
- EMI recipe display and Jade block information, with optional KubeJS
  compatibility

Systems that have not yet been implemented and their planned order are tracked
in the [unimplemented feature ledger](docs/current/unimplemented-gap.md). See
the [player guide](docs/current/player-guide.md) for play-facing documentation
and known limitations.

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

Java 21 is required. The project targets Minecraft 1.21.1 and NeoForge
21.1.243 and builds with the included Gradle Wrapper.

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat test
```

Build artifacts are written to `build/libs/`. Development proxies belong in
the user-level `~/.gradle/gradle.properties`, not in the repository.

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

Capabilities track whether a specification is defined, its runtime works, and
players can use it end to end:

- `frozen`: scope, sources, and dependencies are defined
- `runtime_ready`: the runtime mechanism works, while content or player paths
  may still be incomplete
- `player_complete`: survival acquisition, runtime behavior, UI, translation,
  persistence, and verification paths are complete

The roadmap counts only `player_complete` capabilities as player-facing
implementation progress. Basic fluid-network transfer, item-network
storage/import/export covers, generic-network storage/import/export
covers, Logistics Core plus Dump, and Display CPU monitors have reached
that state.
See the [capability delivery workflow](docs/current/capability-delivery-workflow.md)
for the complete contract.

### Verification

Common verification entry points are:

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile verification
python tools/verify.py integration --profile runtime-java
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile capability-runtime
python tools/verify.py integration --profile player-complete
python tools/verify.py promotion
```

`dev` selects relevant checks from the working-tree changes: ordinary runtime
Java runs JUnit without datagen; datagen providers and generated trees run the
double `runData` profile. `promotion` runs
the full GameTestServer and client only when a capability is raised to
`player_complete`. The
[verification guide](docs/current/verification.md) and
[tooling guide](tools/README.md) explain profile selection, result locations,
and the GameTestServer and client checks used for `player_complete`.

## Code layout

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
- [Capability delivery workflow](docs/current/capability-delivery-workflow.md)
- [Recipe-wave workflow](docs/current/recipe-wave-workflow.md)
- [Semantic naming rules](docs/current/semantic-naming.md)
- [Tooling guide](tools/README.md)
- [Complete documentation index](docs/README.md)

## License

Source code and original project assets are licensed under
[LGPL-3.0-or-later](LICENSE). GT6-derived data, third-party assets, templates,
and their respective licenses are documented in [CREDITS.md](CREDITS.md) and
[NOTICE](NOTICE).

EMI, Jade, and KubeJS integrations are optional and are not bundled.
