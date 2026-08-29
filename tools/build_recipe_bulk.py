#!/usr/bin/env python3
"""Single CLI for the reusable recipe bulk compiler: analyze | compile | check."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t45_common as common
from tools.recipe_bulk import analyze as analyze_mod
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import replay as replay_mod


def _write_analyze() -> dict[str, Any]:
    document = analyze_mod.analyze()
    t35.write_stable(common.ANALYZE_REPORT, document)
    return document


def _check_analyze() -> list[str]:
    return common.check_document(common.ANALYZE_REPORT, analyze_mod.analyze())


def _write_compile() -> dict[str, Any]:
    built = compile_mod.compile()
    compile_mod.write_tree(built["planned"], common.GENERATED_ROOT)
    compile_mod.write_tree(built["planned"], common.CATALOG_FIXTURE_ROOT)
    t35.write_stable(common.COMPILE_REPORT, built["report"])
    fixture_root = (
        ROOT / "src/test/resources/t45_compiler_fixture/t43_replay.json"
    )
    t35.write_stable(fixture_root, replay_mod.replay_t43())
    return built["report"]


def _check_compile() -> list[str]:
    errors: list[str] = []
    errors.extend(_check_analyze())
    built = compile_mod.compile()
    errors.extend(common.check_document(common.COMPILE_REPORT, built["report"]))
    generated = {
        str(path): json.loads(path.read_text(encoding="utf-8"))
        for path in common.generated_family_files()
    }
    expected = {str(path): doc for path, doc in built["planned"]}
    if generated != expected:
        errors.append("T45 generated recipe tree drifted")
    replay = replay_mod.replay_t43()
    if not replay.get("ok"):
        errors.append(
            "T43 semantic replay failed: " + ",".join(replay.get("mismatches") or [])[:200]
        )
    fixture = ROOT / "src/test/resources/t45_compiler_fixture/t43_replay.json"
    if fixture.is_file():
        errors.extend(common.check_document(fixture, replay))
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Recipe bulk compiler")
    parser.add_argument("command", choices=("analyze", "compile", "check"))
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    command = args.command
    if command == "check":
        command = "compile"
    if command == "analyze":
        if args.write:
            _write_analyze()
            print(f"Wrote {common.relative(common.ANALYZE_REPORT)}")
            return 0
        errors = _check_analyze()
    else:
        if args.write:
            _write_compile()
            print(f"Wrote {common.relative(common.COMPILE_REPORT)}")
            return 0
        errors = _check_compile()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"recipe bulk {command} is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
