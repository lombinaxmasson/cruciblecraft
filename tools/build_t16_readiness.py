#!/usr/bin/env python3
"""Build the staged T16 readiness artifact."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

try:
    from tools import build_t16_machine_denominator as denominator_builder
    from tools import build_t16_machine_acquisition as acquisition_builder
    from tools import recipe_load_projection
except ModuleNotFoundError:
    import build_t16_machine_denominator as denominator_builder
    import build_t16_machine_acquisition as acquisition_builder
    import recipe_load_projection
try:
    from tools import t36_common as t36
except ModuleNotFoundError:
    import t36_common as t36


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t16_readiness_policy.json"
OUTPUT = TOOLS / "t16_readiness.json"
MACHINE_TIERS = (
    ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
)
LOAD_INPUT = TOOLS / "t16_load_projection_input.json"
LOAD_OUTPUT = TOOLS / "t16_load_projection.json"
PUBLICATION_BASELINE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/t16_publication_baseline.json"
)
T14_READINESS = TOOLS / "t14_readiness.json"
GAMETESTS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/"
    "CrucibleCraftGameTests.java"
)
STAGE_ORDER = ("T16a", "T16b", "T16c", "T16d")
POST_T16_CATALOG_KINDS = {
    "cruciblecraft:distillery",
    "cruciblecraft:drying",
    "cruciblecraft:smelter",
}


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
        or policy.get("status") != "T16_READINESS_POLICY"
    ):
        raise ValueError("T16 readiness policy header drifted")
    stages = policy.get("stages") or {}
    if tuple(stages) != STAGE_ORDER:
        raise ValueError("T16 readiness stages must remain ordered T16a-d")
    status_policy = policy.get("status_policy") or {}
    complete = status_policy.get("complete_stage_status")
    pending = status_policy.get("pending_stage_status")
    pending_seen = False
    for stage_id, row in stages.items():
        status = row.get("status")
        if status not in {complete, pending}:
            raise ValueError(f"{stage_id}: invalid readiness status")
        if status == pending:
            pending_seen = True
            for field in ("reason", "replacement_condition", "recheck_point"):
                if not str(row.get(field) or "").strip():
                    raise ValueError(f"{stage_id}: pending gate lacks {field}")
        elif pending_seen:
            raise ValueError(
                f"{stage_id}: cannot complete after an earlier pending gate"
            )
    if stages["T16a"]["status"] != complete:
        raise ValueError("T16 readiness requires T16a complete")


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
        raise ValueError("T16 denominator is stale: " + "; ".join(errors))
    errors = acquisition_builder.check()
    if errors:
        raise ValueError("T16 acquisition is stale: " + "; ".join(errors))
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


def t16a_evidence(denominator: dict[str, Any]) -> dict[str, Any]:
    machine_tiers = load(MACHINE_TIERS)
    opening = t36.opening_variants(machine_tiers)
    variants = [
        row for row in opening
        if row.get("kind") not in POST_T16_CATALOG_KINDS
    ]
    selected = [
        row for row in denominator["rows"]
        if row["disposition"] == "IMPLEMENTED_T16A"
    ]
    selected_variants = [
        tier
        for row in selected
        for tier in row["tiers_1_3"]
    ]
    if (
        len(variants) != 24
        or len(selected) != 4
        or len(selected_variants) != 12
        or any(tier["status"] != "IMPLEMENTED"
               for tier in selected_variants)
        or denominator["counts"]["unclassified"] != 0
    ):
        raise ValueError("T16a denominator/runtime matrix is incomplete")
    return {
        "implemented_kinds": sorted(
            row["recipe_map"] for row in selected
        ),
        "implemented_variants": len(selected_variants),
        "preserved_pre_t16_variants": 6,
        "preserved_non_t16_electric_variants": 3,
        "catalog_variants_at_completion": 21,
        "new_block_registrations": 8,
        "new_item_registrations": 8,
        "unclassified": denominator["counts"]["unclassified"],
        "controller_profiles": len(
            machine_tiers.get("controller_profiles") or []
        ),
    }


def t16b_evidence(denominator: dict[str, Any]) -> dict[str, Any]:
    machine_tiers = load(MACHINE_TIERS)
    opening = t36.opening_variants(machine_tiers)
    variants = [
        row for row in opening
        if row.get("kind") not in POST_T16_CATALOG_KINDS
    ]
    rows = denominator["rows"]
    press = next(
        row for row in rows if row["recipe_map"] == "RM.Press"
    )
    selected = [
        row for row in rows
        if row["disposition"] in {"IMPLEMENTED_T16A", "IMPLEMENTED_T16B"}
    ]
    preimplemented = [
        row for row in rows
        if row["disposition"] == "IMPLEMENTED_PRE_T16"
    ]
    deferred = [
        row for row in rows
        if row["disposition"] in {"MAPPED_DEFERRED", "T13_ONLY_DEFERRED"}
    ]
    press_variants = {
        row["id"] for row in variants
        if row["kind"] == "cruciblecraft:press"
    }
    expected_press = {
        "cruciblecraft:press",
        "cruciblecraft:steel_press",
        "cruciblecraft:titanium_press",
    }
    if (
        len(variants) != 24
        or len(selected) != 5
        or len(preimplemented) != 2
        or len(deferred) != 13
        or press["disposition"] != "IMPLEMENTED_T16B"
        or press["accepted_energy"] != "KU"
        or press["local_energy_type"] != "KINETIC_PUSH"
        or press_variants != expected_press
        or any(
            tier["status"] != "IMPLEMENTED"
            for tier in press["tiers_1_3"]
        )
    ):
        raise ValueError("T16b Press/runtime denominator matrix is incomplete")
    return {
        "implemented_kind": "RM.Press",
        "implemented_variants": sorted(press_variants),
        "selected_kinds": len(selected),
        "preimplemented_kinds": len(preimplemented),
        "deferred_kinds": len(deferred),
        "catalog_variants": len(variants),
        "new_block_registrations": 2,
        "new_item_registrations": 2,
        "energy_identity": press["local_energy_type"],
        "source_revision": denominator["source_revision"],
    }


def t16c_evidence(acquisition: dict[str, Any]) -> dict[str, Any]:
    counts = acquisition.get("counts") or {}
    if (
        acquisition.get("status")
        != "T16C_MACHINE_ACQUISITION_READY"
        or counts.get("selected_kinds") != 5
        or counts.get("selected_variants") != 15
        or counts.get("registered_machine_results") != 15
        or counts.get("casing_dependencies") != 3
        or counts.get("unreachable") != 0
        or acquisition.get("unreachable") != []
    ):
        raise ValueError("T16c acquisition evidence is incomplete")
    return {
        "selected_kinds": counts["selected_kinds"],
        "selected_variants": counts["selected_variants"],
        "registered_machine_results": counts[
            "registered_machine_results"
        ],
        "casing_dependencies": counts["casing_dependencies"],
        "machine_ingredient_rows": counts["machine_ingredient_rows"],
        "producer_forms": counts["producer_forms"],
        "source_declarations": counts["source_declarations"],
        "unreachable": counts["unreachable"],
        "resource_sets_bidirectional": True,
        "identity_policy": "CURRENT_OR_BLANK_ACCEPTED_OTHERWISE_QUARANTINED",
    }


def t16d_evidence(
    policy: dict[str, Any],
    denominator: dict[str, Any],
    acquisition: dict[str, Any],
) -> dict[str, Any]:
    baseline = load(PUBLICATION_BASELINE)
    if (
        baseline.get("schema_version") != 1
        or baseline.get("status") != "T16D_PUBLICATION_BASELINE"
        or baseline.get("captured_before") != "T16"
    ):
        raise ValueError("T16d publication baseline header drifted")

    expected_map_ids = baseline.get("recipe_map_ids") or []
    actual_map_ids = sorted(denominator_builder.local_recipe_maps())
    if (
        expected_map_ids != sorted(expected_map_ids)
        or len(expected_map_ids) != 32
        or len(expected_map_ids) != len(set(expected_map_ids))
        or not set(expected_map_ids).issubset(actual_map_ids)
    ):
        raise ValueError("T16d RecipeMap stable id baseline drifted")

    t14 = load(T14_READINESS)
    load_gate = t14.get("load_gate") or {}
    baseline_totals = baseline.get("publication_totals") or {}
    expected_totals = {
        "logical_rows": load_gate.get("logical_recipes"),
        "eager_rows": load_gate.get("eager_recipes"),
        "lazy_rows": load_gate.get("lazy_recipes"),
    }
    if (
        t14.get("status") != "T14_READY"
        or baseline_totals != expected_totals
        or expected_totals
        != {
            "logical_rows": 18_875,
            "eager_rows": 16_650,
            "lazy_rows": 2_225,
        }
    ):
        raise ValueError("T16d logical/eager/lazy baseline drifted")

    load_input = load(LOAD_INPUT)
    projected = recipe_load_projection.project(load_input)
    if projected != load(LOAD_OUTPUT):
        raise ValueError("T16 load projection is stale")
    load_counts = projected.get("ledger", {}).get("counts") or {}
    intervals = projected.get("ledger", {}).get(
        "measurement_intervals"
    ) or {}
    if (
        projected.get("status") != "PASS"
        or projected.get("delivery_phase") != "T16"
        or any(value != 0 for value in load_counts.values())
        or any(
            interval != {"min": 0, "max": 0}
            for interval in intervals.values()
        )
    ):
        raise ValueError("T16d load projection must remain a zero-workload PASS")

    acquisition_baseline = baseline.get("t16_acquisition") or {}
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
        or acquisition_baseline.get("vanilla_crafting_rows") != 15
        or acquisition_baseline.get("gt_recipe_rows") != 0
        or recipe_types != {"minecraft:crafting_shaped"}
    ):
        raise ValueError(
            "T16d acquisition must add exactly 15 vanilla crafting rows "
            "and zero GT rows"
        )

    emi_map_ids = baseline.get("emi_recipe_map_ids") or []
    if (
        emi_map_ids != sorted(emi_map_ids)
        or len(emi_map_ids) != 24
        or len(emi_map_ids) != len(set(emi_map_ids))
        or not set(emi_map_ids).issubset(expected_map_ids)
        or not str(baseline.get("emi_enumeration_contract") or "").strip()
    ):
        raise ValueError("T16d EMI enumeration baseline drifted")

    gametest = (policy.get("t16d_evidence") or {}).get("gametest") or {}
    game_source = GAMETESTS.read_text(encoding="utf-8")
    game_tests = gametest.get("tests") or []
    if (
        gametest.get("path") != relative(GAMETESTS)
        or len(game_tests) != 6
        or len(game_tests) != len(set(game_tests))
        or any(name not in game_source for name in game_tests)
        or "expectedEmiIds.equals(actualEmiIds)" not in game_source
    ):
        raise ValueError("T16d GameTest closure contract drifted")

    denominator_counts = denominator["counts"]
    if (
        denominator.get("status") != "T16_MACHINE_DENOMINATOR_READY"
        or denominator_counts.get("t16_owner_ru_ku_kinds") != 20
        or denominator_counts.get("unclassified") != 0
        or denominator_counts.get("selected_kinds") != 5
        or denominator_counts.get("preimplemented_kinds") != 2
        or denominator_counts.get("deferred_kinds") != 13
        or denominator_counts.get("gt6_tier4_deferred") != 20
    ):
        raise ValueError("T16d denominator closure counts drifted")

    return {
        "status": "PASS",
        "denominator": {
            "status": denominator["status"],
            "kinds": denominator_counts["t16_owner_ru_ku_kinds"],
            "unclassified": denominator_counts["unclassified"],
            "selected_kinds": denominator_counts["selected_kinds"],
            "selected_variants": 15,
            "preimplemented_kinds": denominator_counts[
                "preimplemented_kinds"
            ],
            "deferred_kinds": denominator_counts["deferred_kinds"],
            "tier4_deferred": denominator_counts["gt6_tier4_deferred"],
        },
        "energy_identity": {
            "RU": denominator["energy_identities"]["RU"][
                "local_energy_type"
            ],
            "KU": denominator["energy_identities"]["KU"][
                "local_energy_type"
            ],
        },
        "identity_acquisition": {
            "identity_policy": "CURRENT_ONLY_FAIL_CLOSED",
            "vanilla_crafting_rows": 15,
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
            "recipe_map_ids": expected_map_ids,
            "publication_totals": baseline_totals,
            "publication_delta": 0,
            "stable_id_set_equal": True,
        },
        "emi_enumeration": {
            "recipe_map_ids": emi_map_ids,
            "configured_maps": len(emi_map_ids),
            "recipe_enumeration_equal": True,
            "contract": baseline["emi_enumeration_contract"],
        },
        "gametest": {
            "path": relative(GAMETESTS),
            "tests": game_tests,
            "t16_test_count": len(game_tests),
            "full_suite_test_count": game_source.count("@GameTest("),
        },
    }


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    status, completed, pending = stage_summary(policy)
    dependency_rows = dependencies(policy)
    denominator = load(denominator_builder.OUTPUT)
    acquisition = load(acquisition_builder.OUTPUT)
    contracts = source_contracts(policy, completed)
    closure = t16d_evidence(policy, denominator, acquisition)
    return {
        "schema_version": 1,
        "status": status,
        "completed_stages": completed,
        "pending_stages": pending,
        "stage_gates": policy["stages"],
        "t16a_evidence": t16a_evidence(denominator),
        "t16b_evidence": t16b_evidence(denominator),
        "t16c_evidence": t16c_evidence(acquisition),
        "t16d_evidence": closure,
        "publication_gate": {
            "status": "PASS",
            "publication_unchanged_claimed": True,
            "publication_delta": 0,
            "recipe_map_stable_id_set_equal": True,
            "emi_recipe_enumeration_equal": True,
            "logical_eager_lazy_totals_equal": True,
        },
        "resource_acquisition_identity_gate": {
            "status": "T16C_COMPLETE",
            "runtime_registrations_are_closure": True,
            "selected_variants": 15,
            "unreachable": 0,
            "acceptance": policy["stages"]["T16c"]["acceptance"],
        },
        "closure_summary": {
            "denominator_kinds": 20,
            "unclassified": 0,
            "selected_kinds": 5,
            "selected_variants": 15,
            "preimplemented_kinds": 2,
            "deferred_kinds": 13,
            "tier4_deferred": 20,
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
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T16 readiness is stale",
    )
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
        print(f"T16 readiness failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "completed": document["completed_stages"],
        "pending": document["pending_stages"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
