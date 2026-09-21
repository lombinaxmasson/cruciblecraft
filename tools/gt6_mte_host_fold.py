#!/usr/bin/env python3
"""Fold GT6 MTE dummy identities onto existing live hosts.

Shared across sequential unique-active children: converter, hopper,
processing, reactor rod. Does not rewrite R0 or the connector baseline
ledger. Catalogs are patched first, then the modern id map.
"""
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
from tools import io_common as io

GT6_REVISION = io.SOURCE_REVISION
STATUS = "MTE_HOST_FOLD_READY"
DATA = (
    census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
)
DATA_CATALOG = DATA / "smelter_mte_identity_catalog.json"
TOOLS_CATALOG = census.TOOLS / "smelter_mte_identity_catalog.json"
CONVERTER_TIERS = DATA / "energy_converter_tiers.json"
MACHINE_TIERS = DATA / "machine_tiers.json"
AUTOMATIC_HAMMER_HOSTS = {
    15001: "automatic_hammer",
    15002: "steel_automatic_hammer",
    15003: "titanium_automatic_hammer",
    15004: "tungstensteel_automatic_hammer",
}
REACTOR_RODS = DATA / "nuclear_reactor_rods.json"
HOPPER_EVIDENCE = census.TOOLS / "hopper_hopper_source_evidence.json"
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
LEDGER = (
    census.TOOLS
    / "waves"
    / "content"
    / "gt6-pipe-cable-baseline"
    / "identity_resolution_ledger.json"
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
RECIPE_ROOTS = (
    census.ROOT / "src" / "recipe_generated",
    census.ROOT / "src" / "recipe_support_generated",
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
ARCHIVE = {
    "dummy_item": "unregister CatalogNamedItem",
    "dummy_model": "delete iron_ingot item model",
    "world_stacks": (
        "unregistered dummy ids become air; no NeoForge alias and no "
        "second GT6 art copy"
    ),
    "recipes": "operand remaps dummy path onto the live host id",
}

DOMAINS: dict[str, dict[str, Any]] = {
    "converter": {
        "slug": "content/gt6-mte-converter-host-fold",
        "family": "energy_converter",
        "title": "GT6 Converter Host Fold",
        "collision_reason": "folded onto live converter host",
        "expected_folds": 71,
        "expected_keep": 8,
        "tests": [
            "leadBoilerAndTantalumBoxFoldOntoLiveHosts",
            "luvAndZpmBatteryBoxesStayDummy",
            "steamTurbinesStayDummy",
        ],
        "plan_stem": "GT6能源转换器主机折回详细计划.md",
        "lock_note": (
            "71 converter dummies folded onto live converter BlockItems; "
            "6 turbines and 2 battery boxes stay dummy; not player_complete"
        ),
    },
    "hopper": {
        "slug": "content/gt6-mte-hopper-host-fold",
        "family": "hopper",
        "title": "GT6 Hopper Host Fold",
        "collision_reason": "folded onto live hopper host",
        "expected_folds": 101,
        "expected_keep": 0,
        "tests": [
            "dustFunnelFoldsOntoSteelDustFunnel",
            "leadHopperAndQueueFoldOntoLiveHosts",
        ],
        "plan_stem": "GT6漏斗主机折回详细计划.md",
        "lock_note": (
            "50 hoppers, 50 queue hoppers and the dust funnel folded onto "
            "T30 live hosts; not player_complete"
        ),
    },
    "processing": {
        "slug": "content/gt6-mte-processing-host-fold",
        "family": "processing_machine",
        "title": "GT6 Processing Host Fold",
        "collision_reason": "folded onto live processing host",
        "expected_folds": 86,
        "expected_keep": 0,
        "tests": [
            "automaticHammersStayDummy",
            "bronzeSifterFoldsOntoLiveHost",
            "polarizerMagSepFoldOntoLiveHosts",
            "squeezerLaserStayDummy",
        ],
        "plan_stem": "GT6加工机主机折回详细计划.md",
        "lock_note": (
            "86 processing metas folded onto live processing hosts, including "
            "the four automatic hammers; not player_complete"
        ),
    },
    "reactor": {
        "slug": "content/gt6-mte-reactor-rod-host-fold",
        "family": "reactor",
        "title": "GT6 Reactor Rod Host Fold",
        "collision_reason": "folded onto live reactor rod",
        "expected_folds": 1,
        "expected_keep": 0,
        "tests": [
            "meta9203FoldsOntoNeutronReflectorRod",
        ],
        "plan_stem": "GT6反应棒主机折回详细计划.md",
        "lock_note": (
            "reactor meta 9203 folded onto neutron_reflector_rod; desh "
            "material 9203 is not this host; not player_complete"
        ),
    },
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


def _wave(domain: str) -> Path:
    return census.TOOLS / "waves" / "content" / DOMAINS[domain]["slug"].split("/", 1)[1]


def _pack(domain: str) -> Path:
    slug = DOMAINS[domain]["slug"].replace("/", "_").replace("-", "_")
    return (
        census.ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / f"cruciblecraft_wave_{slug}"
    )


def _game_tests(domain: str) -> Path:
    names = {
        "converter": "MteConverterHostFoldGameTests.java",
        "hopper": "MteHopperHostFoldGameTests.java",
        "processing": "MteProcessingHostFoldGameTests.java",
        "reactor": "MteReactorRodHostFoldGameTests.java",
    }
    return (
        census.ROOT
        / "src"
        / "test"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "gametest"
        / names[domain]
    )


def _r0_family(family: str) -> list[dict[str, Any]]:
    document = census.load_json(R0)
    return [
        row
        for row in document.get("identities") or []
        if row.get("family") == family
    ]


def _catalog_by_meta() -> dict[int, dict[str, Any]]:
    return {
        int(row["meta"]): row
        for row in census.load_json(DATA_CATALOG).get("identities") or []
    }


def converter_targets() -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    tiers = {
        int(row["source_id"]): row
        for row in census.load_json(CONVERTER_TIERS).get("tiers") or []
        if row.get("source_id") is not None
    }
    catalog = _catalog_by_meta()
    folds: list[dict[str, Any]] = []
    keep: list[dict[str, Any]] = []
    for row in _r0_family("energy_converter"):
        meta = int(row["meta"])
        identity = catalog[meta]
        dummy = str(row.get("registry_path") or identity.get("registry_path") or "")
        host = tiers.get(meta)
        if host is None:
            keep.append(
                {
                    "meta": meta,
                    "dummy_path": dummy,
                    "english_name": row.get("english_name"),
                    "gt6_class_or_tag": row.get("gt6_class_or_tag"),
                    "disposition": "keep_distinct",
                }
            )
            continue
        live = _strip_ns(str(host["id"]))
        folds.append(
            {
                "meta": meta,
                "dummy_path": dummy,
                "live_block": f"cruciblecraft:{live}",
                "english_name": row.get("english_name"),
                "gt6_class_or_tag": row.get("gt6_class_or_tag"),
                "source_id": meta,
                "disposition": "fold_live_block",
                "collision_reason": DOMAINS["converter"]["collision_reason"],
            }
        )
    return folds, keep


def hopper_targets() -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    evidence = census.load_json(HOPPER_EVIDENCE)
    by_hopper = {int(row["hopper_gt6_id"]): row for row in evidence.get("rows") or []}
    by_queue = {int(row["queue_gt6_id"]): row for row in evidence.get("rows") or []}
    catalog = _catalog_by_meta()
    folds: list[dict[str, Any]] = []
    keep: list[dict[str, Any]] = []
    for row in _r0_family("hopper"):
        meta = int(row["meta"])
        identity = catalog[meta]
        dummy = str(row.get("registry_path") or identity.get("registry_path") or "")
        if meta == 32704:
            live = "steel_dust_funnel"
            source_id = 32704
        elif meta in by_hopper:
            material = _strip_ns(str(by_hopper[meta]["cc_material"]))
            live = f"{material}_hopper"
            source_id = int(by_hopper[meta]["hopper_gt6_id"])
        elif meta in by_queue:
            material = _strip_ns(str(by_queue[meta]["cc_material"]))
            live = f"{material}_queue_hopper"
            source_id = int(by_queue[meta]["queue_gt6_id"])
        else:
            keep.append(
                {
                    "meta": meta,
                    "dummy_path": dummy,
                    "english_name": row.get("english_name"),
                    "disposition": "keep_distinct",
                }
            )
            continue
        folds.append(
            {
                "meta": meta,
                "dummy_path": dummy,
                "live_block": f"cruciblecraft:{live}",
                "english_name": row.get("english_name"),
                "gt6_class_or_tag": row.get("gt6_class_or_tag"),
                "source_id": source_id,
                "disposition": "fold_live_block",
                "collision_reason": DOMAINS["hopper"]["collision_reason"],
            }
        )
    return folds, keep


def processing_targets() -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    tiers = {
        int(row["sourceId"]): row
        for row in census.load_json(MACHINE_TIERS).get("variants") or []
        if row.get("sourceId") is not None
    }
    catalog = _catalog_by_meta()
    folds: list[dict[str, Any]] = []
    keep: list[dict[str, Any]] = []
    for row in _r0_family("processing_machine"):
        meta = int(row["meta"])
        identity = catalog[meta]
        dummy = str(row.get("registry_path") or identity.get("registry_path") or "")
        host = tiers.get(meta)
        if meta in AUTOMATIC_HAMMER_HOSTS:
            live = AUTOMATIC_HAMMER_HOSTS[meta]
            folds.append(
                {
                    "meta": meta,
                    "dummy_path": dummy,
                    "live_block": f"cruciblecraft:{live}",
                    "english_name": row.get("english_name"),
                    "gt6_class_or_tag": row.get("gt6_class_or_tag"),
                    "source_id": meta,
                    "machine_kind": "automatic_hammer",
                    "disposition": "fold_live_block",
                    "collision_reason": DOMAINS["processing"]["collision_reason"],
                }
            )
            continue
        if host is None:
            keep.append(
                {
                    "meta": meta,
                    "dummy_path": dummy,
                    "english_name": row.get("english_name"),
                    "gt6_class_or_tag": row.get("gt6_class_or_tag"),
                    "disposition": "keep_distinct",
                }
            )
            continue
        live = _strip_ns(str(host["id"]))
        folds.append(
            {
                "meta": meta,
                "dummy_path": dummy,
                "live_block": f"cruciblecraft:{live}",
                "english_name": row.get("english_name"),
                "gt6_class_or_tag": row.get("gt6_class_or_tag"),
                "source_id": meta,
                "machine_kind": host.get("kind"),
                "disposition": "fold_live_block",
                "collision_reason": DOMAINS["processing"]["collision_reason"],
            }
        )
    return folds, keep


def reactor_targets() -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    rods = {
        int(row["source_id"]): row
        for row in census.load_json(REACTOR_RODS).get("rods") or []
        if row.get("source_id") is not None
    }
    catalog = _catalog_by_meta()
    folds: list[dict[str, Any]] = []
    keep: list[dict[str, Any]] = []
    for row in _r0_family("reactor"):
        meta = int(row["meta"])
        identity = catalog[meta]
        dummy = str(row.get("registry_path") or identity.get("registry_path") or "")
        host = rods.get(meta)
        live = _strip_ns(str(host["id"])) if host else ""
        if meta != 9203 or live != "neutron_reflector_rod":
            keep.append(
                {
                    "meta": meta,
                    "dummy_path": dummy,
                    "english_name": row.get("english_name"),
                    "disposition": "keep_distinct",
                }
            )
            continue
        folds.append(
            {
                "meta": meta,
                "dummy_path": dummy,
                "live_block": f"cruciblecraft:{live}",
                "english_name": row.get("english_name"),
                "gt6_class_or_tag": row.get("gt6_class_or_tag"),
                "source_id": meta,
                "material": host.get("material"),
                "disposition": "fold_live_block",
                "collision_reason": DOMAINS["reactor"]["collision_reason"],
            }
        )
    return folds, keep


def _targets(domain: str) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    if domain == "converter":
        return converter_targets()
    if domain == "hopper":
        return hopper_targets()
    if domain == "processing":
        return processing_targets()
    if domain == "reactor":
        return reactor_targets()
    raise ValueError(f"unknown domain {domain}")


def build_overlay(domain: str) -> dict[str, Any]:
    spec = DOMAINS[domain]
    folds, keep = _targets(domain)
    folds = sorted(folds, key=lambda row: int(row["meta"]))
    keep = sorted(keep, key=lambda row: int(row["meta"]))
    return {
        "schema_version": 1,
        "capability_slug": spec["slug"],
        "source_revision": GT6_REVISION,
        "generated_by": f"{spec['slug']} implementation",
        "family": spec["family"],
        "archive_strategy": ARCHIVE,
        "counts": {
            "fold_live_block": len(folds),
            "keep_distinct": len(keep),
        },
        "rows": folds,
        "keep_distinct": keep,
    }


def topology(domain: str, unique_active: bool) -> dict[str, Any]:
    slug = DOMAINS[domain]["slug"]
    return {
        "schema_version": 1,
        "wave_slug": slug,
        "unique_active_wave": slug if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": f"{slug} implementation",
        "append_only": False,
        "next_unassigned": True,
        "remaining_recipe_gap": 0,
    }


def readiness(domain: str, unique_active: bool) -> dict[str, Any]:
    slug = DOMAINS[domain]["slug"]
    return {
        "schema_version": 1,
        "wave_slug": slug,
        "unique_active_wave": slug if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
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


def _patch_modern_map(fold_by_meta: dict[int, dict[str, Any]], reason: str) -> int:
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


def _copy_empty_nbt(domain: str) -> None:
    if not EMPTY_SRC.is_file():
        raise FileNotFoundError(f"missing {census.relative(EMPTY_SRC)}")
    pack = _pack(domain)
    for dest in (
        pack / "structure" / "empty.nbt",
        pack / "gametest" / "structure" / "empty.nbt",
    ):
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.is_file() and dest.read_bytes() == EMPTY_SRC.read_bytes():
            continue
        shutil.copyfile(EMPTY_SRC, dest)


def write(unique_active: bool = True, domain: str = "converter") -> dict[str, Any]:
    if domain not in DOMAINS:
        raise ValueError(f"unknown domain {domain}")
    spec = DOMAINS[domain]
    overlay = build_overlay(domain)
    counts = overlay["counts"]
    if int(counts["fold_live_block"]) != spec["expected_folds"]:
        raise ValueError(
            f"{domain} fold_live_block {counts['fold_live_block']} != "
            f"{spec['expected_folds']}"
        )
    if int(counts["keep_distinct"]) != spec["expected_keep"]:
        raise ValueError(
            f"{domain} keep_distinct {counts['keep_distinct']} != "
            f"{spec['expected_keep']}"
        )
    live_hosts = modern.live_host_paths()
    for row in overlay["rows"]:
        live = _strip_ns(str(row["live_block"]))
        if live not in live_hosts:
            raise ValueError(f"{domain} live host {live} is not registered")
    wave = _wave(domain)
    wave.mkdir(parents=True, exist_ok=True)
    fold_by_meta = {int(row["meta"]): row for row in overlay["rows"]}
    _write_json(wave / "fold_overlay.json", overlay)
    _write_json(wave / "topology.json", topology(domain, unique_active))
    _write_json(wave / "readiness.json", readiness(domain, unique_active))
    _write_json(wave / "archive_strategy.json", ARCHIVE)
    _write_json(wave / "production_lock.json", {"note": spec["lock_note"]})
    folded = _patch_catalogs(fold_by_meta)
    mapped = _patch_modern_map(fold_by_meta, spec["collision_reason"])
    modern.rewrite_catalog_item_tags()
    remapped = _remap_recipe_ids(overlay)
    imports = {}
    if remapped:
        imports = _refresh_closed_machine_imports()
    withdrawn = _withdraw_dummy_models(
        {str(row["dummy_path"]) for row in overlay["rows"]}
    )
    _copy_empty_nbt(domain)
    (wave / "r0_disposition_sha256.txt").write_text(
        _sha256(R0) + "\n", encoding="utf-8"
    )
    (wave / "baseline_ledger_sha256.txt").write_text(
        _sha256(LEDGER) + "\n", encoding="utf-8"
    )
    return {
        "domain": domain,
        "folded_identities": folded,
        "mapped_rows": mapped,
        "recipe_files": remapped,
        "machine_imports": imports,
        "withdrawn_models": withdrawn,
        "fold_live_block": len(fold_by_meta),
    }


def _check_domain(domain: str) -> list[str]:
    spec = DOMAINS[domain]
    wave = _wave(domain)
    overlay_path = wave / "fold_overlay.json"
    errors: list[str] = []
    if not overlay_path.is_file():
        return [f"missing {census.relative(overlay_path)}"]
    live = build_overlay(domain)
    committed = census.load_json(overlay_path)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"{domain} fold_overlay.json drifted: {drift}")
    counts = committed.get("counts") or {}
    if int(counts.get("fold_live_block") or 0) != spec["expected_folds"]:
        errors.append(
            f"{domain} fold_live_block {counts.get('fold_live_block')} != "
            f"{spec['expected_folds']}"
        )
    if int(counts.get("keep_distinct") or 0) != spec["expected_keep"]:
        errors.append(
            f"{domain} keep_distinct {counts.get('keep_distinct')} != "
            f"{spec['expected_keep']}"
        )
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
                errors.append(f"{domain} live host {live_path} is not registered")
            if identity.get("registry_kind") != "existing_item":
                errors.append(f"{domain} meta {meta} still dummy item")
            if identity.get("registry_path") != live_path:
                errors.append(
                    f"{domain} meta {meta} catalog {identity.get('registry_path')} "
                    f"!= {live_path}"
                )
        for row in committed.get("keep_distinct") or []:
            identity = by_meta.get(int(row["meta"]))
            if identity is None:
                errors.append(
                    f"{census.relative(path)} missing keep meta {row['meta']}"
                )
                continue
            dummy_path = str(row.get("dummy_path") or "")
            kind = identity.get("registry_kind")
            if kind == "item":
                continue
            if (
                kind == "existing_item"
                and identity.get("registry_path") == dummy_path
                and dummy_path in live_hosts
            ):
                continue
            errors.append(
                f"{domain} keep meta {row['meta']} lost its dummy item"
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
    tests_path = _game_tests(domain)
    tests = tests_path.read_text(encoding="utf-8") if tests_path.is_file() else ""
    for name in spec["tests"]:
        if f"void {name}" not in tests:
            errors.append(f"missing GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in spec["tests"]:
        if name in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    pack = _pack(domain)
    if not (pack / "structure" / "empty.nbt").is_file():
        errors.append(f"missing {domain} structure/empty.nbt")
    if not (pack / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append(f"missing {domain} gametest/structure/empty.nbt")
    r0_hash = wave / "r0_disposition_sha256.txt"
    if not r0_hash.is_file() or r0_hash.read_text(encoding="utf-8").strip() != _sha256(
        R0
    ):
        errors.append("R0 disposition ledger was modified")
    baseline_hash = wave / "baseline_ledger_sha256.txt"
    if (
        not baseline_hash.is_file()
        or baseline_hash.read_text(encoding="utf-8").strip() != _sha256(LEDGER)
    ):
        errors.append("baseline identity_resolution_ledger was modified")
    return errors


def check() -> list[str]:
    errors: list[str] = []
    found = False
    for domain in DOMAINS:
        overlay = _wave(domain) / "fold_overlay.json"
        if not overlay.is_file():
            continue
        found = True
        errors.extend(_check_domain(domain))
    if not found:
        return ["missing any MTE host-fold overlay"]
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--unique-active", action="store_true")
    parser.add_argument("--no-unique-active", action="store_true")
    parser.add_argument(
        "--domain",
        choices=sorted(DOMAINS),
        default="converter",
    )
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    if args.unique_active and args.no_unique_active:
        parser.error("choose --unique-active or --no-unique-active")
    try:
        if args.write:
            unique_active = not args.no_unique_active
            result = write(unique_active=unique_active, domain=args.domain)
            print(json.dumps(result, ensure_ascii=False, indent=2))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("mte host fold is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"mte host fold failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
