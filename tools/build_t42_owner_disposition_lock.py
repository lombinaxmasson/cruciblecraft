#!/usr/bin/env python3
"""Append-only T42-Owner disposition lock for evidence-backed deferrals."""
from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import builder_cli
from tools import t35_common as t35
from tools import t42_owner_common as common

OUTPUT = common.DISPOSITION_LOCK


def _documents() -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    if not common.TRACK_OVERLAY.is_file() or not common.RECOVERY_EVIDENCE.is_file():
        from tools import build_t42_owner_track_overlay as overlay_builder

        built = overlay_builder.build()
        overlay, recovery = built["overlay"], built["recovery"]
    else:
        overlay = common.load_json(common.TRACK_OVERLAY)
        recovery = common.load_json(common.RECOVERY_EVIDENCE)
    if not common.OBJECT_EXPRESSION_EVIDENCE.is_file():
        raise FileNotFoundError(
            "T42-Owner object-expression evidence input is required"
        )
    evidence = common.load_json(common.OBJECT_EXPRESSION_EVIDENCE)
    if (
        evidence.get("schema_version") != 1
        or evidence.get("status") != "T42_OWNER_OBJECT_EXPRESSION_EVIDENCE"
    ):
        raise ValueError("invalid T42-Owner object-expression evidence header")
    return overlay, recovery, evidence


def lock_row(
    family: dict[str, Any],
    recovery: dict[str, Any] | None,
    object_evidence: dict[str, Any] | None = None,
) -> dict[str, Any]:
    current_owner = str(family.get("current_owner") or "")
    if not common.valid_owner_track(current_owner):
        raise ValueError(f"{family.get('family_id')} lacks a valid current_owner")
    disposition = "retained_current_execution_gap"
    future_owner = None
    owner_state = family["owner_state"]
    recheck = f"{current_owner} owner evidence and bounded execution plan"
    reason = "owner_track_retained_current_execution_gap"
    if recovery and recovery.get("eligible_phase_deferred"):
        disposition = "phase_deferred"
        future_owner = "later:recycling"
        current_owner = None
        owner_state = "phase_deferred"
        reason = "proven_consumed_mte_material_recovery"
        recheck = "source object registration, acquisition, and recycling runtime behavior"
    elif object_evidence:
        future_owner = str(object_evidence.get("future_owner") or "")
        if not future_owner.startswith("later:object_expression/"):
            raise ValueError(f"invalid object-expression future owner: {future_owner}")
        expected_owner = future_owner.removeprefix("later:")
        if family.get("current_owner") != expected_owner:
            raise ValueError(
                f"object-expression evidence owner mismatch for {family['family_id']}"
            )
        if family.get("secondary_owner_tracks"):
            raise ValueError(
                f"object-expression evidence requires a single-axis family: {family['family_id']}"
            )
        disposition = "phase_deferred"
        current_owner = None
        owner_state = "phase_deferred"
        reason = "approved_object_expression_evidence"
        recheck = str(object_evidence.get("recheck_condition") or "")
        if not recheck:
            raise ValueError(
                f"object-expression evidence lacks recheck_condition: {family['family_id']}"
            )
    evidence_root = family.get("evidence_root_sha256")
    if object_evidence:
        evidence_root = common.sha256_stable(
            {
                "owner_track_root": evidence_root,
                "object_boundary_root_sha256": object_evidence[
                    "object_boundary_root_sha256"
                ],
                "runtime_behavior_root_sha256": object_evidence[
                    "runtime_behavior_root_sha256"
                ],
            }
        )
    row = {
        "current_owner": current_owner,
        "disposition": disposition,
        "evidence_root_sha256": evidence_root,
        "family_id": family["family_id"],
        "future_owner": future_owner,
        "host": family["host"],
        "interleave_disposition": (
            "locked_deferred_ordinary_ledger"
            if disposition == "phase_deferred"
            else "tracked_current_execution_gap"
        ),
        "owner_state": owner_state,
        "primary_bucket": family["primary_bucket"],
        "proposed_current_owner": family["current_owner"],
        "reason": reason,
        "recheck_condition": recheck,
        "relation_count": family["relation_count"],
        "secondary_owner_tracks": family.get("secondary_owner_tracks") or [],
        "template_key": family["template_key"],
    }
    if disposition == "phase_deferred":
        if not row["future_owner"] or row["current_owner"] is not None:
            raise ValueError(f"invalid deferred owner lock row: {row['family_id']}")
    elif not common.valid_owner_track(row["current_owner"]):
        raise ValueError(f"retained owner lock row lacks owner: {row['family_id']}")
    return row


def _object_evidence_by_id(
    evidence: dict[str, Any],
    overlay: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    rows = list(evidence.get("approvals") or [])
    known = {row["family_id"]: row for row in overlay.get("families") or []}
    approvals: dict[str, dict[str, Any]] = {}
    for row in rows:
        if not isinstance(row, dict):
            raise ValueError("object-expression approval must be an object")
        family_id = str(row.get("family_id") or "")
        if not family_id or family_id in approvals:
            raise ValueError("object-expression approval family ids must be unique")
        family = known.get(family_id)
        if family is None:
            raise ValueError(f"object-expression approval not in retained gap: {family_id}")
        future_owner = str(row.get("future_owner") or "")
        if not future_owner.startswith("later:object_expression/"):
            raise ValueError(f"invalid object-expression owner: {future_owner}")
        if family.get("current_owner") != future_owner.removeprefix("later:"):
            raise ValueError(f"object-expression approval owner mismatch: {family_id}")
        for key in (
            "object_boundary_root_sha256",
            "runtime_behavior_root_sha256",
            "recheck_condition",
        ):
            if not str(row.get(key) or ""):
                raise ValueError(f"object-expression approval missing {key}: {family_id}")
        approvals[family_id] = row
    return approvals


def build() -> dict[str, Any]:
    overlay, recovery, object_evidence = _documents()
    if overlay.get("family_count") != common.OPENING_EXECUTION_GAP:
        raise ValueError("T42-Owner lock requires 5,300 owner-track rows")
    recovery_by_id = {row["family_id"]: row for row in recovery.get("families") or []}
    object_evidence_by_id = _object_evidence_by_id(object_evidence, overlay)
    families = [
        lock_row(
            row,
            recovery_by_id.get(row["family_id"]),
            object_evidence_by_id.get(row["family_id"]),
        )
        for row in overlay.get("families") or []
    ]
    families.sort(key=lambda row: row["family_id"])
    retained = [
        row for row in families if row["disposition"] == "retained_current_execution_gap"
    ]
    deferred = [row for row in families if row["disposition"] == "phase_deferred"]
    if len(retained) + len(deferred) != common.OPENING_EXECUTION_GAP:
        raise ValueError("T42-Owner disposition lock arithmetic drifted")
    if any(not row["current_owner"] for row in retained):
        raise ValueError("T42-Owner retained lock rows must have current_owner")
    if any(row["current_owner"] for row in deferred):
        raise ValueError("T42-Owner deferred lock rows cannot retain current_owner")
    if any(not str(row["future_owner"] or "").startswith("later:") for row in deferred):
        raise ValueError("T42-Owner deferred lock rows require future_owner")
    return {
        "append_only": True,
        "base_t42_closing_execution_gap": common.OPENING_EXECUTION_GAP,
        "current_owner_counts": common.owner_counts(retained, "current_owner"),
        "deferred_family_ids": [row["family_id"] for row in deferred],
        "families": families,
        "family_count": len(families),
        "future_owner_counts": common.owner_counts(deferred, "future_owner"),
        "generated_by": "python tools/build_t42_owner_disposition_lock.py",
        "note": (
            "Only this append-only lock changes the post-T42 execution gap. "
            "An object kind tag does not phase-defer a family. Recycling leaves "
            "the gap only after every relation proves consumed MTE to registered "
            "material-identity recovery. Object-expression deferral additionally "
            "requires an explicit manual evidence approval."
        ),
        "object_expression_evidence_root_sha256": common.sha256_stable(
            object_evidence.get("approvals") or []
        ),
        "opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "owns_families": 0,
        "reclassification_delta": len(deferred),
        "retained_current_execution_gap": len(retained),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_OWNER_DISPOSITION_LOCKED",
    }


def _approved_identity(row: dict[str, Any]) -> tuple[Any, ...]:
    return (
        row.get("family_id"),
        row.get("disposition"),
        row.get("current_owner"),
        row.get("evidence_root_sha256"),
        row.get("future_owner"),
        row.get("reason"),
        row.get("recheck_condition"),
    )


def write(*, approve_initial_lock: bool = False, approve_owner_lock: bool = False) -> None:
    document = build()
    if not OUTPUT.is_file() and not approve_initial_lock:
        raise ValueError(
            "initial T42-Owner lock requires --approve-initial-lock after owner review"
        )
    if OUTPUT.is_file() and not approve_owner_lock:
        existing = common.load_json(OUTPUT)
        existing_rows = {
            row["family_id"]: row for row in existing.get("families") or []
        }
        derived_rows = {row["family_id"]: row for row in document["families"]}
        missing = sorted(set(existing_rows) - set(derived_rows))
        rewritten = [
            family_id
            for family_id, row in existing_rows.items()
            if family_id not in derived_rows
            or _approved_identity(row) != _approved_identity(derived_rows[family_id])
        ]
        if missing or rewritten:
            values = (missing + rewritten)[:10]
            raise ValueError(
                "refusing to rewrite approved T42-Owner lock rows without "
                "--approve-owner-lock: " + ", ".join(values)
            )
    t35.write_stable(OUTPUT, document)


def check() -> list[str]:
    return common.check_document(OUTPUT, build())


def _args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build T42-Owner disposition lock")
    builder_cli.add_managed_modes(parser)
    parser.add_argument("--approve-initial-lock", action="store_true")
    parser.add_argument("--approve-owner-lock", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = _args(argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write(
                approve_initial_lock=args.approve_initial_lock,
                approve_owner_lock=args.approve_owner_lock,
            )
            print("Wrote T42-Owner disposition lock.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42-Owner disposition lock is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42-Owner disposition lock failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
