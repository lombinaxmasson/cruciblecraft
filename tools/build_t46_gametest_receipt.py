#!/usr/bin/env python3
"""Record and check the isolated T46 GameTest receipt."""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as t46

OUTPUT = t46.GAME_TEST_RECEIPT


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Record or check the isolated T46 GameTest receipt."
    )
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--from-log", type=Path)
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    if args.write and args.from_log is None:
        parser.error("--write requires --from-log; skip is not pass")
    if args.check and args.from_log is not None:
        parser.error("--check does not take --from-log")
    return args


def write(log_path: Path) -> dict:
    if not log_path.is_file():
        raise ValueError(f"missing GameTest log: {t46.relative(log_path)}")
    parsed = t46.parse_gametest_log(t46.read_gametest_log(log_path))
    if parsed.get("status") != "PASS":
        raise ValueError(
            "T46 GameTest log is not PASS; skip_is_not_pass forbids writing a pass"
        )
    t46.commit_gametest_log(log_path)
    document = t46.gametest_receipt_document(parsed, t46.GAME_TEST_EVIDENCE_LOG)
    errors = t46.gametest_receipt_errors(document)
    if errors:
        raise ValueError("; ".join(errors))
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return t46.gametest_receipt_errors()


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    if args.check:
        errors = check()
        if errors:
            print("T46 GameTest receipt is not current:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T46 GameTest receipt is PASS for -Pt46Recipes.")
        return 0
    write(args.from_log)
    print(f"Wrote {t46.relative(OUTPUT)} and {t46.relative(t46.GAME_TEST_EVIDENCE_LOG)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
