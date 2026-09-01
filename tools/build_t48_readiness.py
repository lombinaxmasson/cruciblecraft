#!/usr/bin/env python3
"""Build the T48 readiness account from current overlay evidence."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t48_common as t48  # noqa: E402

OUTPUT = t48.READINESS
T42_REPAIR_READINESS = ROOT / "tools" / "t42_repair_readiness.json"
PROVIDER_JAVA = (
    ROOT / "src/main/java/com/masson/cruciblecraft/recipe/gt/"
    "CompactRecipeFamilyProvider.java"
)
LOADER_JAVA = (
    ROOT / "src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipeMapLoader.java"
)

JAVA_TESTS = (
    ROOT / "src/test/java/com/masson/cruciblecraft/recipe/gt/"
    "CompactT48BathIdentityHarnessTest.java",
    ROOT / "src/test/java/com/masson/cruciblecraft/recipe/gt/"
    "PublicationPolicyLoaderTest.java",
    ROOT / "src/test/java/com/masson/cruciblecraft/recipe/gt/"
    "CompactRecipeFamilyT48IntegratedMeasurementHarness.java",
)
DEFINITION_JAVA = (
    ROOT / "src/main/java/com/masson/cruciblecraft/recipe/gt/"
    "CompactGTRecipeFamilyDefinition.java"
)


def _load_or_build(path: Path, builder_name: str) -> dict[str, Any]:
    if path.is_file():
        return t35.load_json(path)
    module = __import__(f"tools.{builder_name}", fromlist=["build"])
    return module.build()


def java_tests_present() -> bool:
    return all(path.is_file() for path in JAVA_TESTS)


def _source_current() -> dict[str, Any]:
    present = t48.SOURCE.is_file() and t48.RECEIPT.is_file()
    if not present:
        return {
            "compact_current": False,
            "receipt_present": False,
            "dump_verified": False,
            "blockers": ["missing compact source or receipt"],
        }
    source = t35.load_json(t48.SOURCE)
    receipt = t35.load_json(t48.RECEIPT)
    compact_hash = t35.sha256_file(t48.SOURCE)
    compact_ok = (
        source.get("status") == "T48_BATH_SOURCE_FROZEN"
        and not source.get("blockers")
        and receipt.get("compact_sha256") == compact_hash
        and receipt.get("denominator_source_revision") == t48.SOURCE_REVISION
        and int((source.get("work_set") or {}).get("family_count") or 0)
        == t48.CANDIDATE_FAMILY_COUNT
        and int((source.get("assignment") or {}).get("assigned") or 0)
        == t48.CANDIDATE_RELATION_COUNT
        and receipt.get("source_oracle_dirty") is False
    )
    receipt_present = bool(receipt.get("dump_file_hashes")) and bool(
        receipt.get("full_replay")
    )
    dump_verified = False
    blockers = []
    if not compact_ok:
        blockers.append("compact_source_not_current")
    if not receipt_present:
        blockers.append("full_replay_receipt_missing")
    if t48.dump_present():
        dump_hashes = receipt.get("dump_file_hashes") or {}
        dump_verified = True
        relative = t48.relative(t48.BATH_DUMP)
        expected = dump_hashes.get(relative)
        live = t35.sha256_file(t48.BATH_DUMP)
        if expected != live:
            dump_verified = False
            blockers.append(f"full_replay_dump_hash_mismatch:{relative}")
    return {
        "compact_current": compact_ok,
        "receipt_present": receipt_present,
        "dump_present": t48.dump_present(),
        "dump_verified": dump_verified,
        "dump_skip": not t48.dump_present(),
        "skip_is_not_pass": True,
        "blockers": blockers,
    }


def _equivalence_current() -> dict[str, Any]:
    if not t48.EQUIVALENCE.is_file():
        return {"current": False, "generated_files": 0, "logical_ids": 0}
    document = t35.load_json(t48.EQUIVALENCE)
    generated = document.get("generated") or {}
    runtime = document.get("runtime_expected") or {}
    files = t48.generated_family_files()
    return {
        "current": (
            int(generated.get("file_count") or 0) == t48.production_family_count()
            and int(runtime.get("logical_ids") or 0)
            == t48.production_relation_count()
            and len(files) == t48.production_family_count()
            and t48.equivalence_fields_locked()
        ),
        "generated_files": len(files),
        "logical_ids": int(runtime.get("logical_ids") or 0),
        "file_count": int(generated.get("file_count") or 0),
    }


def _player_path_current() -> dict[str, Any]:
    if not t48.PLAYER_PATH.is_file() or not t48.LAYERED_PLAYER_PATH.is_file():
        return {
            "current": False,
            "inputs_reachable": 0,
            "outputs_registered": 0,
            "relations": 0,
            "production_lock_sha256": None,
        }
    document = t35.load_json(t48.PLAYER_PATH)
    layered = t35.load_json(t48.LAYERED_PLAYER_PATH)
    reachable = int(document.get("inputs_reachable") or 0)
    registered = int(document.get("outputs_registered") or 0)
    return {
        "current": (
            layered.get("status") == t48.LAYERED_PLAYER_PATH_STATUS
            and int((layered.get("b2") or {}).get("relation_count") or 0)
            == t48.production_relation_count()
            and int(document.get("families") or 0) == t48.production_family_count()
            and int(document.get("relations") or 0) == t48.production_relation_count()
            and reachable == t48.production_relation_count()
            and registered == t48.production_relation_count()
            and t48.player_path_real()
        ),
        "families": int(document.get("families") or 0),
        "relations": int(document.get("relations") or 0),
        "b0_inputs_reachable": reachable,
        "inputs_reachable": reachable,
        "outputs_registered": registered,
        "layered_status": layered.get("status"),
        "production_lock_sha256": t48.production_lock_sha256(),
    }


def _runtime_current() -> dict[str, Any]:
    return {
        "java_tests_present": java_tests_present(),
        "java_locked_fields_asserted": t48.java_locked_fields_asserted(),
        "player_gametest_present": t48.player_gametest_present(),
        "current": (
            java_tests_present()
            and t48.java_locked_fields_asserted()
            and t48.player_gametest_present()
        ),
    }


def _load_current(delta: dict[str, Any]) -> dict[str, Any]:
    t14 = delta.get("t14_load") or {}
    strategy = t48.production_strategy()
    measurements = (
        t35.load_json(t48.MEASUREMENTS) if t48.MEASUREMENTS.is_file() else {}
    )
    measured = measurements.get("status") == "T48_MATERIALIZATION_MEASUREMENT_READY"
    pending_zero = any(
        row.get("pending") and row.get("delta") == 0
        for row in t14.get("axes") or []
        if isinstance(row, dict)
        and row.get("axis") not in t48.STRATEGY_AXES
    )
    group_winners = strategy.get("group_winners") or {}
    return {
        "winner_derived": not strategy["blocked"],
        "group_winners_present": bool(
            group_winners.get("exact")
            and group_winners.get("exact_multi")
            and group_winners.get("tool_head")
        ),
        "decision_recomputable": bool(strategy.get("recomputable")),
        "measurements_current": measured,
        "hard_ceiling_raised": bool(t14.get("hard_ceiling_raised")),
        "pending_zero_filled": pending_zero,
        "authored_closing": (t14.get("closing") or {}).get("datapack_authored_entries"),
        "current": (
            not strategy["blocked"]
            and bool(strategy.get("recomputable"))
            and measured
            and bool(
                group_winners.get("exact")
                and group_winners.get("exact_multi")
                and group_winners.get("tool_head")
            )
            and not t14.get("hard_ceiling_raised")
            and not pending_zero
            and t48.integrated_load_measured()
        ),
    }


def _validator_zero(values: dict[str, Any], key: str) -> bool:
    if key not in values:
        return False
    return int(values[key]) == 0


def _publication_policies_present() -> bool:
    return all(path.is_file() for path in t48.PUBLICATION_POLICY_DATAPACK_FILES.values())


def _no_t48_java_whitelist() -> bool:
    provider = PROVIDER_JAVA.read_text(encoding="utf-8")
    definition = DEFINITION_JAVA.read_text(encoding="utf-8")
    start = definition.find("historicalPublicationGroup")
    end = definition.find("resolvedPublicationGroup")
    historical = definition[start:end] if start >= 0 and end > start else definition
    return (
        "t48ProductionPolicy" not in provider
        and "t48_bath_exact" not in historical
        and "t48_bath_exact_multi" not in provider
    )


def _centrifuge_whitelist_unchanged() -> bool:
    text = LOADER_JAVA.read_text(encoding="utf-8")
    return (
        "Undeclared compact publication_group" in text
        and "t48_centrifuge" not in text
    )


def evaluate_gates(
    *,
    delta: dict[str, Any],
    topology: dict[str, Any],
) -> dict[str, bool]:
    t41_ready_doc = t35.load_json(t48.T41_READINESS) if t48.T41_READINESS.is_file() else {}
    t42_repair = (
        t35.load_json(T42_REPAIR_READINESS) if T42_REPAIR_READINESS.is_file() else {}
    )
    t42_owner = (
        t35.load_json(t48.T42_OWNER_READINESS)
        if t48.T42_OWNER_READINESS.is_file()
        else {}
    )
    t43_ready = t35.load_json(t48.T43_READINESS) if t48.T43_READINESS.is_file() else {}
    t44_ready = t35.load_json(t48.T44_READINESS) if t48.T44_READINESS.is_file() else {}
    t45_ready = t35.load_json(t48.T45_READINESS) if t48.T45_READINESS.is_file() else {}
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
    lock = t48.load_production_lock()
    candidate = (
        t35.load_json(t48.CANDIDATE_SELECTION)
        if t48.CANDIDATE_SELECTION.is_file()
        else {}
    )
    required_forms = (
        t35.load_json(t48.REQUIRED_FORMS) if t48.REQUIRED_FORMS.is_file() else {}
    )
    runtime_manifest = (
        t35.load_json(t48.RUNTIME_DEPENDENCY_MANIFEST)
        if t48.RUNTIME_DEPENDENCY_MANIFEST.is_file()
        else {}
    )
    catalog = t35.load_json(t48.IDENTITY_CATALOG) if t48.IDENTITY_CATALOG.is_file() else {}
    projection = (
        t35.load_json(t48.LOAD_PROJECTION) if t48.LOAD_PROJECTION.is_file() else {}
    )
    decision = t35.load_json(t48.DECISION) if t48.DECISION.is_file() else {}
    build_text = (ROOT / "build.gradle").read_text(encoding="utf-8")
    v2_hash = t48.v2_policy_hash() if t48.LOAD_POLICY_V2.is_file() else ""
    return {
        "t41_ready": t41_ready_doc.get("status") == "T41_READY",
        "t42_repair_ready": t42_repair.get("status") == "T42_REPAIR_READY",
        "t42_owner_ready": t42_owner.get("status") == "T42_OWNER_READY",
        "t43_ready": t43_ready.get("status") == "T43_READY",
        "t44_ready": t44_ready.get("status") == "T44_STORAGE_READY",
        "t45_ready": t45_ready.get("status") == "T45_READY",
        "t46_ready": t35.load_json(t48.T46_READINESS).get("status") == "T46_READY"
        if t48.T46_READINESS.is_file()
        else False,
        "t47_ready": t35.load_json(t48.T47_READINESS).get("status") == "T47_READY"
        if t48.T47_READINESS.is_file()
        else False,
        "t47_vr_ready": t35.load_json(t48.T47_VR_REPAIR_READINESS).get("status")
        == "T47_VR_READY"
        if t48.T47_VR_REPAIR_READINESS.is_file()
        else False,
        "work_set_matches_production_lock": (
            int((delta.get("work_set") or {}).get("family_count") or 0)
            == t48.production_family_count()
        ),
        "production_lock_current": (
            delta.get("production_lock_sha256") == t48.production_lock_sha256()
            and lock.get("status") == "T48_PRODUCTION_LOCKED"
            and int((lock.get("production") or {}).get("family_count") or 0)
            == t48.COMPLETION_DELTA
        ),
        "candidate_is_non_authoritative": (
            candidate.get("status") == "T48_CANDIDATE_SELECTION"
            and int((candidate.get("coverage") or {}).get("production") or 0)
            == t48.production_family_count()
            and int((candidate.get("coverage") or {}).get("family_count") or 0)
            == t48.CANDIDATE_FAMILY_COUNT
            and (candidate.get("coverage") or {}).get("reclassified") == 0
            and candidate.get("production_authority") is not True
        ),
        "compiler_fixture_is_test_only": (
            "src/t48_compiler_fixture" not in build_text.replace(
                "src/test/resources/t48_compiler_fixture", ""
            )
        ),
        "source_compact_current": source["compact_current"],
        "full_replay_receipt_present": source["receipt_present"],
        "full_replay_dump_verified": (
            bool(source["dump_verified"]) and not bool(source["dump_skip"])
        ),
        "equivalence_current": equivalence["current"],
        "equivalence_fields_locked": t48.equivalence_fields_locked(),
        "java_locked_fields_asserted": t48.java_locked_fields_asserted(),
        "runtime_java_tests_present": runtime["java_tests_present"],
        "player_gametest_present": runtime["player_gametest_present"],
        "player_path_current": player["current"],
        "player_path_real": t48.player_path_real(),
        "locked_support_tree_current": t48.locked_support_tree_current(),
        "load_measured": load["current"],
        "integrated_load_measured": t48.integrated_load_measured(),
        "decision_recomputable_from_measurements": bool(load.get("decision_recomputable")),
        "group_winners_derived": bool(load.get("group_winners_present")),
        "publication_policy_manifests_present": _publication_policies_present(),
        "required_forms_frozen": (
            required_forms.get("status") == "T48_REQUIRED_FORMS_FROZEN"
            and int((required_forms.get("counts") or {}).get("required_form_pairs") or 0)
            > 0
        ),
        "census_structural_validators_zero": all(
            int(value or 0) == 0 for value in validators.values()
        ),
        "identity_closeout_complete": bool(closeout.get("complete")),
        "t35_foundation_unchanged": bool(delta.get("t35_foundation_unchanged")),
        "remaining_ordinary_not_bulk_complete": _validator_zero(
            validators, "remaining_ordinary_not_bulk_complete"
        ),
        "remaining_gap_honest": remaining == t48.EXPECTED_REMAINING_ORDINARY_FAMILIES,
        "topology_t48_complete": bool(topology.get("t48_complete")),
        "topology_t48_not_preassigned": _validator_zero(
            topology_validators, "t48_not_preassigned"
        ),
        "topology_unique_active_honest": (
            (topology.get("t48_complete") and topology.get("unique_active_card") is None)
            or (
                not topology.get("t48_complete")
                and topology.get("unique_active_card") == "T48"
            )
        ),
        "topology_next_issue_id_t49": topology.get("next_issue_id") == "T49",
        "hard_ceiling_not_raised": not load["hard_ceiling_raised"],
        "pending_not_zero_filled": not load["pending_zero_filled"],
        "no_t48_java_historical_whitelist": _no_t48_java_whitelist(),
        "centrifuge_premerge_whitelist_unchanged": _centrifuge_whitelist_unchanged(),
        "runtime_manifest_centrifuge_not_relaxed": (
            runtime_manifest.get("centrifuge_premerge_whitelist_relaxed") is False
            and runtime_manifest.get("historical_java_t48_whitelist") is False
        ),
        "complete_family_count_is_n": (
            int(delta.get("complete_family_count") or 0) == t48.COMPLETION_DELTA
            and int(delta.get("partial_family_count") or 0) == 0
            and int(delta.get("reclassification_delta") or 0) == 0
        ),
        "production_denominator_145_34091_3532": (
            int((lock.get("production") or {}).get("family_count") or 0)
            == t48.EXPECTED_FAMILY_COUNT
            and int((lock.get("production") or {}).get("relation_count") or 0)
            == t48.EXPECTED_RELATION_COUNT
            and int(catalog.get("identity_count") or 0) == t48.EXPECTED_IDENTITY_COUNT
        ),
        "v2_load_policy_pinned": (
            ((projection.get("currentness") or {}).get("budget_policy_file_sha256") == v2_hash)
            and ((projection.get("currentness") or {}).get("load_budget_policy_v2_sha256") == v2_hash)
            and ((decision.get("currentness") or {}).get("load_budget_policy_v2_sha256") == v2_hash)
            and ((delta.get("currentness") or {}).get("load_budget_policy_v2_sha256") == v2_hash)
            and (
                (
                    t35.load_json(t48.INTEGRATED_MEASUREMENTS)
                    if t48.INTEGRATED_MEASUREMENTS.is_file()
                    else {}
                ).get("load_budget_policy_v2_sha256")
                == v2_hash
            )
        ),
        "deferred_recycling_unchanged": (
            int(
                (delta.get("remaining_ordinary") or {}).get("deferred_recycling_count") or 0
            )
            == t48.DEFERRED_RECYCLING_COUNT
        ),
    }


def build() -> dict[str, Any]:
    delta = _load_or_build(t48.CENSUS_DELTA, "build_t48_census_delta")
    topology = _load_or_build(t48.CARD_TOPOLOGY, "build_t48_card_topology")
    publication = _load_or_build(t48.PUBLICATION_DELTA, "build_t48_publication_delta")
    source = _source_current()
    equivalence = _equivalence_current()
    player = _player_path_current()
    runtime = _runtime_current()
    load = _load_current(delta)
    gates = evaluate_gates(delta=delta, topology=topology)
    failed = sorted(name for name, ok in gates.items() if not ok)
    ready = not failed
    return {
        "schema_version": 1,
        "status": "T48_READY" if ready else "T48_BLOCKED",
        "source_revision": t48.SOURCE_REVISION,
        "production_lock_sha256": t48.production_lock_sha256(),
        "generated_by": "python tools/build_t48_readiness.py",
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "t38_history_readonly": True,
        "t39_history_readonly": True,
        "t40_history_readonly": True,
        "t41_history_readonly": True,
        "t42_history_readonly": True,
        "t43_history_readonly": True,
        "t44_history_readonly": True,
        "t45_history_readonly": True,
        "t46_history_readonly": True,
        "t47_history_readonly": True,
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
            "t48_complete": topology.get("t48_complete"),
            "remaining_recipe_gap": topology.get("remaining_recipe_gap"),
            "publication_status": publication.get("status"),
            "generated_file_count": len(t48.generated_family_files()),
            "group_winners": publication.get("group_winners"),
            "complete_family_count": delta.get("complete_family_count"),
            "partial_family_count": delta.get("partial_family_count"),
            "opening_execution_gap": delta.get("opening_execution_gap"),
            "completion_delta": delta.get("completion_delta"),
            "reclassification_delta": delta.get("reclassification_delta"),
        },
        "identity_closeout": delta.get("identity_closeout"),
        "v2_load_policy_sha256": t48.v2_policy_hash() if t48.LOAD_POLICY_V2.is_file() else "",
        "t48_opening": {
            "next_issue_id": topology.get("next_issue_id"),
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "unique_active_after_t48_ready": topology.get("unique_active_card") is None,
            "t14_closing": (delta.get("t14_load") or {}).get("closing"),
            "t14_closing_basis": t48.T14_CLOSING_BASIS,
        },
        "note": (
            "T48_READY is derived from source currentness, equivalence, runtime, "
            "player path, load, census and topology. N=145 complete families / "
            "34091 relations / 3532 identities. Deferred recycling stays 1817. "
            "Remaining recipe gap is 1354. T49 is not issued from this card "
            "and is not preassigned."
        ),
    }


def write() -> dict[str, Any]:
    document = build()
    if document["status"] != "T48_READY":
        raise ValueError(
            "T48 readiness --write is fail-closed until locked player path, "
            "GameTest PASS receipt, and materialization decision exist: "
            + ", ".join(document.get("failed_gates") or [])
        )
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T48",
        "readiness",
        lambda: t48.check_document(OUTPUT, build()),
    )
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    gates = document.get("gates") or {}
    failed = sorted(name for name, ok in gates.items() if not ok)
    if document.get("failed_gates") != failed:
        errors.append("failed_gates does not match derived gates")
    expected = "T48_READY" if not failed else "T48_BLOCKED"
    if document.get("status") != expected:
        errors.append(f"readiness status {document.get('status')} != {expected}")
    if document.get("status") == "T48_READY" and failed:
        errors.append("T48_READY cannot be claimed while gates fail")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t48.parse_write_check(__doc__, argv)
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
        print(f"{t48.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T48 readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
