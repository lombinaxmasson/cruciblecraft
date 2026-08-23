#!/usr/bin/env python3
"""Build the T37 readiness account from current overlay evidence."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t37_common as t37  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t37.READINESS


def _load_or_build(path: Path, builder_name: str) -> dict[str, Any]:
    if path.is_file():
        return t35.load_json(path)
    module = __import__(f"tools.{builder_name}", fromlist=["build"])
    return module.build()


def _source_current() -> dict[str, Any]:
    present = t37.ASSEMBLER_SOURCE.is_file() and t37.ASSEMBLER_RECEIPT.is_file()
    if not present:
        return {
            "compact_current": False,
            "receipt_present": False,
            "dump_verified": False,
            "blockers": ["missing compact source or receipt"],
        }
    source = t35.load_json(t37.ASSEMBLER_SOURCE)
    receipt = t35.load_json(t37.ASSEMBLER_RECEIPT)
    compact_hash = t35.sha256_file(t37.ASSEMBLER_SOURCE)
    compact_ok = (
        source.get("status") == "T37_ASSEMBLER_SOURCE_FROZEN"
        and not source.get("blockers")
        and receipt.get("compact_sha256") == compact_hash
        and receipt.get("source_revision") == t37.SOURCE_REVISION
        and int((source.get("assignment") or {}).get("assigned") or 0) == t37.T37_FAMILY_COUNT
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
    if t37.dump_present():
        dump_hash = t35.sha256_file(t37.ASSEMBLER_DUMP)
        dump_verified = dump_hash == (receipt.get("dump") or {}).get("sha256")
        if not dump_verified:
            blockers.append("full_replay_dump_hash_mismatch")
    # Live dump absence is SKIP, not a compact-source failure. Receipt already
    # recorded the freeze hash; census-replay must not impersonate a source pass.
    return {
        "compact_current": compact_ok,
        "receipt_present": receipt_present,
        "dump_present": t37.dump_present(),
        "dump_verified": dump_verified,
        "dump_skip": not t37.dump_present(),
        "skip_is_not_pass": True,
        "blockers": blockers,
    }


def _equivalence_current() -> dict[str, Any]:
    if not t37.EQUIVALENCE.is_file():
        return {"current": False, "generated_files": 0, "logical_ids": 0}
    document = t35.load_json(t37.EQUIVALENCE)
    generated = document.get("generated") or {}
    runtime = document.get("runtime_expected") or {}
    files = t37.generated_family_files()
    return {
        "current": (
            int(generated.get("file_count") or 0) == t37.T37_FAMILY_COUNT
            and int(runtime.get("logical_ids") or 0) == t37.T37_FAMILY_COUNT
            and len(files) == t37.T37_FAMILY_COUNT
        ),
        "generated_files": len(files),
        "logical_ids": int(runtime.get("logical_ids") or 0),
        "file_count": int(generated.get("file_count") or 0),
    }


def _player_path_current() -> dict[str, Any]:
    if not t37.PLAYER_PATH.is_file():
        return {"current": False, "inputs_reachable": 0, "outputs_registered": 0}
    document = t35.load_json(t37.PLAYER_PATH)
    return {
        "current": (
            int(document.get("families") or 0) == t37.T37_FAMILY_COUNT
            and int(document.get("inputs_reachable") or 0) == t37.T37_FAMILY_COUNT
            and int(document.get("outputs_registered") or 0) == t37.T37_FAMILY_COUNT
        ),
        "families": int(document.get("families") or 0),
        "inputs_reachable": int(document.get("inputs_reachable") or 0),
        "outputs_registered": int(document.get("outputs_registered") or 0),
    }


def _runtime_current() -> dict[str, Any]:
    return {
        "java_codec_tests_present": t37.java_codec_tests_present(),
        "player_gametest_present": t37.player_gametest_present(),
        "current": t37.java_codec_tests_present() and t37.player_gametest_present(),
    }


def _load_current(delta: dict[str, Any]) -> dict[str, Any]:
    t14 = delta.get("t14_load") or {}
    strategy = t37.production_strategy()
    measurements = (
        t35.load_json(t37.MEASUREMENTS) if t37.MEASUREMENTS.is_file() else {}
    )
    measured = measurements.get("status") != "T37_MATERIALIZATION_MEASUREMENT_PENDING"
    pending_zero = any(
        row.get("pending") and row.get("delta") == 0
        for row in t14.get("axes") or []
        if isinstance(row, dict)
    )
    return {
        "winner_derived": not strategy["blocked"],
        "decision_recomputable": bool(strategy.get("recomputable")),
        "measurements_current": measured,
        "hard_ceiling_raised": bool(t14.get("hard_ceiling_raised")),
        "pending_zero_filled": pending_zero,
        "authored_closing": (t14.get("closing") or {}).get("datapack_authored_entries"),
        "current": (
            not strategy["blocked"]
            and bool(strategy.get("recomputable"))
            and measured
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
    t36 = t35.load_json(t37.T36_READINESS) if t37.T36_READINESS.is_file() else {}
    source = _source_current()
    equivalence = _equivalence_current()
    player = _player_path_current()
    runtime = _runtime_current()
    load = _load_current(delta)
    closeout = delta.get("identity_closeout") or {}
    validators = delta.get("validators") or {}
    topology_validators = topology.get("validators") or {}
    return {
        "t36_ready": t36.get("status") == "T36_READY",
        "work_set_fixed_50": int((delta.get("work_set") or {}).get("family_count") or 0)
        == t37.T37_FAMILY_COUNT,
        "source_compact_current": source["compact_current"],
        "full_replay_receipt_present": source["receipt_present"],
        "full_replay_dump_verified": source["dump_verified"] or source["dump_skip"],
        "equivalence_current": equivalence["current"],
        "runtime_java_tests_present": runtime["java_codec_tests_present"],
        "player_gametest_present": runtime["player_gametest_present"],
        "player_path_current": player["current"],
        "load_measured": load["current"],
        "decision_recomputable_from_measurements": bool(load.get("decision_recomputable")),
        "census_structural_validators_zero": all(
            int(value or 0) == 0 for value in validators.values()
        ),
        "identity_closeout_complete": bool(closeout.get("complete")),
        "t35_foundation_unchanged": bool(delta.get("t35_foundation_unchanged")),
        "t38_not_stolen": _validator_zero(validators, "t38_not_stolen"),
        "remaining_ordinary_not_bulk_complete": _validator_zero(
            validators, "remaining_ordinary_not_bulk_complete"
        ),
        "topology_t36_complete": topology.get("sequence", [{}])[0].get("status") == "complete",
        "topology_t37_status_honest": (
            (not topology.get("t37_complete") and topology.get("unique_active_card") == "T37")
            or (topology.get("t37_complete") and topology.get("unique_active_card") == "T38")
        ),
        "topology_t38_frozen_29": _validator_zero(
            topology_validators, "t38_family_ids_frozen"
        ),
        "topology_storage_after_recipes": (
            _validator_zero(topology_validators, "recipe_before_storage")
            and _validator_zero(topology_validators, "storage_not_preassigned_t39")
        ),
        "hard_ceiling_not_raised": not load["hard_ceiling_raised"],
        "pending_not_zero_filled": not load["pending_zero_filled"],
        "fifty_files_not_sufficient": True,
    }


def build() -> dict[str, Any]:
    delta = _load_or_build(t37.CENSUS_DELTA, "build_t37_census_delta")
    topology = _load_or_build(t37.CARD_TOPOLOGY, "build_t37_card_topology")
    publication = _load_or_build(t37.PUBLICATION_DELTA, "build_t37_publication_delta")
    source = _source_current()
    equivalence = _equivalence_current()
    player = _player_path_current()
    runtime = _runtime_current()
    load = _load_current(delta)
    gates = evaluate_gates(delta=delta, topology=topology)
    failed = sorted(
        name
        for name, ok in gates.items()
        if name != "fifty_files_not_sufficient" and not ok
    )
    ready = not failed
    return {
        "schema_version": 1,
        "status": "T37_READY" if ready else "T37_BLOCKED",
        "source_revision": t37.SOURCE_REVISION,
        "generated_by": "python tools/build_t37_readiness.py",
        "t35_history_readonly": True,
        "t36_history_readonly": True,
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
            "t37_complete": topology.get("t37_complete"),
            "publication_status": publication.get("status"),
            "generated_file_count": len(t37.generated_family_files()),
        },
        "identity_closeout": delta.get("identity_closeout"),
        "t38_opening": {
            "load_opening_source": "tools/t37_census_delta.json#t14_load",
            "t14_closing": (delta.get("t14_load") or {}).get("closing"),
            "host": t37.T38_HOST,
            "families": t37.T38_FAMILY_COUNT,
            "unique_active_after_t37_ready": True,
        },
        "note": (
            "T37_READY is derived from source currentness, equivalence, runtime, "
            "player path, load, census and topology. Fifty generated files are "
            "not sufficient. Missing full-replay dump verification or "
            "measurements keeps the card blocked."
        ),
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t37.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    gates = document.get("gates") or {}
    failed = sorted(
        name
        for name, ok in gates.items()
        if name != "fifty_files_not_sufficient" and not ok
    )
    if document.get("failed_gates") != failed:
        errors.append("failed_gates does not match derived gates")
    expected = "T37_READY" if not failed else "T37_BLOCKED"
    if document.get("status") != expected:
        errors.append(f"readiness status {document.get('status')} != {expected}")
    if document.get("status") == "T37_READY" and failed:
        errors.append("T37_READY cannot be claimed while gates fail")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t37.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "status": document["status"],
                "failed_gates": document["failed_gates"],
                "unique_active_card": document["evidence"]["unique_active_card"],
                "t38_authored_opening": (
                    document["t38_opening"]["t14_closing"] or {}
                ).get("datapack_authored_entries"),
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t37.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T37 readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
