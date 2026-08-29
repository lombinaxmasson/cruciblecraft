#!/usr/bin/env python3
"""Record and check the isolated T44 Storage GameTest receipt.

--check never starts a GameTest server. Skip is not pass. The gitignored
run-t44-storage directory is not evidence.
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t39_common as t39  # noqa: E402
from tools import t44_common as common  # noqa: E402

OUTPUT = common.GAMETEST_RECEIPT


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Record or check the isolated T44 GameTest receipt."
    )
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--from-log", type=Path)
    parser.add_argument("--rebind-currentness-only", action="store_true")
    args = parser.parse_args(argv)
    modes = [args.write, args.check, args.rebind_currentness_only]
    if sum(bool(mode) for mode in modes) != 1:
        parser.error("choose exactly one of --write, --check, --rebind-currentness-only")
    if args.write and args.from_log is None:
        parser.error("--write requires --from-log; skip is not pass")
    if args.check and args.from_log is not None:
        parser.error("--check does not take --from-log")
    return args


def bound_artifacts() -> dict[str, Any]:
    return {
        "gametest_java": (
            t35.sha256_file(common.GAME_TEST_JAVA)
            if common.GAME_TEST_JAVA.is_file()
            else None
        ),
        "production_lock": (
            t35.sha256_file(common.PRODUCTION_LOCK)
            if common.PRODUCTION_LOCK.is_file()
            else None
        ),
        "bundled_catalog": (
            t35.sha256_file(common.BUNDLED_CATALOG)
            if common.BUNDLED_CATALOG.is_file()
            else None
        ),
        "equivalence": (
            t35.sha256_file(common.EQUIVALENCE)
            if common.EQUIVALENCE.is_file()
            else None
        ),
    }


def receipt_document(parsed: dict[str, Any], log_text: str) -> dict[str, Any]:
    test_ids = common.discovered_gametest_ids()
    required = len(test_ids)
    return {
        "bound_artifacts": bound_artifacts(),
        "command": common.GAME_TEST_COMMAND,
        "failed": int(parsed.get("failed") or 0),
        "generated_by": "python tools/build_t44_storage_gametest_receipt.py",
        "java_sha256": bound_artifacts()["gametest_java"],
        "java_source": common.relative(common.GAME_TEST_JAVA),
        "log_fingerprint": hashlib.sha256(
            t39.normalize_gametest_log_text(log_text).encode("utf-8")
        ).hexdigest(),
        "log_path": common.relative(common.GAMETEST_LOG),
        "namespace": common.GAME_TEST_NAMESPACE,
        "note": (
            "Source markers are not a pass. This receipt is the isolated "
            "-Pt44Storage result bound to the production lock, bundled catalog, "
            "and committed UTF-8 evidence log. The gitignored run-t44-storage "
            "directory is not evidence. Creative injection is not a player path."
        ),
        "pass_marker": f"All {required} required tests passed :)",
        "passed": int(parsed.get("passed") or 0),
        "required_tests": required,
        "schema_version": 1,
        "skip_is_not_pass": True,
        "status": parsed.get("status") or "FAIL",
        "test_ids": test_ids,
    }


def write(log_path: Path) -> dict[str, Any]:
    if not log_path.is_file():
        raise ValueError(f"missing GameTest log: {common.relative(log_path)}")
    raw = t39.read_gametest_log(log_path)
    text = t39.normalize_gametest_log_text(raw)
    required = len(common.discovered_gametest_ids())
    parsed = common.parse_gametest_log(text, required)
    if parsed.get("status") != "PASS":
        raise ValueError(
            "T44 GameTest log is not PASS; skip_is_not_pass forbids writing a pass"
        )
    common.GAMETEST_LOG.write_text(text, encoding="utf-8", newline="\n")
    document = receipt_document(parsed, text)
    errors = check_document(document)
    if errors:
        raise ValueError("; ".join(errors))
    t35.write_stable(OUTPUT, document)
    return document


def check_document(document: dict[str, Any] | None = None) -> list[str]:
    if document is None and not OUTPUT.is_file():
        return ["missing GameTest receipt: tools/t44_storage_gametest_receipt.json"]
    if not common.GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {common.relative(common.GAME_TEST_JAVA)}"]
    receipt = document if document is not None else common.load_json(OUTPUT)
    errors: list[str] = []
    test_ids = common.discovered_gametest_ids()
    required = len(test_ids)
    live = bound_artifacts()
    if receipt.get("status") != "PASS":
        errors.append("T44 GameTest receipt is not PASS")
    if "-Pt44Storage" not in str(receipt.get("command") or ""):
        errors.append("T44 GameTest receipt command is missing -Pt44Storage")
    if receipt.get("namespace") != common.GAME_TEST_NAMESPACE:
        errors.append("T44 GameTest receipt namespace drifted")
    if receipt.get("required_tests") != required:
        errors.append("T44 GameTest receipt required_tests drifted")
    if receipt.get("passed") != required:
        errors.append(f"T44 GameTest receipt passed count is not {required}")
    if receipt.get("failed") != 0:
        errors.append("T44 GameTest receipt records failures")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("T44 GameTest receipt must record skip_is_not_pass")
    if sorted(receipt.get("test_ids") or []) != test_ids:
        errors.append("T44 GameTest receipt test_ids drifted")
    if (receipt.get("bound_artifacts") or {}) != live:
        errors.append("T44 GameTest receipt bound artifacts drifted")
    if receipt.get("java_sha256") != live.get("gametest_java"):
        errors.append("T44 GameTest Java sha256 drifted")
    if str(receipt.get("log_path") or "") != common.relative(common.GAMETEST_LOG):
        errors.append("T44 GameTest receipt log_path must be tools/t44_gametest.log")
    if not common.GAMETEST_LOG.is_file():
        errors.append("missing committed GameTest log: tools/t44_gametest.log")
    else:
        log_text = t39.normalize_gametest_log_text(
            t39.read_gametest_log(common.GAMETEST_LOG)
        )
        fingerprint = hashlib.sha256(log_text.encode("utf-8")).hexdigest()
        if receipt.get("log_fingerprint") != fingerprint:
            errors.append("T44 GameTest log fingerprint drifted")
        parsed = common.parse_gametest_log(log_text, required)
        if parsed.get("status") != "PASS":
            errors.append("committed T44 GameTest log is not PASS")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    if args.rebind_currentness_only:
        from tools import currentness

        currentness.rebind_sidecar(OUTPUT)
        print(f"rebound currentness sidecar for {common.relative(OUTPUT)}")
        return 0
    if args.write:
        write(args.from_log)
        print(
            f"Wrote {common.relative(OUTPUT)} and {common.relative(common.GAMETEST_LOG)}"
        )
        return 0
    errors = check_document()
    if errors:
        print("T44 GameTest receipt is not current:", file=sys.stderr)
        print("\n".join(f"- {error}" for error in errors), file=sys.stderr)
        return 1
    print("T44 GameTest receipt is PASS for -Pt44Storage.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
