#!/usr/bin/env python3
"""Single material-form overlay authority for Python builders and Java loaders."""
from __future__ import annotations

import argparse
import fnmatch
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import io_common as io

TOOLS = io.TOOLS
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
LEGACY_WHOLE_FILE = "legacy_whole_file"
SECTION_SCOPE = "section"
REAL_REGRESSION = "real_regression"
CURRENT_DEPENDENCY_DRIFT = "current_dependency_drift"
HISTORICAL = "historical"
_CAPABILITIES: list[dict[str, Any]] | None = None

def load_authority() -> dict[str, Any]:
    document = io.load_json(AUTHORITY)
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


def _digest(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def _stable_file_bytes(path: Path) -> bytes:
    return path.read_bytes().replace(b"\r\n", b"\n")


def whole_file_sha256(path: Path | None = None) -> str:
    target = path if path is not None else AUTHORITY
    return _digest(_stable_file_bytes(target))


def section_slice(source: dict[str, Any]) -> dict[str, Any]:
    """Stable slice: gate section, required-forms file, extras, and prereqs."""
    path = ROOT / str(source["path"])
    if not path.is_file():
        raise ValueError(f"material form source is missing: {source['path']}")
    return {
        "extra_factual_forms": list(source.get("extra_factual_forms") or []),
        "gate_section": str(source.get("gate_section") or ""),
        "required_factual_prereqs": dict(source.get("required_factual_prereqs") or {}),
        "required_forms_sha256": _digest(_stable_file_bytes(path)),
    }


def section_sha256(source: dict[str, Any]) -> str:
    return _digest(io.stable_json(section_slice(source)).encode("utf-8"))


def section_index(document: dict[str, Any] | None = None) -> dict[str, dict[str, Any]]:
    authority = document if document is not None else load_authority()
    indexed: dict[str, dict[str, Any]] = {}
    for source in authority.get("sources") or []:
        source_id = str(source["id"])
        record = section_slice(source)
        record["slice_sha256"] = section_sha256(source)
        indexed[source_id] = record
    return indexed


def load_capabilities() -> list[dict[str, Any]]:
    global _CAPABILITIES
    if _CAPABILITIES is None:
        rows: list[dict[str, Any]] = []
        root = TOOLS / "capabilities"
        for path in sorted(root.glob("**/capability.json")):
            rows.append(io.load_json(path))
        _CAPABILITIES = rows
    return _CAPABILITIES


def capability_for_path(relative_path: str) -> dict[str, Any] | None:
    normalized = relative_path.replace("\\", "/")
    for capability in load_capabilities():
        for pattern in capability.get("owned_paths") or []:
            if fnmatch.fnmatchcase(normalized, str(pattern)):
                return capability
    return None


def capability_for_owner(owner: str) -> dict[str, Any] | None:
    for capability in load_capabilities():
        slug = str(capability.get("slug") or "")
        if slug == owner or slug.endswith("/" + owner):
            return capability
    return None


def workflow_requires_current_slice(capability: dict[str, Any] | None) -> bool:
    if capability is None:
        return False
    workflow = str(capability.get("workflow") or "")
    maturity = str(capability.get("maturity") or "")
    return workflow == "active" or (
        maturity == "runtime_ready" and workflow not in {"accepted", "paused"}
    )


def is_historical_receipt(relative_path: str) -> bool:
    normalized = relative_path.replace("\\", "/")
    if normalized.endswith(".currentness.json"):
        return True
    if not normalized.endswith("source_receipt.json"):
        return False
    capability = capability_for_path(normalized)
    if capability is None:
        return False
    return not workflow_requires_current_slice(capability)


def consumer_test_modules(section_ids: set[str] | None = None) -> list[str]:
    """Active cards that consume the given authority sections."""
    modules: list[str] = []
    seen: set[str] = set()
    for source in load_authority().get("sources") or []:
        source_id = str(source.get("id") or "")
        gate_section = str(source.get("gate_section") or "")
        if section_ids is not None and section_ids.isdisjoint({source_id, gate_section}):
            continue
        capability = capability_for_owner(str(source.get("owner") or ""))
        if not workflow_requires_current_slice(capability):
            continue
        for pattern in (capability or {}).get("owned_paths") or []:
            text = str(pattern)
            if not text.startswith("tools/tests/test_") or not text.endswith(".py"):
                continue
            module = Path(text).stem
            if module not in seen:
                seen.add(module)
                modules.append(module)
    return modules


def classify_authority_binding(
    receipt: dict[str, Any],
    *,
    workflow: str,
) -> str:
    """Separate real regressions, live slice drift, and closed-card snapshots."""
    stored_map = receipt.get("authority_sha256")
    stored = stored_map.get("material_form_authority") if isinstance(stored_map, dict) else None
    scope = str(receipt.get("authority_scope") or LEGACY_WHOLE_FILE)
    closed = workflow in {"accepted", "paused"}
    revision = str(receipt.get("source_revision") or "")
    if stored is None and scope == LEGACY_WHOLE_FILE:
        if not revision or revision == io.SOURCE_REVISION:
            return "current"
        return HISTORICAL if closed else REAL_REGRESSION
    if scope == SECTION_SCOPE:
        if not isinstance(stored, dict) or not stored:
            return REAL_REGRESSION
        live = section_index()
        for section_id, digest in stored.items():
            record = live.get(str(section_id))
            if record is None or record.get("slice_sha256") != digest:
                return HISTORICAL if closed else CURRENT_DEPENDENCY_DRIFT
        if revision and revision != io.SOURCE_REVISION:
            return REAL_REGRESSION if not closed else HISTORICAL
        return "current"
    if not isinstance(stored, str) or len(stored) != 64:
        return REAL_REGRESSION
    if stored == whole_file_sha256():
        return "current"
    return HISTORICAL if closed else CURRENT_DEPENDENCY_DRIFT


def build() -> dict[str, Any]:
    document = json.loads(io.stable_json(load_authority()))
    document.pop("authority_sections", None)
    document.pop("whole_file_sha256", None)
    java_overlay_sections(document)
    for source in document.get("sources") or []:
        path = ROOT / str(source["path"])
        if not path.is_file():
            raise ValueError(f"material form source is missing: {source['path']}")
    denominators = document.get("typed_ore_denominators") or {}
    for key, expected in (
        ("factual_ore_materials", 137),
        ("registered_ore_materials", 147),
        ("worldgen_acquisition_ore_delta", 10),
        ("chemical_semantic_vein_ledger", 8),
    ):
        if int(denominators.get(key) or 0) != expected:
            raise ValueError(f"typed ore denominator {key} drifted")
    document["generated_by"] = "python tools/material_form_authority.py"
    document["authority_sections"] = section_index(document)
    body = {
        key: value
        for key, value in document.items()
        if key != "whole_file_sha256"
    }
    document["whole_file_sha256"] = _digest(io.stable_json(body).encode("utf-8"))
    return document


def write() -> dict[str, Any]:
    document = build()
    io.write_stable(OUTPUT, document)
    return document


def patch_committed_gate_schema() -> None:
    """Add schema-v2 authority metadata without changing registered materials."""
    gate = without_workflow_metadata(io.load_json(GATE_OUT))
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
    io.write_stable(GATE_OUT, gate)


def check() -> list[str]:
    expected = io.stable_json(build())
    if not OUTPUT.is_file():
        return ["missing tools/material_form_authority.json"]
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        return [io.stale_error(OUTPUT, expected, actual)]
    gate = io.load_json(GATE_OUT)
    errors: list[str] = []
    if (gate.get("authority") or {}).get("path") != "tools/material_form_authority.json":
        errors.append("material registration gate authority path drifted")
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
