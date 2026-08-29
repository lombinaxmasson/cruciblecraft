#!/usr/bin/env python3
"""Derive T38+ implementation cards from the T35 census work set (R9).

Consumes only `t35_census.json`; does not create census content. Preserves fixed
T36/T37 nodes, excludes the deterministic T37 pilot bundle, and emits a stable
acyclic dependency topology for eligible planned P0/P1 (and admitted P2) families.
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import build_t35_census as census_builder  # noqa: E402
from tools import t27_common as common  # noqa: E402
from tools import t35_common as t35  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t35.CARD_TOPOLOGY
CENSUS = t35.CENSUS
POLICY = t35.POLICY
START_NUMBER = 38
PUBLICATION_DELTA = t35.PUBLICATION_DELTA
PENDING_LOAD_VERDICT = t35.PENDING_LOAD_VERDICT
PRIORITY_RANK = {"P0": 0, "P1": 1, "P2": 2, "P3": 3}


def _node_id(canonical_id: str) -> str:
    return canonical_id


def _work_item(canonical_id: str, record: dict[str, Any]) -> dict[str, Any]:
    axes = record.get("axes") or {}
    load = axes.get("load") or {}
    source = record.get("source") or {}
    return {
        "axes": {
            "closure": dict(axes.get("closure") or {}),
            "fidelity": dict(axes.get("fidelity") or {}),
            "load": dict(load),
        },
        "canonical_id": canonical_id,
        "closure": (axes.get("closure") or {}).get("status"),
        "dependencies": list(record.get("dependencies") or []),
        "disposition": record.get("disposition"),
        "domain": record.get("domain"),
        "fidelity": (axes.get("fidelity") or {}).get("status"),
        "historical_classification": dict(record.get("historical_classification") or {}),
        "load": load.get("status"),
        "load_verdict": load.get("verdict"),
        "owner": record.get("owner"),
        "p2_admission": record.get("p2_admission"),
        "portfolio_priority": record.get("portfolio_priority"),
        "portfolio_scope": record.get("portfolio_scope"),
        "reason": record.get("reason"),
        "recheck_point": record.get("recheck_point"),
        "replacement_condition": record.get("replacement_condition"),
        "source": {
            "artifact": source.get("artifact"),
            "expanded_rows": source.get("expanded_rows"),
            "record_keys": list(source.get("record_keys") or []),
            "revision": source.get("revision"),
            "source_sites": source.get("source_sites"),
        },
    }


def select_work_set(census: dict[str, Any]) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    identities = census.get("identities") or {}
    t37_pilot = census.get("t37_pilot") or {}
    t37_ids = set(t37_pilot.get("family_ids") or [])
    excluded_t37: list[str] = []
    work: list[dict[str, Any]] = []
    for canonical_id, record in identities.items():
        if canonical_id in t37_ids:
            if (
                record.get("disposition") == "planned"
                and (record.get("axes") or {}).get("closure", {}).get("status")
                != "closed"
                and record.get("portfolio_priority") in {"P0", "P1"}
            ):
                excluded_t37.append(canonical_id)
            continue
        if record.get("portfolio_scope") != "in_scope_1x":
            continue
        if record.get("disposition") != "planned":
            continue
        if (record.get("axes") or {}).get("closure", {}).get("status") == "closed":
            continue
        priority = record.get("portfolio_priority")
        if priority in {"P0", "P1"}:
            eligible = True
        elif priority == "P2" and record.get("p2_admission") == t35.P2_ADMISSION:
            eligible = True
        else:
            eligible = False
        if not eligible:
            continue
        work.append(_work_item(canonical_id, record))
    work.sort(
        key=lambda item: (
            PRIORITY_RANK.get(str(item["portfolio_priority"]), 99),
            str(item["domain"] or ""),
            str(item["canonical_id"]),
            str(item["owner"] or ""),
        )
    )
    exclusion = {
        "t37_pilot_families": sorted(excluded_t37),
        "t37_pilot_host_map": t37_pilot.get("host_map"),
        "t37_pilot_source_rows": t37_pilot.get("source_rows"),
    }
    return work, exclusion


def partition_work_set(
    work: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    epoch_a = [
        item for item in work if item.get("portfolio_priority") in {"P0", "P1"}
    ]
    epoch_b = [item for item in work if item.get("portfolio_priority") == "P2"]
    other = [
        item["canonical_id"]
        for item in work
        if item.get("portfolio_priority") not in {"P0", "P1", "P2"}
    ]
    if other:
        raise ValueError(f"generated work set has illegal priorities: {other[:5]}")
    return epoch_a, epoch_b


def validate_work_coverage(work: list[dict[str, Any]], census: dict[str, Any]) -> None:
    t37_ids = set((census.get("t37_pilot") or {}).get("family_ids") or [])
    expected_mandatory = {
        cid
        for cid in (census.get("work_sets") or {}).get("mandatory_p0_p1") or []
        if cid not in t37_ids
    }
    epoch_a, epoch_b = partition_work_set(work)
    covered_a = {item["canonical_id"] for item in epoch_a}
    if covered_a != expected_mandatory:
        missing = sorted(expected_mandatory - covered_a)
        extra = sorted(covered_a - expected_mandatory)
        raise ValueError(
            f"work set mismatch missing={missing[:5]} extra={extra[:5]} "
            f"missing_count={len(missing)} extra_count={len(extra)}"
        )
    for item in epoch_b:
        canonical_id = item["canonical_id"]
        if item.get("p2_admission") != t35.P2_ADMISSION:
            raise ValueError(
                f"{canonical_id} Epoch B P2 missing {t35.P2_ADMISSION}"
            )
        if item.get("portfolio_scope") != "in_scope_1x":
            raise ValueError(f"{canonical_id} Epoch B P2 must be in_scope_1x")


def work_edges(
    work: list[dict[str, Any]],
    identities: dict[str, dict[str, Any]],
) -> tuple[list[tuple[str, str]], dict[str, list[str]]]:
    work_nodes = {_node_id(item["canonical_id"]) for item in work}
    work_lookup = {item["canonical_id"]: item for item in work}
    edges: list[tuple[str, str]] = []
    fixed_deps: dict[str, set[str]] = defaultdict(set)
    broken: list[str] = []
    for item in work:
        src = _node_id(item["canonical_id"])
        fixed_deps[src].add(t35.GLOBAL_EXECUTION_GATE)
        for dep in item["dependencies"]:
            kind = dep.get("kind")
            target = str(dep.get("id") or "")
            if kind == "fixed_card":
                if target not in t35.FIXED_CARDS:
                    broken.append(f"{src} -> invalid fixed_card {target}")
                    continue
                fixed_deps[src].add(target)
                continue
            if kind == "runtime_capability":
                if not target:
                    broken.append(f"{src} -> blank runtime_capability")
                continue
            if kind == "open_item":
                if not target:
                    broken.append(f"{src} -> blank open_item")
                    continue
                continue
            if kind == "canonical_family":
                if target not in identities:
                    broken.append(f"{src} -> missing canonical_family {target}")
                    continue
                dest = _node_id(target)
                if dest not in work_nodes:
                    dest_record = identities[target]
                    closure = (dest_record.get("axes") or {}).get("closure", {}).get(
                        "status"
                    )
                    disposition = dest_record.get("disposition")
                    if disposition == "planned" and closure != "closed":
                        broken.append(
                            f"{src} -> unclosed canonical_family {target} missing from work set"
                        )
                    continue
                edges.append((src, dest))
                continue
            broken.append(f"{src} -> unknown dependency kind {kind}")
    if broken:
        raise ValueError("broken dependencies: " + "; ".join(broken))
    return sorted(set(edges)), {
        src: sorted(deps) for src, deps in sorted(fixed_deps.items())
    }


def _topo_layers(nodes: set[str], edges: list[tuple[str, str]]) -> list[list[str]]:
    incoming: dict[str, int] = {node: 0 for node in nodes}
    outgoing: dict[str, list[str]] = {node: [] for node in nodes}
    for dependent, dependency in edges:
        if dependent not in nodes or dependency not in nodes:
            continue
        outgoing[dependency].append(dependent)
        incoming[dependent] += 1
    ready = sorted(node for node, count in incoming.items() if count == 0)
    layers: list[list[str]] = []
    seen = 0
    while ready:
        layer = list(ready)
        layers.append(layer)
        ready = []
        for node in layer:
            seen += 1
            for dest in outgoing[node]:
                incoming[dest] -= 1
                if incoming[dest] == 0:
                    ready.append(dest)
        ready.sort()
    if seen != len(nodes):
        raise ValueError("dependency graph has a cycle")
    return layers


def _merge_key(item: dict[str, Any]) -> tuple[str, str, str, str, str]:
    return (
        str(item["owner"]),
        str(item["domain"] or ""),
        str(item["fidelity"] or ""),
        str(item["load"] or ""),
        str(item.get("load_verdict") or ""),
    )


def _validate_work_item(item: dict[str, Any]) -> None:
    if item.get("portfolio_scope") != "in_scope_1x":
        raise ValueError(f"{item['canonical_id']} work item must be in_scope_1x")
    owner = str(item.get("owner") or "").strip()
    if not owner or owner.lower() in t35.load_json(POLICY)["owner_rules"]["forbidden_tokens"]:
        raise ValueError(f"{item['canonical_id']} has invalid owner")
    for field in ("replacement_condition", "recheck_point", "reason"):
        if not str(item.get(field) or "").strip():
            raise ValueError(f"{item['canonical_id']} missing {field}")
    load = (item.get("axes") or {}).get("load") or {}
    if load.get("status") == "pending":
        if load.get("verdict") != PENDING_LOAD_VERDICT:
            raise ValueError(f"{item['canonical_id']} pending load missing blocked verdict")
        for key in common.FAKE_ZERO_LOAD_KEYS:
            if load.get(key) == 0:
                raise ValueError(f"{item['canonical_id']} pending load zero-filled {key}")


def merge_and_number(
    work: list[dict[str, Any]],
    edges: list[tuple[str, str]],
    fixed_deps: dict[str, list[str]],
) -> list[dict[str, Any]]:
    for item in work:
        _validate_work_item(item)
    by_owner: dict[str, list[dict[str, Any]]] = {}
    for item in work:
        by_owner.setdefault(str(item["owner"]), []).append(item)
    groups: list[dict[str, Any]] = []
    for owner, items in by_owner.items():
        items = sorted(
            items,
            key=lambda row: (
                PRIORITY_RANK.get(str(row["portfolio_priority"]), 99),
                str(row["domain"] or ""),
                str(row["canonical_id"]),
            ),
        )
        bounds = {_merge_key(item) for item in items}
        if len(bounds) != 1:
            raise ValueError(f"owner {owner} has mixed fidelity/load bounds and cannot merge")
        groups.append(
            {
                "owner": owner,
                "identities": items,
                "merge_key": next(iter(bounds)),
                "fixed_dependencies": sorted(
                    {
                        fixed
                        for item in items
                        for fixed in fixed_deps.get(item["canonical_id"], [])
                    }
                ),
            }
        )
    node_to_owner = {item["canonical_id"]: item["owner"] for item in work}
    group_nodes = {group["owner"] for group in groups}
    group_edges: list[tuple[str, str]] = []
    for dependent, dependency in edges:
        src = node_to_owner.get(dependent)
        dst = node_to_owner.get(dependency)
        if src and dst and src != dst:
            group_edges.append((src, dst))
    layers = _topo_layers(group_nodes, sorted(set(group_edges)))
    ranked: list[dict[str, Any]] = []
    for layer in layers:
        bucket = [group for group in groups if group["owner"] in layer]
        bucket.sort(
            key=lambda group: (
                PRIORITY_RANK.get(
                    str(group["identities"][0]["portfolio_priority"]), 99
                ),
                str(group["identities"][0]["domain"] or ""),
                str(group["identities"][0]["canonical_id"]),
                str(group["owner"]),
            )
        )
        ranked.extend(bucket)
    cards: list[dict[str, Any]] = []
    for index, group in enumerate(ranked):
        card_id = f"T{START_NUMBER + index}"
        axes = group["identities"][0]["axes"]
        load = dict(axes.get("load") or {})
        identities = []
        for item in group["identities"]:
            identities.append(
                {
                    "canonical_id": item["canonical_id"],
                    "domain": item["domain"],
                    "owner": item["owner"],
                    "portfolio_priority": item["portfolio_priority"],
                    "portfolio_scope": item["portfolio_scope"],
                    "disposition": item["disposition"],
                    "closure": item["closure"],
                    "source_lineage": item["source"],
                    "historical_classification": item["historical_classification"],
                }
            )
        cards.append(
            {
                "id": card_id,
                "kind": "generated",
                "started": False,
                "owner_keys": [group["owner"]],
                "track": group["identities"][0]["domain"],
                "merge_key": {
                    "domain": group["merge_key"][1],
                    "fidelity": group["merge_key"][2],
                    "load": group["merge_key"][3],
                    "load_verdict": group["merge_key"][4] or None,
                    "owner": group["merge_key"][0],
                },
                "identities": identities,
                "depends_on": list(group["fixed_dependencies"]),
                "replacement_conditions": sorted(
                    {
                        str(item["replacement_condition"])
                        for item in group["identities"]
                        if item.get("replacement_condition")
                    }
                ),
                "recheck_points": sorted(
                    {
                        str(item["recheck_point"])
                        for item in group["identities"]
                        if item.get("recheck_point")
                    }
                ),
                "source_lineage": [
                    {
                        "canonical_id": item["canonical_id"],
                        "owner": item["owner"],
                        "portfolio_priority": item["portfolio_priority"],
                        "portfolio_scope": item["portfolio_scope"],
                        "disposition": item["disposition"],
                        "replacement_condition": item["replacement_condition"],
                        "recheck_point": item["recheck_point"],
                        "source": item["source"],
                    }
                    for item in group["identities"]
                ],
                "axes": {
                    "closure": {
                        "entry": (axes.get("closure") or {}).get("evidence"),
                        "exit": group["identities"][0].get("replacement_condition"),
                        "status": (axes.get("closure") or {}).get("status"),
                    },
                    "fidelity": {
                        "entry": (axes.get("fidelity") or {}).get("evidence"),
                        "exit": group["identities"][0].get("replacement_condition"),
                        "status": (axes.get("fidelity") or {}).get("status"),
                    },
                    "load": {
                        "entry": load.get("evidence"),
                        "exit": group["identities"][0].get("replacement_condition"),
                        "status": load.get("status"),
                        "verdict": load.get("verdict"),
                    },
                },
                "expected_player_path": group["identities"][0].get("reason"),
                "projected_load_interval": None,
                "integration_profile": "census",
                "publication_delta": dict(PUBLICATION_DELTA),
            }
        )
    owner_to_card = {group["owner"]: card["id"] for group, card in zip(ranked, cards)}
    for card, group in zip(cards, ranked):
        generated_deps = sorted(
            {
                owner_to_card[dst]
                for src, dst in set(group_edges)
                if src == group["owner"] and dst in owner_to_card
            }
        )
        card["depends_on"] = sorted(set(card["depends_on"]) | set(generated_deps))
    return cards


def _fixed_card_entries(policy_nodes: dict[str, Any], census: dict[str, Any]) -> list[dict[str, Any]]:
    t37_pilot = census.get("t37_pilot") or {}
    entries: list[dict[str, Any]] = []
    for card_id in t35.FIXED_CARDS:
        node = dict(policy_nodes[card_id])
        node["started"] = False
        node["generated_card"] = False
        node["publication_delta"] = dict(PUBLICATION_DELTA)
        if card_id == "T37":
            node["pilot"] = {
                "family_ids": list(t37_pilot.get("family_ids") or []),
                "host_map": t37_pilot.get("host_map"),
                "selection_method": t37_pilot.get("selection_method"),
                "source_rows": t37_pilot.get("source_rows"),
            }
        entries.append(node)
    return entries


def build() -> dict[str, Any]:
    census_errors = census_builder.check()
    if census_errors:
        raise ValueError("; ".join(census_errors))
    census = common.load_json(CENSUS)
    if census.get("publication_delta") != PUBLICATION_DELTA:
        raise ValueError("census publication_delta must remain 0/0/0")
    policy = common.load_json(POLICY)
    if policy.get("generated_topology_card_count_before_census") is not None:
        raise ValueError("policy generated_topology_card_count_before_census must remain null")
    work, t37_exclusion = select_work_set(census)
    validate_work_coverage(work, census)
    _epoch_a, epoch_b = partition_work_set(work)
    identities = census.get("identities") or {}
    edges, fixed_deps = work_edges(work, identities)
    cards = merge_and_number(work, edges, fixed_deps)
    gate_bypass = [
        card["id"]
        for card in cards
        if t35.GLOBAL_EXECUTION_GATE not in (card.get("depends_on") or [])
    ]
    if gate_bypass:
        raise ValueError(
            f"Epoch A generated cards bypass {t35.GLOBAL_EXECUTION_GATE}: {gate_bypass}"
        )
    if OUTPUT.is_file():
        previous = json.loads(OUTPUT.read_text(encoding="utf-8"))
        if previous.get("topology_epoch") == t35.TOPOLOGY_EPOCH:
            append_errors = t35.validate_epoch_b_append_only(
                previous.get("cards") or [],
                cards,
            )
            if append_errors:
                raise ValueError("; ".join(append_errors))
    owner_resolution: dict[str, str] = {}
    for card in cards:
        for owner in card["owner_keys"]:
            if owner in owner_resolution:
                raise ValueError(f"owner {owner} resolved to multiple cards")
            owner_resolution[owner] = card["id"]
    fixed_cards = _fixed_card_entries(policy.get("fixed_card_nodes") or {}, census)
    epoch_b_ids = {item["canonical_id"] for item in epoch_b}
    epoch_b_cards = [
        card
        for card in cards
        if any(row["canonical_id"] in epoch_b_ids for row in card.get("identities") or [])
    ]
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(POLICY): common.sha256_file(POLICY),
        common.relative(CENSUS): common.sha256_file(CENSUS),
    }
    return {
        "schema_version": 1,
        "status": "T35_CARD_TOPOLOGY",
        "source_revision": t35.SOURCE_REVISION,
        "generated_by": "python tools/build_t35_card_topology.py --write",
        "topology_epoch": t35.TOPOLOGY_EPOCH,
        "numbering_policy": t35.NUMBERING_POLICY,
        "global_execution_gate": t35.GLOBAL_EXECUTION_GATE,
        "epoch_b": {
            "policy": "append_only",
            "may_renumber_epoch_a": False,
            "admission": (
                "candidate_1x families with ADMITTED_UNDER_SOFT after T37 closure"
            ),
            "admitted_family_count": len(epoch_b),
            "appended_card_count": len(epoch_b_cards),
        },
        "currentness": {
            "owned_inputs": owned_inputs,
            "census_status": census.get("status"),
            "census_source_revision": census.get("source_revision"),
            "census_validators": census.get("validators"),
        },
        "publication_delta": dict(PUBLICATION_DELTA),
        "started": False,
        "start_number": START_NUMBER,
        "generated_card_count": len(cards),
        "selection": (
            "portfolio_scope == in_scope_1x AND disposition == planned AND "
            "closure != closed AND "
            "(portfolio_priority in {P0,P1} OR "
            "(portfolio_priority == P2 AND p2_admission == ADMITTED_UNDER_SOFT))"
        ),
        "exclusion": {
            **t37_exclusion,
            "p2_blocked_count": len(
                (census.get("work_sets") or {}).get("p2_candidates") or []
            ),
            "p3_disposition_count": len(
                (census.get("work_sets") or {}).get("p3_disposition") or []
            ),
        },
        "merge_rule": (
            "same owner, domain, fidelity status, load status, and load verdict; "
            "must not cross fixed-card gates"
        ),
        "fixed_card_nodes": policy.get("fixed_card_nodes"),
        "fixed_cards": fixed_cards,
        "cards": cards,
        "owner_resolution": {
            owner: owner_resolution[owner] for owner in sorted(owner_resolution)
        },
        "validators": {
            "broken_dependencies": 0,
            "cycles": 0,
            "duplicate_owner_resolution": 0,
            "started_generated_cards": sum(1 for card in cards if card.get("started")),
            "t37_pilot_in_generated_cards": 0,
            "t37_gate_bypass": len(gate_bypass),
        },
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return t35.check_generated_document(OUTPUT, build())


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
        else:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(OUTPUT)
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T35 card topology failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "status": document.get("status"),
        "generated_card_count": document.get("generated_card_count"),
        "start_number": document.get("start_number"),
        "publication_delta": document.get("publication_delta"),
        "fixed_cards": [card.get("id") for card in document.get("fixed_cards") or []],
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
