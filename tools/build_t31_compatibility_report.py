#!/usr/bin/env python3
"""Build T31 client/server/optional-mod compatibility report."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402
from tools import t31_common as t31  # noqa: E402

OUTPUT = t31.TOOLS / "t31_compatibility_report.json"
EVIDENCE = t31.TOOLS / "t31_compat_evidence.json"
POLICY = t31.TOOLS / "t31_rc_policy.json"
BUILDER = Path(__file__).resolve()
HANDSHAKE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/network/"
    / "MaterialConfigurationHandshake.java"
)
HOPPER_MENU = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/menu/HopperMenu.java"
)
REPORT_OWNED = ("currentness",)


def _axis(name: str, status: str, evidence: str) -> dict[str, Any]:
    return {
        "evidence": evidence,
        "name": name,
        "status": status,
        "value": None if status == "pending" else True,
        "zero_filled": False,
    }


def build() -> dict[str, Any]:
    handshake = HANDSHAKE.read_text(encoding="utf-8") if HANDSHAKE.is_file() else ""
    hopper_menu = HOPPER_MENU.read_text(encoding="utf-8") if HOPPER_MENU.is_file() else ""
    handshake_ok = 'NETWORK_VERSION = "1"' in handshake
    hopper_does_not_mask = "handshake" not in hopper_menu.lower()
    evidence = common.load_json(EVIDENCE) if EVIDENCE.is_file() else {}
    measured = (evidence.get("axes") or {}) if isinstance(evidence, dict) else {}

    def axis_from_evidence(name: str, fallback_evidence: str) -> dict[str, Any]:
        if measured.get(name) is True:
            return _axis(
                name,
                "measured",
                common.relative(EVIDENCE) if EVIDENCE.is_file() else fallback_evidence,
            )
        return _axis(name, "pending", fallback_evidence)

    axes = {
        "handshake_network_version_1": _axis(
            "handshake_network_version_1",
            "measured" if handshake_ok else "pending",
            common.relative(HANDSHAKE),
        ),
        "hopper_menu_does_not_mask_protocol": _axis(
            "hopper_menu_does_not_mask_protocol",
            "measured" if hopper_does_not_mask else "pending",
            common.relative(HOPPER_MENU) if HOPPER_MENU.is_file() else "missing",
        ),
        "save_reload": axis_from_evidence("save_reload", "R5 dedicated save fixture"),
        "client_cc_only": axis_from_evidence("client_cc_only", "R5 client matrix"),
        "dedicated_cc_only": axis_from_evidence(
            "dedicated_cc_only", "R5 dedicated server matrix"
        ),
        "emi": axis_from_evidence("emi", "existing optional-mod contract"),
        "jade": axis_from_evidence("jade", "existing optional-mod contract"),
        "kubejs": axis_from_evidence("kubejs", "existing optional-mod contract"),
    }
    pending = [name for name, row in axes.items() if row["status"] == "pending"]
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(POLICY): common.sha256_file(POLICY),
        "tools/t31_common.py": common.sha256_file(t31.TOOLS / "t31_common.py"),
    }
    if EVIDENCE.is_file():
        owned_inputs[common.relative(EVIDENCE)] = common.sha256_file(EVIDENCE)
    if HANDSHAKE.is_file():
        owned_inputs[common.relative(HANDSHAKE)] = common.sha256_file(HANDSHAKE)
    status = (
        "T31_COMPATIBILITY_COMPLETE"
        if not pending
        else "T31_COMPATIBILITY_PENDING"
    )
    return {
        "axes": axes,
        "currentness": {"owned_inputs": owned_inputs},
        "generated_by": "python tools/build_t31_compatibility_report.py --write",
        "mod_version": t31.gradle_mod_version(),
        "owned_inputs": owned_inputs,
        "pending": pending,
        "plan": common.load_json(POLICY)["save_network_optional_mod_plan"],
        "schema_version": 1,
        "status": status,
        "status_owner": "build_t31_compatibility_report",
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = build()
    actual = json.loads(OUTPUT.read_text(encoding="utf-8"))
    for key in REPORT_OWNED:
        expected.pop(key, None)
        actual.pop(key, None)
    if common.stable_json(expected) != common.stable_json(actual):
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"status={document.get('status')}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
