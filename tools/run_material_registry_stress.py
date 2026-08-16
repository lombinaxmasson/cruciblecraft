#!/usr/bin/env python3
"""Run or validate the manual material-registry stress benchmark artifacts."""
from __future__ import annotations

import argparse
import json
import os
import platform
import re
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "tools" / "material_registry_stress_report.json"
BUDGET = ROOT / "tools" / "material_registry_budget.json"
SCENARIOS = ("metadata_only", "single_dust")


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def run_scenario(scenario: str) -> dict[str, Any]:
    command = [
        str(ROOT / "gradlew.bat"),
        "materialRegistryStress",
        f"-PstressScenario={scenario}",
        "--console=plain",
    ]
    completed = subprocess.run(
        command,
        cwd=ROOT,
        text=True,
        encoding="utf-8",
        errors="replace",
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )
    matches = re.findall(r"^STRESS_RESULT=(\{.*\})$", completed.stdout, re.MULTILINE)
    if completed.returncode or len(matches) != 1:
        print(completed.stdout, file=sys.stderr)
        raise RuntimeError(
            f"stress scenario {scenario} failed with exit code {completed.returncode}"
        )
    return json.loads(matches[0])


def run_full_server(scenario: str) -> dict[str, Any]:
    command = [
        str(ROOT / "gradlew.bat"),
        "runGameTestServer",
        f"-PmaterialStressScenario={scenario}",
        "-PmaterialStressCount=20000",
        "--console=plain",
    ]
    started = time.perf_counter()
    completed = subprocess.run(
        command,
        cwd=ROOT,
        text=True,
        encoding="utf-8",
        errors="replace",
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )
    elapsed_ms = round((time.perf_counter() - started) * 1000.0, 1)
    passed = completed.returncode == 0 and re.search(
        r"All \d+ required tests passed", completed.stdout
    )
    if not passed:
        print(completed.stdout, file=sys.stderr)
        raise RuntimeError(
            f"full server stress scenario {scenario} failed with "
            f"exit code {completed.returncode}"
        )
    return {
        "scenario": scenario,
        "synthetic_materials": 20_000,
        "elapsed_ms": elapsed_ms,
        "exit_code": completed.returncode,
        "game_tests_passed": True,
    }


def rounded_budget(value: float, *, minimum: int) -> int:
    return max(minimum, int(value * 2.0 + 999) // 1000 * 1000)


def build_budget(results: dict[str, dict[str, Any]]) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "policy": (
            "Manual 20k stress measurements must remain below 2x the committed "
            "baseline (rounded up); the wire protocol remains capped at 4096 entries."
        ),
        "limits": {
            "catalog_bootstrap_ms": rounded_budget(
                max(value["catalog_bootstrap_ms"] for value in results.values()),
                minimum=5_000,
            ),
            "generated_pack_ms": rounded_budget(
                max(value["generated_pack_ms"] for value in results.values()),
                minimum=5_000,
            ),
            "cold_cache_ms": rounded_budget(
                max(value["cold_cache_ms"] for value in results.values()),
                minimum=10_000,
            ),
            "hot_cache_ms": rounded_budget(
                max(value["hot_cache_ms"] for value in results.values()),
                minimum=2_000,
            ),
            "deferred_register_queue_ms": rounded_budget(
                max(
                    value["deferred_register_queue_ms"]
                    for value in results.values()
                ),
                minimum=5_000,
            ),
            "handshake_entry_cap": 4_096,
            "handshake_4000_round_trip_ms": rounded_budget(
                max(
                    value["handshake_4000_round_trip_ms"]
                    for value in results.values()
                ),
                minimum=2_000,
            ),
            "max_generated_pack_bytes": max(
                100_000_000,
                max(value["generated_pack_bytes"] for value in results.values()) * 2,
            ),
        },
        "required_assertions": {
            "synthetic_materials": 20_000,
            "metadata_only_registered_items": 0,
            "single_dust_registered_items": 20_000,
            "over_cap_encode_or_decode_must_fail": True,
            "within_cap_round_trip_entries": 4_000,
        },
    }


def validate(report: dict[str, Any], budget: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    results = report.get("scenarios") or {}
    if set(results) != set(SCENARIOS):
        errors.append("stress report must contain both benchmark scenarios")
        return errors
    limits = budget["limits"]
    required = budget["required_assertions"]
    for scenario, result in results.items():
        if result["synthetic_materials"] != required["synthetic_materials"]:
            errors.append(f"{scenario}: wrong synthetic material count")
        for metric in (
            "catalog_bootstrap_ms",
            "generated_pack_ms",
            "cold_cache_ms",
            "hot_cache_ms",
            "deferred_register_queue_ms",
            "handshake_4000_round_trip_ms",
        ):
            if result[metric] > limits[metric]:
                errors.append(
                    f"{scenario}: {metric}={result[metric]} exceeds {limits[metric]}"
                )
        if result["generated_pack_bytes"] > limits["max_generated_pack_bytes"]:
            errors.append(f"{scenario}: generated pack exceeds byte budget")
        if not result["handshake_20k_expected_failure"]:
            errors.append(f"{scenario}: over-cap handshake unexpectedly encoded")
        if (
            result["handshake_4000_round_trip_entries"]
            != required["within_cap_round_trip_entries"]
        ):
            errors.append(f"{scenario}: within-cap handshake round trip failed")
    if (
        results["metadata_only"]["registered_synthetic_items"]
        != required["metadata_only_registered_items"]
    ):
        errors.append("metadata_only: registered synthetic item count is nonzero")
    if (
        results["single_dust"]["registered_synthetic_items"]
        != required["single_dust_registered_items"]
    ):
        errors.append("single_dust: registered synthetic item count is not 20k")
    if limits["handshake_entry_cap"] != 4_096:
        errors.append("handshake cap must remain 4096")
    full_server_runs = report.get("full_server_runs") or {}
    if set(full_server_runs) != set(SCENARIOS):
        errors.append("stress report must contain both full-server stress runs")
    else:
        for scenario, result in full_server_runs.items():
            if not result.get("game_tests_passed") or result.get("exit_code") != 0:
                errors.append(f"{scenario}: full server stress run failed")
            if result["elapsed_ms"] > limits["full_server_elapsed_ms"]:
                errors.append(f"{scenario}: full server startup exceeds budget")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--write-full-server", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if sum((args.write, args.write_full_server, args.check)) != 1:
        parser.error("choose exactly one mutation or check mode")
    if args.write:
        results = {scenario: run_scenario(scenario) for scenario in SCENARIOS}
        report = {
            "schema_version": 1,
            "measured_at": datetime.now(timezone.utc).isoformat(),
            "environment": {
                "os": platform.platform(),
                "python": platform.python_version(),
                "processor": platform.processor(),
                "logical_cpus": os.cpu_count(),
            },
            "scope": {
                "full_dedicated_join_20k": (
                    "expected protocol rejection; StructurePayload encode/decode cap is 4096"
                ),
                "within_cap_sync": (
                    "actual StructurePayload codec encode/decode round trip at 4000 entries"
                ),
            },
            "scenarios": results,
        }
        budget = build_budget(results)
        REPORT.write_text(stable_json(report), encoding="utf-8", newline="\n")
        BUDGET.write_text(stable_json(budget), encoding="utf-8", newline="\n")
        print(f"Wrote {REPORT}")
        print(f"Wrote {BUDGET}")
        return 0
    if args.write_full_server:
        if not REPORT.is_file() or not BUDGET.is_file():
            print("run --write before --write-full-server", file=sys.stderr)
            return 1
        report = json.loads(REPORT.read_text(encoding="utf-8"))
        full_server_runs = {
            scenario: run_full_server(scenario) for scenario in SCENARIOS
        }
        report["full_server_runs"] = full_server_runs
        budget = json.loads(BUDGET.read_text(encoding="utf-8"))
        budget["limits"]["full_server_elapsed_ms"] = rounded_budget(
            max(value["elapsed_ms"] for value in full_server_runs.values()),
            minimum=300_000,
        )
        REPORT.write_text(stable_json(report), encoding="utf-8", newline="\n")
        BUDGET.write_text(stable_json(budget), encoding="utf-8", newline="\n")
        print(f"Wrote full-server measurements to {REPORT}")
        print(f"Updated full-server budget in {BUDGET}")
        return 0
    if not REPORT.is_file() or not BUDGET.is_file():
        print("stress artifacts are missing; run with --write", file=sys.stderr)
        return 1
    errors = validate(
        json.loads(REPORT.read_text(encoding="utf-8")),
        json.loads(BUDGET.read_text(encoding="utf-8")),
    )
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("material registry stress report satisfies committed budget")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
