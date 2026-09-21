#!/usr/bin/env python3
"""Generic ordinary-closure wave builder: source through WAVE_READY artifacts."""
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
from tools import material_form_authority as form_authority
from tools import census_common as census
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import ordinary_source
from tools.recipe_bulk import ordinary_wave as wave
from tools.recipe_bulk.waves import recipe_wave
from tools.wave_closeout import seal_path, spec_for

SEMANTIC_PREFIXES = (
    "boule",
    "rail_gt",
)


def _ensure_authority_source(slug: str, extra_forms: list[str]) -> None:
    required_rel = f"tools/waves/{slug}/required_forms.json"
    section = slug.replace("/", "_").replace("-", "_") + "_required_forms"
    document = form_authority.load_authority()
    sources = list(document.get("sources") or [])
    payload = {
        "extra_factual_forms": sorted(set(extra_forms) | set(SEMANTIC_PREFIXES)),
        "field": "required_forms",
        "gate_section": section,
        "id": section,
        "java_runtime_visible": True,
        "owner": slug,
        "path": required_rel,
        "required_factual_prereqs": {
            "dust_div72": ["dust"],
            "small_dust": ["dust"],
        },
        "source_hash": "",
    }
    replaced = False
    for index, source in enumerate(sources):
        if source.get("id") == section:
            sources[index] = payload
            replaced = True
            break
    if not replaced:
        sources.append(payload)
    sections = list(document.get("java_overlay_sections") or [])
    if section not in sections:
        sections.append(section)
    document["sources"] = sources
    document["java_overlay_sections"] = sections
    document["note"] = (
        "Single overlay authority. Card builders write required_forms.json only; "
        "they do not write material_registration_gate.json."
    )
    census.write_stable(form_authority.AUTHORITY, form_authority.refresh_hashes(document))
    form_authority.write()


def _write_publication_policies(slug: str, policies: dict[str, dict[str, Any]]) -> None:
    spec = recipe_wave(slug)
    root = spec.generated_root / "publication_policy"
    root.mkdir(parents=True, exist_ok=True)
    for group_id, document in policies.items():
        name = group_id.split(":", 1)[-1].replace("/", "_") + ".json"
        census.write_stable(root / name, document)


def _write_support_supersede_dedup(slug: str) -> None:
    document = wave.build_support_supersede_dedup(slug)
    root = recipe_wave(slug).generated_root / "dedup_rule"
    root.mkdir(parents=True, exist_ok=True)
    name = str(document["rule_id"]).split(":", 1)[1] + ".json"
    census.write_stable(root / name, document)


def _runtime_manifest(slug: str, lock: dict[str, Any], publication: dict[str, Any]) -> dict[str, Any]:
    return {
        "execution_envelopes": {
            group: (
                "gt6_panel"
                if group.startswith(
                    ("cruciblecraft:mixer/", "cruciblecraft:centrifuge/")
                )
                else "chemical_bronze"
            )
            for group in publication["group_winners"]
        },
        "generated_by": "python tools/build_ordinary_wave.py",
        "group_winners": publication["group_winners"],
        "relation_count": lock["production"]["relation_count"],
        "schema_version": 1,
        "status": "RUNTIME_DEPENDENCY_MANIFEST",
        "wave_slug": slug,
    }


def emit_wave(slug: str, *, write_gate: bool) -> dict[str, Any]:
    spec = recipe_wave(slug)
    root = wave.wave_dir(slug)
    root.mkdir(parents=True, exist_ok=True)
    replayed = ordinary_source.replay(slug)
    if slug.startswith("mixer/"):
        replayed["membership_note"] = wave.mixer_membership_note()
    fluids = wave.build_fluid_mapping(slug, replayed)
    wrote_overlays = False
    if fluids.get("mapping"):
        census.write_stable(root / "fluid_mapping.json", fluids)
        wrote_overlays = True
    catalog = wave.build_object_catalog(slug, replayed["relations"])
    census.write_stable(root / "object_catalog.json", catalog)
    merged_objects = wave.merge_object_catalogs()
    census.write_stable(wave.BUNDLED_OBJECT_CATALOG, merged_objects)
    wave.write_item_models(merged_objects)
    wave.write_b1_scatter(merged_objects)
    merged_fluids = wave.merge_fluid_mappings()
    census.write_stable(wave.BUNDLED_FLUID_MAPPING, merged_fluids)
    if wrote_overlays or catalog.get("identities"):
        ordinary_source.invalidate_runtime_maps()
        replayed = ordinary_source.replay(slug)
        if slug.startswith("mixer/"):
            replayed["membership_note"] = wave.mixer_membership_note()
    blocked = [
        row for row in replayed["classifications"] if row["disposition"] == "blocked"
    ]
    if blocked:
        raise ValueError(
            f"{slug} still has {len(blocked)} blocked families: "
            + ",".join(row["template_key"] for row in blocked[:8])
        )
    if slug.startswith("drying/") and not any(
        str(row.get("template_key") or "").endswith("#0149")
        and row.get("disposition") == "complete"
        for row in replayed["classifications"]
    ):
        raise ValueError(
            "drying/ordinary-closure requires live-ready gt.recipe.drying#0149 "
            "as an independently complete family"
        )
    required = wave.build_required_forms(slug, replayed["required_forms"])
    census.write_stable(root / "required_forms.json", required)
    extra = list(required.get("new_prefix_forms") or [])
    _ensure_authority_source(slug, extra)
    if write_gate:
        from tools import build_gt6_material_form_gate as gate

        argv = sys.argv
        sys.argv = ["tools/build_gt6_material_form_gate.py", "--write"]
        try:
            code = gate.main()
        finally:
            sys.argv = argv
        if code:
            raise ValueError("material form gate --write failed")
    source = wave.build_source(slug, replayed)
    census.write_stable(spec.source_path, source)
    candidate = wave.build_candidate(slug, replayed)
    census.write_stable(root / "candidate_selection.json", candidate)
    lock = wave.build_lock(slug, replayed, candidate)
    census.write_stable(spec.lock_path, lock)
    operand_map = wave.build_operand_map(slug, replayed["production_relations"])
    census.write_stable(spec.operand_map_path, operand_map)
    identity_delta = wave.build_identity_delta(slug, operand_map)
    census.write_stable(root / "identity_ledger_delta.json", identity_delta)
    compiled = compile_mod.compile_wave(slug)
    compile_mod.write_tree(
        compiled["planned"],
        spec.generated_root,
        spec.generated_root,
        path_prefix=spec.path_prefix,
    )
    census.write_stable(root / "compile_report.json", compiled["report"])
    publication, policies = wave.build_publication(slug, lock, spec.target_map)
    census.write_stable(root / "publication_group_manifest.json", publication)
    _write_publication_policies(slug, policies)
    _write_support_supersede_dedup(slug)
    runtime_delta = wave.build_runtime_delta(slug, publication, policies)
    census.write_stable(root / "runtime_manifest_delta.json", runtime_delta)
    from tools.recipe_bulk import identity_v3, runtime_v3

    census.write_stable(identity_v3.OUTPUT, identity_v3.build())
    census.write_stable(runtime_v3.OUTPUT, runtime_v3.build())
    shards = wave.build_shards(slug, lock, spec.target_map)
    census.write_stable(root / "shard_manifest.json", shards)
    equivalence = wave.build_equivalence(slug, compiled["planned"], source)
    census.write_stable(spec.equivalence_path, equivalence)
    player_path = wave.build_player_path(slug, compiled["planned"])
    census.write_stable(root / "player_path.json", player_path)
    measurements_path = root / "measurements.json"
    measurements = census.load_json(measurements_path) if measurements_path.is_file() else {}
    load = wave.build_load(slug, publication, lock, measurements)
    census.write_stable(root / "load_projection.json", load)
    runtime = _runtime_manifest(slug, lock, publication)
    census.write_stable(root / "runtime_dependency_manifest.json", runtime)
    closeout = spec_for(slug)
    receipt_path = root / "gametest_receipt.json"
    receipt = census.load_json(receipt_path) if receipt_path.is_file() else None
    passed = bool(receipt and receipt.get("status") == "PASS")
    unique_active = closeout.unique_active_wave if passed else slug
    next_unassigned = closeout.next_unassigned if passed else False
    census = wave.build_census(slug, lock, publication, replayed, load)
    census.write_stable(root / "census_delta.json", census)
    verdict = wave.evaluate_wave_ready(
        slug,
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
        slug,
        census,
        lock,
        unique_active,
        next_unassigned,
        verdict=verdict,
    )
    census.write_stable(root / "topology.json", topology)
    readiness = wave.build_readiness(
        slug,
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
        "blocked": 0,
        "complete": lock["production"]["family_count"],
        "reclassified": len(replayed.get("reclassified") or []),
        "relations": lock["production"]["relation_count"],
        "status": readiness["status"],
        "wave_slug": slug,
    }


def write_receipt(slug: str, log_path: Path) -> dict[str, Any]:
    from tools.wave_closeout import spec_for as closeout_spec

    spec = closeout_spec(slug)
    if not log_path.is_file():
        raise ValueError(f"missing GameTest log: {log_path}")
    required = len(wave.discovered_gametest_ids(spec.gametest_java))
    text = log_path.read_text(encoding="utf-8")
    parsed = wave.parse_gametest_log(text, required)
    if parsed.get("status") != "PASS":
        raise ValueError(f"{slug} GameTest log is not PASS; skip is not pass")
    evidence = wave.wave_dir(slug) / "gametest.log"
    evidence.write_text(text.replace("\r\n", "\n"), encoding="utf-8", newline="\n")
    document = {
        "command": f".\\gradlew.bat runGameTestServer -PwaveRecipes={slug} --no-daemon",
        "failed": parsed["failed"],
        "java_source": census.relative(spec.gametest_java),
        "log_path": census.relative(evidence),
        "namespace": "cruciblecraft_wave_" + slug.replace("/", "_").replace("-", "_"),
        "note": "Isolated -PwaveRecipes result. Skip is not pass.",
        "passed": parsed["passed"],
        "required_tests": parsed["required_tests"],
        "schema_version": 1,
        "skip_is_not_pass": True,
        "status": "PASS",
        "test_ids": wave.discovered_gametest_ids(spec.gametest_java),
        "wave_slug": slug,
    }
    census.write_stable(wave.wave_dir(slug) / "gametest_receipt.json", document)
    return document


def rebuild_closeout(slug: str) -> dict[str, Any]:
    """Rebuild load/census/topology/readiness without touching lock or generated recipes."""
    spec = recipe_wave(slug)
    root = wave.wave_dir(slug)
    lock = census.load_json(spec.lock_path)
    publication = census.load_json(root / "publication_group_manifest.json")
    source = census.load_json(spec.source_path)
    candidate = census.load_json(root / "candidate_selection.json")
    measurements = (
        census.load_json(root / "measurements.json")
        if (root / "measurements.json").is_file()
        else {}
    )
    load = wave.build_load(slug, publication, lock, measurements)
    census.write_stable(root / "load_projection.json", load)
    replayed = {
        "host": source.get("host") or spec.host,
        "reclassified": list(candidate.get("reclassified") or []),
    }
    census = wave.build_census(slug, lock, publication, replayed, load)
    census.write_stable(root / "census_delta.json", census)
    closeout = spec_for(slug)
    receipt = (
        census.load_json(root / "gametest_receipt.json")
        if (root / "gametest_receipt.json").is_file()
        else None
    )
    passed = bool(receipt and receipt.get("status") == "PASS")
    unique_active = closeout.unique_active_wave if passed else slug
    next_unassigned = closeout.next_unassigned if passed else False
    equivalence = (
        census.load_json(spec.equivalence_path) if spec.equivalence_path.is_file() else None
    )
    player_path = (
        census.load_json(root / "player_path.json")
        if (root / "player_path.json").is_file()
        else None
    )
    verdict = wave.evaluate_wave_ready(
        slug,
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
        slug,
        census,
        lock,
        unique_active,
        next_unassigned,
        verdict=verdict,
    )
    census.write_stable(root / "topology.json", topology)
    readiness = wave.build_readiness(
        slug,
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
        "wave_slug": slug,
    }


def check_wave(slug: str) -> list[str]:
    spec = recipe_wave(slug)
    errors: list[str] = []
    if not spec.source_path.is_file():
        return [f"missing source for {slug}"]
    inputs = wave.load_closeout_inputs(slug)
    closeout = spec_for(slug)
    receipt = inputs["receipt"]
    passed = bool(receipt and receipt.get("status") == "PASS")
    unique_active = closeout.unique_active_wave if passed else slug
    verdict = wave.evaluate_wave_ready(
        slug,
        source=inputs["source"],
        lock=inputs["lock"],
        equivalence=inputs["equivalence"],
        player_path=inputs["player_path"],
        receipt=receipt,
        load=inputs["load"],
        census=inputs["census"],
        unique_active=unique_active,
    )
    readiness = census.load_json(wave.wave_dir(slug) / "readiness.json")
    topology = census.load_json(wave.wave_dir(slug) / "topology.json")
    if str(readiness.get("status") or "") != str(verdict.get("status") or ""):
        errors.append(
            f"{slug} readiness.status {readiness.get('status')} != derived {verdict.get('status')}"
        )
    if str(topology.get("status") or "") != str(verdict.get("status") or ""):
        errors.append(
            f"{slug} topology.status {topology.get('status')} != derived {verdict.get('status')}"
        )
    if readiness.get("status") == "WAVE_READY" and not verdict.get("ready"):
        errors.append(
            f"{slug} readiness claims WAVE_READY while derivation blockers="
            + ",".join(verdict.get("blockers") or [])
        )
    if verdict.get("ready") and str((inputs["load"] or {}).get("status") or "") != "LOAD_READY":
        errors.append(f"{slug} WAVE_READY requires load.status LOAD_READY")
    return errors


def repair_runtime_ids(slug: str) -> dict[str, Any]:
    """Rewrite operand runtime ids without resealing production lock or census."""
    spec = recipe_wave(slug)
    root = wave.wave_dir(slug)
    source = census.load_json(spec.source_path)
    relations = list(source.get("relations") or [])
    if not relations:
        raise ValueError(f"{slug} frozen source has no relations")
    catalog = wave.build_object_catalog(slug, relations)
    census.write_stable(root / "object_catalog.json", catalog)
    merged_objects = wave.merge_object_catalogs()
    census.write_stable(wave.BUNDLED_OBJECT_CATALOG, merged_objects)
    wave.write_item_models(merged_objects)
    wave.write_b1_scatter(merged_objects)
    operand_map = wave.build_operand_map(slug, relations)
    census.write_stable(spec.operand_map_path, operand_map)
    identity_delta = wave.build_identity_delta(slug, operand_map)
    census.write_stable(root / "identity_ledger_delta.json", identity_delta)
    from tools.recipe_bulk import identity_v3

    census.write_stable(identity_v3.OUTPUT, identity_v3.build())
    compiled = compile_mod.compile_wave(slug)
    compile_mod.write_tree(
        compiled["planned"],
        spec.generated_root,
        spec.generated_root,
        path_prefix=spec.path_prefix,
    )
    census.write_stable(root / "compile_report.json", compiled["report"])
    return {
        "identities": int(catalog.get("identity_count") or 0),
        "relations": compiled["report"]["relation_count"],
        "slug": slug,
        "status": "runtime_ids_repaired",
    }


def write_seal(slug: str) -> dict[str, Any]:
    return closeout_seal.write_wave_seal(slug)


def apply_integrated_measurements(slug: str, path: Path) -> dict[str, Any]:
    document = census.load_json(path)
    flat = wave.flatten_integrated(document)
    dest = wave.wave_dir(slug) / "measurements.json"
    census.write_stable(dest, flat)
    return rebuild_closeout(slug)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--wave", required=True)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--from-log", type=Path)
    parser.add_argument("--from-integrated", type=Path)
    parser.add_argument("--seal", action="store_true")
    parser.add_argument("--skip-gate", action="store_true")
    parser.add_argument("--rebuild-closeout", action="store_true")
    parser.add_argument(
        "--repair-runtime-ids",
        action="store_true",
        help=(
            "Rewrite object catalogs, identity deltas, and generated recipes "
            "without touching lock/census."
        ),
    )
    args = parser.parse_args(argv)
    if (
        args.write == args.check
        and not args.from_log
        and not args.from_integrated
        and not args.seal
        and not args.rebuild_closeout
        and not args.repair_runtime_ids
    ):
        parser.error(
            "choose --write, --check, --from-log, --from-integrated, --seal, "
            "--rebuild-closeout, or --repair-runtime-ids"
        )
    try:
        if args.repair_runtime_ids:
            summary = repair_runtime_ids(args.wave)
            print(json.dumps(summary, sort_keys=True))
            return 0
        if args.from_integrated is not None:
            summary = apply_integrated_measurements(args.wave, args.from_integrated)
            print(json.dumps(summary, sort_keys=True))
            return 0
        if args.from_log is not None:
            write_receipt(args.wave, args.from_log)
            emit_wave(args.wave, write_gate=False)
            print(f"Wrote GameTest receipt for {args.wave}")
            return 0
        if args.rebuild_closeout:
            summary = rebuild_closeout(args.wave)
            print(json.dumps(summary, sort_keys=True))
            return 0
        if args.seal:
            write_seal(args.wave)
            print(f"Wrote {census.relative(seal_path(args.wave))}")
            return 0
        if args.write:
            summary = emit_wave(args.wave, write_gate=not args.skip_gate)
            print(json.dumps(summary, sort_keys=True))
            return 0
        errors = check_wave(args.wave)
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{args.wave} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"ordinary wave failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
