#!/usr/bin/env python3
"""Compile compact component-rule sources into runtime MaterialRule recipes."""

from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
SOURCE_DIR = ROOT / "tools" / "component_rule_sources"
SOURCE_FILES = (
    "acceptance_form_corrections.json",
    "component_baseline.json",
    "component_rules.json",
    "extruder_shapes.json",
    "material_groups.json",
)
EXTRUDER_INDEX = ROOT / "tools" / "gt6_extruder_templates_index_v5.json"
EXTRUDER_REPORT = ROOT / "tools" / "gt6_extruder_templates_report.json"
T14_EXTRUDER_POLICY = ROOT / "tools" / "extruder_policy.json"
T14_EXTRUDER_COMPACT = ROOT / "tools" / "extruder_compact.json"
T14_EXTRUDER_EXPECTED = ROOT / "tools" / "extruder_expected.json"
T14_EXTRUDER_READINESS = ROOT / "tools" / "extruder_readiness.json"
T14_EXTRUDER_LEGACY_REPLAY = ROOT / "tools" / "extruder_legacy_replay.json"
SELECTOR_POLICY = ROOT / "tools" / "component_selector_policy.json"
MATERIAL_REGISTRATION_GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
MATERIAL_PREFIX_DIR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_prefixes"
)
MATERIAL_DIR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "materials"
)
OUTPUT_ROOT = (
    ROOT
    / "src"
    / "component_rule_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)
MANIFEST = ROOT / "tools" / "component_rule_manifest.json"
GT6_ELECTRICAL_SOURCE = ROOT / "tools" / "gt6_electrical_source.json"
MATERIAL_ACTIVATION_POLICY = (
    ROOT / "tools" / "gt6_material_activation_policy.json"
)
ANY_RUBBER_TAG = "cruciblecraft:any_rubber_plates"
ANY_RUBBER_TAG_OUTPUT = (
    ROOT
    / "src/component_rule_generated/resources/data/cruciblecraft/tags/item"
    / "any_rubber_plates.json"
)
RULE_ID = re.compile(r"^[a-z0-9_]+(?:/[a-z0-9_]+)+$")
TOKEN = re.compile(r"^[a-z0-9_]+$")
RESOURCE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


class SourceError(ValueError):
    """An authoring source is incomplete or invalid."""


@dataclass(frozen=True)
class BuildBundle:
    generated: dict[str, bytes]
    any_rubber_tag: bytes
    manifest: bytes


def _read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        raise SourceError(f"{path}: cannot read valid JSON: {exc}") from exc
    if not isinstance(value, dict):
        raise SourceError(f"{path}: root must be an object")
    return value


def _stable_bytes(value: Any) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def _json_document(data: bytes | str) -> Any:
    if isinstance(data, bytes):
        data = data.decode("utf-8")
    return json.loads(data)


def _json_equal(left: bytes | str, right: bytes | str) -> bool:
    return _json_document(left) == _json_document(right)


def _build_any_rubber_tag(
    groups_document: dict[str, Any],
    electrical_source: dict[str, Any],
    material_dir: Path,
) -> tuple[bytes, dict[str, Any]]:
    _require_exact_keys(
        groups_document,
        {"schema_version", "groups"},
        set(),
        "material_groups.json",
    )
    if groups_document["schema_version"] != 1:
        raise SourceError("material_groups.json: unsupported schema_version")
    groups = groups_document["groups"]
    if not isinstance(groups, dict) or set(groups) != {"any_rubber"}:
        raise SourceError("material_groups.json: expected only any_rubber")
    group = groups["any_rubber"]
    _require_exact_keys(
        group,
        {
            "source_group",
            "source_member_names",
            "member_materials",
            "prefix",
            "item_tag",
        },
        set(),
        "material_groups.json.groups.any_rubber",
    )
    insulation = electrical_source.get("insulation") or {}
    if (
        group["source_group"] != "ANY.Rubber"
        or group["source_member_names"]
        != insulation.get("material_source_names")
        or group["item_tag"] != ANY_RUBBER_TAG
        or group["prefix"] != "plate"
    ):
        raise SourceError(
            "ANY.Rubber group drifted from gt6_electrical_source.json"
        )
    source_to_material: dict[str, str] = {}
    tag_name_by_material: dict[str, str] = {}
    for path in sorted(material_dir.glob("*.json")):
        if path.name in {"index.json", "README.md"}:
            continue
        material = _read_json(path)
        source_name = (
            material.get("gt6_metadata") or {}
        ).get("source_name")
        if source_name:
            source_to_material[source_name] = material["id"]
            tag_name_by_material[material["id"]] = material.get(
                "tag_name", material["id"]
            )
    expected_materials = [
        source_to_material[name]
        for name in insulation["material_source_names"]
    ]
    if group["member_materials"] != expected_materials:
        raise SourceError(
            "ANY.Rubber member material ids drifted from live source mapping"
        )
    tag_values = [
        f"#c:plates/{tag_name_by_material[material]}"
        for material in expected_materials
    ]
    return _stable_bytes({
        "replace": False,
        "values": tag_values,
    }), group


def _build_t6_electrical_expansion_overlay(
    registration_gate_path: Path,
) -> list[dict[str, str]]:
    gate = _read_json(registration_gate_path)
    t6_forms = gate.get("electrical_wire_forms")
    materials = gate.get("materials")
    if not isinstance(t6_forms, dict) or not isinstance(materials, dict):
        raise SourceError(
            f"{registration_gate_path}: missing T6 electrical wire gate"
        )
    activation = _read_json(MATERIAL_ACTIVATION_POLICY)
    records = activation.get("records")
    if not isinstance(records, list):
        raise SourceError(
            f"{MATERIAL_ACTIVATION_POLICY}: missing activation records"
        )
    pre_gate = {
        record["cc_id"]: set(record["pre_gate_registered_forms"])
        for record in records
        if isinstance(record, dict)
        and isinstance(record.get("cc_id"), str)
        and isinstance(record.get("pre_gate_registered_forms"), list)
    }
    newly_registered = sorted(
        material
        for material, forms in t6_forms.items()
        if forms == ["wire"]
        and "wire" not in pre_gate.get(material, set())
    )
    expected_new = [
        "blue_alloy",
        "electrotine_alloy",
        "graphene",
        "hslasteel",
        "naquadah",
        "yttrium_barium_cuprate",
    ]
    if newly_registered != expected_new:
        raise SourceError(
            "T6 electrical wire registration delta drifted: "
            f"{newly_registered}"
        )
    enabled = [
        material
        for material in newly_registered
        if "ingot" in materials.get(material, [])
    ]
    expected_enabled = [
        "blue_alloy",
        "electrotine_alloy",
        "hslasteel",
        "naquadah",
        "yttrium_barium_cuprate",
    ]
    if enabled != expected_enabled:
        raise SourceError(
            "T6 electrical component expansion delta drifted: "
            f"{enabled}"
        )
    return [
        {
            "material": material,
            "form": "wire",
            "rule": "wiremill/ingot_to_wire",
            "classification": "electrical_source_backed_runtime_required",
        }
        for material in enabled
    ]


def _require_exact_keys(
    value: dict[str, Any],
    required: set[str],
    optional: set[str],
    where: str,
) -> None:
    missing = required - value.keys()
    unknown = value.keys() - required - optional
    if missing or unknown:
        raise SourceError(
            f"{where}: missing keys {sorted(missing)}; unknown keys {sorted(unknown)}"
        )


def _require_positive_int(value: Any, where: str) -> int:
    if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
        raise SourceError(f"{where}: expected a positive integer")
    return value


def _require_token(value: Any, where: str) -> str:
    if not isinstance(value, str) or not TOKEN.fullmatch(value):
        raise SourceError(f"{where}: expected a lower-case semantic token")
    return value


def _validate_source_set(source_dir: Path) -> None:
    actual = sorted(
        path.name for path in source_dir.glob("*.json") if path.is_file()
    )
    expected = sorted(SOURCE_FILES)
    if actual != expected:
        raise SourceError(
            f"{source_dir}: source file set drift; expected {expected}, found {actual}"
        )


def _validate_rules(document: dict[str, Any]) -> list[dict[str, Any]]:
    _require_exact_keys(
        document,
        {"schema_version", "namespace", "unit_scale", "prefix_units", "rules"},
        set(),
        "component_rules.json",
    )
    if document["schema_version"] != 1:
        raise SourceError("component_rules.json: schema_version must be 1")
    if document["namespace"] != "cruciblecraft":
        raise SourceError("component_rules.json: namespace must be cruciblecraft")
    if document["unit_scale"] != 144:
        raise SourceError("component_rules.json: unit_scale must remain 144")
    units = document["prefix_units"]
    if not isinstance(units, dict) or not units:
        raise SourceError("component_rules.json: prefix_units must be non-empty")
    for prefix, amount in units.items():
        _require_token(prefix, f"prefix_units[{prefix!r}]")
        _require_positive_int(amount, f"prefix_units[{prefix!r}]")
    rules = document["rules"]
    if not isinstance(rules, list):
        raise SourceError("component_rules.json: rules must be an array")
    if len(rules) != 28:
        raise SourceError(
            f"component_rules.json: expected 28 non-extruder rules, found {len(rules)}"
        )
    ids: list[str] = []
    for index, rule in enumerate(rules):
        if not isinstance(rule, dict):
            raise SourceError(f"rules[{index}]: expected an object")
        rule_id = rule.get("id")
        if not isinstance(rule_id, str) or not RULE_ID.fullmatch(rule_id):
            raise SourceError(f"rules[{index}].id: invalid recipe path")
        ids.append(rule_id)
        kind = rule.get("kind")
        where = f"rules[{index}] ({rule_id})"
        if kind == "target_transform":
            _validate_target_transform(rule, units, where)
        elif kind == "cable":
            _validate_cable(rule, where)
        elif kind == "compatibility_bridge":
            _validate_bridge(rule, where)
        else:
            raise SourceError(f"{where}: unknown kind {kind!r}")
    if ids != sorted(ids):
        raise SourceError("component_rules.json: rules must be sorted by id")
    if len(set(ids)) != len(ids):
        raise SourceError("component_rules.json: duplicate rule id")
    return rules


def _validate_target_transform(
    rule: dict[str, Any], units: dict[str, int], where: str
) -> None:
    _require_exact_keys(
        rule,
        {
            "id",
            "kind",
            "map",
            "selector",
            "inputs",
            "output",
            "source_units",
            "duration",
            "eut",
        },
        set(),
        where,
    )
    map_id = _require_token(rule["map"], f"{where}.map")
    if not rule["id"].startswith(map_id + "/"):
        raise SourceError(f"{where}: id must be nested under map {map_id}")
    _require_token(rule["selector"], f"{where}.selector")
    _require_token(rule["output"], f"{where}.output")
    _require_positive_int(rule["duration"], f"{where}.duration")
    _require_positive_int(rule["eut"], f"{where}.eut")
    source_units = _require_positive_int(rule["source_units"], f"{where}.source_units")
    inputs = rule["inputs"]
    if not isinstance(inputs, list) or not inputs:
        raise SourceError(f"{where}.inputs: expected a non-empty array")
    computed_units = 0
    for input_index, item in enumerate(inputs):
        if not isinstance(item, dict):
            raise SourceError(f"{where}.inputs[{input_index}]: expected an object")
        _require_exact_keys(
            item, {"prefix", "count"}, set(), f"{where}.inputs[{input_index}]"
        )
        prefix = _require_token(
            item["prefix"], f"{where}.inputs[{input_index}].prefix"
        )
        count = _require_positive_int(
            item["count"], f"{where}.inputs[{input_index}].count"
        )
        if prefix not in units:
            raise SourceError(f"{where}: no authored unit value for input {prefix}")
        computed_units += units[prefix] * count
    if computed_units != source_units:
        raise SourceError(
            f"{where}: input units {computed_units} do not equal source_units "
            f"{source_units}"
        )


def _validate_cable(rule: dict[str, Any], where: str) -> None:
    _require_exact_keys(
        rule,
        {"id", "kind", "wire", "cable", "rubber_plates", "duration", "eut"},
        set(),
        where,
    )
    if not rule["id"].startswith("assembler/"):
        raise SourceError(f"{where}: cable rules must target assembler")
    _require_token(rule["wire"], f"{where}.wire")
    _require_token(rule["cable"], f"{where}.cable")
    _require_positive_int(rule["rubber_plates"], f"{where}.rubber_plates")
    _require_positive_int(rule["duration"], f"{where}.duration")
    _require_positive_int(rule["eut"], f"{where}.eut")


def _validate_bridge(rule: dict[str, Any], where: str) -> None:
    _require_exact_keys(
        rule,
        {
            "id",
            "kind",
            "map",
            "material",
            "input_item",
            "output",
            "duration",
            "eut",
            "reason",
        },
        set(),
        where,
    )
    map_id = _require_token(rule["map"], f"{where}.map")
    if not rule["id"].startswith(map_id + "/"):
        raise SourceError(f"{where}: id must be nested under map {map_id}")
    _require_token(rule["material"], f"{where}.material")
    _require_token(rule["output"], f"{where}.output")
    if (
        not isinstance(rule["input_item"], str)
        or not RESOURCE.fullmatch(rule["input_item"])
    ):
        raise SourceError(f"{where}.input_item: invalid resource location")
    _require_positive_int(rule["duration"], f"{where}.duration")
    _require_positive_int(rule["eut"], f"{where}.eut")
    if not isinstance(rule["reason"], str) or not rule["reason"].strip():
        raise SourceError(f"{where}.reason: expected a non-empty explanation")


def _validate_baseline(
    baseline: dict[str, Any], rules: list[dict[str, Any]]
) -> None:
    _require_exact_keys(
        baseline,
        {
            "schema_version",
            "captured_from",
            "capture_test",
            "source_rules",
            "expanded_recipes",
            "expansion_budget",
            "per_rule_expanded",
            "per_map_expanded",
            "shadow_signatures",
            "skip_reasons",
        },
        {"digests"},
        "component_baseline.json",
    )
    if baseline["schema_version"] != 1:
        raise SourceError("component_baseline.json: schema_version must be 1")
    if baseline["source_rules"] != len(rules):
        raise SourceError("component_baseline.json: source rule count drift")
    per_rule = baseline["per_rule_expanded"]
    rule_ids = [rule["id"] for rule in rules]
    if not isinstance(per_rule, dict) or sorted(per_rule) != rule_ids:
        raise SourceError("component_baseline.json: per-rule baseline is incomplete")
    if sum(per_rule.values()) != baseline["expanded_recipes"]:
        raise SourceError("component_baseline.json: per-rule expanded sum drift")
    if sum(baseline["per_map_expanded"].values()) != baseline["expanded_recipes"]:
        raise SourceError("component_baseline.json: per-map expanded sum drift")
    if baseline["expanded_recipes"] > baseline["expansion_budget"]:
        raise SourceError("component_baseline.json: expansion budget exceeded")
    shadows = baseline["shadow_signatures"]
    if shadows != {
        "total": baseline["expanded_recipes"],
        "unique": baseline["expanded_recipes"],
        "duplicates": 0,
    }:
        raise SourceError("component_baseline.json: shadow baseline has duplicates")
    extra = baseline.get("digests")
    if extra is not None and not isinstance(extra, dict):
        raise SourceError("component_baseline.json: extra keys must be an object")
    if (
        not isinstance(baseline["skip_reasons"], list)
        or not baseline["skip_reasons"]
        or len(set(baseline["skip_reasons"])) != len(baseline["skip_reasons"])
    ):
        raise SourceError("component_baseline.json: skip reasons must be unique")


def _validate_acceptance_corrections(
    document: dict[str, Any],
    rules: list[dict[str, Any]],
    registration_gate_path: Path,
    material_dir: Path,
) -> list[dict[str, Any]]:
    _require_exact_keys(
        document,
        {"schema_version", "corrections"},
        set(),
        "acceptance_form_corrections.json",
    )
    if document["schema_version"] != 1:
        raise SourceError(
            "acceptance_form_corrections.json: schema_version must be 1"
        )
    corrections = document["corrections"]
    if not isinstance(corrections, list) or not corrections:
        raise SourceError(
            "acceptance_form_corrections.json: corrections must be non-empty"
        )
    rule_ids = {rule["id"] for rule in rules}
    gate = _read_json(registration_gate_path)
    gate_materials = gate.get("materials")
    if gate.get("schema_version") not in (1, 2) or not isinstance(gate_materials, dict):
        raise SourceError(f"{registration_gate_path}: invalid material registration gate")
    identities: list[tuple[str, tuple[str, ...]]] = []
    for index, correction in enumerate(corrections):
        where = f"acceptance_form_corrections.json corrections[{index}]"
        if not isinstance(correction, dict):
            raise SourceError(f"{where}: expected an object")
        _require_exact_keys(
            correction,
            {
                "material",
                "add_forms",
                "affects_rules",
                "expected_expansion_delta",
                "classification",
                "reason",
            },
            set(),
            where,
        )
        material = _require_token(correction["material"], f"{where}.material")
        forms = correction["add_forms"]
        if (
            not isinstance(forms, list)
            or not forms
            or forms != sorted(set(forms))
            or any(
                not isinstance(form, str) or not TOKEN.fullmatch(form)
                for form in forms
            )
        ):
            raise SourceError(f"{where}.add_forms: expected sorted unique forms")
        affected = correction["affects_rules"]
        if (
            not isinstance(affected, list)
            or not affected
            or affected != sorted(set(affected))
            or any(rule_id not in rule_ids for rule_id in affected)
        ):
            raise SourceError(f"{where}.affects_rules: unknown or duplicate rule")
        _require_positive_int(
            correction["expected_expansion_delta"],
            f"{where}.expected_expansion_delta",
        )
        if (
            correction["classification"]
            != "acceptance_required_not_gt6_original_gate"
        ):
            raise SourceError(
                f"{where}.classification must distinguish acceptance from GT6"
            )
        if (
            not isinstance(correction["reason"], str)
            or not correction["reason"].strip()
        ):
            raise SourceError(f"{where}.reason must be non-empty")
        registered = gate_materials.get(material)
        if not isinstance(registered, list) or not set(forms) <= set(registered):
            raise SourceError(
                f"{where}: corrected forms are absent from registration gate"
            )
        material_document = _read_json(material_dir / f"{material}.json")
        generation_flags = material_document.get("generation_flags", [])
        for form in forms:
            if f"cruciblecraft:generates_{form}" not in generation_flags:
                raise SourceError(
                    f"{where}: {material}/{form} lacks its generation flag"
                )
        identities.append((material, tuple(forms)))
    if identities != sorted(set(identities)):
        raise SourceError(
            "acceptance_form_corrections.json: corrections must be unique and sorted"
        )
    return corrections


def _map_for(rule: dict[str, Any]) -> str:
    if rule["kind"] == "cable":
        return "assembler"
    return rule["map"]


def _prefixed(prefix: str, **extra: str) -> dict[str, str]:
    return {**extra, "prefix": f"cruciblecraft:{prefix}"}


def _compile_rule(rule: dict[str, Any], unit_scale: int) -> dict[str, Any]:
    kind = rule["kind"]
    if kind == "target_transform":
        selector = rule["selector"]
        source_units = rule["source_units"]
        output = rule["output"]
        numerator = f"target_units({selector}) * {source_units}"
        denominator = f"{unit_scale} * prefix_units({output})"
        divisor = f"gcd({numerator}, {denominator})"
        batch_inputs = f"{denominator} / {divisor}"
        batch_outputs = f"{numerator} / {divisor}"
        return {
            "type": "cruciblecraft:material_rule",
            "conditions": [
                (
                    "has_registered_for("
                    f"\"processing_target:{selector}\", {output})"
                )
            ],
            "duration": f"{rule['duration']} * ({batch_inputs})",
            "eut": str(rule["eut"]),
            "item_inputs": [
                _prefixed(
                    item["prefix"],
                    count=f"{item['count']} * ({batch_inputs})",
                )
                for item in rule["inputs"]
            ],
            "item_outputs": [
                _prefixed(
                    output,
                    count=batch_outputs,
                    material_selector=f"processing_target:{selector}",
                )
            ],
            "target": f"cruciblecraft:{rule['map']}",
        }
    if kind == "cable":
        insulation = {"tag": ANY_RUBBER_TAG}
        if rule["rubber_plates"] != 1:
            insulation["count"] = str(rule["rubber_plates"])
        return {
            "type": "cruciblecraft:material_rule",
            "duration": str(rule["duration"]),
            "eut": str(rule["eut"]),
            "item_inputs": [_prefixed(rule["wire"]), insulation],
            "item_outputs": [_prefixed(rule["cable"])],
            "target": "cruciblecraft:assembler",
        }
    return {
        "type": "cruciblecraft:material_rule",
        "duration": str(rule["duration"]),
        "eut": str(rule["eut"]),
        "item_inputs": [{"item": rule["input_item"]}],
        "item_outputs": [_prefixed(rule["output"])],
        "material": rule["material"],
        "target": f"cruciblecraft:{rule['map']}",
    }


def _validate_extruder_source(
    source: dict[str, Any],
    index_path: Path,
    report_path: Path,
    selector_policy_path: Path,
    allowed_prefixes: set[str],
    registration_gate_path: Path,
    material_dir: Path,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]]:
    index = _read_json(index_path)
    report = _read_json(report_path)
    selector_policy = _read_json(selector_policy_path)
    registration_gate = _read_json(registration_gate_path)
    registered_forms = registration_gate.get("materials")
    if registration_gate.get("schema_version") not in (1, 2) or not isinstance(registered_forms, dict):
        raise SourceError(f"{registration_gate_path}: invalid material registration gate")
    material_tags = {
        document["id"]: set(
            (document.get("gt6_metadata") or {}).get("material_tags") or []
        )
        for path in material_dir.glob("*.json")
        if path.name != "index.json"
        for document in [_read_json(path)]
    }
    if not allowed_prefixes:
        raise SourceError(
            "component_rules.json: registered prefix directory is empty"
        )
    if (
        index.get("schema_version") != 5
        or index.get("map") != "gt.recipe.extruder"
        or index.get("functional_template_count") != 62
        or len(index.get("templates", [])) != 62
    ):
        raise SourceError(f"{index_path}: expected the verified 62-template index")
    if (
        report.get("schema_version") != 5
        or report.get("map") != "gt.recipe.extruder"
        or report.get("counts", {}).get("shape_template_count") != 62
        or not report.get("verification", {}).get("replay_verified")
    ):
        raise SourceError(f"{report_path}: expected replay-verified compact evidence")
    extruder_policy = selector_policy.get("maps", {}).get("extruder", {})
    if (
        selector_policy.get("schema_version") != 1
        or extruder_policy.get("delivery") != "T3"
        or extruder_policy.get("selector") != "concrete_shape"
        or extruder_policy.get("support") != "explicit_sparse_relation"
    ):
        raise SourceError(
            f"{selector_policy_path}: extruder selector policy is not T3 concrete-shape sparse support"
        )
    _require_exact_keys(
        source,
        {"schema_version", "evidence", "shapes", "templates", "recipes", "verification"},
        set(),
        "extruder_shapes.json",
    )
    if source["schema_version"] != 1:
        raise SourceError("extruder_shapes.json: schema_version must be 1")
    evidence = source["evidence"]
    expected_evidence = {
        "index": index_path.relative_to(ROOT).as_posix(),
        "report": report_path.relative_to(ROOT).as_posix(),
        "selector": "concrete_shape",
        "selector_policy": selector_policy_path.relative_to(ROOT).as_posix(),
        "support": "explicit_sparse_relation",
    }
    if any(evidence.get(key) != value for key, value in expected_evidence.items()):
        raise SourceError("extruder_shapes.json: compact evidence path or selector drift")
    shapes = source["shapes"]
    if not isinstance(shapes, list) or len(shapes) != 31:
        raise SourceError("extruder_shapes.json: expected exactly 31 shapes")
    shape_ids = [shape.get("id") for shape in shapes]
    shape_items = [shape.get("item") for shape in shapes]
    base_metas = [shape.get("base_meta") for shape in shapes]
    if (
        len(set(shape_ids)) != 31
        or len(set(shape_items)) != 31
        or base_metas != list(range(10001, 10032))
    ):
        raise SourceError("extruder_shapes.json: shape ids/items/metas must be unique and stable")
    for shape in shapes:
        _require_exact_keys(
            shape,
            {"base_meta", "id", "item", "en_us", "zh_cn"},
            set(),
            f"shape {shape.get('id')}",
        )
        _require_token(shape["id"], f"shape {shape.get('id')}.id")
        if shape["item"] != f"cruciblecraft:extruder_shape_{shape['id']}":
            raise SourceError(f"shape {shape['id']}: item id does not match semantic id")

    classifications = source["templates"]
    if not isinstance(classifications, list) or len(classifications) != 62:
        raise SourceError("extruder_shapes.json: expected 62 template classifications")
    indexed = {
        (row["template_id"], row["shape_meta"], row["heat_mode"])
        for row in index["templates"]
    }
    classified = {
        (row.get("template_id"), row.get("shape_meta"), row.get("heat_mode"))
        for row in classifications
    }
    if classified != indexed or len(classified) != 62:
        raise SourceError("extruder_shapes.json: classifications do not match compact index")
    if any(row.get("classification") not in {"playable", "skipped"}
           or not row.get("reason") for row in classifications):
        raise SourceError("extruder_shapes.json: every template needs a playable/skip reason")

    recipes = source["recipes"]
    if not isinstance(recipes, list) or not recipes:
        raise SourceError("extruder_shapes.json: recipes must be a non-empty sparse relation")
    shape_by_id = {shape["id"]: shape for shape in shapes}
    signatures: set[tuple[Any, ...]] = set()
    acceptance = {"copper", "tin", "iron", "gold"}
    accepted: set[str] = set()
    for offset, recipe in enumerate(recipes):
        _require_exact_keys(
            recipe,
            {
                "template_id",
                "shape_meta",
                "heat_mode",
                "shape",
                "material",
                "input",
                "output",
                "duration",
                "eut",
                "provenance",
            },
            set(),
            f"extruder recipe {offset}",
        )
        shape = shape_by_id.get(recipe["shape"])
        if shape is None:
            raise SourceError(f"extruder recipe {offset}: unknown shape")
        if (recipe["template_id"], recipe["shape_meta"], recipe["heat_mode"]) not in indexed:
            raise SourceError(f"extruder recipe {offset}: unknown template provenance")
        if recipe["shape_meta"] not in {
            shape["base_meta"], shape["base_meta"] + 200
        }:
            raise SourceError(f"extruder recipe {offset}: heat variant shape mismatch")
        _require_token(recipe["material"], f"extruder recipe {offset}.material")
        material_forms = registered_forms.get(recipe["material"])
        if not isinstance(material_forms, list):
            raise SourceError(
                f"extruder recipe {offset}: material is absent from registration gate"
            )
        tags = material_tags.get(recipe["material"])
        if tags is None:
            raise SourceError(
                f"extruder recipe {offset}: material definition is absent"
            )
        if "PROCESSING.EXTRUDABLE" not in tags:
            raise SourceError(
                f"extruder recipe {offset}: material lacks PROCESSING.EXTRUDABLE"
            )
        expected_eut = (
            16 if "PROCESSING.EXTRUDABLE_SIMPLE" in tags else 96
        )
        if recipe["eut"] != expected_eut:
            raise SourceError(
                f"extruder recipe {offset}: EU/t is not the exact "
                "PROCESSING.EXTRUDABLE_SIMPLE function"
            )
        for side in ("input", "output"):
            resource = recipe[side]
            _require_exact_keys(
                resource, {"prefix", "count"}, set(),
                f"extruder recipe {offset}.{side}",
            )
            _require_token(resource["prefix"], f"extruder recipe {offset}.{side}.prefix")
            if resource["prefix"] not in allowed_prefixes:
                raise SourceError(
                    f"extruder recipe {offset}.{side}: prefix is outside the 43-prefix catalog"
                )
            if resource["prefix"] not in material_forms:
                raise SourceError(
                    f"extruder recipe {offset}.{side}: form is absent from registration gate"
                )
            _require_positive_int(resource["count"], f"extruder recipe {offset}.{side}.count")
            if resource["count"] > 64:
                raise SourceError(f"extruder recipe {offset}: item count exceeds slot capacity")
        _require_positive_int(recipe["duration"], f"extruder recipe {offset}.duration")
        eut = _require_positive_int(recipe["eut"], f"extruder recipe {offset}.eut")
        if eut > 256:
            raise SourceError(f"extruder recipe {offset}: EU/t exceeds extruder packet limit")
        signature = (
            recipe["material"],
            recipe["shape"],
            recipe["input"]["prefix"],
            recipe["input"]["count"],
        )
        if signature in signatures:
            raise SourceError(f"extruder recipe {offset}: shadow signature {signature}")
        signatures.add(signature)
        if (
            recipe["material"] in acceptance
            and recipe["output"]["prefix"] in {"rod", "long_rod"}
        ):
            accepted.add(recipe["material"])
    if len(recipes) > 4_648:
        raise SourceError(
            f"extruder sparse projection {len(recipes)} exceeds budget 4648"
        )
    if accepted != acceptance:
        raise SourceError(
            "extruder acceptance routes missing for "
            + ", ".join(sorted(acceptance - accepted))
        )
    verification = source["verification"]
    if (
        verification.get("shape_count") != 31
        or verification.get("template_count") != 62
        or verification.get("unclassified") != 0
        or verification.get("playable_relations") != len(recipes)
        or verification.get("shadow_signatures", {}).get("duplicates") != 0
    ):
        raise SourceError("extruder_shapes.json: verification summary drift")
    return shapes, classifications, recipes


def _validate_t14_extruder_compact(
    compact_path: Path,
    recipes: list[dict[str, Any]],
    classifications: list[dict[str, Any]],
    material_dir: Path,
) -> list[dict[str, Any]]:
    compact = _read_json(compact_path)
    _require_exact_keys(
        compact,
        {
            "schema_version",
            "family",
            "runtime_type",
            "selector",
            "authored_entry_count",
            "logical_relation_count",
            "templates",
        },
        {"compact_fingerprint"},
        compact_path.name,
    )
    templates = compact["templates"]
    if (
        compact["schema_version"] != 1
        or compact["family"] != "extruder"
        or compact["runtime_type"] != "cruciblecraft:material_rule"
        or compact["selector"] != "template_grouped_exact_relation_builder"
        or compact["authored_entry_count"] != 20
        or compact["logical_relation_count"] != 2782
        or not isinstance(templates, list)
        or len(templates) != 20
    ):
        raise SourceError(f"{compact_path}: expected the T14a 20 -> 2782 compact table")
    playable = {
        (row["template_id"], row["shape_meta"], row["heat_mode"]): row["shape"]
        for row in classifications
        if row["classification"] == "playable"
    }
    source_by_id = {
        f"cruciblecraft:extruder/{row['shape']}/"
        f"{row['material']}/{row['material']}": row
        for row in recipes
    }
    if len(source_by_id) != 2782:
        raise SourceError("T14a source stable ids are not unique")
    facts: dict[str, tuple[str | None, bool]] = {}
    for material in {row["material"] for row in recipes}:
        document = _read_json(material_dir / f"{material}.json")
        metadata = document.get("gt6_metadata") or {}
        forging = (metadata.get("processing_targets") or {}).get("forging")
        facts[material] = (
            forging.get("material") if isinstance(forging, dict) else None,
            "cruciblecraft:generates_plate_gem"
            in document.get("generation_flags", []),
        )
    seen_ids: set[str] = set()
    seen_authored: set[str] = set()
    rows: list[dict[str, Any]] = []
    for template in templates:
        _require_exact_keys(
            template,
            {
                "authored_id",
                "template_id",
                "shape_meta",
                "shape",
                "shape_item",
                "heat_mode",
                "relation_count",
                "relations",
            },
            set(),
            "T14a compact template",
        )
        authored_id = template["authored_id"]
        if (
            not isinstance(authored_id, str)
            or not authored_id.startswith("cruciblecraft:extruder/compact/")
            or authored_id in seen_authored
        ):
            raise SourceError(f"{compact_path}: invalid or duplicate authored id")
        seen_authored.add(authored_id)
        key = (
            template["template_id"],
            template["shape_meta"],
            template["heat_mode"],
        )
        if playable.get(key) != template["shape"]:
            raise SourceError(f"{authored_id}: not an exact playable v5 template")
        relations = template["relations"]
        if (
            not isinstance(relations, list)
            or len(relations) != template["relation_count"]
            or not relations
        ):
            raise SourceError(f"{authored_id}: relation count drift")
        previous_order = -1
        materials: set[str] = set()
        for relation in relations:
            _require_exact_keys(
                relation,
                {
                    "stable_id",
                    "material",
                    "shape",
                    "shape_meta",
                    "template_id",
                    "input",
                    "output",
                    "duration",
                    "eut",
                    "fallback",
                    "forging_target",
                    "plateGem",
                    "heat_mode",
                    "shadow_order",
                    "provenance",
                },
                set(),
                f"{authored_id} relation",
            )
            stable_id = relation["stable_id"]
            source = source_by_id.get(stable_id)
            if source is None or stable_id in seen_ids:
                raise SourceError(
                    f"{authored_id}: missing source or duplicate stable id {stable_id}"
                )
            seen_ids.add(stable_id)
            expected_forging, expected_plate_gem = facts[relation["material"]]
            expected = {
                "material": source["material"],
                "shape": source["shape"],
                "shape_meta": source["shape_meta"],
                "template_id": source["template_id"],
                "input": source["input"],
                "output": source["output"],
                "duration": source["duration"],
                "eut": source["eut"],
                "fallback": (
                    "dust" if source["input"]["prefix"] == "dust" else "none"
                ),
                "forging_target": expected_forging,
                "plateGem": expected_plate_gem,
                "heat_mode": source["heat_mode"],
                "provenance": source["provenance"],
            }
            actual = {field: relation[field] for field in expected}
            if actual != expected:
                raise SourceError(
                    f"{stable_id}: compact exact relation drift: {actual!r}"
                )
            if (
                relation["shape"] != template["shape"]
                or relation["shape_meta"] != template["shape_meta"]
                or relation["template_id"] != template["template_id"]
                or relation["heat_mode"] != template["heat_mode"]
            ):
                raise SourceError(f"{stable_id}: compact template boundary drift")
            if relation["material"] in materials:
                raise SourceError(f"{authored_id}: ambiguous material relation")
            materials.add(relation["material"])
            order = relation["shadow_order"]
            if not isinstance(order, int) or order <= previous_order:
                raise SourceError(f"{authored_id}: shadow order is not increasing")
            previous_order = order
            rows.append(relation)
    if seen_ids != set(source_by_id):
        raise SourceError(f"{compact_path}: compact/source relation sets differ")
    ordered = sorted(rows, key=lambda row: (row["shadow_order"], row["stable_id"]))
    if [row["shadow_order"] for row in ordered] != list(range(2782)):
        raise SourceError(f"{compact_path}: shadow order is not a total order")
    return templates


def _compile_sparse_extruder(template: dict[str, Any]) -> dict[str, Any]:
    relations = []
    for source in template["relations"]:
        relation = {
            "stable_id": source["stable_id"],
            "material": source["material"],
            "input": source["input"],
            "output": source["output"],
            "duration": source["duration"],
            "eut": source["eut"],
            "fallback": source["fallback"],
            "plate_gem": source["plateGem"],
            "heat_mode": source["heat_mode"],
            "shadow_order": source["shadow_order"],
        }
        if source["forging_target"] is not None:
            relation["forging_target"] = source["forging_target"]
        relations.append(relation)
    return {
        "type": "cruciblecraft:material_rule",
        "target": "cruciblecraft:extruder",
        "sparse": {
            "shape_item": template["shape_item"],
            "shape": template["shape"],
            "shape_meta": template["shape_meta"],
            "template_id": template["template_id"],
            "heat_mode": template["heat_mode"],
            "relations": relations,
        },
    }


def build_bundle(
    source_dir: Path = SOURCE_DIR,
    extruder_index: Path = EXTRUDER_INDEX,
    extruder_report: Path = EXTRUDER_REPORT,
    selector_policy: Path = SELECTOR_POLICY,
    registration_gate: Path = MATERIAL_REGISTRATION_GATE,
    material_prefix_dir: Path = MATERIAL_PREFIX_DIR,
    material_dir: Path = MATERIAL_DIR,
) -> BuildBundle:
    _validate_source_set(source_dir)
    rules_document = _read_json(source_dir / "component_rules.json")
    baseline = _read_json(source_dir / "component_baseline.json")
    corrections_document = _read_json(
        source_dir / "acceptance_form_corrections.json"
    )
    extruder_source = _read_json(source_dir / "extruder_shapes.json")
    groups_document = _read_json(source_dir / "material_groups.json")
    any_rubber_tag, any_rubber_group = _build_any_rubber_tag(
        groups_document,
        _read_json(GT6_ELECTRICAL_SOURCE),
        material_dir,
    )
    rules = _validate_rules(rules_document)
    _validate_baseline(baseline, rules)
    corrections = _validate_acceptance_corrections(
        corrections_document,
        rules,
        registration_gate,
        material_dir,
    )
    t6_overlay = _build_t6_electrical_expansion_overlay(
        registration_gate
    )
    allowed_prefixes = {
        path.stem
        for path in material_prefix_dir.glob("*.json")
        if path.name != "index.json"
    }
    shapes, classifications, extruder_recipes = (
        _validate_extruder_source(
            extruder_source,
            extruder_index,
            extruder_report,
            selector_policy,
            allowed_prefixes,
            registration_gate,
            material_dir,
        )
    )
    compact_templates = _validate_t14_extruder_compact(
        T14_EXTRUDER_COMPACT,
        extruder_recipes,
        classifications,
        material_dir,
    )
    correction_delta = sum(
        correction["expected_expansion_delta"] for correction in corrections
    )
    t6_overlay_delta = len(t6_overlay)
    non_extruder_expanded = (
        baseline["expanded_recipes"]
        + correction_delta
        + t6_overlay_delta
    )
    total_expanded = non_extruder_expanded + len(extruder_recipes)
    if total_expanded > baseline["expansion_budget"]:
        raise SourceError(
            f"projected expansion count {total_expanded} exceeds "
            f"budget {baseline['expansion_budget']}"
        )
    generated: dict[str, bytes] = {
        f"{rule['id']}.json": _stable_bytes(
            _compile_rule(rule, rules_document["unit_scale"])
        )
        for rule in rules
    }
    for template in compact_templates:
        authored_path = template["authored_id"].removeprefix("cruciblecraft:")
        relative = f"{authored_path}.json"
        if relative in generated:
            raise SourceError(f"duplicate generated recipe path: {relative}")
        generated[relative] = _stable_bytes(
            _compile_sparse_extruder(template)
        )
    generated = dict(sorted(generated.items()))
    map_source_counts = Counter(_map_for(rule) for rule in rules)
    map_source_counts["extruder"] = len(compact_templates)
    per_map_expanded = dict(baseline["per_map_expanded"])
    rules_by_id = {rule["id"]: rule for rule in rules}
    for correction in corrections:
        delta = correction["expected_expansion_delta"]
        affected_rules = correction["affects_rules"]
        if delta % len(affected_rules) != 0:
            raise SourceError(
                "acceptance correction delta must divide across affected rules"
            )
        per_rule_delta = delta // len(affected_rules)
        for rule_id in affected_rules:
            per_map_expanded[_map_for(rules_by_id[rule_id])] += per_rule_delta
    per_map_expanded["wiremill"] += t6_overlay_delta
    per_map_expanded["extruder"] = len(extruder_recipes)
    per_rule_expanded = dict(baseline["per_rule_expanded"])
    for correction in corrections:
        delta = correction["expected_expansion_delta"]
        affected_rules = correction["affects_rules"]
        per_rule_delta = delta // len(affected_rules)
        for rule_id in affected_rules:
            per_rule_expanded[rule_id] += per_rule_delta
    per_rule_expanded["wiremill/ingot_to_wire"] += t6_overlay_delta
    per_rule_expanded.update({
        template["authored_id"].removeprefix("cruciblecraft:"):
                template["relation_count"]
        for template in compact_templates
    })
    extruder_signatures = [
        (
            recipe["material"],
            recipe["shape"],
            recipe["input"]["prefix"],
            recipe["input"]["count"],
        )
        for recipe in extruder_recipes
    ]
    if len(set(extruder_signatures)) != len(extruder_signatures):
        raise SourceError("extruder shadow signatures are not unique")
    classification_counts = dict(sorted(Counter(
        row["classification"] for row in classifications
    ).items()))
    skip_reasons = dict(sorted(Counter(
        row["reason"] for row in classifications
        if row["classification"] == "skipped"
    ).items()))
    authored_projected = total_expanded
    manifest = {
        "schema_version": 1,
        "source_schema_version": rules_document["schema_version"],
        "builder": {
            "source": "tools/build_component_rules.py",
        },
        "build_inputs": [
            extruder_index.relative_to(ROOT).as_posix(),
            extruder_report.relative_to(ROOT).as_posix(),
            selector_policy.relative_to(ROOT).as_posix(),
            registration_gate.relative_to(ROOT).as_posix(),
            GT6_ELECTRICAL_SOURCE.relative_to(ROOT).as_posix(),
            MATERIAL_ACTIVATION_POLICY.relative_to(ROOT).as_posix(),
            T14_EXTRUDER_POLICY.relative_to(ROOT).as_posix(),
            T14_EXTRUDER_COMPACT.relative_to(ROOT).as_posix(),
            T14_EXTRUDER_EXPECTED.relative_to(ROOT).as_posix(),
            T14_EXTRUDER_READINESS.relative_to(ROOT).as_posix(),
            T14_EXTRUDER_LEGACY_REPLAY.relative_to(ROOT).as_posix(),
        ],
        "material_groups": {
            "any_rubber": any_rubber_group,
            "generated_tag": (
                "src/component_rule_generated/resources/data/cruciblecraft/"
                "tags/item/any_rubber_plates.json"
            ),
        },
        "runtime_codec": "cruciblecraft:material_rule/MaterialRule.CODEC",
        "source_rules": len(rules) + len(compact_templates),
        "authored_datapack_entries": len(rules) + len(compact_templates),
        "authored_projected_recipes": authored_projected,
        "expanded_recipes": authored_projected,
        "expansion_is_authored_projection": True,
        "non_extruder_expanded_recipes": non_extruder_expanded,
        "extruder_expanded_recipes": len(extruder_recipes),
        "extruder_authored_entries": len(compact_templates),
        "extruder_logical_relations": len(extruder_recipes),
        "extruder_runtime_publication": len(extruder_recipes),
        "extruder_expansion_budget": 4_648,
        "expansion_budget": baseline["expansion_budget"],
        "within_budget": authored_projected <= baseline["expansion_budget"],
        "acceptance_form_corrections": {
            "source": (
                "tools/component_rule_sources/acceptance_form_corrections.json"
            ),
            "classification": "acceptance_required_not_gt6_original_gate",
            "expansion_delta": correction_delta,
            "entries": corrections,
        },
        "electrical_form_expansion": {
            "classification": "electrical_source_backed_runtime_required",
            "expansion_delta": t6_overlay_delta,
            "entries": t6_overlay,
        },
        "per_map": {
            map_id: {
                "source_rules": map_source_counts[map_id],
                "expanded_recipes": per_map_expanded[map_id],
            }
            for map_id in sorted(map_source_counts)
        },
        "per_rule_expanded": dict(sorted(per_rule_expanded.items())),
        "unit_conservation": {
            "status": "verified",
            "conserving_rules": len(rules) - 1 + len(extruder_recipes),
            "documented_compatibility_bridges": 1,
            "unit_scale": rules_document["unit_scale"],
        },
        "skip_reasons": [
            {
                "code": reason,
                "stage": "MaterialRuleExpansion",
                "count": "runtime-dependent",
            }
            for reason in baseline["skip_reasons"]
        ],
        "recipe_ids": {
            "count": authored_projected,
        },
        "recipe_signatures": {
            "count": authored_projected,
        },
        "shadow_signatures": {
            "total": authored_projected,
            "unique": authored_projected,
            "duplicates": 0,
        },
        "extruder_templates": {
            "status": "playable_compact_exact_sparse_projection",
            "source": "tools/gt6_extruder_templates_index_v5.json",
            "compact_source": "tools/extruder_compact.json",
            "expected_source": "tools/extruder_expected.json",
            "legacy_replay": "tools/extruder_legacy_replay.json",
            "authored_entries": len(compact_templates),
            "logical_relations": len(extruder_recipes),
            "runtime_publication": len(extruder_recipes),
            "datapack_entry_delta": len(compact_templates) - len(extruder_recipes),
            "family_compression_ratio": (
                len(extruder_recipes) / len(compact_templates)
            ),
            "registration_gate": (
                "src/main/resources/data/cruciblecraft/material_registration_gate.json"
            ),
            "prefix_count": len(allowed_prefixes),
            "classified": len(classifications),
            "unclassified": 0,
            "classification_counts": classification_counts,
            "skip_reasons": skip_reasons,
        },
        "provenance": {
            "migrated_from": baseline["captured_from"],
            "baseline_capture": baseline["capture_test"],
            "baseline_scope": "GT6-derived pre-acceptance component baseline",
            "acceptance_overlay": (
                "acceptance-required form corrections; explicitly not "
                "claimed as the original GT6 registration gate"
            ),
            "authoring_sources": [
                f"tools/component_rule_sources/{name}" for name in SOURCE_FILES
            ],
            "runtime_expander": (
                "com.masson.cruciblecraft.recipe.rule.MaterialRuleExpansion"
            ),
            "runtime_loader": (
                "com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader"
            ),
            "generated_root": (
                "src/component_rule_generated/resources/data/"
                "cruciblecraft/recipe"
            ),
        },
        "generated_tree": {
            "files": len(generated),
        },
    }
    return BuildBundle(
        generated,
        any_rubber_tag,
        _stable_bytes(manifest),
    )


def write_bundle(
    bundle: BuildBundle,
    output_root: Path = OUTPUT_ROOT,
    manifest_path: Path = MANIFEST,
) -> None:
    tag_output = (
        ANY_RUBBER_TAG_OUTPUT
        if output_root == OUTPUT_ROOT
        else output_root.parent / "any_rubber_plates.json"
    )
    if output_root.exists():
        shutil.rmtree(output_root)
    for relative, content in bundle.generated.items():
        path = output_root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
    tag_output.parent.mkdir(parents=True, exist_ok=True)
    tag_output.write_bytes(bundle.any_rubber_tag)
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_bytes(bundle.manifest)


def check_bundle(
    bundle: BuildBundle,
    output_root: Path = OUTPUT_ROOT,
    manifest_path: Path = MANIFEST,
) -> list[str]:
    errors: list[str] = []
    tag_output = (
        ANY_RUBBER_TAG_OUTPUT
        if output_root == OUTPUT_ROOT
        else output_root.parent / "any_rubber_plates.json"
    )
    expected = set(bundle.generated)
    actual = (
        {
            path.relative_to(output_root).as_posix()
            for path in output_root.rglob("*.json")
            if path.is_file()
        }
        if output_root.is_dir()
        else set()
    )
    for missing in sorted(expected - actual):
        errors.append(f"missing generated recipe: {missing}")
    for extra in sorted(actual - expected):
        errors.append(f"unexpected generated recipe: {extra}")
    for relative in sorted(expected & actual):
        if not _json_equal(
            (output_root / relative).read_bytes(),
            bundle.generated[relative],
        ):
            errors.append(f"generated recipe content drift: {relative}")
    if not manifest_path.is_file():
        errors.append(f"missing manifest: {manifest_path}")
    elif not _json_equal(manifest_path.read_bytes(), bundle.manifest):
        errors.append(f"manifest content drift: {manifest_path}")
    if not tag_output.is_file():
        errors.append(f"missing generated tag: {tag_output}")
    elif not _json_equal(tag_output.read_bytes(), bundle.any_rubber_tag):
        errors.append(
            f"generated tag content drift: {tag_output}"
        )
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="validate sources and require generated JSON to match semantically",
    )
    parser.add_argument(
        "--write",
        action="store_true",
        help="write generated component MaterialRule recipes",
    )
    args = parser.parse_args(argv)
    if args.check == args.write:
        parser.error("choose exactly one of --check or --write")
    try:
        bundle = build_bundle()
        if args.check:
            errors = check_bundle(bundle)
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print(
                f"Component rules are current: {len(bundle.generated)} sources -> "
                f"{json.loads(bundle.manifest)['expanded_recipes']} expansions; "
                "62 extruder templates classified, 0 unclassified."
            )
            return 0
        write_bundle(bundle)
        print(
            f"Wrote {len(bundle.generated)} component MaterialRule recipes and "
            f"{MANIFEST.relative_to(ROOT).as_posix()}."
        )
        return 0
    except SourceError as exc:
        print(f"component rule build failed: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
