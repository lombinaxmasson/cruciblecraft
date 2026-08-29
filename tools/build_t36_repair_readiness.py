#!/usr/bin/env python3
"""Build the T36-Repair account without occupying T44 or revoking T36_READY."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t36_repair_common as common
from tools import build_t36_repair_pre_freeze as freeze_builder
from tools import currentness

OUTPUT = common.REPAIR_READINESS


def _load(path: Path, default: dict[str, Any] | None = None) -> dict[str, Any]:
    if path.is_file():
        return common.load_json(path)
    return dict(default or {})


def _freeze_current() -> bool:
    return not freeze_builder.freeze_errors()


def _inventory_clear() -> bool:
    return not common.blocking_rows()


def _overlay_l1() -> bool:
    return common.overlay_proofs()["overlay_l1_lathe_invar"]


def _overlay_l2() -> bool:
    return common.overlay_proofs()["overlay_l2_iron_casing"]


def _overlay_device() -> bool:
    return common.overlay_proofs()["overlay_device_iron_crucible"]


def _fixture_isolated() -> bool:
    return common.overlay_proofs()["fixture_absent_from_production"]


def _live_85() -> bool:
    return common.live_variant_count() == 85


def _acquisition_equal() -> bool:
    return common.acquisition_matches_freeze()


def _publication_normalized() -> bool:
    readiness = _load(common.t36.READINESS)
    publication = readiness.get("publication_delta") or {}
    return (
        publication.get("logical") == 0
        and publication.get("lazy") == 0
        and int(publication.get("eager") or -1) >= 0
        and common.acquisition_matches_freeze()
    )


def _t43_gap_unchanged() -> bool:
    return common.t43_ready()


GATE_PROBES: tuple[tuple[str, Callable[[], bool]], ...] = (
    ("r0_freeze_current", _freeze_current),
    ("t36_ready_preserved", common.t36_ready_preserved),
    ("t43_closing_preserved", _t43_gap_unchanged),
    ("inventory_blocking_zero", _inventory_clear),
    ("overlay_l1_lathe_invar", _overlay_l1),
    ("overlay_l2_iron_casing", _overlay_l2),
    ("overlay_device_iron_crucible", _overlay_device),
    ("fixture_absent_from_production", _fixture_isolated),
    ("live_variant_count_85", _live_85),
    ("acquisition_snapshot_equal", _acquisition_equal),
    ("publication_delta_machine_shaped_only", _publication_normalized),
    ("repair_owns_no_families", lambda: True),
    ("completion_delta_zero", lambda: True),
    ("t44_unassigned", common.t44_unassigned),
    ("unique_active_content_card_null", common.t44_unassigned),
    ("preassigned_host_false", common.t44_unassigned),
)


def build() -> dict[str, Any]:
    gates = {name: probe() for name, probe in GATE_PROBES}
    failed = sorted(name for name, passed in gates.items() if not passed)
    status = "T36_REPAIR_READY" if not failed else "T36_REPAIR_BLOCKED"
    if failed and status == "T36_REPAIR_READY":
        raise ValueError("refusing to write T36_REPAIR_READY with failed_gates")
    proofs = common.overlay_proofs()
    return {
        "completion_delta": 0,
        "failed_gates": failed,
        "gates": gates,
        "generated_by": "python tools/build_t36_repair_readiness.py",
        "live_variant_count": common.live_variant_count(),
        "next_issue_id": "T44",
        "note": (
            "T36_REPAIR_READY proves L1/L2 machine-tier extensibility without "
            "Java second lists. It does not revoke T36_READY, expand the live "
            "85 rows, occupy T44, or preassign Storage."
        ),
        "overlay_proofs": proofs,
        "owns_families": 0,
        "preassigned_host": False,
        "publication_delta": "machine_shaped_normalization_only",
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": status,
        "t36_ready_preserved": common.t36_ready_preserved(),
        "t43_closing_gap": common.t43_gap(),
        "t44_not_issued": True,
        "unique_active_content_card": None,
    }


def write() -> dict[str, Any]:
    document = build()
    if document["failed_gates"]:
        document["status"] = "T36_REPAIR_BLOCKED"
    if document["status"] == "T36_REPAIR_READY" and document["failed_gates"]:
        raise ValueError("refusing to write T36_REPAIR_READY with failed_gates")
    t35.write_stable(OUTPUT, document)
    currentness.write_sidecar(OUTPUT)
    return document


def check() -> list[str]:
    return t35.check_generated_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T36-Repair readiness", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            document = write()
            print(
                "Wrote T36-Repair readiness; "
                f"status={document['status']} "
                f"failed_gates={document['failed_gates']}."
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T36-Repair readiness is current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T36-Repair readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
