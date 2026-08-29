#!/usr/bin/env python3
"""Plan or check the T40-VR verification dependency DAG."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t40_vr_common as vr

DAG = vr.VERIFICATION_DAG
MANIFESTS = (
    ROOT / "tools" / "t39_runtime_dependency_manifest.json",
    ROOT / "tools" / "t40_runtime_dependency_manifest.json",
)
RECEIPTS = (
    ROOT / "tools" / "t38_gametest_receipt.json",
    ROOT / "tools" / "t39_gametest_receipt.json",
    ROOT / "tools" / "t40_gametest_receipt.json",
)


def load_dag() -> dict[str, Any]:
    document = t35.load_json(DAG)
    nodes = document.get("nodes") or []
    ids = [node["id"] for node in nodes]
    if len(ids) != len(set(ids)):
        raise ValueError("verification DAG node ids are not unique")
    by_id = {node["id"]: node for node in nodes}
    for node in nodes:
        for dep in node.get("depends_on") or []:
            if dep not in by_id:
                raise ValueError(f"DAG node {node['id']} depends on missing {dep}")
    if _has_cycle(by_id):
        raise ValueError("verification DAG is cyclic")
    expected = [
        "source_and_material_gate",
        "generated_resources_and_runtime_manifest",
        "java_and_gametest_behavior",
        "receipt",
        "census_delta",
        "topology",
        "readiness",
        "t42_freeze",
        "t42_snapshot",
        "t42_inventory",
        "t42_b0",
        "t42_overlay",
        "t42_lock",
        "t42_gap_partition",
        "t42_census_delta",
        "t42_topology",
        "t42_readiness",
        "t42_repair_freeze",
        "t42_repair_readiness",
        "t42_owner_freeze",
        "t42_owner_overlay",
        "t42_owner_lock",
        "t42_owner_gap_partition",
        "t42_owner_readiness",
        "t36_repair_freeze",
        "t36_repair_inventory",
        "t36_repair_readiness",
    ]
    actual = [node["id"] for node in nodes]
    if actual != expected:
        raise ValueError(f"verification DAG order drifted: {actual}")
    return document


def _has_cycle(by_id: dict[str, dict[str, Any]]) -> bool:
    visiting: set[str] = set()
    seen: set[str] = set()

    def visit(node_id: str) -> bool:
        if node_id in visiting:
            return True
        if node_id in seen:
            return False
        visiting.add(node_id)
        for dep in by_id[node_id].get("depends_on") or []:
            if visit(dep):
                return True
        visiting.remove(node_id)
        seen.add(node_id)
        return False

    return any(visit(node_id) for node_id in by_id)


def topological_nodes(document: dict[str, Any]) -> list[dict[str, Any]]:
    return list(document.get("nodes") or [])


def stale_nodes(document: dict[str, Any] | None = None) -> list[dict[str, str]]:
    from tools import currentness

    document = document if document is not None else load_dag()
    stale: list[dict[str, str]] = []
    for node in topological_nodes(document):
        for relative in node.get("outputs") or []:
            path = ROOT / relative
            if not path.is_file():
                stale.append(
                    {
                        "node": node["id"],
                        "path": relative,
                        "drift_class": "MISSING",
                    }
                )
                continue
            if currentness.target_row(path) is None:
                continue
            for error in currentness.check_sidecar(path):
                drift_class = error.split(":", 1)[0]
                stale.append(
                    {
                        "node": node["id"],
                        "path": relative,
                        "drift_class": drift_class,
                        "error": error,
                    }
                )
    return stale


def manifests_current() -> bool:
    return all(path.is_file() for path in MANIFESTS)


def refuse_receipt_write() -> str | None:
    if not manifests_current():
        return "manifest stale: refusing receipt write"
    return None


def lifecycle_audit() -> dict[str, Any]:
    """Report frozen issuance metadata without rewriting snapshots."""
    work_set = t35.load_json(ROOT / "tools" / "t40_work_set.json")
    topology = t35.load_json(ROOT / "tools" / "t40_card_topology.json")
    readiness = t35.load_json(ROOT / "tools" / "t40_readiness.json")
    return {
        "work_set_unique_active_card": work_set.get("unique_active_card"),
        "topology_unique_active_card": topology.get("unique_active_card"),
        "readiness_unique_active_card": (readiness.get("evidence") or {}).get(
            "unique_active_card"
        ),
        "frozen_issuance_not_rewritten": True,
        "note": (
            "t40_work_set.json still records unique_active_card=T40; "
            "topology/readiness already use null. T41+ schema owns snapshot_phase."
        ),
    }


def plan() -> dict[str, Any]:
    document = load_dag()
    return {
        "nodes": [node["id"] for node in topological_nodes(document)],
        "stale": stale_nodes(document),
        "receipt_write_block": refuse_receipt_write(),
        "rebind_order": [
            node["id"]
            for node in topological_nodes(document)
            if "rebind" in (node.get("modes") or [])
        ],
        "lifecycle_audit": lifecycle_audit(),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--plan", action="store_true")
    mode.add_argument("--check", action="store_true")
    parser.add_argument("--json", type=Path)
    args = parser.parse_args(argv)
    try:
        document = plan() if args.plan or args.check else {}
        if args.plan:
            encoded = json.dumps(document, indent=2, sort_keys=True) + "\n"
            if args.json:
                from tools import atomic_io

                atomic_io.write_text(args.json, encoded)
            print(encoded, end="")
            return 0
        errors = [
            f"{row['drift_class']}: {row['path']}" for row in document["stale"]
        ]
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("verification DAG outputs are present")
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"verification DAG failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
