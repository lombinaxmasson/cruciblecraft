#!/usr/bin/env python3
"""Lossless, replay-verified template extraction for the GT6 Mixer map."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

try:
    from tools import gt6_recipe_templates as exploratory
except ModuleNotFoundError:
    import gt6_recipe_templates as exploratory


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
MAP_PATH = (
    ROOT
    / "gt6_dump/gt6_recipe_dump/maps/gt.recipe.mixer.json"
)
EXPLORATORY_REPORT = TOOLS / "gt6_recipe_templates_report.json"
SHAPE_GROUPS = TOOLS / "mixer_groups.json"
SHAPE_ANALYZER = TOOLS / "analyze_map_shape.py"
INDEX_OUTPUT = TOOLS / "gt6_mixer_templates_index.json"
MEMBERSHIP_OUTPUT = TOOLS / "gt6_mixer_templates_membership.json"
REPORT_OUTPUT = TOOLS / "gt6_mixer_templates_report.json"
MAP_ID = "gt.recipe.mixer"
REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    )


def pretty(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def compact(value: Any) -> str:
    return stable_json(value) + "\n"


def content_hash(value: Any) -> str:
    return hashlib.sha256(stable_json(value).encode("utf-8")).hexdigest()


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonicalize(value: Any) -> Any:
    if isinstance(value, dict):
        return {
            key: canonicalize(child)
            for key, child in sorted(value.items())
            if key != "displayName"
        }
    if isinstance(value, list):
        return [canonicalize(child) for child in value]
    return value


def row_digest(recipe: dict[str, Any]) -> str:
    return content_hash(canonicalize(recipe))


def multiset_digest(counter: Counter[str]) -> str:
    return content_hash(sorted(counter.items()))


def pointer(parts: list[str | int]) -> str:
    return "/" + "/".join(
        str(part).replace("~", "~0").replace("/", "~1")
        for part in parts
    )


def pointer_parts(value: str) -> list[str]:
    if not value.startswith("/"):
        raise ValueError(f"invalid JSON pointer {value}")
    return [
        part.replace("~1", "/").replace("~0", "~")
        for part in value[1:].split("/")
    ]


def get_pointer(document: Any, value: str) -> Any:
    current = document
    for raw in pointer_parts(value):
        current = current[int(raw)] if isinstance(current, list) else current[raw]
    return copy.deepcopy(current)


def set_pointer(document: Any, value: str, replacement: Any) -> None:
    parts = pointer_parts(value)
    current = document
    for raw in parts[:-1]:
        current = current[int(raw)] if isinstance(current, list) else current[raw]
    final = parts[-1]
    if isinstance(current, list):
        current[int(final)] = copy.deepcopy(replacement)
    else:
        current[final] = copy.deepcopy(replacement)


def marker(value: str) -> dict[str, str]:
    return {"$support": value}


def structure_signature(value: Any) -> Any:
    if isinstance(value, dict):
        return {
            key: structure_signature(child)
            for key, child in sorted(value.items())
        }
    if isinstance(value, list):
        return [structure_signature(child) for child in value]
    return type(value).__name__


def varying_paths(values: list[Any], parts: list[str | int] | None = None) -> list[str]:
    path = [] if parts is None else parts
    if not values:
        return []
    if all(isinstance(value, dict) for value in values):
        keys = [set(value) for value in values]
        if not all(current == keys[0] for current in keys):
            if not path:
                raise ValueError("template root keys differ")
            return [pointer(path)]
        result: list[str] = []
        for key in sorted(keys[0]):
            result.extend(varying_paths(
                [value[key] for value in values],
                [*path, key],
            ))
        return result
    if all(isinstance(value, list) for value in values):
        lengths = {len(value) for value in values}
        if len(lengths) != 1:
            if not path:
                raise ValueError("template root list lengths differ")
            return [pointer(path)]
        result = []
        for index in range(next(iter(lengths))):
            result.extend(varying_paths(
                [value[index] for value in values],
                [*path, index],
            ))
        return result
    if len({stable_json(value) for value in values}) == 1:
        return []
    if not path:
        raise ValueError("template root scalar differs")
    return [pointer(path)]


def apply_support(
    skeleton: dict[str, Any],
    binding_paths: list[str],
    values: list[Any],
) -> dict[str, Any]:
    if len(binding_paths) != len(values):
        raise ValueError("support value count differs from binding paths")
    replay = copy.deepcopy(skeleton)
    for path, value in zip(binding_paths, values):
        set_pointer(replay, path, value)
    unresolved: list[str] = []

    def scan(value: Any, path: str = "") -> None:
        if isinstance(value, dict):
            if set(value) == {"$support"}:
                unresolved.append(path or "/")
                return
            for key, child in value.items():
                scan(child, f"{path}/{key}")
        elif isinstance(value, list):
            for index, child in enumerate(value):
                scan(child, f"{path}/{index}")

    scan(replay)
    if unresolved:
        raise ValueError(f"unresolved Mixer template markers {unresolved[:5]}")
    return replay


def group_key(recipe: dict[str, Any]) -> str:
    canonical = canonicalize(recipe)
    identity = {
        "soft": exploratory.recipe_soft_key(recipe),
        "structure": structure_signature(canonical),
    }
    return content_hash(identity)


def build_template(
    members: list[tuple[int, dict[str, Any]]],
) -> dict[str, Any]:
    canonical_members = [
        (index, canonicalize(recipe)) for index, recipe in members
    ]
    documents = [recipe for _index, recipe in canonical_members]
    binding_paths = sorted(varying_paths(documents))
    skeleton = copy.deepcopy(documents[0])
    for path in binding_paths:
        set_pointer(skeleton, path, marker(path))
    supports = []
    replay_counter: Counter[str] = Counter()
    source_counter: Counter[str] = Counter()
    for index, recipe in canonical_members:
        values = [get_pointer(recipe, path) for path in binding_paths]
        digest = content_hash(recipe)
        replay = apply_support(skeleton, binding_paths, values)
        replay_digest = content_hash(replay)
        if replay_digest != digest:
            raise ValueError(
                f"Mixer template local replay differs at source row {index}"
            )
        supports.append([index, values, digest])
        source_counter[digest] += 1
        replay_counter[replay_digest] += 1
    source_multiset = multiset_digest(source_counter)
    identity = {
        "map": MAP_ID,
        "skeleton": skeleton,
        "binding_paths": binding_paths,
        "source_multiset_sha256": source_multiset,
    }
    template_id = f"sha256:{content_hash(identity)}"
    missing = source_counter - replay_counter
    extra = replay_counter - source_counter
    return {
        "template_id": template_id,
        "skeleton": skeleton,
        "binding_paths": binding_paths,
        "support_kind": "explicit_sparse_multiset",
        "supports": supports,
        "source_count": len(members),
        "source_multiset_sha256": source_multiset,
        "replay": {
            "expected_count": sum(source_counter.values()),
            "actual_count": sum(replay_counter.values()),
            "missing_count": sum(missing.values()),
            "extra_count": sum(extra.values()),
            "replay_verified": not missing and not extra,
        },
    }


def build_documents() -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    source = load(MAP_PATH)
    recipes = source.get("recipes")
    if (
        source.get("nameInternal") != MAP_ID
        or source.get("recipeCount") != 64_245
        or not isinstance(recipes, list)
        or len(recipes) != 64_245
    ):
        raise ValueError("fixed Mixer source map drifted")
    shape_analysis = load(SHAPE_GROUPS)
    if (
        shape_analysis.get("logical_row") != 64_245
        or shape_analysis.get("rows_in_groups")
        + shape_analysis.get("residual_named_rows") != 64_245
    ):
        raise ValueError("Mixer shape analysis does not partition the source")
    templates = []
    assigned: set[int] = set()
    shape_rows: Counter[str] = Counter()
    shape_templates: Counter[str] = Counter()
    for group_index, group in enumerate(shape_analysis["groups"]):
        source_rows = list(group.get("source_rows") or [])
        if (
            len(source_rows) != group["size"]
            or any(index in assigned for index in source_rows)
        ):
            raise ValueError(
                f"Mixer shape group {group_index} overlaps or drifts"
            )
        assigned.update(source_rows)
        template = build_template([
            (index, recipes[index]) for index in source_rows
        ])
        template["shape"] = group["shape"]
        template["analysis_group_index"] = group_index
        templates.append(template)
        shape_templates[group["shape"]] += 1
        shape_rows[group["shape"]] += len(source_rows)
    for index, recipe in enumerate(recipes):
        if index in assigned:
            continue
        template = build_template([(index, recipe)])
        template["shape"] = "opaque"
        template["analysis_group_index"] = None
        templates.append(template)
        shape_templates["opaque"] += 1
        shape_rows["opaque"] += 1
    templates.sort(key=lambda row: row["template_id"])
    membership = [-1] * len(recipes)
    template_ids = []
    replay_counter: Counter[str] = Counter()
    for template_index, template in enumerate(templates):
        template_ids.append(template["template_id"])
        if not template["replay"]["replay_verified"]:
            raise ValueError(
                f"local replay failed for {template['template_id']}"
            )
        for recipe_index, values, digest in template["supports"]:
            if membership[recipe_index] != -1:
                raise ValueError(
                    f"Mixer source row {recipe_index} has multiple templates"
                )
            membership[recipe_index] = template_index
            replay = apply_support(
                template["skeleton"],
                template["binding_paths"],
                values,
            )
            actual = content_hash(replay)
            if actual != digest:
                raise ValueError(
                    f"Mixer membership replay differs at {recipe_index}"
                )
            replay_counter[actual] += 1
    if any(value < 0 for value in membership):
        raise ValueError("Mixer membership leaves source rows unassigned")
    source_counter = Counter(row_digest(recipe) for recipe in recipes)
    missing = source_counter - replay_counter
    extra = replay_counter - source_counter
    source_multiset = multiset_digest(source_counter)
    replay_multiset = multiset_digest(replay_counter)
    index_document = {
        "schema_version": 1,
        "status": "GT6_MIXER_TEMPLATES_READY",
        "source": {
            "repository": "GregTech6/gregtech6",
            "revision": REVISION,
            "map": MAP_ID,
            "path": "gt6_dump/gt6_recipe_dump/maps/gt.recipe.mixer.json",
            "sha256": file_sha256(MAP_PATH),
            "recipe_count": len(recipes),
            "canonical_multiset_sha256": source_multiset,
            "shape_analysis": "tools/mixer_groups.json",
            "shape_analysis_sha256": file_sha256(SHAPE_GROUPS),
            "shape_analyzer_sha256": file_sha256(SHAPE_ANALYZER),
        },
        "templates": templates,
        "verification": {
            "expected_count": sum(source_counter.values()),
            "actual_count": sum(replay_counter.values()),
            "expected_multiset_sha256": source_multiset,
            "actual_multiset_sha256": replay_multiset,
            "missing_count": sum(missing.values()),
            "extra_count": sum(extra.values()),
            "membership_unassigned": membership.count(-1),
            "membership_duplicate": 0,
            "replay_verified": not missing and not extra,
        },
    }
    membership_document = {
        "schema_version": 1,
        "status": "GT6_MIXER_MEMBERSHIP_READY",
        "source_recipe_count": len(recipes),
        "template_ids": template_ids,
        "membership": membership,
        "membership_sha256": content_hash(membership),
    }
    exploratory_report = load(EXPLORATORY_REPORT)
    mixer_exploratory = exploratory_report["maps"][MAP_ID]
    sizes = sorted(
        (template["source_count"] for template in templates),
        reverse=True,
    )
    report_document = {
        "schema_version": 1,
        "status": "GT6_MIXER_TEMPLATE_REPORT_READY",
        "source_recipe_count": len(recipes),
        "template_count": len(templates),
        "singleton_templates": sum(size == 1 for size in sizes),
        "multirow_templates": sum(size > 1 for size in sizes),
        "largest_template_sizes": sizes[:20],
        "exploratory_template_count": mixer_exploratory[
            "template_count"
        ],
        "exploratory_largest_template_sizes": [
            row["expanded_count"]
            for row in mixer_exploratory["largest_templates"][:10]
        ],
        "support_kind": "explicit_sparse_multiset",
        "shape_templates": dict(sorted(shape_templates.items())),
        "shape_rows": dict(sorted(shape_rows.items())),
        "authored_rule_estimate": shape_analysis[
            "authored_rule_estimate"
        ],
        "verification": index_document["verification"],
    }
    return index_document, membership_document, report_document


def validate_compact(
    index_document: dict[str, Any],
    membership_document: dict[str, Any],
    report_document: dict[str, Any],
) -> None:
    if (
        index_document.get("status") != "GT6_MIXER_TEMPLATES_READY"
        or membership_document.get("status")
        != "GT6_MIXER_MEMBERSHIP_READY"
        or report_document.get("status")
        != "GT6_MIXER_TEMPLATE_REPORT_READY"
        or index_document["source"]["recipe_count"] != 64_245
        or membership_document["source_recipe_count"] != 64_245
        or len(membership_document["membership"]) != 64_245
        or index_document["verification"]["missing_count"] != 0
        or index_document["verification"]["extra_count"] != 0
        or not index_document["verification"]["replay_verified"]
        or report_document["verification"]
        != index_document["verification"]
    ):
        raise ValueError("committed Mixer template evidence drifted")
    templates = index_document["templates"]
    if len(templates) != len(membership_document["template_ids"]):
        raise ValueError("Mixer template/membership cardinality drifted")
    if (
        [row["template_id"] for row in templates]
        != membership_document["template_ids"]
    ):
        raise ValueError("Mixer template id dictionary drifted")
    if (
        content_hash(membership_document["membership"])
        != membership_document["membership_sha256"]
    ):
        raise ValueError("Mixer membership digest drifted")


def check_outputs(
    expected: tuple[dict[str, Any], dict[str, Any], dict[str, Any]] | None,
) -> list[str]:
    paths = (INDEX_OUTPUT, MEMBERSHIP_OUTPUT, REPORT_OUTPUT)
    if any(not path.is_file() for path in paths):
        return [
            f"missing {path.relative_to(ROOT).as_posix()}"
            for path in paths
            if not path.is_file()
        ]
    documents = tuple(load(path) for path in paths)
    validate_compact(*documents)
    if expected is None:
        return []
    errors = []
    encoders = (compact, compact, pretty)
    for path, actual, document, encode in zip(
        paths, documents, expected, encoders
    ):
        if actual != document or path.read_text(encoding="utf-8") != encode(document):
            errors.append(path.relative_to(ROOT).as_posix())
    return errors


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
        expected = build_documents() if args.full_replay else None
        if args.write:
            assert expected is not None
            INDEX_OUTPUT.write_text(
                compact(expected[0]), encoding="utf-8", newline="\n"
            )
            MEMBERSHIP_OUTPUT.write_text(
                compact(expected[1]), encoding="utf-8", newline="\n"
            )
            REPORT_OUTPUT.write_text(
                pretty(expected[2]), encoding="utf-8", newline="\n"
            )
            print(
                f"Wrote {expected[2]['template_count']} replay-verified "
                "Mixer templates for 64,245 rows."
            )
            return 0
        errors = check_outputs(expected)
        if errors:
            print(
                "Mixer template artifacts are stale:\n"
                + "\n".join(f"- {error}" for error in errors)
            )
            return 1
        tier = "full replay" if args.full_replay else "compact"
        print(f"Mixer template {tier} evidence is current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"Mixer template build failed: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
