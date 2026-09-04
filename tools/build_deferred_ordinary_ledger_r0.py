#!/usr/bin/env python3
"""Build and check recycling/deferred-ordinary-ledger-r0 artifacts."""
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
from tools import recycling_deferred_r0 as r0
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for

SLUG = r0.SLUG
ROOT_DIR = census.TOOLS / "waves" / "recycling" / "deferred-ordinary-ledger-r0"


def _require_remainder_sealed() -> list[str]:
    errors: list[str] = []
    slug = "recipe-portfolio/ordinary-remainder-closure"
    root = census.TOOLS / "waves" / "recipe-portfolio" / "ordinary-remainder-closure"
    readiness = census.load_json(root / "readiness.json")
    if readiness.get("status") != "WAVE_READY":
        errors.append(f"{slug} readiness is {readiness.get('status')}")
    errors.extend(closeout_seal.check_wave_seal(slug))
    return errors


def build_documents() -> dict[str, Any]:
    errors = _require_remainder_sealed()
    enumerated = r0.enumerate_universe()
    errors.extend(enumerated["errors"])
    if errors:
        raise ValueError("; ".join(errors))
    universe = r0.universe_document(enumerated)
    partition = r0.partition_document(enumerated)
    candidate = enumerated["candidate"]
    n300 = {
        "exceptions": list(r0.N300_EXCEPTIONS),
        "generated_by": r0.GENERATED_BY,
        "padding_forbidden": True,
        "schema_version": 1,
        "source_revision": census.SOURCE_REVISION,
        "status": "N300_EXCEPTIONS",
        "wave_slug": SLUG,
    }
    wave = {
        "cohort": "deferred-ordinary-ledger-r0",
        "depends_on": ["recipe-portfolio/ordinary-remainder-closure"],
        "owns_families": 0,
        "program": r0.PROGRAM,
        "schema_version": 1,
        "unique_active_wave": True,
        "wave_slug": SLUG,
    }
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "enumerated_recycling": enumerated["enumerated_recycling"],
        "generated_by": r0.GENERATED_BY,
        "ledger_total": r0.LEDGER_TOTAL,
        "partial_family_count": 0,
        "reclassification_delta": enumerated["reclassification_delta"],
        "remaining_ordinary": {
            "complete_family_count": 0,
            "completion_delta": 0,
            "deferred_recycling_count": enumerated["enumerated_recycling"],
            "deferred_total": r0.LEDGER_TOTAL,
            "enumerated": True,
            "partial_family_count": 0,
            "reclassification_delta": enumerated["reclassification_delta"],
            "remaining_ordinary_families": 0,
        },
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": census.SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": SLUG,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": r0.GENERATED_BY,
        "next_unassigned": False,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": census.SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": r0.NEXT_CHILD,
        "wave_slug": SLUG,
    }
    readiness = {
        "evidence": {
            "autoclave_tagged_recycling": enumerated["cohort_counts"][
                "autoclave_tagged_recycling"
            ],
            "centrifuge_cross_mod": enumerated["cohort_counts"]["centrifuge_cross_mod"],
            "centrifuge_execution_envelope": enumerated["cohort_counts"][
                "centrifuge_execution_envelope"
            ],
            "complete_family_count": 0,
            "completion_delta": 0,
            "enumerated": True,
            "enumerated_recycling": enumerated["enumerated_recycling"],
            "identity_unique_meta_count": candidate["unique_meta_count"],
            "inherited_recycling": r0.INHERITED_RECYCLING,
            "ledger_total": r0.LEDGER_TOTAL,
            "mislabeled_needs_reclass": enumerated["cohort_counts"][
                "mislabeled_needs_reclass"
            ],
            "n300_exceptions": list(r0.N300_EXCEPTIONS),
            "one_x_joint_exit": False,
            "partial_family_count": 0,
            "program_status": "RECYCLING_DEFERRED_LEDGER_R0_READY",
            "recipe_files_generated": False,
            "reclassification_delta": enumerated["reclassification_delta"],
            "remaining_recipe_gap": 0,
            "smelter_proven_mte_recovery": enumerated["cohort_counts"][
                "smelter_proven_mte_recovery"
            ],
            "smelter_recovery_edge": enumerated["cohort_counts"][
                "smelter_recovery_edge"
            ],
            "unique_meta_equals_proven_family_count": candidate[
                "unique_meta_equals_proven_family_count"
            ],
        },
        "generated_by": r0.GENERATED_BY,
        "next_unassigned": False,
        "schema_version": 1,
        "source_revision": census.SOURCE_REVISION,
        "status": "RECYCLING_DEFERRED_LEDGER_R0_READY",
        "unique_active_wave": r0.NEXT_CHILD,
        "wave_complete": True,
        "wave_slug": SLUG,
    }
    return {
        "candidate": candidate,
        "census": census,
        "n300": n300,
        "partition": partition,
        "readiness": readiness,
        "reclassification_delta": enumerated["reclassification_delta"],
        "topology": topology,
        "universe": universe,
        "wave": wave,
    }


def write_artifacts() -> dict[str, Any]:
    documents = build_documents()
    ROOT_DIR.mkdir(parents=True, exist_ok=True)
    census.write_stable(ROOT_DIR / "wave.json", documents["wave"])
    census.write_stable(ROOT_DIR / "deferred_universe.json", documents["universe"])
    census.write_stable(ROOT_DIR / "cohort_partition.json", documents["partition"])
    census.write_stable(ROOT_DIR / "identity_candidate.json", documents["candidate"])
    census.write_stable(ROOT_DIR / "n300_exceptions.json", documents["n300"])
    census.write_stable(ROOT_DIR / "census_delta.json", documents["census"])
    census.write_stable(ROOT_DIR / "topology.json", documents["topology"])
    census.write_stable(ROOT_DIR / "readiness.json", documents["readiness"])
    hashes = {
        "census": census.sha256_file(ROOT_DIR / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": census.sha256_file(ROOT_DIR / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": census.sha256_file(ROOT_DIR / "topology.json"),
    }
    seal = {
        "card_id": SLUG,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": census.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": census.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": "python tools/build_deferred_ordinary_ledger_r0.py --write",
        "hashes": hashes,
        "note": (
            "Zero-family ledger R0. Enumerated deferred ordinary ledger 1845 "
            "exactly once. inherited_recycling.enumerated is true. No recipe "
            "files or family completion. unique_active_wave hands off to "
            "recycling/smelter-mte-identity."
        ),
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": documents["reclassification_delta"],
        "relation_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": SLUG,
        "source_revision": census.SOURCE_REVISION,
        "status": "SEALED",
    }
    census.write_stable(ROOT_DIR / "closeout_seal.json", seal)
    return {
        "ledger_total": r0.LEDGER_TOTAL,
        "reclassification_delta": documents["reclassification_delta"],
        "status": "RECYCLING_DEFERRED_LEDGER_R0_READY",
        "unique_active_wave": r0.NEXT_CHILD,
        "wave_slug": SLUG,
    }


def check() -> list[str]:
    errors: list[str] = []
    try:
        enumerated = r0.enumerate_universe()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        return [str(error)]
    errors.extend(enumerated["errors"])
    if SLUG not in KNOWN_SEMANTIC_SLUGS:
        errors.append("R0 slug missing from KNOWN_SEMANTIC_SLUGS")
    if SLUG not in known_slugs():
        errors.append("R0 slug missing from wave_closeout")
    spec = spec_for(SLUG)
    if spec.unique_active_wave != r0.NEXT_CHILD:
        errors.append("R0 closeout spec unique_active_wave drifted")
    if spec.next_unassigned is not False:
        errors.append("R0 closeout spec must keep next_unassigned false")
    if spec.owns_families != 0 or spec.production_lock is not None:
        errors.append("R0 closeout spec must be zero-family infrastructure")
    readiness_path = ROOT_DIR / "readiness.json"
    if not readiness_path.is_file():
        return errors + ["missing R0 readiness.json"]
    readiness = census.load_json(readiness_path)
    if readiness.get("status") != "RECYCLING_DEFERRED_LEDGER_R0_READY":
        errors.append("R0 readiness is not RECYCLING_DEFERRED_LEDGER_R0_READY")
    if readiness.get("unique_active_wave") != r0.NEXT_CHILD:
        errors.append("R0 unique_active_wave must hand off to smelter-mte-identity")
    if readiness.get("next_unassigned") is not False:
        errors.append("R0 next_unassigned must be false")
    evidence = readiness.get("evidence") or {}
    if evidence.get("recipe_files_generated") is not False:
        errors.append("R0 must not generate recipe files")
    if int(evidence.get("completion_delta", -1)) != 0:
        errors.append("R0 must not claim family completion")
    census = census.load_json(ROOT_DIR / "census_delta.json")
    if int(census.get("complete_family_count", -1)) != 0:
        errors.append("R0 census claimed family completion")
    universe = census.load_json(ROOT_DIR / "deferred_universe.json")
    if universe.get("enumerated") is not True:
        errors.append("deferred_universe.enumerated must be true")
    if int(universe.get("family_count") or 0) != r0.LEDGER_TOTAL:
        errors.append("deferred_universe family_count drifted from 1845")
    if universe.get("inherited_recycling", {}).get("enumerated") is not True:
        errors.append("inherited_recycling.enumerated must be true")
    if universe.get("inherited_recycling", {}).get("silently_discarded") is not False:
        errors.append("inherited recycling was silently discarded")
    generated = (
        census.ROOT
        / "src"
        / "recipe_generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "recipe"
        / "recycling"
    )
    if generated.is_dir() and any(generated.rglob("*.json")):
        errors.append("R0 generated recycling recipe files")
    errors.extend(closeout_seal.check_wave_seal(SLUG))
    live = r0.universe_document(enumerated)
    drift = census.first_json_diff(live, universe)
    if drift:
        errors.append(f"deferred_universe drifted from live enumeration: {drift}")
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
        print("recycling deferred ordinary ledger R0 is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"recycling deferred ordinary ledger R0 failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
