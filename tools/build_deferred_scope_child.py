#!/usr/bin/env python3
"""Write or check a deferred ordinary post-1.x scope child."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import closeout_seal
from tools import recycling_deferred_scope as scope
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir


def _root(slug: str) -> Path:
    return wave_dir(slug)


def write_child(slug: str) -> dict[str, Any]:
    child = scope.CHILDREN[slug]
    documents = scope.documents(child)
    root = _root(slug)
    root.mkdir(parents=True, exist_ok=True)
    census.write_stable(root / "wave.json", documents["wave"])
    census.write_stable(root / "scope_dispositions.json", documents["scope"])
    census.write_stable(root / "census_delta.json", documents["census"])
    census.write_stable(root / "topology.json", documents["topology"])
    census.write_stable(root / "readiness.json", documents["readiness"])
    census.write_stable(root / "closeout_seal.json", scope.seal_document(child, root))
    generated = (
        census.ROOT
        / "src"
        / "recipe_generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "recipe"
    )
    if slug == "smelter/deferred-recycling-edge":
        forbidden = generated / "smelter" / "deferred_recycling_edge"
        if forbidden.is_dir() and any(forbidden.rglob("*.json")):
            raise ValueError("edge child must not emit recovery recipes")
    if slug == "autoclave/deferred-recycling":
        forbidden = generated / "autoclave" / "deferred_recycling"
        if forbidden.is_dir() and any(forbidden.rglob("*.json")):
            raise ValueError("autoclave child must not emit recipes in 1.x")
    return {
        "owns_families": child.owns_families,
        "post_1x_scope_count": child.owns_families,
        "status": child.status,
        "unique_active_wave": child.next_child,
        "wave_slug": slug,
    }


def check_child(slug: str) -> list[str]:
    errors: list[str] = []
    if slug not in scope.CHILDREN:
        return [f"unknown scope child {slug}"]
    child = scope.CHILDREN[slug]
    if slug not in KNOWN_SEMANTIC_SLUGS or slug not in known_slugs():
        errors.append(f"{slug} is not registered")
        return errors
    spec = spec_for(slug)
    if spec.unique_active_wave != child.next_child:
        errors.append(f"unique_active_wave drifted: {spec.unique_active_wave}")
    if spec.next_unassigned != child.next_unassigned:
        errors.append("next_unassigned drifted")
    if spec.owns_families != child.owns_families:
        errors.append("owns_families drifted")
    if spec.production_lock is not None or spec.receipt is not None:
        errors.append("scope child must not carry a recipe lock or GameTest receipt")
    root = _root(slug)
    if not (root / "readiness.json").is_file():
        return errors + ["scope artifacts are missing"]
    try:
        live = scope.documents(child)
    except ValueError as error:
        return errors + [str(error)]
    for name, key in (
        ("census_delta.json", "census"),
        ("topology.json", "topology"),
        ("readiness.json", "readiness"),
        ("scope_dispositions.json", "scope"),
        ("wave.json", "wave"),
    ):
        committed = census.load_json(root / name)
        drift = census.first_json_diff(live[key], committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = census.load_json(root / "readiness.json")
    if readiness.get("status") != child.status:
        errors.append(f"readiness status {readiness.get('status')}")
    if int(readiness.get("evidence", {}).get("completion_delta") or 0) != 0:
        errors.append("scope child must not complete families as recipes")
    census = census.load_json(root / "census_delta.json")
    remaining = census.get("remaining_ordinary") or {}
    expected_recycling = child.opening_deferred_recycling - child.drop_recycling
    expected_total = child.opening_deferred_total - child.drop_total
    if remaining.get("deferred_recycling_count") != expected_recycling:
        errors.append("deferred recycling arithmetic drifted")
    if remaining.get("deferred_total") != expected_total:
        errors.append("deferred total arithmetic drifted")
    dispositions = census.load_json(root / "scope_dispositions.json").get("dispositions") or []
    if len(dispositions) != child.owns_families:
        errors.append("disposition count drifted from owns_families")
    kinds = {str(row.get("disposition") or "") for row in dispositions}
    if kinds - set(scope.ALLOWED):
        errors.append("illegal disposition leaked")
    if "post_1x_scope" not in kinds:
        errors.append("expected post_1x_scope dispositions")
    errors.extend(closeout_seal.check_wave_seal(slug))
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument(
        "--child",
        required=True,
        choices=sorted(scope.CHILDREN),
    )
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            print(json.dumps(write_child(args.child), sort_keys=True))
            return 0
        errors = check_child(args.child)
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{args.child} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"deferred scope child failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
