#!/usr/bin/env python3
"""Fold HSLA connector dummies onto live hslasteel BlockItems.

Does not rewrite the closed baseline identity ledger or the R0 disposition
ledger. Closed fluid/EU execution subsets stay historical; this overlay is the
current catalog projection.
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

SLUG = "content/gt6-connector-alias-repair"
STATUS = "CONNECTOR_ALIAS_REPAIR_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-connector-alias-repair"
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
EU_SUBSET = (
    census.TOOLS / "waves" / "content" / "gt6-eu-wire-cable-runtime" / "execution_subset.json"
)
ITEM_SUBSET = (
    census.TOOLS / "waves" / "content" / "gt6-item-pipe-runtime" / "execution_subset.json"
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
GAME_TESTS = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "ConnectorAliasRepairGameTests.java"
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
    / "cruciblecraft_wave_content_gt6_connector_alias_repair"
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
OVERLAY_PATH = WAVE / "alias_overlay.json"
CANONICAL = {"hsla_steel": "hslasteel"}
EXPECTED_FOLDS = 9
EXPECTED_FLUID_FOLDS = 5
EXPECTED_EU_FOLDS = 4
EXPECTED_TESTS = [
    "hslaSteelFiveGaugePipesFoldOntoLiveHosts",
    "hslaSteelMappedWiresFoldOntoLiveHosts",
    "hslaSteelUngatedGaugesStayDummy",
]
FLUID_REASON = "folded onto live fluid pipe"
EU_REASON = "folded onto live eu cable"
FIVE_GAUGES = {
    "tiny_fluid_pipe",
    "small_fluid_pipe",
    "fluid_pipe",
    "large_fluid_pipe",
    "huge_fluid_pipe",
}
WIRE_FORMS = {
    "wire",
    "double_wire",
    "triple_wire",
    "quadruple_wire",
    "quintuple_wire",
    "sextuple_wire",
    "octuple_wire",
    "dodecuple_wire",
    "hexadecuple_wire",
    "cable",
    "double_cable",
    "quadruple_cable",
    "octuple_cable",
    "dodecuple_cable",
}


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


def _gated_forms() -> dict[str, set[str]]:
    gate = census.load_json(MATERIAL_GATE)
    return {
        str(material): set(forms or [])
        for material, forms in (gate.get("materials") or {}).items()
    }


def _subset_rows(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []
    return list(census.load_json(path).get("rows") or [])


def _collision_reason(form: str) -> str:
    if form in FIVE_GAUGES:
        return FLUID_REASON
    return EU_REASON


def build_overlay() -> dict[str, Any]:
    gated = _gated_forms()
    live = modern.live_host_paths()
    rows: list[dict[str, Any]] = []
    for path, domain in ((FLUID_SUBSET, "fluid"), (EU_SUBSET, "eu")):
        for row in _subset_rows(path):
            material = str(row.get("material") or "")
            form = str(row.get("cc_form") or "")
            dummy = str(row.get("dummy_path") or "")
            canonical = CANONICAL.get(material)
            if not canonical or not form or not dummy:
                continue
            if form not in gated.get(canonical, set()):
                continue
            live_path = f"{canonical}/{form}"
            if live_path not in live:
                continue
            rows.append(
                {
                    "meta": int(row["meta"]),
                    "domain": domain,
                    "recorded_material": material,
                    "canonical_material": canonical,
                    "cc_form": form,
                    "dummy_path": dummy,
                    "live_block": f"cruciblecraft:{live_path}",
                    "collision_reason": _collision_reason(form),
                    "baseline_disposition": row.get("disposition"),
                }
            )
    rows.sort(key=lambda row: int(row["meta"]))
    fluid = [row for row in rows if row["domain"] == "fluid"]
    eu = [row for row in rows if row["domain"] == "eu"]
    return {
        "schema": "gt6-connector-alias-overlay-v1",
        "capability_slug": SLUG,
        "note": (
            "Canonical identity leak: baseline _cc_material slugified "
            "MT.HSLA source name HSLA-Steel to hsla_steel while the live "
            "material id is hslasteel. Overlay folds exact live BlockItems. "
            "Do not rewrite the closed baseline ledger or R0."
        ),
        "canonical": dict(CANONICAL),
        "counts": {
            "fold_live_block": len(rows),
            "fluid": len(fluid),
            "eu": len(eu),
        },
        "rows": rows,
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
        "generated_by": "content/gt6-connector-alias-repair implementation",
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


def _remaining_canonical_leaks(overlay_metas: set[int]) -> list[dict[str, Any]]:
    gated = _gated_forms()
    live = modern.live_host_paths()
    compact_live = {_compact(material): material for material in gated}
    leaks: list[dict[str, Any]] = []
    for path, domain in (
        (FLUID_SUBSET, "fluid"),
        (ITEM_SUBSET, "item"),
        (EU_SUBSET, "eu"),
    ):
        for row in _subset_rows(path):
            meta = int(row["meta"])
            if meta in overlay_metas:
                continue
            if row.get("disposition") not in {"keep_distinct", "upgrade_live_item"}:
                continue
            material = str(row.get("material") or "")
            form = str(row.get("cc_form") or "")
            if not material or not form:
                continue
            canonical = compact_live.get(_compact(material))
            if not canonical or canonical == material:
                continue
            live_path = f"{canonical}/{form}"
            if form not in gated.get(canonical, set()) or live_path not in live:
                continue
            leaks.append(
                {
                    "meta": meta,
                    "domain": domain,
                    "recorded_material": material,
                    "canonical_material": canonical,
                    "cc_form": form,
                    "dummy_path": row.get("dummy_path"),
                    "live_block": f"cruciblecraft:{live_path}",
                }
            )
    leaks.sort(key=lambda row: int(row["meta"]))
    return leaks


def current_gap(overlay: dict[str, Any]) -> dict[str, Any]:
    overlay_metas = {int(row["meta"]) for row in overlay.get("rows") or []}
    remaining_hsla: list[dict[str, Any]] = []
    for path, domain in ((FLUID_SUBSET, "fluid"), (EU_SUBSET, "eu")):
        for row in _subset_rows(path):
            if str(row.get("material") or "") != "hsla_steel":
                continue
            if int(row["meta"]) in overlay_metas:
                continue
            remaining_hsla.append(
                {
                    "meta": int(row["meta"]),
                    "domain": domain,
                    "cc_form": row.get("cc_form"),
                    "dummy_path": row.get("dummy_path"),
                    "disposition": row.get("disposition"),
                    "reason": (
                        "No live hslasteel BlockItem for this GT6 gauge. "
                        "Keep the modern dummy id."
                    ),
                }
            )
    remaining_hsla.sort(key=lambda row: int(row["meta"]))
    return {
        "schema": "gt6-connector-alias-repair-gap-v1",
        "capability_slug": SLUG,
        "folded": overlay["counts"],
        "hsla_steel_still_dummy": remaining_hsla,
        "other_canonical_leaks": _remaining_canonical_leaks(overlay_metas),
        "blocked": [
            "pipeQuadruple / pipeNonuple BlockItem",
            "restrictive item pipes",
            "wireGt07/09/10/11/13/14/15 prefixes",
            "insulated redstone 27006/27056/27506",
            "plasma/magic destroy/replace, flammable, contactDamage",
            "connector survival obtain grids",
        ],
        "close_target": "runtime_ready",
        "note": (
            "Baseline identity_resolution_ledger and R0 stay frozen. "
            "Insulated redstone stays blocked until laminator plate/foil "
            "recipes parse. Do not alias insulated redstone onto */cable."
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
        reason = str(fold.get("collision_reason") or FLUID_REASON)
        if row.get("registry_path") != live:
            row["registry_path"] = live
            row["runtime_id"] = f"cruciblecraft:{live}"
            row["collision_reason"] = reason
            changed += 1
        elif row.get("collision_reason") != reason:
            row["collision_reason"] = reason
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


def _refresh_loom_source_import() -> dict[str, str]:
    """Replay loom Source Pack so folded dummy IDs become live BlockItems.

    Catalog fold changes compile_row runtime_ids. Closed loom check_import
    regenerates source/lock from the dump, so those documents must follow
    the live hslasteel hosts. Does not rewrite loom topology or reopen the
    loom card.
    """
    from tools.recipe_bulk import source_import
    from tools.waves.prep import machine_prep_common as common

    if not LOOM_IMPORT.is_file():
        raise FileNotFoundError(f"missing {census.relative(LOOM_IMPORT)}")
    wrote = source_import.write_import(LOOM_IMPORT)
    common.freeze_lock(LOOM_WAVE, "machines/loom", LOOM_LOCK_NOTE)
    return {
        "import_slug": str(wrote.get("import_slug") or "machines/loom"),
        "source": census.relative(LOOM_WAVE / "source.json"),
        "lock_candidate": census.relative(LOOM_WAVE / "lock_candidate.json"),
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
    overlay = build_overlay()
    counts = overlay["counts"]
    if int(counts["fold_live_block"]) != EXPECTED_FOLDS:
        raise ValueError(
            f"fold_live_block {counts['fold_live_block']} != {EXPECTED_FOLDS}"
        )
    if int(counts["fluid"]) != EXPECTED_FLUID_FOLDS:
        raise ValueError(f"fluid folds {counts['fluid']} != {EXPECTED_FLUID_FOLDS}")
    if int(counts["eu"]) != EXPECTED_EU_FOLDS:
        raise ValueError(f"eu folds {counts['eu']} != {EXPECTED_EU_FOLDS}")
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
                "HSLA connector dummies folded onto live hslasteel BlockItems; "
                "not player_complete"
            )
        },
    )
    folded = _patch_catalogs(fold_by_meta)
    mapped = _patch_modern_map(fold_by_meta)
    modern.rewrite_catalog_item_tags()
    remapped = _remap_recipe_ids(overlay)
    loom_import = _refresh_loom_source_import()
    withdrawn = _withdraw_dummy_models(folded_dummy_paths())
    _copy_empty_nbt()
    (WAVE / "r0_disposition_sha256.txt").write_text(
        _sha256(R0) + "\n", encoding="utf-8"
    )
    (WAVE / "baseline_ledger_sha256.txt").write_text(
        _sha256(LEDGER) + "\n", encoding="utf-8"
    )
    return {
        "folded_identities": folded,
        "mapped_rows": mapped,
        "recipe_files": remapped,
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
        errors.append(f"alias_overlay.json drifted: {drift}")
    counts = committed.get("counts") or {}
    if int(counts.get("fold_live_block") or 0) != EXPECTED_FOLDS:
        errors.append(
            f"fold_live_block {counts.get('fold_live_block')} != {EXPECTED_FOLDS}"
        )
    if int(counts.get("fluid") or 0) != EXPECTED_FLUID_FOLDS:
        errors.append(f"fluid folds {counts.get('fluid')} != {EXPECTED_FLUID_FOLDS}")
    if int(counts.get("eu") or 0) != EXPECTED_EU_FOLDS:
        errors.append(f"eu folds {counts.get('eu')} != {EXPECTED_EU_FOLDS}")
    gap_path = WAVE / "current_gap.json"
    if not gap_path.is_file():
        errors.append("missing current_gap.json")
    else:
        gap_drift = census.first_json_diff(current_gap(committed), census.load_json(gap_path))
        if gap_drift:
            errors.append(f"current_gap.json drifted: {gap_drift}")
    fold_by_meta = {int(row["meta"]): row for row in committed.get("rows") or []}
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
        if row.get("collision_reason") != fold.get("collision_reason"):
            errors.append(
                f"modern map meta {meta} collision_reason "
                f"{row.get('collision_reason')}"
            )
        dummy = ITEM_MODELS / f"{fold['dummy_path']}.json"
        if dummy.is_file():
            errors.append(f"folded dummy model still present {fold['dummy_path']}")
    leftover = _recipe_remap_pairs(committed)
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
    loom_source = LOOM_WAVE / "source.json"
    if loom_source.is_file():
        loom_text = loom_source.read_text(encoding="utf-8")
        for old, _new in leftover:
            if old in loom_text:
                errors.append(f"loom source.json still names folded dummy {old}")
                break
    tests = GAME_TESTS.read_text(encoding="utf-8") if GAME_TESTS.is_file() else ""
    for name in EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8") if CORE_TESTS.is_file() else ""
    for name in EXPECTED_TESTS:
        if f"void {name}" in core:
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
