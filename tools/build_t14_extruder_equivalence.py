#!/usr/bin/env python3
"""Build the independent T14a Extruder truth set and compact live relation table."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
POLICY = ROOT / "tools" / "t14_extruder_policy.json"
SOURCE = ROOT / "tools" / "component_rule_sources" / "extruder_shapes.json"
INDEX = ROOT / "tools" / "gt6_extruder_templates_index_v5.json"
REPORT = ROOT / "tools" / "gt6_extruder_templates_report.json"
MATERIAL_DIR = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
REGISTRATION_GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
T8_READINESS = ROOT / "tools" / "t8_pipe_readiness.json"
LEGACY_RUNTIME_ROOT = (
    ROOT
    / "src"
    / "component_rule_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "extruder"
)
EXPECTED = ROOT / "tools" / "t14_extruder_expected.json"
COMPACT = ROOT / "tools" / "t14_extruder_compact.json"
READINESS = ROOT / "tools" / "t14_extruder_readiness.json"
LEGACY_REPLAY = ROOT / "tools" / "t14_extruder_legacy_replay.json"
LOCKED_FIELDS = (
    "stable_id",
    "material",
    "shape",
    "input",
    "output",
    "duration",
    "eut",
    "fallback",
    "forging_target",
    "plateGem",
    "heat_mode",
    "shadow_order",
)


class EquivalenceError(ValueError):
    """T14a input or equivalence failure."""


@dataclass(frozen=True)
class ArtifactBundle:
    expected: bytes
    compact: bytes
    readiness: bytes
    legacy_replay: bytes


def read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        raise EquivalenceError(f"{path}: cannot read valid JSON: {exc}") from exc
    if not isinstance(value, dict):
        raise EquivalenceError(f"{path}: root must be an object")
    return value


def stable_bytes(value: Any) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_json(value: Any) -> str:
    return sha256_bytes(stable_bytes(value))


def strip_prefix_namespace(value: str) -> str:
    prefix = "cruciblecraft:"
    return value[len(prefix):] if value.startswith(prefix) else value


def material_facts() -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for path in sorted(MATERIAL_DIR.glob("*.json")):
        if path.name == "index.json":
            continue
        document = read_json(path)
        material = document.get("id")
        if not isinstance(material, str):
            raise EquivalenceError(f"{path}: missing material id")
        metadata = document.get("gt6_metadata") or {}
        forging = (metadata.get("processing_targets") or {}).get("forging")
        forging_target = forging.get("material") if isinstance(forging, dict) else None
        result[material] = {
            "forging_target": forging_target,
            "plate_gem": (
                "cruciblecraft:generates_plate_gem"
                in document.get("generation_flags", [])
            ),
        }
    return result


def capture_legacy_replay() -> dict[str, Any]:
    files = sorted(LEGACY_RUNTIME_ROOT.glob("*/*.json"))
    if len(files) != 2782:
        if LEGACY_REPLAY.is_file():
            replay = read_json(LEGACY_REPLAY)
            if replay.get("captured_rule_count") != 2782:
                raise EquivalenceError(
                    f"{LEGACY_REPLAY}: expected 2782 captured legacy rules"
                )
            return replay
        raise EquivalenceError(
            "cannot bootstrap verification-only legacy replay: expected 2782 "
            f"two-level sparse files under {LEGACY_RUNTIME_ROOT}, found {len(files)}"
        )
    rows: list[dict[str, Any]] = []
    for path in files:
        relative = path.relative_to(
            LEGACY_RUNTIME_ROOT.parent
        ).as_posix()
        raw = path.read_bytes()
        rows.append({
            "path": relative,
            "sha256": sha256_bytes(raw),
            "recipe": json.loads(raw),
        })
    return {
        "schema_version": 1,
        "delivery": "verification_only_not_a_runtime_datapack",
        "captured_from": LEGACY_RUNTIME_ROOT.relative_to(ROOT).as_posix(),
        "captured_rule_count": len(rows),
        "files": rows,
        "files_fingerprint": sha256_json(rows),
    }


def validate_v5_evidence(
    source: dict[str, Any],
    index: dict[str, Any],
    report: dict[str, Any],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    if (
        index.get("schema_version") != 5
        or index.get("functional_template_count") != 62
        or index.get("exact_remainder_count") != 1
        or not index.get("replay_verified")
    ):
        raise EquivalenceError("v5 index is not the replay-verified 62+1 partition")
    if (
        report.get("schema_version") != 5
        or report.get("counts", {}).get("shape_template_count") != 62
        or report.get("counts", {}).get("discrete_recipe_rows") != 1
        or not report.get("verification", {}).get("replay_verified")
    ):
        raise EquivalenceError("v5 report is not replay verified")
    classified = source.get("templates")
    if not isinstance(classified, list) or len(classified) != 62:
        raise EquivalenceError("sparse source must classify all 62 templates")
    counts = Counter(row.get("classification") for row in classified)
    if counts != {"playable": 20, "skipped": 42}:
        raise EquivalenceError(f"template classification drift: {dict(counts)}")
    indexed = {
        (row["template_id"], row["shape_meta"], row["heat_mode"])
        for row in index["templates"]
    }
    authored = {
        (row["template_id"], row["shape_meta"], row["heat_mode"])
        for row in classified
    }
    if authored != indexed:
        raise EquivalenceError("source classifications do not exactly partition v5 index")
    remainder = index.get("exact_remainder")
    if (
        not isinstance(remainder, list)
        or len(remainder) != 1
        or remainder[0].get("classification") != "discrete_recipe"
        or remainder[0].get("multiplicity") != 1
    ):
        raise EquivalenceError("v5 exact remainder classification drift")
    return classified, remainder


def replay_by_path(replay: dict[str, Any]) -> dict[str, dict[str, Any]]:
    files = replay.get("files")
    if not isinstance(files, list) or len(files) != 2782:
        raise EquivalenceError("legacy replay must contain exactly 2782 files")
    result: dict[str, dict[str, Any]] = {}
    for entry in files:
        path = entry.get("path")
        recipe = entry.get("recipe")
        if (
            not isinstance(path, str)
            or not isinstance(recipe, dict)
            or sha256_json(recipe) == ""
        ):
            raise EquivalenceError("invalid legacy replay row")
        if path in result:
            raise EquivalenceError(f"duplicate legacy replay path: {path}")
        result[path] = recipe
    return result


def row_metadata(
    recipe: dict[str, Any],
    facts: dict[str, dict[str, Any]],
    shadow_order: int,
) -> dict[str, Any]:
    material = recipe["material"]
    fact = facts.get(material)
    if fact is None:
        raise EquivalenceError(f"missing material facts for {material}")
    return {
        "stable_id": (
            f"cruciblecraft:extruder/{recipe['shape']}/{material}/{material}"
        ),
        "material": material,
        "shape": recipe["shape"],
        "shape_meta": recipe["shape_meta"],
        "template_id": recipe["template_id"],
        "input": {
            "prefix": recipe["input"]["prefix"],
            "count": recipe["input"]["count"],
        },
        "output": {
            "prefix": recipe["output"]["prefix"],
            "count": recipe["output"]["count"],
        },
        "duration": recipe["duration"],
        "eut": recipe["eut"],
        "fallback": (
            "dust" if recipe["input"]["prefix"] == "dust" else "none"
        ),
        "forging_target": fact["forging_target"],
        "plateGem": fact["plate_gem"],
        "heat_mode": recipe["heat_mode"],
        "shadow_order": shadow_order,
        "provenance": recipe["provenance"],
    }


def assert_legacy_recipe(
    source_row: dict[str, Any],
    legacy: dict[str, Any],
) -> None:
    expected_path = f"extruder/{source_row['shape']}/{source_row['material']}.json"
    if legacy.get("type") != "cruciblecraft:material_rule":
        raise EquivalenceError(f"{expected_path}: wrong legacy recipe type")
    if legacy.get("target") != "cruciblecraft:extruder":
        raise EquivalenceError(f"{expected_path}: wrong legacy target")
    if legacy.get("material") != source_row["material"]:
        raise EquivalenceError(f"{expected_path}: legacy material drift")
    inputs = legacy.get("item_inputs")
    outputs = legacy.get("item_outputs")
    if (
        not isinstance(inputs, list)
        or len(inputs) != 2
        or not isinstance(outputs, list)
        or len(outputs) != 1
    ):
        raise EquivalenceError(f"{expected_path}: legacy IO arity drift")
    actual = {
        "input": {
            "prefix": strip_prefix_namespace(inputs[0].get("prefix", "")),
            "count": int(inputs[0].get("count", "1")),
        },
        "output": {
            "prefix": strip_prefix_namespace(outputs[0].get("prefix", "")),
            "count": int(outputs[0].get("count", "1")),
        },
        "duration": int(legacy.get("duration", "0")),
        "eut": int(legacy.get("eut", "0")),
        "shape_item": inputs[1].get("item"),
        "shape_count": int(inputs[1].get("count", "-1")),
    }
    expected = {
        "input": source_row["input"],
        "output": source_row["output"],
        "duration": source_row["duration"],
        "eut": source_row["eut"],
        "shape_item": f"cruciblecraft:extruder_shape_{source_row['shape']}",
        "shape_count": 0,
    }
    if actual != expected:
        raise EquivalenceError(
            f"{expected_path}: legacy/source drift: {actual!r} != {expected!r}"
        )


def build_expected_independently(
    source: dict[str, Any],
    replay: dict[str, Any],
    facts: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    """Parse the old sparse files directly; never call the compact selector."""
    source_rows = source.get("recipes")
    if not isinstance(source_rows, list) or len(source_rows) != 2782:
        raise EquivalenceError("sparse source must contain 2782 expected rows")
    legacy = replay_by_path(replay)
    by_id: dict[str, dict[str, Any]] = {}
    for row in source_rows:
        stable_id = (
            f"cruciblecraft:extruder/{row['shape']}/"
            f"{row['material']}/{row['material']}"
        )
        if stable_id in by_id:
            raise EquivalenceError(f"duplicate source stable id: {stable_id}")
        by_id[stable_id] = row
    ordered_ids = sorted(by_id)
    relations: list[dict[str, Any]] = []
    for shadow_order, stable_id in enumerate(ordered_ids):
        row = by_id[stable_id]
        path = (
            f"extruder/{row['shape']}/{row['material']}.json"
        )
        legacy_recipe = legacy.get(path)
        if legacy_recipe is None:
            raise EquivalenceError(f"missing legacy replay recipe: {path}")
        assert_legacy_recipe(row, legacy_recipe)
        relations.append(row_metadata(row, facts, shadow_order))
    expected_paths = {
        f"extruder/{row['shape']}/{row['material']}.json"
        for row in source_rows
    }
    if set(legacy) != expected_paths:
        raise EquivalenceError(
            "legacy replay is not bidirectional with sparse expected source"
        )
    return {
        "schema_version": 1,
        "family": "extruder",
        "delivery": "verification_only_expected_set",
        "selector": "independent_legacy_sparse_parser",
        "locked_fields": list(LOCKED_FIELDS),
        "relation_count": len(relations),
        "relations": relations,
        "relation_fingerprint": sha256_json(relations),
    }


def build_compact_production_input(
    source: dict[str, Any],
    classifications: list[dict[str, Any]],
    facts: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    """Group exact source rows by template without using the expected parser."""
    recipes = source.get("recipes")
    if not isinstance(recipes, list) or len(recipes) != 2782:
        raise EquivalenceError("compact builder requires all 2782 source rows")
    stable_ids = sorted(
        f"cruciblecraft:extruder/{row['shape']}/"
        f"{row['material']}/{row['material']}"
        for row in recipes
    )
    if len(set(stable_ids)) != 2782:
        raise EquivalenceError("compact builder found duplicate stable ids")
    order = {stable_id: index for index, stable_id in enumerate(stable_ids)}
    groups: dict[tuple[str, int, str], list[dict[str, Any]]] = defaultdict(list)
    for row in recipes:
        key = (row["template_id"], row["shape_meta"], row["heat_mode"])
        groups[key].append(row_metadata(
            row,
            facts,
            order[
                f"cruciblecraft:extruder/{row['shape']}/"
                f"{row['material']}/{row['material']}"
            ],
        ))
    playable = {
        (row["template_id"], row["shape_meta"], row["heat_mode"]): row
        for row in classifications
        if row["classification"] == "playable"
    }
    if set(groups) != set(playable):
        raise EquivalenceError("compact relation groups differ from playable templates")
    templates: list[dict[str, Any]] = []
    for key in sorted(groups, key=lambda value: (value[1], value[2], value[0])):
        rows = sorted(groups[key], key=lambda row: (
            row["shadow_order"], row["stable_id"]
        ))
        shape = rows[0]["shape"]
        heat_mode = rows[0]["heat_mode"]
        authored_path = f"extruder/compact/{heat_mode}_{shape}"
        if any(
            row["shape"] != shape or row["heat_mode"] != heat_mode
            for row in rows
        ):
            raise EquivalenceError(f"{authored_path}: mixed compact family")
        templates.append({
            "authored_id": f"cruciblecraft:{authored_path}",
            "template_id": key[0],
            "shape_meta": key[1],
            "shape": shape,
            "shape_item": f"cruciblecraft:extruder_shape_{shape}",
            "heat_mode": heat_mode,
            "relation_count": len(rows),
            "relations": rows,
        })
    return {
        "schema_version": 1,
        "family": "extruder",
        "runtime_type": "cruciblecraft:material_rule",
        "selector": "template_grouped_exact_relation_builder",
        "authored_entry_count": len(templates),
        "logical_relation_count": sum(
            template["relation_count"] for template in templates
        ),
        "templates": templates,
        "compact_fingerprint": sha256_json(templates),
    }


def compact_relations(compact: dict[str, Any]) -> list[dict[str, Any]]:
    templates = compact.get("templates")
    if not isinstance(templates, list):
        raise EquivalenceError("compact templates must be a list")
    return sorted(
        [
            relation
            for template in templates
            for relation in template.get("relations", [])
        ],
        key=lambda row: (row["shadow_order"], row["stable_id"]),
    )


def verify_equivalence(
    expected: dict[str, Any],
    compact: dict[str, Any],
) -> None:
    expected_rows = expected.get("relations")
    actual_rows = compact_relations(compact)
    if not isinstance(expected_rows, list):
        raise EquivalenceError("expected relations must be a list")
    expected_by_id = {row["stable_id"]: row for row in expected_rows}
    actual_by_id = {row["stable_id"]: row for row in actual_rows}
    if len(expected_by_id) != len(expected_rows):
        raise EquivalenceError("expected set has duplicate stable ids")
    if len(actual_by_id) != len(actual_rows):
        raise EquivalenceError("compact set has duplicate stable ids")
    missing = sorted(expected_by_id.keys() - actual_by_id.keys())
    extra = sorted(actual_by_id.keys() - expected_by_id.keys())
    if missing or extra:
        raise EquivalenceError(
            f"bidirectional stable-id mismatch: missing={missing[:3]}, extra={extra[:3]}"
        )
    for stable_id in sorted(expected_by_id):
        left = expected_by_id[stable_id]
        right = actual_by_id[stable_id]
        for field in LOCKED_FIELDS:
            if left.get(field) != right.get(field):
                raise EquivalenceError(
                    f"{stable_id}: locked field {field} drift: "
                    f"{left.get(field)!r} != {right.get(field)!r}"
                )
        if left != right:
            raise EquivalenceError(f"{stable_id}: unlocked provenance drift")
    shadows: dict[tuple[Any, ...], str] = {}
    for row in actual_rows:
        signature = (
            row["material"],
            row["shape"],
            row["input"]["prefix"],
            row["input"]["count"],
        )
        previous = shadows.setdefault(signature, row["stable_id"])
        if previous != row["stable_id"]:
            raise EquivalenceError(
                f"ambiguous shadow {signature}: {previous}, {row['stable_id']}"
            )
    expected_orders = list(range(len(expected_rows)))
    actual_orders = [row["shadow_order"] for row in actual_rows]
    if actual_orders != expected_orders:
        raise EquivalenceError("shadow_order must be an exact contiguous total order")


def build_bundle() -> ArtifactBundle:
    policy = read_json(POLICY)
    source = read_json(SOURCE)
    index = read_json(INDEX)
    report = read_json(REPORT)
    replay = capture_legacy_replay()
    facts = material_facts()
    classifications, remainder = validate_v5_evidence(source, index, report)
    expected = build_expected_independently(source, replay, facts)
    compact = build_compact_production_input(source, classifications, facts)
    verify_equivalence(expected, compact)
    gate = read_json(REGISTRATION_GATE)
    registered = gate.get("materials")
    if not isinstance(registered, dict):
        raise EquivalenceError("material registration gate has no materials object")
    for row in expected["relations"]:
        forms = registered.get(row["material"], [])
        if (
            row["input"]["prefix"] not in forms
            or row["output"]["prefix"] not in forms
        ):
            raise EquivalenceError(
                f"{row['stable_id']}: expected relation is outside registration gate"
            )
    t8 = read_json(T8_READINESS)
    t8_count = t8.get("recipe_projection", {}).get("material_expansion_count")
    if t8_count != 257:
        raise EquivalenceError(f"T8 pipe domain drifted: {t8_count}")
    skipped = [
        copy.deepcopy(row)
        for row in classifications
        if row["classification"] == "skipped"
    ]
    readiness = {
        "schema_version": 1,
        "gate": "T14A_EXTRUDER_EQUIVALENCE",
        "status": "READY",
        "scope": {
            "authored_entries": compact["authored_entry_count"],
            "logical_relations": compact["logical_relation_count"],
            "runtime_publication": compact["logical_relation_count"],
            "skipped_templates": len(skipped),
            "exact_remainder": len(remainder),
            "t8_pipe_publication_separate_owner": t8_count,
        },
        "classification": {
            "playable_templates": [
                copy.deepcopy(row)
                for row in classifications
                if row["classification"] == "playable"
            ],
            "skipped_templates": skipped,
            "exact_remainder": copy.deepcopy(remainder),
        },
        "equivalence": {
            "expected_to_compact_missing": 0,
            "compact_to_expected_orphans": 0,
            "locked_fields": list(LOCKED_FIELDS),
            "mutation_gates": [
                "duration",
                "eut",
                "fallback",
                "shadow_order",
                "plateGem",
            ],
            "shadow_duplicates": 0,
            "stable_id_duplicates": 0,
        },
        "relation_fingerprint": expected["relation_fingerprint"],
        "compact_fingerprint": compact["compact_fingerprint"],
        "material_facts_fingerprint": sha256_json(facts),
        "compression": {
            "before_authored_datapack_entries": 2782,
            "after_authored_datapack_entries": compact["authored_entry_count"],
            "datapack_entry_delta": compact["authored_entry_count"] - 2782,
            "logical_relation_delta": 0,
            "publication_delta": 0,
        },
        "inputs_sha256": {
            path.relative_to(ROOT).as_posix(): sha256_bytes(path.read_bytes())
            for path in (
                POLICY,
                SOURCE,
                INDEX,
                REPORT,
                REGISTRATION_GATE,
                T8_READINESS,
            )
        },
        "artifacts_sha256": {
            "expected": sha256_json(expected),
            "compact": sha256_json(compact),
            "legacy_replay": sha256_json(replay),
        },
        "policy_sha256": sha256_json(policy),
        "t14c_interface": copy.deepcopy(policy["t14c_interface"]),
    }
    return ArtifactBundle(
        expected=stable_bytes(expected),
        compact=stable_bytes(compact),
        readiness=stable_bytes(readiness),
        legacy_replay=stable_bytes(replay),
    )


def write_bundle(bundle: ArtifactBundle) -> None:
    for path, content in (
        (EXPECTED, bundle.expected),
        (COMPACT, bundle.compact),
        (READINESS, bundle.readiness),
        (LEGACY_REPLAY, bundle.legacy_replay),
    ):
        path.write_bytes(content)


def check_bundle(bundle: ArtifactBundle) -> list[str]:
    errors: list[str] = []
    for path, expected in (
        (EXPECTED, bundle.expected),
        (COMPACT, bundle.compact),
        (READINESS, bundle.readiness),
        (LEGACY_REPLAY, bundle.legacy_replay),
    ):
        if not path.is_file():
            errors.append(f"missing T14a artifact: {path.relative_to(ROOT)}")
        elif path.read_bytes() != expected:
            errors.append(f"T14a artifact drift: {path.relative_to(ROOT)}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        bundle = build_bundle()
        if args.check:
            errors = check_bundle(bundle)
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print(
                "T14a Extruder equivalence is current: "
                "20 authored -> 2782 logical -> 2782 publication; "
                "42 skipped, 1 exact remainder, T8 pipe 257 separate."
            )
            return 0
        write_bundle(bundle)
        print(
            "Wrote T14a Extruder expected/compact/readiness/legacy replay "
            "artifacts for 2782 relations."
        )
        return 0
    except EquivalenceError as exc:
        print(f"T14a Extruder equivalence failed: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
