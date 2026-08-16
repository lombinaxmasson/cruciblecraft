#!/usr/bin/env python3
"""Build the T24 finding ledger artifact.

T24c card: hand every reproducible finding to T25 with an evidence
class, a reproduce command, a failure boundary and a disposition.
``blocks_beta`` is true only for findings with reproducible evidence;
measurement noise and cross-machine comparisons are never blocking.
The finding rows are authored conclusions of this card (not generated
data); the builder validates them, resolves every evidence pointer
against the referenced artifact and derives the counts.

``status`` is "FINDINGS_LEDGER_READY" only when every row is complete,
every pointer resolves and no row claims blocking; otherwise the key is
written as null (fail-closed, never "").
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

FINDINGS: list[dict[str, Any]] = [
    {
        "id": "T24-F001",
        "title": (
            "500-pipe position-phase schedule yields exactly 100 due "
            "pipes per tick - verified, no action"
        ),
        "evidence_class": "STATIC_INFERENCE",
        "evidence_artifact":
            "tools/t24_scale_evidence.json#/scenarios/target/"
            "pipes_due_per_tick_max",
        "reproduce_command":
            ".\\gradlew.bat test --tests "
            "\"*ScaleWorkloadBoundTest.targetScenarioSchedules"
            "ExactlyOneHundredDuePipesPerTick\"",
        "blocks_beta": False,
        "disposition": "non_blocking",
        "failure_boundary": (
            "PipeTransferPhase.INTERVAL change or a regression in the "
            "5-tick residue partition math; the boundary is the exact "
            "100-due-pipes-per-tick invariant at 500 consecutive "
            "positions"
        ),
        "owner": "T25",
    },
    {
        "id": "T24-F002",
        "title": (
            "Multiblock validation is element-linear and bounded at 2 "
            "ops per position (tank 54, tower 162)"
        ),
        "evidence_class": "SYNTHETIC_BENCHMARK",
        "evidence_artifact":
            "tools/t24_scale_evidence.json#/scenarios/small/"
            "structure_validation_ops_max",
        "reproduce_command":
            ".\\gradlew.bat test --tests "
            "\"*ScaleWorkloadBoundTest.scenarioMultiblock"
            "ValidationOpsAreBounded\"",
        "blocks_beta": False,
        "disposition": "non_blocking",
        "failure_boundary": (
            "MultiblockStructureValidator change breaking isLoaded == "
            "blockState == positions or blockEntity == port count"
        ),
        "owner": "T25",
    },
    {
        "id": "T24-F003",
        "title": (
            "Route discovery cap (32,768 visited pipes) is declared "
            "but not measured at scale"
        ),
        "evidence_class": "STATIC_INFERENCE",
        "evidence_artifact":
            "tools/t24_scale_evidence.json#/measured_at_scale/target",
        "reproduce_command":
            "python tools/build_t24_scale_bounds.py --check",
        "blocks_beta": False,
        "disposition": "non_blocking",
        "skipped_measurement": True,
        "failure_boundary": (
            "The declared cap is enforced by "
            "ItemPipeNetworkTraversal.MAX_VISITED_PIPES; no scenario "
            "has been exercised at scale against it - recheck at T26 "
            "Beta candidate with a declared environment"
        ),
        "owner": "T25",
    },
    {
        "id": "T24-F004",
        "title": (
            "Cover configuration payload bounded at 13 bytes (source "
            "constant + serialization smoke)"
        ),
        "evidence_class": "STATIC_INFERENCE",
        "evidence_artifact":
            "tools/t24_scale_evidence.json#/declared_caps/"
            "cover_configuration_payload_bytes",
        "reproduce_command":
            ".\\gradlew.bat test --tests "
            "\"*ScaleWorkloadBoundTest.scenarioCoverPayload"
            "FitsThirteenByteBound\"",
        "blocks_beta": False,
        "disposition": "non_blocking",
        "failure_boundary": (
            "CoverConfigurationPayload codec change; the boundary is "
            "MAX_ENCODED_BYTES = 13"
        ),
        "owner": "T25",
    },
    {
        "id": "T24-F005",
        "title": (
            "target/stress wall-clock, retained-memory and "
            "network-sync are not measured (explicit SKIP)"
        ),
        "evidence_class": "MEASURED_AT_SCALE",
        "evidence_artifact":
            "tools/t24_scale_evidence.json#/measured_at_scale/stress",
        "reproduce_command":
            "re-run per the replacement_condition recorded in "
            "tools/t24_scale_evidence.json measured_at_scale",
        "blocks_beta": False,
        "disposition": "non_blocking",
        "skipped_measurement": True,
        "failure_boundary": (
            "None - blocked_conclusions are enumerated and no value is "
            "zero-filled; the measurement gap is a declared SKIP"
        ),
        "owner": "T25",
    },
]
REQUIRED_FIELDS = (
    "id",
    "title",
    "evidence_class",
    "evidence_artifact",
    "reproduce_command",
    "blocks_beta",
    "disposition",
    "failure_boundary",
    "owner",
)


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t24_findings.json"
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


def _resolve_pointer(pointer: str) -> Any:
    """Resolve a JSON pointer (artifact#/json/pointer) to a value."""
    artifact_path, _, json_pointer = pointer.partition("#")
    target = ROOT / artifact_path
    if not target.is_file():
        raise ValueError(f"evidence artifact missing: {artifact_path}")
    document = json.loads(target.read_text(encoding="utf-8"))
    value: Any = document
    for part in filter(None, json_pointer.split("/")):
        if isinstance(value, dict):
            value = value.get(part)
        elif isinstance(value, list) and part.isdigit():
            value = value[int(part)]
        else:
            value = None
        if value is None:
            raise ValueError(f"unresolvable pointer: {pointer}")
    return value


def build() -> dict[str, Any]:
    rows = []
    for finding in FINDINGS:
        missing = [field for field in REQUIRED_FIELDS
                   if finding.get(field) in (None, "")]
        if missing:
            raise ValueError(
                f"{finding.get('id')}: missing fields {missing}")
        if finding["blocks_beta"] is True:
            raise ValueError(
                f"{finding['id']}: blocking findings need reproducible "
                "measured evidence; none is declared in T24")
        if finding["evidence_class"] not in (
                "STATIC_INFERENCE",
                "SYNTHETIC_BENCHMARK",
                "MEASURED_AT_SCALE"):
            raise ValueError(
                f"{finding['id']}: unknown evidence_class "
                f"{finding['evidence_class']!r}")
        _resolve_pointer(finding["evidence_artifact"])
        rows.append(dict(finding))

    blocking = sum(1 for row in rows if row["blocks_beta"])
    skipped = sum(1 for row in rows if row.get("skipped_measurement"))
    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "build_t24_findings",
        "status": "FINDINGS_LEDGER_READY" if rows else None,
        "policy": (
            "Only findings with reproducible evidence may be handed to "
            "T25 as blocking. Measurement noise and cross-machine "
            "comparisons are never blocking."
        ),
        "findings": rows,
        "counts": {
            "total": len(rows),
            "blocking": blocking,
            "non_blocking": len(rows) - blocking,
            "skipped_measurement": skipped,
        },
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
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
        print(f"T24 findings failed: {error}", file=sys.stderr)
        return 1
    summary = {"schema_version": document.get("schema_version")}
    if "status" in document and document["status"]:
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
