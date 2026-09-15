#!/usr/bin/env python3
"""Project-level playtest cycle. Human runClient only; never auto-sign."""
from __future__ import annotations

import argparse
import json
import sys
from datetime import date
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import atomic_io
from tools import io_common as io

CYCLE_PATH = ROOT / "tools" / "playtest" / "current_cycle.json"
CHANGE_CLASSES = ("major", "minor", "none")
STATUSES = ("pending", "accepted")
SURVIVAL_ACCESS = (
    "unreviewed",
    "blocked",
    "partial",
    "complete",
    "not_applicable",
)


def dumps(document: Any) -> str:
    return json.dumps(document, indent=2, ensure_ascii=False) + "\n"


def load_cycle() -> dict[str, Any]:
    if not CYCLE_PATH.is_file():
        raise ValueError("missing tools/playtest/current_cycle.json")
    document = io.load_json(CYCLE_PATH)
    errors = validate_cycle(document)
    if errors:
        raise ValueError("; ".join(errors))
    return document


def validate_cycle(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if int(document.get("schema_version") or 0) != 1:
        errors.append("playtest cycle schema_version must be 1")
    if not str(document.get("id") or "").strip():
        errors.append("playtest cycle id required")
    if document.get("status") not in STATUSES:
        errors.append("playtest cycle status must be pending or accepted")
    if not isinstance(document.get("scope"), list):
        errors.append("playtest cycle scope must be a list")
    if document.get("status") == "accepted":
        accepted = document.get("accepted")
        if not isinstance(accepted, dict) or not str(accepted.get("signer") or ""):
            errors.append("accepted cycle requires a human signer")
        if str(accepted.get("source") or "") != "human_report":
            errors.append("accepted cycle source must be human_report")
    elif document.get("accepted") not in (None, {}):
        errors.append("pending cycle cannot keep an accepted block")
    return errors


def check_cycle() -> list[str]:
    if not CYCLE_PATH.is_file():
        return ["missing tools/playtest/current_cycle.json"]
    try:
        document = io.load_json(CYCLE_PATH)
    except (OSError, json.JSONDecodeError) as error:
        return [f"playtest cycle is not JSON: {error}"]
    return validate_cycle(document)


def _write_cycle(document: dict[str, Any]) -> None:
    errors = validate_cycle(document)
    if errors:
        raise ValueError("; ".join(errors))
    CYCLE_PATH.parent.mkdir(parents=True, exist_ok=True)
    atomic_io.write_text(CYCLE_PATH, dumps(document))


def record_change(change_class: str, reason: str, scope_item: str) -> dict[str, Any]:
    if change_class not in CHANGE_CLASSES:
        raise ValueError(f"unsupported change class {change_class!r}")
    cycle = load_cycle()
    if change_class in {"none", "minor"}:
        return cycle
    today = date.today().isoformat()
    if cycle.get("status") == "accepted":
        cycle = {
            "schema_version": 1,
            "id": f"{today}-major",
            "status": "pending",
            "opened_at": today,
            "reason": reason,
            "scope": [scope_item],
            "accepted": None,
        }
    else:
        if reason and reason not in str(cycle.get("reason") or ""):
            cycle["reason"] = f"{cycle.get('reason')}; {reason}".strip("; ")
        scope = list(cycle.get("scope") or [])
        if scope_item and scope_item not in scope:
            scope.append(scope_item)
        cycle["scope"] = scope
        cycle["status"] = "pending"
        cycle["accepted"] = None
    _write_cycle(cycle)
    return cycle


def record_accept(*, cycle_id: str, signer: str, notes: str = "") -> dict[str, Any]:
    cycle = load_cycle()
    if str(cycle.get("id") or "") != str(cycle_id or "").strip():
        raise ValueError(
            f"accept id {cycle_id!r} does not match current cycle "
            f"{cycle.get('id')!r}"
        )
    if cycle.get("status") != "pending":
        raise ValueError("current cycle is not pending")
    signer = str(signer or "").strip()
    if not signer:
        raise ValueError("human signer required")
    cycle["status"] = "accepted"
    cycle["accepted"] = {
        "at": date.today().isoformat(),
        "notes": notes,
        "signer": signer,
        "source": "human_report",
    }
    _write_cycle(cycle)
    return cycle


def survival_access_blocks_runtime_close(value: str | None) -> bool:
    """Obtain review is independent of runtime_ready close."""
    del value
    return False


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("check")
    major = sub.add_parser("record-major")
    major.add_argument("--reason", required=True)
    major.add_argument("--scope", required=True)
    accept = sub.add_parser("record-accept")
    accept.add_argument("--id", required=True)
    accept.add_argument("--signer", required=True)
    accept.add_argument("--notes", default="")
    accept.add_argument(
        "--i-playtested",
        action="store_true",
        help="required: confirms a human ran runClient; CI must never pass this",
    )
    args = parser.parse_args(argv)
    if args.command == "check":
        errors = check_cycle()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        cycle = load_cycle()
        print(f"playtest cycle {cycle['id']} {cycle['status']}")
        return 0
    if args.command == "record-major":
        cycle = record_change("major", args.reason, args.scope)
        print(f"pending {cycle['id']}")
        return 0
    if args.command == "record-accept":
        if not args.i_playtested:
            print(
                "record-accept requires --i-playtested after a real runClient",
                file=sys.stderr,
            )
            return 1
        cycle = record_accept(
            cycle_id=args.id,
            signer=args.signer,
            notes=args.notes,
        )
        print(f"accepted {cycle['id']} by {args.signer}")
        return 0
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
