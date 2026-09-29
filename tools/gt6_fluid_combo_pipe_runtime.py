#!/usr/bin/env python3
"""Register GT6 quadruple/nonuple fluid pipes as live multi-tank BlockItems.

Does not rewrite the closed fluid execution subset, R0, or the baseline
identity ledger. Catalog projection lives in this child's overlay.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import catalog_modern_ids as modern
from tools import census_common as census
from tools import io_common as io

SLUG = "content/gt6-fluid-combo-pipe-runtime"
STATUS = "FLUID_COMBO_PIPE_RUNTIME_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-fluid-combo-pipe-runtime"
BASELINE = census.TOOLS / "waves" / "content" / "gt6-pipe-cable-baseline"
LEDGER = BASELINE / "identity_resolution_ledger.json"
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
FLUID_SUBSET = (
    census.TOOLS / "waves" / "content" / "gt6-fluid-pipe-runtime" / "execution_subset.json"
)
DATA_CATALOG = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "smelter_mte_identity_catalog.json"
)
TOOLS_CATALOG = census.TOOLS / "smelter_mte_identity_catalog.json"
MATERIAL_GATE = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
MATERIALS = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "materials"
)
ITEM_MODELS = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "models"
    / "item"
)
BE = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "blockentity"
    / "FluidPipeBlockEntity.java"
)
CATALOG_JAVA = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "logistics"
    / "pipe"
    / "PipeCatalog.java"
)
GAME_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "FluidComboPipeRuntimeGameTests.java"
)
CORE_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "CrucibleCraftGameTests.java"
)
EMPTY_SRC = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_fluid_pipe_runtime"
    / "structure"
    / "empty.nbt"
)
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_fluid_combo_pipe_runtime"
)
RECIPE_ROOTS = (
    census.ROOT / "src" / "recipe_generated",
    census.ROOT / "src" / "recipe_support_generated",
)
MELTER_WAVE = census.TOOLS / "waves" / "machines" / "melter"
MELTER_IMPORT = MELTER_WAVE / "recipe_import.json"
MELTER_LOCK_NOTE = (
    "live compile for machines/melter; 3601 runtime-registered exact rows "
    "from the 6756-row gt.recipe.melter dump; 3155 overflow rows explicitly "
    "blocked including ungated plant prefixes that Java does not register; "
    "load publication is UNVERIFIED_SCALE and below the 21000 hard cap; "
    "not player_complete"
)
OVERLAY_PATH = WAVE / "combo_overlay.json"
CANONICAL = {"hsla_steel": "hslasteel"}
QUAD_FORM = "quadruple_fluid_pipe"
NONUPLE_FORM = "nonuple_fluid_pipe"
QUAD_FLAG = "cruciblecraft:generates_quadruple_fluid_pipe"
NONUPLE_FLAG = "cruciblecraft:generates_nonuple_fluid_pipe"
MEDIUM_FLAG = "cruciblecraft:generates_fluid_pipe"
SMALL_FLAG = "cruciblecraft:generates_small_fluid_pipe"
REASON = "folded onto live combo fluid pipe"
EXPECTED_TESTS = [
    "comboPipeDummyFoldsOntoLiveHost",
    "comboPipeIndependentTanks",
    "comboPipeIsNotHugeAlias",
]


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def _strip_ns(value: str) -> str:
    return value.split(":", 1)[-1]


def _compact(value: str) -> str:
    return re.sub(r"[^a-z0-9]", "", str(value).lower())


def _subset_rows(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []
    return list(census.load_json(path).get("rows") or [])


def _combo_form(dummy_path: str) -> str | None:
    name = dummy_path.rsplit("/", 1)[-1]
    if name.startswith("quadruple_"):
        return QUAD_FORM
    if name.startswith("nonuple_"):
        return NONUPLE_FORM
    return None


def _patch_material_flags() -> int:
    changed = 0
    for path in sorted(MATERIALS.glob("*.json")):
        document = census.load_json(path)
        if not isinstance(document, dict):
            continue
        flags = list(document.get("generation_flags") or [])
        flag_set = set(flags)
        extra: list[str] = []
        if MEDIUM_FLAG in flag_set and QUAD_FLAG not in flag_set:
            extra.append(QUAD_FLAG)
        if SMALL_FLAG in flag_set and NONUPLE_FLAG not in flag_set:
            extra.append(NONUPLE_FLAG)
        if not extra:
            continue
        document["generation_flags"] = sorted([*flags, *extra])
        payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
        if path.read_text(encoding="utf-8") != payload:
            path.write_text(payload, encoding="utf-8")
        changed += 1
    return changed


def _patch_gate() -> dict[str, int]:
    document = census.load_json(MATERIAL_GATE)
    materials = document.get("materials") or {}
    combo: dict[str, list[str]] = {}
    added = 0
    for material_id, forms in materials.items():
        current = list(forms or [])
        extra: list[str] = []
        if "fluid_pipe" in current and QUAD_FORM not in current:
            extra.append(QUAD_FORM)
        if "small_fluid_pipe" in current and NONUPLE_FORM not in current:
            extra.append(NONUPLE_FORM)
        if extra:
            materials[material_id] = sorted([*current, *extra])
            added += len(extra)
        live = materials[material_id]
        selected = [form for form in live if form in {QUAD_FORM, NONUPLE_FORM}]
        if selected:
            combo[material_id] = selected
    document["materials"] = materials
    document["combo_pipe_forms"] = combo
    counts = document.setdefault("counts", {})
    counts["combo_pipe_forms"] = sum(len(forms) for forms in combo.values())
    counts["registered_forms"] = sum(len(forms) for forms in materials.values())
    io.write_stable(MATERIAL_GATE, document)
    from tools import currentness

    currentness.write_sidecar(MATERIAL_GATE)
    return {
        "materials_with_combo": len(combo),
        "forms_added": added,
        "combo_pipe_forms": int(counts["combo_pipe_forms"]),
        "registered_forms": int(counts["registered_forms"]),
    }


def _canonical_material(recorded: str, gated: dict[str, set[str]]) -> str | None:
    if recorded in CANONICAL:
        return CANONICAL[recorded]
    if recorded in gated:
        return recorded
    compact_live = {_compact(material): material for material in gated}
    return compact_live.get(_compact(recorded))


def build_overlay() -> dict[str, Any]:
    gated = {
        str(material): set(forms or [])
        for material, forms in (census.load_json(MATERIAL_GATE).get("materials") or {}).items()
    }
    live = modern.live_host_paths()
    rows: list[dict[str, Any]] = []
    skipped: list[dict[str, Any]] = []
    for row in _subset_rows(FLUID_SUBSET):
        dummy = str(row.get("dummy_path") or "")
        form = _combo_form(dummy)
        if form is None:
            continue
        recorded = str(row.get("material") or "")
        canonical = _canonical_material(recorded, gated)
        live_path = f"{canonical}/{form}" if canonical else ""
        if (
            not canonical
            or form not in gated.get(canonical, set())
            or live_path not in live
        ):
            skipped.append(
                {
                    "meta": int(row["meta"]),
                    "material": recorded,
                    "dummy_path": dummy,
                    "reason": "No gated live combo BlockItem for this GT6 gauge.",
                }
            )
            continue
        rows.append(
            {
                "meta": int(row["meta"]),
                "material": recorded,
                "canonical_material": canonical,
                "cc_form": form,
                "dummy_path": dummy,
                "live_block": f"cruciblecraft:{live_path}",
                "collision_reason": REASON,
            }
        )
    rows.sort(key=lambda row: int(row["meta"]))
    skipped.sort(key=lambda row: int(row["meta"]))
    return {
        "schema": "gt6-fluid-combo-pipe-overlay-v1",
        "capability_slug": SLUG,
        "counts": {
            "fold_live_block": len(rows),
            "quadruple": sum(1 for row in rows if row["cc_form"] == QUAD_FORM),
            "nonuple": sum(1 for row in rows if row["cc_form"] == NONUPLE_FORM),
            "skipped": len(skipped),
        },
        "rows": rows,
        "skipped": skipped,
    }


def folded_metas() -> set[int]:
    if not OVERLAY_PATH.is_file():
        return set()
    return {
        int(row["meta"])
        for row in census.load_json(OVERLAY_PATH).get("rows") or []
    }


def folded_dummy_paths() -> set[str]:
    if not OVERLAY_PATH.is_file():
        return set()
    return {
        str(row.get("dummy_path") or "")
        for row in census.load_json(OVERLAY_PATH).get("rows") or []
        if row.get("dummy_path")
    }


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": "content/gt6-fluid-combo-pipe-runtime implementation",
        "append_only": False,
        "next_unassigned": True,
        "remaining_recipe_gap": 0,
    }


def readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
    }


def current_gap(overlay: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema": "gt6-fluid-combo-pipe-gap-v1",
        "capability_slug": SLUG,
        "folded": overlay["counts"],
        "still_dummy": overlay.get("skipped") or [],
        "blocked": [
            "restrictive item pipes",
            "wireGt07/09/10/11/13/14/15 prefixes",
            "insulated redstone 27006/27056/27506",
            "plasma/magic destroy/replace, flammable, contactDamage",
            "connector survival obtain grids",
        ],
        "close_target": "runtime_ready",
        "note": (
            "Closed fluid execution subset stays historical keep_distinct. "
            "This overlay is the current catalog projection. "
            "Boxinator/Unboxinator wait for the acquisition child."
        ),
    }


def _patch_catalogs(fold_by_meta: dict[int, dict[str, Any]]) -> int:
    changed = 0
    for path in (DATA_CATALOG, TOOLS_CATALOG):
        document = census.load_json(path)
        identities = list(document.get("identities") or [])
        for identity in identities:
            row = fold_by_meta.get(int(identity["meta"]))
            if row is None:
                continue
            live = _strip_ns(str(row["live_block"]))
            if identity.get("registry_kind") != "existing_item" or identity.get(
                "registry_path"
            ) != live:
                identity["registry_kind"] = "existing_item"
                identity["registry_path"] = live
                identity["runtime_id"] = f"cruciblecraft:{live}"
                changed += 1
        created = [
            identity
            for identity in identities
            if identity.get("registry_kind") == "item"
        ]
        document["new_item_count"] = len(created)
        _write_json(path, document)
    return changed


def _patch_modern_map(fold_by_meta: dict[int, dict[str, Any]]) -> int:
    document = census.load_json(modern.MAP_PATH)
    changed = 0
    for row in document.get("rows") or []:
        if str(row.get("source_item") or "") != "gregtech:gt.multitileentity":
            continue
        fold = fold_by_meta.get(int(row["meta"]))
        if fold is None:
            continue
        live = _strip_ns(str(fold["live_block"]))
        if row.get("registry_path") != live:
            row["registry_path"] = live
            row["runtime_id"] = f"cruciblecraft:{live}"
            row["collision_reason"] = REASON
            changed += 1
        elif row.get("collision_reason") != REASON:
            row["collision_reason"] = REASON
            changed += 1
    document["collision_count"] = sum(
        1 for row in document.get("rows") or [] if row.get("collision_reason")
    )
    _write_json(modern.MAP_PATH, document)
    return changed


def _withdraw_dummy_models(dummies: set[str]) -> int:
    removed = 0
    for dummy in dummies:
        path = ITEM_MODELS / f"{dummy}.json"
        if path.is_file():
            path.unlink()
            removed += 1
    return removed


def _recipe_remap_pairs(overlay: dict[str, Any]) -> list[tuple[str, str]]:
    pairs: list[tuple[str, str]] = []
    for row in overlay.get("rows") or []:
        dummy = str(row.get("dummy_path") or "")
        live = _strip_ns(str(row.get("live_block") or ""))
        if not dummy or not live or dummy == live:
            continue
        pairs.append((f"cruciblecraft:{dummy}", f"cruciblecraft:{live}"))
    pairs.sort(key=lambda item: len(item[0]), reverse=True)
    return pairs


def _remap_recipe_ids(overlay: dict[str, Any]) -> int:
    pairs = _recipe_remap_pairs(overlay)
    if not pairs:
        return 0
    changed = 0
    for root in RECIPE_ROOTS:
        if not root.exists():
            continue
        for path in root.rglob("*.json"):
            if not path.is_file():
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            updated = text
            for old, new in pairs:
                updated = updated.replace(old, new)
            if updated != text:
                path.write_text(updated, encoding="utf-8", newline="\n")
                changed += 1
    return changed


def _refresh_melter_source_import() -> dict[str, str]:
    """Replay melter Source Pack so folded dummy combo IDs become live hosts.

    Catalog fold changes compile_row runtime_ids. Closed melter check_import
    regenerates source/lock from the dump, so those documents must follow the
    live quadruple/nonuple BlockItems. Does not rewrite melter topology or
    reopen the melter card.
    """
    from tools.recipe_bulk import source_import
    from tools.waves.prep import machine_prep_common as common

    if not MELTER_IMPORT.is_file():
        raise FileNotFoundError(f"missing {census.relative(MELTER_IMPORT)}")
    wrote = source_import.write_import(MELTER_IMPORT)
    common.freeze_lock(MELTER_WAVE, "machines/melter", MELTER_LOCK_NOTE)
    return {
        "import_slug": str(wrote.get("import_slug") or "machines/melter"),
        "source": census.relative(MELTER_WAVE / "source.json"),
        "lock_candidate": census.relative(MELTER_WAVE / "lock_candidate.json"),
    }


def _recipe_dummy_leftovers(overlay: dict[str, Any]) -> list[str]:
    leftover = _recipe_remap_pairs(overlay)
    errors: list[str] = []
    for root in RECIPE_ROOTS:
        if not root.exists():
            continue
        for path in root.rglob("*.json"):
            if not path.is_file():
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            for old, _new in leftover:
                if old in text:
                    errors.append(
                        f"{census.relative(path)} still names folded dummy {old}"
                    )
                    break
    return errors


def _copy_empty_nbt() -> None:
    if not EMPTY_SRC.is_file():
        raise FileNotFoundError(f"missing {census.relative(EMPTY_SRC)}")
    for dest in (
        PACK / "structure" / "empty.nbt",
        PACK / "gametest" / "structure" / "empty.nbt",
    ):
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.is_file() and dest.read_bytes() == EMPTY_SRC.read_bytes():
            continue
        shutil.copyfile(EMPTY_SRC, dest)


def write(unique_active: bool = True) -> dict[str, Any]:
    flags = _patch_material_flags()
    gate = _patch_gate()
    overlay = build_overlay()
    if int(overlay["counts"]["fold_live_block"]) <= 0:
        raise ValueError("combo overlay folded zero catalog rows")
    WAVE.mkdir(parents=True, exist_ok=True)
    fold_by_meta = {int(row["meta"]): row for row in overlay["rows"]}
    _write_json(OVERLAY_PATH, overlay)
    _write_json(WAVE / "current_gap.json", current_gap(overlay))
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(WAVE / "readiness.json", readiness(unique_active))
    _write_json(
        WAVE / "production_lock.json",
        {
            "note": (
                "quadruple/nonuple fluid pipes are live multi-tank BlockItems; "
                "not player_complete"
            )
        },
    )
    folded = _patch_catalogs(fold_by_meta)
    mapped = _patch_modern_map(fold_by_meta)
    modern.rewrite_catalog_item_tags()
    remapped = _remap_recipe_ids(overlay)
    modern.write_registry_identity_manifest()
    melter_import = _refresh_melter_source_import()
    withdrawn = _withdraw_dummy_models(folded_dummy_paths())
    _copy_empty_nbt()
    return {
        "material_flag_files": flags,
        "gate": gate,
        "folded_identities": folded,
        "mapped_rows": mapped,
        "remapped_recipe_files": remapped,
        "melter_import": melter_import,
        "withdrawn_models": withdrawn,
        "fold_live_block": len(fold_by_meta),
    }


def check() -> list[str]:
    errors: list[str] = []
    if not OVERLAY_PATH.is_file():
        return [f"missing {census.relative(OVERLAY_PATH)}"]
    live = build_overlay()
    committed = census.load_json(OVERLAY_PATH)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"combo_overlay.json drifted: {drift}")
    counts = committed.get("counts") or {}
    if int(counts.get("fold_live_block") or 0) != 56:
        errors.append(
            f"fold_live_block {counts.get('fold_live_block')} != 56"
        )
    if int(counts.get("quadruple") or 0) != int(counts.get("nonuple") or 0):
        errors.append(
            f"quadruple {counts.get('quadruple')} != nonuple {counts.get('nonuple')}"
        )
    gap_path = WAVE / "current_gap.json"
    if not gap_path.is_file():
        errors.append("missing current_gap.json")
    else:
        gap_drift = census.first_json_diff(
            current_gap(committed), census.load_json(gap_path)
        )
        if gap_drift:
            errors.append(f"current_gap.json drifted: {gap_drift}")
    fold_by_meta = {int(row["meta"]): row for row in committed.get("rows") or []}
    live_hosts = modern.live_host_paths()
    for path in (DATA_CATALOG, TOOLS_CATALOG):
        catalog = census.load_json(path)
        by_meta = {int(row["meta"]): row for row in catalog.get("identities") or []}
        created = [
            row
            for row in catalog.get("identities") or []
            if row.get("registry_kind") == "item"
        ]
        if int(catalog.get("new_item_count") or 0) != len(created):
            errors.append(
                f"{census.relative(path)} new_item_count "
                f"{catalog.get('new_item_count')} != {len(created)}"
            )
        for meta, fold in fold_by_meta.items():
            identity = by_meta.get(meta)
            if identity is None:
                errors.append(f"{census.relative(path)} missing meta {meta}")
                continue
            live_path = _strip_ns(str(fold["live_block"]))
            if live_path not in live_hosts:
                errors.append(f"combo live host {live_path} is not gated")
            if identity.get("registry_kind") != "existing_item":
                errors.append(f"meta {meta} still dummy item")
            if identity.get("registry_path") != live_path:
                errors.append(
                    f"meta {meta} catalog {identity.get('registry_path')} "
                    f"!= {live_path}"
                )
    mapped = {
        int(row["meta"]): row
        for row in census.load_json(modern.MAP_PATH).get("rows") or []
        if str(row.get("source_item") or "") == "gregtech:gt.multitileentity"
    }
    for meta, fold in fold_by_meta.items():
        row = mapped.get(meta)
        live_path = _strip_ns(str(fold["live_block"]))
        if row is None:
            errors.append(f"modern map missing folded meta {meta}")
            continue
        if row.get("registry_path") != live_path:
            errors.append(
                f"modern map meta {meta} {row.get('registry_path')} != {live_path}"
            )
        dummy = ITEM_MODELS / f"{fold['dummy_path']}.json"
        if dummy.is_file() and "iron_ingot" in dummy.read_text(encoding="utf-8"):
            errors.append(f"folded dummy {fold['dummy_path']} still uses iron_ingot")
    be = BE.read_text(encoding="utf-8")
    if "tankCount()" not in be or 'tag.put("tanks"' not in be:
        errors.append("FluidPipeBlockEntity lost multi-tank NBT")
    if "fillMatching" not in be:
        errors.append("FluidPipeBlockEntity lost matching-then-empty fill")
    catalog_java = CATALOG_JAVA.read_text(encoding="utf-8")
    if "QUADRUPLE_FLUID_PIPE" not in catalog_java:
        errors.append("PipeCatalog lost quadruple form")
    if "NONUPLE_FLUID_PIPE" not in catalog_java:
        errors.append("PipeCatalog lost nonuple form")
    if "MAX_RUNTIME_BLOCKS = 500" not in catalog_java:
        errors.append("PipeCatalog budget was not raised for combo pipes")
    tests = GAME_TESTS.read_text(encoding="utf-8") if GAME_TESTS.is_file() else ""
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in EXPECTED_TESTS:
        if name in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing wave structure/empty.nbt")
    if not (PACK / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append("missing wave gametest/structure/empty.nbt")
    errors.extend(_recipe_dummy_leftovers(committed))
    leftover = _recipe_remap_pairs(committed)
    melter_source = MELTER_WAVE / "source.json"
    if melter_source.is_file():
        melter_text = melter_source.read_text(encoding="utf-8")
        for old, _new in leftover:
            if old in melter_text:
                errors.append(f"melter source.json still names folded dummy {old}")
                break
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument(
        "--unique-active",
        action=argparse.BooleanOptionalAction,
        default=True,
    )
    args = parser.parse_args(argv)
    if args.write:
        write(unique_active=args.unique_active)
    errors = check() if args.check or not args.write else []
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    if args.write:
        print(f"wrote {SLUG}")
    else:
        print(f"{SLUG} ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
