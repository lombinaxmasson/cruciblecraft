#!/usr/bin/env python3
"""Build the final T14 materialization, budget and O-26 readiness."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
OUTPUT = TOOLS / "t14_readiness.json"
T13 = TOOLS / "t13_denominator_readiness.json"
T12 = TOOLS / "t12_closure_readiness.json"
EXTRUDER = TOOLS / "t14_extruder_readiness.json"
EXPECTED = TOOLS / "t14_extruder_expected.json"
COMPACT = TOOLS / "t14_extruder_compact.json"
MATERIALIZATION_POLICY = TOOLS / "t14_materialization_policy.json"
DECISION = TOOLS / "t14_materialization_decision.json"
SUPERSEDED_MEASUREMENT = TOOLS / "t14_recipe_load_benchmark.json"
LOAD_POLICY = TOOLS / "t14_load_budget_policy.json"
LOAD_PROJECTION = TOOLS / "t14_extruder_load_projection.json"
LOAD_PROJECTION_INPUT = TOOLS / "t14_extruder_load_projection_input.json"
PROJECTION_TOOL = TOOLS / "recipe_load_projection.py"
PROJECTION_SCHEMA = TOOLS / "recipe_load_projection.schema.json"

SOURCE_CONTRACTS = {
    "provider": (
        "src/main/java/com/masson/cruciblecraft/recipe/gt/ExtruderRecipeFamilyProvider.java",
        (
            "CACHE_CEILING = 512",
            "HOT_MODULO = 5",
            "stableFingerprint",
            "RuntimeSide",
            "sources.size() > AUTHORED_ENTRIES",
            "definitions.size() >= LOGICAL_RELATIONS",
            "shadow_order must be strictly increasing",
            "syncPayloadBytes",
        ),
    ),
    "provider_test": (
        "src/test/java/com/masson/cruciblecraft/recipe/gt/"
        "ExtruderRecipeFamilyProviderTest.java",
        (
            "nineteenSourcesEnumerateIdenticallyOnServerAndDedicatedClient",
            "relationGapsRemainStableAndAreNotRenumbered",
            "fullyDisabledFamilyIsEnumerableOnBothRuntimeSides",
            "sourceAndRelationCeilingsRemainFailClosed",
            "duplicateNegativeAndInvalidRelationsRemainRejectedWithIdentity",
        ),
    ),
    "material_rule": (
        "src/main/java/com/masson/cruciblecraft/recipe/rule/MaterialRule.java",
        (
            "Sparse shadow_order must be strictly increasing at",
            "Sparse shadow_order cannot be negative for",
        ),
    ),
    "material_rule_expansion": (
        "src/main/java/com/masson/cruciblecraft/recipe/rule/"
        "MaterialRuleExpansion.java",
        (
            "public static Expanded expandSparseRelation(",
            "ResourceResolver resolver",
        ),
    ),
    "recipe_map": (
        "src/main/java/com/masson/cruciblecraft/recipe/gt/RecipeMap.java",
        (
            "RecipeFamily",
            "logicalRecipeCount",
            "lazyRecipeCount",
            "cacheCeiling",
        ),
    ),
    "loader": (
        "src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipeMapLoader.java",
        (
            "eagerPublishedRecipes",
            "lazyLogicalRecipes",
            "t14ExtruderAuthoredEntries",
            "t14ExtruderSyncBytes",
            "validateT14MaterializationBudgets",
            "validateCompleteReloadRows",
            "T14OnlineBudgetGate",
        ),
    ),
    "epoch": (
        "src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipeRuntimeEpoch.java",
        ("publishPrepared", "MaterialCatalog.publishPreview", "epoch"),
    ),
    "game_test": (
        "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java",
        (
            # Assertion pattern (keeps direct observation per §5.1).
            # The specific number is verified against T14 baseline + delta
            # by the _validate_publication_delta check below.
            "metrics.eagerPublishedRecipes() ==",
            "metrics.lazyLogicalRecipes() ==",
            "metrics.t14ExtruderCacheCeiling() ==",
            "metrics.t14ExtruderSyncBytes() ==",
            "extruderFamily.cacheSize() <= extruderFamily.cacheCeiling()",
            "onlineGate.allPass()",
        ),
    ),
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def source_contracts() -> dict[str, Any]:
    rows: dict[str, Any] = {}
    for owner, (relative, tokens) in SOURCE_CONTRACTS.items():
        path = ROOT / relative
        source = path.read_text(encoding="utf-8")
        missing = [token for token in tokens if token not in source]
        if missing:
            raise ValueError(
                f"T14 source contract {owner} is incomplete: {missing}"
            )
        rows[owner] = {
            "path": relative,
            "sha256": sha256(path),
            "required_tokens": list(tokens),
        }
    # Delta-aware publication value check (T21 third rework B2).
    # T14 baseline = 16,650.  The GameTest assertion may carry a higher
    # literal when subsequent phases registered a publication delta.
    # We extract the actual literal, sum registered deltas, and compare.
    _validate_publication_delta(rows)
    return rows


def _validate_publication_delta(contracts: dict[str, Any]) -> None:
    """Verify GameTest publication literals == T14 baseline + registered deltas.

    Checks all three counters (allPublished, eagerPublished, lazyLogical)
    against their T14 baselines plus deltas registered in t*_publication_baseline
    files.  A mismatch means either the GameTest number is wrong or a
    publication delta was not registered.
    """
    game_test_row = contracts.get("game_test")
    if game_test_row is None:
        return
    path = ROOT / game_test_row["path"]
    source = path.read_text(encoding="utf-8")

    t14_baselines = {
        "allPublishedRecipes": 18_875,
        "eagerPublishedRecipes": 16_650,
        "lazyLogicalRecipes": 2_225,
    }
    deltas = _compute_registered_deltas()

    for counter, baseline in t14_baselines.items():
        match = re.search(
            rf"metrics\.{counter}\(\)\s*==\s*([0-9_]+)",
            source,
        )
        if match is None:
            raise ValueError(
                f"T14 source contract game_test: "
                f"cannot find metrics.{counter}() == ... literal"
            )
        actual = int(match.group(1).replace("_", ""))
        delta_key = {
            "allPublishedRecipes": "logical_rows_added",
            "eagerPublishedRecipes": "eager_rows_added",
            "lazyLogicalRecipes": "lazy_rows_added",
        }[counter]
        registered = deltas.get(delta_key, 0)
        expected = baseline + registered
        if actual != expected:
            raise ValueError(
                f"T14 source contract game_test: "
                f"metrics.{counter}() literal {actual} != "
                f"T14 baseline {baseline} + registered delta {registered} "
                f"(expected {expected}). "
                f"Either the GameTest number is wrong or a publication delta "
                f"was not registered."
            )


def _compute_registered_deltas() -> dict[str, int]:
    """Return {logical_rows_added, eager_rows_added, lazy_rows_added}
    summed across all publication baseline files.

    Scans for any key ending in ``_publication_delta`` (phase-specific, e.g.
    ``t21_publication_delta``) or the generic ``publication_delta``.
    A corrupt baseline file is a hard error — it must not silently reduce
    the expected delta.
    """
    totals: dict[str, int] = {}
    baseline_dir = ROOT / "src/main/resources/data/cruciblecraft"
    for baseline_path in sorted(baseline_dir.glob("t*_publication_baseline.json")):
        doc = load(baseline_path)
        pub_delta = None
        for key, value in doc.items():
            if key == "publication_delta" or key.endswith("_publication_delta"):
                if isinstance(value, dict):
                    pub_delta = value
                    break
        if pub_delta is None:
            continue
        for rows_key in ("logical_rows_added", "eager_rows_added", "lazy_rows_added"):
            value = pub_delta.get(rows_key, 0)
            if not isinstance(value, int) or value < 0:
                raise ValueError(
                    f"{baseline_path.name}: {rows_key} is not a non-negative int"
                )
            totals[rows_key] = totals.get(rows_key, 0) + value
    return totals


def selected_measurement(
    decision: dict[str, Any], winner: str
) -> dict[str, Any]:
    row = next(
        row
        for row in decision["key_measurements"]
        if row["scale"] == "1x"
    )
    return next(
        candidate
        for candidate in row["candidates"]
        if candidate["candidate"] == winner
    )


def measured_limits(
    decision: dict[str, Any], winner: str
) -> dict[str, Any]:
    row = next(
        row
        for row in decision["key_measurements"]
        if row["scale"] == "20x"
    )
    return next(
        candidate
        for candidate in row["candidates"]
        if candidate["candidate"] == winner
    )


def build() -> dict[str, Any]:
    t13 = load(T13)
    t12 = load(T12)
    extruder = load(EXTRUDER)
    compact = load(COMPACT)
    expected = load(EXPECTED)
    policy = load(MATERIALIZATION_POLICY)
    decision = load(DECISION)
    load_policy = load(LOAD_POLICY)
    projection = load(LOAD_PROJECTION)
    projection_input = load(LOAD_PROJECTION_INPUT)

    if t13["status"] != "T13_READY":
        raise ValueError("T14 requires current T13_READY")
    if (
        extruder["status"] != "READY"
        or extruder["scope"]["authored_entries"] != 20
        or extruder["scope"]["logical_relations"] != 2_782
        or extruder["scope"]["runtime_publication"] != 2_782
        or extruder["compression"]["datapack_entry_delta"] != -2_762
        or compact["logical_relation_count"] != 2_782
        or compact["authored_entry_count"] != 20
        or expected["relation_count"] != 2_782
    ):
        raise ValueError("T14a Extruder equivalence is not closed")
    if (
        policy["schema_version"] != 2
        or policy["status"] != "T14C_PRODUCTION_MEASUREMENT_POLICY"
        or decision["schema_version"] != 2
        or decision["measurement"]["schema_version"] != 2
        or decision["measurement"]["protocol"]["id"]
        != "t14_materialization_lookup_cache_v2"
        or decision["decision"]["status"] != "PRODUCTION_WINNER_READY"
        or decision["decision"]["production_winner"] is None
        or decision["decision"]["production_blockers"]
    ):
        raise ValueError("T14c production winner is not closed")
    winner = decision["decision"]["production_winner"]
    if (
        load_policy["status"] != "T14D_LOAD_BUDGET_POLICY_MEASURED"
        or load_policy["pending_measurements"]
        or projection["status"] != "PASS"
        or projection["families"][0]["strategy"] != winner
    ):
        raise ValueError("T14d load projection or budgets are incomplete")

    current_datapack_entries = t12["load_gate"][
        "concrete_datapack_recipe_entries"
    ]
    if (
        t12["load_gate"]["datapack_recipe_entries"] != 6_025
        or t12["load_gate"]["post_t12_virtualized_recipe_entries"] != 2_762
        or current_datapack_entries != 3_263
    ):
        raise ValueError("T14 authored/logical datapack axes drifted")
    logical_recipes = 18_875
    eager_recipes = 16_650
    lazy_recipes = 2_225
    cache_ceiling = 512
    if projection["ledger"]["counts"] != {
        "datapack_authored_entries": 20,
        "eager_publication_rows": 557,
        "lazy_cache_ceiling_rows": 512,
        "lazy_logical_rows": 2225,
        "logical_rows": 2782,
        "sync_bytes": 331124,
    }:
        raise ValueError("T14 selected family projection drifted")

    chosen = selected_measurement(decision, winner)
    stress = measured_limits(decision, winner)
    return {
        "schema_version": 1,
        "status": "T14_READY",
        "source_revision": t13["source_revision"],
        "delivery_boundary": "T14_EXTRUDER_MATERIALIZATION_AND_LOAD",
        "o_26": {
            "status": "CLOSED",
            "authored_before": 2_782,
            "authored_after": 20,
            "logical_relations": 2_782,
            "full_field_bidirectional_equivalence": True,
            "expected_fingerprint": extruder["relation_fingerprint"],
            "compact_fingerprint": extruder["compact_fingerprint"],
            "t8_pipe_rows_separate": 257,
            "skipped_templates": 42,
            "exact_remainder": 1,
        },
        "materialization": {
            "winner": winner,
            "decision_status": decision["decision"]["status"],
            "winner_algorithm": decision["decision"]["algorithm"],
            "error_band_tied_candidates": decision["decision"][
                "error_band_tied_candidates"
            ],
            "logical_rows": logical_recipes,
            "eager_rows": eager_recipes,
            "lazy_rows": lazy_recipes,
            "extruder": {
                "logical": 2_782,
                "eager": 557,
                "lazy": 2_225,
                "cache_ceiling": cache_ceiling,
                "authored": 20,
            },
            "client_server_fingerprint_equal": (
                chosen["server_stable_fingerprint"]
                == chosen["dedicated_client_stable_fingerprint"]
            ),
        },
        "measurements": {
            "selected_1x": chosen,
            "selected_20x": stress,
            "production_decision_sha256": sha256(DECISION),
            "retained_protocol": policy["retained_memory_protocol"],
            "allocation_protocol": policy["allocation_protocol"],
            "superseded_protocol": {
                "path": SUPERSEDED_MEASUREMENT.relative_to(ROOT).as_posix(),
                "current": False,
                "superseded_by": (
                    "t14_materialization_lookup_cache_v2"
                ),
            },
        },
        "load_gate": {
            "datapack_authored_entries": current_datapack_entries,
            "logical_recipes": logical_recipes,
            "eager_recipes": eager_recipes,
            "lazy_recipes": lazy_recipes,
            "lazy_cache_ceiling": cache_ceiling,
            "authored_to_logical_ratio": round(
                logical_recipes / current_datapack_entries, 6
            ),
            "authored_to_eager_ratio": round(
                eager_recipes / current_datapack_entries, 6
            ),
            "budgets": load_policy["budgets"],
            "pending_measurements": [],
        },
        "projection_template": {
            "tool": PROJECTION_TOOL.relative_to(ROOT).as_posix(),
            "schema": PROJECTION_SCHEMA.relative_to(ROOT).as_posix(),
            "budget_policy": LOAD_POLICY.relative_to(ROOT).as_posix(),
            "selected_input": LOAD_PROJECTION_INPUT.relative_to(
                ROOT
            ).as_posix(),
            "selected_projection": LOAD_PROJECTION.relative_to(
                ROOT
            ).as_posix(),
            "selected_projection_status": projection["status"],
            "t15_t19_fixtures": (
                "tools/tests/fixtures/t14_load_projection_t15_t19.json"
            ),
        },
        "currentness": {
            **{
                path.relative_to(ROOT).as_posix(): sha256(path)
                for path in (
                    T13,
                    T12,
                    EXTRUDER,
                    EXPECTED,
                    COMPACT,
                    MATERIALIZATION_POLICY,
                    DECISION,
                    LOAD_POLICY,
                    LOAD_PROJECTION,
                    LOAD_PROJECTION_INPUT,
                    PROJECTION_TOOL,
                    PROJECTION_SCHEMA,
                    BUILDER,
                )
            },
            "pending_report": {
                "status": "BOUND_TO_FULL_VERIFICATION_REPORT",
                "this_refresh_final_closure_attempted": True,
                "pending": [],
                "evidence": "tools/full_verification_report.json",
            },
        },
        "source_contracts": source_contracts(),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    try:
        document = build()
        encoded = stable(document)
        if args.check:
            if (
                not OUTPUT.is_file()
                or OUTPUT.read_text(encoding="utf-8") != encoded
            ):
                raise ValueError("T14 readiness is stale")
        else:
            OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T14 readiness failed: {error}")
        return 1
    print(
        json.dumps(
            {
                "status": "T14_READY",
                "winner": document["materialization"]["winner"],
                "authored": 20,
                "logical": 18_875,
                "eager": 16_650,
                "lazy": 2_225,
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
