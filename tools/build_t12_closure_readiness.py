#!/usr/bin/env python3
"""Build the final T12 closure/readiness account."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

try:
    from tools import build_processing_machine_energy_audit as energy_audit_builder
except ModuleNotFoundError:
    import build_processing_machine_energy_audit as energy_audit_builder

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
OUTPUT = TOOLS / "t12_closure_readiness.json"
T12A = TOOLS / "t12a_machine_readiness.json"
MACHINE_POLICY = TOOLS / "t12_machine_policy.json"
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
    ROOT / "src/chemical_recipe_generated/resources",
    ROOT / "src/hydrocarbon_recipe_generated/resources",
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
T12_VERTICAL_SLICE_ORDER = (
    "centrifuge",
    "sifter",
    "electrolyzer",
)
T12_RUNTIME_ENERGY = {
    "RU": "KINETIC_ROTATION",
    "KU": "KINETIC_PUSH",
    "EU": "ELECTRIC",
}
T36_T12_KIND_ADDITIONS = {
    "cruciblecraft:tungstensteel_centrifuge",
    "cruciblecraft:tungstensteel_sifter",
    "cruciblecraft:chromium_electrolyzer",
    "cruciblecraft:titanium_electrolyzer",
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
            "variant.tierBand().inputMinimum()",
            "energy.stored()",
            "runtime.tickWork(",
            "workProgress",
        ),
    ),
    "machine_tier_data": (
        "src/main/resources/data/cruciblecraft/machine_tiers.json",
        (
            "\"schemaVersion\": 3",
            "\"source\"",
            "\"tierBand\"",
            "\"variant_rows\"",
            "MultiTileEntityBasicMachine.java:126-131,489-518,712-815",
            "defaults mEfficiency to 10000",
        ),
    ),
    "machine_tier_schema": (
        "src/main/resources/data/cruciblecraft/schema/machine_tiers.schema.json",
        (
            "Shared tier-band identity",
            "Complete machine variant identity",
            "Per-variant parallel limit",
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
    "state_v3": (
        "src/main/java/com/masson/cruciblecraft/machine/processing/ProcessingMachineState.java",
        (
            "VERSION = 3",
            "machine_kind",
            "tier_band",
            "tier_profile is unsupported",
            "version < VERSION",
            "identity_quarantine",
            "energy_identity",
        ),
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
    "coke_projection_source": (
        "src/main/resources/data/cruciblecraft/multiblock_structures/coke_oven.json",
        (
            "behavior_projection:NO_BEHAVIOR_DRIFT",
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


def historical_vertical_slice(policy: dict[str, Any]) -> list[dict[str, Any]]:
    vertical = policy.get("vertical_slice") or {}
    machine_order = vertical.get("machine_order")
    if machine_order != list(T12_VERTICAL_SLICE_ORDER):
        raise ValueError(
            "T12 historical vertical-slice order drifted: "
            f"{machine_order!r}"
        )

    machines = vertical.get("machines") or {}
    result: list[dict[str, Any]] = []
    for machine in T12_VERTICAL_SLICE_ORDER:
        definition = machines.get(machine) or {}
        energy_identity = definition.get("energy_identity")
        runtime_energy = T12_RUNTIME_ENERGY.get(energy_identity)
        tiers = definition.get("tiers")
        if runtime_energy is None or not isinstance(tiers, list) or len(tiers) != 3:
            raise ValueError(
                f"T12 historical vertical-slice definition drifted for {machine}"
            )
        for tier in tiers:
            source_tier = tier.get("source_tier")
            material = tier.get("material")
            nominal_input = tier.get("nominal_input")
            parallel = tier.get("parallel")
            if (
                not isinstance(source_tier, int)
                or not isinstance(material, str)
                or not isinstance(nominal_input, int)
                or not isinstance(parallel, int)
            ):
                raise ValueError(
                    f"T12 historical tier is incomplete for {machine}: {tier!r}"
                )
            result.append({
                "kind": f"cruciblecraft:{machine}",
                "source_tier": source_tier,
                "material": f"cruciblecraft:{material}",
                "energy": runtime_energy,
                "input_nominal": nominal_input,
                "parallel": parallel,
                "parallel_duration": bool(definition.get("parallel_duration")),
                "efficiency": definition.get("efficiency"),
            })

    if len(result) != 9:
        raise ValueError(f"T12 historical variant count drifted: {len(result)}")
    if len({
        (row["kind"], row["source_tier"], row["material"])
        for row in result
    }) != len(result):
        raise ValueError("T12 historical vertical slice contains duplicate rows")
    return result


def observe_live_catalog(
    tiers: dict[str, Any],
    historical_rows: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], dict[str, int], list[dict[str, Any]]]:
    historical_kinds = {row["kind"] for row in historical_rows}
    live_rows = [
        row
        for row in tiers.get("variants", [])
        if row.get("kind") in historical_kinds
    ]
    keyed: dict[tuple[str, int, str], list[dict[str, Any]]] = {}
    for row in live_rows:
        key = (
            row.get("kind"),
            row.get("sourceTier"),
            row.get("material"),
        )
        keyed.setdefault(key, []).append(row)

    selected_ids: set[str] = set()
    historical_observation: list[dict[str, Any]] = []
    for expected in historical_rows:
        key = (
            expected["kind"],
            expected["source_tier"],
            expected["material"],
        )
        matches = keyed.get(key, [])
        if len(matches) != 1:
            raise ValueError(
                "T12 historical catalog containment failed for "
                f"{key}: matches={len(matches)}"
            )
        row = matches[0]
        semantic_fields = {
            "energy": expected["energy"],
            "inputNominal": expected["input_nominal"],
            "parallel": expected["parallel"],
            "parallelDuration": expected["parallel_duration"],
            "efficiency": expected["efficiency"],
        }
        drift = {
            field: {"expected": expected_value, "actual": row.get(field)}
            for field, expected_value in semantic_fields.items()
            if row.get(field) != expected_value
        }
        if drift:
            raise ValueError(
                f"T12 historical catalog semantics drifted for {row.get('id')}: "
                f"{drift}"
            )
        variant_id = row.get("id")
        if not isinstance(variant_id, str):
            raise ValueError(f"T12 historical catalog row lacks id: {row!r}")
        selected_ids.add(variant_id)
        historical_observation.append({
            "id": variant_id,
            "kind": expected["kind"],
            "source_tier": expected["source_tier"],
            "material": expected["material"],
        })

    additions = [
        row for row in live_rows if row.get("id") not in selected_ids
    ]
    addition_ids = {row.get("id") for row in additions}
    if addition_ids != T36_T12_KIND_ADDITIONS:
        raise ValueError(
            "T12 live catalog additions require an explicit owner review: "
            f"{sorted(addition_ids)}"
        )
    by_kind: dict[str, int] = {}
    for row in live_rows:
        kind = row["kind"]
        by_kind[kind] = by_kind.get(kind, 0) + 1
    additions.sort(key=lambda row: str(row["id"]))
    return historical_observation, by_kind, additions


def build() -> dict[str, Any]:
    t12a = load(T12A)
    if t12a["status"] != "T12A_READY":
        raise ValueError(
            "T12 closure requires immutable historical T12A_READY evidence"
        )
    policy = load(MACHINE_POLICY)
    historical_slice = historical_vertical_slice(policy)
    tiers = load(MACHINE_TIERS)
    historical_observation, live_by_kind, t36_additions = observe_live_catalog(
        tiers,
        historical_slice,
    )
    by_kind: dict[str, int] = {}
    for variant in historical_slice:
        by_kind[variant["kind"]] = by_kind.get(variant["kind"], 0) + 1
    if sorted(by_kind.values()) != [3, 3, 3]:
        raise ValueError(f"T12 historical tier shape drifted: {by_kind}")

    structure_paths = sorted(STRUCTURES.glob("*.json"))
    structure_ids = [path.stem for path in structure_paths]
    if structure_ids != ["coke_oven", "distillation_tower", "large_boiler", "large_centrifuge", "large_crucible", "tank_3x3x3"]:
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

    audit_errors = energy_audit_builder.check()
    if audit_errors:
        raise ValueError(
            "T12 processing-machine energy audit is stale: "
            + "; ".join(audit_errors)
        )
    energy_audit = load(energy_audit_builder.OUTPUT)
    audit_counts = energy_audit.get("counts") or {}
    if (
        energy_audit.get("status")
        != "PROCESSING_MACHINE_ENERGY_AUDIT_READY"
        or audit_counts.get("machine_specs") != 25
        or audit_counts.get("implicit_energy_arguments") != 0
        or audit_counts.get("legacy_kinetic") != 4
        or audit_counts.get("new_legacy_kinetic") != 0
    ):
        raise ValueError("T12 processing-machine energy audit is incomplete")

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
            "variants": len(historical_slice),
            "kinds": by_kind,
            "historical_vertical_slice": {
                "variants": len(historical_slice),
                "rows": historical_observation,
            },
            "live_catalog_observation": {
                "owner": "T36",
                "variants": sum(live_by_kind.values()),
                "kinds": live_by_kind,
                "included_in_t12_closure_claim": False,
                "additional_variants": [
                    {
                        "id": row["id"],
                        "kind": row["kind"],
                        "source_tier": row["sourceTier"],
                        "material": row["material"],
                    }
                    for row in t36_additions
                ],
            },
            "publication_delta": 0,
            "authored_material_rule_budget": 0,
            "state_schema_version": 3,
            "tier_identity_field": "tier_band",
            "legacy_tier_identity_field": "tier_profile",
        },
        "energy": {
            "identities": ["RU", "KU", "EU"],
            "ru_topology": ["electric_motor", "rotational_axle", "rotational_gearbox"],
            "ku_topology": "adjacent_push",
            "eu_topology": "existing_cable_network",
            "processing_machine_audit": {
                "path": energy_audit_builder.OUTPUT.relative_to(
                    ROOT
                ).as_posix(),
                # No sha256 here: the audit is written by builder #24, after
                # this closure (#23).  The audit itself validates these counts
                # against this recorded block as a forward edge 23 -> 24.
                "status": energy_audit["status"],
                "machine_specs": audit_counts["machine_specs"],
                "explicit_energy_arguments": audit_counts[
                    "explicit_energy_arguments"
                ],
                "implicit_energy_arguments": audit_counts[
                    "implicit_energy_arguments"
                ],
                "legacy_kinetic": audit_counts["legacy_kinetic"],
                "new_legacy_kinetic": audit_counts["new_legacy_kinetic"],
            },
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
        "currentness": {
            "owned_inputs": {
                BUILDER.relative_to(ROOT).as_posix(): digest(BUILDER),
                MACHINE_POLICY.relative_to(ROOT).as_posix():
                    digest(MACHINE_POLICY),
                MACHINE_TIERS.relative_to(ROOT).as_posix(): digest(MACHINE_TIERS),
            },
            "dependencies": {
                T12A.relative_to(ROOT).as_posix(): digest(T12A),
            },
            "pending_report": {
                "status": "BOUND_TO_FULL_VERIFICATION_REPORT",
                "this_refresh_final_closure_attempted": True,
                "pending": [],
                "evidence": "tools/full_verification_report.json",
            },
        },
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
        OUTPUT.write_bytes(encoded.encode("utf-8"))
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
