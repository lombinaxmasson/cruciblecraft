#!/usr/bin/env python3
"""Project the committed T10 known-form sets into live material definitions."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MATERIALS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "materials"
)
PROJECTION = ROOT / "tools" / "t10_preflight_projection.json"

MULTI_FLAG = "gt6:itemgenerator/multiingots"
HOT_FLAG = "gt6:itemgenerator/hotingots"
T10_FLAGS = {MULTI_FLAG, HOT_FLAG}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def expected_projection() -> dict[str, set[str]]:
    projection = load(PROJECTION)
    routes = projection["route_projections"]
    if (
        routes["multi_ingot"]["material_count"] != 323
        or routes["hot_ingot"]["material_count"] != 321
    ):
        raise ValueError("T10 known-form projection denominator drifted")
    result: dict[str, set[str]] = {}
    for material_id in routes["multi_ingot"]["materials"]:
        result.setdefault(material_id, set()).add(MULTI_FLAG)
    for material_id in routes["hot_ingot"]["materials"]:
        result.setdefault(material_id, set()).add(HOT_FLAG)
    return result


def planned_documents() -> dict[Path, str]:
    projection = expected_projection()
    documents: dict[Path, str] = {}
    seen: set[str] = set()
    for filename in load(MATERIALS / "index.json"):
        path = MATERIALS / filename
        document = load(path)
        material_id = str(document["id"])
        flags = set(document.get("generation_flags") or []) - T10_FLAGS
        flags.update(projection.get(material_id, set()))
        document["generation_flags"] = sorted(flags)
        documents[path] = stable_json(document)
        if material_id in projection:
            seen.add(material_id)
    missing = sorted(set(projection) - seen)
    if missing:
        raise ValueError(
            "T10 known-form projection references absent materials: "
            + ", ".join(missing)
        )
    return documents


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write == args.check:
        parser.error("exactly one of --write or --check is required")
    documents = planned_documents()
    stale = [
        str(path.relative_to(ROOT)).replace("\\", "/")
        for path, content in documents.items()
        if not path.is_file()
        or path.read_text(encoding="utf-8") != content
    ]
    if args.write:
        for path, content in documents.items():
            path.write_text(content, encoding="utf-8", newline="\n")
        print(stable_json({
            "status": "WRITTEN",
            "material_documents": len(documents),
            "changed_documents": len(stale),
        }), end="")
        return 0
    print(stable_json({
        "status": "CURRENT" if not stale else "STALE",
        "stale_documents": stale,
    }), end="")
    return 0 if not stale else 1


if __name__ == "__main__":
    raise SystemExit(main())
