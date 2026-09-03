#!/usr/bin/python3
"""RecipeImportSpec schema, typed checks, and fail-closed field gates."""
from __future__ import annotations

import re
from pathlib import Path
from typing import Any

from tools import t35_common as t35
from tools.recipe_bulk.schema_lite import SchemaError, validate
from tools.recipe_bulk.slugs import SCHEMA_SEMANTIC, WaveSlugError, parse_wave_token
from tools.recipe_bulk.source_pack import SourcePackError, resolve_contained

MOD_RECIPE_MAPS_JAVA = (
    t35.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModRecipeMaps.java"
)
REGISTERED_DATA_AUTHORITIES = frozenset(
    {
        "identity_ledger_v3",
        "material_form_authority",
    }
)
REGISTERED_DIALECTS = frozenset({"gt6"})
ALLOWED_REPRESENTATIONS = frozenset({"exact", "exact_multi"})
FORBIDDEN_SPEC_KEYS = frozenset(
    {
        "publication_winner",
        "publication_group_manifest",
        "shard",
        "shard_manifest",
        "load",
        "player_path",
        "census",
        "census_completion",
        "production_lock",
        "game_test",
        "gametest",
    }
)
MEMBERSHIP_KINDS = frozenset({"work_set", "frozen_ledger"})
SELECTION_KINDS = frozenset({"work_set_members"})
CREATE_MAP = re.compile(r"RecipeMap\s+(\w+)\s+=\s+create\(\"([a-z0-9_]+)\"\)")
ALL_LIST = re.compile(r"List<RecipeMap>\s+ALL\s+=\s+List\.of\((.*?)\);", re.S)

IMPORT_SPEC_SCHEMA: dict[str, Any] = {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://cruciblecraft.invalid/schema/recipe_import_spec.json",
    "type": "object",
    "additionalProperties": False,
    "required": [
        "schema_version",
        "import_slug",
        "source_pack",
        "source_maps",
        "target_map",
        "host",
        "family_membership_source",
        "selection_rule",
        "operand_authorities",
        "representation_policy",
        "stable_id_policy",
        "output_paths",
    ],
    "properties": {
        "schema_version": {"const": 1},
        "import_slug": {"type": "string", "minLength": 1},
        "source_pack": {"type": "string", "minLength": 1},
        "source_maps": {
            "type": "array",
            "items": {"type": "string", "minLength": 1},
        },
        "target_map": {"type": "string", "minLength": 1},
        "host": {"type": "string", "minLength": 1},
        "family_membership_source": {
            "type": "object",
            "additionalProperties": False,
            "required": ["kind", "path"],
            "properties": {
                "kind": {"type": "string", "enum": ["work_set", "frozen_ledger"]},
                "path": {"type": "string", "minLength": 1},
            },
        },
        "selection_rule": {
            "type": "object",
            "additionalProperties": False,
            "required": ["kind"],
            "properties": {
                "kind": {"type": "string", "enum": ["work_set_members"]},
            },
        },
        "operand_authorities": {
            "type": "array",
            "items": {"$ref": "#/$defs/authority"},
        },
        "representation_policy": {
            "type": "object",
            "additionalProperties": False,
            "required": ["allowed"],
            "properties": {
                "allowed": {
                    "type": "array",
                    "items": {"type": "string", "enum": ["exact", "exact_multi"]},
                }
            },
        },
        "stable_id_policy": {
            "type": "object",
            "additionalProperties": False,
            "required": ["algorithm", "include_card_number"],
            "properties": {
                "algorithm": {
                    "const": "source_system_revision_family_relation",
                },
                "include_card_number": {"const": False},
            },
        },
        "output_paths": {
            "type": "object",
            "additionalProperties": False,
            "required": ["source", "receipt", "review", "lock_candidate"],
            "properties": {
                "source": {"type": "string", "minLength": 1},
                "receipt": {"type": "string", "minLength": 1},
                "review": {"type": "string", "minLength": 1},
                "lock_candidate": {"type": "string", "minLength": 1},
            },
        },
        "compare_corpus": {"type": "string", "minLength": 1},
    },
    "$defs": {
        "authority": {
            "type": "object",
            "additionalProperties": False,
            "required": ["kind"],
            "properties": {
                "kind": {"type": "string", "enum": ["data", "source_dialect"]},
                "id": {"type": "string"},
                "adapter": {"type": "string"},
            },
        }
    },
}


class ImportSpecError(ValueError):
    """RecipeImportSpec failed a fail-closed check."""


def schema_path() -> Path:
    return t35.TOOLS / "recipe_import_spec.schema.json"


def existing_recipe_maps() -> dict[str, str]:
    text = MOD_RECIPE_MAPS_JAVA.read_text(encoding="utf-8")
    created = {name: map_id for name, map_id in CREATE_MAP.findall(text)}
    match = ALL_LIST.search(text)
    if not match:
        raise ImportSpecError("ModRecipeMaps.ALL is missing")
    names = [part.strip().strip(",") for part in match.group(1).splitlines()]
    names = [name for name in names if name]
    maps: dict[str, str] = {}
    for name in names:
        map_id = created.get(name)
        if not map_id:
            raise ImportSpecError(f"ModRecipeMaps.ALL entry {name} has no create()")
        maps[f"cruciblecraft:{map_id}"] = map_id
    return maps


def existing_hosts() -> set[str]:
    return set(existing_recipe_maps())


def validate_import_spec_document(document: dict[str, Any]) -> None:
    extra = sorted(set(document) - set(IMPORT_SPEC_SCHEMA["properties"]))
    if extra:
        raise ImportSpecError(f"unknown field {extra[0]}")
    forbidden = sorted(set(document) & FORBIDDEN_SPEC_KEYS)
    if forbidden:
        raise ImportSpecError(f"import spec forbids {forbidden[0]}")
    try:
        validate(document, IMPORT_SPEC_SCHEMA)
    except SchemaError as error:
        raise ImportSpecError(str(error)) from error
    slug = str(document["import_slug"])
    try:
        parsed = parse_wave_token(slug, schema=SCHEMA_SEMANTIC)
    except WaveSlugError as error:
        raise ImportSpecError(str(error)) from error
    if parsed.wave_slug != slug:
        raise ImportSpecError(f"invalid import_slug {slug}")
    maps = existing_recipe_maps()
    target = str(document["target_map"])
    if target not in maps:
        raise ImportSpecError(f"unknown target map {target}")
    host = str(document["host"])
    if host not in existing_hosts():
        raise ImportSpecError(f"unknown host {host}")
    if host != target:
        raise ImportSpecError(f"host {host} must match target_map {target}")
    source_maps = list(document.get("source_maps") or [])
    if not source_maps:
        raise ImportSpecError("source_maps is empty")
    allowed = set(document["representation_policy"]["allowed"])
    if not allowed or allowed - ALLOWED_REPRESENTATIONS:
        raise ImportSpecError("representation_policy.allowed must be exact/exact_multi")
    membership = document["family_membership_source"]
    if str(membership.get("kind") or "") not in MEMBERSHIP_KINDS:
        raise ImportSpecError("unknown family_membership_source.kind")
    if str(document["selection_rule"].get("kind") or "") not in SELECTION_KINDS:
        raise ImportSpecError("unknown selection_rule.kind")
    authorities = document.get("operand_authorities") or []
    if not authorities:
        raise ImportSpecError("operand_authorities is empty")
    seen_dialect = False
    for row in authorities:
        kind = str(row.get("kind") or "")
        if kind == "data":
            authority_id = str(row.get("id") or "")
            if authority_id not in REGISTERED_DATA_AUTHORITIES:
                raise ImportSpecError(f"unregistered data authority {authority_id}")
        elif kind == "source_dialect":
            adapter = str(row.get("adapter") or "")
            if adapter not in REGISTERED_DIALECTS:
                raise ImportSpecError(f"unknown adapter {adapter}")
            seen_dialect = True
        else:
            raise ImportSpecError(f"operand authority cannot be a per-card delegate: {kind}")
    if not seen_dialect:
        raise ImportSpecError("operand_authorities must include a source_dialect adapter")
    if bool(document["stable_id_policy"].get("include_card_number")):
        raise ImportSpecError("stable_id_policy must not include card numbers")


def load_import_spec(path: Path) -> dict[str, Any]:
    document = t35.load_json(path)
    if not isinstance(document, dict):
        raise ImportSpecError("recipe import spec must be an object")
    validate_import_spec_document(document)
    try:
        for key in ("source_pack", "compare_corpus"):
            raw = document.get(key)
            if raw:
                resolve_contained(str(raw))
        membership = document["family_membership_source"]["path"]
        resolve_contained(str(membership))
        output_rows = list(document["output_paths"].items())
    except SourcePackError as error:
        raise ImportSpecError(str(error)) from error
    for key, raw in output_rows:
        try:
            resolved = resolve_contained(str(raw))
        except SourcePackError as error:
            raise ImportSpecError(str(error)) from error
        name = resolved.name
        if name == "production_lock.json":
            raise ImportSpecError("output_paths cannot write production_lock.json")
        if "recipe_generated" in resolved.as_posix():
            raise ImportSpecError("import must not write src/recipe_generated")
        if name in {
            "publication_group_manifest.json",
            "shard_manifest.json",
            "census_delta.json",
        }:
            raise ImportSpecError(f"output_paths cannot write {name}")
        if key == "lock_candidate" and name != "lock_candidate.json":
            raise ImportSpecError("lock_candidate output must be lock_candidate.json")
    return document


def dialect_name(document: dict[str, Any]) -> str:
    for row in document.get("operand_authorities") or []:
        if row.get("kind") == "source_dialect":
            return str(row.get("adapter") or "")
    raise ImportSpecError("missing source_dialect adapter")
