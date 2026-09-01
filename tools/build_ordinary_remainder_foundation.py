#!/usr/bin/env python3
"""Shared operand/acquisition foundation for the ordinary remainder program."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import closeout_seal
from tools import t35_common as t35
from tools import t42_common as t42
from tools import t48_identities as identities
from tools.recipe_bulk import ordinary_r0 as r0
from tools.recipe_bulk import ordinary_source as source
from tools.recipe_bulk import ordinary_wave as wave
from tools.wave_closeout import seal_path as wave_seal_path

SLUG = "ordinary-remainder/operand-foundation"
ROOT_DIR = t35.TOOLS / "waves" / "ordinary-remainder" / "operand-foundation"
N_EXCEPTION = {
    "exception_kind": "final_execution_gap_host_partition",
    "padding_forbidden": True,
    "partition_rule": "target host",
    "program_denominator": 334,
}


def _scan_remainder() -> dict[str, Any]:
    objects: dict[tuple[str, int], dict[str, Any]] = {}
    fluids: dict[str, dict[str, Any]] = {}
    forms: set[str] = set()
    b0_families: list[dict[str, Any]] = []
    recycling: list[dict[str, Any]] = []
    owner_counts: Counter[str] = Counter()
    try:
        fluid_overlay = source.load_fluid_overlay()
    except OSError:
        fluid_overlay = {}
        mixer_fluids = (
            t35.TOOLS / "waves" / "mixer" / "ordinary-closure" / "fluid_mapping.json"
        )
        if mixer_fluids.is_file():
            for row in t35.load_json(mixer_fluids).get("mapping") or []:
                source_fluid = str(row.get("source_fluid") or "")
                runtime = str(row.get("cc_fluid_id") or "")
                if source_fluid and runtime:
                    fluid_overlay[source_fluid] = runtime
                    fluid_overlay[source_fluid.split(":", 1)[-1]] = runtime
    for host in r0.REMAINDER_HOSTS:
        work = source.load_work(host)
        recipes = t42.load_map_recipes(source.source_map_for(host))
        ordinary = source.load_ordinary_indices(host)
        assigned = source.assign_rows(recipes, work, ordinary, host)
        for item in work:
            owner_counts[item.owner] += 1
            row = {
                "family_id": item.family_id,
                "host": host,
                "owner": item.owner,
                "relation_count": item.relation_count,
                "template_key": item.template_key,
            }
            if item.owner == "acquisition/b0":
                b0_families.append(row)
            if item.owner == "recycling/evidence_needed":
                recycling.append(row)
            for _index, recipe in assigned.get(item.template_key, []):
                for operand in list(recipe.get("inputs") or []) + list(
                    recipe.get("outputs") or []
                ):
                    item_id = str(operand.get("item") or "")
                    if not item_id:
                        continue
                    meta = operand.get("meta")
                    if not isinstance(meta, int):
                        meta = 0
                    kind = source.object_kind_for(item_id) or identities.classify_source_item(
                        item_id
                    )
                    if item_id.startswith("gregtech:gt.meta."):
                        form = item_id.rsplit(".", 1)[-1]
                        forms.add(form)
                    objects.setdefault(
                        (item_id, meta),
                        {
                            "display": str(operand.get("displayName") or ""),
                            "hosts": set(),
                            "kind": kind or "unknown",
                            "meta": meta,
                            "source_item": item_id,
                        },
                    )["hosts"].add(host)
                for operand in list(recipe.get("fluidInputs") or []) + list(
                    recipe.get("fluidOutputs") or []
                ):
                    fluid = str(operand.get("fluid") or "")
                    if not fluid:
                        continue
                    fluids.setdefault(
                        fluid,
                        {
                            "cc_fluid_id": fluid_overlay.get(fluid)
                            or fluid_overlay.get(fluid.split(":", 1)[-1]),
                            "display": str(operand.get("displayName") or ""),
                            "hosts": set(),
                            "source_fluid": fluid,
                        },
                    )["hosts"].add(host)
    return {
        "b0_families": b0_families,
        "fluids": fluids,
        "objects": objects,
        "owner_counts": owner_counts,
        "recycling": recycling,
        "required_forms": sorted(forms),
    }


def _identity_row(
    item: str,
    meta: int,
    kind: str,
    display: str,
    hosts: list[str],
) -> dict[str, Any]:
    mapped = (
        "tool_head"
        if kind == "tool_head"
        else "multiitem" if kind == "multiitem" else "object"
    )
    runtime = source.proven_item_runtime(item, meta) or identities.runtime_id_for(
        mapped, item, meta
    )
    return {
        "acquisition_authority": SLUG,
        "behavior": kind,
        "chinese_name": identities.chinese_name(item, meta, display),
        "display_requirements": {
            "distinguishable": True,
            "holdable": True,
            "model": "item/generated",
        },
        "english_name": identities.english_name(item, meta, display),
        "fidelity": "exact_item",
        "hosts": hosts,
        "kind": kind,
        "mapping_class": "exact_item",
        "meta": meta,
        "owner": SLUG,
        "registry_kind": "item",
        "registry_path": identities.registry_path_for(mapped, item, meta),
        "runtime_id": runtime,
        "source_item": item,
        "source_revision": t35.SOURCE_REVISION,
        "texture": "minecraft:item/iron_ingot",
    }


def write_artifacts() -> dict[str, Any]:
    ROOT_DIR.mkdir(parents=True, exist_ok=True)
    r0_doc = r0.global_remainder_r0()
    if r0_doc["errors"]:
        raise ValueError("global R0 drifted: " + "; ".join(r0_doc["errors"]))
    scan = _scan_remainder()
    identities_out = [
        _identity_row(
            item,
            meta,
            str(row["kind"]),
            str(row["display"]),
            sorted(row["hosts"]),
        )
        for (item, meta), row in sorted(scan["objects"].items())
    ]
    catalog = {
        "generated_by": "python tools/build_ordinary_remainder_foundation.py",
        "identities": identities_out,
        "identity_count": len(identities_out),
        "kinds": dict(sorted(Counter(str(row["kind"]) for row in identities_out).items())),
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "TYPED_OBJECT_CATALOG",
        "wave_slug": SLUG,
    }
    fluid_rows = []
    ledger_fluids = source.load_ledger_fluid_overlay()
    for fluid, row in sorted(scan["fluids"].items()):
        runtime = (
            row.get("cc_fluid_id")
            or ledger_fluids.get(fluid)
            or ledger_fluids.get(fluid.split(":", 1)[-1])
            or wave.native_fluid_id(fluid)
        )
        fluid_rows.append(
            {
                "cc_fluid_id": runtime,
                "display": row["display"],
                "english_name": row["display"]
                or fluid.replace(".", " ").replace("_", " "),
                "fidelity": "mapped" if runtime else "needs_current_expression",
                "hosts": sorted(row["hosts"]),
                "owner": SLUG,
                "source_fluid": fluid,
                "source_revision": t35.SOURCE_REVISION,
            }
        )
    fluids = {
        "counts": {
            "mapped": sum(1 for row in fluid_rows if row["fidelity"] == "mapped"),
            "needs_current_expression": sum(
                1 for row in fluid_rows if row["fidelity"] != "mapped"
            ),
        },
        "generated_by": "python tools/build_ordinary_remainder_foundation.py",
        "mapping": fluid_rows,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "TYPED_FLUID_CATALOG",
        "wave_slug": SLUG,
    }
    forms = {
        "generated_by": "python tools/build_ordinary_remainder_foundation.py",
        "required_forms": scan["required_forms"],
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "REQUIRED_FORMS",
        "wave_slug": SLUG,
    }
    acquisition_rows = []
    for family in scan["b0_families"]:
        acquisition_rows.append(
            {
                "b0": "current_survival_sources",
                "b1": "foundation_scatter_and_registered_runtime",
                "disposition": "needs_current_expression",
                "evidence": {
                    "drop": "cruciblecraft:semantic_object_scatter item entity",
                    "place": "cruciblecraft:semantic_object_scatter overworld placed feature",
                    "repeat": "rarity 128 biome modifier minecraft:is_overworld",
                },
                "family_id": family["family_id"],
                "host": family["host"],
                "kind": "worldgen_scatter",
                "owner": family["owner"],
                "recheck_condition": "host ordinary-closure player_path must consume this B1 identity",
            }
        )
    acquisition = {
        "b0_family_count": len(acquisition_rows),
        "generated_by": "python tools/build_ordinary_remainder_foundation.py",
        "rows": acquisition_rows,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "ACQUISITION_MANIFEST",
        "wave_slug": SLUG,
    }
    work_set = {
        "completion_delta": 0,
        "exception": N_EXCEPTION,
        "family_count": 0,
        "generated_by": "python tools/build_ordinary_remainder_foundation.py",
        "hosts": {
            host: {
                "family_count": row["family_count"],
                "relation_count": row["relation_count"],
            }
            for host, row in r0_doc["hosts"].items()
        },
        "owns_families": 0,
        "remaining_recipe_gap": 334,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "WORK_SET",
        "wave_slug": SLUG,
    }
    blocker = {
        "current_projection": {
            "acquisition_b0": len(scan["b0_families"]),
            "owner_counts": dict(sorted(scan["owner_counts"].items())),
            "recycling_evidence_needed": len(scan["recycling"]),
        },
        "frozen_overlay_is_diagnosis_only": True,
        "generated_by": "python tools/build_ordinary_remainder_foundation.py",
        "note": (
            "Frozen t42_blocker_overlay needs_unique_block_or_mte is not treated "
            "as a live MTE deficit. Current projection uses owner rows + dump operands."
        ),
        "recycling_families": scan["recycling"],
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "BLOCKER_AUDIT",
        "wave_slug": SLUG,
    }
    drying = r0.remaining_summary("cruciblecraft:drying")
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "drying_candidate": {
            "family_count": drying["family_count"],
            "family_ids": drying["family_ids"],
            "relation_count": drying["relation_count"],
        },
        "exception": N_EXCEPTION,
        "generated_by": "python tools/build_ordinary_remainder_foundation.py",
        "opening_execution_gap": 334,
        "partial_family_count": 0,
        "reclassification_delta": 0,
        "remaining_ordinary": {
            "complete_family_count": 0,
            "completion_delta": 0,
            "deferred_recycling_count": 1819,
            "opening_execution_gap": 334,
            "partial_family_count": 0,
            "reclassification_delta": 0,
            "remaining_ordinary_families": 334,
        },
        "remaining_recipe_gap": 334,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": SLUG,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": "python tools/build_ordinary_remainder_foundation.py",
        "next_unassigned": True,
        "remaining_recipe_gap": 334,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": None,
        "wave_slug": SLUG,
    }
    readiness = {
        "evidence": {
            "complete_family_count": 0,
            "completion_delta": 0,
            "drying_candidate_families": drying["family_count"],
            "fluid_count": len(fluid_rows),
            "global_r0": {
                "family_total": r0_doc["family_total"],
                "relation_total": r0_doc["relation_total"],
            },
            "object_count": len(identities_out),
            "partial_family_count": 0,
            "remaining_recipe_gap": 334,
        },
        "generated_by": "python tools/build_ordinary_remainder_foundation.py",
        "next_unassigned": True,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": None,
        "wave_complete": True,
        "wave_slug": SLUG,
    }
    t35.write_stable(
        ROOT_DIR / "wave.json",
        {
            "cohort": "operand-foundation",
            "depends_on": ["ordinary-wave/closeout-integrity-repair"],
            "owns_families": 0,
            "schema_version": 1,
            "unique_active_wave": False,
            "wave_slug": SLUG,
        },
    )
    t35.write_stable(ROOT_DIR / "work_set.json", work_set)
    t35.write_stable(
        ROOT_DIR / "r0.json",
        {
            **r0_doc,
            "exception": N_EXCEPTION,
            "generated_by": "python tools/build_ordinary_remainder_foundation.py",
            "schema_version": 1,
            "wave_slug": SLUG,
        },
    )
    t35.write_stable(ROOT_DIR / "blocker_audit.json", blocker)
    t35.write_stable(ROOT_DIR / "object_catalog.json", catalog)
    t35.write_stable(ROOT_DIR / "required_forms.json", forms)
    t35.write_stable(ROOT_DIR / "fluid_mapping.json", fluids)
    t35.write_stable(ROOT_DIR / "acquisition_manifest.json", acquisition)
    t35.write_stable(ROOT_DIR / "census_delta.json", census)
    t35.write_stable(ROOT_DIR / "topology.json", topology)
    t35.write_stable(ROOT_DIR / "readiness.json", readiness)
    merged_objects = wave.merge_object_catalogs()
    t35.write_stable(wave.BUNDLED_OBJECT_CATALOG, merged_objects)
    wave.write_item_models(merged_objects)
    wave.write_b1_scatter(merged_objects)
    merged_fluids = wave.merge_fluid_mappings()
    t35.write_stable(wave.BUNDLED_FLUID_MAPPING, merged_fluids)
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
        "schema_version": 2,
        "status": "SEALED",
        "card_id": SLUG,
        "sealed_at_wave": SLUG,
        "source_revision": t35.SOURCE_REVISION,
        "generated_by": "python tools/build_ordinary_remainder_foundation.py --write",
        "complete_family_count": 0,
        "relation_count": 0,
        "reclassification_delta": 0,
        "remaining_recipe_gap": 334,
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
        "note": "Zero-family operand/acquisition foundation. No completion delta.",
        "repair_wave": "ordinary-wave/closeout-integrity-repair",
        "supersedes_sha256": "ordinary-remainder-operand-foundation-opening",
    }
    t35.write_stable(ROOT_DIR / "closeout_seal.json", seal)
    return {
        "drying_candidate_families": drying["family_count"],
        "fluid_count": len(fluid_rows),
        "object_count": len(identities_out),
        "remaining_recipe_gap": 334,
        "status": "WAVE_READY",
        "wave_slug": SLUG,
    }


def check() -> list[str]:
    errors: list[str] = []
    r0_doc = r0.global_remainder_r0()
    errors.extend(r0_doc["errors"])
    readiness = ROOT_DIR / "readiness.json"
    if not readiness.is_file():
        return errors + ["missing foundation readiness.json"]
    document = t35.load_json(readiness)
    if document.get("status") != "WAVE_READY":
        errors.append("foundation readiness is not WAVE_READY")
    census = t35.load_json(ROOT_DIR / "census_delta.json")
    if int(census.get("complete_family_count", -1)) != 0:
        errors.append("foundation must not claim family completion")
    if int(census.get("remaining_recipe_gap", -1)) != 334:
        errors.append("foundation remaining_recipe_gap drifted from 334")
    drying = (census.get("drying_candidate") or {}).get("family_count")
    opening = (r0.global_remainder_r0().get("hosts") or {}).get(
        "cruciblecraft:drying", {}
    ).get("family_count")
    if drying != opening:
        errors.append(f"drying candidate drifted: catalog={drying} opening={opening}")
    if not (ROOT_DIR / "closeout_seal.json").is_file():
        errors.append("missing foundation closeout_seal.json")
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
            payload = write_artifacts()
            print(json.dumps(payload, sort_keys=True))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("ordinary-remainder operand foundation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"operand foundation failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
