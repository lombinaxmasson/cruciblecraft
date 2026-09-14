#!/usr/bin/env python3
"""Canonical translation keys and player display names."""
from __future__ import annotations

import json
import re
from functools import lru_cache
from pathlib import Path
from typing import Any

from tools import io_common as io

CONTRACT_PATH = (
    io.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "language_display_contract.json"
)
NAMESPACE = "cruciblecraft"
REGISTRY_OBJECT_TYPES = ("block", "item", "fluid", "fluid_type")
CJK_RE = re.compile(r"[\u4e00-\u9fff]")
LATIN_RE = re.compile(r"[A-Za-z]")
WORD_RE = re.compile(r"[A-Za-z0-9]+")
META_SUFFIX_RE = re.compile(r"\s+m\d+$", re.IGNORECASE)
REGISTRY_KEY_RE = re.compile(
    r"^(block|item|fluid|fluid_type)\.cruciblecraft\.(.+)$"
)
TEMPLATE_KEY_PREFIXES = (
    "item.cruciblecraft.material_form.",
    "material.cruciblecraft.",
    "screen.cruciblecraft.",
    "tooltip.cruciblecraft.",
    "message.cruciblecraft.",
    "disconnect.cruciblecraft.",
    "emi.",
    "jade.cruciblecraft.",
    "config.jade.",
    "container.cruciblecraft.",
    "itemGroup.cruciblecraft.",
    "cruciblecraft.configuration.",
)


@lru_cache(maxsize=1)
def contract() -> dict[str, Any]:
    return json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))


def translation_key(
    object_type: str,
    registry_path: str,
    *,
    namespace: str = NAMESPACE,
) -> str:
    if object_type not in REGISTRY_OBJECT_TYPES:
        raise ValueError(f"unsupported object type: {object_type}")
    if not registry_path:
        raise ValueError("registry_path is empty")
    return f"{object_type}.{namespace}.{registry_path.replace('/', '.')}"


def is_template_key(key: str) -> bool:
    return any(key.startswith(prefix) for prefix in TEMPLATE_KEY_PREFIXES)


def registry_backed_match(key: str) -> re.Match[str] | None:
    return REGISTRY_KEY_RE.match(key)


def is_illegal_slash_key(key: str) -> bool:
    match = registry_backed_match(key)
    return match is not None and "/" in match.group(2)


def format_english_id(value: str) -> str:
    if not value:
        raise ValueError("empty id")
    table = contract()
    prefixes = table["prefix_exceptions"]
    if value in prefixes:
        return prefixes[value]
    tokens = table["token_exceptions"]
    parts = [part for part in value.replace("-", "_").split("_") if part]
    rendered: list[str] = []
    for part in parts:
        mapped = tokens.get(part.lower())
        if mapped:
            rendered.append(mapped)
            continue
        rendered.append(part[:1].upper() + part[1:].lower())
    return " ".join(rendered)


def compose_english(material_id: str, form_id: str) -> str:
    return f"{format_english_id(material_id)} {format_english_id(form_id)}"


def has_cjk(text: str) -> bool:
    return bool(CJK_RE.search(text or ""))


def is_english_copy(zh: str, en: str | None = None) -> bool:
    if not zh or not str(zh).strip():
        return False
    value = str(zh).strip()
    if has_cjk(value):
        return False
    if "%s" in value:
        return False
    if en is not None and value == str(en).strip():
        return True
    return bool(LATIN_RE.search(value))


def word_tokens(text: str) -> set[str]:
    return {match.group(0).lower() for match in WORD_RE.finditer(text or "")}


def segment_label(segment: str) -> str:
    table = contract()
    faces = table["slab_faces"]
    if segment in faces:
        return faces[segment]
    return format_english_id(segment)


def _semantic_tokens(tokens: set[str]) -> set[str]:
    return {token for token in tokens if not token.isdigit()}


def _is_alphabetic_variant(segment: str) -> bool:
    letters = any(char.isalpha() for char in segment)
    digits = any(char.isdigit() for char in segment)
    return letters and not digits


def _is_discriminator(token: str) -> bool:
    table = contract()
    if token in table.get("token_exceptions", {}) or token in table.get("dye_colors", []):
        return True
    if token in table.get("slab_faces", {}) or token in table.get("zh_path_tokens", {}):
        return True
    if len(token) == 1 and token.isalpha():
        return True
    return token.isdigit() and len(token) <= 2


def player_english(source_name: str, registry_path: str) -> str:
    name = META_SUFFIX_RE.sub("", source_name or "").strip()
    if not name:
        name = format_english_id(registry_path.replace("/", "_"))
    extras: list[str] = []
    owned = word_tokens(name)
    first = True
    for segment in registry_path.replace(".", "/").split("/"):
        if not segment:
            continue
        label = segment_label(segment)
        unused = word_tokens(label) - owned
        keep = _semantic_tokens(unused)
        keep |= {token for token in unused if token.isdigit() and len(token) <= 2}
        if first:
            keep = {token for token in keep if _is_discriminator(token)}
        first = False
        if not keep or len(keep) > 3:
            continue
        parts = [
            word
            for word in label.split(" ")
            if word and (word_tokens(word) & keep)
        ]
        if not parts:
            continue
        extras.append(" ".join(parts))
        owned |= keep
    if extras:
        return name + " " + " ".join(extras)
    return name


def player_chinese(source_name: str, registry_path: str) -> str | None:
    if not source_name or not has_cjk(source_name):
        return None
    table = contract()["zh_path_tokens"]
    name = META_SUFFIX_RE.sub("", source_name).strip()
    extras: list[str] = []
    for segment in registry_path.replace(".", "/").split("/"):
        if not segment:
            continue
        mapped = table.get(segment)
        if mapped is None:
            if _is_alphabetic_variant(segment):
                return None
            continue
        if mapped and mapped not in name and mapped not in extras:
            extras.append(mapped)
    return name + "".join(extras)


@lru_cache(maxsize=1)
def material_zh_table() -> dict[str, Any]:
    path = (
        io.ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "material_zh_cn.json"
    )
    return json.loads(path.read_text(encoding="utf-8"))


def compose_material_form_zh(registry_path: str) -> str | None:
    if not registry_path or registry_path.count("/") != 1:
        return None
    material, form = registry_path.split("/", 1)
    table = material_zh_table()
    mat = (table.get("materials") or {}).get(material)
    if not mat:
        return None
    name = (
        (table.get("prefixes") or {}).get(form)
        or (table.get("pipes") or {}).get(form)
        or (table.get("conductors") or {}).get(form)
    )
    if not name:
        return None
    return mat + name


def identity_group(key: str) -> tuple[str, ...]:
    match = registry_backed_match(key)
    if match is None:
        return ("other", key)
    kind, path = match.group(1), match.group(2)
    if kind in {"block", "item"}:
        return ("block_item", path)
    if kind in {"fluid", "fluid_type"}:
        return ("fluid", path)
    return ("other", key)


def allowlisted_names(locale: str | None = None) -> set[str]:
    names: set[str] = set()
    for row in contract().get("allowlist") or []:
        if locale and row.get("locale") not in {locale, "*"}:
            continue
        name = row.get("display_name")
        if isinstance(name, str) and name:
            names.add(name)
    return names


def grouped_collisions(
    table: dict[str, str],
    *,
    locale: str | None = None,
) -> list[dict[str, Any]]:
    allowed = allowlisted_names(locale)
    by_name: dict[str, dict[tuple[str, ...], list[str]]] = {}
    for key, raw in table.items():
        if not isinstance(raw, str) or not raw:
            continue
        if is_template_key(key):
            continue
        if registry_backed_match(key) is None:
            continue
        group = identity_group(key)
        by_name.setdefault(raw, {}).setdefault(group, []).append(key)
    collisions: list[dict[str, Any]] = []
    for name, groups in by_name.items():
        if len(groups) < 2 or name in allowed:
            continue
        collisions.append(
            {
                "display_name": name,
                "identities": [
                    {"group": list(group), "keys": sorted(keys)}
                    for group, keys in sorted(groups.items())
                ],
            }
        )
    collisions.sort(key=lambda row: (-len(row["identities"]), row["display_name"]))
    return collisions
