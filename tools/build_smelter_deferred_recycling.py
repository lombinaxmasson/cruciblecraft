#!/usr/bin/env python3
"""Build smelter/deferred-recycling: 1817 compact exact MTE recovery families."""
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
from tools import census_common as census
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import deferred_recycling
from tools.recipe_bulk import identity_v3
from tools.recipe_bulk import ordinary_wave as wave
from tools.recipe_bulk import runtime_v3
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import RECIPE_GENERATED_ROOT, recipe_wave
from tools.wave_closeout import known_slugs
from tools.wave_closeout import seal_path
from tools.wave_closeout import spec_for

SLUG = deferred_recycling.SLUG
NEXT_CHILD = "smelter/deferred-recycling-edge"
GENERATED_BY = "python tools/build_smelter_deferred_recycling.py"
OPENING_DEFERRED_RECYCLING = 1843
OPENING_DEFERRED_TOTAL = 1845
PROVEN_COUNT = deferred_recycling.PROVEN_COUNT
R0_DIR = census.TOOLS / "waves" / "recycling" / "deferred-ordinary-ledger-r0"


def _write_publication_policies(policies: dict[str, dict[str, Any]]) -> None:
    root = RECIPE_GENERATED_ROOT / "publication_policy"
    root.mkdir(parents=True, exist_ok=True)
    for group_id, document in policies.items():
        name = group_id.split(":", 1)[-1].replace("/", "_") + ".json"
        census.write_stable(root / name, document)


def _write_support_supersede_dedup() -> None:
    document = wave.build_support_supersede_dedup(SLUG)
    root = RECIPE_GENERATED_ROOT / "dedup_rule"
    root.mkdir(parents=True, exist_ok=True)
    name = str(document["rule_id"]).split(":", 1)[1] + ".json"
    census.write_stable(root / name, document)


def _runtime_manifest(lock: dict[str, Any], publication: dict[str, Any]) -> dict[str, Any]:
    return {
        "execution_envelopes": {
            group: "chemical_bronze" for group in publication["group_winners"]
        },
        "generated_by": GENERATED_BY,
        "group_winners": publication["group_winners"],
        "relation_count": lock["production"]["relation_count"],
        "schema_version": 1,
        "status": "RUNTIME_DEPENDENCY_MANIFEST",
        "wave_slug": SLUG,
    }


def build_census(
    lock: dict[str, Any],
    publication: dict[str, Any],
    load: dict[str, Any],
) -> dict[str, Any]:
    complete = int(lock["production"]["family_count"])
    if complete != PROVEN_COUNT:
        raise ValueError(f"{SLUG} lock family count {complete} != {PROVEN_COUNT}")
    base = wave.build_census(
        SLUG,
        lock,
        publication,
        {"host": "cruciblecraft:smelter", "reclassified": []},
        load,
    )
    deferred_recycling_count = OPENING_DEFERRED_RECYCLING - complete
    deferred_total = OPENING_DEFERRED_TOTAL - complete
    remaining = dict(base.get("remaining_ordinary") or {})
    remaining.update(
        {
            "complete_family_count": complete,
            "completion_delta": complete,
            "deferred_recycling_count": deferred_recycling_count,
            "deferred_total": deferred_total,
            "enumerated": True,
            "opening_execution_gap": 0,
            "partial_family_count": 0,
            "reclassification_delta": 0,
            "remaining_ordinary_families": 0,
        }
    )
    base.update(
        {
            "completion_delta": complete,
            "generated_by": GENERATED_BY,
            "opening_execution_gap": 0,
            "reclassification_delta": 0,
            "remaining_ordinary": remaining,
            "remaining_recipe_gap": 0,
        }
    )
    return base


def emit_wave() -> dict[str, Any]:
    spec = recipe_wave(SLUG)
    root = wave.wave_dir(SLUG)
    root.mkdir(parents=True, exist_ok=True)
    replayed = deferred_recycling.replay()
    if replayed.get("unmapped_fluids") or replayed.get("unmapped_items"):
        raise ValueError(f"{SLUG} still has unmapped operands")
    if replayed.get("reclassified"):
        raise ValueError(f"{SLUG} must not reclassify proven recovery families")
    source = wave.build_source(SLUG, replayed)
    census.write_stable(spec.source_path, source)
    candidate = wave.build_candidate(SLUG, replayed)
    census.write_stable(root / "candidate_selection.json", candidate)
    lock = wave.build_lock(SLUG, replayed, candidate)
    if int(lock["production"]["family_count"]) != PROVEN_COUNT:
        raise ValueError("production lock drifted from 1817")
    if int(lock["production"]["exact_multi_families"] or 0) != 0:
        raise ValueError("deferred recycling must be compact exact singletons")
    census.write_stable(spec.lock_path, lock)
    operand_map = wave.build_operand_map(SLUG, replayed["production_relations"])
    census.write_stable(spec.operand_map_path, operand_map)
    identity_delta = wave.build_identity_delta(SLUG, operand_map)
    census.write_stable(root / "identity_ledger_delta.json", identity_delta)
    compiled = compile_mod.compile_wave(SLUG)
    if any(
        relation.get("parameterized")
        for _path, document in compiled["planned"]
        for relation in document.get("relations") or []
    ):
        raise ValueError("parameterized compact family leaked into deferred recycling")
    compile_mod.write_tree(
        compiled["planned"],
        spec.generated_root,
        spec.generated_root,
        path_prefix=spec.path_prefix,
    )
    census.write_stable(root / "compile_report.json", compiled["report"])
    publication, policies = wave.build_publication(SLUG, lock, spec.target_map)
    census.write_stable(root / "publication_group_manifest.json", publication)
    _write_publication_policies(policies)
    _write_support_supersede_dedup()
    runtime_delta = wave.build_runtime_delta(SLUG, publication, policies)
    census.write_stable(root / "runtime_manifest_delta.json", runtime_delta)
    census.write_stable(identity_v3.OUTPUT, identity_v3.build())
    census.write_stable(runtime_v3.OUTPUT, runtime_v3.build())
    shards = wave.build_shards(SLUG, lock, spec.target_map)
    census.write_stable(root / "shard_manifest.json", shards)
    equivalence = wave.build_equivalence(SLUG, compiled["planned"], source)
    census.write_stable(spec.equivalence_path, equivalence)
    player_path = wave.build_player_path(SLUG, compiled["planned"])
    census.write_stable(root / "player_path.json", player_path)
    measurements_path = root / "measurements.json"
    measurements = census.load_json(measurements_path) if measurements_path.is_file() else {}
    load = wave.build_load(SLUG, publication, lock, measurements)
    census.write_stable(root / "load_projection.json", load)
    runtime = _runtime_manifest(lock, publication)
    census.write_stable(root / "runtime_dependency_manifest.json", runtime)
    census = build_census(lock, publication, load)
    census.write_stable(root / "census_delta.json", census)
    census.write_stable(
        root / "wave.json",
        {
            "cohort": "deferred_recycling",
            "depends_on": [deferred_recycling.IDENTITY_SLUG],
            "generated_by": GENERATED_BY,
            "owns_families": PROVEN_COUNT,
            "schema_version": 1,
            "wave_slug": SLUG,
        },
    )
    closeout = spec_for(SLUG)
    receipt_path = root / "gametest_receipt.json"
    receipt = census.load_json(receipt_path) if receipt_path.is_file() else None
    passed = bool(receipt and receipt.get("status") == "PASS")
    unique_active = closeout.unique_active_wave if passed else SLUG
    next_unassigned = closeout.next_unassigned if passed else False
    verdict = wave.evaluate_wave_ready(
        SLUG,
        source=source,
        lock=lock,
        equivalence=equivalence,
        player_path=player_path,
        receipt=receipt,
        load=load,
        census=census,
        unique_active=unique_active,
    )
    topology = wave.build_topology(
        SLUG,
        census,
        lock,
        unique_active,
        next_unassigned,
        verdict=verdict,
    )
    census.write_stable(root / "topology.json", topology)
    readiness = wave.build_readiness(
        SLUG,
        census=census,
        lock=lock,
        publication=publication,
        receipt=receipt,
        unique_active=unique_active,
        next_unassigned=next_unassigned,
        verdict=verdict,
        load=load,
    )
    census.write_stable(root / "readiness.json", readiness)
    generated = RECIPE_GENERATED_ROOT / "smelter" / "deferred_recycling"
    recipe_files = list(generated.rglob("gt_recipe_*.json")) if generated.is_dir() else []
    return {
        "blocked": 0,
        "complete": PROVEN_COUNT,
        "recipe_files": len(recipe_files),
        "reclassified": 0,
        "relations": lock["production"]["relation_count"],
        "status": readiness["status"],
        "wave_slug": SLUG,
    }


def rebuild_closeout() -> dict[str, Any]:
    spec = recipe_wave(SLUG)
    root = wave.wave_dir(SLUG)
    lock = census.load_json(spec.lock_path)
    publication = census.load_json(root / "publication_group_manifest.json")
    source = census.load_json(spec.source_path)
    measurements = (
        census.load_json(root / "measurements.json")
        if (root / "measurements.json").is_file()
        else {}
    )
    load = wave.build_load(SLUG, publication, lock, measurements)
    census.write_stable(root / "load_projection.json", load)
    census = build_census(lock, publication, load)
    census.write_stable(root / "census_delta.json", census)
    closeout = spec_for(SLUG)
    receipt = (
        census.load_json(root / "gametest_receipt.json")
        if (root / "gametest_receipt.json").is_file()
        else None
    )
    passed = bool(receipt and receipt.get("status") == "PASS")
    unique_active = closeout.unique_active_wave if passed else SLUG
    next_unassigned = closeout.next_unassigned if passed else False
    equivalence = census.load_json(spec.equivalence_path) if spec.equivalence_path.is_file() else None
    player_path = (
        census.load_json(root / "player_path.json")
        if (root / "player_path.json").is_file()
        else None
    )
    verdict = wave.evaluate_wave_ready(
        SLUG,
        source=source,
        lock=lock,
        equivalence=equivalence,
        player_path=player_path,
        receipt=receipt,
        load=load,
        census=census,
        unique_active=unique_active,
    )
    topology = wave.build_topology(
        SLUG,
        census,
        lock,
        unique_active,
        next_unassigned,
        verdict=verdict,
    )
    census.write_stable(root / "topology.json", topology)
    readiness = wave.build_readiness(
        SLUG,
        census=census,
        lock=lock,
        publication=publication,
        receipt=receipt,
        unique_active=unique_active,
        next_unassigned=next_unassigned,
        verdict=verdict,
        load=load,
    )
    census.write_stable(root / "readiness.json", readiness)
    return {
        "blockers": verdict["blockers"],
        "status": readiness["status"],
        "wave_complete": verdict["ready"],
        "wave_slug": SLUG,
    }


def check_wave() -> list[str]:
    errors: list[str] = []
    if SLUG not in KNOWN_SEMANTIC_SLUGS or SLUG not in known_slugs():
        errors.append(f"{SLUG} is not registered")
        return errors
    spec = spec_for(SLUG)
    if spec.unique_active_wave != NEXT_CHILD:
        errors.append(f"unique_active_wave drifted: {spec.unique_active_wave}")
    if spec.next_unassigned:
        errors.append("next_unassigned must stay false until program closeout")
    if spec.production_lock is None:
        errors.append("deferred recycling requires a production lock")
    inputs = wave.load_closeout_inputs(SLUG)
    source = inputs["source"] or {}
    lock = inputs["lock"] or {}
    census = inputs["census"] or {}
    if str(source.get("status") or "") == "SOURCE_READY":
        if int(source.get("family_count") or 0) != PROVEN_COUNT:
            errors.append("source family_count drifted from 1817")
        if int(source.get("relation_count") or 0) != PROVEN_COUNT:
            errors.append("source relation_count drifted from 1817")
    production = lock.get("production") or {}
    if production:
        if int(production.get("family_count") or 0) != PROVEN_COUNT:
            errors.append("lock family_count drifted from 1817")
        if int(production.get("exact_families") or 0) != PROVEN_COUNT:
            errors.append("lock exact_families drifted from 1817")
        families = list(production.get("families") or [])
        for row in families:
            template = str(row.get("template_key") or "")
            if template in deferred_recycling.EDGE_KEYS:
                errors.append(f"edge family leaked into lock: {template}")
            if str(row.get("representation") or "") != "exact":
                errors.append(f"{template} is not exact singleton")
            group = str(row.get("publication_group") or "")
            if "/ordinary_closure/" in group or "autoclave" in group:
                errors.append(f"{template} used a forbidden publication group")
    generated = RECIPE_GENERATED_ROOT / "smelter" / "deferred_recycling"
    recipe_files = list(generated.rglob("gt_recipe_*.json")) if generated.is_dir() else []
    if production and len(recipe_files) != PROVEN_COUNT:
        errors.append(f"generated recipe files {len(recipe_files)} != 1817")
    remaining = census.get("remaining_ordinary") or {}
    if census:
        if int(census.get("completion_delta") or 0) != PROVEN_COUNT:
            errors.append("census completion_delta drifted from 1817")
        if int(census.get("remaining_recipe_gap", 1)) != 0:
            errors.append("execution gap must stay 0")
        if int(remaining.get("deferred_recycling_count") or 0) != 26:
            errors.append("deferred recycling must drop by exactly 1817 to 26")
        if int(census.get("partial_family_count") or 0) != 0:
            errors.append("partial_family_count must be 0")
    r0_census = census.load_json(R0_DIR / "census_delta.json")
    if int(r0_census.get("remaining_ordinary", {}).get("deferred_recycling_count") or 0) != 1843:
        errors.append("must not rewrite R0 deferred recycling opening 1843")
    receipt = inputs["receipt"]
    passed = bool(receipt and receipt.get("status") == "PASS")
    unique_active = spec.unique_active_wave if passed else SLUG
    verdict = wave.evaluate_wave_ready(
        SLUG,
        source=inputs["source"],
        lock=inputs["lock"],
        equivalence=inputs["equivalence"],
        player_path=inputs["player_path"],
        receipt=receipt,
        load=inputs["load"],
        census=inputs["census"],
        unique_active=unique_active,
    )
    readiness = census.load_json(wave.wave_dir(SLUG) / "readiness.json") if (
        wave.wave_dir(SLUG) / "readiness.json"
    ).is_file() else {}
    if readiness.get("status") == "WAVE_READY" and not verdict.get("ready"):
        errors.append(
            "readiness claims WAVE_READY while derivation blockers="
            + ",".join(verdict.get("blockers") or [])
        )
    if verdict.get("ready") and str((inputs["load"] or {}).get("status") or "") != "LOAD_READY":
        errors.append("WAVE_READY requires load.status LOAD_READY")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--from-log", type=Path)
    parser.add_argument("--from-integrated", type=Path)
    parser.add_argument("--seal", action="store_true")
    parser.add_argument("--rebuild-closeout", action="store_true")
    args = parser.parse_args(argv)
    chosen = [
        args.write,
        args.check,
        args.from_log is not None,
        args.from_integrated is not None,
        args.seal,
        args.rebuild_closeout,
    ]
    if sum(1 for value in chosen if value) != 1:
        parser.error(
            "choose --write, --check, --from-log, --from-integrated, --seal, or --rebuild-closeout"
        )
    try:
        if args.from_integrated is not None:
            document = census.load_json(args.from_integrated)
            flat = wave.flatten_integrated(document)
            census.write_stable(wave.wave_dir(SLUG) / "measurements.json", flat)
            summary = rebuild_closeout()
            print(json.dumps(summary, sort_keys=True))
            return 0
        if args.from_log is not None:
            from tools.build_ordinary_wave import write_receipt

            write_receipt(SLUG, args.from_log)
            emit_wave()
            print(f"Wrote GameTest receipt for {SLUG}")
            return 0
        if args.rebuild_closeout:
            print(json.dumps(rebuild_closeout(), sort_keys=True))
            return 0
        if args.seal:
            closeout_seal.write_wave_seal(SLUG)
            print(f"Wrote {census.relative(seal_path(SLUG))}")
            return 0
        if args.write:
            summary = emit_wave()
            print(json.dumps(summary, sort_keys=True))
            return 0
        errors = check_wave()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{SLUG} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"smelter deferred recycling failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
