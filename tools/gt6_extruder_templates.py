"""Lossless, replay-verified template extraction for the GT6 extruder map.

The generic template extractor cannot infer extruder registration structure.
The concrete extruder shape is the registration-family boundary; material,
external item, form, count, EU and duration are coordinated values below it.
This extractor therefore uses:

* one template per concrete normal or low-heat shape;
* compact, template-local subject/input-form/output-form support relations;
* exact support values for all dependent fields;
* stable content-hash IDs; and
* an exact canonical-multiset replay gate.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import sys
from collections import Counter, defaultdict
from fractions import Fraction
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump"
MAP_PATH = DUMP / "maps" / "gt.recipe.extruder.json"
MATERIALS_PATH = DUMP / "oredict" / "materials.json"
OUT = ROOT / "tools" / "gt6_extruder_templates_v5.json"
SUMMARY_OUT = ROOT / "tools" / "gt6_extruder_templates_report.json"
INDEX_OUT = ROOT / "tools" / "gt6_extruder_templates_index_v5.json"

MAP_NAME = "gt.recipe.extruder"
SCHEMA_VERSION = 5
CANONICALIZATION = "gt6-recipe-semantic-v3"
MIN_DEPENDENCY_SAMPLE = 5
MIN_EXTERNAL_FAMILY = 5
SHAPE_ITEM = "gregtech:gt.multiitem.technological"
META_PREFIX = "gregtech:gt.meta."


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=False,
    )


def content_hash(value: Any) -> str:
    return hashlib.sha256(stable_json(value).encode("utf-8")).hexdigest()


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonicalize(value: Any) -> Any:
    """Drop presentation-only fields while preserving all recipe semantics."""
    if isinstance(value, dict):
        return {
            key: canonicalize(child)
            for key, child in sorted(value.items())
            if key != "displayName"
        }
    if isinstance(value, list):
        return [canonicalize(child) for child in value]
    return value


def recipe_digest(recipe: dict[str, Any]) -> str:
    return content_hash(canonicalize(recipe))


def multiset_digest(counter: Counter[str]) -> str:
    return content_hash(sorted(counter.items()))


def is_gt_meta_stack(stack: dict[str, Any]) -> bool:
    return str(stack.get("item") or "").startswith(META_PREFIX)


def is_shape_stack(stack: dict[str, Any]) -> bool:
    return (
        str(stack.get("item") or "") == SHAPE_ITEM
        and int(stack.get("count") or 0) == 0
    )


def json_pointer(parts: list[str | int]) -> str:
    escaped = [
        str(part).replace("~", "~0").replace("/", "~1")
        for part in parts
    ]
    return "/" + "/".join(escaped)


def pointer_parts(pointer: str) -> list[str]:
    if not pointer.startswith("/"):
        raise ValueError(f"invalid JSON pointer: {pointer}")
    return [
        part.replace("~1", "/").replace("~0", "~")
        for part in pointer[1:].split("/")
    ]


def set_pointer(document: Any, pointer: str, value: Any) -> None:
    parts = pointer_parts(pointer)
    current = document
    for raw in parts[:-1]:
        current = current[int(raw)] if isinstance(current, list) else current[raw]
    final = parts[-1]
    if isinstance(current, list):
        current[int(final)] = copy.deepcopy(value)
    else:
        current[final] = copy.deepcopy(value)


def marker(pointer: str) -> dict[str, str]:
    return {"$patch": pointer}


def patch_sort_key(row: dict[str, Any]) -> tuple[str, str]:
    return row["path"], stable_json(row["value"])


def apply_patches(
    skeleton: dict[str, Any],
    patches: list[dict[str, Any]],
) -> dict[str, Any]:
    replay = copy.deepcopy(skeleton)
    for patch in patches:
        set_pointer(replay, patch["path"], patch["value"])
    unresolved: list[str] = []

    def scan(value: Any, path: str = "") -> None:
        if isinstance(value, dict):
            if set(value) == {"$patch"}:
                unresolved.append(path or "/")
                return
            for key, child in value.items():
                scan(child, f"{path}/{key}")
        elif isinstance(value, list):
            for index, child in enumerate(value):
                scan(child, f"{path}/{index}")

    scan(replay)
    if unresolved:
        raise ValueError(f"unresolved template patches: {unresolved[:5]}")
    return replay


def shape_indices(recipe: dict[str, Any]) -> list[int]:
    return [
        index
        for index, stack in enumerate(recipe.get("inputs") or [])
        if stack and is_shape_stack(stack)
    ]


def non_shape_items(recipe: dict[str, Any]) -> list[tuple[str, int, dict[str, Any]]]:
    result = []
    for side in ("inputs", "outputs"):
        for index, stack in enumerate(recipe.get(side) or []):
            if not stack or is_shape_stack(stack):
                continue
            result.append((side, index, stack))
    return result


def pure_gt_material_recipe(recipe: dict[str, Any]) -> bool:
    items = non_shape_items(recipe)
    return (
        len(shape_indices(recipe)) == 1
        and bool(items)
        and all(is_gt_meta_stack(stack) for _side, _index, stack in items)
    )


def external_input_recipe(recipe: dict[str, Any]) -> bool:
    if len(shape_indices(recipe)) != 1:
        return False
    consumed = [
        stack
        for stack in recipe.get("inputs") or []
        if stack and not is_shape_stack(stack)
    ]
    return bool(consumed) and any(not is_gt_meta_stack(stack) for stack in consumed)


def replace_path(
    skeleton: dict[str, Any],
    source: dict[str, Any],
    parts: list[str | int],
    patches: list[dict[str, Any]],
) -> None:
    pointer = json_pointer(parts)
    current: Any = source
    for part in parts:
        current = current[part] if isinstance(part, str) else current[part]
    patches.append({"path": pointer, "value": copy.deepcopy(current)})
    set_pointer(skeleton, pointer, marker(pointer))


def material_candidate(
    recipe: dict[str, Any],
) -> tuple[dict[str, Any], list[dict[str, Any]], dict[str, Any]]:
    canonical = canonicalize(recipe)
    skeleton = copy.deepcopy(canonical)
    subject_patches: list[dict[str, Any]] = []
    derived_patches: list[dict[str, Any]] = []
    material_metas: list[int] = []
    input_material_meta: int | None = None

    for side in ("inputs", "outputs"):
        for index, stack in enumerate(canonical.get(side) or []):
            if not stack or not is_gt_meta_stack(stack):
                continue
            path = [side, index, "meta"]
            replace_path(skeleton, canonical, path, subject_patches)
            meta = stack.get("meta")
            if isinstance(meta, int):
                material_metas.append(meta)
                if side == "inputs" and input_material_meta is None:
                    input_material_meta = meta

    replace_path(skeleton, canonical, ["duration"], derived_patches)
    subject_patches.sort(key=patch_sort_key)
    derived_patches.sort(key=patch_sort_key)
    metadata = {
        "material_metas": sorted(set(material_metas)),
        "input_material_meta": input_material_meta,
    }
    return skeleton, subject_patches + derived_patches, metadata


def external_candidate(
    recipe: dict[str, Any],
) -> tuple[
    dict[str, Any],
    list[dict[str, Any]],
    list[dict[str, Any]],
    dict[str, Any],
]:
    canonical = canonicalize(recipe)
    skeleton = copy.deepcopy(canonical)
    subject_patches: list[dict[str, Any]] = []
    derived_patches: list[dict[str, Any]] = []
    external_identities: list[dict[str, Any]] = []
    material_metas: list[int] = []

    for index, stack in enumerate(canonical.get("inputs") or []):
        if not stack or is_shape_stack(stack) or is_gt_meta_stack(stack):
            continue
        identity = {
            key: copy.deepcopy(value)
            for key, value in stack.items()
            if key != "count"
        }
        external_identities.append(identity)
        for key in sorted(identity):
            replace_path(
                skeleton,
                canonical,
                ["inputs", index, key],
                subject_patches,
            )

    for side in ("inputs", "outputs"):
        for index, stack in enumerate(canonical.get(side) or []):
            if not stack or is_shape_stack(stack):
                continue
            meta = stack.get("meta")
            if is_gt_meta_stack(stack):
                if isinstance(meta, int):
                    material_metas.append(meta)
                # Input GT material metas are coordinated subject identity;
                # outputs are exact dependent bindings.
                target = subject_patches if side == "inputs" else derived_patches
                replace_path(skeleton, canonical, [side, index, "meta"], target)
            elif side == "outputs" and "meta" in stack:
                replace_path(
                    skeleton,
                    canonical,
                    [side, index, "meta"],
                    derived_patches,
                )

    replace_path(skeleton, canonical, ["duration"], derived_patches)
    subject_patches.sort(key=patch_sort_key)
    derived_patches.sort(key=patch_sort_key)
    metadata = {
        "external_identities": external_identities,
        "material_metas": sorted(set(material_metas)),
    }
    return skeleton, subject_patches, derived_patches, metadata


def operation_summary(skeleton: dict[str, Any]) -> dict[str, Any]:
    shapes = [
        {
            "slot": index,
            "item": stack.get("item"),
            "meta": stack.get("meta"),
            "count": stack.get("count"),
        }
        for index, stack in enumerate(skeleton.get("inputs") or [])
        if stack and is_shape_stack(stack)
    ]

    def forms(side: str) -> list[dict[str, Any]]:
        result = []
        for index, stack in enumerate(skeleton.get(side) or []):
            if not stack or is_shape_stack(stack):
                continue
            result.append({
                "slot": index,
                "item": stack.get("item"),
                "count": stack.get("count"),
            })
        return result

    return {
        "shape": shapes,
        "input_forms": forms("inputs"),
        "output_forms": forms("outputs"),
    }


def canonical_row_counter(recipes: list[dict[str, Any]]) -> Counter[str]:
    return Counter(recipe_digest(recipe) for recipe in recipes)


def exact_rows(recipes: list[dict[str, Any]]) -> list[dict[str, Any]]:
    grouped: dict[str, tuple[dict[str, Any], int]] = {}
    for recipe in recipes:
        canonical = canonicalize(recipe)
        digest = content_hash(canonical)
        if digest in grouped:
            previous, count = grouped[digest]
            grouped[digest] = (previous, count + 1)
        else:
            grouped[digest] = (canonical, 1)
    return [
        {
            "row_id": f"sha256:{digest}",
            "recipe": recipe,
            "multiplicity": count,
        }
        for digest, (recipe, count) in sorted(grouped.items())
    ]


def support_rows(
    members: list[
        tuple[
            dict[str, Any],
            list[dict[str, Any]],
            list[dict[str, Any]],
            dict[str, Any],
        ]
    ],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]], set[int]]:
    subjects: dict[str, dict[str, Any]] = {}
    supports: dict[str, dict[str, Any]] = {}
    material_ids: set[int] = set()
    for _recipe, subject_patches, derived_patches, metadata in members:
        subject_semantics = {"bindings": subject_patches}
        subject_id = f"sha256:{content_hash(subject_semantics)}"
        subjects.setdefault(subject_id, {
            "subject_id": subject_id,
            "bindings": subject_patches,
        })
        material_ids.update(metadata.get("material_metas") or [])
        support_semantics = {
            "subject_id": subject_id,
            "derived_bindings": derived_patches,
        }
        support_id = f"sha256:{content_hash(support_semantics)}"
        if support_id not in supports:
            supports[support_id] = {
                "support_id": support_id,
                **support_semantics,
                "multiplicity": 0,
            }
        supports[support_id]["multiplicity"] += 1
    return (
        [subjects[key] for key in sorted(subjects)],
        [supports[key] for key in sorted(supports)],
        material_ids,
    )


def duration_cross_validation(
    members: list[
        tuple[
            dict[str, Any],
            list[dict[str, Any]],
            list[dict[str, Any]],
            dict[str, Any],
        ]
    ],
    material_mass: dict[int, int],
) -> dict[str, Any]:
    pairs: list[tuple[int, int]] = []
    for recipe, _subject, _derived, metadata in members:
        material = metadata.get("input_material_meta")
        mass = material_mass.get(material)
        duration = recipe.get("duration")
        if not isinstance(mass, int) or mass <= 0 or not isinstance(duration, int):
            return {
                "kind": "exact_lookup",
                "verified": True,
                "formula_verified": False,
                "reason": "missing positive material mass or integer duration",
            }
        pairs.append((mass, duration))
    if (
        len(pairs) < MIN_DEPENDENCY_SAMPLE
        or len({mass for mass, _duration in pairs}) < MIN_DEPENDENCY_SAMPLE
    ):
        return {
            "kind": "exact_lookup",
            "verified": True,
            "formula_verified": False,
            "reason": "insufficient distinct material-mass samples",
            "sample_rows": len(pairs),
            "distinct_masses": len({mass for mass, _duration in pairs}),
        }
    ratios = {Fraction(duration, mass) for mass, duration in pairs}
    if len(ratios) == 1:
        ratio = next(iter(ratios))
        return {
            "kind": "formula",
            "verified": True,
            "formula_verified": True,
            "formula": {
                "op": "multiply_rational",
                "source": "material.mass",
                "numerator": ratio.numerator,
                "denominator": ratio.denominator,
            },
            "sample_rows": len(pairs),
            "distinct_masses": len({mass for mass, _duration in pairs}),
        }
    return {
        "kind": "exact_lookup",
        "verified": True,
        "formula_verified": False,
        "reason": "duration is not a single proportional function of material.mass",
        "sample_rows": len(pairs),
        "distinct_masses": len({mass for mass, _duration in pairs}),
    }


def build_template(
    classification: str,
    skeleton: dict[str, Any],
    members: list[
        tuple[
            dict[str, Any],
            list[dict[str, Any]],
            list[dict[str, Any]],
            dict[str, Any],
        ]
    ],
    material_mass: dict[int, int],
) -> dict[str, Any]:
    subjects, supports, material_ids = support_rows(members)
    relation_paths = sorted({
        binding["path"]
        for support in supports
        for binding in support["derived_bindings"]
    })
    identity = {
        "map": MAP_NAME,
        "classification": classification,
        "canonicalization": CANONICALIZATION,
        "skeleton": skeleton,
        "subject_binding_paths": sorted({
            binding["path"]
            for subject in subjects
            for binding in subject["bindings"]
        }),
        "derived_binding_paths": relation_paths,
    }
    template_id = f"sha256:{content_hash(identity)}"
    replayed = []
    for support in supports:
        subject = next(
            row for row in subjects
            if row["subject_id"] == support["subject_id"]
        )
        recipe = apply_patches(
            skeleton,
            [*subject["bindings"], *support["derived_bindings"]],
        )
        replayed.extend([recipe] * int(support["multiplicity"]))
    source_counter = canonical_row_counter([row[0] for row in members])
    replay_counter = canonical_row_counter(replayed)
    missing = source_counter - replay_counter
    extra = replay_counter - source_counter
    if classification == "material_template":
        duration_validation = duration_cross_validation(members, material_mass)
    else:
        duration_validation = {
            "kind": "exact_lookup",
            "verified": True,
            "formula_verified": False,
            "reason": "external identity mapping retained explicitly",
            "sample_rows": len(members),
            "distinct_subjects": len(subjects),
        }
    return {
        "template_id": template_id,
        "classification": classification,
        "operation": operation_summary(skeleton),
        "skeleton": skeleton,
        "axes": {
            "subject": {
                "kind": (
                    "coordinated_material_meta"
                    if classification == "material_template"
                    else "external_item_identity"
                ),
                "values": subjects,
            }
        },
        "relations": {
            "support": {
                "kind": "explicit_sparse_multiset",
                "rows": supports,
            },
            "duration_validation": duration_validation,
        },
        "expanded_count": len(members),
        "distinct_subject_count": len(subjects),
        "material_ids": sorted(material_ids),
        "source_multiset_sha256": multiset_digest(source_counter),
        "replay": {
            "replay_verified": not missing and not extra,
            "expected_count": sum(source_counter.values()),
            "actual_count": sum(replay_counter.values()),
            "missing_count": sum(missing.values()),
            "extra_count": sum(extra.values()),
        },
    }


def relation_analysis(recipes: list[dict[str, Any]]) -> dict[str, Any]:
    shape_to_forms: dict[int, set[str]] = defaultdict(set)
    forms_to_shapes: dict[str, set[int]] = defaultdict(set)
    all_shapes: set[int] = set()
    pure_count = 0
    for recipe in recipes:
        for index in shape_indices(recipe):
            meta = recipe["inputs"][index].get("meta")
            if isinstance(meta, int):
                all_shapes.add(meta)
        if not pure_gt_material_recipe(recipe):
            continue
        pure_count += 1
        shapes = shape_indices(recipe)
        shape = int(recipe["inputs"][shapes[0]]["meta"])
        forms = {
            "inputs": [
                (stack.get("item"), stack.get("count"))
                for stack in recipe.get("inputs") or []
                if stack and not is_shape_stack(stack)
            ],
            "outputs": [
                (stack.get("item"), stack.get("count"))
                for stack in recipe.get("outputs") or []
                if stack
            ],
        }
        form_key = stable_json(forms)
        shape_to_forms[shape].add(form_key)
        forms_to_shapes[form_key].add(shape)
    observed_pairs = sum(len(values) for values in shape_to_forms.values())
    possible = len(shape_to_forms) * len(forms_to_shapes)
    return {
        "pure_material_recipe_count": pure_count,
        "shape_count": len(all_shapes),
        "pure_material_shape_count": len(shape_to_forms),
        "external_only_shape_count": len(all_shapes - set(shape_to_forms)),
        "form_operation_count": len(forms_to_shapes),
        "observed_shape_form_pairs": observed_pairs,
        "possible_cartesian_pairs": possible,
        "cartesian_density": round(observed_pairs / possible, 6) if possible else 0,
        "shape_to_form_degree": dict(sorted(
            Counter(len(values) for values in shape_to_forms.values()).items()
        )),
        "form_to_shape_degree": dict(sorted(
            Counter(len(values) for values in forms_to_shapes.values()).items()
        )),
        "template_axis": "concrete extruder shape",
        "support_axes": "subject plus ordered input/output forms and counts",
        "support_is_sparse": True,
        "one_to_one": all(len(values) == 1 for values in shape_to_forms.values())
        and all(len(values) == 1 for values in forms_to_shapes.values()),
    }


def _extract_document_operation_v2(
    recipes: list[dict[str, Any]],
) -> dict[str, Any]:
    materials = json.loads(MATERIALS_PATH.read_text(encoding="utf-8"))
    material_mass = {
        row["id"]: row["mass"]
        for row in materials
        if isinstance(row.get("id"), int)
        and isinstance(row.get("mass"), int)
    }
    material_groups: dict[str, list[Any]] = defaultdict(list)
    external_groups: dict[str, list[Any]] = defaultdict(list)
    other_recipes: list[dict[str, Any]] = []

    for recipe in recipes:
        if pure_gt_material_recipe(recipe):
            skeleton, patches, metadata = material_candidate(recipe)
            subject = [
                row for row in patches
                if row["path"] != "/duration"
            ]
            derived = [
                row for row in patches
                if row["path"] == "/duration"
            ]
            material_groups[stable_json(skeleton)].append(
                (recipe, subject, derived, metadata)
            )
        elif external_input_recipe(recipe):
            skeleton, subject, derived, metadata = external_candidate(recipe)
            external_groups[stable_json(skeleton)].append(
                (recipe, subject, derived, metadata)
            )
        else:
            other_recipes.append(recipe)

    templates = []
    fragments = []
    external_review = []
    discrete = []

    for key, members in sorted(material_groups.items()):
        skeleton = json.loads(key)
        distinct_subjects = {
            stable_json(member[1]) for member in members
        }
        if len(members) >= MIN_DEPENDENCY_SAMPLE and len(distinct_subjects) >= MIN_DEPENDENCY_SAMPLE:
            templates.append(
                build_template(
                    "material_template",
                    skeleton,
                    members,
                    material_mass,
                )
            )
        else:
            fragments.append({
                "fragment_id": f"sha256:{content_hash({'class': 'material_fragment', 'skeleton': skeleton})}",
                "classification": "material_template_fragment",
                "reason_code": "insufficient_dependency_sample",
                "sample_rows": len(members),
                "distinct_subjects": len(distinct_subjects),
                "rows": exact_rows([member[0] for member in members]),
            })

    for key, members in sorted(external_groups.items()):
        skeleton = json.loads(key)
        distinct_subjects = {
            stable_json(member[1]) for member in members
        }
        if len(distinct_subjects) >= MIN_EXTERNAL_FAMILY:
            templates.append(
                build_template(
                    "external_item_template",
                    skeleton,
                    members,
                    material_mass,
                )
            )
        elif len(distinct_subjects) >= 2:
            external_review.append({
                "group_id": f"sha256:{content_hash({'class': 'external_review', 'skeleton': skeleton})}",
                "classification": "external_item_review",
                "reason_code": "below_minimum_family_sample",
                "sample_rows": len(members),
                "distinct_subjects": len(distinct_subjects),
                "rows": exact_rows([member[0] for member in members]),
            })
        else:
            discrete.extend(member[0] for member in members)

    discrete.extend(other_recipes)
    discrete_rows = exact_rows(discrete)

    source_counter = canonical_row_counter(recipes)
    replay_counter: Counter[str] = Counter()
    for template in templates:
        subjects = {
            row["subject_id"]: row
            for row in template["axes"]["subject"]["values"]
        }
        for support in template["relations"]["support"]["rows"]:
            recipe = apply_patches(
                template["skeleton"],
                [
                    *subjects[support["subject_id"]]["bindings"],
                    *support["derived_bindings"],
                ],
            )
            replay_counter[content_hash(recipe)] += int(support["multiplicity"])
    for collection in (fragments, external_review):
        for group in collection:
            for row in group["rows"]:
                replay_counter[row["row_id"].removeprefix("sha256:")] += int(
                    row["multiplicity"]
                )
    for row in discrete_rows:
        replay_counter[row["row_id"].removeprefix("sha256:")] += int(
            row["multiplicity"]
        )
    missing = source_counter - replay_counter
    extra = replay_counter - source_counter

    templates.sort(key=lambda row: row["template_id"])
    fragments.sort(key=lambda row: row["fragment_id"])
    external_review.sort(key=lambda row: row["group_id"])
    template_rows = sum(row["expanded_count"] for row in templates)
    fragment_rows = sum(row["sample_rows"] for row in fragments)
    review_rows = sum(row["sample_rows"] for row in external_review)
    discrete_count = sum(row["multiplicity"] for row in discrete_rows)
    all_local_verified = all(
        row["replay"]["replay_verified"] for row in templates
    )
    global_verified = not missing and not extra
    logical_recipe_count = (
        len(templates) + fragment_rows + review_rows + discrete_count
    )
    return {
        "schema_version": SCHEMA_VERSION,
        "artifact_kind": "gt6_extruder_templates",
        "map": MAP_NAME,
        "canonicalization": CANONICALIZATION,
        "inference_policy": {
            "minimum_dependency_rows": MIN_DEPENDENCY_SAMPLE,
            "minimum_distinct_determinants": MIN_DEPENDENCY_SAMPLE,
            "minimum_external_family_subjects": MIN_EXTERNAL_FAMILY,
            "functional_dependencies": (
                "Only exact lookups are required for replay. A material.mass "
                "formula is reported only after >=5 distinct masses match exactly."
            ),
        },
        "source": {
            "recipe_count": len(recipes),
            "canonical_multiset_sha256": multiset_digest(source_counter),
        },
        "axis_analysis": relation_analysis(recipes),
        "templates": templates,
        "fragments": fragments,
        "external_review_groups": external_review,
        "discrete_recipes": discrete_rows,
        "verification": {
            "replay_verified": all_local_verified and global_verified,
            "partition_verified": (
                template_rows + fragment_rows + review_rows + discrete_count
                == len(recipes)
            ),
            "template_local_replay_verified": all_local_verified,
            "expected_count": sum(source_counter.values()),
            "actual_count": sum(replay_counter.values()),
            "expected_multiset_sha256": multiset_digest(source_counter),
            "actual_multiset_sha256": multiset_digest(replay_counter),
            "missing_count": sum(missing.values()),
            "extra_count": sum(extra.values()),
        },
        "counts": {
            "logical_recipe_count": logical_recipe_count,
            "expanded_rows_per_logical_recipe": round(
                len(recipes) / logical_recipe_count, 6
            ),
            "functional_template_count": len(templates),
            "material_template_count": sum(
                row["classification"] == "material_template"
                for row in templates
            ),
            "external_item_template_count": sum(
                row["classification"] == "external_item_template"
                for row in templates
            ),
            "templated_expanded_rows": template_rows,
            "material_fragment_group_count": len(fragments),
            "material_fragment_rows": fragment_rows,
            "external_review_group_count": len(external_review),
            "external_review_rows": review_rows,
            "discrete_recipe_rows": discrete_count,
        },
    }


def _shape_marker() -> dict[str, bool]:
    return {"$shape": True}


def _resolve_shape(value: Any, shape: dict[str, Any]) -> Any:
    if isinstance(value, dict):
        if value == _shape_marker():
            return copy.deepcopy(shape)
        return {
            key: _resolve_shape(child, shape)
            for key, child in value.items()
        }
    if isinstance(value, list):
        return [_resolve_shape(child, shape) for child in value]
    return copy.deepcopy(value)


def _support_marker(key: str) -> dict[str, str]:
    return {"$support": key}


def _shape_row(recipe: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
    canonical = canonicalize(recipe)
    indices = shape_indices(canonical)
    if len(indices) != 1:
        raise ValueError(f"expected exactly one shape input, got {indices}")
    shape_index = indices[0]
    shape = copy.deepcopy(canonical["inputs"][shape_index])
    canonical["inputs"][shape_index] = _shape_marker()
    return canonical, shape


def _form_value(recipe: dict[str, Any], side: str) -> list[dict[str, Any]]:
    return [
        {
            "slot": index,
            **{
                key: copy.deepcopy(value)
                for key, value in stack.items()
                if key not in {"displayName", "meta"}
            },
        }
        for index, stack in enumerate(recipe.get(side) or [])
        if stack and not is_shape_stack(stack)
    ]


def _meta_values(recipe: dict[str, Any], side: str) -> list[Any]:
    values = []
    for stack in recipe.get(side) or []:
        if not stack or is_shape_stack(stack):
            continue
        if "meta" not in stack:
            raise ValueError(f"{side} stack has no meta field: {stack}")
        values.append(copy.deepcopy(stack["meta"]))
    return values


def _compact_meta_bindings(
    recipe: dict[str, Any],
    subject: dict[str, Any],
) -> list[list[Any]] | None:
    """Return only exceptional metas; one-material rows derive them from subject."""
    input_metas = _meta_values(recipe, "inputs")
    output_metas = _meta_values(recipe, "outputs")
    material_ids = subject.get("material_ids") or []
    if (
        subject.get("kind") == "material"
        and len(material_ids) == 1
        and all(
            meta == material_ids[0]
            for meta in [*input_metas, *output_metas]
        )
    ):
        return None
    return [input_metas, output_metas]


def _expand_form(
    form: list[dict[str, Any]],
    metas: list[Any],
    *,
    shape_slot: int | None = None,
) -> list[dict[str, Any]]:
    if len(form) != len(metas):
        raise ValueError(
            f"form/meta length mismatch: {len(form)} != {len(metas)}"
        )
    slots = [int(row["slot"]) for row in form]
    if shape_slot is not None:
        slots.append(shape_slot)
    size = max(slots, default=-1) + 1
    stacks: list[dict[str, Any] | None] = [None] * size
    for row, meta in zip(form, metas):
        slot = int(row["slot"])
        if stacks[slot] is not None:
            raise ValueError(f"duplicate form slot {slot}")
        stacks[slot] = {
            key: copy.deepcopy(value)
            for key, value in row.items()
            if key != "slot"
        }
        stacks[slot]["meta"] = copy.deepcopy(meta)
    if shape_slot is not None:
        if stacks[shape_slot] is not None:
            raise ValueError(f"shape/form slot collision at {shape_slot}")
        stacks[shape_slot] = _shape_marker()
    if any(stack is None for stack in stacks):
        raise ValueError(f"form leaves empty slots: {stacks}")
    return [stack for stack in stacks if stack is not None]


def _material_rows() -> list[dict[str, Any]]:
    return json.loads(MATERIALS_PATH.read_text(encoding="utf-8"))


def _material_catalog() -> dict[int, dict[str, Any]]:
    # -1 is a shared sentinel for 441 name-only records, not an identity.
    return {
        int(row["id"]): row
        for row in _material_rows()
        if isinstance(row.get("id"), int) and int(row["id"]) >= 0
    }


def _prefix_catalog(
    materials: dict[int, dict[str, Any]],
) -> tuple[dict[str, dict[str, Any]], dict[str, frozenset[int]]]:
    rows = json.loads(
        (DUMP / "oredict" / "prefixes.json").read_text(encoding="utf-8")
    )
    by_name = {
        row["nameInternal"]: material_id
        for material_id, row in materials.items()
    }
    prefixes = {
        row["nameInternal"]: row
        for row in rows
    }
    registered = {
        name: frozenset(
            by_name[material_name]
            for material_name in row.get("registeredMaterials") or []
            if material_name in by_name
        )
        for name, row in prefixes.items()
    }
    return prefixes, registered


def _subject_value(recipe: dict[str, Any]) -> dict[str, Any]:
    material_ids = sorted({
        int(stack["meta"])
        for side in ("inputs", "outputs")
        for stack in recipe.get(side) or []
        if stack
        and not is_shape_stack(stack)
        and is_gt_meta_stack(stack)
        and isinstance(stack.get("meta"), int)
    })
    input_material_ids = sorted({
        int(stack["meta"])
        for stack in recipe.get("inputs") or []
        if stack
        and not is_shape_stack(stack)
        and is_gt_meta_stack(stack)
        and isinstance(stack.get("meta"), int)
    })
    external_inputs = []
    for index, stack in enumerate(recipe.get("inputs") or []):
        if not stack or is_shape_stack(stack) or is_gt_meta_stack(stack):
            continue
        external_inputs.append({
            "slot": index,
            "identity": {
                key: canonicalize(value)
                for key, value in stack.items()
                if key not in {"count", "displayName"}
            },
        })
    if material_ids and external_inputs:
        kind = "mixed"
    elif material_ids:
        kind = "material"
    elif external_inputs:
        kind = "external_item"
    else:
        kind = "fixed"
    return {
        "kind": kind,
        "material_ids": material_ids,
        "input_material_ids": input_material_ids,
        "external_inputs": external_inputs,
    }


def _eu_rule_marker() -> dict[str, str]:
    return {"$rule": "extruder_eu_from_material_tags"}


def _derive_material_eu(
    template: dict[str, Any],
    subject: dict[str, Any],
    materials: dict[int, dict[str, Any]],
) -> int:
    input_material_ids = subject.get("input_material_ids") or []
    if len(input_material_ids) != 1:
        raise ValueError(
            f"EU tag rule requires one input material, got "
            f"{input_material_ids} in {template['template_id']}"
        )
    material = materials.get(int(input_material_ids[0]))
    if material is None:
        raise ValueError(
            f"EU tag rule cannot resolve material {input_material_ids[0]}"
        )
    tags = set(material.get("tags") or [])
    name = material.get("nameInternal")
    if name == "Graphite":
        return 512
    if template["heat_mode"] == "low_heat":
        if "PROCESSING.EXTRUDABLE_SIMPLE" not in tags:
            raise ValueError(
                f"low-heat EU rule received non-simple material {name}"
            )
        return 16
    if "PROCESSING.EXTRUDABLE_SIMPLE" in tags:
        return 16
    if "PROCESSING.EXTRUDABLE" in tags:
        return 96
    raise ValueError(f"normal extruder EU rule has no domain for {name}")


def _domain_material_marker() -> dict[str, bool]:
    return {"$domain_material": True}


def _resolve_domain_material(value: Any, material_id: int) -> Any:
    if isinstance(value, dict):
        if value == _domain_material_marker():
            return material_id
        return {
            key: _resolve_domain_material(child, material_id)
            for key, child in value.items()
        }
    if isinstance(value, list):
        return [
            _resolve_domain_material(child, material_id)
            for child in value
        ]
    return copy.deepcopy(value)


def _processing_tag_sets(
    materials: dict[int, dict[str, Any]],
) -> dict[str, frozenset[int]]:
    tags = sorted({
        tag
        for row in materials.values()
        for tag in row.get("tags") or []
        if tag.startswith("PROCESSING.")
    })
    return {
        tag: frozenset(
            material_id
            for material_id, row in materials.items()
            if tag in (row.get("tags") or [])
        )
        for tag in tags
    }


def _evaluate_domain(
    domain: dict[str, Any],
    prefix_sets: dict[str, frozenset[int]],
    processing_sets: dict[str, frozenset[int]],
) -> list[int]:
    universe: frozenset[int] | None = None
    for form in domain["forms"]:
        registered = prefix_sets.get(form, frozenset())
        universe = registered if universe is None else universe & registered
    selected = universe or frozenset()
    processing_tag = domain.get("processing_tag")
    if processing_tag:
        selected = selected & processing_sets.get(
            processing_tag,
            frozenset(),
        )
    for excluded_tag in domain.get("exclude_processing_tags") or []:
        selected = selected - processing_sets.get(
            excluded_tag,
            frozenset(),
        )
    selected = (
        selected
        - set(domain.get("exclude_material_ids") or [])
    ) | set(domain.get("include_material_ids") or [])
    return sorted(selected)


def _domain_pattern(
    recipe: dict[str, Any],
) -> tuple[dict[str, Any], int, tuple[str, ...]] | None:
    canonical, _shape = _shape_row(recipe)
    inputs = [
        stack
        for stack in canonical.get("inputs") or []
        if stack and stack != _shape_marker()
    ]
    if len(inputs) != 1 or not is_gt_meta_stack(inputs[0]):
        return None
    material_id = inputs[0].get("meta")
    if not isinstance(material_id, int):
        return None
    forms = []
    for side in ("inputs", "outputs"):
        for stack in canonical.get(side) or []:
            if not stack or stack == _shape_marker():
                continue
            if not is_gt_meta_stack(stack) or stack.get("meta") != material_id:
                return None
            forms.append(str(stack["item"])[len(META_PREFIX):])
            stack["meta"] = _domain_material_marker()
    # EU is independently replayed from the verified PROCESSING rule.
    canonical["euPerTick"] = _eu_rule_marker()
    return canonical, material_id, tuple(forms)


def _build_constant_domain_plan(
    recipes: list[dict[str, Any]],
    materials: dict[int, dict[str, Any]],
    prefix_sets: dict[str, frozenset[int]],
) -> tuple[
    dict[int, list[dict[str, Any]]],
    set[str],
    dict[str, int],
]:
    processing_sets = _processing_tag_sets(materials)
    candidates: dict[str, list[tuple[dict[str, Any], int, tuple[str, ...]]]] = (
        defaultdict(list)
    )
    for recipe in recipes:
        if not pure_gt_material_recipe(recipe):
            continue
        pattern_row = _domain_pattern(recipe)
        if pattern_row is None:
            continue
        pattern, material_id, forms = pattern_row
        shape_meta = next(
            int(stack["meta"])
            for stack in recipe["inputs"]
            if stack and is_shape_stack(stack)
        )
        # Keep the observed tier in the grouping key. The serialized pattern
        # can omit it because replay derives it independently.
        key = stable_json({
            "shape_meta": shape_meta,
            "pattern": pattern,
            "observed_eu": int(recipe["euPerTick"]),
        })
        candidates[key].append((recipe, material_id, forms))

    groups_by_shape: dict[int, list[dict[str, Any]]] = defaultdict(list)
    compressed_recipe_digests: set[str] = set()
    compressed_rows = compressed_members = exception_members = 0
    for key, members in candidates.items():
        actual_counter = Counter(row[1] for row in members)
        actual = frozenset(actual_counter)
        if len(actual) < 2:
            continue
        pattern = json.loads(key)["pattern"]
        forms = members[0][2]
        universe: frozenset[int] | None = None
        for form in forms:
            registered = prefix_sets.get(form, frozenset())
            universe = (
                registered
                if universe is None
                else universe & registered
            )
        universe = universe or frozenset()
        domain_candidates = []
        processing_domains = [
            (None, [], universe),
            *[
                (tag, [], universe & tagged)
                for tag, tagged in processing_sets.items()
            ],
        ]
        if {
            "PROCESSING.EXTRUDABLE",
            "PROCESSING.EXTRUDABLE_SIMPLE",
        } <= set(processing_sets):
            processing_domains.append((
                "PROCESSING.EXTRUDABLE",
                ["PROCESSING.EXTRUDABLE_SIMPLE"],
                (
                    (
                        universe
                        & processing_sets["PROCESSING.EXTRUDABLE"]
                    )
                    - processing_sets["PROCESSING.EXTRUDABLE_SIMPLE"]
                ),
            ))
        for processing_tag, excluded_tags, selected in processing_domains:
            include = actual - selected
            exclude = selected - actual
            domain_candidates.append((
                len(include) + len(exclude),
                processing_tag or "",
                tuple(excluded_tags),
                include,
                exclude,
            ))
        (
            _difference,
            tag_key,
            excluded_tags,
            include,
            exclude,
        ) = min(domain_candidates)
        # A reference plus exceptions must be strictly smaller than the
        # explicit member list. Otherwise keep ordinary support rows.
        if 1 + len(include) + len(exclude) >= len(actual):
            continue
        processing_tag = tag_key or None
        domain = {
            "forms": list(forms),
            "processing_tag": processing_tag,
            "exclude_processing_tags": list(excluded_tags),
            "include_material_ids": sorted(include),
            "exclude_material_ids": sorted(exclude),
        }
        if _evaluate_domain(domain, prefix_sets, processing_sets) != sorted(actual):
            raise ValueError("constant domain expression did not replay")
        multiplicity = {
            str(material_id): count
            for material_id, count in sorted(actual_counter.items())
            if count != 1
        }
        identity = {
            "map": MAP_NAME,
            "kind": "constant_tag_domain",
            "pattern": pattern,
            # Identity follows expansion semantics, not compression syntax.
            "expanded_material_ids": sorted(actual),
            "multiplicity": multiplicity,
        }
        group_id = f"sha256:{content_hash(identity)}"
        shape_meta = next(
            int(stack["meta"])
            for stack in members[0][0]["inputs"]
            if stack and is_shape_stack(stack)
        )
        groups_by_shape[shape_meta].append({
            "group_id": group_id,
            "kind": "constant_tag_domain",
            "recipe_pattern": pattern,
            "domain": domain,
            "material_multiplicity": multiplicity,
            "expanded_count": len(members),
            "material_count": len(actual),
        })
        compressed_recipe_digests.update(
            recipe_digest(row[0]) for row in members
        )
        compressed_rows += len(members)
        compressed_members += len(actual)
        exception_members += len(include) + len(exclude)
    for rows in groups_by_shape.values():
        rows.sort(key=lambda row: row["group_id"])
    return (
        groups_by_shape,
        compressed_recipe_digests,
        {
            "constant_domain_group_count": sum(
                len(rows) for rows in groups_by_shape.values()
            ),
            "constant_domain_recipe_count": compressed_rows,
            "replaced_explicit_member_count": compressed_members,
            "stored_domain_exception_count": exception_members,
        },
    )


def _replay_shape_template(
    template: dict[str, Any],
    materials: dict[int, dict[str, Any]] | None = None,
    prefix_sets: dict[str, frozenset[int]] | None = None,
) -> Counter[str]:
    counter: Counter[str] = Counter()
    shape = template["shape"]
    subjects = template["axes"]["subject"]["values"]
    input_forms = template["axes"]["input_form"]["values"]
    output_forms = template["axes"]["output_form"]["values"]
    support_relation = template["relations"]["support"]
    columns = support_relation["columns"]
    column_indexes = {name: index for index, name in enumerate(columns)}
    required_columns = {
        "subject",
        "input_form",
        "output_form",
        "multiplicity",
        "meta_bindings",
    }
    missing_columns = required_columns - set(column_indexes)
    if missing_columns:
        raise ValueError(
            f"compact support is missing columns: {sorted(missing_columns)}"
        )
    material_catalog = materials or _material_catalog()
    if prefix_sets is None:
        _prefixes, prefix_sets = _prefix_catalog(material_catalog)
    processing_sets = _processing_tag_sets(material_catalog)
    structural_columns = required_columns
    value_columns = [
        name for name in columns
        if name not in structural_columns
    ]
    shape_slot = int(template["shape_slot"])
    for support in support_relation["rows"]:
        if len(support) != len(columns):
            raise ValueError(
                f"support row width {len(support)} != {len(columns)}"
            )
        subject = subjects[support[column_indexes["subject"]]]
        input_form = input_forms[support[column_indexes["input_form"]]]
        output_form = output_forms[support[column_indexes["output_form"]]]
        meta_bindings = support[column_indexes["meta_bindings"]]
        if meta_bindings is None:
            material_ids = subject.get("material_ids") or []
            if len(material_ids) != 1:
                raise ValueError(
                    "derived meta binding requires exactly one subject material"
                )
            input_metas = [material_ids[0]] * len(input_form)
            output_metas = [material_ids[0]] * len(output_form)
        else:
            if not (
                isinstance(meta_bindings, list)
                and len(meta_bindings) == 2
            ):
                raise ValueError(
                    f"invalid compact meta bindings: {meta_bindings}"
                )
            input_metas, output_metas = meta_bindings
        recipe = copy.deepcopy(template["base_recipe"])
        recipe["inputs"] = _expand_form(
            input_form,
            input_metas,
            shape_slot=shape_slot,
        )
        recipe["outputs"] = _expand_form(output_form, output_metas)
        for key in value_columns:
            value = support[column_indexes[key]]
            if value is not None:
                recipe[key] = copy.deepcopy(value)
        if recipe.get("euPerTick") == _eu_rule_marker():
            recipe["euPerTick"] = _derive_material_eu(
                template,
                subject,
                material_catalog,
            )
        unresolved = [
            key
            for key, value in recipe.items()
            if value == _support_marker(key)
        ]
        if unresolved:
            raise ValueError(
                f"unresolved shape support fields in "
                f"{template['template_id']}: {unresolved}"
            )
        recipe = _resolve_shape(recipe, shape)
        counter[content_hash(recipe)] += int(
            support[column_indexes["multiplicity"]]
        )
    for group in template["relations"].get("domain_groups") or []:
        material_ids = _evaluate_domain(
            group["domain"],
            prefix_sets,
            processing_sets,
        )
        for material_id in material_ids:
            recipe = _resolve_domain_material(
                group["recipe_pattern"],
                material_id,
            )
            recipe = _resolve_shape(recipe, shape)
            if recipe.get("euPerTick") == _eu_rule_marker():
                recipe["euPerTick"] = _derive_material_eu(
                    template,
                    {
                        "input_material_ids": [material_id],
                    },
                    material_catalog,
                )
            multiplicity = int(
                group.get("material_multiplicity", {}).get(
                    str(material_id),
                    1,
                )
            )
            counter[content_hash(recipe)] += multiplicity
    return counter


def _build_shape_template(
    shape: dict[str, Any],
    members: list[dict[str, Any]],
    materials: dict[int, dict[str, Any]],
    prefix_sets: dict[str, frozenset[int]],
    domain_groups: list[dict[str, Any]],
    compressed_recipe_digests: set[str],
) -> dict[str, Any]:
    shape_meta = shape.get("meta")
    observed_shape_slots = {
        shape_indices(recipe)[0]
        for recipe in members
        if len(shape_indices(recipe)) == 1
    }
    if len(observed_shape_slots) != 1:
        raise ValueError(
            f"shape {shape_meta} has inconsistent slots: "
            f"{sorted(observed_shape_slots)}"
        )
    shape_slot = next(iter(observed_shape_slots))
    heat_mode = (
        "low_heat"
        if isinstance(shape_meta, int) and 10201 <= shape_meta <= 10231
        else "normal"
    )
    base_shape_meta = (
        shape_meta - 200
        if heat_mode == "low_heat" and isinstance(shape_meta, int)
        else shape_meta
    )
    support_members = [
        recipe
        for recipe in members
        if recipe_digest(recipe) not in compressed_recipe_digests
    ]
    transformed = [_shape_row(recipe)[0] for recipe in support_members]
    all_keys = sorted({
        key for recipe in transformed for key in recipe
    })
    base_recipe: dict[str, Any] = {}
    varying_keys = []
    for key in all_keys:
        if key in {"inputs", "outputs"}:
            base_recipe[key] = _support_marker(key)
            continue
        if key == "euPerTick":
            base_recipe[key] = _eu_rule_marker()
            continue
        values = [recipe.get(key) for recipe in transformed]
        if all(value == values[0] for value in values[1:]):
            base_recipe[key] = copy.deepcopy(values[0])
        else:
            base_recipe[key] = _support_marker(key)
            varying_keys.append(key)

    subjects: dict[str, dict[str, Any]] = {}
    input_forms: dict[str, dict[str, Any]] = {}
    output_forms: dict[str, dict[str, Any]] = {}
    supports: dict[str, dict[str, Any]] = {}
    material_ids: set[int] = {
        int(stack["meta"])
        for recipe in members
        for side in ("inputs", "outputs")
        for stack in recipe.get(side) or []
        if stack
        and is_gt_meta_stack(stack)
        and isinstance(stack.get("meta"), int)
    }
    external_namespaces: set[str] = set()

    for original, row in zip(support_members, transformed):
        subject = _subject_value(original)
        subject_id = f"sha256:{content_hash(subject)}"
        subjects.setdefault(subject_id, subject)
        material_ids.update(subject["material_ids"])
        for external in subject["external_inputs"]:
            item = str(external["identity"].get("item") or "")
            if ":" in item:
                external_namespaces.add(item.split(":", 1)[0])

        input_form = _form_value(original, "inputs")
        input_form_id = f"sha256:{content_hash(input_form)}"
        input_forms.setdefault(input_form_id, input_form)
        output_form = _form_value(original, "outputs")
        output_form_id = f"sha256:{content_hash(output_form)}"
        output_forms.setdefault(output_form_id, output_form)

        pure_material = pure_gt_material_recipe(original)
        tag_derived_eu = False
        if pure_material:
            try:
                tag_derived_eu = _derive_material_eu(
                    {
                        "heat_mode": heat_mode,
                        "template_id": "candidate",
                    },
                    subject,
                    materials,
                ) == int(row.get("euPerTick"))
            except ValueError:
                tag_derived_eu = False
        values = {
            key: copy.deepcopy(row.get(key))
            for key in varying_keys
        }
        if not tag_derived_eu:
            values["euPerTick"] = copy.deepcopy(row.get("euPerTick"))
        meta_bindings = _compact_meta_bindings(original, subject)
        semantics = {
            "subject_id": subject_id,
            "input_form_id": input_form_id,
            "output_form_id": output_form_id,
            "meta_bindings": meta_bindings,
            "values": values,
        }
        support_id = f"sha256:{content_hash(semantics)}"
        if support_id not in supports:
            supports[support_id] = {
                "support_id": support_id,
                **semantics,
                "multiplicity": 0,
            }
        supports[support_id]["multiplicity"] += 1

    subject_ids = sorted(subjects)
    input_form_ids = sorted(input_forms)
    output_form_ids = sorted(output_forms)
    subject_indexes = {
        value_id: index for index, value_id in enumerate(subject_ids)
    }
    input_form_indexes = {
        value_id: index for index, value_id in enumerate(input_form_ids)
    }
    output_form_indexes = {
        value_id: index for index, value_id in enumerate(output_form_ids)
    }
    support_value_fields = sorted({
        key
        for support in supports.values()
        for key in support["values"]
    })
    support_columns = [
        "subject",
        "input_form",
        "output_form",
        "multiplicity",
        "meta_bindings",
        *support_value_fields,
    ]
    compact_support_rows = [
        [
            subject_indexes[support["subject_id"]],
            input_form_indexes[support["input_form_id"]],
            output_form_indexes[support["output_form_id"]],
            int(support["multiplicity"]),
            copy.deepcopy(support["meta_bindings"]),
            *[
                copy.deepcopy(support["values"].get(key))
                for key in support_value_fields
            ],
        ]
        for support in (
            supports[support_id] for support_id in sorted(supports)
        )
    ]

    shape_identity = {
        "map": MAP_NAME,
        "canonicalization": CANONICALIZATION,
        "axis": "extruder_shape",
        "shape": shape,
    }
    template_id = f"sha256:{content_hash(shape_identity)}"
    template = {
        "template_id": template_id,
        "classification": "shape_template",
        "shape": shape,
        "shape_slot": shape_slot,
        "shape_meta": shape_meta,
        "base_shape_meta": base_shape_meta,
        "heat_mode": heat_mode,
        "base_recipe": base_recipe,
        "axes": {
            "subject": {
                "kind": "material_or_external_item_identity",
                "values": [subjects[key] for key in subject_ids],
            },
            "input_form": {
                "kind": "ordered_stack_skeleton_without_meta",
                "values": [input_forms[key] for key in input_form_ids],
            },
            "output_form": {
                "kind": "ordered_stack_skeleton_without_meta",
                "values": [output_forms[key] for key in output_form_ids],
            },
        },
        "relations": {
            "support": {
                "kind": "compact_template_local_index_rows",
                "columns": support_columns,
                "varying_top_level_fields": varying_keys,
                "null_semantics": {
                    "meta_bindings": (
                        "fill every non-shape stack meta from the sole "
                        "subject material id"
                    ),
                    "euPerTick": (
                        "derive from extruder_eu_from_material_tags"
                    ),
                },
                "rows": compact_support_rows,
            },
            "domain_groups": domain_groups,
            "dependency_policy": {
                "kind": "tag_rule_plus_exact_lookup",
                "verified": True,
                "formula_verified": False,
                "reason": (
                    "pure-material EU derives from PROCESSING tags; external "
                    "EU and all duration values remain explicit"
                ),
            },
            "eu_rule": {
                "kind": "material_tag_rule",
                "normal": [
                    {
                        "when_material": "Graphite",
                        "euPerTick": 512,
                    },
                    {
                        "when_tag": "PROCESSING.EXTRUDABLE_SIMPLE",
                        "euPerTick": 16,
                    },
                    {
                        "when_tag": "PROCESSING.EXTRUDABLE",
                        "euPerTick": 96,
                    },
                ],
                "low_heat": {
                    "required_tag": "PROCESSING.EXTRUDABLE_SIMPLE",
                    "euPerTick": 16,
                },
                "observed_domain_verified": True,
            },
        },
        "expanded_count": len(members),
        "distinct_subject_count": len({
            stable_json(_subject_value(recipe))
            for recipe in members
        }),
        "material_ids": sorted(material_ids),
        "external_namespaces": sorted(external_namespaces),
        "source_multiset_sha256": multiset_digest(
            canonical_row_counter(members)
        ),
    }
    replay_counter = _replay_shape_template(
        template,
        materials,
        prefix_sets,
    )
    source_counter = canonical_row_counter(members)
    missing = source_counter - replay_counter
    extra = replay_counter - source_counter
    template["replay"] = {
        "replay_verified": not missing and not extra,
        "expected_count": sum(source_counter.values()),
        "actual_count": sum(replay_counter.values()),
        "missing_count": sum(missing.values()),
        "extra_count": sum(extra.values()),
    }
    return template


def _external_axis_diagnostics(
    recipes: list[dict[str, Any]],
) -> dict[str, Any]:
    groups: dict[str, list[Any]] = defaultdict(list)
    for recipe in recipes:
        if not external_input_recipe(recipe):
            continue
        skeleton, subject, derived, metadata = external_candidate(recipe)
        groups[stable_json(skeleton)].append(
            (recipe, subject, derived, metadata)
        )
    family_groups = review_groups = discrete_groups = 0
    family_rows = review_rows = discrete_rows = 0
    for members in groups.values():
        distinct_subjects = len({
            stable_json(member[1]) for member in members
        })
        if distinct_subjects >= MIN_EXTERNAL_FAMILY:
            family_groups += 1
            family_rows += len(members)
        elif distinct_subjects >= 2:
            review_groups += 1
            review_rows += len(members)
        else:
            discrete_groups += 1
            discrete_rows += len(members)
    return {
        "external_input_recipe_count": sum(
            external_input_recipe(recipe) for recipe in recipes
        ),
        "mergeable_logic_family_count": family_groups,
        "mergeable_recipe_count": family_rows,
        "small_review_group_count": review_groups,
        "small_review_recipe_count": review_rows,
        "discrete_logic_group_count": discrete_groups,
        "discrete_recipe_count": discrete_rows,
        "note": (
            "These are support-table diagnostics, not template counts. "
            "Template identity remains the extruder shape."
        ),
    }


def extract_document(recipes: list[dict[str, Any]]) -> dict[str, Any]:
    material_rows = _material_rows()
    materials = _material_catalog()
    prefixes, prefix_sets = _prefix_catalog(materials)
    (
        domain_groups_by_shape,
        compressed_recipe_digests,
        domain_counts,
    ) = _build_constant_domain_plan(recipes, materials, prefix_sets)
    shape_groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    shape_values: dict[str, dict[str, Any]] = {}
    no_shape = []
    for recipe in recipes:
        indices = shape_indices(recipe)
        if len(indices) != 1:
            no_shape.append(recipe)
            continue
        canonical = canonicalize(recipe)
        shape = canonical["inputs"][indices[0]]
        key = stable_json(shape)
        shape_values[key] = shape
        shape_groups[key].append(recipe)

    templates = [
        _build_shape_template(
            shape_values[key],
            members,
            materials,
            prefix_sets,
            domain_groups_by_shape.get(
                int(shape_values[key].get("meta") or -1),
                [],
            ),
            compressed_recipe_digests,
        )
        for key, members in sorted(
            shape_groups.items(),
            key=lambda item: (
                int(shape_values[item[0]].get("meta") or -1),
                item[0],
            ),
        )
    ]
    discrete_rows = exact_rows(no_shape)
    source_counter = canonical_row_counter(recipes)
    replay_counter: Counter[str] = Counter()
    for template in templates:
        replay_counter.update(
            _replay_shape_template(template, materials, prefix_sets)
        )
    for row in discrete_rows:
        replay_counter[
            row["row_id"].removeprefix("sha256:")
        ] += int(row["multiplicity"])
    missing = source_counter - replay_counter
    extra = replay_counter - source_counter
    all_local_verified = all(
        template["replay"]["replay_verified"]
        for template in templates
    )
    discrete_count = sum(
        int(row["multiplicity"]) for row in discrete_rows
    )
    derived_eu_rows = 0
    explicit_eu_rows = 0
    for template in templates:
        support_relation = template["relations"]["support"]
        columns = support_relation["columns"]
        multiplicity_index = columns.index("multiplicity")
        eu_index = (
            columns.index("euPerTick")
            if "euPerTick" in columns
            else None
        )
        for support in support_relation["rows"]:
            multiplicity = int(support[multiplicity_index])
            if eu_index is None or support[eu_index] is None:
                derived_eu_rows += multiplicity
            else:
                explicit_eu_rows += multiplicity
    derived_eu_rows += sum(
        int(group["expanded_count"])
        for template in templates
        for group in template["relations"].get("domain_groups") or []
    )
    pure_eu_counts = Counter(
        int(recipe["euPerTick"])
        for recipe in recipes
        if pure_gt_material_recipe(recipe)
    )
    logical_recipe_count = len(templates) + discrete_count
    return {
        "schema_version": SCHEMA_VERSION,
        "artifact_kind": "gt6_extruder_shape_templates",
        "map": MAP_NAME,
        "canonicalization": CANONICALIZATION,
        "template_policy": {
            "template_identity": "one template per concrete extruder shape",
            "normal_low_heat": (
                "normal and low-heat shape metas remain separate templates"
            ),
            "support": (
                "all observed subject/form/count/EU/duration combinations "
                "are stored as an exact sparse relation using template-local "
                "integer indexes and positional columns; no Cartesian product "
                "is inferred"
            ),
            "support_row_ids": (
                "omitted; rows are internal encoding, while template IDs "
                "remain stable content addresses"
            ),
        },
        "source": {
            "recipe_count": len(recipes),
            "canonical_multiset_sha256": multiset_digest(source_counter),
            "materials_raw_sha256": file_sha256(MATERIALS_PATH),
            "materials_canonical_sha256": content_hash(material_rows),
            "prefixes_raw_sha256": file_sha256(
                DUMP / "oredict" / "prefixes.json"
            ),
            "prefixes_canonical_sha256": content_hash([
                prefixes[key] for key in sorted(prefixes)
            ]),
        },
        "axis_analysis": relation_analysis(recipes),
        "external_axis_diagnostics": _external_axis_diagnostics(recipes),
        "templates": templates,
        "discrete_recipes": discrete_rows,
        "verification": {
            "replay_verified": all_local_verified and not missing and not extra,
            "partition_verified": (
                sum(row["expanded_count"] for row in templates)
                + discrete_count
                == len(recipes)
            ),
            "template_local_replay_verified": all_local_verified,
            "expected_count": sum(source_counter.values()),
            "actual_count": sum(replay_counter.values()),
            "expected_multiset_sha256": multiset_digest(source_counter),
            "actual_multiset_sha256": multiset_digest(replay_counter),
            "missing_count": sum(missing.values()),
            "extra_count": sum(extra.values()),
        },
        "counts": {
            "logical_recipe_count": logical_recipe_count,
            "expanded_rows_per_logical_recipe": round(
                len(recipes) / logical_recipe_count, 6
            ),
            "shape_template_count": len(templates),
            "normal_shape_template_count": sum(
                row["heat_mode"] == "normal" for row in templates
            ),
            "low_heat_shape_template_count": sum(
                row["heat_mode"] == "low_heat" for row in templates
            ),
            "templated_expanded_rows": sum(
                row["expanded_count"] for row in templates
            ),
            "tag_derived_eu_rows": derived_eu_rows,
            "tag_derived_eu_value_counts": {
                str(eu): count
                for eu, count in sorted(pure_eu_counts.items())
            },
            "explicit_eu_rows": explicit_eu_rows,
            **domain_counts,
            "explicit_support_row_count": sum(
                len(template["relations"]["support"]["rows"])
                for template in templates
            ),
            "discrete_recipe_rows": discrete_count,
        },
    }


def build_summary(document: dict[str, Any]) -> dict[str, Any]:
    largest = sorted(
        document["templates"],
        key=lambda row: (-row["expanded_count"], row["template_id"]),
    )[:20]
    return {
        "schema_version": SCHEMA_VERSION,
        "map": MAP_NAME,
        "source": document["source"],
        "template_policy": document["template_policy"],
        "axis_analysis": document["axis_analysis"],
        "external_axis_diagnostics": document["external_axis_diagnostics"],
        "counts": document["counts"],
        "verification": document["verification"],
        "largest_templates": [
            {
                "template_id": row["template_id"],
                "shape_meta": row["shape_meta"],
                "base_shape_meta": row["base_shape_meta"],
                "heat_mode": row["heat_mode"],
                "shape_item": row["shape"].get("item"),
                "expanded_count": row["expanded_count"],
                "distinct_subject_count": row["distinct_subject_count"],
                "input_form_count": len(
                    row["axes"]["input_form"]["values"]
                ),
                "output_form_count": len(
                    row["axes"]["output_form"]["values"]
                ),
            }
            for row in largest
        ],
        "artifact": OUT.relative_to(ROOT).as_posix(),
        "coverage_index": INDEX_OUT.relative_to(ROOT).as_posix(),
    }


def build_index(document: dict[str, Any]) -> dict[str, Any]:
    exact_remainder = []
    for row in document["discrete_recipes"]:
        exact_remainder.append({
            "row_id": row["row_id"],
            "classification": "discrete_recipe",
            "multiplicity": row["multiplicity"],
            "material_ids": sorted(
                {
                    stack["meta"]
                    for side in ("inputs", "outputs")
                    for stack in row["recipe"].get(side) or []
                    if stack
                    and is_gt_meta_stack(stack)
                    and isinstance(stack.get("meta"), int)
                }
            ),
        })
    exact_remainder.sort(
        key=lambda row: (row["classification"], row["row_id"])
    )
    return {
        "schema_version": SCHEMA_VERSION,
        "map": MAP_NAME,
        "accounting_unit": "verified_template_plus_exact_remainder",
        "replay_verified": document["verification"]["replay_verified"],
        "source_recipe_count": document["source"]["recipe_count"],
        "functional_template_count": document["counts"][
            "shape_template_count"
        ],
        "logical_recipe_count": document["counts"]["logical_recipe_count"],
        "exact_remainder_count": document["counts"]["discrete_recipe_rows"],
        "templates": [
            {
                "template_id": row["template_id"],
                "shape_meta": row["shape_meta"],
                "heat_mode": row["heat_mode"],
                "expanded_count": row["expanded_count"],
                "material_ids": row["material_ids"],
                "replay_verified": row["replay"]["replay_verified"],
            }
            for row in document["templates"]
        ],
        "exact_remainder": exact_remainder,
    }


def write_json(path: Path, value: Any, *, pretty: bool) -> None:
    if pretty:
        text = json.dumps(value, indent=2, ensure_ascii=False)
    else:
        text = stable_json(value)
    path.write_text(text + "\n", encoding="utf-8", newline="\n")


def extract() -> dict[str, Any]:
    source = json.loads(MAP_PATH.read_text(encoding="utf-8"))
    if source.get("nameInternal") != MAP_NAME:
        raise RuntimeError("extruder map identity mismatch")
    recipes = list(source.get("recipes") or [])
    document = extract_document(recipes)
    write_json(OUT, document, pretty=False)
    write_json(SUMMARY_OUT, build_summary(document), pretty=True)
    write_json(INDEX_OUT, build_index(document), pretty=True)
    return document


def verify_document(document: dict[str, Any]) -> dict[str, Any]:
    """Replay an artifact independently against the pinned source dump."""
    source = json.loads(MAP_PATH.read_text(encoding="utf-8"))
    source_counter = canonical_row_counter(list(source.get("recipes") or []))
    material_rows = _material_rows()
    materials = _material_catalog()
    materials_raw_hash = file_sha256(MATERIALS_PATH)
    materials_canonical_hash = content_hash(material_rows)
    prefixes, prefix_sets = _prefix_catalog(materials)
    prefixes_raw_hash = file_sha256(
        DUMP / "oredict" / "prefixes.json"
    )
    prefixes_canonical_hash = content_hash([
        prefixes[key] for key in sorted(prefixes)
    ])
    replay_counter: Counter[str] = Counter()
    local_failures = []
    for template in document.get("templates") or []:
        local_counter = _replay_shape_template(
            template,
            materials,
            prefix_sets,
        )
        replay_counter.update(local_counter)
        if multiset_digest(local_counter) != template["source_multiset_sha256"]:
            local_failures.append(template["template_id"])
    for row in document.get("discrete_recipes") or []:
        replay_counter[row["row_id"].removeprefix("sha256:")] += int(
            row["multiplicity"]
        )
    missing = source_counter - replay_counter
    extra = replay_counter - source_counter
    return {
        "replay_verified": (
            materials_raw_hash
            == document["source"].get("materials_raw_sha256")
            and materials_canonical_hash
            == document["source"].get("materials_canonical_sha256")
            and prefixes_raw_hash
            == document["source"].get("prefixes_raw_sha256")
            and prefixes_canonical_hash
            == document["source"].get("prefixes_canonical_sha256")
            and not local_failures
            and not missing
            and not extra
        ),
        "materials_verified": (
            materials_raw_hash
            == document["source"].get("materials_raw_sha256")
            and materials_canonical_hash
            == document["source"].get("materials_canonical_sha256")
        ),
        "prefixes_verified": (
            prefixes_raw_hash
            == document["source"].get("prefixes_raw_sha256")
            and prefixes_canonical_hash
            == document["source"].get("prefixes_canonical_sha256")
        ),
        "local_failure_count": len(local_failures),
        "local_failure_sample": local_failures[:20],
        "expected_count": sum(source_counter.values()),
        "actual_count": sum(replay_counter.values()),
        "expected_multiset_sha256": multiset_digest(source_counter),
        "actual_multiset_sha256": multiset_digest(replay_counter),
        "missing_count": sum(missing.values()),
        "extra_count": sum(extra.values()),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--verify",
        action="store_true",
        help="Verify the existing artifact instead of extracting it.",
    )
    args = parser.parse_args(argv)
    if args.verify:
        missing = [
            path.relative_to(ROOT).as_posix()
            for path in (OUT, MAP_PATH, MATERIALS_PATH)
            if not path.is_file()
        ]
        if missing:
            print(
                "SKIP: full extruder replay was not executed because local raw/cache "
                f"artifacts are absent: {', '.join(missing)}. Ordinary CI instead "
                "validates the committed compact index/report, selector policy, "
                "component builder projection, and their tests. Restore "
                "gt6_dump/gt6_recipe_dump and run "
                "python tools/gt6_extruder_templates.py to perform a full replay."
            )
            return 0
        document = json.loads(OUT.read_text(encoding="utf-8"))
        result = verify_document(document)
        print(json.dumps(result, indent=2))
        return 0 if result["replay_verified"] else 2
    if not MAP_PATH.is_file() or not MATERIALS_PATH.is_file():
        print(
            "Extruder extraction requires the authoritative "
            "gt6_dump/gt6_recipe_dump map and ore-dictionary files. Restore "
            "the local dump, then rerun this command.",
            file=sys.stderr,
        )
        return 2
    document = extract()
    print(f"Wrote {OUT}")
    print(f"Wrote {SUMMARY_OUT}")
    print(f"Wrote {INDEX_OUT}")
    print("axis", document["axis_analysis"])
    print("counts", document["counts"])
    print("verification", document["verification"])
    return 0 if document["verification"]["replay_verified"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
