#!/usr/bin/env python3
"""Build the template-based T21 denominator and Beta reverse closure."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from collections import Counter, defaultdict, deque
from pathlib import Path
from typing import Any

try:
    from tools import build_t5_source_projection as source_projection
    from tools import gt6_mixer_templates as mixer_templates
except ModuleNotFoundError:
    import build_t5_source_projection as source_projection
    import gt6_mixer_templates as mixer_templates


ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t21_template_denominator_policy.json"
OUTPUT = TOOLS / "t21_template_denominator.json"
T5_MANIFEST = TOOLS / "t5_chemical_recipe_manifest.json"
ROW_DIAGNOSTIC = TOOLS / "t21_source_denominator.json"
REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
MAP_ID = "gt.recipe.mixer"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def compact(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ) + "\n"


def content_hash(value: Any) -> str:
    return hashlib.sha256(
        json.dumps(
            value,
            ensure_ascii=False,
            separators=(",", ":"),
            sort_keys=True,
        ).encode("utf-8")
    ).hexdigest()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_policy(policy: dict[str, Any]) -> None:
    schema = policy.get("schema_version")
    if (
        schema not in {1, 2}
        or policy.get("status") != "T21_TEMPLATE_DENOMINATOR_POLICY"
        or policy.get("source_revision") != REVISION
        or policy.get("row_diagnostic", {}).get("closure_numerator")
        is not False
    ):
        raise ValueError("T21 template denominator policy drifted")
    classes = policy.get("classifications")
    if (
        not isinstance(classes, list)
        or len(classes) != len(set(classes))
        or "v1_required" not in classes
        or "unclassified" in classes
    ):
        raise ValueError("T21 template classifications drifted")
    # v2: both consumer_proof and operand_proof are mandatory on v1_required units
    if schema >= 2:
        forced = policy.get("forced_v1_templates") or []
        for unit in forced:
            if not isinstance(unit, dict):
                raise ValueError("forced_v1_templates entry is not a dict")
            consumer = str(unit.get("consumer_proof") or "").strip()
            operand = str(unit.get("operand_proof") or "").strip()
            if not consumer:
                raise ValueError(
                    f"v1_required unit {unit.get('template_id', '?')} "
                    f"missing consumer_proof"
                )
            if not operand:
                raise ValueError(
                    f"v1_required unit {unit.get('template_id', '?')} "
                    f"missing operand_proof"
                )
    # Validate partial_coverage_policy includes reachability for v2+
    coverage_policy = str(policy.get("partial_coverage_policy") or "")
    if schema >= 2 and "reachab" not in coverage_policy.lower():
        raise ValueError(
            "partial_coverage_policy must mention reachability for schema v2+"
        )


def source_context() -> dict[str, Any]:
    materials = source_projection.material_documents()
    source_by_id = source_projection.source_material_index(materials)
    cross = load(source_projection.CROSS_REFERENCE)
    normalized_by_fluid = {
        row["fluid"]: row
        for row in load(source_projection.NORMALIZED_FLUIDS)["records"]
    }
    return {
        "materials": materials,
        "source_by_id": source_by_id,
        "gt_prefix_by_item": cross["prefix_item_to_gt_prefix"],
        "normalized_by_fluid": normalized_by_fluid,
    }


def item_identity(
    stack: dict[str, Any],
    context: dict[str, Any],
) -> str | None:
    item = stack.get("item")
    meta = stack.get("meta")
    if (
        item in context["gt_prefix_by_item"]
        and isinstance(meta, int)
        and not isinstance(meta, bool)
    ):
        material = context["source_by_id"].get(meta)
        if material is not None:
            return f"material:{material}"
    if isinstance(item, str) and item:
        return f"item:{item}:{meta if isinstance(meta, int) else 0}"
    return None


def fluid_identity(
    stack: dict[str, Any],
    context: dict[str, Any],
) -> str | None:
    fluid = stack.get("fluid")
    if not isinstance(fluid, str) or not fluid:
        return None
    normalized = context["normalized_by_fluid"].get(fluid)
    if normalized is not None:
        material = context["source_by_id"].get(
            int(normalized["material_id"])
        )
        if material is not None:
            return f"material:{material}"
    return f"fluid:{fluid}"


def recipe_io(
    recipe: dict[str, Any],
    context: dict[str, Any],
) -> tuple[set[str], set[str]]:
    inputs = {
        identity
        for stack in recipe.get("inputs") or []
        if isinstance(stack, dict)
        for identity in [item_identity(stack, context)]
        if identity is not None
    }
    outputs = {
        identity
        for stack in recipe.get("outputs") or []
        if isinstance(stack, dict)
        for identity in [item_identity(stack, context)]
        if identity is not None
    }
    inputs.update(
        identity
        for stack in recipe.get("fluidInputs") or []
        if isinstance(stack, dict)
        for identity in [fluid_identity(stack, context)]
        if identity is not None
    )
    outputs.update(
        identity
        for stack in recipe.get("fluidOutputs") or []
        if isinstance(stack, dict)
        for identity in [fluid_identity(stack, context)]
        if identity is not None
    )
    return inputs, outputs


def current_t5_coverage() -> set[tuple[str, int]]:
    covered = set()
    for row in load(T5_MANIFEST)["generated"]:
        source = row.get("source") or {}
        map_id = source.get("map")
        index = source.get("recipe_index")
        if (
            map_id in source_projection.TARGET_MAPS
            and isinstance(index, int)
        ):
            covered.add((map_id, index))
    return covered


def current_t5_output_seeds() -> set[str]:
    seeds = set()
    for row in load(T5_MANIFEST)["generated"]:
        outputs = row.get("source_outputs") or {}
        for item in outputs.get("items") or []:
            material = item.get("material")
            if isinstance(material, str):
                seeds.add(f"material:{material}")
        for fluid in outputs.get("fluids") or []:
            material = fluid.get("material")
            source_fluid = fluid.get("source_fluid")
            if isinstance(material, str):
                seeds.add(f"material:{material}")
            elif isinstance(source_fluid, str):
                seeds.add(f"fluid:{source_fluid}")
    return seeds


def build() -> dict[str, Any]:
    policy = load(POLICY)
    validate_policy(policy)
    diagnostic = load(ROW_DIAGNOSTIC)
    if diagnostic.get("status") != "T21_SOURCE_DENOMINATOR_READY":
        raise ValueError("T21 row diagnostic is not current")
    mixer_index = load(mixer_templates.INDEX_OUTPUT)
    mixer_membership = load(mixer_templates.MEMBERSHIP_OUTPUT)
    mixer_report = load(mixer_templates.REPORT_OUTPUT)
    mixer_templates.validate_compact(
        mixer_index, mixer_membership, mixer_report
    )
    context = source_context()
    covered = current_t5_coverage()
    map_ids = list(source_projection.TARGET_MAPS)
    rows: list[dict[str, Any]] = []
    output_index: dict[str, list[int]] = defaultdict(list)
    petroleum = set(policy["petroleum_materials"])
    g10_tokens = tuple(policy["g10_fluid_tokens"])
    for map_index, map_id in enumerate(map_ids):
        document = load(source_projection.MAP_ROOT / f"{map_id}.json")
        for recipe_index, recipe in enumerate(document["recipes"]):
            inputs, outputs = recipe_io(recipe, context)
            material_ids = {
                identity.removeprefix("material:")
                for identity in inputs | outputs
                if identity.startswith("material:")
            }
            inactive = (
                recipe.get("enabled") is False
                or recipe.get("hidden") is True
                or recipe.get("fake") is True
            )
            petroleum_row = bool(material_ids & petroleum)
            nuclear = any(
                "ATOMIC.ACTINIDE"
                in set(
                    (context["materials"].get(material) or {})
                    .get("gt6_metadata", {})
                    .get("material_tags", [])
                )
                for material in material_ids
            ) or any("plasma" in identity for identity in inputs | outputs)
            g10 = any(
                token in identity.lower()
                for identity in inputs | outputs
                for token in g10_tokens
            )
            row = {
                "map_index": map_index,
                "recipe_index": recipe_index,
                "inputs": inputs,
                "outputs": outputs,
                "inactive": inactive,
                "petroleum": petroleum_row,
                "nuclear": nuclear,
                "g10": g10,
            }
            row_id = len(rows)
            rows.append(row)
            if not (inactive or petroleum_row or nuclear or g10):
                for output in outputs:
                    output_index[output].append(row_id)

    seeds = {
        f"material:{material}"
        for material in policy["beta_seed_materials"]
    }
    if policy["reverse_closure"]["include_current_t5_outputs_as_seeds"]:
        seeds.update(current_t5_output_seeds())
    available = {
        f"material:{material}"
        for material in policy["currently_available_materials"]
    }
    mixer_map_index = map_ids.index(MAP_ID)
    mixer_row_offset = sum(
        len(load(
            source_projection.MAP_ROOT / f"{prior}.json"
        )["recipes"])
        for prior in map_ids[:mixer_map_index]
    )
    templates_by_id = {
        row["template_id"]: row for row in mixer_index["templates"]
    }
    required_rows: set[int] = set()
    for forced in policy["forced_v1_templates"]:
        template = templates_by_id.get(forced["template_id"])
        if (
            template is None
            or template["source_count"] != forced["expected_rows"]
        ):
            raise ValueError(
                f"forced v1 Mixer template drifted: {forced['template_id']}"
            )
        required_rows.update(
            mixer_row_offset + support[0]
            for support in template["supports"]
        )
    needed = set(seeds)
    for row_id in required_rows:
        needed.update(rows[row_id]["outputs"])
    queue = deque(sorted({
        input_node
        for row_id in required_rows
        for input_node in rows[row_id]["inputs"]
        if input_node not in available
        and not input_node.startswith("item:minecraft:")
    }))
    while queue:
        node = queue.popleft()
        candidates = [
            row_id for row_id in output_index.get(node, [])
            if not (
                rows[row_id]["inactive"]
                or rows[row_id]["petroleum"]
                or rows[row_id]["nuclear"]
                or rows[row_id]["g10"]
            )
        ]
        if not candidates:
            raise ValueError(
                f"v1 Mixer template input lacks an available producer: {node}"
            )
        selected = min(
            candidates,
            key=lambda row_id: (
                (map_ids[rows[row_id]["map_index"]],
                 rows[row_id]["recipe_index"]) not in covered,
                rows[row_id]["map_index"],
                rows[row_id]["recipe_index"],
            ),
        )
        if selected in required_rows:
            continue
        required_rows.add(selected)
        needed.update(rows[selected]["outputs"])
        for input_node in sorted(rows[selected]["inputs"]):
            if (
                input_node not in available
                and not input_node.startswith("item:minecraft:")
                and input_node not in needed
            ):
                needed.add(input_node)
                queue.append(input_node)

    classes = list(policy["classifications"])
    class_index = {value: index for index, value in enumerate(classes)}
    row_classification: list[str] = []
    for row_id, row in enumerate(rows):
        key = (map_ids[row["map_index"]], row["recipe_index"])
        if key in covered:
            classification = "already_covered"
        elif row["inactive"]:
            classification = "out_of_scope"
        elif row["petroleum"]:
            classification = "petroleum_t22"
        elif row["nuclear"]:
            classification = "post_1_0_nuclear"
        elif row["g10"]:
            classification = "post_1_0_g10"
        elif row_id in required_rows:
            classification = "v1_required"
        else:
            classification = "ordinary_optional"
        row_classification.append(classification)

    units: list[list[Any]] = []
    split_templates = 0
    mixer_groups: dict[tuple[int, str], list[int]] = defaultdict(list)
    for recipe_index, template_index in enumerate(
        mixer_membership["membership"]
    ):
        absolute_row = sum(
            summary["source_rows"]
            for summary in diagnostic["map_summaries"]
            if map_ids.index(summary["map"]) < mixer_map_index
        ) + recipe_index
        # Rows were appended map-by-map in the exact TARGET_MAPS order.
        classification = row_classification[absolute_row]
        mixer_groups[(template_index, classification)].append(recipe_index)
    template_class_counts: Counter[int] = Counter()
    for template_index, _classification in mixer_groups:
        template_class_counts[template_index] += 1
    split_templates = sum(value > 1 for value in template_class_counts.values())
    for (template_index, classification), source_indexes in sorted(
        mixer_groups.items()
    ):
        template_id = mixer_membership["template_ids"][template_index]
        unit_identity = {
            "map": MAP_ID,
            "template": template_id,
            "classification": classification,
            "source_indexes_sha256": content_hash(source_indexes),
        }
        evidence = sorted({
            node
            for recipe_index in source_indexes
            for node in rows[mixer_row_offset + recipe_index]["outputs"]
            if node in needed
        })
        units.append([
            f"sha256:{content_hash(unit_identity)}",
            mixer_map_index,
            template_index,
            class_index[classification],
            source_indexes,
            evidence[:16],
        ])

    row_offset = 0
    for map_index, map_id in enumerate(map_ids):
        document = load(source_projection.MAP_ROOT / f"{map_id}.json")
        count = len(document["recipes"])
        if map_id != MAP_ID:
            for recipe_index in range(count):
                classification = row_classification[row_offset + recipe_index]
                identity = {
                    "map": map_id,
                    "recipe_index": recipe_index,
                    "classification": classification,
                }
                evidence = sorted(
                    rows[row_offset + recipe_index]["outputs"] & needed
                )
                units.append([
                    f"sha256:{content_hash(identity)}",
                    map_index,
                    None,
                    class_index[classification],
                    [recipe_index],
                    evidence[:16],
                ])
        row_offset += count

    units.sort(key=lambda row: row[0])
    unit_counts = Counter(classes[row[3]] for row in units)
    expanded_counts = Counter()
    for unit in units:
        expanded_counts[classes[unit[3]]] += len(unit[4])
    v1_units = [unit for unit in units if classes[unit[3]] == "v1_required"]
    if any(not unit[5] for unit in v1_units):
        raise ValueError("v1_required template unit lacks reachability evidence")
    return {
        "schema_version": 2,
        "status": "T21_TEMPLATE_DENOMINATOR_READY",
        "supersedes": {
            "target": "tools/t21_source_denominator.json",
            "disposition": "ordinary_v1_required",
            "note": (
                "Authoritative template denominator (T21 rebaseline). "
                "The input-touch ledger-1 disposition ordinary_v1_required "
                "is superseded by this file's unit classifications; see the "
                "superseded block in t21_source_denominator.json."
            ),
        },
        "source_revision": REVISION,
        "encoding": {
            "classes": classes,
            "maps": map_ids,
            "unit": [
                "unit_id",
                "map_index",
                "mixer_template_index_or_null",
                "classification_index",
                "source_recipe_indexes",
                "reachability_evidence",
            ],
        },
        "counts": {
            "source_rows": len(rows),
            "denominator_units": len(units),
            "mixer_templates": len(mixer_index["templates"]),
            "mixer_templates_split_for_classification": split_templates,
            "unit_classifications": dict(sorted(unit_counts.items())),
            "expanded_row_diagnostics": dict(
                sorted(expanded_counts.items())
            ),
            "v1_required_units": unit_counts["v1_required"],
            "unclassified": 0,
        },
        "beta_closure": {
            "seed_count": len(seeds),
            "stable_identity_count": len(needed),
            "required_source_rows": len(required_rows),
            "seeds": sorted(seeds),
            "identity_sha256": content_hash(sorted(needed)),
        },
        "coverage": {
            "current_t5_source_rows": len(covered),
            "partially_covered_templates_split": split_templates,
            "partially_covered_units": 0,
        },
        "units": units,
        "inputs": {
            "tools/t21_template_denominator_policy.json": sha256(POLICY),
            "tools/gt6_mixer_templates_index.json": sha256(
                mixer_templates.INDEX_OUTPUT
            ),
            "tools/gt6_mixer_templates_membership.json": sha256(
                mixer_templates.MEMBERSHIP_OUTPUT
            ),
            "tools/t5_chemical_recipe_manifest.json": sha256(T5_MANIFEST),
            "tools/t21_source_denominator.json": sha256(ROW_DIAGNOSTIC),
        },
    }


def semantic_document(document: dict[str, Any]) -> dict[str, Any]:
    from tools import semantic_projection as projection

    return projection.ledger_projection(document)["semantic_body"]


def verify_metadata_rebase(
    committed: dict[str, Any],
    candidate: dict[str, Any],
) -> list[str]:
    errors: list[str] = []
    if semantic_document(committed) != semantic_document(candidate):
        errors.append(
            "T21 template denominator metadata rebase changed semantic fields"
        )
    return errors


def validate_compact(document: dict[str, Any]) -> None:
    if (
        document.get("schema_version") != 2
        or document.get("status") != "T21_TEMPLATE_DENOMINATOR_READY"
        or document.get("source_revision") != REVISION
        or document.get("counts", {}).get("source_rows") != 151_433
        or document.get("counts", {}).get("mixer_templates") != 3_414
        or document.get("counts", {}).get("unclassified") != 0
        or len(document.get("units") or [])
        != document["counts"]["denominator_units"]
    ):
        raise ValueError("T21 compact template denominator drifted")
    from tools import currentness

    errors = currentness.check_sidecar(OUTPUT)
    if errors:
        raise ValueError("; ".join(errors))


def check(document: dict[str, Any] | None = None) -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing {OUTPUT.relative_to(ROOT).as_posix()}"]
    from tools import currentness

    committed = load(OUTPUT)
    try:
        validate_compact(committed)
    except ValueError as error:
        return [str(error)]
    if document is None:
        return currentness.check_sidecar(OUTPUT)
    return currentness.check_rebuilt(OUTPUT, document)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    replay = parser.add_mutually_exclusive_group()
    replay.add_argument("--reference-only", action="store_true")
    replay.add_argument("--full-replay", action="store_true")
    args = parser.parse_args()
    if args.write and not args.full_replay:
        parser.error("--write requires --full-replay")
    if args.check and not (args.reference_only or args.full_replay):
        parser.error("--check requires --reference-only or --full-replay")
    try:
        document = build() if args.full_replay else None
        if args.write:
            assert document is not None
            OUTPUT.write_bytes(compact(document).encode("utf-8"))
            print(json.dumps(document["counts"], sort_keys=True))
            return 0
        errors = check(document)
        if errors:
            print("\n".join(errors))
            return 1
        tier = "full replay" if args.full_replay else "compact"
        print(f"T21 template denominator {tier} evidence is current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T21 template denominator build failed: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
