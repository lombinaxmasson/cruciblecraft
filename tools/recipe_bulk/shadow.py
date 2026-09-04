#!/usr/bin/python3
"""Shadow-compile historical compact waves and prove byte/semantic/stable-ID parity."""
from __future__ import annotations

from pathlib import Path
from typing import Any

from tools import json_ledger as ledger
from tools import census_common as census
from tools.recipe_bulk.adapters import adapt_wave, consume_identity
from tools.recipe_bulk.emit import semantic_replay_key
from tools.recipe_bulk.matrix import authored_relation_count, authored_relations
from tools.recipe_bulk.ir import validate_ir_document
from tools.recipe_bulk.write_guard import (
    SHADOW_IR_ROOT,
    assert_not_production_write,
    shadow_ir_path,
)
from tools.recipe_bulk.waves import SHADOW_ORDER, recipe_wave, shadow_waves

STATUS = "RECIPE_WAVE_SHADOW_PARITY"


def production_documents(
    root: Path, prefixes: tuple[str, ...] = ()
) -> dict[str, dict[str, Any]]:
    documents: dict[str, dict[str, Any]] = {}
    trees = [root / prefix for prefix in prefixes] if prefixes else [root]
    for tree in trees:
        if not tree.exists():
            continue
        for path in sorted(p for p in tree.rglob("gt_recipe_*.json") if p.is_file()):
            relative = path.relative_to(root).as_posix()
            documents[relative] = census.load_json(path)
    return documents


def fingerprint(value: Any) -> str:
    return ledger.sha256_record(value)


def consume_collisions(documents: dict[str, dict[str, Any]]) -> dict[str, list[str]]:
    seen: dict[str, list[str]] = {}
    for relative, document in documents.items():
        target = str(document.get("target_map") or "")
        for relation in authored_relations(document):
            key = f"{target}:{consume_identity(relation)}"
            seen.setdefault(key, []).append(
                f"{relative}:{relation.get('stable_id')}"
            )
    return {key: rows for key, rows in seen.items() if len(rows) > 1}


def compare_wave(wave_id: str, persist_ir: bool = False) -> dict[str, Any]:
    spec = recipe_wave(wave_id)
    ir = adapt_wave(spec)
    ir_document = ir.to_document()
    ir_errors = validate_ir_document(ir_document)
    shadow_docs = {family.relative_path: family.document for family in ir.families}
    production = production_documents(spec.generated_root, spec.tree_prefixes)
    mismatches: list[str] = []
    if ir_errors:
        mismatches.extend(f"ir:{error}" for error in ir_errors[:12])
    shadow_names = set(shadow_docs)
    production_names = set(production)
    if shadow_names != production_names:
        missing = sorted(production_names - shadow_names)[:8]
        extra = sorted(shadow_names - production_names)[:8]
        if missing:
            mismatches.append("missing_files:" + ",".join(missing))
        if extra:
            mismatches.append("extra_files:" + ",".join(extra))
    byte_mismatches = 0
    semantic_mismatches = 0
    stable_mismatches = 0
    order_mismatches = 0
    for name in sorted(shadow_names & production_names):
        shadow_text = census.stable_json(shadow_docs[name])
        produced_text = census.stable_json(production[name])
        if shadow_text != produced_text:
            byte_mismatches += 1
        if semantic_replay_key(shadow_docs[name]) != semantic_replay_key(production[name]):
            semantic_mismatches += 1
        shadow_ids = [
            str(row.get("stable_id")) for row in authored_relations(shadow_docs[name])
        ]
        produced_ids = [
            str(row.get("stable_id")) for row in authored_relations(production[name])
        ]
        if shadow_ids != produced_ids:
            stable_mismatches += 1
        shadow_orders = [
            row.get("shadow_order") for row in authored_relations(shadow_docs[name])
        ]
        produced_orders = [
            row.get("shadow_order") for row in authored_relations(production[name])
        ]
        if shadow_orders != produced_orders:
            order_mismatches += 1
    if byte_mismatches:
        mismatches.append(f"byte_identity:{byte_mismatches}")
    if semantic_mismatches:
        mismatches.append(f"semantic_tree:{semantic_mismatches}")
    if stable_mismatches:
        mismatches.append(f"stable_ids:{stable_mismatches}")
    if order_mismatches:
        mismatches.append(f"shadow_order:{order_mismatches}")
    shadow_collisions = consume_collisions(shadow_docs)
    production_collisions = consume_collisions(production)
    if set(shadow_collisions) != set(production_collisions):
        mismatches.append("consume_collisions")
    shadow_ids = sorted(
        [
        str(relation.get("stable_id"))
        for document in shadow_docs.values()
        for relation in authored_relations(document)
        ]
    )
    production_ids = sorted(
        [
        str(relation.get("stable_id"))
        for document in production.values()
        for relation in authored_relations(document)
        ]
    )
    relation_fingerprints = {
        str(relation.get("stable_id")): fingerprint(relation)
        for document in shadow_docs.values()
        for relation in authored_relations(document)
    }
    if persist_ir:
        destination = shadow_ir_path(wave_id)
        assert_not_production_write(destination)
        destination.parent.mkdir(parents=True, exist_ok=True)
        census.write_stable(destination, ir_document)
    return {
        "byte_identity": byte_mismatches == 0 and shadow_names == production_names,
        "consume_collision_count": len(shadow_collisions),
        "family_count": len(shadow_docs),
        "file_count": len(production),
        "mismatches": mismatches,
        "ok": not mismatches,
        "relation_count": sum(
            authored_relation_count(document) for document in shadow_docs.values()
        ),
        "relation_fingerprint_sha256": fingerprint(relation_fingerprints),
        "semantic_root_sha256": fingerprint(
            [semantic_replay_key(shadow_docs[name]) for name in sorted(shadow_docs)]
        ),
        "stable_id_sha256": fingerprint(shadow_ids),
        "production_semantic_root_sha256": fingerprint(
            [semantic_replay_key(production[name]) for name in sorted(production)]
        ),
        "production_stable_id_sha256": fingerprint(production_ids),
        "wave_id": wave_id,
    }


def compare_all(*, persist_ir: bool = False) -> dict[str, Any]:
    first = {wave.wave_id: compare_wave(wave.wave_id, persist_ir=persist_ir) for wave in shadow_waves()}
    second = {wave.wave_id: compare_wave(wave.wave_id, persist_ir=False) for wave in shadow_waves()}
    rebuilds_identical = first == second
    waves = first
    mismatches = [
        f"{wave_id}:{row['mismatches']}"
        for wave_id, row in waves.items()
        if not row["ok"]
    ]
    if not rebuilds_identical:
        mismatches.append("rebuild_drift")
    return {
        "generated_by": "python tools/build_recipe_wave_shadow_parity.py",
        "ok": not mismatches,
        "rebuilds_identical": rebuilds_identical,
        "schema_version": 1,
        "shadow_ir_root": census.relative(SHADOW_IR_ROOT),
        "shadow_order": list(SHADOW_ORDER),
        "status": STATUS,
        "waves": waves,
    }


def write_shadow_ir(wave_id: str, document: dict[str, Any]) -> Path:
    destination = shadow_ir_path(wave_id)
    assert_not_production_write(destination)
    destination.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(destination, document)
    return destination


NEGATIVE_FIXTURE_CHECKS = {
    "relation_set_truncated": "roaster/centrifuge/electrolyzer relation-set replay must compare every ordered relation",
    "lock_authority_mismatch": "smelter/stone and block/object lock stable_id is the production identity authority",
    "ordering_mismatch": "roaster/compact relation order is source_recipe_index then stable_id",
    "stable_id_transform": "assembler compact/wood runtime stable IDs keep only the hex suffix under cruciblecraft:host/cohort/",
}


def evaluate_negative_fixture(name: str, document: dict[str, Any]) -> str:
    """Return an error string when the fixture correctly fails closed."""
    if name == "relation_set_truncated":
        relations = document.get("relations") or []
        source_rows = document.get("source_rows") or []
        if len(relations) < len(source_rows) or len(relations) <= 1:
            return NEGATIVE_FIXTURE_CHECKS[name]
        raise ValueError("relation_set_truncated fixture did not fail")
    if name == "lock_authority_mismatch":
        lock_id = str((document.get("lock_row") or {}).get("stable_id") or "")
        emitted = str(((document.get("relations") or [{}])[0]).get("stable_id") or "")
        if lock_id and emitted and lock_id != emitted:
            return NEGATIVE_FIXTURE_CHECKS[name]
        raise ValueError("lock_authority_mismatch fixture did not fail")
    if name == "ordering_mismatch":
        relations = document.get("relations") or []
        expected = sorted(
            relations,
            key=lambda row: (
                int(row.get("source_recipe_index") or 0),
                str(row.get("stable_id") or ""),
            ),
        )
        if relations != expected:
            return NEGATIVE_FIXTURE_CHECKS[name]
        raise ValueError("ordering_mismatch fixture did not fail")
    if name == "stable_id_transform":
        source_id = str(document.get("source_stable_id") or "")
        runtime_id = str(document.get("runtime_stable_id") or "")
        if source_id.startswith("assembler/compact/") and not runtime_id.startswith(
            "cruciblecraft:assembler/compact/"
        ):
            return NEGATIVE_FIXTURE_CHECKS[name]
        if "/assembler/compact/" in source_id and runtime_id == source_id:
            return NEGATIVE_FIXTURE_CHECKS[name]
        raise ValueError("stable_id_transform fixture did not fail")
    raise KeyError(f"unknown negative fixture {name}")
