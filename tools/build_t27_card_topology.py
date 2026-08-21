#!/usr/bin/env python3
"""Derive T28+ implementation cards from the unclosed v1_required work set.

Card count is computed from the remaining set and merge rule. T27 does not
pre-fill T28/T29 counts, start those cards, or implement their content.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import build_t27_portfolio as portfolio  # noqa: E402
from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
BUILDER = Path(__file__).resolve()
OUTPUT = TOOLS / "t27_card_topology.json"
CONTRACT = TOOLS / "phase5_portfolio_contract.json"
AGGREGATE = portfolio.AGGREGATE
START_NUMBER = 28


def _node_id(table: str, ident: str) -> str:
    return f"{table}/{ident}"


def _pointer(item: dict[str, Any]) -> dict[str, str]:
    return {"canonical_id": item["canonical_id"], "table": item["table"]}


def _record_for(
    table: str,
    ident: str,
    table_docs: dict[str, dict[str, Any]],
    open_items: dict[str, Any],
) -> dict[str, Any]:
    if table == "deferred_open_items":
        matches = [
            row for row in open_items.get("records") or [] if row.get("id") == ident
        ]
    else:
        matches = [
            row
            for row in (table_docs[table].get("records") or [])
            if row.get("canonical_id") == ident
        ]
    if len(matches) != 1:
        raise ValueError(f"{table}/{ident} matches {len(matches)} source records")
    return matches[0]


def _work_item(
    table: str,
    ident: str,
    row: dict[str, Any],
    *,
    source_artifact: str,
) -> dict[str, Any]:
    axes = row.get("axes") or {}
    load = axes.get("load") or {}
    return {
        "axes": {
            "closure": dict(axes.get("closure") or {}),
            "fidelity": dict(axes.get("fidelity") or {}),
            "load": dict(load),
        },
        "canonical_id": ident,
        "cc_implementation": row.get("cc_implementation"),
        "closure": (axes.get("closure") or {}).get("status"),
        "disposition": row.get("disposition"),
        "fidelity": (axes.get("fidelity") or {}).get("status"),
        "load": load.get("status"),
        "load_verdict": load.get("verdict"),
        "owner": row.get("owner"),
        "reason": row.get("reason"),
        "recheck_point": row.get("recheck_point"),
        "replacement_condition": row.get("replacement_condition"),
        "source_artifact": source_artifact,
        "table": table,
        "dependencies": list(row.get("dependencies") or []),
    }


def select_work_set(
    table_docs: dict[str, dict[str, Any]],
    open_items: dict[str, Any],
) -> list[dict[str, Any]]:
    work: list[dict[str, Any]] = []
    for table, document in table_docs.items():
        source = common.relative(portfolio.output_path(table))
        for row in document.get("records") or []:
            if row.get("disposition") != "v1_required":
                continue
            if (row.get("axes") or {}).get("closure", {}).get("status") == "closed":
                continue
            work.append(
                _work_item(table, row["canonical_id"], row, source_artifact=source)
            )
    source = common.relative(portfolio.open_items_path())
    for row in open_items.get("records") or []:
        if row.get("kind") == "canonical_coverage":
            continue
        if row.get("disposition") != "v1_required":
            continue
        if (row.get("axes") or {}).get("closure", {}).get("status") == "closed":
            continue
        work.append(
            _work_item(
                "deferred_open_items",
                row["id"],
                row,
                source_artifact=source,
            )
        )
    work.sort(key=lambda item: (item["table"], item["canonical_id"], item["owner"]))
    return work


def work_edges(
    work: list[dict[str, Any]],
    table_docs: dict[str, dict[str, Any]],
    open_items: dict[str, Any],
) -> list[tuple[str, str]]:
    by_id: dict[str, list[tuple[str, str]]] = {}
    for table, document in table_docs.items():
        for row in document.get("records") or []:
            by_id.setdefault(row["canonical_id"], []).append((table, row["canonical_id"]))
    open_ids = {row["id"] for row in open_items.get("records") or []}
    work_nodes = {_node_id(item["table"], item["canonical_id"]) for item in work}
    work_lookup = {
        (item["table"], item["canonical_id"]): item for item in work
    }
    edges: list[tuple[str, str]] = []
    broken: list[str] = []
    for item in work:
        src = _node_id(item["table"], item["canonical_id"])
        for dep in item["dependencies"]:
            kind = dep.get("kind")
            target = str(dep.get("id") or "")
            if kind == "runtime_capability":
                if not target:
                    broken.append(f"{src} -> blank runtime_capability")
                continue
            if kind == "open_item_id":
                if target not in open_ids:
                    broken.append(f"{src} -> missing open_item {target}")
                    continue
                dest = _node_id("deferred_open_items", target)
                dest_item = work_lookup.get(("deferred_open_items", target))
                if dest_item is None:
                    coverage = next(
                        (
                            row
                            for row in open_items.get("records") or []
                            if row.get("id") == target
                        ),
                        None,
                    )
                    if coverage and coverage.get("kind") == "canonical_coverage":
                        continue
                    closed = next(
                        (
                            row
                            for row in open_items.get("records") or []
                            if row.get("id") == target
                            and (
                                row.get("disposition") == "closed"
                                or (row.get("axes") or {}).get("closure", {}).get("status")
                                == "closed"
                            )
                        ),
                        None,
                    )
                    if closed:
                        continue
                    broken.append(
                        f"{src} -> unclosed open_item {target} is missing from work set"
                    )
                    continue
                edges.append((src, dest))
                continue
            if kind == "canonical_id":
                matches = by_id.get(target) or []
                if len(matches) != 1:
                    broken.append(
                        f"{src} -> canonical_id {target} matches {len(matches)}"
                    )
                    continue
                dest_table, dest_id = matches[0]
                dest = _node_id(dest_table, dest_id)
                if dest not in work_nodes:
                    dest_row = _record_for(dest_table, dest_id, table_docs, open_items)
                    closure = (dest_row.get("axes") or {}).get("closure", {}).get("status")
                    if dest_row.get("disposition") == "v1_required" and closure != "closed":
                        broken.append(
                            f"{src} -> unclosed canonical {dest} is missing from work set"
                        )
                    continue
                edges.append((src, dest))
                continue
            broken.append(f"{src} -> unknown dependency kind {kind}")
    if broken:
        raise ValueError("broken dependencies: " + "; ".join(broken))
    return sorted(set(edges))


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


def _merge_key(item: dict[str, Any]) -> tuple[str, str, str, str]:
    return (
        str(item["owner"]),
        str(item["fidelity"] or ""),
        str(item["load"] or ""),
        str(item.get("load_verdict") or ""),
    )


def merge_and_number(
    work: list[dict[str, Any]],
    edges: list[tuple[str, str]],
) -> list[dict[str, Any]]:
    by_owner: dict[str, list[dict[str, Any]]] = {}
    for item in work:
        owner = item["owner"]
        if not isinstance(owner, str) or owner.strip() in common.OWNER_FORBIDDEN:
            raise ValueError(f"{item['table']}/{item['canonical_id']} has no owner")
        by_owner.setdefault(owner, []).append(item)
    groups: list[dict[str, Any]] = []
    for owner, items in by_owner.items():
        items = sorted(
            items,
            key=lambda item: (item["table"], item["canonical_id"], item["owner"]),
        )
        bounds = {_merge_key(item) for item in items}
        if len(bounds) != 1:
            raise ValueError(
                f"owner {owner} has mixed fidelity/load bounds and cannot merge"
            )
        groups.append({"owner": owner, "identities": items, "merge_key": next(iter(bounds))})
    node_to_owner = {
        _node_id(item["table"], item["canonical_id"]): item["owner"]
        for item in work
    }
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
                group["identities"][0]["table"],
                group["identities"][0]["canonical_id"],
                group["owner"],
            )
        )
        ranked.extend(bucket)
    cards: list[dict[str, Any]] = []
    owner_to_card: dict[str, str] = {}
    for index, group in enumerate(ranked):
        card_id = f"T{START_NUMBER + index}"
        owner_to_card[group["owner"]] = card_id
        identities = []
        for item in group["identities"]:
            identities.append(
                {
                    "canonical_id": item["canonical_id"],
                    "cc_implementation": item["cc_implementation"],
                    "closure": item["closure"],
                    "owner": item["owner"],
                    "source_artifact": item["source_artifact"],
                    "table": item["table"],
                }
            )
        axes = group["identities"][0]["axes"]
        load = dict(axes.get("load") or {})
        if load.get("status") == "pending":
            if load.get("verdict") != common.PENDING_LOAD_VERDICT:
                raise ValueError(f"{card_id} pending load is missing the blocked verdict")
            for key in common.FAKE_ZERO_LOAD_KEYS:
                if load.get(key) == 0:
                    raise ValueError(f"{card_id} pending load must not fill {key}=0")
        cards.append(
            {
                "id": card_id,
                "kind": "implementation",
                "started": False,
                "owner_keys": [group["owner"]],
                "merge_key": {
                    "fidelity": group["merge_key"][1],
                    "load": group["merge_key"][2],
                    "load_verdict": group["merge_key"][3] or None,
                    "owner": group["merge_key"][0],
                },
                "identities": identities,
                "depends_on": [],
                "replacement_conditions": sorted(
                    {
                        item["replacement_condition"]
                        for item in group["identities"]
                        if item.get("replacement_condition")
                    }
                ),
                "recheck_points": sorted(
                    {
                        item["recheck_point"]
                        for item in group["identities"]
                        if item.get("recheck_point")
                    }
                ),
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
                "publication_delta": {"eager": 0, "lazy": 0, "logical": 0},
            }
        )
    for card, group in zip(cards, ranked):
        deps = sorted(
            {
                owner_to_card[dst]
                for src, dst in set(group_edges)
                if src == group["owner"] and dst in owner_to_card
            }
        )
        card["depends_on"] = deps
    return cards


def _excluded(
    table_docs: dict[str, dict[str, Any]],
    open_items: dict[str, Any],
    work: list[dict[str, Any]],
) -> dict[str, Any]:
    closed_v1 = []
    post_1_0 = []
    out_of_scope = []
    for table, document in table_docs.items():
        for row in document.get("records") or []:
            pointer = _pointer({"table": table, "canonical_id": row["canonical_id"]})
            disposition = row.get("disposition")
            closure = (row.get("axes") or {}).get("closure", {}).get("status")
            if disposition == "v1_required" and closure == "closed":
                closed_v1.append(pointer)
            elif disposition == "post_1_0":
                post_1_0.append(pointer)
            elif disposition == "out_of_scope":
                out_of_scope.append(pointer)
    coverage = [
        row["id"]
        for row in open_items.get("records") or []
        if row.get("kind") == "canonical_coverage"
    ]
    rc_recheck = [
        row["id"]
        for row in open_items.get("records") or []
        if row.get("owner") == "T27 RC"
    ]
    work_ids = {(item["table"], item["canonical_id"]) for item in work}
    return {
        "canonical_coverage": sorted(coverage),
        "closed_v1": sorted(closed_v1, key=lambda item: (item["table"], item["canonical_id"])),
        "closed_v1_count": len(closed_v1),
        "out_of_scope_count": len(out_of_scope),
        "post_1_0_count": len(post_1_0),
        "rc_recheck": sorted(rc_recheck),
        "work_set": sorted(
            [{"canonical_id": ident, "table": table} for table, ident in work_ids],
            key=lambda item: (item["table"], item["canonical_id"]),
        ),
    }


def build() -> dict[str, Any]:
    errors = portfolio.check_aggregate()
    if errors:
        raise ValueError("; ".join(errors))
    contract = common.load_json(CONTRACT)
    generation = contract.get("t28_plus_generation") or {}
    if generation.get("card_count") is not None:
        raise ValueError("phase5 t28_plus_generation.card_count must remain null")
    if (contract.get("execution_policy") or {}).get("t28_plus_card_count") is not None:
        raise ValueError("phase5 t28_plus_card_count must remain null")
    if (contract.get("rc_policy") or {}).get("allowed_while_v1_set_nonempty") is not False:
        raise ValueError("RC numbering is forbidden while the v1 set is non-empty")
    aggregate = common.load_json(AGGREGATE)
    table_docs = {
        table: common.load_json(portfolio.output_path(table))
        for table in common.TABLE_SPECS
    }
    open_items = common.load_json(portfolio.open_items_path())
    work = select_work_set(table_docs, open_items)
    expected = {
        (item["table"], item["canonical_id"]) for item in aggregate.get("v1_work_set") or []
    }
    actual = {(item["table"], item["canonical_id"]) for item in work}
    equality = common.set_equality_errors(expected, actual, label="v1_work_set")
    if equality:
        raise ValueError("; ".join(equality))
    edges = work_edges(work, table_docs, open_items)
    cards = merge_and_number(work, edges)
    owner_resolution: dict[str, str] = {}
    for card in cards:
        for owner in card["owner_keys"]:
            if owner in owner_resolution:
                raise ValueError(f"owner {owner} resolved to multiple cards")
            owner_resolution[owner] = card["id"]
    if work and (contract.get("rc_policy") or {}).get("allowed_while_v1_set_nonempty"):
        raise ValueError("RC numbering is forbidden while generated v1 cards remain")
    excluded = _excluded(table_docs, open_items, work)
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(CONTRACT): common.sha256_file(CONTRACT),
        common.relative(AGGREGATE): common.sha256_file(AGGREGATE),
        common.relative(portfolio.open_items_path()): common.sha256_file(
            portfolio.open_items_path()
        ),
        **{
            common.relative(portfolio.output_path(table)): common.sha256_file(
                portfolio.output_path(table)
            )
            for table in common.TABLE_SPECS
        },
    }
    return {
        "schema_version": 1,
        "status": "T27_CARD_TOPOLOGY",
        "source_revision": common.SOURCE_REVISION,
        "generated_by": "python tools/build_t27_card_topology.py --write",
        "currentness": {"owned_inputs": owned_inputs},
        "publication_delta": {"eager": 0, "lazy": 0, "logical": 0},
        "started": False,
        "rc_number": None,
        "start_number": START_NUMBER,
        "card_count": len(cards),
        "selection": generation.get("selection")
        or "disposition == v1_required AND closure != closed",
        "merge_rule": generation.get("merge_rule"),
        "cards": cards,
        "owner_resolution": {
            owner: owner_resolution[owner] for owner in sorted(owner_resolution)
        },
        "excluded": excluded,
        "validators": {
            "broken_dependencies": 0,
            "cycles": 0,
            "duplicate_owner_resolution": 0,
            "rc_numbers": 0,
            "started_cards": 0,
        },
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = common.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
        else:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(OUTPUT)
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T27 card topology failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "card_count": document.get("card_count"),
        "publication_delta": document.get("publication_delta"),
        "rc_number": document.get("rc_number"),
        "started": document.get("started"),
        "status": document.get("status"),
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
