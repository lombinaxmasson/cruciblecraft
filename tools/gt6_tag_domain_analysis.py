"""Analyze GT6 material tags as recipe and item-generation domains."""

from __future__ import annotations

import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

import gt6_extruder_templates as extruder

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "tools" / "gt6_tag_domain_report.json"


def histogram(values: list[int]) -> dict[str, int]:
    return {
        str(key): value
        for key, value in sorted(Counter(values).items())
    }


def best_domain(
    actual: frozenset[int],
    forms: tuple[str, ...],
    prefix_sets: dict[str, frozenset[int]],
    processing_sets: dict[str, frozenset[int]],
) -> dict[str, Any]:
    universe: frozenset[int] | None = None
    for form in forms:
        registered = prefix_sets.get(form, frozenset())
        universe = registered if universe is None else universe & registered
    universe = universe or frozenset()
    candidates = []
    processing_candidates = [("FORM_ONLY", universe), *[
        (tag, universe & members)
        for tag, members in processing_sets.items()
    ]]
    if {
        "PROCESSING.EXTRUDABLE",
        "PROCESSING.EXTRUDABLE_SIMPLE",
    } <= set(processing_sets):
        processing_candidates.append((
            (
                "PROCESSING.EXTRUDABLE"
                " - PROCESSING.EXTRUDABLE_SIMPLE"
            ),
            (
                (
                    universe
                    & processing_sets["PROCESSING.EXTRUDABLE"]
                )
                - processing_sets["PROCESSING.EXTRUDABLE_SIMPLE"]
            ),
        ))
    for tag, tagged in processing_candidates:
        include = actual - tagged
        exclude = tagged - actual
        candidates.append((
            len(include) + len(exclude),
            len(include),
            len(exclude),
            tag,
        ))
    difference, include_count, exclude_count, tag = min(candidates)
    return {
        "predicate": tag,
        "symmetric_difference": difference,
        "include_count": include_count,
        "exclude_count": exclude_count,
    }


def main() -> int:
    materials = json.loads(extruder.MATERIALS_PATH.read_text(encoding="utf-8"))
    prefixes = json.loads(
        (extruder.DUMP / "oredict" / "prefixes.json").read_text(
            encoding="utf-8"
        )
    )
    recipes = json.loads(extruder.MAP_PATH.read_text(encoding="utf-8"))[
        "recipes"
    ]
    by_name = {
        row["nameInternal"]: row["id"]
        for row in materials
        if isinstance(row.get("id"), int) and int(row["id"]) >= 0
    }
    namespace_rows: dict[str, dict[str, Any]] = defaultdict(
        lambda: {
            "tags": set(),
            "materials": set(),
            "records": set(),
            "memberships": 0,
        }
    )
    all_tags = set()
    for material in materials:
        for tag in material.get("tags") or []:
            namespace = tag.split(".", 1)[0] if "." in tag else "<UNNAMESPACED>"
            row = namespace_rows[namespace]
            row["tags"].add(tag)
            if (
                isinstance(material.get("id"), int)
                and int(material["id"]) >= 0
            ):
                row["materials"].add(int(material["id"]))
            row["records"].add(material.get("nameInternal"))
            row["memberships"] += 1
            all_tags.add(tag)
    tag_sets = {
        tag: frozenset(
            row["id"]
            for row in materials
            if isinstance(row.get("id"), int)
            and int(row["id"]) >= 0
            and tag in (row.get("tags") or [])
        )
        for tag in sorted(all_tags)
    }
    processing_sets = {
        tag: members
        for tag, members in tag_sets.items()
        if tag.startswith("PROCESSING.")
    }
    itemgenerator_sets = {
        tag: members
        for tag, members in tag_sets.items()
        if tag.startswith("ITEMGENERATOR.")
    }
    prefix_sets = {
        prefix["nameInternal"]: frozenset(
            by_name[name]
            for name in prefix.get("registeredMaterials") or []
            if name in by_name
        )
        for prefix in prefixes
        if prefix.get("registeredMaterials")
    }

    itemgenerator_matches = []
    for tag, members in itemgenerator_sets.items():
        candidates = sorted(
            (
                len(members ^ registered),
                len(members - registered),
                len(registered - members),
                prefix,
                len(registered),
            )
            for prefix, registered in prefix_sets.items()
        )
        exact = [row[3] for row in candidates if row[0] == 0]
        itemgenerator_matches.append({
            "tag": tag,
            "material_count": len(members),
            "exact_prefixes": exact,
            "nearest_prefixes": [
                {
                    "prefix": row[3],
                    "prefix_material_count": row[4],
                    "symmetric_difference": row[0],
                    "tag_only": row[1],
                    "prefix_only": row[2],
                }
                for row in candidates[:5]
            ],
        })

    material_groups: dict[str, list[Any]] = defaultdict(list)
    for recipe in recipes:
        if not extruder.pure_gt_material_recipe(recipe):
            continue
        skeleton, patches, metadata = extruder.material_candidate(recipe)
        subject = [row for row in patches if row["path"] != "/duration"]
        material_groups[extruder.stable_json(skeleton)].append(
            (subject, metadata)
        )
    legacy_templates = []
    for key, members in material_groups.items():
        if (
            len(members) < extruder.MIN_DEPENDENCY_SAMPLE
            or len({
                extruder.stable_json(row[0]) for row in members
            }) < extruder.MIN_DEPENDENCY_SAMPLE
        ):
            continue
        skeleton = json.loads(key)
        actual = frozenset(
            int(binding["value"])
            for subject, _metadata in members
            for binding in subject
            if binding["path"].startswith("/inputs/")
        )
        forms = tuple(
            str(stack.get("item"))[len(extruder.META_PREFIX):]
            for side in ("inputs", "outputs")
            for stack in skeleton.get(side) or []
            if isinstance(stack, dict)
            and str(stack.get("item") or "").startswith(extruder.META_PREFIX)
        )
        legacy_templates.append({
            "expanded_count": len(members),
            "domain_size": len(actual),
            "domain": best_domain(
                actual,
                forms,
                prefix_sets,
                processing_sets,
            ),
        })

    operation_groups: dict[tuple[Any, ...], list[tuple[int, int, int]]] = (
        defaultdict(list)
    )
    for recipe in recipes:
        if not extruder.pure_gt_material_recipe(recipe):
            continue
        shape_index = extruder.shape_indices(recipe)[0]
        shape = int(recipe["inputs"][shape_index]["meta"])
        inputs = tuple(
            (stack["item"], int(stack.get("count") or 0))
            for stack in recipe["inputs"]
            if stack and not extruder.is_shape_stack(stack)
        )
        outputs = tuple(
            (stack["item"], int(stack.get("count") or 0))
            for stack in recipe.get("outputs") or []
            if stack
        )
        material = next(
            int(stack["meta"])
            for stack in recipe["inputs"]
            if stack and not extruder.is_shape_stack(stack)
        )
        operation_groups[(shape, inputs, outputs)].append((
            material,
            int(recipe["duration"]),
            int(recipe["euPerTick"]),
        ))
    constant_domains = []
    for (_shape, inputs, outputs), rows in operation_groups.items():
        actual = frozenset(row[0] for row in rows)
        if len(actual) < 2 or len({row[1:] for row in rows}) != 1:
            continue
        forms = tuple(
            item[len(extruder.META_PREFIX):]
            for item, _count in (*inputs, *outputs)
            if item.startswith(extruder.META_PREFIX)
        )
        constant_domains.append({
            "expanded_count": len(rows),
            "domain_size": len(actual),
            "domain": best_domain(
                actual,
                forms,
                prefix_sets,
                processing_sets,
            ),
        })

    report = {
        "materials": {
            "record_count": len(materials),
            "distinct_nonnegative_id_count": len({
                int(row["id"])
                for row in materials
                if isinstance(row.get("id"), int)
                and int(row["id"]) >= 0
            }),
            "sentinel_id_record_count": sum(
                row.get("id") == -1 for row in materials
            ),
            "tagged_count": sum(bool(row.get("tags")) for row in materials),
            "distinct_tag_count": len(all_tags),
        },
        "namespaces": {
            namespace: {
                "distinct_tag_count": len(row["tags"]),
                "materials_covered": len(row["materials"]),
                "records_covered": len(row["records"]),
                "id_membership_count": sum(
                    len(tag_sets[tag]) for tag in row["tags"]
                ),
                "record_membership_count": row["memberships"],
            }
            for namespace, row in sorted(namespace_rows.items())
        },
        "tags": {
            namespace: [
                {
                    "tag": tag,
                    "material_count": len(tag_sets[tag]),
                }
                for tag in sorted(namespace_rows[namespace]["tags"])
            ]
            for namespace in sorted(namespace_rows)
        },
        "itemgenerator_prefix_matches": {
            "tag_count": len(itemgenerator_matches),
            "tags_with_exact_prefix_match": sum(
                bool(row["exact_prefixes"])
                for row in itemgenerator_matches
            ),
            "records": itemgenerator_matches,
        },
        "legacy_extruder_material_templates": {
            "template_count": len(legacy_templates),
            "note": (
                "expanded_count is not domain cardinality; confusing these "
                "produced the apparent 176/235 zero-exception anchors"
            ),
            "expanded_count_histogram": histogram([
                row["expanded_count"] for row in legacy_templates
            ]),
            "domain_size_histogram": histogram([
                row["domain_size"] for row in legacy_templates
            ]),
            "exact_tag_and_form_domain_count": sum(
                row["domain"]["symmetric_difference"] == 0
                for row in legacy_templates
            ),
            "within_one_exception_count": sum(
                row["domain"]["symmetric_difference"] <= 1
                for row in legacy_templates
            ),
            "difference_histogram": histogram([
                row["domain"]["symmetric_difference"]
                for row in legacy_templates
            ]),
        },
        "constant_extruder_operations": {
            "group_count": len(constant_domains),
            "expanded_recipe_count": sum(
                row["expanded_count"] for row in constant_domains
            ),
            "exact_tag_and_form_domain_count": sum(
                row["domain"]["symmetric_difference"] == 0
                for row in constant_domains
            ),
            "exact_domain_recipe_count": sum(
                row["expanded_count"]
                for row in constant_domains
                if row["domain"]["symmetric_difference"] == 0
            ),
            "within_one_exception_count": sum(
                row["domain"]["symmetric_difference"] <= 1
                for row in constant_domains
            ),
            "difference_histogram": histogram([
                row["domain"]["symmetric_difference"]
                for row in constant_domains
            ]),
        },
    }
    OUT.write_text(
        json.dumps(report, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print(f"Wrote {OUT}")
    print(json.dumps({
        "materials": report["materials"],
        "namespaces": report["namespaces"],
        "itemgenerator_prefix_matches": {
            key: value
            for key, value in report["itemgenerator_prefix_matches"].items()
            if key != "records"
        },
        "legacy_extruder_material_templates": {
            key: value
            for key, value in report[
                "legacy_extruder_material_templates"
            ].items()
            if not key.endswith("histogram")
        },
        "constant_extruder_operations": {
            key: value
            for key, value in report["constant_extruder_operations"].items()
            if not key.endswith("histogram")
        },
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
