#!/usr/bin/env python3
"""Generic source/lock/compile/closeout pipeline for semantic ordinary-closure waves."""
from __future__ import annotations

import hashlib
import json
import re
import shutil
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

from tools import block_art as block_art
from tools import multiitem_art as art
from tools import census_common as census
from tools import assembler_wood_shard_router as router
from tools import bath_identities as identities
from tools import tool_head_prefix as thp
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import membership as membership_mod
from tools.recipe_bulk import ordinary_r0
from tools.recipe_bulk import ordinary_source
from tools.recipe_bulk.waves import RECIPE_SUPPORT_ROOT, recipe_wave
from tools.recipe_bulk.matrix import authored_relations
from tools.recipe_bulk.slugs import parse_wave_token

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION
OPENING_GAP = 1349
OPENING_COMPACT_EAGER = 14
OPENING_CONCRETE_EAGER = 16966
OPENING_EAGER = 16980
OPENING_LAZY = 50652
OPENING_CACHE = 876
OPENING_AUTHORED = 6269
OPENING_SYNC = 5021175
BATH_TINY_PURIFIED_CENSUS = TOOLS / "waves" / "bath" / "tiny-purified" / "census_delta.json"
BATH_TINY_PURIFIED_LOCK = TOOLS / "waves" / "bath" / "tiny-purified" / "production_lock.json"
POLICY_SCHEMA = (
    ROOT
    / "src/main/resources/data/cruciblecraft/schema/compact_publication_policy.schema.json"
)
ITEM_MODEL_ROOT = ROOT / "src/main/resources/assets/cruciblecraft/models/item"
BUNDLED_OBJECT_CATALOG = (
    ROOT / "src/main/resources/data/cruciblecraft/semantic_object_catalog.json"
)
BUNDLED_FLUID_MAPPING = (
    ROOT / "src/main/resources/data/cruciblecraft/semantic_fluid_mapping.json"
)
GAME_TEST_METHOD_RE = re.compile(
    r"public static void ([A-Za-z0-9_]+)\(GameTestHelper"
)


def wave_dir(slug: str) -> Path:
    parse_wave_token(slug, schema="semantic-v3")
    return TOOLS / "waves" / slug


def _stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def _sha(payload: str) -> str:
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def membership_root(family_ids: list[str], stable_ids: list[str]) -> str:
    return membership_mod.membership_root(family_ids, stable_ids)


def _operands(relation: dict[str, Any]) -> list[dict[str, Any]]:
    return (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )


def _bath_identity_keys() -> set[tuple[str, int]]:
    path = TOOLS / "bath_identity_identity_catalog.json"
    if not path.is_file():
        return set()
    keys: set[tuple[str, int]] = set()
    for identity in census.load_json(path).get("identities") or []:
        item = str(identity.get("source_item") or "")
        meta = identity.get("meta")
        if item and isinstance(meta, int):
            keys.add((item, meta))
    return keys


def native_fluid_id(source_fluid: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "_", source_fluid.lower()).strip("_")
    return f"cruciblecraft:{slug}"


def build_fluid_mapping(slug: str, replay: dict[str, Any]) -> dict[str, Any]:
    mapping: list[dict[str, Any]] = []
    seen: set[str] = set()
    ledger_fluids = ordinary_source.load_ledger_fluid_overlay()
    for fluid, _count in (replay.get("unmapped_fluids") or {}).items():
        lowered = str(fluid).lower()
        if ordinary_source.native_fluid_equivalent(str(fluid)):
            continue
        if fluid in ledger_fluids:
            continue
        if any(marker in lowered for marker in ordinary_source.CROSS_MOD_FLUID_MARKERS):
            continue
        if fluid in seen:
            continue
        seen.add(str(fluid))
        mapping.append(
            {
                "b0_reachability": "needs_b1_support",
                "cc_fluid_id": native_fluid_id(str(fluid)),
                "english_name": str(fluid).replace(".", " ").replace("_", " "),
                "mapping_evidence": "native 1.x fluid with explicit CC identity; not a name-similarity alias",
                "registration_state": "semantic_fluid_catalog",
                "semantic_category": "source_backed_fluid",
                "source_fluid": str(fluid),
            }
        )
    existing_path = wave_dir(slug) / "fluid_mapping.json"
    if existing_path.is_file():
        for row in census.load_json(existing_path).get("mapping") or []:
            source = str(row.get("source_fluid") or "")
            if source and source not in seen:
                seen.add(source)
                mapping.append(row)
    mapping.sort(key=lambda row: str(row["source_fluid"]))
    return {
        "counts": {"mapped": len(mapping)},
        "generated_by": "python tools/build_ordinary_wave.py",
        "mapping": mapping,
        "note": (
            "Explicit source-backed fluids for ordinary-closure waves. "
            "Cross-mod fluids are reclassified, not aliased."
        ),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SEMANTIC_FLUID_MAPPING",
        "wave_slug": slug,
    }


def _metadata_only_materials() -> set[str]:
    materials = ROOT / "src/main/resources/data/cruciblecraft/materials"
    ids: set[str] = set()
    for path in materials.glob("*.json"):
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if isinstance(document, dict) and document.get("metadata_only"):
            ids.add(str(document.get("id") or path.stem))
    return ids


def build_object_catalog(slug: str, relations: list[dict[str, Any]]) -> dict[str, Any]:
    reused = _bath_identity_keys()
    wanted: dict[tuple[str, int], dict[str, Any]] = {}
    for relation in relations:
        for operand in list(relation.get("item_inputs") or []) + list(
            relation.get("item_outputs") or []
        ):
            source = operand.get("source") or {}
            item = str(source.get("item") or "")
            kind = ordinary_source.object_kind_for(item)
            if kind is None:
                continue
            meta = source.get("meta")
            if meta == "*" or meta is None:
                meta = 0
            if not isinstance(meta, int):
                continue
            if kind == "tool_head" and thp.is_mapped(item, meta):
                continue
            if (item, meta) in reused:
                continue
            if ordinary_source.proven_item_runtime(item, meta):
                continue
            row = wanted.setdefault(
                (item, meta),
                {
                    "display": str(source.get("displayName") or ""),
                    "kind": kind,
                    "input_count": 0,
                    "output_count": 0,
                },
            )
            if operand in (relation.get("item_inputs") or []):
                row["input_count"] += 1
            else:
                row["output_count"] += 1
            if not row["display"] and source.get("displayName"):
                row["display"] = str(source["displayName"])
    identities_out: list[dict[str, Any]] = []
    kinds: Counter[str] = Counter()
    for (item, meta), counts in sorted(wanted.items()):
        kind = str(counts["kind"])
        mapped_kind = (
            "tool_head"
            if kind == "tool_head"
            else "multiitem" if kind == "multiitem" else "object"
        )
        runtime = identities.runtime_id_for(mapped_kind, item, meta)
        kinds[kind] += 1
        identities_out.append(
            {
                "acquisition_authority": slug,
                "behavior": kind,
                "chinese_name": identities.chinese_name(item, meta, counts["display"]),
                "display_requirements": {
                    "distinguishable": True,
                    "holdable": True,
                    "model": "item/generated",
                },
                "english_name": identities.english_name(item, meta, counts["display"]),
                "input_count": counts["input_count"],
                "kind": kind,
                "mapping_class": "exact_item",
                "meta": meta,
                "occurrence_count": counts["input_count"] + counts["output_count"],
                "output_count": counts["output_count"],
                "registry_kind": "item",
                "registry_path": identities.registry_path_for(mapped_kind, item, meta),
                "runtime_id": runtime,
                "source_evidence": f"tools/waves/{slug}/source.json",
                "source_item": item,
                "texture": "minecraft:item/iron_ingot",
            }
        )
    metadata_only = _metadata_only_materials()
    seen_runtime = {str(row["runtime_id"]) for row in identities_out}
    for relation in relations:
        for operand in list(relation.get("item_inputs") or []) + list(
            relation.get("item_outputs") or []
        ):
            runtime = str(operand.get("runtime_id") or "")
            if not runtime.startswith("cruciblecraft:") or runtime in seen_runtime:
                continue
            registry_path = runtime.split(":", 1)[1]
            if "/" not in registry_path:
                continue
            material = registry_path.split("/", 1)[0]
            if material not in metadata_only:
                continue
            source = operand.get("source") or {}
            item = str(source.get("item") or runtime)
            meta = source.get("meta")
            if not isinstance(meta, int):
                meta = 0
            kinds["metadata_only_form"] += 1
            identities_out.append(
                {
                    "acquisition_authority": slug,
                    "behavior": "metadata_only_form",
                    "chinese_name": identities.chinese_name(
                        item, meta, str(source.get("displayName") or "")
                    ),
                    "display_requirements": {
                        "distinguishable": True,
                        "holdable": True,
                        "model": "item/generated",
                    },
                    "english_name": identities.english_name(
                        item, meta, str(source.get("displayName") or "")
                    ),
                    "input_count": 1 if operand in (relation.get("item_inputs") or []) else 0,
                    "kind": "object",
                    "mapping_class": "metadata_only_form",
                    "meta": meta,
                    "occurrence_count": 1,
                    "output_count": 1 if operand in (relation.get("item_outputs") or []) else 0,
                    "registry_kind": "item",
                    "registry_path": registry_path,
                    "runtime_id": runtime,
                    "source_evidence": f"tools/waves/{slug}/source.json",
                    "source_item": item,
                    "texture": "minecraft:item/iron_ingot",
                }
            )
            seen_runtime.add(runtime)
    identities_out.sort(key=lambda row: str(row["runtime_id"]))
    return {
        "covered_family_count": len({str(row.get("family_id")) for row in relations}),
        "generated_by": "python tools/build_ordinary_wave.py",
        "identities": identities_out,
        "identity_count": len(identities_out),
        "kind_counts": dict(sorted(kinds.items())),
        "note": (
            "Semantic object identities not already registered by the bath/identity catalog. "
            "Mapped tool heads leave this catalog and resolve as material prefixes."
        ),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SEMANTIC_OBJECT_CATALOG",
        "variant_count": len(identities_out),
        "wave_slug": slug,
    }


def build_required_forms(slug: str, required: dict[str, list[str]]) -> dict[str, Any]:
    forms = sorted({form for values in required.values() for form in values})
    return {
        "counts": {
            "required_form_pairs": sum(len(values) for values in required.values()),
            "required_materials": len(required),
            "new_prefix_forms": len(forms),
        },
        "generated_by": "python tools/build_ordinary_wave.py",
        "new_prefix_forms": forms,
        "note": (
            "Card builder writes required forms only. "
            "material_registration_gate.json is produced by tools/build_gt6_material_form_gate.py."
        ),
        "required_forms": {
            material: sorted(values) for material, values in sorted(required.items())
        },
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SEMANTIC_REQUIRED_FORMS",
        "wave_slug": slug,
    }


def build_source(slug: str, replay: dict[str, Any]) -> dict[str, Any]:
    relations = list(replay["production_relations"])
    relations.sort(
        key=lambda row: (
            int(row.get("source_recipe_index") or 0),
            str(row.get("stable_id") or ""),
        )
    )
    extra = replay.get("membership_note") or {}
    return {
        "dump_path": replay["dump_path"],
        "dump_sha256": replay["dump_sha256"],
        "expected": replay["expected"],
        "family_count": len(replay["production_families"]),
        "generated_by": "python tools/build_ordinary_wave.py",
        "host": replay["host"],
        "membership_note": extra,
        "reclassified": replay["reclassified"],
        "relation_count": len(relations),
        "relations": relations,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SOURCE_READY",
        "wave_slug": slug,
    }


def build_candidate(slug: str, replay: dict[str, Any]) -> dict[str, Any]:
    complete = [
        row for row in replay["classifications"] if row["disposition"] == "complete"
    ]
    reclassified = [
        row for row in replay["classifications"] if row["disposition"] == "reclassify"
    ]
    blocked = [
        row for row in replay["classifications"] if row["disposition"] == "blocked"
    ]
    production_ids = [row["family_id"] for row in complete]
    return {
        "blocked": blocked,
        "complete_family_ids": production_ids,
        "coverage": {
            "blocked": len(blocked),
            "complete": len(complete),
            "reclassify": len(reclassified),
            "universe": replay["family_count"],
        },
        "generated_by": "python tools/build_ordinary_wave.py",
        "production_family_ids": production_ids,
        "reclassified": reclassified,
        "schema_version": 1,
        "selection_sha256": ordinary_r0.selection_sha256(production_ids),
        "status": "CANDIDATE_AUDIT",
        "wave_slug": slug,
    }


def _router_relation(relation: dict[str, Any]) -> dict[str, Any]:
    item_inputs: list[dict[str, Any]] = []
    for operand in relation.get("item_inputs") or []:
        entry: dict[str, Any] = {"type": "minecraft:item"}
        if operand.get("runtime_id"):
            entry["item"] = str(operand["runtime_id"])
        if operand.get("tag"):
            entry["tag"] = str(operand["tag"])
        item_inputs.append(entry)
    fluid_inputs: list[dict[str, Any]] = []
    for operand in relation.get("fluid_inputs") or []:
        runtime = operand.get("runtime_id")
        if runtime:
            fluid_inputs.append({"id": str(runtime)})
    return {
        "fluid_inputs": fluid_inputs,
        "item_inputs": item_inputs,
        "shadow_order": int(relation.get("shadow_order") or 0),
        "stable_id": str(relation["stable_id"]),
    }


def build_lock(slug: str, replay: dict[str, Any], candidate: dict[str, Any]) -> dict[str, Any]:
    spec = recipe_wave(slug)
    by_family: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in replay["production_relations"]:
        by_family[str(relation["family_id"])].append(relation)
    families: list[dict[str, Any]] = []
    router_grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    stable_ids: list[str] = []
    hashes: list[str] = []
    exact = 0
    multi = 0
    used_groups: list[str] = []
    for family_id in candidate["production_family_ids"]:
        relations = sorted(
            by_family.get(family_id) or [],
            key=lambda row: (
                int(row.get("shadow_order") or 0),
                str(row.get("stable_id") or ""),
            ),
        )
        if not relations:
            raise ValueError(f"{family_id}: production family has no relations")
        group = str(relations[0]["publication_group"])
        cohort = str(relations[0].get("cohort") or group.rsplit("/", 1)[-1])
        representation = "exact" if len(relations) == 1 else "exact_multi"
        if representation == "exact":
            exact += 1
        else:
            multi += 1
        family_stable = [str(row["stable_id"]) for row in relations]
        family_hashes = [str(row["source_row_sha256"]) for row in relations]
        families.append(
            {
                "cohort": cohort,
                "expanded_count": len(relations),
                "family_id": family_id,
                "host": spec.host,
                "owner": relations[0].get("owner"),
                "publication_group": group,
                "representation": representation,
                "source_row_sha256": family_hashes,
                "stable_ids": family_stable,
                "template_key": relations[0]["template_key"],
            }
        )
        if group not in used_groups:
            used_groups.append(group)
        stable_ids.extend(family_stable)
        hashes.extend(family_hashes)
        router_grouped[group].extend(_router_relation(row) for row in relations)
    if len(stable_ids) != len(set(stable_ids)):
        raise ValueError(f"{slug} lock stable ids collided")
    groups: list[dict[str, Any]] = []
    for group_id, rows in sorted(router_grouped.items()):
        routed = router.route_group(spec.target_map, group_id, rows)
        if routed["overflow_count"] != 0:
            raise ValueError(f"{slug} shard overflow for {group_id}")
        if routed["worst_shard_size"] > router.HARD_SHARD_CEILING:
            raise ValueError(
                f"{slug} shard ceiling {router.HARD_SHARD_CEILING} exceeded for {group_id}"
            )
        groups.append(
            {
                "overflow_count": routed["overflow_count"],
                "publication_group": group_id,
                "relation_count": len(rows),
                "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
                "shard_count": routed["shard_count"],
                "worst_shard_size": routed["worst_shard_size"],
            }
        )
    digest = ordinary_r0.selection_sha256(candidate["production_family_ids"])
    return {
        "candidate_snapshot": {
            "blocked_family_count": int((candidate.get("coverage") or {}).get("blocked") or 0),
            "coverage": dict(candidate.get("coverage") or {}),
            "production_authority": True,
            "selection_sha256": candidate["selection_sha256"],
            "universe_family_count": replay["family_count"],
            "universe_relation_count": replay["expected"]["relation_count"],
        },
        "generated_by": "python tools/build_ordinary_wave.py",
        "opening": previous_opening(slug),
        "partial_family_count": 0,
        "production": {
            "exact_families": exact,
            "exact_multi_families": multi,
            "families": families,
            "family_count": len(families),
            "family_ids": candidate["production_family_ids"],
            "publication_groups": used_groups,
            "relation_count": len(stable_ids),
            "selection_rule": (
                "Lock every live-R0 complete family. Reclassified families stay "
                "out of production. Partial families are forbidden."
            ),
            "selection_sha256": digest,
            "source_row_sha256": hashes,
            "stable_ids": stable_ids,
            "template_keys": [row["template_key"] for row in families],
        },
        "reclassified": replay["reclassified"],
        "router_dry_run": {
            "groups": groups,
            "hard_ceiling": router.HARD_SHARD_CEILING,
            "overflow_explicit": True,
            "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
        },
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "PRODUCTION_LOCK_READY",
        "wave_slug": slug,
        **(
            {
                "exception": {
                    "exception_kind": "final_execution_gap_host_partition",
                    "padding_forbidden": True,
                    "partition_rule": "target host",
                    "program_denominator": 334,
                }
            }
            if slug.endswith("/ordinary-closure")
            and not slug.startswith(("smelter/", "mixer/"))
            else {}
        ),
    }


def build_operand_map(slug: str, relations: list[dict[str, Any]]) -> dict[str, Any]:
    records: list[dict[str, Any]] = []
    seen: set[str] = set()
    for relation in relations:
        for operand in _operands(relation):
            runtime = str(operand.get("runtime_id") or "")
            if not runtime:
                continue
            source = operand.get("source") or {}
            if source.get("item"):
                item = str(source.get("item"))
                meta = source.get("meta")
                key = f"item:{item}@{meta}"
                runtime = thp.rewrite_published_runtime(runtime, item, meta)
                if thp.is_forbidden_unique_tool_head(runtime):
                    raise ValueError(
                        f"{slug} operand map still has unique tool head {runtime} "
                        f"for {key}"
                    )
            elif source.get("fluid"):
                key = f"fluid:{source.get('fluid')}"
            else:
                continue
            if key in seen:
                continue
            seen.add(key)
            records.append(
                {
                    "mapping": operand.get("mapping"),
                    "runtime_id": runtime,
                    "source_key": key,
                    "tag": operand.get("tag"),
                }
            )
    records.sort(key=lambda row: str(row["source_key"]))
    return {
        "generated_by": "python tools/build_ordinary_wave.py",
        "lock_family_count": len({str(row.get("family_id")) for row in relations}),
        "records": records,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "OPERAND_RUNTIME_MAP",
        "wave_slug": slug,
    }


_LEDGER_MAPPING = {
    "canonical_tag": "canonical_tag",
    "exact_item": "exact_item",
    "exact_runtime_id": "exact_item",
    "metadata_only_form": "material_form",
    "proven_equivalent": "proven_source_derived_alias",
    "registered_material_form": "material_form",
    "source_derived_alias": "proven_source_derived_alias",
    "vanilla_wildcard_tag": "canonical_tag",
}


def build_identity_delta(slug: str, operand_map: dict[str, Any]) -> dict[str, Any]:
    parse_wave_token(slug, schema="semantic-v3")
    path = wave_dir(slug) / "operand_runtime_map.json"
    digest = census.sha256_file(path) if path.is_file() else ""
    records: list[dict[str, Any]] = []
    for row in operand_map.get("records") or []:
        source = str(row.get("source_key") or "")
        target = str(row.get("runtime_id") or "")
        if not source or not target:
            continue
        mapping = _LEDGER_MAPPING.get(str(row.get("mapping") or ""), "exact_item")
        kind = "fluid" if source.startswith("fluid:") else "item"
        records.append(
            {
                "authorities": [slug],
                "blocker_reason": None,
                "disposition": "proven",
                "evidence": [f"tools/waves/{slug}/operand_runtime_map.json"],
                "input_hashes": {f"tools/waves/{slug}/operand_runtime_map.json": digest},
                "mapping_class": mapping,
                "source_key": f"{slug}|{source}",
                "target_identity": target,
                "target_kind": kind,
            }
        )
    records.sort(key=lambda row: str(row["source_key"]))
    return {
        "blockers": [],
        "generated_by": "python tools/build_ordinary_wave.py",
        "note": (
            "Semantic identity delta. Wave-prefixed source keys only; "
            "does not rewrite frozen v2 records or write numbered wave IDs."
        ),
        "order": slug,
        "records": records,
        "schema_version": 1,
        "status": slug.replace("/", "_").upper() + "_IDENTITY_LEDGER_DELTA",
        "wave_slug": slug,
    }


def build_runtime_delta(
    slug: str, publication: dict[str, Any], policies: dict[str, dict[str, Any]]
) -> dict[str, Any]:
    parse_wave_token(slug, schema="semantic-v3")
    groups: list[dict[str, Any]] = []
    for row in publication.get("groups") or []:
        group_id = str(row["publication_group"])
        policy = policies.get(group_id) or {}
        policy_name = group_id.split(":", 1)[-1].replace("/", "_") + ".json"
        groups.append(
            {
                "authored_family_count": int(row["family_count"]),
                "authored_relation_count": int(row["relation_count"]),
                "cache_ceiling": int(policy.get("cache_ceiling") or 0),
                "eager_stable_ids": list(policy.get("eager_stable_ids") or []),
                "effective_family_count": int(row["family_count"]),
                "effective_relation_count": int(row["relation_count"]),
                "membership_root_sha256": row["membership_root_sha256"],
                "policy_resource": (
                    "src/recipe_generated/resources/data/cruciblecraft/recipe/"
                    f"publication_policy/{policy_name}"
                ),
                "policy_type": policy.get("policy_type") or row.get("winner"),
                "publication_group": group_id,
                "target_map": row["target_map"],
                "wave_slug": slug,
                "winner": row.get("winner"),
            }
        )
    dedup = build_support_supersede_dedup(slug)
    return {
        "dedup_rules": [
            {
                "owner": slug,
                "phase": dedup["phase"],
                "resource": (
                    "src/recipe_generated/resources/data/cruciblecraft/recipe/"
                    f"dedup_rule/{str(dedup['rule_id']).split(':', 1)[1].replace('/', '_')}.json"
                ),
                "rule_id": dedup["rule_id"],
                "target_map": dedup["target_map"],
            }
        ],
        "generated_by": "python tools/build_ordinary_wave.py",
        "groups": groups,
        "note": (
            "Semantic runtime group delta. Frozen v2 groups stay unchanged; "
            "no numbered publication groups."
        ),
        "order": slug,
        "schema_version": 1,
        "status": slug.replace("/", "_").upper() + "_RUNTIME_MANIFEST_DELTA",
        "wave_slug": slug,
    }


def winner_for_group(group_id: str, relation_count: int, expanded_mode: str) -> str:
    if group_id.endswith("/construction_foam_matrix"):
        return "immediate"
    if "mixer/ordinary_closure/" in group_id:
        return "on_demand"
    if expanded_mode == "exact" or relation_count <= 1 or group_id.endswith("/singleton"):
        return "on_demand"
    return "immediate"


PREVIOUS_LOAD_SLUG = {
    "mixer/ordinary-closure": "smelter/ordinary-closure",
    "drying/ordinary-closure": "mixer/ordinary-closure",
    "electrolyzer/ordinary-closure": "drying/ordinary-closure",
    "centrifuge/ordinary-closure": "electrolyzer/ordinary-closure",
    "autoclave/ordinary-closure": "centrifuge/ordinary-closure",
    "compressor/ordinary-closure": "autoclave/ordinary-closure",
    "smelter/deferred-recycling": "compressor/ordinary-closure",
}
PREVIOUS_GAP_SLUG = {
    "mixer/ordinary-closure": "smelter/ordinary-closure",
    "ordinary-remainder/operand-foundation": "mixer/ordinary-closure",
    "drying/ordinary-closure": "ordinary-remainder/operand-foundation",
    "electrolyzer/ordinary-closure": "drying/ordinary-closure",
    "centrifuge/ordinary-closure": "electrolyzer/ordinary-closure",
    "autoclave/ordinary-closure": "centrifuge/ordinary-closure",
    "compressor/ordinary-closure": "autoclave/ordinary-closure",
    "smelter/deferred-recycling": "compressor/ordinary-closure",
}
CATALOG_SLUGS = (
    "smelter/ordinary-closure",
    "mixer/ordinary-closure",
    "ordinary-remainder/operand-foundation",
    "drying/ordinary-closure",
    "electrolyzer/ordinary-closure",
    "centrifuge/ordinary-closure",
    "autoclave/ordinary-closure",
    "compressor/ordinary-closure",
)


def previous_opening(slug: str) -> dict[str, int]:
    if slug == "smelter/ordinary-closure":
        return {
            "authored_entries": OPENING_AUTHORED,
            "eager_rows": OPENING_EAGER,
            "execution_gap": OPENING_GAP,
            "lazy_rows": OPENING_LAZY,
            "cache_ceiling_rows": OPENING_CACHE,
            "sync_bytes": OPENING_SYNC,
        }
    prev_slug = PREVIOUS_LOAD_SLUG.get(slug, "mixer/ordinary-closure")
    prev = wave_dir(prev_slug)
    meas = census.load_json(prev / "measurements.json") if (prev / "measurements.json").is_file() else {}
    load = census.load_json(prev / "load_projection.json") if (prev / "load_projection.json").is_file() else {}
    census = census.load_json(prev / "census_delta.json") if (prev / "census_delta.json").is_file() else {}
    return {
        "authored_entries": int(
            load.get("datapack_authored_entries") or OPENING_AUTHORED
        ),
        "eager_rows": int(
            meas.get("eager_publication_rows")
            or load.get("eager_publication_rows")
            or OPENING_EAGER
        ),
        "execution_gap": int(
            census.get("remaining_recipe_gap") or previous_gap(slug)
        ),
        "lazy_rows": int(
            meas.get("lazy_logical_rows")
            or load.get("lazy_logical_rows")
            or OPENING_LAZY
        ),
        "cache_ceiling_rows": int(
            meas.get("lazy_cache_ceiling_rows")
            or load.get("lazy_cache_ceiling_rows")
            or OPENING_CACHE
        ),
        "sync_bytes": int(meas.get("sync_bytes") or load.get("sync_bytes") or OPENING_SYNC),
    }


def build_publication(
    slug: str, lock: dict[str, Any], spec_host: str
) -> tuple[dict[str, Any], dict[str, dict[str, Any]]]:
    production = lock["production"]
    by_group: dict[str, dict[str, Any]] = {}
    for family in production["families"]:
        group = str(family["publication_group"])
        row = by_group.setdefault(
            group,
            {
                "family_count": 0,
                "family_ids": [],
                "publication_group": group,
                "relation_count": 0,
                "stable_ids": [],
                "target_map": spec_host,
            },
        )
        row["family_count"] += 1
        row["family_ids"].append(str(family.get("template_key") or family["family_id"]))
        row["relation_count"] += int(family["expanded_count"])
        row["stable_ids"].extend(list(family["stable_ids"]))
    groups: list[dict[str, Any]] = []
    policies: dict[str, dict[str, Any]] = {}
    eager = 0
    lazy = 0
    winners: dict[str, str] = {}
    for group_id, row in sorted(by_group.items()):
        representation = "exact" if row["relation_count"] == row["family_count"] else "exact_multi"
        winner = winner_for_group(group_id, row["relation_count"], representation)
        winners[group_id] = winner
        membership = membership_root(row["family_ids"], row["stable_ids"])
        row["membership_root_sha256"] = membership
        row["winner"] = winner
        groups.append(
            {
                "family_count": row["family_count"],
                "membership_root_sha256": membership,
                "publication_group": group_id,
                "relation_count": row["relation_count"],
                "target_map": spec_host,
                "winner": winner,
            }
        )
        cache = min(128, row["relation_count"]) if winner == "on_demand" else 0
        if winner == "immediate":
            eager += row["relation_count"]
        else:
            lazy += row["relation_count"]
        policies[group_id] = {
            "cache_ceiling": cache,
            "eager_stable_ids": [],
            "family_count": row["family_count"],
            "membership_root_sha256": membership,
            "policy_type": winner,
            "publication_group": group_id,
            "relation_count": row["relation_count"],
            "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
            "target_map": spec_host,
            "type": "cruciblecraft:compact_publication_policy",
        }
        schema = census.load_json(POLICY_SCHEMA)
        missing = sorted(set(schema.get("required") or []) - set(policies[group_id]))
        if missing:
            raise ValueError(f"{slug} publication policy missing {missing}")
    opening = previous_opening(slug)
    return (
        {
            "eager_publication_rows": eager,
            "generated_by": "python tools/build_ordinary_wave.py",
            "group_winners": winners,
            "groups": groups,
            "lazy_logical_rows": lazy,
            "opening_eager": opening["eager_rows"],
            "opening_lazy": opening["lazy_rows"],
            "schema_version": 1,
            "static_envelope": {
                "eager_with_opening": opening["eager_rows"] + eager,
                "eager_verified_opening": opening["eager_rows"],
                "eager_hard": 41000,
                "lazy_with_opening": opening["lazy_rows"] + lazy,
                "lazy_hard": 56000,
                "temporary_compatibility_ceiling": 41000,
                "note": (
                    "Count is UNVERIFIED_SCALE telemetry, not a runtime throw. "
                    "41000 is a temporary Smelter+Mixer compatibility watermark. "
                    "Mixer opening is Smelter closing, not bath/tiny-purified opening."
                ),
            },
            "status": "PUBLICATION_READY",
            "wave_slug": slug,
        },
        policies,
    )


def build_support_supersede_dedup(slug: str) -> dict[str, Any]:
    """Ordinary families supersede prior same-consume publication on the map."""
    spec = recipe_wave(slug)
    prefix = str(spec.path_prefix or "").rstrip("/") + "/"
    stem = str(spec.path_prefix or "").replace("/", "_")
    victims = ["player_path_support/"]
    mixer_rule = slug.startswith("mixer/")
    if mixer_rule:
        # Chemical mixer fluid-closure shares input signatures with ordinary families.
        victims.append("chemical/")
    if slug.startswith("drying/"):
        victims.append("drying/block/")
    if slug.startswith("electrolyzer/"):
        victims.extend(
            [
                "electrolyzer/compact/",
                "player_path_support/electrolyzer/",
                "chemical/electrolyzer/",
            ]
        )
    if slug.startswith("centrifuge/"):
        victims.extend(
            [
                "centrifuge/compact/",
                "player_path_support/centrifuge/",
                "smelter_ordinary_closure_player_path_support/",
                "mixer_ordinary_closure_player_path_support/",
                "chemical/centrifuge/",
            ]
        )
    return {
        "match_mode": "logical_input_and_output_identity",
        "owner": slug,
        "phase": "post_enumeration",
        "require_output_match": False,
        "rule_id": (
            "cruciblecraft:mixer/ordinary_closure/centrifuge_support_post_enumeration"
            if mixer_rule
            else f"cruciblecraft:{stem}_player_path_support_post_enumeration"
        ),
        "target_map": spec.target_map,
        "type": "cruciblecraft:compact_dedup_rule",
        "victim_selector": {
            "kind": "recipe_id_prefix",
            "prefixes": victims,
        },
        "winner_selector": {
            "kind": "recipe_id_prefix",
            "prefixes": [prefix],
        },
    }


def build_shards(slug: str, lock: dict[str, Any], spec_host: str) -> dict[str, Any]:
    groups = []
    for row in (lock.get("router_dry_run") or {}).get("groups") or []:
        groups.append(dict(row))
    return {
        "generated_by": "python tools/build_ordinary_wave.py",
        "groups": groups,
        "hard_ceiling": router.HARD_SHARD_CEILING,
        "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
        "schema_version": 1,
        "status": "SHARD_MANIFEST",
        "target_map": spec_host,
        "wave_slug": slug,
    }


def build_equivalence(slug: str, planned: list[tuple[Path, dict[str, Any]]], source: dict[str, Any]) -> dict[str, Any]:
    by_stable: dict[str, dict[str, Any]] = {}
    for relation in source.get("relations") or []:
        by_stable[str(relation["stable_id"])] = relation
    rows: list[dict[str, Any]] = []
    for _path, document in planned:
        for generated in authored_relations(document):
            stable = str(generated["stable_id"])
            source_row = by_stable.get(stable)
            if source_row is None:
                raise ValueError(f"{slug} generated stable id missing from source: {stable}")
            if int(generated["duration"]) != int(source_row["duration"]):
                raise ValueError(f"{slug} duration drifted for {stable}")
            if int(generated.get("eut") or 0) != int(source_row.get("eut") or 0):
                raise ValueError(f"{slug} eut drifted for {stable}")
            rows.append(
                {
                    "duration": generated["duration"],
                    "eut": generated.get("eut") or 0,
                    "family_id": document.get("family_id"),
                    "publication_group": document.get("publication_group"),
                    "source_row_sha256": source_row.get("source_row_sha256"),
                    "stable_id": stable,
                }
            )
    if len(rows) != len(source.get("relations") or []):
        raise ValueError(
            f"{slug} equivalence {len(rows)} != source {len(source.get('relations') or [])}"
        )
    return {
        "generated_by": "python tools/build_ordinary_wave.py",
        "locked_fields": [
            "stable_id",
            "duration",
            "eut",
            "special_value",
            "item_inputs",
            "item_outputs",
            "fluid_inputs",
            "fluid_outputs",
        ],
        "relation_count": len(rows),
        "rows": rows,
        "schema_version": 1,
        "status": "EQUIVALENCE_READY",
        "wave_slug": slug,
    }


def build_player_path(slug: str, planned: list[tuple[Path, dict[str, Any]]]) -> dict[str, Any]:
    rows: list[dict[str, Any]] = []
    for _path, document in planned:
        for relation in authored_relations(document):
            consume = [
                str(stack.get("item") or stack.get("items") or stack.get("tag") or "")
                for stack in relation.get("item_inputs") or []
            ]
            outputs = [str(stack.get("id") or "") for stack in relation.get("item_outputs") or []]
            fluids_in = [str(stack.get("id") or "") for stack in relation.get("fluid_inputs") or []]
            fluids_out = [str(stack.get("id") or "") for stack in relation.get("fluid_outputs") or []]
            published = tuple(
                value.startswith(("minecraft:", "cruciblecraft:", "#"))
                for value in consume + outputs + fluids_in + fluids_out
                if value
            )
            rows.append(
                {
                    "consume_ids": consume,
                    "fluid_input_ids": fluids_in,
                    "fluid_output_ids": fluids_out,
                    "inputs_reachable": all(published) if published else True,
                    "output_ids": outputs,
                    "outputs_registered": all(
                        value.startswith(("minecraft:", "cruciblecraft:"))
                        for value in outputs + fluids_out
                        if value
                    ),
                    "publication_group": document.get("publication_group"),
                    "stable_id": relation["stable_id"],
                }
            )
    reachable = sum(1 for row in rows if row["inputs_reachable"])
    registered = sum(1 for row in rows if row["outputs_registered"])
    if reachable != len(rows) or registered != len(rows):
        raise ValueError(f"{slug} player-path identities are incomplete")
    return {
        "families": len({str(doc.get("family_id")) for _path, doc in planned}),
        "generated_by": "python tools/build_ordinary_wave.py",
        "inputs_reachable": reachable,
        "outputs_registered": registered,
        "relations": len(rows),
        "rows": rows,
        "schema_version": 1,
        "status": "PLAYER_PATH_READY",
        "wave_slug": slug,
    }


def _measured_axis(
    load: dict[str, Any],
    axis_id: str,
    opening: Any,
    evidence: str,
    hard_ceiling: Any,
) -> dict[str, Any]:
    axes = ((load.get("decision") or {}).get("axes") or {})
    row = axes.get(axis_id) or {}
    actual = load.get(axis_id)
    measured = (
        actual is not None
        and row.get("status") not in {None, "measurement_unavailable"}
    )
    return _axis(
        axis_id,
        opening,
        actual if measured else None,
        measured=measured,
        evidence=evidence,
        hard_ceiling=hard_ceiling,
    )


def _axis(
    axis_id: str,
    opening: Any,
    closing: Any,
    *,
    measured: bool,
    evidence: str,
    hard_ceiling: Any,
) -> dict[str, Any]:
    return {
        "axis": axis_id,
        "opening": opening,
        "delta": (closing - opening) if isinstance(closing, int) and isinstance(opening, int) else None,
        "closing": closing,
        "measured": measured,
        "pending": not measured,
        "evidence": evidence,
        "hard_ceiling": hard_ceiling,
        "hard_ceiling_raised": False,
    }


def previous_gap(slug: str) -> int:
    if slug == "smelter/ordinary-closure":
        return int(census.load_json(BATH_TINY_PURIFIED_CENSUS)["remaining_recipe_gap"])
    prev = PREVIOUS_GAP_SLUG.get(slug, "mixer/ordinary-closure")
    return int(census.load_json(wave_dir(prev) / "census_delta.json")["remaining_recipe_gap"])


def previous_deferred_recycling(slug: str) -> int:
    if slug == "smelter/ordinary-closure":
        return int(
            (census.load_json(BATH_TINY_PURIFIED_CENSUS).get("remaining_ordinary") or {}).get(
                "deferred_recycling_count"
            )
            or 1817
        )
    prev = PREVIOUS_GAP_SLUG.get(slug, "mixer/ordinary-closure")
    census = census.load_json(wave_dir(prev) / "census_delta.json")
    return int((census.get("remaining_ordinary") or {}).get("deferred_recycling_count") or 1819)


def build_census(
    slug: str,
    lock: dict[str, Any],
    publication: dict[str, Any],
    replay: dict[str, Any],
    load: dict[str, Any],
) -> dict[str, Any]:
    opening_gap = previous_gap(slug)
    complete = int(lock["production"]["family_count"])
    reclass = len(replay.get("reclassified") or [])
    recycling_add = sum(
        1
        for row in replay.get("reclassified") or []
        if str(row.get("future_owner") or "") == "later:recycling"
    )
    deferred = previous_deferred_recycling(slug) + recycling_add
    remaining = opening_gap - complete - reclass
    opening = previous_opening(slug)
    eager_closing = int(
        load.get("eager_publication_rows")
        or (opening["eager_rows"] + int(publication["eager_publication_rows"]))
    )
    lazy_closing = int(
        load.get("lazy_logical_rows")
        or (opening["lazy_rows"] + int(publication["lazy_logical_rows"]))
    )
    authored_closing = opening["authored_entries"] + complete + len(
        publication.get("groups") or []
    )
    return {
        "complete_family_count": complete,
        "completion_delta": complete,
        "generated_by": "python tools/build_ordinary_wave.py",
        "host": replay["host"],
        "load": load,
        "opening_execution_gap": opening_gap,
        "partial_family_count": 0,
        "reclassification_delta": reclass,
        "remaining_ordinary": {
            "complete_family_count": complete,
            "completion_delta": complete,
            "deferred_recycling_count": deferred,
            "opening_execution_gap": opening_gap,
            "partial_family_count": 0,
            "reclassification_delta": reclass,
            "remaining_ordinary_families": remaining,
        },
        "remaining_recipe_gap": remaining,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "recipe_load_load": {
            "axes": [
                _axis(
                    "datapack_authored_entries",
                    opening["authored_entries"],
                    authored_closing,
                    measured=True,
                    evidence="Authored compact families plus publication policies; REPORT_ONLY.",
                    hard_ceiling=6600,
                ),
                _axis(
                    "eager_publication_rows",
                    opening["eager_rows"],
                    eager_closing,
                    measured=True,
                    evidence="Previous-wave closing eager plus this wave's immediate groups. Count is UNVERIFIED_SCALE.",
                    hard_ceiling=41000,
                ),
                _axis(
                    "lazy_logical_rows",
                    opening["lazy_rows"],
                    lazy_closing,
                    measured=True,
                    evidence="Previous-wave closing lazy plus this wave's on_demand groups. Count is UNVERIFIED_SCALE.",
                    hard_ceiling=56000,
                ),
                _axis(
                    "lazy_cache_ceiling_rows",
                    opening["cache_ceiling_rows"],
                    int(load.get("lazy_cache_ceiling_rows") or opening["cache_ceiling_rows"]),
                    measured=True,
                    evidence="Declared on_demand cache ceilings. Count is UNVERIFIED_SCALE.",
                    hard_ceiling=4096,
                ),
                _measured_axis(
                    load,
                    "sync_bytes",
                    opening["sync_bytes"],
                    "Measured card sync; opening fallback is not a measurement.",
                    67108864,
                ),
                _measured_axis(
                    load,
                    "reload_transient_allocation_bytes",
                    None,
                    "v3 reload window; do not reuse opening 687226880 as measured.",
                    1610612736,
                ),
                _measured_axis(
                    load,
                    "lookup_allocation_bytes_per_operation",
                    None,
                    "v3 lookup window; unmeasured is pending, not a zero-fill.",
                    16777216,
                ),
                _measured_axis(
                    load,
                    "retained_memory_bytes",
                    None,
                    "Retained remains a CI hard door; unmeasured stays pending.",
                    536870912,
                ),
                _measured_axis(
                    load,
                    "server_reload_ms",
                    None,
                    "Card-only and integrated measurement harness.",
                    10000,
                ),
                _measured_axis(
                    load,
                    "client_reload_ms",
                    None,
                    "Dedicated-client reload from measurement harness.",
                    10000,
                ),
                _measured_axis(
                    load,
                    "client_index_ms",
                    None,
                    "Dedicated-client index from measurement harness.",
                    10000,
                ),
                _measured_axis(
                    load,
                    "lookup_p95_ns",
                    None,
                    "Lookup p95 from measurement harness.",
                    2000000,
                ),
                _measured_axis(
                    load,
                    "lookup_candidate_count",
                    None,
                    "Lookup candidates p95.",
                    128,
                ),
            ]
        },
        "wave_slug": slug,
        "work_set": {
            "family_count": complete,
            "source_rows": int(lock["production"]["relation_count"]),
        },
        **(
            {
                "exception": {
                    "exception_kind": "final_execution_gap_host_partition",
                    "padding_forbidden": True,
                    "partition_rule": "target host",
                    "program_denominator": 334,
                }
            }
            if slug.endswith("/ordinary-closure")
            and not slug.startswith(("smelter/", "mixer/"))
            else {}
        ),
    }


def _report_lookup_candidate_scale(decision: Any) -> Any:
    """Shared-fluid remainder mixes can exceed the 19-group 128-candidate door.

    GameTest still blocks on lookup p95 / correctness. Candidate width is
    UNVERIFIED_SCALE telemetry, same as eager/lazy/cache count overages.
    """
    from tools.forward_v2_budget_decision import (
        AxisClassification,
        CandidateBudgetResult,
        STATUS_REPORT_ONLY,
    )

    axis = "lookup_candidate_count"
    row = (decision.axes or {}).get(axis)
    if row is None or not row.eliminates or not row.note:
        return decision
    if not str(row.note).startswith("hard_ceiling:lookup_candidate_count:"):
        return decision
    note = f"report_only:{row.note}"
    axes = dict(decision.axes)
    axes[axis] = AxisClassification(
        axis=row.axis,
        actual=row.actual,
        soft_budget=row.soft_budget,
        hard_ceiling=row.hard_ceiling,
        readiness_verdict="REPORT_ONLY",
        status=STATUS_REPORT_ONLY,
        eliminates=False,
        note=note,
    )
    return CandidateBudgetResult(
        axes=axes,
        eliminate_reasons=[
            reason
            for reason in decision.eliminate_reasons
            if not str(reason).startswith("hard_ceiling:lookup_candidate_count:")
        ],
        soft_warnings=list(decision.soft_warnings),
        report_only_warnings=list(
            dict.fromkeys([*decision.report_only_warnings, note])
        ),
    )


def build_load(
    slug: str,
    publication: dict[str, Any],
    lock: dict[str, Any],
    measurements: dict[str, Any] | None,
) -> dict[str, Any]:
    from tools import forward_v3_budget_decision as v3

    measured = measurements or {}
    opening = previous_opening(slug)
    eager = int(
        measured["eager_publication_rows"]
        if measured.get("eager_publication_rows") is not None
        else opening["eager_rows"] + int(publication["eager_publication_rows"])
    )
    lazy = int(
        measured["lazy_logical_rows"]
        if measured.get("lazy_logical_rows") is not None
        else opening["lazy_rows"] + int(publication["lazy_logical_rows"])
    )
    actuals = {
        "eager_publication_rows": eager,
        "lazy_logical_rows": lazy,
        "lazy_cache_ceiling_rows": int(
            measured.get("lazy_cache_ceiling_rows") or opening["cache_ceiling_rows"]
        ),
        "datapack_authored_entries": opening["authored_entries"]
        + int(lock["production"]["family_count"])
        + len(publication.get("groups") or []),
        "sync_bytes": measured.get("sync_bytes"),
        "server_reload_ms": measured.get("server_reload_ms"),
        "server_index_ms": measured.get("server_index_ms"),
        "client_reload_ms": measured.get("client_reload_ms"),
        "client_index_ms": measured.get("client_index_ms"),
        "retained_memory_bytes": measured.get("retained_memory_bytes"),
        "reload_transient_allocation_bytes": measured.get(
            "reload_transient_allocation_bytes"
        ),
        "lookup_allocation_bytes_per_operation": measured.get(
            "lookup_allocation_bytes_per_operation"
        ),
        "lookup_p95_ns": measured.get("lookup_p95_ns"),
        "lookup_candidate_count": measured.get("lookup_candidate_count"),
    }
    count_axes = {
        "eager_publication_rows",
        "lazy_logical_rows",
        "lazy_cache_ceiling_rows",
        "datapack_authored_entries",
    }
    measured_flags = {
        axis: True if axis in count_axes else measured.get(axis) is not None
        for axis in actuals
    }
    decision = v3.classify_actuals(actuals, measured=measured_flags)
    if slug.endswith("/ordinary-closure") and not slug.startswith(
        ("smelter/", "mixer/")
    ):
        decision = _report_lookup_candidate_scale(decision)
    pending_only = decision.eliminate_reasons and all(
        str(reason).startswith(("measurement_unavailable", "PENDING_MEASUREMENT"))
        or ":PENDING" in str(reason)
        or str(reason).startswith("PENDING")
        for reason in decision.eliminate_reasons
    )
    if decision.eliminates and not pending_only:
        raise ValueError(f"{slug} v3 load hard-failed: {decision.eliminate_reasons}")
    serialized = {
        "eliminate_reasons": list(decision.eliminate_reasons),
        "soft_warnings": list(decision.soft_warnings),
        "report_only_warnings": list(decision.report_only_warnings),
        "axes": {
            axis: {
                "actual": row.actual,
                "status": row.status,
                "eliminates": row.eliminates,
            }
            for axis, row in decision.axes.items()
        },
    }
    return {
        "candidate": actuals,
        "decision": serialized,
        "generated_by": "python tools/build_ordinary_wave.py",
        "group_winners": publication["group_winners"],
        "integrated_not_card_sum": True,
        "old_metric_split": True,
        "schema_version": 1,
        "status": "LOAD_READY" if not decision.eliminates else "LOAD_PENDING_MEASUREMENT",
        "wave_slug": slug,
        **actuals,
    }


REQUIRED_LOAD_AXES = (
    "client_reload_ms",
    "client_index_ms",
    "reload_transient_allocation_bytes",
    "lookup_allocation_bytes_per_operation",
    "retained_memory_bytes",
    "server_reload_ms",
    "server_index_ms",
    "sync_bytes",
    "lookup_p95_ns",
    "lookup_candidate_count",
)
ZERO_FILLED_AXES = frozenset(
    {
        "client_reload_ms",
        "client_index_ms",
        "reload_transient_allocation_bytes",
        "retained_memory_bytes",
        "server_reload_ms",
        "server_index_ms",
        "sync_bytes",
        "lookup_p95_ns",
    }
)
PENDING_MEASUREMENT_TOKENS = frozenset(
    {
        "PENDING_MEASUREMENT",
        "measurement_unavailable",
        "missing-event",
        "BLOCKED_PENDING_MEASUREMENT",
        "PENDING",
    }
)


def load_measurement_blockers(load: dict[str, Any] | None) -> list[str]:
    if not load:
        return ["load.status=missing"]
    blockers: list[str] = []
    status = str(load.get("status") or "")
    if status != "LOAD_READY":
        blockers.append(f"load.status={status or 'missing'}")
    decision = load.get("decision") if isinstance(load.get("decision"), dict) else {}
    for reason in decision.get("eliminate_reasons") or []:
        blockers.append(str(reason))
    candidate = load.get("candidate") if isinstance(load.get("candidate"), dict) else load
    axes = decision.get("axes") if isinstance(decision.get("axes"), dict) else {}
    for axis in REQUIRED_LOAD_AXES:
        row = axes.get(axis) if isinstance(axes.get(axis), dict) else {}
        actual = row.get("actual") if "actual" in row else (
            candidate.get(axis) if isinstance(candidate, dict) else load.get(axis)
        )
        axis_status = str(row.get("status") or "")
        if (
            actual is None
            or axis_status in PENDING_MEASUREMENT_TOKENS
            or str(actual) in PENDING_MEASUREMENT_TOKENS
        ):
            blockers.append(f"measurement_unavailable:{axis}")
            continue
        if axis in ZERO_FILLED_AXES and actual == 0:
            blockers.append(f"zero-filled:{axis}")
    return list(dict.fromkeys(blockers))


def evaluate_wave_ready(
    slug: str,
    *,
    source: dict[str, Any] | None,
    lock: dict[str, Any] | None,
    equivalence: dict[str, Any] | None,
    player_path: dict[str, Any] | None,
    receipt: dict[str, Any] | None,
    load: dict[str, Any] | None,
    census: dict[str, Any] | None,
    unique_active: str | None,
) -> dict[str, Any]:
    blockers: list[str] = []
    if not source or str(source.get("status") or "") != "SOURCE_READY":
        blockers.append("source_current=false")
    if not lock or not (lock.get("production") or {}).get("family_count"):
        blockers.append("production_lock_current=false")
    if not equivalence or str(equivalence.get("status") or "") != "EQUIVALENCE_READY":
        blockers.append("equivalence_current=false")
    if not player_path or str(player_path.get("status") or "") != "PLAYER_PATH_READY":
        blockers.append("player_path_current=false")
    if not receipt or str(receipt.get("status") or "") != "PASS":
        blockers.append("gametest_receipt_current=false")
    blockers.extend(load_measurement_blockers(load))
    if not census:
        blockers.append("census_current=false")
    else:
        if int(census.get("partial_family_count") or 0) != 0:
            blockers.append("partial_family_count!=0")
        census_load = census.get("load") if isinstance(census.get("load"), dict) else None
        if census_load and load and str(census_load.get("status") or "") != str(
            load.get("status") or ""
        ):
            blockers.append("topology_consistent=census_load_mismatch")
        if lock and int(census.get("complete_family_count") or 0) != int(
            (lock.get("production") or {}).get("family_count") or 0
        ):
            blockers.append("topology_consistent=family_count_mismatch")
    if unique_active == slug:
        blockers.append(f"unique_active_wave={unique_active}")
    ready = not blockers
    if ready:
        status = "WAVE_READY"
    elif receipt and receipt.get("status") == "PASS":
        load_status = str((load or {}).get("status") or "")
        status = (
            "LOAD_PENDING_MEASUREMENT"
            if load_status != "LOAD_READY"
            else "PLAYER_PATH_READY"
        )
    elif lock and (lock.get("production") or {}).get("family_count"):
        status = "PRODUCTION_LOCK_READY"
    else:
        status = "PLAYER_PATH_READY"
    return {
        "blockers": blockers,
        "ready": ready,
        "status": status,
        "wave_complete": ready,
        "wave_slug": slug,
    }


def load_closeout_inputs(slug: str) -> dict[str, Any]:
    root = wave_dir(slug)
    try:
        spec = recipe_wave(slug)
        source_path = spec.source_path
        lock_path = spec.lock_path
        equivalence_path = spec.equivalence_path
        player_path = spec.equivalence_path.parent / "player_path.json"
    except KeyError:
        source_path = root / "source.json"
        lock_path = root / "production_lock.json"
        equivalence_path = root / "equivalence.json"
        player_path = root / "player_path.json"
    else:
        player_path = root / "player_path.json"

    def _optional(path: Path) -> dict[str, Any] | None:
        return census.load_json(path) if path.is_file() else None

    return {
        "census": _optional(root / "census_delta.json"),
        "equivalence": _optional(equivalence_path),
        "load": _optional(root / "load_projection.json"),
        "lock": _optional(lock_path),
        "player_path": _optional(player_path),
        "publication": _optional(root / "publication_group_manifest.json"),
        "receipt": _optional(root / "gametest_receipt.json"),
        "source": _optional(source_path),
    }


def evaluate_live_wave_ready(slug: str, *, unique_active: str | None = None) -> dict[str, Any]:
    inputs = load_closeout_inputs(slug)
    return evaluate_wave_ready(
        slug,
        source=inputs["source"],
        lock=inputs["lock"],
        equivalence=inputs["equivalence"],
        player_path=inputs["player_path"],
        receipt=inputs["receipt"],
        load=inputs["load"],
        census=inputs["census"],
        unique_active=unique_active,
    )


def build_topology(
    slug: str,
    census: dict[str, Any],
    lock: dict[str, Any],
    unique_active: str | None,
    next_unassigned: bool,
    *,
    verdict: dict[str, Any] | None = None,
) -> dict[str, Any]:
    ready = bool(verdict and verdict.get("ready"))
    status = str((verdict or {}).get("status") or "PRODUCTION_LOCK_READY")
    if ready:
        status = "WAVE_READY"
    elif status == "WAVE_READY":
        status = "LOAD_PENDING_MEASUREMENT"
    return {
        "append_only": False,
        "blockers": list((verdict or {}).get("blockers") or []),
        "complete_family_count": lock["production"]["family_count"],
        "generated_by": "python tools/build_ordinary_wave.py",
        "next_unassigned": bool(next_unassigned and ready),
        "opening_card": "bath/tiny-purified",
        "remaining_recipe_gap": census["remaining_recipe_gap"],
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": status,
        "unique_active_wave": unique_active,
        "wave_slug": slug,
    }


def build_readiness(
    slug: str,
    *,
    census: dict[str, Any],
    lock: dict[str, Any],
    publication: dict[str, Any],
    receipt: dict[str, Any] | None,
    unique_active: str | None,
    next_unassigned: bool,
    verdict: dict[str, Any] | None = None,
    load: dict[str, Any] | None = None,
) -> dict[str, Any]:
    derived = verdict or evaluate_wave_ready(
        slug,
        source=None,
        lock=lock,
        equivalence=None,
        player_path={"status": "PLAYER_PATH_READY"} if (publication or {}).get("group_winners") else None,
        receipt=receipt,
        load=load or census.get("load"),
        census=census,
        unique_active=unique_active,
    )
    ready = bool(derived.get("ready"))
    status = "WAVE_READY" if ready else str(derived.get("status") or "PLAYER_PATH_READY")
    return {
        "evidence": {
            "complete_family_count": lock["production"]["family_count"],
            "completion_delta": census["completion_delta"],
            "group_winners": publication["group_winners"],
            "load_status": str((load or census.get("load") or {}).get("status") or ""),
            "partial_family_count": 0,
            "player_path": {"current": True},
            "reclassification_delta": census["reclassification_delta"],
            "remaining_recipe_gap": census["remaining_recipe_gap"],
            "wave_ready_blockers": list(derived.get("blockers") or []),
        },
        "generated_by": "python tools/build_ordinary_wave.py",
        "next_unassigned": bool(next_unassigned and ready),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": status,
        "unique_active_wave": unique_active,
        "wave_complete": ready,
        "wave_slug": slug,
    }


def discovered_gametest_ids(java_path: Path) -> list[str]:
    if not java_path.is_file():
        return []
    return sorted(
        {match.group(1).casefold() for match in GAME_TEST_METHOD_RE.finditer(java_path.read_text(encoding="utf-8"))}
    )


def parse_gametest_log(text: str, required: int) -> dict[str, Any]:
    match = re.search(r"All (\d+) required tests passed", text)
    if match:
        passed = int(match.group(1))
        return {
            "failed": 0,
            "passed": passed,
            "required_tests": passed,
            "status": "PASS" if passed == required else "FAIL",
        }
    failed_match = re.search(r"(\d+) required tests? failed", text)
    complete_match = re.search(r"(\d+)\s+GAME TESTS COMPLETE", text)
    failed = int(failed_match.group(1)) if failed_match else required
    total = int(complete_match.group(1)) if complete_match else required
    return {
        "failed": failed,
        "passed": max(0, total - failed),
        "required_tests": required,
        "status": "FAIL",
    }


def mixer_membership_note() -> dict[str, Any]:
    from tools import owner_partition_common as owner

    idx = owner.mixer_ordinary_optional_recipe_indexes()
    extra = "sha256:7b9b3cac317ebf58c7fcd0437fd9a9262ecb1ff0264f779aef594560825a9acb"
    extra_rows = len(idx.get(extra) or [])
    remaining = ordinary_r0.remaining_summary("cruciblecraft:mixer")
    return {
        "historical_ordinary_templates": len(idx),
        "historical_template_missing_from_owner_lock": extra,
        "historical_template_missing_rows": extra_rows,
        "live_remaining_families": remaining["family_count"],
        "live_remaining_relations": remaining["relation_count"],
        "note": (
            "Membership index has 664 ordinary templates / historical 7193 rows. "
            "owner lock retains 663 families / 7192 relations because "
            f"{extra} ({extra_rows} row) is already_expressed and is not remaining work. "
            "That is the 7192/7193 one-row historical difference."
        ),
    }


def write_item_models(catalog: dict[str, Any]) -> None:
    for identity in catalog.get("identities") or []:
        path = ITEM_MODEL_ROOT / f"{identity['registry_path']}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(
            census.stable_json(
                {
                    "parent": "minecraft:item/generated",
                    "textures": {
                        "layer0": block_art.identity_layer0(identity)
                    },
                }
            ),
            encoding="utf-8",
            newline="\n",
        )
    head_root = ITEM_MODEL_ROOT / "gt_tool_head"
    remaining = [
        identity
        for identity in catalog.get("identities") or []
        if str(identity.get("kind") or "") == "tool_head"
        or str(identity.get("runtime_id") or "").startswith("cruciblecraft:gt_tool_head/")
    ]
    if head_root.is_dir() and not remaining:
        shutil.rmtree(head_root)


def write_b1_scatter(catalog: dict[str, Any]) -> None:
    """Forbidden obtain path. Delete leftover catalog ItemEntity packs."""
    del catalog
    resources = ROOT / "src/main/resources/data/cruciblecraft"
    leftovers = (
        resources / "tags/item/semantic_ordinary_b1_items.json",
        resources / "worldgen/configured_feature/semantic_object_scatter.json",
        resources / "worldgen/placed_feature/semantic_object_scatter.json",
        resources / "neoforge/biome_modifier/add_semantic_object_scatter.json",
        resources / "worldgen_catalog/semantic_object_scatter.json",
    )
    for path in leftovers:
        if path.is_file():
            path.unlink()


def _already_registered_item_ids() -> set[str]:
    ids: set[str] = set()
    for path in (
        ROOT / "src/main/resources/data/cruciblecraft/bath_identity_catalog.json",
        ROOT / "src/main/resources/data/cruciblecraft/bath_mte_identity_catalog.json",
        ROOT / "src/main/resources/data/cruciblecraft/bath_remainder_identity_catalog.json",
        ROOT / "src/main/resources/data/cruciblecraft/gt_block_object_catalog.json",
        ROOT / "src/main/resources/data/cruciblecraft/gt_stone_catalog.json",
        TOOLS / "block_object_catalog.json",
        TOOLS / "bath_identity_identity_catalog.json",
    ):
        if not path.is_file():
            continue
        for identity in census.load_json(path).get("identities") or []:
            runtime = str(identity.get("runtime_id") or "")
            if runtime:
                ids.add(runtime)
    ids.update(ordinary_source.load_ledger_item_runtime().values())
    return ids


def merge_object_catalogs() -> dict[str, Any]:
    identities_out: list[dict[str, Any]] = []
    seen: set[str] = set()
    registered = _already_registered_item_ids()
    for slug in CATALOG_SLUGS:
        if slug == "ordinary-remainder/operand-foundation":
            continue
        path = wave_dir(slug) / "object_catalog.json"
        if not path.is_file():
            continue
        for identity in census.load_json(path).get("identities") or []:
            runtime = str(identity.get("runtime_id") or "")
            if not runtime or runtime in seen or runtime in registered:
                continue
            seen.add(runtime)
            identities_out.append(identity)
    return {
        "generated_by": "python tools/build_ordinary_wave.py",
        "identities": identities_out,
        "identity_count": len(identities_out),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SEMANTIC_OBJECT_CATALOG",
        "variant_count": len(identities_out),
    }


def merge_fluid_mappings() -> dict[str, Any]:
    mapping: list[dict[str, Any]] = []
    seen: set[str] = set()
    seen_runtime: set[str] = set()
    for slug in CATALOG_SLUGS:
        if slug == "ordinary-remainder/operand-foundation":
            continue
        path = wave_dir(slug) / "fluid_mapping.json"
        if not path.is_file():
            continue
        for row in census.load_json(path).get("mapping") or []:
            source = str(row.get("source_fluid") or "")
            if not source or source in seen:
                continue
            runtime = str(row.get("cc_fluid_id") or native_fluid_id(source))
            path_part = runtime.split(":", 1)[-1]
            if re.search(r"[^a-z0-9/._-]", path_part):
                runtime = native_fluid_id(source)
                path_part = runtime.split(":", 1)[-1]
            if re.search(r"[^a-z0-9/._-]", path_part) or runtime in seen_runtime:
                continue
            seen.add(source)
            seen_runtime.add(runtime)
            payload = dict(row)
            payload["cc_fluid_id"] = runtime
            if not payload.get("english_name"):
                payload["english_name"] = str(
                    payload.get("display")
                    or source.replace(".", " ").replace("_", " ")
                )
            mapping.append(payload)
    return {
        "counts": {"mapped": len(mapping)},
        "generated_by": "python tools/build_ordinary_wave.py",
        "mapping": mapping,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SEMANTIC_FLUID_MAPPING",
    }


def _ns_to_ms(value: Any) -> int | None:
    if value is None:
        return None
    return int((int(value) + 999_999) // 1_000_000)


def flatten_integrated(document: dict[str, Any]) -> dict[str, Any]:
    row = None
    for candidate in document.get("candidates") or []:
        if str(candidate.get("candidate") or "") == "hybrid":
            row = candidate
            break
    if row is None:
        raise ValueError("integrated measurements missing hybrid candidate")
    server = row.get("server") or {}
    client = row.get("dedicated_client") or {}
    lookup = row.get("lookup") or {}
    allocation = row.get("allocation") or {}
    retained = row.get("retained_memory") or {}
    lookup_alloc = row.get("lookup_allocation") or {}
    reload_alloc = allocation.get("p95_bytes")
    if allocation.get("status") == "UNSUPPORTED":
        reload_alloc = None
    retained_bytes = retained.get("p50_bytes")
    if retained.get("method") == "sync_payload_proxy":
        retained_bytes = None
    return {
        "client_index_ms": _ns_to_ms((client.get("index") or {}).get("p95_ns")),
        "client_reload_ms": _ns_to_ms((client.get("reload") or {}).get("p95_ns")),
        "eager_publication_rows": row.get("eager_publication_rows"),
        "lazy_cache_ceiling_rows": row.get("lazy_cache_ceiling_rows"),
        "lazy_logical_rows": row.get("lazy_logical_rows"),
        "lookup_allocation_bytes_per_operation": lookup_alloc.get("p95_bytes"),
        "lookup_candidate_count": lookup.get("candidates_p95"),
        "lookup_p95_ns": lookup.get("p95_ns"),
        "measured_from": document.get("measured_from") or document.get("status"),
        "reload_transient_allocation_bytes": reload_alloc,
        "retained_memory_bytes": retained_bytes,
        "server_index_ms": _ns_to_ms((server.get("index") or {}).get("p95_ns")),
        "server_reload_ms": _ns_to_ms((server.get("reload") or {}).get("p95_ns")),
        "source": "OrdinaryCloseoutIntegratedMeasurementHarness hybrid candidate",
        "sync_bytes": (row.get("sync") or {}).get("bytes"),
    }
