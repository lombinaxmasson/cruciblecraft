#!/usr/bin/env python3
"""GT6 18040-18045 coil and host landing checks. Does not claim unique-active."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from tools import io_common as io

ROOT = io.ROOT
CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "mte_inplace_catalog.json"
)
ACQUISITION = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "mte_inplace_acquisition.json"
)
OVEN = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "multiblock_structures"
    / "large_oven.json"
)
KIND = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "mte"
    / "MteInPlaceKind.java"
)
FUSION = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "fusion"
    / "FusionStructure.java"
)
OVERLAY = (
    ROOT
    / "tools"
    / "waves"
    / "content"
    / "gt6-mte-multiblock-runtime"
    / "runtime_overlay.json"
)
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_coil_host_art_manifest.json"
)
DEDICATED_RECIPE = (
    ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "machines"
    / "large_iridium_coil.json"
)
POLICY = ROOT / "tools" / "structure_multiblock_policy.json"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "machines"
    / "gt6-coil-hosts"
    / "capability.json"
)
COIL_METAS = (18040, 18041, 18042, 18043, 18044, 18045)
HOST_KINDS = {
    17199: "MATTER_FABRICATOR",
    17221: "LARGE_DYNAMO",
    17222: "LARGE_DYNAMO",
    17223: "LARGE_DYNAMO",
    17224: "LARGE_DYNAMO",
    17996: "VON_DA_GRAAGG",
    17998: "LIGHTNING_ROD",
    18104: "MULTIBLOCK_PART",
}


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    catalog = _load(CATALOG)
    by_meta = {row["meta"]: row for row in catalog.get("identities") or []}
    for meta in COIL_METAS:
        row = by_meta.get(meta)
        if row is None:
            errors.append(f"missing coil meta {meta}")
            continue
        if row.get("kind") != "MULTIBLOCK_PART":
            errors.append(f"coil {meta} kind {row.get('kind')}")
        runtime = row.get("runtime_id") or ""
        if not runtime.endswith("_coil"):
            errors.append(f"coil {meta} runtime {runtime}")
        chinese = row.get("chinese_name") or ""
        if not any("\u4e00" <= ch <= "\u9fff" for ch in chinese):
            errors.append(f"coil {meta} missing CJK name {chinese!r}")
    iridium = by_meta.get(18045) or {}
    if iridium.get("runtime_id") != "cruciblecraft:multiblock/large_iridium_coil":
        errors.append("18045 is not the MTE iridium coil")
    for meta, kind in HOST_KINDS.items():
        row = by_meta.get(meta)
        if row is None:
            errors.append(f"missing host meta {meta}")
        elif row.get("kind") != kind:
            errors.append(f"host {meta} kind {row.get('kind')}, expected {kind}")
    kind_text = KIND.read_text(encoding="utf-8")
    for token in (
        "LARGE_DYNAMO",
        "LIGHTNING_ROD",
        "MATTER_FABRICATOR",
        "VON_DA_GRAAGG",
        "dedicatedController()",
    ):
        if token not in kind_text:
            errors.append(f"MteInPlaceKind missing {token}")
    fusion = FUSION.read_text(encoding="utf-8")
    if "CoilHosts.IRIDIUM" not in fusion:
        errors.append("FusionStructure does not match MTE iridium")
    if "ModBlocks.LARGE_IRIDIUM_COIL" in fusion:
        errors.append("FusionStructure still uses dedicated iridium block")
    if DEDICATED_RECIPE.is_file():
        errors.append("dedicated machines/large_iridium_coil recipe is still generated")
    oven = _load(OVEN)
    coils = (oven.get("palette") or {}).get("N") or {}
    if coils.get("uniform_group") != "oven_coils":
        errors.append("large_oven.json missing oven_coils uniform_group")
    if coils.get("tag") != "cruciblecraft:large_oven_coils":
        errors.append("large_oven.json coil tag drifted")
    recipes = {
        row.get("path"): row
        for row in (_load(ACQUISITION).get("recipes") or [])
    }
    for path in (
        "multiblock/large_copper_coil",
        "multiblock/large_niobium_titanium_coil",
        "multiblock/large_nichrome_coil",
        "multiblock/large_carborundum_coil",
        "multiblock/large_osmium_coil",
        "multiblock/large_iridium_coil",
    ):
        recipe = recipes.get(path)
        if recipe is None:
            errors.append(f"missing acquisition recipe {path}")
            continue
        pattern = recipe.get("pattern") or []
        if pattern != ["WWW", "WxW", "WWW"]:
            errors.append(f"{path} pattern drifted {pattern}")
        wire = ((recipe.get("ingredients") or {}).get("W") or {}).get("item") or ""
        if not wire.endswith("/quadruple_wire"):
            errors.append(f"{path} is not quadruple_wire {wire}")
    overlay = _load(OVERLAY)
    if (overlay.get("counts") or {}).get("in_place") != 73:
        errors.append("multiblock overlay row count drifted from 73")
    if not MANIFEST.is_file():
        errors.append("missing gt6_coil_host_art_manifest.json")
    policy = _load(POLICY)
    blob = json.dumps(policy, ensure_ascii=False)
    if "two temperature tiers" in blob or "coil temperature tiers" in blob:
        errors.append("structure policy still claims oven temperature tiers")
    capability = _load(CAPABILITY) if CAPABILITY.is_file() else {}
    if capability.get("workflow") == "active":
        errors.append("gt6-coil-hosts must not steal unique-active")
    if capability.get("slug") != "machines/gt6-coil-hosts":
        errors.append("gt6-coil-hosts capability slug drifted")
    return errors
