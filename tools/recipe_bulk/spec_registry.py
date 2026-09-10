#!/usr/bin/python3
"""Discover RecipeImportSpec documents from data files, not Python tuples."""
from __future__ import annotations

from pathlib import Path
from typing import Any

from tools import census_common as census
from tools.recipe_bulk.import_spec import load_import_spec
from tools.recipe_bulk.models import WaveSpec

RECIPE_GENERATED_ROOT = (
    census.ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe"
)

SEARCH_ROOTS = (
    census.TOOLS / "waves",
    census.ROOT / "src" / "test" / "resources" / "generic_recipe_import",
)


class SpecRegistryError(ValueError):
    """Import spec discovery or compile-spec derivation failed closed."""


def discover_spec_paths() -> list[Path]:
    found: list[Path] = []
    for root in SEARCH_ROOTS:
        if not root.is_dir():
            continue
        found.extend(sorted(root.rglob("recipe_import.json")))
    return found


def load_discovered() -> dict[str, Path]:
    index: dict[str, Path] = {}
    for path in discover_spec_paths():
        document = census.load_json(path)
        slug = str((document or {}).get("import_slug") or "")
        if not slug:
            raise SpecRegistryError(f"{census.relative(path)} missing import_slug")
        previous = index.get(slug)
        if previous is not None and previous != path:
            raise SpecRegistryError(f"duplicate import_slug {slug}")
        index[slug] = path
    return index


def spec_path_for(slug: str) -> Path | None:
    return load_discovered().get(slug)


def load_spec_for(slug: str) -> dict[str, Any]:
    path = spec_path_for(slug)
    if path is None:
        raise SpecRegistryError(f"no recipe_import.json for {slug}")
    return load_import_spec(path)


def production_lock_path(spec_path: Path, document: dict[str, Any] | None = None) -> Path:
    spec = document or census.load_json(spec_path)
    source_out = spec.get("output_paths") or {}
    source_rel = str(source_out.get("source") or "")
    if source_rel:
        return (census.ROOT / source_rel).resolve().parent / "production_lock.json"
    return spec_path.parent / "production_lock.json"


def derive_wave_spec(slug: str) -> WaveSpec:
    path = spec_path_for(slug)
    if path is None:
        raise SpecRegistryError(f"unknown import slug {slug}")
    document = load_import_spec(path)
    if str(slug).startswith("prep/"):
        raise SpecRegistryError(
            f"{slug}: prep waves cannot compile into src/recipe_generated; "
            "use isolated compile"
        )
    lock_path = production_lock_path(path, document)
    if not lock_path.is_file():
        raise SpecRegistryError(
            f"{slug}: compile requires reviewed production_lock.json; "
            "lock_candidate is not production authority"
        )
    outputs = document["output_paths"]
    source_path = census.ROOT / str(outputs["source"])
    wave_root = source_path.parent
    host = str(document["host"])
    path_prefix = host.split(":", 1)[-1]
    return WaveSpec(
        wave_id=slug,
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host=host,
        target_map=str(document["target_map"]),
        source_path=source_path,
        generated_root=RECIPE_GENERATED_ROOT,
        equivalence_path=wave_root / "equivalence.json",
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="cohort_nested",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=lock_path,
        operand_map_path=wave_root / "operand_runtime_map.json",
        wave_slug=slug,
        cohort=slug.split("/", 1)[-1].replace("-", "_"),
        representation="exact_or_exact_multi",
        path_prefix=path_prefix,
        dry_run_without_lock=False,
    )


def try_derive_wave_spec(slug: str) -> WaveSpec | None:
    path = spec_path_for(slug)
    if path is None:
        return None
    return derive_wave_spec(slug)
