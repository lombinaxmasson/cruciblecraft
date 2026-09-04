#!/usr/bin/env python3
"""Shared GameTest behavior vs currentness roots for roaster/compact/centrifuge/compact/electrolyzer/compact receipts."""
from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Any

from tools import census_common as census


def equivalence_roots(equivalence_path: Path) -> dict[str, Any]:
    if not equivalence_path.is_file():
        return {
            "semantic_tree_sha256": None,
            "equivalence_root_sha256": None,
            "relation_fingerprints": {},
        }
    document = census.load_json(equivalence_path)
    fingerprints = dict(document.get("relation_fingerprints") or {})
    semantic_tree = hashlib.sha256(
        census.stable_json(
            {
                "generated_sha256": (document.get("generated") or {}).get("sha256"),
                "relation_fingerprints": fingerprints,
            }
        ).encode("utf-8")
    ).hexdigest()
    equivalence_root = hashlib.sha256(
        census.stable_json(
            {
                "semantic_tree_sha256": semantic_tree,
                "stable_ids": sorted(fingerprints),
            }
        ).encode("utf-8")
    ).hexdigest()
    return {
        "semantic_tree_sha256": semantic_tree,
        "equivalence_root_sha256": equivalence_root,
        "relation_fingerprints": fingerprints,
    }


BEHAVIOR_BOUND_KEYS = frozenset(
    {
        "gametest_java",
        "material_registration_gate_java",
        "material_registration_gate_json",
        "production_lock",
        "roaster_generated_recipes",
        "roaster_player_path_recovery",
        "centrifuge_generated_recipes",
        "centrifuge_locked_support",
        "electrolyzer_generated_recipes",
        "electrolyzer_locked_support",
        "assembler_wood_generated_recipes",
        "assembler_wood_locked_support",
        "bath_mte_generated_recipes",
        "bath_mte_locked_support",
        "bath_remainder_generated_recipes",
        "bath_remainder_locked_support",
        "bath_identity_generated_recipes",
        "bath_identity_locked_support",
    }
)


def split_bound(bound: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
    behavior = {
        key: bound[key] for key in sorted(bound) if key in BEHAVIOR_BOUND_KEYS
    }
    currentness = {
        key: bound[key] for key in sorted(bound) if key not in BEHAVIOR_BOUND_KEYS
    }
    return behavior, currentness


def bound_drift_errors(
    card: str,
    receipt_bound: dict[str, Any],
    live_bound: dict[str, Any],
) -> list[str]:
    receipt_behavior, receipt_currentness = split_bound(receipt_bound)
    live_behavior, live_currentness = split_bound(live_bound)
    errors: list[str] = []
    if receipt_behavior != live_behavior:
        errors.append(
            f"{card} GameTest receipt behavior bound artifacts drifted; "
            "rerun isolated GameTest"
        )
    if receipt_currentness != live_currentness:
        errors.append(
            f"{card} GameTest receipt currentness bound artifacts drifted; "
            "rebind with --from-log"
        )
    return errors


def behavior_root_sha256(
    *,
    bound_behavior: dict[str, Any],
    test_ids: list[str],
    parsed: dict[str, Any],
    lock_sha256: str | None,
    equivalence: dict[str, Any],
) -> str:
    payload = {
        "bound_behavior": bound_behavior,
        "equivalence_root_sha256": equivalence.get("equivalence_root_sha256"),
        "lock_sha256": lock_sha256,
        "parsed_status": parsed.get("status"),
        "semantic_tree_sha256": equivalence.get("semantic_tree_sha256"),
        "test_ids": list(test_ids),
    }
    return hashlib.sha256(census.stable_json(payload).encode("utf-8")).hexdigest()


def currentness_root_sha256(
    *,
    bound_currentness: dict[str, Any],
    log_path: str,
    log_fingerprint: str,
    command: str,
) -> str:
    payload = {
        "bound_currentness": bound_currentness,
        "command": command,
        "log_fingerprint": log_fingerprint,
        "log_path": log_path,
    }
    return hashlib.sha256(census.stable_json(payload).encode("utf-8")).hexdigest()


def attach_receipt_roots(
    document: dict[str, Any],
    *,
    bound: dict[str, Any],
    test_ids: list[str],
    parsed: dict[str, Any],
    command: str,
    log_path: str,
    lock_sha256: str | None,
    equivalence_path: Path,
    log_fingerprint_value: str,
) -> dict[str, Any]:
    equivalence = equivalence_roots(equivalence_path)
    behavior, currentness = split_bound(bound)
    document["semantic_tree_sha256"] = equivalence["semantic_tree_sha256"]
    document["equivalence_root_sha256"] = equivalence["equivalence_root_sha256"]
    document["behavior_root_sha256"] = behavior_root_sha256(
        bound_behavior=behavior,
        test_ids=test_ids,
        parsed=parsed,
        lock_sha256=lock_sha256,
        equivalence=equivalence,
    )
    document["currentness_root_sha256"] = currentness_root_sha256(
        bound_currentness=currentness,
        log_path=log_path,
        log_fingerprint=log_fingerprint_value,
        command=command,
    )
    return document
