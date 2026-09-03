#!/usr/bin/env python3
"""Rebuild semantic recipe waves and compare their parsed JSON directly."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.recipe_bulk.compile import planned_documents_for, write_tree
from tools.recipe_bulk.matrix import load_compact_family_documents
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave


def expected_documents(wave: str) -> dict[str, Any]:
    spec = recipe_wave(wave)
    return {
        path.relative_to(spec.generated_root).as_posix(): document
        for path, document in planned_documents_for(wave)
    }


def committed_documents(wave: str) -> dict[str, Any]:
    spec = recipe_wave(wave)
    if not spec.generated_root.is_dir():
        return {}
    loaded = load_compact_family_documents(spec.generated_root)
    documents: dict[str, Any] = {}
    for raw_path, document in loaded.items():
        path = Path(raw_path)
        try:
            relative = path.relative_to(spec.generated_root).as_posix()
        except ValueError:
            relative = path.as_posix()
        if spec.path_prefix:
            marker = "/" + spec.path_prefix.replace("\\", "/").strip("/") + "/"
            if marker not in "/" + relative:
                continue
        documents[relative] = document
    return documents


def compare_wave(wave: str) -> list[str]:
    expected = expected_documents(wave)
    actual = committed_documents(wave)
    errors: list[str] = []
    missing = sorted(set(expected) - set(actual))
    extra = sorted(set(actual) - set(expected))
    changed = sorted(
        path
        for path in set(expected) & set(actual)
        if expected[path] != actual[path]
    )
    if missing:
        errors.append(f"{wave}: missing {len(missing)} files: {missing[:5]}")
    if extra:
        errors.append(f"{wave}: extra {len(extra)} files: {extra[:5]}")
    if changed:
        errors.append(f"{wave}: changed {len(changed)} files: {changed[:5]}")
    return errors


def write_wave(wave: str) -> None:
    spec = recipe_wave(wave)
    planned = planned_documents_for(wave)
    write_tree(
        planned,
        spec.generated_root,
        spec.generated_root,
        path_prefix=spec.path_prefix,
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--write", action="store_true")
    parser.add_argument(
        "--wave",
        action="append",
        choices=SEMANTIC_COMPILE_ORDER,
        help="Limit to one or more semantic waves; default checks all.",
    )
    args = parser.parse_args(argv)
    waves = tuple(args.wave or SEMANTIC_COMPILE_ORDER)
    try:
        if args.write:
            for wave in waves:
                write_wave(wave)
            return 0
        errors: list[str] = []
        for wave in waves:
            errors.extend(compare_wave(wave))
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(
            json.dumps(
                {"status": "PASS", "waves": list(waves)},
                ensure_ascii=False,
                sort_keys=True,
            )
        )
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"semantic recipe rebuild failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
