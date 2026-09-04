#!/usr/bin/env python3
"""portfolio/exclusion-reclaim-r0: freeze five T13c exclusion categories."""
from __future__ import annotations

import argparse
import json
import re
import sys
from typing import Any

from tools import closeout_seal
from tools import portfolio_one_x as one_x
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir

SLUG = "portfolio/exclusion-reclaim-r0"
PREDECESSOR = "portfolio/logistics-cover-net-r0"
PREDECESSOR_STATUS = "LOGISTICS_COVER_NET_R0_READY"
STATUS = "T13C_EXCLUSION_RECLAIM_R0_READY"
SOURCE_REVISION = census.SOURCE_REVISION
GENERATED_BY = "python tools/build_t13c_exclusion_reclaim_r0.py"
PINNED_CATEGORIES = (
    ("Panels", 6, 348, "outside_fixed_machine_energy_behavior_tree"),
    ("Sensors", 21, 21, "outside_fixed_machine_energy_behavior_tree"),
    ("Portals", 19, 19, "outside_fixed_machine_energy_behavior_tree"),
    ("Batteries", 37, 37, "outside_fixed_machine_energy_behavior_tree"),
    ("Reactors", 46, 46, "reactor_part_not_machine_behavior"),
)
PINNED_NAMES = tuple(row[0] for row in PINNED_CATEGORIES)
EXPECTED_SITES = 129
EXPECTED_EXPANDED = 471
FULL_T35_SITES = 763
FULL_T35_EXPANDED = 1701
REMAINDER_SITES = 634
REMAINDER_EXPANDED = 1230
T13C_CAPABILITY_COUNT = 6
EXPECTED_BRONZE_PROFILES = (
    "cruciblecraft:bronze_firebox",
    "cruciblecraft:bronze_boiler",
    "cruciblecraft:bronze_steam_engine",
    "cruciblecraft:bronze_dynamo",
    "cruciblecraft:bronze_fuel_engine",
    "cruciblecraft:bronze_gas_generator",
)
EXPECTED_DEFINITION_COUNT = 9
EXPECTED_BEHAVIOR_COUNT = 8
FEASIBILITY_VALUES = (
    "bounded_extension",
    "requires_new_runtime",
    "defer_to_portfolio",
    "blocked",
)
FORBIDDEN_SUCCESSORS = (
    "logistics-cover-net-core",
    "combinatorial",
    "nuclear",
    "count-ceiling-kind-envelope",
    "t13c-exclusion-reclaim-core",
    "remainder-exclusion-reclaim",
    "panels-implementation",
    "sensors-implementation",
    "portals-implementation",
    "batteries-implementation",
    "reactors-implementation",
)
ALLOWED_TOPOLOGY_KEYS = (
    "append_only",
    "complete_family_count",
    "generated_by",
    "next_unassigned",
    "remaining_recipe_gap",
    "schema_version",
    "source_revision",
    "status",
    "unique_active_wave",
    "wave_slug",
)
CATEGORY_TOKENS = {
    "Panels": ("panel",),
    "Sensors": ("sensor",),
    "Portals": ("portal",),
    "Batteries": ("battery",),
    "Reactors": ("reactor",),
}
NUCLEAR_PORTFOLIO = "portfolio/nuclear"
BUILTIN_BEHAVIOR_RE = re.compile(r'registerBuiltin\("([a-z0-9_]+)"')
T35_EXCLUSION = census.EXCLUSION_RECLAIM
MACHINE_KINDS = census.TOOLS / "machine_tree_denominators" / "machine_kinds.json"
CAPABILITY_MAP = (
    census.TOOLS / "waves" / "portfolio" / "source-capability-map" / "capability_map.json"
)
GROWTH_ORDER = (
    census.TOOLS / "waves" / "portfolio" / "source-capability-map" / "growth_order.json"
)
ENERGY_CONVERTERS = (
    census.ROOT / "src/main/resources/data/cruciblecraft/energy_converters.json"
)
COVER_DEFINITIONS = (
    census.ROOT / "src/main/resources/data/cruciblecraft/cover_definitions.json"
)
COVER_BEHAVIOR_REGISTRY = (
    census.ROOT
    / "src/main/java/com/masson/cruciblecraft/logistics/pipe/cover"
    / "CoverBehaviorRegistry.java"
)
MACHINE_TIERS = census.MACHINE_TIERS
T30_READINESS = census.TOOLS / "hopper_readiness.json"
STORAGE_LOCK_READINESS = census.TOOLS / "storage_readiness.json"
LEFTOVER_LATER = (
    census.TOOLS / "waves" / "portfolio" / "source-capability-map-r0" / "leftover_later.json"
)
RECIPE_GENERATED = census.ROOT / "src" / "recipe_generated"
RECLAIM_QUESTIONS = {
    "disposition_policy": (
        "Per-category later cards must choose implemented, planned, deferred, "
        "or out_of_scope. R0 does not fill those dispositions."
    ),
    "energy_coupling": (
        "How batteries or reactors would couple to the selected T18 bronze "
        "conversion chain without treating that chain as a battery or reactor "
        "catalog."
    ),
    "fail_closed": (
        "Unknown variant must not silent no-op."
    ),
    "load": (
        "A later implementation card measures player acquisition, save, and "
        "server behavior separately; it cannot share a machine or energy owner."
    ),
    "panel_semantics": (
        "How six Panels source sites expand to 348 registrations. Sites are "
        "not six registrations."
    ),
    "player_acquisition": (
        "How a later implementation enters survival and creative paths."
    ),
    "portal_semantics": (
        "How portals link, travel across dimensions, and enter the save."
    ),
    "reactor_semantics": (
        "Part versus controller. This card must not implement fission, fusion, "
        "or plasma."
    ),
    "sensor_semantics": (
        "What a sensor detects, whether it emits redstone, and whether it "
        "joins any network. Distinct from T35 Redstone Wires and T19 covers."
    ),
}


def generated_by(_slug: str = SLUG) -> str:
    return GENERATED_BY


def nuclear_started() -> bool:
    return one_x.nuclear_started()


def leftover_later_count() -> int:
    leftover = census.load_json(LEFTOVER_LATER)
    total = int(leftover["counts"]["total"])
    if total != 39:
        raise ValueError(f"leftover_later_count {total} != 39")
    return total


def require_registered(slug: str) -> list[str]:
    errors: list[str] = []
    if slug not in KNOWN_SEMANTIC_SLUGS:
        errors.append(f"{slug} missing from KNOWN_SEMANTIC_SLUGS")
    if slug not in known_slugs():
        errors.append(f"{slug} missing from wave_closeout")
    return errors


def require_predecessor() -> list[str]:
    errors = closeout_seal.check_wave_seal(PREDECESSOR)
    readiness = census.load_json(wave_dir(PREDECESSOR) / "readiness.json")
    if readiness.get("status") != PREDECESSOR_STATUS:
        errors.append(
            f"{PREDECESSOR} status {readiness.get('status')} != {PREDECESSOR_STATUS}"
        )
    return errors


def load_t35() -> dict[str, Any]:
    return census.load_json(T35_EXCLUSION)


def category_summary_map(document: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        str(row["category"]): dict(row)
        for row in document.get("category_summaries") or []
    }


def pinned_source_sites(document: dict[str, Any]) -> list[dict[str, Any]]:
    wanted = set(PINNED_NAMES)
    return [dict(row) for row in document["source_sites"] if row.get("category") in wanted]


def authority_hashes() -> dict[str, str]:
    return {
        "capability_map": census.sha256_file(CAPABILITY_MAP),
        "growth_order": census.sha256_file(GROWTH_ORDER),
        "logistics_cover_net_r0_seal": census.sha256_file(
            wave_dir(PREDECESSOR) / "closeout_seal.json"
        ),
        "machine_tree_machine_kinds": census.sha256_file(MACHINE_KINDS),
        "census_excluded_object_reclaim": census.sha256_file(T35_EXCLUSION),
    }


def t13c_capability_rows() -> list[dict[str, Any]]:
    document = census.load_json(CAPABILITY_MAP)
    rows = [
        dict(row)
        for row in document.get("rows") or []
        if str(row.get("gt6_anchor") or "").startswith("T13c exclusion")
    ]
    if len(rows) != T13C_CAPABILITY_COUNT:
        raise ValueError(f"capability map T13c rows {len(rows)} != 6")
    for row in rows:
        if row.get("cc_mechanism") != "none":
            raise ValueError(
                f"{row.get('gt6_anchor')} cc_mechanism is {row.get('cc_mechanism')}"
            )
        if row.get("correspondence_class") != "capability_missing":
            raise ValueError(
                f"{row.get('gt6_anchor')} correspondence_class is "
                f"{row.get('correspondence_class')}"
            )
        if row.get("growth_blocker") != "no_owner":
            raise ValueError(
                f"{row.get('gt6_anchor')} growth_blocker is {row.get('growth_blocker')}"
            )
        if row.get("1x_disposition") != "not_in_1x":
            raise ValueError(
                f"{row.get('gt6_anchor')} 1x_disposition is {row.get('1x_disposition')}"
            )
    return rows


def capability_row_for(category: str) -> dict[str, Any]:
    expected = f"T13c exclusion {category}"
    for row in t13c_capability_rows():
        if row.get("gt6_anchor") == expected:
            return row
    raise ValueError(f"missing capability map row {expected}")


def energy_converter_profiles() -> list[dict[str, Any]]:
    document = census.load_json(ENERGY_CONVERTERS)
    profiles = [dict(row) for row in document.get("profiles") or []]
    ids = tuple(str(row["id"]) for row in profiles)
    if ids != EXPECTED_BRONZE_PROFILES:
        raise ValueError(f"energy converter profiles {ids} != selected bronze chain")
    for row in profiles:
        if row.get("status") != "COMPLETE":
            raise ValueError(f"{row.get('id')} status is {row.get('status')}")
    return profiles


def cover_definition_ids() -> list[str]:
    document = census.load_json(COVER_DEFINITIONS)
    ids = [str(row["id"]) for row in document["definitions"]]
    if len(ids) != EXPECTED_DEFINITION_COUNT:
        raise ValueError(f"cover definition count {len(ids)} != 9")
    blocked = [
        item
        for item in ids
        if any(token in item.lower() for token in ("panel", "sensor", "portal", "battery", "reactor"))
    ]
    if blocked:
        raise ValueError(f"cover_definitions grew T13c ids: {blocked}")
    return ids


def builtin_behavior_ids() -> list[str]:
    text = COVER_BEHAVIOR_REGISTRY.read_text(encoding="utf-8")
    ids = [f"cruciblecraft:{name}" for name in BUILTIN_BEHAVIOR_RE.findall(text)]
    if len(ids) != EXPECTED_BEHAVIOR_COUNT:
        raise ValueError(f"builtin behavior count {len(ids)} != 8")
    return ids


def machine_tier_kinds() -> list[str]:
    document = census.load_json(MACHINE_TIERS)
    kinds = sorted(
        {
            str(row["kind"])
            for row in document.get("variants") or []
            if isinstance(row, dict) and row.get("kind")
        }
    )
    if not kinds:
        raise ValueError("machine_tiers.json has no variant kind rows")
    return kinds


def nuclear_portfolio_named() -> bool:
    growth = census.load_json(GROWTH_ORDER)
    tracks = growth.get("tracks") or []
    return any(str(row.get("slug")) == NUCLEAR_PORTFOLIO for row in tracks)


def closed_hopper_storage() -> dict[str, Any]:
    hopper = census.load_json(T30_READINESS)
    storage = census.load_json(STORAGE_LOCK_READINESS)
    if hopper.get("status") != "T30_READY":
        raise ValueError(f"T30 status {hopper.get('status')} != T30_READY")
    if storage.get("status") != "STORAGE_LOCK_STORAGE_READY":
        raise ValueError(f"storage/lock status {storage.get('status')} != STORAGE_LOCK_STORAGE_READY")
    if int(storage.get("storage_source_sites", 0)) != 28:
        raise ValueError("storage/lock storage_source_sites != 28")
    if int(storage.get("storage_complete_registrations", 0)) != 624:
        raise ValueError("storage/lock storage_complete_registrations != 624")
    if int(storage.get("logistics_source_sites", 0)) != 1:
        raise ValueError("storage/lock logistics_source_sites != 1")
    return {
        "hoppers": {
            "source_sites": 2,
            "expanded_multiplicity": 2,
            "status": "T30_READY",
            "owner_here": False,
        },
        "mass_storage_logistics": {
            "source_sites": 1,
            "expanded_multiplicity": 1,
            "status": "STORAGE_LOCK_STORAGE_READY",
            "owner_here": False,
        },
        "storage": {
            "source_sites": 28,
            "expanded_multiplicity": 624,
            "status": "STORAGE_LOCK_STORAGE_READY",
            "owner_here": False,
        },
    }


def named_runtime_hits(category: str) -> list[str]:
    tokens = CATEGORY_TOKENS[category]
    hits: list[str] = []
    for profile in energy_converter_profiles():
        source = profile.get("source") or {}
        blob = " ".join(
            [
                str(profile.get("id") or ""),
                str(profile.get("runtimeBinding") or ""),
                str(source.get("machineKind") or ""),
            ]
        ).lower()
        if any(token in blob for token in tokens):
            hits.append(str(profile["id"]))
    for item in cover_definition_ids() + builtin_behavior_ids():
        if any(token in item.lower() for token in tokens):
            hits.append(item)
    for kind in machine_tier_kinds():
        if any(token in kind.lower() for token in tokens):
            hits.append(kind)
    return sorted(set(hits))


def inherited_denominator_document() -> dict[str, Any]:
    document = load_t35()
    counts = document["counts"]
    if int(counts["source_sites"]) != FULL_T35_SITES:
        raise ValueError(f"T35 source_sites {counts['source_sites']} != 763")
    if int(counts["expanded_multiplicity"]) != FULL_T35_EXPANDED:
        raise ValueError(
            f"T35 expanded_multiplicity {counts['expanded_multiplicity']} != 1701"
        )
    if int(counts.get("unmapped", 1)) != 0:
        raise ValueError(f"T35 unmapped {counts.get('unmapped')} != 0")
    summaries = category_summary_map(document)
    category_rows: list[dict[str, Any]] = []
    for name, sites, expanded, reason in PINNED_CATEGORIES:
        row = summaries.get(name)
        if row is None:
            raise ValueError(f"T35 missing category {name}")
        if int(row["source_sites"]) != sites:
            raise ValueError(f"{name} source_sites {row['source_sites']} != {sites}")
        if int(row["expanded_multiplicity"]) != expanded:
            raise ValueError(
                f"{name} expanded_multiplicity {row['expanded_multiplicity']} != {expanded}"
            )
        category_rows.append(dict(row))
    sites = pinned_source_sites(document)
    if len(sites) != EXPECTED_SITES:
        raise ValueError(f"pinned source_sites {len(sites)} != 129")
    expanded_total = sum(int(row["multiplicity"]) for row in sites)
    if expanded_total != EXPECTED_EXPANDED:
        raise ValueError(f"pinned expanded {expanded_total} != 471")
    by_category: dict[str, list[dict[str, Any]]] = {name: [] for name in PINNED_NAMES}
    for row in sites:
        by_category[str(row["category"])].append(row)
    for name, expected_sites, expected_expanded, reason in PINNED_CATEGORIES:
        rows = by_category[name]
        if len(rows) != expected_sites:
            raise ValueError(f"{name} site rows {len(rows)} != {expected_sites}")
        actual_expanded = sum(int(row["multiplicity"]) for row in rows)
        if actual_expanded != expected_expanded:
            raise ValueError(f"{name} site expanded {actual_expanded} != {expected_expanded}")
        drifted = [
            str(row["site_key"])
            for row in rows
            if row.get("historical_exclusion_reason") != reason
        ]
        if drifted:
            raise ValueError(f"{name} exclusion reason drifted: {drifted[:3]}")
    panel_sites = by_category["Panels"]
    if len(panel_sites) == EXPECTED_EXPANDED:
        raise ValueError("Panels 6 sites must not be treated as 348 registrations")
    live_by_key = {str(row["site_key"]): row for row in document["source_sites"]}
    for row in sites:
        live = live_by_key[str(row["site_key"])]
        drift = census.first_json_diff(live, row)
        if drift:
            raise ValueError(f"{row['site_key']} is not byte-identical to T35: {drift}")
    already_closed = closed_hopper_storage()
    return {
        "already_closed_elsewhere": already_closed,
        "authority_hashes": authority_hashes(),
        "category_summaries": category_rows,
        "expanded_multiplicity": EXPECTED_EXPANDED,
        "full_t35": {
            "expanded_multiplicity": FULL_T35_EXPANDED,
            "source_sites": FULL_T35_SITES,
            "unmapped": 0,
        },
        "generated_by": GENERATED_BY,
        "remainder_after_slice": {
            "expanded_multiplicity": REMAINDER_EXPANDED,
            "source_sites": REMAINDER_SITES,
        },
        "schema_version": 1,
        "source_artifact": census.relative(T35_EXCLUSION),
        "source_revision": SOURCE_REVISION,
        "source_sites": sites,
        "source_site_count": EXPECTED_SITES,
        "status": "INHERITED_DENOMINATOR_READY",
        "wave_slug": SLUG,
    }


def source_semantics_document(inherited: dict[str, Any]) -> dict[str, Any]:
    document = load_t35()
    summaries = category_summary_map(document)
    redstone = summaries.get("Redstone Wires")
    if redstone is None:
        raise ValueError("T35 missing Redstone Wires contrast category")
    if int(redstone["source_sites"]) != 6 or int(redstone["expanded_multiplicity"]) != 6:
        raise ValueError("Redstone Wires contrast drifted from 6/6")
    bronze_ids = [str(row["id"]) for row in energy_converter_profiles()]
    rows: list[dict[str, Any]] = []
    by_category: dict[str, list[dict[str, Any]]] = {name: [] for name in PINNED_NAMES}
    for site in inherited["source_sites"]:
        by_category[str(site["category"])].append(site)
    for name, sites, expanded, reason in PINNED_CATEGORIES:
        category_sites = by_category[name]
        behavior_classes = sorted({str(row["behavior_class"]) for row in category_sites})
        families = sorted({str(row["canonical_family"]) for row in category_sites})
        reasons = sorted(
            {str(row["historical_exclusion_reason"]) for row in category_sites}
        )
        if reasons != [reason]:
            raise ValueError(f"{name} reasons {reasons} != [{reason}]")
        contrast: dict[str, Any]
        if name == "Panels":
            contrast = {
                "site_multiplicities": [
                    {
                        "multiplicity": int(row["multiplicity"]),
                        "site_key": str(row["site_key"]),
                    }
                    for row in category_sites
                ],
                "sites_are_not_registrations": True,
                "statement": (
                    "Six loader sites expand by multiplicity to 348 registrations. "
                    "They are not six registrations."
                ),
            }
        elif name == "Sensors":
            contrast = {
                "open_questions": [
                    "detection",
                    "redstone_output",
                    "network_membership",
                ],
                "redstone_wires_not_in_slice": {
                    "category": "Redstone Wires",
                    "expanded_multiplicity": int(redstone["expanded_multiplicity"]),
                    "source_sites": int(redstone["source_sites"]),
                },
                "cover_cover_has_sensor": False,
            }
        elif name == "Portals":
            portal_classes = [item for item in behavior_classes if "Portal" in item]
            if not portal_classes:
                raise ValueError("Portals missing Portal behavior_class evidence")
            contrast = {
                "open_questions": ["linking", "dimension_travel", "save"],
                "portal_behavior_classes": portal_classes,
            }
        elif name == "Batteries":
            contrast = {
                "energy_chain_bronze_profiles": bronze_ids,
                "energy_chain_is_battery_catalog": False,
            }
        else:
            contrast = {
                "destination_portfolio": NUCLEAR_PORTFOLIO,
                "nuclear_started": nuclear_started(),
                "open_questions": ["part_vs_controller"],
                "part_not_controller": True,
            }
        rows.append(
            {
                "behavior_classes": behavior_classes,
                "canonical_families": families,
                "category": name,
                "contrast": contrast,
                "evidence_class": "SOURCE_BACKED",
                "expanded_multiplicity": expanded,
                "historical_exclusion_reasons": reasons,
                "source_sites": sites,
            }
        )
    return {
        "categories": rows,
        "generated_by": GENERATED_BY,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SOURCE_SEMANTICS_READY",
        "wave_slug": SLUG,
    }


def existing_mechanism_document() -> dict[str, Any]:
    definitions = cover_definition_ids()
    behaviors = builtin_behavior_ids()
    profiles = energy_converter_profiles()
    capability_rows = t13c_capability_rows()
    already_closed = closed_hopper_storage()
    mapped: list[dict[str, Any]] = []
    analogs = {
        "Panels": "T19 adjacent 9/8 cover stack is not a panel MTE",
        "Sensors": "T19 covers have no sensor behavior; Redstone Wires is a different T35 category",
        "Portals": "T19 adjacent 9/8 cover stack is not a portal MTE",
        "Batteries": "T18 selected bronze conversion chain is not a battery catalog",
        "Reactors": "T18 selected bronze conversion chain is not a reactor catalog",
    }
    for name, _sites, _expanded, _reason in PINNED_CATEGORIES:
        hits = named_runtime_hits(name)
        capability = capability_row_for(name)
        mechanism = str(capability["cc_mechanism"])
        if hits:
            mechanism = hits[0]
        mapped.append(
            {
                "adjacent_analog": analogs[name],
                "category": name,
                "cc_mechanism": mechanism,
                "gap": "mechanism" if mechanism == "none" else "none",
                "named_runtime_hits": hits,
            }
        )
    return {
        "already_closed_elsewhere": already_closed,
        "behavior_count": len(behaviors),
        "behaviors": behaviors,
        "capability_map_t13c": capability_rows,
        "cover_definitions_sha256": census.sha256_file(COVER_DEFINITIONS),
        "definition_count": len(definitions),
        "definitions": definitions,
        "energy_converter_catalog": (
            "src/main/java/com/masson/cruciblecraft/energy/converter/"
            "EnergyConverterCatalog.java"
        ),
        "energy_converter_profiles": [str(row["id"]) for row in profiles],
        "energy_converters_sha256": census.sha256_file(ENERGY_CONVERTERS),
        "generated_by": GENERATED_BY,
        "kinds": mapped,
        "note": (
            "The gap is a missing mechanism, not a missing definition row. "
            "Identity-only building blocks, hopper covers, and the selected "
            "bronze energy chain do not cover these five categories."
        ),
        "nuclear_started": nuclear_started(),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "EXISTING_MECHANISM_READY",
        "wave_slug": SLUG,
    }


def reclaim_contract_document() -> dict[str, Any]:
    return {
        "generated_by": GENERATED_BY,
        "implemented": False,
        "questions": dict(RECLAIM_QUESTIONS),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "RECLAIM_CONTRACT_READY",
        "wave_slug": SLUG,
    }


def derive_feasibility(
    inherited: dict[str, Any],
    semantics: dict[str, Any],
    mechanism: dict[str, Any],
    contract: dict[str, Any],
) -> dict[str, Any]:
    mechanism_by_category = {row["category"]: row for row in mechanism["kinds"]}
    semantics_by_category = {row["category"]: row for row in semantics["categories"]}
    categories: list[dict[str, Any]] = []
    for name, sites, expanded, _reason in PINNED_CATEGORIES:
        missing: list[str] = []
        summary = next(
            row for row in inherited["category_summaries"] if row["category"] == name
        )
        if int(summary["source_sites"]) != sites:
            missing.append("dump")
        if int(summary["expanded_multiplicity"]) != expanded:
            missing.append("dump")
        semantic = semantics_by_category.get(name)
        if semantic is None or int(semantic["source_sites"]) != sites:
            missing.append("dump")
        if set(contract["questions"]) != set(RECLAIM_QUESTIONS):
            missing.append("schema")
        load_text = str(contract["questions"].get("load") or "")
        if "separately" not in load_text:
            missing.append("measurement")
        mapped = mechanism_by_category[name]
        reasons: list[str]
        destination: str | None = None
        if missing:
            verdict = "blocked"
            allows_child = False
            reasons = [f"{name} is missing " + ", ".join(sorted(set(missing)))]
        elif name == "Reactors":
            if not nuclear_portfolio_named():
                verdict = "blocked"
                allows_child = False
                missing.append("schema")
                reasons = ["Reactors require a named portfolio/nuclear destination"]
            elif nuclear_started():
                verdict = "blocked"
                allows_child = False
                missing.append("schema")
                reasons = ["nuclear Track C started must stay false on this card"]
            else:
                verdict = "defer_to_portfolio"
                allows_child = False
                destination = NUCLEAR_PORTFOLIO
                reasons = [
                    "All 46 Reactors carry reactor_part_not_machine_behavior.",
                    "growth_order already names portfolio/nuclear.",
                    "nuclear_started stays false. This card does not implement "
                    "fission, fusion, or plasma.",
                ]
        elif mapped["cc_mechanism"] != "none":
            verdict = "bounded_extension"
            allows_child = True
            reasons = [
                f"{name} maps onto named existing CC runtime {mapped['cc_mechanism']}."
            ]
        else:
            verdict = "requires_new_runtime"
            allows_child = False
            reasons = [
                f"{name} capability-map cc_mechanism is none.",
                "No named energy-converter, cover, or machine-tier runtime covers it.",
                "The gap is a missing mechanism, not a missing definition row.",
            ]
        if verdict not in FEASIBILITY_VALUES:
            raise ValueError(f"illegal feasibility {verdict}")
        categories.append(
            {
                "allows_implementation_child": allows_child,
                "category": name,
                "destination": destination,
                "missing_evidence": sorted(set(missing)),
                "reasons": reasons,
                "verdict": verdict,
            }
        )
    blocked = [row for row in categories if row["verdict"] == "blocked"]
    allows_any = any(row["allows_implementation_child"] for row in categories)
    return {
        "allows_implementation_child": allows_any,
        "categories": categories,
        "generated_by": GENERATED_BY,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "BLOCKED" if blocked else "FEASIBILITY_READY",
        "wave_slug": SLUG,
    }


def recipe_generated_mentions_slug() -> bool:
    if not RECIPE_GENERATED.is_dir():
        return False
    token = "t13c-exclusion-reclaim"
    for path in RECIPE_GENERATED.rglob("*"):
        if token in path.as_posix():
            return True
    return False


def readiness_note(feasibility: dict[str, Any]) -> str:
    parts = [
        f"{row['category']} {row['verdict']}" for row in feasibility["categories"]
    ]
    return (
        f"{STATUS}. Five T13c categories inherited byte-identical 129/471. "
        + "; ".join(parts)
        + ". No implementation child is assigned. unique_active_wave is null."
    )


def evidence_document(
    inherited: dict[str, Any],
    feasibility: dict[str, Any],
) -> dict[str, Any]:
    leftover = leftover_later_count()
    return {
        "allows_implementation_child": feasibility["allows_implementation_child"],
        "completion_delta": 0,
        "feasibility_by_category": {
            row["category"]: row["verdict"] for row in feasibility["categories"]
        },
        "generated_recipe_count": 0,
        "inherited_expanded_multiplicity": inherited["expanded_multiplicity"],
        "inherited_source_sites": inherited["source_site_count"],
        "leftover_later_count": leftover,
        "nuclear_track_c_started": nuclear_started(),
        "owns_families": 0,
        "partial_family_count": 0,
        "production_lock": None,
        "recipe_files_generated": False,
    }


def common_documents(*, evidence: dict[str, Any], note: str) -> dict[str, Any]:
    spec = spec_for(SLUG)
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": GENERATED_BY,
        "leftover_later_count": leftover_later_count(),
        "partial_family_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": SLUG,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": GENERATED_BY,
        "next_unassigned": spec.next_unassigned,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": SLUG,
    }
    readiness = {
        "evidence": evidence,
        "generated_by": GENERATED_BY,
        "next_unassigned": spec.next_unassigned,
        "note": note,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": STATUS,
        "unique_active_wave": spec.unique_active_wave,
        "wave_complete": True,
        "wave_slug": SLUG,
    }
    wave = {
        "cohort": "t13c-exclusion-reclaim-r0",
        "depends_on": [PREDECESSOR],
        "generated_by": GENERATED_BY,
        "owns_families": 0,
        "program": SLUG,
        "schema_version": 1,
        "wave_slug": SLUG,
    }
    return {
        "census_delta.json": census,
        "readiness.json": readiness,
        "topology.json": topology,
        "wave.json": wave,
    }


def build_r0_documents() -> dict[str, Any]:
    errors = require_predecessor()
    if errors:
        raise ValueError("; ".join(errors))
    inherited = inherited_denominator_document()
    semantics = source_semantics_document(inherited)
    mechanism = existing_mechanism_document()
    contract = reclaim_contract_document()
    feasibility = derive_feasibility(inherited, semantics, mechanism, contract)
    blocked = [
        row for row in feasibility["categories"] if row["verdict"] == "blocked"
    ]
    if blocked:
        detail = ", ".join(
            f"{row['category']}:" + ",".join(row["missing_evidence"] or ["blocked"])
            for row in blocked
        )
        raise ValueError(f"feasibility is blocked: {detail}")
    if recipe_generated_mentions_slug():
        raise ValueError("src/recipe_generated mentions t13c-exclusion-reclaim")
    evidence = evidence_document(inherited, feasibility)
    documents = common_documents(
        evidence=evidence,
        note=readiness_note(feasibility),
    )
    documents["inherited_denominator.json"] = inherited
    documents["source_semantics.json"] = semantics
    documents["existing_mechanism.json"] = mechanism
    documents["reclaim_contract.json"] = contract
    documents["feasibility.json"] = feasibility
    return documents


def write_seal() -> dict[str, Any]:
    root = wave_dir(SLUG)
    hashes = {
        "census": census.sha256_file(root / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": census.sha256_file(root / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": census.sha256_file(root / "topology.json"),
    }
    seal = {
        "card_id": SLUG,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": census.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": census.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{GENERATED_BY} --write",
        "hashes": hashes,
        "note": census.load_json(root / "readiness.json").get("note"),
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": 0,
        "relation_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": SLUG,
        "source_revision": SOURCE_REVISION,
        "status": "SEALED",
    }
    census.write_stable(root / "closeout_seal.json", seal)
    return seal


def write_artifacts() -> dict[str, Any]:
    documents = build_r0_documents()
    root = wave_dir(SLUG)
    root.mkdir(parents=True, exist_ok=True)
    for name, document in documents.items():
        census.write_stable(root / name, document)
    write_seal()
    spec = spec_for(SLUG)
    return {
        "status": STATUS,
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": SLUG,
    }


def check_forbidden_successors(haystack: str) -> list[str]:
    errors: list[str] = []
    for token in FORBIDDEN_SUCCESSORS:
        if token in haystack:
            errors.append(f"topology successor {token} is forbidden")
    return errors


def check_artifacts() -> list[str]:
    errors = require_registered(SLUG)
    if errors:
        return errors
    spec = spec_for(SLUG)
    if spec.owns_families != 0:
        errors.append(f"{SLUG} owns_families must be 0")
    if spec.production_lock is not None:
        errors.append(f"{SLUG} must not carry a production lock")
    if spec.unique_active_wave is not None:
        errors.append("unique_active_wave must be null")
    if not spec.next_unassigned:
        errors.append("next_unassigned must be true")
    errors.extend(check_forbidden_successors(spec.unique_active_wave or ""))
    root = wave_dir(SLUG)
    if not (root / "readiness.json").is_file():
        return errors + [f"{SLUG} artifacts are missing"]
    try:
        live = build_r0_documents()
    except ValueError as error:
        return errors + [str(error)]
    for name, document in live.items():
        committed = census.load_json(root / name)
        drift = census.first_json_diff(document, committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = census.load_json(root / "readiness.json")
    if readiness.get("status") != STATUS:
        errors.append(f"{SLUG} status drifted")
    if readiness.get("unique_active_wave") is not None:
        errors.append(f"{SLUG} unique_active_wave must be null")
    if nuclear_started():
        errors.append("nuclear Track C started must stay false")
    evidence = readiness.get("evidence") or {}
    if int(evidence.get("completion_delta", 1)) != 0:
        errors.append(f"{SLUG} completion_delta must be 0")
    if evidence.get("recipe_files_generated"):
        errors.append(f"{SLUG} must not generate recipes")
    if int(evidence.get("leftover_later_count", 0)) != 39:
        errors.append(f"{SLUG} leftover_later_count must be 39")
    if int(evidence.get("inherited_source_sites", 0)) != EXPECTED_SITES:
        errors.append("inherited_source_sites must be 129")
    if int(evidence.get("inherited_expanded_multiplicity", 0)) != EXPECTED_EXPANDED:
        errors.append("inherited_expanded_multiplicity must be 471")
    topology = census.load_json(root / "topology.json")
    extra = sorted(set(topology) - set(ALLOWED_TOPOLOGY_KEYS))
    if extra:
        errors.append(f"topology has forbidden keys: {extra}")
    errors.extend(check_forbidden_successors(json.dumps(topology, sort_keys=True)))
    feasibility = census.load_json(root / "feasibility.json")
    by_category = {row["category"]: row for row in feasibility.get("categories") or []}
    if tuple(by_category) != PINNED_NAMES:
        errors.append("feasibility categories drifted from the pinned five")
    for name, _sites, _expanded, _reason in PINNED_CATEGORIES:
        row = by_category.get(name)
        if row is None:
            errors.append(f"feasibility missing {name}")
            continue
        if row.get("verdict") not in FEASIBILITY_VALUES:
            errors.append(f"{name} feasibility.verdict is not an allowed enum")
        if row.get("verdict") != "bounded_extension" and row.get(
            "allows_implementation_child"
        ):
            errors.append(f"{name} must not allow an implementation child")
        if name == "Reactors":
            if row.get("verdict") != "defer_to_portfolio":
                errors.append("Reactors must defer_to_portfolio")
            if row.get("destination") != NUCLEAR_PORTFOLIO:
                errors.append("Reactors destination must be portfolio/nuclear")
    inherited = census.load_json(root / "inherited_denominator.json")
    if int(inherited.get("source_site_count", 0)) != EXPECTED_SITES:
        errors.append("inherited source_site_count must be 129")
    if int(inherited.get("expanded_multiplicity", 0)) != EXPECTED_EXPANDED:
        errors.append("inherited expanded_multiplicity must be 471")
    live_t35 = load_t35()
    live_by_key = {str(row["site_key"]): row for row in live_t35["source_sites"]}
    for row in inherited.get("source_sites") or []:
        live_row = live_by_key.get(str(row["site_key"]))
        if live_row is None:
            errors.append(f"{row.get('site_key')} missing from live T35")
            continue
        drift = census.first_json_diff(live_row, row)
        if drift:
            errors.append(f"{row['site_key']} is not byte-identical to T35: {drift}")
    panel_rows = [
        row for row in inherited.get("source_sites") or [] if row.get("category") == "Panels"
    ]
    if len(panel_rows) != 6:
        errors.append(f"Panels site count {len(panel_rows)} != 6")
    if sum(int(row.get("multiplicity", 0)) for row in panel_rows) != 348:
        errors.append("Panels expanded multiplicity must stay 348")
    hashes = inherited.get("authority_hashes") or {}
    live_hashes = authority_hashes()
    for key, digest in live_hashes.items():
        if hashes.get(key) != digest:
            errors.append(f"authority hash {key} drifted")
    if recipe_generated_mentions_slug():
        errors.append("src/recipe_generated mentions t13c-exclusion-reclaim")
    errors.extend(closeout_seal.check_wave_seal(SLUG))
    return errors


def main_for(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=f"Write or check {SLUG}.")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            print(json.dumps(write_artifacts(), sort_keys=True))
            return 0
        errors = check_artifacts()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{SLUG} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"{SLUG} failed: {error}", file=sys.stderr)
        return 1
