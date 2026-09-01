#!/usr/bin/env python3
"""Freeze the T48 Bath remainder production lock from complete mapped cohorts."""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t41_shard_router as router
from tools import t48_common as common
from tools.build_t48_identity_catalog import remaining_unmapped_allowed

OUTPUT = common.PRODUCTION_LOCK
SELECTION_RULE = (
    "Lock every remainder family whose candidate_outcome is production: all "
    "source operands have a minecraft:/cruciblecraft: runtime. Families stay "
    "complete (partial=0). Complete identity-kind cohorts may lock as a subset. "
    "Mixer padding is forbidden. B0-open families are included with "
    "needs_b1_support."
)


def _support_block() -> dict[str, Any]:
    pending = {
        "kind": "pending_layered_b1",
        "route_count": 0,
        "route_keys": [],
        "tree_sha256": common.EMPTY_TREE_SHA256,
    }
    if not common.PLAYER_PATH_SUPPORT.is_file():
        return pending
    document = common.load_json(common.PLAYER_PATH_SUPPORT)
    routes = list(document.get("routes") or [])
    if document.get("kind") != common.PLAYER_PATH_REAL_KIND or not routes:
        return pending
    keys = sorted(
        f"{row['source_map']}#{row['source_recipe']}"
        for row in routes
        if row.get("source_map") and row.get("source_recipe")
    )
    if len(keys) != len(routes) or len(set(keys)) != len(keys):
        raise ValueError("T48 support route keys are incomplete or duplicated")
    tree = common.locked_support_tree_sha256()
    if tree == common.EMPTY_TREE_SHA256:
        raise ValueError("T48 support recipes are still an empty tree")
    return {
        "kind": common.PLAYER_PATH_REAL_KIND,
        "route_count": len(routes),
        "route_keys": keys,
        "tree_sha256": tree,
    }


def _operands(relation: dict[str, Any]) -> list[dict[str, Any]]:
    return (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )


def _consume(relation: dict[str, Any]) -> tuple[str, str, str]:
    for operand, count, action in zip(
        relation.get("item_inputs") or [],
        relation.get("item_input_counts") or [],
        relation.get("item_input_actions") or [],
        strict=False,
    ):
        kind = str((action or {}).get("kind") or "").lower()
        if kind != "consume" or int(count or 0) <= 0:
            continue
        source = operand.get("source") or {}
        runtime = common.assert_runtime_id(operand.get("runtime_id"), consume=True)
        mapping = str(operand.get("mapping") or "proven_equivalent")
        return str(source.get("item") or ""), runtime, mapping
    raise ValueError(f"missing consume operand for {relation.get('template_key')}")


def _publication_group(representation: str, unique_kinds: list[str]) -> str:
    if "tool_head" in unique_kinds:
        return common.PUBLICATION_GROUP_TOOL_HEAD
    if representation == "exact":
        return common.PUBLICATION_GROUP_EXACT
    if representation == "exact_multi":
        return common.PUBLICATION_GROUP_EXACT_MULTI
    raise ValueError(f"unexpected representation {representation}")


def _capability_cohort(representation: str, unique_kinds: list[str]) -> str:
    kinds = set(unique_kinds)
    if "tool_head" in kinds:
        return "bath_tool_head"
    if "multiitem" in kinds:
        return "bath_multiitem"
    if any(kind.startswith("gt_prefix:") for kind in kinds) or "gt_prefix" in kinds:
        return "bath_prefix_form"
    if representation == "exact":
        return "bath_exact"
    if not kinds:
        return "bath_typed_exact_multi"
    return "bath_identity_remainder"


def _router_relation(relation: dict[str, Any]) -> dict[str, Any]:
    item_inputs: list[dict[str, Any]] = []
    for operand in relation.get("item_inputs") or []:
        runtime = operand.get("runtime_id")
        tag = operand.get("tag")
        entry: dict[str, Any] = {"type": "minecraft:item"}
        if runtime:
            entry["item"] = str(runtime)
        if tag:
            entry["tag"] = str(tag)
        item_inputs.append(entry if runtime or tag else {})
    fluid_inputs: list[dict[str, Any]] = []
    for operand in relation.get("fluid_inputs") or []:
        runtime = operand.get("runtime_id")
        if runtime:
            fluid_inputs.append({"id": str(runtime)})
    return {
        "fluid_inputs": fluid_inputs,
        "item_inputs": item_inputs,
        "shadow_order": int(relation.get("shadow_order") or 0),
        "stable_id": str(relation["stable_id"]),
    }


def _require_closed() -> dict[str, Any]:
    opening = common.assert_t47_opening_current()
    source = common.load_json(common.SOURCE)
    if source.get("status") != "T48_BATH_SOURCE_FROZEN":
        raise ValueError("T48 production lock requires a frozen remainder source")
    if int((source.get("work_set") or {}).get("source_rows") or 0) != common.CANDIDATE_RELATION_COUNT:
        raise ValueError("T48 source is not the 150/34186 remainder universe")
    candidate = common.load_json(common.CANDIDATE_SELECTION)
    coverage = candidate.get("coverage") or {}
    if int(coverage.get("family_count") or 0) != common.CANDIDATE_FAMILY_COUNT:
        raise ValueError("T48 candidate does not cover the remainder universe")
    if int(coverage.get("reclassified") or 0) != 0:
        raise ValueError("T48 lock forbids reclassified filler families")
    catalog = common.load_json(common.IDENTITY_CATALOG)
    if catalog.get("status") != "T48_IDENTITY_CATALOG":
        raise ValueError("T48 lock requires a frozen identity catalog")
    fluids = common.load_json(common.FLUID_MAPPING)
    if fluids.get("status") != "T48_BATH_FLUID_MAPPING":
        raise ValueError("T48 lock requires a frozen fluid overlay")
    forms = common.load_json(common.REQUIRED_FORMS)
    if forms.get("status") != "T48_REQUIRED_FORMS_FROZEN":
        raise ValueError("T48 lock requires frozen required forms")
    recycling = common.load_json(common.RECYCLING_DISPOSITION)
    if recycling.get("recycling_candidate") is not False:
        raise ValueError("T48 lock requires non-recycling disposition")
    if int(recycling.get("deferred_recycling_untouched") or 0) != common.DEFERRED_RECYCLING_COUNT:
        raise ValueError("T48 lock must leave the 1817 deferred recycling families untouched")
    operand_map = common.load_json(common.OPERAND_RUNTIME_MAP)
    return {
        "candidate": candidate,
        "catalog": catalog,
        "fluids": fluids,
        "forms": forms,
        "opening": opening,
        "operand_map": operand_map,
        "recycling": recycling,
        "source": source,
    }


def _identity_lock_ids(
    work_set: dict[str, Any],
    by_family: dict[str, list[dict[str, Any]]],
) -> set[str]:
    blocker = {
        str(row["family_id"]): row
        for row in common.load_json(common.T42_BLOCKER).get("families") or []
    }
    selected: set[str] = set()
    for row in work_set.get("families") or []:
        family_id = str(row["family_id"])
        if (blocker.get(family_id) or {}).get("recycling_candidate"):
            continue
        ok = True
        for relation in by_family.get(family_id) or []:
            for operand in _operands(relation):
                if not remaining_unmapped_allowed(operand):
                    ok = False
                    break
            if not ok:
                break
        if ok and (by_family.get(family_id) or []):
            selected.add(family_id)
    return selected


def _dry_run(
    grouped: dict[str, list[dict[str, Any]]],
) -> dict[str, Any]:
    groups: list[dict[str, Any]] = []
    for group_id in common.PUBLICATION_GROUPS:
        relations = grouped.get(group_id) or []
        if not relations:
            continue
        routed = router.route_group(common.TARGET_MAP, group_id, relations)
        if routed["overflow_count"] != 0:
            raise ValueError(f"T48 router dry-run overflow for {group_id}")
        if routed["worst_shard_size"] > router.HARD_SHARD_CEILING:
            raise ValueError(
                f"T48 router dry-run exceeded hard ceiling {router.HARD_SHARD_CEILING} "
                f"for {group_id}: {routed['worst_shard_size']}"
            )
        groups.append(
            {
                "overflow_count": routed["overflow_count"],
                "publication_group": group_id,
                "relation_count": len(relations),
                "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
                "shard_count": routed["shard_count"],
                "worst_shard_size": routed["worst_shard_size"],
            }
        )
    if not groups:
        raise ValueError("T48 lock has no non-empty publication groups")
    if router.ROUTING_SCHEMA_VERSION != "t39-shard-v1":
        raise ValueError("T48 shard router must stay on t39-shard-v1")
    return {
        "groups": groups,
        "hard_ceiling": router.HARD_SHARD_CEILING,
        "overflow_explicit": True,
        "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
    }


def build() -> dict[str, Any]:
    closed = _require_closed()
    work_set = common.load_json(common.WORK_SET)
    candidate = closed["candidate"]
    source = closed["source"]
    recycling = closed["recycling"]
    opening = closed["opening"]
    production_ids = [str(value) for value in candidate.get("production_family_ids") or []]
    production_id_set = set(production_ids)
    candidate_by_id = {
        str(row["family_id"]): row for row in candidate.get("families") or []
    }
    work_by_id = {str(row["family_id"]): row for row in work_set.get("families") or []}
    recycling_ids = [
        str(row["family_id"]) for row in recycling.get("families") or []
    ]
    if sorted(production_ids) != sorted(recycling_ids):
        raise ValueError("T48 recycling disposition families drifted from candidate production")
    prior_ids = common.prior_locked_family_ids()
    by_family: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in source.get("relations") or []:
        by_family[str(relation.get("family_id") or "")].append(relation)
    identity_ids = _identity_lock_ids(work_set, by_family)
    if production_id_set != identity_ids:
        raise ValueError(
            f"T48 identity lock set {len(identity_ids)} != candidate production "
            f"{len(production_ids)}"
        )
    b0 = common.b0_identities()
    production_families: list[dict[str, Any]] = []
    stable_ids: list[str] = []
    source_row_sha256: list[str] = []
    router_grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    exact = 0
    multi = 0
    seen_ids: set[str] = set()
    used_groups: set[str] = set()
    for family_id in production_ids:
        if family_id in seen_ids:
            raise ValueError(f"duplicate T48 production family {family_id}")
        seen_ids.add(family_id)
        if family_id in prior_ids:
            raise ValueError(f"prior lock family leaked into T48 lock: {family_id}")
        candidate_row = candidate_by_id[family_id]
        work_row = work_by_id[family_id]
        if candidate_row.get("candidate_outcome") != "production":
            raise ValueError(f"{family_id}: lock family is not a production candidate")
        if candidate_row.get("host") != common.HOST or work_row.get("host") != common.HOST:
            raise ValueError(f"{family_id}: host is not Bath")
        relations = sorted(
            by_family.get(family_id) or [],
            key=lambda item: (
                int(item.get("shadow_order") or 0),
                str(item.get("stable_id") or ""),
            ),
        )
        expanded = int(work_row.get("expanded_count") or 0)
        if not relations or len(relations) != expanded:
            raise ValueError(f"{family_id}: lock source cardinality drifted")
        if len(relations) != int(candidate_row.get("source_relation_count") or 0):
            raise ValueError(f"{family_id}: candidate relation count drifted")
        representation = str(work_row.get("representation") or "")
        if representation == "exact":
            exact += 1
        elif representation == "exact_multi":
            multi += 1
        else:
            raise ValueError(f"{family_id}: representation is not exact/exact_multi")
        unique_kinds = list(work_row.get("unique_kinds") or [])
        group_id = _publication_group(representation, unique_kinds)
        if group_id in {
            common.T46_PUBLICATION_GROUP,
            common.T47_PUBLICATION_GROUP_EXACT,
            common.T47_PUBLICATION_GROUP_EXACT_MULTI,
        }:
            raise ValueError("T48 lock must not reuse T46/T47 publication groups")
        used_groups.add(group_id)
        family_stable = [str(relation["stable_id"]) for relation in relations]
        family_hashes = [str(relation["source_row_sha256"]) for relation in relations]
        source_item, runtime_id, mapping_class = _consume(relations[0])
        typed = f"item:{runtime_id}" in b0 or f"fluid:{runtime_id}" in b0
        production_families.append(
            {
                "b0_status": "typed_b0" if typed else "needs_b1_support",
                "b1_status": (
                    "real_support"
                    if common.player_path_real()
                    else "pending_real_support"
                ),
                "capability_cohort": _capability_cohort(representation, unique_kinds),
                "expanded_count": expanded,
                "family_id": family_id,
                "host": common.HOST,
                "mapped_cc_identity": runtime_id,
                "mapped_runtime_id": runtime_id,
                "mapping_class": mapping_class,
                "publication_group": group_id,
                "representation": representation,
                "source_identity": source_item,
                "source_row_sha256": family_hashes,
                "source_template": work_row["template_key"],
                "stable_ids": family_stable,
                "template_key": work_row["template_key"],
            }
        )
        stable_ids.extend(family_stable)
        source_row_sha256.extend(family_hashes)
        router_grouped[group_id].extend(_router_relation(relation) for relation in relations)
    if len(production_families) < common.MIN_PRODUCTION_FAMILIES:
        raise ValueError(
            f"T48 lock {len(production_families)} < {common.MIN_PRODUCTION_FAMILIES}; "
            "keep BLOCKED or withdraw the card"
        )
    if len(stable_ids) != len(set(stable_ids)):
        raise ValueError("T48 lock stable ids collided")
    if len(source_row_sha256) != len(stable_ids):
        raise ValueError("T48 lock source row hashes drifted from stable ids")
    if int(closed["operand_map"].get("lock_family_count") or 0) != len(production_families):
        raise ValueError("T48 operand map lock_family_count drifted from production")
    digest = common.selection_sha256([row["family_id"] for row in production_families])
    dry_run = _dry_run(router_grouped)
    blocked_count = int((candidate.get("coverage") or {}).get("blocked") or 0)
    blocked_relations = common.CANDIDATE_RELATION_COUNT - len(stable_ids)
    family_ids = [row["family_id"] for row in production_families]
    publication_groups = [group for group in common.PUBLICATION_GROUPS if group in used_groups]
    return {
        "candidate_snapshot": {
            "blocked_family_count": blocked_count,
            "coverage": dict(candidate.get("coverage") or {}),
            "path": common.relative(common.CANDIDATE_SELECTION),
            "production_authority": False,
            "selection_sha256": candidate["selection_sha256"],
            "universe_family_count": common.CANDIDATE_FAMILY_COUNT,
            "universe_relation_count": common.CANDIDATE_RELATION_COUNT,
        },
        "catalog_fixture": {
            "families": len(production_families),
            "identity_catalog_sha256": t35.sha256_file(common.IDENTITY_CATALOG),
            "operand_runtime_map_sha256": t35.sha256_file(common.OPERAND_RUNTIME_MAP),
            "recycling_disposition_sha256": t35.sha256_file(common.RECYCLING_DISPOSITION),
            "relations": len(stable_ids),
            "required_forms_sha256": t35.sha256_file(common.REQUIRED_FORMS),
            "selection_sha256": digest,
            "source_sha256": t35.sha256_file(common.SOURCE),
            "status": "PRODUCTION_COMPLETE_COHORT_SUBSET",
        },
        "generated_by": "python tools/build_t48_production_lock.py",
        "note": (
            f"{len(production_families)}/{len(stable_ids)} is the production "
            "authority. Mixer and T46/T47 Bath groups stay out. Incomplete "
            "cohorts stay in the current gap. No combinatorial deferral."
        ),
        "opening": {
            "authored_entries": opening["authored_entries"],
            "eager_rows": opening["eager_rows"],
            "execution_gap": opening["execution_gap"],
            "lazy_rows": opening["lazy_rows"],
            "cache_ceiling_rows": opening["cache_ceiling_rows"],
            "t14_closing": opening["t14_closing"],
            "t47_production_lock_sha256": opening["t47_production_lock_sha256"],
            "v2_load_policy_sha256": opening["v2_load_policy_sha256"],
        },
        "partial_family_count": 0,
        "phase_deferred": [],
        "production": {
            "exact_families": exact,
            "exact_multi_families": multi,
            "families": production_families,
            "family_count": len(production_families),
            "family_ids": family_ids,
            "publication_groups": publication_groups,
            "relation_count": len(stable_ids),
            "selection_rule": SELECTION_RULE,
            "selection_sha256": digest,
            "source_row_sha256": source_row_sha256,
            "stable_ids": stable_ids,
            "template_keys": [row["template_key"] for row in production_families],
        },
        "remainder_not_locked": {
            "family_count": blocked_count,
            "reason": "blocked_identity_or_unregistered_form",
            "relation_count": blocked_relations,
        },
        "router_dry_run": dry_run,
        "schema_version": 1,
        "selection_rule": SELECTION_RULE,
        "size_exception": common.size_exception(),
        "source_revision": common.SOURCE_REVISION,
        "status": "T48_PRODUCTION_LOCKED",
        "support": _support_block(),
    }


def main(argv: list[str] | None = None) -> int:
    from tools import closeout_seal

    return common.run_managed(
        "Freeze the T48 production lock",
        OUTPUT,
        build=build,
        check=lambda: closeout_seal.live_or_sealed_errors(
            "T48",
            "production_lock",
            lambda: common.check_document(OUTPUT, build()),
        ),
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
