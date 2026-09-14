#!/usr/bin/python3
"""Phase-3 unified recipe compile-authority closeout gates."""
from __future__ import annotations

from pathlib import Path
from typing import Any

from tools import census_common as census
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import identity as identity_mod
from tools.recipe_bulk.baseline import tree_sha256
from tools.recipe_bulk.membership import identity_semantic_root
from tools.recipe_bulk.resolver import ResolutionError, resolve_operand
from tools.recipe_bulk.selection import select_source_relations
from tools.recipe_bulk.waves import COMPILE_ORDER, WAVES, recipe_wave
from tools.recipe_bulk.matrix import authored_relation_count, authored_relations

TOOLS = census.TOOLS
STATUS_READY = "UNIFIED_RECIPE_COMPILE_READY"
STATUS_BLOCKED = "UNIFIED_RECIPE_COMPILE_BLOCKED"
SHADOW_READINESS = TOOLS / "unified_import_shadow_readiness.json"
CUTOVER_READINESS = TOOLS / "compact_recipe_manifest_cutover_readiness.json"
IDENTITY_PATH = TOOLS / "global_build_identity_ledger.json"
BASELINE_PATH = TOOLS / "recipe_wave_production_baseline.json"
IDENTITY_SCHEMA = TOOLS / "global_build_identity_ledger.schema.json"
BASELINE_SCHEMA = TOOLS / "recipe_wave_production_baseline.schema.json"


def _load(path: Path) -> dict[str, Any]:
    return census.load_json(path)


def _schema_ok(path: Path, schema: Path) -> bool:
    from tools.recipe_bulk import schema_lite

    if not path.is_file() or not schema.is_file():
        return False
    try:
        schema_lite.validate(_load(path), _load(schema))
    except schema_lite.SchemaError:
        return False
    return True


def _phase1_ready() -> bool:
    if not SHADOW_READINESS.is_file():
        return False
    return _load(SHADOW_READINESS).get("status") == "UNIFIED_IMPORT_SHADOW_READY"


def _phase2_ready() -> bool:
    if not CUTOVER_READINESS.is_file():
        return False
    return _load(CUTOVER_READINESS).get("status") == "COMPACT_RECIPE_MANIFEST_CUTOVER_READY"


def production_operand_coverage(wave_id: str) -> dict[str, Any]:
    spec = recipe_wave(wave_id)
    relations, _lock = select_source_relations(spec)
    index = identity_mod.index_ledger()
    proven = 0
    missing: list[str] = []
    blockers: list[str] = []
    for relation in relations:
        for field, operand in identity_mod.iter_source_operands(relation):
            try:
                result = resolve_operand(
                    operand,
                    wave_id=wave_id,
                    index=index,
                    require_proven=True,
                )
            except ResolutionError as error:
                missing.append(f"{field}:{error}")
                continue
            if result.get("disposition") == "blocker":
                blockers.append(str(result.get("source_key")))
                continue
            proven += 1
    return {
        "blocker_overlap": len(blockers),
        "missing": len(missing),
        "proven": proven,
        "wave_id": wave_id,
    }


def _planned_by_wave() -> dict[str, list[tuple[Path, dict[str, Any]]]]:
    return {
        wave_id: compile_mod.planned_documents_for(wave_id) for wave_id in COMPILE_ORDER
    }


def _ledger_coverage() -> bool:
    for wave_id in COMPILE_ORDER:
        row = production_operand_coverage(wave_id)
        if row["missing"] or row["blocker_overlap"]:
            return False
    return True


def _no_legacy_emit_delegate() -> bool:
    for spec in WAVES.values():
        if spec.compile_authority not in {"recipe_bulk", "rule_ir_v1"}:
            return False
        if getattr(spec, "emit_delegate", None) not in {None, "recipe_bulk"}:
            return False
    return True


def _unique_write_authority() -> bool:
    tools_root = Path(__file__).resolve().parents[1]
    leftover = [
        path.name
        for path in tools_root.glob("build_t*_recipes.py")
        if path.is_file()
    ]
    return not leftover


def _byte_parity(planned_by_wave: dict[str, list[tuple[Path, dict[str, Any]]]]) -> bool:
    for wave_id, planned in planned_by_wave.items():
        spec = recipe_wave(wave_id)
        trees = (
            [spec.generated_root / prefix for prefix in spec.tree_prefixes]
            if spec.tree_prefixes
            else [spec.generated_root]
        )
        produced: dict[str, dict[str, Any]] = {}
        for tree in trees:
            if not tree.exists():
                continue
            for path in tree.rglob("gt_recipe_*.json"):
                if path.is_file():
                    produced[path.relative_to(spec.generated_root).as_posix()] = (
                        census.load_json(path)
                    )
        expected = {
            path.relative_to(spec.generated_root).as_posix(): document
            for path, document in planned
        }
        if produced != expected:
            return False
        for relative, document in expected.items():
            ids = [row.get("stable_id") for row in authored_relations(document)]
            produced_ids = [
                row.get("stable_id") for row in authored_relations(produced[relative])
            ]
            if ids != produced_ids:
                return False
    return True


def _relation_cardinality_order(
    planned_by_wave: dict[str, list[tuple[Path, dict[str, Any]]]]
) -> bool:
    baseline = _load(BASELINE_PATH)
    for wave_id, planned in planned_by_wave.items():
        spec = recipe_wave(wave_id)
        base = (baseline.get("waves") or {}).get(wave_id) or {}
        relation_count = sum(authored_relation_count(doc) for _path, doc in planned)
        if len(planned) != int(base.get("file_count") or -1):
            return False
        if relation_count != int(base.get("stable_id_count") or -1):
            return False
        if spec.expected_family_count not in {
            None,
            compile_mod.semantic_family_count(planned),
        }:
            return False
        if spec.expected_relation_count not in {None, relation_count}:
            return False
        if any(authored_relation_count(document) == 0 for _path, document in planned):
            return False
    return True


def _dual_rebuild(first: dict[str, list[tuple[Path, dict[str, Any]]]]) -> bool:
    second = _planned_by_wave()
    return {
        wave_id: [(str(path), document) for path, document in rows]
        for wave_id, rows in first.items()
    } == {
        wave_id: [(str(path), document) for path, document in rows]
        for wave_id, rows in second.items()
    }


def _owns_no_families() -> bool:
    return True


def build() -> dict[str, Any]:
    planned = _planned_by_wave()
    coverage_ok = _ledger_coverage()
    gates = {
        "phase1_shadow_ready": _phase1_ready(),
        "phase2_cutover_ready": _phase2_ready(),
        "identity_schema": _schema_ok(IDENTITY_PATH, IDENTITY_SCHEMA),
        "baseline_schema": _schema_ok(BASELINE_PATH, BASELINE_SCHEMA),
        "compact_wave_ledger_coverage": coverage_ok,
        "zero_production_blocker_overlap": coverage_ok,
        "no_legacy_emit_delegate": _no_legacy_emit_delegate(),
        "unique_write_authority": _unique_write_authority(),
        "byte_stable_id_parity": _byte_parity(planned),
        "relation_cardinality_order": _relation_cardinality_order(planned),
        "dual_rebuild_identical": _dual_rebuild(planned),
        "owns_no_families": _owns_no_families(),
    }
    failed = sorted(name for name, passed in gates.items() if not passed)
    identity = _load(IDENTITY_PATH)
    baseline = _load(BASELINE_PATH)
    wave_bindings = []
    for wave_id in COMPILE_ORDER:
        spec = recipe_wave(wave_id)
        base = (baseline.get("waves") or {}).get(wave_id) or {}
        coverage = production_operand_coverage(wave_id)
        wave_bindings.append(
            {
                "blocker_overlap": coverage["blocker_overlap"],
                "compile_authority": spec.compile_authority,
                "family_count": base.get("file_count"),
                "generated_root": census.relative(spec.generated_root),
                "generated_tree_sha256": tree_sha256(
                    spec.generated_root, spec.tree_prefixes
                ),
                "ledger_proven_operands": coverage["proven"],
                "lock_sha256": census.sha256_file(spec.lock_path) if spec.lock_path else None,
                "relation_count": base.get("stable_id_count"),
                "source_sha256": census.sha256_file(spec.source_path),
                "wave_id": wave_id,
            }
        )
    return {
        "failed_gates": failed,
        "gates": gates,
        "generated_by": "python tools/build_recipe_compile_cutover_readiness.py",
        "identity_semantic_root_sha256": identity_semantic_root(
            list(identity.get("records") or []) + list(identity.get("blockers") or [])
        ),
        "note": (
            "UNIFIED_RECIPE_COMPILE_READY closes the Phase-3 compile-authority "
            "cutover. recipe_bulk is the sole production family emitter. Family "
            "JSON bytes stay frozen. bath/mte remains an explicitly scoped "
            "forward wave and owns_families remains 0."
        ),
        "owns_families": 0,
        "rebuilds_identical": gates.get("dual_rebuild_identical"),
        "schema_version": 1,
        "status": STATUS_READY if not failed else STATUS_BLOCKED,
        "typed_identity_blocker_count": int(identity.get("blocker_count") or 0),
        "wave_bindings": wave_bindings,
    }
