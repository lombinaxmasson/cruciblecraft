#!/usr/bin/env python3
"""Fold leftover connector CatalogNamedItem dummies onto live BlockItems.

Smelter and bath catalogs both still carried dummy ids after later overlays
opened the matching CableBlock / PipeBlock hosts. Does not invent hosts,
rewrite R0, or claim unique-active (machines/distillation-tower stays).
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

from tools import catalog_modern_ids as modern
from tools import census_common as census
from tools import io_common as io

SLUG = "content/gt6-connector-live-host-dummy-fold"
STATUS = "CONNECTOR_LIVE_HOST_DUMMY_FOLD_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-connector-live-host-dummy-fold"
BASELINE = census.TOOLS / "waves" / "content" / "gt6-pipe-cable-baseline"
LEDGER = BASELINE / "identity_resolution_ledger.json"
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
DATA = census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
DATA_SMELTER = DATA / "smelter_mte_identity_catalog.json"
TOOLS_SMELTER = census.TOOLS / "smelter_mte_identity_catalog.json"
DATA_BATH = DATA / "bath_mte_identity_catalog.json"
TOOLS_BATH = census.TOOLS / "bath_mte_identity_catalog.json"
MATERIAL_GATE = DATA / "material_registration_gate.json"
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
BATH_JAVA = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "item"
    / "BathMteIdentityCatalog.java"
)
RECIPE_ROOTS = (
    census.ROOT / "src" / "recipe_generated",
    census.ROOT / "src" / "recipe_support_generated",
    census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "recipe",
)
LOOM_WAVE = census.TOOLS / "waves" / "machines" / "loom"
LOOM_IMPORT = LOOM_WAVE / "recipe_import.json"
LOOM_LOCK_NOTE = (
    "live compile for machines/loom; 477 runtime-registered exact rows; "
    "166 unmapped MTE/plant_gt_fiber rows and 691 shadowed input signatures "
    "explicitly_blocked; not player_complete"
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
OVERLAY_PATH = WAVE / "fold_overlay.json"
CANONICAL = {
    "hsla_steel": "hslasteel",
    "treated_wood": "wood_treated",
    "galvanized_steel": "steel_galvanized",
}
CONNECTOR_ROOTS = {
    "electric_wire",
    "fluid_pipe_tile",
    "item_pipe_tile",
    "quadruple",
    "nonuple",
}
CABLE_FORMS = {
    1: "cable",
    2: "double_cable",
    4: "quadruple_cable",
    8: "octuple_cable",
    12: "dodecuple_cable",
}
WIRE_FORMS = {
    1: "wire",
    2: "double_wire",
    3: "triple_wire",
    4: "quadruple_wire",
    5: "quintuple_wire",
    6: "sextuple_wire",
    7: "septuple_wire",
    8: "octuple_wire",
    9: "nonuple_wire",
    10: "decuple_wire",
    11: "undecuple_wire",
    12: "dodecuple_wire",
    13: "tredecuple_wire",
    14: "tetradecuple_wire",
    15: "pentadecuple_wire",
    16: "hexadecuple_wire",
}
REASON = "folded onto live connector host"
EXPECTED_FOLDS = 53
EXPECTED_KEEP = 49
EXPECTED_SMELTER_NEW_ITEMS = 1
EXPECTED_BATH_NEW_ITEMS = 48


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


def parse_dummy(path: str) -> tuple[str, str] | None:
    dummy = _strip_ns(path)
    if "/" not in dummy:
        return None
    root, name = dummy.split("/", 1)
    if root not in CONNECTOR_ROOTS:
        return None
    if root == "electric_wire":
        match = re.fullmatch(r"(\d+)x_(.+)_(wire|cable)", name)
        if not match:
            return None
        gauge = int(match.group(1))
        material = match.group(2)
        kind = match.group(3)
        form = CABLE_FORMS.get(gauge) if kind == "cable" else WIRE_FORMS.get(gauge)
        if not form:
            return None
        return material, form
    if root in {"quadruple", "nonuple"}:
        if not name.endswith("_fluid_pipe"):
            return None
        return name[: -len("_fluid_pipe")], f"{root}_fluid_pipe"
    if root == "fluid_pipe_tile":
        for prefix, form in (
            ("quadruple_", "quadruple_fluid_pipe"),
            ("nonuple_", "nonuple_fluid_pipe"),
        ):
            if name.startswith(prefix) and name.endswith("_fluid_pipe"):
                return name[len(prefix) : -len("_fluid_pipe")], form
        return None
    return None


def _gated_forms() -> dict[str, set[str]]:
    gate = census.load_json(MATERIAL_GATE)
    return {
        str(material): set(forms or [])
        for material, forms in (gate.get("materials") or {}).items()
    }


def _canonical_material(recorded: str, gated: dict[str, set[str]]) -> str | None:
    if recorded in CANONICAL:
        return CANONICAL[recorded]
    if recorded in gated:
        return recorded
    compact_live = {_compact(material): material for material in gated}
    return compact_live.get(_compact(recorded))


def _catalog_identities(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []
    return list(census.load_json(path).get("identities") or [])


def build_overlay() -> dict[str, Any]:
    gated = _gated_forms()
    live = modern.live_host_paths()
    folds: list[dict[str, Any]] = []
    keep: list[dict[str, Any]] = []
    for catalog, path in (
        ("smelter_mte_identity_catalog.json", DATA_SMELTER),
        ("bath_mte_identity_catalog.json", DATA_BATH),
    ):
        for identity in _catalog_identities(path):
            if identity.get("registry_kind") != "item":
                continue
            dummy = str(identity.get("registry_path") or "")
            parsed = parse_dummy(dummy)
            base = {
                "catalog": catalog,
                "meta": int(identity["meta"]),
                "dummy_path": dummy,
                "english_name": identity.get("english_name"),
            }
            if parsed is None:
                keep.append(
                    {
                        **base,
                        "disposition": "keep_distinct",
                        "reason": "Not a leftover connector dummy.",
                    }
                )
                continue
            recorded, form = parsed
            canonical = _canonical_material(recorded, gated)
            live_path = f"{canonical}/{form}" if canonical else ""
            if (
                not canonical
                or form not in gated.get(canonical, set())
                or live_path not in live
            ):
                keep.append(
                    {
                        **base,
                        "recorded_material": recorded,
                        "canonical_material": canonical,
                        "cc_form": form,
                        "disposition": "keep_distinct",
                        "reason": "No gated live BlockItem for this GT6 object.",
                    }
                )
                continue
            folds.append(
                {
                    **base,
                    "recorded_material": recorded,
                    "canonical_material": canonical,
                    "cc_form": form,
                    "live_block": f"cruciblecraft:{live_path}",
                    "disposition": "fold_live_block",
                    "collision_reason": REASON,
                }
            )
    folds.sort(key=lambda row: (str(row["catalog"]), int(row["meta"])))
    keep.sort(key=lambda row: (str(row["catalog"]), int(row["meta"])))
    return {
        "schema": "gt6-connector-live-host-dummy-fold-v1",
        "capability_slug": SLUG,
        "source_revision": GT6_REVISION,
        "note": (
            "Fold leftover smelter/bath connector dummies onto already-gated "
            "CableBlock / PipeBlock hosts. Does not invent hosts or rewrite R0."
        ),
        "counts": {
            "fold_live_block": len(folds),
            "keep_distinct": len(keep),
            "smelter": sum(
                1 for row in folds if row["catalog"].startswith("smelter")
            ),
            "bath": sum(1 for row in folds if row["catalog"].startswith("bath")),
        },
        "rows": folds,
        "keep_distinct": keep,
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


def current_gap(overlay: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema": "gt6-connector-live-host-dummy-fold-gap-v1",
        "capability_slug": SLUG,
        "folded": overlay["counts"],
        "still_dummy": overlay.get("keep_distinct") or [],
        "close_target": "runtime_ready",
        "note": (
            "Loot crate and decorative panels stay dummy. Connector duals with "
            "live hosts are withdrawn. Distillation-tower unique-active is untouched."
        ),
    }


def _patch_catalogs(fold_by_meta: dict[int, dict[str, Any]]) -> int:
    changed = 0
    for path, sync_count in (
        (DATA_SMELTER, True),
        (TOOLS_SMELTER, True),
        (DATA_BATH, False),
        (TOOLS_BATH, False),
    ):
        if not path.is_file():
            continue
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
        if sync_count:
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
        if row.get("registry_path") == live:
            continue
        row["registry_path"] = live
        row["runtime_id"] = f"cruciblecraft:{live}"
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
    seen: set[tuple[str, str]] = set()
    for row in overlay.get("rows") or []:
        dummy = str(row.get("dummy_path") or "")
        live = _strip_ns(str(row.get("live_block") or ""))
        if not dummy or not live or dummy == live:
            continue
        pair = (f"cruciblecraft:{dummy}", f"cruciblecraft:{live}")
        if pair in seen:
            continue
        seen.add(pair)
        pairs.append(pair)
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


def _refresh_closed_machine_imports() -> dict[str, str]:
    from tools.recipe_bulk import source_import
    from tools.waves.prep import machine_prep_common as common

    if not LOOM_IMPORT.is_file():
        raise FileNotFoundError(f"missing {census.relative(LOOM_IMPORT)}")
    if not MELTER_IMPORT.is_file():
        raise FileNotFoundError(f"missing {census.relative(MELTER_IMPORT)}")
    source_import.write_import(LOOM_IMPORT)
    common.freeze_lock(LOOM_WAVE, "machines/loom", LOOM_LOCK_NOTE)
    source_import.write_import(MELTER_IMPORT)
    common.freeze_lock(MELTER_WAVE, "machines/melter", MELTER_LOCK_NOTE)
    return {
        "loom": census.relative(LOOM_WAVE / "source.json"),
        "melter": census.relative(MELTER_WAVE / "source.json"),
    }


def _patch_bath_java(count: int) -> None:
    text = BATH_JAVA.read_text(encoding="utf-8")
    updated = re.sub(
        r"public static final int NEW_ITEM_COUNT = \d+;",
        f"public static final int NEW_ITEM_COUNT = {count};",
        text,
        count=1,
    )
    if updated != text:
        BATH_JAVA.write_text(updated, encoding="utf-8")


def write() -> dict[str, Any]:
    WAVE.mkdir(parents=True, exist_ok=True)
    if OVERLAY_PATH.is_file():
        overlay = census.load_json(OVERLAY_PATH)
    else:
        overlay = build_overlay()
        _write_json(OVERLAY_PATH, overlay)
        _write_json(WAVE / "current_gap.json", current_gap(overlay))
    counts = overlay["counts"]
    if int(counts["fold_live_block"]) != EXPECTED_FOLDS:
        raise ValueError(
            f"fold_live_block {counts['fold_live_block']} != {EXPECTED_FOLDS}"
        )
    if int(counts["keep_distinct"]) != EXPECTED_KEEP:
        raise ValueError(
            f"keep_distinct {counts['keep_distinct']} != {EXPECTED_KEEP}"
        )
    live_hosts = modern.live_host_paths()
    for row in overlay["rows"]:
        live = _strip_ns(str(row["live_block"]))
        if live not in live_hosts:
            raise ValueError(f"live host {live} is not registered")
    _write_json(WAVE / "current_gap.json", current_gap(overlay))
    fold_by_meta = {int(row["meta"]): row for row in overlay["rows"]}
    folded = _patch_catalogs(fold_by_meta)
    mapped = _patch_modern_map(fold_by_meta)
    modern.rewrite_catalog_item_tags()
    remapped = _remap_recipe_ids(overlay)
    imports = _refresh_closed_machine_imports()
    withdrawn = _withdraw_dummy_models(
        {str(row["dummy_path"]) for row in overlay["rows"]}
    )
    _patch_bath_java(EXPECTED_BATH_NEW_ITEMS)
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
        "machine_imports": imports,
        "withdrawn_models": withdrawn,
        "fold_live_block": len(fold_by_meta),
    }


def check() -> list[str]:
    errors: list[str] = []
    if not OVERLAY_PATH.is_file():
        return [f"missing {census.relative(OVERLAY_PATH)}"]
    committed = census.load_json(OVERLAY_PATH)
    leftover = build_overlay()
    if int(leftover["counts"]["fold_live_block"]) != 0:
        errors.append(
            "leftover connector dummy still has a live host: "
            + str(leftover["counts"]["fold_live_block"])
        )
    if int(leftover["counts"]["keep_distinct"]) != EXPECTED_KEEP:
        errors.append(
            f"keep_distinct leftover {leftover['counts']['keep_distinct']} "
            f"!= {EXPECTED_KEEP}"
        )
    counts = committed.get("counts") or {}
    if int(counts.get("fold_live_block") or 0) != EXPECTED_FOLDS:
        errors.append(
            f"fold_live_block {counts.get('fold_live_block')} != {EXPECTED_FOLDS}"
        )
    if int(counts.get("keep_distinct") or 0) != EXPECTED_KEEP:
        errors.append(
            f"keep_distinct {counts.get('keep_distinct')} != {EXPECTED_KEEP}"
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
    catalog_specs = (
        (DATA_SMELTER, EXPECTED_SMELTER_NEW_ITEMS, True),
        (TOOLS_SMELTER, EXPECTED_SMELTER_NEW_ITEMS, True),
        (DATA_BATH, EXPECTED_BATH_NEW_ITEMS, False),
        (TOOLS_BATH, EXPECTED_BATH_NEW_ITEMS, False),
    )
    for path, expected_new, has_count in catalog_specs:
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
            continue
        catalog = census.load_json(path)
        by_meta = {int(row["meta"]): row for row in catalog.get("identities") or []}
        created = [
            row
            for row in catalog.get("identities") or []
            if row.get("registry_kind") == "item"
        ]
        if len(created) != expected_new:
            errors.append(
                f"{census.relative(path)} dummy count {len(created)} != {expected_new}"
            )
        if has_count and int(catalog.get("new_item_count") or 0) != len(created):
            errors.append(
                f"{census.relative(path)} new_item_count "
                f"{catalog.get('new_item_count')} != {len(created)}"
            )
        for meta, fold in fold_by_meta.items():
            identity = by_meta.get(meta)
            if identity is None:
                if str(fold["catalog"]) == path.name:
                    errors.append(f"{census.relative(path)} missing meta {meta}")
                continue
            if identity.get("registry_kind") != "item" and str(
                fold["catalog"]
            ) != path.name:
                live_path = _strip_ns(str(fold["live_block"]))
                if identity.get("registry_path") == live_path:
                    continue
            live_path = _strip_ns(str(fold["live_block"]))
            if live_path not in live_hosts:
                errors.append(f"live host {live_path} is not registered")
            if identity.get("registry_kind") != "existing_item":
                if str(fold["catalog"]) == path.name:
                    errors.append(f"{path.name} meta {meta} still dummy item")
                continue
            if (
                str(fold["catalog"]) == path.name
                and identity.get("registry_path") != live_path
            ):
                errors.append(
                    f"{path.name} meta {meta} catalog "
                    f"{identity.get('registry_path')} != {live_path}"
                )
        for identity in created:
            dummy = str(identity.get("registry_path") or "")
            parsed = parse_dummy(dummy)
            if parsed is None:
                continue
            recorded, form = parsed
            canonical = _canonical_material(recorded, _gated_forms())
            live_path = f"{canonical}/{form}" if canonical else ""
            if canonical and form in _gated_forms().get(canonical, set()) and live_path in live_hosts:
                errors.append(
                    f"{census.relative(path)} leftover dual dummy {dummy}"
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
        if dummy.is_file():
            errors.append(f"folded dummy model still present {fold['dummy_path']}")
    if BATH_JAVA.is_file():
        java = BATH_JAVA.read_text(encoding="utf-8")
        if f"NEW_ITEM_COUNT = {EXPECTED_BATH_NEW_ITEMS};" not in java:
            errors.append("BathMteIdentityCatalog NEW_ITEM_COUNT drifted")
    r0_hash = WAVE / "r0_disposition_sha256.txt"
    if not r0_hash.is_file() or r0_hash.read_text(encoding="utf-8").strip() != _sha256(
        R0
    ):
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
    args = parser.parse_args(argv)
    if args.write:
        write()
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
