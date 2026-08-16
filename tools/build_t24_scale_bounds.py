#!/usr/bin/env python3
"""Build the T24 scale-bounds artifact.

T24b card: derive the scenario-level bounded-count ledger from the
committed measured evidence (tools/t24_scale_evidence.json, the
transcript of the JUnit ScaleWorkloadBoundTest exact-count run) and
cross-check it against the generated workload manifest. Deterministic
operation counts are hard gates; wall-clock is never gated.

``status`` is "MEASURED" only when the evidence is MEASURED,
worst-case, all three scenarios are present, every scenario row has
positive values that stay within the declared caps, and the due-pipe
values agree with the manifest's partition derivation; otherwise the
key is written as null (fail-closed, never "").
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

REQUIRED_SCENARIOS = ("small", "target", "stress")
SCENARIO_FIELDS = (
    "pipes_due_per_tick_max",
    "route_discovery_visited_max",
    "route_cache_entries_per_pipe_max",
    "port_scan_ops_max",
    "structure_validation_ops_max",
    "cover_configuration_payload_bytes_max",
    "menu_container_data_ints",
)
CAP_FIELD_TO_DECLARED = {
    "route_discovery_visited_max": "route_discovery_visited",
    "route_cache_entries_per_pipe_max": "route_cache_entries_per_item_pipe",
    "cover_configuration_payload_bytes_max":
        "cover_configuration_payload_bytes",
}


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t24_scale_bounds.json"
EVIDENCE = TOOLS / "t24_scale_evidence.json"
MANIFEST = TOOLS / "t24_workload_manifest.json"
BUILDER = Path(__file__).resolve()


def _load_if_exists(path: Path, default: Any = None) -> Any:
    if path.is_file():
        return json.loads(path.read_text(encoding="utf-8"))
    return default


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    if not path.is_file():
        return hashlib.sha256(b"").hexdigest()
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def build() -> dict[str, Any]:
    evidence = _load_if_exists(EVIDENCE) or {}
    manifest = _load_if_exists(MANIFEST) or {}
    scenarios = evidence.get("scenarios") or {}
    declared_caps = evidence.get("declared_caps") or {}
    manifest_scenarios = manifest.get("scenarios") or {}

    due_matches_manifest = all(
        name in manifest_scenarios
        and row.get("pipes_due_per_tick_max")
        == (manifest_scenarios[name].get("derived") or {}).get(
            "pipes_due_per_tick"
        )
        for name, row in scenarios.items()
    )
    measured = (
        evidence.get("status") == "MEASURED"
        and evidence.get("worst_case") is True
        and set(scenarios) == set(REQUIRED_SCENARIOS)
        and due_matches_manifest
        and all(
            all(row.get(field, 0) > 0 for field in SCENARIO_FIELDS)
            for row in scenarios.values()
        )
        and all(
            row.get(field, 0)
            <= declared_caps.get(cap_field, -1)
            for row in scenarios.values()
            for field, cap_field in CAP_FIELD_TO_DECLARED.items()
        )
        and all(
            row.get("structure_validation_ops_max", 0)
            <= 2 * declared_caps.get("multiblock_scan_volume", -1)
            for row in scenarios.values()
        )
    )

    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "build_t24_scale_bounds",
        "worst_case": bool(evidence.get("worst_case", False)),
        "status": "MEASURED" if measured else None,
        "scenarios": scenarios,
        "declared_caps": declared_caps,
        "measured_by": evidence.get("measured_by"),
        "policy": (
            "Deterministic operation counts are hard gates; wall-clock "
            "elapsed is recorded for reference only and is never gated. "
            "Bounds are worst-case, never averaged. Uncollectable "
            "metrics are explicit SKIPs, never zero-filled."
        ),
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
                _relative(EVIDENCE): _sha256(EVIDENCE),
                _relative(MANIFEST): _sha256(MANIFEST),
            }
        },
    }
    return document


def check() -> list[str]:
    """Return staleness / integrity errors (empty = clean)."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load_if_exists(OUTPUT)
    expected = build()
    if _stable(on_disk) != _stable(expected):
        errors.append(f"stale generated file: {_relative(OUTPUT)}")
    return errors


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8", newline="\n")
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load_if_exists(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T24 scale bounds failed: {error}", file=sys.stderr)
        return 1
    summary = {"schema_version": document.get("schema_version")}
    if "status" in document and document["status"]:
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
