#!/usr/bin/env python3
"""Append-only T42 disposition lock. Overlay is diagnostic; the lock changes the gap."""
from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_common as common

OUTPUT = common.DISPOSITION_LOCK


def _overlay() -> dict[str, Any]:
    if common.BLOCKER_OVERLAY.is_file():
        return common.load_json(common.BLOCKER_OVERLAY)
    from tools import build_t42_blocker_overlay as overlay_builder

    return overlay_builder.build()["overlay"]


def _host_slug(host: str) -> str:
    return host.split(":", 1)[-1] if ":" in host else host


def lock_row(family: dict[str, Any]) -> dict[str, Any]:
    primary = str(family["primary_bucket"])
    secondary = set(family.get("secondary_blockers") or [])
    disposition = "retained_current_execution_gap"
    future_owner = None
    expressed_by = None
    reason = primary
    recheck = "retain in the current execution gap"
    if primary == "already_expressed":
        disposition = "closed_by_existing_expression"
        expressed_by = "live_published_relation"
        reason = "all_relations_match_live_identity"
        recheck = "logical relation identity vs T21/T37-T41 published recipes"
    elif (
        primary == "combinatorial_unproven"
        and family.get("template_key") in common.KNOWN_COMBINATORIAL_TEMPLATE_KEYS
        and "partial" not in secondary
    ):
        disposition = "phase_deferred"
        future_owner = f"later:combinatorial/{_host_slug(family['host'])}"
        reason = "combinatorial_player_path_unproven"
        recheck = "parameterized/slot/shard/B0/load representation exists"
    elif (
        primary == "needs_unique_block_or_mte"
        and family.get("recycling_candidate")
        and list(family.get("unique_kinds") or []) == ["mte"]
        and family.get("unique_objects")
        and not (
            secondary
            & {
                "partial",
                "acquisition_open",
                "unproven_vanilla",
                "unmapped_operands",
                "missing_fluids",
                "missing_forms",
            }
        )
    ):
        disposition = "phase_deferred"
        future_owner = "later:recycling"
        reason = "proven_recycling_or_disassembly"
        recheck = "source object registration and reachability"
    elif primary == "needs_prefix_or_molten":
        reason = "known_material_missing_form_or_fluid"
        recheck = "bounded material/form/molten/chemical fluid work"
    elif primary == "current_closure_ready":
        reason = "current_closure_ready"
        recheck = "next production lock may select this family"
    else:
        reason = f"retained_{primary}"
        recheck = "insufficient evidence to leave the execution gap"

    if disposition == "phase_deferred" and primary in {
        "current_closure_ready",
        "needs_prefix_or_molten",
    }:
        raise ValueError("ready or prefix families cannot be deferred")

    row = {
        "disposition": disposition,
        "evidence_root_sha256": family.get("evidence_root_sha256"),
        "expressed_by": expressed_by,
        "family_id": family["family_id"],
        "future_owner": future_owner,
        "host": family["host"],
        "primary_bucket": primary,
        "reason": reason,
        "recheck_condition": recheck,
        "relation_count": family.get("relation_count"),
        "template_key": family["template_key"],
    }
    if disposition == "phase_deferred":
        if not row["future_owner"] or not row["reason"] or not row["recheck_condition"]:
            raise ValueError(f"deferred family missing owner/reason/recheck: {row['family_id']}")
        if not row["evidence_root_sha256"]:
            raise ValueError(f"deferred family missing evidence root: {row['family_id']}")
    return row


def build() -> dict[str, Any]:
    overlay = _overlay()
    families = [lock_row(row) for row in overlay.get("families") or []]
    if len(families) != common.OPENING_EXECUTION_GAP:
        raise ValueError(
            f"disposition lock family count {len(families)} != {common.OPENING_EXECUTION_GAP}"
        )
    expressed = [
        row for row in families if row["disposition"] == "closed_by_existing_expression"
    ]
    deferred = [row for row in families if row["disposition"] == "phase_deferred"]
    retained = [
        row for row in families if row["disposition"] == "retained_current_execution_gap"
    ]
    retained_ready = [
        row for row in retained if row["primary_bucket"] == "current_closure_ready"
    ]
    retained_forms = [
        row for row in retained if row["primary_bucket"] == "needs_prefix_or_molten"
    ]
    return {
        "already_expressed_delta": len(expressed),
        "append_only": True,
        "deferred_family_ids": [row["family_id"] for row in deferred],
        "expressed_family_ids": [row["family_id"] for row in expressed],
        "families": families,
        "family_count": len(families),
        "generated_by": "python tools/build_t42_disposition_lock.py",
        "note": (
            "Overlay is diagnostic. Only this lock changes the execution gap. "
            "Ordinary --write is append-only against the repaired lock and "
            "cannot rewrite approved reason, owner, or family selection. "
            "Repair rewrite requires --approve-repair-lock."
        ),
        "opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "owns_families": 0,
        "reclassification_delta": len(deferred),
        "retained_current_execution_gap": len(retained),
        "retained_needs_form_or_molten_count": len(retained_forms),
        "retained_ready_count": len(retained_ready),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_DISPOSITION_LOCKED",
    }


def _approved_identity(row: dict[str, Any]) -> tuple[Any, ...]:
    return (
        row.get("disposition"),
        row.get("reason"),
        row.get("future_owner"),
        row.get("expressed_by"),
        row.get("family_id"),
    )


def write(*, approve_initial_lock: bool = False, approve_repair_lock: bool = False) -> None:
    document = build()
    allow_rewrite = approve_initial_lock or approve_repair_lock
    if OUTPUT.is_file() and not allow_rewrite:
        existing = common.load_json(OUTPUT)
        existing_rows = {
            row["family_id"]: row for row in existing.get("families") or []
        }
        derived_rows = {row["family_id"]: row for row in document["families"]}
        missing = sorted(set(existing_rows) - set(derived_rows))
        if missing:
            raise ValueError(
                "append-only lock cannot drop approved families: " + ", ".join(missing[:10])
            )
        rewritten = [
            family_id
            for family_id, row in existing_rows.items()
            if _approved_identity(row) != _approved_identity(derived_rows[family_id])
        ]
        if rewritten:
            raise ValueError(
                "refusing to rewrite approved T42 lock rows without "
                "--approve-repair-lock: " + ", ".join(rewritten[:10])
            )
    elif not OUTPUT.is_file() and not approve_initial_lock:
        raise ValueError(
            "initial T42 disposition lock requires --approve-initial-lock"
        )
    t35.write_stable(OUTPUT, document)


def check() -> list[str]:
    return common.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Freeze the T42 disposition lock")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    mode.add_argument("--rebind-currentness-only", action="store_true")
    parser.add_argument("--approve-initial-lock", action="store_true")
    parser.add_argument("--approve-repair-lock", action="store_true")
    args = parser.parse_args(argv)
    if args.rebind_currentness_only:
        from tools import currentness

        currentness.rebind_sidecar(OUTPUT)
        print(f"rebound currentness sidecar for {common.relative(OUTPUT)}")
        return 0
    try:
        if args.write:
            write(
                approve_initial_lock=args.approve_initial_lock,
                approve_repair_lock=args.approve_repair_lock,
            )
            print("Wrote T42 disposition lock.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 disposition lock is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42 disposition lock failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
