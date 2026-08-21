#!/usr/bin/env python3
"""Build the staged T27 portfolio-freeze readiness artifact.

The ``status`` field is derived by ``build()`` from the evidence gates
below; it is never hand-written. ``check()`` recomputes and compares
against the committed file. Report-owned fields are excluded from
``check()`` via REPORT_OWNED; T27_READY is bound by full verification
acceptance, not by editing this file.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import build_t27_card_topology as topology  # noqa: E402
from tools import build_t27_opening_snapshot as opening  # noqa: E402
from tools import build_t27_portfolio as portfolio  # noqa: E402
from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
OUTPUT = TOOLS / "t27_readiness.json"
POLICY = TOOLS / "t27_readiness_policy.json"
BUILDER = Path(__file__).resolve()
CONTRACT = TOOLS / "phase5_portfolio_contract.json"
T14_POLICY = TOOLS / "t14_load_budget_policy.json"
REPORT = TOOLS / "full_verification_report.json"

STAGES = ("T27a", "T27b", "T27c", "T27d", "T27e")
REPORT_OWNED = (
    "currentness",
    "runtime",
    "status",
    "completed_stages",
    "pending_stages",
)
FROZEN_LOGICAL = 19_087
FROZEN_EAGER = 16_862
FROZEN_LAZY = 2_225
FROZEN_GAMETEST_TOTAL = 121
ALLOWED_GAMETEST_TOTALS = (121, 131, 137)
FROZEN_JAVA_TESTS = 584
ALLOWED_JAVA_TESTS = (584, 593, 644, 652)
FROZEN_DATAPACK_ENTRIES = 3243
EAGER_HARD_CEILING = 21_000


def _load_runtime() -> dict[str, Any]:
    report = common.load_json(REPORT) if REPORT.is_file() else {}
    game_tests = (report.get("tests") or {}).get("production_game_tests") or {}
    passing = None
    if game_tests.get("result") == "PASS":
        passing = True
    elif game_tests.get("result"):
        passing = False
    return {
        "datapack_recipe_entries": (report.get("rules") or {}).get(
            "datapack_recipe_entries"
        ),
        "gametest_passing": passing,
        "gametest_total": game_tests.get("required_tests"),
        "java_unit_tests": (report.get("tests") or {}).get("java_unit_tests") or {},
        "t26_status": (report.get("t26_readiness_acceptance") or {}).get("status"),
        "report_status": report.get("status"),
    }


def _table_docs() -> dict[str, dict[str, Any]]:
    return {
        table: common.load_json(portfolio.output_path(table))
        for table in common.TABLE_SPECS
    }


def _load_closure(
    opening_doc: dict[str, Any],
    aggregate: dict[str, Any],
    open_items: dict[str, Any],
    tracks: dict[str, Any],
    topology_doc: dict[str, Any],
) -> dict[str, Any]:
    table_docs = _table_docs()
    canonical = sum(
        int((document.get("counts") or {}).get("canonical") or 0)
        for document in table_docs.values()
    )
    unclassified = sum(
        int((document.get("counts") or {}).get("unclassified") or 0)
        for document in table_docs.values()
    )
    work = {
        (item["table"], item["canonical_id"])
        for item in aggregate.get("v1_work_set") or []
    }
    covered = {
        (item["table"], item["canonical_id"])
        for card in topology_doc.get("cards") or []
        for item in card.get("identities") or []
    }
    cards = topology_doc.get("cards") or []
    consecutive = [card.get("id") for card in cards] == [
        f"T{28 + index}" for index in range(len(cards))
    ]
    return {
        "aggregate_canonical": (aggregate.get("counts") or {}).get("canonical"),
        "aggregate_unclassified": (aggregate.get("counts") or {}).get("unclassified"),
        "aggregate_validators_clear": all(
            int(value or 0) == 0
            for value in (aggregate.get("validators") or {}).values()
        ),
        "canonical_total": canonical,
        "open_item_orphan": (open_items.get("counts") or {}).get("orphan"),
        "opening_bound_to_t26_ready": (
            (opening_doc.get("t26_gate") or {}).get("report_status") == "READY"
            and (opening_doc.get("t26_gate") or {}).get("t26_status") == "T26_READY"
        ),
        "t28_plus_covers_work_set": work == covered and consecutive,
        "t28_plus_not_started": (
            topology_doc.get("started") is False
            and all(card.get("started") is False for card in cards)
        ),
        "tracks_not_started": tracks.get("started") is False,
        "unclassified": unclassified,
        "work_set_size": len(work),
    }


def _load_fidelity(
    contract: dict[str, Any],
    table_docs: dict[str, dict[str, Any]],
    open_items: dict[str, Any],
    topology_doc: dict[str, Any],
) -> dict[str, Any]:
    frozen = contract.get("frozen_open_items") or {}
    by_open = {row.get("id"): row for row in open_items.get("records") or []}
    multiblocks = {
        row["canonical_id"]: row
        for row in (table_docs["multiblock_kinds"].get("records") or [])
    }
    maps = {
        row["canonical_id"]: row
        for row in (table_docs["recipe_maps"].get("records") or [])
    }
    mixer = maps.get("gt.recipe.mixer") or {}
    crucible = multiblocks.get("crucible") or {}
    return {
        "anvil_bend_post_1_0": (
            (frozen.get("anvil_bend_big") or {}).get("disposition") == "post_1_0"
            and (frozen.get("anvil_bend_small") or {}).get("disposition") == "post_1_0"
            and (by_open.get("anvil_bend_big") or {}).get("disposition") == "post_1_0"
            and (by_open.get("anvil_bend_small") or {}).get("disposition") == "post_1_0"
        ),
        "crucible_collision_forbidden": (
            (frozen.get("crucible") or {}).get("collision_forbidden")
            == "cruciblecraft:crucible"
            and (frozen.get("crucible") or {}).get("reserved_controller_id")
            == "cruciblecraft:large_crucible"
            and crucible.get("cc_implementation")
            == "cruciblecraft:large_crucible"
        ),
        "f003_f005_rc_contract": (
            (by_open.get("T24-F003") or {}).get("owner") == "T27 RC"
            and (by_open.get("T24-F005") or {}).get("owner") == "T27 RC"
            and (by_open.get("T24-F003") or {}).get("recheck_point")
            == "T27 RC candidate"
        ),
        "map_level_not_row_level": "not row-level" in (
            ((mixer.get("axes") or {}).get("fidelity") or {}).get("evidence") or ""
        ),
        "o36_v1_required": (
            (frozen.get("O-36") or {}).get("disposition") == "v1_required"
            and (frozen.get("O-36") or {}).get("owner")
            == "portfolio:v1/hot_ingot_cooling"
            and (by_open.get("O-36") or {}).get("disposition") == "v1_required"
        ),
        "o41_post_1_0": (
            (frozen.get("O-41") or {}).get("disposition") == "post_1_0"
            and (by_open.get("O-41") or {}).get("disposition") == "post_1_0"
        ),
        "rc_number_absent": topology_doc.get("rc_number") is None,
        "source_revision_pinned": (
            contract.get("source_revision") == common.SOURCE_REVISION
        ),
        "t13_classification_kept": all(
            row.get("t13_classification") in common.T13_CLASSIFICATIONS
            and row.get("disposition") in common.DISPOSITIONS
            for document in table_docs.values()
            for row in document.get("records") or []
        ),
        "t28_plus_card_count_unfilled": (
            (contract.get("execution_policy") or {}).get("t28_plus_card_count") is None
            and (contract.get("t28_plus_generation") or {}).get("card_count") is None
        ),
    }


def _pending_filled_with_zero(value: Any) -> bool:
    if isinstance(value, dict):
        load = value.get("load")
        if isinstance(load, dict) and load.get("status") == "pending":
            for key in common.FAKE_ZERO_LOAD_KEYS:
                if load.get(key) == 0:
                    return True
        return any(_pending_filled_with_zero(item) for item in value.values())
    if isinstance(value, list):
        return any(_pending_filled_with_zero(item) for item in value)
    return False


def _load_load(
    opening_doc: dict[str, Any],
    aggregate: dict[str, Any],
    tracks: dict[str, Any],
    topology_doc: dict[str, Any],
    t14: dict[str, Any],
) -> dict[str, Any]:
    current = (opening_doc.get("publication") or {}).get("current") or {}
    eager = ((t14.get("budgets") or {}).get("eager_publication_rows") or {})
    return {
        "eager_hard_ceiling_is_21000": eager.get("hard_ceiling") == EAGER_HARD_CEILING,
        "opening_from_t26_ready_report": (
            (opening_doc.get("t26_gate") or {}).get("report")
            == "tools/full_verification_report.json"
        ),
        "opening_publication": current,
        "pending_not_filled_with_zero": not any(
            _pending_filled_with_zero(document)
            for document in (aggregate, tracks, topology_doc)
        ),
        "publication_delta": opening_doc.get("publication_delta")
        or {"eager": 0, "lazy": 0, "logical": 0, "scope": "T27_CARD"},
        "t14_policy_current": t14.get("status") == "T14D_LOAD_BUDGET_POLICY_MEASURED",
        "tracks_delta_zero": tracks.get("publication_delta")
        == {"eager": 0, "lazy": 0, "logical": 0},
        "topology_delta_zero": topology_doc.get("publication_delta")
        == {"eager": 0, "lazy": 0, "logical": 0},
    }


def build() -> dict[str, Any]:
    topology_errors = topology.check()
    opening_errors = opening.check()
    policy = common.load_json(POLICY)
    contract = common.load_json(CONTRACT)
    opening_doc = common.load_json(opening.OUTPUT)
    aggregate = common.load_json(portfolio.AGGREGATE)
    open_items = common.load_json(portfolio.open_items_path())
    tracks = common.load_json(portfolio.tracks_path())
    topology_doc = common.load_json(topology.OUTPUT)
    t14 = common.load_json(T14_POLICY)
    table_docs = _table_docs()
    closure = _load_closure(
        opening_doc, aggregate, open_items, tracks, topology_doc
    )
    fidelity = _load_fidelity(contract, table_docs, open_items, topology_doc)
    load_data = _load_load(opening_doc, aggregate, tracks, topology_doc, t14)
    runtime = _load_runtime()
    closure_policy = policy.get("closure_policy") or {}
    zero_content = (
        runtime["gametest_total"] in ALLOWED_GAMETEST_TOTALS
        and runtime["java_unit_tests"].get("tests") in ALLOWED_JAVA_TESTS
        and runtime["datapack_recipe_entries"] == FROZEN_DATAPACK_ENTRIES
    )
    current_pub = load_data["opening_publication"]
    gates_ok = (
        not topology_errors
        and not opening_errors
        and len(closure_policy.get("pending") or []) == 0
        and closure_policy.get("final_closure_attempted") is True
        and closure["opening_bound_to_t26_ready"]
        and closure["canonical_total"] == 765
        and closure["unclassified"] == 0
        and closure["aggregate_canonical"] == 765
        and closure["aggregate_unclassified"] == 0
        and closure["aggregate_validators_clear"]
        and closure["open_item_orphan"] == 0
        and closure["tracks_not_started"]
        and closure["t28_plus_covers_work_set"]
        and closure["t28_plus_not_started"]
        and fidelity["source_revision_pinned"]
        and fidelity["t13_classification_kept"]
        and fidelity["crucible_collision_forbidden"]
        and fidelity["o36_v1_required"]
        and fidelity["o41_post_1_0"]
        and fidelity["anvil_bend_post_1_0"]
        and fidelity["f003_f005_rc_contract"]
        and fidelity["map_level_not_row_level"]
        and fidelity["t28_plus_card_count_unfilled"]
        and fidelity["rc_number_absent"]
        and load_data["t14_policy_current"]
        and load_data["eager_hard_ceiling_is_21000"]
        and load_data["opening_from_t26_ready_report"]
        and current_pub.get("logical") == FROZEN_LOGICAL
        and current_pub.get("eager") == FROZEN_EAGER
        and current_pub.get("lazy") == FROZEN_LAZY
        and load_data["publication_delta"]
        == {"eager": 0, "lazy": 0, "logical": 0, "scope": "T27_CARD"}
        and load_data["tracks_delta_zero"]
        and load_data["topology_delta_zero"]
        and load_data["pending_not_filled_with_zero"]
        and zero_content
        and runtime["gametest_passing"] is True
        and runtime["t26_status"] == "T26_READY"
        and runtime["report_status"] == "READY"
    )
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(POLICY): common.sha256_file(POLICY),
        common.relative(CONTRACT): common.sha256_file(CONTRACT),
        common.relative(opening.OUTPUT): common.sha256_file(opening.OUTPUT),
        common.relative(portfolio.AGGREGATE): common.sha256_file(portfolio.AGGREGATE),
        common.relative(topology.OUTPUT): common.sha256_file(topology.OUTPUT),
        common.relative(T14_POLICY): common.sha256_file(T14_POLICY),
        common.relative(portfolio.open_items_path()): common.sha256_file(
            portfolio.open_items_path()
        ),
        common.relative(portfolio.tracks_path()): common.sha256_file(
            portfolio.tracks_path()
        ),
        **{
            common.relative(portfolio.output_path(table)): common.sha256_file(
                portfolio.output_path(table)
            )
            for table in common.TABLE_SPECS
        },
    }
    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "run_full_verification",
        "policy": (
            "The status field is derived from evidence by build(). "
            "Any mismatch between the committed value and a fresh build "
            "is detected by check(). T27_READY cannot be hand-written."
        ),
        "completed_stages": [],
        "pending_stages": [],
        "closure": closure,
        "fidelity": fidelity,
        "load": load_data,
        "owned_inputs": owned_inputs,
        "runtime": {
            "gametest_passing": runtime["gametest_passing"],
            "gametest_total": runtime["gametest_total"],
        },
        "closure_policy": closure_policy,
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(POLICY): common.sha256_file(POLICY),
            },
            "full_verification": {
                "evidence": "tools/full_verification_report.json",
                "status": "BOUND_TO_FULL_VERIFICATION_REPORT",
                "pending": [],
            },
        },
    }
    if gates_ok:
        document["status"] = "T27_READY"
        document["completed_stages"] = list(STAGES)
        document["pending_stages"] = []
    return document


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    on_disk = common.load_json(OUTPUT)
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status_owner") != "run_full_verification":
        errors.append("status_owner must be run_full_verification")
    expected = build()
    if errors:
        return errors
    disk_stripped = {key: value for key, value in on_disk.items() if key not in REPORT_OWNED}
    expected_stripped = {
        key: value for key, value in expected.items() if key not in REPORT_OWNED
    }
    if common.stable_json(disk_stripped) != common.stable_json(expected_stripped):
        errors.append(f"stale generated file: {common.relative(OUTPUT)}")
    return errors


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T27 readiness failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "schema_version": document.get("schema_version"),
        "status_owner": document.get("status_owner"),
        "work_set_size": (document.get("closure") or {}).get("work_set_size"),
    }
    if "status" in document:
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
