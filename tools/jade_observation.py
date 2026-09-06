#!/usr/bin/env python3
"""Jade observation coverage matrix gate."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import io_common as io

MATRIX = io.TOOLS / "jade_observation_matrix.json"
EN_US = (
    ROOT / "src/generated/resources/assets/cruciblecraft/lang/en_us.json"
)
ZH_CN = (
    ROOT / "src/generated/resources/assets/cruciblecraft/lang/zh_cn.json"
)
REQUIRED_FIELDS = (
    "family",
    "live_identities",
    "static_profile_source",
    "dynamic_state_source",
    "sync_path",
    "display_sections",
    "translation_keys",
    "status",
    "owner",
    "reason",
)
STATUSES = {
    "ready",
    "ready_existing",
    "needs_runtime_accessor",
    "blocked",
    "follow_up",
}
SLICE_READY = {"crucible", "transformer"}


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def compile_manifest(document: dict[str, Any] | None = None) -> dict[str, Any]:
    matrix = document if document is not None else load_json(MATRIX)
    errors: list[str] = []
    if matrix.get("jade_scope_decision") != "foundation_plus_followup":
        errors.append("jade_scope_decision must be foundation_plus_followup")
    if not matrix.get("decision_reason"):
        errors.append("jade_scope_decision is missing a reason")
    families = list(matrix.get("families") or [])
    if not families:
        errors.append("coverage matrix has no families")
    seen: set[str] = set()
    english = load_json(EN_US) if EN_US.is_file() else {}
    chinese = load_json(ZH_CN) if ZH_CN.is_file() else {}
    for row in families:
        name = str(row.get("family") or "")
        if not name:
            errors.append("family row missing name")
            continue
        if name in seen:
            errors.append(f"duplicate family {name}")
        seen.add(name)
        for field in REQUIRED_FIELDS:
            if field not in row:
                errors.append(f"{name} missing {field}")
        status = str(row.get("status") or "")
        if status not in STATUSES:
            errors.append(f"{name} has unknown status {status}")
        if not row.get("live_identities"):
            errors.append(f"{name} missing live_identities")
        if status == "follow_up":
            if not row.get("owner") or not row.get("reason"):
                errors.append(f"{name} follow_up needs owner and reason")
        if name in SLICE_READY and status != "ready":
            errors.append(f"{name} must be ready on this card")
        if status in {"ready", "ready_existing"}:
            for key in row.get("translation_keys") or []:
                if key not in english:
                    errors.append(f"{name} missing en_us {key}")
                if key not in chinese:
                    errors.append(f"{name} missing zh_cn {key}")
    for required in SLICE_READY:
        if required not in seen:
            errors.append(f"coverage matrix missing {required}")
    return {
        "errors": errors,
        "family_count": len(families),
        "generated_by": "tools/jade_observation.py",
        "jade_scope_decision": matrix.get("jade_scope_decision"),
        "schema_version": 1,
        "status": "FAIL" if errors else "PASS",
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if not args.check:
        parser.error("choose --check")
    if not MATRIX.is_file():
        print(f"missing {io.relative(MATRIX)}", file=sys.stderr)
        return 1
    manifest = compile_manifest()
    if manifest["errors"]:
        print("\n".join(manifest["errors"]), file=sys.stderr)
        return 1
    print(
        "jade observation matrix is current "
        f"({manifest['family_count']} families, "
        f"{manifest['jade_scope_decision']})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
