#!/usr/bin/python3
"""Deterministic transport splitting for compact GT recipe families.

Semantic family_id stays unchanged. Physical RecipeHolder filenames may use
``_fragment_NNNN`` so datapack ids remain unique. Split only when a document
would exceed the Java product ceilings.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

from tools.recipe_bulk.matrix import authored_relations, shape_key, wrap_document

PRODUCT_RELATION_CEILING = 4_096
PRODUCT_DICTIONARY_CEILING = 8_192
TRANSPORT_RELATION_BUDGET = 1_024
TRANSPORT_DICTIONARY_BUDGET = 4_096
MAX_TRANSPORT_FRAGMENTS = 256


def semantic_digest(document: dict[str, Any], relations: list[dict[str, Any]]) -> str:
    lines = [
        str(document.get("family_id") or ""),
        str(document.get("target_map") or ""),
        str(document.get("source_revision") or ""),
        str(document.get("publication_group") or ""),
    ]
    ordered = sorted(
        relations,
        key=lambda row: (
            int(row.get("shadow_order") or 0),
            str(row.get("stable_id") or ""),
        ),
    )
    for row in ordered:
        lines.append(
            f"{row.get('stable_id') or ''}\t{int(row.get('shadow_order') or 0)}"
        )
    payload = "".join(f"{line}\n" for line in lines)
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def dictionary_entries(relations: list[dict[str, Any]]) -> int:
    items: set[str] = set()
    fluids: set[str] = set()
    for relation in relations:
        for value in relation.get("item_inputs") or []:
            items.add(_freeze(value))
        for value in relation.get("item_outputs") or []:
            items.add(_freeze(value))
        for value in (relation.get("fluid_inputs") or []) + (
            relation.get("fluid_outputs") or []
        ):
            fluids.add(_freeze(value))
    return max(len(items), len(fluids))


def _freeze(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"))


def needs_split(relations: list[dict[str, Any]]) -> bool:
    return (
        len(relations) > PRODUCT_RELATION_CEILING
        or dictionary_entries(relations) > PRODUCT_DICTIONARY_CEILING
    )


def fragment_path(path: Path, index: int) -> Path:
    return path.with_name(f"{path.stem}_fragment_{index:04d}{path.suffix}")


def assemble_semantic_families(
    documents: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """Reassemble physical holders so each semantic family appears once."""
    grouped: dict[tuple[str, str, str], list[dict[str, Any]]] = {}
    for document in documents:
        key = (
            str(document.get("family_id") or ""),
            str(document.get("target_map") or ""),
            str(document.get("publication_group") or ""),
        )
        grouped.setdefault(key, []).append(document)
    assembled = [reassemble_documents(members) for members in grouped.values()]
    assembled.sort(
        key=lambda document: (
            str(document.get("family_id") or ""),
            str(document.get("target_map") or ""),
            str(document.get("publication_group") or ""),
        )
    )
    return assembled


def reassemble_documents(documents: list[dict[str, Any]]) -> dict[str, Any]:
    """Merge transport fragments of one semantic family into an unsplit document."""
    if not documents:
        raise ValueError("no compact family documents to reassemble")
    ordered = sorted(documents, key=_fragment_sort_key)
    envelopes = [document.get("transport_fragment") for document in ordered]
    if any(envelopes) and not all(envelopes):
        raise ValueError(
            f"{ordered[0].get('family_id')}: mixes transport fragments with unsplit holders"
        )
    if not any(envelopes):
        if len(ordered) != 1:
            raise ValueError(
                f"{ordered[0].get('family_id')}: unsplit semantic family has "
                f"{len(ordered)} documents"
            )
        return ordered[0]
    first = ordered[0]
    header = first["transport_fragment"]
    if int(header["count"]) != len(ordered):
        raise ValueError(
            f"{first.get('family_id')}: transport fragment count "
            f"{len(ordered)} != {header['count']}"
        )
    relations: list[dict[str, Any]] = []
    for index, document in enumerate(ordered):
        envelope = document["transport_fragment"]
        if int(envelope["index"]) != index:
            raise ValueError(
                f"{first.get('family_id')}: missing or duplicate fragment index {index}"
            )
        if (
            str(document.get("family_id")) != str(first.get("family_id"))
            or str(document.get("target_map")) != str(first.get("target_map"))
            or str(document.get("source_revision")) != str(first.get("source_revision"))
            or str(document.get("publication_group") or "")
            != str(first.get("publication_group") or "")
            or int(envelope["count"]) != int(header["count"])
            or int(envelope["total_relations"]) != int(header["total_relations"])
            or str(envelope["semantic_digest"]) != str(header["semantic_digest"])
        ):
            raise ValueError(
                f"{first.get('family_id')}: transport fragment headers drifted"
            )
        relations.extend(authored_relations(document))
    if len(relations) != int(header["total_relations"]):
        raise ValueError(
            f"{first.get('family_id')}: reassembled relation count "
            f"{len(relations)} != {header['total_relations']}"
        )
    digest = semantic_digest(first, relations)
    if digest != str(header["semantic_digest"]):
        raise ValueError(f"{first.get('family_id')}: semantic digest drifted")
    assembled = wrap_document(_header(first), relations)
    assembled.pop("transport_fragment", None)
    return assembled


def split_document(
    path: Path, document: dict[str, Any]
) -> list[tuple[Path, dict[str, Any]]]:
    relations = authored_relations(document)
    if not needs_split(relations):
        return [(path, document)]
    chunks = _chunk_relations(relations)
    if len(chunks) < 2:
        raise ValueError(
            f"{document.get('family_id')}: transport split produced "
            f"{len(chunks)} chunk(s); need at least 2"
        )
    if len(chunks) > MAX_TRANSPORT_FRAGMENTS:
        raise ValueError(
            f"{document.get('family_id')}: transport fragment count "
            f"{len(chunks)} exceeds {MAX_TRANSPORT_FRAGMENTS}"
        )
    digest = semantic_digest(document, relations)
    total = len(relations)
    planned: list[tuple[Path, dict[str, Any]]] = []
    for index, chunk in enumerate(chunks):
        if not chunk:
            raise ValueError(f"{document.get('family_id')}: empty transport fragment")
        if len(chunk) > PRODUCT_RELATION_CEILING:
            raise ValueError(
                f"{document.get('family_id')} fragment {index} has "
                f"{len(chunk)} relations over {PRODUCT_RELATION_CEILING}"
            )
        if dictionary_entries(chunk) > PRODUCT_DICTIONARY_CEILING:
            raise ValueError(
                f"{document.get('family_id')} fragment {index} dictionary exceeds "
                f"{PRODUCT_DICTIONARY_CEILING}"
            )
        fragment = wrap_document(_header(document), chunk)
        fragment["transport_fragment"] = {
            "count": len(chunks),
            "index": index,
            "semantic_digest": digest,
            "total_relations": total,
        }
        planned.append((fragment_path(path, index), fragment))
    return planned


def _header(document: dict[str, Any]) -> dict[str, Any]:
    header = {
        key: value
        for key, value in document.items()
        if key
        not in {
            "authored_form",
            "matrix",
            "relations",
            "transport_fragment",
        }
    }
    return header


def _fragment_sort_key(document: dict[str, Any]) -> tuple[int, str]:
    envelope = document.get("transport_fragment") or {}
    return (int(envelope.get("index") or 0), str(document.get("family_id") or ""))


def _chunk_relations(relations: list[dict[str, Any]]) -> list[list[dict[str, Any]]]:
    grouped: dict[tuple[Any, ...], list[dict[str, Any]]] = {}
    for relation in relations:
        grouped.setdefault(shape_key(relation), []).append(relation)
    chunks: list[list[dict[str, Any]]] = []
    current: list[dict[str, Any]] = []
    for key in sorted(grouped):
        group = grouped[key]
        if _exceeds_budget(current + group) and current:
            chunks.append(current)
            current = []
        if _exceeds_budget(group):
            if current:
                chunks.append(current)
                current = []
            chunks.extend(_slice_group(group))
            continue
        current.extend(group)
    if current:
        chunks.append(current)
    return chunks


def _slice_group(group: list[dict[str, Any]]) -> list[list[dict[str, Any]]]:
    slices: list[list[dict[str, Any]]] = []
    current: list[dict[str, Any]] = []
    for relation in group:
        trial = current + [relation]
        if current and _exceeds_budget(trial):
            slices.append(current)
            current = [relation]
            if _exceeds_budget(current):
                raise ValueError("single compact relation exceeds transport budget")
            continue
        current = trial
    if current:
        slices.append(current)
    return slices


def _exceeds_budget(relations: list[dict[str, Any]]) -> bool:
    return (
        len(relations) > TRANSPORT_RELATION_BUDGET
        or dictionary_entries(relations) > TRANSPORT_DICTIONARY_BUDGET
    )
