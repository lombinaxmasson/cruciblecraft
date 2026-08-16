#!/usr/bin/env python3
"""Build the T23 multiblock load-bounds artifact.

D1 card: per-structure worst-case operation bounds (structure
validation, port scanning, tick amortization, network sync) from the
committed measured evidence (tools/t23_load_evidence.json, produced by
the JUnit MultiblockLoadBoundTest worst-of-N run). Deterministic
operation counts are hard gates; wall-clock is recorded for reference
only and is never gated (CI jitter).

G2 skeleton: the evidence did not exist yet, so ``build()`` emitted a
fail-closed placeholder without a status; ``check()`` stayed green
against the committed placeholder until the D1 evidence landed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t23_load_bounds.json"
EVIDENCE = TOOLS / "t23_load_evidence.json"
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
    structures = evidence.get("structures") or {}
    validation = evidence.get("validation") or {}

    measured = (
        evidence.get("status") == "MEASURED"
        and evidence.get("worst_case") is True
        and validation.get("accessor_calls", 0)
        == 2 * validation.get("worst_case_positions", -1)
        and all(
            row.get("validation_ops_max", 0) > 0
            and row.get("port_scan_ops_max", 0) > 0
            and row.get("tick_amortized_ops_max", 0) > 0
            for row in structures.values()
        )
    )

    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "build_t23_load_bounds",
        "worst_case": bool(evidence.get("worst_case", False)),
        "status": "MEASURED" if measured else None,
        "structures": structures,
        "validation": validation,
        "network_sync": evidence.get("network_sync") or {},
        "port_scan": evidence.get("port_scan") or {},
        "measured_by": evidence.get("measured_by"),
        "policy": (
            "Deterministic operation counts are hard gates; wall-clock "
            "elapsed is recorded for reference only and is never gated. "
            "Bounds are worst-case, never averaged."
        ),
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
                _relative(EVIDENCE): _sha256(EVIDENCE),
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
        print(f"T23 load bounds failed: {error}", file=sys.stderr)
        return 1
    summary = {"schema_version": document.get("schema_version")}
    if "status" in document and document["status"]:
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
