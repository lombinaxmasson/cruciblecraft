#!/usr/bin/env python3
"""Build T31 T14-axis load rollup.

Combines T30 opening publication (delta 0), T14 selected-1x measurements from
the last full verification report, and T31 target/stress scale evidence.
Package size stays pending until the R8 rc.1 jar/zip.
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

OUTPUT = t31.TOOLS / "t31_load_report.json"
SCALE = t31.TOOLS / "t31_scale_recheck.json"
T14 = t31.TOOLS / "t14_load_budget_policy.json"
DELTA = t31.TOOLS / "t30_publication_delta.json"
T30_LOAD = t31.TOOLS / "t30_load_projection.json"
REPORT = t31.REPORT
BUILDER = Path(__file__).resolve()
REPORT_OWNED = ("currentness",)
EAGER_HARD = 21_000
DATAPACK_HARD = 6_600
NS_PER_MS = 1_000_000.0
T14_1X = "tools/full_verification_report.json#t14_load_acceptance.measurements.selected_1x"


def _verdict(value: int | float | None, soft: int | float | None, hard: int | float | None) -> str | None:
    if value is None:
        return None
    if hard is not None and value > hard:
        return "HARD_CEILING_EXCEEDED"
    if soft is not None and value > soft:
        return "SOFT_BUDGET_EXCEEDED"
    return "PASS"


def _axis(
    name: str,
    *,
    base: int | float | None,
    delta: int | float | None,
    measured: int | float | None,
    soft: int | float | None,
    hard: int | float | None,
    evidence: str,
    extra: dict[str, Any] | None = None,
) -> dict[str, Any]:
    status = "pending" if measured is None else "measured"
    row: dict[str, Any] = {
        "base": base,
        "delta": delta,
        "evidence": evidence,
        "hard_ceiling": hard,
        "measured": measured,
        "name": name,
        "soft_budget": soft,
        "status": status,
        "verdict": _verdict(measured, soft, hard),
        "zero_filled": False,
    }
    if extra:
        row.update(extra)
    return row


def _budget(budgets: dict[str, Any], name: str) -> tuple[int | float, int | float]:
    row = budgets[name]
    return row["soft_budget"], row["hard_ceiling"]


def _package_size_axis() -> dict[str, Any]:
    version = t31.gradle_mod_version()
    jar = ROOT / "build" / "libs" / f"cruciblecraft-{version}.jar"
    archive = ROOT / "build" / "distributions" / f"cruciblecraft-{version}.zip"
    pending = {
        "evidence": "R8 rc.1 jar/zip",
        "hard_ceiling": None,
        "jar_bytes": None,
        "measured": None,
        "name": "package_size",
        "soft_budget": None,
        "status": "pending",
        "verdict": None,
        "zero_filled": False,
        "zip_bytes": None,
    }
    if version != t31.TARGET_VERSION or not jar.is_file() or not archive.is_file():
        return pending
    jar_bytes = jar.stat().st_size
    zip_bytes = archive.stat().st_size
    return {
        "evidence": "build/libs and build/distributions rc.1 artifacts",
        "hard_ceiling": None,
        "jar_bytes": jar_bytes,
        "measured": jar_bytes,
        "name": "package_size",
        "soft_budget": None,
        "status": "measured",
        "verdict": "PASS",
        "zero_filled": False,
        "zip_bytes": zip_bytes,
    }


def build() -> dict[str, Any]:
    t14 = common.load_json(T14)
    delta = common.load_json(DELTA)
    scale = common.load_json(SCALE) if SCALE.is_file() else {}
    t30_load = common.load_json(T30_LOAD) if T30_LOAD.is_file() else {}
    report = common.load_json(REPORT) if REPORT.is_file() else {}
    budgets = t14["budgets"]
    selected = (
        ((report.get("t14_load_acceptance") or {}).get("measurements") or {}).get(
            "selected_1x"
        )
        or {}
    )
    tests = report.get("tests") or {}
    target = (scale.get("current") or {}).get("target") or {}
    stress = (scale.get("current") or {}).get("stress") or {}
    opening = delta["opening_publication"]
    eager_soft, eager_hard = _budget(budgets, "eager_publication_rows")
    datapack_soft, datapack_hard = _budget(budgets, "datapack_authored_entries")
    lazy_soft, lazy_hard = _budget(budgets, "lazy_logical_rows")
    cache_soft, cache_hard = _budget(budgets, "lazy_cache_ceiling_rows")
    sync_soft, sync_hard = _budget(budgets, "sync_bytes")
    s_reload_soft, s_reload_hard = _budget(budgets, "server_reload_ms")
    s_index_soft, s_index_hard = _budget(budgets, "server_index_ms")
    c_reload_soft, c_reload_hard = _budget(budgets, "client_reload_ms")
    c_index_soft, c_index_hard = _budget(budgets, "client_index_ms")
    retained_soft, retained_hard = _budget(budgets, "retained_memory_bytes")
    alloc_soft, alloc_hard = _budget(budgets, "allocation_bytes")
    lookup_soft, lookup_hard = _budget(budgets, "lookup_p95_ns")
    cand_soft, cand_hard = _budget(budgets, "lookup_candidate_count")
    datapack_base = ((t30_load.get("t14_axes") or {}).get("datapack_authored_entries") or {})
    datapack_measured = (
        int(datapack_base["projected"])
        if datapack_base.get("projected") is not None
        else None
    )
    eager_measured = int(opening["eager"]) + int(delta["measured"]["eager"])
    lazy_measured = int(opening["lazy"]) + int(delta["measured"]["lazy"])
    logical_measured = int(opening["logical"]) + int(delta["measured"]["logical"])
    axes = {
        "eager_publication_rows": _axis(
            "eager_publication_rows",
            base=int(opening["eager"]),
            delta=int(delta["measured"]["eager"]),
            measured=eager_measured,
            soft=eager_soft,
            hard=eager_hard,
            evidence="tools/t30_publication_delta.json",
            extra={"opening": int(opening["eager"])},
        ),
        "logical_publication_rows": _axis(
            "logical_publication_rows",
            base=int(opening["logical"]),
            delta=int(delta["measured"]["logical"]),
            measured=logical_measured,
            soft=None,
            hard=eager_hard,
            evidence="tools/t30_publication_delta.json",
        ),
        "lazy_logical_rows": _axis(
            "lazy_logical_rows",
            base=int(opening["lazy"]),
            delta=int(delta["measured"]["lazy"]),
            measured=lazy_measured,
            soft=lazy_soft,
            hard=lazy_hard,
            evidence="tools/t30_publication_delta.json",
        ),
        "datapack_authored_entries": _axis(
            "datapack_authored_entries",
            base=datapack_base.get("base"),
            delta=datapack_base.get("delta"),
            measured=datapack_measured,
            soft=datapack_soft,
            hard=datapack_hard,
            evidence="tools/t30_load_projection.json#t14_axes.datapack_authored_entries; T31 content delta 0",
        ),
        "lazy_cache_ceiling_rows": _axis(
            "lazy_cache_ceiling_rows",
            base=512,
            delta=0,
            measured=512,
            soft=cache_soft,
            hard=cache_hard,
            evidence=T14_1X + ".lookup_traces cache.ceiling; T31 content delta 0",
        ),
        "sync_bytes": _axis(
            "sync_bytes",
            base=selected.get("sync_bytes"),
            delta=0,
            measured=selected.get("sync_bytes"),
            soft=sync_soft,
            hard=sync_hard,
            evidence=T14_1X + ".sync_bytes",
        ),
        "server_reload_ms": _axis(
            "server_reload_ms",
            base=(selected["server_reload_p95_ns"] / NS_PER_MS)
            if selected.get("server_reload_p95_ns") is not None
            else None,
            delta=0,
            measured=(selected["server_reload_p95_ns"] / NS_PER_MS)
            if selected.get("server_reload_p95_ns") is not None
            else None,
            soft=s_reload_soft,
            hard=s_reload_hard,
            evidence=T14_1X + ".server_reload_p95_ns / 1e6",
        ),
        "server_index_ms": _axis(
            "server_index_ms",
            base=(selected["server_index_p95_ns"] / NS_PER_MS)
            if selected.get("server_index_p95_ns") is not None
            else None,
            delta=0,
            measured=(selected["server_index_p95_ns"] / NS_PER_MS)
            if selected.get("server_index_p95_ns") is not None
            else None,
            soft=s_index_soft,
            hard=s_index_hard,
            evidence=T14_1X + ".server_index_p95_ns / 1e6",
        ),
        "client_reload_ms": _axis(
            "client_reload_ms",
            base=(selected["dedicated_client_reexpansion_p95_ns"] / NS_PER_MS)
            if selected.get("dedicated_client_reexpansion_p95_ns") is not None
            else None,
            delta=0,
            measured=(selected["dedicated_client_reexpansion_p95_ns"] / NS_PER_MS)
            if selected.get("dedicated_client_reexpansion_p95_ns") is not None
            else None,
            soft=c_reload_soft,
            hard=c_reload_hard,
            evidence=T14_1X + ".dedicated_client_reexpansion_p95_ns / 1e6",
        ),
        "client_index_ms": _axis(
            "client_index_ms",
            base=(selected["dedicated_client_index_p95_ns"] / NS_PER_MS)
            if selected.get("dedicated_client_index_p95_ns") is not None
            else None,
            delta=0,
            measured=(selected["dedicated_client_index_p95_ns"] / NS_PER_MS)
            if selected.get("dedicated_client_index_p95_ns") is not None
            else None,
            soft=c_index_soft,
            hard=c_index_hard,
            evidence=T14_1X + ".dedicated_client_index_p95_ns / 1e6",
        ),
        "retained_memory_bytes": _axis(
            "retained_memory_bytes",
            base=selected.get("retained_total_bytes_p50"),
            delta=0,
            measured=selected.get("retained_total_bytes_p50"),
            soft=retained_soft,
            hard=retained_hard,
            evidence=T14_1X + ".retained_total_bytes_p50",
        ),
        "allocation_bytes": _axis(
            "allocation_bytes",
            base=selected.get("jfr_allocation_bytes_p50"),
            delta=0,
            measured=selected.get("jfr_allocation_bytes_p50"),
            soft=alloc_soft,
            hard=alloc_hard,
            evidence=T14_1X + ".jfr_allocation_bytes_p50",
        ),
        "lookup_p95_ns": _axis(
            "lookup_p95_ns",
            base=selected.get("lookup_p95_ns"),
            delta=0,
            measured=selected.get("lookup_p95_ns"),
            soft=lookup_soft,
            hard=lookup_hard,
            evidence=T14_1X + ".lookup_p95_ns",
        ),
        "lookup_candidate_count": _axis(
            "lookup_candidate_count",
            base=selected.get("lookup_candidates_p95"),
            delta=0,
            measured=selected.get("lookup_candidates_p95"),
            soft=cand_soft,
            hard=cand_hard,
            evidence=T14_1X + ".lookup_candidates_p95",
        ),
        "target_scale": _axis(
            "target_scale",
            base=None,
            delta=None,
            measured=target.get("visited_max"),
            soft=None,
            hard=None,
            evidence="tools/t31_scale_recheck.json#current.target",
            extra={
                "network_sync_bytes": target.get("network_sync_bytes"),
                "status": target.get("status") or "pending",
                "visited_max": target.get("visited_max"),
            },
        ),
        "stress_scale": _axis(
            "stress_scale",
            base=None,
            delta=None,
            measured=stress.get("visited_max"),
            soft=None,
            hard=None,
            evidence="tools/t31_scale_recheck.json#current.stress",
            extra={
                "network_sync_bytes": stress.get("network_sync_bytes"),
                "status": stress.get("status") or "pending",
                "visited_max": stress.get("visited_max"),
            },
        ),
        "package_size": _package_size_axis(),
        "test_totals": _axis(
            "test_totals",
            base=None,
            delta=None,
            measured=(tests.get("production_game_tests") or {}).get("required_tests"),
            soft=None,
            hard=None,
            evidence="tools/full_verification_report.json#tests",
            extra={
                "gametest": (tests.get("production_game_tests") or {}).get(
                    "required_tests"
                ),
                "junit": (tests.get("java_unit_tests") or {}).get("tests"),
                "python": (tests.get("python_unit_tests") or {}).get("tests"),
            },
        ),
        "registry_resource": _axis(
            "registry_resource",
            base=None,
            delta=0,
            measured=(t30_load.get("registration") or {}).get("blocks"),
            soft=None,
            hard=None,
            evidence="tools/t30_load_projection.json#registration; T31 content delta 0",
            extra={
                "registration": t30_load.get("registration"),
                "resources": t30_load.get("resources"),
            },
        ),
    }
    pending = [
        name
        for name, row in axes.items()
        if row.get("status") in ("pending", "PENDING_MEASUREMENT")
    ]
    hard_failures = [
        name
        for name, row in axes.items()
        if row.get("verdict") == "HARD_CEILING_EXCEEDED"
    ]
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(T14): common.sha256_file(T14),
        common.relative(DELTA): common.sha256_file(DELTA),
        "tools/t31_common.py": common.sha256_file(t31.TOOLS / "t31_common.py"),
    }
    if SCALE.is_file():
        owned_inputs[common.relative(SCALE)] = common.sha256_file(SCALE)
    if T30_LOAD.is_file():
        owned_inputs[common.relative(T30_LOAD)] = common.sha256_file(T30_LOAD)
    currentness_inputs = dict(owned_inputs)
    if REPORT.is_file():
        currentness_inputs[common.relative(REPORT)] = common.sha256_file(REPORT)
    status = "T31_LOAD_REPORT_COMPLETE" if not pending else "T31_LOAD_REPORT_PENDING"
    return {
        "axes": axes,
        "currentness": {"owned_inputs": currentness_inputs},
        "datapack_hard_ceiling": DATAPACK_HARD,
        "eager_hard_ceiling": EAGER_HARD,
        "generated_by": "python tools/build_t31_load_report.py --write",
        "hard_ceiling_failures": hard_failures,
        "opening_publication": opening,
        "owned_inputs": owned_inputs,
        "pending": pending,
        "publication_delta_measured": delta["measured"],
        "schema_version": 1,
        "status": status,
        "status_owner": "build_t31_load_report",
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
