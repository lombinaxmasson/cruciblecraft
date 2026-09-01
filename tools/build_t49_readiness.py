#!/usr/bin/env python3
"""Build the T49 readiness account from current overlay evidence."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import wave_bath_tiny_purified as tiny_purified  # noqa: E402

OUTPUT = tiny_purified.READINESS
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
    "CompactT49BathRemainderHarnessTest.java",
    ROOT / "src/test/java/com/masson/cruciblecraft/recipe/gt/"
    "PublicationPolicyLoaderTest.java",
    ROOT / "src/test/java/com/masson/cruciblecraft/recipe/gt/"
    "CompactRecipeFamilyT49IntegratedMeasurementHarness.java",
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
    present = tiny_purified.SOURCE.is_file() and tiny_purified.RECEIPT.is_file()
    if not present:
        return {
            "compact_current": False,
            "receipt_present": False,
            "dump_verified": False,
            "blockers": ["missing compact source or receipt"],
        }
    source = t35.load_json(tiny_purified.SOURCE)
    receipt = t35.load_json(tiny_purified.RECEIPT)
    compact_hash = t35.sha256_file(tiny_purified.SOURCE)
    compact_ok = (
        source.get("status") == "T49_BATH_SOURCE_FROZEN"
        and not source.get("blockers")
        and receipt.get("compact_sha256") == compact_hash
        and receipt.get("denominator_source_revision") == tiny_purified.SOURCE_REVISION
        and int((source.get("work_set") or {}).get("family_count") or 0)
        == tiny_purified.CANDIDATE_FAMILY_COUNT
        and int((source.get("assignment") or {}).get("assigned") or 0)
        == tiny_purified.CANDIDATE_RELATION_COUNT
        and receipt.get("proof_tier") == "parent_slice"
        and receipt.get("skip_is_not_pass") is True
    )
    parent_ok = bool((receipt.get("parent") or {}).get("compact_sha256"))
    blockers = []
    if not compact_ok:
        blockers.append("compact_source_not_current")
    if not parent_ok:
        blockers.append("parent_slice_receipt_missing")
    return {
        "compact_current": compact_ok,
        "receipt_present": parent_ok,
        "dump_present": False,
        "dump_verified": parent_ok,
        "dump_skip": True,
        "skip_is_not_pass": True,
        "proof_tier": receipt.get("proof_tier"),
        "blockers": blockers,
    }


def _equivalence_current() -> dict[str, Any]:
    if not tiny_purified.EQUIVALENCE.is_file():
        return {"current": False, "generated_files": 0, "logical_ids": 0}
    document = t35.load_json(tiny_purified.EQUIVALENCE)
    generated = document.get("generated") or {}
    runtime = document.get("runtime_expected") or {}
    files = tiny_purified.generated_family_files()
    return {
        "current": (
            int(generated.get("file_count") or 0) == tiny_purified.production_family_count()
            and int(runtime.get("logical_ids") or 0)
            == tiny_purified.production_relation_count()
            and len(files) == tiny_purified.production_family_count()
            and tiny_purified.equivalence_fields_locked()
        ),
        "generated_files": len(files),
        "logical_ids": int(runtime.get("logical_ids") or 0),
        "file_count": int(generated.get("file_count") or 0),
    }


def _player_path_current() -> dict[str, Any]:
    if not tiny_purified.PLAYER_PATH.is_file() or not tiny_purified.LAYERED_PLAYER_PATH.is_file():
        return {
            "current": False,
            "inputs_reachable": 0,
            "outputs_registered": 0,
            "relations": 0,
            "production_lock_sha256": None,
        }
    document = t35.load_json(tiny_purified.PLAYER_PATH)
    layered = t35.load_json(tiny_purified.LAYERED_PLAYER_PATH)
    reachable = int(document.get("inputs_reachable") or 0)
    registered = int(document.get("outputs_registered") or 0)
    return {
        "current": (
            layered.get("status") == tiny_purified.LAYERED_PLAYER_PATH_STATUS
            and int((layered.get("b2") or {}).get("relation_count") or 0)
            == tiny_purified.production_relation_count()
            and int(document.get("families") or 0) == tiny_purified.production_family_count()
            and int(document.get("relations") or 0) == tiny_purified.production_relation_count()
            and reachable == tiny_purified.production_relation_count()
            and registered == tiny_purified.production_relation_count()
            and tiny_purified.player_path_real()
        ),
        "families": int(document.get("families") or 0),
        "relations": int(document.get("relations") or 0),
        "b0_inputs_reachable": reachable,
        "inputs_reachable": reachable,
        "outputs_registered": registered,
        "layered_status": layered.get("status"),
        "production_lock_sha256": tiny_purified.production_lock_sha256(),
    }


def _runtime_current() -> dict[str, Any]:
    return {
        "java_tests_present": java_tests_present(),
        "java_locked_fields_asserted": tiny_purified.java_locked_fields_asserted(),
        "player_gametest_present": tiny_purified.player_gametest_present(),
        "current": (
            java_tests_present()
            and tiny_purified.java_locked_fields_asserted()
            and tiny_purified.player_gametest_present()
        ),
    }


def _load_current(delta: dict[str, Any]) -> dict[str, Any]:
    t14 = delta.get("t14_load") or {}
    strategy = tiny_purified.production_strategy()
    measurements = (
        t35.load_json(tiny_purified.MEASUREMENTS) if tiny_purified.MEASUREMENTS.is_file() else {}
    )
    measured = measurements.get("status") == "T49_MATERIALIZATION_MEASUREMENT_READY"
    pending_zero = any(
        row.get("pending") and row.get("delta") == 0
        for row in t14.get("axes") or []
        if isinstance(row, dict)
        and row.get("axis") not in tiny_purified.STRATEGY_AXES
    )
    group_winners = strategy.get("group_winners") or {}
    return {
        "winner_derived": not strategy["blocked"],
        "group_winners_present": bool(group_winners.get("exact_multi")),
        "decision_recomputable": bool(strategy.get("recomputable")),
        "measurements_current": measured,
        "hard_ceiling_raised": bool(t14.get("hard_ceiling_raised")),
        "pending_zero_filled": pending_zero,
        "authored_closing": (t14.get("closing") or {}).get("datapack_authored_entries"),
        "current": (
            not strategy["blocked"]
            and bool(strategy.get("recomputable"))
            and measured
            and bool(group_winners.get("exact_multi"))
            and not t14.get("hard_ceiling_raised")
            and not pending_zero
            and tiny_purified.integrated_load_measured()
        ),
    }


def _validator_zero(values: dict[str, Any], key: str) -> bool:
    if key not in values:
        return False
    return int(values[key]) == 0


def _publication_policies_present() -> bool:
    return all(path.is_file() for path in tiny_purified.PUBLICATION_POLICY_DATAPACK_FILES.values())


def _no_t49_java_whitelist() -> bool:
    provider = PROVIDER_JAVA.read_text(encoding="utf-8")
    definition = DEFINITION_JAVA.read_text(encoding="utf-8")
    start = definition.find("historicalPublicationGroup")
    end = definition.find("resolvedPublicationGroup")
    historical = definition[start:end] if start >= 0 and end > start else definition
    return (
        "t49ProductionPolicy" not in provider
        and "t49_bath_exact" not in historical
        and "t49_bath_exact_multi" not in provider
    )


def _centrifuge_whitelist_unchanged() -> bool:
    text = LOADER_JAVA.read_text(encoding="utf-8")
    return (
        "Undeclared compact publication_group" in text
        and "t49_centrifuge" not in text
    )


def evaluate_gates(
    *,
    delta: dict[str, Any],
    topology: dict[str, Any],
) -> dict[str, bool]:
    t41_ready_doc = t35.load_json(tiny_purified.T41_READINESS) if tiny_purified.T41_READINESS.is_file() else {}
    t42_repair = (
        t35.load_json(T42_REPAIR_READINESS) if T42_REPAIR_READINESS.is_file() else {}
    )
    t42_owner = (
        t35.load_json(tiny_purified.T42_OWNER_READINESS)
        if tiny_purified.T42_OWNER_READINESS.is_file()
        else {}
    )
    t43_ready = t35.load_json(tiny_purified.T43_READINESS) if tiny_purified.T43_READINESS.is_file() else {}
    t44_ready = t35.load_json(tiny_purified.T44_READINESS) if tiny_purified.T44_READINESS.is_file() else {}
    t45_ready = t35.load_json(tiny_purified.T45_READINESS) if tiny_purified.T45_READINESS.is_file() else {}
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
    lock = tiny_purified.load_production_lock()
    candidate = (
        t35.load_json(tiny_purified.CANDIDATE_SELECTION)
        if tiny_purified.CANDIDATE_SELECTION.is_file()
        else {}
    )
    required_forms = (
        t35.load_json(tiny_purified.REQUIRED_FORMS) if tiny_purified.REQUIRED_FORMS.is_file() else {}
    )
    runtime_manifest = (
        t35.load_json(tiny_purified.RUNTIME_DEPENDENCY_MANIFEST)
        if tiny_purified.RUNTIME_DEPENDENCY_MANIFEST.is_file()
        else {}
    )
    catalog = t35.load_json(tiny_purified.IDENTITY_CATALOG) if tiny_purified.IDENTITY_CATALOG.is_file() else {}
    projection = (
        t35.load_json(tiny_purified.LOAD_PROJECTION) if tiny_purified.LOAD_PROJECTION.is_file() else {}
    )
    decision = t35.load_json(tiny_purified.DECISION) if tiny_purified.DECISION.is_file() else {}
    build_text = (ROOT / "build.gradle").read_text(encoding="utf-8")
    v2_hash = tiny_purified.v2_policy_hash() if tiny_purified.LOAD_POLICY_V2.is_file() else ""
    return {
        "t41_ready": t41_ready_doc.get("status") == "T41_READY",
        "t42_repair_ready": t42_repair.get("status") == "T42_REPAIR_READY",
        "t42_owner_ready": t42_owner.get("status") == "T42_OWNER_READY",
        "t43_ready": t43_ready.get("status") == "T43_READY",
        "t44_ready": t44_ready.get("status") == "T44_STORAGE_READY",
        "t45_ready": t45_ready.get("status") == "T45_READY",
        "t46_ready": t35.load_json(tiny_purified.T46_READINESS).get("status") == "T46_READY"
        if tiny_purified.T46_READINESS.is_file()
        else False,
        "t47_ready": t35.load_json(tiny_purified.T47_READINESS).get("status") == "T47_READY"
        if tiny_purified.T47_READINESS.is_file()
        else False,
        "t48_ready": t35.load_json(tiny_purified.T48_READINESS).get("status") == "T48_READY"
        if tiny_purified.T48_READINESS.is_file()
        else False,
        "t47_vr_ready": t35.load_json(tiny_purified.T47_VR_REPAIR_READINESS).get("status")
        == "T47_VR_READY"
        if tiny_purified.T47_VR_REPAIR_READINESS.is_file()
        else False,
        "work_set_matches_production_lock": (
            int((delta.get("work_set") or {}).get("family_count") or 0)
            == tiny_purified.production_family_count()
        ),
        "production_lock_current": (
            delta.get("production_lock_sha256") == tiny_purified.production_lock_sha256()
            and lock.get("status") == "T49_PRODUCTION_LOCKED"
            and int((lock.get("production") or {}).get("family_count") or 0)
            == tiny_purified.COMPLETION_DELTA
        ),
        "candidate_is_non_authoritative": (
            candidate.get("status") == "T49_CANDIDATE_SELECTION"
            and int((candidate.get("coverage") or {}).get("production") or 0)
            == tiny_purified.production_family_count()
            and int((candidate.get("coverage") or {}).get("family_count") or 0)
            == tiny_purified.CANDIDATE_FAMILY_COUNT
            and (candidate.get("coverage") or {}).get("reclassified") == 0
            and candidate.get("production_authority") is not True
        ),
        "compiler_fixture_is_test_only": (
            "src/t49_compiler_fixture" not in build_text.replace(
                "src/test/resources/t49_compiler_fixture", ""
            )
        ),
        "source_compact_current": source["compact_current"],
        "parent_slice_receipt_present": source["receipt_present"],
        "equivalence_current": equivalence["current"],
        "equivalence_fields_locked": tiny_purified.equivalence_fields_locked(),
        "java_locked_fields_asserted": tiny_purified.java_locked_fields_asserted(),
        "runtime_java_tests_present": runtime["java_tests_present"],
        "player_gametest_present": runtime["player_gametest_present"],
        "player_path_current": player["current"],
        "player_path_real": tiny_purified.player_path_real(),
        "locked_support_tree_current": tiny_purified.locked_support_tree_current(),
        "load_measured": load["current"],
        "integrated_load_measured": tiny_purified.integrated_load_measured(),
        "decision_recomputable_from_measurements": bool(load.get("decision_recomputable")),
        "group_winners_derived": bool(load.get("group_winners_present")),
        "publication_policy_manifests_present": _publication_policies_present(),
        "required_forms_frozen": (
            required_forms.get("status") == "T49_REQUIRED_FORMS_FROZEN"
            and int((required_forms.get("counts") or {}).get("required_form_pairs") or 0)
            == 0
        ),
        "census_structural_validators_zero": all(
            int(value or 0) == 0 for value in validators.values()
        ),
        "identity_closeout_complete": bool(closeout.get("complete")),
        "t35_foundation_unchanged": bool(delta.get("t35_foundation_unchanged")),
        "remaining_ordinary_not_bulk_complete": _validator_zero(
            validators, "remaining_ordinary_not_bulk_complete"
        ),
        "remaining_gap_honest": remaining == tiny_purified.EXPECTED_REMAINING_ORDINARY_FAMILIES,
        "topology_t49_complete": bool(topology.get("t49_complete")),
        "topology_t49_not_preassigned": _validator_zero(
            topology_validators, "t49_not_preassigned"
        ),
        "topology_unique_active_honest": (
            (topology.get("t49_complete") and topology.get("unique_active_card") is None)
            or (
                not topology.get("t49_complete")
                and topology.get("unique_active_card") == "T49"
            )
        ),
        "topology_next_issue_id_t50": topology.get("next_issue_id") == "T50",
        "hard_ceiling_not_raised": not load["hard_ceiling_raised"],
        "pending_not_zero_filled": not load["pending_zero_filled"],
        "no_t49_java_historical_whitelist": _no_t49_java_whitelist(),
        "centrifuge_premerge_whitelist_unchanged": _centrifuge_whitelist_unchanged(),
        "runtime_manifest_centrifuge_not_relaxed": (
            runtime_manifest.get("centrifuge_premerge_whitelist_relaxed") is False
            and runtime_manifest.get("historical_java_t49_whitelist") is False
        ),
        "complete_family_count_is_n": (
            int(delta.get("complete_family_count") or 0) == tiny_purified.COMPLETION_DELTA
            and int(delta.get("partial_family_count") or 0) == 0
            and int(delta.get("reclassification_delta") or 0) == 0
        ),
        "production_denominator_145_34091_3532": (
            int((lock.get("production") or {}).get("family_count") or 0)
            == tiny_purified.EXPECTED_FAMILY_COUNT
            and int((lock.get("production") or {}).get("relation_count") or 0)
            == tiny_purified.EXPECTED_RELATION_COUNT
            and int(catalog.get("identity_count") or 0) == tiny_purified.EXPECTED_IDENTITY_COUNT
        ),
        "v2_load_policy_pinned": (
            ((projection.get("currentness") or {}).get("budget_policy_file_sha256") == v2_hash)
            and ((projection.get("currentness") or {}).get("load_budget_policy_v2_sha256") == v2_hash)
            and ((decision.get("currentness") or {}).get("load_budget_policy_v2_sha256") == v2_hash)
            and ((delta.get("currentness") or {}).get("load_budget_policy_v2_sha256") == v2_hash)
            and (
                (
                    t35.load_json(tiny_purified.INTEGRATED_MEASUREMENTS)
                    if tiny_purified.INTEGRATED_MEASUREMENTS.is_file()
                    else {}
                ).get("load_budget_policy_v2_sha256")
                == v2_hash
            )
        ),
        "deferred_recycling_unchanged": (
            int(
                (delta.get("remaining_ordinary") or {}).get("deferred_recycling_count") or 0
            )
            == tiny_purified.DEFERRED_RECYCLING_COUNT
        ),
    }


def build() -> dict[str, Any]:
    delta = _load_or_build(tiny_purified.CENSUS_DELTA, "build_t49_census_delta")
    topology = _load_or_build(tiny_purified.CARD_TOPOLOGY, "build_t49_card_topology")
    publication = _load_or_build(tiny_purified.PUBLICATION_DELTA, "build_t49_publication_delta")
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
        "status": "T49_READY" if ready else "T49_BLOCKED",
        "source_revision": tiny_purified.SOURCE_REVISION,
        "production_lock_sha256": tiny_purified.production_lock_sha256(),
        "generated_by": "python tools/build_t49_readiness.py",
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
        "t48_history_readonly": True,
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
            "t49_complete": topology.get("t49_complete"),
            "remaining_recipe_gap": topology.get("remaining_recipe_gap"),
            "publication_status": publication.get("status"),
            "generated_file_count": len(tiny_purified.generated_family_files()),
            "group_winners": publication.get("group_winners"),
            "complete_family_count": delta.get("complete_family_count"),
            "partial_family_count": delta.get("partial_family_count"),
            "opening_execution_gap": delta.get("opening_execution_gap"),
            "completion_delta": delta.get("completion_delta"),
            "reclassification_delta": delta.get("reclassification_delta"),
        },
        "identity_closeout": delta.get("identity_closeout"),
        "v2_load_policy_sha256": tiny_purified.v2_policy_hash() if tiny_purified.LOAD_POLICY_V2.is_file() else "",
        "t49_opening": {
            "next_issue_id": topology.get("next_issue_id"),
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "unique_active_after_t49_ready": topology.get("unique_active_card") is None,
            "t14_closing": (delta.get("t14_load") or {}).get("closing"),
            "t14_closing_basis": tiny_purified.T14_CLOSING_BASIS,
        },
        "note": (
            "T49_READY is derived from source currentness, equivalence, runtime, "
            "player path, load, census and topology. N=5 complete families / "
            "95 relations / 0 new identities. Deferred recycling stays 1817. "
            "Remaining recipe gap is 1349. Bath ordinary remainder is 0. T50 "
            "is consecutive numbering only and is not preassigned Mixer."
        ),
    }


def write() -> dict[str, Any]:
    document = build()
    if document["status"] != "T49_READY":
        raise ValueError(
            "T49 readiness --write is fail-closed until locked player path, "
            "GameTest PASS receipt, and materialization decision exist: "
            + ", ".join(document.get("failed_gates") or [])
        )
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T49",
        "readiness",
        lambda: tiny_purified.check_document(OUTPUT, build()),
    )
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    gates = document.get("gates") or {}
    failed = sorted(name for name, ok in gates.items() if not ok)
    if document.get("failed_gates") != failed:
        errors.append("failed_gates does not match derived gates")
    expected = "T49_READY" if not failed else "T49_BLOCKED"
    if document.get("status") != expected:
        errors.append(f"readiness status {document.get('status')} != {expected}")
    if document.get("status") == "T49_READY" and failed:
        errors.append("T49_READY cannot be claimed while gates fail")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = tiny_purified.parse_write_check(__doc__, argv)
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
        print(f"{tiny_purified.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T49 readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
