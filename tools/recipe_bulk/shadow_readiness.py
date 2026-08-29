#!/usr/bin/python3
"""Cross-ledger gate that closes the unified import shadow repair phase."""
from __future__ import annotations

from pathlib import Path
from typing import Any, Callable

from tools import t35_common as t35
from tools.recipe_bulk import identity as identity_mod
from tools.recipe_bulk import schema_lite
from tools.recipe_bulk.membership import identity_semantic_root
from tools.recipe_bulk.waves import SHADOW_ORDER

TOOLS = t35.TOOLS
STATUS_READY = "UNIFIED_IMPORT_SHADOW_READY"
STATUS_BLOCKED = "UNIFIED_IMPORT_SHADOW_BLOCKED"
TYPED_BLOCKER_CLASSES = frozenset(
    {
        "unproven_alias",
        "unproven_lossy_alias",
        "unbound_tag",
        "stateful_identity",
    }
)

BASELINE_PATH = TOOLS / "recipe_wave_production_baseline.json"
SHADOW_PATH = TOOLS / "recipe_wave_shadow_parity.json"
IDENTITY_PATH = TOOLS / "global_build_identity_ledger.json"
BASELINE_SCHEMA = TOOLS / "recipe_wave_production_baseline.schema.json"
SHADOW_SCHEMA = TOOLS / "recipe_wave_shadow_parity.schema.json"
IDENTITY_SCHEMA = TOOLS / "global_build_identity_ledger.schema.json"


def _load(path: Path) -> dict[str, Any]:
    return t35.load_json(path)


def _schema_ok(path: Path, schema_path: Path) -> bool:
    try:
        schema_lite.validate(_load(path), _load(schema_path))
    except (OSError, ValueError, schema_lite.SchemaError):
        return False
    return True


def _shadow_ok() -> bool:
    shadow = _load(SHADOW_PATH)
    return (
        shadow.get("ok") is True
        and shadow.get("rebuilds_identical") is True
        and shadow.get("status") == "RECIPE_WAVE_SHADOW_PARITY"
        and list(shadow.get("shadow_order") or []) == list(SHADOW_ORDER)
    )


def _baseline_ok() -> bool:
    baseline = _load(BASELINE_PATH)
    return (
        baseline.get("status") == "RECIPE_WAVE_PRODUCTION_BASELINE"
        and set(baseline.get("waves") or {}) == set(SHADOW_ORDER)
    )


def _cross_ledger_ok() -> bool:
    baseline = _load(BASELINE_PATH)
    shadow = _load(SHADOW_PATH)
    for wave_id in SHADOW_ORDER:
        base = (baseline.get("waves") or {}).get(wave_id) or {}
        row = (shadow.get("waves") or {}).get(wave_id) or {}
        if not row.get("ok"):
            return False
        if row.get("byte_identity") is not True:
            return False
        if row.get("stable_id_sha256") != base.get("stable_id_sha256"):
            return False
        if row.get("production_stable_id_sha256") != base.get("stable_id_sha256"):
            return False
        if row.get("stable_id_sha256") != row.get("production_stable_id_sha256"):
            return False
        if int(row.get("family_count") or -1) != int(base.get("file_count") or -2):
            return False
        if int(row.get("relation_count") or -1) != int(base.get("stable_id_count") or -2):
            return False
        if int(row.get("consume_collision_count") or 0) != 0:
            return False
        if row.get("mismatches"):
            return False
    return True


def _typed_blockers_ok() -> bool:
    identity = _load(IDENTITY_PATH)
    if identity.get("status") != "GLOBAL_BUILD_IDENTITY_LEDGER_V1":
        return False
    for row in identity.get("blockers") or []:
        mapping = str(row.get("mapping_class") or "")
        reason = str(row.get("blocker_reason") or "")
        if mapping not in TYPED_BLOCKER_CLASSES:
            return False
        if not reason:
            return False
        if mapping not in identity_mod.MAPPING_CLASSES:
            return False
    return True


def _owns_no_families() -> bool:
    return True


GATE_PROBES: tuple[tuple[str, Callable[[], bool]], ...] = (
    ("baseline_schema", lambda: _schema_ok(BASELINE_PATH, BASELINE_SCHEMA)),
    ("shadow_schema", lambda: _schema_ok(SHADOW_PATH, SHADOW_SCHEMA)),
    ("identity_schema", lambda: _schema_ok(IDENTITY_PATH, IDENTITY_SCHEMA)),
    ("baseline_status", _baseline_ok),
    ("shadow_zero_drift", _shadow_ok),
    ("cross_ledger_stable_ids", _cross_ledger_ok),
    ("typed_identity_blockers_only", _typed_blockers_ok),
    ("owns_no_families", _owns_no_families),
)


def build() -> dict[str, Any]:
    gates = {name: probe() for name, probe in GATE_PROBES}
    failed = sorted(name for name, passed in gates.items() if not passed)
    identity = _load(IDENTITY_PATH)
    shadow = _load(SHADOW_PATH)
    baseline = _load(BASELINE_PATH)
    wave_bindings = []
    for wave_id in SHADOW_ORDER:
        base = (baseline.get("waves") or {}).get(wave_id) or {}
        row = (shadow.get("waves") or {}).get(wave_id) or {}
        wave_bindings.append(
            {
                "byte_identity": row.get("byte_identity"),
                "family_count": row.get("family_count"),
                "relation_count": row.get("relation_count"),
                "stable_id_sha256": row.get("stable_id_sha256"),
                "wave_id": wave_id,
                "generated_tree_sha256": base.get("generated_tree_sha256"),
            }
        )
    return {
        "failed_gates": failed,
        "gates": gates,
        "generated_by": "python tools/build_unified_import_shadow_readiness.py",
        "identity_semantic_root_sha256": identity_semantic_root(
            list(identity.get("records") or []) + list(identity.get("blockers") or [])
        ),
        "note": (
            "UNIFIED_IMPORT_SHADOW_READY closes the Phase-1 shadow repair gate. "
            "Known typed identity blockers stay recorded and do not block the "
            "runtime-policy cutover. Unclassified blockers fail closed. "
            "This does not occupy T46 or deduct the 2,697 gap."
        ),
        "owns_families": 0,
        "rebuilds_identical": shadow.get("rebuilds_identical"),
        "schema_version": 1,
        "status": STATUS_READY if not failed else STATUS_BLOCKED,
        "typed_identity_blocker_count": int(identity.get("blocker_count") or 0),
        "wave_bindings": wave_bindings,
    }
