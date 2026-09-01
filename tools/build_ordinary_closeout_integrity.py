#!/usr/bin/env python3
"""Repair semantic-wave closeout derivation and Smelter/Mixer load measurements."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import closeout_seal
from tools import t35_common as t35
from tools.build_ordinary_wave import check_wave, rebuild_closeout
from tools.recipe_bulk import ordinary_wave as wave
from tools.wave_closeout import seal_path as wave_seal_path

REPAIR_SLUG = "ordinary-wave/closeout-integrity-repair"
AFFECTED = ("smelter/ordinary-closure", "mixer/ordinary-closure")
REPAIR_ROOT = t35.TOOLS / "waves" / "ordinary-wave" / "closeout-integrity-repair"
FLAT_AXES = (
    "client_reload_ms",
    "client_index_ms",
    "reload_transient_allocation_bytes",
    "lookup_allocation_bytes_per_operation",
    "retained_memory_bytes",
    "server_reload_ms",
    "server_index_ms",
    "sync_bytes",
    "lookup_p95_ns",
    "lookup_candidate_count",
    "eager_publication_rows",
    "lazy_logical_rows",
    "lazy_cache_ceiling_rows",
)


def _sha_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _tree_hash(root: Path | None) -> str | None:
    if root is None or not root.exists():
        return None
    hasher = hashlib.sha256()
    files = sorted(path for path in root.rglob("*") if path.is_file())
    for path in files:
        hasher.update(path.relative_to(root).as_posix().encode("utf-8"))
        hasher.update(path.read_bytes())
    return hasher.hexdigest()


def flatten_integrated(document: dict[str, Any]) -> dict[str, Any]:
    return wave.flatten_integrated(document)


def snapshot_pre_repair() -> dict[str, Any]:
    REPAIR_ROOT.mkdir(parents=True, exist_ok=True)
    seals: dict[str, Any] = {}
    locks: dict[str, Any] = {}
    trees: dict[str, Any] = {}
    copies: dict[str, str] = {}
    for slug in AFFECTED:
        spec_root = wave.wave_dir(slug)
        seal = spec_root / "closeout_seal.json"
        if not seal.is_file():
            raise ValueError(f"missing live seal to snapshot: {seal}")
        dest = REPAIR_ROOT / f"pre_repair_{slug.replace('/', '_')}_closeout_seal.json"
        if not dest.is_file():
            shutil.copyfile(seal, dest)
        copies[slug] = t35.relative(dest)
        seals[slug] = {
            "path": t35.relative(seal),
            "pre_repair_copy": t35.relative(dest),
            "sha256": _sha_file(dest),
        }
        lock = spec_root / "production_lock.json"
        locks[slug] = t35.sha256_file(lock)
        generated = (
            t35.ROOT
            / "src/recipe_generated/resources/data/cruciblecraft/recipe"
            / slug.split("/")[0]
            / "ordinary_closure"
        )
        trees[slug] = _tree_hash(generated)
    document = {
        "affected_waves": list(AFFECTED),
        "generated_by": "python tools/build_ordinary_closeout_integrity.py",
        "generated_tree_sha256": trees,
        "production_lock_sha256": locks,
        "schema_version": 1,
        "seals": seals,
        "status": "PRE_REPAIR_SNAPSHOT",
        "wave_slug": REPAIR_SLUG,
    }
    t35.write_stable(REPAIR_ROOT / "pre_repair_hashes.json", document)
    t35.write_stable(
        REPAIR_ROOT / "affected_seals.json",
        {
            "copies": copies,
            "generated_by": "python tools/build_ordinary_closeout_integrity.py",
            "schema_version": 1,
            "seals": seals,
            "wave_slug": REPAIR_SLUG,
        },
    )
    return document


def apply_integrated_measurements() -> dict[str, str]:
    written: dict[str, str] = {}
    mapping = {
        "smelter/ordinary-closure": REPAIR_ROOT / "smelter_integrated_measurements.json",
        "mixer/ordinary-closure": REPAIR_ROOT / "mixer_integrated_measurements.json",
    }
    for slug, path in mapping.items():
        if not path.is_file():
            continue
        flat = flatten_integrated(t35.load_json(path))
        dest = wave.wave_dir(slug) / "measurements.json"
        t35.write_stable(dest, flat)
        written[slug] = t35.relative(dest)
    return written


def rebuild_affected() -> dict[str, Any]:
    summaries = {}
    for slug in AFFECTED:
        summaries[slug] = rebuild_closeout(slug)
    return summaries


def write_repair_artifacts(summaries: dict[str, Any]) -> dict[str, Any]:
    mixer = t35.load_json(wave.wave_dir("mixer/ordinary-closure") / "census_delta.json")
    smelter = t35.load_json(wave.wave_dir("smelter/ordinary-closure") / "census_delta.json")
    smelter_ready = wave.evaluate_live_wave_ready("smelter/ordinary-closure")
    mixer_ready = wave.evaluate_live_wave_ready("mixer/ordinary-closure")
    pending = 0
    for slug in AFFECTED:
        load = t35.load_json(wave.wave_dir(slug) / "load_projection.json")
        pending += len(wave.load_measurement_blockers(load))
    both_ready = bool(smelter_ready.get("ready") and mixer_ready.get("ready"))
    unique_active = None if both_ready else REPAIR_SLUG
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": "python tools/build_ordinary_closeout_integrity.py",
        "opening_execution_gap": 334,
        "partial_family_count": 0,
        "reclassification_delta": 0,
        "remaining_recipe_gap": int(mixer["remaining_recipe_gap"]),
        "schema_version": 1,
        "smelter_remaining_recipe_gap": int(smelter["remaining_recipe_gap"]),
        "source_revision": t35.SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": REPAIR_SLUG,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": "python tools/build_ordinary_closeout_integrity.py",
        "next_unassigned": both_ready,
        "remaining_recipe_gap": int(mixer["remaining_recipe_gap"]),
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "WAVE_READY" if both_ready else "LOAD_PENDING_MEASUREMENT",
        "unique_active_wave": unique_active,
        "wave_slug": REPAIR_SLUG,
    }
    readiness = {
        "evidence": {
            "affected": {
                "mixer/ordinary-closure": mixer_ready,
                "smelter/ordinary-closure": smelter_ready,
            },
            "complete_family_count": 0,
            "completion_delta": 0,
            "load_pending_axes": pending,
            "partial_family_count": 0,
            "remaining_recipe_gap": int(mixer["remaining_recipe_gap"]),
        },
        "generated_by": "python tools/build_ordinary_closeout_integrity.py",
        "next_unassigned": both_ready,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "WAVE_READY" if both_ready else "LOAD_PENDING_MEASUREMENT",
        "unique_active_wave": unique_active,
        "wave_complete": both_ready,
        "wave_slug": REPAIR_SLUG,
    }
    t35.write_stable(
        REPAIR_ROOT / "wave.json",
        {
            "cohort": "closeout-integrity-repair",
            "depends_on": ["mixer/ordinary-closure"],
            "owns_families": 0,
            "schema_version": 1,
            "unique_active_wave": unique_active is not None,
            "wave_slug": REPAIR_SLUG,
        },
    )
    t35.write_stable(
        REPAIR_ROOT / "measurement_manifest.json",
        {
            "generated_by": "python tools/build_ordinary_closeout_integrity.py",
            "mixes": {
                "mixer": {
                    "artifact": t35.relative(
                        REPAIR_ROOT / "mixer_integrated_measurements.json"
                    ),
                    "groups": "historical compact + Smelter + Mixer",
                },
                "smelter": {
                    "artifact": t35.relative(
                        REPAIR_ROOT / "smelter_integrated_measurements.json"
                    ),
                    "groups": "historical compact + Smelter",
                },
            },
            "schema_version": 1,
            "status": "MEASUREMENT_MANIFEST",
            "wave_slug": REPAIR_SLUG,
        },
    )
    t35.write_stable(REPAIR_ROOT / "census_delta.json", census)
    t35.write_stable(REPAIR_ROOT / "topology.json", topology)
    t35.write_stable(REPAIR_ROOT / "readiness.json", readiness)
    if both_ready:
        hashes = {
            "census": t35.sha256_file(REPAIR_ROOT / "census_delta.json"),
            "topology": t35.sha256_file(REPAIR_ROOT / "topology.json"),
            "readiness": t35.sha256_file(REPAIR_ROOT / "readiness.json"),
            "receipt": None,
            "gametest_java": None,
            "gametest_log": None,
            "generated_recipes": None,
            "locked_support": None,
            "publication_group_manifest": None,
            "shard_manifest": None,
            "runtime_dependency_manifest": None,
            "production_lock": None,
        }
        seal = {
            "schema_version": 2,
            "status": "SEALED",
            "card_id": REPAIR_SLUG,
            "sealed_at_wave": REPAIR_SLUG,
            "source_revision": t35.SOURCE_REVISION,
            "generated_by": "python tools/build_ordinary_closeout_integrity.py --write",
            "complete_family_count": 0,
            "relation_count": 0,
            "reclassification_delta": 0,
            "remaining_recipe_gap": int(mixer["remaining_recipe_gap"]),
            "production_lock_sha256": None,
            "gametest_status": "NONE",
            "receipt_sha256": None,
            "composed_identity_ledger_v2_sha256": t35.sha256_file(
                closeout_seal.IDENTITY_LEDGER_V2
            ),
            "composed_runtime_manifest_v2_sha256": t35.sha256_file(
                closeout_seal.RUNTIME_MANIFEST_V2
            ),
            "hashes": hashes,
            "note": "Zero-family closeout-integrity repair. Does not rewrite frozen v2.",
            "repair_wave": REPAIR_SLUG,
            "supersedes_sha256": "ordinary-wave-closeout-integrity-opening",
        }
        t35.write_stable(REPAIR_ROOT / "closeout_seal.json", seal)
    return {
        "load_pending_axes": pending,
        "mixer_ready": mixer_ready.get("ready"),
        "smelter_ready": smelter_ready.get("ready"),
        "status": readiness["status"],
        "summaries": summaries,
        "wave_slug": REPAIR_SLUG,
    }


def prove_unchanged_locks() -> list[str]:
    errors: list[str] = []
    snapshot = t35.load_json(REPAIR_ROOT / "pre_repair_hashes.json")
    locks = snapshot.get("production_lock_sha256") or {}
    trees = snapshot.get("generated_tree_sha256") or {}
    for slug in AFFECTED:
        live_lock = t35.sha256_file(wave.wave_dir(slug) / "production_lock.json")
        expected = locks.get(slug)
        if expected and live_lock != expected:
            errors.append(f"{slug} production_lock hash drifted during repair")
        generated = (
            t35.ROOT
            / "src/recipe_generated/resources/data/cruciblecraft/recipe"
            / slug.split("/")[0]
            / "ordinary_closure"
        )
        live_tree = _tree_hash(generated)
        if trees.get(slug) and live_tree != trees.get(slug):
            errors.append(f"{slug} generated tree hash drifted during repair")
        census = t35.load_json(wave.wave_dir(slug) / "census_delta.json")
        if slug == "smelter/ordinary-closure":
            if int(census["complete_family_count"]) != 338:
                errors.append("smelter completion drifted")
            if int(census["reclassification_delta"]) != 14:
                errors.append("smelter reclassification drifted")
        if slug == "mixer/ordinary-closure":
            if int(census["complete_family_count"]) != 648:
                errors.append("mixer completion drifted")
            if int(census["reclassification_delta"]) != 15:
                errors.append("mixer reclassification drifted")
            if int(census["remaining_recipe_gap"]) != 334:
                errors.append("opening gap drifted from 334")
    return errors


def rewrite_content_seals() -> dict[str, str]:
    written: dict[str, str] = {}
    snapshot = t35.load_json(REPAIR_ROOT / "pre_repair_hashes.json")
    seals = snapshot.get("seals") or {}
    for slug in AFFECTED:
        pre_sha = str((seals.get(slug) or {}).get("sha256") or "")
        closeout_seal.write_wave_seal(
            slug,
            supersedes_sha256=pre_sha,
            repair_wave=REPAIR_SLUG,
        )
        written[slug] = t35.relative(wave_seal_path(slug))
    return written


def check() -> list[str]:
    errors: list[str] = []
    if not (REPAIR_ROOT / "pre_repair_hashes.json").is_file():
        return ["missing pre_repair_hashes.json"]
    errors.extend(prove_unchanged_locks())
    for slug in AFFECTED:
        errors.extend(check_wave(slug))
        errors.extend(closeout_seal.check_wave_seal(slug))
    mixer = t35.load_json(wave.wave_dir("mixer/ordinary-closure") / "census_delta.json")
    if int(mixer.get("remaining_recipe_gap") or 0) != 334:
        errors.append("mixer remaining_recipe_gap must stay 334 during repair")
    readiness = t35.load_json(REPAIR_ROOT / "readiness.json") if (
        REPAIR_ROOT / "readiness.json"
    ).is_file() else {}
    smelter_ready = wave.evaluate_live_wave_ready("smelter/ordinary-closure")
    mixer_ready = wave.evaluate_live_wave_ready("mixer/ordinary-closure")
    if smelter_ready.get("ready") and mixer_ready.get("ready"):
        if readiness.get("status") != "WAVE_READY":
            errors.append("repair readiness must be WAVE_READY after both content waves")
        if not (REPAIR_ROOT / "closeout_seal.json").is_file():
            errors.append("missing repair closeout_seal.json")
    return errors


def write() -> dict[str, Any]:
    snapshot_pre_repair()
    apply_integrated_measurements()
    summaries = rebuild_affected()
    payload = write_repair_artifacts(summaries)
    errors = prove_unchanged_locks()
    if errors:
        raise ValueError("; ".join(errors))
    smelter_ready = wave.evaluate_live_wave_ready("smelter/ordinary-closure")
    mixer_ready = wave.evaluate_live_wave_ready("mixer/ordinary-closure")
    if smelter_ready.get("ready") and mixer_ready.get("ready"):
        payload["rewritten_seals"] = rewrite_content_seals()
    return payload


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--snapshot", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check and not args.snapshot:
        parser.error("choose --write, --check, or --snapshot")
    try:
        if args.snapshot:
            snapshot_pre_repair()
            print(f"Wrote {t35.relative(REPAIR_ROOT / 'pre_repair_hashes.json')}")
            return 0
        if args.write:
            payload = write()
            print(json.dumps(payload, sort_keys=True))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("ordinary-wave closeout integrity is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"closeout integrity repair failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
