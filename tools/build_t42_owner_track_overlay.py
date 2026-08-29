#!/usr/bin/env python3
"""Partition T42's retained gap into actionable owner tracks and recovery evidence."""
from __future__ import annotations

import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_common as t42
from tools import t42_owner_common as common
from tools import build_t42_blocker_overlay as blocker
from tools import build_t42_owner_pre_freeze as freeze_builder
from tools.build_t42_family_operand_snapshot import expand_relation

OUTPUT = common.TRACK_OVERLAY
RECOVERY_OUTPUT = common.RECOVERY_EVIDENCE


def _snapshot() -> dict[str, Any]:
    if not t42.FAMILY_OPERAND_SNAPSHOT.is_file():
        raise FileNotFoundError("T42-Owner requires t42_family_operand_snapshot.json")
    return t42.load_json(t42.FAMILY_OPERAND_SNAPSHOT)


def _owner_axes(family: dict[str, Any]) -> list[str]:
    secondary = set(family.get("secondary_blockers") or [])
    axes: set[str] = set()
    if family.get("missing_forms"):
        axes.add("material_expression/form")
    if family.get("missing_fluids"):
        molten_only = all(
            str(value).startswith(("molten.", "molten "))
            for value in family.get("missing_fluids") or []
        )
        axes.add("material_expression/molten" if molten_only else "material_expression/fluid")
    if "unmapped_operands" in secondary or family.get("unsupported_semantics"):
        axes.add("identity_mapping/unmapped")
    if "unproven_vanilla" in secondary:
        axes.add("registry_proof/vanilla")
    if "b0_unreachable_inputs" in secondary or "acquisition_open" in secondary:
        axes.add("acquisition/b0")
    if "partial" in secondary:
        axes.add("coordination/multi_axis")
    return sorted(axes)


def _material_expression_track(family: dict[str, Any]) -> str:
    has_forms = bool(family.get("missing_forms"))
    fluids = family.get("missing_fluids") or []
    has_fluids = bool(fluids)
    molten_only = has_fluids and all(
        str(value).startswith(("molten.", "molten ")) for value in fluids
    )
    if has_forms and has_fluids:
        return "material_expression/mixed"
    if has_forms:
        return "material_expression/form"
    if molten_only:
        return "material_expression/molten"
    return "material_expression/fluid"


def _recovery_relation(
    relation: dict[str, Any],
    *,
    catalogs: dict[str, Any],
    registry: set[str],
) -> dict[str, Any]:
    """Assess one source relation as a pure consumed-MTE material recovery."""
    mapped = blocker._map_relation(relation, catalogs)
    item_inputs = list(mapped.get("item_inputs") or [])
    item_outputs = list(mapped.get("item_outputs") or [])
    fluid_inputs = list(mapped.get("fluid_inputs") or [])
    fluid_outputs = list(mapped.get("fluid_outputs") or [])
    actions = list(mapped.get("item_input_actions") or [])
    blockers: set[str] = set()
    material_outputs: set[str] = set()
    input_objects: set[str] = set()
    fluid_output_mappings: list[dict[str, Any]] = []

    def has_positive_amount(operand: dict[str, Any]) -> bool:
        value = operand.get("amount", operand.get("count", 1))
        try:
            return int(value) > 0
        except (TypeError, ValueError):
            return False

    if not item_inputs:
        blockers.add("no_item_input")
    for index, item in enumerate(item_inputs):
        source = t42.map_item_source(item, catalogs)
        action = t42.normalize_action(actions[index] if index < len(actions) else "consume")
        if source.get("kind") != "unique_object" or source.get("unique_kind") != "mte":
            blockers.add("non_mte_item_input")
        if action != "CONSUME":
            blockers.add("non_consumed_mte_input")
        if not has_positive_amount(item):
            blockers.add("non_positive_mte_input")
        input_objects.add(f"{source.get('item')}@{source.get('meta')}")

    if fluid_inputs:
        blockers.add("additional_fluid_input")

    if not item_outputs and not fluid_outputs:
        blockers.add("no_material_output")
    for item in item_outputs:
        source = t42.map_item_source(item, catalogs)
        runtime_id = source.get("runtime_id")
        if (
            source.get("kind") != "material_form"
            or not source.get("material")
            or runtime_id not in registry
            or not has_positive_amount(item)
        ):
            blockers.add("non_registered_material_item_output")
        else:
            material_outputs.add(str(source["material"]))
    for fluid in fluid_outputs:
        fluid_id = str(fluid.get("fluid") or fluid.get("id") or "")
        mapping = (catalogs.get("fluid_mapping") or {}).get(fluid_id) or {}
        runtime_id = str(mapping.get("cc_fluid_id") or "")
        material = str(mapping.get("cc_material") or "")
        derivation = str(mapping.get("derivation") or "")
        mapped = mapping.get("disposition") == "mapped"
        fluid_output_mappings.append(
            {
                "cc_fluid_id": runtime_id or None,
                "cc_material": material or None,
                "derivation": derivation or None,
                "fluid": fluid_id,
                "material_id": mapping.get("material_id"),
                "runtime_registered": runtime_id in registry,
            }
        )
        if (
            not mapped
            or derivation not in {"molten_fluid", "chemical_gate"}
            or not material
            or runtime_id not in registry
            or not has_positive_amount(fluid)
        ):
            blockers.add("non_registered_material_fluid_output")
        else:
            material_outputs.add(material)
    if not material_outputs:
        blockers.add("no_proven_material_identity_output")

    return {
        "input_objects": sorted(input_objects),
        "fluid_output_mappings": fluid_output_mappings,
        "material_outputs": sorted(material_outputs),
        "proven": not blockers,
        "relation_key": f"{relation.get('source_map')}#{relation.get('recipe_index')}",
        "row_sha256": relation.get("row_sha256"),
        "shape_blockers": sorted(blockers),
    }


def _recovery_evidence(
    *,
    retained_by_id: dict[str, dict[str, Any]],
    overlay_by_id: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    """Recompute MTE recovery evidence from compact source relations, not labels."""
    snapshot = _snapshot()
    catalogs = t42.load_runtime_catalogs()
    registry = blocker._registry_ids(catalogs)
    families = {
        str(row["family_id"]): row
        for row in snapshot.get("families") or []
        if str(row.get("family_id") or "") in retained_by_id
    }
    mte_ids = {
        family_id
        for family_id, row in overlay_by_id.items()
        if family_id in retained_by_id and "mte" in set(row.get("unique_kinds") or [])
    }
    rows: list[dict[str, Any]] = []
    for family_id in sorted(mte_ids):
        family = families.get(family_id)
        if family is None:
            raise ValueError(f"recovery evidence missing snapshot family: {family_id}")
        base = overlay_by_id[family_id]
        relations = [
            _recovery_relation(
                expand_relation(family, relation, snapshot.get("dictionaries") or {}),
                catalogs=catalogs,
                registry=registry,
            )
            for relation in family.get("relations") or []
        ]
        secondary = set(base.get("secondary_blockers") or []) - {"unique_objects"}
        family_blockers = set(secondary)
        for relation in relations:
            family_blockers.update(relation["shape_blockers"])
        clean_mte_kind = list(base.get("unique_kinds") or []) == ["mte"]
        if not clean_mte_kind:
            family_blockers.add("mixed_unique_kinds")
        eligible = bool(relations) and clean_mte_kind and all(
            relation["proven"] for relation in relations
        ) and not secondary
        rows.append(
            {
                "eligible_phase_deferred": eligible,
                "evidence_root_sha256": common.sha256_stable(
                    {
                        "family_id": family_id,
                        "relations": relations,
                        "secondary_blockers": sorted(secondary),
                        "unique_kinds": base.get("unique_kinds") or [],
                    }
                ),
                "family_id": family_id,
                "host": base["host"],
                "mte_only": clean_mte_kind,
                "output_materials": sorted(
                    {
                        material
                        for relation in relations
                        for material in relation["material_outputs"]
                    }
                ),
                "reason": (
                    "proven_consumed_mte_material_recovery"
                    if eligible
                    else "recovery_evidence_incomplete"
                ),
                "relation_count": len(relations),
                "relations": relations,
                "secondary_blockers": sorted(secondary),
                "shape_blockers": sorted(family_blockers),
                "template_key": base["template_key"],
            }
        )
    by_host = Counter(row["host"] for row in rows)
    eligible_by_host = Counter(
        row["host"] for row in rows if row["eligible_phase_deferred"]
    )
    return {
        "eligible_phase_deferred_count": sum(
            1 for row in rows if row["eligible_phase_deferred"]
        ),
        "families": rows,
        "family_count": len(rows),
        "generated_by": "python tools/build_t42_owner_track_overlay.py",
        "mte_family_count_by_host": dict(sorted(by_host.items())),
        "note": (
            "A recovery family must consume only MTE item inputs, contain no "
            "additional fluid input, and produce only registered material-form "
            "items or material-identified molten/chemical fluids in every "
            "relation. An MTE kind tag or a smelter/bath host alone is not proof."
        ),
        "opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "phase_deferred_eligible_by_host": dict(sorted(eligible_by_host.items())),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_OWNER_RECOVERY_EVIDENCE",
    }


def _owner_assignment(
    family: dict[str, Any],
    recovery: dict[str, Any] | None,
) -> tuple[str, str, list[str]]:
    """Choose one current owner; all other blockers remain explicit secondary tracks."""
    primary = str(family.get("primary_bucket") or "")
    axes = set(_owner_axes(family))
    unique_kinds = sorted(set(family.get("unique_kinds") or []))
    has_unique_objects = bool(family.get("unique_objects"))

    if primary == "current_closure_ready":
        owner = "recipe_wave/" + str(family["host"]).split(":", 1)[-1]
        return owner, "ready_for_production_lock", sorted(axes)
    if primary == "needs_prefix_or_molten":
        owner = _material_expression_track(family)
        return owner, "bounded_expression_needed", sorted(axes - {owner})

    if has_unique_objects:
        if unique_kinds == ["mte"]:
            if recovery and recovery.get("eligible_phase_deferred"):
                owner = "recycling/proven"
                state = "recovery_proven_for_lock"
            else:
                owner = "recycling/evidence_needed"
                state = "recovery_evidence_needed"
            return owner, state, sorted(axes)
        if len(unique_kinds) == 1 and not axes:
            owner = "object_expression/" + unique_kinds[0]
            return owner, "object_boundary_needed", []
        owner = "coordination/multi_axis"
        object_axes = {f"object_expression/{kind}" for kind in unique_kinds}
        return owner, "multi_axis_coordination_needed", sorted(axes | object_axes)

    non_coordination = sorted(axis for axis in axes if axis != "coordination/multi_axis")
    if len(non_coordination) == 1:
        owner = non_coordination[0]
        state = {
            "identity_mapping/unmapped": "identity_mapping_needed",
            "registry_proof/vanilla": "registry_proof_needed",
            "acquisition/b0": "acquisition_path_needed",
        }.get(owner, "multi_axis_coordination_needed")
        return owner, state, []
    owner = "coordination/multi_axis"
    return owner, "multi_axis_coordination_needed", non_coordination


def build() -> dict[str, Any]:
    base = common.repaired_t42()
    freeze_errors = freeze_builder.freeze_errors()
    if freeze_errors:
        raise ValueError("; ".join(freeze_errors))
    overlay_by_id = {
        row["family_id"]: row for row in base["overlay"].get("families") or []
    }
    retained = [
        row
        for row in base["lock"].get("families") or []
        if row.get("disposition") == "retained_current_execution_gap"
    ]
    retained_by_id = {row["family_id"]: row for row in retained}
    if set(retained_by_id) - set(overlay_by_id):
        raise ValueError("T42-Owner lock contains families absent from overlay")
    evidence = _recovery_evidence(
        retained_by_id=retained_by_id,
        overlay_by_id=overlay_by_id,
    )
    recovery_by_id = {row["family_id"]: row for row in evidence["families"]}

    rows = []
    for family_id in sorted(retained_by_id):
        source = overlay_by_id[family_id]
        recovery = recovery_by_id.get(family_id)
        owner, owner_state, secondary_tracks = _owner_assignment(source, recovery)
        if not common.valid_owner_track(owner):
            raise ValueError(f"{family_id} has invalid current_owner {owner!r}")
        if owner_state not in common.OWNER_STATES:
            raise ValueError(f"{family_id} has invalid owner_state {owner_state!r}")
        rows.append(
            {
                "blocking_axes": _owner_axes(source),
                "current_owner": owner,
                "evidence_root_sha256": common.sha256_stable(
                    {
                        "base_evidence_root": source.get("evidence_root_sha256"),
                        "current_owner": owner,
                        "owner_state": owner_state,
                        "recovery_evidence_root": (
                            recovery.get("evidence_root_sha256") if recovery else None
                        ),
                        "secondary_owner_tracks": secondary_tracks,
                    }
                ),
                "family_id": family_id,
                "host": source["host"],
                "interleave_disposition": "tracked_current_execution_gap",
                "owner_state": owner_state,
                "primary_bucket": source["primary_bucket"],
                "recovery_evidence_state": (
                    recovery["reason"] if recovery else "not_mte_recovery_candidate"
                ),
                "relation_count": source["relation_count"],
                "secondary_owner_tracks": secondary_tracks,
                "template_key": source["template_key"],
            }
        )
    if len(rows) != common.OPENING_EXECUTION_GAP:
        raise ValueError("T42-Owner overlay does not cover the retained execution gap")
    if any(not row["current_owner"] for row in rows):
        raise ValueError("T42-Owner overlay has an unassigned current owner")

    primary_counts = Counter(row["primary_bucket"] for row in rows)
    owner_counts = common.owner_counts(rows, "current_owner")
    state_counts = common.owner_counts(rows, "owner_state")
    overlay = {
        "base_t42_closing_execution_gap": common.OPENING_EXECUTION_GAP,
        "current_owner_counts": owner_counts,
        "family_count": len(rows),
        "families": rows,
        "generated_by": "python tools/build_t42_owner_track_overlay.py",
        "interleave_gate": "owner_partition_complete",
        "note": (
            "This is an additive owner partition over T42's retained gap. "
            "current_owner does not reduce the gap. future_owner is reserved "
            "for a later, evidence-backed phase-deferred disposition lock."
        ),
        "owner_state_counts": state_counts,
        "owns_families": 0,
        "primary_bucket_counts": dict(sorted(primary_counts.items())),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_OWNER_TRACK_OVERLAY",
    }
    return {"overlay": overlay, "recovery": evidence}


def write() -> None:
    documents = build()
    t35.write_stable(OUTPUT, documents["overlay"])
    t35.write_stable(RECOVERY_OUTPUT, documents["recovery"])


def check() -> list[str]:
    documents = build()
    errors = common.check_document(OUTPUT, documents["overlay"])
    errors.extend(common.check_document(RECOVERY_OUTPUT, documents["recovery"]))
    return errors


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42-Owner owner-track overlay", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42-Owner track overlay and recovery evidence.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42-Owner track overlay and recovery evidence are current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42-Owner track overlay failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
