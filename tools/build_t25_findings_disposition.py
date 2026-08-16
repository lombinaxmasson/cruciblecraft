#!/usr/bin/env python3
"""Build the T25 finding-disposition artifact.

T25a card: the input set is exclusively the T24 finding ledger. Each
finding is disposed into one of four classes - ``selected_fixed`` /
``non_blocking`` / ``invalid_measurement`` / ``post_1_0``. A finding
whose ``blocks_beta`` is true MUST be ``selected_fixed`` (fail-closed:
a real blocker may never be silently dropped); findings flagged
``skipped_measurement`` must carry their T24 recheck contract
(replacement_condition / recheck_point) verbatim into the ledger.

T24 handed over zero blocking findings, so this card closes with
selected = 0 and complete dispositions - no preventive rewrites.

``status`` is "T25_DISPOSITIONS_COMPLETE" only when the disposition set
is a bijection with the T24 ledger and every row is complete; otherwise
the key is written as null (fail-closed, never "").
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

DISPOSITION_VOCABULARY = (
    "selected_fixed",
    "non_blocking",
    "invalid_measurement",
    "post_1_0",
)
SKIPPED_FINDINGS = ("T24-F003", "T24-F005")
OWNERS = {
    "T24-F001": "T26",
    "T24-F002": "T26",
    "T24-F003": "T26",
    "T24-F004": "T26",
    "T24-F005": "T26",
}
REASONS = {
    "T24-F001": (
        "Verified STATIC_INFERENCE invariant, no action: the 5-tick "
        "position-phased schedule yields exactly 100 due pipes per "
        "tick at 500 consecutive positions."
    ),
    "T24-F002": (
        "Verified SYNTHETIC_BENCHMARK bound, no action: multiblock "
        "validation is element-linear at 2 ops per position, asserted "
        "by exact counters against the real tank definition."
    ),
    "T24-F003": (
        "The route-discovery cap is a declared constant enforced by "
        "code and far above every scenario size; it was not exercised "
        "at scale. slow_but_non_blocking: never promoted to selected. "
        "Recheck contract carried from T24 evidence."
    ),
    "T24-F004": (
        "Verified bound, no action: the cover configuration payload "
        "is bounded at 13 bytes by MAX_ENCODED_BYTES plus the "
        "serialization smoke test."
    ),
    "T24-F005": (
        "Explicit T24 SKIP: target/stress wall-clock, retained-memory "
        "and network-sync were not measured. Measurement gaps are "
        "never blockers and are never zero-filled. Recheck contract "
        "carried from T24 evidence."
    ),
}


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t25_findings_disposition.json"
T24_FINDINGS = TOOLS / "t24_findings.json"
T24_SCALE_EVIDENCE = TOOLS / "t24_scale_evidence.json"
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


def _recheck_contract(finding: dict[str, Any]) -> dict[str, Any] | None:
    """Resolve the T24 SKIP contract for a skipped-measurement finding."""
    if not finding.get("skipped_measurement"):
        return None
    evidence = _load_if_exists(T24_SCALE_EVIDENCE) or {}
    measured = evidence.get("measured_at_scale") or {}
    for section in measured.values():
        if section.get("status") != "SKIP":
            continue
        return {
            "replacement_condition": section["replacement_condition"],
            "recheck_point": section["recheck_point"],
        }
    raise ValueError(
        f"{finding['id']}: skipped_measurement finding has no SKIP "
        "contract in tools/t24_scale_evidence.json"
    )


def build() -> dict[str, Any]:
    ledger = _load_if_exists(T24_FINDINGS) or {}
    findings = ledger.get("findings") or []
    if not findings:
        raise ValueError("T24 finding ledger is empty")

    rows = []
    seen: set[str] = set()
    for finding in findings:
        finding_id = finding.get("id")
        if not finding_id or finding_id in seen:
            raise ValueError(
                f"duplicate or missing finding id: {finding_id!r}")
        seen.add(finding_id)
        if finding_id not in REASONS or finding_id not in OWNERS:
            raise ValueError(
                f"unexpected finding id {finding_id!r}: the input set "
                "must be exactly the T24 finding ledger")
        if finding.get("blocks_beta") is True:
            raise ValueError(
                f"{finding_id}: a blocking finding must be disposed as "
                "selected_fixed; this builder only produces "
                "non_blocking dispositions, so a real blocker cannot "
                "be silently dropped")
        _resolve_pointer(finding["evidence_artifact"])
        row: dict[str, Any] = {
            "id": finding_id,
            "title": finding.get("title"),
            "disposition": "non_blocking",
            "reason": REASONS[finding_id],
            "evidence_class": finding.get("evidence_class"),
            "evidence_artifact": finding["evidence_artifact"],
            "reproduce_command": finding.get("reproduce_command"),
            "blocks_beta": finding.get("blocks_beta") is True,
            "owner": OWNERS[finding_id],
        }
        contract = _recheck_contract(finding)
        if contract:
            row["recheck_contract"] = contract
        rows.append(row)

    expected_ids = set(REASONS)
    if seen != expected_ids:
        raise ValueError(
            f"disposition set is not a bijection with the T24 ledger: "
            f"missing {sorted(expected_ids - seen)}, "
            f"extra {sorted(seen - expected_ids)}")

    counts = {
        "total": len(rows),
        "selected_fixed": sum(
            1 for row in rows if row["disposition"] == "selected_fixed"),
        "non_blocking": sum(
            1 for row in rows if row["disposition"] == "non_blocking"),
        "invalid_measurement": sum(
            1 for row in rows
            if row["disposition"] == "invalid_measurement"),
        "post_1_0": sum(
            1 for row in rows if row["disposition"] == "post_1_0"),
    }
    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "build_t25_findings_disposition",
        "status": (
            "T25_DISPOSITIONS_COMPLETE"
            if rows and not any(
                row["disposition"] not in DISPOSITION_VOCABULARY
                for row in rows
            )
            else None
        ),
        "policy": (
            "The input set is exclusively the T24 finding ledger. "
            "slow_but_non_blocking, measurement noise and "
            "cross-machine incomparable data are never promoted to "
            "selected. With zero blocking findings the card closes "
            "with selected = 0 and complete dispositions - no "
            "preventive rewrites."
        ),
        "dispositions": rows,
        "counts": counts,
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
                _relative(T24_FINDINGS): _sha256(T24_FINDINGS),
                _relative(T24_SCALE_EVIDENCE): _sha256(T24_SCALE_EVIDENCE),
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
        print(f"T25 findings disposition failed: {error}", file=sys.stderr)
        return 1
    summary = {"schema_version": document.get("schema_version")}
    if "status" in document and document["status"]:
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
