#!/usr/bin/env python3
"""L1b — layered material selection (not binary keep/drop).

Priority: empirical evidence (registered items + recipe appearance) over
originalMod / tags. Emits selected + dropped with explicit rule hits, then
re-checks closure on the selected set.

Evidence metrics:
  recipe_appearances   — hits across ALL dump recipe maps (primary for tiering)
  reviewed_appearances — hits on activation evidence_maps only (secondary)
"""
from __future__ import annotations

import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump"
OUT_SELECTED = TOOLS / "gt6_l1b_selected.json"
OUT_DROPPED = TOOLS / "gt6_l1b_dropped.json"
OUT_NEEDS_REVIEW = TOOLS / "gt6_l1b_needs_review.json"
OUT_PENDING_T0B = TOOLS / "gt6_l1b_pending_t0b.json"
OUT_DEGRADED_PATHS = TOOLS / "gt6_l1b_degraded_paths.json"
OUT_BYPRODUCT_STRIPPING = TOOLS / "gt6_l1b_byproduct_stripping.json"
OUT_REPORT = TOOLS / "gt6_l1b_layer_report.json"

LAYERS = (
    "CORE",
    "PHASE_2",
    "PHASE_3",
    "PHASE_LATER",
    "SHELVED_FICTIONAL",
    "NEEDS_REVIEW",
    "PENDING_T0B",
    "COMPAT_DEAD",
    "NEVER",
)

ACTIVE_LAYER_RANK = {
    "CORE": 0,
    "PHASE_2": 1,
    "PHASE_3": 2,
    "PHASE_LATER": 3,
}

sys.path.insert(0, str(TOOLS))
from gt6_structural_class import (  # noqa: E402
    is_common_isotope,
    is_element_like,
    is_native_mod,
    structural_class,
)

# Rule provenance is policy metadata, not a confidence score inferred after the
# fact. Every classify() rule must be registered here.
RULE_DEFINITIONS: dict[str, dict[str, Any]] = {
    # Direct dump invariants / observed evidence.
    "no_stable_source_id": {
        "basis": "verified",
        "reason": "source_id < 0 directly identifies a non-canonical placeholder/alias row.",
    },
    "tag_invalid_material": {
        "basis": "verified",
        "reason": "GT6 explicitly marks the row PROPERTIES.INVALID_MATERIAL.",
    },
    "empty_material_slot": {
        "basis": "verified",
        "reason": "Material id 0 / Empty is the reserved empty slot.",
    },
    "fictional_material_shelved": {
        "basis": "verified",
        "reason": "Full-range query identifies ids 1181..7999 as post-periodic fictional/sci-fi/magic/joke materials.",
        "falsifier": "A canonical real periodic element or isotope exists with material id in 1181..7999.",
        "falsifier_test": "test_fictional_band_starts_after_verified_periodic_table",
    },
    "element_active_item_evidence": {
        "basis": "verified",
        "reason": "Element encoding is L1a-verified and ACTIVE has registered forms plus recipe item evidence.",
    },
    "element_active_fluid_or_dependency": {
        "basis": "verified",
        "reason": "Element encoding is L1a-verified and the row has direct fluid/dependency evidence.",
    },
    "element_active_formless": {
        "basis": "verified",
        "reason": "Element encoding and item evidence are verified; no current T0 form is registered.",
    },
    "element_deferred_item_evidence": {
        "basis": "verified",
        "reason": "Element encoding and item evidence are verified; activation policy marks it deferred.",
    },
    "element_fluid_only": {
        "basis": "verified",
        "reason": "Element encoding is verified and evidence is fluid-only.",
    },
    "active_item_evidence": {
        "basis": "verified",
        "reason": "ACTIVE row has registered forms and direct recipe item evidence.",
    },
    "deferred_item_evidence_no_t0_form": {
        "basis": "verified",
        "reason": "DEFERRED row has direct recipe item evidence but no active T0 form.",
    },
    "active_item_evidence_formless": {
        "basis": "verified",
        "reason": "ACTIVE row has direct item evidence and no current T0 form.",
    },
    # Human/external inputs.
    "authored_include": {
        "basis": "external",
        "reason": "Existing authored CrucibleCraft material is a reviewed human input.",
    },
    "pending_t0b_non_native_ownership": {
        "basis": "external",
        "reason": "Non-native alloy/organic ownership awaits T0b 1.21.1 mod inventory decisions.",
    },
    "isotope_common_active": {
        "basis": "external",
        "reason": "Common-isotope allowlist is a human phase/scope decision.",
    },
    "isotope_common_active_loose": {
        "basis": "external",
        "reason": "Common-isotope allowlist is a human phase/scope decision.",
    },
    "isotope_common_deferred": {
        "basis": "external",
        "reason": "Common-isotope allowlist is a human phase/scope decision.",
    },
    "isotope_common_cold": {
        "basis": "external",
        "reason": "Common-isotope allowlist is a human phase/scope decision.",
    },
    "explicit_food_exclude": {
        "basis": "external",
        "reason": "Explicit activation exclusion supplied by reviewed policy input.",
    },
    "explicit_special_exclude": {
        "basis": "external",
        "reason": "Explicit activation exclusion supplied by reviewed policy input.",
    },
    "explicit_compat_exclude": {
        "basis": "external",
        "reason": "Explicit activation exclusion supplied by reviewed policy input.",
    },
    # Heuristics / policy inferences.
    "tag_dont_show_component": {
        "basis": "external",
        "reason": "NEVER is a product-scope decision; the tag meaning alone does not determine port scope.",
        "external_dependency": "L2 product-scope verdict for generic component markers.",
    },
    "element_hidden_but_hot": {
        "basis": "external",
        "reason": "Later-phase placement is a product sequencing decision, not a data-derived fact.",
        "external_dependency": "L2 phase-scope decision for non-active periodic elements.",
    },
    "element_hidden_cold": {
        "basis": "inferred",
        "reason": "Infers later-phase placement from hidden element status without strong recipe evidence.",
    },
    "element_deferred": {
        "basis": "external",
        "reason": "Later-phase placement follows the human-authored activation/phase policy.",
        "external_dependency": "L2 phase-scope decision for deferred periodic elements.",
    },
    "element_active_unclassified": {
        "basis": "inferred",
        "reason": "Fallback for ACTIVE element-like rows not covered by stronger evidence rules.",
        "falsifier": "A hit is not ACTIVE, is not element-like, or satisfies a stronger element evidence rule.",
        "falsifier_test": "test_active_fallback_rules_match_only_their_declared_domain",
    },
    "element_out_of_scope_retained": {
        "basis": "inferred",
        "reason": "Retains element-like rows despite prior OUT_OF_SCOPE because periodic-table membership dominates ownership.",
    },
    "element_unclassified": {
        "basis": "inferred",
        "reason": "Fallback element-like classification; should remain rare and reviewable.",
    },
    "isotope_obscure_deferred": {
        "basis": "external",
        "reason": "The common-isotope boundary and phase placement are human scope decisions.",
        "external_dependency": "L2 nuclear-phase scope decision.",
    },
    "tag_food": {
        "basis": "external",
        "reason": "Excluding FOOD-tagged rows is a CrucibleCraft product-scope decision.",
        "external_dependency": "L2 food/material scope decision.",
    },
    "hidden_hot_residual_review": {
        "basis": "external",
        "reason": "Hidden native material remains hot and needs an explicit human disposition.",
        "external_dependency": "L2 verdict for the residual hidden native material.",
    },
    "hidden_cold_residual_review": {
        "basis": "inferred",
        "reason": "Hidden native material remains unexplained after structural and ownership rules.",
    },
    "active_fluid_or_dependency_closure": {
        "basis": "verified",
        "reason": "Direct fluid/dependency evidence requires retention to preserve observed closure.",
        "falsifier": "A row hits this rule without fluid evidence or an explicit dependency/transitive activation reason.",
        "falsifier_test": "test_active_closure_rule_requires_direct_evidence",
    },
    "active_fluid_only": {
        "basis": "inferred",
        "reason": "Infers PHASE_3 from ACTIVE fluid-only evidence.",
    },
    "deferred_fluid_evidence": {
        "basis": "external",
        "reason": "Fluid evidence is observed, but PHASE_3 placement is a product sequencing decision.",
        "external_dependency": "L2 fluid-processing phase decision.",
    },
    "deferred_no_reviewed_map_evidence": {
        "basis": "external",
        "reason": "No reviewed-map evidence is observed, but PHASE_LATER placement follows human phase policy.",
        "external_dependency": "L2 phase-scope decision for deferred cold rows.",
    },
    "active_without_classified_evidence": {
        "basis": "inferred",
        "reason": "Fallback for ACTIVE rows lacking a stronger evidence classification.",
        "falsifier": "A hit is not ACTIVE, is element-like, or satisfies a stronger item/fluid/dependency evidence rule.",
        "falsifier_test": "test_active_fallback_rules_match_only_their_declared_domain",
    },
    "cross_mod_no_reviewed_evidence": {
        "basis": "inferred",
        "reason": "Infers COMPAT_DEAD from non-native ownership and no reviewed evidence.",
    },
    "no_reviewed_evidence": {
        "basis": "inferred",
        "reason": "Infers COMPAT_DEAD from OUT_OF_SCOPE with no reviewed evidence.",
    },
    "out_of_scope_with_reviewed_evidence": {
        "basis": "inferred",
        "reason": "Fallback for OUT_OF_SCOPE rows that nevertheless have reviewed evidence.",
    },
    "unclassified_status": {
        "basis": "inferred",
        "reason": "Final fallback; any hit requires priority review.",
    },
}

# Keep the schema explicit even when a rule has no falsifier yet. This makes
# missing counterexample queries countable technical debt.
for _definition in RULE_DEFINITIONS.values():
    _definition.setdefault("falsifier", None)
    _definition.setdefault("falsifier_test", None)

CLOSURE_PROMOTION_DEFINITION = {
    "basis": "verified",
    "reason": (
        "A material required by an active-layer components edge must be at "
        "least as active as its strongest consumer."
    ),
    "falsifier": (
        "A promoted material has no transitive components path from the "
        "recorded active-layer consumer."
    ),
    "falsifier_test": "test_component_closure_promotion_reaches_fixed_point",
}


def rule_definition(rule: str) -> dict[str, Any]:
    definition = RULE_DEFINITIONS.get(rule)
    if definition is None:
        raise RuntimeError(f"L1b rule missing RULE_DEFINITIONS metadata: {rule}")
    return definition

# Natural breakpoints from L1b histograms.
RECIPE_BUCKETS = ("0", "1-49", "50+")
PREFIX_BUCKETS = ("0", "1", "5+")


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def evidence_appearance_count(evidence: dict[str, Any] | None) -> int:
    """Sum item+fluid hit counts from an evidence dict {kind: {map: n}}."""
    total = 0
    for kind in ("item", "fluid"):
        for count in ((evidence or {}).get(kind) or {}).values():
            try:
                total += int(count)
            except (TypeError, ValueError):
                continue
    return total


def material_evidence_for_source(
    evidence_by_id: dict[int, dict[str, dict[str, int]]],
    source_id: Any,
) -> dict[str, dict[str, int]]:
    """Return evidence for stable IDs without ever looking up sentinel id=-1."""
    if not isinstance(source_id, int) or source_id < 0:
        return {}
    return evidence_by_id.get(source_id, {})


def bucket_recipe(n: int) -> str:
    if n <= 0:
        return "0"
    if n < 50:
        return "1-49"
    return "50+"


def bucket_prefix(n: int) -> str:
    """Coarse three-state bucket (2-4 collapses into 5+ visually as 'many')."""
    if n <= 0:
        return "0"
    if n == 1:
        return "1"
    return "5+"


def bucket_prefix_fine(n: int) -> str:
    if n <= 0:
        return "0"
    if n == 1:
        return "1"
    if n <= 4:
        return "2-4"
    return "5+"


def build_registered_prefix_index(
    prefixes: list[dict[str, Any]],
) -> tuple[dict[str, int], dict[str, list[str]]]:
    """source_name -> prefix count, and source_name -> prefix names."""
    counts: Counter[str] = Counter()
    names: dict[str, list[str]] = defaultdict(list)
    for prefix in prefixes:
        pname = prefix.get("source_name") or ""
        for name in prefix.get("registered_materials") or []:
            if isinstance(name, str) and name:
                counts[name] += 1
                names[name].append(pname)
    return dict(counts), {k: sorted(v) for k, v in names.items()}


def build_full_recipe_evidence(
    recipe_root: Path,
    fluid_map: dict[str, dict[str, Any]],
) -> tuple[
    dict[int, dict[str, dict[str, int]]],
    dict[str, int],
]:
    """Count material hits across every recipe map under dump/maps."""
    sys.path.insert(0, str(TOOLS))
    import import_gt6_oredict as oredict  # noqa: WPS433

    index = load(recipe_root / "index.json")
    entries = index.get("maps") or []
    unnamed = [
        entry
        for entry in entries
        if not (entry.get("nameInternal") or "")
    ]
    nonempty_unnamed = [
        entry
        for entry in unnamed
        if int(entry.get("recipeCount") or 0) != 0
    ]
    if nonempty_unnamed:
        raise SystemExit(
            "cannot collect appearance evidence from nonempty unnamed maps: "
            f"{nonempty_unnamed}"
        )
    map_names = sorted(
        entry.get("nameInternal") or ""
        for entry in entries
        if entry.get("nameInternal")
    )
    inventory = {
        "index_map_count": len(entries),
        "named_map_count_scanned": len(map_names),
        "empty_unnamed_map_count": len(unnamed),
        "nonempty_unscanned_map_count": 0,
    }
    print(
        f"Scanning {len(map_names)} named recipe maps for full appearances "
        f"({len(entries)} total; {len(unnamed)} empty unnamed)..."
    )
    return (
        oredict.recipe_material_evidence(
            recipe_root,
            map_names,
            fluid_map,
        ),
        inventory,
    )


def detect_row_conflicts(
    record: dict[str, Any],
    rule: str,
    case_collision_names: set[str],
    override_include: set[str],
    override_exclude: set[str],
    full_appearances: int,
    full_evidence: dict[str, Any],
    reviewed_evidence: dict[str, Any],
    template_appearances: int | None = None,
) -> list[str]:
    """Per-row conflict tags (closure conflicts applied later)."""
    hits: list[str] = []
    status = record["status"]
    has_item_full = bool((full_evidence or {}).get("item") or {})
    has_fluid_full = bool((full_evidence or {}).get("fluid") or {})
    has_item_rev = bool((reviewed_evidence or {}).get("item") or {})
    has_fluid_rev = bool((reviewed_evidence or {}).get("fluid") or {})
    name = record["source_name"]
    intentional_hot_drop = (
        rule in INTENTIONAL_HOT_DROP_RULES
        or rule == "fictional_material_shelved"
        or rule.startswith("element_")
        or rule.startswith("isotope_")
    )

    if name in case_collision_names:
        hits.append("case_collision")
    if status == "ACTIVE" and not has_item_full and not has_fluid_full:
        hits.append("active_without_evidence")
    if status == "DEFERRED" and has_item_full and full_appearances >= 50:
        hits.append("deferred_with_strong_item_evidence")
    if status == "DEFERRED" and has_item_full and 0 < full_appearances < 50:
        hits.append("deferred_with_mid_item_evidence")
    if status == "ACTIVE" and has_fluid_full and not has_item_full:
        hits.append("active_fluid_only")
    # Ownership parking is not an evidence fight for quiet OUT_OF_SCOPE rows,
    # but ACTIVE evidence against PENDING_T0B is a real classifier conflict
    # (SiliconDioxide / Steel / Bronze class). Prefer template heat when the
    # caller provides it: expanded mixer foam rows otherwise dominate.
    if rule == "pending_t0b_non_native_ownership":
        hot = (
            template_appearances >= 3
            if template_appearances is not None
            else full_appearances >= 50
        )
        strong = (
            template_appearances >= 1
            if template_appearances is not None
            else (has_item_full or has_fluid_full)
        )
        if status == "ACTIVE" and hot:
            hits.append("pending_t0b_with_hot_full_evidence")
        elif status == "ACTIVE" and strong:
            hits.append("pending_t0b_with_full_evidence")
        elif status == "DEFERRED" and (
            (template_appearances or 0) >= 3
            if template_appearances is not None
            else (has_item_full and full_appearances >= 50)
        ):
            hits.append("pending_t0b_deferred_with_strong_item_evidence")
    if status == "OUT_OF_SCOPE" and not intentional_hot_drop:
        if full_appearances >= 50:
            hits.append("out_of_scope_with_hot_full_evidence")
        elif has_item_full or has_fluid_full:
            hits.append("out_of_scope_with_full_evidence")
        if has_item_rev or has_fluid_rev:
            hits.append("out_of_scope_with_reviewed_evidence")
    if status == "OUT_OF_SCOPE" and not has_item_full and not has_fluid_full:
        if record.get("available_t0_forms") or record.get("t0_forms"):
            hits.append("out_of_scope_but_has_t0_forms")
    if name in override_include and status != "ACTIVE":
        hits.append("override_include_not_active")
    if name in override_exclude and (
            has_item_full or has_fluid_full or status == "ACTIVE"):
        hits.append("override_exclude_but_live_evidence")
    if rule == "unclassified_status":
        hits.append("unclassified_rule")
    return hits


# Rules that intentionally drop despite recipe traffic — not classifier/evidence fights.
INTENTIONAL_HOT_DROP_RULES = {
    "tag_food",
    "explicit_food_exclude",
    "tag_dont_show_component",
    "empty_material_slot",
    "explicit_special_exclude",
    "explicit_compat_exclude",
    "tag_invalid_material",
    "no_stable_source_id",
}


def classify(
    record: dict[str, Any],
    material: dict[str, Any],
    full_appearances: int = 0,
) -> tuple[str, str]:
    """Return (layer, rule_id). Two-dimensional: structural_class × originalMod.

    Element-like ids ignore ownership/hidden for keep-or-defer decisions.
    Alloy/organic non-native mods → PENDING_T0B (await T0b interop inventory).
    """
    status = record["status"]
    reason = record.get("reason") or ""
    tags = set(material.get("material_tags") or [])
    evidence = record.get("recipe_evidence") or {}
    has_recipe_item_evidence = bool((evidence.get("item") or {}))
    has_recipe_fluid_evidence = bool((evidence.get("fluid") or {}))
    forms = record.get("t0_forms") or []
    source_id = record.get("source_id", -1)
    source_name = record.get("source_name") or ""
    original_mod = material.get("original_mod")
    sc = structural_class(source_id)
    element_like = is_element_like(source_id)

    # --- structural hard stops ---
    if sc == "no_id" or (isinstance(source_id, int) and source_id < 0):
        return "NEVER", "no_stable_source_id"
    if "PROPERTIES.INVALID_MATERIAL" in tags:
        return "NEVER", "tag_invalid_material"
    if "PROPERTIES.DONT_SHOW_THIS_COMPONENT" in tags and status == "OUT_OF_SCOPE":
        return "NEVER", "tag_dont_show_component"
    if source_id == 0 or source_name == "Empty":
        return "NEVER", "empty_material_slot"

    # Verified post-periodic fictional band: preserve metadata, but do not put
    # it in the active phase queue or T0b ownership queue.
    if sc == "fictional_material":
        return "SHELVED_FICTIONAL", "fictional_material_shelved"

    # --- alloy / organic: ownership is primary ---
    ownership_gated = sc in {"alloy_compound", "organic_misc"}
    if ownership_gated and not is_native_mod(original_mod):
        return "PENDING_T0B", "pending_t0b_non_native_ownership"

    # --- element-like: ignore ownership / hidden; foundation of GT ---
    if element_like:
        if reason.startswith("preserved authored"):
            return "CORE", "authored_include"
        if status == "ACTIVE" and forms and has_recipe_item_evidence:
            return "CORE", "element_active_item_evidence"
        if status == "ACTIVE" and record.get("metadata_only") and (
                has_recipe_fluid_evidence or "dependency" in reason
                or reason.startswith("transitive")
                or reason.startswith("Legitimate")):
            return "CORE", "element_active_fluid_or_dependency"
        if status == "ACTIVE" and has_recipe_item_evidence and not forms:
            return "PHASE_2", "element_active_formless"
        if status == "DEFERRED" and has_recipe_item_evidence:
            return "PHASE_2", "element_deferred_item_evidence"
        if has_recipe_fluid_evidence and not has_recipe_item_evidence:
            return "PHASE_3", "element_fluid_only"
        # Hidden gregapi rare-earths etc. still belong in the table.
        if material.get("hidden") and full_appearances >= 50:
            return "PHASE_LATER", "element_hidden_but_hot"
        if material.get("hidden"):
            return "PHASE_LATER", "element_hidden_cold"
        if status == "DEFERRED":
            return "PHASE_LATER", "element_deferred"
        if status == "ACTIVE":
            return "PHASE_LATER", "element_active_unclassified"
        if status == "OUT_OF_SCOPE":
            return "PHASE_LATER", "element_out_of_scope_retained"
        return "PHASE_LATER", "element_unclassified"

    # --- isotopes: common keep; obscure defer (still not ownership-gated) ---
    if sc == "isotope":
        common = is_common_isotope(source_id, source_name)
        if reason.startswith("preserved authored"):
            return "CORE", "authored_include"
        if common and status == "ACTIVE" and forms and has_recipe_item_evidence:
            return "CORE", "isotope_common_active"
        if common and status == "ACTIVE" and (
                has_recipe_item_evidence
                or has_recipe_fluid_evidence
                or record.get("metadata_only")):
            return "CORE", "isotope_common_active_loose"
        if common and (
                has_recipe_item_evidence
                or has_recipe_fluid_evidence
                or full_appearances > 0):
            return "PHASE_2", "isotope_common_deferred"
        if common:
            return "PHASE_LATER", "isotope_common_cold"
        return "PHASE_LATER", "isotope_obscure_deferred"

    # --- native alloy / organic / midband-other: prior empirical rules ---
    if "PROPERTIES.FOOD" in tags and status == "OUT_OF_SCOPE":
        return "NEVER", "tag_food"
    if status == "OUT_OF_SCOPE" and reason.startswith("Food "):
        return "NEVER", "explicit_food_exclude"
    if status == "OUT_OF_SCOPE" and "Special indestructible" in reason:
        return "NEVER", "explicit_special_exclude"
    if status == "OUT_OF_SCOPE" and "Compatibility/special" in reason:
        return "NEVER", "explicit_compat_exclude"
    if material.get("hidden") and status == "OUT_OF_SCOPE" and "hidden" in reason:
        if full_appearances >= 50:
            return "NEEDS_REVIEW", "hidden_hot_residual_review"
        return "NEEDS_REVIEW", "hidden_cold_residual_review"

    if reason.startswith("preserved authored"):
        return "CORE", "authored_include"
    if status == "ACTIVE" and forms and has_recipe_item_evidence:
        return "CORE", "active_item_evidence"
    if status == "ACTIVE" and record.get("metadata_only") and (
            has_recipe_fluid_evidence or reason.startswith("transitive")
            or reason.startswith("Legitimate")
            or "dependency" in reason):
        if (
            has_recipe_fluid_evidence
            or "dependency" in reason
            or reason.startswith("transitive")
        ):
            return "CORE", "active_fluid_or_dependency_closure"

    if status == "DEFERRED" and has_recipe_item_evidence:
        return "PHASE_2", "deferred_item_evidence_no_t0_form"
    if status == "ACTIVE" and has_recipe_item_evidence and not forms:
        return "PHASE_2", "active_item_evidence_formless"

    if (
        status == "ACTIVE"
        and has_recipe_fluid_evidence
        and not has_recipe_item_evidence
    ):
        return "PHASE_3", "active_fluid_only"
    if status == "DEFERRED" and has_recipe_fluid_evidence:
        return "PHASE_3", "deferred_fluid_evidence"

    if status == "DEFERRED":
        return "PHASE_LATER", "deferred_no_reviewed_map_evidence"
    if status == "ACTIVE":
        return "PHASE_LATER", "active_without_classified_evidence"

    if status == "OUT_OF_SCOPE":
        if (
            not has_recipe_item_evidence
            and not has_recipe_fluid_evidence
        ):
            if original_mod and not is_native_mod(original_mod):
                return "COMPAT_DEAD", "cross_mod_no_reviewed_evidence"
            return "COMPAT_DEAD", "no_reviewed_evidence"
        return "COMPAT_DEAD", "out_of_scope_with_reviewed_evidence"

    return "NEVER", "unclassified_status"


def promote_component_closure(
    all_rows: dict[str, dict[str, Any]],
    materials_by_name: dict[str, dict],
    explicit_excludes: set[str],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Promote component dependencies to a fixed point.

    Active layer strength is CORE > PHASE_2 > PHASE_3 > PHASE_LATER.
    Explicit excludes are never promoted and remain hard closure blockers.
    """
    aliases: dict[str, str] = {}
    for name, material in materials_by_name.items():
        aliases[name] = name
        for alias in material.get("aliases") or []:
            aliases[alias] = name

    promotions: list[dict[str, Any]] = []
    blocked: list[dict[str, Any]] = []
    blocked_keys: set[tuple[str, str]] = set()
    changed = True
    iteration = 0
    while changed:
        changed = False
        iteration += 1
        for consumer_name, consumer_row in sorted(all_rows.items()):
            consumer_layer = consumer_row["layer"]
            if consumer_layer not in ACTIVE_LAYER_RANK:
                continue
            material = materials_by_name.get(consumer_name) or {}
            for component in material.get("components") or []:
                raw_target = component.get("material")
                target_name = aliases.get(raw_target, raw_target)
                target_row = all_rows.get(target_name)
                if target_row is None:
                    continue
                if target_name in explicit_excludes:
                    key = (consumer_name, target_name)
                    if key not in blocked_keys:
                        blocked_keys.add(key)
                        blocked.append({
                            "consumer": consumer_name,
                            "consumer_layer": consumer_layer,
                            "target": target_name,
                            "target_layer": target_row["layer"],
                            "reason": "explicit_activation_exclude",
                        })
                    continue

                target_layer = target_row["layer"]
                target_rank = ACTIVE_LAYER_RANK.get(target_layer)
                consumer_rank = ACTIVE_LAYER_RANK[consumer_layer]
                if target_rank is not None and target_rank <= consumer_rank:
                    continue

                previous_layer = target_layer
                target_row["layer"] = consumer_layer
                promotion = target_row.setdefault("promoted_by_closure", {
                    "basis": CLOSURE_PROMOTION_DEFINITION["basis"],
                    "reason": CLOSURE_PROMOTION_DEFINITION["reason"],
                    "initial_layer": target_row.get("initial_layer", previous_layer),
                    "effective_layer": consumer_layer,
                    "consumers": [],
                    "paths": [],
                })
                promotion["effective_layer"] = consumer_layer
                if consumer_name not in promotion["consumers"]:
                    promotion["consumers"].append(consumer_name)
                source_paths = (
                    (consumer_row.get("promoted_by_closure") or {}).get("paths")
                    or [[consumer_name]]
                )
                for source_path in source_paths:
                    path = [*source_path, target_name]
                    if path not in promotion["paths"]:
                        promotion["paths"].append(path)
                promotions.append({
                    "iteration": iteration,
                    "material": target_name,
                    "from_layer": previous_layer,
                    "to_layer": consumer_layer,
                    "consumer": consumer_name,
                    "path": promotion["paths"][-1],
                    "original_rule": target_row["rule"],
                })
                changed = True

    return promotions, blocked


def closure_violations(
    core: dict[str, dict[str, Any]],
    materials_by_name: dict[str, dict],
    all_rows: dict[str, dict[str, Any]],
) -> list[dict]:
    """CORE references outside CORE, annotated by edge type and target layer."""
    core_names = set(core)
    alias_to_name = {}
    for name in core:
        mat = materials_by_name.get(name) or {}
        for alias in mat.get("aliases") or []:
            alias_to_name[alias] = name
        alias_to_name[name] = name

    def add_violation(
        violations: list[dict[str, Any]],
        source: str,
        edge: str,
        edge_type: str,
        target: str | None,
    ) -> None:
        if not target or target in core_names or target in alias_to_name:
            return
        target_row = all_rows.get(target)
        violations.append({
            "material": source,
            "edge": edge,
            "edge_type": edge_type,
            "target": target,
            "source_layer": "CORE",
            "target_layer": (
                target_row["layer"] if target_row is not None else "MISSING"
            ),
            "severity": (
                "HARD_FAIL" if edge_type == "components"
                else "DEGRADED_ROUTE" if edge_type == "targets"
                else "OPTIONAL_LOSS"
            ),
        })

    violations = []
    for name in core:
        mat = materials_by_name.get(name) or {}
        for comp in mat.get("components") or []:
            add_violation(
                violations, name, "components", "components", comp.get("material")
            )
        for tname, target in (mat.get("processing_targets") or {}).items():
            add_violation(
                violations,
                name,
                f"targets.{tname}",
                "targets",
                target.get("material"),
            )
        for bp in mat.get("byproducts") or []:
            target = (
                bp if isinstance(bp, str)
                else bp.get("material") if isinstance(bp, dict)
                else None
            )
            add_violation(violations, name, "byproducts", "byproducts", target)
    return violations


def degraded_path_stage(edge: str) -> str:
    target_type = edge.removeprefix("targets.")
    if target_type in {"crushing", "smelting", "pulver"}:
        return "T2_MINERAL_CHAIN"
    if target_type in {
        "bending",
        "compressing",
        "cutting",
        "forging",
        "smashing",
        "working",
    }:
        return "T3_FORMING"
    return "T2_SCOPE_REVIEW"


def print_cross_table(cross: dict[str, dict[str, Counter[str]]]) -> None:
    print("cross table recipe × prefix → layer counts:")
    header = f"{'':18}" + "".join(f"{p:>14}" for p in PREFIX_BUCKETS)
    print(header)
    for rb in RECIPE_BUCKETS:
        cells = []
        for pb in PREFIX_BUCKETS:
            layers = cross[rb][pb]
            total = sum(layers.values())
            top = ",".join(f"{k}:{v}" for k, v in layers.most_common(3)) or "-"
            cells.append(f"{total:>4}[{top}]")
        print(f"recipe={rb:<6}" + "".join(f"{c:>14}" for c in cells))


def main() -> int:
    materials = load(TOOLS / "gt6_oredict_materials_normalized.json")["records"]
    raw_materials = load(DUMP / "oredict" / "materials.json")
    policy = load(TOOLS / "gt6_material_activation_policy.json")["records"]
    prefixes = load(TOOLS / "gt6_oredict_prefixes_normalized.json")["records"]
    overrides = load(TOOLS / "gt6_material_activation_overrides.json")
    by_name = {m["source_name"]: m for m in materials}
    raw_by_name = {m["nameInternal"]: m for m in raw_materials}
    nonself_inbound: dict[str, list[dict[str, str]]] = defaultdict(list)
    for material in materials:
        source = material["source_name"]
        for component in material.get("components") or []:
            target = component.get("material")
            if target and target != source:
                nonself_inbound[target].append({
                    "source": source,
                    "edge_type": "components",
                })
        for target_name, target in (material.get("processing_targets") or {}).items():
            dest = target.get("material") if isinstance(target, dict) else None
            if dest and dest != source:
                nonself_inbound[dest].append({
                    "source": source,
                    "edge_type": f"targets.{target_name}",
                })
        for byproduct in material.get("byproducts") or []:
            dest = (
                byproduct if isinstance(byproduct, str)
                else byproduct.get("material") if isinstance(byproduct, dict)
                else None
            )
            if dest and dest != source:
                nonself_inbound[dest].append({
                    "source": source,
                    "edge_type": "byproducts",
                })
    registered_prefix_counts, registered_prefix_names = build_registered_prefix_index(prefixes)

    fluid_map = load(DUMP / "oredict" / "fluid_map.json")
    if not isinstance(fluid_map, dict):
        raise SystemExit("oredict/fluid_map.json must be an object keyed by fluid id")
    full_by_id, recipe_map_inventory = build_full_recipe_evidence(
        DUMP,
        fluid_map,
    )
    negative_evidence_ids = sorted(
        material_id
        for material_id in full_by_id
        if not isinstance(material_id, int) or material_id < 0
    )
    if negative_evidence_ids:
        raise SystemExit(
            "recipe appearance evidence contains invalid material IDs: "
            f"{negative_evidence_ids[:20]}"
        )
    template_heat_path = TOOLS / "gt6_recipe_template_material_heat.json"
    template_heat_by_id: dict[int, dict[str, Any]] = {}
    if template_heat_path.is_file():
        template_heat_document = json.loads(
            template_heat_path.read_text(encoding="utf-8")
        )
        # Template heat is a tiering input only after every contributing map
        # has passed exact replay. Legacy/count-only artifacts are diagnostics.
        if template_heat_document.get("replay_verified") is True:
            for row in template_heat_document.get("records") or []:
                mat_id = row.get("material_id")
                if isinstance(mat_id, int) and mat_id < 0:
                    raise SystemExit(
                        "template appearance evidence contains negative "
                        f"material id {mat_id}"
                    )
                if isinstance(mat_id, int):
                    template_heat_by_id[mat_id] = row

    override_include = set((overrides.get("include") or {}).keys())
    override_exclude = set((overrides.get("exclude") or {}).keys())
    lower_groups: dict[str, list[str]] = defaultdict(list)
    for record in policy:
        lower_groups[record["source_name"].lower()].append(record["source_name"])
    case_collision_names = {
        name
        for names in lower_groups.values()
        if len(set(names)) > 1
        for name in names
    }
    stable_policy_names = {
        record["source_name"]
        for record in policy
        if isinstance(record.get("source_id"), int)
        and int(record["source_id"]) >= 0
    }
    stable_lower_groups: dict[str, list[str]] = defaultdict(list)
    for name in stable_policy_names:
        stable_lower_groups[name.lower()].append(name)

    selected: dict[str, dict[str, Any]] = {}
    dropped: dict[str, dict[str, Any]] = {}
    needs_review: dict[str, dict[str, Any]] = {}
    pending_t0b: dict[str, dict[str, Any]] = {}
    layer_counts: Counter[str] = Counter()
    rule_counts: Counter[str] = Counter()
    basis_counts: Counter[str] = Counter()
    inferred_rule_counts: Counter[str] = Counter()
    external_dependency_counts: Counter[str] = Counter()
    structural_layer: dict[str, Counter[str]] = defaultdict(Counter)
    appearance_hist: Counter[str] = Counter()
    reviewed_hist: Counter[str] = Counter()
    prefix_hist: Counter[str] = Counter()
    prefix_fine_hist: Counter[str] = Counter()
    single_prefix_names: Counter[str] = Counter()
    conflict_counts: Counter[str] = Counter()
    materials_with_any_conflict = 0
    cross: dict[str, dict[str, Counter[str]]] = {
        rb: {pb: Counter() for pb in PREFIX_BUCKETS} for rb in RECIPE_BUCKETS
    }
    hot_out: list[dict[str, Any]] = []

    for record in policy:
        mat = by_name.get(record["source_name"]) or {}
        source_id = record.get("source_id", -1)
        reviewed_evidence = record.get("recipe_evidence") or {}
        full_evidence = material_evidence_for_source(
            full_by_id,
            source_id,
        )
        full_n = evidence_appearance_count(full_evidence)
        reviewed_n = evidence_appearance_count(reviewed_evidence)
        template_row = (
            template_heat_by_id.get(source_id)
            if isinstance(source_id, int) and source_id >= 0
            else None
        )
        template_n = (
            int(template_row["template_appearances"])
            if template_row is not None
            else None
        )
        layer, rule = classify(record, mat, full_n)
        definition = rule_definition(rule)
        basis = definition["basis"]
        sc = structural_class(source_id)
        prefix_n = int(registered_prefix_counts.get(record["source_name"]) or 0)
        prefix_list = registered_prefix_names.get(record["source_name"]) or []
        include_in_summary = (
            isinstance(source_id, int) and source_id >= 0
        )

        rb = bucket_recipe(full_n)
        pb = bucket_prefix(prefix_n)
        if include_in_summary:
            layer_counts[layer] += 1
            rule_counts[rule] += 1
            basis_counts[basis] += 1
            if basis == "inferred":
                inferred_rule_counts[rule] += 1
            if basis == "external" and definition.get("external_dependency"):
                external_dependency_counts[
                    definition["external_dependency"]
                ] += 1
            structural_layer[sc][layer] += 1
            appearance_hist[rb] += 1
            reviewed_hist[bucket_recipe(reviewed_n)] += 1
            prefix_hist[pb] += 1
            prefix_fine_hist[bucket_prefix_fine(prefix_n)] += 1
            cross[rb][pb][layer] += 1
            if prefix_n == 1 and prefix_list:
                single_prefix_names[prefix_list[0]] += 1

        conflicts = detect_row_conflicts(
            record,
            rule,
            case_collision_names,
            override_include,
            override_exclude,
            full_n,
            full_evidence,
            reviewed_evidence,
            template_n,
        )
        # Quiet OUT_OF_SCOPE ownership parking is intentional; strip those tags.
        # Keep pending_t0b_with_* evidence fights — they flag SiliconDioxide-class
        # misparks where ACTIVE/hot rows were parked solely by originalMod.
        if layer == "PENDING_T0B":
            conflicts = [c for c in conflicts if not c.startswith("out_of_scope_")]
        row = {
            "source_id": source_id,
            "source_name": record["source_name"],
            "cc_id": record.get("cc_id"),
            "structural_class": sc,
            "element_like": is_element_like(source_id),
            "activation_status": record["status"],
            "activation_reason": record.get("reason"),
            "initial_layer": layer,
            "layer": layer,
            "rule": rule,
            "rule_basis": basis,
            "rule_basis_reason": definition["reason"],
            "rule_falsifier": definition["falsifier"],
            "rule_falsifier_test": definition["falsifier_test"],
            "review_priority": "inferred_rule" if basis == "inferred" else None,
            "t0_forms": record.get("t0_forms") or [],
            "recipe_evidence": reviewed_evidence,
            "recipe_appearances": full_n,
            "template_appearances": template_n,
            "reviewed_appearances": reviewed_n,
            "registered_prefix_count": prefix_n,
            "registered_prefixes": prefix_list,
            "conflicts": conflicts,
            "original_mod": mat.get("original_mod"),
            "texture_set_item": mat.get("texture_set_item"),
        }
        if layer == "NEEDS_REVIEW":
            row["alias_graph"] = {
                "re_registrations": (
                    raw_by_name.get(record["source_name"], {}).get("reRegistrations")
                    or []
                ),
                "to_this": (
                    raw_by_name.get(record["source_name"], {}).get("toThis") or []
                ),
                "source_of": (
                    raw_by_name.get(record["source_name"], {}).get("sourceOf") or []
                ),
            }
            row["nonself_closure_inbound"] = nonself_inbound.get(
                record["source_name"], []
            )
        if layer == "NEEDS_REVIEW":
            needs_review[record["source_name"]] = row
        elif layer == "PENDING_T0B":
            pending_t0b[record["source_name"]] = row
        elif layer in {"NEVER", "COMPAT_DEAD"}:
            dropped[record["source_name"]] = row
            if full_n >= 50:
                hot_out.append(row)
        else:
            selected[record["source_name"]] = row

    all_rows = {
        **selected,
        **dropped,
        **needs_review,
        **pending_t0b,
    }
    promotions, blocked_promotions = promote_component_closure(
        all_rows,
        by_name,
        override_exclude,
    )

    # Repartition and rebuild all layer-dependent counters after the transitive
    # closure promotion reaches its fixed point.
    selected = {}
    dropped = {}
    needs_review = {}
    pending_t0b = {}
    layer_counts = Counter()
    structural_layer = defaultdict(Counter)
    cross = {
        rb: {pb: Counter() for pb in PREFIX_BUCKETS} for rb in RECIPE_BUCKETS
    }
    hot_out = []
    for name, row in all_rows.items():
        layer = row["layer"]
        include_in_summary = (
            isinstance(row.get("source_id"), int)
            and int(row["source_id"]) >= 0
        )
        if include_in_summary:
            layer_counts[layer] += 1
            structural_layer[row["structural_class"]][layer] += 1
            cross[
                bucket_recipe(int(row["recipe_appearances"]))
            ][
                bucket_prefix(int(row["registered_prefix_count"]))
            ][layer] += 1
        if layer == "NEEDS_REVIEW":
            needs_review[name] = row
        elif layer == "PENDING_T0B":
            pending_t0b[name] = row
        elif layer in {"NEVER", "COMPAT_DEAD"}:
            dropped[name] = row
            if (
                include_in_summary
                and int(row["recipe_appearances"]) >= 50
            ):
                hot_out.append(row)
        else:
            selected[name] = row

    for row in (
        list(selected.values())
        + list(dropped.values())
        + list(needs_review.values())
        + list(pending_t0b.values())
    ):
        if (
            not isinstance(row.get("source_id"), int)
            or int(row["source_id"]) < 0
        ):
            continue
        conflicts = row.get("conflicts") or []
        if conflicts:
            materials_with_any_conflict += 1
        for tag in conflicts:
            conflict_counts[tag] += 1

    core = {k: v for k, v in selected.items() if v["layer"] == "CORE"}
    violations = closure_violations(core, by_name, all_rows)
    closure_cross: dict[str, Counter[str]] = defaultdict(Counter)
    for violation in violations:
        closure_cross[violation["edge_type"]][violation["target_layer"]] += 1
    degraded_paths = [
        {
            **violation,
            "target_type": violation["edge"].removeprefix("targets."),
            "prerequisite_stage": degraded_path_stage(violation["edge"]),
            "action": "retain_source_without_this_generated_route",
        }
        for violation in violations
        if violation["edge_type"] == "targets"
    ]
    byproduct_stripping = [
        {
            **violation,
            "action": "strip_reference_during_L3_generation",
        }
        for violation in violations
        if violation["edge_type"] == "byproducts"
    ]
    for entry in degraded_paths:
        all_rows[entry["material"]].setdefault("degraded_paths", []).append({
            "target_type": entry["target_type"],
            "target": entry["target"],
            "target_layer": entry["target_layer"],
            "prerequisite_stage": entry["prerequisite_stage"],
        })
    for entry in byproduct_stripping:
        all_rows[entry["material"]].setdefault("stripped_byproducts", []).append({
            "target": entry["target"],
            "target_layer": entry["target_layer"],
            "reason": "target_outside_core",
        })
    hard_component_violations = [
        violation
        for violation in violations
        if violation["edge_type"] == "components"
    ]
    closure_gate = {
        "ok": not hard_component_violations,
        "hard_component_violation_count": len(hard_component_violations),
        "all_reference_violation_count": len(violations),
        "promotion_event_count": len(promotions),
        "promoted_material_count": len({
            event["material"] for event in promotions
        }),
        "blocked_promotion_count": len(blocked_promotions),
        "policy": (
            "components dependencies auto-promote transitively to the strongest "
            "consumer layer; unresolved or explicitly blocked CORE components "
            "hard-fail. Processing targets degrade routes; byproducts are stripped."
        ),
        "edge_type_x_target_layer": {
            edge_type: dict(sorted(target_layers.items()))
            for edge_type, target_layers in sorted(closure_cross.items())
        },
    }
    partition_sets = {
        "selected": set(selected),
        "dropped": set(dropped),
        "needs_review": set(needs_review),
        "pending_t0b": set(pending_t0b),
    }
    partition_overlap = sorted(
        name
        for name in set().union(*partition_sets.values())
        if sum(name in names for names in partition_sets.values()) != 1
    )
    partition_union = set().union(*partition_sets.values())
    policy_names = {row["source_name"] for row in policy}
    partition_missing = sorted(policy_names - partition_union)
    partition_extra = sorted(partition_union - policy_names)
    partition_ok = not partition_overlap and not partition_missing and not partition_extra

    hot_out.sort(key=lambda r: (-int(r["recipe_appearances"]), r["source_name"]))
    hot_out_sample = [
        {
            "source_name": r["source_name"],
            "recipe_appearances": r["recipe_appearances"],
            "reviewed_appearances": r["reviewed_appearances"],
            "layer": r["layer"],
            "rule": r["rule"],
            "activation_status": r["activation_status"],
            "activation_reason": r["activation_reason"],
            "registered_prefix_count": r["registered_prefix_count"],
            "registered_prefixes_head": (r.get("registered_prefixes") or [])[:8],
        }
        for r in hot_out[:10]
    ]

    policy_by_name = {
        record["source_name"]: record for record in policy
    }
    sentinel_rows = {
        name: row
        for name, row in all_rows.items()
        if not isinstance(row.get("source_id"), int)
        or int(row["source_id"]) < 0
    }
    sentinel_t0_silent_names = {
        name
        for name, row in sentinel_rows.items()
        if int(row["recipe_appearances"]) == 0
        and bool(
            policy_by_name.get(name, {}).get("available_t0_forms")
        )
    }
    stable_t0_silent_names = {
        name
        for name, row in all_rows.items()
        if isinstance(row.get("source_id"), int)
        and int(row["source_id"]) >= 0
        and int(row["recipe_appearances"]) == 0
        and bool(
            policy_by_name.get(name, {}).get("available_t0_forms")
        )
    }
    sentinel_original_mods = Counter(
        (
            raw_by_name.get(name, {}).get("originalMod")
            or "<unknown>"
        )
        for name in sentinel_rows
    )
    sentinel_prefix_histogram = Counter(
        int(row["registered_prefix_count"])
        for row in sentinel_rows.values()
    )
    sentinel_fluid_rows = sorted(
        {
            (
                fluid_id,
                value.get("material"),
            )
            for fluid_id, value in fluid_map.items()
            if isinstance(value, dict)
            and value.get("materialId") == -1
        }
    )
    sentinel_compatibility_surface = {
        "silent_zone_investigation_status": "CLOSED",
        "record_count": len(sentinel_rows),
        "all_classified_never": all(
            row["layer"] == "NEVER" for row in sentinel_rows.values()
        ),
        "closure_promoted_count": sum(
            bool(row.get("promoted_by_closure"))
            for row in sentinel_rows.values()
        ),
        "zero_full_appearance_count": sum(
            int(row["recipe_appearances"]) == 0
            for row in sentinel_rows.values()
        ),
        "zero_reviewed_appearance_count": sum(
            int(row["reviewed_appearances"]) == 0
            for row in sentinel_rows.values()
        ),
        "registered_any_prefix_count": sum(
            int(row["registered_prefix_count"]) > 0
            for row in sentinel_rows.values()
        ),
        "registered_t0_prefix_and_zero_appearance_count": len(
            sentinel_t0_silent_names
        ),
        "stable_registered_t0_prefix_and_zero_appearance_count": len(
            stable_t0_silent_names
        ),
        "stable_registered_t0_prefix_and_zero_appearance_names": sorted(
            stable_t0_silent_names
        ),
        "same_name_overlap_with_stable_silent_zone_count": len(
            sentinel_t0_silent_names & stable_t0_silent_names
        ),
        "registered_prefix_count_histogram": {
            str(count): members
            for count, members in sorted(
                sentinel_prefix_histogram.items()
            )
        },
        "original_mod_known_count": len(sentinel_rows)
        - sentinel_original_mods["<unknown>"],
        "original_mod_distribution": dict(
            sentinel_original_mods.most_common()
        ),
        "negative_id_fluid_map_records": [
            {
                "fluid": fluid_id,
                "material": material,
            }
            for fluid_id, material in sentinel_fluid_rows
        ],
        "interpretation": (
            "Name-only compatibility surface: prefix registration proves an "
            "OreDict item registration event, while zero recipe appearance "
            "shows no recipe-map use in this dump environment."
        ),
        "silent_zone_conclusion": (
            "The earlier ~285-290 registered-item/zero-recipe estimate was "
            "the name-only sentinel population. Under the stable T0-prefix "
            "scope only ClayBrick remains; this is not a missed-recipe bulk "
            "population."
        ),
    }

    histograms = {
        "excludes_sentinels": True,
        "source_record_count": len(policy),
        "summary_material_count": len(stable_policy_names),
        "recipe_appearances_full": {k: appearance_hist[k] for k in RECIPE_BUCKETS},
        "reviewed_appearances": {k: reviewed_hist[k] for k in RECIPE_BUCKETS},
        "registered_prefix_count": {k: prefix_hist[k] for k in PREFIX_BUCKETS},
        "registered_prefix_count_fine": {
            k: prefix_fine_hist[k] for k in ("0", "1", "2-4", "5+")
        },
        "single_prefix_name": dict(single_prefix_names.most_common()),
        "conflict_types": dict(conflict_counts.most_common()),
        "materials_with_any_conflict": materials_with_any_conflict,
        "case_collision_groups": sum(
            1
            for names in stable_lower_groups.values()
            if len(set(names)) > 1
        ),
        "hot_out_count": len(hot_out),
        "structural_class_x_layer": {
            sc: dict(layers.most_common())
            for sc, layers in sorted(structural_layer.items())
        },
        "cross_recipe_x_prefix_layers": {
            rb: {
                pb: dict(cross[rb][pb].most_common())
                for pb in PREFIX_BUCKETS
            }
            for rb in RECIPE_BUCKETS
        },
        "hot_out_sample": hot_out_sample,
    }

    report = {
        "schema_version": 2,
        "excludes_sentinels": True,
        "universe": len(stable_policy_names),
        "source_record_count": len(policy),
        "recipe_map_inventory": recipe_map_inventory,
        "sentinel_compatibility_surface": sentinel_compatibility_surface,
        "rule_invariants": {
            "registered_prefix_zero_stable_material_count": (
                prefix_fine_hist["0"]
            ),
            "classifier_uses_registered_prefix_count": False,
            "recipe_item_evidence_is_not_prefix_registration": True,
            "note": (
                "No L1b classifier rule uses registered_prefix_count. "
                "Rules named *item_evidence* refer only to recipe-map item "
                "appearances, so they remain live even though every stable "
                "material has at least one registered prefix."
            ),
        },
        "closure_gate": closure_gate,
        "closure_promotions": promotions,
        "blocked_closure_promotions": blocked_promotions,
        "closure_violations": violations,
        "degraded_paths": degraded_paths,
        "byproduct_stripping": byproduct_stripping,
        "layer_counts": {layer: layer_counts[layer] for layer in LAYERS},
        "rule_counts": dict(rule_counts.most_common()),
        "rule_basis_counts": {
            basis: basis_counts[basis]
            for basis in ("verified", "inferred", "external")
        },
        "rules": {
            rule: {
                **RULE_DEFINITIONS[rule],
                "hit_count": rule_counts[rule],
            }
            for rule in sorted(rule_counts)
        },
        "inferred_review_priority": {
            "candidate_count": basis_counts["inferred"],
            "by_rule": dict(inferred_rule_counts.most_common()),
            "note": (
                "These rows are natural needs_review priority candidates; "
                "basis alone does not yet route them into a review bucket."
            ),
        },
        "inferred_falsifier_debt": {
            "rule_count_without_falsifier": sum(
                1
                for rule in inferred_rule_counts
                if not RULE_DEFINITIONS[rule]["falsifier"]
            ),
            "material_count_without_falsifier": sum(
                count
                for rule, count in inferred_rule_counts.items()
                if not RULE_DEFINITIONS[rule]["falsifier"]
            ),
            "rules_without_falsifier": {
                rule: count
                for rule, count in inferred_rule_counts.most_common()
                if not RULE_DEFINITIONS[rule]["falsifier"]
            },
            "rule_count_with_regression_test": sum(
                1
                for rule in inferred_rule_counts
                if RULE_DEFINITIONS[rule]["falsifier_test"]
            ),
        },
        "external_dependencies": {
            "material_count": sum(external_dependency_counts.values()),
            "by_dependency": dict(external_dependency_counts.most_common()),
        },
        "selected_count": sum(
            name in stable_policy_names for name in selected
        ),
        "dropped_count": sum(
            name in stable_policy_names for name in dropped
        ),
        "dropped_record_count_including_sentinels": len(dropped),
        "needs_review_count": len(needs_review),
        "needs_review_diagnostics": {
            name: {
                "alias_graph": row.get("alias_graph") or {},
                "recipe_appearances": row["recipe_appearances"],
                "nonself_closure_inbound": row.get("nonself_closure_inbound") or [],
                "direct_disposition": (
                    "NO_VERIFIED_DISPOSITION: alias graph is empty and no "
                    "non-self closure edge exists, but recipe evidence is nonzero"
                ),
            }
            for name, row in sorted(needs_review.items())
        },
        "pending_t0b_count": len(pending_t0b),
        "core_count": layer_counts["CORE"],
        "partition": {
            "ok": partition_ok,
            "overlap": partition_overlap,
            "missing": partition_missing,
            "extra": partition_extra,
        },
        "histograms": histograms,
        "closure_all_clean": not violations,
        "notes": [
            "All layer/rule/conflict/histogram summaries exclude source_id < 0; "
            "name-only records remain in sentinel_compatibility_surface and the "
            "complete dropped-record artifact.",
            "Layers are 2D: structural_class × originalMod (elements ignore ownership).",
            "PENDING_T0B = alloy/organic/midband-other with non-native originalMod; await T0b inventory.",
            "Element-like is verified periodic Z=1..118 only; id%10 alone is not identity outside id 10..1180.",
            "SHELVED_FICTIONAL is the verified id 1181..7999 band; residual hidden rows route to NEEDS_REVIEW.",
            "recipe_appearances is FULL expanded map count; template_appearances "
            "is the decombinatorialized heat from gt6_recipe_templates.",
            "reviewed_appearances is evidence_maps only.",
            "pending_t0b hot conflicts use template_appearances>=3 when available.",
            "case_collision belongs in L1a alias merge, not L2 review.",
            "Intentional hot drops (FOOD / DONT_SHOW) excluded from OOS conflict tags.",
            "Conflict tags are advisory for review; they do not themselves rewrite "
            "tier. Tier correction for component deps is promote_component_closure.",
            "pending_t0b_with_hot_full_evidence flags ACTIVE/hot rows parked only "
            "by non-native originalMod (SiliconDioxide class).",
        ],
    }

    OUT_SELECTED.write_text(
        json.dumps({"schema_version": 1, "records": dict(sorted(selected.items()))},
                   indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    OUT_DROPPED.write_text(
        json.dumps({"schema_version": 1, "records": dict(sorted(dropped.items()))},
                   indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    OUT_NEEDS_REVIEW.write_text(
        json.dumps(
            {"schema_version": 1, "records": dict(sorted(needs_review.items()))},
            indent=2,
            ensure_ascii=False,
        ) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    OUT_PENDING_T0B.write_text(
        json.dumps({"schema_version": 1, "records": dict(sorted(pending_t0b.items()))},
                   indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    OUT_DEGRADED_PATHS.write_text(
        json.dumps(
            {"schema_version": 1, "records": degraded_paths},
            indent=2,
            ensure_ascii=False,
        ) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    OUT_BYPRODUCT_STRIPPING.write_text(
        json.dumps(
            {"schema_version": 1, "records": byproduct_stripping},
            indent=2,
            ensure_ascii=False,
        ) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    OUT_REPORT.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    print(f"Wrote {OUT_SELECTED}")
    print(f"Wrote {OUT_DROPPED}")
    print(f"Wrote {OUT_NEEDS_REVIEW}")
    print(f"Wrote {OUT_PENDING_T0B}")
    print(f"Wrote {OUT_DEGRADED_PATHS}")
    print(f"Wrote {OUT_BYPRODUCT_STRIPPING}")
    print(f"Wrote {OUT_REPORT}")
    print("closure gate", closure_gate)
    print("layers", {k: layer_counts[k] for k in LAYERS})
    print(
        "CORE", layer_counts["CORE"],
        "needs_review", len(needs_review),
        "pending_t0b", len(pending_t0b),
        "conflicts", materials_with_any_conflict,
    )
    print(
        "rule basis",
        {basis: basis_counts[basis] for basis in ("verified", "inferred", "external")},
    )
    print("inferred priority by rule", dict(inferred_rule_counts.most_common()))
    inferred_without_falsifier = {
        rule: count
        for rule, count in inferred_rule_counts.most_common()
        if not RULE_DEFINITIONS[rule]["falsifier"]
    }
    print(
        "inferred falsifier debt",
        f"rules={len(inferred_without_falsifier)}",
        f"materials={sum(inferred_without_falsifier.values())}",
        inferred_without_falsifier,
    )
    print("structural_class x layer:")
    for sc, layers in sorted(structural_layer.items()):
        print(f"  {sc}: {dict(layers)}")
    print(
        "histogram recipe_appearances "
        f"(FULL {recipe_map_inventory['index_map_count']} maps; "
        f"{recipe_map_inventory['named_map_count_scanned']} named + "
        f"{recipe_map_inventory['empty_unnamed_map_count']} empty unnamed):"
    )
    for key in RECIPE_BUCKETS:
        print(f"  {key:>5} -> {appearance_hist[key]}")
    print("histogram reviewed_appearances (evidence_maps only):")
    for key in RECIPE_BUCKETS:
        print(f"  {key:>5} -> {reviewed_hist[key]}")
    print("histogram registered_prefix_count:")
    for key in ("0", "1", "2-4", "5+"):
        print(f"  {key:>5} -> {prefix_fine_hist[key]}")
    print("single-prefix name histogram (prefix_count==1):")
    for name, n in single_prefix_names.most_common():
        print(f"  {name}: {n}")
    print_cross_table(cross)
    print(f"hot OUT (recipe_appearances>=50 & NEVER/COMPAT_DEAD): {len(hot_out)}")
    print("hot OUT sample (top 10 by appearances):")
    for row in hot_out_sample:
        print(
            f"  {row['source_name']}: full={row['recipe_appearances']} "
            f"reviewed={row['reviewed_appearances']} rule={row['rule']} "
            f"reason={row['activation_reason'][:60]}"
        )
    print(f"conflict materials (any tag): {materials_with_any_conflict}")
    print("conflict types:")
    for tag, n in conflict_counts.most_common():
        print(f"  {tag}: {n}")
    return 0 if closure_gate["ok"] and partition_ok else 2


if __name__ == "__main__":
    raise SystemExit(main())
