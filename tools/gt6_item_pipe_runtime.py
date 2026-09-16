#!/usr/bin/env python3
"""GT6 ordinary item-pipe runtime child. Execution subset only; no R0 rewrite."""
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
from tools import gt6_restrictive_item_pipe_runtime as restrictive_pipe
from tools import io_common as io

SLUG = "content/gt6-item-pipe-runtime"
STATUS = "ITEM_PIPE_RUNTIME_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-item-pipe-runtime"
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
    / "ItemPipeBlockEntity.java"
)
PHASE = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "logistics"
    / "pipe"
    / "PipeTransferPhase.java"
)
BLOCK = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "block"
    / "AbstractPipeBlock.java"
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
    / "ItemPipeRuntimeGameTests.java"
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
    / "ItemNetworkCoreGameTests.java"
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
    / "cruciblecraft_wave_content_gt6_item_pipe_runtime"
)
EXPECTED_TESTS = [
    "itemPipeDisabledInputsOutputs",
    "itemPipeFullDoesNotVoid",
    "itemPipeHasInternalInventory",
    "itemPipeRestrictiveStepSize",
    "itemPipeTenTickSend",
]
ORDINARY_FORMS = {"item_pipe", "large_item_pipe", "huge_item_pipe"}
FOLD_LIVE_BLOCK = 57
IN_CATALOG = 63


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


def load_item_rows() -> list[dict[str, Any]]:
    document = census.load_json(LEDGER)
    return [
        row
        for row in document.get("rows") or []
        if row.get("domain") == "item" and row.get("in_catalog_1817")
    ]


def execution_subset() -> dict[str, Any]:
    rows = load_item_rows()
    previous_rows = {}
    subset_path = WAVE / "execution_subset.json"
    if subset_path.is_file():
        previous_rows = {
            int(row["meta"]): row
            for row in census.load_json(subset_path).get("rows") or []
        }
    dummy_prefixes = {"item_pipe_tile"}
    serialized = []
    for row in rows:
        dummy = str(row.get("registry_path") or "")
        if dummy.split("/")[0] not in dummy_prefixes:
            dummy = str(
                previous_rows.get(int(row["meta"]), {}).get("dummy_path") or dummy
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
        "schema": "gt6-item-pipe-runtime-subset-v1",
        "capability_slug": SLUG,
        "note": (
            "Execution subset of the closed baseline identity_resolution_ledger. "
            "Do not modify that ledger or the R0 disposition ledger."
        ),
        "counts": {
            "in_catalog_item": len(rows),
            "fold_live_block": len(fold),
            "already_shared": len(shared),
            "keep_distinct": len(keep),
            "upgrade_live_item": sum(
                1 for row in rows if row["disposition"] == "upgrade_live_item"
            ),
        },
        "blocked": [
            "pipeRestrictiveMedium",
            "pipeRestrictiveLarge",
            "pipeRestrictiveHuge",
            "cover-pump-only as GT6 inventory substitute",
        ],
        "archive": (
            "no NeoForge alias; folded dummy stacks become the live BlockItem; "
            "unloaded dummy blocks become air"
        ),
        "rows": serialized,
    }


def runtime_notes() -> dict[str, Any]:
    return {
        "ordinary_gauges": sorted(ORDINARY_FORMS),
        "restrictive_registered": False,
        "cover_phase_ticks": 5,
        "inventory_send_ticks": 10,
        "inv_size": "GT6 stacks_per_second / invSize slots",
        "disabled_io": "mDisabledInputs / mDisabledOutputs monkey-wrench cycle",
        "energy_capability": False,
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
        "generated_by": "content/gt6-item-pipe-runtime implementation",
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
        meta = int(row["meta"])
        fold = fold_by_meta.get(meta)
        if fold is None:
            continue
        live = _strip_ns(str(fold["live_block"]))
        if row.get("registry_path") != live:
            row["registry_path"] = live
            row["runtime_id"] = f"cruciblecraft:{live}"
            row["collision_reason"] = "folded onto live item pipe"
            changed += 1
        elif row.get("collision_reason") != "folded onto live item pipe":
            row["collision_reason"] = "folded onto live item pipe"
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


def write(unique_active: bool = True) -> dict[str, Any]:
    subset = execution_subset()
    fold_by_meta = {
        int(row["meta"]): row
        for row in subset["rows"]
        if row["disposition"] == "fold_live_block"
    }
    if len(fold_by_meta) != FOLD_LIVE_BLOCK:
        raise ValueError(
            f"fold_live_block {len(fold_by_meta)} != {FOLD_LIVE_BLOCK}"
        )
    WAVE.mkdir(parents=True, exist_ok=True)
    _write_json(WAVE / "execution_subset.json", subset)
    _write_json(WAVE / "runtime_notes.json", runtime_notes())
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(WAVE / "readiness.json", readiness(unique_active))
    _write_json(
        WAVE / "production_lock.json",
        {"note": "live compile of ordinary item pipes; not player_complete"},
    )
    folded = _patch_catalogs(fold_by_meta)
    mapped = _patch_modern_map(fold_by_meta)
    modern.rewrite_catalog_item_tags()
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
    if int(counts.get("fold_live_block") or 0) != FOLD_LIVE_BLOCK:
        errors.append(
            f"fold_live_block {counts.get('fold_live_block')} != {FOLD_LIVE_BLOCK}"
        )
    if int(counts.get("in_catalog_item") or 0) != IN_CATALOG:
        errors.append(
            f"in_catalog_item {counts.get('in_catalog_item')} != {IN_CATALOG}"
        )
    fold_by_meta = {
        int(row["meta"]): row
        for row in committed.get("rows") or []
        if row.get("disposition") == "fold_live_block"
    }
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
        keep = [
            row
            for row in committed.get("rows") or []
            if row.get("disposition") == "keep_distinct"
        ]
        repaired = restrictive_pipe.folded_metas()
        for row in keep:
            if int(row["meta"]) in repaired:
                continue
            identity = by_meta.get(int(row["meta"]))
            if identity is None:
                continue
            path_id = str(identity.get("registry_path") or "")
            dummy = str(row.get("dummy_path") or "")
            if dummy and path_id != dummy:
                errors.append(
                    f"keep_distinct meta {row['meta']} left dummy path "
                    f"{path_id} != {dummy}"
                )
            live_host = _strip_ns(str(row.get("live_block") or ""))
            if live_host and path_id == live_host:
                errors.append(
                    f"keep_distinct meta {row['meta']} folded onto live host "
                    f"{path_id}"
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
    be = BE.read_text(encoding="utf-8")
    if "SEND_INTERVAL = 10" not in be:
        errors.append("ItemPipeBlockEntity lost the 10-tick send interval")
    if "storeIncoming" not in be:
        errors.append("ItemPipeBlockEntity has no in-pipe inventory insert")
    if "return ItemStack.EMPTY;" in be.split("getStackInSlot", 1)[-1].split(
        "insertItem", 1
    )[0]:
        errors.append("SidedHandler.getStackInSlot still returns empty")
    if "tickCovers" not in be or "PipeTransferPhase.isDue" not in be:
        errors.append("cover pumps are no longer gated on the 5-tick phase")
    if "sendStored" not in be:
        errors.append("10-tick inventory send missing")
    if "cycleDisabledIo" not in be:
        errors.append("disabled I/O cycle missing")
    if "CableNetworkTraversal" in be or "ModCapabilities.ENERGY" in be:
        errors.append("item pipe attached to ENERGY/cable traversal")
    phase = PHASE.read_text(encoding="utf-8")
    if "INTERVAL = 5" not in phase:
        errors.append("PipeTransferPhase.INTERVAL drifted from 5")
    block = BLOCK.read_text(encoding="utf-8")
    if "MONKEY_WRENCH" not in block or "cycleItemPipeIo" not in block:
        errors.append("AbstractPipeBlock lost monkey-wrench I/O")
    catalog_java = CATALOG_JAVA.read_text(encoding="utf-8")
    item_specs = catalog_java.split("ITEM_SPEC_BY_FORM", 1)[-1].split(
        "private static volatile State", 1
    )[0]
    if "pipeRestrictive" in item_specs and not restrictive_pipe.folded_metas():
        errors.append("PipeCatalog registered restrictive item forms")
    tests = GAME_TESTS.read_text(encoding="utf-8")
    for name in EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in EXPECTED_TESTS:
        if name in core:
            errors.append(f"ItemNetworkCoreGameTests absorbed {name}")
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
