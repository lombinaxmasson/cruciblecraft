#!/usr/bin/env python3
"""Build T35 census readiness (R10).

``status`` is derived. ``T35_CENSUS_READY`` requires current upstream T35
artifacts, closed ``T32-VD-001``, zero census validators, reconciled counts,
stable T38+ topology, and load/fidelity semantics including deferred F005
remeasurement. This artifact does not claim ``full_verification_report``, recipes profile,
global release, or GameTest execution; the runtime registry gate is
implemented and hash-locked via the census gate fixture.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import build_t35_card_topology as topology_builder  # noqa: E402
from tools import build_t35_census as census_builder  # noqa: E402
from tools import build_t35_census_inputs as census_inputs_builder  # noqa: E402
from tools import build_t35_excluded_object_reclaim as exclusion_builder  # noqa: E402
from tools import build_t35_load_baseline as load_baseline_builder  # noqa: E402
from tools import build_t35_machine_track as machine_track_builder  # noqa: E402
from tools import build_t35_recipe_families as recipe_families_builder  # noqa: E402
from tools import build_t35_runtime_registry as runtime_registry_builder  # noqa: E402
from tools import t27_common as common  # noqa: E402
from tools import t35_common as t35  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t35.READINESS
POLICY = t35.POLICY
ROOT = t35.ROOT
VERIFICATION_DEBT = common.TOOLS / "known_issues" / "verification-debt.json"
PUBLICATION_DELTA = t35.PUBLICATION_DELTA
T32_VD_001 = "T32-VD-001"
TOPOLOGY_START = topology_builder.START_NUMBER
REPORT_OWNED = ("currentness", "runtime", "status")

UPSTREAM: tuple[tuple[Path, Any], ...] = (
    (t35.INPUTS, census_inputs_builder),
    (t35.RUNTIME_REGISTRY, runtime_registry_builder),
    (t35.EXCLUSION_RECLAIM, exclusion_builder),
    (t35.RECIPE_FAMILIES, recipe_families_builder),
    (t35.LOAD_BASELINE, load_baseline_builder),
    (t35.MACHINE_TRACK, machine_track_builder),
    (t35.CENSUS, census_builder),
    (t35.CARD_TOPOLOGY, topology_builder),
)


def _debt_issue(debt: dict[str, Any], issue_id: str) -> dict[str, Any]:
    for row in debt.get("issues") or []:
        if row.get("id") == issue_id:
            return row
    raise ValueError(f"missing verification debt row {issue_id}")


def _ensure_upstream_current() -> None:
    failures: list[str] = []
    for path, module in UPSTREAM:
        rel = t35.relative(path)
        if not path.is_file():
            failures.append(f"missing upstream artifact: {rel}")
            continue
        errors = t35.compact_check(module)
        if errors:
            failures.extend(errors)
    if failures:
        raise ValueError("; ".join(failures))


def _pending_filled_with_zero(value: Any) -> bool:
    if isinstance(value, dict):
        if value.get("status") == "pending":
            for key in common.FAKE_ZERO_LOAD_KEYS:
                if value.get(key) == 0:
                    return True
        load = value.get("load")
        if isinstance(load, dict) and load.get("status") == "pending":
            for key in common.FAKE_ZERO_LOAD_KEYS:
                if load.get(key) == 0:
                    return True
        return any(_pending_filled_with_zero(item) for item in value.values())
    if isinstance(value, list):
        return any(_pending_filled_with_zero(item) for item in value)
    return False


def _topology_gates(
    topology: dict[str, Any],
    census: dict[str, Any],
) -> dict[str, bool]:
    cards = topology.get("cards") or []
    card_ids = [card.get("id") for card in cards]
    consecutive = card_ids == [f"T{TOPOLOGY_START + index}" for index in range(len(cards))]
    t37_ids = set((census.get("t37_pilot") or {}).get("family_ids") or [])
    generated_canonical = {
        item["canonical_id"]
        for card in cards
        for item in card.get("identities") or []
    }
    pilot_leak = bool(t37_ids & generated_canonical)
    fixed = {card.get("id") for card in topology.get("fixed_cards") or []}
    return {
        "topology_current": topology.get("status") == "T35_CARD_TOPOLOGY",
        "topology_epoch_t35r_a": topology.get("topology_epoch") == t35.TOPOLOGY_EPOCH,
        "topology_numbering_policy": topology.get("numbering_policy")
        == t35.NUMBERING_POLICY,
        "topology_global_execution_gate_t37": topology.get("global_execution_gate")
        == t35.GLOBAL_EXECUTION_GATE,
        "topology_publication_delta_zero": topology.get("publication_delta")
        == PUBLICATION_DELTA,
        "topology_started_false": topology.get("started") is False,
        "generated_cards_not_started": all(card.get("started") is False for card in cards),
        "generated_cards_consecutive_from_t38": consecutive,
        "generated_card_count_matches_cards": topology.get("generated_card_count")
        == len(cards),
        "fixed_nodes_t36_t37": fixed == {"T36", "T37"},
        "fixed_nodes_not_renumbered": all(
            card.get("id") not in card_ids for card in topology.get("fixed_cards") or []
        ),
        "topology_validators_zero": all(
            int(value or 0) == 0
            for key, value in (topology.get("validators") or {}).items()
            if key != "started_generated_cards"
        )
        and int((topology.get("validators") or {}).get("started_generated_cards") or 0)
        == 0,
        "t37_pilot_not_in_generated_cards": not pilot_leak
        and int((topology.get("validators") or {}).get("t37_pilot_in_generated_cards") or 0)
        == 0,
        "epoch_a_generated_cards_depend_on_t37": all(
            t35.GLOBAL_EXECUTION_GATE in (card.get("depends_on") or [])
            for card in cards
        )
        and int((topology.get("validators") or {}).get("t37_gate_bypass") or 0) == 0,
        "epoch_b_append_only_contract": (
            (topology.get("epoch_b") or {}).get("policy") == "append_only"
            and (topology.get("epoch_b") or {}).get("may_renumber_epoch_a") is False
        ),
        "topology_broken_dependencies_zero": int(
            (topology.get("validators") or {}).get("broken_dependencies") or 0
        )
        == 0,
        "topology_cycles_zero": int((topology.get("validators") or {}).get("cycles") or 0)
        == 0,
    }


def _closure_gates(
    policy: dict[str, Any],
    inputs: dict[str, Any],
    exclusion: dict[str, Any],
    recipe_families: dict[str, Any],
    census: dict[str, Any],
    topology: dict[str, Any],
    debt_issue: dict[str, Any],
    runtime_registry: dict[str, Any],
) -> dict[str, bool]:
    counts = census.get("counts") or {}
    validators = census.get("validators") or {}
    membership = recipe_families.get("membership") or {}
    exclusion_counts = exclusion.get("counts") or {}
    t13_tables = inputs.get("t13_seven_tables") or {}
    t22_5 = inputs.get("t22_5_counts") or {}
    storage = policy.get("storage_counts") or {}
    exclusion_policy = policy.get("exclusion_counts") or {}
    identities = census.get("identities") or {}
    machine_track = t35.load_json(t35.MACHINE_TRACK)
    storage_scope = t35.load_json(t35.STORAGE_SCOPE)
    machine_validators = machine_track.get("validators") or {}
    topology_gates = _topology_gates(topology, census)
    gate_fixture = runtime_registry_builder._gate_fixture(runtime_registry)
    fixture_path = ROOT / "src/main/resources/census/t35_runtime_registry_gate.json"
    fixture_on_disk = (
        json.loads(fixture_path.read_text(encoding="utf-8"))
        if fixture_path.is_file()
        else {}
    )
    return {
        "t32_vd_001_closed": debt_issue.get("status") == "closed",
        "census_inputs_publication_delta_zero": inputs.get("publication_delta")
        == PUBLICATION_DELTA,
        "census_publication_delta_zero": census.get("publication_delta")
        == PUBLICATION_DELTA,
        "load_baseline_publication_delta_zero": t35.load_json(t35.LOAD_BASELINE).get(
            "publication_delta"
        )
        == PUBLICATION_DELTA,
        "census_validators_zero": all(int(value or 0) == 0 for value in validators.values()),
        "census_unclassified_zero": int(validators.get("unclassified") or 0) == 0,
        "census_unscoped_zero": int(validators.get("unscoped") or 0) == 0,
        "census_scope_errors_zero": int(validators.get("scope_errors") or 0) == 0,
        "all_identities_have_portfolio_scope": all(
            record.get("portfolio_scope") in t35.PORTFOLIO_SCOPES
            for record in identities.values()
        ),
        "census_owner_violations_zero": int(validators.get("owner_violations") or 0) == 0,
        "census_dependency_errors_zero": int(validators.get("dependency_errors") or 0) == 0,
        "t13_identities_765": int(counts.get("t13_identities") or 0)
        == int(t13_tables.get("canonical_identities_total") or 0)
        == int(policy["t13_seven_tables"]["expected_canonical_identities_total"]),
        "exclusion_source_sites_763": int(counts.get("exclusion_source_sites") or 0)
        == int(exclusion_counts.get("source_sites") or 0)
        == int(exclusion_policy.get("excluded_call_sites") or 0),
        "exclusion_expanded_rows_1701": int(counts.get("exclusion_expanded_rows") or 0)
        == int(exclusion_counts.get("expanded_multiplicity") or 0)
        == int(exclusion_policy.get("excluded_expanded_registrations") or 0),
        "storage_source_sites_28": int(exclusion_counts.get("storage_source_sites") or 0)
        == int(storage.get("expected_source_sites") or 0),
        "storage_expanded_rows_624": int(
            exclusion_counts.get("storage_expanded_multiplicity") or 0
        )
        == int(storage.get("expected_expanded_registrations") or 0),
        "recipe_rows_accounted_78682": int(counts.get("recipe_rows_accounted") or 0)
        == int(t22_5.get("ordinary_optional") or 0)
        == int(membership.get("assigned") or 0),
        "recipe_membership_duplicate_zero": int(membership.get("duplicate") or 0) == 0,
        "recipe_membership_unassigned_zero": int(membership.get("unassigned") or 0) == 0,
        "runtime_ids_expected_equals_mapped": int(counts.get("runtime_ids_expected") or 0)
        == int(counts.get("runtime_ids_mapped") or 0),
        "runtime_registry_gate_fixture_hash_locked": (
            gate_fixture["full_artifact_sha256"]
            == t35.sha256_file(t35.RUNTIME_REGISTRY)
            and fixture_on_disk.get("full_artifact_sha256")
            == gate_fixture["full_artifact_sha256"]
        ),
        "exclusion_unmapped_zero": int(exclusion_counts.get("unmapped") or 0) == 0,
        "exclusion_duplicate_family_membership_zero": int(
            exclusion_counts.get("duplicate_family_membership") or 0
        )
        == 0,
        "census_status_aggregated": census.get("status") == "T35_CENSUS_AGGREGATED",
        "machine_track_current": machine_track.get("status") == "T35_MACHINE_TRACK",
        "machine_track_validators_zero": all(
            int(value or 0) == 0 for value in machine_validators.values()
        ),
        "machine_track_conclusion_a": (
            (machine_track.get("conclusions") or {}).get("material_tier_matrix") == "A"
            and (machine_track.get("conclusions") or {}).get("eu_voltage") == "A"
            and (machine_track.get("conclusions") or {}).get("generated_material_cards_required")
            is False
            and (machine_track.get("conclusions") or {}).get("generated_eu_cards_required")
            is False
            and (machine_track.get("conclusions") or {}).get("generated_tu_cards_required")
            is False
        ),
        "storage_scope_current": storage_scope.get("status") == "T35_STORAGE_SCOPE",
        "storage_adjacent_families_scoped": all(
            identities.get(f"exclusion/{family}", {}).get("portfolio_scope") == scope
            for family, scope in {
                "chest": "post_1x",
                "safe": "post_1x",
                "tank": "post_1x",
                "fluid_container": "candidate_1x",
                "pump": "post_1x",
                "sorting": "post_1x",
                "hopper": "in_scope_1x",
            }.items()
        ),
        **topology_gates,
    }


def _fidelity_gates(
    policy: dict[str, Any],
    inputs: dict[str, Any],
    load_baseline: dict[str, Any],
    census: dict[str, Any],
) -> dict[str, bool]:
    f005 = load_baseline.get("axes", {}).get("f005_release_scale") or {}
    historical = f005.get("historical") or {}
    pipes = load_baseline.get("axes", {}).get("topology_pipes_total") or {}
    visited = load_baseline.get("axes", {}).get("item_route_visited_max") or {}
    superseded = (load_baseline.get("supersedes") or [{}])[0]
    opening = load_baseline.get("opening_publication") or {}
    pub_policy = policy.get("publication_opening") or {}
    historical_ok = True
    for record in census.get("identities", {}).values():
        if "historical_classification" not in record:
            historical_ok = False
            break
    return {
        "source_revision_pinned": policy.get("source_revision") == t35.SOURCE_REVISION,
        "inputs_policy_sha256_current": inputs.get("policy_sha256")
        == t35.sha256_file(POLICY),
        "historical_classification_preserved": historical_ok,
        "f005_historical_closed_preserved": historical.get("current_status") == "CLOSED",
        "f005_canonical_deferred_release_remeasurement": (
            f005.get("canonical_assessment") == "DEFERRED_RELEASE_REMEASUREMENT"
            and f005.get("verdict") == "DEFERRED_RELEASE_REMEASUREMENT"
            and f005.get("verdict") != "PASS"
        ),
        "f005_metrics_not_pass": all(
            metric.get("verdict") == "DEFERRED_RELEASE_REMEASUREMENT"
            for metric in (f005.get("metrics") or {}).values()
        ),
        "topology_pipes_item_route_scales_distinct": pipes.get("opening_base")
        == {"target": 500, "stress": 2000}
        and visited.get("opening_base") == {"target": 250, "stress": 1000}
        and pipes.get("comparable_to") == []
        and visited.get("comparable_to") == [],
        "t27_opening_superseded_not_averaged": superseded.get("opening")
        == {"logical": 19087, "eager": 16862, "lazy": 2225}
        and opening
        == {
            "logical": pub_policy.get("expected_logical"),
            "eager": pub_policy.get("expected_eager"),
            "lazy": pub_policy.get("expected_lazy"),
        },
        "generated_topology_card_count_before_census_null": policy.get(
            "generated_topology_card_count_before_census"
        )
        is None,
    }


def _load_gates(
    load_baseline: dict[str, Any],
    census: dict[str, Any],
    topology: dict[str, Any],
) -> dict[str, bool]:
    t14_ids = load_baseline.get("t14_axis_ids") or []
    axes = load_baseline.get("axes") or {}
    t14_axes = [axes[axis_id] for axis_id in t14_ids if axis_id in axes]
    logical = axes.get("logical_publication_rows") or {}
    return {
        "load_baseline_ready": load_baseline.get("status") == "T35_LOAD_BASELINE_READY",
        "t14_axis_count_13": load_baseline.get("t14_axis_count") == 13,
        "t14_axes_not_zero_filled": all(not axis.get("zero_filled") for axis in t14_axes),
        "logical_publication_diagnostic_only": logical.get("diagnostic") is True
        and logical.get("verdict") == "DIAGNOSTIC_ONLY"
        and logical.get("hard_ceiling") is None,
        "publication_delta_zero": load_baseline.get("publication_delta") == PUBLICATION_DELTA,
        "census_publication_delta_zero": census.get("publication_delta") == PUBLICATION_DELTA,
        "pending_load_not_zero_filled": not (
            _pending_filled_with_zero(census.get("identities") or {})
            or _pending_filled_with_zero(topology.get("cards") or [])
        ),
        "p2_admission_blocked_pending_measurement": all(
            record.get("p2_admission") != t35.P2_ADMISSION
            for record in (census.get("identities") or {}).values()
            if record.get("portfolio_priority") == "P2"
        ),
        "hard_ceiling_mutation_forbidden": (
            (common.load_json(POLICY).get("t14_authority") or {}).get(
                "hard_ceiling_mutation"
            )
            == "forbidden"
        ),
    }


def build() -> dict[str, Any]:
    if not VERIFICATION_DEBT.is_file():
        raise FileNotFoundError(t35.relative(VERIFICATION_DEBT))
    debt = common.load_json(VERIFICATION_DEBT)
    debt_issue = _debt_issue(debt, T32_VD_001)
    if debt_issue.get("status") != "closed":
        raise ValueError(f"{T32_VD_001} must be closed before T35 readiness")
    _ensure_upstream_current()
    policy = common.load_json(POLICY)
    inputs = common.load_json(t35.INPUTS)
    exclusion = common.load_json(t35.EXCLUSION_RECLAIM)
    recipe_families = common.load_json(t35.RECIPE_FAMILIES)
    load_baseline = common.load_json(t35.LOAD_BASELINE)
    census = common.load_json(t35.CENSUS)
    topology = common.load_json(t35.CARD_TOPOLOGY)
    runtime_registry = common.load_json(t35.RUNTIME_REGISTRY)
    machine_track = common.load_json(t35.MACHINE_TRACK)
    storage_scope = common.load_json(t35.STORAGE_SCOPE)
    closure = _closure_gates(
        policy, inputs, exclusion, recipe_families, census, topology, debt_issue,
        runtime_registry,
    )
    fidelity = _fidelity_gates(policy, inputs, load_baseline, census)
    load_data = _load_gates(load_baseline, census, topology)
    closure_ok = all(value is True for value in closure.values())
    fidelity_ok = all(value is True for value in fidelity.values())
    load_ok = all(value is True for value in load_data.values())
    gates_ok = closure_ok and fidelity_ok and load_ok
    owned_inputs = {
        t35.relative(BUILDER): t35.sha256_file(BUILDER),
        t35.relative(POLICY): t35.sha256_file(POLICY),
        t35.relative(VERIFICATION_DEBT): t35.sha256_file(VERIFICATION_DEBT),
        t35.relative(t35.STORAGE_SCOPE): t35.sha256_file(t35.STORAGE_SCOPE),
        **{
            t35.relative(path): t35.sha256_file(path)
            for path, _ in UPSTREAM
        },
    }
    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "static_gates_only",
        "policy": (
            "The status field is derived from evidence by build(). "
            "T35_CENSUS_READY cannot be hand-written. This receipt does not "
            "claim full_verification_report, recipes profile, global release, "
            "or Java runtime registry GameTest execution."
        ),
        "source_revision": t35.SOURCE_REVISION,
        "generated_by": "python tools/build_t35_readiness.py --write",
        "closure": closure,
        "fidelity": fidelity,
        "load": load_data,
        "closure_policy": {
            "final_closure_attempted": True,
            "pending": [],
            "reason": (
                "T35 closes the 1.x census baseline with zero publication "
                "delta, deferred F005 release remeasurement, and generated "
                "T38+ topology only."
            ),
        },
        "owned_inputs": owned_inputs,
        "currentness": {"owned_inputs": owned_inputs},
        "runtime": {
            "java_runtime_gate_schema_version": (
                (runtime_registry.get("java_runtime_gate") or {}).get(
                    "compatible_schema_version"
                )
            ),
        "runtime_registry_gate_configured": True,
            "runtime_registry_gate_fixture": (
                "src/main/resources/census/t35_runtime_registry_gate.json"
            ),
            "runtime_registry_gate_fixture_hash_locked": (
                runtime_registry_builder._gate_fixture(
                    runtime_registry
                )["full_artifact_sha256"]
                == t35.sha256_file(t35.RUNTIME_REGISTRY)
            ),
            "runtime_registry_gate_gametest_class": (
                "com.masson.cruciblecraft.census.T35CensusGameTests"
            ),
            "runtime_registry_gate_gradle_property": "-Pt35Census",
            "runtime_registry_gate_implemented": True,
            "runtime_registry_gate_namespace": "cruciblecraft_census",
            "manual_runtime_registry_gate_required": False,
            "manual_gametest_required": True,
            "note": (
                "Bidirectional Java registry equality is implemented via "
                "T35CensusGameTests in namespace cruciblecraft_census with "
                "expected ids from the hash-locked gate fixture. Static "
                "readiness records wiring and fixture currentness only; "
                "runGameTestServer -Pt35Census is the runtime execution "
                "evidence for the census integration profile."
            ),
        },
        "verification_debt": {
            "issue_id": T32_VD_001,
            "status": debt_issue.get("status"),
            "resolved_by": debt_issue.get("resolved_by"),
        },
        "census_summary": {
            "counts": census.get("counts"),
            "generated_topology_card_count_before_census": census.get(
                "generated_topology_card_count_before_census"
            ),
            "publication_delta": census.get("publication_delta"),
            "validators": census.get("validators"),
            "work_set_mandatory_p0_p1": len(
                (census.get("work_sets") or {}).get("mandatory_p0_p1") or []
            ),
            "portfolio_scope_counts": {
                scope: len((census.get("portfolio_scope_sets") or {}).get(scope) or [])
                for scope in t35.PORTFOLIO_SCOPES
            },
        },
        "topology_summary": {
            "fixed_cards": [card.get("id") for card in topology.get("fixed_cards") or []],
            "generated_card_count": topology.get("generated_card_count"),
            "generated_card_ids": [card.get("id") for card in topology.get("cards") or []],
            "publication_delta": topology.get("publication_delta"),
            "start_number": topology.get("start_number"),
            "topology_epoch": topology.get("topology_epoch"),
            "numbering_policy": topology.get("numbering_policy"),
            "global_execution_gate": topology.get("global_execution_gate"),
            "validators": topology.get("validators"),
        },
        "machine_track_summary": {
            "conclusions": machine_track.get("conclusions"),
            "selected_matrix": machine_track.get("selected_matrix"),
            "validators": machine_track.get("validators"),
        },
        "storage_scope_summary": {
            "status": storage_scope.get("status"),
            "families": {
                family: (storage_scope.get("families") or {}).get(family, {}).get(
                    "portfolio_scope"
                )
                for family in (
                    "chest",
                    "safe",
                    "tank",
                    "fluid_container",
                    "pump",
                    "sorting",
                    "hopper",
                )
            },
        },
    }
    if gates_ok:
        document["status"] = "T35_CENSUS_READY"
    return document


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {t35.relative(OUTPUT)}"]
    on_disk = json.loads(OUTPUT.read_text(encoding="utf-8"))
    expected = build()
    errors: list[str] = []
    if on_disk.get("status") != expected.get("status"):
        errors.append("status is stale or hand-written")
    for key in REPORT_OWNED:
        expected.pop(key, None)
        on_disk.pop(key, None)
    if common.stable_json(expected) != common.stable_json(on_disk):
        diff = t35.first_json_diff(expected, on_disk)
        errors.append(
            f"{t35.relative(OUTPUT)} is stale"
            + (f" at {diff}" if diff else "")
        )
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write or --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {t35.relative(OUTPUT)} status={document.get('status')}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t35.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T35 readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
