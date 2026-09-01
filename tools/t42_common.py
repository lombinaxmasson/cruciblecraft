#!/usr/bin/env python3
"""Shared T42 partition constants, exclude-set accounting, and classifiers."""
from __future__ import annotations

import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, Iterable

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import builder_cli
from tools import t35_common as t35
from tools import t37_common as t37
from tools import t38_common as t38
from tools import t39_common as t39
from tools import t40_common as t40
from tools import t41_common as t41
from tools.build_t37_assembler_source import VANILLA_RUNTIME_ALIASES as T37_VANILLA_ALIASES

ROOT = t35.ROOT
TOOLS = t35.TOOLS
SOURCE_REVISION = t35.SOURCE_REVISION

OPENING_EXECUTION_GAP = 5305
EXPECTED_T35_FAMILIES = 5718
ORDINARY_OPTIONAL_ROWS = 78682
T35_FOUNDATION = dict(t39.T35_FOUNDATION)

OWNER = "portfolio:track_a/t42_partition"
DUMP_MAPS = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"
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
MIXER_MAP = "gt.recipe.mixer"

PARTITION_FREEZE = TOOLS / "t42_partition_freeze.json"
REMAINING_CATALOG = TOOLS / "t42_remaining_catalog.json"
SOURCE_PACK_MANIFEST = TOOLS / "t42_source_pack_manifest.json"
FAMILY_OPERAND_SNAPSHOT = TOOLS / "t42_family_operand_snapshot.json"
RUNTIME_INVENTORY = TOOLS / "t42_runtime_expression_inventory.json"
VANILLA_ALLOWLIST = TOOLS / "t42_vanilla_item_allowlist.json"
REACHABILITY_BASELINE = TOOLS / "t42_reachability_baseline.json"
BLOCKER_OVERLAY = TOOLS / "t42_blocker_overlay.json"
OPERAND_DISPOSITION = TOOLS / "t42_operand_disposition.json"
WAVE_CANDIDATES = TOOLS / "t42_wave_candidates.json"
DISPOSITION_LOCK = TOOLS / "t42_disposition_lock.json"
GAP_PARTITION = TOOLS / "t42_gap_partition.json"
CENSUS_DELTA = TOOLS / "t42_census_delta.json"
CARD_TOPOLOGY = TOOLS / "t42_card_topology.json"
READINESS = TOOLS / "t42_readiness.json"
REPAIR_FREEZE = TOOLS / "t42_repair_pre_freeze.json"
REPAIR_READINESS = TOOLS / "t42_repair_readiness.json"
REPAIR_GIANT_ARTIFACTS = (
    "tools/t42_family_operand_snapshot.json",
    "tools/t42_blocker_overlay.json",
    "tools/t42_disposition_lock.json",
    "tools/t42_runtime_expression_inventory.json",
    "tools/t42_vanilla_item_allowlist.json",
    "tools/t42_gap_partition.json",
)

MATERIAL_GATE = (
    ROOT / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
MATERIAL_FORM_AUTHORITY = TOOLS / "material_form_authority.json"
PREFIX_INDEX = (
    ROOT / "src/main/resources/data/cruciblecraft/material_prefixes/index.json"
)
PREFIX_ROOT = ROOT / "src/main/resources/data/cruciblecraft/material_prefixes"
FLUID_MAPPING = TOOLS / "t22_5_fluid_mapping.json"
ROW_CLASSIFICATION = TOOLS / "t22_5_row_classification.json"
T21_REACHABILITY = TOOLS / "t21_operand_reachability.json"
T21_TEMPLATE_DENOMINATOR = TOOLS / "t21_template_denominator.json"
L1B_OPERANDS = TOOLS / "gt6_l1b_selected_recipe_operands.json"
MIXER_MEMBERSHIP = TOOLS / "gt6_mixer_templates_membership.json"
OREDICT_XREF = TOOLS / "gt6_oredict_cross_reference.json"
MOD_COMPONENTS_JAVA = (
    ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModComponents.java"
)
T35_RUNTIME_REGISTRY = t35.RUNTIME_REGISTRY
T35_RUNTIME_GATE_FIXTURE = (
    ROOT / "src/main/resources/census/t35_runtime_registry_gate.json"
)
T39_PLAYER_PATH_SUPPORT = TOOLS / "t39_player_path_support.json"

PRIMARY_BUCKETS = (
    "current_closure_ready",
    "needs_prefix_or_molten",
    "needs_unique_block_or_mte",
    "combinatorial_unproven",
    "already_expressed",
)
OPERAND_STATES = (
    "proven_equivalent",
    "needs_current_expression",
    "phase_deferred",
    "unsupported",
)
KNOWN_COMBINATORIAL_TEMPLATE_KEYS = (
    "gt.recipe.assembler#0000",
    "gt.recipe.assembler#0001",
    "gt.recipe.electrolyzer#0000",
    "gt.recipe.electrolyzer#0001",
)
UNIQUE_ITEM_PREFIXES = (
    "gregtech:gt.multitileentity",
    "gregtech:gt.multiitem.",
    "gregtech:gt.block.",
)
RECYCLING_OUTPUT_FORMS = frozenset(
    {
        "dust",
        "small_dust",
        "tiny_dust",
        "ingot",
        "nugget",
        "ingot_hot",
    }
)
MATERIAL_FORM_OUTPUT_HOSTS = frozenset(
    {
        "cruciblecraft:smelter",
        "cruciblecraft:bath",
        "cruciblecraft:centrifuge",
        "cruciblecraft:autoclave",
        "cruciblecraft:compressor",
    }
)
EMPTY_ITEM_MARKERS = ("empty_slot", "gt.meta.empty", "gregtech:gt.meta.empty")
TOOL_MARKERS = (".tool.", "gt.meta.tool", "gt.tool.")
CIRCUIT_ITEM = "gregapi:gt.integrated_circuit"
PROGRAMMED_CIRCUIT = "cruciblecraft:programmed_circuit"
MOLTEN_GENERATION_TAG = "ITEMGENERATOR.MOLTEN"
SEMANTIC_ROOT_PATHS = (
    "tools/t41_census_delta.json",
    "tools/t41_readiness.json",
    "tools/t41_card_topology.json",
    "tools/t37_assembler_source.json",
    "tools/t37_census_delta.json",
    "tools/t37_publication_delta.json",
    "tools/t38_roaster_source.json",
    "tools/t38_census_delta.json",
    "tools/t39_production_lock.json",
    "tools/t39_census_delta.json",
    "tools/t40_production_lock.json",
    "tools/t40_census_delta.json",
    "tools/t41_production_lock.json",
    "tools/t35_recipe_families.json",
    "tools/t35_runtime_registry.json",
    "tools/t22_5_row_classification.json",
    "src/main/resources/data/cruciblecraft/material_registration_gate.json",
    "tools/material_form_authority.json",
    "src/main/resources/data/cruciblecraft/material_prefixes/index.json",
    "tools/t22_5_fluid_mapping.json",
    "tools/t21_operand_reachability.json",
    "tools/t41_runtime_dependency_manifest.json",
    "tools/t37_assembler_source_receipt.json",
    "tools/t38_roaster_source_receipt.json",
    "tools/t39_centrifuge_source_receipt.json",
    "tools/t40_electrolyzer_source_receipt.json",
    "tools/t41_assembler_source_receipt.json",
    "tools/gt6_l1b_selected_recipe_operands.json",
)

_CACHE: dict[str, Any] = {}


def relative(path: Path) -> str:
    return t35.relative(path)


def load_json(path: Path) -> Any:
    return t35.load_json(path)


def sha256_file(path: Path) -> str:
    return t35.sha256_file(path)


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def sha256_stable(value: Any) -> str:
    return sha256_text(t35.stable_json(value))


def parse_managed(description: str, argv: list[str] | None = None):
    return builder_cli.parse_managed(description, argv)


def parse_write_check(description: str, argv: list[str] | None = None):
    import argparse

    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    return args


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return t35.check_generated_document(path, document)


def handle_rebind(args: Any, output: Path) -> bool:
    if not getattr(args, "rebind_currentness_only", False):
        return False
    from tools import currentness

    currentness.rebind_sidecar(output)
    print(f"rebound currentness sidecar for {relative(output)}")
    return True


def file_sha256_map(paths: Iterable[str]) -> dict[str, str]:
    hashes: dict[str, str] = {}
    missing: list[str] = []
    for relative_path in paths:
        path = ROOT / relative_path
        if not path.is_file():
            missing.append(relative_path)
            continue
        hashes[relative_path] = sha256_file(path)
    if missing:
        raise ValueError("missing semantic-root files: " + ", ".join(missing))
    return hashes


def t37_closed_family_ids() -> list[str]:
    work = load_json(t37.ASSEMBLER_SOURCE).get("work_set") or {}
    ids = [str(value) for value in work.get("family_ids") or []]
    if len(ids) != 50:
        raise ValueError(f"T37 work_set family_ids must be 50, got {len(ids)}")
    return ids


def t38_closed_family_ids() -> list[str]:
    work = load_json(t38.SOURCE).get("work_set") or {}
    ids = [str(value) for value in work.get("family_ids") or []]
    if len(ids) != 29:
        raise ValueError(f"T38 work_set family_ids must be 29, got {len(ids)}")
    return ids


def exclude_family_ids() -> list[str]:
    ids = [
        *t37_closed_family_ids(),
        *t38_closed_family_ids(),
        *t39.production_family_ids(),
        *t39.phase_deferred_family_ids(),
        *t40.production_family_ids(),
        *t41.production_family_ids(),
    ]
    if len(ids) != len(set(ids)):
        raise ValueError("T42 exclude set has duplicate family ids")
    expected = 50 + 29 + 22 + 7 + 13 + 292
    if len(ids) != expected:
        raise ValueError(
            f"T42 exclude set size {len(ids)} != {expected}"
        )
    return ids


def t35_families() -> list[dict[str, Any]]:
    cached = _CACHE.get("t35_families")
    if cached is not None:
        return cached
    document = load_json(t35.RECIPE_FAMILIES)
    families = list(document.get("families") or [])
    if len(families) != EXPECTED_T35_FAMILIES:
        raise ValueError(
            f"T35 recipe families drifted: {len(families)} != {EXPECTED_T35_FAMILIES}"
        )
    _CACHE["t35_families"] = families
    _CACHE["t35_families_document"] = document
    return families


def remaining_family_rows(
    *,
    exclude: Iterable[str] | None = None,
) -> list[dict[str, Any]]:
    excluded = set(exclude if exclude is not None else exclude_family_ids())
    rows = [
        family
        for family in t35_families()
        if str(family.get("family_id") or "") not in excluded
    ]
    if exclude is None and len(rows) != OPENING_EXECUTION_GAP:
        raise ValueError(
            f"remaining family count {len(rows)} != {OPENING_EXECUTION_GAP}"
        )
    return rows


def membership_root(family: dict[str, Any]) -> str:
    payload = {
        "expanded_count": int(family.get("expanded_count") or 0),
        "family_id": str(family.get("family_id") or ""),
        "membership_evidence": family.get("membership_evidence") or {},
        "membership_kind": str(family.get("membership_kind") or ""),
        "template_key": str(family.get("template_key") or ""),
    }
    return sha256_stable(payload)


def remaining_catalog_document() -> dict[str, Any]:
    families = remaining_family_rows()
    t35_doc = _CACHE.get("t35_families_document") or load_json(t35.RECIPE_FAMILIES)
    excluded = set(exclude_family_ids())
    excluded_rows = [
        family
        for family in t35_families()
        if str(family.get("family_id") or "") in excluded
    ]
    remaining_rows = sum(int(row.get("expanded_count") or 0) for row in families)
    excluded_row_count = sum(
        int(row.get("expanded_count") or 0) for row in excluded_rows
    )
    if remaining_rows + excluded_row_count != ORDINARY_OPTIONAL_ROWS:
        raise ValueError(
            "source-row split drifted: "
            f"remaining={remaining_rows} excluded={excluded_row_count}"
        )
    t35_hosts = t35_doc.get("by_host_map") or {}
    remaining_by_host: dict[str, dict[str, int]] = defaultdict(
        lambda: {"families": 0, "ordinary_source_rows": 0}
    )
    catalog_rows: list[dict[str, Any]] = []
    seen_ids: set[str] = set()
    for family in families:
        family_id = str(family.get("family_id") or "")
        if not family_id or family_id in seen_ids:
            raise ValueError(f"duplicate or empty remaining family_id: {family_id}")
        seen_ids.add(family_id)
        host = str(family.get("cc_host_map") or "")
        expanded = int(family.get("expanded_count") or 0)
        remaining_by_host[host]["families"] += 1
        remaining_by_host[host]["ordinary_source_rows"] += expanded
        catalog_rows.append(
            {
                "cc_host_map": host,
                "expanded_count": expanded,
                "family_id": family_id,
                "membership_kind": str(family.get("membership_kind") or ""),
                "ordinary_source_rows": expanded,
                "source_map": str(family.get("gt_source_map") or ""),
                "source_relation_membership_root": membership_root(family),
                "template_key": str(family.get("template_key") or ""),
            }
        )
    catalog_rows.sort(key=lambda row: row["family_id"])
    host_reconcile: dict[str, Any] = {}
    for host, stats in sorted(t35_hosts.items()):
        historical_families = int(stats.get("families") or 0)
        historical_rows = int(stats.get("expanded_count") or 0)
        excluded_host = [
            row for row in excluded_rows if str(row.get("cc_host_map") or "") == host
        ]
        expected_families = historical_families - len(excluded_host)
        expected_rows = historical_rows - sum(
            int(row.get("expanded_count") or 0) for row in excluded_host
        )
        live = remaining_by_host.get(host) or {"families": 0, "ordinary_source_rows": 0}
        if live["families"] != expected_families or live["ordinary_source_rows"] != expected_rows:
            raise ValueError(
                f"host reconcile failed for {host}: "
                f"live={dict(live)} expected families={expected_families} rows={expected_rows}"
            )
        host_reconcile[host] = {
            "families": live["families"],
            "historical_families": historical_families,
            "historical_rows": historical_rows,
            "ordinary_source_rows": live["ordinary_source_rows"],
        }
    combinatorial_remaining = [
        row["family_id"]
        for row in catalog_rows
        if row["template_key"] in KNOWN_COMBINATORIAL_TEMPLATE_KEYS
    ]
    if len(combinatorial_remaining) != 4:
        raise ValueError(
            "T40/T41 combinatorial families must remain in the 5305 catalog, "
            f"got {len(combinatorial_remaining)}"
        )
    return {
        "by_host": host_reconcile,
        "combinatorial_remaining_family_ids": combinatorial_remaining,
        "exclude_count": len(excluded),
        "exclude_family_ids_root": sha256_stable(sorted(excluded)),
        "family_count": len(catalog_rows),
        "families": catalog_rows,
        "generated_by": "python tools/build_t42_partition_freeze.py",
        "opening_execution_gap": OPENING_EXECUTION_GAP,
        "ordinary_source_rows": remaining_rows,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "T42_REMAINING_CATALOG",
        "t35_families": EXPECTED_T35_FAMILIES,
        "t35_ordinary_optional_rows": ORDINARY_OPTIONAL_ROWS,
        "t41_remaining_opening": OPENING_EXECUTION_GAP,
    }


def intern(table: list[str], index: dict[str, int], value: str | None) -> int | None:
    if value is None:
        return None
    existing = index.get(value)
    if existing is not None:
        return existing
    next_index = len(table)
    table.append(value)
    index[value] = next_index
    return next_index


def dump_present() -> bool:
    return all((DUMP_MAPS / f"{name}.json").is_file() for name in (*SHAPE_MAPS, MIXER_MAP))


def require_dump() -> None:
    missing = [
        relative(DUMP_MAPS / f"{name}.json")
        for name in (*SHAPE_MAPS, MIXER_MAP)
        if not (DUMP_MAPS / f"{name}.json").is_file()
    ]
    if missing:
        raise OSError(
            "missing GT6 dump map required for full replay: " + ", ".join(missing)
        )


def load_map_recipes(map_name: str) -> list[dict[str, Any]]:
    path = DUMP_MAPS / f"{map_name}.json"
    if not path.is_file():
        raise OSError("missing GT6 dump map required for full replay: " + relative(path))
    document = load_json(path)
    if document.get("nameInternal") != map_name:
        raise ValueError(f"map identity mismatch: {map_name}")
    return list(document.get("recipes") or [])


def recipe_template_ids(map_name: str, recipes: list[dict[str, Any]]) -> dict[int, str]:
    from gt6_recipe_templates import _stable_json, recipe_soft_key

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


def mixer_ordinary_optional_recipe_indexes() -> dict[str, frozenset[int]]:
    cached = _CACHE.get("mixer_oo_idx")
    if cached is not None:
        return cached
    t21 = load_json(T21_TEMPLATE_DENOMINATOR)
    classes = t21["encoding"]["classes"]
    maps = t21["encoding"]["maps"]
    mixer_idx = maps.index(MIXER_MAP)
    oo = classes.index("ordinary_optional")
    template_ids = load_json(MIXER_MEMBERSHIP)["template_ids"]
    by_template: dict[str, set[int]] = defaultdict(set)
    for unit in t21.get("units") or []:
        if unit[1] != mixer_idx or unit[3] != oo or unit[2] is None:
            continue
        template_id = str(template_ids[int(unit[2])])
        by_template[template_id].update(int(index) for index in unit[4])
    allowed = {
        str(row["template_id"])
        for row in load_json(ROW_CLASSIFICATION).get("mixer_templates") or []
        if row.get("class") == "ordinary_optional"
    }
    result = {
        template_id: frozenset(indexes)
        for template_id, indexes in by_template.items()
        if template_id in allowed
    }
    _CACHE["mixer_oo_idx"] = result
    return result


def ordinary_optional_row_keys() -> set[tuple[int, int]]:
    cached = _CACHE.get("oo_rows")
    if cached is not None:
        return cached
    classification = load_json(ROW_CLASSIFICATION)
    classes = list(classification.get("classes") or [])
    oo_index = classes.index("ordinary_optional")
    maps = load_json(T21_TEMPLATE_DENOMINATOR)["encoding"]["maps"]
    rows: set[tuple[int, int]] = set()
    for entry in classification.get("non_mixer_rows") or []:
        map_index, recipe_index, class_index = entry
        if class_index == oo_index:
            rows.add((int(map_index), int(recipe_index)))
    _CACHE["oo_rows"] = rows
    _CACHE["template_maps"] = list(maps)
    return rows


def template_maps() -> list[str]:
    if "template_maps" not in _CACHE:
        ordinary_optional_row_keys()
    return list(_CACHE["template_maps"])


def row_sha256(recipe: dict[str, Any]) -> str:
    payload = {
        "canBeBuffered": recipe.get("canBeBuffered"),
        "chances": recipe.get("chances"),
        "duration": recipe.get("duration"),
        "euPerTick": recipe.get("euPerTick"),
        "fluidInputs": recipe.get("fluidInputs"),
        "fluidOutputs": recipe.get("fluidOutputs"),
        "inputs": recipe.get("inputs"),
        "maxChances": recipe.get("maxChances"),
        "notConsumed": recipe.get("notConsumed"),
        "outputs": recipe.get("outputs"),
        "specialValue": recipe.get("specialValue"),
    }
    return sha256_stable(payload)


def _looks_like_tool(item_id: str) -> bool:
    lowered = item_id.lower()
    return any(marker in lowered for marker in TOOL_MARKERS)


def classify_item_action(item: dict[str, Any] | None) -> dict[str, Any]:
    if not isinstance(item, dict):
        return {"kind": "CONSUME", "damage": 0}
    item_id = str(item.get("item") or "")
    count = int(item.get("count") or 0)
    damage = item.get("damage")
    not_consumed = bool(item.get("notConsumed") or item.get("not_consumed"))
    if isinstance(damage, int) and damage > 0:
        return {"kind": "WEAR", "damage": damage}
    if not_consumed or count == 0:
        if _looks_like_tool(item_id) and isinstance(damage, int) and damage > 0:
            return {"kind": "WEAR", "damage": damage}
        return {"kind": "PRESERVE", "damage": 0}
    return {"kind": "CONSUME", "damage": 0}


def operand_action(item: dict[str, Any] | None) -> str:
    classified = classify_item_action(item)
    kind = str(classified["kind"]).lower()
    damage = int(classified.get("damage") or 0)
    if kind == "wear" and damage > 0:
        return f"wear:{damage}"
    return kind


def empty_item(item: Any) -> bool:
    if not item or not isinstance(item, dict):
        return True
    item_id = str(item.get("item") or "")
    if not item_id:
        return True
    return any(marker in item_id for marker in EMPTY_ITEM_MARKERS)


def load_runtime_catalogs() -> dict[str, Any]:
    cached = _CACHE.get("catalogs")
    if cached is not None:
        return cached
    xref = load_json(OREDICT_XREF)
    prefix_item_to_form = {
        str(key): str(value)
        for key, value in (xref.get("prefix_item_to_form") or {}).items()
    }
    prefix_item_to_gt_prefix = {
        str(key): str(value)
        for key, value in (xref.get("prefix_item_to_gt_prefix") or {}).items()
    }
    material_id_to_cc = {
        int(key): str(value)
        for key, value in (xref.get("material_id_to_cc") or {}).items()
        if str(key).lstrip("-").isdigit()
    }
    gate = load_json(MATERIAL_GATE)
    registered_forms = {
        str(material): {str(form) for form in forms}
        for material, forms in (gate.get("materials") or {}).items()
        if isinstance(forms, list)
    }
    form_items: dict[tuple[str, str], str] = {}
    materials_root = ROOT / "src/main/resources/data/cruciblecraft/materials"
    molten_materials: set[str] = set()
    generation_tag_molten: set[str] = set()
    top_level_molten: set[str] = set()
    for path in sorted(materials_root.glob("*.json")):
        if path.name == "index.json":
            continue
        material = load_json(path)
        material_id = str(material.get("id") or path.stem)
        if material.get("molten") or "cruciblecraft:generates_molten" in (
            material.get("generation_flags") or []
        ):
            top_level_molten.add(material_id)
            molten_materials.add(material_id)
        tags = (material.get("gt6_metadata") or {}).get("generation_tags") or []
        if MOLTEN_GENERATION_TAG in tags:
            generation_tag_molten.add(material_id)
        for form, item_id in (material.get("form_items") or {}).items():
            form_items[(material_id, str(form))] = str(item_id)
    prefix_forms: set[str] = set()
    for path in sorted(PREFIX_ROOT.glob("*.json")):
        if path.name == "index.json":
            continue
        prefix = load_json(path)
        prefix_forms.add(str(prefix.get("serialized_path") or path.stem))
    fluid_to_cc: dict[str, str] = {}
    fluid_disposition: dict[str, str] = {}
    fluid_mapping: dict[str, dict[str, Any]] = {}
    fluid_doc = load_json(FLUID_MAPPING)
    for row in fluid_doc.get("mapping") or []:
        if not isinstance(row, dict):
            continue
        fluid_id = str(row.get("fluid") or "")
        cc_id = str(row.get("cc_fluid_id") or "")
        disposition = str(row.get("disposition") or "")
        if fluid_id:
            fluid_disposition[fluid_id] = disposition
            fluid_mapping[fluid_id] = {
                "cc_fluid_id": cc_id,
                "cc_material": str(row.get("cc_material") or ""),
                "derivation": str(row.get("derivation") or ""),
                "disposition": disposition,
                "material_id": row.get("material_id"),
            }
        if disposition == "mapped" and fluid_id and cc_id:
            fluid_to_cc[fluid_id] = cc_id
    builtins = (fluid_doc.get("derivation_chain") or {}).get("builtins") or {}
    for builtin, cc_id in builtins.items():
        fluid_to_cc[str(builtin)] = str(cc_id)
        fluid_disposition[str(builtin)] = "mapped"
    registry = load_json(T35_RUNTIME_REGISTRY)
    registry_ids: dict[str, set[str]] = defaultdict(set)
    for category, rows in (registry.get("categories") or {}).items():
        for row in rows or []:
            if isinstance(row, dict) and row.get("id"):
                registry_ids[str(category)].add(str(row["id"]))
    remaining_molten_fluids = {
        fluid_id
        for fluid_id in fluid_disposition
        if fluid_id.startswith("molten.") or fluid_id.startswith("molten ")
    }
    catalogs = {
        "fluid_disposition": fluid_disposition,
        "fluid_mapping": fluid_mapping,
        "fluid_to_cc": fluid_to_cc,
        "form_items": form_items,
        "generation_tag_molten": generation_tag_molten,
        "material_id_to_cc": material_id_to_cc,
        "molten_materials": molten_materials,
        "prefix_forms": prefix_forms,
        "prefix_item_to_form": prefix_item_to_form,
        "prefix_item_to_gt_prefix": prefix_item_to_gt_prefix,
        "registered_forms": registered_forms,
        "registry_ids": registry_ids,
        "remaining_molten_fluids": remaining_molten_fluids,
        "top_level_molten": top_level_molten,
        "vanilla_aliases": dict(T37_VANILLA_ALIASES),
    }
    _CACHE["catalogs"] = catalogs
    return catalogs


def material_runtime_id(catalogs: dict[str, Any], material: str, form: str) -> str:
    return catalogs["form_items"].get(
        (material, form), f"cruciblecraft:{material}/{form}"
    )


def unique_kind_from_gt_prefix(gt_prefix: str) -> str:
    if gt_prefix.lower().startswith("armor"):
        return "armor"
    if gt_prefix.startswith("toolHead") or gt_prefix.startswith("tool"):
        return "tool_head"
    if gt_prefix.lower().startswith("stone"):
        return "stone"
    if "circuit" in gt_prefix.lower():
        return "circuit"
    return f"gt_prefix:{gt_prefix}"


def unique_object_kind(item_id: str) -> str | None:
    if item_id == CIRCUIT_ITEM or item_id.endswith("gt.integrated_circuit"):
        return None
    if item_id == "gregtech:gt.multitileentity":
        return "mte"
    if item_id.startswith("gregtech:gt.multiitem."):
        return "multiitem"
    if item_id.startswith("gregtech:gt.block."):
        return "block"
    if item_id.startswith("gregapi:"):
        return "gregapi_object"
    lowered = item_id.lower()
    if "foam" in lowered or "cfoam" in lowered:
        return "c_foam"
    if "pipe" in lowered and item_id not in load_runtime_catalogs()["prefix_item_to_form"]:
        if item_id.startswith("gregtech:gt.block.") or "multitile" in item_id:
            return "pipe"
    return None


def map_item_source(
    item: dict[str, Any],
    catalogs: dict[str, Any],
) -> dict[str, Any]:
    item_id = str(item.get("item") or "")
    meta = item.get("meta")
    mapped = {
        "alias": None,
        "form": None,
        "item": item_id,
        "kind": "unknown",
        "material": None,
        "meta": meta,
        "runtime_id": None,
        "unique_kind": None,
    }
    if empty_item(item):
        mapped["kind"] = "empty"
        return mapped
    if item_id == CIRCUIT_ITEM or item_id.endswith("gt.integrated_circuit"):
        config = 0
        try:
            config = int(meta) if meta is not None else 0
        except (TypeError, ValueError):
            config = 0
        mapped.update(
            {
                "alias": PROGRAMMED_CIRCUIT,
                "components": {"cruciblecraft:circuit_config": config},
                "kind": "cc_static",
                "runtime_id": PROGRAMMED_CIRCUIT,
            }
        )
        return mapped
    alias = catalogs["vanilla_aliases"].get(item_id)
    if alias:
        mapped.update(
            {
                "alias": alias,
                "kind": "vanilla_alias",
                "runtime_id": alias,
            }
        )
        return mapped
    if item_id.startswith("minecraft:"):
        mapped.update({"kind": "vanilla_source", "runtime_id": item_id})
        return mapped
    form = catalogs["prefix_item_to_form"].get(item_id)
    material = None
    if meta is not None:
        try:
            material = catalogs["material_id_to_cc"].get(int(meta))
        except (TypeError, ValueError):
            material = None
    if form and material:
        runtime_id = material_runtime_id(catalogs, material, form)
        registered = form in catalogs["registered_forms"].get(material, set())
        mapped.update(
            {
                "form": form,
                "kind": "material_form" if registered else "missing_form",
                "material": material,
                "runtime_id": runtime_id if registered else None,
            }
        )
        return mapped
    if form and not material:
        mapped.update({"form": form, "kind": "unknown_material"})
        return mapped
    gt_prefix = catalogs["prefix_item_to_gt_prefix"].get(item_id)
    if gt_prefix and material:
        mapped.update(
            {
                "kind": "unique_object",
                "material": material,
                "unique_kind": unique_kind_from_gt_prefix(gt_prefix),
            }
        )
        return mapped
    if gt_prefix and not material:
        mapped.update(
            {
                "kind": "unknown_material",
                "unique_kind": unique_kind_from_gt_prefix(gt_prefix),
            }
        )
        return mapped
    unique_kind = unique_object_kind(item_id)
    if unique_kind:
        mapped.update({"kind": "unique_object", "unique_kind": unique_kind})
        return mapped
    if item_id.startswith("cruciblecraft:"):
        mapped.update({"kind": "cc_static", "runtime_id": item_id})
        return mapped
    mapped["kind"] = "unmapped"
    return mapped


def map_fluid_source(
    fluid: dict[str, Any],
    catalogs: dict[str, Any],
) -> dict[str, Any]:
    fluid_id = str(fluid.get("fluid") or fluid.get("id") or "")
    mapped = {
        "fluid": fluid_id,
        "kind": "unknown_fluid",
        "material": None,
        "runtime_id": None,
    }
    if not fluid_id:
        mapped["kind"] = "empty"
        return mapped
    cc_id = catalogs["fluid_to_cc"].get(fluid_id)
    disposition = catalogs["fluid_disposition"].get(fluid_id)
    material = None
    if fluid_id.startswith("molten.") or fluid_id.startswith("molten "):
        slug = fluid_id.split(".", 1)[-1].replace(" ", "_")
        material = catalogs["fluid_to_cc"].get(fluid_id)
        mapped["material"] = slug
        if cc_id:
            mapped.update({"kind": "molten_mapped", "runtime_id": cc_id})
        else:
            mapped["kind"] = "missing_molten"
        return mapped
    if cc_id and disposition == "mapped":
        mapped.update(
            {
                "kind": "chemical_fluid",
                "runtime_id": cc_id,
            }
        )
        return mapped
    if fluid_id in {"water", "lava"} or fluid_id.startswith("minecraft:"):
        runtime = {
            "water": "minecraft:water",
            "lava": "minecraft:lava",
        }.get(fluid_id, fluid_id)
        mapped.update({"kind": "vanilla_fluid", "runtime_id": runtime})
        return mapped
    mapped["kind"] = "unmapped_fluid"
    return mapped


def components_canonical(value: Any) -> str:
    if not value:
        return ""
    if isinstance(value, str):
        return value
    return json.dumps(value, sort_keys=True, separators=(",", ":"))


def stack_identity(operand: dict[str, Any]) -> str:
    item_id = (
        operand.get("runtime_id")
        or operand.get("item")
        or operand.get("id")
        or operand.get("items")
        or ""
    )
    components = operand.get("components") or operand.get("components_patch") or ""
    return f"{item_id}@{components_canonical(components)}"


def normalize_action(value: Any) -> str:
    damage = 0
    if isinstance(value, dict):
        damage = int(value.get("damage") or 0)
        value = value.get("kind")
    text = str(value or "consume").lower()
    if text.startswith("wear"):
        if ":" in text:
            return f"WEAR:{text.split(':', 1)[1]}"
        if damage > 0:
            return f"WEAR:{damage}"
        return "WEAR"
    if text in {"preserve", "not_consumed", "catalyst"}:
        return "PRESERVE"
    if damage > 0:
        return f"WEAR:{damage}"
    return "CONSUME"


def logical_input_identity(relation: dict[str, Any]) -> str:
    items: list[str] = []
    counts = list(relation.get("item_input_counts") or [])
    actions = list(relation.get("item_input_actions") or [])
    for index, operand in enumerate(relation.get("item_inputs") or []):
        if not isinstance(operand, dict):
            continue
        count = counts[index] if index < len(counts) else int(operand.get("count") or 0)
        action = normalize_action(actions[index] if index < len(actions) else "consume")
        items.append(f"{count}:{action}@{stack_identity(operand)}")
    items.sort()
    fluids = []
    for operand in relation.get("fluid_inputs") or []:
        if not isinstance(operand, dict):
            continue
        amount = operand.get("amount") or operand.get("count") or 0
        fluid_id = operand.get("runtime_id") or operand.get("fluid") or operand.get("id") or ""
        fluids.append(
            f"{amount}@{fluid_id}@{components_canonical(operand.get('components'))}"
        )
    fluids.sort()
    return ",".join(items) + "||" + ",".join(fluids)


def output_signature(relation: dict[str, Any]) -> str:
    items = []
    for operand in relation.get("item_outputs") or []:
        if not isinstance(operand, dict):
            continue
        count = operand.get("count") or 1
        item_id = operand.get("runtime_id") or operand.get("id") or operand.get("item") or ""
        items.append(
            f"{count}@{item_id}@{components_canonical(operand.get('components'))}"
        )
    items.sort()
    fluids = []
    for operand in relation.get("fluid_outputs") or []:
        if not isinstance(operand, dict):
            continue
        amount = operand.get("amount") or operand.get("count") or 0
        fluid_id = operand.get("runtime_id") or operand.get("fluid") or operand.get("id") or ""
        fluids.append(
            f"{amount}@{fluid_id}@{components_canonical(operand.get('components'))}"
        )
    fluids.sort()
    return ",".join(items) + "||" + ",".join(fluids)


def recipe_output_identity(relation: dict[str, Any]) -> str:
    return (
        f"{output_signature(relation)}|{relation.get('duration')}|{relation.get('eut')}"
    )


def logical_relation_identity(relation: dict[str, Any]) -> str:
    chances = relation.get("output_chances") or relation.get("chances") or []
    return "|".join(
        [
            logical_input_identity(relation),
            recipe_output_identity(relation),
            json.dumps(chances, separators=(",", ":")),
            str(relation.get("special_value") if "special_value" in relation else relation.get("specialValue") or 0),
            str(bool(relation.get("can_be_buffered", relation.get("canBeBuffered", True)))).lower(),
        ]
    )


COMPONENT_PREDICATES = (
    "cruciblecraft:circuit_config",
    "cruciblecraft:fireproof",
    "cruciblecraft:fluid_cell_content",
    "cruciblecraft:gas_cell_content",
    "cruciblecraft:portable_fluid",
)


def published_relation_identities_by_host() -> dict[str, set[str]]:
    cached = _CACHE.get("published_identities_by_host")
    if cached is not None:
        return cached
    by_host: dict[str, set[str]] = defaultdict(set)
    roots = (
        t37.GENERATED_ROOT,
        t38.GENERATED_ROOT,
        t39.GENERATED_ROOT,
        t40.GENERATED_ROOT,
        t41.GENERATED_ROOT,
        ROOT / "src/chemical_recipe_generated/resources/data/cruciblecraft/recipe",
        ROOT / "src/chemical_recipe_generated/resources/data/cruciblecraft/recipe",
    )
    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("*.json"):
            try:
                document = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            host = str(
                document.get("map")
                or document.get("target_map")
                or document.get("targetMap")
                or ""
            )
            if not host:
                continue
            recipe_type = str(document.get("type") or "")
            if recipe_type == "cruciblecraft:compact_gt_recipe_family":
                for relation in document.get("relations") or []:
                    if isinstance(relation, dict):
                        by_host[host].add(logical_relation_identity(relation))
            elif recipe_type in {
                "cruciblecraft:gt_recipe",
                "cruciblecraft:material_rule",
            }:
                by_host[host].add(logical_relation_identity(document))
    _CACHE["published_identities_by_host"] = dict(by_host)
    return _CACHE["published_identities_by_host"]


def published_relation_identities() -> set[str]:
    cached = _CACHE.get("published_identities")
    if cached is not None:
        return cached
    identities: set[str] = set()
    for values in published_relation_identities_by_host().values():
        identities.update(values)
    _CACHE["published_identities"] = identities
    return identities


def compact_io_identities(relation: dict[str, Any]) -> tuple[set[str], set[str]]:
    from tools.t21_operand_reachability import gt_operand_sets

    return gt_operand_sets(relation)


def support_routes(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []
    return list((load_json(path).get("routes") or []))


def production_compact_relations(root: Path) -> list[dict[str, Any]]:
    relations: list[dict[str, Any]] = []
    if not root.exists():
        return relations
    for path in sorted(root.rglob("*.json")):
        document = json.loads(path.read_text(encoding="utf-8"))
        if document.get("type") != "cruciblecraft:compact_gt_recipe_family":
            continue
        for relation in document.get("relations") or []:
            if isinstance(relation, dict):
                relations.append(relation)
    return relations


def typed_identity(kind: str, runtime_id: str | None) -> str | None:
    if not runtime_id:
        return None
    if runtime_id.startswith("item:") or runtime_id.startswith("fluid:"):
        return runtime_id
    if kind in {
        "chemical_fluid",
        "molten_mapped",
        "vanilla_fluid",
        "missing_molten",
        "unmapped_fluid",
        "unknown_fluid",
    }:
        return f"fluid:{runtime_id}"
    return f"item:{runtime_id}"


def apply_support_and_production(
    frontier: set[str],
    *,
    support_path: Path,
    generated_root: Path,
    layer_name: str,
) -> dict[str, Any]:
    added: list[str] = []
    blocked: list[str] = []
    for route in support_routes(support_path):
        inputs = {str(value) for value in route.get("input_identities") or []}
        outputs = {str(value) for value in route.get("output_identities") or []}
        if outputs and not route.get("output_identities"):
            outputs = {str(route.get("output_identity") or "")} - {""}
        missing = sorted(inputs - frontier)
        if missing:
            blocked.append(f"{layer_name} support {route.get('route_key') or route.get('source_map')}")
            continue
        new = sorted(outputs - frontier)
        frontier.update(outputs)
        added.extend(new)
    for relation in production_compact_relations(generated_root):
        inputs, outputs = compact_io_identities(relation)
        missing = sorted(inputs - frontier)
        if missing:
            blocked.append(
                f"{layer_name} production {relation.get('stable_id')}"
            )
            continue
        new = sorted(outputs - frontier)
        frontier.update(outputs)
        added.extend(new)
    return {
        "added": len(added),
        "blocked": blocked,
        "identity_count": len(frontier),
        "layer": layer_name,
    }


def t14_opening_load() -> dict[str, Any]:
    census = load_json(t41.CENSUS_DELTA)
    closing = (census.get("t14_load") or {}).get("closing") or {}
    if not closing:
        raise ValueError("T41 census t14_load.closing is required for T42 zero-delta")
    return dict(closing)


HOST_SORT_ORDER = (
    "cruciblecraft:drying",
    "cruciblecraft:autoclave",
    "cruciblecraft:compressor",
    "cruciblecraft:centrifuge",
    "cruciblecraft:electrolyzer",
    "cruciblecraft:assembler",
    "cruciblecraft:mixer",
    "cruciblecraft:bath",
    "cruciblecraft:smelter",
    "cruciblecraft:roaster",
)
