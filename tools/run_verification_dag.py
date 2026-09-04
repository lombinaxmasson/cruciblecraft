#!/usr/bin/env python3
"""Plan or check the electrolyzer/compact-VR verification dependency DAG."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import census_common as census
from tools import verification_runtime as vr

DAG = vr.VERIFICATION_DAG
MANIFESTS = (
    ROOT / "tools" / "centrifuge_runtime_dependency_manifest.json",
    ROOT / "tools" / "electrolyzer_runtime_dependency_manifest.json",
)
RECEIPTS = (
    ROOT / "tools" / "roaster_gametest_receipt.json",
    ROOT / "tools" / "centrifuge_gametest_receipt.json",
    ROOT / "tools" / "electrolyzer_gametest_receipt.json",
)


def load_dag() -> dict[str, Any]:
    document = census.load_json(DAG)
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
        "owner_freeze",
        "owner_snapshot",
        "owner_inventory",
        "owner_b0",
        "owner_overlay",
        "owner_lock",
        "owner_gap_partition",
        "owner_census_delta",
        "owner_topology",
        "owner_readiness",
        "owner_repair_freeze",
        "owner_repair_readiness",
        "owner_runtime_freeze",
        "owner_runtime_overlay",
        "owner_runtime_lock",
        "owner_runtime_gap_partition",
        "owner_runtime_readiness",
        "repair_freeze",
        "repair_inventory",
        "repair_readiness",
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
    work_set = census.load_json(ROOT / "tools" / "electrolyzer_work_set.json")
    topology = census.load_json(ROOT / "tools" / "electrolyzer_card_topology.json")
    readiness = census.load_json(ROOT / "tools" / "electrolyzer_readiness.json")
    return {
        "work_set_unique_active_card": work_set.get("unique_active_card"),
        "topology_unique_active_card": topology.get("unique_active_card"),
        "readiness_unique_active_card": (readiness.get("evidence") or {}).get(
            "unique_active_card"
        ),
        "frozen_issuance_not_rewritten": True,
        "note": (
            "electrolyzer_work_set.json still records unique_active_card=electrolyzer/compact; "
            "topology/readiness already use null. assembler/wood+ schema owns snapshot_phase."
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
