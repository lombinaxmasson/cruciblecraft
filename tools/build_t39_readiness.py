#!/usr/bin/env python3
"""Build the T39 readiness account from current overlay evidence."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t38_common as t38  # noqa: E402
from tools import t39_common as t39  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t39.READINESS

JAVA_TESTS = (
    ROOT / "src/test/java/com/masson/cruciblecraft/recipe/gt/CompactT39CentrifugeHarnessTest.java",
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
    present = t39.SOURCE.is_file() and t39.RECEIPT.is_file()
    if not present:
        return {
            "compact_current": False,
            "receipt_present": False,
            "dump_verified": False,
            "blockers": ["missing compact source or receipt"],
        }
    source = t35.load_json(t39.SOURCE)
    receipt = t35.load_json(t39.RECEIPT)
    compact_hash = t35.sha256_file(t39.SOURCE)
    compact_ok = (
        source.get("status") == "T39_CENTRIFUGE_SOURCE_FROZEN"
        and not source.get("blockers")
        and receipt.get("compact_sha256") == compact_hash
        and receipt.get("source_revision") == t39.SOURCE_REVISION
        and int((source.get("work_set") or {}).get("family_count") or 0) == t39.FAMILY_COUNT
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
    if t39.dump_present():
        dump_hash = t35.sha256_file(t39.CENTRIFUGE_DUMP)
        dump_verified = dump_hash == (receipt.get("dump") or {}).get("sha256")
        if not dump_verified:
            blockers.append("full_replay_dump_hash_mismatch")
    return {
        "compact_current": compact_ok,
        "receipt_present": receipt_present,
        "dump_present": t39.dump_present(),
        "dump_verified": dump_verified,
        "dump_skip": not t39.dump_present(),
        "skip_is_not_pass": True,
        "blockers": blockers,
    }


def _equivalence_current() -> dict[str, Any]:
    if not t39.EQUIVALENCE.is_file():
        return {"current": False, "generated_files": 0, "logical_ids": 0}
    document = t35.load_json(t39.EQUIVALENCE)
    generated = document.get("generated") or {}
    runtime = document.get("runtime_expected") or {}
    files = t39.generated_family_files()
    return {
        "current": (
            int(generated.get("file_count") or 0) == t39.production_family_count()
            and int(runtime.get("logical_ids") or 0)
            == t39.production_relation_count()
            and len(files) == t39.production_family_count()
        ),
        "generated_files": len(files),
        "logical_ids": int(runtime.get("logical_ids") or 0),
        "file_count": int(generated.get("file_count") or 0),
    }


def _player_path_current() -> dict[str, Any]:
    if not t39.PLAYER_PATH.is_file() or not t39.LAYERED_PLAYER_PATH.is_file():
        return {
            "current": False,
            "inputs_reachable": 0,
            "outputs_registered": 0,
            "relations": 0,
            "production_lock_sha256": None,
        }
    document = t35.load_json(t39.PLAYER_PATH)
    layered = t35.load_json(t39.LAYERED_PLAYER_PATH)
    validators = layered.get("validators") or {}
    relations = layered.get("production_relations") or []
    reachable = sum(1 for row in relations if row.get("inputs_reachable_in_b1"))
    registered = sum(1 for row in relations if row.get("outputs_registered"))
    return {
        "current": (
            layered.get("status") == "T39_LAYERED_PLAYER_PATH_READY"
            and layered.get("production_lock_sha256") == t39.production_lock_sha256()
            and all(bool(value) for value in validators.values())
            and int(document.get("families") or 0)
            == t39.production_family_count()
            and len(relations) == t39.production_relation_count()
            and reachable == t39.production_relation_count()
            and registered == t39.production_relation_count()
        ),
        "families": int(document.get("families") or 0),
        "relations": len(relations),
        "b0_inputs_reachable": int(document.get("inputs_reachable") or 0),
        "inputs_reachable": reachable,
        "outputs_registered": registered,
        "layered_status": layered.get("status"),
        "production_lock_sha256": layered.get("production_lock_sha256"),
    }


def _runtime_current() -> dict[str, Any]:
    return {
        "java_tests_present": java_tests_present(),
        "player_gametest_present": t39.player_gametest_present(),
        "current": java_tests_present() and t39.player_gametest_present(),
    }


def _load_current(delta: dict[str, Any]) -> dict[str, Any]:
    t14 = delta.get("t14_load") or {}
    strategy = t39.production_strategy()
    measurements = (
        t35.load_json(t39.MEASUREMENTS) if t39.MEASUREMENTS.is_file() else {}
    )
    measured = measurements.get("status") == "T39_MATERIALIZATION_MEASUREMENT_READY"
    pending_zero = any(
        row.get("pending") and row.get("delta") == 0
        for row in t14.get("axes") or []
        if isinstance(row, dict)
        and row.get("axis") not in t39.STRATEGY_AXES
    )
    group_winners = strategy.get("group_winners") or {}
    return {
        "winner_derived": not strategy["blocked"],
        "group_winners_present": all(group_winners.values()),
        "decision_recomputable": bool(strategy.get("recomputable")),
        "measurements_current": measured,
        "hard_ceiling_raised": bool(t14.get("hard_ceiling_raised")),
        "pending_zero_filled": pending_zero,
        "authored_closing": (t14.get("closing") or {}).get("datapack_authored_entries"),
        "current": (
            not strategy["blocked"]
            and bool(strategy.get("recomputable"))
            and measured
            and all(group_winners.values())
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
    t38_ready_doc = t35.load_json(t38.READINESS) if t38.READINESS.is_file() else {}
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
    lock = t39.load_production_lock()
    candidate = (
        t35.load_json(t39.CANDIDATE_SELECTION)
        if t39.CANDIDATE_SELECTION.is_file()
        else {}
    )
    build_text = (ROOT / "build.gradle").read_text(encoding="utf-8")
    return {
        "t38_ready": t38_ready_doc.get("status") == "T38_READY",
        "work_set_matches_production_lock": (
            int((delta.get("work_set") or {}).get("family_count") or 0)
            == t39.production_family_count()
        ),
        "production_lock_current": (
            delta.get("production_lock_sha256") == t39.production_lock_sha256()
            and lock.get("status") == "T39_PRODUCTION_LOCKED"
        ),
        "candidate_is_non_authoritative": (
            candidate.get("status") == "T39_PRODUCTION_CANDIDATE"
            and "candidate" in candidate
            and "signed" not in candidate
        ),
        "phase_owner_audited": (
            len(lock.get("phase_deferred") or []) == 7
            and all(
                row.get("future_owner") == "post_1x:nuclear"
                for row in lock.get("phase_deferred") or []
            )
        ),
        "catalog_fixture_is_test_only": (
            len(t39.generated_family_files(scope="catalog"))
            == t39.CATALOG_FAMILY_COUNT
            and "src/t39_catalog_fixture_generated" not in build_text
        ),
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
        "group_winners_derived": bool(load.get("group_winners_present")),
        "census_structural_validators_zero": all(
            int(value or 0) == 0 for value in validators.values()
        ),
        "identity_closeout_complete": bool(closeout.get("complete")),
        "t35_foundation_unchanged": bool(delta.get("t35_foundation_unchanged")),
        "t38_not_stolen": _validator_zero(validators, "t38_not_stolen"),
        "remaining_ordinary_not_bulk_complete": _validator_zero(
            validators, "remaining_ordinary_not_bulk_complete"
        ),
        "remaining_gap_honest": remaining == t39.EXPECTED_REMAINING_ORDINARY_FAMILIES,
        "topology_t36_complete": topology.get("sequence", [{}])[0].get("status") == "complete",
        "topology_t37_complete": topology.get("sequence", [{}, {}])[1].get("status") == "complete",
        "topology_t38_complete": topology.get("sequence", [{}, {}, {}])[2].get("status") == "complete",
        "topology_t39_complete": bool(topology.get("t39_complete")),
        "topology_t40_not_preassigned": _validator_zero(
            topology_validators, "t40_not_preassigned"
        ),
        "topology_unique_active_honest": (
            (topology.get("t39_complete") and topology.get("unique_active_card") is None)
            or (
                not topology.get("t39_complete")
                and topology.get("unique_active_card") == "T39"
            )
        ),
        "topology_storage_after_recipes": (
            _validator_zero(topology_validators, "recipe_before_storage")
            and _validator_zero(topology_validators, "storage_not_preassigned")
        ),
        "hard_ceiling_not_raised": not load["hard_ceiling_raised"],
        "pending_not_zero_filled": not load["pending_zero_filled"],
        "catalog_fixture_not_production_evidence": True,
    }


def build() -> dict[str, Any]:
    delta = _load_or_build(t39.CENSUS_DELTA, "build_t39_census_delta")
    topology = _load_or_build(t39.T39_CARD_TOPOLOGY, "build_t39_card_topology")
    publication = _load_or_build(t39.PUBLICATION_DELTA, "build_t39_publication_delta")
    source = _source_current()
    equivalence = _equivalence_current()
    player = _player_path_current()
    runtime = _runtime_current()
    load = _load_current(delta)
    gates = evaluate_gates(delta=delta, topology=topology)
    failed = sorted(
        name
        for name, ok in gates.items()
        if name != "catalog_fixture_not_production_evidence" and not ok
    )
    ready = not failed
    return {
        "schema_version": 1,
        "status": "T39_READY" if ready else "T39_BLOCKED",
        "source_revision": t39.SOURCE_REVISION,
        "production_lock_sha256": t39.production_lock_sha256(),
        "generated_by": "python tools/build_t39_readiness.py",
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "t38_history_readonly": True,
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
            "t39_complete": topology.get("t39_complete"),
            "remaining_recipe_gap": topology.get("remaining_recipe_gap"),
            "publication_status": publication.get("status"),
            "generated_file_count": len(t39.generated_family_files()),
            "group_winners": publication.get("group_winners"),
        },
        "identity_closeout": delta.get("identity_closeout"),
        "t40_opening": {
            "next_issue_id": topology.get("next_issue_id"),
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "unique_active_after_t39_ready": topology.get("unique_active_card") is None,
            "t14_closing": (delta.get("t14_load") or {}).get("closing"),
        },
        "note": (
            "T39_READY is derived from source currentness, equivalence, runtime, "
            "player path, load, census and topology. The 157/250 catalog fixture "
            "is not production evidence. Missing lock-current source replay, "
            "layered player path, GameTest receipt, or measured decision keeps "
            "the card blocked."
        ),
    }


def write() -> dict[str, Any]:
    document = build()
    if document["status"] != "T39_READY":
        raise ValueError(
            "T39 readiness --write is fail-closed until locked player path, "
            "GameTest PASS receipt, and materialization decision exist"
        )
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t39.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    gates = document.get("gates") or {}
    failed = sorted(
        name
        for name, ok in gates.items()
        if name != "catalog_fixture_not_production_evidence" and not ok
    )
    if document.get("failed_gates") != failed:
        errors.append("failed_gates does not match derived gates")
    expected = "T39_READY" if not failed else "T39_BLOCKED"
    if document.get("status") != expected:
        errors.append(f"readiness status {document.get('status')} != {expected}")
    if document.get("status") == "T39_READY" and failed:
        errors.append("T39_READY cannot be claimed while gates fail")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t39.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "status": document["status"],
                "failed_gates": document["failed_gates"],
                "unique_active_card": document["evidence"]["unique_active_card"],
                "next_issue_id": document["evidence"]["next_issue_id"],
                "remaining_recipe_gap": document["evidence"]["remaining_recipe_gap"],
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t39.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T39 readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
