#!/usr/bin/env python3
"""Build the T21 chemical classification axis and carbon composition slice."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t21_chemical_axis_policy.json"
T5_READINESS = TOOLS / "t5_chemical_readiness.json"
T5_MANIFEST = TOOLS / "t5_chemical_recipe_manifest.json"
NORMALIZED_MATERIALS = TOOLS / "gt6_oredict_materials_normalized.json"
MATERIAL_ROOT = ROOT / "src/main/resources/data/cruciblecraft/materials"
RAW_ELECTROLYZER = (
    ROOT
    / "gt6_dump/gt6_recipe_dump/maps/gt.recipe.electrolyzer.json"
)
RUNTIME_RECIPE_ROOT = (
    ROOT
    / "src/t5_chemical_generated/resources/data/cruciblecraft/recipe/t5"
)
AXIS_OUTPUT = TOOLS / "t21_chemical_axis.json"
EXPECTED_OUTPUT = TOOLS / "t21_composition_expected.json"
EXPANSION_OUTPUT = TOOLS / "t21_composition_expansion.json"
REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def value_sha256(value: Any) -> str:
    return hashlib.sha256(
        stable(value).encode("utf-8")
    ).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T21_CHEMICAL_CALIBRATION_POLICY"
        or policy.get("source", {}).get("revision") != REVISION
        or policy.get("denominator", {}).get("expected_candidates") != 224
    ):
        raise ValueError("T21 chemical policy header/denominator drifted")
    family = policy.get("selected_family") or {}
    members = family.get("members") or []
    if (
        family.get("id") != "carbon_electrolysis"
        or family.get("map") != "cruciblecraft:electrolyzer"
        or [row.get("material") for row in members]
        != ["charcoal", "coal"]
        or len({row.get("source_recipe_index") for row in members}) != 2
    ):
        raise ValueError("T21 selected carbon family drifted")
    load_policy = policy.get("load") or {}
    if (
        load_policy.get("source_facts") != 2
        or load_policy.get("authored_rules") != 1
        or load_policy.get("datapack_files") != 2
        or load_policy.get("logical_rows") != 2
        or any(
            load_policy.get(key) != 0
            for key in ("logical_delta", "eager_delta", "lazy_delta")
        )
    ):
        raise ValueError("T21 selected-family load contract drifted")


def manifest_rows() -> list[dict[str, Any]]:
    document = load(T5_MANIFEST)
    rows = document.get("generated")
    if document.get("status") != "closure_ready" or not isinstance(rows, list):
        raise ValueError("T5 recipe manifest is not closure-ready")
    return rows


def build_classification(
    policy: dict[str, Any],
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    readiness = load(T5_READINESS)
    candidates = readiness.get("chemical_materials")
    if not isinstance(candidates, list):
        raise ValueError("T5 chemical candidate ledger is missing")
    denominator = policy["denominator"]
    counts = readiness["counts"]
    input_counts = counts["input_ledgers"]
    if (
        len(candidates) != denominator["expected_candidates"]
        or counts["classified"] != denominator["expected_candidates"]
        or counts["unclassified"] != 0
        or input_counts["unique_chemical_union"] != 224
        or input_counts["byproduct_only_debt"]
        != denominator["expected_byproduct_only_debt"]
        or input_counts["byproduct_materials_also_in_chemical_union"]
        != denominator["expected_byproduct_overlap"]
        or input_counts["byproduct_materials_outside_chemical_union"]
        != denominator["expected_byproduct_outside"]
    ):
        raise ValueError("authoritative T5 chemical denominator drifted")

    generated: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in manifest_rows():
        material = row.get("material")
        if isinstance(material, str):
            generated[material].append(row)
    selected = {
        row["material"]
        for row in policy["selected_family"]["members"]
    }
    result: list[dict[str, Any]] = []
    for source in candidates:
        material = source["material"]
        runtime_rows = sorted(
            generated.get(material, []),
            key=lambda row: row["id"],
        )
        if source["source_loader_eligible"]:
            classification = "composition_generated"
            if material in selected:
                implementation = "SELECTED_T21"
            elif source["no_decompose"]:
                implementation = "QUARANTINED_BY_NO_DECOMPOSE"
            elif runtime_rows:
                implementation = "PREIMPLEMENTED_T5"
            else:
                implementation = "SOURCE_ELIGIBLE_NOT_PUBLISHED"
            blocking = None
        elif runtime_rows:
            classification = "named_reaction"
            implementation = "PREIMPLEMENTED_T5_EXACT_ROW"
            blocking = None
        else:
            classification = "blocked_by_new_subsystem"
            implementation = "BLOCKED"
            blocking = {
                "reason": source["reason"],
                "reason_code": source["reason_code"],
                "dependency": (
                    "A fixed source reaction/behavior, machine semantic, "
                    "container route, or explicit no-route product decision."
                ),
                "owner": "T27 portfolio classification",
                "replacement_condition": (
                    "A fixed-revision source fact and closed runtime consumer "
                    "identify one executable implementation shape."
                ),
                "recheck_point": "T27 and any generated chemistry card",
            }
        map_ids = sorted({
            destination["map"]
            for destination in source["map_destinations"]
        } | {
            f"gt.recipe.{row['route']}" for row in runtime_rows
        })
        result.append({
            "material": material,
            "classification": classification,
            "implementation": implementation,
            "t5_classification": source["classification"],
            "origins": list(source["origins"]),
            "source_loader_eligible": source["source_loader_eligible"],
            "no_decompose": source["no_decompose"],
            "composition": dict(source["composition"]),
            "component_common_divider": source[
                "component_common_divider"
            ],
            "referenced_recipe_maps": map_ids,
            "runtime_recipe_ids": [row["id"] for row in runtime_rows],
            "blocking": blocking,
        })

    if len(result) != 224 or len({row["material"] for row in result}) != 224:
        raise ValueError("T21 classification does not cover 224 unique candidates")
    classification_counts = Counter(
        row["classification"] for row in result
    )
    classification_counts["prefix_matrix"] = 0
    if sum(classification_counts.values()) != 224:
        raise ValueError("T21 classification counts do not close")
    map_uses: dict[str, set[str]] = defaultdict(set)
    for row in result:
        for map_id in row["referenced_recipe_maps"]:
            map_uses[map_id].add(row["classification"])
    maps = [
        {
            "map": map_id,
            "candidate_classifications": sorted(kinds),
            "status": "CLASSIFIED",
        }
        for map_id, kinds in sorted(map_uses.items())
    ]
    summary = {
        "candidate_count": len(result),
        "classifications": dict(sorted(classification_counts.items())),
        "referenced_recipe_maps": len(maps),
        "recipe_maps": maps,
        "unclassified": 0,
        "byproduct_only_debt": {
            "rows": input_counts["byproduct_only_debt"],
            "overlap_with_candidates": input_counts[
                "byproduct_materials_also_in_chemical_union"
            ],
            "outside_candidates": input_counts[
                "byproduct_materials_outside_chemical_union"
            ],
            "added_to_candidate_denominator": 0,
        },
    }
    return result, summary


def selected_manifest_row(material: str) -> dict[str, Any]:
    selected = [
        row
        for row in manifest_rows()
        if row.get("material") == material
        and row.get("route") == "electrolyzer"
    ]
    if len(selected) != 1:
        raise ValueError(
            f"{material}: expected one T5 electrolyzer source row, got "
            f"{len(selected)}"
        )
    return selected[0]


def material_document(material: str) -> dict[str, Any]:
    path = MATERIAL_ROOT / f"{material}.json"
    document = load(path)
    if document.get("id") != material:
        raise ValueError(f"{material}: material id drifted")
    return document


def readiness_row(material: str) -> dict[str, Any]:
    rows = [
        row
        for row in load(T5_READINESS)["chemical_materials"]
        if row["material"] == material
    ]
    if len(rows) != 1:
        raise ValueError(f"{material}: T5 readiness row is not unique")
    return rows[0]


def production_recipe(
    policy: dict[str, Any],
    member: dict[str, Any],
) -> dict[str, Any]:
    family = policy["selected_family"]
    material = member["material"]
    material_source = material_document(material)
    readiness = readiness_row(material)
    manifest = selected_manifest_row(material)
    if (
        material_source.get("composition") != {"carbon": 1}
        or material_source.get("no_decompose") is not False
        or readiness["classification"] != "route_ready"
        or readiness["source_loader_eligible"] is not True
        or readiness["no_decompose"] is not False
        or readiness["component_common_divider"] != 1
        or readiness["normalized_components"]
        != [{
            "cc_units": 144,
            "integral_cc_units": True,
            "material": "Carbon",
            "material_id": 60,
            "numerator_u": 648_648_000,
        }]
        or manifest["source"]["recipe_index"]
        != member["source_recipe_index"]
    ):
        raise ValueError(
            f"{material}: carbon composition family source facts drifted"
        )
    source_reference = (
        "gt6_dump/gt6_recipe_dump/maps/gt.recipe.electrolyzer.json"
        f"#recipes[{member['source_recipe_index']}]"
    )
    return {
        "can_be_buffered": family["can_be_buffered"],
        "duration": family["duration"],
        "eut": family["eut"],
        "item_input_counts": [readiness["component_common_divider"]],
        "item_inputs": [{"tag": f"c:dusts/{material}"}],
        "item_outputs": [{
            "count": (
                readiness["normalized_components"][0]["cc_units"] // 144
            ),
            "id": "cruciblecraft:carbon/dust",
        }],
        "map": family["map"],
        "output_chances": [10_000],
        "provenance": {
            "evidence_hashes": [REVISION],
            "selected_source_recipe": source_reference,
            "source_kind": "gt6_pinned_dump_projection",
        },
        "type": "cruciblecraft:gt_recipe",
    }


def independent_expected_recipe(
    policy: dict[str, Any],
    member: dict[str, Any],
    raw_recipe: dict[str, Any],
) -> dict[str, Any]:
    """Project from pinned row/manifest evidence without composition logic."""
    family = policy["selected_family"]
    material = member["material"]
    manifest = selected_manifest_row(material)
    source_inputs = manifest["source_inputs"]
    source_outputs = manifest["source_outputs"]
    items_in = source_inputs["items"]
    items_out = source_outputs["items"]
    if (
        source_inputs["fluids"] != []
        or source_outputs["fluids"] != []
        or items_in != [{
            "count": 1,
            "material": material,
            "prefix": "dust",
            "source_material_id": (
                8336 if material == "charcoal" else 8334
            ),
            "source_prefix": "dust",
        }]
        or items_out != [{
            "count": 1,
            "material": "carbon",
            "prefix": "dust",
            "source_material_id": 60,
            "source_prefix": "dust",
        }]
    ):
        raise ValueError(f"{material}: pinned expected source I/O drifted")
    if (
        raw_recipe.get("duration") != family["duration"]
        or raw_recipe.get("euPerTick") != family["eut"]
        or raw_recipe.get("specialValue") != family["special_value"]
        or raw_recipe.get("canBeBuffered") is not family["can_be_buffered"]
    ):
        raise ValueError(f"{material}: pinned expected scalar fields drifted")
    source_reference = (
        "gt6_dump/gt6_recipe_dump/maps/gt.recipe.electrolyzer.json"
        f"#recipes[{member['source_recipe_index']}]"
    )
    return {
        "can_be_buffered": bool(raw_recipe["canBeBuffered"]),
        "duration": max(16, int(raw_recipe["duration"])),
        "eut": max(16, int(raw_recipe["euPerTick"])),
        "item_input_counts": [int(items_in[0]["count"])],
        "item_inputs": [{"tag": f"c:dusts/{items_in[0]['material']}"}],
        "item_outputs": [{
            "count": int(items_out[0]["count"]),
            "id": (
                f"cruciblecraft:{items_out[0]['material']}/"
                f"{items_out[0]['prefix']}"
            ),
        }],
        "map": family["map"],
        "output_chances": [10_000],
        "provenance": {
            "evidence_hashes": [REVISION],
            "selected_source_recipe": source_reference,
            "source_kind": "gt6_pinned_dump_projection",
        },
        "type": "cruciblecraft:gt_recipe",
    }


def build_expected_full_replay(
    policy: dict[str, Any],
) -> dict[str, Any]:
    raw = load(RAW_ELECTROLYZER)
    recipes = raw.get("recipes")
    if (
        raw.get("nameInternal") != "gt.recipe.electrolyzer"
        or raw.get("recipeCount") != 290
        or not isinstance(recipes, list)
        or len(recipes) != 290
    ):
        raise ValueError("pinned electrolyzer source map drifted")
    rows = []
    for member in policy["selected_family"]["members"]:
        index = member["source_recipe_index"]
        raw_recipe = recipes[index]
        rows.append({
            "material": member["material"],
            "source_recipe_index": index,
            "source_row_sha256": value_sha256(raw_recipe),
            "recipe": independent_expected_recipe(
                policy, member, raw_recipe
            ),
        })
    return {
        "schema_version": 1,
        "status": "T21_COMPOSITION_EXPECTED_READY",
        "family": policy["selected_family"]["id"],
        "source": {
            "repository": policy["source"]["repository"],
            "revision": REVISION,
            "map": "gt.recipe.electrolyzer",
            "map_recipe_count": 290,
            "map_sha256": sha256(RAW_ELECTROLYZER),
        },
        "rows": rows,
    }


def validate_compact_expected(
    policy: dict[str, Any],
    expected: dict[str, Any],
) -> None:
    if (
        expected.get("schema_version") != 1
        or expected.get("status") != "T21_COMPOSITION_EXPECTED_READY"
        or expected.get("family") != policy["selected_family"]["id"]
        or expected.get("source", {}).get("revision") != REVISION
        or expected.get("source", {}).get("map_recipe_count") != 290
    ):
        raise ValueError("T21 compact expected header drifted")
    rows = expected.get("rows")
    if (
        not isinstance(rows, list)
        or [row.get("material") for row in rows] != ["charcoal", "coal"]
        or any(
            not isinstance(row.get("source_row_sha256"), str)
            or len(row["source_row_sha256"]) != 64
            for row in rows
        )
    ):
        raise ValueError("T21 compact expected rows drifted")


def build_expansion(
    policy: dict[str, Any],
    expected: dict[str, Any],
) -> dict[str, Any]:
    validate_compact_expected(policy, expected)
    expected_by_material = {
        row["material"]: row["recipe"] for row in expected["rows"]
    }
    rows = []
    for member in policy["selected_family"]["members"]:
        material = member["material"]
        production = production_recipe(policy, member)
        if production != expected_by_material[material]:
            raise ValueError(
                f"{material}: composition expansion differs from independent expected"
            )
        runtime_path = (
            RUNTIME_RECIPE_ROOT / "electrolyzer" / f"{material}.json"
        )
        runtime = load(runtime_path)
        if runtime != production:
            raise ValueError(
                f"{material}: runtime recipe differs from composition expansion"
            )
        rows.append({
            "material": material,
            "runtime_recipe_id": f"cruciblecraft:t5/electrolyzer/{material}",
            "runtime_path": relative(runtime_path),
            "recipe": production,
        })
    load_policy = policy["load"]
    return {
        "schema_version": 1,
        "status": "T21_COMPOSITION_EXPANSION_READY",
        "family": policy["selected_family"]["id"],
        "counts": {
            "source_facts": len(rows),
            "authored_rules": load_policy["authored_rules"],
            "datapack_files": len(rows),
            "logical_rows": len(rows),
            "eager_rows": len(rows),
            "lazy_rows": 0,
        },
        "publication_delta": {
            "logical": load_policy["logical_delta"],
            "eager": load_policy["eager_delta"],
            "lazy": load_policy["lazy_delta"],
        },
        "rows": rows,
    }


def build_axis(
    policy: dict[str, Any],
    expansion: dict[str, Any],
) -> dict[str, Any]:
    rows, summary = build_classification(policy)
    selected = {
        row["material"]: row
        for row in rows
        if row["material"] in {"coal", "charcoal"}
    }
    if (
        set(selected) != {"coal", "charcoal"}
        or any(
            row["classification"] != "composition_generated"
            or row["implementation"] != "SELECTED_T21"
            for row in selected.values()
        )
    ):
        raise ValueError("T21 selected family is not classified as selected")
    for consumer in policy["selected_family"]["consumers"]:
        document = load(ROOT / consumer)
        ingredient_ids = {
            value.get("item")
            for value in (document.get("key") or {}).values()
            if isinstance(value, dict)
        }
        if "cruciblecraft:carbon/dust" not in ingredient_ids:
            raise ValueError(
                f"T21 carbon consumer does not consume carbon dust: {consumer}"
            )
    return {
        "schema_version": 1,
        "status": "T21_CHEMICAL_CALIBRATION_READY",
        "source_revision": REVISION,
        "denominator": {
            "kind": policy["denominator"]["kind"],
            "candidates": summary["candidate_count"],
            "stale_prose_value": 234,
            "correction": policy["denominator"]["correction"],
        },
        "counts": {
            "classifications": summary["classifications"],
            "referenced_recipe_maps": summary[
                "referenced_recipe_maps"
            ],
            "unclassified": summary["unclassified"],
        },
        "byproduct_only_debt": summary["byproduct_only_debt"],
        "recipe_maps": summary["recipe_maps"],
        "candidates": rows,
        "selected_family": {
            "id": policy["selected_family"]["id"],
            "members": ["charcoal", "coal"],
            "map": policy["selected_family"]["map"],
            "machine_kind": policy["selected_family"]["machine_kind"],
            "energy_identity": policy["selected_family"][
                "energy_identity"
            ],
            "input_acquisition": policy["selected_family"]["acquisition"],
            "consumer_count": len(
                policy["selected_family"]["consumers"]
            ),
            "expected_equals_expansion": True,
            "expansion_equals_runtime": True,
        },
        "load": {
            **expansion["counts"],
            "publication_delta": expansion["publication_delta"],
        },
        "inputs": {
            relative(POLICY): sha256(POLICY),
            relative(T5_READINESS): sha256(T5_READINESS),
            relative(T5_MANIFEST): sha256(T5_MANIFEST),
            relative(EXPECTED_OUTPUT): sha256(EXPECTED_OUTPUT),
        },
    }


def semantic_axis(document: dict[str, Any]) -> dict[str, Any]:
    result = copy.deepcopy(document)
    result.pop("inputs", None)
    return result


def verify_metadata_rebase(
    committed: dict[str, Any],
    candidate: dict[str, Any],
) -> list[str]:
    errors: list[str] = []
    if semantic_axis(committed) != semantic_axis(candidate):
        errors.append(
            "T21 chemical axis metadata rebase changed semantic fields"
        )
    return errors


def check_outputs(
    axis: dict[str, Any],
    expansion: dict[str, Any],
    expected: dict[str, Any],
    replay_expected: dict[str, Any] | None,
) -> list[str]:
    from tools import currentness

    errors = []
    errors.extend(currentness.check_rebuilt(AXIS_OUTPUT, axis))
    documents = (
        (EXPANSION_OUTPUT, expansion),
    )
    for path, document in documents:
        if (
            not path.is_file()
            or path.read_text(encoding="utf-8") != stable(document)
        ):
            errors.append(relative(path))
    if not EXPECTED_OUTPUT.is_file():
        errors.append(relative(EXPECTED_OUTPUT))
    elif load(EXPECTED_OUTPUT) != expected:
        errors.append(relative(EXPECTED_OUTPUT))
    if replay_expected is not None and replay_expected != expected:
        errors.append("full-replay:" + relative(EXPECTED_OUTPUT))
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args()
    if args.write and not args.full_replay:
        parser.error("--write requires --full-replay")
    try:
        policy = load(POLICY)
        validate_policy(policy)
        replay_expected = (
            build_expected_full_replay(policy)
            if args.full_replay
            else None
        )
        if args.write:
            expected = replay_expected
            assert expected is not None
            EXPECTED_OUTPUT.write_text(
                stable(expected), encoding="utf-8", newline="\n"
            )
        else:
            if not EXPECTED_OUTPUT.is_file():
                raise ValueError("T21 compact expected artifact is missing")
            expected = load(EXPECTED_OUTPUT)
            validate_compact_expected(policy, expected)
        expansion = build_expansion(policy, expected)
        axis = build_axis(policy, expansion)
        if args.write:
            EXPANSION_OUTPUT.write_text(
                stable(expansion), encoding="utf-8", newline="\n"
            )
            AXIS_OUTPUT.write_text(
                stable(axis), encoding="utf-8", newline="\n"
            )
            print(
                "Wrote T21 chemical axis and carbon composition evidence."
            )
            return 0
        errors = check_outputs(
            axis, expansion, expected, replay_expected
        )
        if errors:
            print(
                "T21 chemical axis is stale:\n"
                + "\n".join(f"- {error}" for error in errors)
            )
            return 1
        print("T21 chemical axis and composition family are current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T21 chemical axis build failed: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
