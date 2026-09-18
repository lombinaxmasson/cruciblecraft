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

from tools import census_common as census
from tools import block_object_common as common
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
from tools.recipe_bulk.source_import import (
    SourceImportError,
    check_import,
    write_import,
)

WAVE_CHOICES = (*FORWARD_COMPILE_ORDER, *SEMANTIC_COMPILE_ORDER, "all")
AUTHORITY_HISTORICAL = "historical-v1"
AUTHORITY_FORWARD = "forward-v2"
AUTHORITY_SEMANTIC = "semantic-v3"


def _write_analyze() -> dict[str, Any]:
    document = analyze_mod.analyze()
    census.write_stable(common.ANALYZE_REPORT, document)
    return document


def _check_analyze() -> list[str]:
    return common.check_document(common.ANALYZE_REPORT, analyze_mod.analyze())


def _normalize_waves(
    wave: str | None,
    authority: str | None,
) -> tuple[str, ...]:
    resolved = authority
    if wave is not None and wave != "all":
        parsed = parse_wave_token(wave, schema=SCHEMA_SEMANTIC)
        if resolved not in (None, AUTHORITY_SEMANTIC):
            raise ValueError(f"{wave} cannot compile under {resolved}")
        return (parsed.compile_key(),)
    if resolved is None:
        resolved = AUTHORITY_HISTORICAL
    if wave is None:
        if resolved == AUTHORITY_SEMANTIC:
            return SEMANTIC_COMPILE_ORDER
        return FORWARD_COMPILE_ORDER if resolved == AUTHORITY_FORWARD else COMPILE_ORDER
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
            tree_prefixes=spec.tree_prefixes,
        )
        if wave_id == "block/object":
            compile_mod.write_tree(
                built["planned"],
                common.CATALOG_FIXTURE_ROOT,
                spec.generated_root,
            )
            census.write_stable(common.COMPILE_REPORT, built["report"])
            fixture_root = (
                ROOT / "src/test/resources/block_object_compiler_fixture/smelter_stone_replay.json"
            )
            census.write_stable(fixture_root, replay_mod.replay_smelter_stone())
            last_report = built["report"]
        elif wave_id == "bath/mte":
            from tools import bath_mte_common as bath_mte

            census.write_stable(bath_mte.COMPILE_REPORT, built["report"])
            last_report = built["report"]
        elif wave_id == "bath/remainder":
            from tools import bath_remainder_common as bath_remainder

            census.write_stable(bath_remainder.COMPILE_REPORT, built["report"])
            last_report = built["report"]
        elif wave_id == "bath/identity":
            from tools import bath_identity_common as bath_identity

            census.write_stable(bath_identity.COMPILE_REPORT, built["report"])
            bath_identity.write_emitted_shard_proof(built["planned"])
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
    from tools.recipe_bulk.matrix import load_compact_family_documents

    generated = load_compact_family_documents(
        spec.generated_root,
        path_prefix=spec.path_prefix,
        tree_prefixes=spec.tree_prefixes,
    )
    expected = {str(path): doc for path, doc in built["planned"]}
    if generated != expected:
        return [f"{wave_id} generated recipe tree drifted"]
    return []


def _check_compile(waves: tuple[str, ...]) -> list[str]:
    errors: list[str] = []
    if "block/object" in waves:
        errors.extend(_check_analyze())
    for wave_id in waves:
        errors.extend(_check_wave_tree(wave_id))
    if "block/object" in waves:
        built = compile_mod.compile_wave("block/object")
        errors.extend(common.check_document(common.COMPILE_REPORT, built["report"]))
        replay = replay_mod.replay_smelter_stone()
        if not replay.get("ok"):
            errors.append("smelter/stone semantic replay failed: " + ",".join(replay.get("mismatches") or [])[:200])
        fixture = ROOT / "src/test/resources/block_object_compiler_fixture/smelter_stone_replay.json"
        if fixture.is_file():
            errors.extend(common.check_document(fixture, replay))
    if "bath/identity" in waves:
        from tools import bath_identity_common as bath_identity

        built = compile_mod.compile_wave("bath/identity")
        errors.extend(common.check_document(bath_identity.COMPILE_REPORT, built["report"]))
        errors.extend(bath_identity.check_emitted_shard_proof(built["planned"]))
    return errors


def _import_source(spec: str | None, *, write: bool) -> list[str]:
    if not spec:
        raise ValueError("import-source requires --spec")
    path = Path(spec)
    if not path.is_absolute():
        path = ROOT / path
    if write:
        result = write_import(path)
        print(f"Wrote import-source for {result['import_slug']}")
        return []
    return check_import(path)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Recipe bulk compiler")
    parser.add_argument(
        "command",
        choices=("analyze", "compile", "check", "import-source", "rewrite-matrix"),
    )
    parser.add_argument(
        "--wave",
        default=None,
        help="Semantic host/cohort slug such as smelter/ordinary-closure, or all.",
    )
    parser.add_argument(
        "--authority",
        choices=(AUTHORITY_HISTORICAL, AUTHORITY_FORWARD, AUTHORITY_SEMANTIC),
        default=None,
        help="Select the historical, forward, or semantic compile authority.",
    )
    parser.add_argument(
        "--spec",
        default=None,
        help="Path to recipe_import.json for import-source.",
    )
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    command = args.command
    if command == "check":
        command = "compile"
    try:
        if command == "rewrite-matrix":
            from tools.recipe_bulk.matrix import LIVE_RECIPE_ROOT, rewrite_tree

            if not args.write:
                print("rewrite-matrix requires --write", file=sys.stderr)
                return 1
            report = rewrite_tree(LIVE_RECIPE_ROOT)
            print(
                "Rewrote compact matrix families: "
                f"{report['rewritten']} of {report['files']} "
                f"(kept inline {report['kept_inline']})"
            )
            return 0
        if command == "import-source":
            errors = _import_source(args.spec, write=args.write)
        elif command == "analyze":
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
    except (WaveSlugError, ValueError, KeyError, SourceImportError) as error:
        print(str(error), file=sys.stderr)
        return 1
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"recipe bulk {command} is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
