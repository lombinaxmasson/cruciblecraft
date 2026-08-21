#!/usr/bin/env python3
"""Shared T31 helpers: pending metrics, environment, historical T24 SKIP."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from tools import t27_common as common

TOOLS = common.TOOLS
ROOT = common.ROOT
SAMPLES = TOOLS / "t31_scale_samples"
MANIFEST = TOOLS / "t24_workload_manifest.json"
T24_EVIDENCE = TOOLS / "t24_scale_evidence.json"
T26_ISSUES = TOOLS / "t26_known_issues.json"
REPORT = TOOLS / "full_verification_report.json"
GRADLE = ROOT / "gradle.properties"
T30_READINESS = TOOLS / "t30_readiness.json"
ROUTE_CAP = 32_768
MIN_PHYSICAL_RAM_BYTES = 16 * 1024 * 1024 * 1024
TARGET_VERSION = "0.1.0-rc.1"
OPENING_VERSION = "0.1.0-beta.1"
SCENARIOS = ("small", "target", "stress")


def gradle_mod_version() -> str:
    for line in GRADLE.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if stripped.startswith("mod_version="):
            return stripped.split("=", 1)[1].strip()
    raise ValueError("mod_version missing from gradle.properties")


def historical_t24_scale_skip() -> dict[str, Any]:
    evidence = common.load_json(T24_EVIDENCE)
    measured = evidence["measured_at_scale"]
    return {
        "artifact": "tools/t24_scale_evidence.json",
        "rewritten_as_measured_at_t24": False,
        "target_status": measured["target"]["status"],
        "stress_status": measured["stress"]["status"],
        "target_reason": measured["target"].get("reason"),
        "stress_reason": measured["stress"].get("reason"),
    }


def historical_f003_f005() -> dict[str, Any]:
    issues = common.load_json(T26_ISSUES)
    rows = {row["id"]: row for row in issues.get("issues") or []}
    return {
        "T24-F003": {
            "historical_owner": rows["T24-F003"]["owner"],
            "historical_evidence_artifact": rows["T24-F003"]["evidence_artifact"],
            "historical_disposition": rows["T24-F003"]["disposition"],
        },
        "T24-F005": {
            "historical_owner": rows["T24-F005"]["owner"],
            "historical_evidence_artifact": rows["T24-F005"]["evidence_artifact"],
            "historical_disposition": rows["T24-F005"]["disposition"],
        },
    }


def sample_paths(scenario: str) -> list[Path]:
    directory = SAMPLES / scenario
    if not directory.is_dir():
        return []
    return sorted(
        path
        for path in directory.glob("sample_*.json")
        if path.is_file()
    )


def percentile(values: list[int | float], pct: int) -> int | float | None:
    if not values:
        return None
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, int((pct / 100) * len(ordered) + 0.999) - 1))
    return ordered[index]


def summarize_samples(scenario: str) -> dict[str, Any]:
    paths = sample_paths(scenario)
    if not paths:
        return {
            "status": "PENDING_MEASUREMENT",
            "samples": 0,
            "visited_max": None,
            "visited_p95": None,
            "route_cache_entries_max": None,
            "tick_wall_clock_p50_nanos": None,
            "tick_wall_clock_p95_nanos": None,
            "tick_wall_clock_max_nanos": None,
            "reload_index_nanos": None,
            "retained_heap_before_bytes": None,
            "retained_heap_after_bytes": None,
            "network_sync_bytes": None,
            "transfer_conservation_ok": None,
            "route_cap_exceeded": None,
            "zero_filled": False,
        }
    rows = [json.loads(path.read_text(encoding="utf-8")) for path in paths]
    visited = [int(row["visited_max"]) for row in rows]
    conservation = all(bool(row.get("transfer_conservation_ok")) for row in rows)
    cap_exceeded = any(value > ROUTE_CAP for value in visited)
    return {
        "status": "MEASURED_AT_SCALE",
        "samples": len(rows),
        "sample_files": [common.relative(path) for path in paths],
        "visited_max": max(visited),
        "visited_p95": percentile(
            [int(row["visited_p95"]) for row in rows], 95
        ),
        "route_cache_entries_max": max(
            int(row["route_cache_entries_max"]) for row in rows
        ),
        "tick_wall_clock_p50_nanos": percentile(
            [int(row["tick_wall_clock_p50_nanos"]) for row in rows], 50
        ),
        "tick_wall_clock_p95_nanos": percentile(
            [int(row["tick_wall_clock_p95_nanos"]) for row in rows], 95
        ),
        "tick_wall_clock_max_nanos": max(
            int(row["tick_wall_clock_max_nanos"]) for row in rows
        ),
        "reload_index_nanos": max(int(row["reload_index_nanos"]) for row in rows),
        "retained_heap_before_bytes": max(
            int(row["retained_heap_before_bytes"]) for row in rows
        ),
        "retained_heap_after_bytes": max(
            int(row["retained_heap_after_bytes"]) for row in rows
        ),
        "network_sync_bytes": max(int(row["network_sync_bytes"]) for row in rows),
        "transfer_conservation_ok": conservation,
        "route_cap_exceeded": cap_exceeded,
        "zero_filled": False,
        "identity": rows[0].get("identity"),
        "environment": rows[0].get("environment"),
        "seed": rows[0].get("seed"),
        "warmup_ticks": rows[0].get("warmup_ticks"),
        "sampling_ticks": rows[0].get("sampling_ticks"),
    }


def pending_axis(name: str) -> dict[str, Any]:
    return {
        "name": name,
        "status": "pending",
        "value": None,
        "zero_filled": False,
    }


def run_cli(write, check, output: Path) -> int:
    import argparse
    import sys

    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(output)} "
                f"status={document.get('status')}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(output)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        return 1
