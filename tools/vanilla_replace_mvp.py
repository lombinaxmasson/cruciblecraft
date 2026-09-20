#!/usr/bin/env python3
"""content/vanilla-replace-mvp lock currentness."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

from tools import closeout_seal
from tools import census_common as census

SLUG = "content/vanilla-replace-mvp"
STATUS = "VANILLA_REPLACE_MVP_READY"
R0_SLUG = "portfolio/vanilla-replace-r0"
R0_STATUS = "VANILLA_REPLACE_R0_READY"
SOURCE_REVISION = census.SOURCE_REVISION
VANILLA_BLOB = "4c459acd2c7729d4186c5ada9ccd76181745bacf"
GENERATED_BY = "python tools/build_vanilla_replace_mvp.py"
WAVE_DIR = census.TOOLS / "waves" / "content" / "vanilla-replace-mvp"
LOCK_PATH = WAVE_DIR / "vanilla_replace_lock.json"
READINESS_PATH = WAVE_DIR / "readiness.json"
RECIPE_GENERATED = census.ROOT / "src" / "recipe_generated"
MINECRAFT_RECIPE_ROOTS = (
    census.ROOT / "src" / "main" / "resources" / "data" / "minecraft" / "recipe",
    census.ROOT / "src" / "generated" / "resources" / "data" / "minecraft" / "recipe",
)
# GT6 Loader_Recipes_Woods NERFED_WOOD hand-craft (log → 2 planks).
# Not the paper MVP; same overlay folder because recipe ids are minecraft:*.
WOOD_PLANK_MINECRAFT_OVERLAYS = tuple(
    f"src/main/resources/data/minecraft/recipe/{stem}_planks.json"
    for stem in (
        "oak",
        "spruce",
        "birch",
        "jungle",
        "acacia",
        "dark_oak",
        "mangrove",
        "cherry",
        "crimson",
        "warped",
    )
)
POINTER_KEYS = ("file", "files", "lines", "calls", "note")


def load_lock() -> dict[str, Any]:
    if not LOCK_PATH.is_file():
        raise ValueError(f"missing lock: {LOCK_PATH.as_posix()}")
    return census.load_json(LOCK_PATH)


def pointer_ok(pointer: Any) -> bool:
    if not isinstance(pointer, dict) or not pointer:
        return False
    return any(pointer.get(key) for key in POINTER_KEYS)


def minecraft_recipe_files() -> list[Path]:
    hits: list[Path] = []
    for root in MINECRAFT_RECIPE_ROOTS:
        if not root.is_dir():
            continue
        hits.extend(path for path in sorted(root.rglob("*")) if path.is_file())
    return hits


def owned_minecraft_paths(lock: dict[str, Any]) -> set[str]:
    owned: set[str] = set()
    for row in list(lock.get("removed") or []) + list(lock.get("substituted") or []):
        path = str(row.get("datapack_path") or "")
        if path:
            owned.add(path.replace("\\", "/"))
    owned.update(WOOD_PLANK_MINECRAFT_OVERLAYS)
    return owned


def check_row_io(row: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    path = census.ROOT / str(row.get("datapack_path") or "")
    recipe_id = str(row.get("recipe_id") or "")
    if not path.is_file():
        errors.append(f"{recipe_id} missing datapack {path.as_posix()}")
        return errors
    document = census.load_json(path)
    expected = row.get("io") or {}
    result = document.get("result") or {}
    want = expected.get("result") or {}
    if result.get("id") != want.get("id"):
        errors.append(f"{recipe_id} result id {result.get('id')} != {want.get('id')}")
    if int(result.get("count") or -1) != int(want.get("count") or -2):
        errors.append(
            f"{recipe_id} result count {result.get('count')} != {want.get('count')}"
        )
    if list(document.get("pattern") or []) != list(expected.get("pattern") or []):
        errors.append(f"{recipe_id} pattern drifted from lock io")
    key = document.get("key") or {}
    want_key = expected.get("key") or {}
    for symbol, item in want_key.items():
        live = (key.get(symbol) or {}).get("item")
        if live != item:
            errors.append(f"{recipe_id} key {symbol} {live} != {item}")
    if "neoforge:conditions" in document:
        errors.append(f"{recipe_id} substitute must load, not disable")
    return errors


def check_removed_row(row: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    path = census.ROOT / str(row.get("datapack_path") or "")
    recipe_id = str(row.get("recipe_id") or "")
    if not path.is_file():
        errors.append(f"{recipe_id} missing disable datapack {path.as_posix()}")
        return errors
    document = census.load_json(path)
    conditions = document.get("neoforge:conditions") or []
    if not any(
        isinstance(row, dict) and row.get("type") == "neoforge:false"
        for row in conditions
    ):
        errors.append(f"{recipe_id} remove datapack is not neoforge:false")
    return errors


def check_lock(lock: dict[str, Any] | None = None) -> list[str]:
    document = lock if lock is not None else load_lock()
    errors: list[str] = []
    if document.get("wave_slug") != SLUG:
        errors.append("lock wave_slug drifted")
    if document.get("status") != STATUS:
        errors.append(f"lock status {document.get('status')} != {STATUS}")
    if document.get("source_revision") != SOURCE_REVISION:
        errors.append("lock source_revision drifted")
    if document.get("source_blob_sha1") != VANILLA_BLOB:
        errors.append("lock Vanilla.java blob drifted")
    if document.get("owns_families") != 0:
        errors.append("owns_families must be 0")
    if document.get("generated_recipe_count") != 0:
        errors.append("generated_recipe_count must be 0")
    if document.get("production_lock") is not None:
        errors.append("production_lock must be null")
    if document.get("nuclear_started"):
        errors.append("nuclear_started must be false")

    for bucket in (
        "removed",
        "added",
        "substituted",
        "deferred",
        "no_1_21_equivalent",
        "identical_1_21",
        "out_of_scope",
        "supporting_tags",
    ):
        rows = document.get(bucket)
        if not isinstance(rows, list):
            errors.append(f"lock {bucket} must be a list")
            continue
        for index, row in enumerate(rows):
            if not isinstance(row, dict):
                errors.append(f"{bucket}[{index}] is not an object")
                continue
            if not pointer_ok(row.get("gt6_pointer")) and bucket != "out_of_scope":
                if not row.get("reason"):
                    errors.append(f"{bucket}[{index}] needs gt6_pointer or reason")

    if document.get("removed"):
        for row in document["removed"]:
            errors.extend(check_removed_row(row))
    if document.get("added"):
        for row in document["added"]:
            path = census.ROOT / str(row.get("datapack_path") or "")
            if not path.is_file():
                errors.append(f"added {row.get('recipe_id')} missing {path.as_posix()}")
    for row in document.get("substituted") or []:
        if not row.get("recipe_id"):
            errors.append("substituted row missing recipe_id")
        errors.extend(check_row_io(row))

    owned = owned_minecraft_paths(document)
    for path in minecraft_recipe_files():
        relative = census.relative(path).replace("\\", "/")
        if relative not in owned:
            errors.append(f"lock-outside minecraft recipe: {relative}")
    for relative in WOOD_PLANK_MINECRAFT_OVERLAYS:
        overlay = census.ROOT / relative
        if not overlay.is_file():
            errors.append(f"missing wood overlay {relative}")
            continue
        document_json = census.load_json(overlay)
        result = document_json.get("result") or {}
        if int(result.get("count") or -1) != 2:
            errors.append(
                f"{relative} must hand-craft 2 planks, got {result.get('count')}"
            )

    if RECIPE_GENERATED.is_dir():
        for path in RECIPE_GENERATED.rglob("*"):
            if SLUG.replace("/", "-") in path.as_posix() or "vanilla-replace-mvp" in path.as_posix():
                errors.append(f"recipe_generated mentions mvp: {path.as_posix()}")

    for tag in document.get("supporting_tags") or []:
        path = census.ROOT / str(tag.get("datapack_path") or "")
        if not path.is_file():
            errors.append(f"missing tag {path.as_posix()}")
            continue
        live = census.load_json(path)
        values = [str(item) for item in live.get("values") or []]
        expected = [str(item) for item in tag.get("members") or []]
        if values != expected:
            errors.append(f"{tag.get('tag_id')} members {values} != {expected}")
        if not pointer_ok(tag.get("gt6_pointer")):
            errors.append(f"{tag.get('tag_id')} missing gt6_pointer")
    return errors


def check_r0_seal() -> list[str]:
    errors = closeout_seal.check_wave_seal(R0_SLUG)
    root = census.TOOLS / "waves" / "portfolio" / "vanilla-replace-r0"
    mechanism = census.load_json(root / "existing_mechanism.json")
    if mechanism.get("minecraft_recipe_override_count") != 0:
        errors.append("R0 existing_mechanism override count was rewritten")
    readiness = census.load_json(root / "readiness.json")
    if readiness.get("status") != R0_STATUS:
        errors.append("R0 readiness status drifted")
    evidence = readiness.get("evidence") or {}
    if evidence.get("minecraft_recipe_override_count") != 0:
        errors.append("R0 readiness override count was rewritten")
    return errors


def readiness_document() -> dict[str, Any]:
    lock = load_lock()
    return {
        "generated_by": GENERATED_BY,
        "generated_recipe_count": 0,
        "next_unassigned": True,
        "nuclear_started": False,
        "owns_families": 0,
        "production_lock": None,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": STATUS,
        "substituted_count": len(lock.get("substituted") or []),
        "unique_active_wave": None,
        "wave_slug": SLUG,
    }


def write_readiness() -> dict[str, Any]:
    WAVE_DIR.mkdir(parents=True, exist_ok=True)
    document = readiness_document()
    census.write_stable(READINESS_PATH, document)
    return document


def check_readiness() -> list[str]:
    if not READINESS_PATH.is_file():
        return [f"missing {READINESS_PATH.as_posix()}"]
    live = readiness_document()
    committed = census.load_json(READINESS_PATH)
    drift = census.first_json_diff(live, committed)
    if drift:
        return [f"readiness drifted: {drift}"]
    if committed.get("unique_active_wave") is not None:
        return ["unique_active_wave must be null at close"]
    if not committed.get("next_unassigned"):
        return ["next_unassigned must be true"]
    return []


def check_artifacts() -> list[str]:
    errors = check_lock()
    errors.extend(check_readiness())
    errors.extend(check_r0_seal())
    return errors


def write_artifacts() -> dict[str, Any]:
    errors = check_lock()
    if errors:
        raise ValueError("; ".join(errors))
    readiness = write_readiness()
    leftover = check_r0_seal()
    if leftover:
        raise ValueError("; ".join(leftover))
    return {
        "status": STATUS,
        "unique_active_wave": readiness["unique_active_wave"],
        "wave_slug": SLUG,
    }


def main_for(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=f"Write or check {SLUG}.")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            print(json.dumps(write_artifacts(), sort_keys=True))
            return 0
        errors = check_artifacts()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{SLUG} lock derivation is current")
        return 0
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1
