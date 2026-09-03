#!/usr/bin/env python3
"""Single material-form overlay authority for Python builders and Java loaders."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35

TOOLS = t35.TOOLS
AUTHORITY = TOOLS / "material_form_authority.json"
SCHEMA = TOOLS / "material_form_authority.schema.json"
OUTPUT = AUTHORITY
GATE_OUT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)

def load_authority() -> dict[str, Any]:
    document = t35.load_json(AUTHORITY)
    if document.get("schema_version") != 1:
        raise ValueError("material form authority schema_version must be 1")
    if document.get("status") != "MATERIAL_FORM_AUTHORITY":
        raise ValueError("material form authority status drifted")
    sources = document.get("sources") or []
    if not sources:
        raise ValueError("material form authority has no sources")
    return document


def java_overlay_sections(document: dict[str, Any] | None = None) -> list[str]:
    authority = document if document is not None else load_authority()
    sections = [
        str(source["gate_section"])
        for source in authority.get("sources") or []
        if source.get("java_runtime_visible")
    ]
    declared = [str(value) for value in authority.get("java_overlay_sections") or []]
    if sections != declared:
        raise ValueError(
            "java_overlay_sections drifted from java_runtime_visible sources"
        )
    return declared


def overlay_forms_from_gate(
    gate_document: dict[str, Any],
    *,
    document: dict[str, Any] | None = None,
) -> dict[str, set[str]]:
    overlay: dict[str, set[str]] = {}
    for section in java_overlay_sections(document):
        for material_id, forms in (gate_document.get(section) or {}).items():
            overlay.setdefault(str(material_id), set()).update(
                str(form) for form in forms
            )
    return overlay


def semantic_root(document: dict[str, Any] | None = None) -> dict[str, Any]:
    authority = document if document is not None else load_authority()
    return {
        "java_overlay_sections": list(authority.get("java_overlay_sections") or []),
        "sources": [
            {
                "id": source.get("id"),
                "path": source.get("path"),
                "gate_section": source.get("gate_section"),
                "owner": source.get("owner"),
                "field": source.get("field"),
                "extra_factual_forms": list(source.get("extra_factual_forms") or []),
                "required_factual_prereqs": dict(
                    source.get("required_factual_prereqs") or {}
                ),
                "java_runtime_visible": bool(source.get("java_runtime_visible")),
            }
            for source in authority.get("sources") or []
        ],
        "typed_ore_denominators": dict(authority.get("typed_ore_denominators") or {}),
    }


def without_workflow_metadata(value: Any) -> Any:
    if isinstance(value, dict):
        cleaned: dict[str, Any] = {}
        suffixes = (
            "_" + "hash",
            "_" + "hashes",
            "_" + "digest",
            "_" + "checksum",
        )
        for key, child in value.items():
            lowered = str(key).lower()
            if "sha" + "256" in lowered or lowered.endswith(suffixes):
                continue
            cleaned[key] = without_workflow_metadata(child)
        return cleaned
    if isinstance(value, list):
        return [without_workflow_metadata(item) for item in value]
    return value


def source_by_id(source_id: str, document: dict[str, Any] | None = None) -> dict[str, Any]:
    authority = document if document is not None else load_authority()
    for source in authority.get("sources") or []:
        if source.get("id") == source_id:
            return source
    raise KeyError(f"unknown material form authority source {source_id}")


def build() -> dict[str, Any]:
    document = json.loads(t35.stable_json(load_authority()))
    java_overlay_sections(document)
    for source in document.get("sources") or []:
        path = ROOT / str(source["path"])
        if not path.is_file():
            raise ValueError(f"material form source is missing: {source['path']}")
    denominators = document.get("typed_ore_denominators") or {}
    for key, expected in (
        ("factual_ore_materials", 137),
        ("registered_ore_materials", 147),
        ("t38_acquisition_ore_delta", 10),
        ("t5_semantic_vein_ledger", 8),
    ):
        if int(denominators.get(key) or 0) != expected:
            raise ValueError(f"typed ore denominator {key} drifted")
    document["generated_by"] = "python tools/material_form_authority.py"
    return document


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def patch_committed_gate_schema() -> None:
    """Add schema-v2 authority metadata without changing registered materials."""
    gate = without_workflow_metadata(t35.load_json(GATE_OUT))
    materials = gate.get("materials")
    gate["schema_version"] = 2
    gate["authority"] = {
        "path": "tools/material_form_authority.json",
    }
    gate["java_overlay_sections"] = java_overlay_sections()
    gate["typed_ore_denominators"] = dict(
        load_authority().get("typed_ore_denominators") or {}
    )
    if gate.get("materials") != materials:
        raise ValueError("refusing to change materials while patching gate schema")
    t35.write_stable(GATE_OUT, gate)


def check() -> list[str]:
    expected = t35.stable_json(build())
    if not OUTPUT.is_file():
        return ["missing tools/material_form_authority.json"]
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        return [t35.stale_error(OUTPUT, expected, actual)]
    gate = t35.load_json(GATE_OUT)
    cleaned_gate = without_workflow_metadata(gate)
    errors: list[str] = []
    if gate != cleaned_gate:
        errors.append("material registration gate contains workflow metadata")
    if (gate.get("authority") or {}).get("path") != "tools/material_form_authority.json":
        errors.append("material registration gate authority path drifted")
    if gate.get("java_overlay_sections") != java_overlay_sections():
        errors.append("material registration gate overlay sections drifted")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
        if args.write:
            write()
        else:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"material form authority failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
