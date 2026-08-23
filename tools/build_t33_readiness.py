#!/usr/bin/env python3
"""Build T33 surface-scatter readiness.

``status`` is derived. T33_READY requires static closure on the audited
surface-scatter declaration, unchanged T20 catalog denominators, S0 rock
seeds aligned with dynamic tag membership, rock_pack reachability,
portfolio CC-4.5-P4 supersession, and the registry-resolved surface-rock
GameTest contract in source. This artifact does not claim
``runGameTestServer`` was executed.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
OUTPUT = TOOLS / "t33_readiness.json"
POLICY = TOOLS / "t33_readiness_policy.json"
BUILDER = Path(__file__).resolve()
WORLDGEN_READINESS = TOOLS / "worldgen_catalog_readiness.json"
REACHABILITY = TOOLS / "t21_operand_reachability.json"
OPEN_ITEMS = TOOLS / "t27_portfolio" / "deferred_open_items.json"
SURFACE_SCATTER = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/surface_scatter.json"
)
CONFIGURED_SURFACE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/configured_feature/surface_rock_scatter.json"
)
PLACED_SURFACE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/placed_feature/surface_rock_scatter.json"
)
BIOME_MODIFIER = (
    ROOT
    / "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/add_surface_rocks.json"
)
SURFACE_FEATURE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/worldgen/SurfaceRockFeature.java"
)
GAMETEST = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java"
)
ROCK_PACK = ROOT / "src/main/resources/data/cruciblecraft/recipe/rock_pack.json"
CC_4_5_P4 = "CC-4.5-P4"
EXPECTED_GAMETEST = "surfaceRockScatterPlacesFromRuntimeTag"
REPORT_OWNED = ("currentness", "runtime", "status")


def _portfolio_item(open_items: dict[str, Any], item_id: str) -> dict[str, Any]:
    for row in open_items.get("records") or []:
        if row.get("id") == item_id:
            return row
    raise ValueError(f"missing portfolio open item {item_id}")


def _gametest_contract_present() -> bool:
    if not GAMETEST.is_file():
        return False
    text = GAMETEST.read_text(encoding="utf-8")
    required_fragments = (
        f"void {EXPECTED_GAMETEST}(",
        "SurfaceRockConfiguration",
        "surface_rock_scatter",
        "config.rockTag()",
        "findSurfaceRockScatterSeed(",
        "RockBlock",
    )
    return all(fragment in text for fragment in required_fragments)


def _surface_scatter_audit_ok(
    worldgen: dict[str, Any],
    policy: dict[str, Any],
) -> dict[str, bool]:
    scatter = worldgen.get("surface_scatter") or {}
    tag_source = worldgen.get("surface_scatter_rock_tag_source") or {}
    inputs = worldgen.get("inputs") or {}
    historical = policy["historical_t20_counts"]
    counts = worldgen.get("counts") or {}
    return {
        "audit_mode_check_only": scatter.get("audit_mode") == "check_only",
        "declaration_design_policy": scatter.get("design_policy") == "DESIGN_POLICY",
        "runtime_inputs_present": all(
            key in inputs
            for key in (
                "surface_scatter_declarations",
                "configured_surface_runtime",
                "placed_surface_runtime",
                "surface_biome_modifier_runtime",
            )
        ),
        "rock_tag_is_c_rocks": scatter.get("rock_tag") == "c:rocks",
        "runtime_pack_is_generated_material_pack": (
            tag_source.get("runtime_pack") == "GeneratedMaterialPack"
        ),
        "catalog_generated_files_263": counts.get("catalog_generated_files")
        == historical["catalog_generated_files"],
        "all_worldgen_files_274": counts.get("all_worldgen_files")
        == historical["all_worldgen_files"],
        "closure_vein_classifications_129": counts.get(
            "closure_vein_classifications"
        )
        == historical["closure_vein_classifications"],
    }


def _reachability_ok(reachability: dict[str, Any]) -> dict[str, bool]:
    closure = reachability.get("closure") or {}
    scatter = closure.get("surface_scatter") or {}
    unreachable = {
        entry["recipe"]
        for entry in closure.get("unreachable_operand_recipes_all") or []
    }
    seed_count = int(closure.get("seed_surface_rock_count") or 0)
    dynamic_members = int(scatter.get("dynamic_tag_members") or 0)
    rock_material_count = int(scatter.get("rock_material_count") or 0)
    seed_materials = int(closure.get("seed_surface_rock_materials") or 0)
    aligned = (
        seed_count > 0
        and seed_count == dynamic_members == rock_material_count == seed_materials
    )
    return {
        "seed_surface_rock_nonzero": seed_count > 0,
        "s0_seeds_match_dynamic_tag_members": aligned,
        "rock_pack_not_unreachable": common.relative(ROCK_PACK) not in unreachable,
        "surface_scatter_rock_tag_c_rocks": scatter.get("rock_tag") == "c:rocks",
    }


def _portfolio_ok(open_items: dict[str, Any]) -> dict[str, bool]:
    item = _portfolio_item(open_items, CC_4_5_P4)
    axes = item.get("axes") or {}
    return {
        "cc_4_5_p4_closed_open_item": item.get("kind") == "closed_open_item",
        "cc_4_5_p4_owner_t33": item.get("owner") == "T33",
        "cc_4_5_p4_disposition_closed": item.get("disposition") == "closed",
        "cc_4_5_p4_closure_axis_closed": (
            (axes.get("closure") or {}).get("status") == "closed"
        ),
    }


def build() -> dict[str, Any]:
    for path in (
        POLICY,
        WORLDGEN_READINESS,
        REACHABILITY,
        OPEN_ITEMS,
        SURFACE_SCATTER,
        CONFIGURED_SURFACE,
        PLACED_SURFACE,
        BIOME_MODIFIER,
        SURFACE_FEATURE,
        GAMETEST,
    ):
        if not path.is_file():
            raise FileNotFoundError(common.relative(path))
    policy = common.load_json(POLICY)
    worldgen = common.load_json(WORLDGEN_READINESS)
    reachability = common.load_json(REACHABILITY)
    open_items = common.load_json(OPEN_ITEMS)
    scatter = worldgen.get("surface_scatter") or {}
    geometry = worldgen.get("geometry_policy") or {}
    t20_fidelity = worldgen.get("t20_fidelity") or {}
    closure_policy = policy.get("closure_policy") or {}
    gametest_policy = policy.get("gametest_policy") or {}
    audit = _surface_scatter_audit_ok(worldgen, policy)
    reach = _reachability_ok(reachability)
    portfolio = _portfolio_ok(open_items)
    gametest_contract = _gametest_contract_present()
    closure = {
        **audit,
        **reach,
        **portfolio,
        "gametest_contract_present": gametest_contract,
    }
    fidelity = {
        "design_policy_surface_scatter": scatter.get("design_policy")
        == "DESIGN_POLICY",
        "explicit_provenance_pinned": (
            "T33 GT6 WorldgenRocks surface scatter design policy"
            in str(scatter.get("provenance") or "")
        ),
        "independent_surface_feature": scatter.get("feature_type")
        == "cruciblecraft:surface_rock_scatter",
        "t20_fidelity_statuses_preserved": t20_fidelity.get("statuses")
        == geometry.get("fidelity_statuses"),
        "t20_non_claim_preserved": bool(
            str(geometry.get("non_claim") or "").strip()
        ),
        "source_derived_and_design_policy_semantics": (
            t20_fidelity.get("statuses", {}).get("SOURCE_DERIVED") == 73
            and t20_fidelity.get("statuses", {}).get("DESIGN_POLICY") == 56
        ),
    }
    load_data = {
        "catalog_generated_files_unchanged": audit["catalog_generated_files_263"],
        "all_worldgen_files_unchanged": audit["all_worldgen_files_274"],
        "closure_vein_classifications_unchanged": audit[
            "closure_vein_classifications_129"
        ],
        "no_publication_delta_claim": policy.get("load_policy", {}).get(
            "publication_delta_claim"
        )
        is False,
        "no_t20_generated_output_delta": (
            int((worldgen.get("counts") or {}).get("catalog_generated_files") or 0)
            == policy["historical_t20_counts"]["catalog_generated_files"]
        ),
    }
    closure_ok = all(value is True for value in closure.values())
    fidelity_ok = all(value is True for value in fidelity.values())
    load_ok = all(value is True for value in load_data.values())
    gates_ok = (
        closure_ok
        and fidelity_ok
        and load_ok
        and len(closure_policy.get("pending") or []) == 0
        and closure_policy.get("final_closure_attempted") is True
    )
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(POLICY): common.sha256_file(POLICY),
        common.relative(WORLDGEN_READINESS): common.sha256_file(WORLDGEN_READINESS),
        common.relative(REACHABILITY): common.sha256_file(REACHABILITY),
        common.relative(OPEN_ITEMS): common.sha256_file(OPEN_ITEMS),
        common.relative(SURFACE_SCATTER): common.sha256_file(SURFACE_SCATTER),
        common.relative(CONFIGURED_SURFACE): common.sha256_file(CONFIGURED_SURFACE),
        common.relative(PLACED_SURFACE): common.sha256_file(PLACED_SURFACE),
        common.relative(BIOME_MODIFIER): common.sha256_file(BIOME_MODIFIER),
        common.relative(SURFACE_FEATURE): common.sha256_file(SURFACE_FEATURE),
        common.relative(GAMETEST): common.sha256_file(GAMETEST),
    }
    document: dict[str, Any] = {
        "closure": closure,
        "closure_policy": closure_policy,
        "currentness": {"owned_inputs": owned_inputs},
        "fidelity": fidelity,
        "gametest_policy": gametest_policy,
        "generated_by": "python tools/build_t33_readiness.py --write",
        "load": load_data,
        "owned_inputs": owned_inputs,
        "policy": policy.get("status_policy"),
        "runtime": {
            "gametest_contract_present": gametest_contract,
            "manual_gametest_required": True,
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status_owner": "static_gates_only",
        "surface_scatter_summary": {
            "audit_mode": scatter.get("audit_mode"),
            "configured_feature": scatter.get("configured_feature"),
            "design_policy": scatter.get("design_policy"),
            "dynamic_tag_members": (
                (reachability.get("closure") or {})
                .get("surface_scatter", {})
                .get("dynamic_tag_members")
            ),
            "id": scatter.get("id"),
            "placed_feature": scatter.get("placed_feature"),
            "rock_tag": scatter.get("rock_tag"),
            "seed_surface_rock_count": (
                (reachability.get("closure") or {}).get("seed_surface_rock_count")
            ),
        },
        "t20_counts": policy["historical_t20_counts"],
    }
    if gates_ok:
        document["status"] = "T33_READY"
    return document


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = build()
    actual = json.loads(OUTPUT.read_text(encoding="utf-8"))
    for key in REPORT_OWNED:
        expected.pop(key, None)
        actual.pop(key, None)
    if common.stable_json(expected) != common.stable_json(actual):
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    args = parser.parse_args()
    try:
        if args.write:
            write()
            print(f"Wrote {common.relative(OUTPUT)}")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors))
            return 1
        print("T33 readiness is current.")
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"T33 readiness build failed: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
