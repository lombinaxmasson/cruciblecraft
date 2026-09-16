#!/usr/bin/env python3
"""Shared paths and vocabulary for assembler/compact census overlays."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import census_common as census
from tools import repair_common as repair

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION
OWNER = "portfolio:track_a/assembler_compact"
HOST = "cruciblecraft:assembler"
ROASTER_HOST = "cruciblecraft:roaster"
FAMILY_COUNT = 50
ROASTER_FAMILY_COUNT = 29
REMAINING_ORDINARY_FAMILIES = 5668
REVOKED = tuple(f"T{number}" for number in range(38, 47))
RECIPE_WAVE_MIN = 50
RECIPE_WAVE_MAX = 200
PENDING_LOAD_VERDICT = "BLOCKED_PENDING_MEASUREMENT"

T35_FOUNDATION = {
    "machine_tree_identities": 765,
    "exclusion_source_sites": 763,
    "exclusion_expanded_rows": 1701,
    "recipe_rows_accounted": 78682,
    "recipe_families": 5718,
}

T14_COUNTABLE = (
    "datapack_authored_entries",
    "eager_publication_rows",
    "lazy_logical_rows",
)
T14_PENDING = (
    "lazy_cache_ceiling_rows",
    "sync_bytes",
    "server_reload_ms",
    "server_index_ms",
    "client_reload_ms",
    "client_index_ms",
    "retained_memory_bytes",
    "allocation_bytes",
    "lookup_p95_ns",
    "lookup_candidate_count",
)
STRATEGY_AXES = (
    "eager_publication_rows",
    "lazy_logical_rows",
    "lazy_cache_ceiling_rows",
)

ASSEMBLER_SOURCE = TOOLS / "assembler_source.json"
ASSEMBLER_RECEIPT = TOOLS / "assembler_source_receipt.json"
ASSEMBLER_DUMP = (
    ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.assembler.json"
)
PLAYER_PATH = TOOLS / "assembler_compact_player_path.json"
EQUIVALENCE = TOOLS / "assembler_equivalence.json"
POLICY = TOOLS / "assembler_compact_materialization_policy.json"
DECISION = TOOLS / "assembler_compact_materialization_decision.json"
MEASUREMENTS = TOOLS / "assembler_compact_materialization_measurements.json"
PUBLICATION_DELTA = TOOLS / "assembler_compact_publication_delta.json"
CENSUS_DELTA = TOOLS / "assembler_compact_census_delta.json"
CARD_TOPOLOGY = TOOLS / "assembler_compact_card_topology.json"
READINESS = TOOLS / "assembler_compact_readiness.json"
T36_CENSUS_DELTA = repair.CENSUS_DELTA
T36_READINESS = repair.READINESS
T36_TOPOLOGY = TOOLS / "repair_card_topology.json"
GENERATED_ROOT = (
    ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "assembler"
    / "compact"
)
JAVA_CODEC_TESTS = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "CompactGTRecipeFamilyGeneratedTest.java",
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "CompactRecipeFamilyProviderTest.java",
)
GAME_TEST_ROOT = ROOT / "src/test/java/com/masson/cruciblecraft/gametest"

OPENING_DISPOSITION = "planned"
OPENING_CLOSURE = "incomplete"
OPENING_FIDELITY = "source_backed"
OPENING_LOAD = "pending"
CLOSING_DISPOSITION = "implemented"
CLOSING_CLOSURE = "closed"
CLOSING_LOAD = "measured"


def relative(path: Path) -> str:
    return census.relative(path)


def load_json(path: Path) -> Any:
    return census.load_json(path)


def parse_write_check(description: str, argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    return args


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return census.check_generated_document(path, document)


def roaster_frozen_wave() -> dict[str, Any]:
    topology = load_json(T36_TOPOLOGY)
    for row in topology.get("sequence") or []:
        if row.get("id") == "roaster/compact":
            family_ids = list(row.get("family_ids") or [])
            if len(family_ids) != ROASTER_FAMILY_COUNT:
                raise ValueError(
                    f"repair topology roaster family_ids must stay {ROASTER_FAMILY_COUNT}, "
                    f"got {len(family_ids)}"
                )
            if row.get("host") != ROASTER_HOST:
                raise ValueError("T36 topology roaster/compact host drifted from cruciblecraft:roaster")
            return {
                "family_ids": family_ids,
                "host": ROASTER_HOST,
                "size": ROASTER_FAMILY_COUNT,
            }
    raise ValueError("T36 topology is missing the frozen roaster/compact roaster wave")


_CENSUS: dict[str, Any] | None = None


def load_census() -> dict[str, Any]:
    global _CENSUS
    if _CENSUS is None:
        _CENSUS = load_json(census.CENSUS)
    return _CENSUS


def assembler_compact_pilot_ids(census: dict[str, Any] | None = None) -> list[str]:
    census = census if census is not None else load_census()
    family_ids = list((census.get("assembler_compact_pilot") or {}).get("family_ids") or [])
    if len(family_ids) != FAMILY_COUNT:
        raise ValueError(f"assembler compact family_ids must be 50, got {len(family_ids)}")
    return family_ids


def generated_family_files() -> list[Path]:
    if not GENERATED_ROOT.is_dir():
        return []
    return sorted(
        path for path in GENERATED_ROOT.glob("gt_recipe_assembler_*.json") if path.is_file()
    )


def dump_present() -> bool:
    return ASSEMBLER_DUMP.is_file()


def production_strategy(decision: dict[str, Any] | None = None) -> dict[str, Any]:
    decision = decision if decision is not None else (
        load_json(DECISION) if DECISION.is_file() else {}
    )
    nested = decision.get("decision") or {}
    winner = nested.get("production_winner")
    blocked = (
        decision.get("status") == "ASSEMBLER_COMPACT_MATERIALIZATION_DECISION_BLOCKED"
        or nested.get("status") == "PRODUCTION_WINNER_BLOCKED"
        or winner in {None, ""}
    )
    recomputable = False
    if not blocked:
        try:
            from tools import build_assembler_compact_recipe_load_benchmark as benchmark
            benchmark.validate_artifact(decision)
            recomputable = True
        except (ValueError, KeyError, OSError, json.JSONDecodeError):
            blocked = True
            winner = None
    return {
        "blocked": blocked,
        "winner": None if blocked else str(winner),
        "status": nested.get("status") or decision.get("status"),
        "recomputable": recomputable,
    }


def partition_for_winner(winner: str | None) -> dict[str, int | None]:
    table = {
        "immediate": {
            "eager_publication_rows": 50,
            "lazy_logical_rows": 0,
            "lazy_cache_ceiling_rows": 0,
        },
        "on_demand": {
            "eager_publication_rows": 0,
            "lazy_logical_rows": 50,
            "lazy_cache_ceiling_rows": 8,
        },
        "hybrid": {
            "eager_publication_rows": 14,
            "lazy_logical_rows": 36,
            "lazy_cache_ceiling_rows": 8,
        },
    }
    if winner not in table:
        return {
            "eager_publication_rows": None,
            "lazy_logical_rows": None,
            "lazy_cache_ceiling_rows": None,
        }
    return table[winner]


def player_gametest_present() -> bool:
    if not GAME_TEST_ROOT.is_dir():
        return False
    markers = ("cruciblecraft_wave_assembler_compact", "AssemblerCompactGameTests", "-PwaveRecipes=assembler/compact")
    for path in GAME_TEST_ROOT.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        if any(marker in text for marker in markers):
            return True
    return False


def java_codec_tests_present() -> bool:
    return all(path.is_file() for path in JAVA_CODEC_TESTS)
