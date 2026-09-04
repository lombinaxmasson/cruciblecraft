#!/usr/bin/env python3
"""Versioned semantic projections. Compact canonical JSON is the Python/Java ABI."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)

SEMANTIC_PROJECTION_VERSION = 1
KIND_GATE = "material_registration_gate"
KIND_LEDGER = "json_ledger"
KIND_RECIPE_TREE = "datapack_recipe_tree"

SCHEMA_V1_OVERLAY_SECTIONS = (
    "worldgen_acquisition_forms",
    "roaster_required_forms",
    "centrifuge_required_forms",
    "electrolyzer_required_forms",
)

GATE_ENVELOPE_KEYS = (
    "authority",
    "compatibility_baseline",
    "compatibility_forms",
    "counts",
    "policy",
    "schema_version",
    "sources",
    "typed_ore_denominators",
)

NON_JAVA_GATE_SECTIONS = (
    "acceptance_forms",
    "chemical_required_forms",
    "electrical_wire_forms",
    "pipe_forms",
    "known_ingot_forms",
)

LEGACY_LEDGER_KEYS = frozenset(
    {
        "build_inputs_sha256",
        "currentness",
        "currentness_root_sha256",
        "generated_at",
        "generated_by",
        "inputs_sha256",
        "owned_inputs",
        "output_hashes",
        "semantic_root_sha256",
        "source_hashes",
        "tracked_input_hashes",
    }
)

PROVENANCE_SHA_KEYS = frozenset(
    {
        "generated_files",
        "generated_tree",
        "generated_tag_sha256",
        "registration_gate_sha256",
        "source_files_sha256",
        "source_sha256",
    }
)

PROOF_CURRENTNESS_KEYS = frozenset(
    {
        "builder_sha256",
        "output_tree_sha256",
        "source_projection_sha256",
    }
)


class ProjectionError(ValueError):
    """Fail-closed projection / version mismatch."""


def canonical_dumps(value: Any) -> str:
    """Compact UTF-8 JSON; object keys sorted. Matches Java SemanticProjection."""
    return json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )


def canonical_bytes(value: Any) -> bytes:
    return canonical_dumps(value).encode("utf-8")


def sha256_canonical(value: Any) -> str:
    return hashlib.sha256(canonical_bytes(value)).hexdigest()


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _sorted_forms(forms: Any) -> list[str]:
    if not isinstance(forms, list):
        return []
    return sorted({str(item) for item in forms})


def _sorted_form_map(mapping: Any) -> dict[str, list[str]]:
    if not isinstance(mapping, dict):
        return {}
    return {
        str(material): _sorted_forms(forms)
        for material, forms in sorted(mapping.items(), key=lambda item: str(item[0]))
    }


def _is_sha256(value: Any) -> bool:
    return (
        isinstance(value, str)
        and len(value) == 64
        and set(value) <= set("0123456789abcdef")
    )


def _is_hash_map(value: Any) -> bool:
    if not isinstance(value, dict) or not value:
        return False
    return all(isinstance(key, str) and _is_sha256(item) for key, item in value.items())


def overlay_section_names(gate: dict[str, Any]) -> list[str]:
    schema_version = gate.get("schema_version")
    if schema_version == 1:
        return list(SCHEMA_V1_OVERLAY_SECTIONS)
    sections = [str(item) for item in (gate.get("java_overlay_sections") or [])]
    if not sections:
        raise ProjectionError("gate schema v2 is missing java_overlay_sections")
    return sorted(set(sections))


def gate_projection(gate: dict[str, Any]) -> dict[str, Any]:
    sections = overlay_section_names(gate)
    payloads = {
        section: _sorted_form_map(gate.get(section))
        for section in sections
    }
    return {
        "kind": KIND_GATE,
        "materials": _sorted_form_map(gate.get("materials")),
        "overlay_payloads": payloads,
        "overlay_sections": sections,
        "semantic_projection_version": SEMANTIC_PROJECTION_VERSION,
    }


def gate_envelope(gate: dict[str, Any]) -> dict[str, Any]:
    envelope: dict[str, Any] = {}
    for key in GATE_ENVELOPE_KEYS:
        if key in gate:
            envelope[key] = gate[key]
    for section in NON_JAVA_GATE_SECTIONS:
        if section in gate:
            envelope[section] = gate[section]
    return envelope


def _is_builder_stamp(value: Any) -> bool:
    return isinstance(value, dict) and "source_sha256" in value


def _strip_ledger(document: Any) -> Any:
    if isinstance(document, dict):
        stripped: dict[str, Any] = {}
        for key, value in document.items():
            if key in LEGACY_LEDGER_KEYS or key in PROVENANCE_SHA_KEYS:
                continue
            if key == "builder" and _is_builder_stamp(value):
                continue
            if key == "inputs" and _is_hash_map(value):
                continue
            if key == "proof" and isinstance(value, dict):
                stripped[key] = {
                    nested_key: nested_value
                    for nested_key, nested_value in value.items()
                    if nested_key not in PROOF_CURRENTNESS_KEYS
                }
                continue
            stripped[key] = _strip_ledger(value)
        return stripped
    if isinstance(document, list):
        return [_strip_ledger(item) for item in document]
    return document


def ledger_projection(document: Any) -> dict[str, Any]:
    return {
        "kind": KIND_LEDGER,
        "semantic_body": _strip_ledger(document),
        "semantic_projection_version": SEMANTIC_PROJECTION_VERSION,
    }


def ledger_envelope(document: Any) -> dict[str, Any]:
    if not isinstance(document, dict):
        return {}
    envelope: dict[str, Any] = {}
    for key, value in document.items():
        if (
            key in LEGACY_LEDGER_KEYS
            or key in PROVENANCE_SHA_KEYS
            or (key == "builder" and _is_builder_stamp(value))
            or (key == "inputs" and _is_hash_map(value))
        ):
            envelope[key] = value
            continue
        if key == "proof" and isinstance(value, dict):
            proof_currentness = {
                nested_key: nested_value
                for nested_key, nested_value in value.items()
                if nested_key in PROOF_CURRENTNESS_KEYS
            }
            if proof_currentness:
                envelope["proof_currentness"] = proof_currentness
    return envelope


def recipe_entry(path: Path, document: dict[str, Any], *, relative: str) -> dict[str, Any]:
    recipe_id = document.get("id")
    if not isinstance(recipe_id, str) or not recipe_id:
        recipe_id = relative.rsplit(".", 1)[0].replace("\\", "/")
    return {
        "id": recipe_id,
        "path": relative.replace("\\", "/"),
        "recipe": document,
    }


def recipe_tree_projection(root: Path, *, repo: Path = ROOT) -> dict[str, Any]:
    if not root.is_dir():
        entries: list[dict[str, str]] = []
    else:
        entries = []
        for path in sorted(child for child in root.rglob("*.json") if child.is_file()):
            relative = path.relative_to(repo).as_posix()
            document = json.loads(path.read_text(encoding="utf-8"))
            entry = recipe_entry(path, document, relative=relative)
            entries.append(
                {
                    "id": entry["id"],
                    "path": entry["path"],
                    "recipe_sha256": sha256_canonical(entry["recipe"]),
                }
            )
    return {
        "kind": KIND_RECIPE_TREE,
        "root": recipe_tree_root_path(root, repo=repo),
        "recipes": entries,
        "semantic_projection_version": SEMANTIC_PROJECTION_VERSION,
    }


def recipe_tree_root_path(root: Path, *, repo: Path = ROOT) -> str:
    resolved = root.resolve()
    try:
        return resolved.relative_to(repo.resolve()).as_posix()
    except ValueError:
        return root.as_posix()


def project_gate_document(gate: dict[str, Any]) -> dict[str, str]:
    projection = gate_projection(gate)
    return {
        "kind": KIND_GATE,
        "semantic_projection_version": str(SEMANTIC_PROJECTION_VERSION),
        "semantic_root_sha256": sha256_canonical(projection),
        "envelope_sha256": sha256_canonical(gate_envelope(gate)),
    }


def project_ledger_document(document: Any) -> dict[str, str]:
    return {
        "kind": KIND_LEDGER,
        "semantic_projection_version": str(SEMANTIC_PROJECTION_VERSION),
        "semantic_root_sha256": sha256_canonical(ledger_projection(document)),
        "envelope_sha256": sha256_canonical(ledger_envelope(document)),
    }


def project_recipe_tree(root: Path, *, repo: Path = ROOT) -> dict[str, str]:
    projection = recipe_tree_projection(root, repo=repo)
    projection["root"] = recipe_tree_root_path(root, repo=repo)
    return {
        "kind": KIND_RECIPE_TREE,
        "semantic_projection_version": str(SEMANTIC_PROJECTION_VERSION),
        "semantic_root_sha256": sha256_canonical(projection),
        "envelope_sha256": sha256_canonical({}),
    }


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def project_artifact(
    path: Path,
    *,
    kind: str,
    document: Any | None = None,
) -> dict[str, str]:
    if kind == KIND_GATE:
        payload = document if document is not None else load_json(path)
        return project_gate_document(payload)
    if kind == KIND_RECIPE_TREE:
        return project_recipe_tree(path)
    payload = document if document is not None else load_json(path)
    return project_ledger_document(payload)


def require_version(sidecar_version: Any) -> None:
    if sidecar_version != SEMANTIC_PROJECTION_VERSION:
        raise ProjectionError(
            f"PROJECTION_VERSION_MISMATCH: sidecar={sidecar_version!r} "
            f"code={SEMANTIC_PROJECTION_VERSION}"
        )


def gate_semantic_root_sha256(path: Path | None = None) -> str:
    document = load_json(path or GATE)
    return project_gate_document(document)["semantic_root_sha256"]


def whole_file_contract_errors(
    relative: str,
    expected_hash: str,
    path: Path,
) -> list[str]:
    """Legacy whole-file SHA contract. Used only in dual-run tests."""
    if not path.is_file():
        return [f"legacy missing compact input: {relative}"]
    actual = sha256_file(path)
    if actual != expected_hash:
        return [f"legacy compact input hash drifted: {relative}"]
    return []
