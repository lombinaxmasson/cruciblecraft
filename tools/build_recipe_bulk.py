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
from tools.recipe_bulk.waves import (
    COMPILE_ORDER,
    FORWARD_COMPILE_ORDER,
    SEMANTIC_COMPILE_ORDER,
    recipe_wave,
)
from tools.recipe_bulk.slugs import SCHEMA_SEMANTIC, WaveSlugError, parse_wave_token

WAVE_CHOICES = (*FORWARD_COMPILE_ORDER, *SEMANTIC_COMPILE_ORDER, "all")
AUTHORITY_HISTORICAL = "historical-v1"
AUTHORITY_FORWARD = "forward-v2"
AUTHORITY_SEMANTIC = "semantic-v3"


def _write_analyze() -> dict[str, Any]:
    document = analyze_mod.analyze()
    t35.write_stable(common.ANALYZE_REPORT, document)
    return document


def _check_analyze() -> list[str]:
    return common.check_document(common.ANALYZE_REPORT, analyze_mod.analyze())


def _normalize_waves(
    wave: str | None,
    authority: str | None,
) -> tuple[str, ...]:
    resolved = authority
    if wave is not None and "/" in wave:
        parsed = parse_wave_token(wave, schema=SCHEMA_SEMANTIC)
        if resolved not in (None, AUTHORITY_SEMANTIC):
            raise ValueError(f"{wave} cannot compile under {resolved}")
        return (parsed.compile_key(),)
    if wave is not None and str(wave).upper() == "T50":
        raise WaveSlugError(
            "semantic schema rejects milestone token 'T50'; "
            "use a host/cohort slug and do not issue T50"
        )
    if wave in {"T46", "T47", "T48", "T49"}:
        if resolved == AUTHORITY_HISTORICAL:
            raise ValueError(f"{wave} cannot compile under historical-v1")
        if resolved == AUTHORITY_SEMANTIC:
            raise WaveSlugError(f"semantic schema rejects legacy token {wave!r}")
        resolved = AUTHORITY_FORWARD
    if resolved is None:
        resolved = AUTHORITY_HISTORICAL
    if wave is None:
        # Default forward-v2 wave stays T46 so historical/default flows are unchanged.
        return ("T46",) if resolved == AUTHORITY_FORWARD else ("T45",)
    if wave == "all":
        if resolved == AUTHORITY_SEMANTIC:
            return SEMANTIC_COMPILE_ORDER
        return FORWARD_COMPILE_ORDER if resolved == AUTHORITY_FORWARD else COMPILE_ORDER
    allowed = FORWARD_COMPILE_ORDER if resolved == AUTHORITY_FORWARD else COMPILE_ORDER
    if wave not in allowed:
        raise ValueError(f"unsupported recipe wave {wave} for {resolved}")
    return (wave,)


def _write_compile(waves: tuple[str, ...]) -> dict[str, Any]:
    last_report: dict[str, Any] = {}
    for wave_id in waves:
        spec = recipe_wave(wave_id)
        built = compile_mod.compile_wave(wave_id)
        compile_mod.write_tree(
            built["planned"],
            spec.generated_root,
            spec.generated_root,
            path_prefix=spec.path_prefix,
        )
        if wave_id == "T45":
            compile_mod.write_tree(
                built["planned"],
                common.CATALOG_FIXTURE_ROOT,
                spec.generated_root,
            )
            t35.write_stable(common.COMPILE_REPORT, built["report"])
            fixture_root = (
                ROOT / "src/test/resources/t45_compiler_fixture/t43_replay.json"
            )
            t35.write_stable(fixture_root, replay_mod.replay_t43())
            last_report = built["report"]
        elif wave_id == "T46":
            from tools import t46_common as t46

            t35.write_stable(t46.COMPILE_REPORT, built["report"])
            last_report = built["report"]
        elif wave_id == "T47":
            from tools import t47_common as t47

            t35.write_stable(t47.COMPILE_REPORT, built["report"])
            last_report = built["report"]
        elif wave_id == "T48":
            from tools import t48_common as t48

            t35.write_stable(t48.COMPILE_REPORT, built["report"])
            last_report = built["report"]
        elif "/" in wave_id:
            last_report = built["report"]
        else:
            last_report = built["report"]
    return last_report


def _check_wave_tree(wave_id: str) -> list[str]:
    spec = recipe_wave(wave_id)
    built = compile_mod.compile_wave(wave_id)
    if not spec.generated_root.exists():
        if not built["planned"]:
            return []
        return [f"{wave_id} generated recipe tree missing"]
    generated = {
        str(path): json.loads(path.read_text(encoding="utf-8"))
        for path in spec.generated_root.rglob("gt_recipe_*.json")
        if path.is_file()
    }
    if spec.path_prefix:
        marker = "/" + spec.path_prefix.replace("\\", "/").strip("/") + "/"
        generated = {
            path: doc
            for path, doc in generated.items()
            if marker in path.replace("\\", "/")
        }
    expected = {str(path): doc for path, doc in built["planned"]}
    if generated != expected:
        return [f"{wave_id} generated recipe tree drifted"]
    return []


def _check_compile(waves: tuple[str, ...]) -> list[str]:
    errors: list[str] = []
    if "T45" in waves:
        errors.extend(_check_analyze())
    for wave_id in waves:
        errors.extend(_check_wave_tree(wave_id))
    if "T45" in waves:
        built = compile_mod.compile_wave("T45")
        errors.extend(common.check_document(common.COMPILE_REPORT, built["report"]))
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
    parser.add_argument(
        "--wave",
        default=None,
        help="T37–T49, semantic slug such as smelter/ordinary-closure, or all.",
    )
    parser.add_argument(
        "--authority",
        choices=(AUTHORITY_HISTORICAL, AUTHORITY_FORWARD, AUTHORITY_SEMANTIC),
        default=None,
        help="historical-v1 compiles T37–T45. forward-v2 compiles T37–T49. semantic-v3 compiles slug waves.",
    )
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    command = args.command
    if command == "check":
        command = "compile"
    try:
        if command == "analyze":
            if args.write:
                _write_analyze()
                print(f"Wrote {common.relative(common.ANALYZE_REPORT)}")
                return 0
            errors = _check_analyze()
        else:
            waves = _normalize_waves(args.wave, args.authority)
            if args.write:
                _write_compile(waves)
                print(f"Wrote recipe_bulk compile for {','.join(waves)}")
                return 0
            errors = _check_compile(waves)
    except (WaveSlugError, ValueError, KeyError) as error:
        print(str(error), file=sys.stderr)
        return 1
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"recipe bulk {command} is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
