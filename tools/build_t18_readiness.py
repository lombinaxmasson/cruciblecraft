#!/usr/bin/env python3
"""Build the staged T18 readiness artifact without final closure."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

try:
    from tools import build_t18_machine_energy_denominator as denominator_builder
except ModuleNotFoundError:
    import build_t18_machine_energy_denominator as denominator_builder
try:
    from tools import build_t18_converter_acquisition as acquisition_builder
except ModuleNotFoundError:
    import build_t18_converter_acquisition as acquisition_builder
try:
    from tools import build_t17_machine_denominator as publication_support
except ModuleNotFoundError:
    import build_t17_machine_denominator as publication_support
try:
    from tools import recipe_load_projection
except ModuleNotFoundError:
    import recipe_load_projection


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t18_readiness_policy.json"
OUTPUT = TOOLS / "t18_readiness.json"
CONVERTERS = (
    ROOT / "src/main/resources/data/cruciblecraft/energy_converters.json"
)
LOAD_INPUT = TOOLS / "t18_load_projection_input.json"
LOAD_OUTPUT = TOOLS / "t18_load_projection.json"
PUBLICATION_BASELINE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/t18_publication_baseline.json"
)
T17_PUBLICATION_BASELINE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/t17_publication_baseline.json"
)
T14_READINESS = TOOLS / "t14_readiness.json"
O37_PROJECTION = TOOLS / "t18_o37_identity_projection.json"
GAMETESTS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/"
    "CrucibleCraftGameTests.java"
)
STAGE_ORDER = ("T18a", "T18b", "T18c", "T18d")


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T18_READINESS_POLICY"
    ):
        raise ValueError("T18 readiness policy header drifted")
    stages = policy.get("stages") or {}
    if tuple(stages) != STAGE_ORDER:
        raise ValueError("T18 readiness stages must remain ordered T18a-d")
    complete = policy["status_policy"]["complete_stage_status"]
    pending = policy["status_policy"]["pending_stage_status"]
    pending_seen = False
    for stage, row in stages.items():
        status = row.get("status")
        if status not in {complete, pending}:
            raise ValueError(f"{stage}: invalid readiness status")
        if not str(row.get("acceptance") or "").strip():
            raise ValueError(f"{stage}: acceptance is missing")
        if status == pending:
            pending_seen = True
            for field in (
                "reason",
                "replacement_condition",
                "recheck_point",
            ):
                if not str(row.get(field) or "").strip():
                    raise ValueError(
                        f"{stage}: pending gate lacks {field}"
                    )
        elif pending_seen:
            raise ValueError(
                f"{stage}: cannot complete after an earlier pending gate"
            )
    if any(stages[stage]["status"] != complete for stage in STAGE_ORDER):
        raise ValueError("T18 readiness requires T18a-d complete")


def stage_summary(
    policy: dict[str, Any],
) -> tuple[str, list[str], list[str]]:
    validate_policy(policy)
    complete = policy["status_policy"]["complete_stage_status"]
    completed = [
        stage for stage in STAGE_ORDER
        if policy["stages"][stage]["status"] == complete
    ]
    pending = [stage for stage in STAGE_ORDER if stage not in completed]
    status = (
        policy["status_policy"]["ready"]
        if not pending
        else policy["status_policy"]["in_progress"]
    )
    return status, completed, pending


def dependencies(
    policy: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for owner, spec in policy["dependencies"].items():
        path = ROOT / spec["path"]
        document = load(path)
        if document.get("status") != spec["required_status"]:
            raise ValueError(
                f"{owner}: expected {spec['required_status']}"
            )
        result[owner] = {
            "path": spec["path"],
            "status": document["status"],
            "sha256": sha256(path),
        }
    errors = denominator_builder.check()
    if errors:
        raise ValueError("T18 denominator is stale: " + "; ".join(errors))
    errors = acquisition_builder.check()
    if errors:
        raise ValueError("T18 acquisition is stale: " + "; ".join(errors))
    return result


def source_contracts(
    policy: dict[str, Any],
    completed: list[str],
) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for owner, contract in policy["source_contracts"].items():
        stage = contract["stage"]
        if stage not in completed:
            continue
        path = ROOT / contract["path"]
        source = path.read_text(encoding="utf-8")
        missing = [
            token for token in contract.get("required_tokens", [])
            if token not in source
        ]
        if missing:
            raise ValueError(f"{owner}: missing source tokens {missing}")
        result[owner] = {
            "stage": stage,
            "path": contract["path"],
            "sha256": sha256(path),
            "required_tokens": contract.get("required_tokens", []),
        }
    expected = {
        owner for owner, row in policy["source_contracts"].items()
        if row["stage"] in completed
    }
    if set(result) != expected:
        raise ValueError("completed T18 source currentness is incomplete")
    return result


def t18a_evidence(
    policy: dict[str, Any],
    denominator: dict[str, Any],
) -> dict[str, Any]:
    counts = denominator.get("counts") or {}
    rows = denominator.get("rows") or []
    selected = [
        row for row in rows
        if row.get("disposition") == "SELECTED_T18"
    ]
    selected_a = [row for row in selected if row.get("stage") == "T18a"]
    if (
        denominator.get("status")
        != "T18_MACHINE_ENERGY_DENOMINATOR_READY"
        or counts.get("t18_owner_machine_kinds") != 29
        or counts.get("unclassified") != 0
        or len(selected) != 6
        or len(selected_a) != 3
    ):
        raise ValueError("T18a denominator evidence is incomplete")

    catalog = load(CONVERTERS)
    profiles = catalog.get("profiles") or []
    complete = [
        row for row in profiles
        if row.get("stage") == "T18a"
        and row.get("status") == "COMPLETE"
    ]
    profile_ids = sorted(row["id"] for row in complete)
    expected_profiles = sorted({
        "cruciblecraft:bronze_firebox",
        "cruciblecraft:bronze_boiler",
        "cruciblecraft:bronze_steam_engine",
    })
    source_ids = {
        row["id"]: row["source"]["sourceId"] for row in complete
    }
    by_id = {row["id"]: row for row in complete}
    firebox = by_id.get("cruciblecraft:bronze_firebox") or {}
    boiler = by_id.get("cruciblecraft:bronze_boiler") or {}
    engine = by_id.get("cruciblecraft:bronze_steam_engine") or {}
    engine_semantics = engine.get("outputSemantics") or {}
    source_conservation = engine_semantics.get("conservation") or {}
    source_nominal = engine_semantics.get("sourceNominal") or {}
    fixed_output = engine_semantics.get("fixedOutput") or {}
    gt6_runtime = engine_semantics.get("gt6Runtime") or {}
    source_evidence_paths = engine_semantics.get(
        "sourceEvidencePaths"
    ) or []
    if (
        profile_ids != expected_profiles
        or source_ids
        != {
            "cruciblecraft:bronze_firebox": 1102,
            "cruciblecraft:bronze_boiler": 1202,
            "cruciblecraft:bronze_steam_engine": 1302,
        }
        or firebox.get("outputPacket", {}).get(
            "maxAmountPerTick"
        ) != 24
        or firebox.get("efficiencyBps") != 7500
        or boiler.get("conservation")
        != {
            "primaryInput": "HU",
            "primaryInputUnits": 80,
            "secondaryInput": "minecraft:water",
            "secondaryInputUnits": 1,
            "output": "STEAM",
            "outputUnits": 160,
            "exhaust": "NONE",
            "exhaustUnits": 0,
        }
        or engine.get("source", {}).get("sourceId") != 1302
        or engine.get("source", {}).get("sourceLine") != 586
        or engine.get("outputPacket", {}).get("size") != 12
        or engine.get("efficiencyBps") != 5000
        or source_conservation
        != {
            "classification": "SOURCE_BACKED",
            "steamInputMb": 200,
            "kuOutput": 50,
            "steamMbPerKu": 4,
        }
        or source_nominal
        != {
            "classification": "SOURCE_DERIVED_NOMINAL",
            "registeredNumerator": 24,
            "steamPerEu": 2,
            "mOutputKu": 12,
        }
        or fixed_output
        != {
            "classification": "DESIGN_POLICY_FIXED_OUTPUT",
            "kuPerTick": 12,
        }
        or gt6_runtime.get("classification") != "DEFERRED_REPLACEMENT"
        or gt6_runtime.get("minimumKuPerTick") != 6
        or gt6_runtime.get("maximumKuPerTick") != 24
        or gt6_runtime.get("behavior")
        != "STATE_DEPENDENT_MOUTPUT_HALF_TO_DOUBLE"
        or not str(gt6_runtime.get("replacementCondition") or "").strip()
        or not str(gt6_runtime.get("recheckPoint") or "").strip()
        or source_evidence_paths
        != [
            "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/"
            "Loader_MultiTileEntities.java:586",
            "gt6_code/gregtech6/src/main/java/gregapi/data/CS.java:240",
            "gt6_code/gregtech6/src/main/java/gregtech/tileentity/"
            "energy/converters/MultiTileEntityEngineSteam.java:"
            "58,62-63,77-80,98-103,121-146",
        ]
    ):
        raise ValueError("T18a converter profile evidence drifted")

    resource_paths = [
        "src/generated/resources/assets/cruciblecraft/"
        "blockstates/firebox.json",
        "src/generated/resources/assets/cruciblecraft/"
        "models/block/firebox.json",
        "src/generated/resources/assets/cruciblecraft/"
        "models/item/firebox.json",
        "src/generated/resources/data/cruciblecraft/"
        "loot_table/blocks/firebox.json",
        "src/generated/resources/data/cruciblecraft/"
        "recipe/machines/firebox.json",
    ]
    resources = {}
    for relative_path in resource_paths:
        path = ROOT / relative_path
        if not path.is_file():
            raise ValueError(
                f"T18a generated Firebox resource is missing: {relative_path}"
            )
        resources[relative_path] = sha256(path)

    game_source = GAMETESTS.read_text(encoding="utf-8")
    game_tests = policy["stages"]["T18a"]["game_tests"]
    if any(name not in game_source for name in game_tests):
        raise ValueError("T18a GameTest evidence is missing")
    return {
        "denominator": {
            "machine_kinds": counts["t18_owner_machine_kinds"],
            "selected_kinds": counts["selected_kinds"],
            "preimplemented_reference_kinds":
                counts["preimplemented_reference_kinds"],
            "deferred_kinds": counts["deferred_kinds"],
            "energy_identities": counts["energy_identities"],
            "unclassified": counts["unclassified"],
        },
        "selected_t18a_kinds": sorted(
            row["behavior_class"] for row in selected_a
        ),
        "complete_profiles": profile_ids,
        "selected_source_ids": source_ids,
        "firebox": {
            "hu_per_tick": 24,
            "efficiency_bps": 7500,
            "fuel_map": firebox["fuelMap"],
        },
        "boiler": {
            "hu": 80,
            "water_mb": 1,
            "steam_mb": 160,
        },
        "steam_engine": {
            "source_id": 1302,
            "source_line": 586,
            "source_output_expression": "24/STEAM_PER_EU",
            "steam_per_eu": catalog["source"]["steamPerEu"],
            "ku_packet": 12,
            "efficiency_bps": 5000,
            "source_conservation": source_conservation,
            "source_nominal": source_nominal,
            "fixed_output": fixed_output,
            "gt6_runtime": gt6_runtime,
            "source_evidence_paths": source_evidence_paths,
        },
        "resources": resources,
        "junit_tests": policy["stages"]["T18a"]["junit_tests"],
        "game_tests": game_tests,
        "full_suite_test_count": game_source.count("@GameTest("),
    }


def t18b_evidence(
    policy: dict[str, Any],
    denominator: dict[str, Any],
) -> dict[str, Any]:
    selected = {
        row.get("converter_profile"): row
        for row in denominator.get("rows") or []
        if row.get("disposition") == "SELECTED_T18"
    }
    expected_sources = {
        "cruciblecraft:bronze_dynamo": 10111,
        "cruciblecraft:bronze_fuel_engine": 9147,
    }
    if set(expected_sources) - set(selected):
        raise ValueError("T18b denominator source evidence is incomplete")
    for profile, source_id in expected_sources.items():
        if (
            selected[profile].get("selected_source", {}).get("source_id")
            != source_id
        ):
            raise ValueError("T18b denominator source selection drifted")

    catalog = load(CONVERTERS)
    profiles = {
        row["id"]: row for row in catalog.get("profiles") or []
        if row.get("stage") == "T18b"
        and row.get("status") == "COMPLETE"
    }
    if set(profiles) != set(expected_sources):
        raise ValueError("T18b completed profile set drifted")
    dynamo = profiles["cruciblecraft:bronze_dynamo"]
    fuel_engine = profiles["cruciblecraft:bronze_fuel_engine"]
    if (
        dynamo.get("source", {}).get("sourceId") != 10111
        or dynamo.get("inputWindow")
        != {"minimum": 16, "nominal": 32, "maximum": 64}
        or dynamo.get("outputPacket", {}).get("size") != 22
        or dynamo.get("efficiencyBps") != 6875
        or dynamo.get("conservation")
        != {
            "primaryInput": "RU",
            "primaryInputUnits": 32,
            "secondaryInput": "NONE",
            "secondaryInputUnits": 0,
            "output": "EU",
            "outputUnits": 22,
            "exhaust": "LOSS",
            "exhaustUnits": 10,
        }
        or "NBT_WASTE_ENERGY" not in dynamo.get(
            "policy", {}
        ).get("blockage", "")
        or fuel_engine.get("source", {}).get("sourceId") != 9147
        or fuel_engine.get("source", {}).get("outputExpression") != "16"
        or fuel_engine.get("efficiencyBps") != 10000
        or fuel_engine.get("outputPacket", {}).get("identity") != "RU"
        or fuel_engine.get("outputPacket", {}).get("size") != 16
        or fuel_engine.get("conservation", {}).get("outputUnits") != 512
        or fuel_engine.get("conservation", {}).get("exhaustUnits") != 1
        or "CURRENT_KINETIC_ROTATION_IDENTITY_REQUIRED"
        not in fuel_engine.get("policy", {}).get("sourceResolution", "")
        or "MISSING_PARTIAL_OR_WRONG_IDENTITY_QUARANTINED"
        not in fuel_engine.get("policy", {}).get("sourceResolution", "")
    ):
        raise ValueError("T18b converter profile evidence drifted")

    game_source = GAMETESTS.read_text(encoding="utf-8")
    game_tests = policy["stages"]["T18b"]["game_tests"]
    if any(name not in game_source for name in game_tests):
        raise ValueError("T18b GameTest evidence is missing")
    return {
        "complete_profiles": sorted(profiles),
        "selected_source_ids": expected_sources,
        "dynamo": {
            "input_window_ru": [16, 32, 64],
            "nominal_input_ru": 32,
            "output_eu": 22,
            "loss_units": 10,
            "efficiency_bps": 6875,
            "waste_energy": True,
            "legacy_24_to_24_corrected": True,
        },
        "fuel_engine": {
            "fuel_map": fuel_engine["fuelMap"],
            "output_identity": "RU",
            "packet_ru": 16,
            "fuel_recipe_total_ru": 512,
            "current_energy_identity": "KINETIC_ROTATION",
            "identity_policy": "CURRENT_COMPLETE_ONLY",
            "missing_partial_wrong": "QUARANTINE",
        },
        "junit_tests": policy["stages"]["T18b"]["junit_tests"],
        "game_tests": game_tests,
        "full_suite_test_count": game_source.count("@GameTest("),
    }


def t18c_evidence(
    policy: dict[str, Any],
    denominator: dict[str, Any],
) -> dict[str, Any]:
    selected = {
        row.get("converter_profile"): row
        for row in denominator.get("rows") or []
        if row.get("disposition") == "SELECTED_T18"
    }
    source = selected.get("cruciblecraft:bronze_gas_generator") or {}
    if (
        source.get("stage") != "T18c"
        or source.get("selected_source", {}).get("source_id") != 1602
        or source.get("selected_source", {}).get("output_expression") != "24"
        or source.get("selected_source", {}).get(
            "efficiency_expression"
        ) != "7500"
        or source.get("process_map") != "fuel:FM.Burn"
    ):
        raise ValueError("T18c denominator source evidence is incomplete")

    catalog = load(CONVERTERS)
    profiles = {
        row["id"]: row for row in catalog.get("profiles") or []
        if row.get("stage") == "T18c"
        and row.get("status") == "COMPLETE"
    }
    gas = profiles.get("cruciblecraft:bronze_gas_generator") or {}
    if (
        set(profiles) != {"cruciblecraft:bronze_gas_generator"}
        or gas.get("source", {}).get("sourceId") != 1602
        or gas.get("source", {}).get("fuelMap") != "FM.Burn"
        or gas.get("outputPacket")
        != {
            "medium": "ENERGY",
            "identity": "HU",
            "size": 1,
            "maxAmountPerTick": 24,
        }
        or gas.get("efficiencyBps") != 7500
        or gas.get("conservation")
        != {
            "primaryInput": "FM.Burn",
            "primaryInputUnits": 1536,
            "secondaryInput": "NONE",
            "secondaryInputUnits": 0,
            "output": "HU",
            "outputUnits": 1152,
            "exhaust": "RECIPE_DEFINED",
            "exhaustUnits": 9,
        }
        or gas.get("faces", {}).get("energyOutputs") != ["UP"]
        or "CURRENT_HEAT_IDENTITY_REQUIRED"
        not in gas.get("policy", {}).get("sourceResolution", "")
        or "MISSING_PARTIAL_OR_WRONG_IDENTITY_QUARANTINED"
        not in gas.get("policy", {}).get("sourceResolution", "")
    ):
        raise ValueError("T18c gas-generator profile evidence drifted")

    t11 = load(TOOLS / "t11_preflight_projection.json")
    methane = (t11.get("selected_routes") or {}).get(
        "methane_gas_fuel"
    ) or {}
    source_recipe = methane.get("source_recipe") or {}
    if (
        methane.get("map") != "gt.recipe.fuels.burn"
        or methane.get("recipe_index") != 20
        or source_recipe.get("duration") != 24
        or source_recipe.get("eut") != -64
        or source_recipe.get("fluid_inputs")
        != [{"amount": 5, "fluid": "methane"}]
        or source_recipe.get("fluid_outputs")
        != [
            {"amount": 6, "fluid": "water"},
            {"amount": 3, "fluid": "carbondioxide"},
        ]
    ):
        raise ValueError("T18c methane FM.Burn projection drifted")

    o37 = load(TOOLS / "t18_o37_identity_projection.json")
    bridge = o37.get("bridge") or {}
    if (
        o37.get("status") != "O37_CLOSED"
        or o37.get("resolution") != "DESIGN_POLICY"
        or o37.get("negative_evidence", {}).get(
            "direct_binding_candidate_count"
        ) != 0
        or bridge.get("gt6_equivalence")
        != "NO_DIRECT_BINDING_AT_FIXED_REVISION"
        or bridge.get("material_9852_role")
        != "SOURCE_MATERIAL_LAYER_ONLY"
        or bridge.get("publication_delta") != 0
    ):
        raise ValueError("T18c O-37 permanent policy receipt drifted")

    game_source = GAMETESTS.read_text(encoding="utf-8")
    game_tests = policy["stages"]["T18c"]["game_tests"]
    if any(name not in game_source for name in game_tests):
        raise ValueError("T18c GameTest evidence is missing")
    return {
        "complete_profiles": sorted(profiles),
        "selected_source_ids": {
            "cruciblecraft:bronze_gas_generator": 1602,
        },
        "gas_generator": {
            "fuel_map": "FM.Burn",
            "output_identity": "HU",
            "packet_hu": 1,
            "maximum_hu_per_tick": 24,
            "efficiency_bps": 7500,
            "methane_input_mb": 5,
            "source_energy_units": 1536,
            "generated_hu": 1152,
            "water_exhaust_mb": 6,
            "carbon_dioxide_exhaust_mb": 3,
            "energy_output_face": "UP",
            "current_energy_identity": "HEAT",
            "identity_policy": "CURRENT_COMPLETE_ONLY",
            "missing_partial_wrong": "QUARANTINE",
        },
        "o37": {
            "status": o37["status"],
            "resolution": o37["resolution"],
            "closure": o37["closure"],
            "direct_binding_candidates": 0,
            "material_9852_role": bridge["material_9852_role"],
            "publication_delta": bridge["publication_delta"],
        },
        "junit_tests": policy["stages"]["T18c"]["junit_tests"],
        "game_tests": game_tests,
        "full_suite_test_count": game_source.count("@GameTest("),
    }


def t18d_evidence(
    policy: dict[str, Any],
    denominator: dict[str, Any],
    acquisition: dict[str, Any],
) -> dict[str, Any]:
    counts = denominator.get("counts") or {}
    acquisition_counts = acquisition.get("counts") or {}
    if (
        denominator.get("status")
        != "T18_MACHINE_ENERGY_DENOMINATOR_READY"
        or counts.get("t18_owner_machine_kinds") != 29
        or counts.get("energy_identities") != 2
        or counts.get("unclassified") != 0
        or counts.get("selected_kinds") != 6
        or counts.get("preimplemented_reference_kinds") != 3
        or counts.get("deferred_kinds") != 20
    ):
        raise ValueError("T18d denominator closure counts drifted")
    if (
        acquisition.get("status")
        != "T18D_CONVERTER_ACQUISITION_READY"
        or acquisition_counts.get("converter_profiles") != 6
        or acquisition_counts.get("vanilla_crafting_recipes") != 6
        or acquisition_counts.get("gt_recipe_rows_added") != 0
        or acquisition_counts.get("unreachable") != 0
        or acquisition.get("resource_closure", {}).get("status")
        != "BIDIRECTIONAL_CLOSURE"
    ):
        raise ValueError("T18d converter acquisition closure drifted")

    baseline = load(PUBLICATION_BASELINE)
    t17_baseline = load(T17_PUBLICATION_BASELINE)
    if (
        baseline.get("schema_version") != 1
        or baseline.get("status") != "T18D_PUBLICATION_BASELINE"
        or baseline.get("baseline") != "T17"
    ):
        raise ValueError("T18d publication baseline header drifted")
    recipe_map_ids = baseline.get("recipe_map_ids") or []
    t17_map_ids = t17_baseline.get("recipe_map_ids") or []
    actual_map_ids = sorted(publication_support.local_recipe_maps())
    if (
        recipe_map_ids != sorted(recipe_map_ids)
        or len(recipe_map_ids) != 32
        or len(recipe_map_ids) != len(set(recipe_map_ids))
        or recipe_map_ids != t17_map_ids
        or recipe_map_ids != actual_map_ids
    ):
        raise ValueError("T18d RecipeMap ids must equal the T17 baseline")

    publication_totals = baseline.get("publication_totals") or {}
    t14_load = load(T14_READINESS).get("load_gate") or {}
    if (
        publication_totals != t17_baseline.get("publication_totals")
        or publication_totals
        != {
            "logical_rows": t14_load.get("logical_recipes"),
            "eager_rows": t14_load.get("eager_recipes"),
            "lazy_rows": t14_load.get("lazy_recipes"),
        }
        or publication_totals
        != {
            "logical_rows": 18_875,
            "eager_rows": 16_650,
            "lazy_rows": 2_225,
        }
    ):
        raise ValueError(
            "T18d logical/eager/lazy totals must equal the T17 baseline"
        )

    emi_ids = baseline.get("emi_recipe_map_ids") or []
    if (
        emi_ids != sorted(emi_ids)
        or len(emi_ids) != 24
        or len(emi_ids) != len(set(emi_ids))
        or emi_ids != t17_baseline.get("emi_recipe_map_ids")
        or baseline.get("emi_enumeration_contract")
        != t17_baseline.get("emi_enumeration_contract")
        or not set(emi_ids).issubset(recipe_map_ids)
    ):
        raise ValueError("T18d EMI enumeration must equal T17")

    projected = recipe_load_projection.project(load(LOAD_INPUT))
    if projected != load(LOAD_OUTPUT):
        raise ValueError("T18 zero-load projection is stale")
    load_counts = projected.get("ledger", {}).get("counts") or {}
    intervals = projected.get("ledger", {}).get(
        "measurement_intervals"
    ) or {}
    if (
        projected.get("status") != "PASS"
        or projected.get("delivery_phase") != "T18"
        or any(value != 0 for value in load_counts.values())
        or any(
            interval != {"min": 0, "max": 0}
            for interval in intervals.values()
        )
    ):
        raise ValueError("T18d load projection must be a zero-workload PASS")

    acquisition_baseline = baseline.get("t18_acquisition") or {}
    expected_recipe_ids = acquisition.get("expected_set", {}).get(
        "recipe_ids"
    ) or []
    baseline_recipe_ids = acquisition_baseline.get(
        "vanilla_recipe_ids"
    ) or []
    recipe_types = {
        load(ROOT / row["recipe_path"]).get("type")
        for row in acquisition.get("recipes") or []
    }
    if (
        baseline_recipe_ids != sorted(baseline_recipe_ids)
        or baseline_recipe_ids != expected_recipe_ids
        or acquisition_baseline.get(
            "observed_converter_crafting_rows"
        ) != 6
        or acquisition_baseline.get("gt_recipe_rows_added") != 0
        or recipe_types != {"minecraft:crafting_shaped"}
    ):
        raise ValueError(
            "T18d acquisition must close six vanilla recipes and zero GT rows"
        )

    o37 = load(O37_PROJECTION)
    o37_publication = baseline.get("o37_publication") or {}
    if (
        o37.get("status") != "O37_CLOSED"
        or o37.get("resolution") != "DESIGN_POLICY"
        or o37.get("closure")
        != "O37_CLOSED_PERMANENT_DESIGN_POLICY"
        or o37.get("negative_evidence", {}).get(
            "direct_binding_candidate_count"
        ) != 0
        or o37.get("bridge", {}).get("publication_delta") != 0
        or o37_publication
        != {
            "resolution": "DESIGN_POLICY",
            "runtime_registrations_added": 0,
            "gt_recipe_rows_added": 0,
            "publication_delta": 0,
        }
    ):
        raise ValueError("T18d O-37 publication receipt drifted")

    catalog = load(CONVERTERS)
    profiles = {row["id"]: row for row in catalog.get("profiles") or []}
    conservation = {
        "steam_chain": {
            "firebox_efficiency_bps":
                profiles["cruciblecraft:bronze_firebox"]["efficiencyBps"],
            "boiler_hu": profiles["cruciblecraft:bronze_boiler"][
                "conservation"
            ]["primaryInputUnits"],
            "boiler_water_mb": profiles["cruciblecraft:bronze_boiler"][
                "conservation"
            ]["secondaryInputUnits"],
            "boiler_steam_mb": profiles["cruciblecraft:bronze_boiler"][
                "conservation"
            ]["outputUnits"],
            "steam_engine_ku_packet":
                profiles["cruciblecraft:bronze_steam_engine"][
                    "outputPacket"
                ]["size"],
        },
        "fuel_to_ru": {
            "fuel_units": profiles["cruciblecraft:bronze_fuel_engine"][
                "conservation"
            ]["primaryInputUnits"],
            "ru_units": profiles["cruciblecraft:bronze_fuel_engine"][
                "conservation"
            ]["outputUnits"],
            "exhaust_units": profiles[
                "cruciblecraft:bronze_fuel_engine"
            ]["conservation"]["exhaustUnits"],
        },
        "ru_to_eu": {
            "ru_units": profiles["cruciblecraft:bronze_dynamo"][
                "conservation"
            ]["primaryInputUnits"],
            "eu_units": profiles["cruciblecraft:bronze_dynamo"][
                "conservation"
            ]["outputUnits"],
            "loss_units": profiles["cruciblecraft:bronze_dynamo"][
                "conservation"
            ]["exhaustUnits"],
        },
        "gas_to_hu": {
            "source_units": profiles[
                "cruciblecraft:bronze_gas_generator"
            ]["conservation"]["primaryInputUnits"],
            "hu_units": profiles["cruciblecraft:bronze_gas_generator"][
                "conservation"
            ]["outputUnits"],
            "exhaust_units": profiles[
                "cruciblecraft:bronze_gas_generator"
            ]["conservation"]["exhaustUnits"],
        },
    }
    expected_conservation = {
        "steam_chain": {
            "firebox_efficiency_bps": 7_500,
            "boiler_hu": 80,
            "boiler_water_mb": 1,
            "boiler_steam_mb": 160,
            "steam_engine_ku_packet": 12,
        },
        "fuel_to_ru": {
            "fuel_units": 1,
            "ru_units": 512,
            "exhaust_units": 1,
        },
        "ru_to_eu": {
            "ru_units": 32,
            "eu_units": 22,
            "loss_units": 10,
        },
        "gas_to_hu": {
            "source_units": 1_536,
            "hu_units": 1_152,
            "exhaust_units": 9,
        },
    }
    if conservation != expected_conservation:
        raise ValueError("T18d four-chain conservation receipt drifted")

    game_source = GAMETESTS.read_text(encoding="utf-8")
    game_tests = list(dict.fromkeys(
        name
        for stage in ("T18a", "T18b", "T18c")
        for name in policy["stages"][stage]["game_tests"]
    ))
    if len(game_tests) != 8 or any(
        name not in game_source for name in game_tests
    ):
        raise ValueError("T18d GameTest closure evidence drifted")
    return {
        "status": "PASS",
        "denominator": {
            "machine_kinds": counts["t18_owner_machine_kinds"],
            "energy_identities": counts["energy_identities"],
            "unclassified": counts["unclassified"],
            "selected_kinds": counts["selected_kinds"],
            "preimplemented_reference_kinds":
                counts["preimplemented_reference_kinds"],
            "deferred_kinds": counts["deferred_kinds"],
        },
        "four_chain_conservation": conservation,
        "identity": {
            "current_identity_profiles": 2,
            "fuel_engine": "KINETIC_ROTATION",
            "gas_generator": "HEAT",
            "blank_or_missing_policy": "QUARANTINE",
            "partial_or_wrong_policy": "QUARANTINE",
            "pending": [],
        },
        "acquisition": {
            "status": acquisition["status"],
            "profiles": acquisition_counts["converter_profiles"],
            "vanilla_crafting_recipes":
                acquisition_counts["vanilla_crafting_recipes"],
            "resource_closure":
                acquisition["resource_closure"]["status"],
            "gt_recipe_rows_added":
                acquisition_counts["gt_recipe_rows_added"],
            "unreachable": acquisition_counts["unreachable"],
            "pending": [],
        },
        "load_projection": {
            "input": relative(LOAD_INPUT),
            "input_sha256": sha256(LOAD_INPUT),
            "output": relative(LOAD_OUTPUT),
            "output_sha256": sha256(LOAD_OUTPUT),
            "delivery_phase": projected["delivery_phase"],
            "status": projected["status"],
            "incremental_counts": load_counts,
            "measurement_intervals": intervals,
            "pending": [],
        },
        "publication_baseline": {
            "path": relative(PUBLICATION_BASELINE),
            "sha256": sha256(PUBLICATION_BASELINE),
            "t17_path": relative(T17_PUBLICATION_BASELINE),
            "t17_sha256": sha256(T17_PUBLICATION_BASELINE),
            "recipe_map_ids": recipe_map_ids,
            "publication_totals": publication_totals,
            "publication_delta": 0,
            "stable_id_set_equal_to_t17": True,
        },
        "emi_enumeration": {
            "recipe_map_ids": emi_ids,
            "configured_maps": len(emi_ids),
            "recipe_enumeration_equal_to_t17": True,
            "contract": baseline["emi_enumeration_contract"],
        },
        "o37_publication": {
            "status": o37["status"],
            "resolution": o37["resolution"],
            "closure": o37["closure"],
            "direct_binding_candidates": 0,
            "runtime_registrations_added": 0,
            "gt_recipe_rows_added": 0,
            "publication_delta": 0,
            "pending": [],
        },
        "gametest": {
            "path": relative(GAMETESTS),
            "tests": game_tests,
            "t18_test_count": len(game_tests),
            "full_suite_test_count": game_source.count("@GameTest("),
        },
        "pending": [],
    }


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    status, completed, pending = stage_summary(policy)
    dependency_rows = dependencies(policy)
    denominator = load(denominator_builder.OUTPUT)
    contracts = source_contracts(policy, completed)
    evidence = t18a_evidence(policy, denominator)
    evidence_b = t18b_evidence(policy, denominator)
    evidence_c = t18c_evidence(policy, denominator)
    acquisition = load(acquisition_builder.OUTPUT)
    evidence_d = t18d_evidence(policy, denominator, acquisition)
    return {
        "schema_version": 1,
        "status": status,
        "completed_stages": completed,
        "pending_stages": pending,
        "stage_gates": policy["stages"],
        "t18a_evidence": evidence,
        "t18b_evidence": evidence_b,
        "t18c_evidence": evidence_c,
        "t18d_evidence": evidence_d,
        "publication_gate": {
            "status": "PASS",
            "publication_unchanged_claimed": True,
            "publication_delta": 0,
            "recipe_map_stable_id_set_equal_to_t17": True,
            "emi_recipe_enumeration_equal_to_t17": True,
            "logical_eager_lazy_totals_equal_to_t17": True,
            "o37_publication_recorded_separately": True,
        },
        "resource_acquisition_identity_gate": {
            "status": "T18D_COMPLETE",
            "profiles": 6,
            "vanilla_crafting_recipes": 6,
            "gt_recipe_rows_added": 0,
            "unreachable": 0,
            "current_identity_profiles": 2,
            "pending": [],
        },
        "closure_summary": {
            "denominator_kinds": evidence_d["denominator"][
                "machine_kinds"
            ],
            "energy_identities": evidence_d["denominator"][
                "energy_identities"
            ],
            "unclassified": evidence_d["denominator"]["unclassified"],
            "selected_kinds": evidence_d["denominator"][
                "selected_kinds"
            ],
            "preimplemented_reference_kinds": evidence_d[
                "denominator"
            ]["preimplemented_reference_kinds"],
            "deferred_kinds": evidence_d["denominator"][
                "deferred_kinds"
            ],
            "converter_profiles": evidence_d["acquisition"]["profiles"],
            "converter_acquisition_recipes": evidence_d[
                "acquisition"
            ]["vanilla_crafting_recipes"],
            "converter_acquisition_unreachable": evidence_d[
                "acquisition"
            ]["unreachable"],
            "current_identity_profiles": evidence_d["identity"][
                "current_identity_profiles"
            ],
            "four_chain_conservation": "PASS",
            "o37_resolution": evidence_d["o37_publication"][
                "resolution"
            ],
            "o37_publication_delta": evidence_d["o37_publication"][
                "publication_delta"
            ],
            "gt_recipe_row_mutation": evidence_d["acquisition"][
                "gt_recipe_rows_added"
            ],
            "publication_delta": evidence_d["publication_baseline"][
                "publication_delta"
            ],
            "pending": 0,
        },
        "closure_policy": {
            "final_closure_attempted": True,
            "reason": (
                "T18a-d are complete: source identities, four conversion "
                "chains, current-only identity policy, acquisition, "
                "O-37 permanent policy, "
                "zero-load publication and currentness are closed."
            ),
            "pending": [],
        },
        "source_contracts": contracts,
        "currentness": {
            "owned_inputs": {
                relative(BUILDER): sha256(BUILDER),
                relative(POLICY): sha256(POLICY),
            },
            "dependencies": dependency_rows,
            "completed_stage_sources": {
                stage: {
                    owner: row["sha256"]
                    for owner, row in contracts.items()
                    if row["stage"] == stage
                }
                for stage in completed
            },
            "pending_report": {
                **policy["refresh_policy"],
                "pending": [],
            },
        },
    }


def check() -> list[str]:
    encoded = stable(build())
    if not OUTPUT.is_file():
        return [f"missing generated file: {relative(OUTPUT)}"]
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        return [f"stale generated file: {relative(OUTPUT)}"]
    return []


def write() -> dict[str, Any]:
    document = build()
    encoded = stable(document).encode("utf-8")
    tmp = OUTPUT.with_name(OUTPUT.name + ".tmp")
    tmp.write_bytes(encoded)
    tmp.replace(OUTPUT)
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T18 readiness failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "completed": document["completed_stages"],
        "pending": document["pending_stages"],
        "final_closure_attempted":
            document["closure_policy"]["final_closure_attempted"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
