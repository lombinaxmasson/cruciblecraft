#!/usr/bin/env python3
"""Build the current GT6 source-to-runtime coverage reconciliation.

This report deliberately keeps source rows, runtime capabilities, published
recipe files, overflow evidence, and player-path blockers as separate axes.
It is an audit artifact, not a new capability card and not a replacement for
the per-wave production locks.

Recipe coverage is measured in GT6 source rows: every CC recipe row that
carries a row-level evidence hash is attributed to the GT6 map it declares
(``gt.recipe.<map>#NNNN`` template, dump path, or the owning CC map). The
attribution is verified against the local ``gt6_dump`` once and pinned in
``source_attribution.json``; ``--check`` then runs without reference trees and
fails as soon as the recipe tree's evidence set drifts from that pin.
"""
from __future__ import annotations

import argparse
import fnmatch
import gzip
import hashlib
import json
import re
import subprocess
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[4]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

WAVE_ROOT = ROOT / "tools" / "waves" / "portfolio" / "gt6-full-coverage-reassessment"
SCOPE_PATH = WAVE_ROOT / "scope.json"
OUTPUT_PATH = WAVE_ROOT / "coverage.json"
CHEM_OUTPUT_PATH = WAVE_ROOT / "chem_thermal.json"
ATTRIBUTION_PATH = WAVE_ROOT / "source_attribution.json"
SEMANTIC_BUILDER_PATH = WAVE_ROOT / "build_semantic_coverage.py"
SEMANTIC_OUTPUT_PATH = WAVE_ROOT / "semantic_coverage.json"
MARKDOWN_PATH = ROOT / "docs" / "current" / "gt6-full-coverage.md"
WORKFLOW_DOC = "docs/current/gt6-full-coverage-workflow.md"

DUMP_MAPS = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"
NORMALIZED_REFERENCE = ROOT / "tools" / "gt6_recipe_normalized_reference.json"
CACHE_DIR = ROOT / "build" / "gt6_full_coverage"
RESOURCE_ROOTS_PATH = ROOT / "tools" / "generated_resource_roots.json"
SOURCE_SETS_GRADLE = ROOT / "gradle" / "scripts" / "source-sets.gradle"
ENERGY_TYPE_JAVA = (
    ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft" / "api" / "energy" / "EnergyType.java"
)
MTE_INPLACE_CATALOG = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "mte_inplace_catalog.json"
)

MAP_CREATE_RE = re.compile(r'create\("([a-z0-9_]+)"\)')
TEMPLATE_RE = re.compile(r"^((?:gt|mc)\.recipe\.[a-z0-9.]+)#\d+$")
DUMP_ROW_RE = re.compile(r"maps/((?:gt|mc)\.recipe\.[a-z0-9.]+)\.json#recipes\[(\d+)\]$")
FULL_HASH_RE = re.compile(r"^[0-9a-f]{64}$")
SHORT_HASH_RE = re.compile(r"^[0-9a-f]{20}$")

GT_RECIPE = "cruciblecraft:gt_recipe"
COMPACT_FAMILY = "cruciblecraft:compact_gt_recipe_family"
MATERIAL_RULE = "cruciblecraft:material_rule"

CAPABILITY_ALIASES: dict[str, tuple[str, ...]] = {
    "bath": ("machines/bath", "machines/large-bathing-vat"),
    "centrifuge": ("machines/large-centrifuge",),
    "coke_oven": ("machines/coke-oven",),
    "coagulator": ("machines/large-coagulator",),
    "cryo_distillation_tower": ("machines/distillation-tower",),
    "distillation_tower": ("machines/distillation-tower",),
    "electrolyzer": ("machines/large-electrolyzer",),
    "fermenter": ("machines/large-fermenter",),
    "implosion_compressor": ("machines/implosion-compressor",),
    "mixer": ("machines/large-mixer",),
    "oven": ("machines/oven", "machines/large-oven"),
    "shredder": ("machines/large-shredder",),
    "sluice": ("machines/large-sluice",),
    "squeezer": ("machines/large-squeezer",),
    "steam_cracker": ("recipe/gt6-steamcracking-bulk",),
    "steamcracking": ("recipe/gt6-steamcracking-bulk",),
}

# GT6 map key -> CC RecipeMap path. The fuel entries follow row evidence:
# CC fuels_gas carries gt.recipe.fuels.burn rows and CC fuels_gas_turbine
# carries gt.recipe.fuels.gas rows.
LOCAL_MAP_ALIASES = {
    "autocrafting": "autocrafter",
    "cokeoven": "coke_oven",
    "cryodistillationtower": "cryo_distillation_tower",
    "cryomixer": "cryo_mixer",
    "distillationtower": "distillation_tower",
    "fuels_burn": "fuels_gas",
    "fuels_gas": "fuels_gas_turbine",
    "fusionreactor": "fusion",
    "implosioncompressor": "implosion_compressor",
    "laserengraver": "laser_engraver",
    "magneticseparator": "magnetic_separator",
    "scannermolecular": "scanner",
    "sharpener": "sanding",
}

# CC RecipeMaps that have no same-named GT6 map but still carry GT6 content.
# gt6_recipe_normalized_reference.json files component_bender under
# gt.recipe.rollbender.
CC_MAP_GT6_OWNERS = {
    "bender": "gt.recipe.rollbender",
}

MULTIBLOCK_CAPABILITY_ALIASES: dict[str, tuple[str, ...]] = {
    "bath": ("machines/large-bathing-vat",),
    "crucible": ("machines/large-crucible",),
    "cryo_distillation_tower": ("machines/distillation-tower",),
    "fusion_reactor": ("energy/fusion-quantum",),
    "large_heat_exchanger": ("energy/large-heat-exchanger",),
    "large_turbine_gas": ("energy/large-gas-turbine",),
    "large_turbine_steam": ("energy/steam-turbine",),
    "logistics_core": ("logistics/logistics-core",),
    "matter_fabricator": ("machines/gt6-coil-hosts",),
}

MACHINE_KIND_CAPABILITY_ALIASES: dict[str, tuple[str, ...]] = {
    "MultiTileEntityAxle": ("content/gt6-mte-drive-runtime",),
    "MultiTileEntityEngineRotation": ("content/gt6-mte-drive-runtime",),
    "MultiTileEntityGearBox": ("content/gt6-mte-drive-runtime",),
    "MultiTileEntityBatteryBox": ("content/gt6-mte-converter-remainder-runtime",),
    "MultiTileEntityReactorCore2x2": (
        "energy/nuclear-fission-hot-fluids",
        "energy/nuclear-fission-observation-safety",
        "energy/nuclear-fission-survival",
    ),
}

# GT6 RM field -> CC delivery host map, where the names differ.
KIND_HOST_ALIASES = {
    "furnace": "oven",
}

# GT6 TD.Energy symbol -> CC EnergyType constant. Only reported when the
# constant exists in the live enum.
ENERGY_SYMBOL_ALIASES = {
    "CRYO": "CU",
    "ELECTRICITY": "ELECTRIC",
    "LIGHT": "LU",
    "MAGNETIC": "MU",
}

CHEMICAL_THERMAL_NAMES = {
    "autoclave",
    "bath",
    "burnmixer",
    "catalyticcracking",
    "coagulator",
    "compressor",
    "cokeoven",
    "coke_oven",
    "cryo_distillation_tower",
    "cryodistillationtower",
    "cryomixer",
    "cryo_mixer",
    "crystallisationcrucible",
    "distillationtower",
    "distillation_tower",
    "distillery",
    "drying",
    "electrolyzer",
    "fermenter",
    "freezer",
    "melter",
    "mixer",
    "roaster",
    "smelter",
    "steamcracking",
}


class StaleAttribution(RuntimeError):
    """The recipe tree's evidence set no longer matches source_attribution.json."""


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_semantic_coverage() -> dict[str, Any]:
    if not SEMANTIC_OUTPUT_PATH.is_file():
        raise FileNotFoundError(
            f"{SEMANTIC_OUTPUT_PATH.relative_to(ROOT)} is missing; run the "
            "semantic coverage builder first"
        )
    return read_json(SEMANTIC_OUTPUT_PATH)


def run_semantic_builder(mode: str) -> None:
    completed = subprocess.run(
        [sys.executable, str(SEMANTIC_BUILDER_PATH), mode],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        detail = (completed.stdout + completed.stderr).strip()
        raise RuntimeError(
            f"semantic coverage builder {mode} failed"
            + (f": {detail}" if detail else "")
        )


def local_source_revision() -> str | None:
    """HEAD of the gitignored GT6 tree, or None when that tree is absent."""
    source_root = ROOT / "gt6_code" / "gregtech6"
    try:
        completed = subprocess.run(
            ["git", "-C", str(source_root), "rev-parse", "HEAD"],
            check=True,
            capture_output=True,
            text=True,
        )
    except (OSError, subprocess.CalledProcessError):
        return None
    return completed.stdout.strip() or None


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def map_key(name_internal: str) -> str:
    value = re.sub(r"^(gt|mc)\.recipe\.", "", name_internal or "")
    key = re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_") or "unnamed"
    return LOCAL_MAP_ALIASES.get(key, key)


def load_rule_expansion_counts() -> dict[str, int]:
    manifest = read_json(ROOT / "tools" / "component_rule_manifest.json")
    return {
        name: int(entry.get("expanded_recipes") or 0)
        for name, entry in (manifest.get("per_map") or {}).items()
    }


def load_capabilities() -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for path in sorted((ROOT / "tools" / "capabilities").rglob("capability.json")):
        value = read_json(path)
        value["_path"] = path.relative_to(ROOT).as_posix()
        result[value["slug"]] = value
    return result


def load_blockers() -> list[dict[str, Any]]:
    value = read_json(ROOT / "tools" / "blockers" / "catalog.json")
    entries = value.get("entries", value)
    if not isinstance(entries, list):
        raise ValueError("blocker catalog does not contain an entries list")
    return entries


def load_recipe_maps() -> list[dict[str, Any]]:
    value = read_json(ROOT / "tools" / "machine_tree_denominators" / "recipe_maps.json")
    rows = value.get("rows")
    if not isinstance(rows, list):
        raise ValueError("recipe denominator does not contain rows")
    return rows


def load_recipe_map_constants() -> set[str]:
    path = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft" / "registry" / "ModRecipeMaps.java"
    text = path.read_text(encoding="utf-8")
    return set(MAP_CREATE_RE.findall(text))


def denominator_summary(path: Path) -> dict[str, Any]:
    value = read_json(path)
    counts = value.get("counts")
    if isinstance(counts, dict):
        summary = {
            key: counts[key]
            for key in (
                "actual_map_files",
                "canonical_kinds",
                "identities",
                "prefixes",
                "domains",
                "recipes",
                "unclassified",
            )
            if key in counts
        }
    else:
        summary = {}
    rows = value.get("rows")
    if isinstance(rows, list):
        summary["row_count"] = len(rows)
    for key in ("canonical_kinds", "identities", "domains"):
        if isinstance(value.get(key), list):
            summary[key + "_count"] = len(value[key])
    if isinstance(value.get("field_cardinality"), dict):
        summary["field_cardinality"] = value["field_cardinality"]
    return {
        "path": path.relative_to(ROOT).as_posix(),
        "summary": summary,
    }


def load_denominator_summaries() -> list[dict[str, Any]]:
    names = (
        "recipe_maps.json",
        "prefixes.json",
        "itemgenerator_domains.json",
        "machine_kinds.json",
        "energy_identities.json",
        "cover_kinds.json",
        "multiblock_kinds.json",
    )
    base = ROOT / "tools" / "machine_tree_denominators"
    return [denominator_summary(base / name) for name in names]


def load_machine_kind_summary() -> list[dict[str, Any]]:
    value = read_json(ROOT / "tools" / "machine_tree_denominators" / "machine_kinds.json")
    result = []
    for row in value.get("canonical_kinds", []):
        result.append(
            {
                "canonical_key": row.get("canonical_key"),
                "behavior_class": row.get("behavior_class"),
                "recipe_map": row.get("recipe_map"),
                "accepted_energy": row.get("accepted_energy"),
                "emitted_energy": row.get("emitted_energy"),
                "classification": row.get("classification"),
                "owner": row.get("owner"),
                "source_ids": row.get("source_ids", []),
                "fuel_map": row.get("fuel_map"),
                "variant_count": len(row.get("variants", [])),
                "reason": row.get("reason"),
            }
        )
    return result


def squash(value: str) -> str:
    return re.sub(r"[^a-z0-9]", "", value.lower())


# --------------------------------------------------------------------------
# Runtime recipe scan
# --------------------------------------------------------------------------


@dataclass(frozen=True)
class CcRecipeRow:
    cc_map: str
    declared: str | None
    dump_ref: tuple[str, int] | None
    full: tuple[str, ...]
    short: tuple[str, ...]
    source_kind: str


def runtime_resource_roots() -> list[Path]:
    roots = [ROOT / "src" / "main" / "resources"]
    for row in read_json(RESOURCE_ROOTS_PATH).get("roots", []):
        roots.append(ROOT / str(row["path"]))
    return roots


def _gradle_glob_regex(pattern: str) -> re.Pattern[str]:
    out = []
    index = 0
    while index < len(pattern):
        if pattern.startswith("**/", index):
            out.append("(?:.*/)?")
            index += 3
        elif pattern.startswith("**", index):
            out.append(".*")
            index += 2
        elif pattern[index] == "*":
            out.append("[^/]*")
            index += 1
        elif pattern[index] == "?":
            out.append("[^/]")
            index += 1
        else:
            out.append(re.escape(pattern[index]))
            index += 1
    return re.compile("^" + "".join(out) + "$")


def gradle_resource_excludes() -> list[str]:
    text = SOURCE_SETS_GRADLE.read_text(encoding="utf-8")
    match = re.search(r"sourceSets\.main\.resources\s*\{(.*?)\n\}", text, re.S)
    if match is None:
        raise ValueError("source-sets.gradle has no sourceSets.main.resources block")
    return re.findall(r'exclude\("([^"]+)"\)', match.group(1))


def runtime_recipe_files() -> Iterable[Path]:
    excludes = [_gradle_glob_regex(pattern) for pattern in gradle_resource_excludes()]
    for root in runtime_resource_roots():
        data = root / "data"
        if not data.is_dir():
            continue
        for namespace in sorted(data.iterdir()):
            recipe_root = namespace / "recipe"
            if not recipe_root.is_dir():
                continue
            for path in sorted(recipe_root.rglob("*.json")):
                relative = path.relative_to(root).as_posix()
                if any(pattern.match(relative) for pattern in excludes):
                    continue
                yield path


def _map_path(value: Any) -> str | None:
    if isinstance(value, str) and ":" in value:
        return value.split(":", 1)[1]
    return None


def declared_source(selected: Any, family_id: Any) -> tuple[str | None, tuple[str, int] | None]:
    text = str(selected or "")
    template = TEMPLATE_RE.match(text)
    if template:
        return template.group(1), None
    dump = DUMP_ROW_RE.search(text)
    if dump:
        return dump.group(1), (dump.group(1), int(dump.group(2)))
    family = TEMPLATE_RE.match(str(family_id or ""))
    if family:
        return family.group(1), None
    return None, None


def _row(cc_map: str, provenance: dict[str, Any], family_id: Any, hashes: Iterable[Any]) -> CcRecipeRow:
    declared, dump_ref = declared_source(provenance.get("selected_source_recipe"), family_id)
    values = [str(value) for value in hashes if value]
    return CcRecipeRow(
        cc_map=cc_map,
        declared=declared,
        dump_ref=dump_ref,
        full=tuple(sorted({value for value in values if FULL_HASH_RE.match(value)})),
        short=tuple(sorted({value for value in values if SHORT_HASH_RE.match(value)})),
        source_kind=str(provenance.get("source_kind") or "none"),
    )


def scan_cc_recipes() -> tuple[list[CcRecipeRow], dict[str, int]]:
    """Every runtime-loaded GT-map recipe row and material_rule file per CC map."""
    rows: list[CcRecipeRow] = []
    rule_files: Counter[str] = Counter()
    for path in runtime_recipe_files():
        try:
            value = read_json(path)
        except (OSError, json.JSONDecodeError):
            continue
        if not isinstance(value, dict):
            continue
        kind = value.get("type")
        if kind == GT_RECIPE:
            cc_map = _map_path(value.get("map"))
            if cc_map:
                provenance = value.get("provenance") or {}
                rows.append(_row(cc_map, provenance, None, provenance.get("evidence_hashes") or []))
        elif kind == COMPACT_FAMILY:
            cc_map = _map_path(value.get("target_map"))
            if not cc_map:
                continue
            family_id = value.get("family_id")
            matrix = value.get("matrix")
            if isinstance(matrix, dict):
                shared = matrix.get("shared") or {}
                hashes = (matrix.get("dicts") or {}).get("hashes") or []
                for entry in matrix.get("rows") or []:
                    evidence = [hashes[int(entry[4])]] if len(entry) > 4 and int(entry[4]) < len(hashes) else []
                    rows.append(_row(cc_map, shared, family_id, evidence))
            for relation in value.get("relations") or []:
                provenance = relation.get("provenance") or {}
                rows.append(_row(cc_map, provenance, family_id, provenance.get("evidence_hashes") or []))
        elif kind == MATERIAL_RULE:
            cc_map = _map_path(value.get("target"))
            if cc_map:
                rule_files[cc_map] += 1
    return rows, dict(sorted(rule_files.items()))


# --------------------------------------------------------------------------
# Source-row attribution
# --------------------------------------------------------------------------


def name_owners(recipe_rows: list[dict[str, Any]]) -> dict[str, str]:
    """CC map -> GT6 map by registry name; used when a row declares no map."""
    owners: dict[str, str] = {}
    for row in recipe_rows:
        name = row.get("name_internal") or ""
        if not name:
            continue
        owners.setdefault(map_key(name), name)
    for cc_map, gt_map in CC_MAP_GT6_OWNERS.items():
        owners.setdefault(cc_map, gt_map)
    return owners


def base_source(row: CcRecipeRow, owners: dict[str, str]) -> str | None:
    return row.declared or owners.get(row.cc_map)


def evidence_items(rows: list[CcRecipeRow], owners: dict[str, str]) -> list[str]:
    items: set[str] = set()
    for row in rows:
        base = base_source(row, owners) or "-"
        items.update(f"F|{base}|{value}" for value in row.full)
        items.update(f"S|{base}|{value}" for value in row.short)
        if row.dump_ref is not None:
            items.add(f"D|{row.dump_ref[0]}|{row.dump_ref[1]}")
    return sorted(items)


def evidence_digest(rows: list[CcRecipeRow], owners: dict[str, str]) -> str:
    return sha256_text("\n".join(evidence_items(rows, owners)))


def _dump_fingerprint() -> str:
    parts = [
        f"{path.name}:{path.stat().st_size}"
        for path in sorted(DUMP_MAPS.glob("*.json"))
    ]
    return sha256_text("\n".join(parts))


def _row_hash(recipe: Any) -> str:
    return sha256_text(json.dumps(recipe, sort_keys=True, separators=(",", ":"), ensure_ascii=False))


def load_dump_rows() -> dict[str, list[str]]:
    """GT6 map -> row sha256 in dump order, cached under build/."""
    if not DUMP_MAPS.is_dir():
        raise FileNotFoundError(
            f"{DUMP_MAPS.relative_to(ROOT)} is missing; source attribution needs the local GT6 dump"
        )
    fingerprint = _dump_fingerprint()
    cache = CACHE_DIR / "dump_rows.json.gz"
    if cache.is_file():
        with gzip.open(cache, "rt", encoding="utf-8") as handle:
            cached = json.load(handle)
        if cached.get("fingerprint") == fingerprint:
            return cached["maps"]
    maps: dict[str, list[str]] = {}
    for path in sorted(DUMP_MAPS.glob("*.json")):
        document = read_json(path)
        recipes = document.get("recipes") if isinstance(document, dict) else None
        maps[path.stem] = [_row_hash(recipe) for recipe in recipes or []]
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    with gzip.open(cache, "wt", encoding="utf-8") as handle:
        json.dump({"fingerprint": fingerprint, "maps": maps}, handle)
    return maps


def load_reference_rows() -> dict[str, set[str]]:
    """20-hex normalized-reference row hash -> GT6 maps, cached under build/."""
    if not NORMALIZED_REFERENCE.is_file():
        raise FileNotFoundError(
            f"{NORMALIZED_REFERENCE.relative_to(ROOT)} is missing; ore-chain attribution needs it"
        )
    fingerprint = hashlib.sha256(NORMALIZED_REFERENCE.read_bytes()).hexdigest()
    cache = CACHE_DIR / "reference_rows.json.gz"
    if cache.is_file():
        with gzip.open(cache, "rt", encoding="utf-8") as handle:
            cached = json.load(handle)
        if cached.get("fingerprint") == fingerprint:
            return {key: set(value) for key, value in cached["rows"].items()}
    rows: dict[str, set[str]] = defaultdict(set)
    for family_rows in read_json(NORMALIZED_REFERENCE).get("families", {}).values():
        for row in family_rows:
            rows[_row_hash(row)[:20]].add(str(row.get("map") or ""))
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    with gzip.open(cache, "wt", encoding="utf-8") as handle:
        json.dump(
            {"fingerprint": fingerprint, "rows": {key: sorted(value) for key, value in rows.items()}},
            handle,
        )
    return rows


def build_attribution(rows: list[CcRecipeRow], owners: dict[str, str]) -> dict[str, Any]:
    """Verify every evidence hash against the dump. Needs gt6_dump locally.

    A hash stays on its declared map when that map contains the row. GT6
    registers many identical rows in several maps (melter/smelter,
    compressor/rollingmill, mortar/shredder), so a bare hash lookup is not
    enough to pick the map.
    """
    dump = load_dump_rows()
    reference = load_reference_rows()
    dump_members: dict[str, set[str]] = {name: set(hashes) for name, hashes in dump.items()}
    maps_of_hash: dict[str, set[str]] = defaultdict(set)
    for name, hashes in dump.items():
        for value in hashes:
            maps_of_hash[value].add(name)

    full_overrides: dict[str, str] = {}
    short_overrides: dict[str, str] = {}
    unverified_full: set[str] = set()
    unverified_short: set[str] = set()
    ambiguous: set[str] = set()
    dump_refs: dict[str, str | None] = {}
    for row in rows:
        base = base_source(row, owners)
        for value in row.full:
            if base is not None and value in dump_members.get(base, ()):
                continue
            candidates = maps_of_hash.get(value, set())
            if not candidates:
                unverified_full.add(value)
            elif len(candidates) == 1:
                full_overrides[f"{base}|{value}"] = next(iter(candidates))
            else:
                ambiguous.add(value)
        for value in row.short:
            candidates = reference.get(value, set())
            if base is not None and base in candidates:
                continue
            if not candidates:
                unverified_short.add(value)
            elif len(candidates) == 1:
                short_overrides[f"{base}|{value}"] = next(iter(candidates))
            else:
                ambiguous.add(value)
        if row.dump_ref is not None:
            name, index = row.dump_ref
            hashes = dump.get(name, [])
            dump_refs[f"{name}#{index}"] = hashes[index] if index < len(hashes) else None
    return {
        "schema_version": 1,
        "assessment_id": "gt6-full-coverage-reassessment",
        "generated_by": "tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py --write",
        "policy": (
            "Pins which GT6 map each CC evidence hash belongs to. Written only with the "
            "local gt6_dump present; --check compares evidence_digest and fails when the "
            "recipe tree drifts."
        ),
        "evidence_digest": evidence_digest(rows, owners),
        "evidence_item_count": len(evidence_items(rows, owners)),
        "dump_fingerprint": _dump_fingerprint(),
        "reference_sha256": hashlib.sha256(NORMALIZED_REFERENCE.read_bytes()).hexdigest(),
        "full_overrides": dict(sorted(full_overrides.items())),
        "short_overrides": dict(sorted(short_overrides.items())),
        "ambiguous_hashes": sorted(ambiguous),
        "unverified_full_hashes": sorted(unverified_full),
        "unverified_short_hashes": sorted(unverified_short),
        "dump_refs": dict(sorted(dump_refs.items())),
    }


def resolve_attribution(
    rows: list[CcRecipeRow],
    owners: dict[str, str],
    *,
    refresh: bool,
) -> dict[str, Any]:
    digest = evidence_digest(rows, owners)
    pinned = read_json(ATTRIBUTION_PATH) if ATTRIBUTION_PATH.is_file() else None
    if pinned is not None and pinned.get("evidence_digest") == digest:
        return pinned
    if not refresh:
        raise StaleAttribution(
            f"{ATTRIBUTION_PATH.relative_to(ROOT)} does not match the recipe tree; run "
            "build_reconciliation.py --write on a machine with gt6_dump "
            f"(see {WORKFLOW_DOC})"
        )
    return build_attribution(rows, owners)


@dataclass
class MapTrace:
    exact: set[str]
    reference: set[str]
    exact_hosts: Counter[str]
    reference_hosts: Counter[str]


def attribute_rows(
    rows: list[CcRecipeRow],
    owners: dict[str, str],
    attribution: dict[str, Any],
) -> tuple[dict[str, MapTrace], dict[str, Counter[str]], dict[str, int]]:
    """GT6 map -> traced source rows; CC map -> untraced row kinds; CC map -> row count."""
    full_overrides = attribution.get("full_overrides") or {}
    short_overrides = attribution.get("short_overrides") or {}
    unverified = set(attribution.get("unverified_full_hashes") or []) | set(
        attribution.get("unverified_short_hashes") or []
    )
    ambiguous = set(attribution.get("ambiguous_hashes") or [])
    dump_refs = attribution.get("dump_refs") or {}
    traces: dict[str, MapTrace] = defaultdict(
        lambda: MapTrace(set(), set(), Counter(), Counter())
    )
    untraced: dict[str, Counter[str]] = defaultdict(Counter)
    cc_rows: Counter[str] = Counter()
    for row in rows:
        cc_rows[row.cc_map] += 1
        base = base_source(row, owners)
        exact_targets: set[str] = set()
        reference_targets: set[str] = set()
        for value in row.full:
            if value in unverified or value in ambiguous:
                continue
            target = full_overrides.get(f"{base}|{value}", base)
            if target:
                traces[target].exact.add(value)
                exact_targets.add(target)
        if row.dump_ref is not None:
            name, index = row.dump_ref
            value = dump_refs.get(f"{name}#{index}")
            if value:
                traces[name].exact.add(value)
                exact_targets.add(name)
        for value in row.short:
            if value in unverified or value in ambiguous:
                continue
            target = short_overrides.get(f"{base}|{value}", base)
            if target:
                traces[target].reference.add(value)
                reference_targets.add(target)
        for target in exact_targets:
            traces[target].exact_hosts[row.cc_map] += 1
        for target in reference_targets - exact_targets:
            traces[target].reference_hosts[row.cc_map] += 1
        if not exact_targets and not reference_targets:
            untraced[row.cc_map][row.source_kind] += 1
    return dict(traces), dict(untraced), dict(cc_rows)


# --------------------------------------------------------------------------
# Non-recipe axes
# --------------------------------------------------------------------------


class EvidenceIndex:
    """Where a GT6 behavior class shows up on the CC side."""

    def __init__(self, capabilities: dict[str, dict[str, Any]]) -> None:
        self.capabilities = capabilities
        self.capability_texts = {
            slug: json.dumps(value, ensure_ascii=False)
            for slug, value in capabilities.items()
        }
        java_root = ROOT / "src" / "main" / "java"
        self.java_texts = {
            path.relative_to(ROOT).as_posix(): path.read_text(encoding="utf-8", errors="ignore")
            for path in java_root.rglob("*.java")
        }
        data_root = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
        data_paths = list(data_root.glob("*.json"))
        data_paths.extend((data_root / "multiblock_structures").glob("*.json"))
        self.data_texts = {
            path.relative_to(ROOT).as_posix(): path.read_text(encoding="utf-8", errors="ignore")
            for path in data_paths
        }
        self.owned_patterns = {
            slug: [str(pattern) for pattern in value.get("owned_paths") or []]
            for slug, value in capabilities.items()
        }
        self._owner_cache: dict[str, list[str]] = {}

    def owners_of(self, path: str) -> list[str]:
        cached = self._owner_cache.get(path)
        if cached is None:
            cached = sorted(
                slug
                for slug, patterns in self.owned_patterns.items()
                if any(fnmatch.fnmatchcase(path, pattern) for pattern in patterns)
            )
            self._owner_cache[path] = cached
        return cached

    def lookup(
        self, class_names: Iterable[str], *, allow_short: bool = True
    ) -> dict[str, Any]:
        full_names = [name for name in class_names if name]
        tokens = list(full_names)
        if allow_short:
            tokens.extend(name.removeprefix("MultiTileEntity") for name in full_names)
        tokens = [token for token in tokens if token]
        pattern = (
            re.compile(r"\b(?:" + "|".join(re.escape(token) for token in tokens) + r")\b")
            if tokens
            else None
        )
        cap_slugs = sorted(
            slug
            for slug, text in self.capability_texts.items()
            if pattern is not None and pattern.search(text)
        )
        java_files = sorted(
            path
            for path, text in self.java_texts.items()
            if any(name in text for name in full_names)
        )
        data_files = sorted(
            path
            for path, text in self.data_texts.items()
            if any(name in text for name in full_names)
        )
        owner_slugs: set[str] = set()
        for path in [*java_files, *data_files]:
            owners = self.owners_of(path)
            if 0 < len(owners) <= 3:
                owner_slugs.update(owners)
        cap_slugs = sorted(set(cap_slugs) | owner_slugs)
        return {
            "capabilities": capability_snapshot(cap_slugs, self.capabilities),
            "owner_attributed": sorted(owner_slugs),
            "java_files": java_files[:12],
            "java_file_count": len(java_files),
            "data_files": data_files[:12],
            "data_file_count": len(data_files),
        }


def evidence_depth(evidence: dict[str, Any], denominator_class: str | None) -> str:
    caps = evidence["capabilities"]
    if any(cap["maturity"] == "runtime_ready" and cap["workflow"] == "accepted" for cap in caps):
        return "runtime_accepted"
    if caps:
        return "runtime_paused"
    if evidence["java_file_count"]:
        return "runtime_code_uncarded"
    if evidence["data_file_count"]:
        return "identity_only"
    if denominator_class == "out_of_scope":
        return "legacy_exclusion_pending"
    return "denominator_only"


def load_multiblock_summary(
    capabilities: dict[str, dict[str, Any]],
    evidence_index: "EvidenceIndex",
) -> list[dict[str, Any]]:
    value = read_json(ROOT / "tools" / "machine_tree_denominators" / "multiblock_kinds.json")
    result = []
    for row in value.get("canonical_kinds", []):
        canonical = str(row.get("canonical_id") or "")
        hyphen = canonical.replace("_", "-")
        candidates = [
            f"machines/large-{hyphen}",
            f"machines/{hyphen}",
            f"energy/large-{hyphen}",
            f"energy/{hyphen}",
            f"logistics/{hyphen}",
        ]
        raw_members = [str(name) for name in row.get("raw_members") or []]
        evidence = evidence_index.lookup(raw_members, allow_short=False)
        own_slugs = [
            slug
            for slug in MULTIBLOCK_CAPABILITY_ALIASES.get(canonical, ())
            if slug in capabilities
        ] or [slug for slug in candidates if slug in capabilities]
        if own_slugs:
            evidence["capabilities"] = capability_snapshot(own_slugs, capabilities)
        structure_root = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "multiblock_structures"
        structures = sorted(
            path.relative_to(ROOT).as_posix()
            for path in structure_root.glob(f"{canonical}*.json")
        )
        evidence["structure_files"] = structures
        if structures and not evidence["java_file_count"]:
            evidence["java_file_count"] = len(structures)
        depth = evidence_depth(evidence, row.get("disposition"))
        result.append(
            {
                "canonical_id": canonical,
                "raw_members": raw_members,
                "disposition": row.get("disposition"),
                "implementation_status": row.get("implementation_status"),
                "roadmap_bucket": row.get("roadmap_bucket"),
                "cc_multiblock": row.get("cc_multiblock"),
                "owner": row.get("deferred", {}).get("owner")
                if isinstance(row.get("deferred"), dict)
                else None,
                "capabilities": evidence["capabilities"],
                "evidence": evidence,
                "delivery_depth": depth,
            }
        )
    return result


def load_live_cover_definitions() -> list[dict[str, Any]]:
    definitions = []
    data_root = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
    for path in sorted(data_root.glob("*cover_definitions.json")):
        value = read_json(path)
        for definition in value.get("definitions", []):
            if definition.get("id"):
                definitions.append(
                    {
                        "id": definition["id"],
                        "behavior": definition.get("behavior"),
                        "source_file": path.relative_to(ROOT).as_posix(),
                    }
                )
    return definitions


def load_cover_summary(
    live_definitions: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    value = read_json(ROOT / "tools" / "machine_tree_denominators" / "cover_kinds.json")
    live_by_suffix: dict[str, list[dict[str, Any]]] = {}
    for definition in live_definitions:
        suffix = str(definition["id"]).split(":")[-1]
        live_by_suffix.setdefault(squash(suffix), []).append(definition)
    return [
        {
            "canonical_id": row.get("canonical_id"),
            "disposition": row.get("disposition"),
            "implementation_status": row.get("implementation_status"),
            "raw_members": row.get("raw_members", []),
            "live_definitions": live_by_suffix.get(
                squash(str(row.get("canonical_id") or "")), []
            ),
        }
        for row in value.get("canonical_kinds", [])
    ]


def live_energy_types() -> list[str]:
    text = ENERGY_TYPE_JAVA.read_text(encoding="utf-8")
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    body = re.search(r"public enum EnergyType\s*\{(.*?);", text, re.S)
    if body is None:
        raise ValueError("EnergyType enum body not found")
    cleaned = re.sub(r"@\w+", "", body.group(1))
    return [token.strip() for token in cleaned.split(",") if token.strip()]


def load_energy_summary() -> list[dict[str, Any]]:
    value = read_json(ROOT / "tools" / "machine_tree_denominators" / "energy_identities.json")
    live = set(live_energy_types())
    result = []
    for row in value.get("rows", []):
        symbol = str(row.get("symbol") or "")
        candidate = ENERGY_SYMBOL_ALIASES.get(symbol, symbol)
        result.append(
            {
                "symbol": symbol,
                "long_name": row.get("long_name"),
                "classification": row.get("classification"),
                "frozen_local_energy_type": row.get("local_energy_type"),
                "live_energy_type": candidate if candidate in live else None,
                "owner": row.get("owner"),
            }
        )
    return result


def load_itemgenerator_summary() -> list[dict[str, Any]]:
    value = read_json(
        ROOT / "tools" / "machine_tree_denominators" / "itemgenerator_domains.json"
    )
    return [
        {
            "canonical_id": row.get("canonical_id"),
            "classification": row.get("classification"),
            "cc_current_domain": row.get("cc_current_domain"),
            "source_material_count": row.get("source_material_count"),
            "owner": row.get("owner"),
        }
        for row in value.get("records", [])
    ]


def load_prefix_summary() -> dict[str, Any]:
    from tools import gt6_resolve

    value = read_json(ROOT / "tools" / "machine_tree_denominators" / "prefixes.json")
    rows = []
    for row in value.get("records", []):
        canonical = str(row.get("canonical_id") or "")
        resolved = gt6_resolve.resolve_prefix(canonical)
        rows.append(
            {
                "canonical_id": canonical,
                "classification": row.get("classification"),
                "frozen_cc_mapped": bool(row.get("cc_mappings")),
                "cc_prefix": resolved.get("cc_prefix"),
                "cc_mapped": bool(resolved.get("live")),
                "explicit_unused": bool(row.get("explicit_unused")),
                "owner": row.get("owner"),
            }
        )
    return {"summary": value.get("summary", {}), "rows": rows}


def load_mte_disposition_summary() -> dict[str, Any]:
    path = (
        ROOT
        / "tools"
        / "waves"
        / "portfolio"
        / "mte-identity-disposition-r0"
        / "disposition_ledger.json"
    )
    value = read_json(path)
    catalog = read_json(MTE_INPLACE_CATALOG)
    inplace = {str(row.get("registry_path")): row for row in catalog.get("identities", [])}
    ledger_by_family: dict[str, Counter[str]] = {}
    live_by_family: dict[str, Counter[str]] = {}
    live_counts: Counter[str] = Counter()
    for row in value.get("identities", []):
        family = str(row.get("family") or "unknown")
        disposition = str(row.get("disposition"))
        ledger_by_family.setdefault(family, Counter())[disposition] += 1
        live = disposition
        if disposition != "realized_natively" and str(row.get("registry_path")) in inplace:
            live = "inplace_runtime"
        live_by_family.setdefault(family, Counter())[live] += 1
        live_counts[live] += 1
    return {
        "path": path.relative_to(ROOT).as_posix(),
        "inplace_catalog": MTE_INPLACE_CATALOG.relative_to(ROOT).as_posix(),
        "counts": value.get("counts", {}),
        "live_counts": dict(sorted(live_counts.items())),
        "by_family": {
            family: dict(sorted(counter.items()))
            for family, counter in sorted(ledger_by_family.items())
        },
        "live_by_family": {
            family: dict(sorted(counter.items()))
            for family, counter in sorted(live_by_family.items())
        },
    }


def delivery_hosts() -> dict[str, list[str]]:
    value = read_json(
        ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "machine_delivery.json"
    )
    result: dict[str, list[str]] = {}
    for host in value.get("hosts", []):
        recipe_map = str(host.get("recipe_map") or "")
        result.setdefault(squash(recipe_map.split(":")[-1]), []).append(host.get("id"))
    return result


def attach_machine_kind_evidence(
    kinds: list[dict[str, Any]],
    maps: list[dict[str, Any]],
    evidence_index: "EvidenceIndex",
) -> list[dict[str, Any]]:
    maps_by_key: dict[str, dict[str, Any]] = {}
    for row in maps:
        maps_by_key[squash(row["local_map"])] = row
        source_suffix = re.sub(r"^(gt|mc)\.recipe\.", "", str(row["source_map"] or ""))
        maps_by_key.setdefault(squash(source_suffix), row)
    rm_field_aliases = {
        "roasting": "roaster",
        "sharpening": "sharpener",
        "sifting": "sifter",
    }
    hosts = delivery_hosts()
    for kind in kinds:
        recipe_map = str(kind.get("recipe_map") or "")
        key = squash(recipe_map.replace("RM.", "")) if recipe_map.startswith("RM.") else ""
        key = rm_field_aliases.get(key, key)
        map_row = maps_by_key.get(key) if key else None
        if map_row is not None:
            host_key = squash(map_row["local_map"])
            kind["cc_hosts"] = (
                hosts.get(host_key, [])
                or hosts.get(key, [])
                or hosts.get(KIND_HOST_ALIASES.get(key, ""), [])
            )
            kind["recipe_map_depth"] = map_row["delivery_depth"]
            kind["evidence"] = None
            depth = map_row["delivery_depth"]
            if depth == "empty_source":
                # An empty dump map (vanilla furnace, NBT fake recipes, crafting
                # grid) still needs the machine itself.
                depth = "runtime_only" if kind["cc_hosts"] else "denominator_only"
            elif depth == "denominator_only" and kind["cc_hosts"]:
                depth = "runtime_only"
        else:
            behavior = str(kind.get("behavior_class") or "")
            evidence = evidence_index.lookup([behavior])
            alias_slugs = [
                slug
                for slug in MACHINE_KIND_CAPABILITY_ALIASES.get(behavior, ())
                if slug in evidence_index.capabilities
            ]
            if alias_slugs:
                merged = sorted({cap["slug"] for cap in evidence["capabilities"]} | set(alias_slugs))
                evidence["capabilities"] = capability_snapshot(merged, evidence_index.capabilities)
            kind["cc_hosts"] = []
            kind["recipe_map_depth"] = None
            kind["evidence"] = evidence
            depth = evidence_depth(evidence, kind.get("classification"))
        kind["delivery_depth"] = depth
    return kinds


def scan_overflow_files() -> list[dict[str, Any]]:
    result = []
    base = ROOT / "tools" / "waves"
    for path in sorted(base.rglob("overflow.json")):
        try:
            value = read_json(path)
        except (OSError, json.JSONDecodeError):
            continue
        if not isinstance(value, dict):
            continue
        blocked = value.get("blocked_rows")
        if not isinstance(blocked, int):
            overflow = value.get("overflow")
            blocked = len(overflow) if isinstance(overflow, list) else None
        relative = path.relative_to(ROOT).as_posix()
        result.append(
            {
                "path": relative,
                "blocked_rows": blocked,
                "status": value.get("status"),
                "wave_slug": value.get("wave_slug"),
            }
        )
    return result


def overflow_for_map(
    overflow_files: list[dict[str, Any]],
    map_name: str,
    owner_wave_dirs: Iterable[str] = (),
) -> tuple[int, list[dict[str, Any]]]:
    aliases = {
        map_name,
        map_name.replace("_", "-"),
        map_name.replace("-", "_"),
        *owner_wave_dirs,
    }
    matches = []
    for entry in overflow_files:
        haystack = " ".join(
            str(entry.get(key) or "").lower()
            for key in ("path", "wave_slug")
        )
        path_tokens = {
            token
            for token in re.split(r"[^a-z0-9]+", haystack)
            if token
        }
        if any(alias.lower() in path_tokens for alias in aliases):
            matches.append(dict(entry))
    if not matches:
        return 0, []
    exact_dirs = {alias.lower() for alias in aliases}
    exact = []
    for entry in matches:
        parts = str(entry.get("path") or "").lower().split("/")
        directories = parts[:-1]
        entry["shared_wave"] = not any(part in exact_dirs for part in directories)
        if not entry["shared_wave"]:
            exact.append(entry)
    if not exact:
        return 0, matches
    preferred = [
        entry
        for entry in exact
        if "/prep/" not in str(entry.get("path") or "")
    ]
    selected = max(
        preferred or exact,
        key=lambda entry: entry.get("blocked_rows") or 0,
    )
    return selected.get("blocked_rows") or 0, matches


def blocker_rows(entries: list[dict[str, Any]]) -> list[dict[str, Any]]:
    fields = (
        "id",
        "status",
        "planning_bucket",
        "unit",
        "count",
        "root_cause_class",
        "blocks_maturity",
        "title",
    )
    return [{field: entry.get(field) for field in fields} for entry in entries]


def load_form_census() -> dict[str, Any]:
    path = (
        ROOT
        / "tools"
        / "waves"
        / "prep"
        / "material-form-demand-census"
        / "census.json"
    )
    value = read_json(path)
    counts = value.get("counts", {})
    return {
        "path": path.relative_to(ROOT).as_posix(),
        "counts": counts,
        "capability_slug": value.get("capability_slug"),
    }


def identity_disposition_rows(
    capabilities: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    result = []
    for capability in sorted(capabilities.values(), key=lambda item: item["slug"]):
        for identity in capability.get("identity_disposition", []):
            result.append(
                {
                    "capability": capability["slug"],
                    "semantic_key": identity.get("semantic_key"),
                    "disposition": identity.get("disposition"),
                    "runtime_ids": identity.get("runtime_ids", []),
                    "blocker_ids": identity.get("blocker_ids", []),
                }
            )
    return result


def candidate_capabilities(map_name: str, capabilities: dict[str, dict[str, Any]]) -> list[str]:
    aliases = CAPABILITY_ALIASES.get(map_name)
    if aliases is not None:
        return [slug for slug in aliases if slug in capabilities]
    candidate = f"machines/{map_name.replace('_', '-')}"
    return [candidate] if candidate in capabilities else []


def capability_snapshot(
    slugs: Iterable[str],
    capabilities: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    result = []
    for slug in slugs:
        value = capabilities.get(slug)
        if value is None:
            continue
        result.append(
            {
                "slug": slug,
                "title": value.get("title"),
                "maturity": value.get("maturity"),
                "workflow": value.get("workflow"),
                "survival_access": value.get("survival_access"),
                "wave_slug": value.get("wave_slug"),
                "path": value.get("_path"),
            }
        )
    return result


# --------------------------------------------------------------------------
# Recipe-map rows
# --------------------------------------------------------------------------


def delivery_depth(
    row: dict[str, Any],
    registered: bool,
    cap_rows: list[dict[str, Any]],
    traced: int,
    other_evidence: int,
    overflow: int,
) -> str:
    source_rows = row.get("recipe_count", 0)
    if row.get("classification") == "out_of_scope":
        return "legacy_exclusion_pending"
    if source_rows == 0:
        return "empty_source"
    if traced >= source_rows and overflow == 0:
        return "full_replay"
    if traced > 0 or other_evidence > 0:
        return "bounded_subset"
    if registered:
        return "runtime_only"
    if cap_rows:
        return "identity_only"
    return "denominator_only"


def map_rows(
    recipe_rows: list[dict[str, Any]],
    capabilities: dict[str, dict[str, Any]],
    runtime_specs: set[str],
    traces: dict[str, MapTrace],
    untraced: dict[str, Counter[str]],
    cc_row_counts: dict[str, int],
    rule_files: dict[str, int],
    rule_expansions: dict[str, int],
    overflow_files: list[dict[str, Any]],
    blockers: list[dict[str, Any]],
    revision: str,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    # CC map -> the single GT6 map that owns its untraced rows and rules.
    cc_owner: dict[str, str] = {}
    by_name = {row.get("name_internal") or "": row for row in recipe_rows}
    primary: dict[str, str] = {}
    for row in recipe_rows:
        name = row.get("name_internal") or ""
        trace = traces.get(name)
        if trace is not None and trace.exact_hosts:
            primary[name] = trace.exact_hosts.most_common(1)[0][0]
        elif trace is not None and trace.reference_hosts:
            primary[name] = trace.reference_hosts.most_common(1)[0][0]
        else:
            primary[name] = map_key(name)
    for name in sorted(primary, key=lambda item: -len(traces.get(item, MapTrace(set(), set(), Counter(), Counter())).exact)):
        cc_owner.setdefault(primary[name], name)
    for cc_map, gt_map in CC_MAP_GT6_OWNERS.items():
        if gt_map in by_name:
            cc_owner.setdefault(cc_map, gt_map)

    owned_cc: dict[str, list[str]] = defaultdict(list)
    for cc_map, gt_map in cc_owner.items():
        owned_cc[gt_map].append(cc_map)

    result = []
    for row in recipe_rows:
        name = row.get("name_internal") or ""
        local_map = primary.get(name, map_key(name))
        caps = candidate_capabilities(local_map, capabilities)
        cap_rows = capability_snapshot(caps, capabilities)
        trace = traces.get(name) or MapTrace(set(), set(), Counter(), Counter())
        mine = sorted(owned_cc.get(name, []))
        untraced_rows = sum(sum(untraced.get(cc_map, Counter()).values()) for cc_map in mine)
        untraced_kinds: Counter[str] = Counter()
        for cc_map in mine:
            untraced_kinds.update(untraced.get(cc_map, Counter()))
        rules = sum(rule_files.get(cc_map, 0) for cc_map in mine)
        expanded = sum(rule_expansions.get(cc_map, 0) for cc_map in mine)
        owner_wave_dirs = [
            str(cap.get("wave_slug") or "").split("/")[-1]
            for cap in cap_rows
            if cap.get("wave_slug")
        ]
        overflow_rows, overflow_evidence = overflow_for_map(
            overflow_files, local_map, owner_wave_dirs
        )
        related = []
        gap_axes = set()
        for blocker in blockers:
            text = json.dumps(blocker, ensure_ascii=False).lower()
            if local_map in text or local_map.replace("_", "-") in text:
                related.append(blocker.get("id"))
                root_cause = str(blocker.get("root_cause_class") or "")
                if root_cause in {"unmapped_identity"}:
                    gap_axes.add("identity")
                elif root_cause in {"missing_form"}:
                    gap_axes.add("form")
                elif root_cause in {"missing_fluid"}:
                    gap_axes.add("fluid")
                elif root_cause in {"missing_obtain"}:
                    gap_axes.add("obtain")
                elif root_cause in {"unmapped_operand"}:
                    gap_axes.add("operand")
                elif root_cause in {"missing_runtime"}:
                    gap_axes.add("runtime")
                elif root_cause in {"missing_worldgen"}:
                    gap_axes.add("worldgen")
                elif root_cause:
                    gap_axes.add(root_cause)
        registered = local_map in runtime_specs or any(cc_map in runtime_specs for cc_map in mine)
        depth = delivery_depth(
            row,
            registered,
            cap_rows,
            len(trace.exact),
            len(trace.reference) + untraced_rows + rules,
            overflow_rows,
        )
        result.append(
            {
                "source_map": row.get("name_internal"),
                "normalized_map": row["normalized_row_key"],
                "local_map": local_map,
                "source_rows": row.get("recipe_count", 0),
                "denominator_class": row.get("classification"),
                "owner": row.get("owner"),
                "reason": row.get("reason"),
                "source_revision": row.get("source_revision"),
                "source_path": row.get("source_path"),
                "cc_recipe_map_registered": registered,
                "capabilities": cap_rows,
                "traced_source_rows": len(trace.exact),
                "reference_traced_rows": len(trace.reference),
                "cc_host_maps": dict(sorted((trace.exact_hosts + trace.reference_hosts).items())),
                "owned_cc_maps": mine,
                "untraced_cc_rows": untraced_rows,
                "untraced_cc_row_kinds": dict(sorted(untraced_kinds.items())),
                "rule_files": rules,
                "rule_expanded_recipes": expanded,
                "overflow_rows": overflow_rows,
                "overflow_evidence": overflow_evidence,
                "overflow_selection_policy": (
                    "only waves whose directory names this map; shared multi-map "
                    "waves are listed as evidence but not counted; never summed"
                ),
                "related_blockers": sorted(set(filter(None, related))),
                "gap_axes": sorted(gap_axes),
                "player_path": {
                    "survival_access": sorted(
                        {
                            cap.get("survival_access")
                            for cap in cap_rows
                            if cap.get("survival_access")
                        }
                    ),
                    "signoff_required": True,
                },
                "delivery_depth": depth,
                "freshness": {
                    "source_revision_matches_scope": row.get("source_revision") == revision,
                    "evidence_class": "current_worktree_scan",
                },
            }
        )

    unowned = []
    for cc_map in sorted(set(cc_row_counts) | set(rule_files) | set(rule_expansions)):
        if cc_map in cc_owner:
            continue
        rows_on_map = cc_row_counts.get(cc_map, 0)
        traced_elsewhere = sum(
            trace.exact_hosts.get(cc_map, 0) + trace.reference_hosts.get(cc_map, 0)
            for trace in traces.values()
        )
        unowned.append(
            {
                "cc_map": cc_map,
                "registered": cc_map in runtime_specs,
                "cc_rows": rows_on_map,
                "rows_traced_to_gt6": traced_elsewhere,
                "untraced_rows": sum(untraced.get(cc_map, Counter()).values()),
                "rule_files": rule_files.get(cc_map, 0),
                "rule_expanded_recipes": rule_expansions.get(cc_map, 0),
            }
        )
    return result, unowned


def build_report(*, refresh_attribution: bool) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    scope = read_json(SCOPE_PATH)
    observed_revision = local_source_revision()
    expected_source_revision = scope["source"]["revision"]
    if observed_revision is not None and observed_revision != expected_source_revision:
        raise ValueError(
            "local GT6 source revision differs from frozen scope: "
            f"{observed_revision} != {expected_source_revision}"
        )
    # gt6_code/ is gitignored. A clean checkout cannot read that HEAD, so the
    # report keeps the pinned scope revision instead of null.
    actual_source_revision = observed_revision or expected_source_revision
    capabilities = load_capabilities()
    semantic_coverage = load_semantic_coverage()
    evidence_index = EvidenceIndex(capabilities)
    live_cover_definitions = load_live_cover_definitions()
    blockers = load_blockers()
    recipe_rows = load_recipe_maps()
    runtime_specs = load_recipe_map_constants()
    cc_rows, rule_files = scan_cc_recipes()
    owners = name_owners(recipe_rows)
    attribution = resolve_attribution(cc_rows, owners, refresh=refresh_attribution)
    traces, untraced, cc_row_counts = attribute_rows(cc_rows, owners, attribution)
    overflow_files = scan_overflow_files()
    maps, unowned_cc_maps = map_rows(
        recipe_rows,
        capabilities,
        runtime_specs,
        traces,
        untraced,
        cc_row_counts,
        rule_files,
        load_rule_expansion_counts(),
        overflow_files,
        blockers,
        expected_source_revision,
    )
    denominator_summaries = load_denominator_summaries()
    capability_counts = Counter(
        f"{value.get('maturity')}:{value.get('workflow')}"
        for value in capabilities.values()
    )
    blocker_counts = Counter(value.get("status") for value in blockers)
    chemistry_maps = [
        row
        for row in maps
        if "CHEMICAL_OR_THERMAL_PIPELINE" in (row.get("reason") or "")
        or row["local_map"] in CHEMICAL_THERMAL_NAMES
        or map_key(row.get("source_map") or "") in CHEMICAL_THERMAL_NAMES
    ]
    chemistry_pipeline_deferred = [
        row
        for row in chemistry_maps
        if "CHEMICAL_OR_THERMAL_PIPELINE" in (row.get("reason") or "")
    ]
    for row in maps:
        row["raw_row_classes"] = dict(
            (semantic_coverage.get("maps") or {}).get(row["source_map"] or "", {})
        )
        row["raw_row_excluded"] = dict(
            (semantic_coverage.get("maps_excluded") or {}).get(row["source_map"] or "", {})
        )
        row["progress"] = map_progress(row)
    recipe_evidence_grades = raw_row_grades(maps)
    report = {
        "schema_version": 2,
        "assessment_id": scope["assessment_id"],
        "status": "CURRENT_WORKTREE_RECONCILIATION",
        "scope": scope,
        "source_check": {
            "local_revision": actual_source_revision,
            "expected_revision": expected_source_revision,
            "matches": actual_source_revision in (None, expected_source_revision),
        },
        "denominator_tables": denominator_summaries,
        "summary": {
            "recipe_map_count": len(recipe_rows),
            "recipe_source_rows": sum(row.get("recipe_count", 0) for row in recipe_rows),
            "traced_source_rows": sum(row["traced_source_rows"] for row in maps),
            "reference_traced_rows": sum(row["reference_traced_rows"] for row in maps),
            "recipe_evidence_grades": recipe_evidence_grades,
            "cc_recipe_rows": sum(cc_row_counts.values()),
            "cc_material_rule_files": sum(rule_files.values()),
            "capability_count": len(capabilities),
            "capability_states": dict(sorted(capability_counts.items())),
            "blocker_count": len(blockers),
            "blocker_states": dict(sorted(blocker_counts.items())),
            "registered_recipe_maps": len(runtime_specs),
            "cc_maps_with_rows": len(cc_row_counts),
            "overflow_artifacts": len(overflow_files),
            "live_cover_definitions": len(live_cover_definitions),
        },
        "recipe_scan": {
            "roots": [path.relative_to(ROOT).as_posix() for path in runtime_resource_roots()],
            "gradle_excludes": gradle_resource_excludes(),
            "row_kinds": [GT_RECIPE, COMPACT_FAMILY, MATERIAL_RULE],
            "attribution": ATTRIBUTION_PATH.relative_to(ROOT).as_posix(),
            "attribution_digest": attribution.get("evidence_digest"),
            "attribution_overrides": len(attribution.get("full_overrides") or {})
            + len(attribution.get("short_overrides") or {}),
            "unverified_hashes": len(attribution.get("unverified_full_hashes") or [])
            + len(attribution.get("unverified_short_hashes") or []),
            "ambiguous_hashes": len(attribution.get("ambiguous_hashes") or []),
        },
        "capabilities": [
            {
                "slug": value.get("slug"),
                "title": value.get("title"),
                "maturity": value.get("maturity"),
                "workflow": value.get("workflow"),
                "survival_access": value.get("survival_access"),
                "wave_slug": value.get("wave_slug"),
                "path": value.get("_path"),
            }
            for value in sorted(capabilities.values(), key=lambda item: item["slug"])
        ],
        "blockers": blocker_rows(blockers),
        "identity_dispositions": identity_disposition_rows(capabilities),
        "form_demand_census": load_form_census(),
        "recipe_maps": maps,
        "recipe_evidence_grades": recipe_evidence_grades,
        "semantic_coverage": semantic_coverage,
        "unowned_cc_maps": unowned_cc_maps,
        "overflow_artifacts": overflow_files,
        "machine_kinds": attach_machine_kind_evidence(
            load_machine_kind_summary(),
            maps,
            evidence_index,
        ),
        "multiblock_kinds": load_multiblock_summary(capabilities, evidence_index),
        "cover_kinds": load_cover_summary(live_cover_definitions),
        "live_cover_definitions": live_cover_definitions,
        "energy_identities": load_energy_summary(),
        "live_energy_types": live_energy_types(),
        "itemgenerator_domains": load_itemgenerator_summary(),
        "prefixes": load_prefix_summary(),
        "mte_identity_dispositions": load_mte_disposition_summary(),
    }
    chemistry = {
        "schema_version": 2,
        "assessment_id": scope["assessment_id"],
        "status": "CURRENT_WORKTREE_RECONCILIATION",
        "source_revision": scope["source"]["revision"],
        "map_count": len(chemistry_maps),
        "source_rows": sum(row["source_rows"] for row in chemistry_maps),
        "traced_source_rows": sum(row["traced_source_rows"] for row in chemistry_maps),
        "recipe_evidence_grades": raw_row_grades(chemistry_maps),
        "pipeline_deferred_map_count": len(chemistry_pipeline_deferred),
        "pipeline_deferred_source_rows": sum(
            row["source_rows"] for row in chemistry_pipeline_deferred
        ),
        "delivery_depth_counts": dict(
            sorted(Counter(row["delivery_depth"] for row in chemistry_maps).items())
        ),
        "maps": chemistry_maps,
    }
    return report, chemistry, attribution


# --------------------------------------------------------------------------
# Markdown
# --------------------------------------------------------------------------


DEPTH_ORDER = (
    "denominator_only",
    "identity_only",
    "runtime_code_uncarded",
    "runtime_only",
    "runtime_paused",
    "bounded_subset",
    "runtime_accepted",
    "full_replay",
    "empty_source",
    "legacy_exclusion_pending",
)

# Row classes come from semantic_coverage.json; groups are what the overview shows.
RAW_ROW_CLASSES = (
    "source_exact",
    "translated_exact",
    "translated_io_only",
    "translated_item_io",
    "translatable_missing",
    "missing_material_form",
    "missing_material",
    "missing_fluid",
    "missing_object",
    "display_only",
    "legacy_exclusion_pending",
)
IDENTITY_GAP_CLASSES = ("missing_material_form", "missing_material", "missing_fluid", "missing_object")
RAW_ROW_GROUPS = (
    ("proven", "已证明", ("source_exact", "translated_exact")),
    ("partial_match", "部分一致", ("translated_io_only", "translated_item_io")),
    ("recipe_gap", "缺配方", ("translatable_missing",)),
    ("identity_gap", "缺身份", IDENTITY_GAP_CLASSES),
    ("display_only", "展示用", ("display_only",)),
    ("legacy_exclusion_pending", "旧排除待决策", ("legacy_exclusion_pending",)),
)
RAW_ROW_CLASS_LABELS = {
    "source_exact": "hash 逐行证明",
    "translated_exact": "翻译后完全一致",
    "translated_io_only": "输入输出一致，时间/功率不同",
    "translated_item_io": "物品一致，流体不同",
    "translatable_missing": "可翻译但 CC 无此配方",
    "missing_material_form": "CC 有这个材料，但缺这个形态",
    "missing_material": "CC 没有这个材料",
    "missing_fluid": "CC 缺流体",
    "missing_object": "CC 缺物品/方块/模具",
    "display_only": "GT6 NEI 展示行（fake/hidden）",
    "legacy_exclusion_pending": "旧分母排除，待重新决策",
}


def _cell(value: Any) -> str:
    if value is None or value == "" or value == []:
        return "—"
    if isinstance(value, list):
        return ", ".join(str(item) for item in value)
    return str(value).replace("|", "\\|")


def _counter_line(values: Iterable[str]) -> str:
    counts = Counter(values)
    ordered = [key for key in DEPTH_ORDER if key in counts]
    ordered.extend(sorted(key for key in counts if key not in DEPTH_ORDER))
    return "，".join(f"`{key}` {counts[key]}" for key in ordered)


def _depth_sort(row: dict[str, Any]) -> tuple[int, str]:
    depth = row.get("delivery_depth")
    index = DEPTH_ORDER.index(depth) if depth in DEPTH_ORDER else len(DEPTH_ORDER)
    return index, str(row.get("source_map") or row.get("canonical_id") or "")


def _percent(part: int, whole: int) -> str:
    if not whole:
        return "—"
    value = 100.0 * part / whole
    if 0 < value < 0.1:
        return "<0.1%"
    return f"{value:.1f}%"


def raw_row_grades(rows: list[dict[str, Any]]) -> dict[str, Any]:
    """Aggregate the per-row classes of semantic_coverage.json over some maps.

    Every GT6 source row lands in exactly one class, so the classes (and the
    groups built from them) partition the source denominator.
    """
    total = sum(int(row.get("source_rows") or 0) for row in rows)
    classes: Counter[str] = Counter()
    excluded: Counter[str] = Counter()
    for row in rows:
        classes.update(row.get("raw_row_classes") or {})
        excluded.update(row.get("raw_row_excluded") or {})
    unknown = (set(classes) | set(excluded)) - set(RAW_ROW_CLASSES)
    if unknown:
        raise ValueError(f"unknown raw-row classes: {sorted(unknown)}")
    if sum(classes.values()) != total:
        raise ValueError(
            "raw-row classes do not partition the source denominator: "
            f"{sum(classes.values())} != {total}; rerun build_semantic_coverage.py --write"
        )
    progress = _progress_from(classes, excluded)
    maps_with_target = [row["progress"] for row in rows if row.get("progress", {}).get("target_rows")]
    return {
        "source_rows": total,
        "classes": {name: classes[name] for name in RAW_ROW_CLASSES},
        "groups": {
            key: sum(classes[name] for name in members)
            for key, _label, members in RAW_ROW_GROUPS
        },
        "excluded": {name: excluded[name] for name in RAW_ROW_CLASSES if excluded[name]},
        "progress": progress,
        "machine_progress": {
            "maps": len(maps_with_target),
            "mean_recipe_progress": round(
                sum(item["recipe_progress"] for item in maps_with_target) / len(maps_with_target), 4
            )
            if maps_with_target
            else None,
            "maps_complete": sum(1 for item in maps_with_target if item["recipe_progress"] >= 1),
            "maps_untouched": sum(1 for item in maps_with_target if item["proven_rows"] == 0),
        },
    }


def _progress_from(classes: Counter[str] | dict[str, int], excluded: Counter[str] | dict[str, int]) -> dict[str, Any]:
    total = sum(classes.values())
    excluded_total = sum(excluded.values())
    target = total - excluded_total
    proven = sum(classes.get(name, 0) for name in ("source_exact", "translated_exact"))
    identity = sum(
        classes.get(name, 0) - excluded.get(name, 0) for name in IDENTITY_GAP_CLASSES
    )
    legacy = classes.get("legacy_exclusion_pending", 0) - excluded.get("legacy_exclusion_pending", 0)
    return {
        "source_rows": total,
        "excluded_rows": excluded_total,
        "target_rows": target,
        "proven_rows": proven,
        "identity_blocked_rows": identity,
        "undecided_rows": legacy,
        "recipe_progress": round(proven / target, 4) if target else None,
        "identity_ready": round((target - identity - legacy) / target, 4) if target else None,
    }


def map_progress(row: dict[str, Any]) -> dict[str, Any]:
    return _progress_from(row.get("raw_row_classes") or {}, row.get("raw_row_excluded") or {})


def _traced_cell(row: dict[str, Any]) -> str:
    traced = row["traced_source_rows"]
    if not traced:
        return "0"
    return f"{traced}（{_percent(traced, row['source_rows'])}）"


def _share(value: int, total: int) -> str:
    return f"{value}（{_percent(value, total)}）"


def _hosts_cell(row: dict[str, Any]) -> str:
    hosts = row.get("cc_host_maps") or {}
    if not hosts:
        return "—"
    ordered = sorted(hosts.items(), key=lambda item: (-item[1], item[0]))
    return " · ".join(f"{name} {count}" for name, count in ordered)


def _rules_cell(row: dict[str, Any]) -> str:
    files = row.get("rule_files") or 0
    expanded = row.get("rule_expanded_recipes") or 0
    if not files and not expanded:
        return "—"
    if expanded:
        return f"{files} 条 → {expanded}"
    return f"{files} 条"


def _render_recipe_map_table(rows: list[dict[str, Any]]) -> list[str]:
    lines = [
        "| GT6 map | 源行 | 历史源分母分类 | 当前交付深度 | 逐行已证明 | reference 追溯 | CC 承载图（CC 行） | CC 未追溯行 | 材料规则 | overflow | capability | blocker |",
        "| --- | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- | --- |",
    ]
    for row in sorted(rows, key=_depth_sort):
        lines.append(
            "| `{map}` | {rows} | {cls} | `{depth}` | {traced} | {ref} | {hosts} | {untraced} | {rules} | {over} | {caps} | {blk} |".format(
                map=row["source_map"] or "(unnamed)",
                rows=row["source_rows"],
                cls=row["denominator_class"],
                depth=row["delivery_depth"],
                traced=_traced_cell(row),
                ref=row["reference_traced_rows"] or 0,
                hosts=_hosts_cell(row),
                untraced=row["untraced_cc_rows"] or 0,
                rules=_rules_cell(row),
                over=row["overflow_rows"],
                caps=_cell([cap["slug"] for cap in row["capabilities"]]),
                blk=_cell(row["related_blockers"]),
            )
        )
    return lines


def render_markdown(report: dict[str, Any], chemistry: dict[str, Any]) -> str:
    summary = report["summary"]
    scan = report["recipe_scan"]
    maps = report["recipe_maps"]
    kinds = report["machine_kinds"]
    multiblocks = report["multiblock_kinds"]
    covers = report["cover_kinds"]
    energy = report["energy_identities"]
    domains = report["itemgenerator_domains"]
    prefixes = report["prefixes"]
    mte = report["mte_identity_dispositions"]
    census = report["form_demand_census"]["counts"]
    capabilities = report["capabilities"]
    blockers = report["blockers"]
    semantic = report["semantic_coverage"]
    calibration = semantic["calibration"]
    recipe_grades = summary["recipe_evidence_grades"]
    grade_total = recipe_grades["source_rows"]
    grade_classes = recipe_grades["classes"]
    grade_groups = recipe_grades["groups"]
    progress = recipe_grades["progress"]
    machine_progress = recipe_grades["machine_progress"]

    def pct(value: float | None) -> str:
        return "—" if value is None else f"{100 * value:.1f}%"

    open_blockers = [row for row in blockers if row["status"] in {"open", "partial"}]
    source_rows_by_depth: Counter[str] = Counter()
    traced_by_depth: Counter[str] = Counter()
    for row in maps:
        source_rows_by_depth[row["delivery_depth"]] += row["source_rows"]
        traced_by_depth[row["delivery_depth"]] += row["traced_source_rows"]
    covers_live = sum(1 for row in covers if row["live_definitions"])
    live_prefixes = sum(1 for row in prefixes["rows"] if row["cc_mapped"])
    frozen_prefixes = sum(1 for row in prefixes["rows"] if row["frozen_cc_mapped"])
    live_energy = sum(1 for row in energy if row["live_energy_type"])

    lines = [
        "# GT6 全量覆盖重评估",
        "",
        "> 本页由 `tools/waves/portfolio/gt6-full-coverage-reassessment/build_reconciliation.py`",
        "> 从本地 GT6 分母、当前 capability、blocker、wave overflow 和工作树配方生成，不要手改。",
        f"> 何时重跑、各列口径和 CI 检测见 [`{Path(WORKFLOW_DOC).name}`]({Path(WORKFLOW_DOC).name})。",
        "> 各节单位不同（配方行、机器 kind、多方块、前缀、身份、blocker），**不得相加**，",
        "> 也不把 `runtime_ready` 当作完整 GT6 覆盖。",
        "",
        f"GT6 source revision：`{report['scope']['source']['revision']}`。",
        "",
        "## 1. 总览（按轴）",
        "",
        "| 轴 | 分母 | 当前状态分布 |",
        "| --- | --- | --- |",
        f"| 配方图 | {summary['recipe_map_count']} maps / {summary['recipe_source_rows']} 源行 | "
        f"{_counter_line(row['delivery_depth'] for row in maps)} |",
        f"| **配方移植进度** | 目标 {progress['target_rows']} 源行"
        f"（{grade_total} − 决策排除 {progress['excluded_rows']}） | "
        f"**已证明 {pct(progress['recipe_progress'])}**（{progress['proven_rows']}）；"
        f"身份就绪 {pct(progress['identity_ready'])}；"
        f"按机器平均 {pct(machine_progress['mean_recipe_progress'])}"
        f"（{machine_progress['maps']} 台，完成 {machine_progress['maps_complete']}，"
        f"未开始 {machine_progress['maps_untouched']}） |",
        f"| 配方源行逐行分类 | {grade_total} 源行 | "
        + "；".join(
            f"{label} {_share(grade_groups[key], grade_total)}"
            for key, label, _members in RAW_ROW_GROUPS
        )
        + "（互斥，合计等于分母） |",
        f"| 机器 kind | {len(kinds)} canonical kinds | "
        f"{_counter_line(row['delivery_depth'] for row in kinds)} |",
        f"| 多方块控制器 | {len(multiblocks)} canonical kinds | "
        f"{_counter_line(row['delivery_depth'] for row in multiblocks)} |",
        f"| 盖板 | {len(covers)} canonical kinds | 有 CC live id {covers_live}，无 {len(covers) - covers_live} |",
        f"| 能量身份 | {len(energy)} identities | 有 CC `EnergyType` {live_energy}，无 {len(energy) - live_energy} |",
        f"| 物品/流体生成域 | {len(domains)} domains | "
        f"{_counter_line(row['classification'] for row in domains)}（冻结分母） |",
        f"| 材料前缀 | {len(prefixes['rows'])} canonical prefixes | "
        f"{_counter_line(row['classification'] for row in prefixes['rows'])}；"
        f"CC live 已映射 {live_prefixes}（冻结分母记 {frozen_prefixes}） |",
        f"| MTE 身份 | {sum(mte['counts'].values()) if mte['counts'] else '—'} identities | "
        + "，".join(f"`{k}` {v}" for k, v in mte["live_counts"].items())
        + " |",
        f"| 材料形态需求 | {census.get('demand_pairs', '—')} demand pairs | "
        f"openable {census.get('openable', '—')}，gated_unresolved "
        f"{census.get('gated_unresolved', '—')}，ungated 规模 "
        f"{census.get('ungated_generated_flag_pairs', '—')}（规模，非待办） |",
        f"| Capability | {len(capabilities)} | "
        + "，".join(f"`{k}` {v}" for k, v in summary["capability_states"].items())
        + "；survival_access "
        + _counter_line(row.get("survival_access") or "unset" for row in capabilities)
        + " |",
        f"| Blocker | {len(blockers)} | "
        + "，".join(f"`{k}` {v}" for k, v in summary["blocker_states"].items())
        + " |",
        "",
        "配方源行逐行分类（每一条 GT6 源行只落一类，合计等于分母；口径见工作流文档第 3.3 节）：",
        "",
    ]
    for key, label, members in RAW_ROW_GROUPS:
        detail = "，".join(
            f"`{name}` {grade_classes[name]}"
            for name in members
        )
        lines.append(f"- **{label}** {_share(grade_groups[key], grade_total)}：{detail}")
    agreement = calibration.get("agreement")
    lines.extend(
        [
            "",
            f"翻译链校准：在 {calibration['pairs']} 对 hash 已证明的“CC 行 ↔ GT6 源行”上，"
            f"翻译后完全一致 {calibration['equal']}"
            f"（{'—' if agreement is None else f'{100 * agreement:.1f}%'}），"
            f"不一致 {calibration['differs']}，不可翻译 {calibration['untranslatable']}。"
            "不一致的是真实移植差异（例如缺电路编号、有意替换），样例见 `semantic_coverage.json`。",
            "",
            "按交付深度的源行数：",
            "",
        ]
    )
    for depth in DEPTH_ORDER:
        if depth in source_rows_by_depth:
            lines.append(
                f"- `{depth}`：{source_rows_by_depth[depth]} 源行，已追溯 {traced_by_depth[depth]}"
            )
    lines.extend(
        [
            "",
            f"## 2. 全部 GT6 配方图（{len(maps)}）",
            "",
            "列口径（详见工作流文档第 3 节）：",
            "",
            "- **逐行已证明**：CC 运行时配方行上的 evidence hash 对上 GT6 dump 行后，按不同源行去重计数，"
            "与“源行”同单位，括号是占比。`full_replay` 只看这一列是否等于源行数；"
            "逐行分类见第 2.2 节。",
            "- **reference 追溯**：ore-chain 这类按材料族投影的配方只能追到 "
            "`gt6_recipe_normalized_reference.json` 的归一化行；归一化会合并多条 dump 行，所以单列、不相加、不判 `full_replay`。",
            "- **CC 承载图**：这些源行落在哪些 CC RecipeMap 上，数字是 CC 配方行数；"
            "一条 GT6 行可展开成多条 CC 行（OreDict 备选），所以可以大于已追溯源行。GT6 把很多相同的行同时注册进几张图"
            "（melter/smelter、compressor/rollingmill、mortar/shredder），归属以配方自己声明的源图为准。",
            "- **CC 未追溯行**：落在本图对应 CC 图上、但没有行级 GT6 evidence 的配方行"
            "（datagen 手写、`gt6_java_source`、bootstrap、design policy 等），只证明有内容，不算源行。",
            "- **材料规则**：`material_rule` 文件数 → `component_rule_manifest.json` 记录的离线展开数；"
            "没有展开数的规则由运行时按材料展开。",
            "",
            f"扫描范围：{len(scan['roots'])} 个运行时资源根（含 `src/generated/resources`），"
            f"按 `source-sets.gradle` 排除 {len(scan['gradle_excludes'])} 个 pattern；"
            f"CC 配方行 {summary['cc_recipe_rows']}，材料规则文件 {summary['cc_material_rule_files']}。"
            f"源行归属钉在 `{scan['attribution']}`（覆盖 {scan['attribution_overrides']}，"
            f"未能在 dump 中找到 {scan['unverified_hashes']}，多图歧义 {scan['ambiguous_hashes']}）。",
            "",
        ]
    )
    lines.extend(_render_recipe_map_table(maps))

    unowned = report["unowned_cc_maps"]
    lines.extend(
        [
            "",
            "### 2.1 没有归属 GT6 图的 CC 配方图",
            "",
            "这些 CC RecipeMap 上有配方行或材料规则，但不对应任何 GT6 分母行，上表不计。"
            "“已追溯到 GT6”表示其中的行已经算进上表别的 GT6 图。",
            "",
        ]
    )
    if unowned:
        lines.extend(
            [
                "| CC map | 已注册 | CC 行 | 已追溯到 GT6 | 未追溯 | 材料规则 |",
                "| --- | --- | ---: | ---: | ---: | ---: |",
            ]
        )
        for row in unowned:
            rules = row["rule_files"]
            expanded = row["rule_expanded_recipes"]
            rules_cell = f"{rules} 条 → {expanded}" if expanded else (f"{rules} 条" if rules else "—")
            lines.append(
                f"| `{row['cc_map']}` | {'是' if row['registered'] else '否'} | {row['cc_rows']} | "
                f"{row['rows_traced_to_gt6']} | {row['untraced_rows']} | {rules_cell} |"
            )
    else:
        lines.append("（无）")

    lines.extend(
        [
            "",
            "### 2.2 逐行分类（每张 GT6 图）",
            "",
            "每条 GT6 源行都用 recipe wave 的翻译链（`dialects/gt6.compile_row` + `emit`）翻成 CC 身份，"
            "再和该图对应 CC RecipeMap 上的全部运行时配方行、材料规则展开逐条比较。"
            "类别含义：",
            "",
        ]
    )
    for name in RAW_ROW_CLASSES:
        lines.append(f"- `{name}`：{RAW_ROW_CLASS_LABELS[name]}")
    header_names = [name for name in RAW_ROW_CLASSES]
    lines.extend(
        [
            "",
            "“目标”= 源行 − 决策排除；“进度”= 已证明 / 目标；“就绪”= 不缺身份、不待决策的目标行占比。",
            "",
            "| GT6 map | 源行 | 目标 | 进度 | 就绪 | "
            + " | ".join(f"`{name}`" for name in header_names)
            + " |",
            "| --- | ---: | ---: | ---: | ---: | " + " | ".join("---:" for _ in header_names) + " |",
        ]
    )
    for row in sorted(maps, key=lambda item: -int(item["source_rows"] or 0)):
        if not row["source_rows"]:
            continue
        classes = row.get("raw_row_classes") or {}
        row_progress = row.get("progress") or {}
        lines.append(
            f"| `{row['source_map']}` | {row['source_rows']} | {row_progress.get('target_rows', 0)} | "
            f"{pct(row_progress.get('recipe_progress'))} | {pct(row_progress.get('identity_ready'))} | "
            + " | ".join(str(classes.get(name, 0)) for name in header_names)
            + " |"
        )

    lines.extend(
        [
            "",
            "## 3. 化学 / 热处理子集",
            "",
            f"- 相关 maps：{chemistry['map_count']}，GT6 源行 {chemistry['source_rows']}，"
            f"已追溯 {chemistry['traced_source_rows']}",
            f"- 明确 `CHEMICAL_OR_THERMAL_PIPELINE` deferred："
            f"{chemistry['pipeline_deferred_map_count']} maps / "
            f"{chemistry['pipeline_deferred_source_rows']} 源行",
            "- 逐图明细见第 2 节同名行；这里不重复。",
            "",
            f"## 4. 机器 kind（{len(kinds)}）",
            "",
            "有 RecipeMap 的 kind 取对应配方图的深度；GT6 dump 为空的图（原版熔炉、NBT 假配方、合成台）"
            "不代表机器不用做，按 CC 主机判：有主机 → `runtime_only`，没有 → `denominator_only`。"
            "无 RecipeMap 的 kind（发电、转换、储能、传动等）"
            "按 CC 侧证据判定：capability 已 accepted → `runtime_accepted`；只有 paused/frozen 卡 → "
            "`runtime_paused`；有运行时代码但没建卡 → `runtime_code_uncarded`；只在 CC 目录里有身份 → "
            "`identity_only`；都没有 → `denominator_only`。",
            "",
            "| 行为类 | RecipeMap / FuelMap | 能量 in→out | 历史源分母分类 | GT6 source ids | 变体 | CC 主机 / 证据 | 当前交付深度 |",
            "| --- | --- | --- | --- | ---: | ---: | --- | --- |",
        ]
    )
    for row in sorted(kinds, key=lambda item: (_depth_sort(item)[0], str(item.get("behavior_class")), str(item.get("recipe_map")))):
        process = row.get("recipe_map") if row.get("recipe_map") not in (None, "NONE") else row.get("fuel_map")
        evidence = row.get("evidence")
        if evidence is not None:
            if evidence["capabilities"]:
                cc_side = [
                    f"{cap['slug']}（{cap['maturity']}/{cap['workflow']}）"
                    for cap in evidence["capabilities"]
                ]
            elif evidence["java_file_count"]:
                cc_side = [f"代码 {evidence['java_file_count']} 个文件，无 capability"]
            elif evidence["data_file_count"]:
                cc_side = [f"目录 {evidence['data_file_count']} 个文件，无运行时"]
            else:
                cc_side = []
        else:
            cc_side = row.get("cc_hosts") or []
        lines.append(
            "| `{cls}` | {proc} | {ein}→{eout} | {c} | {ids} | {var} | {hosts} | `{depth}` |".format(
                cls=row.get("behavior_class"),
                proc=_cell(process if process not in (None, "NONE") else None),
                ein=row.get("accepted_energy"),
                eout=row.get("emitted_energy"),
                c=row.get("classification"),
                ids=len(row.get("source_ids") or []),
                var=row.get("variant_count"),
                hosts=_cell(cc_side),
                depth=row.get("delivery_depth"),
            )
        )

    lines.extend(
        [
            "",
            f"## 5. 多方块控制器（{len(multiblocks)}）",
            "",
            "按分母里的原始 GT6 控制器类名在 CC capability、运行时代码和数据目录里查证据。",
            "",
            "| GT6 控制器 | 分母 | CC 证据 | 交付深度 |",
            "| --- | --- | --- | --- |",
        ]
    )
    for row in sorted(multiblocks, key=_depth_sort):
        evidence = row["evidence"]
        cc_side = [
            f"{cap['slug']}（{cap['maturity']}/{cap['workflow']}，survival={cap['survival_access'] or 'unset'}）"
            for cap in row["capabilities"]
        ]
        if not cc_side and evidence["java_file_count"]:
            cc_side = [f"代码 {evidence['java_file_count']} 个文件，无 capability"]
        elif not cc_side and evidence["data_file_count"]:
            cc_side = [f"目录 {evidence['data_file_count']} 个文件，无运行时"]
        lines.append(
            f"| `{row['canonical_id']}` | {row['disposition']} | "
            f"{_cell(cc_side)} | `{row['delivery_depth']}` |"
        )

    lines.extend(
        [
            "",
            f"## 6. 盖板（{len(covers)}）",
            "",
            f"当前 CC 盖板定义：{summary['live_cover_definitions']} 条。"
            "“历史源分母分类”来自冻结基线，不是当前实现状态；"
            "“CC live ids”才是当前工作树证据。",
            "",
            "| GT6 盖板 | 历史源分母分类 | CC live ids |",
            "| --- | --- | --- |",
        ]
    )
    for row in sorted(covers, key=lambda item: (not item["live_definitions"], str(item["disposition"]), str(item["canonical_id"]))):
        lines.append(
            f"| `{row['canonical_id']}` | {row['disposition']} | "
            f"{_cell([item['id'] for item in row['live_definitions']])} |"
        )

    lines.extend(
        [
            "",
            f"## 7. 能量身份（{len(energy)}）",
            "",
            "“CC `EnergyType`”读自 live `EnergyType.java`；冻结分母里的本地类型只作对照。",
            "",
            "| GT6 能量 | 名称 | 分母 | CC `EnergyType` | 冻结分母记录 |",
            "| --- | --- | --- | --- | --- |",
        ]
    )
    for row in sorted(energy, key=lambda item: (str(item["classification"]), str(item["symbol"]))):
        lines.append(
            f"| `{row['symbol']}` | {_cell(row['long_name'])} | {row['classification']} | "
            f"{_cell(row['live_energy_type'])} | {_cell(row['frozen_local_energy_type'])} |"
        )

    lines.extend(
        [
            "",
            f"## 8. 物品/流体生成域（{len(domains)}）",
            "",
            "本节整列来自冻结分母 `itemgenerator_domains.json`，不会随工作树变化；"
            "具体形态是否开放看第 9 节 live 前缀和第 11 节形态需求普查。",
            "",
            "| GT6 域 | 分母 | 冻结分母记 CC 域 | 源材料数 |",
            "| --- | --- | --- | ---: |",
        ]
    )
    for row in sorted(domains, key=lambda item: (str(item["classification"]), str(item["canonical_id"]))):
        lines.append(
            f"| `{row['canonical_id']}` | {row['classification']} | "
            f"{_cell(row['cc_current_domain'])} | {_cell(row['source_material_count'])} |"
        )

    prefix_groups: dict[str, list[str]] = {}
    for row in prefixes["rows"]:
        key = row["classification"]
        suffix = "CC 已映射" if row["cc_mapped"] else "CC 未映射"
        prefix_groups.setdefault(f"{key}（{suffix}）", []).append(str(row["canonical_id"]))
    lines.extend(
        [
            "",
            f"## 9. 材料前缀（{len(prefixes['rows'])}）",
            "",
            "“CC 已映射”由 `tools/gt6_resolve.py` 的 `resolve_prefix` 对 live 前缀 JSON 现算。"
            "前缀分母只说明形态类型是否在范围内；具体 `(材料, 前缀)` 是否开放看第 11 节形态需求普查。",
            "",
        ]
    )
    for key in sorted(prefix_groups):
        names = sorted(prefix_groups[key])
        lines.append(f"- **{key}**（{len(names)}）：" + "、".join(f"`{n}`" for n in names))

    lines.extend(
        [
            "",
            "## 10. MTE 身份处置（按家族）",
            "",
            "“R0 账本”是冻结快照；“live”在账本上叠加 `mte_inplace_catalog.json`："
            "目录里有 `MteInPlaceKind` 宿主的身份记为 `inplace_runtime`。"
            "`inplace_runtime` 只说明有运行时宿主，不说明数值与获得格已核对。",
            "",
            "| 家族 | R0 账本 | live |",
            "| --- | --- | --- |",
        ]
    )
    for family, counts in mte["by_family"].items():
        live = mte["live_by_family"].get(family, {})
        lines.append(
            f"| `{family}` | "
            + "，".join(f"`{k}` {v}" for k, v in counts.items())
            + " | "
            + "，".join(f"`{k}` {v}" for k, v in live.items())
            + " |"
        )

    lines.extend(
        [
            "",
            "## 11. 材料形态需求普查",
            "",
            f"- 来源：`{report['form_demand_census']['path']}`",
        ]
    )
    for key, value in sorted(census.items()):
        lines.append(f"- `{key}`：{value}")

    lines.extend(
        [
            "",
            f"## 12. Capability（{len(capabilities)}）",
            "",
            "| capability | maturity | workflow | survival_access |",
            "| --- | --- | --- | --- |",
        ]
    )
    for row in capabilities:
        lines.append(
            f"| `{row['slug']}` | {row['maturity']} | {row['workflow']} | "
            f"{_cell(row.get('survival_access'))} |"
        )

    lines.extend(
        [
            "",
            f"## 13. 未关闭 blocker（{len(open_blockers)}）",
            "",
            "| blocker | 状态 | 排期桶 | 规模 | 根因 | 标题 |",
            "| --- | --- | --- | --- | --- | --- |",
        ]
    )
    for row in sorted(open_blockers, key=lambda item: (str(item["planning_bucket"]), str(item["id"]))):
        amount = "n/a" if row["count"] is None else f"{row['count']} {row['unit'] or ''}".strip()
        lines.append(
            f"| `{row['id']}` | {row['status']} | {row['planning_bucket']} | {amount} | "
            f"{row['root_cause_class']} | {_cell(row['title'])} |"
        )

    excluded_prefixes = [row for row in prefixes["rows"] if row["classification"] == "out_of_scope"]
    unused_prefixes = [row for row in excluded_prefixes if row["explicit_unused"]]
    lines.extend(
        [
            "",
            "## 14. 旧分母排除项（全量目标下需逐项重新决策）",
            "",
            "这些是旧冻结分母当年标的 `out_of_scope`（“third-stage excluded axis”）。本页以完整 GT6 为目标，"
            "它们不能默认算作“不用做”，需要逐项决定：真正移植、明确作为设计排除，或确认 GT6 本身未使用。",
            "",
            "- 配方图："
            + "、".join(
                f"`{row['source_map']}`（{row['source_rows']} 行）"
                for row in maps
                if row["denominator_class"] == "out_of_scope"
            ),
            "- 多方块控制器："
            + "、".join(
                f"`{row['canonical_id']}`（CC：`{row['delivery_depth']}`）"
                for row in multiblocks
                if row["disposition"] == "out_of_scope"
            ),
            "- 盖板："
            + "、".join(
                f"`{row['canonical_id']}`" for row in covers if row["disposition"] == "out_of_scope"
            ),
            "- 能量身份："
            + "、".join(
                f"`{row['symbol']}`" for row in energy if row["classification"] == "out_of_scope"
            ),
            "- 物品/流体生成域："
            + "、".join(
                f"`{row['canonical_id']}`" for row in domains if row["classification"] == "out_of_scope"
            ),
            f"- 材料前缀：{len(excluded_prefixes)} 个，其中 {len(unused_prefixes)} 个是 GT6 源里声明但显式未使用"
            f"（`explicit_unused`），其余 {len(excluded_prefixes) - len(unused_prefixes)} 个需要决策；"
            "名单见第 9 节 `out_of_scope` 组。",
        ]
    )

    lines.extend(
        [
            "",
            "## 15. 读法",
            "",
            "- `full_replay`：已追溯的不同 GT6 源行数等于源行数且无 overflow；仍需 wave/runtime 验证。",
            "- `empty_source`：GT6 固定 dump 里这张图是空的（0 行）。配方不在 RecipeMap 里"
            "（原版熔炉表、坩埚物理、NBT 假配方），不等于没有东西要做；对应机器看第 4 节。",
            "- `bounded_subset`：有已追溯源行、reference 追溯、未追溯 CC 行或材料规则中的任意一种，不代表全图完成。",
            "- `runtime_only`：有 RecipeMap/主机路由，但当前没有任何运行时配方行或规则。",
            "- `identity_only`：只在 CC 目录里有身份或部件登记，没有运行时。",
            "- `runtime_code_uncarded`：CC 有运行时代码或声明式多方块结构，但没有任何 capability 卡，状态未验收。",
            "- `runtime_paused`：有 capability，但是 frozen/paused，未 accepted。",
            "- `runtime_accepted`：无 RecipeMap 的机制（发电/转换/传动等）已有 accepted capability；数值与全部变体仍需逐卡核对。",
            "- `denominator_only`：只有 GT6 分母，CC 侧没有找到任何实现证据。",
            "- `legacy_exclusion_pending`：历史分母曾排除；当前全量目标尚未重新决策，见第 14 节。",
            "- `survival_access` 和获得格是独立轴，不由上面任何一列推出。",
            "",
            "## 16. 各轴来源与新鲜度",
            "",
            "| 轴 | 来源 | 类型 |",
            "| --- | --- | --- |",
            "| 配方图分母、机器 kind、多方块、盖板、能量、生成域、前缀的“分母/历史分类” | "
            "`tools/machine_tree_denominators/*.json` | 冻结分母（GT6 revision 固定，不随工作树变） |",
            "| 已追溯源行 / CC 行 / 材料规则 | 运行时资源根里的配方 JSON + "
            "`source_attribution.json` | live 扫描；归属钉需对照配方 dump 刷新（见代码树参考源） |",
            "| 材料规则展开数 | `tools/component_rule_manifest.json` | 上游产物（`build_component_rules.py`） |",
            "| overflow | `tools/waves/**/overflow.json` | 上游产物（各 wave builder） |",
            "| 机器 kind / 多方块证据 | capability、`src/main/java`、`machine_delivery.json`、多方块结构 | live 扫描 |",
            "| 盖板 live id | `*cover_definitions.json` | live 扫描 |",
            "| 能量 `EnergyType` | `EnergyType.java` | live 扫描 |",
            "| 前缀映射 | `tools/gt6_resolve.py` → `material_prefixes/` | live 扫描 |",
            "| MTE 身份 | R0 账本 + `mte_inplace_catalog.json` | 冻结账本 + live 叠加 |",
            "| 形态需求 | `tools/waves/prep/material-form-demand-census/census.json` | 上游产物（census builder） |",
            "| Capability / Blocker | `tools/capabilities/**`、`tools/blockers/catalog.json` | live 扫描 |",
            "| 逐行分类 / 进度 / 行动清单 | `semantic_coverage.json`、`exclusions.json` | "
            "live 扫描 + 翻译链（`--write` 需本地 dump） |",
        ]
    )
    lines.extend(_render_action_queue(maps, semantic))
    return "\n".join(lines) + "\n"


BLOCKER_KIND_LABELS = {
    "extruder_shape": "挤压模具",
    "form": "材料形态",
    "material": "材料",
    "fluid": "流体",
    "object": "物品/方块",
}


def _recipe_gap_hint(row: dict[str, Any]) -> str:
    if row.get("rule_expanded_recipes") or row.get("rule_files"):
        return "补材料规则模板"
    if row.get("traced_source_rows"):
        return "扩展已有 wave"
    if row.get("delivery_depth") == "denominator_only":
        return "先做机器，再做 wave"
    return "新开 dump wave"


def _render_action_queue(maps: list[dict[str, Any]], semantic: dict[str, Any]) -> list[str]:
    blockers = semantic.get("blockers") or []
    lines = [
        "",
        "## 17. 缺口行动清单（按杠杆排序，自动生成）",
        "",
        "本节只列事实和提示，不是 unique-active 队列；开工仍按能力交付流程开卡。"
        "每条不可翻译的源行只记它**第一个**缺的身份，补上后可能还卡在下一个，"
        "所以“受影响源行”是解锁数的上界。已被 `exclusions.json` 排除的行不计入。",
        "",
        "### 17.1 缺身份（前 40 项）",
        "",
        "“材料形态”是 GT6 配方实际用到、CC 材料已存在但没开的 (材料, 形态)，"
        "按仓库规则应进材料形态需求普查，再由开形态卡打开，不在配方卡上顺手开。",
        "",
        "| 类型 | 缺什么 | 受影响源行 | 涉及材料 | 主要机器 | 来自别的 mod |",
        "| --- | --- | ---: | ---: | --- | --- |",
    ]
    for entry in blockers[:40]:
        top_maps = ", ".join(
            f"{name.removeprefix('gt.recipe.')} {count}"
            for name, count in list((entry.get("top_maps") or {}).items())[:3]
        )
        lines.append(
            f"| {BLOCKER_KIND_LABELS.get(entry['kind'], entry['kind'])} | `{entry['label']}` | "
            f"{entry['rows']} | {entry.get('materials') or '—'} | {top_maps or '—'} | "
            f"{'是' if entry.get('other_mod') else ''} |"
        )
    kinds = semantic.get("blocker_kinds") or {}
    lines.extend(
        [
            "",
            "按类型合计（第一缺口口径）："
            + "，".join(
                f"{BLOCKER_KIND_LABELS.get(kind, kind)} {rows}"
                for kind, rows in sorted(kinds.items(), key=lambda item: -item[1])
            ),
            "",
            "### 17.2 缺配方（按机器，前 20 项）",
            "",
            "这些行已经能完整翻译成 CC 身份，只差配方本身。“提示”按已有内容给出，仅供排期参考。",
            "",
            "| GT6 map | 缺配方行 | 部分一致 | 已证明 | 交付深度 | 提示 |",
            "| --- | ---: | ---: | ---: | --- | --- |",
        ]
    )
    gaps = sorted(
        (row for row in maps if (row.get("raw_row_classes") or {}).get("translatable_missing")),
        key=lambda row: -row["raw_row_classes"]["translatable_missing"],
    )
    for row in gaps[:20]:
        classes = row["raw_row_classes"]
        partial = classes.get("translated_io_only", 0) + classes.get("translated_item_io", 0)
        proven = classes.get("source_exact", 0) + classes.get("translated_exact", 0)
        lines.append(
            f"| `{row['source_map']}` | {classes['translatable_missing']} | {partial} | {proven} | "
            f"`{row['delivery_depth']}` | {_recipe_gap_hint(row)} |"
        )
    candidates = [entry for entry in blockers if entry.get("other_mod")]
    legacy = [row for row in maps if (row.get("raw_row_classes") or {}).get("legacy_exclusion_pending")]
    lines.extend(
        [
            "",
            "### 17.3 排除候选（待决策，不会自动生效）",
            "",
            "下列缺口来自 GT6 以外的 mod，或是旧分母排除的图。"
            "决定不移植的，把规则写进 `tools/waves/portfolio/gt6-full-coverage-reassessment/exclusions.json` "
            "（要写理由和决策人），它们就会从进度目标里移出；决定移植的，留在上面的清单里。",
            "",
        ]
    )
    if candidates:
        lines.append(
            "- 别的 mod："
            + "、".join(f"`{entry['label']}`（{entry['rows']}）" for entry in candidates[:25])
        )
    if legacy:
        lines.append(
            "- 旧分母排除的图："
            + "、".join(
                f"`{row['source_map']}`（{row['raw_row_classes']['legacy_exclusion_pending']}）"
                for row in legacy
            )
        )
    if not candidates and not legacy:
        lines.append("（无）")
    return lines


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument(
        "--verify-source",
        action="store_true",
        help="re-verify source_attribution.json against the local gt6_dump without writing",
    )
    args = parser.parse_args(argv)
    if not (args.write or args.check or args.verify_source):
        parser.error("use --write, --check or --verify-source")
    if args.verify_source:
        rows, _ = scan_cc_recipes()
        owners = name_owners(load_recipe_maps())
        fresh = build_attribution(rows, owners)
        pinned = read_json(ATTRIBUTION_PATH) if ATTRIBUTION_PATH.is_file() else None
        if pinned != fresh:
            print(f"{ATTRIBUTION_PATH.relative_to(ROOT)} differs from the local gt6_dump; run --write")
            return 1
        print(
            "source attribution verified: "
            f"{len(fresh['full_overrides']) + len(fresh['short_overrides'])} overrides, "
            f"{len(fresh['unverified_full_hashes']) + len(fresh['unverified_short_hashes'])} unverified, "
            f"{len(fresh['ambiguous_hashes'])} ambiguous"
        )
        return 0
    try:
        run_semantic_builder("--check" if args.check else "--write")
    except (OSError, RuntimeError) as error:
        print(error)
        return 1
    try:
        report, chemistry, attribution = build_report(refresh_attribution=args.write)
    except StaleAttribution as error:
        print(error)
        return 1
    if args.check:
        stale = []
        if not OUTPUT_PATH.is_file() or read_json(OUTPUT_PATH) != report:
            stale.append(OUTPUT_PATH)
        if not CHEM_OUTPUT_PATH.is_file() or read_json(CHEM_OUTPUT_PATH) != chemistry:
            stale.append(CHEM_OUTPUT_PATH)
        if not MARKDOWN_PATH.is_file() or MARKDOWN_PATH.read_text(encoding="utf-8") != render_markdown(
            report, chemistry
        ):
            stale.append(MARKDOWN_PATH)
        if stale:
            for path in stale:
                print(f"{path.relative_to(ROOT)} is stale")
            print(
                "run python tools/waves/portfolio/gt6-full-coverage-reassessment/"
                "build_reconciliation.py --write"
            )
            return 1
        print("GT6 full coverage reconciliation is current")
        return 0
    write_json(ATTRIBUTION_PATH, attribution)
    write_json(OUTPUT_PATH, report)
    write_json(CHEM_OUTPUT_PATH, chemistry)
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text(
        render_markdown(report, chemistry),
        encoding="utf-8",
    )
    for path in (ATTRIBUTION_PATH, OUTPUT_PATH, CHEM_OUTPUT_PATH, MARKDOWN_PATH):
        print(f"wrote {path.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
