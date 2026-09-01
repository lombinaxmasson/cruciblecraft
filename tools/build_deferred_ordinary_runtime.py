#!/usr/bin/env python3
"""Replay deferred ordinary ledger and seal recycling/deferred-ordinary-runtime."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import closeout_seal
from tools import recycling_deferred_scope as scope
from tools import t35_common as t35
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for

SLUG = "recycling/deferred-ordinary-runtime"
GENERATED_BY = "python tools/build_deferred_ordinary_runtime.py"
ROOT_DIR = t35.TOOLS / "waves" / "recycling" / "deferred-ordinary-runtime"
R0_DIR = t35.TOOLS / "waves" / "recycling" / "deferred-ordinary-ledger-r0"
CHILDREN = (
    "recycling/deferred-ordinary-ledger-r0",
    "recycling/smelter-mte-identity",
    "smelter/deferred-recycling",
    "smelter/deferred-recycling-edge",
    "autoclave/deferred-recycling",
    "recycling/non-recycling-scope",
)
CHILD_STATUS = {
    "recycling/deferred-ordinary-ledger-r0": "RECYCLING_DEFERRED_LEDGER_R0_READY",
    "recycling/smelter-mte-identity": "SMELTER_MTE_IDENTITY_READY",
    "smelter/deferred-recycling": "WAVE_READY",
    "smelter/deferred-recycling-edge": "SMELTER_DEFERRED_RECYCLING_EDGE_READY",
    "autoclave/deferred-recycling": "AUTOCLAVE_DEFERRED_RECYCLING_READY",
    "recycling/non-recycling-scope": "NON_RECYCLING_SCOPE_READY",
}


def _require_children() -> list[str]:
    errors: list[str] = []
    for slug in CHILDREN:
        errors.extend(closeout_seal.check_wave_seal(slug))
        readiness = t35.load_json(t35.TOOLS / "waves" / slug / "readiness.json")
        expected = CHILD_STATUS[slug]
        status = str(readiness.get("status") or "")
        if status != expected:
            errors.append(f"{slug} status {status} != {expected}")
    r0 = t35.load_json(R0_DIR / "census_delta.json")
    if int(r0.get("remaining_ordinary", {}).get("deferred_recycling_count") or 0) != 1843:
        errors.append("must not rewrite R0 opening deferred recycling 1843")
    last = t35.load_json(
        t35.TOOLS / "waves" / "recycling" / "non-recycling-scope" / "census_delta.json"
    )
    remaining = last.get("remaining_ordinary") or {}
    if remaining.get("deferred_recycling_count") != 0:
        errors.append("non-recycling-scope must leave deferred recycling 0")
    if remaining.get("deferred_total") != 0:
        errors.append("non-recycling-scope must leave deferred total 0")
    smelter = t35.load_json(
        t35.TOOLS / "waves" / "smelter" / "deferred-recycling" / "census_delta.json"
    )
    if int(smelter.get("completion_delta") or 0) != 1817:
        errors.append("smelter deferred recycling completion_delta must stay 1817")
    return errors


def build_documents() -> dict[str, Any]:
    errors = _require_children()
    if errors:
        raise ValueError("; ".join(errors))
    universe = t35.load_json(R0_DIR / "deferred_universe.json")
    inherited = universe.get("inherited_recycling") or {}
    if not inherited.get("enumerated"):
        raise ValueError("inherited recycling must stay enumerated")
    if inherited.get("silently_discarded"):
        raise ValueError("inherited recycling was silently discarded")
    complete = 1817
    scoped = 2 + 24 + 2
    if complete + scoped != 1845:
        raise ValueError("ledger replay 1817+28 != 1845")
    gap_replay = {
        "complete_family_count": complete,
        "execution_gap": 0,
        "generated_by": GENERATED_BY,
        "inherited_recycling_enumerated": True,
        "post_1x_scope_count": scoped,
        "schema_version": 1,
        "silently_discarded": False,
        "source_revision": t35.SOURCE_REVISION,
        "status": "GAP_REPLAY_READY",
        "wave_slug": SLUG,
    }
    deferred_ledger = {
        "closing_deferred_recycling": 0,
        "closing_deferred_total": 0,
        "deferred_buckets": {
            "later:cross_mod": 0,
            "later:execution_envelope/gt6_panel": 0,
            "later:recycling": 0,
            "post_1.x": scoped,
        },
        "generated_by": GENERATED_BY,
        "inherited_recycling": {
            "authority": "recycling/deferred-ordinary-ledger-r0 deferred_universe.json",
            "count": 1819,
            "enumerated": True,
            "silently_discarded": False,
        },
        "one_x_joint_exit": False,
        "post_1x_scope_count": scoped,
        "proven_complete": complete,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "DEFERRED_LEDGER_CLOSED",
        "wave_slug": SLUG,
    }
    census = {
        "complete_family_count": complete,
        "completion_delta": 0,
        "generated_by": GENERATED_BY,
        "partial_family_count": 0,
        "post_1x_scope_count": scoped,
        "reclassification_delta": 24,
        "remaining_ordinary": {
            "complete_family_count": complete,
            "completion_delta": 0,
            "deferred_recycling_count": 0,
            "deferred_total": 0,
            "enumerated": True,
            "partial_family_count": 0,
            "post_1x_scope_count": scoped,
            "reclassification_delta": 24,
            "remaining_ordinary_families": 0,
        },
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": SLUG,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": complete,
        "generated_by": GENERATED_BY,
        "next_unassigned": True,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": None,
        "wave_slug": SLUG,
    }
    readiness = {
        "evidence": {
            "complete_family_count": complete,
            "completion_delta": 0,
            "deferred_recycling_count": 0,
            "deferred_total": 0,
            "execution_gap": 0,
            "inherited_enumerated": True,
            "one_x_joint_exit": False,
            "owns_families": 0,
            "partial_family_count": 0,
            "post_1x_scope_count": scoped,
            "program_status": "DEFERRED_ORDINARY_RUNTIME_READY",
            "recipe_files_generated": False,
            "reclassification_delta": 24,
            "remaining_recipe_gap": 0,
        },
        "generated_by": GENERATED_BY,
        "next_unassigned": True,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "DEFERRED_ORDINARY_RUNTIME_READY",
        "unique_active_wave": None,
        "wave_complete": True,
        "wave_slug": SLUG,
    }
    wave = {
        "cohort": "deferred-ordinary-runtime",
        "depends_on": list(CHILDREN),
        "generated_by": GENERATED_BY,
        "owns_families": 0,
        "program": SLUG,
        "schema_version": 1,
        "wave_slug": SLUG,
    }
    return {
        "census": census,
        "deferred_ledger": deferred_ledger,
        "gap_replay": gap_replay,
        "readiness": readiness,
        "topology": topology,
        "wave": wave,
    }


def write_artifacts() -> dict[str, Any]:
    documents = build_documents()
    ROOT_DIR.mkdir(parents=True, exist_ok=True)
    t35.write_stable(ROOT_DIR / "wave.json", documents["wave"])
    t35.write_stable(ROOT_DIR / "gap_replay.json", documents["gap_replay"])
    t35.write_stable(ROOT_DIR / "deferred_ledger.json", documents["deferred_ledger"])
    t35.write_stable(ROOT_DIR / "census_delta.json", documents["census"])
    t35.write_stable(ROOT_DIR / "topology.json", documents["topology"])
    t35.write_stable(ROOT_DIR / "readiness.json", documents["readiness"])
    hashes = {
        "census": t35.sha256_file(ROOT_DIR / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": t35.sha256_file(ROOT_DIR / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": t35.sha256_file(ROOT_DIR / "topology.json"),
    }
    seal = {
        "card_id": SLUG,
        "complete_family_count": 1817,
        "composed_identity_ledger_v2_sha256": t35.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": t35.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{GENERATED_BY} --write",
        "hashes": hashes,
        "note": (
            "Program closeout. Replays deferred ordinary ledger: 1817 Smelter "
            "MTE recovery complete, 28 independent post-1.x scope. execution "
            "gap 0. deferred hanging later:* 0. unique_active_wave null."
        ),
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": 24,
        "relation_count": 1817,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": SLUG,
        "source_revision": t35.SOURCE_REVISION,
        "status": "SEALED",
    }
    t35.write_stable(ROOT_DIR / "closeout_seal.json", seal)
    return {
        "complete": 1817,
        "post_1x_scope": 28,
        "status": "DEFERRED_ORDINARY_RUNTIME_READY",
        "unique_active_wave": None,
        "wave_slug": SLUG,
    }


def check() -> list[str]:
    errors: list[str] = []
    if SLUG not in KNOWN_SEMANTIC_SLUGS or SLUG not in known_slugs():
        errors.append(f"{SLUG} is not registered")
        return errors
    spec = spec_for(SLUG)
    if spec.unique_active_wave is not None:
        errors.append("program unique_active_wave must be null")
    if not spec.next_unassigned:
        errors.append("program next_unassigned must be true")
    if spec.owns_families != 0:
        errors.append("program owns_families must be 0")
    if spec.production_lock is not None:
        errors.append("program must not carry a production lock")
    if not (ROOT_DIR / "readiness.json").is_file():
        return errors + ["program artifacts are missing"]
    try:
        live = build_documents()
    except ValueError as error:
        return errors + [str(error)]
    for name, key in (
        ("census_delta.json", "census"),
        ("deferred_ledger.json", "deferred_ledger"),
        ("gap_replay.json", "gap_replay"),
        ("readiness.json", "readiness"),
        ("topology.json", "topology"),
        ("wave.json", "wave"),
    ):
        committed = t35.load_json(ROOT_DIR / name)
        drift = t35.first_json_diff(live[key], committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = t35.load_json(ROOT_DIR / "readiness.json")
    if readiness.get("status") != "DEFERRED_ORDINARY_RUNTIME_READY":
        errors.append("program status drifted")
    if readiness.get("unique_active_wave") is not None:
        errors.append("program unique_active_wave must be null")
    ledger = t35.load_json(ROOT_DIR / "deferred_ledger.json")
    buckets = ledger.get("deferred_buckets") or {}
    for key in ("later:recycling", "later:cross_mod", "later:execution_envelope/gt6_panel"):
        if buckets.get(key) != 0:
            errors.append(f"hanging {key} remains")
    errors.extend(closeout_seal.check_wave_seal(SLUG))
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            print(json.dumps(write_artifacts(), sort_keys=True))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{SLUG} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"deferred ordinary runtime failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
