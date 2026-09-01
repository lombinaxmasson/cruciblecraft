#!/usr/bin/env python3
"""Build the T38 readiness account from current overlay evidence."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t37_common as t37  # noqa: E402
from tools import t38_common as t38  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t38.READINESS

JAVA_TESTS = (
    ROOT / "src/test/java/com/masson/cruciblecraft/recipe/gt/CompactT38RoasterHarnessTest.java",
    ROOT / "src/test/java/com/masson/cruciblecraft/recipe/gt/CompactGTRecipeFamilyGeneratedTest.java",
)


def _load_or_build(path: Path, builder_name: str) -> dict[str, Any]:
    if path.is_file():
        return t35.load_json(path)
    module = __import__(f"tools.{builder_name}", fromlist=["build"])
    return module.build()


def java_tests_present() -> bool:
    return all(path.is_file() for path in JAVA_TESTS)


def _source_current() -> dict[str, Any]:
    present = t38.SOURCE.is_file() and t38.RECEIPT.is_file()
    if not present:
        return {
            "compact_current": False,
            "receipt_present": False,
            "dump_verified": False,
            "blockers": ["missing compact source or receipt"],
        }
    source = t35.load_json(t38.SOURCE)
    receipt = t35.load_json(t38.RECEIPT)
    compact_hash = t35.sha256_file(t38.SOURCE)
    compact_ok = (
        source.get("status") == "T38_ROASTER_SOURCE_FROZEN"
        and not source.get("blockers")
        and receipt.get("compact_sha256") == compact_hash
        and receipt.get("source_revision") == t38.SOURCE_REVISION
        and int((source.get("work_set") or {}).get("family_count") or 0) == t38.FAMILY_COUNT
    )
    receipt_present = bool((receipt.get("dump") or {}).get("sha256")) and bool(
        receipt.get("full_replay")
    )
    dump_verified = False
    blockers = []
    if not compact_ok:
        blockers.append("compact_source_not_current")
    if not receipt_present:
        blockers.append("full_replay_receipt_missing")
    if t38.dump_present():
        dump_hash = t35.sha256_file(t38.ROASTER_DUMP)
        dump_verified = dump_hash == (receipt.get("dump") or {}).get("sha256")
        if not dump_verified:
            blockers.append("full_replay_dump_hash_mismatch")
    return {
        "compact_current": compact_ok,
        "receipt_present": receipt_present,
        "dump_present": t38.dump_present(),
        "dump_verified": dump_verified,
        "dump_skip": not t38.dump_present(),
        "skip_is_not_pass": True,
        "blockers": blockers,
    }


def _equivalence_current() -> dict[str, Any]:
    if not t38.EQUIVALENCE.is_file():
        return {"current": False, "generated_files": 0, "logical_ids": 0}
    document = t35.load_json(t38.EQUIVALENCE)
    generated = document.get("generated") or {}
    runtime = document.get("runtime_expected") or {}
    files = t38.generated_family_files()
    return {
        "current": (
            int(generated.get("file_count") or 0) == t38.FAMILY_COUNT
            and int(runtime.get("logical_ids") or 0) == t38.SOURCE_ROWS
            and len(files) == t38.FAMILY_COUNT
        ),
        "generated_files": len(files),
        "logical_ids": int(runtime.get("logical_ids") or 0),
        "file_count": int(generated.get("file_count") or 0),
    }


def _player_path_current() -> dict[str, Any]:
    if not t38.PLAYER_PATH.is_file():
        return {"current": False, "inputs_reachable": 0, "outputs_registered": 0}
    document = t35.load_json(t38.PLAYER_PATH)
    return {
        "current": (
            int(document.get("families") or 0) == t38.FAMILY_COUNT
            and int(document.get("inputs_reachable") or 0) == t38.SOURCE_ROWS
            and int(document.get("outputs_registered") or 0) == t38.SOURCE_ROWS
        ),
        "families": int(document.get("families") or 0),
        "inputs_reachable": int(document.get("inputs_reachable") or 0),
        "outputs_registered": int(document.get("outputs_registered") or 0),
    }


def _runtime_current() -> dict[str, Any]:
    return {
        "java_tests_present": java_tests_present(),
        "player_gametest_present": t38.player_gametest_present(),
        "current": java_tests_present() and t38.player_gametest_present(),
    }


def _load_current(delta: dict[str, Any]) -> dict[str, Any]:
    t14 = delta.get("t14_load") or {}
    strategy = t38.production_strategy()
    measurements = (
        t35.load_json(t38.MEASUREMENTS) if t38.MEASUREMENTS.is_file() else {}
    )
    measured = measurements.get("status") != "T38_MATERIALIZATION_MEASUREMENT_PENDING"
    pending_zero = any(
        row.get("pending") and row.get("delta") == 0
        for row in t14.get("axes") or []
        if isinstance(row, dict)
        and row.get("axis") not in t38.STRATEGY_AXES
    )
    winner_on_demand = strategy.get("winner") == "on_demand"
    return {
        "winner_derived": not strategy["blocked"],
        "winner_on_demand": winner_on_demand,
        "decision_recomputable": bool(strategy.get("recomputable")),
        "measurements_current": measured,
        "hard_ceiling_raised": bool(t14.get("hard_ceiling_raised")),
        "pending_zero_filled": pending_zero,
        "authored_closing": (t14.get("closing") or {}).get("datapack_authored_entries"),
        "current": (
            not strategy["blocked"]
            and bool(strategy.get("recomputable"))
            and measured
            and winner_on_demand
            and not t14.get("hard_ceiling_raised")
            and not pending_zero
        ),
    }


def _validator_zero(values: dict[str, Any], key: str) -> bool:
    if key not in values:
        return False
    return int(values[key]) == 0


def evaluate_gates(
    *,
    delta: dict[str, Any],
    topology: dict[str, Any],
) -> dict[str, bool]:
    t37_ready_doc = t35.load_json(t37.READINESS) if t37.READINESS.is_file() else {}
    source = _source_current()
    equivalence = _equivalence_current()
    player = _player_path_current()
    runtime = _runtime_current()
    load = _load_current(delta)
    closeout = delta.get("identity_closeout") or {}
    validators = delta.get("validators") or {}
    topology_validators = topology.get("validators") or {}
    remaining = int(
        (delta.get("remaining_ordinary") or {}).get("remaining_ordinary_families") or 0
    )
    return {
        "t37_ready": t37_ready_doc.get("status") == "T37_READY",
        "work_set_fixed_29": int((delta.get("work_set") or {}).get("family_count") or 0)
        == t38.FAMILY_COUNT,
        "source_compact_current": source["compact_current"],
        "full_replay_receipt_present": source["receipt_present"],
        "full_replay_dump_verified": (
            bool(source["dump_verified"]) and not bool(source["dump_skip"])
        ),
        "equivalence_current": equivalence["current"],
        "runtime_java_tests_present": runtime["java_tests_present"],
        "player_gametest_present": runtime["player_gametest_present"],
        "player_path_current": player["current"],
        "load_measured": load["current"],
        "decision_recomputable_from_measurements": bool(load.get("decision_recomputable")),
        "winner_on_demand": bool(load.get("winner_on_demand")),
        "census_structural_validators_zero": all(
            int(value or 0) == 0 for value in validators.values()
        ),
        "identity_closeout_complete": bool(closeout.get("complete")),
        "t35_foundation_unchanged": bool(delta.get("t35_foundation_unchanged")),
        "t37_not_stolen": _validator_zero(validators, "t37_not_stolen"),
        "remaining_ordinary_not_bulk_complete": _validator_zero(
            validators, "remaining_ordinary_not_bulk_complete"
        ),
        "remaining_gap_5639": remaining == t38.EXPECTED_REMAINING_ORDINARY_FAMILIES,
        "topology_t36_complete": topology.get("sequence", [{}])[0].get("status") == "complete",
        "topology_t37_complete": topology.get("sequence", [{}, {}])[1].get("status") == "complete",
        "topology_t38_complete": bool(topology.get("t38_complete")),
        "topology_t39_not_preassigned": _validator_zero(
            topology_validators, "t39_not_preassigned"
        ),
        "topology_unique_active_honest": (
            (topology.get("t38_complete") and topology.get("unique_active_card") is None)
            or (
                not topology.get("t38_complete")
                and topology.get("unique_active_card") == "T38"
            )
        ),
        "topology_storage_after_recipes": (
            _validator_zero(topology_validators, "recipe_before_storage")
            and _validator_zero(topology_validators, "storage_not_preassigned")
        ),
        "hard_ceiling_not_raised": not load["hard_ceiling_raised"],
        "pending_not_zero_filled": not load["pending_zero_filled"],
        "twenty_nine_files_not_sufficient": True,
    }


def build() -> dict[str, Any]:
    delta = _load_or_build(t38.CENSUS_DELTA, "build_t38_census_delta")
    topology = _load_or_build(t38.T38_CARD_TOPOLOGY, "build_t38_card_topology")
    publication = _load_or_build(t38.PUBLICATION_DELTA, "build_t38_publication_delta")
    source = _source_current()
    equivalence = _equivalence_current()
    player = _player_path_current()
    runtime = _runtime_current()
    load = _load_current(delta)
    gates = evaluate_gates(delta=delta, topology=topology)
    failed = sorted(
        name
        for name, ok in gates.items()
        if name != "twenty_nine_files_not_sufficient" and not ok
    )
    ready = not failed
    return {
        "schema_version": 1,
        "status": "T38_READY" if ready else "T38_BLOCKED",
        "source_revision": t38.SOURCE_REVISION,
        "generated_by": "python tools/build_t38_readiness.py",
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "gates": gates,
        "failed_gates": failed,
        "evidence": {
            "source": source,
            "equivalence": equivalence,
            "runtime": runtime,
            "player_path": player,
            "load": load,
            "census_status": delta.get("status"),
            "topology_epoch": topology.get("epoch"),
            "unique_active_card": topology.get("unique_active_card"),
            "next_issue_id": topology.get("next_issue_id"),
            "t38_complete": topology.get("t38_complete"),
            "remaining_recipe_gap": topology.get("remaining_recipe_gap"),
            "publication_status": publication.get("status"),
            "generated_file_count": len(t38.generated_family_files()),
        },
        "identity_closeout": delta.get("identity_closeout"),
        "t39_opening": {
            "next_issue_id": topology.get("next_issue_id"),
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "unique_active_after_t38_ready": topology.get("unique_active_card") is None,
            "t14_closing": (delta.get("t14_load") or {}).get("closing"),
        },
        "note": (
            "T38_READY is derived from source currentness, equivalence, runtime, "
            "player path, load, census and topology. Twenty-nine generated files "
            "are not sufficient. Missing full-replay dump verification, player "
            "path, or measurements keeps the card blocked. A missing dump is "
            "not a pass."
        ),
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T38",
        "readiness",
        lambda: t38.check_document(OUTPUT, build()),
    )
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    gates = document.get("gates") or {}
    failed = sorted(
        name
        for name, ok in gates.items()
        if name != "twenty_nine_files_not_sufficient" and not ok
    )
    if document.get("failed_gates") != failed:
        errors.append("failed_gates does not match derived gates")
    expected = "T38_READY" if not failed else "T38_BLOCKED"
    if document.get("status") != expected:
        errors.append(f"readiness status {document.get('status')} != {expected}")
    if document.get("status") == "T38_READY" and failed:
        errors.append("T38_READY cannot be claimed while gates fail")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t38.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "status": document["status"],
                "failed_gates": document["failed_gates"],
                "unique_active_card": document["evidence"]["unique_active_card"],
                "next_issue_id": document["evidence"]["next_issue_id"],
                "remaining_recipe_gap": document["evidence"]["remaining_recipe_gap"],
                "t14_closing_authored": (
                    document["t39_opening"]["t14_closing"] or {}
                ).get("datapack_authored_entries"),
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t38.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T38 readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
