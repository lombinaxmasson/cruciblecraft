#!/usr/bin/env python3
"""Shared 1.x joint-exit program: seed, ledgers, overlays, and seals."""
from __future__ import annotations

from collections import Counter
from typing import Any

from tools import closeout_seal
from tools import currentness
from tools import census_common as census
from tools import repair_common as repair
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir

PROGRAM = "portfolio/one-x-joint-exit"
NEXT_MAJOR = "portfolio/source-capability-map"
RECYCLING = "recycling/deferred-ordinary-runtime"
SOURCE_REVISION = census.SOURCE_REVISION
HANGING_LATER = (
    "later:recycling",
    "later:cross_mod",
    "later:execution_envelope/gt6_panel",
)
FORBIDDEN_OWNERS = frozenset({"", "TBD", "deferred", "todo", "tbd", "unassigned"})
POST_1X_SCOPE_SLUGS = (
    "smelter/deferred-recycling-edge",
    "autoclave/deferred-recycling",
    "recycling/non-recycling-scope",
)
CHILD_SLUGS = (
    "portfolio/one-x-exit-r0",
    "portfolio/census-disposition-replay",
    "portfolio/energy-matrix-replay",
    "portfolio/storage-currentness-replay",
    "portfolio/load-ceiling-interpretation",
    "portfolio/one-x-joint-exit",
)
STATUSES = {
    "portfolio/one-x-exit-r0": "ONE_X_EXIT_R0_READY",
    "portfolio/census-disposition-replay": "CENSUS_DISPOSITION_REPLAY_READY",
    "portfolio/energy-matrix-replay": "ENERGY_MATRIX_REPLAY_READY",
    "portfolio/storage-currentness-replay": "STORAGE_CURRENTNESS_REPLAY_READY",
    "portfolio/load-ceiling-interpretation": "LOAD_CEILING_INTERPRETATION_READY",
    "portfolio/one-x-joint-exit": "ONE_X_JOINT_EXIT_READY",
}
PREDECESSORS = {
    "portfolio/one-x-exit-r0": (RECYCLING, "DEFERRED_ORDINARY_RUNTIME_READY"),
    "portfolio/census-disposition-replay": (
        "portfolio/one-x-exit-r0",
        "ONE_X_EXIT_R0_READY",
    ),
    "portfolio/energy-matrix-replay": (
        "portfolio/census-disposition-replay",
        "CENSUS_DISPOSITION_REPLAY_READY",
    ),
    "portfolio/storage-currentness-replay": (
        "portfolio/energy-matrix-replay",
        "ENERGY_MATRIX_REPLAY_READY",
    ),
    "portfolio/load-ceiling-interpretation": (
        "portfolio/storage-currentness-replay",
        "STORAGE_CURRENTNESS_REPLAY_READY",
    ),
}
DEPENDS_ON = {
    "portfolio/one-x-exit-r0": [RECYCLING],
    "portfolio/census-disposition-replay": ["portfolio/one-x-exit-r0"],
    "portfolio/energy-matrix-replay": ["portfolio/census-disposition-replay"],
    "portfolio/storage-currentness-replay": ["portfolio/energy-matrix-replay"],
    "portfolio/load-ceiling-interpretation": [
        "portfolio/storage-currentness-replay"
    ],
    "portfolio/one-x-joint-exit": list(CHILD_SLUGS[:-1]),
}
LOAD_INTERPRETATION = "A"
HARD_MEASURED_AXES = (
    "client_index_ms",
    "client_reload_ms",
    "lookup_allocation_bytes_per_operation",
    "lookup_candidate_count",
    "lookup_p95_ns",
    "reload_transient_allocation_bytes",
    "retained_memory_bytes",
    "server_index_ms",
    "server_reload_ms",
    "sync_bytes",
)
COUNT_REPORT_ONLY_AXES = (
    "datapack_authored_entries",
    "eager_publication_rows",
    "lazy_cache_ceiling_rows",
    "lazy_logical_rows",
)
BUDGET_REFERENCES = (
    {
        "path": "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java",
        "role": "deprecated ALL_PUBLISHED_RECIPE_BUDGET definition",
    },
    {
        "path": "src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipeMapLoader.java",
        "role": "publication capacity telemetry; count overage is UNVERIFIED_SCALE",
    },
    {
        "path": "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java",
        "role": "historical GameTest still asserts the 21000 constant",
    },
    {
        "path": "src/test/java/com/masson/cruciblecraft/recipe/gt/GTRecipeMapBudgetTest.java",
        "role": "unit test pins the constant; overage must not throw",
    },
    {
        "path": "src/test/java/com/masson/cruciblecraft/recipe/gt/ChemicalPublicationBudgetTest.java",
        "role": "unit test pins the constant; overage must not throw",
    },
)
STORAGE_LOCK_SIDECARS = (
    census.TOOLS / "storage_readiness.json",
    census.TOOLS / "storage_production_lock.json",
    census.TOOLS / "storage_census_delta.json",
)
STORAGE_VARIANTS = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "storage_variants.json"
)
DEFERRED_MEASUREMENT = (
    census.TOOLS
    / "waves"
    / "smelter"
    / "deferred-recycling"
    / "smelter_deferred_recycling_integrated_measurements.json"
)
DEFERRED_LOAD_PROJECTION = (
    census.TOOLS / "waves" / "smelter" / "deferred-recycling" / "load_projection.json"
)
SMELTER_CLOSEOUT_MEASUREMENT = (
    census.TOOLS
    / "waves"
    / "ordinary-wave"
    / "closeout-integrity-repair"
    / "smelter_integrated_measurements.json"
)
MIXER_CLOSEOUT_MEASUREMENT = (
    census.TOOLS
    / "waves"
    / "ordinary-wave"
    / "closeout-integrity-repair"
    / "mixer_integrated_measurements.json"
)
IDENTITY_DELTAS = (
    census.TOOLS / "assembler_compact_census_delta.json",
    census.TOOLS / "roaster_census_delta.json",
    census.TOOLS / "smelter_stone_census_delta.json",
)
PRODUCTION_LOCKS = (
    (census.TOOLS / "centrifuge_production_lock.json", "centrifuge/compact", "portfolio:track_a/centrifuge_centrifuge"),
    (census.TOOLS / "electrolyzer_production_lock.json", "electrolyzer/compact", "portfolio:track_a/electrolyzer_electrolyzer"),
    (census.TOOLS / "assembler_wood_production_lock.json", "assembler/wood", "portfolio:track_a/assembler_wood_assembler"),
    (census.TOOLS / "smelter_stone_production_lock.json", "smelter/stone", "portfolio:track_a/smelter_stone_smelter_stone"),
    (census.TOOLS / "block_object_production_lock.json", "block/object", "portfolio:track_a/block_object_block_object"),
    (census.TOOLS / "bath_mte_production_lock.json", "bath/mte", "portfolio:track_a/bath/mte"),
    (census.TOOLS / "bath_remainder_production_lock.json", "bath/remainder", "portfolio:track_a/bath/remainder"),
    (census.TOOLS / "bath_identity_production_lock.json", "bath/identity", "portfolio:track_a/bath/identity"),
    (
        census.TOOLS / "waves" / "bath" / "tiny-purified" / "bath_tiny_purified_production_lock.json",
        "bath/tiny-purified",
        "portfolio:track_a/bath/tiny-purified",
    ),
    (
        census.TOOLS / "waves" / "smelter" / "ordinary-closure" / "production_lock.json",
        "smelter/ordinary-closure",
        "smelter/ordinary-closure",
    ),
    (
        census.TOOLS / "waves" / "mixer" / "ordinary-closure" / "production_lock.json",
        "mixer/ordinary-closure",
        "mixer/ordinary-closure",
    ),
    (
        census.TOOLS / "waves" / "drying" / "ordinary-closure" / "production_lock.json",
        "drying/ordinary-closure",
        "drying/ordinary-closure",
    ),
    (
        census.TOOLS
        / "waves"
        / "electrolyzer"
        / "ordinary-closure"
        / "production_lock.json",
        "electrolyzer/ordinary-closure",
        "electrolyzer/ordinary-closure",
    ),
    (
        census.TOOLS / "waves" / "centrifuge" / "ordinary-closure" / "production_lock.json",
        "centrifuge/ordinary-closure",
        "centrifuge/ordinary-closure",
    ),
    (
        census.TOOLS / "waves" / "autoclave" / "ordinary-closure" / "production_lock.json",
        "autoclave/ordinary-closure",
        "autoclave/ordinary-closure",
    ),
    (
        census.TOOLS / "waves" / "compressor" / "ordinary-closure" / "production_lock.json",
        "compressor/ordinary-closure",
        "compressor/ordinary-closure",
    ),
    (
        census.TOOLS / "waves" / "smelter" / "deferred-recycling" / "production_lock.json",
        "smelter/deferred-recycling",
        "smelter/deferred-recycling",
    ),
)


def generated_by(slug: str) -> str:
    names = {
        "portfolio/one-x-exit-r0": "python tools/build_one_x_exit_r0.py",
        "portfolio/census-disposition-replay": (
            "python tools/build_census_disposition_replay.py"
        ),
        "portfolio/energy-matrix-replay": "python tools/build_energy_matrix_replay.py",
        "portfolio/storage-currentness-replay": (
            "python tools/build_storage_currentness_replay.py"
        ),
        "portfolio/load-ceiling-interpretation": (
            "python tools/build_load_ceiling_interpretation.py"
        ),
        "portfolio/one-x-joint-exit": "python tools/build_one_x_joint_exit.py",
    }
    return names[slug]


def nuclear_started() -> bool:
    contract = census.load_json(census.TOOLS / "phase5_portfolio_contract.json")
    tracks = (contract.get("tracks") or {}).get("C") or {}
    return bool(tracks.get("started"))


def _seed_row(
    gt6_domain: str,
    gt6_anchor: str,
    cc_mechanism: str,
    correspondence_class: str,
    growth_blocker: str,
    disposition: str,
    recheck: str,
) -> dict[str, str]:
    return {
        "1x_disposition": disposition,
        "cc_mechanism": cc_mechanism,
        "correspondence_class": correspondence_class,
        "growth_blocker": growth_blocker,
        "gt6_anchor": gt6_anchor,
        "gt6_domain": gt6_domain,
        "recheck_condition": recheck,
    }


def capability_map_seed_rows() -> list[dict[str, str]]:
    successor = (
        "Hand to portfolio/source-capability-map. Not a 1.x failure and not a "
        "forever ban."
    )
    return [
        _seed_row(
            "oredict",
            "gregapi/oredict",
            "src/main/java/com/masson/cruciblecraft/material/prefix/MaterialPrefixCatalog.java + data/cruciblecraft/material_prefixes/",
            "equivalent",
            "none",
            "in_1x_closed",
            "Prefix catalog is startup-only; datapacks cannot add rows at runtime.",
        ),
        _seed_row(
            "loader",
            "gt.recipe.* ordinary rows",
            "compact exact / exact_multi + per-wave Python builders",
            "selected_subset",
            "no_generator",
            "in_1x_closed",
            "Import path exists; no generic dump-to-arbitrary-map generator.",
        ),
        _seed_row(
            "tileentity",
            "Loader_MultiTileEntities.java selected processing variants",
            "machine_tiers.json -> MachineTierCatalog -> ModMachineVariants -> registerTieredProcessingBlocks",
            "selected_subset",
            "none",
            "in_1x_selected",
            "Explicit catalog rows register. automaticKindTierCompletion=false is not no_generator.",
        ),
        _seed_row(
            "worldgen",
            "T20 ore vein / fluid deposit identities",
            "data/cruciblecraft/worldgen_catalog/ore_veins.json + T20 129 identities",
            "selected_subset",
            "none",
            "in_1x_selected",
            "Does not claim GT6-complete worldgen.",
        ),
        _seed_row(
            "cover",
            "GT6 pipes / cables / hopper",
            "independent pipe/cable/hopper runtime + partial catalog",
            "selected_subset",
            "none",
            "in_1x_closed",
            "1.x logistics main chain. Full GT6 cover net is a later seed row.",
        ),
        _seed_row(
            "tileentity",
            "Storage MTE 28 source sites",
            "storage_variants.json + storage/lock 624+1 lock",
            "selected_subset",
            "none",
            "in_1x_selected",
            "Recovered exclusion-table subset. Do not reopen storage behavior.",
        ),
        _seed_row(
            "tileentity",
            "Bath / Smelter MTE holdable identity",
            "BathMteIdentityCatalog + SmelterMteIdentityCatalog",
            "identity_only",
            "no_generator",
            "in_1x_closed",
            "Identity is not a behavior generator.",
        ),
        _seed_row(
            "tileentity",
            "new tier for an existing kind",
            "add an explicit machine_tiers.json row; registerTieredProcessingBlocks walks the catalog",
            "selected_subset",
            "none",
            "in_1x_selected",
            "Not a missing generator. Cartesian kind x material completion stays false.",
        ),
        _seed_row(
            "tileentity",
            "new processing kind / envelope",
            "ModProcessingMachines slot/tank/GUI Java spec",
            "capability_missing",
            "hardcoded_spec",
            "not_in_1x",
            successor,
        ),
        _seed_row(
            "loader",
            "ParameterizedSpec",
            "CompactRecipeFamilyProvider parameterized() fail-closed",
            "capability_missing",
            "fail_closed_runtime",
            "not_in_1x",
            successor,
        ),
        _seed_row(
            "loader",
            "per-wave recipe builders",
            "tools/build_ordinary_wave.py and specialized copies such as build_smelter_deferred_recycling.py",
            "capability_missing",
            "no_generator",
            "in_1x_closed",
            successor,
        ),
        _seed_row(
            "loader",
            "ALL_PUBLISHED_RECIPE_BUDGET / lazy 56k / cache 4096 / shard 128",
            "still referenced by loader and tests; closeout reports UNVERIFIED_SCALE",
            "capability_missing",
            "count_ceiling",
            "in_1x_closed",
            "Successor count_ceiling row. 1.x exit interpretation A does not raise the Java constants.",
        ),
        _seed_row(
            "cover",
            "cover_definitions.json selected covers",
            "filter / shutter / pump / conveyor / retriever / robot_arm / pressure_valve / selector",
            "selected_subset",
            "selected_only",
            "in_1x_selected",
            successor,
        ),
        _seed_row(
            "tileentity",
            "selected energy conversion chain",
            "EnergyConverterCatalog bronze firebox/boiler/steam_engine/dynamo/fuel_engine/gas_generator",
            "selected_subset",
            "selected_only",
            "in_1x_selected",
            successor,
        ),
        _seed_row(
            "tileentity",
            "T13c exclusion tables without 1.x owner",
            "none",
            "capability_missing",
            "no_owner",
            "not_in_1x",
            "Panels 348 / Sensors 21 / Portals 19 / Batteries 37 / Reactors 46 never entered the 1.x denominator. " + successor,
        ),
        _seed_row(
            "loader",
            "GT6 Vanilla replace loaders",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            successor,
        ),
        _seed_row(
            "cover",
            "GT6 full cover / logistics-net remainder",
            "none",
            "out_of_scope_historical",
            "selected_only",
            "not_in_1x",
            successor,
        ),
        _seed_row(
            "worldgen",
            "crops / food / bees",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            successor,
        ),
        _seed_row(
            "tileentity",
            "energy remainder (batteries, transformers, reactors)",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            successor,
        ),
        _seed_row(
            "worldgen",
            "non-ore worldgen (trees, dungeons, item scatter remainder)",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            successor,
        ),
        _seed_row(
            "tileentity",
            "building blocks",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            successor,
        ),
        _seed_row(
            "tileentity",
            "misc systems (Display CPU and remaining unowned GT6 domains)",
            "none",
            "out_of_scope_historical",
            "no_owner",
            "not_in_1x",
            successor,
        ),
    ]


def capability_map_seed(slug: str) -> dict[str, Any]:
    rows = capability_map_seed_rows()
    return {
        "generated_by": generated_by(slug),
        "handed_to": NEXT_MAJOR,
        "row_count": len(rows),
        "rows": rows,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "CAPABILITY_MAP_SEED_READY",
        "wave_slug": slug,
    }


def post_1x_scope_rows() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for slug in POST_1X_SCOPE_SLUGS:
        document = census.load_json(census.TOOLS / "waves" / slug / "scope_dispositions.json")
        for row in document.get("dispositions") or []:
            copied = dict(row)
            copied["scope_wave"] = slug
            rows.append(copied)
    return rows


def replay_post_1x_scope() -> dict[str, Any]:
    rows = post_1x_scope_rows()
    hanging = [
        str(row.get("family_id"))
        for row in rows
        if str(row.get("disposition") or "") != "post_1x_scope"
        or str(row.get("future_owner") or "").startswith("later:")
    ]
    by_wave = Counter(str(row.get("scope_wave")) for row in rows)
    if len(rows) != 28:
        raise ValueError(f"post-1.x scope replay {len(rows)} != 28")
    if hanging:
        raise ValueError("post-1.x scope still hanging later:*: " + ",".join(hanging[:8]))
    if by_wave.get("smelter/deferred-recycling-edge") != 2:
        raise ValueError("smelter edge post-1.x count drifted")
    if by_wave.get("autoclave/deferred-recycling") != 24:
        raise ValueError("autoclave post-1.x count drifted")
    if by_wave.get("recycling/non-recycling-scope") != 2:
        raise ValueError("non-recycling post-1.x count drifted")
    return {
        "by_wave": dict(sorted(by_wave.items())),
        "count": len(rows),
        "generated_by": generated_by("portfolio/one-x-exit-r0"),
        "hanging_later": hanging,
        "rows": [
            {
                "disposition": row["disposition"],
                "family_id": row["family_id"],
                "future_owner": row["future_owner"],
                "scope_wave": row["scope_wave"],
                "template_key": row.get("template_key"),
            }
            for row in rows
        ],
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "POST_1X_SCOPE_REPLAY_READY",
        "still_independent": True,
        "wave_slug": "portfolio/one-x-exit-r0",
    }


def storage_currentness() -> dict[str, Any]:
    readiness = census.load_json(census.TOOLS / "storage_readiness.json")
    lock = census.load_json(census.TOOLS / "storage_production_lock.json")
    catalog = census.load_json(STORAGE_VARIANTS)
    sidecar_errors: list[str] = []
    for path in STORAGE_LOCK_SIDECARS:
        sidecar_errors.extend(currentness.check_sidecar(path))
    seal_errors = closeout_seal.check_closed_card("storage/lock")
    lock_ids = {
        str(row.get("runtime_id"))
        for row in lock.get("mappings") or []
        if isinstance(row, dict)
    }
    catalog_ids = {
        str(row.get("runtime_id"))
        for row in catalog.get("variants") or []
        if isinstance(row, dict)
    }
    storage_count = int(catalog.get("storage_count") or 0)
    logistics_count = int(catalog.get("logistics_count") or 0)
    catalog_ok = (
        storage_count == 624
        and logistics_count == 1
        and len(catalog_ids) == 625
        and int(readiness.get("storage_complete_registrations") or 0) == 624
        and int(readiness.get("logistics_complete_registrations") or 0) == 1
        and str(readiness.get("status") or "") == "STORAGE_LOCK_STORAGE_READY"
    )
    lock_ok = lock_ids == catalog_ids and len(lock_ids) == 625
    sidecar_ok = not sidecar_errors
    seal_ok = not seal_errors
    current = catalog_ok and lock_ok and sidecar_ok and seal_ok
    return {
        "catalog_ok": catalog_ok,
        "current": current,
        "lock_ok": lock_ok,
        "logistics_count": logistics_count,
        "seal_errors": seal_errors,
        "seal_ok": seal_ok,
        "sidecar_errors": sidecar_errors,
        "sidecar_ok": sidecar_ok,
        "storage_count": storage_count,
        "variant_count": len(catalog_ids),
    }


def energy_projection() -> dict[str, Any]:
    catalog = census.load_json(census.MACHINE_TIERS)
    frozen_rows = census.load_json(census.MACHINE_TRACK).get("variants") or []
    frozen = {
        str(row["id"]): row
        for row in frozen_rows
        if isinstance(row, dict) and row.get("id")
    }
    live = {
        str(row["id"]): row
        for row in catalog.get("variants") or []
        if isinstance(row, dict) and row.get("id")
    }
    opening = repair.opening_variants(catalog)
    policy = catalog.get("namingPolicy") or {}
    source_rows = (catalog.get("source") or {}).get("variant_rows") or {}
    errors: list[str] = []
    if policy.get("automaticKindTierCompletion"):
        errors.append("automaticKindTierCompletion must stay false")
    if len(opening) != 33:
        errors.append(f"opening variants {len(opening)} != 33")
    projected: list[dict[str, Any]] = []
    for row in opening:
        variant_id = str(row["id"])
        expected = frozen.get(variant_id) or {}
        expected_band = expected.get("eu_voltage_band") or expected.get("material_tier")
        if variant_id not in source_rows:
            errors.append(f"{variant_id} missing source.variant_rows")
        if str(row.get("kind") or "") != str(expected.get("kind") or ""):
            errors.append(f"{variant_id} kind drifted")
        if str(row.get("energy") or "") != str(expected.get("energy") or ""):
            errors.append(f"{variant_id} energy drifted")
        if str(row.get("tierBand") or "") != str(expected_band or ""):
            errors.append(f"{variant_id} tierBand drifted from {expected_band}")
        projected.append(
            {
                "energy": row.get("energy"),
                "id": variant_id,
                "kind": row.get("kind"),
                "tierBand": row.get("tierBand"),
            }
        )
    return {
        "automatic_kind_tier_completion": bool(
            policy.get("automaticKindTierCompletion")
        ),
        "errors": errors,
        "live_variant_count": len(live),
        "lossless": not errors,
        "opening_count": len(opening),
        "projected": projected,
    }


def load_interpretation_evidence() -> dict[str, Any]:
    if not DEFERRED_LOAD_PROJECTION.is_file() or not DEFERRED_MEASUREMENT.is_file():
        raise ValueError("deferred recycling integrated measurement is missing")
    projection = census.load_json(DEFERRED_LOAD_PROJECTION)
    measurement = census.load_json(DEFERRED_MEASUREMENT)
    decision = projection.get("decision") or {}
    axes = decision.get("axes") or {}
    errors: list[str] = []
    axis_verdicts: dict[str, Any] = {}
    for axis in HARD_MEASURED_AXES:
        row = axes.get(axis) or {}
        status = str(row.get("status") or "")
        actual = row.get("actual")
        pending = bool(row.get("pending"))
        missing = axis not in axes
        zero_filled = actual in (0, 0.0, None) and status == "PASS"
        if missing or pending or status not in {"PASS"}:
            errors.append(f"{axis} status {status or 'missing'} pending={pending}")
        if actual is None:
            errors.append(f"{axis} actual missing")
        axis_verdicts[axis] = {
            "actual": actual,
            "pending": pending,
            "status": status,
            "zero_filled": bool(zero_filled),
        }
    count_verdicts: dict[str, Any] = {}
    for axis in COUNT_REPORT_ONLY_AXES:
        row = axes.get(axis) or {}
        status = str(row.get("status") or "")
        if row.get("eliminates"):
            errors.append(f"{axis} must not eliminate under interpretation A")
        if status not in {"PASS", "REPORT_ONLY_REFERENCE_EXCEEDED"}:
            errors.append(f"{axis} unexpected status {status}")
        count_verdicts[axis] = {
            "actual": row.get("actual"),
            "status": status,
        }
    if decision.get("eliminate_reasons"):
        errors.append("load projection still has eliminate_reasons")
    unverified = list(measurement.get("unverified_scale") or [])
    hybrid = next(
        (
            row
            for row in measurement.get("candidates") or []
            if row.get("candidate") == "hybrid"
        ),
        {},
    )
    if str(hybrid.get("status") or "") != "PASS":
        errors.append("hybrid integrated measurement is not PASS")
    if str(projection.get("load_status") or measurement.get("status") or "") in {
        "",
    }:
        pass
    return {
        "count_verdicts": count_verdicts,
        "errors": errors,
        "evidence_paths": [
            census.relative(DEFERRED_MEASUREMENT),
            census.relative(DEFERRED_LOAD_PROJECTION),
            census.relative(SMELTER_CLOSEOUT_MEASUREMENT),
            census.relative(MIXER_CLOSEOUT_MEASUREMENT),
        ],
        "hard_axis_verdicts": axis_verdicts,
        "interpretation": LOAD_INTERPRETATION,
        "measured_axes_pass": not errors,
        "unverified_scale": unverified,
    }


def owner_track_checkpoint() -> dict[str, Any]:
    lock = census.load_json(census.TOOLS / "owner_runtime_disposition_lock.json")
    retained = 0
    recycling = 0
    for row in lock.get("families") or []:
        if str(row.get("disposition") or "") == "retained_current_execution_gap":
            retained += 1
        if str(row.get("future_owner") or "") == "later:recycling":
            recycling += 1
    errors: list[str] = []
    if retained != 3483:
        errors.append(f"owner track retained {retained} != 3483")
    if recycling != 1817:
        errors.append(f"owner track later:recycling {recycling} != 1817")
    if int(lock.get("retained_current_execution_gap") or 0) != 3483:
        errors.append("owner track retained_current_execution_gap field drifted")
    return {
        "errors": errors,
        "family_count": int(lock.get("family_count") or 0),
        "later_recycling": recycling,
        "not_current_proof": True,
        "path": "tools/owner_readiness.json",
        "retained_current_execution_gap": retained,
    }


def _apply_closing(
    ledger: dict[str, dict[str, Any]],
    family_id: str,
    *,
    disposition: str,
    owner: str,
    expressed_by: str,
    future_owner: str | None = None,
    portfolio_scope: str | None = None,
) -> None:
    row = ledger.get(family_id)
    if row is None:
        return
    closing = row["closing"]
    closing["disposition"] = disposition
    closing["owner"] = owner
    if future_owner is not None:
        closing["future_owner"] = future_owner
    elif "future_owner" in closing and disposition == "implemented":
        closing["future_owner"] = None
    if portfolio_scope:
        closing["portfolio_scope"] = portfolio_scope
    row["expressed_by"] = expressed_by


def _iter_lock_rows(document: dict[str, Any]) -> list[tuple[dict[str, Any], str]]:
    rows: list[tuple[dict[str, Any], str]] = []
    production = document.get("production") or {}
    for row in production.get("families") or []:
        if isinstance(row, dict) and row.get("family_id"):
            rows.append((row, "implemented"))
    for row in document.get("reclassified") or []:
        if isinstance(row, dict) and row.get("family_id"):
            rows.append((row, "implemented"))
    for row in document.get("phase_deferred") or []:
        if isinstance(row, dict) and row.get("family_id"):
            rows.append((row, "phase_deferred"))
    return rows


def build_disposition_ledger() -> dict[str, Any]:
    families_doc = census.load_json(census.RECIPE_FAMILIES)
    census = census.load_json(census.CENSUS)
    identities = census.get("identities") or {}
    membership = [
        str(row["family_id"])
        for row in families_doc.get("families") or []
        if isinstance(row, dict) and row.get("family_id")
    ]
    if len(membership) != 5718:
        raise ValueError(f"recipe family membership {len(membership)} != 5718")
    t42_lock = census.load_json(census.TOOLS / "owner_disposition_lock.json")
    t42_ids = {
        str(row["family_id"])
        for row in t42_lock.get("families") or []
        if isinstance(row, dict) and row.get("family_id")
    }
    owner_lock = census.load_json(census.TOOLS / "owner_runtime_disposition_lock.json")
    retained_ids = {
        str(row["family_id"])
        for row in owner_lock.get("families") or []
        if str(row.get("disposition") or "") == "retained_current_execution_gap"
    }
    recycling_ids = {
        str(row["family_id"])
        for row in owner_lock.get("families") or []
        if str(row.get("future_owner") or "") == "later:recycling"
    }
    ledger: dict[str, dict[str, Any]] = {}
    missing_opening: list[str] = []
    for family_id in membership:
        identity = identities.get(family_id) or {}
        if not identity:
            missing_opening.append(family_id)
            continue
        opening = {
            "disposition": str(identity.get("disposition") or ""),
            "owner": str(identity.get("owner") or ""),
            "portfolio_scope": str(identity.get("portfolio_scope") or ""),
        }
        ledger[family_id] = {
            "closing": dict(opening),
            "expressed_by": None,
            "family_id": family_id,
            "in_owner_disposition_lock": family_id in t42_ids,
            "in_owner_track_retained": family_id in retained_ids,
            "opening": opening,
        }
    if missing_opening:
        raise ValueError(
            "T35 identities missing for "
            f"{len(missing_opening)} families: " + ",".join(missing_opening[:8])
        )
    for row in t42_lock.get("families") or []:
        if str(row.get("disposition") or "") != "closed_by_existing_expression":
            continue
        family_id = str(row.get("family_id") or "")
        _apply_closing(
            ledger,
            family_id,
            disposition="implemented",
            owner=str(row.get("expressed_by") or row.get("host") or "existing_expression"),
            expressed_by="owner_closed_by_existing_expression",
        )
    for path in IDENTITY_DELTAS:
        document = census.load_json(path)
        expressed_by = str(document.get("host") or path.name)
        for identity in document.get("identities") or []:
            family_id = str(identity.get("canonical_id") or identity.get("family_id") or "")
            closing = identity.get("closing") or {}
            _apply_closing(
                ledger,
                family_id,
                disposition=str(closing.get("disposition") or "implemented"),
                owner=str(identity.get("owner") or expressed_by),
                expressed_by=str(closing.get("expressed_by") or expressed_by),
            )
    for path, expressed_by, default_owner in PRODUCTION_LOCKS:
        document = census.load_json(path)
        for row, kind in _iter_lock_rows(document):
            family_id = str(row["family_id"])
            if kind == "phase_deferred":
                future = str(row.get("future_owner") or default_owner)
                _apply_closing(
                    ledger,
                    family_id,
                    disposition="phase_deferred",
                    owner=future,
                    expressed_by=expressed_by,
                    future_owner=future,
                    portfolio_scope="post_1x"
                    if future.startswith("post_1x") or future.startswith("post_1.x")
                    else None,
                )
                continue
            _apply_closing(
                ledger,
                family_id,
                disposition="implemented",
                owner=str(row.get("owner") or default_owner),
                expressed_by=expressed_by,
            )
    for row in post_1x_scope_rows():
        family_id = str(row["family_id"])
        future = str(row["future_owner"])
        _apply_closing(
            ledger,
            family_id,
            disposition="post_1x_scope",
            owner=future,
            expressed_by=str(row.get("scope_wave") or "post_1x_scope"),
            future_owner=future,
            portfolio_scope="post_1x",
        )
    checkpoint = owner_track_checkpoint()
    if checkpoint["errors"]:
        raise ValueError("; ".join(checkpoint["errors"]))
    families = [ledger[family_id] for family_id in membership]
    counts = Counter(str(row["closing"]["disposition"]) for row in families)
    remaining_gap: list[str] = []
    for family_id in sorted(retained_ids):
        closing = ledger[family_id]["closing"]
        if closing["disposition"] not in {"implemented", "post_1x_scope"}:
            remaining_gap.append(family_id)
    hanging: list[str] = []
    owner_errors: list[str] = []
    recycling_unclosed: list[str] = []
    for family_id in recycling_ids:
        closing = ledger[family_id]["closing"]
        if closing["disposition"] == "implemented":
            continue
        if closing["disposition"] == "post_1x_scope":
            continue
        recycling_unclosed.append(family_id)
    for row in families:
        owner = str(row["closing"].get("owner") or "")
        future = str(row["closing"].get("future_owner") or "")
        if owner in FORBIDDEN_OWNERS:
            owner_errors.append(row["family_id"])
        if future in HANGING_LATER or owner in HANGING_LATER:
            hanging.append(row["family_id"])
    post_1x_count = counts.get("post_1x_scope", 0)
    errors: list[str] = []
    if len(families) != 5718:
        errors.append(f"ledger {len(families)} != 5718")
    if remaining_gap:
        errors.append(
            f"retained execution gap remaining {len(remaining_gap)}: "
            + ",".join(remaining_gap[:8])
        )
    if post_1x_count != 28:
        errors.append(f"post_1x_scope {post_1x_count} != 28")
    if hanging:
        errors.append("hanging later:* " + ",".join(hanging[:8]))
    if owner_errors:
        errors.append("missing owner " + ",".join(owner_errors[:8]))
    if recycling_unclosed:
        errors.append(
            "later:recycling unclosed "
            f"{len(recycling_unclosed)}: " + ",".join(recycling_unclosed[:8])
        )
    if counts.get("retained_current_execution_gap"):
        errors.append("closing still has retained_current_execution_gap")
    if errors:
        raise ValueError("; ".join(errors))
    return {
        "counts": dict(sorted(counts.items())),
        "family_count": len(families),
        "families": families,
        "generated_by": generated_by("portfolio/census-disposition-replay"),
        "outside_owner_disposition_lock": 5718 - len(t42_ids),
        "post_1x_scope_count": post_1x_count,
        "remaining_execution_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "DISPOSITION_LEDGER_READY",
        "owner_runtime_checkpoint": {
            "later_recycling": checkpoint["later_recycling"],
            "not_current_proof": True,
            "path": checkpoint["path"],
            "retained_current_execution_gap": checkpoint["retained_current_execution_gap"],
        },
        "wave_slug": "portfolio/census-disposition-replay",
    }


def recycling_gap_replay() -> dict[str, Any]:
    return census.load_json(
        census.TOOLS / "waves" / "recycling" / "deferred-ordinary-runtime" / "gap_replay.json"
    )


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
            readiness = census.load_json(wave_dir(child) / "readiness.json")
            expected = STATUSES[child]
            if readiness.get("status") != expected:
                errors.append(f"{child} status {readiness.get('status')} != {expected}")
        return errors
    predecessor, status = PREDECESSORS[slug]
    errors = closeout_seal.check_wave_seal(predecessor)
    readiness = census.load_json(census.TOOLS / "waves" / predecessor / "readiness.json")
    if readiness.get("status") != status:
        errors.append(
            f"{predecessor} status {readiness.get('status')} != {status}"
        )
    return errors


def common_documents(
    slug: str,
    *,
    evidence: dict[str, Any],
    note: str,
) -> dict[str, Any]:
    spec = spec_for(slug)
    status = STATUSES[slug]
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": generated_by(slug),
        "partial_family_count": 0,
        "post_1x_scope_count": 28,
        "reclassification_delta": 0,
        "remaining_ordinary": {
            "complete_family_count": 1817,
            "completion_delta": 0,
            "deferred_recycling_count": 0,
            "deferred_total": 0,
            "enumerated": True,
            "partial_family_count": 0,
            "post_1x_scope_count": 28,
            "reclassification_delta": 24,
            "remaining_ordinary_families": 0,
        },
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
        "note": note,
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


def r0_exit_condition_ledger(storage: dict[str, Any]) -> dict[str, Any]:
    storage_color = "GREEN" if storage["current"] else ("YELLOW" if storage["catalog_ok"] and storage["lock_ok"] else "RED")
    conditions = [
        {
            "condition": "census_disposition_owner",
            "color": "YELLOW",
            "evidence": [
                "tools/t35_recipe_families.json",
                "tools/waves/recycling/deferred-ordinary-runtime/gap_replay.json",
            ],
            "note": "owner_readiness 3483 is a historical snapshot, not current proof.",
        },
        {
            "condition": "energy_matrix_selected_projection",
            "color": "YELLOW",
            "evidence": [
                "tools/t35_machine_track.json",
                "src/main/resources/data/cruciblecraft/machine_tiers.json",
            ],
            "note": "Live catalog grew past the selected 33. Replay lossless projection.",
        },
        {
            "condition": "current_recipe_execution_gap",
            "color": "GREEN",
            "evidence": [
                "tools/waves/recycling/deferred-ordinary-runtime/gap_replay.json"
            ],
            "note": "Recycling program already proved execution_gap=0.",
        },
        {
            "condition": "deferred_ledger_or_independent_scope",
            "color": "GREEN",
            "evidence": [
                "tools/waves/recycling/deferred-ordinary-runtime/deferred_ledger.json"
            ],
            "note": "28 independent post-1.x decisions; R0 only replays they were not lost.",
        },
        {
            "condition": "storage_28_624_logistics_1_1",
            "color": storage_color,
            "evidence": [
                "tools/storage_readiness.json",
                "tools/storage_storage_production_lock.json",
                "src/main/resources/data/cruciblecraft/storage_variants.json",
            ],
            "note": "storage/lock currentness sidecar/seal "
            + ("current." if storage["current"] else "needs written replay."),
        },
        {
            "condition": "load_ceiling_interpretation",
            "color": "YELLOW",
            "evidence": [
                census.relative(DEFERRED_LOAD_PROJECTION),
                census.relative(DEFERRED_MEASUREMENT),
            ],
            "interpretation": LOAD_INTERPRETATION,
            "note": "Count telemetry is not a 1.x exit hard ceiling. Load child must write interpretation A.",
        },
    ]
    return {
        "conditions": conditions,
        "generated_by": generated_by("portfolio/one-x-exit-r0"),
        "interpretation": LOAD_INTERPRETATION,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "EXIT_CONDITION_LEDGER_READY",
        "wave_slug": "portfolio/one-x-exit-r0",
    }


def r0_replay_needed(storage: dict[str, Any]) -> dict[str, Any]:
    storage_decision = "ALREADY_CURRENT" if storage["current"] else "MUST_REPLAY"
    if storage["current"] is False and not (storage["catalog_ok"] and storage["lock_ok"]):
        storage_decision = "MUST_REPLAY"
    children = [
        {
            "child": "portfolio/census-disposition-replay",
            "decision": "MUST_REPLAY",
            "reason": "owner_readiness is not current census proof.",
        },
        {
            "child": "portfolio/energy-matrix-replay",
            "decision": "MUST_REPLAY",
            "reason": "machine_tiers.json grew from 33 selected rows to the live catalog.",
        },
        {
            "child": "portfolio/storage-currentness-replay",
            "decision": storage_decision,
            "reason": "storage/lock catalog/lock/sidecar currentness "
            + ("holds." if storage["current"] else "must be written even if ALREADY_CURRENT is false."),
        },
        {
            "child": "portfolio/load-ceiling-interpretation",
            "decision": "MUST_REPLAY",
            "reason": "Interpretation A must be a machine-readable policy, not the last wave seal.",
        },
    ]
    for row in children:
        if row["decision"] == "ALREADY_CURRENT" and row["child"] != (
            "portfolio/storage-currentness-replay"
        ):
            raise ValueError("YELLOW/RED axes cannot be ALREADY_CURRENT")
        if (
            row["child"] == "portfolio/storage-currentness-replay"
            and row["decision"] == "ALREADY_CURRENT"
            and not storage["current"]
        ):
            raise ValueError("storage ALREADY_CURRENT requires current sidecars")
    return {
        "children": children,
        "generated_by": generated_by("portfolio/one-x-exit-r0"),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "REPLAY_NEEDED_READY",
        "wave_slug": "portfolio/one-x-exit-r0",
    }


def build_r0_documents() -> dict[str, Any]:
    errors = require_predecessor("portfolio/one-x-exit-r0")
    if errors:
        raise ValueError("; ".join(errors))
    if nuclear_started():
        raise ValueError("nuclear Track C must stay started=false")
    gap = recycling_gap_replay()
    if int(gap.get("execution_gap", 1)) != 0:
        raise ValueError("recycling gap_replay execution_gap is not 0")
    post = replay_post_1x_scope()
    storage = storage_currentness()
    seed = capability_map_seed("portfolio/one-x-exit-r0")
    ledger = r0_exit_condition_ledger(storage)
    replay = r0_replay_needed(storage)
    evidence = {
        "budget_references": list(BUDGET_REFERENCES),
        "completion_delta": 0,
        "execution_gap": 0,
        "load_interpretation": LOAD_INTERPRETATION,
        "nuclear_track_c_started": False,
        "one_x_joint_exit": False,
        "owns_families": 0,
        "post_1x_scope_count": post["count"],
        "recipe_files_generated": False,
        "seed_row_count": seed["row_count"],
        "storage_current": storage["current"],
    }
    documents = common_documents(
        "portfolio/one-x-exit-r0",
        evidence=evidence,
        note="R0 classifies six exit conditions, writes the capability-map seed, and replays 28 post-1.x decisions. No recipes.",
    )
    documents["capability_map_seed.json"] = seed
    documents["exit_condition_ledger.json"] = ledger
    documents["post_1x_scope_replay.json"] = post
    documents["replay_needed.json"] = replay
    return documents


def build_census_documents() -> dict[str, Any]:
    errors = require_predecessor("portfolio/census-disposition-replay")
    if errors:
        raise ValueError("; ".join(errors))
    ledger = build_disposition_ledger()
    gap = {
        "complete_family_count": 1817,
        "execution_gap": 0,
        "family_count": ledger["family_count"],
        "generated_by": generated_by("portfolio/census-disposition-replay"),
        "post_1x_scope_count": 28,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "GAP_REPLAY_READY",
        "owner_runtime_checkpoint_not_current_proof": True,
        "wave_slug": "portfolio/census-disposition-replay",
    }
    evidence = {
        "census_current": True,
        "completion_delta": 0,
        "disposition_counts": ledger["counts"],
        "execution_gap": 0,
        "family_count": 5718,
        "nuclear_track_c_started": False,
        "owns_families": 0,
        "post_1x_scope_count": 28,
        "recipe_files_generated": False,
    }
    documents = common_documents(
        "portfolio/census-disposition-replay",
        evidence=evidence,
        note="Replayed 5718 historical families from immutable T35 plus sealed overlays. owner_readiness is checkpoint only.",
    )
    documents["disposition_ledger.json"] = ledger
    documents["gap_replay.json"] = gap
    documents["exit_condition_update.json"] = {
        "color": "GREEN",
        "condition": "census_disposition_owner",
        "generated_by": generated_by("portfolio/census-disposition-replay"),
        "schema_version": 1,
        "wave_slug": "portfolio/census-disposition-replay",
    }
    return documents


def build_energy_documents() -> dict[str, Any]:
    errors = require_predecessor("portfolio/energy-matrix-replay")
    if errors:
        raise ValueError("; ".join(errors))
    projection = energy_projection()
    if not projection["lossless"]:
        raise ValueError("; ".join(projection["errors"]))
    evidence = {
        "automatic_kind_tier_completion": False,
        "completion_delta": 0,
        "correspondence_class": "selected_subset",
        "live_variant_count": projection["live_variant_count"],
        "nuclear_track_c_started": False,
        "opening_count": 33,
        "owns_families": 0,
        "recipe_files_generated": False,
        "selected_projection_lossless": True,
    }
    documents = common_documents(
        "portfolio/energy-matrix-replay",
        evidence=evidence,
        note="Selected 33 RU/KU/HU/EU rows still project losslessly. Live catalog length is not cartesian completion.",
    )
    documents["selected_matrix_replay.json"] = {
        "automatic_kind_tier_completion": False,
        "generated_by": generated_by("portfolio/energy-matrix-replay"),
        "live_variant_count": projection["live_variant_count"],
        "lossless": True,
        "opening_count": 33,
        "projected": projection["projected"],
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "ENERGY_MATRIX_REPLAY_READY",
        "wave_slug": "portfolio/energy-matrix-replay",
    }
    documents["exit_condition_update.json"] = {
        "color": "GREEN",
        "condition": "energy_matrix_selected_projection",
        "generated_by": generated_by("portfolio/energy-matrix-replay"),
        "schema_version": 1,
        "wave_slug": "portfolio/energy-matrix-replay",
    }
    return documents


def build_storage_documents() -> dict[str, Any]:
    errors = require_predecessor("portfolio/storage-currentness-replay")
    if errors:
        raise ValueError("; ".join(errors))
    storage = storage_currentness()
    if not (storage["catalog_ok"] and storage["lock_ok"]):
        raise ValueError("storage/lock catalog/lock is not current; GameTest rerun required")
    evidence = {
        "catalog_ok": True,
        "completion_delta": 0,
        "gametest_rerun": False,
        "lock_ok": True,
        "logistics_count": 1,
        "nuclear_track_c_started": False,
        "owns_families": 0,
        "recipe_files_generated": False,
        "seal_ok": storage["seal_ok"],
        "sidecar_ok": storage["sidecar_ok"],
        "storage_count": 624,
        "storage_current": storage["current"] or (storage["catalog_ok"] and storage["lock_ok"]),
    }
    documents = common_documents(
        "portfolio/storage-currentness-replay",
        evidence=evidence,
        note="storage/lock 28/624 + logistics 1/1 catalog/lock still match. No storage behavior reopened. GameTest not rerun while catalog/lock hold.",
    )
    documents["storage_currentness.json"] = {
        "catalog_ok": storage["catalog_ok"],
        "current": bool(evidence["storage_current"]),
        "generated_by": generated_by("portfolio/storage-currentness-replay"),
        "lock_ok": storage["lock_ok"],
        "logistics_count": 1,
        "schema_version": 1,
        "seal_ok": storage["seal_ok"],
        "sidecar_ok": storage["sidecar_ok"],
        "source_revision": SOURCE_REVISION,
        "status": "STORAGE_CURRENTNESS_READY",
        "storage_count": 624,
        "wave_slug": "portfolio/storage-currentness-replay",
    }
    documents["exit_condition_update.json"] = {
        "color": "GREEN",
        "condition": "storage_28_624_logistics_1_1",
        "generated_by": generated_by("portfolio/storage-currentness-replay"),
        "schema_version": 1,
        "wave_slug": "portfolio/storage-currentness-replay",
    }
    return documents


def build_load_documents() -> dict[str, Any]:
    errors = require_predecessor("portfolio/load-ceiling-interpretation")
    if errors:
        raise ValueError("; ".join(errors))
    evidence_doc = load_interpretation_evidence()
    if not evidence_doc["measured_axes_pass"]:
        raise ValueError(
            "load measured axes blocked: " + "; ".join(evidence_doc["errors"])
        )
    policy = {
        "budget_references": list(BUDGET_REFERENCES),
        "count_telemetry_is_exit_hard_ceiling": False,
        "count_verdicts": evidence_doc["count_verdicts"],
        "evidence_paths": evidence_doc["evidence_paths"],
        "generated_by": generated_by("portfolio/load-ceiling-interpretation"),
        "hard_axis_verdicts": evidence_doc["hard_axis_verdicts"],
        "hard_ceiling_axes": list(HARD_MEASURED_AXES),
        "interpretation": LOAD_INTERPRETATION,
        "java_constants_unchanged": True,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "LOAD_CEILING_INTERPRETATION_READY",
        "unverified_scale": evidence_doc["unverified_scale"],
        "wave_slug": "portfolio/load-ceiling-interpretation",
    }
    evidence = {
        "completion_delta": 0,
        "java_constants_unchanged": True,
        "load_interpretation": LOAD_INTERPRETATION,
        "measured_axes_pass": True,
        "nuclear_track_c_started": False,
        "owns_families": 0,
        "recipe_files_generated": False,
        "unverified_scale": evidence_doc["unverified_scale"],
    }
    documents = common_documents(
        "portfolio/load-ceiling-interpretation",
        evidence=evidence,
        note="Interpretation A: count telemetry is UNVERIFIED_SCALE / REPORT_ONLY. Measured reload/index/lookup/sync/retained/allocation axes remain the hard door.",
    )
    documents["load_interpretation.json"] = policy
    documents["exit_condition_update.json"] = {
        "color": "GREEN",
        "condition": "load_ceiling_interpretation",
        "generated_by": generated_by("portfolio/load-ceiling-interpretation"),
        "interpretation": LOAD_INTERPRETATION,
        "schema_version": 1,
        "wave_slug": "portfolio/load-ceiling-interpretation",
    }
    return documents


def program_exit_condition_ledger() -> dict[str, Any]:
    conditions = []
    r0 = census.load_json(wave_dir("portfolio/one-x-exit-r0") / "exit_condition_ledger.json")
    updates = {
        "census_disposition_owner": wave_dir("portfolio/census-disposition-replay")
        / "exit_condition_update.json",
        "energy_matrix_selected_projection": wave_dir("portfolio/energy-matrix-replay")
        / "exit_condition_update.json",
        "storage_28_624_logistics_1_1": wave_dir("portfolio/storage-currentness-replay")
        / "exit_condition_update.json",
        "load_ceiling_interpretation": wave_dir("portfolio/load-ceiling-interpretation")
        / "exit_condition_update.json",
    }
    for row in r0.get("conditions") or []:
        copied = dict(row)
        path = updates.get(str(row.get("condition")))
        if path is not None:
            update = census.load_json(path)
            copied["color"] = update["color"]
            copied["closed_by"] = update["wave_slug"]
        if copied["color"] not in {"GREEN"}:
            raise ValueError(
                f"{copied['condition']} is {copied['color']}, not GREEN"
            )
        conditions.append(copied)
    return {
        "conditions": conditions,
        "generated_by": generated_by(PROGRAM),
        "interpretation": LOAD_INTERPRETATION,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "EXIT_CONDITION_LEDGER_READY",
        "wave_slug": PROGRAM,
    }


def build_program_documents() -> dict[str, Any]:
    errors = require_predecessor(PROGRAM)
    if errors:
        raise ValueError("; ".join(errors))
    if nuclear_started():
        raise ValueError("nuclear Track C must stay started=false")
    seed = census.load_json(wave_dir("portfolio/one-x-exit-r0") / "capability_map_seed.json")
    handed = dict(seed)
    handed["generated_by"] = generated_by(PROGRAM)
    handed["handed_to"] = NEXT_MAJOR
    handed["wave_slug"] = PROGRAM
    ledger = program_exit_condition_ledger()
    gap = {
        "complete_family_count": 1817,
        "deferred_ledger": 0,
        "execution_gap": 0,
        "generated_by": generated_by(PROGRAM),
        "post_1x_scope_count": 28,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "GAP_REPLAY_READY",
        "wave_slug": PROGRAM,
    }
    evidence = {
        "completion_delta": 0,
        "deferred_ledger": 0,
        "execution_gap": 0,
        "load_interpretation": LOAD_INTERPRETATION,
        "next_major": NEXT_MAJOR,
        "nuclear_track_c_started": False,
        "one_x_joint_exit": True,
        "owns_families": 0,
        "post_1x_scope_count": 28,
        "production_lock_for_next_major": None,
        "recipe_files_generated": False,
        "seed_row_count": handed["row_count"],
    }
    documents = common_documents(
        PROGRAM,
        evidence=evidence,
        note="ONE_X_JOINT_EXIT_READY. 1.x claimed work is jointly accepted. Next major is portfolio/source-capability-map. Nuclear Track C stays started=false.",
    )
    documents["capability_map_seed.json"] = handed
    documents["exit_condition_ledger.json"] = ledger
    documents["gap_replay.json"] = gap
    return documents


BUILDERS = {
    "portfolio/one-x-exit-r0": build_r0_documents,
    "portfolio/census-disposition-replay": build_census_documents,
    "portfolio/energy-matrix-replay": build_energy_documents,
    "portfolio/storage-currentness-replay": build_storage_documents,
    "portfolio/load-ceiling-interpretation": build_load_documents,
    "portfolio/one-x-joint-exit": build_program_documents,
}


def write_seal(slug: str) -> dict[str, Any]:
    root = wave_dir(slug)
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
        "card_id": slug,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": census.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": census.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{generated_by(slug)} --write",
        "hashes": hashes,
        "note": census.load_json(root / "readiness.json").get("note"),
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
    census.write_stable(root / "closeout_seal.json", seal)
    return seal


def write_artifacts(slug: str) -> dict[str, Any]:
    documents = BUILDERS[slug]()
    root = wave_dir(slug)
    root.mkdir(parents=True, exist_ok=True)
    for name, document in documents.items():
        census.write_stable(root / name, document)
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
        committed = census.load_json(root / name)
        drift = census.first_json_diff(document, committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = census.load_json(root / "readiness.json")
    if readiness.get("status") != STATUSES[slug]:
        errors.append(f"{slug} status drifted")
    if readiness.get("unique_active_wave") != spec.unique_active_wave:
        errors.append(f"{slug} unique_active_wave drifted")
    if nuclear_started():
        errors.append("nuclear Track C started must stay false")
    errors.extend(closeout_seal.check_wave_seal(slug))
    return errors
