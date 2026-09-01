#!/usr/bin/env python3
"""Classify and project the complete pinned GT6 distillery map for T5.5."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from types import ModuleType
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
TOOLS = ROOT / "tools"
SOURCE_MAP = (
    ROOT
    / "gt6_dump/gt6_recipe_dump/maps/gt.recipe.distillery.json"
)
LEDGER = TOOLS / "t5_distillery_projection.json"
T13_RECIPE_MAPS = TOOLS / "t13_denominators/recipe_maps.json"
T5_FLUID_GATE = (
    ROOT
    / "src/chemical_recipe_generated/resources/data/cruciblecraft/"
    "chemical_fluid_gate.json"
)
RECIPE_ROOT = (
    ROOT
    / "src/chemical_recipe_generated/resources/data/cruciblecraft/recipe/chemical"
)
DISTILLERY_ROOT = RECIPE_ROOT / "distillery"

GT6_REPOSITORY = "GregTech6/gregtech6"
GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
SOURCE_MAP_ID = "gt.recipe.distillery"
SOURCE_SHAPE_LIMITS = (1, 2, 1, 2)
HOST_SHAPE_LIMITS = (2, 2, 2, 3)
CLASSIFICATIONS = (
    "projectable",
    "missing_identity",
    "out_of_scope",
)
BUILTIN_FLUID_ROOTS = {
    "minecraft:lava",
    "minecraft:water",
    "cruciblecraft:creosote",
    "cruciblecraft:steam",
}
_SAFE_PATH = re.compile(r"[^a-z0-9_./-]+")
_IDENTITY_FAILURES = {
    "chemical_fluid_state",
    "external_item",
    "fluid_mapping",
    "fluid_material",
    "item_mapping",
    "item_meta",
    "item_registration",
    "prefix_registration",
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(document: Any) -> str:
    return json.dumps(
        document,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def canonical_hash(document: Any) -> str:
    payload = json.dumps(
        document,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _source_projection_module() -> ModuleType:
    try:
        from tools import build_t5_source_projection as projection
    except ModuleNotFoundError:
        import build_t5_source_projection as projection
    return projection


def _base_recipe_documents(
    base_planned: dict[Path, bytes],
) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for path, content in sorted(base_planned.items()):
        if path.parent == DISTILLERY_ROOT or path.suffix != ".json":
            continue
        try:
            relative = path.relative_to(RECIPE_ROOT)
        except ValueError:
            continue
        recipe_id = (
            "cruciblecraft:chemical/"
            + relative.with_suffix("").as_posix()
        )
        result[recipe_id] = json.loads(content.decode("utf-8"))
    return result


def _network_inventory(
    base_recipes: dict[str, dict[str, Any]],
) -> dict[str, dict[str, list[str]]]:
    fluid_producers: dict[str, list[str]] = defaultdict(list)
    fluid_consumers: dict[str, list[str]] = defaultdict(list)
    item_producers: dict[str, list[str]] = defaultdict(list)
    item_consumers: dict[str, list[str]] = defaultdict(list)
    for recipe_id, recipe in sorted(base_recipes.items()):
        for stack in recipe.get("fluid_inputs", []):
            fluid_consumers[stack["id"]].append(recipe_id)
        for stack in recipe.get("fluid_outputs", []):
            fluid_producers[stack["id"]].append(recipe_id)
        for stack in recipe.get("item_inputs", []):
            identity = stack.get("item") or stack.get("tag")
            if identity:
                item_consumers[identity].append(recipe_id)
        for stack in recipe.get("item_outputs", []):
            item_producers[stack["id"]].append(recipe_id)
    return {
        "fluid_consumers": dict(fluid_consumers),
        "fluid_producers": dict(fluid_producers),
        "item_consumers": dict(item_consumers),
        "item_producers": dict(item_producers),
    }


def _registered_fluid_ids(
    fluid_gate: dict[str, Any],
    materials: dict[str, dict[str, Any]],
) -> set[str]:
    registered = set(BUILTIN_FLUID_ROOTS)
    registered.update(
        f"cruciblecraft:{row['id']}"
        for row in fluid_gate["fluids"]
    )
    registered.update(
        f"cruciblecraft:molten_{material_id}"
        for material_id, document in materials.items()
        if document.get("molten_fluid")
    )
    return registered


def _source_shape(recipe: dict[str, Any]) -> list[int]:
    item_inputs = [
        stack
        for stack in recipe.get("inputs") or []
        if not (
            isinstance(stack, dict)
            and stack.get("item") == "gregapi:gt.integrated_circuit"
            and stack.get("count") == 0
        )
    ]
    return [
        len(item_inputs),
        len(recipe.get("outputs") or []),
        len(recipe.get("fluidInputs") or []),
        len(recipe.get("fluidOutputs") or []),
    ]


def _projected_shape(recipe: dict[str, Any]) -> list[int]:
    return [
        len(recipe.get("item_inputs", [])),
        len(recipe.get("item_outputs", [])),
        len(recipe.get("fluid_inputs", [])),
        len(recipe.get("fluid_outputs", [])),
    ]


def _recipe_slug(recipe: dict[str, Any], index: int) -> str:
    inputs = [
        stack["id"].split(":", 1)[-1].replace("/", "_")
        for key in ("fluid_inputs", "item_inputs")
        for stack in recipe.get(key, [])
        if "id" in stack
    ]
    outputs = [
        stack["id"].split(":", 1)[-1].replace("/", "_")
        for key in ("fluid_outputs", "item_outputs")
        for stack in recipe.get(key, [])
        if "id" in stack
    ]
    slug = "_and_".join(inputs) + "_to_" + "_and_".join(outputs)
    slug = _SAFE_PATH.sub("_", slug.strip("_"))
    return slug or f"source_{index}"


def _closure_evidence(
    projected: dict[str, Any],
    inventory: dict[str, dict[str, list[str]]],
) -> tuple[dict[str, Any], list[str]]:
    missing: list[str] = []
    fluid_inputs: dict[str, list[str]] = {}
    fluid_outputs: dict[str, list[str]] = {}
    item_inputs: dict[str, list[str]] = {}
    item_outputs: dict[str, list[str]] = {}

    for stack in projected.get("fluid_inputs", []):
        identity = stack["id"]
        producers = (
            ["builtin_fluid_root"]
            if identity in BUILTIN_FLUID_ROOTS
            else sorted(inventory["fluid_producers"].get(identity, []))
        )
        fluid_inputs[identity] = producers
        if not producers:
            missing.append(f"fluid_input_without_producer:{identity}")
    for stack in projected.get("fluid_outputs", []):
        identity = stack["id"]
        consumers = sorted(
            inventory["fluid_consumers"].get(identity, [])
        )
        fluid_outputs[identity] = consumers
        if identity not in BUILTIN_FLUID_ROOTS and not consumers:
            missing.append(f"fluid_output_without_consumer:{identity}")
    for stack in projected.get("item_inputs", []):
        identity = stack.get("item") or stack.get("tag")
        if identity is None:
            continue
        producers = (
            ["minecraft_item_root"]
            if identity.startswith("minecraft:")
            else sorted(inventory["item_producers"].get(identity, []))
        )
        item_inputs[identity] = producers
        if not producers:
            missing.append(f"item_input_without_producer:{identity}")
    for stack in projected.get("item_outputs", []):
        identity = stack["id"]
        consumers = sorted(
            inventory["item_consumers"].get(identity, [])
        )
        item_outputs[identity] = consumers
        if not identity.startswith("minecraft:") and not consumers:
            missing.append(f"item_output_without_consumer:{identity}")

    return {
        "fluid_input_producers": fluid_inputs,
        "fluid_output_consumers": fluid_outputs,
        "item_input_producers": item_inputs,
        "item_output_consumers": item_outputs,
    }, sorted(missing)


def plan(
    projection: ModuleType,
    base_planned: dict[Path, bytes],
    fluid_gate: dict[str, Any],
) -> tuple[dict[str, Any], dict[Path, bytes]]:
    materials = projection.material_documents()
    prefixes = projection.prefix_documents()
    source_by_id = projection.source_material_index(materials)
    registration = load(projection.REGISTRATION_GATE)["materials"]
    cross_reference = load(projection.CROSS_REFERENCE)
    fixed_item_document = load(projection.FIXED_ITEM_PROJECTION)
    fixed_items = {
        (row["source"]["item"], int(row["source"]["meta"])): row["cc_item"]
        for row in fixed_item_document["mappings"]
    }
    normalized_by_fluid = {
        row["fluid"]: row
        for row in load(projection.NORMALIZED_FLUIDS)["records"]
    }
    source_document = load(SOURCE_MAP)
    source_recipes = source_document["recipes"]
    if (
        source_document.get("recipeCount") != 1_517
        or len(source_recipes) != 1_517
    ):
        raise ValueError("pinned distillery source must contain 1517 rows")

    base_recipes = _base_recipe_documents(base_planned)
    inventory = _network_inventory(base_recipes)
    registered_fluids = _registered_fluid_ids(fluid_gate, materials)
    rows: list[dict[str, Any]] = []
    generated: list[dict[str, Any]] = []
    planned: dict[Path, bytes] = {}
    counts: Counter[str] = Counter()
    reason_counts: Counter[str] = Counter()
    used_slugs: set[str] = set()
    source_path = SOURCE_MAP.relative_to(ROOT).as_posix()

    for index, source_recipe in enumerate(source_recipes):
        shape = _source_shape(source_recipe)
        base_row: dict[str, Any] = {
            "recipe_index": index,
            "source": {
                "path": f"{source_path}#recipes[{index}]",
                "row_sha256": canonical_hash(source_recipe),
            },
            "source_shape": {
                "actual": shape,
                "host_limits": list(HOST_SHAPE_LIMITS),
                "source_map_limits": list(SOURCE_SHAPE_LIMITS),
            },
        }
        try:
            projected, source_projection = projection.project_recipe(
                source_recipe,
                None,
                source_by_id=source_by_id,
                gt_prefix_by_item=cross_reference[
                    "prefix_item_to_gt_prefix"
                ],
                cc_prefix_by_gt=cross_reference["gt_prefix_to_cc"],
                registration=registration,
                prefixes=prefixes,
                fixed_items=fixed_items,
                normalized_by_fluid=normalized_by_fluid,
                materials=materials,
                map_id=SOURCE_MAP_ID,
            )
        except ValueError as error:
            reason = str(error)
            classification = (
                "missing_identity"
                if reason in _IDENTITY_FAILURES
                else "out_of_scope"
            )
            reason_code = (
                f"projection_{reason}"
                if classification == "missing_identity"
                else f"unsupported_{reason}"
            )
            rows.append({
                **base_row,
                "classification": classification,
                "reason_code": reason_code,
                "reason": (
                    "The pinned row cannot resolve every source operand to a "
                    "currently registered CrucibleCraft, Minecraft, or tagged "
                    "material identity."
                    if classification == "missing_identity"
                    else
                    "The pinned row is outside the current distillery host, "
                    "energy, or active-recipe envelope."
                ),
            })
            counts[classification] += 1
            reason_counts[reason_code] += 1
            continue

        projected_shape = _projected_shape(projected)
        if any(
            actual > limit
            for actual, limit in zip(
                projected_shape,
                HOST_SHAPE_LIMITS,
                strict=True,
            )
        ):
            reason_code = "host_shape_unsupported"
            rows.append({
                **base_row,
                "classification": "out_of_scope",
                "projected": projected,
                "projected_shape": projected_shape,
                "reason_code": reason_code,
                "reason": (
                    "The translated active row exceeds the registered "
                    "distillery item/fluid input/output slot envelope."
                ),
            })
            counts["out_of_scope"] += 1
            reason_counts[reason_code] += 1
            continue

        projected_fluids = {
            stack["id"]
            for key in ("fluid_inputs", "fluid_outputs")
            for stack in projected.get(key, [])
        }
        missing_fluids = sorted(projected_fluids - registered_fluids)
        if missing_fluids:
            reason_code = "runtime_fluid_identity_unregistered"
            rows.append({
                **base_row,
                "classification": "missing_identity",
                "missing_runtime_identities": missing_fluids,
                "projected": projected,
                "reason_code": reason_code,
                "reason": (
                    "The source operands normalize to material fluids, but at "
                    "least one projected fluid is absent from the current T5 "
                    "chemical fluid gate and registered molten-fluid domain."
                ),
            })
            counts["missing_identity"] += 1
            reason_counts[reason_code] += 1
            continue

        closure, closure_missing = _closure_evidence(
            projected,
            inventory,
        )
        if closure_missing:
            reason_code = "internal_closure_unreachable"
            rows.append({
                **base_row,
                "classification": "out_of_scope",
                "closure": closure,
                "closure_missing": closure_missing,
                "projected": projected,
                "reason_code": reason_code,
                "reason": (
                    "Every runtime identity exists, but the current internal "
                    "T5 producer/consumer graph cannot reach and consume this "
                    "row without adding an unproven domain expansion."
                ),
            })
            counts["out_of_scope"] += 1
            reason_counts[reason_code] += 1
            continue

        slug = _recipe_slug(projected, index)
        if slug in used_slugs:
            slug = f"{slug}_source_{index}"
        used_slugs.add(slug)
        recipe_id = f"cruciblecraft:chemical/distillery/{slug}"
        projected["provenance"]["selected_source_recipe"] = (
            f"{source_path}#recipes[{index}]"
        )
        output = DISTILLERY_ROOT / f"{slug}.json"
        planned[output] = stable(projected).encode("utf-8")
        generated.append({
            "id": recipe_id,
            "recipe_index": index,
            "source_path": source_path,
            "source_row_sha256": canonical_hash(source_recipe),
        })
        reason_code = "registered_and_internally_reachable"
        rows.append({
            **base_row,
            "classification": "projectable",
            "closure": closure,
            "generated_recipe_id": recipe_id,
            "projected": projected,
            "reason_code": reason_code,
            "reason": (
                "All source operands map to current runtime identities, the "
                "row fits the registered distillery host, and every non-root "
                "fluid/item edge closes against the existing T5 network."
            ),
            "source_projection": source_projection,
        })
        counts["projectable"] += 1
        reason_counts[reason_code] += 1

    classified = sum(counts.values())
    unclassified = len(source_recipes) - classified
    if unclassified:
        raise ValueError(
            f"distillery ledger has {unclassified} unclassified rows"
        )
    if len(generated) != counts["projectable"]:
        raise ValueError("every projectable distillery row must be generated")

    ledger = {
        "schema_version": 1,
        "status": "BOUNDED_SUBSET_PROJECTED",
        "classification_definitions": {
            "missing_identity": (
                "At least one source operand has no factual registered "
                "runtime identity, or its normalized fluid identity is absent "
                "from the current T5 fluid gate."
            ),
            "out_of_scope": (
                "All required identities resolve, but the active row does not "
                "fit the registered distillery envelope or cannot close against "
                "the existing internal producer/consumer graph."
            ),
            "projectable": (
                "All identities are currently registered, the active source "
                "shape fits the host, and all non-root inputs/outputs are "
                "internally reachable; every such row is generated."
            ),
        },
        "selection": {
            "host_shape_limits": list(HOST_SHAPE_LIMITS),
            "rule": (
                "Exhaustively project all 1517 pinned rows; accept a row only "
                "when current identities resolve, the registered host accepts "
                "its shape/energy, and the existing T5 graph supplies every "
                "non-root input and consumes every non-root output."
            ),
            "source_map_shape_limits": list(SOURCE_SHAPE_LIMITS),
        },
        "source": {
            "builder_sha256": digest(Path(__file__).resolve()),
            "map": SOURCE_MAP_ID,
            "path": source_path,
            "recipe_count": len(source_recipes),
            "repository": GT6_REPOSITORY,
            "revision": GT6_REVISION,
            "sha256": digest(SOURCE_MAP),
        },
        "inputs": {
            projection.REGISTRATION_GATE.relative_to(ROOT).as_posix(): digest(
                projection.REGISTRATION_GATE
            ),
            projection.NORMALIZED_FLUIDS.relative_to(ROOT).as_posix(): digest(
                projection.NORMALIZED_FLUIDS
            ),
            projection.FIXED_ITEM_PROJECTION.relative_to(ROOT).as_posix(): digest(
                projection.FIXED_ITEM_PROJECTION
            ),
            projection.FLUID_GATE.relative_to(ROOT).as_posix(): canonical_hash(
                fluid_gate
            ),
        },
        "counts": {
            "classified": classified,
            "generated_recipes": len(generated),
            "reason_codes": dict(sorted(reason_counts.items())),
            **{
                classification: counts.get(classification, 0)
                for classification in CLASSIFICATIONS
            },
            "source_rows": len(source_recipes),
            "unclassified": unclassified,
        },
        "generated": generated,
        "rows": rows,
    }
    planned[LEDGER] = stable(ledger).encode("utf-8")
    return ledger, planned


def build(
    write: bool = True,
    planned_files: dict[Path, bytes] | None = None,
) -> dict[str, Any]:
    projection = _source_projection_module()
    base_planned: dict[Path, bytes] = {}
    projection.build(
        write=False,
        planned_files=base_planned,
        include_distillery=False,
    )
    fluid_gate = json.loads(
        base_planned[projection.FLUID_GATE].decode("utf-8")
    )
    ledger, planned = plan(projection, base_planned, fluid_gate)
    if planned_files is not None:
        planned_files.update(planned)
    if write:
        if DISTILLERY_ROOT.exists():
            for path in sorted(DISTILLERY_ROOT.glob("*.json")):
                path.unlink()
        for path, content in sorted(planned.items()):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
    return ledger


def check() -> list[str]:
    planned: dict[Path, bytes] = {}
    build(write=False, planned_files=planned)
    actual = (
        set(DISTILLERY_ROOT.glob("*.json"))
        if DISTILLERY_ROOT.exists()
        else set()
    )
    if LEDGER.is_file():
        actual.add(LEDGER)
    errors = [
        f"missing generated file: {path.relative_to(ROOT)}"
        for path in sorted(set(planned) - actual)
    ]
    errors.extend(
        f"extra generated file: {path.relative_to(ROOT)}"
        for path in sorted(actual - set(planned))
    )
    errors.extend(
        f"stale generated file: {path.relative_to(ROOT)}"
        for path in sorted(actual & set(planned))
        if path.read_bytes() != planned[path]
    )
    return errors


def reference_only_check() -> list[str]:
    errors: list[str] = []
    if not LEDGER.is_file():
        return [f"missing generated file: {LEDGER.relative_to(ROOT)}"]
    try:
        ledger = load(LEDGER)
        if LEDGER.read_bytes() != stable(ledger).encode("utf-8"):
            errors.append("distillery ledger is not canonical JSON")
        t13 = load(T13_RECIPE_MAPS)
        map_row = next(
            row
            for row in t13["rows"]
            if row["name_internal"] == SOURCE_MAP_ID
        )
        from tools import currentness

        errors.extend(currentness.check_sidecar(LEDGER))
        source = ledger.get("source") or {}
        if (
            source.get("sha256") != map_row["source_blob"]
            or source.get("recipe_count") != map_row["recipe_count"]
        ):
            errors.append("distillery source receipt drifted")
        rows = ledger.get("rows") or []
        generated = ledger.get("generated") or []
        counts = ledger.get("counts") or {}
        classifications = Counter(
            row.get("classification") for row in rows
        )
        if (
            [row.get("recipe_index") for row in rows]
            != list(range(len(rows)))
            or counts.get("source_rows") != len(rows)
            or counts.get("classified") != len(rows)
            or counts.get("unclassified") != 0
            or counts.get("generated_recipes") != len(generated)
            or any(
                counts.get(name) != classifications[name]
                for name in CLASSIFICATIONS
            )
        ):
            errors.append("distillery compact counts are inconsistent")
        expected_files = {
            DISTILLERY_ROOT / f"{entry['id'].rsplit('/', 1)[-1]}.json"
            for entry in generated
        }
        actual_files = set(DISTILLERY_ROOT.glob("*.json"))
        if actual_files != expected_files:
            errors.append("distillery generated file set drifted")
        for entry in generated:
            path = DISTILLERY_ROOT / f"{entry['id'].rsplit('/', 1)[-1]}.json"
            document = load(path)
            if path.read_bytes() != stable(document).encode("utf-8"):
                errors.append(f"non-canonical generated file: {path}")
            selected = (
                document.get("provenance") or {}
            ).get("selected_source_recipe")
            if selected != (
                f"{entry['source_path']}#recipes[{entry['recipe_index']}]"
            ):
                errors.append(f"distillery provenance drifted: {path.name}")
    except (
        KeyError,
        OSError,
        StopIteration,
        TypeError,
        ValueError,
        json.JSONDecodeError,
    ) as exc:
        errors.append(str(exc))
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if committed distillery artifacts are stale",
    )
    replay_mode = parser.add_mutually_exclusive_group()
    replay_mode.add_argument("--reference-only", action="store_true")
    replay_mode.add_argument("--full-replay", action="store_true")
    args = parser.parse_args()
    if (args.reference_only or args.full_replay) and not args.check:
        parser.error("--reference-only and --full-replay require --check")
    if args.check:
        errors = reference_only_check() if args.reference_only else check()
        if errors:
            print("T5.5 distillery projection is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T5.5 distillery projection matches committed artifacts.")
        return 0
    document = build()
    print(
        "T5.5 distillery projection: "
        f"{document['counts']['projectable']} projectable, "
        f"{document['counts']['missing_identity']} missing identity, "
        f"{document['counts']['out_of_scope']} out of scope"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
