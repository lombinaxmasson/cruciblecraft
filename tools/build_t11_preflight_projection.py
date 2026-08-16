#!/usr/bin/env python3
"""Build the source-backed T11a hydrocarbon identity and load preflight."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t11_preflight_policy.json"
OUTPUT = TOOLS / "t11_preflight_projection.json"
T13_RECIPE_MAPS = TOOLS / "t13_denominators/recipe_maps.json"
FLUID_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "t11_hydrocarbon_fluid_gate.json"
)
T11_CELL_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "t11_cell_content_gate.json"
)
RUNTIME_POLICY = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "t11_hydrocarbon_runtime_policy.json"
)
O37_IDENTITY = TOOLS / "t18_o37_identity_projection.json"
T5_FLUID_GATE = (
    ROOT
    / "src/t5_chemical_generated/resources/data/cruciblecraft"
    / "t5_chemical_fluid_gate.json"
)
T10_FLUID_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "t10_container_fluid_gate.json"
)
T10_CELL_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "t10_cell_content_gate.json"
)
DEPOSIT_CATALOG = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog"
    / "fluid_deposits.json"
)
MATERIAL_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
T8_READINESS = TOOLS / "t8_pipe_readiness.json"
T10_PREFLIGHT = TOOLS / "t10_preflight_projection.json"
PROCESSING_MACHINES = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry"
    / "ModProcessingMachines.java"
)
CHEMICAL_GATE_SOURCE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/material"
    / "ChemicalFluidRegistrationGate.java"
)
GT6_REPOSITORY = "GregTech6/gregtech6"
BUILTIN_FLUIDS = {"minecraft:water"}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def canonical_hash(value: Any) -> str:
    payload = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def round_up(value: int, unit: int) -> int:
    return ((value + unit - 1) // unit) * unit


def fluid_signature(recipe: dict[str, Any], key: str) -> list[dict[str, Any]]:
    return [
        {
            "amount": int(stack["amount"]),
            "fluid": stack["fluid"],
        }
        for stack in recipe.get(key) or []
    ]


def recipe_matches(recipe: dict[str, Any], expected: dict[str, Any]) -> bool:
    return (
        recipe.get("enabled") is not False
        and recipe.get("hidden") is not True
        and recipe.get("fake") is not True
        and recipe.get("duration") == expected["duration"]
        and recipe.get("euPerTick") == expected["eut"]
        and fluid_signature(recipe, "fluidInputs")
        == expected["fluid_inputs"]
        and fluid_signature(recipe, "fluidOutputs")
        == expected["fluid_outputs"]
    )


def runtime_stacks(
    source_stacks: list[dict[str, Any]],
    mapping: dict[str, str],
) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for stack in source_stacks:
        source_id = stack["fluid"]
        runtime_id = mapping.get(source_id)
        if runtime_id is None:
            raise ValueError(f"missing runtime identity mapping for {source_id}")
        result.append({
            "amount": stack["amount"],
            "id": runtime_id,
            "source_fluid": source_id,
        })
    return result


def select_routes(
    policy: dict[str, Any],
) -> tuple[dict[str, dict[str, Any]], dict[str, dict[str, Any]]]:
    selected: dict[str, dict[str, Any]] = {}
    maps: dict[str, dict[str, Any]] = {}
    for route_id, expected in sorted(policy["source_rows"].items()):
        path = ROOT / expected["path"]
        document = load(path)
        recipes = document["recipes"]
        if (
            document.get("nameInternal") != expected["map"]
            or document.get("recipeCount") != expected["expected_source_rows"]
            or len(recipes) != expected["expected_source_rows"]
        ):
            raise ValueError(f"pinned source map drifted for {route_id}")
        matches = [
            (index, recipe)
            for index, recipe in enumerate(recipes)
            if recipe_matches(recipe, expected)
        ]
        if len(matches) != 1:
            raise ValueError(
                f"{route_id} expected exactly one pinned row, found {len(matches)}"
            )
        index, recipe = matches[0]
        mapping = expected["runtime_identity_mapping"]
        selected[route_id] = {
            "id": route_id,
            "map": expected["map"],
            "role": expected["role"],
            "recipe_index": index,
            "source": {
                "path": f"{expected['path']}#recipes[{index}]",
                "row_sha256": canonical_hash(recipe),
            },
            "source_recipe": {
                "duration": recipe["duration"],
                "eut": recipe["euPerTick"],
                "fluid_inputs": fluid_signature(recipe, "fluidInputs"),
                "fluid_outputs": fluid_signature(recipe, "fluidOutputs"),
            },
            "runtime_projection": {
                "fluid_inputs": runtime_stacks(
                    expected["fluid_inputs"], mapping
                ),
                "fluid_outputs": runtime_stacks(
                    expected["fluid_outputs"], mapping
                ),
            },
        }
        maps[expected["map"]] = {
            "path": expected["path"],
            "recipe_count": len(recipes),
            "sha256": digest(path),
        }
    return selected, maps


def build_fluid_gate(
    policy: dict[str, Any],
    selected: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    o37 = load(O37_IDENTITY)
    if (
        o37.get("status") != "O37_CLOSED"
        or o37.get("resolution") != "DESIGN_POLICY"
        or o37.get("negative_evidence", {}).get(
            "direct_binding_candidate_count"
        ) != 0
    ):
        raise ValueError("O-37 fixed-source identity evidence is not closed")
    fluids: list[dict[str, Any]] = []
    for fluid_id, expected in sorted(
        policy["identity_gate"]["entries"].items()
    ):
        material_path = (
            ROOT / expected["material_path"]
            if "material_path" in expected
            else MATERIAL_ROOT / f"{expected['material']}.json"
        )
        material = load(material_path)
        route = selected[expected["source_route"]]
        bridge = policy["identity_gate"]["source_fluid_bridges"].get(
            expected["source_fluid"]
        )
        source_inputs = {
            stack["source_fluid"]: stack["id"]
            for stack in route["runtime_projection"]["fluid_inputs"]
        }
        runtime_identity = f"cruciblecraft:{fluid_id}"
        if source_inputs.get(expected["source_fluid"]) != runtime_identity:
            raise ValueError(f"T11 source/runtime fluid mapping drifted for {fluid_id}")
        if bridge is not None and (
            bridge["runtime_identity"] != runtime_identity
            or bridge["status"] != "DESIGN_POLICY"
            or bridge["closure"]
            != "O37_CLOSED_PERMANENT_DESIGN_POLICY"
            or bridge["gt6_equivalence"]
            != "NO_DIRECT_BINDING_AT_FIXED_REVISION"
            or bridge["closed_item"] != "O-37"
            or bridge["permanent"] is not True
            or bridge["material_9852_role"]
            != "SOURCE_MATERIAL_LAYER_ONLY"
            or "tools/t18_o37_identity_projection.json"
            not in bridge["evidence"]
            or bridge["publication_delta"] != 0
            or {
                key: o37["bridge"][key]
                for key in (
                    "runtime_identity",
                    "status",
                    "closure",
                    "gt6_equivalence",
                    "closed_item",
                    "permanent",
                    "material_9852_role",
                    "publication_delta",
                )
            }
            != {
                key: bridge[key]
                for key in (
                    "runtime_identity",
                    "status",
                    "closure",
                    "gt6_equivalence",
                    "closed_item",
                    "permanent",
                    "material_9852_role",
                    "publication_delta",
                )
            }
        ):
            raise ValueError(f"T11 identity bridge drifted for {fluid_id}")
        source_reason = expected.get("source_reason")
        if bridge is None and (
            not isinstance(source_reason, str) or not source_reason
        ):
            raise ValueError(f"T11 direct identity lacks source reason for {fluid_id}")
        fluids.append({
            "color": material["color"],
            "density": expected["density"],
            "id": fluid_id,
            "material": expected["material"],
            "source": {
                "path": route["source"]["path"],
                "reason": source_reason or (
                    "CrucibleCraft permanent DESIGN_POLICY bridge: the complete "
                    "fixed-revision Java replay found no direct binding between "
                    "liquid_medium_oil and material 9852 MT.CrudeOil. O-37 is "
                    "closed while the T9/runtime crude_oil identity stays fixed."
                ),
                "repository": GT6_REPOSITORY,
                "revision": policy["source_revision"],
            },
            "state": expected["state"],
            "temperature_kelvin": expected["temperature_kelvin"],
            "viscosity": expected["viscosity"],
            "world_placeable": expected["world_placeable"],
        })
    return {
        "fluids": fluids,
        "schema_version": 1,
    }


def gate_ids(gate: dict[str, Any]) -> set[str]:
    return {
        f"cruciblecraft:{entry['id']}"
        for entry in gate["fluids"]
    }


def build_cell_gate(policy: dict[str, Any]) -> dict[str, Any]:
    fluids = []
    for fluid_id, entry in sorted(
        policy["identity_gate"]["entries"].items()
    ):
        if "cruciblecraft:gas_cell" not in entry["logistics"]:
            continue
        if entry["state"] != "gas":
            raise ValueError(
                f"T11 gas-cell identity is not gas: {fluid_id}"
            )
        fluids.append({
            "id": f"cruciblecraft:{fluid_id}",
            "kind": "gas",
            "material": entry["material"],
        })
    if fluids != [{
        "id": "cruciblecraft:natural_gas",
        "kind": "gas",
        "material": "natural_gas",
    }]:
        raise ValueError("T11 cell-content gate drifted")
    return {"fluids": fluids, "schema_version": 1}


def build_closure(
    policy: dict[str, Any],
    selected: dict[str, dict[str, Any]],
    fluid_gate: dict[str, Any],
    cell_gate: dict[str, Any],
) -> dict[str, Any]:
    t5_ids = gate_ids(load(T5_FLUID_GATE))
    t10_ids = gate_ids(load(T10_FLUID_GATE))
    t11_ids = gate_ids(fluid_gate)
    all_ids = t5_ids | t10_ids | t11_ids | BUILTIN_FLUIDS
    required_ids = {
        stack["id"]
        for route in selected.values()
        for side in ("fluid_inputs", "fluid_outputs")
        for stack in route["runtime_projection"][side]
    }
    missing = sorted(required_ids - all_ids)
    if missing:
        raise ValueError(f"T11 source rows have unregistered fluids: {missing}")

    providers: dict[str, str] = {}
    for fluid_id in sorted(required_ids):
        if fluid_id in BUILTIN_FLUIDS:
            providers[fluid_id] = "builtin"
        elif fluid_id in t11_ids:
            providers[fluid_id] = "t11_hydrocarbon_fluid_gate"
        elif fluid_id in t10_ids:
            providers[fluid_id] = "t10_container_fluid_gate"
        elif fluid_id in t5_ids:
            providers[fluid_id] = "t5_chemical_fluid_gate"
    expected_providers = dict(
        policy["identity_gate"]["existing_runtime_identities"]
    )
    expected_providers.update({
        fluid_id: "t11_hydrocarbon_fluid_gate"
        for fluid_id in t11_ids
    })
    if providers != expected_providers:
        raise ValueError("T11 runtime identity providers drifted")

    deposits = {
        row["material"]: row["id"]
        for row in load(DEPOSIT_CATALOG)["deposits"]
    }
    cell_entries = {
        row["id"]: row["kind"]
        for gate in (load(T10_CELL_GATE), cell_gate)
        for row in gate["fluids"]
    }
    if load(T8_READINESS).get("status") != "READY":
        raise ValueError("T8 pipe logistics are not ready")

    distillery = selected["crude_oil_distillation"]
    engine = selected["fuel_oil_engine"]
    gas = selected["methane_gas_fuel"]
    generifier = selected["natural_gas_to_methane"]
    closure = {
        "cruciblecraft:crude_oil": {
            "producer": deposits.get("cruciblecraft:crude_oil"),
            "logistics": ["cruciblecraft:t8_fluid_pipe"],
            "consumer": distillery["id"],
        },
        "cruciblecraft:fuel": {
            "producer": distillery["id"],
            "logistics": (
                ["cruciblecraft:fluid_cell"]
                if cell_entries.get("cruciblecraft:fuel") == "fluid"
                else []
            ),
            "consumer": engine["id"],
        },
        "cruciblecraft:natural_gas": {
            "producer": deposits.get("cruciblecraft:natural_gas"),
            "logistics": (
                ["cruciblecraft:t8_fluid_pipe", "cruciblecraft:gas_cell"]
                if cell_entries.get("cruciblecraft:natural_gas") == "gas"
                else []
            ),
            "consumer": generifier["id"],
        },
        "cruciblecraft:methane": {
            "producer": generifier["id"],
            "logistics": (
                ["cruciblecraft:gas_cell"]
                if cell_entries.get("cruciblecraft:methane") == "gas"
                else []
            ),
            "consumer": gas["id"],
        },
    }
    for fluid_id, row in closure.items():
        if not row["producer"] or not row["logistics"] or not row["consumer"]:
            raise ValueError(f"T11 primary fluid closure is incomplete for {fluid_id}")

    coproducts = policy["coproduct_policy"]
    lubricant = coproducts["cruciblecraft:lubricant"]
    if (
        lubricant["fake_voiding_allowed"]
        or cell_entries.get("cruciblecraft:lubricant") != "fluid"
        or not (
            ROOT
            / "gt6_dump/gt6_recipe_dump/maps"
            / f"{lubricant['source_consumer_map']}.json"
        ).is_file()
    ):
        raise ValueError("T11 lubricant coproduct policy is not recoverable")
    exhaust = coproducts["cruciblecraft:carbon_dioxide"]
    if (
        exhaust["fake_voiding_allowed"]
        or exhaust["runtime_disposition"] != "explicit_exhaust_output"
    ):
        raise ValueError("T11 generator exhaust policy drifted")
    return {
        "identity_providers": providers,
        "source_fluid_bridges": policy["identity_gate"][
            "source_fluid_bridges"
        ],
        "primary_fluids": closure,
        "coproducts": coproducts,
        "registered_runtime_identities": sorted(all_ids),
        "required_runtime_identities": sorted(required_ids),
    }


def distillery_ledger(
    policy: dict[str, Any],
    selected: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    route = selected["crude_oil_distillation"]
    expected = policy["source_rows"]["crude_oil_distillation"]
    source = load(ROOT / expected["path"])
    selected_index = route["recipe_index"]
    rows = [
        {
            "classification": (
                "selected_source_pinned_design_bridge"
                if index == selected_index
                else "deferred_outside_minimal_t11_set"
            ),
            "reason_code": (
                "independent_expectation_with_explicit_identity_bridge"
                if index == selected_index
                else "not_in_t11_minimal_hydrocarbon_expectation_set"
            ),
            "recipe_index": index,
            "row_sha256": canonical_hash(recipe),
        }
        for index, recipe in enumerate(source["recipes"])
    ]
    return {
        "classifications": {
            "deferred_outside_minimal_t11_set": (
                "The pinned row remains classified but is not required by the "
                "first producer/logistics/consumer-closed T11 vertical slice."
            ),
            "selected_source_pinned_design_bridge": (
                "The source row exactly matches the independently authored T11 "
                "expectation; its liquid_medium_oil to crude_oil identity bridge "
                "is explicit DESIGN_POLICY with UNVERIFIED GT6 equivalence."
            ),
        },
        "counts": {
            "classified": len(rows),
            "deferred_outside_minimal_t11_set": len(rows) - 1,
            "selected_source_pinned_design_bridge": 1,
            "source_rows": len(rows),
            "unclassified": 0,
        },
        "rows": rows,
        "selected_recipe_index": selected_index,
    }


def source_map_ledgers(
    policy: dict[str, Any],
    selected: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    ledgers: dict[str, Any] = {}
    for route_id in (
        "natural_gas_to_methane",
        "fuel_oil_engine",
        "methane_gas_fuel",
    ):
        expected = policy["source_rows"][route_id]
        source = load(ROOT / expected["path"])
        selected_index = selected[route_id]["recipe_index"]
        rows = [
            {
                "classification": (
                    "selected_source_backed"
                    if index == selected_index
                    else "deferred_outside_minimal_t11_set"
                ),
                "reason_code": (
                    "independent_expectation_match"
                    if index == selected_index
                    else "not_in_t11_minimal_hydrocarbon_expectation_set"
                ),
                "recipe_index": index,
                "row_sha256": canonical_hash(recipe),
            }
            for index, recipe in enumerate(source["recipes"])
        ]
        ledgers[route_id] = {
            "counts": {
                "classified": len(rows),
                "deferred_outside_minimal_t11_set": len(rows) - 1,
                "selected_source_backed": 1,
                "source_rows": len(rows),
                "unclassified": 0,
            },
            "rows": rows,
            "selected_recipe_index": selected_index,
        }
    return ledgers


def runtime_policy_acceptance(
    policy: dict[str, Any],
) -> dict[str, Any]:
    path = ROOT / policy["load_policy"]["runtime_policy"]
    document = load(path)
    production = document["production"]
    raw_oil = document["raw_oil_identity"]
    publication = document["publication_policy"]
    expected = {
        "cruciblecraft:crude_oil": {
            "state": "liquid",
            "amount_mb": 25,
            "interval_ticks": 20,
            "accumulation_cap_mb": 1000,
            "vent_overflow": False,
        },
        "cruciblecraft:natural_gas": {
            "state": "gas",
            "amount_mb": 5,
            "interval_ticks": 20,
            "accumulation_cap_mb": 1000,
            "vent_overflow": True,
        },
    }
    if (
        document.get("schema_version") != 1
        or document.get("status") != "DESIGN_POLICY"
        or document.get("source_revision") != policy["source_revision"]
        or production != expected
        or document["depletion"] != {
            "mode": "non_depleting",
            "rate_decay": False,
            "legacy_reserve_role": "migration_and_diagnostics_only",
        }
        or document["deposit_migrations"] != {
            "cruciblecraft:methane": "cruciblecraft:natural_gas"
        }
        or document["gas_cloud"]["maximum_emitted_mb"] != 1000
        or "PROPERTIES.FLAMMABLE"
        not in document["hazards"]["cruciblecraft:natural_gas"][
            "material_tags"
        ]
        or document["source_policy"]["gt6_spring_amounts_are_rates"]
        or raw_oil != {
            "status": "DESIGN_POLICY",
            "closure": "O37_CLOSED_PERMANENT_DESIGN_POLICY",
            "worldgen_material": "cruciblecraft:crude_oil",
            "distillery_source_fluid": "liquid_medium_oil",
            "distillery_runtime_input": "cruciblecraft:crude_oil",
            "registered_runtime_fluid": "cruciblecraft:crude_oil",
            "registration_owner": "t11_hydrocarbon_fluid_gate",
            "material_9852": "MT.CrudeOil",
            "material_9852_role": "SOURCE_MATERIAL_LAYER_ONLY",
            "t9_identity_migration": "NONE",
            "duplicate_registration_allowed": False,
            "fluid_id_changed": False,
            "publication_delta": 0,
            "evidence": "tools/t18_o37_identity_projection.json",
        }
        or publication != {
            "t18_o37_recipe_publication_delta": 0,
            "t18_o37_fluid_registration_delta": 0,
            "duplicate_fluid_registration_allowed": False,
        }
    ):
        raise ValueError("T11 hydrocarbon runtime policy drifted")
    return {
        "depletion": document["depletion"],
        "deposit_migrations": document["deposit_migrations"],
        "gas_cloud": document["gas_cloud"],
        "hazards": document["hazards"],
        "path": path.relative_to(ROOT).as_posix(),
        "raw_oil_identity": raw_oil,
        "publication_policy": publication,
        "production": production,
        "sha256": digest(path),
        "status": document["status"],
    }


def load_gate(policy: dict[str, Any], selected_count: int) -> dict[str, Any]:
    expected = policy["load_policy"]
    t10 = load(T10_PREFLIGHT)
    current_entries = t10["load_gate"]["datapack_recipe_entries"]
    current_publication = t10["load_gate"]["published_recipes"]
    if (
        current_entries != expected["current_datapack_recipe_entries"]
        or current_publication != expected["current_published_recipes"]
    ):
        raise ValueError("T10 load baseline drifted before T11")
    additions = expected["projected_t11_published_recipe_additions"]
    entry_additions = expected["projected_t11_datapack_recipe_additions"]
    if (
        additions != selected_count
        or entry_additions
        != selected_count
        + expected["projected_t11_machine_crafting_recipe_additions"]
    ):
        raise ValueError("T11 selected rows and projected additions diverged")
    projected_publication = current_publication + additions
    projected_entries = current_entries + entry_additions
    margin_ceiling = (
        projected_publication * expected["margin_numerator"]
        + expected["margin_denominator"]
        - 1
    ) // expected["margin_denominator"]
    global_budget = round_up(margin_ceiling, expected["round_up_to"])
    if global_budget != expected["expected_global_budget"]:
        raise ValueError(f"T11 global publication budget drifted: {global_budget}")
    projected_chemical = (
        expected["current_t5_chemical_recipes"]
        + expected["projected_chemical_recipe_additions"]
    )
    compression = projected_publication / projected_entries
    if (
        projected_entries > expected["datapack_recipe_entry_budget"]
        or projected_publication > global_budget
        or projected_chemical > expected["t5_chemical_recipe_budget"]
        or compression < expected["minimum_compression_ratio"]
    ):
        raise ValueError("T11 projected load gate failed")

    java = PROCESSING_MACHINES.read_text(encoding="utf-8")
    gate_source = CHEMICAL_GATE_SOURCE.read_text(encoding="utf-8")
    runtime_contract = {
        "chemical_budget_constant_present": (
            "T5_CHEMICAL_RECIPE_BUDGET = 200" in java
        ),
        "fluid_gate_resource_present": (
            "/data/cruciblecraft/t11_hydrocarbon_fluid_gate.json"
            in gate_source
        ),
        "index_build_budget_ms": expected["index_build_budget_ms"],
        "reload_budget_ms": expected["reload_budget_ms"],
        "t11_material_rule_budget_constant_present": (
            "T11_AUTHORED_MATERIAL_RULE_BUDGET = 0" in java
            and "11, T11_AUTHORED_MATERIAL_RULE_BUDGET" in java
        ),
    }
    if not all(
        value
        for key, value in runtime_contract.items()
        if key.endswith("_present")
    ):
        raise ValueError("T11 runtime budget/resource contract is incomplete")
    return {
        "current": {
            "datapack_recipe_entries": current_entries,
            "published_recipes": current_publication,
            "t5_chemical_recipes": expected["current_t5_chemical_recipes"],
        },
        "projected": {
            "compression_ratio": round(compression, 6),
            "datapack_recipe_entries": projected_entries,
            "published_recipes": projected_publication,
            "t5_plus_t11_chemical_recipes": projected_chemical,
        },
        "budgets": {
            "datapack_recipe_entries": expected[
                "datapack_recipe_entry_budget"
            ],
            "minimum_compression_ratio": expected[
                "minimum_compression_ratio"
            ],
            "published_recipes": global_budget,
            "t5_plus_t11_chemical_recipes": expected[
                "t5_chemical_recipe_budget"
            ],
            "t11_authored_material_rules": expected[
                "t11_authored_material_rule_budget"
            ],
        },
        "runtime_contract": runtime_contract,
        "status": "READY",
    }


def planned_documents() -> dict[Path, str]:
    policy = load(POLICY)
    selected, maps = select_routes(policy)
    fluid_gate = build_fluid_gate(policy, selected)
    cell_gate = build_cell_gate(policy)
    closure = build_closure(
        policy, selected, fluid_gate, cell_gate
    )
    ledger = distillery_ledger(policy, selected)
    map_ledgers = source_map_ledgers(policy, selected)
    runtime_policy = runtime_policy_acceptance(policy)
    load_acceptance = load_gate(policy, len(selected))
    route_ids = sorted(selected)
    projection = {
        "schema_version": 1,
        "status": "T11_READY",
        "source_revision": policy["source_revision"],
        "sources": {
            "builder": {
                "path": Path(__file__).resolve().relative_to(ROOT).as_posix(),
                "sha256": digest(Path(__file__).resolve()),
            },
            "policy": {
                "path": POLICY.relative_to(ROOT).as_posix(),
                "sha256": digest(POLICY),
            },
            "runtime_policy": {
                "path": runtime_policy["path"],
                "sha256": runtime_policy["sha256"],
            },
            "o37_identity": {
                "path": O37_IDENTITY.relative_to(ROOT).as_posix(),
                "sha256": digest(O37_IDENTITY),
            },
            "maps": maps,
            "fluid_gates": {
                T5_FLUID_GATE.relative_to(ROOT).as_posix(): digest(
                    T5_FLUID_GATE
                ),
                T10_FLUID_GATE.relative_to(ROOT).as_posix(): digest(
                    T10_FLUID_GATE
                ),
                FLUID_GATE.relative_to(ROOT).as_posix(): canonical_hash(
                    fluid_gate
                ),
                T11_CELL_GATE.relative_to(ROOT).as_posix(): canonical_hash(
                    cell_gate
                ),
            },
        },
        "independent_expectation": {
            "route_ids": route_ids,
            "route_set_sha256": canonical_hash(route_ids),
            "selected_count": len(route_ids),
        },
        "selected_routes": selected,
        "distillery_ledger": ledger,
        "source_map_ledgers": map_ledgers,
        "fluid_identity_closure": closure,
        "runtime_policy": runtime_policy,
        "load_gate": load_acceptance,
    }
    return {
        FLUID_GATE: stable(fluid_gate),
        T11_CELL_GATE: stable(cell_gate),
        OUTPUT: stable(projection),
    }


def build() -> dict[str, Any]:
    planned = planned_documents()
    return json.loads(planned[OUTPUT])


def check() -> list[str]:
    planned = planned_documents()
    return [
        (
            f"missing generated file: {path.relative_to(ROOT).as_posix()}"
            if not path.is_file()
            else f"stale generated file: {path.relative_to(ROOT).as_posix()}"
        )
        for path, content in planned.items()
        if not path.is_file() or path.read_text(encoding="utf-8") != content
    ]


def reference_only_check() -> list[str]:
    errors: list[str] = []
    required = (FLUID_GATE, T11_CELL_GATE, OUTPUT)
    if any(not path.is_file() for path in required):
        return ["T11 compact artifacts are missing"]
    try:
        projection = load(OUTPUT)
        fluid_gate = load(FLUID_GATE)
        cell_gate = load(T11_CELL_GATE)
        for path, document in (
            (OUTPUT, projection),
            (FLUID_GATE, fluid_gate),
            (T11_CELL_GATE, cell_gate),
        ):
            if path.read_text(encoding="utf-8") != stable(document):
                errors.append(
                    f"non-canonical generated file: "
                    f"{path.relative_to(ROOT).as_posix()}"
                )
        sources = projection.get("sources") or {}
        if (sources.get("builder") or {}).get("sha256") != digest(
            Path(__file__).resolve()
        ):
            errors.append("T11 builder hash drifted")
        if (sources.get("policy") or {}).get("sha256") != digest(POLICY):
            errors.append("T11 policy hash drifted")
        runtime_path = ROOT / (sources.get("runtime_policy") or {}).get(
            "path", ""
        )
        if (
            not runtime_path.is_file()
            or (sources.get("runtime_policy") or {}).get("sha256")
            != digest(runtime_path)
        ):
            errors.append("T11 runtime policy hash drifted")
        if (sources.get("o37_identity") or {}) != {
            "path": O37_IDENTITY.relative_to(ROOT).as_posix(),
            "sha256": digest(O37_IDENTITY),
        }:
            errors.append("T11 O-37 identity evidence hash drifted")

        t13 = load(T13_RECIPE_MAPS)
        map_rows = {
            row["name_internal"]: row for row in t13["rows"]
        }
        for map_name, evidence in (sources.get("maps") or {}).items():
            row = map_rows.get(map_name)
            if row is None or (
                evidence.get("sha256"),
                evidence.get("recipe_count"),
            ) != (
                row["source_blob"],
                row["recipe_count"],
            ):
                errors.append(f"T11 source map receipt drifted: {map_name}")
        expected_gates = {
            T5_FLUID_GATE.relative_to(ROOT).as_posix(): digest(T5_FLUID_GATE),
            T10_FLUID_GATE.relative_to(ROOT).as_posix(): digest(T10_FLUID_GATE),
            FLUID_GATE.relative_to(ROOT).as_posix(): canonical_hash(fluid_gate),
            T11_CELL_GATE.relative_to(ROOT).as_posix(): canonical_hash(cell_gate),
        }
        if sources.get("fluid_gates") != expected_gates:
            errors.append("T11 fluid gate hashes drifted")

        selected = projection.get("selected_routes") or {}
        expectation = projection.get("independent_expectation") or {}
        route_ids = sorted(selected)
        if (
            projection.get("status") != "T11_READY"
            or expectation.get("route_ids") != route_ids
            or expectation.get("selected_count") != len(route_ids)
            or expectation.get("route_set_sha256")
            != canonical_hash(route_ids)
        ):
            errors.append("T11 route expectation is inconsistent")
        for name, ledger in (
            projection.get("source_map_ledgers") or {}
        ).items():
            rows = ledger.get("rows") or []
            counts = ledger.get("counts") or {}
            if (
                counts.get("source_rows") != len(rows)
                or counts.get("unclassified") != 0
                or name not in selected
            ):
                errors.append(f"T11 source map ledger drifted: {name}")
    except (KeyError, OSError, TypeError, ValueError, json.JSONDecodeError) as exc:
        errors.append(str(exc))
    return errors


def write() -> dict[str, Any]:
    planned = planned_documents()
    for path, content in planned.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8", newline="\n")
    return json.loads(planned[OUTPUT])


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    replay_mode = parser.add_mutually_exclusive_group()
    replay_mode.add_argument("--reference-only", action="store_true")
    replay_mode.add_argument("--full-replay", action="store_true")
    args = parser.parse_args()
    if (args.reference_only or args.full_replay) and not args.check:
        parser.error("--reference-only and --full-replay require --check")
    if args.check:
        errors = reference_only_check() if args.reference_only else check()
        if errors:
            print("T11 preflight is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T11 preflight matches committed artifacts.")
        return 0
    document = write()
    print(
        "T11 preflight: "
        f"{document['independent_expectation']['selected_count']} source rows, "
        f"{len(document['fluid_identity_closure']['primary_fluids'])} "
        "primary fluid closures"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
