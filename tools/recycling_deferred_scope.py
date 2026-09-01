#!/usr/bin/env python3
"""Post-1.x scope decisions for remaining deferred ordinary ledger children."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from tools import closeout_seal
from tools import t35_common as t35

R0_SLUG = "recycling/deferred-ordinary-ledger-r0"
R0_DIR = t35.TOOLS / "waves" / "recycling" / "deferred-ordinary-ledger-r0"
PROGRAM = "recycling/deferred-ordinary-runtime"
ALLOWED = (
    "proven_equivalent",
    "phase_deferred",
    "post_1x_scope",
    "unsupported",
)
GENERATED_BY = "python tools/build_deferred_scope_child.py"

EDGE_REASONS = {
    "gt.recipe.smelter#1829": (
        "Mortar meta 32075 recovers molten.alumina. alumina.json sets "
        "molten_fluid=false, so 1.x must not alias it to aluminium. The meta "
        "is outside the 1817 Smelter MTE identity catalog."
    ),
    "gt.recipe.smelter#1884": (
        "Miniature nether portal meta 32766 recovers vanilla lava. Plan forbids "
        "mapping lava to an arbitrary molten metal. runtime_registered=false "
        "and the meta is outside the 1817 identity catalog."
    ),
}
AUTOCLAVE_REASON = (
    "R0 proved ordinary autoclave processing, not consumed-MTE recovery: "
    "circuit + dust + steam -> MTE crystal + distilled water. Crystal "
    "gregtech:gt.multitileentity identity and B0 are missing; mixed family "
    "must not premature-complete. Independent post-1.x scope, not the "
    "Smelter MTE->molten builder."
)
CENTRIFUGE_REASONS = {
    "gt.recipe.centrifuge#0010": (
        "Family exceeds centrifuge envelope 1/6/1/6 with 64000 mB tanks. "
        "Expanding the envelope here would change product semantics; a higher "
        "tier or panel belongs in post-1.x."
    ),
    "gt.recipe.centrifuge#0207": (
        "Source depends on ic2pahoehoelava. Plan forbids remapping that IC2 "
        "fluid to vanilla lava. In-mod pahoehoe identity is out of 1.x scope."
    ),
}


@dataclass(frozen=True)
class ScopeChild:
    slug: str
    predecessor: str
    predecessor_status: str
    next_child: str | None
    next_unassigned: bool
    cohorts: tuple[str, ...]
    owns_families: int
    status: str
    opening_deferred_recycling: int
    opening_deferred_total: int
    drop_recycling: int
    drop_total: int
    closing_owner: str
    note: str


CHILDREN: dict[str, ScopeChild] = {
    "smelter/deferred-recycling-edge": ScopeChild(
        slug="smelter/deferred-recycling-edge",
        predecessor="smelter/deferred-recycling",
        predecessor_status="WAVE_READY",
        next_child="autoclave/deferred-recycling",
        next_unassigned=False,
        cohorts=("smelter_recovery_edge",),
        owns_families=2,
        status="SMELTER_DEFERRED_RECYCLING_EDGE_READY",
        opening_deferred_recycling=26,
        opening_deferred_total=28,
        drop_recycling=2,
        drop_total=2,
        closing_owner="post_1.x/smelter_recovery_edge",
        note=(
            "Independent N<300 edge cohort. alumina and lava are not 1817 "
            "MTE recovery. Disposition is post_1x_scope for both families."
        ),
    ),
    "autoclave/deferred-recycling": ScopeChild(
        slug="autoclave/deferred-recycling",
        predecessor="smelter/deferred-recycling-edge",
        predecessor_status="SMELTER_DEFERRED_RECYCLING_EDGE_READY",
        next_child="recycling/non-recycling-scope",
        next_unassigned=False,
        cohorts=("mislabeled_needs_reclass", "autoclave_tagged_recycling"),
        owns_families=24,
        status="AUTOCLAVE_DEFERRED_RECYCLING_READY",
        opening_deferred_recycling=24,
        opening_deferred_total=26,
        drop_recycling=24,
        drop_total=24,
        closing_owner="post_1.x/ordinary_autoclave_processing",
        note=(
            "Owns R0-corrected autoclave 24. Ordinary processing, not Smelter "
            "MTE recovery. Independent post-1.x scope; deferred recycling "
            "decrements because the hanging later:recycling label is closed."
        ),
    ),
    "recycling/non-recycling-scope": ScopeChild(
        slug="recycling/non-recycling-scope",
        predecessor="autoclave/deferred-recycling",
        predecessor_status="AUTOCLAVE_DEFERRED_RECYCLING_READY",
        next_child="recycling/deferred-ordinary-runtime",
        next_unassigned=False,
        cohorts=("centrifuge_execution_envelope", "centrifuge_cross_mod"),
        owns_families=2,
        status="NON_RECYCLING_SCOPE_READY",
        opening_deferred_recycling=0,
        opening_deferred_total=2,
        drop_recycling=0,
        drop_total=2,
        closing_owner="post_1.x/non_recycling_scope",
        note=(
            "Closes centrifuge #0010 envelope and #0207 ic2pahoehoelava as "
            "independent post-1.x scope. Not a recycling production lock."
        ),
    ),
}


def universe() -> dict[str, Any]:
    return t35.load_json(R0_DIR / "deferred_universe.json")


def families_for(child: ScopeChild) -> list[dict[str, Any]]:
    rows = [
        row
        for row in universe().get("families") or []
        if str(row.get("cohort") or "") in child.cohorts
    ]
    if len(rows) != child.owns_families:
        raise ValueError(
            f"{child.slug} universe rows {len(rows)} != owns_families "
            f"{child.owns_families}"
        )
    return rows


def reason_for(template_key: str, child: ScopeChild) -> str:
    if template_key in EDGE_REASONS:
        return EDGE_REASONS[template_key]
    if template_key in CENTRIFUGE_REASONS:
        return CENTRIFUGE_REASONS[template_key]
    if child.slug == "autoclave/deferred-recycling":
        return AUTOCLAVE_REASON
    raise ValueError(f"{child.slug}: no scope reason for {template_key}")


def closing_owner_for(template_key: str, child: ScopeChild) -> str:
    if template_key == "gt.recipe.centrifuge#0010":
        return "post_1.x/centrifuge_execution_envelope"
    if template_key == "gt.recipe.centrifuge#0207":
        return "post_1.x/cross_mod_pahoehoe"
    return child.closing_owner


def dispositions(child: ScopeChild) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for family in families_for(child):
        template = str(family["template_key"])
        rows.append(
            {
                "allowed_dispositions": list(ALLOWED),
                "cohort": family["cohort"],
                "disposition": "post_1x_scope",
                "family_id": family["family_id"],
                "future_owner": closing_owner_for(template, child),
                "host": family["host"],
                "opening_future_owner": family.get("opening_future_owner")
                or family.get("future_owner"),
                "reason": reason_for(template, child),
                "recheck_condition": "independent post-1.x scope; not 1.x joint exit",
                "template_key": template,
            }
        )
    kinds = {row["disposition"] for row in rows}
    unexpected = kinds - set(ALLOWED)
    if unexpected:
        raise ValueError(f"{child.slug} illegal dispositions {sorted(unexpected)}")
    return rows


def require_predecessor(child: ScopeChild) -> list[str]:
    errors = closeout_seal.check_wave_seal(child.predecessor)
    root = t35.TOOLS / "waves" / child.predecessor
    readiness = t35.load_json(root / "readiness.json")
    status = str(readiness.get("status") or "")
    if status != child.predecessor_status:
        errors.append(
            f"{child.predecessor} status {status} != {child.predecessor_status}"
        )
    if child.slug == "smelter/deferred-recycling-edge":
        census = t35.load_json(root / "census_delta.json")
        remaining = census.get("remaining_ordinary") or {}
        if int(remaining.get("deferred_recycling_count") or 0) != 26:
            errors.append("1817 closing deferred recycling must be 26")
        if int(remaining.get("deferred_total") or 0) != 28:
            errors.append("1817 closing deferred total must be 28")
    return errors


def documents(child: ScopeChild) -> dict[str, Any]:
    errors = require_predecessor(child)
    if errors:
        raise ValueError("; ".join(errors))
    rows = dispositions(child)
    closing_recycling = child.opening_deferred_recycling - child.drop_recycling
    closing_total = child.opening_deferred_total - child.drop_total
    if closing_recycling < 0 or closing_total < 0:
        raise ValueError(f"{child.slug} deferred arithmetic went negative")
    unique_active = child.next_child
    wave = {
        "cohort": child.cohorts[0] if len(child.cohorts) == 1 else list(child.cohorts),
        "depends_on": [child.predecessor],
        "generated_by": GENERATED_BY,
        "owns_families": child.owns_families,
        "program": PROGRAM,
        "schema_version": 1,
        "wave_slug": child.slug,
    }
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": GENERATED_BY,
        "partial_family_count": 0,
        "post_1x_scope_count": child.owns_families,
        "reclassification_delta": 0,
        "remaining_ordinary": {
            "complete_family_count": 0,
            "completion_delta": 0,
            "deferred_recycling_count": closing_recycling,
            "deferred_total": closing_total,
            "enumerated": True,
            "partial_family_count": 0,
            "post_1x_scope_count": child.owns_families,
            "reclassification_delta": 0,
            "remaining_ordinary_families": 0,
        },
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": child.slug,
        "work_set": {"family_count": child.owns_families, "source_rows": len(rows)},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": GENERATED_BY,
        "next_unassigned": child.next_unassigned,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": unique_active,
        "wave_slug": child.slug,
    }
    readiness = {
        "evidence": {
            "complete_family_count": 0,
            "completion_delta": 0,
            "one_x_joint_exit": False,
            "owns_families": child.owns_families,
            "partial_family_count": 0,
            "post_1x_scope_count": child.owns_families,
            "program_status": child.status,
            "recipe_files_generated": False,
            "reclassification_delta": 0,
            "remaining_recipe_gap": 0,
        },
        "generated_by": GENERATED_BY,
        "next_unassigned": child.next_unassigned,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": child.status,
        "unique_active_wave": unique_active,
        "wave_complete": True,
        "wave_slug": child.slug,
    }
    scope = {
        "dispositions": rows,
        "generated_by": GENERATED_BY,
        "owns_families": child.owns_families,
        "padding_forbidden": True,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "POST_1X_SCOPE",
        "wave_slug": child.slug,
    }
    return {
        "census": census,
        "readiness": readiness,
        "scope": scope,
        "topology": topology,
        "wave": wave,
    }


def seal_document(child: ScopeChild, root) -> dict[str, Any]:
    hashes = {
        "census": t35.sha256_file(root / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": t35.sha256_file(root / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": t35.sha256_file(root / "topology.json"),
    }
    return {
        "card_id": child.slug,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": t35.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": t35.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{GENERATED_BY} --write",
        "hashes": hashes,
        "note": child.note,
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": 0,
        "relation_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": child.slug,
        "source_revision": t35.SOURCE_REVISION,
        "status": "SEALED",
    }
