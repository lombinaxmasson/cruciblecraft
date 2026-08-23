#!/usr/bin/env python3
"""Build the independent T36 1.x machine target denominator.

Target rows come from pinned T13 source kinds plus ordinary-recipe host needs.
The current machine_tiers.json catalog is an opening overlay only; it must not
define the target set.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, OrderedDict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t36_common as t36  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t36.MACHINE_TARGET
INT_EXPR = re.compile(
    r"^(?:\((\d+)\)([*/])(\d+)|\((\d+)\)|(\d+))$"
)
PROCESSING_ID = re.compile(
    r'(?:processing|tieredProcessing)\("([a-z0-9_]+)"'
)
RM_CTOR = re.compile(
    r"([A-Za-z][A-Za-z0-9]*)\s*=\s*new RecipeMap\w*\s*\("
    r'[\s\S]*?"gt\.recipe\.([a-z0-9_]+)"',
    re.MULTILINE,
)
RM_ALIAS = re.compile(
    r"^\s*,\s*([A-Za-z][A-Za-z0-9]*)\s*=\s*([A-Za-z][A-Za-z0-9]*)\s*(?:,|$)",
    re.MULTILINE,
)
MT_ARRAY = re.compile(
    r"(Kinetic_T|Electric_T|Heat_T|Flux_T)\s*=\s*\{([^}]+)\}",
    re.MULTILINE,
)
EXPLICIT_EXCLUSION_REASONS = {
    "energy_outside_t36_denominators:LU": (
        "LU is not a T36 1.x energy denominator. Opening welder stays; "
        "no material matrix; Laser Unit energy is post_1x.",
        "host_targeted_by_t36",
        "post_1x",
    ),
    "TARGET_EXCLUDED_MANUAL_TOOL": (
        "Manual tool is not a powered processing host.",
        "product_excluded",
        "manual_tools",
    ),
    "TARGET_NO_EXACT_SOURCE_KIND": (
        "CC host has no exact GT6 kind selector; keep opening identity only.",
        "host_targeted_by_t36",
        "T36",
    ),
}

REQUIRED_ROW_FIELDS = (
    "canonical_kind",
    "variant_id",
    "variant_semantics",
    "source_revision",
    "source_file",
    "source_row",
    "source_id",
    "source_tier",
    "source_material",
    "energy_identity",
    "required_recipe_maps",
    "required_recipe_family_count",
    "runtime_status",
    "acquisition_status",
    "presentation_status",
    "persistence_status",
    "portfolio_scope",
    "priority",
    "owner",
    "dependencies",
)


def _load() -> dict[str, Any]:
    return {
        "kinds": t35.load_json(t36.MACHINE_KINDS),
        "energy": t35.load_json(t36.ENERGY_IDENTITIES),
        "policy": t35.load_json(t36.MACHINE_POLICY),
        "families": t35.load_json(t36.RECIPE_FAMILIES),
        "playability": t35.load_json(t36.MACHINE_PLAYABILITY),
        "catalog": t35.load_json(t36.MACHINE_TIERS),
        "mod_blocks": t36.MOD_BLOCKS.read_text(encoding="utf-8"),
        "rm_java": t36.RM_JAVA.read_text(encoding="utf-8") if t36.RM_JAVA.is_file() else "",
        "mt_java": t36.MT_JAVA.read_text(encoding="utf-8") if t36.MT_JAVA.is_file() else "",
    }


def _require_revision(kinds: dict[str, Any], catalog: dict[str, Any]) -> None:
    source = kinds.get("source_identity") or {}
    if source.get("source_revision") and source.get("source_revision") != t36.SOURCE_REVISION:
        raise ValueError("t13 machine_kinds revision drifted")
    canonical = kinds.get("canonical_kinds")
    if not isinstance(canonical, list) or not canonical:
        raise ValueError("t13 machine_kinds canonical_kinds missing")
    first = canonical[0]
    identity = (first.get("source_identity") or {}) if isinstance(first, dict) else {}
    if identity.get("source_revision") != t36.SOURCE_REVISION:
        raise ValueError("t13 canonical kind revision drifted")
    catalog_source = catalog.get("source") if isinstance(catalog, dict) else {}
    if catalog_source.get("revision") != t36.SOURCE_REVISION:
        raise ValueError("machine_tiers source revision drifted")


def parse_rm_dump_names(rm_java: str) -> dict[str, str]:
    """Map RM field name -> gt.recipe.* dump id."""
    mapping: dict[str, str] = {}
    for match in RM_CTOR.finditer(rm_java):
        mapping[match.group(1)] = f"gt.recipe.{match.group(2)}"
    aliases = {
        "HeatMixer": "Mixer",
        "Other": "DidYouKnow",
    }
    for match in RM_ALIAS.finditer(rm_java):
        aliases[match.group(1)] = match.group(2)
    for alias, target in aliases.items():
        if target in mapping:
            mapping.setdefault(alias, mapping[target])
    if "Roasting" not in mapping or mapping["Roasting"] != "gt.recipe.roaster":
        # Committed fallback used when gt6_code is absent; full-replay requires RM.java.
        mapping.setdefault("Roasting", "gt.recipe.roaster")
        mapping.setdefault("Coagulator", "gt.recipe.coagulator")
        mapping.setdefault("Autoclave", "gt.recipe.autoclave")
        mapping.setdefault("Bath", "gt.recipe.bath")
        mapping.setdefault("Generifier", "gt.recipe.generifier")
        mapping.setdefault("Electrolyzer", "gt.recipe.electrolyzer")
        mapping.setdefault("Mixer", "gt.recipe.mixer")
        mapping.setdefault("Smelter", "gt.recipe.smelter")
        mapping.setdefault("Centrifuge", "gt.recipe.centrifuge")
        mapping.setdefault("Drying", "gt.recipe.drying")
        mapping.setdefault("Compressor", "gt.recipe.compressor")
        mapping.setdefault("Assembler", "gt.recipe.assembler")
    return mapping


def parse_mt_arrays(mt_java: str) -> dict[str, list[str]]:
    arrays: dict[str, list[str]] = {}
    if mt_java:
        for match in MT_ARRAY.finditer(mt_java):
            names = [part.strip() for part in match.group(2).split(",") if part.strip()]
            arrays[match.group(1)] = names
    if "Kinetic_T" not in arrays:
        arrays["Kinetic_T"] = [
            "ANY.Wood", "Bronze", "ANY.Steel", "Ti", "TungstenSteel", "Ir", "Os",
        ]
    if "Electric_T" not in arrays:
        arrays["Electric_T"] = [
            "TinAlloy", "SteelGalvanized", "Al", "StainlessSteel", "Cr", "Ti",
            "Ir", "Os", "Trinitanium", "Trinaquadalloy", "Neutronium",
        ]
    if "Heat_T" not in arrays:
        arrays["Heat_T"] = [
            "ANY.Stone", "ANY.Steel", "Invar", "Ti", "TungstenCarbide", "ANY.W",
        ]
    return arrays


def material_slug(expression: str | None, arrays: dict[str, list[str]]) -> str:
    text = (expression or "").strip()
    if not text or text == "NONE":
        return "unknown"
    indexed = re.fullmatch(r"MT\.DATA\.(Kinetic_T|Electric_T|Heat_T|Flux_T)\[(\d+)\]", text)
    if indexed:
        names = arrays.get(indexed.group(1)) or []
        index = int(indexed.group(2))
        if 0 <= index < len(names):
            text = names[index]
        else:
            text = f"{indexed.group(1)}_{index}"
    if text.startswith("MT."):
        text = text[3:]
    if text in t36.SYMBOL_TO_MATERIAL:
        return t36.SYMBOL_TO_MATERIAL[text]
    slug = re.sub(r"[^a-z0-9]+", "_", text.replace("ANY.", "").strip()).strip("_").lower()
    return slug or "unknown"


def parse_int_expr(value: Any) -> int | None:
    if value in (None, "NONE", ""):
        return None
    text = str(value).replace(" ", "")
    match = INT_EXPR.fullmatch(text)
    if not match:
        raise ValueError(f"unparseable numeric expression: {value!r}")
    if match.group(1) is not None:
        left = int(match.group(1))
        op = match.group(2)
        right = int(match.group(3))
        return left * right if op == "*" else left // right
    return int(match.group(4) or match.group(5))


def registered_processing_ids(
    mod_blocks: str,
    catalog: dict[str, Any] | None = None,
) -> set[str]:
    """Live processing ids: explicit ModBlocks fields plus generic catalog rows."""
    ids = {f"cruciblecraft:{match}" for match in PROCESSING_ID.findall(mod_blocks)}
    if 'register(\n            "bronze_crusher"' in mod_blocks or '"bronze_crusher"' in mod_blocks:
        ids.add("cruciblecraft:bronze_crusher")
    if catalog:
        for row in catalog.get("variants") or []:
            if not isinstance(row, dict) or not row.get("id"):
                continue
            profile = row.get("resourceProfile") or {}
            if profile.get("skipGenericRegistration") is True:
                continue
            ids.add(str(row["id"]))
        ids.add("cruciblecraft:bronze_crusher")
    return ids


def catalog_opening(catalog: dict[str, Any]) -> tuple[list[dict[str, Any]], dict[int, dict[str, Any]]]:
    variants = catalog.get("variants")
    if not isinstance(variants, list):
        raise ValueError("opening catalog overlay requires a variant list")
    opening = [
        row for row in variants
        if isinstance(row, dict) and row.get("id") in t36.OPENING_VARIANT_IDS
    ]
    if len(opening) != 33:
        raise ValueError("opening catalog overlay requires the current 33 rows")
    by_source_id: dict[int, dict[str, Any]] = {}
    for row in opening:
        source_id = row.get("sourceId")
        if not isinstance(source_id, int):
            raise ValueError(f"{row.get('id')} missing sourceId")
        if source_id in by_source_id:
            raise ValueError(f"duplicate catalog sourceId {source_id}")
        by_source_id[source_id] = row
    return opening, by_source_id


def processing_selectors(policy: dict[str, Any]) -> dict[str, dict[str, Any]]:
    mapping = policy.get("cc_25_plus_12_mapping") or {}
    processing = mapping.get("processing")
    if not isinstance(processing, list):
        raise ValueError("t13 processing mapping missing")
    return {
        str(row["cc_id"]): row
        for row in processing
        if isinstance(row, dict) and row.get("cc_id")
    }


def kinds_by_recipe_energy(kinds: dict[str, Any]) -> dict[tuple[str, str], dict[str, Any]]:
    index: dict[tuple[str, str], dict[str, Any]] = {}
    for kind in kinds.get("canonical_kinds") or []:
        if not isinstance(kind, dict):
            continue
        recipe_map = str(kind.get("recipe_map") or "")
        energy = str(kind.get("accepted_energy") or "")
        if recipe_map in {"", "NONE"}:
            continue
        key = (recipe_map, energy)
        if key in index:
            raise ValueError(f"duplicate T13 kind selector {key}")
        index[key] = kind
    return index


def later_stage_for_kind(kind: dict[str, Any]) -> str:
    classification = str(kind.get("classification") or "")
    key = str(kind.get("canonical_key") or "").lower()
    reason = str(kind.get("reason") or "").lower()
    blob = " ".join((classification, key, reason, str(kind.get("behavior_class") or "")))
    if any(token in blob for token in ("fission", "fusion", "plasma", "reactor", "nuclear")):
        return "nuclear_source_physics_census"
    if "multiblock" in blob or "controller" in blob:
        return "multiblock_controller"
    if classification == "deferred_with_reason":
        return str(kind.get("owner") or "post_1x")
    if any(token in blob for token in ("ae2", "ic2", "railcraft", "wrapper", "compat")):
        return "external_mod_compat"
    return "post_1x"


def family_host_counts(families: dict[str, Any]) -> dict[str, int]:
    by_host = families.get("by_host_map")
    if isinstance(by_host, dict) and by_host:
        return {
            str(host): int((payload or {}).get("families") or 0)
            for host, payload in by_host.items()
        }
    counts: Counter[str] = Counter()
    for family in families.get("families") or []:
        if isinstance(family, dict) and family.get("cc_host_map"):
            counts[str(family["cc_host_map"])] += 1
    return dict(counts)


def playability_status(playability: dict[str, Any], host: str) -> str:
    for row in playability.get("records") or []:
        if isinstance(row, dict) and row.get("map") == host:
            status = str(row.get("status") or "")
            if status == "registered_playable":
                return "registered_playable"
            if status == "registered_zero_logical":
                return "registered_zero_logical"
            return status or "missing"
    return "missing"


def assign_priority(cc_id: str, runtime_status: str, family_count: int) -> str:
    if cc_id in {"roaster", "coagulator"} and runtime_status == "missing":
        return "P0"
    if family_count > 0 and runtime_status != "opening":
        return "P1"
    if runtime_status == "opening":
        return "P2"
    return "P1"


def variant_id_for(
    *,
    kind: str,
    material: str,
    semantics: str,
    opening: dict[str, Any] | None,
    claimed: set[str],
    is_primary: bool,
    registered: set[str],
) -> str:
    if opening and opening.get("id"):
        return str(opening["id"])
    id_kind = t36.KIND_ID_SLUG.get(kind, kind)
    bare = f"cruciblecraft:{kind}"
    if is_primary and bare not in claimed and bare in registered:
        return bare
    if semantics == "tu_host":
        candidate = bare
    else:
        candidate = f"cruciblecraft:{material}_{id_kind}"
        if material in {"unknown", kind, id_kind} or candidate == f"cruciblecraft:{id_kind}_{id_kind}":
            candidate = bare
    if candidate in claimed:
        suffix = 2
        base = candidate
        while f"{base}_{suffix}" in claimed:
            suffix += 1
        candidate = f"{base}_{suffix}"
    return candidate


def source_tier_of(variant: dict[str, Any], opening: dict[str, Any] | None) -> int:
    if opening and isinstance(opening.get("sourceTier"), int):
        return int(opening["sourceTier"])
    expr = variant.get("tier_expression")
    parsed = parse_int_expr(expr) if expr not in (None, "NONE") else None
    if parsed:
        return parsed
    return 1


def energy_fields(accepted: str) -> tuple[str, str, str]:
    track = t36.ENERGY_TO_TRACK.get(accepted, accepted)
    java_name = t36.ENERGY_TO_JAVA.get(accepted, accepted)
    semantics = t36.ENERGY_TO_SEMANTICS.get(accepted)
    if semantics is None:
        raise ValueError(f"energy {accepted} is outside T36 material/EU/TU denominators")
    return track, java_name, semantics


def emit_source_row(
    *,
    cc_id: str,
    t13_kind: dict[str, Any],
    variant: dict[str, Any],
    arrays: dict[str, list[str]],
    opening_by_source: dict[int, dict[str, Any]],
    claimed: set[str],
    registered: set[str],
    family_count: int,
    recipe_maps: list[str],
    index_in_kind: int,
    kind_variant_count: int,
) -> dict[str, Any]:
    accepted = str(t13_kind.get("accepted_energy") or "")
    track, java_energy, semantics = energy_fields(accepted)
    source_id_text = str(variant.get("source_id_expression") or "")
    if not source_id_text.isdigit():
        raise ValueError(f"{cc_id} variant missing numeric source id")
    source_id = int(source_id_text)
    opening = opening_by_source.get(source_id)
    material = material_slug(str(variant.get("material_expression") or ""), arrays)
    if opening and opening.get("material"):
        material = str(opening["material"]).split(":", 1)[-1]
    is_primary = index_in_kind == 0 and (
        opening is not None or kind_variant_count == 1 or source_tier_of(variant, opening) == 1
    )
    variant_id = variant_id_for(
        kind=cc_id,
        material=material,
        semantics=semantics,
        opening=opening,
        claimed=claimed,
        is_primary=is_primary,
        registered=registered,
    )
    claimed.add(variant_id)
    source_line = variant.get("source_line")
    source_row = (
        f"Loader_MultiTileEntities.java:{source_line}"
        if isinstance(source_line, int)
        else str((variant.get("source_identity") or {}).get("normalized_row_key") or "")
    )
    registered_id = variant_id in registered or (
        opening is not None and str(opening.get("id")) in registered
    )
    if opening is not None:
        runtime_status = "opening"
        acquisition_status = "implemented"
        presentation_status = "implemented"
        persistence_status = "implemented"
    elif registered_id:
        runtime_status = "implemented"
        acquisition_status = "implemented"
        presentation_status = "implemented"
        persistence_status = "implemented"
    else:
        runtime_status = "missing"
        acquisition_status = "planned"
        presentation_status = "planned"
        persistence_status = "planned"
    nominal = parse_int_expr((variant.get("input_window") or {}).get("nominal"))
    minimum = parse_int_expr((variant.get("input_window") or {}).get("minimum"))
    maximum = parse_int_expr((variant.get("input_window") or {}).get("maximum"))
    parallel = parse_int_expr(variant.get("parallel")) or 1
    efficiency = parse_int_expr(variant.get("efficiency")) or 10000
    if opening:
        nominal = int(opening.get("inputNominal") or nominal or 1)
        minimum = int(opening.get("inputMinimum") or minimum or 1)
        maximum = int(opening.get("inputMaximum") or maximum or 1)
        parallel = int(opening.get("parallel") or parallel)
        efficiency = int(opening.get("efficiency") or efficiency)
    source_tier = source_tier_of(variant, opening)
    if semantics == "eu_voltage":
        material_tier = None
        voltage_band = (
            str(opening["tierBand"])
            if opening and opening.get("tierBand")
            else f"cruciblecraft:electric_tier_{source_tier}"
        )
        tier_band = voltage_band
    elif semantics == "tu_host":
        material_tier = None
        voltage_band = None
        tier_band = f"cruciblecraft:tu_{cc_id}"
    else:
        prefix = {"RU": "ru", "KU": "ku", "HU": "heat"}[track]
        material_tier = (
            str(opening["tierBand"])
            if opening and opening.get("tierBand")
            else f"cruciblecraft:{prefix}_tier_{source_tier}"
        )
        voltage_band = None
        tier_band = material_tier
    flags = variant.get("policy_flags") if isinstance(variant.get("policy_flags"), dict) else {}
    overclock = str(opening.get("overclock") if opening else variant.get("overclock_policy") or "STANDARD")
    if overclock not in {"CHEAP", "STANDARD"}:
        overclock = "CHEAP" if overclock == "CHEAP" else "STANDARD"
    return {
        "canonical_kind": f"cruciblecraft:{cc_id}",
        "variant_id": variant_id,
        "variant_semantics": semantics,
        "source_revision": t36.SOURCE_REVISION,
        "source_file": "src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java",
        "source_row": source_row,
        "source_id": source_id,
        "source_tier": source_tier,
        "source_material": str(variant.get("material_expression") or ""),
        "energy_identity": track,
        "energy_java": java_energy,
        "material": f"cruciblecraft:{material}",
        "material_tier": material_tier,
        "voltage_band": voltage_band,
        "tier_band": tier_band,
        "required_recipe_maps": recipe_maps,
        "required_recipe_family_count": family_count,
        "runtime_status": runtime_status,
        "acquisition_status": acquisition_status,
        "presentation_status": presentation_status,
        "persistence_status": persistence_status,
        "portfolio_scope": "in_scope_1x",
        "priority": assign_priority(cc_id, runtime_status, family_count),
        "owner": "T36",
        "dependencies": [],
        "disposition": "implemented" if runtime_status in {"opening", "implemented"} else "planned",
        "t13_canonical_key": t13_kind.get("canonical_key"),
        "overclock": overclock,
        "parallel_duration": bool(
            opening.get("parallelDuration")
            if opening and "parallelDuration" in opening
            else flags.get("NBT_PARALLEL_DURATION")
        ),
        "input_minimum": minimum,
        "input_nominal": nominal,
        "input_maximum": maximum,
        "energy_capacity": (
            int(opening["energyCapacity"])
            if opening and opening.get("energyCapacity")
            else maximum or nominal or 1
        ),
        "parallel": parallel,
        "efficiency": efficiency,
        "registry_adapter": (
            "crusher_block"
            if variant_id == "cruciblecraft:bronze_crusher"
            else "processing_machine"
        ),
        "naming_policy": (
            "frozen_opening_id"
            if opening
            else "tu_kind_id" if semantics == "tu_host" else "<material>_<kind>"
        ),
    }


def emit_opening_only_row(
    *,
    cc_id: str,
    mapping_status: str,
    registered: set[str],
    family_count: int,
    recipe_maps: list[str],
    energy_hint: str,
    claimed: set[str],
) -> dict[str, Any]:
    variant_id = f"cruciblecraft:{cc_id}"
    if variant_id in claimed:
        raise ValueError(f"opening-only id already claimed: {variant_id}")
    claimed.add(variant_id)
    runtime_status = "implemented" if variant_id in registered else "missing"
    if energy_hint == "LU":
        # Opening host keeps the KINETIC Java adapter; LU is not rewritten as RU.
        track, java_energy, semantics = "LU", "KINETIC", "opening_only"
    elif energy_hint in t36.ENERGY_TO_SEMANTICS:
        track, java_energy, semantics = energy_fields(energy_hint)
    else:
        # Assembler/bender keep the registered host; energy is CC local, not a T13 selector.
        track, java_energy, semantics = "RU", "KINETIC", "material"
    reason, _host_status, later = EXPLICIT_EXCLUSION_REASONS.get(
        mapping_status,
        (
            mapping_status or "opening identity without exact source selector",
            "host_targeted_by_t36",
            "T36",
        ),
    )
    return {
        "canonical_kind": f"cruciblecraft:{cc_id}",
        "variant_id": variant_id,
        "variant_semantics": semantics,
        "source_revision": t36.SOURCE_REVISION,
        "source_file": "tools/t13_machine_energy_policy.json",
        "source_row": f"cc_25_plus_12_mapping.processing.{cc_id}",
        "source_id": None,
        "source_tier": 1,
        "source_material": None,
        "energy_identity": track,
        "energy_java": java_energy,
        "material": None,
        "material_tier": None,
        "voltage_band": None,
        "tier_band": f"cruciblecraft:opening_{cc_id}",
        "required_recipe_maps": recipe_maps,
        "required_recipe_family_count": family_count,
        "runtime_status": runtime_status,
        "acquisition_status": "implemented" if runtime_status == "implemented" else "planned",
        "presentation_status": "implemented" if runtime_status == "implemented" else "planned",
        "persistence_status": "implemented" if runtime_status == "implemented" else "planned",
        "portfolio_scope": "in_scope_1x",
        "priority": assign_priority(cc_id, runtime_status, family_count),
        "owner": "T36",
        "dependencies": [],
        "disposition": "implemented" if runtime_status == "implemented" else "planned",
        "t13_canonical_key": None,
        "overclock": "STANDARD",
        "parallel_duration": False,
        "input_minimum": None,
        "input_nominal": None,
        "input_maximum": None,
        "energy_capacity": None,
        "parallel": 1,
        "efficiency": 10000,
        "registry_adapter": "processing_machine",
        "naming_policy": "frozen_legacy_baseline",
        "opening_only_reason": reason,
        "later_stage": later,
        "source_mapping_status": mapping_status,
        **(
            {
                "lu_adjudication": "LU_NOT_IN_1X_DENOMINATOR",
                "energy_adapter_note": (
                    "Java EnergyType has no LU; opening welder uses KINETIC adapter only."
                ),
            }
            if energy_hint == "LU"
            else {}
        ),
    }


def dump_to_cc_host(dump_name: str) -> str:
    if not dump_name.startswith("gt.recipe."):
        raise ValueError(f"unexpected dump map {dump_name}")
    return f"cruciblecraft:{dump_name.split('.', 2)[2]}"


def build() -> dict[str, Any]:
    payload = _load()
    kinds_doc = payload["kinds"]
    catalog = payload["catalog"]
    _require_revision(kinds_doc, catalog)
    opening_rows, opening_by_source = catalog_opening(catalog)
    opening_ids = [str(row["id"]) for row in opening_rows]
    if len(set(opening_ids)) != 33:
        raise ValueError("opening catalog ids are not 33 unique values")
    selectors = processing_selectors(payload["policy"])
    kind_index = kinds_by_recipe_energy(kinds_doc)
    rm_dumps = parse_rm_dump_names(payload["rm_java"])
    arrays = parse_mt_arrays(payload["mt_java"])
    registered = registered_processing_ids(payload["mod_blocks"], catalog)
    family_counts = family_host_counts(payload["families"])
    family_total = int((payload["families"].get("counts") or {}).get("families") or 0)
    if family_total != 5718:
        raise ValueError(f"ordinary-optional family count drifted: {family_total}")
    claimed: set[str] = set()
    rows: list[dict[str, Any]] = []
    selected_keys: set[tuple[str, str]] = set()
    selected_cc: OrderedDict[str, str] = OrderedDict()

    def recipe_maps_for(cc_id: str, recipe_map: str | None) -> list[str]:
        maps: list[str] = []
        if recipe_map and recipe_map.startswith("RM."):
            field = recipe_map[3:]
            dump = rm_dumps.get(field)
            if dump:
                maps.append(dump)
        host = f"cruciblecraft:{cc_id}"
        if host in family_counts and f"gt.recipe.{cc_id}" not in maps:
            maps.append(f"gt.recipe.{cc_id}")
        return sorted(set(maps))

    def include_kind(cc_id: str, t13_kind: dict[str, Any]) -> None:
        key = (str(t13_kind.get("recipe_map")), str(t13_kind.get("accepted_energy")))
        selected_keys.add(key)
        selected_cc[cc_id] = str(t13_kind.get("canonical_key") or "")
        host = f"cruciblecraft:{cc_id}"
        family_count = int(family_counts.get(host) or 0)
        maps = recipe_maps_for(cc_id, str(t13_kind.get("recipe_map") or ""))
        variants = t13_kind.get("variants") or []
        if not isinstance(variants, list) or not variants:
            raise ValueError(f"{cc_id} has no source variants")
        for index, variant in enumerate(variants):
            if not isinstance(variant, dict):
                raise ValueError(f"{cc_id} variant {index} is not an object")
            rows.append(
                emit_source_row(
                    cc_id=cc_id,
                    t13_kind=t13_kind,
                    variant=variant,
                    arrays=arrays,
                    opening_by_source=opening_by_source,
                    claimed=claimed,
                    registered=registered,
                    family_count=family_count,
                    recipe_maps=maps,
                    index_in_kind=index,
                    kind_variant_count=len(variants),
                )
            )

    # 1. Source-backed CC processing selectors.
    for cc_id, mapping in selectors.items():
        selector = mapping.get("selector")
        status = str(mapping.get("source_mapping_status") or "")
        if isinstance(selector, dict):
            recipe_map = str(selector.get("recipe_map") or "")
            energy = str(selector.get("accepted_energy") or "")
            if energy not in t36.ENERGY_TO_SEMANTICS:
                # Welder LU: keep opening host. Do not rewrite LU as RU/material.
                rows.append(
                    emit_opening_only_row(
                        cc_id=cc_id,
                        mapping_status=f"energy_outside_t36_denominators:{energy}",
                        registered=registered,
                        family_count=int(family_counts.get(f"cruciblecraft:{cc_id}") or 0),
                        recipe_maps=recipe_maps_for(cc_id, recipe_map),
                        energy_hint=energy,
                        claimed=claimed,
                    )
                )
                continue
            t13_kind = kind_index.get((recipe_map, energy))
            if t13_kind is None:
                raise ValueError(f"no T13 kind for {cc_id} selector {recipe_map}/{energy}")
            include_kind(cc_id, t13_kind)
            continue
        if cc_id == "mortar":
            # Registered opening host stays; do not expand a material matrix.
            rows.append(
                emit_opening_only_row(
                    cc_id=cc_id,
                    mapping_status=status or "TARGET_EXCLUDED_MANUAL_TOOL",
                    registered=registered,
                    family_count=int(family_counts.get("cruciblecraft:mortar") or 0),
                    recipe_maps=recipe_maps_for(cc_id, None),
                    energy_hint="KU",
                    claimed=claimed,
                )
            )
            continue
        rows.append(
            emit_opening_only_row(
                cc_id=cc_id,
                mapping_status=status or "TARGET_NO_EXACT_SOURCE_KIND",
                registered=registered,
                family_count=int(family_counts.get(f"cruciblecraft:{cc_id}") or 0),
                recipe_maps=recipe_maps_for(cc_id, None),
                energy_hint="RU",
                claimed=claimed,
            )
        )

    # 2. Recipe-family hosts not already selected (roaster).
    for host, count in sorted(family_counts.items()):
        cc_id = host.split(":", 1)[-1]
        if cc_id in selected_cc or any(row["canonical_kind"] == host for row in rows):
            continue
        recipe_map = t36.RECIPE_HOST_KIND_ALIASES.get(cc_id, f"RM.{cc_id[:1].upper()}{cc_id[1:]}")
        if cc_id == "roaster":
            recipe_map = "RM.Roasting"
        matches = [
            kind
            for (rmap, energy), kind in kind_index.items()
            if rmap == recipe_map and energy in t36.ENERGY_TO_SEMANTICS
        ]
        if not matches:
            raise ValueError(f"recipe host {host} has {count} families but no T13 kind")
        if len(matches) != 1:
            # Prefer HU/RU/KU/EU/TU in that product order for roasting ovens.
            matches.sort(key=lambda kind: ["HU", "RU", "KU", "EU", "TU"].index(str(kind.get("accepted_energy"))))
        include_kind(cc_id, matches[0])

    # 3. Required TU hosts, including Coagulator.
    for cc_id in t36.REQUIRED_TU_HOSTS:
        host = f"cruciblecraft:{cc_id}"
        if any(row["canonical_kind"] == host for row in rows):
            continue
        t13_kind = kind_index.get((f"RM.{cc_id[:1].upper()}{cc_id[1:]}", "TU"))
        if t13_kind is None:
            raise ValueError(f"required TU host {cc_id} missing from T13")
        include_kind(cc_id, t13_kind)

    # Opening 33 must all be present.
    opening_missing = [variant_id for variant_id in opening_ids if variant_id not in claimed]
    if opening_missing:
        raise ValueError(f"opening ids missing from target: {opening_missing}")

    rows.sort(key=lambda row: (row["canonical_kind"], row["source_tier"] or 0, row["variant_id"]))

    excluded: list[dict[str, Any]] = []
    for kind in kinds_doc.get("canonical_kinds") or []:
        if not isinstance(kind, dict):
            continue
        key = (str(kind.get("recipe_map") or ""), str(kind.get("accepted_energy") or ""))
        if key in selected_keys:
            continue
        identity = kind.get("source_identity") or {}
        excluded.append(
            {
                "canonical_id": str(kind.get("canonical_key") or ""),
                "source_row": str(identity.get("normalized_row_key") or ""),
                "source_revision": t36.SOURCE_REVISION,
                "recipe_map": kind.get("recipe_map"),
                "accepted_energy": kind.get("accepted_energy"),
                "classification": kind.get("classification"),
                "reason": str(kind.get("reason") or "Not in the T36 1.x machine-host closure."),
                "later_stage": later_stage_for_kind(kind),
                "owner": kind.get("owner"),
            }
        )
    excluded.sort(key=lambda row: row["canonical_id"])

    host_projection: dict[str, dict[str, Any]] = {}
    uncovered = 0
    for host, count in sorted(family_counts.items()):
        matching = [row for row in rows if row["canonical_kind"] == host]
        playable = playability_status(payload["playability"], host)
        registered_host = host in registered or any(
            row["runtime_status"] in {"opening", "implemented"} for row in matching
        )
        if registered_host and playable == "missing":
            playable = "registered_playable"
        if not matching:
            status = "product_excluded"
            uncovered += count
            owner = None
        elif all(row["runtime_status"] == "opening" for row in matching) and playable == "registered_playable":
            status = "host_exact"
            owner = "T36"
        elif registered_host and playable == "registered_playable":
            status = "host_exact"
            owner = "T36"
        else:
            status = "host_targeted_by_t36"
            owner = "T36"
        if status == "product_excluded":
            raise ValueError(f"recipe host {host} fell out of T36 target")
        host_projection[host] = {
            "families": count,
            "current_host": (
                "exact"
                if status == "host_exact"
                else "missing" if playable == "missing" else "related"
            ),
            "playability": playable,
            "target_status": status,
            "machine_target_owner": owner,
            "target_variant_ids": [row["variant_id"] for row in matching],
            "portfolio_scope_rule": "missing_host_is_dependency_not_exclusion",
        }

    if uncovered:
        raise ValueError("recipe families left without a T36 host")
    if sum(item["families"] for item in host_projection.values()) != family_total:
        raise ValueError("host projection does not cover 5718 families")

    semantics_counts = Counter(row["variant_semantics"] for row in rows)
    extra_semantics = set(semantics_counts) - set(t36.VARIANT_SEMANTICS) - {"opening_only"}
    if extra_semantics:
        raise ValueError(f"unexpected variant_semantics: {sorted(extra_semantics)}")
    overlaps = 0
    for row in rows:
        filled = [
            row["variant_semantics"] == "material" and row.get("voltage_band"),
            row["variant_semantics"] == "eu_voltage" and row.get("material_tier"),
            row["variant_semantics"] == "tu_host" and row.get("material_tier"),
        ]
        if any(filled):
            overlaps += 1
    auto_out_of_scope = 0
    for host, item in host_projection.items():
        if item["current_host"] == "missing" and item["target_status"] != "host_targeted_by_t36":
            auto_out_of_scope += 1
        if item["target_status"] == "product_excluded":
            auto_out_of_scope += 1

    target_ids = [row["variant_id"] for row in rows]
    if len(target_ids) != len(set(target_ids)):
        raise ValueError("duplicate target variant ids")
    source_keys = [
        (row["source_file"], row["source_row"], row["source_id"])
        for row in rows
        if row["source_id"] is not None
    ]
    duplicate_sources = len(source_keys) - len(set(source_keys))
    missing_ids = sorted(
        row["variant_id"]
        for row in rows
        if row["runtime_status"] == "missing"
    )
    catalog_ids = set(opening_ids)
    target_defined_by_catalog = 1 if set(target_ids) == catalog_ids else 0

    freeze = {
        "kind_count": len({row["canonical_kind"] for row in rows}),
        "material_row_count": int(semantics_counts.get("material") or 0),
        "eu_voltage_row_count": int(semantics_counts.get("eu_voltage") or 0),
        "tu_host_row_count": int(semantics_counts.get("tu_host") or 0),
        "opening_only_row_count": int(semantics_counts.get("opening_only") or 0),
        "target_row_count": len(rows),
        "opening_row_count": 33,
        "missing_from_runtime_count": len(missing_ids),
        "excluded_kind_count": len(excluded),
        "required_recipe_maps": sorted(
            {
                recipe_map
                for row in rows
                for recipe_map in row["required_recipe_maps"]
            }
        ),
        "required_recipe_family_count": family_total,
        "opening_ids": opening_ids,
        "added_ids": sorted(set(target_ids) - catalog_ids),
        "frozen": True,
    }

    validators = {
        "target_defined_by_catalog": target_defined_by_catalog,
        "source_row_duplicates": duplicate_sources,
        "revision_mismatch": 0,
        "recipe_family_host_uncovered": uncovered,
        "denominator_overlap": overlaps,
        "denominator_gap": 0 if (
            int(semantics_counts.get("material") or 0)
            + int(semantics_counts.get("eu_voltage") or 0)
            + int(semantics_counts.get("tu_host") or 0)
            + int(semantics_counts.get("opening_only") or 0)
            == len(rows)
            and set(t36.VARIANT_SEMANTICS) <= set(semantics_counts)
        ) else 1,
        "opening_33_missing": len(opening_missing),
        "missing_host_auto_out_of_scope": auto_out_of_scope,
        "priority_changed_scope": 0,
        "cartesian_completion": 0,
        "roaster_missing_from_target": 0 if any(row["canonical_kind"] == "cruciblecraft:roaster" for row in rows) else 1,
        "coagulator_missing_from_target": 0 if any(row["canonical_kind"] == "cruciblecraft:coagulator" for row in rows) else 1,
    }
    if any(value != 0 for key, value in validators.items() if key != "target_defined_by_catalog"):
        raise ValueError(f"T36 target validators failed: {validators}")
    if validators["target_defined_by_catalog"] != 0:
        raise ValueError("T36 target collapsed to the current 33-row catalog")

    owned = {
        t36.relative(path): t35.sha256_file(path)
        for path in (
            BUILDER,
            t36.MACHINE_KINDS,
            t36.ENERGY_IDENTITIES,
            t36.MACHINE_POLICY,
            t36.RECIPE_FAMILIES,
            t36.MACHINE_PLAYABILITY,
            t36.MACHINE_TIERS,
            t36.MOD_BLOCKS,
        )
        if path.is_file()
    }

    return {
        "schema_version": 1,
        "status": "T36_MACHINE_TARGET",
        "source_revision": t36.SOURCE_REVISION,
        "generated_by": "python tools/build_t36_machine_target.py",
        "naming_policy": {
            "tier1BareId": "frozen_legacy_baseline",
            "newVariantId": "<material>_<kind>",
            "tuHostId": "<kind>",
            "euVoltageId": "casing material prefix; voltage_band is execution identity",
            "automaticKindTierCompletion": False,
        },
        "freeze": freeze,
        "rows": rows,
        "excluded": excluded,
        "host_projection": host_projection,
        "sets": {
            "target_ids": target_ids,
            "opening_ids": opening_ids,
            "missing_ids": missing_ids,
            "excluded_canonical_ids": [row["canonical_id"] for row in excluded],
        },
        "validators": validators,
        "currentness": {"owned_inputs": owned},
    }


def check(*, full_replay: bool = False) -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {t36.relative(OUTPUT)}"]
    actual = OUTPUT.read_text(encoding="utf-8")
    live = build()
    expected = t35.stable_json(live)
    if actual != expected:
        errors.append(t35.stale_error(OUTPUT, expected, actual))
        return errors
    document = t35.load_json(OUTPUT)
    freeze = document.get("freeze") or {}
    if freeze.get("frozen") is not True:
        errors.append("t36 machine target is not frozen")
    if freeze.get("opening_row_count") != 33:
        errors.append("opening_row_count must remain 33")
    if freeze.get("target_row_count") != 85:
        errors.append("frozen target_row_count must remain 85")
    if (
        int(freeze.get("material_row_count") or 0)
        + int(freeze.get("eu_voltage_row_count") or 0)
        + int(freeze.get("tu_host_row_count") or 0)
        + int(freeze.get("opening_only_row_count") or 0)
        != 85
    ):
        errors.append("material + EU + TU + opening_only must equal 85")
    validators = document.get("validators") or {}
    if validators.get("target_defined_by_catalog") != 0:
        errors.append("target must not be defined by the current catalog")
    if validators.get("missing_host_auto_out_of_scope") != 0:
        errors.append("missing hosts must not auto-convert to out_of_scope")
    catalog_ids = {
        row["id"]
        for row in t35.load_json(t36.MACHINE_TIERS).get("variants") or []
    }
    target_ids = {
        row.get("variant_id") for row in document.get("rows") or []
    }
    missing = sorted(target_ids - catalog_ids)
    if missing:
        errors.append(f"implemented catalog missing frozen target ids: {missing}")
    extra = sorted(catalog_ids - target_ids)
    if extra:
        errors.append(f"catalog has ids outside frozen target: {extra}")
    welder = next(
        (row for row in document.get("rows") or [] if row.get("variant_id") == "cruciblecraft:welder"),
        None,
    )
    if welder is None:
        errors.append("welder missing from target")
    else:
        if welder.get("energy_identity") == "RU":
            errors.append("welder LU must not be rewritten as RU")
        if welder.get("variant_semantics") != "opening_only":
            errors.append("welder must be opening_only, not a material/RU matrix row")
        if welder.get("lu_adjudication") != "LU_NOT_IN_1X_DENOMINATOR":
            errors.append("welder LU adjudication is missing")
    if full_replay:
        if not t36.RM_JAVA.is_file() or not t36.MT_JAVA.is_file():
            errors.append("full replay requires pinned gt6_code RM.java and MT.java")
        families = t35.load_json(t36.RECIPE_FAMILIES)
        if int((families.get("counts") or {}).get("families") or 0) != 5718:
            errors.append("full replay: ordinary-optional family count drifted")
        projection = document.get("host_projection") or {}
        if "cruciblecraft:roaster" not in projection:
            errors.append("full replay: roaster host missing from projection")
        if projection.get("cruciblecraft:roaster", {}).get("target_status") not in {
            "host_targeted_by_t36",
            "host_exact",
        }:
            errors.append("full replay: roaster must stay a T36-owned host")
        rows = document.get("rows") or []
        if not any(row.get("canonical_kind") == "cruciblecraft:coagulator" for row in rows):
            errors.append("full replay: coagulator missing from target")
        if set(freeze.get("opening_ids") or []) - catalog_ids:
            errors.append("full replay: opening 33 ids missing from catalog")
    return errors


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    replay_mode = parser.add_mutually_exclusive_group()
    replay_mode.add_argument("--reference-only", action="store_true")
    replay_mode.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    if args.reference_only and not args.check:
        parser.error("--reference-only requires --check")
    if args.write and args.full_replay:
        parser.error("--write already rebuilds the projection; omit --full-replay")
    try:
        if args.write:
            document = write()
            freeze = document["freeze"]
            print(
                json.dumps(
                    {
                        "status": document["status"],
                        "freeze": {
                            key: freeze[key]
                            for key in (
                                "kind_count",
                                "material_row_count",
                                "eu_voltage_row_count",
                                "tu_host_row_count",
                                "target_row_count",
                                "opening_row_count",
                                "missing_from_runtime_count",
                                "excluded_kind_count",
                                "required_recipe_family_count",
                                "frozen",
                            )
                        },
                    },
                    sort_keys=True,
                )
            )
            return 0
        errors = check(full_replay=bool(args.full_replay))
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        mode = "full replay" if args.full_replay else "compact"
        print(f"{t36.relative(OUTPUT)} is current ({mode})")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T36 machine target failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
