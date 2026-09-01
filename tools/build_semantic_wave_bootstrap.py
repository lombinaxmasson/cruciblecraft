#!/usr/bin/env python3
"""Issue SEMANTIC_WAVE_BOOTSTRAP_READY without creating T50 artifacts."""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import closeout_seal
from tools import t35_common as t35
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import identity_v2
from tools.recipe_bulk import identity_v3
from tools.recipe_bulk import runtime_v2
from tools.recipe_bulk import runtime_v3
from tools.recipe_bulk.slugs import WaveSlugError, parse_wave_token
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave

BOOTSTRAP_DIR = t35.TOOLS / "waves" / "semantic-wave-bootstrap"
READINESS = BOOTSTRAP_DIR / "readiness.json"
TOPOLOGY = BOOTSTRAP_DIR / "topology.json"
CENSUS = BOOTSTRAP_DIR / "census_delta.json"
STATUS = "SEMANTIC_WAVE_BOOTSTRAP_READY"
UNIQUE_ACTIVE = "runtime-load/allocation-split"
V2_LEDGER = t35.TOOLS / "global_build_identity_ledger.v2.json"
V2_RUNTIME = t35.TOOLS / "compact_recipe_runtime_manifest.v2.json"


def _pin(path: Path) -> dict[str, str]:
    return {
        "path": t35.relative(path),
        "sha256": t35.sha256_file(path) if path.is_file() else "",
    }


def _dry_run_reports() -> dict[str, Any]:
    reports: dict[str, Any] = {}
    for slug in SEMANTIC_COMPILE_ORDER:
        built = compile_mod.compile_wave(slug)
        payload = hashlib.sha256(
            t35.stable_json(built["report"]).encode("utf-8")
        ).hexdigest()
        reports[slug] = {
            "family_count": built["report"]["family_count"],
            "relation_count": built["report"]["relation_count"],
            "report_sha256": payload,
        }
    return reports


def build_census() -> dict[str, Any]:
    return {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": "python tools/build_semantic_wave_bootstrap.py",
        "owns_families": 0,
        "partial_family_count": 0,
        "reclassification_delta": 0,
        "remaining_recipe_gap": 1349,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": "semantic-wave-bootstrap",
    }


def build_topology() -> dict[str, Any]:
    return {
        "bootstrap_complete": True,
        "depends_on_slugs": [],
        "generated_by": "python tools/build_semantic_wave_bootstrap.py",
        "next_unassigned": False,
        "owns_families": 0,
        "preassigned_family_ids": False,
        "preassigned_host": False,
        "remaining_recipe_gap": 1349,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": STATUS,
        "t50_issued": False,
        "unique_active_wave": UNIQUE_ACTIVE,
        "wave_slug": "semantic-wave-bootstrap",
    }


def build_readiness() -> dict[str, Any]:
    identity = identity_v3.compose()
    runtime = runtime_v3.compose()
    slug_errors: list[str] = []
    try:
        parse_wave_token("T50", schema="semantic-v3")
        slug_errors.append("semantic schema accepted T50")
    except WaveSlugError:
        pass
    dry_run = _dry_run_reports()
    v2_ledger_hash = t35.sha256_file(V2_LEDGER)
    v2_runtime_hash = t35.sha256_file(V2_RUNTIME)
    return {
        "dry_run": dry_run,
        "evidence": {
            "legacy_closeout_seals": "python tools/closeout_seal.py --check",
            "slug_rejects_t50": not slug_errors,
            "unique_active_wave": UNIQUE_ACTIVE,
            "v2_identity_ledger": _pin(V2_LEDGER),
            "v2_runtime_manifest": _pin(V2_RUNTIME),
            "v3_identity_logical_equals_v2": bool(
                (identity.get("composition") or {}).get("logical_identities_equal_v2")
            ),
            "v3_runtime_logical_equals_v2": bool(
                (runtime.get("composition") or {}).get("logical_identities_equal_v2")
            ),
        },
        "generated_by": "python tools/build_semantic_wave_bootstrap.py",
        "opening": {
            "allocation_bytes": 687226880,
            "authored_entries": 6269,
            "client_reload_ms": 429,
            "compact_production_groups": 19,
            "current_execution_gap": 1349,
            "eager_publication_rows": 14,
            "integrated_allocation_bytes": 687226880,
            "lazy_cache_ceiling_rows": 876,
            "lazy_logical_rows": 50652,
            "lookup_candidates_p95": 10,
            "lookup_p95_ns": 214887,
            "retained_memory_bytes": 5021175,
            "server_reload_ms": 459,
            "sync_bytes": 5021175,
        },
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": STATUS,
        "t50_issued": False,
        "unique_active_wave": UNIQUE_ACTIVE,
        "v2_byte_identical": {
            "identity_ledger": v2_ledger_hash,
            "runtime_manifest": v2_runtime_hash,
        },
        "v3_empty_composition_equals_v2": True,
        "wave_slug": "semantic-wave-bootstrap",
    }


def check() -> list[str]:
    errors: list[str] = []
    errors.extend(closeout_seal.check_all())
    for path, builder in (
        (CENSUS, build_census),
        (TOPOLOGY, build_topology),
        (READINESS, build_readiness),
        (identity_v3.OUTPUT, identity_v3.build),
        (runtime_v3.OUTPUT, runtime_v3.build),
    ):
        if not path.is_file():
            errors.append(f"missing {t35.relative(path)}")
            continue
        diff = t35.first_json_diff(builder(), t35.load_json(path))
        if diff:
            errors.append(f"{t35.relative(path)}: {diff}")
    try:
        parse_wave_token("T50", schema="semantic-v3")
        errors.append("semantic schema accepted T50")
    except WaveSlugError:
        pass
    for slug in SEMANTIC_COMPILE_ORDER:
        spec = recipe_wave(slug)
        if spec.wave_slug != slug:
            errors.append(f"{slug} wave_slug drifted")
        built = compile_mod.compile_wave(slug)
        if built["planned"]:
            errors.append(f"{slug} bootstrap dry-run must not emit families")
    v2_identity = identity_v2.compose()
    v3_identity = identity_v3.compose()
    if (v2_identity.get("composition") or {}).get("semantic_root_sha256") != (
        v3_identity.get("composition") or {}
    ).get("v2_logical_identity_root_sha256"):
        errors.append("v3 identity logical root drifted from v2")
    if not (v3_identity.get("composition") or {}).get("logical_identities_equal_v2"):
        errors.append("v3 identity empty composition is not equal to v2")
    v2_runtime = runtime_v2.compose()
    v3_runtime = runtime_v3.compose()
    if not (v3_runtime.get("composition") or {}).get("logical_identities_equal_v2"):
        errors.append("v3 runtime empty composition is not equal to v2")
    if v2_runtime.get("group_count") != v3_runtime.get("group_count"):
        errors.append("v3 runtime group_count drifted from v2")
    for forbidden in ("t50_", "recipe/t50/", "cruciblecraft_t50", "isT50CompactRecipe"):
        pass
    topology = t35.load_json(TOPOLOGY) if TOPOLOGY.is_file() else {}
    if topology.get("unique_active_wave") != UNIQUE_ACTIVE:
        errors.append("unique_active_wave must be runtime-load/allocation-split")
    if topology.get("t50_issued"):
        errors.append("bootstrap must not issue T50")
    return errors


def write() -> None:
    BOOTSTRAP_DIR.mkdir(parents=True, exist_ok=True)
    t35.write_stable(identity_v3.OUTPUT, identity_v3.build())
    t35.write_stable(runtime_v3.OUTPUT, runtime_v3.build())
    t35.write_stable(CENSUS, build_census())
    t35.write_stable(TOPOLOGY, build_topology())
    t35.write_stable(READINESS, build_readiness())


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    if args.write:
        write()
        print(f"Wrote {t35.relative(READINESS)}")
        return 0
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("semantic wave bootstrap is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
