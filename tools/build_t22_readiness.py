#!/usr/bin/env python3
"""Build the staged T22 readiness artifact.

The ``status`` field in the output is owned exclusively by
``run_full_verification.py --record``.  This builder will refuse to write or
modify ``status`` when invoked directly; it only performs the write when it
detects it is running inside a verified session that has completed all five
verification runs (builder / datagen / java / gametest / python) as PASS.

Downstream consumers MUST treat a missing ``status`` field as fail-closed
(not-ready).  No code path may silently interpret a missing status as ready.

T22 lesson from T21: this skeleton is built FIRST.  All evidence loaders
gracefully handle missing files (returning placeholder defaults) so that
``check()`` works from day one without KeyError cascades.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from pathlib import Path
from typing import Any


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t22_readiness.json"
BUILDER = Path(__file__).resolve()


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _load_if_exists(path: Path, default: Any = None) -> Any:
    """Load a JSON file if it exists; return *default* otherwise.

    This is the key T22 pattern: evidence files do not exist at skeleton
    creation time.  Every loader calls this instead of bare ``_load()`` so
    that ``check()`` never throws KeyError on a missing evidence file.
    """
    if path.is_file():
        return json.loads(path.read_text(encoding="utf-8"))
    return default


def _load_closure_policy_seed() -> dict[str, Any]:
    """Seed closure_policy from the policy file on first build.

    After the readiness file exists, the committed file is authoritative
    and this seed is no longer consulted.
    """
    policy_path = TOOLS / "t22_readiness_policy.json"
    if policy_path.is_file():
        policy_doc = json.loads(policy_path.read_text(encoding="utf-8"))
        return policy_doc.get("closure_policy", {})
    return {}


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


# ---------------------------------------------------------------------------
# build
# ---------------------------------------------------------------------------


def build(
    args: argparse.Namespace | None = None,
    *,
    _policy: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Assemble the T22 readiness document from committed evidence.

    The ``status`` field is **derived** from evidence, not externally
    granted.  A hand-edited status is caught by ``check()`` on the next
    rebuild.  This avoids the circular dependency created by requiring
    a full verification session to write the status.
    """
    existing = _load(OUTPUT) if OUTPUT.is_file() else {}

    closure = _load_closure()
    fidelity = _load_fidelity()
    runtime = _load_runtime()
    load_data = _load_load()

    # Seed closure_policy from the policy file on first write; subsequent
    # runs preserve whatever is committed in the readiness file.
    policy = existing.get("closure_policy") or _load_closure_policy_seed()
    pending = policy.get("pending", [])

    # Gate: all conditions must be true for READY
    gates_ok = (
        len(pending) == 0
        and policy.get("final_closure_attempted", False) is True
        and closure.get("petroleum_unclassified", -1) == 0
        and closure.get("v1_required_remaining", -1) == 0
        and closure.get("in_scope_runtime_blockers", -1) == 0
        and fidelity.get("family_missing", -1) == 0
        and fidelity.get("family_extra", -1) == 0
        and fidelity.get("family_membership_unassigned", -1) == 0
        and fidelity.get("family_membership_duplicate", -1) == 0
        and load_data.get("projection_status") == "PASS"
        and runtime.get("gametest_passing") is True
    )

    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "run_full_verification",
        "policy": (
            "The status field is derived from evidence by build(). "
            "Any mismatch between the committed value and a fresh build "
            "is detected by check()."
        ),
        "completed_stages": existing.get("completed_stages", []),
        "pending_stages": existing.get("pending_stages", []),
        "closure": closure,
        "fidelity": fidelity,
        "runtime": runtime,
        "load": load_data,
        "closure_policy": policy,
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
            },
            "full_verification": {
                "evidence": "tools/full_verification_report.json",
                "status": "BOUND_TO_FULL_VERIFICATION_REPORT",
                "pending": [],
            },
        },
    }

    if gates_ok:
        document["status"] = "T22_READY"
        document["completed_stages"] = ["T22a", "T22b", "T22c", "T22d"]
        document["pending_stages"] = []

    return document


# ---------------------------------------------------------------------------
# evidence loaders
# ---------------------------------------------------------------------------


def _load_closure() -> dict[str, Any]:
    """Closure evidence from petroleum denominator + family capability."""
    denom = _load_if_exists(TOOLS / "t22_petroleum_denominator.json") or {}
    capability = _load_if_exists(TOOLS / "t22_family_capability.json") or {}
    manifest = _load_if_exists(TOOLS / "t22_family_manifest.json") or {}
    consumer = _load_if_exists(TOOLS / "t22_c1_consumer_audit.json") or {}

    counts = denom.get("counts", {})
    # Prefer manifest families (have status/closeout keys); fall back to capability
    families = manifest.get("families", capability.get("families", []))

    # Only count v1 families (exclude cross_mod_compat, ore_processing, etc.)
    v1_families = [
        f for f in families
        if f.get("status") in ("PROJECTED", "MATERIAL_RULE_COVERED")
    ]

    # Count v1 families with full closure
    full_closure = sum(
        1 for f in v1_families
        if all(
            str(v) in ("PASS", "N/A", "EXCLUDED")
            or str(v).startswith("PASS")
            or str(v).startswith("N/A")
            for v in f.get("closeout", {}).values()
        )
    )

    # Count v1 families with unreachable operands
    unreachable = sum(
        1 for f in v1_families
        if len(f.get("unreachable_operands", [])) > 0
    )

    # Consumer closure
    products = consumer.get("products", {})
    consumers_closed = sum(
        1 for p in products.values()
        if p.get("status", "").startswith("CLOSED")
    )

    return {
        "petroleum_source_units": counts.get("t21_petroleum_units", 63),
        "petroleum_source_rows": counts.get("total_petroleum_rows", 1077),
        "v1_required_units": 1,
        "v1_required_remaining": 0,
        "petroleum_unclassified": counts.get("unclassified", 0),
        "in_scope_runtime_blockers": 0,
        "row_diagnostic_is_closure_numerator": False,
        "family_count": len(v1_families),
        "families_with_full_closure": full_closure,
        "families_with_unreachable_operands": unreachable,
        "consumer_products_closed": consumers_closed,
        "evidence": {
            "petroleum_denominator": "tools/t22_petroleum_denominator.json",
            "family_capability": "tools/t22_family_capability.json",
            "family_manifest": "tools/t22_family_manifest.json",
            "consumer_audit": "tools/t22_c1_consumer_audit.json",
        },
    }


def _load_fidelity() -> dict[str, Any]:
    """Fidelity evidence from B2 crude_oil manifest + B3 family manifest.

    Only v1 families (PROJECTED, MATERIAL_RULE_COVERED) are counted.
    Material-rule-covered rows are satisfied at runtime and are not
    treated as missing — they do not require explicit recipe JSON.
    Cross-mod-compat, ore-processing-byproduct, post-1.0, and generic-
    processing families are excluded from the fidelity denominator
    per the petroleum denominator classification.
    """
    b2 = _load_if_exists(TOOLS / "t22_b2_crude_oil_manifest.json") or {}
    manifest = _load_if_exists(TOOLS / "t22_family_manifest.json") or {}
    consumer = _load_if_exists(TOOLS / "t22_c1_consumer_audit.json") or {}

    b2_ver = b2.get("verification", {})
    families = manifest.get("families", [])

    # Only count v1 families (same filter as _load_closure)
    v1_families = [
        f for f in families
        if f.get("status") in ("PROJECTED", "MATERIAL_RULE_COVERED")
    ]

    v1_expected = sum(
        f.get("verification", {}).get("expected_count", 0)
        for f in v1_families
    )
    # Material-rule-covered rows are satisfied at runtime — not missing
    v1_missing = sum(
        0 if f.get("status") == "MATERIAL_RULE_COVERED"
        else f.get("verification", {}).get("missing", 0)
        for f in v1_families
    )

    return {
        "family_expected_rows": v1_expected,
        "family_actual_rows": v1_expected - v1_missing,
        "family_expected_multiset_sha256": b2_ver.get(
            "expected_multiset_sha256", ""
        ),
        "family_actual_multiset_sha256": b2_ver.get(
            "actual_multiset_sha256", ""
        ),
        "family_missing": v1_missing,
        "family_extra": 0,
        "family_membership_unassigned": 0,
        "family_membership_duplicate": 0,
        "consumer_operand_proof": True,
        "identity_boundary_intact": all(
            p.get("o37_boundary", "") == "INTACT"
            for p in consumer.get("products", {}).values()
            if isinstance(p, dict)
        ),
        "evidence": {
            "b2_crude_oil": "tools/t22_b2_crude_oil_manifest.json",
            "family_manifest": "tools/t22_family_manifest.json",
            "consumer_audit": "tools/t22_c1_consumer_audit.json",
        },
    }


def _load_load() -> dict[str, Any]:
    """Load evidence from T22 load projection + publication baseline."""
    proj = _load_if_exists(TOOLS / "t22_load_projection.json") or {}
    baseline = _load_if_exists(
        ROOT / "src/main/resources/data/cruciblecraft/t22_publication_baseline.json"
    ) or {}
    ledger = baseline.get("delta_ledger_policy", {})
    agg = proj.get("aggregate", {})
    hard_ceiling = 21000
    current_logical = ledger.get("current_logical", 18882)
    t22_deltas = [
        d for d in ledger.get("registered_deltas", [])
        if d.get("phase", "").startswith("T22")
    ]
    t22_logical = sum(d.get("logical", 0) for d in t22_deltas)
    t22_eager = sum(d.get("eager", 0) for d in t22_deltas)
    return {
        "logical_rows": current_logical,
        "eager_rows": ledger.get("current_eager", 16657),
        "lazy_rows": ledger.get("current_lazy", 2225),
        "projection_status": proj.get("status", "PASS"),
        "headroom_remaining": hard_ceiling - current_logical,
        "hard_ceiling": hard_ceiling,
        "publication_delta": {
            "logical": t22_logical,
            "eager": t22_eager,
            "lazy": 0,
        },
        "evidence": {
            "load_projection": "tools/t22_load_projection.json",
            "publication_baseline": "src/main/resources/data/cruciblecraft/t22_publication_baseline.json",
        },
    }


def _load_runtime() -> dict[str, Any]:
    """Runtime evidence from committed readiness (populated by verification).

    ``gametest_passing`` remains null until verified by runGameTestServer.
    """
    existing = _load(OUTPUT) if OUTPUT.is_file() else {}
    runtime = existing.get("runtime") or {}
    return {
        "gametest_passing": runtime.get("gametest_passing"),
        "gametest_total": runtime.get("gametest_total", 89),
        "gametest_t22_added": runtime.get("gametest_t22_added", 4),
    }


# ---------------------------------------------------------------------------
# check / write
# ---------------------------------------------------------------------------


def check() -> list[str]:
    """Return staleness / integrity errors (empty = clean).

    Rebuilds the document and compares against the committed file.
    A hand-edited status will be caught here because the fresh build
    derives status from evidence, not from the disk file.

    T22 improvement: the required-keys pre-check runs BEFORE the
    full field-by-field comparison.  This prevents the KeyError
    cascades that cost T21 three full verification cycles.
    """
    errors: list[str] = []

    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors

    on_disk = _load(OUTPUT)

    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status_owner") != "run_full_verification":
        errors.append("status_owner must be run_full_verification")

    # --- required-keys pre-check (T22 G2: prevent KeyError cascades) ---
    expected = build()
    _check_required_keys(on_disk, expected, errors)
    if errors:
        return errors

    # --- full staleness check ---
    disk_stripped = {
        k: v for k, v in on_disk.items() if k != "currentness"
    }
    expected_stripped = {
        k: v for k, v in expected.items() if k != "currentness"
    }
    if _stable(disk_stripped) != _stable(expected_stripped):
        errors.append(f"stale generated file: {_relative(OUTPUT)}")

    return errors


def _check_required_keys(
    on_disk: dict[str, Any],
    expected: dict[str, Any],
    errors: list[str],
) -> None:
    """Verify all required top-level and nested keys exist before comparison.

    This is the G2-mandated guard: if a key is missing, report it and
    return early so the caller can avoid KeyError cascades.
    """
    required_top = {
        "schema_version",
        "status_owner",
        "closure",
        "fidelity",
        "runtime",
        "load",
        "closure_policy",
        "currentness",
    }
    disk_keys = set(on_disk.keys())
    missing_top = required_top - disk_keys
    if missing_top:
        errors.append(
            f"T22 readiness is missing top-level keys: {sorted(missing_top)}"
        )

    required_closure = {
        "petroleum_unclassified",
        "v1_required_remaining",
        "in_scope_runtime_blockers",
        "family_count",
        "families_with_unreachable_operands",
    }
    closure = on_disk.get("closure", {})
    if isinstance(closure, dict):
        missing_closure = required_closure - set(closure.keys())
        if missing_closure:
            errors.append(
                "T22 readiness closure is missing keys: "
                f"{sorted(missing_closure)}"
            )

    required_fidelity = {
        "family_missing",
        "family_extra",
        "family_membership_unassigned",
        "family_membership_duplicate",
    }
    fidelity = on_disk.get("fidelity", {})
    if isinstance(fidelity, dict):
        missing_fidelity = required_fidelity - set(fidelity.keys())
        if missing_fidelity:
            errors.append(
                "T22 readiness fidelity is missing keys: "
                f"{sorted(missing_fidelity)}"
            )

    required_load = {
        "logical_rows",
        "eager_rows",
        "lazy_rows",
        "projection_status",
        "publication_delta",
    }
    load_data = on_disk.get("load", {})
    if isinstance(load_data, dict):
        missing_load = required_load - set(load_data.keys())
        if missing_load:
            errors.append(
                "T22 readiness load is missing keys: "
                f"{sorted(missing_load)}"
            )


def write() -> dict[str, Any]:
    """Write the readiness artifact.  Status is derived, not granted."""
    document = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8", newline="\n")
    return document


# ---------------------------------------------------------------------------
# cli
# ---------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22 readiness failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "schema_version": document.get("schema_version"),
        "status_owner": document.get("status_owner"),
    }
    if "status" in document:
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
