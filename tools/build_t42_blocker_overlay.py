#!/usr/bin/env python3
"""Classify remaining ordinary families into T42 blocker buckets."""
from __future__ import annotations

import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_common as common
from tools.build_t42_family_operand_snapshot import iter_snapshot_relations

OUTPUT = common.BLOCKER_OVERLAY
DISPOSITION = common.OPERAND_DISPOSITION
CANDIDATES = common.WAVE_CANDIDATES


def _b0() -> set[str]:
    if common.REACHABILITY_BASELINE.is_file():
        document = common.load_json(common.REACHABILITY_BASELINE)
        identities = document.get("reachable_identities")
        if identities:
            return set(identities)
        added = set(document.get("added_identities") or [])
        t21 = common.load_json(common.T21_REACHABILITY)
        base = set((t21.get("closure") or {}).get("reachable_identities") or [])
        return base | added
    from tools import build_t42_reachability_baseline as baseline

    rebuilt = baseline.build()
    identities = rebuilt.get("reachable_identities")
    if identities:
        return set(identities)
    added = set(rebuilt.get("added_identities") or [])
    t21 = common.load_json(common.T21_REACHABILITY)
    return set((t21.get("closure") or {}).get("reachable_identities") or []) | added


def _allowlist() -> set[str]:
    if common.VANILLA_ALLOWLIST.is_file():
        return set(common.load_json(common.VANILLA_ALLOWLIST).get("item_ids") or [])
    from tools import build_t42_runtime_expression_inventory as inventory

    document = inventory.build()
    return set(document["_allowlist"]["item_ids"])


def _registry_ids(catalogs: dict[str, Any]) -> set[str]:
    ids: set[str] = set()
    for category in ("items", "blocks", "fluids"):
        ids.update(catalogs["registry_ids"].get(category) or [])
    return ids


def _operand_row(
    family: dict[str, Any],
    mapped: dict[str, Any],
    *,
    side: str,
    index: int,
    b0: set[str],
    allowlist: set[str],
    registry: set[str],
    combinatorial: bool,
) -> dict[str, Any]:
    runtime_id = mapped.get("runtime_id")
    kind = str(mapped.get("kind") or "")
    identity = common.typed_identity(kind, runtime_id)
    disposition = "unsupported"
    reason = kind or "unmapped"
    if combinatorial and kind in {"unique_object", "unmapped", "unknown"}:
        disposition = "phase_deferred"
        reason = "combinatorial_player_path_unproven"
    elif kind in {"material_form", "cc_static"}:
        if runtime_id and runtime_id in registry:
            disposition = "proven_equivalent"
            reason = kind
        else:
            disposition = "needs_current_expression"
            reason = "runtime_id_not_registered"
    elif kind in {"chemical_fluid", "molten_mapped", "vanilla_fluid"}:
        fluid_id = runtime_id or ""
        if fluid_id in registry or fluid_id in {
            "minecraft:water",
            "minecraft:lava",
        } or identity in b0:
            disposition = "proven_equivalent"
            reason = kind
        else:
            disposition = "needs_current_expression"
            reason = "fluid_not_registered"
    elif kind == "missing_form":
        disposition = "needs_current_expression"
        reason = "missing_material_form"
    elif kind == "missing_molten":
        disposition = "needs_current_expression"
        reason = "missing_molten"
    elif kind == "unique_object":
        disposition = "phase_deferred"
        reason = f"unique_object:{mapped.get('unique_kind')}"
    elif kind == "empty":
        disposition = "proven_equivalent"
        reason = "empty_slot"
    elif kind == "vanilla_alias":
        disposition = "proven_equivalent"
        reason = "vanilla_alias"
    elif kind == "vanilla_source":
        if runtime_id in allowlist:
            disposition = "proven_equivalent"
            reason = "vanilla_allowlist"
        else:
            disposition = "needs_current_expression"
            reason = "minecraft_prefix_not_proof"
    future_owner = None
    if disposition == "phase_deferred" and combinatorial:
        host = str(family.get("cc_host_map") or "").split(":", 1)[-1]
        future_owner = f"later:combinatorial/{host}"
    elif disposition == "phase_deferred" and kind == "unique_object":
        future_owner = "later:object_expression/" + str(
            mapped.get("unique_kind") or "object"
        )
    b0_ok = True
    if side.endswith("inputs") and identity and disposition == "proven_equivalent":
        b0_ok = identity in b0
        if not b0_ok:
            reason = "b0_unreachable"
    return {
        "b0_reachable": b0_ok if side.endswith("inputs") else None,
        "disposition": disposition,
        "family_id": family["family_id"],
        "future_owner": future_owner,
        "kind": kind,
        "operand_index": index,
        "reason": reason,
        "runtime_id": runtime_id,
        "side": side,
        "source": {
            "fluid": mapped.get("fluid"),
            "form": mapped.get("form"),
            "item": mapped.get("item"),
            "material": mapped.get("material"),
            "meta": mapped.get("meta"),
            "unique_kind": mapped.get("unique_kind"),
        },
        "template_key": family["template_key"],
    }


def _remap_item(item: dict[str, Any], catalogs: dict[str, Any]) -> dict[str, Any]:
    interned_form = item.get("form")
    interned_material = item.get("material")
    interned_alias = item.get("alias")
    remapped = common.map_item_source(item, catalogs)
    if interned_form and remapped.get("form") and interned_form != remapped.get("form"):
        raise ValueError(
            "snapshot interned form drifted from overlay remap: "
            f"{item.get('item')} {interned_form} != {remapped.get('form')}"
        )
    if (
        interned_material
        and remapped.get("material")
        and interned_material != remapped.get("material")
    ):
        raise ValueError(
            "snapshot interned material drifted from overlay remap: "
            f"{item.get('item')} {interned_material} != {remapped.get('material')}"
        )
    if interned_alias and remapped.get("alias") and interned_alias != remapped.get("alias"):
        raise ValueError(
            "snapshot interned alias drifted from overlay remap: "
            f"{item.get('item')} {interned_alias} != {remapped.get('alias')}"
        )
    return {**item, **remapped}


def _map_relation(
    relation: dict[str, Any],
    catalogs: dict[str, Any],
) -> dict[str, Any]:
    mapped = dict(relation)
    mapped["item_inputs"] = [
        _remap_item(item, catalogs) for item in relation.get("item_inputs") or []
    ]
    mapped["item_outputs"] = [
        _remap_item(item, catalogs) for item in relation.get("item_outputs") or []
    ]
    mapped["fluid_inputs"] = [
        {**item, **common.map_fluid_source(item, catalogs)}
        for item in relation.get("fluid_inputs") or []
    ]
    mapped["fluid_outputs"] = [
        {**item, **common.map_fluid_source(item, catalogs)}
        for item in relation.get("fluid_outputs") or []
    ]
    return mapped


def classify_family(
    *,
    family: dict[str, Any],
    relation_summaries: list[dict[str, Any]],
    operand_rows: list[dict[str, Any]],
    published: set[str],
) -> dict[str, Any]:
    combinatorial = family["template_key"] in common.KNOWN_COMBINATORIAL_TEMPLATE_KEYS
    kinds = {row["kind"] for row in operand_rows}
    reasons = {row["reason"] for row in operand_rows}
    unique_objects = sorted(
        {
            f"{row['source'].get('item')}@{row['source'].get('meta')}"
            for row in operand_rows
            if row["kind"] == "unique_object"
        }
    )
    unique_kinds = sorted(
        {
            str(row["source"].get("unique_kind") or "")
            for row in operand_rows
            if row["kind"] == "unique_object" and row["source"].get("unique_kind")
        }
    )
    missing_forms = sorted(
        {
            f"{row['source'].get('material')}/{row['source'].get('form')}"
            for row in operand_rows
            if row["reason"] == "missing_material_form"
        }
    )
    missing_fluids = sorted(
        {
            str(row["source"].get("fluid") or row["source"].get("material") or "")
            for row in operand_rows
            if row["reason"] in {"missing_molten", "unmapped_fluid", "fluid_not_registered"}
        }
    )
    b0_miss = sorted(
        {
            str(row.get("runtime_id") or "")
            for row in operand_rows
            if row.get("b0_reachable") is False
        }
    )
    expressed = sum(
        1 for summary in relation_summaries if summary["identity"] in published
    )
    all_expressed = bool(relation_summaries) and expressed == len(relation_summaries)
    partial_expressed = expressed not in {0, len(relation_summaries)}
    consume_unique = all(
        row["kind"] == "unique_object"
        for row in operand_rows
        if row["side"] == "item_inputs"
    ) and any(row["side"] == "item_inputs" for row in operand_rows)
    produce_forms = all(
        row["kind"] in {"material_form", "missing_form"}
        or row["source"].get("form") in common.RECYCLING_OUTPUT_FORMS
        for row in operand_rows
        if row["side"] == "item_outputs"
    ) and any(row["side"] == "item_outputs" for row in operand_rows)
    recycling = bool(
        unique_objects
        and family["cc_host_map"] in common.MATERIAL_FORM_OUTPUT_HOSTS
        and consume_unique
        and produce_forms
    )
    proven_kinds = {"proven_equivalent"}
    all_proven = bool(operand_rows) and all(
        row["disposition"] in proven_kinds for row in operand_rows
    )
    unmapped = bool(
        kinds
        & {
            "unmapped",
            "unknown",
            "unknown_material",
            "unknown_fluid",
            "unmapped_fluid",
        }
    )
    secondary: list[str] = []
    if partial_expressed:
        secondary.append("partial")
    if b0_miss:
        secondary.append("b0_unreachable_inputs")
    if missing_forms:
        secondary.append("missing_forms")
    if missing_fluids:
        secondary.append("missing_fluids")
    if unique_objects:
        secondary.append("unique_objects")
    if combinatorial:
        secondary.append("combinatorial_template")
    if "minecraft_prefix_not_proof" in reasons:
        secondary.append("unproven_vanilla")
    if unmapped:
        secondary.append("unmapped_operands")
    has_fluid = any(row["side"].startswith("fluid_") for row in operand_rows)
    representation = "exact"
    if int(family.get("relation_count") or len(relation_summaries)) > 1:
        representation = "exact_multi"
    if combinatorial:
        representation = "parameterized_unproven"

    if unique_objects and not unique_kinds:
        raise ValueError(
            f"{family['family_id']} unique_object without unique_kinds"
        )

    if (
        all_expressed
        and not partial_expressed
        and "minecraft_prefix_not_proof" not in reasons
        and not unmapped
    ):
        primary = "already_expressed"
    elif combinatorial:
        primary = "combinatorial_unproven"
    elif unique_objects:
        primary = "needs_unique_block_or_mte"
    elif missing_forms or "missing_molten" in reasons or missing_fluids:
        primary = "needs_prefix_or_molten"
    elif all_proven and not b0_miss and not partial_expressed:
        primary = "current_closure_ready"
    else:
        primary = "needs_unique_block_or_mte"
        if b0_miss and all_proven:
            secondary.append("acquisition_open")
        else:
            secondary.append("residual_not_ready")
            if not unique_kinds and "unmapped_operands" not in secondary:
                secondary.append("unmapped_operands")

    if primary == "current_closure_ready" and (
        b0_miss or partial_expressed or combinatorial
    ):
        raise ValueError(f"{family['family_id']} marked ready with blockers")
    if primary == "already_expressed" and partial_expressed:
        raise ValueError(f"{family['family_id']} partial collision as already_expressed")

    return {
        "b0_unreachable_inputs": b0_miss,
        "cc_host_map": family["cc_host_map"],
        "evidence_root_sha256": common.sha256_stable(
            {
                "family_id": family["family_id"],
                "identities": [row["identity"] for row in relation_summaries],
                "reasons": sorted(reasons),
            }
        ),
        "family_id": family["family_id"],
        "fluid_reachability_approximate": has_fluid,
        "host": family["cc_host_map"],
        "lossy_aliases": sorted(
            reason
            for reason in reasons
            if "lossy" in reason or reason == "minecraft_prefix_not_proof"
        ),
        "membership_kind": family["membership_kind"],
        "missing_fluids": [value for value in missing_fluids if value],
        "missing_forms": missing_forms,
        "missing_runtime_ids": sorted(
            {
                f"{row['side']}:{row['source'].get('item') or row['source'].get('fluid')}"
                for row in operand_rows
                if not row.get("runtime_id")
                and row["kind"]
                not in {"empty", "unique_object", "missing_form", "missing_molten"}
            }
        ),
        "primary_bucket": primary,
        "published_relation_matches": expressed,
        "recycling_candidate": recycling,
        "relation_count": len(relation_summaries),
        "representation_status": representation,
        "secondary_blockers": sorted(set(secondary)),
        "source_revision": common.SOURCE_REVISION,
        "template_key": family["template_key"],
        "unique_kinds": unique_kinds,
        "unique_objects": unique_objects,
        "unsupported_semantics": sorted(
            reason
            for reason in reasons
            if reason
            in {"unmapped", "unknown", "unknown_fluid", "unknown_material", "unmapped_fluid"}
        ),
    }


def _compact_disposition(operand_rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    grouped: dict[tuple[Any, ...], dict[str, Any]] = {}
    for row in operand_rows:
        key = (
            row["kind"],
            row["disposition"],
            row["reason"],
            row["source"].get("item"),
            row["source"].get("meta"),
            row["source"].get("fluid"),
            row["source"].get("form"),
            row["source"].get("material"),
            row.get("runtime_id"),
        )
        entry = grouped.get(key)
        if entry is None:
            entry = {
                "b0_reachable": row.get("b0_reachable"),
                "disposition": row["disposition"],
                "family_count": 0,
                "family_ids": [],
                "future_owner": row.get("future_owner"),
                "kind": row["kind"],
                "reason": row["reason"],
                "runtime_id": row.get("runtime_id"),
                "source": row["source"],
            }
            grouped[key] = entry
        family_id = row["family_id"]
        if family_id not in entry["family_ids"]:
            entry["family_ids"].append(family_id)
            entry["family_count"] += 1
    rows = []
    for entry in grouped.values():
        entry["family_ids"] = sorted(entry["family_ids"])[:20]
        rows.append(entry)
    rows.sort(
        key=lambda row: (
            row["kind"],
            str(row["source"].get("item") or row["source"].get("fluid") or ""),
            str(row["source"].get("meta") or ""),
        )
    )
    return rows


def build(*, snapshot: dict[str, Any] | None = None) -> dict[str, Any]:
    if snapshot is None:
        if not common.FAMILY_OPERAND_SNAPSHOT.is_file():
            raise FileNotFoundError("T42 family operand snapshot is required")
        snapshot = common.load_json(common.FAMILY_OPERAND_SNAPSHOT)
    catalogs = common.load_runtime_catalogs()
    b0 = _b0()
    allowlist = _allowlist()
    registry = _registry_ids(catalogs)
    published_by_host = common.published_relation_identities_by_host()
    operand_rows: list[dict[str, Any]] = []
    overlay_rows: list[dict[str, Any]] = []
    grouped: dict[str, dict[str, Any]] = {}
    for family, relation in iter_snapshot_relations(snapshot):
        grouped.setdefault(
            family["family_id"],
            {"family": family, "operand_rows": [], "summaries": []},
        )
        mapped_relation = _map_relation(relation, catalogs)
        identity = common.logical_relation_identity(mapped_relation)
        grouped[family["family_id"]]["summaries"].append({"identity": identity})
        combinatorial = family["template_key"] in common.KNOWN_COMBINATORIAL_TEMPLATE_KEYS
        for side in ("item_inputs", "item_outputs", "fluid_inputs", "fluid_outputs"):
            for index, operand in enumerate(mapped_relation.get(side) or []):
                row = _operand_row(
                    family,
                    operand,
                    side=side,
                    index=index,
                    b0=b0,
                    allowlist=allowlist,
                    registry=registry,
                    combinatorial=combinatorial,
                )
                grouped[family["family_id"]]["operand_rows"].append(row)
                operand_rows.append(row)
    missing_primary: list[str] = []
    for payload in grouped.values():
        row = classify_family(
            family=payload["family"],
            relation_summaries=payload["summaries"],
            operand_rows=payload["operand_rows"],
            published=published_by_host.get(payload["family"]["cc_host_map"], set()),
        )
        if row["primary_bucket"] not in common.PRIMARY_BUCKETS:
            missing_primary.append(row["family_id"])
        overlay_rows.append(row)
    overlay_rows.sort(key=lambda row: row["family_id"])
    if missing_primary:
        raise ValueError(
            "T42 overlay unclassified families: " + ", ".join(missing_primary[:20])
        )
    buckets = Counter(row["primary_bucket"] for row in overlay_rows)
    if set(buckets) - set(common.PRIMARY_BUCKETS):
        raise ValueError(f"unknown primary buckets: {sorted(set(buckets))}")
    if len(overlay_rows) != int(snapshot.get("family_count") or 0):
        raise ValueError("overlay family count drifted from snapshot")
    unique_kind_counts: Counter[str] = Counter()
    for row in overlay_rows:
        for kind in row.get("unique_kinds") or []:
            unique_kind_counts[str(kind)] += 1
    unique_bucket = int(buckets.get("needs_unique_block_or_mte") or 0)
    unique_with_kind = sum(
        1
        for row in overlay_rows
        if row["primary_bucket"] == "needs_unique_block_or_mte" and row.get("unique_kinds")
    )
    unique_without_kind = unique_bucket - unique_with_kind
    mte_count = int(unique_kind_counts.get("mte") or 0)
    if unique_bucket and mte_count == unique_bucket:
        raise ValueError(
            "unique_kind_counts[mte] must not equal the unique-object bucket; "
            "MTE is not the residual catch-all"
        )
    if unique_with_kind + unique_without_kind != unique_bucket:
        raise ValueError("unique residual split drifted from unique-bucket family count")
    operand_counts = Counter(row["disposition"] for row in operand_rows)
    compact_rows = _compact_disposition(operand_rows)
    overlay = {
        "bucket_counts": dict(sorted(buckets.items())),
        "family_count": len(overlay_rows),
        "families": overlay_rows,
        "generated_by": "python tools/build_t42_blocker_overlay.py",
        "note": (
            "needs_unique_block_or_mte is a residual bucket, not 'all independent "
            "objects' and not 'all MTE'. unique_kind_counts.mte counts families "
            "whose unique_kinds include mte. Empty unique_kinds stay residual "
            "(unmapped / unproven vanilla / B0 / residual_not_ready). "
            "gregtech:gt.stone.andesite and gt.armor.hazmat.* are unmapped; "
            "prefix_item_to_gt_prefix does not cover those IDs. "
            "cruciblecraft:programmed_circuit is a mapping, not T35/B0 proof."
        ),
        "opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "partial_family_count": sum(
            1 for row in overlay_rows if "partial" in row["secondary_blockers"]
        ),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_BLOCKER_OVERLAY",
        "unique_bucket_family_count": unique_bucket,
        "unique_kind_counts": dict(sorted(unique_kind_counts.items())),
        "unique_object_kind_family_count": unique_with_kind,
        "unique_residual_without_kind_count": unique_without_kind,
    }
    if any(
        row["primary_bucket"] == "already_expressed"
        and "partial" in row["secondary_blockers"]
        for row in overlay_rows
    ):
        raise ValueError("partial families cannot be already_expressed")
    disposition = {
        "counts": dict(sorted(operand_counts.items())),
        "generated_by": "python tools/build_t42_blocker_overlay.py",
        "occurrence_count": len(operand_rows),
        "operand_count": len(compact_rows),
        "rows": compact_rows,
        "schema_version": 1,
        "status": "T42_OPERAND_DISPOSITION",
    }
    return {
        "candidates": _wave_candidates(overlay_rows),
        "disposition": disposition,
        "overlay": overlay,
    }


def _wave_candidates(rows: list[dict[str, Any]]) -> dict[str, Any]:
    by_bucket: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        by_bucket[row["primary_bucket"]].append(row)

    def sort_key(row: dict[str, Any]) -> tuple[Any, ...]:
        host_rank = (
            common.HOST_SORT_ORDER.index(row["host"])
            if row["host"] in common.HOST_SORT_ORDER
            else 99
        )
        b0 = 0 if not row["b0_unreachable_inputs"] else 1
        representation = 0 if row["representation_status"].startswith("exact") else 1
        return (host_rank, b0, representation, row["relation_count"], row["family_id"])

    bucket_stats = {}
    for bucket, families in by_bucket.items():
        ordered = sorted(families, key=sort_key)
        largest = max(ordered, key=lambda row: row["relation_count"])
        bucket_stats[bucket] = {
            "family_count": len(ordered),
            "largest_family_id": largest["family_id"],
            "largest_relation_count": largest["relation_count"],
            "row_count": sum(row["relation_count"] for row in ordered),
        }
    drying_ready = [
        row["family_id"]
        for row in sorted(by_bucket.get("current_closure_ready") or [], key=sort_key)
        if row["host"] == "cruciblecraft:drying"
    ]
    return {
        "authoritative": False,
        "buckets": bucket_stats,
        "generated_by": "python tools/build_t42_blocker_overlay.py",
        "note": (
            "Sort-only. T43 should first inspect cruciblecraft:drying "
            "current_closure_ready; this is not a topology assignment. "
            "T35 drying host total is 152, not the ready-slice size. "
            "Programmed-circuit mapping is not expression proof; autoclave/"
            "mixer circuit slots stay needs_current_expression. Unique-bucket "
            "families with empty unique_kinds are residual, not independent objects."
        ),
        "priority_intent": {
            "host": "cruciblecraft:drying",
            "ready_family_count": len(drying_ready),
            "ready_family_ids": drying_ready,
            "t35_host_family_total": 152,
        },
        "schema_version": 1,
        "sort": "runnable_host,b0_closure,representation,relation_count,family_id",
        "status": "T42_WAVE_CANDIDATES",
    }


def write() -> None:
    documents = build()
    t35.write_stable(OUTPUT, documents["overlay"])
    t35.write_stable(DISPOSITION, documents["disposition"])
    t35.write_stable(CANDIDATES, documents["candidates"])


def check() -> list[str]:
    documents = build()
    errors = []
    errors.extend(common.check_document(OUTPUT, documents["overlay"]))
    errors.extend(common.check_document(DISPOSITION, documents["disposition"]))
    errors.extend(common.check_document(CANDIDATES, documents["candidates"]))
    return errors


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Classify T42 remaining ordinary families", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42 blocker overlay, operand disposition, and wave candidates.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 blocker overlay is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42 blocker overlay failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
