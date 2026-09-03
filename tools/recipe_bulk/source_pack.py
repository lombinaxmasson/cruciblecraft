#!/usr/bin/python3
"""Source Pack identity, file hashes, path containment, and replay tier."""
from __future__ import annotations

import hashlib
import re
from pathlib import Path
from typing import Any

from tools import t35_common as t35
from tools.recipe_bulk.schema_lite import SchemaError, validate

SOURCE_REVISION = t35.SOURCE_REVISION
ALLOWED_SOURCE_SYSTEMS = frozenset({"gt6"})
ALLOWED_DIALECTS = frozenset({"gt6"})
FORBIDDEN_SYSTEMS = frozenset({"gt6u", "gt6U", "GT6U"})
FILE_ROLE = frozenset({"dump_slice", "work_set", "authority", "compare_corpus", "other"})
SHA256_HEX = re.compile(r"^[0-9a-f]{64}$")

ALLOWED_ROOTS = (
    t35.TOOLS / "waves",
    t35.ROOT / "src" / "test" / "resources" / "generic_recipe_import",
)

MANIFEST_SCHEMA: dict[str, Any] = {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://cruciblecraft.invalid/schema/source_pack_manifest.json",
    "type": "object",
    "additionalProperties": False,
    "required": [
        "schema_version",
        "source_pack_id",
        "source_system",
        "source_revision",
        "source_dialect",
        "files",
        "provenance_policy",
        "full_replay",
    ],
    "properties": {
        "schema_version": {"const": 1},
        "source_pack_id": {"type": "string", "minLength": 1},
        "source_system": {"type": "string", "enum": ["gt6"]},
        "source_revision": {"type": "string", "minLength": 1},
        "source_dialect": {"type": "string", "enum": ["gt6"]},
        "files": {
            "type": "array",
            "items": {"$ref": "#/$defs/file_entry"},
        },
        "provenance_policy": {
            "type": "object",
            "additionalProperties": False,
            "required": ["append_only", "forbid_gt6u"],
            "properties": {
                "append_only": {"const": True},
                "forbid_gt6u": {"const": True},
            },
        },
        "full_replay": {
            "type": "object",
            "additionalProperties": False,
            "required": ["required_for_first_generation", "skip_is_not_pass"],
            "properties": {
                "required_for_first_generation": {"type": "boolean"},
                "skip_is_not_pass": {"const": True},
            },
        },
    },
    "$defs": {
        "file_entry": {
            "type": "object",
            "additionalProperties": False,
            "required": ["path", "sha256", "role"],
            "properties": {
                "path": {"type": "string", "minLength": 1},
                "sha256": {"type": "string", "minLength": 64, "maxLength": 64},
                "role": {
                    "type": "string",
                    "enum": [
                        "dump_slice",
                        "work_set",
                        "authority",
                        "compare_corpus",
                        "other",
                    ],
                },
            },
        }
    },
}


class SourcePackError(ValueError):
    """Source Pack failed a fail-closed check."""


def schema_path() -> Path:
    return t35.TOOLS / "source_pack_manifest.schema.json"


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def posix_relative(path: Path) -> str:
    return t35.relative(path).replace("\\", "/")


def resolve_contained(raw: str, *, origin: Path | None = None) -> Path:
    text = str(raw or "").replace("\\", "/").strip()
    if not text:
        raise SourcePackError("empty source pack path")
    if text.startswith("/") or re.match(r"^[A-Za-z]:/", text):
        raise SourcePackError(f"absolute path rejected: {text}")
    candidate = (origin or t35.ROOT).joinpath(*text.split("/"))
    try:
        resolved = candidate.resolve()
        root = t35.ROOT.resolve()
        resolved.relative_to(root)
    except (OSError, ValueError) as error:
        raise SourcePackError(f"path escape rejected: {text}") from error
    if ".." in Path(text).parts:
        raise SourcePackError(f"path escape rejected: {text}")
    allowed = False
    for base in ALLOWED_ROOTS:
        try:
            resolved.relative_to(base.resolve())
            allowed = True
            break
        except ValueError:
            continue
    if not allowed:
        raise SourcePackError(f"path outside allowed roots: {text}")
    return resolved


def validate_manifest_document(document: dict[str, Any]) -> None:
    try:
        validate(document, MANIFEST_SCHEMA)
    except SchemaError as error:
        raise SourcePackError(str(error)) from error
    system = str(document.get("source_system") or "")
    if system in FORBIDDEN_SYSTEMS or system not in ALLOWED_SOURCE_SYSTEMS:
        raise SourcePackError(f"source_system {system!r} is not allowed")
    dialect = str(document.get("source_dialect") or "")
    if dialect not in ALLOWED_DIALECTS:
        raise SourcePackError(f"unknown adapter/dialect {dialect!r}")
    revision = str(document.get("source_revision") or "")
    if revision != SOURCE_REVISION:
        raise SourcePackError(
            f"source_revision {revision} != pinned {SOURCE_REVISION}"
        )
    files = document.get("files") or []
    if not files:
        raise SourcePackError("source pack files[] is empty")
    seen_paths: set[str] = set()
    for entry in files:
        path = str(entry["path"])
        digest = str(entry["sha256"])
        if path in seen_paths:
            raise SourcePackError(f"duplicate source pack path {path}")
        seen_paths.add(path)
        if not SHA256_HEX.fullmatch(digest):
            raise SourcePackError(f"invalid sha256 for {path}")
        if str(entry.get("role") or "") not in FILE_ROLE:
            raise SourcePackError(f"unknown file role for {path}")


def load_manifest(path: Path) -> dict[str, Any]:
    document = t35.load_json(path)
    if not isinstance(document, dict):
        raise SourcePackError("source pack manifest must be an object")
    validate_manifest_document(document)
    return document


def verify_files(
    manifest: dict[str, Any],
    *,
    origin: Path | None = None,
    require_present: bool,
) -> dict[str, Path]:
    resolved: dict[str, Path] = {}
    missing: list[str] = []
    drifted: list[str] = []
    for entry in manifest.get("files") or []:
        rel = str(entry["path"])
        path = resolve_contained(rel, origin=origin)
        resolved[rel] = path
        if not path.is_file():
            missing.append(rel)
            continue
        actual = sha256_file(path)
        expected = str(entry["sha256"])
        if actual != expected:
            drifted.append(rel)
    if drifted:
        raise SourcePackError("source pack hash drift: " + ",".join(drifted))
    if missing and require_present:
        skip_is_not_pass = bool(
            (manifest.get("full_replay") or {}).get("skip_is_not_pass")
        )
        if skip_is_not_pass:
            raise SourcePackError(
                "full replay missing source files and skip_is_not_pass: "
                + ",".join(missing)
            )
        raise SourcePackError("source pack files missing: " + ",".join(missing))
    return resolved


def replay_tier(
    manifest: dict[str, Any],
    resolved: dict[str, Path],
    *,
    first_generation: bool,
) -> str:
    missing = [rel for rel, path in resolved.items() if not path.is_file()]
    full = manifest.get("full_replay") or {}
    if missing:
        if first_generation and full.get("required_for_first_generation"):
            raise SourcePackError(
                "full replay required for first generation; missing "
                + ",".join(missing)
            )
        if full.get("skip_is_not_pass"):
            raise SourcePackError(
                "missing source is not PASS: " + ",".join(missing)
            )
        return "reference_only"
    return "full_replay"


def files_by_role(manifest: dict[str, Any], role: str) -> list[str]:
    return [
        str(entry["path"])
        for entry in manifest.get("files") or []
        if str(entry.get("role") or "") == role
    ]
