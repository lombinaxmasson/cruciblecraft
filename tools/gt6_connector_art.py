#!/usr/bin/env python3
"""GT6 connector art child. Copy local gregtech6_w iconsets; no invented cable.png."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools import gt6_connector_alias_repair as alias_repair
from tools import gt6_connector_live_host_dummy_fold as live_host_fold
from tools import gt6_eu_missing_wire_gauges_runtime as missing_gauges
from tools import gt6_fluid_combo_pipe_runtime as combo_pipe
from tools import gt6_restrictive_item_pipe_runtime as restrictive_pipe
from tools import io_common as io

SLUG = "content/gt6-connector-art"
STATUS = "CONNECTOR_ART_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-connector-art"
GT6_W = (
    census.ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
)
GT6_TEX = Path("src/main/resources/assets/gregtech/textures/blocks")
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
ITEM_MODELS = ASSETS / "models" / "item"
DEST_ROOT = "assets/cruciblecraft/textures/block/gt6_import"
REDSTONE_WIRE = (
    "assets/cruciblecraft/textures/block/gt6_import/materialicons/copper/wire.png"
)
REDSTONE_OVERLAY = (
    "assets/cruciblecraft/textures/block/gt6_import/materialicons/copper/"
    "wire_overlay.png"
)
COPPER = "materialicons/copper"
ICONSETS = "iconsets"
FLUID_SUBSET = (
    census.TOOLS / "waves" / "content" / "gt6-fluid-pipe-runtime" / "execution_subset.json"
)
ITEM_SUBSET = (
    census.TOOLS / "waves" / "content" / "gt6-item-pipe-runtime" / "execution_subset.json"
)
EU_SUBSET = (
    census.TOOLS / "waves" / "content" / "gt6-eu-wire-cable-runtime" / "execution_subset.json"
)
LEDGER = (
    census.TOOLS
    / "waves"
    / "content"
    / "gt6-pipe-cable-baseline"
    / "identity_resolution_ledger.json"
)
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
EMPTY_SRC = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_item_pipe_runtime"
    / "structure"
    / "empty.nbt"
)
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_connector_art"
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
    / "ConnectorArtGameTests.java"
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
STATE_JAVA = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "datagen"
    / "ModBlockStateProvider.java"
)
EXPECTED_TESTS = [
    "connectorArtManifestResolvesLocalGt6",
    "foldedRowsCopyZeroPng",
    "inPlaceRowsHaveNoIronIngotModel",
]
MANIFESTS = {
    "fluid": ASSETS / "gt6_fluid_pipe_art_manifest.json",
    "item": ASSETS / "gt6_item_pipe_art_manifest.json",
    "eu": ASSETS / "gt6_eu_wire_cable_art_manifest.json",
}
PIPE_FILES = [
    "pipetiny",
    "pipetiny_overlay",
    "pipesmall",
    "pipesmall_overlay",
    "pipemedium",
    "pipemedium_overlay",
    "pipelarge",
    "pipelarge_overlay",
    "pipehuge",
    "pipehuge_overlay",
    "pipeside",
    "pipeside_overlay",
    "pipequadruple",
    "pipequadruple_overlay",
    "pipenonuple",
    "pipenonuple_overlay",
]
INSULATION_FILES = [
    "insulation_tiny",
    "insulation_small",
    "insulation_medium",
    "insulation_large",
    "insulation_huge",
    "insulation_full",
]


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _gt6_file(relative: str) -> Path:
    return GT6_W / relative.replace("\\", "/")


def _dest_file(destination: str) -> Path:
    return census.ROOT / "src" / "main" / "resources" / destination.replace("\\", "/")


def _import_row(
    *,
    gt6_source: str,
    destination: str,
    runtime_ids: list[str],
    overlay: str | None = None,
    copy: bool = True,
) -> dict[str, Any]:
    source_path = _gt6_file(gt6_source)
    dest_path = _dest_file(destination)
    digest = _sha256(source_path)
    copied = False
    if copy:
        dest_path.parent.mkdir(parents=True, exist_ok=True)
        if not dest_path.is_file() or dest_path.read_bytes() != source_path.read_bytes():
            shutil.copyfile(source_path, dest_path)
            copied = True
        else:
            copied = False
    elif dest_path.is_file():
        if _sha256(dest_path) != digest:
            raise ValueError(f"{destination} drifted from {gt6_source}")
    else:
        raise FileNotFoundError(destination)
    row: dict[str, Any] = {
        "source": "gt6_referencable_port_code/gregtech6_w",
        "gt6_source": gt6_source,
        "destination": destination,
        "sha256": digest,
        "copied": copied,
        "runtime_ids": runtime_ids,
    }
    if overlay:
        row["overlay"] = overlay
    return row


def _copper_src(name: str) -> str:
    return f"{GT6_TEX.as_posix()}/{COPPER}/{name}.png"


def _copper_dest(name: str) -> str:
    return f"{DEST_ROOT}/{COPPER}/{name}.png"


def _icon_src(name: str) -> str:
    return f"{GT6_TEX.as_posix()}/{ICONSETS}/{name}.png"


def _icon_dest(name: str) -> str:
    return f"{DEST_ROOT}/{ICONSETS}/{name}.png"


def _subset_rows(path: Path, disposition: str) -> list[dict[str, Any]]:
    return [
        row
        for row in census.load_json(path).get("rows") or []
        if row.get("disposition") == disposition
    ]


def _runtime_ids(rows: list[dict[str, Any]]) -> list[str]:
    ids: list[str] = []
    seen: set[str] = set()
    for row in rows:
        live = str(row.get("live_block") or "")
        dummy = str(row.get("dummy_path") or "")
        for value in (live, f"cruciblecraft:{dummy}" if dummy else ""):
            if value and value not in seen:
                seen.add(value)
                ids.append(value)
    return ids


def _dummy_model_payload(dummy_path: str) -> dict[str, Any]:
    name = dummy_path.rsplit("/", 1)[-1]
    if "restrictive" in name:
        layer0 = f"cruciblecraft:block/gt6_import/{ICONSETS}/pipe_restrictor"
        return {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": layer0},
        }
    if "quadruple" in name:
        base = "pipequadruple"
    elif "nonuple" in name:
        base = "pipenonuple"
    elif dummy_path.startswith("electric_wire/") and name.endswith("_cable"):
        return {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": "cruciblecraft:block/gt6_import/materialicons/copper/wire",
                "layer1": "cruciblecraft:block/gt6_import/iconsets/insulation_tiny",
            },
        }
    elif dummy_path.startswith("electric_wire/"):
        return {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": "cruciblecraft:block/gt6_import/materialicons/copper/wire",
                "layer1": (
                    "cruciblecraft:block/gt6_import/materialicons/copper/"
                    "wire_overlay"
                ),
            },
        }
    else:
        base = "pipemedium"
    return {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"cruciblecraft:block/gt6_import/{COPPER}/{base}",
            "layer1": f"cruciblecraft:block/gt6_import/{COPPER}/{base}_overlay",
        },
    }


def _write_dummy_model(dummy_path: str) -> None:
    path = ITEM_MODELS / f"{dummy_path}.json"
    _write_json(path, _dummy_model_payload(dummy_path))


def _cleanup_folded_models() -> int:
    removed = 0
    dummies = {
        str(row.get("dummy_path") or "")
        for subset in (FLUID_SUBSET, ITEM_SUBSET, EU_SUBSET)
        for row in _subset_rows(subset, "fold_live_block")
    }
    dummies.update(alias_repair.folded_dummy_paths())
    dummies.update(combo_pipe.folded_dummy_paths())
    dummies.update(restrictive_pipe.folded_dummy_paths())
    dummies.update(missing_gauges.folded_dummy_paths())
    dummies.update(live_host_fold.folded_dummy_paths())
    for dummy in dummies:
        if not dummy:
            continue
        path = ITEM_MODELS / f"{dummy}.json"
        if path.is_file():
            path.unlink()
            removed += 1
    return removed


def _rewrite_keep_models() -> int:
    written = 0
    repaired = (
        alias_repair.folded_dummy_paths()
        | combo_pipe.folded_dummy_paths()
        | restrictive_pipe.folded_dummy_paths()
        | missing_gauges.folded_dummy_paths()
        | live_host_fold.folded_dummy_paths()
    )
    for subset in (FLUID_SUBSET, ITEM_SUBSET, EU_SUBSET):
        for disposition in ("keep_distinct", "upgrade_live_item"):
            for row in _subset_rows(subset, disposition):
                dummy = str(row.get("dummy_path") or "")
                if not dummy or dummy in repaired:
                    continue
                _write_dummy_model(dummy)
                written += 1
    return written


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


def _fluid_manifest() -> dict[str, Any]:
    live = _subset_rows(FLUID_SUBSET, "fold_live_block")
    keep = _subset_rows(FLUID_SUBSET, "keep_distinct")
    shared = _runtime_ids(live + keep)
    imports = [
        _import_row(
            gt6_source=_copper_src(name),
            destination=_copper_dest(name),
            runtime_ids=shared,
            overlay=_copper_dest(name.replace("_overlay", "") + "_overlay")
            if not name.endswith("_overlay")
            else None,
        )
        for name in PIPE_FILES
    ]
    return {
        "domain": "fluid",
        "source_revision": GT6_REVISION,
        "note": "Shared copper iconset. Fold/already_shared rows copy 0 extra PNG.",
        "tint": {
            "core": "material",
            "overlay": "none"
        },
        "imports": imports,
    }


def _item_manifest() -> dict[str, Any]:
    live = _subset_rows(ITEM_SUBSET, "fold_live_block")
    keep = _subset_rows(ITEM_SUBSET, "keep_distinct")
    shared = _runtime_ids(live + keep)
    imports = []
    for name in (
        "pipemedium",
        "pipemedium_overlay",
        "pipelarge",
        "pipelarge_overlay",
        "pipehuge",
        "pipehuge_overlay",
    ):
        imports.append(
            _import_row(
                gt6_source=_copper_src(name),
                destination=_copper_dest(name),
                runtime_ids=shared,
                copy=False,
            )
        )
    imports.append(
        _import_row(
            gt6_source=_icon_src("pipe_restrictor"),
            destination=_icon_dest("pipe_restrictor"),
            runtime_ids=[
                f"cruciblecraft:{row['dummy_path']}"
                for row in keep
                if "restrictive" in str(row.get("dummy_path") or "")
            ],
        )
    )
    return {
        "domain": "item",
        "source_revision": GT6_REVISION,
        "note": "Reuses fluid copper pipe iconset. Restrictor is item-only. No extra fold PNG.",
        "tint": {
            "core": "material",
            "overlay": "none"
        },
        "imports": imports,
    }


def _eu_manifest() -> dict[str, Any]:
    live = _subset_rows(EU_SUBSET, "fold_live_block")
    keep = _subset_rows(EU_SUBSET, "keep_distinct")
    upgrades = _subset_rows(EU_SUBSET, "upgrade_live_item")
    shared = _runtime_ids(live + keep + upgrades)
    imports = [
        _import_row(
            gt6_source=_copper_src("wire"),
            destination=REDSTONE_WIRE,
            runtime_ids=shared,
            overlay=REDSTONE_OVERLAY,
            copy=False,
        ),
        _import_row(
            gt6_source=_copper_src("wire_overlay"),
            destination=REDSTONE_OVERLAY,
            runtime_ids=shared,
            copy=False,
        ),
    ]
    for name in INSULATION_FILES:
        imports.append(
            _import_row(
                gt6_source=_icon_src(name),
                destination=_icon_dest(name),
                runtime_ids=shared,
            )
        )
    return {
        "domain": "eu",
        "source_revision": GT6_REVISION,
        "note": (
            "GT6 has no dedicated cable texture. Cables are wire + insulation overlay. "
            "Redstone already owns copper/wire.png; this child does not recopy it."
        ),
        "tint": {
            "core": "material",
            "overlay": "none"
        },
        "imports": imports,
    }


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": f"{SLUG} implementation",
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


def runtime_notes() -> dict[str, Any]:
    return {
        "close_target": "runtime_ready",
        "obtain": "explicitly_blocked",
        "no_invented_cable_png": True,
        "redstone_wire_not_recopied": True,
        "insulated_redstone_deferred": True,
    }


def extra_png_for_folds() -> int:
    fold_ids = set(_runtime_ids(_subset_rows(FLUID_SUBSET, "fold_live_block")))
    fold_ids.update(_runtime_ids(_subset_rows(ITEM_SUBSET, "fold_live_block")))
    fold_ids.update(_runtime_ids(_subset_rows(EU_SUBSET, "fold_live_block")))
    extra = 0
    for path in MANIFESTS.values():
        document = census.load_json(path)
        for row in document.get("imports") or []:
            if row.get("copied") and set(row.get("runtime_ids") or []) <= fold_ids:
                extra += 1
    return extra


def write(unique_active: bool = True) -> dict[str, Any]:
    WAVE.mkdir(parents=True, exist_ok=True)
    fluid = _fluid_manifest()
    item = _item_manifest()
    eu = _eu_manifest()
    _write_json(MANIFESTS["fluid"], fluid)
    _write_json(MANIFESTS["item"], item)
    _write_json(MANIFESTS["eu"], eu)
    rewritten = _rewrite_keep_models()
    removed = _cleanup_folded_models()
    _copy_empty_nbt()
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(WAVE / "readiness.json", readiness(unique_active))
    _write_json(WAVE / "runtime_notes.json", runtime_notes())
    return {
        "rewritten_dummy_models": rewritten,
        "removed_folded_models": removed,
        "fold_extra_png": extra_png_for_folds(),
    }


def check() -> list[str]:
    errors: list[str] = []
    for key, path in MANIFESTS.items():
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
            continue
        document = census.load_json(path)
        if document.get("source_revision") != GT6_REVISION:
            errors.append(f"{key} source_revision drifted")
        for row in document.get("imports") or []:
            dest = _dest_file(str(row.get("destination") or ""))
            if not dest.is_file():
                errors.append(f"missing destination {row.get('destination')}")
                continue
            if _sha256(dest) != str(row.get("sha256") or ""):
                errors.append(f"{row.get('destination')} does not match recorded hash")
            if "cable.png" in str(row.get("gt6_source") or ""):
                errors.append("invented a GT6 cable.png source")
    extra = extra_png_for_folds()
    if extra:
        errors.append(f"fold rows copied {extra} extra PNG files")
    eu = census.load_json(MANIFESTS["eu"])
    for row in eu.get("imports") or []:
        dest = str(row.get("destination") or "")
        if dest in {REDSTONE_WIRE, REDSTONE_OVERLAY} and row.get("copied"):
            errors.append("EU recopy of redstone copper wire art")
    repaired = (
        alias_repair.folded_dummy_paths()
        | combo_pipe.folded_dummy_paths()
        | restrictive_pipe.folded_dummy_paths()
        | missing_gauges.folded_dummy_paths()
        | live_host_fold.folded_dummy_paths()
    )
    for subset in (FLUID_SUBSET, ITEM_SUBSET, EU_SUBSET):
        for row in _subset_rows(subset, "keep_distinct") + _subset_rows(
            subset, "upgrade_live_item"
        ):
            dummy = str(row.get("dummy_path") or "")
            if not dummy or dummy in repaired:
                continue
            model = ITEM_MODELS / f"{dummy}.json"
            if not model.is_file():
                errors.append(f"missing dummy model {dummy}")
                continue
            text = model.read_text(encoding="utf-8")
            if "iron_ingot" in text:
                errors.append(f"{dummy} still uses iron_ingot")
        for row in _subset_rows(subset, "fold_live_block"):
            dummy = str(row.get("dummy_path") or "")
            model = ITEM_MODELS / f"{dummy}.json"
            if dummy and model.is_file() and "iron_ingot" in model.read_text(
                encoding="utf-8"
            ):
                errors.append(f"folded dummy {dummy} still has an iron_ingot model")
    for dummy in repaired:
        model = ITEM_MODELS / f"{dummy}.json"
        if dummy and model.is_file() and "iron_ingot" in model.read_text(
            encoding="utf-8"
        ):
            errors.append(f"folded dummy {dummy} still has an iron_ingot model")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing wave structure/empty.nbt")
    if not (PACK / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append("missing wave gametest/structure/empty.nbt")
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
