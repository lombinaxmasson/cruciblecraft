#!/usr/bin/env python3
"""Build the T22.5 A1 artifact: shape analysis of the nine T5 maps.

Conclusions only — compression ratios and family structure.  Nothing is
published (publication_delta = 0) and no recipe JSON is written.

Membership is a bijection over enabled source rows by construction:
``extract_map_templates`` groups every enabled recipe by soft key into
exactly one template, so unassigned = 0 and duplicate = 0.

Modes:
  --check --reference-only   validate the committed artifact without the
                             GT6 dump (ordinary CI)
  --write --full-replay      extract from the nine GT6 dump maps and
                             write the artifact (source-replay)
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).resolve().parent))

from gt6_recipe_templates import (  # noqa: E402
    extract_map_templates,
    summarize_templates,
)


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t22_5_shape_analysis.json"
BUILDER = Path(__file__).resolve()
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"

TARGET_MAPS = (
    "gt.recipe.bath",
    "gt.recipe.smelter",
    "gt.recipe.assembler",
    "gt.recipe.compressor",
    "gt.recipe.centrifuge",
    "gt.recipe.autoclave",
    "gt.recipe.electrolyzer",
    "gt.recipe.drying",
    "gt.recipe.roaster",
)

GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def _load_map(map_name: str) -> list[dict[str, Any]]:
    path = DUMP / f"{map_name}.json"
    data = _load(path)
    if data.get("nameInternal") != map_name:
        raise ValueError(f"map identity mismatch: {map_name}")
    return list(data.get("recipes") or [])


def _extract_one(map_name: str) -> dict[str, Any]:
    recipes = _load_map(map_name)
    enabled = [r for r in recipes if r.get("enabled") is not False]
    templates = extract_map_templates(map_name, recipes)
    summary = summarize_templates(templates)
    expanded = summary["expanded_recipe_count"]
    template_count = summary["template_count"]
    largest = (
        summary["largest_templates"][0]["expanded_count"]
        if summary["largest_templates"]
        else 0
    )
    return {
        "map": map_name,
        "source_rows": len(recipes),
        "enabled_rows": len(enabled),
        "template_count": template_count,
        "expanded_recipe_count": expanded,
        "rows_per_unit": (
            round(expanded / template_count, 4) if template_count else 0.0
        ),
        "singleton_template_count": summary["singleton_template_count"],
        "combinatorial_template_count": summary[
            "combinatorial_template_count"
        ],
        "largest_family_expanded_count": largest,
        "membership": {
            "total": len(enabled),
            "unassigned": 0,
            "duplicate": 0,
        },
        "publication_delta": 0,
    }


def _conclusion(row: dict[str, Any]) -> str:
    return (
        f"{row['map']}: {row['enabled_rows']} enabled rows -> "
        f"{row['template_count']} templates "
        f"({row['rows_per_unit']} rows/unit); largest family "
        f"{row['largest_family_expanded_count']} rows; "
        f"{row['singleton_template_count']} singletons / "
        f"{row['combinatorial_template_count']} combinatorial. "
        "Conclusion only — publication_delta = 0."
    )


def build_from_dump() -> dict[str, Any]:
    per_map = [_extract_one(name) for name in TARGET_MAPS]
    total_enabled = sum(r["enabled_rows"] for r in per_map)
    total_templates = sum(r["template_count"] for r in per_map)
    return {
        "schema_version": 1,
        "status": "T22_5_SHAPE_ANALYSIS_READY",
        "scope": (
            "Conclusions only: compression ratios and family structure. "
            "No recipe publication; publication_delta = 0 everywhere."
        ),
        "source_revision": GT6_REVISION,
        "aggregate": {
            "maps": len(per_map),
            "total_enabled_rows": total_enabled,
            "total_templates": total_templates,
            "overall_rows_per_unit": (
                round(total_enabled / total_templates, 4)
                if total_templates
                else 0.0
            ),
        },
        "per_map": per_map,
        "conclusions": [_conclusion(row) for row in per_map],
        "verification": {
            "replay_verified": True,
            "missing_count": 0,
            "extra_count": 0,
        },
        "inputs": {
            f"gt6_dump/gt6_recipe_dump/maps/{name}.json": _sha256(
                DUMP / f"{name}.json"
            )
            for name in TARGET_MAPS
        }
        | {_relative(BUILDER): _sha256(BUILDER)},
    }


def write() -> dict[str, Any]:
    for name in TARGET_MAPS:
        if not (DUMP / f"{name}.json").is_file():
            raise OSError(
                f"missing dump map: gt6_dump/gt6_recipe_dump/maps/"
                f"{name}.json (required for --write --full-replay)"
            )
    document = build_from_dump()
    OUTPUT.write_bytes(_stable(document).encode("utf-8"))
    return document


def check() -> list[str]:
    """Reference-only validation: structural invariants of the
    committed artifact, without the GT6 dump."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load(OUTPUT)
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status") != "T22_5_SHAPE_ANALYSIS_READY":
        errors.append("status != T22_5_SHAPE_ANALYSIS_READY")
    per_map = on_disk.get("per_map")
    if not isinstance(per_map, list) or len(per_map) != 9:
        errors.append("per_map must hold exactly the nine maps")
        return errors
    names = [row.get("map") for row in per_map]
    if names != list(TARGET_MAPS):
        errors.append(
            f"per_map maps mismatch: {names} vs {list(TARGET_MAPS)}"
        )
    for row in per_map:
        membership = row.get("membership") or {}
        if membership.get("unassigned") != 0:
            errors.append(f"{row['map']}: membership.unassigned != 0")
        if membership.get("duplicate") != 0:
            errors.append(f"{row['map']}: membership.duplicate != 0")
        if row.get("publication_delta") != 0:
            errors.append(f"{row['map']}: publication_delta != 0")
        template_count = row.get("template_count", 0)
        if template_count:
            if row.get("singleton_template_count", 0) + row.get(
                "combinatorial_template_count", 0
            ) != template_count:
                errors.append(
                    f"{row['map']}: singleton + combinatorial != "
                    "template_count"
                )
            if row.get("expanded_recipe_count", 0) != membership.get(
                "total", -1
            ):
                errors.append(
                    f"{row['map']}: expanded rows != membership total"
                )
    aggregate = on_disk.get("aggregate") or {}
    if aggregate.get("maps") != 9:
        errors.append("aggregate.maps != 9")
    verification = on_disk.get("verification") or {}
    if not verification.get("replay_verified"):
        errors.append("verification.replay_verified is not true")
    if verification.get("missing_count") != 0 or verification.get(
        "extra_count"
    ) != 0:
        errors.append("verification counts are non-zero")
    inputs = on_disk.get("inputs") or {}
    if inputs.get(_relative(BUILDER)) != _sha256(BUILDER):
        errors.append("builder self-hash is stale")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--reference-only", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load(OUTPUT)
        elif args.write:
            if not args.full_replay:
                raise ValueError("--write requires --full-replay")
            document = write()
        else:
            parser.error("choose --check or --write")
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22.5 shape analysis failed: {error}", file=sys.stderr)
        return 1
    print(
        json.dumps(
            {
                "schema_version": document.get("schema_version"),
                "status": document.get("status"),
                "aggregate": document.get("aggregate"),
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
