#!/usr/bin/env python3
"""Build and check T35 ordinary_optional recipe families.

Consumes ``tools/t22_5_row_classification.json`` and compatible linked
artifacts to group all 78,682 ``ordinary_optional`` rows into stable
portfolio families keyed by CC host map plus semantic template fingerprint.

Modes:
  --check --reference-only   validate the committed compact artifact
                             without the GT6 dump (ordinary CI)
  --write --full-replay      rebuild from dump maps and write the artifact
  --check --full-replay      byte-exact replay check against the dump
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "tools"))

from gt6_recipe_templates import (  # noqa: E402
    _stable_json,
    extract_map_templates,
    recipe_soft_key,
)
from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
BUILDER = Path(__file__).resolve()
ROW_CLASSIFICATION = TOOLS / "t22_5_row_classification.json"
TEMPLATE_DENOMINATOR = TOOLS / "t21_template_denominator.json"
SHAPE_ANALYSIS = TOOLS / "t22_5_shape_analysis.json"
MACHINE_PLAYABILITY = TOOLS / "t22_5_machine_playability.json"
MIXER_INDEX = TOOLS / "gt6_mixer_templates_index.json"
TEMPLATE_MODULE = TOOLS / "gt6_recipe_templates.py"
DUMP_MAPS = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"
OUTPUT = TOOLS / "t35_recipe_families.json"

SOURCE_REVISION = common.SOURCE_REVISION
ORDINARY_OPTIONAL_ROWS = 78682
GROUPING_METHOD = (
    "semantic_template: non-mixer rows grouped by gt6_recipe_templates "
    "soft-key skeleton templates filtered to ordinary_optional; mixer rows "
    "grouped by gt6_mixer_templates_index template_id"
)

# Nine non-mixer maps covered by T22.5 A1 shape analysis.
SHAPE_MAPS = (
    "gt.recipe.bath",
    "gt.recipe.smelter",
    "gt.recipe.assembler",
    "gt.recipe.compressor",
    "gt.recipe.centrifuge",
    "gt.recipe.autoclave",
    "gt.recipe.electrolyzer",
    "gt.recipe.drying",
    "gt.recipe.roaster",
)

COMPACT_INPUTS: tuple[tuple[Path, str], ...] = (
    (ROW_CLASSIFICATION, common.relative(ROW_CLASSIFICATION)),
    (TEMPLATE_DENOMINATOR, common.relative(TEMPLATE_DENOMINATOR)),
    (SHAPE_ANALYSIS, common.relative(SHAPE_ANALYSIS)),
    (MACHINE_PLAYABILITY, common.relative(MACHINE_PLAYABILITY)),
    (MIXER_INDEX, common.relative(MIXER_INDEX)),
    (TEMPLATE_MODULE, common.relative(TEMPLATE_MODULE)),
)


def gt_to_cc_host_map(gt_map: str) -> str:
    if not gt_map.startswith("gt.recipe."):
        raise ValueError(f"unexpected gt map id: {gt_map}")
    return f"cruciblecraft:{gt_map.split('.', 2)[2]}"


def _playability_by_map() -> dict[str, dict[str, Any]]:
    document = common.load_json(MACHINE_PLAYABILITY)
    return {
        str(row.get("map") or ""): row
        for row in document.get("records") or []
        if isinstance(row, dict) and row.get("map")
    }


def _host_evidence(cc_map: str, playability: dict[str, dict[str, Any]]) -> dict[str, Any]:
    row = playability.get(cc_map)
    if row is None:
        return {
            "cc_host_map": cc_map,
            "status": "missing",
            "authored_gt_recipes": 0,
            "material_rule_files": 0,
            "logical_lower_bound": 0,
            "evidence": "no t22_5_machine_playability record",
        }
    status = str(row.get("status") or "unknown")
    if status == "registered_zero_logical":
        normalized = "registered_zero_logical"
    elif status == "registered_playable":
        normalized = "registered_playable"
    elif status == "not_implemented":
        normalized = "missing"
    else:
        normalized = status
    return {
        "cc_host_map": cc_map,
        "status": normalized,
        "authored_gt_recipes": int(row.get("authored_gt_recipes") or 0),
        "material_rule_files": int(row.get("material_rule_files") or 0),
        "logical_lower_bound": int(row.get("logical_lower_bound") or 0),
        "evidence": "tools/t22_5_machine_playability.json",
    }


def _validate_classification(classification: dict[str, Any]) -> None:
    counts = classification.get("counts") or {}
    by_class = counts.get("by_class") or {}
    if int(by_class.get("ordinary_optional") or 0) != ORDINARY_OPTIONAL_ROWS:
        raise ValueError(
            "t22_5_row_classification ordinary_optional count drifted "
            f"(got {by_class.get('ordinary_optional')})"
        )
    if int(counts.get("unclassified") or 0) != 0:
        raise ValueError("t22_5_row_classification has unclassified rows")


def _ordinary_optional_rows(
    classification: dict[str, Any],
) -> tuple[set[tuple[int, int]], list[dict[str, Any]]]:
    classes = classification["classes"]
    oo_index = classes.index("ordinary_optional")
    non_mixer = {
        (row[0], row[1])
        for row in classification.get("non_mixer_rows") or []
        if row[2] == oo_index
    }
    mixer_templates = [
        row
        for row in classification.get("mixer_templates") or []
        if row.get("class") == "ordinary_optional"
    ]
    return non_mixer, mixer_templates


def _dump_map_hashes() -> dict[str, str]:
    hashes: dict[str, str] = {}
    for map_name in SHAPE_MAPS:
        path = DUMP_MAPS / f"{map_name}.json"
        if not path.is_file():
            raise OSError(
                f"missing GT6 dump map required for full replay: "
                f"{common.relative(path)}"
            )
        hashes[common.relative(path)] = common.sha256_file(path)
    return hashes


def _load_map_recipes(map_name: str) -> list[dict[str, Any]]:
    path = DUMP_MAPS / f"{map_name}.json"
    if not path.is_file():
        raise OSError(
            f"missing GT6 dump map required for semantic template grouping: "
            f"{common.relative(path)}"
        )
    document = common.load_json(path)
    if document.get("nameInternal") != map_name:
        raise ValueError(f"map identity mismatch: {map_name}")
    return list(document.get("recipes") or [])


def _recipe_template_ids(
    map_name: str,
    recipes: list[dict[str, Any]],
) -> dict[int, str]:
    groups: dict[tuple[Any, ...], list[int]] = defaultdict(list)
    for index, recipe in enumerate(recipes):
        if recipe.get("enabled") is False:
            continue
        groups[recipe_soft_key(recipe)].append(index)
    sorted_groups = sorted(
        groups.items(),
        key=lambda item: (-len(item[1]), _stable_json(item[0])),
    )
    membership: dict[int, str] = {}
    for group_index, (_key, members) in enumerate(sorted_groups):
        template_id = f"{map_name}#{group_index:04d}"
        for recipe_index in members:
            membership[recipe_index] = template_id
    return membership


def _template_totals(
    map_name: str,
    recipes: list[dict[str, Any]],
) -> dict[str, int]:
    totals: dict[str, int] = defaultdict(int)
    membership = _recipe_template_ids(map_name, recipes)
    for template_id in membership.values():
        totals[template_id] += 1
    return dict(totals)


def _validate_shape_analysis(
    shape_analysis: dict[str, Any],
    non_mixer_oo_by_map: Counter[str],
    semantic_family_counts: Counter[str],
) -> None:
    per_map = {
        str(row.get("map") or ""): row
        for row in shape_analysis.get("per_map") or []
        if isinstance(row, dict)
    }
    for map_name in sorted(non_mixer_oo_by_map):
        if map_name not in per_map:
            raise ValueError(f"shape analysis missing map {map_name}")
        families_for_map = semantic_family_counts[map_name]
        template_count = int(per_map[map_name].get("template_count") or 0)
        if families_for_map > template_count:
            raise ValueError(
                f"{map_name}: ordinary_optional families {families_for_map} "
                f"exceed shape-analysis template_count {template_count}"
            )


def _membership_evidence(
    *,
    membership_kind: str,
    template_id: str,
    ordinary_optional_rows: int,
    template_total_rows: int | None = None,
    ledger_unit_count: int | None = None,
    template_index: int | None = None,
    mixer_template_id: str | None = None,
) -> dict[str, Any]:
    evidence: dict[str, Any] = {
        "kind": membership_kind,
        "template_id": template_id,
        "ordinary_optional_rows": ordinary_optional_rows,
    }
    if template_total_rows is not None:
        evidence["template_total_rows"] = template_total_rows
    if ledger_unit_count is not None:
        evidence["ledger_unit_count"] = ledger_unit_count
    if template_index is not None:
        evidence["template_index"] = template_index
    if mixer_template_id is not None:
        evidence["mixer_template_id"] = mixer_template_id
    return evidence


def recompute_membership(
    classification: dict[str, Any] | None = None,
    ledger: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Recompute exact ordinary_optional membership from dump replay inputs."""
    classification = classification or common.load_json(ROW_CLASSIFICATION)
    ledger = ledger or common.load_json(TEMPLATE_DENOMINATOR)
    _validate_classification(classification)
    maps = ledger["encoding"]["maps"]
    oo_rows, mixer_templates = _ordinary_optional_rows(classification)

    assigned_rows: set[tuple[int, int]] = set()
    family_counts: Counter[str] = Counter()
    non_mixer_oo_by_map: Counter[str] = Counter()
    semantic_family_counts: Counter[str] = Counter()

    for map_name in SHAPE_MAPS:
        if map_name not in maps:
            raise ValueError(f"ledger missing map {map_name}")
        map_index = maps.index(map_name)
        recipes = _load_map_recipes(map_name)
        membership = _recipe_template_ids(map_name, recipes)
        groups: dict[str, list[tuple[int, int]]] = defaultdict(list)
        for recipe_index, template_id in membership.items():
            row_key = (map_index, recipe_index)
            if row_key in oo_rows:
                groups[template_id].append(row_key)
        for template_id, row_keys in groups.items():
            if not row_keys:
                continue
            if any(key in assigned_rows for key in row_keys):
                raise ValueError(
                    f"duplicate row assignment in template {template_id}"
                )
            assigned_rows.update(row_keys)
            family_counts[template_id] += len(row_keys)
            non_mixer_oo_by_map[map_name] += len(row_keys)
            semantic_family_counts[map_name] += 1

    mixer_rows = 0
    for item in mixer_templates:
        template_index = int(item["template_index"])
        row_count = int(item["rows"])
        assigned_rows.add(("mixer", template_index))
        mixer_rows += row_count
        family_counts[str(item["template_id"])] += row_count

    non_mixer_assigned = {key for key in assigned_rows if isinstance(key[0], int)}
    if non_mixer_assigned != oo_rows:
        missing = sorted(oo_rows - non_mixer_assigned)
        extra = sorted(non_mixer_assigned - oo_rows)
        raise ValueError(
            f"non-mixer membership mismatch missing={len(missing)} "
            f"extra={len(extra)}"
        )

    assigned_count = len(non_mixer_assigned) + mixer_rows
    if assigned_count != ORDINARY_OPTIONAL_ROWS:
        raise ValueError(
            f"assigned_rows={assigned_count}, expected {ORDINARY_OPTIONAL_ROWS}"
        )

    return {
        "assigned_rows": assigned_count,
        "families": len(family_counts),
        "non_mixer_families": sum(semantic_family_counts.values()),
        "mixer_families": len(mixer_templates),
        "non_mixer_oo_by_map": dict(sorted(non_mixer_oo_by_map.items())),
        "semantic_family_counts": dict(sorted(semantic_family_counts.items())),
    }


def build() -> dict[str, Any]:
    classification = common.load_json(ROW_CLASSIFICATION)
    ledger = common.load_json(TEMPLATE_DENOMINATOR)
    shape_analysis = common.load_json(SHAPE_ANALYSIS)
    playability = _playability_by_map()
    _validate_classification(classification)

    maps = ledger["encoding"]["maps"]
    oo_rows, mixer_templates = _ordinary_optional_rows(classification)

    pending: dict[str, dict[str, Any]] = {}
    assigned_rows: set[tuple[int, int]] = set()
    non_mixer_oo_by_map: Counter[str] = Counter()
    semantic_family_counts: Counter[str] = Counter()

    for map_name in SHAPE_MAPS:
        map_index = maps.index(map_name)
        recipes = _load_map_recipes(map_name)
        extract_map_templates(map_name, recipes)
        membership = _recipe_template_ids(map_name, recipes)
        totals = _template_totals(map_name, recipes)
        groups: dict[str, list[tuple[int, int]]] = defaultdict(list)
        for recipe_index, template_id in membership.items():
            row_key = (map_index, recipe_index)
            if row_key in oo_rows:
                groups[template_id].append(row_key)

        for template_id in sorted(groups):
            row_keys = groups[template_id]
            if not row_keys:
                continue
            if any(key in assigned_rows for key in row_keys):
                raise ValueError(
                    f"duplicate row assignment in template {template_id}"
                )
            assigned_rows.update(row_keys)

            gt_map = map_name
            cc_map = gt_to_cc_host_map(gt_map)
            template_key = template_id
            family_id = f"portfolio:track_a/{cc_map}/{template_key}"
            if family_id in pending:
                raise ValueError(f"duplicate family id {family_id}")
            pending[family_id] = {
                "family_id": family_id,
                "gt_source_map": gt_map,
                "cc_host_map": cc_map,
                "template_key": template_key,
                "template_fingerprint": template_id,
                "classification": "ordinary_optional",
                "membership_kind": "semantic_template",
                "expanded_count": len(row_keys),
                "membership_evidence": _membership_evidence(
                    membership_kind="semantic_template",
                    template_id=template_id,
                    ordinary_optional_rows=len(row_keys),
                    template_total_rows=totals[template_id],
                    ledger_unit_count=len(row_keys),
                ),
                "host": _host_evidence(cc_map, playability),
                "evidence": {
                    "grouping": "gt6_recipe_templates soft-key skeleton",
                    "shape_analysis_map": map_name,
                    "template_index": None,
                    "mixer_template_id": None,
                },
            }
            non_mixer_oo_by_map[map_name] += len(row_keys)
            semantic_family_counts[map_name] += 1

    _validate_shape_analysis(
        shape_analysis,
        non_mixer_oo_by_map,
        semantic_family_counts,
    )

    mixer_templates_list = common.load_json(MIXER_INDEX)["templates"]
    for item in mixer_templates:
        template_index = int(item["template_index"])
        template_id = str(item["template_id"])
        row_count = int(item["rows"])
        if template_index >= len(mixer_templates_list):
            raise ValueError(f"missing mixer template index {template_index}")
        template = mixer_templates_list[template_index]
        if str(template.get("template_id") or "") != template_id:
            raise ValueError(
                f"mixer template index {template_index} id mismatch"
            )
        gt_map = "gt.recipe.mixer"
        cc_map = gt_to_cc_host_map(gt_map)
        template_key = template_id
        family_id = f"portfolio:track_a/{cc_map}/{template_key}"
        if family_id in pending:
            raise ValueError(f"duplicate family id {family_id}")
        pending[family_id] = {
            "family_id": family_id,
            "gt_source_map": gt_map,
            "cc_host_map": cc_map,
            "template_key": template_key,
            "template_fingerprint": template_id,
            "classification": "ordinary_optional",
            "membership_kind": "mixer_template",
            "expanded_count": row_count,
            "membership_evidence": _membership_evidence(
                membership_kind="mixer_template",
                template_id=template_id,
                ordinary_optional_rows=row_count,
                template_index=template_index,
                mixer_template_id=template_id,
            ),
            "host": _host_evidence(cc_map, playability),
            "evidence": {
                "grouping": "gt6_mixer_templates_index",
                "shape_analysis_map": None,
                "template_index": template_index,
                "mixer_template_id": template_id,
            },
        }
        assigned_rows.add(("mixer", template_index))

    recomputed = recompute_membership(classification, ledger)
    if recomputed["assigned_rows"] != ORDINARY_OPTIONAL_ROWS:
        raise ValueError("recomputed membership drifted")

    families = [pending[key] for key in sorted(pending)]
    host_map_counts: dict[str, int] = defaultdict(int)
    host_map_families: dict[str, int] = defaultdict(int)
    for family in families:
        host_map_counts[family["cc_host_map"]] += family["expanded_count"]
        host_map_families[family["cc_host_map"]] += 1

    by_host_map = {
        host_map: {
            "families": host_map_families[host_map],
            "expanded_count": host_map_counts[host_map],
            "host": _host_evidence(host_map, playability),
        }
        for host_map in sorted(host_map_counts)
    }

    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        **{
            relative: common.sha256_file(path)
            for path, relative in COMPACT_INPUTS
        },
    }
    compact_source_hashes = {
        relative: common.sha256_file(path)
        for path, relative in COMPACT_INPUTS
    }

    return {
        "schema_version": 2,
        "status": "T35_RECIPE_FAMILIES_READY",
        "source_revision": SOURCE_REVISION,
        "generated_by": "python tools/build_t35_recipe_families.py",
        "grouping": {
            "method": GROUPING_METHOD,
            "non_mixer_maps": list(SHAPE_MAPS),
            "mixer_map": "gt.recipe.mixer",
        },
        "proof_tier": {
            "compact": "reference-only CI via source_hashes and crosswalks",
            "full_replay": "gt6_dump map replay for semantic template grouping",
        },
        "currentness": {"owned_inputs": owned_inputs},
        "counts": {
            "ordinary_optional_rows": ORDINARY_OPTIONAL_ROWS,
            "assigned_rows": recomputed["assigned_rows"],
            "unassigned_rows": 0,
            "duplicate_assignments": 0,
            "families": len(families),
            "non_mixer_families": recomputed["non_mixer_families"],
            "mixer_families": recomputed["mixer_families"],
        },
        "membership": {
            "total": ORDINARY_OPTIONAL_ROWS,
            "assigned": recomputed["assigned_rows"],
            "unassigned": 0,
            "duplicate": 0,
            "recomputable": True,
        },
        "by_host_map": by_host_map,
        "families": families,
        "source_hashes": compact_source_hashes,
        "full_replay": {
            "replay_verified": True,
            "dump_map_hashes": _dump_map_hashes(),
        },
    }


def _validate_family_records(families: list[dict[str, Any]]) -> list[str]:
    errors: list[str] = []
    forbidden_fields = {"source_row_keys"}
    for family in families:
        for field in forbidden_fields:
            if field in family:
                errors.append(f"family {family.get('family_id')} retains {field}")
        template_key = str(family.get("template_key") or "")
        family_id = str(family.get("family_id") or "")
        if template_key.startswith("sha256:") and family.get("membership_kind") != "mixer_template":
            errors.append(
                f"non-mixer family {family_id} uses ledger-style template_key"
            )
        if family.get("membership_kind") == "semantic_template":
            if "#" not in template_key:
                errors.append(
                    f"semantic family {family_id} lacks template group index"
                )
            if template_key.startswith("sha256:"):
                errors.append(
                    f"semantic family {family_id} uses row-level sha256 key"
                )
        evidence = family.get("membership_evidence") or {}
        if int(evidence.get("ordinary_optional_rows") or 0) != int(
            family.get("expanded_count") or 0
        ):
            errors.append(
                f"membership_evidence mismatch for {family.get('family_id')}"
            )
    return errors


def _validate_counts_and_membership(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    counts = document.get("counts") or {}
    membership = document.get("membership") or {}
    if counts.get("ordinary_optional_rows") != ORDINARY_OPTIONAL_ROWS:
        errors.append("ordinary_optional_rows count drifted")
    if counts.get("assigned_rows") != ORDINARY_OPTIONAL_ROWS:
        errors.append("assigned_rows != 78682")
    if counts.get("unassigned_rows") != 0:
        errors.append("unassigned_rows != 0")
    if counts.get("duplicate_assignments") != 0:
        errors.append("duplicate_assignments != 0")
    if membership.get("unassigned") != 0 or membership.get("duplicate") != 0:
        errors.append("membership duplicate/unassigned != 0")
    if counts.get("families", ORDINARY_OPTIONAL_ROWS) >= ORDINARY_OPTIONAL_ROWS // 2:
        errors.append("families lack meaningful semantic aggregation")
    if int(counts.get("non_mixer_families") or 0) + int(
        counts.get("mixer_families") or 0
    ) != int(counts.get("families") or -1):
        errors.append("non_mixer_families + mixer_families != families")
    return errors


def _validate_compact_source_hashes(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    on_disk_hashes = document.get("source_hashes") or {}
    for path, relative in COMPACT_INPUTS:
        if on_disk_hashes.get(relative) != common.sha256_file(path):
            errors.append(f"source_hashes.{relative} is stale")
    owned = document.get("currentness", {}).get("owned_inputs") or {}
    builder_relative = common.relative(BUILDER)
    if owned.get(builder_relative) != common.sha256_file(BUILDER):
        errors.append("currentness.owned_inputs builder hash is stale")
    for path, relative in COMPACT_INPUTS:
        if owned.get(relative) != common.sha256_file(path):
            errors.append(f"currentness.owned_inputs.{relative} is stale")
    return errors


def _validate_against_classification(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    classification = common.load_json(ROW_CLASSIFICATION)
    ledger = common.load_json(TEMPLATE_DENOMINATOR)
    try:
        _validate_classification(classification)
    except ValueError as error:
        errors.append(str(error))
        return errors

    maps = ledger["encoding"]["maps"]
    oo_index = classification["classes"].index("ordinary_optional")
    expected_non_mixer: Counter[str] = Counter()
    for map_index, recipe_index, class_index in (
        classification.get("non_mixer_rows") or []
    ):
        if class_index != oo_index:
            continue
        expected_non_mixer[maps[map_index]] += 1

    observed_non_mixer: Counter[str] = Counter()
    for family in document.get("families") or []:
        if family.get("membership_kind") != "semantic_template":
            continue
        observed_non_mixer[str(family.get("gt_source_map") or "")] += int(
            family.get("expanded_count") or 0
        )

    for map_name in SHAPE_MAPS:
        if observed_non_mixer[map_name] != expected_non_mixer[map_name]:
            errors.append(
                f"{map_name}: family expanded_count "
                f"{observed_non_mixer[map_name]} != classification "
                f"{expected_non_mixer[map_name]}"
            )

    expected_mixer = sorted(
        (
            int(row["template_index"]),
            str(row["template_id"]),
            int(row["rows"]),
        )
        for row in classification.get("mixer_templates") or []
        if row.get("class") == "ordinary_optional"
    )
    observed_mixer = sorted(
        (
            int((family.get("membership_evidence") or {}).get("template_index")),
            str(family.get("template_fingerprint") or ""),
            int(family.get("expanded_count") or 0),
        )
        for family in document.get("families") or []
        if family.get("membership_kind") == "mixer_template"
    )
    if expected_mixer != observed_mixer:
        errors.append("mixer families drift vs t22_5_row_classification")

    mixer_rows = sum(row[2] for row in expected_mixer)
    non_mixer_rows = sum(expected_non_mixer.values())
    if non_mixer_rows + mixer_rows != ORDINARY_OPTIONAL_ROWS:
        errors.append("classification crosswalk does not sum to 78682")
    return errors


def _validate_against_shape_analysis(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    shape_analysis = common.load_json(SHAPE_ANALYSIS)
    per_map = {
        str(row.get("map") or ""): row
        for row in shape_analysis.get("per_map") or []
        if isinstance(row, dict)
    }
    families_by_map: Counter[str] = Counter()
    for family in document.get("families") or []:
        if family.get("membership_kind") != "semantic_template":
            continue
        families_by_map[str(family.get("gt_source_map") or "")] += 1
    for map_name, family_count in sorted(families_by_map.items()):
        row = per_map.get(map_name)
        if row is None:
            errors.append(f"shape analysis missing map {map_name}")
            continue
        template_count = int(row.get("template_count") or 0)
        if family_count > template_count:
            errors.append(
                f"{map_name}: families {family_count} exceed shape-analysis "
                f"template_count {template_count}"
            )
    return errors


def _validate_by_host_map(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    observed: Counter[str] = Counter()
    families_by_host: Counter[str] = Counter()
    for family in document.get("families") or []:
        host_map = str(family.get("cc_host_map") or "")
        observed[host_map] += int(family.get("expanded_count") or 0)
        families_by_host[host_map] += 1
    for host_map, block in (document.get("by_host_map") or {}).items():
        if int(block.get("expanded_count") or 0) != observed[host_map]:
            errors.append(f"by_host_map expanded_count mismatch for {host_map}")
        if int(block.get("families") or 0) != families_by_host[host_map]:
            errors.append(f"by_host_map families mismatch for {host_map}")
    expanded = sum(observed.values())
    if expanded != ORDINARY_OPTIONAL_ROWS:
        errors.append("family expanded_count sum != 78682")
    return errors


def reference_only_check() -> list[str]:
    """Validate the committed compact artifact without GT6 dump replay."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {common.relative(OUTPUT)}")
        return errors

    try:
        document = common.load_json(OUTPUT)
    except json.JSONDecodeError as error:
        return [f"malformed artifact JSON: {error}"]

    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != common.stable_json(document):
        errors.append(f"{common.relative(OUTPUT)} is not canonical JSON")

    if document.get("schema_version") != 2:
        errors.append("schema_version != 2")
    if document.get("status") != "T35_RECIPE_FAMILIES_READY":
        errors.append("status != T35_RECIPE_FAMILIES_READY")
    if document.get("source_revision") != SOURCE_REVISION:
        errors.append("source_revision drifted")

    errors.extend(_validate_counts_and_membership(document))

    families = document.get("families") or []
    if not isinstance(families, list) or not families:
        errors.append("families must be a non-empty list")
        return errors

    errors.extend(_validate_family_records(families))
    family_ids = [item.get("family_id") for item in families]
    if len(family_ids) != len(set(family_ids)):
        errors.append("duplicate family_id values on disk")

    errors.extend(_validate_compact_source_hashes(document))
    errors.extend(_validate_against_classification(document))
    errors.extend(_validate_against_shape_analysis(document))
    errors.extend(_validate_by_host_map(document))

    full_replay = document.get("full_replay") or {}
    if not isinstance(full_replay.get("dump_map_hashes"), dict):
        errors.append("full_replay.dump_map_hashes must be present")
    elif full_replay.get("replay_verified") is not True:
        errors.append("full_replay.replay_verified must be true")
    return errors


def check() -> list[str]:
    """Full replay check: compact invariants plus byte-exact rebuild."""
    errors = reference_only_check()
    if errors:
        return errors
    try:
        expected = common.stable_json(build())
    except OSError as error:
        errors.append(str(error))
        return errors
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        errors.append(f"{common.relative(OUTPUT)} is stale under full replay")
        return errors

    document = common.load_json(OUTPUT)
    recorded = (document.get("full_replay") or {}).get("dump_map_hashes") or {}
    for map_name in SHAPE_MAPS:
        path = DUMP_MAPS / f"{map_name}.json"
        relative = common.relative(path)
        if recorded.get(relative) != common.sha256_file(path):
            errors.append(f"full_replay.dump_map_hashes.{relative} is stale")
    return errors


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    replay_mode = parser.add_mutually_exclusive_group()
    replay_mode.add_argument("--reference-only", action="store_true")
    replay_mode.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)

    if args.check and not (args.reference_only or args.full_replay):
        parser.error("--check requires --reference-only or --full-replay")
    if args.write and not args.full_replay:
        parser.error("--write requires --full-replay")
    if args.reference_only and not args.check:
        parser.error("--reference-only requires --check")
    if args.full_replay and not (args.check or args.write):
        parser.error("--full-replay requires --check or --write")

    selected = [flag for flag in (args.write, args.check) if flag]
    if len(selected) != 1:
        parser.error("choose exactly one of --write, --check")

    try:
        if args.write:
            document = write()
            tier = "full replay"
        else:
            errors = (
                reference_only_check()
                if args.reference_only
                else check()
            )
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(OUTPUT)
            tier = "compact" if args.reference_only else "full replay"
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T35 recipe families failed: {error}", file=sys.stderr)
        return 1

    print(
        json.dumps(
            {
                "counts": document.get("counts"),
                "proof_tier": tier,
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
