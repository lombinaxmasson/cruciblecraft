#!/usr/bin/env python3
"""Program closeout for recipe-portfolio/ordinary-remainder-closure."""
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
from tools import t35_common as t35
from tools.recipe_bulk import ordinary_r0 as r0
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for

SLUG = "recipe-portfolio/ordinary-remainder-closure"
ROOT_DIR = t35.TOOLS / "waves" / "recipe-portfolio" / "ordinary-remainder-closure"
OPENING_FAMILIES = 334
OPENING_RELATIONS = 2047
OPENING_DEFERRED_RECYCLING = 1819
NEXT_MAJOR = "recycling/deferred-ordinary-runtime"
HOST_CHILDREN = (
    "drying/ordinary-closure",
    "electrolyzer/ordinary-closure",
    "centrifuge/ordinary-closure",
    "autoclave/ordinary-closure",
    "compressor/ordinary-closure",
)
PREREQ_SLUGS = (
    "ordinary-wave/closeout-integrity-repair",
    "ordinary-remainder/operand-foundation",
    *HOST_CHILDREN,
)


def _wave_root(slug: str) -> Path:
    return t35.TOOLS / "waves" / Path(*slug.split("/"))


def _require_sealed_ready(slug: str) -> list[str]:
    errors: list[str] = []
    root = _wave_root(slug)
    readiness = root / "readiness.json"
    seal = root / "closeout_seal.json"
    if not readiness.is_file():
        return [f"{slug} missing readiness.json"]
    if not seal.is_file():
        return [f"{slug} missing closeout_seal.json"]
    ready = t35.load_json(readiness)
    sealed = t35.load_json(seal)
    if ready.get("status") != "WAVE_READY":
        errors.append(f"{slug} readiness is {ready.get('status')}")
    if ready.get("unique_active_wave") is not None:
        errors.append(f"{slug} unique_active_wave is not null")
    if sealed.get("status") != "SEALED":
        errors.append(f"{slug} seal is {sealed.get('status')}")
    errors.extend(closeout_seal.check_wave_seal(slug))
    return errors


def _child_account(slug: str) -> dict[str, Any]:
    census = t35.load_json(_wave_root(slug) / "census_delta.json")
    lock = t35.load_json(_wave_root(slug) / "production_lock.json")
    remaining = census.get("remaining_ordinary") or {}
    reclassified: list[dict[str, Any]] = []
    for row in lock.get("reclassified") or []:
        future = str(row.get("future_owner") or "")
        recheck = str(row.get("recheck_condition") or "")
        if not future or not recheck:
            raise ValueError(f"{slug} reclass missing future_owner/recheck")
        reclassified.append(
            {
                "expanded_count": int(row.get("expanded_count") or 0),
                "family_id": str(row["family_id"]),
                "future_owner": future,
                "owner": str(row.get("owner") or ""),
                "reason": str(row.get("reason") or ""),
                "recheck_condition": recheck,
                "source_wave": slug,
                "template_key": str(row.get("template_key") or ""),
            }
        )
    return {
        "complete_family_count": int(census.get("complete_family_count") or 0),
        "completion_delta": int(census.get("completion_delta") or 0),
        "deferred_recycling_count": int(
            remaining.get("deferred_recycling_count") or 0
        ),
        "partial_family_count": int(census.get("partial_family_count") or 0),
        "reclassification_delta": int(census.get("reclassification_delta") or 0),
        "reclassified": reclassified,
        "remaining_recipe_gap": int(census.get("remaining_recipe_gap") or 0),
        "wave_slug": slug,
    }


def program_proof() -> dict[str, Any]:
    errors: list[str] = []
    opening = r0.global_remainder_r0()
    errors.extend(opening.get("errors") or [])
    if int(opening.get("family_total") or 0) != OPENING_FAMILIES:
        errors.append("opening family_total drifted from 334")
    if int(opening.get("relation_total") or 0) != OPENING_RELATIONS:
        errors.append("opening relation_total drifted from 2047")
    live = r0.live_remainder_replay()
    errors.extend(live.get("errors") or [])
    children: list[dict[str, Any]] = []
    proven_new: list[dict[str, Any]] = []
    completion = 0
    reclass = 0
    partial = 0
    for slug in HOST_CHILDREN:
        errors.extend(_require_sealed_ready(slug))
        child = _child_account(slug)
        children.append({key: child[key] for key in child if key != "reclassified"})
        completion += int(child["completion_delta"])
        reclass += int(child["reclassification_delta"])
        partial += int(child["partial_family_count"])
        proven_new.extend(child["reclassified"])
        if int(child["reclassification_delta"]) != len(child["reclassified"]):
            errors.append(
                f"{slug} reclass count {child['reclassification_delta']} "
                f"!= lock rows {len(child['reclassified'])}"
            )
    for slug in PREREQ_SLUGS:
        if slug in HOST_CHILDREN:
            continue
        errors.extend(_require_sealed_ready(slug))
    accounted = completion + reclass
    if accounted != OPENING_FAMILIES:
        errors.append(f"completion+reclass={accounted} expected 334")
    if partial:
        errors.append(f"partial_family_count={partial}")
    last = children[-1] if children else {}
    closing_recycling = int(last.get("deferred_recycling_count") or 0)
    recycling_new = [
        row for row in proven_new if row["future_owner"] == "later:recycling"
    ]
    other_new = [
        row for row in proven_new if row["future_owner"] != "later:recycling"
    ]
    expected_recycling = OPENING_DEFERRED_RECYCLING + len(recycling_new)
    if closing_recycling != expected_recycling:
        errors.append(
            f"deferred_recycling={closing_recycling} expected {expected_recycling}"
        )
    if last.get("remaining_recipe_gap") != 0:
        errors.append("compressor remaining_recipe_gap is not 0")
    if live.get("family_total") != 0:
        errors.append("live remainder replay is not 0")
    buckets: dict[str, int] = {
        "later:recycling": expected_recycling,
    }
    for row in other_new:
        buckets[row["future_owner"]] = buckets.get(row["future_owner"], 0) + 1
    return {
        "accounted_families": accounted,
        "children": children,
        "closing_deferred_recycling": closing_recycling,
        "closing_deferred_total": expected_recycling + len(other_new),
        "completion_delta": completion,
        "deferred_buckets": buckets,
        "errors": errors,
        "live": live,
        "next_major_program": NEXT_MAJOR,
        "one_x_joint_exit": False,
        "opening": {
            "deferred_recycling": OPENING_DEFERRED_RECYCLING,
            "families": OPENING_FAMILIES,
            "relations": OPENING_RELATIONS,
        },
        "opening_r0": {
            "family_total": opening.get("family_total"),
            "relation_total": opening.get("relation_total"),
            "status": opening.get("status"),
        },
        "partial_family_count": partial,
        "proven_new_deferred": proven_new,
        "reclassification_delta": reclass,
        "source_revision": t35.SOURCE_REVISION,
        "status": (
            "ORDINARY_REMAINDER_CLOSURE_READY" if not errors else "PROGRAM_DRIFT"
        ),
    }


def write_artifacts() -> dict[str, Any]:
    proof = program_proof()
    if proof["errors"]:
        raise ValueError("; ".join(proof["errors"]))
    ROOT_DIR.mkdir(parents=True, exist_ok=True)
    generated_by = "python tools/build_ordinary_remainder_closure.py"
    wave = {
        "cohort": "ordinary-remainder-closure",
        "depends_on": list(PREREQ_SLUGS),
        "next_major_program": NEXT_MAJOR,
        "owns_families": 0,
        "schema_version": 1,
        "unique_active_wave": False,
        "wave_slug": SLUG,
    }
    gap_replay = {
        "accounted_families": proof["accounted_families"],
        "children": proof["children"],
        "completion_delta": proof["completion_delta"],
        "generated_by": generated_by,
        "live": proof["live"],
        "opening": proof["opening"],
        "opening_r0": proof["opening_r0"],
        "partial_family_count": 0,
        "reclassification_delta": proof["reclassification_delta"],
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "GAP_REPLAY_ZERO",
        "wave_slug": SLUG,
    }
    deferred = {
        "closing_deferred_recycling": proof["closing_deferred_recycling"],
        "closing_deferred_total": proof["closing_deferred_total"],
        "deferred_buckets": proof["deferred_buckets"],
        "generated_by": generated_by,
        "inherited_recycling": {
            "authority": "mixer/ordinary-closure census remaining_ordinary.deferred_recycling_count",
            "count": OPENING_DEFERRED_RECYCLING,
            "enumerated": False,
            "silently_discarded": False,
        },
        "next_owner": NEXT_MAJOR,
        "one_x_joint_exit": False,
        "proven_new_deferred": proof["proven_new_deferred"],
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "DEFERRED_LEDGER_READY",
        "wave_slug": SLUG,
    }
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": generated_by,
        "opening_execution_gap": OPENING_FAMILIES,
        "partial_family_count": 0,
        "program_accounted_families": proof["accounted_families"],
        "program_completion_delta": proof["completion_delta"],
        "program_reclassification_delta": proof["reclassification_delta"],
        "reclassification_delta": 0,
        "remaining_ordinary": {
            "complete_family_count": 0,
            "completion_delta": 0,
            "deferred_recycling_count": proof["closing_deferred_recycling"],
            "deferred_total": proof["closing_deferred_total"],
            "opening_execution_gap": OPENING_FAMILIES,
            "partial_family_count": 0,
            "program_accounted_families": proof["accounted_families"],
            "reclassification_delta": 0,
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
        "complete_family_count": 0,
        "generated_by": generated_by,
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
            "accounted_families": proof["accounted_families"],
            "complete_family_count": 0,
            "completion_delta": 0,
            "deferred_recycling_count": proof["closing_deferred_recycling"],
            "deferred_total": proof["closing_deferred_total"],
            "next_major_program": NEXT_MAJOR,
            "one_x_joint_exit": False,
            "partial_family_count": 0,
            "program_status": "ORDINARY_REMAINDER_CLOSURE_READY",
            "reclassification_delta": 0,
            "remaining_recipe_gap": 0,
        },
        "generated_by": generated_by,
        "next_unassigned": True,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": None,
        "wave_complete": True,
        "wave_slug": SLUG,
    }
    t35.write_stable(ROOT_DIR / "wave.json", wave)
    t35.write_stable(ROOT_DIR / "gap_replay.json", gap_replay)
    t35.write_stable(ROOT_DIR / "deferred_ledger.json", deferred)
    t35.write_stable(ROOT_DIR / "census_delta.json", census)
    t35.write_stable(ROOT_DIR / "topology.json", topology)
    t35.write_stable(ROOT_DIR / "readiness.json", readiness)
    hashes = {
        "census": t35.sha256_file(ROOT_DIR / "census_delta.json"),
        "topology": t35.sha256_file(ROOT_DIR / "topology.json"),
        "readiness": t35.sha256_file(ROOT_DIR / "readiness.json"),
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
        "schema_version": 1,
        "status": "SEALED",
        "card_id": SLUG,
        "sealed_at_wave": SLUG,
        "source_revision": t35.SOURCE_REVISION,
        "generated_by": "python tools/build_ordinary_remainder_closure.py --write",
        "complete_family_count": 0,
        "relation_count": 0,
        "reclassification_delta": 0,
        "remaining_recipe_gap": 0,
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
        "note": (
            "Zero-family program closeout. Execution gap replay is 0. "
            "Child completion+reclass = 334. Deferred ledger is 1819 plus "
            "proven new deferred. unique_active_wave is null. Next major "
            "program is recycling/deferred-ordinary-runtime; 1.x joint exit "
            "and nuclear census stay closed."
        ),
    }
    t35.write_stable(ROOT_DIR / "closeout_seal.json", seal)
    return {
        "accounted_families": proof["accounted_families"],
        "deferred_recycling": proof["closing_deferred_recycling"],
        "deferred_total": proof["closing_deferred_total"],
        "remaining_recipe_gap": 0,
        "status": "ORDINARY_REMAINDER_CLOSURE_READY",
        "unique_active_wave": None,
        "wave_slug": SLUG,
    }


def check() -> list[str]:
    errors: list[str] = []
    proof = program_proof()
    errors.extend(proof["errors"])
    readiness = ROOT_DIR / "readiness.json"
    if not readiness.is_file():
        return errors + ["missing program readiness.json"]
    document = t35.load_json(readiness)
    if document.get("status") != "WAVE_READY":
        errors.append("program readiness is not WAVE_READY")
    if document.get("unique_active_wave") is not None:
        errors.append("program unique_active_wave must be null")
    if document.get("next_unassigned") is not True:
        errors.append("program next_unassigned must be true")
    evidence = document.get("evidence") or {}
    if evidence.get("program_status") != "ORDINARY_REMAINDER_CLOSURE_READY":
        errors.append("missing ORDINARY_REMAINDER_CLOSURE_READY")
    if evidence.get("one_x_joint_exit") is not False:
        errors.append("1.x joint exit must stay false")
    census = t35.load_json(ROOT_DIR / "census_delta.json")
    if int(census.get("complete_family_count", -1)) != 0:
        errors.append("program must not claim family completion")
    if int(census.get("remaining_recipe_gap", -1)) != 0:
        errors.append("program remaining_recipe_gap must be 0")
    if int(census.get("program_accounted_families", -1)) != OPENING_FAMILIES:
        errors.append("program accounted families drifted from 334")
    deferred = t35.load_json(ROOT_DIR / "deferred_ledger.json")
    if deferred.get("next_owner") != NEXT_MAJOR:
        errors.append("deferred ledger next_owner drifted")
    if int(deferred.get("inherited_recycling", {}).get("count") or 0) != OPENING_DEFERRED_RECYCLING:
        errors.append("inherited recycling count drifted from 1819")
    if SLUG not in known_slugs():
        errors.append("program slug missing from wave_closeout")
    errors.extend(closeout_seal.check_wave_seal(SLUG))
    spec = spec_for(SLUG)
    if spec.unique_active_wave is not None or spec.next_unassigned is not True:
        errors.append("program closeout spec must be next_unassigned with null active wave")
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
            payload = write_artifacts()
            print(json.dumps(payload, sort_keys=True))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("ordinary remainder program closeout is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"ordinary remainder program closeout failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
