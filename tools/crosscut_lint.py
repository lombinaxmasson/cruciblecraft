#!/usr/bin/env python3
"""Repository-wide checks that used to be copied into individual card tests."""
from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any, Iterable

from tools import io_common as io

# Destination token -> hints that mean the row really is that object.
ART_ALIAS_HINTS = (
    ("multiblock_casing", ("multiblock_casing",)),
    ("pipe_filter_cover", ("pipe_filter", "pipefilter")),
    ("conveyor_cover", ("conveyor",)),
    ("block/furnace", ("furnace",)),
    ("copper_block", ("copper_block", "copperblock")),
)
_BRACE = re.compile(r"\{([^{}]+)\}")
SCATTER_TYPES = (
    "GtItemScatterFeature",
    "GtBlockObjectScatterFeature",
    "GtStoneScatterFeature",
    "ItemScatterConfiguration",
)
SOLE_STAND_IN_ITEMS = frozenset(
    {
        "cruciblecraft:programmed_circuit",
        "cruciblecraft:multiblock_casing",
        "cruciblecraft:pipe_filter_cover",
        "cruciblecraft:conveyor_cover",
        "minecraft:furnace",
        "minecraft:chest",
    }
)
PIPE_TABLE_PARTS = frozenset({"table", "item_table"})


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _relative(root: Path, path: Path) -> str:
    try:
        return path.resolve().relative_to(root.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def _art_rows(document: Any) -> list[dict[str, Any]]:
    if not isinstance(document, dict):
        return []
    rows: list[dict[str, Any]] = []
    for key in ("imports", "rows", "copies"):
        value = document.get(key)
        if isinstance(value, list):
            rows.extend(row for row in value if isinstance(row, dict))
    return rows


def _bases(root: Path, manifest: Path) -> list[Path]:
    bases = [root, root / "src" / "main" / "resources"]
    for parent in manifest.parents:
        bases.append(parent)
        if parent == root:
            break
    unique: list[Path] = []
    for base in bases:
        if base not in unique:
            unique.append(base)
    return unique


def _brace_expand(text: str) -> list[str]:
    match = _BRACE.search(text)
    if match is None:
        return [text]
    prefix, suffix = text[: match.start()], text[match.end() :]
    expanded: list[str] = []
    for option in match.group(1).split(","):
        expanded.extend(_brace_expand(prefix + option.strip() + suffix))
    return expanded


def _pattern_present(root: Path, pattern: str, manifest: Path) -> bool:
    globbing = any(mark in pattern for mark in "*?")
    for base in _bases(root, manifest):
        if globbing:
            matches = list(base.glob(pattern))
            if any(path.is_file() for path in matches):
                return True
            for path in matches:
                if path.is_dir() and any(child.is_file() for child in path.rglob("*")):
                    return True
            continue
        candidate = base / pattern
        if candidate.is_file():
            return True
        if candidate.is_dir() and any(path.is_file() for path in candidate.rglob("*")):
            return True
    return False


def _destination_present(root: Path, destination: str, manifest: Path) -> bool:
    relative = destination.replace("\\", "/").lstrip("/")
    return all(
        _pattern_present(root, pattern, manifest)
        for pattern in _brace_expand(relative)
    )


def _source_blob(row: dict[str, Any], document: dict[str, Any]) -> str:
    parts = [
        str(row.get(key) or "")
        for key in ("source", "gt6_source", "gt6_w", "gt6_class", "note")
    ]
    parts.append(str(document.get("source") or ""))
    return " ".join(parts).replace("\\", "/")


def _source_is_gt6(row: dict[str, Any], document: dict[str, Any]) -> bool:
    blob = _source_blob(row, document)
    return "gregtech6_w" in blob or "assets/gregtech/" in blob


def _alias_tokens(destination: str, row: dict[str, Any], document: dict[str, Any]) -> list[str]:
    dest = destination.replace("\\", "/").lower()
    source = _source_blob(row, document).lower()
    aliased: list[str] = []
    gt6 = "assets/gregtech/" in source or "gregtech6_w" in source
    for token, hints in ART_ALIAS_HINTS:
        in_dest = token in dest
        in_source = token in source
        if not in_dest and not in_source:
            continue
        if in_source and not gt6:
            aliased.append(token)
            continue
        if in_dest and not any(hint in source for hint in hints):
            aliased.append(token)
    return aliased


def art_alias_errors(root: Path | None = None) -> list[str]:
    root = root or io.ROOT
    errors: list[str] = []
    scan_root = root / "src" if (root / "src").is_dir() else root
    manifests = sorted(
        path
        for path in scan_root.rglob("*art_manifest*.json")
        if "gt6_code" not in path.parts and "gt6_referencable_port_code" not in path.parts
    )
    for path in manifests:
        try:
            document = _load(path)
        except json.JSONDecodeError:
            errors.append(f"{_relative(root, path)}: art manifest is not json")
            continue
        relative = _relative(root, path)
        for index, row in enumerate(_art_rows(document)):
            destination = str(row.get("destination") or "")
            label = f"{relative}#{index}"
            if not destination:
                errors.append(f"{label}: art row has no destination")
                continue
            document_dict = document if isinstance(document, dict) else {}
            for token in _alias_tokens(destination, row, document_dict):
                errors.append(f"{label}: art aliases {token}")
            if not _destination_present(root, destination, path):
                errors.append(f"{label}: art destination missing {destination}")
            if not _source_is_gt6(row, document_dict):
                errors.append(f"{label}: art source is not under gregtech6_w")
    return errors


def _item_strings(value: Any) -> Iterable[str]:
    if isinstance(value, str) and ":" in value and not value.startswith("#"):
        yield value
    elif isinstance(value, list):
        for entry in value:
            yield from _item_strings(entry)


def _iter_item_ids(node: Any) -> Iterable[str]:
    if isinstance(node, dict):
        for key in ("item", "id", "items"):
            yield from _item_strings(node.get(key))
        for value in node.values():
            yield from _iter_item_ids(value)
    elif isinstance(node, list):
        for value in node:
            yield from _iter_item_ids(value)


def _ingredient_ids(document: dict[str, Any]) -> list[str]:
    payload = {
        key: value
        for key, value in document.items()
        if key not in {"result", "results"}
    }
    return list(_iter_item_ids(payload))


def _source_text(document: dict[str, Any]) -> str:
    parts: list[str] = []
    for key in ("gt6", "gt6_source", "source", "source_line", "note"):
        value = document.get(key)
        if isinstance(value, str):
            parts.append(value)
    return " ".join(parts)


def recipe_stand_in_errors(root: Path | None = None) -> list[str]:
    root = root or io.ROOT
    errors: list[str] = []
    recipe_dirs = []
    for base in (
        root / "src" / "main" / "resources" / "data",
        root / "src" / "generated" / "resources" / "data",
    ):
        if base.is_dir():
            recipe_dirs.extend(path for path in base.rglob("*.json") if "recipe" in path.parts)
    for path in recipe_dirs:
        try:
            document = _load(path)
        except json.JSONDecodeError:
            continue
        if not isinstance(document, dict):
            continue
        ingredients = _ingredient_ids(document)
        if not ingredients:
            continue
        relative = _relative(root, path)
        source = _source_text(document)
        unique = set(ingredients)
        if len(unique) == 1 and next(iter(unique)) in SOLE_STAND_IN_ITEMS:
            item = next(iter(unique))
            if item not in source:
                errors.append(f"{relative}: recipe stand-in {item}")
        parts = set(path.parts)
        if "pipe" in parts and PIPE_TABLE_PARTS.intersection(parts):
            for item in ingredients:
                if _form(item) == "plate" and item not in source:
                    errors.append(f"{relative}: plate stand-in {item}")
                    break
    return errors


def _form(item_id: str) -> str:
    leaf = item_id.split(":", 1)[-1]
    if "/" in leaf:
        return leaf.split("/", 1)[1]
    return leaf


def scatter_errors(root: Path | None = None) -> list[str]:
    root = root or io.ROOT
    errors: list[str] = []
    java_root = root / "src" / "main" / "java"
    if not java_root.is_dir():
        return errors
    for path in java_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        relative = _relative(root, path)
        for name in SCATTER_TYPES:
            count = text.count(name)
            if count:
                errors.append(f"{relative}: retired scatter type {name} x{count}")
        if "worldgen" in path.parts and "new ItemEntity" in text:
            errors.append(f"{relative}: worldgen constructs ItemEntity")
    return errors


def withdrawn_dummy_errors(root: Path | None = None) -> list[str]:
    """Live recipes and tags must not cite a dummy id that emit already folded away."""
    root = root or io.ROOT
    from tools.recipe_bulk.emit import rewrite_folded_host_runtime

    errors: list[str] = []
    seen: set[str] = set()
    for relative, item in _tag_and_recipe_ids(root):
        if not item.startswith("cruciblecraft:"):
            continue
        live = rewrite_folded_host_runtime(item)
        if live == item:
            continue
        error = f"{relative}: withdrawn dummy id {item} -> {live}"
        if error not in seen:
            seen.add(error)
            errors.append(error)
    return errors


def allowlist_path() -> Path:
    return io.ROOT / "tools" / "crosscut_allowlist.json"


def allowlist_section(section: str, path: Path | None = None) -> list[str]:
    document_path = path or allowlist_path()
    if not document_path.is_file():
        return []
    document = _load(document_path)
    rows = document.get(section) or []
    errors: list[str] = []
    for row in rows:
        if not isinstance(row, dict) or not str(row.get("blocker_id") or "").strip():
            raise ValueError(f"{document_path}: allowlist row missing blocker_id")
        error = str(row.get("error") or "").strip()
        if not error:
            raise ValueError(f"{document_path}: allowlist row missing error")
        errors.append(error)
    return errors


def guarded(section: str, errors: list[str], path: Path | None = None) -> list[str]:
    return apply_allowlist(errors, allowlist_section(section, path))


def _tag_and_recipe_ids(root: Path) -> Iterable[tuple[str, str]]:
    data_roots = (
        root / "src" / "main" / "resources" / "data",
        root / "src" / "generated" / "resources" / "data",
    )
    for base in data_roots:
        if not base.is_dir():
            continue
        for path in base.rglob("*.json"):
            parts = path.parts
            if "recipe" not in parts and "tags" not in parts:
                continue
            try:
                document = _load(path)
            except json.JSONDecodeError:
                continue
            relative = _relative(root, path)
            if "tags" in parts and isinstance(document, dict):
                for value in document.get("values") or []:
                    if isinstance(value, str) and not value.startswith("#") and ":" in value:
                        yield relative, value
                    elif isinstance(value, dict) and isinstance(value.get("id"), str):
                        yield relative, value["id"]
            if "recipe" in parts:
                for item in _iter_item_ids(document):
                    yield relative, item


def apply_allowlist(errors: list[str], allowed: Iterable[str]) -> list[str]:
    """Drop known debt. Every allowlist entry must still be a live error."""
    allowed_list = list(allowed)
    remaining = [error for error in errors if error not in set(allowed_list)]
    stale = [entry for entry in allowed_list if entry not in errors]
    for entry in stale:
        remaining.append(f"allowlist entry is no longer a violation: {entry}")
    return remaining
