#!/usr/bin/env python3
"""Build the T24 workload manifest artifact.

T24a card: deterministically derive a workload manifest from the
authored scenario policy (tools/t24_workload_policy.json) and pin a
scenario identity fingerprint (workload_identity). The manifest is the
single data description the CI hard gates consume; scenarios are
rebuildable from an empty directory because policy + builder fully
determine the document.

Derivation rules (STATIC_INFERENCE):
- pipes_total = fluid_pipes + item_pipes;
- the pipe scheduler is position-phased on a 5-tick interval
  (PipeTransferPhase), so pipes_due_per_tick = pipes_total // 5 exactly
  when pipes_partition_exact holds (all declared totals are divisible
  by 5 by design);
- workload_identity = sha256 over the scenarios block only, excluding
  currentness, so identical inputs always yield the identical
  fingerprint.

``status`` is "MANIFEST_COMPLETE" only when all three scenarios are
present, internally consistent and partition-exact; otherwise the key
is written as null (fail-closed, never "").
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

REQUIRED_SCENARIOS = ("small", "target", "stress")
COUNT_KEYS = (
    "processing_machines",
    "energy_converters",
    "fluid_pipes",
    "item_pipes",
    "covers",
    "multiblocks",
    "petroleum_chains",
)


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t24_workload_manifest.json"
POLICY = TOOLS / "t24_workload_policy.json"
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


def _complete(scenarios: dict[str, Any]) -> bool:
    if set(scenarios) != set(REQUIRED_SCENARIOS):
        return False
    for name in REQUIRED_SCENARIOS:
        row = scenarios[name]
        counts = row.get("counts") or {}
        composition = row.get("composition") or {}
        ticks = row.get("ticks") or {}
        derived = row.get("derived") or {}
        if not row.get("purpose") or not isinstance(row.get("seed"), int):
            return False
        chunks = row.get("loaded_chunks") or {}
        if chunks.get("x", 0) <= 0 or chunks.get("z", 0) <= 0:
            return False
        if any(counts.get(key, 0) <= 0 for key in COUNT_KEYS[:-1]):
            return False
        if counts.get("petroleum_chains", -1) < 0:
            return False
        pairs = composition.get("converter_pairs") or {}
        if sum(pairs.values()) != counts.get("energy_converters", -1):
            return False
        machines = composition.get("processing_machines") or {}
        if (
            machines.get("RU", 0) + machines.get("KU", 0)
            + machines.get("EU", 0)
        ) != counts.get("processing_machines", -1):
            return False
        if not composition.get("multiblock_ids"):
            return False
        if ticks.get("warmup", 0) <= 0 or ticks.get("sampling", 0) <= 0:
            return False
        if not row.get("declared_evidence_class"):
            return False
        if derived.get("pipes_partition_exact") is not True:
            return False
        if (
            derived.get("pipes_due_per_tick", -1)
            != derived.get("pipes_total", -2) // 5
        ):
            return False
    return True


def build() -> dict[str, Any]:
    policy = _load_if_exists(POLICY) or {}
    scenarios: dict[str, Any] = {}
    for name, row in sorted((policy.get("scenarios") or {}).items()):
        counts = row.get("counts") or {}
        pipes_total = counts.get("fluid_pipes", 0) + counts.get(
            "item_pipes", 0
        )
        scenarios[name] = {
            **row,
            "derived": {
                "pipes_total": pipes_total,
                # 5-tick position-phased schedule: exactly 1/5 due each tick
                "pipes_due_per_tick": pipes_total // 5,
                "pipes_partition_exact": pipes_total % 5 == 0,
            },
        }

    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "build_t24_workload_manifest",
        "fidelity_class": policy.get("fidelity_class"),
        "scenarios": scenarios,
        "status": "MANIFEST_COMPLETE" if _complete(scenarios) else None,
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
                _relative(POLICY): _sha256(POLICY),
            }
        },
    }
    # Scenario identity fingerprint: excludes currentness so identical
    # inputs always yield the identical fingerprint.
    document["workload_identity"] = hashlib.sha256(
        _stable({"scenarios": scenarios}).encode("utf-8")
    ).hexdigest()
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
        print(f"T24 workload manifest failed: {error}", file=sys.stderr)
        return 1
    summary = {"schema_version": document.get("schema_version")}
    if "status" in document and document["status"]:
        summary["status"] = document["status"]
    summary["workload_identity"] = document.get("workload_identity")
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
