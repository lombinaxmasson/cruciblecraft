#!/usr/bin/env python3
"""Record and check the isolated T38 GameTest receipt.

--check never starts a GameTest server. It only accepts a committed PASS
receipt whose bound artifacts still match T38RecipeGameTests,
MaterialRegistrationGate, T38 recipe resource trees, and the committed
UTF-8 evidence log at tools/t38_gametest.log.

--write requires --from-log. It normalizes that log (including UTF-16) into
the version-controlled evidence file, then writes the receipt against that
file. A missing or failing log is not a pass. The gitignored run directory
is not evidence. Skip is not pass.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t38_common as t38  # noqa: E402

OUTPUT = t38.GAME_TEST_RECEIPT


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Record or check the isolated T38 GameTest receipt."
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
        raise ValueError(f"missing GameTest log: {t38.relative(log_path)}")
    parsed = t38.parse_gametest_log(t38.read_gametest_log(log_path))
    if parsed.get("status") != "PASS":
        raise ValueError(
            "T38 GameTest log is not PASS; skip_is_not_pass forbids writing a pass"
        )
    t38.commit_gametest_log(log_path)
    document = t38.gametest_receipt_document(parsed, t38.GAME_TEST_EVIDENCE_LOG)
    errors = t38.gametest_receipt_errors(document)
    if errors:
        raise ValueError("; ".join(errors))
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return t38.gametest_receipt_errors()


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    if args.check:
        errors = check()
        if errors:
            print("T38 GameTest receipt is not current:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T38 GameTest receipt is PASS for -Pt38Recipes.")
        return 0
    write(args.from_log)
    print(f"Wrote {t38.relative(OUTPUT)} and {t38.relative(t38.GAME_TEST_EVIDENCE_LOG)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
