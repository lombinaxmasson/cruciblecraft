#!/usr/bin/env python3
"""Source-capability-map program: schema freeze, inventory, growth order, closeout."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from typing import Any

from tools import closeout_seal
from tools import portfolio_one_x as one_x
from tools import t35_common as t35
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir

PROGRAM = "portfolio/source-capability-map"
R0 = "portfolio/source-capability-map-r0"
INVENTORY = "portfolio/source-capability-inventory"
GROWTH = "portfolio/source-capability-growth-order"
JOINT_EXIT = "portfolio/one-x-joint-exit"
SOURCE_REVISION = t35.SOURCE_REVISION
SEED_FIELDS = (
    "1x_disposition",
    "cc_mechanism",
    "correspondence_class",
    "growth_blocker",
    "gt6_anchor",
    "gt6_domain",
    "recheck_condition",
)
CORRESPONDENCE_CLASSES = (
    "equivalent",
    "selected_subset",
    "identity_only",
    "capability_missing",
    "post_1x",
    "out_of_scope_historical",
)
GROWTH_BLOCKERS = (
    "none",
    "no_generator",
    "hardcoded_spec",
    "count_ceiling",
    "no_owner",
    "fail_closed_runtime",
    "selected_only",
)
DISPOSITIONS_1X = ("in_1x_closed", "in_1x_selected", "not_in_1x")
GT6_DOMAINS = ("cover", "loader", "oredict", "tileentity", "worldgen")
LEFTOVER_FIELDS = (
    "disposition",
    "family_id",
    "owner",
    "portfolio_scope",
    "source_artifact",
    "template_key",
)
LEFTOVER_DISPOSITIONS = ("phase_deferred", "post_1x_scope")
LEFTOVER_SCOPES = ("candidate_1x", "post_1x")
CHILD_SLUGS = (R0, INVENTORY, GROWTH, PROGRAM)
STATUSES = {
    R0: "SOURCE_CAPABILITY_MAP_R0_READY",
    INVENTORY: "SOURCE_CAPABILITY_INVENTORY_READY",
    GROWTH: "SOURCE_CAPABILITY_GROWTH_ORDER_READY",
    PROGRAM: "SOURCE_CAPABILITY_MAP_READY",
}
PREDECESSORS = {
    R0: (JOINT_EXIT, "ONE_X_JOINT_EXIT_READY"),
    INVENTORY: (R0, "SOURCE_CAPABILITY_MAP_R0_READY"),
    GROWTH: (INVENTORY, "SOURCE_CAPABILITY_INVENTORY_READY"),
}
DEPENDS_ON = {
    R0: [JOINT_EXIT],
    INVENTORY: [R0],
    GROWTH: [INVENTORY],
    PROGRAM: [R0, INVENTORY, GROWTH],
}
NOTES = {
    R0: (
        "SOURCE_CAPABILITY_MAP_R0_READY. Schema frozen; 22 seed rows inherited "
        "byte-identical; leftover 39 accounted. No recipes. No inventory expansion."
    ),
    INVENTORY: (
        "SOURCE_CAPABILITY_INVENTORY_READY. Capability map replayable against the "
        "pinned dump. Leftover 39 accounted. No implementation started."
    ),
    GROWTH: (
        "SOURCE_CAPABILITY_GROWTH_ORDER_READY. next_major assigned on the mechanism "
        "track. Nuclear Track C stays started=false. No recipes."
    ),
    PROGRAM: (
        "SOURCE_CAPABILITY_MAP_READY. Capability map and growth-order are current. "
        "next_major comes from growth-order. Nuclear Track C stays started=false. "
        "unique_active_wave is null."
    ),
}
JOINT_SEED = t35.TOOLS / "waves" / "portfolio" / "one-x-joint-exit" / "capability_map_seed.json"
DISPOSITION_LEDGER = (
    t35.TOOLS / "waves" / "portfolio" / "census-disposition-replay" / "disposition_ledger.json"
)
POST_1X_SCOPE = (
    t35.TOOLS / "waves" / "portfolio" / "one-x-exit-r0" / "post_1x_scope_replay.json"
)
DUMP_ROOT = t35.ROOT / "gt6_dump" / "gt6_recipe_dump"
DUMP_INDEX = DUMP_ROOT / "index.json"
DUMP_MAPS = DUMP_ROOT / "maps"
FIX_ROOT = t35.ROOT / "4.5Fix"
PREFIX_INDEX = (
    t35.ROOT
    / "src/main/resources/data/cruciblecraft/material_prefixes/index.json"
)
COVER_DEFINITIONS = (
    t35.ROOT / "src/main/resources/data/cruciblecraft/cover_definitions.json"
)
ENERGY_CONVERTERS = (
    t35.ROOT / "src/main/resources/data/cruciblecraft/energy_converters.json"
)
ORE_VEINS = (
    t35.ROOT / "src/main/resources/data/cruciblecraft/worldgen_catalog/ore_veins.json"
)
FLUID_DEPOSITS = (
    t35.ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/fluid_deposits.json"
)
SURFACE_SCATTER = (
    t35.ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/surface_scatter.json"
)
MACHINE_TIERS = (
    t35.ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
)
T27_RECIPE_MAPS = t35.TOOLS / "t27_portfolio" / "recipe_maps.json"
T13_COVER_POLICY = t35.TOOLS / "t13_cover_multiblock_policy.json"
T35_EXCLUSION = t35.TOOLS / "t35_excluded_object_reclaim.json"
WORLDGEN_OTHER = DUMP_ROOT / "worldgen" / "other_features.json"
EXPECTED_NUCLEAR = frozenset(
    {
        "gt.recipe.centrifuge#0169",
        "gt.recipe.centrifuge#0176",
        "gt.recipe.centrifuge#0180",
        "gt.recipe.centrifuge#0185",
        "gt.recipe.centrifuge#0188",
        "gt.recipe.centrifuge#0190",
        "gt.recipe.centrifuge#0191",
    }
)
EXPECTED_ASSEMBLER = frozenset(
    {"gt.recipe.assembler#0000", "gt.recipe.assembler#0001"}
)
EXPECTED_ELECTROLYZER = frozenset(
    {"gt.recipe.electrolyzer#0000", "gt.recipe.electrolyzer#0001"}
)
EXPECTED_SMELTER = frozenset(
    {"gt.recipe.smelter#1829", "gt.recipe.smelter#1884"}
)
EXPECTED_CENTRIFUGE_NON = frozenset(
    {"gt.recipe.centrifuge#0010", "gt.recipe.centrifuge#0207"}
)
T13C_COUNTS = (
    ("Panels", 348),
    ("Sensors", 21),
    ("Portals", 19),
    ("Batteries", 37),
    ("Reactors", 46),
)
CC_PROCESSING_MAPS = (
    ("coke_oven", ("gt.recipe.cokeoven",)),
    ("crusher", ("gt.recipe.crusher",)),
    ("anvil", ("gt.recipe.anvil",)),
    ("anvil_bend_small", ("gt.recipe.anvil.bend.small",)),
    ("anvil_bend_big", ("gt.recipe.anvil.bend.big",)),
    ("sluice", ("gt.recipe.sluice",)),
    ("bath", ("gt.recipe.bath",)),
    ("centrifuge", ("gt.recipe.centrifuge",)),
    ("shredder", ("gt.recipe.shredder",)),
    ("sifter", ("gt.recipe.sifter",)),
    ("smelter", ("gt.recipe.smelter",)),
    ("cooling", ("gt.recipe.cooling",)),
    ("mortar", ("gt.recipe.mortar",)),
    ("extruder", ("gt.recipe.extruder",)),
    ("cutter", ("gt.recipe.cutter",)),
    ("lathe", ("gt.recipe.lathe",)),
    ("rollingmill", ("gt.recipe.rollingmill",)),
    ("rollbender", ("gt.recipe.rollbender",)),
    ("wiremill", ("gt.recipe.wiremill",)),
    ("bender", ("gt.recipe.anvil.bend.small", "gt.recipe.anvil.bend.big")),
    ("assembler", ("gt.recipe.assembler",)),
    ("welder", ("gt.recipe.welder",)),
    ("press", ("gt.recipe.press",)),
    ("electrolyzer", ("gt.recipe.electrolyzer",)),
    ("mixer", ("gt.recipe.mixer",)),
    ("distillery", ("gt.recipe.distillery",)),
    ("autoclave", ("gt.recipe.autoclave",)),
    ("drying", ("gt.recipe.drying",)),
    ("compressor", ("gt.recipe.compressor",)),
    ("generifier", ("gt.recipe.generifier",)),
    ("roaster", ("gt.recipe.roaster",)),
    ("coagulator", ("gt.recipe.coagulator",)),
)
SELECTED_COVERS = (
    "cruciblecraft:filter",
    "cruciblecraft:shutter",
    "cruciblecraft:pump",
    "cruciblecraft:conveyor",
    "cruciblecraft:retriever_item",
    "cruciblecraft:robot_arm",
    "cruciblecraft:pressure_valve",
    "cruciblecraft:selector_manual",
)
FUEL_CC_MAPS = (
    ("fuels_engine", "gt.recipe.fuels.engine"),
    ("fuels_gas", "gt.recipe.fuels.gas"),
)
COUNT_CEILING_POINTS = (
    (
        "ALL_PUBLISHED_RECIPE_BUDGET = 21000",
        "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java",
    ),
    (
        "ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING = 56000",
        "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java",
    ),
    (
        "ALL_LAZY_RECIPE_CACHE_HARD_CEILING = 4096",
        "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java",
    ),
    (
        "RECIPE_LOOKUP_MAX_CANDIDATE_HARD_CEILING / HARD_SHARD_CEILING = 128",
        "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java + src/main/java/com/masson/cruciblecraft/recipe/gt/CompactRecipeShardRouter.java",
    ),
)
NEXT_MAJOR_SLUG = "portfolio/generic-recipe-generator"


def generated_by(slug: str) -> str:
    names = {
        R0: "python tools/build_source_capability_map_r0.py",
        INVENTORY: "python tools/source_capability_map.py",
        GROWTH: "python tools/build_source_capability_growth_order.py",
        PROGRAM: "python tools/build_source_capability_map.py",
    }
    return names[slug]


def nuclear_started() -> bool:
    return one_x.nuclear_started()


def require_registered(slug: str) -> list[str]:
    errors: list[str] = []
    if slug not in KNOWN_SEMANTIC_SLUGS:
        errors.append(f"{slug} missing from KNOWN_SEMANTIC_SLUGS")
    if slug not in known_slugs():
        errors.append(f"{slug} missing from wave_closeout")
    return errors


def require_predecessor(slug: str) -> list[str]:
    if slug == PROGRAM:
        errors: list[str] = []
        for child in CHILD_SLUGS[:-1]:
            errors.extend(closeout_seal.check_wave_seal(child))
            readiness = t35.load_json(wave_dir(child) / "readiness.json")
            expected = STATUSES[child]
            if readiness.get("status") != expected:
                errors.append(f"{child} status {readiness.get('status')} != {expected}")
        return errors
    predecessor, status = PREDECESSORS[slug]
    errors = closeout_seal.check_wave_seal(predecessor)
    readiness = t35.load_json(wave_dir(predecessor) / "readiness.json")
    if readiness.get("status") != status:
        errors.append(f"{predecessor} status {readiness.get('status')} != {status}")
    return errors


def require_opening_files() -> None:
    missing = [
        t35.relative(path)
        for path in (JOINT_SEED, DISPOSITION_LEDGER, POST_1X_SCOPE)
        if not path.is_file()
    ]
    if missing:
        raise ValueError("opening artifact missing: " + ", ".join(missing))


def dump_present() -> bool:
    return DUMP_INDEX.is_file() and DUMP_MAPS.is_dir()


def require_dump() -> None:
    if FIX_ROOT.exists() and not dump_present():
        raise ValueError("4.5Fix/ is not a canonical dump; gt6_dump/gt6_recipe_dump is required")
    if not dump_present():
        raise ValueError("gt6_dump/gt6_recipe_dump missing; refuse inventory")


def seed_fields(row: dict[str, Any]) -> dict[str, str]:
    return {field: str(row[field]) for field in SEED_FIELDS}


def inherited_seed_rows() -> list[dict[str, str]]:
    require_opening_files()
    document = t35.load_json(JOINT_SEED)
    rows = [seed_fields(row) for row in document.get("rows") or []]
    if len(rows) != 22:
        raise ValueError(f"inherited seed {len(rows)} != 22")
    if str(document.get("source_revision")) != SOURCE_REVISION:
        raise ValueError("joint-exit seed source_revision drifted")
    return rows


def leftover_rows() -> list[dict[str, str]]:
    require_opening_files()
    rows: list[dict[str, str]] = []
    ledger = t35.load_json(DISPOSITION_LEDGER)
    ledger_rel = t35.relative(DISPOSITION_LEDGER)
    for family in ledger.get("families") or []:
        closing = family.get("closing") or {}
        if closing.get("disposition") != "phase_deferred":
            continue
        family_id = str(family["family_id"])
        owner = str(closing.get("owner") or closing.get("future_owner") or "")
        rows.append(
            {
                "disposition": "phase_deferred",
                "family_id": family_id,
                "owner": owner,
                "portfolio_scope": str(closing["portfolio_scope"]),
                "source_artifact": ledger_rel,
                "template_key": family_id.rsplit("/", 1)[-1],
            }
        )
    post = t35.load_json(POST_1X_SCOPE)
    post_rel = t35.relative(POST_1X_SCOPE)
    for row in post.get("rows") or []:
        family_id = str(row["family_id"])
        rows.append(
            {
                "disposition": "post_1x_scope",
                "family_id": family_id,
                "owner": str(row["future_owner"]),
                "portfolio_scope": "post_1x",
                "source_artifact": post_rel,
                "template_key": str(row["template_key"]),
            }
        )
    rows.sort(
        key=lambda row: (
            0 if row["disposition"] == "phase_deferred" else 1,
            row["owner"],
            row["family_id"],
        )
    )
    by_disposition = Counter(row["disposition"] for row in rows)
    by_owner = Counter(row["owner"] for row in rows)
    keys = {row["template_key"] for row in rows}
    hanging = [
        row["family_id"]
        for row in rows
        if not row["owner"]
        or row["disposition"] not in LEFTOVER_DISPOSITIONS
        or row["portfolio_scope"] not in LEFTOVER_SCOPES
    ]
    if len(rows) != 39:
        raise ValueError(f"leftover {len(rows)} != 39")
    if by_disposition.get("phase_deferred") != 11:
        raise ValueError("phase_deferred leftover drifted")
    if by_disposition.get("post_1x_scope") != 28:
        raise ValueError("post_1x_scope leftover drifted")
    if hanging:
        raise ValueError("hanging leftover: " + ",".join(hanging[:8]))
    if {row["template_key"] for row in rows if row["owner"] == "later:assembler_combinatorial"} != EXPECTED_ASSEMBLER:
        raise ValueError("assembler combinatorial leftover drifted")
    if {row["template_key"] for row in rows if row["owner"] == "later:electrolyzer_combinatorial"} != EXPECTED_ELECTROLYZER:
        raise ValueError("electrolyzer combinatorial leftover drifted")
    if {row["template_key"] for row in rows if row["owner"] == "post_1x:nuclear"} != EXPECTED_NUCLEAR:
        raise ValueError("nuclear leftover drifted")
    if {row["template_key"] for row in rows if row["owner"] == "post_1.x/smelter_recovery_edge"} != EXPECTED_SMELTER:
        raise ValueError("smelter leftover drifted")
    if by_owner.get("post_1.x/ordinary_autoclave_processing") != 24:
        raise ValueError("autoclave leftover drifted")
    if {row["template_key"] for row in rows if row["owner"] in (
        "post_1.x/centrifuge_execution_envelope",
        "post_1.x/cross_mod_pahoehoe",
    )} != EXPECTED_CENTRIFUGE_NON:
        raise ValueError("centrifuge non-recycling leftover drifted")
    if "later:recycling" in keys:
        raise ValueError("later:recycling leaked into leftover")
    return rows


def leftover_document() -> dict[str, Any]:
    rows = leftover_rows()
    by_owner = Counter(row["owner"] for row in rows)
    by_disposition = Counter(row["disposition"] for row in rows)
    return {
        "counts": {
            "by_owner": dict(sorted(by_owner.items())),
            "phase_deferred": by_disposition["phase_deferred"],
            "post_1x_scope": by_disposition["post_1x_scope"],
            "total": len(rows),
        },
        "generated_by": generated_by(R0),
        "hanging_unaccounted": [],
        "rows": rows,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "LEFTOVER_LATER_READY",
        "wave_slug": R0,
    }


def schema_document() -> dict[str, Any]:
    return {
        "allowed_1x_dispositions": list(DISPOSITIONS_1X),
        "allowed_correspondence_classes": list(CORRESPONDENCE_CLASSES),
        "allowed_growth_blockers": list(GROWTH_BLOCKERS),
        "allowed_gt6_domains": list(GT6_DOMAINS),
        "allowed_leftover_dispositions": list(LEFTOVER_DISPOSITIONS),
        "allowed_leftover_portfolio_scopes": list(LEFTOVER_SCOPES),
        "frozen": True,
        "generated_by": generated_by(R0),
        "leftover_fields": list(LEFTOVER_FIELDS),
        "row_fields": list(SEED_FIELDS),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SCHEMA_FROZEN",
        "wave_slug": R0,
    }


def inherited_seed_document() -> dict[str, Any]:
    rows = inherited_seed_rows()
    return {
        "generated_by": generated_by(R0),
        "row_count": len(rows),
        "rows": rows,
        "schema_version": 1,
        "source_artifact": t35.relative(JOINT_SEED),
        "source_revision": SOURCE_REVISION,
        "status": "INHERITED_SEED_READY",
        "wave_slug": R0,
    }


def count_ceiling_refs_document() -> dict[str, Any]:
    refs = [dict(row) for row in one_x.BUDGET_REFERENCES]
    if len(refs) != 5:
        raise ValueError(f"count ceiling refs {len(refs)} != 5")
    for row in refs:
        if not (t35.ROOT / row["path"]).is_file():
            raise ValueError(f"count ceiling path missing: {row['path']}")
    return {
        "generated_by": generated_by(R0),
        "refs": refs,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "COUNT_CEILING_REFS_READY",
        "wave_slug": R0,
    }


def common_documents(slug: str, *, evidence: dict[str, Any]) -> dict[str, Any]:
    spec = spec_for(slug)
    status = STATUSES[slug]
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": generated_by(slug),
        "leftover_later_count": 39,
        "partial_family_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": slug,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": generated_by(slug),
        "next_unassigned": spec.next_unassigned,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": slug,
    }
    readiness = {
        "evidence": evidence,
        "generated_by": generated_by(slug),
        "next_unassigned": spec.next_unassigned,
        "note": NOTES[slug],
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": status,
        "unique_active_wave": spec.unique_active_wave,
        "wave_complete": True,
        "wave_slug": slug,
    }
    wave = {
        "cohort": slug.split("/", 1)[-1],
        "depends_on": list(DEPENDS_ON[slug]),
        "generated_by": generated_by(slug),
        "owns_families": 0,
        "program": PROGRAM,
        "schema_version": 1,
        "wave_slug": slug,
    }
    return {
        "census_delta.json": census,
        "readiness.json": readiness,
        "topology.json": topology,
        "wave.json": wave,
    }


def _map_row(
    gt6_domain: str,
    gt6_anchor: str,
    cc_mechanism: str,
    correspondence_class: str,
    growth_blocker: str,
    disposition: str,
    recheck: str,
    **extra: Any,
) -> dict[str, Any]:
    if gt6_domain not in GT6_DOMAINS:
        raise ValueError(f"undeclared gt6_domain {gt6_domain}")
    if correspondence_class not in CORRESPONDENCE_CLASSES:
        raise ValueError(f"undeclared correspondence_class {correspondence_class}")
    if growth_blocker not in GROWTH_BLOCKERS:
        raise ValueError(f"undeclared growth_blocker {growth_blocker}")
    if disposition not in DISPOSITIONS_1X:
        raise ValueError(f"undeclared 1x_disposition {disposition}")
    row: dict[str, Any] = {
        "1x_disposition": disposition,
        "cc_mechanism": cc_mechanism,
        "correspondence_class": correspondence_class,
        "growth_blocker": growth_blocker,
        "gt6_anchor": gt6_anchor,
        "gt6_domain": gt6_domain,
        "recheck_condition": recheck,
    }
    row.update(extra)
    return row


def dump_map_ids() -> set[str]:
    require_dump()
    index = t35.load_json(DUMP_INDEX)
    ids = {
        str(row.get("nameInternal") or "")
        for row in index.get("maps") or []
        if str(row.get("nameInternal") or "").startswith("gt.recipe.")
    }
    if not ids:
        raise ValueError("dump index has no gt.recipe.* maps")
    return ids


def dump_map_exists(name: str) -> bool:
    return (DUMP_MAPS / f"{name}.json").is_file()


def t13c_summaries() -> dict[str, int]:
    document = t35.load_json(T35_EXCLUSION)
    counts = {
        str(row["category"]): int(row["expanded_multiplicity"])
        for row in document.get("category_summaries") or []
    }
    for category, expected in T13C_COUNTS:
        if counts.get(category) != expected:
            raise ValueError(f"T13c {category} {counts.get(category)} != {expected}")
    return counts


def inventory_rows() -> list[dict[str, Any]]:
    require_dump()
    seed = inherited_seed_rows()
    leftover = leftover_rows()
    dump_ids = dump_map_ids()
    claimed_dump: set[str] = set()
    rows: list[dict[str, Any]] = []
    for row in seed:
        copied = dict(row)
        copied["inherited_from_seed"] = True
        rows.append(copied)

    prefix_count = len(t35.load_json(PREFIX_INDEX))
    rows.append(
        _map_row(
            "oredict",
            "gregapi/oredict startup catalog boundary",
            "src/main/java/com/masson/cruciblecraft/material/prefix/MaterialPrefixCatalog.java",
            "equivalent",
            "none",
            "in_1x_closed",
            "Startup-only catalog. Datapacks cannot add prefix rows after bootstrap. Not a missing generator.",
        )
    )
    rows.append(
        _map_row(
            "oredict",
            "gt6_dump/gt6_recipe_dump/oredict",
            f"data/cruciblecraft/material_prefixes/ ({prefix_count} bundled prefix JSON files via index.json)",
            "selected_subset",
            "none",
            "in_1x_closed",
            "CC ships a startup prefix catalog. Dump oredict is the source concept, not a missing-line census.",
            dump_present=DUMP_ROOT.joinpath("oredict").is_dir(),
        )
    )

    if len(CC_PROCESSING_MAPS) != 32:
        raise ValueError("processing map table must be 32")
    for cc_id, dump_names in CC_PROCESSING_MAPS:
        present = all(dump_map_exists(name) for name in dump_names)
        claimed_dump.update(dump_names)
        if present:
            correspondence = "selected_subset"
            blocker = "no_generator"
            recheck = (
                f"CC map cruciblecraft:{cc_id} has dump map(s) "
                + ",".join(dump_names)
                + ". Presence is not a generic dump-to-map generator."
            )
        else:
            correspondence = "identity_only"
            blocker = "none"
            recheck = (
                f"CC map cruciblecraft:{cc_id} is registered; pinned dump has no "
                + ",".join(dump_names)
                + "."
            )
        rows.append(
            _map_row(
                "loader",
                " + ".join(dump_names),
                f"cruciblecraft:{cc_id} via src/main/java/com/masson/cruciblecraft/registry/ModRecipeMaps.java",
                correspondence,
                blocker,
                "in_1x_selected",
                recheck,
                dump_present=present,
            )
        )
    remainder = sorted(
        name
        for name in dump_ids
        if name not in claimed_dump and name not in {dump for _, dump in FUEL_CC_MAPS}
    )
    rows.append(
        _map_row(
            "loader",
            "gt6_dump/gt6_recipe_dump/index.json GT6-only remainder maps",
            "none; see tools/t27_portfolio/recipe_maps.json",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            f"One remainder pointer covering {len(remainder)} dump maps without a CC processing RecipeMap. Not a recipe census.",
            remainder_count=len(remainder),
        )
    )

    tiers = t35.load_json(MACHINE_TIERS)
    variant_count = len(tiers.get("variants") or [])
    auto = bool((tiers.get("namingPolicy") or {}).get("automaticKindTierCompletion"))
    if auto:
        raise ValueError("automaticKindTierCompletion must stay false")
    rows.append(
        _map_row(
            "tileentity",
            "Loader_MultiTileEntities.java selected processing variants",
            f"data/cruciblecraft/machine_tiers.json ({variant_count} explicit variant rows)",
            "selected_subset",
            "none",
            "in_1x_selected",
            "Explicit catalog rows are datapack-addable. automaticKindTierCompletion=false is not no_generator.",
        )
    )
    rows.append(
        _map_row(
            "tileentity",
            "new tier for an existing kind",
            "add an explicit machine_tiers.json row; MachineTierCatalog walks the catalog",
            "selected_subset",
            "none",
            "in_1x_selected",
            "Cartesian kind x material completion stays false. An explicit row is not a missing generator.",
        )
    )
    rows.append(
        _map_row(
            "tileentity",
            "new processing kind / envelope",
            "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java ProcessingMachineSpec / CONFIGURED_MACHINES",
            "capability_missing",
            "hardcoded_spec",
            "not_in_1x",
            "New kind envelopes remain Java specs. Do not open ParameterizedSpec or automaticKindTierCompletion here.",
        )
    )

    cover_ids = {str(row.get("id")) for row in (t35.load_json(COVER_DEFINITIONS).get("definitions") or [])}
    for cover_id in SELECTED_COVERS:
        if cover_id not in cover_ids:
            raise ValueError(f"selected cover missing: {cover_id}")
        rows.append(
            _map_row(
                "cover",
                f"GT6 cover kind corresponding to {cover_id}",
                f"data/cruciblecraft/cover_definitions.json {cover_id}",
                "selected_subset",
                "selected_only",
                "in_1x_selected",
                "Selected cover. Remainder logistics/controller/redstone/detector stay none.",
            )
        )
    if "cruciblecraft:conveyor_fast" not in cover_ids:
        raise ValueError("conveyor_fast missing")
    rows.append(
        _map_row(
            "cover",
            "GT6 conveyor (fast variant)",
            "data/cruciblecraft/cover_definitions.json cruciblecraft:conveyor_fast",
            "selected_subset",
            "selected_only",
            "in_1x_selected",
            "Extra selected cover definition on the conveyor behavior. Not a full logistics net.",
        )
    )
    if not T13_COVER_POLICY.is_file():
        raise ValueError("t13 cover policy missing")
    for anchor, label in (
        ("controller_*", "controller"),
        ("detector_running_*", "detector"),
        ("logistics_*", "logistics"),
        ("controller_redstone / controller_auto_redstone", "redstone"),
    ):
        rows.append(
            _map_row(
                "cover",
                f"GT6 {label} covers ({anchor}) in tools/t13_cover_multiblock_policy.json",
                "none",
                "out_of_scope_historical",
                "selected_only",
                "not_in_1x",
                "Selected covers exist. This remainder is not a forever ban.",
            )
        )

    energy = t35.load_json(ENERGY_CONVERTERS)
    profiles = [str(row["id"]) for row in energy.get("profiles") or []]
    if len(profiles) != 6:
        raise ValueError(f"energy converter profiles {len(profiles)} != 6")
    for profile in profiles:
        rows.append(
            _map_row(
                "tileentity",
                "Loader_MultiTileEntities.java selected energy conversion chain",
                f"src/main/java/com/masson/cruciblecraft/energy/converter/EnergyConverterCatalog.java + energy_converters.json {profile}",
                "selected_subset",
                "selected_only",
                "in_1x_selected",
                "Selected bronze conversion profile. Batteries/transformers/reactors stay none.",
            )
        )
    for cc_id, dump_name in FUEL_CC_MAPS:
        present = dump_map_exists(dump_name)
        rows.append(
            _map_row(
                "loader",
                dump_name,
                f"cruciblecraft:{cc_id} via ModRecipeMaps",
                "selected_subset" if present else "identity_only",
                "selected_only",
                "in_1x_selected",
                "Fuel map on the selected energy chain, not a processing RecipeMap in the 32.",
                dump_present=present,
            )
        )
    for anchor, note in (
        (
            "GT6 batteries",
            "tools/t35_excluded_object_reclaim.json Batteries",
        ),
        (
            "GT6 transformers",
            "Loader_MultiTileEntities.java transformer variants",
        ),
        (
            "GT6 reactors",
            "tools/t35_excluded_object_reclaim.json Reactors",
        ),
    ):
        rows.append(
            _map_row(
                "tileentity",
                anchor,
                "none",
                "out_of_scope_historical",
                "no_owner",
                "not_in_1x",
                f"Energy remainder. Source pointer: {note}. Not next_major unless growth-order assigns it.",
            )
        )

    veins = t35.load_json(ORE_VEINS).get("veins") or []
    if len(veins) != 129:
        raise ValueError(f"T20 ore veins {len(veins)} != 129")
    deposits = t35.load_json(FLUID_DEPOSITS).get("deposits") or []
    if len(deposits) < 2:
        raise ValueError("fluid deposits missing oil/gas")
    scatter = t35.load_json(SURFACE_SCATTER)
    rows.append(
        _map_row(
            "worldgen",
            "GT6 ore vein identities",
            "data/cruciblecraft/worldgen_catalog/ore_veins.json (T20 129 identities)",
            "selected_subset",
            "none",
            "in_1x_selected",
            "Selected ore-vein catalog. Does not claim GT6-complete worldgen.",
        )
    )
    rows.append(
        _map_row(
            "worldgen",
            "GT6 fluid springs / oil and gas deposits",
            "data/cruciblecraft/worldgen_catalog/fluid_deposits.json",
            "selected_subset",
            "none",
            "in_1x_selected",
            "Selected oil/gas deposits. Dump fluid springs remain a larger source set.",
        )
    )
    rows.append(
        _map_row(
            "worldgen",
            "GT6 WorldgenRocks / surface scatter",
            f"data/cruciblecraft/worldgen_catalog/surface_scatter.json ({scatter.get('id')})",
            "selected_subset",
            "none",
            "in_1x_selected",
            "Selected pebble/surface scatter. Not GT6-complete worldgen.",
        )
    )
    if not WORLDGEN_OTHER.is_file():
        raise ValueError("dump worldgen/other_features.json missing")
    for anchor, mechanism_note in (
        (
            "gt6_dump/gt6_recipe_dump/worldgen/other_features.json WorldgenTree*",
            "trees",
        ),
        (
            "gt6_dump/gt6_recipe_dump/worldgen/other_features.json WorldgenDungeonGT",
            "dungeons",
        ),
        (
            "gt6_dump/gt6_recipe_dump/worldgen/other_features.json WorldgenPlanetRocks / moon.rocks / mars.rocks",
            "planets",
        ),
        (
            "gt6_dump/gt6_recipe_dump/worldgen/other_features.json WorldgenCenterBiomes / center.*",
            "center",
        ),
    ):
        rows.append(
            _map_row(
                "worldgen",
                anchor,
                "none",
                "out_of_scope_historical",
                "no_owner",
                "not_in_1x",
                f"Non-ore worldgen {mechanism_note}. Anchor points at the pinned dump feature list.",
            )
        )

    rows.append(
        _map_row(
            "loader",
            "gregtech.loaders.c.Loader_Recipes_Vanilla",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            "Vanilla replace loader. Not a 1.x failure and not a forever ban.",
        )
    )
    rows.append(
        _map_row(
            "loader",
            "gregtech.loaders.c.Loader_Recipes_Replace + gregtech.asm.transformers.minecraft.Replacements",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            "Vanilla replacement transformer. CC mechanism is none.",
        )
    )
    rows.append(
        _map_row(
            "worldgen",
            "GT6 crops / plants (dump worldgen plant.* + gt.recipe.squeezer)",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            "Crops/food/bees remain none. Anchor is the GT6 plant/crop source, not 'later'.",
        )
    )
    rows.append(
        _map_row(
            "worldgen",
            "GT6 food maps (gt.recipe.juicer / gt.recipe.fermenter)",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            "Food processing maps are none in CC.",
        )
    )
    rows.append(
        _map_row(
            "worldgen",
            "gt.recipe.bumblequeen / gt.recipe.bumblelyzer + WorldgenHives",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            "Bee/hive source maps and worldgen hives. CC mechanism is none.",
        )
    )
    rows.append(
        _map_row(
            "tileentity",
            "GT6 building-block item/block identities",
            "none",
            "identity_only",
            "no_owner",
            "not_in_1x",
            "Building-block identity without CC behavior.",
        )
    )
    rows.append(
        _map_row(
            "tileentity",
            "GT6 building-block behaviors (hardness, multiblock parts, decorative machines)",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            "Building-block behavior is none. Identity-only is a different row.",
        )
    )

    summaries = t13c_summaries()
    for category, expected in T13C_COUNTS:
        rows.append(
            _map_row(
                "tileentity",
                f"T13c exclusion {category}",
                "none",
                "capability_missing",
                "no_owner",
                "not_in_1x",
                f"tools/t35_excluded_object_reclaim.json {category} expanded_multiplicity={summaries[category]}. Never entered the 1.x denominator.",
            )
        )

    def _leftover_group(
        owners: tuple[str, ...],
        gt6_anchor: str,
        correspondence: str,
        blocker: str,
        recheck: str,
    ) -> None:
        matched = [row for row in leftover if row["owner"] in owners]
        if not matched:
            raise ValueError(f"leftover group empty: {owners}")
        rows.append(
            _map_row(
                "loader",
                gt6_anchor,
                "none",
                correspondence,
                blocker,
                "not_in_1x",
                recheck,
                accounted_by=[row["family_id"] for row in matched],
            )
        )

    _leftover_group(
        ("later:assembler_combinatorial",),
        "gt.recipe.assembler combinatorial remainder",
        "capability_missing",
        "no_generator",
        "later:assembler_combinatorial expressed_by t41. Must appear in growth-order.",
    )
    _leftover_group(
        ("later:electrolyzer_combinatorial",),
        "gt.recipe.electrolyzer combinatorial remainder",
        "capability_missing",
        "no_generator",
        "later:electrolyzer_combinatorial expressed_by t40. Must appear in growth-order.",
    )
    _leftover_group(
        ("post_1x:nuclear",),
        "gt.recipe.centrifuge nuclear remainder",
        "post_1x",
        "no_owner",
        "post_1x:nuclear expressed_by t39. Independent unless growth-order assigns nuclear next.",
    )
    _leftover_group(
        ("post_1.x/smelter_recovery_edge",),
        "gt.recipe.smelter recovery edge",
        "post_1x",
        "no_owner",
        "post-1.x smelter recovery edge. Still independent.",
    )
    _leftover_group(
        ("post_1.x/ordinary_autoclave_processing",),
        "gt.recipe.autoclave ordinary processing remainder",
        "post_1x",
        "no_owner",
        "post-1.x ordinary autoclave processing. Still independent.",
    )
    _leftover_group(
        (
            "post_1.x/centrifuge_execution_envelope",
            "post_1.x/cross_mod_pahoehoe",
        ),
        "gt.recipe.centrifuge#0010 execution envelope + gt.recipe.centrifuge#0207 pahoehoe",
        "post_1x",
        "no_owner",
        "recycling/non-recycling-scope. Still independent.",
    )

    for label, path in COUNT_CEILING_POINTS:
        rows.append(
            _map_row(
                "loader",
                label,
                path + " (still referenced; load interpretation A does not raise the constant)",
                "capability_missing",
                "count_ceiling",
                "in_1x_closed",
                "Count telemetry is not a hard top. This row only registers the live code point.",
            )
        )

    accounted = {
        family_id
        for row in rows
        for family_id in (row.get("accounted_by") or [])
    }
    leftover_ids = {row["family_id"] for row in leftover}
    missing = sorted(leftover_ids - accounted)
    extra = sorted(accounted - leftover_ids)
    if missing or extra:
        raise ValueError(
            "leftover accounting drifted missing="
            + ",".join(missing[:8])
            + " extra="
            + ",".join(extra[:8])
        )
    return rows


def coverage_document(rows: list[dict[str, Any]]) -> dict[str, Any]:
    seed_rows = [row for row in rows if row.get("inherited_from_seed")]
    leftover = leftover_rows()
    accounted = {
        family_id
        for row in rows
        for family_id in (row.get("accounted_by") or [])
    }
    expanded = sorted({row["gt6_domain"] for row in rows if not row.get("inherited_from_seed")})
    present = dump_present()
    if not present:
        raise ValueError("dump_present is false; refuse SOURCE_CAPABILITY_INVENTORY_READY")
    if len(seed_rows) != 22:
        raise ValueError("rows_from_seed != 22")
    if len(accounted) != 39:
        raise ValueError(f"leftover_later_accounted {len(accounted)} != 39")
    return {
        "domains_expanded": expanded,
        "dump_present": present,
        "generated_by": generated_by(INVENTORY),
        "leftover_later_accounted": len(accounted),
        "row_count": len(rows),
        "rows_from_seed": len(seed_rows),
        "rows_new": len(rows) - len(seed_rows),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "COVERAGE_READY",
        "unmapped_gt6_domains": [],
        "wave_slug": INVENTORY,
    }


def capability_map_document(slug: str, rows: list[dict[str, Any]]) -> dict[str, Any]:
    return {
        "generated_by": generated_by(slug),
        "row_count": len(rows),
        "rows": rows,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "CAPABILITY_MAP_READY",
        "wave_slug": slug,
    }


def choose_next_major(rows: list[dict[str, Any]]) -> str:
    blockers = Counter(
        str(row["growth_blocker"])
        for row in rows
        if row.get("growth_blocker") not in (None, "none")
    )
    if not blockers:
        raise ValueError("inventory has no growth blockers")
    if blockers.get("no_generator", 0) <= 0:
        raise ValueError("expected no_generator to remain on the map")
    return NEXT_MAJOR_SLUG


def leftover_by_owner(owner: str) -> list[str]:
    return [row["family_id"] for row in leftover_rows() if row["owner"] == owner]


def growth_order_document(slug: str, rows: list[dict[str, Any]]) -> dict[str, Any]:
    next_major = choose_next_major(rows)
    later_star = {
        "later:assembler_combinatorial": {
            "disposition": "merge_into_next_major",
            "families": leftover_by_owner("later:assembler_combinatorial"),
            "target": next_major,
        },
        "later:electrolyzer_combinatorial": {
            "disposition": "merge_into_next_major",
            "families": leftover_by_owner("later:electrolyzer_combinatorial"),
            "target": next_major,
        },
        "post_1x:nuclear": {
            "disposition": "remain_independent_not_next_major",
            "families": leftover_by_owner("post_1x:nuclear"),
            "nuclear_started": False,
        },
        "post_1x_recycling_scope": {
            "count": 28,
            "disposition": "remain_independent_not_next_major",
            "owners": {
                "post_1.x/centrifuge_execution_envelope": leftover_by_owner(
                    "post_1.x/centrifuge_execution_envelope"
                ),
                "post_1.x/cross_mod_pahoehoe": leftover_by_owner(
                    "post_1.x/cross_mod_pahoehoe"
                ),
                "post_1.x/ordinary_autoclave_processing": leftover_by_owner(
                    "post_1.x/ordinary_autoclave_processing"
                ),
                "post_1.x/smelter_recovery_edge": leftover_by_owner(
                    "post_1.x/smelter_recovery_edge"
                ),
            },
        },
    }
    assembler = later_star["later:assembler_combinatorial"]["families"]
    electro = later_star["later:electrolyzer_combinatorial"]["families"]
    if len(assembler) + len(electro) != 4:
        raise ValueError("later:* combinatorial disposition must cover 4 families")
    tracks = [
        {
            "does_not_implement": "nuclear, vanilla replace, crops/food/bees, cover net, new GT6 domains, production lock",
            "owns": "generic dump-to-arbitrary-map generator replacing per-wave builders; later:assembler_combinatorial and later:electrolyzer_combinatorial",
            "slug": next_major,
        },
        {
            "does_not_implement": "recipe content, nuclear, vanilla replace",
            "owns": "count-ceiling interpretation follow-through and ProcessingMachineSpec datafication",
            "slug": "portfolio/count-ceiling-kind-envelope",
        },
        {
            "does_not_implement": "new RecipeMaps, nuclear, vanilla replace",
            "owns": "bounded logistics cover net and T13c exclusion reclaim on existing mechanisms",
            "slug": "portfolio/existing-mechanism-bounded-domains",
        },
        {
            "does_not_implement": "growth generator, nuclear unless separately assigned",
            "owns": "vanilla replace, non-ore worldgen, crops/food/bees",
            "slug": "portfolio/large-content-branches",
        },
        {
            "does_not_implement": "anything until assigned next_major",
            "owns": "post_1x:nuclear centrifuge families and Track C",
            "slug": "portfolio/nuclear",
        },
    ]
    if nuclear_started():
        raise ValueError("nuclear Track C must stay started=false")
    return {
        "generated_by": generated_by(slug),
        "later_star_disposition": later_star,
        "next_major": next_major,
        "nuclear_started": False,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "GROWTH_ORDER_READY",
        "tracks": tracks,
        "wave_slug": slug,
    }


def build_r0_documents() -> dict[str, Any]:
    errors = require_predecessor(R0)
    if errors:
        raise ValueError("; ".join(errors))
    if nuclear_started():
        raise ValueError("nuclear Track C must stay started=false")
    leftover = leftover_document()
    seed = inherited_seed_document()
    evidence = {
        "completion_delta": 0,
        "count_ceiling_ref_count": 5,
        "growth_order": False,
        "inventory_expansion": False,
        "leftover_later_count": leftover["counts"]["total"],
        "nuclear_track_c_started": False,
        "owns_families": 0,
        "recipe_files_generated": False,
        "schema_frozen": True,
        "seed_row_count": seed["row_count"],
        "source_capability_map_py": False,
    }
    documents = common_documents(R0, evidence=evidence)
    documents["count_ceiling_refs.json"] = count_ceiling_refs_document()
    documents["inherited_seed.json"] = seed
    documents["leftover_later.json"] = leftover
    documents["schema.json"] = schema_document()
    return documents


def build_inventory_documents() -> dict[str, Any]:
    errors = require_predecessor(INVENTORY)
    if errors:
        raise ValueError("; ".join(errors))
    if nuclear_started():
        raise ValueError("nuclear Track C must stay started=false")
    require_dump()
    rows = inventory_rows()
    coverage = coverage_document(rows)
    evidence = {
        "completion_delta": 0,
        "dump_present": True,
        "leftover_later_accounted": coverage["leftover_later_accounted"],
        "nuclear_track_c_started": False,
        "owns_families": 0,
        "recipe_files_generated": False,
        "row_count": coverage["row_count"],
        "rows_from_seed": coverage["rows_from_seed"],
        "rows_new": coverage["rows_new"],
    }
    documents = common_documents(INVENTORY, evidence=evidence)
    documents["capability_map.json"] = capability_map_document(INVENTORY, rows)
    documents["coverage.json"] = coverage
    return documents


def build_growth_documents() -> dict[str, Any]:
    errors = require_predecessor(GROWTH)
    if errors:
        raise ValueError("; ".join(errors))
    if nuclear_started():
        raise ValueError("nuclear Track C must stay started=false")
    rows = inventory_rows()
    order = growth_order_document(GROWTH, rows)
    evidence = {
        "completion_delta": 0,
        "later_star_explicit": True,
        "next_major": order["next_major"],
        "nuclear_started": False,
        "owns_families": 0,
        "production_lock_for_next_major": None,
        "recipe_files_generated": False,
    }
    documents = common_documents(GROWTH, evidence=evidence)
    documents["growth_order.json"] = order
    return documents


def build_program_documents() -> dict[str, Any]:
    errors = require_predecessor(PROGRAM)
    if errors:
        raise ValueError("; ".join(errors))
    if nuclear_started():
        raise ValueError("nuclear Track C must stay started=false")
    rows = inventory_rows()
    coverage = coverage_document(rows)
    order = growth_order_document(PROGRAM, rows)
    if order["next_major"] != NEXT_MAJOR_SLUG:
        raise ValueError("next_major must come from growth-order derivation")
    if order["nuclear_started"]:
        raise ValueError("nuclear_started must stay false")
    evidence = {
        "capability_map_current": True,
        "completion_delta": 0,
        "dump_present": coverage["dump_present"],
        "growth_order_current": True,
        "leftover_later_accounted": coverage["leftover_later_accounted"],
        "next_major": order["next_major"],
        "nuclear_track_c_started": False,
        "owns_families": 0,
        "production_lock_for_next_major": None,
        "recipe_files_generated": False,
        "seed_row_count": 22,
    }
    documents = common_documents(PROGRAM, evidence=evidence)
    documents["capability_map.json"] = capability_map_document(PROGRAM, rows)
    documents["growth_order.json"] = order
    return documents


BUILDERS = {
    R0: build_r0_documents,
    INVENTORY: build_inventory_documents,
    GROWTH: build_growth_documents,
    PROGRAM: build_program_documents,
}


def write_seal(slug: str) -> dict[str, Any]:
    root = wave_dir(slug)
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
    seal = {
        "card_id": slug,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": t35.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": t35.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{generated_by(slug)} --write",
        "hashes": hashes,
        "note": t35.load_json(root / "readiness.json").get("note"),
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": 0,
        "relation_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": slug,
        "source_revision": SOURCE_REVISION,
        "status": "SEALED",
    }
    t35.write_stable(root / "closeout_seal.json", seal)
    return seal


def write_artifacts(slug: str) -> dict[str, Any]:
    documents = BUILDERS[slug]()
    root = wave_dir(slug)
    root.mkdir(parents=True, exist_ok=True)
    for name, document in documents.items():
        t35.write_stable(root / name, document)
    write_seal(slug)
    spec = spec_for(slug)
    return {
        "status": STATUSES[slug],
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": slug,
    }


def check_artifacts(slug: str) -> list[str]:
    errors = require_registered(slug)
    if errors:
        return errors
    spec = spec_for(slug)
    if spec.owns_families != 0:
        errors.append(f"{slug} owns_families must be 0")
    if spec.production_lock is not None:
        errors.append(f"{slug} must not carry a production lock")
    if slug == PROGRAM:
        if spec.unique_active_wave is not None:
            errors.append("program unique_active_wave must be null")
        if not spec.next_unassigned:
            errors.append("program next_unassigned must be true")
    root = wave_dir(slug)
    if not (root / "readiness.json").is_file():
        return errors + [f"{slug} artifacts are missing"]
    try:
        live = BUILDERS[slug]()
    except ValueError as error:
        return errors + [str(error)]
    for name, document in live.items():
        committed = t35.load_json(root / name)
        drift = t35.first_json_diff(document, committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = t35.load_json(root / "readiness.json")
    if readiness.get("status") != STATUSES[slug]:
        errors.append(f"{slug} status drifted")
    if readiness.get("unique_active_wave") != spec.unique_active_wave:
        errors.append(f"{slug} unique_active_wave drifted")
    if nuclear_started():
        errors.append("nuclear Track C started must stay false")
    if int(readiness.get("evidence", {}).get("completion_delta", 1)) != 0:
        errors.append(f"{slug} completion_delta must be 0")
    if readiness.get("evidence", {}).get("recipe_files_generated"):
        errors.append(f"{slug} must not generate recipes")
    errors.extend(closeout_seal.check_wave_seal(slug))
    return errors


def main_for(slug: str, argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=f"Write or check {slug}.")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            print(json.dumps(write_artifacts(slug), sort_keys=True))
            return 0
        errors = check_artifacts(slug)
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{slug} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"{slug} failed: {error}", file=sys.stderr)
        return 1
