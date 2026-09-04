#!/usr/bin/env python3
"""Shared owner track paths, post-repair guards, and owner-track vocabulary."""
from __future__ import annotations

from collections import Counter
from pathlib import Path
from typing import Any, Iterable

from tools import owner_partition_common as owner

ROOT = owner.ROOT
TOOLS = owner.TOOLS
SOURCE_REVISION = owner.SOURCE_REVISION

OWNER = "portfolio:track_a/owner_runtime"
OPENING_EXECUTION_GAP = 5300

PRE_FREEZE = TOOLS / "owner_runtime_pre_freeze.json"
TRACK_OVERLAY = TOOLS / "owner_runtime_track_overlay.json"
RECOVERY_EVIDENCE = TOOLS / "owner_runtime_recovery_evidence.json"
OBJECT_EXPRESSION_EVIDENCE = TOOLS / "owner_runtime_object_expression_evidence.json"
DISPOSITION_LOCK = TOOLS / "owner_runtime_disposition_lock.json"
GAP_PARTITION = TOOLS / "owner_runtime_gap_partition.json"
READINESS = TOOLS / "owner_runtime_readiness.json"

BASE_ARTIFACTS = (
    "tools/owner_repair_readiness.json",
    "tools/owner_blocker_overlay.json",
    "tools/owner_disposition_lock.json",
    "tools/owner_gap_partition.json",
    "tools/owner_card_topology.json",
)

OWNER_TRACK_PREFIXES = (
    "recipe_wave/",
    "material_expression/",
    "recycling/",
    "object_expression/",
    "identity_mapping/",
    "registry_proof/",
    "acquisition/",
    "coordination/",
)

OWNER_STATES = frozenset(
    {
        "ready_for_production_lock",
        "bounded_expression_needed",
        "recovery_evidence_needed",
        "recovery_proven_for_lock",
        "object_boundary_needed",
        "identity_mapping_needed",
        "registry_proof_needed",
        "acquisition_path_needed",
        "multi_axis_coordination_needed",
        "phase_deferred",
    }
)


def load_json(path: Path) -> Any:
    return owner.load_json(path)


def sha256_file(path: Path) -> str:
    return owner.sha256_file(path)


def sha256_stable(value: Any) -> str:
    return owner.sha256_stable(value)


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return owner.check_document(path, document)


def parse_managed(description: str, argv: list[str] | None = None):
    return owner.parse_managed(description, argv)


def handle_rebind(args: Any, output: Path) -> bool:
    return owner.handle_rebind(args, output)


def _require(path: Path, name: str) -> dict[str, Any]:
    if not path.is_file():
        raise FileNotFoundError(f"owner track requires {name}: {owner.relative(path)}")
    document = load_json(path)
    if not isinstance(document, dict):
        raise ValueError(f"owner track {name} is not an object")
    return document


def repaired_owner_track() -> dict[str, dict[str, Any]]:
    """Load the post-owner repair authority and reject pre-repair inputs."""
    repair = _require(owner.REPAIR_READINESS, "owner repair readiness")
    if repair.get("status") != "OWNER_REPAIR_READY" or repair.get("failed_gates") != []:
        raise ValueError("owner track requires OWNER_REPAIR_READY with failed_gates=[]")
    overlay = _require(owner.BLOCKER_OVERLAY, "repaired blocker overlay")
    lock = _require(owner.DISPOSITION_LOCK, "repaired disposition lock")
    gap = _require(owner.GAP_PARTITION, "repaired gap partition")
    topology = _require(owner.CARD_TOPOLOGY, "owner topology")
    if overlay.get("family_count") != owner.OPENING_EXECUTION_GAP:
        raise ValueError("owner track requires the repaired 5,305-family overlay")
    if lock.get("family_count") != owner.OPENING_EXECUTION_GAP:
        raise ValueError("owner track requires the repaired 5,305-family lock")
    if gap.get("closing_execution_gap") != OPENING_EXECUTION_GAP:
        raise ValueError(
            "owner track opening must be owner post-repair closing 5,300"
        )
    retained = [
        row
        for row in lock.get("families") or []
        if row.get("disposition") == "retained_current_execution_gap"
    ]
    if len(retained) != OPENING_EXECUTION_GAP:
        raise ValueError(
            f"owner track retained family count {len(retained)} != {OPENING_EXECUTION_GAP}"
        )
    return {
        "gap": gap,
        "lock": lock,
        "overlay": overlay,
        "repair": repair,
        "topology": topology,
    }


def base_artifact_hashes() -> dict[str, str]:
    paths = [ROOT / value for value in BASE_ARTIFACTS]
    missing = [owner.relative(path) for path in paths if not path.is_file()]
    if missing:
        raise FileNotFoundError("missing owner track base artifacts: " + ", ".join(missing))
    return {owner.relative(path): sha256_file(path) for path in paths}


def valid_owner_track(value: str | None) -> bool:
    return bool(value) and any(str(value).startswith(prefix) for prefix in OWNER_TRACK_PREFIXES)


def owner_counts(rows: Iterable[dict[str, Any]], key: str) -> dict[str, int]:
    counts = Counter(str(row.get(key) or "") for row in rows)
    return dict(sorted(counts.items()))
