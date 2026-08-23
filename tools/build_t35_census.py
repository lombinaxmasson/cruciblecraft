#!/usr/bin/env python3
"""Build and check T35 aggregate census (R7).

Consumes foundation artifacts (inputs, runtime registry, exclusion reclaim,
recipe families, load baseline) and emits a compact disposition/priority census
with runtime bijection indexes, overlays, work sets, and validators.
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import build_t35_census_inputs as census_inputs_builder  # noqa: E402
from tools import build_t35_excluded_object_reclaim as exclusion_builder  # noqa: E402
from tools import build_t35_load_baseline as load_baseline_builder  # noqa: E402
from tools import build_t35_machine_track as machine_track_builder  # noqa: E402
from tools import build_t35_recipe_families as recipe_families_builder  # noqa: E402
from tools import build_t35_runtime_registry as runtime_registry_builder  # noqa: E402
from tools import t27_common as common  # noqa: E402
from tools import t35_common as t35  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t35.CENSUS
T27_PORTFOLIO = t35.TOOLS / "t27_portfolio"
PRESENTATION = t35.TOOLS / "t34_presentation.json"
ART_MANIFEST = t35.TOOLS / "t34_gt6_art_manifest.json"
WORLDGEN = t35.TOOLS / "worldgen_catalog_readiness.json"
REACHABILITY = t35.TOOLS / "t21_operand_reachability.json"

SOURCE_REVISION = t35.SOURCE_REVISION
PUBLICATION_DELTA = t35.PUBLICATION_DELTA
PENDING_LOAD_VERDICT = t35.PENDING_LOAD_VERDICT

STORAGE_TRACK_FAMILIES = frozenset(
    exclusion_builder.STORAGE_CORE_FAMILIES
    + ("mass_storage_logistics",)
)
STORAGE_PLANNED_P1 = frozenset(exclusion_builder.STORAGE_CORE_FAMILIES)
IMPLEMENTED_EXCLUSION_FAMILIES = frozenset({"hopper"})
T37_EXCLUDED_HOSTS = frozenset(
    {
        "cruciblecraft:bath",
        "cruciblecraft:smelter",
        "cruciblecraft:mixer",
    }
)
T37_ROW_MIN = 50
T37_ROW_MAX = 200

FOUNDATION_ARTIFACTS: tuple[tuple[Path, Any], ...] = (
    (t35.INPUTS, census_inputs_builder),
    (t35.RUNTIME_REGISTRY, runtime_registry_builder),
    (t35.EXCLUSION_RECLAIM, exclusion_builder),
    (t35.RECIPE_FAMILIES, recipe_families_builder),
    (t35.LOAD_BASELINE, load_baseline_builder),
    (t35.MACHINE_TRACK, machine_track_builder),
)


def _pending_load(evidence: str) -> dict[str, Any]:
    return {
        "status": "pending",
        "verdict": PENDING_LOAD_VERDICT,
        "evidence": evidence,
    }


def _axes(
    *,
    closure_status: str,
    closure_evidence: str,
    fidelity_status: str,
    fidelity_evidence: str,
    load: dict[str, Any],
) -> dict[str, Any]:
    return {
        "closure": {"status": closure_status, "evidence": closure_evidence},
        "fidelity": {"status": fidelity_status, "evidence": fidelity_evidence},
        "load": load,
    }


def _dependency(kind: str, ident: str) -> dict[str, str]:
    if kind not in t35.DEPENDENCY_KINDS:
        raise ValueError(f"invalid dependency kind: {kind}")
    return {"kind": kind, "id": ident}


def _validate_owner(owner: str) -> None:
    if not owner or owner.strip().lower() in t35.load_json(t35.POLICY)["owner_rules"][
        "forbidden_tokens"
    ]:
        raise ValueError(f"invalid owner: {owner!r}")


def _load_t27_table(table: str) -> dict[str, dict[str, Any]]:
    path = T27_PORTFOLIO / f"{table}.json"
    if not path.is_file():
        return {}
    document = common.load_json(path)
    return {
        str(row.get("canonical_id") or ""): row
        for row in document.get("records") or []
        if isinstance(row, dict)
    }


def _cc_impl_index() -> dict[str, list[str]]:
    index: dict[str, list[str]] = defaultdict(list)
    for table in common.TABLE_SPECS:
        t27_rows = _load_t27_table(table)
        for ident, row in t27_rows.items():
            impl = row.get("cc_implementation")
            if not isinstance(impl, str) or not impl.strip() or impl == "none":
                continue
            if impl.startswith("aliases:") or impl.startswith("related_host:"):
                continue
            key = impl if ":" in impl else f"cruciblecraft:{impl}"
            index[key].append(f"t13/{table}/{ident}")
    for family in STORAGE_TRACK_FAMILIES:
        index.setdefault(f"exclusion:{family}", []).append(f"exclusion/{family}")
    index.setdefault("exclusion/hopper", []).append("exclusion/hopper")
    return {key: sorted(set(values)) for key, values in sorted(index.items())}


def _validate_foundations() -> dict[str, Any]:
    checks: dict[str, Any] = {"artifact_checks": {}, "publication_delta": PUBLICATION_DELTA}
    for path, module in FOUNDATION_ARTIFACTS:
        rel = common.relative(path)
        if not path.is_file():
            raise FileNotFoundError(f"missing foundation artifact: {rel}")
        errors = t35.compact_check(module)
        checks["artifact_checks"][rel] = {
            "status": "pass" if not errors else "fail",
            "errors": errors,
        }
        if errors:
            raise ValueError(f"foundation check failed for {rel}: {'; '.join(errors)}")
    inputs_doc = common.load_json(t35.INPUTS)
    if inputs_doc.get("publication_delta") != PUBLICATION_DELTA:
        raise ValueError("census inputs publication_delta must remain 0/0/0")
    checks["source_hashes"] = {
        rel: common.sha256_file(path) for path, _ in FOUNDATION_ARTIFACTS
    }
    checks["inputs_policy_sha256"] = inputs_doc.get("policy_sha256")
    return checks


def _select_t37_pilot(families: list[dict[str, Any]]) -> dict[str, Any]:
    playable_hosts = sorted(
        {
            family["cc_host_map"]
            for family in families
            if family.get("host", {}).get("status") == "registered_playable"
            and family["cc_host_map"] not in T37_EXCLUDED_HOSTS
        }
    )
    for host in playable_hosts:
        host_families = sorted(
            [
                family
                for family in families
                if family["cc_host_map"] == host
                and family.get("host", {}).get("status") == "registered_playable"
            ],
            key=lambda row: row["family_id"],
        )
        picked: list[dict[str, Any]] = []
        total = 0
        for family in host_families:
            count = int(family.get("expanded_count") or 0)
            if total + count > T37_ROW_MAX:
                continue
            picked.append(family)
            total += count
            if total >= T37_ROW_MIN:
                return {
                    "fixed_card": "T37",
                    "host_map": host,
                    "family_ids": [row["family_id"] for row in picked],
                    "source_rows": total,
                    "selection_method": (
                        "deterministic_bundle: first playable non-bath/smelter/mixer "
                        "host by cc_host_map; accumulate families by family_id until "
                        f"source_rows in [{T37_ROW_MIN}, {T37_ROW_MAX}]"
                    ),
                }
    raise ValueError("no deterministic T37 pilot bundle found")


def _classify_t13(
    table: str,
    t13_row: dict[str, Any],
    t27_row: dict[str, Any] | None,
    spec: dict[str, str],
) -> dict[str, Any]:
    ident = str(t13_row.get(spec["id_field"]) or "")
    t13_class = str(t13_row.get(spec["classification_field"]) or "")
    t27_disposition = (t27_row or {}).get("disposition")
    t27_impl = (t27_row or {}).get("cc_implementation")
    closure = ((t27_row or {}).get("axes") or {}).get("closure", {}).get("status")
    canonical_id = f"t13/{table}/{ident}"
    owner = (t27_row or {}).get("owner") or f"portfolio:t13/{table}"
    reason = (t27_row or {}).get("reason") or (
        f"T13 {t13_class} canonical identity on fixed revision snapshot."
    )
    replacement = (t27_row or {}).get("replacement_condition") or (
        "Machine-verifiable host, acquisition, and load evidence required before closure."
    )
    recheck = (t27_row or {}).get("recheck_point") or "T35 aggregate census recheck"
    dependencies = list((t27_row or {}).get("dependencies") or [])

    runtime_ids: list[str] = []
    if isinstance(t27_impl, str) and t27_impl.strip() and t27_impl not in {
        "none",
    } and not t27_impl.startswith(("aliases:", "related_host:")):
        runtime_key = t27_impl if ":" in t27_impl else f"cruciblecraft:{t27_impl}"
        runtime_ids = [runtime_key]

    if t27_disposition == "out_of_scope":
        disposition = "out_of_scope"
        priority = "P3"
    elif isinstance(t27_impl, str) and t27_impl.strip() not in {"", "none"} and (
        closure == "closed" or t27_disposition == "v1_required"
    ):
        disposition = "implemented"
        priority = "P0" if t27_disposition == "v1_required" else "P1"
    elif t27_disposition == "v1_required":
        disposition = "planned"
        priority = "P0"
        dependencies.append(_dependency("fixed_card", "T36"))
    elif t27_disposition == "post_1_0":
        disposition = "deferred"
        priority = "P3"
    else:
        disposition = "deferred"
        priority = "P2"

    if disposition == "planned" and priority == "P2":
        priority = "P3"

    load = ((t27_row or {}).get("axes") or {}).get("load") or _pending_load(
        "T13 identity load requires bounded measurement; no zero fill."
    )
    if load.get("status") == "pending":
        load = dict(load)
        load.setdefault("verdict", PENDING_LOAD_VERDICT)

    return {
        "domain": "t13",
        "canonical_id": canonical_id,
        "table": table,
        "source": {
            "revision": SOURCE_REVISION,
            "artifact": spec["artifact"],
            "record_key": ident,
        },
        "historical_classification": {
            "t13": t13_class,
            "t27": t27_disposition,
        },
        "cc_runtime": {
            "status": "registered" if runtime_ids else "not_registered",
            "ids": runtime_ids,
            "evidence": [spec["artifact"]],
        },
        "disposition": disposition,
        "portfolio_priority": priority,
        "reason": reason,
        "owner": owner,
        "dependencies": dependencies,
        "replacement_condition": replacement,
        "recheck_point": recheck,
        "axes": _axes(
            closure_status=(closure or "incomplete"),
            closure_evidence=(
                ((t27_row or {}).get("axes") or {}).get("closure", {}).get("evidence")
                or "T27 portfolio closure overlay."
            ),
            fidelity_status=(
                ((t27_row or {}).get("axes") or {}).get("fidelity", {}).get("status")
                or "source_backed"
            ),
            fidelity_evidence=(
                ((t27_row or {}).get("axes") or {}).get("fidelity", {}).get("evidence")
                or spec["artifact"]
            ),
            load=load,
        ),
    }


def _classify_exclusion(summary: dict[str, Any]) -> dict[str, Any]:
    family = str(summary["canonical_family"])
    canonical_id = f"exclusion/{family}"
    sites = int(summary["source_sites"])
    expanded = int(summary["expanded_multiplicity"])
    is_storage_track = family in STORAGE_TRACK_FAMILIES
    is_logistics_cross = family == "mass_storage_logistics"

    if family in IMPLEMENTED_EXCLUSION_FAMILIES:
        disposition = "implemented"
        priority = "P1"
        owner = f"portfolio:exclusion/{family}"
        reason = (
            "T30 hopper source evidence maps GT6 Hoppers exclusion sites to playable "
            "CC hopper/queue_hopper/dust_funnel runtime without duplicating source sites."
        )
        replacement = "Already hosted by T30 bounded logistics contract."
        recheck = "T37 does not own hopper exclusion closure."
        runtime_status = "registered"
        runtime_evidence = ["tools/t30_hopper_source_policy.json"]
        closure = "closed"
    elif family in STORAGE_PLANNED_P1:
        disposition = "planned"
        priority = "P1"
        owner = f"portfolio:storage/{family}"
        reason = (
            "Source-backed storage family on the announced 1.x storage track; retains "
            f"exact lineage ({sites} sites / {expanded} expanded)."
        )
        replacement = (
            "Future storage-track card must preserve canonical family boundary and "
            "source multiplicity; not equivalent to generic chests or material storage."
        )
        recheck = "generated T38+ storage-track card"
        runtime_status = "not_registered"
        runtime_evidence = ["tools/t35_excluded_object_reclaim.json"]
        closure = "incomplete"
    elif is_logistics_cross:
        disposition = "planned"
        priority = "P1"
        owner = "portfolio:storage/mass_storage_logistics"
        reason = (
            "Cross-category Logistics storage behavior adjacent to Storage 28/624; "
            "included on storage track without inflating Storage 624 denominator."
        )
        replacement = "Storage-track card must link logistics mass storage separately."
        recheck = "generated T38+ storage-track card"
        runtime_status = "not_registered"
        runtime_evidence = ["tools/t35_excluded_object_reclaim.json"]
        closure = "incomplete"
    elif family.startswith(("compat/", "reactor/")):
        disposition = "out_of_scope"
        priority = "P3"
        owner = f"portfolio:exclusion/{family}"
        reason = "Compatibility wrapper or reactor part outside fixed machine/energy tree."
        replacement = "No 1.x replacement; retain source lineage only."
        recheck = "post-1.x product review"
        runtime_status = "not_registered"
        runtime_evidence = ["tools/t35_excluded_object_reclaim.json"]
        closure = "not_applicable"
    elif family in {"fluid_container", "chest", "safe", "tank", "sorting", "pump"}:
        disposition = "deferred"
        priority = "P2"
        owner = f"portfolio:exclusion/{family}"
        reason = (
            f"Adjacent storage/logistics exclusion family ({sites}/{expanded}) "
            "requires storage-track cross-check before admission."
        )
        replacement = "Storage-track card or explicit out-of-scope supersession required."
        recheck = "P2 admission after cumulative load measurement"
        runtime_status = "not_registered"
        runtime_evidence = ["tools/t35_excluded_object_reclaim.json"]
        closure = "incomplete"
    else:
        disposition = "deferred"
        priority = "P3"
        owner = f"portfolio:exclusion/{family}"
        reason = (
            f"Excluded registration family ({sites} sites / {expanded} expanded) "
            "awaits explicit 1.x disposition route."
        )
        replacement = "Owner card must cite source sites and expanded multiplicity."
        recheck = "generated T38+ card or explicit out-of-scope supersession"
        runtime_status = "not_registered"
        runtime_evidence = ["tools/t35_excluded_object_reclaim.json"]
        closure = "incomplete"

    dependencies: list[dict[str, str]] = []
    if disposition == "planned" and family in STORAGE_TRACK_FAMILIES:
        dependencies.append(_dependency("fixed_card", "T36"))
    if priority == "P2":
        p2_admission = "BLOCKED_PENDING_MEASUREMENT"
    else:
        p2_admission = None

    record: dict[str, Any] = {
        "domain": "exclusion",
        "canonical_id": canonical_id,
        "source": {
            "revision": SOURCE_REVISION,
            "artifact": "tools/t35_excluded_object_reclaim.json",
            "source_sites": sites,
            "expanded_rows": expanded,
            "record_keys": [],
        },
        "historical_classification": {
            "t13": "excluded_before_classification",
            "t27": None,
        },
        "cc_runtime": {
            "status": runtime_status,
            "ids": [],
            "evidence": runtime_evidence,
        },
        "disposition": disposition,
        "portfolio_priority": priority,
        "reason": reason,
        "owner": owner,
        "dependencies": dependencies,
        "replacement_condition": replacement,
        "recheck_point": recheck,
        "axes": _axes(
            closure_status=closure,
            closure_evidence="tools/t35_excluded_object_reclaim.json",
            fidelity_status="source_backed",
            fidelity_evidence="763/1701 exclusion reclaim lineage",
            load=_pending_load(
                "Exclusion family load requires bounded storage-track measurement."
            ),
        ),
    }
    if p2_admission is not None:
        record["p2_admission"] = p2_admission
    return record


def _load_storage_scope() -> dict[str, Any]:
    if not t35.STORAGE_SCOPE.is_file():
        raise FileNotFoundError(t35.relative(t35.STORAGE_SCOPE))
    document = t35.load_json(t35.STORAGE_SCOPE)
    if document.get("status") != "T35_STORAGE_SCOPE":
        raise ValueError("storage scope status must be T35_STORAGE_SCOPE")
    if document.get("source_revision") != SOURCE_REVISION:
        raise ValueError("storage scope source revision drifted")
    denominators = document.get("denominators") or {}
    storage = denominators.get("storage") or {}
    logistics = denominators.get("mass_storage_logistics") or {}
    if storage != {
        "source_sites": 28,
        "expanded_rows": 624,
        "counts_toward_storage_624": True,
    }:
        raise ValueError("storage scope must preserve Storage 28/624")
    if logistics != {
        "source_sites": 1,
        "expanded_rows": 1,
        "counts_toward_storage_624": False,
    }:
        raise ValueError("storage scope must preserve logistics 1/1 outside Storage 624")
    required = {
        "bookshelf",
        "bottle_crate",
        "drawer",
        "locker",
        "mass_storage_barrel",
        "mass_storage_box",
        "mass_storage_standard",
        "storage_inserter",
        "mass_storage_logistics",
        "hopper",
        "chest",
        "safe",
        "tank",
        "fluid_container",
        "pump",
        "sorting",
    }
    families = document.get("families")
    if not isinstance(families, dict) or set(families) != required:
        raise ValueError("storage scope families must exactly cover T35R declared families")
    for family, row in families.items():
        if row.get("portfolio_scope") not in t35.PORTFOLIO_SCOPES:
            raise ValueError(f"storage scope {family} has invalid portfolio_scope")
        if row.get("disposition") not in t35.DISPOSITIONS:
            raise ValueError(f"storage scope {family} has invalid disposition")
        if row.get("portfolio_priority") not in t35.PRIORITIES:
            raise ValueError(f"storage scope {family} has invalid portfolio_priority")
        if not str(row.get("owner") or "").strip():
            raise ValueError(f"storage scope {family} missing owner")
    locker = families["locker"]
    folded = locker.get("folded_source_behaviors") or []
    charging = next(
        (row for row in folded if row.get("behavior_class") == "MultiTileEntityLockerCharging"),
        None,
    )
    if not isinstance(charging, dict) or not str(charging.get("behavior_diff") or "").strip():
        raise ValueError("storage scope locker missing charging behavior_diff")
    return document


def _apply_storage_scope(
    record: dict[str, Any],
    storage_scope: dict[str, Any],
) -> dict[str, Any]:
    canonical_id = str(record["canonical_id"])
    if not canonical_id.startswith("exclusion/"):
        return record
    family = canonical_id.split("/", 1)[1]
    policy = (storage_scope.get("families") or {}).get(family)
    if not isinstance(policy, dict):
        return record
    updated = dict(record)
    for key in ("disposition", "portfolio_priority", "owner"):
        if updated.get(key) != policy.get(key):
            raise ValueError(
                f"storage scope {family} {key}={policy.get(key)!r} "
                f"does not match census classification {updated.get(key)!r}"
            )
    if policy.get("reason"):
        updated["reason"] = policy["reason"]
    updated["storage_scope"] = {
        "portfolio_scope": policy["portfolio_scope"],
        "scope_reason": policy.get("reason") or updated["reason"],
    }
    if policy.get("folded_source_behaviors"):
        updated["storage_scope"]["folded_source_behaviors"] = policy[
            "folded_source_behaviors"
        ]
    if policy.get("subsequent_track"):
        updated["subsequent_track"] = policy["subsequent_track"]
    return updated


def _classify_recipe(
    family: dict[str, Any],
    *,
    t37_ids: set[str],
) -> dict[str, Any]:
    family_id = str(family["family_id"])
    expanded = int(family.get("expanded_count") or 0)
    host = family.get("host") or {}
    host_map = str(family.get("cc_host_map") or "")
    host_status = str(host.get("status") or "missing")

    if family_id in t37_ids:
        disposition = "planned"
        priority = "P1"
        owner = "portfolio:track_a/t37_pilot"
        reason = (
            "Deterministic T37 ordinary_optional pilot family with playable host and "
            f"bounded {expanded} source rows."
        )
        replacement = "T37 must measure source→authored/logical/eager/lazy on this family only."
        recheck = "fixed card T37 closure"
        dependencies = [_dependency("fixed_card", "T37")]
        p2_admission = None
    elif host_map in T37_EXCLUDED_HOSTS:
        disposition = "deferred"
        priority = "P3"
        owner = "portfolio:track_a/deferred_large_map"
        reason = (
            f"ordinary_optional family on deferred large map host {host_map}; "
            "not eligible as first pilot."
        )
        replacement = "Admit only through generated track after host-level measurement."
        recheck = "generated T38+ recipe-track card"
        dependencies = []
        p2_admission = None
    elif host_status == "registered_playable":
        disposition = "deferred"
        priority = "P2"
        owner = f"portfolio:track_a/{host_map}"
        reason = (
            "Optional recipe family on playable host; P2 admission blocked pending "
            "cumulative soft-budget measurement."
        )
        replacement = "Generated recipe-track card after P2 admission under soft budgets."
        recheck = "P2 admission recheck against T14 axes"
        dependencies = [_dependency("fixed_card", "T37")]
        p2_admission = "BLOCKED_PENDING_MEASUREMENT"
    else:
        disposition = "out_of_scope"
        priority = "P3"
        owner = "portfolio:track_a/missing_host"
        reason = "ordinary_optional family without playable CC host on current overlay."
        replacement = "Requires host implementation before recipe-family closure."
        recheck = "host availability recheck"
        dependencies = []
        p2_admission = None

    return {
        "domain": "recipe",
        "canonical_id": family_id,
        "source": {
            "revision": SOURCE_REVISION,
            "artifact": "tools/t35_recipe_families.json",
            "source_sites": 0,
            "expanded_rows": expanded,
            "record_keys": [family.get("template_key") or family.get("template_fingerprint")],
        },
        "historical_classification": {
            "t13": None,
            "t27": "track_a/ordinary_optional",
        },
        "cc_runtime": {
            "status": "registered" if host_status == "registered_playable" else "missing",
            "ids": [host_map] if host_map else [],
            "evidence": ["tools/t22_5_machine_playability.json"],
        },
        "disposition": disposition,
        "portfolio_priority": priority,
        "reason": reason,
        "owner": owner,
        "dependencies": dependencies,
        "replacement_condition": replacement,
        "recheck_point": recheck,
        "p2_admission": p2_admission,
        "axes": _axes(
            closure_status="incomplete",
            closure_evidence="Recipe family closure requires pilot or admitted P2 card.",
            fidelity_status="source_backed",
            fidelity_evidence=family.get("membership_kind") or "semantic_template",
            load=_pending_load(
                "Recipe family load interval requires pilot measurement; no zero fill."
            ),
        ),
    }


def _runtime_local_identity(
    key: str,
    *,
    domain: str,
    reason: str,
    owner: str,
    runtime_ids: list[str],
) -> dict[str, Any]:
    return {
        "domain": domain,
        "canonical_id": key,
        "source": {
            "revision": SOURCE_REVISION,
            "artifact": "tools/t35_runtime_registry.json",
            "source_sites": 0,
            "expanded_rows": len(runtime_ids),
            "record_keys": runtime_ids[:3],
        },
        "historical_classification": {"t13": None, "t27": None},
        "cc_runtime": {
            "status": "registered",
            "ids": sorted(runtime_ids),
            "evidence": ["tools/t35_runtime_registry.json"],
        },
        "disposition": "implemented",
        "portfolio_priority": "P3",
        "reason": reason,
        "owner": owner,
        "dependencies": [],
        "replacement_condition": "Local runtime catalog identity; not a GT6 source family.",
        "recheck_point": "runtime registry bidirectional gate",
        "axes": _axes(
            closure_status="closed",
            closure_evidence="Present in expected runtime registry.",
            fidelity_status="design_policy",
            fidelity_evidence="CC local/generated runtime identity",
            load={"status": "not_applicable", "evidence": "Registry count axis only."},
        ),
    }


def _material_id_from_runtime(runtime_id: str) -> str:
    path = runtime_id.split(":", 1)[1]
    return path.split("/", 1)[0]


def _presentation_overlay() -> dict[str, str]:
    if not PRESENTATION.is_file():
        return {}
    document = common.load_json(PRESENTATION)
    overlay: dict[str, str] = {}
    for runtime_id, row in (document.get("presentation") or {}).items():
        if isinstance(row, dict):
            overlay[str(runtime_id)] = str(row.get("classification") or "ART_UNKNOWN")
    for member in (document.get("scope") or {}).get("members") or []:
        if isinstance(member, dict) and member.get("id"):
            overlay.setdefault(
                str(member["id"]),
                str(member.get("classification") or "ART_UNKNOWN"),
            )
    return overlay


def _worldgen_overlay() -> dict[str, str]:
    if not WORLDGEN.is_file():
        return {}
    document = common.load_json(WORLDGEN)
    scatter = document.get("surface_scatter") or {}
    return {
        str(scatter.get("configured_feature") or ""): "runtime_placed",
        str(scatter.get("placed_feature") or ""): "runtime_placed",
    }


def _attach_overlays(
    record: dict[str, Any],
    *,
    presentation: dict[str, str],
    worldgen: dict[str, str],
) -> None:
    runtime_ids = (record.get("cc_runtime") or {}).get("ids") or []
    art = [
        presentation[runtime_id]
        for runtime_id in runtime_ids
        if runtime_id in presentation
    ]
    wg = [
        worldgen[runtime_id]
        for runtime_id in runtime_ids
        if runtime_id in worldgen
    ]
    record["overlays"] = {
        "worldgen": wg[0] if wg else "not_applicable",
        "acquisition": "reachable" if record.get("disposition") == "implemented" else "deferred_dependency",
        "presentation": art[0] if art else "ART_PLACEHOLDER",
    }


def _validate_dependencies(identities: dict[str, dict[str, Any]]) -> list[str]:
    errors: list[str] = []
    for canonical_id, record in identities.items():
        for dep in record.get("dependencies") or []:
            kind = dep.get("kind")
            target = str(dep.get("id") or "")
            if kind == "fixed_card" and target not in t35.FIXED_CARDS:
                errors.append(f"{canonical_id} invalid fixed_card {target}")
            elif kind == "canonical_family" and target not in identities:
                errors.append(f"{canonical_id} missing canonical_family {target}")
            elif kind == "open_item" and not target:
                errors.append(f"{canonical_id} blank open_item dependency")
            elif kind == "runtime_capability" and not target:
                errors.append(f"{canonical_id} blank runtime_capability dependency")
    return errors


def _detect_cycles(edges: list[tuple[str, str]]) -> list[str]:
    graph: dict[str, list[str]] = defaultdict(list)
    for src, dst in edges:
        graph[src].append(dst)
    visiting: set[str] = set()
    visited: set[str] = set()
    cycles: list[str] = []

    def dfs(node: str, stack: list[str]) -> None:
        if node in visiting:
            cycles.append(" -> ".join(stack + [node]))
            return
        if node in visited:
            return
        visiting.add(node)
        for nxt in graph[node]:
            dfs(nxt, stack + [node])
        visiting.remove(node)
        visited.add(node)

    for node in sorted(graph):
        dfs(node, [])
    return cycles


def build() -> dict[str, Any]:
    foundation = _validate_foundations()
    policy = t35.load_json(t35.POLICY)
    storage_scope = _load_storage_scope()
    inputs_doc = t35.load_json(t35.INPUTS)
    exclusion_doc = t35.load_json(t35.EXCLUSION_RECLAIM)
    recipe_doc = t35.load_json(t35.RECIPE_FAMILIES)
    runtime_doc = t35.load_json(t35.RUNTIME_REGISTRY)
    load_doc = t35.load_json(t35.LOAD_BASELINE)
    impl_index = _cc_impl_index()
    presentation = _presentation_overlay()
    worldgen = _worldgen_overlay()

    identities: dict[str, dict[str, Any]] = {}

    for table, spec in common.TABLE_SPECS.items():
        t13_document = common.load_json(ROOT / spec["artifact"])
        t27_rows = _load_t27_table(table)
        for t13_row in common.t13_rows(table, t13_document):
            record = _classify_t13(
                table,
                t13_row,
                t27_rows.get(str(t13_row.get(spec["id_field"]) or "")),
                spec,
            )
            _validate_owner(record["owner"])
            _attach_overlays(record, presentation=presentation, worldgen=worldgen)
            identities[record["canonical_id"]] = record

    for summary in exclusion_doc.get("family_summaries") or []:
        record = _classify_exclusion(summary)
        record = _apply_storage_scope(record, storage_scope)
        _validate_owner(record["owner"])
        identities[record["canonical_id"]] = record

    t37_pilot = _select_t37_pilot(recipe_doc.get("families") or [])
    t37_ids = set(t37_pilot["family_ids"])
    for family in recipe_doc.get("families") or []:
        record = _classify_recipe(family, t37_ids=t37_ids)
        _validate_owner(record["owner"])
        identities[record["canonical_id"]] = record

    def _runtime_key(category: str, runtime_id: str) -> str:
        return f"{category}/{runtime_id}"

    runtime_to_census: dict[str, str] = {}
    material_runtime: dict[str, list[str]] = defaultdict(list)
    static_runtime: dict[str, list[str]] = defaultdict(list)
    machine_runtime: dict[str, list[str]] = defaultdict(list)
    worldgen_runtime: dict[str, list[str]] = defaultdict(list)

    for category, rows in (runtime_doc.get("categories") or {}).items():
        for row in rows:
            runtime_id = str(row.get("id") or "")
            runtime_key = _runtime_key(category, runtime_id)
            kind = str(row.get("identity_kind") or "")
            if kind == "generated_material_form":
                material_key = _material_id_from_runtime(runtime_id)
                local_id = f"runtime_local:material/{material_key}"
                material_runtime[local_id].append(runtime_key)
                continue
            if kind == "generated_catalog":
                runtime_to_census[runtime_key] = "exclusion/hopper"
                continue
            if kind == "source_backed_variant":
                path = runtime_id.split(":", 1)[1]
                local_id = f"runtime_local:machine_tier/{path}"
                machine_runtime[local_id].append(runtime_key)
                continue
            if kind == "data_driven":
                path = runtime_id.split(":", 1)[1]
                local_id = f"runtime_local:worldgen/{path}"
                worldgen_runtime[local_id].append(runtime_key)
                continue
            matches = impl_index.get(runtime_id) or []
            if matches:
                runtime_to_census[runtime_key] = matches[0]
            else:
                path = runtime_id.split(":", 1)[1]
                local_id = f"runtime_local:static/{path}"
                static_runtime[local_id].append(runtime_key)

    for local_id, runtime_keys in sorted(material_runtime.items()):
        if local_id not in identities:
            identities[local_id] = _runtime_local_identity(
                local_id,
                domain="runtime_local",
                reason="Generated material-form runtime bucket.",
                owner="portfolio:runtime/material_catalog",
                runtime_ids=runtime_keys,
            )
        for runtime_key in runtime_keys:
            runtime_to_census[runtime_key] = local_id

    for local_id, runtime_keys in sorted(machine_runtime.items()):
        if local_id not in identities:
            identities[local_id] = _runtime_local_identity(
                local_id,
                domain="runtime_local",
                reason="Machine tier block registered via machine_tiers.json.",
                owner="portfolio:runtime/machine_tier",
                runtime_ids=runtime_keys,
            )
        for runtime_key in runtime_keys:
            runtime_to_census[runtime_key] = local_id

    for local_id, runtime_keys in sorted(static_runtime.items()):
        if local_id not in identities:
            identities[local_id] = _runtime_local_identity(
                local_id,
                domain="runtime_local",
                reason="Static one-off runtime without T13 mapping.",
                owner="portfolio:runtime/static_one_off",
                runtime_ids=runtime_keys,
            )
        for runtime_key in runtime_keys:
            runtime_to_census[runtime_key] = local_id

    for local_id, runtime_keys in sorted(worldgen_runtime.items()):
        if local_id not in identities:
            identities[local_id] = _runtime_local_identity(
                local_id,
                domain="runtime_local",
                reason="Data-driven worldgen runtime identity.",
                owner="portfolio:runtime/worldgen",
                runtime_ids=runtime_keys,
            )
        for runtime_key in runtime_keys:
            runtime_to_census[runtime_key] = local_id

    expected_runtime = int(runtime_doc.get("total_expected_ids") or 0)
    if len(runtime_to_census) != expected_runtime:
        raise ValueError(
            f"runtime mapping count {len(runtime_to_census)} != expected {expected_runtime}"
        )
    unique_runtime_ids = {
        key.split("/", 1)[1]
        for key in runtime_to_census
        if "/" in key
    }
    if len(unique_runtime_ids) != len(
        {
            row["id"]
            for rows in (runtime_doc.get("categories") or {}).values()
            for row in rows
        }
    ):
        raise ValueError("runtime namespace id set drifted during mapping")
    if len(set(runtime_to_census.values())) > len(identities):
        raise ValueError("runtime mapping references unknown identities")

    for canonical_id, record in sorted(identities.items()):
        identities[canonical_id] = t35.assign_portfolio_scope(
            record,
            storage_scope=storage_scope,
            t37_ids=t37_ids,
        )

    disposition_sets = {name: [] for name in t35.DISPOSITIONS}
    priority_sets = {name: [] for name in t35.PRIORITIES}
    scope_sets = {name: [] for name in t35.PORTFOLIO_SCOPES}
    owner_index: dict[str, list[str]] = defaultdict(list)
    for canonical_id, record in identities.items():
        disposition_sets[record["disposition"]].append(canonical_id)
        priority_sets[record["portfolio_priority"]].append(canonical_id)
        scope_sets[record["portfolio_scope"]].append(canonical_id)
        owner_index[record["owner"]].append(canonical_id)

    owner_violations = sum(
        1
        for record in identities.values()
        if not str(record.get("owner") or "").strip()
        or str(record.get("owner") or "").strip().lower()
        in policy["owner_rules"]["forbidden_tokens"]
    )

    dependency_errors = _validate_dependencies(identities)
    dep_edges = [
        (canonical_id, f"{dep['kind']}:{dep['id']}")
        for canonical_id, record in identities.items()
        for dep in record.get("dependencies") or []
    ]
    cycles = _detect_cycles(dep_edges)

    mandatory_work_set = sorted(
        canonical_id
        for canonical_id, record in identities.items()
        if record["portfolio_scope"] == "in_scope_1x"
        and record["disposition"] == "planned"
        and (record.get("axes") or {}).get("closure", {}).get("status") != "closed"
        and record["portfolio_priority"] in {"P0", "P1"}
    )
    p2_candidates = sorted(
        canonical_id
        for canonical_id, record in identities.items()
        if record["portfolio_scope"] == "candidate_1x"
    )
    p3_disposition = sorted(
        canonical_id
        for canonical_id, record in identities.items()
        if record["portfolio_priority"] == "P3"
        or record["disposition"] == "out_of_scope"
    )

    t13_count = sum(1 for key in identities if key.startswith("t13/"))
    exclusion_count = sum(1 for key in identities if key.startswith("exclusion/"))
    recipe_count = sum(1 for key in identities if key.startswith("portfolio:track_a/"))
    runtime_local_count = sum(1 for key in identities if key.startswith("runtime_local:"))

    recipe_rows = sum(
        int(record["source"]["expanded_rows"])
        for key, record in identities.items()
        if key.startswith("portfolio:track_a/")
    )

    scope_errors: list[str] = []
    unscoped = 0
    for canonical_id, record in identities.items():
        scope = record.get("portfolio_scope")
        if scope not in t35.PORTFOLIO_SCOPES:
            unscoped += 1
            continue
        contract = record.get("scope_contract")
        if not isinstance(contract, dict):
            scope_errors.append(f"{canonical_id} missing scope_contract")
            continue
        closure = (record.get("axes") or {}).get("closure", {}).get("status")
        if scope == "in_scope_1x" and closure != "closed":
            if canonical_id not in mandatory_work_set:
                scope_errors.append(
                    f"{canonical_id} in_scope_1x incomplete missing fixed/generated work set"
                )
        if scope == "candidate_1x":
            required = {"admission_criterion", "measurement_owner", "recheck_epoch"}
            if not required <= set(contract):
                scope_errors.append(f"{canonical_id} candidate_1x contract incomplete")
        if scope == "post_1x" and not str(contract.get("subsequent_track") or "").startswith(
            "post_1x/"
        ):
            scope_errors.append(f"{canonical_id} post_1x contract incomplete")

    validators = {
        "unclassified": 0,
        "unscoped": unscoped,
        "scope_errors": len(scope_errors),
        "unmapped_runtime": 0,
        "orphan_runtime_targets": 0,
        "duplicate_runtime_mapping": 0,
        "owner_violations": owner_violations,
        "dependency_errors": len(dependency_errors),
        "dependency_cycles": len(cycles),
        "source_publication_delta": 0 if foundation["publication_delta"] == PUBLICATION_DELTA else 1,
    }
    if dependency_errors or cycles or owner_violations or unscoped or scope_errors:
        raise ValueError(
            "census validator failure: "
            f"errors={dependency_errors} cycles={cycles} owners={owner_violations} "
            f"unscoped={unscoped} scope_errors={scope_errors[:3]}"
        )

    owned_inputs = {
        t35.relative(BUILDER): t35.sha256_file(BUILDER),
        t35.relative(t35.POLICY): t35.sha256_file(t35.POLICY),
        t35.relative(t35.STORAGE_SCOPE): t35.sha256_file(t35.STORAGE_SCOPE),
    }
    for path, _ in FOUNDATION_ARTIFACTS:
        owned_inputs[t35.relative(path)] = t35.sha256_file(path)

    return {
        "schema_version": 1,
        "status": "T35_CENSUS_AGGREGATED",
        "source_revision": SOURCE_REVISION,
        "generated_by": "python tools/build_t35_census.py",
        "publication_delta": PUBLICATION_DELTA,
        "currentness": {
            "owned_inputs": owned_inputs,
            "foundation_checks": foundation,
            "inputs_policy_sha256": inputs_doc.get("policy_sha256"),
            "load_baseline_status": load_doc.get("status"),
        },
        "fixed_card_nodes": policy["fixed_card_nodes"],
        "generated_topology_card_count_before_census": policy[
            "generated_topology_card_count_before_census"
        ],
        "counts": {
            "t13_identities": t13_count,
            "exclusion_families": exclusion_count,
            "recipe_families": recipe_count,
            "runtime_local_identities": runtime_local_count,
            "identities_total": len(identities),
            "unscoped": unscoped,
            "runtime_ids_expected": expected_runtime,
            "runtime_ids_mapped": len(runtime_to_census),
            "recipe_rows_accounted": recipe_rows,
            "exclusion_source_sites": exclusion_doc["counts"]["source_sites"],
            "exclusion_expanded_rows": exclusion_doc["counts"]["expanded_multiplicity"],
        },
        "indexes": {
            "by_domain": {
                "t13": t13_count,
                "exclusion": exclusion_count,
                "recipe": recipe_count,
                "runtime_local": runtime_local_count,
            },
            "runtime_to_census": dict(sorted(runtime_to_census.items())),
            "owner": {owner: sorted(ids) for owner, ids in sorted(owner_index.items())},
        },
        "work_sets": {
            "mandatory_p0_p1": mandatory_work_set,
            "p2_candidates": p2_candidates,
            "p3_disposition": p3_disposition,
        },
        "t37_pilot": t37_pilot,
        "identities": identities,
        "disposition_sets": {
            key: sorted(values) for key, values in disposition_sets.items()
        },
        "portfolio_priority_sets": {
            key: sorted(values) for key, values in priority_sets.items()
        },
        "portfolio_scope_sets": {
            key: sorted(values) for key, values in scope_sets.items()
        },
        "validators": validators,
    }


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {t35.relative(OUTPUT)}"]
    expected = t35.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        errors.append(t35.stale_error(OUTPUT, expected, actual))
    return errors


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = t35.load_json(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T35 census failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "status": document.get("status"),
        "counts": document.get("counts"),
        "validators": document.get("validators"),
        "t37_pilot": {
            "host_map": (document.get("t37_pilot") or {}).get("host_map"),
            "source_rows": (document.get("t37_pilot") or {}).get("source_rows"),
            "family_count": len((document.get("t37_pilot") or {}).get("family_ids") or []),
        },
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
