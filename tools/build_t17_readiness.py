#!/usr/bin/env python3
"""Build the staged T17 readiness artifact."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

try:
    from tools import build_t17_machine_denominator as denominator_builder
except ModuleNotFoundError:
    import build_t17_machine_denominator as denominator_builder
try:
    from tools import build_t17_machine_acquisition as acquisition_builder
except ModuleNotFoundError:
    import build_t17_machine_acquisition as acquisition_builder
try:
    from tools import recipe_load_projection
except ModuleNotFoundError:
    import recipe_load_projection


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t17_readiness_policy.json"
OUTPUT = TOOLS / "t17_readiness.json"
MACHINE_TIERS = (
    ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
)
LOAD_INPUT = TOOLS / "t17_load_projection_input.json"
LOAD_OUTPUT = TOOLS / "t17_load_projection.json"
PUBLICATION_BASELINE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/t17_publication_baseline.json"
)
T16_PUBLICATION_BASELINE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/t16_publication_baseline.json"
)
T14_READINESS = TOOLS / "t14_readiness.json"
GAMETESTS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/"
    "CrucibleCraftGameTests.java"
)
STAGE_ORDER = ("T17a", "T17b", "T17c", "T17d")


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
        or policy.get("status") != "T17_READINESS_POLICY"
    ):
        raise ValueError("T17 readiness policy header drifted")
    stages = policy.get("stages") or {}
    if tuple(stages) != STAGE_ORDER:
        raise ValueError("T17 readiness stages must remain ordered T17a-d")
    status_policy = policy.get("status_policy") or {}
    complete = status_policy.get("complete_stage_status")
    pending = status_policy.get("pending_stage_status")
    pending_seen = False
    for stage_id, row in stages.items():
        status = row.get("status")
        if status not in {complete, pending}:
            raise ValueError(f"{stage_id}: invalid readiness status")
        if not str(row.get("acceptance") or "").strip():
            raise ValueError(f"{stage_id}: acceptance is missing")
        if status == pending:
            pending_seen = True
            for field in (
                "reason",
                "replacement_condition",
                "recheck_point",
            ):
                if not str(row.get(field) or "").strip():
                    raise ValueError(
                        f"{stage_id}: pending gate lacks {field}"
                    )
        elif pending_seen:
            raise ValueError(
                f"{stage_id}: cannot complete after an earlier pending gate"
            )
    if any(stages[stage]["status"] != complete for stage in STAGE_ORDER):
        raise ValueError("T17 readiness requires T17a-d complete")


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


def dependencies(policy: dict[str, Any]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for owner, spec in policy["dependencies"].items():
        path = ROOT / spec["path"]
        document = load(path)
        if document.get("status") != spec["required_status"]:
            raise ValueError(
                f"{owner}: expected dependency status "
                f"{spec['required_status']}"
            )
        result[owner] = {
            "path": spec["path"],
            "status": document["status"],
            "sha256": sha256(path),
        }
    errors = denominator_builder.check()
    if errors:
        raise ValueError("T17 denominator is stale: " + "; ".join(errors))
    errors = acquisition_builder.check()
    if errors:
        raise ValueError("T17 acquisition is stale: " + "; ".join(errors))
    return result


def source_contracts(
    policy: dict[str, Any],
    completed: list[str],
) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for owner, contract in policy["source_contracts"].items():
        stage = contract.get("stage")
        if stage not in completed:
            raise ValueError(
                f"{owner}: source contract belongs to incomplete {stage}"
            )
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
            "required_tokens": contract["required_tokens"],
        }
    return result


def t17a_evidence(denominator: dict[str, Any]) -> dict[str, Any]:
    tiers = load(MACHINE_TIERS).get("variants") or []
    selected_rows = [
        row for row in denominator["rows"]
        if row["disposition"] == "IMPLEMENTED_T17A"
    ]
    reference_rows = [
        row for row in denominator["rows"]
        if row["disposition"] == "PREIMPLEMENTED_REFERENCE"
    ]
    selected_variants = [
        variant
        for row in selected_rows
        for variant in row["source_variants"]
        if variant["status"] == "IMPLEMENTED"
    ]
    selected_catalog = sorted(
        row["id"] for row in tiers
        if row["kind"] in {
            "cruciblecraft:distillery",
            "cruciblecraft:drying",
            "cruciblecraft:smelter",
        }
    )
    expected_catalog = sorted({
        "cruciblecraft:distillery",
        "cruciblecraft:invar_distillery",
        "cruciblecraft:titanium_distillery",
        "cruciblecraft:drying",
        "cruciblecraft:invar_drying",
        "cruciblecraft:titanium_drying",
        "cruciblecraft:smelter",
        "cruciblecraft:invar_smelter",
        "cruciblecraft:titanium_smelter",
    })
    counts = denominator["counts"]
    if (
        denominator.get("status") != "T17_MACHINE_DENOMINATOR_READY"
        or counts.get("t17_owner_hu_eu_kinds") != 28
        or counts.get("energy_kinds") != {"EU": 16, "HU": 12}
        or counts.get("unclassified") != 0
        or len(selected_rows) != 3
        or len(reference_rows) != 1
        or len(selected_variants) != 9
        or len(tiers) != 33
        or selected_catalog != expected_catalog
    ):
        raise ValueError("T17a denominator/runtime matrix is incomplete")
    return {
        "implemented_kinds": sorted(
            row["recipe_map"] for row in selected_rows
        ),
        "implemented_variants": len(selected_variants),
        "preimplemented_reference": reference_rows[0]["recipe_map"],
        "owner_kinds": counts["t17_owner_hu_eu_kinds"],
        "energy_kind_counts": counts["energy_kinds"],
        "deferred_kinds": counts["deferred_kinds"],
        "heat_tier4_deferred": counts["heat_tier4_deferred"],
        "electric_tier4_5_deferred": counts[
            "electric_tier4_5_deferred"
        ],
        "catalog_variants": len(tiers),
        "selected_catalog_variants": selected_catalog,
        "new_block_registrations": 6,
        "new_item_registrations": 6,
        "gt_recipe_row_mutation": 0,
        "unclassified": counts["unclassified"],
    }


def t17b_evidence(
    policy: dict[str, Any],
    contracts: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    tiers = load(MACHINE_TIERS).get("variants") or []
    hu_kinds = {
        "cruciblecraft:distillery": [8, 16, 32],
        "cruciblecraft:drying": [8, 16, 32],
        "cruciblecraft:smelter": [1_000, 1_000, 1_000],
    }
    hu_rows = [
        row for row in tiers if row.get("kind") in hu_kinds
    ]
    eu_rows = [
        row for row in tiers
        if row.get("kind") == "cruciblecraft:electrolyzer"
    ]
    mixer_rows = [
        row for row in tiers
        if row.get("kind") == "cruciblecraft:mixer"
    ]
    for kind, parallels in hu_kinds.items():
        rows = sorted(
            (row for row in hu_rows if row["kind"] == kind),
            key=lambda row: row["sourceTier"],
        )
        if (
            len(rows) != 3
            or [row["parallel"] for row in rows] != parallels
            or any(row["energy"] != "HEAT" for row in rows)
            or any(row["overclock"] != "CHEAP" for row in rows)
            or any(not row["parallelDuration"] for row in rows)
        ):
            raise ValueError(f"T17b HU execution matrix drifted for {kind}")
    eu_rows.sort(key=lambda row: row["sourceTier"])
    if (
        len(hu_rows) != 9
        or len(eu_rows) != 3
        or mixer_rows
        or [row["sourceTier"] for row in eu_rows] != [1, 2, 3]
        or [row["parallel"] for row in eu_rows] != [1, 2, 4]
        or any(row["energy"] != "ELECTRIC" for row in eu_rows)
        or any(row["overclock"] != "STANDARD" for row in eu_rows)
        or any(not row["parallelDuration"] for row in eu_rows)
    ):
        raise ValueError("T17b Electrolyzer reference matrix drifted")
    for row in [*hu_rows, *eu_rows]:
        nominal = row["inputNominal"]
        if (
            row["inputMinimum"] != nominal // 2
            or row["inputMaximum"] != nominal * 2
            or row["energyCapacity"] != row["inputMaximum"]
        ):
            raise ValueError(
                f"T17b input window drifted for {row['id']}"
            )

    gate = policy["stages"]["T17b"]
    contract_owners = gate.get("source_contracts") or []
    if (
        not gate.get("junit_tests")
        or not gate.get("game_tests")
        or len(contract_owners) != len(set(contract_owners))
        or any(
            owner not in contracts
            or contracts[owner]["stage"] != "T17b"
            for owner in contract_owners
        )
    ):
        raise ValueError("T17b tests/source contracts are incomplete")
    return {
        "hu_execution_kinds": sorted(hu_kinds),
        "hu_execution_variants": len(hu_rows),
        "hu_parallel_limits": hu_kinds,
        "hu_energy_contract": "HEAT_ADJACENT_BOTTOM_FIREBOX",
        "eu_reference": "cruciblecraft:electrolyzer",
        "eu_reference_variants": [row["id"] for row in eu_rows],
        "eu_parallel_limits": [row["parallel"] for row in eu_rows],
        "eu_energy_contract": "ELECTRIC_BUFFERED_CABLE_ENDPOINT",
        "electric_mixer_tier_variants": len(mixer_rows),
        "junit_tests": gate["junit_tests"],
        "game_tests": gate["game_tests"],
        "source_contracts": contract_owners,
    }


def t17c_evidence(
    policy: dict[str, Any],
    contracts: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    acquisition = load(acquisition_builder.OUTPUT)
    counts = acquisition.get("counts") or {}
    expected = acquisition.get("expected_set") or {}
    references = acquisition.get("preimplemented_references") or []
    if (
        acquisition.get("status")
        != "T17C_MACHINE_ACQUISITION_READY"
        or counts.get("selected_variants") != 9
        or counts.get("casing_dependencies") != 2
        or counts.get("machine_ingredient_rows") != 42
        or counts.get("registered_machine_results") != 9
        or counts.get("preimplemented_reference_variants") != 3
        or counts.get("gt_recipe_rows_added") != 0
        or counts.get("unreachable") != 0
        or len(expected.get("machine_variant_recipe_ids") or []) != 9
        or len(references) != 3
        or any(
            not row.get("preimplemented")
            or row.get("generated_by_t17c") is not False
            for row in references
        )
    ):
        raise ValueError("T17c acquisition closure is incomplete")

    gate = policy["stages"]["T17c"]
    contract_owners = gate.get("source_contracts") or []
    if (
        not gate.get("junit_tests")
        or not gate.get("game_tests")
        or len(contract_owners) != len(set(contract_owners))
        or any(
            owner not in contracts
            or contracts[owner]["stage"] != "T17c"
            for owner in contract_owners
        )
    ):
        raise ValueError("T17c tests/source contracts are incomplete")
    return {
        "selected_kinds": counts["selected_kinds"],
        "selected_variants": counts["selected_variants"],
        "machine_variant_recipe_ids":
            expected["machine_variant_recipe_ids"],
        "casing_dependencies": counts["casing_dependencies"],
        "machine_ingredient_rows": counts["machine_ingredient_rows"],
        "producer_forms": counts["producer_forms"],
        "preimplemented_electrolyzer_variants": [
            row["result_item"] for row in references
        ],
        "gt_recipe_rows_added": counts["gt_recipe_rows_added"],
        "unreachable": counts["unreachable"],
        "junit_tests": gate["junit_tests"],
        "game_tests": gate["game_tests"],
        "source_contracts": contract_owners,
    }


def t17d_evidence(
    policy: dict[str, Any],
    denominator: dict[str, Any],
    acquisition: dict[str, Any],
) -> dict[str, Any]:
    baseline = load(PUBLICATION_BASELINE)
    t16_baseline = load(T16_PUBLICATION_BASELINE)
    if (
        baseline.get("schema_version") != 1
        or baseline.get("status") != "T17D_PUBLICATION_BASELINE"
        or baseline.get("baseline") != "T16"
    ):
        raise ValueError("T17d publication baseline header drifted")

    expected_map_ids = baseline.get("recipe_map_ids") or []
    t16_map_ids = t16_baseline.get("recipe_map_ids") or []
    actual_map_ids = sorted(denominator_builder.local_recipe_maps())
    if (
        expected_map_ids != sorted(expected_map_ids)
        or len(expected_map_ids) != 32
        or len(expected_map_ids) != len(set(expected_map_ids))
        or expected_map_ids != t16_map_ids
        or expected_map_ids != actual_map_ids
    ):
        raise ValueError("T17d RecipeMap stable ids must equal the T16 baseline")

    t14 = load(T14_READINESS)
    load_gate = t14.get("load_gate") or {}
    publication_totals = baseline.get("publication_totals") or {}
    expected_totals = {
        "logical_rows": load_gate.get("logical_recipes"),
        "eager_rows": load_gate.get("eager_recipes"),
        "lazy_rows": load_gate.get("lazy_recipes"),
    }
    if (
        t14.get("status") != "T14_READY"
        or publication_totals
        != t16_baseline.get("publication_totals")
        or publication_totals != expected_totals
        or publication_totals
        != {
            "logical_rows": 18_875,
            "eager_rows": 16_650,
            "lazy_rows": 2_225,
        }
    ):
        raise ValueError("T17d logical/eager/lazy totals must equal T16")

    load_input = load(LOAD_INPUT)
    projected = recipe_load_projection.project(load_input)
    if projected != load(LOAD_OUTPUT):
        raise ValueError("T17 load projection is stale")
    load_counts = projected.get("ledger", {}).get("counts") or {}
    intervals = projected.get("ledger", {}).get(
        "measurement_intervals"
    ) or {}
    if (
        projected.get("status") != "PASS"
        or projected.get("delivery_phase") != "T17"
        or any(value != 0 for value in load_counts.values())
        or any(
            interval != {"min": 0, "max": 0}
            for interval in intervals.values()
        )
    ):
        raise ValueError("T17d load projection must remain a zero-workload PASS")

    acquisition_baseline = baseline.get("t17_acquisition") or {}
    expected_recipe_ids = acquisition.get("expected_set", {}).get(
        "machine_variant_recipe_ids"
    ) or []
    baseline_recipe_ids = acquisition_baseline.get("vanilla_recipe_ids") or []
    recipe_types = {
        load(ROOT / row["recipe_path"]).get("type")
        for row in acquisition.get("machine_variant_recipes") or []
    }
    if (
        baseline_recipe_ids != sorted(baseline_recipe_ids)
        or baseline_recipe_ids != expected_recipe_ids
        or acquisition_baseline.get("vanilla_crafting_rows") != 9
        or acquisition_baseline.get("gt_recipe_rows") != 0
        or recipe_types != {"minecraft:crafting_shaped"}
    ):
        raise ValueError(
            "T17d acquisition must add exactly 9 vanilla crafting rows "
            "and zero GT rows"
        )

    emi_map_ids = baseline.get("emi_recipe_map_ids") or []
    if (
        emi_map_ids != sorted(emi_map_ids)
        or len(emi_map_ids) != 24
        or len(emi_map_ids) != len(set(emi_map_ids))
        or emi_map_ids != t16_baseline.get("emi_recipe_map_ids")
        or baseline.get("emi_enumeration_contract")
        != t16_baseline.get("emi_enumeration_contract")
        or not set(emi_map_ids).issubset(expected_map_ids)
    ):
        raise ValueError("T17d EMI enumeration must equal the T16 baseline")

    gametest = (policy.get("t17d_evidence") or {}).get("gametest") or {}
    game_source = GAMETESTS.read_text(encoding="utf-8")
    game_tests = gametest.get("tests") or []
    if (
        gametest.get("path") != relative(GAMETESTS)
        or len(game_tests) != 8
        or len(game_tests) != len(set(game_tests))
        or any(name not in game_source for name in game_tests)
        or "expectedEnumeration.equals(actualEnumeration)" not in game_source
        or "expectedMapIds.equals(t16MapIds)" not in game_source
        or "expectedEmiIds.equals(t16EmiIds)" not in game_source
    ):
        raise ValueError("T17d GameTest closure contract drifted")

    counts = denominator["counts"]
    if (
        denominator.get("status") != "T17_MACHINE_DENOMINATOR_READY"
        or counts.get("t17_owner_hu_eu_kinds") != 28
        or counts.get("energy_kinds") != {"EU": 16, "HU": 12}
        or counts.get("unclassified") != 0
        or counts.get("selected_kinds") != 3
        or counts.get("preimplemented_reference_kinds") != 1
        or counts.get("deferred_kinds") != 24
        or counts.get("heat_tier4_deferred") != 10
        or counts.get("electric_tier4_5_deferred") != 32
    ):
        raise ValueError("T17d denominator closure counts drifted")

    return {
        "status": "PASS",
        "denominator": {
            "status": denominator["status"],
            "kinds": counts["t17_owner_hu_eu_kinds"],
            "energy_kinds": counts["energy_kinds"],
            "unclassified": counts["unclassified"],
            "selected_kinds": counts["selected_kinds"],
            "selected_variants": 9,
            "preimplemented_reference_kinds": counts[
                "preimplemented_reference_kinds"
            ],
            "deferred_kinds": counts["deferred_kinds"],
            "heat_tier4_deferred": counts["heat_tier4_deferred"],
            "electric_tier4_5_deferred": counts[
                "electric_tier4_5_deferred"
            ],
        },
        "energy_topology": {
            "HU": "HEAT_ADJACENT_BOTTOM_FIREBOX",
            "EU": "ELECTRIC_BUFFERED_CABLE_ENDPOINT",
            "hu_execution_variants": 9,
            "eu_reference_variants": 3,
            "electric_mixer_tier_variants": 0,
        },
        "migration_acquisition": {
            "exact_legacy_tier1_migrations": 3,
            "vanilla_crafting_rows": 9,
            "gt_recipe_rows": 0,
            "unreachable": acquisition["counts"]["unreachable"],
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
        },
        "publication_baseline": {
            "path": relative(PUBLICATION_BASELINE),
            "sha256": sha256(PUBLICATION_BASELINE),
            "t16_path": relative(T16_PUBLICATION_BASELINE),
            "t16_sha256": sha256(T16_PUBLICATION_BASELINE),
            "recipe_map_ids": expected_map_ids,
            "publication_totals": publication_totals,
            "publication_delta": 0,
            "stable_id_set_equal_to_t16": True,
        },
        "emi_enumeration": {
            "recipe_map_ids": emi_map_ids,
            "configured_maps": len(emi_map_ids),
            "recipe_enumeration_equal_to_t16": True,
            "contract": baseline["emi_enumeration_contract"],
        },
        "gametest": {
            "path": relative(GAMETESTS),
            "tests": game_tests,
            "t17_test_count": len(game_tests),
            "full_suite_test_count": game_source.count("@GameTest("),
        },
    }


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    status, completed, pending = stage_summary(policy)
    dependency_rows = dependencies(policy)
    denominator = load(denominator_builder.OUTPUT)
    contracts = source_contracts(policy, completed)
    evidence = t17a_evidence(denominator)
    execution_evidence = t17b_evidence(policy, contracts)
    acquisition_evidence = t17c_evidence(policy, contracts)
    closure_evidence = t17d_evidence(
        policy, denominator, load(acquisition_builder.OUTPUT)
    )
    return {
        "schema_version": 1,
        "status": status,
        "completed_stages": completed,
        "pending_stages": pending,
        "stage_gates": policy["stages"],
        "t17a_evidence": evidence,
        "t17b_evidence": execution_evidence,
        "t17c_evidence": acquisition_evidence,
        "t17d_evidence": closure_evidence,
        "publication_gate": {
            "status": "PASS",
            "publication_unchanged_claimed": True,
            "publication_delta": 0,
            "recipe_map_stable_id_set_equal_to_t16": True,
            "emi_recipe_enumeration_equal_to_t16": True,
            "logical_eager_lazy_totals_equal_to_t16": True,
        },
        "resource_acquisition_migration_gate": {
            "status": "T17C_COMPLETE",
            "runtime_registrations_are_closure": True,
            "selected_variants": 9,
            "unreachable": 0,
            "acceptance": policy["stages"]["T17c"]["acceptance"],
        },
        "closure_summary": {
            "denominator_kinds": evidence["owner_kinds"],
            "energy_kind_counts": evidence["energy_kind_counts"],
            "unclassified": evidence["unclassified"],
            "selected_kinds": len(evidence["implemented_kinds"]),
            "selected_variants": evidence["implemented_variants"],
            "preimplemented_reference_kinds": 1,
            "deferred_kinds": evidence["deferred_kinds"],
            "gt_recipe_row_mutation": evidence["gt_recipe_row_mutation"],
            "hu_execution_variants": execution_evidence[
                "hu_execution_variants"
            ],
            "eu_reference_variants": len(
                execution_evidence["eu_reference_variants"]
            ),
            "electric_mixer_tier_variants": execution_evidence[
                "electric_mixer_tier_variants"
            ],
            "resources_acquisition_full_closure": "COMPLETE_T17C",
            "machine_acquisition_variants": acquisition_evidence[
                "selected_variants"
            ],
            "machine_acquisition_unreachable": acquisition_evidence[
                "unreachable"
            ],
            "preimplemented_electrolyzer_variants": len(
                acquisition_evidence[
                    "preimplemented_electrolyzer_variants"
                ]
            ),
            "heat_tier4_deferred": evidence["heat_tier4_deferred"],
            "electric_tier4_5_deferred": evidence[
                "electric_tier4_5_deferred"
            ],
            "publication_delta": 0,
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
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
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
        print(f"T17 readiness failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "completed": document["completed_stages"],
        "pending": document["pending_stages"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
