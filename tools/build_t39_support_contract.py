#!/usr/bin/env python3
"""T39 locked-support contract: 34 production routes, discovery is diagnose-only."""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t39_common as t39
from tools import t40_vr_common as vr

OUTPUT = vr.T39_SUPPORT_CONTRACT


def _route_files() -> dict[str, str]:
    files = sorted(
        path for path in t39.LOCKED_SUPPORT_ROOT.glob("*.json") if path.is_file()
    )
    return {t39.relative(path): t35.sha256_file(path) for path in files}


def build() -> dict[str, Any]:
    lock = t39.load_production_lock()
    support = lock.get("support") or {}
    route_keys = list(support.get("route_keys") or [])
    files = _route_files()
    order_independent = hashlib.sha256(
        t35.stable_json(files).encode("utf-8")
    ).hexdigest()
    emit_order = hashlib.sha256(
        t35.stable_json(route_keys).encode("utf-8")
    ).hexdigest()
    return {
        "schema_version": 1,
        "status": "T39_SUPPORT_CONTRACT",
        "generated_by": "python tools/build_t39_support_contract.py",
        "lock_sha256": t39.production_lock_sha256(),
        "locked_route_count": int(support.get("route_count") or len(route_keys)),
        "route_keys": route_keys,
        "order_independent_route_content_root_sha256": order_independent,
        "topological_emit_order_root_sha256": emit_order,
        "emitted_files": files,
        "note": (
            "Production emit reads the 34 locked routes. Discovery of 35 "
            "routes is --diagnose only and must not change production or lock."
        ),
    }


def write() -> dict[str, Any]:
    document = build()
    if int(document["locked_route_count"]) != 34:
        raise ValueError("T39 support contract must stay at 34 locked routes")
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = vr.check_document(OUTPUT, build())
    document = build()
    if int(document["locked_route_count"]) != 34:
        errors.append("T39 support contract locked_route_count drifted from 34")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = vr.parse_write_check(__doc__, argv)
    try:
        if args.write:
            write()
        else:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T39 support contract failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
