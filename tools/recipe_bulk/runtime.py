#!/usr/bin/python3
"""Aggregate T37–T45 compact publication + dedup runtime authority."""
from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path
from typing import Any

from tools import t35_common as t35
from tools import t37_common as t37
from tools import t38_common as t38
from tools import t39_common as t39
from tools import t40_common as t40
from tools import t41_common as t41
from tools import t43_common as t43
from tools import t45_common as t45
from tools.recipe_bulk.membership import identity_semantic_root, membership_root
from tools.recipe_bulk.waves import SHADOW_ORDER
from tools.recipe_bulk.write_guard import relative

ROOT = t35.ROOT
TOOLS = t35.TOOLS
POLICY_TYPE = "cruciblecraft:compact_publication_policy"
DEDUP_TYPE = "cruciblecraft:compact_dedup_rule"
ROUTING_SCHEMA_VERSION = "compact-shard-v1"
STATUS = "COMPACT_RECIPE_RUNTIME_MANIFEST"
EXPECTED_GROUP_COUNT = 12
T37_EAGER_COUNT = 14
T37_LAZY_COUNT = 36
T41_AUTHORED_RELATIONS = 292
T41_LIVE_RELATIONS = 242
HISTORICAL_GROUPS = {
    "cruciblecraft:assembler": "cruciblecraft:assembler/compact",
    "cruciblecraft:roaster": "cruciblecraft:roaster/compact",
}
FAMILY_LOADERS = {
    "T37": t37.generated_family_files,
    "T38": t38.generated_family_files,
    "T39": t39.generated_family_files,
    "T40": t40.generated_family_files,
    "T41": t41.generated_family_files,
    "T43": t43.generated_family_files,
    "T45": t45.generated_family_files,
}

# Frozen production winners/cache. T37 eager IDs are computed, then pinned.
GROUP_SPECS: tuple[dict[str, Any], ...] = (
    {
        "wave_id": "T37",
        "target_map": "cruciblecraft:assembler",
        "publication_group": "cruciblecraft:assembler/compact",
        "policy_type": "hybrid",
        "cache_ceiling": 8,
        "eager_mode": "t37_hybrid_selector",
        "resource_name": "t37_assembler.json",
        "expected_authored_families": 50,
        "expected_authored_relations": 50,
        "bind_live_membership": False,
    },
    {
        "wave_id": "T38",
        "target_map": "cruciblecraft:roaster",
        "publication_group": "cruciblecraft:roaster/compact",
        "policy_type": "on_demand",
        "cache_ceiling": 16,
        "eager_mode": "empty",
        "resource_name": "t38_roaster.json",
        "expected_authored_families": 29,
        "expected_authored_relations": 73,
        "bind_live_membership": False,
    },
    {
        "wave_id": "T39",
        "target_map": "cruciblecraft:centrifuge",
        "publication_group": "cruciblecraft:centrifuge/singleton",
        "policy_type": "on_demand",
        "cache_ceiling": 19,
        "eager_mode": "empty",
        "resource_name": "t39_centrifuge_singleton.json",
        "expected_authored_families": 19,
        "expected_authored_relations": 19,
        "bind_live_membership": False,
    },
    {
        "wave_id": "T39",
        "target_map": "cruciblecraft:centrifuge",
        "publication_group": "cruciblecraft:centrifuge/multi",
        "policy_type": "hybrid",
        "cache_ceiling": 13,
        "eager_mode": "empty",
        "resource_name": "t39_centrifuge_multi.json",
        "expected_authored_families": 3,
        "expected_authored_relations": 13,
        "bind_live_membership": False,
    },
    {
        "wave_id": "T40",
        "target_map": "cruciblecraft:electrolyzer",
        "publication_group": "cruciblecraft:electrolyzer/singleton",
        "policy_type": "on_demand",
        "cache_ceiling": 11,
        "eager_mode": "empty",
        "resource_name": "t40_electrolyzer_singleton.json",
        "expected_authored_families": 11,
        "expected_authored_relations": 11,
        "bind_live_membership": False,
    },
    {
        "wave_id": "T40",
        "target_map": "cruciblecraft:electrolyzer",
        "publication_group": "cruciblecraft:electrolyzer/multi",
        "policy_type": "hybrid",
        "cache_ceiling": 11,
        "eager_mode": "empty",
        "resource_name": "t40_electrolyzer_multi.json",
        "expected_authored_families": 2,
        "expected_authored_relations": 11,
        "bind_live_membership": False,
    },
    {
        "wave_id": "T41",
        "target_map": "cruciblecraft:assembler",
        "publication_group": "cruciblecraft:assembler/planks",
        "policy_type": "hybrid",
        "cache_ceiling": 16,
        "eager_mode": "empty",
        "resource_name": "t41_assembler_planks.json",
        "expected_authored_families": 85,
        "expected_authored_relations": 85,
        "bind_live_membership": True,
    },
    {
        "wave_id": "T41",
        "target_map": "cruciblecraft:assembler",
        "publication_group": "cruciblecraft:assembler/fireproof",
        "policy_type": "hybrid",
        "cache_ceiling": 16,
        "eager_mode": "empty",
        "resource_name": "t41_assembler_fireproof.json",
        "expected_authored_families": 144,
        "expected_authored_relations": 144,
        "bind_live_membership": True,
    },
    {
        "wave_id": "T41",
        "target_map": "cruciblecraft:assembler",
        "publication_group": "cruciblecraft:assembler/planks2",
        "policy_type": "on_demand",
        "cache_ceiling": 16,
        "eager_mode": "empty",
        "resource_name": "t41_assembler_planks2.json",
        "expected_authored_families": 63,
        "expected_authored_relations": 63,
        "bind_live_membership": True,
    },
    {
        "wave_id": "T43",
        "target_map": "cruciblecraft:smelter",
        "publication_group": "cruciblecraft:smelter/stone",
        "policy_type": "hybrid",
        "cache_ceiling": 24,
        "eager_mode": "t43_hybrid",
        "resource_name": "t43_smelter_stone.json",
        "expected_authored_families": 407,
        "expected_authored_relations": 407,
        "bind_live_membership": False,
    },
    {
        "wave_id": "T45",
        "target_map": "cruciblecraft:smelter",
        "publication_group": "cruciblecraft:smelter/block",
        "policy_type": "hybrid",
        "cache_ceiling": 24,
        "eager_mode": "empty",
        "resource_name": "t45_smelter_block.json",
        "expected_authored_families": 271,
        "expected_authored_relations": 271,
        "bind_live_membership": False,
    },
    {
        "wave_id": "T45",
        "target_map": "cruciblecraft:drying",
        "publication_group": "cruciblecraft:drying/block",
        "policy_type": "on_demand",
        "cache_ceiling": 24,
        "eager_mode": "empty",
        "resource_name": "t45_drying_block.json",
        "expected_authored_families": 108,
        "expected_authored_relations": 108,
        "bind_live_membership": False,
    },
)

CUTOVER_WAVES = ("T37", "T38", "T39", "T40", "T41")


def datapack_policy_path(spec: dict[str, Any]) -> Path:
    wave = str(spec["wave_id"]).lower()
    return (
        ROOT
        / f"src/{wave}_recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
        / spec["resource_name"]
    )


def datapack_dedup_root() -> Path:
    return (
        ROOT
        / "src/compact_recipe_policy_generated/resources/data/cruciblecraft/recipe/dedup_rule"
    )


def t37_policy_resource() -> Path:
    return (
        ROOT
        / "src/t37_recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
        / "t37_assembler.json"
    )


def compact_cutover_runtime_files(file_fn, tree_fn) -> dict[str, Any]:
    java = ROOT / "src/main/java/com/masson/cruciblecraft/recipe/gt"
    schema = ROOT / "src/main/resources/data/cruciblecraft/schema"
    return {
        "dedup_rule_definition": file_fn(java / "CompactDedupRuleDefinition.java"),
        "dedup_rule_schema": file_fn(schema / "compact_dedup_rule.schema.json"),
        "dedup_rules": tree_fn(
            ROOT / "src/compact_recipe_policy_generated/resources"
        ),
        "deduplicator": file_fn(java / "CompactRecipeDeduplicator.java"),
        "publication_policy": file_fn(java / "CompactPublicationPolicy.java"),
        "publication_policy_definition": file_fn(
            java / "CompactPublicationPolicyDefinition.java"
        ),
        "publication_policy_schema": file_fn(
            schema / "compact_publication_policy.schema.json"
        ),
    }


def resolved_publication_group(family: dict[str, Any]) -> str:
    declared = family.get("publication_group")
    if isinstance(declared, str) and declared:
        return declared
    target = str(family.get("target_map") or "")
    group = HISTORICAL_GROUPS.get(target)
    if group is None:
        raise ValueError(
            f"compact family {family.get('family_id')} must declare publication_group"
        )
    return group


def load_wave_families(wave_id: str) -> list[dict[str, Any]]:
    files = FAMILY_LOADERS[wave_id]()
    documents: list[dict[str, Any]] = []
    for path in files:
        document = json.loads(path.read_text(encoding="utf-8"))
        document["_source_path"] = relative(path)
        documents.append(document)
    return documents


def _operand_token(value: Any, default_key: str = "item") -> str:
    if isinstance(value, str):
        return f"{default_key}:{value}"
    if not isinstance(value, dict):
        return json.dumps(value, sort_keys=True)
    if "item" in value:
        return f"item:{value['item']}"
    if "id" in value and "amount" in value:
        return f"fluid:{value['id']}#{value['amount']}"
    if "id" in value:
        return f"item:{value['id']}"
    if "tag" in value:
        return f"tag:{value['tag']}"
    return json.dumps(value, sort_keys=True)


def _action_kind(value: Any) -> str:
    if isinstance(value, dict):
        return str(value.get("kind") or "consume")
    return str(value)


def logical_input_identity(relation: dict[str, Any]) -> str:
    items: list[str] = []
    inputs = list(relation.get("item_inputs") or [])
    counts = list(relation.get("item_input_counts") or [])
    actions = list(relation.get("item_input_actions") or [])
    if len(counts) != len(inputs) or len(actions) != len(inputs):
        raise ValueError("item input counts/actions drifted from item_inputs")
    for index, operand in enumerate(inputs):
        items.append(
            f"{counts[index]}:{_action_kind(actions[index])}@{_operand_token(operand)}"
        )
    items.sort()
    fluids = sorted(
        _operand_token(stack, "fluid") for stack in (relation.get("fluid_inputs") or [])
    )
    return ",".join(items) + "||" + ",".join(fluids)


def recipe_output_identity(relation: dict[str, Any]) -> str:
    items = sorted(
        f"{int(stack.get('count') or 1)}@{_operand_token(stack)}"
        for stack in (relation.get("item_outputs") or [])
    )
    fluids = sorted(
        _operand_token(stack, "fluid") for stack in (relation.get("fluid_outputs") or [])
    )
    return (
        ",".join(items)
        + "||"
        + ",".join(fluids)
        + "|"
        + str(relation.get("duration"))
        + "|"
        + str(relation.get("eut"))
    )


def _family_stable_ids(family: dict[str, Any]) -> list[str]:
    return [str(relation["stable_id"]) for relation in family.get("relations") or []]


def apply_t41_source_dedup(
    t37_families: list[dict[str, Any]],
    t41_families: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    t37_outputs: dict[str, str] = {}
    for family in t37_families:
        for relation in family.get("relations") or []:
            t37_outputs[logical_input_identity(relation)] = recipe_output_identity(
                relation
            )
    kept: list[dict[str, Any]] = []
    for family in t41_families:
        remaining = []
        for relation in family.get("relations") or []:
            expected = t37_outputs.get(logical_input_identity(relation))
            if expected is None or expected != recipe_output_identity(relation):
                remaining.append(relation)
        if not remaining:
            continue
        if len(remaining) == len(family.get("relations") or []):
            kept.append(family)
            continue
        rewritten = dict(family)
        rewritten["relations"] = remaining
        kept.append(rewritten)
    return kept


def t37_eager_stable_ids(families: list[dict[str, Any]]) -> list[str]:
    family_ids = sorted(str(family["family_id"]) for family in families)
    first_ten = set(family_ids[:10])
    eager: set[str] = set()
    for family in families:
        family_id = str(family["family_id"])
        for relation in family.get("relations") or []:
            if int(relation["duration"]) <= 16 or family_id in first_ten:
                eager.add(str(relation["stable_id"]))
    ordered = sorted(eager)
    if len(ordered) != T37_EAGER_COUNT:
        raise ValueError(
            f"T37 hybrid eager count drifted: {len(ordered)} != {T37_EAGER_COUNT}"
        )
    return ordered


def t43_eager_stable_ids() -> list[str]:
    return sorted(str(value) for value in t43.hybrid_eager_stable_ids("stone"))


def grouped_membership(
    families: list[dict[str, Any]],
) -> dict[str, dict[str, Any]]:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for family in families:
        grouped[resolved_publication_group(family)].append(family)
    rows: dict[str, dict[str, Any]] = {}
    for group_id, members in grouped.items():
        family_ids = [str(family["family_id"]) for family in members]
        stable_ids = [
            stable_id
            for family in members
            for stable_id in _family_stable_ids(family)
        ]
        rows[group_id] = {
            "family_count": len(members),
            "family_ids": sorted(family_ids),
            "membership_root_sha256": membership_root(family_ids, stable_ids),
            "publication_group": group_id,
            "relation_count": len(stable_ids),
            "stable_ids": sorted(stable_ids),
        }
    return rows


def _eager_ids(spec: dict[str, Any], families: list[dict[str, Any]]) -> list[str]:
    mode = spec["eager_mode"]
    if mode == "empty":
        return []
    if mode == "t37_hybrid_selector":
        return t37_eager_stable_ids(families)
    if mode == "t43_hybrid":
        return t43_eager_stable_ids()
    raise ValueError(f"unknown eager_mode {mode}")


def policy_document(
    spec: dict[str, Any],
    membership: dict[str, Any],
    eager_stable_ids: list[str],
) -> dict[str, Any]:
    document = {
        "cache_ceiling": spec["cache_ceiling"],
        "eager_stable_ids": eager_stable_ids,
        "family_count": membership["family_count"],
        "membership_root_sha256": membership["membership_root_sha256"],
        "policy_type": spec["policy_type"],
        "publication_group": spec["publication_group"],
        "relation_count": membership["relation_count"],
        "routing_schema_version": ROUTING_SCHEMA_VERSION,
        "target_map": spec["target_map"],
        "type": POLICY_TYPE,
    }
    return document


def dedup_rules() -> list[dict[str, Any]]:
    return [
        {
            "type": DEDUP_TYPE,
            "rule_id": "cruciblecraft:t37_t41_assembler_pre_snapshot",
            "owner": "T37",
            "phase": "pre_snapshot",
            "target_map": "cruciblecraft:assembler",
            "match_mode": "logical_input_and_output_identity",
            "require_output_match": True,
            "winner_selector": {
                "kind": "publication_group",
                "publication_groups": ["cruciblecraft:assembler/compact"],
            },
            "victim_selector": {
                "kind": "publication_group",
                "publication_groups": [
                    "cruciblecraft:assembler/planks",
                    "cruciblecraft:assembler/fireproof",
                    "cruciblecraft:assembler/planks2",
                ],
            },
        },
        {
            "type": DEDUP_TYPE,
            "rule_id": "cruciblecraft:t37_t41_assembler_post_enumeration",
            "owner": "T37",
            "phase": "post_enumeration",
            "target_map": "cruciblecraft:assembler",
            "match_mode": "logical_input_and_output_identity",
            "require_output_match": True,
            "winner_selector": {
                "kind": "recipe_id_prefix",
                "prefixes": ["assembler/compact/"],
            },
            "victim_selector": {
                "kind": "recipe_id_prefix",
                "prefixes": ["assembler/wood/", "player_path_support/assembler_wood/"],
            },
        },
        {
            "type": DEDUP_TYPE,
            "rule_id": "cruciblecraft:t39_t5_centrifuge_post_enumeration",
            "owner": "T39",
            "phase": "post_enumeration",
            "target_map": "cruciblecraft:centrifuge",
            "match_mode": "input_and_output_signature",
            "require_output_match": True,
            "winner_selector": {
                "kind": "recipe_id_prefix",
                "prefixes": ["centrifuge/compact/", "player_path_support/centrifuge/"],
            },
            "victim_selector": {
                "kind": "recipe_id_prefix",
                "prefixes": ["t5/"],
            },
        },
        {
            "type": DEDUP_TYPE,
            "rule_id": "cruciblecraft:t40_t5_electrolyzer_post_enumeration",
            "owner": "T40",
            "phase": "post_enumeration",
            "target_map": "cruciblecraft:electrolyzer",
            "match_mode": "input_and_output_signature",
            "require_output_match": True,
            "winner_selector": {
                "kind": "recipe_id_prefix",
                "prefixes": ["electrolyzer/compact/", "player_path_support/electrolyzer/"],
            },
            "victim_selector": {
                "kind": "recipe_id_prefix",
                "prefixes": ["t5/"],
            },
        },
    ]


def validate_dedup_rules(rules: list[dict[str, Any]]) -> None:
    seen: set[str] = set()
    known_phase = {"pre_snapshot", "post_enumeration"}
    known_mode = {
        "logical_input_and_output_identity",
        "input_and_output_signature",
    }
    known_owner = {"T37", "T38", "T39", "T40", "T41", "T43", "T45"}
    victim_keys: dict[tuple[str, str], set[str]] = {}
    for rule in rules:
        rule_id = str(rule.get("rule_id") or "")
        if not rule_id:
            raise ValueError("compact dedup rule is missing rule_id")
        if rule_id in seen:
            raise ValueError(f"duplicate compact dedup rule_id {rule_id}")
        seen.add(rule_id)
        owner = str(rule.get("owner") or "")
        if owner not in known_owner:
            raise ValueError(f"undeclared compact dedup owner on {rule_id}")
        phase = str(rule.get("phase") or "")
        if phase not in known_phase:
            raise ValueError(f"unknown compact dedup phase on {rule_id}: {phase}")
        mode = str(rule.get("match_mode") or "")
        if mode not in known_mode:
            raise ValueError(f"unknown compact dedup match_mode on {rule_id}: {mode}")
        winner = _selector_values(rule.get("winner_selector") or {})
        victim = _selector_values(rule.get("victim_selector") or {})
        overlap = winner & victim
        if overlap:
            raise ValueError(
                f"compact dedup selector overlap on {rule_id}: {sorted(overlap)}"
            )
        key = (phase, str(rule.get("target_map") or ""))
        previous = victim_keys.setdefault(key, set())
        collided = previous & victim
        if collided:
            raise ValueError(
                f"compact dedup victim selector overlap on {key}: {sorted(collided)}"
            )
        previous.update(victim)


def _selector_values(selector: dict[str, Any]) -> set[str]:
    kind = str(selector.get("kind") or "")
    if kind == "publication_group":
        values = {str(value) for value in selector.get("publication_groups") or []}
    elif kind == "recipe_id_prefix":
        values = {str(value) for value in selector.get("prefixes") or []}
    else:
        raise ValueError(f"unknown compact dedup selector kind {kind}")
    if not values:
        raise ValueError(f"compact dedup selector {kind} is empty")
    return values


def _ledger_bindings() -> dict[str, str]:
    baseline = t35.load_json(TOOLS / "recipe_wave_production_baseline.json")
    shadow = t35.load_json(TOOLS / "recipe_wave_shadow_parity.json")
    identity = t35.load_json(TOOLS / "global_build_identity_ledger.json")
    return {
        "identity_semantic_root_sha256": identity_semantic_root(
            list(identity.get("records") or []) + list(identity.get("blockers") or [])
        ),
        "production_baseline_sha256": t35.sha256_file(
            TOOLS / "recipe_wave_production_baseline.json"
        ),
        "shadow_parity_sha256": t35.sha256_file(
            TOOLS / "recipe_wave_shadow_parity.json"
        ),
        "shadow_status": str(shadow.get("status") or ""),
        "baseline_status": str(baseline.get("status") or ""),
    }


def build_group_rows() -> list[dict[str, Any]]:
    families_by_wave = {wave_id: load_wave_families(wave_id) for wave_id in SHADOW_ORDER}
    live_t41 = apply_t41_source_dedup(
        families_by_wave["T37"],
        families_by_wave["T41"],
    )
    authored_by_wave = {
        wave_id: grouped_membership(documents)
        for wave_id, documents in families_by_wave.items()
    }
    live_by_wave = dict(authored_by_wave)
    live_by_wave["T41"] = grouped_membership(live_t41)
    t41_live_total = sum(
        row["relation_count"] for row in live_by_wave["T41"].values()
    )
    if t41_live_total != T41_LIVE_RELATIONS:
        raise ValueError(
            f"T41 live relation count drifted: {t41_live_total} != {T41_LIVE_RELATIONS}"
        )
    t41_authored_total = sum(
        row["relation_count"] for row in authored_by_wave["T41"].values()
    )
    if t41_authored_total != T41_AUTHORED_RELATIONS:
        raise ValueError(
            f"T41 authored relation count drifted: {t41_authored_total} != {T41_AUTHORED_RELATIONS}"
        )
    rows: list[dict[str, Any]] = []
    seen_groups: set[str] = set()
    for spec in GROUP_SPECS:
        group_id = spec["publication_group"]
        if group_id in seen_groups:
            raise ValueError(f"duplicate publication group {group_id}")
        seen_groups.add(group_id)
        wave_id = spec["wave_id"]
        authored = authored_by_wave[wave_id].get(group_id)
        live = live_by_wave[wave_id].get(group_id)
        if authored is None or live is None:
            raise ValueError(f"missing generated membership for {group_id}")
        if authored["family_count"] != spec["expected_authored_families"]:
            raise ValueError(
                f"{group_id} authored family_count drifted: "
                f"{authored['family_count']} != {spec['expected_authored_families']}"
            )
        if authored["relation_count"] != spec["expected_authored_relations"]:
            raise ValueError(
                f"{group_id} authored relation_count drifted: "
                f"{authored['relation_count']} != {spec['expected_authored_relations']}"
            )
        membership = live if spec["bind_live_membership"] else authored
        families = families_by_wave[wave_id]
        if spec["bind_live_membership"]:
            families = [
                family
                for family in live_t41
                if resolved_publication_group(family) == group_id
            ]
        elif wave_id == "T37":
            families = families_by_wave["T37"]
        eager = _eager_ids(spec, families)
        if spec["eager_mode"] == "t37_hybrid_selector":
            if len(eager) != T37_EAGER_COUNT:
                raise ValueError("T37 eager stable IDs drifted")
            if membership["relation_count"] - len(eager) != T37_LAZY_COUNT:
                raise ValueError("T37 lazy count drifted")
            unknown = [value for value in eager if not value.startswith("cruciblecraft:")]
            if unknown:
                raise ValueError(f"T37 eager id missing prefix: {unknown[0]}")
        policy_path = datapack_policy_path(spec)
        rows.append(
            {
                "authored_family_count": authored["family_count"],
                "authored_relation_count": authored["relation_count"],
                "cache_ceiling": spec["cache_ceiling"],
                "eager_stable_ids": eager,
                "effective_family_count": membership["family_count"],
                "effective_relation_count": membership["relation_count"],
                "membership_root_sha256": membership["membership_root_sha256"],
                "policy_resource": relative(policy_path),
                "policy_type": spec["policy_type"],
                "publication_group": group_id,
                "target_map": spec["target_map"],
                "wave_id": wave_id,
                "winner": spec["policy_type"],
            }
        )
    if len(rows) != EXPECTED_GROUP_COUNT:
        raise ValueError(f"expected {EXPECTED_GROUP_COUNT} groups, got {len(rows)}")
    return rows


def build() -> dict[str, Any]:
    groups = build_group_rows()
    rules = dedup_rules()
    validate_dedup_rules(rules)
    bindings = _ledger_bindings()
    return {
        "bindings": bindings,
        "dedup_rules": [
            {
                "owner": rule["owner"],
                "phase": rule["phase"],
                "resource": relative(
                    datapack_dedup_root() / f"{rule['rule_id'].split(':', 1)[1]}.json"
                ),
                "rule_id": rule["rule_id"],
                "target_map": rule["target_map"],
            }
            for rule in rules
        ],
        "generated_by": "python tools/build_compact_recipe_runtime_manifest.py",
        "group_count": len(groups),
        "groups": groups,
        "note": (
            "Aggregate runtime compatibility authority for T37–T45 publication "
            "groups and the four compact dedup rules. Does not rewrite family JSON. "
            "T41 policy membership binds post-source-dedup live counts."
        ),
        "owns_families": 0,
        "schema_version": 1,
        "status": STATUS,
        "t37_eager_count": T37_EAGER_COUNT,
        "t37_lazy_count": T37_LAZY_COUNT,
        "t41_authored_relation_count": T41_AUTHORED_RELATIONS,
        "t41_live_relation_count": T41_LIVE_RELATIONS,
    }


def cutover_policy_documents() -> dict[str, dict[str, Any]]:
    groups = {row["publication_group"]: row for row in build_group_rows()}
    documents: dict[str, dict[str, Any]] = {}
    for spec in GROUP_SPECS:
        if spec["wave_id"] not in CUTOVER_WAVES:
            continue
        row = groups[spec["publication_group"]]
        documents[spec["publication_group"]] = policy_document(
            spec,
            {
                "family_count": row["effective_family_count"],
                "membership_root_sha256": row["membership_root_sha256"],
                "relation_count": row["effective_relation_count"],
            },
            row["eager_stable_ids"],
        )
    return documents


def all_policy_documents() -> dict[str, dict[str, Any]]:
    groups = {row["publication_group"]: row for row in build_group_rows()}
    documents: dict[str, dict[str, Any]] = {}
    for spec in GROUP_SPECS:
        row = groups[spec["publication_group"]]
        documents[spec["publication_group"]] = policy_document(
            spec,
            {
                "family_count": row["effective_family_count"],
                "membership_root_sha256": row["membership_root_sha256"],
                "relation_count": row["effective_relation_count"],
            },
            row["eager_stable_ids"],
        )
    return documents


def spec_for_group(publication_group: str) -> dict[str, Any]:
    for spec in GROUP_SPECS:
        if spec["publication_group"] == publication_group:
            return spec
    raise KeyError(publication_group)
