#!/usr/bin/env python3
"""Semantic-wave closeout specs: slug identity, unique_active_wave, next_unassigned."""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

from tools import io_common as io
from tools.recipe_bulk.slugs import parse_wave_token

TOOLS = io.TOOLS
WAVES_ROOT = TOOLS / "waves"


@dataclass(frozen=True)
class WaveCloseoutSpec:
    wave_slug: str
    census: Path
    topology: Path
    readiness: Path
    receipt: Path | None
    production_lock: Path | None
    generated_root: Path | None
    support_root: Path | None
    gametest_java: Path | None
    gametest_log: Path | None
    publication_group_manifest: Path | None
    shard_manifest: Path | None
    runtime_dependency_manifest: Path | None
    complete_key: str
    unique_active_wave: str | None
    next_unassigned: bool
    owns_families: int = 0


def wave_dir(slug: str) -> Path:
    parse_wave_token(slug, schema="semantic-v3")
    return WAVES_ROOT / slug


def seal_path(slug: str) -> Path:
    if slug == "semantic-wave-bootstrap":
        return WAVES_ROOT / slug / "closeout_seal.json"
    return wave_dir(slug) / "closeout_seal.json"


def _wave_specs() -> dict[str, WaveCloseoutSpec]:
    bootstrap = WAVES_ROOT / "semantic-wave-bootstrap"
    allocation = WAVES_ROOT / "runtime-load" / "allocation-split"
    smelter = WAVES_ROOT / "smelter" / "ordinary-closure"
    mixer = WAVES_ROOT / "mixer" / "ordinary-closure"
    naming = WAVES_ROOT / "naming" / "active-semantic-migration"
    repair = WAVES_ROOT / "ordinary-wave" / "closeout-integrity-repair"
    foundation = WAVES_ROOT / "ordinary-remainder" / "operand-foundation"
    program = WAVES_ROOT / "recipe-portfolio" / "ordinary-remainder-closure"
    recycling_r0 = WAVES_ROOT / "recycling" / "deferred-ordinary-ledger-r0"
    smelter_identity = WAVES_ROOT / "recycling" / "smelter-mte-identity"
    smelter_deferred = WAVES_ROOT / "smelter" / "deferred-recycling"
    smelter_edge = WAVES_ROOT / "smelter" / "deferred-recycling-edge"
    autoclave_deferred = WAVES_ROOT / "autoclave" / "deferred-recycling"
    non_recycling = WAVES_ROOT / "recycling" / "non-recycling-scope"
    recycling_program = WAVES_ROOT / "recycling" / "deferred-ordinary-runtime"

    def _portfolio(slug: str, unique_active: str | None, next_unassigned: bool) -> WaveCloseoutSpec:
        root = WAVES_ROOT / slug
        return WaveCloseoutSpec(
            wave_slug=slug,
            census=root / "census_delta.json",
            topology=root / "topology.json",
            readiness=root / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=unique_active,
            next_unassigned=next_unassigned,
            owns_families=0,
        )

    generated = io.ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe"
    support = io.ROOT / "src/recipe_support_generated/resources/data/cruciblecraft/recipe"

    def _ordinary(host: str) -> WaveCloseoutSpec:
        slug = f"{host}/ordinary-closure"
        root = WAVES_ROOT / host / "ordinary-closure"
        return WaveCloseoutSpec(
            wave_slug=slug,
            census=root / "census_delta.json",
            topology=root / "topology.json",
            readiness=root / "readiness.json",
            receipt=root / "gametest_receipt.json",
            production_lock=root / "production_lock.json",
            generated_root=generated / host / "ordinary_closure",
            support_root=support / host / "ordinary_closure",
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / f"{host.title()}OrdinaryClosureGameTests.java"
            ),
            gametest_log=root / "gametest.log",
            publication_group_manifest=root / "publication_group_manifest.json",
            shard_manifest=root / "shard_manifest.json",
            runtime_dependency_manifest=root / "runtime_dependency_manifest.json",
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        )

    return {
        "ordinary-wave/closeout-integrity-repair": WaveCloseoutSpec(
            wave_slug="ordinary-wave/closeout-integrity-repair",
            census=repair / "census_delta.json",
            topology=repair / "topology.json",
            readiness=repair / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime-load/allocation-split": WaveCloseoutSpec(
            wave_slug="runtime-load/allocation-split",
            census=allocation / "census_delta.json",
            topology=allocation / "topology.json",
            readiness=allocation / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="allocation_split_complete",
            unique_active_wave="smelter/ordinary-closure",
            next_unassigned=False,
            owns_families=0,
        ),
        "smelter/ordinary-closure": WaveCloseoutSpec(
            wave_slug="smelter/ordinary-closure",
            census=smelter / "census_delta.json",
            topology=smelter / "topology.json",
            readiness=smelter / "readiness.json",
            receipt=smelter / "gametest_receipt.json",
            production_lock=smelter / "production_lock.json",
            generated_root=generated / "smelter" / "ordinary_closure",
            support_root=support / "smelter" / "ordinary_closure",
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "SmelterOrdinaryClosureGameTests.java"
            ),
            gametest_log=smelter / "gametest.log",
            publication_group_manifest=smelter / "publication_group_manifest.json",
            shard_manifest=smelter / "shard_manifest.json",
            runtime_dependency_manifest=smelter / "runtime_dependency_manifest.json",
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "mixer/ordinary-closure": WaveCloseoutSpec(
            wave_slug="mixer/ordinary-closure",
            census=mixer / "census_delta.json",
            topology=mixer / "topology.json",
            readiness=mixer / "readiness.json",
            receipt=mixer / "gametest_receipt.json",
            production_lock=mixer / "production_lock.json",
            generated_root=generated / "mixer" / "ordinary_closure",
            support_root=support / "mixer" / "ordinary_closure",
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "MixerOrdinaryClosureGameTests.java"
            ),
            gametest_log=mixer / "gametest.log",
            publication_group_manifest=mixer / "publication_group_manifest.json",
            shard_manifest=mixer / "shard_manifest.json",
            runtime_dependency_manifest=mixer / "runtime_dependency_manifest.json",
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "ordinary-remainder/operand-foundation": WaveCloseoutSpec(
            wave_slug="ordinary-remainder/operand-foundation",
            census=foundation / "census_delta.json",
            topology=foundation / "topology.json",
            readiness=foundation / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "recipe-portfolio/ordinary-remainder-closure": WaveCloseoutSpec(
            wave_slug="recipe-portfolio/ordinary-remainder-closure",
            census=program / "census_delta.json",
            topology=program / "topology.json",
            readiness=program / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "recycling/deferred-ordinary-ledger-r0": WaveCloseoutSpec(
            wave_slug="recycling/deferred-ordinary-ledger-r0",
            census=recycling_r0 / "census_delta.json",
            topology=recycling_r0 / "topology.json",
            readiness=recycling_r0 / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave="recycling/smelter-mte-identity",
            next_unassigned=False,
            owns_families=0,
        ),
        "recycling/smelter-mte-identity": WaveCloseoutSpec(
            wave_slug="recycling/smelter-mte-identity",
            census=smelter_identity / "census_delta.json",
            topology=smelter_identity / "topology.json",
            readiness=smelter_identity / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave="smelter/deferred-recycling",
            next_unassigned=False,
            owns_families=0,
        ),
        "smelter/deferred-recycling": WaveCloseoutSpec(
            wave_slug="smelter/deferred-recycling",
            census=smelter_deferred / "census_delta.json",
            topology=smelter_deferred / "topology.json",
            readiness=smelter_deferred / "readiness.json",
            receipt=smelter_deferred / "gametest_receipt.json",
            production_lock=smelter_deferred / "production_lock.json",
            generated_root=generated / "smelter" / "deferred_recycling",
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "SmelterDeferredRecyclingGameTests.java"
            ),
            gametest_log=smelter_deferred / "gametest.log",
            publication_group_manifest=smelter_deferred
            / "publication_group_manifest.json",
            shard_manifest=smelter_deferred / "shard_manifest.json",
            runtime_dependency_manifest=smelter_deferred
            / "runtime_dependency_manifest.json",
            complete_key="wave_complete",
            unique_active_wave="smelter/deferred-recycling-edge",
            next_unassigned=False,
            owns_families=1817,
        ),
        "smelter/deferred-recycling-edge": WaveCloseoutSpec(
            wave_slug="smelter/deferred-recycling-edge",
            census=smelter_edge / "census_delta.json",
            topology=smelter_edge / "topology.json",
            readiness=smelter_edge / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave="autoclave/deferred-recycling",
            next_unassigned=False,
            owns_families=2,
        ),
        "autoclave/deferred-recycling": WaveCloseoutSpec(
            wave_slug="autoclave/deferred-recycling",
            census=autoclave_deferred / "census_delta.json",
            topology=autoclave_deferred / "topology.json",
            readiness=autoclave_deferred / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave="recycling/non-recycling-scope",
            next_unassigned=False,
            owns_families=24,
        ),
        "recycling/non-recycling-scope": WaveCloseoutSpec(
            wave_slug="recycling/non-recycling-scope",
            census=non_recycling / "census_delta.json",
            topology=non_recycling / "topology.json",
            readiness=non_recycling / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave="recycling/deferred-ordinary-runtime",
            next_unassigned=False,
            owns_families=2,
        ),
        "recycling/deferred-ordinary-runtime": WaveCloseoutSpec(
            wave_slug="recycling/deferred-ordinary-runtime",
            census=recycling_program / "census_delta.json",
            topology=recycling_program / "topology.json",
            readiness=recycling_program / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "portfolio/one-x-exit-r0": _portfolio(
            "portfolio/one-x-exit-r0",
            "portfolio/census-disposition-replay",
            False,
        ),
        "portfolio/census-disposition-replay": _portfolio(
            "portfolio/census-disposition-replay",
            "portfolio/energy-matrix-replay",
            False,
        ),
        "portfolio/energy-matrix-replay": _portfolio(
            "portfolio/energy-matrix-replay",
            "portfolio/storage-currentness-replay",
            False,
        ),
        "portfolio/storage-currentness-replay": _portfolio(
            "portfolio/storage-currentness-replay",
            "portfolio/load-ceiling-interpretation",
            False,
        ),
        "portfolio/load-ceiling-interpretation": _portfolio(
            "portfolio/load-ceiling-interpretation",
            "portfolio/one-x-joint-exit",
            False,
        ),
        "portfolio/one-x-joint-exit": _portfolio(
            "portfolio/one-x-joint-exit",
            None,
            True,
        ),
        "portfolio/source-capability-map-r0": _portfolio(
            "portfolio/source-capability-map-r0",
            "portfolio/source-capability-inventory",
            False,
        ),
        "portfolio/source-capability-inventory": _portfolio(
            "portfolio/source-capability-inventory",
            "portfolio/source-capability-growth-order",
            False,
        ),
        "portfolio/source-capability-growth-order": _portfolio(
            "portfolio/source-capability-growth-order",
            "portfolio/source-capability-map",
            False,
        ),
        "portfolio/source-capability-map": _portfolio(
            "portfolio/source-capability-map",
            None,
            True,
        ),
        "portfolio/generic-recipe-generator-r0": _portfolio(
            "portfolio/generic-recipe-generator-r0",
            "portfolio/generic-recipe-import-core",
            False,
        ),
        "portfolio/generic-recipe-import-core": _portfolio(
            "portfolio/generic-recipe-import-core",
            "portfolio/generic-recipe-import-proof",
            False,
        ),
        "portfolio/generic-recipe-import-proof": _portfolio(
            "portfolio/generic-recipe-import-proof",
            "portfolio/generic-recipe-generator",
            False,
        ),
        "portfolio/generic-recipe-generator": _portfolio(
            "portfolio/generic-recipe-generator",
            None,
            True,
        ),
        "portfolio/logistics-cover-net-r0": _portfolio(
            "portfolio/logistics-cover-net-r0",
            None,
            True,
        ),
        "portfolio/exclusion-reclaim-r0": _portfolio(
            "portfolio/exclusion-reclaim-r0",
            None,
            True,
        ),
        "portfolio/non-ore-worldgen-r0": _portfolio(
            "portfolio/non-ore-worldgen-r0",
            None,
            True,
        ),
        "portfolio/vanilla-replace-r0": _portfolio(
            "portfolio/vanilla-replace-r0",
            None,
            True,
        ),
        "portfolio/crops-food-bees-r0": _portfolio(
            "portfolio/crops-food-bees-r0",
            None,
            True,
        ),
        "runtime/item-network-core": WaveCloseoutSpec(
            wave_slug="runtime/item-network-core",
            census=WAVES_ROOT / "runtime" / "item-network-core" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "item-network-core" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "item-network-core" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "item-network-core" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "ItemNetworkCoreGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "item-network-core" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime/fluid-network-basic-transfer": WaveCloseoutSpec(
            wave_slug="runtime/fluid-network-basic-transfer",
            census=WAVES_ROOT / "runtime" / "fluid-network-basic-transfer" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "fluid-network-basic-transfer" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "fluid-network-basic-transfer" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "fluid-network-basic-transfer" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "FluidNetworkCoreGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "fluid-network-basic-transfer" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime/generic-network-core": WaveCloseoutSpec(
            wave_slug="runtime/generic-network-core",
            census=WAVES_ROOT / "runtime" / "generic-network-core" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "generic-network-core" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "generic-network-core" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "generic-network-core" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "GenericNetworkCoreGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "generic-network-core" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime/logistics-core": WaveCloseoutSpec(
            wave_slug="runtime/logistics-core",
            census=WAVES_ROOT / "runtime" / "logistics-core" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "logistics-core" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "logistics-core" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "logistics-core" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "LogisticsCoreGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "logistics-core" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime/display-cpu": WaveCloseoutSpec(
            wave_slug="runtime/display-cpu",
            census=WAVES_ROOT / "runtime" / "display-cpu" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "display-cpu" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "display-cpu" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "display-cpu" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "DisplayCpuGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "display-cpu" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime/converter-catalog": WaveCloseoutSpec(
            wave_slug="runtime/converter-catalog",
            census=WAVES_ROOT / "runtime" / "converter-catalog" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "converter-catalog" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "converter-catalog" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "converter-catalog" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "EnergyConverterCatalogGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "converter-catalog" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime/batteries": WaveCloseoutSpec(
            wave_slug="runtime/batteries",
            census=WAVES_ROOT / "runtime" / "batteries" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "batteries" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "batteries" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "batteries" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "EnergyBatteriesGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "batteries" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime/transformers": WaveCloseoutSpec(
            wave_slug="runtime/transformers",
            census=WAVES_ROOT / "runtime" / "transformers" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "transformers" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "transformers" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "transformers" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "EnergyTransformersGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "transformers" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime/fission-hot-fluids": WaveCloseoutSpec(
            wave_slug="runtime/fission-hot-fluids",
            census=WAVES_ROOT / "runtime" / "fission-hot-fluids" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "fission-hot-fluids" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "fission-hot-fluids" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "fission-hot-fluids" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "NuclearFissionHotFluidsGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "fission-hot-fluids" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "runtime/fission-survival": WaveCloseoutSpec(
            wave_slug="runtime/fission-survival",
            census=WAVES_ROOT / "runtime" / "fission-survival" / "census_delta.json",
            topology=WAVES_ROOT / "runtime" / "fission-survival" / "topology.json",
            readiness=WAVES_ROOT / "runtime" / "fission-survival" / "readiness.json",
            receipt=WAVES_ROOT / "runtime" / "fission-survival" / "gametest_receipt.json",
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=(
                io.ROOT
                / "src/main/java/com/masson/cruciblecraft/gametest"
                / "NuclearFissionGameTests.java"
            ),
            gametest_log=(
                WAVES_ROOT / "runtime" / "fission-survival" / "gametest.log"
            ),
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="wave_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "drying/ordinary-closure": _ordinary("drying"),
        "electrolyzer/ordinary-closure": _ordinary("electrolyzer"),
        "centrifuge/ordinary-closure": _ordinary("centrifuge"),
        "autoclave/ordinary-closure": _ordinary("autoclave"),
        "compressor/ordinary-closure": _ordinary("compressor"),
        "naming/active-semantic-migration": WaveCloseoutSpec(
            wave_slug="naming/active-semantic-migration",
            census=naming / "census_delta.json",
            topology=naming / "topology.json",
            readiness=naming / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="naming_complete",
            unique_active_wave=None,
            next_unassigned=True,
            owns_families=0,
        ),
        "semantic-wave-bootstrap": WaveCloseoutSpec(
            wave_slug="semantic-wave-bootstrap",
            census=bootstrap / "census_delta.json",
            topology=bootstrap / "topology.json",
            readiness=bootstrap / "readiness.json",
            receipt=None,
            production_lock=None,
            generated_root=None,
            support_root=None,
            gametest_java=None,
            gametest_log=None,
            publication_group_manifest=None,
            shard_manifest=None,
            runtime_dependency_manifest=None,
            complete_key="bootstrap_complete",
            unique_active_wave="runtime-load/allocation-split",
            next_unassigned=False,
            owns_families=0,
        ),
    }


def spec_for(slug: str) -> WaveCloseoutSpec:
    specs = _wave_specs()
    if slug == "semantic-wave-bootstrap":
        return specs[slug]
    parse_wave_token(slug, schema="semantic-v3")
    if slug not in specs:
        raise ValueError(f"no semantic closeout spec for {slug}")
    return specs[slug]


def known_slugs() -> tuple[str, ...]:
    return tuple(_wave_specs())
