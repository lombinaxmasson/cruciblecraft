#!/usr/bin/env python3
"""Fail on `Path.write_text(...)` calls that omit `newline="\\n"`.

Python's text mode translates "\\n" to `os.linesep` on write, so a call without
an explicit `newline=` produces **CRLF on Windows and LF on Linux**.  Every
`--check` path in this repo compares with `Path.read_text(...)`, which applies
universal-newline translation on read and therefore *cannot see the
difference*.  The drift only becomes visible to consumers that hash raw bytes
(`read_bytes()`), of which this repo has 66.

That combination produced the T10 failure: `apply_t8_pipe_metadata --write`
(builder #14) rewrote all 1,774 material files as CRLF on Windows,
`build_t10_preflight_projection` (#17) recorded the CRLF
`material_tree_sha256`, and `apply_t10_form_flags --write` (#19) rewrote them
back to LF.  Net churn across the sweep was zero, so the rebuild reported a
fixed point while the committed T10 hash described a tree state that no longer
existed on disk.

Usage:
  python tools/check_text_write_newline.py          # report, exit 1 on findings
  python tools/check_text_write_newline.py --json
"""
from __future__ import annotations

import argparse
import ast
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"

# Call sites whose target is never byte-hashed and never version controlled.
# Every entry needs a reason; an unjustified entry re-opens the bug class.
ALLOWLIST: dict[str, str] = {}


def offenders(path: Path) -> list[tuple[int, str]]:
    try:
        tree = ast.parse(path.read_text(encoding="utf-8", errors="ignore"))
    except SyntaxError:
        return []
    found = []
    for node in ast.walk(tree):
        if not isinstance(node, ast.Call):
            continue
        func = node.func
        if not isinstance(func, ast.Attribute) or func.attr != "write_text":
            continue
        if any(kw.arg == "newline" for kw in node.keywords):
            continue
        target = ast.unparse(func.value)
        found.append((node.lineno, f"{target}.write_text(...)"))
    return found


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    findings = []
    for script in sorted(TOOLS.glob("*.py")):
        for lineno, snippet in offenders(script):
            key = f"{script.name}:{lineno}"
            if key in ALLOWLIST:
                continue
            findings.append({"file": f"tools/{script.name}",
                             "line": lineno, "call": snippet})

    if args.json:
        print(json.dumps(findings, indent=2))
    else:
        for row in findings:
            print(f'{row["file"]}:{row["line"]}  {row["call"]}'
                  '  -- missing newline="\\n"')
        print()
        print(f"{len(findings)} write_text call(s) without an explicit newline. "
              'On Windows these emit CRLF; every --check uses read_text() and '
              "cannot detect it.")
    return 1 if findings else 0


if __name__ == "__main__":
    raise SystemExit(main())
