#!/usr/bin/env python3
"""Build the current T15 residual/acquisition readiness gate."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

try:
    from tools import build_t15_machine_acquisition as acquisition_builder
    from tools import build_t15_matcher_boundary as boundary_builder
    from tools import recipe_load_projection
except ModuleNotFoundError:
    import build_t15_machine_acquisition as acquisition_builder
    import build_t15_matcher_boundary as boundary_builder
    import recipe_load_projection


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t15_readiness_policy.json"
OUTPUT = TOOLS / "t15_readiness.json"
LEGACY_T12A = TOOLS / "t12_machine_readiness.json"
ACQUISITION = TOOLS / "t15_machine_acquisition.json"
MATCHER_BOUNDARY = TOOLS / "t15_matcher_boundary.json"
LOAD_INPUT = TOOLS / "t15_load_projection_input.json"
LOAD_OUTPUT = TOOLS / "t15_load_projection.json"
STAGE_ORDER = ("T15a", "T15b", "T15c", "T15d", "T15e")


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_policy(policy: dict[str, Any]) -> None:
    if policy.get("schema_version") != 1:
        raise ValueError("T15 readiness policy schema must be 1")
    stages = policy.get("stages") or {}
    if tuple(stages) != STAGE_ORDER:
        raise ValueError("T15 readiness stages must remain ordered T15a-e")
    status_policy = policy["status_policy"]
    allowed = {
        status_policy["complete_stage_status"],
        status_policy["pending_stage_status"],
    }
    pending_seen = False
    for stage_id, stage in stages.items():
        status = stage.get("status")
        if status not in allowed:
            raise ValueError(f"{stage_id}: invalid status {status!r}")
        if status == status_policy["pending_stage_status"]:
            pending_seen = True
            if not str(stage.get("replacement_condition") or "").strip():
                raise ValueError(
                    f"{stage_id}: pending stage lacks replacement condition"
                )
        elif pending_seen:
            raise ValueError(
                f"{stage_id}: cannot complete after an earlier pending stage"
            )
    if stages["T15a"]["status"] != status_policy["complete_stage_status"]:
        raise ValueError("T15a single-source-of-truth gate must be complete")

    supersession = policy["supersession"]
    expected = {
        "historical_artifact": "tools/t12a_machine_readiness.json",
        "historical_status": "T12A_READY",
        "historical_load_status": "T12A_PREPROJECTION_READY",
        "historical_builder": "tools/build_t12_machine_readiness.py",
        "current_artifact": "tools/t15_readiness.json",
        "current_builder": "tools/build_t15_readiness.py",
        "current_owner": "T15",
    }
    drift = {
        key: (supersession.get(key), value)
        for key, value in expected.items()
        if supersession.get(key) != value
    }
    if drift:
        raise ValueError(f"T15 supersession policy drifted: {drift}")
    if (
        not supersession.get("history_preserved")
        or supersession.get("historical_currentness_claimed")
    ):
        raise ValueError("T12a must be preserved without a currentness claim")


def stage_summary(
    policy: dict[str, Any],
) -> tuple[str, list[str], list[str]]:
    validate_policy(policy)
    complete_status = policy["status_policy"]["complete_stage_status"]
    completed = [
        stage_id
        for stage_id, stage in policy["stages"].items()
        if stage["status"] == complete_status
    ]
    pending = [
        stage_id for stage_id in STAGE_ORDER if stage_id not in completed
    ]
    status = (
        policy["status_policy"]["ready"]
        if not pending
        else policy["status_policy"]["in_progress"]
    )
    return status, completed, pending


def historical_evidence(policy: dict[str, Any]) -> dict[str, Any]:
    supersession = policy["supersession"]
    path = ROOT / supersession["historical_artifact"]
    if LEGACY_T12A.exists():
        raise ValueError(
            "legacy tools/t12_machine_readiness.json must not coexist with "
            "the frozen T12a artifact"
        )
    document = load(path)
    if (
        document.get("status") != supersession["historical_status"]
        or document.get("load_gate", {}).get("status")
        != supersession["historical_load_status"]
    ):
        raise ValueError("historical T12a preprojection semantics drifted")
    return {
        "path": path.relative_to(ROOT).as_posix(),
        "sha256": sha256(path),
        "status": document["status"],
        "load_status": document["load_gate"]["status"],
        "builder": supersession["historical_builder"],
        "check": "frozen status/load-status verification only",
        "immutable": True,
        "current": False,
    }


def current_dependencies(
    policy: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    rows: dict[str, dict[str, Any]] = {}
    documents: dict[str, dict[str, Any]] = {}
    for owner, dependency in policy["dependencies"].items():
        path = ROOT / dependency["path"]
        document = load(path)
        if document.get("status") != dependency["required_status"]:
            raise ValueError(
                f"{owner} readiness must be {dependency['required_status']}"
            )
        rows[dependency["path"]] = {
            "owner": owner,
            "sha256": sha256(path),
            "status": document["status"],
        }
        documents[owner] = document

    t12_history = documents["T12"].get("t12a_historical_evidence") or {}
    if (
        t12_history.get("path") != "tools/t12a_machine_readiness.json"
        or t12_history.get("current") is not False
        or t12_history.get("immutable") is not True
    ):
        raise ValueError("T12 closure does not identify T12a as immutable history")
    t14_currentness = documents["T14"].get("currentness") or {}
    if (
        "tools/t12_closure_readiness.json" not in t14_currentness
        or "tools/t12a_machine_readiness.json" in t14_currentness
    ):
        raise ValueError("T14 currentness does not use the current T12 closure")
    return rows


def source_contracts(policy: dict[str, Any]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for owner, contract in policy["source_contracts"].items():
        stage = contract.get("stage")
        if stage not in STAGE_ORDER:
            raise ValueError(
                f"T15 source contract {owner} has invalid stage {stage!r}"
            )
        if (
            policy["stages"][stage]["status"]
            != policy["status_policy"]["complete_stage_status"]
        ):
            raise ValueError(
                f"T15 source contract {owner} belongs to pending {stage}"
            )
        path = ROOT / contract["path"]
        source = path.read_text(encoding="utf-8")
        required = contract.get("required_tokens") or []
        forbidden = contract.get("forbidden_tokens") or []
        missing = [token for token in required if token not in source]
        present = [token for token in forbidden if token in source]
        if missing or present:
            raise ValueError(
                f"T15 source contract {owner} failed: "
                f"missing={missing}, forbidden_present={present}"
            )
        result[owner] = {
            "stage": stage,
            "path": contract["path"],
            "sha256": sha256(path),
            "required_tokens": required,
            "forbidden_tokens": forbidden,
        }
    return result


def completed_stage_sources(
    completed: list[str],
    contracts: dict[str, Any],
) -> dict[str, dict[str, str]]:
    result: dict[str, dict[str, str]] = {}
    for stage in completed:
        owned = {
            owner: contract["sha256"]
            for owner, contract in contracts.items()
            if contract["stage"] == stage
        }
        if not owned:
            raise ValueError(f"{stage}: complete stage lacks source contracts")
        result[stage] = owned
    return result


def t15c_evidence(policy: dict[str, Any]) -> dict[str, Any] | None:
    if (
        policy["stages"]["T15c"]["status"]
        != policy["status_policy"]["complete_stage_status"]
    ):
        return None
    errors = acquisition_builder.check()
    if errors:
        raise ValueError("T15c acquisition evidence is stale: " + "; ".join(errors))
    acquisition = load(ACQUISITION)
    counts = acquisition.get("counts") or {}
    if (
        acquisition.get("status") != "T15C_MACHINE_ACQUISITION_READY"
        or counts.get("casing_recipes") != 6
        or counts.get("machine_variant_recipes") != 9
        or counts.get("registered_results") != 15
        or counts.get("unreachable") != 0
        or acquisition.get("unreachable") != []
    ):
        raise ValueError("T15c acquisition counts or status drifted")

    load_input = load(LOAD_INPUT)
    projected = recipe_load_projection.project(load_input)
    committed_projection = load(LOAD_OUTPUT)
    if projected != committed_projection:
        raise ValueError("T15 load projection is stale")
    load_counts = projected.get("ledger", {}).get("counts") or {}
    intervals = projected.get("ledger", {}).get("measurement_intervals") or {}
    if (
        projected.get("status") != "PASS"
        or projected.get("delivery_phase") != "T15"
        or any(value != 0 for value in load_counts.values())
        or any(
            interval != {"min": 0, "max": 0}
            for interval in intervals.values()
        )
    ):
        raise ValueError("T15c load projection must remain a zero-workload PASS")
    return {
        "status": "PASS",
        "acquisition": {
            "path": ACQUISITION.relative_to(ROOT).as_posix(),
            "sha256": sha256(ACQUISITION),
            "counts": {
                "casing_recipes": counts["casing_recipes"],
                "machine_variant_recipes": counts[
                    "machine_variant_recipes"
                ],
                "registered_results": counts["registered_results"],
                "unreachable": counts["unreachable"],
            },
        },
        "load_projection": {
            "input": LOAD_INPUT.relative_to(ROOT).as_posix(),
            "input_sha256": sha256(LOAD_INPUT),
            "output": LOAD_OUTPUT.relative_to(ROOT).as_posix(),
            "output_sha256": sha256(LOAD_OUTPUT),
            "status": projected["status"],
            "delivery_phase": projected["delivery_phase"],
            "publication_delta": load_counts[
                "eager_publication_rows"
            ],
            "incremental_counts": load_counts,
            "measurement_intervals": intervals,
        },
    }


def t15d_evidence(
    policy: dict[str, Any],
    contracts: dict[str, Any],
) -> dict[str, Any] | None:
    if (
        policy["stages"]["T15d"]["status"]
        != policy["status_policy"]["complete_stage_status"]
    ):
        return None
    evidence = policy.get("t15d_evidence") or {}
    junit = evidence.get("junit") or {}
    junit_classes = junit.get("classes") or []
    junit_count = sum(
        len(row.get("tests") or []) for row in junit_classes
    )
    if junit_count != 6 or junit.get("test_count") != junit_count:
        raise ValueError("T15d must record exactly six dedicated JUnit tests")
    if any(
        not (ROOT / row.get("path", "")).is_file()
        or not row.get("tests")
        for row in junit_classes
    ):
        raise ValueError("T15d JUnit evidence has a missing source or test list")

    gametest = evidence.get("gametest") or {}
    gametest_path = ROOT / gametest.get("path", "")
    gametest_names = gametest.get("tests") or []
    if (
        not gametest_path.is_file()
        or gametest.get("added_test_count") != 3
        or len(gametest_names) != 3
    ):
        raise ValueError("T15d must record exactly three added GameTests")
    current_suite_count = gametest_path.read_text(
        encoding="utf-8"
    ).count("@GameTest(")
    if (
        gametest.get("full_suite_test_count") != 56
        or current_suite_count < 56
    ):
        raise ValueError(
            "T15d historical GameTest suite count must remain 56"
        )

    declared_contracts = set(evidence.get("source_contracts") or [])
    stage_contracts = {
        owner
        for owner, contract in contracts.items()
        if contract["stage"] == "T15d"
    }
    if declared_contracts != stage_contracts:
        raise ValueError(
            "T15d evidence source contracts drifted: "
            f"declared={sorted(declared_contracts)}, "
            f"actual={sorted(stage_contracts)}"
        )
    return {
        "status": "PASS",
        "junit": junit,
        "gametest": gametest,
        "source_contract_sha256": {
            owner: contracts[owner]["sha256"]
            for owner in sorted(stage_contracts)
        },
    }


def t15e_evidence(
    policy: dict[str, Any],
    contracts: dict[str, Any],
) -> dict[str, Any] | None:
    if (
        policy["stages"]["T15e"]["status"]
        != policy["status_policy"]["complete_stage_status"]
    ):
        return None
    errors = boundary_builder.check()
    if errors:
        raise ValueError("T15e matcher boundary is stale: " + "; ".join(errors))
    boundary = load(MATCHER_BOUNDARY)
    physical = boundary.get("physical_structure") or {}
    host = boundary.get("host_layout") or {}
    relationship = boundary.get("port_host_boundary") or {}
    matcher = boundary.get("matcher_boundary") or {}
    benchmark = boundary.get("benchmark") or {}
    audit = boundary.get("historical_correction_audit") or {}
    if (
        boundary.get("status") != "T15E_MATCHER_BOUNDARY_READY"
        or physical.get("scan_volume") != 18
        or physical.get("item_fluid_ports") != 15
        or physical.get("energy_input_ports") != 2
        or physical.get("controllers") != 1
        or host.get("item_inputs") != 1
        or host.get("item_outputs") != 6
        or host.get("fluid_inputs") != 1
        or host.get("fluid_outputs") != 2
        or relationship.get("shared_processing_hosts") != 1
        or relationship.get("item_matcher_supplies") != 1
        or relationship.get("fluid_matcher_supplies") != 1
        or relationship.get("physical_ports_expand_matcher_supplies")
        is not False
        or matcher.get("presence_item_supply_cap") != 12
        or matcher.get("presence_cap_triggered") is not False
        or matcher.get("matcher_rewritten") is not False
        or benchmark.get("dense_supply_counts") != [12, 16, 32, 64]
        or benchmark.get("presence_cap_rejection_supply_counts")
        != [16, 32, 64]
        or benchmark.get("all_within_budget") is not True
        or audit.get("historical_expected_item_fluid_ports") != 16
        or audit.get("historical_status")
        != "SUPERSEDED_INCORRECT_EXPECTATION"
    ):
        raise ValueError("T15e port/matcher boundary evidence drifted")

    evidence = policy.get("t15e_evidence") or {}
    junit = evidence.get("junit") or {}
    junit_classes = junit.get("classes") or []
    junit_count = sum(
        len(row.get("tests") or []) for row in junit_classes
    )
    gametest = evidence.get("gametest") or {}
    if (
        junit.get("test_count") != 2
        or junit_count != 2
        or any(
            not (ROOT / row.get("path", "")).is_file()
            or not row.get("tests")
            for row in junit_classes
        )
        or gametest.get("strengthened_test_count") != 1
        or len(gametest.get("tests") or []) != 1
        or not (ROOT / gametest.get("path", "")).is_file()
    ):
        raise ValueError("T15e Java/GameTest evidence is incomplete")
    current_suite_count = (
        ROOT / gametest["path"]
    ).read_text(encoding="utf-8").count("@GameTest(")
    if (
        gametest.get("full_suite_test_count") != 56
        or current_suite_count < 56
    ):
        raise ValueError("T15e historical GameTest suite count drifted")

    declared_contracts = set(evidence.get("source_contracts") or [])
    stage_contracts = {
        owner
        for owner, contract in contracts.items()
        if contract["stage"] == "T15e"
    }
    if declared_contracts != stage_contracts:
        raise ValueError(
            "T15e evidence source contracts drifted: "
            f"declared={sorted(declared_contracts)}, "
            f"actual={sorted(stage_contracts)}"
        )
    return {
        "status": "PASS",
        "artifact": {
            "path": MATCHER_BOUNDARY.relative_to(ROOT).as_posix(),
            "sha256": sha256(MATCHER_BOUNDARY),
            "status": boundary["status"],
        },
        "physical_structure": physical,
        "host_layout": host,
        "port_host_boundary": relationship,
        "matcher_boundary": matcher,
        "benchmark": benchmark,
        "historical_correction_audit": audit,
        "junit": junit,
        "gametest": gametest,
        "source_contract_sha256": {
            owner: contracts[owner]["sha256"]
            for owner in sorted(stage_contracts)
        },
    }


def build() -> dict[str, Any]:
    policy = load(POLICY)
    status, completed, pending = stage_summary(policy)
    history = historical_evidence(policy)
    dependencies = current_dependencies(policy)
    contracts = source_contracts(policy)
    stage_sources = completed_stage_sources(completed, contracts)
    acquisition = t15c_evidence(policy)
    identity = t15d_evidence(policy, contracts)
    boundary = t15e_evidence(policy, contracts)
    supersession = {
        **policy["supersession"],
        "historical_artifact_sha256": history["sha256"],
        "replacement_condition": (
            (
                "Complete "
                + ", ".join(pending)
                + " and regenerate this artifact as T15_READY."
            )
            if pending
            else "All T15 stages are complete; this artifact is T15_READY."
        ),
    }
    return {
        "schema_version": 1,
        "status": status,
        "delivery_boundary": policy["delivery_boundary"],
        "completed_stages": completed,
        "pending_stages": pending,
        "stage_gates": policy["stages"],
        "historical_immutable_evidence": history,
        "supersession": supersession,
        "currentness": {
            "current_dependencies": dependencies,
            "completed_stage_sources": stage_sources,
            "owned_inputs": {
                POLICY.relative_to(ROOT).as_posix(): sha256(POLICY),
                BUILDER.relative_to(ROOT).as_posix(): sha256(BUILDER),
            },
            "pending_report": {
                **policy["refresh_policy"],
                "pending": [],
            },
        },
        "source_contracts": contracts,
        "t15c_acquisition_gate": acquisition,
        "t15d_identity_gate": identity,
        "t15e_matcher_boundary_gate": boundary,
    }


def check() -> list[str]:
    encoded = stable(build())
    if not OUTPUT.is_file():
        return [f"missing generated file: {OUTPUT.relative_to(ROOT).as_posix()}"]
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        return [f"stale generated file: {OUTPUT.relative_to(ROOT).as_posix()}"]
    return []


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed current T15 readiness is stale",
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
        print(f"T15 readiness failed: {error}")
        return 1
    print(
        json.dumps(
            {
                "status": document["status"],
                "completed": document["completed_stages"],
                "pending": document["pending_stages"],
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
