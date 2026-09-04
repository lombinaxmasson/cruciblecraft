#!/usr/bin/python3
"""Refuse production/generated writes from the unified shadow pipeline."""
from __future__ import annotations

import fnmatch
import json
from pathlib import Path

from tools import io_common as files

ROOT = files.ROOT
TOOLS = files.TOOLS
MANIFEST = TOOLS / "authority_manifest.json"
SHADOW_IR_ROOT = ROOT / "build" / "shadow_ir"

ALLOWED_LEDGER_RELATIVE = frozenset(
    {
        "tools/recipe_wave_production_baseline.json",
        "tools/global_build_identity_ledger.json",
        "tools/recipe_wave_shadow_parity.json",
        "tools/unified_import_shadow_readiness.json",
        "tools/compact_recipe_runtime_manifest.json",
        "tools/compact_recipe_manifest_cutover_readiness.json",
        "tools/recipe_wave_production_baseline.currentness.json",
        "tools/global_build_identity_ledger.currentness.json",
        "tools/recipe_wave_shadow_parity.currentness.json",
        "tools/unified_import_shadow_readiness.currentness.json",
        "tools/compact_recipe_runtime_manifest.currentness.json",
        "tools/compact_recipe_manifest_cutover_readiness.currentness.json",
        "tools/unified_recipe_compile_readiness.json",
        "tools/unified_recipe_compile_readiness.currentness.json",
    }
)


class ProductionWriteError(PermissionError):
    """Shadow or ledger builders tried to mutate a production generated root."""


def relative(path: Path) -> str:
    return files.relative(Path(path))


def protected_globs() -> tuple[str, ...]:
    document = json.loads(MANIFEST.read_text(encoding="utf-8"))
    return tuple(document.get("protected_path_globs") or [])


def is_authority_protected(path: Path) -> bool:
    try:
        relative_path = Path(path).resolve().relative_to(ROOT.resolve()).as_posix()
    except (OSError, ValueError):
        return False
    return any(
        fnmatch.fnmatchcase(relative_path, glob) for glob in protected_globs()
    )


def is_production_generated(path: Path) -> bool:
    relative_path = relative(path)
    return (
        "_recipe_generated/" in relative_path
        or relative_path.startswith("src/recipe_generated/")
        or relative_path.startswith("src/recipe_support_generated/")
        or relative_path.startswith("src/main/java/")
    )


def is_shadow_ir(path: Path) -> bool:
    try:
        Path(path).resolve().relative_to(SHADOW_IR_ROOT.resolve())
        return True
    except (OSError, ValueError):
        return False


def assert_not_production_write(path: Path) -> None:
    if is_production_generated(path):
        raise ProductionWriteError(
            "unified import shadow must not write production generated roots: "
            + relative(path)
        )
    if is_shadow_ir(path):
        return
    relative_path = relative(path)
    if is_authority_protected(path) and relative_path not in ALLOWED_LEDGER_RELATIVE:
        raise ProductionWriteError(
            "unified import shadow must not write protected authority path: "
            + relative_path
        )


def assert_ledger_write(path: Path) -> None:
    relative_path = relative(path)
    if relative_path not in ALLOWED_LEDGER_RELATIVE:
        raise ProductionWriteError(
            f"ledger builder may only write committed ledgers, not {relative_path}"
        )
    if is_production_generated(path):
        raise ProductionWriteError(
            "ledger builder must not write production generated roots: "
            + relative_path
        )


def shadow_ir_path(wave_id: str) -> Path:
    return SHADOW_IR_ROOT / f"{wave_id.lower()}.json"


def assert_legacy_production_write_forbidden(wave_id: str) -> None:
    raise ProductionWriteError(
        f"{wave_id} production family roots can only be written by "
        "python tools/build_recipe_bulk.py compile --write"
    )
