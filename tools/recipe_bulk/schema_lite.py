#!/usr/bin/python3
"""Minimal JSON Schema subset used by recipe-wave ledgers (no jsonschema dep)."""
from __future__ import annotations

from typing import Any


class SchemaError(ValueError):
    """Document failed a schema-lite check."""


def validate(document: Any, schema: dict[str, Any], path: str = "$") -> None:
    errors = collect_errors(document, schema, schema, path)
    if errors:
        raise SchemaError("; ".join(errors[:8]))


def collect_errors(
    document: Any,
    schema: dict[str, Any],
    root: dict[str, Any],
    path: str,
) -> list[str]:
    schema = _resolve(schema, root)
    type_spec = schema.get("type")
    if isinstance(type_spec, list):
        for option in type_spec:
            branch = dict(schema)
            branch["type"] = option
            if not collect_errors(document, branch, root, path):
                return []
        return [f"{path}: expected one of {type_spec}"]
    expected_type = type_spec
    errors: list[str] = []
    if expected_type == "object":
        if not isinstance(document, dict):
            return [f"{path}: expected object"]
        required = schema.get("required") or []
        missing = [key for key in required if key not in document]
        if missing:
            errors.append(f"{path}: missing {missing[0]}")
        additional = schema.get("additionalProperties", True)
        properties = schema.get("properties") or {}
        if additional is False:
            extra = sorted(set(document) - set(properties))
            if extra:
                errors.append(f"{path}: unexpected {extra[0]}")
        for key, value in document.items():
            child_schema = properties.get(key)
            if child_schema is None:
                if isinstance(additional, dict):
                    errors.extend(
                        collect_errors(value, additional, root, f"{path}.{key}")
                    )
                continue
            errors.extend(collect_errors(value, child_schema, root, f"{path}.{key}"))
        return errors
    if expected_type == "array":
        if not isinstance(document, list):
            return [f"{path}: expected array"]
        item_schema = schema.get("items")
        if isinstance(item_schema, dict):
            for index, item in enumerate(document):
                errors.extend(
                    collect_errors(item, item_schema, root, f"{path}[{index}]")
                )
        return errors
    if "const" in schema and document != schema["const"]:
        errors.append(f"{path}: expected const {schema['const']!r}")
    if "enum" in schema and document not in schema["enum"]:
        errors.append(f"{path}: not in enum")
    if expected_type == "string":
        if not isinstance(document, str):
            return [f"{path}: expected string"]
        minimum = schema.get("minLength")
        maximum = schema.get("maxLength")
        if minimum is not None and len(document) < int(minimum):
            errors.append(f"{path}: string shorter than {minimum}")
        if maximum is not None and len(document) > int(maximum):
            errors.append(f"{path}: string longer than {maximum}")
    elif expected_type == "integer":
        if type(document) is not int:
            return [f"{path}: expected integer"]
        minimum = schema.get("minimum")
        if minimum is not None and document < int(minimum):
            errors.append(f"{path}: integer below {minimum}")
    elif expected_type == "boolean":
        if not isinstance(document, bool):
            return [f"{path}: expected boolean"]
    elif expected_type == "null":
        if document is not None:
            return [f"{path}: expected null"]
    return errors


def _resolve(schema: dict[str, Any], root: dict[str, Any]) -> dict[str, Any]:
    ref = schema.get("$ref")
    if not isinstance(ref, str):
        return schema
    if not ref.startswith("#/$defs/"):
        raise SchemaError(f"unsupported $ref {ref}")
    name = ref.split("/")[-1]
    resolved = (root.get("$defs") or {}).get(name)
    if not isinstance(resolved, dict):
        raise SchemaError(f"missing $defs {name}")
    merged = dict(resolved)
    for key, value in schema.items():
        if key != "$ref":
            merged[key] = value
    return merged
