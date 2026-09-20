#!/usr/bin/env python3
"""GT6 EU wire/cable runtime child. Execution subset only; no R0 rewrite."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import catalog_modern_ids as modern
from tools import census_common as census
from tools import gt6_connector_alias_repair as alias_repair
from tools import gt6_eu_missing_wire_gauges_runtime as missing_gauges
from tools import io_common as io

SLUG = "content/gt6-eu-wire-cable-runtime"
STATUS = "EU_WIRE_CABLE_RUNTIME_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-eu-wire-cable-runtime"
BASELINE = census.TOOLS / "waves" / "content" / "gt6-pipe-cable-baseline"
LEDGER = BASELINE / "identity_resolution_ledger.json"
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
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
BATH_DATA_CATALOG = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "bath_mte_identity_catalog.json"
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
CABLE_BLOCK = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "block"
    / "CableBlock.java"
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
    / "EuWireCableRuntimeGameTests.java"
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
    / "cruciblecraft_wave_content_gt6_eu_wire_cable_runtime"
)
EXPECTED_TESTS = [
    "cableLossAndOverloadPerSpecification",
    "electricWireGt01IsCableBlock",
    "higherWireGaugesArePlaceableOrExplicitlyUpgrade",
    "redstoneMaterialsAreNotElectricalConductors",
]
MAPPED_FORMS = {
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
DUMMY_PREFIX = "electric_wire"
COLLISION_REASON = "folded onto live eu cable"
IN_CATALOG = 404
FOLD_LIVE_BLOCK = 195
ALREADY_SHARED = 12
KEEP_DISTINCT = 177
UPGRADE_LIVE_ITEM = 20
EXPECTED_WIRES = 231
EXPECTED_CABLES = 116
MATERIAL_GATE = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)


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


def _live_host(row: dict[str, Any]) -> str | None:
    live = row.get("live_block")
    if live:
        return _strip_ns(str(live))
    material = row.get("material")
    form = row.get("cc_form")
    if material and form:
        return f"{material}/{form}"
    return None


def load_eu_rows() -> list[dict[str, Any]]:
    document = census.load_json(LEDGER)
    return [
        row
        for row in document.get("rows") or []
        if row.get("domain") == "eu" and row.get("in_catalog_1817")
    ]


def _is_dummy(path: str | None) -> bool:
    return bool(path) and str(path).split("/")[0] == DUMMY_PREFIX


def execution_subset() -> dict[str, Any]:
    rows = load_eu_rows()
    previous_rows = {}
    subset_path = WAVE / "execution_subset.json"
    if subset_path.is_file():
        previous_rows = {
            int(row["meta"]): row
            for row in census.load_json(subset_path).get("rows") or []
        }
    serialized = []
    for row in rows:
        dummy = str(row.get("registry_path") or "")
        if dummy.split("/")[0] != DUMMY_PREFIX:
            dummy = str(
                previous_rows.get(int(row["meta"]), {}).get("dummy_path")
                or row.get("r0_registry_path")
                or dummy
            )
        serialized.append(
            {
                "meta": int(row["meta"]),
                "disposition": row["disposition"],
                "cc_form": row.get("cc_form"),
                "material": row.get("material"),
                "dummy_path": dummy,
                "live_block": row.get("live_block"),
                "reason": row.get("reason"),
            }
        )
    fold = [row for row in serialized if row["disposition"] == "fold_live_block"]
    keep = [row for row in serialized if row["disposition"] == "keep_distinct"]
    shared = [row for row in serialized if row["disposition"] == "already_shared"]
    return {
        "schema": "gt6-eu-wire-cable-runtime-subset-v1",
        "capability_slug": SLUG,
        "note": (
            "Execution subset of the closed baseline identity_resolution_ledger. "
            "Do not modify that ledger or the R0 disposition ledger by hand. "
            "Recipe mapped 259 is evidence, not a dummy-deletion set."
        ),
        "counts": {
            "in_catalog_eu": len(rows),
            "fold_live_block": len(fold),
            "already_shared": len(shared),
            "keep_distinct": len(keep),
            "upgrade_live_item": sum(
                1 for row in rows if row["disposition"] == "upgrade_live_item"
            ),
        },
        "blocked": [
            "wireGt07/09/10/11/13/14/15",
            "unprefixed graphene/superconductor gauges",
            "red_alloy/signalum/lumium electrical conductors",
        ],
        "archive": (
            "no NeoForge alias; folded dummy stacks become the live CableBlockItem; "
            "unloaded dummy blocks become air"
        ),
        "rows": serialized,
    }


def runtime_notes() -> dict[str, Any]:
    return {
        "mapped_wire_specs": [
            "wireGt01",
            "wireGt02",
            "wireGt03",
            "wireGt04",
            "wireGt05",
            "wireGt06",
            "wireGt08",
            "wireGt12",
            "wireGt16",
        ],
        "mapped_cable_specs": [
            "cableGt01",
            "cableGt02",
            "cableGt04",
            "cableGt08",
            "cableGt12",
        ],
        "expected_wires": EXPECTED_WIRES,
        "expected_cables": EXPECTED_CABLES,
        "redstone_materials": ["lumium", "red_alloy", "signalum"],
        "recipe_mapped_is_not_deletion": True,
        "obtain": "explicitly_blocked",
        "close_target": "runtime_ready",
    }


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": "content/gt6-eu-wire-cable-runtime implementation",
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


def _patch_catalogs(fold_by_meta: dict[int, dict[str, Any]]) -> int:
    changed = 0
    for path in (DATA_CATALOG, TOOLS_CATALOG):
        document = census.load_json(path)
        identities = list(document.get("identities") or [])
        for identity in identities:
            meta = int(identity["meta"])
            row = fold_by_meta.get(meta)
            if row is None:
                continue
            live = _live_host(row)
            if live is None:
                continue
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
        meta = int(row["meta"])
        fold = fold_by_meta.get(meta)
        if fold is None:
            continue
        live = _live_host(fold)
        if live is None:
            continue
        if row.get("registry_path") != live:
            row["registry_path"] = live
            row["runtime_id"] = f"cruciblecraft:{live}"
            row["collision_reason"] = COLLISION_REASON
            changed += 1
        elif row.get("collision_reason") != COLLISION_REASON:
            row["collision_reason"] = COLLISION_REASON
            changed += 1
    document["collision_count"] = sum(
        1 for row in document.get("rows") or [] if row.get("collision_reason")
    )
    _write_json(modern.MAP_PATH, document)
    return changed


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


def _gated_forms() -> dict[str, set[str]]:
    gate = census.load_json(MATERIAL_GATE)
    return {
        str(material): set(forms or [])
        for material, forms in (gate.get("materials") or {}).items()
    }


def _is_placeable(row: dict[str, Any], gated: dict[str, set[str]]) -> bool:
    form = row.get("cc_form")
    material = row.get("material")
    return bool(form in MAPPED_FORMS and form in gated.get(str(material), set()))


def _dummy_path(row: dict[str, Any]) -> str:
    dummy = str(row.get("dummy_path") or "")
    if _is_dummy(dummy):
        return dummy
    r0 = str(row.get("r0_registry_path") or "")
    if _is_dummy(r0):
        return r0
    current = str(row.get("registry_path") or "")
    return current


def _fold_targets(rows: list[dict[str, Any]]) -> dict[int, dict[str, Any]]:
    gated = _gated_forms()
    targets: dict[int, dict[str, Any]] = {}
    for row in rows:
        if not _is_placeable(row, gated):
            continue
        if row.get("disposition") != "fold_live_block":
            continue
        dummy = _dummy_path(row)
        host = _live_host(row)
        if not host or not _is_dummy(dummy):
            continue
        if row.get("disposition") not in {
            "fold_live_block",
            "upgrade_live_item",
        } and not _is_dummy(str(row.get("registry_path") or dummy)):
            # After rewrite, dummy_path is preserved on fold_live_block rows.
            if row.get("disposition") != "fold_live_block":
                continue
        targets[int(row["meta"])] = {
            **row,
            "live_block": f"cruciblecraft:{host}",
            "dummy_path": dummy,
        }
    return targets


def _bath_item_paths() -> set[str]:
    document = census.load_json(BATH_DATA_CATALOG)
    return {
        str(identity.get("registry_path") or "")
        for identity in document.get("identities") or []
        if identity.get("registry_kind") == "item" and identity.get("registry_path")
    }


def _sync_new_item_count(catalog: dict[str, Any]) -> None:
    created = [
        identity
        for identity in catalog.get("identities") or []
        if identity.get("registry_kind") == "item"
    ]
    catalog["new_item_count"] = len(created)


def _restore_ungated_dummies(rows: list[dict[str, Any]]) -> int:
    gated = _gated_forms()
    restore = {
        int(row["meta"]): _dummy_path(row)
        for row in rows
        if row.get("disposition") == "upgrade_live_item"
        and not _is_placeable(row, gated)
        and _is_dummy(_dummy_path(row))
    }
    keep_metas = {
        int(row["meta"])
        for row in rows
        if row.get("disposition") == "keep_distinct"
    }
    changed = 0
    if restore:
        document = census.load_json(modern.MAP_PATH)
        for row in document.get("rows") or []:
            if str(row.get("source_item") or "") != "gregtech:gt.multitileentity":
                continue
            dummy = restore.get(int(row["meta"]))
            if dummy is None:
                continue
            if row.get("registry_path") != dummy or row.get("collision_reason"):
                row["registry_path"] = dummy
                row["runtime_id"] = f"cruciblecraft:{dummy}"
                row.pop("collision_reason", None)
                changed += 1
        document["collision_count"] = sum(
            1 for row in document.get("rows") or [] if row.get("collision_reason")
        )
        _write_json(modern.MAP_PATH, document)
    bath_items = _bath_item_paths()
    for path in (DATA_CATALOG, TOOLS_CATALOG):
        catalog = census.load_json(path)
        for identity in catalog.get("identities") or []:
            meta = int(identity["meta"])
            dummy = restore.get(meta)
            if dummy is not None:
                if identity.get("registry_kind") != "item" or identity.get(
                    "registry_path"
                ) != dummy:
                    identity["registry_kind"] = "item"
                    identity["registry_path"] = dummy
                    identity["runtime_id"] = f"cruciblecraft:{dummy}"
                    changed += 1
                continue
            if meta not in keep_metas:
                continue
            path_id = str(identity.get("registry_path") or "")
            if identity.get("registry_kind") == "item" and path_id in bath_items:
                identity["registry_kind"] = "existing_item"
                changed += 1
        _sync_new_item_count(catalog)
        _write_json(path, catalog)
    return changed


def write(unique_active: bool = True) -> dict[str, Any]:
    rows = load_eu_rows()
    previous_path = WAVE / "execution_subset.json"
    previous_rows = {}
    if previous_path.is_file():
        previous_rows = {
            int(row["meta"]): row
            for row in census.load_json(previous_path).get("rows") or []
        }
    for row in rows:
        previous = previous_rows.get(int(row["meta"]))
        if previous and previous.get("dummy_path"):
            row["dummy_path"] = previous["dummy_path"]
    _restore_ungated_dummies(rows)
    fold_by_meta = _fold_targets(rows)
    if len(fold_by_meta) != FOLD_LIVE_BLOCK:
        raise ValueError(
            f"fold_live_block {len(fold_by_meta)} != {FOLD_LIVE_BLOCK}"
        )
    WAVE.mkdir(parents=True, exist_ok=True)
    folded = _patch_catalogs(fold_by_meta)
    mapped = _patch_modern_map(fold_by_meta)
    modern.rewrite_catalog_item_tags()
    subset = execution_subset()
    _write_json(WAVE / "execution_subset.json", subset)
    _write_json(WAVE / "runtime_notes.json", runtime_notes())
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(WAVE / "readiness.json", readiness(unique_active))
    _write_json(
        WAVE / "production_lock.json",
        {"note": "live compile of mapped EU gauges; not player_complete"},
    )
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
        "fold_live_block": len(fold_by_meta),
    }


def check() -> list[str]:
    errors: list[str] = []
    subset_path = WAVE / "execution_subset.json"
    if not subset_path.is_file():
        return [f"missing {census.relative(subset_path)}"]
    live = execution_subset()
    committed = census.load_json(subset_path)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"execution_subset.json drifted: {drift}")
    counts = committed.get("counts") or {}
    expected = {
        "fold_live_block": FOLD_LIVE_BLOCK,
        "in_catalog_eu": IN_CATALOG,
        "already_shared": ALREADY_SHARED,
        "keep_distinct": KEEP_DISTINCT,
        "upgrade_live_item": UPGRADE_LIVE_ITEM,
    }
    for key, value in expected.items():
        if int(counts.get(key) or 0) != value:
            errors.append(f"{key} {counts.get(key)} != {value}")
    fold_by_meta = {
        int(row["meta"]): row
        for row in committed.get("rows") or []
        if row.get("disposition") == "fold_live_block"
    }
    bath_items = _bath_item_paths()
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
            live_path = _live_host(fold)
            if identity.get("registry_kind") != "existing_item":
                errors.append(f"meta {meta} still dummy item")
            if identity.get("registry_path") != live_path:
                errors.append(
                    f"meta {meta} catalog {identity.get('registry_path')} "
                    f"!= {live_path}"
                )
        keep = [
            row
            for row in committed.get("rows") or []
            if row.get("disposition") == "keep_distinct"
        ]
        repaired = alias_repair.folded_metas() | missing_gauges.folded_metas()
        for row in keep:
            if int(row["meta"]) in repaired:
                continue
            identity = by_meta.get(int(row["meta"]))
            if identity is None:
                continue
            path_id = str(identity.get("registry_path") or "")
            dummy = str(row.get("dummy_path") or "")
            if dummy and _is_dummy(dummy) and path_id != dummy:
                errors.append(
                    f"keep_distinct meta {row['meta']} left dummy path "
                    f"{path_id} != {dummy}"
                )
            live_host = _strip_ns(str(row.get("live_block") or ""))
            if live_host and path_id == live_host and row.get("cc_form") is None:
                errors.append(
                    f"keep_distinct meta {row['meta']} folded onto live host "
                    f"{path_id}"
                )
            if identity.get("registry_kind") == "item" and path_id in bath_items:
                errors.append(
                    f"keep_distinct meta {row['meta']} re-registers bath dummy "
                    f"{path_id}"
                )
        smelter_items = {
            str(identity.get("registry_path") or "")
            for identity in catalog.get("identities") or []
            if identity.get("registry_kind") == "item"
        }
        overlap = sorted(path for path in smelter_items & bath_items if path)
        if overlap:
            errors.append(
                f"{census.relative(path)} re-registers bath dummies: "
                + ", ".join(overlap[:8])
            )
        upgrades = [
            row
            for row in committed.get("rows") or []
            if row.get("disposition") == "upgrade_live_item"
        ]
        for row in upgrades:
            identity = by_meta.get(int(row["meta"]))
            if identity is None:
                continue
            dummy = str(row.get("dummy_path") or "")
            path_id = str(identity.get("registry_path") or "")
            if dummy and path_id != dummy:
                errors.append(
                    f"upgrade_live_item meta {row['meta']} left dummy path "
                    f"{path_id} != {dummy}"
                )
            if identity.get("registry_kind") != "item":
                errors.append(
                    f"upgrade_live_item meta {row['meta']} is not a dummy item"
                )
    mapped = {
        int(row["meta"]): row
        for row in census.load_json(modern.MAP_PATH).get("rows") or []
        if str(row.get("source_item") or "") == "gregtech:gt.multitileentity"
    }
    for meta, fold in fold_by_meta.items():
        row = mapped.get(meta)
        live_path = _live_host(fold)
        if row is None:
            errors.append(f"modern map missing folded meta {meta}")
            continue
        if row.get("registry_path") != live_path:
            errors.append(
                f"modern map meta {meta} {row.get('registry_path')} != {live_path}"
            )
        if row.get("collision_reason") != COLLISION_REASON:
            errors.append(
                f"modern map meta {meta} collision_reason "
                f"{row.get('collision_reason')}"
            )
    catalog_java = CATALOG_JAVA.read_text(encoding="utf-8")
    expected_wires = (
        missing_gauges.EXPECTED_WIRES
        if missing_gauges.folded_metas()
        else EXPECTED_WIRES
    )
    expected_cables = (
        missing_gauges.EXPECTED_CABLES
        if missing_gauges.folded_metas()
        else EXPECTED_CABLES
    )
    if f"EXPECTED_WIRE_BLOCKS = {expected_wires}" not in catalog_java:
        errors.append("ElectricalConductorCatalog wire census drifted")
    if f"EXPECTED_CABLE_BLOCKS = {expected_cables}" not in catalog_java:
        errors.append("ElectricalConductorCatalog cable census drifted")
    if missing_gauges.folded_metas():
        if "wireGt07" not in catalog_java:
            errors.append("missing-gauge overlay present but catalog lost wireGt07")
    elif "wireGt07" in catalog_java:
        errors.append("ElectricalConductorCatalog invented wireGt07")
    if "DOUBLE_WIRE" not in catalog_java or "HEXADECUPLE_WIRE" not in catalog_java:
        errors.append("mapped wire gauges missing from ElectricalConductorCatalog")
    if "red_alloy" not in catalog_java:
        errors.append("redstone materials are not excluded from the EU catalog")
    if 'case "cableGt08" -> 12' not in catalog_java:
        errors.append("cableGt08 width is not the GT6 PX_P[12] source value")
    cable_block = CABLE_BLOCK.read_text(encoding="utf-8")
    if "widthFor(" in cable_block:
        errors.append("CableBlock still has a private width switch")
    tests = GAME_TESTS.read_text(encoding="utf-8")
    for name in EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in EXPECTED_TESTS:
        if f"void {name}" in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing wave structure/empty.nbt")
    if not (PACK / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append("missing wave gametest/structure/empty.nbt")
    r0_hash_path = WAVE / "r0_disposition_sha256.txt"
    if not r0_hash_path.is_file():
        errors.append("missing r0_disposition_sha256.txt")
    elif r0_hash_path.read_text(encoding="utf-8").strip() != _sha256(R0):
        errors.append("R0 disposition ledger was modified")
    baseline_hash_path = WAVE / "baseline_ledger_sha256.txt"
    if not baseline_hash_path.is_file():
        errors.append("missing baseline_ledger_sha256.txt")
    elif (
        baseline_hash_path.read_text(encoding="utf-8").strip() != _sha256(LEDGER)
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
