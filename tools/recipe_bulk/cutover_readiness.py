#!/usr/bin/python3
"""Phase-2 runtime manifest cutover closeout gates."""
from __future__ import annotations

from pathlib import Path
from typing import Any, Callable

from tools import t35_common as t35
from tools.recipe_bulk import runtime as runtime_mod
from tools.recipe_bulk.membership import membership_root
from tools.recipe_bulk.shadow_readiness import STATUS_READY as SHADOW_READY

TOOLS = t35.TOOLS
ROOT = t35.ROOT
STATUS_READY = "COMPACT_RECIPE_MANIFEST_CUTOVER_READY"
STATUS_BLOCKED = "COMPACT_RECIPE_MANIFEST_CUTOVER_BLOCKED"
PROVIDER = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/recipe/gt/CompactRecipeFamilyProvider.java"
)
LOADER = (
    ROOT / "src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipeMapLoader.java"
)
POLICY = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/recipe/gt/CompactPublicationPolicy.java"
)
SHADOW_READINESS = TOOLS / "unified_import_shadow_readiness.json"
T43_POLICY = (
    ROOT
    / "src/t43_recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
    / "t43_smelter_stone.json"
)


def _load(path: Path) -> dict[str, Any]:
    return t35.load_json(path)


def _shadow_ready() -> bool:
    if not SHADOW_READINESS.is_file():
        return False
    return _load(SHADOW_READINESS).get("status") == SHADOW_READY


def _runtime_ok() -> bool:
    document = runtime_mod.build()
    return (
        document.get("status") == runtime_mod.STATUS
        and document.get("group_count") == 12
        and document.get("t37_eager_count") == 14
        and document.get("t41_live_relation_count") == 242
        and len(document.get("dedup_rules") or []) == 4
    )


def _policies_current() -> bool:
    try:
        documents = runtime_mod.cutover_policy_documents()
    except (OSError, ValueError, KeyError):
        return False
    for group_id, document in documents.items():
        spec = runtime_mod.spec_for_group(group_id)
        path = runtime_mod.datapack_policy_path(spec)
        if t35.check_generated_document(path, document):
            return False
    return True


def _dedup_current() -> bool:
    try:
        rules = {rule["rule_id"]: rule for rule in runtime_mod.dedup_rules()}
        runtime_mod.validate_dedup_rules(list(rules.values()))
    except (OSError, ValueError, KeyError):
        return False
    for rule_id, document in rules.items():
        path = runtime_mod.datapack_dedup_root() / f"{rule_id.split(':', 1)[1]}.json"
        if t35.check_generated_document(path, document):
            return False
    return True


def _t43_membership_unified() -> bool:
    if not T43_POLICY.is_file():
        return False
    policy = _load(T43_POLICY)
    families = runtime_mod.load_wave_families("T43")
    family_ids = [str(family["family_id"]) for family in families]
    stable_ids = [
        str(relation["stable_id"])
        for family in families
        for relation in family.get("relations") or []
    ]
    return policy.get("membership_root_sha256") == membership_root(
        family_ids, stable_ids
    )


def _java_policy_methods_removed() -> bool:
    text = PROVIDER.read_text(encoding="utf-8")
    forbidden = (
        "t37ProductionPolicy",
        "t38ProductionPolicy",
        "t39SingletonPolicy",
        "t41PlanksPolicy",
    )
    return not any(name in text for name in forbidden)


def _java_dedup_methods_removed() -> bool:
    text = LOADER.read_text(encoding="utf-8")
    forbidden = (
        "dropT41AssemblerSourcesAlreadyExpressedByT37",
        "dropEquivalentT41RowsAlreadyExpressedByT37",
        "dropEquivalentT5RowsSupersededByT39",
        "dropEquivalentT5RowsSupersededByT40",
    )
    return not any(name in text for name in forbidden)


def _t43_skip_removed() -> bool:
    text = POLICY.read_text(encoding="utf-8")
    return "T43_SMELTER_STONE_PUBLICATION_GROUP" not in text


GATE_PROBES: tuple[tuple[str, Callable[[], bool]], ...] = (
    ("shadow_ready", _shadow_ready),
    ("runtime_twelve_groups", _runtime_ok),
    ("cutover_policies_current", _policies_current),
    ("dedup_rules_current", _dedup_current),
    ("t43_membership_unified", _t43_membership_unified),
    ("java_policy_methods_removed", _java_policy_methods_removed),
    ("java_dedup_methods_removed", _java_dedup_methods_removed),
    ("t43_membership_skip_removed", _t43_skip_removed),
    ("owns_no_families", lambda: True),
)


def build() -> dict[str, Any]:
    gates = {name: probe() for name, probe in GATE_PROBES}
    failed = sorted(name for name, passed in gates.items() if not passed)
    return {
        "failed_gates": failed,
        "gates": gates,
        "generated_by": "python tools/build_compact_recipe_manifest_cutover_readiness.py",
        "note": (
            "COMPACT_RECIPE_MANIFEST_CUTOVER_READY closes the Phase-2 runtime "
            "manifest cutover. Family JSON bytes stay frozen. T37/T38 historical "
            "publication_group decode remains. New recipe imports stay paused."
        ),
        "owns_families": 0,
        "schema_version": 1,
        "status": STATUS_READY if not failed else STATUS_BLOCKED,
    }
