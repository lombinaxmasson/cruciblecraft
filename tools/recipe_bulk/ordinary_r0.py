#!/usr/bin/env python3
"""Replay remaining ordinary families for semantic host-closure waves."""
from __future__ import annotations

import hashlib
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

from tools import t35_common as t35

T42_OWNER = t35.TOOLS / "t42_owner_disposition_lock.json"
T43_LOCK = t35.TOOLS / "t43_production_lock.json"
T45_LOCK = t35.TOOLS / "t45_production_lock.json"
T35_FAMILIES = t35.TOOLS / "t35_recipe_families.json"
DEFERRED_RECYCLING = "later:recycling"
REMAINDER_HOSTS = (
    "cruciblecraft:drying",
    "cruciblecraft:electrolyzer",
    "cruciblecraft:centrifuge",
    "cruciblecraft:autoclave",
    "cruciblecraft:compressor",
)
FOUNDATION_CLOSED_LOCKS = (
    t35.TOOLS / "waves" / "smelter" / "ordinary-closure" / "production_lock.json",
    t35.TOOLS / "waves" / "mixer" / "ordinary-closure" / "production_lock.json",
)
HOST_ORDINARY_LOCKS = FOUNDATION_CLOSED_LOCKS + (
    t35.TOOLS / "waves" / "drying" / "ordinary-closure" / "production_lock.json",
    t35.TOOLS / "waves" / "electrolyzer" / "ordinary-closure" / "production_lock.json",
    t35.TOOLS / "waves" / "centrifuge" / "ordinary-closure" / "production_lock.json",
    t35.TOOLS / "waves" / "autoclave" / "ordinary-closure" / "production_lock.json",
    t35.TOOLS / "waves" / "compressor" / "ordinary-closure" / "production_lock.json",
)
CLOSED_ORDINARY_LOCKS = HOST_ORDINARY_LOCKS
CLOSED_EXECUTION_HOSTS = (
    "cruciblecraft:smelter",
    "cruciblecraft:mixer",
)

COHORT_GROUPS = {
    "coordination/multi_axis": "multiitem",
    "object_expression/multiitem": "multiitem",
    "object_expression/tool_head": "tool_head",
    "material_expression/form": "material_form",
    "identity_mapping/unmapped": "unmapped",
    "object_expression/block": "unmapped",
    "acquisition/b0": "acquisition",
    "recycling/evidence_needed": "unmapped",
}


def _lock_family_ids(path: Path, host: str | None = None) -> set[str]:
    document = t35.load_json(path)
    ids: set[str] = set()
    host_token = f":{host.split(':', 1)[-1]}/" if host else None
    for row in (document.get("production") or {}).get("families") or []:
        family_id = str(row["family_id"])
        row_host = str(row.get("host") or "")
        if host:
            if row_host and row_host != host:
                continue
            if not row_host and host_token and host_token not in family_id:
                continue
        ids.add(family_id)
    return ids


def _family_ids_from_lock(path: Path) -> set[str]:
    if not path.is_file():
        return set()
    seal = path.parent / "closeout_seal.json"
    if not seal.is_file() or t35.load_json(seal).get("status") != "SEALED":
        return set()
    document = t35.load_json(path)
    ids = {
        str(row["family_id"])
        for row in (document.get("production") or {}).get("families") or []
    }
    ids.update(str(row["family_id"]) for row in document.get("reclassified") or [])
    return ids


def closed_ordinary_family_ids(*, opening: bool = False) -> set[str]:
    paths = FOUNDATION_CLOSED_LOCKS if opening else HOST_ORDINARY_LOCKS
    ids: set[str] = set()
    for path in paths:
        ids |= _family_ids_from_lock(path)
    return ids


def remaining_owner_rows(host: str) -> list[dict[str, Any]]:
    owner = t35.load_json(T42_OWNER)
    locked = (
        _lock_family_ids(T43_LOCK, host)
        | _lock_family_ids(T45_LOCK, host)
        | closed_ordinary_family_ids()
    )
    rows: list[dict[str, Any]] = []
    for row in owner.get("families") or []:
        if str(row.get("host") or "") != host:
            continue
        if str(row.get("future_owner") or "") == DEFERRED_RECYCLING:
            continue
        if str(row.get("disposition") or "") == "phase_deferred":
            continue
        family_id = str(row["family_id"])
        if family_id in locked:
            continue
        rows.append(row)
    return rows


def global_remainder_r0() -> dict[str, Any]:
    seen: set[str] = set()
    duplicates: list[str] = []
    forbidden: list[str] = []
    hosts: dict[str, dict[str, Any]] = {}
    family_total = 0
    relation_total = 0
    for host in REMAINDER_HOSTS:
        rows = [
            row
            for row in t35.load_json(T42_OWNER).get("families") or []
            if str(row.get("host") or "") == host
            and str(row.get("future_owner") or "") != DEFERRED_RECYCLING
            and str(row.get("disposition") or "") != "phase_deferred"
            and str(row["family_id"])
            not in (
                _lock_family_ids(T43_LOCK, host)
                | _lock_family_ids(T45_LOCK, host)
                | closed_ordinary_family_ids(opening=True)
            )
        ]
        summary = {
            "family_count": len(rows),
            "family_ids": [str(row["family_id"]) for row in rows],
            "relation_count": sum(int(row.get("relation_count") or 0) for row in rows),
        }
        hosts[host] = summary
        family_total += int(summary["family_count"])
        relation_total += int(summary["relation_count"])
        for family_id in summary["family_ids"]:
            if family_id in seen:
                duplicates.append(family_id)
            seen.add(family_id)
    for host in CLOSED_EXECUTION_HOSTS:
        leaked = remaining_summary(host)
        if leaked["family_count"]:
            forbidden.append(f"{host}:{leaked['family_count']}")
    for family_id in seen:
        if "#bath" in family_id or family_id.startswith("gt.recipe.bath"):
            forbidden.append(f"bath_in_gap:{family_id}")
    errors: list[str] = []
    if family_total != 334:
        errors.append(f"family_total={family_total} expected 334")
    if relation_total != 2047:
        errors.append(f"relation_total={relation_total} expected 2047")
    if duplicates:
        errors.append("duplicate_families=" + ",".join(duplicates[:8]))
    if forbidden:
        errors.append("closed_hosts_leaked=" + ",".join(forbidden))
    return {
        "duplicates": duplicates,
        "errors": errors,
        "family_total": family_total,
        "forbidden_host_leaks": forbidden,
        "hosts": hosts,
        "program_denominator": 334,
        "relation_total": relation_total,
        "source_revision": t35.SOURCE_REVISION,
        "status": "GLOBAL_R0_READY" if not errors else "GLOBAL_R0_DRIFT",
    }


def publication_group_for(host: str, owner: str, expanded_count: int) -> str:
    host_path = host.split(":", 1)[-1]
    if host_path == "mixer":
        if "foam" in owner or owner == "recipe_wave/mixer":
            return "cruciblecraft:mixer/ordinary_closure/construction_foam_matrix"
        if owner.startswith("material_expression") or expanded_count > 1:
            return "cruciblecraft:mixer/ordinary_closure/material_matrix"
        return "cruciblecraft:mixer/ordinary_closure/opaque"
    cohort = COHORT_GROUPS.get(owner)
    if cohort is None and owner.startswith("object_expression/gt_prefix"):
        cohort = "gt_prefix"
    if cohort is None:
        cohort = "singleton" if expanded_count == 1 else "multiitem"
    if expanded_count == 1 and cohort in {"multiitem", "material_form"}:
        cohort = "singleton"
    return f"cruciblecraft:{host_path}/ordinary_closure/{cohort}"


def remaining_summary(host: str) -> dict[str, Any]:
    rows = remaining_owner_rows(host)
    owners = Counter(str(row.get("current_owner") or "unknown") for row in rows)
    relations = sum(int(row.get("relation_count") or 0) for row in rows)
    return {
        "family_count": len(rows),
        "host": host,
        "owners": dict(sorted(owners.items())),
        "relation_count": relations,
        "template_keys": [str(row["template_key"]) for row in rows],
        "family_ids": [str(row["family_id"]) for row in rows],
    }


def live_remainder_replay() -> dict[str, Any]:
    """Current execution-gap replay after sealed ordinary-closure locks."""
    hosts: dict[str, dict[str, Any]] = {}
    family_total = 0
    relation_total = 0
    for host in REMAINDER_HOSTS + CLOSED_EXECUTION_HOSTS:
        summary = remaining_summary(host)
        hosts[host] = {
            "family_count": summary["family_count"],
            "family_ids": summary["family_ids"],
            "relation_count": summary["relation_count"],
        }
        family_total += int(summary["family_count"])
        relation_total += int(summary["relation_count"])
    errors: list[str] = []
    if family_total != 0:
        errors.append(f"live_family_total={family_total} expected 0")
    if relation_total != 0:
        errors.append(f"live_relation_total={relation_total} expected 0")
    return {
        "errors": errors,
        "family_total": family_total,
        "hosts": hosts,
        "relation_total": relation_total,
        "source_revision": t35.SOURCE_REVISION,
        "status": "LIVE_REMAINDER_ZERO" if not errors else "LIVE_REMAINDER_OPEN",
    }


def selection_sha256(family_ids: list[str]) -> str:
    payload = "".join(f"{value}\n" for value in family_ids)
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()
