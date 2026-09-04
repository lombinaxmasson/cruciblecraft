#!/usr/bin/env python3
"""Replay R0 smelter_proven_mte_recovery as compact exact singletons."""
from __future__ import annotations

from collections import Counter
from typing import Any

from tools import closeout_seal
from tools import census_common as census
from tools import owner_partition_common as owner
from tools import bath_identities as identities
from tools.recipe_bulk import ordinary_source as src
from tools.recipe_bulk.ordinary_source import WorkFamily

SLUG = "smelter/deferred-recycling"
HOST = "cruciblecraft:smelter"
R0_SLUG = "recycling/deferred-ordinary-ledger-r0"
IDENTITY_SLUG = "recycling/smelter-mte-identity"
R0_DIR = census.TOOLS / "waves" / "recycling" / "deferred-ordinary-ledger-r0"
IDENTITY_DIR = census.TOOLS / "waves" / "recycling" / "smelter-mte-identity"
PROVEN_COUNT = 1817
EDGE_KEYS = ("gt.recipe.smelter#1829", "gt.recipe.smelter#1884")
COHORT = "smelter_proven_mte_recovery"
OWNER = "recycling/smelter_mte_recovery"


def publication_group_for_material(material: str) -> str:
    return f"cruciblecraft:smelter/deferred_recycling/{material}"


def _require_predecessors() -> None:
    errors = closeout_seal.check_wave_seal(R0_SLUG)
    errors.extend(closeout_seal.check_wave_seal(IDENTITY_SLUG))
    r0 = census.load_json(R0_DIR / "readiness.json")
    if r0.get("status") != "RECYCLING_DEFERRED_LEDGER_R0_READY":
        errors.append("R0 is not RECYCLING_DEFERRED_LEDGER_R0_READY")
    identity = census.load_json(IDENTITY_DIR / "readiness.json")
    if identity.get("status") != "SMELTER_MTE_IDENTITY_READY":
        errors.append("identity child is not SMELTER_MTE_IDENTITY_READY")
    candidate = census.load_json(R0_DIR / "identity_candidate.json")
    if not candidate.get("unique_meta_equals_proven_family_count"):
        errors.append("R0 unique meta proof failed")
    if int(candidate.get("proven_family_count") or 0) != PROVEN_COUNT:
        errors.append("R0 proven family count drifted from 1817")
    if errors:
        raise ValueError("; ".join(errors))


def proven_families() -> list[dict[str, Any]]:
    universe = census.load_json(R0_DIR / "deferred_universe.json")
    rows = [
        row
        for row in universe.get("families") or []
        if str(row.get("cohort") or "") == COHORT
    ]
    if len(rows) != PROVEN_COUNT:
        raise ValueError(
            f"R0 proven cohort {len(rows)} != {PROVEN_COUNT}"
        )
    return rows


def replay() -> dict[str, Any]:
    _require_predecessors()
    families = proven_families()
    recipes = owner.load_map_recipes(src.source_map_for(HOST))
    maps = {
        "stone": src.load_stone_runtime(),
        "mte": src.load_mte_runtime(),
        "block": src.load_block_runtime(),
        "items": src.load_item_overlay(),
        "aliases": identities.load_reused_aliases(),
        "fluids": src.load_fluid_overlay(),
    }
    catalogs = src.load_catalogs()
    work: list[WorkFamily] = []
    relations: list[dict[str, Any]] = []
    materials: list[str] = []
    for family in families:
        template_key = str(family["template_key"])
        if template_key in EDGE_KEYS:
            raise ValueError(f"{template_key} leaked into proven 1817")
        outputs = list(family.get("output_materials") or [])
        if len(outputs) != 1 or not outputs[0]:
            raise ValueError(f"{template_key}: proven family must have one material")
        material = str(outputs[0])
        keys = [key for key in family.get("relation_keys") or [] if key]
        if len(keys) != 1:
            raise ValueError(f"{template_key}: expected one dump relation key")
        recipe_index = int(str(keys[0]).rsplit("#", 1)[-1])
        if recipe_index < 0 or recipe_index >= len(recipes):
            raise ValueError(f"{template_key}: dump index {recipe_index} out of range")
        item = WorkFamily(
            family_id=str(family["family_id"]),
            template_key=template_key,
            record={"expanded_count": 1, "template_key": template_key},
            owner=OWNER,
            relation_count=1,
        )
        work.append(item)
        materials.append(material)
        relation, errors = src.compile_relation(
            work=item,
            recipe=recipes[recipe_index],
            recipe_index=recipe_index,
            shadow_order=0,
            catalogs=catalogs,
            maps=maps,
            host=HOST,
            slug=SLUG,
        )
        if relation.get("parameterized"):
            raise ValueError(f"{template_key}: parameterized source must not compile")
        relation["publication_group"] = publication_group_for_material(material)
        relation["cohort"] = material
        relation["owner"] = OWNER
        if src.relation_blocked(relation) or errors:
            raise ValueError(
                f"{template_key} blocked after identity overlay: "
                + ",".join(errors[:8] or ["unmapped operand"])
            )
        fluids = list(relation.get("fluid_outputs") or [])
        if len(fluids) != 1:
            raise ValueError(f"{template_key}: expected one molten output")
        runtime = str(fluids[0].get("runtime_id") or "")
        expected_fluid = f"cruciblecraft:molten_{material}"
        if runtime != expected_fluid:
            raise ValueError(
                f"{template_key}: molten runtime {runtime} != {expected_fluid}"
            )
        consume = list(relation.get("item_inputs") or [])
        if len(consume) != 1:
            raise ValueError(f"{template_key}: expected one consumed MTE item")
        runtime_item = str(consume[0].get("runtime_id") or "")
        if not runtime_item.startswith("cruciblecraft:"):
            raise ValueError(f"{template_key}: consume is not a CC identity")
        relations.append(relation)
    required = src.collect_required_forms(relations)
    family_ids = [item.family_id for item in work]
    template_keys = [item.template_key for item in work]
    if len(set(template_keys)) != PROVEN_COUNT:
        raise ValueError("proven template keys are not unique")
    classifications = [
        {
            "disposition": "complete",
            "expanded_count": 1,
            "family_id": item.family_id,
            "future_owner": None,
            "owner": OWNER,
            "reason": "proven_consumed_mte_material_recovery",
            "relation_hashes": [str(relation["source_row_sha256"])],
            "template_key": item.template_key,
        }
        for item, relation in zip(work, relations, strict=True)
    ]
    mapping_counts: Counter[str] = Counter()
    for relation in relations:
        for operand in src._operands(relation):
            mapping_counts[str(operand.get("mapping") or "unknown")] += 1
    dump_path = src.dump_path_for(HOST)
    return {
        "assigned": len(relations),
        "blockers": [],
        "classifications": classifications,
        "dump_path": census.relative(dump_path),
        "dump_sha256": census.sha256_file(dump_path),
        "expected": {
            "family_count": PROVEN_COUNT,
            "family_ids": family_ids,
            "host": HOST,
            "owners": {OWNER: PROVEN_COUNT},
            "relation_count": PROVEN_COUNT,
            "template_keys": template_keys,
        },
        "family_count": PROVEN_COUNT,
        "host": HOST,
        "mapping_counts": dict(sorted(mapping_counts.items())),
        "materials": sorted(set(materials)),
        "production_families": family_ids,
        "production_relations": relations,
        "production_work": work,
        "reclassified": [],
        "relations": relations,
        "required_forms": {
            material: sorted(forms) for material, forms in sorted(required.items())
        },
        "unmapped_fluids": {},
        "unmapped_items": {},
        "wave_slug": SLUG,
        "work": work,
    }
