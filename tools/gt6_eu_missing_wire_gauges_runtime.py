#!/usr/bin/env python3
"""Register GT6 wireGt07/09/10/11/13/14/15 as live CableBlockItems.

Does not rewrite the closed EU execution subset, R0, or the baseline
identity ledger. Catalog projection lives in this child's overlay.
Red alloy, Signalum, and Lumium stay out of ElectricalConductorCatalog.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import catalog_modern_ids as modern
from tools import census_common as census
from tools import io_common as io

SLUG = "content/gt6-eu-missing-wire-gauges-runtime"
STATUS = "EU_MISSING_WIRE_GAUGES_RUNTIME_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-eu-missing-wire-gauges-runtime"
BASELINE = census.TOOLS / "waves" / "content" / "gt6-pipe-cable-baseline"
LEDGER = BASELINE / "identity_resolution_ledger.json"
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
EU_SUBSET = (
    census.TOOLS / "waves" / "content" / "gt6-eu-wire-cable-runtime" / "execution_subset.json"
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
CATALOG_JAVA = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "energy"
    / "cable"
    / "ElectricalConductorCatalog.java"
)
GAME_TESTS = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "EuMissingWireGaugesRuntimeGameTests.java"
)
CORE_TESTS = (
    census.ROOT
    / "src"
    / "main"
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
    / "cruciblecraft_wave_content_gt6_eu_wire_cable_runtime"
    / "structure"
    / "empty.nbt"
)
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_eu_missing_wire_gauges_runtime"
)
RECIPE_ROOTS = (
    census.ROOT / "src" / "recipe_generated",
    census.ROOT / "src" / "recipe_support_generated",
)
LOOM_WAVE = census.TOOLS / "waves" / "machines" / "loom"
LOOM_IMPORT = LOOM_WAVE / "recipe_import.json"
LOOM_LOCK_NOTE = (
    "live compile for machines/loom; 465 runtime-registered exact rows; "
    "232 unmapped MTE/plant_gt_fiber rows and 637 shadowed input signatures "
    "explicitly_blocked; not player_complete"
)
MELTER_WAVE = census.TOOLS / "waves" / "machines" / "melter"
MELTER_IMPORT = MELTER_WAVE / "recipe_import.json"
MELTER_LOCK_NOTE = (
    "live compile for machines/melter; 3961 runtime-registered exact rows "
    "from the 6756-row gt.recipe.melter dump; 2795 overflow rows explicitly "
    "blocked; load publication is UNVERIFIED_SCALE and below the 21000 hard "
    "cap; not player_complete"
)
OVERLAY_PATH = WAVE / "missing_gauge_overlay.json"
CANONICAL = {"hsla_steel": "hslasteel"}
REDSTONE = {"red_alloy", "signalum", "lumium"}
WIRE_FLAG = "cruciblecraft:generates_wire"
GAUGES = (
    ("wireGt07", "septuple_wire", 7),
    ("wireGt09", "nonuple_wire", 9),
    ("wireGt10", "decuple_wire", 10),
    ("wireGt11", "undecuple_wire", 11),
    ("wireGt13", "tredecuple_wire", 13),
    ("wireGt14", "tetradecuple_wire", 14),
    ("wireGt15", "pentadecuple_wire", 15),
)
FORM_BY_GAUGE = {gauge: form for _spec, form, gauge in GAUGES}
SPEC_BY_FORM = {form: spec for spec, form, _gauge in GAUGES}
FLAG_BY_FORM = {
    form: f"cruciblecraft:generates_{form}" for _spec, form, _gauge in GAUGES
}
REASON = "folded onto live missing-gauge EU wire"
EXPECTED_TESTS = [
    "missingGaugeDummiesFoldOntoLiveHost",
    "missingGaugeWiresAreLiveCableBlocks",
    "missingGaugesAreNotMappedWireAlias",
]
EXPECTED_FOLDS = 168
EXPECTED_WIRES = 445
EXPECTED_CABLES = 116


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _strip_ns(value: str) -> str:
    return value.split(":", 1)[-1]


def _compact(value: str) -> str:
    return re.sub(r"[^a-z0-9]", "", str(value).lower())


def _subset_rows(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []
    return list(census.load_json(path).get("rows") or [])


def _electrical_specs(material_id: str) -> set[str]:
    path = MATERIALS / f"{material_id}.json"
    if not path.is_file():
        return set()
    document = census.load_json(path)
    if not isinstance(document, dict):
        return set()
    specs = (document.get("gt6_metadata") or {}).get("electrical_by_specification") or {}
    return {str(name) for name in specs}


def _missing_form(dummy_path: str) -> str | None:
    name = dummy_path.rsplit("/", 1)[-1]
    for _spec, form, gauge in sorted(GAUGES, key=lambda row: row[2], reverse=True):
        if name.startswith(f"{gauge}x_"):
            return form
    return None


def _electrical_wire_materials() -> set[str]:
    gate = census.load_json(MATERIAL_GATE)
    return {
        str(material)
        for material in (gate.get("electrical_wire_forms") or {})
        if material not in REDSTONE
    }


def _patch_material_flags() -> int:
    allowed = _electrical_wire_materials()
    changed = 0
    for path in sorted(MATERIALS.glob("*.json")):
        document = census.load_json(path)
        if not isinstance(document, dict):
            continue
        material_id = path.stem
        if material_id not in allowed:
            continue
        flags = list(document.get("generation_flags") or [])
        flag_set = set(flags)
        if WIRE_FLAG not in flag_set:
            continue
        specs = _electrical_specs(material_id)
        extra: list[str] = []
        for spec, form, _gauge in GAUGES:
            flag = FLAG_BY_FORM[form]
            if spec in specs and flag not in flag_set:
                extra.append(flag)
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
    allowed = {
        str(material)
        for material in (document.get("electrical_wire_forms") or {})
        if material not in REDSTONE
    }
    missing: dict[str, list[str]] = {}
    added = 0
    for material_id, forms in materials.items():
        if material_id not in allowed:
            continue
        current = list(forms or [])
        if "wire" not in current:
            continue
        specs = _electrical_specs(material_id)
        extra: list[str] = []
        for spec, form, _gauge in GAUGES:
            if spec in specs and form not in current:
                extra.append(form)
        if extra:
            materials[material_id] = sorted([*current, *extra])
            added += len(extra)
        live = materials[material_id]
        selected = [form for form in live if form in SPEC_BY_FORM]
        if selected:
            missing[material_id] = selected
    document["materials"] = materials
    document["missing_wire_forms"] = missing
    counts = document.setdefault("counts", {})
    counts["missing_wire_forms"] = sum(len(forms) for forms in missing.values())
    counts["registered_forms"] = sum(len(forms) for forms in materials.values())
    io.write_stable(MATERIAL_GATE, document)
    from tools import currentness

    currentness.write_sidecar(MATERIAL_GATE)
    return {
        "materials_with_missing_gauges": len(missing),
        "forms_added": added,
        "missing_wire_forms": int(counts["missing_wire_forms"]),
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
    for row in _subset_rows(EU_SUBSET):
        dummy = str(row.get("dummy_path") or "")
        form = _missing_form(dummy)
        if form is None:
            continue
        recorded = str(row.get("material") or "")
        canonical = _canonical_material(recorded, gated)
        live_path = f"{canonical}/{form}" if canonical else ""
        if (
            not canonical
            or canonical in REDSTONE
            or form not in gated.get(canonical, set())
            or live_path not in live
        ):
            skipped.append(
                {
                    "meta": int(row["meta"]),
                    "material": recorded,
                    "dummy_path": dummy,
                    "reason": "No gated live missing-gauge CableBlockItem.",
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
    by_form = {form: 0 for form in SPEC_BY_FORM}
    for row in rows:
        by_form[row["cc_form"]] += 1
    return {
        "schema": "gt6-eu-missing-wire-gauges-overlay-v1",
        "capability_slug": SLUG,
        "counts": {
            "fold_live_block": len(rows),
            "skipped": len(skipped),
            **by_form,
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
        "generated_by": "content/gt6-eu-missing-wire-gauges-runtime implementation",
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
        "schema": "gt6-eu-missing-wire-gauges-gap-v1",
        "capability_slug": SLUG,
        "folded": overlay["counts"],
        "still_dummy": overlay.get("skipped") or [],
        "blocked": [
            "insulated redstone 27006/27056/27506",
            "plasma/magic destroy/replace, flammable, contactDamage",
            "connector survival obtain grids",
        ],
        "close_target": "runtime_ready",
        "note": (
            "Closed EU execution subset stays historical keep_distinct. "
            "This overlay is the current catalog projection. "
            "Redstone materials never enter ElectricalConductorCatalog."
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


def _refresh_source_import(
    wave: Path,
    import_path: Path,
    slug: str,
    note: str,
) -> dict[str, str]:
    from tools.recipe_bulk import source_import
    from tools.waves.prep import machine_prep_common as common

    if not import_path.is_file():
        raise FileNotFoundError(f"missing {census.relative(import_path)}")
    wrote = source_import.write_import(import_path)
    common.freeze_lock(wave, slug, note)
    return {
        "import_slug": str(wrote.get("import_slug") or slug),
        "source": census.relative(wave / "source.json"),
        "lock_candidate": census.relative(wave / "lock_candidate.json"),
    }


def _refresh_closed_machine_imports() -> dict[str, dict[str, str]]:
    return {
        "loom": _refresh_source_import(
            LOOM_WAVE, LOOM_IMPORT, "machines/loom", LOOM_LOCK_NOTE
        ),
        "melter": _refresh_source_import(
            MELTER_WAVE, MELTER_IMPORT, "machines/melter", MELTER_LOCK_NOTE
        ),
    }


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
    if int(overlay["counts"]["fold_live_block"]) != EXPECTED_FOLDS:
        raise ValueError(
            f"fold_live_block {overlay['counts']['fold_live_block']} != {EXPECTED_FOLDS}"
        )
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
                "wireGt07/09/10/11/13/14/15 are live CableBlockItems; "
                "not player_complete"
            )
        },
    )
    folded = _patch_catalogs(fold_by_meta)
    mapped = _patch_modern_map(fold_by_meta)
    modern.rewrite_catalog_item_tags()
    remapped = _remap_recipe_ids(overlay)
    modern.write_registry_identity_manifest()
    loom_import = _refresh_closed_machine_imports()
    withdrawn = _withdraw_dummy_models(folded_dummy_paths())
    _copy_empty_nbt()
    (WAVE / "r0_disposition_sha256.txt").write_text(
        _sha256(R0) + "\n", encoding="utf-8"
    )
    (WAVE / "baseline_ledger_sha256.txt").write_text(
        _sha256(LEDGER) + "\n", encoding="utf-8"
    )
    return {
        "material_flag_files": flags,
        "gate": gate,
        "folded_identities": folded,
        "mapped_rows": mapped,
        "remapped_recipe_files": remapped,
        "loom_import": loom_import,
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
        errors.append(f"missing_gauge_overlay.json drifted: {drift}")
    counts = committed.get("counts") or {}
    if int(counts.get("fold_live_block") or 0) != EXPECTED_FOLDS:
        errors.append(
            f"fold_live_block {counts.get('fold_live_block')} != {EXPECTED_FOLDS}"
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
                errors.append(f"missing-gauge live host {live_path} is not gated")
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
    catalog_java = CATALOG_JAVA.read_text(encoding="utf-8")
    if f"EXPECTED_WIRE_BLOCKS = {EXPECTED_WIRES}" not in catalog_java:
        errors.append("ElectricalConductorCatalog wire census was not raised")
    if f"EXPECTED_CABLE_BLOCKS = {EXPECTED_CABLES}" not in catalog_java:
        errors.append("ElectricalConductorCatalog cable census drifted")
    if "SEPTUPLE_WIRE" not in catalog_java or "wireGt07" not in catalog_java:
        errors.append("ElectricalConductorCatalog lost wireGt07")
    if "red_alloy" not in catalog_java:
        errors.append("redstone exclusion missing from ElectricalConductorCatalog")
    tests = GAME_TESTS.read_text(encoding="utf-8") if GAME_TESTS.is_file() else ""
    for name in EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in EXPECTED_TESTS:
        if name in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing wave structure/empty.nbt")
    if not (PACK / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append("missing wave gametest/structure/empty.nbt")
    r0_hash = WAVE / "r0_disposition_sha256.txt"
    if not r0_hash.is_file() or r0_hash.read_text(encoding="utf-8").strip() != _sha256(R0):
        errors.append("R0 disposition ledger was modified")
    baseline_hash = WAVE / "baseline_ledger_sha256.txt"
    if (
        not baseline_hash.is_file()
        or baseline_hash.read_text(encoding="utf-8").strip() != _sha256(LEDGER)
    ):
        errors.append("baseline identity_resolution_ledger was modified")
    errors.extend(_recipe_dummy_leftovers(committed))
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
