#!/usr/bin/env python3
"""Shared T42-Owner paths, post-repair guards, and owner-track vocabulary."""
from __future__ import annotations

from collections import Counter
from pathlib import Path
from typing import Any, Iterable

from tools import t42_common as t42

ROOT = t42.ROOT
TOOLS = t42.TOOLS
SOURCE_REVISION = t42.SOURCE_REVISION

OWNER = "portfolio:track_a/t42_owner_partition"
OPENING_EXECUTION_GAP = 5300

PRE_FREEZE = TOOLS / "t42_owner_pre_freeze.json"
TRACK_OVERLAY = TOOLS / "t42_owner_track_overlay.json"
RECOVERY_EVIDENCE = TOOLS / "t42_owner_recovery_evidence.json"
OBJECT_EXPRESSION_EVIDENCE = TOOLS / "t42_owner_object_expression_evidence.json"
DISPOSITION_LOCK = TOOLS / "t42_owner_disposition_lock.json"
GAP_PARTITION = TOOLS / "t42_owner_gap_partition.json"
READINESS = TOOLS / "t42_owner_readiness.json"

BASE_ARTIFACTS = (
    "tools/t42_repair_readiness.json",
    "tools/t42_blocker_overlay.json",
    "tools/t42_disposition_lock.json",
    "tools/t42_gap_partition.json",
    "tools/t42_card_topology.json",
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
    return t42.load_json(path)


def sha256_file(path: Path) -> str:
    return t42.sha256_file(path)


def sha256_stable(value: Any) -> str:
    return t42.sha256_stable(value)


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return t42.check_document(path, document)


def parse_managed(description: str, argv: list[str] | None = None):
    return t42.parse_managed(description, argv)


def handle_rebind(args: Any, output: Path) -> bool:
    return t42.handle_rebind(args, output)


def _require(path: Path, name: str) -> dict[str, Any]:
    if not path.is_file():
        raise FileNotFoundError(f"T42-Owner requires {name}: {t42.relative(path)}")
    document = load_json(path)
    if not isinstance(document, dict):
        raise ValueError(f"T42-Owner {name} is not an object")
    return document


def repaired_t42() -> dict[str, dict[str, Any]]:
    """Load the post-T42-Repair authority and reject pre-repair inputs."""
    repair = _require(t42.REPAIR_READINESS, "T42-Repair readiness")
    if repair.get("status") != "T42_REPAIR_READY" or repair.get("failed_gates") != []:
        raise ValueError("T42-Owner requires T42_REPAIR_READY with failed_gates=[]")
    overlay = _require(t42.BLOCKER_OVERLAY, "repaired blocker overlay")
    lock = _require(t42.DISPOSITION_LOCK, "repaired disposition lock")
    gap = _require(t42.GAP_PARTITION, "repaired gap partition")
    topology = _require(t42.CARD_TOPOLOGY, "T42 topology")
    if overlay.get("family_count") != t42.OPENING_EXECUTION_GAP:
        raise ValueError("T42-Owner requires the repaired 5,305-family overlay")
    if lock.get("family_count") != t42.OPENING_EXECUTION_GAP:
        raise ValueError("T42-Owner requires the repaired 5,305-family lock")
    if gap.get("closing_execution_gap") != OPENING_EXECUTION_GAP:
        raise ValueError(
            "T42-Owner opening must be T42 post-repair closing 5,300"
        )
    retained = [
        row
        for row in lock.get("families") or []
        if row.get("disposition") == "retained_current_execution_gap"
    ]
    if len(retained) != OPENING_EXECUTION_GAP:
        raise ValueError(
            f"T42-Owner retained family count {len(retained)} != {OPENING_EXECUTION_GAP}"
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
    missing = [t42.relative(path) for path in paths if not path.is_file()]
    if missing:
        raise FileNotFoundError("missing T42-Owner base artifacts: " + ", ".join(missing))
    return {t42.relative(path): sha256_file(path) for path in paths}


def valid_owner_track(value: str | None) -> bool:
    return bool(value) and any(str(value).startswith(prefix) for prefix in OWNER_TRACK_PREFIXES)


def owner_counts(rows: Iterable[dict[str, Any]], key: str) -> dict[str, int]:
    counts = Counter(str(row.get(key) or "") for row in rows)
    return dict(sorted(counts.items()))
