#!/usr/bin/env python3
"""Build T31 target/stress scale recheck evidence.

Historical T24 SKIP fields stay in tools/t24_scale_evidence.json.
This builder only reads them to prove they were not rewritten.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402
from tools import t31_common as t31  # noqa: E402

OUTPUT = t31.TOOLS / "t31_scale_recheck.json"
POLICY = t31.TOOLS / "t31_rc_policy.json"
BUILDER = Path(__file__).resolve()
REPORT_OWNED = ("currentness",)


def _finding(current: dict[str, Any], scenario_keys: tuple[str, ...]) -> dict[str, Any]:
    measured = all(
        current[name]["status"] == "MEASURED_AT_SCALE" for name in scenario_keys
    )
    cap_ok = all(
        current[name].get("route_cap_exceeded") is False for name in scenario_keys
    ) if measured else False
    conservation = all(
        current[name].get("transfer_conservation_ok") is True for name in scenario_keys
    ) if measured else False
    network = all(
        isinstance(current[name].get("network_sync_bytes"), int)
        and current[name]["network_sync_bytes"] > 0
        for name in scenario_keys
    ) if measured else False
    wall = all(
        current[name].get("tick_wall_clock_p50_nanos") not in (None, 0)
        for name in scenario_keys
    ) if measured else False
    memory = all(
        current[name].get("retained_heap_after_bytes") not in (None, 0)
        for name in scenario_keys
    ) if measured else False
    return {
        "measured": measured,
        "route_cap_ok": cap_ok,
        "conservation_ok": conservation,
        "network_sync_measured": network,
        "wall_clock_measured": wall,
        "retained_memory_measured": memory,
    }


def build() -> dict[str, Any]:
    policy = common.load_json(POLICY)
    historical = t31.historical_t24_scale_skip()
    if historical["target_status"] != "SKIP" or historical["stress_status"] != "SKIP":
        raise ValueError("T24 historical SKIP fields were rewritten")
    current = {
        name: t31.summarize_samples(name) for name in ("target", "stress")
    }
    small = t31.summarize_samples("small")
    findings = t31.historical_f003_f005()
    f003 = _finding(current, ("target", "stress"))
    f005 = {
        "measured": f003["measured"],
        "network_sync_measured": f003["network_sync_measured"],
        "wall_clock_measured": f003["wall_clock_measured"],
        "retained_memory_measured": f003["retained_memory_measured"],
        "zero_filled": any(
            current[name].get("zero_filled") for name in ("target", "stress")
        ),
    }
    if current["target"]["status"] != "MEASURED_AT_SCALE":
        f003_status = "OPEN"
        f005_status = "OPEN"
        blocked = "BLOCKED_PENDING_TARGET_MEASUREMENT"
    elif current["stress"]["status"] != "MEASURED_AT_SCALE":
        f003_status = "OPEN"
        f005_status = "OPEN"
        blocked = "BLOCKED_PENDING_STRESS_MEASUREMENT"
    elif not f003["route_cap_ok"] or not f003["conservation_ok"]:
        f003_status = "BLOCKER"
        f005_status = "OPEN"
        blocked = "BLOCKED_SCALE_GATE"
    elif not f005["network_sync_measured"] or not f005["wall_clock_measured"] or not f005["retained_memory_measured"]:
        f003_status = "CLOSED"
        f005_status = "OPEN"
        blocked = "BLOCKED_PENDING_F005_METRICS"
    else:
        f003_status = "CLOSED"
        f005_status = "CLOSED"
        blocked = None
    ram = policy["opening"]["physical_ram_bytes"]
    env_ok = ram >= t31.MIN_PHYSICAL_RAM_BYTES
    if not env_ok:
        blocked = "BLOCKED_ENVIRONMENT"
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(POLICY): common.sha256_file(POLICY),
        common.relative(t31.MANIFEST): common.sha256_file(t31.MANIFEST),
        common.relative(t31.T24_EVIDENCE): common.sha256_file(t31.T24_EVIDENCE),
        common.relative(t31.T26_ISSUES): common.sha256_file(t31.T26_ISSUES),
        "tools/t31_common.py": common.sha256_file(t31.TOOLS / "t31_common.py"),
    }
    status = "T31_SCALE_RECHECK_COMPLETE" if blocked is None else "T31_SCALE_RECHECK_PENDING"
    return {
        "blocked": blocked,
        "current": current,
        "currentness": {"owned_inputs": owned_inputs},
        "environment": {
            "physical_ram_bytes": ram,
            "physical_ram_meets_16gib": env_ok,
            "physical_ram_not_xmx": True,
        },
        "f003": {
            **findings["T24-F003"],
            "current_status": f003_status,
            "current_artifact": "tools/t31_scale_recheck.json",
            **f003,
        },
        "f005": {
            **findings["T24-F005"],
            "current_status": f005_status,
            "current_artifact": "tools/t31_scale_recheck.json",
            **f005,
        },
        "generated_by": "python tools/build_t31_scale_recheck.py --write",
        "historical_t24": historical,
        "owned_inputs": owned_inputs,
        "route_discovery_cap": t31.ROUTE_CAP,
        "schema_version": 1,
        "small_harness": small,
        "status": status,
        "status_owner": "build_t31_scale_recheck",
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = build()
    actual = json.loads(OUTPUT.read_text(encoding="utf-8"))
    for key in REPORT_OWNED:
        expected.pop(key, None)
        actual.pop(key, None)
    if common.stable_json(expected) != common.stable_json(actual):
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"status={document.get('status')}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
