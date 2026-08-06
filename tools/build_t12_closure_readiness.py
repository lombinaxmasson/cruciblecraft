#!/usr/bin/env python3
"""Build the final T12 closure/readiness account."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t12_closure_readiness.json"
T12A = TOOLS / "t12a_machine_readiness.json"
BENCHMARK = TOOLS / "t12_capacity_matcher_benchmark.json"
MACHINE_TIERS = (
    ROOT
    / "src/main/resources/data/cruciblecraft/machine_tiers.json"
)
STRUCTURES = (
    ROOT
    / "src/main/resources/data/cruciblecraft/multiblock_structures"
)
RESOURCE_ROOTS = (
    ROOT / "src/main/resources",
    ROOT / "src/generated/resources",
    ROOT / "src/ore_chain_generated/resources",
    ROOT / "src/worldgen_generated/resources",
    ROOT / "src/worldgen_catalog_generated/resources",
    ROOT / "src/component_rule_generated/resources",
    ROOT / "src/t5_chemical_generated/resources",
    ROOT / "src/t11_hydrocarbon_generated/resources",
)
T16_RECIPE_PATHS = {
    "data/cruciblecraft/recipe/machines/steel_lathe.json",
    "data/cruciblecraft/recipe/machines/titanium_lathe.json",
    "data/cruciblecraft/recipe/machines/steel_rollingmill.json",
    "data/cruciblecraft/recipe/machines/titanium_rollingmill.json",
    "data/cruciblecraft/recipe/machines/steel_wiremill.json",
    "data/cruciblecraft/recipe/machines/titanium_wiremill.json",
    "data/cruciblecraft/recipe/machines/steel_shredder.json",
    "data/cruciblecraft/recipe/machines/titanium_shredder.json",
    "data/cruciblecraft/recipe/machines/steel_press.json",
    "data/cruciblecraft/recipe/machines/titanium_press.json",
}
SOURCE_CONTRACTS = {
    "energy_types": (
        "src/main/java/com/masson/cruciblecraft/api/energy/EnergyType.java",
        ("KINETIC_ROTATION", "KINETIC_PUSH"),
    ),
    "kind_tier": (
        "src/main/java/com/masson/cruciblecraft/registry/ModMachineVariants.java",
        ("MachineKindSpec", "MachineTierCatalog", "CENTRIFUGE", "SIFTER", "ELECTROLYZER"),
    ),
    "execution_plan": (
        "src/main/java/com/masson/cruciblecraft/machine/processing/MachineExecutionPlan.java",
        ("parallelDuration", "OverclockPolicy.STANDARD", "totalWork"),
    ),
    "processing_runtime_window": (
        "src/main/java/com/masson/cruciblecraft/content/blockentity/ProcessingMachineBlockEntity.java",
        (
            "variant.tier().inputMinimum()",
            "energy.stored()",
            "runtime.tickWork(",
            "workProgress",
        ),
    ),
    "machine_tier_data": (
        "src/main/resources/data/cruciblecraft/machine_tiers.json",
        (
            "\"source\"",
            "\"variant_rows\"",
            "MultiTileEntityBasicMachine.java:126-131,489-518,712-815",
            "defaults mEfficiency to 10000",
        ),
    ),
    "machine_tier_loader": (
        "src/main/java/com/masson/cruciblecraft/machine/processing/MachineTierCatalog.java",
        (
            "document.source.complete()",
            "Machine tier source rows do not cover variants",
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
        ),
    ),
    "state_v2": (
        "src/main/java/com/masson/cruciblecraft/machine/processing/ProcessingMachineState.java",
        ("VERSION = 2", "machine_kind", "tier_profile", "energy_identity"),
    ),
    "ru_axle": (
        "src/main/java/com/masson/cruciblecraft/content/blockentity/RotationalAxleBlockEntity.java",
        ("KINETIC_ROTATION", "MAX_POWER = 4L", "MAX_PACKET = 64L"),
    ),
    "ru_gearbox": (
        "src/main/java/com/masson/cruciblecraft/content/blockentity/RotationalGearboxBlockEntity.java",
        ("KINETIC_ROTATION", "MAX_PACKET = 64L", "overloaded"),
    ),
    "ru_producer": (
        "src/main/java/com/masson/cruciblecraft/content/blockentity/ElectricMotorBlockEntity.java",
        ("INPUT_NOMINAL = 32L", "OUTPUT_SIZE = 16L", "KINETIC_ROTATION"),
    ),
    "multiblock_validator": (
        "src/main/java/com/masson/cruciblecraft/content/multiblock/MultiblockStructureValidator.java",
        ("UNLOADED", "MAX_DIAGNOSTICS", "worldPosition"),
    ),
    "coke_migration_source": (
        "src/main/resources/data/cruciblecraft/multiblock_structures/coke_oven.json",
        (
            "behavior_migration:NO_BEHAVIOR_DRIFT",
            "t12a_machine_readiness.json#structure_projection.coke_oven",
            "CokeOvenStructure",
        ),
    ),
    "game_tests": (
        "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java",
        (
            "largeCentrifugeUsesJsonPortsAndProcessingHost",
            "t12RuAxleGearboxPowersTieredCentrifuge",
            "t12TierProfilesExposeDistinctFailureStates",
            "steamEnginePowersSifterThroughKu",
        ),
    ),
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(document: Any) -> str:
    return json.dumps(
        document, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def datapack_recipe_entries() -> int:
    paths: set[str] = set()
    for root in RESOURCE_ROOTS:
        if not root.is_dir():
            continue
        for path in root.glob("data/*/recipe/**/*.json"):
            relative = path.relative_to(root).as_posix()
            if relative not in T16_RECIPE_PATHS:
                paths.add(relative)
    return len(paths)


def post_t12_virtualized_recipe_entries() -> int:
    readiness = TOOLS / "t14_extruder_readiness.json"
    if not readiness.is_file():
        return 0
    document = load(readiness)
    if document.get("status") != "READY":
        raise ValueError("T14 virtualized recipe readiness is not closed")
    delta = document["compression"]["datapack_entry_delta"]
    if delta > 0:
        raise ValueError("T14 datapack compaction delta must not be positive")
    return -delta


def source_contracts() -> dict[str, Any]:
    result: dict[str, Any] = {}
    for owner, (relative, required) in SOURCE_CONTRACTS.items():
        path = ROOT / relative
        source = path.read_text(encoding="utf-8")
        missing = [token for token in required if token not in source]
        if missing:
            raise ValueError(
                f"T12 source contract {owner} is incomplete: {missing}"
            )
        result[owner] = {
            "path": relative,
            "sha256": digest(path),
            "required_tokens": list(required),
        }
    return result


def build() -> dict[str, Any]:
    t12a = load(T12A)
    if t12a["status"] != "T12A_READY":
        raise ValueError(
            "T12 closure requires immutable historical T12A_READY evidence"
        )
    tiers = load(MACHINE_TIERS)
    t12_kinds = {
        "cruciblecraft:centrifuge",
        "cruciblecraft:sifter",
        "cruciblecraft:electrolyzer",
    }
    variants = [
        row
        for row in tiers.get("variants", [])
        if row.get("kind") in t12_kinds
    ]
    if len(variants) != 9:
        raise ValueError(f"T12 variant count drifted: {len(variants)}")
    by_kind: dict[str, int] = {}
    for variant in variants:
        by_kind[variant["kind"]] = by_kind.get(variant["kind"], 0) + 1
    if sorted(by_kind.values()) != [3, 3, 3]:
        raise ValueError(f"T12 tier shape drifted: {by_kind}")

    structure_paths = sorted(STRUCTURES.glob("*.json"))
    structure_ids = [path.stem for path in structure_paths]
    if structure_ids != ["coke_oven", "large_centrifuge"]:
        raise ValueError(f"T12 structure set drifted: {structure_ids}")
    if (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/multiblock/CokeOvenStructure.java"
    ).exists() or (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/multiblock/CokeOvenStructureLayout.java"
    ).exists():
        raise ValueError("Coke Oven still has a dedicated structure class")

    benchmark = load(BENCHMARK)
    failed_benchmarks = [
        row["id"]
        for row in benchmark["scenarios"]
        if not row["within_budget"]
    ]
    if failed_benchmarks:
        raise ValueError(
            f"T12 matcher benchmarks exceed budget: {failed_benchmarks}"
        )

    concrete_entries = datapack_recipe_entries()
    virtualized_entries = post_t12_virtualized_recipe_entries()
    entries = concrete_entries + virtualized_entries
    if entries > 6_600:
        raise ValueError(f"T12 datapack entries exceed budget: {entries}")
    published = t12a["load_gate"]["current"]["published_recipes"]
    if published != 18_875:
        raise ValueError(
            f"T12 changed tier-neutral publication: {published}"
        )
    contracts = source_contracts()
    return {
        "schema_version": 1,
        "status": "T12_READY",
        "source_revision": t12a["gt6_source"]["revision"],
        "t12a_historical_evidence": {
            "path": T12A.relative_to(ROOT).as_posix(),
            "sha256": digest(T12A),
            "status": t12a["status"],
            "load_status": t12a["load_gate"]["status"],
            "current": False,
            "immutable": True,
            "processing_kinds": t12a["processing_kind_ledger"]["counts"],
            "non_spec_kinds": t12a["non_spec_kind_ledger"]["counts"],
        },
        "kind_tier": {
            "variants": len(variants),
            "kinds": by_kind,
            "publication_delta": 0,
            "authored_material_rule_budget": 0,
            "state_schema_version": 2,
        },
        "energy": {
            "identities": ["RU", "KU", "EU"],
            "ru_topology": ["electric_motor", "rotational_axle", "rotational_gearbox"],
            "ku_topology": "adjacent_push",
            "eu_topology": "existing_cable_network",
        },
        "multiblock": {
            "structures": structure_ids,
            "dedicated_coke_structure_classes": 0,
            "large_centrifuge_scan_volume": 18,
            "diagnostic_limit": 16,
        },
        "matcher": {
            "artifact": BENCHMARK.relative_to(ROOT).as_posix(),
            "sha256": digest(BENCHMARK),
            "all_within_budget": True,
            "presence_supply_cap": 12,
            "historical_t12_claim": {
                "current": False,
                "superseded_statement": (
                    "The former T12 closure described six runtime supplies "
                    "without a source parser."
                ),
                "reason_superseded": (
                    "It conflated an undocumented closure-era count with the "
                    "live matcher host layout."
                ),
            },
            "t15_corrected_authority": {
                "owner": "T15e",
                "artifact": "tools/t15_matcher_boundary.json",
                "derivation": (
                    "live structure JSON + ModProcessingMachines.CENTRIFUGE "
                    "parser"
                ),
                "currentness_claimed_here": False,
            },
        },
        "load_gate": {
            "datapack_recipe_entries": entries,
            "concrete_datapack_recipe_entries": concrete_entries,
            "post_t12_virtualized_recipe_entries": virtualized_entries,
            "datapack_recipe_entry_budget": 6_600,
            "published_recipes": published,
            "published_recipe_budget": 21_000,
            "compression_ratio": round(published / entries, 6),
            "minimum_compression_ratio": 3.0,
        },
        "runtime_acceptance": {
            "expected_game_tests": 53,
            "routes": [
                "three_machine_three_tier_catalog",
                "steam_engine_to_ku_sifter",
                "electric_motor_to_axle_to_gearbox_to_ru_centrifuge",
                "eu_cable_to_electrolyzer",
                "json_coke_oven",
                "json_large_centrifuge",
            ],
        },
        "source_contracts": contracts,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    encoded = stable(build())
    if args.check:
        if not OUTPUT.is_file() or OUTPUT.read_text(
            encoding="utf-8"
        ) != encoded:
            raise SystemExit("T12 closure readiness is stale")
    else:
        OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
    print(
        json.dumps(
            {
                "output": OUTPUT.relative_to(ROOT).as_posix(),
                "status": json.loads(encoded)["status"],
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
